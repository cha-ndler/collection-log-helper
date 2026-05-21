/*
 * Copyright (c) 2025, cha-ndler
 */
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.guidance.GuidanceSequencer;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VarbitChangeRouterTest
{
	@Mock private Client client;
	@Mock private CollectionLogHelperConfig config;
	@Mock private DataModule dataModule;
	@Mock private GuidanceModule guidanceModule;
	@Mock private SyncModule syncModule;
	@Mock private AuthoringLogger authoringLogger;
	@Mock private PlayerCollectionState collectionState;
	@Mock private SlayerTaskState slayerTaskState;
	@Mock private GuidanceSequencer guidanceSequencer;
	@Mock private SyncStateCoordinator syncStateCoordinator;

	private VarbitChangeRouter router;
	private boolean flagRefreshesCalled;
	private boolean onCollectionStateChangedCalled;
	private boolean onSlayerTaskChangedCalled;

	@BeforeEach
	public void setUp()
	{
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		when(dataModule.getSlayerTaskState()).thenReturn(slayerTaskState);
		when(guidanceModule.getGuidanceSequencer()).thenReturn(guidanceSequencer);
		when(syncModule.getSyncStateCoordinator()).thenReturn(syncStateCoordinator);
		router = new VarbitChangeRouter(client, config, dataModule, guidanceModule, syncModule, authoringLogger);
		flagRefreshesCalled = false;
		onCollectionStateChangedCalled = false;
		onSlayerTaskChangedCalled = false;
		router.setCallbacks(
			() -> flagRefreshesCalled = true,
			() -> onCollectionStateChangedCalled = true,
			() -> onSlayerTaskChangedCalled = true);
	}

	private VarbitChanged makeEvent(int varbitId, int value)
	{
		VarbitChanged e = new VarbitChanged();
		e.setVarbitId(varbitId);
		e.setValue(value);
		return e;
	}

	@Test
	public void unrelatedVarbit_stillFlagsRefreshes_andForwardsToSequencer()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(9999, 0));
		verify(collectionState).refreshVarps();
		verify(guidanceSequencer).onVarbitChanged(9999, 0);
		verify(slayerTaskState).refresh();
		assertTrue( flagRefreshesCalled,"flagRefreshes should always fire");
		assertFalse( onCollectionStateChangedCalled,"no collection-state delta");
		assertFalse( onSlayerTaskChangedCalled,"no slayer delta");
	}

	@Test
	public void authoringLog_firesOnceWhenTickAdvances()
	{
		when(config.guidanceAuthoring()).thenReturn(true);
		when(client.getTickCount()).thenReturn(42);
		when(authoringLogger.getLastVarbitLogTick()).thenReturn(41);
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		router.handle(makeEvent(123, 4));
		verify(authoringLogger).setLastVarbitLogTick(42);
		verify(authoringLogger).log(anyString(), any(), any());
	}

	@Test
	public void authoringLog_doesNotFireOnSameTick()
	{
		when(config.guidanceAuthoring()).thenReturn(true);
		when(client.getTickCount()).thenReturn(42);
		when(authoringLogger.getLastVarbitLogTick()).thenReturn(42);
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		router.handle(makeEvent(123, 4));
		verify(authoringLogger, never()).setLastVarbitLogTick(anyInt());
		verify(authoringLogger, never()).log(anyString(), any(), any());
	}

	@Test
	public void authoringLog_doesNotFireWhenAuthoringDisabled()
	{
		when(config.guidanceAuthoring()).thenReturn(false);
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		router.handle(makeEvent(123, 4));
		verify(authoringLogger, never()).log(anyString(), any(), any());
	}

	@Test
	public void slayerTask_activeToInactive_triggersSlayerCallback()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(true, false);
		when(slayerTaskState.getCreatureName()).thenReturn("GreaterDemon", "GreaterDemon");
		when(slayerTaskState.getRemaining()).thenReturn(50, 50);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(1, 0));
		assertTrue(onSlayerTaskChangedCalled);
		assertFalse(onCollectionStateChangedCalled);
	}

	@Test
	public void slayerTask_creatureChange_triggersSlayerCallback()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(true, true);
		when(slayerTaskState.getCreatureName()).thenReturn("GreaterDemon", "BlackDemon");
		when(slayerTaskState.getRemaining()).thenReturn(50, 50);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(2, 0));
		assertTrue(onSlayerTaskChangedCalled);
	}

	@Test
	public void slayerTask_remainingChange_triggersSlayerCallback()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(true, true);
		when(slayerTaskState.getCreatureName()).thenReturn("GreaterDemon", "GreaterDemon");
		when(slayerTaskState.getRemaining()).thenReturn(50, 49);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(3, 0));
		assertTrue(onSlayerTaskChangedCalled);
	}

	@Test
	public void slayerTask_noChange_noSlayerCallback()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(true, true);
		when(slayerTaskState.getCreatureName()).thenReturn("GreaterDemon", "GreaterDemon");
		when(slayerTaskState.getRemaining()).thenReturn(50, 50);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(4, 0));
		assertFalse(onSlayerTaskChangedCalled);
	}

	@Test
	public void scriptScanActive_shortCircuitsBeforeObtainedCheck()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		router.handle(makeEvent(5, 0));
		assertTrue(flagRefreshesCalled);
		assertFalse(onCollectionStateChangedCalled);
		assertFalse(onSlayerTaskChangedCalled);
		verify(collectionState, never()).getTotalObtained();
		verify(syncStateCoordinator, never()).onCollectionStateChanged(anyInt());
	}

	@Test
	public void obtainedCountDelta_firesCollectionAndSlayerCallbacks()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(101);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(6, 0));
		verify(syncStateCoordinator).onCollectionStateChanged(101);
		assertTrue( onCollectionStateChangedCalled,"obtained-count callback should fire");
		assertTrue( onSlayerTaskChangedCalled,"slayer callback fires (new item forces rebuild)");
	}

	@Test
	public void obtainedCountUnchanged_noCollectionCallback()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(100);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(7, 0));
		verify(syncStateCoordinator, never()).onCollectionStateChanged(anyInt());
		assertFalse(onCollectionStateChangedCalled);
		assertFalse(onSlayerTaskChangedCalled);
	}

	@Test
	public void nullCallbacks_doNotNpe()
	{
		router.setCallbacks(null, null, null);
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(false);
		when(collectionState.getTotalObtained()).thenReturn(101);
		when(syncStateCoordinator.getLastObtainedCount()).thenReturn(100);
		router.handle(makeEvent(8, 0));
		verify(syncStateCoordinator).onCollectionStateChanged(101);
	}

	@Test
	public void sequencerForward_alwaysHappens()
	{
		when(slayerTaskState.isTaskActive()).thenReturn(false);
		when(slayerTaskState.getCreatureName()).thenReturn(null);
		when(slayerTaskState.getRemaining()).thenReturn(0);
		when(syncStateCoordinator.isScriptScanActive()).thenReturn(true);
		router.handle(makeEvent(555, 7));
		verify(guidanceSequencer, times(1)).onVarbitChanged(555, 7);
	}
}
