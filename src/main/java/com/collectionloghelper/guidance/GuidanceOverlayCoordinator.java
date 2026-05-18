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
 * <p>Thread safety: the tracked guidance NPC reference is owned by
 * {@link NpcTrackerHelper}, which marks it volatile because it is written
 * on the client thread (via NpcSpawned/NpcDespawned and scanForTrackedNpc)
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
	private final OverlayStepApplier overlayStepApplier;
	private final OverlaySourceApplier overlaySourceApplier;
	private final WorldMapController worldMapController;
	private final DynamicTargetManager dynamicTargetManager;
	private final NpcTrackerHelper npcTrackerHelper;

	// -- Guidance UI state (previously in plugin) --

	private CollectionLogWorldMapPoint activeMapPoint;
	private GuidanceInfoBox activeInfoBox;

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
		WidgetHighlightOverlay widgetHighlightOverlay,
		OverlayStepApplier overlayStepApplier,
		OverlaySourceApplier overlaySourceApplier,
		WorldMapController worldMapController,
		DynamicTargetManager dynamicTargetManager,
		NpcTrackerHelper npcTrackerHelper)
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
		this.overlayStepApplier = overlayStepApplier;
		this.overlaySourceApplier = overlaySourceApplier;
		this.worldMapController = worldMapController;
		this.dynamicTargetManager = dynamicTargetManager;
		this.npcTrackerHelper = npcTrackerHelper;
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
		npcTrackerHelper.clear();
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
	 * Called from the plugin's onGameTick. Handles world map arrow rotation,
	 * dispatches the deferred ShortestPath "path" message, and updates
	 * dynamic target overlays for steps that carry a
	 * {@link com.collectionloghelper.data.GuidanceStep#getDynamicTargetEvaluator()} key.
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

		// Dynamic target evaluator dispatch (delegated to DynamicTargetManager).
		activeMapPoint = dynamicTargetManager.tick(new DynamicTargetManager.Input(
			guidanceSequencer.isActive(), guidanceSequencer.getRawCurrentStep(),
			activeMapPoint, activeTargetItemId, collectionLogIcon)).getActiveMapPoint();

		// World map arrow rotation
		worldMapController.updateArrow(activeMapPoint);
	}

	/**
	 * Handles an NPC spawn event by delegating to {@link NpcTrackerHelper},
	 * passing the current sequencer / step / target-item context.
	 */
	public void onNpcSpawned(NPC npc)
	{
		npcTrackerHelper.onNpcSpawned(npc, guidanceSequencer.isActive(),
			guidanceSequencer.getRawCurrentStep(), activeTargetItemId);
	}

	/**
	 * Handles an NPC despawn event by delegating to {@link NpcTrackerHelper}.
	 */
	public void onNpcDespawned(NPC npc)
	{
		npcTrackerHelper.onNpcDespawned(npc);
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

	/** Applies a step to all overlays via {@link OverlayStepApplier}, then runs the dynamic override. */
	private void applyStepToOverlays(GuidanceStep step, String sourceName, CollectionLogSource source)
	{
		OverlayStepApplier.Result result = overlayStepApplier.apply(step, sourceName, source,
			new OverlayStepApplier.Input(activeTargetItemId, lastMessagedStepIndex, activeMapPoint, collectionLogIcon),
			step1 -> worldMapController.resolveStepIconType(step1, activeTargetItemId),
			worldMapController::shouldSetHintArrowTo,
			wp -> pendingShortestPathTarget = wp);
		lastMessagedStepIndex = result.lastMessagedStepIndex;
		activeMapPoint = result.activeMapPoint;
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
	 *
	 * <p>Delegates to {@link OverlaySourceApplier} to keep this coordinator
	 * focused on lifecycle/state ownership. The applier holds no mutable state
	 * of its own; the {@code activeMapPoint} and {@code pendingShortestPathTarget}
	 * fields stay owned here and are threaded through {@link OverlaySourceApplier.Input}
	 * / {@link OverlaySourceApplier.Result}.</p>
	 */
	private void applySourceToOverlays(CollectionLogSource source)
	{
		OverlaySourceApplier.Result result = overlaySourceApplier.apply(source,
			new OverlaySourceApplier.Input(activeMapPoint, collectionLogIcon),
			worldMapController::shouldSetHintArrowTo,
			wp -> pendingShortestPathTarget = wp);
		activeMapPoint = result.activeMapPoint;
	}

	private void clearGuidanceOverlays()
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
		activeMapPoint = null;
		worldMapPointManager.removeIf(CollectionLogWorldMapPoint.class::isInstance);
		if (panel != null)
		{
			panel.hideClueGuidance();
		}
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
		npcTrackerHelper.scanForTrackedNpc(step, activeTargetItemId);

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

	/**
	 * Re-applies the hint arrow against the current guidance step, respecting
	 * {@code config.showHintArrow()}. Called by the config-change handler when
	 * "Show Hint Arrow" is toggled mid-guidance so the change takes effect
	 * immediately rather than at the next step transition.
	 *
	 * <p>Delegates to {@link WorldMapController#refreshHintArrow(boolean, GuidanceStep)},
	 * supplying the current sequencer activity flag and current step.</p>
	 */
	public void refreshHintArrow()
	{
		worldMapController.refreshHintArrow(guidanceSequencer.isActive(), guidanceSequencer.getCurrentStep());
	}

}
