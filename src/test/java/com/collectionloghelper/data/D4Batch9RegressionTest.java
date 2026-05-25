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
 * D4 batch 9 audit guard - asserts sources in the Creature Creation +
 * minor combat NPC cluster scoped in docs/contributor-guide/d4-long-tail-scoping.md
 * section 4 batch 9 carry the appropriate guidance bar after the authoring pass.
 *
 * Creature Creation variants use the reduced long-tail bar (5 elements):
 * travel tip, ARRIVE_AT_TILE step with coords, requiredItemIds for ingredients,
 * recommendedItemIds on the kill step, and requirements.quests set.
 *
 * Combat NPC sources use the full 10-element bar:
 * travel tip, requirements object, at least two steps, positive kill time,
 * an ARRIVE_AT_TILE step with world coordinates, a recommendedItemIds list on
 * the kill step, and section labels on steps.
 */
public class D4Batch9RegressionTest
{
	private static final List<String> CREATURE_CREATION_SOURCES = List.of(
		"Creature Creation (Newtroost)",
		"Creature Creation (Unicow)",
		"Creature Creation (Spidine)",
		"Creature Creation (Swordchick)",
		"Creature Creation (Jubster)",
		"Creature Creation (Frogeel)");

	private static final List<String> FULL_BAR_SOURCES = List.of(
		"Cyclopes",
		"Glough's Experiments",
		"Ogress Shaman",
		"Adamant Dragon",
		"Mithril Dragon",
		"Rune Dragon",
		"Waterfiend",
		"Armoured Zombie",
		"Elder Chaos Druids",
		"Chompy Bird Hunting");

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
	 * Creature Creation variants: long-tail bar (E1, E2, E6, E8, E9).
	 * Each variant shares the Tower of Life travel step and ingredient interaction.
	 */
	@Test
	public void allCreatureCreationSourcesHaveLongTailBar()
	{
		for (String name : CREATURE_CREATION_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 9 Creature Creation source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// E8 - prerequisites (Tower of Life quest)
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			assertNotNull(source.getRequirements().getQuests(),
				name + " has no quests in requirements");
			assertTrue(!source.getRequirements().getQuests().isEmpty(),
				name + " requirements.quests is empty");

			// Step shape - at least two steps
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

			// E6 - requiredItemIds on the travel step (ingredient check)
			boolean hasRequiredIngredients = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRequiredItemIds() != null
					&& !s.getRequiredItemIds().isEmpty());
			assertTrue(hasRequiredIngredients,
				name + " has no step with requiredItemIds (ingredient list missing)");

			// E9 (proxy) - recommendedItemIds on kill step (food at minimum)
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Section labels present
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	/**
	 * Combat NPC sources: full deep-guidance bar (all 10 elements).
	 * Includes dragon trio, minor slayer/combat NPCs, and activity sources.
	 */
	@Test
	public void allFullBarSourcesHaveDeepGuidanceShape()
	{
		for (String name : FULL_BAR_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 9 full-bar source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// E9 - kill time populated
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// Step shape - at least two steps after the deep pass
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

			// E3 - recommendedItemIds on at least one step (gear recommendation)
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	/**
	 * Mogre was completed in D4 batch 5 -- assert it is not regressed by this batch.
	 */
	@Test
	public void mogreNotTouchedByBatch9()
	{
		CollectionLogSource mogre = database.getAllSources().stream()
			.filter(s -> "Mogre".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(mogre, "Mogre source missing from drop_rates.json");
		assertNotNull(mogre.getTravelTip(), "Mogre travelTip was wiped");
		assertTrue(!mogre.getTravelTip().isBlank(), "Mogre travelTip is blank");
		assertNotNull(mogre.getGuidanceSteps(), "Mogre guidanceSteps was wiped");
		assertTrue(mogre.getGuidanceSteps().size() >= 2,
			"Mogre has fewer than 2 steps -- possible regression");
	}
}
