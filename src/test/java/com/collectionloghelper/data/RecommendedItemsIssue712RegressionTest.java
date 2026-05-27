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
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Issue #712 guard - two skilling sources carried mis-authored recommendedItemIds.
 *
 * Fishing (Swordfish) listed Saradomin halo (12637), which has nothing to do with
 * harpoon fishing; the correct aids are a Harpoon or better.
 *
 * Aerial Fishing listed three Pearl fishing rods (22842/22844/22846) plus Needle
 * (1733); aerial fishing equips a free cormorant glove and uses gathered bait, so
 * the only genuine recommended item is a Knife (946) for cutting fish offcuts.
 */
public class RecommendedItemsIssue712RegressionTest
{
	private static final String SWORDFISH = "Fishing (Swordfish)";
	private static final String AERIAL_FISHING = "Aerial Fishing";

	private static final int SARADOMIN_HALO = 12637;
	private static final int HARPOON = 311;
	private static final int DRAGON_HARPOON = 21028;
	private static final int INFERNAL_HARPOON = 21031;
	private static final int CRYSTAL_HARPOON = 23762;

	private static final int KNIFE = 946;
	private static final int NEEDLE = 1733;
	private static final List<Integer> PEARL_RODS = List.of(22842, 22844, 22846);

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

	private CollectionLogSource source(String name)
	{
		CollectionLogSource source = database.getAllSources().stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElse(null);
		assertNotNull(source, "missing source: " + name);
		return source;
	}

	private List<Integer> recommendedItemIds(String name)
	{
		return source(name).getGuidanceSteps().stream()
			.filter(s -> s.getRecommendedItemIds() != null && !s.getRecommendedItemIds().isEmpty())
			.flatMap(s -> s.getRecommendedItemIds().stream())
			.distinct()
			.collect(Collectors.toList());
	}

	@Test
	public void swordfishHasHarpoonsNotHalo()
	{
		List<Integer> recommended = recommendedItemIds(SWORDFISH);

		assertFalse(recommended.contains(SARADOMIN_HALO),
			SWORDFISH + " must not recommend Saradomin halo (12637)");

		assertTrue(recommended.contains(HARPOON),
			SWORDFISH + " should recommend Harpoon (311)");
		assertTrue(recommended.contains(DRAGON_HARPOON),
			SWORDFISH + " should recommend Dragon harpoon (21028)");
		assertTrue(recommended.contains(INFERNAL_HARPOON),
			SWORDFISH + " should recommend Infernal harpoon (21031)");
		assertTrue(recommended.contains(CRYSTAL_HARPOON),
			SWORDFISH + " should recommend Crystal harpoon (23762)");
	}

	@Test
	public void aerialFishingHasKnifeNotPearlRodsOrNeedle()
	{
		List<Integer> recommended = recommendedItemIds(AERIAL_FISHING);

		assertFalse(recommended.contains(NEEDLE),
			AERIAL_FISHING + " must not recommend Needle (1733)");
		for (int pearlRod : PEARL_RODS)
		{
			assertFalse(recommended.contains(pearlRod),
				AERIAL_FISHING + " must not recommend Pearl rod (" + pearlRod + ")");
		}

		assertTrue(recommended.contains(KNIFE),
			AERIAL_FISHING + " should recommend Knife (946)");
	}
}
