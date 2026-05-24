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
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry;
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
import net.runelite.api.Client;
import net.runelite.api.Player;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Tests the sequencer-dispatch path for dynamic target evaluators.
 *
 * <p>Verifies that {@link GuidanceOverlayCoordinator#tick()} invokes the
 * registered {@link DynamicTargetEvaluator} when the active guidance step
 * has a non-null {@code dynamicTargetEvaluator} key, and pushes the returned
 * {@link WorldPoint} to the overlay.
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>When the active step has a registered evaluator key and the evaluator
 *       returns a non-null point, {@code guidanceOverlay.setTargetPoint} is
 *       called with that point.</li>
 *   <li>When the active step has no {@code dynamicTargetEvaluator} key (null),
 *       the evaluator is never invoked and {@code setTargetPoint} is not called
 *       via the tick path.</li>
 *   <li>When the evaluator key is unknown (not registered), the tick path is a
 *       no-op for overlay updates.</li>
 *   <li>When the evaluator returns null, the tick path is a no-op.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicTargetEvaluatorDispatchTest
{
	private static final WorldPoint DYNAMIC_POINT = new WorldPoint(1641, 4000, 0);

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
	private Player localPlayer;

	/** Registry with one stub evaluator that always returns {@link #DYNAMIC_POINT}. */
	private DynamicTargetEvaluatorRegistry registry;
	private GuidanceOverlayCoordinator coordinator;

	private static final String STUB_KEY = "test_stub_evaluator";

	@BeforeEach
	public void setUp() throws Exception
	{
		registry = new DynamicTargetEvaluatorRegistry();
		// Register a predictable stub evaluator
		registry.register(STUB_KEY, (c, step) -> DYNAMIC_POINT);

		OverlayStepApplier overlayStepApplier = newOverlayStepApplier();
		OverlaySourceApplier overlaySourceApplier = newOverlaySourceApplier();
		WorldMapController worldMapController = newWorldMapController();
		DynamicTargetManager dynamicTargetManager = newDynamicTargetManager(worldMapController);
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

		lenient().when(client.getLocalPlayer()).thenReturn(localPlayer);
		lenient().when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
		lenient().when(localPlayer.getWorldView()).thenReturn(null);
		lenient().when(config.showHintArrow()).thenReturn(false);
	}

	/**
	 * When the active step has a registered evaluator key, tick() must call the
	 * evaluator and push its result to guidanceOverlay.setTargetPoint.
	 */
	@Test
	public void tick_stepWithRegisteredEvaluatorKey_pushesDynamicPointToOverlay()
	{
		GuidanceStep step = stepWithEvaluator(STUB_KEY);
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);

		coordinator.tick();

		ArgumentCaptor<WorldPoint> pointCaptor = ArgumentCaptor.forClass(WorldPoint.class);
		verify(guidanceOverlay, atLeastOnce()).setTargetPoint(pointCaptor.capture());
		assertEquals(DYNAMIC_POINT, pointCaptor.getValue());
	}

	/**
	 * When the active step has no dynamicTargetEvaluator (null key), tick() must
	 * not call setTargetPoint on the overlay via the dynamic path.
	 */
	@Test
	public void tick_stepWithNullEvaluatorKey_doesNotPushDynamicPoint()
	{
		GuidanceStep step = stepWithEvaluator(null);
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);

		coordinator.tick();

		verify(guidanceOverlay, never()).setTargetPoint(any());
	}

	/**
	 * When the evaluator key is not registered, tick() is a no-op for overlay
	 * updates (registry returns null, code path short-circuits).
	 */
	@Test
	public void tick_stepWithUnknownEvaluatorKey_doesNotPushDynamicPoint()
	{
		GuidanceStep step = stepWithEvaluator("unknown_evaluator_key");
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);

		coordinator.tick();

		verify(guidanceOverlay, never()).setTargetPoint(any());
	}

	/**
	 * When the evaluator returns null, tick() must not push anything to the overlay.
	 */
	@Test
	public void tick_evaluatorReturnsNull_doesNotPushDynamicPoint()
	{
		registry.register(STUB_KEY, (c, step) -> null);

		GuidanceStep step = stepWithEvaluator(STUB_KEY);
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);

		coordinator.tick();

		verify(guidanceOverlay, never()).setTargetPoint(any());
	}

	/**
	 * When guidance is not active, tick() must not call any evaluator.
	 */
	@Test
	public void tick_guidanceNotActive_doesNotPushDynamicPoint()
	{
		when(guidanceSequencer.isActive()).thenReturn(false);

		coordinator.tick();

		verify(guidanceOverlay, never()).setTargetPoint(any());
	}

	// ── Helper ────────────────────────────────────────────────────────────────

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
		return ctor.newInstance(client, registry, worldMapPointManager,
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

	private static GuidanceStep stepWithEvaluator(String evaluatorKey)
	{
		return new GuidanceStep(
			"Dynamic step",
			null,  // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			null,
			null /* perItemRecommendedItemIds */,  // recommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,  // completionNpcIds
			null,  // worldMessage
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null,  // conditionalAlternatives
			null,  // section
			null,  // waypoints
			evaluatorKey,  // dynamicTargetEvaluator
			null,  // conditionTree
			null  // perItemStepPriority
		);
	}
}
