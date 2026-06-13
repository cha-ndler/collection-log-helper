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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

/**
 * Records what the guidance overlays actually DREW each rendered frame, so the
 * dev actuation bridge can read the real on-screen draw-state deterministically
 * (no OCR). Overlays only know what they draw during their render pass — the
 * highlighted-id sets, the book-marker decision, the tile poly, and the tooltip
 * are transient locals of that call — so a read that runs off the render pass
 * cannot recompute them. This recorder captures them as a side effect of
 * rendering and publishes an immutable snapshot via a single volatile reference.
 *
 * <p><b>Disabled by default — production pays nothing.</b> Recording is gated on
 * a {@code volatile boolean} that ONLY the dev harness ever flips on. While
 * disabled, every {@link #beginFrame}, {@code record*}, and {@link #publish}
 * call is a single boolean check that returns immediately — zero allocation, the
 * accumulation buffers are never touched, and the published snapshot stays at the
 * empty sentinel. Behaviour in a shipped build is therefore unchanged in effect.
 *
 * <p><b>Concurrency.</b> The accumulation buffers are touched ONLY by the render
 * thread, ONLY between {@link #beginFrame} and {@link #publish}. {@link #latest}
 * reads a single volatile reference to an immutable {@link Snapshot}; it takes no
 * lock and never blocks the render thread. A reader that races a frame in
 * progress sees the previous complete snapshot, never a half-built one.
 */
@Singleton
public final class RenderStateRecorder
{
	/** Immutable per-frame snapshot of what the guidance overlays drew. */
	public static final class Snapshot
	{
		public final int snapshotTick;
		public final boolean guidanceActive;
		public final int[] highlightedNpcIds;
		public final int[] highlightedObjectIds;
		public final int[] highlightedGroundItemIds;
		public final List<Target> targets;
		public final WorldPoint tileTarget;
		public final String tooltipText;

		Snapshot(int snapshotTick, boolean guidanceActive, int[] highlightedNpcIds,
			int[] highlightedObjectIds, int[] highlightedGroundItemIds, List<Target> targets,
			WorldPoint tileTarget, String tooltipText)
		{
			this.snapshotTick = snapshotTick;
			this.guidanceActive = guidanceActive;
			this.highlightedNpcIds = highlightedNpcIds;
			this.highlightedObjectIds = highlightedObjectIds;
			this.highlightedGroundItemIds = highlightedGroundItemIds;
			this.targets = targets;
			this.tileTarget = tileTarget;
			this.tooltipText = tooltipText;
		}
	}

	/** A single drawn guidance target: its kind, id, book-marker state, and z-offset. */
	public static final class Target
	{
		public final String type;
		public final int id;
		public final boolean bookMarker;
		public final int zOffset;

		Target(String type, int id, boolean bookMarker, int zOffset)
		{
			this.type = type;
			this.id = id;
			this.bookMarker = bookMarker;
			this.zOffset = zOffset;
		}
	}

	private static final Snapshot EMPTY = new Snapshot(
		-1, false, new int[0], new int[0], new int[0],
		Collections.emptyList(), null, null);

	/**
	 * Recording master switch. Only the dev harness flips this on. When false,
	 * every record/begin/publish call is a single volatile read and returns.
	 */
	private volatile boolean enabled;

	// Per-frame accumulation buffers — touched only by the render thread between
	// beginFrame() and publish(). Never read by latest().
	private final List<Integer> npcIds = new ArrayList<>();
	private final List<Integer> objectIds = new ArrayList<>();
	private final List<Integer> groundItemIds = new ArrayList<>();
	private final List<Target> targets = new ArrayList<>();
	private WorldPoint tileTarget;
	private String tooltipText;
	private boolean active;

	/** Published snapshot — safely published to readers by the volatile write. */
	private volatile Snapshot latest = EMPTY;

	/** Enables recording. Idempotent. Called only by the dev harness startUp. */
	public void enable()
	{
		this.enabled = true;
	}

	/**
	 * Disables recording and resets the published snapshot to the empty sentinel,
	 * so a later {@link #latest} after the harness shuts down reports no frame.
	 * Idempotent.
	 */
	public void disable()
	{
		this.enabled = false;
		this.latest = EMPTY;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	/** Starts a new frame's accumulation. No-op (single boolean check) when disabled. */
	public void beginFrame(boolean guidanceActive)
	{
		if (!enabled)
		{
			return;
		}
		npcIds.clear();
		objectIds.clear();
		groundItemIds.clear();
		targets.clear();
		tileTarget = null;
		tooltipText = null;
		active = guidanceActive;
	}

	public void recordNpc(int id)
	{
		if (!enabled)
		{
			return;
		}
		npcIds.add(id);
	}

	public void recordObject(int id)
	{
		if (!enabled)
		{
			return;
		}
		objectIds.add(id);
	}

	public void recordGroundItem(int id)
	{
		if (!enabled)
		{
			return;
		}
		groundItemIds.add(id);
	}

	public void recordTarget(String type, int id, boolean bookMarker, int zOffset)
	{
		if (!enabled)
		{
			return;
		}
		targets.add(new Target(type, id, bookMarker, zOffset));
	}

	public void recordTile(WorldPoint p)
	{
		if (!enabled)
		{
			return;
		}
		tileTarget = p;
	}

	public void recordTooltip(String t)
	{
		if (!enabled)
		{
			return;
		}
		tooltipText = t;
	}

	/**
	 * Swaps the accumulated frame in as the new published snapshot. No-op (single
	 * boolean check) when disabled. The new {@link Snapshot} is immutable, so the
	 * volatile write safely publishes it to {@link #latest} readers.
	 */
	public void publish(int clientTick)
	{
		if (!enabled)
		{
			return;
		}
		latest = new Snapshot(clientTick, active,
			toIntArray(npcIds), toIntArray(objectIds), toIntArray(groundItemIds),
			List.copyOf(targets), tileTarget, tooltipText);
	}

	/** Returns the most recently published immutable snapshot (the empty sentinel until the first publish). */
	public Snapshot latest()
	{
		return latest;
	}

	private static int[] toIntArray(List<Integer> src)
	{
		int[] out = new int[src.size()];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = src.get(i);
		}
		return out;
	}
}
