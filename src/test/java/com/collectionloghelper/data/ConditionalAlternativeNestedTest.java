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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B3 — value-type tests for {@link ConditionalAlternative#getNestedAlternatives()}.
 *
 * <p>Covers: field presence, null/empty sentinel behaviour, equals/hashCode, and
 * Gson round-trip deserialisation. Lombok {@code @Value} makes the generated getter
 * return the list reference directly (fields are {@code final}), so no defensive copy
 * is required — immutability is guaranteed at construction time.
 */
public class ConditionalAlternativeNestedTest
{
	// ── nestedAlternatives: null by default ───────────────────────────────────

	@Test
	public void nestedAlternatives_notSet_isNull()
	{
		ConditionalAlternative alt = flatAlt("description", null);
		assertNull(alt.getNestedAlternatives());
	}

	// ── nestedAlternatives: empty list ────────────────────────────────────────

	@Test
	public void nestedAlternatives_emptyList_isEmptyNotNull()
	{
		ConditionalAlternative alt = flatAlt("description", Collections.emptyList());
		assertNotNull(alt.getNestedAlternatives());
		assertTrue(alt.getNestedAlternatives().isEmpty());
	}

	// ── nestedAlternatives: single child ──────────────────────────────────────

	@Test
	public void nestedAlternatives_singleChild_roundTrips()
	{
		ConditionalAlternative child = flatAlt("child description", null);
		ConditionalAlternative parent = flatAlt("parent description", Collections.singletonList(child));

		List<ConditionalAlternative> nested = parent.getNestedAlternatives();
		assertNotNull(nested);
		assertEquals(1, nested.size());
		assertEquals("child description", nested.get(0).getDescription());
		assertNull(nested.get(0).getNestedAlternatives());
	}

	// ── nestedAlternatives: multiple children ─────────────────────────────────

	@Test
	public void nestedAlternatives_multipleChildren_preservesOrder()
	{
		ConditionalAlternative child1 = flatAlt("child 1", null);
		ConditionalAlternative child2 = flatAlt("child 2", null);
		ConditionalAlternative parent = flatAlt("parent", Arrays.asList(child1, child2));

		List<ConditionalAlternative> nested = parent.getNestedAlternatives();
		assertEquals(2, nested.size());
		assertEquals("child 1", nested.get(0).getDescription());
		assertEquals("child 2", nested.get(1).getDescription());
	}

	// ── equals/hashCode: two instances with identical fields are equal ─────────

	@Test
	public void equals_sameFields_areEqual()
	{
		ConditionalAlternative a = flatAlt("desc", null);
		ConditionalAlternative b = flatAlt("desc", null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equals_withMatchingNestedAlternatives_areEqual()
	{
		ConditionalAlternative child = flatAlt("child", null);
		ConditionalAlternative a = flatAlt("parent", Collections.singletonList(child));
		ConditionalAlternative b = flatAlt("parent", Collections.singletonList(flatAlt("child", null)));
		assertEquals(a, b);
	}

	// ── Gson deserialisation: absent field → null ─────────────────────────────

	@Test
	public void deserialise_missingNestedAlternatives_fieldIsNull()
	{
		Gson gson = new GsonBuilder().create();
		String json = "{\"description\":\"flat alt\"}";
		ConditionalAlternative alt = gson.fromJson(json, ConditionalAlternative.class);
		assertNull(
			alt.getNestedAlternatives(),"nestedAlternatives should be null when absent from JSON");
	}

	// ── Gson deserialisation: inline nested array ─────────────────────────────

	@Test
	public void deserialise_withNestedAlternatives_populatesList()
	{
		Gson gson = new GsonBuilder().create();
		String json = "{"
			+ "\"description\":\"parent alt\","
			+ "\"nestedAlternatives\":["
			+ "  {\"description\":\"child alt A\"},"
			+ "  {\"description\":\"child alt B\",\"worldX\":3000}"
			+ "]"
			+ "}";

		ConditionalAlternative alt = gson.fromJson(json, ConditionalAlternative.class);

		assertNotNull(alt.getNestedAlternatives());
		assertEquals(2, alt.getNestedAlternatives().size());
		assertEquals("child alt A", alt.getNestedAlternatives().get(0).getDescription());
		assertEquals("child alt B", alt.getNestedAlternatives().get(1).getDescription());
		assertEquals(Integer.valueOf(3000), alt.getNestedAlternatives().get(1).getWorldX());
	}

	// ── Gson deserialisation: 3-level nesting ────────────────────────────────

	@Test
	public void deserialise_threeLevelNesting_structureIsCorrect()
	{
		Gson gson = new GsonBuilder().create();
		String json = "{"
			+ "\"description\":\"level 1\","
			+ "\"nestedAlternatives\":[{"
			+ "  \"description\":\"level 2\","
			+ "  \"nestedAlternatives\":[{"
			+ "    \"description\":\"level 3\""
			+ "  }]"
			+ "}]"
			+ "}";

		ConditionalAlternative level1 = gson.fromJson(json, ConditionalAlternative.class);

		assertEquals("level 1", level1.getDescription());
		ConditionalAlternative level2 = level1.getNestedAlternatives().get(0);
		assertEquals("level 2", level2.getDescription());
		ConditionalAlternative level3 = level2.getNestedAlternatives().get(0);
		assertEquals("level 3", level3.getDescription());
		assertNull(level3.getNestedAlternatives());
	}

	// ── Gson serialise then deserialise round-trip ────────────────────────────

	@Test
	public void gsonRoundTrip_withNesting_preservesStructure()
	{
		Gson gson = new GsonBuilder().create();
		ConditionalAlternative inner = flatAlt("inner desc", null);
		ConditionalAlternative outer = flatAlt("outer desc", Collections.singletonList(inner));

		String json = gson.toJson(outer);
		ConditionalAlternative restored = gson.fromJson(json, ConditionalAlternative.class);

		assertEquals("outer desc", restored.getDescription());
		assertNotNull(restored.getNestedAlternatives());
		assertEquals("inner desc", restored.getNestedAlternatives().get(0).getDescription());
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	/**
	 * Constructs a minimal {@link ConditionalAlternative} with only description and
	 * nestedAlternatives set; all other override fields are null.
	 */
	static ConditionalAlternative flatAlt(String description,
		List<ConditionalAlternative> nestedAlternatives)
	{
		return new ConditionalAlternative(
			null,              // requirements
			description,
			null, null, null,  // worldX, worldY, worldPlane
			null,              // travelTip
			null,              // npcId
			null,              // interactAction
			null,              // objectId
			null,              // completionCondition
			null,              // completionDistance
			null,              // completionNpcId
			nestedAlternatives
		);
	}
}
