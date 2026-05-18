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
import com.collectionloghelper.data.ItemObjectTier;
import com.collectionloghelper.data.PlayerInventoryState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Pure tier-resolution half of the dynamic item-to-object override that
 * {@link GuidanceOverlayCoordinator} applies on step change and on inventory
 * change. Given a {@link GuidanceStep} that carries
 * {@link GuidanceStep#getDynamicItemObjectTiers()}, scans the player's
 * inventory and returns the matched object-id / item-id / action / tooltip
 * snapshot, or {@link Result#EMPTY} when no tier matches.
 *
 * <p>Extracted from {@code GuidanceOverlayCoordinator.applyDynamicItemObjectOverlays}
 * so the tier-resolution logic is independently testable and the per-tick
 * (overlay refresh) no-op path allocates nothing -- {@link Result#EMPTY} is a
 * cached singleton returned whenever the step has no dynamic tiers or none of
 * the tiers' items are present in inventory.</p>
 *
 * <p>The overlay-mutation half stays on the coordinator because that class
 * owns the overlay collaborators and the no-match branch is a no-op there
 * (no setters fire).</p>
 *
 * @see GuidanceOverlayCoordinator
 * @see ItemObjectTier
 */
@Singleton
public class DynamicItemObjectTierResolver
{
	private final PlayerInventoryState playerInventoryState;

	@Inject
	DynamicItemObjectTierResolver(PlayerInventoryState playerInventoryState)
	{
		this.playerInventoryState = playerInventoryState;
	}

	/**
	 * Walks {@code step.getDynamicItemObjectTiers()} and collects every tier
	 * whose item set intersects the current inventory. Mirrors the original
	 * inline loop: at most one match per tier (avoid duplicate items), tooltip
	 * derived from the first matching tier (or the step description when
	 * multiple tiers match), action derived from the first matching tier's
	 * {@code interactAction} (falling back to the step's
	 * {@code objectInteractAction}).
	 *
	 * @param step the current raw guidance step, or {@code null}
	 * @return matched object-id / item-id / action / tooltip snapshot, or
	 *         {@link Result#EMPTY} when the step has no dynamic tiers or
	 *         nothing in inventory matches.  Never returns {@code null}.
	 */
	Result resolve(@Nullable GuidanceStep step)
	{
		if (step == null
			|| step.getDynamicItemObjectTiers() == null
			|| step.getDynamicItemObjectTiers().isEmpty())
		{
			return Result.EMPTY;
		}

		Set<Integer> matchedObjectIds = null;
		List<Integer> matchedItemIds = null;
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
					if (matchedObjectIds == null)
					{
						matchedObjectIds = new HashSet<>();
						matchedItemIds = new ArrayList<>();
					}
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

		if (matchedObjectIds == null || matchedObjectIds.isEmpty())
		{
			return Result.EMPTY;
		}

		// When multiple tiers matched, the merged tooltip uses the step
		// description instead of the first tier's "<action> <tierName>" form.
		String finalTooltip = matchedItemIds.size() > 1 ? step.getDescription() : tooltipText;
		return new Result(matchedObjectIds, matchedItemIds, action, finalTooltip);
	}

	/**
	 * Snapshot returned by {@link #resolve(GuidanceStep)}. {@link #EMPTY} is a
	 * cached singleton used for every no-op path; callers MUST check
	 * {@link #hasMatch()} before touching the collection getters.
	 */
	static final class Result
	{
		/** Cached singleton used for the no-op path (no allocation). */
		static final Result EMPTY = new Result(
			Collections.emptySet(), Collections.emptyList(), null, null);

		private final Set<Integer> objectIds;
		private final List<Integer> itemIds;
		@Nullable
		private final String action;
		@Nullable
		private final String tooltipText;

		Result(
			Set<Integer> objectIds,
			List<Integer> itemIds,
			@Nullable String action,
			@Nullable String tooltipText)
		{
			this.objectIds = objectIds;
			this.itemIds = itemIds;
			this.action = action;
			this.tooltipText = tooltipText;
		}

		/** True when at least one tier matched the inventory. */
		boolean hasMatch()
		{
			return !objectIds.isEmpty();
		}

		Set<Integer> getObjectIds()
		{
			return objectIds;
		}

		List<Integer> getItemIds()
		{
			return itemIds;
		}

		@Nullable
		String getAction()
		{
			return action;
		}

		@Nullable
		String getTooltipText()
		{
			return tooltipText;
		}
	}
}
