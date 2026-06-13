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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D5 batch C regression guard — locks diary-shortcut conditional alternatives
 * on mid-tier and boss combat sources.
 *
 * <p>For each wired source the first guidance step must:
 * <ul>
 *   <li>Carry at least one {@code conditionalAlternatives} entry.</li>
 *   <li>That entry's {@code requirements.diaries} must contain exactly the
 *       expected diary constant string.</li>
 *   <li>The base step description (no requirements) must remain as the
 *       fallback — unchanged from the pre-D5C wording.</li>
 * </ul>
 *
 * <p>Wired sources and diary constants:
 * <ul>
 *   <li>Kalphite Queen      — {@code DESERT_ELITE}</li>
 *   <li>Dagannoth Rex       — {@code FREMENNIK_HARD}</li>
 *   <li>Dagannoth Prime     — {@code FREMENNIK_HARD}</li>
 *   <li>Dagannoth Supreme   — {@code FREMENNIK_HARD}</li>
 *   <li>Alchemical Hydra    — {@code KOUREND_ELITE}</li>
 * </ul>
 *
 * <p>Skipped sources (no meaningful diary shortcut):
 * King Black Dragon (Wilderness lever route, no diary improvement),
 * Cerberus (Key master teleport already optimal, no diary shortcut),
 * Grotesque Guardians (Slayer Tower, no diary-gated travel improvement).
 *
 * <p>Pattern mirrors {@link C6B2RegressionTest}.
 */
public class D5CBranchRegressionTest
{
	// Wiki-meta audit fix: the desert amulet 4 teleport described by the KQ
	// alternative is an Elite Desert Diary reward, not Hard.
	private static final String DESERT_ELITE = "DESERT_ELITE";
	private static final String FREMENNIK_HARD = "FREMENNIK_HARD";
	private static final String KOUREND_ELITE = "KOUREND_ELITE";

	/**
	 * Per-source expectations: source name -> expected diary constant.
	 * Insertion order is preserved for stable failure reporting.
	 */
	private static final Map<String, String> EXPECTED_DIARY_BY_SOURCE = buildExpected();

	private static Map<String, String> buildExpected()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Kalphite Queen", DESERT_ELITE);
		map.put("Dagannoth Rex", FREMENNIK_HARD);
		map.put("Dagannoth Prime", FREMENNIK_HARD);
		map.put("Dagannoth Supreme", FREMENNIK_HARD);
		map.put("Alchemical Hydra", KOUREND_ELITE);
		return map;
	}

	/**
	 * Per-source base-step description tokens that must remain in the
	 * unrestricted fallback step after D5C wiring.
	 */
	private static final Map<String, String> EXPECTED_BASE_MARKER = buildBaseMarkers();

	private static Map<String, String> buildBaseMarkers()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Kalphite Queen", "Kalphite Lair entrance in the Kharidian Desert");
		map.put("Dagannoth Rex", "Rellekka docks");
		map.put("Dagannoth Prime", "Rellekka docks");
		map.put("Dagannoth Supreme", "Rellekka docks");
		map.put("Alchemical Hydra", "Karuulm Slayer Dungeon");
		return map;
	}

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
	public void firstStepCarriesDiaryConditionalAlternative()
	{
		for (Map.Entry<String, String> entry : EXPECTED_DIARY_BY_SOURCE.entrySet())
		{
			String name = entry.getKey();
			String expectedDiary = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(), name + ": guidanceSteps must not be empty");

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
			assertNotNull(alternatives, name + ": first step must declare conditionalAlternatives");
			assertFalse(alternatives.isEmpty(),
				name + ": expected at least one conditional alternative for the diary route");

			ConditionalAlternative alt = findDiaryAlternative(alternatives, expectedDiary);
			assertNotNull(alt,
				name + ": expected a conditional alternative with diaries containing '" + expectedDiary
					+ "', but none matched");

			SourceRequirements altReqs = alt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertNotNull(altReqs.getDiaries(), name + ": requirements.diaries must be set");
			assertTrue(altReqs.getDiaries().contains(expectedDiary),
				name + ": expected diaries to contain '" + expectedDiary
					+ "' but got " + altReqs.getDiaries());

			assertNotNull(alt.getDescription(),
				name + ": conditional alternative must override description");
			assertNotNull(alt.getTravelTip(),
				name + ": conditional alternative must override travelTip");
		}
	}

	@Test
	public void baseStepDescriptionIsUntouched()
	{
		for (Map.Entry<String, String> entry : EXPECTED_BASE_MARKER.entrySet())
		{
			String name = entry.getKey();
			String expectedMarker = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			String baseDescription = firstStep.getDescription();
			assertNotNull(baseDescription, name + ": base step description must not be null");
			assertTrue(baseDescription.contains(expectedMarker),
				name + ": base step description must still contain the original fallback wording '"
					+ expectedMarker + "'. Got: " + baseDescription);
		}
	}

	@Test
	public void wiredSourcesArePresentInDatabase()
	{
		for (String name : EXPECTED_DIARY_BY_SOURCE.keySet())
		{
			assertNotNull(findSource(name),
				"Expected source missing from drop_rates.json: " + name);
		}
		assertEquals(5, EXPECTED_DIARY_BY_SOURCE.size(),
			"D5C covers exactly 5 wired sources");
	}

	private static ConditionalAlternative findDiaryAlternative(
		List<ConditionalAlternative> alternatives, String expectedDiary)
	{
		for (ConditionalAlternative candidate : alternatives)
		{
			SourceRequirements reqs = candidate.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<String> diaries = reqs.getDiaries();
			if (diaries != null && diaries.contains(expectedDiary))
			{
				return candidate;
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
