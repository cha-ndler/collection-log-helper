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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link StepSectionGrouper}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Empty/null inputs return empty groups</li>
 *   <li>All-null sections returns empty groups (flat layout)</li>
 *   <li>All-same section collapses into one group</li>
 *   <li>Consecutive same-section steps merge; switch starts new group</li>
 *   <li>Null-section steps become "Other" when sectioned mode is active</li>
 *   <li>Consecutive null-section runs merge into one "Other" block</li>
 *   <li>{@link StepSectionGrouper#sectionNameFor} returns correct section or null</li>
 * </ul>
 */
public class StepSectionGrouperTest
{
	/** Builds a minimal GuidanceStep with the given section label (may be null). */
	private static GuidanceStep step(String section)
	{
		return new GuidanceStep(
			"Step",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			null,  // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null, null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0, null, null,
			0, 0,
			null, null, null, null,
			section,
			null, // waypoints
			null  // dynamicTargetEvaluator
		);
	}

	// ── Empty / no-section inputs ────────────────────────────────────────────

	@Test
	public void group_nullList_returnsEmpty()
	{
		assertTrue(StepSectionGrouper.group(null).isEmpty());
	}

	@Test
	public void group_emptyList_returnsEmpty()
	{
		assertTrue(StepSectionGrouper.group(Collections.emptyList()).isEmpty());
	}

	@Test
	public void group_allNullSections_returnsEmptyForFlatLayout()
	{
		List<GuidanceStep> steps = Arrays.asList(step(null), step(null), step(null));
		assertTrue("No sections set — must return empty list for flat layout",
			StepSectionGrouper.group(steps).isEmpty());
	}

	// ── Single section ───────────────────────────────────────────────────────

	@Test
	public void group_allSameSectionLabel_oneGroup()
	{
		List<GuidanceStep> steps = Arrays.asList(
			step("Travel"), step("Travel"), step("Travel"));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals(1, groups.size());
		assertEquals("Travel", groups.get(0).getName());
		assertEquals(Arrays.asList(1, 2, 3), groups.get(0).getStepIndices());
	}

	// ── Consecutive same-section steps merge; switch starts new group ────────

	@Test
	public void group_twoDistinctSections_twoGroups()
	{
		List<GuidanceStep> steps = Arrays.asList(
			step("Starting off"),
			step("Starting off"),
			step("Combat"),
			step("Combat"));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals(2, groups.size());

		assertEquals("Starting off", groups.get(0).getName());
		assertEquals(Arrays.asList(1, 2), groups.get(0).getStepIndices());

		assertEquals("Combat", groups.get(1).getName());
		assertEquals(Arrays.asList(3, 4), groups.get(1).getStepIndices());
	}

	@Test
	public void group_alternatingLabels_noMergeAcrossInterleave()
	{
		// A, B, A should produce three groups (A is not consecutive the second time)
		List<GuidanceStep> steps = Arrays.asList(
			step("Travel"),
			step("Combat"),
			step("Travel"));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals(3, groups.size());
		assertEquals("Travel", groups.get(0).getName());
		assertEquals("Combat", groups.get(1).getName());
		assertEquals("Travel", groups.get(2).getName());
	}

	// ── Null-section steps become "Other" ────────────────────────────────────

	@Test
	public void group_nullSectionAmongSectioned_becomesOther()
	{
		// Steps: null, "Travel", null
		// Because "Travel" is set, sectioned mode activates; null becomes "Other"
		List<GuidanceStep> steps = Arrays.asList(
			step(null),
			step("Travel"),
			step(null));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals(3, groups.size());
		assertEquals(StepSectionGroup.OTHER_SECTION_NAME, groups.get(0).getName());
		assertEquals("Travel", groups.get(1).getName());
		assertEquals(StepSectionGroup.OTHER_SECTION_NAME, groups.get(2).getName());
	}

	@Test
	public void group_consecutiveNullSections_mergedIntoOneOtherGroup()
	{
		List<GuidanceStep> steps = Arrays.asList(
			step(null),
			step(null),
			step("Loot"));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		assertEquals(2, groups.size());
		assertEquals(StepSectionGroup.OTHER_SECTION_NAME, groups.get(0).getName());
		assertEquals(Arrays.asList(1, 2), groups.get(0).getStepIndices());
		assertEquals("Loot", groups.get(1).getName());
	}

	// ── 1-based step indices ─────────────────────────────────────────────────

	@Test
	public void group_stepIndicesAreOneBased()
	{
		List<GuidanceStep> steps = Arrays.asList(
			step("A"),
			step("B"),
			step("A"));

		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);
		// A at position 0 → index 1; B at position 1 → index 2; A at position 2 → index 3
		assertEquals(Arrays.asList(1), groups.get(0).getStepIndices());
		assertEquals(Arrays.asList(2), groups.get(1).getStepIndices());
		assertEquals(Arrays.asList(3), groups.get(2).getStepIndices());
	}

	// ── sectionNameFor ───────────────────────────────────────────────────────

	@Test
	public void sectionNameFor_knownIndex_returnsCorrectSection()
	{
		List<GuidanceStep> steps = Arrays.asList(step("Alpha"), step("Alpha"), step("Beta"));
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);

		assertEquals("Alpha", StepSectionGrouper.sectionNameFor(groups, 1));
		assertEquals("Alpha", StepSectionGrouper.sectionNameFor(groups, 2));
		assertEquals("Beta",  StepSectionGrouper.sectionNameFor(groups, 3));
	}

	@Test
	public void sectionNameFor_unknownIndex_returnsNull()
	{
		List<GuidanceStep> steps = Collections.singletonList(step("Alpha"));
		List<StepSectionGroup> groups = StepSectionGrouper.group(steps);

		assertNull(StepSectionGrouper.sectionNameFor(groups, 99));
	}

	@Test
	public void sectionNameFor_emptyGroups_returnsNull()
	{
		assertNull(StepSectionGrouper.sectionNameFor(Collections.emptyList(), 1));
	}
}
