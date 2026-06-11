/*
 * Copyright (c) 2026, cha-ndler
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
package com.collectionloghelper.guidance;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.EfficiencyModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.lifecycle.CollectionStateChangeHandler;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for #801: auto-advance must not abandon a drop-less grind.
 *
 * <p>The live failure: {@code GuidanceOverlayCoordinator.onSequenceComplete}
 * deactivates guidance (which stops the sequencer and nulls its active source)
 * BEFORE invoking the completion callback, so the handler's not-unlocked
 * re-activate guard saw a null source and fell through to ranked advance.
 *
 * <p>The original unit test missed this because it hand-wired the handler's
 * active-source supplier with a non-null source. These tests drive the REAL
 * integration ordering instead: a real {@link GuidanceSequencer}, a real
 * {@link OverlayDeactivator} (so deactivation really stops the sequencer), a
 * real {@link GuidanceOverlayCoordinator}, and a real
 * {@link CollectionStateChangeHandler} wired exactly like
 * {@code CollectionLogHelperPlugin.startUp()} — then complete a sequence via
 * the sequencer and observe which source gets (re-)activated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceSequenceCompleteOrderingTest
{
	// Sequencer collaborators
	@Mock private PlayerInventoryState inventoryState;
	@Mock private PlayerCollectionState collectionState;
	@Mock private RequirementsChecker requirementsChecker;

	// Coordinator collaborators (all mocked except sequencer + deactivator)
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private EventBus eventBus;
	@Mock private CollectionLogHelperConfig config;
	@Mock private PlayerTravelCapabilities travelCapabilities;
	@Mock private PlayerInventoryState playerInventoryState;
	@Mock private ItemManager itemManager;
	@Mock private RequiredItemResolver requiredItemResolver;
	@Mock private WorldMapPointManager worldMapPointManager;
	@Mock private InfoBoxManager infoBoxManager;
	@Mock private GuidanceOverlay guidanceOverlay;
	@Mock private GuidanceMinimapOverlay guidanceMinimapOverlay;
	@Mock private DialogHighlightOverlay dialogHighlightOverlay;
	@Mock private ObjectHighlightOverlay objectHighlightOverlay;
	@Mock private ItemHighlightOverlay itemHighlightOverlay;
	@Mock private WorldMapRouteOverlay worldMapRouteOverlay;
	@Mock private WorldMapDestinationOverlay worldMapDestinationOverlay;
	@Mock private GroundItemHighlightOverlay groundItemHighlightOverlay;
	@Mock private WidgetHighlightOverlay widgetHighlightOverlay;
	@Mock private OverlayStepApplier overlayStepApplier;
	@Mock private OverlaySourceApplier overlaySourceApplier;
	@Mock private WorldMapController worldMapController;
	@Mock private DynamicTargetManager dynamicTargetManager;
	@Mock private NpcTrackerHelper npcTrackerHelper;
	@Mock private StepChangeHandler stepChangeHandler;
	@Mock private DynamicItemObjectTierResolver dynamicItemObjectTierResolver;

	// Handler collaborators
	@Mock private DataModule dataModule;
	@Mock private EfficiencyModule efficiencyModule;
	@Mock private GuidanceModule guidanceModule;
	@Mock
	@SuppressWarnings("unchecked")
	private java.util.function.BiConsumer<CollectionLogSource, Integer> activateGuidanceCallback;

	private GuidanceSequencer sequencer;
	private GuidanceOverlayCoordinator coordinator;
	private CollectionStateChangeHandler handler;

	private CollectionLogSource giantMole;
	private CollectionLogSource narwhal;
	private List<ScoredItem> rankedSources;

	@BeforeEach
	public void setUp() throws Exception
	{
		// Real sequencer: stopSequence() genuinely nulls the active source,
		// which is the heart of the #801 ordering bug.
		Constructor<GuidanceSequencer> seqCtor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class,
			com.collectionloghelper.guidance.bosses.BossGuidanceRegistry.class);
		seqCtor.setAccessible(true);
		sequencer = seqCtor.newInstance(inventoryState, collectionState, requirementsChecker, null);

		when(inventoryState.hasItem(anyInt())).thenReturn(false);
		when(inventoryState.hasAllItems(any())).thenReturn(true);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		// Real deactivator wired to the REAL sequencer — deactivateGuidance()
		// must actually stop the sequence, as it does live.
		OverlayDeactivator overlayDeactivator = new OverlayDeactivator(
			client, clientThread, eventBus, config,
			worldMapPointManager, infoBoxManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay,
			npcTrackerHelper, sequencer);

		Constructor<GuidanceOverlayCoordinator> ctor =
			GuidanceOverlayCoordinator.class.getDeclaredConstructor(
				Client.class, ClientThread.class, EventBus.class, CollectionLogHelperConfig.class,
				GuidanceSequencer.class, RequirementsChecker.class, PlayerTravelCapabilities.class,
				PlayerInventoryState.class, ItemManager.class, RequiredItemResolver.class,
				WorldMapPointManager.class, InfoBoxManager.class, GuidanceOverlay.class,
				GuidanceMinimapOverlay.class, DialogHighlightOverlay.class, ObjectHighlightOverlay.class,
				ItemHighlightOverlay.class, WorldMapRouteOverlay.class, WorldMapDestinationOverlay.class,
				GroundItemHighlightOverlay.class, WidgetHighlightOverlay.class, OverlayStepApplier.class,
				OverlaySourceApplier.class, OverlayDeactivator.class, WorldMapController.class,
				DynamicTargetManager.class, NpcTrackerHelper.class, StepChangeHandler.class,
				DynamicItemObjectTierResolver.class);
		ctor.setAccessible(true);
		coordinator = ctor.newInstance(
			client, clientThread, eventBus, config,
			sequencer, requirementsChecker, travelCapabilities,
			playerInventoryState, itemManager, requiredItemResolver,
			worldMapPointManager, infoBoxManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay,
			overlayStepApplier, overlaySourceApplier, overlayDeactivator,
			worldMapController, dynamicTargetManager, npcTrackerHelper, stepChangeHandler,
			dynamicItemObjectTierResolver);

		// Stub the mocked collaborators the activation/step path threads results from.
		when(overlayStepApplier.apply(any(), anyString(), any(), any(), any(), any(), any()))
			.thenReturn(new OverlayStepApplier.Result(-1, null));
		when(dynamicItemObjectTierResolver.resolve(any()))
			.thenReturn(DynamicItemObjectTierResolver.Result.EMPTY);
		when(requirementsChecker.getUnmetRequirements(anyString()))
			.thenReturn(Collections.emptyList());

		// Real handler, wired exactly like CollectionLogHelperPlugin.startUp().
		handler = new CollectionStateChangeHandler(
			client, config, dataModule, efficiencyModule, guidanceModule);
		when(dataModule.getCollectionState()).thenReturn(collectionState);
		when(config.autoAdvanceGuidance()).thenReturn(true);

		narwhal = mock(CollectionLogSource.class);
		when(narwhal.getName()).thenReturn("Narwhal");
		rankedSources = Collections.singletonList(
			new ScoredItem(narwhal, 50.0, 1, "next pick", false, 50.0, null, 50.0));

		handler.setCallbacks(
			() -> { /* panel rebuild flags — not under test */ },
			() -> rankedSources,
			activateGuidanceCallback,
			sequencer::wasTargetSlotUnlocked);
		coordinator.setOnSequenceCompleteCallback(handler::handleSequenceComplete);

		giantMole = mock(CollectionLogSource.class);
		when(giantMole.getName()).thenReturn("Giant Mole");
		when(giantMole.getCategory()).thenReturn(CollectionLogCategory.BOSSES);
		when(giantMole.getGuidanceHelperKey()).thenReturn(null);
		when(giantMole.getGuidanceSteps()).thenReturn(
			Collections.singletonList(manualKillStep()));
		when(giantMole.getItems()).thenReturn(Collections.singletonList(
			new CollectionLogItem(7418, "Mole claw", 1.0 / 25, false, null, 0, 0, false, false)));
	}

	private static GuidanceStep manualKillStep()
	{
		return new GuidanceStep(
			"Kill the Giant Mole",
			null,           // perItemStepDescription
			0, 0, 0,        // worldX, worldY, worldPlane
			0, null, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,     // completionItemId, completionItemCount, completionDistance, completionNpcId
			null,           // completionNpcIds
			null,           // worldMessage
			0, null, null,  // objectId, objectIds, objectInteractAction
			null, null,     // highlightItemIds, groundItemIds
			null,           // completionChatPattern
			0, 0,           // completionVarbitId, completionVarbitValue
			false,          // useItemOnObject
			0,              // objectMaxDistance
			null,           // objectFilterTiles
			null,           // highlightWidgetIds
			0, 0,           // loopBackToStep, loopCount
			null,           // skipIfHasAnyItemIds
			null,           // dynamicItemObjectTiers
			null,           // completionZone
			null,           // conditionalAlternatives
			null,           // section
			null,           // waypoints
			null,           // dynamicTargetEvaluator
			null,           // conditionTree
			null,           // perItemStepPriority
			null,           // activityObtainableItemIds
			null            // restockIfMissingAllItemIds (#719)
		);
	}

	/** Activates guidance and completes the single manual step like a live boss kill. */
	private void activateAndCompleteSequence()
	{
		coordinator.activateGuidance(giantMole, null, null);
		assertTrue(sequencer.isActive(), "sequence should be active on the manual kill step");

		// Completing the final step fires sequence-complete through the REAL
		// chain: coordinator.onSequenceComplete -> deactivateGuidance ->
		// OverlayDeactivator.deactivate -> sequencer.stopSequence -> callback.
		sequencer.advanceStep();

		assertFalse(sequencer.isActive(), "sequence must be stopped after completion");
		assertNull(sequencer.getActiveSource(),
			"the live bug precondition: the sequencer source is gone by callback time");
	}

	/**
	 * The #801 live repro: a boss kill completes the sequence with no drop
	 * (target slot not unlocked) and the source still has missing items.
	 * Auto-advance must re-activate the SAME source — not roll to the next
	 * ranked pick.
	 */
	@Test
	public void dropLessCompletion_reActivatesCompletedSource()
	{
		activateAndCompleteSequence();

		verify(activateGuidanceCallback).accept(giantMole, null);
		verify(activateGuidanceCallback, never()).accept(narwhal, null);
	}

	/**
	 * The live Castle Wars case: a source whose items are ALL obtained can
	 * only ever complete drop-less. Re-activating would loop forever — it must
	 * advance to the next ranked pick instead.
	 */
	@Test
	public void fullyObtainedSource_advancesInsteadOfRelooping()
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(true);

		activateAndCompleteSequence();

		verify(activateGuidanceCallback, never()).accept(giantMole, null);
		verify(activateGuidanceCallback).accept(narwhal, null);
	}

	/**
	 * When the target slot unlocked during the sequence, the pre-#801 advance
	 * behaviour is preserved: move on to the next ranked pick.
	 */
	@Test
	public void slotUnlocked_advancesToNextRankedPick() throws Exception
	{
		coordinator.activateGuidance(giantMole, null, null);
		assertTrue(sequencer.isActive());

		// Mark the target slot unlocked the way the live path does before completion.
		java.lang.reflect.Field f = GuidanceSequencer.class.getDeclaredField("targetSlotUnlocked");
		f.setAccessible(true);
		f.setBoolean(sequencer, true);

		sequencer.advanceStep();

		verify(activateGuidanceCallback).accept(narwhal, null);
		verify(activateGuidanceCallback, never()).accept(giantMole, null);
	}
}
