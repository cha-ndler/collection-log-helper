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

import com.collectionloghelper.data.condition.ConditionNode;
import com.collectionloghelper.data.condition.LeafNode;
import com.collectionloghelper.data.condition.OrNode;
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
 * B1 Phase 4 pilot - structural regression guard for the {@code Wintertodt}
 * brazier-light / pyromancer-heal step's {@code conditionTree}.
 *
 * <p>The pilot rationale (see scoping doc section 4) is that the legacy flat
 * enum coerces a disjunctive completion ("the round finished from the player's
 * point of view") into a single {@code MANUAL} step. The actual round-end
 * signal in Wintertodt is one of two chat messages: a global notice that the
 * boss has been subdued, or the per-player points tally that fires only for
 * players who scored at least 500. Either is sufficient to advance the step.
 *
 * <p>Tree authored on this step:
 *
 * <pre>
 * "conditionTree": {
 *   "or": [
 *     { "chat_message_received": "The Wintertodt has been subdued" },
 *     { "chat_message_received": "Your Wintertodt subdued count is:" }
 *   ]
 * }
 * </pre>
 *
 * <p>This test is intentionally structural. The live evaluator is exercised
 * by {@code ConditionNodeEvaluatorTest}; here we only assert that
 * {@code drop_rates.json} parses into the expected tree shape and that the
 * legacy flat field is preserved for the fall-through path. Mirrors
 * {@code TroubleBrewingConditionTreePilotTest}, which is the Phase 3 pilot.
 */
public class WintertodtConditionTreePilotTest
{
	private static final String SOURCE_NAME = "Wintertodt";
	private static final String SUBDUED_GLOBAL_PATTERN = "The Wintertodt has been subdued";
	private static final String SUBDUED_PERSONAL_PATTERN = "Your Wintertodt subdued count is:";

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
	public void wintertodtSourceLoads()
	{
		CollectionLogSource source = findWintertodt();
		assertNotNull(source, SOURCE_NAME + " must be present in drop_rates.json");
		assertNotNull(source.getGuidanceSteps(),
			SOURCE_NAME + " must have guidance steps");
		assertTrue(source.getGuidanceSteps().size() >= 1,
			SOURCE_NAME + " must have at least one guidance step");
	}

	@Test
	public void brazierStep_hasDisjunctiveConditionTree()
	{
		GuidanceStep brazier = findBrazierStep();

		ConditionNode tree = brazier.getConditionTree();
		assertNotNull(tree,
			"brazier-light step must opt into conditionTree; see B1 Phase 3 scoping doc section 4");

		assertTrue(tree instanceof OrNode,
			"root must be OrNode (subdued-global OR subdued-personal); got "
				+ tree.getClass().getSimpleName());

		OrNode root = (OrNode) tree;
		assertEquals(2, root.getChildren().size(),
			"root OR must have exactly 2 children: the two round-end chat patterns");

		// Child 0: CHAT_MESSAGE_RECEIVED for the global subdued notice.
		ConditionNode globalChild = root.getChildren().get(0);
		assertTrue(globalChild instanceof LeafNode,
			"first child must be a LeafNode for CHAT_MESSAGE_RECEIVED; got "
				+ globalChild.getClass().getSimpleName());
		LeafNode globalLeaf = (LeafNode) globalChild;
		assertSame(CompletionCondition.CHAT_MESSAGE_RECEIVED, globalLeaf.getType(),
			"first leaf must be CHAT_MESSAGE_RECEIVED");
		assertEquals(SUBDUED_GLOBAL_PATTERN, globalLeaf.getChatPattern(),
			"first leaf must carry the global round-end subdued pattern");

		// Child 1: CHAT_MESSAGE_RECEIVED for the per-player points tally.
		ConditionNode personalChild = root.getChildren().get(1);
		assertTrue(personalChild instanceof LeafNode,
			"second child must be a LeafNode for CHAT_MESSAGE_RECEIVED; got "
				+ personalChild.getClass().getSimpleName());
		LeafNode personalLeaf = (LeafNode) personalChild;
		assertSame(CompletionCondition.CHAT_MESSAGE_RECEIVED, personalLeaf.getType(),
			"second leaf must be CHAT_MESSAGE_RECEIVED");
		assertEquals(SUBDUED_PERSONAL_PATTERN, personalLeaf.getChatPattern(),
			"second leaf must carry the per-player subdued-count pattern");
	}

	@Test
	public void brazierStep_keepsLegacyFlatField()
	{
		GuidanceStep brazier = findBrazierStep();

		// The flat field is preserved verbatim: anyone running on an older
		// build, or any code path that has not yet been migrated to consult
		// the tree, sees exactly the pre-pilot behaviour (manual advance).
		assertSame(CompletionCondition.MANUAL, brazier.getCompletionCondition(),
			"brazier-light step must keep MANUAL as its legacy flat condition for the fall-through path");
	}

	@Test
	public void onlyBrazierStepCarriesConditionTree()
	{
		CollectionLogSource source = findWintertodt();
		int treeCount = 0;
		for (GuidanceStep step : source.getGuidanceSteps())
		{
			if (step != null && step.getConditionTree() != null)
			{
				treeCount++;
			}
		}
		assertEquals(1, treeCount,
			SOURCE_NAME + " must opt exactly one step (the brazier-light) into conditionTree; "
				+ "found " + treeCount);
	}

	private CollectionLogSource findWintertodt()
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

	private GuidanceStep findBrazierStep()
	{
		CollectionLogSource source = findWintertodt();
		// Find by description fragment that is unique to the brazier-light step
		// and resilient to future step insertions earlier in the chain.
		for (GuidanceStep step : source.getGuidanceSteps())
		{
			if (step != null
				&& step.getDescription() != null
				&& step.getDescription().toLowerCase().contains("feed kindling to the brazier"))
			{
				return step;
			}
		}
		throw new AssertionError(
			"Brazier-light step not found in " + SOURCE_NAME + " guidance sequence");
	}
}
