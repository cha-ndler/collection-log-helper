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
package com.collectionloghelper.data;

import java.util.List;
import lombok.Value;

/**
 * A single tier in a prioritized item-to-object mapping. Used for activities where
 * the player has consumable items (e.g., keys, tokens) that correspond to specific
 * interactable objects (e.g., chests, doors), and there is a preferred order of use.
 *
 * Tiers are evaluated lowest-first: the first tier where the player has any item
 * in inventory gets its objects highlighted and the matching item highlighted.
 *
 * Examples: shade keys → chests, skilling tokens → reward shops, clue steps → dig sites.
 */
@Value
public class ItemObjectTier
{
	/** Display name for this tier (e.g., "Bronze", "Steel", "Gold"). */
	String name;

	/** Item IDs that belong to this tier (e.g., all 5 color variants of a bronze shade key). */
	List<Integer> itemIds;

	/** Object IDs to highlight when this tier is active (e.g., all 5 bronze chest variants). */
	List<Integer> objectIds;

	/** Action text to display on highlighted objects (e.g., "Open"). Null = use step default. */
	String interactAction;
}
