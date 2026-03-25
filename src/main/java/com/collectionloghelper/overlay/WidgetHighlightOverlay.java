package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Highlights arbitrary game interface widgets during guidance steps.
 * Each widget is identified by a [groupId, childId] pair. This enables
 * highlighting UI elements like spell icons, prayer buttons, or shop
 * interfaces as part of step-by-step guidance.
 */
@Singleton
public class WidgetHighlightOverlay extends Overlay
{
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
	private static final int FILL_ALPHA = 100;

	private final Client client;
	private final CollectionLogHelperConfig config;
	private volatile List<int[]> highlightWidgets = new ArrayList<>();
	private Color cachedOverlayColor;
	private Color cachedFillColor;

	@Inject
	private WidgetHighlightOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	/**
	 * Sets the list of widgets to highlight. Each entry is an int[] of
	 * [groupId, childId].
	 */
	public void setHighlightWidgets(List<int[]> widgets)
	{
		if (widgets == null || widgets.isEmpty())
		{
			this.highlightWidgets = new ArrayList<>();
		}
		else
		{
			this.highlightWidgets = new ArrayList<>(widgets);
		}
	}

	public void clearHighlights()
	{
		this.highlightWidgets = new ArrayList<>();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<int[]> widgets = this.highlightWidgets;
		if (widgets.isEmpty() || !config.showOverlays())
		{
			return null;
		}

		Color color = config.overlayColor();
		if (!color.equals(cachedOverlayColor))
		{
			cachedOverlayColor = color;
			cachedFillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA);
		}
		graphics.setStroke(STROKE_2);

		for (int[] widgetRef : widgets)
		{
			if (widgetRef == null || widgetRef.length < 2)
			{
				continue;
			}

			Widget widget = client.getWidget(widgetRef[0], widgetRef[1]);
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
			{
				continue;
			}

			graphics.setColor(cachedFillColor);
			graphics.fill(bounds);
			graphics.setColor(color);
			graphics.draw(bounds);
		}

		return null;
	}
}
