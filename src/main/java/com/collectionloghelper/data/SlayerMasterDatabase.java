package com.collectionloghelper.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads slayer_task_weights.json and provides task weight data per Slayer master.
 * <p>
 * Data sourced from the OSRS Wiki pages for each Slayer master:
 * Duradel, Nieve, Konar, and Turael.
 */
@Slf4j
@Singleton
public class SlayerMasterDatabase
{
	/** Task weights keyed by master name, then creature name. */
	private final Map<String, Map<String, Integer>> masterTaskWeights = new LinkedHashMap<>();

	/** Total weight per master (sum of all task weights). */
	private final Map<String, Integer> masterTotalWeights = new LinkedHashMap<>();

	@Inject
	private Gson gson;

	public void load()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/collectionloghelper/slayer_task_weights.json"))
		{
			if (is == null)
			{
				log.warn("slayer_task_weights.json not found in resources");
				return;
			}

			JsonObject root = gson.fromJson(new InputStreamReader(is), JsonObject.class);
			JsonObject masters = root.getAsJsonObject("masters");
			if (masters == null)
			{
				log.warn("slayer_task_weights.json missing 'masters' key");
				return;
			}

			for (Map.Entry<String, JsonElement> masterEntry : masters.entrySet())
			{
				String masterName = masterEntry.getKey();
				JsonObject masterObj = masterEntry.getValue().getAsJsonObject();
				JsonObject tasks = masterObj.getAsJsonObject("tasks");
				if (tasks == null)
				{
					continue;
				}

				Map<String, Integer> taskWeights = new LinkedHashMap<>();
				int totalWeight = 0;

				for (Map.Entry<String, JsonElement> taskEntry : tasks.entrySet())
				{
					String creatureName = taskEntry.getKey();
					JsonObject taskObj = taskEntry.getValue().getAsJsonObject();
					int weight = taskObj.get("weight").getAsInt();
					taskWeights.put(creatureName, weight);
					totalWeight += weight;
				}

				masterTaskWeights.put(masterName, Collections.unmodifiableMap(taskWeights));
				masterTotalWeights.put(masterName, totalWeight);
			}

			log.debug("Loaded slayer task weights for {} masters", masterTaskWeights.size());
		}
		catch (Exception e)
		{
			log.error("Failed to load slayer task weights", e);
		}
	}

	/**
	 * Returns the names of all loaded Slayer masters.
	 */
	public List<String> getMasterNames()
	{
		return new ArrayList<>(masterTaskWeights.keySet());
	}

	/**
	 * Returns the task-to-weight map for the given master, or an empty map
	 * if the master is not found.
	 */
	public Map<String, Integer> getTaskWeights(String masterName)
	{
		Map<String, Integer> weights = masterTaskWeights.get(masterName);
		return weights != null ? weights : Collections.emptyMap();
	}

	/**
	 * Returns the total weight (sum of all task weights) for the given master.
	 */
	public int getTotalWeight(String masterName)
	{
		Integer total = masterTotalWeights.get(masterName);
		return total != null ? total : 0;
	}

	/**
	 * Returns the probability of being assigned a specific creature task
	 * from the given master: weight / totalWeight.
	 */
	public double getTaskProbability(String masterName, String creatureName)
	{
		Map<String, Integer> weights = masterTaskWeights.get(masterName);
		if (weights == null)
		{
			return 0.0;
		}
		Integer weight = weights.get(creatureName);
		if (weight == null)
		{
			return 0.0;
		}
		int total = getTotalWeight(masterName);
		return total > 0 ? (double) weight / total : 0.0;
	}
}
