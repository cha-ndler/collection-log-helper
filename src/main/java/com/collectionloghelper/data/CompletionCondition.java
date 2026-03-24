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
