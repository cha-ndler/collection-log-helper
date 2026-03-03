package com.collectionloghelper.data;

public enum RewardType
{
	DROP,        // Standard probabilistic drops
	SHOP,        // All items bought with points/currency (dropRate=1.0)
	MIXED,       // Real drops + shop/guaranteed items in same source
	GUARANTEED,  // Completion rewards with some real drops
	MILESTONE    // Kill-count threshold unlocks
}
