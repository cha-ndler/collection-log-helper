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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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
	private static final Color LABEL_COLOR = new Color(180, 180, 180);
	private static final Color VALUE_COLOR = Color.WHITE;

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final SlayerTaskState slayerTaskState;

	@Inject
	PlayerCapabilityDebugOverlay(Client client, CollectionLogHelperConfig config,
		SlayerTaskState slayerTaskState)
	{
		this.client = client;
		this.config = config;
		this.slayerTaskState = slayerTaskState;
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

		Player localPlayer = client.getLocalPlayer();

		// Combat level
		String combatLevel = localPlayer != null
			? String.valueOf(localPlayer.getCombatLevel()) : "?";
		addRow("Combat", combatLevel);

		// Key skill levels relevant to access gating
		addRow("Slayer", skillLevel(Skill.SLAYER));
		addRow("Construction", skillLevel(Skill.CONSTRUCTION));
		addRow("Farming", skillLevel(Skill.FARMING));
		addRow("Magic", skillLevel(Skill.MAGIC));

		// Active spellbook
		int spellbookId = safeVarbit(VARBIT_ACTIVE_SPELLBOOK);
		addRow("Spellbook", resolveArrayName(SPELLBOOK_NAMES, spellbookId));

		// Active prayer book
		int prayerBookId = safeVarbit(VARBIT_ACTIVE_PRAYER_BOOK);
		addRow("Prayers", resolveArrayName(PRAYER_BOOK_NAMES, prayerBookId));

		// Slayer task (creature + remaining count)
		if (slayerTaskState.isTaskActive())
		{
			String task = slayerTaskState.getCreatureName() + " x" + slayerTaskState.getRemaining();
			addRow("Task", task);
		}
		else
		{
			addRow("Task", "none");
		}

		// Quest completions. The RuneLite Quest enum includes miniquests and
		// some sub-entries (RFD subquests, etc.) as distinct values, so this
		// count exceeds the player-visible "Completed N/N" shown in the in-game
		// Quest List. Labeled "Quests + miniquests" to make the discrepancy
		// self-explaining; see #487 for the longer-term reclassification.
		int finishedCount = countFinishedQuests();
		addRow("Quests + miniquests", String.valueOf(finishedCount));
		panelComponent.getChildren().add(LineComponent.builder()
			.left("  (RuneLite enum count)")
			.leftColor(new Color(140, 140, 140))
			.build());

		// POH availability
		int pohLocation = safeVarbit(VARBIT_POH_LOCATION);
		addRow("POH", pohLocation > 0 ? "yes" : "no");

		return super.render(graphics);
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
}
