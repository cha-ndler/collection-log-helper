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

import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerInventoryState;
import net.runelite.client.game.ItemManager;
import java.util.Collections;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link StepProgressView}.
 */
public class StepProgressViewTest
{
	private ItemManager itemManager;
	private PlayerInventoryState inventoryState;
	private PlayerBankState bankState;
	private StepProgressView view;

	@Before
	public void setUp()
	{
		itemManager = Mockito.mock(ItemManager.class);
		inventoryState = Mockito.mock(PlayerInventoryState.class);
		bankState = Mockito.mock(PlayerBankState.class);
		view = new StepProgressView(itemManager, inventoryState, bankState);
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

	/** Flush the Swing event queue so invokeLater-scheduled work completes. */
	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { });
	}
}
