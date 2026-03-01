package com.collectionloghelper.overlay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import lombok.Setter;
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
	private static final int DOT_RADIUS = 4;

	private final Client client;

	@Setter
	private WorldPoint targetPoint;

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

		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetPoint);
		if (localPoint == null)
		{
			return null;
		}

		Point minimapPoint = Perspective.localToMinimap(client, localPoint);
		if (minimapPoint == null)
		{
			return null;
		}

		graphics.setColor(MINIMAP_DOT_COLOR);
		graphics.fillOval(
			minimapPoint.getX() - DOT_RADIUS,
			minimapPoint.getY() - DOT_RADIUS,
			DOT_RADIUS * 2,
			DOT_RADIUS * 2);

		return null;
	}

	public void clearTarget()
	{
		targetPoint = null;
	}
}
