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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Value-type tests for {@link StepWaypoint}: field accessors, equals, hashCode, toString.
 *
 * <p>Lombok {@code @Value} generates equals/hashCode from all fields; these tests
 * verify that contract so regressions in the generated code surface immediately.
 */
public class StepWaypointTest
{
	// ── Construction and field access ─────────────────────────────────────────

	@Test
	public void constructor_storesAllFields()
	{
		StepWaypoint wp = new StepWaypoint(1435, 9700, 0, 10);

		assertEquals(1435, wp.getWorldX());
		assertEquals(9700, wp.getWorldY());
		assertEquals(0, wp.getPlane());
		assertEquals(10, wp.getRadius());
	}

	@Test
	public void constructor_upperFloor_planeStoredCorrectly()
	{
		StepWaypoint wp = new StepWaypoint(3200, 3400, 2, 5);

		assertEquals(2, wp.getPlane());
	}

	// ── equals ────────────────────────────────────────────────────────────────

	@Test
	public void equals_identicalValues_returnsTrue()
	{
		StepWaypoint a = new StepWaypoint(1435, 9700, 0, 10);
		StepWaypoint b = new StepWaypoint(1435, 9700, 0, 10);

		assertEquals(a, b);
	}

	@Test
	public void equals_differentWorldX_returnsFalse()
	{
		StepWaypoint a = new StepWaypoint(1435, 9700, 0, 10);
		StepWaypoint b = new StepWaypoint(1436, 9700, 0, 10);

		assertNotEquals(a, b);
	}

	@Test
	public void equals_differentRadius_returnsFalse()
	{
		StepWaypoint a = new StepWaypoint(1435, 9700, 0, 10);
		StepWaypoint b = new StepWaypoint(1435, 9700, 0, 20);

		assertNotEquals(a, b);
	}

	@Test
	public void equals_differentPlane_returnsFalse()
	{
		StepWaypoint a = new StepWaypoint(1435, 9700, 0, 10);
		StepWaypoint b = new StepWaypoint(1435, 9700, 1, 10);

		assertNotEquals(a, b);
	}

	// ── hashCode ──────────────────────────────────────────────────────────────

	@Test
	public void hashCode_identicalValues_match()
	{
		StepWaypoint a = new StepWaypoint(1435, 9700, 0, 10);
		StepWaypoint b = new StepWaypoint(1435, 9700, 0, 10);

		assertEquals(a.hashCode(), b.hashCode());
	}

	// ── toString ──────────────────────────────────────────────────────────────

	@Test
	public void toString_containsAllFieldValues()
	{
		StepWaypoint wp = new StepWaypoint(1435, 9700, 0, 10);
		String s = wp.toString();

		// Lombok @Value toString format: StepWaypoint(worldX=1435, worldY=9700, plane=0, radius=10)
		assertEquals(true, s.contains("1435"));
		assertEquals(true, s.contains("9700"));
		assertEquals(true, s.contains("10"));
	}
}
