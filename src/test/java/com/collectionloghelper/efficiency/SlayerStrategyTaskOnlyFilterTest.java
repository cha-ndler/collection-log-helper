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
package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SlayerMasterDatabase;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies that strategy advice (block list and best-master recommendation)
 * only counts creatures whose missing items genuinely REQUIRE a Slayer task.
 * Off-task-farmable creatures must not surface as "useful" tasks.
 *
 * <p>"gargoyles" maps to "Gargoyle" (off-task farmable, NOT task-only) plus
 * "Grotesque Guardians" (task-only boss). "hellhounds" maps to "Cerberus",
 * which is task-only.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SlayerStrategyTaskOnlyFilterTest
{
	@Mock
	private SlayerMasterDatabase slayerMasterDatabase;
	@Mock
	private DropRateDatabase dropRateDatabase;
	@Mock
	private PlayerCollectionState collectionState;

	private SlayerStrategyCalculator calculator;

	@BeforeEach
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
		return new CollectionLogSource(name, CollectionLogCategory.SLAYER, 3000, 3000, 0,
			60, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null, null, null, null, 0, null, 0, items, null, null, null);
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

	/**
	 * A creature whose only missing items come from an OFF-task-farmable source
	 * (Gargoyle) must be treated as wasteful and land in the block list, while a
	 * task-only creature with missing items (hellhounds -> Cerberus) must not.
	 */
	@Test
	public void blockList_offTaskFarmableCreature_isBlocked_taskOnlyIsNot()
	{
		Map<String, Integer> tasks = new LinkedHashMap<>();
		tasks.put("gargoyles", 9);
		tasks.put("hellhounds", 7);
		setupMasterWithTasks("Duradel", tasks);

		// Gargoyle: off-task farmable, has a missing item. Must NOT count as useful.
		when(dropRateDatabase.getSourceByName("Gargoyle"))
			.thenReturn(makeSource("Gargoyle", Collections.singletonList(makeItem(1, "Granite maul"))));
		// Grotesque Guardians (task-only boss variant): all items already obtained.
		when(collectionState.isItemObtained(2)).thenReturn(true);
		when(dropRateDatabase.getSourceByName("Grotesque Guardians"))
			.thenReturn(makeSource("Grotesque Guardians", Collections.singletonList(makeItem(2, "Black tourmaline core"))));
		// Cerberus: task-only, has a missing item -> useful.
		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(3, "Primordial crystal"))));

		List<String> blockList = calculator.getRecommendedBlockList("Duradel");

		assertTrue(blockList.contains("gargoyles"),
			"off-task-farmable gargoyles should be wasteful and blocked");
		assertFalse(blockList.contains("hellhounds"),
			"task-only hellhounds (Cerberus) should remain a useful task");
	}

	/**
	 * The best-master recommendation must be driven by task-only useful tasks,
	 * not by off-task-farmable creatures.
	 */
	@Test
	public void recommendedMaster_offTaskFarmableDoesNotShiftRecommendation()
	{
		// MasterA: only an off-task-farmable creature with missing items -> 0% useful.
		Map<String, Integer> tasksA = new LinkedHashMap<>();
		tasksA.put("gargoyles", 10);
		setupMasterWithTasks("MasterA", tasksA);

		// MasterB: a task-only creature with missing items -> 100% useful.
		Map<String, Integer> tasksB = new LinkedHashMap<>();
		tasksB.put("hellhounds", 10);
		setupMasterWithTasks("MasterB", tasksB);

		when(slayerMasterDatabase.getMasterNames())
			.thenReturn(java.util.Arrays.asList("MasterA", "MasterB"));

		when(dropRateDatabase.getSourceByName("Gargoyle"))
			.thenReturn(makeSource("Gargoyle", Collections.singletonList(makeItem(1, "Granite maul"))));
		// Grotesque Guardians fully obtained so gargoyles offers nothing on-task.
		when(collectionState.isItemObtained(2)).thenReturn(true);
		when(dropRateDatabase.getSourceByName("Grotesque Guardians"))
			.thenReturn(makeSource("Grotesque Guardians", Collections.singletonList(makeItem(2, "Black tourmaline core"))));
		when(dropRateDatabase.getSourceByName("Cerberus"))
			.thenReturn(makeSource("Cerberus", Collections.singletonList(makeItem(3, "Primordial crystal"))));

		assertEquals("MasterB", calculator.getRecommendedMaster(),
			"master whose only useful tasks are off-task-farmable should not win");
	}
}
