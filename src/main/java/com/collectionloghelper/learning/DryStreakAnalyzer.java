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
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Computes the dry-streak feed for a player's collection log state.
 *
 * <p>For each unobtained probabilistic item the analyzer:
 * <ol>
 *   <li>Derives the median expected kill count using the formula
 *       {@code medianKc = log(0.5) / log(1 - dropRate)}.</li>
 *   <li>Divides the actual kill count for the item's source by that median to
 *       produce a <em>dryness ratio</em> ({@code multipleOfExpected}).</li>
 *   <li>Classifies the ratio into {@link DrynessClass#NORMAL} (&lt;= 2x),
 *       {@link DrynessClass#DRY} (&gt; 2x and &lt;= 5x), or
 *       {@link DrynessClass#VERY_DRY} (&gt; 5x).</li>
 * </ol>
 *
 * <p>Items with a zero or negative drop rate, guaranteed drops (rate &gt;= 1),
 * and items classified as {@link DrynessClass#NORMAL} are excluded from the
 * result list.
 *
 * <p>The returned list is sorted by {@code multipleOfExpected} descending so the
 * worst dry streaks appear first. This is the default feed sort; callers may
 * re-sort by other criteria after receiving the list.
 *
 * <p>KC data is supplied as a {@code Map<String, Integer>} keyed by source name
 * (case-sensitive, matching {@link CollectionLogSource#getName()}). This keeps
 * the analyzer decoupled from any specific KC-data source (in-game varps,
 * TempleOSRS sync, manual entry, etc.).
 */
@Slf4j
@Singleton
public class DryStreakAnalyzer
{
	/** Lower bound: ratios at or below this threshold are classified NORMAL and excluded. */
	static final double DRY_THRESHOLD = 2.0;
	/** Upper bound: ratios above this threshold are classified VERY_DRY. */
	static final double VERY_DRY_THRESHOLD = 5.0;

	private final DropRateDatabase database;

	@Inject
	public DryStreakAnalyzer(DropRateDatabase database)
	{
		this.database = database;
	}

	/**
	 * Computes the dry-streak feed.
	 *
	 * @param collectionState current player collection state (obtained item IDs)
	 * @param killCountsBySource kill counts keyed by source name; sources absent
	 *                           from the map contribute no entries to the feed
	 * @return entries where {@code multipleOfExpected > 2}, sorted by
	 *         {@code multipleOfExpected} descending (worst streak first)
	 */
	public List<DryStreakEntry> analyze(
		PlayerCollectionState collectionState,
		Map<String, Integer> killCountsBySource)
	{
		if (killCountsBySource == null || killCountsBySource.isEmpty())
		{
			return Collections.emptyList();
		}

		List<DryStreakEntry> feed = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			Integer actualKc = killCountsBySource.get(source.getName());
			if (actualKc == null || actualKc <= 0)
			{
				continue;
			}

			for (CollectionLogItem item : source.getItems())
			{
				if (collectionState.isItemObtained(item.getItemId()))
				{
					continue;
				}

				double dropRate = item.getDropRate();
				if (dropRate <= 0.0 || dropRate >= 1.0)
				{
					// Cannot compute a meaningful median for guaranteed or zero-rate items
					continue;
				}

				double medianKc = computeMedianKc(dropRate);
				if (medianKc <= 0)
				{
					continue;
				}

				double ratio = actualKc / medianKc;
				if (ratio <= DRY_THRESHOLD)
				{
					continue;
				}

				DrynessClass cls = ratio > VERY_DRY_THRESHOLD ? DrynessClass.VERY_DRY : DrynessClass.DRY;
				feed.add(new DryStreakEntry(item, source, ratio, cls));
			}
		}

		feed.sort(Comparator.comparingDouble(DryStreakEntry::getMultipleOfExpected).reversed());
		return Collections.unmodifiableList(feed);
	}

	/**
	 * Returns the median kill count needed for a 50% chance of having received a
	 * drop at least once, using the formula:
	 *
	 * <pre>medianKc = log(0.5) / log(1 - dropRate)</pre>
	 *
	 * @param dropRate per-kill drop probability in (0, 1)
	 * @return median expected KC, or 0 if the result is not positive
	 */
	public static double computeMedianKc(double dropRate)
	{
		if (dropRate <= 0.0 || dropRate >= 1.0)
		{
			return 0;
		}
		double median = Math.log(0.5) / Math.log(1.0 - dropRate);
		return median > 0 ? median : 0;
	}

	/**
	 * Classifies a dryness ratio against the standard thresholds.
	 *
	 * @param ratio {@code actualKc / medianExpectedKc}
	 * @return the corresponding {@link DrynessClass}
	 */
	public static DrynessClass classify(double ratio)
	{
		if (ratio > VERY_DRY_THRESHOLD)
		{
			return DrynessClass.VERY_DRY;
		}
		if (ratio > DRY_THRESHOLD)
		{
			return DrynessClass.DRY;
		}
		return DrynessClass.NORMAL;
	}
}
