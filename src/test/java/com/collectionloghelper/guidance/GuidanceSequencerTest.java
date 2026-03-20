package com.collectionloghelper.guidance;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RewardType;
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
			PlayerInventoryState.class, PlayerCollectionState.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState);
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
			null,           // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			0, 0            // loopBackToStep, loopCount
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			pattern,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			loopBackToStep, loopCount
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			null,
			0, null, null,
			null, null,
			null,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			0, 0
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
			RewardType.DROP, 0, false, null, 1, false, 0, null, 0, null, null,
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
}
