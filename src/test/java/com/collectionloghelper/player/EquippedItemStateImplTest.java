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

import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EquippedItemStateImplTest
{
	@Mock
	private Client client;

	@Mock
	private ItemContainer container;

	private EquippedItemStateImpl equippedState;

	@Before
	public void setUp() throws Exception
	{
		Constructor<EquippedItemStateImpl> ctor =
			EquippedItemStateImpl.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		equippedState = ctor.newInstance(client);
	}

	// =========================================================================
	// Initial state — snapshot is empty before any refresh
	// =========================================================================

	@Test
	public void initialState_hasEquipped_returnsFalse()
	{
		assertFalse(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
	}

	@Test
	public void initialState_getEquippedItems_returnsEmptySet()
	{
		assertTrue(equippedState.getEquippedItems().isEmpty());
	}

	@Test
	public void initialState_hasTeleportItemEquipped_returnsFalse()
	{
		assertFalse(equippedState.hasTeleportItemEquipped());
	}

	// =========================================================================
	// refresh — container absent (player on login screen)
	// =========================================================================

	@Test
	public void refresh_noContainer_snapshotEmpty()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(null);

		equippedState.refresh();

		assertTrue(equippedState.getEquippedItems().isEmpty());
		assertFalse(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
		assertFalse(equippedState.hasTeleportItemEquipped());
	}

	// =========================================================================
	// refresh — empty container (no items equipped)
	// =========================================================================

	@Test
	public void refresh_emptyContainer_snapshotEmpty()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[0]);

		equippedState.refresh();

		assertTrue(equippedState.getEquippedItems().isEmpty());
		assertFalse(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void refresh_containerWithOnlyEmptySlots_snapshotEmpty()
	{
		// Empty slots have id == -1
		Item emptySlot = new Item(-1, 0);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{emptySlot, emptySlot, emptySlot});

		equippedState.refresh();

		assertTrue(equippedState.getEquippedItems().isEmpty());
	}

	// =========================================================================
	// refresh — single item equipped
	// =========================================================================

	@Test
	public void refresh_singleItem_detectedByHasEquipped()
	{
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4});

		equippedState.refresh();

		assertTrue(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
		assertFalse(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY3));
	}

	@Test
	public void refresh_nonTeleportItem_hasEquippedTrueButTeleportHelperFalse()
	{
		// Dragon scimitar (not a teleport item)
		int dragonScimitar = 4587;
		Item scimitar = new Item(dragonScimitar, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{scimitar});

		equippedState.refresh();

		assertTrue(equippedState.hasEquipped(dragonScimitar));
		assertFalse(equippedState.hasTeleportItemEquipped());
	}

	// =========================================================================
	// refresh — multiple items equipped
	// =========================================================================

	@Test
	public void refresh_multipleItems_allDetected()
	{
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		Item duelingRing = new Item(ItemID.RING_OF_DUELING8, 1);
		Item emptySlot = new Item(-1, 0);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4, emptySlot, duelingRing});

		equippedState.refresh();

		assertTrue(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
		assertTrue(equippedState.hasEquipped(ItemID.RING_OF_DUELING8));
		assertFalse(equippedState.hasEquipped(ItemID.RING_OF_DUELING1));
		assertEquals(2, equippedState.getEquippedItems().size());
	}

	// =========================================================================
	// hasTeleportItemEquipped — shortcut helper
	// =========================================================================

	@Test
	public void hasTeleportItemEquipped_gloryEquipped_returnsTrue()
	{
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void hasTeleportItemEquipped_maxCapeEquipped_returnsTrue()
	{
		Item maxCape = new Item(ItemID.MAX_CAPE, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{maxCape});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void hasTeleportItemEquipped_craftingCapeEquipped_returnsTrue()
	{
		Item craftingCape = new Item(ItemID.CRAFTING_CAPE, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{craftingCape});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void hasTeleportItemEquipped_royalSeedPodEquipped_returnsTrue()
	{
		Item seedPod = new Item(ItemID.ROYAL_SEED_POD, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{seedPod});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void hasTeleportItemEquipped_constructionCapeEquipped_returnsTrue()
	{
		Item constructCape = new Item(ItemID.CONSTRUCT_CAPE, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{constructCape});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	@Test
	public void hasTeleportItemEquipped_eternalGloryEquipped_returnsTrue()
	{
		Item eternal = new Item(ItemID.AMULET_OF_ETERNAL_GLORY, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{eternal});

		equippedState.refresh();

		assertTrue(equippedState.hasTeleportItemEquipped());
	}

	// =========================================================================
	// TELEPORT_ITEMS set — size sanity check
	// =========================================================================

	@Test
	public void teleportItems_setSize_matchesExpectedCount()
	{
		// 7 glory variants (uncharged + 1-6) + 7 trimmed glory variants (uncharged + 1-6)
		// + 1 eternal glory
		// + 8 ring of dueling (1-8)
		// + 7 combat bracelet (uncharged + 1-6)
		// + 7 skills necklace (uncharged + 1-6)
		// + 8 games necklace (1-8)
		// + 1 royal seed pod
		// + 2 crafting cape (untrimmed + trimmed)
		// + 1 max cape
		// + 2 construction cape (untrimmed + trimmed)
		// = 51
		assertEquals(51, EquippedItemStateImpl.TELEPORT_ITEMS.size());
	}

	// =========================================================================
	// refresh — client throws exception
	// =========================================================================

	@Test
	public void refresh_clientThrows_snapshotUnchanged()
	{
		// Pre-seed a snapshot with a glory equipped
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4});
		equippedState.refresh();
		assertTrue(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));

		// Now throw on the next refresh — snapshot should be retained
		when(client.getItemContainer(InventoryID.EQUIPMENT))
			.thenThrow(new RuntimeException("client not ready"));
		equippedState.refresh();

		// Snapshot retained from the previous successful refresh
		assertTrue(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
	}

	// =========================================================================
	// refresh — state updates on second call
	// =========================================================================

	@Test
	public void refresh_secondCall_updatesSnapshot()
	{
		// First refresh: ring of dueling equipped
		Item duelingRing = new Item(ItemID.RING_OF_DUELING8, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{duelingRing});
		equippedState.refresh();
		assertTrue(equippedState.hasEquipped(ItemID.RING_OF_DUELING8));

		// Second refresh: nothing equipped
		when(container.getItems()).thenReturn(new Item[0]);
		equippedState.refresh();
		assertFalse(equippedState.hasEquipped(ItemID.RING_OF_DUELING8));
		assertTrue(equippedState.getEquippedItems().isEmpty());
	}

	// =========================================================================
	// reset — clears snapshot
	// =========================================================================

	@Test
	public void reset_afterRefreshWithData_snapshotCleared()
	{
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4});
		equippedState.refresh();
		assertTrue(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));

		equippedState.reset();

		assertFalse(equippedState.hasEquipped(ItemID.AMULET_OF_GLORY4));
		assertTrue(equippedState.getEquippedItems().isEmpty());
		assertFalse(equippedState.hasTeleportItemEquipped());
	}

	// =========================================================================
	// getEquippedItems — returned set is unmodifiable
	// =========================================================================

	@Test(expected = UnsupportedOperationException.class)
	public void getEquippedItems_returnedSet_isUnmodifiable()
	{
		Item glory4 = new Item(ItemID.AMULET_OF_GLORY4, 1);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{glory4});
		equippedState.refresh();

		// Should throw UnsupportedOperationException
		equippedState.getEquippedItems().add(99999);
	}
}
