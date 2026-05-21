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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VerifiedSceneIdRegistryTest
{
	private VerifiedSceneIdRegistry registry;

	@BeforeEach
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
		assertFalse( registry.all().isEmpty(),"Registry should contain at least one entry");
	}

	@Test
	public void load_allEntriesHaveObjectName()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull( entry.getObjectName(),"objectName must not be null");
			assertFalse( entry.getObjectName().isBlank(),"objectName must not be blank");
		}
	}

	@Test
	public void load_allEntriesHaveNonZeroExamineId()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertTrue(
				entry.getExamineId() > 0,
				"examineId must be > 0 for " + entry.getObjectName());
		}
	}

	@Test
	public void load_allEntriesHaveAtLeastOneSceneId()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull( entry.getSceneIds(),"sceneIds must not be null for " + entry.getObjectName());
			assertFalse(
				entry.getSceneIds().isEmpty(),
				"sceneIds must not be empty for " + entry.getObjectName());
		}
	}

	@Test
	public void load_allEntriesHaveConfidence()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull( entry.getConfidence(),"confidence must not be null for " + entry.getObjectName());
			assertFalse( entry.getConfidence().isBlank(),"confidence must not be blank for " + entry.getObjectName());
		}
	}

	@Test
	public void load_allEntriesHaveSource()
	{
		for (VerifiedSceneId entry : registry.all())
		{
			assertNotNull( entry.getSource(),"source must not be null for " + entry.getObjectName());
			assertFalse( entry.getSource().isBlank(),"source must not be blank for " + entry.getObjectName());
		}
	}

	@Test
	public void load_atLeastTenEntries()
	{
		assertTrue(
			registry.all().size() >= 10,
			"Expected at least 10 seeded entries, got " + registry.all().size());
	}

	// ========================================================================
	// lookupByExamineId — canonical divergence case
	// ========================================================================

	@Test
	public void lookupByExamineId_shellbaneGryphonCacheId_found()
	{
		// 58441 is the examine/cache ID for Shellbane Gryphon cave entrance
		Optional<VerifiedSceneId> result = registry.lookupByExamineId(58441);
		assertTrue( result.isPresent(),"Shellbane Gryphon examine ID 58441 must be in the registry");
		assertEquals("Shellbane Gryphon Cave entrance", result.get().getObjectName());
	}

	@Test
	public void lookupByExamineId_unknownId_empty()
	{
		Optional<VerifiedSceneId> result = registry.lookupByExamineId(999999);
		assertFalse( result.isPresent(),"Unknown examine ID must return empty");
	}

	// ========================================================================
	// lookupBySceneId — scene-tile ID lookup
	// ========================================================================

	@Test
	public void lookupBySceneId_shellbaneGryphonSceneId_found()
	{
		// 58439 is the actual scene tile ID rendered for the Shellbane Gryphon entrance
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(58439);
		assertTrue( result.isPresent(),"Shellbane Gryphon scene ID 58439 must be in the registry");
		assertEquals("Shellbane Gryphon Cave entrance", result.get().getObjectName());
	}

	@Test
	public void lookupBySceneId_chambersOfXericId_found()
	{
		// 29777 is the confirmed scene ID for Chambers of Xeric entrance
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(29777);
		assertTrue( result.isPresent(),"CoX scene ID 29777 must be in the registry");
	}

	@Test
	public void lookupBySceneId_unknownId_empty()
	{
		Optional<VerifiedSceneId> result = registry.lookupBySceneId(1);
		assertFalse( result.isPresent(),"Unknown scene ID must return empty");
	}

	// ========================================================================
	// all — immutability
	// ========================================================================

	@Test
	public void all_returnsUnmodifiableList()
	{
		assertThrows(UnsupportedOperationException.class, () ->
		{
			registry.all().add(null);
		});
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
		assertTrue( byExamine.isPresent(),"ToB examine ID 32653 must be in registry");
		assertTrue( byScene.isPresent(),"ToB scene ID 32653 must be in registry");
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
			1,
			loaded.all().size(),
			"Unsupported schema version must still yield entries (loader warns, does not drop)");
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
			1,
			loaded.all().size(),
			"Missing version field must be tolerated, not NPE or silently return empty");
		assertTrue(loaded.lookupByExamineId(22222).isPresent());
	}

	@Test
	public void loadFromJson_malformedJson_returnsEmpty()
	{
		VerifiedSceneIdRegistry loaded = VerifiedSceneIdRegistry.loadFromJson("{not valid json", new Gson());
		assertTrue( loaded.all().isEmpty(),"Malformed JSON must return an empty registry, not throw");
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
		assertTrue( divergent >= 1,"At least one divergent (examine != scene) entry must exist");
	}

	@Test
	public void shellbaneEntry_examineIdDiffersFromSceneId()
	{
		Optional<VerifiedSceneId> entry = registry.lookupByExamineId(58441);
		assertTrue(entry.isPresent());
		assertFalse(
			entry.get().getSceneIds().contains(58441),
			"Shellbane examine ID 58441 must not appear in sceneIds list");
		assertTrue(
			entry.get().getSceneIds().contains(58439),
			"Shellbane scene ID 58439 must appear in sceneIds list");
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
