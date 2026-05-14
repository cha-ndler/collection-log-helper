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
package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogSource;
import java.util.List;
import lombok.Value;

/**
 * Per-item cross-source recommendation produced by {@link CrossSourceRanker}.
 *
 * <p>For each unobtained item that appears in more than one source this record
 * captures all candidate sources ranked by expected time to obtain, and
 * surfaces the single best source as {@link #best}.
 *
 * <p>Items available from only one source are included unchanged so callers do
 * not need to merge two lists.
 */
@Value
public class CrossSourceRecommendation
{
	/**
	 * The efficiency score for one (item, source) pair, expressed as expected
	 * seconds to obtain. Lower is better.
	 *
	 * <p>A value of {@link Double#MAX_VALUE} means the time cannot be computed
	 * for this source (zero drop rate or unknown kill time).
	 */
	@Value
	public static class SourceScore
	{
		/** The source that contains the item. */
		CollectionLogSource source;

		/**
		 * Expected seconds to obtain the item from {@link #source}.
		 * {@link Double#MAX_VALUE} when the time is indeterminate.
		 */
		double expectedSeconds;
	}

	/** Collection-log item ID of the recommended item. */
	int itemId;

	/** Human-readable item name for display without an extra lookup. */
	String itemName;

	/**
	 * All sources that contain this item, each scored by expected seconds,
	 * sorted from fastest (lowest seconds) to slowest.
	 */
	List<SourceScore> sources;

	/**
	 * The fastest source for this item — always the first element of
	 * {@link #sources}. Never null.
	 */
	SourceScore best;
}
