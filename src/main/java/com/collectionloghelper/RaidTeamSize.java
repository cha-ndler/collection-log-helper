package com.collectionloghelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RaidTeamSize
{
	SOLO("Solo", 1.0),
	DUO("Duo", 0.75),
	TRIO("Trio", 0.6),
	FULL_GROUP("Full Group (4-5)", 0.5);

	private final String displayName;
	private final double killTimeMultiplier;

	@Override
	public String toString()
	{
		return displayName;
	}
}
