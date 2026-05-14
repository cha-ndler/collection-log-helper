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
package com.collectionloghelper.guidance.helper;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Pilot Java helper for Cerberus (D-01 hybrid, B6).
 *
 * <p>This PR mirrors Cerberus's existing JSON guidance steps in Java code to
 * prove that the helper plumbing works end-to-end. The step data is
 * intentionally identical to the JSON so that no behaviour changes occur when
 * the helper key is absent (plumbing-only milestone).
 *
 * <p>{@link #isStepSatisfied} always returns {@code false}, delegating all
 * satisfaction evaluation to the default
 * {@link com.collectionloghelper.guidance.GuidanceSequencer} logic.
 */
@Singleton
public class CerberusHelper implements GuidanceHelper
{
	/** NPC ID for Cerberus (all combat variants). */
	static final int CERBERUS_NPC_ID = 5862;

	/** Object ID for the Iron Winch gate in the Cerberus arena. */
	static final int IRON_WINCH_OBJECT_ID = 23104;

	/** Recommended item IDs: Prayer potion (4), Super combat potion (4), Antidote++ (4). */
	static final List<Integer> RECOMMENDED_ITEMS = Collections.unmodifiableList(
		Arrays.asList(2434, 3024, 12695));

	@Inject
	CerberusHelper()
	{
	}

	@Override
	public List<GuidanceStep> getSteps(Client client, ClientThread clientThread)
	{
		GuidanceStep travelStep = new GuidanceStep(
			"Use a Key master teleport or teleport to your house in Taverley",
			null,              // perItemStepDescription
			1310, 1251, 0,     // worldX, worldY, worldPlane (dungeon entrance)
			0, null,           // npcId, perItemNpcId
			null,              // interactAction
			null,              // dialogOptions
			"Key master teleport (fastest), or POH Taverley portal + navigate dungeon",
			null,              // requiredItemIds
			null,              // perItemRequiredItemIds
			null,              // recommendedItemIds
			null,              // perItemRecommendedItemIds
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 8, 0,        // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,              // completionNpcIds
			null,              // worldMessage
			0, null, null,     // objectId, objectIds, objectInteractAction
			null, null,        // highlightItemIds, groundItemIds
			null,              // completionChatPattern
			0, 0,              // completionVarbitId, completionVarbitValue
			false,             // useItemOnObject
			0,                 // objectMaxDistance
			null,              // objectFilterTiles
			null,              // highlightWidgetIds
			0, 0,              // loopBackToStep, loopCount
			null,              // skipIfHasAnyItemIds
			null,              // dynamicItemObjectTiers
			null,              // completionZone
			null,              // conditionalAlternatives
			"Travel",          // section
			null,              // waypoints
			null               // dynamicTargetEvaluator
		);

		GuidanceStep gateStep = new GuidanceStep(
			"Turn an Iron Winch to open a gate and enter the boss arena."
				+ " Requires a hellhound Slayer task",
			null,              // perItemStepDescription
			1291, 1254, 0,     // worldX, worldY, worldPlane (winch location)
			0, null,           // npcId, perItemNpcId
			null,              // interactAction
			null,              // dialogOptions
			null,              // travelTip
			null,              // requiredItemIds
			null,              // perItemRequiredItemIds
			null,              // recommendedItemIds
			null,              // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,        // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,              // completionNpcIds
			null,              // worldMessage
			IRON_WINCH_OBJECT_ID, null, "Turn",  // objectId, objectIds, objectInteractAction
			null, null,        // highlightItemIds, groundItemIds
			null,              // completionChatPattern
			0, 0,              // completionVarbitId, completionVarbitValue
			false,             // useItemOnObject
			0,                 // objectMaxDistance
			null,              // objectFilterTiles
			null,              // highlightWidgetIds
			0, 0,              // loopBackToStep, loopCount
			null,              // skipIfHasAnyItemIds
			null,              // dynamicItemObjectTiers
			null,              // completionZone
			null,              // conditionalAlternatives
			"Travel",          // section
			null,              // waypoints
			null               // dynamicTargetEvaluator
		);

		GuidanceStep killStep = new GuidanceStep(
			"Kill Cerberus. Protect from Magic, watch for the triple ghost special attack",
			null,              // perItemStepDescription
			1240, 1251, 0,     // worldX, worldY, worldPlane (arena centre)
			CERBERUS_NPC_ID, null,  // npcId, perItemNpcId
			"Attack",          // interactAction
			null,              // dialogOptions
			null,              // travelTip
			null,              // requiredItemIds
			null,              // perItemRequiredItemIds
			RECOMMENDED_ITEMS, // recommendedItemIds
			null,              // perItemRecommendedItemIds
			CompletionCondition.ACTOR_DEATH,
			0, 0, 0, CERBERUS_NPC_ID,  // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,              // completionNpcIds
			null,              // worldMessage
			0, null, null,     // objectId, objectIds, objectInteractAction
			null, null,        // highlightItemIds, groundItemIds
			null,              // completionChatPattern
			0, 0,              // completionVarbitId, completionVarbitValue
			false,             // useItemOnObject
			0,                 // objectMaxDistance
			null,              // objectFilterTiles
			null,              // highlightWidgetIds
			0, 0,              // loopBackToStep, loopCount
			null,              // skipIfHasAnyItemIds
			null,              // dynamicItemObjectTiers
			null,              // completionZone
			null,              // conditionalAlternatives
			"Combat",          // section
			null,              // waypoints
			null               // dynamicTargetEvaluator
		);

		return Arrays.asList(travelStep, gateStep, killStep);
	}

	/**
	 * Always returns {@code false}, delegating satisfaction evaluation to the
	 * default sequencer logic. This is intentional for the B6 plumbing pilot
	 * — future iterations may add richer Cerberus-specific checks here.
	 */
	@Override
	public boolean isStepSatisfied(Client client, GuidanceStep step, int stepIndex)
	{
		return false;
	}
}
