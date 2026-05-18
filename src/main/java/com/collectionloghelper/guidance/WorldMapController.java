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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Owns world-map / hint-arrow plumbing for guidance.
 *
 * <p>Pure extraction from {@link GuidanceOverlayCoordinator}: this controller
 * holds no mutable state of its own.  Coordinator state it needs to read
 * (the active map point, current guidance step, active target item ID,
 * sequencer activity flag) is passed in via parameters so the coordinator
 * remains the single owner of guidance state.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Rotating the world-map edge-snap arrow each tick to point at the
 *       active map point ({@link #updateArrow(CollectionLogWorldMapPoint)}).</li>
 *   <li>Deciding whether a hint arrow should be placed at a given world
 *       point ({@link #shouldSetHintArrowTo(WorldPoint)}).</li>
 *   <li>Re-applying the hint arrow when {@code showHintArrow} is toggled or
 *       on teleport ({@link #refreshHintArrow(boolean, GuidanceStep)}).</li>
 *   <li>Mapping a guidance step to a {@link WorldMapDestinationOverlay.StepIconType}
 *       ({@link #resolveStepIconType(GuidanceStep, Integer)}).</li>
 * </ul>
 */
@Slf4j
@Singleton
public class WorldMapController
{
	private final Client client;
	private final ClientThread clientThread;
	private final CollectionLogHelperConfig config;

	@Inject
	WorldMapController(Client client, ClientThread clientThread, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
	}

	/**
	 * Rotates the active world-map point's edge-snap arrow to point from the
	 * current world-map viewport centre towards the target.  Returns silently
	 * when there is no active map point or the world map is not loaded.
	 *
	 * <p>The world map Y axis is inverted relative to screen-space (higher Y =
	 * further north = up on the map), so positive {@code dy} maps to "up".</p>
	 */
	public void updateArrow(@Nullable CollectionLogWorldMapPoint activeMapPoint)
	{
		if (activeMapPoint == null)
		{
			return;
		}

		net.runelite.api.worldmap.WorldMap worldMap = client.getWorldMap();
		if (worldMap == null)
		{
			return;
		}

		net.runelite.api.Point mapCenter = worldMap.getWorldMapPosition();
		if (mapCenter == null)
		{
			return;
		}

		WorldPoint target = activeMapPoint.getWorldPoint();
		int dx = target.getX() - mapCenter.getX();
		int dy = target.getY() - mapCenter.getY();

		// Map 8 octants to arrow directions
		int degrees;
		if (Math.abs(dx) > Math.abs(dy) * 2)
		{
			degrees = dx > 0 ? 0 : 180;
		}
		else if (Math.abs(dy) > Math.abs(dx) * 2)
		{
			degrees = dy > 0 ? 270 : 90;
		}
		else if (dx > 0)
		{
			degrees = dy > 0 ? 315 : 45;
		}
		else
		{
			degrees = dy > 0 ? 225 : 135;
		}

		activeMapPoint.rotateArrow(degrees);
	}

	/**
	 * Re-applies the hint arrow against the current guidance step, respecting
	 * {@code config.showHintArrow()}.  Called by the config-change handler when
	 * "Show Hint Arrow" is toggled mid-guidance so the change takes effect
	 * immediately rather than at the next step transition.
	 *
	 * <p>When {@code isActive} is false or the step has no world target, the
	 * arrow is cleared.  When the toggle is on and a step with a world target
	 * is active, the arrow is re-set at that target.</p>
	 *
	 * <p>All client mutations are dispatched on the client thread.</p>
	 */
	public void refreshHintArrow(boolean isActive, @Nullable GuidanceStep step)
	{
		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (!isActive)
			{
				return;
			}

			if (step == null || step.getWorldX() <= 0)
			{
				return;
			}

			WorldPoint worldPoint = new WorldPoint(
				step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			if (shouldSetHintArrowTo(worldPoint))
			{
				client.setHintArrow(worldPoint);
			}
		});
	}

	/**
	 * Decides whether a hint arrow should be placed at the given world point.
	 * Snapshots {@code getLocalPlayer()} once to avoid TOCTOU races.  Skips
	 * hint arrows inside non-top-level {@code WorldView}s (e.g. sailing boats).
	 */
	public boolean shouldSetHintArrowTo(@Nullable WorldPoint worldPoint)
	{
		if (!config.showHintArrow() || worldPoint == null)
		{
			return false;
		}
		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return false;
		}
		if (lp.getWorldView() != client.getTopLevelWorldView())
		{
			return false;
		}
		WorldPoint playerLoc = lp.getWorldLocation();
		return playerLoc != null && playerLoc.getPlane() == worldPoint.getPlane();
	}

	/**
	 * Determines the world-map destination icon type for a guidance step.
	 * NPC steps use the NPC icon, object steps use the object/chest icon,
	 * and all other steps use the generic tile diamond.
	 */
	public WorldMapDestinationOverlay.StepIconType resolveStepIconType(
		GuidanceStep step, @Nullable Integer activeTargetItemId)
	{
		if (step.resolveNpcId(activeTargetItemId) > 0)
		{
			return WorldMapDestinationOverlay.StepIconType.NPC;
		}
		if (step.getObjectId() > 0
			|| (step.getObjectIds() != null && !step.getObjectIds().isEmpty()))
		{
			return WorldMapDestinationOverlay.StepIconType.OBJECT;
		}
		return WorldMapDestinationOverlay.StepIconType.TILE;
	}
}
