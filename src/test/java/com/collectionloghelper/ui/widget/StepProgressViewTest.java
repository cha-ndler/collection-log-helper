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
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@BeforeEach
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
		assertFalse( view.isVisible(),"StepProgressView must start hidden");
	}

	@Test
	public void hideStep_afterShowStep_actuallyHides() throws Exception
	{
		view.showStep(1, 3, "Do the thing", false, Collections.emptyList());
		flushEdt();
		assertTrue( view.isVisible(),"showStep should make widget visible");

		view.hideStep();
		flushEdt();
		assertFalse( view.isVisible(),"hideStep must make widget invisible");
	}

	@Test
	public void showStep_makesVisible() throws Exception
	{
		view.showStep(2, 4, "Pick up reward", true, Collections.emptyList());
		flushEdt();
		assertTrue( view.isVisible(),"showStep must make widget visible");
	}

	// ── Step-description wrap regression tests (issue #575) ─────────────────

	/**
	 * Regression for issue #575. The General Graardor kill-step description was
	 * truncated mid-string in the side panel because the HTML wrap width was
	 * larger than the worst-case usable label width in a RuneLite plugin panel
	 * (panel = 225px, minus shell border, widget border, and scrollbar gutter).
	 *
	 * <p>The fix lowers the wrap width and HTML-escapes the description. This test
	 * asserts the rendered HTML contains the full description verbatim — if the
	 * wrap width drifts back up or the escaping breaks the inline text, this
	 * test fails before the panel ever ships.
	 */
	@Test
	public void showStep_longDescription_fullTextPresentVerbatim() throws Exception
	{
		String longDescription = "Enter the Bandos boss room and kill General Graardor";
		view.showStep(4, 5, longDescription, false, Collections.emptyList());
		flushEdt();

		JTextArea area = findStepProgressArea(view);
		assertNotNull(area, "Step-progress text area must be present");
		String rendered = area.getText();
		assertNotNull(rendered, "Step-progress text must be set");
		assertTrue(
			rendered.contains(longDescription),
			"Rendered text must contain full description verbatim — no mid-string truncation (#575). Actual: "
				+ rendered);
	}

	/**
	 * Description text is now rendered as plain text by a {@link JTextArea}, so
	 * reserved characters that the prior HTML-rendered JLabel had to escape
	 * (&lt;, &gt;, &amp;) must now appear verbatim. This pins the contract so a
	 * future revert to HTML rendering — which would re-introduce both the wrap
	 * truncation bug (#575) and the need for escaping — surfaces immediately.
	 */
	@Test
	public void showStep_descriptionWithReservedChars_renderedVerbatim() throws Exception
	{
		String descriptionWithMarkup = "Use <halberd> & break the door";
		view.showStep(1, 1, descriptionWithMarkup, false, Collections.emptyList());
		flushEdt();

		JTextArea area = findStepProgressArea(view);
		assertNotNull(area, "Step-progress text area must be present");
		String rendered = area.getText();
		assertNotNull(rendered);
		assertTrue(
			rendered.contains("<halberd>"),
			"'<halberd>' must appear verbatim (no HTML escaping). Actual: " + rendered);
		assertTrue(
			rendered.contains("& break"),
			"'&' must appear verbatim (no HTML escaping). Actual: " + rendered);
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
		assertFalse(
			findRequiredItemsPanel(view).isVisible(),"Required-items panel must be hidden when item list is empty");
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
		assertNotNull( nameLabel,"Name label must be present for HELD row");
		assertEquals(
			RequiredItemDisplay.COLOR_HELD, nameLabel.getForeground(),"HELD row must use green colour");
		assertFalse(
			nameLabel.getToolTipText() != null
				&& nameLabel.getToolTipText().contains("in bank"),"HELD tooltip must not mention bank");
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
		assertNotNull( nameLabel,"Name label must be present for IN_BANK row");
		assertEquals( Color.WHITE, nameLabel.getForeground(),"IN_BANK row must use white colour");
		assertTrue(
			nameLabel.getToolTipText() != null
				&& nameLabel.getToolTipText().contains("in bank"),"IN_BANK tooltip must contain 'in bank' hint");
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
		assertNotNull( nameLabel,"Name label must be present for MISSING row");
		assertEquals(
			RequiredItemDisplay.COLOR_MISSING, nameLabel.getForeground(),"MISSING row must use red colour");
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
		assertEquals( 3, nameLabels.size(),"Must have exactly 3 name labels");
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
		assertEquals(
			RequiredItemDisplay.COLOR_MISSING, first.getForeground(),"Initially MISSING must be red");

		// Second call: item is now HELD (player picked it up)
		view.showStep(1, 1, "Collect item", false, Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD)));
		flushEdt();

		JLabel updated = findFirstNameLabel(view);
		assertEquals(
			RequiredItemDisplay.COLOR_HELD, updated.getForeground(),"After transition to HELD must be green");
	}

	// ── Idempotent-render tests (#681) ──────────────────────────────────────
	//
	// While guidance is active, GuidanceSequencer.onInventoryChanged re-notifies the
	// current (unchanged) step on every inventory change. Each re-notify reaches
	// showStep; without an idempotency guard the item rows are torn down (removeAll)
	// and rebuilt every time, making the "Items needed" section visibly flash. The
	// guard skips the rebuild when the full render signature is unchanged. These tests
	// pin that contract via component identity: a true rebuild replaces the row JPanel
	// instances, an idempotent skip leaves the exact same instances in place.

	/**
	 * A second showStep with byte-for-byte identical inputs must NOT rebuild: the row
	 * panel instances must be the same objects as after the first call (no removeAll,
	 * no teardown → no flash). This is the core #681 fix.
	 */
	@Test
	public void showStep_identicalSecondCall_doesNotRebuild() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.MISSING));

		view.showStep(1, 3, "Collect item", false, rows);
		flushEdt();
		List<Component> firstRows = rowComponents(findRequiredItemsPanel(view));
		assertFalse(firstRows.isEmpty(), "First render must produce rows");

		// Re-notify of the same step with identical resolved items (the inventory-change case)
		view.showStep(1, 3, "Collect item", false,
			Collections.singletonList(new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.MISSING)));
		flushEdt();
		List<Component> secondRows = rowComponents(findRequiredItemsPanel(view));

		assertEquals(firstRows.size(), secondRows.size(), "Row count must be unchanged");
		for (int i = 0; i < firstRows.size(); i++)
		{
			assertTrue(
				firstRows.get(i) == secondRows.get(i),
				"Identical re-notify must reuse the same row instances (no rebuild) — #681");
		}
	}

	/**
	 * A second showStep with CHANGED content (item availability flipped) must rebuild:
	 * the row panel instances must be replaced so the new colour/status is rendered.
	 * Liveness must be preserved — only redundant re-notifies are skipped.
	 */
	@Test
	public void showStep_changedSecondCall_rebuilds() throws Exception
	{
		view.showStep(1, 3, "Collect item", false, Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.MISSING)));
		flushEdt();
		List<Component> firstRows = rowComponents(findRequiredItemsPanel(view));
		assertFalse(firstRows.isEmpty());

		// Player picked the item up — status flips MISSING → HELD, must re-render
		view.showStep(1, 3, "Collect item", false, Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD)));
		flushEdt();
		List<Component> secondRows = rowComponents(findRequiredItemsPanel(view));

		assertTrue(
			firstRows.isEmpty() || secondRows.isEmpty() || firstRows.get(0) != secondRows.get(0),
			"Changed content must rebuild row instances (liveness preserved)");
		JLabel updated = findFirstNameLabel(view);
		assertEquals(
			RequiredItemDisplay.COLOR_HELD, updated.getForeground(),
			"Changed re-notify must render the new HELD colour");
	}

	/**
	 * After hideStep the idempotency cache must be invalidated: a subsequent showStep
	 * with inputs identical to the pre-hide render must rebuild (the panels were torn
	 * down by hideStep, so reusing the cache would leave them empty).
	 */
	@Test
	public void showStep_afterHide_rebuildsEvenIfIdentical() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD));

		view.showStep(1, 1, "Do thing", false, rows);
		flushEdt();
		view.hideStep();
		flushEdt();

		view.showStep(1, 1, "Do thing", false,
			Collections.singletonList(new RequiredItemDisplay(TINDERBOX_ID, "Tinderbox", Status.HELD)));
		flushEdt();

		assertTrue(
			findRequiredItemsPanel(view).isVisible(),
			"showStep after hideStep must rebuild and re-show the items panel");
		assertFalse(
			rowComponents(findRequiredItemsPanel(view)).isEmpty(),
			"Items panel must contain rows again after a re-show");
	}

	/** Returns the row JPanel children of {@code panel} (the per-item rows, excluding labels/struts). */
	private static List<Component> rowComponents(JPanel panel)
	{
		List<Component> rows = new java.util.ArrayList<>();
		if (panel == null)
		{
			return rows;
		}
		for (Component c : panel.getComponents())
		{
			if (c instanceof JPanel)
			{
				rows.add(c);
			}
		}
		return rows;
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
		assertNotNull( sectionsPanel,"sectionsPanel must be present in widget");
		assertFalse(
			sectionsPanel.isVisible(),"sectionsPanel must be hidden when no step has a section");
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
		assertTrue(
			sectionsPanel.isVisible(),"sectionsPanel must be visible when steps have sections");
		// At least two header buttons (one per section)
		List<JButton> buttons = findAllButtons(sectionsPanel);
		assertEquals( 2, buttons.size(),"Must have one header button per section");
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
		assertEquals( 3, buttons.size(),"Must have 3 header buttons (Travel, Combat, Travel)");

		// Travel (step 1) — not active, should be collapsed
		assertTrue(
			buttons.get(0).getText().startsWith("▶"),"Inactive section must start with collapse arrow");
		// Combat (step 2) — active, must be expanded
		assertTrue(
			buttons.get(1).getText().startsWith("▼"),"Active section must start with expand arrow");
		// Travel (step 3) — not active, should be collapsed
		assertTrue(
			buttons.get(2).getText().startsWith("▶"),"Inactive section must start with collapse arrow");
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
		assertTrue(
			buttons.get(1).getText().startsWith("▼"),"New active section must show expanded arrow after step change");
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
		assertFalse(
			reqPanel.isVisible(),"Inline required-items panel must be hidden in sectioned mode");
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
		assertTrue( findSectionsPanel(view).isVisible(),"sectionsPanel must be visible before hide");

		view.hideStep();
		flushEdt();
		assertFalse( findSectionsPanel(view).isVisible(),"sectionsPanel must be hidden after hideStep");
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
		assertNotNull( recPanel,"recommendedItemsPanel must be present in widget");
		assertTrue(
			recPanel.isVisible(),"recommendedItemsPanel must be visible when rows are non-empty");
		// Required panel must be hidden since we passed empty
		JPanel reqPanel = findRequiredItemsPanel(view);
		assertFalse(
			reqPanel.isVisible(),"Required items panel must be hidden when required list is empty");
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
		assertFalse(
			recPanel.isVisible(),"recommendedItemsPanel must be hidden when recommended list is empty");
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

		assertTrue(
			findRequiredItemsPanel(view).isVisible(),"Items needed panel must be visible when required list populated");
		assertTrue(
			findRecommendedItemsPanel(view).isVisible(),"Recommended panel must be visible when recommended list populated");
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
		assertEquals(
			RequiredItemDisplay.COLOR_HELD, label.getForeground(),"HELD recommended row must use green colour");
	}

	@Test
	public void updateRecommendedItemDisplay_inBankStatus_whiteLabelWithTooltip() throws Exception
	{
		view.updateRecommendedItemDisplay(Collections.singletonList(
			new RequiredItemDisplay(PYRE_LOGS_ID, "Pyre logs", Status.IN_BANK)));
		flushEdt();

		JLabel label = findFirstRecommendedNameLabel(view);
		assertNotNull(label);
		assertEquals(
			Color.WHITE, label.getForeground(),"IN_BANK recommended row must use white colour");
		assertTrue(
			label.getToolTipText() != null && label.getToolTipText().contains("in bank"),"IN_BANK recommended tooltip must contain 'in bank'");
	}

	@Test
	public void updateRecommendedItemDisplay_missingStatus_redLabel() throws Exception
	{
		view.updateRecommendedItemDisplay(Collections.singletonList(
			new RequiredItemDisplay(SHADE_REMAINS_ID, "Loar remains", Status.MISSING)));
		flushEdt();

		JLabel label = findFirstRecommendedNameLabel(view);
		assertNotNull(label);
		assertEquals(
			RequiredItemDisplay.COLOR_MISSING, label.getForeground(),"MISSING recommended row must use red colour");
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

		assertFalse(
			findRequiredItemsPanel(view).isVisible(),"Required panel must be hidden after hideStep");
		assertFalse(
			findRecommendedItemsPanel(view).isVisible(),"Recommended panel must be hidden after hideStep");
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
		assertEquals(
			recIds, step.getRecommendedItemIds(),"recommendedItemIds must be accessible via getter");
	}

	/**
	 * Schema-level: a step constructed without recommendedItemIds (null) has
	 * a null getter return rather than an empty list.
	 */
	@Test
	public void guidanceStep_recommendedItemIds_nullWhenAbsent()
	{
		GuidanceStep step = stepWithSection(null); // no recommendedItemIds
		assertFalse(
			step.getRecommendedItemIds() != null,"recommendedItemIds must be null when not set");
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
	 * Returns the step-progress label — the first direct-child {@link JLabel} of
	 * the StepProgressView itself (constructed first in the BoxLayout). Used by
	 * the #575 wrap-regression tests.
	 */
	private static JTextArea findStepProgressArea(StepProgressView view)
	{
		for (Component c : view.getComponents())
		{
			if (c instanceof JTextArea)
			{
				return (JTextArea) c;
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

	// ── Icon-driven step-control button tests (#547) ────────────────────────
	//
	// Originally added for #369 to assert the Skip/Reset/Sync text buttons fired
	// their callbacks. #547 replaced the text buttons with music-player style
	// icon buttons (skip-next, stop, restart, sync) so these tests now locate
	// the buttons by tooltip instead of by label text.

	/**
	 * Plain-English tooltip required by the #547 acceptance criteria, one per
	 * action icon. These strings are the contract — UX validators read them as
	 * the discoverability story for each control.
	 */
	private static final String TOOLTIP_SKIP = "Skip to next step";
	private static final String TOOLTIP_STOP = "Stop guidance — clear all overlays";
	private static final String TOOLTIP_RESTART = "Restart this task from the first step";
	private static final String TOOLTIP_SYNC = "Sync collection-log state";

	/**
	 * After construction all four icon-driven controls (Skip, Stop, Restart,
	 * Sync) must be present and resolvable by tooltip.
	 */
	@Test
	public void construction_hasAllFourIconButtons()
	{
		assertNotNull( findButtonByTooltip(view, TOOLTIP_SKIP),"Skip icon button must exist");
		assertNotNull( findButtonByTooltip(view, TOOLTIP_STOP),"Stop icon button must exist");
		assertNotNull( findButtonByTooltip(view, TOOLTIP_RESTART),"Restart icon button must exist");
		assertNotNull( findButtonByTooltip(view, TOOLTIP_SYNC),"Sync icon button must exist");
	}

	/**
	 * Icon buttons must render as icon-only — empty (or null) text so the glyph
	 * is the sole visual affordance.
	 */
	@Test
	public void iconButtons_haveNoText()
	{
		for (String tooltip : new String[]{TOOLTIP_SKIP, TOOLTIP_STOP, TOOLTIP_RESTART, TOOLTIP_SYNC})
		{
			JButton btn = findButtonByTooltip(view, tooltip);
			assertNotNull( btn,"Icon button for tooltip '" + tooltip + "' must exist");
			String text = btn.getText();
			assertTrue(
				text == null || text.isEmpty(),"Icon button '" + tooltip + "' must have empty text but had '" + text + "'");
			assertNotNull(
				btn.getIcon(),"Icon button '" + tooltip + "' must have a graphic icon set");
		}
	}

	/**
	 * Each icon button must offer a click target of at least 24x24 to satisfy the
	 * #547 acceptance criteria.
	 */
	@Test
	public void iconButtons_clickTargetAtLeast24x24()
	{
		for (String tooltip : new String[]{TOOLTIP_SKIP, TOOLTIP_STOP, TOOLTIP_RESTART, TOOLTIP_SYNC})
		{
			JButton btn = findButtonByTooltip(view, tooltip);
			assertNotNull(btn);
			java.awt.Dimension size = btn.getPreferredSize();
			assertTrue(
				size.width >= 24,"Icon button '" + tooltip + "' must be ≥24px wide (was " + size.width + ")");
			assertTrue(
				size.height >= 24,"Icon button '" + tooltip + "' must be ≥24px tall (was " + size.height + ")");
		}
	}

	/**
	 * The Skip icon button must invoke the skip callback when clicked.
	 */
	@Test
	public void skipButton_click_invokesSkipCallback() throws Exception
	{
		AtomicBoolean fired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> fired.set(true), () -> {}, () -> {}, () -> {});
		flushEdt();

		JButton btn = findButtonByTooltip(view, TOOLTIP_SKIP);
		assertNotNull(btn);
		SwingUtilities.invokeAndWait(btn::doClick);

		assertTrue( fired.get(),"Skip callback must fire when Skip icon clicked");
	}

	/**
	 * The Stop icon button must invoke the stop callback when clicked.
	 * This is the new affordance from #547 — replaces the surprise of "Reset
	 * actually restarts the task" by giving deactivation its own icon.
	 */
	@Test
	public void stopButton_click_invokesStopCallback() throws Exception
	{
		AtomicBoolean fired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> fired.set(true), () -> {}, () -> {});
		flushEdt();

		JButton btn = findButtonByTooltip(view, TOOLTIP_STOP);
		assertNotNull(btn);
		SwingUtilities.invokeAndWait(btn::doClick);

		assertTrue( fired.get(),"Stop callback must fire when Stop icon clicked");
	}

	/**
	 * The Stop icon button must visually render its red border so the destructive
	 * affordance is distinguishable from the non-destructive controls. The shared
	 * {@code buildIconButton} helper disables border painting for a clean icon-only
	 * look on Skip/Restart/Sync; the Stop button must re-enable it after the
	 * explicit red border is applied, otherwise the border is silently dropped at
	 * paint time and Stop looks identical to the other icons (#547 regression).
	 */
	@Test
	public void stopButton_paintsRedBorder() throws Exception
	{
		JButton stopBtn = findButtonByTooltip(view, TOOLTIP_STOP);
		assertNotNull( stopBtn,"Stop icon button must exist");
		assertTrue(
			stopBtn.isBorderPainted(),"Stop button must paint its border so the red outline is visible");
		assertNotNull( stopBtn.getBorder(),"Stop button must carry an explicit border");

		// The other icon buttons must remain borderless for the clean icon-only look.
		for (String tooltip : new String[]{TOOLTIP_SKIP, TOOLTIP_RESTART, TOOLTIP_SYNC})
		{
			JButton btn = findButtonByTooltip(view, tooltip);
			assertNotNull(btn);
			assertFalse(
				btn.isBorderPainted(),
				"Non-destructive icon button '" + tooltip
					+ "' must not paint a border (icon-only look)");
		}
	}

	/**
	 * The Restart icon button must invoke the reset callback when clicked.
	 */
	@Test
	public void restartButton_click_invokesResetCallback() throws Exception
	{
		AtomicBoolean fired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> {}, () -> fired.set(true), () -> {});
		flushEdt();

		JButton btn = findButtonByTooltip(view, TOOLTIP_RESTART);
		assertNotNull(btn);
		SwingUtilities.invokeAndWait(btn::doClick);

		assertTrue( fired.get(),"Restart callback must fire when Restart icon clicked");
	}

	/**
	 * The Sync icon button must invoke the sync callback when clicked.
	 */
	@Test
	public void syncButton_click_invokesSyncCallback() throws Exception
	{
		AtomicBoolean fired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> {}, () -> {}, () -> fired.set(true));
		flushEdt();

		JButton btn = findButtonByTooltip(view, TOOLTIP_SYNC);
		assertNotNull(btn);
		SwingUtilities.invokeAndWait(btn::doClick);

		assertTrue( fired.get(),"Sync callback must fire when Sync icon clicked");
	}

	/**
	 * Behavioural compatibility for the legacy 4-arg setCallbacks overload
	 * (advancer, skipper, resetter, syncer). Clicking restart/sync via the
	 * icon buttons must still fire the corresponding callbacks after the
	 * legacy overload is invoked.
	 */
	@Test
	public void legacyFourArgSetCallbacks_stillRoutesRestartAndSync() throws Exception
	{
		AtomicBoolean resetFired = new AtomicBoolean(false);
		AtomicBoolean syncFired = new AtomicBoolean(false);
		view.setCallbacks(() -> {}, () -> {}, () -> resetFired.set(true), () -> syncFired.set(true));
		flushEdt();

		JButton restartBtn = findButtonByTooltip(view, TOOLTIP_RESTART);
		JButton syncBtn = findButtonByTooltip(view, TOOLTIP_SYNC);
		assertNotNull(restartBtn);
		assertNotNull(syncBtn);
		SwingUtilities.invokeAndWait(() ->
		{
			restartBtn.doClick();
			syncBtn.doClick();
		});

		assertTrue( resetFired.get(),"Legacy 4-arg overload must still route to restart");
		assertTrue( syncFired.get(),"Legacy 4-arg overload must still route to sync");
	}

	/**
	 * setCallbacks(advancer, skipper) two-arg overload must remain valid — existing
	 * callers must not break.
	 */
	@Test
	public void setCallbacks_twoArg_doesNotThrow()
	{
		view.setCallbacks(() -> {}, () -> {});
	}

	/**
	 * When setCallbacks is called with null stop/reset/sync callbacks, clicking the
	 * buttons must not throw a NullPointerException.
	 */
	@Test
	public void iconButtons_nullCallbacks_doNotThrow() throws Exception
	{
		view.setCallbacks(() -> {}, () -> {}, null, null, null);
		flushEdt();

		JButton stopBtn = findButtonByTooltip(view, TOOLTIP_STOP);
		JButton restartBtn = findButtonByTooltip(view, TOOLTIP_RESTART);
		JButton syncBtn = findButtonByTooltip(view, TOOLTIP_SYNC);
		assertNotNull(stopBtn);
		assertNotNull(restartBtn);
		assertNotNull(syncBtn);
		SwingUtilities.invokeAndWait(() ->
		{
			stopBtn.doClick();
			restartBtn.doClick();
			syncBtn.doClick();
		});
	}

	/**
	 * Tooltip text contract — the exact plain-English strings the UX checklist
	 * promises will appear on hover for each control.
	 */
	@Test
	public void iconButtons_haveExpectedTooltips()
	{
		assertEquals(TOOLTIP_SKIP, findButtonByTooltip(view, TOOLTIP_SKIP).getToolTipText());
		assertEquals(TOOLTIP_STOP, findButtonByTooltip(view, TOOLTIP_STOP).getToolTipText());
		assertEquals(TOOLTIP_RESTART, findButtonByTooltip(view, TOOLTIP_RESTART).getToolTipText());
		assertEquals(TOOLTIP_SYNC, findButtonByTooltip(view, TOOLTIP_SYNC).getToolTipText());
	}

	// ── Helpers (icon button row) ────────────────────────────────────────────

	/**
	 * Finds the first {@link JButton} anywhere in {@code root}'s component tree
	 * whose tooltip equals {@code tooltip}. Searches recursively. Used for the
	 * #547 icon-button row where buttons have empty text.
	 */
	private static JButton findButtonByTooltip(JPanel root, String tooltip)
	{
		for (Component c : root.getComponents())
		{
			if (c instanceof JButton)
			{
				JButton btn = (JButton) c;
				if (tooltip.equals(btn.getToolTipText()))
				{
					return btn;
				}
			}
			else if (c instanceof JPanel)
			{
				JButton found = findButtonByTooltipInPanel((JPanel) c, tooltip);
				if (found != null)
				{
					return found;
				}
			}
		}
		return null;
	}

	private static JButton findButtonByTooltipInPanel(JPanel panel, String tooltip)
	{
		for (Component c : panel.getComponents())
		{
			if (c instanceof JButton)
			{
				JButton btn = (JButton) c;
				if (tooltip.equals(btn.getToolTipText()))
				{
					return btn;
				}
			}
			else if (c instanceof JPanel)
			{
				JButton found = findButtonByTooltipInPanel((JPanel) c, tooltip);
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
			null, // waypoints
			null,  // dynamicTargetEvaluator
			null,  // conditionTree
			null  // perItemStepPriority
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
			null, // section
			null, // waypoints
			null,   // dynamicTargetEvaluator
			null,   // conditionTree
			null   // perItemStepPriority
		);
	}
}
