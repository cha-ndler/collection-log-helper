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
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.ItemObjectTier;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementRow;
import com.collectionloghelper.data.RequirementsChecker;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Coordinates all guidance overlay state: activating/deactivating guidance,
 * applying step data to overlays, managing the world map arrow, and tracking
 * the guidance NPC. Extracted from CollectionLogHelperPlugin to reduce its size.
 *
 * <p>Thread safety: {@link #trackedGuidanceNpc} is volatile because it is
 * written on the client thread (via NpcSpawned/NpcDespawned and scanForTrackedNpc)
 * but read on the EDT by overlay render methods.</p>
 */
@Slf4j
@Singleton
public class GuidanceOverlayCoordinator
{
	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final CollectionLogHelperConfig config;
	private final GuidanceSequencer guidanceSequencer;
	private final RequirementsChecker requirementsChecker;
	private final PlayerTravelCapabilities travelCapabilities;
	private final PlayerInventoryState playerInventoryState;
	private final ItemManager itemManager;
	private final RequiredItemResolver requiredItemResolver;
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

	// -- Guidance UI state (previously in plugin) --

	private CollectionLogWorldMapPoint activeMapPoint;
	private GuidanceInfoBox activeInfoBox;

	/**
	 * Tracked NPC for guidance overlay. Written on client thread via
	 * NpcSpawned/NpcDespawned and scanForTrackedNpc, read on EDT by
	 * overlay render. Must remain volatile.
	 */
	private volatile NPC trackedGuidanceNpc;

	/**
	 * The collection-log item ID that was active when guidance was last activated,
	 * if the player launched guidance from an item-specific context (e.g. the
	 * Item Detail view or the Top Pick button).  Null when no item target is known.
	 * Used by {@link RequiredItemResolver} to select the per-item required-item
	 * override for multi-method sources such as Shades of Mort'ton.
	 * Cleared by {@link #deactivateGuidance()}.
	 */
	@Getter
	@Nullable
	private Integer activeTargetItemId;

	/** Last guidance step index for which a worldMessage was sent (prevents spam). */
	private int lastMessagedStepIndex = -1;

	/** Pending ShortestPath target -- set after "clear", sent as "path" on the next game tick. */
	private WorldPoint pendingShortestPathTarget;

	/** Cached collection log icon for world map points. Set by plugin at startup. */
	@Setter
	private BufferedImage collectionLogIcon;

	/** Reference to the plugin instance for InfoBox construction. Set by plugin at startup. */
	@Setter
	private Plugin pluginInstance;

	/** Reference to the panel for UI updates. Set by plugin after panel creation. */
	@Setter
	private CollectionLogHelperPanel panel;

	/**
	 * Optional callback invoked after a guidance sequence completes and overlays
	 * are cleared. Used by the plugin for auto-advance and panel rebuild logic.
	 */
	@Setter
	private Runnable onSequenceCompleteCallback;

	@Inject
	GuidanceOverlayCoordinator(
		Client client,
		ClientThread clientThread,
		EventBus eventBus,
		CollectionLogHelperConfig config,
		GuidanceSequencer guidanceSequencer,
		RequirementsChecker requirementsChecker,
		PlayerTravelCapabilities travelCapabilities,
		PlayerInventoryState playerInventoryState,
		ItemManager itemManager,
		RequiredItemResolver requiredItemResolver,
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
		WidgetHighlightOverlay widgetHighlightOverlay)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.config = config;
		this.guidanceSequencer = guidanceSequencer;
		this.requirementsChecker = requirementsChecker;
		this.travelCapabilities = travelCapabilities;
		this.playerInventoryState = playerInventoryState;
		this.itemManager = itemManager;
		this.requiredItemResolver = requiredItemResolver;
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
	}

	/**
	 * Activates guidance for the given source. Sets up overlays and, for
	 * multi-step sources, starts the sequencer.
	 *
	 * @param source the collection log source to guide
	 * @param cachedPlayerLocation current player location for sequencer init
	 * @param targetItemId the collection-log item ID the player is hunting, or
	 *                     {@code null} when the caller has no specific item context.
	 *                     Stored on the coordinator so {@link RequiredItemResolver}
	 *                     can select the correct per-item consumable override.
	 */
	public void activateGuidance(CollectionLogSource source, WorldPoint cachedPlayerLocation,
		@Nullable Integer targetItemId)
	{
		// Note: deliberately NO early-return on config.showOverlays() here.
		// Show Overlays controls whether *overlays render*, not whether
		// guidance is active. Each overlay's render() method self-gates
		// on config.showOverlays() (added in cha-ndler/collection-log-helper#386),
		// so toggling the option off hides them on the next paint without
		// stopping the sequencer or the panel banner. Closes
		// cha-ndler/collection-log-helper#373.

		// Warn if the source has unmet requirements (don't block, just warn)
		List<String> unmetReqs = requirementsChecker.getUnmetRequirements(source.getName());
		if (!unmetReqs.isEmpty())
		{
			String unmetList = String.join(", ", unmetReqs);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				"[Collection Log Helper] Warning: " + source.getName() + " requires " + unmetList, "");
		}

		// Build requirement rows on the client thread (snapshot); passed to the panel header.
		final List<RequirementRow> requirementRows = requirementsChecker.buildRequirementRows(source);

		// Clear any existing guidance first, including InfoBox and sequencer.
		// deactivateGuidance() resets activeTargetItemId to null, so restore it afterwards.
		deactivateGuidance();
		this.activeTargetItemId = targetItemId;

		// If source has multi-step guidance, start the sequencer
		if (source.getGuidanceSteps() != null && !source.getGuidanceSteps().isEmpty())
		{
			guidanceSequencer.setPlayerLocation(cachedPlayerLocation);
			// startSequence runs the initial skip-chain synchronously. If all steps
			// are already satisfied, onSequenceComplete fires inside startSequence,
			// which calls deactivateGuidance() — leave panel cleared and return.
			guidanceSequencer.startSequence(source, this::onStepChanged, this::onSequenceComplete);

			if (!guidanceSequencer.isActive())
			{
				// All steps were already satisfied — sequence completed immediately.
				// onSequenceComplete already called deactivateGuidance(); nothing more to do.
				log.debug("Multi-step guidance for {} completed immediately (all steps already satisfied)",
					source.getName());
				return;
			}

			// Sequence is active: landed on a mid-sequence step after skip-chain.
			// Explicitly push the landed step to the panel to ensure the blue box is
			// rendered even when the skip-chain bypassed the normal step-transition path.
			GuidanceStep step = guidanceSequencer.getCurrentStep();
			GuidanceStep rawStep = guidanceSequencer.getRawCurrentStep();
			if (panel != null)
			{
				final int landedIdx = guidanceSequencer.getCurrentIndex() + 1;
				final int stepTotal = guidanceSequencer.getTotalSteps();
				final String stepDesc = step != null ? step.resolveDescription(activeTargetItemId) : "";
				final boolean stepManual =
					step != null && step.getCompletionCondition() == CompletionCondition.MANUAL;
				final List<GuidanceStep> sourceSteps =
					source.getGuidanceSteps() != null ? source.getGuidanceSteps() : Collections.emptyList();
				panel.setGuidanceState(true, source, requirementRows);
				// Show step text immediately without items; resolve names on the client
				// thread (~1 tick) to avoid the EDT assert in ItemManager#getItemComposition
				// (see cha-ndler/collection-log-helper#388).
				panel.updateStepProgress(landedIdx, stepTotal, stepDesc, stepManual,
					Collections.emptyList(), sourceSteps);
				final GuidanceStep rawForResolve = rawStep;
				clientThread.invokeLater(() ->
				{
					final List<RequiredItemDisplay> resolvedItems =
						requiredItemResolver.resolve(rawForResolve);
					final List<RequiredItemDisplay> resolvedRecommended =
						requiredItemResolver.resolveRecommended(rawForResolve);
					panel.updateStepProgress(landedIdx, stepTotal, stepDesc, stepManual,
						resolvedItems, resolvedRecommended, sourceSteps);
				});
			}
			// Add InfoBox showing the correct landed step (not always "1/N")
			if (!source.getItems().isEmpty() && pluginInstance != null)
			{
				BufferedImage icon = itemManager.getImage(source.getItems().get(0).getItemId());
				activeInfoBox = new GuidanceInfoBox(icon, pluginInstance);
				int landedStep = guidanceSequencer.getCurrentIndex() + 1;
				activeInfoBox.setStepText(landedStep + "/" + guidanceSequencer.getTotalSteps());
				activeInfoBox.setTooltipText(source.getName() + ": "
					+ (step != null ? step.getDescription() : ""));
				infoBoxManager.addInfoBox(activeInfoBox);
			}
			log.debug("Multi-step guidance activated for {} ({} steps, starting at step {})",
				source.getName(), source.getGuidanceSteps().size(),
				guidanceSequencer.getCurrentIndex() + 1);
			return;
		}

		if (source.getCategory() == CollectionLogCategory.CLUES)
		{
			// Clue sources: text overlay + panel banner instead of map markers
			guidanceOverlay.setClueGuidanceText("Do " + source.getName());
			// Clue-only sources never carry per-step required items.
			guidanceOverlay.setRequiredItems(Collections.emptyList());
			if (panel != null)
			{
				panel.showClueGuidance(source);
			}

			// Client API calls and ShortestPath messages must run on the client thread
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
				}
			});
		}
		else
		{
			// Non-clue sources: world map, tile, and minimap overlays
			applySourceToOverlays(source);
		}

		// Always update panel guidance state from the plugin
		if (panel != null)
		{
			panel.setGuidanceState(true, source, requirementRows);
			panel.hideStepProgress();
		}

		log.debug("Guidance activated for {} ({})", source.getName(), source.getCategory());
	}

	/**
	 * Deactivates all guidance, clearing overlays, sequencer, and InfoBox.
	 */
	public void deactivateGuidance()
	{
		activeTargetItemId = null;
		lastMessagedStepIndex = -1;
		trackedGuidanceNpc = null;
		if (activeInfoBox != null)
		{
			infoBoxManager.removeInfoBox(activeInfoBox);
			activeInfoBox = null;
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
		activeMapPoint = null;
		pendingShortestPathTarget = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
			}
		});

		if (panel != null)
		{
			panel.hideClueGuidance();
			panel.hideStepProgress();
			panel.setGuidanceState(false, null, null);
		}

		log.debug("Guidance deactivated");
	}

	/**
	 * Called from the plugin's onGameTick. Handles world map arrow rotation
	 * and dispatches the deferred ShortestPath "path" message.
	 */
	public void tick()
	{
		// Dispatch deferred ShortestPath "path" message (1-tick after "clear")
		if (pendingShortestPathTarget != null)
		{
			WorldPoint target = pendingShortestPathTarget;
			pendingShortestPathTarget = null;
			Map<String, Object> data = new HashMap<>();
			Player lp = client.getLocalPlayer();
			if (lp != null)
			{
				data.put("start", lp.getWorldLocation());
			}
			data.put("target", target);
			eventBus.post(new PluginMessage("shortestpath", "path", data));
		}

		// World map arrow rotation
		updateWorldMapArrow();
	}

	/**
	 * Handles an NPC spawn event. If guidance is active and the NPC matches
	 * the current step's target, begins tracking it.
	 */
	public void onNpcSpawned(NPC npc)
	{
		if (guidanceSequencer.isActive() && trackedGuidanceNpc == null)
		{
			GuidanceStep step = guidanceSequencer.getRawCurrentStep();
			if (step != null && step.resolveNpcId(activeTargetItemId) > 0
				&& npc.getId() == step.resolveNpcId(activeTargetItemId))
			{
				trackedGuidanceNpc = npc;
				guidanceOverlay.setTrackedNpc(npc);
			}
		}
	}

	/**
	 * Handles an NPC despawn event. Clears tracking if the despawned NPC
	 * was the currently tracked guidance NPC.
	 */
	public void onNpcDespawned(NPC npc)
	{
		if (npc == trackedGuidanceNpc)
		{
			trackedGuidanceNpc = null;
			guidanceOverlay.setTrackedNpc(null);
		}
	}

	/**
	 * Triggers dynamic overlay updates when inventory changes while guidance is active.
	 */
	public void onItemContainerChanged()
	{
		if (guidanceSequencer.isActive())
		{
			applyDynamicItemObjectOverlays();
		}
	}

	/**
	 * Returns true if guidance is currently active for the given source.
	 */
	public boolean isSourceGuided(CollectionLogSource source)
	{
		return guidanceSequencer.isActive() && source.equals(guidanceSequencer.getActiveSource());
	}

	/**
	 * Rebuilds the cached set of source names that have at least one unobtained item.
	 */
	public Set<String> rebuildSourcesWithMissingItems(DropRateDatabase database,
		PlayerCollectionState collectionState)
	{
		Set<String> missing = new HashSet<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					missing.add(source.getName());
					break;
				}
			}
		}
		return missing;
	}

	// -- Private methods --

	/**
	 * Applies a single guidance step's data to all overlays.
	 */
	private void applyStepToOverlays(GuidanceStep step, String sourceName, CollectionLogSource source)
	{
		// Send world message hint if this step has one (only once per step, not on re-notify)
		int stepIndex = guidanceSequencer.getCurrentIndex();
		if (step.getWorldMessage() != null && !step.getWorldMessage().isEmpty()
			&& stepIndex != lastMessagedStepIndex)
		{
			lastMessagedStepIndex = stepIndex;
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
		objectHighlightOverlay.setTooltipText(step.resolveDescription(activeTargetItemId));

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
			guidanceOverlay.setLocationDescription(step.resolveDescription(activeTargetItemId));
			String rawTravelTip = step.getTravelTip();
			if ((rawTravelTip == null || rawTravelTip.isEmpty()) && source != null)
			{
				rawTravelTip = source.getTravelTip();
			}
			String travelTip = travelCapabilities.selectBestTravelTip(rawTravelTip);
			guidanceOverlay.setTravelTip(travelTip);
			log.debug("Travel capabilities for step '{}': {}", step.getDescription(), travelCapabilities.getSummary());
			guidanceOverlay.setTargetNpcId(step.resolveNpcId(activeTargetItemId));
			guidanceOverlay.setInteractAction(step.getInteractAction());
			dialogHighlightOverlay.setTargetDialogOptions(step.getDialogOptions());
			dialogHighlightOverlay.setGuidanceActive(true);
			guidanceMinimapOverlay.setTargetPoint(worldPoint);
			worldMapRouteOverlay.setTargetPoint(worldPoint);
			worldMapDestinationOverlay.setTarget(worldPoint, resolveStepIconType(step));
			// Register a CollectionLogWorldMapPoint for click-to-focus behaviour
			// (setJumpOnClick). WorldMapDestinationOverlay draws the on-screen icon
			// but does not receive click events — only a WorldMapPoint does (#429).
			// To avoid a double edge-snap arrow (#410), WorldMapDestinationOverlay
			// suppresses its own off-screen arrow while the map point is active
			// (see WorldMapDestinationOverlay.setMapPointActive).
			if (activeMapPoint != null)
			{
				worldMapPointManager.remove(activeMapPoint);
			}
			activeMapPoint = new CollectionLogWorldMapPoint(worldPoint,
				step.resolveDescription(activeTargetItemId), collectionLogIcon);
			worldMapPointManager.add(activeMapPoint);
			worldMapDestinationOverlay.setMapPointActive(true);

			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();

				// Skip hint arrow inside Sailing instances -- surface world
				// coords render at wrong positions in instanced WorldViews
				if (shouldSetHintArrowTo(worldPoint))
				{
					client.setHintArrow(worldPoint);
				}

				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
					pendingShortestPathTarget = worldPoint;
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
			if (activeMapPoint != null)
			{
				worldMapPointManager.remove(activeMapPoint);
				activeMapPoint = null;
			}
			guidanceOverlay.setClueGuidanceText(step.resolveDescription(activeTargetItemId));
			clientThread.invokeLater(() ->
			{
				client.clearHintArrow();
				if (config.useShortestPath())
				{
					eventBus.post(new PluginMessage("shortestpath", "clear"));
				}
			});
		}

		// Dynamic overlay override for Shades of Mort'ton key/chest highlighting
		applyDynamicItemObjectOverlays();
	}

	/**
	 * Dynamically overrides overlays when the current step has dynamicItemObjectTiers.
	 * Scans inventory for the lowest tier with matching items, then highlights
	 * that tier's objects and the matching item. Called on step change and on
	 * inventory change while a guidance sequence is active.
	 */
	private void applyDynamicItemObjectOverlays()
	{
		GuidanceStep step = guidanceSequencer.getRawCurrentStep();
		if (step == null || step.getDynamicItemObjectTiers() == null
			|| step.getDynamicItemObjectTiers().isEmpty())
		{
			return;
		}

		// Collect ALL matching tiers so multiple keys highlight multiple chests
		Set<Integer> matchedObjectIds = new HashSet<>();
		List<Integer> matchedItemIds = new ArrayList<>();
		String tooltipText = null;
		String action = null;

		for (ItemObjectTier tier : step.getDynamicItemObjectTiers())
		{
			if (tier.getItemIds() == null)
			{
				continue;
			}
			for (int itemId : tier.getItemIds())
			{
				if (playerInventoryState.hasItem(itemId))
				{
					if (tier.getObjectIds() != null && !tier.getObjectIds().isEmpty())
					{
						matchedObjectIds.addAll(tier.getObjectIds());
					}
					matchedItemIds.add(itemId);
					if (action == null)
					{
						action = tier.getInteractAction() != null
							? tier.getInteractAction()
							: step.getObjectInteractAction();
					}
					if (tooltipText == null)
					{
						tooltipText = tier.getName() != null
							? (action + " " + tier.getName())
							: step.getDescription();
					}
					break; // Only match first key per tier (avoid duplicates)
				}
			}
		}

		if (!matchedObjectIds.isEmpty())
		{
			objectHighlightOverlay.setTargetObjectIds(matchedObjectIds);
			objectHighlightOverlay.setObjectInteractAction(action);
			objectHighlightOverlay.setTooltipText(
				matchedItemIds.size() > 1 ? step.getDescription() : tooltipText);
			itemHighlightOverlay.setTargetItemIds(matchedItemIds);
		}
	}

	/**
	 * Applies a source's default location data to overlays (non-sequencer path).
	 */
	private void applySourceToOverlays(CollectionLogSource source)
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
		if (activeMapPoint != null)
		{
			worldMapPointManager.remove(activeMapPoint);
		}
		activeMapPoint = new CollectionLogWorldMapPoint(worldPoint, displayName, collectionLogIcon);
		worldMapPointManager.add(activeMapPoint);
		worldMapDestinationOverlay.setMapPointActive(true);

		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (shouldSetHintArrowTo(worldPoint))
			{
				client.setHintArrow(worldPoint);
			}

			if (config.useShortestPath())
			{
				eventBus.post(new PluginMessage("shortestpath", "clear"));
				pendingShortestPathTarget = worldPoint;
			}
		});
	}

	private void clearGuidanceOverlays()
	{
		trackedGuidanceNpc = null;
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
		activeMapPoint = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (panel != null)
		{
			panel.hideClueGuidance();
		}
	}

	/**
	 * Scans currently loaded NPCs to find a match for the given step's target NPC ID.
	 * Called once when a new step activates to seed the tracked NPC reference.
	 */
	private void scanForTrackedNpc(GuidanceStep step)
	{
		trackedGuidanceNpc = null;
		guidanceOverlay.setTrackedNpc(null);

		final int resolvedNpcId = step == null ? 0 : step.resolveNpcId(activeTargetItemId);
		if (resolvedNpcId <= 0)
		{
			return;
		}

		final int targetNpcId = resolvedNpcId;
		clientThread.invokeLater(() ->
		{
			WorldView wv = client.getTopLevelWorldView();
			if (wv == null)
			{
				return;
			}
			for (NPC npc : wv.npcs())
			{
				if (npc != null && npc.getId() == targetNpcId)
				{
					trackedGuidanceNpc = npc;
					guidanceOverlay.setTrackedNpc(npc);
					break;
				}
			}
		});
	}

	/**
	 * Callback from GuidanceSequencer when the current step changes.
	 */
	private void onStepChanged(GuidanceStep step)
	{
		clearGuidanceOverlays();
		CollectionLogSource activeSource = guidanceSequencer.getActiveSource();
		String sourceName = activeSource != null ? activeSource.getName() : "";
		applyStepToOverlays(step, sourceName, activeSource);
		scanForTrackedNpc(step);

		// Update InfoBox progress
		if (activeInfoBox != null)
		{
			int current = guidanceSequencer.getCurrentIndex() + 1;
			int total = guidanceSequencer.getTotalSteps();
			activeInfoBox.setStepText(current + "/" + total);
			String tooltip = step.getDescription();
			if (guidanceSequencer.getCumulativeTrackThreshold() > 0)
			{
				tooltip += "\n" + guidanceSequencer.getCumulativeActionCount()
					+ "/" + guidanceSequencer.getCumulativeTrackThreshold()
					+ " actions tracked";
			}
			activeInfoBox.setTooltipText(tooltip);
			if (current == total)
			{
				activeInfoBox.setTextColor(java.awt.Color.GREEN);
			}
		}

		if (panel != null)
		{
			final int current = guidanceSequencer.getCurrentIndex() + 1;
			final int total = guidanceSequencer.getTotalSteps();
			final String desc = step.resolveDescription(activeTargetItemId);
			final boolean isManual = step.getCompletionCondition() == CompletionCondition.MANUAL;
			final GuidanceStep rawStep = guidanceSequencer.getRawCurrentStep();
			final CollectionLogSource stepChangeSource = guidanceSequencer.getActiveSource();
			final List<GuidanceStep> sourceSteps = stepChangeSource != null
				&& stepChangeSource.getGuidanceSteps() != null
				? stepChangeSource.getGuidanceSteps() : Collections.emptyList();

			// Push description + step progress to the panel immediately (safe from any thread
			// because showStep dispatches to the EDT). Required-item resolution is deferred
			// to the client thread because RequiredItemResolver.resolve() calls
			// ItemManager.getItemComposition, which asserts caller-is-client-thread. See
			// cha-ndler/collection-log-helper#388 for the original trace.
			panel.updateStepProgress(current, total, desc, isManual,
				Collections.emptyList(), Collections.emptyList(), sourceSteps);
			clientThread.invokeLater(() ->
			{
				final List<RequiredItemDisplay> resolvedItems =
					requiredItemResolver.resolve(rawStep);
				final List<RequiredItemDisplay> resolvedRecommended =
					requiredItemResolver.resolveRecommended(rawStep);
				panel.updateStepProgress(current, total, desc, isManual,
					resolvedItems, resolvedRecommended, sourceSteps);
			});
		}
	}

	/**
	 * Callback from GuidanceSequencer when the entire sequence is complete.
	 * Deactivates guidance overlays, then notifies the plugin via the
	 * optional onSequenceCompleteCallback for auto-advance and rebuild logic.
	 */
	private void onSequenceComplete()
	{
		String sourceName = guidanceSequencer.getActiveSource() != null
			? guidanceSequencer.getActiveSource().getName() : "unknown";
		deactivateGuidance();
		log.debug("Guidance sequence complete for {}", sourceName);
		if (onSequenceCompleteCallback != null)
		{
			onSequenceCompleteCallback.run();
		}
	}

	private void updateWorldMapArrow()
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
		// World map Y is inverted (higher Y = further north = up on map)
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
	 * {@code config.showHintArrow()}. Called by the config-change handler when
	 * "Show Hint Arrow" is toggled mid-guidance so the change takes effect
	 * immediately rather than at the next step transition.
	 *
	 * <p>When the toggle goes OFF (or guidance is inactive), the arrow is
	 * cleared. When the toggle goes ON and a step with a world target is
	 * active, the arrow is re-set at that target.
	 *
	 * <p>All client mutations are dispatched on the client thread.
	 */
	public void refreshHintArrow()
	{
		clientThread.invokeLater(() ->
		{
			client.clearHintArrow();

			if (!guidanceSequencer.isActive())
			{
				return;
			}

			GuidanceStep step = guidanceSequencer.getCurrentStep();
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
	 * Snapshots getLocalPlayer() once to avoid TOCTOU races. Skips hint arrows
	 * inside non-top-level WorldViews (e.g. sailing boats).
	 */
	private boolean shouldSetHintArrowTo(WorldPoint worldPoint)
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
	private WorldMapDestinationOverlay.StepIconType resolveStepIconType(GuidanceStep step)
	{
		if (step.resolveNpcId(activeTargetItemId) > 0)
		{
			return WorldMapDestinationOverlay.StepIconType.NPC;
		}
		if (step.getObjectId() > 0 || (step.getObjectIds() != null && !step.getObjectIds().isEmpty()))
		{
			return WorldMapDestinationOverlay.StepIconType.OBJECT;
		}
		return WorldMapDestinationOverlay.StepIconType.TILE;
	}
}
