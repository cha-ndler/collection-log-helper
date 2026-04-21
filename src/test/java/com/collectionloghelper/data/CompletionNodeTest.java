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
package com.collectionloghelper.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the CompletionNode discriminated union (B1). The tests use a
 * simple Set-backed "truthy conditions" predicate to exercise the tree walk
 * without touching real sequencer state.
 */
public class CompletionNodeTest
{
	private static Predicate<CompletionCondition> truthyWhenIn(Set<CompletionCondition> truthy)
	{
		return truthy::contains;
	}

	private static Set<CompletionCondition> set(CompletionCondition... entries)
	{
		return new HashSet<>(Arrays.asList(entries));
	}

	// ========================================================================
	// Leaf
	// ========================================================================

	@Test
	public void leaf_evaluatesViaPredicate()
	{
		CompletionNode leaf = CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED);
		assertTrue(leaf.evaluate(truthyWhenIn(set(CompletionCondition.ITEM_OBTAINED))));
		assertFalse(leaf.evaluate(truthyWhenIn(set(CompletionCondition.MANUAL))));
	}

	@Test
	public void leaf_kindIsLeaf()
	{
		assertEquals(CompletionNode.Kind.LEAF, CompletionNode.leaf(CompletionCondition.MANUAL).getKind());
	}

	@Test(expected = IllegalArgumentException.class)
	public void leaf_nullConditionRejected()
	{
		CompletionNode.leaf(null);
	}

	// ========================================================================
	// All (AND)
	// ========================================================================

	@Test
	public void all_trueWhenEveryChildTrue()
	{
		CompletionNode node = CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED),
			CompletionNode.leaf(CompletionCondition.PLAYER_ON_PLANE)
		));
		assertTrue(node.evaluate(truthyWhenIn(
			set(CompletionCondition.ITEM_OBTAINED, CompletionCondition.PLAYER_ON_PLANE))));
	}

	@Test
	public void all_falseWhenAnyChildFalse()
	{
		CompletionNode node = CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED),
			CompletionNode.leaf(CompletionCondition.PLAYER_ON_PLANE)
		));
		assertFalse(node.evaluate(truthyWhenIn(set(CompletionCondition.ITEM_OBTAINED))));
	}

	@Test
	public void all_emptyIsVacuouslyTrue()
	{
		assertTrue(CompletionNode.all(Collections.emptyList())
			.evaluate(truthyWhenIn(Collections.emptySet())));
	}

	@Test
	public void all_shortCircuitsOnFirstFalse()
	{
		final boolean[] secondEvaluated = {false};
		CompletionNode node = CompletionNode.all(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.MANUAL),
			new CompletionNode()
			{
				@Override
				public Kind getKind()
				{
					return Kind.LEAF;
				}

				@Override
				public boolean evaluate(Predicate<CompletionCondition> p)
				{
					secondEvaluated[0] = true;
					return true;
				}
			}
		));
		assertFalse(node.evaluate(truthyWhenIn(set(CompletionCondition.ITEM_OBTAINED))));
		assertFalse("All should not evaluate children after first false", secondEvaluated[0]);
	}

	// ========================================================================
	// Any (OR)
	// ========================================================================

	@Test
	public void any_trueWhenAnyChildTrue()
	{
		CompletionNode node = CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.MANUAL),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED)
		));
		assertTrue(node.evaluate(truthyWhenIn(set(CompletionCondition.ITEM_OBTAINED))));
	}

	@Test
	public void any_falseWhenAllChildrenFalse()
	{
		CompletionNode node = CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.MANUAL),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED)
		));
		assertFalse(node.evaluate(truthyWhenIn(set(CompletionCondition.PLAYER_ON_PLANE))));
	}

	@Test
	public void any_emptyIsVacuouslyFalse()
	{
		assertFalse(CompletionNode.any(Collections.emptyList())
			.evaluate(truthyWhenIn(Collections.emptySet())));
	}

	@Test
	public void any_shortCircuitsOnFirstTrue()
	{
		final boolean[] secondEvaluated = {false};
		CompletionNode node = CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED),
			new CompletionNode()
			{
				@Override
				public Kind getKind()
				{
					return Kind.LEAF;
				}

				@Override
				public boolean evaluate(Predicate<CompletionCondition> p)
				{
					secondEvaluated[0] = true;
					return false;
				}
			}
		));
		assertTrue(node.evaluate(truthyWhenIn(set(CompletionCondition.ITEM_OBTAINED))));
		assertFalse("Any should not evaluate children after first true", secondEvaluated[0]);
	}

	// ========================================================================
	// Nested branches
	// ========================================================================

	@Test
	public void nested_allOfAny_andVice_versa()
	{
		// (A OR B) AND (C OR D)
		CompletionNode inner1 = CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED),
			CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM)
		));
		CompletionNode inner2 = CompletionNode.any(Arrays.asList(
			CompletionNode.leaf(CompletionCondition.PLAYER_ON_PLANE),
			CompletionNode.leaf(CompletionCondition.ARRIVE_AT_ZONE)
		));
		CompletionNode root = CompletionNode.all(Arrays.asList(inner1, inner2));

		assertTrue(root.evaluate(truthyWhenIn(
			set(CompletionCondition.INVENTORY_HAS_ITEM, CompletionCondition.ARRIVE_AT_ZONE))));
		assertFalse("Fails when second branch has no truthy child", root.evaluate(truthyWhenIn(
			set(CompletionCondition.ITEM_OBTAINED))));
	}

	// ========================================================================
	// Defensive copies & immutability
	// ========================================================================

	@Test
	public void all_defensiveCopyOfChildren()
	{
		java.util.ArrayList<CompletionNode> children = new java.util.ArrayList<>();
		children.add(CompletionNode.leaf(CompletionCondition.MANUAL));
		CompletionNode.All node = CompletionNode.all(children);
		children.add(CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED));

		assertEquals("Node must not see mutations to the caller's list", 1, node.getChildren().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void all_nullEntryRejected()
	{
		CompletionNode.all(Arrays.asList(CompletionNode.leaf(CompletionCondition.MANUAL), null));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void all_getChildrenIsUnmodifiable()
	{
		CompletionNode.All node = CompletionNode.all(Collections.singletonList(
			CompletionNode.leaf(CompletionCondition.MANUAL)));
		node.getChildren().add(CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED));
	}

	@Test
	public void equals_and_hashCode_byStructure()
	{
		List<CompletionNode> kidsA = Arrays.asList(
			CompletionNode.leaf(CompletionCondition.MANUAL),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED));
		List<CompletionNode> kidsB = Arrays.asList(
			CompletionNode.leaf(CompletionCondition.MANUAL),
			CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED));
		assertEquals(CompletionNode.all(kidsA), CompletionNode.all(kidsB));
		assertEquals(CompletionNode.all(kidsA).hashCode(), CompletionNode.all(kidsB).hashCode());
		assertNotEquals(CompletionNode.all(kidsA), CompletionNode.any(kidsB));
	}
}
