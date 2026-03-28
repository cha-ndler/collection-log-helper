/*
 * Copyright (c) 2025, Chandler
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

/**
 * Defines how a guidance step is considered complete.
 */
public enum CompletionCondition
{
	/** Step completes when a specific item appears in the collection log. */
	ITEM_OBTAINED,

	/** Step completes when the player's inventory contains a specific item. */
	INVENTORY_HAS_ITEM,

	/** Step completes when the player arrives within range of the target tile. */
	ARRIVE_AT_TILE,

	/** Step completes when the player enters the target zone (rectangular area). */
	ARRIVE_AT_ZONE,

	/** Step completes when the player interacts with a specific NPC. */
	NPC_TALKED_TO,

	/** Step completes when the player's inventory no longer contains a specific item. */
	INVENTORY_NOT_HAS_ITEM,

	/** Step completes when the player reaches the target world plane (ignores XY). */
	PLAYER_ON_PLANE,

	/** Step completes when a specific NPC dies. */
	ACTOR_DEATH,

	/** Step completes when a specific chat message pattern is received. */
	CHAT_MESSAGE_RECEIVED,

	/** Step must be manually advanced by the player via the panel. */
	MANUAL
}
