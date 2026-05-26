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
package com.collectionloghelper.ui.preview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * Headless renderer for Swing components. Lays out and paints a component tree
 * to a {@link BufferedImage} without a native window, so the side panel can be
 * screenshotted from a test or background job. Test-scoped only.
 */
public final class PanelSnapshot
{
	private static final Color BACKGROUND = new Color(40, 40, 40);
	private static final int MEASURE_HEIGHT = 4000;

	private PanelSnapshot()
	{
	}

	/**
	 * Lays out {@code panel} at the given width and paints it to an ARGB image
	 * sized to the panel's preferred height.
	 */
	public static BufferedImage render(JComponent panel, int width)
	{
		// First pass: give it generous height so preferred-size resolves.
		panel.setSize(width, MEASURE_HEIGHT);
		forceLayout(panel);

		Dimension pref = panel.getPreferredSize();
		int h = Math.max(pref.height, 1);

		// Second pass: lock to the resolved height and re-layout.
		panel.setSize(width, h);
		forceLayout(panel);

		BufferedImage img = new BufferedImage(width, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, width, h);
		panel.printAll(g);
		g.dispose();
		return img;
	}

	/**
	 * Recursively resolves layout bounds top-down. A native peer normally
	 * triggers this; without one we must drive {@code doLayout()} ourselves so
	 * BoxLayout/BorderLayout child bounds are non-zero before painting.
	 */
	private static void forceLayout(Component c)
	{
		if (!(c instanceof Container))
		{
			return;
		}
		Container container = (Container) c;
		container.doLayout();
		for (Component child : container.getComponents())
		{
			forceLayout(child);
		}
	}

	/** Writes the image as a PNG, creating parent directories as needed. */
	public static void writePng(BufferedImage img, Path out) throws IOException
	{
		if (out.getParent() != null)
		{
			Files.createDirectories(out.getParent());
		}
		ImageIO.write(img, "png", out.toFile());
	}
}
