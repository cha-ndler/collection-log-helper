package com.collectionloghelper.efficiency;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EfficiencyCalculator
{
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final RequirementsChecker requirementsChecker;
	private final CollectionLogHelperConfig config;

	@Inject
	private EfficiencyCalculator(DropRateDatabase database, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker, CollectionLogHelperConfig config)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.requirementsChecker = requirementsChecker;
		this.config = config;
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

			ScoredItem scored = scoreSource(source, locked);
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

			ScoredItem scored = scoreSourcePetsOnly(source, locked);
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

	private ScoredItem scoreSource(CollectionLogSource source, boolean locked)
	{
		double dropDropRate = 0;
		int missingCount = 0;
		int guaranteedCount = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (!collectionState.isItemObtained(item.getItemId()))
			{
				missingCount++;
				if (item.getDropRate() >= 1.0)
				{
					guaranteedCount++;
				}
				else
				{
					dropDropRate += item.getDropRate();
				}
			}
		}

		if (missingCount == 0)
		{
			return null;
		}

		// All items are guaranteed (reward shop) — flat low score
		if (guaranteedCount == missingCount)
		{
			String reasoning = String.format("%d missing items (reward shop)", missingCount);
			return new ScoredItem(source, missingCount * 0.1, missingCount, reasoning, locked);
		}

		// Only score based on real probabilistic drops, ignore guaranteed items
		if (dropDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingCount - guaranteedCount;
		double expectedKills = 1.0 / dropDropRate;
		double expectedHours = expectedKills * (source.getKillTimeSeconds() / 3600.0);
		double score = (dropCount / expectedHours) * 100.0;

		String reasoning;
		if (guaranteedCount > 0)
		{
			reasoning = String.format("%d missing drops + %d shop items, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			reasoning = String.format("%d missing items, ~%.0f kills expected", missingCount, expectedKills);
		}

		return new ScoredItem(source, score, missingCount, reasoning, locked);
	}

	private ScoredItem scoreSourcePetsOnly(CollectionLogSource source, boolean locked)
	{
		double dropDropRate = 0;
		int missingPetCount = 0;
		int guaranteedCount = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (item.isPet() && !collectionState.isItemObtained(item.getItemId()))
			{
				missingPetCount++;
				if (item.getDropRate() >= 1.0)
				{
					guaranteedCount++;
				}
				else
				{
					dropDropRate += item.getDropRate();
				}
			}
		}

		if (missingPetCount == 0)
		{
			return null;
		}

		if (guaranteedCount == missingPetCount)
		{
			String reasoning = String.format("%d missing pets (reward shop)", missingPetCount);
			return new ScoredItem(source, missingPetCount * 0.1, missingPetCount, reasoning, locked);
		}

		if (dropDropRate <= 0)
		{
			return null;
		}

		int dropCount = missingPetCount - guaranteedCount;
		double expectedKills = 1.0 / dropDropRate;
		double expectedHours = expectedKills * (source.getKillTimeSeconds() / 3600.0);
		double score = (dropCount / expectedHours) * 100.0;

		String reasoning;
		if (guaranteedCount > 0)
		{
			reasoning = String.format("%d missing pet drops + %d shop, ~%.0f kills expected",
				dropCount, guaranteedCount, expectedKills);
		}
		else
		{
			reasoning = String.format("%d missing pets, ~%.0f kills expected", missingPetCount, expectedKills);
		}

		return new ScoredItem(source, score, missingPetCount, reasoning, locked);
	}
}
