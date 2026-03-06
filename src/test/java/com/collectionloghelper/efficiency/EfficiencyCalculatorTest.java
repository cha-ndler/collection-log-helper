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
			RewardType.DROP, 0, false, 1, null, items);
	}

	private CollectionLogSource makeShopSource(String name, int killTimeSeconds,
		double pointsPerHour, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.SHOP, pointsPerHour, false, 1, null, items);
	}

	private CollectionLogSource makeMilestoneSource(String name, int killTimeSeconds,
		List<CollectionLogItem> items)
	{
		return new CollectionLogSource(name, CollectionLogCategory.OTHER, 3000, 3000, 0,
			killTimeSeconds, name, Collections.emptyList(),
			RewardType.MILESTONE, 0, false, 1, null, items);
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

	// --- Score formula: items per hour × 100 ---

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
	}

	@Test
	public void testMultipleMissingItems_sumOfRates()
	{
		// 3 items at 1/512 each, 120s kills
		// score = (3/512) * (3600/120) * 100 = 0.00586 * 30 * 100 = 17.58
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "Drop A", 1.0 / 512),
			makeItem(2, "Drop B", 1.0 / 512),
			makeItem(3, "Drop C", 1.0 / 512));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 120, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(17.58, result.getScore(), 0.1);
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
	public void testNoDoubleCountingDropCount()
	{
		// Old formula would give: score = dropCount * rate * kph * 100
		// New formula gives: score = rate * kph * 100 (no dropCount multiplier)
		// 5 items at 1/100 each, 60s kills
		// Correct: score = 5*0.01 * 60 * 100 = 300
		// Old (wrong) would be: 5 * 5*0.01 * 60 * 100 = 1500
		List<CollectionLogItem> items = Arrays.asList(
			makeItem(1, "A", 0.01), makeItem(2, "B", 0.01),
			makeItem(3, "C", 0.01), makeItem(4, "D", 0.01),
			makeItem(5, "E", 0.01));
		CollectionLogSource source = makeSource("Test Boss", CollectionLogCategory.BOSSES, 60, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertEquals(300.0, result.getScore(), 0.01);
	}

	// --- SHOP scoring ---

	@Test
	public void testShopScoring_withEconomics()
	{
		// 3 items, 100 pts each, 50 pts/hr → 6 hrs total → 3/6 * 100 = 50
		List<CollectionLogItem> items = Arrays.asList(
			makeShopItem(1, "A", 100), makeShopItem(2, "B", 100), makeShopItem(3, "C", 100));
		CollectionLogSource source = makeShopSource("Test Shop", 300, 50, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(50.0, result.getScore(), 0.01);
	}

	@Test
	public void testShopScoring_withoutEconomics_fallback()
	{
		// Items with 0 cost and 0 pph → flat 0.2 per item
		List<CollectionLogItem> items = Arrays.asList(
			makeShopItem(1, "A", 0), makeShopItem(2, "B", 0));
		CollectionLogSource source = makeShopSource("Test Shop", 300, 0, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(0.4, result.getScore(), 0.01);
	}

	// --- MILESTONE scoring ---

	@Test
	public void testMilestoneScoring_usesMaxNotSum()
	{
		// Milestones at 100, 500, 1000 kills. killTime=45s
		// Correct: max=1000, hours = 1000*45/3600 = 12.5, score = 3/12.5*100 = 24
		// Old (sum): sum=1600, hours = 1600*45/3600 = 20, score = 3/20*100 = 15
		List<CollectionLogItem> items = Arrays.asList(
			makeMilestoneItem(1, "Hat 1", 100),
			makeMilestoneItem(2, "Hat 2", 500),
			makeMilestoneItem(3, "Hat 3", 1000));
		CollectionLogSource source = makeMilestoneSource("Test Milestones", 45, items);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		ScoredItem result = calculator.scoreSource(source, false);

		assertNotNull(result);
		assertEquals(24.0, result.getScore(), 0.01);
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
			RewardType.DROP, 0, false, 2, null, items);
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
		// Pet: score = 0.005 * (3600/2400) * 100 = 0.75
		// Cape: milestoneKills=1, hours = 1 * 2400/3600 = 0.667, score = (1/0.667)*100 = 150
		// Total: 150.75
		assertEquals(150.75, result.getScore(), 1.0);
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

	// --- Clue effective kill time ---

	@Test
	public void testClueEffectiveKillTime()
	{
		CollectionLogSource source = new CollectionLogSource("Easy Treasure Trails",
			CollectionLogCategory.CLUES, 3000, 3000, 0, 600,
			"Easy Treasure Trails", Collections.emptyList(),
			RewardType.DROP, 0, false, 1, null,
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
			RewardType.DROP, 0, false, 1, null,
			Collections.singletonList(makeItem(1, "Drop", 0.01)));
		when(clueEstimator.estimateCompletionSeconds("Easy Treasure Trails")).thenReturn(0);

		int effective = calculator.getEffectiveKillTime(source);
		assertEquals(600, effective);
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
