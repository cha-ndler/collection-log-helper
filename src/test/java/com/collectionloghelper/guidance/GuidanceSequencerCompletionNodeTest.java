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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.CompletionNode;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link GuidanceSequencer} honours {@link CompletionNode} trees on
 * guidance steps (B1). Separate class so it does not disturb the large existing
 * {@link GuidanceSequencerTest} fixture.
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceSequencerCompletionNodeTest
{
	@Mock
	private PlayerInventoryState inventoryState;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;

	private GuidanceSequencer sequencer;

	@Before
	public void setUp() throws Exception
	{
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasAllItems(org.mockito.ArgumentMatchers.anyList())).thenReturn(true);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker);
	}

	// ========================================================================
	// Tree-driven eager completion
	// ========================================================================

	@Test
	public void allNodeRequiresBothLeavesSatisfied()
	{
		// (INVENTORY_HAS_ITEM item=42) AND (ITEM_OBTAINED item=42)
		GuidanceStep step = stepWithNode(CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED))), 42);
		GuidanceStep trailing = manualStep();

		// Only inventory satisfied — should NOT skip step 1.
		when(inventoryState.hasItemCount(42, 1)).thenReturn(true);
		when(collectionState.isItemObtained(42)).thenReturn(false);
		startSequence(Arrays.asList(step, trailing));
		assertEquals(0, sequencer.getCurrentIndex());
	}

	@Test
	public void allNodeSkipsStepWhenBothLeavesSatisfied()
	{
		GuidanceStep step = stepWithNode(CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED))), 42);
		GuidanceStep trailing = manualStep();

		when(inventoryState.hasItemCount(42, 1)).thenReturn(true);
		when(collectionState.isItemObtained(42)).thenReturn(true);
		startSequence(Arrays.asList(step, trailing));
		assertEquals("Tree AND should advance past step when both leaves true", 1, sequencer.getCurrentIndex());
	}

	@Test
	public void anyNodeSkipsWhenAnyLeafSatisfied()
	{
		GuidanceStep step = stepWithNode(CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED))), 99);
		GuidanceStep trailing = manualStep();

		when(collectionState.isItemObtained(99)).thenReturn(true);
		startSequence(Arrays.asList(step, trailing));
		assertEquals("Tree OR should advance past step when any leaf true", 1, sequencer.getCurrentIndex());
	}

	@Test
	public void anyNodeDoesNotSkipWhenAllLeavesFalse()
	{
		GuidanceStep step = stepWithNode(CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED))), 7);
		GuidanceStep trailing = manualStep();

		// Nothing satisfied.
		startSequence(Arrays.asList(step, trailing));
		assertEquals(0, sequencer.getCurrentIndex());
	}

	@Test
	public void nestedTreeEvaluatesCorrectly()
	{
		// ALL( ITEM_OBTAINED, ANY(INVENTORY_HAS_ITEM, PLAYER_ON_PLANE) )
		CompletionNode tree = CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED),
			CompletionNode.any(Arrays.asList(
				CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
				CompletionNode.leaf(CompletionCondition.PLAYER_ON_PLANE)
			))
		));
		GuidanceStep step = stepWithNode(tree, 123);
		GuidanceStep trailing = manualStep();

		when(collectionState.isItemObtained(123)).thenReturn(true);
		sequencer.setPlayerLocation(new WorldPoint(0, 0, step.getWorldPlane()));
		startSequence(Arrays.asList(step, trailing));

		assertEquals("Tree skips when outer AND satisfied via nested OR via plane match",
			1, sequencer.getCurrentIndex());
	}

	@Test
	public void treeTakesPrecedenceOverLegacyEnum()
	{
		// Step's legacy completionCondition would say "skip (ITEM_OBTAINED true)",
		// but the tree says "need both INVENTORY_HAS_ITEM AND ITEM_OBTAINED" — and
		// inventory is empty. Tree wins, so the step must not be skipped.
		GuidanceStep step = stepWithCustomEnumAndNode(
			CompletionCondition.ITEM_OBTAINED,
			CompletionNode.all(Arrays.asList(
				CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
				CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED))),
			55);
		GuidanceStep trailing = manualStep();

		lenient().when(collectionState.isItemObtained(55)).thenReturn(true);
		lenient().when(inventoryState.hasItemCount(55, 1)).thenReturn(false);
		startSequence(Arrays.asList(step, trailing));

		assertEquals("Node-aware path must override legacy single-enum satisfaction",
			0, sequencer.getCurrentIndex());
	}

	@Test
	public void legacyPathStillWorksWhenNodeAbsent()
	{
		// Sanity check: null completionNode falls through to existing switch.
		GuidanceStep step = stepWithCustomEnumAndNode(
			CompletionCondition.ITEM_OBTAINED, null, 77);
		GuidanceStep trailing = manualStep();

		when(collectionState.isItemObtained(77)).thenReturn(true);
		startSequence(Arrays.asList(step, trailing));

		assertEquals("Legacy enum path must still advance when condition satisfied",
			1, sequencer.getCurrentIndex());
	}

	// ========================================================================
	// Helpers
	// ========================================================================

	private GuidanceStep stepWithNode(CompletionNode node, int itemId)
	{
		return stepWithCustomEnumAndNode(CompletionCondition.MANUAL, node, itemId);
	}

	private GuidanceStep stepWithCustomEnumAndNode(CompletionCondition legacy, CompletionNode node, int itemId)
	{
		return new GuidanceStep(
			"Step with tree",
			0, 0, 0,
			0, null, null,
			null, null,
			legacy,
			itemId, 1, 0, 0,
			null,                          // completionNpcIds
			null,                          // worldMessage
			0, null, null,                 // objectId, objectIds, objectInteractAction
			null, null,                    // highlightItemIds, groundItemIds
			null,                          // completionChatPattern
			0, 0,                          // completionVarbitId, completionVarbitValue
			false,                         // useItemOnObject
			0,                             // objectMaxDistance
			null,                          // objectFilterTiles
			null,                          // highlightWidgetIds
			0, 0,                          // loopBackToStep, loopCount
			null,                          // skipIfHasAnyItemIds
			null,                          // dynamicItemObjectTiers
			null,                          // completionZone
			null,                          // conditionalAlternatives
			node                           // completionNode
		);
	}

	private GuidanceStep manualStep()
	{
		return stepWithCustomEnumAndNode(CompletionCondition.MANUAL, null, 0);
	}

	private void startSequence(List<GuidanceStep> steps)
	{
		CollectionLogSource source = new CollectionLogSource(
			"Test Source", CollectionLogCategory.BOSSES, 3000, 3000, 0,
			60, 0, "Test Source", Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			steps, null, 0, null, 0, Collections.emptyList());
		sequencer.startSequence(source, s -> {}, () -> {});
	}
}
