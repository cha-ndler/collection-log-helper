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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

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
	public void hide_doesNotThrow()
	{
		view.hide();
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
	public void hide_afterShowStep_doesNotThrow()
	{
		view.showStep(1, 3, "Do the thing", false, Collections.emptyList());
		view.hide();
	}
}
