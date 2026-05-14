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
import java.util.Map;

/**
 * Result of a TempleOSRS KC sync operation.
 *
 * <p>Follows the fail-soft pattern: a failed fetch yields
 * {@link #failure(String)} so callers need not handle exceptions.
 */
public final class SyncResult
{
	private final boolean success;
	private final String errorMessage;
	/** Map from CLH source name to KC, populated only on success. */
	private final Map<String, Integer> kcBySource;
	private final int skippedCount;

	private SyncResult(boolean success, String errorMessage,
		Map<String, Integer> kcBySource, int skippedCount)
	{
		this.success = success;
		this.errorMessage = errorMessage;
		this.kcBySource = kcBySource;
		this.skippedCount = skippedCount;
	}

	public static SyncResult success(Map<String, Integer> kcBySource, int skippedCount)
	{
		return new SyncResult(true, null,
			Collections.unmodifiableMap(kcBySource), skippedCount);
	}

	public static SyncResult failure(String errorMessage)
	{
		return new SyncResult(false, errorMessage, Collections.emptyMap(), 0);
	}

	public boolean isSuccess()
	{
		return success;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	/** KC map keyed by CLH source name. Empty on failure. */
	public Map<String, Integer> getKcBySource()
	{
		return kcBySource;
	}

	/** Number of TempleOSRS activity names with no CLH source mapping (logged, not failed). */
	public int getSkippedCount()
	{
		return skippedCount;
	}

	@Override
	public String toString()
	{
		if (!success)
		{
			return "SyncResult{failure: " + errorMessage + "}";
		}
		return "SyncResult{success: " + kcBySource.size() + " sources, skipped=" + skippedCount + "}";
	}
}
