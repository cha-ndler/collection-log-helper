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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjectHighlightOverlayNearestTest
{
	private static TileObject objectAt(int x, int y)
	{
		TileObject obj = mock(TileObject.class);
		when(obj.getLocalLocation()).thenReturn(new LocalPoint(x, y));
		return obj;
	}

	@Test
	void picksTheClosestInstanceToThePlayer()
	{
		TileObject far = objectAt(1000, 1000);
		TileObject near = objectAt(120, 100);
		TileObject mid = objectAt(400, 400);
		List<TileObject> objects = new ArrayList<>(List.of(far, mid, near));

		TileObject result = ObjectHighlightOverlay.nearestObject(objects, new LocalPoint(100, 100));

		assertSame(near, result, "should pick the instance with the smallest squared distance");
	}

	@Test
	void returnsNullForEmptyOrNullInput()
	{
		assertNull(ObjectHighlightOverlay.nearestObject(Collections.emptyList(), new LocalPoint(0, 0)));
		assertNull(ObjectHighlightOverlay.nearestObject(null, new LocalPoint(0, 0)));
	}

	@Test
	void fallsBackToFirstWhenPlayerLocationUnavailable()
	{
		TileObject first = objectAt(500, 500);
		TileObject second = objectAt(10, 10);
		List<TileObject> objects = List.of(first, second);

		// Null player location (e.g. mid-teleport) must still yield a drawable target.
		assertSame(first, ObjectHighlightOverlay.nearestObject(objects, null));
	}

	@Test
	void skipsObjectsWithNoLocalLocation()
	{
		TileObject noLoc = mock(TileObject.class);
		when(noLoc.getLocalLocation()).thenReturn(null);
		TileObject located = objectAt(300, 300);
		List<TileObject> objects = new ArrayList<>(List.of(noLoc, located));

		assertEquals(located, ObjectHighlightOverlay.nearestObject(objects, new LocalPoint(290, 290)));
	}
}
