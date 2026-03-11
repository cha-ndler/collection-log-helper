package com.collectionloghelper.guidance;

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerInventoryState;
import java.util.List;
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
	/** Grand Exchange bank location for synthetic "go to bank" steps. */
	private static final int GE_BANK_X = 3164;
	private static final int GE_BANK_Y = 3489;
	private static final int GE_BANK_PLANE = 0;

	private final PlayerInventoryState inventoryState;

	private volatile CollectionLogSource activeSource;
	private volatile List<GuidanceStep> steps;
	private volatile int currentIndex;
	private volatile boolean active;

	private Consumer<GuidanceStep> onStepChanged;
	private Runnable onSequenceComplete;

	@Inject
	private GuidanceSequencer(PlayerInventoryState inventoryState)
	{
		this.inventoryState = inventoryState;
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
		onStepChanged = null;
		onSequenceComplete = null;
	}

	/**
	 * Returns the current guidance step, or null if no sequence is active.
	 * If the current step requires items not in inventory, returns a synthetic
	 * "go to bank" step instead.
	 */
	public GuidanceStep getCurrentStep()
	{
		if (!active || steps == null || currentIndex >= steps.size())
		{
			return null;
		}

		GuidanceStep step = steps.get(currentIndex);

		// Check if the step requires items not currently in inventory
		if (step.getRequiredItemIds() != null && !step.getRequiredItemIds().isEmpty()
			&& !inventoryState.hasAllItems(step.getRequiredItemIds()))
		{
			return createBankStep(step);
		}

		return step;
	}

	/**
	 * Returns the raw current step without bank routing substitution.
	 */
	public GuidanceStep getRawCurrentStep()
	{
		if (!active || steps == null || currentIndex >= steps.size())
		{
			return null;
		}
		return steps.get(currentIndex);
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.INVENTORY_HAS_ITEM
			&& step.getCompletionItemId() > 0
			&& inventoryState.hasItem(step.getCompletionItemId()))
		{
			log.info("Step {} complete (INVENTORY_HAS_ITEM: {})", currentIndex + 1, step.getCompletionItemId());
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
		if (step != null && step.getCompletionCondition() == CompletionCondition.ARRIVE_AT_TILE
			&& step.getWorldX() > 0)
		{
			WorldPoint stepPoint = new WorldPoint(step.getWorldX(), step.getWorldY(), step.getWorldPlane());
			if (playerLocation.distanceTo2D(stepPoint) <= step.getCompletionDistance())
			{
				log.info("Step {} complete (ARRIVE_AT_TILE: within {} tiles)",
					currentIndex + 1, step.getCompletionDistance());
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
	 * Manually advances to the next step (for MANUAL completion or Skip button).
	 */
	public void advanceStep()
	{
		if (!active || steps == null)
		{
			return;
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
				return step.getCompletionItemId() > 0 && inventoryState.hasItem(step.getCompletionItemId());
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
			0, 0, 0,
			null  // worldMessage
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
