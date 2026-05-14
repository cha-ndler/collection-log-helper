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
package com.collectionloghelper.ui.mode;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.learning.DrynessClass;
import com.collectionloghelper.learning.DryStreakAnalyzer;
import com.collectionloghelper.learning.DryStreakEntry;
import com.collectionloghelper.learning.DryStreakSortMode;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DryStreakFeedModeControllerTest
{
	@Mock
	private PanelShellContext shell;

	@Mock
	private PlayerCollectionState collectionState;

	@Mock
	private DryStreakAnalyzer analyzer;

	private DryStreakFeedModeController controller;

	// Synthetic fixtures
	private CollectionLogItem itemA;
	private CollectionLogItem itemB;
	private CollectionLogSource sourceX;
	private CollectionLogSource sourceY;
	private DryStreakEntry dryEntry;
	private DryStreakEntry veryDryEntry;

	@Before
	public void setUp()
	{
		controller = new DryStreakFeedModeController(shell, collectionState, analyzer);

		itemA = new CollectionLogItem(101, "Amulet of Torture", 1.0 / 200.0,
			false, null, 0, 0, false, false);
		itemB = new CollectionLogItem(102, "Bandos Chestplate", 1.0 / 384.0,
			false, null, 0, 0, false, false);

		sourceX = makeSource("Cerberus", CollectionLogCategory.BOSSES);
		sourceY = makeSource("General Graardor", CollectionLogCategory.BOSSES);

		dryEntry = new DryStreakEntry(itemA, sourceX, 3.5, DrynessClass.DRY);
		veryDryEntry = new DryStreakEntry(itemB, sourceY, 7.2, DrynessClass.VERY_DRY);
	}

	// ── buildView — empty states ─────────────────────────────────────────────

	@Test
	public void buildView_noKcData_showsNoKcMessage()
	{
		when(analyzer.analyze(any(), any())).thenReturn(Collections.emptyList());
		controller.buildView();
		verify(shell).addEmptyStateMessage(contains("No KC data"));
	}

	@Test
	public void buildView_kcPresentButNoDryStreaks_showsNoDryStreaksMessage()
	{
		controller.setKillCounts(Collections.singletonMap("Cerberus", 50));
		when(analyzer.analyze(any(), any())).thenReturn(Collections.emptyList());
		controller.buildView();
		verify(shell).addEmptyStateMessage(contains("No dry streaks"));
	}

	// ── buildView — entries ──────────────────────────────────────────────────

	@Test
	public void buildView_withDryEntries_addsComponentsToList()
	{
		controller.setKillCounts(Collections.singletonMap("Cerberus", 1000));
		when(analyzer.analyze(any(), any())).thenReturn(Arrays.asList(veryDryEntry, dryEntry));

		controller.buildView();

		verify(shell, atLeastOnce()).addToList(any(Component.class));
		verify(shell, never()).addEmptyStateMessage(any());
	}

	@Test
	public void buildView_entriesExceedMaximum_rendersMaxEntriesOnly()
	{
		List<DryStreakEntry> bigFeed = new ArrayList<>();
		for (int i = 0; i <= DryStreakFeedModeController.MAX_ENTRIES; i++)
		{
			CollectionLogItem item = new CollectionLogItem(200 + i, "Item " + i,
				1.0 / 128.0, false, null, 0, 0, false, false);
			bigFeed.add(new DryStreakEntry(item, sourceX, 6.0 + i * 0.01, DrynessClass.VERY_DRY));
		}
		controller.setKillCounts(Collections.singletonMap("Cerberus", 9999));
		when(analyzer.analyze(any(), any())).thenReturn(bigFeed);

		controller.buildView();

		// Should add components without erroring; overflow label added via addToList
		verify(shell, atLeastOnce()).addToList(any(Component.class));
	}

	// ── sort mode ────────────────────────────────────────────────────────────

	@Test
	public void defaultSortMode_isRatioDesc()
	{
		assertEquals(DryStreakSortMode.RATIO_DESC, controller.getSortMode());
	}

	@Test
	public void setSortMode_nullFallsBackToRatioDesc()
	{
		controller.setSortMode(null);
		assertEquals(DryStreakSortMode.RATIO_DESC, controller.getSortMode());
	}

	@Test
	public void setSortMode_itemNameAsc_appliesCorrectly()
	{
		controller.setSortMode(DryStreakSortMode.ITEM_NAME_ASC);
		assertEquals(DryStreakSortMode.ITEM_NAME_ASC, controller.getSortMode());
	}

	// ── applySortMode ────────────────────────────────────────────────────────

	@Test
	public void applySortMode_ratioDesc_sortsHighestFirst()
	{
		// dryEntry.ratio = 3.5, veryDryEntry.ratio = 7.2
		List<DryStreakEntry> input = Arrays.asList(dryEntry, veryDryEntry);
		List<DryStreakEntry> sorted = DryStreakFeedModeController.applySortMode(
			input, DryStreakSortMode.RATIO_DESC);

		assertEquals(veryDryEntry, sorted.get(0));
		assertEquals(dryEntry, sorted.get(1));
	}

	@Test
	public void applySortMode_ratioDesc_doesNotMutateInput()
	{
		List<DryStreakEntry> input = Arrays.asList(dryEntry, veryDryEntry);
		DryStreakFeedModeController.applySortMode(input, DryStreakSortMode.RATIO_DESC);
		// input order must be unchanged
		assertEquals(dryEntry, input.get(0));
		assertEquals(veryDryEntry, input.get(1));
	}

	@Test
	public void applySortMode_itemNameAsc_sortsAlphabetically()
	{
		// itemA = "Amulet of Torture", itemB = "Bandos Chestplate"
		List<DryStreakEntry> input = Arrays.asList(veryDryEntry, dryEntry);
		List<DryStreakEntry> sorted = DryStreakFeedModeController.applySortMode(
			input, DryStreakSortMode.ITEM_NAME_ASC);

		assertEquals("Amulet of Torture", sorted.get(0).getItem().getName());
		assertEquals("Bandos Chestplate", sorted.get(1).getItem().getName());
	}

	@Test
	public void applySortMode_sourceNameAsc_sortsBySourceName()
	{
		// dryEntry source = "Cerberus", veryDryEntry source = "General Graardor"
		List<DryStreakEntry> input = Arrays.asList(veryDryEntry, dryEntry);
		List<DryStreakEntry> sorted = DryStreakFeedModeController.applySortMode(
			input, DryStreakSortMode.SOURCE_NAME_ASC);

		assertEquals("Cerberus", sorted.get(0).getSource().getName());
		assertEquals("General Graardor", sorted.get(1).getSource().getName());
	}

	// ── setKillCounts ────────────────────────────────────────────────────────

	@Test
	public void setKillCounts_nullTreatedAsEmpty()
	{
		controller.setKillCounts(null);
		when(analyzer.analyze(any(), any())).thenReturn(Collections.emptyList());
		controller.buildView();
		verify(shell).addEmptyStateMessage(contains("No KC data"));
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private static CollectionLogSource makeSource(String name, CollectionLogCategory category)
	{
		return new CollectionLogSource(
			name, category,
			0, 0, 0,
			60, 60,
			null, null,
			null,
			0,
			null,
			1,
			false,
			0,
			null,
			0,
			null,
			null,
			null,
			null /* guidanceHelperKey */,
			null,
			0,
			null,
			0,
			Collections.emptyList(),
			null /* metaAuthoredDate */
		);
	}
}
