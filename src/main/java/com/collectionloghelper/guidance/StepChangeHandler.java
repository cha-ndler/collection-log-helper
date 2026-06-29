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
import com.collectionloghelper.overlay.GuidanceInfoBox;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;

/**
 * Handles the InfoBox progress refresh + panel step-progress push that
 * {@link GuidanceOverlayCoordinator} performs each time the sequencer
 * advances to a new step. Pure extraction of the second half of
 * {@code GuidanceOverlayCoordinator.onStepChanged} (the overlay/NPC-tracker
 * portion stays in the coordinator because it mutates coordinator-owned
 * state such as {@code lastMessagedStepIndex} and {@code activeMapPoint}).
 *
 * <p>No state of its own: collaborators are injected and the coordinator
 * threads its mutable view of the world through {@link Input}. The
 * {@code activeInfoBox} is mutated in-place via {@code setStepText} /
 * {@code setTooltipText} / {@code setTextColor}, so no result-write-back
 * is needed.</p>
 *
 * <p>Performance note: {@code onStepChanged} fires on guidance step
 * transitions (not per-tick) but can fire multiple times in rapid
 * succession during fast-skip / cascade. The guard-fail paths
 * ({@code activeInfoBox == null}, {@code panel == null}) allocate nothing.</p>
 */
@Slf4j
@Singleton
public class StepChangeHandler
{
	private final ClientThread clientThread;
	private final GuidanceSequencer guidanceSequencer;
	private final RequiredItemResolver requiredItemResolver;

	/**
	 * Zero-based index of the last step this handler pushed to the panel, or {@code -1}
	 * before the first push. Used to distinguish a genuine step CHANGE from a re-notify
	 * of the SAME step (#681).
	 *
	 * <p>On a same-step re-notify — fired by {@code GuidanceSequencer.onInventoryChanged}
	 * on every inventory change while guidance is active — the immediate empty-items push
	 * is skipped so the "Items needed" section never blinks empty→full. The empty push
	 * exists only to show step text snappily on a real step change while item names resolve
	 * off the EDT (see #388); on a re-notify the items are already on screen and resolving
	 * directly (then relying on the {@code StepProgressView} idempotency guard) avoids the
	 * intermediate empty frame entirely.</p>
	 */
	private int lastShownStepIndex = -1;

	@Inject
	StepChangeHandler(
		ClientThread clientThread,
		GuidanceSequencer guidanceSequencer,
		RequiredItemResolver requiredItemResolver)
	{
		this.clientThread = clientThread;
		this.guidanceSequencer = guidanceSequencer;
		this.requiredItemResolver = requiredItemResolver;
	}

	/**
	 * Refreshes the InfoBox progress text/tooltip/colour and pushes a panel
	 * step-progress update for the given step. Pure extraction -- no
	 * behavioural change.
	 *
	 * @param step the new current step (must not be null; the sequencer
	 *             never invokes the step-changed callback with a null step)
	 * @param in coordinator-owned mutable refs (active InfoBox, panel,
	 *           active-target-item id) consumed by this handler
	 */
	void handle(GuidanceStep step, Input in)
	{
		// Update InfoBox progress (mutates in.activeInfoBox in-place;
		// no field reassignment needed back on the coordinator).
		if (in.activeInfoBox != null)
		{
			int current = guidanceSequencer.getCurrentIndex() + 1;
			int total = guidanceSequencer.getTotalSteps();
			in.activeInfoBox.setStepText(current + "/" + total);
			String tooltip = step.getDescription();
			if (guidanceSequencer.getCumulativeTrackThreshold() > 0)
			{
				tooltip += "\n" + guidanceSequencer.getCumulativeActionCount()
					+ "/" + guidanceSequencer.getCumulativeTrackThreshold()
					+ " actions tracked";
			}
			final String infoBoxHint = guidanceSequencer.getActiveStepHint();
			if (infoBoxHint != null)
			{
				tooltip += "\n" + infoBoxHint;
			}
			in.activeInfoBox.setTooltipText(tooltip);
			if (current == total)
			{
				in.activeInfoBox.setTextColor(Color.GREEN);
			}
		}

		if (in.panel != null)
		{
			final int current = guidanceSequencer.getCurrentIndex() + 1;
			final int total = guidanceSequencer.getTotalSteps();
			final String stepHint = guidanceSequencer.getActiveStepHint();
			final String baseDesc = step.resolveDescription(in.activeTargetItemId);
			final String desc = stepHint != null ? baseDesc + "\n" + stepHint : baseDesc;
			final boolean isManual = step.getCompletionCondition() == CompletionCondition.MANUAL;
			final GuidanceStep rawStep = guidanceSequencer.getRawCurrentStep();
			final CollectionLogSource stepChangeSource = guidanceSequencer.getActiveSource();
			final List<GuidanceStep> sourceSteps = stepChangeSource != null
				&& stepChangeSource.getGuidanceSteps() != null
				? stepChangeSource.getGuidanceSteps() : Collections.emptyList();

			// Distinguish a genuine step CHANGE from a re-notify of the SAME step (#681).
			// onInventoryChanged re-notifies the current step on every inventory change;
			// on those re-notifies the items are already on screen, so the immediate empty
			// push would needlessly clear them and cause a visible flash.
			final int currentIndex = guidanceSequencer.getCurrentIndex();
			final boolean sameStepRenotify = currentIndex == lastShownStepIndex;
			lastShownStepIndex = currentIndex;

			// Push description + step progress to the panel immediately (safe from any thread
			// because showStep dispatches to the EDT). Required-item resolution is deferred
			// to the client thread because RequiredItemResolver.resolve() calls
			// ItemManager.getItemComposition, which asserts caller-is-client-thread. See
			// cha-ndler/collection-log-helper#388 for the original trace.
			//
			// On a genuine step change we do the two-phase push: empty items immediately
			// (snappy step text), resolved items ~1 tick later. On a same-step re-notify we
			// SKIP the empty push and go straight to resolving — combined with the
			// StepProgressView idempotency guard (#681) an unchanged re-notify is a complete
			// no-op and one whose item availability changed updates in place with no blink.
			if (!sameStepRenotify)
			{
				in.panel.updateStepProgress(current, total, desc, isManual,
					Collections.emptyList(), Collections.emptyList(), sourceSteps);
			}
			final CollectionLogHelperPanel panelRef = in.panel;
			clientThread.invokeLater(() ->
			{
				final List<RequiredItemDisplay> resolvedItems =
					requiredItemResolver.resolve(rawStep);
				final List<RequiredItemDisplay> resolvedRecommended =
					requiredItemResolver.resolveRecommended(rawStep);
				final java.util.Set<Integer> activityIds = rawStep != null
					? new java.util.HashSet<>(rawStep.getActivityObtainableItemIds())
					: Collections.emptySet();
				panelRef.updateStepProgress(current, total, desc, isManual,
					resolvedItems, resolvedRecommended, sourceSteps, activityIds);
			});
		}
	}

	/**
	 * Coordinator-owned mutable refs this handler reads but does not own.
	 * No result type: {@code activeInfoBox} is mutated in-place via setters,
	 * not replaced.
	 */
	static final class Input
	{
		@Nullable
		final GuidanceInfoBox activeInfoBox;
		@Nullable
		final CollectionLogHelperPanel panel;
		@Nullable
		final Integer activeTargetItemId;

		Input(
			@Nullable GuidanceInfoBox activeInfoBox,
			@Nullable CollectionLogHelperPanel panel,
			@Nullable Integer activeTargetItemId)
		{
			this.activeInfoBox = activeInfoBox;
			this.panel = panel;
			this.activeTargetItemId = activeTargetItemId;
		}
	}
}
