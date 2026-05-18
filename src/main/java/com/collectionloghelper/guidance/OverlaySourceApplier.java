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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Applies a {@link CollectionLogSource}'s default location data to all guidance
 * overlays (the non-sequencer path). Used when guidance is activated for a
 * single-step source that has no per-step {@code GuidanceStep} list.
 *
 * <p>Pure extraction from {@link GuidanceOverlayCoordinator}: this class holds
 * no mutable state of its own. Mutable coordinator state that the source
 * application touches (active world map point, pending shortest-path target)
 * is passed in via {@link Input} and returned via {@link Result} so the
 * coordinator remains the single owner of that state.</p>
 *
 * <p>Mirrors the structural pattern established by {@link OverlayStepApplier}.</p>
 */
@Slf4j
@Singleton
public class OverlaySourceApplier
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final CollectionLogHelperConfig config;
	private final RequirementsChecker requirementsChecker;
	private final WorldMapPointManager worldMapPointManager;
	private final GuidanceOverlay guidanceOverlay;
	private final GuidanceMinimapOverlay guidanceMinimapOverlay;
	private final DialogHighlightOverlay dialogHighlightOverlay;
	private final WorldMapRouteOverlay worldMapRouteOverlay;
	private final WorldMapDestinationOverlay worldMapDestinationOverlay;

	@Inject
	OverlaySourceApplier(
		Client client,
		ClientThread clientThread,
		EventBus eventBus,
		CollectionLogHelperConfig config,
		RequirementsChecker requirementsChecker,
		WorldMapPointManager worldMapPointManager,
		GuidanceOverlay guidanceOverlay,
		GuidanceMinimapOverlay guidanceMinimapOverlay,
		DialogHighlightOverlay dialogHighlightOverlay,
		WorldMapRouteOverlay worldMapRouteOverlay,
		WorldMapDestinationOverlay worldMapDestinationOverlay)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.requirementsChecker = requirementsChecker;
		this.worldMapPointManager = worldMapPointManager;
		this.guidanceOverlay = guidanceOverlay;
		this.guidanceMinimapOverlay = guidanceMinimapOverlay;
		this.dialogHighlightOverlay = dialogHighlightOverlay;
		this.worldMapRouteOverlay = worldMapRouteOverlay;
		this.worldMapDestinationOverlay = worldMapDestinationOverlay;
	}

	/**
	 * Applies the given source's default location data to all overlays. Pure
	 * extraction of {@code GuidanceOverlayCoordinator.applySourceToOverlays} --
	 * no behavioral change. The coordinator's mutable state related to this
	 * method is carried in {@link Input} and returned in {@link Result}.
	 *
	 * @param source the active collection log source
	 * @param in coordinator-owned mutable state read by this apply pass
	 * @param hintArrowPredicate delegate to {@code shouldSetHintArrowTo} on the coordinator
	 * @param pendingShortestPathTargetSetter callback invoked on the client thread to assign
	 *                                        the coordinator's {@code pendingShortestPathTarget}
	 *                                        field (preserves original deferred-write semantics)
	 * @return updated state to be written back onto the coordinator
	 */
	Result apply(
		CollectionLogSource source,
		Input in,
		Predicate<WorldPoint> hintArrowPredicate,
		Consumer<WorldPoint> pendingShortestPathTargetSetter)
	{
		WorldPoint worldPoint = source.getWorldPoint(requirementsChecker);
		String displayName = source.getDisplayLocation(requirementsChecker);

		guidanceOverlay.setTargetPoint(worldPoint);
		guidanceOverlay.setTargetName(source.getName());
		guidanceOverlay.setLocationDescription(displayName);
		// Non-sequencer path: no step context, so no per-step item requirements.
		guidanceOverlay.setRequiredItems(Collections.emptyList());
		guidanceOverlay.setTravelTip(source.getTravelTip());
		guidanceOverlay.setTargetNpcId(source.getNpcId());
		guidanceOverlay.setInteractAction(source.getInteractAction());
		dialogHighlightOverlay.setTargetDialogOptions(source.getDialogOptions());
		dialogHighlightOverlay.setGuidanceActive(true);
		guidanceMinimapOverlay.setTargetPoint(worldPoint);
		worldMapRouteOverlay.setTargetPoint(worldPoint);
		// Non-sequencer path: no step context, use generic TILE icon.
		worldMapDestinationOverlay.setTarget(worldPoint, WorldMapDestinationOverlay.StepIconType.TILE);
		// Register a CollectionLogWorldMapPoint for click-to-focus behaviour (#429).
		// WorldMapDestinationOverlay suppresses its off-screen arrow while the map
		// point is active to avoid a duplicate edge-snap arrow (#410).
		CollectionLogWorldMapPoint updatedMapPoint = in.activeMapPoint;
		if (updatedMapPoint != null)
		{
			worldMapPointManager.remove(updatedMapPoint);
		}
		updatedMapPoint = new CollectionLogWorldMapPoint(worldPoint, displayName, in.collectionLogIcon);
		worldMapPointManager.add(updatedMapPoint);
		worldMapDestinationOverlay.setMapPointActive(true);

		final CollectionLogWorldMapPoint mapPointForReturn = updatedMapPoint;

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (hintArrowPredicate.test(worldPoint))
			{
				client.setHintArrow(worldPoint);
			}

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
				pendingShortestPathTargetSetter.accept(worldPoint);
			}
		});

		return new Result(mapPointForReturn);
	}

	/**
	 * Coordinator-owned mutable state that this applier reads but does not own.
	 */
	static final class Input
	{
		@Nullable
		final CollectionLogWorldMapPoint activeMapPoint;
		@Nullable
		final BufferedImage collectionLogIcon;

		Input(
			@Nullable CollectionLogWorldMapPoint activeMapPoint,
			@Nullable BufferedImage collectionLogIcon)
		{
			this.activeMapPoint = activeMapPoint;
			this.collectionLogIcon = collectionLogIcon;
		}
	}

	/**
	 * Updated state to be written back onto the coordinator after apply().
	 */
	static final class Result
	{
		@Nullable
		final CollectionLogWorldMapPoint activeMapPoint;

		Result(@Nullable CollectionLogWorldMapPoint activeMapPoint)
		{
			this.activeMapPoint = activeMapPoint;
		}
	}
}
