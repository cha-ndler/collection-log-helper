/*
 * Copyright (c) 2025, Chandler
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
import com.collectionloghelper.data.Zone;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
	/** Grand Exchange bank location for synthetic "go to bank" steps. */
	private static final int GE_BANK_X = 3164;
	private static final int GE_BANK_Y = 3489;
	private static final int GE_BANK_PLANE = 0;

	private final PlayerInventoryState inventoryState;
	private final PlayerCollectionState collectionState;
	private final RequirementsChecker requirementsChecker;

	private volatile WorldPoint lastKnownPlayerLocation;
	private volatile CollectionLogSource activeSource;
	private volatile List<GuidanceStep> steps;
	private volatile int currentIndex;
	private volatile boolean active;
	private volatile int loopIterationsCompleted;
	private volatile int cumulativeActionCount;

	/** Cached compiled pattern for the current step's chat completion regex. */
	private Pattern compiledChatPattern;
	/** The source pattern string that produced compiledChatPattern, for invalidation. */
	private String compiledChatPatternSource;
	/** Cached WorldPoint for the current step's target location. */
	private WorldPoint cachedStepPoint;
	private int cachedStepPointIndex = -1;

	/** Cache of resolved alternatives keyed by step index. Cleared when a new sequence starts. */
	private final Map<Integer, GuidanceStep> resolvedAlternatives = new HashMap<>();

	private Consumer<GuidanceStep> onStepChanged;
	private Runnable onSequenceComplete;

	@Inject
	private GuidanceSequencer(PlayerInventoryState inventoryState, PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker)
	{
		this.inventoryState = inventoryState;
		this.collectionState = collectionState;
		this.requirementsChecker = requirementsChecker;
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
		this.steps = source.getGuidanceSteps();
		this.currentIndex = 0;
		this.loopIterationsCompleted = 0;
		this.cumulativeActionCount = 0;
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
		resolvedAlternatives.clear();
		onStepChanged = null;
		onSequenceComplete = null;
	}

	/**
	 * Returns the current guidance step, or null if no sequence is active.
	 * If the step has conditional alternatives, returns the resolved version.
	 * If the current step requires items not in inventory, returns a synthetic
	 * "go to bank" step instead.
	 */
	public GuidanceStep getCurrentStep()
	{
		if (!active || steps == null || currentIndex >= steps.size())
		{
			return null;
		}

		GuidanceStep step = resolveStep(currentIndex);

		// Check if the step requires items not currently in inventory
		if (step.getRequiredItemIds() != null && !step.getRequiredItemIds().isEmpty()
			&& !inventoryState.hasAllItems(step.getRequiredItemIds()))
		{
			return createBankStep(step);
		}

		return step;
	}

	/**
	 * Returns the raw current step without bank routing substitution,
	 * but with conditional alternatives resolved.
	 */
	public GuidanceStep getRawCurrentStep()
	{
		if (!active || steps == null || currentIndex >= steps.size())
		{
			return null;
		}
		return resolveStep(currentIndex);
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

	public int getCumulativeActionCount()
	{
		return cumulativeActionCount;
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.ITEM_OBTAINED
			&& step.getCompletionItemId() == itemId)
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
		if (step != null && step.getCompletionItemId() > 0)
		{
			if (step.getCompletionCondition() == CompletionCondition.INVENTORY_HAS_ITEM
				&& inventoryState.hasItemCount(step.getCompletionItemId(), step.getCompletionItemCount()))
			{
				log.info("Step {} complete (INVENTORY_HAS_ITEM: {} x{})",
					currentIndex + 1, step.getCompletionItemId(), step.getCompletionItemCount());
				advanceStep();
				return;
			}

			if (step.getCompletionCondition() == CompletionCondition.INVENTORY_NOT_HAS_ITEM
				&& !inventoryState.hasItem(step.getCompletionItemId()))
			{
				log.info("Step {} complete (INVENTORY_NOT_HAS_ITEM: {})",
					currentIndex + 1, step.getCompletionItemId());
				advanceStep();
				return;
			}
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
			if (cachedStepPointIndex != currentIndex)
			{
				cachedStepPointIndex = currentIndex;
				cachedStepPoint = new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			}
			WorldPoint stepPoint = cachedStepPoint;
			int dist = playerLocation.distanceTo2D(stepPoint);
			if (playerLocation.getPlane() == step.getWorldPlane()
				&& dist <= step.getCompletionDistance())
			{
				log.info("Step {} complete (ARRIVE_AT_TILE: within {} tiles, plane {})",
					currentIndex + 1, step.getCompletionDistance(), step.getWorldPlane());
				advanceStep();
			}
		}

		if (step.getCompletionCondition() == CompletionCondition.ARRIVE_AT_ZONE)
		{
			Zone zone = step.getZone();
			if (zone != null && zone.contains(playerLocation))
			{
				log.info("Step {} complete (ARRIVE_AT_ZONE: player in zone [{},{} - {},{}] plane {})",
					currentIndex + 1, zone.getMinX(), zone.getMinY(), zone.getMaxX(), zone.getMaxY(), zone.getPlane());
				advanceStep();
			}
		}

		if (step.getCompletionCondition() == CompletionCondition.PLAYER_ON_PLANE)
		{
			if (playerLocation.getPlane() == step.getWorldPlane())
			{
				log.info("Step {} complete (PLAYER_ON_PLANE: plane {})",
					currentIndex + 1, step.getWorldPlane());
				advanceStep();
			}
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.ACTOR_DEATH
			&& step.getCompletionNpcId() == npcId)
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.CHAT_MESSAGE_RECEIVED
			&& step.getCompletionChatPattern() != null)
		{
			String patternStr = step.getCompletionChatPattern();
			if (!patternStr.equals(compiledChatPatternSource))
			{
				compiledChatPatternSource = patternStr;
				compiledChatPattern = Pattern.compile(patternStr);
			}
			if (compiledChatPattern.matcher(message).find())
			{
				log.info("Step {} complete (CHAT_MESSAGE_RECEIVED: matched '{}')",
					currentIndex + 1, step.getCompletionChatPattern());
				advanceStep();
			}
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.NPC_TALKED_TO
			&& step.getCompletionNpcId() == npcId)
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.VARBIT_AT_LEAST
			&& step.getCompletionVarbitId() == varbitId
			&& value >= step.getCompletionVarbitValue())
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
		if (!active || steps == null)
		{
			return;
		}

		loopIterationsCompleted = 0;
		currentIndex++;
		skipSatisfiedSteps();

		if (currentIndex >= steps.size())
		{
			log.info("Guidance sequence complete (skipped) for {}",
				activeSource != null ? activeSource.getName() : "?");
			active = false;
			if (onSequenceComplete != null)
			{
				onSequenceComplete.run();
			}
		}
		else
		{
			GuidanceStep step = getCurrentStep();
			if (step != null)
			{
				log.info("Skipped to step {}/{}: {}", currentIndex + 1, steps.size(), step.getDescription());
				notifyStepChanged(step);
			}
		}
	}

	/**
	 * Advances to the next step, respecting loop conditions.
	 * Used for automatic completion and the Next Step button.
	 */
	public void advanceStep()
	{
		if (!active || steps == null)
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
				skipSatisfiedSteps();
				if (active)
				{
					GuidanceStep step = getCurrentStep();
					if (step != null)
					{
						log.info("Resumed at step {}/{}: {}", currentIndex + 1, steps.size(), step.getDescription());
						notifyStepChanged(step);
					}
				}
				return;
			}
			log.info("All {} loop iterations complete — advancing past loop", completingStep.getLoopCount());
			loopIterationsCompleted = 0;
		}

		currentIndex++;
		skipSatisfiedSteps();

		if (currentIndex >= steps.size())
		{
			log.info("Guidance sequence complete for {}", activeSource != null ? activeSource.getName() : "?");
			active = false;
			if (onSequenceComplete != null)
			{
				onSequenceComplete.run();
			}
		}
		else
		{
			GuidanceStep step = getCurrentStep();
			if (step != null)
			{
				log.info("Advanced to step {}/{}: {}", currentIndex + 1, steps.size(), step.getDescription());
				notifyStepChanged(step);
			}
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
		if (step.getCompletionCondition() == null)
		{
			return false;
		}

		switch (step.getCompletionCondition())
		{
			case INVENTORY_HAS_ITEM:
				return step.getCompletionItemId() > 0
					&& inventoryState.hasItemCount(step.getCompletionItemId(), step.getCompletionItemCount());
			case INVENTORY_NOT_HAS_ITEM:
				return step.getCompletionItemId() > 0 && !inventoryState.hasItem(step.getCompletionItemId());
			case ITEM_OBTAINED:
				return step.getCompletionItemId() > 0 && collectionState.isItemObtained(step.getCompletionItemId());
			case ARRIVE_AT_TILE:
				return lastKnownPlayerLocation != null && step.getWorldX() > 0
					&& lastKnownPlayerLocation.getPlane() == step.getWorldPlane()
					&& lastKnownPlayerLocation.distanceTo2D(
						new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane()))
					<= step.getCompletionDistance();
			case ARRIVE_AT_ZONE:
				Zone zone = step.getZone();
				return lastKnownPlayerLocation != null && zone != null
					&& zone.contains(lastKnownPlayerLocation);
			case PLAYER_ON_PLANE:
				return lastKnownPlayerLocation != null
					&& lastKnownPlayerLocation.getPlane() == step.getWorldPlane();
			case ACTOR_DEATH:
			case CHAT_MESSAGE_RECEIVED:
			case VARBIT_AT_LEAST:
				return false;
			default:
				return false;
		}
	}

	private GuidanceStep createBankStep(GuidanceStep originalStep)
	{
		return new GuidanceStep(
			"Get required items from bank",
			GE_BANK_X, GE_BANK_Y, GE_BANK_PLANE,
			0, null, null,
			"Grand Exchange bank",
			null,
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,  // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null,  // highlightItemIds
			null,  // groundItemIds
			null,  // completionChatPattern
			0, 0,  // completionVarbitId, completionVarbitValue
			false,  // useItemOnObject
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // completionZone
			null   // conditionalAlternatives
		);
	}

	private void notifyStepChanged(GuidanceStep step)
	{
		if (onStepChanged != null)
		{
			onStepChanged.accept(step);
		}
	}
}
