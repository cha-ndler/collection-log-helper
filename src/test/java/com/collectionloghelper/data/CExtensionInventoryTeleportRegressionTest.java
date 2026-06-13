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
 * C-extension regression guard - locks the inventory-teleport conditional
 * alternatives wired on the five sources that were deferred from B1 closure
 * (PR #628) until the {@code inventoryItemIds} (PR #640) and
 * {@code inventoryItemIdsAny} (PR #645) schema fields landed.
 *
 * <p>Per-source insertions, ordered behind any pre-existing diary / POH
 * alternative (sequencer priority: diary &gt; POH &gt; equipped &gt;
 * inventory &gt; base):
 *
 * <ul>
 *   <li>Tombs of Amascut / 300 / 500 Invocation: existing diary alternative
 *       at [0], new inventory-any alternative at [1] listing all 7
 *       {@code PHARAOHS_SCEPTRE*} ItemID variants (26945-26951).</li>
 *   <li>Zulrah: existing POH (FAIRY_RING) alternative at [0], new
 *       inventory alternative at [1] listing
 *       {@code TELEPORTSCROLL_ZULANDRA} (12938).</li>
 *   <li>Corrupted Gauntlet / The Gauntlet: inventory-any alternative at
 *       [0] listing {@code GAUNTLET_TELEPORT_CRYSTAL} (23904) and
 *       {@code GAUNTLET_TELEPORT_CRYSTAL_HM} (23858). (The WESTERN_ELITE
 *       diary alternative that used to sit at [0] was removed by the
 *       wiki-meta audit as fabricated.)</li>
 * </ul>
 *
 * <p>Mirrors {@code C6B3OverlapRegressionTest} (stacked-alternative shape)
 * for ToA/Zulrah/Gauntlets.
 *
 * <p>The original wave-8a Phantom Muspah entry (Quetzal-whistle inventory
 * alternative) was removed from the corpus: the Quetzal Transport System
 * serves Varlamore landing sites only and has no Muspah destination.
 */
public class CExtensionInventoryTeleportRegressionTest
{
	/** Pharaoh's sceptre variants: uncharged tiers 1-3, charged, charged_initial (jeweled). */
	private static final List<Integer> PHARAOHS_SCEPTRE_VARIANTS = List.of(
		26945, 26946, 26947, 26948, 26949, 26950, 26951);

	/** Teleport crystal variants for Prifddinas: regular + Hard-Mode-charged. */
	private static final List<Integer> GAUNTLET_TELEPORT_CRYSTAL_VARIANTS = List.of(
		23904, 23858);

	/** Zul-andra teleport scroll, single-ID inventory item. */
	private static final List<Integer> ZULANDRA_SCROLL = List.of(12938);

	/**
	 * Per-source spec: index where the inventory alternative is expected,
	 * whether the alternative is OR-semantic ({@code inventoryItemIdsAny}) or
	 * AND-semantic ({@code inventoryItemIds}), and the expected ID list.
	 */
	private static final Map<String, InventorySpec> SPECS = buildSpecs();

	private static Map<String, InventorySpec> buildSpecs()
	{
		Map<String, InventorySpec> map = new LinkedHashMap<>();
		map.put("Tombs of Amascut",
			new InventorySpec(1, true, PHARAOHS_SCEPTRE_VARIANTS, 2));
		map.put("Tombs of Amascut (300 Invocation)",
			new InventorySpec(1, true, PHARAOHS_SCEPTRE_VARIANTS, 2));
		map.put("Tombs of Amascut (500 Invocation)",
			new InventorySpec(1, true, PHARAOHS_SCEPTRE_VARIANTS, 2));
		map.put("Zulrah",
			new InventorySpec(1, false, ZULANDRA_SCROLL, 2));
		// Wiki-meta audit fix: the WESTERN_ELITE diary alternative both
		// Gauntlet sources used to stack ahead of the inventory alternative
		// was fabricated (the Elite Western diary attaches no Prifddinas
		// teleport to any cape), so the inventory alternative is now the
		// only conditional alternative, at index 0.
		map.put("Corrupted Gauntlet",
			new InventorySpec(0, true, GAUNTLET_TELEPORT_CRYSTAL_VARIANTS, 1));
		map.put("The Gauntlet",
			new InventorySpec(0, true, GAUNTLET_TELEPORT_CRYSTAL_VARIANTS, 1));
		return map;
	}

	/**
	 * Per-source base-step description tokens that MUST still appear in the
	 * unmodified first guidance step, proving the inventory-teleport edit
	 * only touched {@code conditionalAlternatives} and left the base route
	 * intact.
	 */
	private static final Map<String, String> EXPECTED_BASE_DESCRIPTION_MARKER = buildBaseMarkers();

	private static Map<String, String> buildBaseMarkers()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Tombs of Amascut", "Pharaoh's sceptre to teleport to the Necropolis");
		map.put("Tombs of Amascut (300 Invocation)", "Pharaoh's sceptre to teleport to the Necropolis");
		map.put("Tombs of Amascut (500 Invocation)", "Pharaoh's sceptre to teleport to the Necropolis");
		map.put("Zulrah", "using a Zul-andra teleport scroll");
		map.put("Corrupted Gauntlet", "Teleport to Prifddinas via teleport crystal");
		map.put("The Gauntlet", "Teleport to Prifddinas via teleport crystal");
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
	public void inventoryAlternativeIsAtExpectedIndexWithExpectedIds()
	{
		for (Map.Entry<String, InventorySpec> entry : SPECS.entrySet())
		{
			String name = entry.getKey();
			InventorySpec spec = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(),
				name + ": guidanceSteps must not be empty");

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
			assertNotNull(alternatives,
				name + ": first step must declare conditionalAlternatives");
			assertEquals(spec.expectedTotalAlternatives, alternatives.size(),
				name + ": expected " + spec.expectedTotalAlternatives
					+ " conditionalAlternatives entries, found " + alternatives.size());

			ConditionalAlternative alt = alternatives.get(spec.index);
			assertNotNull(alt,
				name + ": conditional alternative missing at index " + spec.index);

			SourceRequirements reqs = alt.getRequirements();
			assertNotNull(reqs,
				name + ": inventory alternative must declare requirements");

			if (spec.anySemantic)
			{
				assertNull(reqs.getInventoryItemIds(),
					name + ": inventoryItemIds should be null on an OR-semantic alternative");
				assertEquals(spec.expectedIds, reqs.getInventoryItemIdsAny(),
					name + ": inventoryItemIdsAny mismatch at index " + spec.index);
			}
			else
			{
				assertNull(reqs.getInventoryItemIdsAny(),
					name + ": inventoryItemIdsAny should be null on an AND-semantic alternative");
				assertEquals(spec.expectedIds, reqs.getInventoryItemIds(),
					name + ": inventoryItemIds mismatch at index " + spec.index);
			}

			assertNotNull(alt.getDescription(),
				name + ": inventory alternative must override description");
			assertTrue(
				alt.getDescription().toLowerCase().contains("inventory")
					|| alt.getDescription().toLowerCase().contains("right-click"),
				name + ": inventory alternative description should mention inventory/right-click; got: "
					+ alt.getDescription());

			assertNotNull(alt.getTravelTip(),
				name + ": inventory alternative must override travelTip");
			assertTrue(alt.getTravelTip().toLowerCase().contains("inventory"),
				name + ": inventory alternative travelTip should mention inventory; got: "
					+ alt.getTravelTip());
		}
	}

	@Test
	public void priorAlternativesArePreserved()
	{
		// Zulrah: existing POH (FAIRY_RING) at [0] must survive the insertion at [1].
		ConditionalAlternative zulrahPoh =
			findSource("Zulrah").getGuidanceSteps().get(0).getConditionalAlternatives().get(0);
		assertNotNull(zulrahPoh.getRequirements());
		assertEquals(List.of("FAIRY_RING"), zulrahPoh.getRequirements().getPohTeleports(),
			"Zulrah: pre-existing FAIRY_RING POH alternative must remain at index 0");

		// ToA x3: existing DESERT_HARD diary at [0] must survive.
		for (String name : List.of(
			"Tombs of Amascut",
			"Tombs of Amascut (300 Invocation)",
			"Tombs of Amascut (500 Invocation)"))
		{
			ConditionalAlternative diary =
				findSource(name).getGuidanceSteps().get(0).getConditionalAlternatives().get(0);
			assertNotNull(diary.getRequirements());
			assertEquals(List.of("DESERT_HARD"), diary.getRequirements().getDiaries(),
				name + ": pre-existing DESERT_HARD diary alternative must remain at index 0");
		}

		// Gauntlets: the fabricated WESTERN_ELITE diary alternative must NOT
		// return (wiki-meta audit: the Elite Western diary attaches no
		// Prifddinas teleport to any cape).
		for (String name : List.of("Corrupted Gauntlet", "The Gauntlet"))
		{
			for (ConditionalAlternative alt
				: findSource(name).getGuidanceSteps().get(0).getConditionalAlternatives())
			{
				if (alt.getRequirements() != null && alt.getRequirements().getDiaries() != null)
				{
					assertFalse(alt.getRequirements().getDiaries().contains("WESTERN_ELITE"),
						name + ": the fabricated WESTERN_ELITE diary alternative must not be reintroduced");
				}
			}
		}
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
			assertNotNull(baseDescription,
				name + ": base step description must not be null");
			assertTrue(
				baseDescription.contains(expectedMarker),
				name + ": base step description must still contain the original fallback wording '"
					+ expectedMarker + "'. Got: " + baseDescription);
		}
	}

	@Test
	public void allRemainingSourceFamiliesArePresentInDatabase()
	{
		// Cardinality lock: 6 JSON entries across the 4 remaining source families.
		// The Phantom Muspah Quetzal-whistle alternative was removed: the Quetzal
		// Transport System serves Varlamore landing sites only and has no Muspah
		// destination, so the alternative it asserted was fabricated.
		assertEquals(6, SPECS.size(),
			"C-extension wave-8a covers exactly 6 source entries (ToA x 3 + Zulrah + Gauntlet x 2)");
		for (String name : SPECS.keySet())
		{
			assertNotNull(findSource(name),
				"Expected source missing from drop_rates.json: " + name);
		}
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

	/** Per-source expectation envelope. */
	private static final class InventorySpec
	{
		final int index;
		final boolean anySemantic;
		final List<Integer> expectedIds;
		final int expectedTotalAlternatives;

		InventorySpec(int index, boolean anySemantic, List<Integer> expectedIds,
			int expectedTotalAlternatives)
		{
			this.index = index;
			this.anySemantic = anySemantic;
			this.expectedIds = expectedIds;
			this.expectedTotalAlternatives = expectedTotalAlternatives;
		}
	}
}
