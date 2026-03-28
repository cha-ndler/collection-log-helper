/*
 * Copyright (c) 2025, Chandler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
    List<String> mutuallyExclusiveSources;
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
