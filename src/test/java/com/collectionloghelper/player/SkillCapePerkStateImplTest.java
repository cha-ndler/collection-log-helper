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
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SkillCapePerkStateImplTest
{
	@Mock
	private Client client;

	@Mock
	private ItemContainer equipmentContainer;

	private SkillCapePerkStateImpl perkState;

	@Before
	public void setUp() throws Exception
	{
		Constructor<SkillCapePerkStateImpl> ctor =
			SkillCapePerkStateImpl.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		perkState = ctor.newInstance(client);
	}

	// =========================================================================
	// Initial state
	// =========================================================================

	@Test
	public void initialState_allPerksFalse()
	{
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			assertFalse("Expected false before refresh for " + perk,
				perkState.hasPerkAvailable(perk));
		}
	}

	// =========================================================================
	// hasPerkAvailable null guard
	// =========================================================================

	@Test
	public void hasPerkAvailable_nullPerk_returnsFalse()
	{
		assertFalse(perkState.hasPerkAvailable(null));
	}

	// =========================================================================
	// Equipped-cape variant
	// =========================================================================

	@Test
	public void refresh_craftingCapeEquipped_craftingTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_CRAFTING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));
	}

	@Test
	public void refresh_craftingCapeTrimmedEquipped_craftingTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_CRAFTING_TRIMMED);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));
	}

	@Test
	public void refresh_farmingCapeEquipped_farmingTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_FARMING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.FARMING_TELE));
	}

	@Test
	public void refresh_constructionCapeEquipped_constructionTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_CONSTRUCTION);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CONSTRUCTION_TELE));
	}

	@Test
	public void refresh_magicCapeTrimmedEquipped_spellbookSwapTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_MAGIC_TRIMMED);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.MAGIC_SPELLBOOK_SWAP));
	}

	@Test
	public void refresh_fishingCapeEquipped_fishBarrelTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_FISHING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.FISHING_FISH_BARREL));
	}

	@Test
	public void refresh_hunterCapeEquipped_firestrikerTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_HUNTING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.HUNTER_FIRESTRIKER));
	}

	@Test
	public void refresh_agilityCapeEquipped_gracefulPieceTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_AGILITY);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.AGILITY_GRACEFUL_PIECE));
	}

	@Test
	public void refresh_miningCapeEquipped_crystalPickaxeTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_MINING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.MINING_CRYSTAL_PICKAXE));
	}

	@Test
	public void refresh_woodcuttingCapeEquipped_crystalAxeTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_WOODCUTTING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.WOODCUTTING_CRYSTAL_AXE));
	}

	@Test
	public void refresh_runecraftingCapeEquipped_rcRunecraftingTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_RUNECRAFTING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.RC_RUNECRAFTING));
	}

	@Test
	public void refresh_maxCapeEquipped_maxCapeTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_MAX);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.MAX_CAPE_TELES));
	}

	@Test
	public void refresh_maxCapeWornVariantEquipped_maxCapeTeleTrue()
	{
		stubEquipped(ItemID.SKILLCAPE_MAX_WORN);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.MAX_CAPE_TELES));
	}

	// =========================================================================
	// Owned-status fallback (99 level, cape not equipped)
	// =========================================================================

	@Test
	public void refresh_crafting99NoCapeEquipped_craftingTeleTrue()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(99);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));
	}

	@Test
	public void refresh_farming99NoCapeEquipped_farmingTeleTrue()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(99);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.FARMING_TELE));
	}

	@Test
	public void refresh_agility99NoCapeEquipped_gracefulPieceTrue()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(Skill.AGILITY)).thenReturn(99);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.AGILITY_GRACEFUL_PIECE));
	}

	// =========================================================================
	// Neither equipped nor 99
	// =========================================================================

	@Test
	public void refresh_noCapeSkill98_craftingTeleFalse()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(98);
		perkState.refresh();
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));
	}

	@Test
	public void refresh_allSkills98NoCape_allPerksFalse()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(98);
		perkState.refresh();
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			assertFalse("Expected false at skill 98 for " + perk,
				perkState.hasPerkAvailable(perk));
		}
	}

	// =========================================================================
	// MAX_CAPE_TELES owned-status fallback disabled
	// =========================================================================

	@Test
	public void refresh_maxCapeNotEquipped_maxCapeTeleFalse()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(99);
		perkState.refresh();
		assertFalse("MAX_CAPE_TELES must not activate via owned-status fallback",
			perkState.hasPerkAvailable(SkillCapePerk.MAX_CAPE_TELES));
	}

	// =========================================================================
	// Null container
	// =========================================================================

	@Test
	public void refresh_nullContainer_allPerksFalse()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(null);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(98);
		perkState.refresh();
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			assertFalse("Expected false with null container for " + perk,
				perkState.hasPerkAvailable(perk));
		}
	}

	// =========================================================================
	// Client exception handling
	// =========================================================================

	@Test
	public void refresh_containerAndSkillReadThrow_allPerksFalse()
	{
		when(client.getItemContainer(InventoryID.EQUIPMENT))
			.thenThrow(new RuntimeException("client not ready"));
		when(client.getRealSkillLevel(any(Skill.class)))
			.thenThrow(new RuntimeException("client not ready"));
		perkState.refresh();
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			assertFalse("Expected false when client throws for " + perk,
				perkState.hasPerkAvailable(perk));
		}
	}

	// =========================================================================
	// reset
	// =========================================================================

	@Test
	public void reset_afterRefreshWithCape_allFalse()
	{
		stubEquipped(ItemID.SKILLCAPE_CRAFTING);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));

		perkState.reset();

		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			assertFalse("Expected false after reset for " + perk,
				perkState.hasPerkAvailable(perk));
		}
	}

	// =========================================================================
	// Second refresh updates state
	// =========================================================================

	@Test
	public void refresh_secondCallUpdatesState()
	{
		stubNoEquipment();
		when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(98);
		perkState.refresh();
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.FARMING_TELE));

		when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(99);
		perkState.refresh();
		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.FARMING_TELE));
	}

	// =========================================================================
	// Cape equipped does not bleed into unrelated perks
	// =========================================================================

	@Test
	public void refresh_craftingCapeEquipped_otherPerksUnaffected()
	{
		stubEquipped(ItemID.SKILLCAPE_CRAFTING);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(98);
		perkState.refresh();

		assertTrue(perkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE));
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.FARMING_TELE));
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.CONSTRUCTION_TELE));
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.MAGIC_SPELLBOOK_SWAP));
		assertFalse(perkState.hasPerkAvailable(SkillCapePerk.MAX_CAPE_TELES));
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private void stubEquipped(int itemId)
	{
		// Item is a final class — use the concrete constructor, not Mockito mock.
		Item item = new Item(itemId, 1);
		when(equipmentContainer.getItems()).thenReturn(new Item[]{item});
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipmentContainer);
		// Do not stub getRealSkillLevel: when the equipped-item check succeeds, the
		// owned-status fallback is never reached. The Mockito mock returns 0 by default
		// (< 99), so any test that does fall through gets the correct "not 99" result.
	}

	private void stubNoEquipment()
	{
		when(equipmentContainer.getItems()).thenReturn(new Item[0]);
		when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipmentContainer);
	}
}
