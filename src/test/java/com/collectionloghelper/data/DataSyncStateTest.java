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

import java.lang.reflect.Constructor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataSyncStateTest
{
	private DataSyncState syncState;

	@Before
	public void setUp() throws Exception
	{
		Constructor<DataSyncState> ctor = DataSyncState.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		syncState = ctor.newInstance();
	}

	// ========================================================================
	// Initial state
	// ========================================================================

	@Test
	public void initialState_notSynced()
	{
		assertFalse(syncState.isCollectionLogSynced());
		assertFalse(syncState.isBankScanned());
		assertFalse(syncState.isFullySynced());
	}

	@Test
	public void initialState_loginTimestampZero()
	{
		assertEquals(0, syncState.getLoginTimestamp());
	}

	// ========================================================================
	// isFullySynced
	// ========================================================================

	@Test
	public void isFullySynced_onlyCollectionLog()
	{
		syncState.setCollectionLogSynced(true);
		assertFalse(syncState.isFullySynced());
	}

	@Test
	public void isFullySynced_onlyBank()
	{
		syncState.setBankScanned(true);
		assertFalse(syncState.isFullySynced());
	}

	@Test
	public void isFullySynced_both()
	{
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		assertTrue(syncState.isFullySynced());
	}

	// ========================================================================
	// reset
	// ========================================================================

	@Test
	public void reset_clearsAllState()
	{
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		syncState.setLoginTimestamp(System.currentTimeMillis());

		syncState.reset();

		assertFalse(syncState.isCollectionLogSynced());
		assertFalse(syncState.isBankScanned());
		assertEquals(0, syncState.getLoginTimestamp());
		assertFalse(syncState.isFullySynced());
	}

	// ========================================================================
	// isReminderExpired
	// ========================================================================

	@Test
	public void isReminderExpired_noLoginTimestamp()
	{
		assertFalse(syncState.isReminderExpired());
	}

	@Test
	public void isReminderExpired_recentLogin()
	{
		syncState.setLoginTimestamp(System.currentTimeMillis());
		assertFalse(syncState.isReminderExpired());
	}

	@Test
	public void isReminderExpired_oldLogin()
	{
		// Login was 3 minutes ago — beyond the 2-minute threshold
		syncState.setLoginTimestamp(System.currentTimeMillis() - 180_000);
		assertTrue(syncState.isReminderExpired());
	}

	@Test
	public void isReminderExpired_exactlyAtThreshold()
	{
		// Login was just under 2 minutes ago — should NOT be expired (> not >=)
		syncState.setLoginTimestamp(System.currentTimeMillis() - 119_000);
		assertFalse(syncState.isReminderExpired());
	}

	// ========================================================================
	// Setter/getter round-trip
	// ========================================================================

	@Test
	public void setCollectionLogSynced_roundTrip()
	{
		syncState.setCollectionLogSynced(true);
		assertTrue(syncState.isCollectionLogSynced());
		syncState.setCollectionLogSynced(false);
		assertFalse(syncState.isCollectionLogSynced());
	}

	@Test
	public void setBankScanned_roundTrip()
	{
		syncState.setBankScanned(true);
		assertTrue(syncState.isBankScanned());
		syncState.setBankScanned(false);
		assertFalse(syncState.isBankScanned());
	}

	@Test
	public void setLoginTimestamp_roundTrip()
	{
		long ts = 1234567890L;
		syncState.setLoginTimestamp(ts);
		assertEquals(ts, syncState.getLoginTimestamp());
	}

	// ========================================================================
	// State transitions — SYNCED back to UNSYNCED
	// ========================================================================

	@Test
	public void transitionFromSyncedBackToUnsynced_collectionLog()
	{
		// Arrange — fully synced
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		assertTrue(syncState.isFullySynced());

		// Act — un-sync the collection log
		syncState.setCollectionLogSynced(false);

		// Assert
		assertFalse(syncState.isCollectionLogSynced());
		assertFalse(syncState.isFullySynced());
		assertTrue(syncState.isBankScanned()); // bank state unchanged
	}

	@Test
	public void transitionFromSyncedBackToUnsynced_bank()
	{
		// Arrange — fully synced
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		assertTrue(syncState.isFullySynced());

		// Act — un-sync the bank
		syncState.setBankScanned(false);

		// Assert
		assertFalse(syncState.isBankScanned());
		assertFalse(syncState.isFullySynced());
		assertTrue(syncState.isCollectionLogSynced()); // collection log unchanged
	}

	@Test
	public void transitionFromSyncedToUnsyncedAndBackToSynced()
	{
		// Arrange — start fully synced
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);

		// Act — intermediate unsynced state
		syncState.setCollectionLogSynced(false);
		assertFalse(syncState.isFullySynced());

		// Act — re-sync
		syncState.setCollectionLogSynced(true);

		// Assert
		assertTrue(syncState.isFullySynced());
	}

	// ========================================================================
	// Reset transitions
	// ========================================================================

	@Test
	public void resetAfterFullSync_returnsToInitialState()
	{
		// Arrange
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		syncState.setLoginTimestamp(999_000L);

		// Act
		syncState.reset();

		// Assert — back to clean initial state
		assertFalse(syncState.isCollectionLogSynced());
		assertFalse(syncState.isBankScanned());
		assertFalse(syncState.isFullySynced());
		assertEquals(0, syncState.getLoginTimestamp());
		assertFalse(syncState.isReminderExpired());
	}

	@Test
	public void resetIsIdempotent()
	{
		// Arrange
		syncState.setCollectionLogSynced(true);
		syncState.reset();

		// Act — second reset on already-clean state
		syncState.reset();

		// Assert
		assertFalse(syncState.isCollectionLogSynced());
		assertFalse(syncState.isBankScanned());
		assertEquals(0, syncState.getLoginTimestamp());
	}

	// ========================================================================
	// isReminderExpired boundary values
	// ========================================================================

	@Test
	public void isReminderExpired_negativeTimestamp_returnsFalse()
	{
		// loginTimestamp <= 0 should never expire
		syncState.setLoginTimestamp(-1L);
		assertFalse(syncState.isReminderExpired());
	}

	@Test
	public void isReminderExpired_justOverThreshold_returnsTrue()
	{
		// Login was 2 minutes + 1 ms ago — should be expired
		syncState.setLoginTimestamp(System.currentTimeMillis() - 120_001);
		assertTrue(syncState.isReminderExpired());
	}

	@Test
	public void isReminderExpired_afterReset_returnsFalse()
	{
		// Arrange — set old timestamp, trigger expiry
		syncState.setLoginTimestamp(System.currentTimeMillis() - 180_000);
		assertTrue(syncState.isReminderExpired());

		// Act
		syncState.reset();

		// Assert — reset clears timestamp so reminder no longer expired
		assertFalse(syncState.isReminderExpired());
	}

	// ========================================================================
	// isFullySynced — flag independence
	// ========================================================================

	@Test
	public void isFullySynced_setTrueThenFalseForOneFlag_remainsFalse()
	{
		// Toggle collectionLog true/false while bank stays true
		syncState.setBankScanned(true);
		syncState.setCollectionLogSynced(true);
		assertTrue(syncState.isFullySynced());

		syncState.setCollectionLogSynced(false);
		assertFalse(syncState.isFullySynced());
	}

	@Test
	public void isFullySynced_independentOfLoginTimestamp()
	{
		// loginTimestamp should not affect isFullySynced
		syncState.setCollectionLogSynced(true);
		syncState.setBankScanned(true);
		syncState.setLoginTimestamp(0L);
		assertTrue(syncState.isFullySynced());
	}
}
