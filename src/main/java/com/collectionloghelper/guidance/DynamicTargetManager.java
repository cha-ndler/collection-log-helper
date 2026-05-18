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
 * (active map point, current guidance step, sequencer activity, active
 * target item id, cached log icon) is passed in via the {@link Input}
 * carrier, and any state mutation the coordinator must apply (the new
 * active map point) is returned in {@link Result} so the coordinator
 * remains the single owner of guidance state.</p>
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
 *       reporting the new map point back via {@link Result}.</li>
 *   <li>Re-applying the hint arrow via {@link WorldMapController} when
 *       {@code config.showHintArrow()} is enabled and the local player is
 *       on a compatible plane / world view.</li>
 * </ul>
 *
 * <p>Called on the client thread once per game tick.  Returns silently
 * (with {@link Result#unchanged(CollectionLogWorldMapPoint)}) when guidance
 * is inactive, the step has no evaluator key, the key is unregistered, or
 * the evaluator returns {@code null}.</p>
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
	 * Returns a {@link Result} describing any change to the coordinator's
	 * {@code activeMapPoint} so the caller can apply it without this manager
	 * needing direct access to coordinator state.
	 */
	public Result tick(Input input)
	{
		if (!input.guidanceActive)
		{
			return Result.unchanged(input.activeMapPoint);
		}
		GuidanceStep step = input.currentStep;
		if (step == null || step.getDynamicTargetEvaluator() == null)
		{
			return Result.unchanged(input.activeMapPoint);
		}
		DynamicTargetEvaluator evaluator = registry.get(step.getDynamicTargetEvaluator());
		if (evaluator == null)
		{
			return Result.unchanged(input.activeMapPoint);
		}
		WorldPoint dynamicPoint = evaluator.evaluate(client, step);
		if (dynamicPoint == null)
		{
			return Result.unchanged(input.activeMapPoint);
		}

		guidanceOverlay.setTargetPoint(dynamicPoint);
		guidanceMinimapOverlay.setTargetPoint(dynamicPoint);
		worldMapRouteOverlay.setTargetPoint(dynamicPoint);
		worldMapDestinationOverlay.setTarget(dynamicPoint,
			worldMapController.resolveStepIconType(step, input.activeTargetItemId));

		if (input.activeMapPoint != null)
		{
			worldMapPointManager.remove(input.activeMapPoint);
		}
		CollectionLogWorldMapPoint newMapPoint = new CollectionLogWorldMapPoint(dynamicPoint,
			step.resolveDescription(input.activeTargetItemId), input.collectionLogIcon);
		worldMapPointManager.add(newMapPoint);
		worldMapDestinationOverlay.setMapPointActive(true);

		if (worldMapController.shouldSetHintArrowTo(dynamicPoint))
		{
			client.setHintArrow(dynamicPoint);
		}

		return Result.changed(newMapPoint);
	}

	/**
	 * Inputs the coordinator passes to {@link #tick(Input)}.  Mirrors the
	 * subset of coordinator state the dynamic-target dispatch reads.
	 */
	public static final class Input
	{
		private final boolean guidanceActive;
		@Nullable
		private final GuidanceStep currentStep;
		@Nullable
		private final CollectionLogWorldMapPoint activeMapPoint;
		@Nullable
		private final Integer activeTargetItemId;
		@Nullable
		private final BufferedImage collectionLogIcon;

		public Input(boolean guidanceActive,
			@Nullable GuidanceStep currentStep,
			@Nullable CollectionLogWorldMapPoint activeMapPoint,
			@Nullable Integer activeTargetItemId,
			@Nullable BufferedImage collectionLogIcon)
		{
			this.guidanceActive = guidanceActive;
			this.currentStep = currentStep;
			this.activeMapPoint = activeMapPoint;
			this.activeTargetItemId = activeTargetItemId;
			this.collectionLogIcon = collectionLogIcon;
		}
	}

	/**
	 * Result of a single {@link #tick(Input)} call.  Carries the (possibly
	 * new) active world map point so the coordinator can replace its own
	 * reference without exposing internal fields to this manager.
	 */
	public static final class Result
	{
		private final boolean changed;
		@Nullable
		private final CollectionLogWorldMapPoint activeMapPoint;

		private Result(boolean changed, @Nullable CollectionLogWorldMapPoint activeMapPoint)
		{
			this.changed = changed;
			this.activeMapPoint = activeMapPoint;
		}

		static Result changed(CollectionLogWorldMapPoint newPoint)
		{
			return new Result(true, newPoint);
		}

		static Result unchanged(@Nullable CollectionLogWorldMapPoint existingPoint)
		{
			return new Result(false, existingPoint);
		}

		/** True when the evaluator fired and a new active map point was produced. */
		public boolean isChanged()
		{
			return changed;
		}

		/**
		 * The active world map point the coordinator should now hold.  When
		 * {@link #isChanged()} is false this is the input's existing point
		 * (echoed for caller convenience).
		 */
		@Nullable
		public CollectionLogWorldMapPoint getActiveMapPoint()
		{
			return activeMapPoint;
		}
	}
}
