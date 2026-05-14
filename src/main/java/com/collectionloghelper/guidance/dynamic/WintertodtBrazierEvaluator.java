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
package com.collectionloghelper.guidance.dynamic;

import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.guidance.DynamicTargetEvaluator;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/**
 * Dynamic target evaluator for the Wintertodt minigame.
 *
 * <p>Returns the world location of the nearest active (lit) brazier NPC each
 * game tick.  Active braziers are identified by NPC IDs 29312 and 29313
 * (the lit states observed in the Wintertodt Prison region per the OSRS Wiki
 * <a href="https://oldschool.runescape.wiki/w/Brazier">Brazier</a> page).
 * Broken/unlit brazier IDs (29314, 31926) are intentionally excluded so that
 * the overlay only points players toward braziers they should be tending.
 *
 * <p>When no active brazier is visible in the current WorldView, {@code null}
 * is returned and the coordinator falls back to the step's static coordinates
 * (if any).
 *
 * <p>Registered in {@link DynamicTargetEvaluatorRegistry} under the key
 * {@code "wintertodt_active_brazier"}.
 */
public class WintertodtBrazierEvaluator implements DynamicTargetEvaluator
{
	/**
	 * NPC ID for a lit (active) brazier — standard form.
	 * Source: https://oldschool.runescape.wiki/w/Brazier
	 */
	static final int BRAZIER_LIT_1 = 29312;

	/**
	 * NPC ID for a lit (active) brazier — alternate/damaged-but-burning form.
	 * Source: https://oldschool.runescape.wiki/w/Brazier
	 */
	static final int BRAZIER_LIT_2 = 29313;

	@Override
	@Nullable
	public WorldPoint evaluate(Client client, GuidanceStep step)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return null;
		}

		NPC nearest = null;
		int nearestDist = Integer.MAX_VALUE;

		WorldPoint playerLocation = null;
		net.runelite.api.Player lp = client.getLocalPlayer();
		if (lp != null)
		{
			playerLocation = lp.getWorldLocation();
		}

		for (NPC npc : wv.npcs())
		{
			if (npc == null)
			{
				continue;
			}
			int id = npc.getId();
			if (id != BRAZIER_LIT_1 && id != BRAZIER_LIT_2)
			{
				continue;
			}
			if (playerLocation != null)
			{
				WorldPoint loc = npc.getWorldLocation();
				if (loc != null)
				{
					int dist = playerLocation.distanceTo2D(loc);
					if (dist < nearestDist)
					{
						nearestDist = dist;
						nearest = npc;
					}
				}
			}
			else
			{
				// No player location — just return the first active brazier found
				nearest = npc;
				break;
			}
		}

		return nearest != null ? nearest.getWorldLocation() : null;
	}
}
