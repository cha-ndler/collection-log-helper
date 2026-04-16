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
package com.collectionloghelper.data;

import com.collectionloghelper.data.ClueItemDatabase.ClueTier;
import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerBankStateTest
{
	@Mock
	private Client client;

	@Mock
	private ConfigManager configManager;

	@Mock
	private ItemContainer bankContainer;

	private PlayerBankState bankState;

	@Before
	public void setUp() throws Exception
	{
		Constructor<PlayerBankState> ctor = PlayerBankState.class.getDeclaredConstructor(
			Client.class, ConfigManager.class);
		ctor.setAccessible(true);
		bankState = ctor.newInstance(client, configManager);
	}

	// ========================================================================
	// scanBank — null and empty
	// ========================================================================

	@Test
	public void scanBank_nullContainer_noData()
	{
		bankState.scanBank(null);
		assertEquals(0, bankState.getTotalCaskets());
		assertEquals(0, bankState.getTotalScrolls());
		assertEquals(0, bankState.getTotalContainers());
		assertFalse(bankState.hasBankItemData());
	}

	@Test
	public void scanBank_nullItems_noData()
	{
		when(bankContainer.getItems()).thenReturn(null);
		bankState.scanBank(bankContainer);
		assertEquals(0, bankState.getTotalCaskets());
		assertFalse(bankState.hasBankItemData());
	}

	@Test
	public void scanBank_emptyBank_noData()
	{
		when(bankContainer.getItems()).thenReturn(new Item[0]);
		bankState.scanBank(bankContainer);
		assertEquals(0, bankState.getTotalCaskets());
		assertEquals(0, bankState.getTotalScrolls());
		assertFalse(bankState.hasBankItemData());
	}

	// ========================================================================
	// scanBank — casket detection
	// ========================================================================

	@Test
	public void scanBank_detectsCaskets()
	{
		Item[] items = {makeItem(ItemID.TRAIL_REWARD_CASKET_ELITE, 3)};
		when(bankContainer.getItems()).thenReturn(items);

		bankState.scanBank(bankContainer);
		assertEquals(3, bankState.getCasketCount(ClueTier.ELITE));
		assertEquals(3, bankState.getTotalCaskets());
	}

	@Test
	public void scanBank_multipleTierCaskets()
	{
		Item[] items = {
			makeItem(ItemID.TRAIL_REWARD_CASKET_EASY, 2),
			makeItem(ItemID.TRAIL_REWARD_CASKET_MEDIUM, 1),
			makeItem(ItemID.TRAIL_REWARD_CASKET_HARD, 5)
		};
		when(bankContainer.getItems()).thenReturn(items);

		bankState.scanBank(bankContainer);
		assertEquals(2, bankState.getCasketCount(ClueTier.EASY));
		assertEquals(1, bankState.getCasketCount(ClueTier.MEDIUM));
		assertEquals(5, bankState.getCasketCount(ClueTier.HARD));
		assertEquals(8, bankState.getTotalCaskets());
	}

	// ========================================================================
	// hasItem — generic item tracking
	// ========================================================================

	@Test
	public void scanBank_tracksAllItemIds()
	{
		Item[] items = {
			makeItem(4151, 1),  // Abyssal whip
			makeItem(11832, 1), // Bandos chestplate
			makeItem(995, 50000) // Coins
		};
		when(bankContainer.getItems()).thenReturn(items);

		bankState.scanBank(bankContainer);
		assertTrue(bankState.hasBankItemData());
		assertTrue(bankState.hasItem(4151));
		assertTrue(bankState.hasItem(11832));
		assertTrue(bankState.hasItem(995));
		assertFalse(bankState.hasItem(12345));
	}

	@Test
	public void hasItem_falseBeforeScan()
	{
		assertFalse(bankState.hasItem(4151));
		assertFalse(bankState.hasBankItemData());
	}

	// ========================================================================
	// scanBank — skips invalid items
	// ========================================================================

	@Test
	public void scanBank_skipsInvalidItems()
	{
		Item[] items = {
			makeItem(0, 1),      // ID 0
			makeItem(-1, 1),     // Negative ID
			makeItem(4151, 0),   // Zero quantity
			makeItem(4151, -1),  // Negative quantity
			makeItem(11832, 1)   // Valid item
		};
		when(bankContainer.getItems()).thenReturn(items);

		bankState.scanBank(bankContainer);
		assertTrue(bankState.hasItem(11832));
		assertFalse(bankState.hasItem(0));
		assertFalse(bankState.hasItem(-1));
		// 4151 has 0 and -1 quantity — should not be tracked
		assertFalse(bankState.hasItem(4151));
	}

	// ========================================================================
	// getCasketSummary
	// ========================================================================

	@Test
	public void getCasketSummary_nullWhenEmpty()
	{
		assertNull(bankState.getCasketSummary());
	}

	@Test
	public void getCasketSummary_singleTier()
	{
		Item[] items = {makeItem(ItemID.TRAIL_REWARD_CASKET_EASY, 3)};
		when(bankContainer.getItems()).thenReturn(items);
		bankState.scanBank(bankContainer);

		String summary = bankState.getCasketSummary();
		assertNotNull(summary);
		assertTrue(summary.contains("3"));
		assertTrue(summary.contains("caskets"));
	}

	// ========================================================================
	// reset
	// ========================================================================

	@Test
	public void reset_clearsAllState()
	{
		Item[] items = {makeItem(4151, 1), makeItem(ItemID.TRAIL_REWARD_CASKET_EASY, 2)};
		when(bankContainer.getItems()).thenReturn(items);
		bankState.scanBank(bankContainer);

		assertTrue(bankState.hasBankItemData());
		assertTrue(bankState.hasItem(4151));
		assertEquals(2, bankState.getTotalCaskets());

		bankState.reset();

		assertFalse(bankState.hasBankItemData());
		assertFalse(bankState.hasItem(4151));
		assertEquals(0, bankState.getTotalCaskets());
	}

	@Test
	public void scanBank_overwritesPreviousScan()
	{
		Item[] firstScan = {makeItem(4151, 1)};
		when(bankContainer.getItems()).thenReturn(firstScan);
		bankState.scanBank(bankContainer);
		assertTrue(bankState.hasItem(4151));

		Item[] secondScan = {makeItem(11832, 1)};
		when(bankContainer.getItems()).thenReturn(secondScan);
		bankState.scanBank(bankContainer);
		assertFalse(bankState.hasItem(4151));
		assertTrue(bankState.hasItem(11832));
	}

	// ========================================================================
	// isLoadedFromCache
	// ========================================================================

	@Test
	public void scanBank_notLoadedFromCache()
	{
		Item[] items = {makeItem(4151, 1)};
		when(bankContainer.getItems()).thenReturn(items);
		bankState.scanBank(bankContainer);
		assertFalse(bankState.isLoadedFromCache());
	}

	// ========================================================================
	// Helpers
	// ========================================================================

	private Item makeItem(int id, int quantity)
	{
		return new Item(id, quantity);
	}
}
