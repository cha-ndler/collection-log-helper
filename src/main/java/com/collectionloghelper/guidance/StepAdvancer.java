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

import com.collectionloghelper.data.GuidanceStep;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Pure step-advancement decision logic extracted from {@link GuidanceSequencer}.
 *
 * <p>This class holds no mutable state. It inspects a snapshot of sequencer
 * state ({@link SkipChainInput} / {@link AdvanceInput}) and returns the new
 * index, loop counter, and "sequence complete" flag the sequencer should
 * apply. The sequencer remains the single owner of all field mutations,
 * listener notifications, and {@code onSequenceComplete} callbacks.</p>
 *
 * <p>Mirrors the carrier-record pattern established by {@link OverlayStepApplier}
 * (PR #510) and {@link CompletionChecker} (PR #512) for the Wave 4 god-class
 * splits tracked by issue #503.</p>
 */
@Slf4j
@Singleton
public class StepAdvancer
{
	private final CompletionChecker completionChecker;

	@Inject
	StepAdvancer(CompletionChecker completionChecker)
	{
		this.completionChecker = completionChecker;
	}

	/**
	 * Runs the "skip already-satisfied steps" scan starting from
	 * {@link SkipChainInput#startIndex}.
	 *
	 * <p>Pure: emits {@code log.debug} traces for each skipped step but never
	 * mutates state and never invokes callbacks. The caller applies
	 * {@link SkipChainResult#newIndex()} and, when
	 * {@link SkipChainResult#sequenceComplete()} is {@code true}, fires the
	 * {@code onSequenceComplete} callback and flips {@code active} to false —
	 * preserving the original behaviour where the skip-chain itself reported
	 * completion.</p>
	 */
	public SkipChainResult runSkipChain(SkipChainInput input)
	{
		final List<GuidanceStep> steps = input.steps;
		int index = input.startIndex;
		while (input.active && steps != null && index < steps.size())
		{
			GuidanceStep step = steps.get(index);
			if (completionChecker.isStepAlreadySatisfied(step, input.playerLocation))
			{
				log.debug("Skipping already-satisfied step {}: {}", index + 1, step.getDescription());
				index++;
			}
			else
			{
				break;
			}
		}

		boolean sequenceComplete = steps != null && index >= steps.size();
		return new SkipChainResult(index, sequenceComplete);
	}

	/**
	 * Plans the {@link GuidanceSequencer#skipStep()} transition: bump the index
	 * by one, reset the loop counter, and run the skip-chain from the new
	 * index. The returned {@link TransitionOutcome} carries the final index,
	 * loop-iterations counter, and whether the sequence is now complete.
	 *
	 * <p>Returns {@link TransitionOutcome#NO_OP} when the sequence is already
	 * inactive or has been cleaned up.</p>
	 */
	public TransitionOutcome skip(TransitionInput input)
	{
		if (!input.active || input.steps == null)
		{
			return TransitionOutcome.NO_OP;
		}
		SkipChainResult chain = runSkipChain(new SkipChainInput(
			input.currentIndex + 1, input.steps, input.active, input.playerLocation));
		return TransitionOutcome.transition(chain.newIndex(), 0, chain.sequenceComplete(), false);
	}

	/**
	 * Plans the {@link GuidanceSequencer#advanceStep()} transition: respect
	 * any active loop on the completing step, then run the skip-chain. The
	 * returned {@link TransitionOutcome} signals loop-back resume vs normal
	 * advance via {@link TransitionOutcome#loopBackResume()} so the caller can
	 * choose the appropriate log label.
	 *
	 * <p>Returns {@link TransitionOutcome#NO_OP} when the sequence is already
	 * inactive or has been cleaned up.</p>
	 */
	public TransitionOutcome advance(TransitionInput input)
	{
		if (!input.active || input.steps == null)
		{
			return TransitionOutcome.NO_OP;
		}

		final GuidanceStep completingStep = input.completingStep;
		boolean loopBackResume = false;
		int nextIndex;
		int newLoopIterations;
		if (completingStep != null
			&& completingStep.getLoopBackToStep() > 0
			&& completingStep.getLoopCount() > 0)
		{
			int incremented = input.loopIterationsCompleted + 1;
			if (incremented < completingStep.getLoopCount())
			{
				nextIndex = completingStep.getLoopBackToStep() - 1; // 1-indexed -> 0-indexed
				newLoopIterations = incremented;
				loopBackResume = true;
				log.info("Loop iteration {}/{} complete — looping back to step {}",
					incremented, completingStep.getLoopCount(),
					completingStep.getLoopBackToStep());
			}
			else
			{
				log.info("All {} loop iterations complete — advancing past loop", completingStep.getLoopCount());
				nextIndex = input.currentIndex + 1;
				newLoopIterations = 0;
			}
		}
		else
		{
			nextIndex = input.currentIndex + 1;
			newLoopIterations = input.loopIterationsCompleted;
		}

		SkipChainResult chain = runSkipChain(new SkipChainInput(
			nextIndex, input.steps, input.active, input.playerLocation));
		return TransitionOutcome.transition(chain.newIndex(), newLoopIterations,
			chain.sequenceComplete(), loopBackResume);
	}

	/**
	 * Snapshot of sequencer state read by {@link #runSkipChain}.
	 */
	public static final class SkipChainInput
	{
		final int startIndex;
		@Nullable
		final List<GuidanceStep> steps;
		final boolean active;
		@Nullable
		final WorldPoint playerLocation;

		public SkipChainInput(
			int startIndex,
			@Nullable List<GuidanceStep> steps,
			boolean active,
			@Nullable WorldPoint playerLocation)
		{
			this.startIndex = startIndex;
			this.steps = steps;
			this.active = active;
			this.playerLocation = playerLocation;
		}
	}

	/**
	 * Result of {@link #runSkipChain}. The caller applies {@code newIndex} and,
	 * when {@code sequenceComplete} is {@code true}, fires its
	 * {@code onSequenceComplete} callback (which may in turn null out the step
	 * list via {@code stopSequence}). Equivalent to the original tail of
	 * {@code skipSatisfiedSteps()} in {@link GuidanceSequencer}.
	 */
	public static final class SkipChainResult
	{
		private final int newIndex;
		private final boolean sequenceComplete;

		SkipChainResult(int newIndex, boolean sequenceComplete)
		{
			this.newIndex = newIndex;
			this.sequenceComplete = sequenceComplete;
		}

		public int newIndex()
		{
			return newIndex;
		}

		public boolean sequenceComplete()
		{
			return sequenceComplete;
		}
	}

	/**
	 * Snapshot of sequencer state read by {@link #skip} / {@link #advance}.
	 */
	public static final class TransitionInput
	{
		final int currentIndex;
		final int loopIterationsCompleted;
		@Nullable
		final List<GuidanceStep> steps;
		final boolean active;
		@Nullable
		final WorldPoint playerLocation;
		@Nullable
		final GuidanceStep completingStep;

		public TransitionInput(
			int currentIndex,
			int loopIterationsCompleted,
			@Nullable List<GuidanceStep> steps,
			boolean active,
			@Nullable WorldPoint playerLocation,
			@Nullable GuidanceStep completingStep)
		{
			this.currentIndex = currentIndex;
			this.loopIterationsCompleted = loopIterationsCompleted;
			this.steps = steps;
			this.active = active;
			this.playerLocation = playerLocation;
			this.completingStep = completingStep;
		}
	}

	/**
	 * Unified result of {@link #skip} / {@link #advance}. Carries the new
	 * index, new loop counter, whether the sequence is now complete (caller
	 * fires {@code onSequenceComplete}), whether this was a loop-back resume
	 * (caller chooses "Resumed at" vs "Advanced to" log label), and a
	 * {@link #NO_OP} sentinel for early-return cases.
	 */
	public static final class TransitionOutcome
	{
		public static final TransitionOutcome NO_OP = new TransitionOutcome(0, 0, false, false, true);

		private final int newIndex;
		private final int newLoopIterationsCompleted;
		private final boolean sequenceComplete;
		private final boolean loopBackResume;
		private final boolean noOp;

		private TransitionOutcome(int newIndex, int newLoopIterationsCompleted,
			boolean sequenceComplete, boolean loopBackResume, boolean noOp)
		{
			this.newIndex = newIndex;
			this.newLoopIterationsCompleted = newLoopIterationsCompleted;
			this.sequenceComplete = sequenceComplete;
			this.loopBackResume = loopBackResume;
			this.noOp = noOp;
		}

		static TransitionOutcome transition(int newIndex, int newLoopIterationsCompleted,
			boolean sequenceComplete, boolean loopBackResume)
		{
			return new TransitionOutcome(newIndex, newLoopIterationsCompleted,
				sequenceComplete, loopBackResume, false);
		}

		public int newIndex()
		{
			return newIndex;
		}

		public int newLoopIterationsCompleted()
		{
			return newLoopIterationsCompleted;
		}

		public boolean sequenceComplete()
		{
			return sequenceComplete;
		}

		public boolean loopBackResume()
		{
			return loopBackResume;
		}

		public boolean noOp()
		{
			return noOp;
		}
	}
}
