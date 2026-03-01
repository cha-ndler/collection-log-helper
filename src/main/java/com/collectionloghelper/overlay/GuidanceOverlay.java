package com.collectionloghelper.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class GuidanceOverlay extends Overlay
{
	private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 255, 128);
	private static final Color BORDER_COLOR = new Color(0, 255, 255);
	private static final Color FILL_COLOR = new Color(0, 255, 255, 50);

	private final Client client;

	@Setter
	private WorldPoint targetPoint;
	@Setter
	private String targetName;

	@Inject
	private GuidanceOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
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

		Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
		if (poly == null)
		{
			return null;
		}

		OverlayUtil.renderPolygon(graphics, poly, BORDER_COLOR, FILL_COLOR,
			new BasicStroke(2.0f));

		if (targetName != null)
		{
			OverlayUtil.renderTextLocation(graphics,
				Perspective.getCanvasTextLocation(client, graphics, localPoint, targetName, 150),
				targetName, BORDER_COLOR);
		}

		return null;
	}

	public void clearTarget()
	{
		targetPoint = null;
		targetName = null;
	}
}
