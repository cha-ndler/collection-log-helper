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
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;

/**
 * Shared floating "collection-log target" marker for in-world guidance overlays.
 *
 * <p>The previous implementation (#714) drew the panel's 16x16 cyan-on-transparent
 * book PNG resized to 18px directly on top of the target. At that size the PNG is
 * illegible against the game world - it reads as a blue blob with a line - and
 * sitting on the target rather than above it hid the object. This helper instead
 * draws a clean, programmatic book glyph (the same Java2D drawn-icon approach used
 * by the panel's status/step icons) and projects it ABOVE the target with a
 * vertical pixel offset, quest-helper style.
 *
 * <p>The glyph is rasterised once into a {@link BufferedImage} at construction so
 * the render hot-path performs no allocation and no IO - it only projects a point
 * and blits the cached image.
 */
final class GuidanceTargetMarker
{
	/** Glyph canvas size in pixels - large enough to be legible as a small book. */
	private static final int SIZE = 26;

	/** Pixels to lift the glyph above the target's canvas point so it floats clear. */
	private static final int HEIGHT_OFFSET = 80;

	private static final Color BOOK_FILL = new Color(0xE8, 0xC4, 0x6A);
	private static final Color BOOK_OUTLINE = Color.BLACK;
	private static final Color PAGE_LINE = new Color(0x3A, 0x2A, 0x10);

	private final BufferedImage glyph;

	GuidanceTargetMarker()
	{
		this.glyph = buildGlyph();
	}

	/**
	 * Projects the target's local point to the canvas and blits the cached glyph
	 * centred horizontally and floating {@link #HEIGHT_OFFSET} pixels above it.
	 * No-op when the point cannot be projected. Allocates nothing.
	 */
	void draw(Graphics2D graphics, Client client, LocalPoint localPoint)
	{
		if (localPoint == null)
		{
			return;
		}
		Point canvasPoint = Perspective.getCanvasImageLocation(client, localPoint, glyph, HEIGHT_OFFSET);
		if (canvasPoint != null)
		{
			graphics.drawImage(glyph, canvasPoint.getX(), canvasPoint.getY(), null);
		}
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
