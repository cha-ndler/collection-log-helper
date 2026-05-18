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
package com.collectionloghelper.guidance;

import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.overlay.GuidanceOverlay;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;

/**
 * Owns the lifecycle of the currently tracked guidance NPC.
 *
 * <p>Extracted from {@link GuidanceOverlayCoordinator}: this helper holds
 * the single mutable reference to the in-world NPC the guidance overlay
 * is following, and exposes the lifecycle entry points the coordinator
 * (and its event router) need:
 * <ul>
 *   <li>{@link #onNpcSpawned(NPC, boolean, GuidanceStep, Integer)} — promote
 *       a newly spawned NPC to "tracked" when it matches the active step's
 *       target NPC ID and nothing is currently tracked.</li>
 *   <li>{@link #onNpcDespawned(NPC)} — clear tracking when the despawned
 *       NPC is the one we were following.</li>
 *   <li>{@link #scanForTrackedNpc(GuidanceStep, Integer)} — sweep the
 *       active world view for a matching NPC when a new step activates,
 *       so the overlay does not need to wait for a spawn event.</li>
 *   <li>{@link #clear()} — drop tracking unconditionally (called by the
 *       coordinator on guidance teardown).</li>
 * </ul>
 *
 * <p>Thread safety: the underlying {@code trackedGuidanceNpc} reference is
 * volatile because it is written on the client thread (via NpcSpawned /
 * NpcDespawned and {@link #scanForTrackedNpc}) but read on the EDT by
 * overlay render methods through {@link GuidanceOverlay#setTrackedNpc}.</p>
 */
@Slf4j
@Singleton
public class NpcTrackerHelper
{
	private final Client client;
	private final ClientThread clientThread;
	private final GuidanceOverlay guidanceOverlay;

	/**
	 * Tracked NPC for guidance overlay. Written on client thread via
	 * NpcSpawned / NpcDespawned and {@link #scanForTrackedNpc}, read on
	 * EDT by overlay render. Must remain volatile.
	 */
	private volatile NPC trackedGuidanceNpc;

	@Inject
	NpcTrackerHelper(Client client, ClientThread clientThread, GuidanceOverlay guidanceOverlay)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.guidanceOverlay = guidanceOverlay;
	}

	/**
	 * Handles an NPC spawn event.  When guidance is active, nothing is
	 * currently tracked, and the spawned NPC matches the active step's
	 * resolved target NPC ID, promotes the NPC to "tracked" and pushes it
	 * into the guidance overlay.
	 *
	 * @param npc the spawned NPC
	 * @param guidanceActive whether the sequencer currently has an active sequence
	 * @param currentStep the active guidance step, or {@code null}
	 * @param activeTargetItemId the active target item id used to resolve
	 *                           per-item NPC overrides, or {@code null}
	 */
	public void onNpcSpawned(NPC npc, boolean guidanceActive,
		@Nullable GuidanceStep currentStep, @Nullable Integer activeTargetItemId)
	{
		if (!guidanceActive || trackedGuidanceNpc != null || currentStep == null)
		{
			return;
		}
		int resolvedNpcId = currentStep.resolveNpcId(activeTargetItemId);
		if (resolvedNpcId > 0 && npc.getId() == resolvedNpcId)
		{
			trackedGuidanceNpc = npc;
			guidanceOverlay.setTrackedNpc(npc);
		}
	}

	/**
	 * Handles an NPC despawn event.  Clears tracking when the despawned NPC
	 * is the one currently being tracked.
	 */
	public void onNpcDespawned(NPC npc)
	{
		if (npc == trackedGuidanceNpc)
		{
			trackedGuidanceNpc = null;
			guidanceOverlay.setTrackedNpc(null);
		}
	}

	/**
	 * Scans the active world view for an NPC matching the given step's
	 * resolved target NPC ID.  Called once when a new step activates to
	 * seed the tracked NPC reference without waiting for a spawn event.
	 *
	 * <p>The scan itself runs on the client thread via
	 * {@link ClientThread#invokeLater}; the synchronous prelude clears any
	 * existing tracking so the overlay does not briefly point at a stale
	 * NPC from the previous step.</p>
	 */
	public void scanForTrackedNpc(@Nullable GuidanceStep step, @Nullable Integer activeTargetItemId)
	{
		trackedGuidanceNpc = null;
		guidanceOverlay.setTrackedNpc(null);

		final int resolvedNpcId = step == null ? 0 : step.resolveNpcId(activeTargetItemId);
		if (resolvedNpcId <= 0)
		{
			return;
		}

		final int targetNpcId = resolvedNpcId;
		clientThread.invokeLater(() ->
		{
			WorldView wv = client.getTopLevelWorldView();
			if (wv == null)
			{
				return;
			}
			for (NPC npc : wv.npcs())
			{
				if (npc != null && npc.getId() == targetNpcId)
				{
					trackedGuidanceNpc = npc;
					guidanceOverlay.setTrackedNpc(npc);
					break;
				}
			}
		});
	}

	/**
	 * Drops tracking unconditionally.  Called by the coordinator on guidance
	 * teardown ({@code deactivateGuidance} / {@code clearGuidanceOverlays})
	 * before the fresh scan that {@link #scanForTrackedNpc} performs for the
	 * next step.
	 *
	 * <p>Does <em>not</em> push {@code setTrackedNpc(null)} to the overlay —
	 * the coordinator already clears the overlay via
	 * {@link GuidanceOverlay#clearTarget} on teardown, and
	 * {@link #scanForTrackedNpc} applies its own overlay clear before the
	 * new scan.</p>
	 */
	public void clear()
	{
		trackedGuidanceNpc = null;
	}

	/**
	 * Returns the currently tracked NPC, or {@code null} when nothing is
	 * being tracked.  Exposed primarily for tests; production overlay
	 * rendering goes through {@link GuidanceOverlay} instead.
	 */
	@Nullable
	public NPC getTrackedGuidanceNpc()
	{
		return trackedGuidanceNpc;
	}
}
