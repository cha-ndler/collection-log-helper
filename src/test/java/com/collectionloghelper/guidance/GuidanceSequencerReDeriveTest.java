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
import java.util.concurrent.atomic.AtomicInteger;
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
 * Reference matrix for the state-driven loop fix (00-discovery F1/F5 + correct-loop
 * semantics): {@link GuidanceSequencer#reDeriveState()} must, mid-session and without
 * re-activation, recompute the active step as a pure function of
 * {@code (inventory, location, clog-unlock)} for gather-loop sources, while leaving
 * linear sources untouched.
 *
 * <p>Models a minimal Shades-of-Mort'ton loop: bank -> burn pyres (zone, needs burn
 * materials) -> pick up key (manual) -> enter catacombs (manual) -> open chests (zone,
 * needs keys + declares key loop-fuel). The open-chests step's
 * {@code restockIfMissingAllItemIds} is the opt-in signal that marks this a gather loop.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequencerReDeriveTest
{
	private static final int LOG = 3438;
	private static final int REMAINS = 3406;
	private static final int TINDERBOX = 590;
	private static final int KEY = 3450;
	private static final List<Integer> BURN_MATERIALS = Arrays.asList(LOG, REMAINS, TINDERBOX);

	private static final int BANK_TILE_X = 3488;

	// Funeral-pyre zone (step 2) the burn step is gated to.
	private static final int PYRE_MIN_X = 3500;
	private static final int PYRE_MIN_Y = 3290;
	private static final int PYRE_MAX_X = 3510;
	private static final int PYRE_MAX_Y = 3300;

	// Catacomb zone (step 5) the open-chests loop step is gated to.
	private static final int CAT_MIN_X = 3490;
	private static final int CAT_MIN_Y = 9620;
	private static final int CAT_MAX_X = 3520;
	private static final int CAT_MAX_Y = 9650;
	private static final int PLANE = 0;

	private static final int BURN_STEP_INDEX = 1;
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

	// ---- Forward / backward re-derivation (the loop) ----

	@Test
	public void reDerivesToBurnStepWhenPreparedAtPyres()
	{
		// Start ambiguous (away, empty) so activation parks at the bank step.
		startAt(new WorldPoint(0, 0, 0));
		assertEquals(0, sequencer.getCurrentIndex(), "precondition: ambiguous start at bank");

		// Player banks materials and walks to the pyres -> should resolve to burn step.
		holds(BURN_MATERIALS);
		sequencer.setPlayerLocation(new WorldPoint(PYRE_MIN_X + 2, PYRE_MIN_Y + 2, PLANE));
		sequencer.reDeriveState();

		assertEquals(BURN_STEP_INDEX, sequencer.getCurrentIndex(),
			"prepared with burn materials at the pyres should resolve to the burn step");
	}

	@Test
	public void reDerivesToChestStepWhenHoldingKeysInCatacomb()
	{
		startAt(new WorldPoint(0, 0, 0));

		// Player now holds keys (no burn materials) and is in the catacomb.
		holds(Collections.singletonList(KEY));
		sequencer.setPlayerLocation(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, PLANE));
		sequencer.reDeriveState();

		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(),
			"holding keys in the catacomb should resolve to the open-chests loop step");
	}

	@Test
	public void reDerivesBackToBankWhenLastKeyConsumed()
	{
		// Player begins in the catacomb holding a key -> activation lands on chests.
		holds(Collections.singletonList(KEY));
		startAt(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, PLANE));
		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(), "precondition: started at chests");

		// They open the last chest: no keys, no materials, no longer anywhere confirmable.
		holdsNothing();
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		sequencer.reDeriveState();

		assertEquals(0, sequencer.getCurrentIndex(),
			"with no keys and no materials the loop must resolve back to the bank step");
	}

	// ---- Back-compat + economy guards ----

	@Test
	public void linearSourceIsNotReDerived()
	{
		AtomicInteger notifications = new AtomicInteger();
		// Linear source: identical steps but WITHOUT the loop-fuel opt-in signal.
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		sequencer.startSequence(makeSource(linearShadesLikeSteps()),
			s -> notifications.incrementAndGet(), () -> { });
		int startNotifications = notifications.get();

		// A world-state that WOULD jump a gather-loop source forward.
		holds(Collections.singletonList(KEY));
		sequencer.setPlayerLocation(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, PLANE));
		sequencer.reDeriveState();

		assertEquals(0, sequencer.getCurrentIndex(),
			"a linear source (no loop fuel) must not be re-derived mid-session");
		assertEquals(startNotifications, notifications.get(),
			"a no-op re-derivation must not notify listeners");
	}

	@Test
	public void noNotificationWhenResolvedStepUnchanged()
	{
		AtomicInteger notifications = new AtomicInteger();
		holds(Collections.singletonList(KEY));
		sequencer.setPlayerLocation(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, PLANE));
		sequencer.startSequence(makeSource(gatherShadesLikeSteps()),
			s -> notifications.incrementAndGet(), () -> { });
		int afterStart = notifications.get();

		// State unchanged -> resolves to the same step -> must not re-notify.
		sequencer.reDeriveState();

		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex());
		assertEquals(afterStart, notifications.get(),
			"re-deriving to the same step must not fire a redundant step-change");
	}

	@Test
	public void doesNotReDeriveAfterTargetSlotUnlocked()
	{
		holds(Collections.singletonList(KEY));
		startAt(new WorldPoint(CAT_MIN_X + 5, CAT_MIN_Y + 5, PLANE));
		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex());

		// Target clog slot unlocked: termination is owned by onItemObtained; the
		// re-derivation path must yield so it cannot resurrect a completed source.
		setTargetSlotUnlocked();
		holdsNothing();
		sequencer.setPlayerLocation(new WorldPoint(0, 0, 0));
		sequencer.reDeriveState();

		assertEquals(LOOP_STEP_INDEX, sequencer.getCurrentIndex(),
			"after target unlock the re-derivation must no-op");
	}

	// ---- Helpers ----

	private void startAt(WorldPoint location)
	{
		AtomicReference<GuidanceStep> lastStep = new AtomicReference<>();
		sequencer.setPlayerLocation(location);
		sequencer.startSequence(makeSource(gatherShadesLikeSteps()), lastStep::set, () -> { });
	}

	private void holds(List<Integer> itemIds)
	{
		holdsNothing();
		for (int id : itemIds)
		{
			when(inventoryState.hasItem(id)).thenReturn(true);
		}
	}

	private void holdsNothing()
	{
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
	}

	private void setTargetSlotUnlocked()
	{
		try
		{
			java.lang.reflect.Field f = GuidanceSequencer.class.getDeclaredField("targetSlotUnlocked");
			f.setAccessible(true);
			f.setBoolean(sequencer, true);
		}
		catch (ReflectiveOperationException e)
		{
			throw new RuntimeException(e);
		}
	}

	/** Gather-loop steps: the open-chests step declares key loop-fuel (opt-in signal). */
	private List<GuidanceStep> gatherShadesLikeSteps()
	{
		return Arrays.asList(
			tileStep("Bank: gather pyre logs, shade remains, a tinderbox", null,
				CompletionCondition.ARRIVE_AT_TILE, BANK_TILE_X, 0),
			zoneStep("Burn shade remains on the funeral pyres", BURN_MATERIALS,
				new int[] {PYRE_MIN_X, PYRE_MIN_Y, PYRE_MAX_X, PYRE_MAX_Y, PLANE}, null),
			manualStep("Pick up the shade keys you earned"),
			manualStep("Enter the catacombs"),
			zoneStep("Open chests matching your shade keys", Collections.singletonList(KEY),
				new int[] {CAT_MIN_X, CAT_MIN_Y, CAT_MAX_X, CAT_MAX_Y, PLANE},
				Collections.singletonList(KEY)));
	}

	/** Identical shape but no loop-fuel -> linear source (not opted into re-derivation). */
	private List<GuidanceStep> linearShadesLikeSteps()
	{
		return Arrays.asList(
			tileStep("Bank: gather pyre logs, shade remains, a tinderbox", null,
				CompletionCondition.ARRIVE_AT_TILE, BANK_TILE_X, 0),
			zoneStep("Burn shade remains on the funeral pyres", BURN_MATERIALS,
				new int[] {PYRE_MIN_X, PYRE_MIN_Y, PYRE_MAX_X, PYRE_MAX_Y, PLANE}, null),
			manualStep("Pick up the shade keys you earned"),
			manualStep("Enter the catacombs"),
			zoneStep("Open chests matching your shade keys", Collections.singletonList(KEY),
				new int[] {CAT_MIN_X, CAT_MIN_Y, CAT_MAX_X, CAT_MAX_Y, PLANE}, null));
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
		return tileStep(desc, null, CompletionCondition.MANUAL, 0, 0);
	}

	private GuidanceStep tileStep(String description, List<Integer> requiredItemIds,
		CompletionCondition condition, int worldX, int worldY)
	{
		return buildStep(description, requiredItemIds, condition, worldX, worldY, null, null);
	}

	private GuidanceStep zoneStep(String description, List<Integer> requiredItemIds,
		int[] completionZone, List<Integer> restockFuel)
	{
		return buildStep(description, requiredItemIds, CompletionCondition.ARRIVE_AT_ZONE,
			0, 0, completionZone, restockFuel);
	}

	private GuidanceStep buildStep(String description, List<Integer> requiredItemIds,
		CompletionCondition condition, int worldX, int worldY, int[] completionZone,
		List<Integer> restockFuel)
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
			restockFuel);          // restockIfMissingAllItemIds
	}
}
