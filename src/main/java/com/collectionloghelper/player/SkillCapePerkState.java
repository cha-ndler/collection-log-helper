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
 * Read-only view of which skill-cape perks are currently available to
 * the logged-in player.
 *
 * <p>A perk is considered "available" when either:
 * <ol>
 *   <li>The player has the relevant skill cape equipped (detected by item ID
 *       in the equipment container), or</li>
 *   <li>The player's real skill level for the backing skill is {@code >= 99}
 *       (owned-status fallback — the player can wear the cape on demand).</li>
 * </ol>
 *
 * <p>The {@link SkillCapePerk#MAX_CAPE_TELES} perk uses equipped-item
 * detection only; its owned-status fallback is intentionally disabled because
 * the Max cape requires all 23 skills at 99, which cannot be verified with a
 * single-skill level check.
 *
 * <p>Implementations read the equipment container and skill levels from the
 * RuneLite {@link net.runelite.api.Client} on the client thread and cache the
 * results for safe multi-thread reads.
 *
 * <p>Usage:
 * <pre>{@code
 * if (skillCapePerkState.hasPerkAvailable(SkillCapePerk.CRAFTING_TELE)) {
 *     // route player via Crafting Guild teleport
 * }
 * }</pre>
 */
@ImplementedBy(SkillCapePerkStateImpl.class)
public interface SkillCapePerkState
{
	/**
	 * Returns {@code true} if the given perk is currently available to the
	 * player — either because the relevant cape is equipped or because the
	 * player's real skill level is {@code >= 99}.
	 *
	 * <p>Always returns {@code false} — never throws — if {@code perk} is
	 * {@code null} or if the client state has not been read yet.
	 *
	 * @param perk the perk to test; {@code null} returns {@code false}
	 * @return {@code true} if the perk is available
	 */
	boolean hasPerkAvailable(SkillCapePerk perk);

	/**
	 * Re-reads the equipment container and skill levels from the client and
	 * updates the cached state. Must be called on the client thread (e.g., in
	 * a {@code GameStateChanged}, {@code ItemContainerChanged}, or
	 * {@code StatChanged} event handler, and after login).
	 */
	void refresh();

	/**
	 * Clears all cached state. Should be called on logout so stale equipment
	 * and level data are not served to a subsequent login.
	 */
	void reset();
}
