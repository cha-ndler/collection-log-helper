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
 * new {@code inventoryItemIds} field wired in.
 *
 * <p>Pairs with {@link RequirementsCheckerB0Test} (the Tier B0 PR #623 sibling)
 * which covers the {@code pohTeleports} and {@code equippedItemIds} clauses.
 * This class adds the same matrix for the inventory-item clause: empty list is
 * vacuously satisfied, single-item presence is AND-tested via
 * {@link PlayerInventoryState#hasItem(int)}, multi-item is AND across all IDs,
 * and the field composes AND-wise with every other clause.
 *
 * <p>{@link PlayerInventoryState} is mocked at the concrete class level (it is
 * a Singleton, not an interface). {@link PohTeleportInventory} and
 * {@link EquippedItemState} mocks are present to satisfy the constructor but
 * are not stubbed in the inventory-only tests. Strict stubbing is disabled so
 * AND-clause short-circuiting in failure tests does not surface
 * UnnecessaryStubbingException.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RequirementsCheckerInventoryItemsTest
{
	// Pharaoh's sceptre charges-5: an inventory-teleport item that is wieldable
	// but whose Jaltevas teleport is invoked from the inventory regardless.
	private static final int PHARAOHS_SCEPTRE_5 = 26945;

	// Quetzal whistle: inventory-only Phantom Muspah teleport, not wieldable.
	private static final int QUETZAL_WHISTLE = 29782;

	// Ring of Shadows: equipment-slot teleport (the C2 case), included here to
	// pin the cross-field AND-semantics for the inventory + equipped combo.
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
	public void meetsRequirements_emptyInventoryItemIds_isTrue()
	{
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Collections.emptyList(), null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- Single-item AND --------------------------------------------------------

	@Test
	public void meetsRequirements_singleInventoryItemPresent_isTrue()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = inventoryReq(PHARAOHS_SCEPTRE_5);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_singleInventoryItemAbsent_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		SourceRequirements req = inventoryReq(PHARAOHS_SCEPTRE_5);
		assertFalse(checker.meetsRequirements(req));
	}

	// -- Multi-item AND ---------------------------------------------------------

	@Test
	public void meetsRequirements_multipleInventoryItemsAllPresent_isTrue()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, QUETZAL_WHISTLE), null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_multipleInventoryItemsOneMissing_isFalse()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null,
			Arrays.asList(PHARAOHS_SCEPTRE_5, QUETZAL_WHISTLE), null);
		assertFalse(checker.meetsRequirements(req));
	}

	// -- Null entries inside the list are skipped, not NPE ---------------------

	@Test
	public void meetsRequirements_inventoryItemIdsWithNullEntry_skipsNullAndCheck()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		List<Integer> ids = Arrays.asList(null, PHARAOHS_SCEPTRE_5);
		SourceRequirements req = new SourceRequirements(
			null, null, null, null, null, ids, null);
		assertTrue(checker.meetsRequirements(req));
	}

	// -- AND-semantics across multiple clauses ---------------------------------

	@Test
	public void meetsRequirements_pohAndEquippedAndInventory_allSatisfied_isTrue()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			Collections.singletonList(RING_OF_SHADOWS),
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null);
		assertTrue(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_otherClausesSatisfiedButInventoryAbsent_isFalse()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(true);
		lenient().when(equippedItemState.hasEquipped(RING_OF_SHADOWS)).thenReturn(true);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			Collections.singletonList(RING_OF_SHADOWS),
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null);
		assertFalse(checker.meetsRequirements(req));
	}

	@Test
	public void meetsRequirements_inventoryPresentButPohAbsent_isFalse()
	{
		lenient().when(pohTeleportInventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY)).thenReturn(false);
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);
		SourceRequirements req = new SourceRequirements(
			null, null, null,
			Collections.singletonList("JEWELLERY_BOX_FANCY"),
			null,
			Collections.singletonList(PHARAOHS_SCEPTRE_5), null);
		assertFalse(checker.meetsRequirements(req));
	}

	// -- Alternative selection (mirrors GuidanceStep.resolveAlternative) -------

	@Test
	public void alternativeSelection_inventoryRouteWins_whenItemPresent()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(true);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: Pharaoh sceptre inventory", inventoryReq(PHARAOHS_SCEPTRE_5)),
			altWithReq("Slow: walk", null));

		assertEquals("Fast: Pharaoh sceptre inventory", selectFirstMatching(alts));
	}

	@Test
	public void alternativeSelection_fallsBackWhenInventoryItemAbsent()
	{
		lenient().when(playerInventoryState.hasItem(PHARAOHS_SCEPTRE_5)).thenReturn(false);

		List<ConditionalAlternative> alts = Arrays.asList(
			altWithReq("Fast: Pharaoh sceptre inventory", inventoryReq(PHARAOHS_SCEPTRE_5)),
			altWithReq("Fallback: Quetzal whistle inventory", inventoryReq(QUETZAL_WHISTLE)));

		lenient().when(playerInventoryState.hasItem(QUETZAL_WHISTLE)).thenReturn(true);
		assertEquals("Fallback: Quetzal whistle inventory", selectFirstMatching(alts));
	}

	// -- Helpers ----------------------------------------------------------------

	private static SourceRequirements inventoryReq(int itemId)
	{
		return new SourceRequirements(
			null, null, null, null, null,
			Collections.singletonList(itemId), null);
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
