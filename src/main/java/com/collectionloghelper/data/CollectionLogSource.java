/*
 * Copyright (c) 2025, cha-ndler
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
import javax.annotation.Nullable;
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
	List<String> mutuallyExclusiveSources;
	int rollsPerKill;
	boolean aggregated;
	int afkLevel;
	String travelTip;
	int npcId;
	String interactAction;
	List<String> dialogOptions;
	List<GuidanceStep> guidanceSteps;

	/**
	 * Optional key naming a registered {@link com.collectionloghelper.guidance.bosses.BossGuidance}
	 * implementation that supplies steps for this source. When non-null the sequencer
	 * delegates step generation to the boss guidance instead of {@link #guidanceSteps}.
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 */
	@Nullable
	String guidanceHelperKey;

	SourceRequirements requirements;
	int cumulativeTrackItemId;
	List<Integer> cumulativeTrackObjectIds;
	int cumulativeTrackThreshold;
	List<CollectionLogItem> items;

	/**
	 * ISO 8601 date string (e.g. "2024-01-01") indicating when the meta-sensitive
	 * recommendations for this source were last authored or verified. Null means no
	 * date is known and the plugin will not surface a staleness annotation.
	 *
	 * <p>Backward compatible: existing JSON without this field deserialises to null.
	 */
	@Nullable
	String metaAuthoredDate;

	/**
	 * Optional override for the wiki Strategies page URL surfaced in the source
	 * header (#573). When null, {@link #getEffectiveWikiStrategyUrl()} derives a
	 * URL from the source name (e.g. {@code General Graardor} →
	 * {@code https://oldschool.runescape.wiki/w/General_Graardor/Strategies}).
	 * Sources whose wiki strategy page does not match the derived URL can override
	 * with the full URL.
	 *
	 * <p>Backward compatible: existing JSON without this field deserialises to null.
	 */
	@Nullable
	String wikiStrategyUrl;

	/**
	 * Optional source-level recommended gear item IDs (#573). When set, the
	 * source-header recommended chip strip uses this list verbatim. When null,
	 * {@link #getEffectiveRecommendedItemIds()} rolls up the union of per-step
	 * {@code recommendedItemIds} from {@link #guidanceSteps} so authors don't
	 * have to maintain two parallel lists.
	 *
	 * <p>Backward compatible: existing JSON without this field deserialises to null.
	 */
	@Nullable
	List<Integer> recommendedItemIds;

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

	/**
	 * Returns the effective wiki Strategies page URL for this source (#573).
	 *
	 * <p>If {@link #wikiStrategyUrl} is non-blank, returns it verbatim. Otherwise
	 * derives a URL from {@link #name} using the OSRS Wiki convention of
	 * replacing spaces with underscores and appending {@code /Strategies}:
	 * <pre>https://oldschool.runescape.wiki/w/&lt;urlEncodedName&gt;/Strategies</pre>
	 *
	 * <p>The derived URL is a best-effort guess. Sources whose wiki page name
	 * differs from the source name should set {@link #wikiStrategyUrl} explicitly.
	 *
	 * @return a non-null wiki Strategies URL
	 */
	public String getEffectiveWikiStrategyUrl()
	{
		if (wikiStrategyUrl != null && !wikiStrategyUrl.isEmpty())
		{
			return wikiStrategyUrl;
		}
		String slug = name == null ? "" : name.replace(' ', '_');
		return "https://oldschool.runescape.wiki/w/" + slug + "/Strategies";
	}

	/**
	 * Returns the effective source-level recommended-item IDs for the source
	 * header chip strip (#573).
	 *
	 * <p>If {@link #recommendedItemIds} is non-empty, returns it. Otherwise rolls
	 * up the union of per-step {@code recommendedItemIds} across
	 * {@link #guidanceSteps}, preserving insertion order and deduplicating.
	 * Returns an empty list when neither field has data.
	 *
	 * @return a non-null, possibly empty list of item IDs
	 */
	public List<Integer> getEffectiveRecommendedItemIds()
	{
		if (recommendedItemIds != null && !recommendedItemIds.isEmpty())
		{
			return recommendedItemIds;
		}
		if (guidanceSteps == null || guidanceSteps.isEmpty())
		{
			return java.util.Collections.emptyList();
		}
		java.util.LinkedHashSet<Integer> rollup = new java.util.LinkedHashSet<>();
		for (GuidanceStep step : guidanceSteps)
		{
			List<Integer> stepIds = step.getRecommendedItemIds();
			if (stepIds == null)
			{
				continue;
			}
			for (Integer id : stepIds)
			{
				if (id != null && id > 0)
				{
					rollup.add(id);
				}
			}
		}
		return new java.util.ArrayList<>(rollup);
	}
}
