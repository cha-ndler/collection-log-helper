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
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Gson adapter that deserialises the {@code conditionTree} JSON shape into a
 * {@link ConditionNode} tree.
 *
 * <p>Discriminator is the single recognised top-level key on each object:
 * <ul>
 *   <li>{@code and} - array of child trees, builds {@link AndNode}.</li>
 *   <li>{@code or} - array of child trees, builds {@link OrNode}.</li>
 *   <li>{@code not} - single child tree, builds {@link NotNode}.</li>
 *   <li>leaf-type-keys (lower-case form of each {@link CompletionCondition}
 *       enum value), builds {@link LeafNode}.</li>
 * </ul>
 *
 * <p>Leaf shapes:
 * <pre>
 *   { "item_obtained": 12345 }
 *   { "inventory_has_item": 12345, "count": 2 }
 *   { "inventory_not_has_item": 12345 }
 *   { "arrive_at_tile": [3200, 3200, 0], "distance": 5 }
 *   { "arrive_at_zone": [3200, 3200, 3210, 3210, 0] }
 *   { "npc_talked_to": 1234 }
 *   { "actor_death": 1234 }
 *   { "actor_death": 0, "npc_ids": [2042, 2043, 2044] }
 *   { "player_on_plane": 2 }
 *   { "chat_message_received": "You have completed.*" }
 *   { "varbit_at_least": 1234, "value": 5 }
 *   { "manual": true }
 * </pre>
 *
 * <p>Unknown discriminators raise {@link JsonParseException}. Empty AND/OR
 * child lists and null NOT children parse successfully (the node's
 * {@code evaluate} method handles the vacuous case explicitly).
 */
@Slf4j
public class ConditionNodeDeserializer implements JsonDeserializer<ConditionNode>
{
	private static final String KEY_AND = "and";
	private static final String KEY_OR = "or";
	private static final String KEY_NOT = "not";

	private static final String LEAF_ITEM_OBTAINED = "item_obtained";
	private static final String LEAF_INVENTORY_HAS_ITEM = "inventory_has_item";
	private static final String LEAF_INVENTORY_NOT_HAS_ITEM = "inventory_not_has_item";
	private static final String LEAF_ARRIVE_AT_TILE = "arrive_at_tile";
	private static final String LEAF_ARRIVE_AT_ZONE = "arrive_at_zone";
	private static final String LEAF_NPC_TALKED_TO = "npc_talked_to";
	private static final String LEAF_ACTOR_DEATH = "actor_death";
	private static final String LEAF_PLAYER_ON_PLANE = "player_on_plane";
	private static final String LEAF_CHAT_MESSAGE_RECEIVED = "chat_message_received";
	private static final String LEAF_VARBIT_AT_LEAST = "varbit_at_least";
	private static final String LEAF_MANUAL = "manual";

	@Override
	public ConditionNode deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context)
	{
		if (element == null || element.isJsonNull())
		{
			return null;
		}
		if (!element.isJsonObject())
		{
			throw new JsonParseException("conditionTree node must be a JSON object, got: " + element);
		}
		JsonObject obj = element.getAsJsonObject();

		if (obj.has(KEY_AND))
		{
			return new AndNode(parseChildList(obj.get(KEY_AND), context, KEY_AND));
		}
		if (obj.has(KEY_OR))
		{
			return new OrNode(parseChildList(obj.get(KEY_OR), context, KEY_OR));
		}
		if (obj.has(KEY_NOT))
		{
			JsonElement childEl = obj.get(KEY_NOT);
			ConditionNode child = childEl == null || childEl.isJsonNull()
				? null
				: deserialize(childEl, ConditionNode.class, context);
			return new NotNode(child);
		}
		return parseLeaf(obj);
	}

	private List<ConditionNode> parseChildList(JsonElement childrenEl, JsonDeserializationContext context, String key)
	{
		if (childrenEl == null || childrenEl.isJsonNull())
		{
			log.warn("conditionTree '{}' has null children; treating as empty", key);
			return new ArrayList<>();
		}
		if (!childrenEl.isJsonArray())
		{
			throw new JsonParseException("conditionTree '" + key + "' must be a JSON array, got: " + childrenEl);
		}
		JsonArray arr = childrenEl.getAsJsonArray();
		List<ConditionNode> out = new ArrayList<>(arr.size());
		for (JsonElement e : arr)
		{
			out.add(deserialize(e, ConditionNode.class, context));
		}
		return out;
	}

	private ConditionNode parseLeaf(JsonObject obj)
	{
		// Each leaf-type key is mutually exclusive: the first match wins.
		if (obj.has(LEAF_ITEM_OBTAINED))
		{
			return leaf(CompletionCondition.ITEM_OBTAINED).itemId(intAt(obj, LEAF_ITEM_OBTAINED)).build();
		}
		if (obj.has(LEAF_INVENTORY_HAS_ITEM))
		{
			return leaf(CompletionCondition.INVENTORY_HAS_ITEM)
				.itemId(intAt(obj, LEAF_INVENTORY_HAS_ITEM))
				.count(optionalInt(obj, "count"))
				.build();
		}
		if (obj.has(LEAF_INVENTORY_NOT_HAS_ITEM))
		{
			return leaf(CompletionCondition.INVENTORY_NOT_HAS_ITEM)
				.itemId(intAt(obj, LEAF_INVENTORY_NOT_HAS_ITEM))
				.build();
		}
		if (obj.has(LEAF_ARRIVE_AT_TILE))
		{
			return leaf(CompletionCondition.ARRIVE_AT_TILE)
				.tile(intArrayAt(obj, LEAF_ARRIVE_AT_TILE, 3))
				.distance(optionalInt(obj, "distance"))
				.build();
		}
		if (obj.has(LEAF_ARRIVE_AT_ZONE))
		{
			return leaf(CompletionCondition.ARRIVE_AT_ZONE)
				.zone(intArrayAt(obj, LEAF_ARRIVE_AT_ZONE, 5))
				.build();
		}
		if (obj.has(LEAF_NPC_TALKED_TO))
		{
			return leaf(CompletionCondition.NPC_TALKED_TO)
				.npcId(intAt(obj, LEAF_NPC_TALKED_TO))
				.build();
		}
		if (obj.has(LEAF_ACTOR_DEATH))
		{
			int primary = intAt(obj, LEAF_ACTOR_DEATH);
			return leaf(CompletionCondition.ACTOR_DEATH)
				.npcId(primary)
				.npcIds(optionalIntList(obj, "npc_ids"))
				.build();
		}
		if (obj.has(LEAF_PLAYER_ON_PLANE))
		{
			return leaf(CompletionCondition.PLAYER_ON_PLANE)
				.plane(intAt(obj, LEAF_PLAYER_ON_PLANE))
				.build();
		}
		if (obj.has(LEAF_CHAT_MESSAGE_RECEIVED))
		{
			return leaf(CompletionCondition.CHAT_MESSAGE_RECEIVED)
				.chatPattern(stringAt(obj, LEAF_CHAT_MESSAGE_RECEIVED))
				.build();
		}
		if (obj.has(LEAF_VARBIT_AT_LEAST))
		{
			return leaf(CompletionCondition.VARBIT_AT_LEAST)
				.varbitId(intAt(obj, LEAF_VARBIT_AT_LEAST))
				.varbitValue(optionalInt(obj, "value"))
				.build();
		}
		if (obj.has(LEAF_MANUAL))
		{
			return leaf(CompletionCondition.MANUAL).build();
		}
		throw new JsonParseException("conditionTree node has no recognised discriminator key: " + obj);
	}

	private static int intAt(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull() || !el.isJsonPrimitive())
		{
			throw new JsonParseException("conditionTree '" + key + "' must be a primitive int");
		}
		return el.getAsInt();
	}

	private static Integer optionalInt(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull() || !el.isJsonPrimitive())
		{
			return null;
		}
		return el.getAsInt();
	}

	private static String stringAt(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull() || !el.isJsonPrimitive())
		{
			throw new JsonParseException("conditionTree '" + key + "' must be a primitive string");
		}
		return el.getAsString();
	}

	private static int[] intArrayAt(JsonObject obj, String key, int expectedLength)
	{
		JsonElement el = obj.get(key);
		if (el == null || !el.isJsonArray())
		{
			throw new JsonParseException("conditionTree '" + key + "' must be a JSON array of length " + expectedLength);
		}
		JsonArray arr = el.getAsJsonArray();
		if (arr.size() != expectedLength)
		{
			throw new JsonParseException("conditionTree '" + key + "' must have exactly " + expectedLength
				+ " entries, got " + arr.size());
		}
		int[] out = new int[expectedLength];
		for (int i = 0; i < expectedLength; i++)
		{
			out[i] = arr.get(i).getAsInt();
		}
		return out;
	}

	private static List<Integer> optionalIntList(JsonObject obj, String key)
	{
		JsonElement el = obj.get(key);
		if (el == null || el.isJsonNull())
		{
			return null;
		}
		if (!el.isJsonArray())
		{
			throw new JsonParseException("conditionTree '" + key + "' must be a JSON array");
		}
		JsonArray arr = el.getAsJsonArray();
		List<Integer> out = new ArrayList<>(arr.size());
		for (JsonElement e : arr)
		{
			out.add(e.getAsInt());
		}
		return out;
	}

	private static LeafBuilder leaf(CompletionCondition type)
	{
		return new LeafBuilder(type);
	}

	/** Minimal fluent builder for {@link LeafNode}; deserializer-internal. */
	private static final class LeafBuilder
	{
		private final CompletionCondition type;
		private Integer itemId;
		private Integer count;
		private int[] tile;
		private Integer distance;
		private int[] zone;
		private Integer npcId;
		private List<Integer> npcIds;
		private Integer plane;
		private String chatPattern;
		private Integer varbitId;
		private Integer varbitValue;

		LeafBuilder(CompletionCondition type)
		{
			this.type = type;
		}

		LeafBuilder itemId(Integer v) { this.itemId = v; return this; }
		LeafBuilder count(Integer v) { this.count = v; return this; }
		LeafBuilder tile(int[] v) { this.tile = v; return this; }
		LeafBuilder distance(Integer v) { this.distance = v; return this; }
		LeafBuilder zone(int[] v) { this.zone = v; return this; }
		LeafBuilder npcId(Integer v) { this.npcId = v; return this; }
		LeafBuilder npcIds(List<Integer> v) { this.npcIds = v; return this; }
		LeafBuilder plane(Integer v) { this.plane = v; return this; }
		LeafBuilder chatPattern(String v) { this.chatPattern = v; return this; }
		LeafBuilder varbitId(Integer v) { this.varbitId = v; return this; }
		LeafBuilder varbitValue(Integer v) { this.varbitValue = v; return this; }

		LeafNode build()
		{
			return new LeafNode(type, itemId, count, tile, distance, zone,
				npcId, npcIds, plane, chatPattern, varbitId, varbitValue);
		}
	}
}
