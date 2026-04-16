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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OverlayRegistryTest
{
	@Mock
	private OverlayManager overlayManager;
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
	private GroundItemHighlightOverlay groundItemHighlightOverlay;
	@Mock
	private WidgetHighlightOverlay widgetHighlightOverlay;

	private OverlayRegistry registry;

	@Before
	public void setUp()
	{
		registry = new OverlayRegistry(
			overlayManager,
			guidanceOverlay,
			guidanceMinimapOverlay,
			dialogHighlightOverlay,
			objectHighlightOverlay,
			itemHighlightOverlay,
			worldMapRouteOverlay,
			groundItemHighlightOverlay,
			widgetHighlightOverlay
		);
	}

	// ========================================================================
	// registerAll
	// ========================================================================

	@Test
	public void testRegisterAllAddsGuidanceOverlay()
	{
		// Act
		registry.registerAll();

		// Assert
		verify(overlayManager).add(guidanceOverlay);
	}

	@Test
	public void testRegisterAllAddsGuidanceMinimapOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(guidanceMinimapOverlay);
	}

	@Test
	public void testRegisterAllAddsDialogHighlightOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(dialogHighlightOverlay);
	}

	@Test
	public void testRegisterAllAddsObjectHighlightOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(objectHighlightOverlay);
	}

	@Test
	public void testRegisterAllAddsItemHighlightOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(itemHighlightOverlay);
	}

	@Test
	public void testRegisterAllAddsWorldMapRouteOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(worldMapRouteOverlay);
	}

	@Test
	public void testRegisterAllAddsGroundItemHighlightOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(groundItemHighlightOverlay);
	}

	@Test
	public void testRegisterAllAddsWidgetHighlightOverlay()
	{
		registry.registerAll();
		verify(overlayManager).add(widgetHighlightOverlay);
	}

	@Test
	public void testRegisterAllAddsEightOverlays()
	{
		// Arrange + Act
		registry.registerAll();

		// Assert — exactly 8 add() calls, no more
		verify(overlayManager, times(8)).add(any());
	}

	@Test
	public void testRegisterAllPreservesRegistrationOrder()
	{
		// Arrange
		InOrder inOrder = inOrder(overlayManager);

		// Act
		registry.registerAll();

		// Assert — order preserved from source
		inOrder.verify(overlayManager).add(guidanceOverlay);
		inOrder.verify(overlayManager).add(guidanceMinimapOverlay);
		inOrder.verify(overlayManager).add(dialogHighlightOverlay);
		inOrder.verify(overlayManager).add(objectHighlightOverlay);
		inOrder.verify(overlayManager).add(itemHighlightOverlay);
		inOrder.verify(overlayManager).add(worldMapRouteOverlay);
		inOrder.verify(overlayManager).add(groundItemHighlightOverlay);
		inOrder.verify(overlayManager).add(widgetHighlightOverlay);
	}

	// ========================================================================
	// unregisterAll
	// ========================================================================

	@Test
	public void testUnregisterAllRemovesEightOverlays()
	{
		registry.unregisterAll();
		verify(overlayManager, times(8)).remove(any());
	}

	@Test
	public void testUnregisterAllRemovesGuidanceOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(guidanceOverlay);
	}

	@Test
	public void testUnregisterAllRemovesGuidanceMinimapOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(guidanceMinimapOverlay);
	}

	@Test
	public void testUnregisterAllRemovesDialogHighlightOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(dialogHighlightOverlay);
	}

	@Test
	public void testUnregisterAllRemovesObjectHighlightOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(objectHighlightOverlay);
	}

	@Test
	public void testUnregisterAllRemovesItemHighlightOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(itemHighlightOverlay);
	}

	@Test
	public void testUnregisterAllRemovesWorldMapRouteOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(worldMapRouteOverlay);
	}

	@Test
	public void testUnregisterAllRemovesGroundItemHighlightOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(groundItemHighlightOverlay);
	}

	@Test
	public void testUnregisterAllRemovesWidgetHighlightOverlay()
	{
		registry.unregisterAll();
		verify(overlayManager).remove(widgetHighlightOverlay);
	}

	@Test
	public void testUnregisterAllPreservesRemovalOrder()
	{
		InOrder inOrder = inOrder(overlayManager);

		registry.unregisterAll();

		inOrder.verify(overlayManager).remove(guidanceOverlay);
		inOrder.verify(overlayManager).remove(guidanceMinimapOverlay);
		inOrder.verify(overlayManager).remove(dialogHighlightOverlay);
		inOrder.verify(overlayManager).remove(objectHighlightOverlay);
		inOrder.verify(overlayManager).remove(itemHighlightOverlay);
		inOrder.verify(overlayManager).remove(worldMapRouteOverlay);
		inOrder.verify(overlayManager).remove(groundItemHighlightOverlay);
		inOrder.verify(overlayManager).remove(widgetHighlightOverlay);
	}

	// ========================================================================
	// Symmetry
	// ========================================================================

	@Test
	public void testRegisterThenUnregisterIsSymmetric()
	{
		// Arrange + Act
		registry.registerAll();
		registry.unregisterAll();

		// Assert — each overlay added exactly once and removed exactly once
		verify(overlayManager, times(1)).add(guidanceOverlay);
		verify(overlayManager, times(1)).remove(guidanceOverlay);
		verify(overlayManager, times(1)).add(guidanceMinimapOverlay);
		verify(overlayManager, times(1)).remove(guidanceMinimapOverlay);
		verify(overlayManager, times(1)).add(widgetHighlightOverlay);
		verify(overlayManager, times(1)).remove(widgetHighlightOverlay);
	}

	@Test
	public void testRegisterAllCalledTwiceInvokesAddTwice()
	{
		// Arrange + Act — simulates calling startUp twice (shouldn't happen but documents behavior)
		registry.registerAll();
		registry.registerAll();

		// Assert — add called twice for each overlay
		verify(overlayManager, times(2)).add(guidanceOverlay);
		verify(overlayManager, times(2)).add(widgetHighlightOverlay);
	}

	@Test
	public void testUnregisterAllCalledWithoutRegisterStillInvokesRemove()
	{
		// Unregistering without prior registration should still call remove (OverlayManager handles no-op)
		registry.unregisterAll();
		verify(overlayManager, times(1)).remove(guidanceOverlay);
		verify(overlayManager, times(1)).remove(widgetHighlightOverlay);
	}
}
