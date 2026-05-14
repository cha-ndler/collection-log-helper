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
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
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
 * <p>Data is persisted to {@code kill_times.json} in the per-character directory and
 * survives across sessions. The file is written after each kill to ensure consistency.
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

	/** File where rolling data is persisted; set on {@link #init(File)}. */
	private File dataFile;

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
		loadFromDisk();
	}

	/**
	 * Resets tracker state on logout / plugin shutdown.
	 * Does NOT delete the persisted file — data survives across sessions.
	 */
	public void reset()
	{
		windows.clear();
		lastKillTime.clear();
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
	 * <p>Called from {@link #onActorDeath(ActorDeath)} when the feature is enabled.
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

		saveToDisk();
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

	private void saveToDisk()
	{
		if (dataFile == null)
		{
			return;
		}
		Map<String, int[]> serializable = new HashMap<>();
		for (Map.Entry<String, Deque<Integer>> entry : windows.entrySet())
		{
			int[] arr = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
			serializable.put(entry.getKey(), arr);
		}
		try (FileWriter writer = new FileWriter(dataFile))
		{
			gson.toJson(serializable, writer);
		}
		catch (IOException e)
		{
			log.warn("KillTimeTracker: failed to save {}", dataFile, e);
		}
	}
}
