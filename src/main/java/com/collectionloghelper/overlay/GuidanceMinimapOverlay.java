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

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class GuidanceMinimapOverlay extends Overlay
{
	private static final int ARROW_INNER_DIST = 55;
	private static final int ARROW_OUTER_DIST = 65;
	private static final BasicStroke STROKE_6 = new BasicStroke(6);
	private static final BasicStroke STROKE_3 = new BasicStroke(3);
	private static final BasicStroke STROKE_1 = new BasicStroke(1);

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final Line2D.Double arrowLine = new Line2D.Double();
	private final Polygon arrowHead = new Polygon();
	private final AffineTransform arrowTransform = new AffineTransform();

	private volatile WorldPoint targetPoint;

	@Inject
	private GuidanceMinimapOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (targetPoint == null)
		{
			return null;
		}

		Color overlayColor = config.overlayColor();

		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetPoint);

		// If target is visible on the minimap, no arrow needed
		if (localPoint != null)
		{
			Point minimapPoint = Perspective.localToMinimap(client, localPoint);
			if (minimapPoint != null)
			{
				return null;
			}
		}

		// Target is far away — draw direction arrow on minimap edge
		// Uses camera yaw rotation so the arrow always points correctly
		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}

		Point playerMinimapLoc = client.getLocalPlayer().getMinimapLocation();
		if (playerMinimapLoc == null)
		{
			return null;
		}

		// Compute camera-rotated minimap point for the target
		Point destOnMinimap = getMinimapPoint(playerLocation, targetPoint);
		if (destOnMinimap == null)
		{
			return null;
		}

		// Draw a line from player center toward the destination, clamped to minimap edge
		double dx = destOnMinimap.getX() - playerMinimapLoc.getX();
		double dy = destOnMinimap.getY() - playerMinimapLoc.getY();
		double angle = Math.atan2(dy, dx);

		int startX = playerMinimapLoc.getX() + (int) (ARROW_INNER_DIST * Math.cos(angle));
		int startY = playerMinimapLoc.getY() + (int) (ARROW_INNER_DIST * Math.sin(angle));
		int endX = playerMinimapLoc.getX() + (int) (ARROW_OUTER_DIST * Math.cos(angle));
		int endY = playerMinimapLoc.getY() + (int) (ARROW_OUTER_DIST * Math.sin(angle));

		arrowLine.setLine(startX, startY, endX, endY);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawArrow(graphics, arrowLine, overlayColor);

		return null;
	}

	/**
	 * Compute where a world point would appear on the minimap, accounting
	 * for camera yaw rotation. Based on Quest Helper's QuestPerspective approach.
	 */
	private Point getMinimapPoint(WorldPoint player, WorldPoint destination)
	{
		int x = destination.getX() - player.getX();
		int y = destination.getY() - player.getY();

		float maxDist = Math.max(Math.abs(x), Math.abs(y));
		if (maxDist == 0)
		{
			return null;
		}

		// Normalize to a 100-unit scale (preserves direction, discards distance)
		x = (int) ((x * 100f) / maxDist);
		y = (int) ((y * 100f) / maxDist);

		// Rotate by camera yaw using RuneLite's fixed-point trig tables
		final int cameraAngle = client.getCameraYawTarget() & 0x7FF;
		final int sin = Perspective.SINE[cameraAngle];
		final int cos = Perspective.COSINE[cameraAngle];

		final int xx = (y * sin + cos * x) >> 16;
		final int yy = (sin * x - y * cos) >> 16;

		// Get player's minimap location as the center
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}
		Point playerMinimap = localPlayer.getMinimapLocation();
		if (playerMinimap == null)
		{
			return null;
		}

		return new Point(playerMinimap.getX() + xx, playerMinimap.getY() + yy);
	}

	/**
	 * Draw an arrow (shaft + arrowhead) in Quest Helper style:
	 * black border stroke + colored inner stroke + filled arrowhead.
	 */
	private void drawArrow(Graphics2D graphics, Line2D.Double line, Color color)
	{
		// Black border
		graphics.setColor(Color.BLACK);
		graphics.setStroke(STROKE_6);
		graphics.draw(line);
		drawArrowHead(graphics, line, 2, 2);

		// Colored fill
		graphics.setColor(color);
		graphics.setStroke(STROKE_3);
		graphics.draw(line);
		drawArrowHead(graphics, line, 0, 0);

		graphics.setStroke(STROKE_1);
	}

	/**
	 * Draw a triangular arrowhead at the end of a line, rotated to match direction.
	 */
	private void drawArrowHead(Graphics2D graphics, Line2D.Double line,
		int extraHeight, int extraWidth)
	{
		arrowHead.reset();
		arrowHead.addPoint(0, 6 + extraHeight);
		arrowHead.addPoint(-6 - extraWidth, -1 - extraHeight);
		arrowHead.addPoint(6 + extraWidth, -1 - extraHeight);

		double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);

		arrowTransform.setToIdentity();
		arrowTransform.translate(line.x2, line.y2);
		arrowTransform.rotate(angle - Math.PI / 2.0);

		AffineTransform savedTransform = graphics.getTransform();
		graphics.setTransform(arrowTransform);
		graphics.fill(arrowHead);
		graphics.setTransform(savedTransform);
	}

	public void setTargetPoint(WorldPoint targetPoint)
	{
		this.targetPoint = targetPoint;
	}

	public void clearTarget()
	{
		targetPoint = null;
	}
}
