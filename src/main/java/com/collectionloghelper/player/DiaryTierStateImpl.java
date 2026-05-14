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

import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Varbits;

/**
 * RuneLite-backed implementation of {@link DiaryTierState}.
 * <p>
 * Reads the canonical Achievement Diary completion varbits (one per
 * region-tier pair) via the RuneLite {@link Client} and caches them in an
 * in-memory map. The map is populated once per {@link #refresh()} call and
 * cleared on {@link #reset()}.
 * <p>
 * Varbit IDs are sourced from {@link Varbits} using the
 * {@code DIARY_<REGION>_<TIER>} naming convention. All 48 region-tier
 * combinations are wired explicitly so the compiler catches any future
 * constant renames.
 * <p>
 * Varbit values observed:
 * <ul>
 *   <li>Ardougne:  Easy=4458, Medium=4459, Hard=4460, Elite=4461</li>
 *   <li>Desert:    Easy=4483, Medium=4484, Hard=4485, Elite=4486</li>
 *   <li>Falador:   Easy=4462, Medium=4463, Hard=4464, Elite=4465</li>
 *   <li>Fremennik: Easy=4491, Medium=4492, Hard=4493, Elite=4494</li>
 *   <li>Kandarin:  Easy=4475, Medium=4476, Hard=4477, Elite=4478</li>
 *   <li>Karamja:   Easy=3578, Medium=3599, Hard=3611, Elite=4566</li>
 *   <li>Kourend:   Easy=7925, Medium=7926, Hard=7927, Elite=7928</li>
 *   <li>Lumbridge: Easy=4495, Medium=4496, Hard=4497, Elite=4498</li>
 *   <li>Morytania: Easy=4487, Medium=4488, Hard=4489, Elite=4490</li>
 *   <li>Varrock:   Easy=4479, Medium=4480, Hard=4481, Elite=4482</li>
 *   <li>Western:   Easy=4471, Medium=4472, Hard=4473, Elite=4474</li>
 *   <li>Wilderness:Easy=4466, Medium=4467, Hard=4468, Elite=4469</li>
 * </ul>
 * <p>
 * Reference: RuneLite AchievementDiaryPlugin.java
 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/achievementdiary/AchievementDiaryPlugin.java
 */
@Slf4j
@Singleton
public class DiaryTierStateImpl implements DiaryTierState
{
	/**
	 * Static lookup: region -> tier -> varbit ID.
	 * Two-level EnumMap avoids boxing and provides O(1) access.
	 */
	private static final Map<DiaryRegion, Map<DiaryTier, Integer>> VARBIT_IDS;

	static
	{
		VARBIT_IDS = new EnumMap<>(DiaryRegion.class);

		addRegion(DiaryRegion.ARDOUGNE,
			Varbits.DIARY_ARDOUGNE_EASY,
			Varbits.DIARY_ARDOUGNE_MEDIUM,
			Varbits.DIARY_ARDOUGNE_HARD,
			Varbits.DIARY_ARDOUGNE_ELITE);

		addRegion(DiaryRegion.DESERT,
			Varbits.DIARY_DESERT_EASY,
			Varbits.DIARY_DESERT_MEDIUM,
			Varbits.DIARY_DESERT_HARD,
			Varbits.DIARY_DESERT_ELITE);

		addRegion(DiaryRegion.FALADOR,
			Varbits.DIARY_FALADOR_EASY,
			Varbits.DIARY_FALADOR_MEDIUM,
			Varbits.DIARY_FALADOR_HARD,
			Varbits.DIARY_FALADOR_ELITE);

		addRegion(DiaryRegion.FREMENNIK,
			Varbits.DIARY_FREMENNIK_EASY,
			Varbits.DIARY_FREMENNIK_MEDIUM,
			Varbits.DIARY_FREMENNIK_HARD,
			Varbits.DIARY_FREMENNIK_ELITE);

		addRegion(DiaryRegion.KANDARIN,
			Varbits.DIARY_KANDARIN_EASY,
			Varbits.DIARY_KANDARIN_MEDIUM,
			Varbits.DIARY_KANDARIN_HARD,
			Varbits.DIARY_KANDARIN_ELITE);

		addRegion(DiaryRegion.KARAMJA,
			Varbits.DIARY_KARAMJA_EASY,
			Varbits.DIARY_KARAMJA_MEDIUM,
			Varbits.DIARY_KARAMJA_HARD,
			Varbits.DIARY_KARAMJA_ELITE);

		addRegion(DiaryRegion.KOUREND,
			Varbits.DIARY_KOUREND_EASY,
			Varbits.DIARY_KOUREND_MEDIUM,
			Varbits.DIARY_KOUREND_HARD,
			Varbits.DIARY_KOUREND_ELITE);

		addRegion(DiaryRegion.LUMBRIDGE,
			Varbits.DIARY_LUMBRIDGE_EASY,
			Varbits.DIARY_LUMBRIDGE_MEDIUM,
			Varbits.DIARY_LUMBRIDGE_HARD,
			Varbits.DIARY_LUMBRIDGE_ELITE);

		addRegion(DiaryRegion.MORYTANIA,
			Varbits.DIARY_MORYTANIA_EASY,
			Varbits.DIARY_MORYTANIA_MEDIUM,
			Varbits.DIARY_MORYTANIA_HARD,
			Varbits.DIARY_MORYTANIA_ELITE);

		addRegion(DiaryRegion.VARROCK,
			Varbits.DIARY_VARROCK_EASY,
			Varbits.DIARY_VARROCK_MEDIUM,
			Varbits.DIARY_VARROCK_HARD,
			Varbits.DIARY_VARROCK_ELITE);

		addRegion(DiaryRegion.WESTERN,
			Varbits.DIARY_WESTERN_EASY,
			Varbits.DIARY_WESTERN_MEDIUM,
			Varbits.DIARY_WESTERN_HARD,
			Varbits.DIARY_WESTERN_ELITE);

		addRegion(DiaryRegion.WILDERNESS,
			Varbits.DIARY_WILDERNESS_EASY,
			Varbits.DIARY_WILDERNESS_MEDIUM,
			Varbits.DIARY_WILDERNESS_HARD,
			Varbits.DIARY_WILDERNESS_ELITE);
	}

	private static void addRegion(DiaryRegion region, int easy, int medium, int hard, int elite)
	{
		Map<DiaryTier, Integer> tiers = new EnumMap<>(DiaryTier.class);
		tiers.put(DiaryTier.EASY, easy);
		tiers.put(DiaryTier.MEDIUM, medium);
		tiers.put(DiaryTier.HARD, hard);
		tiers.put(DiaryTier.ELITE, elite);
		VARBIT_IDS.put(region, tiers);
	}

	// -------------------------------------------------------------------------

	private final Client client;

	/** Cached completion flags — populated by refresh(), cleared by reset(). */
	private final Map<DiaryRegion, Map<DiaryTier, Boolean>> cache;

	@Inject
	DiaryTierStateImpl(Client client)
	{
		this.client = client;
		this.cache = new EnumMap<>(DiaryRegion.class);
		for (DiaryRegion region : DiaryRegion.values())
		{
			Map<DiaryTier, Boolean> tierMap = new EnumMap<>(DiaryTier.class);
			for (DiaryTier tier : DiaryTier.values())
			{
				tierMap.put(tier, false);
			}
			cache.put(region, tierMap);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A varbit value of {@code 1} indicates completion, matching the convention
	 * used by the game and RuneLite's AchievementDiaryPlugin.
	 */
	@Override
	public boolean hasDiary(DiaryRegion region, DiaryTier tier)
	{
		if (region == null || tier == null)
		{
			return false;
		}
		Map<DiaryTier, Boolean> tierMap = cache.get(region);
		if (tierMap == null)
		{
			return false;
		}
		Boolean value = tierMap.get(tier);
		return value != null && value;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Reads all 48 diary varbits from the client in a single pass. Must be
	 * called on the client thread.
	 */
	@Override
	public void refresh()
	{
		for (DiaryRegion region : DiaryRegion.values())
		{
			Map<DiaryTier, Integer> varbitMap = VARBIT_IDS.get(region);
			Map<DiaryTier, Boolean> tierCache = cache.get(region);
			for (DiaryTier tier : DiaryTier.values())
			{
				int varbitId = varbitMap.get(tier);
				boolean complete;
				try
				{
					complete = client.getVarbitValue(varbitId) == 1;
				}
				catch (Exception e)
				{
					log.warn("Failed to read diary varbit {} ({} {})", varbitId, region, tier, e);
					complete = false;
				}
				tierCache.put(tier, complete);
			}
		}
		log.debug("DiaryTierState refreshed");
	}

	/** {@inheritDoc} */
	@Override
	public void reset()
	{
		for (Map<DiaryTier, Boolean> tierMap : cache.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				tierMap.put(tier, false);
			}
		}
		log.debug("DiaryTierState reset");
	}
}
