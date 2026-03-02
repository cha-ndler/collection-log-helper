package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
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

	@Inject
	private EfficiencyCalculator(DropRateDatabase database, PlayerCollectionState collectionState)
	{
		this.database = database;
		this.collectionState = collectionState;
	}

	public List<ScoredItem> rankByEfficiency()
	{
		List<ScoredItem> results = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			ScoredItem scored = scoreSource(source);
			if (scored != null)
			{
				results.add(scored);
			}
		}

		results.sort(Comparator.comparingDouble(ScoredItem::getScore).reversed());
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

		for (CollectionLogSource source : database.getAllSources())
		{
			ScoredItem scored = scoreSourcePetsOnly(source);
			if (scored != null)
			{
				results.add(scored);
			}
		}

		results.sort(Comparator.comparingDouble(ScoredItem::getScore).reversed());
		return results;
	}

	private ScoredItem scoreSource(CollectionLogSource source)
	{
		double combinedDropRate = 0;
		int missingCount = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (!collectionState.isItemObtained(item.getItemId()))
			{
				combinedDropRate += item.getDropRate();
				missingCount++;
			}
		}

		if (missingCount == 0 || combinedDropRate <= 0)
		{
			return null;
		}

		double expectedKills = 1.0 / combinedDropRate;
		double expectedHours = expectedKills * (source.getKillTimeSeconds() / 3600.0);
		double score = (missingCount / expectedHours) * 100.0;

		String reasoning = String.format("%d missing items, ~%.0f kills expected", missingCount, expectedKills);

		return new ScoredItem(source, score, missingCount, reasoning);
	}

	private ScoredItem scoreSourcePetsOnly(CollectionLogSource source)
	{
		double combinedDropRate = 0;
		int missingPetCount = 0;

		for (CollectionLogItem item : source.getItems())
		{
			if (item.isPet() && !collectionState.isItemObtained(item.getItemId()))
			{
				combinedDropRate += item.getDropRate();
				missingPetCount++;
			}
		}

		if (missingPetCount == 0 || combinedDropRate <= 0)
		{
			return null;
		}

		double expectedKills = 1.0 / combinedDropRate;
		double expectedHours = expectedKills * (source.getKillTimeSeconds() / 3600.0);
		double score = (missingPetCount / expectedHours) * 100.0;

		String reasoning = String.format("%d missing pets, ~%.0f kills expected", missingPetCount, expectedKills);

		return new ScoredItem(source, score, missingPetCount, reasoning);
	}
}
