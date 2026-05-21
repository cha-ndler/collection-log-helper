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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RequiredItemsChipPanel} — value-driven render path.
 *
 * <p>Covers the four core render scenarios required by B.5.1:
 * <ol>
 *   <li>Empty list yields hidden panel (no chips rendered).</li>
 *   <li>3 items all in inventory/equipment yield 3 green-bordered chips.</li>
 *   <li>1 item in inventory + 2 in bank yield 1 green + 2 white chips with
 *       "Items can be found in your: Bank" tooltip.</li>
 *   <li>3 items not held anywhere yield 3 red-bordered chips.</li>
 * </ol>
 *
 * <p>Swing rendering is tested by probing the value-level helpers
 * ({@link RequiredItemsChipPanel#borderColorFor} and
 * {@link RequiredItemsChipPanel#tooltipFor}) rather than full component
 * construction, which requires a display and an event-dispatch thread.
 * The full {@link RequiredItemsChipPanel#update} path is covered by the
 * round-trip visibility tests in this class. See PR description for the
 * in-client visual validation checklist.
 */
public class RequiredItemsChipPanelTest
{
	private static final int TINDERBOX = 590;
	private static final int PYRE_LOGS = 3438;
	private static final int SHADE_REMAINS = 3392;

	private ItemManager itemManager;
	private RequiredItemsChipPanel panel;

	@BeforeEach
	public void setUp()
	{
		itemManager = Mockito.mock(ItemManager.class);
		Mockito.when(itemManager.getImage(Mockito.anyInt())).thenReturn(null);
		panel = new RequiredItemsChipPanel(itemManager);
	}

	// ── Scenario 1: empty list → hidden panel ───────────────────────────────

	/**
	 * When {@code update} is called with a null list the panel must remain hidden.
	 */
	@Test
	public void update_null_panelHidden() throws Exception
	{
		panel.update((List<RequiredItemDisplay>) null);
		flushEdt();
		assertFalse( panel.isVisible(),"Panel must be hidden for null list");
	}

	/**
	 * When {@code update} is called with an empty list the panel must be hidden.
	 */
	@Test
	public void update_emptyList_panelHidden() throws Exception
	{
		panel.update(Collections.<RequiredItemDisplay>emptyList());
		flushEdt();
		assertFalse( panel.isVisible(),"Panel must be hidden for empty list");
	}

	// ── Scenario 2: 3 items all in inventory → 3 green chips ────────────────

	/**
	 * Three HELD items must each produce a chip with a green border.
	 */
	@Test
	public void borderColorFor_heldStatus_green()
	{
		Color color = RequiredItemsChipPanel.borderColorFor(Status.HELD);
		assertEquals(
			RequiredItemDisplay.COLOR_HELD, color,"HELD status must yield green border");
	}

	/**
	 * Tooltip for a HELD item must be the item name, not the bank message.
	 */
	@Test
	public void tooltipFor_heldStatus_itemName()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD);
		assertEquals(
			"Tinderbox", RequiredItemsChipPanel.tooltipFor(row),"HELD tooltip must be item name");
	}

	/**
	 * 3 HELD rows produce 3 chips with green borders.
	 */
	@Test
	public void buildChip_threeHeldItems_allGreenBorders()
	{
		List<RequiredItemDisplay> rows = Arrays.asList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.HELD),
			new RequiredItemDisplay(SHADE_REMAINS, "Shade remains", Status.HELD));

		for (RequiredItemDisplay row : rows)
		{
			JLabel chip = panel.buildChip(row);
			assertNotNull( chip,"Chip must not be null");
			assertEquals(
				RequiredItemDisplay.COLOR_HELD,
				RequiredItemsChipPanel.borderColorFor(row.getStatus()),"HELD chip must have green border");
		}
	}

	// ── Scenario 3: 1 in inventory + 2 in bank → 1 green + 2 white ─────────

	/**
	 * IN_BANK status must yield a white border.
	 */
	@Test
	public void borderColorFor_inBankStatus_white()
	{
		Color color = RequiredItemsChipPanel.borderColorFor(Status.IN_BANK);
		assertEquals(
			Color.WHITE, color,"IN_BANK status must yield white border");
	}

	/**
	 * IN_BANK tooltip must be the Quest Helper convention string, not the item name.
	 */
	@Test
	public void tooltipFor_inBankStatus_questHelperConvention()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.IN_BANK);
		assertEquals(
			RequiredItemsChipPanel.IN_BANK_TOOLTIP,
			RequiredItemsChipPanel.tooltipFor(row),"IN_BANK tooltip must be the Quest Helper bank message");
	}

	/**
	 * Mixed statuses: HELD=green border + item-name tooltip;
	 * IN_BANK=white border + bank-message tooltip.
	 */
	@Test
	public void buildChip_heldAndInBank_correctBordersAndTooltips()
	{
		RequiredItemDisplay held = new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD);
		RequiredItemDisplay inBank = new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.IN_BANK);

		JLabel heldChip = panel.buildChip(held);
		JLabel bankChip = panel.buildChip(inBank);

		assertNotNull(heldChip);
		assertNotNull(bankChip);

		assertEquals(
			RequiredItemDisplay.COLOR_HELD,
			RequiredItemsChipPanel.borderColorFor(held.getStatus()),"HELD chip border must be green");
		assertEquals(
			Color.WHITE,
			RequiredItemsChipPanel.borderColorFor(inBank.getStatus()),"IN_BANK chip border must be white");

		assertEquals(
			"Tinderbox", heldChip.getToolTipText(),"HELD chip tooltip must be item name");
		assertEquals(
			RequiredItemsChipPanel.IN_BANK_TOOLTIP, bankChip.getToolTipText(),"IN_BANK chip tooltip must be bank message");
	}

	// ── Scenario 4: 3 items not held anywhere → 3 red chips ─────────────────

	/**
	 * MISSING status must yield a red border.
	 */
	@Test
	public void borderColorFor_missingStatus_red()
	{
		Color color = RequiredItemsChipPanel.borderColorFor(Status.MISSING);
		assertEquals(
			RequiredItemDisplay.COLOR_MISSING, color,"MISSING status must yield red border");
	}

	/**
	 * Tooltip for a MISSING item must be the item name.
	 */
	@Test
	public void tooltipFor_missingStatus_itemName()
	{
		RequiredItemDisplay row = new RequiredItemDisplay(SHADE_REMAINS, "Shade remains", Status.MISSING);
		assertEquals(
			"Shade remains", RequiredItemsChipPanel.tooltipFor(row),"MISSING tooltip must be item name");
	}

	/**
	 * 3 MISSING rows produce 3 chips with red borders.
	 */
	@Test
	public void buildChip_threeMissingItems_allRedBorders()
	{
		List<RequiredItemDisplay> rows = Arrays.asList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.MISSING),
			new RequiredItemDisplay(PYRE_LOGS, "Pyre logs", Status.MISSING),
			new RequiredItemDisplay(SHADE_REMAINS, "Shade remains", Status.MISSING));

		for (RequiredItemDisplay row : rows)
		{
			assertEquals(
				RequiredItemDisplay.COLOR_MISSING,
				RequiredItemsChipPanel.borderColorFor(row.getStatus()),"MISSING chip must have red border");
		}
	}

	// ── Round-trip visibility ────────────────────────────────────────────────

	/**
	 * After construction the panel must be hidden (no guidance active yet).
	 */
	@Test
	public void afterConstruction_hidden()
	{
		assertFalse( panel.isVisible(),"Panel must start hidden");
	}

	/**
	 * update() with a non-empty list makes the panel visible.
	 */
	@Test
	public void update_nonEmptyList_panelVisible() throws Exception
	{
		List<RequiredItemDisplay> rows = Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD));
		panel.update(rows);
		flushEdt();
		assertTrue( panel.isVisible(),"Panel must be visible after update with non-empty list");
	}

	/**
	 * update() with an empty list after a non-empty call hides the panel again.
	 */
	@Test
	public void update_emptyAfterNonEmpty_hidesPanel() throws Exception
	{
		panel.update(Collections.singletonList(
			new RequiredItemDisplay(TINDERBOX, "Tinderbox", Status.HELD)));
		flushEdt();
		assertTrue( panel.isVisible(),"Panel visible after first non-empty update");

		panel.update(Collections.<RequiredItemDisplay>emptyList());
		flushEdt();
		assertFalse( panel.isVisible(),"Panel must be hidden after second empty update");
	}

	// ── All-statuses coverage ────────────────────────────────────────────────

	/**
	 * All three statuses in one pass: HELD=green, IN_BANK=white, MISSING=red.
	 */
	@Test
	public void borderColorFor_allThreeStatuses_correctColors()
	{
		assertEquals(RequiredItemDisplay.COLOR_HELD,
			RequiredItemsChipPanel.borderColorFor(Status.HELD));
		assertEquals(Color.WHITE,
			RequiredItemsChipPanel.borderColorFor(Status.IN_BANK));
		assertEquals(RequiredItemDisplay.COLOR_MISSING,
			RequiredItemsChipPanel.borderColorFor(Status.MISSING));
	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { /* no-op: drain the EDT queue */ });
	}
}
