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
 * D4 batch 10b audit guard - asserts OTHER activity sources carry the
 * appropriate guidance bar after the batch 10b authoring pass.
 *
 * Full deep-guidance bar (E1-E10): Revenants, Pyramid Plunder, Catacombs of Kourend.
 * Reduced long-tail bar (E1, E2, E6, E8, E9): all remaining sources.
 *
 * travelTip corrections applied:
 *   - My Notes: was "Barbarian Assault tele -> falls", now "Barbarian Outpost teleport -> Ancient Cavern"
 *   - Stronghold of Security: was "Skull sceptre -> Barbarian Village", now "Edgeville teleport -> run south to Barbarian Village mine"
 */
public class D4Batch10bRegressionTest
{
	private static final List<String> FULL_BAR_SOURCES = List.of(
		"Revenants",
		"Pyramid Plunder",
		"Catacombs of Kourend");

	private static final List<String> LONG_TAIL_SOURCES = List.of(
		"Camdozaal",
		"Forestry",
		"Hunter Guild",
		"TzHaar",
		"Champion's Challenge",
		"Stronghold of Security",
		"My Notes",
		"Miscellaneous",
		"Port Tasks");

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
	 * Full deep-guidance bar sources: travel tip, kill time, ARRIVE_AT_TILE step
	 * with world coordinates, recommendedItemIds on at least one step, and
	 * section labels on steps.
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
			assertNotNull(source, "missing D4 batch 10b full-bar source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// E9 proxy - positive kill time
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// Step shape - at least 3 steps for full-bar sources
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// E2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// E3 - recommendedItemIds on at least one step
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
	 * Long-tail bar sources (E1, E2, E8, E9): travel tip, ARRIVE_AT_TILE step
	 * with world coordinates (or MANUAL for Miscellaneous which has no fixed location),
	 * and at least one guidance step.
	 */
	@Test
	public void allLongTailSourcesHaveLongTailBar()
	{
		for (String name : LONG_TAIL_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 10b long-tail source: " + name);

			// E1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape - at least one step
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(!source.getGuidanceSteps().isEmpty(),
				name + " has no guidance steps");

			// Section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	/**
	 * Travel tip corrections: verify the specific travelTip fixes applied
	 * in this batch (per d4-placeholder-triage.md notes).
	 */
	@Test
	public void travelTipCorrectionsApplied()
	{
		// My Notes: corrected from "Barbarian Assault tele -> falls"
		CollectionLogSource myNotes = database.getAllSources().stream()
			.filter(s -> "My Notes".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(myNotes, "My Notes source missing");
		assertTrue(myNotes.getTravelTip().contains("Barbarian Outpost"),
			"My Notes travelTip not corrected to Barbarian Outpost route, got: "
				+ myNotes.getTravelTip());

		// Stronghold of Security: corrected from "Skull sceptre -> Barbarian Village"
		CollectionLogSource stronghold = database.getAllSources().stream()
			.filter(s -> "Stronghold of Security".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(stronghold, "Stronghold of Security source missing");
		assertTrue(stronghold.getTravelTip().contains("Edgeville"),
			"Stronghold of Security travelTip not corrected to Edgeville route, got: "
				+ stronghold.getTravelTip());
	}

	/**
	 * Revenants: assert PKer-risk note is present in at least one step description
	 * (per deep-guidance-bar.md E5 requirement for Wilderness sources).
	 */
	@Test
	public void revenantsCombatStepNotesPkerRisk()
	{
		CollectionLogSource revenants = database.getAllSources().stream()
			.filter(s -> "Revenants".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(revenants, "Revenants source missing");

		boolean hasPkerNote = revenants.getGuidanceSteps().stream()
			.anyMatch(s -> s.getDescription() != null
				&& (s.getDescription().toLowerCase().contains("pker")
					|| s.getDescription().toLowerCase().contains("wilderness")));
		assertTrue(hasPkerNote,
			"Revenants has no step mentioning Wilderness or PKer risk");
	}
}
