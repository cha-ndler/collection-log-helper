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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws a colored route line from the player's position to the guidance target
 * on the world map when it is open. Uses the same coordinate conversion approach
 * as RuneLite's WorldMapOverlay (credit: Morgan Lewis / RuneLite contributors).
 */
@Singleton
public class WorldMapRouteOverlay extends Overlay
{
	private static final float LINE_WIDTH = 2.0f;
	private static final int LINE_ALPHA = 180;
	private static final BasicStroke LINE_STROKE = new BasicStroke(LINE_WIDTH);

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final Line2D.Double routeLine = new Line2D.Double();

	private volatile WorldPoint targetPoint;
	private Color cachedOverlayColor;
	private Color cachedLineColor;

	@Inject
	private WorldMapRouteOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.WORLDMAP);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final WorldPoint target = this.targetPoint;
		if (target == null)
		{
			return null;
		}

		// Check if the world map is open by looking for the map container widget
		Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (mapWidget == null)
		{
			return null;
		}

		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return null;
		}

		WorldPoint playerLocation = lp.getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}

		Rectangle mapBounds = mapWidget.getBounds();

		Point playerMapPoint = mapWorldPointToGraphicsPoint(playerLocation, mapBounds);
		Point targetMapPoint = mapWorldPointToGraphicsPoint(target, mapBounds);

		if (playerMapPoint == null || targetMapPoint == null)
		{
			return null;
		}

		// Only draw the route line when both endpoints are within the map bounds.
		// When the target is off-screen, CollectionLogWorldMapPoint's edge-snap
		// arrow already indicates direction — drawing a clipped line just creates
		// visual artifacts near the arrow sprite.
		boolean playerVisible = mapBounds.contains(playerMapPoint.getX(), playerMapPoint.getY());
		boolean targetVisible = mapBounds.contains(targetMapPoint.getX(), targetMapPoint.getY());

		if (playerVisible && targetVisible)
		{
			Shape previousClip = graphics.getClip();
			graphics.setClip(mapBounds);

			Color overlayColor = config.overlayColor();
			if (!overlayColor.equals(cachedOverlayColor))
			{
				cachedOverlayColor = overlayColor;
				cachedLineColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
					overlayColor.getBlue(), LINE_ALPHA);
			}

			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(cachedLineColor);
			graphics.setStroke(LINE_STROKE);
			routeLine.setLine(
				playerMapPoint.getX(), playerMapPoint.getY(),
				targetMapPoint.getX(), targetMapPoint.getY()
			);
			graphics.draw(routeLine);

			graphics.setClip(previousClip);
		}

		return null;
	}

	/**
	 * Converts a WorldPoint to pixel coordinates on the world map widget.
	 * Based on RuneLite's WorldMapOverlay.mapWorldPointToGraphicsPoint
	 * (credit: Morgan Lewis / RuneLite contributors, BSD 2-Clause).
	 */
	private Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint, Rectangle worldMapRect)
	{
		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null || worldMap.getWorldMapData() == null)
		{
			return null;
		}

		if (!worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY()))
		{
			return null;
		}

		float pixelsPerTile = worldMap.getWorldMapZoom();

		int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

		Point worldMapPosition = worldMap.getWorldMapPosition();
		if (worldMapPosition == null)
		{
			return null;
		}

		int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
		int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
		int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

		int xGraphDiff = (int) (xTileOffset * pixelsPerTile);
		int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

		yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
		xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

		yGraphDiff = worldMapRect.height - yGraphDiff;
		yGraphDiff += (int) worldMapRect.getY();
		xGraphDiff += (int) worldMapRect.getX();

		return new Point(xGraphDiff, yGraphDiff);
	}

	public void setTargetPoint(WorldPoint point)
	{
		this.targetPoint = point;
	}

	public void clearTarget()
	{
		this.targetPoint = null;
	}
}
