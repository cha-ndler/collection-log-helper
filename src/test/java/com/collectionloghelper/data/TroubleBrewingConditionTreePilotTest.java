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

import com.collectionloghelper.data.condition.AndNode;
import com.collectionloghelper.data.condition.ConditionNode;
import com.collectionloghelper.data.condition.LeafNode;
import com.collectionloghelper.data.condition.NotNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B1 Phase 3 pilot - structural regression guard for the
 * {@code Trouble Brewing} victory-drop step's {@code conditionTree}.
 *
 * <p>The pilot rationale (see scoping doc section 4) is that the legacy
 * flat enum coerces a conjunctive completion ("back in the lobby AND no
 * brewing item in hand") into a single {@code MANUAL} step. The tree
 * authored on this step expresses the conjunction natively:
 *
 * <pre>
 * "conditionTree": {
 *   "and": [
 *     { "arrive_at_zone": [3805, 3017, 3815, 3024, 0] },
 *     { "not": { "inventory_has_item": 1929 } }
 *   ]
 * }
 * </pre>
 *
 * <p>This test is intentionally structural. The live evaluator is exercised
 * by {@code ConditionNodeEvaluatorTest}; here we only assert that
 * {@code drop_rates.json} parses into the expected tree shape and that the
 * legacy flat field is preserved for the fall-through path.
 */
public class TroubleBrewingConditionTreePilotTest
{
	private static final String SOURCE_NAME = "Trouble Brewing";
	private static final int LOBBY_MIN_X = 3805;
	private static final int LOBBY_MIN_Y = 3017;
	private static final int LOBBY_MAX_X = 3815;
	private static final int LOBBY_MAX_Y = 3024;
	private static final int LOBBY_PLANE = 0;
	private static final int BUCKET_OF_WATER_ITEM_ID = 1929;

	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();

		// Stock Gson. The conditionTree adapter is wired via @JsonAdapter on
		// GuidanceStep.conditionTree, so no manual registration is needed -
		// matching what RuneLite supplies via @Inject in production.
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);

		database.load();
	}

	@Test
	public void troubleBrewingSourceLoads()
	{
		CollectionLogSource source = findTroubleBrewing();
		assertNotNull(source, SOURCE_NAME + " must be present in drop_rates.json");
		assertNotNull(source.getGuidanceSteps(),
			SOURCE_NAME + " must have guidance steps");
		assertTrue(source.getGuidanceSteps().size() >= 1,
			SOURCE_NAME + " must have at least one guidance step");
	}

	@Test
	public void victoryDropStep_hasConjunctiveConditionTree()
	{
		GuidanceStep victory = findVictoryDropStep();

		ConditionNode tree = victory.getConditionTree();
		assertNotNull(tree,
			"victory-drop step must opt into conditionTree; see B1 Phase 3 scoping doc");

		assertTrue(tree instanceof AndNode,
			"root must be AndNode (in-lobby AND no-brew-item); got "
				+ tree.getClass().getSimpleName());

		AndNode root = (AndNode) tree;
		assertEquals(2, root.getChildren().size(),
			"root AND must have exactly 2 children: zone leaf and NOT(inventory) subtree");

		// Child 0: ARRIVE_AT_ZONE leaf with the lobby rectangle.
		ConditionNode zoneChild = root.getChildren().get(0);
		assertTrue(zoneChild instanceof LeafNode,
			"first child must be a LeafNode for ARRIVE_AT_ZONE; got "
				+ zoneChild.getClass().getSimpleName());
		LeafNode zoneLeaf = (LeafNode) zoneChild;
		assertSame(CompletionCondition.ARRIVE_AT_ZONE, zoneLeaf.getType(),
			"first leaf must be ARRIVE_AT_ZONE");
		int[] zone = zoneLeaf.getZone();
		assertNotNull(zone, "ARRIVE_AT_ZONE leaf must carry a zone array");
		assertEquals(5, zone.length, "zone array must be [minX, minY, maxX, maxY, plane]");
		assertEquals(LOBBY_MIN_X, zone[0]);
		assertEquals(LOBBY_MIN_Y, zone[1]);
		assertEquals(LOBBY_MAX_X, zone[2]);
		assertEquals(LOBBY_MAX_Y, zone[3]);
		assertEquals(LOBBY_PLANE, zone[4]);

		// Child 1: NOT(INVENTORY_HAS_ITEM bucket-of-water).
		ConditionNode notChild = root.getChildren().get(1);
		assertTrue(notChild instanceof NotNode,
			"second child must be a NotNode for the absent-brew-item guard; got "
				+ notChild.getClass().getSimpleName());
		ConditionNode invChild = ((NotNode) notChild).getChild();
		assertNotNull(invChild, "NOT must wrap a non-null inner node");
		assertTrue(invChild instanceof LeafNode,
			"NOT must wrap an INVENTORY_HAS_ITEM leaf; got "
				+ invChild.getClass().getSimpleName());
		LeafNode invLeaf = (LeafNode) invChild;
		assertSame(CompletionCondition.INVENTORY_HAS_ITEM, invLeaf.getType(),
			"NOT's child must be INVENTORY_HAS_ITEM");
		assertNotNull(invLeaf.getItemId(), "INVENTORY_HAS_ITEM leaf must carry an itemId");
		assertEquals(BUCKET_OF_WATER_ITEM_ID, invLeaf.getItemId().intValue(),
			"the active brew-item guard must reference bucket of water (id 1929) - "
				+ "the same item the source's existing cumulative tracker uses");
	}

	@Test
	public void victoryDropStep_keepsLegacyFlatField()
	{
		GuidanceStep victory = findVictoryDropStep();

		// The flat field is preserved verbatim: anyone running on an older
		// build, or any code path that has not yet been migrated to consult
		// the tree, sees exactly the pre-pilot behaviour.
		assertSame(CompletionCondition.MANUAL, victory.getCompletionCondition(),
			"victory-drop step must keep MANUAL as its legacy flat condition for the fall-through path");
	}

	@Test
	public void onlyVictoryDropStepCarriesConditionTree()
	{
		CollectionLogSource source = findTroubleBrewing();
		int treeCount = 0;
		for (GuidanceStep step : source.getGuidanceSteps())
		{
			if (step != null && step.getConditionTree() != null)
			{
				treeCount++;
			}
		}
		assertEquals(1, treeCount,
			SOURCE_NAME + " must opt exactly one step (the victory-drop) into conditionTree; "
				+ "found " + treeCount);
	}

	private CollectionLogSource findTroubleBrewing()
	{
		for (CollectionLogSource source : database.getAllSources())
		{
			if (SOURCE_NAME.equals(source.getName()))
			{
				return source;
			}
		}
		throw new AssertionError(SOURCE_NAME + " not found in drop_rates.json");
	}

	private GuidanceStep findVictoryDropStep()
	{
		CollectionLogSource source = findTroubleBrewing();
		// The victory-drop step is the last step in the sequence: the AFK /
		// trade-Honest-Jimmy step. Finding by description keeps the test
		// resilient to future step insertions earlier in the chain.
		for (GuidanceStep step : source.getGuidanceSteps())
		{
			if (step != null
				&& step.getDescription() != null
				&& step.getDescription().toLowerCase().contains("afk until the game ends"))
			{
				return step;
			}
		}
		throw new AssertionError(
			"Victory-drop step not found in " + SOURCE_NAME + " guidance sequence");
	}
}
