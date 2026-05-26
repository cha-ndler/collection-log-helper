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
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gson serialisation/deserialisation tests for the two D5 schema fields on
 * {@link SourceRequirements}: {@code questMilestones} and
 * {@code skillCapePerks}.
 *
 * <p>Both fields are purely additive: existing drop_rates.json sources that
 * omit them must continue to deserialise with the getters returning
 * {@code null}. Covers absent / null / empty / single / multi shapes plus a
 * full nine-field round-trip.
 */
public class SourceRequirementsD5GsonTest
{
	private static final Gson GSON = new GsonBuilder().create();

	// -- Backwards compatibility -------------------------------------------------

	@Test
	public void deserialise_legacyJsonWithoutFields_bothNull()
	{
		String json = "{\"quests\":[\"DESERT_TREASURE_II\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getQuestMilestones());
		assertNull(req.getSkillCapePerks());
	}

	@Test
	public void deserialise_legacyJsonWithAllSevenPriorFields_bothNull()
	{
		String json = "{"
			+ "\"quests\":[\"DESERT_TREASURE_II\"],"
			+ "\"skills\":[{\"skill\":\"SLAYER\",\"level\":95}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557],"
			+ "\"inventoryItemIds\":[12938],"
			+ "\"inventoryItemIdsAny\":[26945,26946]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getQuestMilestones());
		assertNull(req.getSkillCapePerks());
	}

	// -- questMilestones field shapes -------------------------------------------

	@Test
	public void deserialise_questMilestonesNull_isNull()
	{
		String json = "{\"questMilestones\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getQuestMilestones());
	}

	@Test
	public void deserialise_questMilestonesEmpty_isEmptyNotNull()
	{
		String json = "{\"questMilestones\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getQuestMilestones());
		assertTrue(req.getQuestMilestones().isEmpty());
	}

	@Test
	public void deserialise_questMilestonesSingle_oneEntry()
	{
		String json = "{\"questMilestones\":[\"FAIRYTALE_II__CURE_A_QUEEN\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("FAIRYTALE_II__CURE_A_QUEEN"),
			req.getQuestMilestones());
	}

	@Test
	public void deserialise_questMilestonesMultiple_preservesOrder()
	{
		String json = "{\"questMilestones\":[\"FAIRYTALE_II__CURE_A_QUEEN\",\"DESERT_TREASURE_II\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList("FAIRYTALE_II__CURE_A_QUEEN", "DESERT_TREASURE_II"),
			req.getQuestMilestones());
	}

	// -- skillCapePerks field shapes --------------------------------------------

	@Test
	public void deserialise_skillCapePerksNull_isNull()
	{
		String json = "{\"skillCapePerks\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getSkillCapePerks());
	}

	@Test
	public void deserialise_skillCapePerksEmpty_isEmptyNotNull()
	{
		String json = "{\"skillCapePerks\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getSkillCapePerks());
		assertTrue(req.getSkillCapePerks().isEmpty());
	}

	@Test
	public void deserialise_skillCapePerksSingle_oneEntry()
	{
		String json = "{\"skillCapePerks\":[\"CRAFTING\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("CRAFTING"), req.getSkillCapePerks());
	}

	@Test
	public void deserialise_skillCapePerksMultiple_preservesOrder()
	{
		String json = "{\"skillCapePerks\":[\"FARMING\",\"HUNTER\",\"CONSTRUCTION\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList("FARMING", "HUNTER", "CONSTRUCTION"),
			req.getSkillCapePerks());
	}

	// -- All nine fields populated ----------------------------------------------

	@Test
	public void deserialise_allNineFieldsPopulated_parsesAll()
	{
		String json = "{"
			+ "\"quests\":[\"TROLL_STRONGHOLD\"],"
			+ "\"skills\":[{\"skill\":\"STRENGTH\",\"level\":70}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557],"
			+ "\"inventoryItemIds\":[12938],"
			+ "\"inventoryItemIdsAny\":[26945,26946],"
			+ "\"questMilestones\":[\"FAIRYTALE_II__CURE_A_QUEEN\"],"
			+ "\"skillCapePerks\":[\"FARMING\"]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("FAIRYTALE_II__CURE_A_QUEEN"), req.getQuestMilestones());
		assertEquals(Collections.singletonList("FARMING"), req.getSkillCapePerks());
	}

	// -- Round-trip --------------------------------------------------------------

	@Test
	public void gsonRoundTrip_withD5Fields_preservesStructure()
	{
		SourceRequirements original = new SourceRequirements(
			Collections.singletonList("DESERT_TREASURE_II"),
			null,
			Collections.singletonList("FREMENNIK_HARD"),
			Arrays.asList("JEWELLERY_BOX_FANCY"),
			Arrays.asList(22557),
			Arrays.asList(12938),
			Arrays.asList(26945, 26946),
			Arrays.asList("FAIRYTALE_II__CURE_A_QUEEN", "DESERT_TREASURE_II"),
			Arrays.asList("FARMING", "HUNTER"));
		String json = GSON.toJson(original);
		SourceRequirements restored = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(original, restored);
	}

	@Test
	public void equals_differingQuestMilestones_notEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null, null, null,
			Collections.singletonList("DESERT_TREASURE_II"), null);
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null, null, null,
			Collections.singletonList("TROLL_STRONGHOLD"), null);
		assertTrue(!a.equals(b));
	}

	@Test
	public void equals_differingSkillCapePerks_notEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null, null, null, null, null, null,
			Collections.singletonList("FARMING"));
		SourceRequirements b = new SourceRequirements(
			null, null, null, null, null, null, null, null,
			Collections.singletonList("HUNTER"));
		assertTrue(!a.equals(b));
	}
}
