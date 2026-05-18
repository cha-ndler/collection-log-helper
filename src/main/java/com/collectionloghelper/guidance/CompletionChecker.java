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
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.StepWaypoint;
import com.collectionloghelper.data.Zone;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Evaluates whether {@link GuidanceStep} completion conditions are satisfied
 * by a given event or by the player's current state.
 *
 * <p>Pure extraction from {@link GuidanceSequencer}: this class holds no
 * mutable state of its own. Caches that the sequencer maintains for hot-path
 * evaluation (compiled chat regex, resolved tile WorldPoint) are passed in via
 * {@link ChatPatternCache} / {@link TilePointCache} input records and returned
 * as updated copies so the sequencer remains the single owner of that state.</p>
 *
 * <p>Methods return pure booleans — they never mutate sequencer fields, never
 * call {@code advanceStep()}, and never log step-advance messages. Callers
 * inspect the boolean and apply the resulting state transition themselves.</p>
 */
@Slf4j
@Singleton
public class CompletionChecker
{
	private final PlayerInventoryState inventoryState;
	private final PlayerCollectionState collectionState;

	@Inject
	CompletionChecker(PlayerInventoryState inventoryState, PlayerCollectionState collectionState)
	{
		this.inventoryState = inventoryState;
		this.collectionState = collectionState;
	}

	/**
	 * Returns the first item id from {@code step.skipIfHasAnyItemIds} that the
	 * player currently has, or -1 when none match. {@link SkipIfHasAnyResult#matched()}
	 * is a convenience accessor for {@code matchedItemId != -1}.
	 */
	public SkipIfHasAnyResult evaluateSkipIfHasAny(GuidanceStep step)
	{
		if (step == null || step.getSkipIfHasAnyItemIds() == null)
		{
			return SkipIfHasAnyResult.NO_MATCH;
		}
		for (int itemId : step.getSkipIfHasAnyItemIds())
		{
			if (inventoryState.hasItem(itemId))
			{
				return new SkipIfHasAnyResult(itemId);
			}
		}
		return SkipIfHasAnyResult.NO_MATCH;
	}

	/**
	 * Returns true if the step is satisfied by an ITEM_OBTAINED event for
	 * {@code obtainedItemId}.
	 */
	public boolean isItemObtainedSatisfying(GuidanceStep step, int obtainedItemId)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.ITEM_OBTAINED
			&& step.getCompletionItemId() == obtainedItemId;
	}

	/**
	 * Returns true if the step's INVENTORY_HAS_ITEM condition is satisfied by
	 * the player's current inventory.
	 */
	public boolean isInventoryHasItemSatisfying(GuidanceStep step)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.INVENTORY_HAS_ITEM
			&& step.getCompletionItemId() > 0
			&& inventoryState.hasItemCount(step.getCompletionItemId(), step.getCompletionItemCount());
	}

	/**
	 * Returns true if the step's INVENTORY_NOT_HAS_ITEM condition is satisfied
	 * (i.e., the item is absent from the player's inventory).
	 */
	public boolean isInventoryNotHasItemSatisfying(GuidanceStep step)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.INVENTORY_NOT_HAS_ITEM
			&& step.getCompletionItemId() > 0
			&& !inventoryState.hasItem(step.getCompletionItemId());
	}

	/**
	 * Returns true if the step is satisfied by an ACTOR_DEATH event for the
	 * given npc id.
	 */
	public boolean isNpcDeathSatisfying(GuidanceStep step, int npcId)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.ACTOR_DEATH
			&& step.matchesCompletionNpc(npcId);
	}

	/**
	 * Returns true if the step is satisfied by an NPC_TALKED_TO event for the
	 * given npc id.
	 */
	public boolean isNpcInteractedSatisfying(GuidanceStep step, int npcId)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.NPC_TALKED_TO
			&& step.getCompletionNpcId() == npcId;
	}

	/**
	 * Returns true if the step is satisfied by a VARBIT_AT_LEAST condition
	 * given the supplied {@code value} for the configured varbit.
	 */
	public boolean isVarbitAtLeastSatisfying(GuidanceStep step, int varbitId, int value)
	{
		return step != null
			&& step.getCompletionCondition() == CompletionCondition.VARBIT_AT_LEAST
			&& step.getCompletionVarbitId() == varbitId
			&& value >= step.getCompletionVarbitValue();
	}

	/**
	 * Returns true if the player is currently inside the step's ARRIVE_AT_ZONE
	 * zone.
	 */
	public boolean isArriveAtZoneSatisfying(GuidanceStep step, WorldPoint playerLocation)
	{
		if (step == null || playerLocation == null
			|| step.getCompletionCondition() != CompletionCondition.ARRIVE_AT_ZONE)
		{
			return false;
		}
		Zone zone = step.getZone();
		return zone != null && zone.contains(playerLocation);
	}

	/**
	 * Returns true if the player's plane matches the step's PLAYER_ON_PLANE
	 * target.
	 */
	public boolean isPlayerOnPlaneSatisfying(GuidanceStep step, WorldPoint playerLocation)
	{
		return step != null
			&& playerLocation != null
			&& step.getCompletionCondition() == CompletionCondition.PLAYER_ON_PLANE
			&& playerLocation.getPlane() == step.getWorldPlane();
	}

	/**
	 * Evaluates one tick of the ordered-waypoint progression for an
	 * ARRIVE_AT_TILE step that declares a {@code waypoints} list.
	 *
	 * <p>{@link WaypointProgressResult#crossedWaypointIndex()} carries the
	 * updated waypoint cursor — callers must store it back onto their state.
	 * {@link WaypointProgressResult#satisfied()} is true once every waypoint
	 * has been crossed.</p>
	 */
	public WaypointProgressResult evaluateWaypointProgress(GuidanceStep step, int stepNumberForLog,
		WorldPoint playerLocation, int crossedWaypointIndex)
	{
		List<StepWaypoint> waypoints = step != null ? step.getWaypoints() : null;
		if (waypoints == null || waypoints.isEmpty() || playerLocation == null)
		{
			return new WaypointProgressResult(false, crossedWaypointIndex);
		}

		int cursor = crossedWaypointIndex;
		if (cursor < waypoints.size())
		{
			StepWaypoint target = waypoints.get(cursor);
			if (playerLocation.getPlane() == target.getPlane())
			{
				WorldPoint targetPoint = new WorldPoint(target.getWorldX(), target.getWorldY(), target.getPlane());
				if (playerLocation.distanceTo2D(targetPoint) <= target.getRadius())
				{
					log.info("Step {} — crossed waypoint {}/{} at ({},{}) plane {}",
						stepNumberForLog, cursor + 1, waypoints.size(),
						target.getWorldX(), target.getWorldY(), target.getPlane());
					cursor++;
				}
			}
		}
		return new WaypointProgressResult(cursor >= waypoints.size(), cursor);
	}

	/**
	 * Evaluates the step's ARRIVE_AT_TILE condition for the legacy single-tile
	 * case (i.e., the step declares no waypoint list). Reuses an existing
	 * cached {@link WorldPoint} if {@code cache} matches the current step
	 * index; otherwise builds a fresh one and returns it inside
	 * {@link ArriveAtTileResult}.
	 *
	 * <p>Callers must store the returned cache back onto their own state.</p>
	 */
	public ArriveAtTileResult evaluateArriveAtTile(GuidanceStep step, int stepIndex,
		WorldPoint playerLocation, TilePointCache cache)
	{
		if (step == null || playerLocation == null
			|| step.getCompletionCondition() != CompletionCondition.ARRIVE_AT_TILE
			|| step.getWorldX() <= 0)
		{
			return new ArriveAtTileResult(false, cache);
		}

		TilePointCache outCache = cache;
		WorldPoint stepPoint;
		if (cache == null || cache.stepIndex() != stepIndex)
		{
			stepPoint = new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			outCache = new TilePointCache(stepIndex, stepPoint);
		}
		else
		{
			stepPoint = cache.point();
		}

		boolean satisfied = playerLocation.getPlane() == step.getWorldPlane()
			&& playerLocation.distanceTo2D(stepPoint) <= step.getCompletionDistance();
		return new ArriveAtTileResult(satisfied, outCache);
	}

	/**
	 * Evaluates the step's CHAT_MESSAGE_RECEIVED condition against
	 * {@code message}. Reuses the supplied compiled {@link Pattern} when the
	 * source string is unchanged; otherwise compiles a fresh pattern and
	 * returns it inside {@link ChatMatchResult}.
	 *
	 * <p>Callers must store the returned cache back onto their own state.</p>
	 */
	public ChatMatchResult evaluateChatMessage(GuidanceStep step, String message, ChatPatternCache cache)
	{
		if (step == null || message == null
			|| step.getCompletionCondition() != CompletionCondition.CHAT_MESSAGE_RECEIVED
			|| step.getCompletionChatPattern() == null)
		{
			return new ChatMatchResult(false, cache);
		}

		String patternStr = step.getCompletionChatPattern();
		ChatPatternCache outCache = cache;
		Pattern compiled;
		if (cache == null || !patternStr.equals(cache.source()))
		{
			compiled = Pattern.compile(patternStr);
			outCache = new ChatPatternCache(patternStr, compiled);
		}
		else
		{
			compiled = cache.compiled();
		}

		boolean matched = compiled.matcher(message).find();
		return new ChatMatchResult(matched, outCache);
	}

	/**
	 * Returns true if the supplied {@code step} is already satisfied by the
	 * player's current state. Used by the sequencer's skip-chain to fast-forward
	 * past steps the player no longer needs to perform.
	 *
	 * <p>Conditions that require an in-flight event (ACTOR_DEATH,
	 * CHAT_MESSAGE_RECEIVED, VARBIT_AT_LEAST) always return false here — they
	 * can only be satisfied as the event arrives.</p>
	 */
	public boolean isStepAlreadySatisfied(GuidanceStep step, WorldPoint lastKnownPlayerLocation)
	{
		if (step == null)
		{
			return false;
		}

		// OR-logic skip: if inventory has ANY of these items, skip the step
		if (evaluateSkipIfHasAny(step).matched())
		{
			return true;
		}

		if (step.getCompletionCondition() == null)
		{
			return false;
		}

		switch (step.getCompletionCondition())
		{
			case INVENTORY_HAS_ITEM:
				return step.getCompletionItemId() > 0
					&& inventoryState.hasItemCount(step.getCompletionItemId(), step.getCompletionItemCount());
			case INVENTORY_NOT_HAS_ITEM:
				return step.getCompletionItemId() > 0 && !inventoryState.hasItem(step.getCompletionItemId());
			case ITEM_OBTAINED:
				return step.getCompletionItemId() > 0 && collectionState.isItemObtained(step.getCompletionItemId());
			case ARRIVE_AT_TILE:
				return lastKnownPlayerLocation != null && step.getWorldX() > 0
					&& lastKnownPlayerLocation.getPlane() == step.getWorldPlane()
					&& lastKnownPlayerLocation.distanceTo2D(
						new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane()))
						<= step.getCompletionDistance();
			case ARRIVE_AT_ZONE:
				Zone zone = step.getZone();
				return lastKnownPlayerLocation != null && zone != null
					&& zone.contains(lastKnownPlayerLocation);
			case PLAYER_ON_PLANE:
				return lastKnownPlayerLocation != null
					&& lastKnownPlayerLocation.getPlane() == step.getWorldPlane();
			case ACTOR_DEATH:
			case CHAT_MESSAGE_RECEIVED:
			case VARBIT_AT_LEAST:
				return false;
			default:
				return false;
		}
	}

	/** Result of an ARRIVE_AT_TILE evaluation, carrying the updated tile cache. */
	public static final class ArriveAtTileResult
	{
		private final boolean satisfied;
		private final TilePointCache cache;

		public ArriveAtTileResult(boolean satisfied, TilePointCache cache)
		{
			this.satisfied = satisfied;
			this.cache = cache;
		}

		public boolean satisfied() { return satisfied; }
		public TilePointCache cache() { return cache; }
	}

	/** Result of one tick of waypoint progression. */
	public static final class WaypointProgressResult
	{
		private final boolean satisfied;
		private final int crossedWaypointIndex;

		public WaypointProgressResult(boolean satisfied, int crossedWaypointIndex)
		{
			this.satisfied = satisfied;
			this.crossedWaypointIndex = crossedWaypointIndex;
		}

		public boolean satisfied() { return satisfied; }
		public int crossedWaypointIndex() { return crossedWaypointIndex; }
	}

	/** Result of a CHAT_MESSAGE_RECEIVED evaluation, carrying the updated pattern cache. */
	public static final class ChatMatchResult
	{
		private final boolean matched;
		private final ChatPatternCache cache;

		public ChatMatchResult(boolean matched, ChatPatternCache cache)
		{
			this.matched = matched;
			this.cache = cache;
		}

		public boolean matched() { return matched; }
		public ChatPatternCache cache() { return cache; }
	}

	/** Result of a skipIfHasAnyItemIds evaluation; {@code matchedItemId} is -1 when no match. */
	public static final class SkipIfHasAnyResult
	{
		public static final SkipIfHasAnyResult NO_MATCH = new SkipIfHasAnyResult(-1);

		private final int matchedItemId;

		public SkipIfHasAnyResult(int matchedItemId)
		{
			this.matchedItemId = matchedItemId;
		}

		public int matchedItemId() { return matchedItemId; }
		public boolean matched() { return matchedItemId != -1; }
	}

	/** Cached compiled {@link Pattern} keyed by its source string. */
	public static final class ChatPatternCache
	{
		private final String source;
		private final Pattern compiled;

		public ChatPatternCache(String source, Pattern compiled)
		{
			this.source = source;
			this.compiled = compiled;
		}

		public String source() { return source; }
		public Pattern compiled() { return compiled; }
	}

	/** Cached {@link WorldPoint} for an ARRIVE_AT_TILE step, keyed by step index. */
	public static final class TilePointCache
	{
		private final int stepIndex;
		private final WorldPoint point;

		public TilePointCache(int stepIndex, WorldPoint point)
		{
			this.stepIndex = stepIndex;
			this.point = point;
		}

		public int stepIndex() { return stepIndex; }
		public WorldPoint point() { return point; }
	}
}
