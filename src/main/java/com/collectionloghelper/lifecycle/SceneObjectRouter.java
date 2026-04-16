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

import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.eventbus.Subscribe;

/**
 * Routes scene-object spawn/despawn events for wall, decorative, and ground
 * objects straight to the ObjectHighlightOverlay. These handlers were pure
 * pass-throughs in the plugin and share no state with other plugin logic,
 * so extracting them is behavior-preserving.
 *
 * GameObjectSpawned/Despawned are intentionally NOT routed here because
 * those handlers are interleaved with authoring-log side-effects that live
 * in the plugin.
 *
 * Must be registered on the EventBus via eventBus.register(this) in
 * Plugin.startUp() and unregistered in Plugin.shutDown() to match the
 * plugin's own @Subscribe lifecycle.
 */
@Singleton
public class SceneObjectRouter
{
	private final ObjectHighlightOverlay objectHighlightOverlay;

	@Inject
	public SceneObjectRouter(ObjectHighlightOverlay objectHighlightOverlay)
	{
		this.objectHighlightOverlay = objectHighlightOverlay;
	}

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
}
