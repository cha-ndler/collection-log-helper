/*
 * Copyright (c) 2025, Chandler <ch@ndler.net>
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
