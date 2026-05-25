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
 * D4 batch 12b audit guard - asserts the six clue rare-reward sub-table sources
 * (Hard/Elite/Master Treasure Trail Rewards (Rare), Shared Treasure Trail Rewards,
 * Scroll Cases, The Mimic) carry the reduced long-tail bar after the batch 12b
 * authoring pass: travel tip, at least one ARRIVE_AT_TILE step with world
 * coordinates, section labels on steps, and a requirements object (E8 prerequisite).
 *
 * The full deep-guidance bar (4+ steps, gear recommendations, kill-loop) is not
 * asserted here: these are sub-pages of parent clue-tier activities and are
 * explicitly scoped to the long-tail bar per docs/contributor-guide/deep-guidance-bar.md.
 */
public class D4Batch12bRegressionTest
{
	private static final List<String> BATCH12B_SOURCES = List.of(
		"Hard Treasure Trail Rewards (Rare)",
		"Elite Treasure Trail Rewards (Rare)",
		"Master Treasure Trail Rewards (Rare)",
		"Shared Treasure Trail Rewards",
		"Scroll Cases",
		"The Mimic");

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
	public void allBatch12bSourcesHaveLongTailBar()
	{
		for (String name : BATCH12B_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 12b source: " + name);

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

			// Section labels - at least one step must carry a section label
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");

			// E8 - requirements object present (clue tier prerequisite)
			assertNotNull(source.getRequirements(),
				name + " has no requirements object (E8 prerequisite)");
		}
	}
}
