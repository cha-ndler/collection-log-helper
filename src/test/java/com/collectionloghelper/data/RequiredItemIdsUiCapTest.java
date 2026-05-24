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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the requiredItemIds UI rendering cap.
 *
 * <p>Tracks the audit in
 * {@code docs/contributor-guide/required-item-ids-ui-cap-audit.md}: the
 * side-panel {@code RequiredItemsChipPanel} renders chips in a single
 * non-wrapping {@code BoxLayout.X_AXIS} row inside a 225px RuneLite
 * {@code PluginPanel}. After the "Items needed:" heading, roughly 5 chips
 * fit before the row clips on the right edge. Six or more entries are a
 * confirmed visual overflow.
 *
 * <p>This test mirrors
 * {@link DropRateDatabaseTest#load_recommendedItemIds_whenPresent_areValidAndBounded}
 * and asserts {@code requiredItemIds.size() <= 5} on every step in
 * {@code drop_rates.json}. Five chips is the visual sweet-spot the audit
 * recommends; it is stricter than the {@code recommendedItemIds <= 6}
 * discipline because the chip row is the only authoring surface that
 * actually clips. PR #654 trimmed the worst three offenders and seeded
 * this guard at threshold 6; this PR trims the remaining seven (Drake,
 * Guardians of the Rift, Hydra, Mahogany Homes, Skeletal Wyvern, The Fight
 * Caves, The Inferno) and tightens the cap to 5.
 *
 * <p>Trim least-essential items (or move them to
 * {@code recommendedItemIds} when its own {@code <= 6} cap permits) when this
 * test fails on a new authoring PR.
 */
public class RequiredItemIdsUiCapTest
{
	/** Maximum entries per {@code requiredItemIds} list before chip overflow. */
	private static final int MAX_REQUIRED_ITEM_IDS = 5;

	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();

		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);

		database.load();
	}

	@Test
	public void load_requiredItemIds_areBoundedByUiCap()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : source.getGuidanceSteps())
			{
				List<Integer> required = step.getRequiredItemIds();
				if (required == null || required.isEmpty())
				{
					continue;
				}
				String ctx = source.getName() + " / " + step.getDescription();
				assertTrue(
					required.size() <= MAX_REQUIRED_ITEM_IDS,
					"requiredItemIds must have at most " + MAX_REQUIRED_ITEM_IDS
						+ " entries (UI chip-row visual cap; see "
						+ "required-item-ids-ui-cap-audit.md); got "
						+ required.size() + " for: " + ctx);
			}
		}
	}
}
