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
 * Named sub-milestones within quests that gate guidance access, travel, or
 * unlock content before the quest is fully complete.
 *
 * <p>This is a curated set focused on milestones that are practically useful
 * for routing guidance — not every step of every quest. Each constant documents
 * the varplayer/varbit that backs it and what capability it unlocks.
 *
 * <p>Groups:
 * <ul>
 *   <li>LOST_CITY_* — dramen staff and fairy ring access</li>
 *   <li>PLAGUE_CITY_*, BIOHAZARD_* — Ardougne access</li>
 *   <li>FAIRYTALE_II_* — fairy ring network</li>
 *   <li>CHILDREN_OF_THE_SUN_* — Varlamore region access</li>
 *   <li>RFD_* — Recipe for Disaster sub-quest unlock tiers (Culinaromancer's
 *     Chest access and Barrows gloves)</li>
 * </ul>
 */
public enum QuestSubMilestone
{
	// -------------------------------------------------------------------------
	// Lost City (varplayer 147)
	// -------------------------------------------------------------------------

	/**
	 * The dramen branch has been cut from the Dramen tree (varplayer 147 >= 4).
	 * The player can craft a dramen staff and therefore reach Zanaris, but the
	 * quest is not yet finished (entering the shed completes it at value 6).
	 * Practical use: fairy ring access only requires the staff, not quest
	 * completion.
	 *
	 * <p>Source: OSRS Wiki — RuneScape:Varplayer/147 value 4.
	 */
	LOST_CITY_DRAMEN_BRANCH_CUT,

	/**
	 * The first dramen staff has been crafted (varplayer 147 >= 5).
	 * The player holds a dramen staff and can equip it to use fairy rings
	 * (when Fairytale II is started).
	 *
	 * <p>Source: OSRS Wiki — RuneScape:Varplayer/147 value 5.
	 */
	LOST_CITY_DRAMEN_STAFF_CRAFTED,

	/**
	 * Lost City is fully completed (varplayer 147 == 6; QuestState.FINISHED).
	 * Zanaris is accessible; the player is ready for Fairytale I and II.
	 */
	LOST_CITY_COMPLETE,

	// -------------------------------------------------------------------------
	// Plague City (varplayer 165) and Biohazard (varplayer 68)
	// -------------------------------------------------------------------------

	/**
	 * Plague City is complete (QuestState.FINISHED on Quest.PLAGUE_CITY).
	 * Grants access to West Ardougne and unlocks the Ardougne Teleport spell
	 * (Magic 51+). The gate between East and West Ardougne is passable.
	 *
	 * <p>Source: OSRS Wiki — RuneScape:Varplayer/165.
	 */
	PLAGUE_CITY_COMPLETE,

	/**
	 * Biohazard is complete (QuestState.FINISHED on Quest.BIOHAZARD).
	 * Continues the Elf quest series; also a prerequisite for several RFD
	 * sub-quests (Freeing the Lumbridge Guide) and Ardougne diary tasks.
	 *
	 * <p>Source: OSRS Wiki — RuneScape:Varplayer/68.
	 */
	BIOHAZARD_COMPLETE,

	// -------------------------------------------------------------------------
	// Fairytale II — Cure a Queen (varbit tracked via Quest enum, id 47)
	// -------------------------------------------------------------------------

	/**
	 * Fairytale II has been started (QuestState.IN_PROGRESS or FINISHED).
	 * Once the quest is in progress, the player can use fairy rings with a
	 * dramen or lunar staff. This is the primary check used by
	 * {@link com.collectionloghelper.data.PlayerTravelCapabilities} for fairy
	 * ring access.
	 *
	 * <p>Source: OSRS Wiki — Fairytale II completion notes; RuneLite Quest
	 * enum id 47 (FAIRYTALE_II__CURE_A_QUEEN).
	 */
	FAIRYTALE_II_FAIRY_RINGS_UNLOCKED,

	// -------------------------------------------------------------------------
	// Children of the Sun (Quest enum id 3450)
	// -------------------------------------------------------------------------

	/**
	 * Children of the Sun is complete (QuestState.FINISHED on
	 * Quest.CHILDREN_OF_THE_SUN). Grants access to the Varlamore region via
	 * the Quetzal Transport System and Varlamore fairy ring destinations.
	 * Required for all Twilight Emissaries series quests.
	 *
	 * <p>Source: OSRS Wiki — Children of the Sun; RuneLite Quest enum id 3450.
	 */
	CHILDREN_OF_THE_SUN_COMPLETE,

	// -------------------------------------------------------------------------
	// Recipe for Disaster sub-quests
	// Each sub-quest uses a separate Quest enum entry and grants progressive
	// access to Culinaromancer's Chest tiers (food + equipment).
	// -------------------------------------------------------------------------

	/**
	 * RFD — Another Cook's Quest complete (Quest.RECIPE_FOR_DISASTER__ANOTHER_COOKS_QUEST).
	 * Grants initial Culinaromancer's Chest access (tier 0 — basic food
	 * items). Prerequisite for all eight council-member sub-quests.
	 *
	 * <p>Source: RuneLite Quest enum id 2307.
	 */
	RFD_ANOTHER_COOKS_QUEST_COMPLETE,

	/**
	 * RFD — Freeing the Mountain Dwarf complete
	 * (Quest.RECIPE_FOR_DISASTER__MOUNTAIN_DWARF).
	 * Grants Culinaromancer's Chest tier 1 (iron gloves, bronze-iron food).
	 *
	 * <p>Source: RuneLite Quest enum id 2308.
	 */
	RFD_FREEING_MOUNTAIN_DWARF_COMPLETE,

	/**
	 * RFD — Freeing Wartface and Bentnoze (Goblin generals) complete
	 * (Quest.RECIPE_FOR_DISASTER__WARTFACE__BENTNOZE).
	 * Grants Culinaromancer's Chest tier 2.
	 *
	 * <p>Source: RuneLite Quest enum id 2309.
	 */
	RFD_FREEING_GOBLIN_GENERALS_COMPLETE,

	/**
	 * RFD — Freeing Pirate Pete complete
	 * (Quest.RECIPE_FOR_DISASTER__PIRATE_PETE).
	 * Sub-quest tracked internally via varbit 1895 (complete = 110).
	 * Grants Culinaromancer's Chest tier 3 (steel gloves).
	 *
	 * <p>Source: RuneLite Quest enum id 2310; OSRS Wiki RuneScape:Varbit/1895.
	 */
	RFD_FREEING_PIRATE_PETE_COMPLETE,

	/**
	 * RFD — Freeing the Lumbridge Guide complete
	 * (Quest.RECIPE_FOR_DISASTER__LUMBRIDGE_GUIDE).
	 * Grants Culinaromancer's Chest tier 4 (black gloves).
	 * Requires Biohazard and several other quests.
	 *
	 * <p>Source: RuneLite Quest enum id 2311.
	 */
	RFD_FREEING_LUMBRIDGE_GUIDE_COMPLETE,

	/**
	 * RFD — Freeing Evil Dave complete
	 * (Quest.RECIPE_FOR_DISASTER__EVIL_DAVE).
	 * Grants Culinaromancer's Chest tier 5 (mithril gloves).
	 *
	 * <p>Source: RuneLite Quest enum id 2312.
	 */
	RFD_FREEING_EVIL_DAVE_COMPLETE,

	/**
	 * RFD — Freeing Skrach Uglogwee complete
	 * (Quest.RECIPE_FOR_DISASTER__SKRACH_UGLOGWEE).
	 * Grants Culinaromancer's Chest tier 6 (adamant gloves).
	 *
	 * <p>Source: RuneLite Quest enum id 2313.
	 */
	RFD_FREEING_SKRACH_UGLOGWEE_COMPLETE,

	/**
	 * RFD — Freeing Sir Amik Varze complete
	 * (Quest.RECIPE_FOR_DISASTER__SIR_AMIK_VARZE).
	 * Grants Culinaromancer's Chest tier 7 (rune gloves).
	 * Requires Lost City and partial Legends' Quest.
	 *
	 * <p>Source: RuneLite Quest enum id 2314.
	 */
	RFD_FREEING_SIR_AMIK_VARZE_COMPLETE,

	/**
	 * RFD — Freeing King Awowogei complete
	 * (Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI).
	 * Grants Culinaromancer's Chest tier 8 (dragon gloves).
	 *
	 * <p>Source: RuneLite Quest enum id 2315.
	 */
	RFD_FREEING_KING_AWOWOGEI_COMPLETE,

	/**
	 * RFD fully complete — Culinaromancer defeated
	 * (Quest.RECIPE_FOR_DISASTER__CULINAROMANCER).
	 * Grants full Culinaromancer's Chest access including Barrows gloves.
	 *
	 * <p>Source: RuneLite Quest enum id 2316.
	 */
	RFD_CULINAROMANCER_DEFEATED,
}
