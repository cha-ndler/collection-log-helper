/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * The port index -> dock world-coordinate table below is factual OSRS game data.
 * The index/coordinate values were cross-referenced against the public,
 * BSD-2-Clause-licensed "Where's My Boat" plugin
 * (https://github.com/ArchRBX/wheresmyboat, (c) 2025 nucleon, Cooper Morris,
 * AtchRBX), whose enums/Port.java enumerates each Sailing port with its
 * navigation WorldPoint. Attribution retained per BSD-2-Clause.
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
package com.collectionloghelper.sailing;

import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

/**
 * Enumerates every OSRS Sailing port/dock, keyed by the port index stored in the
 * per-boat {@code SAILING_BOAT_N_PORT} varbit, with the dock's navigation
 * {@link WorldPoint}. Used by {@link SailingDockResolver} to turn a boat's
 * current port index into a real-world coordinate the guidance system can point
 * at.
 *
 * <p>The port index is the value the game writes to {@code SAILING_BOAT_N_PORT};
 * it is stable across the sailing-boat slots.
 */
public enum SailingPort
{
	PORT_SARIM(0, "Port Sarim", 1, 3050, 3192),
	THE_PANDEMONIUM(1, "The Pandemonium", 1, 3069, 2986),
	LANDS_END(2, "Land's End", 5, 1506, 3402),
	MUSA_POINT(3, "Musa Point", 10, 2960, 3147),
	HOSIDIUS(4, "Hosidius", 5, 1726, 3452),
	RIMMINGTON(5, "Rimmington", 18, 2905, 3226),
	CATHERBY(6, "Catherby", 20, 2796, 3412),
	PORT_PISCARILIUS(7, "Port Piscarilius", 15, 1845, 3687),
	BRIMHAVEN(8, "Brimhaven", 25, 2757, 3229),
	ARDOUGNE(9, "Ardougne", 28, 2671, 3265),
	PORT_KHAZARD(10, "Port Khazard", 30, 2685, 3161),
	WITCHAVEN(11, "Witchaven", 34, 2746, 3304),
	ENTRANA(12, "Entrana", 36, 2878, 3335),
	CIVITAS_ILLA_FORTIS(13, "Civitas illa Fortis", 38, 1774, 3141),
	CORSAIR_COVE(14, "Corsair Cove", 40, 2579, 2843),
	CAIRN_ISLE(15, "Cairn Isle", 42, 2749, 2951),
	SUNSET_COAST(16, "Sunset Coast", 44, 1511, 2975),
	THE_SUMMER_SHORE(17, "The Summer Shore", 45, 3174, 2367),
	ALDARIN(18, "Aldarin", 46, 1452, 2970),
	RUINS_OF_UNKAH(19, "Ruins of Unkah", 48, 3143, 2824),
	VOID_KNIGHTS_OUTPOST(20, "Void Knights' Outpost", 50, 2651, 2678),
	PORT_ROBERTS(21, "Port Roberts", 50, 1860, 3306),
	RED_ROCK(22, "Red Rock", 52, 2752, 2496),
	RELLEKKA(23, "Rellekka", 62, 2630, 3705),
	ETCETERIA(24, "Etceteria", 65, 2611, 3840),
	PORT_TYRAS(25, "Port Tyras", 66, 2144, 3120),
	DEEPFIN_POINT(26, "Deepfin Point", 67, 1923, 2758),
	JATIZSO(27, "Jatizso", 68, 2412, 3780),
	NEITIZNOT(28, "Neitiznot", 68, 2308, 3783),
	PRIFDDINAS(29, "Prifddinas", 70, 2158, 3324),
	PISCATORIS(30, "Piscatoris", 75, 2303, 3690),
	LUNAR_ISLE(31, "Lunar Isle", 76, 2151, 3880),
	ISLE_OF_SOULS(32, "Isle of Souls", 55, 2282, 2823),
	WATERBIRTH_ISLAND(33, "Waterbirth Island", 74, 2543, 3765),
	WEISS(34, "Weiss", 80, 2860, 3972),
	DOGNOSE_ISLAND(35, "Dognose Island", 40, 3061, 2639),
	REMOTE_ISLAND(36, "Remote Island", 45, 2971, 2603),
	THE_LITTLE_PEARL(37, "The Little Pearl", 45, 3354, 2216),
	THE_ONYX_CREST(38, "The Onyx Crest", 47, 2997, 2288),
	LAST_LIGHT(39, "Last Light", 52, 2850, 2331),
	CHARRED_ISLAND(40, "Charred Island", 60, 2660, 2395),
	VATRACHOS_ISLAND(41, "Vatrachos Island", 46, 1872, 2985),
	ANGLERS_RETREAT(42, "Anglers' Retreat", 51, 2467, 2721),
	MINOTAURS_REST(43, "Minotaurs' Rest", 54, 1958, 3117),
	ISLE_OF_BONES(44, "Isle of Bones", 56, 2532, 2531),
	TEAR_OF_THE_SOUL(45, "Tear of the Soul", 61, 2318, 2774),
	WINTUMBER_ISLAND(46, "Wintumber Island", 63, 2058, 2606),
	THE_CROWN_JEWEL(47, "The Crown Jewel", 64, 1765, 2659),
	RAINBOWS_END(48, "Rainbow's End", 69, 2344, 2270),
	SUNBLEAK_ISLAND(49, "Sunbleak Island", 72, 2189, 2327),
	SHIMMERING_ATOLL(50, "Shimmering Atoll", 49, 1557, 2771),
	LAGUNA_AURORAE(51, "Laguna Aurorae", 58, 1202, 2733),
	CHINCHOMPA_ISLAND(52, "Chinchompa Island", 42, 1892, 3429),
	LLEDRITH_ISLAND(53, "Lledrith Island", 66, 2097, 3188),
	YNYSDAIL(54, "Ynysdail", 73, 2222, 3466),
	BUCCANEERS_HAVEN(55, "Buccaneers' Haven", 76, 2080, 3690),
	DRUMSTICK_ISLE(56, "Drumstick Isle", 79, 2150, 3530),
	BRITTLE_ISLE(57, "Brittle Isle", 81, 1954, 4056),
	GRIMSTONE(58, "Grimstone", 87, 2927, 4056);

	private final int portId;
	private final String displayName;
	private final int sailingLevelRequired;
	private final int worldX;
	private final int worldY;

	SailingPort(int portId, String displayName, int sailingLevelRequired, int worldX, int worldY)
	{
		this.portId = portId;
		this.displayName = displayName;
		this.sailingLevelRequired = sailingLevelRequired;
		this.worldX = worldX;
		this.worldY = worldY;
	}

	public int getPortId()
	{
		return portId;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getSailingLevelRequired()
	{
		return sailingLevelRequired;
	}

	/**
	 * @return the dock's navigation tile (all Sailing docks are on plane 0).
	 */
	public WorldPoint getDock()
	{
		return new WorldPoint(worldX, worldY, 0);
	}

	/**
	 * Resolves a {@code SAILING_BOAT_N_PORT} varbit value to its port.
	 *
	 * @param portId the port index read from the varbit
	 * @return the matching port, or empty if the index is unknown
	 */
	public static Optional<SailingPort> fromId(int portId)
	{
		for (SailingPort p : values())
		{
			if (p.portId == portId)
			{
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}
}
