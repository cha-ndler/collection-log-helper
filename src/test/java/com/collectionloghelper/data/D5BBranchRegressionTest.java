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
 * D5 batch B regression guard - locks the POH-teleport conditional alternatives
 * wired on skilling and wilderness sources:
 *
 * <ul>
 *   <li>(Zalcano was removed by the wiki-meta audit: fairy ring code BIS
 *       does not exist and Prifddinas has no fairy ring destination.)</li>
 *   <li>Volcanic Mine: {@code DIGSITE_PENDANT} -&gt; Fossil Island.</li>
 *   <li>Catacombs of Kourend: {@code XERICS_TALISMAN} -&gt; Xeric's Heart.</li>
 *   <li>Motherlode Mine: {@code JEWELLERY_BOX_FANCY} -&gt; Skills necklace -&gt; Mining Guild.</li>
 * </ul>
 *
 * <p>This batch shipped 4 of its 8 scoped sources; Hallowed Sepulchre, Rooftop
 * Agility, Colossal Wyrm Agility, and Revenants are deferred to a follow-up.
 *
 * <p>For each wired source, some guidance step must carry a
 * {@code conditionalAlternatives} entry whose {@code requirements.pohTeleports}
 * equals the expected single-element list, the alternative must override
 * description + travelTip (mentioning POH), and that step's base description
 * must remain (the unrestricted route is not replaced). Index-agnostic: the
 * alternative may sit on any step.
 */
public class D5BBranchRegressionTest
{
	private static final Map<String, List<String>> EXPECTED = buildExpected();

	private static Map<String, List<String>> buildExpected()
	{
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put("Volcanic Mine",        List.of("DIGSITE_PENDANT"));
		map.put("Catacombs of Kourend", List.of("XERICS_TALISMAN"));
		map.put("Motherlode Mine",      List.of("JEWELLERY_BOX_FANCY"));
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
	public void wiredSourceCarriesPohTeleportConditionalAlternative()
	{
		for (Map.Entry<String, List<String>> entry : EXPECTED.entrySet())
		{
			String name = entry.getKey();
			List<String> expectedPoh = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");

			GuidanceStep wiredStep = null;
			ConditionalAlternative wiredAlt = null;
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				List<ConditionalAlternative> alts = step.getConditionalAlternatives();
				if (alts == null)
				{
					continue;
				}
				ConditionalAlternative match = findPohAlternative(alts, expectedPoh);
				if (match != null)
				{
					wiredStep = step;
					wiredAlt = match;
					break;
				}
			}

			assertNotNull(wiredAlt,
				name + ": expected a conditional alternative with pohTeleports = " + expectedPoh);

			SourceRequirements altReqs = wiredAlt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertEquals(expectedPoh, altReqs.getPohTeleports(), name + ": pohTeleports mismatch");

			assertNotNull(wiredAlt.getDescription(), name + ": alternative must override description");
			assertTrue(wiredAlt.getDescription().toUpperCase().contains("POH"),
				name + ": alternative description should mention POH; got: " + wiredAlt.getDescription());
			assertNotNull(wiredAlt.getTravelTip(), name + ": alternative must override travelTip");

			// Fallback preserved: the step carrying the alternative still has a base description.
			assertNotNull(wiredStep.getDescription(), name + ": base step description must remain");
			assertFalse(wiredStep.getDescription().isBlank(), name + ": base step description must not be blank");
		}
	}

	@Test
	public void expectedSourcesArePresentInDatabase()
	{
		for (String name : EXPECTED.keySet())
		{
			assertNotNull(findSource(name), "Expected source missing from drop_rates.json: " + name);
		}
		assertEquals(3, EXPECTED.size(), "D5 batch B (partial) covers exactly 3 sources");
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
