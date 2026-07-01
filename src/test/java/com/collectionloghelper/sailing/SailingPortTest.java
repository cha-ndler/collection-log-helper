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

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SailingPortTest
{
	@Test
	void portIdsAreUnique()
	{
		Set<Integer> ids = new HashSet<>();
		for (SailingPort p : SailingPort.values())
		{
			assertTrue(ids.add(p.getPortId()), "duplicate port id: " + p.getPortId() + " (" + p + ")");
		}
	}

	@Test
	void allDocksArePositivePlaneZeroTiles()
	{
		for (SailingPort p : SailingPort.values())
		{
			WorldPoint d = p.getDock();
			assertTrue(d.getX() > 0 && d.getY() > 0, p + " has non-positive dock coords");
			assertEquals(0, d.getPlane(), p + " dock should be on plane 0");
		}
	}

	@Test
	void fromId_mapsKnownPorts()
	{
		assertEquals(SailingPort.PORT_SARIM, SailingPort.fromId(0).orElse(null));
		assertEquals(SailingPort.PORT_PISCARILIUS, SailingPort.fromId(7).orElse(null));
		assertEquals(SailingPort.PORT_KHAZARD, SailingPort.fromId(10).orElse(null));
		assertEquals(SailingPort.GRIMSTONE, SailingPort.fromId(58).orElse(null));
	}

	@Test
	void fromId_unknownIsEmpty()
	{
		assertFalse(SailingPort.fromId(-1).isPresent());
		assertFalse(SailingPort.fromId(999).isPresent());
	}

	@Test
	void seaTreasureFragmentIslandsAreMapped()
	{
		// The 7 named fragment islands the Sea Treasures follow-up will anchor to.
		assertEquals(new WorldPoint(3061, 2639, 0), SailingPort.DOGNOSE_ISLAND.getDock());
		assertEquals(new WorldPoint(2997, 2288, 0), SailingPort.THE_ONYX_CREST.getDock());
		assertEquals(new WorldPoint(1872, 2985, 0), SailingPort.VATRACHOS_ISLAND.getDock());
		assertEquals(new WorldPoint(1860, 3306, 0), SailingPort.PORT_ROBERTS.getDock());
		assertEquals(new WorldPoint(1557, 2771, 0), SailingPort.SHIMMERING_ATOLL.getDock());
		assertEquals(new WorldPoint(2532, 2531, 0), SailingPort.ISLE_OF_BONES.getDock());
		assertEquals(new WorldPoint(2058, 2606, 0), SailingPort.WINTUMBER_ISLAND.getDock());
	}
}
