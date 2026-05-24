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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration guard for the additive {@code inventoryItemIdsAny} field on
 * {@link SourceRequirements}.
 *
 * <p>This PR adds the field, evaluator wiring, and Gson coverage but performs
 * zero pilot wiring in production data. This test walks the full production
 * {@code drop_rates.json} and asserts that every {@link SourceRequirements}
 * instance reached via top-level source requirements, conditional-alternative
 * requirements (flat and nested), and waypoint requirements has a null
 * {@code inventoryItemIdsAny}. The first opportunity to flip a single source
 * is a separate later data PR that wires the 4 deferred multi-tier teleport
 * sources (Tombs of Amascut, Phantom Muspah, The Gauntlet, Corrupted
 * Gauntlet).
 *
 * <p>Mirrors {@link InventoryItemsMigrationTest} and the earlier
 * {@code B1MigrationTest} / {@code B1aRegressionTest} / {@code B1bRegressionTest}
 * pattern: walk the database, accumulate violations, fail with one readable
 * message.
 */
public class InventoryItemsAnyMigrationTest
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
	public void databaseLoadsAndProducesSources()
	{
		assertNotNull(database.getAllSources());
		assertTrue(database.getAllSources().size() > 0,
			"Production drop_rates.json must load at least one source");
	}

	@Test
	public void everySourceRequirementsHasNullInventoryItemIdsAny()
	{
		List<String> violations = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			checkSource(source, violations);
		}

		assertTrue(violations.isEmpty(),
			"inventoryItemIdsAny invariant violated. No production source may set inventoryItemIdsAny in this PR; pilot wiring lands in a follow-up data PR (Tombs of Amascut, Phantom Muspah, The Gauntlet, Corrupted Gauntlet). Violations:\n"
				+ String.join("\n", violations));
	}

	private static void checkSource(CollectionLogSource source, List<String> violations)
	{
		String sourceName = source.getName();
		checkRequirements(sourceName + " (top-level requirements)", source.getRequirements(), violations);

		// CollectionLogSource owns the requirement-bearing Waypoint list. GuidanceStep
		// has a separate StepWaypoint list which is a lightweight overlay struct and
		// carries no SourceRequirements, so it is not walked here.
		List<Waypoint> sourceWaypoints = source.getWaypoints();
		if (sourceWaypoints != null)
		{
			for (int w = 0; w < sourceWaypoints.size(); w++)
			{
				Waypoint wp = sourceWaypoints.get(w);
				if (wp != null)
				{
					checkRequirements(sourceName + " waypoint " + (w + 1),
						wp.getRequirements(), violations);
				}
			}
		}

		List<GuidanceStep> steps = source.getGuidanceSteps();
		if (steps == null)
		{
			return;
		}
		for (int i = 0; i < steps.size(); i++)
		{
			GuidanceStep step = steps.get(i);
			if (step == null)
			{
				continue;
			}
			String stepLabel = sourceName + " step " + (i + 1);

			List<ConditionalAlternative> alts = step.getConditionalAlternatives();
			if (alts != null)
			{
				for (int a = 0; a < alts.size(); a++)
				{
					ConditionalAlternative alt = alts.get(a);
					if (alt == null)
					{
						continue;
					}
					checkRequirements(stepLabel + " alt " + (a + 1),
						alt.getRequirements(), violations);

					List<ConditionalAlternative> nested = alt.getNestedAlternatives();
					if (nested != null)
					{
						for (int n = 0; n < nested.size(); n++)
						{
							ConditionalAlternative nestedAlt = nested.get(n);
							if (nestedAlt != null)
							{
								checkRequirements(stepLabel + " alt " + (a + 1)
										+ " nested " + (n + 1),
									nestedAlt.getRequirements(), violations);
							}
						}
					}
				}
			}
		}
	}

	private static void checkRequirements(String label, SourceRequirements reqs, List<String> violations)
	{
		if (reqs == null)
		{
			return;
		}
		if (reqs.getInventoryItemIdsAny() != null)
		{
			violations.add(label + " has non-null inventoryItemIdsAny");
		}
	}
}
