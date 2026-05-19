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
package com.collectionloghelper.player;

import com.google.inject.ImplementedBy;

/**
 * Read-only view of a player's Achievement Diary completion state.
 * <p>
 * Implementations cache the varbit values read from the RuneLite client and
 * expose them through this interface so callers (branch selectors, requirement
 * checkers, UI components) are decoupled from the RuneLite API.
 *
 * <p>Usage:
 * <pre>{@code
 * if (diaryTierState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD)) {
 *     // player has Karamja Hard diary complete
 * }
 * }</pre>
 */
@ImplementedBy(DiaryTierStateImpl.class)
public interface DiaryTierState
{
	/**
	 * Returns {@code true} if the player has completed the given diary tier for
	 * the given region.
	 *
	 * @param region the diary region (e.g. {@link DiaryRegion#KARAMJA})
	 * @param tier   the difficulty tier (e.g. {@link DiaryTier#HARD})
	 * @return {@code true} if the diary is complete, {@code false} otherwise
	 */
	boolean hasDiary(DiaryRegion region, DiaryTier tier);

	/**
	 * Refreshes all cached varbit values from the RuneLite client.
	 * Must be called on the client thread (e.g. in a {@code VarbitChanged}
	 * event handler or after login).
	 */
	void refresh();

	/**
	 * Resets all cached state to {@code false} (e.g. on logout).
	 */
	void reset();
}
