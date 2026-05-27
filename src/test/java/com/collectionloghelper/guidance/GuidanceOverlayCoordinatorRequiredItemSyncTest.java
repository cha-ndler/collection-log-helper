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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
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
import java.lang.reflect.Method;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression for cha-ndler/collection-log-helper#717: the in-game overlay's
 * "Item requirements" availability must stay in sync with the side panel across
 * same-step re-notifies.
 *
 * <p>The overlay's required-item availability is pushed by {@link OverlayStepApplier}
 * only on a genuine step change. On a same-step re-notify (fired on every inventory
 * change while guidance is active -- e.g. a bank withdrawal during Shades of Mort'ton)
 * the apply path is skipped to avoid a visual blink (#683), but the side panel
 * ({@link StepChangeHandler}) still re-resolves availability every time. Before the
 * fix this left the overlay holding a stale snapshot from step entry: a freshly
 * scanned bank item (Oak pyre logs) showed "(bank)" in the panel but not the overlay.
 *
 * <p>The fix re-resolves the overlay's required items on every re-notify through the
 * same {@link RequiredItemResolver} the panel uses, so both surfaces agree.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceOverlayCoordinatorRequiredItemSyncTest
{
	private static final int OAK_PYRE_LOGS = 3438;

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
	private OverlayStepApplier overlayStepApplier;
	@Mock
	private OverlaySourceApplier overlaySourceApplier;
	@Mock
	private OverlayDeactivator overlayDeactivator;
	@Mock
	private WorldMapController worldMapController;
	@Mock
	private DynamicTargetManager dynamicTargetManager;
	@Mock
	private NpcTrackerHelper npcTrackerHelper;
	@Mock
	private StepChangeHandler stepChangeHandler;
	@Mock
	private DynamicItemObjectTierResolver dynamicItemObjectTierResolver;

	private GuidanceOverlayCoordinator coordinator;
	private Method onStepChanged;

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

		onStepChanged = GuidanceOverlayCoordinator.class.getDeclaredMethod(
			"onStepChanged", GuidanceStep.class);
		onStepChanged.setAccessible(true);

		when(overlayDeactivator.clearOverlays(any())).thenReturn(
			new OverlayDeactivator.Result(null, null, null));
		when(overlayStepApplier.apply(any(), anyString(), any(), any(), any(), any(), any()))
			.thenReturn(new OverlayStepApplier.Result(-1, null));
		when(dynamicItemObjectTierResolver.resolve(any()))
			.thenReturn(DynamicItemObjectTierResolver.Result.EMPTY);

		CollectionLogSource source = mock(CollectionLogSource.class);
		when(source.getName()).thenReturn("Shades of Mort'ton");
		when(guidanceSequencer.getActiveSource()).thenReturn(source);

		// Execute client-thread runnables inline so the deferred resolve is observable.
		doAnswer(inv ->
		{
			((Runnable) inv.getArgument(0)).run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));
	}

	private void fireStepChanged(GuidanceStep step) throws Exception
	{
		onStepChanged.invoke(coordinator, step);
	}

	private static RequiredItemDisplay row(Status status)
	{
		return new RequiredItemDisplay(OAK_PYRE_LOGS, "Pyre logs", status);
	}

	/**
	 * On a same-step re-notify after a bank scan, the overlay must be re-pushed the
	 * freshly resolved availability (IN_BANK) instead of keeping the step-entry
	 * snapshot (MISSING) -- matching what the side panel resolves on the same notify.
	 */
	@Test
	public void sameStepRenotify_refreshesOverlayRequiredItemsToMatchPanel() throws Exception
	{
		GuidanceStep step = mock(GuidanceStep.class);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);

		// Step entry: Oak pyre logs not yet known (bank not scanned) -> MISSING.
		when(requiredItemResolver.resolve(step))
			.thenReturn(Collections.singletonList(row(Status.MISSING)));
		fireStepChanged(step);

		// Bank scan completes; the resolver now reports the item IN_BANK. This is the
		// SAME state both the overlay refresh and the panel (StepChangeHandler) read.
		when(requiredItemResolver.resolve(step))
			.thenReturn(Collections.singletonList(row(Status.IN_BANK)));

		// Inventory change re-notifies the same step (same index + same reference).
		fireStepChanged(step);

		// The overlay's latest required-items push must reflect the refreshed availability.
		ArgumentCaptor<List<RequiredItemDisplay>> captor = ArgumentCaptor.forClass(List.class);
		verify(guidanceOverlay, atLeastOnce()).setRequiredItems(captor.capture());
		List<RequiredItemDisplay> latest = captor.getValue();
		assertEquals(1, latest.size());
		assertEquals(Status.IN_BANK, latest.get(0).getStatus(),
			"Overlay must re-resolve to IN_BANK on a same-step re-notify, matching the panel (#717)");

		// And the re-resolve must route through the SAME RequiredItemResolver the panel uses.
		verify(requiredItemResolver, atLeastOnce()).resolve(step);
	}
}
