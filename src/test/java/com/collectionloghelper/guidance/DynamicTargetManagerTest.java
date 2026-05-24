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
import com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry;
import com.collectionloghelper.overlay.CollectionLogWorldMapPoint;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.WorldMapDestinationOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link DynamicTargetManager}.
 *
 * <p>Focused on the per-tick allocation discipline introduced for issue #527.
 * The pre-#527 implementation allocated a fresh {@code Input} carrier and a
 * fresh {@code Result} wrapper on every game tick — including the (very
 * common) inactive-guidance and active-but-no-evaluator paths.  After the
 * fix:</p>
 *
 * <ul>
 *   <li>The coordinator owns the {@code !isActive()} guard, so the manager
 *       is never invoked at all on inactive ticks.  (Verified at the
 *       coordinator level by {@code DynamicTargetEvaluatorDispatchTest}.)</li>
 *   <li>On active ticks, the three remaining guard-fail paths inside
 *       {@link DynamicTargetManager#tick} return {@code null} without
 *       constructing any wrapper objects, and without touching any of the
 *       four overlay setters or the world-map-point manager.</li>
 *   <li>On the happy path (evaluator returns a non-null point), the manager
 *       still pushes the target to every location-sensitive overlay and
 *       returns a fresh {@link CollectionLogWorldMapPoint}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicTargetManagerTest
{
	private static final WorldPoint DYNAMIC_POINT = new WorldPoint(3200, 3200, 0);
	private static final String STUB_KEY = "test_stub_evaluator";

	@Mock
	private Client client;
	@Mock
	private ClientThread clientThread;
	@Mock
	private CollectionLogHelperConfig config;
	@Mock
	private WorldMapPointManager worldMapPointManager;
	@Mock
	private GuidanceOverlay guidanceOverlay;
	@Mock
	private GuidanceMinimapOverlay guidanceMinimapOverlay;
	@Mock
	private WorldMapRouteOverlay worldMapRouteOverlay;
	@Mock
	private WorldMapDestinationOverlay worldMapDestinationOverlay;

	private DynamicTargetEvaluatorRegistry registry;
	private WorldMapController worldMapController;
	private DynamicTargetManager manager;

	@BeforeEach
	public void setUp() throws Exception
	{
		registry = new DynamicTargetEvaluatorRegistry();
		worldMapController = newWorldMapController();
		manager = newManager();
		lenient().when(config.showHintArrow()).thenReturn(false);
	}

	/**
	 * Guard-fail path #1: step is null.  Manager must return {@code null}
	 * (the cached "no change" sentinel) without touching any overlay.
	 */
	@Test
	public void tick_nullStep_returnsNullAndAllocatesNothing()
	{
		CollectionLogWorldMapPoint result = manager.tick(null, null, null, null);

		assertNull(result);
		verifyNoOverlayInteractions();
	}

	/**
	 * Guard-fail path #2: step has no {@code dynamicTargetEvaluator} key.
	 * Manager must return {@code null} without touching any overlay.
	 */
	@Test
	public void tick_stepWithoutEvaluatorKey_returnsNullAndAllocatesNothing()
	{
		GuidanceStep step = stepWithEvaluator(null);

		CollectionLogWorldMapPoint result = manager.tick(step, null, null, null);

		assertNull(result);
		verifyNoOverlayInteractions();
	}

	/**
	 * Guard-fail path #3: evaluator key is set but not registered.
	 * Manager must return {@code null} without touching any overlay.
	 */
	@Test
	public void tick_unregisteredEvaluatorKey_returnsNullAndAllocatesNothing()
	{
		GuidanceStep step = stepWithEvaluator("definitely_not_registered");

		CollectionLogWorldMapPoint result = manager.tick(step, null, null, null);

		assertNull(result);
		verifyNoOverlayInteractions();
	}

	/**
	 * Guard-fail path #4: registered evaluator returns {@code null}.
	 * Manager must return {@code null} without touching any overlay.
	 */
	@Test
	public void tick_evaluatorReturnsNull_returnsNullAndAllocatesNothing()
	{
		registry.register(STUB_KEY, (c, s) -> null);
		GuidanceStep step = stepWithEvaluator(STUB_KEY);

		CollectionLogWorldMapPoint result = manager.tick(step, null, null, null);

		assertNull(result);
		verifyNoOverlayInteractions();
	}

	/**
	 * Happy path: evaluator returns a real {@link WorldPoint}.  Manager must
	 * push the point to every location-sensitive overlay and return a fresh
	 * {@link CollectionLogWorldMapPoint}.
	 */
	@Test
	public void tick_evaluatorReturnsPoint_pushesToOverlaysAndReturnsNewMapPoint()
	{
		registry.register(STUB_KEY, (c, s) -> DYNAMIC_POINT);
		GuidanceStep step = stepWithEvaluator(STUB_KEY);

		CollectionLogWorldMapPoint result = manager.tick(step, null, null, null);

		assertNotNull(result);
		verify(guidanceOverlay).setTargetPoint(DYNAMIC_POINT);
		verify(guidanceMinimapOverlay).setTargetPoint(DYNAMIC_POINT);
		verify(worldMapRouteOverlay).setTargetPoint(DYNAMIC_POINT);
		verify(worldMapDestinationOverlay).setMapPointActive(true);
		verify(worldMapPointManager).add(result);
	}

	/**
	 * Happy path with a pre-existing active map point: the old point must be
	 * removed from the world-map-point manager before the new one is added.
	 */
	@Test
	public void tick_evaluatorReturnsPoint_removesPreviousActiveMapPoint()
	{
		registry.register(STUB_KEY, (c, s) -> DYNAMIC_POINT);
		GuidanceStep step = stepWithEvaluator(STUB_KEY);
		CollectionLogWorldMapPoint previous = new CollectionLogWorldMapPoint(
			new WorldPoint(1, 2, 0), "prev", null);

		CollectionLogWorldMapPoint result = manager.tick(step, previous, null, null);

		assertNotNull(result);
		verify(worldMapPointManager).remove(previous);
		verify(worldMapPointManager).add(result);
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private void verifyNoOverlayInteractions()
	{
		verify(guidanceOverlay, never()).setTargetPoint(any());
		verify(guidanceMinimapOverlay, never()).setTargetPoint(any());
		verify(worldMapRouteOverlay, never()).setTargetPoint(any());
		verify(worldMapDestinationOverlay, never()).setMapPointActive(true);
		verify(worldMapPointManager, never()).add(any());
		verify(worldMapPointManager, never()).remove(any());
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
			null /* perItemRecommendedItemIds */,
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
			null  // conditionTree
		);
	}

	private WorldMapController newWorldMapController() throws Exception
	{
		Constructor<WorldMapController> ctor = WorldMapController.class.getDeclaredConstructor(
			Client.class, ClientThread.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		return ctor.newInstance(client, clientThread, config);
	}

	private DynamicTargetManager newManager() throws Exception
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
}
