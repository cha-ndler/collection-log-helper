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

import com.collectionloghelper.lifecycle.SyncStateCoordinator;
import com.collectionloghelper.sync.CollectionLogNetImporter;
import com.collectionloghelper.sync.SourceKcStore;
import com.collectionloghelper.sync.TempleOsrsKcSyncer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Aggregate holder for the plugin's sync-layer Guice singletons.
 *
 * <p>Groups the four sync/import dependencies that {@code CollectionLogHelperPlugin}
 * previously injected individually, reducing the plugin's {@code @Inject} field count
 * and providing a single seam for sync-layer wiring.
 *
 * <p>Fourth and final sub-module extraction tracked by issue #504.
 */
@Singleton
@Getter
public class SyncModule
{
	private final SyncStateCoordinator syncStateCoordinator;
	private final CollectionLogNetImporter collectionLogNetImporter;
	private final TempleOsrsKcSyncer templeOsrsKcSyncer;
	private final SourceKcStore sourceKcStore;

	@Inject
	public SyncModule(
		SyncStateCoordinator syncStateCoordinator,
		CollectionLogNetImporter collectionLogNetImporter,
		TempleOsrsKcSyncer templeOsrsKcSyncer,
		SourceKcStore sourceKcStore)
	{
		this.syncStateCoordinator = syncStateCoordinator;
		this.collectionLogNetImporter = collectionLogNetImporter;
		this.templeOsrsKcSyncer = templeOsrsKcSyncer;
		this.sourceKcStore = sourceKcStore;
	}
}
