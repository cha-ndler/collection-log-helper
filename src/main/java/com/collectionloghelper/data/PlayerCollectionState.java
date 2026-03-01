package com.collectionloghelper.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;

@Singleton
public class PlayerCollectionState
{
	private final Client client;

	@Inject
	private PlayerCollectionState(Client client)
	{
		this.client = client;
	}

	public boolean isItemObtained(int varbitId)
	{
		if (varbitId <= 0)
		{
			return false;
		}
		return client.getVarbitValue(varbitId) != 0;
	}

	public int getTotalObtained()
	{
		return client.getVarpValue(VarPlayerID.COLLECTION_COUNT);
	}

	public int getTotalPossible()
	{
		return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX);
	}

	public int getCategoryCount(CollectionLogCategory category)
	{
		switch (category)
		{
			case BOSSES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_BOSSES);
			case RAIDS:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_RAIDS);
			case CLUES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_CLUES);
			case MINIGAMES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MINIGAMES);
			case OTHER:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_OTHER);
			default:
				return 0;
		}
	}

	public int getCategoryMax(CollectionLogCategory category)
	{
		switch (category)
		{
			case BOSSES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_BOSSES_MAX);
			case RAIDS:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_RAIDS_MAX);
			case CLUES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_CLUES_MAX);
			case MINIGAMES:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MINIGAMES_MAX);
			case OTHER:
				return client.getVarpValue(VarPlayerID.COLLECTION_COUNT_OTHER_MAX);
			default:
				return 0;
		}
	}

	public double getCompletionPercentage()
	{
		int max = getTotalPossible();
		if (max == 0)
		{
			return 0.0;
		}
		return (getTotalObtained() / (double) max) * 100.0;
	}
}
