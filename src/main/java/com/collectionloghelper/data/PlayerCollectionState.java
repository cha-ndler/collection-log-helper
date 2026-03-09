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
	private static final String SYNCED_COUNT_KEY = "lastSyncedCount";

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
	 * Must be called from the client thread. Reads the 12 most recently obtained
	 * item IDs from varps and marks them as obtained. These varps are always
	 * available after login without needing the collection log widget open.
	 */
	public boolean captureRecentItems()
	{
		int[] lastItemVarps = {
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM0,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM1,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM2,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM3,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM4,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM5,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM6,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM7,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM8,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM9,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM10,
			VarPlayerID.COLLECTION_OVERVIEW_LAST_ITEM11,
		};

		boolean changed = false;
		for (int varp : lastItemVarps)
		{
			int itemId = client.getVarpValue(varp);
			if (itemId > 0 && obtainedItemIds.add(itemId))
			{
				changed = true;
			}
		}

		if (changed)
		{
			saveObtainedItems();
			log.info("Captured recent items from varps (total tracked: {})", obtainedItemIds.size());
		}
		return changed;
	}

	/**
	 * Check if an item is obtained by its item ID (NOT varbit ID).
	 */
	public boolean isItemObtained(int itemId)
	{
		return obtainedItemIds.contains(itemId);
	}

	/**
	 * Check if a nearby item ID is in the obtained set. Useful for diagnosing
	 * item ID mismatches where the collection log reports a different ID than expected.
	 */
	public void debugItemId(int expectedId, String itemName)
	{
		if (obtainedItemIds.contains(expectedId))
		{
			return; // No mismatch
		}
		// Check nearby IDs (noted variants are typically +1)
		for (int offset = -5; offset <= 5; offset++)
		{
			if (offset != 0 && obtainedItemIds.contains(expectedId + offset))
			{
				log.warn("Item ID mismatch for '{}': expected {} but found {} in obtained set (offset {})",
					itemName, expectedId, expectedId + offset, offset);
				return;
			}
		}
		log.debug("Item '{}' (ID {}) not found in obtained set (set size: {})",
			itemName, expectedId, obtainedItemIds.size());
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

	/**
	 * Save the current totalObtained varp as the last synced count.
	 * Called after a successful collection log sync.
	 */
	public void saveLastSyncedCount()
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP, SYNCED_COUNT_KEY,
			String.valueOf(totalObtained));
		log.debug("Saved last synced count: {}", totalObtained);
	}

	/**
	 * Returns the last synced totalObtained count, or -1 if never synced.
	 */
	public int getLastSyncedCount()
	{
		String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, SYNCED_COUNT_KEY);
		if (saved != null && !saved.isEmpty())
		{
			try
			{
				return Integer.parseInt(saved.trim());
			}
			catch (NumberFormatException e)
			{
				log.warn("Failed to parse lastSyncedCount", e);
			}
		}
		return -1;
	}

	/**
	 * Returns true if the cached obtained items are still fresh — i.e., the
	 * varp totalObtained matches the last synced count, meaning no new items
	 * have been obtained since the last sync.
	 */
	public boolean isCacheFresh()
	{
		int lastSynced = getLastSyncedCount();
		return lastSynced >= 0 && lastSynced == totalObtained && !obtainedItemIds.isEmpty();
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
