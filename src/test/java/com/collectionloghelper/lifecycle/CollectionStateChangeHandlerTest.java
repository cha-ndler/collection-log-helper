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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.runelite.api.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests for {@link CollectionStateChangeHandler} -- the Wave 13 (#503)
 * extraction that owns {@code onSequenceComplete},
 * {@code rebuildSourcesWithMissingItems}, and
 * {@code exportEfficiencyIfEnabled}.
 *
 * <p>The B4.4 per-item-target-id behaviour is covered separately by
 * {@code OnSequenceCompletePerItemTest}; this file focuses on the
 * happy-path/no-op contract for the other two methods plus allocation-free
 * guard-fail behaviour.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CollectionStateChangeHandlerTest
{
	@Mock private Client client;
	@Mock private CollectionLogHelperConfig config;
	@Mock private DataModule dataModule;
	@Mock private EfficiencyModule efficiencyModule;
	@Mock private GuidanceModule guidanceModule;

	@Mock private GuidanceOverlayCoordinator coordinator;
	@Mock private DropRateDatabase database;
	@Mock private PlayerCollectionState collectionState;
	@Mock private PluginDataManager pluginDataManager;
	@Mock private EfficiencyCalculator calculator;

	@Mock
	@SuppressWarnings("unchecked")
	private BiConsumer<CollectionLogSource, Integer> activateGuidanceCallback;

	private CollectionStateChangeHandler handler;
	private boolean flagsCalled;

	@BeforeEach
	public void setUp()
	{
		handler = new CollectionStateChangeHandler(
			client, config, dataModule, efficiencyModule, guidanceModule);
		flagsCalled = false;
		handler.setCallbacks(
			() -> flagsCalled = true,
			Collections::emptyList,
			activateGuidanceCallback,
			() -> true,   // slot unlocked by default so existing advance tests still pass
			() -> null);
	}

	// ── handleSequenceComplete ────────────────────────────────────────────────

	@Test
	public void handleSequenceComplete_alwaysFiresFlagCallback()
	{
		when(config.autoAdvanceGuidance()).thenReturn(false);
		handler.handleSequenceComplete();
		assertTrue( flagsCalled,"flag callback must always fire so the panel rebuilds");
	}

	@Test
	public void handleSequenceComplete_autoAdvanceDisabled_doesNotActivate()
	{
		when(config.autoAdvanceGuidance()).thenReturn(false);
		handler.handleSequenceComplete();
		verifyNoInteractions(activateGuidanceCallback);
	}

	@Test
	public void handleSequenceComplete_autoAdvanceEnabled_emptyRanked_doesNotActivate()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		// supplier already returns Collections.emptyList()
		handler.handleSequenceComplete();
		verifyNoInteractions(activateGuidanceCallback);
		assertTrue(flagsCalled);
	}

	@Test
	public void handleSequenceComplete_skipsLockedSources()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		CollectionLogSource lockedSource = minimalSource("Locked Source");
		CollectionLogSource unlockedSource = minimalSource("Unlocked Source");
		ScoredItem locked = new ScoredItem(lockedSource, 100.0, 1, "L", true, 100.0, null, 100.0);
		ScoredItem unlocked = new ScoredItem(unlockedSource, 50.0, 2, "U", false, 50.0, null, 50.0);
		handler.setCallbacks(
			() -> flagsCalled = true,
			() -> java.util.Arrays.asList(locked, unlocked),
			activateGuidanceCallback,
			() -> true,
			() -> null);
		handler.handleSequenceComplete();
		verify(activateGuidanceCallback).accept(unlockedSource, null);
		verify(activateGuidanceCallback, never()).accept(lockedSource, null);
	}

	@Test
	public void handleSequenceComplete_nullRankedSupplierResult_noActivation()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		handler.setCallbacks(
			() -> flagsCalled = true,
			() -> null,
			activateGuidanceCallback,
			() -> true,
			() -> null);
		handler.handleSequenceComplete();
		verifyNoInteractions(activateGuidanceCallback);
	}

	@Test
	public void handleSequenceComplete_nullCallbacks_doNotNpe()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		handler.setCallbacks(null, null, null, null, null);
		// Must complete without throwing
		handler.handleSequenceComplete();
	}

	// ── rebuildSourcesWithMissingItems ────────────────────────────────────────

	@Test
	public void rebuildSourcesWithMissingItems_delegatesToCoordinator()
	{
		when(guidanceModule.getGuidanceCoordinator()).thenReturn(coordinator);
		when(dataModule.getDatabase()).thenReturn(database);
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		Set<String> partitioned = new HashSet<>();
		partitioned.add("Source A"); // incomplete
		partitioned.add("Source C"); // incomplete
		// (Sources B and D are complete and intentionally absent.)
		when(coordinator.rebuildSourcesWithMissingItems(database, collectionState))
			.thenReturn(partitioned);

		Set<String> result = handler.rebuildSourcesWithMissingItems();

		assertSame( partitioned, result,"handler must return the coordinator's set verbatim");
		assertEquals(2, result.size());
		assertTrue( result.contains("Source A"),"incomplete sources included");
		assertTrue( result.contains("Source C"),"incomplete sources included");
		assertFalse( result.contains("Source B"),"complete sources excluded");
		assertFalse( result.contains("Source D"),"complete sources excluded");
	}

	// ── exportEfficiencyIfEnabled ─────────────────────────────────────────────

	@Test
	public void exportEfficiencyIfEnabled_disabled_isNoOp()
	{
		when(config.exportEfficiencyLog()).thenReturn(false);
		handler.exportEfficiencyIfEnabled();
		// Disabled path must not touch data/efficiency modules at all
		verify(dataModule, never()).getPluginDataManager();
		verify(efficiencyModule, never()).getCalculator();
	}

	@Test
	public void exportEfficiencyIfEnabled_enabled_writesToCharacterFile()
	{
		when(config.exportEfficiencyLog()).thenReturn(true);
		when(dataModule.getPluginDataManager()).thenReturn(pluginDataManager);
		File characterFile = new File("efficiency-export.txt");
		when(pluginDataManager.getFile("efficiency-export.txt")).thenReturn(characterFile);
		when(efficiencyModule.getCalculator()).thenReturn(calculator);

		handler.exportEfficiencyIfEnabled();

		verify(calculator).exportEfficiencyList(characterFile, client);
	}

	@Test
	public void exportEfficiencyIfEnabled_enabled_fallsBackWhenPlayerNameUnavailable()
	{
		when(config.exportEfficiencyLog()).thenReturn(true);
		when(dataModule.getPluginDataManager()).thenReturn(pluginDataManager);
		when(pluginDataManager.getFile("efficiency-export.txt")).thenReturn(null);
		when(efficiencyModule.getCalculator()).thenReturn(calculator);

		handler.exportEfficiencyIfEnabled();

		// Fallback path still invokes the calculator with some non-null file
		verify(calculator).exportEfficiencyList(any(File.class), any());
	}

	// ── slot-unlock gate (#598) ───────────────────────────────────────────────

	/**
	 * Regression: last step completes, target slot did NOT unlock.
	 * Auto-advance must loop back on the same source, not advance to the next one.
	 */
	@Test
	public void handleSequenceComplete_slotNotUnlocked_reActivatesSameSource()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		CollectionLogSource activeSource = minimalSource("Active Source");
		CollectionLogSource nextSource = minimalSource("Next Source");
		ScoredItem nextItem = new ScoredItem(nextSource, 50.0, 1, "N", false, 50.0, null, 50.0);
		handler.setCallbacks(
			() -> flagsCalled = true,
			() -> java.util.Collections.singletonList(nextItem),
			activateGuidanceCallback,
			() -> false,              // slot did NOT unlock
			() -> activeSource);     // active source at sequence-complete time
		handler.handleSequenceComplete();
		// Must re-activate the current source, not advance to nextSource
		verify(activateGuidanceCallback).accept(activeSource, null);
		verify(activateGuidanceCallback, never()).accept(nextSource, null);
	}

	/**
	 * Regression: last step completes, target slot DID unlock.
	 * Auto-advance must fire on the next top-ranked source.
	 */
	@Test
	public void handleSequenceComplete_slotUnlocked_advancesToNextSource()
	{
		when(config.autoAdvanceGuidance()).thenReturn(true);
		CollectionLogSource activeSource = minimalSource("Active Source");
		CollectionLogSource nextSource = minimalSource("Next Source");
		ScoredItem nextItem = new ScoredItem(nextSource, 50.0, 1, "N", false, 50.0, null, 50.0);
		handler.setCallbacks(
			() -> flagsCalled = true,
			() -> java.util.Collections.singletonList(nextItem),
			activateGuidanceCallback,
			() -> true,              // slot DID unlock
			() -> activeSource);
		handler.handleSequenceComplete();
		// Must advance to the next ranked source, not loop back
		verify(activateGuidanceCallback).accept(nextSource, null);
		verify(activateGuidanceCallback, never()).accept(activeSource, null);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static CollectionLogSource minimalSource(String name)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			0, 0, 0,
			0, 0,
			null, null, null, 0.0,
			null, 0, false, 0,
			null, 0, null, null,
			null,
			null, null, 0, null, 0,
			Collections.emptyList()
		, null, null, null);
	}
}
