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
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * Owns the symmetric teardown that previously lived inline in
 * {@code CollectionLogHelperPlugin.shutDown()}. Mirrors the established
 * lifecycle extraction pattern (Waves 11-14) -- the plugin retains state
 * ownership; this routine sequences the teardown calls on the injected
 * collaborators and lets the plugin clear its own private state via the
 * supplied {@code clearPluginState} callback.
 *
 * <h2>Teardown order</h2>
 * The order matches the pre-extraction shutDown body exactly:
 * <ol>
 *   <li>Unregister the {@code !clh} chat command.</li>
 *   <li>Close the authoring logger file handle.</li>
 *   <li>Tear down the panel (if it was constructed).</li>
 *   <li>Remove the nav button from the client toolbar.</li>
 *   <li>Unregister overlays.</li>
 *   <li>Unregister event-bus listeners (scene router, guidance routers,
 *       movement tracker, kill-time tracker).</li>
 *   <li>Reset state on each collaborator
 *       (kill-time tracker, guidance, sync coordinator, kc store,
 *       player location resolver, overlays, data sync state, bank
 *       state, inventory state, slayer state, travel caps, collection state,
 *       plugin data manager).</li>
 *   <li>Shut down the shared HTTP result executor (if present).  The
 *       TempleOSRS syncer no longer manages its own executor (#478, #479)
 *       -- the runtime-provided {@code ScheduledExecutorService} it uses is
 *       shut down by the RuneLite runtime, not by this routine.</li>
 *   <li>Clear plugin-private state via the {@code clearPluginState}
 *       callback -- the plugin owns {@code sourcesWithMissingItems},
 *       {@code pendingPanelRebuild}, {@code rankedSourcesDirty}, and
 *       {@code cachedRankedSources}.</li>
 * </ol>
 *
 * <p>Part of issue #503 -- splitting {@code CollectionLogHelperPlugin} into
 * focused collaborators (Wave 15).
 */
@Slf4j
@Singleton
public class PluginShutdownRoutine
{
	private final ChatCommandManager chatCommandManager;
	private final AuthoringLogger authoringLogger;
	private final SyncModule sync;
	private final ClientToolbar clientToolbar;
	private final OverlayRegistry overlayRegistry;
	private final EventBus eventBus;
	private final SceneEventRouter sceneEventRouter;
	private final GuidanceModule guidance;
	private final EfficiencyModule efficiency;
	private final DataModule data;
	private final PlayerTravelCapabilities travelCapabilities;
	private final PlayerLocationResolver playerLocationResolver;
	private final com.collectionloghelper.player.PohTeleportInventory pohTeleportInventory;
	private final com.collectionloghelper.player.EquippedItemState equippedItemState;
	private final com.collectionloghelper.player.DiaryTierState diaryTierState;
	private final com.collectionloghelper.player.SkillCapePerkState skillCapePerkState;
	private final com.collectionloghelper.player.PlayerQuestProgressState playerQuestProgressState;

	@Inject
	public PluginShutdownRoutine(
		ChatCommandManager chatCommandManager,
		AuthoringLogger authoringLogger,
		SyncModule sync,
		ClientToolbar clientToolbar,
		OverlayRegistry overlayRegistry,
		EventBus eventBus,
		SceneEventRouter sceneEventRouter,
		GuidanceModule guidance,
		EfficiencyModule efficiency,
		DataModule data,
		PlayerTravelCapabilities travelCapabilities,
		PlayerLocationResolver playerLocationResolver,
		com.collectionloghelper.player.PohTeleportInventory pohTeleportInventory,
		com.collectionloghelper.player.EquippedItemState equippedItemState,
		com.collectionloghelper.player.DiaryTierState diaryTierState,
		com.collectionloghelper.player.SkillCapePerkState skillCapePerkState,
		com.collectionloghelper.player.PlayerQuestProgressState playerQuestProgressState)
	{
		this.chatCommandManager = chatCommandManager;
		this.authoringLogger = authoringLogger;
		this.sync = sync;
		this.clientToolbar = clientToolbar;
		this.overlayRegistry = overlayRegistry;
		this.eventBus = eventBus;
		this.sceneEventRouter = sceneEventRouter;
		this.guidance = guidance;
		this.efficiency = efficiency;
		this.data = data;
		this.travelCapabilities = travelCapabilities;
		this.playerLocationResolver = playerLocationResolver;
		this.pohTeleportInventory = pohTeleportInventory;
		this.equippedItemState = equippedItemState;
		this.diaryTierState = diaryTierState;
		this.skillCapePerkState = skillCapePerkState;
		this.playerQuestProgressState = playerQuestProgressState;
	}

	/**
	 * Performs the full plugin teardown. Called once from
	 * {@code CollectionLogHelperPlugin.shutDown()}.
	 *
	 * @param panel the plugin's panel (may be {@code null} if {@code startUp}
	 *              failed before constructing it)
	 * @param navButton the plugin's nav button
	 * @param httpResultExecutor the shared HTTP result executor (may be
	 *                            {@code null} if {@code startUp} failed before
	 *                            creating it)
	 * @param clearPluginState callback that clears plugin-private state
	 *                         ({@code sourcesWithMissingItems},
	 *                         {@code pendingPanelRebuild},
	 *                         {@code rankedSourcesDirty},
	 *                         {@code cachedRankedSources}). Invoked exactly once.
	 */
	public void tearDown(
		CollectionLogHelperPanel panel,
		NavigationButton navButton,
		ExecutorService httpResultExecutor,
		Runnable clearPluginState)
	{
		chatCommandManager.unregisterCommand("clh");
		authoringLogger.close();
		if (panel != null)
		{
			panel.shutDown();
		}
		clientToolbar.removeNavigation(navButton);
		overlayRegistry.unregisterAll();
		eventBus.unregister(sceneEventRouter);
		eventBus.unregister(guidance.getGuidanceEventRouter());
		eventBus.unregister(guidance.getGuidanceMovementTracker());
		eventBus.unregister(efficiency.getKillTimeTracker());
		eventBus.unregister(pohTeleportInventory);
		eventBus.unregister(equippedItemState);
		eventBus.unregister(diaryTierState);
		eventBus.unregister(skillCapePerkState);
		eventBus.unregister(playerQuestProgressState);
		efficiency.getKillTimeTracker().reset();
		guidance.getGuidanceCoordinator().deactivateGuidance();
		sync.getSyncStateCoordinator().reset();
		sync.getSourceKcStore().clear();
		// TempleOsrsKcSyncer no longer owns its executor (#479) -- the shared
		// ScheduledExecutorService is now lifecycle-managed by the RuneLite
		// runtime, so no per-class shutdown call is needed.
		if (httpResultExecutor != null)
		{
			httpResultExecutor.shutdownNow();
		}
		if (clearPluginState != null)
		{
			clearPluginState.run();
		}
		playerLocationResolver.reset();
		guidance.getGuidanceOverlay().setShowCollectionLogReminder(false);
		guidance.getGuidanceOverlay().setShowBankReminder(false);
		data.getDataSyncState().reset();
		data.getPlayerBankState().reset();
		data.getPlayerInventoryState().reset();
		data.getSlayerTaskState().reset();
		travelCapabilities.reset();
		data.getCollectionState().clearState();
		data.getPluginDataManager().reset();
	}
}
