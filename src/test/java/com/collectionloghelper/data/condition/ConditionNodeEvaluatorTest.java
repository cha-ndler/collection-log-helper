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

import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boolean truth-table verification for {@link ConditionNode#evaluate}.
 *
 * <p>Builds reflective {@link PlayerInventoryState} / {@link PlayerCollectionState}
 * snapshots so leaves operate against a real state object, then verifies AND/OR/NOT
 * short-circuiting and the vacuous-empty cases.
 */
public class ConditionNodeEvaluatorTest
{
	private PlayerInventoryState inventory;
	private PlayerCollectionState collection;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<PlayerInventoryState> invCtor = PlayerInventoryState.class.getDeclaredConstructor();
		invCtor.setAccessible(true);
		inventory = invCtor.newInstance();

		// Use the single-arg test constructor on PlayerCollectionState; fall back to a no-arg
		// reflective build if not present. We only need a working isItemObtained() in this test.
		collection = newCollectionState();
	}

	private static PlayerCollectionState newCollectionState() throws Exception
	{
		// PlayerCollectionState's @Inject constructor takes (Client, ConfigManager, DropRateDatabase).
		// All branches we exercise (isItemObtained, markItemObtained) only touch the obtainedItemIds
		// set, so passing nulls here is safe for this leaf-evaluation test.
		Constructor<?> c = PlayerCollectionState.class.getDeclaredConstructors()[0];
		c.setAccessible(true);
		Object[] args = new Object[c.getParameterCount()];
		return (PlayerCollectionState) c.newInstance(args);
	}

	private static void seedInventory(PlayerInventoryState inv, Map<Integer, Integer> idsAndCounts) throws Exception
	{
		// Build the inner InventorySnapshot via reflection so the test does not depend
		// on the Item / ItemContainer mocking surface.
		Class<?> snapshotClass = Class.forName("com.collectionloghelper.data.PlayerInventoryState$InventorySnapshot");
		Constructor<?> sctor = snapshotClass.getDeclaredConstructor(Set.class, Map.class);
		sctor.setAccessible(true);
		Set<Integer> ids = new HashSet<>(idsAndCounts.keySet());
		Object snapshot = sctor.newInstance(ids, new HashMap<>(idsAndCounts));

		Field snapField = PlayerInventoryState.class.getDeclaredField("snapshot");
		snapField.setAccessible(true);
		snapField.set(inv, snapshot);
	}

	private static void seedObtained(PlayerCollectionState state, int... itemIds) throws Exception
	{
		Field f = PlayerCollectionState.class.getDeclaredField("obtainedItemIds");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Set<Integer> set = (Set<Integer>) f.get(state);
		for (int id : itemIds)
		{
			set.add(id);
		}
	}

	private ConditionEvaluationContext ctx()
	{
		return ConditionEvaluationContext.stateOnly(inventory, collection, null);
	}

	private ConditionEvaluationContext ctxAt(WorldPoint p)
	{
		return ConditionEvaluationContext.stateOnly(inventory, collection, p);
	}

	// -- Leaves ---------------------------------------------------------------

	@Test
	public void leaf_inventoryHasItem_satisfied() throws Exception
	{
		seedInventory(inventory, Map.of(995, 50000));
		assertTrue(ConditionNodes.inventoryHasItem(995, 1).evaluate(ctx()));
		assertTrue(ConditionNodes.inventoryHasItem(995, 1000).evaluate(ctx()));
	}

	@Test
	public void leaf_inventoryHasItem_insufficientCount() throws Exception
	{
		seedInventory(inventory, Map.of(995, 5));
		assertFalse(ConditionNodes.inventoryHasItem(995, 100).evaluate(ctx()));
	}

	@Test
	public void leaf_inventoryHasItem_missingItem()
	{
		assertFalse(ConditionNodes.inventoryHasItem(995, 1).evaluate(ctx()));
	}

	@Test
	public void leaf_inventoryNotHasItem_trueWhenMissing()
	{
		assertTrue(ConditionNodes.inventoryNotHasItem(995).evaluate(ctx()));
	}

	@Test
	public void leaf_inventoryNotHasItem_falseWhenPresent() throws Exception
	{
		seedInventory(inventory, Map.of(995, 1));
		assertFalse(ConditionNodes.inventoryNotHasItem(995).evaluate(ctx()));
	}

	@Test
	public void leaf_itemObtained_fromCollectionState() throws Exception
	{
		seedObtained(collection, 12345);
		assertTrue(ConditionNodes.itemObtained(12345).evaluate(ctx()));
	}

	@Test
	public void leaf_itemObtained_fromInFlightEvent()
	{
		ConditionEvaluationContext c = new ConditionEvaluationContext(
			inventory, collection, null, 12345, -1, -1, null, -1, 0);
		assertTrue(ConditionNodes.itemObtained(12345).evaluate(c));
	}

	@Test
	public void leaf_arriveAtTile_withinRadius()
	{
		WorldPoint here = new WorldPoint(3200, 3200, 0);
		assertTrue(ConditionNodes.arriveAtTile(3201, 3199, 0, 5).evaluate(ctxAt(here)));
	}

	@Test
	public void leaf_arriveAtTile_wrongPlane()
	{
		WorldPoint here = new WorldPoint(3200, 3200, 0);
		assertFalse(ConditionNodes.arriveAtTile(3200, 3200, 1, 5).evaluate(ctxAt(here)));
	}

	@Test
	public void leaf_playerOnPlane()
	{
		WorldPoint upstairs = new WorldPoint(3200, 3200, 2);
		assertTrue(ConditionNodes.playerOnPlane(2).evaluate(ctxAt(upstairs)));
		assertFalse(ConditionNodes.playerOnPlane(0).evaluate(ctxAt(upstairs)));
	}

	@Test
	public void leaf_actorDeath_primaryId()
	{
		ConditionEvaluationContext c = new ConditionEvaluationContext(
			inventory, collection, null, -1, 2042, -1, null, -1, 0);
		assertTrue(ConditionNodes.actorDeath(2042).evaluate(c));
	}

	@Test
	public void leaf_actorDeath_alternateId()
	{
		ConditionEvaluationContext c = new ConditionEvaluationContext(
			inventory, collection, null, -1, 2044, -1, null, -1, 0);
		assertTrue(ConditionNodes.actorDeath(2042, Arrays.asList(2043, 2044)).evaluate(c));
	}

	@Test
	public void leaf_chatMessage_regexMatch()
	{
		ConditionEvaluationContext c = new ConditionEvaluationContext(
			inventory, collection, null, -1, -1, -1,
			"You have completed the quest!", -1, 0);
		assertTrue(ConditionNodes.chatMessageReceived("You have completed.*").evaluate(c));
	}

	@Test
	public void leaf_varbitAtLeast_idMatchesAndValueCrosses()
	{
		ConditionEvaluationContext c = new ConditionEvaluationContext(
			inventory, collection, null, -1, -1, -1, null, 4567, 10);
		assertTrue(ConditionNodes.varbitAtLeast(4567, 5).evaluate(c));
		assertFalse(ConditionNodes.varbitAtLeast(4567, 50).evaluate(c));
		assertFalse(ConditionNodes.varbitAtLeast(9999, 5).evaluate(c));
	}

	@Test
	public void leaf_manual_alwaysFalse()
	{
		assertFalse(ConditionNodes.manual().evaluate(ctx()));
	}

	// -- AND truth table ------------------------------------------------------

	@Test
	public void and_allTrue_true() throws Exception
	{
		seedInventory(inventory, Map.of(995, 100, 4151, 1));
		assertTrue(ConditionNodes.and(
			ConditionNodes.inventoryHasItem(995, 1),
			ConditionNodes.inventoryHasItem(4151, 1)).evaluate(ctx()));
	}

	@Test
	public void and_anyFalse_false() throws Exception
	{
		seedInventory(inventory, Map.of(995, 100));
		assertFalse(ConditionNodes.and(
			ConditionNodes.inventoryHasItem(995, 1),
			ConditionNodes.inventoryHasItem(4151, 1)).evaluate(ctx()));
	}

	@Test
	public void and_emptyChildren_vacuouslyTrue()
	{
		assertTrue(new AndNode(Collections.emptyList()).evaluate(ctx()));
	}

	@Test
	public void and_shortCircuitsOnFirstFalse()
	{
		final boolean[] secondCalled = {false};
		ConditionNode tripwire = c -> { secondCalled[0] = true; return true; };
		ConditionNode falsy = c -> false;
		new AndNode(Arrays.asList(falsy, tripwire)).evaluate(ctx());
		assertFalse(secondCalled[0], "AND must short-circuit after first false child");
	}

	// -- OR truth table -------------------------------------------------------

	@Test
	public void or_anyTrue_true() throws Exception
	{
		seedInventory(inventory, Map.of(995, 100));
		assertTrue(ConditionNodes.or(
			ConditionNodes.inventoryHasItem(4151, 1),
			ConditionNodes.inventoryHasItem(995, 1)).evaluate(ctx()));
	}

	@Test
	public void or_allFalse_false()
	{
		assertFalse(ConditionNodes.or(
			ConditionNodes.inventoryHasItem(4151, 1),
			ConditionNodes.inventoryHasItem(995, 1)).evaluate(ctx()));
	}

	@Test
	public void or_emptyChildren_vacuouslyFalse()
	{
		assertFalse(new OrNode(Collections.emptyList()).evaluate(ctx()));
	}

	@Test
	public void or_shortCircuitsOnFirstTrue()
	{
		final boolean[] secondCalled = {false};
		ConditionNode tripwire = c -> { secondCalled[0] = true; return true; };
		ConditionNode truthy = c -> true;
		new OrNode(Arrays.asList(truthy, tripwire)).evaluate(ctx());
		assertFalse(secondCalled[0], "OR must short-circuit after first true child");
	}

	// -- NOT truth table ------------------------------------------------------

	@Test
	public void not_invertsChildTrue()
	{
		assertFalse(ConditionNodes.not(c -> true).evaluate(ctx()));
	}

	@Test
	public void not_invertsChildFalse()
	{
		assertTrue(ConditionNodes.not(c -> false).evaluate(ctx()));
	}

	@Test
	public void not_nullChild_returnsTrue()
	{
		assertTrue(new NotNode(null).evaluate(ctx()));
	}

	// -- Nested boolean expression --------------------------------------------

	@Test
	public void nested_andOfOrAndNot_consistent() throws Exception
	{
		seedInventory(inventory, Map.of(995, 1000));
		// (has-coins AND NOT has-shark) OR has-rune-scim -> true (first conjunct true)
		ConditionNode expr = ConditionNodes.or(
			ConditionNodes.and(
				ConditionNodes.inventoryHasItem(995, 1),
				ConditionNodes.not(ConditionNodes.inventoryHasItem(385, 1))),
			ConditionNodes.inventoryHasItem(1333, 1));
		assertTrue(expr.evaluate(ctx()));
	}
}
