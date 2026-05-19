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
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
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
	// Charset regression - issue #433 / ASCII-only policy - issue #501
	// ========================================================================

	/**
	 * Guards against two related corruption modes in guidance step descriptions:
	 *
	 * <ul>
	 *   <li>Windows-1252 mojibake (e.g. UTF-8 em-dash bytes misread as cp1252)
	 *       that results from reading UTF-8 with the platform default charset
	 *       on Windows (issue #433).</li>
	 *   <li>Any non-ASCII codepoint in description text. Per issue #501,
	 *       drop_rates.json is ASCII-only; fancy punctuation (em-dash, smart
	 *       quotes, ellipsis, non-breaking space) must be replaced with ASCII
	 *       equivalents. The Gradle task {@code lintDropRates} enforces this on
	 *       the raw file; this test extends the guarantee to the loaded model so
	 *       it catches drift introduced via {@code perItemStepDescription}
	 *       overrides or future merge accidents.</li>
	 * </ul>
	 */
	@Test
	public void load_guidanceStepDescriptions_noCharsetCorruption()
	{
		// UTF-8 em-dash (0xE2 0x80 0x94) misread as cp1252 yields U+00E2 U+20AC U+201D.
		// We detect the leading two codepoints, which uniquely indicate cp1252 mojibake.
		final String MOJIBAKE_PREFIX = new String(new char[]{(char) 0x00E2, (char) 0x20AC});

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
						"Mojibake sequence detected in description for source '"
							+ source.getName() + "': " + desc,
						desc.contains(MOJIBAKE_PREFIX));
					assertAscii("description for source '" + source.getName() + "'", desc);
				}

				if (step.getPerItemStepDescription() != null)
				{
					for (Map.Entry<Integer, String> entry : step.getPerItemStepDescription().entrySet())
					{
						String override = entry.getValue();
						if (override != null)
						{
							assertFalse(
								"Mojibake sequence detected in perItemStepDescription (item "
									+ entry.getKey() + ") for source '" + source.getName()
									+ "': " + override,
								override.contains(MOJIBAKE_PREFIX));
							assertAscii(
								"perItemStepDescription (item " + entry.getKey()
									+ ") for source '" + source.getName() + "'",
								override);
						}
					}
				}
			}
		}
	}

	private static void assertAscii(String context, String value)
	{
		for (int i = 0; i < value.length(); i++)
		{
			int cp = value.codePointAt(i);
			if (cp > 0x7F)
			{
				fail("Non-ASCII codepoint U+" + String.format("%04X", cp)
					+ " at index " + i + " in " + context + ": " + value);
			}
		}
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

	// ========================================================================
	// GuidanceStep.perItemRecommendedItemIds — regression (B.5.2)
	// ========================================================================

	/**
	 * Regression: as of B.5.2 release, no production step in drop_rates.json uses
	 * {@code perItemRecommendedItemIds}.  This test enforces the invariant so a
	 * future data backfill that sets the field is explicit and visible in review.
	 *
	 * <p>If this test begins failing intentionally (a data author added the field),
	 * delete or update it with a comment referencing the PR that introduced the data.
	 */
	@Test
	public void regression_noProductionStepHasPerItemRecommendedItemIds()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				assertNull(
					"perItemRecommendedItemIds must not be set in production data yet "
						+ "(source: " + source.getName() + ")",
					step.getPerItemRecommendedItemIds());
			}
		}
	}

	/**
	 * Regression test for #548: the Custodian Stalker source's first guidance step
	 * (Travel to the Stalker Den entrance) must use ARRIVE_AT_ZONE with a zone that
	 * covers both the surface entrance area and the underground den interior.
	 *
	 * <p>The prior ARRIVE_AT_TILE with a 15-tile radius around (1324, 3364, 0) could
	 * be bypassed when the player's pathfinding-to-squeeze-through skipped the
	 * proximity window, leaving step 1/3 stuck and requiring manual Skip. Switching
	 * to a zone that includes both the surface approach (Y ~3360) and the
	 * underground den (Y ~9820) on plane 0 guarantees the step auto-advances once
	 * the player is anywhere meaningful inside the den region.
	 */
	@Test
	public void regression_548_custodianStalker_step1_usesArriveAtZone()
	{
		CollectionLogSource custodianStalker = null;
		for (CollectionLogSource source : database.getAllSources())
		{
			if ("Custodian Stalker".equals(source.getName()))
			{
				custodianStalker = source;
				break;
			}
		}
		assertNotNull("Custodian Stalker source must exist in drop_rates.json", custodianStalker);

		List<GuidanceStep> steps = custodianStalker.getGuidanceSteps();
		assertNotNull("Custodian Stalker must have guidance steps", steps);
		assertTrue("Custodian Stalker must have at least one guidance step", !steps.isEmpty());

		GuidanceStep travelStep = steps.get(0);
		assertEquals(
			"Step 1 must use ARRIVE_AT_ZONE so arrival auto-advances via either surface "
				+ "or underground (issue #548)",
			CompletionCondition.ARRIVE_AT_ZONE, travelStep.getCompletionCondition());

		Zone zone = travelStep.getZone();
		assertNotNull("Step 1 must declare a completionZone for ARRIVE_AT_ZONE", zone);
		assertEquals("Zone must be on plane 0", 0, zone.getPlane());

		// Zone must contain the documented surface entrance object location.
		assertTrue("Zone must contain the surface entrance at (1324, 3364, 0)",
			zone.contains(new net.runelite.api.coords.WorldPoint(1324, 3364, 0)));

		// Zone must also contain the underground den interior so a player who
		// descends without crossing the surface radius still auto-advances.
		assertTrue("Zone must contain the underground den interior at (1301, 9820, 0)",
			zone.contains(new net.runelite.api.coords.WorldPoint(1301, 9820, 0)));

		// Wrong plane must not match.
		assertFalse("Zone must not match plane 1",
			zone.contains(new net.runelite.api.coords.WorldPoint(1324, 3364, 1)));
	}

	/**
	 * Regression test for #555: the 23 sources migrated from ARRIVE_AT_TILE to
	 * ARRIVE_AT_ZONE in the batch follow-up to #553 must all have a step 0 that:
	 *   1. Uses ARRIVE_AT_ZONE (not ARRIVE_AT_TILE).
	 *   2. Declares a zone on plane 0 covering both a representative surface coord
	 *      AND a representative underground coord for that source.
	 *   3. Rejects plane 1+ (so the auto-advance doesn't fire on the floor above).
	 *
	 * <p>Modelled on the #548 Custodian Stalker regression test above. Each case
	 * carries the source name, expected zone array, one surface coord that must be
	 * inside the zone, and one underground coord that must also be inside. Coords
	 * are sourced from the issue #555 audit table.
	 */
	@Test
	public void regression_555_batchMigratedSources_useArriveAtZone()
	{
		Object[][] cases = new Object[][]
		{
			// { name, expectedZone, surfaceCoord, undergroundCoord }
			{"Sarachnis",                  new int[]{1690, 3560, 1860, 9920, 0},  new WorldPoint(1701, 3574, 0),  new WorldPoint(1847, 9910, 0)},
			{"Kraken",                     new int[]{2260, 3600, 2290, 10030, 0}, new WorldPoint(2278, 3611, 0),  new WorldPoint(2272, 10016, 0)},
			{"Thermonuclear smoke devil",  new int[]{2370, 3050, 2420, 9460, 0},  new WorldPoint(2412, 3061, 0),  new WorldPoint(2379, 9452, 0)},
			{"Alchemical Hydra",           new int[]{1300, 3795, 1380, 10280, 0}, new WorldPoint(1311, 3807, 0),  new WorldPoint(1364, 10265, 0)},
			{"Wyrm",                       new int[]{1300, 3795, 1330, 10220, 0}, new WorldPoint(1311, 3807, 0),  new WorldPoint(1311, 10205, 0)},
			{"Drake",                      new int[]{1300, 3795, 1330, 10075, 0}, new WorldPoint(1311, 3807, 0),  new WorldPoint(1310, 10057, 0)},
			{"Hydra",                      new int[]{1300, 3795, 1330, 10205, 0}, new WorldPoint(1311, 3807, 0),  new WorldPoint(1310, 10190, 0)},
			{"Cave Crawler",               new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2788, 10000, 0)},
			{"Rockslug",                   new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2794, 10018, 0)},
			{"Cockatrice",                 new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2792, 10035, 0)},
			{"Pyrefiend",                  new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2761, 10008, 0)},
			{"Basilisk",                   new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2744, 10008, 0)},
			{"Jelly",                      new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2704, 10028, 0)},
			{"Turoth",                     new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2722, 10002, 0)},
			{"Kurask",                     new int[]{2690, 3600, 2810, 10050, 0}, new WorldPoint(2797, 3614, 0),  new WorldPoint(2701, 9992, 0)},
			{"Aquanite",                   new int[]{2210, 3465, 2290, 9895, 0},  new WorldPoint(2218, 3477, 0),  new WorldPoint(2275, 9880, 0)},
			{"Fossil Island Wyvern",       new int[]{3590, 3765, 3760, 10305, 0}, new WorldPoint(3745, 3777, 0),  new WorldPoint(3602, 10290, 0)},
			{"Catacombs of Kourend",       new int[]{1620, 3660, 1690, 10070, 0}, new WorldPoint(1636, 3673, 0),  new WorldPoint(1664, 10050, 0)},
			{"Kalphite Queen",             new int[]{3215, 3095, 3520, 9520, 0},  new WorldPoint(3227, 3108, 0),  new WorldPoint(3471, 9506, 0)},
			{"Scorpia",                    new int[]{3215, 3920, 3250, 10355, 0}, new WorldPoint(3231, 3936, 0),  new WorldPoint(3233, 10341, 0)},
			{"Perilous Moons",             new int[]{1420, 3115, 1450, 9720, 0},  new WorldPoint(1435, 3131, 0),  new WorldPoint(1435, 9700, 0)},
			{"Giants' Foundry",            new int[]{3345, 3140, 3380, 11500, 0}, new WorldPoint(3360, 3151, 0),  new WorldPoint(3365, 11489, 0)},
			{"King Black Dragon",          new int[]{3015, 3830, 3080, 10265, 0}, new WorldPoint(3028, 3842, 0),  new WorldPoint(3067, 10253, 0)},
		};

		for (Object[] tc : cases)
		{
			String name = (String) tc[0];
			int[] expectedZone = (int[]) tc[1];
			WorldPoint surface = (WorldPoint) tc[2];
			WorldPoint underground = (WorldPoint) tc[3];

			CollectionLogSource source = null;
			for (CollectionLogSource s : database.getAllSources())
			{
				if (name.equals(s.getName()))
				{
					source = s;
					break;
				}
			}
			assertNotNull("Source must exist in drop_rates.json: " + name, source);

			List<GuidanceStep> steps = source.getGuidanceSteps();
			assertNotNull("Source must have guidance steps: " + name, steps);
			assertTrue("Source must have at least one guidance step: " + name, !steps.isEmpty());

			GuidanceStep travelStep = steps.get(0);
			assertEquals(
				"Step 0 must use ARRIVE_AT_ZONE (issue #555): " + name,
				CompletionCondition.ARRIVE_AT_ZONE, travelStep.getCompletionCondition());

			Zone zone = travelStep.getZone();
			assertNotNull("Step 0 must declare a completionZone: " + name, zone);
			assertEquals("Zone must be on plane 0: " + name, 0, zone.getPlane());

			// Surface entrance must be inside the zone (auto-advance must fire on arrival
			// at the surface entrance, matching the prior ARRIVE_AT_TILE behaviour).
			assertTrue("Zone must contain surface coord " + surface + " for " + name,
				zone.contains(surface));

			// Underground destination must also be inside the zone (auto-advance must fire
			// even when the player descends without crossing the surface radius).
			assertTrue("Zone must contain underground coord " + underground + " for " + name,
				zone.contains(underground));

			// Plane 1+ must never match — prevents false-positives on the floor above.
			assertFalse("Zone must not match plane 1 at surface for " + name,
				zone.contains(new WorldPoint(surface.getX(), surface.getY(), 1)));
			assertFalse("Zone must not match plane 2 at surface for " + name,
				zone.contains(new WorldPoint(surface.getX(), surface.getY(), 2)));
		}
	}
}