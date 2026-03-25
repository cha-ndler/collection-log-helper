package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
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
	private static final int ARROWHEAD_SIZE = 12;
	private static final BasicStroke LINE_STROKE = new BasicStroke(LINE_WIDTH);

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final Polygon arrowPolygon = new Polygon();
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

		if (client.getLocalPlayer() == null)
		{
			return null;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
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

		// Clip to the map widget bounds so the line doesn't draw outside the map
		Shape previousClip = graphics.getClip();
		graphics.setClip(mapBounds);

		Color overlayColor = config.overlayColor();
		if (!overlayColor.equals(cachedOverlayColor))
		{
			cachedOverlayColor = overlayColor;
			cachedLineColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), LINE_ALPHA);
		}
		Color lineColor = cachedLineColor;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Draw the route line
		graphics.setColor(lineColor);
		graphics.setStroke(LINE_STROKE);
		routeLine.setLine(
			playerMapPoint.getX(), playerMapPoint.getY(),
			targetMapPoint.getX(), targetMapPoint.getY()
		);
		graphics.draw(routeLine);

		// Draw arrowhead at the target end
		drawArrowhead(graphics, playerMapPoint, targetMapPoint, lineColor);

		// Restore previous clip so other overlays are not affected
		graphics.setClip(previousClip);

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

	/**
	 * Draws a small filled arrowhead at the target point, pointing in the
	 * direction from the player to the target.
	 */
	private void drawArrowhead(Graphics2D graphics, Point from, Point to, Color color)
	{
		double dx = to.getX() - from.getX();
		double dy = to.getY() - from.getY();
		double angle = Math.atan2(dy, dx);

		int tipX = to.getX();
		int tipY = to.getY();

		int x1 = tipX - (int) (ARROWHEAD_SIZE * Math.cos(angle - Math.PI / 6));
		int y1 = tipY - (int) (ARROWHEAD_SIZE * Math.sin(angle - Math.PI / 6));
		int x2 = tipX - (int) (ARROWHEAD_SIZE * Math.cos(angle + Math.PI / 6));
		int y2 = tipY - (int) (ARROWHEAD_SIZE * Math.sin(angle + Math.PI / 6));

		arrowPolygon.reset();
		arrowPolygon.addPoint(tipX, tipY);
		arrowPolygon.addPoint(x1, y1);
		arrowPolygon.addPoint(x2, y2);

		graphics.setColor(color);
		graphics.fillPolygon(arrowPolygon);
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
