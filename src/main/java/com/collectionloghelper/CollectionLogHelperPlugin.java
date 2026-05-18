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

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.sync.CollectionLogNetImporter;
import com.collectionloghelper.sync.ImportResult;
import com.collectionloghelper.sync.LootSyncManager;
import com.collectionloghelper.sync.SourceKcStore;
import com.collectionloghelper.sync.SyncResult;
import com.collectionloghelper.sync.TempleOsrsKcSyncer;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.learning.DryStreakAnalyzer;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import com.collectionloghelper.guidance.RequiredItemResolver;
import com.collectionloghelper.learning.KillTimeTracker;
import com.collectionloghelper.lifecycle.AuthoringLogger;
import com.collectionloghelper.lifecycle.GuidanceEventRouter;
import com.collectionloghelper.lifecycle.GuidanceUIState;
import com.collectionloghelper.lifecycle.OverlayRegistry;
import com.collectionloghelper.lifecycle.SceneEventRouter;
import com.collectionloghelper.lifecycle.SyncStateCoordinator;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
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
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Collection Log Helper",
	description = "Guides players through efficient collection log completion with overlays and efficiency scoring",
	tags = {"collection", "clog", "slayer", "clues", "efficiency", "guide"}
)
public class CollectionLogHelperPlugin extends Plugin
{
	private static final Pattern COLLECTION_LOG_PATTERN =
		Pattern.compile("New item added to your collection log: (.*)");

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
	private EfficiencyCalculator calculator;

	@Inject
	private ClueCompletionEstimator clueEstimator;

	@Inject
	private RequirementsChecker requirementsChecker;

	@Inject
	private GuidanceOverlay guidanceOverlay;

	@Inject
	private ObjectHighlightOverlay objectHighlightOverlay;

	@Inject
	private SlayerStrategyCalculator slayerStrategyCalculator;

	@Inject
	private DryStreakAnalyzer dryStreakAnalyzer;

	@Inject
	private PlayerTravelCapabilities travelCapabilities;

	@Inject
	private GuidanceSequencer guidanceSequencer;

	@Inject
	private GuidanceOverlayCoordinator guidanceCoordinator;

	@Inject
	private RequiredItemResolver requiredItemResolver;

	@Inject
	private OverlayRegistry overlayRegistry;

	@Inject
	private SceneEventRouter sceneEventRouter;

	@Inject
	private AuthoringLogger authoringLogger;

	@Inject
	private SyncStateCoordinator syncStateCoordinator;

	@Inject
	private GuidanceUIState guidanceUIState;

	@Inject
	private GuidanceEventRouter guidanceEventRouter;

	@Inject
	private com.collectionloghelper.guidance.GuidanceMovementTracker guidanceMovementTracker;

	@Inject
	private CollectionLogNetImporter collectionLogNetImporter;

	@Inject
	private KillTimeTracker killTimeTracker;

	@Inject
	private TempleOsrsKcSyncer templeOsrsKcSyncer;

	@Inject
	private SourceKcStore sourceKcStore;

	@Inject
	private LootSyncManager lootSyncManager;


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

	/**
	 * Player location cached on the client thread each game tick.
	 * Read by guidance sequencer and authoring log — volatile ensures visibility.
	 */
	private volatile WorldPoint cachedPlayerLocation;

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
			config, data.getDatabase(), data.getCollectionState(), calculator, clueEstimator,
			itemManager, requirementsChecker, data.getDataSyncState(), data.getSlayerTaskState(),
			slayerStrategyCalculator, data.getPlayerInventoryState(), data.getPlayerBankState(),
			dryStreakAnalyzer,
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
			() -> clientThread.invokeLater(guidanceSequencer::advanceStep),
			() -> clientThread.invokeLater(guidanceSequencer::skipStep),
			() -> clientThread.invokeLater(guidanceSequencer::restartFromStep0),
			() -> clientThread.invokeLater(guidanceSequencer::syncToCurrentState)
		);

		// Wire collectionlog.net import callback — fetches username from the logged-in player.
		// Called from the EDT; submits async work and posts the result back via the panel method.
		panel.setCollectionLogNetImportCallback(() ->
		{
			String username = client.getLocalPlayer() != null
				? client.getLocalPlayer().getName()
				: null;
			if (username == null || username.isEmpty())
			{
				panel.onCollectionLogNetImportComplete("Log in first");
				return;
			}
			Future<ImportResult> future =
				collectionLogNetImporter.importProfile(username);
			// Poll the result on the shared daemon executor so the EDT is not blocked
			httpResultExecutor.submit(() ->
			{
				try
				{
					ImportResult result = future.get();
					panel.onCollectionLogNetImportComplete(result.toToastMessage());
					if (result.isSuccess())
					{
						// Trigger a panel rebuild on the EDT
						panel.rebuild();
					}
				}
				catch (Exception e)
				{
					log.warn("collectionlog.net import result waiter failed", e);
					panel.onCollectionLogNetImportComplete("collectionlog.net: error");
				}
			});
		});

		// Wire TempleOSRS KC sync button callback
		panel.setTempleSyncCallback(this::onTempleSyncRequested);

		// Wire coordinator with references it needs from the plugin
		guidanceCoordinator.setPluginInstance(this);
		guidanceCoordinator.setPanel(panel);
		guidanceCoordinator.setOnSequenceCompleteCallback(this::onSequenceComplete);
		// Wire coordinator into resolver (post-construction, avoids circular injection)
		requiredItemResolver.setCoordinator(guidanceCoordinator);

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
		guidanceCoordinator.setCollectionLogIcon(collectionLogIcon);
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
		guidanceEventRouter.setMissingItemsSupplier(() -> sourcesWithMissingItems);
		guidanceEventRouter.setActivateGuidanceCallback(
			(java.util.function.Consumer<CollectionLogSource>) this::activateGuidance);
		guidanceEventRouter.setOnFilterConfigChanged(this::onFilterConfigChanged);
		guidanceEventRouter.setOnSyncConfigChanged(this::onSyncConfigChanged);
		eventBus.register(guidanceEventRouter);
		eventBus.register(guidanceMovementTracker);
		eventBus.register(killTimeTracker);

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
				syncStateCoordinator.setLastObtainedCount(data.getCollectionState().getTotalObtained());
				rebuildSourcesWithMissingItems();
				if (panel != null)
				{
					panel.rebuild();
				}
			});
		}

		chatCommandManager.registerCommand("clh", this::onClhCommand);
		log.info("Collection Log Helper started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		chatCommandManager.unregisterCommand("clh");
		authoringLogger.close();
		collectionLogNetImporter.shutdown();
		if (panel != null)
		{
			panel.shutDown();
		}
		clientToolbar.removeNavigation(navButton);
		overlayRegistry.unregisterAll();
		eventBus.unregister(sceneEventRouter);
		eventBus.unregister(guidanceEventRouter);
		eventBus.unregister(guidanceMovementTracker);
		eventBus.unregister(killTimeTracker);
		killTimeTracker.reset();
		deactivateGuidance();
		syncStateCoordinator.reset();
		sourceKcStore.clear();
		templeOsrsKcSyncer.shutdown();
		if (httpResultExecutor != null)
		{
			httpResultExecutor.shutdownNow();
			httpResultExecutor = null;
		}
		sourcesWithMissingItems.clear();
		pendingPanelRebuild = false;
		rankedSourcesDirty = true;
		cachedRankedSources = null;
		cachedPlayerLocation = null;
		guidanceOverlay.setShowCollectionLogReminder(false);
		guidanceOverlay.setShowBankReminder(false);
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
			syncStateCoordinator.onGameStateLoggedIn();
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
				syncStateCoordinator.setLastObtainedCount(data.getCollectionState().getTotalObtained());
				rebuildSourcesWithMissingItems();

				// Rescan scene for tracked objects after scene (re)load
				objectHighlightOverlay.rescanScene();

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
			clueEstimator.resetBucket();
			data.getSlayerTaskState().reset();
			syncStateCoordinator.onGameStateLoginScreen();
			syncStateCoordinator.setLastObtainedCount(-1);
			sourcesWithMissingItems.clear();
			slayerRefreshPending = false;
			pendingTravelVarbitRefresh = false;
			cachedPlayerLocation = null;
			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
			data.getDataSyncState().reset();
			data.getPlayerBankState().reset();
			data.getPlayerInventoryState().reset();
			travelCapabilities.reset();
			killTimeTracker.reset();
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
		// Runs on the client thread — safe to call client API
		data.getCollectionState().refreshVarps();

		// Authoring: log varbit changes (throttle to 1 per tick to avoid spam)
		if (config.guidanceAuthoring() && client.getTickCount() != authoringLogger.getLastVarbitLogTick())
		{
			authoringLogger.setLastVarbitLogTick(client.getTickCount());
			authoringLogger.log("VARBIT_CHANGED varbitId=%d value=%d",
				event.getVarbitId(), event.getValue());
		}

		// Forward varbit change to guidance sequencer for VARBIT_AT_LEAST completion
		guidanceSequencer.onVarbitChanged(event.getVarbitId(), event.getValue());

		// Refresh Slayer task state and rebuild if the task changed
		boolean wasActive = data.getSlayerTaskState().isTaskActive();
		String oldCreature = data.getSlayerTaskState().getCreatureName();
		int oldRemaining = data.getSlayerTaskState().getRemaining();
		data.getSlayerTaskState().refresh();

		boolean slayerChanged = wasActive != data.getSlayerTaskState().isTaskActive()
			|| (data.getSlayerTaskState().getCreatureName() != null
				&& !data.getSlayerTaskState().getCreatureName().equals(oldCreature))
			|| data.getSlayerTaskState().getRemaining() != oldRemaining;

		// Flag requirements and travel capabilities for refresh on next game tick (debounced).
		// Quest states are varbits that fire hundreds of times during login,
		// so we can't call refreshAccessibility() here — it scans all sources.
		pendingRequirementsRefresh = true;
		pendingTravelVarbitRefresh = true;

		// Don't trigger rebuilds mid-scan; the settle logic in onGameTick
		// will fire a single rebuild once script 4100 stops firing.
		if (syncStateCoordinator.isScriptScanActive())
		{
			return;
		}

		int currentCount = data.getCollectionState().getTotalObtained();
		if (currentCount != syncStateCoordinator.getLastObtainedCount())
		{
			syncStateCoordinator.onCollectionStateChanged(currentCount);
			rebuildSourcesWithMissingItems();
			slayerChanged = true; // rebuild anyway
		}

		if (slayerChanged)
		{
			pendingPanelRebuild = true;
			rankedSourcesDirty = true;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		clueEstimator.resetBucket();
		pendingRequirementsRefresh = true;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		if (config.guidanceAuthoring())
		{
			authoringLogger.log("CHAT type=%s msg='%s'", event.getType(), event.getMessage());
		}

		// Forward chat messages to guidance sequencer for CHAT_MESSAGE_RECEIVED condition
		if (guidanceSequencer.isActive())
		{
			guidanceSequencer.onChatMessage(Text.removeTags(event.getMessage()));
		}

		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(Text.removeTags(event.getMessage()));
		if (matcher.find())
		{
			String itemName = matcher.group(1);
			log.debug("New collection log item: {}", itemName);

			// O(1) lookup by name instead of scanning all sources × items
			CollectionLogItem item = data.getDatabase().getItemByName(itemName);
			if (item != null)
			{
				data.getCollectionState().markItemObtained(item.getItemId());
				guidanceSequencer.onItemObtained(item.getItemId());
				log.debug("Marked item {} (ID: {}) as obtained", itemName, item.getItemId());
			}

			pendingPanelRebuild = true;
			rankedSourcesDirty = true;
		}
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
		syncStateCoordinator.onScriptPreFired(event.getScriptId(), se != null ? se.getArguments() : null);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Debounced requirements refresh — flagged by onVarbitChanged, runs once per tick
		if (pendingRequirementsRefresh)
		{
			pendingRequirementsRefresh = false;
			boolean reqsChanged = requirementsChecker.refreshAccessibility(data.getDatabase().getAllSources());
			if (reqsChanged && !syncStateCoordinator.isScriptScanActive())
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
				killTimeTracker.init(data.getPluginDataManager().getCharacterDir());
			}
		}

		// Dispatch deferred ShortestPath "path" and world map arrow via coordinator
		guidanceCoordinator.tick();

		// Cache player location for guidance sequencer and authoring log (client thread only).
		// When sailing, the player is inside a WorldEntity whose inner WorldView
		// returns boat-local coords.  Detect this and transform to real-world
		// coordinates via WorldEntity.transformToMainWorld().
		// Ref: LlemonDuck/sailing SailingUtil, RuneLite WorldEntity API
		if (client.getLocalPlayer() != null)
		{
			cachedPlayerLocation = resolvePlayerWorldLocation();
			authoringLogger.setPlayerLocation(cachedPlayerLocation);

			// Check ARRIVE_AT_TILE completion for guidance sequencer
			if (guidanceSequencer.isActive())
			{
				guidanceSequencer.setPlayerLocation(cachedPlayerLocation);
				guidanceSequencer.onPlayerMoved(cachedPlayerLocation);
			}

		}

		// Deferred slayer task refresh — varps may not be loaded in the initial
		// invokeLater after LOGGED_IN, so re-read a few ticks later when the
		// server has definitely sent all varp data.
		if (slayerRefreshPending && syncStateCoordinator.getLoginTickDelay() <= 7)
		{
			slayerRefreshPending = false;
			data.getSlayerTaskState().refresh();
			pendingPanelRebuild = true;
			rankedSourcesDirty = true;
		}

		// Delegate all remaining sync-lifecycle logic to the coordinator
		SyncStateCoordinator.SyncTickResult syncResult = syncStateCoordinator.tickSync(
			panel,
			() -> {
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
			},
			this::exportEfficiencyIfEnabled
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

	private void exportEfficiencyIfEnabled()
	{
		if (!config.exportEfficiencyLog())
		{
			return;
		}
		java.io.File exportFile = data.getPluginDataManager().getFile("efficiency-export.txt");
		if (exportFile == null)
		{
			// Fallback if player name not yet available
			exportFile = new java.io.File(
				net.runelite.client.RuneLite.RUNELITE_DIR, "collection-log-efficiency-export.txt");
		}
		calculator.exportEfficiencyList(exportFile, client);
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
		clientThread.invokeLater(() -> guidanceCoordinator.activateGuidance(
			source, cachedPlayerLocation, targetItemId));
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
			cachedRankedSources = calculator.rankByEfficiency();
			rankedSourcesDirty = false;
		}
		return cachedRankedSources;
	}

	/**
	 * Callback from coordinator when a guidance sequence completes.
	 * Handles auto-advance to next source and panel rebuild.
	 */
	private void onSequenceComplete()
	{
		pendingPanelRebuild = true;
		rankedSourcesDirty = true;
		if (config.autoAdvanceGuidance())
		{
			// Auto-activate guidance for the next top efficiency pick.
			// Pass the best-item target ID (B4.4) so per-item overrides
			// (perItemRequiredItemIds, perItemStepDescription, perItemNpcId)
			// activate automatically without the user right-clicking an item.
			List<ScoredItem> ranked = getRankedSources();
			ranked.stream()
				.filter(s -> !s.isLocked())
				.findFirst()
				.ifPresent(topPick ->
				{
					Integer targetItemId = topPick.getBestItem() != null
						? topPick.getBestItem().getItemId()
						: null;
					activateGuidance(topPick.getSource(), targetItemId);
				});
		}
	}

	public void deactivateGuidance()
	{
		guidanceCoordinator.deactivateGuidance();
	}

	/**
	 * Initiates an asynchronous TempleOSRS KC sync for the current player.
	 *
	 * <p>Triggered by the panel "Sync KC from TempleOSRS" button. The sync runs on
	 * the {@link TempleOsrsKcSyncer} background thread; on completion the result is
	 * applied on the EDT and the panel button is reset.
	 *
	 * <p>Fail-soft: any error is logged and surfaced as a chat message; no exception
	 * reaches the caller.
	 */
	private void onTempleSyncRequested()
	{
		if (!config.enableTempleOsrsSync())
		{
			return;
		}

		String playerName = data.getPluginDataManager().getCurrentPlayerName();
		if (playerName == null || playerName.isEmpty())
		{
			log.warn("TempleOSRS sync requested but player name is not available");
			if (panel != null)
			{
				panel.onTempleSyncComplete(false);
			}
			return;
		}

		log.debug("Requesting TempleOSRS KC sync for '{}'", playerName);

		Future<SyncResult> future = templeOsrsKcSyncer.syncKc(playerName);

		// Wait for the result on the shared daemon executor so the EDT is not blocked
		httpResultExecutor.submit(() ->
		{
			SyncResult result;
			try
			{
				result = future.get(30, TimeUnit.SECONDS);
			}
			catch (Exception e)
			{
				log.warn("TempleOSRS sync future failed for '{}': {}", playerName, e.getMessage());
				result = SyncResult.failure("Sync timed out or failed: " + e.getMessage());
			}

			final SyncResult finalResult = result;
			if (finalResult.isSuccess())
			{
				// Validate mapped names against the DB and apply only known sources
				Map<String, Integer> validated = new java.util.HashMap<>();
				for (Map.Entry<String, Integer> entry : finalResult.getKcBySource().entrySet())
				{
					if (data.getDatabase().getSourceByName(entry.getKey()) != null)
					{
						validated.put(entry.getKey(), entry.getValue());
					}
					else
					{
						log.debug("TempleOSRS KC entry '{}' not found in CLH database - skipping",
							entry.getKey());
					}
				}
				sourceKcStore.update(validated);
				log.info("TempleOSRS KC sync complete: {} sources updated, {} skipped",
					validated.size(), finalResult.getSkippedCount());

				clientThread.invokeLater(() ->
					client.addChatMessage(
						net.runelite.api.ChatMessageType.GAMEMESSAGE, "",
						"<col=00c8c8>[Collection Log Helper]</col> TempleOSRS KC synced: "
							+ validated.size() + " sources updated.",
						null));
			}
			else
			{
				log.warn("TempleOSRS KC sync failed: {}", finalResult.getErrorMessage());
				clientThread.invokeLater(() ->
					client.addChatMessage(
						net.runelite.api.ChatMessageType.GAMEMESSAGE, "",
						"<col=ff0000>[Collection Log Helper]</col> TempleOSRS KC sync failed: "
							+ finalResult.getErrorMessage(),
						null));
			}

			if (panel != null)
			{
				panel.onTempleSyncComplete(finalResult.isSuccess());
			}
		});
	}

	/**
	 * Rebuilds the cached set of source names that have at least one unobtained item.
	 * Called when collection state changes to avoid per-menu-entry item scanning.
	 */
	private void rebuildSourcesWithMissingItems()
	{
		sourcesWithMissingItems = guidanceCoordinator.rebuildSourcesWithMissingItems(
			data.getDatabase(), data.getCollectionState());
	}

	/**
	 * Resolve the player's real-world location, transforming local coordinates
	 * back to template/overworld coordinates so they compare correctly against
	 * the static {@code worldX/worldY} values in {@code drop_rates.json}.
	 *
	 * Three cases:
	 * <ol>
	 *   <li><b>Standard top-level world view</b> — return {@code getWorldLocation()}.</li>
	 *   <li><b>Instanced region</b> (CoX/ToB/ToA, GWD, Vorkath, Royal Titans, many
	 *       quest/clue rooms, etc.) — {@code getWorldLocation()} returns
	 *       instance-template coords that don't match the static JSON tile.
	 *       Translate via {@link WorldPoint#fromLocalInstance(Client, LocalPoint)}
	 *       to recover the overworld coords.</li>
	 *   <li><b>Sailing WorldEntity</b> — player is inside a non-top-level
	 *       WorldView; map the local point back through
	 *       {@code transformToMainWorld}.</li>
	 * </ol>
	 *
	 * Falls back to plain {@code getWorldLocation()} on any error or when the
	 * required API is unavailable.
	 */
	private WorldPoint resolvePlayerWorldLocation()
	{
		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return null;
		}
		WorldPoint fallback = lp.getWorldLocation();
		try
		{
			WorldView playerView = lp.getWorldView();
			if (playerView == null || playerView.isTopLevel())
			{
				// Top-level view — handle instanced regions by translating
				// the player's local point back to the overworld template tile.
				// Without this, ARRIVE_AT_TILE checks in instanced bosses
				// (Royal Titans, raids, etc.) never match the JSON coords.
				if (client.isInInstancedRegion())
				{
					LocalPoint localPoint = lp.getLocalLocation();
					if (localPoint != null)
					{
						WorldPoint templatePoint = WorldPoint.fromLocalInstance(client, localPoint);
						if (templatePoint != null)
						{
							return templatePoint;
						}
					}
				}
				return fallback;
			}

			// Player is inside a non-top-level WorldView (e.g. a sailing boat).
			// Find the WorldEntity whose inner WorldView matches the player's.
			WorldView topLevel = client.getTopLevelWorldView();
			for (WorldEntity entity : topLevel.worldEntities())
			{
				if (entity.getWorldView() == playerView)
				{
					LocalPoint playerLocal = lp.getLocalLocation();
					LocalPoint mainLocal = entity.transformToMainWorld(playerLocal);
					if (mainLocal != null)
					{
						return WorldPoint.fromLocal(topLevel,
							mainLocal.getX(), mainLocal.getY(),
							topLevel.getPlane());
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Player-location resolution failed, using fallback", e);
		}
		return fallback;
	}

	private void onClhCommand(ChatMessage chatMessage, String message)
	{
		int obtained = data.getCollectionState().getTotalObtained();
		int total = data.getCollectionState().getTotalPossible();
		String progressLine;
		if (total > 0)
		{
			double pct = (obtained * 100.0) / total;
			progressLine = String.format("Collection Log: %d/%d (%.1f%%)", obtained, total, pct);
		}
		else
		{
			progressLine = "Collection Log: not synced";
		}

		String guidanceLine;
		if (guidanceSequencer.isActive() && guidanceSequencer.getActiveSource() != null)
		{
			String sourceName = guidanceSequencer.getActiveSource().getName();
			int step = guidanceSequencer.getCurrentIndex() + 1;
			int totalSteps = guidanceSequencer.getTotalSteps();
			guidanceLine = String.format("Guiding: %s step %d/%d", sourceName, step, totalSteps);
		}
		else
		{
			guidanceLine = "No active guidance";
		}

		String topPickLine;
		List<ScoredItem> ranked = getRankedSources();
		ScoredItem topPick = ranked.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.orElse(null);
		if (topPick != null)
		{
			double hours = topPick.getScore() > 0 ? 100.0 / topPick.getScore() : 0;
			String timeStr = ClueCompletionEstimator.formatTime((int) (hours * 3600));
			topPickLine = String.format("Top pick: %s (~%s)", topPick.getSource().getName(), timeStr);
		}
		else
		{
			topPickLine = "Top pick: none available";
		}

		String output = "<col=00c8c8>[Collection Log Helper]</col> "
			+ progressLine + " | " + guidanceLine + " | " + topPickLine;
		clientThread.invokeLater(() ->
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", output, null));
	}

	@Provides
	CollectionLogHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogHelperConfig.class);
	}
}
