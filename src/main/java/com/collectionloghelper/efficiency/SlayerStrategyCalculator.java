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

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.SlayerCreatureDatabase;
import com.collectionloghelper.data.SlayerMasterDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Computes optimal Slayer strategy for collection log completion.
 * <p>
 * Recommends which Slayer master to use, which creatures to block,
 * and assesses the current task's collection log value.
 */
@Singleton
public class SlayerStrategyCalculator
{
	/** Maximum number of block-list slots available to players. */
	private static final int MAX_BLOCK_SLOTS = 6;

	private final SlayerMasterDatabase slayerMasterDatabase;
	private final DropRateDatabase dropRateDatabase;
	private final PlayerCollectionState collectionState;

	@Inject
	private SlayerStrategyCalculator(SlayerMasterDatabase slayerMasterDatabase,
		DropRateDatabase dropRateDatabase, PlayerCollectionState collectionState)
	{
		this.slayerMasterDatabase = slayerMasterDatabase;
		this.dropRateDatabase = dropRateDatabase;
		this.collectionState = collectionState;
	}

	/**
	 * Returns the Slayer master that gives the highest probability of
	 * receiving a task with missing collection log items.
	 */
	public String getRecommendedMaster()
	{
		String bestMaster = null;
		double bestProbability = -1;

		for (String masterName : slayerMasterDatabase.getMasterNames())
		{
			double useful = getUsefulTaskProbability(masterName);
			if (useful > bestProbability)
			{
				bestProbability = useful;
				bestMaster = masterName;
			}
		}

		return bestMaster;
	}

	/**
	 * Returns the optimal block list for the given master: up to 6 creatures
	 * that have the highest task weight but NO missing collection log items.
	 * Blocking these maximizes the probability of getting a useful assignment.
	 *
	 * @param masterName the Slayer master to optimize for
	 */
	public List<String> getRecommendedBlockList(String masterName)
	{
		if (masterName == null)
		{
			return Collections.emptyList();
		}

		Map<String, Integer> taskWeights = slayerMasterDatabase.getTaskWeights(masterName);
		if (taskWeights.isEmpty())
		{
			return Collections.emptyList();
		}

		// Find tasks that are "wasteful" (no missing collection log items)
		List<Map.Entry<String, Integer>> wastefulTasks = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : taskWeights.entrySet())
		{
			if (!hasAnyMissingItems(entry.getKey()))
			{
				wastefulTasks.add(entry);
			}
		}

		// Sort by weight descending — blocking high-weight tasks has the most impact
		wastefulTasks.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed());

		List<String> blockList = new ArrayList<>();
		for (int i = 0; i < Math.min(MAX_BLOCK_SLOTS, wastefulTasks.size()); i++)
		{
			blockList.add(wastefulTasks.get(i).getKey());
		}

		return blockList;
	}

	/**
	 * Returns a map of master name to expected tasks until a "useful" assignment
	 * (one that has missing collection log items). Lower is better.
	 */
	public Map<String, Double> getMasterComparison()
	{
		Map<String, Double> comparison = new LinkedHashMap<>();

		for (String masterName : slayerMasterDatabase.getMasterNames())
		{
			double probability = getUsefulTaskProbability(masterName);
			double expectedTasks = probability > 0 ? 1.0 / probability : Double.MAX_VALUE;
			comparison.put(masterName, expectedTasks);
		}

		return comparison;
	}

	/**
	 * Returns the number of missing collection log items obtainable from
	 * sources associated with the given Slayer creature.
	 *
	 * @param creatureName the Slayer task creature name (case-insensitive)
	 */
	public int getMissingItemsForCreature(String creatureName)
	{
		List<String> sourceNames = SlayerCreatureDatabase.getSourcesForCreature(creatureName);
		if (sourceNames.isEmpty())
		{
			return 0;
		}

		int missingCount = 0;
		for (String sourceName : sourceNames)
		{
			CollectionLogSource source = dropRateDatabase.getSourceByName(sourceName);
			if (source == null)
			{
				continue;
			}
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					missingCount++;
				}
			}
		}

		return missingCount;
	}

	/**
	 * Returns the collection log source names associated with the given
	 * Slayer creature that still have missing items.
	 *
	 * @param creatureName the Slayer task creature name (case-insensitive)
	 */
	public List<String> getUsefulSourcesForCreature(String creatureName)
	{
		List<String> sourceNames = SlayerCreatureDatabase.getSourcesForCreature(creatureName);
		List<String> usefulSources = new ArrayList<>();

		for (String sourceName : sourceNames)
		{
			CollectionLogSource source = dropRateDatabase.getSourceByName(sourceName);
			if (source == null)
			{
				continue;
			}
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					usefulSources.add(sourceName);
					break;
				}
			}
		}

		return usefulSources;
	}

	/**
	 * Returns the probability that a random task from the given master
	 * will be "useful" (maps to a source with missing items).
	 */
	private double getUsefulTaskProbability(String masterName)
	{
		Map<String, Integer> taskWeights = slayerMasterDatabase.getTaskWeights(masterName);
		int totalWeight = slayerMasterDatabase.getTotalWeight(masterName);
		if (totalWeight == 0)
		{
			return 0.0;
		}

		int usefulWeight = 0;
		for (Map.Entry<String, Integer> entry : taskWeights.entrySet())
		{
			if (hasAnyMissingItems(entry.getKey()))
			{
				usefulWeight += entry.getValue();
			}
		}

		return (double) usefulWeight / totalWeight;
	}

	/**
	 * Returns true if the given Slayer creature maps to any collection log
	 * source that has at least one missing item.
	 */
	private boolean hasAnyMissingItems(String creatureName)
	{
		List<String> sourceNames = SlayerCreatureDatabase.getSourcesForCreature(creatureName);
		if (sourceNames.isEmpty())
		{
			return false;
		}

		for (String sourceName : sourceNames)
		{
			CollectionLogSource source = dropRateDatabase.getSourceByName(sourceName);
			if (source == null)
			{
				continue;
			}
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					return true;
				}
			}
		}

		return false;
	}
}
