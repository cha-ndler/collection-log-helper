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
import java.util.regex.Pattern;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

/**
 * Regression guard for #737: the step-keyed completion caches
 * ({@code tilePointCache}, {@code chatPatternCache}) must be reset whenever the
 * guidance sequence (re)starts, is reset, is synced, or is stopped. Otherwise an
 * {@code ARRIVE_AT_TILE} step at index N reuses a stale tile cached for index N
 * of a previously-guided source, and the arrival never auto-advances.
 *
 * <p>The caches are populated directly via reflection (rather than driving the
 * full onPlayerMoved event path) so each lifecycle entry point is asserted in
 * isolation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequencerCacheResetTest
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
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class,
			com.collectionloghelper.guidance.bosses.BossGuidanceRegistry.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker, null);
	}

	@Test
	@DisplayName("startSequence clears caches carried over from a prior source (#737)")
	public void startSequenceClearsCaches() throws Exception
	{
		startActiveSequence("Source A");
		polluteCaches();
		// Switching to a new source must not inherit the prior source's tile cache.
		startActiveSequence("Source B");
		assertCachesCleared();
	}

	@Test
	@DisplayName("stopSequence clears the step-keyed caches (#737)")
	public void stopSequenceClearsCaches() throws Exception
	{
		startActiveSequence("Source A");
		polluteCaches();
		sequencer.stopSequence();
		assertCachesCleared();
	}

	@Test
	@DisplayName("restartFromStep0 clears the step-keyed caches (#737)")
	public void restartFromStep0ClearsCaches() throws Exception
	{
		startActiveSequence("Source A");
		polluteCaches();
		sequencer.restartFromStep0();
		assertCachesCleared();
	}

	@Test
	@DisplayName("syncToCurrentState clears the step-keyed caches (#737)")
	public void syncToCurrentStateClearsCaches() throws Exception
	{
		startActiveSequence("Source A");
		polluteCaches();
		sequencer.syncToCurrentState();
		assertCachesCleared();
	}

	// ---- helpers ----

	/** A two-step MANUAL source keeps the sequence active (MANUAL never auto-advances). */
	private void startActiveSequence(String name)
	{
		List<GuidanceStep> steps = Arrays.asList(makeManualStep("Step 1"), makeManualStep("Step 2"));
		CollectionLogSource source = new CollectionLogSource(name, CollectionLogCategory.BOSSES,
			3000, 3000, 0, 60, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			steps, null, null, 0, null, 0, Collections.emptyList(), null, null, null);
		sequencer.startSequence(source, s -> { }, () -> { });
	}

	private void polluteCaches() throws Exception
	{
		setField("tilePointCache",
			new CompletionChecker.TilePointCache(0, new WorldPoint(1, 1, 0)));
		setField("chatPatternCache",
			new CompletionChecker.ChatPatternCache("stale", Pattern.compile("stale")));
	}

	private void assertCachesCleared() throws Exception
	{
		assertNull(getField("tilePointCache"), "tilePointCache must be reset");
		assertNull(getField("chatPatternCache"), "chatPatternCache must be reset");
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

	private GuidanceStep makeManualStep(String description)
	{
		return new GuidanceStep(
			description,
			null,           // perItemStepDescription
			0, 0, 0,        // worldX, worldY, worldPlane
			0, null, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,     // completionItemId, completionItemCount, completionDistance, completionNpcId
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
			null,           // conditionalAlternatives
			null,           // section
			null,           // waypoints
			null,           // dynamicTargetEvaluator
			null,           // conditionTree
			null,           // perItemStepPriority
			null,           // activityObtainableItemIds
			null            // restockIfMissingAllItemIds (#719)
		);
	}
}
