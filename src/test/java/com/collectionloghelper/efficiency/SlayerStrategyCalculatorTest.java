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
package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SlayerMasterDatabase;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class SlayerStrategyCalculatorTest
{
	@Mock
	private SlayerMasterDatabase slayerMasterDatabase;
	@Mock
	private DropRateDatabase dropRateDatabase;
	@Mock
	private PlayerCollectionState collectionState;

	private SlayerStrategyCalculator calculator;

	@Before
	public void setUp() throws Exception
	{
		Constructor<SlayerStrategyCalculator> ctor =
			SlayerStrategyCalculator.class.getDeclaredConstructor(
				SlayerMasterDatabase.class, DropRateDatabase.class, PlayerCollectionState.class);
		ctor.setAccessible(true);
		calculator = ctor.newInstance(slayerMasterDatabase, dropRateDatabase, collectionState);

		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);
	}

	private CollectionLogSource makeSource(String name, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			60, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogItem makeItem(int id, String name)
	{
		return new CollectionLogItem(id, name, 0.01, false, null, 0, 0, false, false);
	}

	private void setupMasterWithTasks(String masterName, Map<String, Integer> tasks)
	{
		when(slayerMasterDatabase.getTaskWeights(masterName)).thenReturn(tasks);
		int total = tasks.values().stream().mapToInt(Integer::intValue).sum();
		when(slayerMasterDatabase.getTotalWeight(masterName)).thenReturn(total);
	}

	// ========================================================================
	// getRecommendedMaster
	// ========================================================================

	@Test
	public void getRecommendedMaster_noMasters_returnsNull()
	{
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.emptyList());
		assertNull(calculator.getRecommendedMaster());
	}

	@Test
	public void getRecommendedMaster_singleMasterWithUsefulTasks()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("hellhounds", 10);
		setupMasterWithTasks("Duradel", tasks);
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.singletonList("Duradel"));

		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(1, "Primordial crystal"))));

		assertEquals("Duradel", calculator.getRecommendedMaster());
	}

	@Test
	public void getRecommendedMaster_picksHigherUsefulProbability()
	{
		// Master A: 50% useful tasks
		Map<String, Integer> tasksA = new LinkedHashMap<>();
		tasksA.put("hellhounds", 5);
		tasksA.put("chickens", 5);
		setupMasterWithTasks("MasterA", tasksA);

		// Master B: 100% useful tasks
		Map<String, Integer> tasksB = new LinkedHashMap<>();
		tasksB.put("hellhounds", 10);
		setupMasterWithTasks("MasterB", tasksB);

		when(slayerMasterDatabase.getMasterNames()).thenReturn(Arrays.asList("MasterA", "MasterB"));

		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(1, "Primordial crystal"))));

		assertEquals("MasterB", calculator.getRecommendedMaster());
	}

	// ========================================================================
	// getRecommendedBlockList
	// ========================================================================

	@Test
	public void getRecommendedBlockList_nullMaster_returnsEmpty()
	{
		assertTrue(calculator.getRecommendedBlockList(null).isEmpty());
	}

	@Test
	public void getRecommendedBlockList_noTasks_returnsEmpty()
	{
		when(slayerMasterDatabase.getTaskWeights("Duradel")).thenReturn(Collections.emptyMap());
		assertTrue(calculator.getRecommendedBlockList("Duradel").isEmpty());
	}

	@Test
	public void getRecommendedBlockList_blocksHighWeightWastefulTasks()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("hellhounds", 8);
		tasks.put("chickens", 10);
		tasks.put("rats", 5);
		setupMasterWithTasks("Duradel", tasks);

		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(1, "Primordial crystal"))));

		List<String> blockList = calculator.getRecommendedBlockList("Duradel");

		assertEquals(2, blockList.size());
		assertEquals("chickens", blockList.get(0));
		assertEquals("rats", blockList.get(1));
	}

	@Test
	public void getRecommendedBlockList_maxSixSlots()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		for (int i = 0; i < 8; i++)
		{
			tasks.put("wasteful" + i, 10 - i);
		}
		setupMasterWithTasks("Duradel", tasks);

		List<String> blockList = calculator.getRecommendedBlockList("Duradel");
		assertEquals(6, blockList.size());
	}

	@Test
	public void getRecommendedBlockList_allItemsObtained_blocksEverything()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("hellhounds", 10);
		setupMasterWithTasks("Duradel", tasks);

		when(collectionState.isItemObtained(anyInt())).thenReturn(true);
		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(1, "Primordial crystal"))));

		List<String> blockList = calculator.getRecommendedBlockList("Duradel");
		assertEquals(1, blockList.size());
		assertEquals("hellhounds", blockList.get(0));
	}

	// ========================================================================
	// getMasterComparison
	// ========================================================================

	@Test
	public void getMasterComparison_noMasters_returnsEmpty()
	{
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.emptyList());
		assertTrue(calculator.getMasterComparison().isEmpty());
	}

	@Test
	public void getMasterComparison_returnsExpectedTasksPerMaster()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("hellhounds", 5);
		tasks.put("chickens", 5);
		setupMasterWithTasks("Duradel", tasks);
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.singletonList("Duradel"));

		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(1, "Primordial crystal"))));

		Map<String, Double> comparison = calculator.getMasterComparison();
		assertEquals(2.0, comparison.get("Duradel"), 0.01);
	}

	@Test
	public void getMasterComparison_zeroUsefulProbability_returnsMaxValue()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("chickens", 10);
		setupMasterWithTasks("Duradel", tasks);
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.singletonList("Duradel"));

		Map<String, Double> comparison = calculator.getMasterComparison();
		assertEquals(Double.MAX_VALUE, comparison.get("Duradel"), 0.0);
	}

	// ========================================================================
	// getMissingItemsForCreature
	// ========================================================================

	@Test
	public void getMissingItemsForCreature_unknownCreature_returnsZero()
	{
		assertEquals(0, calculator.getMissingItemsForCreature("chickens"));
	}

	@Test
	public void getMissingItemsForCreature_allMissing()
	{
		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Arrays.asList(
				makeItem(1, "Primordial crystal"),
				makeItem(2, "Pegasian crystal"),
				makeItem(3, "Eternal crystal"))));

		assertEquals(3, calculator.getMissingItemsForCreature("hellhounds"));
	}

	@Test
	public void getMissingItemsForCreature_someObtained()
	{
		when(collectionState.isItemObtained(1)).thenReturn(true);
		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Arrays.asList(
				makeItem(1, "Primordial crystal"),
				makeItem(2, "Pegasian crystal"),
				makeItem(3, "Eternal crystal"))));

		assertEquals(2, calculator.getMissingItemsForCreature("hellhounds"));
	}

	@Test
	public void getMissingItemsForCreature_multipleSourcesAggregated()
	{
		when(dropRateDatabase.getSourceByName("Dagannoth Rex"))
			.thenReturn(makeSource("Dagannoth Rex", Collections.singletonList(makeItem(10, "Berserker ring"))));
		when(dropRateDatabase.getSourceByName("Dagannoth Prime"))
			.thenReturn(makeSource("Dagannoth Prime", Collections.singletonList(makeItem(11, "Seercull"))));
		when(dropRateDatabase.getSourceByName("Dagannoth Supreme"))
			.thenReturn(makeSource("Dagannoth Supreme", Collections.singletonList(makeItem(12, "Dragon axe"))));

		assertEquals(3, calculator.getMissingItemsForCreature("dagannoth"));
	}

	// ========================================================================
	// getUsefulSourcesForCreature
	// ========================================================================

	@Test
	public void getUsefulSourcesForCreature_unknownCreature_returnsEmpty()
	{
		assertTrue(calculator.getUsefulSourcesForCreature("chickens").isEmpty());
	}

	@Test
	public void getUsefulSourcesForCreature_someSourcesComplete()
	{
		when(collectionState.isItemObtained(10)).thenReturn(true);
		when(dropRateDatabase.getSourceByName("Dagannoth Rex"))
			.thenReturn(makeSource("Dagannoth Rex", Collections.singletonList(makeItem(10, "Berserker ring"))));
		when(dropRateDatabase.getSourceByName("Dagannoth Prime"))
			.thenReturn(makeSource("Dagannoth Prime", Collections.singletonList(makeItem(11, "Seercull"))));
		when(dropRateDatabase.getSourceByName("Dagannoth Supreme"))
			.thenReturn(makeSource("Dagannoth Supreme", Collections.singletonList(makeItem(12, "Dragon axe"))));

		List<String> useful = calculator.getUsefulSourcesForCreature("dagannoth");
		assertEquals(2, useful.size());
		assertTrue(useful.contains("Dagannoth Prime"));
		assertTrue(useful.contains("Dagannoth Supreme"));
		assertFalse(useful.contains("Dagannoth Rex"));
	}

	@Test
	public void getUsefulSourcesForCreature_nullSource_skipped()
	{
		when(dropRateDatabase.getSourceByName("Cerberus")).thenReturn(null);
		assertTrue(calculator.getUsefulSourcesForCreature("hellhounds").isEmpty());
	}
}
