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

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DataSyncState
{
	private volatile boolean collectionLogSynced;
	private volatile boolean bankScanned;
	private volatile long loginTimestamp;

	@Inject
	private DataSyncState()
	{
	}

	public boolean isCollectionLogSynced()
	{
		return collectionLogSynced;
	}

	public void setCollectionLogSynced(boolean synced)
	{
		this.collectionLogSynced = synced;
	}

	public boolean isBankScanned()
	{
		return bankScanned;
	}

	public void setBankScanned(boolean scanned)
	{
		this.bankScanned = scanned;
	}

	public boolean isFullySynced()
	{
		return collectionLogSynced && bankScanned;
	}

	public long getLoginTimestamp()
	{
		return loginTimestamp;
	}

	public void setLoginTimestamp(long timestamp)
	{
		this.loginTimestamp = timestamp;
	}

	/**
	 * Returns true if reminders should be auto-dismissed (2 minutes after login).
	 */
	public boolean isReminderExpired()
	{
		if (loginTimestamp <= 0)
		{
			return false;
		}
		return System.currentTimeMillis() - loginTimestamp > 120_000;
	}

	public void reset()
	{
		collectionLogSynced = false;
		bankScanned = false;
		loginTimestamp = 0;
	}
}
