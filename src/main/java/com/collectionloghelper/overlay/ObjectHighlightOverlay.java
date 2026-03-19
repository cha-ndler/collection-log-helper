package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import javax.inject.Singleton;

@Singleton
public class ObjectHighlightOverlay extends Overlay
{
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;
	private static final int TEXT_HEIGHT_OFFSET = 130;
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
	private static final Font BOLD_12 = new Font(Font.DIALOG, Font.BOLD, 12);

	private final Client client;
	private final CollectionLogHelperConfig config;

	@Inject
	private TooltipManager tooltipManager;

	private volatile Set<Integer> targetObjectIds = Collections.emptySet();
	private volatile String objectInteractAction;
	private volatile boolean useItemOnObject;
	private volatile String tooltipText;

	/**
	 * Cached list of scene objects matching targetObjectIds.
	 * Populated by a one-time scan when targets change, then maintained
	 * incrementally via onObjectSpawned/onObjectDespawned events forwarded
	 * from the plugin. This avoids scanning 10k+ tiles every frame.
	 */
	private volatile List<TileObject> matchedObjects = Collections.emptyList();

	@Inject
	private ObjectHighlightOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void setTargetObjectId(int objectId)
	{
		this.targetObjectIds = objectId > 0 ? Set.of(objectId) : Collections.emptySet();
		rescanScene();
	}

	public void setTargetObjectIds(Set<Integer> objectIds)
	{
		this.targetObjectIds = objectIds != null ? objectIds : Collections.emptySet();
		rescanScene();
	}

	public void setObjectInteractAction(String action)
	{
		this.objectInteractAction = action;
	}

	public void setUseItemOnObject(boolean value)
	{
		this.useItemOnObject = value;
	}

	public void setTooltipText(String text)
	{
		this.tooltipText = text;
	}

	public void clearTarget()
	{
		this.targetObjectIds = Collections.emptySet();
		this.objectInteractAction = null;
		this.useItemOnObject = false;
		this.tooltipText = null;
		this.matchedObjects = Collections.emptyList();
	}

	/**
	 * Called by the plugin when a game object spawns. Adds it to the cache
	 * if it matches the current target IDs.
	 */
	public void onObjectSpawned(TileObject obj)
	{
		Set<Integer> ids = targetObjectIds;
		if (ids.isEmpty() || obj == null || !ids.contains(obj.getId()))
		{
			return;
		}
		List<TileObject> current = new ArrayList<>(matchedObjects);
		current.add(obj);
		matchedObjects = current;
	}

	/**
	 * Called by the plugin when a game object despawns. Removes it from the cache.
	 */
	public void onObjectDespawned(TileObject obj)
	{
		if (obj == null || matchedObjects.isEmpty())
		{
			return;
		}
		List<TileObject> current = new ArrayList<>(matchedObjects);
		current.remove(obj);
		matchedObjects = current;
	}

	/**
	 * Full scene rescan — called when target IDs change or on scene load.
	 * Must be called from the client thread.
	 */
	public void rescanScene()
	{
		Set<Integer> ids = targetObjectIds;
		if (ids.isEmpty())
		{
			matchedObjects = Collections.emptyList();
			return;
		}

		net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			matchedObjects = Collections.emptyList();
			return;
		}
		Scene scene = worldView.getScene();
		if (scene == null)
		{
			matchedObjects = Collections.emptyList();
			return;
		}
		int plane = worldView.getPlane();
		Tile[][][] allTiles = scene.getTiles();
		if (allTiles == null || plane < 0 || plane >= allTiles.length)
		{
			matchedObjects = Collections.emptyList();
			return;
		}

		List<TileObject> found = new ArrayList<>();
		Tile[][] tiles = allTiles[plane];
		for (Tile[] row : tiles)
		{
			if (row == null)
			{
				continue;
			}
			for (Tile tile : row)
			{
				if (tile == null)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject go : gameObjects)
					{
						if (go != null && ids.contains(go.getId()))
						{
							found.add(go);
						}
					}
				}
				WallObject wo = tile.getWallObject();
				if (wo != null && ids.contains(wo.getId()))
				{
					found.add(wo);
				}
				DecorativeObject deco = tile.getDecorativeObject();
				if (deco != null && ids.contains(deco.getId()))
				{
					found.add(deco);
				}
			}
		}
		matchedObjects = found;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Snapshot volatile fields to prevent thread-safety races
		final List<TileObject> objects = this.matchedObjects;
		final String action = this.objectInteractAction;
		final boolean useItem = this.useItemOnObject;
		final String tipText = this.tooltipText;

		if (objects.isEmpty())
		{
			return null;
		}

		Color overlayColor = config.overlayColor();
		final Point mousePos = client.getMouseCanvasPosition();
		final String builtTooltip = OverlayTooltipHelper.buildTooltip(tipText, action);
		boolean tooltipShown = false;

		for (TileObject obj : objects)
		{
			Shape hull = getHull(obj);
			LocalPoint localPoint = obj.getLocalLocation();
			tooltipShown |= renderObjectHighlight(graphics, hull, localPoint,
				overlayColor, action, useItem, mousePos, builtTooltip, tooltipShown);
		}

		return null;
	}

	private static Shape getHull(TileObject obj)
	{
		if (obj instanceof GameObject)
		{
			return ((GameObject) obj).getConvexHull();
		}
		if (obj instanceof WallObject)
		{
			return ((WallObject) obj).getConvexHull();
		}
		if (obj instanceof DecorativeObject)
		{
			return ((DecorativeObject) obj).getConvexHull();
		}
		return null;
	}

	private boolean renderObjectHighlight(Graphics2D graphics, Shape hull, LocalPoint localPoint,
		Color overlayColor, String action, boolean useItem,
		Point mousePos, String builtTooltip, boolean tooltipAlreadyShown)
	{
		boolean showedTooltip = false;
		if (hull != null)
		{
			Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 30);
			graphics.setColor(fillColor);
			graphics.fill(hull);
			graphics.setColor(overlayColor);
			graphics.setStroke(STROKE_2);
			graphics.draw(hull);

			// Draw downward-pointing arrow above the object hull
			Rectangle bounds = hull.getBounds();
			int arrowX = (int) bounds.getCenterX();
			int arrowTipY = (int) bounds.getMinY() - ARROW_GAP;
			renderDirectionArrow(graphics, arrowX, arrowTipY, overlayColor);

			// Show tooltip when mouse hovers over the object hull (once per frame)
			if (!tooltipAlreadyShown && builtTooltip != null
				&& mousePos != null && hull.contains(mousePos.getX(), mousePos.getY()))
			{
				tooltipManager.add(new Tooltip(builtTooltip));
				showedTooltip = true;
			}
		}

		// Render action text above the object
		if (localPoint != null && action != null)
		{
			String displayText = useItem ? "Use " + action + " \u2192" : action;
			Point textPoint = Perspective.getCanvasTextLocation(
				client, graphics, localPoint, displayText, TEXT_HEIGHT_OFFSET);
			if (textPoint != null)
			{
				renderOutlinedText(graphics, textPoint, displayText, overlayColor);
			}
		}
		return showedTooltip;
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
}
