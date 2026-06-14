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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Global authoring-invariant guard over EVERY source in the production
 * {@code drop_rates.json}.
 *
 * <p>Unlike the name-keyed batch/category audits (e.g. {@code SoloBossesDeepGuidanceAuditTest},
 * which iterates a fixed {@code SOLO_BOSS_SOURCES} allowlist), this guard does not depend on a
 * hard-coded list of source names. It loads the real production database and walks
 * {@link DropRateDatabase#getAllSources()}, so a source that is not enumerated in any batch
 * audit still gets covered. A regression on any source — lost {@code guidanceSteps}, missing
 * primary coordinates, no kill/activity time, or dropping below two steps — fails the build.
 *
 * <p>The single exception to the two-step floor is an explicit allowlist of catch-all
 * pseudo-sources ({@code Miscellaneous}, {@code Random Events}) that legitimately carry a
 * single informational step because they are not real activities.
 */
public class SourceInvariantAuditTest
{
	/**
	 * Catch-all pseudo-sources that legitimately have a single step: these two are not real
	 * activities, so they carry one informational step rather than a full guidance sequence.
	 */
	private static final Set<String> SINGLE_STEP_ALLOWLIST = Set.of("Miscellaneous", "Random Events");

	/**
	 * Legal OSRS world-coordinate envelope (mirrors the toolkit's travel_path_verify bounds).
	 * A coordinate outside this range is a placeholder/garbage value, not a real tile. This
	 * guards against the Shellbane-class bug (PR #998) where positive-but-impossible coords
	 * (worldX=13993, worldY=9901) passed the non-positive check below yet were never reachable.
	 */
	private static final int MIN_WORLD_X = 900;
	private static final int MAX_WORLD_X = 4500;
	private static final int MIN_WORLD_Y = 1100;
	private static final int MAX_WORLD_Y = 13500;

	private DropRateDatabase database;

	@BeforeEach
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
	 * Asserts that every source in the production database meets the baseline authoring
	 * invariants. Violations are accumulated (not short-circuited on the first failure) so a
	 * single run reports every regression at once.
	 */
	@Test
	public void allSourcesMeetBaselineAuthoringInvariants()
	{
		List<String> violations = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			String name = source.getName();

			// 1. Name is non-null and non-blank.
			if (name == null || name.trim().isEmpty())
			{
				violations.add("source with null/blank name (category="
					+ source.getCategory() + ")");
				continue;
			}

			// 2. Guidance steps are non-null and non-empty.
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null || steps.isEmpty())
			{
				violations.add("'" + name + "' has null/empty guidanceSteps");
			}
			else
			{
				// 3. Step count >= 2, except allowlisted catch-all pseudo-sources (>= 1).
				int minSteps = SINGLE_STEP_ALLOWLIST.contains(name) ? 1 : 2;
				if (steps.size() < minSteps)
				{
					violations.add("'" + name + "' has " + steps.size()
						+ " guidance step(s), expected at least " + minSteps);
				}

				// 6. Every step has a non-null, non-blank description.
				for (int i = 0; i < steps.size(); i++)
				{
					GuidanceStep step = steps.get(i);
					String desc = step.getDescription();
					if (desc == null || desc.trim().isEmpty())
					{
						violations.add("'" + name + "' step index " + i
							+ " has null/blank description");
					}
				}
			}

			// 4. Every real source has a primary location.
			if (source.getWorldX() <= 0 || source.getWorldY() <= 0)
			{
				violations.add("'" + name + "' has non-positive primary coordinates (worldX="
					+ source.getWorldX() + ", worldY=" + source.getWorldY() + ")");
			}
			if (source.getWorldPlane() < 0)
			{
				violations.add("'" + name + "' has negative worldPlane ("
					+ source.getWorldPlane() + ")");
			}

			// 5. Every source has a kill/activity time in at least one field.
			if (source.getKillTimeSeconds() <= 0 && source.getIronKillTimeSeconds() <= 0)
			{
				violations.add("'" + name + "' has no kill time (killTimeSeconds="
					+ source.getKillTimeSeconds() + ", ironKillTimeSeconds="
					+ source.getIronKillTimeSeconds() + ")");
			}

			// 7. Coordinates that are set must fall within the legal OSRS world envelope.
			//    Source coords are always set (guarded as positive above); step coords are
			//    optional (0,0 means "no anchor" and is skipped, matching travel_path_verify).
			if (source.getWorldX() > 0 && source.getWorldY() > 0)
			{
				checkCoordBounds(violations, name, "primary location",
					source.getWorldX(), source.getWorldY());
			}
			if (steps != null)
			{
				for (int i = 0; i < steps.size(); i++)
				{
					GuidanceStep step = steps.get(i);
					if (step.getWorldX() != 0 && step.getWorldY() != 0)
					{
						checkCoordBounds(violations, name, "step index " + i,
							step.getWorldX(), step.getWorldY());
					}
				}
			}
		}

		assertTrue(
			violations.isEmpty(),
			"Source authoring-invariant violations (" + violations.size() + "):\n"
				+ String.join("\n", violations));
	}

	/**
	 * Records a violation if (x, y) falls outside the legal OSRS world envelope. Catches
	 * positive-but-impossible placeholder coordinates that the non-positive guard misses.
	 */
	private static void checkCoordBounds(List<String> violations, String name, String where, int x, int y)
	{
		if (x < MIN_WORLD_X || x > MAX_WORLD_X || y < MIN_WORLD_Y || y > MAX_WORLD_Y)
		{
			violations.add("'" + name + "' " + where + " coordinate out of legal world bounds (worldX="
				+ x + ", worldY=" + y + "); expected x in [" + MIN_WORLD_X + "," + MAX_WORLD_X
				+ "], y in [" + MIN_WORLD_Y + "," + MAX_WORLD_Y + "]");
		}
	}
}
