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

/**
 * Highlights inventory items that are relevant to the current guidance step.
 * Uses WidgetItemOverlay to render colored borders and fills on matching items.
 */
public class ItemHighlightOverlay extends WidgetItemOverlay
{
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
		graphics.setStroke(new BasicStroke(2.0f));
		graphics.draw(bounds);

		// Draw a small arrow indicator when this item should be used on an object
		if (useItemOnObject)
		{
			graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.BOLD, 10f));
			graphics.setColor(Color.BLACK);
			int arrowX = bounds.x + bounds.width - 10;
			int arrowY = bounds.y + bounds.height - 2;
			graphics.drawString("\u2192", arrowX + 1, arrowY + 1);
			graphics.setColor(color);
			graphics.drawString("\u2192", arrowX, arrowY);
		}
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
