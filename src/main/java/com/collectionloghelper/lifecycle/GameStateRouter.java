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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;

/**
 * Processes {@link GameStateChanged} events. Mirrors the
 * {@link VarbitChangeRouter} / {@code ChatEventHandler} extraction pattern --
 * the plugin keeps the {@code @Subscribe} annotation and delegates the body
 * to {@link #handle(GameStateChanged)}.
 *
 * <p>Two transitions matter:
 * <ul>
 *   <li>{@link GameState#LOGGED_IN} -- bootstraps cached varp/varbit state,
 *       refreshes accessibility/travel/slayer caches, rebuilds the
 *       sources-with-missing set, rescans the scene for tracked objects, and
 *       refreshes the panel's sync-status indicators.</li>
 *   <li>{@link GameState#LOGIN_SCREEN} -- clears per-character mutable state
 *       (collection state, sync flags, slayer task, travel caps, kill-time
 *       tracker, plugin data manager).</li>
 * </ul>
 *
 * <p>All other transitions (LOADING, CONNECTION_LOST, HOPPING, etc.) are
 * no-ops. The guard path is allocation-free: a single {@code ==} comparison
 * against each enum constant, no lambda capture, no per-event {@code new}
 * sites.
 *
 * <p>The LOGGED_IN bootstrap is dispatched to the client thread via the
 * plugin-supplied {@link ClientThread} because it touches client-thread-only
 * APIs ({@code refreshVarps}, {@code loadObtainedItems},
 * {@code refreshAccessibility}, {@code refreshQuestState}). The lambda
 * captured for {@link ClientThread#invokeLater} is the only per-event
 * allocation on the LOGGED_IN path -- and only one per transition, not per
 * tick.
 *
 * <p>Plugin callbacks keep state ownership in the plugin:
 * <ul>
 *   <li>{@code panelSupplier} -- returns the (nullable) panel so refresh
 *       can short-circuit if startUp hasn't completed.</li>
 *   <li>{@code missingItemsRebuilder} / {@code missingItemsApplier} --
 *       rebuilds the sources-with-missing cache and writes the result back to
 *       the plugin field.</li>
 *   <li>{@code onLoggedInFlags} -- sets {@code slayerRefreshPending} on the
 *       plugin.</li>
 *   <li>{@code onLoginScreenFlags} -- clears the plugin's transient flags on
 *       logout.</li>
 * </ul>
 *
 * <p>Part of issue #503 -- splitting {@code CollectionLogHelperPlugin} into
 * focused collaborators (Wave 15).
 */
@Slf4j
@Singleton
public class GameStateRouter
{
	private final ClientThread clientThread;
	private final DataModule data;
	private final EfficiencyModule efficiency;
	private final GuidanceModule guidance;
	private final SyncModule sync;
	private final RequirementsChecker requirementsChecker;
	private final PlayerTravelCapabilities travelCapabilities;
	private final PlayerLocationResolver playerLocationResolver;

	private Supplier<CollectionLogHelperPanel> panelSupplier;
	private Consumer<Set<String>> missingItemsApplier;
	private Supplier<Set<String>> missingItemsRebuilder;
	private Runnable clearMissingItems;
	private Runnable onLoggedInFlags;
	private Runnable onLoginScreenFlags;

	@Inject
	public GameStateRouter(
		ClientThread clientThread,
		DataModule data,
		EfficiencyModule efficiency,
		GuidanceModule guidance,
		SyncModule sync,
		RequirementsChecker requirementsChecker,
		PlayerTravelCapabilities travelCapabilities,
		PlayerLocationResolver playerLocationResolver)
	{
		this.clientThread = clientThread;
		this.data = data;
		this.efficiency = efficiency;
		this.guidance = guidance;
		this.sync = sync;
		this.requirementsChecker = requirementsChecker;
		this.travelCapabilities = travelCapabilities;
		this.playerLocationResolver = playerLocationResolver;
	}

	/**
	 * Wires plugin-owned callbacks. Called once from
	 * {@code CollectionLogHelperPlugin.startUp()}. Kept off the constructor to
	 * avoid a circular Guice dependency on the plugin itself.
	 *
	 * @param panelSupplier returns the plugin's panel (nullable during startup races)
	 * @param missingItemsRebuilder rebuilds and returns the new sources-with-missing set
	 * @param missingItemsApplier assigns the rebuilt set back to the plugin field
	 * @param onLoggedInFlags sets {@code slayerRefreshPending} on the plugin
	 * @param onLoginScreenFlags clears the plugin's transient flags on logout
	 */
	public void setCallbacks(
		Supplier<CollectionLogHelperPanel> panelSupplier,
		Supplier<Set<String>> missingItemsRebuilder,
		Consumer<Set<String>> missingItemsApplier,
		Runnable clearMissingItems,
		Runnable onLoggedInFlags,
		Runnable onLoginScreenFlags)
	{
		this.panelSupplier = panelSupplier;
		this.missingItemsRebuilder = missingItemsRebuilder;
		this.missingItemsApplier = missingItemsApplier;
		this.clearMissingItems = clearMissingItems;
		this.onLoggedInFlags = onLoggedInFlags;
		this.onLoginScreenFlags = onLoginScreenFlags;
	}

	/**
	 * Handles a single {@link GameStateChanged} event. The LOGGED_IN bootstrap
	 * is dispatched to the client thread; LOGIN_SCREEN reset runs inline (its
	 * callees are thread-safe -- {@code clearState}, {@code clearCache}, and
	 * the various {@code reset()} methods are pure state mutations on plugin-
	 * managed objects).
	 *
	 * <p>All other game states are no-ops.
	 */
	public void handle(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGGED_IN)
		{
			handleLoggedIn();
		}
		else if (gameState == GameState.LOGIN_SCREEN)
		{
			handleLoginScreen();
		}
	}

	private void handleLoggedIn()
	{
		// LOGGED_IN fires multiple times during login/transitions.
		// Only reset sync state on the first fire to avoid clearing
		// collection log / bank sync flags mid-session.
		sync.getSyncStateCoordinator().onGameStateLoggedIn();
		if (onLoggedInFlags != null)
		{
			onLoggedInFlags.run();
		}
		clientThread.invokeLater(() ->
		{
			data.getCollectionState().refreshVarps();
			data.getCollectionState().loadObtainedItems();
			data.getCollectionState().captureRecentItems();
			requirementsChecker.refreshAccessibility(data.getDatabase().getAllSources());
			travelCapabilities.refreshQuestState();
			travelCapabilities.refreshVarbits();
			data.getSlayerTaskState().refresh();
			sync.getSyncStateCoordinator().setLastObtainedCount(data.getCollectionState().getTotalObtained());
			if (missingItemsRebuilder != null && missingItemsApplier != null)
			{
				missingItemsApplier.accept(missingItemsRebuilder.get());
			}

			// Rescan scene for tracked objects after scene (re)load
			guidance.getObjectHighlightOverlay().rescanScene();

			// Per-character dir and cache-fresh check are handled in
			// onGameTick once varps and player name are available.

			CollectionLogHelperPanel panel = panelSupplier != null ? panelSupplier.get() : null;
			if (panel != null)
			{
				if (data.getDataSyncState().isCollectionLogSynced()
					&& data.getCollectionState().getTotalObtained() > 0)
				{
					panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
						data.getCollectionState().getTotalObtained());
				}
				else
				{
					panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.NOT_SYNCED, 0);
				}
				panel.updateDataSyncWarning();
				if (data.getDataSyncState().isBankScanned())
				{
					panel.updateClueSummary(data.getPlayerBankState());
				}
				panel.rebuild();
			}
		});
	}

	private void handleLoginScreen()
	{
		data.getCollectionState().clearState();
		requirementsChecker.clearCache();
		efficiency.getClueEstimator().resetBucket();
		data.getSlayerTaskState().reset();
		sync.getSyncStateCoordinator().onGameStateLoginScreen();
		sync.getSyncStateCoordinator().setLastObtainedCount(-1);
		if (clearMissingItems != null)
		{
			clearMissingItems.run();
		}
		if (onLoginScreenFlags != null)
		{
			onLoginScreenFlags.run();
		}
		playerLocationResolver.reset();
		guidance.getGuidanceOverlay().setShowCollectionLogReminder(false);
		guidance.getGuidanceOverlay().setShowBankReminder(false);
		data.getDataSyncState().reset();
		data.getPlayerBankState().reset();
		data.getPlayerInventoryState().reset();
		travelCapabilities.reset();
		efficiency.getKillTimeTracker().reset();
		data.getPluginDataManager().reset();
	}
}
