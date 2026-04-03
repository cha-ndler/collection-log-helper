/*
 * Copyright (c) 2025, Chandler
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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

public class CollectionLogWorldMapPoint extends WorldMapPoint
{
	private static final int ICON_SIZE = 32;
	private static final int ARROW_SIZE = 32;
	private static final Color BADGE_COLOR = new Color(0, 200, 200);
	private static final Color BADGE_BORDER = new Color(0, 100, 100);

	private final BufferedImage mapIcon;
	private final Point mapIconAnchor;
	private final Map<Integer, BufferedImage> arrows = new HashMap<>();
	private BufferedImage activeArrow;

	public CollectionLogWorldMapPoint(WorldPoint worldPoint, String name, BufferedImage itemIcon)
	{
		super(worldPoint, null);

		mapIcon = createMapIcon(itemIcon);
		mapIconAnchor = new Point(mapIcon.getWidth() / 2, mapIcon.getHeight());

		// Generate 8 directional arrow sprites
		for (int deg = 0; deg < 360; deg += 45)
		{
			arrows.put(deg, createArrowImage(deg));
		}
		activeArrow = arrows.get(0);

		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setName(name);
		this.setImage(mapIcon);
		this.setImagePoint(mapIconAnchor);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(activeArrow);
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(mapIcon);
		this.setImagePoint(mapIconAnchor);
	}

	public void rotateArrow(int degrees)
	{
		BufferedImage newArrow = arrows.get(degrees);
		if (newArrow != null && activeArrow != newArrow)
		{
			activeArrow = newArrow;
			if (isCurrentlyEdgeSnapped())
			{
				setImage(activeArrow);
			}
		}
	}

	/**
	 * Create the on-map icon: a circular badge with the item icon composited on top,
	 * with a pin-point triangle at the bottom (like a map marker).
	 */
	private static BufferedImage createMapIcon(BufferedImage itemIcon)
	{
		int width = ICON_SIZE;
		int height = ICON_SIZE + 8;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		int circleDiam = width - 4;
		int circleX = 2;
		int circleY = 2;
		int centerX = width / 2;
		int centerY = circleY + circleDiam / 2;

		// Pin point triangle
		int[] triX = {centerX - 6, centerX + 6, centerX};
		int[] triY = {centerY + 4, centerY + 4, height - 1};

		// Border
		g.setColor(BADGE_BORDER);
		g.fillOval(circleX - 1, circleY - 1, circleDiam + 2, circleDiam + 2);
		g.setStroke(new BasicStroke(2.0f));
		g.fillPolygon(triX, triY, 3);

		// Badge fill
		g.setColor(BADGE_COLOR);
		g.fillOval(circleX, circleY, circleDiam, circleDiam);
		g.fillPolygon(triX, triY, 3);

		// Border outline
		g.setColor(BADGE_BORDER);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(circleX, circleY, circleDiam, circleDiam);

		// Draw item icon centered in the circle
		if (itemIcon != null)
		{
			int iconW = Math.min(itemIcon.getWidth(), circleDiam - 6);
			int iconH = Math.min(itemIcon.getHeight(), circleDiam - 6);
			int iconX = centerX - iconW / 2;
			int iconY = centerY - iconH / 2;
			g.drawImage(itemIcon, iconX, iconY, iconW, iconH, null);
		}

		g.dispose();
		return img;
	}

	/**
	 * Create a directional arrow sprite rotated to the given degree (0=right, 90=down, etc.).
	 */
	private static BufferedImage createArrowImage(int degrees)
	{
		int size = ARROW_SIZE;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Chevron pointing right (0 degrees), centered in the image
		int cx = size / 2;
		int cy = size / 2;
		int[] xPoints = {cx + 12, cx - 6, cx - 2, cx - 6};
		int[] yPoints = {cy, cy - 9, cy, cy + 9};

		AffineTransform tx = new AffineTransform();
		tx.rotate(Math.toRadians(degrees), cx, cy);
		g.setTransform(tx);

		// Solid fill
		g.setColor(BADGE_COLOR);
		g.fillPolygon(xPoints, yPoints, 4);

		// Black outline on top
		g.setColor(BADGE_BORDER);
		g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawPolygon(xPoints, yPoints, 4);

		g.dispose();
		return img;
	}
}
