package com.collectionloghelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RaidTeamSize
{
	SOLO("Solo", 1.0, 1.0),
	DUO("Duo", 0.75, 0.55),
	TRIO("Trio", 0.6, 0.4),
	FULL_GROUP("Full Group (4-5)", 0.5, 0.28);

	private final String displayName;
	private final double killTimeMultiplier;
	private final double dropRateMultiplier;

	@Override
	public String toString()
	{
		return displayName;
	}
}
