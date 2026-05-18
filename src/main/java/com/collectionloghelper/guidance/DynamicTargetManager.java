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

import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Owns the per-tick dynamic-target evaluator dispatch for guidance.
 *
 * <p>Pure extraction from {@link GuidanceOverlayCoordinator}: this manager
 * holds no mutable state of its own.  Coordinator state it needs to read
 * (active map point, current guidance step, active target item id, cached
 * log icon) is passed in as method arguments, and any state mutation the
 * coordinator must apply (the new active map point) is returned directly
 * so the coordinator remains the single owner of guidance state.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Looking up the {@link DynamicTargetEvaluator} registered under the
 *       step's {@code dynamicTargetEvaluator} key.</li>
 *   <li>Evaluating it against the live client and current step to obtain
 *       a fresh {@link WorldPoint} target.</li>
 *   <li>Pushing the new target into the four location-sensitive overlays
 *       (in-world, minimap, world-map route, world-map destination).</li>
 *   <li>Refreshing the world map point in {@link WorldMapPointManager} and
 *       returning the new map point to the coordinator.</li>
 *   <li>Re-applying the hint arrow via {@link WorldMapController} when
 *       {@code config.showHintArrow()} is enabled and the local player is
 *       on a compatible plane / world view.</li>
 * </ul>
 *
 * <p>Called on the client thread once per game tick <em>only when guidance
 * is active</em> (the coordinator guards the call site).  Returns
 * {@code null} when the step has no evaluator key, the key is unregistered,
 * or the evaluator returns {@code null}; the coordinator treats {@code null}
 * as "no change" and keeps its existing active map point.</p>
 *
 * <p>This signature is allocation-free on guard-fail paths (issue #527):
 * inactive ticks bypass the call entirely (Option 1 — coordinator-side
 * guard), and the three active-tick guard-fails return a cached {@code null}
 * sentinel without constructing any wrapper objects.</p>
 */
@Slf4j
@Singleton
public class DynamicTargetManager
{
	private final Client client;
	private final DynamicTargetEvaluatorRegistry registry;
	private final WorldMapPointManager worldMapPointManager;
	private final GuidanceOverlay guidanceOverlay;
	private final GuidanceMinimapOverlay guidanceMinimapOverlay;
	private final WorldMapRouteOverlay worldMapRouteOverlay;
	private final WorldMapDestinationOverlay worldMapDestinationOverlay;
	private final WorldMapController worldMapController;

	@Inject
	DynamicTargetManager(
		Client client,
		DynamicTargetEvaluatorRegistry registry,
		WorldMapPointManager worldMapPointManager,
		GuidanceOverlay guidanceOverlay,
		GuidanceMinimapOverlay guidanceMinimapOverlay,
		WorldMapRouteOverlay worldMapRouteOverlay,
		WorldMapDestinationOverlay worldMapDestinationOverlay,
		WorldMapController worldMapController)
	{
		this.client = client;
		this.registry = registry;
		this.worldMapPointManager = worldMapPointManager;
		this.guidanceOverlay = guidanceOverlay;
		this.guidanceMinimapOverlay = guidanceMinimapOverlay;
		this.worldMapRouteOverlay = worldMapRouteOverlay;
		this.worldMapDestinationOverlay = worldMapDestinationOverlay;
		this.worldMapController = worldMapController;
	}

	/**
	 * Dispatches the dynamic-target evaluator for the active step (when present)
	 * and pushes the resulting world point to all location-sensitive overlays.
	 *
	 * <p><b>Precondition:</b> the caller has already verified that guidance is
	 * active; this method does not re-check {@code guidanceSequencer.isActive()}.
	 * The {@code !guidanceActive} guard was lifted to the coordinator call site
	 * to avoid per-tick allocations on inactive ticks (issue #527, Option 1).</p>
	 *
	 * @param currentStep         the active guidance step, may be {@code null}
	 * @param activeMapPoint      the coordinator's current active map point, may be {@code null}
	 * @param activeTargetItemId  the active target item id, may be {@code null}
	 * @param collectionLogIcon   the cached log icon for the new map point, may be {@code null}
	 * @return the new {@link CollectionLogWorldMapPoint} when the evaluator fired and
	 *         produced a fresh target, or {@code null} when no change occurred (the
	 *         coordinator should keep its existing {@code activeMapPoint}).
	 */
	@Nullable
	public CollectionLogWorldMapPoint tick(
		@Nullable GuidanceStep currentStep,
		@Nullable CollectionLogWorldMapPoint activeMapPoint,
		@Nullable Integer activeTargetItemId,
		@Nullable BufferedImage collectionLogIcon)
	{
		if (currentStep == null || currentStep.getDynamicTargetEvaluator() == null)
		{
			return null;
		}
		DynamicTargetEvaluator evaluator = registry.get(currentStep.getDynamicTargetEvaluator());
		if (evaluator == null)
		{
			return null;
		}
		WorldPoint dynamicPoint = evaluator.evaluate(client, currentStep);
		if (dynamicPoint == null)
		{
			return null;
		}

		guidanceOverlay.setTargetPoint(dynamicPoint);
		guidanceMinimapOverlay.setTargetPoint(dynamicPoint);
		worldMapRouteOverlay.setTargetPoint(dynamicPoint);
		worldMapDestinationOverlay.setTarget(dynamicPoint,
			worldMapController.resolveStepIconType(currentStep, activeTargetItemId));

		if (activeMapPoint != null)
		{
			worldMapPointManager.remove(activeMapPoint);
		}
		CollectionLogWorldMapPoint newMapPoint = new CollectionLogWorldMapPoint(dynamicPoint,
			currentStep.resolveDescription(activeTargetItemId), collectionLogIcon);
		worldMapPointManager.add(newMapPoint);
		worldMapDestinationOverlay.setMapPointActive(true);

		if (worldMapController.shouldSetHintArrowTo(dynamicPoint))
		{
			client.setHintArrow(dynamicPoint);
		}

		return newMapPoint;
	}
}
