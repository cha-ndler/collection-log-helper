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
import com.collectionloghelper.data.SlayerMasterDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Skill;

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
	private final SlayerMasterDatabase slayerMasterDatabase;

	@Inject
	private EfficiencyCalculator(DropRateDatabase database, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker, CollectionLogHelperConfig config,
		ClueCompletionEstimator clueEstimator, SlayerTaskState slayerTaskState,
		SlayerMasterDatabase slayerMasterDatabase)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.requirementsChecker = requirementsChecker;
		this.config = config;
		this.clueEstimator = clueEstimator;
		this.slayerTaskState = slayerTaskState;
		this.slayerMasterDatabase = slayerMasterDatabase;
	}

	public List<ScoredItem> rankByEfficiency()
	{
		List<ScoredItem> results = new ArrayList<>();
		boolean hideLocked = config.hideLockedContent();
		int afkMinLevel = config.afkFilter().getMinAfkLevel();

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}
			if (afkMinLevel > 0 && source.getAfkLevel() < afkMinLevel)
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
		int afkMinLevel = config.afkFilter().getMinAfkLevel();

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}
			if (afkMinLevel > 0 && source.getAfkLevel() < afkMinLevel)
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
			item.getDropOnlyScore() * SLAYER_TASK_BOOST,
			item.getBestItem(), item.getBestItemScore() * SLAYER_TASK_BOOST);
	}

	public ScoredItem scoreSource(CollectionLogSource source, boolean locked)
	{
		int missingCount = 0;
		int rolls = source.getRollsPerKill();
		int killTimeSeconds = getEffectiveKillTime(source);
		double killsPerHour = killTimeSeconds > 0 ? 3600.0 / killTimeSeconds : 0;

		// Track the best (fastest-to-obtain) individual item
		CollectionLogItem bestItem = null;
		double bestItemScore = 0;

		// Accumulate combined probability of obtaining ANY missing item per kill.
		// For probabilistic drops: P(any) = 1 - product(1 - p_i) for each missing item.
		// For multi-roll sources the per-item rates already account for rolls via
		// scoreIndividualItem, but the combined rate must use per-kill effective rates.
		double productMissAll = 1.0;
		int guaranteedMissing = 0;
		double bestGuaranteedScore = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (collectionState.isItemObtained(item.getItemId()))
			{
				continue;
			}
			missingCount++;

			double itemScore = scoreIndividualItem(item, source, killsPerHour, killTimeSeconds, rolls);
			if (itemScore > bestItemScore)
			{
				bestItemScore = itemScore;
				bestItem = item;
			}

			if (item.getDropRate() > 0 && item.getDropRate() < 1.0)
			{
				double effectiveRate = rolls > 1
					? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
					: item.getDropRate();
				productMissAll *= (1.0 - effectiveRate);
			}
			else if (item.getDropRate() >= 1.0)
			{
				guaranteedMissing++;
				if (itemScore > bestGuaranteedScore)
				{
					bestGuaranteedScore = itemScore;
				}
			}
		}

		if (missingCount == 0)
		{
			return null;
		}

		// Combined score: expected new log slots per hour from this source.
		// For probabilistic items: use combined "any new item" rate.
		// For guaranteed items: add the best guaranteed item's score (only one
		// can be the next unlock, so don't sum them all).
		//
		// Aggregated sources (e.g. "Miscellaneous") bundle items from unrelated
		// activities that cannot be farmed at a single location. Combined-rate
		// scoring would be misleading, so fall back to best-item scoring.
		double combinedDropRate = 1.0 - productMissAll;
		double combinedScore;
		if (source.isAggregated())
		{
			combinedScore = bestItemScore;
			combinedDropRate = 0;
		}
		else
		{
			combinedScore = combinedDropRate * killsPerHour * 100.0;
			if (guaranteedMissing > 0 && bestGuaranteedScore > combinedScore)
			{
				combinedScore = bestGuaranteedScore;
			}
		}

		double score = combinedScore;
		if (score <= 0)
		{
			score = missingCount * 0.2;
		}

		String reasoning = buildCombinedReasoning(bestItem, bestItemScore, missingCount,
			killTimeSeconds, rolls, combinedDropRate, killsPerHour, guaranteedMissing);
		double dropOnlyScore = combinedDropRate > 0 ? combinedDropRate * killsPerHour * 100.0 : 0;

		return new ScoredItem(source, score, missingCount, reasoning, locked,
			dropOnlyScore, bestItem, bestItemScore);
	}

	/**
	 * Scores an individual item based on how fast it can be obtained.
	 * Returns score = (1 / hoursToObtain) * 100, consistent with the overall scoring formula.
	 */
	double scoreIndividualItem(CollectionLogItem item, CollectionLogSource source,
		double killsPerHour, int killTimeSeconds, int rolls)
	{
		if (item.getDropRate() <= 0)
		{
			return 0;
		}
		if (item.getDropRate() >= 1.0)
		{
			// Guaranteed item — score by acquisition method
			if (item.getMilestoneKills() > 0 && killTimeSeconds > 0)
			{
				double hoursNeeded = item.getMilestoneKills() * (killTimeSeconds / 3600.0);
				return (1.0 / hoursNeeded) * 100.0;
			}
			else if (item.getPointCost() > 0 && source.getPointsPerHour() > 0)
			{
				double hoursNeeded = item.getPointCost() / source.getPointsPerHour();
				return (1.0 / hoursNeeded) * 100.0;
			}
			else
			{
				return 0.2;
			}
		}
		else
		{
			// Probabilistic drop — score by expected kills
			double effectiveRate = rolls > 1
				? 1.0 - Math.pow(1.0 - item.getDropRate(), rolls)
				: item.getDropRate();
			return effectiveRate * killsPerHour * 100.0;
		}
	}

	private String buildCombinedReasoning(CollectionLogItem bestItem, double bestItemScore,
		int missingCount, int killTimeSeconds, int rollsPerKill,
		double combinedDropRate, double killsPerHour, int guaranteedMissing)
	{
		if (bestItem == null)
		{
			return String.format("%d missing items", missingCount);
		}

		// Show combined "any new slot" time for the source
		String anySlotTime;
		if (combinedDropRate > 0 && killsPerHour > 0)
		{
			double anySlotHours = 1.0 / (combinedDropRate * killsPerHour);
			anySlotTime = formatHours(anySlotHours);
		}
		else
		{
			anySlotTime = "N/A";
		}

		// Also show the best individual item time
		String bestItemTime = bestItemScore > 0 ? formatHours(100.0 / bestItemScore) : "N/A";

		if (missingCount == 1)
		{
			// Only one item missing — combined rate equals single item rate
			if (bestItem.getDropRate() >= 1.0)
			{
				return String.format("%s in ~%s", bestItem.getName(), bestItemTime);
			}
			double effectiveRate = rollsPerKill > 1
				? 1.0 - Math.pow(1.0 - bestItem.getDropRate(), rollsPerKill)
				: bestItem.getDropRate();
			return String.format("%s ~%.0f kills, ~%s",
				bestItem.getName(), 1.0 / effectiveRate, bestItemTime);
		}

		// Multiple missing items — show combined rate and best item
		int probMissing = missingCount - guaranteedMissing;
		if (combinedDropRate > 0 && probMissing > 0)
		{
			long anySlotDenom = Math.max(2, Math.round(1.0 / combinedDropRate));
			return String.format("Any of %d items ~1/%d/kill, ~%s (best: %s ~%s)",
				missingCount, anySlotDenom, anySlotTime,
				bestItem.getName(), bestItemTime);
		}

		return String.format("%s in ~%s (%d missing total)",
			bestItem.getName(), bestItemTime, missingCount);
	}

	/**
	 * Returns the effective kill/completion time for a source. Adjusts for:
	 * - CLUES: account-progression-aware estimate from {@link ClueCompletionEstimator}
	 * - RAIDS: team size multiplier from config
	 * - SLAYER task-only: inflated by 1/P(creature|master) to account for task acquisition overhead
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

		// Slayer task-only sources: inflate kill time by 1/P(creature|master)
		// to account for the fact that you can only kill these on-task.
		// Skip if player is currently on this creature's task (boost handles that).
		if (SlayerCreatureDatabase.isTaskOnlySource(source.getName())
			&& !isOnSlayerTask(source))
		{
			double taskProb = getBestTaskProbability(source.getName());
			if (taskProb > 0 && taskProb < 1.0)
			{
				return Math.max(1, (int) (baseTime / taskProb));
			}
		}

		return baseTime;
	}

	/**
	 * Returns the highest probability of being assigned the creature task
	 * for the given source, across all Slayer masters.
	 */
	private double getBestTaskProbability(String sourceName)
	{
		String creature = SlayerCreatureDatabase.getCreatureForSource(sourceName);
		if (creature == null)
		{
			return 0;
		}

		double bestProb = 0;
		for (String masterName : slayerMasterDatabase.getMasterNames())
		{
			double prob = slayerMasterDatabase.getTaskProbability(masterName, creature);
			if (prob > bestProb)
			{
				bestProb = prob;
			}
		}
		return bestProb;
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
		int missingPetCount = 0;
		int rolls = source.getRollsPerKill();
		int killTimeSeconds = getEffectiveKillTime(source);
		double killsPerHour = killTimeSeconds > 0 ? 3600.0 / killTimeSeconds : 0;

		CollectionLogItem bestPet = null;
		double bestPetScore = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (!item.isPet() || collectionState.isItemObtained(item.getItemId()))
			{
				continue;
			}
			missingPetCount++;

			double itemScore = scoreIndividualItem(item, source, killsPerHour, killTimeSeconds, rolls);
			if (itemScore > bestPetScore)
			{
				bestPetScore = itemScore;
				bestPet = item;
			}
		}

		if (missingPetCount == 0)
		{
			return null;
		}

		// Pet hunt typically has 1 pet per source, so combined = best.
		// For sources with multiple pets, use the best score (pets are
		// independent rolls, not a combined table).
		double score = bestPetScore > 0 ? bestPetScore : missingPetCount * 0.2;
		String reasoning = buildCombinedReasoning(bestPet, bestPetScore, missingPetCount,
			killTimeSeconds, rolls, 0, killsPerHour, 0);
		double dropOnlyScore = bestPet != null && bestPet.getDropRate() < 1.0 ? bestPetScore : 0;

		return new ScoredItem(source, score, missingPetCount, reasoning, locked,
			dropOnlyScore, bestPet, bestPetScore);
	}

	/**
	 * Exports the full ranked efficiency list to a text file for debugging.
	 * Each source shows its score, reasoning, and per-item breakdown.
	 */
	public void exportEfficiencyList(File outputFile, Client client)
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
			pw.printf("=== Collection Log Helper — Efficiency Export ===%n");
			pw.printf("Exported: %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			pw.printf("Collection Log: %d/%d (%.1f%%)%n", totalObtained, totalItems,
				totalItems > 0 ? 100.0 * totalObtained / totalItems : 0);
			pw.printf("Varp totalObtained: %d | Tracked items: %d%n",
				collectionState.getTotalObtained(), collectionState.getObtainedCount());
			pw.printf("Total sources with missing items: %d%n", scored.size());

			// Player stats context
			if (client != null)
			{
				try
				{
					pw.printf("%n=== Account Stats ===%n");
					pw.printf("Combat: %d | Total: %d%n",
						client.getLocalPlayer() != null ? client.getLocalPlayer().getCombatLevel() : 0,
						client.getTotalLevel());
					for (Skill skill : Skill.values())
					{
						if (skill == Skill.OVERALL)
						{
							continue;
						}
						pw.printf("  %-15s %d%n", skill.getName() + ":", client.getRealSkillLevel(skill));
					}
				}
				catch (Exception e)
				{
					pw.printf("  (Could not read player stats)%n");
				}
			}

			// Slayer task context
			if (slayerTaskState.isTaskActive())
			{
				pw.printf("%nSlayer Task: %s (%d remaining)%n",
					slayerTaskState.getCreatureName(), slayerTaskState.getRemaining());
			}
			pw.println();

			int rank = 0;
			for (ScoredItem si : scored)
			{
				rank++;
				CollectionLogSource src = si.getSource();
				boolean onTask = isOnSlayerTask(src);

				pw.printf("--- #%d: %s ---%n", rank, src.getName());
				pw.printf("  Category: %s | RewardType: %s | AFK: %d | Locked: %s%s%n",
					src.getCategory(), src.getRewardType(), src.getAfkLevel(), si.isLocked(),
					onTask ? " | ON SLAYER TASK" : "");
				pw.printf("  Location: %s%n", src.getDisplayLocation(requirementsChecker));
				if (src.getTravelTip() != null && !src.getTravelTip().isEmpty())
				{
					pw.printf("  Travel: %s%n", src.getTravelTip());
				}
				pw.printf("  killTimeSeconds: %d (effective: %d) | rollsPerKill: %d%n",
					src.getKillTimeSeconds(), getEffectiveKillTime(src), src.getRollsPerKill());
				if (si.isLocked())
				{
					List<String> unmet = requirementsChecker.getUnmetRequirements(src.getName());
					if (!unmet.isEmpty())
					{
						pw.printf("  Unmet requirements: %s%n", String.join(", ", unmet));
					}
				}
				if (src.getPointsPerHour() > 0)
				{
					pw.printf("  pointsPerHour: %.0f%n", src.getPointsPerHour());
				}
				pw.printf("  Score: %.2f | DropOnlyScore: %.2f | Missing: %d%n",
					si.getScore(), si.getDropOnlyScore(), si.getMissingItemCount());
				pw.printf("  DisplayTime: %s%n", formatExportTime(si.getScore()));
				if (si.getBestItem() != null)
				{
					pw.printf("  BestItem: %s (score: %.2f, time: %s)%n",
						si.getBestItem().getName(), si.getBestItemScore(),
						formatExportTime(si.getBestItemScore()));
				}
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
						long denom = Math.max(2, Math.round(1.0 / item.getDropRate()));
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
