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
package com.collectionloghelper.data;

import java.util.Collections;
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
 * Uses a single volatile snapshot to ensure atomic reads across itemIds and itemCounts.
 */
@Slf4j
@Singleton
public class PlayerInventoryState
{
	private static final InventorySnapshot EMPTY = new InventorySnapshot(
		Collections.emptySet(), Collections.emptyMap());

	private volatile InventorySnapshot snapshot = EMPTY;
	private volatile Set<Integer> equippedItemIds = Collections.emptySet();

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

		snapshot = new InventorySnapshot(newIds, newCounts);
	}

	/**
	 * Scans the equipment container and updates tracked equipped item IDs.
	 */
	public void scanEquipment(ItemContainer equipmentContainer)
	{
		Set<Integer> newIds = new HashSet<>();

		if (equipmentContainer != null)
		{
			Item[] items = equipmentContainer.getItems();
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

		equippedItemIds = newIds;
	}

	/**
	 * Returns true if the player's inventory contains the given item ID.
	 */
	public boolean hasItem(int itemId)
	{
		return snapshot.itemIds.contains(itemId);
	}

	/**
	 * Returns true if the player has the given item equipped.
	 */
	public boolean hasEquippedItem(int itemId)
	{
		return equippedItemIds.contains(itemId);
	}

	/**
	 * Returns the count of the given item ID in the player's inventory.
	 */
	public int getItemCount(int itemId)
	{
		return snapshot.itemCounts.getOrDefault(itemId, 0);
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
		return snapshot.itemIds.containsAll(requiredIds);
	}

	/**
	 * Resets tracked inventory state.
	 */
	public void reset()
	{
		snapshot = EMPTY;
		equippedItemIds = Collections.emptySet();
	}

	private static class InventorySnapshot
	{
		final Set<Integer> itemIds;
		final Map<Integer, Integer> itemCounts;

		InventorySnapshot(Set<Integer> itemIds, Map<Integer, Integer> itemCounts)
		{
			this.itemIds = itemIds;
			this.itemCounts = itemCounts;
		}
	}
}
