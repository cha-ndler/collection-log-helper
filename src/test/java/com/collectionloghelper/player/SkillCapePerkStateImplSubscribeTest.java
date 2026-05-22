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
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
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
 * Verifies that {@link SkillCapePerkStateImpl} refreshes its snapshot on
 * {@code ItemContainerChanged} (worn + inventory) and {@code StatChanged} (#611).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SkillCapePerkStateImplSubscribeTest
{
	@Mock
	private Client client;

	@Mock
	private ItemContainer container;

	private SkillCapePerkStateImpl detector;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<SkillCapePerkStateImpl> ctor =
			SkillCapePerkStateImpl.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		detector = ctor.newInstance(client);
	}

	@Test
	public void onItemContainerChanged_wornContainer_refreshesSnapshot()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(null);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(0);

		ItemContainerChanged event =
			new ItemContainerChanged(net.runelite.api.gameval.InventoryID.WORN, container);

		detector.onItemContainerChanged(event);

		verify(client, atLeastOnce()).getItemContainer(InventoryID.EQUIPMENT);
	}

	@Test
	public void onItemContainerChanged_inventoryContainer_refreshesSnapshot()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(null);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(0);

		ItemContainerChanged event =
			new ItemContainerChanged(net.runelite.api.gameval.InventoryID.INV, container);

		detector.onItemContainerChanged(event);

		// INV is also relevant for cape ownership-via-swap, so refresh should fire.
		verify(client, atLeastOnce()).getItemContainer(InventoryID.EQUIPMENT);
	}

	@Test
	public void onItemContainerChanged_unrelatedContainer_doesNotRefresh()
	{
		// BANK is neither WORN nor INV — refresh must be skipped.
		ItemContainerChanged event =
			new ItemContainerChanged(net.runelite.api.gameval.InventoryID.BANK, container);

		detector.onItemContainerChanged(event);

		verify(client, never()).getItemContainer(any(InventoryID.class));
	}

	@Test
	public void onStatChanged_refreshesSnapshot()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(null);
		when(client.getRealSkillLevel(Skill.MAGIC)).thenReturn(99);
		when(client.getRealSkillLevel(argThat(s -> s != null && s != Skill.MAGIC))).thenReturn(0);

		StatChanged event = new StatChanged(Skill.MAGIC, 13_034_431, 99, 99);
		detector.onStatChanged(event);

		// Refresh ran and now MAGIC_CAPE perk is available via the 99-level owned-status fallback.
		verify(client, atLeastOnce()).getRealSkillLevel(any(Skill.class));
		assertTrue(detector.hasPerkAvailable(SkillCapePerk.MAGIC_SPELLBOOK_SWAP));
	}

}
