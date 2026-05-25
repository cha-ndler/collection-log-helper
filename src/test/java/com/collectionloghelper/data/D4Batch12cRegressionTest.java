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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * D4 batch 12c audit guard - asserts the five misc OTHER residue sources
 * scoped in docs/contributor-guide/d4-long-tail-scoping.md batch 12c carry
 * the appropriate guidance bar after the authoring pass.
 *
 * Wilderness God Wars Dungeon and Stingray (Boat Combat) use the full bar.
 * Prifddinas Elf and Fountain of Rune use the reduced long-tail bar.
 * Random Events uses the special passive treatment from scoping Q5.
 */
public class D4Batch12cRegressionTest
{
	// Wilderness GWD: full 4-step bar (travel, obstacle, kill, exit)
	private static final List<String> FULL_BAR_SOURCES = List.of(
		"Wilderness God Wars Dungeon");

	// Boat Combat sources: 2-step bar (travel + kill) matching Albatross/Narwhal precedent
	private static final List<String> BOAT_COMBAT_SOURCES = List.of(
		"Stingray (Boat Combat)");

	private static final List<String> LONG_TAIL_SOURCES = List.of(
		"Prifddinas Elf",
		"Fountain of Rune");

	private static final String RANDOM_EVENTS = "Random Events";

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

	@Test
	public void fullBarSourcesHaveDeepGuidanceShape()
	{
		for (String name : FULL_BAR_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 12c full-bar source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(), name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(), name + " travelTip is blank");

			// Step shape - at least 4 steps for full bar
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 4,
				name + " has fewer than 4 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// Element 2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 3 - at least one step with recommendedItemIds
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Element 8 - requirements populated
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");

			// Section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels, name + " has no step with a section label");
		}
	}

	@Test
	public void boatCombatSourcesHaveFullBarShape()
	{
		for (String name : BOAT_COMBAT_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 12c boat-combat source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(), name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(), name + " travelTip is blank");

			// 2-step minimum matching Albatross/Narwhal precedent (travel + kill)
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 2,
				name + " has fewer than 2 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// Element 2 - at least one ARRIVE_AT_TILE step
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 3 - kill step with recommendedItemIds
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Element 8 - requirements (Sailing level)
			assertNotNull(source.getRequirements(), name + " has no requirements object");

			// Section labels
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels, name + " has no step with a section label");
		}
	}

	@Test
	public void longTailSourcesHaveReducedBar()
	{
		for (String name : LONG_TAIL_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 12c long-tail source: " + name);

			// Element 1 - travel tip present
			assertNotNull(source.getTravelTip(), name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(), name + " travelTip is blank");

			// Element 2 - at least one ARRIVE_AT_TILE step
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 8 - requirements populated
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");

			// Element 9 - at least one item with a positive drop rate
			assertNotNull(source.getItems(), name + " has no items");
			assertTrue(!source.getItems().isEmpty(), name + " items list is empty");
			assertTrue(source.getItems().stream().allMatch(i -> i.getDropRate() > 0),
				name + " has an item with non-positive drop rate");
		}
	}

	@Test
	public void randomEventsHasPassiveShape()
	{
		CollectionLogSource source = database.getAllSources().stream()
			.filter(s -> RANDOM_EVENTS.equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(source, "missing D4 batch 12c source: " + RANDOM_EVENTS);

		// Q5 special: travelTip must be empty string (not null, not a route)
		assertNotNull(source.getTravelTip(), RANDOM_EVENTS + " travelTip must be non-null (set to empty string)");
		assertTrue(source.getTravelTip().isEmpty(), RANDOM_EVENTS + " travelTip must be empty string for passive source");

		// Single MANUAL step only - no ARRIVE_AT_TILE travel flow
		assertNotNull(source.getGuidanceSteps(), RANDOM_EVENTS + " has no guidanceSteps");
		assertEquals(1, source.getGuidanceSteps().size(),
			RANDOM_EVENTS + " must have exactly 1 guidance step (passive passive source)");
		assertEquals(CompletionCondition.MANUAL,
			source.getGuidanceSteps().get(0).getCompletionCondition(),
			RANDOM_EVENTS + " single step must be MANUAL");

		// No ARRIVE_AT_TILE step (passive - no travel flow)
		boolean hasArriveStep = source.getGuidanceSteps().stream()
			.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE);
		assertTrue(!hasArriveStep, RANDOM_EVENTS + " must not have an ARRIVE_AT_TILE step");

		// Element 9 - items populated
		assertNotNull(source.getItems(), RANDOM_EVENTS + " has no items");
		assertTrue(!source.getItems().isEmpty(), RANDOM_EVENTS + " items list is empty");
	}
}
