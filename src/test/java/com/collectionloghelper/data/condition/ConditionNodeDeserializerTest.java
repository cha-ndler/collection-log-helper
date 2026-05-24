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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Verifies the JSON shape of {@link ConditionNodeDeserializer}:
 * one round-trip case per leaf type, plus composite + edge cases.
 */
public class ConditionNodeDeserializerTest
{
	private final Gson gson = new GsonBuilder()
		.registerTypeAdapter(ConditionNode.class, new ConditionNodeDeserializer())
		.create();

	// -- Leaf shapes ----------------------------------------------------------

	@Test
	public void leaf_itemObtained()
	{
		LeafNode leaf = (LeafNode) gson.fromJson("{\"item_obtained\":12345}", ConditionNode.class);
		assertEquals(CompletionCondition.ITEM_OBTAINED, leaf.getType());
		assertEquals(12345, leaf.getItemId());
	}

	@Test
	public void leaf_inventoryHasItem_withCount()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"inventory_has_item\":995,\"count\":3}", ConditionNode.class);
		assertEquals(CompletionCondition.INVENTORY_HAS_ITEM, leaf.getType());
		assertEquals(995, leaf.getItemId());
		assertEquals(3, leaf.getCount());
	}

	@Test
	public void leaf_inventoryHasItem_noCountDefaultsNull()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"inventory_has_item\":995}", ConditionNode.class);
		assertEquals(995, leaf.getItemId());
		assertNull(leaf.getCount());
	}

	@Test
	public void leaf_inventoryNotHasItem()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"inventory_not_has_item\":4151}", ConditionNode.class);
		assertEquals(CompletionCondition.INVENTORY_NOT_HAS_ITEM, leaf.getType());
		assertEquals(4151, leaf.getItemId());
	}

	@Test
	public void leaf_arriveAtTile()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"arrive_at_tile\":[3200,3300,1],\"distance\":7}", ConditionNode.class);
		assertEquals(CompletionCondition.ARRIVE_AT_TILE, leaf.getType());
		assertArrayEquals(new int[]{3200, 3300, 1}, leaf.getTile());
		assertEquals(7, leaf.getDistance());
	}

	@Test
	public void leaf_arriveAtZone()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"arrive_at_zone\":[3200,3300,3210,3310,0]}", ConditionNode.class);
		assertEquals(CompletionCondition.ARRIVE_AT_ZONE, leaf.getType());
		assertArrayEquals(new int[]{3200, 3300, 3210, 3310, 0}, leaf.getZone());
	}

	@Test
	public void leaf_npcTalkedTo()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"npc_talked_to\":1234}", ConditionNode.class);
		assertEquals(CompletionCondition.NPC_TALKED_TO, leaf.getType());
		assertEquals(1234, leaf.getNpcId());
	}

	@Test
	public void leaf_actorDeath_withAlternateIds()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"actor_death\":2042,\"npc_ids\":[2043,2044]}", ConditionNode.class);
		assertEquals(CompletionCondition.ACTOR_DEATH, leaf.getType());
		assertEquals(2042, leaf.getNpcId());
		assertEquals(2, leaf.getNpcIds().size());
		assertEquals(2043, leaf.getNpcIds().get(0));
	}

	@Test
	public void leaf_playerOnPlane()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"player_on_plane\":2}", ConditionNode.class);
		assertEquals(CompletionCondition.PLAYER_ON_PLANE, leaf.getType());
		assertEquals(2, leaf.getPlane());
	}

	@Test
	public void leaf_chatMessageReceived()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"chat_message_received\":\"You have.*\"}", ConditionNode.class);
		assertEquals(CompletionCondition.CHAT_MESSAGE_RECEIVED, leaf.getType());
		assertEquals("You have.*", leaf.getChatPattern());
	}

	@Test
	public void leaf_varbitAtLeast()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"varbit_at_least\":4567,\"value\":5}", ConditionNode.class);
		assertEquals(CompletionCondition.VARBIT_AT_LEAST, leaf.getType());
		assertEquals(4567, leaf.getVarbitId());
		assertEquals(5, leaf.getVarbitValue());
	}

	@Test
	public void leaf_manual()
	{
		LeafNode leaf = (LeafNode) gson.fromJson(
			"{\"manual\":true}", ConditionNode.class);
		assertEquals(CompletionCondition.MANUAL, leaf.getType());
	}

	// -- Composite shapes -----------------------------------------------------

	@Test
	public void composite_and_twoLeaves()
	{
		ConditionNode node = gson.fromJson(
			"{\"and\":[{\"inventory_has_item\":995},{\"player_on_plane\":0}]}",
			ConditionNode.class);
		assertInstanceOf(AndNode.class, node);
		AndNode and = (AndNode) node;
		assertEquals(2, and.getChildren().size());
		assertInstanceOf(LeafNode.class, and.getChildren().get(0));
		assertInstanceOf(LeafNode.class, and.getChildren().get(1));
	}

	@Test
	public void composite_or_threeLeaves()
	{
		ConditionNode node = gson.fromJson(
			"{\"or\":[{\"item_obtained\":1},{\"item_obtained\":2},{\"item_obtained\":3}]}",
			ConditionNode.class);
		assertInstanceOf(OrNode.class, node);
		OrNode or = (OrNode) node;
		assertEquals(3, or.getChildren().size());
	}

	@Test
	public void composite_not_singleChild()
	{
		ConditionNode node = gson.fromJson(
			"{\"not\":{\"inventory_has_item\":995}}", ConditionNode.class);
		assertInstanceOf(NotNode.class, node);
		NotNode not = (NotNode) node;
		assertInstanceOf(LeafNode.class, not.getChild());
	}

	@Test
	public void composite_nested_andOrNot()
	{
		ConditionNode node = gson.fromJson(
			"{\"and\":["
				+ "{\"or\":[{\"item_obtained\":1},{\"item_obtained\":2}]},"
				+ "{\"not\":{\"inventory_has_item\":3}}"
				+ "]}", ConditionNode.class);
		assertInstanceOf(AndNode.class, node);
		AndNode and = (AndNode) node;
		assertInstanceOf(OrNode.class, and.getChildren().get(0));
		assertInstanceOf(NotNode.class, and.getChildren().get(1));
	}

	// -- Edge cases -----------------------------------------------------------

	@Test
	public void edge_andEmptyChildren_parsesAsVacuouslyTrue()
	{
		ConditionNode node = gson.fromJson("{\"and\":[]}", ConditionNode.class);
		assertInstanceOf(AndNode.class, node);
		assertTrue(((AndNode) node).getChildren().isEmpty());
	}

	@Test
	public void edge_orEmptyChildren_parsesAsVacuouslyFalse()
	{
		ConditionNode node = gson.fromJson("{\"or\":[]}", ConditionNode.class);
		assertInstanceOf(OrNode.class, node);
		assertTrue(((OrNode) node).getChildren().isEmpty());
	}

	@Test
	public void edge_notNullChild()
	{
		ConditionNode node = gson.fromJson("{\"not\":null}", ConditionNode.class);
		assertInstanceOf(NotNode.class, node);
		assertNull(((NotNode) node).getChild());
	}

	@Test
	public void edge_unknownDiscriminator_throws()
	{
		assertThrows(JsonParseException.class,
			() -> gson.fromJson("{\"frobnicate\":1}", ConditionNode.class));
	}

	@Test
	public void edge_arriveAtTileWrongArrayLength_throws()
	{
		assertThrows(JsonParseException.class,
			() -> gson.fromJson("{\"arrive_at_tile\":[3200,3300]}", ConditionNode.class));
	}

	@Test
	public void edge_arriveAtZoneWrongArrayLength_throws()
	{
		assertThrows(JsonParseException.class,
			() -> gson.fromJson("{\"arrive_at_zone\":[3200,3300,3210]}", ConditionNode.class));
	}

	@Test
	public void edge_nonObjectRoot_throws()
	{
		assertThrows(JsonParseException.class,
			() -> gson.fromJson("[1,2,3]", ConditionNode.class));
	}

	@Test
	public void edge_nullJsonReturnsNull()
	{
		ConditionNode node = gson.fromJson("null", ConditionNode.class);
		assertNull(node);
	}

	@Test
	public void edge_andChildrenMustBeArray()
	{
		assertThrows(JsonParseException.class,
			() -> gson.fromJson("{\"and\":42}", ConditionNode.class));
	}

	@Test
	public void edge_itemObtainedMustBeInt()
	{
		// Non-numeric string for an int leaf field surfaces as either a Gson
		// JsonParseException or a wrapped NumberFormatException depending on the
		// JsonElement.getAsInt() path; either signals a malformed leaf.
		assertThrows(RuntimeException.class,
			() -> gson.fromJson("{\"item_obtained\":\"foo\"}", ConditionNode.class));
	}
}
