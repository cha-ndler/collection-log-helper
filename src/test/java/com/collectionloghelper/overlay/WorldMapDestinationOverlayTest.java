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

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.api.worldmap.WorldMapData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorldMapDestinationOverlay} covering:
 * - Returns null and draws nothing when target is null (hidden state)
 * - Returns null and draws nothing when {@code showOverlays()} is false
 * - Returns null when the world map widget is not open
 * - Returns null when local player is null
 * - Calls drawImage when target is visible on the map (on-screen destination icon)
 * - Calls drawImage when target is off-screen (edge-snap arrow)
 * - clearTarget() hides the overlay
 * - Null icon type defaults to TILE without throwing
 */
@RunWith(MockitoJUnitRunner.class)
public class WorldMapDestinationOverlayTest
{
	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private Graphics2D graphics;

	@Mock
	private Widget mapWidget;

	@Mock
	private Player player;

	private WorldMapDestinationOverlay overlay;

	/** A realistic map bounds rectangle (600 x 500 at position 0,0). */
	private static final Rectangle MAP_BOUNDS = new Rectangle(0, 0, 600, 500);

	/** Player world location used across tests. */
	private static final WorldPoint PLAYER_WORLD = new WorldPoint(3200, 3200, 0);

	/**
	 * A target tile close to the player — at zoom 4.0 it projects inside MAP_BOUNDS
	 * (roughly ±5 tiles from center = ±20 px, well inside 600x500).
	 */
	private static final WorldPoint TARGET_ON_SCREEN = new WorldPoint(3205, 3205, 0);

	/**
	 * A target tile far from the player — at zoom 1.0 it projects outside MAP_BOUNDS
	 * (300 tiles away = 300 px from center, beyond the 300px half-width of MAP_BOUNDS).
	 */
	private static final WorldPoint TARGET_OFF_SCREEN = new WorldPoint(3500, 3500, 0);

	@Before
	public void setUp()
	{
		when(config.showOverlays()).thenReturn(true);
		when(mapWidget.getBounds()).thenReturn(MAP_BOUNDS);
		when(player.getWorldLocation()).thenReturn(PLAYER_WORLD);
		overlay = new WorldMapDestinationOverlay(client, config);
	}

	// -----------------------------------------------------------------------
	// Hidden / guard-clause checks
	// -----------------------------------------------------------------------

	@Test
	public void render_returnsNull_whenNoTargetSet()
	{
		// No target — render should skip all drawing
		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	@Test
	public void render_returnsNull_whenShowOverlaysFalse()
	{
		when(config.showOverlays()).thenReturn(false);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	@Test
	public void render_returnsNull_whenWorldMapWidgetAbsent()
	{
		when(client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)).thenReturn(null);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	@Test
	public void render_returnsNull_whenLocalPlayerNull()
	{
		when(client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)).thenReturn(mapWidget);
		when(client.getLocalPlayer()).thenReturn(null);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	@Test
	public void render_returnsNull_afterClearTarget()
	{
		// Set then immediately clear — should behave as if no target was set
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);
		overlay.clearTarget();

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	// -----------------------------------------------------------------------
	// On-screen rendering (destination icon drawn at target tile)
	// -----------------------------------------------------------------------

	@Test
	public void render_drawsDestinationIcon_whenTargetOnScreen()
	{
		// At zoom 4.0, target 5 tiles away projects ~20 px from center — inside MAP_BOUNDS
		wireFullSetup(TARGET_ON_SCREEN, 4.0f);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyDrew();
	}

	@Test
	public void render_drawsNpcIcon_forNpcStepType()
	{
		wireFullSetup(TARGET_ON_SCREEN, 4.0f);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.NPC);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyDrew();
	}

	@Test
	public void render_drawsObjectIcon_forObjectStepType()
	{
		wireFullSetup(TARGET_ON_SCREEN, 4.0f);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.OBJECT);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyDrew();
	}

	// -----------------------------------------------------------------------
	// Off-screen rendering (edge-snap arrow)
	// -----------------------------------------------------------------------

	@Test
	public void render_drawsArrow_whenTargetOffScreen()
	{
		// At zoom 1.0, target 300 tiles away projects 300 px from center.
		// MAP_BOUNDS is 600 wide so center is at 300 — projection lands at 600 or beyond.
		wireFullSetup(TARGET_OFF_SCREEN, 1.0f);
		overlay.setTarget(TARGET_OFF_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyDrew();
	}

	// -----------------------------------------------------------------------
	// mapPointActive: off-screen arrow suppressed when a WorldMapPoint is registered
	// -----------------------------------------------------------------------

	/**
	 * When {@link WorldMapDestinationOverlay#setMapPointActive(boolean)} is {@code true}
	 * and the target is off-screen, the overlay must NOT draw its own edge-snap arrow.
	 * The {@link CollectionLogWorldMapPoint}'s snap arrow handles direction in that case
	 * to prevent the duplicate arrow from #410.
	 */
	@Test
	public void render_noArrow_whenTargetOffScreenAndMapPointActive()
	{
		wireFullSetup(TARGET_OFF_SCREEN, 1.0f);
		overlay.setTarget(TARGET_OFF_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);
		overlay.setMapPointActive(true);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoDraw();
	}

	/**
	 * When {@link WorldMapDestinationOverlay#setMapPointActive(boolean)} is {@code true}
	 * but the target IS on-screen, the overlay must still draw the destination icon
	 * (the on-screen icon is distinct from the map point's badge, so no duplication).
	 */
	@Test
	public void render_drawsDestinationIcon_whenTargetOnScreenEvenIfMapPointActive()
	{
		wireFullSetup(TARGET_ON_SCREEN, 4.0f);
		overlay.setTarget(TARGET_ON_SCREEN, WorldMapDestinationOverlay.StepIconType.TILE);
		overlay.setMapPointActive(true);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyDrew();
	}

	// -----------------------------------------------------------------------
	// Null / default handling
	// -----------------------------------------------------------------------

	@Test
	public void setTarget_nullIconType_defaultsToTileWithoutThrowing()
	{
		// Passing null for icon type must not throw — defaults to TILE
		overlay.setTarget(TARGET_ON_SCREEN, null);

		wireFullSetup(TARGET_ON_SCREEN, 4.0f);

		Dimension result = overlay.render(graphics);
		// Must not throw; drawImage should be called (TILE icon rendered)
		assertNull(result);
		verifyDrew();
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/**
	 * Wires client mocks for the full render path: widget open, player present,
	 * world map initialised at the given zoom level centered on PLAYER_WORLD.
	 *
	 * <p>All mock objects are created before being passed to {@code thenReturn}
	 * to avoid the Mockito UnfinishedStubbingException that occurs when a mock
	 * is created (and immediately stubbed) inside a {@code thenReturn(...)} argument.
	 */
	private void wireFullSetup(WorldPoint target, float zoom)
	{
		when(client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)).thenReturn(mapWidget);
		when(client.getLocalPlayer()).thenReturn(player);

		// Build world map mock before passing it into thenReturn
		WorldMap worldMap = buildWorldMap(zoom);
		when(client.getWorldMap()).thenReturn(worldMap);
	}

	/**
	 * Builds a minimal WorldMap mock. {@code surfaceContainsPosition} always
	 * returns true so coordinate conversion proceeds for any tile.
	 *
	 * <p>The map center is set to PLAYER_WORLD so that TARGET_ON_SCREEN (5 tiles
	 * away) projects close to the widget center.
	 */
	private static WorldMap buildWorldMap(float zoom)
	{
		WorldMapData worldMapData = mock(WorldMapData.class);
		when(worldMapData.surfaceContainsPosition(anyInt(), anyInt())).thenReturn(true);

		WorldMap worldMap = mock(WorldMap.class);
		when(worldMap.getWorldMapData()).thenReturn(worldMapData);
		when(worldMap.getWorldMapZoom()).thenReturn(zoom);
		when(worldMap.getWorldMapPosition())
			.thenReturn(new Point(PLAYER_WORLD.getX(), PLAYER_WORLD.getY()));

		return worldMap;
	}

	/** Asserts that {@code graphics.drawImage} was called exactly once. */
	private void verifyDrew()
	{
		verify(graphics).drawImage(
			ArgumentMatchers.any(),
			anyInt(),
			anyInt(),
			ArgumentMatchers.isNull());
	}

	/** Asserts that {@code graphics.drawImage} was never called. */
	private void verifyNoDraw()
	{
		verify(graphics, never()).drawImage(
			ArgumentMatchers.any(),
			anyInt(),
			anyInt(),
			ArgumentMatchers.any());
	}
}
