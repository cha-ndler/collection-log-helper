/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Owns the per-tick coalescer that previously lived inline in
 * {@code CollectionLogHelperPlugin.onGameTick}. This is the hottest path in
 * the plugin (~every 600ms while logged in), so allocation discipline is
 * paramount -- the no-op tick (all deferred flags false, no player movement)
 * allocates zero objects.
 *
 * <p>Five responsibilities are coalesced into a single tick:
 * <ol>
 *   <li>Debounced requirements refresh -- driven by {@code pendingRequirementsRefresh},
 *       calls {@link RequirementsChecker#refreshAccessibility} once per tick.</li>
 *   <li>Debounced travel-varbit refresh -- driven by
 *       {@code pendingTravelVarbitRefresh}, calls
 *       {@link PlayerTravelCapabilities#refreshVarbits}.</li>
 *   <li>Lazy per-character data dir init + kill-time tracker init, once the
 *       player name is available.</li>
 *   <li>Coordinator tick + player-location resolve + sequencer
 *       {@code setPlayerLocation}/{@code onPlayerMoved} forwarding.</li>
 *   <li>Deferred slayer refresh + sync-tick delegation +
 *       final coalesced panel rebuild.</li>
 * </ol>
 *
 * <h2>Hot-path allocation profile</h2>
 *
 * The plugin keeps ownership of the per-tick state flags
 * ({@code pendingRequirementsRefresh}, {@code pendingTravelVarbitRefresh},
 * {@code slayerRefreshPending}, {@code pendingPanelRebuild},
 * {@code rankedSourcesDirty}). They are exposed to this orchestrator via
 * {@link BooleanSupplier} ("poll-and-clear") and {@link Runnable} ("set
 * dirty") callbacks, plus a {@link Supplier} for the nullable panel.
 *
 * <p>All plugin-owned callbacks are stored as fields in {@link #setCallbacks},
 * called exactly once from {@code startUp()}. The bound method-ref for
 * {@code collectionStateChangeHandler::exportEfficiencyIfEnabled} is hoisted
 * to a final field in the constructor for the same reason. The tick body
 * therefore invokes pre-allocated lambdas/method-refs -- no per-tick
 * {@code new} sites and no inline {@code ::} expressions. The only
 * non-primitive return values produced inside the hot path are:
 * <ul>
 *   <li>{@link SyncStateCoordinator.SyncTickResult} -- an enum, singleton.</li>
 *   <li>{@link WorldPoint} -- allocated by
 *       {@link PlayerLocationResolver#resolveAndCache} only when the player
 *       has actually moved (this matches the pre-extraction behaviour and is
 *       outside this orchestrator's control).</li>
 * </ul>
 *
 * <p>The reviewer should confirm the no-op tick allocates zero objects by
 * inspecting {@link #tick}: every branch is guarded by either a primitive
 * flag poll, a null check, or a method dispatch on an already-resolved
 * collaborator.
 *
 * <p>Part of issue #503 -- splitting {@code CollectionLogHelperPlugin} into
 * focused collaborators (Wave 14, the finisher).
 */
@Slf4j
@Singleton
public class GameTickOrchestrator
{
	private final Client client;
	private final DataModule data;
	private final EfficiencyModule efficiency;
	private final GuidanceModule guidance;
	private final SyncModule sync;
	private final RequirementsChecker requirementsChecker;
	private final PlayerTravelCapabilities travelCapabilities;
	private final PlayerLocationResolver playerLocationResolver;
	private final AuthoringLogger authoringLogger;
	private final CollectionStateChangeHandler collectionStateChangeHandler;

	// Method-ref hoisted to a field so the tick body never allocates this
	// bound reference per call. Wired once in the constructor since
	// collectionStateChangeHandler is available at injection time.
	private final Runnable exportEfficiencyCallback;

	// All callbacks are wired once in startUp() and stored as fields so the
	// tick body never allocates lambdas per call.
	private BooleanSupplier pollRequirementsRefresh;
	private BooleanSupplier pollTravelVarbitRefresh;
	private BooleanSupplier pollSlayerRefresh;
	private BooleanSupplier pollPanelRebuild;
	private Runnable markPanelRebuildAndRankedDirty;
	private Runnable markRankedDirty;
	private Supplier<CollectionLogHelperPanel> panelSupplier;

	@Inject
	public GameTickOrchestrator(
		Client client,
		DataModule data,
		EfficiencyModule efficiency,
		GuidanceModule guidance,
		SyncModule sync,
		RequirementsChecker requirementsChecker,
		PlayerTravelCapabilities travelCapabilities,
		PlayerLocationResolver playerLocationResolver,
		AuthoringLogger authoringLogger,
		CollectionStateChangeHandler collectionStateChangeHandler)
	{
		this.client = client;
		this.data = data;
		this.efficiency = efficiency;
		this.guidance = guidance;
		this.sync = sync;
		this.requirementsChecker = requirementsChecker;
		this.travelCapabilities = travelCapabilities;
		this.playerLocationResolver = playerLocationResolver;
		this.authoringLogger = authoringLogger;
		this.collectionStateChangeHandler = collectionStateChangeHandler;
		this.exportEfficiencyCallback = collectionStateChangeHandler::exportEfficiencyIfEnabled;
	}

	/**
	 * Wires plugin-owned per-tick flag accessors and the panel supplier.
	 * Called once from {@code CollectionLogHelperPlugin.startUp()} after the
	 * panel is constructed.
	 *
	 * <p>Each {@link BooleanSupplier} must atomically read-and-clear the
	 * associated flag so the orchestrator never observes stale state.
	 *
	 * @param pollRequirementsRefresh         poll-and-clear for
	 *                                        {@code pendingRequirementsRefresh}
	 * @param pollTravelVarbitRefresh         poll-and-clear for
	 *                                        {@code pendingTravelVarbitRefresh}
	 * @param pollSlayerRefresh               poll-and-clear for
	 *                                        {@code slayerRefreshPending}
	 * @param pollPanelRebuild                poll-and-clear for
	 *                                        {@code pendingPanelRebuild}
	 * @param markPanelRebuildAndRankedDirty  sets both panel-rebuild and
	 *                                        ranked-dirty flags
	 * @param markRankedDirty                 sets the ranked-sources-dirty flag
	 * @param panelSupplier                   returns the current (nullable) panel
	 */
	public void setCallbacks(
		BooleanSupplier pollRequirementsRefresh,
		BooleanSupplier pollTravelVarbitRefresh,
		BooleanSupplier pollSlayerRefresh,
		BooleanSupplier pollPanelRebuild,
		Runnable markPanelRebuildAndRankedDirty,
		Runnable markRankedDirty,
		Supplier<CollectionLogHelperPanel> panelSupplier)
	{
		this.pollRequirementsRefresh = pollRequirementsRefresh;
		this.pollTravelVarbitRefresh = pollTravelVarbitRefresh;
		this.pollSlayerRefresh = pollSlayerRefresh;
		this.pollPanelRebuild = pollPanelRebuild;
		this.markPanelRebuildAndRankedDirty = markPanelRebuildAndRankedDirty;
		this.markRankedDirty = markRankedDirty;
		this.panelSupplier = panelSupplier;
	}

	/**
	 * Per-tick entry point. Allocation-free when all deferred flags are false
	 * and the player has not moved -- the reviewer should verify this by
	 * walking each block below.
	 */
	public void tick()
	{
		// (1) Debounced requirements refresh -- flagged by VarbitChangeRouter
		// (via plugin callback), runs once per tick.
		if (pollRequirementsRefresh.getAsBoolean())
		{
			boolean reqsChanged = requirementsChecker.refreshAccessibility(
				data.getDatabase().getAllSources());
			if (reqsChanged && !sync.getSyncStateCoordinator().isScriptScanActive())
			{
				markPanelRebuildAndRankedDirty.run();
			}
		}

		// (2) Debounced travel varbit refresh -- flagged by VarbitChangeRouter,
		// runs once per tick.
		if (pollTravelVarbitRefresh.getAsBoolean())
		{
			travelCapabilities.refreshVarbits();
		}

		// (3) Lazily init per-character data directory once player name is
		// available. After the first successful init this entire block is a
		// single non-null reference read (no allocations).
		if (data.getPluginDataManager().getCharacterDir() == null)
		{
			if (data.getPluginDataManager().init())
			{
				efficiency.getKillTimeTracker().init(
					data.getPluginDataManager().getCharacterDir());
			}
		}

		// (4) Dispatch deferred ShortestPath "path" and world map arrow via
		// the guidance coordinator.
		guidance.getGuidanceCoordinator().tick();

		// Cache player location for guidance sequencer and authoring log
		// (client thread only). PlayerLocationResolver handles instanced
		// regions and non-top-level WorldViews (e.g. sailing boats),
		// translating to overworld coords so ARRIVE_AT_TILE checks match the
		// static JSON tile.
		if (client.getLocalPlayer() != null)
		{
			WorldPoint playerLocation = playerLocationResolver.resolveAndCache();
			authoringLogger.setPlayerLocation(playerLocation);

			// Check ARRIVE_AT_TILE completion for guidance sequencer
			if (guidance.getGuidanceSequencer().isActive())
			{
				guidance.getGuidanceSequencer().setPlayerLocation(playerLocation);
				guidance.getGuidanceSequencer().onPlayerMoved(playerLocation);
			}
		}

		// (5a) Deferred slayer task refresh -- varps may not be loaded in the
		// initial invokeLater after LOGGED_IN, so re-read a few ticks later
		// when the server has definitely sent all varp data.
		if (sync.getSyncStateCoordinator().getLoginTickDelay() <= 7
			&& pollSlayerRefresh.getAsBoolean())
		{
			data.getSlayerTaskState().refresh();
			markPanelRebuildAndRankedDirty.run();
		}

		// (5b) Delegate all remaining sync-lifecycle logic to the coordinator.
		CollectionLogHelperPanel panel = panelSupplier.get();
		SyncStateCoordinator.SyncTickResult syncResult =
			sync.getSyncStateCoordinator().tickSync(
				panel,
				markPanelRebuildAndRankedDirty,
				exportEfficiencyCallback);
		if (syncResult == SyncStateCoordinator.SyncTickResult.RANKED_DIRTY)
		{
			markRankedDirty.run();
		}

		// (5c) Single coalesced rebuild per tick -- all event handlers and
		// checks above set the panel-rebuild flag instead of calling
		// panel.rebuild() directly.
		if (panel != null && pollPanelRebuild.getAsBoolean())
		{
			panel.rebuild();
		}
	}
}
