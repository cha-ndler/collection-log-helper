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
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the {@code activityObtainableItemIds} field added to
 * {@link GuidanceStep} in the Phase 2 guidance-items redesign.
 *
 * <p>Acceptance criteria:
 * <ol>
 *   <li>JSON without the field deserialises so the raw field is null and the
 *       non-null accessor returns an empty list (backwards-compatible for the
 *       ~225 existing sources).</li>
 *   <li>JSON with the field deserialises to the declared list.</li>
 *   <li>The "Shades of Mort'ton" pilot: its burn step carries the on-site
 *       Fiyr remains (3404) in {@code activityObtainableItemIds}, and its
 *       recommended list no longer contains the materials/on-site items it
 *       previously listed (Tinderbox 590, pyre logs 3446, remains 3404).</li>
 * </ol>
 */
public class ActivityObtainableItemsTest
{
	/** Fiyr remains - shade remains obtained on-site by killing shades during the activity. */
	private static final int FIYR_REMAINS = 3404;
	/** Tinderbox - a brought material, not a task aid. */
	private static final int TINDERBOX = 590;
	/** Yew pyre logs - a brought material, not a task aid. */
	private static final int YEW_PYRE_LOGS = 3446;
	/** Flamtaer bag - the single convenience aid kept under Recommended. */
	private static final int FLAMTAER_BAG = 25630;

	private static List<CollectionLogSource> sources;

	@BeforeAll
	public static void loadModel() throws Exception
	{
		Gson gson = new GsonBuilder().create();
		Type listType = new TypeToken<List<CollectionLogSource>>() {}.getType();
		try (InputStream is = ActivityObtainableItemsTest.class
			.getResourceAsStream("/com/collectionloghelper/drop_rates.json"))
		{
			assertNotNull(is, "drop_rates.json must be on the classpath");
			sources = gson.fromJson(new InputStreamReader(is), listType);
		}
		assertNotNull(sources, "drop_rates.json must parse to a source list");
	}

	@Test
	public void jsonWithoutField_rawNull_accessorEmpty()
	{
		Gson gson = new GsonBuilder().create();
		GuidanceStep step = gson.fromJson("{\"description\":\"x\"}", GuidanceStep.class);
		assertNotNull(step.getActivityObtainableItemIds(),
			"accessor must never return null");
		assertTrue(step.getActivityObtainableItemIds().isEmpty(),
			"accessor must return an empty list when the field is absent");
	}

	@Test
	public void jsonWithField_deserialisesToList()
	{
		Gson gson = new GsonBuilder().create();
		GuidanceStep step = gson.fromJson(
			"{\"description\":\"x\",\"activityObtainableItemIds\":[3404,3450]}",
			GuidanceStep.class);
		assertEquals(2, step.getActivityObtainableItemIds().size());
		assertTrue(step.getActivityObtainableItemIds().contains(FIYR_REMAINS));
		assertTrue(step.getActivityObtainableItemIds().contains(3450));
	}

	@Test
	public void mostSources_haveNoActivityObtainableField()
	{
		// The field is a narrow, opt-in pilot: only Shades of Mort'ton uses it.
		// Every OTHER source must leave it null so the existing data is untouched.
		for (CollectionLogSource src : sources)
		{
			if ("Shades of Mort'ton".equals(src.getName()) || src.getGuidanceSteps() == null)
			{
				continue;
			}
			for (GuidanceStep step : src.getGuidanceSteps())
			{
				assertTrue(step.getActivityObtainableItemIds().isEmpty(),
					"non-pilot source '" + src.getName()
						+ "' must not set activityObtainableItemIds");
			}
		}
	}

	@Test
	public void shadesPilot_burnStep_tagsFiyrRemainsFromActivity()
	{
		GuidanceStep burnStep = shadesBurnStep();
		assertTrue(burnStep.getActivityObtainableItemIds().contains(FIYR_REMAINS),
			"Shades burn step must tag Fiyr remains as obtained on-site");
	}

	@Test
	public void shadesPilot_recommended_isAidsOnly()
	{
		GuidanceStep burnStep = shadesBurnStep();
		List<Integer> recommended = burnStep.getRecommendedItemIds();
		assertNotNull(recommended, "Shades burn step keeps a recommended list");
		assertFalse(recommended.contains(TINDERBOX),
			"Tinderbox is a brought material, not a recommended aid");
		assertFalse(recommended.contains(YEW_PYRE_LOGS),
			"pyre logs are a brought material, not a recommended aid");
		assertFalse(recommended.contains(FIYR_REMAINS),
			"shade remains are obtained on-site, not a recommended aid");
		assertTrue(recommended.contains(FLAMTAER_BAG),
			"the Flamtaer bag is the one convenience aid kept under Recommended");
	}

	/** Locates the Shades "burn shade remains" step (the only step with the activity field). */
	private static GuidanceStep shadesBurnStep()
	{
		CollectionLogSource shades = sources.stream()
			.filter(s -> "Shades of Mort'ton".equals(s.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Shades of Mort'ton source missing"));
		assertNotNull(shades.getGuidanceSteps(), "Shades must have guidance steps");
		return shades.getGuidanceSteps().stream()
			.filter(s -> !s.getActivityObtainableItemIds().isEmpty())
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"Shades must have a step with activityObtainableItemIds"));
	}
}
