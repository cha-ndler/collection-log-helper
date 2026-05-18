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
package com.collectionloghelper.guidance.bosses;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.lang.reflect.Constructor;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CerberusGuidanceTest
{
	private CerberusGuidance guidance;

	@Before
	public void setUp() throws Exception
	{
		Constructor<CerberusGuidance> ctor = CerberusGuidance.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		guidance = ctor.newInstance();
	}

	@Test
	public void getSteps_returnsThreeSteps()
	{
		List<GuidanceStep> steps = guidance.getSteps(null, null);
		assertEquals("Cerberus guidance must produce exactly 3 steps", 3, steps.size());
	}

	@Test
	public void getSteps_firstStep_isTravelToTaverley()
	{
		List<GuidanceStep> steps = guidance.getSteps(null, null);
		GuidanceStep travelStep = steps.get(0);
		assertEquals(CompletionCondition.ARRIVE_AT_TILE, travelStep.getCompletionCondition());
		assertEquals("Travel", travelStep.getSection());
		assertEquals(1310, travelStep.getWorldX());
		assertEquals(1251, travelStep.getWorldY());
		assertEquals(0, travelStep.getWorldPlane());
		assertEquals(8, travelStep.getCompletionDistance());
	}

	@Test
	public void getSteps_secondStep_isGateStep()
	{
		List<GuidanceStep> steps = guidance.getSteps(null, null);
		GuidanceStep gateStep = steps.get(1);
		assertEquals(CompletionCondition.MANUAL, gateStep.getCompletionCondition());
		assertEquals(CerberusGuidance.IRON_WINCH_OBJECT_ID, gateStep.getObjectId());
		assertEquals("Turn", gateStep.getObjectInteractAction());
		assertEquals("Travel", gateStep.getSection());
	}

	@Test
	public void getSteps_thirdStep_isKillCerberus()
	{
		List<GuidanceStep> steps = guidance.getSteps(null, null);
		GuidanceStep killStep = steps.get(2);
		assertEquals(CompletionCondition.ACTOR_DEATH, killStep.getCompletionCondition());
		assertEquals(CerberusGuidance.CERBERUS_NPC_ID, killStep.getNpcId());
		assertEquals(CerberusGuidance.CERBERUS_NPC_ID, killStep.getCompletionNpcId());
		assertEquals("Attack", killStep.getInteractAction());
		assertEquals("Combat", killStep.getSection());
		assertNotNull("Kill step must have recommended items", killStep.getRecommendedItemIds());
		assertFalse("Recommended items must not be empty", killStep.getRecommendedItemIds().isEmpty());
	}

	@Test
	public void isStepSatisfied_alwaysReturnsFalse()
	{
		List<GuidanceStep> steps = guidance.getSteps(null, null);
		for (int i = 0; i < steps.size(); i++)
		{
			assertFalse(
				"isStepSatisfied must always return false for step " + i,
				guidance.isStepSatisfied(null, steps.get(i), i));
		}
	}
}
