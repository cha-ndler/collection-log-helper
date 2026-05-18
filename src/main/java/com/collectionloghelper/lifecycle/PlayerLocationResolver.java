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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Resolves the local player's "real-world" {@link WorldPoint} on the client
 * thread and caches the most recent result for cross-thread readers.
 *
 * <p>Most callers can just use {@code lp.getWorldLocation()}, but two cases
 * require translation:
 * <ul>
 *   <li><b>Instanced regions</b> (raids, Royal Titans, etc.): the player's
 *   raw world coords point inside the instance template — ARRIVE_AT_TILE
 *   checks expect the underlying overworld tile. We translate via
 *   {@link WorldPoint#fromLocalInstance(Client, LocalPoint)}.</li>
 *   <li><b>Non-top-level WorldViews</b> (e.g. sailing boats): the player is
 *   inside a {@link WorldEntity} whose inner {@link WorldView} returns
 *   boat-local coordinates. We locate the matching entity and transform via
 *   {@link WorldEntity#transformToMainWorld(LocalPoint)}.</li>
 * </ul>
 *
 * <p>The cached value is {@code volatile} so guidance/authoring code reading
 * from non-client threads observes the most recent client-thread write.
 *
 * <p>Extracted from {@code CollectionLogHelperPlugin} as part of #503 to keep
 * the plugin class focused on RuneLite lifecycle wiring.
 */
@Slf4j
@Singleton
public class PlayerLocationResolver
{
	private final Client client;

	/**
	 * Player location cached on the client thread each game tick.
	 * Read by guidance sequencer and authoring log — volatile ensures visibility.
	 */
	private volatile WorldPoint cachedLocation;

	@Inject
	public PlayerLocationResolver(Client client)
	{
		this.client = client;
	}

	/**
	 * Resolves the player's real-world {@link WorldPoint}, updates the cache,
	 * and returns the resolved value. Returns {@code null} if no local player
	 * is present.
	 *
	 * <p>Must be called on the client thread.
	 */
	@Nullable
	public WorldPoint resolveAndCache()
	{
		WorldPoint resolved = resolve();
		cachedLocation = resolved;
		return resolved;
	}

	/**
	 * Returns the most recently cached player location, or {@code null} if none
	 * has been resolved (or the cache has been {@link #reset()}).
	 */
	@Nullable
	public WorldPoint getCachedLocation()
	{
		return cachedLocation;
	}

	/**
	 * Clears the cached location. Invoked on logout / profile change so stale
	 * coordinates do not leak across sessions.
	 */
	public void reset()
	{
		cachedLocation = null;
	}

	/**
	 * Resolves the player's "real-world" {@link WorldPoint}, handling the
	 * instanced-region and non-top-level WorldView cases described in the
	 * class javadoc. Falls back to plain {@code getWorldLocation()} on any
	 * error or when the required API is unavailable.
	 */
	@Nullable
	private WorldPoint resolve()
	{
		Player lp = client.getLocalPlayer();
		if (lp == null)
		{
			return null;
		}
		WorldPoint fallback = lp.getWorldLocation();
		try
		{
			WorldView playerView = lp.getWorldView();
			if (playerView == null || playerView.isTopLevel())
			{
				// Top-level view — handle instanced regions by translating
				// the player's local point back to the overworld template tile.
				// Without this, ARRIVE_AT_TILE checks in instanced bosses
				// (Royal Titans, raids, etc.) never match the JSON coords.
				if (client.isInInstancedRegion())
				{
					LocalPoint localPoint = lp.getLocalLocation();
					if (localPoint != null)
					{
						WorldPoint templatePoint = WorldPoint.fromLocalInstance(client, localPoint);
						if (templatePoint != null)
						{
							return templatePoint;
						}
					}
				}
				return fallback;
			}

			// Player is inside a non-top-level WorldView (e.g. a sailing boat).
			// Find the WorldEntity whose inner WorldView matches the player's.
			WorldView topLevel = client.getTopLevelWorldView();
			for (WorldEntity entity : topLevel.worldEntities())
			{
				if (entity.getWorldView() == playerView)
				{
					LocalPoint playerLocal = lp.getLocalLocation();
					LocalPoint mainLocal = entity.transformToMainWorld(playerLocal);
					if (mainLocal != null)
					{
						return WorldPoint.fromLocal(topLevel,
							mainLocal.getX(), mainLocal.getY(),
							topLevel.getPlane());
					}
					break;
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Player-location resolution failed, using fallback", e);
		}
		return fallback;
	}
}
