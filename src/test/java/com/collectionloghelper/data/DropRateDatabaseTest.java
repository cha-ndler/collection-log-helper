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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DropRateDatabaseTest
{
	private DropRateDatabase database;

	@Before
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();

		// Inject Gson via reflection (normally handled by Guice @Inject)
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);

		database.load();
	}

	// ========================================================================
	// load() — basic integrity of the real drop_rates.json
	// ========================================================================

	@Test
	public void load_populatesSources()
	{
		assertFalse(database.getAllSources().isEmpty());
		assertTrue("Expected at least 200 sources", database.getAllSources().size() >= 200);
	}

	@Test
	public void load_allSourcesHaveNames()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			assertNotNull("Source has null name", source.getName());
			assertFalse("Source has empty name", source.getName().isEmpty());
		}
	}

	@Test
	public void load_allSourcesHaveItems()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			assertNotNull("Source '" + source.getName() + "' has null items", source.getItems());
			assertFalse("Source '" + source.getName() + "' has no items", source.getItems().isEmpty());
		}
	}

	@Test
	public void load_allItemsHavePositiveDropRateOrPointCostOrMilestone()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				boolean hasDropRate = item.getDropRate() > 0;
				boolean hasPointCost = item.getPointCost() > 0;
				boolean hasMilestone = item.getMilestoneKills() > 0;
				assertTrue(
					"Item '" + item.getName() + "' in source '" + source.getName()
						+ "' has no drop rate, point cost, or milestone",
					hasDropRate || hasPointCost || hasMilestone);
			}
		}
	}

	@Test
	public void load_noDropRatesExceedOne()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				assertTrue(
					"Item '" + item.getName() + "' in '" + source.getName()
						+ "' has dropRate " + item.getDropRate() + " > 1.0",
					item.getDropRate() <= 1.0);
			}
		}
	}

	@Test
	public void load_allSourcesHaveCategory()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			assertNotNull("Source '" + source.getName() + "' has null category", source.getCategory());
		}
	}

	// ========================================================================
	// getSourceByName — lookup
	// ========================================================================

	@Test
	public void getSourceByName_existingSource()
	{
		CollectionLogSource source = database.getSourceByName("General Graardor");
		assertNotNull(source);
		assertEquals("General Graardor", source.getName());
	}

	@Test
	public void getSourceByName_caseInsensitive()
	{
		CollectionLogSource source = database.getSourceByName("general graardor");
		assertNotNull(source);
		assertEquals("General Graardor", source.getName());
	}

	@Test
	public void getSourceByName_nonExistent()
	{
		assertNull(database.getSourceByName("Nonexistent Boss"));
	}

	// ========================================================================
	// getItemById — lookup
	// ========================================================================

	@Test
	public void getItemById_existingItem()
	{
		CollectionLogSource source = database.getAllSources().get(0);
		int firstItemId = source.getItems().get(0).getItemId();

		CollectionLogItem item = database.getItemById(firstItemId);
		assertNotNull(item);
		assertEquals(firstItemId, item.getItemId());
	}

	@Test
	public void getItemById_nonExistent()
	{
		assertNull(database.getItemById(999999));
	}

	// ========================================================================
	// getItemByName — lookup
	// ========================================================================

	@Test
	public void getItemByName_existingItem()
	{
		CollectionLogSource graardor = database.getSourceByName("General Graardor");
		assertNotNull(graardor);
		String firstName = graardor.getItems().get(0).getName();

		CollectionLogItem item = database.getItemByName(firstName);
		assertNotNull(item);
		assertEquals(firstName, item.getName());
	}

	@Test
	public void getItemByName_caseInsensitive()
	{
		CollectionLogSource graardor = database.getSourceByName("General Graardor");
		assertNotNull(graardor);
		String firstName = graardor.getItems().get(0).getName();

		CollectionLogItem item = database.getItemByName(firstName.toUpperCase());
		assertNotNull(item);
	}

	@Test
	public void getItemByName_nonExistent()
	{
		assertNull(database.getItemByName("Totally Fake Item"));
	}

	// ========================================================================
	// getSourceByNpcId — lookup
	// ========================================================================

	@Test
	public void getSourceByNpcId_existingNpc()
	{
		CollectionLogSource sourceWithNpc = null;
		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getNpcId() > 0)
			{
				sourceWithNpc = source;
				break;
			}
		}
		assertNotNull("No source with NPC ID found", sourceWithNpc);

		CollectionLogSource found = database.getSourceByNpcId(sourceWithNpc.getNpcId());
		assertNotNull(found);
		assertEquals(sourceWithNpc.getName(), found.getName());
	}

	@Test
	public void getSourceByNpcId_nonExistent()
	{
		assertNull(database.getSourceByNpcId(999999));
	}

	// ========================================================================
	// getSourcesByCategory — lookup
	// ========================================================================

	@Test
	public void getSourcesByCategory_bosses_notEmpty()
	{
		List<CollectionLogSource> bosses = database.getSourcesByCategory(CollectionLogCategory.BOSSES);
		assertFalse(bosses.isEmpty());
	}

	@Test
	public void getSourcesByCategory_allCategoriesPresent()
	{
		for (CollectionLogCategory cat : CollectionLogCategory.values())
		{
			List<CollectionLogSource> sources = database.getSourcesByCategory(cat);
			assertFalse("Category " + cat + " has no sources", sources.isEmpty());
		}
	}

	// ========================================================================
	// getAllSources — immutability
	// ========================================================================

	@Test(expected = UnsupportedOperationException.class)
	public void getAllSources_returnsUnmodifiableList()
	{
		database.getAllSources().add(null);
	}

	// ========================================================================
	// Data consistency
	// ========================================================================

	@Test
	public void allItemIds_foundInIndex()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				CollectionLogItem found = database.getItemById(item.getItemId());
				assertNotNull("Item ID " + item.getItemId() + " not found in index", found);
			}
		}
	}

	// ========================================================================
	// Specific known sources — spot checks
	// ========================================================================

	@Test
	public void spotCheck_generalGraardor_isBossCategory()
	{
		CollectionLogSource source = database.getSourceByName("General Graardor");
		assertNotNull(source);
		assertEquals(CollectionLogCategory.BOSSES, source.getCategory());
	}

	@Test
	public void spotCheck_generalGraardor_hasPositiveKillTime()
	{
		CollectionLogSource source = database.getSourceByName("General Graardor");
		assertNotNull(source);
		assertTrue(source.getKillTimeSeconds() > 0);
	}
}
