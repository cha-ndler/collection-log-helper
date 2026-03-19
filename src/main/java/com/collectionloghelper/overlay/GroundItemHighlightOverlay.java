package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class GroundItemHighlightOverlay extends Overlay
{
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;
	private static final int TEXT_HEIGHT_OFFSET = 50;
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
	private static final Font BOLD_12 = new Font(Font.DIALOG, Font.BOLD, 12);

	private final Client client;
	private final CollectionLogHelperConfig config;

	private volatile Set<Integer> targetGroundItemIds = Collections.emptySet();

	/**
	 * Cached list of ground items matching targetGroundItemIds.
	 * Maintained via onItemSpawned/onItemDespawned events forwarded
	 * from the plugin, avoiding a full tile grid scan every frame.
	 */
	private volatile List<TrackedGroundItem> matchedItems = Collections.emptyList();

	@Inject
	private GroundItemHighlightOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void setTargetGroundItemIds(Set<Integer> itemIds)
	{
		this.targetGroundItemIds = itemIds != null ? itemIds : Collections.emptySet();
		this.matchedItems = Collections.emptyList();
	}

	public void clearTargets()
	{
		this.targetGroundItemIds = Collections.emptySet();
		this.matchedItems = Collections.emptyList();
	}

	/**
	 * Called by the plugin when a ground item spawns. Adds it to the cache
	 * if it matches the current target IDs.
	 */
	public void onItemSpawned(TileItem item, Tile tile)
	{
		Set<Integer> ids = targetGroundItemIds;
		if (ids.isEmpty() || item == null || !ids.contains(item.getId()))
		{
			return;
		}
		List<TrackedGroundItem> current = new ArrayList<>(matchedItems);
		current.add(new TrackedGroundItem(item, tile));
		matchedItems = current;
	}

	/**
	 * Called by the plugin when a ground item despawns. Removes it from the cache.
	 */
	public void onItemDespawned(TileItem item)
	{
		if (item == null || matchedItems.isEmpty())
		{
			return;
		}
		List<TrackedGroundItem> current = new ArrayList<>(matchedItems);
		current.removeIf(t -> t.item == item);
		matchedItems = current;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final List<TrackedGroundItem> items = this.matchedItems;

		if (items.isEmpty())
		{
			return null;
		}

		Color overlayColor = config.overlayColor();

		for (TrackedGroundItem tracked : items)
		{
			renderGroundItemHighlight(graphics, tracked.tile, tracked.item, overlayColor);
		}

		return null;
	}

	private void renderGroundItemHighlight(Graphics2D graphics, Tile tile, TileItem item, Color overlayColor)
	{
		LocalPoint localPoint = tile.getLocalLocation();
		if (localPoint == null)
		{
			return;
		}

		Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly != null)
		{
			Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 30);
			graphics.setColor(fillColor);
			graphics.fill(tilePoly);
			graphics.setColor(overlayColor);
			graphics.setStroke(STROKE_2);
			graphics.draw(tilePoly);

			// Draw downward-pointing arrow above the tile
			int arrowX = (int) tilePoly.getBounds().getCenterX();
			int arrowTipY = (int) tilePoly.getBounds().getMinY() - ARROW_GAP;
			renderDirectionArrow(graphics, arrowX, arrowTipY, overlayColor);
		}

		// Render item name above the tile
		String itemName = getItemName(item.getId());
		if (itemName != null)
		{
			Point textPoint = Perspective.getCanvasTextLocation(
				client, graphics, localPoint, itemName, TEXT_HEIGHT_OFFSET);
			if (textPoint != null)
			{
				renderOutlinedText(graphics, textPoint, itemName, overlayColor);
			}
		}
	}

	private String getItemName(int itemId)
	{
		ItemComposition composition = client.getItemDefinition(itemId);
		return composition != null ? composition.getName() : null;
	}

	private void renderDirectionArrow(Graphics2D graphics, int x, int tipY, Color color)
	{
		int halfW = ARROW_WIDTH / 2;
		int topY = tipY - ARROW_HEIGHT;

		Polygon arrow = new Polygon(
			new int[]{x, x + halfW, x - halfW},
			new int[]{tipY, topY, topY},
			3
		);

		graphics.setColor(Color.BLACK);
		graphics.setStroke(STROKE_2);
		graphics.drawPolygon(arrow);

		graphics.setColor(color);
		graphics.fillPolygon(arrow);
	}

	private void renderOutlinedText(Graphics2D graphics, Point point, String text, Color color)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(BOLD_12);
		FontMetrics fm = graphics.getFontMetrics();
		int x = point.getX() - fm.stringWidth(text) / 2;
		int y = point.getY();

		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.drawString(text, x - 1, y - 1);
		graphics.drawString(text, x + 1, y - 1);
		graphics.drawString(text, x - 1, y + 1);

		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	private static class TrackedGroundItem
	{
		final TileItem item;
		final Tile tile;

		TrackedGroundItem(TileItem item, Tile tile)
		{
			this.item = item;
			this.tile = tile;
		}
	}
}
