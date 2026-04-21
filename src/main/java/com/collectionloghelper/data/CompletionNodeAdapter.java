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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gson adapter for {@link CompletionNode} that accepts three shapes:
 *
 * <ol>
 *   <li>A JSON string ({@code "ITEM_OBTAINED"}) — coerced to
 *       {@code CompletionNode.Leaf(CompletionCondition.ITEM_OBTAINED)}. This is the
 *       legacy shape still used by every production source file, which is why B1 is
 *       purely additive.</li>
 *   <li>A JSON object with {@code "op": "leaf"} and {@code "condition": "ITEM_OBTAINED"} —
 *       the explicit leaf form, useful when hand-authoring trees.</li>
 *   <li>A JSON object with {@code "op": "all" | "any"} and {@code "nodes": [...]} —
 *       the branching form. Children are parsed recursively via the same adapter, so
 *       any branch can nest to arbitrary depth.</li>
 * </ol>
 *
 * <p>The adapter is lenient to field casing on {@code op}/{@code OP} and both
 * {@code nodes}/{@code children} keys, because the authoring conventions for existing
 * JSON sources favour snake_case while the runtime code prefers {@code nodes}.
 */
public final class CompletionNodeAdapter
	implements JsonDeserializer<CompletionNode>, JsonSerializer<CompletionNode>
{
	private static final String KEY_OP = "op";
	private static final String KEY_CONDITION = "condition";
	private static final String KEY_NODES = "nodes";
	private static final String KEY_CHILDREN = "children";

	private static final String OP_LEAF = "leaf";
	private static final String OP_ALL = "all";
	private static final String OP_ANY = "any";

	@Override
	public CompletionNode deserialize(JsonElement element, Type type, JsonDeserializationContext ctx)
		throws JsonParseException
	{
		if (element == null || element.isJsonNull())
		{
			return null;
		}

		// Legacy shape: bare string enum → wrap as Leaf.
		if (element.isJsonPrimitive())
		{
			JsonPrimitive prim = element.getAsJsonPrimitive();
			if (prim.isString())
			{
				return CompletionNode.leaf(parseCondition(prim.getAsString()));
			}
			throw new JsonParseException("CompletionNode primitive must be a string, got: " + prim);
		}

		if (element.isJsonObject())
		{
			JsonObject obj = element.getAsJsonObject();
			String op = readOp(obj);
			if (op == null)
			{
				// No explicit op — if a "condition" field is present, treat as implicit leaf.
				if (obj.has(KEY_CONDITION))
				{
					return CompletionNode.leaf(parseCondition(obj.get(KEY_CONDITION).getAsString()));
				}
				throw new JsonParseException("CompletionNode object must define \"op\" (leaf|all|any)");
			}

			switch (op)
			{
				case OP_LEAF:
					if (!obj.has(KEY_CONDITION))
					{
						throw new JsonParseException("Leaf node requires \"condition\" field");
					}
					return CompletionNode.leaf(parseCondition(obj.get(KEY_CONDITION).getAsString()));
				case OP_ALL:
					return CompletionNode.all(readChildren(obj, ctx));
				case OP_ANY:
					return CompletionNode.any(readChildren(obj, ctx));
				default:
					throw new JsonParseException("Unknown CompletionNode op: " + op);
			}
		}

		throw new JsonParseException("CompletionNode must be a string or object, got: " + element);
	}

	@Override
	public JsonElement serialize(CompletionNode node, Type type, JsonSerializationContext ctx)
	{
		if (node == null)
		{
			return null;
		}
		if (node instanceof CompletionNode.Leaf)
		{
			// Round-trip as the legacy string form so legacy consumers keep working.
			return new JsonPrimitive(((CompletionNode.Leaf) node).getCondition().name());
		}
		JsonObject obj = new JsonObject();
		if (node instanceof CompletionNode.All)
		{
			obj.addProperty(KEY_OP, OP_ALL);
			obj.add(KEY_NODES, serializeChildren(((CompletionNode.All) node).getChildren(), ctx));
			return obj;
		}
		if (node instanceof CompletionNode.Any)
		{
			obj.addProperty(KEY_OP, OP_ANY);
			obj.add(KEY_NODES, serializeChildren(((CompletionNode.Any) node).getChildren(), ctx));
			return obj;
		}
		throw new JsonParseException("Unknown CompletionNode subtype: " + node.getClass());
	}

	private static String readOp(JsonObject obj)
	{
		if (obj.has(KEY_OP))
		{
			return obj.get(KEY_OP).getAsString().toLowerCase(Locale.ROOT);
		}
		if (obj.has("OP"))
		{
			return obj.get("OP").getAsString().toLowerCase(Locale.ROOT);
		}
		return null;
	}

	private List<CompletionNode> readChildren(JsonObject obj, JsonDeserializationContext ctx)
	{
		JsonElement childrenEl = obj.has(KEY_NODES) ? obj.get(KEY_NODES)
			: obj.has(KEY_CHILDREN) ? obj.get(KEY_CHILDREN) : null;
		if (childrenEl == null || !childrenEl.isJsonArray())
		{
			throw new JsonParseException("Branching CompletionNode requires \"nodes\" array");
		}
		JsonArray array = childrenEl.getAsJsonArray();
		List<CompletionNode> children = new ArrayList<>(array.size());
		for (JsonElement child : array)
		{
			CompletionNode parsed = ctx.deserialize(child, CompletionNode.class);
			if (parsed == null)
			{
				throw new JsonParseException("Null entry in CompletionNode children");
			}
			children.add(parsed);
		}
		return children;
	}

	private JsonArray serializeChildren(List<CompletionNode> children, JsonSerializationContext ctx)
	{
		JsonArray array = new JsonArray();
		for (CompletionNode child : children)
		{
			array.add(ctx.serialize(child, CompletionNode.class));
		}
		return array;
	}

	private static CompletionCondition parseCondition(String raw)
	{
		if (raw == null)
		{
			throw new JsonParseException("CompletionCondition name must not be null");
		}
		try
		{
			return CompletionCondition.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex)
		{
			throw new JsonParseException("Unknown CompletionCondition: " + raw, ex);
		}
	}
}
