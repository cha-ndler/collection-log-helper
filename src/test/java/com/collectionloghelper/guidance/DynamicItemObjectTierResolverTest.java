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
import com.collectionloghelper.data.ItemObjectTier;
import com.collectionloghelper.data.PlayerInventoryState;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link DynamicItemObjectTierResolver}. Pins the tier-resolution
 * contract extracted from {@code GuidanceOverlayCoordinator.applyDynamicItemObjectOverlays}:
 *
 * <ul>
 *   <li>No tiers configured -- {@link DynamicItemObjectTierResolver.Result#EMPTY}
 *       singleton (no allocation, no inventory probes).</li>
 *   <li>Single tier with required item present -- result mirrors that tier's
 *       object/item ids and uses the tier-specific tooltip ({@code "<action> <name>"}).</li>
 *   <li>Multiple tiers matching -- object/item ids merged; tooltip falls back to
 *       the step description (matches legacy 2-tier branch).</li>
 *   <li>No tier's items in inventory -- {@link DynamicItemObjectTierResolver.Result#EMPTY}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicItemObjectTierResolverTest
{
	private static final String STEP_DESC = "Loot the chest";

	@Mock
	private PlayerInventoryState playerInventoryState;

	private DynamicItemObjectTierResolver resolver;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<DynamicItemObjectTierResolver> ctor =
			DynamicItemObjectTierResolver.class.getDeclaredConstructor(PlayerInventoryState.class);
		ctor.setAccessible(true);
		resolver = ctor.newInstance(playerInventoryState);
	}

	@Test
	public void resolve_nullStep_returnsEmptySingletonAndProbesNothing()
	{
		DynamicItemObjectTierResolver.Result result = resolver.resolve(null);

		assertSame(
			DynamicItemObjectTierResolver.Result.EMPTY, result,"null step must return the EMPTY singleton (no allocation)");
		assertFalse(result.hasMatch());
		verifyNoInteractions(playerInventoryState);
	}

	@Test
	public void resolve_stepWithoutDynamicTiers_returnsEmptySingleton()
	{
		GuidanceStep step = stepWithTiers(null);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertSame(DynamicItemObjectTierResolver.Result.EMPTY, result);
		verifyNoInteractions(playerInventoryState);
	}

	@Test
	public void resolve_stepWithEmptyTiersList_returnsEmptySingleton()
	{
		GuidanceStep step = stepWithTiers(Collections.emptyList());

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertSame(DynamicItemObjectTierResolver.Result.EMPTY, result);
		verifyNoInteractions(playerInventoryState);
	}

	@Test
	public void resolve_singleTierItemPresent_returnsTierObjectsAndTooltip()
	{
		ItemObjectTier bronze = new ItemObjectTier(
			"Bronze",
			Arrays.asList(101, 102),
			Arrays.asList(2001, 2002),
			"Open");
		GuidanceStep step = stepWithTiers(Collections.singletonList(bronze));
		when(playerInventoryState.hasItem(101)).thenReturn(true);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertTrue(result.hasMatch());
		assertEquals(2, result.getObjectIds().size());
		assertTrue(result.getObjectIds().containsAll(Arrays.asList(2001, 2002)));
		assertEquals(Collections.singletonList(101), result.getItemIds());
		assertEquals("Open", result.getAction());
		assertEquals("Open Bronze", result.getTooltipText());
	}

	@Test
	public void resolve_singleTierFirstItemMatches_breaksAfterFirstKey()
	{
		// Tier has two item IDs; first matches in inventory. Loop must break after
		// the first match (legacy "Only match first key per tier (avoid duplicates)").
		ItemObjectTier tier = new ItemObjectTier(
			"Steel",
			Arrays.asList(201, 202),
			Collections.singletonList(3001),
			"Use");
		GuidanceStep step = stepWithTiers(Collections.singletonList(tier));
		when(playerInventoryState.hasItem(201)).thenReturn(true);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertTrue(result.hasMatch());
		assertEquals(Collections.singletonList(201), result.getItemIds());
	}

	@Test
	public void resolve_multipleTiersMatching_mergesObjectsAndUsesStepDescription()
	{
		// Bronze + Steel both have a matching item -- merged object set, tooltip
		// falls back to step description (legacy: matchedItemIds.size() > 1).
		ItemObjectTier bronze = new ItemObjectTier(
			"Bronze",
			Collections.singletonList(101),
			Collections.singletonList(2001),
			"Open");
		ItemObjectTier steel = new ItemObjectTier(
			"Steel",
			Collections.singletonList(201),
			Collections.singletonList(3001),
			"Open");
		GuidanceStep step = stepWithTiers(Arrays.asList(bronze, steel));
		when(playerInventoryState.hasItem(101)).thenReturn(true);
		when(playerInventoryState.hasItem(201)).thenReturn(true);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertTrue(result.hasMatch());
		assertEquals(2, result.getObjectIds().size());
		assertTrue(result.getObjectIds().containsAll(Arrays.asList(2001, 3001)));
		assertEquals(Arrays.asList(101, 201), result.getItemIds());
		assertEquals("Open", result.getAction());
		// Multi-tier match: tooltip falls back to step description, not "<action> <name>"
		assertEquals(STEP_DESC, result.getTooltipText());
	}

	@Test
	public void resolve_noTierItemInInventory_returnsEmptySingleton()
	{
		ItemObjectTier tier = new ItemObjectTier(
			"Bronze",
			Collections.singletonList(101),
			Collections.singletonList(2001),
			"Open");
		GuidanceStep step = stepWithTiers(Collections.singletonList(tier));
		// hasItem(101) is false by default -- nothing matches.

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertSame(
			DynamicItemObjectTierResolver.Result.EMPTY, result,"no-match path must return the EMPTY singleton (no allocation)");
		assertFalse(result.hasMatch());
	}

	@Test
	public void resolve_tierWithNullItemIds_isSkipped()
	{
		// Defensive: a malformed tier with null itemIds must not throw and must
		// not prevent later tiers from matching.
		ItemObjectTier malformed = new ItemObjectTier("Empty", null, null, null);
		ItemObjectTier good = new ItemObjectTier(
			"Steel",
			Collections.singletonList(201),
			Collections.singletonList(3001),
			"Use");
		GuidanceStep step = stepWithTiers(Arrays.asList(malformed, good));
		when(playerInventoryState.hasItem(201)).thenReturn(true);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertTrue(result.hasMatch());
		assertEquals(Collections.singletonList(3001),
			Arrays.asList(result.getObjectIds().toArray(new Integer[0])));
		assertEquals(Collections.singletonList(201), result.getItemIds());
	}

	@Test
	public void resolve_tierActionNull_fallsBackToStepObjectInteractAction()
	{
		// Tier has no interactAction -- falls back to step.objectInteractAction.
		ItemObjectTier tier = new ItemObjectTier(
			"Gold",
			Collections.singletonList(301),
			Collections.singletonList(4001),
			null);
		GuidanceStep step = stepWithTiersAndAction(
			Collections.singletonList(tier), "Search");
		when(playerInventoryState.hasItem(301)).thenReturn(true);

		DynamicItemObjectTierResolver.Result result = resolver.resolve(step);

		assertTrue(result.hasMatch());
		assertEquals("Search", result.getAction());
		assertEquals("Search Gold", result.getTooltipText());
	}

	@Test
	public void resultEmpty_isAllocationFreeNoOp()
	{
		// The EMPTY singleton invariants the coordinator relies on: hasMatch=false
		// short-circuits before any overlay setter fires, and the getters return
		// non-null empty collections so a misuse can't throw NPE.
		DynamicItemObjectTierResolver.Result empty = DynamicItemObjectTierResolver.Result.EMPTY;

		assertFalse(empty.hasMatch());
		assertTrue(empty.getObjectIds().isEmpty());
		assertTrue(empty.getItemIds().isEmpty());
		assertNull(empty.getAction());
		assertNull(empty.getTooltipText());
	}

	// -- helpers --

	private static GuidanceStep stepWithTiers(List<ItemObjectTier> tiers)
	{
		return stepWithTiersAndAction(tiers, "Open");
	}

	private static GuidanceStep stepWithTiersAndAction(
		List<ItemObjectTier> tiers, String objectInteractAction)
	{
		return new GuidanceStep(
			STEP_DESC,
			null,           // perItemStepDescription
			0, 0, 0,       // worldX, worldY, worldPlane
			0, null, null, null, // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,    // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,           // completionNpcIds
			null,           // worldMessage
			0, null, objectInteractAction,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			0, 0,           // completionVarbitId, completionVarbitValue
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			null,           // highlightWidgetIds
			0, 0,           // loopBackToStep, loopCount
			null,           // skipIfHasAnyItemIds
			tiers,          // dynamicItemObjectTiers
			null,           // completionZone
			null,           // conditionalAlternatives
			null,           // section
			null,           // waypoints
			null,            // dynamicTargetEvaluator
			null            // conditionTree
		);
	}
}
