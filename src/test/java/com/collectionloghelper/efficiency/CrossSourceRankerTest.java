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

import com.collectionloghelper.AccountType;
import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.RaidTeamSize;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.CrossSourceRecommendation.SourceScore;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link CrossSourceRanker}.
 *
 * <p>Uses synthetic sources and items to verify:
 * <ul>
 *   <li>Single-source items pass through unmodified.</li>
 *   <li>Multi-source items are ranked with the fastest source first.</li>
 *   <li>Obtained items are excluded from all recommendations.</li>
 *   <li>Items with zero drop rates produce {@link Double#MAX_VALUE} expected seconds.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CrossSourceRankerTest
{
	@Mock
	private DropRateDatabase database;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;
	@Mock
	private CollectionLogHelperConfig config;
	@Mock
	private ClueCompletionEstimator clueEstimator;
	@Mock
	private SlayerTaskState slayerTaskState;
	@Mock
	private SlayerMasterDatabase slayerMasterDatabase;

	private EfficiencyCalculator efficiencyCalculator;
	private CrossSourceRanker ranker;

	@BeforeEach
	public void setUp() throws Exception
	{
		lenient().when(requirementsChecker.isAccessible(anyString())).thenReturn(true);
		lenient().when(config.hideLockedContent()).thenReturn(false);
		lenient().when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		lenient().when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		lenient().when(config.accountType()).thenReturn(AccountType.MAIN);
		lenient().when(slayerTaskState.isTaskActive()).thenReturn(false);
		lenient().when(clueEstimator.estimateCompletionSeconds(anyString())).thenReturn(0);

		Constructor<EfficiencyCalculator> ctor = EfficiencyCalculator.class.getDeclaredConstructor(
			DropRateDatabase.class, PlayerCollectionState.class,
			RequirementsChecker.class, CollectionLogHelperConfig.class,
			ClueCompletionEstimator.class, SlayerTaskState.class,
			SlayerMasterDatabase.class);
		ctor.setAccessible(true);
		efficiencyCalculator = ctor.newInstance(database, collectionState, requirementsChecker,
			config, clueEstimator, slayerTaskState, slayerMasterDatabase);

		Constructor<CrossSourceRanker> rankerCtor = CrossSourceRanker.class.getDeclaredConstructor(
			DropRateDatabase.class, PlayerCollectionState.class, EfficiencyCalculator.class);
		rankerCtor.setAccessible(true);
		ranker = rankerCtor.newInstance(database, collectionState, efficiencyCalculator);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private CollectionLogSource makeSource(String name, int killTimeSeconds, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null, null,
			null /* guidanceHelperKey */, null, 0, null, 0, items, null /* metaAuthoredDate */, null, null);
	}

	private CollectionLogItem makeItem(int id, String name, double dropRate)
	{
		return new CollectionLogItem(id, name, dropRate, false, null, 0, 0, false, false);
	}

	// -------------------------------------------------------------------------
	// scoreToSeconds utility
	// -------------------------------------------------------------------------

	@Test
	public void scoreToSeconds_zeroScore_returnsMaxValue()
	{
		assertEquals(Double.MAX_VALUE, CrossSourceRanker.scoreToSeconds(0.0), 0.0);
	}

	@Test
	public void scoreToSeconds_negativeScore_returnsMaxValue()
	{
		assertEquals(Double.MAX_VALUE, CrossSourceRanker.scoreToSeconds(-1.0), 0.0);
	}

	@Test
	public void scoreToSeconds_positiveScore_returnsCorrectSeconds()
	{
		// score = 100 => hoursToObtain = 1 => seconds = 3600
		assertEquals(3600.0, CrossSourceRanker.scoreToSeconds(100.0), 0.001);
	}

	@Test
	public void scoreToSeconds_halfScore_returnsDoubleTime()
	{
		// score = 50 => hoursToObtain = 2 => seconds = 7200
		assertEquals(7200.0, CrossSourceRanker.scoreToSeconds(50.0), 0.001);
	}

	// -------------------------------------------------------------------------
	// Single-source items pass through
	// -------------------------------------------------------------------------

	@Test
	public void rank_singleSourceItem_appearsInResult()
	{
		CollectionLogItem item = makeItem(1001, "Dragon axe", 1.0 / 512.0);
		CollectionLogSource source = makeSource("Dagannoth Kings", 120, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(1001)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(1, results.size());
		CrossSourceRecommendation rec = results.get(0);
		assertEquals(1001, rec.getItemId());
		assertEquals("Dragon axe", rec.getItemName());
		assertEquals(1, rec.getSources().size());
		assertNotNull(rec.getBest());
		assertEquals(source, rec.getBest().getSource());
	}

	@Test
	public void rank_singleSourceItem_bestMatchesOnlySource()
	{
		CollectionLogItem item = makeItem(2001, "Abyssal whip", 1.0 / 512.0);
		CollectionLogSource source = makeSource("Abyssal Sire", 90, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(2001)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(source, results.get(0).getBest().getSource());
	}

	// -------------------------------------------------------------------------
	// Multi-source items ranked correctly
	// -------------------------------------------------------------------------

	/**
	 * Item 3001 appears in two sources with the same drop rate but different kill
	 * times. Source A has a 3x faster kill time, so it must rank first.
	 */
	@Test
	public void rank_multiSourceItem_fastestSourceRankedFirst()
	{
		double dropRate = 1.0 / 100.0;
		CollectionLogItem item = makeItem(3001, "Twisted bow", dropRate);

		CollectionLogSource sourceA = makeSource("CoX Fast", 30, Arrays.asList(item));
		CollectionLogSource sourceB = makeSource("CoX Slow", 90, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Arrays.asList(sourceA, sourceB));
		when(collectionState.isItemObtained(3001)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(1, results.size());
		CrossSourceRecommendation rec = results.get(0);
		assertEquals(3001, rec.getItemId());
		assertEquals(2, rec.getSources().size());

		assertEquals(sourceA, rec.getBest().getSource());
		assertTrue(rec.getSources().get(0).getExpectedSeconds() < rec.getSources().get(1).getExpectedSeconds());
	}

	@Test
	public void rank_multiSourceItem_sourcesAreSortedAscendingByExpectedSeconds()
	{
		double dropRate = 1.0 / 200.0;
		CollectionLogItem item = makeItem(4001, "Ancestral hat", dropRate);

		// sourceB is faster (60s kill time vs 120s)
		CollectionLogSource sourceA = makeSource("Chambers of Xeric", 120, Arrays.asList(item));
		CollectionLogSource sourceB = makeSource("Tombs of Amascut", 60, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Arrays.asList(sourceA, sourceB));
		when(collectionState.isItemObtained(4001)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		CrossSourceRecommendation rec = results.get(0);
		List<SourceScore> sources = rec.getSources();

		for (int i = 0; i < sources.size() - 1; i++)
		{
			assertTrue(sources.get(i).getExpectedSeconds() <= sources.get(i + 1).getExpectedSeconds());
		}
		assertEquals(sourceB, rec.getBest().getSource());
	}

	// -------------------------------------------------------------------------
	// Obtained items excluded
	// -------------------------------------------------------------------------

	@Test
	public void rank_obtainedItem_excludedFromResults()
	{
		CollectionLogItem item = makeItem(5001, "Bandos chestplate", 1.0 / 384.0);
		CollectionLogSource source = makeSource("General Graardor", 120, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(5001)).thenReturn(true);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertTrue(results.isEmpty());
	}

	@Test
	public void rank_mixedObtainedAndUnobtained_onlyUnobtainedReturned()
	{
		CollectionLogItem obtained = makeItem(6001, "Bandos chestplate", 1.0 / 384.0);
		CollectionLogItem unobtained = makeItem(6002, "Bandos tassets", 1.0 / 384.0);
		CollectionLogSource source = makeSource("General Graardor", 120, Arrays.asList(obtained, unobtained));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(6001)).thenReturn(true);
		when(collectionState.isItemObtained(6002)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(1, results.size());
		assertEquals(6002, results.get(0).getItemId());
	}

	@Test
	public void rank_allItemsObtained_returnsEmptyList()
	{
		CollectionLogItem item1 = makeItem(7001, "Armadyl chestplate", 1.0 / 381.0);
		CollectionLogItem item2 = makeItem(7002, "Armadyl chainskirt", 1.0 / 381.0);
		CollectionLogSource source = makeSource("Kree'arra", 90, Arrays.asList(item1, item2));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(7001)).thenReturn(true);
		when(collectionState.isItemObtained(7002)).thenReturn(true);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertTrue(results.isEmpty());
	}

	// -------------------------------------------------------------------------
	// Outer list sorted by best expected seconds
	// -------------------------------------------------------------------------

	@Test
	public void rank_outerListSortedByBestExpectedSeconds()
	{
		// Item A: very slow to obtain
		CollectionLogItem itemA = makeItem(8001, "Slow item", 1.0 / 1000.0);
		CollectionLogSource sourceA = makeSource("Slow Boss", 1800, Arrays.asList(itemA));

		// Item B: fast to obtain
		CollectionLogItem itemB = makeItem(8002, "Fast item", 1.0 / 10.0);
		CollectionLogSource sourceB = makeSource("Fast Boss", 60, Arrays.asList(itemB));

		when(database.getAllSources()).thenReturn(Arrays.asList(sourceA, sourceB));
		when(collectionState.isItemObtained(8001)).thenReturn(false);
		when(collectionState.isItemObtained(8002)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(2, results.size());
		assertEquals(8002, results.get(0).getItemId());
		assertEquals(8001, results.get(1).getItemId());
		assertTrue(results.get(0).getBest().getExpectedSeconds()
			< results.get(1).getBest().getExpectedSeconds());
	}

	// -------------------------------------------------------------------------
	// Zero drop rate items
	// -------------------------------------------------------------------------

	@Test
	public void rank_zeroDropRateItem_expectedSecondsIsMaxValue()
	{
		CollectionLogItem item = makeItem(9001, "Zero rate item", 0.0);
		CollectionLogSource source = makeSource("Mystery Source", 120, Arrays.asList(item));

		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(collectionState.isItemObtained(9001)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		// Item still included (fallback count score keeps source non-null)
		assertFalse(results.isEmpty());
		assertEquals(Double.MAX_VALUE, results.get(0).getBest().getExpectedSeconds(), 0.0);
	}

	// -------------------------------------------------------------------------
	// Non-overlapping sources
	// -------------------------------------------------------------------------

	@Test
	public void rank_twoSourcesNoOverlap_bothItemsPresent()
	{
		CollectionLogItem item1 = makeItem(10001, "Whip", 1.0 / 512.0);
		CollectionLogItem item2 = makeItem(10002, "Visage", 1.0 / 5000.0);
		CollectionLogSource source1 = makeSource("Abyssal Sire", 90, Arrays.asList(item1));
		CollectionLogSource source2 = makeSource("Skeletal Wyvern", 120, Arrays.asList(item2));

		when(database.getAllSources()).thenReturn(Arrays.asList(source1, source2));
		when(collectionState.isItemObtained(10001)).thenReturn(false);
		when(collectionState.isItemObtained(10002)).thenReturn(false);

		List<CrossSourceRecommendation> results = ranker.rank(false);

		assertEquals(2, results.size());
		assertTrue(results.stream().allMatch(r -> r.getSources().size() == 1));
	}

	// -------------------------------------------------------------------------
	// Result immutability
	// -------------------------------------------------------------------------

	@Test
	public void rank_returnedList_isImmutable()
	{
		when(database.getAllSources()).thenReturn(Collections.emptyList());

		List<CrossSourceRecommendation> results = ranker.rank(false);

		try
		{
			results.add(null);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException expected)
		{
			// correct
		}
	}
}
