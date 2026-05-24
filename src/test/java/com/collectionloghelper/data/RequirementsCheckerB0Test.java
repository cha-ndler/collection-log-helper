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

import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PohTeleport;
import com.collectionloghelper.player.PohTeleportInventory;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * Tier B0 — behaviour tests for
 * {@link RequirementsChecker#meetsRequirements(SourceRequirements)} with the
 * new {@code pohTeleports} and {@code equippedItemIds} fields wired in.
 *
 * <p>Covers AND-semantics across all field clauses and alternative-selection
 * (first-match wins) — the latter exercises the same iteration order the
 * sequencer uses in {@link GuidanceStep#resolveAlternative(RequirementsChecker)}.
 *
 * <p>{@link PohTeleportInventory} and {@link EquippedItemState} are mocked at
 * the interface level. The {@link Client} mock backs the existing skill clause.
 * Strict stubbing is disabled because the AND-clause short-circuits skip later
 * stubs in failure tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequirementsCheckerB0Test
{
	private static final int RING_OF_SHADOWS = 28266;
	private static final int DRAKANS_MEDALLION = 22557;

	@Mock
	private Client client;

	@Mock
	private PohTeleportInventory pohTeleportInventory;

	@Mock
	private EquippedItemState equippedItemState;

	@Mock
	private PlayerInventoryState playerInventoryState;

	private RequirementsChecker checker;

	@BeforeEach
	public void setUp() throws Exception
	{
		// The @Inject constructor is private — use reflection to construct without Guice.
		Constructor<RequirementsChecker> ctor = RequirementsChecker.class.getDeclaredConstructor(
			Client.class, PohTeleportInventory.class, EquippedItemState.class,
			PlayerInventoryState.class);
		ctor.setAccessible(true);
		checker = ctor.newInstance(client, pohTeleportInventory, equippedItemState, playerInventoryState);
	}

	// ── null requirements ───────────────────────────────────────────────────

	@Test
	public void meetsRequirements_nullArg_isTrue()
	{
		assertTrue(checker.meetsRequirements(null));
	}

	// ── pohTeleports clause ─────────────────────────────────────────────────

	@Test
	public void meetsRequirements_pohTeleportPresent_isTrue()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		SourceRequirements req = pohReq("JEWELLERY_BOX_FANCY");
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_pohTeleportAbsent_isFalse()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.MOUNTED_GLORY)).thenReturn(false);
		SourceRequirements req = pohReq("MOUNTED_GLORY");
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_unknownPohTeleportEnum_isFalse()
	{
		// Unknown enum names are rejected (treated as unmet) — see RequirementsChecker.
		SourceRequirements req = pohReq("DOES_NOT_EXIST");
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_multiplePohTeleports_andSemantics()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.FAIRY_RING)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Arrays.asList("JEWELLERY_BOX_FANCY", "FAIRY_RING"),
			null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_emptyPohTeleports_isTrue()
	{
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.emptyList(),
			null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// ── equippedItemIds clause ──────────────────────────────────────────────

	@Test
	public void meetsRequirements_equippedItemPresent_isTrue()
	{
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		SourceRequirements req = equippedReq(RING_OF_SHADOWS);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_equippedItemAbsent_isFalse()
	{
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(false);
		SourceRequirements req = equippedReq(RING_OF_SHADOWS);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_multipleEquippedItems_andSemantics()
	{
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(DRAKANS_MEDALLION)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null,
			Arrays.asList(RING_OF_SHADOWS, DRAKANS_MEDALLION), null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_emptyEquippedItemIds_isTrue()
	{
		SourceRequirements req = new SourceRequirements(
			null, null, null, null,
			Collections.emptyList(), null);
		assertTrue(checker.meetsRequirements(req));
	}

	// ── AND-semantics across multiple fields ────────────────────────────────

	@Test
	public void meetsRequirements_skillAndPohAndEquipped_allSatisfied_isTrue()
	{
		lenient().when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(80);
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null,
			Collections.singletonList(new SkillRequirement("STRENGTH", 70)),
			null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			Collections.singletonList(RING_OF_SHADOWS), null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_skillSatisfiedButPohAbsent_isFalse()
	{
		lenient().when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(80);
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null,
			Collections.singletonList(new SkillRequirement("STRENGTH", 70)),
			null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_pohSatisfiedButEquippedAbsent_isFalse()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			Collections.singletonList(RING_OF_SHADOWS), null);
		assertFalse(checker.meetsRequirements(req));
	}

	// ── Alternative selection — first matching alternative wins ─────────────
	//
	// These tests mirror the iteration loop in
	// GuidanceStep.resolveAlternative(checker): the first alternative whose
	// requirements pass meetsRequirements() is selected. We assert this by
	// matching directly against meetsRequirements() in the same order, which
	// is what the sequencer does.

	@Test
	public void alternativeSelection_pohEquippedRouteWins_whenBothPresent()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: POH + ring", new SourceRequirements(
				null, null, null,
				Collections.singletonList("JEWELLERY_BOX_FANCY"),
				Collections.singletonList(RING_OF_SHADOWS), null)),
			altWithReq("Slow: ring only", equippedReq(RING_OF_SHADOWS)));

		assertEquals("Fast: POH + ring", selectFirstMatching(alts));
	}

	@Test
	public void alternativeSelection_fallsBackToSecondAlt_whenFirstNewFieldUnmet()
	{
		// Player has the ring but no POH teleport — slow route wins.
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(false);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: POH + ring", new SourceRequirements(
				null, null, null,
				Collections.singletonList("JEWELLERY_BOX_FANCY"),
				Collections.singletonList(RING_OF_SHADOWS), null)),
			altWithReq("Slow: ring only", equippedReq(RING_OF_SHADOWS)));

		assertEquals("Slow: ring only", selectFirstMatching(alts));
	}

	@Test
	public void alternativeSelection_noMatch_returnsNull()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(false);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(false);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("POH route", pohReq("JEWELLERY_BOX_FANCY")),
			altWithReq("Equipped route", equippedReq(RING_OF_SHADOWS)));

		assertEquals(null, selectFirstMatching(alts));
	}

	// ── Helpers ─────────────────────────────────────────────────────────────

	private static SourceRequirements pohReq(String teleportName)
	{
		return new SourceRequirements(
			null, null, null,
			Collections.singletonList(teleportName),
			null, null);
	}

	private static SourceRequirements equippedReq(int itemId)
	{
		return new SourceRequirements(
			null, null, null, null,
			Collections.singletonList(itemId), null);
	}

	private static ConditionalAlternative altWithReq(String description, SourceRequirements requirements)
	{
		return new ConditionalAlternative(
			requirements,
			description,
			null, null, null,  // worldX, worldY, worldPlane
			null,              // travelTip
			null,              // npcId
			null,              // interactAction
			null,              // objectId
			null,              // completionCondition
			null,              // completionDistance
			null,              // completionNpcId
			null               // nestedAlternatives
		);
	}

	/**
	 * Mirrors {@link GuidanceStep#resolveAlternative} selection logic against
	 * the same checker — returns the description of the first matching alt, or
	 * {@code null} when none match.
	 */
	private String selectFirstMatching(List<ConditionalAlternative> alts)
	{
		for (ConditionalAlternative alt : alts)
		{
			if (alt.getRequirements() != null && checker.meetsRequirements(alt.getRequirements()))
			{
				return alt.getDescription();
			}
		}
		return null;
	}
}
