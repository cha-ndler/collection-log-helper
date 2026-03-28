/*
 * Copyright (c) 2025, Chandler <ch@ndler.net>
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
package com.collectionloghelper.data;

import com.collectionloghelper.data.ClueItemDatabase.ClueItemType;
import com.collectionloghelper.data.ClueItemDatabase.ClueTier;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.config.ConfigManager;

/**
 * Tracks scanned bank contents relevant to clue efficiency scoring.
 * Populated when the bank is opened and ItemContainerChanged fires.
 */
@Slf4j
@Singleton
public class PlayerBankState
{
	private static final String CONFIG_GROUP = "collectionloghelper";
	private static final String BANK_SCANNED_KEY = "bankScanned";
	private static final String BANK_CASKETS_KEY = "bankCaskets";
	private static final String BANK_SCROLLS_KEY = "bankScrolls";
	private static final String BANK_CONTAINERS_KEY = "bankContainers";
	private static final String BANK_ITEM_IDS_KEY = "bankItemIds";

	private final Client client;
	private final ConfigManager configManager;

	/** Casket counts per tier (unopened reward caskets). */
	private final Map<ClueTier, Integer> casketCounts = new EnumMap<>(ClueTier.class);

	/** Clue scroll counts per tier (unstarted scrolls detected by ID or name). */
	private final Map<ClueTier, Integer> scrollCounts = new EnumMap<>(ClueTier.class);

	/** Clue container counts per tier (bottles + geodes + nests). */
	private final Map<ClueTier, Integer> containerCounts = new EnumMap<>(ClueTier.class);

	/** All item IDs seen in the bank (for generic item lookup). */
	private volatile Set<Integer> allBankItemIds = Collections.emptySet();

	/** Whether the current data was loaded from cache (not a fresh scan). */
	private boolean loadedFromCache;

	@Inject
	private PlayerBankState(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	/**
	 * Scans the bank container for clue-related items and updates internal counts.
	 *
	 * @param bankContainer the bank's ItemContainer from ItemContainerChanged
	 */
	public void scanBank(ItemContainer bankContainer)
	{
		clearCounts();

		if (bankContainer == null)
		{
			return;
		}

		Item[] items = bankContainer.getItems();
		if (items == null)
		{
			return;
		}

		Set<Integer> seenIds = new HashSet<>();
		for (Item item : items)
		{
			int itemId = item.getId();
			int quantity = item.getQuantity();

			if (itemId <= 0 || quantity <= 0)
			{
				continue;
			}

			seenIds.add(itemId);

			// Check the static database first (caskets, bottles, geodes, nests,
			// and beginner/master scrolls)
			if (ClueItemDatabase.isClueItem(itemId))
			{
				ClueTier tier = ClueItemDatabase.getTier(itemId);
				ClueItemType type = ClueItemDatabase.getType(itemId);

				if (tier != null && type != null)
				{
					switch (type)
					{
						case CASKET:
							casketCounts.merge(tier, quantity, Integer::sum);
							break;
						case SCROLL:
							scrollCounts.merge(tier, quantity, Integer::sum);
							break;
						case BOTTLE:
						case GEODE:
						case NEST:
							containerCounts.merge(tier, quantity, Integer::sum);
							break;
					}
				}
				continue;
			}

			// Check if this item was previously discovered as a clue scroll
			// via name matching (avoids redundant getItemDefinition calls)
			if (ClueItemDatabase.isClueRelatedItem(itemId))
			{
				ClueTier cachedTier = getTierByItemName(itemId);
				if (cachedTier != null)
				{
					scrollCounts.merge(cachedTier, quantity, Integer::sum);
				}
				continue;
			}

			// Skip items already confirmed as non-clue via prior name lookup
			if (ClueItemDatabase.isKnownNonClueItem(itemId))
			{
				continue;
			}

			// For truly unknown items, check item name to catch step-specific
			// clue scroll IDs (easy/medium/hard/elite have hundreds of per-step
			// variants). Cache the result to avoid future lookups.
			ClueTier nameTier = getTierByItemName(itemId);
			if (nameTier != null)
			{
				ClueItemDatabase.markAsClueScroll(itemId);
				scrollCounts.merge(nameTier, quantity, Integer::sum);
			}
			else
			{
				ClueItemDatabase.markAsNonClueItem(itemId);
			}
		}

		allBankItemIds = Collections.unmodifiableSet(seenIds);
		loadedFromCache = false;
		logScanResults();
		saveToCache();
	}

	/**
	 * Looks up the item name via ItemComposition and checks if it matches
	 * a clue scroll naming pattern.
	 */
	private ClueTier getTierByItemName(int itemId)
	{
		try
		{
			ItemComposition comp = client.getItemDefinition(itemId);
			if (comp == null)
			{
				return null;
			}
			return ClueItemDatabase.getTierFromName(comp.getName());
		}
		catch (Exception e)
		{
			// Defensive — item composition lookup can fail for invalid IDs
			return null;
		}
	}

	private void logScanResults()
	{
		int totalCaskets = getTotalCaskets();
		int totalScrolls = getTotalScrolls();
		int totalContainers = getTotalContainers();

		if (totalCaskets > 0 || totalScrolls > 0 || totalContainers > 0)
		{
			log.info("Bank scan found: {} caskets, {} clue scrolls, {} clue containers (bottles/geodes/nests)",
				totalCaskets, totalScrolls, totalContainers);

			for (ClueTier tier : ClueTier.values())
			{
				int caskets = getCasketCount(tier);
				int scrolls = getScrollCount(tier);
				int containers = getContainerCount(tier);
				if (caskets > 0 || scrolls > 0 || containers > 0)
				{
					log.info("  {}: {} caskets, {} scrolls, {} containers",
						tier, caskets, scrolls, containers);
				}
			}
		}
		else
		{
			log.info("Bank scan: no clue-related items found");
		}
	}

	/**
	 * Returns count of unopened reward caskets for a given tier.
	 */
	public int getCasketCount(ClueTier tier)
	{
		return casketCounts.getOrDefault(tier, 0);
	}

	/**
	 * Returns count of clue scrolls for a given tier.
	 */
	public int getScrollCount(ClueTier tier)
	{
		return scrollCounts.getOrDefault(tier, 0);
	}

	/**
	 * Returns count of clue containers (bottles, geodes, nests) for a given tier.
	 */
	public int getContainerCount(ClueTier tier)
	{
		return containerCounts.getOrDefault(tier, 0);
	}

	/**
	 * Returns true if the bank contains the given item ID.
	 * Only reliable after a bank scan has been performed this session.
	 */
	public boolean hasItem(int itemId)
	{
		return allBankItemIds.contains(itemId);
	}

	/**
	 * Returns true if any bank data (generic item IDs) is available from a scan.
	 */
	public boolean hasBankItemData()
	{
		return !allBankItemIds.isEmpty();
	}

	/**
	 * Returns total unopened caskets across all tiers.
	 */
	public int getTotalCaskets()
	{
		return casketCounts.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Returns total clue scrolls across all tiers.
	 */
	public int getTotalScrolls()
	{
		return scrollCounts.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Returns total clue containers across all tiers.
	 */
	public int getTotalContainers()
	{
		return containerCounts.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Builds a human-readable summary of unopened caskets for display in the panel.
	 * Returns null if no caskets are found.
	 */
	public String getCasketSummary()
	{
		if (getTotalCaskets() == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (ClueTier tier : ClueTier.values())
		{
			int count = getCasketCount(tier);
			if (count > 0)
			{
				if (sb.length() > 0)
				{
					sb.append(", ");
				}
				sb.append(count).append(" ").append(tier);
			}
		}
		sb.append(getTotalCaskets() == 1 ? " casket" : " caskets");
		sb.append(" ready to open");
		return sb.toString();
	}

	/**
	 * Builds a human-readable summary of clue containers (bottles/geodes/nests).
	 * Returns null if none are found.
	 */
	public String getContainerSummary()
	{
		if (getTotalContainers() == 0)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (ClueTier tier : ClueTier.values())
		{
			int count = getContainerCount(tier);
			if (count > 0)
			{
				if (sb.length() > 0)
				{
					sb.append(", ");
				}
				sb.append(count).append(" ").append(tier);
			}
		}
		sb.append(getTotalContainers() == 1 ? " clue container" : " clue containers");
		return sb.toString();
	}

	/**
	 * Returns true if the current data was loaded from cache rather than a fresh bank scan.
	 */
	public boolean isLoadedFromCache()
	{
		return loadedFromCache;
	}

	/**
	 * Returns true if any cached bank data exists for this RS profile.
	 */
	public boolean hasCachedData()
	{
		return configManager.getRSProfileConfiguration(CONFIG_GROUP, BANK_CASKETS_KEY) != null;
	}

	/**
	 * Load cached bank scan data from config (per RS profile).
	 * Returns true if cache was found and loaded.
	 */
	public boolean loadFromCache()
	{
		clearCounts();

		// Check sentinel flag — indicates bank was previously scanned (even if empty)
		String scanned = configManager.getRSProfileConfiguration(CONFIG_GROUP, BANK_SCANNED_KEY);
		if (!"true".equals(scanned))
		{
			return false;
		}

		loadMapFromConfig(BANK_CASKETS_KEY, casketCounts);
		loadMapFromConfig(BANK_SCROLLS_KEY, scrollCounts);
		loadMapFromConfig(BANK_CONTAINERS_KEY, containerCounts);
		loadItemIdsFromConfig();

		loadedFromCache = true;
		log.info("Loaded cached bank data: {} caskets, {} scrolls, {} containers, {} item IDs",
			getTotalCaskets(), getTotalScrolls(), getTotalContainers(), allBankItemIds.size());
		return true;
	}

	/**
	 * Save current bank scan data to config (per RS profile).
	 */
	private void saveToCache()
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP, BANK_SCANNED_KEY, "true");
		saveMapToConfig(BANK_CASKETS_KEY, casketCounts);
		saveMapToConfig(BANK_SCROLLS_KEY, scrollCounts);
		saveMapToConfig(BANK_CONTAINERS_KEY, containerCounts);
		saveItemIdsToConfig();
	}

	private void saveItemIdsToConfig()
	{
		Set<Integer> ids = allBankItemIds;
		if (ids.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, BANK_ITEM_IDS_KEY);
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (int id : ids)
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(id);
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, BANK_ITEM_IDS_KEY, sb.toString());
	}

	private void saveMapToConfig(String key, Map<ClueTier, Integer> map)
	{
		if (map.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(CONFIG_GROUP, key);
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<ClueTier, Integer> entry : map.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(',');
			}
			sb.append(entry.getKey().name()).append(':').append(entry.getValue());
		}
		configManager.setRSProfileConfiguration(CONFIG_GROUP, key, sb.toString());
	}

	private boolean loadMapFromConfig(String key, Map<ClueTier, Integer> map)
	{
		String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
		if (saved == null || saved.isEmpty())
		{
			return false;
		}
		boolean loaded = false;
		for (String entry : saved.split(","))
		{
			try
			{
				String[] parts = entry.split(":");
				if (parts.length == 2)
				{
					ClueTier tier = ClueTier.valueOf(parts[0].trim());
					int count = Integer.parseInt(parts[1].trim());
					if (count > 0)
					{
						map.put(tier, count);
						loaded = true;
					}
				}
			}
			catch (IllegalArgumentException e)
			{
				log.warn("Skipping invalid cached bank entry '{}' for key {}", entry, key, e);
			}
		}
		return loaded;
	}

	private void loadItemIdsFromConfig()
	{
		String saved = configManager.getRSProfileConfiguration(CONFIG_GROUP, BANK_ITEM_IDS_KEY);
		if (saved == null || saved.isEmpty())
		{
			return;
		}
		Set<Integer> ids = new HashSet<>();
		for (String entry : saved.split(","))
		{
			try
			{
				ids.add(Integer.parseInt(entry.trim()));
			}
			catch (NumberFormatException e)
			{
				log.warn("Skipping invalid cached bank item ID '{}'", entry);
			}
		}
		allBankItemIds = Collections.unmodifiableSet(ids);
	}

	/**
	 * Resets all tracked bank state (in-memory only, does not clear cache).
	 */
	public void reset()
	{
		clearCounts();
		loadedFromCache = false;
	}

	private void clearCounts()
	{
		casketCounts.clear();
		scrollCounts.clear();
		containerCounts.clear();
		allBankItemIds = Collections.emptySet();
	}
}
