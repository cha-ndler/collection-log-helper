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
package com.collectionloghelper;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for B4.4: {@code onSequenceComplete()} must pass the best-item target ID
 * from the top-ranked {@link ScoredItem} to
 * {@link CollectionLogHelperPlugin#activateGuidance(CollectionLogSource, Integer)}
 * when auto-advancing guidance after a sequence completes.
 *
 * <p>This ensures per-item overrides ({@code perItemRequiredItemIds},
 * {@code perItemStepDescription}, {@code perItemNpcId}) activate automatically
 * without the user right-clicking on a specific item.
 */
@RunWith(MockitoJUnitRunner.class)
public class OnSequenceCompletePerItemTest
{
	private static final int BEST_ITEM_ID = 1234;

	@Mock
	private net.runelite.client.callback.ClientThread clientThread;

	@Mock
	private GuidanceOverlayCoordinator guidanceCoordinator;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private EfficiencyCalculator calculator;

	private CollectionLogHelperPlugin plugin;

	@Before
	public void setUp() throws Exception
	{
		plugin = new CollectionLogHelperPlugin();
		injectField("clientThread", clientThread);
		injectField("guidanceCoordinator", guidanceCoordinator);
		injectField("config", config);
		injectField("calculator", calculator);

		// Make clientThread.invokeLater execute the runnable immediately so
		// coordinator.activateGuidance is reachable within the same test call.
		doAnswer(inv ->
		{
			Runnable r = inv.getArgument(0);
			r.run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));

		when(config.autoAdvanceGuidance()).thenReturn(true);
	}

	/**
	 * When the top-ranked {@code ScoredItem} has a non-null {@code bestItem},
	 * {@code onSequenceComplete} must call
	 * {@code coordinator.activateGuidance(source, location, bestItem.getItemId())}.
	 */
	@Test
	public void onSequenceComplete_withBestItem_passesBestItemIdToCoordinator() throws Exception
	{
		CollectionLogSource source = minimalSource("Test Source");
		CollectionLogItem bestItem = new CollectionLogItem(
			BEST_ITEM_ID, "Test Item", 1.0 / 128, false, null, 0, 0, false, false);
		ScoredItem topPick = new ScoredItem(source, 100.0, 1, "Best", false, 100.0, bestItem, 100.0);

		when(calculator.rankByEfficiency()).thenReturn(Collections.singletonList(topPick));

		invokeOnSequenceComplete();

		ArgumentCaptor<Integer> itemIdCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(guidanceCoordinator).activateGuidance(eq(source), any(), itemIdCaptor.capture());
		assertEquals(BEST_ITEM_ID, (int) itemIdCaptor.getValue());
	}

	/**
	 * When the top-ranked {@code ScoredItem} has {@code bestItem == null},
	 * {@code onSequenceComplete} must call
	 * {@code coordinator.activateGuidance(source, location, null)} and must not throw.
	 */
	@Test
	public void onSequenceComplete_withNullBestItem_passesNullTargetIdToCoordinator() throws Exception
	{
		CollectionLogSource source = minimalSource("Null Best Item Source");
		// bestItem is null — pure-guaranteed source or no specific item identified
		ScoredItem topPick = new ScoredItem(source, 50.0, 2, "Some reason", false, 50.0, null, 0.0);

		when(calculator.rankByEfficiency()).thenReturn(Collections.singletonList(topPick));

		invokeOnSequenceComplete();

		verify(guidanceCoordinator).activateGuidance(eq(source), any(), isNull());
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	/**
	 * Invokes the private {@code onSequenceComplete()} method via reflection.
	 */
	private void invokeOnSequenceComplete() throws Exception
	{
		Method m = CollectionLogHelperPlugin.class.getDeclaredMethod("onSequenceComplete");
		m.setAccessible(true);
		m.invoke(plugin);
	}

	private void injectField(String fieldName, Object value) throws Exception
	{
		Field field = CollectionLogHelperPlugin.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private static CollectionLogSource minimalSource(String name)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			0, 0, 0,
			0, 0,
			null, null, null, 0.0,
			null, 0, false, 0,
			null, 0, null, null,
			null,
			null, 0, null, 0,
			Collections.emptyList()
		);
	}
}
