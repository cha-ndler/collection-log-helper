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
}
