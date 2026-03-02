package com.collectionloghelper;

import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

	@Override
	protected void startUp() throws Exception
	{
		database.load();

		panel = new CollectionLogHelperPanel(
			database, collectionState, calculator, itemManager,
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

		log.debug("Collection Log Helper started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(guidanceOverlay);
		overlayManager.remove(guidanceMinimapOverlay);
		deactivateGuidance();
		lastObtainedCount = -1;

		log.debug("Collection Log Helper stopped");
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
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

			if (panel != null)
			{
				panel.rebuild();
			}
		}
	}

	public void activateGuidance(WorldPoint worldPoint, String sourceName)
	{
		if (!config.showOverlays())
		{
			return;
		}

		guidanceOverlay.setTargetPoint(worldPoint);
		guidanceOverlay.setTargetName(sourceName);
		guidanceMinimapOverlay.setTargetPoint(worldPoint);

		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		worldMapPointManager.add(new CollectionLogWorldMapPoint(worldPoint, sourceName));

		log.debug("Guidance activated for {} at {}", sourceName, worldPoint);
	}

	public void deactivateGuidance()
	{
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);

		log.debug("Guidance deactivated");
	}

	@Provides
	CollectionLogHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CollectionLogHelperConfig.class);
	}
}
