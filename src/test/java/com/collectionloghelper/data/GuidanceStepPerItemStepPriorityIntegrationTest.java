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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration test asserting that the new {@code perItemStepPriority} schema
 * field on {@link GuidanceStep} (B4.3.4 Phase 1+2) does not alter the behaviour
 * of any other resolver method when the field is null.
 *
 * <p>Mirrors the {@code B1GuidanceStepIntegrationTest} pattern: a synthetic
 * step with the new field set is exercised end-to-end via Gson serialisation
 * + the existing resolver helpers, confirming the new field is orthogonal to
 * description / npc / required-items / conditional alternatives / condition tree.
 */
public class GuidanceStepPerItemStepPriorityIntegrationTest
{
	private final Gson gson = new GsonBuilder().create();

	// -- fall-through: null field changes nothing ------------------------------

	@Test
	public void nullField_doesNotChangeAnyOtherResolver()
	{
		Map<Integer, String> descOverrides = new HashMap<>();
		descOverrides.put(4708, "Hunt Ahrim's hood");

		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(4708, 1672);

		GuidanceStep step = stepWith(
			"Generic kill step",
			descOverrides,
			999,
			npcOverrides,
			null /* perItemStepPriority */);

		// All sibling resolvers still work as before.
		assertEquals("Hunt Ahrim's hood", step.resolveDescription(4708));
		assertEquals("Generic kill step", step.resolveDescription(null));
		assertEquals(1672, step.resolveNpcId(4708));
		assertEquals(999, step.resolveNpcId(null));

		// And the new resolver returns the default for any input when the map is null.
		assertEquals(0, step.resolvePriority(4708));
		assertEquals(0, step.resolvePriority(null));
		assertNull(step.getPerItemStepPriority(),
			"null perItemStepPriority must remain null after construction");
	}

	// -- populated field works independently of siblings ----------------------

	@Test
	public void populatedField_returnsOverrideWithoutDisturbingSiblings()
	{
		Map<Integer, String> descOverrides = new HashMap<>();
		descOverrides.put(4708, "Hunt Ahrim's hood");

		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(4708, 1672);

		Map<Integer, Integer> priorityMap = new HashMap<>();
		priorityMap.put(4708, 100);
		priorityMap.put(4712, 100);

		GuidanceStep step = stepWith(
			"Generic kill step",
			descOverrides,
			999,
			npcOverrides,
			priorityMap);

		assertEquals("Hunt Ahrim's hood", step.resolveDescription(4708));
		assertEquals(1672, step.resolveNpcId(4708));
		assertEquals(100, step.resolvePriority(4708));
		assertEquals(100, step.resolvePriority(4712));
		assertEquals(0, step.resolvePriority(4716),
			"Dharok helm is not in this Ahrim-kill step's priority map; resolver must return 0");
	}

	// -- Gson round-trip with the new field populated -------------------------

	@Test
	public void gsonRoundTrip_preservesPerItemStepPriority()
	{
		String json = "{"
			+ "\"description\":\"Kill Ahrim\","
			+ "\"perItemStepPriority\":{\"4708\":100,\"4712\":100,\"4714\":100,\"4710\":100},"
			+ "\"completionCondition\":\"VARBIT_AT_LEAST\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull(step.getPerItemStepPriority());
		assertEquals(4, step.getPerItemStepPriority().size());
		assertEquals(100, step.resolvePriority(4708));
		assertEquals(100, step.resolvePriority(4712));
		assertEquals(100, step.resolvePriority(4714));
		assertEquals(100, step.resolvePriority(4710));
		assertEquals(0, step.resolvePriority(4716),
			"a key absent from the deserialised map must resolve to default 0");
	}

	// -- helper -----------------------------------------------------------------

	private static GuidanceStep stepWith(
		String description,
		Map<Integer, String> perItemStepDescription,
		int npcId,
		Map<Integer, Integer> perItemNpcId,
		Map<Integer, Integer> perItemStepPriority)
	{
		return new GuidanceStep(
			description,
			perItemStepDescription,
			0, 0, 0,                    // worldX, worldY, worldPlane
			npcId, perItemNpcId, null, null,
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
			perItemStepPriority         // perItemStepPriority
		);
	}
}
