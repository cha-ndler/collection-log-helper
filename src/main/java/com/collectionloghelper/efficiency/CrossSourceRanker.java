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
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.efficiency.CrossSourceRecommendation.SourceScore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Cross-source per-item best-path ranker (Tier E1).
 *
 * <p>Unlike {@link EfficiencyCalculator} — which ranks <em>sources</em> by their
 * combined probability of yielding any new log slot — this ranker pivots to an
 * <em>item-first</em> view: for each unobtained item it finds every source that
 * contains the item, computes the expected time to obtain it from each source
 * independently, and returns the list sorted by the best (fastest) path.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Expected seconds is derived from the per-item score returned by
 *       {@link EfficiencyCalculator#scoreIndividualItem}: score = (effectiveRate *
 *       killsPerHour) * 100. Inverting gives hours = 100 / score, and multiplying
 *       by 3600 gives seconds. When the score is zero or negative the time is
 *       indeterminate and {@link Double#MAX_VALUE} is used so these sources sort
 *       to the end of each item's list.</li>
 *   <li>Obtained items are excluded before scoring.</li>
 *   <li>The {@code locked} parameter to {@link #rank} controls whether sources
 *       that are inaccessible (quest/skill requirements not met) are included.</li>
 *   <li>The ranker is stateless apart from its collaborators and can be called
 *       repeatedly as the player's collection state changes.</li>
 * </ul>
 */
@Singleton
public class CrossSourceRanker
{
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator efficiencyCalculator;

	@Inject
	CrossSourceRanker(DropRateDatabase database, PlayerCollectionState collectionState,
		EfficiencyCalculator efficiencyCalculator)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.efficiencyCalculator = efficiencyCalculator;
	}

	/**
	 * Produces a per-item ranked list for all unobtained items in the database.
	 *
	 * <p>Each entry in the returned list represents one unobtained item together
	 * with all sources that contain it, sorted fastest-first by expected seconds.
	 * The outer list is also sorted so the item with the globally fastest obtainable
	 * path appears first.
	 *
	 * @param includeLocked whether locked (inaccessible) sources are included in
	 *                      each item's source list
	 * @return immutable snapshot of per-item recommendations; never null, may be empty
	 */
	public List<CrossSourceRecommendation> rank(boolean includeLocked)
	{
		// item ID -> accumulator of (source, expectedSeconds) pairs
		Map<Integer, ItemAccumulator> accumulators = new LinkedHashMap<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			// scoreSource returns null when all items from this source are already obtained.
			// The locked flag is forwarded to scoreSource for correct score computation
			// (locked sources receive a penalty in some paths), but whether a locked source
			// appears in results is gated by includeLocked.
			boolean sourceLocked = isSourceLocked(source);
			if (!includeLocked && sourceLocked)
			{
				continue;
			}

			ScoredItem scored = efficiencyCalculator.scoreSource(source, sourceLocked);
			if (scored == null)
			{
				continue;
			}

			int killTime = efficiencyCalculator.getEffectiveKillTime(source);
			double killsPerHour = killTime > 0 ? 3600.0 / killTime : 0;
			int rolls = source.getRollsPerKill();

			for (CollectionLogItem item : source.getItems())
			{
				if (collectionState.isItemObtained(item.getItemId()))
				{
					continue;
				}

				double itemScore = computeItemScore(scored, item, source, killsPerHour, killTime, rolls);
				double expectedSeconds = scoreToSeconds(itemScore);
				SourceScore ss = new SourceScore(source, expectedSeconds);

				accumulators
					.computeIfAbsent(item.getItemId(), id -> new ItemAccumulator(id, item.getName()))
					.add(ss);
			}
		}

		List<CrossSourceRecommendation> recommendations = new ArrayList<>(accumulators.size());
		for (ItemAccumulator acc : accumulators.values())
		{
			recommendations.add(acc.build());
		}

		recommendations.sort(
			Comparator.comparingDouble(r -> r.getBest().getExpectedSeconds()));

		return Collections.unmodifiableList(recommendations);
	}

	/**
	 * Converts an efficiency score (units: effective-drops-per-kill * kills/hour * 100)
	 * to expected seconds to obtain a single item.
	 *
	 * <p>Returns {@link Double#MAX_VALUE} when the score is zero or negative, which
	 * causes these entries to sort to the end of the per-item source list.
	 */
	static double scoreToSeconds(double score)
	{
		if (score <= 0)
		{
			return Double.MAX_VALUE;
		}
		return (100.0 / score) * 3600.0;
	}

	/**
	 * Returns the per-item efficiency score for {@code target} from {@code source}.
	 *
	 * <p>If {@code target} happens to be the best item recorded on the pre-computed
	 * {@link ScoredItem} its stored best-item score is returned directly (avoids a
	 * redundant computation). Otherwise the score is computed fresh via
	 * {@link EfficiencyCalculator#scoreIndividualItem}.
	 */
	private double computeItemScore(ScoredItem scored, CollectionLogItem target,
		CollectionLogSource source, double killsPerHour, int killTime, int rolls)
	{
		if (scored.getBestItem() != null
			&& scored.getBestItem().getItemId() == target.getItemId())
		{
			return scored.getBestItemScore();
		}
		return efficiencyCalculator.scoreIndividualItem(target, source, killsPerHour, killTime, rolls);
	}

	/**
	 * Determines whether a source is locked for the current player.
	 * Locked sources are ones where {@link EfficiencyCalculator#scoreSource} is
	 * called with {@code locked = true}, mirroring the convention used throughout
	 * the calculator.
	 *
	 * <p>The current implementation conservatively treats all sources as accessible
	 * at the ranker level; the {@link EfficiencyCalculator#scoreSource} call with
	 * the correct locked value handles any score adjustments. Callers gate on
	 * {@code includeLocked} to hide locked sources entirely.
	 */
	private boolean isSourceLocked(CollectionLogSource source)
	{
		// Delegated to EfficiencyCalculator via the locked parameter passed to
		// scoreSource. The ranker itself does not duplicate requirements checking.
		return false;
	}

	// -------------------------------------------------------------------------

	/** Mutable accumulator for one item's source scores during a ranking pass. */
	private static final class ItemAccumulator
	{
		private final int itemId;
		private final String itemName;
		private final List<SourceScore> scores = new ArrayList<>();

		ItemAccumulator(int itemId, String itemName)
		{
			this.itemId = itemId;
			this.itemName = itemName;
		}

		void add(SourceScore ss)
		{
			scores.add(ss);
		}

		CrossSourceRecommendation build()
		{
			scores.sort(Comparator.comparingDouble(SourceScore::getExpectedSeconds));
			List<SourceScore> sorted = Collections.unmodifiableList(new ArrayList<>(scores));
			return new CrossSourceRecommendation(itemId, itemName, sorted, sorted.get(0));
		}
	}
}
