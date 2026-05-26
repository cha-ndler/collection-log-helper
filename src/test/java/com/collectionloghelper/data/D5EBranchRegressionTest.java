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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D5 batch E regression guard - locks the inventory-teleport-jewellery
 * conditional alternatives wired with {@code inventoryItemIdsAny} over the
 * charged-tier item IDs only.
 *
 * <p>Per the D5-E decision (mirroring the D5-F vector): charged teleport
 * jewellery is modelled with {@code inventoryItemIdsAny} over the
 * charged-variant IDs, NOT {@code equippedItemIds}. An equipped chargeable
 * item reads true at 0 charges (a false promise); a depleted item carries a
 * different ID, and a fully-depleted Ring of dueling crumbles to nothing, so
 * listing only the charged-variant IDs is the correct true-inventory vector.
 * One alternative per source; the unrestricted base step stays as fallback.
 *
 * <p>Wired sources and vectors:
 * <ul>
 *   <li>Castle Wars   - Ring of dueling (charged) -&gt; Castle Wars lobby</li>
 *   <li>Chaos Fanatic - Ring of dueling (charged) -&gt; Ferox Enclave bank</li>
 *   <li>Obor          - Amulet of glory (charged) -&gt; Edgeville bank
 *       (stacked behind the pre-existing MOUNTED_GLORY POH alternative)</li>
 *   <li>Revenants     - Amulet of glory (charged) -&gt; Edgeville bank</li>
 * </ul>
 *
 * <p>Pattern mirrors {@link CExtensionInventoryTeleportRegressionTest} and
 * {@link D5CBranchRegressionTest}.
 */
public class D5EBranchRegressionTest
{
	/** Ring of dueling charged tiers 8..1 (even IDs). No 0-charge variant - it crumbles to dust. */
	private static final List<Integer> RING_OF_DUELING_CHARGED = List.of(
		2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566);

	/** Amulet of glory charged tiers 1..6 plus eternal glory (unlimited). Excludes uncharged 1704. */
	private static final List<Integer> AMULET_OF_GLORY_CHARGED = List.of(
		1706, 1708, 1710, 1712, 11976, 11978, 19707);

	/**
	 * Per-source expectation: source name -&gt; expected charged-tier ID list
	 * that must appear on exactly one first-step conditional alternative via
	 * {@code inventoryItemIdsAny}. Insertion order is preserved for stable
	 * failure reporting.
	 */
	private static final Map<String, List<Integer>> EXPECTED_IDS_BY_SOURCE = buildExpected();

	private static Map<String, List<Integer>> buildExpected()
	{
		Map<String, List<Integer>> map = new LinkedHashMap<>();
		map.put("Castle Wars", RING_OF_DUELING_CHARGED);
		map.put("Chaos Fanatic", RING_OF_DUELING_CHARGED);
		map.put("Obor", AMULET_OF_GLORY_CHARGED);
		map.put("Revenants", AMULET_OF_GLORY_CHARGED);
		return map;
	}

	/**
	 * Per-source base-step description tokens that must remain in the
	 * unrestricted fallback step after D5E wiring.
	 */
	private static final Map<String, String> EXPECTED_BASE_MARKER = buildBaseMarkers();

	private static Map<String, String> buildBaseMarkers()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Castle Wars", "Travel to Castle Wars via the Minigame teleport");
		map.put("Chaos Fanatic", "Bank in Ferox Enclave");
		map.put("Obor", "Bank in Edgeville");
		map.put("Revenants", "Bank in Edgeville or Ferox Enclave");
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
	public void firstStepCarriesChargedInventoryAlternative()
	{
		for (Map.Entry<String, List<Integer>> entry : EXPECTED_IDS_BY_SOURCE.entrySet())
		{
			String name = entry.getKey();
			List<Integer> expectedIds = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(), name + ": guidanceSteps must not be empty");

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
			assertNotNull(alternatives, name + ": first step must declare conditionalAlternatives");
			assertFalse(alternatives.isEmpty(),
				name + ": expected at least one conditional alternative for the jewellery route");

			ConditionalAlternative alt = findInventoryAnyAlternative(alternatives, expectedIds);
			assertNotNull(alt,
				name + ": expected a conditional alternative with inventoryItemIdsAny == " + expectedIds
					+ ", but none matched");

			SourceRequirements reqs = alt.getRequirements();
			assertNotNull(reqs, name + ": jewellery alternative must declare requirements");
			assertNull(reqs.getInventoryItemIds(),
				name + ": inventoryItemIds must be null on this OR-semantic jewellery alternative");
			assertNull(reqs.getEquippedItemIds(),
				name + ": equippedItemIds must be null - charged jewellery is an inventory vector here");
			assertEquals(expectedIds, reqs.getInventoryItemIdsAny(),
				name + ": inventoryItemIdsAny must equal the charged-tier ID list exactly");

			assertNotNull(alt.getDescription(),
				name + ": jewellery alternative must override description");
			assertNotNull(alt.getTravelTip(),
				name + ": jewellery alternative must override travelTip");
			assertTrue(
				alt.getDescription().toLowerCase().contains("ring of dueling")
					|| alt.getDescription().toLowerCase().contains("amulet of glory"),
				name + ": description should name the jewellery; got: " + alt.getDescription());
			assertTrue(alt.getTravelTip().toLowerCase().contains("inventory"),
				name + ": travelTip should mention the inventory rub; got: " + alt.getTravelTip());
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
	public void oborKeepsPreExistingPohAlternativeAhead()
	{
		// Obor's Bank step already had a MOUNTED_GLORY POH alternative; the D5E
		// inventory-glory alternative stacks behind it, so the POH alt must survive at index 0.
		ConditionalAlternative poh =
			findSource("Obor").getGuidanceSteps().get(0).getConditionalAlternatives().get(0);
		assertNotNull(poh.getRequirements());
		assertEquals(List.of("MOUNTED_GLORY"), poh.getRequirements().getPohTeleports(),
			"Obor: pre-existing MOUNTED_GLORY POH alternative must remain at index 0");
	}

	@Test
	public void wiredSourcesArePresentAndCountMatches()
	{
		for (String name : EXPECTED_IDS_BY_SOURCE.keySet())
		{
			assertNotNull(findSource(name),
				"Expected source missing from drop_rates.json: " + name);
		}
		assertEquals(4, EXPECTED_IDS_BY_SOURCE.size(),
			"D5E wires exactly 4 inventory-teleport-jewellery sources");
	}

	private static ConditionalAlternative findInventoryAnyAlternative(
		List<ConditionalAlternative> alternatives, List<Integer> expectedIds)
	{
		for (ConditionalAlternative candidate : alternatives)
		{
			SourceRequirements reqs = candidate.getRequirements();
			if (reqs == null)
			{
				continue;
			}
			List<Integer> any = reqs.getInventoryItemIdsAny();
			if (any != null && any.equals(expectedIds))
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
