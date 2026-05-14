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
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Value-type tests for the {@code section} field added to {@link GuidanceStep} in B.5.4.
 *
 * <p>Acceptance criteria:
 * <ol>
 *   <li>A step constructed with a non-null section returns that value via {@link GuidanceStep#getSection()}.</li>
 *   <li>A step constructed with a null section returns null via {@link GuidanceStep#getSection()}.</li>
 *   <li>JSON without a {@code section} field deserialises to a null section (backwards-compatible).</li>
 *   <li>JSON with a {@code section} field deserialises to the correct string value.</li>
 *   <li>The section field is preserved after {@code resolveAlternative} when no alternative matches.</li>
 * </ol>
 */
public class GuidanceStepSectionTest
{
	private static final Gson GSON = new GsonBuilder().create();

	// ── Value-type: getSection() ─────────────────────────────────────────────

	@Test
	public void getSection_nonNull_returnsLabel()
	{
		GuidanceStep step = stepWithSection("Travel");
		assertEquals("Travel", step.getSection());
	}

	@Test
	public void getSection_null_returnsNull()
	{
		GuidanceStep step = stepWithSection(null);
		assertNull("section must be null when constructed with null", step.getSection());
	}

	// ── JSON deserialisation: backwards-compatible ───────────────────────────

	@Test
	public void deserialise_jsonWithoutSection_sectionIsNull()
	{
		String json = "{\"description\":\"Kill the boss\",\"completionCondition\":\"MANUAL\"}";
		GuidanceStep step = GSON.fromJson(json, GuidanceStep.class);
		assertNull("section must be null when absent from JSON", step.getSection());
	}

	// ── JSON deserialisation: field present ──────────────────────────────────

	@Test
	public void deserialise_jsonWithSection_sectionPopulated()
	{
		String json = "{"
			+ "\"description\":\"Bank prep\","
			+ "\"completionCondition\":\"MANUAL\","
			+ "\"section\":\"Bank prep\""
			+ "}";
		GuidanceStep step = GSON.fromJson(json, GuidanceStep.class);
		assertEquals("Bank prep", step.getSection());
	}

	@Test
	public void deserialise_jsonWithTravelSection_correctValue()
	{
		String json = "{"
			+ "\"description\":\"Walk to boss\","
			+ "\"completionCondition\":\"MANUAL\","
			+ "\"section\":\"Travel\""
			+ "}";
		GuidanceStep step = GSON.fromJson(json, GuidanceStep.class);
		assertEquals("Travel", step.getSection());
	}

	// ── Section preserved through resolveAlternative (no match) ─────────────

	@Test
	public void resolveAlternative_noMatchingAlternative_sectionPreserved()
	{
		GuidanceStep step = stepWithSection("Combat");
		RequirementsChecker checker = Mockito.mock(RequirementsChecker.class);
		Mockito.when(checker.meetsRequirements(Mockito.any())).thenReturn(false);
		// No conditionalAlternatives set; resolveAlternative returns this step unchanged
		GuidanceStep resolved = step.resolveAlternative(checker);
		assertEquals("section must be preserved when no alternative matches",
			"Combat", resolved.getSection());
	}

	@Test
	public void resolveAlternative_nullAlternatives_sectionPreserved()
	{
		GuidanceStep step = stepWithSection("Loot");
		RequirementsChecker checker = Mockito.mock(RequirementsChecker.class);
		Mockito.when(checker.meetsRequirements(Mockito.any())).thenReturn(true);
		GuidanceStep resolved = step.resolveAlternative(checker);
		assertEquals("section must be preserved when conditionalAlternatives is null",
			"Loot", resolved.getSection());
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private static GuidanceStep stepWithSection(String section)
	{
		return new GuidanceStep(
			"Test step",
			null,   // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,   // perItemRequiredItemIds
			null,   // recommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,   // completionNpcIds
			null,   // worldMessage
			0, null, null,
			null, null,
			null,   // completionChatPattern
			0, 0,
			false,
			0, null, null,
			0, 0,
			null, null, null,
			null,   // conditionalAlternatives
			section
		);
	}
}
