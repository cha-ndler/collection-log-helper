package com.collectionloghelper;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

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
	private DropRateDatabase database;

	@Inject
	private PlayerCollectionState collectionState;

	@Inject
	private EfficiencyCalculator calculator;

	@Inject
	private RequirementsChecker requirementsChecker;

	@Inject
	private GuidanceOverlay guidanceOverlay;

	@Inject
	private GuidanceMinimapOverlay guidanceMinimapOverlay;

	/** Minimum tile movement before proximity view is refreshed. */
	private static final int PROXIMITY_REFRESH_TILES = 10;

	private CollectionLogHelperPanel panel;
	private NavigationButton navButton;
	private int lastObtainedCount = -1;
	private boolean collectionLogOpen;
	private BufferedImage collectionLogIcon;
	private CollectionLogWorldMapPoint activeMapPoint;

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
	 * Read by the EDT in buildProximityView() — volatile ensures visibility.
	 */
	private volatile WorldPoint cachedPlayerLocation;
	private WorldPoint lastProximityLocation;

	@Override
	protected void startUp() throws Exception
	{
		database.load();

		panel = new CollectionLogHelperPanel(
			config, database, collectionState, calculator, itemManager,
			requirementsChecker,
			this::activateGuidance, this::deactivateGuidance, this::syncCollectionLog,
			() -> cachedPlayerLocation);
		panel.setMode(config.defaultMode());

		collectionLogIcon = itemManager.getImage(ItemID.COLLECTION_LOG);
		final BufferedImage icon = collectionLogIcon != null
			? collectionLogIcon
			: ImageUtil.loadImageResource(getClass(), "panel_icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Collection Log Helper")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		overlayManager.add(guidanceOverlay);
		overlayManager.add(guidanceMinimapOverlay);

		// If already logged in (e.g., plugin enabled mid-session), load state
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
				collectionState.captureRecentItems();
				requirementsChecker.refreshAccessibility(database.getAllSources());
				lastObtainedCount = collectionState.getTotalObtained();
				panel.rebuild();
			});
		}

		log.info("Collection Log Helper started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(guidanceOverlay);
		overlayManager.remove(guidanceMinimapOverlay);
		deactivateGuidance();
		lastObtainedCount = -1;
		collectionLogOpen = false;
		autoSyncPending = false;
		scriptScanActive = false;
		scanSettleCountdown = 0;
		hasCompletedFullSync = false;
		syncReminderSent = false;
		loginTickDelay = 0;
		cachedPlayerLocation = null;
		lastProximityLocation = null;
		guidanceOverlay.setShowSyncReminder(false);
		collectionState.clearState();

		log.info("Collection Log Helper stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			// Wait a few ticks after login before sending the sync reminder
			loginTickDelay = 10;
			clientThread.invokeLater(() ->
			{
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
				collectionState.captureRecentItems();
				requirementsChecker.refreshAccessibility(database.getAllSources());
				lastObtainedCount = collectionState.getTotalObtained();
				if (panel != null)
				{
					panel.rebuild();
				}
			});
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			collectionState.clearState();
			requirementsChecker.clearCache();
			lastObtainedCount = -1;
			collectionLogOpen = false;
			autoSyncPending = false;
			scriptScanActive = false;
			scanSettleCountdown = 0;
			hasCompletedFullSync = false;
			syncReminderSent = false;
			loginTickDelay = 0;
			cachedPlayerLocation = null;
			lastProximityLocation = null;
			guidanceOverlay.setShowSyncReminder(false);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Runs on the client thread — safe to call client API
		collectionState.refreshVarps();

		// Don't trigger rebuilds mid-scan; the settle logic in onGameTick
		// will fire a single rebuild once script 4100 stops firing.
		if (scriptScanActive)
		{
			return;
		}

		int currentCount = collectionState.getTotalObtained();
		if (currentCount == lastObtainedCount)
		{
			return;
		}

		lastObtainedCount = currentCount;
		if (panel != null)
		{
			panel.rebuild();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		boolean changed = requirementsChecker.refreshAccessibility(database.getAllSources());
		if (changed && panel != null)
		{
			panel.rebuild();
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

		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(event.getMessage());
		if (matcher.find())
		{
			String itemName = matcher.group(1);
			log.debug("New collection log item: {}", itemName);

			// Find the item in the database and mark it obtained
			for (CollectionLogSource source : database.getAllSources())
			{
				for (CollectionLogItem item : source.getItems())
				{
					if (item.getName().equalsIgnoreCase(itemName))
					{
						collectionState.markItemObtained(item.getItemId());
						log.debug("Marked item {} (ID: {}) as obtained", itemName, item.getItemId());
						break;
					}
				}
			}

			if (panel != null)
			{
				panel.rebuild();
			}
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
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
		// Cache player location for the EDT's proximity view (client thread only).
		// Proximity uses distanceTo2D() which ignores plane, so even boat-local
		// coords while sailing will produce usable (approximate) distances.
		// TODO: Transform sailing coords via WorldEntity for exact positions.
		// Ref: LlemonDuck/sailing SailingUtil, sololegends/runelite-friend-finder
		if (client.getLocalPlayer() != null)
		{
			cachedPlayerLocation = client.getLocalPlayer().getWorldLocation();

			// Refresh proximity view when player moves significantly
			if (panel != null
				&& panel.getCurrentMode() == CollectionLogHelperPanel.Mode.PROXIMITY)
			{
				WorldPoint current = cachedPlayerLocation;
				if (lastProximityLocation == null
					|| current.distanceTo2D(lastProximityLocation) >= PROXIMITY_REFRESH_TILES)
				{
					lastProximityLocation = current;
					panel.rebuild();
				}
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
				guidanceOverlay.setShowSyncReminder(false);
				log.info("Auto-sync complete: {} obtained items captured via script scan",
					scriptScanItemCount);
				scriptScanItemCount = 0;
				if (panel != null)
				{
					panel.rebuild();
				}
			}
		}

		// Send a one-time sync reminder after login if no full sync has been done
		if (loginTickDelay > 0 && config.showSyncReminder())
		{
			loginTickDelay--;
			if (loginTickDelay == 0 && !hasCompletedFullSync && !syncReminderSent)
			{
				syncReminderSent = true;
				guidanceOverlay.setShowSyncReminder(true);
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=00c8c8>[Collection Log Helper]</col> Open your Collection Log to sync your obtained items.",
					null);
			}
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

	/**
	 * Sync action triggered by the panel's Sync button.
	 * If the collection log is open, triggers a full search-mode scan.
	 * Otherwise, captures recent items from varps.
	 */
	private void syncCollectionLog()
	{
		clientThread.invokeLater(() ->
		{
			collectionState.captureRecentItems();

			Widget frame = client.getWidget(InterfaceID.Collection.FRAME);
			if (frame != null && !frame.isHidden())
			{
				triggerSearchModeScan();
				log.info("Sync: triggered search-mode scan");
			}
			else
			{
				log.info("Sync: captured recent items from varps (open in-game Collection Log for full sync)");
				if (panel != null)
				{
					panel.rebuild();
				}
			}
		});
	}

	public void activateGuidance(CollectionLogSource source)
	{
		if (!config.showOverlays())
		{
			return;
		}

		// Clear any existing guidance first (thread-safe overlay operations)
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		activeMapPoint = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (panel != null)
		{
			panel.hideClueGuidance();
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
			WorldPoint worldPoint = source.getWorldPoint();
			String displayName = source.getDisplayLocation();

			guidanceOverlay.setTargetPoint(worldPoint);
			guidanceOverlay.setTargetName(source.getName());
			guidanceOverlay.setLocationDescription(displayName);
			guidanceMinimapOverlay.setTargetPoint(worldPoint);
			activeMapPoint = new CollectionLogWorldMapPoint(worldPoint, displayName, collectionLogIcon);
			worldMapPointManager.add(activeMapPoint);

			// ShortestPath re-guidance: send "clear" before "path" with a
			// 1-tick delay. restartPathfinding() (invoked by the "path"
			// message) cancels the old pathfinder but does NOT reset
			// lastLocation. On the next game tick isNearPath() sees the
			// player at the same lastLocation and short-circuits, so the
			// new path never renders until the player moves. Sending
			// "clear" first goes through setTarget(UNDEFINED) which
			// properly tears down pathfinding state. The nested
			// invokeLater ensures the clear is processed on one client
			// tick and the new path request arrives on the next.
			// Ref: https://github.com/Skretzo/shortest-path
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				if (config.showHintArrow())
				{
					client.setHintArrow(worldPoint);
				}

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));

					clientThread.invokeLater(() ->
					{
						Map<String, Object> data = new HashMap<>();
						if (client.getLocalPlayer() != null)
						{
							data.put("start", client.getLocalPlayer().getWorldLocation());
						}
						data.put("target", worldPoint);
						eventBus.post(new PluginMessage("shortestpath", "path", data));
					});
				}
			});
		}

		// Always update panel guidance state from the plugin
		if (panel != null)
		{
			panel.setGuidanceState(true, source);
		}

		log.debug("Guidance activated for {} ({})", source.getName(), source.getCategory());
	}

	public void deactivateGuidance()
	{
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		activeMapPoint = null;
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
			panel.setGuidanceState(false, null);
		}

		log.debug("Guidance deactivated");
	}

	@Provides
	CollectionLogHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogHelperConfig.class);
	}
}
