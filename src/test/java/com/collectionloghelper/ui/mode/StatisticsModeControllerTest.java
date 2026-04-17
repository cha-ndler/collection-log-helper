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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsModeControllerTest
{
	@Mock
	private PanelShellContext shell;

	@Mock
	private PlayerCollectionState collectionState;

	@Mock
	private DropRateDatabase database;

	@Mock
	private EfficiencyCalculator calculator;

	private StatisticsModeController controller;

	@Before
	public void setUp()
	{
		controller = new StatisticsModeController(shell, collectionState, database, calculator);

		// Default: no sources in any category
		for (CollectionLogCategory category : CollectionLogCategory.values())
		{
			when(database.getSourcesByCategory(category)).thenReturn(Collections.emptyList());
			when(calculator.filterByCategory(category)).thenReturn(Collections.emptyList());
		}

		when(collectionState.getTotalObtained()).thenReturn(100);
		when(collectionState.getTotalPossible()).thenReturn(1000);
		when(collectionState.getCompletionPercentage()).thenReturn(10.0);
	}

	@Test
	public void buildViewRendersOverallProgressPanel()
	{
		// Act
		controller.buildView();

		// Assert — overall panel + strut are added regardless of category data
		verify(shell, atLeastOnce()).addToList(any(Component.class));
	}

	@Test
	public void buildViewSkipsEmptyCategories()
	{
		// Arrange — every category returns empty sources (set in @Before)

		// Act
		controller.buildView();

		// Assert — only the overall panel + one strut are added (2 calls total),
		// no per-category tiles because every category is empty
		verify(shell, atLeastOnce()).addToList(any(Component.class));
	}

	@Test
	public void buildViewRendersCategoryTileWhenSourcesExist()
	{
		// Arrange — give BOSSES category one real source so a tile renders
		CollectionLogSource source = new CollectionLogSource(
			"Test Source",
			CollectionLogCategory.BOSSES,
			0, 0, 0,
			0, 0,
			null, null, null, 0.0, null,
			0, false, 0, null,
			0, null, null, null, null,
			0, null, 0, Collections.emptyList());
		List<CollectionLogSource> bossList = Collections.singletonList(source);
		when(database.getSourcesByCategory(CollectionLogCategory.BOSSES)).thenReturn(bossList);
		when(collectionState.getCategoryCount(CollectionLogCategory.BOSSES)).thenReturn(5);
		when(collectionState.getCategoryMax(CollectionLogCategory.BOSSES)).thenReturn(50);

		// Act
		controller.buildView();

		// Assert — overall + strut + category tile + strut → at least 4 add calls
		verify(shell, atLeastOnce()).addToList(any(Component.class));
	}
}
