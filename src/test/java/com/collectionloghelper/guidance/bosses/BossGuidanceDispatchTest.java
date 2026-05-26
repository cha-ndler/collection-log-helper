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
package com.collectionloghelper.guidance.bosses;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.guidance.GuidanceSequencer;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Verifies that GuidanceSequencer correctly dispatches to a BossGuidance when
 * the source has a non-null guidanceHelperKey, and falls back to JSON steps when
 * it does not.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BossGuidanceDispatchTest
{
	@Mock
	private PlayerInventoryState inventoryState;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;

	private BossGuidanceRegistry registry;
	private GuidanceSequencer sequencer;

	@BeforeEach
	public void setUp() throws Exception
	{
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasAllItems(anyList())).thenReturn(true);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<CerberusGuidance> cerberusCtor = CerberusGuidance.class.getDeclaredConstructor();
		cerberusCtor.setAccessible(true);
		CerberusGuidance cerberusGuidance = cerberusCtor.newInstance();

		Constructor<BossGuidanceRegistry> regCtor =
			BossGuidanceRegistry.class.getDeclaredConstructor(CerberusGuidance.class);
		regCtor.setAccessible(true);
		registry = regCtor.newInstance(cerberusGuidance);

		Constructor<GuidanceSequencer> seqCtor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class,
			BossGuidanceRegistry.class);
		seqCtor.setAccessible(true);
		sequencer = seqCtor.newInstance(inventoryState, collectionState, requirementsChecker, registry);
	}

	private static GuidanceStep makeManualStep(String description)
	{
		return new GuidanceStep(
			description,
			null, 0, 0, 0, 0, null, null, null, null, null, null, null,
			null /* perItemRecommendedItemIds */,
			CompletionCondition.MANUAL,
			0, 0, 0, 0, null, null,
			0, null, null, null, null, null,
			0, 0, false, 0, null, null, 0, 0, null, null, null, null, null,
			null /* waypoints */, null /* dynamicTargetEvaluator */,
			null /* conditionTree */,
						null, /* perItemStepPriority */
						null  // activityObtainableItemIds
		);
	}

	private static CollectionLogSource makeSource(String name, String helperKey, List<GuidanceStep> jsonSteps)
	{
		List<CollectionLogItem> items = Collections.singletonList(
			new CollectionLogItem(1, "Test Item", 0.001, false, null, 0, 0, false, false));
		return new CollectionLogSource(
			name, CollectionLogCategory.BOSSES,
			0, 0, 0, 60, 67, null,
			Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0,
			null, 5862, "Attack", null,
			jsonSteps,
			helperKey,
			null, 0, null, 0, items, null /* metaAuthoredDate */
		, null, null);
	}

	@Test
	public void startSequence_withCerberusHelperKey_usesBossSteps()
	{
		GuidanceStep jsonOnlyStep = makeManualStep("JSON-only step");
		CollectionLogSource source = makeSource("Cerberus", "cerberus",
			Collections.singletonList(jsonOnlyStep));

		AtomicReference<GuidanceStep> firstStep = new AtomicReference<>();
		sequencer.startSequence(source, firstStep::set, () -> {});

		assertTrue( sequencer.isActive(),"Sequencer must be active");
		GuidanceStep current = sequencer.getCurrentStep();
		assertNotNull( current,"Current step must not be null");
		assertEquals(CompletionCondition.ARRIVE_AT_TILE, current.getCompletionCondition());
		assertEquals("Travel", current.getSection());
		assertEquals(
			3, sequencer.getTotalSteps(),"Sequencer must use 3 steps from CerberusGuidance");
		assertFalse(
			"JSON-only step".equals(current.getDescription()),"JSON-only step must not appear when boss guidance is used");
	}

	@Test
	public void startSequence_withNullHelperKey_usesJsonSteps()
	{
		GuidanceStep jsonStep = makeManualStep("My JSON step");
		CollectionLogSource source = makeSource("Test Source", null,
			Collections.singletonList(jsonStep));

		sequencer.startSequence(source, s -> {}, () -> {});

		assertTrue( sequencer.isActive(),"Sequencer must be active");
		GuidanceStep current = sequencer.getCurrentStep();
		assertNotNull(current);
		assertEquals(
			"My JSON step", current.getDescription(),"JSON step description must be used when no helper key");
		assertEquals( 1, sequencer.getTotalSteps(),"Only 1 JSON step");
	}

	@Test
	public void startSequence_withUnknownHelperKey_fallsBackToJsonSteps()
	{
		GuidanceStep jsonStep = makeManualStep("Fallback JSON step");
		CollectionLogSource source = makeSource("Unknown", "no_such_helper",
			Collections.singletonList(jsonStep));

		sequencer.startSequence(source, s -> {}, () -> {});

		assertTrue( sequencer.isActive(),"Sequencer must be active");
		GuidanceStep current = sequencer.getCurrentStep();
		assertNotNull(current);
		assertEquals(
			"Fallback JSON step", current.getDescription(),"Unknown helper key must fall back to JSON step");
	}
}
