package com.collectionloghelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountType
{
	MAIN("Main"),
	IRONMAN("Ironman");

	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
