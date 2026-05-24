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
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gson deserialisation tests for the {@code inventoryItemIdsAny} OR-semantic
 * field on {@link SourceRequirements}.
 *
 * <p>Covers absent / null / empty / single-element / multi-element JSON shapes,
 * backwards compatibility with the existing six-field schema, and Gson
 * behaviour on malformed input. The field is purely additive: existing
 * drop_rates.json sources that omit it must continue to deserialise with
 * {@code getInventoryItemIdsAny() == null}.
 *
 * <p>Pairs with {@link SourceRequirementsInventoryItemsGsonTest} which covers
 * the AND-semantic sibling {@code inventoryItemIds}. This class adds the same
 * coverage for the OR-semantic field that closes the multi-charge-tier
 * teleport gap (Pharaoh's sceptre 1-5 + Jeweled, Quetzal whistle tiers,
 * Crystal teleport charge tiers).
 */
public class SourceRequirementsInventoryItemsAnyGsonTest
{
	private static final Gson GSON = new GsonBuilder().create();

	// -- Backwards compatibility -------------------------------------------------

	@Test
	public void deserialise_legacyJsonWithoutField_inventoryItemIdsAnyIsNull()
	{
		String json = "{\"quests\":[\"DESERT_TREASURE_II\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getInventoryItemIdsAny());
	}

	@Test
	public void deserialise_legacyJsonWithAllPriorFields_inventoryItemIdsAnyIsNull()
	{
		// Sanity guard: legacy JSON that exercises every pre-existing field,
		// including the AND-semantic inventoryItemIds, must still produce a
		// parsed object with inventoryItemIdsAny == null.
		String json = "{"
			+ "\"quests\":[\"DESERT_TREASURE_II\"],"
			+ "\"skills\":[{\"skill\":\"SLAYER\",\"level\":95}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557],"
			+ "\"inventoryItemIds\":[12938]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("DESERT_TREASURE_II"), req.getQuests());
		assertNotNull(req.getSkills());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertEquals(Collections.singletonList("JEWELLERY_BOX_FANCY"), req.getPohTeleports());
		assertEquals(Collections.singletonList(22557), req.getEquippedItemIds());
		assertEquals(Collections.singletonList(12938), req.getInventoryItemIds());
		assertNull(req.getInventoryItemIdsAny());
	}

	// -- Field-only shapes -------------------------------------------------------

	@Test
	public void deserialise_inventoryItemIdsAnyNull_isNull()
	{
		String json = "{\"inventoryItemIdsAny\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getInventoryItemIdsAny());
	}

	@Test
	public void deserialise_inventoryItemIdsAnyEmpty_isEmptyNotNull()
	{
		String json = "{\"inventoryItemIdsAny\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getInventoryItemIdsAny());
		assertTrue(req.getInventoryItemIdsAny().isEmpty());
	}

	@Test
	public void deserialise_inventoryItemIdsAnySingle_oneItem()
	{
		// Pharaoh's sceptre charges-5 placeholder ID; value chosen for shape, not lookup.
		String json = "{\"inventoryItemIdsAny\":[26945]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList(26945), req.getInventoryItemIdsAny());
	}

	@Test
	public void deserialise_inventoryItemIdsAnyMultiple_preservesOrder()
	{
		// Pharaoh's sceptre charges 1-5 plus Jeweled: 6-tier multi-charge variant
		// set, exactly the shape this field exists to express.
		String json = "{\"inventoryItemIdsAny\":[26945,26946,26947,26948,26949,26950]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList(26945, 26946, 26947, 26948, 26949, 26950),
			req.getInventoryItemIdsAny());
	}

	@Test
	public void deserialise_inventoryItemIdsAnyNegativeId_parsed()
	{
		// Gson does not validate item-ID positivity at parse time; that contract
		// lives in the evaluator (RequirementsChecker) and the lint layer above.
		// Mirrors the AND-semantic sibling so the malformed-input contract is
		// uniform across both inventory fields.
		String json = "{\"inventoryItemIdsAny\":[-1]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList(-1), req.getInventoryItemIdsAny());
	}

	@Test
	public void deserialise_inventoryItemIdsAnyContainingString_throws()
	{
		// Mirrors Gson's default strict number-parsing for typed list elements.
		String json = "{\"inventoryItemIdsAny\":[\"not-an-int\"]}";
		assertThrows(JsonSyntaxException.class,
			() -> GSON.fromJson(json, SourceRequirements.class));
	}

	@Test
	public void deserialise_inventoryItemIdsAnyContainingFloat_throws()
	{
		// Gson rejects non-integer JSON numbers for the declared Integer element
		// type. Pins current behaviour so the malformed-input contract is
		// explicit rather than implicit.
		String json = "{\"inventoryItemIdsAny\":[26945.7]}";
		assertThrows(JsonSyntaxException.class,
			() -> GSON.fromJson(json, SourceRequirements.class));
	}

	// -- All seven fields populated ---------------------------------------------

	@Test
	public void deserialise_allSevenFieldsPopulated_parsesAll()
	{
		String json = "{"
			+ "\"quests\":[\"TROLL_STRONGHOLD\"],"
			+ "\"skills\":[{\"skill\":\"STRENGTH\",\"level\":70}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557],"
			+ "\"inventoryItemIds\":[12938],"
			+ "\"inventoryItemIdsAny\":[26945,26946,26947]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("TROLL_STRONGHOLD"), req.getQuests());
		assertEquals(1, req.getSkills().size());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertEquals(Collections.singletonList("JEWELLERY_BOX_FANCY"), req.getPohTeleports());
		assertEquals(Collections.singletonList(22557), req.getEquippedItemIds());
		assertEquals(Collections.singletonList(12938), req.getInventoryItemIds());
		assertEquals(Arrays.asList(26945, 26946, 26947), req.getInventoryItemIdsAny());
	}

	// -- equals / hashCode -------------------------------------------------------

	@Test
	public void equals_sameInventoryItemIdsAny_areEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null, null,
			Arrays.asList(26945, 26946));
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null, null,
			Arrays.asList(26945, 26946));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equals_differingInventoryItemIdsAny_notEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null, null,
			Collections.singletonList(26945));
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null, null,
			Collections.singletonList(26946));
		assertTrue(!a.equals(b));
	}

	@Test
	public void equals_inventoryItemIdsAnyVsInventoryItemIds_notEqual()
	{
		// The two list fields are distinct positional slots even when populated
		// with identical IDs. equals/hashCode must distinguish them, otherwise
		// the evaluator would not be able to choose AND vs OR semantics.
		SourceRequirements andOnly = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(26945), null);
		SourceRequirements anyOnly = new SourceRequirements(
			null, null, null, null, null, null,
			Collections.singletonList(26945));
		assertTrue(!andOnly.equals(anyOnly));
	}

	// -- Round-trip --------------------------------------------------------------

	@Test
	public void gsonRoundTrip_withInventoryItemIdsAny_preservesStructure()
	{
		SourceRequirements original = new SourceRequirements(
			Collections.singletonList("DESERT_TREASURE_II"),
			null,
			Collections.singletonList("FREMENNIK_HARD"),
			Arrays.asList("JEWELLERY_BOX_FANCY"),
			Arrays.asList(22557),
			Arrays.asList(12938),
			Arrays.asList(26945, 26946, 26947, 26948, 26949, 26950));
		String json = GSON.toJson(original);
		SourceRequirements restored = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(original, restored);
	}
}
