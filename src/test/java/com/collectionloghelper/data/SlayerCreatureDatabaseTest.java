/*
 * Copyright (c) 2025, Chandler
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

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class SlayerCreatureDatabaseTest
{
	// ========================================================================
	// getSourcesForCreature
	// ========================================================================

	@Test
	public void getSourcesForCreature_null_returnsEmpty()
	{
		assertTrue(SlayerCreatureDatabase.getSourcesForCreature(null).isEmpty());
	}

	@Test
	public void getSourcesForCreature_unknownCreature_returnsEmpty()
	{
		assertTrue(SlayerCreatureDatabase.getSourcesForCreature("chickens").isEmpty());
	}

	@Test
	public void getSourcesForCreature_singleMapping()
	{
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature("greater demons");
		assertEquals(1, sources.size());
		assertEquals("K'ril Tsutsaroth", sources.get(0));
	}

	@Test
	public void getSourcesForCreature_multipleMapping()
	{
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature("dagannoth");
		assertEquals(3, sources.size());
		assertTrue(sources.contains("Dagannoth Rex"));
		assertTrue(sources.contains("Dagannoth Prime"));
		assertTrue(sources.contains("Dagannoth Supreme"));
	}

	@Test
	public void getSourcesForCreature_caseInsensitive()
	{
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature("Black Demons");
		assertEquals(2, sources.size());
		assertTrue(sources.contains("Demonic gorillas"));
		assertTrue(sources.contains("Skotizo"));
	}

	@Test
	public void getSourcesForCreature_hydras()
	{
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature("hydras");
		assertEquals(2, sources.size());
		assertTrue(sources.contains("Hydra"));
		assertTrue(sources.contains("Alchemical Hydra"));
	}

	@Test
	public void getSourcesForCreature_caveKraken()
	{
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature("cave kraken");
		assertEquals(2, sources.size());
		assertTrue(sources.contains("Cave Kraken"));
		assertTrue(sources.contains("Kraken"));
	}

	// ========================================================================
	// isSourceOnTask
	// ========================================================================

	@Test
	public void isSourceOnTask_nullCreature()
	{
		assertFalse(SlayerCreatureDatabase.isSourceOnTask(null, "Cerberus"));
	}

	@Test
	public void isSourceOnTask_nullSource()
	{
		assertFalse(SlayerCreatureDatabase.isSourceOnTask("hellhounds", null));
	}

	@Test
	public void isSourceOnTask_matchingPair()
	{
		assertTrue(SlayerCreatureDatabase.isSourceOnTask("hellhounds", "Cerberus"));
	}

	@Test
	public void isSourceOnTask_nonMatchingPair()
	{
		assertFalse(SlayerCreatureDatabase.isSourceOnTask("hellhounds", "Kraken"));
	}

	@Test
	public void isSourceOnTask_caseInsensitiveCreature()
	{
		assertTrue(SlayerCreatureDatabase.isSourceOnTask("HELLHOUNDS", "Cerberus"));
	}

	@Test
	public void isSourceOnTask_sourceNameIsCaseSensitive()
	{
		assertFalse(SlayerCreatureDatabase.isSourceOnTask("hellhounds", "cerberus"));
	}

	// ========================================================================
	// getCreatureForSource — reverse lookup
	// ========================================================================

	@Test
	public void getCreatureForSource_null()
	{
		assertNull(SlayerCreatureDatabase.getCreatureForSource(null));
	}

	@Test
	public void getCreatureForSource_unknownSource()
	{
		assertNull(SlayerCreatureDatabase.getCreatureForSource("General Graardor"));
	}

	@Test
	public void getCreatureForSource_knownSource()
	{
		assertEquals("hellhounds", SlayerCreatureDatabase.getCreatureForSource("Cerberus"));
	}

	@Test
	public void getCreatureForSource_krakenBoss()
	{
		assertEquals("cave kraken", SlayerCreatureDatabase.getCreatureForSource("Kraken"));
	}

	@Test
	public void getCreatureForSource_demonicGorillas()
	{
		assertEquals("black demons", SlayerCreatureDatabase.getCreatureForSource("Demonic gorillas"));
	}

	// ========================================================================
	// isTaskOnlySource
	// ========================================================================

	@Test
	public void isTaskOnlySource_null()
	{
		assertFalse(SlayerCreatureDatabase.isTaskOnlySource(null));
	}

	@Test
	public void isTaskOnlySource_cerberus()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Cerberus"));
	}

	@Test
	public void isTaskOnlySource_kraken()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Kraken"));
	}

	@Test
	public void isTaskOnlySource_alchemicalHydra()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Alchemical Hydra"));
	}

	@Test
	public void isTaskOnlySource_grotesqueGuardians()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Grotesque Guardians"));
	}

	@Test
	public void isTaskOnlySource_metaOnlyDustDevil()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Dust Devil"));
	}

	@Test
	public void isTaskOnlySource_metaOnlyNechryael()
	{
		assertTrue(SlayerCreatureDatabase.isTaskOnlySource("Nechryael"));
	}

	@Test
	public void isTaskOnlySource_freelyFarmableGargoyle()
	{
		assertFalse(SlayerCreatureDatabase.isTaskOnlySource("Gargoyle"));
	}

	@Test
	public void isTaskOnlySource_nonSlayerBoss()
	{
		assertFalse(SlayerCreatureDatabase.isTaskOnlySource("General Graardor"));
	}

	// ========================================================================
	// Reverse lookup consistency
	// ========================================================================

	@Test
	public void reverseMapping_cerberusToHellhoundsAndBack()
	{
		String creature = SlayerCreatureDatabase.getCreatureForSource("Cerberus");
		assertNotNull(creature);
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature(creature);
		assertTrue(sources.contains("Cerberus"));
	}

	@Test
	public void reverseMapping_krakenToCaveKrakenAndBack()
	{
		String creature = SlayerCreatureDatabase.getCreatureForSource("Kraken");
		assertNotNull(creature);
		List<String> sources = SlayerCreatureDatabase.getSourcesForCreature(creature);
		assertTrue(sources.contains("Kraken"));
		assertTrue(sources.contains("Cave Kraken"));
	}
}
