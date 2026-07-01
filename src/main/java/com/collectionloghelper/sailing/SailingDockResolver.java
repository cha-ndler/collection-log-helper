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
package com.collectionloghelper.sailing;

import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Resolves the dock of the player's <em>best</em> owned Sailing boat so guidance
 * for port-based Sailing sources can point at the port where the player's ship
 * actually is, rather than a fixed anchor.
 *
 * <p>OSRS exposes up to five boat slots via varbits. For each slot the game
 * stores whether it is owned, its type/tier, its stored max HP, and its current
 * port index. "Best" is defined as the highest boat type/tier, tie-broken by the
 * higher stored max HP. The winning boat's port index is mapped to a dock
 * {@link WorldPoint} via {@link SailingPort}.
 *
 * <p>All reads use raw varbit ids ({@link Client#getVarbitValue(int)}) so the
 * feature works regardless of whether the pinned client's {@code VarbitID} class
 * names these constants. Varbit ids are from RuneLite {@code VarbitID.java}
 * (cache rev 2026-06-25): boat 1 OWNED/TYPE/PORT = 19258/19259/19260, stride 38
 * between slots; STORED_MAXHP = 19463..19467.
 *
 * <p>{@link #resolveBestShipDock()} reads client state and must be called on the
 * client thread.
 */
@Slf4j
@Singleton
public class SailingDockResolver
{
	private static final int BOAT_SLOTS = 5;
	private static final int OWNED_BASE = 19258;
	private static final int TYPE_BASE = 19259;
	private static final int PORT_BASE = 19260;
	private static final int SLOT_STRIDE = 38;
	private static final int MAXHP_BASE = 19463;

	/**
	 * Canonical Port Piscarilius sailing-dock anchor (cache-confirmed) that
	 * port-based Sailing sources use as the static first-step target in
	 * drop_rates.json. A first step landing here is the "travel to your sailing
	 * port" step and is eligible for the dynamic dock override.
	 */
	// NB: this static-data sentinel (1824,3691) is intentionally NOT the same tile
	// as {@link SailingPort#PORT_PISCARILIUS}'s dock (1845,3687). This is the value
	// authors placed in drop_rates.json as the "generic sailing port" first-step
	// target; the enum holds the actual navigation tile. Do not "reconcile" them.
	private static final int ANCHOR_X = 1824;
	private static final int ANCHOR_Y = 3691;
	private static final int ANCHOR_PLANE = 0;

	private final Client client;

	@Inject
	private SailingDockResolver(Client client)
	{
		this.client = client;
	}

	/**
	 * Resolves the dock coordinate of the player's best owned Sailing boat.
	 *
	 * <p>Must run on the client thread (reads varbits).
	 *
	 * @return the best boat's dock tile, or empty if the player owns no boat or
	 *         the boat's port index is unknown
	 */
	public Optional<WorldPoint> resolveBestShipDock()
	{
		int bestType = -1;
		int bestMaxHp = -1;
		int bestPortId = -1;

		for (int slot = 0; slot < BOAT_SLOTS; slot++)
		{
			if (client.getVarbitValue(OWNED_BASE + slot * SLOT_STRIDE) <= 0)
			{
				continue;
			}

			int type = client.getVarbitValue(TYPE_BASE + slot * SLOT_STRIDE);
			int maxHp = client.getVarbitValue(MAXHP_BASE + slot);

			if (type > bestType || (type == bestType && maxHp > bestMaxHp))
			{
				bestType = type;
				bestMaxHp = maxHp;
				bestPortId = client.getVarbitValue(PORT_BASE + slot * SLOT_STRIDE);
			}
		}

		if (bestPortId < 0)
		{
			return Optional.empty();
		}

		Optional<SailingPort> port = SailingPort.fromId(bestPortId);
		if (!port.isPresent())
		{
			log.debug("Best Sailing boat is at unknown port index {}", bestPortId);
		}
		return port.map(SailingPort::getDock);
	}

	/**
	 * Computes a dynamic-dock coordinate override for a guidance step, or empty if
	 * the step is not an eligible Sailing first step or the player owns no boat.
	 *
	 * <p>Eligible = the first step (index 0) of a port-based Sailing source, i.e.
	 * an {@code ARRIVE_AT_TILE} step whose static target is the canonical Port
	 * Piscarilius sailing-dock anchor. Such sources currently point every player at
	 * Piscarilius; this redirects them to the dock of their best owned boat.
	 *
	 * <p>Must run on the client thread (reads varbits).
	 *
	 * @param step  the guidance step being resolved
	 * @param index the step's zero-based index in the sequence
	 * @return the best-boat dock to use for this step, or empty to keep the static target
	 */
	public Optional<WorldPoint> resolveFirstStepOverride(GuidanceStep step, int index)
	{
		if (index != 0 || step == null)
		{
			return Optional.empty();
		}
		if (step.getCompletionCondition() != CompletionCondition.ARRIVE_AT_TILE)
		{
			return Optional.empty();
		}
		if (step.getWorldX() != ANCHOR_X || step.getWorldY() != ANCHOR_Y || step.getWorldPlane() != ANCHOR_PLANE)
		{
			return Optional.empty();
		}
		return resolveBestShipDock();
	}
}
