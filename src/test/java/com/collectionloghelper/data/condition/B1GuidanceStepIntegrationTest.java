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
package com.collectionloghelper.data.condition;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 Phase 1+2 integration: synthetic {@link GuidanceStep} carrying a
 * non-null {@code conditionTree}. Verifies the contract Phase 3 will
 * dispatch against:
 * <ul>
 *   <li>{@link GuidanceStep#getConditionTree()} round-trips the tree.</li>
 *   <li>The tree's {@link ConditionNode#evaluate} drives a true/false answer
 *       independent of the legacy flat {@code completionCondition} enum.</li>
 *   <li>Legacy fields remain populated and accessible on the same step, so
 *       Phase 3 can prefer the tree while keeping the flat enum as a
 *       fall-through.</li>
 *   <li>A step authored as JSON (full inline shape) deserialises with the
 *       tree present and the flat enum unchanged.</li>
 * </ul>
 */
public class B1GuidanceStepIntegrationTest
{
	private final Gson gson = new GsonBuilder()
		.registerTypeAdapter(ConditionNode.class, new ConditionNodeDeserializer())
		.create();

	// -- Synthetic step with conditionTree set --------------------------------

	@Test
	public void conditionTree_wins_inventoryHasBothCoinsAndShark() throws Exception
	{
		ConditionNode tree = ConditionNodes.and(
			ConditionNodes.inventoryHasItem(995, 1),
			ConditionNodes.inventoryHasItem(385, 1));

		GuidanceStep step = makeStepWithTree(tree, CompletionCondition.MANUAL);

		// Tree round-trips.
		assertNotNull(step.getConditionTree());
		assertEquals(tree, step.getConditionTree());

		// Legacy field is still observable (Phase 3 dispatcher uses this when
		// the tree is null).
		assertEquals(CompletionCondition.MANUAL, step.getCompletionCondition());

		// Tree drives the actual answer when wired into evaluation.
		PlayerInventoryState inv = newInventory();
		seedInventory(inv, Map.of(995, 1, 385, 1));
		ConditionEvaluationContext ctx = ConditionEvaluationContext.stateOnly(
			inv, newCollection(), null);
		assertTrue(step.getConditionTree().evaluate(ctx),
			"tree should be satisfied when both items present");

		seedInventory(inv, Map.of(995, 1));
		assertFalse(step.getConditionTree().evaluate(ctx),
			"tree should be unsatisfied when only coins present");
	}

	@Test
	public void conditionTree_disagreesFromLegacyEnum_treeIsOwnAuthority() throws Exception
	{
		// Legacy enum says ITEM_OBTAINED of item 99999 (player has not obtained).
		// Tree says: OR(inventory_has 995, manual=false). With coins, OR is true.
		// The point is the tree's truth value is independent of the legacy enum,
		// proving Phase 3 can swap dispatch source without coupling the two.
		ConditionNode tree = ConditionNodes.or(
			ConditionNodes.inventoryHasItem(995, 1),
			ConditionNodes.manual());

		GuidanceStep step = makeStepWithTree(tree, CompletionCondition.ITEM_OBTAINED);

		PlayerInventoryState inv = newInventory();
		seedInventory(inv, Map.of(995, 50));
		ConditionEvaluationContext ctx = ConditionEvaluationContext.stateOnly(
			inv, newCollection(), null);

		assertTrue(step.getConditionTree().evaluate(ctx),
			"tree (OR with inventory) should be true even though the legacy ITEM_OBTAINED would be false");
	}

	@Test
	public void legacyStep_noConditionTree_stayssNull() throws Exception
	{
		GuidanceStep step = makeStepWithTree(null, CompletionCondition.MANUAL);
		assertNull(step.getConditionTree(),
			"a step authored without conditionTree must keep the field null - this is the "
				+ "default behaviour for all 225 existing sources");
	}

	// -- Authored-as-JSON round-trip ------------------------------------------

	@Test
	public void inlineJson_withConditionTree_deserialisesWithBothFields() throws Exception
	{
		// Minimal JSON that mirrors how a future Phase 3 pilot step would be
		// authored: flat enum kept for backwards-compat, tree added on top.
		String json = "{"
			+ "\"description\":\"Synthetic test step\","
			+ "\"completionCondition\":\"MANUAL\","
			+ "\"conditionTree\":{\"and\":[{\"inventory_has_item\":995},{\"player_on_plane\":0}]}"
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull(step);
		assertEquals(CompletionCondition.MANUAL, step.getCompletionCondition());
		assertNotNull(step.getConditionTree(),
			"conditionTree must deserialise into a ConditionNode tree via the registered adapter");
		assertTrue(step.getConditionTree() instanceof AndNode,
			"root must be AndNode; got " + step.getConditionTree().getClass().getSimpleName());
		assertEquals(2, ((AndNode) step.getConditionTree()).getChildren().size());
	}

	@Test
	public void inlineJson_withoutConditionTree_keepsFieldNull()
	{
		String json = "{\"description\":\"legacy step\",\"completionCondition\":\"MANUAL\"}";
		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull(step);
		assertNull(step.getConditionTree(),
			"a step authored without the conditionTree key must keep the field null");
		assertEquals(CompletionCondition.MANUAL, step.getCompletionCondition());
	}

	// -- Helpers --------------------------------------------------------------

	private static GuidanceStep makeStepWithTree(ConditionNode tree, CompletionCondition flat)
	{
		return new GuidanceStep(
			"Synthetic step",
			null, 0, 0, 0,
			0, null, null, null, null, null, null, null, null,
			flat,
			0, 0, 0, 0, null, null,
			0, null, null, null, null, null,
			0, 0, false, 0, null, null, 0, 0, null, null, null, null, null,
			null /* waypoints */, null /* dynamicTargetEvaluator */,
			tree,
						null, /* perItemStepPriority */
						null  // activityObtainableItemIds
		
			, null // restockIfMissingAllItemIds (#719)
		);
	}

	private static PlayerInventoryState newInventory() throws Exception
	{
		Constructor<PlayerInventoryState> c = PlayerInventoryState.class.getDeclaredConstructor();
		c.setAccessible(true);
		return c.newInstance();
	}

	private static PlayerCollectionState newCollection() throws Exception
	{
		Constructor<?> c = PlayerCollectionState.class.getDeclaredConstructors()[0];
		c.setAccessible(true);
		Object[] args = new Object[c.getParameterCount()];
		return (PlayerCollectionState) c.newInstance(args);
	}

	private static void seedInventory(PlayerInventoryState inv, Map<Integer, Integer> idsAndCounts) throws Exception
	{
		Class<?> snapshotClass = null;
		for (Class<?> declared : PlayerInventoryState.class.getDeclaredClasses())
		{
			if ("InventorySnapshot".equals(declared.getSimpleName()))
			{
				snapshotClass = declared;
				break;
			}
		}
		if (snapshotClass == null)
		{
			throw new IllegalStateException("InventorySnapshot nested class not found");
		}
		Constructor<?> sctor = snapshotClass.getDeclaredConstructor(Set.class, Map.class);
		sctor.setAccessible(true);
		Set<Integer> ids = new HashSet<>(idsAndCounts.keySet());
		Object snapshot = sctor.newInstance(ids, new HashMap<>(idsAndCounts));

		Field f = PlayerInventoryState.class.getDeclaredField("snapshot");
		f.setAccessible(true);
		f.set(inv, snapshot);
	}
}
