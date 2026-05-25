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
 * D4 batch 5 audit guard - asserts the fourteen low-level slayer sources scoped
 * in docs/contributor-guide/d4-long-tail-scoping.md section 4 carry the
 * long-tail bar after the batch 5 authoring pass.
 *
 * Long-tail bar elements checked here:
 *   E1  - travelTip present and non-blank
 *   E2  - at least one auto-arrival step (ARRIVE_AT_TILE, ARRIVE_AT_ZONE, or
 *          PLAYER_ON_PLANE) with positive world coordinates
 *   E6  - requiredItemIds on the kill step for Mogre (fishing explosive) and
 *          Rockslug (bag of salt); waived with reason for the other twelve
 *          sources which need no consumable access item
 *   E8  - requirements block present with at least one skill or quest entry
 *   E9  - at least one item with a positive dropRate
 *
 * Section labels (Travel / Combat) are also asserted on every source since they
 * drive the panel UI grouping for all authored sources regardless of bar level.
 *
 * Elements E3, E4, E5, E7, E10 are waived for this batch per the long-tail bar
 * definition: these low-level slayer monsters have no phase mechanics, no
 * multi-cycle loop, and require no hard-to-replace inventory loadout beyond the
 * two cases covered by E6 above.
 */
public class D4Batch5RegressionTest
{
	/** All fourteen batch-5 sources in the order they appear in the scoping doc. */
	private static final List<String> BATCH5_SOURCES = List.of(
		"Crawling Hand",
		"Cave Crawler",
		"Cockatrice",
		"Pyrefiend",
		"Mogre",
		"Rockslug",
		"Jelly",
		"Brine Rat",
		"Turoth",
		"Bloodveld",
		"Infernal Mage",
		"Dust Devil",
		"Aberrant Spectre",
		"Kurask");

	/**
	 * Sources with a quest prerequisite for access.
	 * Brine Rat requires Olaf's Quest to enter Brine Rat Cavern.
	 */
	private static final List<String> QUEST_GATED_SOURCES = List.of(
		"Brine Rat");

	/**
	 * Sources where E6 (bank-and-return) is wired because the kill step
	 * requires a consumable item that must be in inventory:
	 *   Mogre    - fishing explosive (item 6664) to spawn the NPC
	 *   Rockslug - bag of salt (item 4161) to finish the kill
	 */
	private static final List<String> E6_WIRED_SOURCES = List.of(
		"Mogre",
		"Rockslug");

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

	/** All fourteen sources must exist in the database. */
	@Test
	public void allBatch5SourcesExistInDatabase()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
		}
	}

	/** E1 - travelTip must be set and non-blank on every source. */
	@Test
	public void allBatch5SourcesHaveTravelTip()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip (E1)");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank (E1)");
		}
	}

	/** E2 - at least one auto-arrival step with positive world coordinates. */
	@Test
	public void allBatch5SourcesHaveAutoArrivalStep()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps (E2)");

			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s ->
					(s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
						|| s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_ZONE
						|| s.getCompletionCondition() == CompletionCondition.PLAYER_ON_PLANE)
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no auto-arrival step with positive world coordinates (E2)");
		}
	}

	/**
	 * E6 - Mogre and Rockslug must declare requiredItemIds on their kill step.
	 * The remaining twelve sources are waived: they need no consumable access
	 * item to enter or complete the kill.
	 */
	@Test
	public void e6WiredSourcesHaveRequiredItemIds()
	{
		for (String name : E6_WIRED_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 E6 source: " + name);
			boolean hasRequiredItems = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequiredItems,
				name + " has no step with requiredItemIds - E6 wiring is mandatory for this source");
		}
	}

	/** E8 - every source must have a requirements block with at least one skill or quest. */
	@Test
	public void allBatch5SourcesHaveRequirements()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
			assertNotNull(source.getRequirements(),
				name + " has no requirements block (E8)");

			boolean hasSkillOrQuest =
				(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty())
				|| (source.getRequirements().getQuests() != null
					&& !source.getRequirements().getQuests().isEmpty());
			assertTrue(hasSkillOrQuest,
				name + " requirements block has neither skills nor quests (E8)");
		}
	}

	/** E8 (quest variant) - Brine Rat must declare a quest prerequisite. */
	@Test
	public void questGatedSourcesHaveQuestRequirement()
	{
		for (String name : QUEST_GATED_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 quest-gated source: " + name);
			assertNotNull(source.getRequirements(),
				name + " has no requirements block");
			assertNotNull(source.getRequirements().getQuests(),
				name + " has no quests list in requirements - Olaf's Quest is required for Brine Rat Cavern access");
			assertTrue(!source.getRequirements().getQuests().isEmpty(),
				name + " quests list is empty - Olaf's Quest must be declared");
		}
	}

	/** E9 - at least one item must have a positive dropRate. */
	@Test
	public void allBatch5SourcesHaveDropRates()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
			assertNotNull(source.getItems(),
				name + " has no items array (E9)");
			assertTrue(!source.getItems().isEmpty(),
				name + " has an empty items array (E9)");
			boolean hasPositiveRate = source.getItems().stream()
				.anyMatch(i -> i.getDropRate() > 0);
			assertTrue(hasPositiveRate,
				name + " has no item with a positive dropRate (E9)");
		}
	}

	/** Section labels (Travel / Combat) must be present on at least one step per source. */
	@Test
	public void allBatch5SourcesHaveSectionLabels()
	{
		for (String name : BATCH5_SOURCES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "missing D4 batch 5 source: " + name);
			boolean hasSectionLabel = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabel,
				name + " has no step with a section label");
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
