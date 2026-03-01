package com.collectionloghelper.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionLogCategory
{
    BOSSES("Bosses"),
    RAIDS("Raids"),
    CLUES("Clues"),
    MINIGAMES("Minigames"),
    OTHER("Other");

    private final String displayName;
}
