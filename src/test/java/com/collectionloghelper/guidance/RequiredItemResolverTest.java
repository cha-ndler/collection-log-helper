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
package com.collectionloghelper.guidance;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequiredItemResolverTest
{
	@Mock
	private PlayerInventoryState inventoryState;

	@Mock
	private PlayerBankState bankState;

	@Mock
	private ItemManager itemManager;

	@Mock
	private ItemComposition tinderboxComp;
	@Mock
	private ItemComposition pyreLogsComp;
	@Mock
	private ItemComposition remainsComp;

	private RequiredItemResolver resolver;

	private static final int TINDERBOX = 590;
	private static final int PYRE_LOGS = 3438;
	private static final int SHADE_REMAINS = 3392;

	@Before
	public void setUp()
	{
		resolver = new RequiredItemResolver(inventoryState, bankState, itemManager);

		// Default: nothing held, nothing in bank — keeps each test focused on
		// the one signal it stubs.
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasEquippedItem(anyInt())).thenReturn(false);
		lenient().when(bankState.hasItem(anyInt())).thenReturn(false);

		lenient().when(tinderboxComp.getName()).thenReturn("Tinderbox");
		lenient().when(pyreLogsComp.getName()).thenReturn("Pyre logs");
		lenient().when(remainsComp.getName()).thenReturn("Loar remains");
		lenient().when(itemManager.getItemComposition(TINDERBOX)).thenReturn(tinderboxComp);
		lenient().when(itemManager.getItemComposition(PYRE_LOGS)).thenReturn(pyreLogsComp);
		lenient().when(itemManager.getItemComposition(SHADE_REMAINS)).thenReturn(remainsComp);
	}

	@Test
	public void resolveReturnsEmptyForNullStep()
	{
		List<RequiredItemDisplay> result = resolver.resolve(null);
		assertTrue("Null step must yield an empty result", result.isEmpty());
	}

	@Test
	public void resolveReturnsEmptyForStepWithNoRequiredItems()
	{
		GuidanceStep step = stepWithRequiredItems(null);
		assertTrue(resolver.resolve(step).isEmpty());

		step = stepWithRequiredItems(Collections.emptyList());
		assertTrue(resolver.resolve(step).isEmpty());
	}

	@Test
	public void heldInInventoryResolvesToHeld()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(TINDERBOX)));

		assertEquals(1, rows.size());
		assertEquals("Tinderbox", rows.get(0).getName());
		assertEquals(Status.HELD, rows.get(0).getStatus());
	}

	@Test
	public void heldEquippedResolvesToHeldEvenWhenNotInInventory()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(false);
		when(inventoryState.hasEquippedItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(TINDERBOX)));

		assertEquals(Status.HELD, rows.get(0).getStatus());
	}

	@Test
	public void itemFoundOnlyInBankResolvesToInBank()
	{
		when(bankState.hasItem(PYRE_LOGS)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(PYRE_LOGS)));

		assertEquals(Status.IN_BANK, rows.get(0).getStatus());
		assertEquals("Pyre logs", rows.get(0).getName());
	}

	@Test
	public void itemNotFoundAnywhereResolvesToMissing()
	{
		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(SHADE_REMAINS)));

		assertEquals(Status.MISSING, rows.get(0).getStatus());
	}

	/**
	 * Held-in-inventory must short-circuit before the bank check so we don't
	 * pay a cache lookup on every required item the player is already carrying.
	 */
	@Test
	public void inventoryShortCircuitsBeforeBankCheck()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(TINDERBOX)));

		assertEquals(Status.HELD, rows.get(0).getStatus());
		verify(bankState, never()).hasItem(TINDERBOX);
	}

	@Test
	public void multipleItemsResolveIndependently()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);
		when(bankState.hasItem(PYRE_LOGS)).thenReturn(true);
		// SHADE_REMAINS is neither held nor in bank — MISSING by default

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Arrays.asList(TINDERBOX, PYRE_LOGS, SHADE_REMAINS)));

		assertEquals(3, rows.size());
		assertEquals(Status.HELD, rows.get(0).getStatus());
		assertEquals(Status.IN_BANK, rows.get(1).getStatus());
		assertEquals(Status.MISSING, rows.get(2).getStatus());
	}

	@Test
	public void invalidItemIdsAreSkipped()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Arrays.asList(0, -1, TINDERBOX, null)));

		assertEquals("Only the one valid id must produce a row", 1, rows.size());
		assertEquals(Status.HELD, rows.get(0).getStatus());
	}

	@Test
	public void itemCompositionFailureFallsBackToIdLabel()
	{
		final int unknownId = 99999;
		when(itemManager.getItemComposition(unknownId))
			.thenThrow(new RuntimeException("Cache miss"));

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(unknownId)));

		assertEquals(1, rows.size());
		assertEquals("Item #" + unknownId, rows.get(0).getName());
		assertEquals(Status.MISSING, rows.get(0).getStatus());
	}

	/**
	 * Regression for cha-ndler/collection-log-helper#388. ItemManager.getItem-
	 * Composition asserts "must be called on client thread"; from the EDT this
	 * throws java.lang.AssertionError, which is NOT a RuntimeException. The
	 * original catch (RuntimeException) let it propagate and silently aborted
	 * the entire activateGuidance call. The catch now widens to Throwable so
	 * the player still gets a usable row + the rest of activation completes.
	 */
	@Test
	public void itemCompositionAssertionErrorFallsBackToIdLabel()
	{
		final int badThreadId = 590;
		when(itemManager.getItemComposition(badThreadId))
			.thenThrow(new AssertionError("must be called on client thread"));

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(badThreadId)));

		assertEquals(1, rows.size());
		assertEquals("Item #" + badThreadId, rows.get(0).getName());
		assertEquals(Status.MISSING, rows.get(0).getStatus());
	}

	// --- Helpers ---

	private static GuidanceStep stepWithRequiredItems(List<Integer> requiredItemIds)
	{
		return new GuidanceStep(
			"Test step",
			0, 0, 0,
			0, null, null,
			null,
			requiredItemIds,
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,
			null,
			0, null, null,
			null,
			null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null,
			null,
			null,
			null
		);
	}
}
