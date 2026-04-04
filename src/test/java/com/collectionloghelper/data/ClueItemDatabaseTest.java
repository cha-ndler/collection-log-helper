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

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClueItemDatabaseTest
{
	// ========================================================================
	// getTier — known casket IDs
	// ========================================================================

	@Test
	public void getTier_beginnerCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.BEGINNER,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_BEGINNER));
	}

	@Test
	public void getTier_easyCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.EASY,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_EASY));
	}

	@Test
	public void getTier_mediumCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.MEDIUM,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_MEDIUM));
	}

	@Test
	public void getTier_hardCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.HARD,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_HARD));
	}

	@Test
	public void getTier_eliteCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.ELITE,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_ELITE));
	}

	@Test
	public void getTier_masterCasket()
	{
		assertEquals(ClueItemDatabase.ClueTier.MASTER,
			ClueItemDatabase.getTier(ItemID.TRAIL_REWARD_CASKET_MASTER));
	}

	@Test
	public void getTier_unknownItem()
	{
		assertNull(ClueItemDatabase.getTier(999999));
	}

	// ========================================================================
	// getType — item type classification
	// ========================================================================

	@Test
	public void getType_casket()
	{
		assertEquals(ClueItemDatabase.ClueItemType.CASKET,
			ClueItemDatabase.getType(ItemID.TRAIL_REWARD_CASKET_HARD));
	}

	@Test
	public void getType_bottle()
	{
		assertEquals(ClueItemDatabase.ClueItemType.BOTTLE,
			ClueItemDatabase.getType(ItemID.FISHING_CLUE_BOTTLE_EASY));
	}

	@Test
	public void getType_geode()
	{
		assertEquals(ClueItemDatabase.ClueItemType.GEODE,
			ClueItemDatabase.getType(ItemID.MINING_CLUE_GEODE_HARD));
	}

	@Test
	public void getType_nest()
	{
		assertEquals(ClueItemDatabase.ClueItemType.NEST,
			ClueItemDatabase.getType(ItemID.WC_CLUE_NEST_MEDIUM));
	}

	@Test
	public void getType_scroll()
	{
		assertEquals(ClueItemDatabase.ClueItemType.SCROLL,
			ClueItemDatabase.getType(ItemID.TRAIL_CLUE_BEGINNER));
	}

	@Test
	public void getType_unknownItem()
	{
		assertNull(ClueItemDatabase.getType(999999));
	}

	// ========================================================================
	// isClueItem / isCasket
	// ========================================================================

	@Test
	public void isClueItem_knownCasket()
	{
		assertTrue(ClueItemDatabase.isClueItem(ItemID.TRAIL_REWARD_CASKET_EASY));
	}

	@Test
	public void isClueItem_knownBottle()
	{
		assertTrue(ClueItemDatabase.isClueItem(ItemID.FISHING_CLUE_BOTTLE_HARD));
	}

	@Test
	public void isClueItem_unknownItem()
	{
		assertFalse(ClueItemDatabase.isClueItem(999999));
	}

	@Test
	public void isCasket_trueForCasket()
	{
		assertTrue(ClueItemDatabase.isCasket(ItemID.TRAIL_REWARD_CASKET_MASTER));
	}

	@Test
	public void isCasket_falseForBottle()
	{
		assertFalse(ClueItemDatabase.isCasket(ItemID.FISHING_CLUE_BOTTLE_EASY));
	}

	@Test
	public void isCasket_falseForUnknown()
	{
		assertFalse(ClueItemDatabase.isCasket(999999));
	}

	// ========================================================================
	// getTierFromName — name-based clue tier detection
	// ========================================================================

	@Test
	public void getTierFromName_null()
	{
		assertNull(ClueItemDatabase.getTierFromName(null));
	}

	@Test
	public void getTierFromName_nonClueItem()
	{
		assertNull(ClueItemDatabase.getTierFromName("Abyssal whip"));
	}

	@Test
	public void getTierFromName_easyClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.EASY,
			ClueItemDatabase.getTierFromName("Clue scroll (easy)"));
	}

	@Test
	public void getTierFromName_mediumClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.MEDIUM,
			ClueItemDatabase.getTierFromName("Clue scroll (medium)"));
	}

	@Test
	public void getTierFromName_hardClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.HARD,
			ClueItemDatabase.getTierFromName("Clue scroll (hard)"));
	}

	@Test
	public void getTierFromName_eliteClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.ELITE,
			ClueItemDatabase.getTierFromName("Clue scroll (elite)"));
	}

	@Test
	public void getTierFromName_masterClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.MASTER,
			ClueItemDatabase.getTierFromName("Clue scroll (master)"));
	}

	@Test
	public void getTierFromName_beginnerClueScroll()
	{
		assertEquals(ClueItemDatabase.ClueTier.BEGINNER,
			ClueItemDatabase.getTierFromName("Clue scroll (beginner)"));
	}

	@Test
	public void getTierFromName_caseInsensitive()
	{
		assertEquals(ClueItemDatabase.ClueTier.HARD,
			ClueItemDatabase.getTierFromName("CLUE SCROLL (HARD)"));
	}

	@Test
	public void getTierFromName_notAClueScrollWithTierKeyword()
	{
		assertNull(ClueItemDatabase.getTierFromName("Easy difficulty task"));
	}

	// ========================================================================
	// Runtime discovery: markAsClueScroll / markAsNonClueItem
	// ========================================================================

	@Test
	public void markAsClueScroll_addsToRelatedItems()
	{
		int testId = 777777;
		ClueItemDatabase.markAsClueScroll(testId);
		assertTrue(ClueItemDatabase.isClueRelatedItem(testId));
	}

	@Test
	public void markAsNonClueItem_addsToKnownNonClue()
	{
		int testId = 888888;
		ClueItemDatabase.markAsNonClueItem(testId);
		assertTrue(ClueItemDatabase.isKnownNonClueItem(testId));
	}

	@Test
	public void isClueRelatedItem_staticRegisteredItem()
	{
		assertTrue(ClueItemDatabase.isClueRelatedItem(ItemID.TRAIL_REWARD_CASKET_HARD));
	}

	// ========================================================================
	// Enum display names
	// ========================================================================

	@Test
	public void clueTier_displayNames()
	{
		assertEquals("Beginner", ClueItemDatabase.ClueTier.BEGINNER.toString());
		assertEquals("Easy", ClueItemDatabase.ClueTier.EASY.toString());
		assertEquals("Medium", ClueItemDatabase.ClueTier.MEDIUM.toString());
		assertEquals("Hard", ClueItemDatabase.ClueTier.HARD.toString());
		assertEquals("Elite", ClueItemDatabase.ClueTier.ELITE.toString());
		assertEquals("Master", ClueItemDatabase.ClueTier.MASTER.toString());
	}

	@Test
	public void clueItemType_displayNames()
	{
		assertEquals("Clue scroll", ClueItemDatabase.ClueItemType.SCROLL.toString());
		assertEquals("Reward casket", ClueItemDatabase.ClueItemType.CASKET.toString());
		assertEquals("Clue bottle", ClueItemDatabase.ClueItemType.BOTTLE.toString());
		assertEquals("Clue geode", ClueItemDatabase.ClueItemType.GEODE.toString());
		assertEquals("Clue nest", ClueItemDatabase.ClueItemType.NEST.toString());
	}
}
