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
 * D5 batch A regression guard - locks the POH-teleport conditional alternatives
 * wired on slayer and mid-tier boss sources:
 *
 * <ul>
 *   <li>Abyssal Demon / Aberrant Spectre / Nechryael:
 *       {@code JEWELLERY_BOX_FANCY} -&gt; Combat bracelet -&gt; Ranging Guild.</li>
 *   <li>Smoke Devil: {@code JEWELLERY_BOX_BASIC} -&gt; Ring of dueling -&gt; Castle Wars.</li>
 *   <li>Dark Beast: {@code FAIRY_RING} -&gt; CIQ -&gt; Iorwerth Dungeon.</li>
 *   <li>Sarachnis: {@code XERICS_TALISMAN} -&gt; Xeric's Heart.</li>
 *   <li>Giant Mole: {@code JEWELLERY_BOX_ORNATE} -&gt; Ring of wealth -&gt; Falador.</li>
 *   <li>Obor: {@code MOUNTED_GLORY} -&gt; Edgeville bank.</li>
 * </ul>
 *
 * <p>For each source the wired guidance step must:
 * <ul>
 *   <li>Carry at least one {@code conditionalAlternatives} entry.</li>
 *   <li>That entry's {@code requirements.pohTeleports} must equal the expected
 *       single-element list of {@link com.collectionloghelper.player.PohTeleport}
 *       enum-name strings.</li>
 *   <li>That entry must override both {@code description} and {@code travelTip}.</li>
 *   <li>The base step's description must still contain the original fallback wording,
 *       proving the unrestricted route was not replaced.</li>
 * </ul>
 *
 * <p>Pattern mirrors {@code C6B2RegressionTest}.
 */
public class D5ABranchRegressionTest
{
	private static final String JEWELLERY_BOX_BASIC = "JEWELLERY_BOX_BASIC";
	private static final String JEWELLERY_BOX_FANCY = "JEWELLERY_BOX_FANCY";
	private static final String JEWELLERY_BOX_ORNATE = "JEWELLERY_BOX_ORNATE";
	private static final String MOUNTED_GLORY = "MOUNTED_GLORY";
	private static final String FAIRY_RING = "FAIRY_RING";
	private static final String XERICS_TALISMAN = "XERICS_TALISMAN";

	/**
	 * Per-source expectations: source name -&gt; [step index, expected pohTeleports list].
	 * Insertion order is preserved for stable failure reporting.
	 */
	private static final Map<String, StepExpectation> EXPECTED = buildExpected();

	private static Map<String, StepExpectation> buildExpected()
	{
		Map<String, StepExpectation> map = new LinkedHashMap<>();
		map.put("Abyssal Demon",    new StepExpectation(0, List.of(JEWELLERY_BOX_FANCY),  "Slayer Tower top floor (slayer ring)"));
		map.put("Aberrant Spectre", new StepExpectation(0, List.of(JEWELLERY_BOX_FANCY),  "Slayer Tower (slayer ring or fairy ring CKS)"));
		map.put("Nechryael",        new StepExpectation(0, List.of(JEWELLERY_BOX_FANCY),  "Slayer Tower (slayer ring or fairy ring CKS)"));
		map.put("Smoke Devil",      new StepExpectation(0, List.of(JEWELLERY_BOX_BASIC),  "Smoke Devil Dungeon south of Castle Wars"));
		map.put("Dark Beast",       new StepExpectation(0, List.of(FAIRY_RING),            "Iorwerth Dungeon in Prifddinas"));
		map.put("Sarachnis",        new StepExpectation(0, List.of(XERICS_TALISMAN),       "Forthos Dungeon, Hosidius"));
		map.put("Giant Mole",       new StepExpectation(0, List.of(JEWELLERY_BOX_ORNATE), "Falador Park"));
		map.put("Obor",             new StepExpectation(0, List.of(MOUNTED_GLORY),         "Bank in Edgeville"));
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
	public void wiredStepCarriesPohTeleportConditionalAlternative()
	{
		for (Map.Entry<String, StepExpectation> entry : EXPECTED.entrySet())
		{
			String name = entry.getKey();
			StepExpectation expect = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);
			assertNotNull(source.getGuidanceSteps(), name + ": guidanceSteps must not be null");
			assertFalse(source.getGuidanceSteps().isEmpty(), name + ": guidanceSteps must not be empty");
			assertTrue(
				source.getGuidanceSteps().size() > expect.stepIndex,
				name + ": expected at least " + (expect.stepIndex + 1) + " guidance step(s)");

			GuidanceStep step = source.getGuidanceSteps().get(expect.stepIndex);
			List<ConditionalAlternative> alternatives = step.getConditionalAlternatives();
			assertNotNull(alternatives,
				name + ": step[" + expect.stepIndex + "] must declare conditionalAlternatives");
			assertFalse(alternatives.isEmpty(),
				name + ": step[" + expect.stepIndex + "] must have at least one conditional alternative");

			ConditionalAlternative alt = findPohAlternative(alternatives, expect.pohTeleports);
			assertNotNull(alt,
				name + ": expected a conditional alternative with pohTeleports = " + expect.pohTeleports
					+ " but none matched");

			SourceRequirements altReqs = alt.getRequirements();
			assertNotNull(altReqs, name + ": conditional alternative must declare requirements");
			assertNotNull(altReqs.getPohTeleports(), name + ": requirements.pohTeleports must be set");
			assertEquals(
				expect.pohTeleports,
				altReqs.getPohTeleports(),
				name + ": pohTeleports mismatch");

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

	@Test
	public void baseStepDescriptionIsUntouched()
	{
		for (Map.Entry<String, StepExpectation> entry : EXPECTED.entrySet())
		{
			String name = entry.getKey();
			StepExpectation expect = entry.getValue();

			CollectionLogSource source = findSource(name);
			assertNotNull(source, "Expected source: " + name);

			GuidanceStep step = source.getGuidanceSteps().get(expect.stepIndex);
			String baseDescription = step.getDescription();
			assertNotNull(baseDescription, name + ": base step description must not be null");
			assertTrue(
				baseDescription.contains(expect.baseDescriptionMarker),
				name + ": base step must still contain original fallback wording '"
					+ expect.baseDescriptionMarker + "'. Got: " + baseDescription);
		}
	}

	@Test
	public void expectedSourcesArePresentInDatabase()
	{
		for (String name : EXPECTED.keySet())
		{
			assertNotNull(findSource(name), "Expected source missing from drop_rates.json: " + name);
		}
		assertEquals(8, EXPECTED.size(),
			"D5 batch A covers exactly 8 sources");
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

	private static final class StepExpectation
	{
		final int stepIndex;
		final List<String> pohTeleports;
		final String baseDescriptionMarker;

		StepExpectation(int stepIndex, List<String> pohTeleports, String baseDescriptionMarker)
		{
			this.stepIndex = stepIndex;
			this.pohTeleports = pohTeleports;
			this.baseDescriptionMarker = baseDescriptionMarker;
		}
	}
}
