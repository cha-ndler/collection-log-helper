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
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B.5.4 production-data regression test.
 *
 * <p>Loads the production {@code drop_rates.json} and validates the structural
 * correctness of the {@link GuidanceStep#getSection()} field:
 * <ul>
 *   <li>The field must deserialise from JSON without errors (null = absent, which is valid).</li>
 *   <li>When a section value IS present it must be a non-empty, non-blank string — an
 *       empty {@code "section":""} is an authoring bug that should be caught here.</li>
 * </ul>
 *
 * <p>The {@code section} field is opt-in and additive. Sources without any section
 * labels continue to use the flat panel layout unchanged.
 */
public class B54RegressionTest
{
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
	 * Wherever a guidance step has a non-null section, the value must be a non-blank
	 * string. An empty section label is an authoring error that would render an
	 * unnamed group header in the panel.
	 */
	@Test
	public void productionData_sectionWhenPresent_isNonBlank()
	{
		assertNotNull( database.getAllSources(),"Database must load successfully");

		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (GuidanceStep step : steps)
			{
				String section = step.getSection();
				if (section == null)
				{
					continue; // null is valid — step renders in flat layout
				}
				assertFalse(
					section.trim().isEmpty()
				,
					"section must not be blank when present."
						+ " Source: " + source.getName()
						+ ", step: \"" + step.getDescription() + "\"");
			}
		}
	}
}
