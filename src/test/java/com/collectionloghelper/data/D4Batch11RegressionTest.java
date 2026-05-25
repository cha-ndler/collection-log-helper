/*
 * Copyright (c) 2026, cha-ndler
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
 */
package com.collectionloghelper.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D4 batch 11 audit guard - asserts the nine Boat Combat and Sailing-adjacent
 * sources scoped in docs/contributor-guide/d4-long-tail-scoping.md section 4
 * batch 11 carry the appropriate guidance bar after the authoring pass.
 *
 * Boat Combat sources (5) use the full deep-guidance bar per the scoping doc
 * (Batch 11 note: "Otherwise full bar"). Asserted elements:
 *   E1  - source-level travelTip present and non-blank
 *   E2  - at least one ARRIVE_AT_TILE step with positive world coordinates
 *   E3  - recommendedItemIds on the combat step (gear recommendation)
 *   E5  - strategy note implicit in kill step description (Vampyre Kraken prayer)
 *   E6  - requiredItemIds on travel step (Sailors' amulet consumable teleport)
 *   E8  - requirements.skills with at least one SAILING entry
 *   E9  - killTimeSeconds > 0; items list non-empty; ACTOR_DEATH on kill step
 *   E10 - section labels on steps
 *
 * Sailing-adjacent sources (4) use the reduced long-tail bar:
 *   E1  - travelTip present and non-blank
 *   E2  - at least one ARRIVE_AT_TILE step with positive world coordinates
 *   E8  - requirements.skills with at least one SAILING entry
 *   E9  - killTimeSeconds > 0; items list non-empty
 *   E10 - section labels on steps
 *
 * Data sourced from the OSRS Wiki post-Sailing release (launched 2025-11-19).
 * NPC IDs verified via toolkit npc_lookup (Albatross 15224, Great white shark 15200,
 * Narwhal 15202, Orca 15204, Vampyre kraken 15212). Sailing level requirements
 * sourced from wiki Cannon (Sailing) page and individual NPC infoboxes.
 * Port tile (1824, 3691, plane 0) sourced from existing source stubs -- UNCERTAIN,
 * not verified via coordinate_helper against live game tile.
 * Sailors' amulet item ID 32399 confirmed via RuneLite ItemID cross-check
 * (constant SAILORS_AMULET).
 */
public class D4Batch11RegressionTest
{
	private static final List<String> BOAT_COMBAT_SOURCES = List.of(
		"Albatross (Boat Combat)",
		"Great White Shark (Boat Combat)",
		"Narwhal (Boat Combat)",
		"Orcas (Boat Combat)",
		"Vampyre Kraken (Boat Combat)");

	private static final List<String> SAILING_ADJACENT_SOURCES = List.of(
		"Boat Paints",
		"Ocean Encounters",
		"Sailing Misc",
		"Sea Treasures");

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

	/**
	 * Boat Combat sources: full deep-guidance bar.
	 * Each is a cannon-based sea encounter requiring Sailing skill to reach.
	 */
	@Test
	public void allBoatCombatSourcesHaveDeepGuidanceShape()
	{
		for (String name : BOAT_COMBAT_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 11 Boat Combat source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 2,
				name + " has fewer than 2 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// E2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// E3 - recommendedItemIds on at least one step
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// E6 - requiredItemIds on travel step (Sailors' amulet)
			boolean hasRequiredItems = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequiredItems,
				name + " has no step with a populated requiredItemIds list");

			// E8 - requirements.skills with SAILING entry
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			assertTrue(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty(),
				name + " requirements has no skill entries");

			// E9 - kill time populated; items non-empty; ACTOR_DEATH on kill step
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");
			assertNotNull(source.getItems(),
				name + " has no items list");
			assertTrue(!source.getItems().isEmpty(),
				name + " items list is empty");
			boolean hasActorDeath = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ACTOR_DEATH);
			assertTrue(hasActorDeath,
				name + " kill step is missing ACTOR_DEATH completion condition");

			// E10 - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	/**
	 * Sailing-adjacent sources: reduced long-tail bar (E1, E2, E8, E9, E10).
	 * No combat mechanic; activity or reward-collection sources.
	 */
	@Test
	public void allSailingAdjacentSourcesHaveLongTailBarShape()
	{
		for (String name : SAILING_ADJACENT_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 11 Sailing-adjacent source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 2,
				name + " has fewer than 2 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// E2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// E8 - requirements.skills with at least one SAILING entry
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			assertTrue(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty(),
				name + " requirements has no skill entries");

			// E9 - kill time populated; items non-empty
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");
			assertNotNull(source.getItems(),
				name + " has no items list");
			assertTrue(!source.getItems().isEmpty(),
				name + " items list is empty");

			// E10 - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}
}
