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

import com.collectionloghelper.data.CollectionLogSource;
import java.util.OptionalInt;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves the effective kill time for a source, preferring per-account learned
 * data from {@link KillTimeTracker} over the static {@code killTimeSeconds} value
 * from the database.
 *
 * <p>When {@link KillTimeTracker#getLearnedKillTime(String)} returns empty (feature
 * disabled, not enough observations, or source not tracked), this service falls back
 * to {@link CollectionLogSource#getKillTimeSeconds()}.
 *
 * <p>The caller (typically {@link com.collectionloghelper.efficiency.EfficiencyCalculator})
 * retrieves the base kill time from here before applying category-level adjustments
 * such as raid team-size multipliers or Slayer overhead inflation.
 */
@Singleton
public class PersonalizedKillTime
{
	private final KillTimeTracker tracker;

	@Inject
	PersonalizedKillTime(KillTimeTracker tracker)
	{
		this.tracker = tracker;
	}

	/**
	 * Returns the best available kill time for {@code source}.
	 *
	 * <ul>
	 *   <li>If the tracker has learned data with at least one observation, returns the
	 *       rolling average (rounded to the nearest second).</li>
	 *   <li>Otherwise returns {@link CollectionLogSource#getKillTimeSeconds()} as the
	 *       static fallback.</li>
	 * </ul>
	 *
	 * @param source the source whose kill time is needed
	 * @return effective kill time in seconds, always &ge; 0
	 */
	public int getEffectiveBaseKillTime(CollectionLogSource source)
	{
		OptionalInt learned = tracker.getLearnedKillTime(source.getName());
		if (learned.isPresent())
		{
			return learned.getAsInt();
		}
		return source.getKillTimeSeconds();
	}
}
