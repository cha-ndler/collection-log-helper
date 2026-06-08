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
 * D2 batch 2 audit guard — asserts the four DT2 awakened-style boss sources exist
 * by name and each carries at least three guidance steps after the deep-guidance pass.
 * DT2 bosses are gated only by the Desert Treasure II quest; they carry no skill-level
 * access requirement (the wiki lists none), so no skill requirement is asserted here.
 */
public class Dt2DeepGuidanceAuditTest
{
	private static final List<String> DT2_SOURCES = List.of(
		"Duke Sucellus",
		"The Leviathan",
		"The Whisperer",
		"Vardorvis",
		"Duke Sucellus (Awakened)",
		"The Leviathan (Awakened)",
		"The Whisperer (Awakened)",
		"Vardorvis (Awakened)");

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
	public void allFourDt2SourcesHaveDeepGuidanceShape()
	{
		for (String name : DT2_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing DT2 source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(), name + " has no source-level requirements");
			assertNotNull(source.getRequirements().getQuests(),
				name + " has no source-level quest requirements");
			assertTrue(source.getRequirements().getQuests().stream()
					.anyMatch(q -> q.startsWith("DESERT_TREASURE_II")),
				name + " is not gated on the Desert Treasure II quest");
		}
	}
}
