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
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

/**
 * Resolves a {@link GuidanceStep}'s required item IDs into display rows
 * (name + availability) by consulting inventory and bank state.
 *
 * <p>This runs at step-transition time (not in any render loop), so the
 * cost of {@link ItemManager#getItemComposition(int)} is paid once per
 * step change rather than every frame.
 */
@Slf4j
@Singleton
public class RequiredItemResolver
{
	private final PlayerInventoryState inventoryState;
	private final PlayerBankState bankState;
	private final ItemManager itemManager;

	@Inject
	RequiredItemResolver(
		PlayerInventoryState inventoryState,
		PlayerBankState bankState,
		ItemManager itemManager)
	{
		this.inventoryState = inventoryState;
		this.bankState = bankState;
		this.itemManager = itemManager;
	}

	/**
	 * Resolves a step's {@code requiredItemIds} into display rows.
	 * Returns an empty list when the step is null or has no required items.
	 */
	public List<RequiredItemDisplay> resolve(GuidanceStep step)
	{
		if (step == null)
		{
			return Collections.emptyList();
		}
		return resolveIds(step.getRequiredItemIds());
	}

	/**
	 * Resolves a step's {@code recommendedItemIds} into display rows.
	 * Returns an empty list when the step is null or has no recommended items.
	 */
	public List<RequiredItemDisplay> resolveRecommended(GuidanceStep step)
	{
		if (step == null)
		{
			return Collections.emptyList();
		}
		return resolveIds(step.getRecommendedItemIds());
	}

	/**
	 * Resolves an arbitrary list of item IDs into display rows.
	 * Returns an empty list when {@code ids} is null or empty, or contains
	 * only invalid IDs. Shared implementation for both required and recommended
	 * item resolution so colouring logic stays consistent.
	 */
	public List<RequiredItemDisplay> resolveIds(List<Integer> ids)
	{
		if (ids == null || ids.isEmpty())
		{
			return Collections.emptyList();
		}
		List<RequiredItemDisplay> out = new ArrayList<>(ids.size());
		for (Integer itemId : ids)
		{
			if (itemId == null || itemId <= 0)
			{
				continue;
			}
			out.add(resolveSingle(itemId));
		}
		return out;
	}

	private RequiredItemDisplay resolveSingle(int itemId)
	{
		Status status = resolveStatus(itemId);
		return new RequiredItemDisplay(itemId, lookupName(itemId), status);
	}

	private Status resolveStatus(int itemId)
	{
		if (inventoryState.hasItem(itemId) || inventoryState.hasEquippedItem(itemId))
		{
			return Status.HELD;
		}
		if (bankState.hasItem(itemId))
		{
			return Status.IN_BANK;
		}
		return Status.MISSING;
	}

	private String lookupName(int itemId)
	{
		try
		{
			return itemManager.getItemComposition(itemId).getName();
		}
		catch (Throwable ex)
		{
			// Defensive: getItemComposition can fail for several reasons:
			//   - Invalid item IDs (RuntimeException, e.g. NPE on null comp)
			//   - Cache not yet loaded
			//   - Caller not on client thread (AssertionError "must be called
			//     on client thread") — see cha-ndler/collection-log-helper#388
			// AssertionError extends Error, not RuntimeException, so catching
			// only RuntimeException let it propagate and silently aborted the
			// entire guidance activation. Catching Throwable here keeps the
			// resolver fail-safe: the player sees "Item #N" instead of nothing,
			// and the calling activation path completes normally.
			log.warn("Item composition lookup failed for id {}: {}",
				itemId, ex.toString());
			return "Item #" + itemId;
		}
	}
}
