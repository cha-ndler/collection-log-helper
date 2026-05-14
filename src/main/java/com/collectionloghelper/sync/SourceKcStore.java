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
package com.collectionloghelper.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe in-memory store for per-source kill counts synced from TempleOSRS.
 *
 * <p>Keyed by CLH source name (exact match, not lower-cased).
 * Values are the most recently synced KC for that source.
 *
 * <p>Cleared on plugin shutdown or logout so stale data does not persist
 * across character switches.
 */
@Slf4j
@Singleton
public class SourceKcStore
{
	/** KC map: source name to kill count. */
	private final ConcurrentHashMap<String, Integer> kcMap = new ConcurrentHashMap<>();

	@Inject
	SourceKcStore()
	{
	}

	/**
	 * Bulk-update the store with all KC values from a successful sync.
	 *
	 * @param kcBySource map from CLH source name to KC (as returned by
	 *                   {@link SyncResult#getKcBySource()})
	 */
	public void update(Map<String, Integer> kcBySource)
	{
		kcMap.putAll(kcBySource);
		log.debug("SourceKcStore updated with {} entries", kcBySource.size());
	}

	/**
	 * Returns the synced KC for {@code sourceName}, or {@code 0} if no KC
	 * has been synced for that source.
	 */
	public int getKc(String sourceName)
	{
		return kcMap.getOrDefault(sourceName, 0);
	}

	/**
	 * Returns a read-only snapshot of the entire KC map.
	 */
	public Map<String, Integer> snapshot()
	{
		return Collections.unmodifiableMap(new HashMap<>(kcMap));
	}

	/**
	 * Returns the number of sources with synced KC data.
	 */
	public int size()
	{
		return kcMap.size();
	}

	/**
	 * Clears all synced KC data. Call on logout or plugin shutdown.
	 */
	public void clear()
	{
		kcMap.clear();
		log.debug("SourceKcStore cleared");
	}
}
