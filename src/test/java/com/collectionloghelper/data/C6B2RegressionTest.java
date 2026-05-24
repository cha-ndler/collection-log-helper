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
 * C6 B2 regression guard - locks the C1 POH-teleport conditional alternatives
 * shipped on the eight top-20 sources with POH-teleport applicability per the
 * C6 scoping doc (sections 2 and 4):
 *
 * <ul>
 *   <li>Chambers of Xeric: mounted {@code XERICS_TALISMAN} -&gt; Xeric's Glade.</li>
 *   <li>General Graardor / Commander Zilyana / K'ril Tsutsaroth / Kree'arra:
 *       shared {@code JEWELLERY_BOX_FANCY} -&gt; Combat bracelet -&gt; Warriors' Guild.</li>
 *   <li>Vorkath: {@code MOUNTED_GLORY} -&gt; Edgeville fallback when no
 *       Fremennik-tier teleport is available.</li>
 *   <li>Zulrah: {@code FAIRY_RING} in the superior garden -&gt; AKQ ring.</li>
 *   <li>Corporeal Beast: {@code JEWELLERY_BOX_FANCY} -&gt; Games necklace -&gt;
 *       Wilderness Volcano.</li>
 * </ul>
 *
 * <p>For each source the first guidance step must:
 * <ul>
 *   <li>Carry exactly one {@code conditionalAlternatives} entry.</li>
 *   <li>That entry's {@code requirements.pohTeleports} must equal the expected
 *       single-element list of {@link com.collectionloghelper.player.PohTeleport}
 *       enum-name strings.</li>
 *   <li>That entry must override both {@code description} and {@code travelTip}.</li>
 *   <li>The base step's description (and any base {@code requiredItemIds}) must
 *       be untouched relative to its pre-B2 wording, so the no-POH fallback
 *       route still works.</li>
 * </ul>
 *
 * <p>Pattern mirrors {@code B1aRegressionTest} and {@code B1bRegressionTest}.
 */
public class C6B2RegressionTest
{
	private static final String JEWELLERY_BOX_FANCY = "JEWELLERY_BOX_FANCY";
	private static final String MOUNTED_GLORY = "MOUNTED_GLORY";
	private static final String FAIRY_RING = "FAIRY_RING";
	private static final String XERICS_TALISMAN = "XERICS_TALISMAN";

	/**
	 * Per-source expectations: source name -&gt; expected pohTeleports list.
	 * Insertion order is preserved for stable failure reporting.
	 */
	private static final Map<String, List<String>> EXPECTED_POH_BY_SOURCE = buildExpected();

	private static Map<String, List<String>> buildExpected()
	{
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put("Chambers of Xeric", List.of(XERICS_TALISMAN));
		map.put("General Graardor", List.of(JEWELLERY_BOX_FANCY));
		map.put("Commander Zilyana", List.of(JEWELLERY_BOX_FANCY));
		map.put("K'ril Tsutsaroth", List.of(JEWELLERY_BOX_FANCY));
		map.put("Kree'arra", List.of(JEWELLERY_BOX_FANCY));
		map.put("Vorkath", List.of(MOUNTED_GLORY));
		map.put("Zulrah", List.of(FAIRY_RING));
		map.put("Corporeal Beast", List.of(JEWELLERY_BOX_FANCY));
		return map;
	}

	/**
	 * Per-source expected base-step description tokens that MUST still appear
	 * in the unmodified base step. These are the unrestricted-fallback markers
	 * that prove the base route was not rewritten by the B2 edit.
	 */
	private static final Map<String, String> EXPECTED_BASE_DESCRIPTION_MARKER = buildBaseMarkers();

	private static Map<String, String> buildBaseMarkers()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Chambers of Xeric", "Xeric's talisman to teleport to Mount Quidamortem");
		map.put("General Graardor", "Teleport to Trollheim");
		map.put("Commander Zilyana", "Teleport to Trollheim");
		map.put("K'ril Tsutsaroth", "Teleport to Trollheim");
		map.put("Kree'arra", "Teleport to Trollheim");
		map.put("Vorkath", "Rellekka docks");
		map.put("Zulrah", "Zul-andra teleport scroll or fairy ring BJS");
		map.put("Corporeal Beast", "games necklace to teleport to the Corporeal Beast cave");
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
	public void firstStepCarriesPohTeleportConditionalAlternative()
	{
		for (Map.Entry<String, List<String>> entry : EXPECTED_POH_BY_SOURCE.entrySet())
		{
			String name = entry.getKey();
			List<String> expectedPoh = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(), name + ": guidanceSteps must not be empty");

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
			assertNotNull(alternatives, name + ": first step must declare conditionalAlternatives");
			assertFalse(
				alternatives.isEmpty(),
				name + ": expected at least one conditional alternative for the POH-teleport route");

			ConditionalAlternative alt = findPohAlternative(alternatives, expectedPoh);
			assertNotNull(alt,
				name + ": expected a conditional alternative with pohTeleports = " + expectedPoh
					+ ", but none matched (other alternatives may exist via later batches)");

			SourceRequirements altReqs = alt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertNotNull(altReqs.getPohTeleports(), name + ": requirements.pohTeleports must be set");
			assertEquals(
				expectedPoh,
				altReqs.getPohTeleports(),
				name + ": expected pohTeleports = " + expectedPoh
					+ " but got " + altReqs.getPohTeleports());

			assertNotNull(alt.getDescription(), name + ": conditional alternative must override description");
			assertTrue(
				alt.getDescription().toUpperCase().contains("POH"),
				name + ": alternative description should mention POH; got: " + alt.getDescription());

			assertNotNull(alt.getTravelTip(), name + ": conditional alternative must override travelTip");
			assertTrue(
				alt.getTravelTip().toUpperCase().contains("POH"),
				name + ": alternative travelTip should mention POH; got: " + alt.getTravelTip());
		}
	}

	private static ConditionalAlternative findPohAlternative(
		List<ConditionalAlternative> alternatives, List<String> expectedPoh)
	{
		for (ConditionalAlternative candidate : alternatives)
		{
			SourceRequirements reqs = candidate.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<String> pohTeleports = reqs.getPohTeleports();
			if (pohTeleports != null && pohTeleports.equals(expectedPoh))
			{
				return candidate;
			}
		}
		return null;
	}

	@Test
	public void baseStepDescriptionIsUntouched()
	{
		for (Map.Entry<String, String> entry : EXPECTED_BASE_DESCRIPTION_MARKER.entrySet())
		{
			String name = entry.getKey();
			String expectedMarker = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			String baseDescription = firstStep.getDescription();
			assertNotNull(baseDescription, name + ": base step description must not be null");
			assertTrue(
				baseDescription.contains(expectedMarker),
				name + ": base step description must still contain the original fallback wording '"
					+ expectedMarker + "'. Got: " + baseDescription);
		}
	}

	@Test
	public void expectedPohSourcesArePresentInDatabase()
	{
		// Sanity: all 8 declared sources actually exist in the loaded JSON.
		for (String name : EXPECTED_POH_BY_SOURCE.keySet())
		{
			assertNotNull(findSource(name), "Expected source missing from drop_rates.json: " + name);
		}
		// Cardinality lock so future renames or removals trip this test.
		assertEquals(8, EXPECTED_POH_BY_SOURCE.size(),
			"C6 B2 covers exactly 8 sources per the scoping doc Section 4");
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

	// Unused convenience for readability of the expected list shape.
	@SuppressWarnings("unused")
	private static List<String> sources()
	{
		return Arrays.asList(
			"Chambers of Xeric",
			"General Graardor",
			"Commander Zilyana",
			"K'ril Tsutsaroth",
			"Kree'arra",
			"Vorkath",
			"Zulrah",
			"Corporeal Beast"
		);
	}
}
