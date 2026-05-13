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

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import lombok.Value;

/**
 * A single collection-log item together with its parent source and the
 * per-item efficiency score, used to drive the sorted item list inside
 * Category Focus mode.
 *
 * <p>Sort order (descending efficiency, obtained items sink):
 * <ol>
 *   <li>Unobtained items rank first, ordered by descending {@link #score}.</li>
 *   <li>Obtained items rank after all unobtained, also ordered by descending score.</li>
 *   <li>Ties on score break alphabetically on {@link CollectionLogItem#getName()}.</li>
 * </ol>
 *
 * <p>Locked items interleave with unlocked items at their natural score position;
 * they are NOT segregated to the bottom (consistent with Efficient mode, issue #392).
 */
@Value
public class RankedCategoryItem
{
	CollectionLogItem item;
	CollectionLogSource source;
	/**
	 * Per-item efficiency score (same units as {@link ScoredItem#getScore()}).
	 * Computed by {@link EfficiencyCalculator#scoreIndividualItem}.
	 */
	double score;
	boolean locked;
	boolean obtained;
}
