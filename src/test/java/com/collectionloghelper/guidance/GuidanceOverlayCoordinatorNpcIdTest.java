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
package com.collectionloghelper.guidance;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests that {@link GuidanceOverlayCoordinator#activateGuidance} passes the
 * per-item-resolved NPC ID to {@link GuidanceOverlay#setTargetNpcId} when a step
 * has a {@code perItemNpcId} override for the active target item.
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>When {@code targetItemId} is non-null and the active step has a matching
 *       {@code perItemNpcId} entry, {@code guidanceOverlay.setTargetNpcId} receives
 *       the override NPC ID.</li>
 *   <li>When {@code targetItemId} is null (no item context), {@code guidanceOverlay.setTargetNpcId}
 *       receives the static {@code npcId}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceOverlayCoordinatorNpcIdTest
{
	private static final int TARGET_ITEM_ID = 2003;
	private static final int STATIC_NPC_ID = 5000;
	private static final int OVERRIDE_NPC_ID = 6001;

	@Mock
	private Client client;
	@Mock
	private ClientThread clientThread;
	@Mock
	private EventBus eventBus;
	@Mock
	private CollectionLogHelperConfig config;
	@Mock
	private GuidanceSequencer guidanceSequencer;
	@Mock
	private com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry dynamicTargetEvaluatorRegistry;
	@Mock
	private RequirementsChecker requirementsChecker;
	@Mock
	private PlayerTravelCapabilities travelCapabilities;
	@Mock
	private PlayerInventoryState playerInventoryState;
	@Mock
	private ItemManager itemManager;
	@Mock
	private RequiredItemResolver requiredItemResolver;
	@Mock
	private WorldMapPointManager worldMapPointManager;
	@Mock
	private InfoBoxManager infoBoxManager;
	@Mock
	private GuidanceOverlay guidanceOverlay;
	@Mock
	private GuidanceMinimapOverlay guidanceMinimapOverlay;
	@Mock
	private DialogHighlightOverlay dialogHighlightOverlay;
	@Mock
	private ObjectHighlightOverlay objectHighlightOverlay;
	@Mock
	private ItemHighlightOverlay itemHighlightOverlay;
	@Mock
	private WorldMapRouteOverlay worldMapRouteOverlay;
	@Mock
	private WorldMapDestinationOverlay worldMapDestinationOverlay;
	@Mock
	private GroundItemHighlightOverlay groundItemHighlightOverlay;
	@Mock
	private WidgetHighlightOverlay widgetHighlightOverlay;

	private GuidanceOverlayCoordinator coordinator;

	@BeforeEach
	public void setUp() throws Exception
	{
		OverlayStepApplier overlayStepApplier = newOverlayStepApplier();
		OverlaySourceApplier overlaySourceApplier = newOverlaySourceApplier();
		WorldMapController worldMapController = newWorldMapController();
		NpcTrackerHelper npcTrackerHelper = newNpcTrackerHelper();
		OverlayDeactivator overlayDeactivator = newOverlayDeactivator(npcTrackerHelper);
		StepChangeHandler stepChangeHandler = newStepChangeHandler();
		DynamicItemObjectTierResolver dynamicItemObjectTierResolver = newDynamicItemObjectTierResolver();
		Constructor<GuidanceOverlayCoordinator> ctor =
			GuidanceOverlayCoordinator.class.getDeclaredConstructor(
				Client.class,
				ClientThread.class,
				EventBus.class,
				CollectionLogHelperConfig.class,
				GuidanceSequencer.class,
				RequirementsChecker.class,
				PlayerTravelCapabilities.class,
				PlayerInventoryState.class,
				ItemManager.class,
				RequiredItemResolver.class,
				WorldMapPointManager.class,
				InfoBoxManager.class,
				GuidanceOverlay.class,
				GuidanceMinimapOverlay.class,
				DialogHighlightOverlay.class,
				ObjectHighlightOverlay.class,
				ItemHighlightOverlay.class,
				WorldMapRouteOverlay.class,
				WorldMapDestinationOverlay.class,
				GroundItemHighlightOverlay.class,
				WidgetHighlightOverlay.class,
				OverlayStepApplier.class,
				OverlaySourceApplier.class,
				OverlayDeactivator.class,
				WorldMapController.class,
				DynamicTargetManager.class,
				NpcTrackerHelper.class,
				StepChangeHandler.class,
				DynamicItemObjectTierResolver.class);
		ctor.setAccessible(true);
		DynamicTargetManager dynamicTargetManager = newDynamicTargetManager(worldMapController);
		coordinator = ctor.newInstance(
			client, clientThread, eventBus, config,
			guidanceSequencer, requirementsChecker, travelCapabilities,
			playerInventoryState, itemManager, requiredItemResolver,
			worldMapPointManager, infoBoxManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay,
			overlayStepApplier, overlaySourceApplier, overlayDeactivator,
			worldMapController, dynamicTargetManager, npcTrackerHelper, stepChangeHandler,
			dynamicItemObjectTierResolver);

		when(requirementsChecker.getUnmetRequirements(any())).thenReturn(Collections.emptyList());
		when(requirementsChecker.buildRequirementRows(any())).thenReturn(Collections.emptyList());
	}

	/**
	 * When the active step has a {@code perItemNpcId} entry for the active target item,
	 * the coordinator must pass the override NPC ID to {@code guidanceOverlay.setTargetNpcId}.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void activateGuidance_withTargetItemId_overlayReceivesOverrideNpcId()
	{
		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(TARGET_ITEM_ID, OVERRIDE_NPC_ID);

		GuidanceStep step = minimalStepWithNpcOverride(STATIC_NPC_ID, npcOverrides);
		CollectionLogSource source = sourceWithSteps("Perilous Moons", step);

		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		when(guidanceSequencer.getActiveSource()).thenReturn(source);
		// getTotalSteps not needed: panel is null and InfoBox is not created in this test
		org.mockito.Mockito.lenient().when(guidanceSequencer.getTotalSteps()).thenReturn(1);

		// Capture the onStepChanged callback passed to startSequence and invoke it
		// synchronously so applyStepToOverlays (which calls setTargetNpcId) runs.
		doAnswer(inv ->
		{
			Consumer<GuidanceStep> onStepChanged = inv.getArgument(1);
			onStepChanged.accept(step);
			return null;
		}).when(guidanceSequencer).startSequence(any(), any(), any());

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), TARGET_ITEM_ID);

		ArgumentCaptor<Integer> npcIdCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(guidanceOverlay, atLeastOnce()).setTargetNpcId(npcIdCaptor.capture());

		assertEquals(OVERRIDE_NPC_ID, (int) npcIdCaptor.getAllValues().get(0));
	}

	/**
	 * When no target item is set (null), the coordinator must pass the static NPC ID
	 * even if the step has a non-empty {@code perItemNpcId} map.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void activateGuidance_withNullTargetItemId_overlayReceivesStaticNpcId()
	{
		Map<Integer, Integer> npcOverrides = new HashMap<>();
		npcOverrides.put(TARGET_ITEM_ID, OVERRIDE_NPC_ID);

		GuidanceStep step = minimalStepWithNpcOverride(STATIC_NPC_ID, npcOverrides);
		CollectionLogSource source = sourceWithSteps("Perilous Moons", step);

		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		when(guidanceSequencer.getActiveSource()).thenReturn(source);
		org.mockito.Mockito.lenient().when(guidanceSequencer.getTotalSteps()).thenReturn(1);

		// Capture the onStepChanged callback passed to startSequence and invoke it
		// synchronously so applyStepToOverlays (which calls setTargetNpcId) runs.
		doAnswer(inv ->
		{
			Consumer<GuidanceStep> onStepChanged = inv.getArgument(1);
			onStepChanged.accept(step);
			return null;
		}).when(guidanceSequencer).startSequence(any(), any(), any());

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), null);

		ArgumentCaptor<Integer> npcIdCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(guidanceOverlay, atLeastOnce()).setTargetNpcId(npcIdCaptor.capture());

		assertEquals(STATIC_NPC_ID, (int) npcIdCaptor.getAllValues().get(0));
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static GuidanceStep minimalStepWithNpcOverride(int npcId,
		Map<Integer, Integer> perItemNpcId)
	{
		return new GuidanceStep(
			"Kill the boss",
			null,           // perItemStepDescription
			3200, 3200, 0, // worldX, worldY, worldPlane — non-zero to reach the NPC branch
			npcId, perItemNpcId, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.ACTOR_DEATH,
			0, 0, 0, npcId,  // completionItemId, completionItemCount, completionDistance, completionNpcId
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
			null, // section
			null, // waypoints
			null,  // dynamicTargetEvaluator
			null,  // conditionTree
						null, // perItemStepPriority
						null  // activityObtainableItemIds
		
			, null // restockIfMissingAllItemIds (#719)
		);
	}

	private WorldMapController newWorldMapController() throws Exception
	{
		Constructor<WorldMapController> ctor = WorldMapController.class.getDeclaredConstructor(
			Client.class, ClientThread.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, config);
	}

	private DynamicTargetManager newDynamicTargetManager(WorldMapController worldMapController)
		throws Exception
	{
		Constructor<DynamicTargetManager> ctor = DynamicTargetManager.class.getDeclaredConstructor(
			Client.class, DynamicTargetEvaluatorRegistry.class, WorldMapPointManager.class,
			GuidanceOverlay.class, GuidanceMinimapOverlay.class,
			WorldMapRouteOverlay.class, WorldMapDestinationOverlay.class,
			WorldMapController.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, dynamicTargetEvaluatorRegistry, worldMapPointManager,
			guidanceOverlay, guidanceMinimapOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay, worldMapController);
	}

	private NpcTrackerHelper newNpcTrackerHelper() throws Exception
	{
		Constructor<NpcTrackerHelper> ctor = NpcTrackerHelper.class.getDeclaredConstructor(
			Client.class, ClientThread.class, GuidanceOverlay.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, guidanceOverlay);
	}

	private StepChangeHandler newStepChangeHandler() throws Exception
	{
		Constructor<StepChangeHandler> ctor = StepChangeHandler.class.getDeclaredConstructor(
			ClientThread.class, GuidanceSequencer.class, RequiredItemResolver.class);
		ctor.setAccessible(true);
		return ctor.newInstance(clientThread, guidanceSequencer, requiredItemResolver);
	}

	private DynamicItemObjectTierResolver newDynamicItemObjectTierResolver() throws Exception
	{
		Constructor<DynamicItemObjectTierResolver> ctor =
			DynamicItemObjectTierResolver.class.getDeclaredConstructor(PlayerInventoryState.class);
		ctor.setAccessible(true);
		return ctor.newInstance(playerInventoryState);
	}

	private OverlayStepApplier newOverlayStepApplier() throws Exception
	{
		Constructor<OverlayStepApplier> ctor = OverlayStepApplier.class.getDeclaredConstructor(
			Client.class, ClientThread.class, EventBus.class, CollectionLogHelperConfig.class,
			GuidanceSequencer.class, PlayerTravelCapabilities.class, RequiredItemResolver.class,
			WorldMapPointManager.class, GuidanceOverlay.class, GuidanceMinimapOverlay.class,
			DialogHighlightOverlay.class, ObjectHighlightOverlay.class, ItemHighlightOverlay.class,
			WorldMapRouteOverlay.class, WorldMapDestinationOverlay.class,
			GroundItemHighlightOverlay.class, WidgetHighlightOverlay.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, eventBus, config, guidanceSequencer,
			travelCapabilities, requiredItemResolver, worldMapPointManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay);
	}

	private OverlaySourceApplier newOverlaySourceApplier() throws Exception
	{
		Constructor<OverlaySourceApplier> ctor = OverlaySourceApplier.class.getDeclaredConstructor(
			Client.class, ClientThread.class, EventBus.class, CollectionLogHelperConfig.class,
			RequirementsChecker.class, WorldMapPointManager.class,
			GuidanceOverlay.class, GuidanceMinimapOverlay.class, DialogHighlightOverlay.class,
			WorldMapRouteOverlay.class, WorldMapDestinationOverlay.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, eventBus, config,
			requirementsChecker, worldMapPointManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay);
	}

	private OverlayDeactivator newOverlayDeactivator(NpcTrackerHelper npcTrackerHelper) throws Exception
	{
		Constructor<OverlayDeactivator> ctor = OverlayDeactivator.class.getDeclaredConstructor(
			Client.class, ClientThread.class, EventBus.class, CollectionLogHelperConfig.class,
			WorldMapPointManager.class, InfoBoxManager.class,
			GuidanceOverlay.class, GuidanceMinimapOverlay.class, DialogHighlightOverlay.class,
			ObjectHighlightOverlay.class, ItemHighlightOverlay.class,
			WorldMapRouteOverlay.class, WorldMapDestinationOverlay.class,
			GroundItemHighlightOverlay.class, WidgetHighlightOverlay.class,
			NpcTrackerHelper.class, GuidanceSequencer.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, eventBus, config,
			worldMapPointManager, infoBoxManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay,
			npcTrackerHelper, guidanceSequencer);
	}

	private static CollectionLogSource sourceWithSteps(String name, GuidanceStep... steps)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			0, 0, 0,
			0, 0,
			null, null, null, 0.0,
			null, 0, false, 0,
			null, 0, null, null,
			Arrays.asList(steps),
			null,
			null, 0, null, 0,
			Collections.emptyList()
		, null, null, null);
	}
}
