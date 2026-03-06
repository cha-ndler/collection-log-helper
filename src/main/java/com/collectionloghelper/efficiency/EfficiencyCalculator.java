package com.collectionloghelper.efficiency;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SlayerCreatureDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EfficiencyCalculator
{
	/**
	 * Score multiplier applied to sources that match the player's current Slayer task.
	 * The player is already committed to killing these NPCs, so drops from task-relevant
	 * bosses are effectively "free" efficiency.
	 */
	static final double SLAYER_TASK_BOOST = 1.5;

	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final RequirementsChecker requirementsChecker;
	private final CollectionLogHelperConfig config;
	private final ClueCompletionEstimator clueEstimator;
	private final SlayerTaskState slayerTaskState;

	@Inject
	private EfficiencyCalculator(DropRateDatabase database, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker, CollectionLogHelperConfig config,
		ClueCompletionEstimator clueEstimator, SlayerTaskState slayerTaskState)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.requirementsChecker = requirementsChecker;
		this.config = config;
		this.clueEstimator = clueEstimator;
		this.slayerTaskState = slayerTaskState;
	}

	public List<ScoredItem> rankByEfficiency()
	{
		List<ScoredItem> results = new ArrayList<>();
		boolean hideLocked = config.hideLockedContent();

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			ScoredItem scored = applySlayerBoost(scoreSource(source, locked));
			if (scored != null)
			{
				results.add(scored);
			}
		}

		// Unlocked first (by score desc), then locked (by score desc)
		results.sort(Comparator
			.comparing(ScoredItem::isLocked)
			.thenComparing(Comparator.comparingDouble(ScoredItem::getScore).reversed()));
		return results;
	}

	public List<ScoredItem> filterByCategory(CollectionLogCategory category)
	{
		return rankByEfficiency().stream()
			.filter(s -> s.getSource().getCategory() == category)
			.collect(Collectors.toList());
	}

	public List<ScoredItem> filterPetsOnly()
	{
		List<ScoredItem> results = new ArrayList<>();
		boolean hideLocked = config.hideLockedContent();

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			ScoredItem scored = applySlayerBoost(scoreSourcePetsOnly(source, locked));
			if (scored != null)
			{
				results.add(scored);
			}
		}

		results.sort(Comparator
			.comparing(ScoredItem::isLocked)
			.thenComparing(Comparator.comparingDouble(ScoredItem::getScore).reversed()));
		return results;
	}

	/**
	 * Returns true if the given source is boosted by the player's current Slayer task.
	 */
	public boolean isOnSlayerTask(CollectionLogSource source)
	{
		return slayerTaskState.isTaskActive()
			&& SlayerCreatureDatabase.isSourceOnTask(slayerTaskState.getCreatureName(), source.getName());
	}

	private ScoredItem applySlayerBoost(ScoredItem item)
	{
		if (item == null || !isOnSlayerTask(item.getSource()))
		{
			return item;
		}
		String boostedReasoning = item.getReasoning() + " [Slayer task x" + SLAYER_TASK_BOOST + "]";
		return new ScoredItem(item.getSource(), item.getScore() * SLAYER_TASK_BOOST,
			item.getMissingItemCount(), boostedReasoning, item.isLocked());
	}

	public ScoredItem scoreSource(CollectionLogSource source, boolean locked)
	{
		double dropDropRate = 0;
		double maxDropRate = 0;
		int missingCount = 0;
		int guaranteedCount = 0;
		double totalPointCost = 0;
		double totalMilestoneKills = 0;
		int rolls = source.getRollsPerKill();

		for (CollectionLogItem item : source.getItems())
		{
			if (!collectionState.isItemObtained(item.getItemId()))
			{
				missingCount++;
				if (item.getDropRate() >= 1.0)
				{
					guaranteedCount++;
					totalPointCost += item.getPointCost();
					totalMilestoneKills += item.getMilestoneKills();
				}
				else
				{
					double effectiveRate = rolls > 1
						? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
						: item.getDropRate();
					dropDropRate += effectiveRate;
					maxDropRate = Math.max(maxDropRate, effectiveRate);
				}
			}
		}

		if (missingCount == 0)
		{
			return null;
		}

		RewardType rewardType = source.getRewardType();

		// SHOP sources: all items bought with points/currency
		if (rewardType == RewardType.SHOP && guaranteedCount == missingCount)
		{
			double score;
			String reasoning;
			if (source.getPointsPerHour() > 0 && totalPointCost > 0)
			{
				double hoursNeeded = totalPointCost / source.getPointsPerHour();
				score = (missingCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing shop items, ~%.1f hours of points needed", missingCount, hoursNeeded);
			}
			else
			{
				score = missingCount * 0.2;
				reasoning = String.format("%d missing shop items", missingCount);
			}
			return new ScoredItem(source, score, missingCount, reasoning, locked);
		}

		// MILESTONE sources: kill-count threshold unlocks
		if (rewardType == RewardType.MILESTONE && guaranteedCount == missingCount)
		{
			double score;
			String reasoning;
			if (totalMilestoneKills > 0 && source.getKillTimeSeconds() > 0)
			{
				double hoursNeeded = totalMilestoneKills * (source.getKillTimeSeconds() / 3600.0);
				score = (missingCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing milestone items, ~%.0f kills needed", missingCount, totalMilestoneKills);
			}
			else
			{
				score = missingCount * 0.3;
				reasoning = String.format("%d missing milestone items", missingCount);
			}
			return new ScoredItem(source, score, missingCount, reasoning, locked);
		}

		// All items are guaranteed but not tagged as SHOP/MILESTONE — flat score
		if (guaranteedCount == missingCount)
		{
			String reasoning = String.format("%d missing items (reward shop)", missingCount);
			return new ScoredItem(source, missingCount * 0.2, missingCount, reasoning, locked);
		}

		// No probabilistic drops to score
		if (dropDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingCount - guaranteedCount;
		double effectiveDropRate = source.isMutuallyExclusive() ? maxDropRate : dropDropRate;
		double expectedKills = 1.0 / effectiveDropRate;

		// Use account-progression-aware clue completion time for CLUES sources
		int killTimeSeconds = getEffectiveKillTime(source);
		double expectedHours = expectedKills * (killTimeSeconds / 3600.0);
		double dropScore = (dropCount / expectedHours) * 100.0;

		// MIXED sources: combine drop score with guaranteed item score
		double score;
		String reasoning;
		if (rewardType == RewardType.MIXED && guaranteedCount > 0)
		{
			double guaranteedScore = guaranteedCount * 0.2;
			score = dropScore + guaranteedScore;
			reasoning = String.format("%d missing drops + %d shop items, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else if (guaranteedCount > 0)
		{
			score = dropScore;
			reasoning = String.format("%d missing drops + %d shop items, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			score = dropScore;
			reasoning = String.format("%d missing items, ~%.0f kills expected", missingCount, expectedKills);
		}

		return new ScoredItem(source, score, missingCount, reasoning, locked);
	}

	/**
	 * Returns the effective kill/completion time for a source. For CLUES sources,
	 * uses the account-progression-aware estimate from {@link ClueCompletionEstimator}.
	 * For all other sources, returns the fixed killTimeSeconds from the database.
	 */
	int getEffectiveKillTime(CollectionLogSource source)
	{
		if (source.getCategory() == CollectionLogCategory.CLUES)
		{
			int estimated = clueEstimator.estimateCompletionSeconds(source.getName());
			if (estimated > 0)
			{
				return estimated;
			}
		}
		return source.getKillTimeSeconds();
	}

	private ScoredItem scoreSourcePetsOnly(CollectionLogSource source, boolean locked)
	{
		double dropDropRate = 0;
		double maxDropRate = 0;
		int missingPetCount = 0;
		int guaranteedCount = 0;
		double totalPointCost = 0;
		double totalMilestoneKills = 0;
		int rolls = source.getRollsPerKill();

		for (CollectionLogItem item : source.getItems())
		{
			if (item.isPet() && !collectionState.isItemObtained(item.getItemId()))
			{
				missingPetCount++;
				if (item.getDropRate() >= 1.0)
				{
					guaranteedCount++;
					totalPointCost += item.getPointCost();
					totalMilestoneKills += item.getMilestoneKills();
				}
				else
				{
					double effectiveRate = rolls > 1
						? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
						: item.getDropRate();
					dropDropRate += effectiveRate;
					maxDropRate = Math.max(maxDropRate, effectiveRate);
				}
			}
		}

		if (missingPetCount == 0)
		{
			return null;
		}

		RewardType rewardType = source.getRewardType();

		// SHOP sources: pets bought with points
		if (rewardType == RewardType.SHOP && guaranteedCount == missingPetCount)
		{
			double score;
			String reasoning;
			if (source.getPointsPerHour() > 0 && totalPointCost > 0)
			{
				double hoursNeeded = totalPointCost / source.getPointsPerHour();
				score = (missingPetCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing shop pets, ~%.1f hours of points needed", missingPetCount, hoursNeeded);
			}
			else
			{
				score = missingPetCount * 0.2;
				reasoning = String.format("%d missing shop pets", missingPetCount);
			}
			return new ScoredItem(source, score, missingPetCount, reasoning, locked);
		}

		// MILESTONE sources
		if (rewardType == RewardType.MILESTONE && guaranteedCount == missingPetCount)
		{
			double score;
			String reasoning;
			if (totalMilestoneKills > 0 && source.getKillTimeSeconds() > 0)
			{
				double hoursNeeded = totalMilestoneKills * (source.getKillTimeSeconds() / 3600.0);
				score = (missingPetCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing milestone pets, ~%.0f kills needed", missingPetCount, totalMilestoneKills);
			}
			else
			{
				score = missingPetCount * 0.3;
				reasoning = String.format("%d missing milestone pets", missingPetCount);
			}
			return new ScoredItem(source, score, missingPetCount, reasoning, locked);
		}

		// All guaranteed pets but not SHOP/MILESTONE tagged
		if (guaranteedCount == missingPetCount)
		{
			String reasoning = String.format("%d missing pets (reward shop)", missingPetCount);
			return new ScoredItem(source, missingPetCount * 0.2, missingPetCount, reasoning, locked);
		}

		if (dropDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingPetCount - guaranteedCount;
		double effectiveDropRate = source.isMutuallyExclusive() ? maxDropRate : dropDropRate;
		double expectedKills = 1.0 / effectiveDropRate;
		int killTimeSeconds = getEffectiveKillTime(source);
		double expectedHours = expectedKills * (killTimeSeconds / 3600.0);
		double dropScore = (dropCount / expectedHours) * 100.0;

		// MIXED sources: combine drop score with guaranteed pet score
		double score;
		String reasoning;
		if (rewardType == RewardType.MIXED && guaranteedCount > 0)
		{
			double guaranteedScore = guaranteedCount * 0.2;
			score = dropScore + guaranteedScore;
			reasoning = String.format("%d missing pet drops + %d shop, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else if (guaranteedCount > 0)
		{
			score = dropScore;
			reasoning = String.format("%d missing pet drops + %d shop, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			score = dropScore;
			reasoning = String.format("%d missing pets, ~%.0f kills expected", missingPetCount, expectedKills);
		}

		return new ScoredItem(source, score, missingPetCount, reasoning, locked);
	}
}
