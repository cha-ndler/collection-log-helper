package com.collectionloghelper.data;

import java.util.List;
import lombok.Value;

/**
 * A single step in a multi-step guidance sequence for obtaining collection log items.
 * Each step describes what the player should do, where to go, and how the system
 * detects completion.
 */
@Value
public class GuidanceStep
{
	/** Human-readable description of what to do in this step. */
	String description;

	/** Target world coordinates for this step (0 = no location). */
	int worldX;
	int worldY;
	int worldPlane;

	/** NPC to highlight for this step (0 = no NPC). */
	int npcId;

	/** Right-click action to highlight on the NPC (e.g., "Attack", "Talk-to"). */
	String interactAction;

	/** Dialog options to highlight when talking to an NPC. */
	List<String> dialogOptions;

	/** Travel tip for reaching this step's location. */
	String travelTip;

	/** Item IDs that must be in inventory before this step can proceed. */
	List<Integer> requiredItemIds;

	/** How the system detects this step is complete. */
	CompletionCondition completionCondition;

	/** Item ID used for ITEM_OBTAINED or INVENTORY_HAS_ITEM completion checks. */
	int completionItemId;

	/** Tile distance threshold for ARRIVE_AT_TILE completion (default 5). */
	int completionDistance;

	/** NPC ID for NPC_TALKED_TO completion check. */
	int completionNpcId;

	/** Chat message to display when this step activates (null = no message). */
	String worldMessage;

	/** Game object ID to highlight for this step (0 = no object). */
	int objectId;

	/** Right-click action to display on the highlighted object (e.g., "Chop", "Mine"). */
	String objectInteractAction;

	/** Item IDs to highlight in the player's inventory during this step. */
	List<Integer> highlightItemIds;

	/** When true, overlays show "Use X on Y" style prompts instead of simple action labels. */
	boolean useItemOnObject;

	public int getCompletionDistance()
	{
		return completionDistance > 0 ? completionDistance : 5;
	}
}
