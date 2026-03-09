package com.collectionloghelper.efficiency;

import com.collectionloghelper.data.CollectionLogItem;
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
	/**
	 * The single most efficient (fastest-to-obtain) missing item from this source.
	 * Used by the panel to display only the next recommended item.
	 * Null if no specific item can be identified (e.g. pure-guaranteed sources).
	 */
	CollectionLogItem bestItem;
	/**
	 * The individual score of the best item. This represents accurate time-to-obtain
	 * for that specific item, unlike the source-level score which may be a blended average.
	 */
	double bestItemScore;
}
