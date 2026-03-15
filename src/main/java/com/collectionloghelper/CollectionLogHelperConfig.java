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
		position = 100
	)
	String guidanceSection = "guidance";

	@ConfigSection(
		name = "Developer",
		description = "Development tools for authoring guidance sequences",
		position = 200,
		closedByDefault = true
	)
	String developerSection = "developer";

	@ConfigItem(
		keyName = "defaultMode",
		name = "Mode",
		description = "The default mode to show when opening the panel",
		position = 0
	)
	default CollectionLogHelperPanel.Mode defaultMode()
	{
		return CollectionLogHelperPanel.Mode.EFFICIENT;
	}

	@ConfigItem(
		keyName = "hideObtainedItems",
		name = "Hide Obtained Items",
		description = "Hide items you have already obtained from the collection log",
		position = 1
	)
	default boolean hideObtainedItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideLockedContent",
		name = "Hide Locked Content",
		description = "Hide sources that require quests or skill levels you haven't met",
		position = 2
	)
	default boolean hideLockedContent()
	{
		return true;
	}

	@ConfigItem(
		keyName = "exportEfficiencyLog",
		name = "Export Efficiency Log",
		description = "Write a detailed efficiency ranking to ~/.runelite/collection-log-helper/{player}/efficiency-export.txt after each sync",
		position = 3
	)
	default boolean exportEfficiencyLog()
	{
		return false;
	}

	@ConfigItem(
		keyName = "raidTeamSize",
		name = "Raid Team Size",
		description = "Your typical raid team size — adjusts estimated completion times for CoX, ToB, and ToA",
		position = 4
	)
	default RaidTeamSize raidTeamSize()
	{
		return RaidTeamSize.SOLO;
	}

	@ConfigItem(
		keyName = "proximityMaxDistance",
		name = "Proximity Max Distance",
		description = "Maximum tile distance for Proximity mode (0 = unlimited)",
		position = 5
	)
	default int proximityMaxDistance()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "afkFilter",
		name = "Efficient AFK",
		description = "Filter sources by AFK level in Efficient and Pet Hunt modes",
		position = 6,
		hidden = true
	)
	default AfkFilter afkFilter()
	{
		return AfkFilter.OFF;
	}

	@ConfigItem(
		keyName = "showSyncReminder",
		name = "Show Sync Reminder",
		description = "Show a reminder to open the Collection Log after login to sync obtained items",
		position = 7
	)
	default boolean showSyncReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBankScanReminder",
		name = "Show Bank Scan Reminder",
		description = "Show a reminder to open your Bank after login so the plugin can scan for clue scrolls and other items",
		position = 8
	)
	default boolean showBankScanReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoAdvanceGuidance",
		name = "Auto-Advance Guidance",
		description = "Automatically start guidance for the next best source when a guided sequence completes",
		section = guidanceSection,
		position = 4
	)
	default boolean autoAdvanceGuidance()
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

	@ConfigItem(
		keyName = "notifyOnStepComplete",
		name = "Step Notifications",
		description = "Show a RuneLite notification when a guidance step auto-completes",
		section = guidanceSection,
		position = 5
	)
	default boolean notifyOnStepComplete()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyOnSequenceComplete",
		name = "Sequence Notifications",
		description = "Show a RuneLite notification when an entire guidance sequence completes",
		section = guidanceSection,
		position = 6
	)
	default boolean notifyOnSequenceComplete()
	{
		return true;
	}

	@ConfigItem(
		keyName = "guidanceAuthoring",
		name = "Guidance Authoring Mode",
		description = "Log all game interactions (menu clicks, NPC IDs, object IDs, animations, chat messages, locations) to a file for authoring new guidance sequences",
		section = developerSection,
		position = 0
	)
	default boolean guidanceAuthoring()
	{
		return false;
	}
}
