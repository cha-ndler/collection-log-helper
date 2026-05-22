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
package com.collectionloghelper.player;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * RuneLite-backed implementation of {@link EquippedItemState}.
 *
 * <p>Reads the equipment container ({@link InventoryID#EQUIPMENT}) via the
 * RuneLite {@link Client} on the client thread and caches a snapshot of item
 * IDs for lock-free reads from any thread. The snapshot is replaced atomically
 * on each {@link #refresh()} call and cleared by {@link #reset()}.
 *
 * <p>Teleport-enabling items tracked by {@link #hasTeleportItemEquipped()}:
 * <ul>
 *   <li>Amulet of glory (uncharged and charges 1-6, trimmed variants, eternal)</li>
 *   <li>Ring of dueling (charges 1-8)</li>
 *   <li>Combat bracelet (charges 1-6)</li>
 *   <li>Skills necklace (uncharged and charges 1-6)</li>
 *   <li>Games necklace (charges 1-8)</li>
 *   <li>Royal seed pod (Glough's seed pod — Gnome Stronghold teleport)</li>
 *   <li>Crafting cape / trimmed (Crafting Guild teleport)</li>
 *   <li>Max cape (includes base and Ardougne variants)</li>
 *   <li>Construction cape / trimmed (POH teleport)</li>
 * </ul>
 *
 * <p>Only charged variants of jewellery are listed where the uncharged base
 * item does not itself offer a teleport (e.g., Ring of dueling(0) cannot
 * teleport). For Amulet of glory the uncharged variant ({@code AMULET_OF_GLORY})
 * is included because it has the same item ID as the worn uncharged glory — the
 * player may still choose to equip it.
 */
@Slf4j
@Singleton
public class EquippedItemStateImpl implements EquippedItemState
{
	/**
	 * All item IDs that provide at least one relevant teleport option when
	 * equipped. Used by {@link #hasTeleportItemEquipped()}.
	 */
	static final Set<Integer> TELEPORT_ITEMS = ImmutableSet.of(
		// Amulet of glory (uncharged worn form + charged variants 1-6)
		ItemID.AMULET_OF_GLORY,
		ItemID.AMULET_OF_GLORY1,
		ItemID.AMULET_OF_GLORY2,
		ItemID.AMULET_OF_GLORY3,
		ItemID.AMULET_OF_GLORY4,
		ItemID.AMULET_OF_GLORY5,
		ItemID.AMULET_OF_GLORY6,
		// Amulet of glory (trimmed variants)
		ItemID.AMULET_OF_GLORY_T,
		ItemID.AMULET_OF_GLORY_T1,
		ItemID.AMULET_OF_GLORY_T2,
		ItemID.AMULET_OF_GLORY_T3,
		ItemID.AMULET_OF_GLORY_T4,
		ItemID.AMULET_OF_GLORY_T5,
		ItemID.AMULET_OF_GLORY_T6,
		// Amulet of eternal glory (infinite charges)
		ItemID.AMULET_OF_ETERNAL_GLORY,
		// Ring of dueling (charges 1-8; uncharged ring has no teleport)
		ItemID.RING_OF_DUELING1,
		ItemID.RING_OF_DUELING2,
		ItemID.RING_OF_DUELING3,
		ItemID.RING_OF_DUELING4,
		ItemID.RING_OF_DUELING5,
		ItemID.RING_OF_DUELING6,
		ItemID.RING_OF_DUELING7,
		ItemID.RING_OF_DUELING8,
		// Combat bracelet (uncharged worn form + charges 1-6)
		ItemID.COMBAT_BRACELET,
		ItemID.COMBAT_BRACELET1,
		ItemID.COMBAT_BRACELET2,
		ItemID.COMBAT_BRACELET3,
		ItemID.COMBAT_BRACELET4,
		ItemID.COMBAT_BRACELET5,
		ItemID.COMBAT_BRACELET6,
		// Skills necklace (uncharged worn form + charges 1-6)
		ItemID.SKILLS_NECKLACE,
		ItemID.SKILLS_NECKLACE1,
		ItemID.SKILLS_NECKLACE2,
		ItemID.SKILLS_NECKLACE3,
		ItemID.SKILLS_NECKLACE4,
		ItemID.SKILLS_NECKLACE5,
		ItemID.SKILLS_NECKLACE6,
		// Games necklace (charges 1-8)
		ItemID.GAMES_NECKLACE1,
		ItemID.GAMES_NECKLACE2,
		ItemID.GAMES_NECKLACE3,
		ItemID.GAMES_NECKLACE4,
		ItemID.GAMES_NECKLACE5,
		ItemID.GAMES_NECKLACE6,
		ItemID.GAMES_NECKLACE7,
		ItemID.GAMES_NECKLACE8,
		// Royal seed pod (Gnome Stronghold teleport — unlimited charges)
		ItemID.ROYAL_SEED_POD,
		// Crafting cape / trimmed (Crafting Guild teleport)
		ItemID.CRAFTING_CAPE,
		ItemID.CRAFTING_CAPET,
		// Max cape (POH + all skill teleports)
		ItemID.MAX_CAPE,
		// Construction cape / trimmed (POH teleport)
		ItemID.CONSTRUCT_CAPE,
		ItemID.CONSTRUCT_CAPET
	);

	// -------------------------------------------------------------------------

	private final Client client;

	/**
	 * Volatile reference so reads from any thread always see the most recent
	 * snapshot written by {@link #refresh()} on the client thread.
	 */
	private volatile Set<Integer> snapshot = Collections.emptySet();

	@Inject
	EquippedItemStateImpl(Client client)
	{
		this.client = client;
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasEquipped(int itemId)
	{
		return snapshot.contains(itemId);
	}

	/** {@inheritDoc} */
	@Override
	public Set<Integer> getEquippedItems()
	{
		return snapshot;
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasTeleportItemEquipped()
	{
		Set<Integer> current = snapshot;
		for (int id : current)
		{
			if (TELEPORT_ITEMS.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Reads {@link InventoryID#EQUIPMENT} from the client. If the container
	 * is absent (e.g., the player is on the login screen) the snapshot is set
	 * to an empty set. Item slots with {@code id == -1} (empty slots) are
	 * skipped.
	 */
	@Override
	public void refresh()
	{
		try
		{
			ItemContainer container = client.getItemContainer(InventoryID.EQUIPMENT);
			if (container == null)
			{
				snapshot = Collections.emptySet();
				log.debug("EquippedItemState refreshed: container absent, snapshot cleared");
				return;
			}

			Item[] items = container.getItems();
			Set<Integer> next = new HashSet<>(items.length * 2);
			for (Item item : items)
			{
				if (item.getId() != -1)
				{
					next.add(item.getId());
				}
			}
			snapshot = Collections.unmodifiableSet(next);
			log.debug("EquippedItemState refreshed: {} item(s) equipped", next.size());
		}
		catch (Exception e)
		{
			log.warn("EquippedItemState refresh failed", e);
			// Retain previous snapshot rather than replacing with potentially
			// partial data.
		}
	}

	/**
	 * Refreshes the equipment snapshot when the worn-items container changes.
	 *
	 * <p>Compares {@link ItemContainerChanged#getContainerId()} against the
	 * {@code WORN} container id (the int-valued gameval constant). Other
	 * containers (inventory, bank, etc.) are ignored so this handler is a
	 * no-op for the vast majority of {@link ItemContainerChanged} events.
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == net.runelite.api.gameval.InventoryID.WORN)
		{
			refresh();
		}
	}

	/** {@inheritDoc} */
	@Override
	public void reset()
	{
		snapshot = Collections.emptySet();
		log.debug("EquippedItemState reset");
	}
}
