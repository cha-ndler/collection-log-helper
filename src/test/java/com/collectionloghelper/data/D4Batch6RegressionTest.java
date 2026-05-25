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
 * D4 batch 6 audit guard - asserts the 14 mid-high slayer sources scoped in
 * docs/contributor-guide/d4-long-tail-scoping.md section 4 (batch 6) carry
 * the relaxed long-tail bar after the batch 6 authoring pass:
 * travel tip (E1), at least one ARRIVE_AT_TILE or ARRIVE_AT_ZONE step with
 * world coordinates (E2), requirements block reflecting the slayer-level or
 * quest gate (E8), at least one item with a positive drop rate (E9),
 * section labels on steps, and a recommendedItemIds list on the kill step.
 * Sources with a consumable access item (mirror shield, witchwood icon) also
 * declare requiredItemIds on the earliest step that needs them (E6).
 * Skotizo is excluded -- it is covered by D2/D3 as a BOSSES source.
 */
public class D4Batch6RegressionTest
{
	private static final List<String> BATCH6_SOURCES = List.of(
		"Abyssal Demon",
		"Dark Beast",
		"Nechryael",
		"Smoke Devil",
		"Cave Kraken",
		"Cave Horror",
		"Spiritual Mage",
		"Spiritual Mage (Zarosian)",
		"Basilisk",
		"Basilisk Knight",
		"Fossil Island Wyvern",
		"Warped Creature",
		"Lava Strykewyrm",
		"Araxyte");

	/**
	 * Sources that require a consumable access item (mirror shield or witchwood
	 * icon) and must therefore declare requiredItemIds on at least one step (E6).
	 */
	private static final List<String> E6_SOURCES = List.of(
		"Basilisk",
		"Basilisk Knight",
		"Cave Horror");

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
	public void allBatch6SourcesExistInDatabase()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);
		}
	}

	@Test
	public void allBatch6SourcesHaveTravelTip()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			// E1 - travel tip set and non-blank
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");
		}
	}

	@Test
	public void allBatch6SourcesHaveAtLeastTwoGuidanceSteps()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 2,
				name + " has fewer than 2 guidance steps after the long-tail bar pass (got "
					+ source.getGuidanceSteps().size() + ")");
		}
	}

	@Test
	public void allBatch6SourcesHaveArriveStep()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			// E2 - at least one ARRIVE_AT_TILE or ARRIVE_AT_ZONE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s ->
					(s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
						|| s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_ZONE)
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE/ARRIVE_AT_ZONE step with positive world coordinates");
		}
	}

	@Test
	public void allBatch6SourcesHaveRequirements()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			// E8 - requirements block present (slayer-level or quest gate)
			assertNotNull(source.getRequirements(),
				name + " has no requirements block; a slayer level or quest gate is expected");
		}
	}

	@Test
	public void allBatch6SourcesHaveDropRates()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			// E9 - at least one item with a positive drop rate
			assertNotNull(source.getItems(),
				name + " has no items array");
			assertTrue(!source.getItems().isEmpty(),
				name + " has an empty items array");
			boolean hasRate = source.getItems().stream()
				.anyMatch(i -> i.getDropRate() > 0);
			assertTrue(hasRate,
				name + " has no item with a positive dropRate");
		}
	}

	@Test
	public void allBatch6SourcesHaveSectionLabels()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	@Test
	public void allBatch6SourcesHaveRecommendedItemsOnKillStep()
	{
		for (String name : BATCH6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 source: " + name);

			boolean hasRecommended = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommended,
				name + " has no step with a populated recommendedItemIds list");
		}
	}

	@Test
	public void e6SourcesHaveRequiredItemsOnAccessStep()
	{
		for (String name : E6_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 6 E6 source: " + name);

			// E6 - consumable access item declared on at least one step
			boolean hasRequired = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequired,
				name + " has no step with requiredItemIds; expected an access-item gate (E6)");
		}
	}

	// -- helpers --

	private CollectionLogSource findSource(String name)
	{
		return database.getAllSources().stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElse(null);
	}
}
