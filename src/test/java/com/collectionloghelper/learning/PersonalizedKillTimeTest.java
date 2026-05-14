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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import java.util.Collections;
import java.util.OptionalInt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PersonalizedKillTime}.
 *
 * <p>Verifies the preference ordering: learned data wins when present;
 * falls back to the static {@link CollectionLogSource#getKillTimeSeconds()} otherwise.
 *
 * <p>{@link CollectionLogSource} is a Lombok {@code @Value} final class and cannot be mocked;
 * tests construct real instances via the all-args constructor helper.
 */
@RunWith(MockitoJUnitRunner.class)
public class PersonalizedKillTimeTest
{
	private static final String SOURCE_NAME = "Giant Mole";
	private static final int STATIC_KILL_TIME = 120;
	private static final int LEARNED_KILL_TIME = 75;

	@Mock
	private KillTimeTracker tracker;

	private PersonalizedKillTime personalizedKillTime;

	@Before
	public void setUp()
	{
		personalizedKillTime = new PersonalizedKillTime(tracker);
	}

	// ── Helper ───────────────────────────────────────────────────────────────────

	private static CollectionLogSource makeSource(String name, int killTimeSeconds)
	{
		return new CollectionLogSource(
			name, CollectionLogCategory.BOSSES,
			/* worldX */ 0, /* worldY */ 0, /* worldPlane */ 0,
			killTimeSeconds, /* ironKillTimeSeconds */ 0,
			/* locationDescription */ null, /* waypoints */ null,
			/* rewardType */ null, /* pointsPerHour */ 0,
			/* mutuallyExclusiveSources */ null, /* rollsPerKill */ 0,
			/* aggregated */ false, /* afkLevel */ 0, /* travelTip */ null,
			/* npcId */ 0, /* interactAction */ null, /* dialogOptions */ null,
			/* guidanceSteps */ null, /* requirements */ null,
			/* cumulativeTrackItemId */ 0, /* cumulativeTrackObjectIds */ null,
			/* cumulativeTrackThreshold */ 0,
			Collections.emptyList());
	}

	// ── No learned data ──────────────────────────────────────────────────────────

	@Test
	public void noLearnedData_returnsStaticKillTime()
	{
		CollectionLogSource source = makeSource(SOURCE_NAME, STATIC_KILL_TIME);
		when(tracker.getLearnedKillTime(SOURCE_NAME)).thenReturn(OptionalInt.empty());

		int result = personalizedKillTime.getEffectiveBaseKillTime(source);

		assertEquals(STATIC_KILL_TIME, result);
	}

	@Test
	public void sourceWithZeroStaticKillTime_noLearnedData_returnsZero()
	{
		CollectionLogSource source = makeSource(SOURCE_NAME, 0);
		when(tracker.getLearnedKillTime(SOURCE_NAME)).thenReturn(OptionalInt.empty());

		int result = personalizedKillTime.getEffectiveBaseKillTime(source);

		assertEquals(0, result);
	}

	// ── Learned data present ─────────────────────────────────────────────────────

	@Test
	public void learnedDataPresent_returnsLearnedKillTime()
	{
		CollectionLogSource source = makeSource(SOURCE_NAME, STATIC_KILL_TIME);
		when(tracker.getLearnedKillTime(SOURCE_NAME)).thenReturn(OptionalInt.of(LEARNED_KILL_TIME));

		int result = personalizedKillTime.getEffectiveBaseKillTime(source);

		assertEquals(LEARNED_KILL_TIME, result);
	}

	@Test
	public void learnedDataFasterThanStatic_returnsFasterValue()
	{
		CollectionLogSource source = makeSource(SOURCE_NAME, STATIC_KILL_TIME);
		int fastLearned = 30;
		when(tracker.getLearnedKillTime(SOURCE_NAME)).thenReturn(OptionalInt.of(fastLearned));

		int result = personalizedKillTime.getEffectiveBaseKillTime(source);

		assertEquals(fastLearned, result);
	}

	@Test
	public void sourceWithZeroStaticKillTime_learnedDataStillWins()
	{
		CollectionLogSource source = makeSource(SOURCE_NAME, 0);
		when(tracker.getLearnedKillTime(SOURCE_NAME)).thenReturn(OptionalInt.of(LEARNED_KILL_TIME));

		int result = personalizedKillTime.getEffectiveBaseKillTime(source);

		assertEquals(LEARNED_KILL_TIME, result);
	}
}
