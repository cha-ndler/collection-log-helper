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
	@Mock
	private SlayerMasterDatabase slayerMasterDatabase;

	private EfficiencyCalculator calculator;

	@Before
	public void setUp() throws Exception
	{
		lenient().when(requirementsChecker.isAccessible(anyString())).thenReturn(true);
		lenient().when(config.hideLockedContent()).thenReturn(false);
		lenient().when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		lenient().when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		lenient().when(config.accountType()).thenReturn(AccountType.MAIN);

		// Use reflection to call the private constructor
		Constructor<EfficiencyCalculator> ctor = EfficiencyCalculator.class.getDeclaredConstructor(
			DropRateDatabase.class, PlayerCollectionState.class,
			RequirementsChecker.class, CollectionLogHelperConfig.class,
			ClueCompletionEstimator.class, SlayerTaskState.class,
			SlayerMasterDatabase.class);
		ctor.setAccessible(true);
		calculator = ctor.newInstance(database, collectionState, requirementsChecker,
			config, clueEstimator, slayerTaskState, slayerMasterDatabase);
	}

	private CollectionLogSource makeSource(String name, CollectionLogCategory category,
		int killTimeSeconds, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, category, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogSource makeShopSource(String name, int killTimeSeconds,
		double pointsPerHour, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.SHOP, pointsPerHour, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogSource makeMilestoneSource(String name, int killTimeSeconds,
		List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.MILESTONE, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogSource makeMixedSource(String name, int killTimeSeconds,
		double pointsPerHour, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.MIXED, pointsPerHour, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogSource makeAggregatedSource(String name, int killTimeSeconds,
		List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, 0, name, Collections.emptyList(),
			RewardType.DROP, 0, false, 1, true, 0, null, 0, null, null, null, null, 0, null, 0, items);
	}

	private CollectionLogItem makeItem(int id, String name, double dropRate)
	{
		return new CollectionLogItem(id, name, dropRate, 0, false, null, 0, 0, false, false);
	}

	private CollectionLogItem makeIndependentItem(int id, String name, double dropRate)
	{
		return new CollectionLogItem(id, name, dropRate, 0, false, null, 0, 0, false, true);
	}

	private CollectionLogItem makeItemRequiresPrevious(int id, String name, double dropRate)
	{
		return new CollectionLogItem(id, name, dropRate, 0, false, null, 0, 0, true, false);
	}

	private CollectionLogItem makeShopItem(int id, String name, int pointCost)
	{
		return new CollectionLogItem(id, name, 1.0, 0, false, null, pointCost, 0, false, false);
	}

	private CollectionLogItem makeMilestoneItem(int id, String name, int milestoneKills)
	{
		return new CollectionLogItem(id, name, 1.0, 0, false, null, 0, milestoneKills, false, false);
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
	public void testMultipleMissingItems_combinedRateScore()
	{
		// 3 items at 1/512 each, 120s kills = 30 kills/hr
		// Combined rate: 1 - (1 - 1/512)^3 ≈ 0.005848
		// Score = combinedRate * 30 * 100 = 17.54
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 1.0 / 512),
			makeItem(2, "Drop B", 1.0 / 512),
			makeItem(3, "Drop C", 1.0 / 512));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 120, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		double combinedRate = 1.0 - Math.pow(1.0 - 1.0 / 512, 3);
		double expectedScore = combinedRate * 30 * 100;
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
		// 5 items at 0.01 each, 60s kills = 60 kills/hr
		// Combined rate: 1 - (1-0.01)^5 = 0.04901
		// Score = 0.04901 * 60 * 100 = 294.06
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "A", 0.01), makeItem(2, "B", 0.01),
			makeItem(3, "C", 0.01), makeItem(4, "D", 0.01),
			makeItem(5, "E", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double combinedRate = 1.0 - Math.pow(1.0 - 0.01, 5);
		double expectedScore = combinedRate * 60 * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
		assertNotNull(result.getBestItem());
	}

	@Test
	public void testBestItemSelected_mixedRates()
	{
		// Items with different rates — best = highest drop rate
		// Combined rate: 1 - (1-0.001)*(1-0.1)*(1-0.01) = 0.10891
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Slow", 0.001),
			makeItem(2, "Fast", 0.1),
			makeItem(3, "Medium", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double combinedRate = 1.0 - (1 - 0.001) * (1 - 0.1) * (1 - 0.01);
		double expectedScore = combinedRate * 60 * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
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
		// Raid source: 1800s solo. Raids apply both kill time and drop rate multipliers.
		List<CollectionLogItem> items = Collections.singletonList(makeItem(1, "Unique", 0.01));
		CollectionLogSource source = makeSource("Test Raid", CollectionLogCategory.RAIDS, 1800, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		// Solo: dropRate=0.01*1.0=0.01, killTime=1800*1.0=1800s, kph=2
		// score = 0.01 * 2 * 100 = 2.0
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.SOLO);
		ScoredItem solo = calculator.scoreSource(source, false);
		assertEquals(2.0, solo.getScore(), 0.01);

		// Trio: dropRate=0.01*0.4=0.004, killTime=1800*0.6=1080s, kph=3.33
		// score = 0.004 * 3.33 * 100 = 1.33
		when(config.raidTeamSize()).thenReturn(RaidTeamSize.TRIO);
		ScoredItem trio = calculator.scoreSource(source, false);
		double trioRate = 0.01 * RaidTeamSize.TRIO.getDropRateMultiplier();
		double trioKph = 3600.0 / (1800 * RaidTeamSize.TRIO.getKillTimeMultiplier());
		assertEquals(trioRate * trioKph * 100, trio.getScore(), 0.01);
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
			3000, 3000, 0, 120, 0, "Test", Collections.emptyList(),
			RewardType.DROP, 0, false, 2, false, 0, null, 0, null, null, null, null, 0, null, 0, items);
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
	public void testNonAggregatedSource_combinedRateScore()
	{
		// Normal source: uses combined rate of all missing items
		// Combined rate: 1 - (1-1/100)*(1-1/500)*(1-1/1000) = 0.01298
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 1.0 / 100),
			makeItem(2, "Drop B", 1.0 / 500),
			makeItem(3, "Drop C", 1.0 / 1000));
		CollectionLogSource source = makeSource("Normal Boss", CollectionLogCategory.BOSSES, 30, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double combinedRate = 1.0 - (1.0 - 1.0 / 100) * (1.0 - 1.0 / 500) * (1.0 - 1.0 / 1000);
		double expectedScore = combinedRate * (3600.0 / 30) * 100;
		assertEquals(expectedScore, result.getScore(), 0.01);
		assertEquals("Drop A", result.getBestItem().getName());
	}

	// --- Clue effective kill time ---

	@Test
	public void testClueEffectiveKillTime()
	{
		CollectionLogSource source = new CollectionLogSource("Easy Treasure Trails",
			CollectionLogCategory.CLUES, 3000, 3000, 0, 600, 0,
			"Easy Treasure Trails", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(clueEstimator.estimateCompletionSeconds("Easy Treasure Trails")).thenReturn(900);

		int effective = calculator.getEffectiveKillTime(source);
		assertEquals(900, effective);
	}

	@Test
	public void testClueEffectiveKillTime_fallback()
	{
		CollectionLogSource source = new CollectionLogSource("Easy Treasure Trails",
			CollectionLogCategory.CLUES, 3000, 3000, 0, 600, 0,
			"Easy Treasure Trails", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
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

	// --- AFK filter ---

	@Test
	public void testAfkFilter_excludesLowAfkLevel()
	{
		// Source with afkLevel 0 should be excluded when filter is AFK+ (minLevel=2)
		CollectionLogSource source = new CollectionLogSource("Test Boss", CollectionLogCategory.BOSSES,
			3000, 3000, 0, 60, 0, "Test Boss", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		lenient().when(collectionState.isItemObtained(1)).thenReturn(false);
		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(config.afkFilter()).thenReturn(AfkFilter.AFK);

		List<ScoredItem> results = calculator.rankByEfficiency();
		assertTrue(results.isEmpty());
	}

	@Test
	public void testAfkFilter_includesHighAfkLevel()
	{
		// Source with afkLevel 2 should be included when filter is AFK+ (minLevel=2)
		CollectionLogSource source = new CollectionLogSource("AFK Source", CollectionLogCategory.BOSSES,
			3000, 3000, 0, 60, 0, "AFK Source", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 2, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(collectionState.isItemObtained(1)).thenReturn(false);
		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(config.afkFilter()).thenReturn(AfkFilter.AFK);

		List<ScoredItem> results = calculator.rankByEfficiency();
		assertEquals(1, results.size());
	}

	@Test
	public void testAfkFilter_disabledShowsAll()
	{
		// When filter is OFF, all sources show regardless of afkLevel
		CollectionLogSource source = new CollectionLogSource("Test Boss", CollectionLogCategory.BOSSES,
			3000, 3000, 0, 60, 0, "Test Boss", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(collectionState.isItemObtained(1)).thenReturn(false);
		when(database.getAllSources()).thenReturn(Collections.singletonList(source));
		when(config.afkFilter()).thenReturn(AfkFilter.OFF);

		List<ScoredItem> results = calculator.rankByEfficiency();
		assertEquals(1, results.size());
	}

	// --- Slayer task overhead ---

	@Test
	public void testSlayerTaskOverhead_inflatesKillTime()
	{
		// "Abyssal Demon" is task-only; with P=0.05 and base kill time 20s,
		// effective kill time should be 20/0.05 = 400s
		CollectionLogSource source = new CollectionLogSource("Abyssal Demon", CollectionLogCategory.OTHER,
			3000, 3000, 0, 20, 0, "Abyssal Demon", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Whip", 0.001953)));
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Collections.singletonList("Duradel"));
		when(slayerMasterDatabase.getTaskProbability("Duradel", "abyssal demons")).thenReturn(0.05);

		int effectiveTime = calculator.getEffectiveKillTime(source);
		assertEquals(400, effectiveTime);
	}

	@Test
	public void testSlayerTaskOverhead_noOverheadWhenOnTask()
	{
		// If currently on abyssal demon task, no overhead applied
		CollectionLogSource source = new CollectionLogSource("Abyssal Demon", CollectionLogCategory.OTHER,
			3000, 3000, 0, 20, 0, "Abyssal Demon", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Whip", 0.001953)));
		when(slayerTaskState.isTaskActive()).thenReturn(true);
		when(slayerTaskState.getCreatureName()).thenReturn("Abyssal demons");

		int effectiveTime = calculator.getEffectiveKillTime(source);
		assertEquals(20, effectiveTime);
	}

	@Test
	public void testSlayerTaskOverhead_noOverheadForBossVariant()
	{
		// "Abyssal Sire" is NOT task-only (it's a boss), so no overhead
		CollectionLogSource source = new CollectionLogSource("Abyssal Sire", CollectionLogCategory.BOSSES,
			3000, 3000, 0, 180, 0, "Abyssal Sire", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Unsired", 0.0078)));

		int effectiveTime = calculator.getEffectiveKillTime(source);
		assertEquals(180, effectiveTime);
	}

	@Test
	public void testSlayerTaskOverhead_usesBestMaster()
	{
		// Two masters: Duradel P=0.06, Nieve P=0.04 — should use Duradel (higher P = lower overhead)
		CollectionLogSource source = new CollectionLogSource("Abyssal Demon", CollectionLogCategory.OTHER,
			3000, 3000, 0, 12, 0, "Abyssal Demon", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.singletonList(makeItem(1, "Whip", 0.001953)));
		when(slayerMasterDatabase.getMasterNames()).thenReturn(Arrays.asList("Duradel", "Nieve"));
		when(slayerMasterDatabase.getTaskProbability("Duradel", "abyssal demons")).thenReturn(0.06);
		when(slayerMasterDatabase.getTaskProbability("Nieve", "abyssal demons")).thenReturn(0.04);

		int effectiveTime = calculator.getEffectiveKillTime(source);
		// 12 / 0.06 = 200
		assertEquals(200, effectiveTime);
	}

	// --- Sequential drop dependency (requiresPrevious) ---

	@Test
	public void testRequiresPrevious_predecessorNotObtained_excludedFromScore()
	{
		// 3 sequential items: A (base), B (requires A), C (requires B)
		// If none are obtained, B can't drop without A and C can't drop without B.
		// Only A should contribute to scoring.
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Part A", 0.01),
			makeItemRequiresPrevious(2, "Part B", 0.01),
			makeItemRequiresPrevious(3, "Part C", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(1)).thenReturn(false);
		when(collectionState.isItemObtained(2)).thenReturn(false);
		when(collectionState.isItemObtained(3)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(3, result.getMissingItemCount());
		// Only Part A contributes to combined rate: 0.01 * 60 * 100 = 60
		assertEquals(60.0, result.getScore(), 0.01);
	}

	@Test
	public void testRequiresPrevious_predecessorObtained_includedInScore()
	{
		// If A is obtained, B can now drop (its predecessor is satisfied).
		// C still can't drop (B not obtained). Only B contributes.
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Part A", 0.01),
			makeItemRequiresPrevious(2, "Part B", 0.01),
			makeItemRequiresPrevious(3, "Part C", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(1)).thenReturn(true);
		when(collectionState.isItemObtained(2)).thenReturn(false);
		when(collectionState.isItemObtained(3)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(2, result.getMissingItemCount());
		// Only Part B contributes: 0.01 * 60 * 100 = 60
		assertEquals(60.0, result.getScore(), 0.01);
	}

	@Test
	public void testRequiresPrevious_allPredecessorsObtained_allContribute()
	{
		// If A and B are obtained, only C is missing and its predecessor (B) is obtained,
		// so C contributes normally.
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Part A", 0.01),
			makeItemRequiresPrevious(2, "Part B", 0.01),
			makeItemRequiresPrevious(3, "Part C", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(1)).thenReturn(true);
		when(collectionState.isItemObtained(2)).thenReturn(true);
		when(collectionState.isItemObtained(3)).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(1, result.getMissingItemCount());
		// Part C: 0.01 * 60 * 100 = 60
		assertEquals(60.0, result.getScore(), 0.01);
	}

	// --- Independent drop scoring ---

	@Test
	public void testIndependentItems_combinedRate()
	{
		// 2 independent items at 1/100 each, 60s kills = 60 kills/hr
		// Independent combined = 1 - (1-0.01)*(1-0.01) = 0.0199
		// Overall = 1 - (1-0)*(1-0.0199) = 0.0199
		// Score = 0.0199 * 60 * 100 = 119.4
		List<CollectionLogItem> items = Arrays.asList(
			makeIndependentItem(1, "Indep A", 0.01),
			makeIndependentItem(2, "Indep B", 0.01));
		CollectionLogSource source = makeSource("Indep Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double expectedIndepCombined = 1.0 - Math.pow(1.0 - 0.01, 2);
		double expectedScore = expectedIndepCombined * 60 * 100;
		assertEquals(expectedScore, result.getScore(), 0.1);
	}

	@Test
	public void testMixedIndependentAndStandard_combinedCorrectly()
	{
		// 1 standard item at 1/50 + 1 independent item (pet) at 1/3000
		// Standard combined = 0.02
		// Independent combined = 1/3000 = 0.000333
		// Overall = 1 - (1-0.02)*(1-0.000333) = 1 - 0.98*0.999667 = 0.020327
		// Score = 0.020327 * 60 * 100 = 121.96
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Main drop", 0.02),
			makeIndependentItem(2, "Pet", 1.0 / 3000));
		CollectionLogSource source = makeSource("Mixed Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		double standardCombined = 0.02;
		double independentCombined = 1.0 / 3000;
		double overallCombined = 1.0 - (1.0 - standardCombined) * (1.0 - independentCombined);
		double expectedScore = overallCombined * 60 * 100;
		assertEquals(expectedScore, result.getScore(), 0.1);
	}
}
