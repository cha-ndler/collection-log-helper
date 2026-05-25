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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link GuidanceStep#resolvePriority(Integer)} -- the
 * single-branch evaluator that backs the B4.3.4 Phase 1+2 schema field
 * {@code perItemStepPriority}.
 *
 * <p>The contract mirrors {@code resolveNpcId} / {@code resolveDescription}:
 * an override is returned when the map has an entry for the active target
 * item id; otherwise the default (0) is returned. There is no production
 * sequencer integration in Phase 1+2 -- the helper is the entire surface.
 */
public class PerItemStepPriorityEvaluatorTest
{
	// Synthetic Ahrim-kill-step fixture mirroring the B4.3.4 design table.
	private static final int AHRIM_HOOD = 4708;
	private static final int AHRIM_ROBETOP = 4712;
	private static final int AHRIM_ROBESKIRT = 4714;
	private static final int AHRIM_STAFF = 4710;

	private static final int DHAROK_HELM = 4716;
	private static final int BOLT_RACK = 4740; // not owned by any per-brother step

	private static final int PRIORITY = 100;

	// -- null target ------------------------------------------------------------

	@Test
	public void resolvePriority_nullTarget_returnsZero()
	{
		Map<Integer, Integer> map = new HashMap<>();
		map.put(AHRIM_HOOD, PRIORITY);

		GuidanceStep step = stepWithPriority(map);

		assertEquals(0, step.resolvePriority(null),
			"null target id must always resolve to default priority 0");
	}

	// -- null map ---------------------------------------------------------------

	@Test
	public void resolvePriority_nullMap_returnsZero()
	{
		GuidanceStep step = stepWithPriority(null);

		assertEquals(0, step.resolvePriority(AHRIM_HOOD),
			"null perItemStepPriority map must always resolve to default priority 0");
	}

	// -- empty map --------------------------------------------------------------

	@Test
	public void resolvePriority_emptyMap_returnsZero()
	{
		GuidanceStep step = stepWithPriority(Collections.emptyMap());

		assertEquals(0, step.resolvePriority(AHRIM_HOOD),
			"empty perItemStepPriority map must always resolve to default priority 0");
	}

	// -- matching entry returns override ----------------------------------------

	@Test
	public void resolvePriority_matchingEntry_returnsOverride()
	{
		Map<Integer, Integer> map = new HashMap<>();
		map.put(AHRIM_HOOD, PRIORITY);
		map.put(AHRIM_ROBETOP, PRIORITY);
		map.put(AHRIM_ROBESKIRT, PRIORITY);
		map.put(AHRIM_STAFF, PRIORITY);

		GuidanceStep step = stepWithPriority(map);

		assertEquals(PRIORITY, step.resolvePriority(AHRIM_HOOD));
		assertEquals(PRIORITY, step.resolvePriority(AHRIM_ROBETOP));
		assertEquals(PRIORITY, step.resolvePriority(AHRIM_ROBESKIRT));
		assertEquals(PRIORITY, step.resolvePriority(AHRIM_STAFF));
	}

	// -- non-matching entry falls back to 0 ------------------------------------

	@Test
	public void resolvePriority_targetNotInMap_returnsZero()
	{
		// Ahrim-kill step: owns only Ahrim set pieces.
		Map<Integer, Integer> map = new HashMap<>();
		map.put(AHRIM_HOOD, PRIORITY);
		map.put(AHRIM_ROBETOP, PRIORITY);

		GuidanceStep step = stepWithPriority(map);

		assertEquals(0, step.resolvePriority(DHAROK_HELM),
			"a Dharok-helm hunt against the Ahrim-kill step must resolve to 0 (not Ahrim's owner)");
		assertEquals(0, step.resolvePriority(BOLT_RACK),
			"Bolt rack has no per-brother entry on any step and must resolve to 0 everywhere");
	}

	// -- distinct priority values per item are preserved ---------------------

	@Test
	public void resolvePriority_distinctValues_preservedPerKey()
	{
		Map<Integer, Integer> map = new HashMap<>();
		map.put(100, 1);
		map.put(200, 50);
		map.put(300, 999);

		GuidanceStep step = stepWithPriority(map);

		assertEquals(1, step.resolvePriority(100));
		assertEquals(50, step.resolvePriority(200));
		assertEquals(999, step.resolvePriority(300));
		assertEquals(0, step.resolvePriority(400));
	}

	// -- negative priorities are passed through (sequencer may interpret) -----

	@Test
	public void resolvePriority_negativeValue_returnedVerbatim()
	{
		Map<Integer, Integer> map = new HashMap<>();
		map.put(AHRIM_HOOD, -7);

		GuidanceStep step = stepWithPriority(map);

		assertEquals(-7, step.resolvePriority(AHRIM_HOOD),
			"resolver must return the mapped value verbatim; semantic interpretation belongs to the sequencer");
	}

	// -- helper -----------------------------------------------------------------

	private static GuidanceStep stepWithPriority(Map<Integer, Integer> perItemStepPriority)
	{
		return new GuidanceStep(
			"Kill the brother",
			null,                       // perItemStepDescription
			0, 0, 0,                    // worldX, worldY, worldPlane
			0, null, null, null,        // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,                 // travelTip, requiredItemIds
			null,                       // perItemRequiredItemIds
			null,                       // recommendedItemIds
			null,                       // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,                 // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,                       // completionNpcIds
			null,                       // worldMessage
			0, null, null,              // objectId, objectIds, objectInteractAction
			null, null,                 // highlightItemIds, groundItemIds
			null,                       // completionChatPattern
			0, 0,                       // completionVarbitId, completionVarbitValue
			false,                      // useItemOnObject
			0,                          // objectMaxDistance
			null,                       // objectFilterTiles
			null,                       // highlightWidgetIds
			0, 0,                       // loopBackToStep, loopCount
			null,                       // skipIfHasAnyItemIds
			null,                       // dynamicItemObjectTiers
			null,                       // completionZone
			null,                       // conditionalAlternatives
			null,                       // section
			null,                       // waypoints
			null,                       // dynamicTargetEvaluator
			null,                       // conditionTree
			perItemStepPriority         // perItemStepPriority (under test)
		);
	}
}
