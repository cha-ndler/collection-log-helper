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

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * B3 regression guard — asserts that no entry in the production {@code drop_rates.json}
 * uses the new {@link ConditionalAlternative#getNestedAlternatives()} field.
 *
 * <p>This test must stay green until a deliberate B3 data PR explicitly migrates a source
 * to use nested alternatives. When that happens, update this guard to reflect the
 * intentional change (either adjust the assertion scope or remove this test and replace it
 * with a per-source contract test).
 *
 * <p>The pattern mirrors the regression guards used in B4.1 (perItemRequiredItemIds) and
 * B4.3.0b (perItemNpcId): load real production data, walk the tree, assert the new field
 * is null everywhere.
 */
public class B3RegressionTest
{
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

	/**
	 * Walks every ConditionalAlternative in every guidance step of every production source
	 * and asserts that {@code nestedAlternatives} is null.
	 *
	 * <p>A non-null value would mean JSON was authored with the B3 shape before any source
	 * was validated for nested branching.
	 */
	@Test
	public void productionData_noConditionalAlternativeUsesNestedAlternatives()
	{
		int checkedAlternatives = 0;

		for (CollectionLogSource source : database.getAllSources())
		{
			List<GuidanceStep> steps = source.getGuidanceSteps();
			if (steps == null)
			{
				continue;
			}
			for (GuidanceStep step : steps)
			{
				List<ConditionalAlternative> alternatives = step.getConditionalAlternatives();
				if (alternatives == null)
				{
					continue;
				}
				for (ConditionalAlternative alt : alternatives)
				{
					checkedAlternatives++;
					assertNull(
						alt.getNestedAlternatives()
					,
						"Production source '" + source.getName() + "' has a ConditionalAlternative"
							+ " with nestedAlternatives set — B3 shape is not yet permitted in"
							+ " production data. Remove nestedAlternatives from this entry or"
							+ " update this regression guard after explicit B3 validation.");
				}
			}
		}

		System.out.println("[B3RegressionTest] Checked " + checkedAlternatives
			+ " ConditionalAlternative(s) across all production sources — all nestedAlternatives null.");
	}
}
