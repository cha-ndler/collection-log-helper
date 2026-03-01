package com.collectionloghelper.data;

import java.util.List;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class CollectionLogSource
{
    String name;
    CollectionLogCategory category;
    int worldX;
    int worldY;
    int worldPlane;
    int killTimeSeconds;
    List<CollectionLogItem> items;

    public WorldPoint getWorldPoint()
    {
        return new WorldPoint(worldX, worldY, worldPlane);
    }
}
