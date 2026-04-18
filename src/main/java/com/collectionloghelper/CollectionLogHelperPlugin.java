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
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Collection Log Helper",
	description = "Guides players through efficient collection log completion",
	tags = {"collection", "log", "helper", "efficiency", "guide"}
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
	private DropRateDatabase database;

	@Inject
	private PlayerCollectionState collectionState;

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
	private DataSyncState dataSyncState;

	@Inject
	private PlayerBankState playerBankState;

	@Inject
	private PlayerInventoryState playerInventoryState;

	@Inject
	private SlayerTaskState slayerTaskState;

	@Inject
	private SlayerMasterDatabase slayerMasterDatabase;

	@Inject
	private SlayerStrategyCalculator slayerStrategyCalculator;

	@Inject
	private PlayerTravelCapabilities travelCapabilities;

	@Inject
	private com.collectionloghelper.data.PluginDataManager pluginDataManager;

	@Inject
	private GuidanceSequencer guidanceSequencer;

	@Inject
	private GuidanceOverlayCoordinator guidanceCoordinator;

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


	private CollectionLogHelperPanel panel;
	private NavigationButton navButton;

	/** Cached set of source names that have at least one unobtained item. Rebuilt when collection state changes. */
	private Set<String> sourcesWithMissingItems = new HashSet<>();
	private volatile boolean pendingRequirementsRefresh = false;
	private volatile boolean pendingTravelVarbitRefresh = false;
	private BufferedImage collectionLogIcon;

	// Deferred cache-fresh check: set once the full sync completes
	private boolean hasCompletedFullSync;

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
		database.load();
		slayerMasterDatabase.load();

		panel = new CollectionLogHelperPanel(
			config, database, collectionState, calculator, clueEstimator,
			itemManager, requirementsChecker, dataSyncState, slayerTaskState,
			slayerStrategyCalculator, playerInventoryState, playerBankState,
			this::activateGuidance, this::deactivateGuidance,
			filter -> configManager.setConfiguration("collectionloghelper", "afkFilter", filter.name()),
			sort -> configManager.setConfiguration("collectionloghelper", "efficientSortMode", sort.name()));
		panel.setMode(config.defaultMode());
		// Route step-advance and skip through the client thread so overlay
		// rescans (e.g. ObjectHighlightOverlay.rescanScene) fire in the same
		// game-frame as auto-completion events rather than the next tick.
		panel.setStepCallbacks(
			() -> clientThread.invokeLater(guidanceSequencer::advanceStep),
			() -> clientThread.invokeLater(guidanceSequencer::skipStep)
		);

		// Wire coordinator with references it needs from the plugin
		guidanceCoordinator.setPluginInstance(this);
		guidanceCoordinator.setPanel(panel);
		guidanceCoordinator.setOnSequenceCompleteCallback(this::onSequenceComplete);

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
		guidanceEventRouter.setActivateGuidanceCallback(this::activateGuidance);
		eventBus.register(guidanceEventRouter);

		// If already logged in (e.g., plugin enabled mid-session), load state
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
				collectionState.captureRecentItems();
				requirementsChecker.refreshAccessibility(database.getAllSources());
				travelCapabilities.refreshQuestState();
				travelCapabilities.refreshVarbits();
				syncStateCoordinator.setLastObtainedCount(collectionState.getTotalObtained());
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
		if (panel != null)
		{
			panel.shutDown();
		}
		clientToolbar.removeNavigation(navButton);
		overlayRegistry.unregisterAll();
		eventBus.unregister(sceneEventRouter);
		eventBus.unregister(guidanceEventRouter);
		deactivateGuidance();
		syncStateCoordinator.reset();
		sourcesWithMissingItems.clear();
		pendingPanelRebuild = false;
		rankedSourcesDirty = true;
		cachedRankedSources = null;
		cachedPlayerLocation = null;
		guidanceOverlay.setShowCollectionLogReminder(false);
		guidanceOverlay.setShowBankReminder(false);
		dataSyncState.reset();
		playerBankState.reset();
		playerInventoryState.reset();
		slayerTaskState.reset();
		travelCapabilities.reset();
		collectionState.clearState();
		pluginDataManager.reset();

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
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
				collectionState.captureRecentItems();
				requirementsChecker.refreshAccessibility(database.getAllSources());
				travelCapabilities.refreshQuestState();
				travelCapabilities.refreshVarbits();
				slayerTaskState.refresh();
				syncStateCoordinator.setLastObtainedCount(collectionState.getTotalObtained());
				rebuildSourcesWithMissingItems();

				// Rescan scene for tracked objects after scene (re)load
				objectHighlightOverlay.rescanScene();

				// Per-character dir and cache-fresh check are handled in
				// onGameTick once varps and player name are available.

				if (panel != null)
				{
					if (dataSyncState.isCollectionLogSynced()
						&& collectionState.getTotalObtained() > 0)
					{
						panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
							collectionState.getTotalObtained());
					}
					else
					{
						panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.NOT_SYNCED, 0);
					}
					panel.updateDataSyncWarning();
					if (dataSyncState.isBankScanned())
					{
						panel.updateClueSummary(playerBankState);
					}
					panel.rebuild();
				}
			});
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			collectionState.clearState();
			requirementsChecker.clearCache();
			clueEstimator.resetBucket();
			slayerTaskState.reset();
			syncStateCoordinator.onGameStateLoginScreen();
			syncStateCoordinator.setLastObtainedCount(-1);
			sourcesWithMissingItems.clear();
			slayerRefreshPending = false;
			pendingTravelVarbitRefresh = false;
			cachedPlayerLocation = null;
			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
			dataSyncState.reset();
			playerBankState.reset();
			playerInventoryState.reset();
			travelCapabilities.reset();
			pluginDataManager.reset();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// RS profile is now available — pre-load cached obtained items so they're
		// ready when onGameTick performs the cache-fresh check (varps aren't
		// available yet during this event, so the full check happens in onGameTick).
		collectionState.loadObtainedItems();
		collectionState.captureRecentItems();
		log.debug("RuneScapeProfileChanged — loaded {} obtained items from RS profile",
			collectionState.getTotalObtained());
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Runs on the client thread — safe to call client API
		collectionState.refreshVarps();

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
		boolean wasActive = slayerTaskState.isTaskActive();
		String oldCreature = slayerTaskState.getCreatureName();
		int oldRemaining = slayerTaskState.getRemaining();
		slayerTaskState.refresh();

		boolean slayerChanged = wasActive != slayerTaskState.isTaskActive()
			|| (slayerTaskState.getCreatureName() != null
				&& !slayerTaskState.getCreatureName().equals(oldCreature))
			|| slayerTaskState.getRemaining() != oldRemaining;

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

		int currentCount = collectionState.getTotalObtained();
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
			CollectionLogItem item = database.getItemByName(itemName);
			if (item != null)
			{
				collectionState.markItemObtained(item.getItemId());
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
		for (ItemStack itemStack : event.getItems())
		{
			int itemId = itemStack.getId();
			CollectionLogItem item = database.getItemById(itemId);
			if (item != null && !collectionState.isItemObtained(itemId))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=00c8c8>[Collection Log Helper]</col> Collection log drop: " + item.getName(),
					null);
				guidanceSequencer.onItemObtained(itemId);
			}
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		log.debug("ItemContainerChanged fired: containerId={}, BANK={}, match={}",
			event.getContainerId(), InventoryID.BANK, event.getContainerId() == InventoryID.BANK);

		if (config.guidanceAuthoring())
		{
			authoringLogger.logContainerChange(event, client);
		}

		if (event.getContainerId() == InventoryID.INV)
		{
			net.runelite.api.ItemContainer invContainer = event.getItemContainer();
			if (invContainer != null)
			{
				playerInventoryState.scanInventory(invContainer);

				if (guidanceSequencer.isActive())
				{
					guidanceSequencer.onInventoryChanged();
					guidanceCoordinator.onItemContainerChanged();
				}
			}
		}

		if (event.getContainerId() == InventoryID.WORN)
		{
			net.runelite.api.ItemContainer equipContainer = event.getItemContainer();
			if (equipContainer != null)
			{
				playerInventoryState.scanEquipment(equipContainer);
			}
		}

		if (event.getContainerId() == InventoryID.BANK)
		{
			// Scan bank for clue-related items and travel teleports every time it updates
			net.runelite.api.ItemContainer bankContainer = event.getItemContainer();
			if (bankContainer != null)
			{
				playerBankState.scanBank(bankContainer);
				travelCapabilities.scanBank(bankContainer);
			}

			if (!dataSyncState.isBankScanned())
			{
				dataSyncState.setBankScanned(true);
				guidanceOverlay.setShowBankReminder(false);
				log.info("Bank opened — marked as scanned for this session");
			}

			if (panel != null)
			{
				panel.updateDataSyncWarning();
				panel.updateClueSummary(playerBankState);
			}
		}
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
			boolean reqsChanged = requirementsChecker.refreshAccessibility(database.getAllSources());
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
		if (pluginDataManager.getCharacterDir() == null)
		{
			pluginDataManager.init();
		}

		// Deferred cache-fresh check: varps aren't loaded during LOGGED_IN or
		// RuneScapeProfileChanged, so retry here once totalObtained becomes valid
		if (!hasCompletedFullSync && collectionState.getTotalObtained() > 0
			&& collectionState.isCacheFresh())
		{
			dataSyncState.setCollectionLogSynced(true);
			hasCompletedFullSync = true;
			log.info("Cache is fresh (varp {} matches last sync) — skipping sync prompt",
				collectionState.getTotalObtained());

			if (playerBankState.loadFromCache())
			{
				dataSyncState.setBankScanned(true);
				log.info("Bank cache loaded — skipping bank scan prompt");
			}

			exportEfficiencyIfEnabled();

			if (panel != null)
			{
				panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
					collectionState.getTotalObtained());
				panel.updateDataSyncWarning();
			}
			pendingPanelRebuild = true;
			rankedSourcesDirty = true;

			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
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
			slayerTaskState.refresh();
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
		java.io.File exportFile = pluginDataManager.getFile("efficiency-export.txt");
		if (exportFile == null)
		{
			// Fallback if player name not yet available
			exportFile = new java.io.File(
				net.runelite.client.RuneLite.RUNELITE_DIR, "collection-log-efficiency-export.txt");
		}
		calculator.exportEfficiencyList(exportFile, client);
	}

	public void activateGuidance(CollectionLogSource source)
	{
		guidanceCoordinator.activateGuidance(source, cachedPlayerLocation);
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
			// Auto-activate guidance for the next top efficiency pick
			List<ScoredItem> ranked = getRankedSources();
			ranked.stream()
				.filter(s -> !s.isLocked())
				.findFirst()
				.ifPresent(topPick -> activateGuidance(topPick.getSource()));
		}
	}

	public void deactivateGuidance()
	{
		guidanceCoordinator.deactivateGuidance();
	}

	/**
	 * Rebuilds the cached set of source names that have at least one unobtained item.
	 * Called when collection state changes to avoid per-menu-entry item scanning.
	 */
	private void rebuildSourcesWithMissingItems()
	{
		sourcesWithMissingItems = guidanceCoordinator.rebuildSourcesWithMissingItems(
			database, collectionState);
	}

	/**
	 * Resolve the player's real-world location, transforming boat-local
	 * coordinates when the player is inside a sailing WorldEntity.
	 *
	 * When sailing, the player's WorldView is the boat's inner view (not
	 * top-level).  We find the owning WorldEntity and use
	 * {@code transformToMainWorld} to map the player's local point back to
	 * overworld coordinates.
	 *
	 * Falls back to the plain {@code getWorldLocation()} if the WorldEntity
	 * API is unavailable (older RuneLite) or on any error.
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
			log.debug("WorldEntity transform failed, using fallback location", e);
		}
		return fallback;
	}

	private void onClhCommand(ChatMessage chatMessage, String message)
	{
		int obtained = collectionState.getTotalObtained();
		int total = collectionState.getTotalPossible();
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
