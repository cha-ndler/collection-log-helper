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
package com.collectionloghelper.data;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

@Value
public class SourceRequirements
{
	List<String> quests;
	List<SkillRequirement> skills;
	/**
	 * Achievement diary requirements, e.g. {@code "LUMBRIDGE_HARD"}, {@code "KANDARIN_ELITE"}.
	 * Format: {@code <AREA>_<TIER>} where TIER is EASY, MEDIUM, HARD, or ELITE.
	 * May be {@code null} when the source has no diary prerequisites.
	 */
	List<String> diaries;

	/**
	 * POH-fixture teleport requirements, e.g. {@code "MOUNTED_GLORY"},
	 * {@code "JEWELLERY_BOX_FANCY"}. Each entry must be a
	 * {@link com.collectionloghelper.player.PohTeleport} enum constant name.
	 *
	 * <p>Evaluated AND-wise against
	 * {@code PohTeleportInventory.hasTeleport(...)}: the requirement is met
	 * only when every listed teleport is available. Unrecognised names are
	 * treated as unmet and logged.
	 *
	 * <p>{@code null} or empty when the source/alternative has no POH-fixture
	 * prerequisite. Added by Tier B0 as a C6 prerequisite.
	 */
	@Nullable
	List<String> pohTeleports;

	/**
	 * Equipped-item requirements expressed as RuneLite {@code ItemID} integers.
	 *
	 * <p>Evaluated AND-wise against
	 * {@code EquippedItemState.hasEquipped(int)}: the requirement is met
	 * only when every listed item ID is currently in the player's equipment
	 * container.
	 *
	 * <p>Charge-aware fallback (e.g. an equipped Drakan's medallion at 0
	 * charges) is the responsibility of the authoring layer — a conditional
	 * alternative that depends on a chargeable equip should always be paired
	 * with a non-equipped fallback alternative. See C6 §5 Q4 in
	 * {@code docs/contributor-guide/c6-top-20-player-state-wiring-scoping.md}.
	 *
	 * <p>{@code null} or empty when the source/alternative has no equipped-item
	 * prerequisite. Added by Tier B0 as a C6 prerequisite.
	 */
	@Nullable
	List<Integer> equippedItemIds;

	/**
	 * Inventory-item requirements expressed as RuneLite {@code ItemID} integers.
	 *
	 * <p>Evaluated AND-wise against
	 * {@code PlayerInventoryState.hasItem(int)}: the requirement is met only
	 * when every listed item ID is currently present in the player's regular
	 * inventory container. {@code null} entries inside the list are skipped.
	 *
	 * <p>Use this field for teleport vectors that are right-clicked from the
	 * inventory regardless of whether the item is wieldable. Examples:
	 * Pharaoh's sceptre's Jaltevas teleport, Crystal teleport seed, Quetzal
	 * whistle, and Zul-andra teleport scroll all check the player's inventory,
	 * not the equipment container. Wieldable jewellery whose canonical
	 * teleport is from the equipment slot (Drakan's medallion, Ring of
	 * Shadows) should continue to use {@code equippedItemIds} instead.
	 *
	 * <p>For an OR-semantic over multiple charge-tier variants of the same
	 * item (e.g. Pharaoh's sceptre charges 1-5), a future
	 * {@code inventoryItemIdsAny} field is planned but not yet authored.
	 *
	 * <p>{@code null} or empty when the source/alternative has no
	 * inventory-item prerequisite. Added by the Wave-1 follow-up to close the
	 * inventory-vs-equipped distinction left open by Tier B0.
	 */
	@Nullable
	List<Integer> inventoryItemIds;
}
