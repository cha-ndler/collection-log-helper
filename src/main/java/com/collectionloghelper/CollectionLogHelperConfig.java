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
		name = "Sync",
		description = "External data sync settings",
		position = 150,
		closedByDefault = false
	)
	String syncSection = "sync";

	@ConfigItem(
		keyName = "enableCollectionLogNetImport",
		name = "Auto-sync collectionlog.net on login",
		description = "Automatically imports your obtained-items list from collectionlog.net (by RSN) shortly after login. Disable here to turn the auto-import off.",
		section = syncSection,
		position = 0
	)
	default boolean enableCollectionLogNetImport()
	{
		return true;
	}


	@ConfigSection(
		name = "Learning",
		description = "Per-account learning features",
		position = 160,
		closedByDefault = true
	)
	String learningSection = "learning";

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
		position = 6
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
		return NpcHighlightStyle.OUTLINE_GLOW;
	}

	@ConfigItem(
		keyName = "objectHighlightStyle",
		name = "Object Highlight Style",
		description = "How guided objects are highlighted in the game world",
		section = guidanceSection,
		position = 2
	)
	default ObjectHighlightStyle objectHighlightStyle()
	{
		return ObjectHighlightStyle.OUTLINE_GLOW;
	}

	@ConfigItem(
		keyName = "showHintArrow",
		name = "Show Hint Arrow",
		description = "Show the in-game yellow hint arrow at the guidance target",
		section = guidanceSection,
		position = 3
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
		position = 4
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
		position = 5
	)
	default Color overlayColor()
	{
		return new Color(0, 255, 255);
	}

	@ConfigItem(
		keyName = "learnKillTimes",
		name = "Learn Kill Times (opt-in)",
		description = "Observe actual kill cycles and use your personal rolling average to refine efficiency estimates. Data is stored locally per account. Off by default.",
		section = learningSection,
		position = 0
	)
	default boolean learnKillTimes()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableTempleOsrsSync",
		name = "Auto-sync TempleOSRS KC on login",
		description = "Automatically pulls your per-source kill counts from TempleOSRS (by RSN) shortly after login. Disable here to turn the auto-sync off.",
		section = syncSection,
		position = 1
	)
	default boolean enableTempleOsrsSync()
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

	@ConfigItem(
		keyName = "enableCapabilityDebugOverlay",
		name = "Player capability debug overlay",
		description = "Developer-facing overlay showing detected player state (Tier C7). Shows combat level, key skills, spellbook, prayer book, slayer task, quest count, and POH status.",
		section = developerSection,
		position = 1
	)
	default boolean enableCapabilityDebugOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableCrossSourceMode",
		name = "Cross-source recommendation mode",
		description = "Show a per-item best-path ranking that surfaces the fastest source for each unobtained item across all sources (Tier E1). Off by default — panel integration is in progress.",
		position = 10,
		hidden = true
	)
	default boolean enableCrossSourceMode()
	{
		return false;
	}

	// -------------------------------------------------------------------------
	// POH teleport manual overrides (C1)
	// These checkboxes let players declare POH teleport fixtures that varbit
	// detection cannot confirm from outside the house. Resolution rule:
	// varbit-detected positive OR config-override positive → has teleport.
	// Only fixtures with unreliable outside-the-POH varbits have overrides here;
	// varbit-detected fixtures (jewellery box, portal nexus, mounted pendants)
	// are authoritative when positive and do not need a config override.
	// -------------------------------------------------------------------------

	@ConfigSection(
		name = "POH Teleports",
		description = "Manually declare POH teleport fixtures that cannot be reliably auto-detected outside the house",
		position = 170,
		closedByDefault = true
	)
	String pohSection = "poh";

	@ConfigItem(
		keyName = "manualPohMountedGlory",
		name = "Has Mounted Glory",
		description = "Check if your POH has a mounted Amulet of Glory (Construction 47). "
			+ "Varbit detection is unreliable outside the house; this override is the primary detection path.",
		section = pohSection,
		position = 0
	)
	default boolean manualPohMountedGlory()
	{
		return false;
	}

	@ConfigItem(
		keyName = "manualPohSpiritTree",
		name = "Has Spirit Tree",
		description = "Check if your POH superior garden has a spirit tree or spirit tree & fairy ring "
			+ "(Construction 75, Farming 83). Varbit detection is unreliable outside the house.",
		section = pohSection,
		position = 1
	)
	default boolean manualPohSpiritTree()
	{
		return false;
	}

	@ConfigItem(
		keyName = "manualPohFairyRing",
		name = "Has Fairy Ring",
		description = "Check if your POH superior garden has a fairy ring or spirit tree & fairy ring "
			+ "(Construction 85). Varbit detection is unreliable outside the house.",
		section = pohSection,
		position = 2
	)
	default boolean manualPohFairyRing()
	{
		return false;
	}
}
