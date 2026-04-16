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

import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.GuidanceInfoBox;
import javax.inject.Singleton;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

/**
 * Holds the 5 mutable guidance UI fields that were previously scattered across
 * CollectionLogHelperPlugin. Extracted here so guidance state has a single home
 * and the plugin class is smaller.
 *
 * <p>Thread-safety note: {@link #trackedGuidanceNpc} is {@code volatile} because
 * it is written on the client thread (NpcSpawned/NpcDespawned events and
 * scanForTrackedNpc) and read on the EDT by overlay render methods.
 */
@Singleton
public class GuidanceUIState
{
	private CollectionLogWorldMapPoint activeMapPoint;
	private GuidanceInfoBox activeInfoBox;

	/**
	 * Written on client thread, read on EDT by overlay render — must stay volatile.
	 */
	private volatile NPC trackedGuidanceNpc;

	private int lastMessagedStepIndex = -1;
	private WorldPoint pendingShortestPathTarget;

	public CollectionLogWorldMapPoint getActiveMapPoint()
	{
		return activeMapPoint;
	}

	public void setActiveMapPoint(CollectionLogWorldMapPoint activeMapPoint)
	{
		this.activeMapPoint = activeMapPoint;
	}

	public GuidanceInfoBox getActiveInfoBox()
	{
		return activeInfoBox;
	}

	public void setActiveInfoBox(GuidanceInfoBox activeInfoBox)
	{
		this.activeInfoBox = activeInfoBox;
	}

	public NPC getTrackedGuidanceNpc()
	{
		return trackedGuidanceNpc;
	}

	public void setTrackedGuidanceNpc(NPC trackedGuidanceNpc)
	{
		this.trackedGuidanceNpc = trackedGuidanceNpc;
	}

	public int getLastMessagedStepIndex()
	{
		return lastMessagedStepIndex;
	}

	public void setLastMessagedStepIndex(int lastMessagedStepIndex)
	{
		this.lastMessagedStepIndex = lastMessagedStepIndex;
	}

	public WorldPoint getPendingShortestPathTarget()
	{
		return pendingShortestPathTarget;
	}

	public void setPendingShortestPathTarget(WorldPoint pendingShortestPathTarget)
	{
		this.pendingShortestPathTarget = pendingShortestPathTarget;
	}

	/**
	 * Resets all 5 guidance UI fields to their default/null values.
	 * Call when deactivating guidance or shutting down the plugin.
	 */
	public void clearAll()
	{
		activeMapPoint = null;
		activeInfoBox = null;
		trackedGuidanceNpc = null;
		lastMessagedStepIndex = -1;
		pendingShortestPathTarget = null;
	}
}
