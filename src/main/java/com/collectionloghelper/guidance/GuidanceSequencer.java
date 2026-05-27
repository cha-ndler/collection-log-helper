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

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.StepWaypoint;
import com.collectionloghelper.data.Zone;
import com.collectionloghelper.guidance.bosses.BossGuidance;
import com.collectionloghelper.guidance.bosses.BossGuidanceRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Core sequencer for multi-step guidance sequences. Manages the current step index,
 * evaluates completion conditions, and notifies listeners when steps change or complete.
 */
@Slf4j
@Singleton
public class GuidanceSequencer
{
	private final PlayerInventoryState inventoryState;
	private final RequirementsChecker requirementsChecker;
	private final BossGuidanceRegistry bossRegistry;
	private final CompletionChecker completionChecker;
	private final StepAdvancer stepAdvancer;
	private final SequencerEventAdapter eventAdapter;

	private volatile WorldPoint lastKnownPlayerLocation;
	private volatile CollectionLogSource activeSource;
	private volatile List<GuidanceStep> steps;
	private volatile int currentIndex;
	private volatile boolean active;
	private volatile int loopIterationsCompleted;
	private volatile int cumulativeActionCount;

	/**
	 * Number of waypoints the player has already crossed for the current step (B2).
	 * Resets to 0 whenever the active step changes or the sequence restarts.
	 * Only meaningful when the current step has a non-empty {@code waypoints} list.
	 */
	private volatile int crossedWaypointIndex;

	/** Cached compiled pattern for the current step's chat completion regex. */
	private CompletionChecker.ChatPatternCache chatPatternCache;
	/** Cached WorldPoint for the current step's target location. */
	private CompletionChecker.TilePointCache tilePointCache;

	/** Cache of resolved alternatives keyed by step index. Cleared when a new sequence starts. */
	private final Map<Integer, GuidanceStep> resolvedAlternatives = new HashMap<>();

	private Consumer<GuidanceStep> onStepChanged;
	private Runnable onSequenceComplete;

	/**
	 * Set to {@code true} when {@link #onItemObtained(int)} detects that the
	 * obtained item belongs to the active source's target item list.  Reset to
	 * {@code false} each time a new sequence starts.  Callers (e.g.,
	 * {@code CollectionStateChangeHandler}) read this via
	 * {@link #wasTargetSlotUnlocked()} to decide whether the sequence completing
	 * means the collection-log slot actually changed.
	 */
	private volatile boolean targetSlotUnlocked;

	/**
	 * Supplies the active guidance target collection-log item id, used to resolve
	 * {@code perItemRequiredItemIds} overrides during depletion / restock detection.
	 * Wired via {@link #setActiveTargetSupplier} after construction to avoid a
	 * circular Guice injection (the coordinator already injects this sequencer).
	 * Null-safe: when unset, depletion logic falls back to the step's static
	 * {@code requiredItemIds}.
	 */
	private java.util.function.Supplier<Integer> activeTargetItemIdSupplier;

	/**
	 * True while guidance is parked on a restock step after the active looping step
	 * ran out of fuel ({@code restockIfMissingAllItemIds}, #719). While latched,
	 * {@link #onPlayerMoved} does not auto-advance (so a location-gated bank step
	 * does not immediately complete by arrival). Cleared once the player holds all
	 * of the restock step's required items, or when the sequence stops / restarts.
	 */
	private volatile boolean awaitingRestock;

	/** Index (0-based) of the restock step guidance was reset to; valid only while {@link #awaitingRestock}. */
	private volatile int restockStepIndex;

	@Inject
	private GuidanceSequencer(PlayerInventoryState inventoryState, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker, BossGuidanceRegistry bossRegistry)
	{
		this.inventoryState = inventoryState;
		this.requirementsChecker = requirementsChecker;
		this.bossRegistry = bossRegistry;
		this.completionChecker = new CompletionChecker(inventoryState, collectionState);
		this.stepAdvancer = new StepAdvancer(this.completionChecker);
		this.eventAdapter = new SequencerEventAdapter(this.completionChecker);
	}

	/**
	 * Sets the player's current location for ARRIVE_AT_TILE pre-checks.
	 * Should be called before startSequence().
	 */
	public void setPlayerLocation(WorldPoint location)
	{
		this.lastKnownPlayerLocation = location;
	}

	/**
	 * Wires the active-target-item-id supplier used by depletion / restock
	 * detection to resolve {@code perItemRequiredItemIds} overrides. Must be called
	 * once during plugin startup, after both the coordinator and this sequencer are
	 * constructed by Guice (mirrors {@code RequiredItemResolver.setCoordinator}).
	 */
	public void setActiveTargetSupplier(java.util.function.Supplier<Integer> supplier)
	{
		this.activeTargetItemIdSupplier = supplier;
	}

	/**
	 * Starts a guidance sequence for the given source.
	 * Loads steps, sets index to 0, and skips any already-satisfied steps.
	 */
	public void startSequence(CollectionLogSource source, Consumer<GuidanceStep> stepChanged,
		Runnable sequenceComplete)
	{
		this.activeSource = source;
		// Route through Java boss guidance when the source opts in, else fall back to JSON steps
		BossGuidance boss = bossRegistry != null
			? bossRegistry.get(source.getGuidanceHelperKey())
			: null;
		if (boss != null)
		{
			log.debug("Using Java boss guidance '{}' for source {}", source.getGuidanceHelperKey(), source.getName());
			this.steps = boss.getSteps(null, null);
		}
		else
		{
			this.steps = source.getGuidanceSteps();
		}
		this.currentIndex = 0;
		this.loopIterationsCompleted = 0;
		this.cumulativeActionCount = 0;
		this.crossedWaypointIndex = 0;
		this.resolvedAlternatives.clear();
		this.onStepChanged = stepChanged;
		this.onSequenceComplete = sequenceComplete;
		this.targetSlotUnlocked = false;
		this.awaitingRestock = false;
		this.restockStepIndex = 0;
		this.active = true;

		// Skip any steps whose conditions are already met
		skipSatisfiedSteps();

		// Mid-activity re-sync (#719): if the player is already standing in a
		// later step's confirmable area AND holds that step's required items,
		// jump them forward to it instead of starting at step 1.
		advanceToFurthestSatisfiedState();

		// Mid-activity activation while already depleted (e.g. in the Shades
		// catacombs with no keys and no remains) — park on the restock step.
		checkDepletionAndMaybeReset();

		if (active)
		{
			GuidanceStep step = getCurrentStep();
			if (step != null)
			{
				log.info("Guidance sequence started for {} — step 1/{}: {}",
					source.getName(), steps.size(), step.getDescription());
				notifyStepChanged(step);
			}
		}
	}

	/**
	 * Stops the current sequence and clears all state.
	 */
	public void stopSequence()
	{
		active = false;
		activeSource = null;
		steps = null;
		currentIndex = 0;
		crossedWaypointIndex = 0;
		awaitingRestock = false;
		restockStepIndex = 0;
		resolvedAlternatives.clear();
		onStepChanged = null;
		onSequenceComplete = null;
	}

	/**
	 * Returns the current guidance step, or null if no sequence is active.
	 * If the step has conditional alternatives, returns the resolved version.
	 *
	 * <p>Missing {@code requiredItemIds} no longer trigger a synthetic bank
	 * routing step. Required items are surfaced informationally via the panel
	 * and in-game overlay (with green / yellow / red availability borders);
	 * the player chooses when and where to bank.
	 */
	public GuidanceStep getCurrentStep()
	{
		if (!active || steps == null || currentIndex >= steps.size())
		{
			return null;
		}
		return resolveStep(currentIndex);
	}

	/**
	 * Returns the raw current step without bank routing substitution,
	 * but with conditional alternatives resolved.
	 */
	public GuidanceStep getRawCurrentStep()
	{
		return getCurrentStep();
	}

	/**
	 * Resolves conditional alternatives for the step at the given index.
	 * The result is cached so resolution only happens once per step activation,
	 * not on every tick.
	 */
	private GuidanceStep resolveStep(int index)
	{
		GuidanceStep step = steps.get(index);
		if (step.getConditionalAlternatives() == null || step.getConditionalAlternatives().isEmpty())
		{
			return step;
		}

		return resolvedAlternatives.computeIfAbsent(index, i ->
		{
			GuidanceStep resolved = step.resolveAlternative(requirementsChecker);
			if (resolved != step)
			{
				log.info("Step {} resolved conditional alternative: {}", i + 1, resolved.getDescription());
			}
			return resolved;
		});
	}

	public int getCurrentIndex()
	{
		return currentIndex;
	}

	public int getTotalSteps()
	{
		return steps != null ? steps.size() : 0;
	}

	public boolean isActive()
	{
		return active;
	}

	public CollectionLogSource getActiveSource()
	{
		return activeSource;
	}

	public int getLoopIterationsCompleted()
	{
		return loopIterationsCompleted;
	}

	/**
	 * Replaces the step-changed callback without restarting the sequence.
	 * Used by tests to observe step-change notifications after
	 * {@link #startSequence} has already been called.
	 */
	public void setOnStepChanged(Consumer<GuidanceStep> callback)
	{
		this.onStepChanged = callback;
	}

	/**
	 * Forces the guidance sequence back to step 1 (index 0) WITHOUT running the
	 * skip-chain. Used by the Reset button to let the user replay the full
	 * sequence from the beginning even if their current state already satisfies
	 * earlier steps.
	 *
	 * <p>The sequence remains active and all callbacks are preserved. Loop
	 * counters and resolved-alternative caches are cleared so the sequence
	 * behaves as if it were freshly started at step 0.
	 *
	 * <p>No-op if the sequencer is not currently active.
	 */
	public void restartFromStep0()
	{
		if (!active || steps == null)
		{
			return;
		}

		currentIndex = 0;
		loopIterationsCompleted = 0;
		cumulativeActionCount = 0;
		crossedWaypointIndex = 0;
		awaitingRestock = false;
		restockStepIndex = 0;
		resolvedAlternatives.clear();
		// Do NOT call skipSatisfiedSteps() — Reset forces step 0 unconditionally.

		GuidanceStep step = getCurrentStep();
		if (step != null)
		{
			log.info("Guidance sequence reset to step 1/{} for {}",
				steps.size(), activeSource != null ? activeSource.getName() : "?");
			notifyStepChanged(step);
		}
	}

	/**
	 * Re-evaluates the activation skip-chain from step 0 against current player
	 * state and jumps to the first unsatisfied step. Used by the Sync button to
	 * let the user catch up after picking up items or travelling while guidance
	 * was already active.
	 *
	 * <p>Unlike Reset, Sync does NOT change {@link #active}, does NOT clear
	 * callbacks, and does NOT stop/restart the underlying sequence — it only
	 * moves the index forward (or backward, if step-satisfaction
	 * semantics allow) to the correct position.
	 *
	 * <p>Always fires {@link #notifyStepChanged} so the panel and overlays
	 * refresh even if the index did not change.
	 *
	 * <p>No-op if the sequencer is not currently active.
	 */
	public void syncToCurrentState()
	{
		if (!active || steps == null)
		{
			return;
		}

		currentIndex = 0;
		loopIterationsCompleted = 0;
		crossedWaypointIndex = 0;
		awaitingRestock = false;
		restockStepIndex = 0;
		resolvedAlternatives.clear();
		// Re-run the skip-chain to find the first unsatisfied step
		skipSatisfiedSteps();

		if (!active)
		{
			// All steps already satisfied — sequence completed via skipSatisfiedSteps
			log.info("Sync completed sequence immediately (all steps already satisfied) for {}",
				activeSource != null ? activeSource.getName() : "?");
			return;
		}

		// State-derived forward jump (#719): land on the furthest step whose
		// area + required items the player demonstrably satisfies.
		advanceToFurthestSatisfiedState();

		// If the player synced while depleted, park on the restock step.
		checkDepletionAndMaybeReset();

		GuidanceStep step = getCurrentStep();
		if (step != null)
		{
			log.info("Guidance synced to step {}/{} for {}",
				currentIndex + 1, steps.size(), activeSource != null ? activeSource.getName() : "?");
			notifyStepChanged(step);
		}
	}

	public int getCumulativeActionCount()
	{
		return cumulativeActionCount;
	}

	/**
	 * Returns the number of waypoints crossed so far in the current step (B2).
	 * Only meaningful when the active step has a non-empty {@code waypoints} list.
	 * Resets to 0 whenever the sequencer advances, restarts, or stops.
	 */
	public int getCrossedWaypointIndex()
	{
		return crossedWaypointIndex;
	}

	/**
	 * Returns the cumulative track threshold from the active source, or 0 if none.
	 */
	public int getCumulativeTrackThreshold()
	{
		return activeSource != null ? activeSource.getCumulativeTrackThreshold() : 0;
	}

	/**
	 * Called when the player performs a tracked use-item-on-object action
	 * (e.g., emptying a bucket of water into the Trouble Brewing hopper).
	 * Increments the cumulative counter and force-completes any active loop
	 * when the threshold is reached.
	 */
	public void onTrackedAction()
	{
		if (!active || activeSource == null)
		{
			return;
		}
		int threshold = activeSource.getCumulativeTrackThreshold();
		if (threshold <= 0)
		{
			return;
		}
		cumulativeActionCount++;
		log.debug("Cumulative action {}/{}", cumulativeActionCount, threshold);
		if (cumulativeActionCount >= threshold)
		{
			log.info("Cumulative threshold reached ({}/{}), completing loop", cumulativeActionCount, threshold);
			// Force-complete any active loop and advance past it
			for (int i = currentIndex; i < steps.size(); i++)
			{
				GuidanceStep s = steps.get(i);
				if (s.getLoopCount() > 0)
				{
					loopIterationsCompleted = s.getLoopCount();
					currentIndex = i;
					advanceStep();
					return;
				}
			}
		}
	}

	/**
	 * Called when a collection log item is obtained. Checks ITEM_OBTAINED condition
	 * and sets {@link #targetSlotUnlocked} if the item belongs to the active source.
	 *
	 * <p>When the obtained item is one of the active source's target collection-log
	 * slots, the guidance for that source is logically finished regardless of the
	 * current step / inventory state (e.g., the player still holds a chest key) — so
	 * the sequence is completed immediately. This routes through the normal
	 * {@code onSequenceComplete} callback, which deactivates guidance and lets the
	 * auto-advance logic roll to the next Top Pick (the target-slot-unlocked flag is
	 * already set, so {@link #wasTargetSlotUnlocked()} reports {@code true}). Without
	 * this, guidance could stay pinned on a completed item because the final step's
	 * own completion condition (inventory / chat) had not yet fired (#708).
	 *
	 * <p>The auto-complete shortcut only applies to single-pass guidance. When the
	 * current step is mid-loop (it declares a loop-back and the loop is not yet
	 * exhausted, e.g. Shades of Mort'ton cycling chest opens), obtaining a target
	 * item only records the unlock flag and falls through to the normal
	 * step-completion / loop-back path — completing the sequence here would kill the
	 * loop and leave stale highlights (#715).
	 */
	public void onItemObtained(int itemId)
	{
		if (!active)
		{
			return;
		}

		// Flag if the obtained item is one of this source's target collection-log slots.
		if (!targetSlotUnlocked && activeSource != null && activeSource.getItems() != null)
		{
			for (com.collectionloghelper.data.CollectionLogItem item : activeSource.getItems())
			{
				if (item.getItemId() == itemId)
				{
					targetSlotUnlocked = true;
					if (isCurrentStepMidLoop())
					{
						// Looping source mid-loop: do NOT complete the sequence. Record the
						// unlock and let the normal step-completion / loop-back logic continue
						// cycling the loop.
						log.info("Target slot unlocked for {} (itemId={}) mid-loop — keeping guidance active",
							activeSource.getName(), itemId);
						break;
					}
					log.info("Target slot unlocked for {} (itemId={}) — completing guidance",
						activeSource.getName(), itemId);
					// The guided collection-log item was obtained: the sequence is done,
					// even if the final step's completion condition has not fired yet.
					fireSequenceComplete();
					return;
				}
			}
		}

		GuidanceStep step = getRawCurrentStep();
		if (completionChecker.isItemObtainedSatisfying(step, itemId))
		{
			log.info("Step {} complete (ITEM_OBTAINED: {})", currentIndex + 1, itemId);
			advanceStep();
		}
	}

	/**
	 * Returns {@code true} if any of the active source's collection-log target
	 * items were obtained since the current sequence started.  Used by
	 * {@code CollectionStateChangeHandler} to gate auto-advance: only advance
	 * to the next source when the slot actually unlocked, not merely because the
	 * final guidance step completed.
	 *
	 * <p>Returns {@code false} after {@link #startSequence} until
	 * {@link #onItemObtained} confirms a target item was received.</p>
	 */
	public boolean wasTargetSlotUnlocked()
	{
		return targetSlotUnlocked;
	}

	/**
	 * Called when inventory changes. Checks INVENTORY_HAS_ITEM condition and
	 * re-evaluates bank routing for the current step.
	 */
	public void onInventoryChanged()
	{
		if (!active)
		{
			return;
		}

		// Restock latch (#719): while parked on a restock step, ignore other
		// inventory-driven advances until the player has re-acquired all of that
		// step's required items, then resume normal guidance.
		if (awaitingRestock)
		{
			if (playerHasAllRequiredItems(getRawCurrentStep()))
			{
				awaitingRestock = false;
				log.info("Restock complete at step {} — resuming guidance for {}",
					restockStepIndex + 1, activeSource != null ? activeSource.getName() : "?");
				GuidanceStep resumed = getCurrentStep();
				if (resumed != null)
				{
					notifyStepChanged(resumed);
				}
			}
			return;
		}

		GuidanceStep step = getRawCurrentStep();

		// Auto-advance if skipIfHasAnyItemIds is now satisfied (e.g., key just entered inventory).
		//
		// Exception (#707): in a looping / restock activity the player must keep
		// gathering many of a recurring item (e.g. several Shade keys per catacomb
		// run) before moving on. Auto-advancing off the gather step after the FIRST
		// pickup clears its in-game highlight prematurely, so the player loses sight
		// of the remaining keys to collect. While the sequence is a looping/restock
		// activity, suppress the skip auto-advance so the gather step — and its
		// highlight — stays active until the player advances manually (Next/Skip) or
		// the loop naturally re-enters it.
		CompletionChecker.SkipIfHasAnyResult skip = completionChecker.evaluateSkipIfHasAny(step);
		if (skip.matched() && !isRecurringGatherSequence())
		{
			log.info("Step {} auto-advancing (skipIfHasAnyItemIds satisfied: item {})",
				currentIndex + 1, skip.matchedItemId());
			advanceStep();
			return;
		}

		if (completionChecker.isInventoryHasItemSatisfying(step))
		{
			log.info("Step {} complete (INVENTORY_HAS_ITEM: {} x{})",
				currentIndex + 1, step.getCompletionItemId(), step.getCompletionItemCount());
			advanceStep();
			return;
		}

		if (completionChecker.isInventoryNotHasItemSatisfying(step))
		{
			log.info("Step {} complete (INVENTORY_NOT_HAS_ITEM: {})",
				currentIndex + 1, step.getCompletionItemId());
			advanceStep();
			return;
		}

		// Loop-fuel depletion (#719): if the active looping step just ran dry,
		// park on the restock step instead of staying stuck on a MANUAL loop step.
		if (checkDepletionAndMaybeReset())
		{
			GuidanceStep restockStep = getCurrentStep();
			if (restockStep != null)
			{
				notifyStepChanged(restockStep);
			}
			return;
		}

		// Re-notify step changed in case bank routing status changed
		GuidanceStep current = getCurrentStep();
		if (current != null)
		{
			notifyStepChanged(current);
		}
	}

	/**
	 * Called each game tick with the player's current position. Checks ARRIVE_AT_TILE condition.
	 */
	public void onPlayerMoved(WorldPoint playerLocation)
	{
		if (!active || playerLocation == null)
		{
			return;
		}

		// While parked on a restock step (#719), suppress location-based
		// auto-advance so a bank step gated by ARRIVE_AT_TILE / ARRIVE_AT_ZONE
		// does not immediately complete just because the player is standing
		// there. The latch clears via onInventoryChanged once the player has
		// restocked the step's required items.
		if (awaitingRestock)
		{
			return;
		}

		GuidanceStep step = getRawCurrentStep();
		if (step == null)
		{
			return;
		}

		if (step.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
			&& step.getWorldX() > 0)
		{
			// B2 — when the step declares an ordered waypoint list, require each waypoint to be
			// crossed in order before treating the step as complete.
			List<StepWaypoint> stepWaypoints = step.getWaypoints();
			if (stepWaypoints != null && !stepWaypoints.isEmpty())
			{
				CompletionChecker.WaypointProgressResult wp = completionChecker.evaluateWaypointProgress(
					step, currentIndex + 1, playerLocation, crossedWaypointIndex);
				crossedWaypointIndex = wp.crossedWaypointIndex();
				if (wp.satisfied())
				{
					log.info("Step {} complete (ARRIVE_AT_TILE via waypoint sequence: all {} waypoints crossed)",
						currentIndex + 1, stepWaypoints.size());
					advanceStep();
				}
				// Waypoint-mode: do not fall through to the legacy single-tile check.
				return;
			}

			// Legacy single-tile arrival check (no waypoints declared).
			CompletionChecker.ArriveAtTileResult tileResult =
				completionChecker.evaluateArriveAtTile(step, currentIndex, playerLocation, tilePointCache);
			tilePointCache = tileResult.cache();
			if (tileResult.satisfied())
			{
				log.info("Step {} complete (ARRIVE_AT_TILE: within {} tiles, plane {})",
					currentIndex + 1, step.getCompletionDistance(), step.getWorldPlane());
				advanceStep();
			}
		}

		if (completionChecker.isArriveAtZoneSatisfying(step, playerLocation))
		{
			Zone zone = step.getZone();
			log.info("Step {} complete (ARRIVE_AT_ZONE: player in zone [{},{} - {},{}] plane {})",
				currentIndex + 1, zone.getMinX(), zone.getMinY(), zone.getMaxX(), zone.getMaxY(), zone.getPlane());
			advanceStep();
		}

		if (completionChecker.isPlayerOnPlaneSatisfying(step, playerLocation))
		{
			log.info("Step {} complete (PLAYER_ON_PLANE: plane {})",
				currentIndex + 1, step.getWorldPlane());
			advanceStep();
		}
	}

	/**
	 * Called when an NPC dies. Checks ACTOR_DEATH condition.
	 */
	public void onNpcDeath(int npcId)
	{
		if (!active)
		{
			return;
		}
		applyEventOutcome(eventAdapter.onNpcDeath(getRawCurrentStep(), currentIndex + 1, npcId));
	}

	/**
	 * Called when a chat message is received. Checks CHAT_MESSAGE_RECEIVED condition using regex.
	 */
	public void onChatMessage(String message)
	{
		if (!active)
		{
			return;
		}
		SequencerEventAdapter.EventOutcome outcome =
			eventAdapter.onChatMessage(getRawCurrentStep(), currentIndex + 1, message, chatPatternCache);
		chatPatternCache = outcome.chatCache();
		applyEventOutcome(outcome);
	}

	/**
	 * Called when the player interacts with an NPC. Checks NPC_TALKED_TO condition.
	 */
	public void onNpcInteracted(int npcId)
	{
		if (!active)
		{
			return;
		}
		applyEventOutcome(eventAdapter.onNpcInteracted(getRawCurrentStep(), currentIndex + 1, npcId));
	}

	/**
	 * Called when a varbit changes. Checks VARBIT_AT_LEAST condition.
	 */
	public void onVarbitChanged(int varbitId, int value)
	{
		if (!active)
		{
			return;
		}
		applyEventOutcome(eventAdapter.onVarbitChanged(getRawCurrentStep(), currentIndex + 1, varbitId, value));
	}

	/**
	 * Logs the adapter-supplied completion line and advances to the next step
	 * when the event outcome reports the current step is satisfied. No-op
	 * when the outcome is not satisfied.
	 */
	private void applyEventOutcome(SequencerEventAdapter.EventOutcome outcome)
	{
		if (!outcome.satisfied())
		{
			return;
		}
		log.info("{}", outcome.completionLogMessage());
		advanceStep();
	}

	/**
	 * Unconditionally skips the current step, bypassing any active loop.
	 * Used by the panel's Skip button to let users move past steps they
	 * want to handle manually or that are stuck.
	 */
	public void skipStep()
	{
		applyTransition(stepAdvancer.skip(buildTransitionInput()),
			"Skipped to", "Guidance sequence complete (skipped)");
	}

	/**
	 * Advances to the next step, respecting loop conditions.
	 * Used for automatic completion and the Next Step button.
	 */
	public void advanceStep()
	{
		applyTransition(stepAdvancer.advance(buildTransitionInput()),
			"Advanced to", "Guidance sequence complete");
	}

	/**
	 * Skips steps whose completion conditions are already satisfied. Used by
	 * {@link #startSequence} and {@link #syncToCurrentState}; the result is
	 * applied directly with no transition log line — the caller emits its own
	 * step-started trace.
	 */
	private void skipSatisfiedSteps()
	{
		StepAdvancer.SkipChainResult result = stepAdvancer.runSkipChain(
			new StepAdvancer.SkipChainInput(currentIndex, steps, active, lastKnownPlayerLocation));
		currentIndex = result.newIndex();
		if (result.sequenceComplete())
		{
			log.info("All steps already satisfied for {}", activeSource != null ? activeSource.getName() : "?");
			fireSequenceComplete();
		}
	}

	private StepAdvancer.TransitionInput buildTransitionInput()
	{
		return new StepAdvancer.TransitionInput(currentIndex, loopIterationsCompleted, steps, active,
			lastKnownPlayerLocation, getRawCurrentStep());
	}

	/**
	 * Applies a {@link StepAdvancer.TransitionOutcome}: writes the new index
	 * and loop counter, fires the appropriate log line, and either
	 * {@code notifyStepChanged} or {@code onSequenceComplete}.
	 */
	private void applyTransition(StepAdvancer.TransitionOutcome outcome, String advancedLabel, String completeLabel)
	{
		if (outcome.noOp())
		{
			return;
		}
		final List<GuidanceStep> currentSteps = this.steps;
		currentIndex = outcome.newIndex();
		loopIterationsCompleted = outcome.newLoopIterationsCompleted();
		crossedWaypointIndex = 0;
		if (outcome.sequenceComplete())
		{
			log.info("{} for {}", completeLabel, activeSource != null ? activeSource.getName() : "?");
			fireSequenceComplete();
			return;
		}
		GuidanceStep step = getCurrentStep();
		if (step != null)
		{
			String label = outcome.loopBackResume() ? "Resumed at" : advancedLabel;
			log.info("{} step {}/{}: {}", label, currentIndex + 1, currentSteps.size(), step.getDescription());
			notifyStepChanged(step);
		}
	}

	/**
	 * Returns {@code true} when the current step is part of an active, not-yet-exhausted
	 * loop. A step loops when it declares both {@code loopBackToStep > 0} and
	 * {@code loopCount > 0}; the loop is still active while more iterations remain.
	 *
	 * <p>The exhaustion test mirrors {@link StepAdvancer#advance}: completing the step
	 * increments the iteration counter, and the loop only continues while
	 * {@code loopIterationsCompleted + 1 < loopCount}. On the final iteration this
	 * returns {@code false}, so a genuinely-last loop pass still allows the sequence to
	 * complete normally.
	 */
	private boolean isCurrentStepMidLoop()
	{
		GuidanceStep step = getRawCurrentStep();
		return step != null
			&& step.getLoopBackToStep() > 0
			&& step.getLoopCount() > 0
			&& loopIterationsCompleted + 1 < step.getLoopCount();
	}

	/**
	 * Returns {@code true} when the active sequence is a looping / restock activity
	 * — i.e. any step declares loop-fuel via {@code restockIfMissingAllItemIds}
	 * (#719). Such sequences expect the player to keep gathering many of a recurring
	 * consumable, so the gather step's {@code skipIfHasAnyItemIds} auto-advance is
	 * suppressed to keep that step's highlight active past the first pickup (#707).
	 *
	 * <p>Reuses the existing restock-fuel signal rather than introducing a new
	 * schema field: a sequence only carries that field when it is built around a
	 * consume-many loop.
	 */
	private boolean isRecurringGatherSequence()
	{
		if (steps == null)
		{
			return false;
		}
		for (GuidanceStep step : steps)
		{
			if (step != null && !step.getRestockIfMissingAllItemIds().isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Loop-fuel depletion check (#719). When the current step declares
	 * {@code restockIfMissingAllItemIds}, the player holds NONE of those consumables,
	 * and the target collection-log slot is not yet obtained, the loop has run dry.
	 * Parks guidance on the earliest step whose required items the player is missing
	 * (the restock / bank step) and latches {@link #awaitingRestock}.
	 *
	 * <p>Pure state mutation — does NOT notify listeners. Callers that act on a
	 * {@code true} return fire their own {@link #notifyStepChanged}. Returns
	 * {@code false} (no-op) when not depleted, when no restock target can be found,
	 * or when already awaiting restock.
	 */
	private boolean checkDepletionAndMaybeReset()
	{
		if (!active || awaitingRestock || targetSlotUnlocked)
		{
			return false;
		}
		GuidanceStep step = getRawCurrentStep();
		if (step == null)
		{
			return false;
		}
		List<Integer> fuel = step.getRestockIfMissingAllItemIds();
		if (fuel.isEmpty() || !playerHasNoneOf(fuel))
		{
			return false;
		}
		int restockIdx = findEarliestStepMissingRequiredItems();
		if (restockIdx < 0)
		{
			// Nothing to restock (player still holds every required item) — leave
			// the player on the current step rather than forcing a pointless reset.
			return false;
		}
		log.info("Loop depleted at step {}/{} for {} — parking on restock step {}",
			currentIndex + 1, steps.size(),
			activeSource != null ? activeSource.getName() : "?", restockIdx + 1);
		currentIndex = restockIdx;
		restockStepIndex = restockIdx;
		loopIterationsCompleted = 0;
		crossedWaypointIndex = 0;
		awaitingRestock = true;
		return true;
	}

	/**
	 * Returns the index of the earliest step whose effective required items the
	 * player is currently missing at least one of, or -1 when no such step exists.
	 * Used as the restock target for {@link #checkDepletionAndMaybeReset}.
	 */
	private int findEarliestStepMissingRequiredItems()
	{
		if (steps == null)
		{
			return -1;
		}
		for (int i = 0; i < steps.size(); i++)
		{
			List<Integer> required = effectiveRequiredItemIds(steps.get(i));
			if (required == null || required.isEmpty())
			{
				continue;
			}
			for (Integer id : required)
			{
				if (id != null && id > 0 && !playerHolds(id))
				{
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Mid-activity state-derivation (#719). Scans forward from the current index
	 * for the FURTHEST-along step whose <em>area is positively confirmed</em>
	 * (player within the step's {@code completionDistance} of its
	 * {@code worldX/worldY/worldPlane}, or inside its {@code completionZone}) AND
	 * whose effective required items the player fully holds. When such a step is
	 * found, jumps {@link #currentIndex} to it so an activation mid-activity lands
	 * the player where they actually are (e.g. already in the Shades catacomb
	 * holding keys → the open-chests step) instead of step 1.
	 *
	 * <p>Conservative by design: a step whose location cannot be confirmed (no
	 * coordinates and no zone, or the player's last-known location is unknown) is
	 * never treated as a forward target — wrong-forward placement is worse than
	 * starting earlier. No-op when nothing later qualifies, so it composes cleanly
	 * with the skip-chain (already run) and the depletion/restock check (run
	 * after). Pure index mutation — does not notify listeners.
	 */
	private void advanceToFurthestSatisfiedState()
	{
		if (!active || steps == null || lastKnownPlayerLocation == null)
		{
			return;
		}
		int furthest = -1;
		for (int i = currentIndex + 1; i < steps.size(); i++)
		{
			GuidanceStep step = steps.get(i);
			if (playerIsConfirmablyInStepArea(step) && playerHasAllRequiredItems(step))
			{
				furthest = i;
			}
		}
		if (furthest > currentIndex)
		{
			log.info("State-derived start: player is in step {}/{}'s area holding its items — "
				+ "starting there instead of step {}", furthest + 1, steps.size(), currentIndex + 1);
			currentIndex = furthest;
			loopIterationsCompleted = 0;
			crossedWaypointIndex = 0;
		}
	}

	/**
	 * Returns true only when the player's last-known location can be POSITIVELY
	 * confirmed to be inside the given step's area — either within the step's
	 * {@code completionDistance} of its {@code worldX/worldY/worldPlane} tile, or
	 * inside its {@code completionZone}. A step that declares neither a tile
	 * ({@code worldX <= 0}) nor a zone returns {@code false}: its position is
	 * unconfirmable, so it must not be treated as a state-derived jump target.
	 */
	private boolean playerIsConfirmablyInStepArea(GuidanceStep step)
	{
		if (step == null || lastKnownPlayerLocation == null)
		{
			return false;
		}
		Zone zone = step.getZone();
		if (zone != null && zone.contains(lastKnownPlayerLocation))
		{
			return true;
		}
		if (step.getWorldX() > 0
			&& lastKnownPlayerLocation.getPlane() == step.getWorldPlane()
			&& lastKnownPlayerLocation.distanceTo2D(
				new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane()))
				<= step.getCompletionDistance())
		{
			return true;
		}
		return false;
	}

	/**
	 * Returns the effective required-item list for a step: the
	 * {@code perItemRequiredItemIds} override for the active target item when
	 * present, otherwise the step's static {@code requiredItemIds}.
	 */
	private List<Integer> effectiveRequiredItemIds(GuidanceStep step)
	{
		if (step == null)
		{
			return null;
		}
		if (step.getPerItemRequiredItemIds() != null && activeTargetItemIdSupplier != null)
		{
			Integer target = activeTargetItemIdSupplier.get();
			if (target != null)
			{
				List<Integer> override = step.getPerItemRequiredItemIds().get(target);
				if (override != null)
				{
					return override;
				}
			}
		}
		return step.getRequiredItemIds();
	}

	/** Returns true if the player currently holds {@code itemId} (inventory or equipped). */
	private boolean playerHolds(int itemId)
	{
		return inventoryState.hasItem(itemId) || inventoryState.hasEquippedItem(itemId);
	}

	/** Returns true if the player holds none of the given item ids (and the list is non-empty). */
	private boolean playerHasNoneOf(List<Integer> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return false;
		}
		for (Integer id : ids)
		{
			if (id != null && id > 0 && playerHolds(id))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns true if the player holds every effective required item for the step.
	 * Used to release the restock latch once the player has re-banked. An empty
	 * required list counts as satisfied.
	 */
	private boolean playerHasAllRequiredItems(GuidanceStep step)
	{
		List<Integer> required = effectiveRequiredItemIds(step);
		if (required == null || required.isEmpty())
		{
			return true;
		}
		for (Integer id : required)
		{
			if (id != null && id > 0 && !playerHolds(id))
			{
				return false;
			}
		}
		return true;
	}

	private void fireSequenceComplete()
	{
		active = false;
		if (onSequenceComplete != null)
		{
			onSequenceComplete.run();
		}
	}

	private void notifyStepChanged(GuidanceStep step)
	{
		if (onStepChanged != null)
		{
			onStepChanged.accept(step);
		}
	}
}
