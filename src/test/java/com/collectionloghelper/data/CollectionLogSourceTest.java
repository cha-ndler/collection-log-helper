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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CollectionLogSourceTest
{
	@Mock
	private RequirementsChecker checker;

	private CollectionLogSource makeSource(String name, int x, int y, int plane,
		String locationDesc, List<Waypoint> waypoints)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, x, y, plane,
			60, 0, locationDesc, waypoints,
			null, 0, null, 0, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.emptyList());
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
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("DRAGON_SLAYER_II"), null);
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
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("DRAGON_SLAYER_II"), null);
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
		SourceRequirements reqs1 = new SourceRequirements(Collections.singletonList("QUEST_A"), null);
		SourceRequirements reqs2 = new SourceRequirements(Collections.singletonList("QUEST_B"), null);
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
		SourceRequirements reqs = new SourceRequirements(Collections.singletonList("QUEST_A"), null);
		Waypoint wp1 = new Waypoint("First", 1000, 1000, 0, reqs);
		Waypoint wp2 = new Waypoint("Last", 2000, 2000, 0, reqs);

		when(checker.meetsRequirements(any())).thenReturn(false);

		CollectionLogSource source = makeSource("Test", 3000, 3100, 0, "Default",
			Arrays.asList(wp1, wp2));
		assertEquals("Last", source.getDisplayLocation(checker));
	}
}
