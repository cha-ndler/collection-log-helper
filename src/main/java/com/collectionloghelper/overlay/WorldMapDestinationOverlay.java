/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Renders a directional arrow (when destination is off-screen) and a destination
 * icon (when destination is on-screen) on the world map for the active guidance step.
 *
 * <p>This overlay is additive to {@link WorldMapRouteOverlay}'s route line. It
 * draws on top via the same MANUAL layer so both render inside the world map widget.
 *
 * <p>Arrow is drawn toward the target from the edge of the map view when the
 * destination tile is outside the visible bounds. The destination icon is drawn
 * at the target's map position when it is inside the visible bounds.
 *
 * <p>Per-frame allocation rules (feedback_overlay_performance.md):
 * Arrow {@link BufferedImage}s are cached once at construction. No new objects
 * are allocated in the render path.
 */
@Singleton
public class WorldMapDestinationOverlay extends Overlay
{
	/** Size of the cached directional arrow sprites (px). */
	private static final int ARROW_SPRITE_SIZE = 32;

	/** Size of the destination icon drawn at the target tile (px). */
	private static final int ICON_SIZE = 20;

	/** Inset from the map edge at which the off-screen arrow is placed (px). */
	private static final int EDGE_INSET = 18;

	/** Fill colour for arrow and icon (teal to match CollectionLogWorldMapPoint). */
	private static final Color FILL_COLOR = new Color(0, 200, 200);

	/** Outline/border colour. */
	private static final Color BORDER_COLOR = new Color(0, 100, 100);

	/** Stroke used to outline the destination icon shapes. */
	private static final BasicStroke ICON_STROKE =
		new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/** Stroke used to outline the edge-snap arrow. */
	private static final BasicStroke ARROW_STROKE =
		new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final CollectionLogHelperConfig config;

	/** Target world-point set by the coordinator. Volatile for cross-thread visibility. */
	private volatile WorldPoint targetPoint;

	/** Step type used to pick the icon variant. Volatile for cross-thread visibility. */
	private volatile StepIconType iconType = StepIconType.TILE;

	/**
	 * When true, a {@link CollectionLogWorldMapPoint} is registered with the
	 * {@link net.runelite.client.ui.overlay.worldmap.WorldMapPointManager} and its
	 * edge-snap arrow already handles the off-screen direction indicator on the world
	 * map. In that case this overlay skips its own off-screen arrow to prevent a
	 * duplicate arrow (#410). The on-screen destination icon is still drawn.
	 */
	private volatile boolean mapPointActive;

	/**
	 * 8 pre-rendered directional arrow sprites (0°, 45°, 90°, …, 315°).
	 * 0° = pointing right; angles increase clockwise (screen-space).
	 * Allocated once at construction; never re-allocated per frame.
	 */
	private final BufferedImage[] arrowSprites = new BufferedImage[8];

	/**
	 * 3 pre-rendered destination icon sprites: NPC, object, tile.
	 * Allocated once at construction; never re-allocated per frame.
	 */
	private final BufferedImage npcIcon;
	private final BufferedImage objectIcon;
	private final BufferedImage tileIcon;

	@Inject
	WorldMapDestinationOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.WORLDMAP);
		// Slightly higher priority than WorldMapRouteOverlay so destination icon
		// renders on top of the route line endpoint.
		setPriority(PRIORITY_HIGH + 0.01f);

		// Pre-render all 8 directional arrow sprites at construction time
		for (int i = 0; i < 8; i++)
		{
			arrowSprites[i] = buildArrowSprite(i * 45);
		}

		// Pre-render destination icon variants at construction time
		npcIcon = buildDestinationIcon(IconVariant.NPC);
		objectIcon = buildDestinationIcon(IconVariant.OBJECT);
		tileIcon = buildDestinationIcon(IconVariant.TILE);
	}

	// -------------------------------------------------------------------------
	// Public API (called by GuidanceOverlayCoordinator)
	// -------------------------------------------------------------------------

	/**
	 * Sets the destination tile and icon type for the active guidance step.
	 *
	 * @param point    the world-map destination tile (may be null to hide)
	 * @param stepIconType icon variant based on step type (NPC, object, or tile)
	 */
	public void setTarget(WorldPoint point, StepIconType stepIconType)
	{
		this.targetPoint = point;
		this.iconType = stepIconType != null ? stepIconType : StepIconType.TILE;
	}

	/** Clears the destination, hiding this overlay. */
	public void clearTarget()
	{
		this.targetPoint = null;
	}

	/**
	 * Controls whether a {@link CollectionLogWorldMapPoint} is currently registered
	 * alongside this overlay.
	 *
	 * <p>When {@code true}, this overlay skips its off-screen edge arrow (the
	 * {@link CollectionLogWorldMapPoint}'s snap arrow handles that visual) but continues
	 * to draw the on-screen destination icon at the target tile. This prevents the
	 * double-arrow regression described in #410 while still providing the click-to-focus
	 * behaviour supplied by {@link CollectionLogWorldMapPoint#isJumpOnClick()}.
	 *
	 * @param active {@code true} when a map point is registered; {@code false} otherwise
	 */
	public void setMapPointActive(boolean active)
	{
		this.mapPointActive = active;
	}

	// -------------------------------------------------------------------------
	// Overlay render
	// -------------------------------------------------------------------------

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Snapshot volatile fields once — prevents mid-frame races
		final WorldPoint target = this.targetPoint;
		if (target == null || !config.showOverlays())
		{
			return null;
		}

		Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (mapWidget == null)
		{
			return null;
		}

		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return null;
		}

		WorldPoint playerLocation = lp.getWorldLocation();
		if (playerLocation == null)
		{
			return null;
		}

		Rectangle mapBounds = mapWidget.getBounds();

		Point targetMapPoint = mapWorldPointToGraphicsPoint(target, mapBounds);
		if (targetMapPoint == null)
		{
			// Target is outside surfaceContainsPosition — skip rendering
			return null;
		}

		boolean targetVisible = mapBounds.contains(targetMapPoint.getX(), targetMapPoint.getY());

		Shape previousClip = graphics.getClip();
		graphics.setClip(mapBounds);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (targetVisible)
		{
			renderDestinationIcon(graphics, targetMapPoint);
		}
		else if (!mapPointActive)
		{
			// Only draw the edge arrow when no CollectionLogWorldMapPoint is registered.
			// When a map point is active its own edge-snap arrow provides the direction
			// indicator; drawing here too would produce a duplicate arrow (#410).
			Point playerMapPoint = mapWorldPointToGraphicsPoint(playerLocation, mapBounds);
			renderOffScreenArrow(graphics, mapBounds, playerMapPoint, targetMapPoint);
		}

		graphics.setClip(previousClip);
		return null;
	}

	// -------------------------------------------------------------------------
	// Private rendering helpers (no allocations)
	// -------------------------------------------------------------------------

	/**
	 * Draws the destination icon at the target's map pixel position.
	 * Uses a pre-rendered sprite selected by the current {@link StepIconType}.
	 */
	private void renderDestinationIcon(Graphics2D graphics, Point targetMapPoint)
	{
		BufferedImage icon = selectIcon();
		int x = targetMapPoint.getX() - icon.getWidth() / 2;
		int y = targetMapPoint.getY() - icon.getHeight() / 2;
		graphics.drawImage(icon, x, y, null);
	}

	/**
	 * Draws a directional arrow on the map edge pointing toward the off-screen target.
	 *
	 * <p>The arrow position is computed by tracing the line from the player's map position
	 * (or the map center when the player is off-surface) toward the target, then snapping
	 * to the inset map boundary. The nearest pre-rendered 45°-increment sprite is used.
	 *
	 * <p>Per feedback_worldmap_arrow_rendering.md: never draw a clipped route line to an
	 * off-screen target — the edge-snap arrow alone indicates direction.
	 */
	private void renderOffScreenArrow(Graphics2D graphics, Rectangle mapBounds,
		Point playerMapPoint, Point targetMapPoint)
	{
		// Use either the player's on-screen map position or the map center as arrow origin
		double fromX = (playerMapPoint != null && mapBounds.contains(playerMapPoint.getX(), playerMapPoint.getY()))
			? playerMapPoint.getX()
			: mapBounds.getCenterX();
		double fromY = (playerMapPoint != null && mapBounds.contains(playerMapPoint.getX(), playerMapPoint.getY()))
			? playerMapPoint.getY()
			: mapBounds.getCenterY();

		double toX = targetMapPoint.getX();
		double toY = targetMapPoint.getY();

		double dx = toX - fromX;
		double dy = toY - fromY;

		if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001)
		{
			// Coincident points — no useful direction to draw
			return;
		}

		// Angle in screen-space radians (0 = right, positive = clockwise)
		double angleRad = Math.atan2(dy, dx);

		// Find the edge-snap position: trace the direction vector to the inset boundary
		double minX = mapBounds.getX() + EDGE_INSET;
		double minY = mapBounds.getY() + EDGE_INSET;
		double maxX = mapBounds.getX() + mapBounds.getWidth() - EDGE_INSET;
		double maxY = mapBounds.getY() + mapBounds.getHeight() - EDGE_INSET;

		// Parameterise: find the smallest t where the ray hits a boundary
		double t = Double.MAX_VALUE;
		if (dx > 0)
		{
			t = Math.min(t, (maxX - fromX) / dx);
		}
		else if (dx < 0)
		{
			t = Math.min(t, (minX - fromX) / dx);
		}
		if (dy > 0)
		{
			t = Math.min(t, (maxY - fromY) / dy);
		}
		else if (dy < 0)
		{
			t = Math.min(t, (minY - fromY) / dy);
		}

		double arrowX = Math.max(minX, Math.min(maxX, fromX + dx * t));
		double arrowY = Math.max(minY, Math.min(maxY, fromY + dy * t));

		// Select the sprite whose angle is nearest to the computed angle (8 sprites, 45° each)
		double angleDeg = Math.toDegrees(angleRad);
		if (angleDeg < 0)
		{
			angleDeg += 360.0;
		}
		int spriteIndex = (int) Math.round(angleDeg / 45.0) % 8;
		BufferedImage sprite = arrowSprites[spriteIndex];

		int drawX = (int) arrowX - sprite.getWidth() / 2;
		int drawY = (int) arrowY - sprite.getHeight() / 2;
		graphics.drawImage(sprite, drawX, drawY, null);
	}

	/** Returns the destination icon sprite matching the current step type. */
	private BufferedImage selectIcon()
	{
		switch (iconType)
		{
			case NPC:
				return npcIcon;
			case OBJECT:
				return objectIcon;
			default:
				return tileIcon;
		}
	}

	// -------------------------------------------------------------------------
	// Coordinate conversion (same algorithm as WorldMapRouteOverlay)
	// -------------------------------------------------------------------------

	/**
	 * Converts a WorldPoint to pixel coordinates on the world map widget.
	 * Returns null when the world map is not initialised or the point is off-surface.
	 */
	private Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint, Rectangle worldMapRect)
	{
		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null || worldMap.getWorldMapData() == null)
		{
			return null;
		}

		if (!worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY()))
		{
			return null;
		}

		float pixelsPerTile = worldMap.getWorldMapZoom();

		int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

		Point worldMapPosition = worldMap.getWorldMapPosition();
		if (worldMapPosition == null)
		{
			return null;
		}

		int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
		int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
		int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

		int xGraphDiff = (int) (xTileOffset * pixelsPerTile);
		int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

		yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
		xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

		yGraphDiff = worldMapRect.height - yGraphDiff;
		yGraphDiff += (int) worldMapRect.getY();
		xGraphDiff += (int) worldMapRect.getX();

		return new Point(xGraphDiff, yGraphDiff);
	}

	// -------------------------------------------------------------------------
	// Sprite builders (called once at construction, never in render loop)
	// -------------------------------------------------------------------------

	/**
	 * Builds a single directional arrow sprite pointing at the given angle.
	 * 0° = right, 90° = down (screen-space, y-axis down).
	 *
	 * <p>Per feedback_worldmap_arrow_rendering.md: fill first, then draw outline;
	 * use JOIN_ROUND / CAP_ROUND to prevent miter artifacts on narrow chevrons.
	 */
	private static BufferedImage buildArrowSprite(int degrees)
	{
		int size = ARROW_SPRITE_SIZE;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int cx = size / 2;
		int cy = size / 2;

		// Chevron pointing right at 0° — matches CollectionLogWorldMapPoint shape
		int[] xPoints = {cx + 12, cx - 6, cx - 2, cx - 6};
		int[] yPoints = {cy, cy - 9, cy, cy + 9};

		AffineTransform tx = new AffineTransform();
		tx.rotate(Math.toRadians(degrees), cx, cy);
		g.setTransform(tx);

		// Fill first, then outline (per memory guidance — fill-before-outline)
		g.setColor(FILL_COLOR);
		g.fillPolygon(xPoints, yPoints, 4);
		g.setColor(BORDER_COLOR);
		g.setStroke(ARROW_STROKE);
		g.drawPolygon(xPoints, yPoints, 4);

		g.dispose();
		return img;
	}

	/**
	 * Builds a destination icon sprite for the given variant.
	 *
	 * <ul>
	 *   <li>NPC — person silhouette (head circle + body triangle)</li>
	 *   <li>OBJECT — chest (rectangle with lid line and keyhole)</li>
	 *   <li>TILE — diamond tile marker (Quest Helper style)</li>
	 * </ul>
	 */
	private static BufferedImage buildDestinationIcon(IconVariant variant)
	{
		int size = ICON_SIZE;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int cx = size / 2;
		int cy = size / 2;

		switch (variant)
		{
			case NPC:
				// Head circle
				g.setColor(FILL_COLOR);
				g.fillOval(cx - 3, cy - 8, 6, 6);
				g.setColor(BORDER_COLOR);
				g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.drawOval(cx - 3, cy - 8, 6, 6);
				// Body triangle (torso)
				int[] bx = {cx - 5, cx + 5, cx};
				int[] by = {cy + 5, cy + 5, cy - 2};
				g.setColor(FILL_COLOR);
				g.fillPolygon(bx, by, 3);
				g.setColor(BORDER_COLOR);
				g.setStroke(ICON_STROKE);
				g.drawPolygon(bx, by, 3);
				break;

			case OBJECT:
				// Chest body
				int chestX = cx - 6;
				int chestY = cy - 4;
				int chestW = 12;
				int chestH = 8;
				g.setColor(FILL_COLOR);
				g.fillRect(chestX, chestY, chestW, chestH);
				g.setColor(BORDER_COLOR);
				g.setStroke(ICON_STROKE);
				g.drawRect(chestX, chestY, chestW, chestH);
				// Lid divider line
				g.drawLine(chestX, chestY + 3, chestX + chestW, chestY + 3);
				// Keyhole dot
				g.fillOval(cx - 1, chestY + 4, 3, 3);
				break;

			case TILE:
			default:
				// Diamond tile marker
				int[] dx = {cx, cx + 7, cx, cx - 7};
				int[] dy = {cy - 7, cy, cy + 7, cy};
				g.setColor(FILL_COLOR);
				g.fillPolygon(dx, dy, 4);
				g.setColor(BORDER_COLOR);
				g.setStroke(ICON_STROKE);
				g.drawPolygon(dx, dy, 4);
				break;
		}

		g.dispose();
		return img;
	}

	// -------------------------------------------------------------------------
	// Supporting enums
	// -------------------------------------------------------------------------

	/** Determines which destination icon sprite is drawn at the target tile. */
	public enum StepIconType
	{
		/** Step targets an NPC (has npcId). */
		NPC,
		/** Step targets a game object (has objectId). */
		OBJECT,
		/** Generic tile destination. */
		TILE
	}

	/** Internal discriminant for {@link #buildDestinationIcon}. */
	private enum IconVariant
	{
		NPC,
		OBJECT,
		TILE
	}
}
