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
import com.collectionloghelper.ui.widget.SourceRecommendedItemsChipPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link DetailViewBuilder}: the helper that populates the
 * detail-view container with an {@link ItemDetailPanel}. Extracted from
 * {@link CollectionLogHelperPanel#showDetail} as part of the issue #503
 * god-class split.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

	@BeforeEach
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
		return newSource(name, items, null, null);
	}

	private static CollectionLogSource newSource(
		String name,
		List<CollectionLogItem> items,
		String wikiStrategyUrl,
		List<Integer> recommendedItemIds)
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
			null,                          // metaAuthoredDate
			wikiStrategyUrl,               // wikiStrategyUrl (#573)
			recommendedItemIds);           // recommendedItemIds (#573)
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

		assertEquals(
			1, target.getComponentCount(),"Target should hold exactly one child (the ItemDetailPanel)");
		Component child = target.getComponent(0);
		assertTrue( child instanceof ItemDetailPanel,"Child must be an ItemDetailPanel");
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
		assertNotNull( back,"ItemDetailPanel should have a Back button");
		back.doClick();

		assertEquals( 1, backCalls.get(),"Back callback should fire once");
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
		assertNotNull( guideMe,"ItemDetailPanel should have a Guide Me button");
		guideMe.doClick();

		assertSame( source, activatorSource.get(),"Guide Me should pass the source to the activator");
		assertEquals(
			101, activatorItemId.get(),"Guide Me should pass the clicked item's id to the activator");
		assertEquals(
			0, deactivatorCalls.get(),"Deactivator should not fire when guidance was inactive");
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
		assertNotNull( stop,"ItemDetailPanel should expose a Stop Guidance button when guiding this source");
		stop.doClick();

		assertEquals(
			1, deactivatorCalls.get(),"Deactivator should fire once when Stop Guidance is clicked");
		assertEquals( -1, activatorItemId.get(),"Activator must NOT fire when stopping guidance");
	}

	/**
	 * Regression coverage for #576 — the step-control STOP icon used to stop
	 * guidance without flipping the source-level Guide Me / Stop Guidance button
	 * back to "Guide Me", leaving the two buttons desynced. After the fix, the
	 * builder's {@code syncGuidanceState(false, null)} fan-out reaches the
	 * currently-rendered detail panel and reverts its button.
	 */
	@Test
	public void syncGuidanceStateFlipsStopGuidanceButtonBackToGuideMe() throws Exception
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		// Render the detail view in the "currently guiding this source" state so the
		// source-level button reads "Stop Guidance".
		builder.populate(target, item, source, true, source, () -> { });
		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		assertNotNull(
			findFirstButton(detail, "Stop Guidance"),"Detail view should expose a Stop Guidance button before sync");

		// Simulate the step-control STOP icon path: GuidanceOverlayCoordinator
		// .deactivateGuidance -> OverlayDeactivator -> panel.setGuidanceState(false, null, null)
		// -> detailViewBuilder.syncGuidanceState(false, null).
		builder.syncGuidanceState(false, null);
		javax.swing.SwingUtilities.invokeAndWait(() -> { /* flush EDT */ });

		assertNotNull(
			findFirstButton(detail, "Guide Me"),"After sync the detail button must read 'Guide Me'");
		assertEquals(
			null, findFirstButton(detail, "Stop Guidance"),"Stop Guidance button must no longer be present after sync");
	}

	/**
	 * Guards the inverse direction: when guidance is re-activated for the same
	 * source from another surface (e.g. the Top Pick "Guide Me" button), the
	 * detail-view button should flip into "Stop Guidance" mode without needing a
	 * detail-view rebuild.
	 */
	@Test
	public void syncGuidanceStateFlipsGuideMeButtonToStopGuidanceWhenActivatedElsewhere() throws Exception
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> { });
		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		assertNotNull(
			findFirstButton(detail, "Guide Me"),"Initial detail view should show 'Guide Me'");

		builder.syncGuidanceState(true, source);
		javax.swing.SwingUtilities.invokeAndWait(() -> { /* flush EDT */ });

		assertNotNull(
			findFirstButton(detail, "Stop Guidance"),"After sync the detail button must read 'Stop Guidance'");
	}

	/**
	 * Sync calls before any detail view has been populated must be a safe no-op
	 * (the user may toggle guidance from the Quick Guide panel before ever
	 * entering detail view).
	 */
	@Test
	public void syncGuidanceStateBeforePopulateIsNoOp()
	{
		// No populate() call — lastDetail is still null.
		builder.syncGuidanceState(false, null);
		builder.syncGuidanceState(true, source);
		// Reaching this line without an NPE is the assertion.
	}

	/**
	 * #573 — the detail panel must expose a "Wiki Strategy" button on the action
	 * row so players can open the source's wiki Strategies page in their default
	 * browser before activating guidance.
	 */
	@Test
	public void populateRendersWikiStrategyButton()
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> { });

		ItemDetailPanel detail = (ItemDetailPanel) target.getComponent(0);
		JButton wikiStrategy = findFirstButton(detail, "Wiki Strategy");
		assertNotNull( wikiStrategy,"ItemDetailPanel should expose a Wiki Strategy button");
		assertEquals("Wiki Strategy", wikiStrategy.getText());
	}

	/**
	 * #573 — when {@code wikiStrategyUrl} is null the source should derive a
	 * default Strategies URL from the source name with spaces converted to
	 * underscores. Verifies the resolver on {@link CollectionLogSource} rather
	 * than the side-effecting browser launch (which would open a real window).
	 */
	@Test
	public void wikiStrategyUrlDerivesFromSourceNameWhenOverrideIsNull()
	{
		CollectionLogSource graardor =
			newSource("General Graardor", Arrays.asList(item), null, null);
		assertEquals(
			"https://oldschool.runescape.wiki/w/General_Graardor/Strategies",
			graardor.getEffectiveWikiStrategyUrl());
	}

	/**
	 * #573 — when {@code wikiStrategyUrl} is set the override is returned
	 * verbatim, allowing sources whose wiki page name does not match the
	 * source name to point at the correct URL.
	 */
	@Test
	public void wikiStrategyUrlUsesOverrideWhenSet()
	{
		String override = "https://oldschool.runescape.wiki/w/Theatre_of_Blood/Strategies/Verzik_Vitur";
		CollectionLogSource tob = newSource("Theatre of Blood", Arrays.asList(item), override, null);
		assertEquals(override, tob.getEffectiveWikiStrategyUrl());
	}

	/**
	 * #573 — when the source carries an explicit source-level
	 * {@code recommendedItemIds} list, the source-header chip strip is rendered
	 * (visible) with the same order. The strip is a
	 * {@link SourceRecommendedItemsChipPanel} added as a child of the
	 * {@link ItemDetailPanel}.
	 */
	@Test
	public void populateRendersSourceLevelRecommendedStripWhenOverrideIsSet() throws Exception
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		List<Integer> recOverride = Arrays.asList(11802, 12817, 4151); // Bandos chest, helm, whip
		CollectionLogSource overrideSrc =
			newSource("Test Boss", Arrays.asList(item, otherItem), null, recOverride);

		builder.populate(target, item, overrideSrc, false, null, () -> { });

		// Chip-strip update is dispatched via SwingUtilities.invokeLater inside
		// the widget; flush the EDT before asserting visibility.
		javax.swing.SwingUtilities.invokeAndWait(() -> { /* flush */ });

		SourceRecommendedItemsChipPanel strip = findFirstChipPanel(target);
		assertNotNull( strip,"Source-level recommended chip panel must be present");
		assertTrue(
			strip.isVisible(),"Source-level recommended chip panel must be visible when override is non-empty");
	}

	/**
	 * #573 — sources with neither a source-level recommended override nor any
	 * per-step recommended items should hide the chip strip entirely (the
	 * blank-space contract). The base test {@link #source} has empty guidance
	 * steps and no override, so the strip should be hidden.
	 */
	@Test
	public void populateHidesSourceLevelRecommendedStripWhenNoData() throws Exception
	{
		when(collectionState.isItemObtained(anyInt())).thenReturn(false);
		when(requirementsChecker.isAccessible(anyString())).thenReturn(true);

		builder.populate(target, item, source, false, null, () -> { });
		javax.swing.SwingUtilities.invokeAndWait(() -> { /* flush */ });

		SourceRecommendedItemsChipPanel strip = findFirstChipPanel(target);
		assertNotNull( strip,"Source-level recommended chip panel must be present in the tree");
		assertEquals(
			false, strip.isVisible(),"Strip must be hidden when no recommended items exist");
	}

	/**
	 * #573 — when the source has no source-level override but per-step
	 * recommended items exist, {@link CollectionLogSource#getEffectiveRecommendedItemIds()}
	 * rolls up the union (preserving insertion order, deduplicated). Verified
	 * directly on the source rather than via the chip strip to avoid the
	 * heavyweight {@link com.collectionloghelper.data.GuidanceStep} constructor.
	 */
	@Test
	public void effectiveRecommendedItemIdsReturnsOverrideWhenSet()
	{
		List<Integer> recOverride = Arrays.asList(11802, 12817);
		CollectionLogSource src =
			newSource("Test Boss", Arrays.asList(item), null, recOverride);

		assertEquals(recOverride, src.getEffectiveRecommendedItemIds());
	}

	/**
	 * Walks the panel tree to locate the first
	 * {@link SourceRecommendedItemsChipPanel}. Returns {@code null} if none
	 * exists in the subtree.
	 */
	private static SourceRecommendedItemsChipPanel findFirstChipPanel(java.awt.Container root)
	{
		for (Component c : root.getComponents())
		{
			if (c instanceof SourceRecommendedItemsChipPanel)
			{
				return (SourceRecommendedItemsChipPanel) c;
			}
			if (c instanceof java.awt.Container)
			{
				SourceRecommendedItemsChipPanel nested =
					findFirstChipPanel((java.awt.Container) c);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
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
