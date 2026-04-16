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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Routes scene-level spawn/despawn events to the appropriate overlay.
 * Covers wall, decorative, ground, and game objects (to ObjectHighlightOverlay)
 * plus ground items (to GroundItemHighlightOverlay).
 *
 * <p>These handlers were pure pass-throughs in the plugin and share no mutable
 * state with other plugin logic. Authoring-log side-effects are delegated to
 * a {@link Consumer} callback set via {@link #setAuthoringLogger(Consumer)}.
 *
 * <p>Must be registered on the EventBus via {@code eventBus.register(this)}
 * in Plugin.startUp() and unregistered in Plugin.shutDown().
 */
@Singleton
public class SceneEventRouter
{
	private final ObjectHighlightOverlay objectHighlightOverlay;
	private final GroundItemHighlightOverlay groundItemHighlightOverlay;
	private final CollectionLogHelperConfig config;

	/**
	 * Optional authoring-log callback. When non-null and authoring mode is
	 * enabled, scene events are forwarded as formatted strings for logging.
	 */
	private Consumer<String> authoringLogger;

	@Inject
	public SceneEventRouter(
		ObjectHighlightOverlay objectHighlightOverlay,
		GroundItemHighlightOverlay groundItemHighlightOverlay,
		CollectionLogHelperConfig config)
	{
		this.objectHighlightOverlay = objectHighlightOverlay;
		this.groundItemHighlightOverlay = groundItemHighlightOverlay;
		this.config = config;
	}

	/**
	 * Sets the authoring-log callback used when guidance authoring mode is
	 * enabled. Pass {@code null} to disable logging.
	 */
	public void setAuthoringLogger(Consumer<String> logger)
	{
		this.authoringLogger = logger;
	}

	// ── Wall objects ────────────────────────────────────────────────────

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getWallObject());
	}

	// ── Decorative objects ──────────────────────────────────────────────

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getDecorativeObject());
	}

	// ── Ground objects ──────────────────────────────────────────────────

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getGroundObject());
	}

	// ── Game objects ────────────────────────────────────────────────────

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		objectHighlightOverlay.onObjectSpawned(event.getGameObject());

		if (config.guidanceAuthoring() && authoringLogger != null)
		{
			WorldPoint wp = event.getTile().getWorldLocation();
			authoringLogger.accept(String.format("OBJECT_SPAWN id=%d at=[%d,%d,%d]",
				event.getGameObject().getId(), wp.getX(), wp.getY(), wp.getPlane()));
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		objectHighlightOverlay.onObjectDespawned(event.getGameObject());

		if (config.guidanceAuthoring() && authoringLogger != null)
		{
			WorldPoint wp = event.getTile().getWorldLocation();
			authoringLogger.accept(String.format("OBJECT_DESPAWN id=%d at=[%d,%d,%d]",
				event.getGameObject().getId(), wp.getX(), wp.getY(), wp.getPlane()));
		}
	}

	// ── Ground items ────────────────────────────────────────────────────

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		groundItemHighlightOverlay.onItemSpawned(event.getItem(), event.getTile());

		if (config.guidanceAuthoring() && authoringLogger != null)
		{
			WorldPoint wp = event.getTile().getWorldLocation();
			authoringLogger.accept(String.format("GROUND_ITEM_SPAWN id=%d qty=%d at=[%d,%d,%d]",
				event.getItem().getId(), event.getItem().getQuantity(),
				wp.getX(), wp.getY(), wp.getPlane()));
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		groundItemHighlightOverlay.onItemDespawned(event.getItem());

		if (config.guidanceAuthoring() && authoringLogger != null)
		{
			WorldPoint wp = event.getTile().getWorldLocation();
			authoringLogger.accept(String.format("GROUND_ITEM_DESPAWN id=%d at=[%d,%d,%d]",
				event.getItem().getId(), wp.getX(), wp.getY(), wp.getPlane()));
		}
	}
}
