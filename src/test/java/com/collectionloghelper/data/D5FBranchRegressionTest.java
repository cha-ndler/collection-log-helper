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
 * D5 batch F regression guard - locks the inventory-teleport conditional
 * alternatives wired on slayer + Kourend sources via {@code inventoryItemIdsAny}
 * (OR-semantics over charge-tier / model variants):
 *
 * <ul>
 *   <li>Sarachnis: inventory Xeric's talisman -&gt; Xeric's Heart (talisman models
 *       13393 / 18465).</li>
 *   <li>Nechryael: inventory Slayer ring -&gt; Slayer Tower entrance (rings 1-8 +
 *       eternal).</li>
 *   <li>Gargoyle: inventory Slayer ring -&gt; Slayer Tower entrance.</li>
 *   <li>Kurask: inventory Slayer ring -&gt; Fremennik Slayer Dungeon entrance.</li>
 *   <li>Bloodveld: inventory Slayer ring -&gt; Slayer Tower entrance.</li>
 * </ul>
 *
 * <p>For each wired source, some guidance step must carry a
 * {@code conditionalAlternatives} entry whose
 * {@code requirements.inventoryItemIdsAny} equals the expected ID list, the
 * alternative must override description + travelTip (mentioning the inventory
 * teleport vector), and that step's base description must remain (the
 * unrestricted route is the fallback). Index-agnostic: the alternative may sit
 * on any step.
 */
public class D5FBranchRegressionTest
{
	private static final List<Integer> SLAYER_RING_ANY =
		List.of(11866, 11867, 11868, 11869, 11870, 11871, 11872, 11873, 21268);
	private static final List<Integer> XERIC_TALISMAN_ANY = List.of(13393, 18465);

	private static final Map<String, List<Integer>> EXPECTED = buildExpected();

	private static Map<String, List<Integer>> buildExpected()
	{
		Map<String, List<Integer>> map = new LinkedHashMap<>();
		map.put("Sarachnis", XERIC_TALISMAN_ANY);
		map.put("Nechryael", SLAYER_RING_ANY);
		map.put("Gargoyle",  SLAYER_RING_ANY);
		map.put("Kurask",    SLAYER_RING_ANY);
		map.put("Bloodveld", SLAYER_RING_ANY);
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
	public void wiredSourceCarriesInventoryTeleportConditionalAlternative()
	{
		for (Map.Entry<String, List<Integer>> entry : EXPECTED.entrySet())
		{
			String name = entry.getKey();
			List<Integer> expectedAny = entry.getValue();

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
				ConditionalAlternative match = findInventoryAlternative(alts, expectedAny);
				if (match != null)
				{
					wiredStep = step;
					wiredAlt = match;
					break;
				}
			}

			assertNotNull(wiredAlt,
				name + ": expected a conditional alternative with inventoryItemIdsAny = " + expectedAny);

			SourceRequirements altReqs = wiredAlt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertEquals(expectedAny, altReqs.getInventoryItemIdsAny(),
				name + ": inventoryItemIdsAny mismatch");

			assertNotNull(wiredAlt.getDescription(), name + ": alternative must override description");
			assertTrue(wiredAlt.getDescription().toLowerCase().contains("inventory")
					|| wiredAlt.getDescription().toLowerCase().contains("rub"),
				name + ": alternative description should mention the inventory teleport; got: "
					+ wiredAlt.getDescription());
			assertNotNull(wiredAlt.getTravelTip(), name + ": alternative must override travelTip");
			assertTrue(wiredAlt.getTravelTip().toLowerCase().contains("inventory"),
				name + ": alternative travelTip should mention inventory; got: " + wiredAlt.getTravelTip());

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
		assertEquals(5, EXPECTED.size(), "D5 batch F covers exactly 5 sources");
	}

	private static ConditionalAlternative findInventoryAlternative(
		List<ConditionalAlternative> alternatives, List<Integer> expectedAny)
	{
		for (ConditionalAlternative candidate : alternatives)
		{
			SourceRequirements reqs = candidate.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<Integer> inventoryAny = reqs.getInventoryItemIdsAny();
			if (inventoryAny != null && inventoryAny.equals(expectedAny))
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
