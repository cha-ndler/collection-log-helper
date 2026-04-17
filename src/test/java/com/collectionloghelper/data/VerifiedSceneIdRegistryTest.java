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
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VerifiedSceneIdRegistryTest
{
	private VerifiedSceneIdRegistry registry;

	@Before
	public void setUp()
	{
		registry = VerifiedSceneIdRegistry.load(new Gson());
	}

	// ========================================================================
	// load — JSON schema integrity
	// ========================================================================

	@Test
	public void load_succeeds_returnsNonEmptyRegistry()
	{
		assertFalse("Registry should contain at least one entry", registry.all().isEmpty());
	}

	@Test
	public void load_allEntriesHaveObjectName()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull("objectName must not be null", entry.getObjectName());
			assertFalse("objectName must not be blank", entry.getObjectName().isBlank());
		}
	}

	@Test
	public void load_allEntriesHaveNonZeroExamineId()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertTrue(
				"examineId must be > 0 for " + entry.getObjectName(),
				entry.getExamineId() > 0);
		}
	}

	@Test
	public void load_allEntriesHaveAtLeastOneSceneId()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull("sceneIds must not be null for " + entry.getObjectName(), entry.getSceneIds());
			assertFalse(
				"sceneIds must not be empty for " + entry.getObjectName(),
				entry.getSceneIds().isEmpty());
		}
	}

	@Test
	public void load_allEntriesHaveConfidence()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull("confidence must not be null for " + entry.getObjectName(), entry.getConfidence());
			assertFalse("confidence must not be blank for " + entry.getObjectName(), entry.getConfidence().isBlank());
		}
	}

	@Test
	public void load_allEntriesHaveSource()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull("source must not be null for " + entry.getObjectName(), entry.getSource());
			assertFalse("source must not be blank for " + entry.getObjectName(), entry.getSource().isBlank());
		}
	}

	@Test
	public void load_atLeastTenEntries()
	{
		assertTrue(
			"Expected at least 10 seeded entries, got " + registry.all().size(),
			registry.all().size() >= 10);
	}

	// ========================================================================
	// lookupByExamineId — canonical divergence case
	// ========================================================================

	@Test
	public void lookupByExamineId_shellbaneGryphonCacheId_found()
	{
		// 58441 is the examine/cache ID for Shellbane Gryphon cave entrance
		Optional<VerifiedSceneId> result = registry.lookupByExamineId(58441);
		assertTrue("Shellbane Gryphon examine ID 58441 must be in the registry", result.isPresent());
		assertEquals("Shellbane Gryphon Cave entrance", result.get().getObjectName());
	}

	@Test
	public void lookupByExamineId_unknownId_empty()
	{
		Optional<VerifiedSceneId> result = registry.lookupByExamineId(999999);
		assertFalse("Unknown examine ID must return empty", result.isPresent());
	}

	// ========================================================================
	// lookupBySceneId — scene-tile ID lookup
	// ========================================================================

	@Test
	public void lookupBySceneId_shellbaneGryphonSceneId_found()
	{
		// 58439 is the actual scene tile ID rendered for the Shellbane Gryphon entrance
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(58439);
		assertTrue("Shellbane Gryphon scene ID 58439 must be in the registry", result.isPresent());
		assertEquals("Shellbane Gryphon Cave entrance", result.get().getObjectName());
	}

	@Test
	public void lookupBySceneId_chambersOfXericId_found()
	{
		// 29777 is the confirmed scene ID for Chambers of Xeric entrance
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(29777);
		assertTrue("CoX scene ID 29777 must be in the registry", result.isPresent());
	}

	@Test
	public void lookupBySceneId_unknownId_empty()
	{
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(1);
		assertFalse("Unknown scene ID must return empty", result.isPresent());
	}

	// ========================================================================
	// all — immutability
	// ========================================================================

	@Test(expected = UnsupportedOperationException.class)
	public void all_returnsUnmodifiableList()
	{
		registry.all().add(null);
	}

	// ========================================================================
	// Cross-lookup consistency — examineId and sceneId resolve same entry
	// ========================================================================

	@Test
	public void crossLookup_shellbane_examineAndSceneReturnSameEntry()
	{
		Optional<VerifiedSceneId> byExamine = registry.lookupByExamineId(58441);
		Optional<VerifiedSceneId> byScene = registry.lookupBySceneId(58439);
		assertTrue(byExamine.isPresent());
		assertTrue(byScene.isPresent());
		assertEquals(byExamine.get().getObjectName(), byScene.get().getObjectName());
	}

	@Test
	public void crossLookup_nonDivergentEntry_examineAndSceneReturnSameEntry()
	{
		// Theatre of Blood (32653): examine ID == scene ID, both should resolve the same entry
		Optional<VerifiedSceneId> byExamine = registry.lookupByExamineId(32653);
		Optional<VerifiedSceneId> byScene = registry.lookupBySceneId(32653);
		assertTrue("ToB examine ID 32653 must be in registry", byExamine.isPresent());
		assertTrue("ToB scene ID 32653 must be in registry", byScene.isPresent());
		assertEquals(byExamine.get().getObjectName(), byScene.get().getObjectName());
	}

	// ========================================================================
	// Schema version — tolerant of unsupported and missing version fields
	// ========================================================================

	@Test
	public void loadFromJson_unsupportedVersion_stillReturnsEntries()
	{
		String json = "{"
			+ "\"version\": 99,"
			+ "\"entries\": ["
			+ "  {"
			+ "    \"objectName\": \"Test entrance\","
			+ "    \"examineId\": 11111,"
			+ "    \"sceneIds\": [11112],"
			+ "    \"confidence\": \"high\","
			+ "    \"source\": \"unit test\","
			+ "    \"notes\": \"synthetic fixture for schema-version tolerance\""
			+ "  }"
			+ "]"
			+ "}";

		VerifiedSceneIdRegistry loaded = VerifiedSceneIdRegistry.loadFromJson(json, new Gson());

		assertEquals(
			"Unsupported schema version must still yield entries (loader warns, does not drop)",
			1,
			loaded.all().size());
		assertTrue(loaded.lookupByExamineId(11111).isPresent());
		assertTrue(loaded.lookupBySceneId(11112).isPresent());
	}

	@Test
	public void loadFromJson_missingVersion_stillReturnsEntries()
	{
		// Regression guard: prior implementation NPE'd on root.get("version").getAsInt() when the
		// field was absent. The load-path swallowed the NPE via a broad catch and returned an empty
		// registry — a silent degradation. The loader now null-guards the version read.
		String json = "{"
			+ "\"entries\": ["
			+ "  {"
			+ "    \"objectName\": \"Version-less entrance\","
			+ "    \"examineId\": 22222,"
			+ "    \"sceneIds\": [22222],"
			+ "    \"confidence\": \"high\","
			+ "    \"source\": \"unit test\""
			+ "  }"
			+ "]"
			+ "}";

		VerifiedSceneIdRegistry loaded = VerifiedSceneIdRegistry.loadFromJson(json, new Gson());

		assertEquals(
			"Missing version field must be tolerated, not NPE or silently return empty",
			1,
			loaded.all().size());
		assertTrue(loaded.lookupByExamineId(22222).isPresent());
	}

	@Test
	public void loadFromJson_malformedJson_returnsEmpty()
	{
		VerifiedSceneIdRegistry loaded = VerifiedSceneIdRegistry.loadFromJson("{not valid json", new Gson());
		assertTrue("Malformed JSON must return an empty registry, not throw", loaded.all().isEmpty());
	}

	// ========================================================================
	// Divergence detection — Shellbane is the only known examine/scene split
	// ========================================================================

	@Test
	public void divergentEntries_shellbaneIsPresent()
	{
		long divergent = registry.all().stream()
			.filter(e -> !e.getSceneIds().contains(e.getExamineId()))
			.count();
		assertTrue("At least one divergent (examine != scene) entry must exist", divergent >= 1);
	}

	@Test
	public void shellbaneEntry_examineIdDiffersFromSceneId()
	{
		Optional<VerifiedSceneId> entry = registry.lookupByExamineId(58441);
		assertTrue(entry.isPresent());
		assertFalse(
			"Shellbane examine ID 58441 must not appear in sceneIds list",
			entry.get().getSceneIds().contains(58441));
		assertTrue(
			"Shellbane scene ID 58439 must appear in sceneIds list",
			entry.get().getSceneIds().contains(58439));
	}

	// ========================================================================
	// empty() factory
	// ========================================================================

	@Test
	public void empty_returnsRegistryWithNoEntries()
	{
		VerifiedSceneIdRegistry empty = VerifiedSceneIdRegistry.empty();
		assertTrue(empty.all().isEmpty());
		assertFalse(empty.lookupByExamineId(58441).isPresent());
		assertFalse(empty.lookupBySceneId(58439).isPresent());
	}
}
