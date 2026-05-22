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
package com.collectionloghelper.player;

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * Live implementation of {@link QuestProgressState}.
 *
 * <p>Reads varplayer/varbit values and {@link Quest} state from the RuneLite
 * {@link Client} on the client thread, then caches a snapshot in an
 * {@code EnumMap<QuestSubMilestone, Boolean>} for lock-free reads from any
 * thread.
 *
 * <p>Varplayer IDs are sourced from the OSRS Wiki:
 * <ul>
 *   <li>Lost City: RuneScape:Varplayer/147</li>
 *   <li>Plague City: RuneScape:Varplayer/165</li>
 *   <li>Biohazard: RuneScape:Varplayer/68</li>
 * </ul>
 *
 * <p>RFD sub-quests use {@link Quest#getState(Client)} via RuneLite's
 * per-sub-quest Quest enum entries (ids 2307–2316), which internally call the
 * {@code QUEST_STATUS_GET} script.
 */
@Slf4j
@Singleton
public class PlayerQuestProgressState implements QuestProgressState
{
	// --- Varplayer IDs (sourced from OSRS Wiki) ---

	/** Lost City progress. Value 4 = dramen branch cut; 5 = staff crafted; 6 = complete. */
	private static final int VARP_LOST_CITY = 147;

	/** Plague City progress. Non-zero and final value = complete. */
	private static final int VARP_PLAGUE_CITY = 165;

	/** Biohazard progress. Non-zero and final value = complete. */
	private static final int VARP_BIOHAZARD = 68;

	// --- Lost City threshold values ---
	private static final int LOST_CITY_BRANCH_CUT_VALUE = 4;
	private static final int LOST_CITY_STAFF_CRAFTED_VALUE = 5;
	private static final int LOST_CITY_COMPLETE_VALUE = 6;

	// --- Plague City / Biohazard complete values (used as fallback cross-check) ---
	/** Plague City: final varplayer value indicating quest complete. */
	private static final int PLAGUE_CITY_COMPLETE_VALUE = 100;

	/** Biohazard: final varplayer value indicating quest complete. */
	private static final int BIOHAZARD_COMPLETE_VALUE = 30;

	private final Client client;

	/**
	 * Cached snapshot of all milestone states.
	 * Written atomically on the client thread via {@link #refresh()};
	 * readable from any thread via {@link #hasSubProgress(QuestSubMilestone)}.
	 */
	private volatile Map<QuestSubMilestone, Boolean> snapshot = emptySnapshot();

	@Inject
	PlayerQuestProgressState(Client client)
	{
		this.client = client;
	}

	@Override
	public boolean hasSubProgress(QuestSubMilestone milestone)
	{
		Boolean result = snapshot.get(milestone);
		return result != null && result;
	}

	/**
	 * Re-reads all tracked varplayer/varbit values and quest states from the
	 * client. Must be called on the client thread.
	 */
	@Override
	public void refresh()
	{
		Map<QuestSubMilestone, Boolean> next = new EnumMap<>(QuestSubMilestone.class);

		try
		{
			populateLostCity(next);
			populateQuestCompletion(next);
			populateRfd(next);
		}
		catch (Exception e)
		{
			log.warn("QuestProgressState refresh failed", e);
			// Leave snapshot as previous value on error rather than replacing with partial data.
			return;
		}

		snapshot = next;
		log.debug("QuestProgressState refreshed: {}", next);
	}

	/**
	 * Refreshes the quest-milestone snapshot on any {@link VarbitChanged} event.
	 *
	 * <p>Quest progression is driven by a mix of varplayer values (Lost City,
	 * Plague City, Biohazard) and {@link Quest#getState(Client)} reads (RFD
	 * sub-quests, Fairytale II, Children of the Sun). Quest varplayer/varbit
	 * IDs span the full varbit space and change at every game update, so a
	 * targeted filter is brittle. The full refresh is cheap (one varp read +
	 * a small fixed number of {@code QUEST_STATUS_GET} script calls).
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		refresh();
	}

	@Override
	public void reset()
	{
		snapshot = emptySnapshot();
	}

	// -----------------------------------------------------------------------
	// Private population helpers
	// -----------------------------------------------------------------------

	/**
	 * Reads varplayer 147 (Lost City) and sets the three Lost City milestones.
	 */
	private void populateLostCity(Map<QuestSubMilestone, Boolean> out)
	{
		int varp = client.getVarpValue(VARP_LOST_CITY);
		out.put(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT, varp >= LOST_CITY_BRANCH_CUT_VALUE);
		out.put(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED, varp >= LOST_CITY_STAFF_CRAFTED_VALUE);
		out.put(QuestSubMilestone.LOST_CITY_COMPLETE, varp >= LOST_CITY_COMPLETE_VALUE);
	}

	/**
	 * Reads quest completion state for all non-RFD quests tracked by this
	 * service: Plague City, Biohazard, Fairytale II, and Children of the Sun.
	 *
	 * <p>Plague City and Biohazard are also confirmed via their varplayer
	 * values as a cross-check, but the authoritative check is QuestState.
	 */
	private void populateQuestCompletion(Map<QuestSubMilestone, Boolean> out)
	{
		// Plague City — also confirmed via varplayer 165 >= 100
		boolean plagueCityDone = isQuestFinished(Quest.PLAGUE_CITY);
		if (!plagueCityDone)
		{
			// Fallback: varplayer cross-check in case Quest enum lags a game update
			plagueCityDone = client.getVarpValue(VARP_PLAGUE_CITY) >= PLAGUE_CITY_COMPLETE_VALUE;
		}
		out.put(QuestSubMilestone.PLAGUE_CITY_COMPLETE, plagueCityDone);

		// Biohazard — also confirmed via varplayer 68 >= 30
		boolean biohazardDone = isQuestFinished(Quest.BIOHAZARD);
		if (!biohazardDone)
		{
			biohazardDone = client.getVarpValue(VARP_BIOHAZARD) >= BIOHAZARD_COMPLETE_VALUE;
		}
		out.put(QuestSubMilestone.BIOHAZARD_COMPLETE, biohazardDone);

		// Fairytale II — started is sufficient for fairy ring access
		QuestState fairytaleIIState = questState(Quest.FAIRYTALE_II__CURE_A_QUEEN);
		out.put(
			QuestSubMilestone.FAIRYTALE_II_FAIRY_RINGS_UNLOCKED,
			fairytaleIIState != QuestState.NOT_STARTED
		);

		// Children of the Sun — must be fully complete for Varlamore access
		out.put(QuestSubMilestone.CHILDREN_OF_THE_SUN_COMPLETE,
			isQuestFinished(Quest.CHILDREN_OF_THE_SUN));
	}

	/**
	 * Reads each RFD sub-quest via its RuneLite Quest enum entry and maps it
	 * to the corresponding milestone.
	 */
	private void populateRfd(Map<QuestSubMilestone, Boolean> out)
	{
		out.put(QuestSubMilestone.RFD_ANOTHER_COOKS_QUEST_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST));

		out.put(QuestSubMilestone.RFD_FREEING_MOUNTAIN_DWARF_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__MOUNTAIN_DWARF));

		out.put(QuestSubMilestone.RFD_FREEING_GOBLIN_GENERALS_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE));

		out.put(QuestSubMilestone.RFD_FREEING_PIRATE_PETE_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__PIRATE_PETE));

		out.put(QuestSubMilestone.RFD_FREEING_LUMBRIDGE_GUIDE_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE));

		out.put(QuestSubMilestone.RFD_FREEING_EVIL_DAVE_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__EVIL_DAVE));

		out.put(QuestSubMilestone.RFD_FREEING_SKRACH_UGLOGWEE_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE));

		out.put(QuestSubMilestone.RFD_FREEING_SIR_AMIK_VARZE_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__SIR_AMIK_VARZE));

		out.put(QuestSubMilestone.RFD_FREEING_KING_AWOWOGEI_COMPLETE,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI));

		out.put(QuestSubMilestone.RFD_CULINAROMANCER_DEFEATED,
			isQuestFinished(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER));
	}

	// -----------------------------------------------------------------------
	// Convenience helpers
	// -----------------------------------------------------------------------

	private boolean isQuestFinished(Quest quest)
	{
		return questState(quest) == QuestState.FINISHED;
	}

	private QuestState questState(Quest quest)
	{
		try
		{
			return quest.getState(client);
		}
		catch (Exception e)
		{
			log.warn("Failed to read quest state for {}", quest, e);
			return QuestState.NOT_STARTED;
		}
	}

	private static Map<QuestSubMilestone, Boolean> emptySnapshot()
	{
		Map<QuestSubMilestone, Boolean> map = new EnumMap<>(QuestSubMilestone.class);
		for (QuestSubMilestone m : QuestSubMilestone.values())
		{
			map.put(m, false);
		}
		return map;
	}
}
