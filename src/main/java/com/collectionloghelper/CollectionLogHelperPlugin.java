/*
 * Copyright (c) 2025, Chandler
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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.ItemObjectTier;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceInfoBox;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
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
	private static final int COLLECTION_LOG_GROUP_ID = InterfaceID.Collection.FRAME >> 16;

	/**
	 * Client script 4100 fires once per obtained item when the collection log
	 * enters search mode. Args: [source, itemId, itemCount].
	 * Used by TempleOSRS and WikiSync for full collection log scanning.
	 */
	private static final int SCRIPT_COLLECTION_LOG_ITEM = 4100;

	/** Client script 2240 triggers the collection log search mode. */
	private static final int SCRIPT_COLLECTION_LOG_SEARCH = 2240;

	/** Ticks to wait after the last script 4100 fires before finalizing the scan. */
	private static final int SCAN_SETTLE_TICKS = 3;

	private static final String MENU_OPTION_GUIDE = "Collection Log Guide";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private CollectionLogHelperConfig config;

	@Inject
	private ConfigManager configManager;


	@Inject
	private InfoBoxManager infoBoxManager;

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
	private GuidanceMinimapOverlay guidanceMinimapOverlay;

	@Inject
	private DialogHighlightOverlay dialogHighlightOverlay;

	@Inject
	private ObjectHighlightOverlay objectHighlightOverlay;

	@Inject
	private ItemHighlightOverlay itemHighlightOverlay;

	@Inject
	private WorldMapRouteOverlay worldMapRouteOverlay;

	@Inject
	private GroundItemHighlightOverlay groundItemHighlightOverlay;

	@Inject
	private WidgetHighlightOverlay widgetHighlightOverlay;

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


	private CollectionLogHelperPanel panel;
	private NavigationButton navButton;
	private int lastObtainedCount = -1;

	/** Cached set of source names that have at least one unobtained item. Rebuilt when collection state changes. */
	private Set<String> sourcesWithMissingItems = new HashSet<>();
	private volatile boolean pendingRequirementsRefresh = false;
	private volatile boolean pendingTravelVarbitRefresh = false;
	private boolean clogNotificationChecked = false;
	private boolean collectionLogOpen;
	private BufferedImage collectionLogIcon;
	private CollectionLogWorldMapPoint activeMapPoint;
	private GuidanceInfoBox activeInfoBox;

	// Auto-sync state: triggered when collection log widget opens
	private boolean autoSyncPending;
	private boolean scriptScanActive;
	private int scriptScanItemCount;
	private int scanSettleCountdown;
	private boolean hasCompletedFullSync;
	private boolean syncReminderSent;
	private int loginTickDelay;

	/**
	 * Player location cached on the client thread each game tick.
	 * Read by guidance sequencer and authoring log — volatile ensures visibility.
	 */
	private volatile WorldPoint cachedPlayerLocation;

	/** Tracks the last guidance step index for which a worldMessage was sent (prevents spam on re-notify). */
	private int lastMessagedStepIndex = -1;

	/** Pending ShortestPath target — set after "clear", sent as "path" on the next game tick. */
	private WorldPoint pendingShortestPathTarget;
	private boolean slayerRefreshPending;

	/** Coalesces multiple panel.rebuild() calls into a single rebuild per game tick. */
	private boolean pendingPanelRebuild;

	/** Cached ranked efficiency list — recomputed only when dirty. */
	private List<ScoredItem> cachedRankedSources;
	private boolean rankedSourcesDirty = true;

	/** Tracked NPC for guidance overlay — maintained via NpcSpawned/NpcDespawned events. */
	private volatile NPC trackedGuidanceNpc;

	/** Writer for guidance authoring event log. Opened when authoring mode enabled. */
	private java.io.PrintWriter authoringLogWriter;

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
			filter -> configManager.setConfiguration("collectionloghelper", "afkFilter", filter.name()));
		panel.setMode(config.defaultMode());
		panel.setStepCallbacks(
			() -> guidanceSequencer.advanceStep(),
			() -> guidanceSequencer.skipStep()
		);

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
		overlayManager.add(guidanceOverlay);
		overlayManager.add(guidanceMinimapOverlay);
		overlayManager.add(dialogHighlightOverlay);
		overlayManager.add(objectHighlightOverlay);
		overlayManager.add(itemHighlightOverlay);
		overlayManager.add(worldMapRouteOverlay);
		overlayManager.add(groundItemHighlightOverlay);
		overlayManager.add(widgetHighlightOverlay);

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
				lastObtainedCount = collectionState.getTotalObtained();
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
		if (authoringLogWriter != null)
		{
			authoringLogWriter.close();
			authoringLogWriter = null;
		}
		if (panel != null)
		{
			panel.shutDown();
		}
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(guidanceOverlay);
		overlayManager.remove(guidanceMinimapOverlay);
		overlayManager.remove(dialogHighlightOverlay);
		overlayManager.remove(objectHighlightOverlay);
		overlayManager.remove(itemHighlightOverlay);
		overlayManager.remove(worldMapRouteOverlay);
		overlayManager.remove(groundItemHighlightOverlay);
		overlayManager.remove(widgetHighlightOverlay);
		deactivateGuidance();
		lastObtainedCount = -1;
		sourcesWithMissingItems.clear();
		collectionLogOpen = false;
		autoSyncPending = false;
		scriptScanActive = false;
		scanSettleCountdown = 0;
		hasCompletedFullSync = false;
		syncReminderSent = false;
		loginTickDelay = 0;
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
			boolean freshLogin = loginTickDelay == 0 && !syncReminderSent;
			loginTickDelay = 10;
			slayerRefreshPending = true;
			if (freshLogin)
			{
				dataSyncState.reset();
				dataSyncState.setLoginTimestamp(System.currentTimeMillis());
			}
			clientThread.invokeLater(() ->
			{
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
				collectionState.captureRecentItems();
				requirementsChecker.refreshAccessibility(database.getAllSources());
				travelCapabilities.refreshQuestState();
				travelCapabilities.refreshVarbits();
				slayerTaskState.refresh();
				lastObtainedCount = collectionState.getTotalObtained();
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
			lastObtainedCount = -1;
			sourcesWithMissingItems.clear();
			collectionLogOpen = false;
			autoSyncPending = false;
			scriptScanActive = false;
			scanSettleCountdown = 0;
			hasCompletedFullSync = false;
			syncReminderSent = false;
			clogNotificationChecked = false;
			loginTickDelay = 0;
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
		if (config.guidanceAuthoring() && client.getTickCount() != lastVarbitLogTick)
		{
			lastVarbitLogTick = client.getTickCount();
			authoringLog("VARBIT_CHANGED varbitId=%d value=%d",
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
		if (scriptScanActive)
		{
			return;
		}

		int currentCount = collectionState.getTotalObtained();
		if (currentCount != lastObtainedCount)
		{
			lastObtainedCount = currentCount;
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
	public void onActorDeath(ActorDeath event)
	{
		if (!guidanceSequencer.isActive())
		{
			return;
		}
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			if (config.guidanceAuthoring())
			{
				authoringLog("DEATH npcId=%d name='%s'", npc.getId(), npc.getName());
			}
			guidanceSequencer.onNpcDeath(npc.getId());
		}
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
			authoringLog("CHAT type=%s msg='%s'", event.getType(), event.getMessage());
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
			logContainerChange(event);
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
					applyDynamicItemObjectOverlays();
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
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Capture dialog widget text for authoring mode
		if (config.guidanceAuthoring())
		{
			// Player dialog choices (group 219)
			if (event.getGroupId() == 219)
			{
				clientThread.invokeLater(() ->
				{
					Widget container = client.getWidget(219, 1);
					if (container != null)
					{
						Widget[] children = container.getDynamicChildren();
						if (children != null)
						{
							StringBuilder sb = new StringBuilder("DIALOG_OPTIONS");
							for (Widget child : children)
							{
								if (child != null && child.getText() != null && !child.getText().isEmpty())
								{
									sb.append(" '").append(child.getText()).append("'");
								}
							}
							authoringLog(sb.toString());
						}
					}
				});
			}
			// NPC dialog (group 231)
			if (event.getGroupId() == 231)
			{
				clientThread.invokeLater(() ->
				{
					Widget textWidget = client.getWidget(231, 4);
					if (textWidget != null && textWidget.getText() != null)
					{
						authoringLog("DIALOG_NPC text='%s'", textWidget.getText());
					}
				});
			}
		}

		if (event.getGroupId() != COLLECTION_LOG_GROUP_ID)
		{
			return;
		}

		log.info("Collection log widget loaded (group {})", event.getGroupId());
		collectionLogOpen = true;

		// Trigger automatic full scan: programmatically activate search mode
		// which causes script 4100 to fire once per obtained item
		autoSyncPending = true;
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != SCRIPT_COLLECTION_LOG_ITEM)
		{
			return;
		}

		// Script 4100 fires per obtained item during search-mode iteration
		// Args: [source_widget, itemId, itemCount]
		Object[] args = event.getScriptEvent().getArguments();
		if (args != null && args.length >= 3)
		{
			int itemId = (int) args[1];
			if (itemId > 0)
			{
				collectionState.markItemObtained(itemId);
				scriptScanItemCount++;
			}
		}

		// Reset the settle countdown — more items may be coming
		scriptScanActive = true;
		scanSettleCountdown = SCAN_SETTLE_TICKS;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Debounced requirements refresh — flagged by onVarbitChanged, runs once per tick
		if (pendingRequirementsRefresh)
		{
			pendingRequirementsRefresh = false;
			boolean reqsChanged = requirementsChecker.refreshAccessibility(database.getAllSources());
			if (reqsChanged && !scriptScanActive)
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

		// Dispatch deferred ShortestPath "path" message (1-tick after "clear")
		if (pendingShortestPathTarget != null)
		{
			WorldPoint target = pendingShortestPathTarget;
			pendingShortestPathTarget = null;
			Map<String, Object> data = new HashMap<>();
			if (client.getLocalPlayer() != null)
			{
				data.put("start", client.getLocalPlayer().getWorldLocation());
			}
			data.put("target", target);
			eventBus.post(new PluginMessage("shortestpath", "path", data));
		}

		// Cache player location for guidance sequencer and authoring log (client thread only).
		// When sailing, the player is inside a WorldEntity whose inner WorldView
		// returns boat-local coords.  Detect this and transform to real-world
		// coordinates via WorldEntity.transformToMainWorld().
		// Ref: LlemonDuck/sailing SailingUtil, RuneLite WorldEntity API
		if (client.getLocalPlayer() != null)
		{
			cachedPlayerLocation = resolvePlayerWorldLocation();

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
		if (slayerRefreshPending && loginTickDelay <= 7)
		{
			slayerRefreshPending = false;
			slayerTaskState.refresh();
			pendingPanelRebuild = true;
			rankedSourcesDirty = true;
		}

		// One-time check: warn if in-game collection log notification is disabled.
		// Without it, the "New item added to your collection log" chat message
		// never fires and our real-time detection silently fails.
		// Approach from C Engineer: Completed plugin (m0bilebtw).
		if (!clogNotificationChecked && loginTickDelay <= 5 && client.getGameState() == GameState.LOGGED_IN)
		{
			clogNotificationChecked = true;
			int setting = client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM);
			if (setting == 0 || setting == 2)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"[Collection Log Helper] Warning: Your collection log notification setting is " +
					"disabled. Enable it in Settings > All Settings > Collection Log > 'New item notification' " +
					"for real-time item detection.", "");
			}
		}

		// Auto-sync: trigger search mode when collection log first opens
		if (autoSyncPending && collectionLogOpen)
		{
			autoSyncPending = false;
			triggerSearchModeScan();
		}

		// Wait for script 4100 to finish firing, then finalize
		if (scriptScanActive)
		{
			scanSettleCountdown--;
			if (scanSettleCountdown <= 0)
			{
				scriptScanActive = false;
				hasCompletedFullSync = true;
				dataSyncState.setCollectionLogSynced(true);
				guidanceOverlay.setShowCollectionLogReminder(false);
				collectionState.saveLastSyncedCount();
				int capturedCount = scriptScanItemCount;
				log.info("Auto-sync complete: {} obtained items captured via script scan",
					capturedCount);
				scriptScanItemCount = 0;
				if (panel != null)
				{
					panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
						collectionState.getTotalObtained());
					panel.updateDataSyncWarning();
				}
				pendingPanelRebuild = true;
				rankedSourcesDirty = true;
				exportEfficiencyIfEnabled();
			}
		}

		// Send one-time sync reminders after login
		if (loginTickDelay > 0)
		{
			loginTickDelay--;
			if (loginTickDelay == 0 && !syncReminderSent)
			{
				syncReminderSent = true;
				if (config.showSyncReminder() && !hasCompletedFullSync)
				{
					guidanceOverlay.setShowCollectionLogReminder(true);
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						"<col=00c8c8>[Collection Log Helper]</col> Open your in-game Collection Log (click the quest tab icon) to sync progress.",
						null);
				}
				if (config.showBankScanReminder() && !dataSyncState.isBankScanned())
				{
					guidanceOverlay.setShowBankReminder(true);
				}
				if (panel != null)
				{
					panel.updateDataSyncWarning();
				}
			}
		}

		// Auto-dismiss overlay reminders after 2 minutes
		if (dataSyncState.isReminderExpired())
		{
			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
		}

		// Detect collection log closed
		if (collectionLogOpen)
		{
			Widget frame = client.getWidget(InterfaceID.Collection.FRAME);
			if (frame == null || frame.isHidden())
			{
				collectionLogOpen = false;
			}
		}

		// World map arrow rotation
		updateWorldMapArrow();

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

	private void updateWorldMapArrow()
	{
		if (activeMapPoint == null)
		{
			return;
		}

		net.runelite.api.worldmap.WorldMap worldMap = client.getWorldMap();
		if (worldMap == null)
		{
			return;
		}

		net.runelite.api.Point mapCenter = worldMap.getWorldMapPosition();
		if (mapCenter == null)
		{
			return;
		}

		WorldPoint target = activeMapPoint.getWorldPoint();
		int dx = target.getX() - mapCenter.getX();
		// World map Y is inverted (higher Y = further north = up on map)
		int dy = target.getY() - mapCenter.getY();

		// Map 8 octants to arrow directions
		int degrees;
		if (Math.abs(dx) > Math.abs(dy) * 2)
		{
			degrees = dx > 0 ? 0 : 180;
		}
		else if (Math.abs(dy) > Math.abs(dx) * 2)
		{
			degrees = dy > 0 ? 270 : 90;
		}
		else if (dx > 0)
		{
			degrees = dy > 0 ? 315 : 45;
		}
		else
		{
			degrees = dy > 0 ? 225 : 135;
		}

		activeMapPoint.rotateArrow(degrees);
	}

	/**
	 * Programmatically trigger collection log search mode, which causes
	 * script 4100 to fire once per obtained item. This is the same technique
	 * used by TempleOSRS and WikiSync for full collection log scanning.
	 * After triggering search, immediately clicks "Back" so the user
	 * doesn't see the search UI flash.
	 */
	private void triggerSearchModeScan()
	{
		scriptScanItemCount = 0;
		scriptScanActive = false;
		scanSettleCountdown = SCAN_SETTLE_TICKS;

		if (panel != null)
		{
			panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCING, 0);
		}

		log.info("Triggering collection log search-mode scan");

		// Click the Search toggle to enter search mode (fires script 4100 for each obtained item)
		client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE,
			MenuAction.CC_OP, 1, -1, "Search", null);

		// Run the search script to complete the transition
		client.runScript(SCRIPT_COLLECTION_LOG_SEARCH);

		// Click Back to exit search mode so the UI returns to normal
		client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE,
			MenuAction.CC_OP, 1, -1, "Back", null);
	}

	public void activateGuidance(CollectionLogSource source)
	{
		if (!config.showOverlays())
		{
			return;
		}

		// Warn if the source has unmet requirements (don't block, just warn)
		List<String> unmetReqs = requirementsChecker.getUnmetRequirements(source.getName());
		if (!unmetReqs.isEmpty())
		{
			String unmetList = String.join(", ", unmetReqs);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"[Collection Log Helper] Warning: " + source.getName() + " requires " + unmetList, "");
		}

		// Clear any existing guidance first, including InfoBox and sequencer
		deactivateGuidance();

		// If source has multi-step guidance, start the sequencer
		if (source.getGuidanceSteps() != null && !source.getGuidanceSteps().isEmpty())
		{
			guidanceSequencer.setPlayerLocation(cachedPlayerLocation);
			// startSequence triggers onStepChanged callback which handles
			// applyStepToOverlays and scanForTrackedNpc for the initial step
			guidanceSequencer.startSequence(source, this::onStepChanged, this::onSequenceComplete);
			GuidanceStep step = guidanceSequencer.getCurrentStep();
			if (panel != null)
			{
				panel.setGuidanceState(true, source);
				GuidanceStep rawStep = guidanceSequencer.getRawCurrentStep();
				panel.updateStepProgress(
					guidanceSequencer.getCurrentIndex() + 1,
					guidanceSequencer.getTotalSteps(),
					step != null ? step.getDescription() : "",
					step != null && step.getCompletionCondition() == CompletionCondition.MANUAL,
					rawStep != null ? rawStep.getRequiredItemIds() : null);
			}
			// Add InfoBox showing step progress
			if (!source.getItems().isEmpty())
			{
				BufferedImage icon = itemManager.getImage(source.getItems().get(0).getItemId());
				activeInfoBox = new GuidanceInfoBox(icon, this);
				activeInfoBox.setStepText("1/" + guidanceSequencer.getTotalSteps());
				activeInfoBox.setTooltipText(source.getName() + ": "
					+ (step != null ? step.getDescription() : ""));
				infoBoxManager.addInfoBox(activeInfoBox);
			}
			log.debug("Multi-step guidance activated for {} ({} steps)",
				source.getName(), source.getGuidanceSteps().size());
			return;
		}

		if (source.getCategory() == CollectionLogCategory.CLUES)
		{
			// Clue sources: text overlay + panel banner instead of map markers
			guidanceOverlay.setClueGuidanceText("Do " + source.getName());
			if (panel != null)
			{
				panel.showClueGuidance(source);
			}

			// Client API calls and ShortestPath messages must run on the client thread
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
				}
			});
		}
		else
		{
			// Non-clue sources: world map, tile, and minimap overlays
			applySourceToOverlays(source);
		}

		// Always update panel guidance state from the plugin
		if (panel != null)
		{
			panel.setGuidanceState(true, source);
			panel.hideStepProgress();
		}

		log.debug("Guidance activated for {} ({})", source.getName(), source.getCategory());
	}

	/**
	 * Applies a single guidance step's data to all overlays.
	 */
	private void applyStepToOverlays(GuidanceStep step, String sourceName, CollectionLogSource source)
	{
		// Send world message hint if this step has one (only once per step, not on re-notify)
		int stepIndex = guidanceSequencer.getCurrentIndex();
		if (step.getWorldMessage() != null && !step.getWorldMessage().isEmpty()
			&& stepIndex != lastMessagedStepIndex)
		{
			lastMessagedStepIndex = stepIndex;
			clientThread.invokeLater(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=00c8c8>[Collection Log Helper]</col> " + step.getWorldMessage(),
					null));
		}

		// Set tile filter before target IDs so it's active during rescan
		if (step.getObjectFilterTiles() != null && !step.getObjectFilterTiles().isEmpty())
		{
			List<WorldPoint> tiles = new java.util.ArrayList<>();
			for (int[] t : step.getObjectFilterTiles())
			{
				if (t != null && t.length >= 3)
				{
					tiles.add(new WorldPoint(t[0], t[1], t[2]));
				}
			}
			objectHighlightOverlay.setObjectFilterTiles(tiles);
		}
		else if (step.getObjectMaxDistance() > 0 && step.getWorldX() > 0)
		{
			objectHighlightOverlay.setObjectFilter(
				new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane()),
				step.getObjectMaxDistance());
		}
		else
		{
			objectHighlightOverlay.setObjectFilter(null, 0);
		}
		objectHighlightOverlay.setTargetObjectIds(step.getAllObjectIds());
		objectHighlightOverlay.setObjectInteractAction(step.getObjectInteractAction());
		objectHighlightOverlay.setUseItemOnObject(step.isUseItemOnObject());
		objectHighlightOverlay.setTooltipText(step.getDescription());

		itemHighlightOverlay.setTargetItemIds(step.getHighlightItemIds());

		groundItemHighlightOverlay.setTargetGroundItemIds(
			step.getGroundItemIds() != null ? new HashSet<>(step.getGroundItemIds()) : null);

		if (step.getHighlightWidgetIds() != null && step.getHighlightWidgetIds().length > 0)
		{
			List<int[]> widgets = new java.util.ArrayList<>();
			for (int[] ref : step.getHighlightWidgetIds())
			{
				if (ref != null && ref.length >= 2)
				{
					widgets.add(ref);
				}
			}
			widgetHighlightOverlay.setHighlightWidgets(widgets);
		}
		else
		{
			widgetHighlightOverlay.clearHighlights();
		}

		// Suppress tile highlight when ObjectHighlightOverlay handles the visual
		guidanceOverlay.setSuppressTileHighlight(!step.getAllObjectIds().isEmpty());

		if (step.getWorldX() > 0)
		{
			WorldPoint worldPoint = new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			guidanceOverlay.setTargetPoint(worldPoint);
			guidanceOverlay.setTargetName(sourceName);
			guidanceOverlay.setLocationDescription(step.getDescription());
			String rawTravelTip = step.getTravelTip();
			if ((rawTravelTip == null || rawTravelTip.isEmpty()) && source != null)
			{
				rawTravelTip = source.getTravelTip();
			}
			String travelTip = travelCapabilities.selectBestTravelTip(rawTravelTip);
			guidanceOverlay.setTravelTip(travelTip);
			log.debug("Travel capabilities for step '{}': {}", step.getDescription(), travelCapabilities.getSummary());
			guidanceOverlay.setTargetNpcId(step.getNpcId());
			guidanceOverlay.setInteractAction(step.getInteractAction());
			dialogHighlightOverlay.setTargetDialogOptions(step.getDialogOptions());
			dialogHighlightOverlay.setGuidanceActive(true);
			guidanceMinimapOverlay.setTargetPoint(worldPoint);
			worldMapRouteOverlay.setTargetPoint(worldPoint);
			if (activeMapPoint != null)
			{
				worldMapPointManager.remove(activeMapPoint);
			}
			activeMapPoint = new CollectionLogWorldMapPoint(worldPoint, step.getDescription(), collectionLogIcon);
			worldMapPointManager.add(activeMapPoint);

			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				// Skip hint arrow inside Sailing instances — surface world
				// coords render at wrong positions in instanced WorldViews
				boolean inInstance = client.getLocalPlayer() != null
					&& client.getLocalPlayer().getWorldView() != client.getTopLevelWorldView();
				if (config.showHintArrow()
					&& !inInstance
					&& client.getLocalPlayer() != null
					&& client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane())
				{
					client.setHintArrow(worldPoint);
				}

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
					pendingShortestPathTarget = worldPoint;
				}
			});
		}
		else
		{
			// Step with no location — clear previous target and show text overlay only
			guidanceOverlay.setTargetPoint(null);
			guidanceOverlay.setTargetName(null);
			guidanceOverlay.setTargetNpcId(0);
			guidanceOverlay.setInteractAction(null);
			guidanceOverlay.setLocationDescription(null);
			guidanceOverlay.setTravelTip(null);
			guidanceMinimapOverlay.setTargetPoint(null);
			worldMapRouteOverlay.setTargetPoint(null);
			if (activeMapPoint != null)
			{
				worldMapPointManager.remove(activeMapPoint);
				activeMapPoint = null;
			}
			guidanceOverlay.setClueGuidanceText(step.getDescription());
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();
				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
				}
			});
		}

		// Dynamic overlay override for Shades of Mort'ton key/chest highlighting
		applyDynamicItemObjectOverlays();
	}

	/**
	 * Dynamically overrides overlays when the current step has dynamicItemObjectTiers.
	 * Scans inventory for the lowest tier with matching items, then highlights
	 * that tier's objects and the matching item. Called on step change and on
	 * inventory change while a guidance sequence is active.
	 */
	private void applyDynamicItemObjectOverlays()
	{
		GuidanceStep step = guidanceSequencer.getRawCurrentStep();
		if (step == null || step.getDynamicItemObjectTiers() == null
			|| step.getDynamicItemObjectTiers().isEmpty())
		{
			return;
		}

		// Collect ALL matching tiers so multiple keys highlight multiple chests
		Set<Integer> matchedObjectIds = new HashSet<>();
		List<Integer> matchedItemIds = new java.util.ArrayList<>();
		String tooltipText = null;
		String action = null;

		for (ItemObjectTier tier : step.getDynamicItemObjectTiers())
		{
			if (tier.getItemIds() == null)
			{
				continue;
			}
			for (int itemId : tier.getItemIds())
			{
				if (playerInventoryState.hasItem(itemId))
				{
					if (tier.getObjectIds() != null && !tier.getObjectIds().isEmpty())
					{
						matchedObjectIds.addAll(tier.getObjectIds());
					}
					matchedItemIds.add(itemId);
					if (action == null)
					{
						action = tier.getInteractAction() != null
							? tier.getInteractAction()
							: step.getObjectInteractAction();
					}
					if (tooltipText == null)
					{
						tooltipText = tier.getName() != null
							? (action + " " + tier.getName())
							: step.getDescription();
					}
					break; // Only match first key per tier (avoid duplicates)
				}
			}
		}

		if (!matchedObjectIds.isEmpty())
		{
			objectHighlightOverlay.setTargetObjectIds(matchedObjectIds);
			objectHighlightOverlay.setObjectInteractAction(action);
			objectHighlightOverlay.setTooltipText(
				matchedItemIds.size() > 1 ? step.getDescription() : tooltipText);
			itemHighlightOverlay.setTargetItemIds(matchedItemIds);
		}
	}

	/**
	 * Applies a source's default location data to overlays (non-sequencer path).
	 * ShortestPath re-guidance: send "clear" now, then "path" on the next game
	 * tick via pendingShortestPathTarget. The 1-tick delay guarantees the clear
	 * is fully processed before the new path request arrives.
	 * Ref: https://github.com/Skretzo/shortest-path
	 */
	private void applySourceToOverlays(CollectionLogSource source)
	{
		WorldPoint worldPoint = source.getWorldPoint(requirementsChecker);
		String displayName = source.getDisplayLocation(requirementsChecker);

		guidanceOverlay.setTargetPoint(worldPoint);
		guidanceOverlay.setTargetName(source.getName());
		guidanceOverlay.setLocationDescription(displayName);
		guidanceOverlay.setTravelTip(source.getTravelTip());
		guidanceOverlay.setTargetNpcId(source.getNpcId());
		guidanceOverlay.setInteractAction(source.getInteractAction());
		dialogHighlightOverlay.setTargetDialogOptions(source.getDialogOptions());
		dialogHighlightOverlay.setGuidanceActive(true);
		guidanceMinimapOverlay.setTargetPoint(worldPoint);
		worldMapRouteOverlay.setTargetPoint(worldPoint);
		if (activeMapPoint != null)
		{
			worldMapPointManager.remove(activeMapPoint);
		}
		activeMapPoint = new CollectionLogWorldMapPoint(worldPoint, displayName, collectionLogIcon);
		worldMapPointManager.add(activeMapPoint);

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			boolean inInstance = client.getLocalPlayer() != null
				&& client.getLocalPlayer().getWorldView() != client.getTopLevelWorldView();
			if (config.showHintArrow()
				&& !inInstance
				&& client.getLocalPlayer() != null
				&& client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane())
			{
				client.setHintArrow(worldPoint);
			}

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
				pendingShortestPathTarget = worldPoint;
			}
		});
	}

	private void clearGuidanceOverlays()
	{
		trackedGuidanceNpc = null;
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		worldMapRouteOverlay.clearTarget();
		dialogHighlightOverlay.clear();
		objectHighlightOverlay.clearTarget();
		itemHighlightOverlay.clearTarget();
		groundItemHighlightOverlay.clearTargets();
		widgetHighlightOverlay.clearHighlights();
		clientThread.invokeLater(() -> client.clearHintArrow());
		activeMapPoint = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (panel != null)
		{
			panel.hideClueGuidance();
		}
	}

	/**
	 * Scans currently loaded NPCs to find a match for the given step's target NPC ID.
	 * Called once when a new step activates to seed the tracked NPC reference.
	 */
	private void scanForTrackedNpc(GuidanceStep step)
	{
		trackedGuidanceNpc = null;
		guidanceOverlay.setTrackedNpc(null);

		if (step == null || step.getNpcId() <= 0)
		{
			return;
		}

		final int targetNpcId = step.getNpcId();
		clientThread.invokeLater(() ->
		{
			WorldView wv = client.getTopLevelWorldView();
			if (wv == null)
			{
				return;
			}
			for (NPC npc : wv.npcs())
			{
				if (npc != null && npc.getId() == targetNpcId)
				{
					trackedGuidanceNpc = npc;
					guidanceOverlay.setTrackedNpc(npc);
					break;
				}
			}
		});
	}

	/**
	 * Callback from GuidanceSequencer when the current step changes.
	 */
	private void onStepChanged(GuidanceStep step)
	{
		clearGuidanceOverlays();
		CollectionLogSource activeSource = guidanceSequencer.getActiveSource();
		String sourceName = activeSource != null ? activeSource.getName() : "";
		applyStepToOverlays(step, sourceName, activeSource);
		scanForTrackedNpc(step);

		// Update InfoBox progress
		if (activeInfoBox != null)
		{
			int current = guidanceSequencer.getCurrentIndex() + 1;
			int total = guidanceSequencer.getTotalSteps();
			activeInfoBox.setStepText(current + "/" + total);
			String tooltip = step.getDescription();
			if (guidanceSequencer.getCumulativeTrackThreshold() > 0)
			{
				tooltip += "\n" + guidanceSequencer.getCumulativeActionCount()
					+ "/" + guidanceSequencer.getCumulativeTrackThreshold()
					+ " actions tracked";
			}
			activeInfoBox.setTooltipText(tooltip);
			if (current == total)
			{
				activeInfoBox.setTextColor(java.awt.Color.GREEN);
			}
		}

		if (panel != null)
		{
			GuidanceStep rawStep = guidanceSequencer.getRawCurrentStep();
			panel.updateStepProgress(
				guidanceSequencer.getCurrentIndex() + 1,
				guidanceSequencer.getTotalSteps(),
				step.getDescription(),
				step.getCompletionCondition() == CompletionCondition.MANUAL,
				rawStep != null ? rawStep.getRequiredItemIds() : null);
		}
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
	 * Callback from GuidanceSequencer when the entire sequence is complete.
	 */
	private void onSequenceComplete()
	{
		String sourceName = guidanceSequencer.getActiveSource() != null
			? guidanceSequencer.getActiveSource().getName() : "unknown";
		deactivateGuidance();

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
		lastMessagedStepIndex = -1;
		trackedGuidanceNpc = null;
		if (activeInfoBox != null)
		{
			infoBoxManager.removeInfoBox(activeInfoBox);
			activeInfoBox = null;
		}
		guidanceSequencer.stopSequence();
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		worldMapRouteOverlay.clearTarget();
		dialogHighlightOverlay.clear();
		objectHighlightOverlay.clearTarget();
		itemHighlightOverlay.clearTarget();
		groundItemHighlightOverlay.clearTargets();
		widgetHighlightOverlay.clearHighlights();
		activeMapPoint = null;
		pendingShortestPathTarget = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
			}
		});

		if (panel != null)
		{
			panel.hideClueGuidance();
			panel.hideStepProgress();
			panel.setGuidanceState(false, null);
		}

		log.debug("Guidance deactivated");
	}

	/**
	 * Rebuilds the cached set of source names that have at least one unobtained item.
	 * Called when collection state changes to avoid per-menu-entry item scanning.
	 */
	private void rebuildSourcesWithMissingItems()
	{
		Set<String> missing = new HashSet<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					missing.add(source.getName());
					break;
				}
			}
		}
		sourcesWithMissingItems = missing;
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showOverlays())
		{
			return;
		}

		int type = event.getType();
		if (type < MenuAction.NPC_FIRST_OPTION.getId() || type > MenuAction.NPC_FIFTH_OPTION.getId())
		{
			return;
		}

		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}

		int npcId = npc.getId();
		CollectionLogSource source = database.getSourceByNpcId(npcId);
		if (source == null)
		{
			return;
		}

		// Skip if guidance is already active for this source
		if (guidanceSequencer.isActive() && source.equals(guidanceSequencer.getActiveSource()))
		{
			return;
		}

		// Check if source has any missing items (O(1) cached lookup)
		if (!sourcesWithMissingItems.contains(source.getName()))
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption(MENU_OPTION_GUIDE)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setIdentifier(npcId);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();

		// Handle "Collection Log Guide" right-click menu action
		if (action == MenuAction.RUNELITE && MENU_OPTION_GUIDE.equals(event.getMenuOption()))
		{
			CollectionLogSource source = database.getSourceByNpcId(event.getId());
			if (source != null)
			{
				activateGuidance(source);
			}
			return;
		}

		// Authoring mode: log all interactions regardless of guidance state
		if (config.guidanceAuthoring())
		{
			authoringLog("MENU option='%s' target='%s' action=%s id=%d param0=%d param1=%d",
				event.getMenuOption(), event.getMenuTarget(), action,
				event.getId(), event.getParam0(), event.getParam1());

			if (action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
				|| action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
				|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION)
			{
				authoringLog("OBJECT id=%d option='%s'", event.getId(), event.getMenuOption());
			}
			else if (action == MenuAction.NPC_FIRST_OPTION || action == MenuAction.NPC_SECOND_OPTION
				|| action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_FOURTH_OPTION
				|| action == MenuAction.NPC_FIFTH_OPTION)
			{
				NPC npc = event.getMenuEntry().getNpc();
				if (npc != null)
				{
					authoringLog("NPC id=%d name='%s' option='%s'",
						npc.getId(), npc.getName(), event.getMenuOption());
				}
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
			{
				authoringLog("USE_ITEM_ON_OBJECT objectId=%d itemId=%d", event.getId(), event.getParam0());
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_NPC)
			{
				authoringLog("USE_ITEM_ON_NPC npcIndex=%d", event.getId());
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_WIDGET)
			{
				authoringLog("USE_ITEM_ON_ITEM param0=%d param1=%d", event.getParam0(), event.getParam1());
			}
		}

		if (!guidanceSequencer.isActive())
		{
			return;
		}

		// Track cumulative use-item-on-object actions for guidance (e.g., Trouble Brewing hopper)
		if (action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
		{
			CollectionLogSource source = guidanceSequencer.getActiveSource();
			if (source != null && source.getCumulativeTrackItemId() > 0
					&& source.getCumulativeTrackObjectIds() != null)
			{
				int objectId = event.getId();
				int itemId = event.getParam0();
				if (itemId == source.getCumulativeTrackItemId()
						&& source.getCumulativeTrackObjectIds().contains(objectId))
				{
					guidanceSequencer.onTrackedAction();
				}
			}
		}

		// Detect NPC interactions for NPC_TALKED_TO completion condition.
		if (action == MenuAction.NPC_FIRST_OPTION || action == MenuAction.NPC_SECOND_OPTION
			|| action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_FOURTH_OPTION
			|| action == MenuAction.NPC_FIFTH_OPTION)
		{
			NPC npc = event.getMenuEntry().getNpc();
			if (npc != null)
			{
				guidanceSequencer.onNpcInteracted(npc.getId());
			}
		}
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
		WorldPoint fallback = client.getLocalPlayer().getWorldLocation();
		try
		{
			WorldView playerView = client.getLocalPlayer().getWorldView();
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
					LocalPoint playerLocal = client.getLocalPlayer().getLocalLocation();
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

	// ---- Guidance Authoring Event Logger ----

	private void authoringLog(String format, Object... args)
	{
		if (!config.guidanceAuthoring())
		{
			return;
		}
		if (authoringLogWriter == null)
		{
			java.io.File logFile = pluginDataManager.getFile("authoring-log.txt");
			if (logFile == null)
			{
				logFile = new java.io.File(
					net.runelite.client.RuneLite.RUNELITE_DIR, "clh-authoring-log.txt");
			}
			try
			{
				authoringLogWriter = new java.io.PrintWriter(
					new java.io.FileWriter(logFile, true), true);
				authoringLogWriter.printf("=== Authoring session started %s ===%n",
					java.time.LocalDateTime.now().format(
						java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			}
			catch (java.io.IOException e)
			{
				log.error("Failed to open authoring log", e);
				return;
			}
		}
		WorldPoint loc = cachedPlayerLocation;
		String locStr = loc != null
			? String.format("[%d,%d,%d]", loc.getX(), loc.getY(), loc.getPlane()) : "[?,?,?]";
		String timestamp = java.time.LocalTime.now().format(
			java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
		authoringLogWriter.printf("%s %s %s%n", timestamp, locStr, String.format(format, args));
	}

	/** Logs inventory or equipment container changes with slot names for equipment. */
	private void logContainerChange(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV)
		{
			net.runelite.api.ItemContainer c = client.getItemContainer(InventoryID.INV);
			if (c == null)
			{
				return;
			}
			StringBuilder sb = new StringBuilder("INVENTORY");
			for (net.runelite.api.Item item : c.getItems())
			{
				if (item.getId() > 0 && item.getQuantity() > 0)
				{
					sb.append(String.format(" %d x%d", item.getId(), item.getQuantity()));
				}
			}
			authoringLog(sb.toString());
		}
		else if (containerId == InventoryID.WORN)
		{
			net.runelite.api.ItemContainer c = client.getItemContainer(InventoryID.WORN);
			if (c == null)
			{
				return;
			}
			String[] slotNames = {"Head", "Cape", "Amulet", "Weapon", "Body",
				"Shield", "?", "Legs", "?", "Gloves", "Boots", "?", "Ring", "Ammo"};
			StringBuilder sb = new StringBuilder("EQUIPMENT");
			net.runelite.api.Item[] items = c.getItems();
			for (int i = 0; i < items.length && i < slotNames.length; i++)
			{
				if (items[i].getId() > 0)
				{
					sb.append(String.format(" %s=%d", slotNames[i], items[i].getId()));
				}
			}
			authoringLog(sb.toString());
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!config.guidanceAuthoring() || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		int animId = client.getLocalPlayer().getAnimation();
		if (animId != -1)
		{
			authoringLog("ANIMATION player=%d", animId);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		groundItemHighlightOverlay.onItemSpawned(event.getItem(), event.getTile());

		if (!config.guidanceAuthoring())
		{
			return;
		}
		WorldPoint wp = event.getTile().getWorldLocation();
		authoringLog("GROUND_ITEM_SPAWN id=%d qty=%d at=[%d,%d,%d]",
			event.getItem().getId(), event.getItem().getQuantity(),
			wp.getX(), wp.getY(), wp.getPlane());
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		groundItemHighlightOverlay.onItemDespawned(event.getItem());

		if (!config.guidanceAuthoring())
		{
			return;
		}
		WorldPoint wp = event.getTile().getWorldLocation();
		authoringLog("GROUND_ITEM_DESPAWN id=%d at=[%d,%d,%d]",
			event.getItem().getId(), wp.getX(), wp.getY(), wp.getPlane());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getGameObject());

		if (!config.guidanceAuthoring())
		{
			return;
		}
		WorldPoint wp = event.getTile().getWorldLocation();
		authoringLog("OBJECT_SPAWN id=%d at=[%d,%d,%d]",
			event.getGameObject().getId(), wp.getX(), wp.getY(), wp.getPlane());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getGameObject());

		if (!config.guidanceAuthoring())
		{
			return;
		}
		WorldPoint wp = event.getTile().getWorldLocation();
		authoringLog("OBJECT_DESPAWN id=%d at=[%d,%d,%d]",
			event.getGameObject().getId(), wp.getX(), wp.getY(), wp.getPlane());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getGroundObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (config.guidanceAuthoring())
		{
			authoringLog("NPC_SPAWN id=%d name='%s' index=%d", npc.getId(), npc.getName(), npc.getIndex());
		}

		// Track the spawned NPC if it matches the current guidance step's target
		if (guidanceSequencer.isActive() && trackedGuidanceNpc == null)
		{
			GuidanceStep step = guidanceSequencer.getRawCurrentStep();
			if (step != null && step.getNpcId() > 0 && npc.getId() == step.getNpcId())
			{
				trackedGuidanceNpc = npc;
				guidanceOverlay.setTrackedNpc(npc);
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();

		if (config.guidanceAuthoring())
		{
			authoringLog("NPC_DESPAWN id=%d name='%s'", npc.getId(), npc.getName());
		}

		// Clear tracked NPC if it despawned
		if (npc == trackedGuidanceNpc)
		{
			trackedGuidanceNpc = null;
			guidanceOverlay.setTrackedNpc(null);
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (!guidanceSequencer.isActive())
		{
			return;
		}

		if (event.getSource() == client.getLocalPlayer() && event.getTarget() instanceof NPC)
		{
			NPC npc = (NPC) event.getTarget();
			GuidanceStep step = guidanceSequencer.getRawCurrentStep();
			if (step != null && step.getCompletionCondition() == CompletionCondition.NPC_TALKED_TO
				&& step.getCompletionNpcId() == npc.getId())
			{
				guidanceSequencer.onNpcInteracted(npc.getId());
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!config.guidanceAuthoring())
		{
			return;
		}
		if (event.getActor() == client.getLocalPlayer())
		{
			authoringLog("HITSPLAT_RECEIVED type=%d amount=%d",
				event.getHitsplat().getHitsplatType(), event.getHitsplat().getAmount());
		}
		else if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			authoringLog("HITSPLAT_DEALT npcId=%d name='%s' type=%d amount=%d",
				npc.getId(), npc.getName(),
				event.getHitsplat().getHitsplatType(), event.getHitsplat().getAmount());
		}
	}

	/** Last varbit change tick — throttle to avoid flooding the log. */
	private int lastVarbitLogTick = -1;

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
