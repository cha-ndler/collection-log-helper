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

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link PlayerQuestProgressState}.
 *
 * <p>{@link net.runelite.api.Quest#getState(Client)} internally calls
 * {@code client.runScript(ScriptID.QUEST_STATUS_GET, id)} and reads
 * {@code client.getIntStack()[0]}, where:
 * <ul>
 *   <li>2 = FINISHED</li>
 *   <li>1 = NOT_STARTED</li>
 *   <li>0 = IN_PROGRESS</li>
 * </ul>
 *
 * <p>Tests mock both {@code runScript} and {@code getIntStack} to control
 * returned quest state, alongside {@code getVarpValue} for direct varplayer
 * milestones.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class QuestProgressStateTest
{
	private static final int VARP_LOST_CITY = 147;
	private static final int VARP_PLAGUE_CITY = 165;
	private static final int VARP_BIOHAZARD = 68;

	/** Script return value meaning QuestState.FINISHED. */
	private static final int QUEST_FINISHED = 2;
	/** Script return value meaning QuestState.NOT_STARTED. */
	private static final int QUEST_NOT_STARTED = 1;
	/** Script return value meaning QuestState.IN_PROGRESS. */
	private static final int QUEST_IN_PROGRESS = 0;

	@Mock
	private Client client;

	@Mock
	private ClientThread clientThread;

	private PlayerQuestProgressState state;

	/** Backing array for getIntStack() — updated via stubScriptReturn(). */
	private final int[] intStack = new int[]{QUEST_NOT_STARTED};

	@BeforeEach
	public void setUp()
	{
		when(client.getIntStack()).thenReturn(intStack);

		// Default all varplayer reads to 0 (not started)
		when(client.getVarpValue(anyInt())).thenReturn(0);

		state = new PlayerQuestProgressState(client, clientThread);
	}

	// -----------------------------------------------------------------------
	// reset() / initial state
	// -----------------------------------------------------------------------

	@Test
	public void hasSubProgress_beforeRefresh_returnsFalse()
	{
		// No refresh called — all milestones should return false
		for (QuestSubMilestone m : QuestSubMilestone.values())
		{
			assertFalse( state.hasSubProgress(m),"Expected false before refresh: " + m);
		}
	}

	@Test
	public void reset_clearsAllMilestones()
	{
		// Arrange — mark Lost City complete so we have a true value
		when(client.getVarpValue(VARP_LOST_CITY)).thenReturn(6);
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));

		// Act
		state.reset();

		// Assert
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// Lost City (varplayer 147)
	// -----------------------------------------------------------------------

	@Test
	public void lostCity_notStarted_allMilestonesFalse()
	{
		// setUp stubs getVarpValue(anyInt()) = 0; no override needed
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT));
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED));
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	@Test
	public void lostCity_dramenBranchCut_branchMilestoneTrue()
	{
		// varplayer 4 = branch cut, staff not yet crafted
		when(client.getVarpValue(VARP_LOST_CITY)).thenReturn(4);
		stubScriptReturn(QUEST_IN_PROGRESS);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT));
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED));
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	@Test
	public void lostCity_dramenStaffCrafted_staffAndBranchMilestonesTrue()
	{
		// varplayer 5 = staff crafted
		when(client.getVarpValue(VARP_LOST_CITY)).thenReturn(5);
		stubScriptReturn(QUEST_IN_PROGRESS);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT));
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED));
		assertFalse(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	@Test
	public void lostCity_questComplete_allMilestonesTrue()
	{
		// varplayer 6 = quest complete
		when(client.getVarpValue(VARP_LOST_CITY)).thenReturn(6);
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT));
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED));
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// Plague City (Quest enum / varplayer 165)
	// -----------------------------------------------------------------------

	@Test
	public void plagueCity_notStarted_milestoneIsFalse()
	{
		// setUp stubs getVarpValue(anyInt()) = 0; varplayer fallback also returns 0 < 100
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.PLAGUE_CITY_COMPLETE));
	}

	@Test
	public void plagueCity_questFinished_milestoneIsTrue()
	{
		// isQuestFinished returns true when script says FINISHED — no varplayer read needed
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.PLAGUE_CITY_COMPLETE));
	}

	@Test
	public void plagueCity_varpFallback_milestoneIsTrue()
	{
		// QuestState returns NOT_STARTED but varplayer >= 100 (fallback path)
		when(client.getVarpValue(VARP_PLAGUE_CITY)).thenReturn(100);
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.PLAGUE_CITY_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// Biohazard (Quest enum / varplayer 68)
	// -----------------------------------------------------------------------

	@Test
	public void biohazard_notStarted_milestoneIsFalse()
	{
		// setUp stubs getVarpValue(anyInt()) = 0; script says NOT_STARTED
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.BIOHAZARD_COMPLETE));
	}

	@Test
	public void biohazard_questFinished_milestoneIsTrue()
	{
		// isQuestFinished returns true when script says FINISHED — no varplayer read needed
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.BIOHAZARD_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// Fairytale II — started grants fairy ring access
	// -----------------------------------------------------------------------

	@Test
	public void fairytaleII_notStarted_fairyRingMilestoneIsFalse()
	{
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.FAIRYTALE_II_FAIRY_RINGS_UNLOCKED));
	}

	@Test
	public void fairytaleII_inProgress_fairyRingMilestoneIsTrue()
	{
		stubScriptReturn(QUEST_IN_PROGRESS);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.FAIRYTALE_II_FAIRY_RINGS_UNLOCKED));
	}

	@Test
	public void fairytaleII_finished_fairyRingMilestoneIsTrue()
	{
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.FAIRYTALE_II_FAIRY_RINGS_UNLOCKED));
	}

	// -----------------------------------------------------------------------
	// Children of the Sun
	// -----------------------------------------------------------------------

	@Test
	public void childrenOfTheSun_notFinished_milestoneIsFalse()
	{
		stubScriptReturn(QUEST_IN_PROGRESS);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.CHILDREN_OF_THE_SUN_COMPLETE));
	}

	@Test
	public void childrenOfTheSun_finished_milestoneIsTrue()
	{
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.CHILDREN_OF_THE_SUN_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// RFD sub-quests
	// -----------------------------------------------------------------------

	@Test
	public void rfd_noSubquestsComplete_allRfdMilestonesFalse()
	{
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_ANOTHER_COOKS_QUEST_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_MOUNTAIN_DWARF_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_GOBLIN_GENERALS_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_PIRATE_PETE_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_LUMBRIDGE_GUIDE_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_EVIL_DAVE_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_SKRACH_UGLOGWEE_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_SIR_AMIK_VARZE_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_KING_AWOWOGEI_COMPLETE));
		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_CULINAROMANCER_DEFEATED));
	}

	@Test
	public void rfd_allSubquestsFinished_allRfdMilestonesTrue()
	{
		stubScriptReturn(QUEST_FINISHED);
		state.refresh();

		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_ANOTHER_COOKS_QUEST_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_MOUNTAIN_DWARF_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_GOBLIN_GENERALS_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_PIRATE_PETE_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_LUMBRIDGE_GUIDE_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_EVIL_DAVE_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_SKRACH_UGLOGWEE_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_SIR_AMIK_VARZE_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_FREEING_KING_AWOWOGEI_COMPLETE));
		assertTrue(state.hasSubProgress(QuestSubMilestone.RFD_CULINAROMANCER_DEFEATED));
	}

	@Test
	public void rfd_culinaromancerNotDefeatedWhenAllInProgress()
	{
		stubScriptReturn(QUEST_IN_PROGRESS);
		state.refresh();

		assertFalse(state.hasSubProgress(QuestSubMilestone.RFD_CULINAROMANCER_DEFEATED));
	}

	// -----------------------------------------------------------------------
	// Error resilience
	// -----------------------------------------------------------------------

	@Test
	public void refresh_varpThrows_keepsPreviousSnapshot()
	{
		// First refresh succeeds with Lost City complete
		when(client.getVarpValue(VARP_LOST_CITY)).thenReturn(6);
		stubScriptReturn(QUEST_NOT_STARTED);
		state.refresh();
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));

		// Second refresh: getVarpValue throws
		when(client.getVarpValue(anyInt())).thenThrow(new RuntimeException("simulated error"));

		state.refresh(); // must not propagate

		// Previous snapshot still intact
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	// -----------------------------------------------------------------------
	// Helper
	// -----------------------------------------------------------------------

	/**
	 * Stubs {@code client.runScript(...)} to update the int stack return
	 * value and sets the backing int stack array to the given return value.
	 * {@link net.runelite.api.Quest#getState(Client)} reads
	 * {@code intStack[0]} after calling runScript, so this controls what
	 * state all quest lookups return under the global stub.
	 */
	private void stubScriptReturn(int returnValue)
	{
		intStack[0] = returnValue;
		doAnswer(inv -> {
			intStack[0] = returnValue;
			return null;
		}).when(client).runScript(anyInt(), (Object[]) org.mockito.ArgumentMatchers.any());
	}
}
