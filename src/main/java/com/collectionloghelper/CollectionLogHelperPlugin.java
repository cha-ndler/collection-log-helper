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
package com.collectionloghelper;

import com.collectionloghelper.chat.ChatEventHandler;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.sync.CollectionLogNetImportOrchestrator;
import com.collectionloghelper.sync.LootSyncManager;
import com.collectionloghelper.sync.TempleSyncOrchestrator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.lifecycle.AuthoringLogger;
import com.collectionloghelper.lifecycle.CollectionStateChangeHandler;
import com.collectionloghelper.lifecycle.OverlayRegistry;
import com.collectionloghelper.lifecycle.PlayerLocationResolver;
import com.collectionloghelper.lifecycle.SceneEventRouter;
import com.collectionloghelper.lifecycle.SyncStateCoordinator;
import com.collectionloghelper.lifecycle.VarbitChangeRouter;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Collection Log Helper",
	description = "Guides players through efficient collection log completion with overlays and efficiency scoring",
	tags = {"collection", "clog", "slayer", "clues", "efficiency", "guide"}
)
public class CollectionLogHelperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private CollectionLogHelperConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatCommandManager chatCommandManager;

	@Inject
	private DataModule data;

	@Inject
	private EfficiencyModule efficiency;

	@Inject
	private RequirementsChecker requirementsChecker;

	@Inject
	private GuidanceModule guidance;

	@Inject
	private PlayerTravelCapabilities travelCapabilities;

	@Inject
	private OverlayRegistry overlayRegistry;

	@Inject
	private SceneEventRouter sceneEventRouter;

	@Inject
	private AuthoringLogger authoringLogger;

	@Inject
	private SyncModule sync;

	@Inject
	private LootSyncManager lootSyncManager;

	@Inject
	private TempleSyncOrchestrator templeSyncOrchestrator;

	@Inject
	private CollectionLogNetImportOrchestrator collectionLogNetImportOrchestrator;

	@Inject
	private ChatEventHandler chatEventHandler;

	@Inject
	private PlayerLocationResolver playerLocationResolver;

	@Inject
	private VarbitChangeRouter varbitChangeRouter;

	@Inject
	private CollectionStateChangeHandler collectionStateChangeHandler;


	private CollectionLogHelperPanel panel;
	private NavigationButton navButton;

	/** Cached set of source names that have at least one unobtained item. Rebuilt when collection state changes. */
	private Set<String> sourcesWithMissingItems = new HashSet<>();
	private volatile boolean pendingRequirementsRefresh = false;
	private volatile boolean pendingTravelVarbitRefresh = false;
	private BufferedImage collectionLogIcon;

	/**
	 * Daemon executor used to wait on async sync futures (collectionlog.net import,
	 * TempleOSRS KC sync) off the EDT/client thread. Lazily created in startUp and
	 * shut down in shutDown; one shared instance prevents the per-click executor
	 * leak that the ad-hoc {@code Executors.newSingleThreadExecutor()} pattern caused.
	 */
	private ExecutorService httpResultExecutor;

	private boolean slayerRefreshPending;

	/** Coalesces multiple panel.rebuild() calls into a single rebuild per game tick. */
	private boolean pendingPanelRebuild;

	/** Cached ranked efficiency list — recomputed only when dirty. */
	private List<ScoredItem> cachedRankedSources;
	private boolean rankedSourcesDirty = true;

	@Override
	protected void startUp() throws Exception
	{
		data.getDatabase().load();
		data.getSlayerMasterDatabase().load();

		httpResultExecutor = Executors.newSingleThreadExecutor(r ->
		{
			Thread t = new Thread(r, "clh-http-result-waiter");
			t.setDaemon(true);
			return t;
		});

		panel = new CollectionLogHelperPanel(
			config, data.getDatabase(), data.getCollectionState(), efficiency.getCalculator(), efficiency.getClueEstimator(),
			itemManager, requirementsChecker, data.getDataSyncState(), data.getSlayerTaskState(),
			efficiency.getSlayerStrategyCalculator(), data.getPlayerInventoryState(), data.getPlayerBankState(),
			efficiency.getDryStreakAnalyzer(),
			(java.util.function.BiConsumer<CollectionLogSource, Integer>) this::activateGuidance,
			this::deactivateGuidance,
			filter -> configManager.setConfiguration("collectionloghelper", "afkFilter", filter.name()),
			sort -> configManager.setConfiguration("collectionloghelper", "efficientSortMode", sort.name()));
		panel.setMode(config.defaultMode());
		// Route step-advance, skip, reset, and sync through the client thread
		// so overlay rescans fire in the same game-frame as auto-completion
		// events. Reset and Sync require RequirementsChecker / inventory state
		// that are client-thread-only, matching the pattern established in
		// commits c528d0ae and cha-ndler/collection-log-helper#409.
		panel.setStepCallbacks(
			() -> clientThread.invokeLater(guidance.getGuidanceSequencer()::advanceStep),
			() -> clientThread.invokeLater(guidance.getGuidanceSequencer()::skipStep),
			() -> clientThread.invokeLater(guidance.getGuidanceSequencer()::restartFromStep0),
			() -> clientThread.invokeLater(guidance.getGuidanceSequencer()::syncToCurrentState)
		);

		// Wire collectionlog.net import callback — delegates to the orchestrator
		// which resolves the player name, dispatches the import, and posts the
		// result back through the panel.
		panel.setCollectionLogNetImportCallback(
			() -> collectionLogNetImportOrchestrator.requestImport(panel, httpResultExecutor));

		// Wire TempleOSRS KC sync button callback
		panel.setTempleSyncCallback(() -> templeSyncOrchestrator.requestSync(panel, httpResultExecutor));

		// Wire coordinator with references it needs from the plugin
		guidance.getGuidanceCoordinator().setPluginInstance(this);
		guidance.getGuidanceCoordinator().setPanel(panel);
		collectionStateChangeHandler.setCallbacks(
			() ->
			{
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			},
			this::getRankedSources,
			this::activateGuidance);
		guidance.getGuidanceCoordinator().setOnSequenceCompleteCallback(
			collectionStateChangeHandler::handleSequenceComplete);
		// Wire coordinator into resolver (post-construction, avoids circular injection)
		guidance.getRequiredItemResolver().setCoordinator(guidance.getGuidanceCoordinator());

		// Use a placeholder icon initially, then swap to the real item sprite once loaded
		final BufferedImage placeholder = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Collection Log Helper")
			.icon(placeholder)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		// AsyncBufferedImage may be blank at startup; onLoaded rebuilds the nav button
		collectionLogIcon = itemManager.getImage(ItemID.COLLECTION_LOG);
		guidance.getGuidanceCoordinator().setCollectionLogIcon(collectionLogIcon);
		((net.runelite.client.util.AsyncBufferedImage) collectionLogIcon).onLoaded(() ->
		{
			clientToolbar.removeNavigation(navButton);
			navButton = NavigationButton.builder()
				.tooltip("Collection Log Helper")
				.icon(collectionLogIcon)
				.priority(6)
				.panel(panel)
				.build();
			clientToolbar.addNavigation(navButton);
		});
		overlayRegistry.registerAll();
		sceneEventRouter.setAuthoringLogger(msg -> authoringLogger.log("%s", msg));
		eventBus.register(sceneEventRouter);
		guidance.getGuidanceEventRouter().setMissingItemsSupplier(() -> sourcesWithMissingItems);
		guidance.getGuidanceEventRouter().setActivateGuidanceCallback(
			(java.util.function.Consumer<CollectionLogSource>) this::activateGuidance);
		guidance.getGuidanceEventRouter().setOnFilterConfigChanged(this::onFilterConfigChanged);
		guidance.getGuidanceEventRouter().setOnSyncConfigChanged(this::onSyncConfigChanged);
		eventBus.register(guidance.getGuidanceEventRouter());
		eventBus.register(guidance.getGuidanceMovementTracker());
		eventBus.register(efficiency.getKillTimeTracker());

		// If already logged in (e.g., plugin enabled mid-session), load state
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				data.getCollectionState().refreshVarps();
				data.getCollectionState().loadObtainedItems();
				data.getCollectionState().captureRecentItems();
				requirementsChecker.refreshAccessibility(data.getDatabase().getAllSources());
				travelCapabilities.refreshQuestState();
				travelCapabilities.refreshVarbits();
				sync.getSyncStateCoordinator().setLastObtainedCount(data.getCollectionState().getTotalObtained());
				sourcesWithMissingItems = collectionStateChangeHandler.rebuildSourcesWithMissingItems();
				if (panel != null)
				{
					panel.rebuild();
				}
			});
		}

		chatEventHandler.setCallbacks(
			this::getRankedSources,
			() ->
			{
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			});
		varbitChangeRouter.setCallbacks(
			() ->
			{
				pendingRequirementsRefresh = true;
				pendingTravelVarbitRefresh = true;
			},
			() -> sourcesWithMissingItems = collectionStateChangeHandler.rebuildSourcesWithMissingItems(),
			() ->
			{
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			});
		chatCommandManager.registerCommand("clh", chatEventHandler::onClhCommand);
		log.info("Collection Log Helper started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		chatCommandManager.unregisterCommand("clh");
		authoringLogger.close();
		sync.getCollectionLogNetImporter().shutdown();
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
		efficiency.getKillTimeTracker().reset();
		deactivateGuidance();
		sync.getSyncStateCoordinator().reset();
		sync.getSourceKcStore().clear();
		sync.getTempleOsrsKcSyncer().shutdown();
		if (httpResultExecutor != null)
		{
			httpResultExecutor.shutdownNow();
			httpResultExecutor = null;
		}
		sourcesWithMissingItems.clear();
		pendingPanelRebuild = false;
		rankedSourcesDirty = true;
		cachedRankedSources = null;
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

		log.info("Collection Log Helper stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// LOGGED_IN fires multiple times during login/transitions.
			// Only reset sync state on the first fire to avoid clearing
			// collection log / bank sync flags mid-session.
			sync.getSyncStateCoordinator().onGameStateLoggedIn();
			slayerRefreshPending = true;
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
				sourcesWithMissingItems = collectionStateChangeHandler.rebuildSourcesWithMissingItems();

				// Rescan scene for tracked objects after scene (re)load
				guidance.getObjectHighlightOverlay().rescanScene();

				// Per-character dir and cache-fresh check are handled in
				// onGameTick once varps and player name are available.

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
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			data.getCollectionState().clearState();
			requirementsChecker.clearCache();
			efficiency.getClueEstimator().resetBucket();
			data.getSlayerTaskState().reset();
			sync.getSyncStateCoordinator().onGameStateLoginScreen();
			sync.getSyncStateCoordinator().setLastObtainedCount(-1);
			sourcesWithMissingItems.clear();
			slayerRefreshPending = false;
			pendingTravelVarbitRefresh = false;
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

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// RS profile is now available — pre-load cached obtained items so they're
		// ready when onGameTick performs the cache-fresh check (varps aren't
		// available yet during this event, so the full check happens in onGameTick).
		data.getCollectionState().loadObtainedItems();
		data.getCollectionState().captureRecentItems();
		log.debug("RuneScapeProfileChanged — loaded {} obtained items from RS profile",
			data.getCollectionState().getTotalObtained());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		varbitChangeRouter.handle(event);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		efficiency.getClueEstimator().resetBucket();
		pendingRequirementsRefresh = true;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		chatEventHandler.handleChatMessage(event);
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		lootSyncManager.handleNpcLoot(event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		lootSyncManager.handleItemContainerChanged(event, panel);
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		ScriptEvent se = event.getScriptEvent();
		sync.getSyncStateCoordinator().onScriptPreFired(event.getScriptId(), se != null ? se.getArguments() : null);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Debounced requirements refresh — flagged by onVarbitChanged, runs once per tick
		if (pendingRequirementsRefresh)
		{
			pendingRequirementsRefresh = false;
			boolean reqsChanged = requirementsChecker.refreshAccessibility(data.getDatabase().getAllSources());
			if (reqsChanged && !sync.getSyncStateCoordinator().isScriptScanActive())
			{
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			}
		}

		// Debounced travel varbit refresh — flagged by onVarbitChanged, runs once per tick
		if (pendingTravelVarbitRefresh)
		{
			pendingTravelVarbitRefresh = false;
			travelCapabilities.refreshVarbits();
		}

		// Lazily init per-character data directory once player name is available
		if (data.getPluginDataManager().getCharacterDir() == null)
		{
			if (data.getPluginDataManager().init())
			{
				efficiency.getKillTimeTracker().init(data.getPluginDataManager().getCharacterDir());
			}
		}

		// Dispatch deferred ShortestPath "path" and world map arrow via coordinator
		guidance.getGuidanceCoordinator().tick();

		// Cache player location for guidance sequencer and authoring log (client thread only).
		// PlayerLocationResolver handles instanced regions and non-top-level WorldViews
		// (e.g. sailing boats), translating to overworld coords so ARRIVE_AT_TILE checks
		// match the static JSON tile.
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

		// Deferred slayer task refresh — varps may not be loaded in the initial
		// invokeLater after LOGGED_IN, so re-read a few ticks later when the
		// server has definitely sent all varp data.
		if (slayerRefreshPending && sync.getSyncStateCoordinator().getLoginTickDelay() <= 7)
		{
			slayerRefreshPending = false;
			data.getSlayerTaskState().refresh();
			pendingPanelRebuild = true;
			rankedSourcesDirty = true;
		}

		// Delegate all remaining sync-lifecycle logic to the coordinator
		SyncStateCoordinator.SyncTickResult syncResult = sync.getSyncStateCoordinator().tickSync(
			panel,
			() -> {
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			},
			collectionStateChangeHandler::exportEfficiencyIfEnabled
		);
		if (syncResult == SyncStateCoordinator.SyncTickResult.RANKED_DIRTY)
		{
			rankedSourcesDirty = true;
		}

		// Single coalesced rebuild per tick — all event handlers and checks above
		// set pendingPanelRebuild instead of calling panel.rebuild() directly.
		if (pendingPanelRebuild && panel != null)
		{
			pendingPanelRebuild = false;
			panel.rebuild();
		}
	}

	/**
	 * Activates guidance for {@code source}, ensuring all work runs on the client
	 * thread.  {@link com.collectionloghelper.guidance.GuidanceOverlayCoordinator#activateGuidance}
	 * transitively calls {@link com.collectionloghelper.data.RequirementsChecker#buildRequirementRows},
	 * which uses {@code client.getQuestState} and similar client-thread-only APIs.
	 * Wrapping here (rather than inside the coordinator) covers all call paths —
	 * both the Item Detail "Guide Me" button
	 * ({@link com.collectionloghelper.ui.CollectionLogHelperPanel}) and the Top Pick
	 * green button ({@link com.collectionloghelper.ui.widget.QuickGuidePanelView}) — in
	 * one place, mirroring the step-advance / skip fix in commit c528d0ae.
	 *
	 * <p>This method returns immediately; activation completes on the next client-thread
	 * tick.  The panel's "Stop Guidance" button state may lag by one tick — this is
	 * acceptable and consistent with the step-advance pattern.
	 */
	public void activateGuidance(CollectionLogSource source)
	{
		activateGuidance(source, null);
	}

	/**
	 * Activates guidance for {@code source} targeting a specific collection-log item.
	 * The {@code targetItemId} is stored on the coordinator so
	 * {@link RequiredItemResolver} can select the correct per-item consumable
	 * override (e.g. the correct shade tier for Shades of Mort'ton).
	 *
	 * <p>Pass {@code null} when the call site has no specific item context.
	 *
	 * <p>All work runs on the client thread (same rationale as the no-arg overload).
	 */
	public void activateGuidance(CollectionLogSource source, @Nullable Integer targetItemId)
	{
		// Route through the client thread: RequirementsChecker.buildRequirementRows
		// and related coordinator work require client-thread context.  Mirrors the
		// step-advance / skip wrap added in commit c528d0ae.
		clientThread.invokeLater(() -> guidance.getGuidanceCoordinator().activateGuidance(
			source, playerLocationResolver.getCachedLocation(), targetItemId));
	}

	/**
	 * Callback invoked by {@link com.collectionloghelper.lifecycle.GuidanceEventRouter}
	 * when a filter-affecting config key changes mid-session. Marks the
	 * ranked-sources cache dirty and the panel-rebuild flag so the next
	 * game-tick coalesces a single rebuild against the new filter state.
	 *
	 * <p>Closes cha-ndler/collection-log-helper#364 (Hide Locked Content
	 * + sibling filter toggles previously stayed inert because nothing
	 * listened for config changes).
	 */
	private void onFilterConfigChanged()
	{
		rankedSourcesDirty = true;
		pendingPanelRebuild = true;
	}

	/**
	 * Invoked by {@link GuidanceEventRouter} when one of the sync-related
	 * config toggles ({@code enableCollectionLogNetImport} or
	 * {@code enableTempleOsrsSync}) changes. Refreshes the visibility of the
	 * corresponding panel button so toggling the checkbox makes the button
	 * appear/disappear immediately, without a panel restart.
	 *
	 * <p>Closes cha-ndler/collection-log-helper#488 — previously the buttons
	 * were created and added to the panel but their visibility was set only
	 * once (in the panel constructor), so toggling the config did nothing
	 * visible until the panel was rebuilt for some other reason.
	 */
	private void onSyncConfigChanged()
	{
		panel.updateCollectionLogNetImportButton();
		panel.updateTempleSyncButtonVisibility();
	}

	/**
	 * Returns a cached ranked efficiency list, recomputing only when the dirty flag is set.
	 */
	private List<ScoredItem> getRankedSources()
	{
		if (rankedSourcesDirty || cachedRankedSources == null)
		{
			cachedRankedSources = efficiency.getCalculator().rankByEfficiency();
			rankedSourcesDirty = false;
		}
		return cachedRankedSources;
	}

	public void deactivateGuidance()
	{
		guidance.getGuidanceCoordinator().deactivateGuidance();
	}

	@Provides
	CollectionLogHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogHelperConfig.class);
	}
}
