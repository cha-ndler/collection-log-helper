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
 * B1 migration guard.
 *
 * <p>The B1 schema landing is strictly additive: a new {@code conditionTree}
 * slot on {@link GuidanceStep}. Phase 1+2 opted no production source in.
 * Phase 3 begins flipping a small number of pilot sources where the flat
 * enum genuinely misrepresents completion. Each pilot must be listed in
 * {@link #PHASE_3_PILOT_SOURCES} so this guard distinguishes intentional
 * pilots from accidental opt-ins.
 *
 * <p>Mirrors the B2 / B3 / B5 regression-guard pattern: walk the database,
 * accumulate violations, fail with one readable message.
 */
public class B1MigrationTest
{
	/**
	 * Sources that have intentionally opted into {@code conditionTree} via
	 * B1 Phase 3 pilots. Any new entry here must come in the same PR as the
	 * {@code drop_rates.json} edit that adds the tree, and that PR must also
	 * ship a structural regression test under
	 * {@code src/test/java/com/collectionloghelper/data/}.
	 *
	 * <p>Current pilots:
	 * <ul>
	 *   <li>Trouble Brewing - victory-drop step, see
	 *       {@code TroubleBrewingConditionTreePilotTest}.</li>
	 *   <li>Wintertodt - brazier-light / pyromancer-heal step, see
	 *       {@code WintertodtConditionTreePilotTest}.</li>
	 * </ul>
	 */
	private static final java.util.Set<String> PHASE_3_PILOT_SOURCES =
		java.util.Set.of("Trouble Brewing", "Wintertodt");

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
	public void onlyAllowlistedPilotsCarryConditionTree()
	{
		List<String> violations = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			boolean isPilot = PHASE_3_PILOT_SOURCES.contains(source.getName());
			for (int i = 0; i < steps.size(); i++)
			{
				GuidanceStep step = steps.get(i);
				if (step == null)
				{
					continue;
				}
				if (step.getConditionTree() != null && !isPilot)
				{
					violations.add(source.getName() + " step " + (i + 1)
						+ " has non-null conditionTree but is not in PHASE_3_PILOT_SOURCES; "
						+ "add the source name to the allowlist and ship a structural test alongside the JSON edit");
				}
			}
		}

		assertTrue(violations.isEmpty(),
			"B1 conditionTree opt-in invariant violated. Only allowlisted Phase 3 pilots may set conditionTree. Violations:\n"
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
