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
import net.runelite.api.events.ItemContainerChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that {@link EquippedItemStateImpl} refreshes its snapshot when the
 * worn-items container changes and ignores other container changes (#611).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EquippedItemStateImplSubscribeTest
{
	@Mock
	private Client client;

	@Mock
	private ItemContainer container;

	private EquippedItemStateImpl detector;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<EquippedItemStateImpl> ctor =
			EquippedItemStateImpl.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		detector = ctor.newInstance(client);
	}

	@Test
	public void onItemContainerChanged_wornContainer_refreshesSnapshot()
	{
		// Arrange — a glory4 sitting in the (mocked) WORN container.
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(container);
		when(container.getItems()).thenReturn(new Item[]{new Item(ItemID.AMULET_OF_GLORY4, 1)});

		ItemContainerChanged event =
			new ItemContainerChanged(net.runelite.api.gameval.InventoryID.WORN, container);

		// Act
		detector.onItemContainerChanged(event);

		// Assert — refresh ran and the snapshot now contains the glory.
		verify(client, atLeastOnce()).getItemContainer(InventoryID.EQUIPMENT);
		assertTrue(detector.hasEquipped(ItemID.AMULET_OF_GLORY4));
		assertTrue(detector.hasTeleportItemEquipped());
	}

	@Test
	public void onItemContainerChanged_inventoryContainer_doesNotRefresh()
	{
		ItemContainerChanged event =
			new ItemContainerChanged(net.runelite.api.gameval.InventoryID.INV, container);

		detector.onItemContainerChanged(event);

		// Inventory changes are not the worn container — refresh should be skipped.
		verify(client, never()).getItemContainer(any(InventoryID.class));
		assertTrue(detector.getEquippedItems().isEmpty());
	}
}
