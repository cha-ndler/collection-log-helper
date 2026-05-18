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
package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DetailViewBuilder}: the helper that populates the
 * detail-view container with an {@link ItemDetailPanel}. Extracted from
 * {@link CollectionLogHelperPanel#showDetail} as part of the issue #503
 * god-class split.
 */
@RunWith(MockitoJUnitRunner.class)
public class DetailViewBuilderTest
{
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;
	@Mock
	private ItemManager itemManager;
	@Mock
	private ClueCompletionEstimator clueEstimator;

	private DetailViewBuilder builder;
	private JPanel target;
	private CollectionLogItem item;
	private CollectionLogItem otherItem;
	private CollectionLogSource source;
	private AtomicReference<CollectionLogSource> activatorSource;
	private AtomicInteger activatorItemId;
	private AtomicInteger deactivatorCalls;

	@Before
	public void setUp()
	{
		// ItemManager.getImage(int) returns an AsyncBufferedImage that
		// ItemDetailPanel immediately uses for an ImageIcon and onLoaded callback.
		AsyncBufferedImage img = mock(AsyncBufferedImage.class);
		lenient().when(itemManager.getImage(anyInt())).thenReturn(img);
		lenient().when(requirementsChecker.getUnmetRequirements(anyString())).thenReturn(Collections.emptyList());
		lenient().when(requirementsChecker.hasFairyRingAccess()).thenReturn(false);

		activatorSource = new AtomicReference<>();
		activatorItemId = new AtomicInteger(-1);
		deactivatorCalls = new AtomicInteger(0);

		BiConsumer<CollectionLogSource, Integer> activator = (s, id) ->
		{
			activatorSource.set(s);
			activatorItemId.set(id);
		};
		Runnable deactivator = () -> deactivatorCalls.incrementAndGet();

		builder = new DetailViewBuilder(
			collectionState, requirementsChecker, itemManager, clueEstimator,
			activator, deactivator);

		target = new JPanel(new BorderLayout());

		item = new CollectionLogItem(101, "Test Item", 1.0 / 128, false, "wiki", 0, 0, false, false);
		otherItem = new CollectionLogItem(102, "Other Item", 1.0 / 256, false, "wiki2", 0, 0, false, false);
		List<CollectionLogItem> items = Arrays.asList(item, otherItem);
		source = newSource("Test Boss", items);
	}

	private static CollectionLogSource newSource(String name, List<CollectionLogItem> items)
	{
		return new CollectionLogSource(
			name,                          // name
			CollectionLogCategory.BOSSES,  // category
			0, 0, 0,                       // worldX, worldY, worldPlane
			60,                            // killTimeSeconds
			60,                            // ironKillTimeSeconds
			"",                            // locationDescription
			Collections.emptyList(),       // waypoints
			null,                          // rewardType
			0.0,                           // pointsPerHour
			Collections.emptyList(),       // mutuallyExclusiveSources
			1,                             // rollsPerKill
			false,                         // aggregated
			0,                             // afkLevel
			"",                            // travelTip
			-1,                            // npcId
			"",                            // interactAction
			Collections.emptyList(),       // dialogOptions
			Collections.emptyList(),       // guidanceSteps
			null,                          // guidanceHelperKey
			null,                          // requirements
			0,                             // cumulativeTrackItemId
			Collections.emptyList(),       // cumulativeTrackObjectIds
			0,                             // cumulativeTrackThreshold
			items,                         // items
			null);                         // metaAuthoredDate
	}

	@Test
	public void populateClearsTargetAndAddsItemDetailPanelToNorth()
	{
		// pre-seed target with a stale child to verify it gets cleared
		target.add(new JLabel("stale"), BorderLayout.NORTH);
		assertEquals(1, target.getComponentCount());

		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible("Test Boss")).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> { });

		assertEquals("Target should hold exactly one child (the ItemDetailPanel)",
			1, target.getComponentCount());
		Component child = target.getComponent(0);
		assertTrue("Child must be an ItemDetailPanel", child instanceof ItemDetailPanel);
		// The child should be pinned to NORTH on the BorderLayout target.
		assertSame(child, ((BorderLayout) target.getLayout()).getLayoutComponent(BorderLayout.NORTH));
	}

	@Test
	public void populateCountsObtainedItemsAcrossSource()
	{
		// Only `item` is obtained, not `otherItem`. The builder must iterate
		// every item in the source to compute sourceObtained / sourceTotal.
		when(collectionState.isItemObtained(101)).thenReturn(true);
		when(collectionState.isItemObtained(102)).thenReturn(false);
		when(requirementsChecker.isAccessible("Test Boss")).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> { });

		// Verify the iteration touched every item in the source (the loop
		// also queries item 101 in addition to the explicit `obtained` check,
		// so `atLeastOnce()` is the right assertion strength here).
		org.mockito.Mockito.verify(collectionState, org.mockito.Mockito.atLeastOnce()).isItemObtained(101);
		org.mockito.Mockito.verify(collectionState).isItemObtained(102);
	}

	@Test
	public void populateRebindsBackCallbackToCallerSuppliedRunnable()
	{
		AtomicInteger backCalls = new AtomicInteger(0);
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> backCalls.incrementAndGet());

		// Locate the Back button on the produced ItemDetailPanel and click it.
		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		javax.swing.JButton back = findFirstButton(detail, "Back");
		assertNotNull("ItemDetailPanel should have a Back button", back);
		back.doClick();

		assertEquals("Back callback should fire once", 1, backCalls.get());
	}

	@Test
	public void populateWiresGuidanceActivatorWithSourceAndItemId()
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		// guidanceActive=false so the Guide Me button is not in the Stop state.
		builder.populate(target, item, source, false, null, () -> { });

		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		javax.swing.JButton guideMe = findFirstButton(detail, "Guide Me");
		assertNotNull("ItemDetailPanel should have a Guide Me button", guideMe);
		guideMe.doClick();

		assertSame("Guide Me should pass the source to the activator", source, activatorSource.get());
		assertEquals("Guide Me should pass the clicked item's id to the activator",
			101, activatorItemId.get());
		assertEquals("Deactivator should not fire when guidance was inactive",
			0, deactivatorCalls.get());
	}

	@Test
	public void populateWiresGuidanceDeactivatorWhenGuidingThisSource()
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		// guidanceActive=true AND guidedSource.name == source.name => isGuidingThis=true.
		// In that state the button is in "Stop Guidance" mode and triggers the deactivator.
		builder.populate(target, item, source, true, source, () -> { });

		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		javax.swing.JButton stop = findFirstButton(detail, "Stop Guidance");
		assertNotNull("ItemDetailPanel should expose a Stop Guidance button when guiding this source", stop);
		stop.doClick();

		assertEquals("Deactivator should fire once when Stop Guidance is clicked",
			1, deactivatorCalls.get());
		assertEquals("Activator must NOT fire when stopping guidance", -1, activatorItemId.get());
	}

	private static javax.swing.JButton findFirstButton(java.awt.Container root, String labelSubstring)
	{
		for (Component c : root.getComponents())
		{
			if (c instanceof javax.swing.JButton)
			{
				javax.swing.JButton b = (javax.swing.JButton) c;
				String text = b.getText();
				if (text != null && text.contains(labelSubstring))
				{
					return b;
				}
			}
			if (c instanceof java.awt.Container)
			{
				javax.swing.JButton nested = findFirstButton((java.awt.Container) c, labelSubstring);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}
}
