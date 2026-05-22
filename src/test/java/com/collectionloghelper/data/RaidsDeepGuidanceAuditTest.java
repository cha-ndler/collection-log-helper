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
 * D2 batch 3 audit guard - asserts the raid sources exist by name, each carries
 * at least three guidance steps after the deep-guidance pass, and each declares
 * a source-level requirements object. Raids do not have hard skill gates so the
 * skills list may be empty, but the requirements object itself must be present.
 */
public class RaidsDeepGuidanceAuditTest
{
	private static final List<String> RAID_SOURCES = List.of(
		"Chambers of Xeric",
		"Chambers of Xeric (Challenge Mode)",
		"Theatre of Blood",
		"Theatre of Blood (Hard Mode)",
		"Tombs of Amascut",
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
	public void allRaidSourcesHaveDeepGuidanceShape()
	{
		for (String name : RAID_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing raid source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(),
				name + " has no source-level requirements object");
		}
	}
}
