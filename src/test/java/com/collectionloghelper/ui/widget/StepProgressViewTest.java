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

import com.collectionloghelper.guidance.RequiredItemDisplay;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import net.runelite.client.game.ItemManager;
import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

	// ── Helpers ─────────────────────────────────────────────────────────────

	/** Flush the Swing event queue so invokeLater-scheduled work completes. */
	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
	}

	/**
	 * Finds the required-items sub-panel inside {@code view}. It is the first
	 * {@link JPanel} direct child of the StepProgressView.
	 */
	private static JPanel findRequiredItemsPanel(StepProgressView view)
	{
		for (Component c : view.getComponents())
		{
			if (c instanceof JPanel)
			{
				return (JPanel) c;
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
}
