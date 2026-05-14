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
package com.collectionloghelper.data;

import lombok.Value;

/**
 * An ordered waypoint tile that a player must cross to satisfy a guidance step's
 * tile-sequence path (B2). Used exclusively by {@link GuidanceStep#getWaypoints()}.
 *
 * <p>This type is distinct from {@link Waypoint}, which models named travel
 * waypoints with source requirements. {@code StepWaypoint} is a lightweight
 * positional checkpoint: a tile coordinate plus the radius (in tiles) within
 * which the player must stand for the waypoint to count as crossed.
 *
 * <p>Gson deserialises this from JSON objects inside the {@code waypoints} array:
 * <pre>
 * { "worldX": 1435, "worldY": 3128, "plane": 0, "radius": 10 }
 * </pre>
 */
@Value
public class StepWaypoint
{
	/** World X coordinate of this waypoint tile. */
	int worldX;

	/** World Y coordinate of this waypoint tile. */
	int worldY;

	/**
	 * Game plane (floor) for this waypoint (0 = surface, 1-3 = upper floors /
	 * dungeon floors). Player must be on this plane for the waypoint to register.
	 */
	int plane;

	/**
	 * Crossing radius in tiles. The player must come within this many tiles of
	 * {@link #worldX}/{@link #worldY} (Chebyshev / distanceTo2D) while on
	 * {@link #plane} for the waypoint to count as crossed.
	 */
	int radius;
}
