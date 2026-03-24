package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
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

	private volatile List<Integer> targetItemIds = Collections.emptyList();
	private volatile boolean useItemOnObject;

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
		Rectangle bounds = widgetItem.getCanvasBounds();

		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 65));
		graphics.fill(bounds);
		graphics.setColor(color);
		graphics.setStroke(STROKE_2);
		graphics.draw(bounds);
	}

	public void setTargetItemIds(List<Integer> itemIds)
	{
		this.targetItemIds = itemIds != null ? itemIds : Collections.emptyList();
	}

	public void setUseItemOnObject(boolean value)
	{
		this.useItemOnObject = value;
	}

	public void clearTarget()
	{
		this.targetItemIds = Collections.emptyList();
		this.useItemOnObject = false;
	}
}
