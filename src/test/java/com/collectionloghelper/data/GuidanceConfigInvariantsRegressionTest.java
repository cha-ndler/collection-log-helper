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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Build-enforced ratchet for the guidance-step completion invariants derived
 * from the live engine (see {@code scripts/audit_guidance_config.py} and
 * {@code docs/guidance-audit/}). Each invariant below is a field the engine
 * REQUIRES for a given {@link CompletionCondition} to ever fire; a step missing
 * it can never auto-advance.
 *
 * <p>Two flavours of assertion:
 * <ul>
 *   <li><b>Hard guards</b> ({@code == 0}) — invariants the data currently
 *       satisfies everywhere. New data must never introduce a violation.</li>
 *   <li><b>Ratchet ceilings</b> ({@code <= N}) — known backlogs (#739/A, #739/B,
 *       and the new ARRIVE_AT_ZONE class). The count may only shrink: a fix is
 *       always allowed, a regression fails the build. Lower the ceiling as the
 *       backlog is worked down so the ratchet stays tight.</li>
 * </ul>
 *
 * <p>This is the CI half of the audit: the Python analyzer discovers and
 * calibrates; this test makes the invariants un-regressable on every
 * {@code ./gradlew build}.
 */
public class GuidanceConfigInvariantsRegressionTest
{
	// Known backlog ceilings as of master @ 3015c35d. DECREMENT as fixes land.
	private static final int ACTOR_DEATH_MISSING_NPC_CEILING = 5;   // #739/A
	private static final int LOOP_NEVER_ENGAGES_CEILING = 15;       // #739/B
	private static final int ARRIVE_AT_ZONE_MISSING_ZONE_CEILING = 5; // NEW (N1)

	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);
		database.load();
	}

	// ---- Hard guards: must stay at zero -------------------------------------

	@Test
	@DisplayName("ITEM_OBTAINED / INVENTORY_* steps all declare a completion item id")
	public void itemConditionsHaveItemId()
	{
		assertEquals(0, violations((step, cond) ->
				(cond == CompletionCondition.ITEM_OBTAINED
					|| cond == CompletionCondition.INVENTORY_HAS_ITEM
					|| cond == CompletionCondition.INVENTORY_NOT_HAS_ITEM)
					&& step.getCompletionItemId() <= 0).size(),
			"item-based completion needs completionItemId > 0 (CompletionChecker.java:96-121)");
	}

	@Test
	@DisplayName("NPC_TALKED_TO steps declare a completion npc id")
	public void talkConditionsHaveNpcId()
	{
		assertEquals(0, violations((step, cond) ->
				cond == CompletionCondition.NPC_TALKED_TO && step.getCompletionNpcId() <= 0).size(),
			"NPC_TALKED_TO needs completionNpcId > 0 (CompletionChecker.java:142-143)");
	}

	@Test
	@DisplayName("CHAT_MESSAGE_RECEIVED steps declare a pattern")
	public void chatConditionsHavePattern()
	{
		assertEquals(0, violations((step, cond) ->
				cond == CompletionCondition.CHAT_MESSAGE_RECEIVED
					&& (step.getCompletionChatPattern() == null
						|| step.getCompletionChatPattern().isEmpty())).size(),
			"CHAT_MESSAGE_RECEIVED needs completionChatPattern (CompletionChecker.java:269-270)");
	}

	@Test
	@DisplayName("VARBIT_AT_LEAST steps declare a varbit id")
	public void varbitConditionsHaveVarbitId()
	{
		assertEquals(0, violations((step, cond) ->
				cond == CompletionCondition.VARBIT_AT_LEAST && step.getCompletionVarbitId() <= 0).size(),
			"VARBIT_AT_LEAST needs completionVarbitId > 0 (CompletionChecker.java:153-154)");
	}

	@Test
	@DisplayName("no loopBackToStep points outside its sequence")
	public void loopTargetsInRange()
	{
		List<String> bad = new ArrayList<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (int i = 0; i < steps.size(); i++)
			{
				int lbs = steps.get(i).getLoopBackToStep();
				if (lbs > 0 && (lbs < 1 || lbs > steps.size()))
				{
					bad.add(source.getName() + " step[" + i + "] loopBackToStep=" + lbs);
				}
			}
		}
		assertEquals(0, bad.size(),
			"loopBackToStep must be a valid 1.." + "len step number (StepAdvancer.java:142): " + bad);
	}

	// ---- Ratchet ceilings: may only shrink ----------------------------------

	@Test
	@DisplayName("ACTOR_DEATH-without-npc backlog does not grow (#739/A)")
	public void actorDeathMissingNpcRatchet()
	{
		List<String> bad = violations((step, cond) ->
			cond == CompletionCondition.ACTOR_DEATH
				&& step.getCompletionNpcId() <= 0
				&& (step.getCompletionNpcIds() == null || step.getCompletionNpcIds().isEmpty()));
		assertTrue(bad.size() <= ACTOR_DEATH_MISSING_NPC_CEILING,
			"ACTOR_DEATH steps with no completion npc id never auto-advance; ceiling is "
				+ ACTOR_DEATH_MISSING_NPC_CEILING + " but found " + bad.size() + ": " + bad);
	}

	@Test
	@DisplayName("loopBackToStep-without-loopCount backlog does not grow (#739/B)")
	public void loopNeverEngagesRatchet()
	{
		List<String> bad = new ArrayList<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (int i = 0; i < steps.size(); i++)
			{
				GuidanceStep step = steps.get(i);
				if (step.getLoopBackToStep() > 0 && step.getLoopCount() <= 0)
				{
					bad.add(source.getName() + " step[" + i + "]");
				}
			}
		}
		assertTrue(bad.size() <= LOOP_NEVER_ENGAGES_CEILING,
			"loopBackToStep set but loopCount 0/absent never loops (StepAdvancer.java:135-137); "
				+ "ceiling is " + LOOP_NEVER_ENGAGES_CEILING + " but found " + bad.size() + ": " + bad);
	}

	@Test
	@DisplayName("ARRIVE_AT_ZONE-without-zone backlog does not grow (N1)")
	public void arriveAtZoneMissingZoneRatchet()
	{
		List<String> bad = violations((step, cond) ->
			cond == CompletionCondition.ARRIVE_AT_ZONE && step.getZone() == null);
		assertTrue(bad.size() <= ARRIVE_AT_ZONE_MISSING_ZONE_CEILING,
			"ARRIVE_AT_ZONE steps with no completionZone never auto-advance "
				+ "(GuidanceStep.getZone / CompletionChecker.java:169-170); ceiling is "
				+ ARRIVE_AT_ZONE_MISSING_ZONE_CEILING + " but found " + bad.size() + ": " + bad);
	}

	// ---- helper -------------------------------------------------------------

	private interface StepPredicate
	{
		boolean test(GuidanceStep step, CompletionCondition cond);
	}

	private List<String> violations(StepPredicate predicate)
	{
		List<String> out = new ArrayList<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (int i = 0; i < steps.size(); i++)
			{
				GuidanceStep step = steps.get(i);
				CompletionCondition cond = step.getCompletionCondition();
				if (cond != null && predicate.test(step, cond))
				{
					out.add(source.getName() + " step[" + i + "] (" + cond + ")");
				}
			}
		}
		return out;
	}
}
