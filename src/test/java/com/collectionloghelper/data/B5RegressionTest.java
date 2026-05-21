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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B5 regression guard — asserts that no entry in the production
 * {@code drop_rates.json} has a non-null
 * {@link GuidanceStep#getDynamicTargetEvaluator()} field.
 *
 * <p>The {@code dynamicTargetEvaluator} field is reserved for puzzle/dynamic
 * content authored specifically against a registered evaluator.  Production
 * sources must not set it until a deliberate B5 data PR introduces Wintertodt
 * (or other puzzle) steps.  This guard fires if such a field appears in the
 * data before the corresponding evaluator plumbing is reviewed.
 *
 * <p>Pattern mirrors the regression guards used in B3 (nestedAlternatives),
 * B4.1 (perItemRequiredItemIds), and B4.3.0b (perItemNpcId).
 */
public class B5RegressionTest
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

	@Test
	public void noProductionStepHasDynamicTargetEvaluatorSet()
	{
		List<String> violations = new ArrayList<>();

		for (CollectionLogSource source : database.getAllSources())
		{
			if (source.getGuidanceSteps() == null)
			{
				continue;
			}
			for (int i = 0; i < source.getGuidanceSteps().size(); i++)
			{
				GuidanceStep step = source.getGuidanceSteps().get(i);
				if (step.getDynamicTargetEvaluator() != null)
				{
					violations.add(source.getName() + " step " + (i + 1)
						+ ": dynamicTargetEvaluator=\"" + step.getDynamicTargetEvaluator() + "\"");
				}
			}
		}

		assertTrue(
			violations.isEmpty(),
			"No production step should carry dynamicTargetEvaluator — this field is reserved "
				+ "for deliberate B5 puzzle/dynamic data PRs. Violations:\n"
				+ String.join("\n", violations));
	}
}
