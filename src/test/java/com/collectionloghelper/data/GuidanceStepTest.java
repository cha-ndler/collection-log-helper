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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link GuidanceStep#resolveDescription(Integer)} and the
 * {@code perItemStepDescription} schema field.
 *
 * <p>Acceptance criteria verified:
 * <ol>
 *   <li>resolveDescription(null) returns the static description.</li>
 *   <li>resolveDescription(itemId) returns the override when the map has a matching entry.</li>
 *   <li>resolveDescription(itemId) falls back to static when the map exists but has no entry for the target.</li>
 *   <li>resolveDescription(itemId) falls back to static when the map is null.</li>
 *   <li>Schema deserialisation: JSON without {@code perItemStepDescription} yields a null field (backwards-compatible).</li>
 *   <li>Schema deserialisation: JSON with {@code perItemStepDescription} populates the map correctly.</li>
 * </ol>
 */
public class GuidanceStepTest
{
	private static final int ITEM_TIER1 = 1001;
	private static final int ITEM_TIER5 = 1005;
	private static final int ITEM_UNKNOWN = 9999;

	private static final String STATIC_DESC = "Kill soldiers for armour";
	private static final String TIER1_DESC = "Kill Tier 1 soldiers for tier 1 armour (40 kills)";
	private static final String TIER5_DESC = "Kill Tier 5 soldiers for tier 5 armour (200 kills)";

	// ── resolveDescription: null targetItemId ──────────────────────────────────

	@Test
	public void resolveDescription_nullTarget_returnsStaticDescription()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(ITEM_TIER1, TIER1_DESC);

		GuidanceStep step = stepWithOverrides(STATIC_DESC, overrides);

		assertEquals(STATIC_DESC, step.resolveDescription(null));
	}

	// ── resolveDescription: map has matching entry ─────────────────────────────

	@Test
	public void resolveDescription_targetInMap_returnsOverride()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(ITEM_TIER1, TIER1_DESC);
		overrides.put(ITEM_TIER5, TIER5_DESC);

		GuidanceStep step = stepWithOverrides(STATIC_DESC, overrides);

		assertEquals(TIER1_DESC, step.resolveDescription(ITEM_TIER1));
		assertEquals(TIER5_DESC, step.resolveDescription(ITEM_TIER5));
	}

	// ── resolveDescription: map exists but target not in it ───────────────────

	@Test
	public void resolveDescription_targetNotInMap_returnsStaticDescription()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(ITEM_TIER1, TIER1_DESC);

		GuidanceStep step = stepWithOverrides(STATIC_DESC, overrides);

		assertEquals(STATIC_DESC, step.resolveDescription(ITEM_UNKNOWN));
	}

	// ── resolveDescription: null map ──────────────────────────────────────────

	@Test
	public void resolveDescription_nullMap_returnsStaticDescription()
	{
		GuidanceStep step = stepWithOverrides(STATIC_DESC, null);

		assertEquals(STATIC_DESC, step.resolveDescription(ITEM_TIER1));
	}

	// ── resolveDescription: empty map ─────────────────────────────────────────

	@Test
	public void resolveDescription_emptyMap_returnsStaticDescription()
	{
		GuidanceStep step = stepWithOverrides(STATIC_DESC, Collections.emptyMap());

		assertEquals(STATIC_DESC, step.resolveDescription(ITEM_TIER1));
	}

	// ── Schema deserialisation: backwards-compatible (no field) ───────────────

	@Test
	public void deserialise_jsonWithoutPerItemStepDescription_fieldIsNull()
	{
		Gson gson = new GsonBuilder().create();
		String json = "{"
			+ "\"description\":\"Kill soldiers\","
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertEquals("Kill soldiers", step.getDescription());
		assertNull("perItemStepDescription should be null when absent from JSON",
			step.getPerItemStepDescription());
	}

	// ── Schema deserialisation: field present ─────────────────────────────────

	@Test
	public void deserialise_jsonWithPerItemStepDescription_populatesMap()
	{
		Gson gson = new GsonBuilder().create();
		String json = "{"
			+ "\"description\":\"Kill soldiers\","
			+ "\"perItemStepDescription\":{\"1001\":\"Tier 1 override\",\"1005\":\"Tier 5 override\"},"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		// Gson deserialises JSON object keys as Strings; the Map<Integer,String> type
		// token causes Gson to parse "1001" as Integer 1001 via standard type adaptation.
		Map<Integer, String> map = step.getPerItemStepDescription();
		assertEquals("Tier 1 override", map.get(1001));
		assertEquals("Tier 5 override", map.get(1005));
		assertEquals("Kill soldiers", step.resolveDescription(9999));
		assertEquals("Tier 1 override", step.resolveDescription(1001));
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private static GuidanceStep stepWithOverrides(String description,
		Map<Integer, String> perItemStepDescription)
	{
		return new GuidanceStep(
			description,
			perItemStepDescription,
			0, 0, 0,       // worldX, worldY, worldPlane
			0, null, null,  // npcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,    // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,           // completionNpcIds
			null,           // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			0, 0,           // completionVarbitId, completionVarbitValue
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			null,           // highlightWidgetIds
			0, 0,           // loopBackToStep, loopCount
			null,           // skipIfHasAnyItemIds
			null,           // dynamicItemObjectTiers
			null,           // completionZone
			null,           // conditionalAlternatives
			null            // section
		);
	}
}
