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
 * D2 batch 1 audit guard — asserts the four GWD boss sources exist by name,
 * each carries at least three guidance steps after the deep-guidance pass,
 * and each declares a source-level skill requirement.
 */
public class GwdDeepGuidanceAuditTest
{
	private static final List<String> GWD_SOURCES = List.of(
		"General Graardor",
		"Commander Zilyana",
		"K'ril Tsutsaroth",
		"Kree'arra");

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
	public void allFourGwdSourcesHaveDeepGuidanceShape()
	{
		for (String name : GWD_SOURCES)
		{
			CollectionLogSource source = database.getAllSources().stream()
				.filter(s -> name.equals(s.getName()))
				.findFirst()
				.orElse(null);
			assertNotNull(source, "missing GWD source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + " has no guidanceSteps");
			assertTrue(source.getGuidanceSteps().size() >= 3,
				name + " has fewer than 3 guidance steps");
			assertNotNull(source.getRequirements(), name + " has no source-level requirements");
			assertNotNull(source.getRequirements().getSkills(),
				name + " has no source-level skill requirements");
			assertTrue(!source.getRequirements().getSkills().isEmpty(),
				name + " source-level skill requirements list is empty");
		}
	}
}
