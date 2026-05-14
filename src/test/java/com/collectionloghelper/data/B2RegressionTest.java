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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * Backward-compatibility regression test for the B2 tile-sequence pathing schema
 * extension.
 *
 * <p>The {@link GuidanceStep#getWaypoints()} field is strictly opt-in: no production
 * guidance step in {@code drop_rates.json} should declare a {@code waypoints} array
 * until an explicit D-tier data-backfill PR adds sequences. This test loads the real
 * production JSON and asserts that every step's {@code waypoints} field is {@code null}.
 *
 * <p>If this test ever fails, it means a data contributor has added waypoints to a
 * production step without a corresponding schema bump — the test failure is the
 * intended signal.
 */
public class B2RegressionTest
{
	private DropRateDatabase database;

	@Before
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();

		// Inject Gson via reflection (normally handled by Guice @Inject in production)
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);

		database.load();
	}

	/**
	 * Asserts that every {@link GuidanceStep} in every source in the production
	 * {@code drop_rates.json} has {@code waypoints == null}.
	 *
	 * <p>Production data must stay on the legacy single-target shape until a
	 * D-tier backfill PR explicitly migrates specific sources.
	 */
	@Test
	public void allProductionSteps_waypointsIsNull()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (GuidanceStep step : steps)
			{
				assertNull(
					"B2 regression: source '" + source.getName()
						+ "' step '" + step.getDescription()
						+ "' unexpectedly has a non-null 'waypoints' list. "
						+ "Production data must not use the B2 waypoints field "
						+ "until a D-tier backfill PR is merged.",
					step.getWaypoints()
				);
			}
		}
	}
}
