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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.efficiency.ScoredItem;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;

/**
 * Owns the trio of collection-state lifecycle responsibilities that previously
 * lived inline in {@link com.collectionloghelper.CollectionLogHelperPlugin}:
 *
 * <ol>
 *   <li>{@link #handleSequenceComplete(CollectionLogSource)} -- callback invoked by
 *       {@code GuidanceOverlayCoordinator} when an active guidance sequence
 *       finishes.  Flags a panel rebuild and ranked-source recompute, and
 *       auto-activates guidance for the next top efficiency pick when
 *       {@link CollectionLogHelperConfig#autoAdvanceGuidance()} is enabled.</li>
 *   <li>{@link #rebuildSourcesWithMissingItems()} -- recomputes the cached set
 *       of source names that still have unobtained items.  Returns the new
 *       set so the plugin can assign it to its {@code sourcesWithMissingItems}
 *       field (state ownership stays with the plugin to match
 *       {@code shutDown}'s clear semantics).</li>
 *   <li>{@link #exportEfficiencyIfEnabled()} -- writes the efficiency export
 *       file when {@link CollectionLogHelperConfig#exportEfficiencyLog()} is
 *       enabled.  Falls back to the global RuneLite directory if the
 *       per-character data manager has not resolved a player name yet.</li>
 * </ol>
 *
 * <p>The plugin wires four narrow callbacks once in {@code startUp()}:
 * <ul>
 *   <li>{@code onSequenceCompleteFlags} -- toggles {@code pendingPanelRebuild}
 *       and {@code rankedSourcesDirty} on the plugin.</li>
 *   <li>{@code rankedSourcesSupplier} -- returns the plugin's cached ranked
 *       efficiency list so auto-advance picks the same top item the panel
 *       would show.</li>
 *   <li>{@code activateGuidanceCallback} -- delegates back to the plugin's
 *       {@code activateGuidance(source, targetItemId)} so the client-thread
 *       wrap stays in one place.</li>
 * </ul>
 *
 * <p>Performance: {@link #handleSequenceComplete(CollectionLogSource)} only fires when a guidance
 * sequence completes (rare, user-driven).  Allocation discipline still holds
 * on guard-fail paths -- when auto-advance is disabled the method returns
 * after the flag callback with no allocations (see #527 / #523 lessons).
 *
 * <p>Part of issue #503 -- splitting {@code CollectionLogHelperPlugin} into
 * focused collaborators (Wave 13).
 */
@Slf4j
@Singleton
public class CollectionStateChangeHandler
{
	private final Client client;
	private final CollectionLogHelperConfig config;
	private final DataModule data;
	private final EfficiencyModule efficiency;
	private final GuidanceModule guidance;

	private Runnable onSequenceCompleteFlags;
	private Supplier<List<ScoredItem>> rankedSourcesSupplier;
	private BiConsumer<CollectionLogSource, Integer> activateGuidanceCallback;
	/**
	 * Returns {@code true} when the active source's target collection-log slot
	 * was unlocked during the sequence that just completed.  Wired to
	 * {@link com.collectionloghelper.guidance.GuidanceSequencer#wasTargetSlotUnlocked()}.
	 */
	private Supplier<Boolean> targetSlotUnlockedSupplier;

	@Inject
	public CollectionStateChangeHandler(
		Client client,
		CollectionLogHelperConfig config,
		DataModule data,
		EfficiencyModule efficiency,
		GuidanceModule guidance)
	{
		this.client = client;
		this.config = config;
		this.data = data;
		this.efficiency = efficiency;
		this.guidance = guidance;
	}

	/**
	 * Wires plugin-owned callbacks. Called once from
	 * {@code CollectionLogHelperPlugin.startUp()} after the plugin's private
	 * state and panel are constructed. Kept off the constructor to avoid a
	 * circular Guice dependency on the plugin itself.
	 */
	public void setCallbacks(
		Runnable onSequenceCompleteFlags,
		Supplier<List<ScoredItem>> rankedSourcesSupplier,
		BiConsumer<CollectionLogSource, Integer> activateGuidanceCallback,
		Supplier<Boolean> targetSlotUnlockedSupplier)
	{
		this.onSequenceCompleteFlags = onSequenceCompleteFlags;
		this.rankedSourcesSupplier = rankedSourcesSupplier;
		this.activateGuidanceCallback = activateGuidanceCallback;
		this.targetSlotUnlockedSupplier = targetSlotUnlockedSupplier;
	}

	/**
	 * Callback invoked by {@code GuidanceOverlayCoordinator} when a guidance
	 * sequence completes.  Flags a panel rebuild and ranked-source recompute.
	 *
	 * <p>Auto-advance (when {@link CollectionLogHelperConfig#autoAdvanceGuidance()}
	 * is enabled) gates on whether the completed source's target collection-log
	 * slot actually unlocked during the sequence:
	 * <ul>
	 *   <li>Slot unlocked → advance to the next top efficiency pick as before.</li>
	 *   <li>Slot NOT unlocked and the source still has missing items →
	 *       re-activate guidance on the same source so the player loops
	 *       (e.g., repeated boss kills) until the drop arrives.</li>
	 *   <li>Slot NOT unlocked but every item is already obtained → advance; a
	 *       fully-obtained source can only ever complete drop-less, so
	 *       re-activating it would loop forever (#801, live Castle Wars case).</li>
	 * </ul>
	 *
	 * <p>This prevents the plugin from abandoning a source mid-grind just because
	 * the player completed the last guidance step (e.g., "kill boss") without
	 * receiving the target item.
	 *
	 * @param completedSource the source whose sequence just completed, captured
	 *                        by the coordinator BEFORE deactivation — by callback
	 *                        time the sequencer's own active source is already
	 *                        null (#801). May be {@code null} when no source
	 *                        context exists, in which case auto-advance falls
	 *                        through to the ranked pick.
	 */
	public void handleSequenceComplete(@Nullable CollectionLogSource completedSource)
	{
		if (onSequenceCompleteFlags != null)
		{
			onSequenceCompleteFlags.run();
		}
		if (!config.autoAdvanceGuidance())
		{
			return;
		}
		if (activateGuidanceCallback == null)
		{
			return;
		}

		boolean slotUnlocked = targetSlotUnlockedSupplier != null
			&& Boolean.TRUE.equals(targetSlotUnlockedSupplier.get());

		if (!slotUnlocked && completedSource != null)
		{
			if (hasMissingItems(completedSource))
			{
				// Target slot did not unlock — loop back on the same source.
				log.debug("Sequence complete but target slot not unlocked for {} — re-activating",
					completedSource.getName());
				activateGuidanceCallback.accept(completedSource, null);
				return;
			}
			// Every item already obtained: this source can only ever complete
			// drop-less. Fall through to the ranked advance instead of looping.
			log.debug("Sequence complete for {} with no missing items — advancing",
				completedSource.getName());
		}

		if (rankedSourcesSupplier == null)
		{
			return;
		}
		List<ScoredItem> ranked = rankedSourcesSupplier.get();
		if (ranked == null)
		{
			return;
		}
		ranked.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick ->
			{
				Integer targetItemId = topPick.getBestItem() != null
					? topPick.getBestItem().getItemId()
					: null;
				activateGuidanceCallback.accept(topPick.getSource(), targetItemId);
			});
	}

	/**
	 * Returns true when the source still has at least one unobtained
	 * collection-log item. Guards the not-unlocked re-activation loop: a
	 * fully-obtained source must advance, never re-loop (#801).
	 */
	private boolean hasMissingItems(CollectionLogSource source)
	{
		List<CollectionLogItem> items = source.getItems();
		if (items == null)
		{
			return false;
		}
		PlayerCollectionState collectionState = data.getCollectionState();
		for (CollectionLogItem item : items)
		{
			if (!collectionState.isItemObtained(item.getItemId()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Rebuilds and returns the set of source names that have at least one
	 * unobtained item. The plugin assigns the result to its own
	 * {@code sourcesWithMissingItems} field so the lifecycle reset in
	 * {@code shutDown} retains its clear semantics.
	 */
	public Set<String> rebuildSourcesWithMissingItems()
	{
		return guidance.getGuidanceCoordinator().rebuildSourcesWithMissingItems(
			data.getDatabase(), data.getCollectionState());
	}

	/**
	 * Writes the efficiency export file when
	 * {@link CollectionLogHelperConfig#exportEfficiencyLog()} is enabled.
	 * No-op when the config is disabled. Falls back to the global RuneLite
	 * directory if the per-character data manager has not resolved a player
	 * name yet (login race condition).
	 */
	public void exportEfficiencyIfEnabled()
	{
		if (!config.exportEfficiencyLog())
		{
			return;
		}
		File exportFile = data.getPluginDataManager().getFile("efficiency-export.txt");
		if (exportFile == null)
		{
			// Fallback if player name not yet available
			exportFile = new File(
				RuneLite.RUNELITE_DIR, "collection-log-efficiency-export.txt");
		}
		efficiency.getCalculator().exportEfficiencyList(exportFile, client);
	}
}
