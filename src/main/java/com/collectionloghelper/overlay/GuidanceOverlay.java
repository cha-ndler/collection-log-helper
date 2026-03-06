package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class GuidanceOverlay extends OverlayPanel
{
	private static final int MAX_PANEL_WIDTH = 200;

	private final Client client;
	private final CollectionLogHelperConfig config;

	private volatile WorldPoint targetPoint;
	private volatile String targetName;
	private volatile String locationDescription;
	private volatile String clueGuidanceText;
	private volatile boolean showSyncReminder;

	@Inject
	private GuidanceOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setMovable(true);
		setSnappable(true);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Color overlayColor = config.overlayColor();

		if (targetPoint == null)
		{
			if (clueGuidanceText != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(clueGuidanceText)
					.color(Color.WHITE)
					.build());
				addSyncReminderIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			if (showSyncReminder)
			{
				panelComponent.getChildren().clear();
				addSyncReminderIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		// Tile highlight rendering
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetPoint);
		if (localPoint == null)
		{
			// Target tile is not on screen — show compact direction panel
			if (targetName != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(targetName)
					.color(new Color(255, 200, 0))
					.build());
				String loc = locationDescription != null ? locationDescription : "";
				if (!loc.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(loc)
						.leftColor(Color.WHITE)
						.build());
				}
				addSyncReminderIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
		if (poly == null)
		{
			return null;
		}

		Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
			overlayColor.getBlue(), 50);
		OverlayUtil.renderPolygon(graphics, poly, overlayColor, fillColor,
			new BasicStroke(2.0f));

		if (targetName != null)
		{
			OverlayUtil.renderTextLocation(graphics,
				Perspective.getCanvasTextLocation(client, graphics, localPoint, targetName, 150),
				targetName, overlayColor);
		}

		return null;
	}

	public void setTargetPoint(WorldPoint targetPoint)
	{
		this.targetPoint = targetPoint;
	}

	public void setTargetName(String targetName)
	{
		this.targetName = targetName;
	}

	public void setLocationDescription(String locationDescription)
	{
		this.locationDescription = locationDescription;
	}

	public void setClueGuidanceText(String clueGuidanceText)
	{
		this.clueGuidanceText = clueGuidanceText;
	}

	public void setShowSyncReminder(boolean show)
	{
		this.showSyncReminder = show;
	}

	public void clearTarget()
	{
		targetPoint = null;
		targetName = null;
		locationDescription = null;
		clueGuidanceText = null;
	}

	private void addSyncReminderIfNeeded()
	{
		if (showSyncReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Collection Log to sync")
				.color(new Color(255, 170, 0))
				.build());
		}
	}
}
