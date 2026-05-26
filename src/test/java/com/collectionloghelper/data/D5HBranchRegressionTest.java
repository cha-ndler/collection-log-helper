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
 * D5 batch H regression guard - locks the skill-cape-perk conditional
 * alternatives wired on skilling sources via the new {@code skillCapePerks}
 * field (a level-99 proxy for the cape's travel perk):
 *
 * <ul>
 *   <li>Hespori: {@code FARMING} -&gt; Farming cape teleport into the Farming Guild.</li>
 *   <li>Farming (Fruit Trees): {@code FARMING} -&gt; Farming cape to the guild fruit tree patch.</li>
 *   <li>Black Chinchompas: {@code HUNTER} -&gt; Hunter cape to the black chinchompa area.</li>
 * </ul>
 *
 * <p>For each wired source, some guidance step must carry a
 * {@code conditionalAlternatives} entry whose {@code requirements.getSkillCapePerks()}
 * equals the expected single-element list, the alternative must override
 * description + travelTip (mentioning the skill cape), and that step's base
 * description must remain (the unrestricted route is not replaced).
 * Index-agnostic: the alternative may sit on any step.
 */
public class D5HBranchRegressionTest
{
	private static final Map<String, List<String>> EXPECTED = buildExpected();

	private static Map<String, List<String>> buildExpected()
	{
		Map<String, List<String>> map = new LinkedHashMap<>();
		map.put("Hespori",                List.of("FARMING"));
		map.put("Farming (Fruit Trees)",  List.of("FARMING"));
		map.put("Black Chinchompas",      List.of("HUNTER"));
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
	public void wiredSourceCarriesSkillCapePerkConditionalAlternative()
	{
		for (Map.Entry<String, List<String>> entry : EXPECTED.entrySet())
		{
			String name = entry.getKey();
			List<String> expectedPerks = entry.getValue();

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
				ConditionalAlternative match = findSkillCapeAlternative(alts, expectedPerks);
				if (match != null)
				{
					wiredStep = step;
					wiredAlt = match;
					break;
				}
			}

			assertNotNull(wiredAlt,
				name + ": expected a conditional alternative with skillCapePerks = " + expectedPerks);

			SourceRequirements altReqs = wiredAlt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertEquals(expectedPerks, altReqs.getSkillCapePerks(), name + ": skillCapePerks mismatch");

			assertNotNull(wiredAlt.getDescription(), name + ": alternative must override description");
			assertTrue(wiredAlt.getDescription().toLowerCase().contains("cape"),
				name + ": alternative description should mention the skill cape; got: "
					+ wiredAlt.getDescription());
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
		assertEquals(3, EXPECTED.size(), "D5 batch H wires exactly 3 skill-cape-perk sources");
	}

	private static ConditionalAlternative findSkillCapeAlternative(
		List<ConditionalAlternative> alternatives, List<String> expectedPerks)
	{
		for (ConditionalAlternative candidate : alternatives)
		{
			SourceRequirements reqs = candidate.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<String> perks = reqs.getSkillCapePerks();
			if (perks != null && perks.equals(expectedPerks))
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
