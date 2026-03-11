package com.collectionloghelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AfkFilter
{
	OFF("Off", 0),
	SEMI_AFK("Semi-AFK+", 1),
	AFK("AFK+", 2),
	VERY_AFK("Very AFK", 3);

	private final String displayName;
	private final int minAfkLevel;

	@Override
	public String toString()
	{
		return displayName;
	}
}
