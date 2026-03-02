package com.collectionloghelper.data;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class PlayerCollectionState
{
	private static final String CONFIG_GROUP = "collectionloghelper";
	private static final String OBTAINED_KEY = "obtainedItems";

	private final Client client;
	private final ConfigManager configManager;

	// Cached varp values — updated on client thread via refreshVarps(), safe to read from any thread
	private volatile int totalObtained;
	private volatile int totalPossible;
	private volatile int bossesCount, bossesMax;
	private volatile int raidsCount, raidsMax;
	private volatile int cluesCount, cluesMax;
	private volatile int minigamesCount, minigamesMax;
	private volatile int otherCount, otherMax;

	// Per-item obtained tracking — thread-safe set of item IDs, persisted via ConfigManager
	private final Set<Integer> obtainedItemIds = ConcurrentHashMap.newKeySet();

	@Inject
	private PlayerCollectionState(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	/**
	 * Must be called from the client thread. Reads all collection log
	 * varp values and caches them for thread-safe access from any thread.
	 */
	public void refreshVarps()
	{
		totalObtained = client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
		totalPossible = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
		bossesCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_BOSSES);
		bossesMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_BOSSES_MAX);
		raidsCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_RAIDS);
		raidsMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_RAIDS_MAX);
		cluesCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_CLUES);
		cluesMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_CLUES_MAX);
		minigamesCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MINIGAMES);
		minigamesMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MINIGAMES_MAX);
		otherCount = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_OTHER);
		otherMax = client.getVarpValue(VarPlayerID.COLLECTION_COUNT_OTHER_MAX);
	}

	/**
	 * Load the obtained item set from persisted config (per RS profile).
	 */
	public void loadObtainedItems()
	{
		obtainedItemIds.clear();
		String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, OBTAINED_KEY);
		if (saved != null && !saved.isEmpty())
		{
			try
			{
				Arrays.stream(saved.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.mapToInt(Integer::parseInt)
					.forEach(obtainedItemIds::add);
				log.debug("Loaded {} obtained items from config", obtainedItemIds.size());
			}
			catch (NumberFormatException e)
			{
				log.warn("Failed to parse obtained items config", e);
			}
		}
	}

	/**
	 * Save the obtained item set to persisted config (per RS profile).
	 */
	private void saveObtainedItems()
	{
		String csv = obtainedItemIds.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(","));
		configManager.setRSProfileConfiguration(CONFIG_GROUP, OBTAINED_KEY, csv);
	}

	/**
	 * Check if an item is obtained by its item ID (NOT varbit ID).
	 */
	public boolean isItemObtained(int itemId)
	{
		return obtainedItemIds.contains(itemId);
	}

	/**
	 * Mark an item as obtained. Returns true if newly added.
	 */
	public boolean markItemObtained(int itemId)
	{
		if (obtainedItemIds.add(itemId))
		{
			saveObtainedItems();
			return true;
		}
		return false;
	}

	public int getObtainedCount()
	{
		return obtainedItemIds.size();
	}

	public int getTotalObtained()
	{
		return totalObtained;
	}

	public int getTotalPossible()
	{
		return totalPossible;
	}

	public int getCategoryCount(CollectionLogCategory category)
	{
		switch (category)
		{
			case BOSSES:
				return bossesCount;
			case RAIDS:
				return raidsCount;
			case CLUES:
				return cluesCount;
			case MINIGAMES:
				return minigamesCount;
			case OTHER:
				return otherCount;
			default:
				return 0;
		}
	}

	public int getCategoryMax(CollectionLogCategory category)
	{
		switch (category)
		{
			case BOSSES:
				return bossesMax;
			case RAIDS:
				return raidsMax;
			case CLUES:
				return cluesMax;
			case MINIGAMES:
				return minigamesMax;
			case OTHER:
				return otherMax;
			default:
				return 0;
		}
	}

	public double getCompletionPercentage()
	{
		if (totalPossible == 0)
		{
			return 0.0;
		}
		return (totalObtained / (double) totalPossible) * 100.0;
	}

	public void clearState()
	{
		totalObtained = 0;
		totalPossible = 0;
		bossesCount = bossesMax = 0;
		raidsCount = raidsMax = 0;
		cluesCount = cluesMax = 0;
		minigamesCount = minigamesMax = 0;
		otherCount = otherMax = 0;
		obtainedItemIds.clear();
	}
}
