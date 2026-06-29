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
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.ItemObjectTier;
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
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.lang.reflect.Constructor;
import java.util.Collections;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F3 (stale highlight not cleared): when guidance is on a dynamic-tier step whose
 * tiers no longer match the inventory (the player used their last shade key), an
 * inventory change must CLEAR the tier-driven object/item highlight rather than
 * leave the previous tier outlined. Steps with no dynamic tiers (plain static
 * highlights) must be left untouched on a no-match.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceOverlayCoordinatorStaleHighlightTest
{
	@Mock private Client client;
	@Mock private ClientThread clientThread;
	@Mock private EventBus eventBus;
	@Mock private CollectionLogHelperConfig config;
	@Mock private GuidanceSequencer guidanceSequencer;
	@Mock private RequirementsChecker requirementsChecker;
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
	@Mock private OverlayDeactivator overlayDeactivator;
	@Mock private WorldMapController worldMapController;
	@Mock private DynamicTargetManager dynamicTargetManager;
	@Mock private NpcTrackerHelper npcTrackerHelper;
	@Mock private StepChangeHandler stepChangeHandler;
	@Mock private DynamicItemObjectTierResolver dynamicItemObjectTierResolver;

	private GuidanceOverlayCoordinator coordinator;

	@BeforeEach
	public void setUp() throws Exception
	{
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

		when(guidanceSequencer.isActive()).thenReturn(true);
	}

	@Test
	public void clearsStaleHighlightWhenDynamicTierStepNoLongerMatches()
	{
		GuidanceStep step = mock(GuidanceStep.class);
		when(step.getDynamicItemObjectTiers())
			.thenReturn(Collections.singletonList(mock(ItemObjectTier.class)));
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		// Player used the last key -> no tier matches the inventory.
		when(dynamicItemObjectTierResolver.resolve(step))
			.thenReturn(DynamicItemObjectTierResolver.Result.EMPTY);

		coordinator.onItemContainerChanged();

		verify(objectHighlightOverlay).clearTarget();
		verify(itemHighlightOverlay).clearTarget();
	}

	@Test
	public void doesNotClearStaticHighlightStepOnNoMatch()
	{
		GuidanceStep step = mock(GuidanceStep.class);
		// A plain step with NO dynamic tiers (its highlight is static).
		when(step.getDynamicItemObjectTiers()).thenReturn(null);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(dynamicItemObjectTierResolver.resolve(step))
			.thenReturn(DynamicItemObjectTierResolver.Result.EMPTY);

		coordinator.onItemContainerChanged();

		verify(objectHighlightOverlay, never()).clearTarget();
		verify(itemHighlightOverlay, never()).clearTarget();
	}

	@Test
	public void appliesHighlightAndDoesNotClearWhenTierMatches()
	{
		GuidanceStep step = mock(GuidanceStep.class);
		when(step.getDynamicItemObjectTiers())
			.thenReturn(Collections.singletonList(mock(ItemObjectTier.class)));
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(dynamicItemObjectTierResolver.resolve(step)).thenReturn(
			new DynamicItemObjectTierResolver.Result(
				Collections.singleton(4114), Collections.singletonList(3450), "Open", "Open chest"));

		coordinator.onItemContainerChanged();

		verify(objectHighlightOverlay).setTargetObjectIds(Collections.singleton(4114));
		verify(objectHighlightOverlay, never()).clearTarget();
		verify(itemHighlightOverlay, never()).clearTarget();
	}
}
