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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Programmatic status glyphs rendered with Java2D, sharing the drawn-icon
 * approach established by {@link StepControlIcons} (#547). Drawing at runtime
 * avoids bundling third-party icon-set PNGs and lets the colour be themed per
 * state without keeping multiple PNG variants in sync.
 *
 * <p>Provides:
 * <ul>
 *   <li>collapse chevrons ({@link #chevronRight(Color)} / {@link #chevronDown(Color)})
 *       for collapsible-section toggles;</li>
 *   <li>a solid status dot ({@link #statusDot(Color)}) for the sync-status
 *       indicator.</li>
 * </ul>
 *
 * <p>Each factory returns a freshly-allocated {@link BufferedImage}.
 */
final class StatusIcons
{
	/** Standard glyph canvas size - small enough to sit inline with small-font labels. */
	static final int GLYPH_SIZE = 14;

	private StatusIcons()
	{
	}

	/**
	 * Right-pointing chevron - the collapsed-state affordance for a collapsible
	 * toggle. Replaces an ASCII right-triangle while keeping the same visual cue.
	 */
	static BufferedImage chevronRight(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			Path2D.Float tri = new Path2D.Float();
			tri.moveTo(4.5f, 3f);
			tri.lineTo(10f, 7f);
			tri.lineTo(4.5f, 11f);
			tri.closePath();
			g.fill(tri);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Down-pointing chevron - the expanded-state affordance for a collapsible
	 * toggle.
	 */
	static BufferedImage chevronDown(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			Path2D.Float tri = new Path2D.Float();
			tri.moveTo(3f, 4.5f);
			tri.lineTo(7f, 10f);
			tri.lineTo(11f, 4.5f);
			tri.closePath();
			g.fill(tri);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	/**
	 * Solid status dot used by the sync-status indicator. The caller supplies the
	 * state colour (amber = not synced, cyan = syncing, green = synced).
	 */
	static BufferedImage statusDot(Color color)
	{
		BufferedImage img = blank();
		Graphics2D g = prepare(img, color);
		try
		{
			g.fillOval(2, 2, 10, 10);
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
