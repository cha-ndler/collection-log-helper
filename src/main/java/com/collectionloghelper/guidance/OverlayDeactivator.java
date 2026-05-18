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
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceInfoBox;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Handles the overlay deactivation cluster from {@link GuidanceOverlayCoordinator}:
 * the partial between-step {@code clearGuidanceOverlays} pass and the full
 * {@code deactivateGuidance} pass that tears down all guidance state.
 *
 * <p>Pure extraction: this class holds no mutable guidance state of its own.
 * Coordinator-owned mutable state (active world-map point, active InfoBox,
 * pending shortest-path target, panel reference) is passed in via {@link Input}
 * and returned via {@link Result} so the coordinator remains the single owner
 * of that state.</p>
 *
 * <p>Mirrors the structural pattern established by {@link OverlaySourceApplier}.</p>
 */
@Slf4j
@Singleton
public class OverlayDeactivator
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final CollectionLogHelperConfig config;
	private final WorldMapPointManager worldMapPointManager;
	private final InfoBoxManager infoBoxManager;
	private final GuidanceOverlay guidanceOverlay;
	private final GuidanceMinimapOverlay guidanceMinimapOverlay;
	private final DialogHighlightOverlay dialogHighlightOverlay;
	private final ObjectHighlightOverlay objectHighlightOverlay;
	private final ItemHighlightOverlay itemHighlightOverlay;
	private final WorldMapRouteOverlay worldMapRouteOverlay;
	private final WorldMapDestinationOverlay worldMapDestinationOverlay;
	private final GroundItemHighlightOverlay groundItemHighlightOverlay;
	private final WidgetHighlightOverlay widgetHighlightOverlay;
	private final NpcTrackerHelper npcTrackerHelper;
	private final GuidanceSequencer guidanceSequencer;

	@Inject
	OverlayDeactivator(
		Client client,
		ClientThread clientThread,
		EventBus eventBus,
		CollectionLogHelperConfig config,
		WorldMapPointManager worldMapPointManager,
		InfoBoxManager infoBoxManager,
		GuidanceOverlay guidanceOverlay,
		GuidanceMinimapOverlay guidanceMinimapOverlay,
		DialogHighlightOverlay dialogHighlightOverlay,
		ObjectHighlightOverlay objectHighlightOverlay,
		ItemHighlightOverlay itemHighlightOverlay,
		WorldMapRouteOverlay worldMapRouteOverlay,
		WorldMapDestinationOverlay worldMapDestinationOverlay,
		GroundItemHighlightOverlay groundItemHighlightOverlay,
		WidgetHighlightOverlay widgetHighlightOverlay,
		NpcTrackerHelper npcTrackerHelper,
		GuidanceSequencer guidanceSequencer)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.worldMapPointManager = worldMapPointManager;
		this.infoBoxManager = infoBoxManager;
		this.guidanceOverlay = guidanceOverlay;
		this.guidanceMinimapOverlay = guidanceMinimapOverlay;
		this.dialogHighlightOverlay = dialogHighlightOverlay;
		this.objectHighlightOverlay = objectHighlightOverlay;
		this.itemHighlightOverlay = itemHighlightOverlay;
		this.worldMapRouteOverlay = worldMapRouteOverlay;
		this.worldMapDestinationOverlay = worldMapDestinationOverlay;
		this.groundItemHighlightOverlay = groundItemHighlightOverlay;
		this.widgetHighlightOverlay = widgetHighlightOverlay;
		this.npcTrackerHelper = npcTrackerHelper;
		this.guidanceSequencer = guidanceSequencer;
	}

	/**
	 * Clears guidance overlays between sequencer steps. Pure extraction of
	 * {@code GuidanceOverlayCoordinator.clearGuidanceOverlays} -- no behavioural
	 * change. Sequencer state, InfoBox, and {@code activeTargetItemId} are
	 * preserved (only a partial reset for the step transition).
	 *
	 * @param in coordinator-owned mutable state read by this clear pass
	 * @return updated state to be written back onto the coordinator
	 */
	Result clearOverlays(Input in)
	{
		npcTrackerHelper.clear();
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		worldMapRouteOverlay.clearTarget();
		worldMapDestinationOverlay.clearTarget();
		worldMapDestinationOverlay.setMapPointActive(false);
		dialogHighlightOverlay.clear();
		objectHighlightOverlay.clearTarget();
		itemHighlightOverlay.clearTarget();
		groundItemHighlightOverlay.clearTargets();
		widgetHighlightOverlay.clearHighlights();
		clientThread.invokeLater(() -> client.clearHintArrow());
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (in.panel != null)
		{
			in.panel.hideClueGuidance();
		}
		// activeInfoBox and pendingShortestPathTarget unchanged by the partial clear.
		return new Result(null, in.activeInfoBox, in.pendingShortestPathTarget);
	}

	/**
	 * Fully deactivates guidance: clears all overlays, stops the sequencer,
	 * removes the InfoBox, posts the ShortestPath "clear" message, and resets
	 * the panel state. Pure extraction of
	 * {@code GuidanceOverlayCoordinator.deactivateGuidance} -- no behavioural
	 * change.
	 *
	 * @param in coordinator-owned mutable state read by this deactivate pass
	 * @return updated state to be written back onto the coordinator
	 */
	Result deactivate(Input in)
	{
		npcTrackerHelper.clear();
		GuidanceInfoBox infoBox = in.activeInfoBox;
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
		guidanceSequencer.stopSequence();
		guidanceOverlay.clearTarget();
		guidanceMinimapOverlay.clearTarget();
		worldMapRouteOverlay.clearTarget();
		worldMapDestinationOverlay.clearTarget();
		worldMapDestinationOverlay.setMapPointActive(false);
		dialogHighlightOverlay.clear();
		objectHighlightOverlay.clearTarget();
		itemHighlightOverlay.clearTarget();
		groundItemHighlightOverlay.clearTargets();
		widgetHighlightOverlay.clearHighlights();
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
			}
		});

		if (in.panel != null)
		{
			in.panel.hideClueGuidance();
			in.panel.hideStepProgress();
			in.panel.setGuidanceState(false, null, null);
		}

		log.debug("Guidance deactivated");
		return new Result(null, infoBox, null);
	}

	/**
	 * Coordinator-owned mutable state that this deactivator reads but does not own.
	 */
	static final class Input
	{
		@Nullable
		final GuidanceInfoBox activeInfoBox;
		@Nullable
		final WorldPoint pendingShortestPathTarget;
		@Nullable
		final CollectionLogHelperPanel panel;

		Input(
			@Nullable GuidanceInfoBox activeInfoBox,
			@Nullable WorldPoint pendingShortestPathTarget,
			@Nullable CollectionLogHelperPanel panel)
		{
			this.activeInfoBox = activeInfoBox;
			this.pendingShortestPathTarget = pendingShortestPathTarget;
			this.panel = panel;
		}
	}

	/**
	 * Updated state to be written back onto the coordinator after a clear or
	 * deactivate pass.
	 */
	static final class Result
	{
		@Nullable
		final CollectionLogWorldMapPoint activeMapPoint;
		@Nullable
		final GuidanceInfoBox activeInfoBox;
		@Nullable
		final WorldPoint pendingShortestPathTarget;

		Result(
			@Nullable CollectionLogWorldMapPoint activeMapPoint,
			@Nullable GuidanceInfoBox activeInfoBox,
			@Nullable WorldPoint pendingShortestPathTarget)
		{
			this.activeMapPoint = activeMapPoint;
			this.activeInfoBox = activeInfoBox;
			this.pendingShortestPathTarget = pendingShortestPathTarget;
		}
	}
}
