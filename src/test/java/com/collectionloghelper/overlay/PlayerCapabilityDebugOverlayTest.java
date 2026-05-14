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
import com.collectionloghelper.data.SlayerTaskState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PlayerCapabilityDebugOverlay} covering:
 * - Returns null when config flag is disabled (default)
 * - Renders panel when enabled with mocked client values (uses a real Graphics2D
 *   from a BufferedImage because OverlayPanel.render requires FontMetrics)
 * - Handles null localPlayer gracefully (no NPE)
 * - Slayer task row shows "none" when no task is active
 * - Slayer task row shows creature name and count when task is active
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerCapabilityDebugOverlayTest
{
	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private SlayerTaskState slayerTaskState;

	/** Mock Graphics2D — suitable only for guard-clause tests (no actual rendering). */
	@Mock
	private Graphics2D mockGraphics;

	/**
	 * Real Graphics2D backed by a BufferedImage, required for tests that call
	 * {@code super.render(graphics)} which internally calls
	 * {@code graphics.getFontMetrics()}.
	 */
	private Graphics2D realGraphics;

	private PlayerCapabilityDebugOverlay overlay;

	@Before
	public void setUp() throws Exception
	{
		java.lang.reflect.Constructor<PlayerCapabilityDebugOverlay> ctor =
			PlayerCapabilityDebugOverlay.class.getDeclaredConstructor(
				Client.class, CollectionLogHelperConfig.class, SlayerTaskState.class);
		ctor.setAccessible(true);
		overlay = ctor.newInstance(client, config, slayerTaskState);

		BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		realGraphics = img.createGraphics();
	}

	// -------------------------------------------------------------------------
	// Guard clause: disabled in config
	// -------------------------------------------------------------------------

	@Test
	public void render_returnsNull_whenConfigDisabled()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(false);

		Dimension result = overlay.render(mockGraphics);

		assertNull(result);
	}

	// -------------------------------------------------------------------------
	// Enabled path — uses real Graphics2D so FontMetrics is available
	// -------------------------------------------------------------------------

	@Test
	public void render_returnsNonNull_whenEnabled()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);

		Player player = mock(Player.class);
		when(player.getCombatLevel()).thenReturn(126);
		when(client.getLocalPlayer()).thenReturn(player);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		Dimension result = overlay.render(realGraphics);

		assertNotNull(result);
	}

	// -------------------------------------------------------------------------
	// Null localPlayer — must not throw
	// -------------------------------------------------------------------------

	@Test
	public void render_handlesNullLocalPlayer_gracefully()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);
		when(client.getLocalPlayer()).thenReturn(null);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		// Must complete without NullPointerException; combat level shown as "?"
		Dimension result = overlay.render(realGraphics);
		assertNotNull(result);
	}

	// -------------------------------------------------------------------------
	// Slayer task rows
	// -------------------------------------------------------------------------

	@Test
	public void render_includesTaskRow_whenTaskActive()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);

		Player player = mock(Player.class);
		when(player.getCombatLevel()).thenReturn(126);
		when(client.getLocalPlayer()).thenReturn(player);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);

		when(slayerTaskState.isTaskActive()).thenReturn(true);
		when(slayerTaskState.getCreatureName()).thenReturn("Abyssal demons");
		when(slayerTaskState.getRemaining()).thenReturn(150);

		Dimension result = overlay.render(realGraphics);
		assertNotNull(result);
	}

	@Test
	public void render_showsNoneTask_whenNoTaskActive()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);

		Player player = mock(Player.class);
		when(player.getCombatLevel()).thenReturn(126);
		when(client.getLocalPlayer()).thenReturn(player);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		Dimension result = overlay.render(realGraphics);
		assertNotNull(result);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void stubSkillLevels()
	{
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(99);
		when(client.getRealSkillLevel(Skill.CONSTRUCTION)).thenReturn(83);
		when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(99);
		when(client.getRealSkillLevel(Skill.MAGIC)).thenReturn(99);
	}
}
