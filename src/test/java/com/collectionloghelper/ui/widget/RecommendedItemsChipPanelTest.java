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
import java.awt.Color;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RecommendedItemsChipPanel} (B.5.2).
 *
 * <p>Mirrors {@link RequiredItemsChipPanelTest} in structure. Covers:
 * <ol>
 *   <li>Empty / null list yields hidden panel.</li>
 *   <li>Muted-alpha border colours for all three statuses (HELD/IN_BANK/MISSING).</li>
 *   <li>Tooltip conventions (item name vs. bank message).</li>
 *   <li>Round-trip visibility: hidden after construction, visible after non-empty
 *       update, hidden again after empty update.</li>
 *   <li>Heading label reads "Recommended:" (not "Items needed:").</li>
 *   <li>Muted colours have reduced alpha vs. required panel colours.</li>
 * </ol>
 */
public class RecommendedItemsChipPanelTest
{
	private static final int TINDERBOX = 590;
	private static final int PYRE_LOGS = 3438;
	private static final int SHARK = 385;

	private ItemManager itemManager;
	private RecommendedItemsChipPanel panel;

	@Before
	public void setUp()
	{
		itemManager = Mockito.mock(ItemManager.class);
		Mockito.when(itemManager.getImage(Mockito.anyInt())).thenReturn(null);
		panel = new RecommendedItemsChipPanel(itemManager);
	}

	// ── Scenario 1: empty / null list → panel hidden ─────────────────────────

	@Test
	public void update_null_panelHidden() throws Exception
	{
		panel.update((List<RequiredItemDisplay>) null);
		flushEdt();
		assertFalse("Panel must be hidden for null list", panel.isVisible());
	}

	@Test
	public void update_emptyList_panelHidden() throws Exception
	{
		panel.update(Collections.<RequiredItemDisplay>emptyList());
		flushEdt();
		assertFalse("Panel must be hidden for empty list", panel.isVisible());
	}

	// ── Scenario 2: HELD → muted green border ────────────────────────────────

	@Test
	public void borderColorFor_heldStatus_mutedGreen()
	{
		Color color = RecommendedItemsChipPanel.borderColorFor(Status.HELD);
		assertEquals("HELD status must yield muted green border",
			RecommendedItemsChipPanel.COLOR_HELD_MUTED, color);
	}

	@Test
	public void heldMutedColor_hasReducedAlpha()
	{
		assertTrue("HELD muted colour must have alpha less than 255",
			RecommendedItemsChipPanel.COLOR_HELD_MUTED.getAlpha() < 255);
		assertFalse("Muted HELD colour must differ from required HELD colour",
			RecommendedItemsChipPanel.COLOR_HELD_MUTED.equals(RequiredItemDisplay.COLOR_HELD));
	}

	@Test
	public void tooltipFor_heldStatus_itemName()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD);
		assertEquals("HELD tooltip must be item name",
			"Tinderbox", RecommendedItemsChipPanel.tooltipFor(row));
	}

	// ── Scenario 3: IN_BANK → muted white border + bank tooltip ─────────────

	@Test
	public void borderColorFor_inBankStatus_mutedWhite()
	{
		Color color = RecommendedItemsChipPanel.borderColorFor(Status.IN_BANK);
		assertEquals("IN_BANK status must yield muted off-white border",
			RecommendedItemsChipPanel.COLOR_IN_BANK_MUTED, color);
	}

	@Test
	public void tooltipFor_inBankStatus_questHelperConvention()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.IN_BANK);
		assertEquals("IN_BANK tooltip must be Quest Helper bank message",
			RecommendedItemsChipPanel.IN_BANK_TOOLTIP,
			RecommendedItemsChipPanel.tooltipFor(row));
	}

	// ── Scenario 4: MISSING → muted red border ───────────────────────────────

	@Test
	public void borderColorFor_missingStatus_mutedRed()
	{
		Color color = RecommendedItemsChipPanel.borderColorFor(Status.MISSING);
		assertEquals("MISSING status must yield muted red border",
			RecommendedItemsChipPanel.COLOR_MISSING_MUTED, color);
	}

	@Test
	public void missingMutedColor_hasReducedAlpha()
	{
		assertTrue("MISSING muted colour must have alpha less than 255",
			RecommendedItemsChipPanel.COLOR_MISSING_MUTED.getAlpha() < 255);
		assertFalse("Muted MISSING colour must differ from required MISSING colour",
			RecommendedItemsChipPanel.COLOR_MISSING_MUTED.equals(RequiredItemDisplay.COLOR_MISSING));
	}

	@Test
	public void tooltipFor_missingStatus_itemName()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(SHARK, "Shark", Status.MISSING);
		assertEquals("MISSING tooltip must be item name",
			"Shark", RecommendedItemsChipPanel.tooltipFor(row));
	}

	// ── All-statuses coverage ────────────────────────────────────────────────

	@Test
	public void borderColorFor_allThreeStatuses_correctMutedColors()
	{
		assertEquals(RecommendedItemsChipPanel.COLOR_HELD_MUTED,
			RecommendedItemsChipPanel.borderColorFor(Status.HELD));
		assertEquals(RecommendedItemsChipPanel.COLOR_IN_BANK_MUTED,
			RecommendedItemsChipPanel.borderColorFor(Status.IN_BANK));
		assertEquals(RecommendedItemsChipPanel.COLOR_MISSING_MUTED,
			RecommendedItemsChipPanel.borderColorFor(Status.MISSING));
	}

	// ── buildChip ────────────────────────────────────────────────────────────

	@Test
	public void buildChip_heldItem_itemNameTooltip()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD);
		JLabel chip = panel.buildChip(row);

		assertNotNull("Chip must not be null", chip);
		assertEquals("HELD chip tooltip must be item name",
			"Tinderbox", chip.getToolTipText());
	}

	@Test
	public void buildChip_inBankItem_bankTooltip()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.IN_BANK);
		JLabel chip = panel.buildChip(row);

		assertNotNull("Chip must not be null", chip);
		assertEquals("IN_BANK chip tooltip must be bank message",
			RecommendedItemsChipPanel.IN_BANK_TOOLTIP, chip.getToolTipText());
	}

	@Test
	public void buildChip_threeMixedStatuses_allChipsBuilt()
	{
		List<RequiredItemDisplay> rows = Arrays.asList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.IN_BANK),
			new RequiredItemDisplay(SHARK, "Shark", Status.MISSING));

		for (RequiredItemDisplay row : rows)
		{
			JLabel chip = panel.buildChip(row);
			assertNotNull("Chip must not be null for status " + row.getStatus(), chip);
		}
	}

	// ── Round-trip visibility ────────────────────────────────────────────────

	@Test
	public void afterConstruction_hidden()
	{
		assertFalse("Panel must start hidden", panel.isVisible());
	}

	@Test
	public void update_nonEmptyList_panelVisible() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD));
		panel.update(rows);
		flushEdt();
		assertTrue("Panel must be visible after update with non-empty list", panel.isVisible());
	}

	@Test
	public void update_emptyAfterNonEmpty_hidesPanel() throws Exception
	{
		panel.update(Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD)));
		flushEdt();
		assertTrue("Panel visible after first non-empty update", panel.isVisible());

		panel.update(Collections.<RequiredItemDisplay>emptyList());
		flushEdt();
		assertFalse("Panel must be hidden after second empty update", panel.isVisible());
	}

	// ── Heading text ─────────────────────────────────────────────────────────

	/**
	 * The chip row must contain a "Recommended:" heading label after a non-empty update.
	 */
	@Test
	public void update_nonEmptyList_headingIsRecommended() throws Exception
	{
		panel.update(Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD)));
		flushEdt();

		// The chip row is the first (and only) JPanel child of the panel
		JPanel chipRow = null;
		for (Component c : panel.getComponents())
		{
			if (c instanceof JPanel)
			{
				chipRow = (JPanel) c;
				break;
			}
		}
		assertNotNull("Chip row panel must exist", chipRow);

		boolean foundHeading = false;
		for (Component c : chipRow.getComponents())
		{
			if (c instanceof JLabel && "Recommended:".equals(((JLabel) c).getText()))
			{
				foundHeading = true;
				break;
			}
		}
		assertTrue("Chip row must contain a 'Recommended:' heading label", foundHeading);
	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { /* no-op: drain the EDT queue */ });
	}
}
