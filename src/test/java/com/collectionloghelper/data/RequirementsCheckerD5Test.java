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
package com.collectionloghelper.data;

import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PohTeleportInventory;
import java.lang.reflect.Constructor;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * D5 schema behaviour tests for the {@code questMilestones} and
 * {@code skillCapePerks} clauses of
 * {@link RequirementsChecker#meetsRequirements(SourceRequirements)}.
 *
 * <p>{@code questMilestones} is looser than {@code quests}: a quest that is
 * IN_PROGRESS or FINISHED satisfies it, only NOT_STARTED fails.
 * {@code skillCapePerks} is satisfied when the player's real level in the named
 * skill is at least 99 (the level-99 proxy for owning the cape).
 *
 * <p>Quest state is driven the same way the existing checker tests drive it:
 * {@code Quest.getState(client)} runs a script and reads
 * {@code client.getIntStack()[0]}, where 2 = FINISHED, 1 = NOT_STARTED, and
 * any other value = IN_PROGRESS. The {@link Client} mock is reused for skill
 * levels via {@code getRealSkillLevel}. Strict stubbing is disabled because
 * the AND-clause short-circuits skip later stubs in failure tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequirementsCheckerD5Test
{
	private static final String QUEST_NAME = "DRAGON_SLAYER_II";

	@Mock
	private Client client;

	@Mock
	private PohTeleportInventory pohTeleportInventory;

	@Mock
	private EquippedItemState equippedItemState;

	@Mock
	private PlayerInventoryState playerInventoryState;

	private RequirementsChecker checker;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<RequirementsChecker> ctor = RequirementsChecker.class.getDeclaredConstructor(
			Client.class, PohTeleportInventory.class, EquippedItemState.class,
			PlayerInventoryState.class);
		ctor.setAccessible(true);
		checker = ctor.newInstance(client, pohTeleportInventory, equippedItemState, playerInventoryState);
	}

	// -- questMilestones --------------------------------------------------------

	@Test
	public void meetsRequirements_questMilestoneFinished_isTrue()
	{
		// intStack[0] = 2 -> FINISHED
		lenient().when(client.getIntStack()).thenReturn(new int[]{2});
		assertTrue(checker.meetsRequirements(questMilestoneReq()));
	}

	@Test
	public void meetsRequirements_questMilestoneInProgress_isTrue()
	{
		// intStack[0] = 0 (any value other than 1 or 2) -> IN_PROGRESS.
		// Started is enough for a milestone — this is the key difference from quests.
		lenient().when(client.getIntStack()).thenReturn(new int[]{0});
		assertTrue(checker.meetsRequirements(questMilestoneReq()));
	}

	@Test
	public void meetsRequirements_questMilestoneNotStarted_isFalse()
	{
		// intStack[0] = 1 -> NOT_STARTED
		lenient().when(client.getIntStack()).thenReturn(new int[]{1});
		assertFalse(checker.meetsRequirements(questMilestoneReq()));
	}

	@Test
	public void meetsRequirements_unknownQuestMilestoneEnum_isTrue()
	{
		// Fail-closed resolution logs a warning; an unresolved name is skipped,
		// so a requirement consisting only of an unknown name has no unmet entry.
		assertTrue(checker.meetsRequirements(questMilestoneReq("DOES_NOT_EXIST")));
	}

	// -- skillCapePerks ---------------------------------------------------------

	@Test
	public void meetsRequirements_skillCapePerkAtLevel99_isTrue()
	{
		lenient().when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(99);
		assertTrue(checker.meetsRequirements(skillCapePerkReq("CRAFTING")));
	}

	@Test
	public void meetsRequirements_skillCapePerkAboveLevel99_isTrue()
	{
		// Real level is capped at 99, but guard the >= boundary explicitly.
		lenient().when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(120);
		assertTrue(checker.meetsRequirements(skillCapePerkReq("FARMING")));
	}

	@Test
	public void meetsRequirements_skillCapePerkBelowLevel99_isFalse()
	{
		lenient().when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(98);
		assertFalse(checker.meetsRequirements(skillCapePerkReq("SLAYER")));
	}

	@Test
	public void meetsRequirements_unknownSkillCapePerkEnum_isTrue()
	{
		// Fail-closed: unknown skill name is skipped, so an otherwise-empty
		// requirement has no unmet entry. (RUNECRAFTING is intentionally wrong;
		// the real constant is RUNECRAFT.)
		assertTrue(checker.meetsRequirements(skillCapePerkReq("RUNECRAFTING")));
	}

	// -- Helpers ----------------------------------------------------------------

	private static SourceRequirements questMilestoneReq()
	{
		return questMilestoneReq(QUEST_NAME);
	}

	private static SourceRequirements questMilestoneReq(String questName)
	{
		return new SourceRequirements(
			null, null, null, null, null, null, null,
			Collections.singletonList(questName), null);
	}

	private static SourceRequirements skillCapePerkReq(String skillName)
	{
		return new SourceRequirements(
			null, null, null, null, null, null, null,
			null, Collections.singletonList(skillName));
	}
}
