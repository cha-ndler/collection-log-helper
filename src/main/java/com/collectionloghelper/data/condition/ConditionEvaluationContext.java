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
import javax.annotation.Nullable;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * Snapshot of live state passed to {@link ConditionNode#evaluate}.
 *
 * <p>Aggregates the five input sources the flat-enum
 * {@link com.collectionloghelper.guidance.CompletionChecker} reads today:
 * <ul>
 *   <li>{@link PlayerInventoryState} - for inventory/equipment checks.</li>
 *   <li>{@link PlayerCollectionState} - for ITEM_OBTAINED checks against the
 *       collection log.</li>
 *   <li>{@link WorldPoint} player location - for ARRIVE_AT_TILE,
 *       ARRIVE_AT_ZONE, PLAYER_ON_PLANE.</li>
 *   <li>Last in-flight event values - last NPC death id, last chat message,
 *       last varbit snapshot map - so event-driven leaves can be evaluated
 *       inside the same tick the event fired.</li>
 * </ul>
 *
 * <p>All event fields are {@code @Nullable}. Tree evaluation MUST handle the
 * "no event yet this tick" case by returning false from the affected leaf,
 * matching the legacy behaviour of
 * {@link com.collectionloghelper.guidance.CompletionChecker#isStepAlreadySatisfied}
 * which returns false for event-driven conditions.
 *
 * <p>For varbits, callers pass a single {@code lastVarbitId} +
 * {@code lastVarbitValue}; the leaf only matches when the id matches and
 * the value clears its threshold. This mirrors the existing
 * {@code isVarbitAtLeastSatisfying(step, varbitId, value)} signature.
 */
@Value
public class ConditionEvaluationContext
{
	PlayerInventoryState inventoryState;
	PlayerCollectionState collectionState;

	@Nullable
	WorldPoint playerLocation;

	/** Item id from the most recent collection-log obtained event, or -1. */
	int lastObtainedItemId;

	/** NPC id from the most recent ACTOR_DEATH event, or -1. */
	int lastDeadNpcId;

	/** NPC id from the most recent NPC_TALKED_TO event, or -1. */
	int lastInteractedNpcId;

	/** Chat message text from the most recent CHAT_MESSAGE_RECEIVED event, or null. */
	@Nullable
	String lastChatMessage;

	/** Varbit id from the most recent varbit-change event, or -1. */
	int lastVarbitId;

	/** Varbit value from the most recent varbit-change event, or 0. */
	int lastVarbitValue;

	/**
	 * Builds a context that only carries player-state inputs (no in-flight
	 * events). Event-driven leaves will evaluate to false. Convenience for
	 * fast-forward / skip-chain evaluation where only persistent state is
	 * relevant.
	 */
	public static ConditionEvaluationContext stateOnly(
		PlayerInventoryState inventoryState,
		PlayerCollectionState collectionState,
		@Nullable WorldPoint playerLocation)
	{
		return new ConditionEvaluationContext(
			inventoryState, collectionState, playerLocation,
			-1, -1, -1, null, -1, 0);
	}
}
