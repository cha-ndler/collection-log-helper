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
import com.collectionloghelper.guidance.RequiredItemDisplay;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import net.runelite.client.game.ItemManager;
import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link StepProgressView}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Construction and basic visibility contract (including issue #353 regression)</li>
 *   <li>Required-items subsection: hidden when empty, visible with rows</li>
 *   <li>All three availability states (HELD / IN_BANK / MISSING) reflected in name
 *       label colours and tooltip text</li>
 *   <li>State transition: colours update when rows change between showStep calls</li>
 * </ul>
 */
public class StepProgressViewTest
{
	private static final int TINDERBOX_ID = 590;
	private static final int PYRE_LOGS_ID = 3438;
	private static final int SHADE_REMAINS_ID = 3392;

	private ItemManager itemManager;
	private StepProgressView view;

	@Before
	public void setUp()
	{
		itemManager = Mockito.mock(ItemManager.class);
		// itemManager.getImage() is called for sprites; return null safely — the
		// test does not exercise icon loading.
		Mockito.when(itemManager.getImage(Mockito.anyInt())).thenReturn(null);
		view = new StepProgressView(itemManager);
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void showStep_withNoRequiredItems_doesNotThrow()
	{
		view.showStep(1, 5, "Travel to Lumbridge", false, Collections.emptyList());
	}

	@Test
	public void showStep_withNullRequiredItems_doesNotThrow()
	{
		view.showStep(2, 5, "Kill the boss", true, null);
	}

	@Test
	public void showStep_manualStep_doesNotThrow()
	{
		view.showStep(3, 5, "Open the chest", true, Collections.emptyList());
	}

	@Test
	public void hideStep_doesNotThrow()
	{
		view.hideStep();
	}

	@Test
	public void setCallbacks_doesNotThrow()
	{
		view.setCallbacks(() -> {}, () -> {});
	}

	@Test
	public void setCallbacks_nullCallbacks_doesNotThrow()
	{
		// snapshot-then-null-check: null callbacks should not throw on set
		view.setCallbacks(null, null);
	}

	@Test
	public void hideStep_afterShowStep_doesNotThrow()
	{
		view.showStep(1, 3, "Do the thing", false, Collections.emptyList());
		view.hideStep();
	}

	// ── Visibility regression tests for issue #353 ──────────────────────────
	//
	// The widget previously defined public void hide(), which shadowed the
	// deprecated-but-still-wired java.awt.Component#hide(). Component#setVisible
	// dispatches to hide()/show() internally, so the override silently turned
	// setVisible(false) into a no-op and left the Skip-button panel visible in
	// every state where guidance wasn't running. These tests pin the contract.

	@Test
	public void afterConstruction_isNotVisible()
	{
		assertFalse("StepProgressView must start hidden", view.isVisible());
	}

	@Test
	public void hideStep_afterShowStep_actuallyHides() throws Exception
	{
		view.showStep(1, 3, "Do the thing", false, Collections.emptyList());
		flushEdt();
		assertTrue("showStep should make widget visible", view.isVisible());

		view.hideStep();
		flushEdt();
		assertFalse("hideStep must make widget invisible", view.isVisible());
	}

	@Test
	public void showStep_makesVisible() throws Exception
	{
		view.showStep(2, 4, "Pick up reward", true, Collections.emptyList());
		flushEdt();
		assertTrue("showStep must make widget visible", view.isVisible());
	}

	// ── Required-items subsection tests (B.5.1) ─────────────────────────────

	/**
	 * When the step has no required items the subsection must be invisible so no
	 * empty "Items needed:" heading appears in the panel.
	 */
	@Test
	public void updateRequiredItemDisplay_emptyList_subsectionHidden() throws Exception
	{
		view.showStep(1, 1, "no items", false, Collections.emptyList());
		flushEdt();
		assertFalse("Required-items panel must be hidden when item list is empty",
			findRequiredItemsPanel(view).isVisible());
	}

	/**
	 * A HELD row must use the green colour constant and not append a bank tooltip.
	 */
	@Test
	public void updateRequiredItemDisplay_heldStatus_greenNameLabel() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));

		view.updateRequiredItemDisplay(rows);
		flushEdt();

		JLabel nameLabel = findFirstNameLabel(view);
		assertNotNull("Name label must be present for HELD row", nameLabel);
		assertEquals("HELD row must use green colour",
			RequiredItemDisplay.COLOR_HELD, nameLabel.getForeground());
		assertFalse("HELD tooltip must not mention bank",
			nameLabel.getToolTipText() != null
				&& nameLabel.getToolTipText().contains("in bank"));
	}

	/**
	 * An IN_BANK row must use white text and append the "ℹ in bank" suffix to the
	 * tooltip so the player knows where to fetch the item from.
	 */
	@Test
	public void updateRequiredItemDisplay_inBankStatus_whiteLabelWithBankTooltip() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.IN_BANK));

		view.updateRequiredItemDisplay(rows);
		flushEdt();

		JLabel nameLabel = findFirstNameLabel(view);
		assertNotNull("Name label must be present for IN_BANK row", nameLabel);
		assertEquals("IN_BANK row must use white colour", Color.WHITE, nameLabel.getForeground());
		assertTrue("IN_BANK tooltip must contain 'in bank' hint",
			nameLabel.getToolTipText() != null
				&& nameLabel.getToolTipText().contains("in bank"));
	}

	/**
	 * A MISSING row must use the red colour constant.
	 */
	@Test
	public void updateRequiredItemDisplay_missingStatus_redNameLabel() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(SHADE_REMAINS_ID, "Loar remains", Status.MISSING));

		view.updateRequiredItemDisplay(rows);
		flushEdt();

		JLabel nameLabel = findFirstNameLabel(view);
		assertNotNull("Name label must be present for MISSING row", nameLabel);
		assertEquals("MISSING row must use red colour",
			RequiredItemDisplay.COLOR_MISSING, nameLabel.getForeground());
	}

	/**
	 * All three statuses can appear in the same strip. Verifies ordering and that
	 * each status gets its correct colour independently.
	 */
	@Test
	public void updateRequiredItemDisplay_allThreeStatuses_correctColoursInOrder() throws Exception
	{
		List<RequiredItemDisplay> rows = Arrays.asList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.IN_BANK),
			new RequiredItemDisplay(SHADE_REMAINS_ID, "Loar remains", Status.MISSING));

		view.updateRequiredItemDisplay(rows);
		flushEdt();

		List<JLabel> nameLabels = findAllNameLabels(view);
		assertEquals("Must have exactly 3 name labels", 3, nameLabels.size());
		assertEquals(RequiredItemDisplay.COLOR_HELD, nameLabels.get(0).getForeground());
		assertEquals(Color.WHITE, nameLabels.get(1).getForeground());
		assertEquals(RequiredItemDisplay.COLOR_MISSING, nameLabels.get(2).getForeground());
	}

	/**
	 * State transition: colours must update when showStep is called a second time
	 * with different availability data for the same item.
	 */
	@Test
	public void showStep_stateTransition_coloursUpdateOnSecondCall() throws Exception
	{
		// First call: item is MISSING
		view.showStep(1, 1, "Collect item", false, Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.MISSING)));
		flushEdt();

		JLabel first = findFirstNameLabel(view);
		assertEquals("Initially MISSING must be red",
			RequiredItemDisplay.COLOR_MISSING, first.getForeground());

		// Second call: item is now HELD (player picked it up)
		view.showStep(1, 1, "Collect item", false, Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD)));
		flushEdt();

		JLabel updated = findFirstNameLabel(view);
		assertEquals("After transition to HELD must be green",
			RequiredItemDisplay.COLOR_HELD, updated.getForeground());
	}

	// ── Section rendering tests (B.5.4) ─────────────────────────────────────

	/**
	 * When the step list contains no section labels the sectionsPanel must stay hidden
	 * and the flat required-items panel is used instead.
	 */
	@Test
	public void showStep_withNoSectionData_sectionsPanelHidden() throws Exception
	{
		List<GuidanceStep> noSectionSteps = Arrays.asList(stepWithSection(null), stepWithSection(null));
		view.showStep(1, 2, "Travel", false, Collections.emptyList(), noSectionSteps);
		flushEdt();

		JPanel sectionsPanel = findSectionsPanel(view);
		assertNotNull("sectionsPanel must be present in widget", sectionsPanel);
		assertFalse("sectionsPanel must be hidden when no step has a section",
			sectionsPanel.isVisible());
	}

	/**
	 * When at least one step in allSteps has a section label, the sectionsPanel
	 * must become visible and contain at least one section header button.
	 */
	@Test
	public void showStep_withSectionData_sectionsPanelVisible() throws Exception
	{
		List<GuidanceStep> steps = Arrays.asList(
			stepWithSection("Travel"),
			stepWithSection("Combat"));
		view.showStep(1, 2, "Walk to boss", false, Collections.emptyList(), steps);
		flushEdt();

		JPanel sectionsPanel = findSectionsPanel(view);
		assertNotNull(sectionsPanel);
		assertTrue("sectionsPanel must be visible when steps have sections",
			sectionsPanel.isVisible());
		// At least two header buttons (one per section)
		List<JButton> buttons = findAllButtons(sectionsPanel);
		assertEquals("Must have one header button per section", 2, buttons.size());
	}

	/**
	 * The active step's section header must begin with the expanded arrow "▼".
	 * Other sections default to collapsed "▶".
	 */
	@Test
	public void showStep_activeSectionForceExpanded_showsDownArrow() throws Exception
	{
		// 3 steps: step 1 in "Travel", step 2 in "Combat", step 3 in "Travel"
		List<GuidanceStep> steps = Arrays.asList(
			stepWithSection("Travel"),
			stepWithSection("Combat"),
			stepWithSection("Travel"));
		// Active step = step 2 (in "Combat")
		view.showStep(2, 3, "Fight the boss", false, Collections.emptyList(), steps);
		flushEdt();

		JPanel sectionsPanel = findSectionsPanel(view);
		List<JButton> buttons = findAllButtons(sectionsPanel);
		assertEquals("Must have 3 header buttons (Travel, Combat, Travel)", 3, buttons.size());

		// Travel (step 1) — not active, should be collapsed
		assertTrue("Inactive section must start with collapse arrow",
			buttons.get(0).getText().startsWith("▶"));
		// Combat (step 2) — active, must be expanded
		assertTrue("Active section must start with expand arrow",
			buttons.get(1).getText().startsWith("▼"));
		// Travel (step 3) — not active, should be collapsed
		assertTrue("Inactive section must start with collapse arrow",
			buttons.get(2).getText().startsWith("▶"));
	}

	/**
	 * When switching to a different active step, the previously active section
	 * can remain expanded (user toggled it) but the new active section is
	 * force-expanded.
	 */
	@Test
	public void showStep_activeStepChanges_newSectionForceExpanded() throws Exception
	{
		List<GuidanceStep> steps = Arrays.asList(
			stepWithSection("Starting off"),
			stepWithSection("Travel"));

		// First render: step 1 active
		view.showStep(1, 2, "Start", false, Collections.emptyList(), steps);
		flushEdt();

		// Second render: step 2 becomes active
		view.showStep(2, 2, "Walk", false, Collections.emptyList(), steps);
		flushEdt();

		JPanel sectionsPanel = findSectionsPanel(view);
		List<JButton> buttons = findAllButtons(sectionsPanel);
		assertEquals(2, buttons.size());
		// "Travel" section (button index 1) must be force-expanded
		assertTrue("New active section must show expanded arrow after step change",
			buttons.get(1).getText().startsWith("▼"));
	}

	/**
	 * When showStep is called with sections, the inline required-items panel
	 * (flat layout) must be hidden — items are shown inside the active section body.
	 */
	@Test
	public void showStep_withSections_inlineRequiredItemsPanelHidden() throws Exception
	{
		List<GuidanceStep> steps = Collections.singletonList(stepWithSection("Travel"));
		List<RequiredItemDisplay> items = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));

		view.showStep(1, 1, "Go somewhere", false, items, steps);
		flushEdt();

		// The flat requiredItemsPanel (first JPanel child) must be hidden in sectioned mode
		JPanel reqPanel = findRequiredItemsPanel(view);
		assertNotNull(reqPanel);
		assertFalse("Inline required-items panel must be hidden in sectioned mode",
			reqPanel.isVisible());
	}

	/**
	 * hideStep must also clear the sections panel.
	 */
	@Test
	public void hideStep_afterSectionedShow_sectionsPanelHidden() throws Exception
	{
		List<GuidanceStep> steps = Collections.singletonList(stepWithSection("Combat"));
		view.showStep(1, 1, "Fight", false, Collections.emptyList(), steps);
		flushEdt();
		assertTrue("sectionsPanel must be visible before hide", findSectionsPanel(view).isVisible());

		view.hideStep();
		flushEdt();
		assertFalse("sectionsPanel must be hidden after hideStep", findSectionsPanel(view).isVisible());
	}

	// ── Recommended-items subsection tests (B.5.2) ──────────────────────────

	/**
	 * When required items are empty but recommended items are populated, the
	 * "Recommended:" subsection must be visible and the "Items needed:" panel hidden.
	 */
	@Test
	public void updateRecommendedItemDisplay_withRows_subsectionVisible() throws Exception
	{
		List<RequiredItemDisplay> recRows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));

		view.showStep(1, 1, "Boss fight", false,
			Collections.<RequiredItemDisplay>emptyList(),
			recRows,
			Collections.<GuidanceStep>emptyList());
		flushEdt();

		JPanel recPanel = findRecommendedItemsPanel(view);
		assertNotNull("recommendedItemsPanel must be present in widget", recPanel);
		assertTrue("recommendedItemsPanel must be visible when rows are non-empty",
			recPanel.isVisible());
		// Required panel must be hidden since we passed empty
		JPanel reqPanel = findRequiredItemsPanel(view);
		assertFalse("Required items panel must be hidden when required list is empty",
			reqPanel.isVisible());
	}

	/**
	 * When recommended items list is empty the subsection must remain hidden.
	 */
	@Test
	public void updateRecommendedItemDisplay_emptyList_subsectionHidden() throws Exception
	{
		view.showStep(1, 1, "No rec items", false,
			Collections.<RequiredItemDisplay>emptyList(),
			Collections.<RequiredItemDisplay>emptyList(),
			Collections.<GuidanceStep>emptyList());
		flushEdt();

		JPanel recPanel = findRecommendedItemsPanel(view);
		assertFalse("recommendedItemsPanel must be hidden when recommended list is empty",
			recPanel.isVisible());
	}

	/**
	 * Both sections must be visible when both required and recommended lists are populated.
	 */
	@Test
	public void showStep_bothRequiredAndRecommended_bothSectionsVisible() throws Exception
	{
		List<RequiredItemDisplay> req = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));
		List<RequiredItemDisplay> rec = Collections.singletonList(
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.MISSING));

		view.showStep(1, 1, "Do thing", false, req, rec, Collections.<GuidanceStep>emptyList());
		flushEdt();

		assertTrue("Items needed panel must be visible when required list populated",
			findRequiredItemsPanel(view).isVisible());
		assertTrue("Recommended panel must be visible when recommended list populated",
			findRecommendedItemsPanel(view).isVisible());
	}

	/**
	 * Recommended rows use the same colour semantics as required rows (HELD=green,
	 * IN_BANK=white+tooltip, MISSING=red). Parameterised across all three statuses.
	 */
	@Test
	public void updateRecommendedItemDisplay_heldStatus_greenLabel() throws Exception
	{
		view.updateRecommendedItemDisplay(Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD)));
		flushEdt();

		JLabel label = findFirstRecommendedNameLabel(view);
		assertNotNull(label);
		assertEquals("HELD recommended row must use green colour",
			RequiredItemDisplay.COLOR_HELD, label.getForeground());
	}

	@Test
	public void updateRecommendedItemDisplay_inBankStatus_whiteLabelWithTooltip() throws Exception
	{
		view.updateRecommendedItemDisplay(Collections.singletonList(
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.IN_BANK)));
		flushEdt();

		JLabel label = findFirstRecommendedNameLabel(view);
		assertNotNull(label);
		assertEquals("IN_BANK recommended row must use white colour",
			Color.WHITE, label.getForeground());
		assertTrue("IN_BANK recommended tooltip must contain 'in bank'",
			label.getToolTipText() != null && label.getToolTipText().contains("in bank"));
	}

	@Test
	public void updateRecommendedItemDisplay_missingStatus_redLabel() throws Exception
	{
		view.updateRecommendedItemDisplay(Collections.singletonList(
			new RequiredItemDisplay(SHADE_REMAINS_ID, "Loar remains", Status.MISSING)));
		flushEdt();

		JLabel label = findFirstRecommendedNameLabel(view);
		assertNotNull(label);
		assertEquals("MISSING recommended row must use red colour",
			RequiredItemDisplay.COLOR_MISSING, label.getForeground());
	}

	/**
	 * hideStep must clear both required and recommended panels.
	 */
	@Test
	public void hideStep_clearsBothItemPanels() throws Exception
	{
		List<RequiredItemDisplay> req = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));
		List<RequiredItemDisplay> rec = Collections.singletonList(
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.MISSING));

		view.showStep(1, 1, "Do thing", false, req, rec, Collections.<GuidanceStep>emptyList());
		flushEdt();

		view.hideStep();
		flushEdt();

		assertFalse("Required panel must be hidden after hideStep",
			findRequiredItemsPanel(view).isVisible());
		assertFalse("Recommended panel must be hidden after hideStep",
			findRecommendedItemsPanel(view).isVisible());
	}

	/**
	 * Schema-level: a step constructed with a non-null recommendedItemIds list
	 * exposes the list correctly via getRecommendedItemIds().
	 */
	@Test
	public void guidanceStep_recommendedItemIds_accessible()
	{
		List<Integer> recIds = Arrays.asList(TINDERBOX_ID, PYRE_LOGS_ID);
		GuidanceStep step = stepWithRecommendedItems(recIds);
		assertEquals("recommendedItemIds must be accessible via getter",
			recIds, step.getRecommendedItemIds());
	}

	/**
	 * Schema-level: a step constructed without recommendedItemIds (null) has
	 * a null getter return rather than an empty list.
	 */
	@Test
	public void guidanceStep_recommendedItemIds_nullWhenAbsent()
	{
		GuidanceStep step = stepWithSection(null); // no recommendedItemIds
		assertFalse("recommendedItemIds must be null when not set",
			step.getRecommendedItemIds() != null);
	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	/** Flush the Swing event queue so invokeLater-scheduled work completes. */
	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
	}

	/**
	 * Finds the required-items sub-panel inside {@code view}.
	 * The layout order is: chipPanel (1st JPanel), requiredItemsPanel (2nd),
	 * recommendedItemsPanel (3rd), sectionsPanel (4th).
	 * This helper returns the 2nd JPanel (requiredItemsPanel).
	 */
	private static JPanel findRequiredItemsPanel(StepProgressView view)
	{
		int panelCount = 0;
		for (Component c : view.getComponents())
		{
			if (c instanceof JPanel)
			{
				panelCount++;
				if (panelCount == 3)
				{
					return (JPanel) c;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the recommended-items sub-panel inside {@code view}. It is the third
	 * {@link JPanel} direct child of the StepProgressView (chip panel, required,
	 * then recommended).
	 */
	private static JPanel findRecommendedItemsPanel(StepProgressView view)
	{
		int panelCount = 0;
		for (Component c : view.getComponents())
		{
			if (c instanceof JPanel)
			{
				panelCount++;
				if (panelCount == 4)
				{
					return (JPanel) c;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the first item-name label from the recommended-items sub-panel.
	 */
	private static JLabel findFirstRecommendedNameLabel(StepProgressView view)
	{
		JPanel recPanel = findRecommendedItemsPanel(view);
		if (recPanel == null)
		{
			return null;
		}
		for (Component c : recPanel.getComponents())
		{
			if (!(c instanceof JPanel))
			{
				continue;
			}
			JPanel rowPanel = (JPanel) c;
			for (Component child : rowPanel.getComponents())
			{
				if (child instanceof JLabel)
				{
					JLabel label = (JLabel) child;
					if (label.getText() != null && !label.getText().isEmpty())
					{
						return label;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the first item-name {@link JLabel} found inside the required-items
	 * sub-panel (skips the header label and icon-only labels).
	 */
	private static JLabel findFirstNameLabel(StepProgressView view)
	{
		List<JLabel> labels = findAllNameLabels(view);
		return labels.isEmpty() ? null : labels.get(0);
	}

	/**
	 * Collects all item-name labels from the required-items strip (one per row).
	 * The header "Items needed:" label and icon-only labels are excluded.
	 */
	private static List<JLabel> findAllNameLabels(StepProgressView view)
	{
		List<JLabel> result = new java.util.ArrayList<>();
		JPanel reqPanel = findRequiredItemsPanel(view);
		if (reqPanel == null)
		{
			return result;
		}
		for (Component c : reqPanel.getComponents())
		{
			if (!(c instanceof JPanel))
			{
				continue;
			}
			JPanel rowPanel = (JPanel) c;
			for (Component child : rowPanel.getComponents())
			{
				if (child instanceof JLabel)
				{
					JLabel label = (JLabel) child;
					// Skip icon labels (no text) and pick name labels that have text
					if (label.getText() != null && !label.getText().isEmpty())
					{
						result.add(label);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Finds the sectionsPanel — the fifth {@link JPanel} direct child of the
	 * StepProgressView (chipPanel, recChipPanel, requiredItemsPanel,
	 * recommendedItemsPanel, then sectionsPanel).
	 */
	private static JPanel findSectionsPanel(StepProgressView view)
	{
		int panelCount = 0;
		for (Component c : view.getComponents())
		{
			if (c instanceof JPanel)
			{
				panelCount++;
				if (panelCount == 5)
				{
					return (JPanel) c;
				}
			}
		}
		return null;
	}

	/**
	 * Collects all {@link JButton} components found directly inside {@code panel}
	 * or inside any direct JPanel child (section block container).
	 */
	private static List<JButton> findAllButtons(JPanel panel)
	{
		List<JButton> result = new java.util.ArrayList<>();
		if (panel == null)
		{
			return result;
		}
		for (Component c : panel.getComponents())
		{
			if (c instanceof JButton)
			{
				result.add((JButton) c);
			}
			else if (c instanceof JPanel)
			{
				// Section block: first child is the header JButton
				for (Component child : ((JPanel) c).getComponents())
				{
					if (child instanceof JButton)
					{
						result.add((JButton) child);
					}
				}
			}
		}
		return result;
	}

	// ── Reset / Sync button tests (#369) ────────────────────────────────────

	/**
	 * After construction the Reset and Sync buttons must be present in the
	 * step-button row (findable via findButtonByText).
	 */
	@Test
	public void construction_hasResetAndSyncButtons()
	{
		assertNotNull("Reset button must exist after construction", findButtonByText(view, "Reset"));
		assertNotNull("Sync button must exist after construction", findButtonByText(view, "Sync"));
	}

	/**
	 * The Reset button must invoke the reset callback when clicked.
	 */
	@Test
	public void resetButton_click_invokesResetCallback() throws Exception
	{
		AtomicBoolean resetFired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> resetFired.set(true), () -> {});
		flushEdt();

		JButton resetBtn = findButtonByText(view, "Reset");
		assertNotNull("Reset button must be present", resetBtn);
		SwingUtilities.invokeAndWait(() -> resetBtn.doClick());

		assertTrue("Reset callback must fire when Reset button is clicked", resetFired.get());
	}

	/**
	 * The Sync button must invoke the sync callback when clicked.
	 */
	@Test
	public void syncButton_click_invokesSyncCallback() throws Exception
	{
		AtomicBoolean syncFired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> {}, () -> syncFired.set(true));
		flushEdt();

		JButton syncBtn = findButtonByText(view, "Sync");
		assertNotNull("Sync button must be present", syncBtn);
		SwingUtilities.invokeAndWait(() -> syncBtn.doClick());

		assertTrue("Sync callback must fire when Sync button is clicked", syncFired.get());
	}

	/**
	 * setCallbacks(advancer, skipper) two-arg overload must remain valid after the
	 * new four-arg overload is introduced — existing callers are not broken.
	 */
	@Test
	public void setCallbacks_twoArg_doesNotThrow()
	{
		view.setCallbacks(() -> {}, () -> {});
	}

	/**
	 * When setCallbacks is called with null reset/sync callbacks, clicking the buttons
	 * must not throw a NullPointerException.
	 */
	@Test
	public void resetAndSyncButtons_nullCallbacks_doNotThrow() throws Exception
	{
		view.setCallbacks(() -> {}, () -> {}, null, null);
		flushEdt();

		JButton resetBtn = findButtonByText(view, "Reset");
		JButton syncBtn = findButtonByText(view, "Sync");
		assertNotNull(resetBtn);
		assertNotNull(syncBtn);
		SwingUtilities.invokeAndWait(() ->
		{
			resetBtn.doClick();
			syncBtn.doClick();
		});
	}

	/**
	 * The Reset button has the tooltip "Restart from step 1".
	 */
	@Test
	public void resetButton_hasExpectedTooltip()
	{
		JButton resetBtn = findButtonByText(view, "Reset");
		assertNotNull(resetBtn);
		assertEquals("Restart from step 1", resetBtn.getToolTipText());
	}

	/**
	 * The Sync button has the tooltip "Re-evaluate current state".
	 */
	@Test
	public void syncButton_hasExpectedTooltip()
	{
		JButton syncBtn = findButtonByText(view, "Sync");
		assertNotNull(syncBtn);
		assertEquals("Re-evaluate current state", syncBtn.getToolTipText());
	}

	// ── Helpers (Reset/Sync) ─────────────────────────────────────────────────

	/**
	 * Finds the first {@link JButton} anywhere in {@code root}'s component tree
	 * whose text equals {@code text}. Searches recursively.
	 */
	private static JButton findButtonByText(JPanel root, String text)
	{
		for (Component c : root.getComponents())
		{
			if (c instanceof JButton)
			{
				JButton btn = (JButton) c;
				if (text.equals(btn.getText()))
				{
					return btn;
				}
			}
			else if (c instanceof JPanel)
			{
				JButton found = findButtonInPanel((JPanel) c, text);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static JButton findButtonInPanel(JPanel panel, String text)
	{
		for (Component c : panel.getComponents())
		{
			if (c instanceof JButton)
			{
				JButton btn = (JButton) c;
				if (text.equals(btn.getText()))
				{
					return btn;
				}
			}
			else if (c instanceof JPanel)
			{
				JButton found = findButtonInPanel((JPanel) c, text);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	/** Builds a minimal {@link GuidanceStep} with the given section label (may be null). */
	private static GuidanceStep stepWithSection(String section)
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
			null  // waypoints
		);
	}

	/** Builds a minimal {@link GuidanceStep} with the given recommendedItemIds. */
	private static GuidanceStep stepWithRecommendedItems(List<Integer> recommendedItemIds)
	{
		return new GuidanceStep(
			"Step",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			recommendedItemIds,
			null,  // perItemRecommendedItemIds
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
			null,
			null  // waypoints
		);
	}
}
