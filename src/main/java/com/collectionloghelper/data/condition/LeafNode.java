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
import com.collectionloghelper.data.Zone;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * Wraps a single atomic {@link CompletionCondition} value plus its supporting
 * fields. Leaf evaluation mirrors the legacy
 * {@link com.collectionloghelper.guidance.CompletionChecker} branches exactly,
 * so a leaf carrying e.g. {@code INVENTORY_HAS_ITEM} with {@code itemId=12345}
 * is satisfied under exactly the same player-state as the flat-enum form.
 *
 * <p>Supporting fields use boxed wrapper types where "absent" matters: a
 * leaf for {@code INVENTORY_HAS_ITEM} only ever populates {@code itemId} and
 * (optionally) {@code count}; all other fields stay null. The deserializer
 * builds these via the leaf-type-key in the JSON shape; see
 * {@link ConditionNodeDeserializer}.
 *
 * <p>Event-driven leaves ({@code ACTOR_DEATH}, {@code NPC_TALKED_TO},
 * {@code CHAT_MESSAGE_RECEIVED}, {@code VARBIT_AT_LEAST},
 * {@code ITEM_OBTAINED}) read their inputs from
 * {@link ConditionEvaluationContext}'s last-event fields. When no event is
 * present (the {@code lastXxx} field is at its sentinel) they return false.
 * This matches legacy behaviour: in fast-forward / skip-chain evaluation
 * those conditions cannot be satisfied without an in-flight event.
 *
 * <p>{@code MANUAL} leaves always return false from {@link #evaluate} - by
 * definition the player advances them from the panel, never the tree.
 */
@Value
public class LeafNode implements ConditionNode
{
	CompletionCondition type;

	/** Item id for ITEM_OBTAINED / INVENTORY_HAS_ITEM / INVENTORY_NOT_HAS_ITEM. */
	@Nullable
	Integer itemId;

	/** Count for INVENTORY_HAS_ITEM (defaults to 1 when null). */
	@Nullable
	Integer count;

	/** Tile [x, y, plane] for ARRIVE_AT_TILE. */
	@Nullable
	int[] tile;

	/** Distance threshold for ARRIVE_AT_TILE (defaults to 5 when null). */
	@Nullable
	Integer distance;

	/** Zone [minX, minY, maxX, maxY, plane] for ARRIVE_AT_ZONE. */
	@Nullable
	int[] zone;

	/** Primary NPC id for NPC_TALKED_TO / ACTOR_DEATH. */
	@Nullable
	Integer npcId;

	/** Additional NPC ids for ACTOR_DEATH (multi-form bosses). */
	@Nullable
	List<Integer> npcIds;

	/** Plane for PLAYER_ON_PLANE. */
	@Nullable
	Integer plane;

	/** Chat pattern for CHAT_MESSAGE_RECEIVED. */
	@Nullable
	String chatPattern;

	/** Varbit id for VARBIT_AT_LEAST. */
	@Nullable
	Integer varbitId;

	/** Threshold for VARBIT_AT_LEAST. */
	@Nullable
	Integer varbitValue;

	@Override
	public boolean evaluate(ConditionEvaluationContext ctx)
	{
		if (type == null || ctx == null)
		{
			return false;
		}
		switch (type)
		{
			case ITEM_OBTAINED:
				return evaluateItemObtained(ctx);
			case INVENTORY_HAS_ITEM:
				return evaluateInventoryHasItem(ctx);
			case INVENTORY_NOT_HAS_ITEM:
				return evaluateInventoryNotHasItem(ctx);
			case ARRIVE_AT_TILE:
				return evaluateArriveAtTile(ctx);
			case ARRIVE_AT_ZONE:
				return evaluateArriveAtZone(ctx);
			case NPC_TALKED_TO:
				return evaluateNpcTalkedTo(ctx);
			case ACTOR_DEATH:
				return evaluateActorDeath(ctx);
			case PLAYER_ON_PLANE:
				return evaluatePlayerOnPlane(ctx);
			case CHAT_MESSAGE_RECEIVED:
				return evaluateChatMessage(ctx);
			case VARBIT_AT_LEAST:
				return evaluateVarbitAtLeast(ctx);
			case MANUAL:
				return false;
			default:
				return false;
		}
	}

	private boolean evaluateItemObtained(ConditionEvaluationContext ctx)
	{
		if (itemId == null)
		{
			return false;
		}
		// Honour an in-flight obtained event when present, otherwise consult
		// the persistent collection-log state. The OR matches the legacy
		// CompletionChecker which can satisfy on either signal.
		if (ctx.getLastObtainedItemId() == itemId)
		{
			return true;
		}
		return ctx.getCollectionState() != null
			&& ctx.getCollectionState().isItemObtained(itemId);
	}

	private boolean evaluateInventoryHasItem(ConditionEvaluationContext ctx)
	{
		if (itemId == null || ctx.getInventoryState() == null)
		{
			return false;
		}
		int needed = count != null && count > 0 ? count : 1;
		return ctx.getInventoryState().hasItemCount(itemId, needed);
	}

	private boolean evaluateInventoryNotHasItem(ConditionEvaluationContext ctx)
	{
		if (itemId == null || ctx.getInventoryState() == null)
		{
			return false;
		}
		return !ctx.getInventoryState().hasItem(itemId);
	}

	private boolean evaluateArriveAtTile(ConditionEvaluationContext ctx)
	{
		WorldPoint player = ctx.getPlayerLocation();
		if (tile == null || tile.length < 3 || player == null)
		{
			return false;
		}
		int targetPlane = tile[2];
		if (player.getPlane() != targetPlane)
		{
			return false;
		}
		int radius = distance != null && distance > 0 ? distance : 5;
		WorldPoint target = new WorldPoint(tile[0], tile[1], targetPlane);
		return player.distanceTo2D(target) <= radius;
	}

	private boolean evaluateArriveAtZone(ConditionEvaluationContext ctx)
	{
		WorldPoint player = ctx.getPlayerLocation();
		if (zone == null || zone.length != 5 || player == null)
		{
			return false;
		}
		Zone z = new Zone(zone[0], zone[1], zone[2], zone[3], zone[4]);
		return z.contains(player);
	}

	private boolean evaluateNpcTalkedTo(ConditionEvaluationContext ctx)
	{
		return npcId != null && ctx.getLastInteractedNpcId() == npcId;
	}

	private boolean evaluateActorDeath(ConditionEvaluationContext ctx)
	{
		int dead = ctx.getLastDeadNpcId();
		if (dead < 0)
		{
			return false;
		}
		if (npcId != null && dead == npcId)
		{
			return true;
		}
		if (npcIds != null)
		{
			for (int id : npcIds)
			{
				if (id == dead)
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean evaluatePlayerOnPlane(ConditionEvaluationContext ctx)
	{
		WorldPoint player = ctx.getPlayerLocation();
		return plane != null && player != null && player.getPlane() == plane;
	}

	private boolean evaluateChatMessage(ConditionEvaluationContext ctx)
	{
		String message = ctx.getLastChatMessage();
		if (message == null || chatPattern == null)
		{
			return false;
		}
		// Compile per evaluate: trees fire at most once per tick and only when
		// CHAT_MESSAGE_RECEIVED leaves are present, so the cost is negligible
		// and keeping the leaf immutable+stateless simplifies equals/hashCode.
		Pattern p = Pattern.compile(chatPattern);
		return p.matcher(message).find();
	}

	private boolean evaluateVarbitAtLeast(ConditionEvaluationContext ctx)
	{
		return varbitId != null
			&& varbitValue != null
			&& ctx.getLastVarbitId() == varbitId
			&& ctx.getLastVarbitValue() >= varbitValue;
	}
}
