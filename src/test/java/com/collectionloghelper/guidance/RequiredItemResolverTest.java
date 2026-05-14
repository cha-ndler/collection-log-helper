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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private GuidanceOverlayCoordinator coordinator;

	@Mock
	private ItemComposition tinderboxComp;
	@Mock
	private ItemComposition pyreLogsComp;
	@Mock
	private ItemComposition remainsComp;
	@Mock
	private ItemComposition loarRemainsComp;
	@Mock
	private ItemComposition loarPyreLogsComp;

	private RequiredItemResolver resolver;

	private static final int TINDERBOX = 590;
	private static final int PYRE_LOGS = 3438;
	private static final int SHADE_REMAINS = 3392;
	// Mort'ton tier consumables
	private static final int LOAR_REMAINS = 3402;
	private static final int LOAR_PYRE_LOGS = 3438;
	// Sample clog item IDs for per-item map tests
	private static final int BRONZE_LOCK = 25442;
	private static final int GOLD_LOCK = 25454;

	@Before
	public void setUp()
	{
		resolver = new RequiredItemResolver(inventoryState, bankState, itemManager);
		resolver.setCoordinator(coordinator);

		// Default: nothing held, nothing in bank — keeps each test focused on
		// the one signal it stubs.
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasEquippedItem(anyInt())).thenReturn(false);
		lenient().when(bankState.hasItem(anyInt())).thenReturn(false);
		// Default: no active target item
		lenient().when(coordinator.getActiveTargetItemId()).thenReturn(null);

		lenient().when(tinderboxComp.getName()).thenReturn("Tinderbox");
		lenient().when(pyreLogsComp.getName()).thenReturn("Pyre logs");
		lenient().when(remainsComp.getName()).thenReturn("Loar remains");
		lenient().when(loarRemainsComp.getName()).thenReturn("Loar remains");
		lenient().when(itemManager.getItemComposition(TINDERBOX)).thenReturn(tinderboxComp);
		lenient().when(itemManager.getItemComposition(PYRE_LOGS)).thenReturn(pyreLogsComp);
		lenient().when(itemManager.getItemComposition(SHADE_REMAINS)).thenReturn(remainsComp);
		lenient().when(itemManager.getItemComposition(LOAR_REMAINS)).thenReturn(loarRemainsComp);
		// Note: LOAR_PYRE_LOGS (3438) is the same OSRS item as PYRE_LOGS — Loar tier
		// pyre logs are named "Pyre logs" without a tier prefix.  pyreLogsComp above
		// covers both constants.  The loarPyreLogsComp @Mock field is intentionally
		// left as a no-op mock to keep the constant-naming consistent with worker tests.
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

	@Test
	public void resolvedRowPreservesItemId()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolve(
			stepWithRequiredItems(Collections.singletonList(TINDERBOX)));

		assertEquals("Item ID must be preserved in the display row so the panel can load the sprite",
			TINDERBOX, rows.get(0).getItemId());
	}

	// ── resolveRecommended (B.5.2) ──────────────────────────────────────────

	@Test
	public void resolveRecommendedReturnsEmptyForNullStep()
	{
		assertTrue("Null step must yield an empty recommended result",
			resolver.resolveRecommended(null).isEmpty());
	}

	@Test
	public void resolveRecommendedReturnsEmptyWhenFieldIsNull()
	{
		GuidanceStep step = stepWithRequiredAndRecommendedItems(null, null);
		assertTrue(resolver.resolveRecommended(step).isEmpty());
	}

	@Test
	public void resolveRecommendedReturnsEmptyWhenFieldIsEmptyList()
	{
		GuidanceStep step = stepWithRequiredAndRecommendedItems(null, Collections.emptyList());
		assertTrue(resolver.resolveRecommended(step).isEmpty());
	}

	@Test
	public void resolveRecommendedHeldInInventory_resolvesToHeld()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(
			stepWithRequiredAndRecommendedItems(null, Collections.singletonList(TINDERBOX)));

		assertEquals(1, rows.size());
		assertEquals(Status.HELD, rows.get(0).getStatus());
		assertEquals("Tinderbox", rows.get(0).getName());
	}

	@Test
	public void resolveRecommendedInBank_resolvesToInBank()
	{
		when(bankState.hasItem(PYRE_LOGS)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(
			stepWithRequiredAndRecommendedItems(null, Collections.singletonList(PYRE_LOGS)));

		assertEquals(1, rows.size());
		assertEquals(Status.IN_BANK, rows.get(0).getStatus());
	}

	@Test
	public void resolveRecommendedMissing_resolvesToMissing()
	{
		List<RequiredItemDisplay> rows = resolver.resolveRecommended(
			stepWithRequiredAndRecommendedItems(null, Collections.singletonList(SHADE_REMAINS)));

		assertEquals(1, rows.size());
		assertEquals(Status.MISSING, rows.get(0).getStatus());
	}

	/**
	 * Verifies that required and recommended lists are resolved independently and
	 * produce separate display rows with correct statuses each.
	 */
	@Test
	public void resolveRequiredAndRecommendedAreIndependent()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);
		when(bankState.hasItem(PYRE_LOGS)).thenReturn(true);

		GuidanceStep step = stepWithRequiredAndRecommendedItems(
			Collections.singletonList(TINDERBOX),
			Collections.singletonList(PYRE_LOGS));

		List<RequiredItemDisplay> required = resolver.resolve(step);
		List<RequiredItemDisplay> recommended = resolver.resolveRecommended(step);

		assertEquals(1, required.size());
		assertEquals(Status.HELD, required.get(0).getStatus());

		assertEquals(1, recommended.size());
		assertEquals(Status.IN_BANK, recommended.get(0).getStatus());
	}

	// ── resolveIds (B.5.2) ──────────────────────────────────────────────────

	@Test
	public void resolveIdsNullReturnsEmpty()
	{
		assertTrue(resolver.resolveIds(null).isEmpty());
	}

	@Test
	public void resolveIdsEmptyListReturnsEmpty()
	{
		assertTrue(resolver.resolveIds(Collections.emptyList()).isEmpty());
	}

	@Test
	public void resolveIdsFiltersInvalidIds()
	{
		// IDs <= 0 are invalid and must be skipped
		List<RequiredItemDisplay> rows = resolver.resolveIds(Arrays.asList(0, -1, null));
		assertTrue("Invalid item IDs must be skipped", rows.isEmpty());

		// itemManager must never be called for invalid IDs
		verify(itemManager, never()).getItemComposition(0);
	}

	@Test
	public void resolveIdsProducesCorrectRowsForValidIds()
	{
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);

		List<RequiredItemDisplay> rows = resolver.resolveIds(Collections.singletonList(TINDERBOX));

		assertEquals(1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
		assertEquals("Tinderbox", rows.get(0).getName());
		assertEquals(Status.HELD, rows.get(0).getStatus());
	}

	// ── perItemRequiredItemIds override (AC: #417) ─────────────────────────────

	/**
	 * When the coordinator has an active target item that is a key in the step's
	 * perItemRequiredItemIds map, the resolver should use the override list instead
	 * of the static requiredItemIds.
	 */
	@Test
	public void perItemOverride_usedWhenTargetMatchesMapKey()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(BRONZE_LOCK);

		Map<Integer, List<Integer>> perItemMap = new HashMap<>();
		perItemMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, LOAR_REMAINS, LOAR_PYRE_LOGS));
		perItemMap.put(GOLD_LOCK, Arrays.asList(TINDERBOX, 3410, 3446));

		GuidanceStep step = stepWithPerItemMap(
			Collections.singletonList(TINDERBOX), // static fallback
			perItemMap);

		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals("Override list must have 3 items (tinderbox + loar remains + loar pyre logs)",
			3, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
		assertEquals(LOAR_REMAINS, rows.get(1).getItemId());
		assertEquals(LOAR_PYRE_LOGS, rows.get(2).getItemId());
	}

	/**
	 * When the coordinator has no active target item (null), the resolver should
	 * fall back to the step's static requiredItemIds.
	 */
	@Test
	public void perItemOverride_fallsBackWhenNoActiveTarget()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(null);

		Map<Integer, List<Integer>> perItemMap = new HashMap<>();
		perItemMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, LOAR_REMAINS, LOAR_PYRE_LOGS));

		GuidanceStep step = stepWithPerItemMap(
			Collections.singletonList(TINDERBOX), // static list
			perItemMap);

		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals("Must fall back to static required list (1 item)", 1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
	}

	/**
	 * When the coordinator has an active target item but it is NOT a key in the
	 * map, the resolver falls back to the static requiredItemIds.
	 */
	@Test
	public void perItemOverride_fallsBackWhenTargetNotInMap()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(99999);

		Map<Integer, List<Integer>> perItemMap = new HashMap<>();
		perItemMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, LOAR_REMAINS, LOAR_PYRE_LOGS));

		GuidanceStep step = stepWithPerItemMap(
			Collections.singletonList(TINDERBOX), // static list
			perItemMap);

		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals("Must fall back to static list when target ID is not a map key", 1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
	}

	/**
	 * When the step has no perItemRequiredItemIds map (null), the resolver must
	 * always use the static requiredItemIds regardless of the active target.
	 */
	@Test
	public void perItemOverride_fallsBackWhenStepHasNoMap()
	{
		// lenient: resolver short-circuits on null map before reading the target,
		// so this stub may not be exercised — but its INTENT (verify fallback even
		// with a target set) is what the test is asserting.
		lenient().when(coordinator.getActiveTargetItemId()).thenReturn(BRONZE_LOCK);

		// stepWithRequiredItems creates a step with null perItemRequiredItemIds
		GuidanceStep step = stepWithRequiredItems(Collections.singletonList(TINDERBOX));

		List<RequiredItemDisplay> rows = resolver.resolve(step);

		assertEquals("No map on step — must use static required list", 1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
	}

	/**
	 * Verifies that the coordinator reference is optional: resolver with no
	 * coordinator set (null) uses the static requiredItemIds.
	 */
	@Test
	public void perItemOverride_noCoordinator_usesStaticList()
	{
		RequiredItemResolver resolverNoCoord =
			new RequiredItemResolver(inventoryState, bankState, itemManager);
		// deliberately do NOT call setCoordinator

		Map<Integer, List<Integer>> perItemMap = new HashMap<>();
		perItemMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, LOAR_REMAINS));

		GuidanceStep step = stepWithPerItemMap(
			Collections.singletonList(TINDERBOX),
			perItemMap);

		List<RequiredItemDisplay> rows = resolverNoCoord.resolve(step);

		assertEquals("No coordinator wired — must use static required list", 1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
	}


	// ── perItemRecommendedItemIds override (B.5.2) ─────────────────────────────

	/**
	 * When the coordinator has an active target item that is a key in the step's
	 * perItemRecommendedItemIds map, resolveRecommended uses the override list.
	 */
	@Test
	public void perItemRecommendedOverride_usedWhenTargetMatchesMapKey()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(BRONZE_LOCK);

		Map<Integer, List<Integer>> perItemRecMap = new HashMap<>();
		perItemRecMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, PYRE_LOGS));
		perItemRecMap.put(GOLD_LOCK, Collections.singletonList(SHADE_REMAINS));

		GuidanceStep step = stepFullWithPerItemRec(
			null,
			null,
			Collections.singletonList(SHADE_REMAINS),  // static fallback
			perItemRecMap);

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(step);

		assertEquals("Override list must have 2 items for BRONZE_LOCK target", 2, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
		assertEquals(PYRE_LOGS, rows.get(1).getItemId());
	}

	/**
	 * When no active target, resolveRecommended falls back to the static recommendedItemIds.
	 */
	@Test
	public void perItemRecommendedOverride_fallsBackWhenNoActiveTarget()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(null);

		Map<Integer, List<Integer>> perItemRecMap = new HashMap<>();
		perItemRecMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, PYRE_LOGS));

		GuidanceStep step = stepFullWithPerItemRec(
			null, null, Collections.singletonList(SHADE_REMAINS), perItemRecMap);

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(step);

		assertEquals("Must fall back to static recommended list (1 item)", 1, rows.size());
		assertEquals(SHADE_REMAINS, rows.get(0).getItemId());
	}

	/**
	 * When the target is not in the map, falls back to the static recommendedItemIds.
	 */
	@Test
	public void perItemRecommendedOverride_fallsBackWhenTargetNotInMap()
	{
		when(coordinator.getActiveTargetItemId()).thenReturn(99999);

		Map<Integer, List<Integer>> perItemRecMap = new HashMap<>();
		perItemRecMap.put(BRONZE_LOCK, Arrays.asList(TINDERBOX, PYRE_LOGS));

		GuidanceStep step = stepFullWithPerItemRec(
			null, null, Collections.singletonList(SHADE_REMAINS), perItemRecMap);

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(step);

		assertEquals("Must fall back to static recommended list when target not in map",
			1, rows.size());
		assertEquals(SHADE_REMAINS, rows.get(0).getItemId());
	}

	/**
	 * When the step has no perItemRecommendedItemIds map (null), uses the static list.
	 */
	@Test
	public void perItemRecommendedOverride_fallsBackWhenStepHasNoMap()
	{
		lenient().when(coordinator.getActiveTargetItemId()).thenReturn(BRONZE_LOCK);

		GuidanceStep step = stepWithRequiredAndRecommendedItems(
			null, Collections.singletonList(TINDERBOX));

		List<RequiredItemDisplay> rows = resolver.resolveRecommended(step);

		assertEquals("No map on step — must use static recommended list", 1, rows.size());
		assertEquals(TINDERBOX, rows.get(0).getItemId());
	}
	// --- Helpers ---

	private static GuidanceStep stepWithRequiredItems(List<Integer> requiredItemIds)
	{
		return stepWithRequiredAndRecommendedItems(requiredItemIds, null);
	}

	private static GuidanceStep stepWithRequiredAndRecommendedItems(
		List<Integer> requiredItemIds, List<Integer> recommendedItemIds)
	{
		return stepFull(requiredItemIds, null, recommendedItemIds);
	}

	private static GuidanceStep stepWithPerItemMap(
		List<Integer> requiredItemIds, Map<Integer, List<Integer>> perItemMap)
	{
		return stepFull(requiredItemIds, perItemMap, null);
	}

	private static GuidanceStep stepFull(
		List<Integer> requiredItemIds,
		Map<Integer, List<Integer>> perItemRequiredItemIds,
		List<Integer> recommendedItemIds)
	{
		return new GuidanceStep(
			"Test step",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null,
			requiredItemIds,
			perItemRequiredItemIds,
			recommendedItemIds,
			null,  // perItemRecommendedItemIds
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
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null,  // conditionalAlternatives
			null, // section
			null, // waypoints
			null  // dynamicTargetEvaluator
		);
	}

	private static GuidanceStep stepFullWithPerItemRec(
		List<Integer> requiredItemIds,
		Map<Integer, List<Integer>> perItemRequiredItemIds,
		List<Integer> recommendedItemIds,
		Map<Integer, List<Integer>> perItemRecommendedItemIds)
	{
		return new GuidanceStep(
			"Test step",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null,
			requiredItemIds,
			perItemRequiredItemIds,
			recommendedItemIds,
			perItemRecommendedItemIds,
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
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null,  // conditionalAlternatives
			null,  // section
			null,  // waypoints
			null   // dynamicTargetEvaluator
		);
	}
}