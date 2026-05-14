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

/**
 * Detects partial quest progress for quests with meaningful pre-completion
 * milestones that gate guidance, travel, or unlock content.
 *
 * <p>Implementations read varplayer/varbit state on the client thread and
 * cache results for safe multi-thread reads. Callers should invoke
 * {@link #refresh()} after login and on relevant VarbitChanged events.
 *
 * <p>Rationale for a separate interface (rather than extending
 * {@link com.collectionloghelper.data.PlayerTravelCapabilities}):
 * quest sub-progress is conceptually broader than travel — it covers content
 * unlocks (Varlamore), equipment tiers (RFD gloves), and access gating that
 * the travel model does not track.
 */
public interface QuestProgressState
{
	/**
	 * Returns {@code true} if the player has reached the given sub-milestone.
	 *
	 * <p>A milestone is considered reached when its backing varplayer/varbit
	 * value meets or exceeds the threshold for that milestone, or when the
	 * relevant RuneLite {@link net.runelite.api.Quest} reports
	 * {@link net.runelite.api.QuestState#FINISHED} (for completion-only
	 * milestones).
	 *
	 * <p>Returns {@code false} — never throws — when state cannot be read
	 * (e.g., called off-thread before the first refresh).
	 *
	 * @param milestone the sub-milestone to check
	 * @return {@code true} if the milestone has been reached
	 */
	boolean hasSubProgress(QuestSubMilestone milestone);

	/**
	 * Re-reads all backing varplayer/varbit values from the client and caches
	 * them. Must be called from the client thread (e.g., from an
	 * {@code @Subscribe} handler for {@code VarbitChanged} or after login).
	 */
	void refresh();

	/**
	 * Clears all cached state. Should be called on logout so stale values are
	 * not served to a subsequent login.
	 */
	void reset();
}
