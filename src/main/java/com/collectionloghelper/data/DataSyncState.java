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
