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

import com.collectionloghelper.data.RequirementRow;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RequirementsView} (B.5.3 requirements section).
 *
 * <p>Covers:
 * <ul>
 *   <li>Construction — view starts hidden</li>
 *   <li>All-met: section is visible, all labels green</li>
 *   <li>One-unmet: section is visible, unmet label red, met label green</li>
 *   <li>No requirements: section is hidden (no empty header)</li>
 *   <li>State transition: label colour updates when rows change between calls</li>
 *   <li>hideRequirements: section hidden and rows cleared</li>
 *   <li>Diary row: correct label text rendered</li>
 * </ul>
 */
public class RequirementsViewTest
{
	private RequirementsView view;

	// Helpers
	private static RequirementRow metQuestRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.QUEST, label,
			"COMPLETED", RequirementRow.COLOR_MET, true);
	}

	private static RequirementRow inProgressQuestRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.QUEST, label,
			"IN PROGRESS", RequirementRow.COLOR_IN_PROGRESS, false);
	}

	private static RequirementRow unmetQuestRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.QUEST, label,
			"NOT STARTED", RequirementRow.COLOR_UNMET, false);
	}

	private static RequirementRow metSkillRow(String label, String state)
	{
		return new RequirementRow(RequirementRow.Category.SKILL, label,
			state, RequirementRow.COLOR_MET, true);
	}

	private static RequirementRow unmetSkillRow(String label, String state)
	{
		return new RequirementRow(RequirementRow.Category.SKILL, label,
			state, RequirementRow.COLOR_UNMET, false);
	}

	@Before
	public void setUp()
	{
		view = new RequirementsView();
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void initiallyHidden()
	{
		assertFalse("RequirementsView should start hidden", view.isVisible());
	}

	@Test
	public void updateRows_emptyList_hidden()
	{
		view.updateRows(Collections.emptyList());
		assertFalse("Empty rows: view must remain hidden", view.isVisible());
	}

	@Test
	public void updateRows_allMet_sectionVisible()
	{
		List<RequirementRow> rows = Arrays.asList(
			metQuestRow("Dragon Slayer II"),
			metSkillRow("Agility 70", "level 75/70")
		);
		view.updateRows(rows);
		assertTrue("All-met rows: view must be visible", view.isVisible());
	}

	@Test
	public void updateRows_allMet_labelsGreen()
	{
		List<RequirementRow> rows = Collections.singletonList(
			metQuestRow("Dragon Slayer II")
		);
		view.updateRows(rows);

		JLabel rowLabel = findFirstRowLabel(view);
		assertNotNull("Expected at least one row label", rowLabel);
		assertEquals("Met row must be green",
			RequirementRow.COLOR_MET, rowLabel.getForeground());
	}

	@Test
	public void updateRows_oneUnmet_sectionVisible()
	{
		List<RequirementRow> rows = Arrays.asList(
			metQuestRow("Dragon Slayer II"),
			unmetSkillRow("Agility 70", "level 60/70")
		);
		view.updateRows(rows);
		assertTrue("Mixed rows: view must be visible", view.isVisible());
	}

	@Test
	public void updateRows_oneUnmet_unmetLabelRed()
	{
		List<RequirementRow> rows = Collections.singletonList(
			unmetQuestRow("Dragon Slayer II")
		);
		view.updateRows(rows);

		JLabel rowLabel = findFirstRowLabel(view);
		assertNotNull("Expected at least one row label", rowLabel);
		assertEquals("Unmet row must be red",
			RequirementRow.COLOR_UNMET, rowLabel.getForeground());
	}

	@Test
	public void updateRows_inProgressQuest_yellow()
	{
		List<RequirementRow> rows = Collections.singletonList(
			inProgressQuestRow("Dragon Slayer II")
		);
		view.updateRows(rows);

		JLabel rowLabel = findFirstRowLabel(view);
		assertNotNull(rowLabel);
		assertEquals("In-progress quest must be yellow",
			RequirementRow.COLOR_IN_PROGRESS, rowLabel.getForeground());
	}

	@Test
	public void stateTransition_unmetBecomingMet_colourUpdates()
	{
		// First call: skill unmet
		view.updateRows(Collections.singletonList(
			unmetSkillRow("Agility 70", "level 60/70")
		));
		JLabel firstLabel = findFirstRowLabel(view);
		assertNotNull(firstLabel);
		assertEquals(RequirementRow.COLOR_UNMET, firstLabel.getForeground());

		// Second call: skill now met (simulates a level-up between refreshes)
		view.updateRows(Collections.singletonList(
			metSkillRow("Agility 70", "level 75/70")
		));
		JLabel secondLabel = findFirstRowLabel(view);
		assertNotNull(secondLabel);
		assertEquals("After level-up, row must switch to green",
			RequirementRow.COLOR_MET, secondLabel.getForeground());
	}

	@Test
	public void hideRequirements_hidesSection() throws Exception
	{
		view.updateRows(Collections.singletonList(metQuestRow("Dragon Slayer II")));
		assertTrue(view.isVisible());

		view.hideRequirements();
		flushEdt();
		assertFalse("hideRequirements must hide the section", view.isVisible());
	}

	@Test
	public void hideRequirements_clearsRows() throws Exception
	{
		view.updateRows(Collections.singletonList(metQuestRow("Dragon Slayer II")));
		view.hideRequirements();
		flushEdt();
		assertEquals("hideRequirements must remove all child components",
			0, view.getComponentCount());
	}

	@Test
	public void updateRows_diaryRow_renderedCorrectly()
	{
		RequirementRow diaryRow = new RequirementRow(
			RequirementRow.Category.DIARY,
			"Lumbridge Elite",
			"COMPLETED",
			RequirementRow.COLOR_MET,
			true
		);
		view.updateRows(Collections.singletonList(diaryRow));
		assertTrue(view.isVisible());

		JLabel rowLabel = findFirstRowLabel(view);
		assertNotNull(rowLabel);
		assertTrue("Diary label must contain the diary name",
			rowLabel.getText().contains("Lumbridge Elite"));
		assertEquals(RequirementRow.COLOR_MET, rowLabel.getForeground());
	}

	@Test
	public void updateRows_rowLabelContainsStateText()
	{
		RequirementRow row = metSkillRow("Agility 70", "level 75/70");
		view.updateRows(Collections.singletonList(row));

		JLabel rowLabel = findFirstRowLabel(view);
		assertNotNull(rowLabel);
		assertTrue("Row label must contain state text",
			rowLabel.getText().contains("level 75/70"));
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/** Flush the Swing event queue so invokeLater-scheduled work completes. */
	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
	}

	/**
	 * Finds the first non-header JLabel child of the view (the first requirement row label).
	 * Skips the bold "Requirements:" header by checking whether the font is bold.
	 */
	private static JLabel findFirstRowLabel(RequirementsView view)
	{
		for (int i = 0; i < view.getComponentCount(); i++)
		{
			Component c = view.getComponent(i);
			if (c instanceof JLabel)
			{
				JLabel label = (JLabel) c;
				// Skip the bold "Requirements:" header label
				if (label.getFont() != null && label.getFont().isBold())
				{
					continue;
				}
				return label;
			}
		}
		return null;
	}
}
