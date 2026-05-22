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
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
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
 *       task, quest count (enum-derived ratio plus the in-game Quest Points
 *       varplayer for cross-checking against the Quest List header), POH built.</li>
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

	/**
	 * Quest enum entries that the in-game "Quests Completed: N/M" header does not count.
	 *
	 * <p>RuneLite's {@link Quest} enum exposes three kinds of entry:
	 * <ol>
	 *   <li>Main quests — counted by the in-game Quest List header.</li>
	 *   <li>Miniquests — tracked in the Quest List but excluded from the N/M tally.</li>
	 *   <li>Recipe for Disaster sub-entries — the parent {@code RECIPE_FOR_DISASTER} is a
	 *       main quest, but its 10 sub-entries are tracked separately and excluded.</li>
	 * </ol>
	 *
	 * <p>Filtering this set out of {@code Quest.values()} makes our overlay numerator
	 * and denominator match the in-game tab on any account (#487).
	 *
	 * <p>List sourced from the OSRS Wiki Miniquest category and the RFD sub-quest list.
	 * Add to this set when Jagex releases a new miniquest.
	 */
	private static final Set<Quest> NON_MAIN_QUEST_ENTRIES = EnumSet.of(
		// 19 miniquests (OSRS Wiki: Miniquest category)
		Quest.ALFRED_GRIMHANDS_BARCRAWL,
		Quest.ENTER_THE_ABYSS,
		Quest.THE_GENERALS_SHADOW,
		Quest.BARBARIAN_TRAINING,
		Quest.SKIPPY_AND_THE_MOGRES,
		Quest.CURSE_OF_THE_EMPTY_LORD,
		Quest.LAIR_OF_TARN_RAZORLOR,
		Quest.BEAR_YOUR_SOUL,
		Quest.THE_ENCHANTED_KEY,
		Quest.MAGE_ARENA_I,
		Quest.FAMILY_PEST,
		Quest.MAGE_ARENA_II,
		Quest.IN_SEARCH_OF_KNOWLEDGE,
		Quest.DADDYS_HOME,
		Quest.THE_FROZEN_DOOR,
		Quest.HOPESPEARS_WILL,
		Quest.INTO_THE_TOMBS,
		Quest.HIS_FAITHFUL_SERVANTS,
		Quest.VALE_TOTEMS,
		// 10 Recipe for Disaster sub-entries (parent RECIPE_FOR_DISASTER stays in count)
		Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST,
		Quest.RECIPE_FOR_DISASTER__MOUNTAIN_DWARF,
		Quest.RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE,
		Quest.RECIPE_FOR_DISASTER__PIRATE_PETE,
		Quest.RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE,
		Quest.RECIPE_FOR_DISASTER__EVIL_DAVE,
		Quest.RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE,
		Quest.RECIPE_FOR_DISASTER__SIR_AMIK_VARZE,
		Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI,
		Quest.RECIPE_FOR_DISASTER__CULINAROMANCER);

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

		safeRender("Account", this::renderAccountSummary);
		renderSection("POH Teleports (C1)");
		safeRender("POH Teleports", this::renderPohTeleports);
		renderSection("Equipped (C2)");
		safeRender("Equipped", this::renderEquipped);
		renderSection("Diary tiers (C3)");
		safeRender("Diary tiers", this::renderDiary);
		renderSection("Cape perks (C4)");
		safeRender("Cape perks", this::renderCapePerks);
		renderSection("Quest sub-milestones (C5)");
		safeRender("Quest sub-milestones", this::renderQuestSubMilestones);

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

		// Quest counts (#487): walk Quest.values() but filter NON_MAIN_QUEST_ENTRIES
		// so the row matches the in-game "Quests Completed: N/M" header. Quest points
		// is the raw QP varplayer for a second independent cross-check.
		int finishedCount = countFinishedQuests();
		int totalMainQuests = Quest.values().length - NON_MAIN_QUEST_ENTRIES.size();
		addRow("Quests", finishedCount + "/" + totalMainQuests);
		addRow("Quest points", String.valueOf(safeVarp(VarPlayer.QUEST_POINTS)));

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

	/**
	 * Run a section renderer and, if it throws or trips an NPE because a data
	 * source is wired up incorrectly, emit a dim {@code (error)} row instead of
	 * letting the exception bubble up and kill the overlay paint thread.
	 */
	private void safeRender(String sectionLabel, Runnable body)
	{
		try
		{
			body.run();
		}
		catch (Exception e)
		{
			addDimRow(sectionLabel + ": (error)");
		}
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

	private int safeVarp(int varpId)
	{
		try
		{
			return client.getVarpValue(varpId);
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
			if (NON_MAIN_QUEST_ENTRIES.contains(quest))
			{
				continue;
			}
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
