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
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link GuidanceStep#resolveDescription(Integer)},
 * {@link GuidanceStep#resolveNpcId(Integer)}, and the
 * {@code perItemStepDescription} / {@code perItemNpcId} schema fields.
 *
 * <p>Acceptance criteria verified:
 * <ol>
 *   <li>resolveDescription(null) returns the static description.</li>
 *   <li>resolveDescription(itemId) returns the override when the map has a matching entry.</li>
 *   <li>resolveDescription(itemId) falls back to static when the map exists but has no entry for the target.</li>
 *   <li>resolveDescription(itemId) falls back to static when the map is null.</li>
 *   <li>resolveNpcId(null) returns the static npcId.</li>
 *   <li>resolveNpcId(itemId) returns the override when the map has a matching entry.</li>
 *   <li>resolveNpcId(itemId) falls back to static when the map exists but has no entry for the target.</li>
 *   <li>resolveNpcId(itemId) falls back to static when the map is null.</li>
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

	// ── Cyclopes defender tier chain (B4.3.3) ────────────────────────────────

	/** Item IDs for the Warriors' Guild defender tier chain. */
	private static final int BRONZE_DEFENDER = 8844;
	private static final int IRON_DEFENDER   = 8845;
	private static final int RUNE_DEFENDER   = 8850;
	private static final int DRAGON_DEFENDER = 12954;

	private static final String CYCLOPES_STATIC_DESC =
		"Kill Cyclopes for defender drops. Each kill costs 10 warrior guild tokens";

	/**
	 * Verifies that resolveDescription returns the Iron defender override text
	 * (containing both "Iron" and "Bronze") when targeted at the Iron defender item.
	 * This exercises the mid-chain scenario where the player holds the previous tier.
	 */
	@Test
	public void cyclopes_resolveDescription_ironDefender_mentionsBronzeHolding()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(BRONZE_DEFENDER,
			"Kill Cyclopes (no prerequisite) to receive a Bronze defender and start the chain.");
		overrides.put(IRON_DEFENDER,
			"Kill Cyclopes while holding a Bronze defender in inventory to receive an Iron defender.");
		overrides.put(RUNE_DEFENDER,
			"Kill Cyclopes while holding an Adamant defender in inventory to receive a Rune defender.");
		overrides.put(DRAGON_DEFENDER,
			"Kill Cyclopes while holding a Rune defender in inventory to receive the Dragon defender.");

		GuidanceStep step = stepWithOverrides(CYCLOPES_STATIC_DESC, overrides);

		String resolved = step.resolveDescription(IRON_DEFENDER);
		assertTrue("Override for Iron defender must mention 'Iron'",
			resolved.contains("Iron"));
		assertTrue("Override for Iron defender must mention 'Bronze' (what to hold)",
			resolved.contains("Bronze"));
	}

	/**
	 * Verifies that the Dragon defender entry (the terminal tier) resolves
	 * correctly and references the Rune defender as the required held item.
	 */
	@Test
	public void cyclopes_resolveDescription_dragonDefender_mentionsRuneHolding()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(DRAGON_DEFENDER,
			"Kill Cyclopes while holding a Rune defender in inventory to receive the Dragon defender.");

		GuidanceStep step = stepWithOverrides(CYCLOPES_STATIC_DESC, overrides);

		String resolved = step.resolveDescription(DRAGON_DEFENDER);
		assertTrue("Dragon defender override must mention 'Dragon'",
			resolved.contains("Dragon"));
		assertTrue("Dragon defender override must mention 'Rune' (what to hold)",
			resolved.contains("Rune"));
	}

	/**
	 * When no targetItemId is supplied the coordinator has no active item in context
	 * (e.g. the source has multiple uncollected tiers). The static generic description
	 * should be returned unchanged.
	 */
	@Test
	public void cyclopes_resolveDescription_noTarget_returnsStaticDescription()
	{
		Map<Integer, String> overrides = new HashMap<>();
		overrides.put(BRONZE_DEFENDER,
			"Kill Cyclopes (no prerequisite) to receive a Bronze defender and start the chain.");
		overrides.put(IRON_DEFENDER,
			"Kill Cyclopes while holding a Bronze defender in inventory to receive an Iron defender.");

		GuidanceStep step = stepWithOverrides(CYCLOPES_STATIC_DESC, overrides);

		assertEquals(CYCLOPES_STATIC_DESC, step.resolveDescription(null));
	}

	// ── resolveNpcId constants ─────────────────────────────────────────────────

	private static final int STATIC_NPC_ID = 5000;
	private static final int OVERRIDE_NPC_ID = 6001;
	private static final int OTHER_NPC_ID = 7777;

	// ── resolveNpcId: null targetItemId ───────────────────────────────────────

	@Test
	public void resolveNpcId_nullTarget_returnsStaticNpcId()
	{
		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(ITEM_TIER1, OVERRIDE_NPC_ID);

		GuidanceStep step = stepWithNpcOverrides(STATIC_NPC_ID, npcOverrides);

		assertEquals(STATIC_NPC_ID, step.resolveNpcId(null));
	}

	// ── resolveNpcId: map has matching entry ──────────────────────────────────

	@Test
	public void resolveNpcId_targetInMap_returnsOverrideNpcId()
	{
		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(ITEM_TIER1, OVERRIDE_NPC_ID);
		npcOverrides.put(ITEM_TIER5, OTHER_NPC_ID);

		GuidanceStep step = stepWithNpcOverrides(STATIC_NPC_ID, npcOverrides);

		assertEquals(OVERRIDE_NPC_ID, step.resolveNpcId(ITEM_TIER1));
		assertEquals(OTHER_NPC_ID, step.resolveNpcId(ITEM_TIER5));
	}

	// ── resolveNpcId: map exists but target not in it ─────────────────────────

	@Test
	public void resolveNpcId_targetNotInMap_returnsStaticNpcId()
	{
		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(ITEM_TIER1, OVERRIDE_NPC_ID);

		GuidanceStep step = stepWithNpcOverrides(STATIC_NPC_ID, npcOverrides);

		assertEquals(STATIC_NPC_ID, step.resolveNpcId(ITEM_UNKNOWN));
	}

	// ── resolveNpcId: null map ────────────────────────────────────────────────

	@Test
	public void resolveNpcId_nullMap_returnsStaticNpcId()
	{
		GuidanceStep step = stepWithNpcOverrides(STATIC_NPC_ID, null);

		assertEquals(STATIC_NPC_ID, step.resolveNpcId(ITEM_TIER1));
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private static GuidanceStep stepWithOverrides(String description,
		Map<Integer, String> perItemStepDescription)
	{
		return new GuidanceStep(
			description,
			perItemStepDescription,
			0, 0, 0,       // worldX, worldY, worldPlane
			0, null, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
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

	private static GuidanceStep stepWithNpcOverrides(int npcId,
		Map<Integer, Integer> perItemNpcId)
	{
		return new GuidanceStep(
			STATIC_DESC,
			null,           // perItemStepDescription
			0, 0, 0,       // worldX, worldY, worldPlane
			npcId, perItemNpcId, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
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
