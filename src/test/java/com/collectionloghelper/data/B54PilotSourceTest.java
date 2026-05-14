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

import com.collectionloghelper.ui.widget.StepSectionGroup;
import com.collectionloghelper.ui.widget.StepSectionGrouper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * End-to-end grouping test using the B.5.4 pilot fixture
 * ({@code b54_pilot_source.json}).
 *
 * <p>The fixture defines a single source with 8 steps across 4 sections:
 * <ul>
 *   <li>Steps 1–2: "Bank prep"</li>
 *   <li>Steps 3–5: "Travel"</li>
 *   <li>Steps 6–7: "Combat"</li>
 *   <li>Step  8:   "Loot"</li>
 * </ul>
 * Tests verify that {@link StepSectionGrouper#group} produces the correct 4 groups
 * with the correct step indices.
 */
public class B54PilotSourceTest
{
	private List<GuidanceStep> steps;

	@Before
	public void setUp() throws Exception
	{
		Gson gson = new GsonBuilder().create();
		Type listType = new TypeToken<List<CollectionLogSource>>() {}.getType();

		InputStream is = getClass().getResourceAsStream("/com/collectionloghelper/b54_pilot_source.json");
		assertNotNull("Pilot fixture must be present on classpath", is);

		List<CollectionLogSource> sources = gson.fromJson(new InputStreamReader(is), listType);
		assertEquals("Fixture must contain exactly one source", 1, sources.size());

		steps = sources.get(0).getGuidanceSteps();
		assertNotNull("Pilot source must have guidance steps", steps);
		assertEquals("Pilot source must have exactly 8 guidance steps", 8, steps.size());
	}

	@Test
	public void pilotFixture_allStepsDeserialiseWithCorrectSections()
	{
		assertEquals("Bank prep", steps.get(0).getSection());
		assertEquals("Bank prep", steps.get(1).getSection());
		assertEquals("Travel",    steps.get(2).getSection());
		assertEquals("Travel",    steps.get(3).getSection());
		assertEquals("Travel",    steps.get(4).getSection());
		assertEquals("Combat",    steps.get(5).getSection());
		assertEquals("Combat",    steps.get(6).getSection());
		assertEquals("Loot",      steps.get(7).getSection());
	}

	@Test
	public void pilotFixture_grouper_producesFourGroups()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals("8 steps across 4 sections must produce 4 groups", 4, groups.size());
	}

	@Test
	public void pilotFixture_grouper_bankPrepGroup()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		StepSectionGroup bankPrep = groups.get(0);
		assertEquals("Bank prep", bankPrep.getName());
		assertEquals(Arrays.asList(1, 2), bankPrep.getStepIndices());
	}

	@Test
	public void pilotFixture_grouper_travelGroup()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		StepSectionGroup travel = groups.get(1);
		assertEquals("Travel", travel.getName());
		assertEquals(Arrays.asList(3, 4, 5), travel.getStepIndices());
	}

	@Test
	public void pilotFixture_grouper_combatGroup()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		StepSectionGroup combat = groups.get(2);
		assertEquals("Combat", combat.getName());
		assertEquals(Arrays.asList(6, 7), combat.getStepIndices());
	}

	@Test
	public void pilotFixture_grouper_lootGroup()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		StepSectionGroup loot = groups.get(3);
		assertEquals("Loot", loot.getName());
		assertEquals(Arrays.asList(8), loot.getStepIndices());
	}

	@Test
	public void pilotFixture_sectionNameFor_returnsCorrectSection()
	{
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);

		assertEquals("Bank prep", StepSectionGrouper.sectionNameFor(groups, 1));
		assertEquals("Bank prep", StepSectionGrouper.sectionNameFor(groups, 2));
		assertEquals("Travel",    StepSectionGrouper.sectionNameFor(groups, 3));
		assertEquals("Travel",    StepSectionGrouper.sectionNameFor(groups, 4));
		assertEquals("Travel",    StepSectionGrouper.sectionNameFor(groups, 5));
		assertEquals("Combat",    StepSectionGrouper.sectionNameFor(groups, 6));
		assertEquals("Combat",    StepSectionGrouper.sectionNameFor(groups, 7));
		assertEquals("Loot",      StepSectionGrouper.sectionNameFor(groups, 8));
	}
}
