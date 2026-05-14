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
 * Read-only snapshot of the player's currently-equipped items, focused on
 * items that influence guidance routing (teleport jewellery, skill capes,
 * the Max cape, etc.).
 *
 * <p>Implementations read {@code client.getItemContainer(InventoryID.EQUIPMENT)}
 * on the client thread, cache a snapshot, and expose it through this interface
 * so guidance-branch selectors and overlay components are decoupled from the
 * RuneLite API.
 *
 * <p>Usage:
 * <pre>{@code
 * if (equippedItemState.hasEquipped(ItemID.AMULET_OF_GLORY4)) {
 *     // player has a 4-charge glory equipped
 * }
 * if (equippedItemState.hasTeleportItemEquipped()) {
 *     // player has any recognised teleport item equipped
 * }
 * }</pre>
 */
public interface EquippedItemState
{
	/**
	 * Returns {@code true} if the item with the given ID is currently equipped.
	 *
	 * @param itemId the RuneLite {@code ItemID} constant to check
	 * @return {@code true} if that exact item ID is in the equipment container
	 */
	boolean hasEquipped(int itemId);

	/**
	 * Returns an unmodifiable snapshot of all item IDs currently in the
	 * equipment container.  Returns an empty set when the container is absent
	 * or the player is logged out.
	 *
	 * @return immutable set of equipped item IDs
	 */
	Set<Integer> getEquippedItems();

	/**
	 * Convenience helper — returns {@code true} if any item in
	 * {@link EquippedItemStateImpl#TELEPORT_ITEMS} is currently equipped.
	 *
	 * @return {@code true} if the player has at least one recognised teleport
	 *         item equipped
	 */
	boolean hasTeleportItemEquipped();

	/**
	 * Re-reads the equipment container from the client and updates the cached
	 * snapshot. Must be called on the client thread (e.g., in a
	 * {@code GameStateChanged} or {@code ItemContainerChanged} handler).
	 */
	void refresh();

	/**
	 * Clears all cached state (e.g., on logout) so stale equipment is not
	 * served to subsequent logins.
	 */
	void reset();
}
