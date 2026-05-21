/*
 * Copyright (c) 2025, cha-ndler
 */
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import com.collectionloghelper.learning.KillTimeTracker;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.io.File;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests for {@link GameTickOrchestrator} -- the Wave 14 (#503) extraction
 * that owns the per-tick coalescer body that previously lived inline in
 * {@code CollectionLogHelperPlugin.onGameTick}.
 *
 * <p>Covers all five responsibility branches independently, the allocation-
 * free no-op tick (no flags set, no player), and the coalesced panel rebuild.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GameTickOrchestratorTest
{
	@Mock private Client client;
	@Mock private DataModule dataModule;
	@Mock private EfficiencyModule efficiencyModule;
	@Mock private GuidanceModule guidanceModule;
	@Mock private SyncModule syncModule;
	@Mock private RequirementsChecker requirementsChecker;
	@Mock private PlayerTravelCapabilities travelCapabilities;
	@Mock private PlayerLocationResolver playerLocationResolver;
	@Mock private AuthoringLogger authoringLogger;
	@Mock private CollectionStateChangeHandler collectionStateChangeHandler;

	@Mock private DropRateDatabase database;
	@Mock private PluginDataManager pluginDataManager;
	@Mock private KillTimeTracker killTimeTracker;
	@Mock private GuidanceOverlayCoordinator guidanceCoordinator;
	@Mock private GuidanceSequencer guidanceSequencer;
	@Mock private SyncStateCoordinator syncStateCoordinator;
	@Mock private SlayerTaskState slayerTaskState;
	@Mock private CollectionLogHelperPanel panel;
	@Mock private Player localPlayer;

	private GameTickOrchestrator orchestrator;

	// Plugin-owned flag mirrors. The orchestrator never sees these fields
	// directly -- only via the poll-and-clear callbacks, matching prod wiring.
	private boolean reqsFlag;
	private boolean travelFlag;
	private boolean slayerFlag;
	private boolean panelRebuildFlag;
	private boolean rankedDirtyFlag;
	private CollectionLogHelperPanel currentPanel;

	@BeforeEach
	public void setUp()
	{
		when(dataModule.getDatabase()).thenReturn(database);
		when(dataModule.getPluginDataManager()).thenReturn(pluginDataManager);
		when(dataModule.getSlayerTaskState()).thenReturn(slayerTaskState);
		when(efficiencyModule.getKillTimeTracker()).thenReturn(killTimeTracker);
		when(guidanceModule.getGuidanceCoordinator()).thenReturn(guidanceCoordinator);
		when(guidanceModule.getGuidanceSequencer()).thenReturn(guidanceSequencer);
		when(syncModule.getSyncStateCoordinator()).thenReturn(syncStateCoordinator);

		orchestrator = new GameTickOrchestrator(
			client, dataModule, efficiencyModule, guidanceModule, syncModule,
			requirementsChecker, travelCapabilities, playerLocationResolver,
			authoringLogger, collectionStateChangeHandler);

		reqsFlag = false;
		travelFlag = false;
		slayerFlag = false;
		panelRebuildFlag = false;
		rankedDirtyFlag = false;
		currentPanel = panel;

		orchestrator.setCallbacks(
			this::pollReqs,
			this::pollTravel,
			this::pollSlayer,
			this::pollPanelRebuild,
			() ->
			{
				panelRebuildFlag = true;
				rankedDirtyFlag = true;
			},
			() -> rankedDirtyFlag = true,
			() -> currentPanel);
	}

	private boolean pollReqs()
	{
		if (!reqsFlag) return false;
		reqsFlag = false;
		return true;
	}

	private boolean pollTravel()
	{
		if (!travelFlag) return false;
		travelFlag = false;
		return true;
	}

	private boolean pollSlayer()
	{
		if (!slayerFlag) return false;
		slayerFlag = false;
		return true;
	}

	private boolean pollPanelRebuild()
	{
		if (!panelRebuildFlag) return false;
		panelRebuildFlag = false;
		return true;
	}

	/**
	 * Returns a sync tick that does nothing (no rebuild, no dirty, returns CLEAN).
	 * Stubs SyncStateCoordinator.tickSync with safe defaults so each test starts
	 * from a quiet baseline.
	 */
	private void stubQuietSyncTick()
	{
		when(syncStateCoordinator.tickSync(any(), any(), any()))
			.thenReturn(SyncStateCoordinator.SyncTickResult.CLEAN);
	}

	// ── (1) Requirements refresh ─────────────────────────────────────────────

	@Test
	public void requirementsFlagSet_callsRefreshAccessibility_andClearsFlag()
	{
		stubQuietSyncTick();
		reqsFlag = true;
		List<CollectionLogSource> sources = Collections.emptyList();
		when(database.getAllSources()).thenReturn(sources);
		when(requirementsChecker.refreshAccessibility(sources)).thenReturn(false);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(requirementsChecker).refreshAccessibility(sources);
		assertFalse( reqsFlag,"poll-and-clear must reset the flag");
	}

	@Test
	public void requirementsChanged_and_scriptScanInactive_marksDirty()
	{
		stubQuietSyncTick();
		reqsFlag = true;
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		when(requirementsChecker.refreshAccessibility(any())).thenReturn(true);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		// markPanelRebuildAndRankedDirty sets panelRebuildFlag, which is then
		// consumed by the final coalesced rebuild in the same tick. Assert
		// behaviorally: the panel got rebuilt and ranked dirty was set.
		verify(panel).rebuild();
		assertTrue( rankedDirtyFlag,"ranked sources marked dirty when reqs changed");
	}

	@Test
	public void requirementsChanged_but_scriptScanActive_doesNotMarkDirty()
	{
		stubQuietSyncTick();
		reqsFlag = true;
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		when(requirementsChecker.refreshAccessibility(any())).thenReturn(true);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		assertFalse( panelRebuildFlag,"script-scan gates the dirty mark");
		assertFalse( rankedDirtyFlag,"script-scan gates the dirty mark");
	}

	// ── (2) Travel varbit refresh ────────────────────────────────────────────

	@Test
	public void travelFlagSet_callsRefreshVarbits_andClearsFlag()
	{
		stubQuietSyncTick();
		travelFlag = true;
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(travelCapabilities).refreshVarbits();
		assertFalse( travelFlag,"poll-and-clear must reset the flag");
	}

	@Test
	public void travelFlagUnset_doesNotCallRefreshVarbits()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(travelCapabilities, never()).refreshVarbits();
	}

	// ── (3) Per-character init ───────────────────────────────────────────────

	@Test
	public void characterDirNull_initsPluginDataManager_andKillTimeTracker()
	{
		stubQuietSyncTick();
		File characterDir = new File(".");
		when(pluginDataManager.getCharacterDir()).thenReturn(null, characterDir);
		when(pluginDataManager.init()).thenReturn(true);

		orchestrator.tick();

		verify(pluginDataManager).init();
		verify(killTimeTracker).init(characterDir);
	}

	@Test
	public void characterDirNull_initFails_doesNotInitKillTimeTracker()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(null);
		when(pluginDataManager.init()).thenReturn(false);

		orchestrator.tick();

		verify(killTimeTracker, never()).init(any());
	}

	@Test
	public void characterDirAlreadySet_skipsInit()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(pluginDataManager, never()).init();
		verify(killTimeTracker, never()).init(any());
	}

	// ── (4) Coordinator + player location ────────────────────────────────────

	@Test
	public void coordinatorTickAlwaysCalled()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(guidanceCoordinator).tick();
	}

	@Test
	public void playerPresent_andSequencerActive_forwardsLocation()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		WorldPoint location = new WorldPoint(3200, 3200, 0);
		when(playerLocationResolver.resolveAndCache()).thenReturn(location);
		when(guidanceSequencer.isActive()).thenReturn(true);

		orchestrator.tick();

		verify(authoringLogger).setPlayerLocation(location);
		verify(guidanceSequencer).setPlayerLocation(location);
		verify(guidanceSequencer).onPlayerMoved(location);
	}

	@Test
	public void playerPresent_butSequencerInactive_doesNotForwardToSequencer()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		WorldPoint location = new WorldPoint(3200, 3200, 0);
		when(playerLocationResolver.resolveAndCache()).thenReturn(location);
		when(guidanceSequencer.isActive()).thenReturn(false);

		orchestrator.tick();

		verify(authoringLogger).setPlayerLocation(location);
		verify(guidanceSequencer, never()).setPlayerLocation(any());
		verify(guidanceSequencer, never()).onPlayerMoved(any());
	}

	@Test
	public void playerAbsent_skipsLocationResolution()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));
		when(client.getLocalPlayer()).thenReturn(null);

		orchestrator.tick();

		verify(playerLocationResolver, never()).resolveAndCache();
		verify(authoringLogger, never()).setPlayerLocation(any());
	}

	// ── (5a) Deferred slayer refresh ─────────────────────────────────────────

	@Test
	public void slayerFlagSet_andLoginTickDelayInWindow_refreshes()
	{
		stubQuietSyncTick();
		slayerFlag = true;
		when(syncStateCoordinator.getLoginTickDelay()).thenReturn(3);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(slayerTaskState).refresh();
		// Slayer refresh sets panelRebuildFlag, which is then consumed by the
		// final coalesced rebuild in the same tick. Assert behaviorally.
		verify(panel).rebuild();
		assertTrue( rankedDirtyFlag,"slayer refresh should mark ranked dirty");
		assertFalse( slayerFlag,"poll-and-clear must reset the flag");
	}

	@Test
	public void slayerFlagSet_butLoginTickDelayOverWindow_doesNotRefresh()
	{
		stubQuietSyncTick();
		slayerFlag = true;
		when(syncStateCoordinator.getLoginTickDelay()).thenReturn(8);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(slayerTaskState, never()).refresh();
		assertTrue( slayerFlag,"flag must remain set so a later tick handles it");
	}

	// ── (5b) Sync tick + RANKED_DIRTY propagation ────────────────────────────

	@Test
	public void syncTickReturnsRankedDirty_marksRankedDirty()
	{
		when(syncStateCoordinator.tickSync(any(), any(), any()))
			.thenReturn(SyncStateCoordinator.SyncTickResult.RANKED_DIRTY);
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		assertTrue( rankedDirtyFlag,"RANKED_DIRTY result must mark ranked sources dirty");
	}

	@Test
	public void syncTickReturnsClean_doesNotMarkRankedDirty()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		assertFalse( rankedDirtyFlag,"CLEAN result must not mark ranked sources dirty");
	}

	// ── (5c) Coalesced panel rebuild ─────────────────────────────────────────

	@Test
	public void coalescedRebuild_firesOnceEvenWhenMultipleSubFlagsSetIt()
	{
		// Force two sub-branches to set the panel-rebuild flag, and then
		// observe that only one panel.rebuild() lands at the end.
		reqsFlag = true;
		slayerFlag = true;
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		when(requirementsChecker.refreshAccessibility(any())).thenReturn(true);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(syncStateCoordinator.getLoginTickDelay()).thenReturn(2);
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(panel, times(1)).rebuild();
	}

	@Test
	public void panelRebuildFlagFalse_doesNotRebuild()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		verify(panel, never()).rebuild();
	}

	@Test
	public void panelNull_doesNotRebuildEvenWhenFlagSet()
	{
		panelRebuildFlag = true;
		currentPanel = null;
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		orchestrator.tick();

		// Cannot verify on a null panel directly; the test is that no exception
		// is thrown and the flag is left cleared by no one (the orchestrator
		// short-circuits on null panel before calling pollPanelRebuild).
		assertTrue( panelRebuildFlag,"flag preserved when panel absent");
	}

	// ── (no-op tick allocation profile) ──────────────────────────────────────

	@Test
	public void noOpTick_doesNotTouchAnyMutator()
	{
		// All flags false, no player, no script-scan, no login-tick window,
		// character dir already set. The tick should be a near-pure dispatch:
		// it polls primitive flags, asks the coordinator to tick (a no-op in
		// production when there are no deferred tasks), and asks the sync
		// coordinator to tick (also a no-op in the steady-state).
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));
		when(client.getLocalPlayer()).thenReturn(null);

		orchestrator.tick();

		// Mutators that would indicate hot-path work
		verify(requirementsChecker, never()).refreshAccessibility(any());
		verify(travelCapabilities, never()).refreshVarbits();
		verify(pluginDataManager, never()).init();
		verify(killTimeTracker, never()).init(any());
		verifyNoInteractions(playerLocationResolver);
		verify(slayerTaskState, never()).refresh();
		verify(panel, never()).rebuild();
		assertFalse(panelRebuildFlag);
		assertFalse(rankedDirtyFlag);

		// What the orchestrator IS allowed to call on a no-op tick:
		// - coordinator.tick() (drains deferred ShortestPath work; a no-op
		//   when nothing was deferred)
		// - syncStateCoordinator.tickSync(...) (handles login/sync state)
		verify(guidanceCoordinator, times(1)).tick();
		verify(syncStateCoordinator, times(1))
			.tickSync(eq(panel), any(), any());
	}

	// ── exportEfficiency callback hoisted to a field (#541 follow-up) ────────

	/**
	 * The {@code exportEfficiencyIfEnabled} method-ref used to be created
	 * inline per-tick in the call to {@code tickSync(...)}. It is now hoisted
	 * to a final field initialized once in the constructor. This test asserts
	 * the field is wired, is the same instance across ticks (i.e. not
	 * reallocated per tick), and that invoking it dispatches to the underlying
	 * handler.
	 */
	@Test
	public void exportEfficiencyCallback_isHoistedAndInvokesHandler()
	{
		stubQuietSyncTick();
		when(pluginDataManager.getCharacterDir()).thenReturn(new File("."));

		ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

		// Tick twice and capture the export callback both times.
		orchestrator.tick();
		orchestrator.tick();

		verify(syncStateCoordinator, times(2))
			.tickSync(any(), any(), captor.capture());

		List<Runnable> captured = captor.getAllValues();
		assertNotNull( captured.get(0),"export callback must be non-null on first tick");
		assertNotNull( captured.get(1),"export callback must be non-null on second tick");
		assertSame(
			captured.get(0), captured.get(1),
			"export callback must be the same hoisted instance across ticks "
				+ "(not reallocated per tick)");

		// Invoking the captured callback must dispatch to the underlying
		// handler exactly as the inline method-ref did before the hoist.
		captured.get(0).run();
		verify(collectionStateChangeHandler).exportEfficiencyIfEnabled();
	}
}
