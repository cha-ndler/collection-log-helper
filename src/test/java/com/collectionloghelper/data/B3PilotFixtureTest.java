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
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * B3 end-to-end pilot fixture test. Loads {@code b3_pilot_source.json} (a synthetic
 * source that uses nested alternatives), deserialises it through Gson, and verifies
 * that the nested structure is intact and that {@link GuidanceStep#resolveAlternative}
 * correctly applies overrides at each level.
 */
@RunWith(MockitoJUnitRunner.class)
public class B3PilotFixtureTest
{
	@Mock
	private RequirementsChecker checker;

	// ── Fixture load and structural integrity ─────────────────────────────────

	@Test
	public void fixture_loadsAndDeserialises()
	{
		List<CollectionLogSource> sources = loadFixture();
		assertNotNull(sources);
		assertEquals(1, sources.size());

		CollectionLogSource source = sources.get(0);
		assertEquals("B3 Pilot Source", source.getName());

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull(steps);
		assertEquals(2, steps.size());
	}

	@Test
	public void fixture_firstStepHasConditionalAlternative()
	{
		GuidanceStep step = loadFirstStep();
		assertEquals("Default: walk there manually", step.getDescription());

		List<ConditionalAlternative> alts = step.getConditionalAlternatives();
		assertNotNull(alts);
		assertEquals(1, alts.size());
	}

	@Test
	public void fixture_level1AlternativeHasNestedAlternatives()
	{
		ConditionalAlternative level1 = loadFirstStep().getConditionalAlternatives().get(0);
		assertEquals("Level 1: use fairy ring", level1.getDescription());
		assertEquals(Integer.valueOf(2400), level1.getWorldX());
		assertEquals(Integer.valueOf(4435), level1.getWorldY());

		assertNotNull(level1.getNestedAlternatives());
		assertEquals(1, level1.getNestedAlternatives().size());
	}

	@Test
	public void fixture_level2AlternativeHasNestedAlternatives()
	{
		ConditionalAlternative level1 = loadFirstStep().getConditionalAlternatives().get(0);
		ConditionalAlternative level2 = level1.getNestedAlternatives().get(0);

		assertEquals("Level 2: agility shortcut near fairy ring", level2.getDescription());
		assertEquals(Integer.valueOf(2405), level2.getWorldX());

		assertNotNull(level2.getNestedAlternatives());
		assertEquals(1, level2.getNestedAlternatives().size());
	}

	@Test
	public void fixture_level3AlternativeHasNoNestedAlternatives()
	{
		ConditionalAlternative level1 = loadFirstStep().getConditionalAlternatives().get(0);
		ConditionalAlternative level2 = level1.getNestedAlternatives().get(0);
		ConditionalAlternative level3 = level2.getNestedAlternatives().get(0);

		assertEquals("80 Agility shortcut saves 2 extra tiles", level3.getTravelTip());
		assertNull("Level 3 should have no further nesting", level3.getNestedAlternatives());
	}

	// ── resolveAlternative integration: all levels match ─────────────────────

	@Test
	public void resolveAlternative_allLevelsMatch_deepestOverrideWins()
	{
		GuidanceStep step = loadFirstStep();
		ConditionalAlternative level1 = step.getConditionalAlternatives().get(0);
		ConditionalAlternative level2 = level1.getNestedAlternatives().get(0);
		ConditionalAlternative level3 = level2.getNestedAlternatives().get(0);

		when(checker.meetsRequirements(level1.getRequirements())).thenReturn(true);
		when(checker.meetsRequirements(level2.getRequirements())).thenReturn(true);
		when(checker.meetsRequirements(level3.getRequirements())).thenReturn(true);

		GuidanceStep resolved = step.resolveAlternative(checker);

		// Level 2 overrides description (level 3 does not override description)
		assertEquals("Level 2: agility shortcut near fairy ring", resolved.getDescription());
		// Level 2 overrides worldX to 2405
		assertEquals(2405, resolved.getWorldX());
		// Level 1 set worldY=4435; level 2/3 don't override it
		assertEquals(4435, resolved.getWorldY());
		// Level 3 overrides travelTip
		assertEquals("80 Agility shortcut saves 2 extra tiles", resolved.getTravelTip());
		// Resolved step should not carry alternatives (already flattened)
		assertNull(resolved.getConditionalAlternatives());
	}

	// ── resolveAlternative integration: only level 1 matches ─────────────────

	@Test
	public void resolveAlternative_onlyLevel1Matches_level1OverrideOnly()
	{
		GuidanceStep step = loadFirstStep();
		ConditionalAlternative level1 = step.getConditionalAlternatives().get(0);
		ConditionalAlternative level2 = level1.getNestedAlternatives().get(0);

		when(checker.meetsRequirements(level1.getRequirements())).thenReturn(true);
		when(checker.meetsRequirements(level2.getRequirements())).thenReturn(false);

		GuidanceStep resolved = step.resolveAlternative(checker);

		assertEquals("Level 1: use fairy ring", resolved.getDescription());
		assertEquals(2400, resolved.getWorldX());
		assertEquals(4435, resolved.getWorldY());
		assertEquals("Use fairy ring BIP", resolved.getTravelTip());
	}

	// ── resolveAlternative integration: no match → base step ─────────────────

	@Test
	public void resolveAlternative_noMatch_returnsBaseStep()
	{
		GuidanceStep step = loadFirstStep();
		ConditionalAlternative level1 = step.getConditionalAlternatives().get(0);

		when(checker.meetsRequirements(level1.getRequirements())).thenReturn(false);

		GuidanceStep resolved = step.resolveAlternative(checker);

		assertEquals("Default: walk there manually", resolved.getDescription());
		assertEquals(3000, resolved.getWorldX());
		assertEquals(3000, resolved.getWorldY());
		assertEquals("Walk south", resolved.getTravelTip());
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private List<CollectionLogSource> loadFixture()
	{
		Gson gson = new GsonBuilder().create();
		InputStream is = getClass().getResourceAsStream(
			"/com/collectionloghelper/data/b3_pilot_source.json");
		assertNotNull("b3_pilot_source.json must be on the test classpath", is);

		Type listType = new TypeToken<List<CollectionLogSource>>()
		{
		}.getType();
		return gson.fromJson(new InputStreamReader(is), listType);
	}

	private GuidanceStep loadFirstStep()
	{
		return loadFixture().get(0).getGuidanceSteps().get(0);
	}
}
