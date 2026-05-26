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
	private final OverlayDeactivator overlayDeactivator;
	private final WorldMapController worldMapController;
	private final DynamicTargetManager dynamicTargetManager;
	private final NpcTrackerHelper npcTrackerHelper;
	private final StepChangeHandler stepChangeHandler;
	private final DynamicItemObjectTierResolver dynamicItemObjectTierResolver;

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

	/**
	 * Identity of the step whose data was last applied to the in-game overlays by
	 * {@link #onStepChanged(GuidanceStep)}, or {@code -1} / {@code null} before the
	 * first application. Used to distinguish a genuine step CHANGE from a re-notify
	 * of the SAME step (#683).
	 *
	 * <p>{@code GuidanceSequencer.onInventoryChanged} re-notifies the current
	 * (unchanged) step on every {@code ItemContainerChanged} while guidance is
	 * active - effectively every action during activities like Shades of Mort'ton.
	 * Each re-notify reaches {@link #onStepChanged(GuidanceStep)}, which previously
	 * tore down ({@link #clearGuidanceOverlays()}) and rebuilt
	 * ({@link #applyStepToOverlays(GuidanceStep, String, CollectionLogSource)}) the
	 * in-game blue box / highlights every time, producing a visible blink.</p>
	 *
	 * <p>This mirrors the side-panel idempotency guards added in #681
	 * ({@code StepProgressView.lastRenderValid} + {@code StepChangeHandler.lastShownStepIndex}):
	 * track the last-applied identity and early-return when an incoming re-notify
	 * matches. Identity is the sequencer step index PLUS the resolved
	 * {@link GuidanceStep} reference - the index catches ordinary re-notifies and the
	 * step reference catches a same-index conditional-alternative swap (resolved steps
	 * are cached per index by the sequencer, so reference identity is stable and a
	 * genuine swap yields a different object). Reset to the sentinel by
	 * {@link #deactivateGuidance()} so re-activating any source re-applies on the
	 * first step.</p>
	 *
	 * <p>Read/written only on the path that fires {@code onStepChanged} (the client
	 * thread that drives the sequencer), so no synchronization is needed.</p>
	 */
	private int lastAppliedOverlayStepIndex = -1;
	@Nullable
	private GuidanceStep lastAppliedOverlayStep;

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
		OverlayDeactivator overlayDeactivator,
		WorldMapController worldMapController,
		DynamicTargetManager dynamicTargetManager,
		NpcTrackerHelper npcTrackerHelper,
		StepChangeHandler stepChangeHandler,
		DynamicItemObjectTierResolver dynamicItemObjectTierResolver)
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
		this.overlayDeactivator = overlayDeactivator;
		this.worldMapController = worldMapController;
		this.dynamicTargetManager = dynamicTargetManager;
		this.npcTrackerHelper = npcTrackerHelper;
		this.stepChangeHandler = stepChangeHandler;
		this.dynamicItemObjectTierResolver = dynamicItemObjectTierResolver;
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
		// Reset the #683 overlay idempotency identity so re-activating any source
		// (including the same one) re-applies overlays on its first step instead of
		// being suppressed as a "same step" re-notify.
		lastAppliedOverlayStepIndex = -1;
		lastAppliedOverlayStep = null;
		OverlayDeactivator.Result result = overlayDeactivator.deactivate(
			new OverlayDeactivator.Input(activeInfoBox, pendingShortestPathTarget, panel));
		activeMapPoint = result.activeMapPoint;
		activeInfoBox = result.activeInfoBox;
		pendingShortestPathTarget = result.pendingShortestPathTarget;
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
		// The active-guidance guard is held here (#527) so inactive ticks
		// skip the call entirely and allocate nothing on the hot path.
		if (guidanceSequencer.isActive())
		{
			CollectionLogWorldMapPoint newMapPoint = dynamicTargetManager.tick(
				guidanceSequencer.getRawCurrentStep(),
				activeMapPoint, activeTargetItemId, collectionLogIcon);
			if (newMapPoint != null)
			{
				activeMapPoint = newMapPoint;
			}
		}

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
	 * Delegates the tier-scan / inventory-match to
	 * {@link DynamicItemObjectTierResolver}; the coordinator only owns the overlay
	 * setter calls (we hold the overlay collaborators). The resolver returns
	 * {@link DynamicItemObjectTierResolver.Result#EMPTY} as a cached singleton on
	 * the no-match path, so this method allocates nothing per overlay refresh tick
	 * when no tier matches.
	 *
	 * <p>Called on step change ({@link #applyStepToOverlays}) and on inventory
	 * change while a guidance sequence is active
	 * ({@link #onItemContainerChanged()}).</p>
	 */
	private void applyDynamicItemObjectOverlays()
	{
		DynamicItemObjectTierResolver.Result result =
			dynamicItemObjectTierResolver.resolve(guidanceSequencer.getRawCurrentStep());
		if (result.hasMatch())
		{
			objectHighlightOverlay.setTargetObjectIds(result.getObjectIds());
			objectHighlightOverlay.setObjectInteractAction(result.getAction());
			objectHighlightOverlay.setTooltipText(result.getTooltipText());
			itemHighlightOverlay.setTargetItemIds(result.getItemIds());
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
		OverlayDeactivator.Result result = overlayDeactivator.clearOverlays(
			new OverlayDeactivator.Input(activeInfoBox, pendingShortestPathTarget, panel));
		activeMapPoint = result.activeMapPoint;
		activeInfoBox = result.activeInfoBox;
		pendingShortestPathTarget = result.pendingShortestPathTarget;
	}

	/**
	 * Callback from GuidanceSequencer when the current step changes (or is re-notified).
	 * On a genuine step change, performs the overlay clear + apply + NPC-tracker scan
	 * locally (those paths mutate coordinator-owned state such as
	 * {@code lastMessagedStepIndex} and {@code activeMapPoint}). On a same-step re-notify
	 * (fired by {@code GuidanceSequencer.onInventoryChanged} on every inventory change)
	 * the clear + re-apply is skipped to avoid an overlay blink (#683). The InfoBox/panel
	 * progress refresh is always delegated to {@link StepChangeHandler}, whose own #681
	 * guards keep an unchanged re-notify a no-op.
	 */
	private void onStepChanged(GuidanceStep step)
	{
		// Same-step idempotency guard (#683), mirroring the side-panel guards from #681
		// (StepProgressView.lastRenderValid / StepChangeHandler.lastShownStepIndex).
		// onInventoryChanged re-notifies the current (unchanged) step on every inventory
		// change while guidance is active; without this guard each re-notify clears and
		// re-applies the in-game overlays, producing a visible blink. Identity is the
		// sequencer step index plus the resolved step reference (catches a same-index
		// conditional-alternative swap). When both match the last application this is a
		// redundant re-notify, so skip the clear + re-apply entirely.
		//
		// The StepChangeHandler delegation below is NOT skipped: its own #681 guards make
		// an unchanged re-notify a no-op while still letting genuine item-availability
		// changes update the panel in place.
		final int currentIndex = guidanceSequencer.getCurrentIndex();
		final boolean sameStepRenotify = lastAppliedOverlayStepIndex == currentIndex
			&& lastAppliedOverlayStep == step;
		if (!sameStepRenotify)
		{
			lastAppliedOverlayStepIndex = currentIndex;
			lastAppliedOverlayStep = step;
			clearGuidanceOverlays();
			CollectionLogSource applySource = guidanceSequencer.getActiveSource();
			String applySourceName = applySource != null ? applySource.getName() : "";
			applyStepToOverlays(step, applySourceName, applySource);
			npcTrackerHelper.scanForTrackedNpc(step, activeTargetItemId);
		}

		stepChangeHandler.handle(step,
			new StepChangeHandler.Input(activeInfoBox, panel, activeTargetItemId));
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
