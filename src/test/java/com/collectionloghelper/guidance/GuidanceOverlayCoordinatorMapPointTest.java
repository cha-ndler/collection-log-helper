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
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
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
import java.lang.reflect.Field;
import java.util.Collections;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for world-map point registration and the interaction between
 * {@link CollectionLogWorldMapPoint} and {@link WorldMapDestinationOverlay}.
 *
 * <p>Issue #429: {@link CollectionLogWorldMapPoint} must be registered via
 * {@link WorldMapPointManager#add} so its {@code setJumpOnClick(true)} flag provides
 * click-to-focus-worldmap behaviour on the world-map edge arrow.
 *
 * <p>Issue #410: both {@link CollectionLogWorldMapPoint} and
 * {@link WorldMapDestinationOverlay} render an edge-snap arrow when the destination is
 * off-screen; showing both at once produces a duplicate. The fix: the coordinator adds
 * the map point AND calls {@link WorldMapDestinationOverlay#setMapPointActive(boolean)}
 * so the overlay suppresses its own off-screen arrow while the map point is active.
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceOverlayCoordinatorMapPointTest
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

	private GuidanceOverlayCoordinator coordinator;

	@Before
	public void setUp() throws Exception
	{
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
				WidgetHighlightOverlay.class);
		ctor.setAccessible(true);
		coordinator = ctor.newInstance(
			client, clientThread, eventBus, config,
			guidanceSequencer, requirementsChecker, travelCapabilities,
			playerInventoryState, itemManager, requiredItemResolver,
			worldMapPointManager, infoBoxManager,
			guidanceOverlay, guidanceMinimapOverlay, dialogHighlightOverlay,
			objectHighlightOverlay, itemHighlightOverlay,
			worldMapRouteOverlay, worldMapDestinationOverlay,
			groundItemHighlightOverlay, widgetHighlightOverlay);

		// Default stubs: no unmet requirements; buildRequirementRows called unconditionally
		when(requirementsChecker.getUnmetRequirements(any())).thenReturn(Collections.emptyList());
		when(requirementsChecker.buildRequirementRows(any())).thenReturn(Collections.emptyList());
	}

	/**
	 * When a non-clue, non-step source with a world coordinate is activated,
	 * {@link WorldMapPointManager#add} must be called with a
	 * {@link CollectionLogWorldMapPoint} so that clicking the world-map edge arrow
	 * focuses the world map on the destination (click-to-focus, #429).
	 */
	@Test
	public void activateGuidance_nonClueSource_addsMapPointForClickToFocus()
	{
		CollectionLogSource source = sourceAtCoords("Zulrah", 2268, 3073, 0);

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), null);

		verify(worldMapPointManager).add(any(CollectionLogWorldMapPoint.class));
	}

	/**
	 * When the source has an empty guidance-steps list (non-multi-step path), the same
	 * contract applies: a {@link CollectionLogWorldMapPoint} must be registered.
	 */
	@Test
	public void activateGuidance_emptyStepsSource_addsMapPointForClickToFocus()
	{
		CollectionLogSource source = sourceAtCoordsWithEmptySteps("Barrows", 3565, 3289, 0);

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), null);

		verify(worldMapPointManager).add(any(CollectionLogWorldMapPoint.class));
	}

	/**
	 * After activating guidance with a world coordinate,
	 * {@link WorldMapDestinationOverlay#setMapPointActive(boolean)} must be called with
	 * {@code true} so the overlay suppresses its own edge-snap arrow and avoids a
	 * duplicate alongside the map point's arrow (#410).
	 */
	@Test
	public void activateGuidance_nonClueSource_suppressesDestinationOverlayEdgeArrow()
	{
		CollectionLogSource source = sourceAtCoords("Zulrah", 2268, 3073, 0);

		coordinator.activateGuidance(source, new WorldPoint(3200, 3200, 0), null);

		verify(worldMapDestinationOverlay).setMapPointActive(true);
	}

	/**
	 * Regression test for issue #434: {@link GuidanceOverlayCoordinator#tick()} must not
	 * NPE when {@code pendingShortestPathTarget} is non-null but {@code getLocalPlayer()}
	 * returns null (i.e. during the login sequence before the player entity spawns).
	 *
	 * <p>The old code called {@code client.getLocalPlayer()} twice — once for the null-check
	 * and once for {@code .getWorldLocation()} — creating a TOCTOU window.  The fix snapshots
	 * the player reference once and uses that snapshot exclusively.
	 */
	@Test
	public void tick_nullLocalPlayer_doesNotThrow() throws Exception
	{
		// Arrange — inject a pending shortest-path target via reflection to
		// simulate guidance being active while the player has not yet spawned.
		Field field = GuidanceOverlayCoordinator.class.getDeclaredField("pendingShortestPathTarget");
		field.setAccessible(true);
		field.set(coordinator, new WorldPoint(3200, 3200, 0));

		when(client.getLocalPlayer()).thenReturn(null);

		// Act — must not throw NPE
		coordinator.tick();

		// Assert — ShortestPath event was still posted (without a "start" key)
		verify(eventBus).post(any());
	}

	// ---- helpers ----

	/** Builds a minimal BOSSES source with coordinates but no guidance steps. */
	private static CollectionLogSource sourceAtCoords(
		String name, int x, int y, int plane)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			x, y, plane,
			0, 0,
			null,  // locationDescription
			null,  // waypoints
			null,  // rewardType
			0.0,   // pointsPerHour
			null,  // mutuallyExclusiveSources
			0,     // rollsPerKill
			false, // aggregated
			0,     // afkLevel
			null,  // travelTip
			0,     // npcId
			null,  // interactAction
			null,  // dialogOptions
			null,  // guidanceSteps — no multi-step
			null,  // requirements
			0,     // cumulativeTrackItemId
			null,  // cumulativeTrackObjectIds
			0,     // cumulativeTrackThreshold
			Collections.emptyList()  // items
		);
	}

	/** Builds a BOSSES source with coordinates and an explicitly empty guidanceSteps list. */
	private static CollectionLogSource sourceAtCoordsWithEmptySteps(
		String name, int x, int y, int plane)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			x, y, plane,
			0, 0,
			null,
			null,
			null,
			0.0,
			null,
			0,
			false,
			0,
			null,
			0,
			null,
			null,
			Collections.emptyList(),  // empty guidanceSteps — skips multi-step branch
			null,
			0,
			null,
			0,
			Collections.emptyList()
		);
	}
}
