/*
 * Copyright (c) 2025, Chandler
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
import java.awt.Shape;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Slf4j
@Singleton
public class ObjectHighlightOverlay extends Overlay
{
	private static final int TEXT_HEIGHT_OFFSET = 130;
	private static final BasicStroke STROKE_1_5 = new BasicStroke(1.5f);
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);

	private final Client client;
	private final ClientThread clientThread;
	private final CollectionLogHelperConfig config;

	@Inject
	private TooltipManager tooltipManager;

	private volatile Set<Integer> targetObjectIds = Collections.emptySet();
	private volatile String objectInteractAction;
	private volatile boolean useItemOnObject;
	private volatile String tooltipText;
	private volatile WorldPoint filterTile;
	private volatile int filterMaxDistance;
	private volatile List<WorldPoint> filterTiles;
	private volatile String cachedDisplayText;

	/**
	 * Cached list of scene objects matching targetObjectIds.
	 * Populated by a one-time scan when targets change, then maintained
	 * incrementally via onObjectSpawned/onObjectDespawned events forwarded
	 * from the plugin. This avoids scanning 10k+ tiles every frame.
	 */
	private volatile List<TileObject> matchedObjects = Collections.emptyList();

	@Inject
	private ObjectHighlightOverlay(Client client, ClientThread clientThread, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public void setTargetObjectId(int objectId)
	{
		this.targetObjectIds = objectId > 0 ? Set.of(objectId) : Collections.emptySet();
		clientThread.invokeLater(this::rescanScene);
	}

	public void setTargetObjectIds(Set<Integer> objectIds)
	{
		this.targetObjectIds = objectIds != null ? objectIds : Collections.emptySet();
		clientThread.invokeLater(this::rescanScene);
	}

	public void setObjectInteractAction(String action)
	{
		this.objectInteractAction = action;
		rebuildDisplayText(action, this.useItemOnObject);
	}

	public void setUseItemOnObject(boolean value)
	{
		this.useItemOnObject = value;
		rebuildDisplayText(this.objectInteractAction, value);
	}

	public void setTooltipText(String text)
	{
		this.tooltipText = text;
	}

	/**
	 * Sets an optional tile-distance filter. When filterTile is non-null and maxDistance > 0,
	 * only objects within maxDistance tiles of filterTile are highlighted.
	 * Must be called BEFORE setTargetObjectIds so the filter is active during rescan.
	 */
	public void setObjectFilter(WorldPoint tile, int maxDistance)
	{
		this.filterTile = tile;
		this.filterMaxDistance = maxDistance;
		this.filterTiles = null;
	}

	/**
	 * Sets an exact-tile filter. Only objects whose world location matches one of the
	 * given tiles are highlighted. Overrides the distance-based filter.
	 * Must be called BEFORE setTargetObjectIds so the filter is active during rescan.
	 */
	public void setObjectFilterTiles(List<WorldPoint> tiles)
	{
		this.filterTiles = tiles;
		if (tiles != null && !tiles.isEmpty())
		{
			// Clear distance filter when using exact tiles
			this.filterTile = null;
			this.filterMaxDistance = 0;
		}
	}

	public void clearTarget()
	{
		this.targetObjectIds = Collections.emptySet();
		this.objectInteractAction = null;
		this.useItemOnObject = false;
		this.tooltipText = null;
		this.filterTile = null;
		this.filterMaxDistance = 0;
		this.filterTiles = null;
		this.matchedObjects = Collections.emptyList();
		this.cachedDisplayText = null;
	}

	private void rebuildDisplayText(String action, boolean useItem)
	{
		if (action != null)
		{
			cachedDisplayText = useItem ? "Use " + action + " \u2192" : action;
		}
		else
		{
			cachedDisplayText = null;
		}
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
		if (!passesTileFilter(obj))
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
						if (go != null && ids.contains(go.getId()) && passesTileFilter(go))
						{
							found.add(go);
						}
					}
				}
				WallObject wo = tile.getWallObject();
				if (wo != null && ids.contains(wo.getId()) && passesTileFilter(wo))
				{
					found.add(wo);
				}
				DecorativeObject deco = tile.getDecorativeObject();
				if (deco != null && ids.contains(deco.getId()) && passesTileFilter(deco))
				{
					found.add(deco);
				}
				GroundObject ground = tile.getGroundObject();
				if (ground != null && ids.contains(ground.getId()) && passesTileFilter(ground))
				{
					found.add(ground);
				}
			}
		}
		log.debug("rescanScene: looking for {} on plane {}, found {} matches", ids, plane, found.size());
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

	private boolean passesTileFilter(TileObject obj)
	{
		// Exact tile filter takes priority
		List<WorldPoint> exactTiles = filterTiles;
		if (exactTiles != null && !exactTiles.isEmpty())
		{
			WorldPoint objWorld = obj.getWorldLocation();
			if (objWorld == null)
			{
				return false;
			}
			for (WorldPoint allowed : exactTiles)
			{
				if (objWorld.distanceTo2D(allowed) <= 1 && objWorld.getPlane() == allowed.getPlane())
				{
					return true;
				}
			}
			return false;
		}

		// Distance-based filter
		WorldPoint tile = filterTile;
		int maxDist = filterMaxDistance;
		if (tile == null || maxDist <= 0)
		{
			return true;
		}
		WorldPoint objWorld = obj.getWorldLocation();
		if (objWorld == null)
		{
			return false;
		}
		return objWorld.distanceTo2D(tile) <= maxDist;
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
			graphics.setColor(overlayColor);
			graphics.setStroke(STROKE_1_5);
			graphics.draw(hull);

			// Show tooltip when mouse hovers over the object hull (once per frame)
			if (!tooltipAlreadyShown && builtTooltip != null
				&& mousePos != null && hull.contains(mousePos.getX(), mousePos.getY()))
			{
				tooltipManager.add(new Tooltip(builtTooltip));
				showedTooltip = true;
			}
		}

		// Render action text above the object
		String displayText = this.cachedDisplayText;
		if (localPoint != null && displayText != null)
		{
			Point textPoint = Perspective.getCanvasTextLocation(
				client, graphics, localPoint, displayText, TEXT_HEIGHT_OFFSET);
			if (textPoint != null)
			{
				OverlayUtil.renderTextLocation(graphics, textPoint, displayText, overlayColor);
			}
		}
		return showedTooltip;
	}
}
