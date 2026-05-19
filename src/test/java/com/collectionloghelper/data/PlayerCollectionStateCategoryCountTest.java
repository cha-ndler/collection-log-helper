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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Regression coverage for issue #545 — SLAYER and SKILLING categories must
 * derive their count/max from the source database because OSRS does not
 * expose aggregate varps for those categories.
 *
 * <p>The enum-walk guard at the bottom is the long-term signal: if a future
 * PR adds a new {@link CollectionLogCategory} value without extending
 * {@code getCategoryCount} / {@code getCategoryMax}, this suite fails at
 * CI time instead of in a live client months later.
 */
public class PlayerCollectionStateCategoryCountTest
{
	private DropRateDatabase database;
	private PlayerCollectionState state;

	@Before
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);
		database.load();

		Client client = mock(Client.class);
		ConfigManager configManager = mock(ConfigManager.class);

		// Constructor is private — reflect to invoke. Mirrors the reflection pattern in DropRateDatabaseTest.
		Constructor<PlayerCollectionState> ctor = PlayerCollectionState.class.getDeclaredConstructor(
			Client.class, ConfigManager.class, DropRateDatabase.class);
		ctor.setAccessible(true);
		state = ctor.newInstance(client, configManager, database);
	}

	private Set<Integer> collectCategoryItemIds(CollectionLogCategory category)
	{
		Set<Integer> ids = new HashSet<>();
		List<CollectionLogSource> sources = database.getSourcesByCategory(category);
		for (CollectionLogSource source : sources)
		{
			for (CollectionLogItem item : source.getItems())
			{
				ids.add(item.getItemId());
			}
		}
		return ids;
	}

	@SuppressWarnings("unchecked")
	private void seedObtained(Set<Integer> itemIds) throws Exception
	{
		Field field = PlayerCollectionState.class.getDeclaredField("obtainedItemIds");
		field.setAccessible(true);
		Set<Integer> obtained = (Set<Integer>) field.get(state);
		obtained.clear();
		obtained.addAll(itemIds);
	}

	private void setVarpField(String fieldName, int value) throws Exception
	{
		Field f = PlayerCollectionState.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		f.setInt(state, value);
	}

	// ========================================================================
	// SLAYER coverage
	// ========================================================================

	@Test
	public void getCategoryCount_slayer_returnsAggregateOfObtainedSlayerItems() throws Exception
	{
		Set<Integer> slayerItemIds = collectCategoryItemIds(CollectionLogCategory.SLAYER);
		assertTrue("Slayer category should have items in the source DB", slayerItemIds.size() > 0);

		// Seed a deterministic subset.
		Set<Integer> seeded = new HashSet<>();
		int target = Math.min(5, slayerItemIds.size());
		int i = 0;
		for (Integer id : slayerItemIds)
		{
			if (i++ >= target)
			{
				break;
			}
			seeded.add(id);
		}
		seedObtained(seeded);

		int count = state.getCategoryCount(CollectionLogCategory.SLAYER);
		assertEquals("SLAYER count should equal the deduplicated obtained slayer item count",
			seeded.size(), count);
	}

	@Test
	public void getCategoryMax_slayer_returnsTotalSlayerItemCount()
	{
		Set<Integer> slayerItemIds = collectCategoryItemIds(CollectionLogCategory.SLAYER);
		int max = state.getCategoryMax(CollectionLogCategory.SLAYER);
		assertEquals("SLAYER max should equal deduplicated total of slayer item IDs",
			slayerItemIds.size(), max);
		assertTrue("SLAYER max should be > 0 (fix for #545)", max > 0);
	}

	@Test
	public void getCategoryCount_slayer_deduplicatesItemsAcrossSources() throws Exception
	{
		// If an item appears in multiple slayer sources, it must still count once.
		Set<Integer> slayerItemIds = collectCategoryItemIds(CollectionLogCategory.SLAYER);
		seedObtained(slayerItemIds);
		int count = state.getCategoryCount(CollectionLogCategory.SLAYER);
		assertEquals("Count with all slayer items obtained must equal deduplicated max",
			slayerItemIds.size(), count);
	}

	// ========================================================================
	// SKILLING coverage
	// ========================================================================

	@Test
	public void getCategoryCount_skilling_returnsAggregateOfObtainedSkillingItems() throws Exception
	{
		Set<Integer> skillingItemIds = collectCategoryItemIds(CollectionLogCategory.SKILLING);
		assertTrue("Skilling category should have items in the source DB", skillingItemIds.size() > 0);

		Set<Integer> seeded = new HashSet<>();
		int target = Math.min(3, skillingItemIds.size());
		int i = 0;
		for (Integer id : skillingItemIds)
		{
			if (i++ >= target)
			{
				break;
			}
			seeded.add(id);
		}
		seedObtained(seeded);

		int count = state.getCategoryCount(CollectionLogCategory.SKILLING);
		assertEquals("SKILLING count should equal deduplicated obtained skilling items",
			seeded.size(), count);
	}

	@Test
	public void getCategoryMax_skilling_returnsTotalSkillingItemCount()
	{
		Set<Integer> skillingItemIds = collectCategoryItemIds(CollectionLogCategory.SKILLING);
		int max = state.getCategoryMax(CollectionLogCategory.SKILLING);
		assertEquals("SKILLING max should equal deduplicated total of skilling item IDs",
			skillingItemIds.size(), max);
		assertTrue("SKILLING max should be > 0 (fix for #545)", max > 0);
	}

	// ========================================================================
	// Varp-backed categories — regression guard
	// ========================================================================

	@Test
	public void getCategoryCount_bosses_stillUsesVarpValue() throws Exception
	{
		setVarpField("bossesCount", 42);
		setVarpField("bossesMax", 333);
		// Seed an arbitrary item — the varp branch must ignore it for BOSSES.
		Set<Integer> seeded = new HashSet<>();
		seeded.add(99999);
		seedObtained(seeded);

		assertEquals("BOSSES count must come from the bossesCount varp field, not source aggregation",
			42, state.getCategoryCount(CollectionLogCategory.BOSSES));
		assertEquals("BOSSES max must come from the bossesMax varp field, not source aggregation",
			333, state.getCategoryMax(CollectionLogCategory.BOSSES));
	}

	@Test
	public void getCategoryCount_raids_stillUsesVarpValue() throws Exception
	{
		setVarpField("raidsCount", 7);
		setVarpField("raidsMax", 50);
		assertEquals(7, state.getCategoryCount(CollectionLogCategory.RAIDS));
		assertEquals(50, state.getCategoryMax(CollectionLogCategory.RAIDS));
	}

	@Test
	public void getCategoryCount_clues_stillUsesVarpValue() throws Exception
	{
		setVarpField("cluesCount", 12);
		setVarpField("cluesMax", 100);
		assertEquals(12, state.getCategoryCount(CollectionLogCategory.CLUES));
		assertEquals(100, state.getCategoryMax(CollectionLogCategory.CLUES));
	}

	@Test
	public void getCategoryCount_minigames_stillUsesVarpValue() throws Exception
	{
		setVarpField("minigamesCount", 3);
		setVarpField("minigamesMax", 25);
		assertEquals(3, state.getCategoryCount(CollectionLogCategory.MINIGAMES));
		assertEquals(25, state.getCategoryMax(CollectionLogCategory.MINIGAMES));
	}

	@Test
	public void getCategoryCount_other_stillUsesVarpValue() throws Exception
	{
		setVarpField("otherCount", 9);
		setVarpField("otherMax", 80);
		assertEquals(9, state.getCategoryCount(CollectionLogCategory.OTHER));
		assertEquals(80, state.getCategoryMax(CollectionLogCategory.OTHER));
	}

	// ========================================================================
	// Enum-walk regression guard — THE long-term signal.
	//
	// If a future PR adds a new CollectionLogCategory value without extending
	// PlayerCollectionState.getCategoryCount / getCategoryMax, this test fails
	// at CI time, not in a live client months later.
	// ========================================================================

	@Test
	public void allCategories_returnNonNegativeCountAndMax() throws Exception
	{
		// Set non-zero varps so varp-backed categories don't accidentally pass with 0/0.
		setVarpField("bossesCount", 1);
		setVarpField("bossesMax", 1);
		setVarpField("raidsCount", 1);
		setVarpField("raidsMax", 1);
		setVarpField("cluesCount", 1);
		setVarpField("cluesMax", 1);
		setVarpField("minigamesCount", 1);
		setVarpField("minigamesMax", 1);
		setVarpField("otherCount", 1);
		setVarpField("otherMax", 1);

		for (CollectionLogCategory category : CollectionLogCategory.values())
		{
			int count = state.getCategoryCount(category);
			int max = state.getCategoryMax(category);

			assertTrue("Category " + category + " returned negative count: " + count, count >= 0);
			assertTrue("Category " + category + " returned negative max: " + max, max >= 0);
			assertTrue(
				"Category " + category + " has max < count (max=" + max + ", count=" + count + ")",
				max >= count);
			assertTrue(
				"Category " + category + " has 0 max — accessor likely missing an enum case "
					+ "(see issue #545; extend the switch in PlayerCollectionState).",
				max > 0);
		}
	}

	@Test
	public void allCategories_sourceDatabaseHasAtLeastOneItem()
	{
		// Sanity check: every category in the enum must have at least one
		// source/item in drop_rates.json. If not, the source DB itself is the
		// issue, not the accessor.
		for (CollectionLogCategory category : CollectionLogCategory.values())
		{
			List<CollectionLogSource> sources = database.getSourcesByCategory(category);
			assertNotNull("Category " + category + " returned null source list", sources);
			assertTrue("Category " + category + " has no sources in drop_rates.json",
				!sources.isEmpty());
		}
	}
}
