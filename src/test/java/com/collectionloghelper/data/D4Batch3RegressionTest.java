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
 * D4 batch 3 audit guard - asserts the eight gathering-skill sources scoped in
 * docs/contributor-guide/d4-long-tail-scoping.md section 4 carry the full
 * deep-guidance bar after the batch 3 authoring pass:
 * travel tip, requirements or metaAuthoredDate, at least four steps, an
 * ARRIVE_AT_TILE step with world coordinates, a recommendedItemIds list on at
 * least one step, a section label on steps, and an activity-loop step
 * (loopBackToStep greater than zero).
 */
public class D4Batch3RegressionTest
{
	private static final List<String> BATCH3_SOURCES = List.of(
		"Motherlode Mine",
		"Mining (Gemstone Rocks)",
		"Woodcutting (Teak Trees)",
		"Fishing (Swordfish)",
		"Deep Sea Fishing",
		"Aerial Fishing",
		"Cutting Squid",
		"Underwater Crabs");

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
	public void allBatch3SourcesExist()
	{
		for (String name : BATCH3_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 3 source: " + name);
		}
	}

	@Test
	public void allBatch3SourcesHaveDeepGuidanceShape()
	{
		for (String name : BATCH3_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 3 source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape - at least four steps after the deep pass
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 4,
				name + " has fewer than 4 guidance steps after the deep-guidance pass (got "
					+ source.getGuidanceSteps().size() + ")");

			// Element 2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 3 (skilling variant) - at least one step with recommendedItemIds
			// (E3 = tool tier for skilling sources; Deep Sea Fishing is exempt since
			// the net is carried on the boat, not in the player inventory)
			if (!"Deep Sea Fishing".equals(name))
			{
				boolean hasRecommendedGear = source.getGuidanceSteps().stream()
					.anyMatch(s -> s.getRecommendedItemIds() != null
						&& !s.getRecommendedItemIds().isEmpty());
				assertTrue(hasRecommendedGear,
					name + " has no step with a populated recommendedItemIds list");
			}

			// Element 4 - activity loop mandatory for skilling sources
			boolean hasLoop = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getLoopBackToStep() > 0);
			assertTrue(hasLoop,
				name + " has no loop step (loopBackToStep > 0) - E4 is mandatory for skilling sources");

			// Element 10 - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	@Test
	public void allBatch3SourcesHaveRequirementsBlock()
	{
		for (String name : BATCH3_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 3 source: " + name);

			boolean hasRequirements = source.getRequirements() != null
				&& (source.getRequirements().getQuests() != null
					|| source.getRequirements().getSkills() != null);

			assertTrue(hasRequirements,
				name + " has no requirements block - skilling sources should declare their minimum skill or quest prerequisites (E8)");
		}
	}
}
