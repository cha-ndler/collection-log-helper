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
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Applies a single {@link GuidanceStep}'s data to all guidance overlays.
 *
 * <p>Pure extraction from {@link GuidanceOverlayCoordinator}: this class holds
 * no mutable state of its own. Mutable coordinator state that the step
 * application touches (last messaged step index, active world map point,
 * pending shortest-path target) is passed in via {@link Input} and returned
 * via {@link Result} so the coordinator remains the single owner of that
 * state.</p>
 */
@Slf4j
@Singleton
public class OverlayStepApplier
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final CollectionLogHelperConfig config;
	private final GuidanceSequencer guidanceSequencer;
	private final PlayerTravelCapabilities travelCapabilities;
	private final RequiredItemResolver requiredItemResolver;
	private final WorldMapPointManager worldMapPointManager;
	private final GuidanceOverlay guidanceOverlay;
	private final GuidanceMinimapOverlay guidanceMinimapOverlay;
	private final DialogHighlightOverlay dialogHighlightOverlay;
	private final ObjectHighlightOverlay objectHighlightOverlay;
	private final ItemHighlightOverlay itemHighlightOverlay;
	private final WorldMapRouteOverlay worldMapRouteOverlay;
	private final WorldMapDestinationOverlay worldMapDestinationOverlay;
	private final GroundItemHighlightOverlay groundItemHighlightOverlay;
	private final WidgetHighlightOverlay widgetHighlightOverlay;

	@Inject
	OverlayStepApplier(
		Client client,
		ClientThread clientThread,
		EventBus eventBus,
		CollectionLogHelperConfig config,
		GuidanceSequencer guidanceSequencer,
		PlayerTravelCapabilities travelCapabilities,
		RequiredItemResolver requiredItemResolver,
		WorldMapPointManager worldMapPointManager,
		GuidanceOverlay guidanceOverlay,
		GuidanceMinimapOverlay guidanceMinimapOverlay,
		DialogHighlightOverlay dialogHighlightOverlay,
		ObjectHighlightOverlay objectHighlightOverlay,
		ItemHighlightOverlay itemHighlightOverlay,
		WorldMapRouteOverlay worldMapRouteOverlay,
		WorldMapDestinationOverlay worldMapDestinationOverlay,
		GroundItemHighlightOverlay groundItemHighlightOverlay,
		WidgetHighlightOverlay widgetHighlightOverlay)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.guidanceSequencer = guidanceSequencer;
		this.travelCapabilities = travelCapabilities;
		this.requiredItemResolver = requiredItemResolver;
		this.worldMapPointManager = worldMapPointManager;
		this.guidanceOverlay = guidanceOverlay;
		this.guidanceMinimapOverlay = guidanceMinimapOverlay;
		this.dialogHighlightOverlay = dialogHighlightOverlay;
		this.objectHighlightOverlay = objectHighlightOverlay;
		this.itemHighlightOverlay = itemHighlightOverlay;
		this.worldMapRouteOverlay = worldMapRouteOverlay;
		this.worldMapDestinationOverlay = worldMapDestinationOverlay;
		this.groundItemHighlightOverlay = groundItemHighlightOverlay;
		this.widgetHighlightOverlay = widgetHighlightOverlay;
	}

	/**
	 * Applies the given step's data to all overlays. Pure extraction of
	 * {@code GuidanceOverlayCoordinator.applyStepToOverlays} -- no behavioral
	 * change. The coordinator's mutable state related to this method is
	 * carried in {@link Input} and returned in {@link Result}.
	 *
	 * @param step the step to render
	 * @param sourceName user-facing source label
	 * @param source the active collection log source (may be null for non-sequencer paths)
	 * @param in coordinator-owned mutable state read by this apply pass
	 * @param iconTypeResolver delegate to {@code resolveStepIconType} on the coordinator
	 * @param hintArrowPredicate delegate to {@code shouldSetHintArrowTo} on the coordinator
	 * @param pendingShortestPathTargetSetter callback invoked on the client thread to assign
	 *                                        the coordinator's {@code pendingShortestPathTarget}
	 *                                        field (matches original deferred-write semantics)
	 * @return updated state to be written back onto the coordinator
	 */
	Result apply(
		GuidanceStep step,
		String sourceName,
		@Nullable CollectionLogSource source,
		Input in,
		Function<GuidanceStep, WorldMapDestinationOverlay.StepIconType> iconTypeResolver,
		Predicate<WorldPoint> hintArrowPredicate,
		Consumer<WorldPoint> pendingShortestPathTargetSetter)
	{
		int updatedLastMessagedStepIndex = in.lastMessagedStepIndex;
		CollectionLogWorldMapPoint updatedMapPoint = in.activeMapPoint;

		// Send world message hint if this step has one (only once per step, not on re-notify)
		int stepIndex = guidanceSequencer.getCurrentIndex();
		if (step.getWorldMessage() != null && !step.getWorldMessage().isEmpty()
			&& stepIndex != updatedLastMessagedStepIndex)
		{
			updatedLastMessagedStepIndex = stepIndex;
			clientThread.invokeLater(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=00c8c8>[Collection Log Helper]</col> " + step.getWorldMessage(),
					null));
		}

		// Set tile filter before target IDs so it's active during rescan
		if (step.getObjectFilterTiles() != null && !step.getObjectFilterTiles().isEmpty())
		{
			List<WorldPoint> tiles = new ArrayList<>();
			for (int[] t : step.getObjectFilterTiles())
			{
				if (t != null && t.length >= 3)
				{
					tiles.add(new WorldPoint(t[0], t[1], t[2]));
				}
			}
			objectHighlightOverlay.setObjectFilterTiles(tiles);
		}
		else if (step.getObjectMaxDistance() > 0 && step.getWorldX() > 0)
		{
			objectHighlightOverlay.setObjectFilter(
				new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane()),
				step.getObjectMaxDistance());
		}
		else
		{
			objectHighlightOverlay.setObjectFilter(null, 0);
		}
		objectHighlightOverlay.setTargetObjectIds(step.getAllObjectIds());
		objectHighlightOverlay.setObjectInteractAction(step.getObjectInteractAction());
		objectHighlightOverlay.setUseItemOnObject(step.isUseItemOnObject());
		objectHighlightOverlay.setTooltipText(step.resolveDescription(in.activeTargetItemId));

		itemHighlightOverlay.setTargetItemIds(step.getHighlightItemIds());

		groundItemHighlightOverlay.setTargetGroundItemIds(
			step.getGroundItemIds() != null ? new HashSet<>(step.getGroundItemIds()) : null);

		if (step.getHighlightWidgetIds() != null && step.getHighlightWidgetIds().length > 0)
		{
			List<int[]> widgets = new ArrayList<>();
			for (int[] ref : step.getHighlightWidgetIds())
			{
				if (ref != null && ref.length >= 2)
				{
					widgets.add(ref);
				}
			}
			widgetHighlightOverlay.setHighlightWidgets(widgets);
		}
		else
		{
			widgetHighlightOverlay.clearHighlights();
		}

		// Suppress tile highlight when ObjectHighlightOverlay handles the visual
		guidanceOverlay.setSuppressTileHighlight(!step.getAllObjectIds().isEmpty());

		// Resolve required-item availability for this step once and push to the
		// overlay; the render loop must never touch inventory/bank state.
		//
		// MUST run on the client thread: RequiredItemResolver.resolve() calls
		// ItemManager.getItemComposition(int), which asserts caller-is-client-
		// thread. Calling from the EDT (which is where activateGuidance lives
		// when triggered by a panel button click) throws AssertionError, aborts
		// the rest of activateGuidance mid-flight, and leaves the panel state
		// out of sync with the overlay state (overlay set, panel button stays
		// "Guide Me"). See cha-ndler/collection-log-helper#388 for the trace
		// that surfaced this. The deferral is cheap (~1 tick at worst) and
		// idempotent against rapid step changes because each call wins.
		final GuidanceStep stepForResolve = step;
		clientThread.invokeLater(() ->
			guidanceOverlay.setRequiredItems(requiredItemResolver.resolve(stepForResolve)));

		if (step.getWorldX() > 0)
		{
			WorldPoint worldPoint = new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			guidanceOverlay.setTargetPoint(worldPoint);
			guidanceOverlay.setTargetName(sourceName);
			guidanceOverlay.setLocationDescription(step.resolveDescription(in.activeTargetItemId));
			String rawTravelTip = step.getTravelTip();
			if ((rawTravelTip == null || rawTravelTip.isEmpty()) && source != null)
			{
				rawTravelTip = source.getTravelTip();
			}
			String travelTip = travelCapabilities.selectBestTravelTip(rawTravelTip);
			guidanceOverlay.setTravelTip(travelTip);
			log.debug("Travel capabilities for step '{}': {}", step.getDescription(), travelCapabilities.getSummary());
			guidanceOverlay.setTargetNpcId(step.resolveNpcId(in.activeTargetItemId));
			guidanceOverlay.setInteractAction(step.getInteractAction());
			dialogHighlightOverlay.setTargetDialogOptions(step.getDialogOptions());
			dialogHighlightOverlay.setGuidanceActive(true);
			guidanceMinimapOverlay.setTargetPoint(worldPoint);
			worldMapRouteOverlay.setTargetPoint(worldPoint);
			worldMapDestinationOverlay.setTarget(worldPoint, iconTypeResolver.apply(step));
			// Register a CollectionLogWorldMapPoint for click-to-focus behaviour
			// (setJumpOnClick). WorldMapDestinationOverlay draws the on-screen icon
			// but does not receive click events -- only a WorldMapPoint does (#429).
			// To avoid a double edge-snap arrow (#410), WorldMapDestinationOverlay
			// suppresses its own off-screen arrow while the map point is active
			// (see WorldMapDestinationOverlay.setMapPointActive).
			if (updatedMapPoint != null)
			{
				worldMapPointManager.remove(updatedMapPoint);
			}
			updatedMapPoint = new CollectionLogWorldMapPoint(worldPoint,
				step.resolveDescription(in.activeTargetItemId), in.collectionLogIcon);
			worldMapPointManager.add(updatedMapPoint);
			worldMapDestinationOverlay.setMapPointActive(true);

			final WorldPoint hintTarget = worldPoint;
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				// Skip hint arrow inside Sailing instances -- surface world
				// coords render at wrong positions in instanced WorldViews
				if (hintArrowPredicate.test(hintTarget))
				{
					client.setHintArrow(hintTarget);
				}

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
					pendingShortestPathTargetSetter.accept(hintTarget);
				}
			});
		}
		else
		{
			// Step with no location -- clear previous target and show text overlay only.
			// Dialog options are still applied here: a step may be a pure dialog-choice
			// step with no world coordinates (e.g. "Choose the 'Yes' option").
			dialogHighlightOverlay.setTargetDialogOptions(step.getDialogOptions());
			dialogHighlightOverlay.setGuidanceActive(true);
			guidanceOverlay.setTargetPoint(null);
			guidanceOverlay.setTargetName(null);
			guidanceOverlay.setTargetNpcId(0);
			guidanceOverlay.setInteractAction(null);
			guidanceOverlay.setLocationDescription(null);
			guidanceOverlay.setTravelTip(null);
			guidanceMinimapOverlay.setTargetPoint(null);
			worldMapRouteOverlay.setTargetPoint(null);
			worldMapDestinationOverlay.clearTarget();
			worldMapDestinationOverlay.setMapPointActive(false);
			if (updatedMapPoint != null)
			{
				worldMapPointManager.remove(updatedMapPoint);
				updatedMapPoint = null;
			}
			guidanceOverlay.setClueGuidanceText(step.resolveDescription(in.activeTargetItemId));
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();
				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
				}
			});
		}

		return new Result(updatedLastMessagedStepIndex, updatedMapPoint);
	}

	/**
	 * Snapshot of the coordinator-owned state this applier reads.
	 */
	static final class Input
	{
		@Nullable
		final Integer activeTargetItemId;
		final int lastMessagedStepIndex;
		@Nullable
		final CollectionLogWorldMapPoint activeMapPoint;
		@Nullable
		final BufferedImage collectionLogIcon;

		Input(
			@Nullable Integer activeTargetItemId,
			int lastMessagedStepIndex,
			@Nullable CollectionLogWorldMapPoint activeMapPoint,
			@Nullable BufferedImage collectionLogIcon)
		{
			this.activeTargetItemId = activeTargetItemId;
			this.lastMessagedStepIndex = lastMessagedStepIndex;
			this.activeMapPoint = activeMapPoint;
			this.collectionLogIcon = collectionLogIcon;
		}
	}

	/**
	 * Updated state the coordinator must write back onto itself after an apply pass.
	 */
	static final class Result
	{
		final int lastMessagedStepIndex;
		@Nullable
		final CollectionLogWorldMapPoint activeMapPoint;

		Result(
			int lastMessagedStepIndex,
			@Nullable CollectionLogWorldMapPoint activeMapPoint)
		{
			this.lastMessagedStepIndex = lastMessagedStepIndex;
			this.activeMapPoint = activeMapPoint;
		}
	}
}
