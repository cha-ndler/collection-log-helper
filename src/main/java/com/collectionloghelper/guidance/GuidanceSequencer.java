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

	@Inject
	private GuidanceSequencer(PlayerInventoryState inventoryState, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker, BossGuidanceRegistry bossRegistry)
	{
		this.inventoryState = inventoryState;
		this.requirementsChecker = requirementsChecker;
		this.bossRegistry = bossRegistry;
		this.completionChecker = new CompletionChecker(inventoryState, collectionState);
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
		this.active = true;

		// Skip any steps whose conditions are already met
		skipSatisfiedSteps();

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
	 * moves the index forward (or backward, if {@link #isStepAlreadySatisfied}
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
	 * Called when a collection log item is obtained. Checks ITEM_OBTAINED condition.
	 */
	public void onItemObtained(int itemId)
	{
		if (!active)
		{
			return;
		}

		GuidanceStep step = getRawCurrentStep();
		if (completionChecker.isItemObtainedSatisfying(step, itemId))
		{
			log.info("Step {} complete (ITEM_OBTAINED: {})", currentIndex + 1, itemId);
			advanceStep();
		}
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

		GuidanceStep step = getRawCurrentStep();

		// Auto-advance if skipIfHasAnyItemIds is now satisfied (e.g., key just entered inventory)
		CompletionChecker.SkipIfHasAnyResult skip = completionChecker.evaluateSkipIfHasAny(step);
		if (skip.matched())
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

		GuidanceStep step = getRawCurrentStep();
		if (completionChecker.isNpcDeathSatisfying(step, npcId))
		{
			log.info("Step {} complete (ACTOR_DEATH: {})", currentIndex + 1, npcId);
			advanceStep();
		}
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

		GuidanceStep step = getRawCurrentStep();
		CompletionChecker.ChatMatchResult chatResult =
			completionChecker.evaluateChatMessage(step, message, chatPatternCache);
		chatPatternCache = chatResult.cache();
		if (chatResult.matched())
		{
			log.info("Step {} complete (CHAT_MESSAGE_RECEIVED: matched '{}')",
				currentIndex + 1, step.getCompletionChatPattern());
			advanceStep();
		}
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

		GuidanceStep step = getRawCurrentStep();
		if (completionChecker.isNpcInteractedSatisfying(step, npcId))
		{
			log.info("Step {} complete (NPC_TALKED_TO: {})", currentIndex + 1, npcId);
			advanceStep();
		}
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

		GuidanceStep step = getRawCurrentStep();
		if (completionChecker.isVarbitAtLeastSatisfying(step, varbitId, value))
		{
			log.info("Step {} complete (VARBIT_AT_LEAST: varbit {} = {} >= {})",
				currentIndex + 1, varbitId, value, step.getCompletionVarbitValue());
			advanceStep();
		}
	}

	/**
	 * Unconditionally skips the current step, bypassing any active loop.
	 * Used by the panel's Skip button to let users move past steps they
	 * want to handle manually or that are stuck.
	 */
	public void skipStep()
	{
		final List<GuidanceStep> currentSteps = this.steps;
		if (!active || currentSteps == null)
		{
			return; // sequence already complete or never started
		}

		loopIterationsCompleted = 0;
		crossedWaypointIndex = 0;
		currentIndex++;
		skipSatisfiedSteps();
		finishStepTransition(currentSteps, "Skipped to", "Guidance sequence complete (skipped)");
	}

	/**
	 * Advances to the next step, respecting loop conditions.
	 * Used for automatic completion and the Next Step button.
	 */
	public void advanceStep()
	{
		final List<GuidanceStep> currentSteps = this.steps;
		if (!active || currentSteps == null)
		{
			return;
		}

		// Check if the completing step has a loop
		GuidanceStep completingStep = getRawCurrentStep();
		if (completingStep != null && completingStep.getLoopBackToStep() > 0 && completingStep.getLoopCount() > 0)
		{
			loopIterationsCompleted++;
			if (loopIterationsCompleted < completingStep.getLoopCount())
			{
				int targetIndex = completingStep.getLoopBackToStep() - 1; // convert 1-indexed to 0-indexed
				log.info("Loop iteration {}/{} complete — looping back to step {}",
					loopIterationsCompleted, completingStep.getLoopCount(), completingStep.getLoopBackToStep());
				currentIndex = targetIndex;
				crossedWaypointIndex = 0;
				skipSatisfiedSteps();

				// skipSatisfiedSteps() may fire onSequenceComplete -> stopSequence(),
				// nulling this.steps. Snapshot was captured above; use it for size checks.
				if (this.steps == null)
				{
					// sequence completed (and cleaned up) inside skipSatisfiedSteps()
					return;
				}

				if (active)
				{
					GuidanceStep step = getCurrentStep();
					if (step != null)
					{
						log.info("Resumed at step {}/{}: {}", currentIndex + 1, currentSteps.size(), step.getDescription());
						notifyStepChanged(step);
					}
				}
				return;
			}
			log.info("All {} loop iterations complete — advancing past loop", completingStep.getLoopCount());
			loopIterationsCompleted = 0;
		}

		currentIndex++;
		crossedWaypointIndex = 0;
		skipSatisfiedSteps();
		finishStepTransition(currentSteps, "Advanced to", "Guidance sequence complete");
	}

	/**
	 * Post-skip-chain finalizer shared by {@link #skipStep} and {@link #advanceStep}.
	 * Handles the three cases produced by {@link #skipSatisfiedSteps}: sequence
	 * cleaned up by {@link #stopSequence}, advanced past the final step, or
	 * landed on a new step that needs a change notification.
	 */
	private void finishStepTransition(List<GuidanceStep> currentSteps, String advancedLabel,
		String completeLabel)
	{
		// skipSatisfiedSteps() may fire onSequenceComplete -> stopSequence(), nulling this.steps.
		if (this.steps == null)
		{
			return;
		}

		if (currentIndex >= currentSteps.size())
		{
			log.info("{} for {}", completeLabel, activeSource != null ? activeSource.getName() : "?");
			active = false;
			if (onSequenceComplete != null)
			{
				onSequenceComplete.run();
			}
			return;
		}

		GuidanceStep step = getCurrentStep();
		if (step != null)
		{
			log.info("{} step {}/{}: {}", advancedLabel, currentIndex + 1, currentSteps.size(), step.getDescription());
			notifyStepChanged(step);
		}
	}

	/**
	 * Skips steps whose completion conditions are already satisfied.
	 */
	private void skipSatisfiedSteps()
	{
		while (active && steps != null && currentIndex < steps.size())
		{
			GuidanceStep step = steps.get(currentIndex);
			if (isStepAlreadySatisfied(step))
			{
				log.debug("Skipping already-satisfied step {}: {}", currentIndex + 1, step.getDescription());
				currentIndex++;
			}
			else
			{
				break;
			}
		}

		if (steps != null && currentIndex >= steps.size())
		{
			log.info("All steps already satisfied for {}", activeSource != null ? activeSource.getName() : "?");
			active = false;
			if (onSequenceComplete != null)
			{
				onSequenceComplete.run();
			}
		}
	}

	private boolean isStepAlreadySatisfied(GuidanceStep step)
	{
		return completionChecker.isStepAlreadySatisfied(step, lastKnownPlayerLocation);
	}

	private void notifyStepChanged(GuidanceStep step)
	{
		if (onStepChanged != null)
		{
			onStepChanged.accept(step);
		}
	}
}
