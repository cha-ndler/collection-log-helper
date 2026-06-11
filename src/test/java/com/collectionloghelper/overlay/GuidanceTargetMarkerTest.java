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
import java.lang.reflect.Method;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import com.collectionloghelper.CollectionLogHelperConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies the floating guidance target marker. The marker now renders the real
 * collection-log book item sprite ({@code ItemID.COLLECTION_LOG}) once it loads
 * from {@link ItemManager} (#720), falling back to a programmatically-drawn book
 * glyph rasterised once at construction while the async sprite loads (or when no
 * {@link ItemManager} is available). The in-world projection cannot be exercised
 * headless, so these tests confirm the glyph builds, the sprite is preferred once
 * resolved, null local points are no-ops, and all three highlight overlays
 * (object, ground-item, and the NPC path in {@code GuidanceOverlay}) hold a
 * marker.
 */
public class GuidanceTargetMarkerTest
{
	@Test
	public void glyphIsBuiltWithPositiveDimensions()
	{
		GuidanceTargetMarker marker = new GuidanceTargetMarker(null);
		BufferedImage glyph = readGlyph(marker);
		assertNotNull(glyph, "marker glyph should be rasterised at construction");
		assertTrue(glyph.getWidth() > 0, "glyph width should be positive");
		assertTrue(glyph.getHeight() > 0, "glyph height should be positive");
	}

	@Test
	public void fallsBackToGlyphWhenNoItemManager()
	{
		GuidanceTargetMarker marker = new GuidanceTargetMarker(null);
		assertSame(readGlyph(marker), resolveImage(marker),
			"with no ItemManager the marker must render the fallback glyph");
	}

	@Test
	public void requestsCollectionLogSpriteAndShowsGlyphUntilLoaded()
	{
		// getImage returns an AsyncBufferedImage that has not finished loading, so
		// the marker keeps showing the glyph until the async onLoaded callback fires.
		AsyncBufferedImage sprite = mock(AsyncBufferedImage.class);
		ItemManager itemManager = mock(ItemManager.class);
		lenient().when(itemManager.getImage(anyInt())).thenReturn(sprite);

		GuidanceTargetMarker marker = new GuidanceTargetMarker(itemManager);
		assertSame(readGlyph(marker), resolveImage(marker),
			"glyph shown until the async sprite reports loaded");
		// The marker must request the real collection-log book sprite, not some other item.
		verify(itemManager).getImage(ItemID.COLLECTION_LOG);
	}

	@Test
	public void drawWithNullLocalPointIsNoOp()
	{
		GuidanceTargetMarker marker = new GuidanceTargetMarker(null);
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
		ItemManager itemManager = mock(ItemManager.class);

		Constructor<ObjectHighlightOverlay> ctor = ObjectHighlightOverlay.class.getDeclaredConstructor(
			Client.class, ClientThread.class, CollectionLogHelperConfig.class, ItemManager.class);
		ctor.setAccessible(true);
		ObjectHighlightOverlay overlay = ctor.newInstance(client, clientThread, config, itemManager);

		assertNotNull(readMarker(overlay), "object overlay should hold a target marker");
	}

	@Test
	public void groundItemOverlayHoldsMarkerInstance() throws Exception
	{
		Client client = mock(Client.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		ItemManager itemManager = mock(ItemManager.class);

		Constructor<GroundItemHighlightOverlay> ctor = GroundItemHighlightOverlay.class
			.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class, ItemManager.class);
		ctor.setAccessible(true);
		GroundItemHighlightOverlay overlay = ctor.newInstance(client, config, itemManager);

		assertNotNull(readMarker(overlay), "ground-item overlay should hold a target marker");
	}

	@Test
	public void guidanceOverlayHoldsMarkerInstance() throws Exception
	{
		// #802: NPC kill/talk targets must get the same floating book marker as
		// objects and ground items. The NPC highlight lives in GuidanceOverlay.
		Client client = mock(Client.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);
		ItemManager itemManager = mock(ItemManager.class);

		Constructor<GuidanceOverlay> ctor = GuidanceOverlay.class.getDeclaredConstructor(
			Client.class, CollectionLogHelperConfig.class, ItemManager.class);
		ctor.setAccessible(true);
		GuidanceOverlay overlay = ctor.newInstance(client, config, itemManager);

		assertNotNull(readMarker(overlay), "guidance (NPC) overlay should hold a target marker");
	}

	private static BufferedImage resolveImage(GuidanceTargetMarker marker)
	{
		try
		{
			Method m = GuidanceTargetMarker.class.getDeclaredMethod("resolveImage");
			m.setAccessible(true);
			return (BufferedImage) m.invoke(marker);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}
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
