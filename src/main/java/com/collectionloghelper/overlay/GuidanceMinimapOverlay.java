package com.collectionloghelper.overlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
	private static final Color MINIMAP_DOT_COLOR = new Color(0, 255, 255);
	private static final Color ARROW_COLOR = new Color(220, 30, 30);
	private static final Color ARROW_BORDER_COLOR = new Color(50, 0, 0);
	private static final int DOT_RADIUS = 4;
	private static final int ARROW_SIZE = 10;
	private static final int MINIMAP_RADIUS = 70;
	private static final int FLASH_INTERVAL_MS = 600;

	private final Client client;

	private volatile WorldPoint targetPoint;

	private long lastFlashTime = 0;
	private boolean flashVisible = true;

	@Inject
	private GuidanceMinimapOverlay(Client client)
	{
		this.client = client;
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

		// Update flash state
		long now = System.currentTimeMillis();
		if (now - lastFlashTime > FLASH_INTERVAL_MS)
		{
			flashVisible = !flashVisible;
			lastFlashTime = now;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetPoint);

		// If target is on the loaded map, try to show it as a dot on minimap
		if (localPoint != null)
		{
			Point minimapPoint = Perspective.localToMinimap(client, localPoint);
			if (minimapPoint != null)
			{
				graphics.setColor(MINIMAP_DOT_COLOR);
				graphics.fillOval(
					minimapPoint.getX() - DOT_RADIUS,
					minimapPoint.getY() - DOT_RADIUS,
					DOT_RADIUS * 2,
					DOT_RADIUS * 2);
				return null;
			}
		}

		// Target is far away — draw flashing edge arrow on minimap
		if (!flashVisible)
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

		// Get minimap center: the player is always centered on the minimap
		LocalPoint playerLocal = client.getLocalPlayer().getLocalLocation();
		Point minimapCenter = Perspective.localToMinimap(client, playerLocal);
		if (minimapCenter == null)
		{
			return null;
		}

		int cx = minimapCenter.getX();
		int cy = minimapCenter.getY();

		// Calculate angle from player to target
		double dx = targetPoint.getX() - playerLocation.getX();
		double dy = targetPoint.getY() - playerLocation.getY();
		double angle = Math.atan2(dy, dx);

		// Position arrow at edge of minimap circle (Y inverted for screen coords)
		int arrowX = cx + (int) (MINIMAP_RADIUS * Math.cos(angle));
		int arrowY = cy - (int) (MINIMAP_RADIUS * Math.sin(angle));

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		drawDirectionArrow(graphics, arrowX, arrowY, angle);

		return null;
	}

	private void drawDirectionArrow(Graphics2D graphics, int x, int y, double angle)
	{
		int size = ARROW_SIZE;

		// Arrow tip points toward target (screen Y is inverted)
		int tipX = x + (int) (size * Math.cos(angle));
		int tipY = y - (int) (size * Math.sin(angle));

		// Two base points at +-135 degrees from direction
		double baseAngle1 = angle + Math.PI * 0.75;
		double baseAngle2 = angle - Math.PI * 0.75;
		int base1X = x + (int) (size * 0.7 * Math.cos(baseAngle1));
		int base1Y = y - (int) (size * 0.7 * Math.sin(baseAngle1));
		int base2X = x + (int) (size * 0.7 * Math.cos(baseAngle2));
		int base2Y = y - (int) (size * 0.7 * Math.sin(baseAngle2));

		int[] xPoints = {tipX, base1X, base2X};
		int[] yPoints = {tipY, base1Y, base2Y};

		// Dark border for contrast
		graphics.setColor(ARROW_BORDER_COLOR);
		graphics.fillPolygon(xPoints, yPoints, 3);

		// Slightly smaller red fill
		int innerTipX = x + (int) ((size - 1) * Math.cos(angle));
		int innerTipY = y - (int) ((size - 1) * Math.sin(angle));
		int inner1X = x + (int) ((size - 1) * 0.65 * Math.cos(baseAngle1));
		int inner1Y = y - (int) ((size - 1) * 0.65 * Math.sin(baseAngle1));
		int inner2X = x + (int) ((size - 1) * 0.65 * Math.cos(baseAngle2));
		int inner2Y = y - (int) ((size - 1) * 0.65 * Math.sin(baseAngle2));

		graphics.setColor(ARROW_COLOR);
		graphics.fillPolygon(
			new int[]{innerTipX, inner1X, inner2X},
			new int[]{innerTipY, inner1Y, inner2Y}, 3);
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
