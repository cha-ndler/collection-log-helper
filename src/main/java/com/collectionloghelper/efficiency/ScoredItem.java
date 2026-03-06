package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogSource;
import lombok.Value;

@Value
public class ScoredItem
{
	CollectionLogSource source;
	double score;
	int missingItemCount;
	String reasoning;
	boolean locked;
	/**
	 * Score based only on probabilistic drops, excluding guaranteed item inflation.
	 * When equal to {@link #score}, the source has no guaranteed items.
	 */
	double dropOnlyScore;
}
