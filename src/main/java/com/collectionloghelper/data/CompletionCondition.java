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

	/** Step completes when the player interacts with a specific NPC. */
	NPC_TALKED_TO,

	/** Step must be manually advanced by the player via the panel. */
	MANUAL
}
