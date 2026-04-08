/*
 * Copyright (c) 2025, Chandler
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
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.ConditionalAlternative;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SkillRequirement;
import com.collectionloghelper.data.SourceRequirements;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuidanceSequencerTest
{
	@Mock
	private PlayerInventoryState inventoryState;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;

	private GuidanceSequencer sequencer;

	@Before
	public void setUp() throws Exception
	{
		// Default: inventory is empty, no items obtained
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasAllItems(org.mockito.ArgumentMatchers.anyList())).thenReturn(true);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker);
	}

	// ---- Helper methods ----

	private GuidanceStep makeManualStep(String description)
	{
		return new GuidanceStep(
			description,
			0, 0, 0,       // worldX, worldY, worldPlane
			0, null, null,  // npcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,    // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,           // completionNpcIds
			null,           // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			0, 0,           // completionVarbitId, completionVarbitValue
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			null,           // highlightWidgetIds
			0, 0,           // loopBackToStep, loopCount
			null,           // skipIfHasAnyItemIds
			null,           // dynamicItemObjectTiers
			null,           // completionZone
			null            // conditionalAlternatives
		);
	}

	private GuidanceStep makeStep(CompletionCondition condition, int completionItemId)
	{
		return new GuidanceStep(
			"Step: " + condition.name(),
			0, 0, 0,
			0, null, null,
			null, null,
			condition,
			completionItemId, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeInventoryHasItemStep(int itemId, int count)
	{
		return new GuidanceStep(
			"Collect " + count + " items",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.INVENTORY_HAS_ITEM,
			itemId, count, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeArriveStep(int x, int y, int plane, int distance)
	{
		return new GuidanceStep(
			"Walk to location",
			x, y, plane,
			0, null, null,
			null, null,
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, distance, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeNpcTalkedToStep(int npcId)
	{
		return new GuidanceStep(
			"Talk to NPC",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.NPC_TALKED_TO,
			0, 0, 0, npcId,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeActorDeathStep(int npcId)
	{
		return new GuidanceStep(
			"Kill NPC",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.ACTOR_DEATH,
			0, 0, 0, npcId,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeChatMessageStep(String pattern)
	{
		return new GuidanceStep(
			"Wait for chat",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.CHAT_MESSAGE_RECEIVED,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			pattern,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeLoopingStep(String description, CompletionCondition condition,
		int completionNpcId, int loopBackToStep, int loopCount)
	{
		return new GuidanceStep(
			description,
			0, 0, 0,
			0, null, null,
			null, null,
			condition,
			0, 0, 0, completionNpcId,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			loopBackToStep, loopCount,
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makePlaneStep(int plane)
	{
		return new GuidanceStep(
			"Go to plane " + plane,
			0, 0, plane,
			0, null, null,
			null, null,
			CompletionCondition.PLAYER_ON_PLANE,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeVarbitStep(int varbitId, int varbitValue)
	{
		return new GuidanceStep(
			"Wait for varbit",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.VARBIT_AT_LEAST,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			varbitId, varbitValue,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private GuidanceStep makeStepWithRequiredItems(List<Integer> requiredItemIds)
	{
		return new GuidanceStep(
			"Step needing items",
			0, 0, 0,
			0, null, null,
			null, requiredItemIds,
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private CollectionLogSource makeSource(String name, List<GuidanceStep> steps)
	{
		return makeSource(name, steps, 0);
	}

	private CollectionLogSource makeSource(String name, List<GuidanceStep> steps, int cumulativeTrackThreshold)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 3000, 3000, 0,
			60, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			steps, null, 0, null, cumulativeTrackThreshold, Collections.emptyList());
	}

	private void startSequence(List<GuidanceStep> steps)
	{
		startSequence(steps, s -> {}, () -> {});
	}

	private void startSequence(List<GuidanceStep> steps, Consumer<GuidanceStep> onStep, Runnable onComplete)
	{
		CollectionLogSource source = makeSource("Test Source", steps);
		sequencer.startSequence(source, onStep, onComplete);
	}

	// ---- Tests ----

	@Test
	public void testStartSequenceSetsActiveState()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Step 1"),
			makeManualStep("Step 2")
		);

		assertFalse(sequencer.isActive());
		startSequence(steps);
		assertTrue(sequencer.isActive());
		assertEquals(0, sequencer.getCurrentIndex());
		assertEquals(2, sequencer.getTotalSteps());
		assertNotNull(sequencer.getCurrentStep());
		assertEquals("Step 1", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testStopSequenceClearsState()
	{
		startSequence(Arrays.asList(makeManualStep("Step 1")));
		assertTrue(sequencer.isActive());

		sequencer.stopSequence();
		assertFalse(sequencer.isActive());
		assertNull(sequencer.getCurrentStep());
		assertEquals(0, sequencer.getTotalSteps());
	}

	@Test
	public void testBasicStepProgression()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Step 1"),
			makeManualStep("Step 2"),
			makeManualStep("Step 3")
		);

		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(steps, s -> {}, () -> completed.set(true));

		assertEquals(0, sequencer.getCurrentIndex());
		assertEquals("Step 1", sequencer.getCurrentStep().getDescription());

		sequencer.advanceStep();
		assertEquals(1, sequencer.getCurrentIndex());
		assertEquals("Step 2", sequencer.getCurrentStep().getDescription());

		sequencer.advanceStep();
		assertEquals(2, sequencer.getCurrentIndex());
		assertEquals("Step 3", sequencer.getCurrentStep().getDescription());
		assertFalse(completed.get());

		sequencer.advanceStep();
		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testSingleStepSequence()
	{
		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(Arrays.asList(makeManualStep("Only step")), s -> {}, () -> completed.set(true));

		assertEquals("Only step", sequencer.getCurrentStep().getDescription());

		sequencer.advanceStep();
		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testEmptyStepsCompleteImmediately()
	{
		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(Collections.emptyList(), s -> {}, () -> completed.set(true));

		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testGetCurrentStepReturnsNullWhenInactive()
	{
		assertNull(sequencer.getCurrentStep());
		assertNull(sequencer.getRawCurrentStep());
		assertEquals(0, sequencer.getTotalSteps());
	}

	@Test
	public void testItemObtainedCompletesStep()
	{
		int targetItemId = 12345;
		List<GuidanceStep> steps = Arrays.asList(
			makeStep(CompletionCondition.ITEM_OBTAINED, targetItemId),
			makeManualStep("After obtain")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Wrong item ID should not advance
		sequencer.onItemObtained(99999);
		assertEquals(0, sequencer.getCurrentIndex());

		// Correct item ID advances
		sequencer.onItemObtained(targetItemId);
		assertEquals(1, sequencer.getCurrentIndex());
		assertEquals("After obtain", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testInventoryHasItemCompletesStep()
	{
		int itemId = 555;
		int count = 10;
		List<GuidanceStep> steps = Arrays.asList(
			makeInventoryHasItemStep(itemId, count),
			makeManualStep("Done collecting")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Not enough items yet
		sequencer.onInventoryChanged();
		assertEquals(0, sequencer.getCurrentIndex());

		// Now has enough
		when(inventoryState.hasItemCount(itemId, count)).thenReturn(true);
		sequencer.onInventoryChanged();
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testInventoryNotHasItemCompletesStep()
	{
		int itemId = 777;
		List<GuidanceStep> steps = Arrays.asList(
			makeStep(CompletionCondition.INVENTORY_NOT_HAS_ITEM, itemId),
			makeManualStep("Item gone")
		);

		// Player starts with the item
		when(inventoryState.hasItem(itemId)).thenReturn(true);
		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Item still present
		sequencer.onInventoryChanged();
		assertEquals(0, sequencer.getCurrentIndex());

		// Item removed
		when(inventoryState.hasItem(itemId)).thenReturn(false);
		sequencer.onInventoryChanged();
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testArriveAtTileCompletesStep()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeArriveStep(3000, 3000, 0, 5),
			makeManualStep("Arrived")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Too far away
		sequencer.onPlayerMoved(new WorldPoint(3100, 3100, 0));
		assertEquals(0, sequencer.getCurrentIndex());

		// Wrong plane
		sequencer.onPlayerMoved(new WorldPoint(3000, 3000, 1));
		assertEquals(0, sequencer.getCurrentIndex());

		// Within distance on correct plane
		sequencer.onPlayerMoved(new WorldPoint(3003, 3002, 0));
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testPlayerOnPlaneCompletesStep()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makePlaneStep(2),
			makeManualStep("On plane 2")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Wrong plane
		sequencer.onPlayerMoved(new WorldPoint(3000, 3000, 0));
		assertEquals(0, sequencer.getCurrentIndex());

		// Correct plane
		sequencer.onPlayerMoved(new WorldPoint(3000, 3000, 2));
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testNpcTalkedToCompletesStep()
	{
		int npcId = 1234;
		List<GuidanceStep> steps = Arrays.asList(
			makeNpcTalkedToStep(npcId),
			makeManualStep("Talked")
		);

		startSequence(steps);

		// Wrong NPC
		sequencer.onNpcInteracted(9999);
		assertEquals(0, sequencer.getCurrentIndex());

		// Correct NPC
		sequencer.onNpcInteracted(npcId);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testActorDeathCompletesStep()
	{
		int npcId = 5678;
		List<GuidanceStep> steps = Arrays.asList(
			makeActorDeathStep(npcId),
			makeManualStep("Dead")
		);

		startSequence(steps);

		// Wrong NPC death
		sequencer.onNpcDeath(1111);
		assertEquals(0, sequencer.getCurrentIndex());

		// Correct NPC death
		sequencer.onNpcDeath(npcId);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testChatMessageCompletesStep()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeChatMessageStep("You completed.*quest"),
			makeManualStep("Quest done")
		);

		startSequence(steps);

		// Non-matching message
		sequencer.onChatMessage("Hello world");
		assertEquals(0, sequencer.getCurrentIndex());

		// Matching message
		sequencer.onChatMessage("You completed the quest");
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testChatMessageRegexPartialMatch()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeChatMessageStep("\\d+ coins"),
			makeManualStep("Got coins")
		);

		startSequence(steps);

		sequencer.onChatMessage("You received 500 coins as a reward.");
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testVarbitAtLeastCompletesStep()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeVarbitStep(3975, 40),
			makeManualStep("Enter boss room")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Varbit below threshold — should not advance
		sequencer.onVarbitChanged(3975, 39);
		assertEquals(0, sequencer.getCurrentIndex());

		// Wrong varbit — should not advance
		sequencer.onVarbitChanged(3972, 40);
		assertEquals(0, sequencer.getCurrentIndex());

		// Varbit at threshold — should advance
		sequencer.onVarbitChanged(3975, 40);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testVarbitAtLeastAboveThresholdCompletesStep()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeVarbitStep(3975, 40),
			makeManualStep("Enter boss room")
		);

		startSequence(steps);

		// Varbit above threshold — should also advance
		sequencer.onVarbitChanged(3975, 45);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testActorDeathWithMultipleNpcIds()
	{
		// Simulates Zulrah: NPC can die as any of 3 forms (2042, 2043, 2044)
		GuidanceStep multiFormKillStep = new GuidanceStep(
			"Kill Zulrah",
			0, 0, 0,
			2042, null, null,
			null, null,
			CompletionCondition.ACTOR_DEATH,
			0, 0, 0, 0,
			Arrays.asList(2042, 2043, 2044),  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0, null, null,
			0, 0,
			null,
			null,
			null,
			null
		);

		List<GuidanceStep> steps = Arrays.asList(
			multiFormKillStep,
			makeManualStep("Collect loot")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());

		// Wrong NPC — should not advance
		sequencer.onNpcDeath(9999);
		assertEquals(0, sequencer.getCurrentIndex());

		// Kill as blue form (2043) — should advance
		sequencer.onNpcDeath(2043);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testActorDeathWithMultipleNpcIdsFallsBackToSingleId()
	{
		// When completionNpcIds is null, falls back to completionNpcId
		List<GuidanceStep> steps = Arrays.asList(
			makeActorDeathStep(5862),
			makeManualStep("Collect loot")
		);

		startSequence(steps);

		sequencer.onNpcDeath(5862);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testLoopingBehavior()
	{
		// Steps: 1=Walk, 2=Kill (loops back to 1, 3 times), 3=Done
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Walk to boss"),
			makeLoopingStep("Kill boss", CompletionCondition.ACTOR_DEATH, 100, 1, 3),
			makeManualStep("Collect loot")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());
		assertEquals(0, sequencer.getLoopIterationsCompleted());

		// Advance past step 1
		sequencer.advanceStep();
		assertEquals(1, sequencer.getCurrentIndex());

		// Complete loop iteration 1 -> loops back to step 1 (index 0)
		sequencer.onNpcDeath(100);
		assertEquals(1, sequencer.getLoopIterationsCompleted());
		assertEquals(0, sequencer.getCurrentIndex());

		// Advance past step 1 again
		sequencer.advanceStep();
		assertEquals(1, sequencer.getCurrentIndex());

		// Complete loop iteration 2 -> loops back to step 1
		sequencer.onNpcDeath(100);
		assertEquals(2, sequencer.getLoopIterationsCompleted());
		assertEquals(0, sequencer.getCurrentIndex());

		// Advance past step 1 again
		sequencer.advanceStep();
		assertEquals(1, sequencer.getCurrentIndex());

		// Complete loop iteration 3 -> all iterations done, advance past loop
		sequencer.onNpcDeath(100);
		assertEquals(2, sequencer.getCurrentIndex());
		assertEquals("Collect loot", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testLoopingCompletesSequenceIfLastStep()
	{
		// Loop step is the last step
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Prep"),
			makeLoopingStep("Kill boss", CompletionCondition.ACTOR_DEATH, 100, 1, 2)
		);

		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(steps, s -> {}, () -> completed.set(true));

		sequencer.advanceStep(); // past Prep
		assertEquals(1, sequencer.getCurrentIndex());

		// Iteration 1 -> loop back
		sequencer.onNpcDeath(100);
		assertEquals(0, sequencer.getCurrentIndex());
		assertFalse(completed.get());

		sequencer.advanceStep(); // back to kill step
		// Iteration 2 -> done with loop, advance past -> sequence complete
		sequencer.onNpcDeath(100);
		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testSkipStepBypassesLoop()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Walk"),
			makeLoopingStep("Kill", CompletionCondition.ACTOR_DEATH, 100, 1, 5),
			makeManualStep("Loot")
		);

		startSequence(steps);
		sequencer.advanceStep(); // past Walk
		assertEquals(1, sequencer.getCurrentIndex());

		// Skip bypasses the entire loop
		sequencer.skipStep();
		assertEquals(2, sequencer.getCurrentIndex());
		assertEquals("Loot", sequencer.getCurrentStep().getDescription());
		assertEquals(0, sequencer.getLoopIterationsCompleted());
	}

	@Test
	public void testSkipStepAdvancesToNextUnsatisfied()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Step 1"),
			makeManualStep("Step 2"),
			makeManualStep("Step 3")
		);

		startSequence(steps);
		sequencer.skipStep();
		assertEquals(1, sequencer.getCurrentIndex());
		assertEquals("Step 2", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testSkipLastStepCompletesSequence()
	{
		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(Arrays.asList(makeManualStep("Only")), s -> {}, () -> completed.set(true));

		sequencer.skipStep();
		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testSkipSatisfiedStepsOnStart()
	{
		int itemId = 100;
		// First step has INVENTORY_HAS_ITEM that's already satisfied
		when(inventoryState.hasItemCount(itemId, 1)).thenReturn(true);

		List<GuidanceStep> steps = Arrays.asList(
			makeInventoryHasItemStep(itemId, 1),
			makeManualStep("Second step")
		);

		startSequence(steps);
		// Should skip the already-satisfied first step
		assertEquals(1, sequencer.getCurrentIndex());
		assertEquals("Second step", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testSkipSatisfiedStepsItemObtained()
	{
		int itemId = 200;
		when(collectionState.isItemObtained(itemId)).thenReturn(true);

		List<GuidanceStep> steps = Arrays.asList(
			makeStep(CompletionCondition.ITEM_OBTAINED, itemId),
			makeManualStep("Next")
		);

		startSequence(steps);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testSkipSatisfiedArriveAtTile()
	{
		// Player is already at the target location
		sequencer.setPlayerLocation(new WorldPoint(3000, 3000, 0));

		List<GuidanceStep> steps = Arrays.asList(
			makeArriveStep(3002, 3001, 0, 5),
			makeManualStep("Already there")
		);

		startSequence(steps);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testSkipSatisfiedPlayerOnPlane()
	{
		sequencer.setPlayerLocation(new WorldPoint(3000, 3000, 1));

		List<GuidanceStep> steps = Arrays.asList(
			makePlaneStep(1),
			makeManualStep("On correct plane")
		);

		startSequence(steps);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testActorDeathAndChatNeverPreSatisfied()
	{
		// ACTOR_DEATH and CHAT_MESSAGE_RECEIVED should never be skipped
		List<GuidanceStep> steps = Arrays.asList(
			makeActorDeathStep(100),
			makeChatMessageStep("test"),
			makeManualStep("End")
		);

		startSequence(steps);
		assertEquals(0, sequencer.getCurrentIndex());
	}

	@Test
	public void testAllStepsSatisfiedCompletesImmediately()
	{
		int item1 = 100, item2 = 200;
		when(inventoryState.hasItemCount(item1, 1)).thenReturn(true);
		when(inventoryState.hasItemCount(item2, 1)).thenReturn(true);

		AtomicBoolean completed = new AtomicBoolean(false);
		List<GuidanceStep> steps = Arrays.asList(
			makeInventoryHasItemStep(item1, 1),
			makeInventoryHasItemStep(item2, 1)
		);

		startSequence(steps, s -> {}, () -> completed.set(true));
		assertTrue(completed.get());
		assertFalse(sequencer.isActive());
	}

	@Test
	public void testBankRoutingWhenMissingRequiredItems()
	{
		List<Integer> required = Arrays.asList(100, 200);
		when(inventoryState.hasAllItems(required)).thenReturn(false);

		List<GuidanceStep> steps = Arrays.asList(
			makeStepWithRequiredItems(required),
			makeManualStep("End")
		);

		startSequence(steps);
		GuidanceStep current = sequencer.getCurrentStep();
		// Should return a bank routing step instead
		assertEquals("Get required items from bank", current.getDescription());
		assertEquals(CompletionCondition.MANUAL, current.getCompletionCondition());

		// Raw step should still be the original
		GuidanceStep raw = sequencer.getRawCurrentStep();
		assertEquals("Step needing items", raw.getDescription());
	}

	@Test
	public void testBankRoutingNotAppliedWhenItemsPresent()
	{
		List<Integer> required = Arrays.asList(100, 200);
		when(inventoryState.hasAllItems(required)).thenReturn(true);

		List<GuidanceStep> steps = Arrays.asList(
			makeStepWithRequiredItems(required),
			makeManualStep("End")
		);

		startSequence(steps);
		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Step needing items", current.getDescription());
	}

	@Test
	public void testStepChangedCallbackFired()
	{
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Step 1"),
			makeManualStep("Step 2")
		);

		AtomicReference<String> lastStepDesc = new AtomicReference<>();
		startSequence(steps, s -> lastStepDesc.set(s.getDescription()), () -> {});

		assertEquals("Step 1", lastStepDesc.get());

		sequencer.advanceStep();
		assertEquals("Step 2", lastStepDesc.get());
	}

	@Test
	public void testSequenceCompleteCallbackFired()
	{
		AtomicBoolean completed = new AtomicBoolean(false);
		startSequence(Arrays.asList(makeManualStep("Only")), s -> {}, () -> completed.set(true));

		assertFalse(completed.get());
		sequencer.advanceStep();
		assertTrue(completed.get());
	}

	@Test
	public void testEventsIgnoredWhenInactive()
	{
		// None of these should throw or change state
		sequencer.onItemObtained(123);
		sequencer.onInventoryChanged();
		sequencer.onPlayerMoved(new WorldPoint(0, 0, 0));
		sequencer.onNpcDeath(456);
		sequencer.onChatMessage("test");
		sequencer.onNpcInteracted(789);
		sequencer.advanceStep();
		sequencer.skipStep();
		sequencer.onTrackedAction();

		assertFalse(sequencer.isActive());
		assertNull(sequencer.getCurrentStep());
	}

	@Test
	public void testInventoryNotHasItemSkippedOnStart()
	{
		int itemId = 300;
		// Player doesn't have the item, so INVENTORY_NOT_HAS_ITEM is already satisfied
		when(inventoryState.hasItem(itemId)).thenReturn(false);

		List<GuidanceStep> steps = Arrays.asList(
			makeStep(CompletionCondition.INVENTORY_NOT_HAS_ITEM, itemId),
			makeManualStep("Next")
		);

		startSequence(steps);
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testGetActiveSource()
	{
		CollectionLogSource source = makeSource("Zulrah", Arrays.asList(makeManualStep("Kill")));
		sequencer.startSequence(source, s -> {}, () -> {});

		assertEquals("Zulrah", sequencer.getActiveSource().getName());
	}

	@Test
	public void testCumulativeTrackedAction()
	{
		// 3 steps: prep, kill (loops 10 times), loot
		// Cumulative threshold = 3, should force-complete the loop early
		List<GuidanceStep> steps = Arrays.asList(
			makeManualStep("Prep"),
			makeLoopingStep("Kill", CompletionCondition.ACTOR_DEATH, 100, 1, 10),
			makeManualStep("Loot")
		);

		CollectionLogSource source = makeSource("Test", steps, 3);
		sequencer.startSequence(source, s -> {}, () -> {});

		sequencer.advanceStep(); // past Prep
		assertEquals(1, sequencer.getCurrentIndex());

		assertEquals(0, sequencer.getCumulativeActionCount());
		sequencer.onTrackedAction();
		assertEquals(1, sequencer.getCumulativeActionCount());
		sequencer.onTrackedAction();
		assertEquals(2, sequencer.getCumulativeActionCount());

		// Third action hits threshold, should force-complete the loop
		sequencer.onTrackedAction();
		assertEquals(2, sequencer.getCurrentIndex());
		assertEquals("Loot", sequencer.getCurrentStep().getDescription());
	}

	@Test
	public void testCumulativeTrackThresholdZeroIgnored()
	{
		List<GuidanceStep> steps = Arrays.asList(makeManualStep("Step 1"));
		CollectionLogSource source = makeSource("Test", steps, 0);
		sequencer.startSequence(source, s -> {}, () -> {});

		assertEquals(0, sequencer.getCumulativeTrackThreshold());
		sequencer.onTrackedAction();
		// Should not crash or change state
		assertEquals(0, sequencer.getCurrentIndex());
	}

	@Test
	public void testQuantityCheckDefaultsToOne()
	{
		// completionItemCount = 0 should default to 1 via getter
		int itemId = 400;
		GuidanceStep step = makeStep(CompletionCondition.INVENTORY_HAS_ITEM, itemId);
		assertEquals(1, step.getCompletionItemCount());

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		// Having 1 item should satisfy since default count is 1
		when(inventoryState.hasItemCount(itemId, 1)).thenReturn(true);
		sequencer.onInventoryChanged();
		assertEquals(1, sequencer.getCurrentIndex());
	}

	@Test
	public void testAdvanceStepWhenNoSteps()
	{
		// Sequence already completed (empty steps)
		startSequence(Collections.emptyList());
		// Should not throw
		sequencer.advanceStep();
		sequencer.skipStep();
	}

	// ---- Conditional alternative tests ----

	private SourceRequirements makeQuestRequirement(String questName)
	{
		return new SourceRequirements(Arrays.asList(questName), null);
	}

	private SourceRequirements makeSkillRequirement(String skill, int level)
	{
		return new SourceRequirements(null, Arrays.asList(new SkillRequirement(skill, level)));
	}

	private GuidanceStep makeStepWithAlternatives(String description, int worldX, int worldY,
		String travelTip, List<ConditionalAlternative> alternatives)
	{
		return new GuidanceStep(
			description,
			worldX, worldY, 0,
			0, null, null,
			travelTip, null,
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 5, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null,
			null,
			null,
			alternatives
		);
	}

	@Test
	public void testConditionalAlternativeUsedWhenRequirementsMet()
	{
		SourceRequirements reqs = makeQuestRequirement("LOST_CITY");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs,
			"Use fairy ring",  // override description
			2400, 4435, null,  // override coordinates
			"Fairy ring BIP",  // override travel tip
			null, null, null, null, null, null  // no other overrides
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Walk there manually", 3000, 3000, "Walk south", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Use fairy ring", current.getDescription());
		assertEquals(2400, current.getWorldX());
		assertEquals(4435, current.getWorldY());
		assertEquals("Fairy ring BIP", current.getTravelTip());
		// Plane should fall through from base step
		assertEquals(0, current.getWorldPlane());
	}

	@Test
	public void testConditionalAlternativeFallbackWhenRequirementsNotMet()
	{
		SourceRequirements reqs = makeQuestRequirement("SONG_OF_THE_ELVES");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(false);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs,
			"Teleport to Prifddinas", 3200, 6100, null,
			"Prifddinas teleport",
			null, null, null, null, null, null
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Walk from Ardougne", 2600, 3300, "Ardougne teleport", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		// Should use base step since requirements not met
		assertEquals("Walk from Ardougne", current.getDescription());
		assertEquals(2600, current.getWorldX());
		assertEquals(3300, current.getWorldY());
		assertEquals("Ardougne teleport", current.getTravelTip());
	}

	@Test
	public void testConditionalAlternativeFirstMatchWins()
	{
		SourceRequirements reqs1 = makeQuestRequirement("FAIRYTALE_II__CURE_A_QUEEN");
		SourceRequirements reqs2 = makeSkillRequirement("AGILITY", 70);
		when(requirementsChecker.meetsRequirements(reqs1)).thenReturn(false);
		when(requirementsChecker.meetsRequirements(reqs2)).thenReturn(true);

		ConditionalAlternative alt1 = new ConditionalAlternative(
			reqs1,
			"Fairy ring route", 2400, 4400, null,
			"Fairy ring CKS",
			null, null, null, null, null, null
		);
		ConditionalAlternative alt2 = new ConditionalAlternative(
			reqs2,
			"Agility shortcut", 2500, 3500, null,
			"Use agility shortcut",
			null, null, null, null, null, null
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Long walk", 3000, 3000, "Walk manually", Arrays.asList(alt1, alt2));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		// First alternative fails, second matches
		assertEquals("Agility shortcut", current.getDescription());
		assertEquals(2500, current.getWorldX());
		assertEquals(3500, current.getWorldY());
		assertEquals("Use agility shortcut", current.getTravelTip());
	}

	@Test
	public void testConditionalAlternativeNullFieldsFallThrough()
	{
		SourceRequirements reqs = makeQuestRequirement("LOST_CITY");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(true);

		// Only override description and travelTip, leave coordinates null
		ConditionalAlternative alt = new ConditionalAlternative(
			reqs,
			"Use fairy ring",  // override description
			null, null, null,  // coordinates fall through
			"Fairy ring BIP",  // override travel tip
			null, null, null, null, null, null
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Walk there", 3000, 3000, "Walk south", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Use fairy ring", current.getDescription());
		// Coordinates should fall through from base step
		assertEquals(3000, current.getWorldX());
		assertEquals(3000, current.getWorldY());
		assertEquals("Fairy ring BIP", current.getTravelTip());
	}

	@Test
	public void testConditionalAlternativeResolutionIsCached()
	{
		SourceRequirements reqs = makeQuestRequirement("LOST_CITY");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs, "Alt route", 2000, 2000, null,
			"Alt tip", null, null, null, null, null, null
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Base route", 3000, 3000, "Base tip", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep first = sequencer.getCurrentStep();
		assertEquals("Alt route", first.getDescription());

		// Now change requirements to not met — but cache should still return alt
		lenient().when(requirementsChecker.meetsRequirements(reqs)).thenReturn(false);
		GuidanceStep second = sequencer.getCurrentStep();
		assertEquals("Alt route", second.getDescription());
	}

	@Test
	public void testConditionalAlternativeWithCompletionConditionOverride()
	{
		SourceRequirements reqs = makeQuestRequirement("LOST_CITY");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs,
			"Talk to fairy", null, null, null,
			null,
			100, "Talk-to",  // override npcId and interactAction
			null,
			CompletionCondition.NPC_TALKED_TO,  // override completion condition
			null,
			100  // override completionNpcId
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Walk to destination", 3000, 3000, "Walk south", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Talk to fairy", current.getDescription());
		assertEquals(100, current.getNpcId());
		assertEquals("Talk-to", current.getInteractAction());
		assertEquals(CompletionCondition.NPC_TALKED_TO, current.getCompletionCondition());
		assertEquals(100, current.getCompletionNpcId());
		// Coordinates fall through
		assertEquals(3000, current.getWorldX());
		assertEquals(3000, current.getWorldY());
	}

	@Test
	public void testStepWithNoAlternativesUnchanged()
	{
		// Step with null conditionalAlternatives should pass through unchanged
		GuidanceStep step = makeManualStep("Simple step");
		assertNull(step.getConditionalAlternatives());

		List<GuidanceStep> steps = Arrays.asList(step);
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Simple step", current.getDescription());
	}

	@Test
	public void testStepWithEmptyAlternativesUnchanged()
	{
		GuidanceStep step = new GuidanceStep(
			"No alternatives",
			3000, 3000, 0,
			0, null, null,
			null, null,
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null,
			null,
			null,
			Collections.emptyList()  // empty list of alternatives
		);

		List<GuidanceStep> steps = Arrays.asList(step);
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("No alternatives", current.getDescription());
	}

	@Test
	public void testConditionalAlternativeWithNullRequirementsSkipped()
	{
		// Alternative with null requirements should not match
		ConditionalAlternative alt = new ConditionalAlternative(
			null,  // null requirements
			"Should not be used", 2000, 2000, null,
			null, null, null, null, null, null, null
		);

		GuidanceStep step = makeStepWithAlternatives(
			"Base step", 3000, 3000, "Base tip", Arrays.asList(alt));

		List<GuidanceStep> steps = Arrays.asList(step, makeManualStep("Done"));
		startSequence(steps);

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Base step", current.getDescription());
	}

	@Test
	public void testResolveAlternativeDirectly()
	{
		// Test GuidanceStep.resolveAlternative() directly
		SourceRequirements reqs = makeQuestRequirement("DRAGON_SLAYER_I");
		RequirementsChecker mockChecker = org.mockito.Mockito.mock(RequirementsChecker.class);
		when(mockChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs, "Dragon route", 2800, 3400, null,
			"Teleport to Crandor", null, null, null, null, null, null
		);

		GuidanceStep base = makeStepWithAlternatives(
			"Walk to Karamja", 2900, 3100, "Charter ship", Arrays.asList(alt));

		GuidanceStep resolved = base.resolveAlternative(mockChecker);
		assertNotSame(base, resolved);
		assertEquals("Dragon route", resolved.getDescription());
		assertEquals(2800, resolved.getWorldX());
		assertEquals(3400, resolved.getWorldY());
		assertEquals("Teleport to Crandor", resolved.getTravelTip());
		// The resolved step should not carry alternatives (already resolved)
		assertNull(resolved.getConditionalAlternatives());
	}

	@Test
	public void testResolveAlternativeReturnsSelfWhenNoMatch()
	{
		SourceRequirements reqs = makeQuestRequirement("SONG_OF_THE_ELVES");
		RequirementsChecker mockChecker = org.mockito.Mockito.mock(RequirementsChecker.class);
		when(mockChecker.meetsRequirements(reqs)).thenReturn(false);

		ConditionalAlternative alt = new ConditionalAlternative(
			reqs, "Elf route", null, null, null,
			null, null, null, null, null, null, null
		);

		GuidanceStep base = makeStepWithAlternatives(
			"Long walk", 3000, 3000, "Walk", Arrays.asList(alt));

		GuidanceStep resolved = base.resolveAlternative(mockChecker);
		assertSame(base, resolved);
	}
}
