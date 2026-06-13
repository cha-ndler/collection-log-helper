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
package com.collectionloghelper.overlay;

import net.runelite.api.coords.WorldPoint;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RenderStateRecorderTest
{
	private RenderStateRecorder recorder;

	@BeforeEach
	public void setUp()
	{
		recorder = new RenderStateRecorder();
	}

	@Test
	public void disabledByDefaultLatestStaysEmptySentinel()
	{
		assertFalse(recorder.isEnabled());

		RenderStateRecorder.Snapshot before = recorder.latest();
		assertEquals(-1, before.snapshotTick);
		assertFalse(before.guidanceActive);

		// Every record/begin/publish is a no-op while disabled.
		recorder.beginFrame(true);
		recorder.recordNpc(123);
		recorder.recordObject(456);
		recorder.recordGroundItem(789);
		recorder.recordTarget("npc", 123, true, 90);
		recorder.recordTile(new WorldPoint(1, 2, 0));
		recorder.recordTooltip("hello");
		recorder.publish(42);

		// latest() is unchanged: same sentinel reference, still tick -1, empty arrays.
		assertSame(before, recorder.latest());
		assertEquals(-1, recorder.latest().snapshotTick);
		assertEquals(0, recorder.latest().highlightedNpcIds.length);
		assertTrue(recorder.latest().targets.isEmpty());
	}

	@Test
	public void enabledRoundTripsRecordedFrameIntoLatest()
	{
		recorder.enable();
		assertTrue(recorder.isEnabled());

		recorder.beginFrame(true);
		recorder.recordNpc(123);
		recorder.recordObject(456);
		recorder.recordGroundItem(789);
		recorder.recordTarget("npc", 123, false, 90);
		recorder.recordTarget("object", 456, true, 80);
		recorder.recordTile(new WorldPoint(3200, 3200, 1));
		recorder.recordTooltip("Open the chest");
		recorder.publish(99);

		RenderStateRecorder.Snapshot s = recorder.latest();
		assertEquals(99, s.snapshotTick);
		assertTrue(s.guidanceActive);
		assertArrayEquals(new int[]{123}, s.highlightedNpcIds);
		assertArrayEquals(new int[]{456}, s.highlightedObjectIds);
		assertArrayEquals(new int[]{789}, s.highlightedGroundItemIds);
		assertEquals(2, s.targets.size());
		assertEquals("npc", s.targets.get(0).type);
		assertFalse(s.targets.get(0).bookMarker);
		assertEquals(90, s.targets.get(0).zOffset);
		assertEquals("object", s.targets.get(1).type);
		assertTrue(s.targets.get(1).bookMarker);
		assertEquals(80, s.targets.get(1).zOffset);
		assertEquals(3200, s.tileTarget.getX());
		assertEquals(1, s.tileTarget.getPlane());
		assertEquals("Open the chest", s.tooltipText);
	}

	@Test
	public void publishedSnapshotIsImmutableAcrossASubsequentFrame()
	{
		recorder.enable();

		recorder.beginFrame(true);
		recorder.recordNpc(111);
		recorder.recordTarget("npc", 111, true, 90);
		recorder.recordTile(new WorldPoint(1, 1, 0));
		recorder.recordTooltip("first");
		recorder.publish(10);
		RenderStateRecorder.Snapshot first = recorder.latest();

		// A whole second frame must not mutate the first snapshot.
		recorder.beginFrame(false);
		recorder.recordObject(222);
		recorder.recordTarget("object", 222, true, 80);
		recorder.publish(20);
		RenderStateRecorder.Snapshot second = recorder.latest();

		assertNotSame(first, second);

		// First snapshot is frozen at its original values.
		assertEquals(10, first.snapshotTick);
		assertTrue(first.guidanceActive);
		assertArrayEquals(new int[]{111}, first.highlightedNpcIds);
		assertEquals(1, first.targets.size());
		assertEquals("npc", first.targets.get(0).type);
		assertEquals("first", first.tooltipText);

		// Second snapshot reflects only the second frame (tile/tooltip cleared by beginFrame).
		assertEquals(20, second.snapshotTick);
		assertFalse(second.guidanceActive);
		assertEquals(0, second.highlightedNpcIds.length);
		assertArrayEquals(new int[]{222}, second.highlightedObjectIds);
		assertNull(second.tileTarget);
		assertNull(second.tooltipText);
	}

	@Test
	public void disableResetsLatestToSentinel()
	{
		recorder.enable();
		recorder.beginFrame(true);
		recorder.recordNpc(5);
		recorder.publish(7);
		assertEquals(7, recorder.latest().snapshotTick);

		recorder.disable();
		assertFalse(recorder.isEnabled());
		assertEquals(-1, recorder.latest().snapshotTick);
	}
}
