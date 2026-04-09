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
		keyName = "accountType",
		name = "Account Type",
		description = "Adjusts completion rates for main accounts vs ironmen",
		position = 4
	)
	default AccountType accountType()
	{
		return AccountType.MAIN;
	}

	@ConfigItem(
		keyName = "raidTeamSize",
		name = "Raid Team Size",
		description = "Your typical raid team size — adjusts estimated completion times for CoX, ToB, and ToA",
		position = 5
	)
	default RaidTeamSize raidTeamSize()
	{
		return RaidTeamSize.SOLO;
	}

	@ConfigItem(
		keyName = "afkFilter",
		name = "Efficient AFK",
		description = "Filter sources by AFK level in Efficient and Pet Hunt modes",
		position = 7,
		hidden = true
	)
	default AfkFilter afkFilter()
	{
		return AfkFilter.OFF;
	}

	@ConfigItem(
		keyName = "efficientSortMode",
		name = "Efficient Sort",
		description = "Sort order for sources in Efficient mode",
		position = 8,
		hidden = true
	)
	default EfficientSortMode efficientSortMode()
	{
		return EfficientSortMode.EFFICIENCY;
	}

	@ConfigItem(
		keyName = "showSyncReminder",
		name = "Show Sync Reminder",
		description = "Show a reminder to open the Collection Log after login to sync obtained items",
		position = 8
	)
	default boolean showSyncReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBankScanReminder",
		name = "Show Bank Scan Reminder",
		description = "Show a reminder to open your Bank after login so the plugin can scan for clue scrolls and other items",
		position = 9
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
		position = 5
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
		keyName = "npcHighlightStyle",
		name = "NPC Highlight Style",
		description = "How guided NPCs are highlighted in the game world",
		section = guidanceSection,
		position = 1
	)
	default NpcHighlightStyle npcHighlightStyle()
	{
		return NpcHighlightStyle.HULL;
	}

	@ConfigItem(
		keyName = "showHintArrow",
		name = "Show Hint Arrow",
		description = "Show the in-game yellow hint arrow at the guidance target",
		section = guidanceSection,
		position = 2
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
		position = 3
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
		position = 4
	)
	default Color overlayColor()
	{
		return new Color(0, 255, 255);
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
