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
 * D2 batch 6 audit guard - asserts the ten wilderness boss + Barrows + clue +
 * skilling minigame sources exist by name and each carries at least three
 * guidance steps after the deep-guidance pass.
 *
 * The four wilderness bosses (Vet'ion, Callisto, Venenatis, King Black Dragon)
 * and Barrows are asserted on guidance shape only - the JSON does not currently
 * model a "Wilderness" requirement and Barrows' Priest in Peril prereq is
 * already present (verified by the shape assertion via getRequirements()).
 *
 * The three clue tiers (Hard, Elite, Master) have no skill or quest prereq.
 *
 * The two skilling minigames (Wintertodt, Tempoross) gate on Firemaking 50 and
 * Fishing 35 respectively - both must declare a source-level skill requirement.
 */
public class WildernessBarrowsCluesMinigamesDeepGuidanceAuditTest
{
	private static final List<String> WILDERNESS_BOSS_SOURCES = List.of(
		"Vet'ion",
		"Callisto",
		"Venenatis",
		"King Black Dragon");

	private static final List<String> BARROWS_SOURCE = List.of(
		"Barrows");

	private static final List<String> CLUE_TIER_SOURCES = List.of(
		"Hard Treasure Trails",
		"Elite Treasure Trails",
		"Master Treasure Trails");

	private static final List<String> SKILLING_MINIGAME_SOURCES = List.of(
		"Wintertodt",
		"Tempoross");

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
	public void allFourWildernessBossSourcesHaveDeepGuidanceShape()
	{
		for (String name : WILDERNESS_BOSS_SOURCES)
		{
			CollectionLogSource source = findByName(name);
			assertNotNull(source, "missing Wilderness boss source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
		}
	}

	@Test
	public void barrowsHasDeepGuidanceShapeAndQuestRequirement()
	{
		for (String name : BARROWS_SOURCE)
		{
			CollectionLogSource source = findByName(name);
			assertNotNull(source, "missing source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(),
				name + " has no source-level requirements");
			assertNotNull(source.getRequirements().getQuests(),
				name + " has no source-level quest requirements");
			assertTrue(!source.getRequirements().getQuests().isEmpty(),
				name + " source-level quest requirements list is empty");
		}
	}

	@Test
	public void allThreeClueTierSourcesHaveDeepGuidanceShape()
	{
		for (String name : CLUE_TIER_SOURCES)
		{
			CollectionLogSource source = findByName(name);
			assertNotNull(source, "missing clue tier source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
		}
	}

	@Test
	public void bothSkillingMinigamesHaveDeepGuidanceShapeAndSkillRequirement()
	{
		for (String name : SKILLING_MINIGAME_SOURCES)
		{
			CollectionLogSource source = findByName(name);
			assertNotNull(source, "missing skilling minigame source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(),
				name + " has no source-level requirements");
			assertNotNull(source.getRequirements().getSkills(),
				name + " has no source-level skill requirements");
			assertTrue(!source.getRequirements().getSkills().isEmpty(),
				name + " source-level skill requirements list is empty");
		}
	}

	private CollectionLogSource findByName(String name)
	{
		return database.getAllSources().stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElse(null);
	}
}
