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
import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.HitsplatApplied;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;

/**
 * Unit tests for {@link KillTimeTracker}.
 *
 * <p>Uses a temporary directory to exercise persistence without touching the
 * real filesystem. The {@code recordKill} package-visible method is called
 * directly so most tests do not depend on the RuneLite event bus.
 * Event-driven tests for attribution (#481) use real event objects with mocked actors.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KillTimeTrackerTest
{
	private static final int NPC_ID = 42;
	private static final int NPC_INDEX = 7;
	private static final String SOURCE_NAME = "Giant Mole";
	private static final long T0 = 1_000_000L;

	@TempDir
	public File tmp;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private DropRateDatabase database;

	private KillTimeTracker tracker;

	@BeforeEach
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
		assertTrue( avg > 30,"Expected avg > 30 after eviction, got " + avg);
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
		File characterDir = newTempFolder(tmp, "Zezima");
		tracker.init(characterDir);

		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);

		// Drain debounced background writer so the file is on disk for the assertion.
		tracker.reset();

		File expected = new File(characterDir, KillTimeTracker.DATA_FILE_NAME);
		assertTrue( expected.exists(),"kill_times.json should exist under characterDir");
	}

	@Test
	public void twoDistinctCharacterDirs_dataIsIsolated() throws Exception
	{
		// Simulate one account
		File acct1Dir = newTempFolder(tmp, "Zezima");
		tracker.init(acct1Dir);
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);
		assertEquals(1, tracker.getObservationCount(SOURCE_NAME));

		// Logout / switch account — state is cleared (reset() flushes pending writes too)
		tracker.reset();
		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));

		// Second account in its own directory starts with no data
		KillTimeTracker tracker2 = new KillTimeTracker(config, database, new GsonBuilder().create());
		File acct2Dir = newTempFolder(tmp, "Lynx_Titan");
		tracker2.init(acct2Dir);

		assertFalse(tracker2.getLearnedKillTime(SOURCE_NAME).isPresent());
	}

	// ── Persistence round-trip ──────────────────────────────────────────────────

	@Test
	public void saveThenLoad_preservesWindow() throws Exception
	{
		File characterDir = newTempFolder(tmp, "player");
		tracker.init(characterDir);

		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 45_000L); // 45 s written to disk

		// reset() drains the debounced background writer synchronously, so the data
		// is guaranteed on disk before tracker2 loads it.
		tracker.reset();

		// New tracker instance reads from the same file
		KillTimeTracker tracker2 = new KillTimeTracker(config, database, new GsonBuilder().create());
		tracker2.init(characterDir);

		OptionalInt loaded = tracker2.getLearnedKillTime(SOURCE_NAME);
		assertTrue( loaded.isPresent(),"Expected persisted data to survive round-trip");
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

	// ── Issue #480: disk I/O off client thread ──────────────────────────────────

	/**
	 * Regression for #480: a kill-record loop at Wintertodt-style throughput must
	 * not block on disk I/O. After {@code init()} the tracker installs a background
	 * single-thread executor and {@code recordKill} only marks state dirty; it does
	 * NOT perform a synchronous write per call.
	 *
	 * <p>We assert this by verifying that immediately after recording a kill, the
	 * persisted file does NOT yet exist — it would exist if write were synchronous.
	 * Then {@code reset()} drains the executor and the file appears.
	 */
	@Test
	public void recordKill_doesNotWriteSynchronously_afterInit() throws Exception
	{
		File characterDir = newTempFolder(tmp, "perfPlayer");
		tracker.init(characterDir);
		File dataFile = new File(characterDir, KillTimeTracker.DATA_FILE_NAME);
		assertFalse( dataFile.exists(),"data file should not exist before any kill");

		// Two kills => one sample recorded. The save is debounced for SAVE_DEBOUNCE_MS,
		// so the file should NOT exist immediately after recordKill returns.
		tracker.recordKill(NPC_ID, T0);
		tracker.recordKill(NPC_ID, T0 + 60_000L);
		assertFalse(
			dataFile.exists(),"data file must not be written synchronously inside recordKill (#480)");

		// reset() drains the executor and flushes, so now the file must exist.
		tracker.reset();
		assertTrue( dataFile.exists(),"data file should be flushed by reset()");
	}

	/**
	 * Regression for #480: many rapid kills inside the debounce window coalesce
	 * into a single disk write — the producer thread returns immediately.
	 *
	 * <p>This is a wall-clock budget check: 100 kills should complete in well under
	 * the debounce window because they only mark state dirty.
	 */
	@Test
	public void recordKill_manyRapidKills_returnImmediately() throws Exception
	{
		File characterDir = newTempFolder(tmp, "turboPlayer");
		tracker.init(characterDir);

		long startNanos = System.nanoTime();
		long t = T0;
		tracker.recordKill(NPC_ID, t);
		for (int i = 0; i < 100; i++)
		{
			t += 30_000L;
			tracker.recordKill(NPC_ID, t);
		}
		long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

		// 100 calls should be sub-second even on slow CI. Generous budget = 500ms.
		// The bug (#480) had each kill blocking on FileWriter, which on a slow disk
		// can be 10-50ms per kill — 100 kills would push past the budget.
		assertTrue(
			elapsedMs < 500,"100 recordKill calls should be fast (<500ms), was " + elapsedMs + "ms");

		tracker.reset();
	}

	// ── Issue #481: local-player attribution ────────────────────────────────────

	/**
	 * Regression for #481: deaths of NPCs the local player did NOT damage are
	 * NOT recorded. This is the core fix — group content (CoX/ToB/ToA, Wintertodt,
	 * Tempoross, GWD masses) used to poison the average with other players' kills.
	 */
	@Test
	public void onActorDeath_unattributedNpc_doesNotRecord()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(NPC_ID);
		when(npc.getIndex()).thenReturn(NPC_INDEX);

		// First death starts no timer because the NPC was not tagged by the player.
		tracker.onActorDeath(deathOf(npc));
		// Even a "second" death is silently dropped because no attribution.
		tracker.onActorDeath(deathOf(npc));

		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
		assertFalse(tracker.getLearnedKillTime(SOURCE_NAME).isPresent());
	}

	/**
	 * Regression for #481: when the player tags and kills two NPCs spaced apart,
	 * the elapsed time between attributed deaths is recorded in the rolling window.
	 *
	 * <p>Drives both event handlers end-to-end through the event bus methods (not the
	 * package-visible {@code recordKill}), so this exercises the attribution gate
	 * AND the time-between-deaths math together. Because {@code onActorDeath} reads
	 * {@link System#currentTimeMillis()} we can't dictate the exact elapsed value;
	 * we only assert that an observation was recorded and the value is plausible.
	 */
	@Test
	public void onActorDeath_twoAttributedKills_recordsObservation() throws Exception
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(NPC_ID);
		when(npc.getIndex()).thenReturn(NPC_INDEX);

		// First kill: tag + die. Seeds the timer, no sample yet.
		tracker.onHitsplatApplied(mineHitsplatOn(npc));
		tracker.onActorDeath(deathOf(npc));
		assertEquals(
			0, tracker.getObservationCount(SOURCE_NAME),"first attributed death seeds the timer, no sample");

		// Sleep long enough to be above MIN_KILL_SECONDS = 3s (use 3.1s to be safe).
		Thread.sleep(3_100L);

		// Second kill: tag + die. Now the elapsed time is recorded as a sample.
		tracker.onHitsplatApplied(mineHitsplatOn(npc));
		tracker.onActorDeath(deathOf(npc));

		assertEquals(
			1, tracker.getObservationCount(SOURCE_NAME),"second attributed death records one sample");
		OptionalInt result = tracker.getLearnedKillTime(SOURCE_NAME);
		assertTrue(result.isPresent());
		int learned = result.getAsInt();
		assertTrue(
			learned >= KillTimeTracker.MIN_KILL_SECONDS
				&& learned <= KillTimeTracker.MAX_KILL_SECONDS,"learned kill time should be in plausible range, was " + learned);
	}

	/**
	 * Regression for #481: the "isMine" attribution mark is cleared after the
	 * matching death event, so a respawned NPC reusing the same scene index
	 * cannot inherit attribution from a previous kill.
	 */
	@Test
	public void onActorDeath_clearsAttributionMark()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(NPC_ID);
		when(npc.getIndex()).thenReturn(NPC_INDEX);

		tracker.onHitsplatApplied(mineHitsplatOn(npc));
		assertTrue(tracker.isPlayerDamaged(NPC_INDEX));

		tracker.onActorDeath(deathOf(npc));
		assertFalse(
			tracker.isPlayerDamaged(NPC_INDEX),"death should consume the player-damaged mark");
	}

	/**
	 * Regression for #481: hitsplats from OTHER players ({@link Hitsplat#isMine()}
	 * == false) do not mark an NPC as player-damaged.
	 */
	@Test
	public void onHitsplatApplied_otherPlayersHitsplats_doNotMarkAttribution()
	{
		// No getIndex() stub — the production code returns before calling getIndex()
		// when isMine() is false, so stubbing it would be flagged as unnecessary.
		NPC npc = mock(NPC.class);

		tracker.onHitsplatApplied(otherHitsplatOn(npc));
		assertFalse(
			tracker.isPlayerDamaged(NPC_INDEX),"non-mine hitsplats must not mark attribution");
	}

	/**
	 * Regression for #481: the feature flag gates both event handlers — when
	 * disabled, no attribution marks are recorded.
	 */
	@Test
	public void featureDisabled_attributionAndDeathEvents_areNoOps()
	{
		when(config.learnKillTimes()).thenReturn(false);

		// No stubs on the NPC mock — both handlers must short-circuit at the feature
		// flag check before touching the npc, so even an un-stubbed mock is safe.
		NPC npc = mock(NPC.class);

		tracker.onHitsplatApplied(mineHitsplatOn(npc));
		assertFalse(tracker.isPlayerDamaged(NPC_INDEX));

		tracker.onActorDeath(deathOf(npc));
		assertEquals(0, tracker.getObservationCount(SOURCE_NAME));
	}

	// ── Test helpers ────────────────────────────────────────────────────────────

	private static ActorDeath deathOf(Actor actor)
	{
		return new ActorDeath(actor);
	}

	private static HitsplatApplied mineHitsplatOn(Actor actor)
	{
		Hitsplat hitsplat = mock(Hitsplat.class);
		when(hitsplat.isMine()).thenReturn(true);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(actor);
		event.setHitsplat(hitsplat);
		return event;
	}

	private static HitsplatApplied otherHitsplatOn(Actor actor)
	{
		// Mockito's default for boolean is false → isMine() returns false without stubbing,
		// which is exactly what we want for an "other player" hitsplat. Stubbing to the
		// default trips MockitoJUnitRunner.STRICT_STUBS as an unnecessary stubbing.
		Hitsplat hitsplat = mock(Hitsplat.class);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(actor);
		event.setHitsplat(hitsplat);
		return event;
	}

	private static File newTempFile(File dir, String name) throws IOException
	{
		File f = new File(dir, name);
		f.createNewFile();
		return f;
	}

	private static File newTempFolder(File dir, String name) throws IOException
	{
		File f = new File(dir, name);
		f.mkdirs();
		return f;
	}
}
