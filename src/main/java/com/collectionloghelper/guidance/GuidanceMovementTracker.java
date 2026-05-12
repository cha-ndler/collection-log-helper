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

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks the player's per-tick movement during active guidance and reacts to
 * jumps that the rest of the guidance pipeline can't see through normal step
 * transitions.
 *
 * <p>Currently handles a single concern: long-distance teleport detection,
 * which re-applies the hint arrow after a scene reload would otherwise leave
 * it stale (cha-ndler/collection-log-helper#381). Extracted from
 * {@code GuidanceEventRouter} so additional movement-driven guidance behavior
 * (e.g. step regression on player-left-step-location for
 * cha-ndler/collection-log-helper#378) can live alongside it without further
 * inflating the router.
 *
 * <p><b>Thread model:</b> {@code @Subscribe} handlers fire on RuneLite's
 * client thread, so all field access and {@link Client} reads happen there.
 * No volatile fields needed. The downstream {@code refreshHintArrow()} call
 * dispatches its own client-thread work via {@code ClientThread.invokeLater}.
 *
 * <p><b>Registration:</b> the plugin must call
 * {@code eventBus.register(this)} in {@code startUp()} and
 * {@code eventBus.unregister(this)} in {@code shutDown()}.
 */
@Singleton
public class GuidanceMovementTracker
{
	/**
	 * Chebyshev-distance threshold (tiles) above which a single-tick player
	 * position delta is treated as a teleport rather than walking. Running
	 * caps at 2 tiles/tick in OSRS, so any single-tick delta over this is
	 * unambiguously a teleport, fairy ring, mounted glory, etc. — never
	 * normal locomotion.
	 */
	static final int TELEPORT_DISTANCE_THRESHOLD = 10;

	private final Client client;
	private final GuidanceSequencer guidanceSequencer;
	private final GuidanceOverlayCoordinator guidanceCoordinator;

	/**
	 * Player world location at the end of the previous game tick. Reset to
	 * {@code null} when guidance is inactive so the next active tick re-
	 * baselines without firing a false-positive refresh on the resumption
	 * tick.
	 */
	private WorldPoint lastTickPlayerLocation;

	@Inject
	public GuidanceMovementTracker(
		Client client,
		GuidanceSequencer guidanceSequencer,
		GuidanceOverlayCoordinator guidanceCoordinator)
	{
		this.client = client;
		this.guidanceSequencer = guidanceSequencer;
		this.guidanceCoordinator = guidanceCoordinator;
	}

	/**
	 * Detects long-distance teleports during active guidance and re-applies
	 * the hint arrow so it doesn't go stale at the previous step location.
	 *
	 * <p>Background: {@code client.setHintArrow(WorldPoint)} is bound to the
	 * loaded scene. When the player teleports (Mort'ton → GE → back), the
	 * scene reloads and the previous arrow target is no longer rendered;
	 * walking back into range does not auto-restore it. Before this handler,
	 * the only paths that re-set the arrow were step transitions and the
	 * "Show Hint Arrow" config toggle — neither fires on a teleport.
	 *
	 * <p>{@link GuidanceOverlayCoordinator#refreshHintArrow()} already gates
	 * on {@code config.showHintArrow()} and the active step's world target,
	 * so this method fires unconditionally on teleport and lets the
	 * coordinator decide whether to set, clear, or no-op.
	 *
	 * <p>Closes cha-ndler/collection-log-helper#381.
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!guidanceSequencer.isActive())
		{
			lastTickPlayerLocation = null;
			return;
		}

		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return;
		}

		WorldPoint current = lp.getWorldLocation();
		if (current == null)
		{
			return;
		}

		WorldPoint previous = lastTickPlayerLocation;
		lastTickPlayerLocation = current;

		if (previous == null)
		{
			return;
		}

		if (previous.getPlane() != current.getPlane()
			|| chebyshev(previous, current) > TELEPORT_DISTANCE_THRESHOLD)
		{
			guidanceCoordinator.refreshHintArrow();
		}
	}

	/**
	 * Chebyshev (chessboard) distance between two world points on the same
	 * plane. Caller is responsible for plane equality. Inlined instead of
	 * relying on {@code WorldPoint.distanceTo} so the algorithm is explicit
	 * and unit-testable without depending on RuneLite API distance semantics
	 * (which return {@code Integer.MAX_VALUE} across planes).
	 */
	private static int chebyshev(WorldPoint a, WorldPoint b)
	{
		int dx = Math.abs(a.getX() - b.getX());
		int dy = Math.abs(a.getY() - b.getY());
		return Math.max(dx, dy);
	}
}
