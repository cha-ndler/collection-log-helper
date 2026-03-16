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
    int ironKillTimeSeconds;
    String locationDescription;
    List<Waypoint> waypoints;
    RewardType rewardType;
    double pointsPerHour;
    boolean mutuallyExclusive;
    int rollsPerKill;
    boolean aggregated;
    int afkLevel;
    String travelTip;
    int npcId;
    String interactAction;
    List<String> dialogOptions;
    List<GuidanceStep> guidanceSteps;
    SourceRequirements requirements;
    int cumulativeTrackItemId;
    List<Integer> cumulativeTrackObjectIds;
    int cumulativeTrackThreshold;
    List<CollectionLogItem> items;

    public RewardType getRewardType()
    {
        return rewardType != null ? rewardType : RewardType.DROP;
    }

    public int getRollsPerKill()
    {
        return rollsPerKill > 0 ? rollsPerKill : 1;
    }

    public WorldPoint getWorldPoint()
    {
        if (waypoints != null && !waypoints.isEmpty())
        {
            return waypoints.get(0).getWorldPoint();
        }
        return new WorldPoint(worldX, worldY, worldPlane);
    }

    /**
     * Returns the best accessible waypoint for this source based on the player's
     * quest/skill progression. Falls back to the first waypoint or default coords.
     */
    public WorldPoint getWorldPoint(RequirementsChecker checker)
    {
        if (waypoints != null && !waypoints.isEmpty())
        {
            for (Waypoint wp : waypoints)
            {
                if (wp.getRequirements() == null || checker.meetsRequirements(wp.getRequirements()))
                {
                    return wp.getWorldPoint();
                }
            }
            // All waypoints locked — use last as fallback
            return waypoints.get(waypoints.size() - 1).getWorldPoint();
        }
        return new WorldPoint(worldX, worldY, worldPlane);
    }

    /**
     * Returns the display location for the best accessible waypoint.
     */
    public String getDisplayLocation(RequirementsChecker checker)
    {
        if (waypoints != null && !waypoints.isEmpty())
        {
            for (Waypoint wp : waypoints)
            {
                if (wp.getRequirements() == null || checker.meetsRequirements(wp.getRequirements()))
                {
                    return wp.getName() != null ? wp.getName() : getDisplayLocation();
                }
            }
            Waypoint last = waypoints.get(waypoints.size() - 1);
            return last.getName() != null ? last.getName() : getDisplayLocation();
        }
        return getDisplayLocation();
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
