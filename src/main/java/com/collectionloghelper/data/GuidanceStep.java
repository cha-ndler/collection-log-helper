package com.collectionloghelper.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	/** Required quantity for INVENTORY_HAS_ITEM checks (0 or 1 = any quantity). */
	int completionItemCount;

	/** Tile distance threshold for ARRIVE_AT_TILE completion (default 5). */
	int completionDistance;

	/** NPC ID for NPC_TALKED_TO completion check. */
	int completionNpcId;

	/** Chat message to display when this step activates (null = no message). */
	String worldMessage;

	/** Game object ID to highlight for this step (0 = no object). */
	int objectId;

	/** Additional object IDs to highlight (e.g., both team variants in Trouble Brewing). */
	List<Integer> objectIds;

	/** Right-click action to display on the highlighted object (e.g., "Chop", "Mine"). */
	String objectInteractAction;

	/** Item IDs to highlight in the player's inventory during this step. */
	List<Integer> highlightItemIds;

	/** Item IDs to highlight on the ground during this step. */
	List<Integer> groundItemIds;

	/** Regex pattern for CHAT_MESSAGE_RECEIVED completion check. */
	String completionChatPattern;

	/** When true, overlays show "Use X on Y" style prompts instead of simple action labels. */
	boolean useItemOnObject;

	/**
	 * Max tile distance from worldX/worldY to highlight objects (0 = no filter).
	 * When set, only objects within this radius of the step's coordinates are highlighted.
	 * Useful when multiple instances of the same object ID exist but only one is correct
	 * (e.g., Trouble Brewing hoppers).
	 */
	int objectMaxDistance;

	/**
	 * Specific tile coordinates where objects should be highlighted.
	 * Each entry is [x, y, plane]. When non-null, only objects at one of these
	 * tiles are highlighted, overriding objectMaxDistance.
	 */
	List<int[]> objectFilterTiles;

	/** 1-indexed step number to loop back to when this step completes (0 = no loop). */
	int loopBackToStep;

	/** Total number of loop iterations before advancing past this step (0 = no loop). */
	int loopCount;

	public int getCompletionDistance()
	{
		return completionDistance > 0 ? completionDistance : 5;
	}

	public int getCompletionItemCount()
	{
		return completionItemCount > 0 ? completionItemCount : 1;
	}

	/**
	 * Returns all object IDs to highlight, combining objectId and objectIds.
	 */
	public Set<Integer> getAllObjectIds()
	{
		Set<Integer> ids = new HashSet<>();
		if (objectId > 0)
		{
			ids.add(objectId);
		}
		if (objectIds != null)
		{
			for (int id : objectIds)
			{
				if (id > 0)
				{
					ids.add(id);
				}
			}
		}
		return ids.isEmpty() ? Collections.emptySet() : ids;
	}
}
