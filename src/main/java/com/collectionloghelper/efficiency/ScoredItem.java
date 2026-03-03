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
}
