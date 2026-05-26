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
	 * item (e.g. Pharaoh's sceptre charges 1-5), use the companion
	 * {@link #inventoryItemIdsAny} field instead.
	 *
	 * <p>{@code null} or empty when the source/alternative has no
	 * inventory-item prerequisite. Added by the Wave-1 follow-up to close the
	 * inventory-vs-equipped distinction left open by Tier B0.
	 */
	@Nullable
	List<Integer> inventoryItemIds;

	/**
	 * Inventory-item requirements expressed as RuneLite {@code ItemID} integers
	 * with OR-semantics across the listed IDs.
	 *
	 * <p>Evaluated via {@code PlayerInventoryState.hasItem(int)} as an
	 * any-match: the requirement is met when the player's regular inventory
	 * contains AT LEAST ONE of the listed item IDs. {@code null} entries
	 * inside the list are skipped. An empty list is treated as vacuously
	 * satisfied to match the existing pattern on the AND-semantic siblings
	 * ({@link #pohTeleports}, {@link #equippedItemIds},
	 * {@link #inventoryItemIds}).
	 *
	 * <p>Use this field for inventory-teleport vectors that have multiple
	 * functionally-equivalent charge-tier or revision variants, all of which
	 * should satisfy the same requirement. Examples:
	 * <ul>
	 *   <li>Pharaoh's sceptre charges 1-5 plus Jeweled (6 IDs) for Tombs of
	 *       Amascut's Jaltevas teleport;</li>
	 *   <li>Quetzal whistle basic / enhanced / perfected / perfected-infinite
	 *       for Phantom Muspah;</li>
	 *   <li>Teleport crystal charge-tier variants for The Gauntlet and
	 *       Corrupted Gauntlet.</li>
	 * </ul>
	 *
	 * <p>The OR clause AND-combines with every other field on
	 * {@link SourceRequirements}, including {@link #inventoryItemIds}: a
	 * source that sets both lists requires every ID in
	 * {@code inventoryItemIds} AND at least one ID from
	 * {@code inventoryItemIdsAny}.
	 *
	 * <p>{@code null} or empty when the source/alternative has no
	 * any-of-inventory prerequisite. Added by the Wave-7c follow-up to mirror
	 * the AND-semantic {@link #inventoryItemIds} added in the prior PR.
	 */
	@Nullable
	List<Integer> inventoryItemIdsAny;

	/**
	 * Quest-milestone requirements expressed as RuneLite {@code Quest} enum
	 * constant names, e.g. {@code "FAIRYTALE_II__CURE_A_QUEEN"}.
	 *
	 * <p>Looser than the {@link #quests} field: where {@code quests} requires
	 * the quest to be {@code FINISHED}, an entry here is satisfied as soon as
	 * the quest has been STARTED. Evaluated AND-wise via
	 * {@code Quest.valueOf(name).getState(client)}: the requirement is met only
	 * when every listed quest is {@code IN_PROGRESS} or {@code FINISHED} (i.e.
	 * not {@code NOT_STARTED}). This covers the case where requiring full
	 * completion is over-restrictive — a travel vector or unlock that becomes
	 * available the moment the quest is begun.
	 *
	 * <p>Unrecognised names are treated as unmet and logged, mirroring the
	 * fail-closed resolution used by {@link #quests} and {@link #pohTeleports}.
	 *
	 * <p>{@code null} or empty when the source/alternative has no
	 * quest-started prerequisite. Added by the D5 schema PR; data wiring
	 * stacks on this in a later PR.
	 */
	@Nullable
	List<String> questMilestones;

	/**
	 * Skill-cape travel-perk requirements expressed as RuneLite {@code Skill}
	 * enum constant names, e.g. {@code "CRAFTING"}, {@code "FARMING"},
	 * {@code "SLAYER"}, {@code "HUNTER"}, {@code "CONSTRUCTION"}.
	 * (Note: the runecrafting constant is {@code RUNECRAFT}, not
	 * {@code RUNECRAFTING}.)
	 *
	 * <p>A skill-cape teleport/travel perk is available only to a player who
	 * owns the corresponding cape, and owning the cape requires level 99 in
	 * that skill. This field uses that level-99 threshold as a proxy for cape
	 * ownership: it is the necessary condition, not proof the player has
	 * actually purchased and is wearing the cape. Evaluated AND-wise via
	 * {@code client.getRealSkillLevel(Skill.valueOf(name))}: the requirement
	 * is met only when the player's level in every listed skill is
	 * {@code >= 99}.
	 *
	 * <p>Unrecognised names are treated as unmet and logged, mirroring the
	 * fail-closed resolution used by {@link #skills} and {@link #pohTeleports}.
	 *
	 * <p>{@code null} or empty when the source/alternative has no
	 * skill-cape-perk prerequisite. Added by the D5 schema PR; data wiring
	 * stacks on this in a later PR.
	 */
	@Nullable
	List<String> skillCapePerks;
}
