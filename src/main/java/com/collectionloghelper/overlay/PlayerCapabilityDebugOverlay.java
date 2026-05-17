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
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.player.DiaryRegion;
import com.collectionloghelper.player.DiaryTier;
import com.collectionloghelper.player.DiaryTierState;
import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PlayerQuestProgressState;
import com.collectionloghelper.player.PohTeleport;
import com.collectionloghelper.player.PohTeleportInventory;
import com.collectionloghelper.player.QuestSubMilestone;
import com.collectionloghelper.player.SkillCapePerk;
import com.collectionloghelper.player.SkillCapePerkState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Developer-facing overlay (Tier C7) that surfaces all detected player-capability
 * state so guidance authors can verify branching logic without attaching a debugger.
 *
 * <p>Renders three blocks:
 * <ol>
 *   <li><b>Account summary</b> — combat, key skills, spellbook, prayer book, slayer
 *       task, quest enum count, POH built.</li>
 *   <li><b>Tier C detection</b> — one section per detection layer:
 *       C1 POH teleport inventory, C2 equipped items, C3 diary tiers per region,
 *       C4 skill cape perks, C5 partial-quest sub-milestones.</li>
 *   <li><b>Truncation</b> — sub-milestone lists are capped at {@link #MAX_SUBMILESTONES_SHOWN}
 *       to keep the overlay manageable.</li>
 * </ol>
 *
 * <p>Visible only when {@link CollectionLogHelperConfig#enableCapabilityDebugOverlay()}
 * is {@code true}. Disabled by default — zero impact on normal players.
 */
@Singleton
public class PlayerCapabilityDebugOverlay extends OverlayPanel
{
	/** Varbit controlling the active spellbook (same constant as PlayerTravelCapabilities). */
	private static final int VARBIT_ACTIVE_SPELLBOOK = 4070;

	/**
	 * Varbit controlling the active prayer book.
	 * 0 = Normal prayers, 1 = Ancient Curses.
	 */
	private static final int VARBIT_ACTIVE_PRAYER_BOOK = 8143;

	/** Varbit controlling the POH location (0 = no POH). */
	private static final int VARBIT_POH_LOCATION = 2187;

	private static final String[] SPELLBOOK_NAMES = {"Standard", "Ancient", "Lunar", "Arceuus"};
	private static final String[] PRAYER_BOOK_NAMES = {"Normal", "Ancient Curses"};

	private static final Color TITLE_COLOR = new Color(255, 200, 0);
	private static final Color SECTION_COLOR = new Color(120, 200, 255);
	private static final Color LABEL_COLOR = new Color(180, 180, 180);
	private static final Color VALUE_COLOR = Color.WHITE;
	private static final Color VALUE_DIM = new Color(140, 140, 140);

	/** Cap on sub-milestones rendered to avoid the overlay dominating the viewport. */
	private static final int MAX_SUBMILESTONES_SHOWN = 8;

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final SlayerTaskState slayerTaskState;
	private final PohTeleportInventory pohTeleportInventory;
	private final EquippedItemState equippedItemState;
	private final DiaryTierState diaryTierState;
	private final SkillCapePerkState skillCapePerkState;
	private final PlayerQuestProgressState questProgressState;

	@Inject
	PlayerCapabilityDebugOverlay(Client client, CollectionLogHelperConfig config,
		SlayerTaskState slayerTaskState,
		PohTeleportInventory pohTeleportInventory,
		EquippedItemState equippedItemState,
		DiaryTierState diaryTierState,
		SkillCapePerkState skillCapePerkState,
		PlayerQuestProgressState questProgressState)
	{
		this.client = client;
		this.config = config;
		this.slayerTaskState = slayerTaskState;
		this.pohTeleportInventory = pohTeleportInventory;
		this.equippedItemState = equippedItemState;
		this.diaryTierState = diaryTierState;
		this.skillCapePerkState = skillCapePerkState;
		this.questProgressState = questProgressState;
		setPosition(OverlayPosition.TOP_RIGHT);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableCapabilityDebugOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("CLH Capability Debug")
			.color(TITLE_COLOR)
			.build());

		renderAccountSummary();
		renderSection("POH Teleports (C1)");
		renderPohTeleports();
		renderSection("Equipped (C2)");
		renderEquipped();
		renderSection("Diary tiers (C3)");
		renderDiary();
		renderSection("Cape perks (C4)");
		renderCapePerks();
		renderSection("Quest sub-milestones (C5)");
		renderQuestSubMilestones();

		return super.render(graphics);
	}

	// -------------------------------------------------------------------------
	// Block: account summary (unchanged from pre-#486 behaviour)
	// -------------------------------------------------------------------------

	private void renderAccountSummary()
	{
		Player localPlayer = client.getLocalPlayer();

		String combatLevel = localPlayer != null
			? String.valueOf(localPlayer.getCombatLevel()) : "?";
		addRow("Combat", combatLevel);

		addRow("Slayer", skillLevel(Skill.SLAYER));
		addRow("Construction", skillLevel(Skill.CONSTRUCTION));
		addRow("Farming", skillLevel(Skill.FARMING));
		addRow("Magic", skillLevel(Skill.MAGIC));

		int spellbookId = safeVarbit(VARBIT_ACTIVE_SPELLBOOK);
		addRow("Spellbook", resolveArrayName(SPELLBOOK_NAMES, spellbookId));

		int prayerBookId = safeVarbit(VARBIT_ACTIVE_PRAYER_BOOK);
		addRow("Prayers", resolveArrayName(PRAYER_BOOK_NAMES, prayerBookId));

		if (slayerTaskState.isTaskActive())
		{
			String task = slayerTaskState.getCreatureName() + " x" + slayerTaskState.getRemaining();
			addRow("Task", task);
		}
		else
		{
			addRow("Task", "none");
		}

		// "Quest entries" rather than "Quests done" — the RuneLite Quest enum
		// counts miniquests and some sub-entries separately from the in-game
		// Quest List, so this number can exceed the player's main-quest count
		// (see #487 for the longer-term reclassification).
		int finishedCount = countFinishedQuests();
		addRow("Quest entries", String.valueOf(finishedCount));

		int pohLocation = safeVarbit(VARBIT_POH_LOCATION);
		addRow("POH built", pohLocation > 0 ? "yes" : "no");
	}

	// -------------------------------------------------------------------------
	// Block: Tier C detection sections (added for #486)
	// -------------------------------------------------------------------------

	/** C1 — list every {@link PohTeleport} reported available. */
	private void renderPohTeleports()
	{
		Set<PohTeleport> teleports = pohTeleportInventory.getAvailableTeleports();
		if (teleports.isEmpty())
		{
			addDimRow("(none detected)");
			return;
		}
		for (PohTeleport tele : PohTeleport.values())
		{
			if (teleports.contains(tele))
			{
				addRow(humanise(tele.name()), "yes");
			}
		}
	}

	/** C2 — count + first equipped item ID for sanity-check. Full list is large. */
	private void renderEquipped()
	{
		Set<Integer> equipped = equippedItemState.getEquippedItems();
		addRow("Items equipped", String.valueOf(equipped.size()));
		if (!equipped.isEmpty())
		{
			// Show first item ID as a sanity check that detection is wired up.
			int sample = equipped.iterator().next();
			addRow("Sample item ID", String.valueOf(sample));
		}
	}

	/** C3 — per region, render the highest completed tier (or "-" if none). */
	private void renderDiary()
	{
		for (DiaryRegion region : DiaryRegion.values())
		{
			DiaryTier highest = highestCompletedTier(region);
			addRow(humanise(region.name()), highest != null ? highest.name() : "-");
		}
	}

	/** C4 — list every {@link SkillCapePerk} reported available. */
	private void renderCapePerks()
	{
		int shown = 0;
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			if (skillCapePerkState.hasPerkAvailable(perk))
			{
				addRow(humanise(perk.name()), "yes");
				shown++;
			}
		}
		if (shown == 0)
		{
			addDimRow("(none available)");
		}
	}

	/**
	 * C5 — list every completed {@link QuestSubMilestone}, capped at
	 * {@link #MAX_SUBMILESTONES_SHOWN}.
	 */
	private void renderQuestSubMilestones()
	{
		int total = 0;
		int shown = 0;
		for (QuestSubMilestone milestone : QuestSubMilestone.values())
		{
			if (questProgressState.hasSubProgress(milestone))
			{
				total++;
				if (shown < MAX_SUBMILESTONES_SHOWN)
				{
					addRow(humanise(milestone.name()), "yes");
					shown++;
				}
			}
		}
		if (total == 0)
		{
			addDimRow("(none completed)");
			return;
		}
		if (total > shown)
		{
			addDimRow("(+" + (total - shown) + " more)");
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private void addRow(String label, String value)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.leftColor(LABEL_COLOR)
			.right(value)
			.rightColor(VALUE_COLOR)
			.build());
	}

	private void addDimRow(String label)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.leftColor(VALUE_DIM)
			.build());
	}

	private void renderSection(String label)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(" ")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.leftColor(SECTION_COLOR)
			.build());
	}

	private String skillLevel(Skill skill)
	{
		try
		{
			return String.valueOf(client.getRealSkillLevel(skill));
		}
		catch (Exception e)
		{
			return "?";
		}
	}

	private int safeVarbit(int varbitId)
	{
		try
		{
			return client.getVarbitValue(varbitId);
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	private static String resolveArrayName(String[] names, int index)
	{
		if (index >= 0 && index < names.length)
		{
			return names[index];
		}
		return String.valueOf(index);
	}

	private int countFinishedQuests()
	{
		int count = 0;
		for (Quest quest : Quest.values())
		{
			try
			{
				if (quest.getState(client) == QuestState.FINISHED)
				{
					count++;
				}
			}
			catch (Exception ignored)
			{
				// individual quest lookup failures do not invalidate the count
			}
		}
		return count;
	}

	private DiaryTier highestCompletedTier(DiaryRegion region)
	{
		DiaryTier highest = null;
		for (DiaryTier tier : DiaryTier.values())
		{
			if (diaryTierState.hasDiary(region, tier))
			{
				highest = tier;
			}
		}
		return highest;
	}

	/** Convert {@code FOO_BAR_BAZ} to {@code Foo bar baz} for display. */
	private static String humanise(String upperSnake)
	{
		if (upperSnake == null || upperSnake.isEmpty())
		{
			return upperSnake;
		}
		String lower = upperSnake.toLowerCase().replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
