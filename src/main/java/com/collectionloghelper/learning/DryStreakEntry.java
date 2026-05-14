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
package com.collectionloghelper.learning;

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import lombok.Value;

/**
 * A single entry in the dry-streak feed representing an unobtained item whose
 * actual kill count significantly exceeds the statistically expected median kill
 * count for a 50% drop probability.
 *
 * <p>The {@code multipleOfExpected} field is the dryness ratio:
 * {@code actualKc / medianExpectedKc}. A value of 3.0 means the player has
 * done three times as many kills as the statistical median without receiving
 * the item. Items with a ratio at or below 2x are classified {@link DrynessClass#NORMAL}
 * and are excluded from the feed by default.
 */
@Value
public class DryStreakEntry
{
	CollectionLogItem item;
	CollectionLogSource source;
	/**
	 * Actual kill count divided by the median expected kill count
	 * ({@code log(0.5) / log(1 - dropRate)}).
	 * Zero if the expected KC could not be computed (e.g. zero drop rate).
	 */
	double multipleOfExpected;
	/** Dryness tier derived from {@link #multipleOfExpected}. */
	DrynessClass classification;
}
