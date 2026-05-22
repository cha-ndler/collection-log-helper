/*
 * Copyright (c) 2025, cha-ndler
 */
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.guidance.GuidanceMovementTracker;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.learning.KillTimeTracker;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.sync.CollectionLogNetImporter;
import com.collectionloghelper.sync.SourceKcStore;
import com.collectionloghelper.sync.TempleOsrsKcSyncer;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.concurrent.ExecutorService;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests for {@link PluginShutdownRoutine} -- the Wave 15 (#503) extraction
 * that owns the body of {@code CollectionLogHelperPlugin.shutDown()}.
 *
 * <p>Covers the canonical teardown sequence, null-tolerance for the panel
 * and executor (in case startUp failed early), and the plugin-private state
 * clear callback.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PluginShutdownRoutineTest
{
	@Mock private ChatCommandManager chatCommandManager;
	@Mock private AuthoringLogger authoringLogger;
	@Mock private SyncModule syncModule;
	@Mock private ClientToolbar clientToolbar;
	@Mock private OverlayRegistry overlayRegistry;
	@Mock private EventBus eventBus;
	@Mock private SceneEventRouter sceneEventRouter;
	@Mock private GuidanceModule guidanceModule;
	@Mock private EfficiencyModule efficiencyModule;
	@Mock private DataModule dataModule;
	@Mock private PlayerTravelCapabilities travelCapabilities;
	@Mock private PlayerLocationResolver playerLocationResolver;
	@Mock private com.collectionloghelper.player.PohTeleportInventory pohTeleportInventory;
	@Mock private com.collectionloghelper.player.EquippedItemState equippedItemState;
	@Mock private com.collectionloghelper.player.DiaryTierState diaryTierState;
	@Mock private com.collectionloghelper.player.SkillCapePerkState skillCapePerkState;
	@Mock private com.collectionloghelper.player.PlayerQuestProgressState playerQuestProgressState;

	@Mock private CollectionLogNetImporter collectionLogNetImporter;
	@Mock private SyncStateCoordinator syncStateCoordinator;
	@Mock private SourceKcStore sourceKcStore;
	@Mock private TempleOsrsKcSyncer templeOsrsKcSyncer;
	@Mock private GuidanceEventRouter guidanceEventRouter;
	@Mock private GuidanceMovementTracker guidanceMovementTracker;
	@Mock private GuidanceOverlayCoordinator guidanceCoordinator;
	@Mock private GuidanceOverlay guidanceOverlay;
	@Mock private KillTimeTracker killTimeTracker;
	@Mock private PlayerCollectionState collectionState;
	@Mock private DataSyncState dataSyncState;
	@Mock private PlayerBankState playerBankState;
	@Mock private PlayerInventoryState playerInventoryState;
	@Mock private SlayerTaskState slayerTaskState;
	@Mock private PluginDataManager pluginDataManager;
	@Mock private ExecutorService executor;
	@Mock private CollectionLogHelperPanel panel;

	private NavigationButton navButton;
	private PluginShutdownRoutine routine;

	@BeforeEach
	public void setUp()
	{
		when(syncModule.getCollectionLogNetImporter()).thenReturn(collectionLogNetImporter);
		when(syncModule.getSyncStateCoordinator()).thenReturn(syncStateCoordinator);
		when(syncModule.getSourceKcStore()).thenReturn(sourceKcStore);
		when(syncModule.getTempleOsrsKcSyncer()).thenReturn(templeOsrsKcSyncer);
		when(guidanceModule.getGuidanceEventRouter()).thenReturn(guidanceEventRouter);
		when(guidanceModule.getGuidanceMovementTracker()).thenReturn(guidanceMovementTracker);
		when(guidanceModule.getGuidanceCoordinator()).thenReturn(guidanceCoordinator);
		when(guidanceModule.getGuidanceOverlay()).thenReturn(guidanceOverlay);
		when(efficiencyModule.getKillTimeTracker()).thenReturn(killTimeTracker);
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		when(dataModule.getDataSyncState()).thenReturn(dataSyncState);
		when(dataModule.getPlayerBankState()).thenReturn(playerBankState);
		when(dataModule.getPlayerInventoryState()).thenReturn(playerInventoryState);
		when(dataModule.getSlayerTaskState()).thenReturn(slayerTaskState);
		when(dataModule.getPluginDataManager()).thenReturn(pluginDataManager);

		// NavigationButton is final and cannot be mocked; construct a minimal real one
		navButton = NavigationButton.builder()
			.tooltip("test")
			.icon(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB))
			.priority(1)
			.panel(panel)
			.build();

		routine = new PluginShutdownRoutine(
			chatCommandManager, authoringLogger, syncModule, clientToolbar, overlayRegistry,
			eventBus, sceneEventRouter, guidanceModule, efficiencyModule, dataModule,
			travelCapabilities, playerLocationResolver,
			pohTeleportInventory, equippedItemState, diaryTierState,
			skillCapePerkState, playerQuestProgressState);
	}

	@Test
	public void tearDown_unregistersChatCommand()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(chatCommandManager).unregisterCommand("clh");
	}

	@Test
	public void tearDown_closesAuthoringLogger()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(authoringLogger).close();
	}

	@Test
	public void tearDown_doesNotInteractWithCollectionLogNetImporter()
	{
		// #478: CollectionLogNetImporter no longer owns its executor; the
		// runtime-provided ScheduledExecutorService is shut down by the
		// RuneLite runtime, not by this routine.  The teardown must not
		// invoke any method on the importer.
		routine.tearDown(panel, navButton, executor, () -> { });
		org.mockito.Mockito.verifyNoInteractions(collectionLogNetImporter);
	}

	@Test
	public void tearDown_shutsDownPanelWhenPresent()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(panel).shutDown();
	}

	@Test
	public void tearDown_skipsPanelShutdownWhenNull()
	{
		routine.tearDown(null, navButton, executor, () -> { });
		verify(panel, never()).shutDown();
	}

	@Test
	public void tearDown_removesNavButton()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(clientToolbar).removeNavigation(navButton);
	}

	@Test
	public void tearDown_unregistersAllOverlays()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(overlayRegistry).unregisterAll();
	}

	@Test
	public void tearDown_unregistersEventBusListeners()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(eventBus).unregister(sceneEventRouter);
		verify(eventBus).unregister(guidanceEventRouter);
		verify(eventBus).unregister(guidanceMovementTracker);
		verify(eventBus).unregister(killTimeTracker);
	}

	@Test
	public void tearDown_resetsCollaboratorState()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(killTimeTracker).reset();
		verify(guidanceCoordinator).deactivateGuidance();
		verify(syncStateCoordinator).reset();
		verify(sourceKcStore).clear();
		// #479: TempleOsrsKcSyncer no longer owns its executor; the
		// runtime-provided ScheduledExecutorService is shut down by the
		// RuneLite runtime, not by this routine.  The teardown must not
		// invoke any method on the syncer.
		org.mockito.Mockito.verifyNoInteractions(templeOsrsKcSyncer);
		verify(playerLocationResolver).reset();
		verify(guidanceOverlay).setShowCollectionLogReminder(false);
		verify(guidanceOverlay).setShowBankReminder(false);
		verify(dataSyncState).reset();
		verify(playerBankState).reset();
		verify(playerInventoryState).reset();
		verify(slayerTaskState).reset();
		verify(travelCapabilities).reset();
		verify(collectionState).clearState();
		verify(pluginDataManager).reset();
	}

	@Test
	public void tearDown_shutsDownExecutorWhenPresent()
	{
		routine.tearDown(panel, navButton, executor, () -> { });
		verify(executor).shutdownNow();
	}

	@Test
	public void tearDown_skipsExecutorWhenNull()
	{
		routine.tearDown(panel, navButton, null, () -> { });
		verify(executor, never()).shutdownNow();
	}

	@Test
	public void tearDown_invokesClearPluginStateCallback()
	{
		boolean[] called = {false};
		routine.tearDown(panel, navButton, executor, () -> called[0] = true);
		assertTrue( called[0],"clearPluginState callback must fire once");
	}

	@Test
	public void tearDown_tolerantOfNullClearCallback()
	{
		// Must not NPE
		routine.tearDown(panel, navButton, executor, null);
	}

	@Test
	public void tearDown_chatCommandUnregisterPrecedesEventBusUnregister()
	{
		// The original shutDown body unregisters the chat command first so any
		// in-flight !clh handler doesn't fire mid-teardown. Lock the order.
		routine.tearDown(panel, navButton, executor, () -> { });
		InOrder order = inOrder(chatCommandManager, eventBus);
		order.verify(chatCommandManager).unregisterCommand("clh");
		order.verify(eventBus).unregister(sceneEventRouter);
	}
}
