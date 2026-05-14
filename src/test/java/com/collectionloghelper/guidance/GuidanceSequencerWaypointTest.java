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
import com.collectionloghelper.data.StepWaypoint;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

/**
 * State-machine tests for the B2 tile-sequence waypoint evaluation in
 * {@link GuidanceSequencer}.
 *
 * <p>Acceptance criteria verified:
 * <ol>
 *   <li>Player crosses waypoints in order → step completes when all are crossed.</li>
 *   <li>Player crosses out of order → waypoint index does not advance.</li>
 *   <li>Null waypoints → legacy ARRIVE_AT_TILE behaviour unchanged.</li>
 *   <li>Empty waypoints list → legacy ARRIVE_AT_TILE behaviour unchanged.</li>
 *   <li>Crossed waypoint index resets when the sequencer advances to the next step.</li>
 *   <li>Partial crossing (not all waypoints done) → step not yet complete.</li>
 * </ol>
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceSequencerWaypointTest
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
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasAllItems(org.mockito.ArgumentMatchers.anyList())).thenReturn(true);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker);
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/**
	 * Creates a minimal ARRIVE_AT_TILE step with an ordered waypoint list.
	 * Final destination tile is (3200, 3400, 0).
	 */
	private GuidanceStep makeWaypointStep(List<StepWaypoint> waypoints)
	{
		return new GuidanceStep(
			"Walk through waypoints",
			null,  // perItemStepDescription
			3200, 3400, 0,  // worldX, worldY, worldPlane (final destination)
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			null,  // recommendedItemIds
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 5, 0,  // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,  // completionNpcIds
			null,  // worldMessage
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,    // objectMaxDistance
			null, // objectFilterTiles
			null, // highlightWidgetIds
			0, 0, // loopBackToStep, loopCount
			null, // skipIfHasAnyItemIds
			null, // dynamicItemObjectTiers
			null, // completionZone
			null, // conditionalAlternatives
			null, // section
			waypoints,
			null  // dynamicTargetEvaluator
		);
	}

	/**
	 * Creates a minimal MANUAL step (used as a trailing step to verify step advance).
	 */
	private GuidanceStep makeManualStep(String description)
	{
		return new GuidanceStep(
			description,
			null,
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,
			null,
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,
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
			null,
			null,
			null, // waypoints
			null  // dynamicTargetEvaluator
		);
	}

	private CollectionLogSource makeSource(GuidanceStep... steps)
	{
		return new CollectionLogSource("Waypoint Test Source", CollectionLogCategory.BOSSES, 3200, 3400, 0,
			60, 0, "Test Source", Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			Arrays.asList(steps), null, 0, null, 0, Collections.emptyList());
	}

	// ── Test cases ────────────────────────────────────────────────────────────

	/**
	 * Crossing all three waypoints in order completes the step.
	 */
	@Test
	public void waypointsInOrder_stepCompletesWhenAllCrossed()
	{
		List<StepWaypoint> waypoints = Arrays.asList(
			new StepWaypoint(3100, 3100, 0, 5),
			new StepWaypoint(3150, 3150, 0, 5),
			new StepWaypoint(3200, 3400, 0, 5)
		);

		AtomicBoolean stepAdvanced = new AtomicBoolean(false);
		GuidanceStep waypointStep = makeWaypointStep(waypoints);
		GuidanceStep finalStep = makeManualStep("Done");
		CollectionLogSource source = makeSource(waypointStep, finalStep);

		sequencer.startSequence(source, step -> stepAdvanced.set(true), () -> {});
		stepAdvanced.set(false); // reset after startSequence notification

		// Cross waypoint 0
		sequencer.onPlayerMoved(new WorldPoint(3100, 3100, 0));
		assertEquals(1, sequencer.getCrossedWaypointIndex());
		assertFalse("Step should not be complete after first waypoint", stepAdvanced.get());

		// Cross waypoint 1
		sequencer.onPlayerMoved(new WorldPoint(3150, 3150, 0));
		assertEquals(2, sequencer.getCrossedWaypointIndex());
		assertFalse("Step should not be complete after second waypoint", stepAdvanced.get());

		// Cross waypoint 2 (final)
		sequencer.onPlayerMoved(new WorldPoint(3200, 3400, 0));
		// After final waypoint, sequencer advances to step 2 (manual step) and fires stepChanged
		assertTrue("Step should be complete after all waypoints crossed", stepAdvanced.get());
		assertEquals(1, sequencer.getCurrentIndex()); // advanced to step index 1
	}

	/**
	 * Crossing waypoint 2 before waypoint 0 does not advance the waypoint index.
	 */
	@Test
	public void waypointsOutOfOrder_indexDoesNotAdvance()
	{
		List<StepWaypoint> waypoints = Arrays.asList(
			new StepWaypoint(3100, 3100, 0, 5),
			new StepWaypoint(3150, 3150, 0, 5),
			new StepWaypoint(3200, 3400, 0, 5)
		);

		GuidanceStep waypointStep = makeWaypointStep(waypoints);
		CollectionLogSource source = makeSource(waypointStep);

		sequencer.startSequence(source, step -> {}, () -> {});

		// Jump directly to waypoint 2's location without crossing waypoint 0
		sequencer.onPlayerMoved(new WorldPoint(3200, 3400, 0));

		// Index should still be 0 — waypoint 0 has not been crossed
		assertEquals("Waypoint index must not advance when out of order", 0, sequencer.getCrossedWaypointIndex());
		assertEquals("Step should not have advanced", 0, sequencer.getCurrentIndex());
	}

	/**
	 * Null waypoints → falls through to legacy single-tile ARRIVE_AT_TILE check.
	 * A step with worldX/worldY/completionDistance set and null waypoints should
	 * complete when the player arrives at the target tile.
	 */
	@Test
	public void nullWaypoints_legacyArrivalBehaviourUnchanged()
	{
		GuidanceStep step = new GuidanceStep(
			"Walk to destination",
			null,
			3200, 3400, 0,  // worldX, worldY, worldPlane
			0, null, null, null,
			null, null,
			null,
			null,
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 5, 0,
			null,
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
			null,
			null,
			null, // waypoints = null
			null  // dynamicTargetEvaluator
		);

		AtomicBoolean stepAdvanced = new AtomicBoolean(false);
		GuidanceStep finalStep = makeManualStep("Done");
		CollectionLogSource source = makeSource(step, finalStep);

		sequencer.startSequence(source, s -> stepAdvanced.set(true), () -> {});
		stepAdvanced.set(false);

		// Arrive at the target tile
		sequencer.onPlayerMoved(new WorldPoint(3200, 3400, 0));

		assertTrue("Legacy ARRIVE_AT_TILE should still complete the step", stepAdvanced.get());
	}

	/**
	 * Empty waypoints list → falls through to legacy single-tile check (same as null).
	 */
	@Test
	public void emptyWaypoints_legacyArrivalBehaviourUnchanged()
	{
		GuidanceStep step = new GuidanceStep(
			"Walk to destination",
			null,
			3200, 3400, 0,
			0, null, null, null,
			null, null,
			null,
			null,
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 5, 0,
			null,
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
			null,
			null,
			Collections.emptyList(), // waypoints = empty list
			null  // dynamicTargetEvaluator
		);

		AtomicBoolean stepAdvanced = new AtomicBoolean(false);
		GuidanceStep finalStep = makeManualStep("Done");
		CollectionLogSource source = makeSource(step, finalStep);

		sequencer.startSequence(source, s -> stepAdvanced.set(true), () -> {});
		stepAdvanced.set(false);

		sequencer.onPlayerMoved(new WorldPoint(3200, 3400, 0));

		assertTrue("Empty waypoints should behave like null (legacy path)", stepAdvanced.get());
	}

	/**
	 * Crossed waypoint index resets to 0 when the sequencer advances to the next step.
	 */
	@Test
	public void crossedWaypointIndex_resetsOnStepAdvance()
	{
		List<StepWaypoint> waypoints = Arrays.asList(
			new StepWaypoint(3100, 3100, 0, 5),
			new StepWaypoint(3200, 3400, 0, 5)
		);

		GuidanceStep waypointStep = makeWaypointStep(waypoints);
		GuidanceStep finalStep = makeManualStep("Done");
		CollectionLogSource source = makeSource(waypointStep, finalStep);

		sequencer.startSequence(source, step -> {}, () -> {});

		// Cross waypoint 0 only — step not yet complete
		sequencer.onPlayerMoved(new WorldPoint(3100, 3100, 0));
		assertEquals(1, sequencer.getCrossedWaypointIndex());

		// Manually skip to step 2
		sequencer.skipStep();

		// Index should be reset after advancing
		assertEquals(0, sequencer.getCrossedWaypointIndex());
		assertEquals(1, sequencer.getCurrentIndex()); // now on step 2 (index 1)
	}

	/**
	 * Partial crossing (only first of three waypoints done) keeps step active.
	 */
	@Test
	public void partialCrossing_stepRemainsActive()
	{
		List<StepWaypoint> waypoints = Arrays.asList(
			new StepWaypoint(3100, 3100, 0, 5),
			new StepWaypoint(3150, 3150, 0, 5),
			new StepWaypoint(3200, 3400, 0, 5)
		);

		GuidanceStep waypointStep = makeWaypointStep(waypoints);
		CollectionLogSource source = makeSource(waypointStep);

		sequencer.startSequence(source, step -> {}, () -> {});

		// Cross only first waypoint
		sequencer.onPlayerMoved(new WorldPoint(3100, 3100, 0));

		assertEquals(0, sequencer.getCurrentIndex()); // still on step 0
		assertTrue("Sequencer should still be active", sequencer.isActive());
		assertEquals(1, sequencer.getCrossedWaypointIndex());
	}
}
