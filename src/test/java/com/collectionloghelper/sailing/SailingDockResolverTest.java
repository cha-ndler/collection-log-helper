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
package com.collectionloghelper.sailing;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.lang.reflect.Constructor;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SailingDockResolverTest
{
	// Varbit ids (RuneLite VarbitID.java). Boat N (1-based): base + (N-1)*stride.
	private static final int STRIDE = 38;
	private static final int OWNED = 19258;
	private static final int TYPE = 19259;
	private static final int PORT = 19260;
	private static final int MAXHP = 19463; // contiguous 19463..19467

	private Client client;
	private SailingDockResolver resolver;

	@BeforeEach
	void setUp() throws Exception
	{
		client = mock(Client.class);
		Constructor<SailingDockResolver> c = SailingDockResolver.class.getDeclaredConstructor(Client.class);
		c.setAccessible(true);
		resolver = c.newInstance(client);
	}

	/** Stubs the four state varbits for a 1-based boat slot. */
	private void boat(int slot, int owned, int type, int maxHp, int portId)
	{
		int off = (slot - 1) * STRIDE;
		when(client.getVarbitValue(OWNED + off)).thenReturn(owned);
		when(client.getVarbitValue(TYPE + off)).thenReturn(type);
		when(client.getVarbitValue(PORT + off)).thenReturn(portId);
		when(client.getVarbitValue(MAXHP + (slot - 1))).thenReturn(maxHp);
	}

	@Test
	void noBoatsOwned_returnsEmpty()
	{
		// Mockito returns 0 for all unstubbed getVarbitValue -> nothing owned.
		assertFalse(resolver.resolveBestShipDock().isPresent());
	}

	@Test
	void singleOwnedBoat_returnsItsPortDock()
	{
		boat(1, 1, 3, 100, 0); // Port Sarim (id 0) = 3050,3192
		Optional<WorldPoint> dock = resolver.resolveBestShipDock();
		assertTrue(dock.isPresent());
		assertEquals(new WorldPoint(3050, 3192, 0), dock.get());
	}

	@Test
	void picksHighestTypeTier()
	{
		boat(1, 1, 3, 999, 0);  // lower tier, high hp, Port Sarim
		boat(2, 1, 7, 10, 7);   // higher tier, low hp, Port Piscarilius (id 7) = 1845,3687
		Optional<WorldPoint> dock = resolver.resolveBestShipDock();
		assertTrue(dock.isPresent());
		assertEquals(new WorldPoint(1845, 3687, 0), dock.get());
	}

	@Test
	void tieOnType_breaksByHigherMaxHp()
	{
		boat(1, 1, 5, 100, 0);   // Port Sarim
		boat(2, 1, 5, 200, 10);  // same tier, higher hp, Port Khazard (id 10) = 2685,3161
		Optional<WorldPoint> dock = resolver.resolveBestShipDock();
		assertTrue(dock.isPresent());
		assertEquals(new WorldPoint(2685, 3161, 0), dock.get());
	}

	@Test
	void unknownPortIndex_returnsEmpty()
	{
		boat(1, 1, 5, 100, 999); // no such port
		assertFalse(resolver.resolveBestShipDock().isPresent());
	}

	@Test
	void firstStepOverride_appliesToArriveAtAnchorWhenBoatOwned()
	{
		boat(1, 1, 5, 100, 10); // Port Khazard
		GuidanceStep step = arriveStep(1824, 3691, 0);
		Optional<WorldPoint> override = resolver.resolveFirstStepOverride(step, 0);
		assertTrue(override.isPresent());
		assertEquals(new WorldPoint(2685, 3161, 0), override.get());
	}

	@Test
	void firstStepOverride_ignoresNonZeroIndex()
	{
		boat(1, 1, 5, 100, 10);
		assertFalse(resolver.resolveFirstStepOverride(arriveStep(1824, 3691, 0), 1).isPresent());
	}

	@Test
	void firstStepOverride_ignoresNonAnchorTile()
	{
		boat(1, 1, 5, 100, 10);
		assertFalse(resolver.resolveFirstStepOverride(arriveStep(3000, 3000, 0), 0).isPresent());
	}

	@Test
	void firstStepOverride_ignoresNonArriveCondition()
	{
		boat(1, 1, 5, 100, 10);
		GuidanceStep manual = step(1824, 3691, 0, CompletionCondition.MANUAL);
		assertFalse(resolver.resolveFirstStepOverride(manual, 0).isPresent());
	}

	@Test
	void firstStepOverride_anchorButNoBoat_returnsEmpty()
	{
		// no boats stubbed
		assertFalse(resolver.resolveFirstStepOverride(arriveStep(1824, 3691, 0), 0).isPresent());
	}

	private static GuidanceStep arriveStep(int x, int y, int plane)
	{
		return step(x, y, plane, CompletionCondition.ARRIVE_AT_TILE);
	}

	private static GuidanceStep step(int x, int y, int plane, CompletionCondition condition)
	{
		return new GuidanceStep(
			"Sailing step",
			null, x, y, plane,
			0, null, null, null, null, null, null, null, null,
			condition,
			0, 0, 0, 0, null, null,
			0, null, null, null, null, null,
			0, 0, false, 0, null, null, 0, 0, null, null, null, null, null,
			null, null,
			null,
			null,
			null,
			null);
	}
}
