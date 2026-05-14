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

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

/**
 * Skill-cape perks relevant to guidance routing, activity unlocks, or
 * travel shortcuts in the Collection Log Helper.
 *
 * <p>Each constant declares:
 * <ul>
 *   <li>The {@link Skill} whose real level {@code >= 99} indicates that the
 *       player owns (or is eligible to own) the cape and can access the perk
 *       even when the cape is not equipped — the "owned-status fallback".</li>
 *   <li>The set of {@link ItemID} constants representing wearable cape
 *       variants — regular and trimmed. Max-cape variants are <em>not</em>
 *       listed here for individual perks; the implementation handles the Max
 *       cape centrally via {@link SkillCapePerk#MAX_CAPE_TELES}.</li>
 * </ul>
 *
 * <p>Item ID sources: RuneLite {@code ItemID} constants (master, May 2026),
 * cross-checked against the OSRS Wiki skill-cape article.
 *
 * <pre>
 * Perk                    Skill           Regular   Trimmed
 * ----------------------- --------------- --------- ---------
 * CRAFTING_TELE           CRAFTING        9780      9781
 * FARMING_TELE            FARMING         9810      9811
 * MAX_CAPE_TELES          OVERALL         13280     (variants, see below)
 * CONSTRUCTION_TELE       CONSTRUCTION    9789      9790
 * MAGIC_SPELLBOOK_SWAP    MAGIC           9762      9763
 * FISHING_FISH_BARREL     FISHING         9798      9799
 * HUNTER_FIRESTRIKER      HUNTER          9948      9949
 * AGILITY_GRACEFUL_PIECE  AGILITY         9771      9772
 * MINING_CRYSTAL_PICKAXE  MINING          9792      9793
 * WOODCUTTING_CRYSTAL_AXE WOODCUTTING     9807      9808
 * RC_RUNECRAFTING         RUNECRAFT       9765      9766
 * </pre>
 */
public enum SkillCapePerk
{
	/**
	 * Crafting cape teleport: once-per-day (unlimited with trimmed) teleport
	 * to the Crafting Guild.
	 *
	 * <p>Cape IDs: SKILLCAPE_CRAFTING (9780), SKILLCAPE_CRAFTING_TRIMMED (9781).
	 */
	CRAFTING_TELE(
		Skill.CRAFTING,
		ImmutableSet.of(
			ItemID.SKILLCAPE_CRAFTING,         // 9780
			ItemID.SKILLCAPE_CRAFTING_TRIMMED  // 9781
		)
	),

	/**
	 * Farming cape teleport: once-per-day teleport to any farming patch.
	 *
	 * <p>Cape IDs: SKILLCAPE_FARMING (9810), SKILLCAPE_FARMING_TRIMMED (9811).
	 */
	FARMING_TELE(
		Skill.FARMING,
		ImmutableSet.of(
			ItemID.SKILLCAPE_FARMING,         // 9810
			ItemID.SKILLCAPE_FARMING_TRIMMED  // 9811
		)
	),

	/**
	 * Max cape: grants access to all individual skill-cape perks and
	 * teleports via the Max-cape right-click menu.
	 *
	 * <p>Wearable Max-cape variants tracked (SKILLCAPE_MAX_* IDs):
	 * base (13280), worn (13342), Firecape trim (13329), Infernal trim (21285),
	 * Saradomin (13331 / 21776), Zamorak (13333 / 21780),
	 * Guthix (13335 / 21784), Anma (13337), Ardougne (20760),
	 * Assembler (21898).
	 *
	 * <p>The owned-status fallback uses {@link Skill#OVERALL} as a sentinel;
	 * {@link SkillCapePerkStateImpl} skips the level check for this perk and
	 * relies on the cape's presence in the equipment container as the primary
	 * signal.
	 */
	MAX_CAPE_TELES(
		Skill.OVERALL,
		ImmutableSet.of(
			ItemID.SKILLCAPE_MAX,              // 13280
			ItemID.SKILLCAPE_MAX_WORN,         // 13342
			ItemID.SKILLCAPE_MAX_FIRECAPE,     // 13329
			ItemID.SKILLCAPE_MAX_INFERNALCAPE, // 21285
			ItemID.SKILLCAPE_MAX_SARADOMIN,    // 13331
			ItemID.SKILLCAPE_MAX_SARADOMIN2,   // 21776
			ItemID.SKILLCAPE_MAX_ZAMORAK,      // 13333
			ItemID.SKILLCAPE_MAX_ZAMORAK2,     // 21780
			ItemID.SKILLCAPE_MAX_GUTHIX,       // 13335
			ItemID.SKILLCAPE_MAX_GUTHIX2,      // 21784
			ItemID.SKILLCAPE_MAX_ANMA,         // 13337
			ItemID.SKILLCAPE_MAX_ARDY,         // 20760
			ItemID.SKILLCAPE_MAX_ASSEMBLER     // 21898
		)
	),

	/**
	 * Construction cape teleport: unlimited free teleport to player-owned house.
	 *
	 * <p>Cape IDs: SKILLCAPE_CONSTRUCTION (9789), SKILLCAPE_CONSTRUCTION_TRIMMED (9790).
	 */
	CONSTRUCTION_TELE(
		Skill.CONSTRUCTION,
		ImmutableSet.of(
			ItemID.SKILLCAPE_CONSTRUCTION,         // 9789
			ItemID.SKILLCAPE_CONSTRUCTION_TRIMMED  // 9790
		)
	),

	/**
	 * Magic cape perk: switch spellbook at will without visiting an altar.
	 *
	 * <p>Cape IDs: SKILLCAPE_MAGIC (9762), SKILLCAPE_MAGIC_TRIMMED (9763).
	 */
	MAGIC_SPELLBOOK_SWAP(
		Skill.MAGIC,
		ImmutableSet.of(
			ItemID.SKILLCAPE_MAGIC,         // 9762
			ItemID.SKILLCAPE_MAGIC_TRIMMED  // 9763
		)
	),

	/**
	 * Fishing cape perk: access to the Fish Barrel (stores 28 fish)
	 * while the cape is worn.
	 *
	 * <p>Cape IDs: SKILLCAPE_FISHING (9798), SKILLCAPE_FISHING_TRIMMED (9799).
	 */
	FISHING_FISH_BARREL(
		Skill.FISHING,
		ImmutableSet.of(
			ItemID.SKILLCAPE_FISHING,         // 9798
			ItemID.SKILLCAPE_FISHING_TRIMMED  // 9799
		)
	),

	/**
	 * Hunter cape perk: access to the Firestriker lighting tool (functions
	 * as both tinderbox and fire-starter).
	 *
	 * <p>Cape IDs: SKILLCAPE_HUNTING (9948), SKILLCAPE_HUNTING_TRIMMED (9949).
	 */
	HUNTER_FIRESTRIKER(
		Skill.HUNTER,
		ImmutableSet.of(
			ItemID.SKILLCAPE_HUNTING,         // 9948
			ItemID.SKILLCAPE_HUNTING_TRIMMED  // 9949
		)
	),

	/**
	 * Agility cape perk: any single piece of Graceful equipped grants the
	 * full-set passive run-energy regeneration bonus.
	 *
	 * <p>Cape IDs: SKILLCAPE_AGILITY (9771), SKILLCAPE_AGILITY_TRIMMED (9772).
	 */
	AGILITY_GRACEFUL_PIECE(
		Skill.AGILITY,
		ImmutableSet.of(
			ItemID.SKILLCAPE_AGILITY,         // 9771
			ItemID.SKILLCAPE_AGILITY_TRIMMED  // 9772
		)
	),

	/**
	 * Mining cape perk: Crystal Pickaxe stored in the cape (summon on demand);
	 * also unlocks a teleport to the Mining Guild.
	 *
	 * <p>Cape IDs: SKILLCAPE_MINING (9792), SKILLCAPE_MINING_TRIMMED (9793).
	 */
	MINING_CRYSTAL_PICKAXE(
		Skill.MINING,
		ImmutableSet.of(
			ItemID.SKILLCAPE_MINING,         // 9792
			ItemID.SKILLCAPE_MINING_TRIMMED  // 9793
		)
	),

	/**
	 * Woodcutting cape perk: Crystal Axe stored in the cape (summon on demand).
	 *
	 * <p>Cape IDs: SKILLCAPE_WOODCUTTING (9807), SKILLCAPE_WOODCUTTING_TRIMMED (9808).
	 */
	WOODCUTTING_CRYSTAL_AXE(
		Skill.WOODCUTTING,
		ImmutableSet.of(
			ItemID.SKILLCAPE_WOODCUTTING,         // 9807
			ItemID.SKILLCAPE_WOODCUTTING_TRIMMED  // 9808
		)
	),

	/**
	 * Runecrafting cape perk: unlimited teleports to the Runecrafting Guild
	 * and a free daily Essence Pouch repair.
	 *
	 * <p>Cape IDs: SKILLCAPE_RUNECRAFTING (9765), SKILLCAPE_RUNECRAFTING_TRIMMED (9766).
	 */
	RC_RUNECRAFTING(
		Skill.RUNECRAFT,
		ImmutableSet.of(
			ItemID.SKILLCAPE_RUNECRAFTING,         // 9765
			ItemID.SKILLCAPE_RUNECRAFTING_TRIMMED  // 9766
		)
	);

	// -------------------------------------------------------------------------

	/**
	 * The RuneLite {@link Skill} whose real level {@code >= 99} activates
	 * the owned-status fallback in {@link SkillCapePerkStateImpl}.
	 *
	 * <p>{@link Skill#OVERALL} is used as a sentinel for {@link #MAX_CAPE_TELES}
	 * because the Max cape requires all 23 skills at 99 — the implementation
	 * must handle this case explicitly.
	 */
	private final Skill skill;

	/**
	 * Immutable set of item IDs for wearable variants of this perk's cape
	 * (regular and trimmed only). Max-cape variants are centralised on
	 * {@link #MAX_CAPE_TELES}.
	 */
	private final Set<Integer> capeItemIds;

	SkillCapePerk(Skill skill, Set<Integer> capeItemIds)
	{
		this.skill = skill;
		this.capeItemIds = capeItemIds;
	}

	/** Returns the skill whose level-99 real level implies cape ownership. */
	public Skill getSkill()
	{
		return skill;
	}

	/** Returns the immutable set of wearable cape item IDs for this perk. */
	public Set<Integer> getCapeItemIds()
	{
		return capeItemIds;
	}
}
