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
 * Gson deserialisation tests for the {@code inventoryItemIds} field on
 * {@link SourceRequirements}.
 *
 * <p>Covers absent / null / empty / single-element / multi-element JSON shapes,
 * legacy backwards compatibility, and Gson behaviour on malformed input. The
 * field is purely additive: existing drop_rates.json sources that omit it must
 * continue to deserialise with {@code getInventoryItemIds() == null}.
 *
 * <p>Pairs with {@link SourceRequirementsB0Test} which covers the same matrix
 * for {@code pohTeleports} and {@code equippedItemIds} introduced by Tier B0
 * (PR #623). This class adds the same coverage for the inventory-item field
 * that closes the wieldable-but-used-from-inventory teleport gap left by B0.
 */
public class SourceRequirementsInventoryItemsGsonTest
{
	private static final Gson GSON = new GsonBuilder().create();

	// -- Backwards compatibility -------------------------------------------------

	@Test
	public void deserialise_legacyJsonWithoutField_inventoryItemIdsIsNull()
	{
		String json = "{\"quests\":[\"DESERT_TREASURE_II\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getInventoryItemIds());
	}

	@Test
	public void deserialise_legacyJsonWithAllPriorFields_inventoryItemIdsIsNull()
	{
		// Sanity guard: legacy JSON that exercises every pre-existing field must
		// still produce a parsed object with inventoryItemIds == null.
		String json = "{"
			+ "\"quests\":[\"DESERT_TREASURE_II\"],"
			+ "\"skills\":[{\"skill\":\"SLAYER\",\"level\":95}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("DESERT_TREASURE_II"), req.getQuests());
		assertNotNull(req.getSkills());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertEquals(Collections.singletonList("JEWELLERY_BOX_FANCY"), req.getPohTeleports());
		assertEquals(Collections.singletonList(22557), req.getEquippedItemIds());
		assertNull(req.getInventoryItemIds());
	}

	// -- Field-only shapes -------------------------------------------------------

	@Test
	public void deserialise_inventoryItemIdsNull_isNull()
	{
		String json = "{\"inventoryItemIds\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getInventoryItemIds());
	}

	@Test
	public void deserialise_inventoryItemIdsEmpty_isEmptyNotNull()
	{
		String json = "{\"inventoryItemIds\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getInventoryItemIds());
		assertTrue(req.getInventoryItemIds().isEmpty());
	}

	@Test
	public void deserialise_inventoryItemIdsSingle_oneItem()
	{
		// Pharaoh's sceptre charges-5 placeholder ID; value chosen for shape, not lookup.
		String json = "{\"inventoryItemIds\":[26945]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList(26945), req.getInventoryItemIds());
	}

	@Test
	public void deserialise_inventoryItemIdsMultiple_preservesOrder()
	{
		// Zul-andra teleport scroll + Quetzal whistle + Crystal teleport seed placeholders.
		String json = "{\"inventoryItemIds\":[12938,29782,23959]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList(12938, 29782, 23959), req.getInventoryItemIds());
	}

	@Test
	public void deserialise_inventoryItemIdsNegativeId_parsed()
	{
		// Gson does not validate item-ID positivity at parse time; that contract
		// lives in the evaluator (RequirementsChecker) and the lint layer above.
		// This test pins current behaviour so a future evaluator-side guard is
		// an explicit decision rather than an accidental break.
		String json = "{\"inventoryItemIds\":[-1]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList(-1), req.getInventoryItemIds());
	}

	@Test
	public void deserialise_inventoryItemIdsContainingString_throws()
	{
		// Mirrors Gson's default strict number-parsing for typed list elements.
		String json = "{\"inventoryItemIds\":[\"not-an-int\"]}";
		assertThrows(JsonSyntaxException.class,
			() -> GSON.fromJson(json, SourceRequirements.class));
	}

	@Test
	public void deserialise_inventoryItemIdsContainingFloat_throws()
	{
		// Gson rejects non-integer JSON numbers for the declared Integer element
		// type. This pins current behaviour so the malformed-input contract is
		// explicit rather than implicit.
		String json = "{\"inventoryItemIds\":[26945.7]}";
		assertThrows(JsonSyntaxException.class,
			() -> GSON.fromJson(json, SourceRequirements.class));
	}

	// -- All six fields populated -----------------------------------------------

	@Test
	public void deserialise_allSixFieldsPopulated_parsesAll()
	{
		String json = "{"
			+ "\"quests\":[\"TROLL_STRONGHOLD\"],"
			+ "\"skills\":[{\"skill\":\"STRENGTH\",\"level\":70}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557],"
			+ "\"inventoryItemIds\":[26945,23959]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("TROLL_STRONGHOLD"), req.getQuests());
		assertEquals(1, req.getSkills().size());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertEquals(Collections.singletonList("JEWELLERY_BOX_FANCY"), req.getPohTeleports());
		assertEquals(Collections.singletonList(22557), req.getEquippedItemIds());
		assertEquals(Arrays.asList(26945, 23959), req.getInventoryItemIds());
	}

	// -- equals / hashCode -------------------------------------------------------

	@Test
	public void equals_sameInventoryItemIds_areEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null,
			Arrays.asList(26945, 23959), null, null, null);
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null,
			Arrays.asList(26945, 23959), null, null, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equals_differingInventoryItemIds_notEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(26945), null, null, null);
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(23959), null, null, null);
		assertTrue(!a.equals(b));
	}

	// -- Round-trip --------------------------------------------------------------

	@Test
	public void gsonRoundTrip_withInventoryItemIds_preservesStructure()
	{
		SourceRequirements original = new SourceRequirements(
			Collections.singletonList("DESERT_TREASURE_II"),
			null,
			Collections.singletonList("FREMENNIK_HARD"),
			Arrays.asList("JEWELLERY_BOX_FANCY"),
			Arrays.asList(22557),
			Arrays.asList(26945, 23959), null, null, null);
		String json = GSON.toJson(original);
		SourceRequirements restored = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(original, restored);
	}
}
