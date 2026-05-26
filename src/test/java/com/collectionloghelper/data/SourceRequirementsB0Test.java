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
 * Tier B0 — value-type tests for {@link SourceRequirements#getPohTeleports()} and
 * {@link SourceRequirements#getEquippedItemIds()}.
 *
 * <p>Covers Gson deserialisation with the new fields absent / null / empty /
 * populated, plus equals/hashCode for the extended value type. Backwards
 * compatibility check: existing JSON without the new fields must deserialise
 * unchanged with the new fields {@code null}.
 */
public class SourceRequirementsB0Test
{
	private static final Gson GSON = new GsonBuilder().create();

	// ── Backwards compatibility ────────────────────────────────────────────────

	@Test
	public void deserialise_legacyJsonWithoutNewFields_pohTeleportsIsNull()
	{
		String json = "{\"quests\":[\"TROLL_STRONGHOLD\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getPohTeleports());
		assertNull(req.getEquippedItemIds());
	}

	@Test
	public void deserialise_legacyJsonWithoutNewFields_existingFieldsParse()
	{
		String json = "{\"quests\":[\"DESERT_TREASURE_II\"],\"skills\":[{\"skill\":\"SLAYER\",\"level\":95}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("DESERT_TREASURE_II"), req.getQuests());
		assertNotNull(req.getSkills());
		assertEquals(1, req.getSkills().size());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertNull(req.getPohTeleports());
		assertNull(req.getEquippedItemIds());
	}

	// ── pohTeleports field ─────────────────────────────────────────────────────

	@Test
	public void deserialise_pohTeleportsNull_isNull()
	{
		String json = "{\"pohTeleports\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getPohTeleports());
	}

	@Test
	public void deserialise_pohTeleportsEmpty_isEmptyNotNull()
	{
		String json = "{\"pohTeleports\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getPohTeleports());
		assertTrue(req.getPohTeleports().isEmpty());
	}

	@Test
	public void deserialise_pohTeleportsPopulated_preservesOrder()
	{
		String json = "{\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\",\"MOUNTED_GLORY\"]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList("JEWELLERY_BOX_FANCY", "MOUNTED_GLORY"), req.getPohTeleports());
	}

	// ── equippedItemIds field ──────────────────────────────────────────────────

	@Test
	public void deserialise_equippedItemIdsNull_isNull()
	{
		String json = "{\"equippedItemIds\":null}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNull(req.getEquippedItemIds());
	}

	@Test
	public void deserialise_equippedItemIdsEmpty_isEmptyNotNull()
	{
		String json = "{\"equippedItemIds\":[]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertNotNull(req.getEquippedItemIds());
		assertTrue(req.getEquippedItemIds().isEmpty());
	}

	@Test
	public void deserialise_equippedItemIdsPopulated_preservesOrder()
	{
		String json = "{\"equippedItemIds\":[22557,28266]}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Arrays.asList(22557, 28266), req.getEquippedItemIds());
	}

	// ── All five fields together ──────────────────────────────────────────────

	@Test
	public void deserialise_allFiveFieldsPopulated_parsesAll()
	{
		String json = "{"
			+ "\"quests\":[\"TROLL_STRONGHOLD\"],"
			+ "\"skills\":[{\"skill\":\"STRENGTH\",\"level\":70}],"
			+ "\"diaries\":[\"FREMENNIK_HARD\"],"
			+ "\"pohTeleports\":[\"JEWELLERY_BOX_FANCY\"],"
			+ "\"equippedItemIds\":[22557]"
			+ "}";
		SourceRequirements req = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(Collections.singletonList("TROLL_STRONGHOLD"), req.getQuests());
		assertEquals(1, req.getSkills().size());
		assertEquals(Collections.singletonList("FREMENNIK_HARD"), req.getDiaries());
		assertEquals(Collections.singletonList("JEWELLERY_BOX_FANCY"), req.getPohTeleports());
		assertEquals(Collections.singletonList(22557), req.getEquippedItemIds());
	}

	// ── equals / hashCode ─────────────────────────────────────────────────────

	@Test
	public void equals_sameNewFields_areEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null,
			Collections.singletonList("MOUNTED_GLORY"),
			Collections.singletonList(22557), null, null, null, null);
		SourceRequirements b = new SourceRequirements(
			null, null, null,
			Collections.singletonList("MOUNTED_GLORY"),
			Collections.singletonList(22557), null, null, null, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equals_differingPohTeleports_notEqual()
	{
		SourceRequirements a = new SourceRequirements(
			null, null, null,
			Collections.singletonList("MOUNTED_GLORY"),
			null, null, null, null, null);
		SourceRequirements b = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			null, null, null, null, null);
		assertTrue(!a.equals(b));
	}

	// ── Round-trip ─────────────────────────────────────────────────────────────

	@Test
	public void gsonRoundTrip_withNewFields_preservesStructure()
	{
		SourceRequirements original = new SourceRequirements(
			Collections.singletonList("DESERT_TREASURE_II"),
			null,
			Collections.singletonList("FREMENNIK_HARD"),
			Arrays.asList("JEWELLERY_BOX_FANCY", "MOUNTED_GLORY"),
			Arrays.asList(22557, 28266), null, null, null, null);
		String json = GSON.toJson(original);
		SourceRequirements restored = GSON.fromJson(json, SourceRequirements.class);
		assertEquals(original, restored);
	}
}
