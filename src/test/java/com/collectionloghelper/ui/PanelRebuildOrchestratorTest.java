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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PanelRebuildOrchestrator}: the helper that snapshots
 * and restores scroll position + expanded-category state across a panel
 * rebuild. Extracted from {@link CollectionLogHelperPanel#rebuild()} as part
 * of issue #503 god-class splits.
 */
@RunWith(MockitoJUnitRunner.class)
public class PanelRebuildOrchestratorTest
{
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;
	@Mock
	private ItemManager itemManager;
	@Mock
	private CategorySummaryPanel.ItemClickHandler clickHandler;

	private JPanel hostPanel;
	private JPanel listContainer;
	private JScrollPane scrollPane;
	private PanelRebuildOrchestrator orchestrator;

	@Before
	public void setUp()
	{
		when(collectionState.getCategoryCount(any())).thenReturn(0);
		when(collectionState.getCategoryMax(any())).thenReturn(10);

		listContainer = new JPanel();
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));

		hostPanel = new JPanel(new BorderLayout());
		// Give the host a tall preferred size so the enclosing JScrollPane's
		// vertical scrollbar has actual range to scroll within.
		hostPanel.setPreferredSize(new Dimension(200, 5000));
		hostPanel.add(listContainer, BorderLayout.NORTH);

		scrollPane = new JScrollPane(hostPanel);
		scrollPane.setSize(200, 400);
		scrollPane.doLayout();
		scrollPane.getViewport().doLayout();

		orchestrator = new PanelRebuildOrchestrator(hostPanel, listContainer);
	}

	private CategorySummaryPanel newCategoryPanel(CollectionLogCategory category)
	{
		return new CategorySummaryPanel(
			category,
			Collections.<CollectionLogSource>emptyList(),
			collectionState,
			requirementsChecker,
			itemManager,
			clickHandler,
			false);
	}

	private JScrollBar vBar()
	{
		return scrollPane.getVerticalScrollBar();
	}

	@Test
	public void captureReturnsZeroScrollAndEmptySetWhenNothingExpanded()
	{
		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();

		assertNotNull(snapshot);
		assertEquals(0, snapshot.getScrollPosition());
		assertTrue(snapshot.getExpandedCategories().isEmpty());
	}

	@Test
	public void captureRecordsScrollPositionFromEnclosingScrollPane()
	{
		vBar().setValue(250);

		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();

		assertEquals(250, snapshot.getScrollPosition());
	}

	@Test
	public void captureRecordsOnlyCurrentlyExpandedCategoryNames()
	{
		CategorySummaryPanel bosses = newCategoryPanel(CollectionLogCategory.BOSSES);
		CategorySummaryPanel raids = newCategoryPanel(CollectionLogCategory.RAIDS);
		CategorySummaryPanel clues = newCategoryPanel(CollectionLogCategory.CLUES);
		listContainer.add(bosses);
		listContainer.add(raids);
		listContainer.add(clues);
		bosses.setExpanded(true);
		clues.setExpanded(true);

		Set<String> expanded = orchestrator.capture().getExpandedCategories();

		assertEquals(2, expanded.size());
		assertTrue(expanded.contains("Bosses"));
		assertTrue(expanded.contains("Clues"));
		assertFalse(expanded.contains("Raids"));
	}

	@Test
	public void captureIgnoresNonCategoryChildren()
	{
		listContainer.add(new JLabel("ignored"));
		listContainer.add(newCategoryPanel(CollectionLogCategory.BOSSES));

		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();

		// non-category child must not appear; the unexpanded category is also absent.
		assertTrue(snapshot.getExpandedCategories().isEmpty());
	}

	@Test
	public void restoreExpandedReExpandsMatchingCategoriesAfterRebuild()
	{
		CategorySummaryPanel bosses = newCategoryPanel(CollectionLogCategory.BOSSES);
		CategorySummaryPanel raids = newCategoryPanel(CollectionLogCategory.RAIDS);
		listContainer.add(bosses);
		listContainer.add(raids);
		bosses.setExpanded(true);

		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();

		// Simulate a rebuild: tear down and re-create with same categories.
		listContainer.removeAll();
		CategorySummaryPanel bosses2 = newCategoryPanel(CollectionLogCategory.BOSSES);
		CategorySummaryPanel raids2 = newCategoryPanel(CollectionLogCategory.RAIDS);
		listContainer.add(bosses2);
		listContainer.add(raids2);

		orchestrator.restoreExpanded(snapshot);

		assertTrue("Bosses should be re-expanded", bosses2.isExpanded());
		assertFalse("Raids was not in snapshot - must remain collapsed", raids2.isExpanded());
	}

	@Test
	public void restoreExpandedIsNoOpWhenSnapshotIsEmpty()
	{
		CategorySummaryPanel bosses = newCategoryPanel(CollectionLogCategory.BOSSES);
		listContainer.add(bosses);

		PanelRebuildOrchestrator.RebuildSnapshot empty = orchestrator.capture();
		orchestrator.restoreExpanded(empty);

		assertFalse(bosses.isExpanded());
	}

	@Test
	public void restoreExpandedDoesNotReExpandRemovedCategory()
	{
		CategorySummaryPanel bosses = newCategoryPanel(CollectionLogCategory.BOSSES);
		listContainer.add(bosses);
		bosses.setExpanded(true);

		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();

		// Rebuild produces a different set of categories - Bosses is gone.
		listContainer.removeAll();
		CategorySummaryPanel raids = newCategoryPanel(CollectionLogCategory.RAIDS);
		listContainer.add(raids);

		orchestrator.restoreExpanded(snapshot);

		assertFalse("Raids was not in snapshot - must remain collapsed", raids.isExpanded());
	}

	@Test
	public void deferScrollRestoreSchedulesValueOnVerticalScrollBar() throws Exception
	{
		vBar().setValue(400);
		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();
		vBar().setValue(0);

		orchestrator.deferScrollRestore(snapshot);
		// Flush the EDT to let the deferred invokeLater run.
		SwingUtilities.invokeAndWait(() -> { /* no-op flush */ });

		assertEquals(400, vBar().getValue());
	}

	@Test
	public void deferScrollRestoreIsNoOpWhenScrollPositionIsZero() throws Exception
	{
		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orchestrator.capture();
		vBar().setValue(123);

		orchestrator.deferScrollRestore(snapshot);
		SwingUtilities.invokeAndWait(() -> { /* no-op flush */ });

		// No restore was scheduled - the post-call scroll value sticks.
		assertEquals(123, vBar().getValue());
	}

	@Test
	public void deferScrollRestoreIsNoOpWhenPanelNotInScrollPane() throws Exception
	{
		JPanel orphanPanel = new JPanel();
		JPanel orphanList = new JPanel();
		PanelRebuildOrchestrator orphan = new PanelRebuildOrchestrator(orphanPanel, orphanList);

		PanelRebuildOrchestrator.RebuildSnapshot snapshot = orphan.capture();

		// Just call it; assert no exception thrown.
		orphan.deferScrollRestore(snapshot);
		SwingUtilities.invokeAndWait(() -> { /* no-op flush */ });

		assertEquals(0, snapshot.getScrollPosition());
	}

	@Test
	public void snapshotExpandedCategoriesIsImmutable()
	{
		CategorySummaryPanel bosses = newCategoryPanel(CollectionLogCategory.BOSSES);
		listContainer.add(bosses);
		bosses.setExpanded(true);

		Set<String> snap = orchestrator.capture().getExpandedCategories();

		try
		{
			snap.add("Mutated");
			throw new AssertionError("Expected snapshot set to be unmodifiable");
		}
		catch (UnsupportedOperationException expected)
		{
			// pass
		}
	}
}
