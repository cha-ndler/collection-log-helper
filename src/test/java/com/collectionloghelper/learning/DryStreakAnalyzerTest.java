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
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DryStreakAnalyzer}.
 *
 * <p>Uses synthetic drop rates and KC data so the tests are deterministic and
 * independent of the production {@code drop_rates.json} fixture.
 */
@RunWith(MockitoJUnitRunner.class)
public class DryStreakAnalyzerTest
{
	/** 1/128 drop rate — a common OSRS rare drop denomination. */
	private static final double RATE_1_IN_128 = 1.0 / 128.0;
	/** 1/512 drop rate — very rare. */
	private static final double RATE_1_IN_512 = 1.0 / 512.0;

	@Mock
	private DropRateDatabase database;

	@Mock
	private PlayerCollectionState collectionState;

	private DryStreakAnalyzer analyzer;

	// Synthetic items
	private CollectionLogItem rareItem;
	private CollectionLogItem veryRareItem;
	private CollectionLogItem guaranteedItem;
	private CollectionLogItem zeroRateItem;

	// Synthetic sources
	private CollectionLogSource sourceA;
	private CollectionLogSource sourceB;

	@Before
	public void setUp()
	{
		analyzer = new DryStreakAnalyzer(database);

		rareItem = new CollectionLogItem(1001, "Rare Drop", RATE_1_IN_128,
			false, null, 0, 0, false, false);
		veryRareItem = new CollectionLogItem(1002, "Very Rare Drop", RATE_1_IN_512,
			false, null, 0, 0, false, false);
		guaranteedItem = new CollectionLogItem(1003, "Guaranteed Item", 1.0,
			false, null, 0, 100, false, false);
		zeroRateItem = new CollectionLogItem(1004, "Zero Rate Item", 0.0,
			false, null, 0, 0, false, false);

		sourceA = makeSource("Boss Alpha", CollectionLogCategory.BOSSES,
			Arrays.asList(rareItem, guaranteedItem, zeroRateItem));
		sourceB = makeSource("Boss Beta", CollectionLogCategory.BOSSES,
			Collections.singletonList(veryRareItem));

		when(database.getAllSources()).thenReturn(Arrays.asList(sourceA, sourceB));
	}

	// ── computeMedianKc ─────────────────────────────────────────────────────

	@Test
	public void computeMedianKc_rate1in128_returnsExpectedValue()
	{
		// median = log(0.5) / log(1 - 1/128) ≈ 88.7
		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		assertEquals(88.7, median, 0.5);
	}

	@Test
	public void computeMedianKc_rate1in512_returnsExpectedValue()
	{
		// median = log(0.5) / log(1 - 1/512) ≈ 354.9
		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_512);
		assertEquals(354.9, median, 0.5);
	}

	@Test
	public void computeMedianKc_zeroRate_returnsZero()
	{
		assertEquals(0.0, DryStreakAnalyzer.computeMedianKc(0.0), 0.0);
	}

	@Test
	public void computeMedianKc_oneRate_returnsZero()
	{
		assertEquals(0.0, DryStreakAnalyzer.computeMedianKc(1.0), 0.0);
	}

	@Test
	public void computeMedianKc_negativeRate_returnsZero()
	{
		assertEquals(0.0, DryStreakAnalyzer.computeMedianKc(-0.1), 0.0);
	}

	// ── classify ────────────────────────────────────────────────────────────

	@Test
	public void classify_ratioAtExactlyTwo_returnsNormal()
	{
		assertEquals(DrynessClass.NORMAL, DryStreakAnalyzer.classify(2.0));
	}

	@Test
	public void classify_ratioJustAboveTwo_returnsDry()
	{
		assertEquals(DrynessClass.DRY, DryStreakAnalyzer.classify(2.01));
	}

	@Test
	public void classify_ratioAtExactlyFive_returnsDry()
	{
		assertEquals(DrynessClass.DRY, DryStreakAnalyzer.classify(5.0));
	}

	@Test
	public void classify_ratioJustAboveFive_returnsVeryDry()
	{
		assertEquals(DrynessClass.VERY_DRY, DryStreakAnalyzer.classify(5.01));
	}

	@Test
	public void classify_ratioTen_returnsVeryDry()
	{
		assertEquals(DrynessClass.VERY_DRY, DryStreakAnalyzer.classify(10.0));
	}

	@Test
	public void classify_ratioOne_returnsNormal()
	{
		assertEquals(DrynessClass.NORMAL, DryStreakAnalyzer.classify(1.0));
	}

	// ── analyze — empty and guard cases ─────────────────────────────────────

	@Test
	public void analyze_nullKcMap_returnsEmptyList()
	{
		List<DryStreakEntry> result = analyzer.analyze(collectionState, null);
		assertTrue(result.isEmpty());
	}

	@Test
	public void analyze_emptyKcMap_returnsEmptyList()
	{
		List<DryStreakEntry> result = analyzer.analyze(collectionState,
			Collections.emptyMap());
		assertTrue(result.isEmpty());
	}

	@Test
	public void analyze_sourceNotInKcMap_producesNoEntries()
	{
		// Only sourceB is in the map, and its item is not obtained
		when(collectionState.isItemObtained(1002)).thenReturn(false);

		Map<String, Integer> kc = Collections.singletonMap("Boss Beta", 1);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);
		// 1 kill vs median ~355 → ratio 0.003 → NORMAL → excluded
		assertTrue(result.isEmpty());
	}

	// ── analyze — dryness classification via KC data ─────────────────────────

	@Test
	public void analyze_kcAtExactlyTwoTimesMedian_excluded()
	{
		when(collectionState.isItemObtained(1001)).thenReturn(false);
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		// Use floor to ensure the integer KC does not exceed 2x the (continuous) median.
		// Math.round can push the ratio just above 2.0 due to integer truncation.
		int kcAtOrBelowTwoX = (int) Math.floor(median * 2.0);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", kcAtOrBelowTwoX);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);
		// Ratio <= 2.0 → NORMAL → excluded from feed
		assertTrue(result.isEmpty());
	}

	@Test
	public void analyze_kcSlightlyAboveTwoTimesMedian_classifiedAsDry()
	{
		when(collectionState.isItemObtained(1001)).thenReturn(false);
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		// Use 3x median to safely land in DRY (>2x, <=5x)
		int kcAtThreeX = (int) Math.round(median * 3.0);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", kcAtThreeX);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);

		assertEquals(1, result.size());
		assertEquals(DrynessClass.DRY, result.get(0).getClassification());
		assertEquals(rareItem, result.get(0).getItem());
	}

	@Test
	public void analyze_kcAboveFiveTimesMedian_classifiedAsVeryDry()
	{
		when(collectionState.isItemObtained(1001)).thenReturn(false);
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		int kcAtSixX = (int) Math.round(median * 6.0);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", kcAtSixX);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);

		assertEquals(1, result.size());
		assertEquals(DrynessClass.VERY_DRY, result.get(0).getClassification());
	}

	@Test
	public void analyze_guaranteedItemIgnored()
	{
		// Only guaranteed item is missing — should not appear in feed
		when(collectionState.isItemObtained(1001)).thenReturn(true);
		when(collectionState.isItemObtained(1003)).thenReturn(false);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		int bigKc = (int) Math.round(median * 10.0);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", bigKc);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);
		assertTrue("Guaranteed items must be excluded from the dry-streak feed", result.isEmpty());
	}

	@Test
	public void analyze_zeroRateItemIgnored()
	{
		// Only zero-rate item is missing
		when(collectionState.isItemObtained(1001)).thenReturn(true);
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(false);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", 99999);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);
		assertTrue("Zero-rate items must be excluded from the dry-streak feed", result.isEmpty());
	}

	@Test
	public void analyze_obtainedItemsExcluded()
	{
		when(collectionState.isItemObtained(1001)).thenReturn(true); // obtained
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		int bigKc = (int) Math.round(median * 10.0);

		Map<String, Integer> kc = Collections.singletonMap("Boss Alpha", bigKc);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);
		assertTrue("Obtained items must not appear in the dry-streak feed", result.isEmpty());
	}

	// ── analyze — sort order ─────────────────────────────────────────────────

	@Test
	public void analyze_multipleEntries_sortedByRatioDescending()
	{
		// sourceA has rareItem (1/128), sourceB has veryRareItem (1/512)
		// Give sourceA a huge KC relative to its median and sourceB a smaller ratio
		when(collectionState.isItemObtained(1001)).thenReturn(false);
		when(collectionState.isItemObtained(1002)).thenReturn(false);
		when(collectionState.isItemObtained(1003)).thenReturn(true);
		when(collectionState.isItemObtained(1004)).thenReturn(true);

		double medianA = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);   // ~89
		double medianB = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_512);   // ~355

		// sourceA ratio ~8x (VERY_DRY), sourceB ratio ~3x (DRY)
		int kcA = (int) Math.round(medianA * 8.0);
		int kcB = (int) Math.round(medianB * 3.0);

		Map<String, Integer> kc = new HashMap<>();
		kc.put("Boss Alpha", kcA);
		kc.put("Boss Beta", kcB);

		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);

		assertEquals(2, result.size());
		// First entry must have higher ratio
		assertTrue("Feed must be sorted worst-streak-first",
			result.get(0).getMultipleOfExpected() > result.get(1).getMultipleOfExpected());
		assertEquals(DrynessClass.VERY_DRY, result.get(0).getClassification());
		assertEquals(DrynessClass.DRY, result.get(1).getClassification());
	}

	@Test
	public void analyze_multipleItemsSameSource_allDryItemsIncluded()
	{
		// sourceA has two unobtained drop-rate items at same KC
		CollectionLogItem itemX = new CollectionLogItem(2001, "Item X", RATE_1_IN_128,
			false, null, 0, 0, false, false);
		CollectionLogItem itemY = new CollectionLogItem(2002, "Item Y", RATE_1_IN_128,
			false, null, 0, 0, false, false);
		CollectionLogSource twoItemSource = makeSource("Two Item Boss",
			CollectionLogCategory.BOSSES, Arrays.asList(itemX, itemY));
		when(database.getAllSources()).thenReturn(Collections.singletonList(twoItemSource));
		when(collectionState.isItemObtained(2001)).thenReturn(false);
		when(collectionState.isItemObtained(2002)).thenReturn(false);

		double median = DryStreakAnalyzer.computeMedianKc(RATE_1_IN_128);
		int kcAtFourX = (int) Math.round(median * 4.0);

		Map<String, Integer> kc = Collections.singletonMap("Two Item Boss", kcAtFourX);
		List<DryStreakEntry> result = analyzer.analyze(collectionState, kc);

		assertEquals("Both unobtained items from the same source must appear", 2, result.size());
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private static CollectionLogSource makeSource(
		String name, CollectionLogCategory category, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(
			name, category,
			0, 0, 0,   // worldX, worldY, worldPlane
			60, 60,     // killTimeSeconds, ironKillTimeSeconds
			null, null, // locationDescription, waypoints
			null,       // rewardType
			0,          // pointsPerHour
			null,       // mutuallyExclusiveSources
			1,          // rollsPerKill
			false,      // aggregated
			0,          // afkLevel
			null,       // travelTip
			0,          // npcId
			null,       // interactAction
			null,       // dialogOptions
			null,       // guidanceSteps
			null,       // guidanceHelperKey
			null,       // requirements
			0,          // cumulativeTrackItemId
			null,       // cumulativeTrackObjectIds
			0,          // cumulativeTrackThreshold
			items,
			null        // metaAuthoredDate
		, null, null);
	}
}
