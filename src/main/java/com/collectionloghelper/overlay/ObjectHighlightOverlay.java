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
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Singleton;

@Singleton
public class ObjectHighlightOverlay extends Overlay
{
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;
	private static final int TEXT_HEIGHT_OFFSET = 130;

	private final Client client;
	private final CollectionLogHelperConfig config;

	private volatile Set<Integer> targetObjectIds = Collections.emptySet();
	private volatile String objectInteractAction;
	private volatile boolean useItemOnObject;

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
	}

	public void setTargetObjectIds(Set<Integer> objectIds)
	{
		this.targetObjectIds = objectIds != null ? objectIds : Collections.emptySet();
	}

	public void setObjectInteractAction(String action)
	{
		this.objectInteractAction = action;
	}

	public void setUseItemOnObject(boolean value)
	{
		this.useItemOnObject = value;
	}

	public void clearTarget()
	{
		this.targetObjectIds = Collections.emptySet();
		this.objectInteractAction = null;
		this.useItemOnObject = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Snapshot volatile fields to prevent thread-safety races
		final Set<Integer> objIds = this.targetObjectIds;
		final String action = this.objectInteractAction;
		final boolean useItem = this.useItemOnObject;

		if (objIds.isEmpty())
		{
			return null;
		}

		Color overlayColor = config.overlayColor();
		net.runelite.api.WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		Scene scene = worldView.getScene();
		if (scene == null)
		{
			return null;
		}
		int plane = worldView.getPlane();
		Tile[][][] allTiles = scene.getTiles();
		if (allTiles == null || plane < 0 || plane >= allTiles.length)
		{
			return null;
		}
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

				// Check game objects
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects != null)
				{
					for (GameObject gameObject : gameObjects)
					{
						if (gameObject != null && objIds.contains(gameObject.getId()))
						{
							renderObjectHighlight(graphics, gameObject.getConvexHull(),
								gameObject.getLocalLocation(), overlayColor, action, useItem);
						}
					}
				}

				// Check wall objects
				WallObject wallObject = tile.getWallObject();
				if (wallObject != null && objIds.contains(wallObject.getId()))
				{
					renderObjectHighlight(graphics, wallObject.getConvexHull(),
						wallObject.getLocalLocation(), overlayColor, action, useItem);
				}

				// Check decorative objects
				DecorativeObject decorativeObject = tile.getDecorativeObject();
				if (decorativeObject != null && objIds.contains(decorativeObject.getId()))
				{
					renderObjectHighlight(graphics, decorativeObject.getConvexHull(),
						decorativeObject.getLocalLocation(), overlayColor, action, useItem);
				}
			}
		}

		return null;
	}

	private void renderObjectHighlight(Graphics2D graphics, Shape hull, LocalPoint localPoint,
		Color overlayColor, String action, boolean useItem)
	{
		if (hull != null)
		{
			Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 30);
			graphics.setColor(fillColor);
			graphics.fill(hull);
			graphics.setColor(overlayColor);
			graphics.setStroke(new BasicStroke(2.0f));
			graphics.draw(hull);

			// Draw downward-pointing arrow above the object hull
			Rectangle bounds = hull.getBounds();
			int arrowX = (int) bounds.getCenterX();
			int arrowTipY = (int) bounds.getMinY() - ARROW_GAP;
			renderDirectionArrow(graphics, arrowX, arrowTipY, overlayColor);
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
		graphics.setStroke(new BasicStroke(2.0f));
		graphics.drawPolygon(arrow);

		graphics.setColor(color);
		graphics.fillPolygon(arrow);
	}

	private void renderOutlinedText(Graphics2D graphics, Point point, String text, Color color)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = graphics.getFont().deriveFont(Font.BOLD, 12f);
		graphics.setFont(font);
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
