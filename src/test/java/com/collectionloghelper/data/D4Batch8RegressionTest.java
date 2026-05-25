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
 * D4 batch 8 audit guard - asserts the eight salvage-table sources scoped in
 * docs/contributor-guide/d4-long-tail-scoping.md section 4 (Batch 8) carry the
 * reduced shop/table bar after the batch 8 authoring pass.
 *
 * The shop/table bar for salvage sources is:
 *   E1  - source-level travelTip present and non-blank
 *   E2  - at least one ARRIVE_AT_TILE step with positive world coordinates
 *   E8  - requirements object with at least one SAILING skill entry
 *   E9  - killTimeSeconds > 0; items list non-empty
 *   E10 - section labels on steps (structural signal; one batch-level citation
 *          covers all eight sources in the PR description)
 *
 * Elements NOT asserted (intentionally dropped per long-tail bar):
 *   E3 (combat gear / skilling kit) - no combat mechanic; salvage is sorted passively
 *   E4 (loop detection)             - one-shot interaction per salvage sort; no loop
 *   E5 (safespot / strategy note)   - no combat mechanic
 *   E6 (bank-and-return)            - no consumable access item required; waived
 *   E7 (inventory loadout)          - no special items to bring; waived
 *
 * Sailing-dependency flag: all eight sources require Sailing skill levels (15-80).
 * Drop rates are sourced from the OSRS Wiki salvage table pages (verified against
 * the in-game interface post-Sailing release). Item IDs cross-checked via
 * TempleOSRS canonical ID list.
 */
public class D4Batch8RegressionTest
{
	private static final List<String> BATCH8_SOURCES = List.of(
		"Barracuda Salvage",
		"Fishy Salvage",
		"Fremennik Salvage",
		"Large Salvage",
		"Martial Salvage",
		"Opulent Salvage",
		"Plundered Salvage",
		"Small Salvage");

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
	public void allBatch8SourcesHaveShopTableBarShape()
	{
		for (String name : BATCH8_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 8 source: " + name);

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

			// E8 - requirements object with at least one skill entry (SAILING)
			assertNotNull(source.getRequirements(),
				name + " has no requirements object");
			assertTrue(source.getRequirements().getSkills() != null
					&& !source.getRequirements().getSkills().isEmpty(),
				name + " requirements has no skill entries");

			// E9 - kill time populated; items list non-empty
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");
			assertNotNull(source.getItems(),
				name + " has no items list");
			assertTrue(!source.getItems().isEmpty(),
				name + " items list is empty");

			// E10 (structural) - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");
		}
	}
}
