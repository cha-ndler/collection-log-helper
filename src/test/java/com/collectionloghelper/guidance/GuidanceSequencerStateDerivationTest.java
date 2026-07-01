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
package com.collectionloghelper.guidance;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies mid-activity state-derivation on guidance activation (#719): when the
 * player activates guidance while already standing in a later step's confirmable
 * area AND holding that step's required items, the sequencer starts them at that
 * step instead of step 1. The conservative safety case — ambiguous state with no
 * confirmable area / missing items — falls back to step 1 unchanged.
 *
 * <p>Models a minimal Shades-of-Mort'ton sequence: a bank step, three transit
 * steps, and a final loop step gated to the catacomb zone whose required items
 * are the shade keys.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequencerStateDerivationTest
{
	private static final int KEY = 3450;
	private static final int BANK_TILE_X = 3488;

	// Catacomb zone bounds the open-chests loop step is gated to.
	private static final int CAT_MIN_X = 3490;
	private static final int CAT_MIN_Y = 9620;
	private static final int CAT_MAX_X = 3520;
	private static final int CAT_MAX_Y = 9650;
	private static final int CAT_PLANE = 0;

	private static final int LOOP_STEP_INDEX = 4;

	@Mock
	private PlayerInventoryState inventoryState;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;

	private GuidanceSequencer sequencer;

	@BeforeEach
	public void setUp() throws Exception
	{
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasEquippedItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class,
			com.collectionloghelper.guidance.bosses.BossGuidanceRegistry.class, com.collectionloghelper.sailing.SailingDockResolver.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker, null, null);
	}

	// ---- Tests ----

	@Test
	public void startsAtLoopStepWhenInCatacombHoldingKeys()
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		// Player is inside the catacomb zone and holds a shade key.
		when(inventoryState.hasItem(KEY)).thenReturn(true);
		sequencer.setPlayerLocation(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, CAT_PLANE));

		sequencer.startSequence(makeSource(shadesLikeSteps()), lastStep::set, () -> { });

		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(),
			"in catacomb holding keys should start at the open-chests step");
		assertEquals("Open chests matching your shade keys", lastStep.get().getDescription());
	}

	@Test
	public void fallsBackToStepOneWhenStateAmbiguous()
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		// Player is NOT in the catacomb (unconfirmable for later steps) and holds
		// no keys — no later step's preconditions can be confirmed.
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));

		sequencer.startSequence(makeSource(shadesLikeSteps()), lastStep::set, () -> { });

		assertEquals(0, sequencer.getCurrentIndex(),
			"ambiguous state must conservatively start at step 1");
		assertEquals("Bank: bring keys", lastStep.get().getDescription());
	}

	@Test
	public void doesNotJumpWhenInAreaButMissingRequiredItems()
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		// Player is in the catacomb zone but holds NO keys — the loop step's
		// required items are not satisfied, so no forward jump.
		sequencer.setPlayerLocation(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, CAT_PLANE));

		sequencer.startSequence(makeSource(shadesLikeSteps()), lastStep::set, () -> { });

		assertEquals(0, sequencer.getCurrentIndex(),
			"in area but missing required keys must not jump forward");
	}

	// ---- Scenario helpers ----

	private List<GuidanceStep> shadesLikeSteps()
	{
		return Arrays.asList(
			tileStep("Bank: bring keys", Collections.singletonList(KEY),
				CompletionCondition.ARRIVE_AT_TILE, BANK_TILE_X, 0, 0),
			manualStep("Burn remains"),
			manualStep("Pick up key"),
			manualStep("Enter catacombs"),
			catacombLoopStep());
	}

	private CollectionLogSource makeSource(List<GuidanceStep> steps)
	{
		return new CollectionLogSource("Shades-like", CollectionLogCategory.BOSSES, 3000, 3000, 0,
			60, 0, "Shades-like", Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			steps, null, null, 0, null, 0, Collections.emptyList(), null, null, null);
	}

	private GuidanceStep manualStep(String desc)
	{
		return tileStep(desc, null, CompletionCondition.MANUAL, 0, 0, 0);
	}

	/** The final open-chests loop step: zone-gated to the catacomb, requires keys. */
	private GuidanceStep catacombLoopStep()
	{
		return buildStep("Open chests matching your shade keys", Collections.singletonList(KEY),
			CompletionCondition.ARRIVE_AT_ZONE, 0, 0,
			new int[] {CAT_MIN_X, CAT_MIN_Y, CAT_MAX_X, CAT_MAX_Y, CAT_PLANE});
	}

	private GuidanceStep tileStep(String description, List<Integer> requiredItemIds,
		CompletionCondition condition, int worldX, int worldY, int worldPlane)
	{
		return buildStep(description, requiredItemIds, condition, worldX, worldY, null);
	}

	private GuidanceStep buildStep(String description, List<Integer> requiredItemIds,
		CompletionCondition condition, int worldX, int worldY, int[] completionZone)
	{
		return new GuidanceStep(
			description,
			null,                  // perItemStepDescription
			worldX, worldY, 0,     // worldX, worldY, worldPlane
			0, null, null, null,   // npcId, perItemNpcId, interactAction, dialogOptions
			null, requiredItemIds, // travelTip, requiredItemIds
			null,                  // perItemRequiredItemIds
			null,                  // recommendedItemIds
			null,                  // perItemRecommendedItemIds
			condition,
			0, 0, 0, 0,            // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,                  // completionNpcIds
			null,                  // worldMessage
			0, null, null,         // objectId, objectIds, objectInteractAction
			null, null,            // highlightItemIds, groundItemIds
			null,                  // completionChatPattern
			0, 0,                  // completionVarbitId, completionVarbitValue
			false,                 // useItemOnObject
			0,                     // objectMaxDistance
			null,                  // objectFilterTiles
			null,                  // highlightWidgetIds
			0, 0,                  // loopBackToStep, loopCount
			null,                  // skipIfHasAnyItemIds
			null,                  // dynamicItemObjectTiers
			completionZone,        // completionZone
			null,                  // conditionalAlternatives
			null,                  // section
			null,                  // waypoints
			null,                  // dynamicTargetEvaluator
			null,                  // conditionTree
			null,                  // perItemStepPriority
			null,                  // activityObtainableItemIds
			null);                 // restockIfMissingAllItemIds
	}
}
