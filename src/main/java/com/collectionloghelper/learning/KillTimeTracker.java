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
package com.collectionloghelper.learning;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.DropRateDatabase;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.eventbus.Subscribe;

/**
 * Observes actual NPC kill cycles and maintains a per-source rolling average
 * kill time, stored in the per-character data directory.
 *
 * <p>Only active when {@link CollectionLogHelperConfig#learnKillTimes()} is {@code true}.
 * The rolling window is {@value #WINDOW_SIZE} kills; earlier observations are discarded
 * once the window is full.
 *
 * <p>Timestamps are wall-clock milliseconds obtained from {@link System#currentTimeMillis()}.
 * Kill time is measured as the elapsed time between two successive kills of any NPC
 * that maps to the same {@link com.collectionloghelper.data.CollectionLogSource}.
 *
 * <h2>Attribution (issue #481)</h2>
 * Only deaths of NPCs the local player has personally damaged are recorded.
 * {@link HitsplatApplied} events with {@link Hitsplat#isMine()} mark the NPC's
 * scene index as "tagged by me"; on {@link ActorDeath} the kill is only recorded
 * if the NPC's index is in the tagged set. This prevents group content
 * (CoX/ToB/ToA, Wintertodt, Tempoross, GWD masses) from poisoning the personal
 * rolling average with other players' kills.
 *
 * <h2>Persistence (issue #480)</h2>
 * Disk I/O is moved off the client thread. {@code recordKill} marks the in-memory
 * state dirty and the actual file write happens on a background single-thread
 * executor, debounced by {@value #SAVE_DEBOUNCE_MS} ms so high-throughput sources
 * (Wintertodt, Tempoross, Barbarian Assault) coalesce into one write per debounce
 * window instead of one per kill.
 */
@Slf4j
@Singleton
public class KillTimeTracker
{
	/** Number of recent kill observations to keep per source. */
	static final int WINDOW_SIZE = 20;

	/** Minimum plausible kill time in seconds — guards against teleport / lag spikes. */
	static final int MIN_KILL_SECONDS = 3;

	/** Maximum plausible kill time in seconds — 30 min cap prevents stale data poisoning. */
	static final int MAX_KILL_SECONDS = 1800;

	/**
	 * Debounce window for persisting kill-time data to disk.
	 * High-throughput sources (Wintertodt) generate many kills per minute; coalescing
	 * those into a single write per window prevents the disk I/O storm that motivated #480.
	 */
	static final long SAVE_DEBOUNCE_MS = 2_000L;

	static final String DATA_FILE_NAME = "kill_times.json";

	private final CollectionLogHelperConfig config;
	private final DropRateDatabase database;
	private final Gson gson;

	/**
	 * Rolling window of kill-cycle durations (seconds) keyed by source name.
	 * Populated from the persisted JSON on load and updated in-memory on each kill.
	 */
	private final Map<String, Deque<Integer>> windows = new HashMap<>();

	/** Timestamp (ms) of the most recent kill event, keyed by source name. */
	private final Map<String, Long> lastKillTime = new HashMap<>();

	/**
	 * NPC scene indices the local player has personally damaged.
	 * Populated by {@link #onHitsplatApplied(HitsplatApplied)} and consumed
	 * (removed) by {@link #onActorDeath(ActorDeath)} to gate kill recording.
	 * Thread-safety: only mutated from the client thread.
	 */
	private final Set<Integer> playerDamagedNpcIndices = new HashSet<>();

	/** File where rolling data is persisted; set on {@link #init(File)}. */
	private File dataFile;

	/**
	 * Single-thread executor for off-client-thread persistence.
	 * Created lazily on first {@link #init(File)} so tests that drive
	 * {@link #recordKill(int, long)} without persistence don't spin up a thread.
	 */
	private ExecutorService saveExecutor;

	/** Set when state has been mutated since the last persisted snapshot. */
	private final AtomicBoolean dirty = new AtomicBoolean(false);

	/** Set while a debounced save is in-flight so we coalesce instead of queueing N writes. */
	private final AtomicBoolean saveScheduled = new AtomicBoolean(false);

	@Inject
	KillTimeTracker(CollectionLogHelperConfig config, DropRateDatabase database, Gson gson)
	{
		this.config = config;
		this.database = database;
		this.gson = new GsonBuilder().create();
	}

	/**
	 * Initialise the tracker for a specific player session by pointing it at the
	 * per-character data directory.  Loads any previously persisted data.
	 *
	 * @param characterDir per-character data directory (created by {@code PluginDataManager})
	 */
	public void init(File characterDir)
	{
		if (characterDir == null)
		{
			return;
		}
		dataFile = new File(characterDir, DATA_FILE_NAME);
		ensureExecutor();
		loadFromDisk();
	}

	/**
	 * Resets tracker state on logout / plugin shutdown.
	 *
	 * <p>Flushes any pending writes synchronously before clearing in-memory state
	 * so we don't lose the tail end of a kill session, then shuts the background
	 * executor down. Does NOT delete the persisted file — data survives across sessions.
	 */
	public void reset()
	{
		flushAndShutdownExecutor();
		windows.clear();
		lastKillTime.clear();
		playerDamagedNpcIndices.clear();
		dataFile = null;
	}

	/**
	 * Returns the learned average kill time for the given source name, or
	 * {@link OptionalInt#empty()} if no data is available (tracker disabled,
	 * not enough kills, or source not tracked).
	 *
	 * @param sourceName the {@link com.collectionloghelper.data.CollectionLogSource#getName()}
	 * @return observed average in seconds, or empty
	 */
	public OptionalInt getLearnedKillTime(String sourceName)
	{
		if (!config.learnKillTimes())
		{
			return OptionalInt.empty();
		}
		Deque<Integer> window = windows.get(sourceName);
		if (window == null || window.isEmpty())
		{
			return OptionalInt.empty();
		}
		double avg = window.stream().mapToInt(Integer::intValue).average().orElse(0);
		return avg > 0 ? OptionalInt.of((int) Math.round(avg)) : OptionalInt.empty();
	}

	/**
	 * Returns the number of kill observations recorded for the given source.
	 */
	public int getObservationCount(String sourceName)
	{
		Deque<Integer> window = windows.get(sourceName);
		return window == null ? 0 : window.size();
	}

	/**
	 * Records a kill event for the NPC that just died.
	 * Finds the source the NPC belongs to, computes elapsed seconds since the last kill
	 * of that source, and pushes the sample into the rolling window.
	 *
	 * <p>Called from {@link #onActorDeath(ActorDeath)} when the feature is enabled
	 * and the NPC was attributed to the local player.
	 * Package-visible for testing.
	 *
	 * @param npcId the RuneLite NPC id of the dying actor
	 * @param nowMs current wall-clock milliseconds
	 */
	void recordKill(int npcId, long nowMs)
	{
		String sourceName = database.getSourceNameByNpcId(npcId);
		if (sourceName == null)
		{
			return;
		}

		Long prev = lastKillTime.put(sourceName, nowMs);
		if (prev == null)
		{
			// First kill for this source this session — start the timer, no sample yet.
			return;
		}

		int elapsedSeconds = (int) ((nowMs - prev) / 1000L);
		if (elapsedSeconds < MIN_KILL_SECONDS || elapsedSeconds > MAX_KILL_SECONDS)
		{
			log.debug("KillTimeTracker: discarding outlier for '{}' — {}s", sourceName, elapsedSeconds);
			return;
		}

		Deque<Integer> window = windows.computeIfAbsent(sourceName, k -> new ArrayDeque<>(WINDOW_SIZE));
		if (window.size() >= WINDOW_SIZE)
		{
			window.pollFirst();
		}
		window.addLast(elapsedSeconds);

		log.debug("KillTimeTracker: '{}' kill time {}s — window avg {}s (n={})",
			sourceName, elapsedSeconds,
			window.stream().mapToInt(Integer::intValue).average().orElse(0),
			window.size());

		scheduleSave();
	}

	/**
	 * Marks the local player as having damaged the NPC at the given scene index.
	 *
	 * <p>Package-visible for testing — production callers go through
	 * {@link #onHitsplatApplied(HitsplatApplied)}.
	 */
	void markPlayerDamaged(int npcIndex)
	{
		playerDamagedNpcIndices.add(npcIndex);
	}

	/**
	 * Returns {@code true} if the NPC at the given scene index has at least one
	 * hitsplat attributed to the local player. Package-visible for testing.
	 */
	boolean isPlayerDamaged(int npcIndex)
	{
		return playerDamagedNpcIndices.contains(npcIndex);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!config.learnKillTimes())
		{
			return;
		}
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		Hitsplat hitsplat = event.getHitsplat();
		// Only "mine" hitsplats count toward attribution. Hitsplat.isMine() returns
		// true for damage the local player dealt (DAMAGE_ME, BLOCK_ME, POISON, etc.).
		if (hitsplat == null || !hitsplat.isMine())
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		playerDamagedNpcIndices.add(npc.getIndex());
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!config.learnKillTimes())
		{
			return;
		}
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();

		// Attribution gate (#481): only record kills where the local player landed
		// at least one hitsplat. Group content (CoX/ToB/ToA, Wintertodt, GWD masses,
		// BA) otherwise poisons the personal rolling average with other players' kills.
		boolean attributed = playerDamagedNpcIndices.remove(npc.getIndex());
		if (!attributed)
		{
			log.debug("KillTimeTracker: skipping unattributed death of npcId={} (no local-player hitsplat)",
				npc.getId());
			return;
		}

		recordKill(npc.getId(), System.currentTimeMillis());
	}

	// ── Persistence ─────────────────────────────────────────────────────────────

	private void loadFromDisk()
	{
		if (dataFile == null || !dataFile.exists())
		{
			return;
		}
		try (FileReader reader = new FileReader(dataFile))
		{
			Type type = new TypeToken<Map<String, int[]>>(){}.getType();
			Map<String, int[]> raw = gson.fromJson(reader, type);
			if (raw == null)
			{
				return;
			}
			for (Map.Entry<String, int[]> entry : raw.entrySet())
			{
				Deque<Integer> deque = new ArrayDeque<>(WINDOW_SIZE);
				for (int v : entry.getValue())
				{
					if (deque.size() >= WINDOW_SIZE)
					{
						deque.pollFirst();
					}
					deque.addLast(v);
				}
				if (!deque.isEmpty())
				{
					windows.put(entry.getKey(), deque);
				}
			}
			log.debug("KillTimeTracker: loaded {} sources from {}", windows.size(), dataFile);
		}
		catch (IOException e)
		{
			log.warn("KillTimeTracker: failed to load {}", dataFile, e);
		}
	}

	/**
	 * Marks state dirty and schedules a debounced background save (#480).
	 *
	 * <p>If a save is already scheduled within the current {@link #SAVE_DEBOUNCE_MS}
	 * window, this is a no-op — the in-flight write will pick up the latest snapshot
	 * because it reads {@link #windows} at flush time. This coalesces N kills inside
	 * one window into a single disk write, eliminating the per-kill stutter that
	 * motivated #480.
	 *
	 * <p>Falls back to a synchronous write when no executor is available — that path
	 * is only hit by unit tests that exercise {@link #recordKill(int, long)} without
	 * calling {@link #init(File)}.
	 */
	private void scheduleSave()
	{
		dirty.set(true);
		if (dataFile == null)
		{
			return;
		}
		if (saveExecutor == null || saveExecutor.isShutdown())
		{
			// No background thread available (e.g. tests). Flush inline so persistence
			// round-trip tests still pass; the production wiring always sets up the executor.
			flushIfDirty();
			return;
		}
		if (!saveScheduled.compareAndSet(false, true))
		{
			return;
		}
		try
		{
			saveExecutor.submit(() ->
			{
				try
				{
					// Debounce: wait SAVE_DEBOUNCE_MS so subsequent kills within the window
					// coalesce into one write. Reads the live windows map under no lock —
					// only mutated on the client thread, and the writer is a single thread,
					// so a momentarily-stale snapshot just means the next dirty flag flips
					// back on and we run again.
					Thread.sleep(SAVE_DEBOUNCE_MS);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					// Fall through: still attempt to flush so we don't lose data on shutdown.
				}
				finally
				{
					saveScheduled.set(false);
				}
				flushIfDirty();
			});
		}
		catch (java.util.concurrent.RejectedExecutionException e)
		{
			// Executor was shut down between the check and submit; fall back to inline write.
			saveScheduled.set(false);
			flushIfDirty();
		}
	}

	/**
	 * Writes the current state to disk if dirty. Safe to call from any thread.
	 * Package-visible for testing.
	 */
	void flushIfDirty()
	{
		if (!dirty.getAndSet(false))
		{
			return;
		}
		if (dataFile == null)
		{
			return;
		}
		// Snapshot under a brief lock on windows — the producer (client thread) may
		// be mutating concurrently with the background writer.
		Map<String, int[]> serializable = new HashMap<>();
		synchronized (windows)
		{
			for (Map.Entry<String, Deque<Integer>> entry : windows.entrySet())
			{
				int[] arr = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
				serializable.put(entry.getKey(), arr);
			}
		}
		try (FileWriter writer = new FileWriter(dataFile))
		{
			gson.toJson(serializable, writer);
		}
		catch (IOException e)
		{
			log.warn("KillTimeTracker: failed to save {}", dataFile, e);
			// Re-mark dirty so the next scheduleSave retries.
			dirty.set(true);
		}
	}

	private void ensureExecutor()
	{
		if (saveExecutor != null && !saveExecutor.isShutdown())
		{
			return;
		}
		saveExecutor = Executors.newSingleThreadExecutor(r ->
		{
			Thread t = new Thread(r, "clh-kill-time-save");
			t.setDaemon(true);
			return t;
		});
	}

	/**
	 * Synchronously drains any pending save and shuts down the background executor.
	 * Called from {@link #reset()}; safe to call when no executor is running.
	 */
	private void flushAndShutdownExecutor()
	{
		ExecutorService toShutdown = saveExecutor;
		saveExecutor = null;
		if (toShutdown == null)
		{
			// Still flush any dirty state inline so persistence-only tests work.
			flushIfDirty();
			return;
		}
		// Stop accepting new tasks; interrupt the in-flight debounce sleep so we flush now.
		toShutdown.shutdownNow();
		try
		{
			toShutdown.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		// Final synchronous flush on the calling thread — guarantees no data loss.
		saveScheduled.set(false);
		flushIfDirty();
	}

	/**
	 * Returns an immutable snapshot of the player-damaged NPC index set for testing.
	 */
	Set<Integer> snapshotPlayerDamagedNpcIndices()
	{
		return Collections.unmodifiableSet(new HashSet<>(playerDamagedNpcIndices));
	}
}
