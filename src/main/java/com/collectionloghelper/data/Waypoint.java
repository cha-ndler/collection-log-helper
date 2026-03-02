package com.collectionloghelper.data;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class Waypoint
{
    String name;
    int worldX;
    int worldY;
    int worldPlane;

    public WorldPoint getWorldPoint()
    {
        return new WorldPoint(worldX, worldY, worldPlane);
    }
}
