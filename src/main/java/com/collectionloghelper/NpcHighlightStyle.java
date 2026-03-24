package com.collectionloghelper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NpcHighlightStyle
{
	HULL("Hull"),
	OUTLINE("Outline"),
	TILE("Tile");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
