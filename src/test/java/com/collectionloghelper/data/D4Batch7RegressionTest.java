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
 * D4 batch 7 audit guard - asserts the eight recent slayer additions and
 * Varlamore sources scoped in docs/contributor-guide/d4-long-tail-scoping.md
 * section 4 (Batch 7) carry the full deep-guidance bar after the batch 7
 * authoring pass: travel tip, requirements object, at least three steps,
 * positive kill time, an ARRIVE_AT_TILE or MANUAL step, a recommendedItemIds
 * list on the kill step (where applicable), and section labels on steps.
 *
 * Superior Slayer Monster is task-agnostic per Q4 option (a) in the scoping
 * doc: it carries two MANUAL steps (Note + Combat) and no travel tip, which
 * is the correct shape for a cross-task umbrella source.
 */
public class D4Batch7RegressionTest
{
	/**
	 * The seven standard slayer sources that receive a full travel + kill flow.
	 * Aquanite and Custodian Stalker have pre-existing regression tests (#548, #555)
	 * that pin their step[0] shape; they are listed separately below.
	 */
	private static final List<String> STANDARD_SOURCES = List.of(
		"Earthen Nagua",
		"Frost Nagua",
		"Sulphur Nagua",
		"Gryphon",
		"Terror Dog");

	/**
	 * Sources with pre-existing regression-test contracts on step count or step[0] shape.
	 * Verified separately so the D4-batch7 assertions do not conflict with #548/#555.
	 */
	private static final List<String> CONSTRAINED_SOURCES = List.of(
		"Custodian Stalker",
		"Aquanite");

	/**
	 * Superior Slayer Monster is task-agnostic: no travel tip, two MANUAL steps.
	 */
	private static final String SUPERIOR_SOURCE = "Superior Slayer Monster";

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
	public void standardBatch7SourcesHaveDeepGuidanceShape()
	{
		for (String name : STANDARD_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 7 source: " + name);

			// Element 1 - travel tip present and non-blank
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape - at least three steps after the deep pass
			// (Bank + Travel + Combat minimum; sources with dungeon descent have 4)
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps after the deep-guidance pass (got "
					+ source.getGuidanceSteps().size() + ")");

			// Element 9 - kill time populated
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// Element 2 - at least one ARRIVE_AT_TILE or ARRIVE_AT_ZONE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> (s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
						|| s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_ZONE)
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE or ARRIVE_AT_ZONE step with positive world coordinates");

			// Element 3 - kill step exposes recommendedItemIds
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear,
				name + " has no step with a populated recommendedItemIds list");

			// Element 8 - requirements object with at least one skill entry
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			assertTrue(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty(),
				name + " requirements has no skill entries");

			// Element 10 - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}

	@Test
	public void constrainedBatch7SourcesHaveDeepGuidanceMinimum()
	{
		for (String name : CONSTRAINED_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 7 source: " + name);

			// Travel tip present
			assertNotNull(source.getTravelTip(), name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(), name + " travelTip is blank");

			// Kill time populated
			assertTrue(source.getKillTimeSeconds() > 0, name + " has non-positive killTimeSeconds");

			// Requirements present with at least one skill
			assertNotNull(source.getRequirements(), name + " has no requirements object");
			assertTrue(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty(),
				name + " requirements has no skill entries");

			// Has recommendedItemIds on at least one step (E3)
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			boolean hasRecommendedGear = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty());
			assertTrue(hasRecommendedGear, name + " has no step with recommendedItemIds");

			// Section labels present (E10 structural signal)
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels, name + " has no step with a section label");
		}
	}

	@Test
	public void superiorSlayerMonsterHasTaskAgnosticShape()
	{
		CollectionLogSource source = database.getAllSources().stream()
			.filter(s -> SUPERIOR_SOURCE.equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(source, "missing D4 batch 7 source: " + SUPERIOR_SOURCE);

		// Q4(a): task-agnostic source - travelTip is intentionally empty
		assertNotNull(source.getTravelTip(),
			SUPERIOR_SOURCE + " travelTip field must be present (empty string is valid)");

		// Two MANUAL steps: Note + Combat
		assertNotNull(source.getGuidanceSteps(),
			SUPERIOR_SOURCE + " has no guidanceSteps");
		assertTrue(source.getGuidanceSteps().size() >= 2,
			SUPERIOR_SOURCE + " has fewer than 2 guidance steps (got "
				+ source.getGuidanceSteps().size() + ")");

		boolean allManual = source.getGuidanceSteps().stream()
			.allMatch(s -> s.getCompletionCondition() == CompletionCondition.MANUAL);
		assertTrue(allManual,
			SUPERIOR_SOURCE + " should have only MANUAL steps (task-agnostic source)");

		// Section labels present (Note + Combat)
		boolean hasSectionLabels = source.getGuidanceSteps().stream()
			.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
		assertTrue(hasSectionLabels,
			SUPERIOR_SOURCE + " has no step with a section label");

		// Element 8 - minimum Slayer level prerequisite
		assertNotNull(source.getRequirements(),
			SUPERIOR_SOURCE + " has no requirements object");
		assertTrue(source.getRequirements().getSkills() != null
				&& !source.getRequirements().getSkills().isEmpty(),
			SUPERIOR_SOURCE + " requirements has no skill entries");

		// Element 9 - kill time populated
		assertTrue(source.getKillTimeSeconds() > 0,
			SUPERIOR_SOURCE + " has non-positive killTimeSeconds");

		// Items table must be present
		assertNotNull(source.getItems(),
			SUPERIOR_SOURCE + " has no items");
		assertTrue(!source.getItems().isEmpty(),
			SUPERIOR_SOURCE + " items list is empty");
	}
}
