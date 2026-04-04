/*
 * Copyright (c) 2025, Chandler
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClueCompletionEstimatorTest
{
	private static final int VARP_QUEST_POINTS = 101;

	@Mock
	private Client client;

	private ClueCompletionEstimator estimator;

	@Before
	public void setUp() throws Exception
	{
		Constructor<ClueCompletionEstimator> ctor =
			ClueCompletionEstimator.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		estimator = ctor.newInstance(client);
	}

	private void setCachedBucket(ClueCompletionEstimator.ProgressionBucket bucket) throws Exception
	{
		Field field = ClueCompletionEstimator.class.getDeclaredField("cachedBucket");
		field.setAccessible(true);
		field.set(estimator, bucket);
	}

	// ========================================================================
	// getTierIndex — static method, no client needed
	// ========================================================================

	@Test
	public void getTierIndex_beginner()
	{
		assertEquals(0, ClueCompletionEstimator.getTierIndex("Beginner Treasure Trails"));
	}

	@Test
	public void getTierIndex_easy()
	{
		assertEquals(1, ClueCompletionEstimator.getTierIndex("Easy Treasure Trails"));
	}

	@Test
	public void getTierIndex_medium()
	{
		assertEquals(2, ClueCompletionEstimator.getTierIndex("Medium Treasure Trails"));
	}

	@Test
	public void getTierIndex_hard()
	{
		assertEquals(3, ClueCompletionEstimator.getTierIndex("Hard Treasure Trails"));
	}

	@Test
	public void getTierIndex_elite()
	{
		assertEquals(4, ClueCompletionEstimator.getTierIndex("Elite Treasure Trails"));
	}

	@Test
	public void getTierIndex_master()
	{
		assertEquals(5, ClueCompletionEstimator.getTierIndex("Master Treasure Trails"));
	}

	@Test
	public void getTierIndex_null()
	{
		assertEquals(-1, ClueCompletionEstimator.getTierIndex(null));
	}

	@Test
	public void getTierIndex_nonClue()
	{
		assertEquals(-1, ClueCompletionEstimator.getTierIndex("General Graardor"));
	}

	@Test
	public void getTierIndex_caseInsensitive()
	{
		assertEquals(3, ClueCompletionEstimator.getTierIndex("HARD treasure trails"));
	}

	// ========================================================================
	// formatTime — static utility
	// ========================================================================

	@Test
	public void formatTime_underOneMinute()
	{
		assertEquals("45s", ClueCompletionEstimator.formatTime(45));
	}

	@Test
	public void formatTime_exactMinutes()
	{
		assertEquals("2 min", ClueCompletionEstimator.formatTime(120));
	}

	@Test
	public void formatTime_minutesAndSeconds()
	{
		assertEquals("1 min 30s", ClueCompletionEstimator.formatTime(90));
	}

	@Test
	public void formatTime_zeroSeconds()
	{
		assertEquals("0s", ClueCompletionEstimator.formatTime(0));
	}

	@Test
	public void formatTime_oneSecond()
	{
		assertEquals("1s", ClueCompletionEstimator.formatTime(1));
	}

	@Test
	public void formatTime_exactlyOneMinute()
	{
		assertEquals("1 min", ClueCompletionEstimator.formatTime(60));
	}

	@Test
	public void formatTime_largeValue()
	{
		assertEquals("20 min", ClueCompletionEstimator.formatTime(1200));
	}

	// ========================================================================
	// estimateCompletionSeconds — uses cached bucket
	// ========================================================================

	@Test
	public void estimateCompletionSeconds_nonClueReturnsNegativeOne() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.LATE);
		assertEquals(-1, estimator.estimateCompletionSeconds("General Graardor"));
	}

	@Test
	public void estimateCompletionSeconds_earlyBeginner() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.EARLY);
		assertEquals(120, estimator.estimateCompletionSeconds("Beginner Treasure Trails"));
	}

	@Test
	public void estimateCompletionSeconds_midHard() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.MID);
		assertEquals(420, estimator.estimateCompletionSeconds("Hard Treasure Trails"));
	}

	@Test
	public void estimateCompletionSeconds_lateMaster() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.LATE);
		assertEquals(600, estimator.estimateCompletionSeconds("Master Treasure Trails"));
	}

	@Test
	public void estimateCompletionSeconds_maxedEasy() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.MAXED);
		assertEquals(90, estimator.estimateCompletionSeconds("Easy Treasure Trails"));
	}

	@Test
	public void estimateCompletionSeconds_maxedElite() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.MAXED);
		assertEquals(300, estimator.estimateCompletionSeconds("Elite Treasure Trails"));
	}

	// ========================================================================
	// getBucket / resetBucket — caching behavior
	// ========================================================================

	@Test
	public void resetBucket_clearsCachedValue() throws Exception
	{
		setCachedBucket(ClueCompletionEstimator.ProgressionBucket.MAXED);
		estimator.resetBucket();

		// After reset, getBucket should recompute — mock early-game stats
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(1);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(0);

		assertEquals(ClueCompletionEstimator.ProgressionBucket.EARLY, estimator.getBucket());
	}

	@Test
	public void getBucket_earlyGame() throws Exception
	{
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(30);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(10);

		assertEquals(ClueCompletionEstimator.ProgressionBucket.EARLY, estimator.getBucket());
	}

	@Test
	public void getBucket_midGame() throws Exception
	{
		// Total level ~1380 (60 * 23), combat ~80+
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(60);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(50);

		assertEquals(ClueCompletionEstimator.ProgressionBucket.MID, estimator.getBucket());
	}

	@Test
	public void getBucket_lateGame() throws Exception
	{
		// Total level ~2070 (90 * 23), high combat, many quests
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(90);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(250);

		assertEquals(ClueCompletionEstimator.ProgressionBucket.LATE, estimator.getBucket());
	}

	@Test
	public void getBucket_maxed() throws Exception
	{
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(99);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(300);

		assertEquals(ClueCompletionEstimator.ProgressionBucket.MAXED, estimator.getBucket());
	}

	@Test
	public void getBucket_cachedOnSecondCall() throws Exception
	{
		for (Skill skill : Skill.values())
		{
			lenient().when(client.getRealSkillLevel(skill)).thenReturn(99);
		}
		when(client.getVarpValue(VARP_QUEST_POINTS)).thenReturn(300);

		ClueCompletionEstimator.ProgressionBucket first = estimator.getBucket();
		ClueCompletionEstimator.ProgressionBucket second = estimator.getBucket();

		assertSame(first, second);
	}

	// ========================================================================
	// ProgressionBucket display names
	// ========================================================================

	@Test
	public void progressionBucket_displayNames()
	{
		assertEquals("Early-game", ClueCompletionEstimator.ProgressionBucket.EARLY.getDisplayName());
		assertEquals("Mid-game", ClueCompletionEstimator.ProgressionBucket.MID.getDisplayName());
		assertEquals("Late-game", ClueCompletionEstimator.ProgressionBucket.LATE.getDisplayName());
		assertEquals("Maxed", ClueCompletionEstimator.ProgressionBucket.MAXED.getDisplayName());
	}
}
