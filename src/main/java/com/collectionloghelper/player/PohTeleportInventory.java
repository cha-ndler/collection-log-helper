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

import java.util.Set;

/**
 * Read-only view of which POH-based teleports the current player has access to.
 *
 * <p>Implementations combine two detection sources per the D-02
 * aggressive-detection-with-fallbacks decision:
 * <ol>
 *   <li><b>Varbit detection</b> — reads game varbits set on login; authoritative
 *       when a positive signal is present but only available for teleports that
 *       have a reliably readable varbit outside the house (jewellery box tiers,
 *       portal nexus tier, mounted digsite pendant, mounted Xeric's talisman).
 *       </li>
 *   <li><b>Manual config override</b> — user-declared checkbox in the plugin
 *       config; used as a fallback when varbit detection cannot confirm a
 *       teleport (e.g. mounted glory, spirit tree, fairy ring — fixtures whose
 *       varbits are unreliable outside the POH).</li>
 * </ol>
 *
 * <p>Resolution rule (D-02):
 * <ul>
 *   <li>YES if varbit detects positive (config override irrelevant).</li>
 *   <li>YES if varbit is absent/zero but config override is {@code true}.</li>
 *   <li>NO if both varbit and config override are negative/false.</li>
 * </ul>
 *
 * <p>Wiring into Tier B guidance branching is deferred to milestone C6.
 * This interface is intentionally standalone for C1.
 */
public interface PohTeleportInventory
{
	/**
	 * Returns {@code true} if the player has access to the given POH teleport,
	 * based on varbit detection and/or manual config override.
	 *
	 * @param teleport the POH teleport to check
	 * @return {@code true} if the teleport is available
	 */
	boolean hasTeleport(PohTeleport teleport);

	/**
	 * Returns the set of all POH teleports currently available to the player.
	 * The returned set is an immutable snapshot.
	 *
	 * @return immutable set of available {@link PohTeleport} values
	 */
	Set<PohTeleport> getAvailableTeleports();

	/**
	 * Refreshes the varbit-based detection state from the game client.
	 * Must be called on the client thread (e.g., from a VarbitChanged handler
	 * or on login). No-op when the client is unavailable.
	 */
	void refresh();

	/**
	 * Resets all cached varbit state. Call on logout.
	 */
	void reset();
}
