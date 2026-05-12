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
package com.collectionloghelper.guidance;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Event-level tests for {@link GuidanceMovementTracker} pinning the
 * cha-ndler/collection-log-helper#381 contract: the tracker must call
 * {@code GuidanceOverlayCoordinator.refreshHintArrow()} when a teleport
 * happens during active guidance, and must NOT fire on ordinary walking,
 * first-tick baseline, or inactive guidance.
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceMovementTrackerTest
{
	@Mock
	private Client client;

	@Mock
	private GuidanceSequencer guidanceSequencer;

	@Mock
	private GuidanceOverlayCoordinator guidanceCoordinator;

	@Mock
	private Player localPlayer;

	private GuidanceMovementTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new GuidanceMovementTracker(client, guidanceSequencer, guidanceCoordinator);
		when(client.getLocalPlayer()).thenReturn(localPlayer);
	}

	/** First tick after activation establishes the baseline; no refresh fires. */
	@Test
	public void onGameTick_firstTickAfterActivation_doesNotRefresh()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));

		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/** Walking one tile between ticks must not trigger a refresh. */
	@Test
	public void onGameTick_walkingOneTile_doesNotRefresh()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))
			.thenReturn(new WorldPoint(3201, 3200, 0));

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/** Running two tiles between ticks (max walk speed) must not trigger a refresh. */
	@Test
	public void onGameTick_runningTwoTiles_doesNotRefresh()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))
			.thenReturn(new WorldPoint(3202, 3202, 0));

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/**
	 * Long-distance teleport (Mort'ton → GE is ~80 tiles) must trigger refresh.
	 * This is the #381 regression case.
	 */
	@Test
	public void onGameTick_longDistanceTeleport_refreshesHintArrow()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3492, 3299, 0))   // Mort'ton
			.thenReturn(new WorldPoint(3164, 3486, 0));  // Grand Exchange

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator).refreshHintArrow();
	}

	/** Plane change (climbing stairs, dungeon entry) must trigger refresh. */
	@Test
	public void onGameTick_planeChange_refreshesHintArrow()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))
			.thenReturn(new WorldPoint(3200, 3200, 1));

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator).refreshHintArrow();
	}

	/** Exactly at threshold (10 tiles) is still treated as walking; no refresh. */
	@Test
	public void onGameTick_deltaAtThreshold_doesNotRefresh()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))
			.thenReturn(new WorldPoint(3210, 3200, 0));

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/** One tile above threshold fires refresh — pins the boundary. */
	@Test
	public void onGameTick_deltaJustOverThreshold_refreshes()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))
			.thenReturn(new WorldPoint(3211, 3200, 0));

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator).refreshHintArrow();
	}

	/** Inactive guidance: no refresh fires regardless of position delta. */
	@Test
	public void onGameTick_guidanceInactive_doesNotRefresh()
	{
		when(guidanceSequencer.isActive()).thenReturn(false);

		tracker.onGameTick(new GameTick());
		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
		// Player location must not even be queried when inactive
		verify(localPlayer, never()).getWorldLocation();
	}

	/**
	 * Guidance deactivates mid-tracking — when it reactivates and a teleport
	 * happens on the very first active tick, no false refresh fires (baseline
	 * was reset on the inactive tick).
	 */
	@Test
	public void onGameTick_deactivationResetsBaseline_noFalseRefreshOnResume()
	{
		when(guidanceSequencer.isActive())
			.thenReturn(true)   // tick 1: active, sets baseline at Mort'ton
			.thenReturn(false)  // tick 2: inactive, clears baseline
			.thenReturn(true);  // tick 3: re-active at GE, must NOT diff vs Mort'ton
		when(localPlayer.getWorldLocation())
			.thenReturn(new WorldPoint(3492, 3299, 0))
			.thenReturn(new WorldPoint(3164, 3486, 0));

		tracker.onGameTick(new GameTick());  // baseline = Mort'ton
		tracker.onGameTick(new GameTick());  // inactive, baseline cleared
		tracker.onGameTick(new GameTick());  // re-active, baseline re-established at GE

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/** Null player (e.g. mid-login frame) is a no-op, no NPE. */
	@Test
	public void onGameTick_nullLocalPlayer_doesNotThrow()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(client.getLocalPlayer()).thenReturn(null);

		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}

	/** Null world location (transient client state) is a no-op, no NPE. */
	@Test
	public void onGameTick_nullWorldLocation_doesNotThrow()
	{
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(localPlayer.getWorldLocation()).thenReturn(null);

		tracker.onGameTick(new GameTick());

		verify(guidanceCoordinator, never()).refreshHintArrow();
	}
}
