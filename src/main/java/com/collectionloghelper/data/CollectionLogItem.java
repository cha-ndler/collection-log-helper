package com.collectionloghelper.data;

import lombok.Value;

@Value
public class CollectionLogItem
{
    int itemId;
    String name;
    double dropRate;
    int varbitId;
    boolean isPet;
    String wikiPage;
}
