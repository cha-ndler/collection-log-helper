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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.overlay;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import com.collectionloghelper.CollectionLogHelperConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies the floating guidance target marker glyph is rasterised once at
 * construction (the render hot-path must not perform IO or allocation). The
 * earlier implementation (#714) blitted the 16x16 panel book PNG resized to
 * 18px, which read as an illegible blue blob on the target. The marker is now a
 * programmatically-drawn book glyph that floats above the target. The in-world
 * projection cannot be exercised headless, so these tests confirm the glyph
 * builds, has positive dimensions, and that both overlays hold a marker
 * instance.
 */
public class GuidanceTargetMarkerTest
{
	@Test
	public void glyphIsBuiltWithPositiveDimensions()
	{
		GuidanceTargetMarker marker = new GuidanceTargetMarker();
		BufferedImage glyph = readGlyph(marker);
		assertNotNull(glyph, "marker glyph should be rasterised at construction");
		assertTrue(glyph.getWidth() > 0, "glyph width should be positive");
		assertTrue(glyph.getHeight() > 0, "glyph height should be positive");
	}

	@Test
	public void drawWithNullLocalPointIsNoOp()
	{
		GuidanceTargetMarker marker = new GuidanceTargetMarker();
		Client client = mock(Client.class);
		BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas.createGraphics();
		try
		{
			assertDoesNotThrow(() -> marker.draw(g, client, null));
		}
		finally
		{
			g.dispose();
		}
	}

	@Test
	public void objectOverlayHoldsMarkerInstance() throws Exception
	{
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);

		Constructor<ObjectHighlightOverlay> ctor = ObjectHighlightOverlay.class
			.getDeclaredConstructor(Client.class, ClientThread.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		ObjectHighlightOverlay overlay = ctor.newInstance(client, clientThread, config);

		assertNotNull(readMarker(overlay), "object overlay should hold a target marker");
	}

	@Test
	public void groundItemOverlayHoldsMarkerInstance() throws Exception
	{
		Client client = mock(Client.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);

		Constructor<GroundItemHighlightOverlay> ctor = GroundItemHighlightOverlay.class
			.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		GroundItemHighlightOverlay overlay = ctor.newInstance(client, config);

		assertNotNull(readMarker(overlay), "ground-item overlay should hold a target marker");
	}

	private static BufferedImage readGlyph(GuidanceTargetMarker marker)
	{
		try
		{
			Field field = GuidanceTargetMarker.class.getDeclaredField("glyph");
			field.setAccessible(true);
			return (BufferedImage) field.get(marker);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static GuidanceTargetMarker readMarker(Object overlay) throws Exception
	{
		Field field = overlay.getClass().getDeclaredField("targetMarker");
		field.setAccessible(true);
		return (GuidanceTargetMarker) field.get(overlay);
	}
}
