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
 * D2 batch 4 audit guard — asserts the top solo boss sources exist by name,
 * each carries at least three guidance steps after the deep-guidance pass,
 * and each declares a source-level requirements object. Solo bosses gate
 * mostly through quests rather than skills (Corporeal Beast has no gate at
 * all), so the requirements object itself must be present but its skills
 * list may be empty.
 */
public class SoloBossesDeepGuidanceAuditTest
{
	private static final List<String> SOLO_BOSS_SOURCES = List.of(
		"Vorkath",
		"Zulrah",
		"Phantom Muspah",
		"The Nightmare",
		"Phosani's Nightmare",
		"Corporeal Beast",
		"The Gauntlet",
		"Corrupted Gauntlet");

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
	public void allSoloBossSourcesHaveDeepGuidanceShape()
	{
		for (String name : SOLO_BOSS_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing solo boss source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(),
				name + " has no source-level requirements object");
		}
	}
}
