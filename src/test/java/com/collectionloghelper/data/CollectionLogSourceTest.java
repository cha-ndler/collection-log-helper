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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CollectionLogSourceTest
{
	@Mock
	private RequirementsChecker checker;

	private CollectionLogSource makeSource(String name, int x, int y, int plane,
		String locationDesc, List<Waypoint> waypoints)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, x, y, plane,
			60, 0, locationDesc, waypoints,
			null, 0, null, 0, false, 0, null, 0, null, null, null, null, null, 0, null, 0,
			Collections.emptyList(), null, null, null);

	}

	// ========================================================================
	// getRewardType — null fallback
	// ========================================================================

	@Test
	public void getRewardType_nullFallsToDrop()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3000, 0, null, null);
		assertEquals(RewardType.DROP, source.getRewardType());
	}

	// ========================================================================
	// getRollsPerKill — default fallback
	// ========================================================================

	@Test
	public void getRollsPerKill_zeroDefaultsToOne()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3000, 0, null, null);
		assertEquals(1, source.getRollsPerKill());
	}

	// ========================================================================
	// getWorldPoint — no waypoints
	// ========================================================================

	@Test
	public void getWorldPoint_noWaypoints_usesBaseCoords()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3100, 1, null, null);
		WorldPoint wp = source.getWorldPoint();
		assertEquals(3000, wp.getX());
		assertEquals(3100, wp.getY());
		assertEquals(1, wp.getPlane());
	}

	@Test
	public void getWorldPoint_emptyWaypoints_usesBaseCoords()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, Collections.emptyList());
		WorldPoint wp = source.getWorldPoint();
		assertEquals(3000, wp.getX());
		assertEquals(3100, wp.getY());
	}

	// ========================================================================
	// getWorldPoint — with waypoints (no checker)
	// ========================================================================

	@Test
	public void getWorldPoint_withWaypoints_returnsFirst()
	{
		Waypoint wp1 = new Waypoint("Near", 2500, 2600, 0, null);
		Waypoint wp2 = new Waypoint("Far", 3500, 3600, 0, null);
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, Arrays.asList(wp1, wp2));

		WorldPoint result = source.getWorldPoint();
		assertEquals(2500, result.getX());
		assertEquals(2600, result.getY());
	}

	// ========================================================================
	// getWorldPoint(checker) — requirement-gated waypoints
	// ========================================================================

	@Test
	public void getWorldPointWithChecker_firstAccessible()
	{
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("DRAGON_SLAYER_II"), null, null, null, null, null, null, null, null);
		Waypoint wp1 = new Waypoint("Shortcut", 2500, 2600, 0, reqs);
		Waypoint wp2 = new Waypoint("Walk", 3500, 3600, 0, null);

		when(checker.meetsRequirements(reqs)).thenReturn(true);

		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, Arrays.asList(wp1, wp2));
		WorldPoint result = source.getWorldPoint(checker);
		assertEquals(2500, result.getX());
	}

	@Test
	public void getWorldPointWithChecker_firstLocked_fallsToSecond()
	{
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("DRAGON_SLAYER_II"), null, null, null, null, null, null, null, null);
		Waypoint wp1 = new Waypoint("Shortcut", 2500, 2600, 0, reqs);
		Waypoint wp2 = new Waypoint("Walk", 3500, 3600, 0, null);

		when(checker.meetsRequirements(reqs)).thenReturn(false);

		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, Arrays.asList(wp1, wp2));
		WorldPoint result = source.getWorldPoint(checker);
		assertEquals(3500, result.getX());
	}

	@Test
	public void getWorldPointWithChecker_allLocked_fallsToLast()
	{
		SourceRequirements reqs1 = new SourceRequirements(Collections.singletonList("QUEST_A"), null, null, null, null, null, null, null, null);
		SourceRequirements reqs2 = new SourceRequirements(Collections.singletonList("QUEST_B"), null, null, null, null, null, null, null, null);
		Waypoint wp1 = new Waypoint("Best", 1000, 1000, 0, reqs1);
		Waypoint wp2 = new Waypoint("Alt", 2000, 2000, 0, reqs2);

		when(checker.meetsRequirements(any())).thenReturn(false);

		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, Arrays.asList(wp1, wp2));
		WorldPoint result = source.getWorldPoint(checker);
		assertEquals(2000, result.getX());
	}

	@Test
	public void getWorldPointWithChecker_noWaypoints_usesBaseCoords()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, null, null);
		WorldPoint result = source.getWorldPoint(checker);
		assertEquals(3000, result.getX());
		assertEquals(3100, result.getY());
	}

	// ========================================================================
	// getDisplayLocation — with and without checker
	// ========================================================================

	@Test
	public void getDisplayLocation_usesLocationDescription()
	{
		CollectionLogSource source = makeSource("Cerberus", 1310, 1251, 0, "Taverley Dungeon", null);
		assertEquals("Taverley Dungeon", source.getDisplayLocation());
	}

	@Test
	public void getDisplayLocation_fallsBackToName()
	{
		CollectionLogSource source = makeSource("Cerberus", 1310, 1251, 0, null, null);
		assertEquals("Cerberus", source.getDisplayLocation());
	}

	@Test
	public void getDisplayLocation_emptyDescriptionFallsBackToName()
	{
		CollectionLogSource source = makeSource("Cerberus", 1310, 1251, 0, "", null);
		assertEquals("Cerberus", source.getDisplayLocation());
	}

	@Test
	public void getDisplayLocationWithChecker_accessibleWaypointWithName()
	{
		Waypoint wp = new Waypoint("Shortcut Route", 2500, 2600, 0, null);
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, "Default Location",
			Collections.singletonList(wp));

		assertEquals("Shortcut Route", source.getDisplayLocation(checker));
	}

	@Test
	public void getDisplayLocationWithChecker_waypointWithNullName()
	{
		Waypoint wp = new Waypoint(null, 2500, 2600, 0, null);
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, "Default Location",
			Collections.singletonList(wp));

		assertEquals("Default Location", source.getDisplayLocation(checker));
	}

	@Test
	public void getDisplayLocationWithChecker_noWaypoints()
	{
		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, "Default Location", null);
		assertEquals("Default Location", source.getDisplayLocation(checker));
	}

	@Test
	public void getDisplayLocationWithChecker_allLockedFallsToLastName()
	{
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("QUEST_A"), null, null, null, null, null, null, null, null);
		Waypoint wp1 = new Waypoint("First", 1000, 1000, 0, reqs);
		Waypoint wp2 = new Waypoint("Last", 2000, 2000, 0, reqs);

		when(checker.meetsRequirements(any())).thenReturn(false);

		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, "Default",
			Arrays.asList(wp1, wp2));
		assertEquals("Last", source.getDisplayLocation(checker));
	}

	// ========================================================================
	// #573 — wiki Strategies URL derivation + per-source override
	// ========================================================================

	private CollectionLogSource makeSourceWith573(String name, String wikiOverride,
		List<Integer> recommendedItemIds, List<GuidanceStep> guidanceSteps)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 3000, 3000, 0,
			60, 0, null, Collections.emptyList(),
			null, 0, null, 0, false, 0, null, 0, null, null,
			guidanceSteps != null ? guidanceSteps : Collections.<GuidanceStep>emptyList(),
			null, null, 0, null, 0,
			Collections.<CollectionLogItem>emptyList(), null,
			wikiOverride, recommendedItemIds);
	}

	@Test
	public void getEffectiveWikiStrategyUrl_derivesFromSourceNameWithUnderscores()
	{
		CollectionLogSource graardor = makeSourceWith573("General Graardor", null, null, null);
		assertEquals(
			"https://oldschool.runescape.wiki/w/General_Graardor/Strategies",
			graardor.getEffectiveWikiStrategyUrl());
	}

	@Test
	public void getEffectiveWikiStrategyUrl_returnsOverrideVerbatimWhenSet()
	{
		String url = "https://oldschool.runescape.wiki/w/Theatre_of_Blood/Strategies";
		CollectionLogSource tob = makeSourceWith573("Theatre of Blood", url, null, null);
		assertEquals(url, tob.getEffectiveWikiStrategyUrl());
	}

	@Test
	public void getEffectiveWikiStrategyUrl_emptyOverrideFallsBackToDerivedUrl()
	{
		CollectionLogSource s = makeSourceWith573("Cerberus", "", null, null);
		assertEquals("https://oldschool.runescape.wiki/w/Cerberus/Strategies",
			s.getEffectiveWikiStrategyUrl());
	}

	@Test
	public void getEffectiveWikiStrategyUrl_singleWordSourceName()
	{
		CollectionLogSource s = makeSourceWith573("Cerberus", null, null, null);
		assertEquals("https://oldschool.runescape.wiki/w/Cerberus/Strategies",
			s.getEffectiveWikiStrategyUrl());
	}

	// ========================================================================
	// #573 — source-level recommended item rollup with optional override
	// ========================================================================

	@Test
	public void getEffectiveRecommendedItemIds_returnsOverrideWhenSet()
	{
		List<Integer> override = Arrays.asList(11802, 12817, 4151);
		CollectionLogSource s = makeSourceWith573("Test", null, override, null);
		assertEquals(override, s.getEffectiveRecommendedItemIds());
	}

	@Test
	public void getEffectiveRecommendedItemIds_emptyOverrideRollsUpFromSteps()
	{
		GuidanceStep step = stepWithRecommendedIds(Arrays.asList(100, 200));
		CollectionLogSource s = makeSourceWith573("Test", null,
			Collections.<Integer>emptyList(), Collections.singletonList(step));

		assertEquals(Arrays.asList(100, 200), s.getEffectiveRecommendedItemIds());
	}

	@Test
	public void getEffectiveRecommendedItemIds_nullOverrideRollsUpUnionAcrossSteps()
	{
		GuidanceStep s1 = stepWithRecommendedIds(Arrays.asList(100, 200));
		GuidanceStep s2 = stepWithRecommendedIds(Arrays.asList(200, 300)); // dedupe 200
		GuidanceStep s3 = stepWithRecommendedIds(Arrays.asList(400));
		CollectionLogSource s = makeSourceWith573("Test", null, null, Arrays.asList(s1, s2, s3));

		assertEquals(
			Arrays.asList(100, 200, 300, 400), s.getEffectiveRecommendedItemIds(),"Rollup must dedupe and preserve insertion order");
	}

	@Test
	public void getEffectiveRecommendedItemIds_skipsNullAndNonPositiveIds()
	{
		GuidanceStep s1 = stepWithRecommendedIds(Arrays.asList(0, -1, 100, null));
		CollectionLogSource s = makeSourceWith573("Test", null, null, Collections.singletonList(s1));

		assertEquals(Collections.singletonList(100), s.getEffectiveRecommendedItemIds());
	}

	@Test
	public void getEffectiveRecommendedItemIds_emptyEverythingReturnsEmpty()
	{
		CollectionLogSource s = makeSourceWith573("Test", null, null, Collections.<GuidanceStep>emptyList());
		assertTrue(s.getEffectiveRecommendedItemIds().isEmpty());
	}

	@Test
	public void getEffectiveRecommendedItemIds_skipsStepsWithNullRecommendedList()
	{
		GuidanceStep withRec = stepWithRecommendedIds(Arrays.asList(100));
		GuidanceStep withoutRec = stepWithRecommendedIds(null);
		CollectionLogSource s = makeSourceWith573("Test", null, null,
			Arrays.asList(withoutRec, withRec, withoutRec));

		assertEquals(Collections.singletonList(100), s.getEffectiveRecommendedItemIds());
	}

	/**
	 * Builds a minimal {@link GuidanceStep} carrying only the
	 * {@code recommendedItemIds} field — every other slot is zero/empty/null.
	 * Mirrors the pattern used in {@code GuidanceStepTest#stepWithOverrides}.
	 */
	private static GuidanceStep stepWithRecommendedIds(List<Integer> recommendedItemIds)
	{
		return new GuidanceStep(
			"desc",
			null,           // perItemStepDescription
			0, 0, 0,        // worldX, worldY, worldPlane
			0, null, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			recommendedItemIds,
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,    // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,           // completionNpcIds
			null,           // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			0, 0,           // completionVarbitId, completionVarbitValue
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			null,           // highlightWidgetIds
			0, 0,           // loopBackToStep, loopCount
			null,           // skipIfHasAnyItemIds
			null,           // dynamicItemObjectTiers
			null,           // completionZone
			null,           // conditionalAlternatives
			null,           // section
			null,           // waypoints
			null,            // dynamicTargetEvaluator
			null,            // conditionTree
			null            // perItemStepPriority
		);
	}
}
