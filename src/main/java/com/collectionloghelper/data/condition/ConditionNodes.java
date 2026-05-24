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
import java.util.Arrays;
import java.util.List;

/**
 * Static factories for terse {@link ConditionNode} construction in tests and
 * (eventually) Phase 3 pilot wiring. Not used in the deserialization path.
 */
public final class ConditionNodes
{
	private ConditionNodes() { }

	public static AndNode and(ConditionNode... children)
	{
		return new AndNode(Arrays.asList(children));
	}

	public static OrNode or(ConditionNode... children)
	{
		return new OrNode(Arrays.asList(children));
	}

	public static NotNode not(ConditionNode child)
	{
		return new NotNode(child);
	}

	public static LeafNode itemObtained(int itemId)
	{
		return new LeafNode(CompletionCondition.ITEM_OBTAINED, itemId,
			null, null, null, null, null, null, null, null, null, null);
	}

	public static LeafNode inventoryHasItem(int itemId, int count)
	{
		return new LeafNode(CompletionCondition.INVENTORY_HAS_ITEM, itemId, count,
			null, null, null, null, null, null, null, null, null);
	}

	public static LeafNode inventoryNotHasItem(int itemId)
	{
		return new LeafNode(CompletionCondition.INVENTORY_NOT_HAS_ITEM, itemId,
			null, null, null, null, null, null, null, null, null, null);
	}

	public static LeafNode arriveAtTile(int x, int y, int plane, int distance)
	{
		return new LeafNode(CompletionCondition.ARRIVE_AT_TILE, null, null,
			new int[]{x, y, plane}, distance, null, null, null, null, null, null, null);
	}

	public static LeafNode arriveAtZone(int minX, int minY, int maxX, int maxY, int plane)
	{
		return new LeafNode(CompletionCondition.ARRIVE_AT_ZONE, null, null, null, null,
			new int[]{minX, minY, maxX, maxY, plane}, null, null, null, null, null, null);
	}

	public static LeafNode npcTalkedTo(int npcId)
	{
		return new LeafNode(CompletionCondition.NPC_TALKED_TO, null, null, null, null, null,
			npcId, null, null, null, null, null);
	}

	public static LeafNode actorDeath(int npcId)
	{
		return new LeafNode(CompletionCondition.ACTOR_DEATH, null, null, null, null, null,
			npcId, null, null, null, null, null);
	}

	public static LeafNode actorDeath(int primaryNpcId, List<Integer> alternateIds)
	{
		return new LeafNode(CompletionCondition.ACTOR_DEATH, null, null, null, null, null,
			primaryNpcId, alternateIds, null, null, null, null);
	}

	public static LeafNode playerOnPlane(int plane)
	{
		return new LeafNode(CompletionCondition.PLAYER_ON_PLANE, null, null, null, null, null,
			null, null, plane, null, null, null);
	}

	public static LeafNode chatMessageReceived(String pattern)
	{
		return new LeafNode(CompletionCondition.CHAT_MESSAGE_RECEIVED, null, null, null, null, null,
			null, null, null, pattern, null, null);
	}

	public static LeafNode varbitAtLeast(int varbitId, int value)
	{
		return new LeafNode(CompletionCondition.VARBIT_AT_LEAST, null, null, null, null, null,
			null, null, null, null, varbitId, value);
	}

	public static LeafNode manual()
	{
		return new LeafNode(CompletionCondition.MANUAL, null, null, null, null, null,
			null, null, null, null, null, null);
	}
}
