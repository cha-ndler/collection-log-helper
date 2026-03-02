package com.collectionloghelper;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
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
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
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
	private GuidanceOverlay guidanceOverlay;

	@Inject
	private GuidanceMinimapOverlay guidanceMinimapOverlay;

	private CollectionLogHelperPanel panel;
	private NavigationButton navButton;
	private int lastObtainedCount = -1;
	private boolean collectionLogOpen;

	@Override
	protected void startUp() throws Exception
	{
		database.load();

		panel = new CollectionLogHelperPanel(
			config, database, collectionState, calculator, itemManager,
			this::activateGuidance, this::deactivateGuidance);
		panel.setMode(config.defaultMode());

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");

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
		collectionState.clearState();

		log.info("Collection Log Helper stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				collectionState.refreshVarps();
				collectionState.loadObtainedItems();
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
			lastObtainedCount = -1;
			collectionLogOpen = false;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Runs on the client thread — safe to call client API
		collectionState.refreshVarps();

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
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!collectionLogOpen)
		{
			return;
		}

		// Check if the collection log is still visible
		Widget itemsContainer = client.getWidget(InterfaceID.Collection.ITEMS);
		if (itemsContainer == null || itemsContainer.isHidden())
		{
			collectionLogOpen = false;
			return;
		}

		scanCollectionLogWidget(itemsContainer);
	}

	private void scanCollectionLogWidget(Widget itemsContainer)
	{
		Widget[] children = itemsContainer.getDynamicChildren();
		if (children == null || children.length == 0)
		{
			return;
		}

		boolean changed = false;
		for (Widget child : children)
		{
			int itemId = child.getItemId();
			int opacity = child.getOpacity();

			// Obtained items have opacity 0 (fully visible), unobtained are faded
			if (itemId > 0 && opacity == 0)
			{
				if (collectionState.markItemObtained(itemId))
				{
					changed = true;
				}
			}
		}

		if (changed)
		{
			log.info("Collection log scan: synced obtained items (total: {})",
				collectionState.getObtainedCount());
			if (panel != null)
			{
				panel.rebuild();
			}
		}
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
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (panel != null)
		{
			panel.hideClueGuidance();
		}

		// Client API calls and ShortestPath messages must run on the client thread
		// (ShortestPath's handler calls client API internally)
		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
			}
		});

		if (source.getCategory() == CollectionLogCategory.CLUES)
		{
			// Clue sources: text overlay + panel banner instead of map markers
			guidanceOverlay.setClueGuidanceText("Do " + source.getName());
			if (panel != null)
			{
				panel.showClueGuidance(source);
			}
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
			worldMapPointManager.add(new CollectionLogWorldMapPoint(worldPoint, displayName));

			clientThread.invokeLater(() ->
			{
				if (config.showHintArrow())
				{
					client.setHintArrow(worldPoint);
				}

				if (config.useShortestPath())
				{
					Map<String, Object> data = new HashMap<>();
					data.put("target", worldPoint);
					eventBus.post(new PluginMessage("shortestpath", "path", data));
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
