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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
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
			item.getMissingItemCount(), boostedReasoning, item.isLocked(),
			item.getDropOnlyScore() * SLAYER_TASK_BOOST);
	}

	public ScoredItem scoreSource(CollectionLogSource source, boolean locked)
	{
		double combinedDropRate = 0;
		int missingCount = 0;
		int guaranteedCount = 0;
		double totalPointCost = 0;
		double maxMilestoneKills = 0;
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
					maxMilestoneKills = Math.max(maxMilestoneKills, item.getMilestoneKills());
				}
				else
				{
					double effectiveRate = rolls > 1
						? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
						: item.getDropRate();
					combinedDropRate += effectiveRate;
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
				reasoning = String.format("%d missing shop items, ~%s of points needed", missingCount, formatHours(hoursNeeded));
			}
			else
			{
				score = missingCount * 0.2;
				reasoning = String.format("%d missing shop items", missingCount);
			}
			return new ScoredItem(source, score, missingCount, reasoning, locked, score);
		}

		// MILESTONE sources: kill-count threshold unlocks
		if (rewardType == RewardType.MILESTONE && guaranteedCount == missingCount)
		{
			double score;
			String reasoning;
			if (maxMilestoneKills > 0 && source.getKillTimeSeconds() > 0)
			{
				double hoursNeeded = maxMilestoneKills * (source.getKillTimeSeconds() / 3600.0);
				score = (missingCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing milestone items, ~%.0f kills needed", missingCount, maxMilestoneKills);
			}
			else
			{
				score = missingCount * 0.3;
				reasoning = String.format("%d missing milestone items", missingCount);
			}
			return new ScoredItem(source, score, missingCount, reasoning, locked, score);
		}

		// All items are guaranteed but not tagged as SHOP/MILESTONE — flat score
		if (guaranteedCount == missingCount)
		{
			String reasoning = String.format("%d missing items (reward shop)", missingCount);
			return new ScoredItem(source, missingCount * 0.2, missingCount, reasoning, locked, missingCount * 0.2);
		}

		// No probabilistic drops to score
		if (combinedDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingCount - guaranteedCount;

		// Score = expected new items per hour × 100.
		// By linearity of expectation, E[new items per kill] = sum of individual rates,
		// regardless of whether drops are mutually exclusive or independent.
		int killTimeSeconds = getEffectiveKillTime(source);
		double killsPerHour = 3600.0 / killTimeSeconds;
		double newItemsPerHour = combinedDropRate * killsPerHour;
		double dropScore = newItemsPerHour * 100.0;
		double expectedKills = 1.0 / combinedDropRate;

		// Sources with both guaranteed and probabilistic items: combine scores
		double score;
		String reasoning;
		if (guaranteedCount > 0)
		{
			double guaranteedScore;
			if (maxMilestoneKills > 0 && killTimeSeconds > 0)
			{
				double hoursNeeded = maxMilestoneKills * (killTimeSeconds / 3600.0);
				guaranteedScore = (guaranteedCount / hoursNeeded) * 100.0;
			}
			else if (source.getPointsPerHour() > 0 && totalPointCost > 0)
			{
				double hoursNeeded = totalPointCost / source.getPointsPerHour();
				guaranteedScore = (guaranteedCount / hoursNeeded) * 100.0;
			}
			else
			{
				guaranteedScore = guaranteedCount * 0.2;
			}
			score = dropScore + guaranteedScore;
			reasoning = String.format("%d missing drops + %d guaranteed, ~%.0f kills to next drop",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			score = dropScore;
			reasoning = String.format("%d missing items, ~%.0f kills to next drop", dropCount, expectedKills);
		}

		return new ScoredItem(source, score, missingCount, reasoning, locked, dropScore);
	}

	/**
	 * Returns the effective kill/completion time for a source. Adjusts for:
	 * - CLUES: account-progression-aware estimate from {@link ClueCompletionEstimator}
	 * - RAIDS: team size multiplier from config
	 * - All others: fixed killTimeSeconds from the database
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
		int baseTime = source.getKillTimeSeconds();
		if (source.getCategory() == CollectionLogCategory.RAIDS)
		{
			double multiplier = config.raidTeamSize().getKillTimeMultiplier();
			return Math.max(1, (int) (baseTime * multiplier));
		}
		return baseTime;
	}

	private static String formatHours(double hours)
	{
		if (hours < 1.0 / 60.0)
		{
			return "< 1 min";
		}
		else if (hours < 1)
		{
			long min = Math.max(1, Math.round(hours * 60));
			return min + " min";
		}
		else
		{
			return String.format("%.1f hours", hours);
		}
	}

	private ScoredItem scoreSourcePetsOnly(CollectionLogSource source, boolean locked)
	{
		double combinedDropRate = 0;
		int missingPetCount = 0;
		int guaranteedCount = 0;
		double totalPointCost = 0;
		double maxMilestoneKills = 0;
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
					maxMilestoneKills = Math.max(maxMilestoneKills, item.getMilestoneKills());
				}
				else
				{
					double effectiveRate = rolls > 1
						? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
						: item.getDropRate();
					combinedDropRate += effectiveRate;
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
				reasoning = String.format("%d missing shop pets, ~%s of points needed", missingPetCount, formatHours(hoursNeeded));
			}
			else
			{
				score = missingPetCount * 0.2;
				reasoning = String.format("%d missing shop pets", missingPetCount);
			}
			return new ScoredItem(source, score, missingPetCount, reasoning, locked, score);
		}

		// MILESTONE sources
		if (rewardType == RewardType.MILESTONE && guaranteedCount == missingPetCount)
		{
			double score;
			String reasoning;
			if (maxMilestoneKills > 0 && source.getKillTimeSeconds() > 0)
			{
				double hoursNeeded = maxMilestoneKills * (source.getKillTimeSeconds() / 3600.0);
				score = (missingPetCount / hoursNeeded) * 100.0;
				reasoning = String.format("%d missing milestone pets, ~%.0f kills needed", missingPetCount, maxMilestoneKills);
			}
			else
			{
				score = missingPetCount * 0.3;
				reasoning = String.format("%d missing milestone pets", missingPetCount);
			}
			return new ScoredItem(source, score, missingPetCount, reasoning, locked, score);
		}

		// All guaranteed pets but not SHOP/MILESTONE tagged
		if (guaranteedCount == missingPetCount)
		{
			String reasoning = String.format("%d missing pets (reward shop)", missingPetCount);
			return new ScoredItem(source, missingPetCount * 0.2, missingPetCount, reasoning, locked, missingPetCount * 0.2);
		}

		if (combinedDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingPetCount - guaranteedCount;
		int killTimeSeconds = getEffectiveKillTime(source);
		double killsPerHour = 3600.0 / killTimeSeconds;
		double newItemsPerHour = combinedDropRate * killsPerHour;
		double dropScore = newItemsPerHour * 100.0;
		double expectedKills = 1.0 / combinedDropRate;

		// Sources with both guaranteed and probabilistic pet items: combine scores
		double score;
		String reasoning;
		if (guaranteedCount > 0)
		{
			double guaranteedScore;
			int petKillTime = getEffectiveKillTime(source);
			if (maxMilestoneKills > 0 && petKillTime > 0)
			{
				double hoursNeeded = maxMilestoneKills * (petKillTime / 3600.0);
				guaranteedScore = (guaranteedCount / hoursNeeded) * 100.0;
			}
			else if (source.getPointsPerHour() > 0 && totalPointCost > 0)
			{
				double hoursNeeded = totalPointCost / source.getPointsPerHour();
				guaranteedScore = (guaranteedCount / hoursNeeded) * 100.0;
			}
			else
			{
				guaranteedScore = guaranteedCount * 0.2;
			}
			score = dropScore + guaranteedScore;
			reasoning = String.format("%d missing pet drops + %d guaranteed, ~%.0f kills to next drop",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			score = dropScore;
			reasoning = String.format("%d missing pets, ~%.0f kills to next drop", missingPetCount, expectedKills);
		}

		return new ScoredItem(source, score, missingPetCount, reasoning, locked, dropScore);
	}

	/**
	 * Exports the full ranked efficiency list to a text file for debugging.
	 * Each source shows its score, reasoning, and per-item breakdown.
	 */
	public void exportEfficiencyList(File outputFile)
	{
		List<ScoredItem> scored = rankByEfficiency();
		int totalObtained = 0;
		int totalItems = 0;
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				totalItems++;
				if (collectionState.isItemObtained(item.getItemId()))
				{
					totalObtained++;
				}
			}
		}

		try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile)))
		{
			pw.printf("=== Efficiency Export ===%n");
			pw.printf("Collection Log: %d/%d (%.1f%%)%n", totalObtained, totalItems,
				totalItems > 0 ? 100.0 * totalObtained / totalItems : 0);
			pw.printf("Total sources with missing items: %d%n%n", scored.size());

			int rank = 0;
			for (ScoredItem si : scored)
			{
				rank++;
				CollectionLogSource src = si.getSource();
				boolean onTask = isOnSlayerTask(src);

				pw.printf("--- #%d: %s ---%n", rank, src.getName());
				pw.printf("  Category: %s | RewardType: %s | Locked: %s%s%n",
					src.getCategory(), src.getRewardType(), si.isLocked(),
					onTask ? " | ON SLAYER TASK" : "");
				pw.printf("  killTimeSeconds: %d (effective: %d) | rollsPerKill: %d%n",
					src.getKillTimeSeconds(), getEffectiveKillTime(src), src.getRollsPerKill());
				if (src.getPointsPerHour() > 0)
				{
					pw.printf("  pointsPerHour: %.0f%n", src.getPointsPerHour());
				}
				pw.printf("  Score: %.2f | DropOnlyScore: %.2f | Missing: %d%n",
					si.getScore(), si.getDropOnlyScore(), si.getMissingItemCount());
				pw.printf("  DisplayTime: %s%n", formatExportTime(si.getScore()));
				pw.printf("  Reasoning: %s%n", si.getReasoning());
				pw.println("  Items:");
				for (CollectionLogItem item : src.getItems())
				{
					boolean obtained = collectionState.isItemObtained(item.getItemId());
					String status = obtained ? "[OBTAINED]" : "[MISSING] ";
					String rateStr;
					if (item.getDropRate() >= 1.0)
					{
						rateStr = "Guaranteed";
						if (item.getPointCost() > 0)
						{
							rateStr += String.format(" (cost: %d pts)", item.getPointCost());
						}
						if (item.getMilestoneKills() > 0)
						{
							rateStr += String.format(" (milestone: %d kills)", item.getMilestoneKills());
						}
					}
					else
					{
						long denom = Math.round(1.0 / item.getDropRate());
						rateStr = "1/" + denom;
					}
					pw.printf("    %s %-40s %s (id: %d)%n", status, item.getName(), rateStr, item.getItemId());
				}
				pw.println();
			}
			log.info("Exported efficiency list to {}", outputFile.getAbsolutePath());
		}
		catch (IOException e)
		{
			log.error("Failed to export efficiency list", e);
		}
	}

	private static String formatExportTime(double score)
	{
		if (score <= 0)
		{
			return "N/A";
		}
		double hours = 100.0 / score;
		if (hours < 1.0 / 60.0)
		{
			return "< 1 min";
		}
		else if (hours < 1)
		{
			long min = Math.max(1, Math.round(hours * 60));
			return "~" + min + " min";
		}
		else if (hours < 24)
		{
			return String.format("~%.1f hrs", hours);
		}
		else
		{
			return String.format("~%.1f days", hours / 24.0);
		}
	}
}
