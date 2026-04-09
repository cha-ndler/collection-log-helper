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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerInventoryStateTest
{
	@Mock
	private ItemContainer inventoryContainer;

	@Mock
	private ItemContainer equipmentContainer;

	private PlayerInventoryState state;

	@Before
	public void setUp() throws Exception
	{
		Constructor<PlayerInventoryState> ctor = PlayerInventoryState.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		state = ctor.newInstance();
	}

	// ========================================================================
	// scanInventory
	// ========================================================================

	@Test
	public void initialState_empty()
	{
		assertFalse(state.hasItem(4151));
		assertEquals(0, state.getItemCount(4151));
		assertFalse(state.hasEquippedItem(4151));
	}

	@Test
	public void scanInventory_tracksItems()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{
			new Item(4151, 1), new Item(995, 50000)
		});
		state.scanInventory(inventoryContainer);

		assertTrue(state.hasItem(4151));
		assertTrue(state.hasItem(995));
		assertFalse(state.hasItem(11832));
		assertEquals(1, state.getItemCount(4151));
		assertEquals(50000, state.getItemCount(995));
	}

	@Test
	public void scanInventory_nullContainer()
	{
		state.scanInventory(null);
		assertFalse(state.hasItem(4151));
	}

	@Test
	public void scanInventory_nullItems()
	{
		when(inventoryContainer.getItems()).thenReturn(null);
		state.scanInventory(inventoryContainer);
		assertFalse(state.hasItem(4151));
	}

	@Test
	public void scanInventory_skipsInvalidItems()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{
			new Item(0, 1), new Item(-1, 1), new Item(4151, 0), new Item(11832, 1)
		});
		state.scanInventory(inventoryContainer);

		assertFalse(state.hasItem(0));
		assertFalse(state.hasItem(-1));
		assertFalse(state.hasItem(4151)); // quantity 0
		assertTrue(state.hasItem(11832));
	}

	@Test
	public void scanInventory_overwritesPrevious()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{new Item(4151, 1)});
		state.scanInventory(inventoryContainer);
		assertTrue(state.hasItem(4151));

		when(inventoryContainer.getItems()).thenReturn(new Item[]{new Item(11832, 1)});
		state.scanInventory(inventoryContainer);
		assertFalse(state.hasItem(4151));
		assertTrue(state.hasItem(11832));
	}

	// ========================================================================
	// hasItemCount
	// ========================================================================

	@Test
	public void hasItemCount_sufficientQuantity()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{new Item(1925, 25)});
		state.scanInventory(inventoryContainer);

		assertTrue(state.hasItemCount(1925, 25));
		assertTrue(state.hasItemCount(1925, 1));
		assertFalse(state.hasItemCount(1925, 26));
	}

	@Test
	public void getItemCount_stackedItems()
	{
		// Same item ID in multiple slots should sum quantities
		when(inventoryContainer.getItems()).thenReturn(new Item[]{
			new Item(4151, 3), new Item(4151, 2)
		});
		state.scanInventory(inventoryContainer);

		assertEquals(5, state.getItemCount(4151));
		assertTrue(state.hasItemCount(4151, 5));
	}

	// ========================================================================
	// hasAllItems
	// ========================================================================

	@Test
	public void hasAllItems_allPresent()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{
			new Item(4151, 1), new Item(11832, 1), new Item(995, 100)
		});
		state.scanInventory(inventoryContainer);

		assertTrue(state.hasAllItems(Arrays.asList(4151, 11832)));
		assertTrue(state.hasAllItems(Arrays.asList(4151)));
	}

	@Test
	public void hasAllItems_someMissing()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{new Item(4151, 1)});
		state.scanInventory(inventoryContainer);

		assertFalse(state.hasAllItems(Arrays.asList(4151, 11832)));
	}

	@Test
	public void hasAllItems_emptyList()
	{
		assertTrue(state.hasAllItems(Collections.emptyList()));
		assertTrue(state.hasAllItems(null));
	}

	// ========================================================================
	// scanEquipment
	// ========================================================================

	@Test
	public void scanEquipment_tracksEquipped()
	{
		when(equipmentContainer.getItems()).thenReturn(new Item[]{
			new Item(11832, 1), new Item(11834, 1) // BCP + tassets
		});
		state.scanEquipment(equipmentContainer);

		assertTrue(state.hasEquippedItem(11832));
		assertTrue(state.hasEquippedItem(11834));
		assertFalse(state.hasEquippedItem(4151));
	}

	@Test
	public void scanEquipment_nullContainer()
	{
		state.scanEquipment(null);
		assertFalse(state.hasEquippedItem(11832));
	}

	// ========================================================================
	// reset
	// ========================================================================

	@Test
	public void reset_clearsAll()
	{
		when(inventoryContainer.getItems()).thenReturn(new Item[]{new Item(4151, 1)});
		state.scanInventory(inventoryContainer);
		when(equipmentContainer.getItems()).thenReturn(new Item[]{new Item(11832, 1)});
		state.scanEquipment(equipmentContainer);

		assertTrue(state.hasItem(4151));
		assertTrue(state.hasEquippedItem(11832));

		state.reset();

		assertFalse(state.hasItem(4151));
		assertFalse(state.hasEquippedItem(11832));
		assertEquals(0, state.getItemCount(4151));
	}
}
