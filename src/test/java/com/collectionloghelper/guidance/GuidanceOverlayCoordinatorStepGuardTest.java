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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the in-game OVERLAY idempotency fix (#683): {@code onStepChanged} must
 * clear + re-apply the overlays on a genuine step change but must NOT do so on a
 * same-step re-notify (fired by {@code GuidanceSequencer.onInventoryChanged} on every
 * inventory change while guidance is active), which otherwise produces a visible blink.
 *
 * <p>This mirrors the side-panel guard tests added in #681 ({@code StepProgressViewTest}):
 * the collaborators that perform the visible teardown/rebuild are mocked, and the test
 * asserts the invocation counts of the clear ({@code OverlayDeactivator.clearOverlays})
 * and apply ({@code OverlayStepApplier.apply}) calls. {@code onStepChanged} is package-
 * private-only as a sequencer callback, so it is invoked via reflection.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceOverlayCoordinatorStepGuardTest
{
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

	// Collaborators that own the visible overlay teardown/rebuild; mocked so the test
	// can count clear vs apply invocations directly.
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

		// Stub the mocked collaborators that onStepChanged threads results back from.
		when(overlayDeactivator.clearOverlays(any())).thenReturn(
			new OverlayDeactivator.Result(null, null, null));
		when(overlayStepApplier.apply(any(), anyString(), any(), any(), any(), any(), any()))
			.thenReturn(new OverlayStepApplier.Result(-1, null));
		// applyDynamicItemObjectOverlays() runs inside applyStepToOverlays; keep it a no-op.
		when(dynamicItemObjectTierResolver.resolve(any()))
			.thenReturn(DynamicItemObjectTierResolver.Result.EMPTY);

		CollectionLogSource source = mock(CollectionLogSource.class);
		when(source.getName()).thenReturn("Shades of Mort'ton");
		when(guidanceSequencer.getActiveSource()).thenReturn(source);
	}

	private void fireStepChanged(GuidanceStep step) throws Exception
	{
		onStepChanged.invoke(coordinator, step);
	}

	/**
	 * A same-step re-notify (same sequencer index AND same resolved step reference)
	 * must NOT clear + re-apply the overlays after the first application: the first
	 * genuine notify applies once, and the following identical re-notifies are no-ops.
	 */
	@Test
	public void sameStepRenotify_doesNotClearOrReapplyOverlays() throws Exception
	{
		GuidanceStep step = mock(GuidanceStep.class);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);

		// First notify: genuine application.
		fireStepChanged(step);
		// Two identical re-notifies, as onInventoryChanged would fire on inventory churn.
		fireStepChanged(step);
		fireStepChanged(step);

		// Cleared / applied exactly once, despite three notifications.
		verify(overlayDeactivator, times(1)).clearOverlays(any());
		verify(overlayStepApplier, times(1)).apply(any(), anyString(), any(), any(), any(), any(), any());
		// The panel/InfoBox delegation still runs on every notify (its own #681 guards
		// keep an unchanged re-notify a no-op without suppressing genuine item changes).
		verify(stepChangeHandler, times(3)).handle(any(), any());
	}

	/**
	 * A genuine step change (advance to a new index with a new resolved step) must
	 * clear + re-apply the overlays, even though the previous step had already applied.
	 */
	@Test
	public void genuineStepChange_clearsAndReappliesOverlays() throws Exception
	{
		GuidanceStep stepOne = mock(GuidanceStep.class);
		GuidanceStep stepTwo = mock(GuidanceStep.class);

		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);
		fireStepChanged(stepOne);

		// Advance: new index, new step object.
		when(guidanceSequencer.getCurrentIndex()).thenReturn(1);
		fireStepChanged(stepTwo);

		// Each genuine step applied once -> two clears, two applies.
		verify(overlayDeactivator, times(2)).clearOverlays(any());
		verify(overlayStepApplier, times(2)).apply(any(), anyString(), any(), any(), any(), any(), any());
	}

	/**
	 * Deactivation must reset the tracked overlay-step identity so re-activating the
	 * SAME step (same index + same step object) re-applies on the first notify rather
	 * than being suppressed as a stale "same step" match (avoids a stuck-blank overlay).
	 */
	@Test
	public void deactivateThenSameStep_reappliesOverlays() throws Exception
	{
		when(overlayDeactivator.deactivate(any())).thenReturn(
			new OverlayDeactivator.Result(null, null, null));

		GuidanceStep step = mock(GuidanceStep.class);
		when(guidanceSequencer.getCurrentIndex()).thenReturn(0);

		fireStepChanged(step);
		coordinator.deactivateGuidance();
		// Re-activate onto the identical step index + reference.
		fireStepChanged(step);

		// Two genuine applications: one before deactivation, one after the identity reset.
		verify(overlayDeactivator, times(2)).clearOverlays(any());
		verify(overlayStepApplier, times(2)).apply(any(), anyString(), any(), any(), any(), any(), any());
	}
}
