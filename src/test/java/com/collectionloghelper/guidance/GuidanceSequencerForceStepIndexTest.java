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

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link GuidanceSequencer#forceStepIndex(int)} — the dev-bridge step-jump
 * seam. Steps are mocked (the real {@code GuidanceStep} constructor has 40+ args
 * and forceStepIndex only needs {@code steps.size()} and a non-null resolved step
 * for the notification path).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequencerForceStepIndexTest
{
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
		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class,
			com.collectionloghelper.guidance.bosses.BossGuidanceRegistry.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker, null);
	}

	private GuidanceStep mockStep(String description)
	{
		GuidanceStep step = mock(GuidanceStep.class);
		// Null conditional-alternatives => resolveStep returns the step as-is.
		lenient().when(step.getConditionalAlternatives()).thenReturn(null);
		lenient().when(step.getDescription()).thenReturn(description);
		return step;
	}

	/**
	 * Starts a sequence directly (bypassing skip-chain heuristics) by injecting the
	 * step list and active flag via reflection, leaving the index at 0.
	 */
	private void startWith(List<GuidanceStep> steps, AtomicReference<GuidanceStep> lastNotified) throws Exception
	{
		setField("steps", steps);
		setField("active", true);
		setField("currentIndex", 0);
		sequencer.setOnStepChanged(lastNotified::set);
	}

	private void setField(String name, Object value) throws Exception
	{
		Field f = GuidanceSequencer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(sequencer, value);
	}

	private Object getField(String name) throws Exception
	{
		Field f = GuidanceSequencer.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(sequencer);
	}

	@Test
	public void forceStepIndexJumpsResetsTransientStateAndNotifies() throws Exception
	{
		GuidanceStep s0 = mockStep("Step 1");
		GuidanceStep s1 = mockStep("Step 2");
		GuidanceStep s2 = mockStep("Step 3");
		AtomicReference<GuidanceStep> notified = new AtomicReference<>();
		startWith(Arrays.asList(s0, s1, s2), notified);

		// Dirty the per-step transient state so we can prove it gets reset.
		setField("loopIterationsCompleted", 3);
		setField("crossedWaypointIndex", 2);
		setField("awaitingRestock", true);
		setField("restockStepIndex", 1);

		sequencer.forceStepIndex(2);

		assertEquals(2, sequencer.getCurrentIndex());
		assertEquals(0, sequencer.getLoopIterationsCompleted());
		assertEquals(0, sequencer.getCrossedWaypointIndex());
		assertEquals(false, getField("awaitingRestock"));
		assertEquals(0, getField("restockStepIndex"));
		// Fires the same step-changed notification the normal advance path fires.
		assertEquals(s2, notified.get());
	}

	@Test
	public void forceStepIndexNoActiveSequenceThrowsIllegalState() throws Exception
	{
		// Fresh sequencer: never started, no steps, not active.
		assertFalse(sequencer.isActive());
		assertThrows(IllegalStateException.class, () -> sequencer.forceStepIndex(0));
	}

	@Test
	public void forceStepIndexOutOfRangeThrowsIndexOutOfBounds() throws Exception
	{
		startWith(Arrays.asList(mockStep("Step 1"), mockStep("Step 2")), new AtomicReference<>());

		assertThrows(IndexOutOfBoundsException.class, () -> sequencer.forceStepIndex(2));
		assertThrows(IndexOutOfBoundsException.class, () -> sequencer.forceStepIndex(-1));
	}
}
