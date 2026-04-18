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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Round-trip tests for {@link CompletionNodeAdapter}. Exercises the three
 * accepted input shapes (legacy string, explicit leaf object, branching
 * all/any object) plus error cases.
 */
public class CompletionNodeAdapterTest
{
	private Gson gson;

	@Before
	public void setUp()
	{
		gson = new GsonBuilder()
			.registerTypeAdapter(CompletionNode.class, new CompletionNodeAdapter())
			.create();
	}

	// ========================================================================
	// Legacy string form (the shape every production source uses)
	// ========================================================================

	@Test
	public void legacyStringCoercedToLeaf()
	{
		CompletionNode node = gson.fromJson("\"ITEM_OBTAINED\"", CompletionNode.class);
		assertEquals(CompletionNode.Kind.LEAF, node.getKind());
		assertEquals(CompletionCondition.ITEM_OBTAINED, ((CompletionNode.Leaf) node).getCondition());
	}

	@Test
	public void legacyStringCaseInsensitive()
	{
		CompletionNode node = gson.fromJson("\"manual\"", CompletionNode.class);
		assertEquals(CompletionCondition.MANUAL, ((CompletionNode.Leaf) node).getCondition());
	}

	@Test(expected = JsonParseException.class)
	public void unknownConditionRejected()
	{
		gson.fromJson("\"NOT_A_REAL_CONDITION\"", CompletionNode.class);
	}

	// ========================================================================
	// Explicit leaf object form
	// ========================================================================

	@Test
	public void explicitLeafObject()
	{
		String json = "{\"op\": \"leaf\", \"condition\": \"INVENTORY_HAS_ITEM\"}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionNode.Kind.LEAF, node.getKind());
		assertEquals(CompletionCondition.INVENTORY_HAS_ITEM, ((CompletionNode.Leaf) node).getCondition());
	}

	@Test
	public void implicitLeafFromConditionField()
	{
		// Authoring convenience: an object with just { "condition": "X" } is also a Leaf.
		String json = "{\"condition\": \"ARRIVE_AT_ZONE\"}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionCondition.ARRIVE_AT_ZONE, ((CompletionNode.Leaf) node).getCondition());
	}

	// ========================================================================
	// Branching all / any form
	// ========================================================================

	@Test
	public void allWithTwoLeaves()
	{
		String json = "{\"op\": \"all\", \"nodes\": [\"ITEM_OBTAINED\", \"PLAYER_ON_PLANE\"]}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionNode.Kind.ALL, node.getKind());
		assertEquals(2, ((CompletionNode.All) node).getChildren().size());
	}

	@Test
	public void anyWithTwoLeaves()
	{
		String json = "{\"op\": \"any\", \"nodes\": [\"ITEM_OBTAINED\", \"INVENTORY_HAS_ITEM\"]}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionNode.Kind.ANY, node.getKind());
	}

	@Test
	public void nestedAllContainingAny()
	{
		String json = "{"
			+ "\"op\": \"all\","
			+ "\"nodes\": ["
			+ "  \"ARRIVE_AT_TILE\","
			+ "  {\"op\": \"any\", \"nodes\": [\"INVENTORY_HAS_ITEM\", \"ITEM_OBTAINED\"]}"
			+ "]}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionNode.Kind.ALL, node.getKind());
		CompletionNode.All all = (CompletionNode.All) node;
		assertEquals(2, all.getChildren().size());
		assertEquals(CompletionNode.Kind.LEAF, all.getChildren().get(0).getKind());
		assertEquals(CompletionNode.Kind.ANY, all.getChildren().get(1).getKind());
	}

	@Test
	public void childrenKeyAccepted()
	{
		// Author-friendly alias — we accept either "nodes" or "children".
		String json = "{\"op\": \"any\", \"children\": [\"ITEM_OBTAINED\"]}";
		CompletionNode node = gson.fromJson(json, CompletionNode.class);
		assertEquals(CompletionNode.Kind.ANY, node.getKind());
	}

	@Test(expected = JsonParseException.class)
	public void branchingWithoutNodesArrayRejected()
	{
		gson.fromJson("{\"op\": \"all\"}", CompletionNode.class);
	}

	@Test(expected = JsonParseException.class)
	public void unknownOpRejected()
	{
		gson.fromJson("{\"op\": \"xor\", \"nodes\": []}", CompletionNode.class);
	}

	@Test(expected = JsonParseException.class)
	public void objectWithoutOpOrConditionRejected()
	{
		gson.fromJson("{\"foo\": \"bar\"}", CompletionNode.class);
	}

	// ========================================================================
	// Round-trip serialization
	// ========================================================================

	@Test
	public void leafRoundTripsAsLegacyString()
	{
		CompletionNode node = CompletionNode.leaf(CompletionCondition.MANUAL);
		String json = gson.toJson(node, CompletionNode.class);
		assertEquals("\"MANUAL\"", json);
	}

	@Test
	public void branchRoundTripPreservesStructure()
	{
		CompletionNode original = CompletionNode.all(java.util.Arrays.asList(
			CompletionNode.leaf(CompletionCondition.ARRIVE_AT_TILE),
			CompletionNode.any(java.util.Arrays.asList(
				CompletionNode.leaf(CompletionCondition.INVENTORY_HAS_ITEM),
				CompletionNode.leaf(CompletionCondition.ITEM_OBTAINED)
			))
		));
		String json = gson.toJson(original, CompletionNode.class);
		CompletionNode parsed = gson.fromJson(json, CompletionNode.class);
		assertEquals(original, parsed);
	}

	// ========================================================================
	// Null handling
	// ========================================================================

	@Test
	public void nullJsonReturnsNull()
	{
		assertNull(gson.fromJson("null", CompletionNode.class));
	}
}
