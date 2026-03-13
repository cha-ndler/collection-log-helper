package com.collectionloghelper.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

/**
 * Tracks the player's current inventory contents for guidance step checks.
 * Populated each time the inventory changes via ItemContainerChanged.
 */
@Slf4j
@Singleton
public class PlayerInventoryState
{
	private volatile Set<Integer> itemIds = new HashSet<>();
	private volatile Map<Integer, Integer> itemCounts = new HashMap<>();

	@Inject
	private PlayerInventoryState()
	{
	}

	/**
	 * Scans the inventory container and updates tracked item IDs.
	 */
	public void scanInventory(ItemContainer inventoryContainer)
	{
		Set<Integer> newIds = new HashSet<>();
		Map<Integer, Integer> newCounts = new HashMap<>();

		if (inventoryContainer != null)
		{
			Item[] items = inventoryContainer.getItems();
			if (items != null)
			{
				for (Item item : items)
				{
					if (item.getId() > 0 && item.getQuantity() > 0)
					{
						newIds.add(item.getId());
						newCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
					}
				}
			}
		}

		itemIds = newIds;
		itemCounts = newCounts;
	}

	/**
	 * Returns true if the player's inventory contains the given item ID.
	 */
	public boolean hasItem(int itemId)
	{
		return itemIds.contains(itemId);
	}

	/**
	 * Returns the count of the given item ID in the player's inventory.
	 */
	public int getItemCount(int itemId)
	{
		return itemCounts.getOrDefault(itemId, 0);
	}

	/**
	 * Returns true if the player's inventory contains at least the given count of the item.
	 */
	public boolean hasItemCount(int itemId, int count)
	{
		return getItemCount(itemId) >= count;
	}

	/**
	 * Returns true if the player's inventory contains all of the given item IDs.
	 */
	public boolean hasAllItems(java.util.List<Integer> requiredIds)
	{
		if (requiredIds == null || requiredIds.isEmpty())
		{
			return true;
		}
		return itemIds.containsAll(requiredIds);
	}

	/**
	 * Resets tracked inventory state.
	 */
	public void reset()
	{
		itemIds = new HashSet<>();
		itemCounts = new HashMap<>();
	}
}
