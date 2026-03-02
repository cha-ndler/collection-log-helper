package com.collectionloghelper;

import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("collectionloghelper")
public interface CollectionLogHelperConfig extends Config
{
	@ConfigSection(
		name = "Guidance",
		description = "Guidance overlay settings",
		position = 0
	)
	String guidanceSection = "guidance";

	@ConfigItem(
		keyName = "defaultMode",
		name = "Default Mode",
		description = "The default mode to show when opening the panel"
	)
	default CollectionLogHelperPanel.Mode defaultMode()
	{
		return CollectionLogHelperPanel.Mode.EFFICIENT;
	}

	@ConfigItem(
		keyName = "hideObtainedItems",
		name = "Hide Obtained Items",
		description = "Hide items you have already obtained from the collection log"
	)
	default boolean hideObtainedItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSyncReminder",
		name = "Show Sync Reminder",
		description = "Show a reminder to open the Collection Log after login to sync obtained items"
	)
	default boolean showSyncReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlays",
		name = "Show Overlays",
		description = "Show guidance overlays when using Guide Me",
		section = guidanceSection,
		position = 0
	)
	default boolean showOverlays()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHintArrow",
		name = "Show Hint Arrow",
		description = "Show the in-game yellow hint arrow at the guidance target",
		section = guidanceSection,
		position = 1
	)
	default boolean showHintArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "useShortestPath",
		name = "Shortest Path Integration",
		description = "Request a path from the Shortest Path plugin (if installed) when guidance is activated",
		section = guidanceSection,
		position = 2
	)
	default boolean useShortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayColor",
		name = "Overlay Color",
		description = "Color used for guidance overlays",
		section = guidanceSection,
		position = 3
	)
	default Color overlayColor()
	{
		return new Color(0, 255, 255);
	}
}
