package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import javax.inject.Singleton;

/**
 * Highlights inventory items that are relevant to the current guidance step.
 * Uses WidgetItemOverlay to render colored borders and fills on matching items.
 */
@Singleton
public class ItemHighlightOverlay extends WidgetItemOverlay
{
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);

	private final CollectionLogHelperConfig config;

	private volatile Set<Integer> targetItemIds = Collections.emptySet();
	private volatile boolean useItemOnObject;
	private Color cachedOverlayColor;
	private Color cachedFillColor;

	@Inject
	private ItemHighlightOverlay(CollectionLogHelperConfig config)
	{
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (targetItemIds.isEmpty())
		{
			return;
		}

		if (!targetItemIds.contains(itemId))
		{
			return;
		}

		Color color = config.overlayColor();
		if (!color.equals(cachedOverlayColor))
		{
			cachedOverlayColor = color;
			cachedFillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 65);
		}
		Rectangle bounds = widgetItem.getCanvasBounds();

		graphics.setColor(cachedFillColor);
		graphics.fill(bounds);
		graphics.setColor(color);
		graphics.setStroke(STROKE_2);
		graphics.draw(bounds);
	}

	public void setTargetItemIds(List<Integer> itemIds)
	{
		this.targetItemIds = itemIds != null ? new HashSet<>(itemIds) : Collections.emptySet();
	}

	public void setUseItemOnObject(boolean value)
	{
		this.useItemOnObject = value;
	}

	public void clearTarget()
	{
		this.targetItemIds = Collections.emptySet();
		this.useItemOnObject = false;
	}
}
