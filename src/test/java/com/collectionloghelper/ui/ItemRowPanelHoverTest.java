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
package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for issue #428: the {@code mouseExited} handler on a
 * search-result row must not reset the hover background when the mouse
 * merely crosses into a child component (icon, name label, rate label, …).
 *
 * <p>Before the fix, every intra-row child transition called
 * {@code setBackground()}, triggering rapid repaints that smeared the
 * non-opaque child labels and produced a ghost-text artefact — e.g. a
 * tier suffix such as "(1)" rendered twice with a horizontal offset.
 *
 * <p>The fix: in {@code mouseExited}, skip the background reset when
 * {@code e.getPoint()} is still within the panel's own bounds (which is
 * true for panel→child transitions but false when the mouse truly leaves).
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemRowPanelHoverTest
{
	@Mock
	private ItemManager itemManager;

	private ItemRowPanel row;
	private Color normalBg;

	@Before
	public void setUp()
	{
		AsyncBufferedImage image = mock(AsyncBufferedImage.class);
		when(itemManager.getImage(anyInt())).thenReturn(image);

		CollectionLogItem item = new CollectionLogItem(
			13359, "Shayzien helm (1)", 1.0, false, null, 0, 40, false, true);
		CollectionLogSource source = new CollectionLogSource(
			"Shayzien Armour", CollectionLogCategory.OTHER,
			1516, 3617, 0, 36, 0, null, null, null, 0, null, 0, false, 2,
			null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(item), null /* metaAuthoredDate */);

		row = new ItemRowPanel(item, source, false, 0, false,
			Collections.emptyList(), itemManager, () -> {});

		// Give the panel a concrete size so contains() returns meaningful results
		row.setSize(220, 36);
		normalBg = row.getBackground();
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	/** Fire mouseEntered from a point OUTSIDE the panel bounds (true hover start). */
	private void fireEnteredFromOutside()
	{
		// Point (-1, 18) is outside the panel — simulates mouse arriving from above
		MouseEvent e = new MouseEvent(row, MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, -1, 18, 1, false);
		for (MouseListener l : row.getMouseListeners())
		{
			l.mouseEntered(e);
		}
	}

	/** Fire mouseExited to a point INSIDE the panel bounds (mouse moved to child). */
	private void fireExitedToChild()
	{
		// Point (24, 18) is inside the row — simulates moving to a child widget
		MouseEvent e = new MouseEvent(row, MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, 24, 18, 1, false);
		for (MouseListener l : row.getMouseListeners())
		{
			l.mouseExited(e);
		}
	}

	/** Fire mouseExited to a point OUTSIDE the panel bounds (true hover end). */
	private void fireExitedToOutside()
	{
		// Point (-1, 18) is outside the row — simulates mouse leaving the row
		MouseEvent e = new MouseEvent(row, MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, -1, 18, 1, false);
		for (MouseListener l : row.getMouseListeners())
		{
			l.mouseExited(e);
		}
	}

	// ── Tests ────────────────────────────────────────────────────────────────

	@Test
	public void mouseEntered_activatesHoverBackground()
	{
		fireEnteredFromOutside();

		assertNotEquals("entering the row must change the background to the hover colour",
			normalBg, row.getBackground());
	}

	@Test
	public void mouseExited_toOutside_restoresNormalBackground()
	{
		fireEnteredFromOutside();
		Color hoverBg = row.getBackground();
		assertNotEquals(normalBg, hoverBg); // pre-condition

		fireExitedToOutside();

		assertEquals("exiting the row boundary must restore the normal background",
			normalBg, row.getBackground());
	}

	@Test
	public void mouseExited_toChildComponent_doesNotResetBackground()
	{
		// Regression: before the fix, moving into any child (icon label, name label,
		// rate label) fired mouseExited and reset the background, which in turn
		// caused a repaint race that left ghost text — see issue #428.
		fireEnteredFromOutside();
		Color hoverBg = row.getBackground();

		fireExitedToChild(); // exit point still within row bounds

		assertEquals(
			"exiting into a child component must NOT reset the hover background (issue #428)",
			hoverBg, row.getBackground());
	}

	@Test
	public void hoverCycle_enterThenExit_restoresNormalBg()
	{
		// Smoke: full enter / exit cycle leaves the background clean
		fireEnteredFromOutside();
		fireExitedToOutside();

		assertEquals("full hover cycle must end at normal background",
			normalBg, row.getBackground());
	}
}
