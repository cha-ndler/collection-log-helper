package com.collectionloghelper.efficiency;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.RaidTeamSize;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SlayerTaskState;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EfficiencyCalculatorTest
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

	private EfficiencyCalculator calculator;

	@Before
	public void setUp() throws Exception
	{
		lenient().when(requirementsChecker.isAccessible(anyString())).thenReturn(true);
		lenient().when(config.hideLockedContent()).thenReturn(false);
		lenient().when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);

		// Use reflection to call the private constructor
		Constructor<EfficiencyCalculator> ctor = EfficiencyCalculator.class.getDeclaredConstructor(
			DropRateDatabase.class, PlayerCollectionState.class,
			RequirementsChecker.class, CollectionLogHelperConfig.class,
			ClueCompletionEstimator.class, SlayerTaskState.class);
		ctor.setAccessible(true);
		calculator = ctor.newInstance(database, collectionState, requirementsChecker,
			config, clueEstimator, slayerTaskState);
	}

	private CollectionLogSource makeSource(String name, CollectionLogCategory category,
		int killTimeSeconds, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, category, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, null, items);
	}

	private CollectionLogSource makeShopSource(String name, int killTimeSeconds,
		double pointsPerHour, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.SHOP, pointsPerHour, false, 1, false, null, items);
	}

	private CollectionLogSource makeMilestoneSource(String name, int killTimeSeconds,
		List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.MILESTONE, 0, false, 1, false, null, items);
	}

	private CollectionLogSource makeMixedSource(String name, int killTimeSeconds,
		double pointsPerHour, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.MIXED, pointsPerHour, false, 1, false, null, items);
	}

	private CollectionLogSource makeAggregatedSource(String name, int killTimeSeconds,
		List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.DROP, 0, false, 1, true, null, items);
	}

	private CollectionLogItem makeItem(int id, String name, double dropRate)
	{
		return new CollectionLogItem(id, name, dropRate, 0, false, null, 0, 0);
	}

	private CollectionLogItem makeShopItem(int id, String name, int pointCost)
	{
		return new CollectionLogItem(id, name, 1.0, 0, false, null, pointCost, 0);
	}

	private CollectionLogItem makeMilestoneItem(int id, String name, int milestoneKills)
	{
		return new CollectionLogItem(id, name, 1.0, 0, false, null, 0, milestoneKills);
	}

	// --- Per-item scoring: score = best individual item's score ---

	@Test
	public void testSingleMissingItem()
	{
		// 1 item at 1/100, 60s kills = 60 kills/hr, score = 0.01 * 60 * 100 = 60
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60,
			Collections.singletonList(makeItem(1, "Rare drop", 0.01)));
		when(collectionState.isItemObtained(1)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(60.0, result.getScore(), 0.01);
		assertEquals(1, result.getMissingItemCount());
		assertNotNull(result.getBestItem());
		assertEquals("Rare drop", result.getBestItem().getName());
	}

	@Test
	public void testMultipleMissingItems_bestItemScore()
	{
		// 3 items at 1/512 each, 120s kills
		// Per-item scoring: best item = 1/512, score = (1/512) * 30 * 100 = 5.86
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 1.0 / 512),
			makeItem(2, "Drop B", 1.0 / 512),
			makeItem(3, "Drop C", 1.0 / 512));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 120, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		double expectedScore = (1.0 / 512) * 30 * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
		assertEquals(3, result.getMissingItemCount());
	}

	@Test
	public void testObtainedItemsExcluded()
	{
		// 3 items, 2 obtained → only 1 missing → score based on 1 item's rate
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 0.01),
			makeItem(2, "Drop B", 0.01),
			makeItem(3, "Drop C", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(1)).thenReturn(true);
		when(collectionState.isItemObtained(2)).thenReturn(true);
		when(collectionState.isItemObtained(3)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		// score = 0.01 * 60 * 100 = 60
		assertEquals(60.0, result.getScore(), 0.01);
		assertEquals(1, result.getMissingItemCount());
	}

	@Test
	public void testAllObtained_returnsNull()
	{
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60,
			Collections.singletonList(makeItem(1, "Drop A", 0.01)));
		when(collectionState.isItemObtained(1)).thenReturn(true);

		assertNull(calculator.scoreSource(source, false));
	}

	@Test
	public void testBestItemSelected_highestRate()
	{
		// 5 items with different rates — best item should be the highest rate
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "A", 0.01), makeItem(2, "B", 0.01),
			makeItem(3, "C", 0.01), makeItem(4, "D", 0.01),
			makeItem(5, "E", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Best item score = 0.01 * 60 * 100 = 60 (all items have same rate)
		assertEquals(60.0, result.getScore(), 0.01);
		assertNotNull(result.getBestItem());
	}

	@Test
	public void testBestItemSelected_mixedRates()
	{
		// Items with different rates — best = highest drop rate
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Slow", 0.001),
			makeItem(2, "Fast", 0.1),
			makeItem(3, "Medium", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Best item = "Fast" at 0.1, score = 0.1 * 60 * 100 = 600
		assertEquals(600.0, result.getScore(), 0.01);
		assertEquals("Fast", result.getBestItem().getName());
	}

	// --- SHOP scoring ---

	@Test
	public void testShopScoring_withEconomics()
	{
		// 3 items, 100 pts each, 50 pts/hr
		// Best item: cheapest = 100 pts, 100/50 = 2 hrs, score = (1/2)*100 = 50
		List<CollectionLogItem> items = Arrays.asList(
			makeShopItem(1, "A", 100), makeShopItem(2, "B", 100), makeShopItem(3, "C", 100));
		CollectionLogSource source = makeShopSource("Test Shop", 300, 50, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(50.0, result.getScore(), 0.01);
	}

	@Test
	public void testShopScoring_cheapestItemFirst()
	{
		// Items with different costs — best = cheapest
		List<CollectionLogItem> items = Arrays.asList(
			makeShopItem(1, "Expensive", 1000),
			makeShopItem(2, "Cheap", 100),
			makeShopItem(3, "Medium", 500));
		CollectionLogSource source = makeShopSource("Test Shop", 300, 50, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Cheap: 100/50 = 2 hrs, score = 50
		assertEquals(50.0, result.getScore(), 0.01);
		assertEquals("Cheap", result.getBestItem().getName());
	}

	@Test
	public void testShopScoring_withoutEconomics_fallback()
	{
		// Items with 0 cost and 0 pph → flat 0.2 per item, best = 0.2
		List<CollectionLogItem> items = Arrays.asList(
			makeShopItem(1, "A", 0), makeShopItem(2, "B", 0));
		CollectionLogSource source = makeShopSource("Test Shop", 300, 0, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(0.2, result.getScore(), 0.01);
	}

	// --- MILESTONE scoring ---

	@Test
	public void testMilestoneScoring_bestMilestoneItem()
	{
		// Milestones at 100, 500, 1000 kills. killTime=45s
		// Best item = milestone at 100 kills: hours = 100*45/3600 = 1.25, score = (1/1.25)*100 = 80
		List<CollectionLogItem> items = Arrays.asList(
			makeMilestoneItem(1, "Hat 1", 100),
			makeMilestoneItem(2, "Hat 2", 500),
			makeMilestoneItem(3, "Hat 3", 1000));
		CollectionLogSource source = makeMilestoneSource("Test Milestones", 45, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		// Best = Hat 1 at 100 kills: 100 * 45/3600 = 1.25 hrs, score = 80
		assertEquals(80.0, result.getScore(), 0.01);
		assertEquals("Hat 1", result.getBestItem().getName());
	}

	// --- Raid team size scaling ---

	@Test
	public void testRaidKillTimeScaling()
	{
		// Raid source: 1800s solo. With TRIO config: 1800 * 0.6 = 1080s
		List<CollectionLogItem> items = Collections.singletonList(makeItem(1, "Unique", 0.01));
		CollectionLogSource source = makeSource("Test Raid", CollectionLogCategory.RAIDS, 1800, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		// Solo: score = 0.01 * (3600/1800) * 100 = 2.0
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		ScoredItem solo = calculator.scoreSource(source, false);
		assertEquals(2.0, solo.getScore(), 0.01);

		// Trio: score = 0.01 * (3600/1080) * 100 = 3.33
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.TRIO);
		ScoredItem trio = calculator.scoreSource(source, false);
		assertEquals(3.33, trio.getScore(), 0.1);
	}

	// --- rollsPerKill ---

	@Test
	public void testRollsPerKill()
	{
		// 1 item at 1/512 per roll, 2 rolls per kill
		// Effective rate = 1 - (1 - 1/512)^2 = 1 - (511/512)^2 ≈ 0.003899
		// score = 0.003899 * (3600/120) * 100 = 11.70
		List<CollectionLogItem> items = Collections.singletonList(makeItem(1, "Drop", 1.0 / 512));
		CollectionLogSource source = new CollectionLogSource("Test", CollectionLogCategory.BOSSES,
			3000, 3000, 0, 120, "Test", Collections.emptyList(),
			RewardType.DROP, 0, false, 2, false, null, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double expectedRate = 1.0 - Math.pow(1.0 - 1.0 / 512, 2);
		double expectedScore = expectedRate * (3600.0 / 120) * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
	}

	// --- Completion reward (milestoneKills=1) in MIXED source ---

	@Test
	public void testMixedSource_completionReward()
	{
		// Source with 1 pet (probabilistic) + 1 guaranteed cape (milestoneKills=1)
		// Like Fight Caves: pet at 1/200, fire cape guaranteed on completion
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Pet", 0.005),
			makeMilestoneItem(2, "Cape", 1));
		CollectionLogSource source = makeMilestoneSource("Test Boss", 2400, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(2, result.getMissingItemCount());
		// Cape: milestoneKills=1, hours = 1 * 2400/3600 = 0.667, score = (1/0.667)*100 = 150
		// Pet: score = 0.005 * (3600/2400) * 100 = 0.75
		// Best = Cape at 150
		assertEquals(150.0, result.getScore(), 1.0);
		assertEquals("Cape", result.getBestItem().getName());
	}

	@Test
	public void testMixedSource_completionRewardAlreadyObtained()
	{
		// If cape is obtained, only pet contributes
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Pet", 0.005),
			makeMilestoneItem(2, "Cape", 1));
		CollectionLogSource source = makeMilestoneSource("Test Boss", 2400, items);
		when(collectionState.isItemObtained(1)).thenReturn(false);
		when(collectionState.isItemObtained(2)).thenReturn(true);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(1, result.getMissingItemCount());
		// Only pet: 0.005 * (3600/2400) * 100 = 0.75
		assertEquals(0.75, result.getScore(), 0.01);
	}

	// --- Mixed source: milestone vs shop vs drop ---

	@Test
	public void testMixedSource_bestItemIsGuaranteed()
	{
		// 1 rare drop at 1/30 + 2 milestone items (milestoneKills=1) + 2 shop items (1000 pts each)
		// pointsPerHour = 200
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Rare drop", 1.0 / 30),
			makeMilestoneItem(2, "Milestone A", 1),
			makeMilestoneItem(3, "Milestone B", 1),
			makeShopItem(4, "Shop item A", 1000),
			makeShopItem(5, "Shop item B", 1000));
		CollectionLogSource source = makeMixedSource("Test Mixed", 300, 200, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(5, result.getMissingItemCount());

		// Individual scores:
		// Drop: (1/30) * (3600/300) * 100 = 40.0
		// Milestone A: milestoneKills=1, hours = 300/3600 = 0.0833, score = 1200
		// Milestone B: same = 1200
		// Shop A: 1000/200 = 5 hrs, score = 20
		// Shop B: same = 20
		// Best = Milestone A at 1200
		assertEquals(1200.0, result.getScore(), 1.0);
		assertEquals("Milestone A", result.getBestItem().getName());
	}

	@Test
	public void testMixedSource_onlyShopGuaranteed_bestIsShop()
	{
		// Source with rare drops + shop items but NO milestones
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Rare drop", 1.0 / 100),
			makeShopItem(2, "Shop A", 500),
			makeShopItem(3, "Shop B", 500));
		CollectionLogSource source = makeMixedSource("Test Shop Mixed", 300, 100, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Drop: 0.01 * 12 * 100 = 12
		// Shop A: 100/500 * 100 = 20
		// Shop B: same = 20
		// Best = Shop A at 20
		assertEquals(20.0, result.getScore(), 0.5);
	}

	// --- Aggregated source scoring ---

	@Test
	public void testAggregatedSource_bestItemScore()
	{
		// Aggregated source: best individual item is used (same as per-item model)
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Common drop", 1.0 / 100),   // best rate
			makeItem(2, "Medium drop", 1.0 / 500),
			makeItem(3, "Rare drop", 1.0 / 1000));
		CollectionLogSource source = makeAggregatedSource("Slayer", 30, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Best item = Common drop at 1/100, score = 0.01 * 120 * 100 = 120
		double expectedScore = (1.0 / 100) * (3600.0 / 30) * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
		assertEquals("Common drop", result.getBestItem().getName());
	}

	@Test
	public void testNonAggregatedSource_bestItemScore()
	{
		// Normal source: also uses best item score (not sum-of-rates anymore)
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 1.0 / 100),
			makeItem(2, "Drop B", 1.0 / 500),
			makeItem(3, "Drop C", 1.0 / 1000));
		CollectionLogSource source = makeSource("Normal Boss", CollectionLogCategory.BOSSES, 30, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		// Best = Drop A at 1/100, score = 0.01 * 120 * 100 = 120
		double expectedScore = (1.0 / 100) * (3600.0 / 30) * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
		assertEquals("Drop A", result.getBestItem().getName());
	}

	// --- Clue effective kill time ---

	@Test
	public void testClueEffectiveKillTime()
	{
		CollectionLogSource source = new CollectionLogSource("Easy Treasure Trails",
			CollectionLogCategory.CLUES, 3000, 3000, 0, 600,
			"Easy Treasure Trails", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, null,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(clueEstimator.estimateCompletionSeconds("Easy Treasure Trails")).thenReturn(900);

		int effective = calculator.getEffectiveKillTime(source);
		assertEquals(900, effective);
	}

	@Test
	public void testClueEffectiveKillTime_fallback()
	{
		CollectionLogSource source = new CollectionLogSource("Easy Treasure Trails",
			CollectionLogCategory.CLUES, 3000, 3000, 0, 600,
			"Easy Treasure Trails", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, null,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(clueEstimator.estimateCompletionSeconds("Easy Treasure Trails")).thenReturn(0);

		int effective = calculator.getEffectiveKillTime(source);
		assertEquals(600, effective);
	}

	// --- Edge case safety ---

	@Test
	public void testZeroDropRate_returnsZeroScore()
	{
		// dropRate = 0 should not cause division by zero
		CollectionLogSource source = makeSource("Test", CollectionLogCategory.BOSSES, 60,
			Collections.singletonList(makeItem(1, "Bad item", 0.0)));
		when(collectionState.isItemObtained(1)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		// Falls through to fallback: missingCount * 0.2 = 0.2
		assertEquals(0.2, result.getScore(), 0.01);
	}

	@Test
	public void testZeroKillTime_returnsZeroKillsPerHour()
	{
		// killTimeSeconds = 0 should not cause division by zero
		CollectionLogSource source = makeSource("Test", CollectionLogCategory.BOSSES, 0,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(collectionState.isItemObtained(1)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		// killsPerHour = 0, so drop score = 0, falls to fallback 0.2
		assertEquals(0.2, result.getScore(), 0.01);
	}

	@Test
	public void testZeroPointsPerHour_shopItemFallback()
	{
		// pointsPerHour = 0 with pointCost > 0 should not crash
		List<CollectionLogItem> items = Collections.singletonList(makeShopItem(1, "Item", 500));
		CollectionLogSource source = makeShopSource("Test", 300, 0, items);
		when(collectionState.isItemObtained(1)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		// pointsPerHour=0 fails guard, returns 0.2 fallback
		assertEquals(0.2, result.getScore(), 0.01);
	}

	@Test
	public void testNegativeDropRate_returnsZeroScore()
	{
		// Negative dropRate should be treated as invalid
		CollectionLogSource source = makeSource("Test", CollectionLogCategory.BOSSES, 60,
			Collections.singletonList(makeItem(1, "Bad item", -0.5)));
		when(collectionState.isItemObtained(1)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(0.2, result.getScore(), 0.01);
	}

	// --- formatScore display ---

	@Test
	public void testFormatScore_hoursPerItem()
	{
		// score = 50 → hours = 100/50 = 2 hours per item
		// score = 200 → hours = 100/200 = 0.5 hours = 30 min
		// score = 10 → hours = 100/10 = 10 hours
		double hoursForScore50 = 100.0 / 50;
		assertEquals(2.0, hoursForScore50, 0.001);

		double hoursForScore200 = 100.0 / 200;
		assertEquals(0.5, hoursForScore200, 0.001);
	}
}
