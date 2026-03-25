package com.collectionloghelper.data;

import lombok.Value;

/**
 * A conditional alternative for a guidance step. When a step has conditional alternatives,
 * the sequencer evaluates each alternative's requirements in order and uses the first one
 * whose requirements are met. Only non-null fields override the base step — null fields
 * fall through to the parent step's values.
 */
@Value
public class ConditionalAlternative
{
	/** Quest/skill requirements that must be met for this alternative to activate. */
	SourceRequirements requirements;

	/** Override description (null = use parent step's). */
	String description;

	/** Override world coordinates (null = use parent step's). */
	Integer worldX;
	Integer worldY;
	Integer worldPlane;

	/** Override travel tip (null = use parent step's). */
	String travelTip;

	/** Override NPC ID (null = use parent step's). */
	Integer npcId;

	/** Override interact action (null = use parent step's). */
	String interactAction;

	/** Override object ID (null = use parent step's). */
	Integer objectId;

	/** Override completion condition (null = use parent step's). */
	CompletionCondition completionCondition;

	/** Override completion distance (null = use parent step's). */
	Integer completionDistance;

	/** Override completion NPC ID (null = use parent step's). */
	Integer completionNpcId;
}
