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
 * D4 batch 4 audit guard - asserts the nine non-gathering skilling and timed-event
 * sources scoped in docs/contributor-guide/d4-long-tail-scoping.md section 4 carry
 * the full deep-guidance bar after the batch 4 authoring pass:
 * travel tip, at least one ARRIVE_AT_TILE step with world coordinates, section
 * labels on steps, and (for repeatable sources) a loop step.  Prifddinas Rabbit
 * is a one-kill source and is exempt from the loop assertion.
 * Requirements (E8) are asserted for sources that have a meaningful skill or quest
 * gate per the OSRS Wiki.
 */
public class D4Batch4RegressionTest
{
	/** All nine batch-4 sources. */
	private static final List<String> BATCH4_SOURCES = List.of(
		"Rooftop Agility",
		"Colossal Wyrm Agility",
		"Black Chinchompas",
		"Prifddinas Rabbit",
		"Pickpocketing Darkmeyer Vyre",
		"Thieving (Seed Stalls)",
		"Farming (Fruit Trees)",
		"Runecrafting (Fire Runes)",
		"Shooting Stars");

	/**
	 * Repeatable sources that must declare a loop step (loopBackToStep set).
	 * Prifddinas Rabbit is a one-kill drop and is intentionally excluded.
	 */
	private static final List<String> LOOP_SOURCES = List.of(
		"Rooftop Agility",
		"Colossal Wyrm Agility",
		"Black Chinchompas",
		"Pickpocketing Darkmeyer Vyre",
		"Thieving (Seed Stalls)",
		"Farming (Fruit Trees)",
		"Runecrafting (Fire Runes)",
		"Shooting Stars");

	/**
	 * Sources that must declare a requirements block (skill or quest gate).
	 */
	private static final List<String> GATED_SOURCES = List.of(
		"Rooftop Agility",
		"Colossal Wyrm Agility",
		"Black Chinchompas",
		"Prifddinas Rabbit",
		"Pickpocketing Darkmeyer Vyre",
		"Thieving (Seed Stalls)",
		"Farming (Fruit Trees)",
		"Runecrafting (Fire Runes)",
		"Shooting Stars");

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
	public void allBatch4SourcesExistInDatabase()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);
		}
	}

	@Test
	public void allBatch4SourcesHaveTravelTip()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");
		}
	}

	@Test
	public void allBatch4SourcesHaveAtLeastTwoGuidanceSteps()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);

			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 2,
				name + " has fewer than 2 guidance steps after the deep-guidance pass (got "
					+ source.getGuidanceSteps().size() + ")");
		}
	}

	@Test
	public void allBatch4SourcesHaveArriveAtTileStep()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);

			// Element 2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");
		}
	}

	@Test
	public void allBatch4SourcesHaveSectionLabels()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);

			// Steps must carry section labels for multi-step sources
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	@Test
	public void repeatableSourcesHaveLoopStep()
	{
		for (String name : LOOP_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 loop source: " + name);

			// Element 4 - at least one step declares a loop (loopCount > 0 is the discriminator;
			// loopBackToStep may be 0 for "return to first step", so only loopCount is checked)
			boolean hasLoop = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getLoopCount() > 0);
			assertTrue(hasLoop,
				name + " has no loop step (loopCount > 0) after the deep-guidance pass");
		}
	}

	@Test
	public void gatedSourcesHaveRequirements()
	{
		for (String name : GATED_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 gated source: " + name);

			// Element 8 - requirements block present
			assertNotNull(source.getRequirements(),
				name + " has no requirements block; a skill or quest gate is expected");
		}
	}

	@Test
	public void allBatch4SourcesHaveDropRates()
	{
		for (String name : BATCH4_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 4 source: " + name);

			// Element 9 - at least one item has a positive drop rate
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

	// -- helpers --

	private CollectionLogSource findSource(String name)
	{
		return database.getAllSources().stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElse(null);
	}
}
