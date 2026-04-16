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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GroundObject;
import net.runelite.api.WallObject;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SceneObjectRouterTest
{
	@Mock
	private ObjectHighlightOverlay objectHighlightOverlay;

	@Mock
	private WallObject wallObject;

	@Mock
	private DecorativeObject decorativeObject;

	@Mock
	private GroundObject groundObject;

	private SceneObjectRouter router;

	@Before
	public void setUp()
	{
		router = new SceneObjectRouter(objectHighlightOverlay);
	}

	// ========================================================================
	// WallObject routing
	// ========================================================================

	@Test
	public void testOnWallObjectSpawnedForwardsToOverlay()
	{
		// Arrange
		WallObjectSpawned event = new WallObjectSpawned();
		event.setWallObject(wallObject);

		// Act
		router.onWallObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectSpawned(wallObject);
	}

	@Test
	public void testOnWallObjectDespawnedForwardsToOverlay()
	{
		// Arrange
		WallObjectDespawned event = new WallObjectDespawned();
		event.setWallObject(wallObject);

		// Act
		router.onWallObjectDespawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectDespawned(wallObject);
	}

	@Test
	public void testOnWallObjectSpawnedDoesNotForwardDecorativeOrGround()
	{
		// Arrange
		WallObjectSpawned event = new WallObjectSpawned();
		event.setWallObject(wallObject);

		// Act
		router.onWallObjectSpawned(event);

		// Assert — no calls with decorative or ground object
		verify(objectHighlightOverlay, never()).onObjectSpawned(decorativeObject);
		verify(objectHighlightOverlay, never()).onObjectSpawned(groundObject);
	}

	// ========================================================================
	// DecorativeObject routing
	// ========================================================================

	@Test
	public void testOnDecorativeObjectSpawnedForwardsToOverlay()
	{
		// Arrange
		DecorativeObjectSpawned event = new DecorativeObjectSpawned();
		event.setDecorativeObject(decorativeObject);

		// Act
		router.onDecorativeObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectSpawned(decorativeObject);
	}

	@Test
	public void testOnDecorativeObjectDespawnedForwardsToOverlay()
	{
		// Arrange
		DecorativeObjectDespawned event = new DecorativeObjectDespawned();
		event.setDecorativeObject(decorativeObject);

		// Act
		router.onDecorativeObjectDespawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectDespawned(decorativeObject);
	}

	@Test
	public void testOnDecorativeObjectSpawnedDoesNotForwardWallOrGround()
	{
		// Arrange
		DecorativeObjectSpawned event = new DecorativeObjectSpawned();
		event.setDecorativeObject(decorativeObject);

		// Act
		router.onDecorativeObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay, never()).onObjectSpawned(wallObject);
		verify(objectHighlightOverlay, never()).onObjectSpawned(groundObject);
	}

	// ========================================================================
	// GroundObject routing
	// ========================================================================

	@Test
	public void testOnGroundObjectSpawnedForwardsToOverlay()
	{
		// Arrange
		GroundObjectSpawned event = new GroundObjectSpawned();
		event.setGroundObject(groundObject);

		// Act
		router.onGroundObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectSpawned(groundObject);
	}

	@Test
	public void testOnGroundObjectDespawnedForwardsToOverlay()
	{
		// Arrange
		GroundObjectDespawned event = new GroundObjectDespawned();
		event.setGroundObject(groundObject);

		// Act
		router.onGroundObjectDespawned(event);

		// Assert
		verify(objectHighlightOverlay).onObjectDespawned(groundObject);
	}

	@Test
	public void testOnGroundObjectSpawnedDoesNotForwardWallOrDecorative()
	{
		// Arrange
		GroundObjectSpawned event = new GroundObjectSpawned();
		event.setGroundObject(groundObject);

		// Act
		router.onGroundObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay, never()).onObjectSpawned(wallObject);
		verify(objectHighlightOverlay, never()).onObjectSpawned(decorativeObject);
	}

	// ========================================================================
	// No cross-contamination between spawn and despawn
	// ========================================================================

	@Test
	public void testSpawnEventDoesNotTriggerDespawn()
	{
		// Arrange
		WallObjectSpawned event = new WallObjectSpawned();
		event.setWallObject(wallObject);

		// Act
		router.onWallObjectSpawned(event);

		// Assert
		verify(objectHighlightOverlay, never()).onObjectDespawned(any());
	}

	@Test
	public void testDespawnEventDoesNotTriggerSpawn()
	{
		// Arrange
		WallObjectDespawned event = new WallObjectDespawned();
		event.setWallObject(wallObject);

		// Act
		router.onWallObjectDespawned(event);

		// Assert
		verify(objectHighlightOverlay, never()).onObjectSpawned(any());
	}

	// ========================================================================
	// Multiple events in sequence
	// ========================================================================

	@Test
	public void testMultipleSpawnEventsForwardInOrder()
	{
		// Arrange
		WallObjectSpawned wallEvent = new WallObjectSpawned();
		wallEvent.setWallObject(wallObject);
		DecorativeObjectSpawned decEvent = new DecorativeObjectSpawned();
		decEvent.setDecorativeObject(decorativeObject);

		// Act
		router.onWallObjectSpawned(wallEvent);
		router.onDecorativeObjectSpawned(decEvent);

		// Assert — both forwarded independently
		verify(objectHighlightOverlay, times(1)).onObjectSpawned(wallObject);
		verify(objectHighlightOverlay, times(1)).onObjectSpawned(decorativeObject);
	}

	@Test
	public void testSpawnAndDespawnPairForwardsBoth()
	{
		// Arrange
		GroundObjectSpawned spawnEvent = new GroundObjectSpawned();
		spawnEvent.setGroundObject(groundObject);
		GroundObjectDespawned despawnEvent = new GroundObjectDespawned();
		despawnEvent.setGroundObject(groundObject);

		// Act
		router.onGroundObjectSpawned(spawnEvent);
		router.onGroundObjectDespawned(despawnEvent);

		// Assert
		verify(objectHighlightOverlay, times(1)).onObjectSpawned(groundObject);
		verify(objectHighlightOverlay, times(1)).onObjectDespawned(groundObject);
	}
}
