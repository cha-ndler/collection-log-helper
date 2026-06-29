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
import java.lang.reflect.Field;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies loop-fuel depletion / restock-latch behaviour (#719): when a looping
 * step declares {@code restockIfMissingAllItemIds} and the player holds none of
 * those consumables, guidance parks on the earliest step whose required items the
 * player is missing and stays there (ignoring location-based auto-advance) until
 * the player has restocked.
 *
 * <p>Models a minimal Shades-of-Mort'ton sequence: a location-gated bank step,
 * three transit steps, and a final {@code MANUAL} loop step carrying the restock
 * fuel list (keys + remains + logs, no reusable tinderbox).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequencerRestockTest
{
	private static final int TINDERBOX = 590;
	private static final int REMAINS = 3404;
	private static final int LOGS = 3446;
	private static final int KEY = 3450;
	private static final int BANK_TILE_X = 3488;
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
			com.collectionloghelper.guidance.bosses.BossGuidanceRegistry.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker, null);
	}

	// ---- Tests ----

	@Test
	public void depletionResetsToBankStepAndLatches() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		// Player keeps the reusable tinderbox but has no remains/logs/keys.
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);
		startAtLoopStep(lastStep);

		sequencer.onInventoryChanged();

		assertEquals(0, sequencer.getCurrentIndex(), "depleted loop should reset to the bank step");
		assertTrue(awaitingRestock(), "should latch awaiting-restock");
		assertEquals("Bank: bring remains, logs, tinderbox", lastStep.get().getDescription());
	}

	@Test
	public void latchSuppressesArriveAtTileAutoAdvance() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);
		startAtLoopStep(lastStep);
		sequencer.onInventoryChanged(); // -> parked on bank step, latched

		// Player is standing on the bank tile; without the latch ARRIVE_AT_TILE
		// would advance off the bank step immediately.
		sequencer.onPlayerMoved(new WorldPoint(BANK_TILE_X, 0, 0));

		assertEquals(0, sequencer.getCurrentIndex(), "latch must keep guidance on the bank step");
		assertTrue(awaitingRestock());
	}

	@Test
	public void restockingReleasesLatchAndResumes() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		when(inventoryState.hasItem(TINDERBOX)).thenReturn(true);
		startAtLoopStep(lastStep);
		sequencer.onInventoryChanged(); // parked + latched
		assertTrue(awaitingRestock());

		// Player re-banks: now holds all bank-step required items.
		when(inventoryState.hasItem(REMAINS)).thenReturn(true);
		when(inventoryState.hasItem(LOGS)).thenReturn(true);
		sequencer.onInventoryChanged();

		assertFalse(awaitingRestock(), "latch should clear once restocked");
		assertEquals(0, sequencer.getCurrentIndex(), "remains on bank step, ready to proceed");
	}

	@Test
	public void noResetWhileFuelRemains() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		// Still holding a key — loop is not depleted.
		when(inventoryState.hasItem(KEY)).thenReturn(true);
		startAtLoopStep(lastStep);

		sequencer.onInventoryChanged();

		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(),
			"should stay on the loop step while fuel remains");
		assertFalse(awaitingRestock());
	}

	/**
	 * #707: in a looping / restock activity the gather step highlights a recurring
	 * item (the shade keys). Obtaining the first key must NOT auto-advance off that
	 * step via {@code skipIfHasAnyItemIds}, so the in-game highlight persists while
	 * the player keeps collecting. The gather step stays active (its highlight target
	 * unchanged) until the player advances manually or the loop re-enters it.
	 */
	@Test
	public void recurringGatherStepKeepsHighlightAfterFirstPickup() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		sequencer.startSequence(makeSource(gatherLoopSteps()), lastStep::set, () -> { });

		// Precondition: parked on the gather step (index 0) highlighting the keys.
		assertEquals(0, sequencer.getCurrentIndex(), "precondition: on the gather step");
		assertEquals(Collections.singletonList(KEY), lastStep.get().getGroundItemIds(),
			"precondition: gather step highlights the recurring key");

		// Player picks up ONE key — skipIfHasAnyItemIds would normally auto-advance.
		when(inventoryState.hasItem(KEY)).thenReturn(true);
		sequencer.onInventoryChanged();

		assertEquals(0, sequencer.getCurrentIndex(),
			"recurring gather step must stay active so its highlight persists past the first pickup");
		assertEquals(Collections.singletonList(KEY), lastStep.get().getGroundItemIds(),
			"highlight target should remain the recurring key");
	}

	/**
	 * #822: the recurring-gather MANUAL step deliberately suppresses auto-advance, which
	 * reads as "stuck". {@code getActiveStepHint()} must surface a "Keep gathering - press
	 * Next" hint on that step so the deliberate pause is not mistaken for a hang.
	 */
	@Test
	public void recurringGatherStepSurfacesKeepGatheringHint() throws Exception
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		sequencer.startSequence(makeSource(gatherLoopSteps()), lastStep::set, () -> { });

		assertEquals(0, sequencer.getCurrentIndex(), "precondition: on the recurring gather step");
		String hint = sequencer.getActiveStepHint();
		assertTrue(hint != null && hint.contains("Keep gathering"),
			"recurring-gather MANUAL step should surface a 'Keep gathering' hint (#822), got: " + hint);
	}

	// ---- Scenario helpers ----

	private void startAtLoopStep(AtomicReference<GuidanceStep> lastStep)
	{
		// Player is at the bank tile but holds nothing, so the bank step does not
		// auto-complete on start; manually advance to the loop step.
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		CollectionLogSource source = makeSource(shadesLikeSteps());
		sequencer.startSequence(source, lastStep::set, () -> { });
		for (int i = 0; i < LOOP_STEP_INDEX; i++)
		{
			sequencer.advanceStep();
		}
		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(), "precondition: parked on the loop step");
	}

	/**
	 * Minimal looping gather sequence (#707): a gather step that highlights the
	 * recurring key on the ground and declares {@code skipIfHasAnyItemIds}, followed
	 * by a MANUAL loop step carrying the restock fuel (which marks the whole sequence
	 * as a looping / restock activity).
	 */
	private List<GuidanceStep> gatherLoopSteps()
	{
		return Arrays.asList(
			gatherStep("Pick up your shade key", Collections.singletonList(KEY)),
			step("Open chests matching your shade keys", null,
				CompletionCondition.MANUAL, 0, 1, 50, Collections.singletonList(KEY)));
	}

	/**
	 * Builds a gather step that highlights {@code keyIds} on the ground and skips
	 * itself once the player holds any of them. Used to model #707's recurring
	 * pickup step.
	 */
	private GuidanceStep gatherStep(String description, List<Integer> keyIds)
	{
		return new GuidanceStep(
			description,
			null,                  // perItemStepDescription
			0, 0, 0,               // worldX, worldY, worldPlane
			0, null, null, null,   // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,            // travelTip, requiredItemIds
			null,                  // perItemRequiredItemIds
			null,                  // recommendedItemIds
			null,                  // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,            // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,                  // completionNpcIds
			null,                  // worldMessage
			0, null, null,         // objectId, objectIds, objectInteractAction
			null, keyIds,          // highlightItemIds, groundItemIds
			null,                  // completionChatPattern
			0, 0,                  // completionVarbitId, completionVarbitValue
			false,                 // useItemOnObject
			0,                     // objectMaxDistance
			null,                  // objectFilterTiles
			null,                  // highlightWidgetIds
			0, 0,                  // loopBackToStep, loopCount
			keyIds,                // skipIfHasAnyItemIds
			null,                  // dynamicItemObjectTiers
			null,                  // completionZone
			null,                  // conditionalAlternatives
			null,                  // section
			null,                  // waypoints
			null,                  // dynamicTargetEvaluator
			null,                  // conditionTree
			null,                  // perItemStepPriority
			null,                  // activityObtainableItemIds
			null);                 // restockIfMissingAllItemIds
	}

	private List<GuidanceStep> shadesLikeSteps()
	{
		return Arrays.asList(
			step("Bank: bring remains, logs, tinderbox", Arrays.asList(TINDERBOX, REMAINS, LOGS),
				CompletionCondition.ARRIVE_AT_TILE, BANK_TILE_X, 0, 0, null),
			manualStep("Burn remains"),
			manualStep("Pick up key"),
			manualStep("Enter catacombs"),
			step("Open chests matching your shade keys", null,
				CompletionCondition.MANUAL, 0, 2, 50, Arrays.asList(KEY, REMAINS, LOGS)));
	}

	private boolean awaitingRestock() throws Exception
	{
		Field f = GuidanceSequencer.class.getDeclaredField("awaitingRestock");
		f.setAccessible(true);
		return f.getBoolean(sequencer);
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
		return step(desc, null, CompletionCondition.MANUAL, 0, 0, 0, null);
	}

	private GuidanceStep step(String description, List<Integer> requiredItemIds,
		CompletionCondition condition, int worldX, int loopBackToStep, int loopCount,
		List<Integer> restockIfMissingAllItemIds)
	{
		return new GuidanceStep(
			description,
			null,                  // perItemStepDescription
			worldX, 0, 0,          // worldX, worldY, worldPlane
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
			loopBackToStep, loopCount,
			null,                  // skipIfHasAnyItemIds
			null,                  // dynamicItemObjectTiers
			null,                  // completionZone
			null,                  // conditionalAlternatives
			null,                  // section
			null,                  // waypoints
			null,                  // dynamicTargetEvaluator
			null,                  // conditionTree
			null,                  // perItemStepPriority
			null,                  // activityObtainableItemIds
			restockIfMissingAllItemIds);
	}
}
