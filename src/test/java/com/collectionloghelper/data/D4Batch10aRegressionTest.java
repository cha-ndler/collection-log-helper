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
 * D4 batch 10a audit guard - asserts the seven rare-table chest sources scoped
 * in docs/contributor-guide/d4-long-tail-scoping.md section 4 (Batch 10, first
 * split) carry the reduced open-chest bar after the authoring pass.
 *
 * The open-chest bar applied here is the long-tail bar (5 elements):
 *   E1  - source-level travelTip present and non-blank
 *   E2  - at least one ARRIVE_AT_TILE step with positive world coordinates
 *   E6  - requiredItemIds present on the open step for key-gated sources
 *         (Brimstone Chest, Larran's Big Chest, Elven Crystal Chest,
 *          Zombie Pirate Locker); waived for Lost Schematics, Fossil Island
 *          Notes, and Monkey Backpacks which have no consumable access key
 *   E8  - requirements object where a quest or skill gate exists (Elven
 *         Crystal Chest: Song of the Elves; Lost Schematics: Sailing 1;
 *         Fossil Island Notes: Bone Voyage; Monkey Backpacks: already present)
 *   E9  - items list non-empty; killTimeSeconds > 0
 *   E10 - section labels on steps (structural signal; one batch-level citation
 *         in the PR description covers all seven sources)
 *
 * Elements intentionally NOT asserted (dropped per long-tail bar):
 *   E3 (combat gear / skilling kit) - no combat mechanic; chest opens only
 *   E4 (loop detection)             - one-shot open interaction; no loop
 *   E5 (safespot / strategy note)   - no combat mechanic on open step
 *   E7 (inventory loadout)          - key bank-and-return (E6) covers access
 *                                     items; full loadout list is overkill
 *
 * Drop rates sourced from the OSRS Wiki chest/locker pages. Item IDs for
 * access keys cross-checked via RuneLite ItemID constants:
 *   KONAR_KEY=23083, SLAYER_WILDERNESS_KEY=23490, PRIF_CRYSTAL_KEY=23951,
 *   ZOMBIE_PIRATE_WILDY_KEY=29449.
 */
public class D4Batch10aRegressionTest
{
	/** All seven sources in this batch. */
	private static final List<String> BATCH10A_SOURCES = List.of(
		"Brimstone Chest",
		"Larran's Big Chest",
		"Elven Crystal Chest",
		"Zombie Pirate Locker",
		"Lost Schematics",
		"Fossil Island Notes",
		"Monkey Backpacks");

	/**
	 * Key-gated sources: the open step must carry a requiredItemIds list so
	 * the plugin redirects players to the bank when the key is missing.
	 */
	private static final List<String> KEY_GATED_SOURCES = List.of(
		"Brimstone Chest",
		"Larran's Big Chest",
		"Elven Crystal Chest",
		"Zombie Pirate Locker");

	/**
	 * Sources with a quest or skill gate that must have a requirements object.
	 * Monkey Backpacks already had requirements before this batch.
	 */
	private static final List<String> GATED_SOURCES = List.of(
		"Elven Crystal Chest",
		"Lost Schematics",
		"Fossil Island Notes",
		"Monkey Backpacks");

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
	public void allBatch10aSourcesHaveOpenChestBarShape()
	{
		for (String name : BATCH10A_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 10a source: " + name);

			// E1 - travel tip present and non-blank
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// E2 - at least one ARRIVE_AT_TILE step with positive world coordinates
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// E9 - items list non-empty; killTimeSeconds > 0
			assertNotNull(source.getItems(),
				name + " has no items list");
			assertTrue(!source.getItems().isEmpty(),
				name + " items list is empty");
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// E10 (structural) - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	@Test
	public void keyGatedSourcesHaveRequiredItemIdsOnOpenStep()
	{
		for (String name : KEY_GATED_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing key-gated batch 10a source: " + name);

			// E6 - at least one step must carry a non-empty requiredItemIds list
			boolean hasKeyRequirement = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasKeyRequirement,
				name + " has no step with requiredItemIds (key bank-and-return missing)");
		}
	}

	@Test
	public void gatedSourcesHaveRequirementsObject()
	{
		for (String name : GATED_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing gated batch 10a source: " + name);

			// E8 - requirements object present with at least one quest or skill entry
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			boolean hasQuestOrSkill =
				(source.getRequirements().getQuests() != null
					&& !source.getRequirements().getQuests().isEmpty())
				|| (source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty());
			assertTrue(hasQuestOrSkill,
				name + " requirements has neither quests nor skills entries");
		}
	}
}
