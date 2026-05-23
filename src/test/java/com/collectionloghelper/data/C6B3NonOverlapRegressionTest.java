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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C6 B3 (non-B2-overlap) regression guard — locks the C3 diary-tier
 * conditional alternatives shipped against the three top-20 sources that do
 * not overlap with the parallel B2 (POH-teleport) batch:
 *
 * <ul>
 *   <li>Tombs of Amascut + Tombs of Amascut (300 Invocation):
 *       {@code DESERT_HARD} unlocks unlimited-charge Pharaoh's sceptre.</li>
 *   <li>The Gauntlet + Corrupted Gauntlet:
 *       {@code WESTERN_ELITE} unlocks the cape Crystal-teleport variant
 *       (Prifddinas teleport rubbed onto max/quest/achievement cape).</li>
 * </ul>
 *
 * <p>The three CoX-Kourend / Graardor-Fremennik / Vorkath-Fremennik diary
 * rows overlap with B2's POH-teleport authoring and are deferred to a
 * post-B2 follow-up PR.
 *
 * <p>Each source's first guidance step must carry at least one
 * {@code conditionalAlternatives} entry whose {@code requirements.diaries}
 * exactly matches the expected diary string, and which overrides both
 * {@code description} and {@code travelTip}. Pattern mirrors
 * {@code B1aRegressionTest} (ring of shadows) and {@code B1bRegressionTest}
 * (Drakan's medallion).
 *
 * <p>Diary string format follows the existing {@code SourceRequirements.diaries}
 * convention {@code <AREA>_<TIER>}, where {@code AREA} is the
 * {@link com.collectionloghelper.player.DiaryRegion} enum constant name
 * ({@code WESTERN}, not the colloquial {@code WESTERN_PROVINCES}) and
 * {@code TIER} is the {@link com.collectionloghelper.player.DiaryTier} name.
 * The varbit lookup in {@code RequirementsChecker.resolveDiaryVarbit}
 * resolves these against {@code Varbits.DIARY_<AREA>_<TIER>}, e.g.
 * {@code Varbits.DIARY_WESTERN_ELITE} and {@code Varbits.DIARY_DESERT_HARD}.
 */
public class C6B3NonOverlapRegressionTest
{
	private static final String DESERT_HARD = "DESERT_HARD";
	private static final String WESTERN_ELITE = "WESTERN_ELITE";

	private static final List<String> TOA_SOURCES = Arrays.asList(
		"Tombs of Amascut",
		"Tombs of Amascut (300 Invocation)",
		"Tombs of Amascut (500 Invocation)"
	);

	private static final List<String> GAUNTLET_SOURCES = Arrays.asList(
		"The Gauntlet",
		"Corrupted Gauntlet"
	);

	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();

		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);

		database.load();
	}

	@Test
	public void toaSourcesCarryDesertHardDiaryAlternative()
	{
		for (String name : TOA_SOURCES)
		{
			assertSourceHasDiaryAlternative(
				name,
				DESERT_HARD,
				"Pharaoh's sceptre",
				"Desert Hard diary");
		}
	}

	@Test
	public void gauntletSourcesCarryWesternEliteDiaryAlternative()
	{
		for (String name : GAUNTLET_SOURCES)
		{
			assertSourceHasDiaryAlternative(
				name,
				WESTERN_ELITE,
				"Crystal teleport",
				"Western Provinces Elite diary");
		}
	}

	private void assertSourceHasDiaryAlternative(
		String sourceName,
		String expectedDiary,
		String descriptionKeyword,
		String travelTipKeyword)
	{
		CollectionLogSource source = findSource(sourceName);
		assertNotNull(source, "Expected source: " + sourceName);
		assertNotNull(source.getGuidanceSteps(),
			sourceName + ": guidanceSteps must not be null");
		assertTrue(!source.getGuidanceSteps().isEmpty(),
			sourceName + ": guidanceSteps must not be empty");

		GuidanceStep firstStep = source.getGuidanceSteps().get(0);
		List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
		assertNotNull(alternatives,
			sourceName + ": first step must declare conditionalAlternatives");

		ConditionalAlternative diaryAlt = findDiaryAlternative(alternatives, expectedDiary);
		assertNotNull(diaryAlt,
			sourceName + ": expected a conditional alternative with diaries = ["
				+ expectedDiary + "], but none matched");

		SourceRequirements altReqs = diaryAlt.getRequirements();
		assertNotNull(altReqs,
			sourceName + ": diary conditional alternative must declare requirements");
		assertEquals(
			List.of(expectedDiary),
			altReqs.getDiaries(),
			sourceName + ": expected diaries = [" + expectedDiary + "]");

		assertNotNull(diaryAlt.getDescription(),
			sourceName + ": diary conditional alternative must override description");
		assertTrue(
			diaryAlt.getDescription().contains(descriptionKeyword),
			sourceName + ": alternative description should mention '" + descriptionKeyword
				+ "'; got: " + diaryAlt.getDescription());

		assertNotNull(diaryAlt.getTravelTip(),
			sourceName + ": diary conditional alternative must override travelTip");
		assertTrue(
			diaryAlt.getTravelTip().contains(travelTipKeyword),
			sourceName + ": alternative travelTip should mention '" + travelTipKeyword
				+ "'; got: " + diaryAlt.getTravelTip());
	}

	private static ConditionalAlternative findDiaryAlternative(
		List<ConditionalAlternative> alternatives, String expectedDiary)
	{
		for (ConditionalAlternative alt : alternatives)
		{
			SourceRequirements reqs = alt.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<String> diaries = reqs.getDiaries();
			if (diaries != null && diaries.contains(expectedDiary))
			{
				return alt;
			}
		}
		return null;
	}

	private CollectionLogSource findSource(String name)
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (name.equals(source.getName()))
			{
				return source;
			}
		}
		return null;
	}
}
