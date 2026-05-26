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
 * Behaviour tests for
 * {@link RequirementsChecker#meetsRequirements(SourceRequirements)} with the
 * new {@code inventoryItemIdsAny} OR-semantic field wired in.
 *
 * <p>Pairs with {@link RequirementsCheckerInventoryItemsTest} (the AND-semantic
 * sibling) which covers the {@code inventoryItemIds} clause. This class adds
 * the same matrix for the OR-semantic any-of clause: empty list is vacuously
 * satisfied, any-one match passes, none-match fails, multi-item OR is
 * short-circuit-friendly, and the field composes AND-wise with every other
 * clause (including the AND-semantic {@code inventoryItemIds} sibling).
 *
 * <p>{@link PlayerInventoryState} is mocked at the concrete class level
 * (Singleton, no interface). Strict stubbing is disabled so AND-clause
 * short-circuiting in failure tests does not surface
 * UnnecessaryStubbingException.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequirementsCheckerInventoryItemsAnyTest
{
	// Pharaoh's sceptre charges-5: one of six charge-tier variants of the
	// Jaltevas teleport. The any-of field exists to express the OR across the
	// full 26945..26950 set without forcing the player to hold every charge.
	private static final int PHARAOHS_SCEPTRE_5 = 26945;
	private static final int PHARAOHS_SCEPTRE_4 = 26946;
	private static final int PHARAOHS_SCEPTRE_3 = 26947;

	// Quetzal whistle: multi-tier inventory teleport for Phantom Muspah.
	// Stand-in value for the basic-tier variant used in OR-vs-AND combo tests.
	private static final int QUETZAL_WHISTLE = 29782;

	// Ring of Shadows: equipment-slot teleport (the C2 case), included to pin
	// cross-field AND-semantics for the equipped + any-of-inventory combo.
	private static final int RING_OF_SHADOWS = 28266;

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
		// The @Inject constructor is private; reflectively construct without Guice.
		Constructor<RequirementsChecker> ctor = RequirementsChecker.class.getDeclaredConstructor(
			Client.class, PohTeleportInventory.class, EquippedItemState.class,
			PlayerInventoryState.class);
		ctor.setAccessible(true);
		checker = ctor.newInstance(client, pohTeleportInventory, equippedItemState, playerInventoryState);
	}

	// -- Empty / vacuous case ---------------------------------------------------

	@Test
	public void meetsRequirements_emptyInventoryItemIdsAny_isTrue()
	{
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null,
			Collections.emptyList(), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- Single-item OR ---------------------------------------------------------

	@Test
	public void meetsRequirements_singleItemPresent_isTrue()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = anyReq(PHARAOHS_SCEPTRE_5);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_singleItemAbsent_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		SourceRequirements req = anyReq(PHARAOHS_SCEPTRE_5);
		assertFalse(checker.meetsRequirements(req));
	}

	// -- Multi-item OR ----------------------------------------------------------

	@Test
	public void meetsRequirements_multipleItemsOnePresent_isTrue()
	{
		// First entry absent, second present: classic any-of pass case.
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_3)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4, PHARAOHS_SCEPTRE_3), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_multipleItemsNonePresent_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_3)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4, PHARAOHS_SCEPTRE_3), null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_multipleItemsAllPresent_isTrue()
	{
		// All-present is still a pass (OR is satisfied by at least one).
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- Null entries inside the list ------------------------------------------

	@Test
	public void meetsRequirements_nullEntryThenMatch_isTrue()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		List<Integer> ids = Arrays.asList(null, PHARAOHS_SCEPTRE_5);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null, ids, null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_allNullEntries_isTrue()
	{
		// An all-null any-of list collapses to "no real entries", which matches
		// the vacuous-empty contract. Skipping the check is consistent with the
		// AND-semantic siblings' empty-list behaviour.
		List<Integer> ids = Arrays.asList((Integer) null, (Integer) null);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, null, ids, null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- AND-with-other-clauses semantics --------------------------------------

	@Test
	public void meetsRequirements_anyOfPlusEquipped_bothSatisfied_isTrue()
	{
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null,
			Collections.singletonList(RING_OF_SHADOWS),
			null,
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_anyOfPasses_butEquippedFails_isFalse()
	{
		// OR-clause matches but the AND-combined equipped clause does not, so
		// the overall requirement fails. Pins the AND-combination contract.
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null,
			Collections.singletonList(RING_OF_SHADOWS),
			null,
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_anyOfPlusPohAndQuest_allSatisfied_isTrue()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			null,
			null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- AND-semantic inventoryItemIds + OR-semantic inventoryItemIdsAny together

	@Test
	public void meetsRequirements_inventoryAndPlusAny_bothSatisfied_isTrue()
	{
		// Player must hold every ID in inventoryItemIds AND at least one ID
		// from inventoryItemIdsAny. This is the multi-tier teleport pattern
		// combined with a single mandatory consumable (hypothetical).
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(QUETZAL_WHISTLE),
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_inventoryAndPasses_butAnyFails_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(QUETZAL_WHISTLE),
			Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_anyPasses_butInventoryAndFails_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(QUETZAL_WHISTLE),
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null, null);
		assertFalse(checker.meetsRequirements(req));
	}

	// -- Alternative selection (mirrors GuidanceStep.resolveAlternative) -------

	@Test
	public void alternativeSelection_anyOfRouteWins_whenOneItemPresent()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(true);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: Pharaoh sceptre any tier",
				new SourceRequirements(null, null, null, null, null, null,
					Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null)),
			altWithReq("Slow: walk", null));

		assertEquals("Fast: Pharaoh sceptre any tier", selectFirstMatching(alts));
	}

	@Test
	public void alternativeSelection_fallsBackWhenNoChargeTierHeld()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_4)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(true);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: Pharaoh sceptre any tier",
				new SourceRequirements(null, null, null, null, null, null,
					Arrays.asList(PHARAOHS_SCEPTRE_5, PHARAOHS_SCEPTRE_4), null, null)),
			altWithReq("Fallback: Quetzal whistle inventory",
				new SourceRequirements(null, null, null, null, null,
					Collections.singletonList(QUETZAL_WHISTLE), null, null, null)));

		assertEquals("Fallback: Quetzal whistle inventory", selectFirstMatching(alts));
	}

	// -- Helpers ----------------------------------------------------------------

	private static SourceRequirements anyReq(int itemId)
	{
		return new SourceRequirements(
			null, null, null, null, null, null,
			Collections.singletonList(itemId), null, null);
	}

	private static ConditionalAlternative altWithReq(String description, SourceRequirements requirements)
	{
		return new ConditionalAlternative(
			requirements,
			description,
			null, null, null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);
	}

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
