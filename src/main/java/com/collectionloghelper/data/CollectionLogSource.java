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
    String locationDescription;
    List<Waypoint> waypoints;
    RewardType rewardType;
    double pointsPerHour;
    SourceRequirements requirements;
    List<CollectionLogItem> items;

    public RewardType getRewardType()
    {
        return rewardType != null ? rewardType : RewardType.DROP;
    }

    public WorldPoint getWorldPoint()
    {
        if (waypoints != null && !waypoints.isEmpty())
        {
            return waypoints.get(0).getWorldPoint();
        }
        return new WorldPoint(worldX, worldY, worldPlane);
    }

    public String getDisplayLocation()
    {
        if (locationDescription != null && !locationDescription.isEmpty())
        {
            return locationDescription;
        }
        return name;
    }
}
