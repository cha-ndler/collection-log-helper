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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1a regression guard — locks the ring-of-shadows ×4 C2 conditional
 * alternatives shipped against the DT2 boss sources.
 *
 * <p>For each of Vardorvis / Duke Sucellus / The Leviathan / The Whisperer the
 * first guidance step must:
 * <ul>
 *   <li>Carry exactly one {@code conditionalAlternatives} entry.</li>
 *   <li>That entry's {@code requirements.equippedItemIds} must equal
 *       {@code [28327]} (charged Ring of shadows worn ItemID — verified via
 *       abextm cache viewer; see
 *       {@code .claude/memory/feedback_item_id_cross_validation.md}).</li>
 *   <li>That entry must override {@code description} and {@code travelTip}.</li>
 *   <li>The base step's description must NOT mention "Ring of Shadows" — the
 *       base must always be a working no-equip fallback per the C6 scoping
 *       doc §5 Q4 charge-aware-fallback rule (the ring's uncharged variant
 *       28329 is a different ItemID and would not satisfy
 *       {@code hasEquipped(28327)}).</li>
 * </ul>
 *
 * <p>Pattern mirrors B2/B3/B5 regression guards: load the production JSON,
 * iterate the named sources, assert each invariant, accumulate violations for
 * a single readable failure message.
 */
public class B1aRegressionTest
{
	private static final int RING_OF_SHADOWS_CHARGED_ITEM_ID = 28327;

	private static final List<String> B1A_SOURCE_NAMES = Arrays.asList(
		"Vardorvis",
		"Duke Sucellus",
		"The Leviathan",
		"The Whisperer"
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
	public void firstStepCarriesRingOfShadowsConditionalAlternative()
	{
		for (String name : B1A_SOURCE_NAMES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(), name + ": guidanceSteps must not be empty");

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			List<ConditionalAlternative> alternatives = firstStep.getConditionalAlternatives();
			assertNotNull(alternatives, name + ": first step must declare conditionalAlternatives");
			assertEquals(
				1,
				alternatives.size(),
				name + ": expected exactly one conditional alternative for the ring-of-shadows route; found "
					+ alternatives.size());

			ConditionalAlternative alt = alternatives.get(0);
			SourceRequirements altReqs = alt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertNotNull(altReqs.getEquippedItemIds(), name + ": requirements.equippedItemIds must be set");
			assertEquals(
				List.of(RING_OF_SHADOWS_CHARGED_ITEM_ID),
				altReqs.getEquippedItemIds(),
				name + ": expected equippedItemIds = [" + RING_OF_SHADOWS_CHARGED_ITEM_ID + "]");

			assertNotNull(alt.getDescription(), name + ": conditional alternative must override description");
			assertTrue(
				alt.getDescription().contains("Ring of Shadows"),
				name + ": alternative description should mention Ring of Shadows; got: " + alt.getDescription());

			assertNotNull(alt.getTravelTip(), name + ": conditional alternative must override travelTip");
			assertTrue(
				alt.getTravelTip().startsWith("Ring of Shadows"),
				name + ": alternative travelTip should start with 'Ring of Shadows'; got: " + alt.getTravelTip());
		}
	}

	@Test
	public void baseStepDescriptionIsNoEquipFallback()
	{
		for (String name : B1A_SOURCE_NAMES)
		{
			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);

			GuidanceStep firstStep = source.getGuidanceSteps().get(0);
			String baseDescription = firstStep.getDescription();
			assertNotNull(baseDescription, name + ": base step description must not be null");
			assertFalse(
				baseDescription.contains("Ring of Shadows"),
				name + ": base step description must NOT mention 'Ring of Shadows' — the base must be a "
					+ "no-equip fallback route (§5 Q4). Got: " + baseDescription);

			List<Integer> baseRequired = firstStep.getRequiredItemIds();
			if (baseRequired != null)
			{
				assertFalse(
					baseRequired.contains(RING_OF_SHADOWS_CHARGED_ITEM_ID),
					name + ": base step requiredItemIds must not include ring of shadows ("
						+ RING_OF_SHADOWS_CHARGED_ITEM_ID + ") — it belongs in the conditional alternative only.");
			}
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
}
