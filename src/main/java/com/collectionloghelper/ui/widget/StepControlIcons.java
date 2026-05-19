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
package com.collectionloghelper.ui.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Programmatic media-control style glyphs rendered with Java2D for the
 * active-guidance step-control button row (#547).
 *
 * <p>Glyphs are drawn at runtime instead of shipped as PNG assets so that:
 * <ul>
 *   <li>no third-party icon-set license needs to ride along with the plugin
 *       jar (Material Icons / Feather / etc.);</li>
 *   <li>colour can be themed without keeping multiple PNG variants in sync;</li>
 *   <li>the icon row scales cleanly with the existing button preferred-size
 *       contract (28x28 click target, 16x16 glyph inside).</li>
 * </ul>
 *
 * <p>Each public factory returns a freshly-allocated {@link BufferedImage}.
 * Two passes (idle / hover) are typically prepared once at construction time
 * by the caller and swapped via {@link javax.swing.JButton#setIcon}.
 */
final class StepControlIcons
{
	/** Standard glyph canvas size — matches the 16x16 sprite slot used by the row. */
	static final int GLYPH_SIZE = 16;

	private StepControlIcons()
	{
	}

	/**
	 * Skip-next glyph — a right-pointing triangle followed by a thin vertical bar,
	 * matching the familiar media-player "next track" symbol. Used for "Skip to
	 * next step".
	 */
	static BufferedImage skipNext(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			Path2D.Float tri = new Path2D.Float();
			tri.moveTo(3f, 3f);
			tri.lineTo(3f, 13f);
			tri.lineTo(11f, 8f);
			tri.closePath();
			g.fill(tri);
			// Trailing vertical bar
			g.fillRect(12, 3, 2, 10);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Stop glyph — a filled square. Used for "Stop guidance — clear all overlays".
	 */
	static BufferedImage stop(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			g.fillRect(3, 3, 10, 10);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Restart glyph — circular arrow pointing back to its start position.
	 * Used for "Restart this task from the first step".
	 */
	static BufferedImage restart(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			// Open arc, leaving a gap at the top-right for the arrowhead
			g.drawArc(3, 3, 10, 10, 60, 270);
			// Arrowhead at the open end of the arc
			Path2D.Float head = new Path2D.Float();
			head.moveTo(8f, 1f);
			head.lineTo(11.5f, 4f);
			head.lineTo(8f, 6f);
			head.closePath();
			g.fill(head);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Sync glyph — two opposing curved arrows in a closed loop, distinct from
	 * the single-arrow {@link #restart(Color)} glyph. Used for "Sync collection-log
	 * state".
	 */
	static BufferedImage sync(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			// Two opposing arcs to suggest a bidirectional refresh
			g.drawArc(3, 3, 10, 10, 20, 150);
			g.drawArc(3, 3, 10, 10, 200, 150);
			// Top-right arrowhead pointing down-right
			Path2D.Float h1 = new Path2D.Float();
			h1.moveTo(13f, 2.5f);
			h1.lineTo(13.5f, 6.5f);
			h1.lineTo(10f, 4.5f);
			h1.closePath();
			g.fill(h1);
			// Bottom-left arrowhead pointing up-left
			Path2D.Float h2 = new Path2D.Float();
			h2.moveTo(3f, 13.5f);
			h2.lineTo(2.5f, 9.5f);
			h2.lineTo(6f, 11.5f);
			h2.closePath();
			g.fill(h2);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	private static BufferedImage blank()
	{
		return new BufferedImage(GLYPH_SIZE, GLYPH_SIZE, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D prepare(BufferedImage img, Color color)
	{
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setColor(color);
		return g;
	}
}
