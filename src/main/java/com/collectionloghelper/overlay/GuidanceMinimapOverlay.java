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
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class GuidanceMinimapOverlay extends Overlay
{
	private static final int DOT_RADIUS = 4;
	private static final int ARROW_INNER_DIST = 55;
	private static final int ARROW_OUTER_DIST = 65;

	private final Client client;
	private final CollectionLogHelperConfig config;

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

		// If target is on the loaded map, try to show it as a dot on minimap
		if (localPoint != null)
		{
			Point minimapPoint = Perspective.localToMinimap(client, localPoint);
			if (minimapPoint != null)
			{
				// Outlined dot for visibility
				graphics.setColor(Color.BLACK);
				graphics.fillOval(
					minimapPoint.getX() - DOT_RADIUS - 1,
					minimapPoint.getY() - DOT_RADIUS - 1,
					(DOT_RADIUS + 1) * 2,
					(DOT_RADIUS + 1) * 2);
				graphics.setColor(overlayColor);
				graphics.fillOval(
					minimapPoint.getX() - DOT_RADIUS,
					minimapPoint.getY() - DOT_RADIUS,
					DOT_RADIUS * 2,
					DOT_RADIUS * 2);
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

		Line2D.Double line = new Line2D.Double(startX, startY, endX, endY);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawArrow(graphics, line, overlayColor);

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
		Point playerMinimap = client.getLocalPlayer().getMinimapLocation();
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
		graphics.setStroke(new BasicStroke(6));
		graphics.draw(line);
		drawArrowHead(graphics, line, 2, 2);

		// Colored fill
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(3));
		graphics.draw(line);
		drawArrowHead(graphics, line, 0, 0);

		graphics.setStroke(new BasicStroke(1));
	}

	/**
	 * Draw a triangular arrowhead at the end of a line, rotated to match direction.
	 */
	private void drawArrowHead(Graphics2D graphics, Line2D.Double line,
		int extraHeight, int extraWidth)
	{
		Polygon arrowHead = new Polygon();
		arrowHead.addPoint(0, 6 + extraHeight);
		arrowHead.addPoint(-6 - extraWidth, -1 - extraHeight);
		arrowHead.addPoint(6 + extraWidth, -1 - extraHeight);

		double angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);

		AffineTransform tx = new AffineTransform();
		tx.translate(line.x2, line.y2);
		tx.rotate(angle - Math.PI / 2.0);

		Graphics2D g = (Graphics2D) graphics.create();
		g.setTransform(tx);
		g.fill(arrowHead);
		g.dispose();
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
