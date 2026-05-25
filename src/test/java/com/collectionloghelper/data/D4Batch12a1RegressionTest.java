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
 * D4 batch 12a-1 audit guard - asserts the seven long-tail minigame sources
 * (Pest Control, Soul Wars, Last Man Standing, Castle Wars, Tithe Farm,
 * Trouble Brewing, Barbarian Assault) carry the full deep-guidance bar after
 * the batch 12a-1 authoring pass: travel tip, at least four guidance steps,
 * section labels on steps, an ARRIVE_AT_TILE step with world coordinates,
 * an activity-loop declaration (loopBackToStep), and a recommendedItemIds
 * list on the activity step.
 *
 * Trouble Brewing is asserted separately - it has 8 steps including a
 * multi-sub-loop brewing cycle, so its section and loop assertions use a
 * broader shape check rather than the 4-step minimum.
 */
public class D4Batch12a1RegressionTest
{
	private static final List<String> STANDARD_MINIGAME_SOURCES = List.of(
		"Pest Control",
		"Soul Wars",
		"Last Man Standing",
		"Castle Wars",
		"Tithe Farm",
		"Barbarian Assault");

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
	 * Standard minigame sources: full deep-guidance bar with travel, queue,
	 * activity-loop, and reward sections.
	 */
	@Test
	public void allStandardMinigameSourcesHaveDeepGuidanceShape()
	{
		for (String name : STANDARD_MINIGAME_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 12a-1 source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step count - at least four steps (Travel, Queue, Round, Reward)
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 4,
				name + " has fewer than 4 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// E2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// E4 - activity-loop declared (minigame round loops back to queue step >= 1)
			boolean hasLoop = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getLoopBackToStep() > 0);
			assertTrue(hasLoop,
				name + " has no step with loopBackToStep > 0");

			// E3 - recommendedItemIds on at least one step
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// E10 proxy - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	/**
	 * Trouble Brewing: 8-step brewing loop. Asserts section labels, loop
	 * presence, ARRIVE_AT_TILE, and minimum step count separately because its
	 * structure differs from the 4-section standard minigame shape.
	 */
	@Test
	public void troubleBrewingHasDeepGuidanceShape()
	{
		CollectionLogSource source = database.getAllSources().stream()
			.filter(s -> "Trouble Brewing".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(source, "Trouble Brewing source missing from drop_rates.json");

		// E1 - travel tip
		assertNotNull(source.getTravelTip(), "Trouble Brewing has no travelTip");
		assertTrue(!source.getTravelTip().isBlank(), "Trouble Brewing travelTip is blank");

		// Step count - multi-step brewing loop retains at least 6 steps
		assertNotNull(source.getGuidanceSteps(), "Trouble Brewing has no guidanceSteps");
		assertTrue(source.getGuidanceSteps().size() >= 6,
			"Trouble Brewing has fewer than 6 guidance steps (got "
				+ source.getGuidanceSteps().size() + ")");

		// E2 - at least one ARRIVE_AT_TILE step with world coords
		boolean hasArriveStep = source.getGuidanceSteps().stream()
			.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
				&& s.getWorldX() > 0
				&& s.getWorldY() > 0);
		assertTrue(hasArriveStep,
			"Trouble Brewing has no ARRIVE_AT_TILE step with positive world coordinates");

		// E4 - brewing sub-loop present (loops to step >= 1)
		boolean hasLoop = source.getGuidanceSteps().stream()
			.anyMatch(s -> s.getLoopBackToStep() > 0);
		assertTrue(hasLoop, "Trouble Brewing has no loopBackToStep > 0 declaration");

		// E10 proxy - section labels on steps
		boolean hasSectionLabels = source.getGuidanceSteps().stream()
			.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
		assertTrue(hasSectionLabels, "Trouble Brewing has no step with a section label");

		// E8 - requirements present (Cabin Fever quest + 40 Cooking)
		assertNotNull(source.getRequirements(), "Trouble Brewing has no requirements object");
	}
}
