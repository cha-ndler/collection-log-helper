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
import java.util.List;
import java.util.Map;
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

	// ========================================================================
	// GuidanceStep.recommendedItemIds — schema deserialization (B.5.2)
	// ========================================================================

	/**
	 * A GuidanceStep JSON with {@code recommendedItemIds} present must
	 * deserialise the list correctly.
	 */
	@Test
	public void guidanceStep_withRecommendedItemIds_deserializesCorrectly()
	{
		Gson gson = new GsonBuilder().create();
		String json =
			"{\"description\":\"Test step\","
			+ "\"completionCondition\":\"MANUAL\","
			+ "\"requiredItemIds\":[590],"
			+ "\"recommendedItemIds\":[4151,2440]}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull("Step must not be null", step);
		assertNotNull("requiredItemIds must be deserialised", step.getRequiredItemIds());
		assertEquals(1, step.getRequiredItemIds().size());
		assertEquals(Integer.valueOf(590), step.getRequiredItemIds().get(0));

		assertNotNull("recommendedItemIds must be deserialised", step.getRecommendedItemIds());
		assertEquals(2, step.getRecommendedItemIds().size());
		assertEquals(Integer.valueOf(4151), step.getRecommendedItemIds().get(0));
		assertEquals(Integer.valueOf(2440), step.getRecommendedItemIds().get(1));
	}

	/**
	 * A GuidanceStep JSON without {@code recommendedItemIds} (existing data)
	 * must deserialise with a null field — no NPE, backwards compatible.
	 */
	@Test
	public void guidanceStep_withoutRecommendedItemIds_nullField()
	{
		Gson gson = new GsonBuilder().create();
		String json =
			"{\"description\":\"Legacy step\","
			+ "\"completionCondition\":\"MANUAL\","
			+ "\"requiredItemIds\":[590]}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull("Step must not be null", step);
		assertNull("recommendedItemIds must be null when absent from JSON",
			step.getRecommendedItemIds());
	}

	// ========================================================================
	// Charset regression — issue #433
	// ========================================================================

	/**
	 * Verifies that em-dash characters in step descriptions are loaded correctly
	 * as the Unicode em-dash (U+2014, "—") rather than the Windows-1252 mojibake
	 * sequence "â€"" that results from reading UTF-8 bytes with the platform
	 * default charset on Windows.
	 *
	 * <p>The Perilous Moons step 3 description ("… Each has unique mechanics — learn
	 * the safe tiles …") is the known reproducer from issue #433.
	 */
	@Test
	public void load_guidanceStepDescriptions_noCharsetCorruption()
	{
		final String MOJIBAKE = "â€”"; // UTF-8 em-dash bytes misread as Windows-1252
		final String EM_DASH   = "—";              // correct em-dash

		boolean foundAtLeastOneEmDash = false;

		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				String desc = step.getDescription();
				if (desc != null)
				{
					assertFalse(
						"Mojibake em-dash detected in description for source '"
							+ source.getName() + "': " + desc,
						desc.contains(MOJIBAKE));
					if (desc.contains(EM_DASH))
					{
						foundAtLeastOneEmDash = true;
					}
				}

				if (step.getPerItemStepDescription() != null)
				{
					for (Map.Entry<Integer, String> entry : step.getPerItemStepDescription().entrySet())
					{
						String override = entry.getValue();
						if (override != null)
						{
							assertFalse(
								"Mojibake em-dash detected in perItemStepDescription (item "
									+ entry.getKey() + ") for source '" + source.getName()
									+ "': " + override,
								override.contains(MOJIBAKE));
						}
					}
				}
			}
		}

		assertTrue(
			"Expected at least one em-dash in guidance step descriptions — "
				+ "test data may be empty or step sources changed",
			foundAtLeastOneEmDash);
	}

	// ========================================================================
	// E2 — metaAuthoredDate regression: no production source should have it set
	// ========================================================================

	/**
	 * Regression guard: the E2 plumbing ships without a production data backfill.
	 * No source in drop_rates.json should carry a non-null metaAuthoredDate until
	 * the Tier D sweep intentionally populates the field.
	 */
	@Test
	public void load_noProductionSourceHasMetaAuthoredDate()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			assertNull(
				"Source '" + source.getName() + "' has metaAuthoredDate set; "
					+ "production backfill is not part of this PR",
				source.getMetaAuthoredDate());
		}
	}

	/**
	 * Where recommendedItemIds is present in drop_rates.json (B.5.2b backfill),
	 * each array must be non-empty, contain only positive item IDs, and have
	 * no more than 6 entries (per authoring spec).
	 */
	@Test
	public void load_recommendedItemIds_whenPresent_areValidAndBounded()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				List<Integer> recommended = step.getRecommendedItemIds();
				if (recommended == null)
				{
					continue; // null is fine — most steps have no recommendedItemIds
				}
				String ctx = source.getName() + " / " + step.getDescription();
				assertFalse("recommendedItemIds must not be empty: " + ctx, recommended.isEmpty());
				assertTrue("recommendedItemIds must have at most 6 entries: " + ctx,
					recommended.size() <= 6);
				for (int id : recommended)
				{
					assertTrue("recommendedItemIds must contain only positive IDs (got " + id + "): " + ctx,
						id > 0);
				}
			}
		}
	}

	// ========================================================================
	// Issue #306 Tier 3 backfill — CHAT_MESSAGE_RECEIVED upgrades
	// ========================================================================

	/**
	 * Brimhaven Agility Arena final step must use CHAT_MESSAGE_RECEIVED with the
	 * confirmed per-ticket award message (OSRS Wiki: Ticket_Dispenser).
	 */
	@Test
	public void spotCheck_brimhavenAgilityArena_finalStep_isChatMessageReceived()
	{
		CollectionLogSource source = database.getSourceByName("Brimhaven Agility Arena");
		assertNotNull("Brimhaven Agility Arena source must exist", source);

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull("Brimhaven Agility Arena must have guidance steps", steps);
		assertFalse("Brimhaven Agility Arena must have at least one step", steps.isEmpty());

		GuidanceStep finalStep = steps.get(steps.size() - 1);
		assertEquals(
			"Brimhaven Agility Arena final step must use CHAT_MESSAGE_RECEIVED",
			CompletionCondition.CHAT_MESSAGE_RECEIVED,
			finalStep.getCompletionCondition());
		assertNotNull(
			"Brimhaven Agility Arena final step must have a completionChatPattern",
			finalStep.getCompletionChatPattern());
		assertTrue(
			"Brimhaven Agility Arena chat pattern must match ticket award message",
			"You have received an Agility Arena Ticket and Brimhaven Voucher!"
				.contains(finalStep.getCompletionChatPattern()));
	}

	/**
	 * Pest Control final step must use CHAT_MESSAGE_RECEIVED with the confirmed
	 * game-win message (OSRS Wiki: Pest_Control).
	 */
	@Test
	public void spotCheck_pestControl_finalStep_isChatMessageReceived()
	{
		CollectionLogSource source = database.getSourceByName("Pest Control");
		assertNotNull("Pest Control source must exist", source);

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull("Pest Control must have guidance steps", steps);
		assertFalse("Pest Control must have at least one step", steps.isEmpty());

		GuidanceStep finalStep = steps.get(steps.size() - 1);
		assertEquals(
			"Pest Control final step must use CHAT_MESSAGE_RECEIVED",
			CompletionCondition.CHAT_MESSAGE_RECEIVED,
			finalStep.getCompletionCondition());
		assertNotNull(
			"Pest Control final step must have a completionChatPattern",
			finalStep.getCompletionChatPattern());
		assertEquals(
			"Pest Control chat pattern must match game-win message exactly",
			"You have successfully defended the island!",
			finalStep.getCompletionChatPattern());
	}

	/**
	 * Every CHAT_MESSAGE_RECEIVED step in the database must have a non-null,
	 * non-empty completionChatPattern (schema invariant).
	 */
	@Test
	public void load_allChatMessageReceivedSteps_haveNonEmptyPattern()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				if (step.getCompletionCondition() == CompletionCondition.CHAT_MESSAGE_RECEIVED)
				{
					String ctx = source.getName() + " / " + step.getDescription();
					assertNotNull(
						"CHAT_MESSAGE_RECEIVED step must have completionChatPattern: " + ctx,
						step.getCompletionChatPattern());
					assertFalse(
						"CHAT_MESSAGE_RECEIVED step must have non-empty completionChatPattern: " + ctx,
						step.getCompletionChatPattern().isEmpty());
				}
			}
		}
	}
}
