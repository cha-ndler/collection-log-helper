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
package com.collectionloghelper.di;

import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Aggregate holder for the plugin's data-layer Guice singletons.
 *
 * <p>Groups the eight data-layer dependencies that {@code CollectionLogHelperPlugin}
 * previously injected individually, reducing the plugin's {@code @Inject} field count
 * and providing a single seam for data-layer wiring.
 *
 * <p>First of four planned sub-module extractions tracked by issue #504.
 */
@Singleton
@Getter
public class DataModule
{
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final DataSyncState dataSyncState;
	private final PlayerBankState playerBankState;
	private final PlayerInventoryState playerInventoryState;
	private final SlayerTaskState slayerTaskState;
	private final SlayerMasterDatabase slayerMasterDatabase;
	private final PluginDataManager pluginDataManager;

	@Inject
	public DataModule(
		DropRateDatabase database,
		PlayerCollectionState collectionState,
		DataSyncState dataSyncState,
		PlayerBankState playerBankState,
		PlayerInventoryState playerInventoryState,
		SlayerTaskState slayerTaskState,
		SlayerMasterDatabase slayerMasterDatabase,
		PluginDataManager pluginDataManager)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.dataSyncState = dataSyncState;
		this.playerBankState = playerBankState;
		this.playerInventoryState = playerInventoryState;
		this.slayerTaskState = slayerTaskState;
		this.slayerMasterDatabase = slayerMasterDatabase;
		this.pluginDataManager = pluginDataManager;
	}
}
