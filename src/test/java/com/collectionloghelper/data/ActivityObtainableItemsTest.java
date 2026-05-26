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

	/** Brimhaven Agility Arena Graceful recolour pieces - earned on-site via tickets. */
	private static final List<Integer> BRIMHAVEN_GRACEFUL =
		List.of(13237, 13238, 13239, 13240, 13241, 13242);

	/** Gricoller's can - aids watering AND is a Tithe Farm reward, so it sits in both lists. */
	private static final int GRICOLLERS_CAN = 13353;
	/** Seed box - purely a Tithe Farm reward, not used mid-round. */
	private static final int SEED_BOX = 13639;
	/** Intricate pouch - handed out by the GotR Rewards Guardian after a game, not brought. */
	private static final int INTRICATE_POUCH = 26908;

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
		// The field is opt-in: the Shades of Mort'ton pilot plus the Batch 1-3
		// re-authoring (Brimhaven Agility Arena, Tithe Farm, Guardians of the Rift)
		// are the only users. Every OTHER source must leave it null so the
		// existing data is untouched.
		List<String> knownUsers = List.of(
			"Shades of Mort'ton",
			"Brimhaven Agility Arena",
			"Tithe Farm",
			"Guardians of the Rift");
		for (CollectionLogSource src : sources)
		{
			if (knownUsers.contains(src.getName()) || src.getGuidanceSteps() == null)
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
	public void brimhaven_gracefulPieces_movedToActivityObtainable()
	{
		CollectionLogSource brimhaven = sources.stream()
			.filter(s -> "Brimhaven Agility Arena".equals(s.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Brimhaven Agility Arena source missing"));
		assertNotNull(brimhaven.getGuidanceSteps(), "Brimhaven must have guidance steps");

		// The Graceful recolour pieces are earned on-site via tickets, so they must
		// live in activityObtainableItemIds and never in any recommended list.
		boolean tagged = brimhaven.getGuidanceSteps().stream()
			.anyMatch(step -> step.getActivityObtainableItemIds().containsAll(BRIMHAVEN_GRACEFUL));
		assertTrue(tagged,
			"Brimhaven must tag the Graceful recolour pieces as obtained on-site");

		for (GuidanceStep step : brimhaven.getGuidanceSteps())
		{
			List<Integer> recommended = step.getRecommendedItemIds();
			if (recommended == null)
			{
				continue;
			}
			for (int pieceId : BRIMHAVEN_GRACEFUL)
			{
				assertFalse(recommended.contains(pieceId),
					"on-site Graceful piece " + pieceId
						+ " must not appear in Brimhaven recommendedItemIds");
			}
		}
	}

	@Test
	public void titheFarm_gricollersCan_inBothLists_seedBoxObtainableOnly()
	{
		CollectionLogSource tithe = sourceByName("Tithe Farm");
		assertNotNull(tithe.getGuidanceSteps(), "Tithe Farm must have guidance steps");

		// Gricoller's can aids watering AND is a reward, so it must appear in both
		// the recommended aids and the on-site obtainable list. The Seed box is a
		// pure reward and must only be tagged as obtained on-site.
		GuidanceStep setupStep = tithe.getGuidanceSteps().stream()
			.filter(s -> !s.getActivityObtainableItemIds().isEmpty())
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"Tithe Farm must have a step with activityObtainableItemIds"));

		List<Integer> recommended = setupStep.getRecommendedItemIds();
		assertNotNull(recommended, "Tithe Farm setup step keeps a recommended list");
		assertTrue(recommended.contains(GRICOLLERS_CAN),
			"Gricoller's can must stay in recommended (it aids watering)");
		assertFalse(recommended.contains(SEED_BOX),
			"Seed box is a pure reward and must leave the recommended list");

		List<Integer> obtainable = setupStep.getActivityObtainableItemIds();
		assertTrue(obtainable.contains(GRICOLLERS_CAN),
			"Gricoller's can must also be tagged as obtained on-site");
		assertTrue(obtainable.contains(SEED_BOX),
			"Seed box must be tagged as obtained on-site");
	}

	@Test
	public void guardiansOfTheRift_intricatePouch_movedToActivityObtainable()
	{
		CollectionLogSource gotr = sourceByName("Guardians of the Rift");
		assertNotNull(gotr.getGuidanceSteps(), "GotR must have guidance steps");

		// The Intricate pouch is handed out by the Rewards Guardian after a game,
		// so it must live in activityObtainableItemIds and never in any recommended
		// list. The brought tools (chisel, pickaxe, pouches) stay in recommended.
		boolean tagged = gotr.getGuidanceSteps().stream()
			.anyMatch(step -> step.getActivityObtainableItemIds().contains(INTRICATE_POUCH));
		assertTrue(tagged,
			"GotR must tag the Intricate pouch as obtained on-site");

		for (GuidanceStep step : gotr.getGuidanceSteps())
		{
			List<Integer> recommended = step.getRecommendedItemIds();
			if (recommended == null)
			{
				continue;
			}
			assertFalse(recommended.contains(INTRICATE_POUCH),
				"the on-site Intricate pouch must not appear in GotR recommendedItemIds");
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

	/** Locates a source by exact name or fails the test. */
	private static CollectionLogSource sourceByName(String name)
	{
		return sources.stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError(name + " source missing"));
	}

	/** Locates the Shades "burn shade remains" step (the only step with the activity field). */
	private static GuidanceStep shadesBurnStep()
	{
		CollectionLogSource shades = sourceByName("Shades of Mort'ton");
		assertNotNull(shades.getGuidanceSteps(), "Shades must have guidance steps");
		return shades.getGuidanceSteps().stream()
			.filter(s -> !s.getActivityObtainableItemIds().isEmpty())
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"Shades must have a step with activityObtainableItemIds"));
	}
}
