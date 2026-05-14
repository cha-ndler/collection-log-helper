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
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.OptionalInt;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KillTimeTracker}.
 *
 * <p>Uses a temporary directory to exercise persistence without touching the
 * real filesystem. The {@code recordKill} package-visible method is called
 * directly so tests do not depend on the RuneLite event bus.
 */
@RunWith(MockitoJUnitRunner.class)
public class KillTimeTrackerTest
{
	private static final int NPC_ID = 42;
	private static final String SOURCE_NAME = "Giant Mole";
	private static final long T0 = 1_000_000L;

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private DropRateDatabase database;

	private KillTimeTracker tracker;

	@Before
	public void setUp()
	{
		when(config.learnKillTimes()).thenReturn(true);
		when(database.getSourceNameByNpcId(NPC_ID)).thenReturn(SOURCE_NAME);

		tracker = new KillTimeTracker(config, database, new GsonBuilder().create());
	}

	// ── Rolling window ──────────────────────────────────────────────────────────

	@Test
	public void firstKill_startsTimer_noSampleRecorded()
	{
		tracker.recordKill(NPC_ID, T0);

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
		assertFalse(tracker.getLearnedKillTime(SOURCE_NAME).isPresent());
	}

	@Test
	public void secondKill_recordsElapsedSeconds()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L); // 60 s

		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));
		OptionalInt result = tracker.getLearnedKillTime(SOURCE_NAME);
		assertTrue(result.isPresent());
		assertEquals(60, result.getAsInt());
	}

	@Test
	public void multipleKills_rollingAverageIsCorrect()
	{
		// First kill sets start time; subsequent kills each contribute one sample.
		long t = T0;
		tracker.recordKill(NPC_ID, t);
		t += 40_000L;
		tracker.recordKill(NPC_ID, t); // 40 s
		t += 60_000L;
		tracker.recordKill(NPC_ID, t); // 60 s
		t += 80_000L;
		tracker.recordKill(NPC_ID, t); // 80 s
		t += 100_000L;
		tracker.recordKill(NPC_ID, t); // 100 s

		// window = [40, 60, 80, 100], avg = 70
		assertEquals(4, tracker.getObservationCount(SOURCE_NAME));
		assertEquals(70, tracker.getLearnedKillTime(SOURCE_NAME).getAsInt());
	}

	@Test
	public void rollingWindow_excessKillsDropOldestFirst()
	{
		// Fill the 20-kill window, then push one more to trigger eviction.
		long t = T0;
		tracker.recordKill(NPC_ID, t);
		for (int i = 0; i < KillTimeTracker.WINDOW_SIZE; i++)
		{
			t += 30_000L;
			tracker.recordKill(NPC_ID, t);
		}
		assertEquals(KillTimeTracker.WINDOW_SIZE, tracker.getObservationCount(SOURCE_NAME));

		// 21st kill at 90 s evicts the oldest 30 s sample
		t += 90_000L;
		tracker.recordKill(NPC_ID, t);

		assertEquals(KillTimeTracker.WINDOW_SIZE, tracker.getObservationCount(SOURCE_NAME));
		// avg must be > 30 because the 90 s sample replaced one 30 s sample
		int avg = tracker.getLearnedKillTime(SOURCE_NAME).getAsInt();
		assertTrue("Expected avg > 30 after eviction, got " + avg, avg > 30);
	}

	// ── Outlier rejection ───────────────────────────────────────────────────────

	@Test
	public void tooShortInterval_discarded()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 1_000L); // 1 s — below MIN_KILL_SECONDS

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
	}

	@Test
	public void tooLongInterval_discarded()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + (31L * 60 * 1000)); // 31 min — above MAX_KILL_SECONDS

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
	}

	@Test
	public void exactlyMinInterval_accepted()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + (KillTimeTracker.MIN_KILL_SECONDS * 1000L));

		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));
	}

	@Test
	public void exactlyMaxInterval_accepted()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + (KillTimeTracker.MAX_KILL_SECONDS * 1000L));

		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));
	}

	// ── Feature flag ────────────────────────────────────────────────────────────

	@Test
	public void featureDisabled_getLearnedKillTime_returnsEmpty()
	{
		// Record a kill cycle while the feature is on
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);
		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));

		// Disable the feature — query must return empty even with stored data
		when(config.learnKillTimes()).thenReturn(false);
		assertFalse(tracker.getLearnedKillTime(SOURCE_NAME).isPresent());
	}

	// ── Unknown NPC ─────────────────────────────────────────────────────────────

	@Test
	public void unknownNpc_recordKill_isNoOp()
	{
		int unknownNpcId = 9999;
		when(database.getSourceNameByNpcId(unknownNpcId)).thenReturn(null);

		tracker.recordKill(unknownNpcId, T0);
		tracker.recordKill(unknownNpcId, T0 + 60_000L);

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
	}

	// ── RSN namespacing (per-character dir) ─────────────────────────────────────

	@Test
	public void init_withCharacterDir_writesDataFileInThatDir() throws Exception
	{
		File characterDir = tmp.newFolder("Zezima");
		tracker.init(characterDir);

		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);

		File expected = new File(characterDir, KillTimeTracker.DATA_FILE_NAME);
		assertTrue("kill_times.json should exist under characterDir", expected.exists());
	}

	@Test
	public void twoDistinctCharacterDirs_dataIsIsolated() throws Exception
	{
		// Simulate one account
		File acct1Dir = tmp.newFolder("Zezima");
		tracker.init(acct1Dir);
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);
		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));

		// Logout / switch account — state is cleared
		tracker.reset();
		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));

		// Second account in its own directory starts with no data
		KillTimeTracker tracker2 = new KillTimeTracker(config, database, new GsonBuilder().create());
		File acct2Dir = tmp.newFolder("Lynx_Titan");
		tracker2.init(acct2Dir);

		assertFalse(tracker2.getLearnedKillTime(SOURCE_NAME).isPresent());
	}

	// ── Persistence round-trip ──────────────────────────────────────────────────

	@Test
	public void saveThenLoad_preservesWindow() throws Exception
	{
		File characterDir = tmp.newFolder("player");
		tracker.init(characterDir);

		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 45_000L); // 45 s written to disk

		// New tracker instance reads from the same file
		KillTimeTracker tracker2 = new KillTimeTracker(config, database, new GsonBuilder().create());
		tracker2.init(characterDir);

		OptionalInt loaded = tracker2.getLearnedKillTime(SOURCE_NAME);
		assertTrue("Expected persisted data to survive round-trip", loaded.isPresent());
		assertEquals(45, loaded.getAsInt());
	}

	@Test
	public void reset_clearsInMemoryState()
	{
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);
		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));

		tracker.reset();

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
		assertFalse(tracker.getLearnedKillTime(SOURCE_NAME).isPresent());
	}
}
