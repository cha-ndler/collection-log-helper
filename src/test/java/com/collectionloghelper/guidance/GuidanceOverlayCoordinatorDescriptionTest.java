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
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link GuidanceOverlayCoordinator#activateGuidance} passes the
 * per-item-resolved description to the panel's {@code updateStepProgress} call.
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>When {@code targetItemId} is non-null and the active step has a matching
 *       {@code perItemStepDescription} entry, {@code updateStepProgress} receives
 *       the override text.</li>
 *   <li>When {@code targetItemId} is null (no item context), {@code updateStepProgress}
 *       receives the static description.</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceOverlayCoordinatorDescriptionTest
{
	private static final int TARGET_ITEM_ID = 1001;
	private static final String STATIC_DESC = "Kill soldiers for armour";
	private static final String OVERRIDE_DESC = "Kill Tier 1 soldiers (40 kills)";

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
	@Mock
	private CollectionLogHelperPanel panel;

	private GuidanceOverlayCoordinator coordinator;

	@Before
	public void setUp() throws Exception
	{
		OverlayStepApplier overlayStepApplier = newOverlayStepApplier();
		WorldMapController worldMapController = newWorldMapController();
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
				WorldMapController.class,
				DynamicTargetManager.class);
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
			overlayStepApplier, worldMapController, dynamicTargetManager);

		coordinator.setPanel(panel);

		when(requirementsChecker.getUnmetRequirements(any())).thenReturn(Collections.emptyList());
		when(requirementsChecker.buildRequirementRows(any())).thenReturn(Collections.emptyList());
	}

	/**
	 * When the active step has a {@code perItemStepDescription} entry for the active
	 * target item, the coordinator must pass the override text — not the static
	 * description — to {@code panel.updateStepProgress}.
	 */
	@Test
	public void activateGuidance_withTargetItemId_panelReceivesOverrideDescription()
	{
		Map<Integer, String> descOverrides = new HashMap<>();
		descOverrides.put(TARGET_ITEM_ID, OVERRIDE_DESC);

		GuidanceStep step = minimalStepWithDescOverride(STATIC_DESC, descOverrides);
		CollectionLogSource source = sourceWithSteps("Shayzien Armour", step);

		// Sequencer reports the step as active at index 0
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		when(guidanceSequencer.getTotalSteps()).thenReturn(1);
		// getActiveSource is used by onStepChanged (not the activation path); lenient to avoid strict fail
		org.mockito.Mockito.lenient().when(guidanceSequencer.getActiveSource()).thenReturn(source);

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), TARGET_ITEM_ID);

		ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
		// The activation path calls the 6-arg overload: (int, int, String, boolean, List, List)
		verify(panel, atLeastOnce()).updateStepProgress(
			anyInt(), anyInt(), descCaptor.capture(), anyBoolean(), anyList(), anyList());

		// The first updateStepProgress call (before deferred item resolution) uses the resolved desc.
		assertEquals(OVERRIDE_DESC, descCaptor.getAllValues().get(0));
	}

	/**
	 * When no target item is set (null), the coordinator must pass the static description
	 * even if the step has a non-empty {@code perItemStepDescription} map.
	 */
	@Test
	public void activateGuidance_withNullTargetItemId_panelReceivesStaticDescription()
	{
		Map<Integer, String> descOverrides = new HashMap<>();
		descOverrides.put(TARGET_ITEM_ID, OVERRIDE_DESC);

		GuidanceStep step = minimalStepWithDescOverride(STATIC_DESC, descOverrides);
		CollectionLogSource source = sourceWithSteps("Shayzien Armour", step);

		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		when(guidanceSequencer.getTotalSteps()).thenReturn(1);
		// getActiveSource is used by onStepChanged (not the activation path); lenient to avoid strict fail
		org.mockito.Mockito.lenient().when(guidanceSequencer.getActiveSource()).thenReturn(source);

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), null);

		ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
		// The activation path calls the 6-arg overload: (int, int, String, boolean, List, List)
		verify(panel, atLeastOnce()).updateStepProgress(
			anyInt(), anyInt(), descCaptor.capture(), anyBoolean(), anyList(), anyList());

		assertEquals(STATIC_DESC, descCaptor.getAllValues().get(0));
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static GuidanceStep minimalStepWithDescOverride(String description,
		Map<Integer, String> perItemStepDescription)
	{
		return new GuidanceStep(
			description,
			perItemStepDescription,
			0, 0, 0,       // worldX, worldY, worldPlane
			0, null, null, null,  // npcId, perItemNpcId, interactAction, dialogOptions
			null, null,     // travelTip, requiredItemIds
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,    // completionItemId, completionItemCount, completionDistance, completionNpcId
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
			null  // dynamicTargetEvaluator
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

	private static CollectionLogSource sourceWithSteps(String name, GuidanceStep... steps)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.MINIGAMES,
			0, 0, 0,
			0, 0,
			null, null, null, 0.0,
			null, 0, false, 0,
			null, 0, null, null,
			Arrays.asList(steps),
			null, null, 0, null, 0,
			Collections.emptyList()
		, null);
	}
}
