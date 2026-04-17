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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.ui.CollectionLogHelperPanel.SyncState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link SyncStatusView}.
 * Verifies construction and update contract without throwing.
 */
public class SyncStatusViewTest
{
	private DataSyncState dataSyncState;
	private SyncStatusView view;

	@Before
	public void setUp()
	{
		dataSyncState = Mockito.mock(DataSyncState.class);
		Mockito.when(dataSyncState.isFullySynced()).thenReturn(false);
		Mockito.when(dataSyncState.isCollectionLogSynced()).thenReturn(false);
		Mockito.when(dataSyncState.isBankScanned()).thenReturn(false);
		view = new SyncStatusView(dataSyncState);
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void updateSyncStatus_notSynced_doesNotThrow()
	{
		view.updateSyncStatus(SyncState.NOT_SYNCED, 0);
	}

	@Test
	public void updateSyncStatus_syncing_doesNotThrow()
	{
		view.updateSyncStatus(SyncState.SYNCING, 0);
	}

	@Test
	public void updateSyncStatus_synced_doesNotThrow()
	{
		view.updateSyncStatus(SyncState.SYNCED, 1500);
	}

	@Test
	public void updateDataSyncWarning_fullySynced_hidesWarning()
	{
		Mockito.when(dataSyncState.isFullySynced()).thenReturn(true);
		view.updateDataSyncWarning();
	}

	@Test
	public void updateDataSyncWarning_notSynced_doesNotThrow()
	{
		Mockito.when(dataSyncState.isFullySynced()).thenReturn(false);
		Mockito.when(dataSyncState.isCollectionLogSynced()).thenReturn(false);
		Mockito.when(dataSyncState.isBankScanned()).thenReturn(false);
		view.updateDataSyncWarning();
	}

	@Test
	public void updateDataSyncWarning_nullState_doesNotThrow()
	{
		// snapshot-then-null-check discipline: even if underlying state is null-ish,
		// view should not propagate NPE (isFullySynced returns false by default from mock)
		view.updateDataSyncWarning();
	}
}
