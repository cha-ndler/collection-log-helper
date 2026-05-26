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
package com.collectionloghelper.guidance.dynamic;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link WintertodtBrazierEvaluator} with a mocked {@link Client}.
 *
 * <p>Verified scenarios:
 * <ol>
 *   <li>Returns null when the top-level WorldView is null.</li>
 *   <li>Returns null when no active brazier NPC is in the WorldView.</li>
 *   <li>Returns null when only broken/unlit braziers are present (ID 29314, 31926).</li>
 *   <li>Returns the location of a single lit brazier (ID 29312).</li>
 *   <li>Returns the location of a single lit brazier (ID 29313).</li>
 *   <li>Returns the nearest lit brazier when multiple are present.</li>
 *   <li>Falls back to first found brazier when player location is unavailable.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WintertodtBrazierEvaluatorTest
{
	@Mock
	private Client client;
	@Mock
	private WorldView worldView;
	@Mock
	private Player localPlayer;

	private WintertodtBrazierEvaluator evaluator;
	private GuidanceStep dummyStep;

	/** A fixed player position inside the Wintertodt arena. */
	private static final WorldPoint PLAYER_POS = new WorldPoint(1640, 4000, 0);

	@BeforeEach
	public void setUp()
	{
		evaluator = new WintertodtBrazierEvaluator();
		dummyStep = makeStep();

		when(client.getTopLevelWorldView()).thenReturn(worldView);
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getWorldLocation()).thenReturn(PLAYER_POS);
	}

	// ── Null/empty world view ─────────────────────────────────────────────────

	@Test
	public void evaluate_nullWorldView_returnsNull()
	{
		when(client.getTopLevelWorldView()).thenReturn(null);
		assertNull(evaluator.evaluate(client, dummyStep));
	}

	@Test
	public void evaluate_emptyNpcList_returnsNull()
	{
		stubNpcs(Collections.emptyList());
		assertNull(evaluator.evaluate(client, dummyStep));
	}

	// ── Wrong NPC IDs ─────────────────────────────────────────────────────────

	@Test
	public void evaluate_onlyBrokenBrazierPresent_returnsNull()
	{
		NPC broken = mockNpc(29314, new WorldPoint(1620, 4000, 0));
		stubNpcs(Collections.singletonList(broken));
		assertNull( evaluator.evaluate(client, dummyStep),"Broken brazier (29314) must not be returned");
	}

	@Test
	public void evaluate_onlyUnlitBrazierVariant_returnsNull()
	{
		NPC unlit = mockNpc(31926, new WorldPoint(1620, 4000, 0));
		stubNpcs(Collections.singletonList(unlit));
		assertNull( evaluator.evaluate(client, dummyStep),"Unlit brazier (31926) must not be returned");
	}

	// ── Single lit brazier ────────────────────────────────────────────────────

	@Test
	public void evaluate_singleLitBrazierType1_returnsItsLocation()
	{
		WorldPoint expected = new WorldPoint(1621, 3999, 0);
		NPC lit = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_1, expected);
		stubNpcs(Collections.singletonList(lit));
		assertEquals(expected, evaluator.evaluate(client, dummyStep));
	}

	@Test
	public void evaluate_singleLitBrazierType2_returnsItsLocation()
	{
		WorldPoint expected = new WorldPoint(1655, 4002, 0);
		NPC lit = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_2, expected);
		stubNpcs(Collections.singletonList(lit));
		assertEquals(expected, evaluator.evaluate(client, dummyStep));
	}

	// ── Multiple lit braziers — nearest wins ──────────────────────────────────

	@Test
	public void evaluate_multipleLitBraziers_returnsNearest()
	{
		// PLAYER_POS = (1640, 4000). Near brazier at (1641, 4000) — distance 1.
		// Far brazier at (1620, 4000) — distance 20.
		WorldPoint nearPoint = new WorldPoint(1641, 4000, 0);
		WorldPoint farPoint  = new WorldPoint(1620, 4000, 0);

		NPC near = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_1, nearPoint);
		NPC far  = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_2, farPoint);

		stubNpcs(Arrays.asList(far, near));
		assertEquals( nearPoint,
			evaluator.evaluate(client, dummyStep),"Should return the nearest lit brazier");
	}

	// ── No player location fallback ───────────────────────────────────────────

	@Test
	public void evaluate_noPlayerLocation_returnsFirstFoundBrazier()
	{
		when(client.getLocalPlayer()).thenReturn(null);

		WorldPoint firstPoint  = new WorldPoint(1621, 3999, 0);
		WorldPoint secondPoint = new WorldPoint(1655, 4002, 0);

		NPC first  = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_1, firstPoint);
		NPC second = mockNpc(WintertodtBrazierEvaluator.BRAZIER_LIT_2, secondPoint);

		stubNpcs(Arrays.asList(first, second));
		assertEquals(
			firstPoint, evaluator.evaluate(client, dummyStep),"Should return the first found brazier when player location unavailable");
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	/** Stubs {@code worldView.npcs()} to iterate over {@code npcs}. */
	@SuppressWarnings("unchecked")
	private void stubNpcs(List<NPC> npcs)
	{
		IndexedObjectSet<NPC> npcSet = mock(IndexedObjectSet.class);
		Iterator<NPC> iter1 = npcs.iterator();
		Iterator<NPC> iter2 = npcs.iterator();
		when(npcSet.iterator()).thenReturn(iter1, iter2);
		doReturn(npcSet).when(worldView).npcs();
	}

	private static NPC mockNpc(int id, WorldPoint location)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(id);
		when(npc.getWorldLocation()).thenReturn(location);
		return npc;
	}

	private static GuidanceStep makeStep()
	{
		return new GuidanceStep(
			"Light the nearest brazier",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			null,
			null /* perItemRecommendedItemIds */,  // recommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,  // worldMessage
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null,  // conditionalAlternatives
			null,  // section
			null,  // waypoints
			DynamicTargetEvaluatorRegistry.WINTERTODT_ACTIVE_BRAZIER,  // dynamicTargetEvaluator
			null,  // conditionTree
						null, // perItemStepPriority
						null  // activityObtainableItemIds
		);
	}
}
