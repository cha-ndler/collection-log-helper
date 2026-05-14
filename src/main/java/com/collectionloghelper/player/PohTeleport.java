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
 * Enumeration of player-owned house teleport capabilities that can be detected
 * via varbits or manually declared in plugin config.
 *
 * <p>These represent fixtures built inside the POH that provide teleport access
 * (jewellery box tiers, superior garden features, portal nexus, and mounted
 * jewellery inside the portal nexus room). They are distinct from the portable
 * teleport items already tracked by
 * {@link com.collectionloghelper.data.PlayerTravelCapabilities}.
 *
 * <p>Detection strategy per D-02 (aggressive detection with fallbacks):
 * <ul>
 *   <li>Varbit-detected positive <b>OR</b> config-override positive means the
 *       teleport is considered available.</li>
 *   <li>Both must be negative/zero/false for the teleport to be considered
 *       unavailable.</li>
 * </ul>
 *
 * <p>Jewellery box tiers are ordered by capability (BASIC &lt; FANCY &lt;
 * ORNATE). Callers that need to enumerate available destinations should check
 * the highest available tier.
 *
 * <p>Wiring into Tier B guidance branching is deferred to milestone C6; this
 * enum and its backing service are intentionally standalone for C1.
 */
public enum PohTeleport
{
	// -------------------------------------------------------------------------
	// Mounted Amulet of Glory (Throne Room / Skill Hall decoration hotspot)
	//   Construction level: 47
	//   Teleports: Edgeville, Karamja, Draynor Village, Al Kharid (unlimited)
	//
	//   No dedicated varbit for this decoration is reliably readable outside the
	//   house; manual config override is the primary detection path.
	// -------------------------------------------------------------------------

	/**
	 * Mounted Amulet of Glory in the POH (Construction level 47).
	 * Provides unlimited Edgeville, Karamja, Draynor, and Al Kharid teleports.
	 * No reliable varbit outside the house — manual config is primary.
	 */
	MOUNTED_GLORY,

	// -------------------------------------------------------------------------
	// Jewellery box (Achievement Gallery — jewellery box space)
	//   Construction levels: 81 / 86 / 91
	//   Varbit 2308 (POH_JEWELLERY_BOX): 0 = absent, 1 = Basic, 2 = Fancy,
	//                                    3 = Ornate
	//   Basic:  Games necklace + Ring of dueling
	//   Fancy:  + Skills necklace + Combat bracelet
	//   Ornate: + Amulet of glory + Ring of wealth
	// -------------------------------------------------------------------------

	/**
	 * Basic jewellery box (Construction level 81).
	 * Provides unlimited Games necklace and Ring of dueling teleports.
	 * Detected via varbit 2308 &gt;= 1.
	 */
	JEWELLERY_BOX_BASIC,

	/**
	 * Fancy jewellery box (Construction level 86).
	 * Includes Basic destinations plus Skills necklace and Combat bracelet.
	 * Detected via varbit 2308 &gt;= 2.
	 */
	JEWELLERY_BOX_FANCY,

	/**
	 * Ornate jewellery box (Construction level 91).
	 * Includes Fancy destinations plus Amulet of glory and Ring of wealth.
	 * Detected via varbit 2308 &gt;= 3.
	 */
	JEWELLERY_BOX_ORNATE,

	// -------------------------------------------------------------------------
	// Superior garden — teleport space
	//   Spirit tree:  Construction 75 + Farming 83 (requires Tree Gnome Village)
	//   Fairy ring:   Construction 85 (requires starting Fairytale II)
	//
	//   No dedicated varbit is reliably readable outside the house for either
	//   fixture. PlayerTravelCapabilities tracks the quest prerequisite
	//   separately; these entries model the physical POH fixture.
	// -------------------------------------------------------------------------

	/**
	 * Spirit tree in the POH superior garden (Construction 75, Farming 83).
	 * Uses the spirit tree network; requires Tree Gnome Village quest.
	 * No reliable varbit outside the house — manual config is primary.
	 */
	SPIRIT_TREE,

	/**
	 * Fairy ring in the POH superior garden (Construction 85).
	 * Uses the fairy ring network; requires starting Fairytale II.
	 * No reliable varbit outside the house — manual config is primary.
	 */
	FAIRY_RING,

	// -------------------------------------------------------------------------
	// Portal nexus (standalone room — any tier)
	//   Construction levels: 72 (Marble), 82 (Gilded), 92 (Crystalline)
	//   Varbit 6670 (POH_PORTAL_NEXUS): 0 = absent, 1 = Marble, 2 = Gilded,
	//                                   3 = Crystalline
	//
	//   For C1 we track only whether a nexus exists (any tier), not individual
	//   destination slots. A non-zero varbit means at least a Marble nexus is
	//   present; specific destinations are out of scope until C6.
	// -------------------------------------------------------------------------

	/**
	 * Portal nexus in the POH (any tier: Marble/Gilded/Crystalline).
	 * Provides configurable standard-spellbook teleports.
	 * Detected via varbit 6670 != 0.
	 */
	PORTAL_NEXUS,

	// -------------------------------------------------------------------------
	// Mounted jewellery in the portal nexus room (amulet space hotspots)
	//   Both require the portal nexus room to exist first.
	//
	//   Mounted digsite pendant:  Construction 82
	//     Varbit 6651 (POH_MOUNTED_DIGSITE): non-zero when mounted
	//     Teleports: Digsite, Fossil Island, Lithkren Vault
	//
	//   Mounted Xeric's talisman: Construction 72
	//     Varbit 6617 (POH_MOUNTED_XERICS): non-zero when mounted
	//     Teleports: Xeric's Glade, Lookout, Inferno, Heart, Honour
	// -------------------------------------------------------------------------

	/**
	 * Mounted digsite pendant in the portal nexus room (Construction 82).
	 * Provides Digsite, Fossil Island, and Lithkren Vault teleports.
	 * Detected via varbit 6651 != 0.
	 */
	DIGSITE_PENDANT,

	/**
	 * Mounted Xeric's talisman in the portal nexus room (Construction 72).
	 * Provides Xeric's Glade, Lookout, Inferno, Heart, and Honour teleports.
	 * Detected via varbit 6617 != 0.
	 */
	XERICS_TALISMAN,
}
