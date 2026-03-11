package com.collectionloghelper.data;

import java.util.HashSet;
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
					}
				}
			}
		}

		itemIds = newIds;
	}

	/**
	 * Returns true if the player's inventory contains the given item ID.
	 */
	public boolean hasItem(int itemId)
	{
		return itemIds.contains(itemId);
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
	}
}
