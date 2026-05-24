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
 * D3 batch 4 audit guard - asserts the seven mid-tier boss sources
 * (Skotizo, Sarachnis, Scurrius, Thermonuclear smoke devil, Perilous Moons,
 * Hespori, Zalcano) all reach the 10-element deep-guidance bar. Mirrors
 * D3Batch1RegressionTest in shape and intent.
 */
public class D3Batch4RegressionTest
{
	private static final List<String> BATCH4_SOURCES = List.of(
		"Skotizo",
		"Sarachnis",
		"Scurrius",
		"Thermonuclear smoke devil",
		"Perilous Moons",
		"Hespori",
		"Zalcano");

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
	public void allBatch4SourcesHaveDeepGuidanceShape()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D3 batch 4 source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Element 8 - prerequisites object present (Skotizo/Sarachnis/Scurrius use {})
			assertNotNull(source.getRequirements(),
				name + " has no source-level requirements object");

			// Step shape - at least five steps after the deep pass
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 5,
				name + " has fewer than 5 guidance steps after the deep-guidance pass (got "
					+ source.getGuidanceSteps().size() + ")");

			// Element 9 - kill time populated
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// Element 2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 3 - kill step exposes recommendedItemIds
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Element 6/7 - inventory loadout / bank-and-return wiring
			boolean hasRequiredItems = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequiredItems,
				name + " has no step with a populated requiredItemIds list");

			// Element 10 - section labels on steps for sources with >5 steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}
}
