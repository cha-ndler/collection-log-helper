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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Shared floating "collection-log target" marker for in-world guidance overlays.
 *
 * <p>Renders the real OSRS collection-log book item sprite
 * ({@link ItemID#COLLECTION_LOG}) floating ABOVE the target with a vertical pixel
 * offset, quest-helper style — the same sprite the world-map destination overlay
 * uses, for visual consistency (#720). The sprite is fetched once from
 * {@link ItemManager} and cached; because {@code getImage} returns an
 * {@link AsyncBufferedImage} that fills in on a later client tick, a programmatic
 * book glyph is drawn as a fallback until the sprite has loaded (and remains the
 * marker if no {@link ItemManager} is available).
 *
 * <p>Both the glyph and the resolved sprite are cached, so the render hot-path
 * performs no allocation and no IO - it only projects a point and blits an image.
 */
final class GuidanceTargetMarker
{
	/** Glyph canvas size in pixels - large enough to be legible as a small book. */
	private static final int SIZE = 26;

	/** Pixels to lift the marker above the target's canvas point so it floats clear. */
	private static final int HEIGHT_OFFSET = 80;

	private static final Color BOOK_FILL = new Color(0xE8, 0xC4, 0x6A);
	private static final Color BOOK_OUTLINE = Color.BLACK;
	private static final Color PAGE_LINE = new Color(0x3A, 0x2A, 0x10);

	@Nullable
	private final ItemManager itemManager;

	/** Programmatic fallback glyph, drawn until the real item sprite has loaded. */
	private final BufferedImage glyph;

	/** Real collection-log book sprite, lazily resolved from {@link ItemManager}. */
	@Nullable
	private BufferedImage sprite;

	/** True once {@link #sprite} has finished loading and is safe to blit. */
	private volatile boolean spriteReady;

	GuidanceTargetMarker(@Nullable ItemManager itemManager)
	{
		this.itemManager = itemManager;
		this.glyph = buildGlyph();
	}

	/**
	 * Projects the target's local point to the canvas and blits the marker centred
	 * horizontally and floating {@link #HEIGHT_OFFSET} pixels above it. Prefers the
	 * real collection-log sprite once loaded, falling back to the glyph meanwhile.
	 * No-op when the point cannot be projected. Allocates nothing on the hot path.
	 */
	void draw(Graphics2D graphics, Client client, LocalPoint localPoint)
	{
		if (localPoint == null)
		{
			return;
		}
		BufferedImage image = resolveImage();
		Point canvasPoint = Perspective.getCanvasImageLocation(client, localPoint, image, HEIGHT_OFFSET);
		if (canvasPoint != null)
		{
			graphics.drawImage(image, canvasPoint.getX(), canvasPoint.getY(), null);
		}
	}

	/**
	 * Returns the real collection-log sprite once it has loaded, otherwise the
	 * fallback glyph. The sprite request is made lazily on first draw and cached;
	 * {@link AsyncBufferedImage#onLoaded} flips {@link #spriteReady} when the pixels
	 * are ready so we never blit a half-loaded (transparent) image.
	 */
	private BufferedImage resolveImage()
	{
		if (spriteReady && sprite != null)
		{
			return sprite;
		}
		if (sprite == null && itemManager != null)
		{
			BufferedImage requested = itemManager.getImage(ItemID.COLLECTION_LOG);
			sprite = requested;
			if (requested instanceof AsyncBufferedImage)
			{
				((AsyncBufferedImage) requested).onLoaded(() -> spriteReady = true);
			}
			else if (requested != null)
			{
				spriteReady = true;
			}
		}
		return spriteReady && sprite != null ? sprite : glyph;
	}

	/**
	 * Rasterises a small open-book glyph: a rounded gold cover with a black outline
	 * and two page lines, plus a subtle drop shadow for contrast against bright or
	 * dark scenery. Drawn once at construction.
	 */
	private static BufferedImage buildGlyph()
	{
		BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			// Book body inset from the canvas edges, leaving room for the outline.
			final int x = 4;
			final int y = 5;
			final int w = SIZE - 8;
			final int h = SIZE - 11;

			// Drop shadow for contrast on light backgrounds.
			g.setColor(new Color(0, 0, 0, 90));
			g.fillRoundRect(x + 1, y + 2, w, h, 5, 5);

			// Cover fill.
			g.setColor(BOOK_FILL);
			g.fillRoundRect(x, y, w, h, 5, 5);

			// Outline.
			g.setColor(BOOK_OUTLINE);
			g.setStroke(new BasicStroke(1.5f));
			g.drawRoundRect(x, y, w, h, 5, 5);

			// Spine down the middle.
			int spineX = x + w / 2;
			g.drawLine(spineX, y + 2, spineX, y + h - 2);

			// Two short page lines per side to read as text/pages.
			g.setColor(PAGE_LINE);
			g.setStroke(new BasicStroke(1f));
			int leftStart = x + 3;
			int leftEnd = spineX - 3;
			int rightStart = spineX + 3;
			int rightEnd = x + w - 3;
			int line1Y = y + h / 3;
			int line2Y = y + (2 * h) / 3;
			g.drawLine(leftStart, line1Y, leftEnd, line1Y);
			g.drawLine(leftStart, line2Y, leftEnd, line2Y);
			g.drawLine(rightStart, line1Y, rightEnd, line1Y);
			g.drawLine(rightStart, line2Y, rightEnd, line2Y);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}
}
