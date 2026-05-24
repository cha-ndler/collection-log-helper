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
 * D3 batch 3 audit guard - asserts the four capstone PvM + nostalgic-instance
 * sources (Sol Heredit, The Inferno, The Fight Caves, Nex) exist by name, each
 * carries at least six guidance steps after the deep-guidance pass (Bank /
 * Travel / Combat-enter / Combat-kill / Loot / Bank-return), each declares a
 * travel tip, a recommendedItemIds list on the kill step (capped at 6 entries),
 * a non-zero kill time, and at least one ARRIVE_AT_TILE step with positive
 * world coordinates and a section label.
 */
public class D3Batch3RegressionTest
{
	private static final List<String> BATCH3_SOURCES = List.of(
		"Sol Heredit",
		"The Inferno",
		"The Fight Caves",
		"Nex");

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
	public void allBatch3SourcesHaveDeepGuidanceShape()
	{
		for (String name : BATCH3_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D3 batch 3 source: " + name);

			// Element 1 - source-level travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape - at least six steps after the deep pass
			// (Bank / Travel / Combat-enter / Combat-kill / Loot / Bank-return)
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 6,
				name + " has fewer than 6 guidance steps after the deep-guidance pass (got "
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

			// Element 3 - at least one step exposes recommendedItemIds, capped at 6
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty()
					&& s.getRecommendedItemIds().size() <= 6);
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated, bounded (<=6) recommendedItemIds list");

			// Element 6/7 - inventory loadout / bank-and-return wiring
			boolean hasRequiredItems = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequiredItems,
				name + " has no step with a populated requiredItemIds list");

			// Element 10 - section labels on steps (Bank / Travel / Combat / Loot)
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}
}
