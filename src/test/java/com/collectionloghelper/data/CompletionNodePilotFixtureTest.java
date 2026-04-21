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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pilot-fixture test proving the new {@link CompletionNode} shape round-trips end to
 * end via the same {@link com.google.gson.Gson} wiring that production uses. The
 * fixture lives at {@code src/test/resources/com/collectionloghelper/data/b1_pilot_source.json}
 * and is the ONLY place where the new tree shape appears — the 225 production source
 * files stay on the legacy single-enum form (B1 is strictly additive).
 */
public class CompletionNodePilotFixtureTest
{
	private Gson gson;

	@Before
	public void setUp()
	{
		gson = new GsonBuilder()
			.registerTypeAdapter(CompletionNode.class, new CompletionNodeAdapter())
			.create();
	}

	private List<CollectionLogSource> loadPilotFixture() throws Exception
	{
		try (InputStream is = getClass().getResourceAsStream(
			"/com/collectionloghelper/data/b1_pilot_source.json"))
		{
			assertNotNull("Pilot fixture must be on the test classpath", is);
			Type listType = new TypeToken<List<CollectionLogSource>>(){}.getType();
			return gson.fromJson(new InputStreamReader(is), listType);
		}
	}

	@Test
	public void pilotFixtureParsesWithoutError() throws Exception
	{
		List<CollectionLogSource> sources = loadPilotFixture();
		assertEquals(1, sources.size());
		assertEquals("B1 Pilot Source", sources.get(0).getName());
	}

	@Test
	public void legacyStepKeepsNullCompletionNode() throws Exception
	{
		GuidanceStep step = loadPilotFixture().get(0).getGuidanceSteps().get(0);
		assertNull("Legacy-shape step must not synthesize a completionNode", step.getCompletionNode());
		assertEquals(CompletionCondition.MANUAL, step.getCompletionCondition());
	}

	@Test
	public void andStepHasAllKind() throws Exception
	{
		GuidanceStep step = loadPilotFixture().get(0).getGuidanceSteps().get(1);
		CompletionNode node = step.getCompletionNode();
		assertNotNull("AND step must carry a completionNode", node);
		assertEquals(CompletionNode.Kind.ALL, node.getKind());

		CompletionNode.All all = (CompletionNode.All) node;
		assertEquals(2, all.getChildren().size());
		assertEquals(CompletionCondition.INVENTORY_HAS_ITEM,
			((CompletionNode.Leaf) all.getChildren().get(0)).getCondition());
		assertEquals(CompletionCondition.ARRIVE_AT_ZONE,
			((CompletionNode.Leaf) all.getChildren().get(1)).getCondition());
	}

	@Test
	public void orStepHasAnyKind() throws Exception
	{
		GuidanceStep step = loadPilotFixture().get(0).getGuidanceSteps().get(2);
		CompletionNode node = step.getCompletionNode();
		assertNotNull(node);
		assertEquals(CompletionNode.Kind.ANY, node.getKind());
		assertEquals(2, ((CompletionNode.Any) node).getChildren().size());
	}

	@Test
	public void nestedStepPreservesBranching() throws Exception
	{
		GuidanceStep step = loadPilotFixture().get(0).getGuidanceSteps().get(3);
		CompletionNode root = step.getCompletionNode();
		assertEquals(CompletionNode.Kind.ALL, root.getKind());
		CompletionNode.All all = (CompletionNode.All) root;
		assertEquals(2, all.getChildren().size());
		assertEquals(CompletionNode.Kind.LEAF, all.getChildren().get(0).getKind());
		assertEquals(CompletionNode.Kind.ANY, all.getChildren().get(1).getKind());

		CompletionNode.Any inner = (CompletionNode.Any) all.getChildren().get(1);
		assertEquals(2, inner.getChildren().size());
		assertEquals(CompletionCondition.PLAYER_ON_PLANE,
			((CompletionNode.Leaf) inner.getChildren().get(1)).getCondition());
	}

	@Test
	public void pilotFixtureTreesEvaluateAsExpected() throws Exception
	{
		GuidanceStep andStep = loadPilotFixture().get(0).getGuidanceSteps().get(1);
		CompletionNode andNode = andStep.getCompletionNode();
		// Both true → complete
		assertTrue(andNode.evaluate(c ->
			c == CompletionCondition.INVENTORY_HAS_ITEM || c == CompletionCondition.ARRIVE_AT_ZONE));
		// Only one true → incomplete
		assertFalse(andNode.evaluate(c -> c == CompletionCondition.INVENTORY_HAS_ITEM));

		GuidanceStep orStep = loadPilotFixture().get(0).getGuidanceSteps().get(2);
		CompletionNode orNode = orStep.getCompletionNode();
		// Any true → complete
		assertTrue(orNode.evaluate(c -> c == CompletionCondition.ITEM_OBTAINED));
		// None true → incomplete
		assertFalse(orNode.evaluate(c -> false));
	}
}
