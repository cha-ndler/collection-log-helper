/*
 * Copyright (c) 2025, cha-ndler
 */
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.learning.KillTimeTracker;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GameStateRouter} -- the Wave 15 (#503) extraction that
 * owns the body of {@code CollectionLogHelperPlugin.onGameStateChanged}.
 *
 * <p>Covers:
 * <ul>
 *   <li>LOGGED_IN bootstrap dispatches client-thread work and refreshes the
 *       collection / travel / slayer caches plus panel sync indicators.</li>
 *   <li>LOGIN_SCREEN reset clears per-character state without dispatching to
 *       the client thread.</li>
 *   <li>All other transitions (LOADING, CONNECTION_LOST, HOPPING, STARTING)
 *       are no-ops.</li>
 *   <li>Null-callback safety on both transition handlers.</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class GameStateRouterTest
{
	@Mock private ClientThread clientThread;
	@Mock private DataModule dataModule;
	@Mock private EfficiencyModule efficiencyModule;
	@Mock private GuidanceModule guidanceModule;
	@Mock private SyncModule syncModule;
	@Mock private RequirementsChecker requirementsChecker;
	@Mock private PlayerTravelCapabilities travelCapabilities;
	@Mock private PlayerLocationResolver playerLocationResolver;

	@Mock private PlayerCollectionState collectionState;
	@Mock private DataSyncState dataSyncState;
	@Mock private PlayerBankState playerBankState;
	@Mock private PlayerInventoryState playerInventoryState;
	@Mock private SlayerTaskState slayerTaskState;
	@Mock private PluginDataManager pluginDataManager;
	@Mock private DropRateDatabase database;
	@Mock private ClueCompletionEstimator clueEstimator;
	@Mock private KillTimeTracker killTimeTracker;
	@Mock private GuidanceOverlay guidanceOverlay;
	@Mock private ObjectHighlightOverlay objectHighlightOverlay;
	@Mock private SyncStateCoordinator syncStateCoordinator;
	@Mock private CollectionLogHelperPanel panel;

	private GameStateRouter router;

	// Plugin-owned state mirrors -- the router never touches these fields directly,
	// only via the callbacks wired in setCallbacks().
	private Set<String> sourcesWithMissingItems;
	private boolean slayerRefreshPending;
	private boolean pendingTravelVarbitRefresh;
	private boolean missingItemsCleared;

	@Before
	public void setUp()
	{
		sourcesWithMissingItems = new HashSet<>();
		sourcesWithMissingItems.add("seed");
		slayerRefreshPending = false;
		pendingTravelVarbitRefresh = true;
		missingItemsCleared = false;

		// Run clientThread.invokeLater inline so we can assert on the bootstrap body
		doAnswer(inv ->
		{
			Runnable r = inv.getArgument(0);
			r.run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));

		router = new GameStateRouter(
			clientThread, dataModule, efficiencyModule, guidanceModule, syncModule,
			requirementsChecker, travelCapabilities, playerLocationResolver);
		router.setCallbacks(
			() -> panel,
			() -> Collections.singleton("rebuilt-source"),
			set -> sourcesWithMissingItems = set,
			() ->
			{
				sourcesWithMissingItems.clear();
				missingItemsCleared = true;
			},
			() -> slayerRefreshPending = true,
			() ->
			{
				slayerRefreshPending = false;
				pendingTravelVarbitRefresh = false;
			});
	}

	// ── LOGGED_IN ───────────────────────────────────────────────────────────────

	@Test
	public void loggedIn_dispatchesBootstrapOnClientThread()
	{
		stubLoggedInBootstrap();

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		verify(clientThread).invokeLater(any(Runnable.class));
		// Bootstrap touched the canonical state-refresh sequence
		verify(collectionState).refreshVarps();
		verify(collectionState).loadObtainedItems();
		verify(collectionState).captureRecentItems();
		verify(requirementsChecker).refreshAccessibility(any());
		verify(travelCapabilities).refreshQuestState();
		verify(travelCapabilities).refreshVarbits();
		verify(slayerTaskState).refresh();
		verify(syncStateCoordinator).setLastObtainedCount(anyInt());
		verify(objectHighlightOverlay).rescanScene();
	}

	@Test
	public void loggedIn_flagsSlayerRefreshPending()
	{
		stubLoggedInBootstrap();

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		assertTrue("slayerRefreshPending must be set so onGameTick refreshes Slayer", slayerRefreshPending);
	}

	@Test
	public void loggedIn_appliesRebuiltMissingItemsSet()
	{
		stubLoggedInBootstrap();

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		assertEquals(Collections.singleton("rebuilt-source"), sourcesWithMissingItems);
	}

	@Test
	public void loggedIn_panelSyncedWhenLogSyncedAndCountPositive()
	{
		stubLoggedInBootstrap();
		when(dataSyncState.isCollectionLogSynced()).thenReturn(true);
		when(collectionState.getTotalObtained()).thenReturn(42);

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		verify(panel).updateSyncStatus(eq(CollectionLogHelperPanel.SyncState.SYNCED), eq(42));
		verify(panel, never()).updateSyncStatus(eq(CollectionLogHelperPanel.SyncState.NOT_SYNCED), anyInt());
		verify(panel).rebuild();
	}

	@Test
	public void loggedIn_panelNotSyncedWhenLogNotSynced()
	{
		stubLoggedInBootstrap();
		when(dataSyncState.isCollectionLogSynced()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(0);

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		verify(panel).updateSyncStatus(eq(CollectionLogHelperPanel.SyncState.NOT_SYNCED), eq(0));
		verify(panel, never()).updateSyncStatus(eq(CollectionLogHelperPanel.SyncState.SYNCED), anyInt());
	}

	@Test
	public void loggedIn_nullPanel_doesNotNpe()
	{
		stubLoggedInBootstrap();
		router.setCallbacks(
			() -> null,
			() -> Collections.emptySet(),
			set -> { },
			() -> { },
			() -> slayerRefreshPending = true,
			() -> { });

		router.handle(gameStateEvent(GameState.LOGGED_IN));
		verifyNoInteractions(panel);
	}

	@Test
	public void loggedIn_callsSyncCoordinatorOnGameStateLoggedIn()
	{
		stubLoggedInBootstrap();

		router.handle(gameStateEvent(GameState.LOGGED_IN));

		verify(syncStateCoordinator).onGameStateLoggedIn();
	}

	// ── LOGIN_SCREEN ────────────────────────────────────────────────────────────

	@Test
	public void loginScreen_clearsAllPerCharacterState()
	{
		stubLoginScreenReset();

		router.handle(gameStateEvent(GameState.LOGIN_SCREEN));

		verify(collectionState).clearState();
		verify(requirementsChecker).clearCache();
		verify(clueEstimator).resetBucket();
		verify(slayerTaskState).reset();
		verify(syncStateCoordinator).onGameStateLoginScreen();
		verify(syncStateCoordinator).setLastObtainedCount(-1);
		verify(playerLocationResolver).reset();
		verify(guidanceOverlay).setShowCollectionLogReminder(false);
		verify(guidanceOverlay).setShowBankReminder(false);
		verify(dataSyncState).reset();
		verify(playerBankState).reset();
		verify(playerInventoryState).reset();
		verify(travelCapabilities).reset();
		verify(killTimeTracker).reset();
		verify(pluginDataManager).reset();
	}

	@Test
	public void loginScreen_clearsPluginTransientFlags()
	{
		stubLoginScreenReset();
		slayerRefreshPending = true;
		pendingTravelVarbitRefresh = true;

		router.handle(gameStateEvent(GameState.LOGIN_SCREEN));

		assertFalse("slayerRefreshPending must be cleared on logout", slayerRefreshPending);
		assertFalse("pendingTravelVarbitRefresh must be cleared on logout", pendingTravelVarbitRefresh);
	}

	@Test
	public void loginScreen_clearsMissingItemsSetInPlace()
	{
		stubLoginScreenReset();
		Set<String> before = sourcesWithMissingItems;

		router.handle(gameStateEvent(GameState.LOGIN_SCREEN));

		assertTrue("clearMissingItems callback must fire on logout", missingItemsCleared);
		// The clear() call mutates the seeded set in place -- reference unchanged
		assertSame("missing-items reference must be the same plugin-owned set", before, sourcesWithMissingItems);
		assertTrue("set must be empty after clear", sourcesWithMissingItems.isEmpty());
	}

	@Test
	public void loginScreen_doesNotDispatchClientThread()
	{
		stubLoginScreenReset();

		router.handle(gameStateEvent(GameState.LOGIN_SCREEN));

		verifyNoInteractions(clientThread);
	}

	// ── No-op transitions ───────────────────────────────────────────────────────

	@Test
	public void loading_isNoOp()
	{
		router.handle(gameStateEvent(GameState.LOADING));
		verifyNoInteractions(clientThread, collectionState, dataSyncState, syncStateCoordinator);
		assertFalse(missingItemsCleared);
	}

	@Test
	public void connectionLost_isNoOp()
	{
		router.handle(gameStateEvent(GameState.CONNECTION_LOST));
		verifyNoInteractions(clientThread, collectionState, dataSyncState, syncStateCoordinator);
	}

	@Test
	public void hopping_isNoOp()
	{
		router.handle(gameStateEvent(GameState.HOPPING));
		verifyNoInteractions(clientThread, collectionState, dataSyncState, syncStateCoordinator);
	}

	@Test
	public void starting_isNoOp()
	{
		router.handle(gameStateEvent(GameState.STARTING));
		verifyNoInteractions(clientThread, collectionState, dataSyncState, syncStateCoordinator);
	}

	// ── Null callbacks ──────────────────────────────────────────────────────────

	@Test
	public void loggedIn_nullCallbacks_doNotNpe()
	{
		stubLoggedInBootstrap();
		router.setCallbacks(null, null, null, null, null, null);

		router.handle(gameStateEvent(GameState.LOGGED_IN));
		// Reaches this line without an NPE
	}

	@Test
	public void loginScreen_nullCallbacks_doNotNpe()
	{
		stubLoginScreenReset();
		router.setCallbacks(null, null, null, null, null, null);

		router.handle(gameStateEvent(GameState.LOGIN_SCREEN));
		// Reaches this line without an NPE
	}

	// ── helpers ─────────────────────────────────────────────────────────────────

	private void stubLoggedInBootstrap()
	{
		when(syncModule.getSyncStateCoordinator()).thenReturn(syncStateCoordinator);
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		when(dataModule.getDatabase()).thenReturn(database);
		when(database.getAllSources()).thenReturn(Collections.emptyList());
		when(dataModule.getSlayerTaskState()).thenReturn(slayerTaskState);
		when(dataModule.getDataSyncState()).thenReturn(dataSyncState);
		when(dataModule.getPlayerBankState()).thenReturn(playerBankState);
		when(guidanceModule.getObjectHighlightOverlay()).thenReturn(objectHighlightOverlay);
	}

	private static GameStateChanged gameStateEvent(GameState state)
	{
		GameStateChanged e = new GameStateChanged();
		e.setGameState(state);
		return e;
	}

	private void stubLoginScreenReset()
	{
		when(syncModule.getSyncStateCoordinator()).thenReturn(syncStateCoordinator);
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		when(dataModule.getSlayerTaskState()).thenReturn(slayerTaskState);
		when(dataModule.getDataSyncState()).thenReturn(dataSyncState);
		when(dataModule.getPlayerBankState()).thenReturn(playerBankState);
		when(dataModule.getPlayerInventoryState()).thenReturn(playerInventoryState);
		when(dataModule.getPluginDataManager()).thenReturn(pluginDataManager);
		when(efficiencyModule.getClueEstimator()).thenReturn(clueEstimator);
		when(efficiencyModule.getKillTimeTracker()).thenReturn(killTimeTracker);
		when(guidanceModule.getGuidanceOverlay()).thenReturn(guidanceOverlay);
	}
}
