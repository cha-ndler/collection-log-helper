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
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.lifecycle.CollectionStateChangeHandler;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for B4.4: {@code handleSequenceComplete()} must pass the best-item
 * target ID from the top-ranked {@link ScoredItem} to the wired
 * {@code activateGuidance} callback (which in production is
 * {@link CollectionLogHelperPlugin#activateGuidance(CollectionLogSource, Integer)})
 * when auto-advancing guidance after a sequence completes.
 *
 * <p>This ensures per-item overrides ({@code perItemRequiredItemIds},
 * {@code perItemStepDescription}, {@code perItemNpcId}) activate automatically
 * without the user right-clicking on a specific item.
 *
 * <p>Wave 13 of #503 moved {@code onSequenceComplete} from
 * {@link CollectionLogHelperPlugin} into
 * {@link CollectionStateChangeHandler}, so these tests now drive the handler
 * directly via its callback wiring.
 */
@RunWith(MockitoJUnitRunner.class)
public class OnSequenceCompletePerItemTest
{
	private static final int BEST_ITEM_ID = 1234;

	@Mock
	private net.runelite.api.Client client;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private DataModule dataModule;

	@Mock
	private EfficiencyModule efficiencyModule;

	@Mock
	private GuidanceModule guidanceModule;

	@Mock
	@SuppressWarnings("unchecked")
	private BiConsumer<CollectionLogSource, Integer> activateGuidanceCallback;

	private CollectionStateChangeHandler handler;
	private List<ScoredItem> rankedSources;

	@Before
	public void setUp()
	{
		handler = new CollectionStateChangeHandler(
			client, config, dataModule, efficiencyModule, guidanceModule);
		rankedSources = Collections.emptyList();
		handler.setCallbacks(
			() -> { /* flag toggle no-op for this test */ },
			() -> rankedSources,
			activateGuidanceCallback);
		when(config.autoAdvanceGuidance()).thenReturn(true);
	}

	/**
	 * When the top-ranked {@code ScoredItem} has a non-null {@code bestItem},
	 * {@code handleSequenceComplete} must call
	 * {@code activateGuidanceCallback.accept(source, bestItem.getItemId())}.
	 */
	@Test
	public void onSequenceComplete_withBestItem_passesBestItemIdToCallback()
	{
		CollectionLogSource source = minimalSource("Test Source");
		CollectionLogItem bestItem = new CollectionLogItem(
			BEST_ITEM_ID, "Test Item", 1.0 / 128, false, null, 0, 0, false, false);
		ScoredItem topPick = new ScoredItem(source, 100.0, 1, "Best", false, 100.0, bestItem, 100.0);
		rankedSources = Collections.singletonList(topPick);

		handler.handleSequenceComplete();

		ArgumentCaptor<Integer> itemIdCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(activateGuidanceCallback).accept(eq(source), itemIdCaptor.capture());
		assertEquals(BEST_ITEM_ID, (int) itemIdCaptor.getValue());
	}

	/**
	 * When the top-ranked {@code ScoredItem} has {@code bestItem == null},
	 * {@code handleSequenceComplete} must call
	 * {@code activateGuidanceCallback.accept(source, null)} and must not throw.
	 */
	@Test
	public void onSequenceComplete_withNullBestItem_passesNullTargetIdToCallback()
	{
		CollectionLogSource source = minimalSource("Null Best Item Source");
		// bestItem is null — pure-guaranteed source or no specific item identified
		ScoredItem topPick = new ScoredItem(source, 50.0, 2, "Some reason", false, 50.0, null, 0.0);
		rankedSources = Collections.singletonList(topPick);

		handler.handleSequenceComplete();

		verify(activateGuidanceCallback).accept(eq(source), isNull());
	}

	// ── helpers ───────────────────────────────────────────────────────────────

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
			null, null, 0, null, 0,
			Collections.emptyList()
		, null);
	}
}
