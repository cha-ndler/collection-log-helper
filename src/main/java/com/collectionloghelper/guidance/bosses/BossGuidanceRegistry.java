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
package com.collectionloghelper.guidance.bosses;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Registry that maps {@code guidanceHelperKey} strings to their
 * {@link BossGuidance} implementations.
 *
 * <p>The registry is populated at plugin startup. When a
 * {@link com.collectionloghelper.data.CollectionLogSource} carries a non-null
 * {@code guidanceHelperKey}, the
 * {@link com.collectionloghelper.guidance.GuidanceSequencer} calls
 * {@link #get(String)} to retrieve the boss guidance before starting a sequence.
 *
 * <p>Keys are lower-case strings matching the {@code guidanceHelperKey} field
 * value in {@code drop_rates.json} (e.g. {@code "cerberus"}).
 */
@Singleton
public class BossGuidanceRegistry
{
	private final Map<String, BossGuidance> bosses;

	@Inject
	BossGuidanceRegistry(CerberusGuidance cerberusGuidance)
	{
		Map<String, BossGuidance> map = new HashMap<>();
		map.put("cerberus", cerberusGuidance);
		this.bosses = Collections.unmodifiableMap(map);
	}

	/**
	 * Returns the {@link BossGuidance} registered for the given key, or
	 * {@code null} if no boss guidance is registered for that key.
	 *
	 * @param key the {@code guidanceHelperKey} value from the source (may be null)
	 * @return the registered boss guidance, or {@code null}
	 */
	@Nullable
	public BossGuidance get(@Nullable String key)
	{
		if (key == null)
		{
			return null;
		}
		return bosses.get(key);
	}

	/**
	 * Returns an unmodifiable view of the full registry map.
	 * Exposed for testing.
	 */
	public Map<String, BossGuidance> getAllBosses()
	{
		return bosses;
	}
}
