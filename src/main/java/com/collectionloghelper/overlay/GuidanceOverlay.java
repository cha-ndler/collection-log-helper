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
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class GuidanceOverlay extends OverlayPanel
{
	private static final Color BORDER_COLOR = new Color(0, 255, 255);
	private static final Color FILL_COLOR = new Color(0, 255, 255, 50);

	private final Client client;

	@Setter
	private WorldPoint targetPoint;
	@Setter
	private String targetName;
	@Setter
	private String clueGuidanceText;

	@Inject
	private GuidanceOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setMovable(true);
		setSnappable(true);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (targetPoint == null)
		{
			if (clueGuidanceText != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(clueGuidanceText)
					.color(Color.WHITE)
					.build());
				panelComponent.setPreferredSize(new Dimension(
					graphics.getFontMetrics().stringWidth(clueGuidanceText) + 20, 0));
				return super.render(graphics);
			}
			return null;
		}

		// Tile highlight rendering — uses DYNAMIC behavior
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
		clueGuidanceText = null;
	}
}
