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
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gson deserialisation cases for the new {@code perItemStepPriority} schema
 * field on {@link GuidanceStep} (B4.3.4 Phase 1+2).
 *
 * <p>Covers: absent, explicit null, empty object, single entry, multiple
 * entries, and malformed values. The malformed cases verify that Gson surfaces
 * a {@link JsonSyntaxException} rather than silently accepting bad data --
 * the regression suite and the {@code B4_3_4_MigrationTest} both rely on
 * authoring-time failures, not silent map corruption.
 */
public class PerItemStepPriorityDeserializerTest
{
	private final Gson gson = new GsonBuilder().create();

	// -- absent -----------------------------------------------------------------

	@Test
	public void absentField_deserialisesToNullMap()
	{
		String json = "{"
			+ "\"description\":\"Kill brother\","
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull(step);
		assertNull(step.getPerItemStepPriority(),
			"perItemStepPriority must be null when absent from JSON (additive-field invariant)");
	}

	// -- explicit null ----------------------------------------------------------

	@Test
	public void explicitNull_deserialisesToNullMap()
	{
		String json = "{"
			+ "\"description\":\"Kill brother\","
			+ "\"perItemStepPriority\":null,"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNull(step.getPerItemStepPriority());
	}

	// -- empty object -----------------------------------------------------------

	@Test
	public void emptyObject_deserialisesToEmptyMap()
	{
		String json = "{"
			+ "\"description\":\"Kill brother\","
			+ "\"perItemStepPriority\":{},"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		Map<Integer, Integer> map = step.getPerItemStepPriority();
		assertNotNull(map, "empty JSON object should deserialise to an empty (non-null) map");
		assertTrue(map.isEmpty(), "empty JSON object must yield an empty map");
	}

	// -- single entry -----------------------------------------------------------

	@Test
	public void singleEntry_deserialisesIntoMap()
	{
		String json = "{"
			+ "\"description\":\"Kill Ahrim\","
			+ "\"perItemStepPriority\":{\"4708\":100},"
			+ "\"completionCondition\":\"VARBIT_AT_LEAST\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		Map<Integer, Integer> map = step.getPerItemStepPriority();
		assertNotNull(map);
		assertEquals(1, map.size());
		assertEquals(Integer.valueOf(100), map.get(4708));
	}

	// -- multiple entries (Ahrim's four set pieces) ----------------------------

	@Test
	public void multipleEntries_allKeysAndValuesPreserved()
	{
		// Ahrim's hood / robetop / robeskirt / staff per the B4.3.4 design table.
		String json = "{"
			+ "\"description\":\"Kill Ahrim\","
			+ "\"perItemStepPriority\":{"
			+ "  \"4708\":100,"
			+ "  \"4712\":100,"
			+ "  \"4714\":100,"
			+ "  \"4710\":100"
			+ "},"
			+ "\"completionCondition\":\"VARBIT_AT_LEAST\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		Map<Integer, Integer> map = step.getPerItemStepPriority();
		assertNotNull(map);
		assertEquals(4, map.size());
		assertEquals(Integer.valueOf(100), map.get(4708));
		assertEquals(Integer.valueOf(100), map.get(4712));
		assertEquals(Integer.valueOf(100), map.get(4714));
		assertEquals(Integer.valueOf(100), map.get(4710));
	}

	// -- distinct priority values per item -------------------------------------

	@Test
	public void distinctValues_preservedPerKey()
	{
		String json = "{"
			+ "\"description\":\"Kill mixed minions\","
			+ "\"perItemStepPriority\":{\"100\":1,\"200\":50,\"300\":999},"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		Map<Integer, Integer> map = step.getPerItemStepPriority();
		assertEquals(Integer.valueOf(1), map.get(100));
		assertEquals(Integer.valueOf(50), map.get(200));
		assertEquals(Integer.valueOf(999), map.get(300));
	}

	// -- malformed: non-numeric value ------------------------------------------

	@Test
	public void malformedValue_nonNumeric_throws()
	{
		String json = "{"
			+ "\"description\":\"Kill brother\","
			+ "\"perItemStepPriority\":{\"4708\":\"high\"},"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		assertThrows(JsonSyntaxException.class,
			() -> gson.fromJson(json, GuidanceStep.class),
			"non-numeric priority value must fail at deserialisation time");
	}

	// -- malformed: array instead of object ------------------------------------

	@Test
	public void malformedShape_arrayInsteadOfObject_throws()
	{
		String json = "{"
			+ "\"description\":\"Kill brother\","
			+ "\"perItemStepPriority\":[1,2,3],"
			+ "\"completionCondition\":\"MANUAL\""
			+ "}";

		assertThrows(JsonSyntaxException.class,
			() -> gson.fromJson(json, GuidanceStep.class),
			"array value for perItemStepPriority must fail at deserialisation time");
	}
}
