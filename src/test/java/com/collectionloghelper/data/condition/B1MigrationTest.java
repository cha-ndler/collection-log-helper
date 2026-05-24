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
package com.collectionloghelper.data.condition;

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.GuidanceStep;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 Phase 1+2 migration guard.
 *
 * <p>The B1 schema landing is strictly additive: a new {@code conditionTree}
 * slot on {@link GuidanceStep}, but zero existing production sources opt in.
 * This test loads the full production {@code drop_rates.json} and asserts that
 * every step on every source has a null {@code conditionTree}. Phase 3 (a
 * separate later PR) is the first opportunity to flip a single source.
 *
 * <p>Mirrors the B2 / B3 / B5 regression-guard pattern: walk the database,
 * accumulate violations, fail with one readable message.
 */
public class B1MigrationTest
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

	@Test
	public void everyStepHasNullConditionTree()
	{
		List<String> violations = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (int i = 0; i < steps.size(); i++)
			{
				GuidanceStep step = steps.get(i);
				if (step == null)
				{
					continue;
				}
				if (step.getConditionTree() != null)
				{
					violations.add(source.getName() + " step " + (i + 1)
						+ " has non-null conditionTree; Phase 1+2 must not pilot any source");
				}
			}
		}

		assertTrue(violations.isEmpty(),
			"B1 Phase 1+2 invariant violated. No production source may set conditionTree until Phase 3. Violations:\n"
				+ String.join("\n", violations));
	}

	@Test
	public void databaseLoadsAndProducesSources()
	{
		assertNotNull(database.getAllSources());
		assertTrue(database.getAllSources().size() > 0,
			"Production drop_rates.json must load at least one source");
	}
}
