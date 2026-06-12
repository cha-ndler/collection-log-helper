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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C6 B3 (B2-overlap follow-up) regression guard - locks the C3 diary-tier
 * conditional alternatives that had to wait for B2's POH-teleport authoring
 * (PR #630) to land before they could be stacked ahead of the existing
 * POH alternatives.
 *
 * <p>Three sources, each with one diary alternative inserted at
 * {@code conditionalAlternatives[0]} so the sequencer evaluates it before
 * B2's POH alternative (which shifts to index {@code [1]}):
 *
 * <ul>
 *   <li>Chambers of Xeric: {@code KOUREND_HARD} -> in-Lovakengj Xeric's
 *       Heart teleport (2 tiles from prep room).</li>
 *   <li>General Graardor: {@code FREMENNIK_HARD} -> diary-route overland
 *       to GWD entrance.</li>
 *   <li>Vorkath: {@code FREMENNIK_HARD} -> Fremennik cape teleport to
 *       Rellekka, run north to docks for Torfinn.</li>
 * </ul>
 *
 * <p>Guards three properties per source:
 * <ol>
 *   <li>Diary alternative is at {@code conditionalAlternatives[0]} with the
 *       expected single-entry {@code diaries} list.</li>
 *   <li>B2's POH alternative still exists at {@code conditionalAlternatives[1]}
 *       with its original {@code pohTeleports} value preserved.</li>
 *   <li>The base step's {@code description} is untouched (the stacking edit
 *       only touched {@code conditionalAlternatives}).</li>
 * </ol>
 *
 * <p>Diary string format follows the existing {@code SourceRequirements.diaries}
 * convention {@code <AREA>_<TIER>} where {@code AREA} matches a
 * {@link com.collectionloghelper.player.DiaryRegion} enum constant
 * ({@code KOUREND}, {@code FREMENNIK}) and {@code TIER} matches a
 * {@link com.collectionloghelper.player.DiaryTier} enum constant
 * ({@code HARD}).
 */
public class C6B3OverlapRegressionTest
{
	private static final String KOUREND_HARD = "KOUREND_HARD";
	private static final String FREMENNIK_HARD = "FREMENNIK_HARD";

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
	public void chambersOfXericNoLongerCarriesKourendHardDiaryAlternative()
	{
		// Wiki-meta audit fix: the KOUREND_HARD alternative claimed Xeric's
		// Heart lands near the CoX prep room and is a Kourend Hard unlock;
		// Xeric's Heart teleports to Kourend Castle and no diary gates any
		// Xeric's talisman destination. The alternative was removed and must
		// not be reintroduced; B2's POH alternative is preserved.
		CollectionLogSource source = findSource("Chambers of Xeric");
		assertNotNull(source, "Expected source: Chambers of Xeric");
		GuidanceStep firstStep = source.getGuidanceSteps().get(0);
		assertTrue(
			firstStep.getDescription().contains("Use Xeric's talisman to teleport to Mount Quidamortem"),
			"Chambers of Xeric: base step description should keep the talisman route; got: "
				+ firstStep.getDescription());
		List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
		assertNotNull(alternatives, "Chambers of Xeric: first step must declare conditionalAlternatives");
		for (ConditionalAlternative alt : alternatives)
		{
			if (alt.getRequirements() != null && alt.getRequirements().getDiaries() != null)
			{
				assertTrue(!alt.getRequirements().getDiaries().contains(KOUREND_HARD),
					"Chambers of Xeric: fabricated KOUREND_HARD alternative must not be reintroduced");
			}
		}
		ConditionalAlternative pohAlt = findPohAlternative(alternatives, "XERICS_TALISMAN");
		assertNotNull(pohAlt, "Chambers of Xeric: B2's XERICS_TALISMAN POH alternative must be preserved");
	}

	@Test
	public void generalGraardorStacksFremennikHardAheadOfJewelleryBox()
	{
		assertStackedDiaryAndPoh(
			"General Graardor",
			FREMENNIK_HARD,
			"JEWELLERY_BOX_FANCY",
			"Goblin Village",
			"Teleport to Trollheim and run north to the God Wars Dungeon entrance");
	}

	@Test
	public void vorkathStacksFremennikHardAheadOfMountedGlory()
	{
		assertStackedDiaryAndPoh(
			"Vorkath",
			FREMENNIK_HARD,
			"MOUNTED_GLORY",
			"Fremennik cape",
			"Travel to Rellekka docks, sail to Ungael Island via Torfinn");
	}

	@Test
	public void vorkathMountedGloryAlternativeNoLongerClaimsGamesNecklaceRoute()
	{
		// Collateral fix from #630 review (MEDIUM): the original copy claimed
		// a games necklace from Edgeville reaches Rellekka. It does not.
		CollectionLogSource source = findSource("Vorkath");
		assertNotNull(source, "Expected source: Vorkath");
		GuidanceStep firstStep = source.getGuidanceSteps().get(0);
		List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
		ConditionalAlternative pohAlt = findPohAlternative(alternatives, "MOUNTED_GLORY");
		assertNotNull(pohAlt, "Vorkath: expected MOUNTED_GLORY POH alternative");

		String desc = pohAlt.getDescription();
		assertNotNull(desc, "Vorkath: MOUNTED_GLORY alternative must declare description");
		assertTrue(
			!desc.contains("games necklace"),
			"Vorkath: MOUNTED_GLORY description must not claim a games necklace route; got: " + desc);
		assertTrue(
			desc.contains("onward to Rellekka docks"),
			"Vorkath: MOUNTED_GLORY description should describe an onward Rellekka route; got: " + desc);
	}

	private void assertStackedDiaryAndPoh(
		String sourceName,
		String expectedDiary,
		String expectedPohTeleport,
		String diaryDescriptionKeyword,
		String baseStepDescriptionSubstring)
	{
		CollectionLogSource source = findSource(sourceName);
		assertNotNull(source, "Expected source: " + sourceName);
		assertNotNull(source.getGuidanceSteps(),
			sourceName + ": guidanceSteps must not be null");
		assertTrue(!source.getGuidanceSteps().isEmpty(),
			sourceName + ": guidanceSteps must not be empty");

		GuidanceStep firstStep = source.getGuidanceSteps().get(0);

		// (3) Base step description preserved.
		assertNotNull(firstStep.getDescription(),
			sourceName + ": first step description must not be null");
		assertTrue(
			firstStep.getDescription().contains(baseStepDescriptionSubstring),
			sourceName + ": base step description should still contain '"
				+ baseStepDescriptionSubstring + "'; got: " + firstStep.getDescription());

		List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
		assertNotNull(alternatives,
			sourceName + ": first step must declare conditionalAlternatives");
		assertTrue(alternatives.size() >= 2,
			sourceName + ": expected at least 2 conditional alternatives (diary + POH); got: "
				+ alternatives.size());

		// (1) Diary alternative at index [0].
		ConditionalAlternative diaryAlt = alternatives.get(0);
		SourceRequirements diaryReqs = diaryAlt.getRequirements();
		assertNotNull(diaryReqs,
			sourceName + ": conditionalAlternatives[0] must declare requirements");
		assertEquals(
			List.of(expectedDiary),
			diaryReqs.getDiaries(),
			sourceName + ": conditionalAlternatives[0] must declare diaries = ["
				+ expectedDiary + "]");
		assertNotNull(diaryAlt.getDescription(),
			sourceName + ": diary alternative must override description");
		assertTrue(
			diaryAlt.getDescription().contains(diaryDescriptionKeyword),
			sourceName + ": diary alternative description should mention '"
				+ diaryDescriptionKeyword + "'; got: " + diaryAlt.getDescription());
		assertNotNull(diaryAlt.getTravelTip(),
			sourceName + ": diary alternative must override travelTip");

		// (2) B2 POH alternative at index [1], preserved.
		ConditionalAlternative pohAlt = alternatives.get(1);
		SourceRequirements pohReqs = pohAlt.getRequirements();
		assertNotNull(pohReqs,
			sourceName + ": conditionalAlternatives[1] must declare requirements");
		assertEquals(
			List.of(expectedPohTeleport),
			pohReqs.getPohTeleports(),
			sourceName + ": conditionalAlternatives[1] must declare pohTeleports = ["
				+ expectedPohTeleport + "]");
		assertNotNull(pohAlt.getDescription(),
			sourceName + ": POH alternative must retain description");
		assertNotNull(pohAlt.getTravelTip(),
			sourceName + ": POH alternative must retain travelTip");
	}

	private static ConditionalAlternative findPohAlternative(
		List<ConditionalAlternative> alternatives, String expectedPohTeleport)
	{
		for (ConditionalAlternative alt : alternatives)
		{
			SourceRequirements reqs = alt.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<String> pohTeleports = reqs.getPohTeleports();
			if (pohTeleports != null && pohTeleports.contains(expectedPohTeleport))
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
