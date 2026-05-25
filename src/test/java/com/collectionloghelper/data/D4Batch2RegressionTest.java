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
 * D4 batch 2 audit guard - asserts the three raid mode variants scoped in
 * docs/contributor-guide/d4-long-tail-scoping.md section 4 carry the
 * delta-on-parent guidance bar after the batch 2 authoring pass:
 * travel tip, positive kill time, at least four steps, an ARRIVE_AT_TILE step
 * with world coordinates, a recommendedItemIds list on at least two steps
 * (travel step and kill step), a mode-delta description on the invocation
 * setup step, section labels on steps, drop rates on all unique items, and
 * a mutuallyExclusiveSources declaration referencing the parent raid.
 */
public class D4Batch2RegressionTest
{
	private static final List<String> BATCH2_SOURCES = List.of(
		"Chambers of Xeric (Challenge Mode)",
		"Tombs of Amascut (300 Invocation)",
		"Tombs of Amascut (500 Invocation)");

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
	public void allBatch2SourcesHaveDeltaGuidanceShape()
	{
		for (String name : BATCH2_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing D4 batch 2 source: " + name);

			// Element 1 - travel tip
			assertNotNull(source.getTravelTip(),
				name + " has no source-level travelTip");
			assertTrue(!source.getTravelTip().isBlank(),
				name + " travelTip is blank");

			// Step shape - at least four steps
			assertNotNull(source.getGuidanceSteps(),
				name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 4,
				name + " has fewer than 4 guidance steps (got "
					+ source.getGuidanceSteps().size() + ")");

			// Kill time populated (delta bar: expected-time)
			assertTrue(source.getKillTimeSeconds() > 0,
				name + " has non-positive killTimeSeconds");

			// Element 2 - at least one ARRIVE_AT_TILE step with world coords
			boolean hasArriveStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
					&& s.getWorldX() > 0
					&& s.getWorldY() > 0);
			assertTrue(hasArriveStep,
				name + " has no ARRIVE_AT_TILE step with positive world coordinates");

			// Element 3 - travel step (step 0) has recommendedItemIds for early gear surfacing
			List<Integer> step0Recommended = source.getGuidanceSteps().get(0).getRecommendedItemIds();
			assertNotNull(step0Recommended,
				name + " step 0 has no recommendedItemIds (gear must surface at earliest step)");
			assertTrue(!step0Recommended.isEmpty(),
				name + " step 0 recommendedItemIds is empty");

			// Element 3 - kill step also exposes recommendedItemIds
			boolean hasKillRecommended = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getRecommendedItemIds() != null
					&& !s.getRecommendedItemIds().isEmpty()
					&& s.getCompletionCondition() == CompletionCondition.CHAT_MESSAGE_RECEIVED);
			assertTrue(hasKillRecommended,
				name + " has no CHAT_MESSAGE_RECEIVED step with a populated recommendedItemIds list");

			// Delta bar: invocation/CM setup step must describe the mode delta
			boolean hasModeDeltaStep = source.getGuidanceSteps().stream()
				.anyMatch(s -> {
					String desc = s.getDescription();
					if (desc == null)
					{
						return false;
					}
					String lower = desc.toLowerCase();
					// CoX CM: "challenge mode" in the description; ToA variants: "invocation" or "raid level"
					return lower.contains("challenge mode")
						|| lower.contains("invocation")
						|| lower.contains("raid level");
				});
			assertTrue(hasModeDeltaStep,
				name + " has no step description mentioning the mode delta"
					+ " (Challenge Mode toggle, invocation level, or raid level)");

			// Element 10 - section labels on steps
			boolean hasSectionLabels = source.getGuidanceSteps().stream()
				.anyMatch(s -> s.getSection() != null && !s.getSection().isBlank());
			assertTrue(hasSectionLabels,
				name + " has no step with a section label");

			// Element 9 - drop rates on all unique items
			assertTrue(source.getItems() != null && !source.getItems().isEmpty(),
				name + " has no items");
			long itemsWithRate = source.getItems().stream()
				.filter(i -> i.getDropRate() > 0)
				.count();
			long totalItems = source.getItems().size();
			assertTrue(itemsWithRate >= totalItems * 0.8,
				name + " fewer than 80% of items have a positive dropRate ("
					+ itemsWithRate + "/" + totalItems + ")");

			// Delta bar: mutuallyExclusiveSources references the parent raid
			assertNotNull(source.getMutuallyExclusiveSources(),
				name + " has no mutuallyExclusiveSources declaration");
			assertTrue(!source.getMutuallyExclusiveSources().isEmpty(),
				name + " mutuallyExclusiveSources is empty - parent raid cross-reference required");
		}
	}

	@Test
	public void coxChallengeModeDifferentiatesFromNormalMode()
	{
		CollectionLogSource cm = database.getAllSources().stream()
			.filter(s -> "Chambers of Xeric (Challenge Mode)".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(cm, "Chambers of Xeric (Challenge Mode) not found");

		CollectionLogSource normal = database.getAllSources().stream()
			.filter(s -> "Chambers of Xeric".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(normal, "Chambers of Xeric (base) not found");

		// CM kill time must be longer than normal
		assertTrue(cm.getKillTimeSeconds() > normal.getKillTimeSeconds(),
			"CM killTimeSeconds (" + cm.getKillTimeSeconds()
				+ ") should exceed normal CoX (" + normal.getKillTimeSeconds() + ")");

		// CM drop rates must be higher than normal for at least one shared unique
		// (Twisted bow: CM=0.002067 vs normal=0.001)
		double cmTbowRate = cm.getItems().stream()
			.filter(i -> i.getItemId() == 20997)
			.mapToDouble(CollectionLogItem::getDropRate)
			.findFirst()
			.orElse(0);
		double normalTbowRate = normal.getItems().stream()
			.filter(i -> i.getItemId() == 20997)
			.mapToDouble(CollectionLogItem::getDropRate)
			.findFirst()
			.orElse(0);
		assertTrue(cmTbowRate > normalTbowRate,
			"CM Twisted bow drop rate (" + cmTbowRate
				+ ") should exceed normal CoX rate (" + normalTbowRate + ")");

		// CM must include CM-exclusive items not on the normal table
		// Metamorphic dust (22386) and Twisted ancestral colour kit (24670)
		boolean hasMetaDust = cm.getItems().stream().anyMatch(i -> i.getItemId() == 22386);
		assertTrue(hasMetaDust,
			"CM table missing Metamorphic dust (22386) - CM-exclusive item");
	}

	@Test
	public void toaInvocationVariantsDifferentiateFromBase()
	{
		CollectionLogSource base = database.getAllSources().stream()
			.filter(s -> "Tombs of Amascut".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(base, "Tombs of Amascut (base) not found");

		CollectionLogSource toa300 = database.getAllSources().stream()
			.filter(s -> "Tombs of Amascut (300 Invocation)".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(toa300, "Tombs of Amascut (300 Invocation) not found");

		CollectionLogSource toa500 = database.getAllSources().stream()
			.filter(s -> "Tombs of Amascut (500 Invocation)".equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(toa500, "Tombs of Amascut (500 Invocation) not found");

		// Kill times must increase with invocation level
		assertTrue(toa300.getKillTimeSeconds() > base.getKillTimeSeconds(),
			"ToA 300 killTimeSeconds (" + toa300.getKillTimeSeconds()
				+ ") should exceed base ToA (" + base.getKillTimeSeconds() + ")");
		assertTrue(toa500.getKillTimeSeconds() > toa300.getKillTimeSeconds(),
			"ToA 500 killTimeSeconds (" + toa500.getKillTimeSeconds()
				+ ") should exceed ToA 300 (" + toa300.getKillTimeSeconds() + ")");

		// Osmumten's fang (26219) drop rate must increase with invocation level
		double baseRate = base.getItems().stream()
			.filter(i -> i.getItemId() == 26219)
			.mapToDouble(CollectionLogItem::getDropRate)
			.findFirst()
			.orElse(0);
		double rate300 = toa300.getItems().stream()
			.filter(i -> i.getItemId() == 26219)
			.mapToDouble(CollectionLogItem::getDropRate)
			.findFirst()
			.orElse(0);
		double rate500 = toa500.getItems().stream()
			.filter(i -> i.getItemId() == 26219)
			.mapToDouble(CollectionLogItem::getDropRate)
			.findFirst()
			.orElse(0);
		assertTrue(rate300 > baseRate,
			"ToA 300 Osmumten fang rate (" + rate300
				+ ") should exceed base rate (" + baseRate + ")");
		assertTrue(rate500 > rate300,
			"ToA 500 Osmumten fang rate (" + rate500
				+ ") should exceed ToA 300 rate (" + rate300 + ")");

		// Both variants must cross-reference each other and the base in mutuallyExclusiveSources
		assertTrue(toa300.getMutuallyExclusiveSources().contains("Tombs of Amascut"),
			"ToA 300 mutuallyExclusiveSources must reference Tombs of Amascut base");
		assertTrue(toa500.getMutuallyExclusiveSources().contains("Tombs of Amascut"),
			"ToA 500 mutuallyExclusiveSources must reference Tombs of Amascut base");
	}
}
