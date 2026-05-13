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

import com.collectionloghelper.data.GuidanceStep;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the ordered list of {@link StepSectionGroup}s from a flat step list.
 *
 * <h3>Grouping rules</h3>
 * <ul>
 *   <li>A step's section label is obtained via {@link GuidanceStep#getSection()}.</li>
 *   <li>If <em>no</em> step in the list has a non-null section, this method returns an
 *       empty list — the caller should use the flat layout instead.</li>
 *   <li>Consecutive steps with the same section label (including consecutive null-section
 *       steps) are merged into one {@link StepSectionGroup}.</li>
 *   <li>Steps with a null section use the sentinel name
 *       {@link StepSectionGroup#OTHER_SECTION_NAME}.</li>
 * </ul>
 *
 * <p>Step indices in the returned groups are 1-based to match the "Step N/total" display
 * convention used throughout the panel.
 */
public final class StepSectionGrouper
{
	private StepSectionGrouper()
	{
	}

	/**
	 * Converts a flat list of guidance steps into an ordered list of section groups.
	 *
	 * @param steps the full ordered list of steps for the active source; must not be null
	 * @return ordered groups, or an empty list when no step carries a section label
	 */
	public static List<StepSectionGroup> group(List<GuidanceStep> steps)
	{
		if (steps == null || steps.isEmpty())
		{
			return Collections.emptyList();
		}

		boolean anySectionSet = false;
		for (GuidanceStep step : steps)
		{
			if (step.getSection() != null)
			{
				anySectionSet = true;
				break;
			}
		}

		if (!anySectionSet)
		{
			return Collections.emptyList();
		}

		List<StepSectionGroup> groups = new ArrayList<>();
		String currentLabel = null;
		List<Integer> currentIndices = new ArrayList<>();

		for (int i = 0; i < steps.size(); i++)
		{
			String label = steps.get(i).getSection();
			String effectiveLabel = label != null ? label : StepSectionGroup.OTHER_SECTION_NAME;

			if (currentLabel == null)
			{
				// First step: start the first group
				currentLabel = effectiveLabel;
				currentIndices.add(i + 1); // 1-based
			}
			else if (effectiveLabel.equals(currentLabel))
			{
				// Same section as the running group — extend it
				currentIndices.add(i + 1);
			}
			else
			{
				// Section changed — close the current group and start a new one
				groups.add(new StepSectionGroup(currentLabel, Collections.unmodifiableList(currentIndices)));
				currentLabel = effectiveLabel;
				currentIndices = new ArrayList<>();
				currentIndices.add(i + 1);
			}
		}

		// Close the final open group
		if (currentLabel != null && !currentIndices.isEmpty())
		{
			groups.add(new StepSectionGroup(currentLabel, Collections.unmodifiableList(currentIndices)));
		}

		return Collections.unmodifiableList(groups);
	}

	/**
	 * Returns the name of the section that contains {@code oneBasedStepIndex}, or
	 * {@code null} if the step index is not found in any group.
	 *
	 * @param groups        the ordered group list from {@link #group}
	 * @param oneBasedIndex 1-based step index
	 */
	public static String sectionNameFor(List<StepSectionGroup> groups, int oneBasedIndex)
	{
		for (StepSectionGroup group : groups)
		{
			if (group.getStepIndices().contains(oneBasedIndex))
			{
				return group.getName();
			}
		}
		return null;
	}
}
