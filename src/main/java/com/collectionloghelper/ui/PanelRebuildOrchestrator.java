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

import java.awt.Component;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Captures and restores transient view state across a panel rebuild so the user
 * does not lose their place when the panel tears down and re-builds the list
 * container.
 *
 * <p>Two pieces of state are managed:
 * <ul>
 *   <li>Vertical scroll position of the enclosing {@link JScrollPane} (if any).</li>
 *   <li>The set of expanded category names — looked up by matching
 *       {@link CategorySummaryPanel#getCategoryName()} against the children of
 *       the list container before and after the rebuild.</li>
 * </ul>
 *
 * <p>This class is intentionally not a Guice {@code @Singleton}; it is
 * constructed inside the panel's constructor and bound to that panel's
 * Swing components.
 *
 * <p>The expected call sequence wrapping a rebuild is:
 * <pre>{@code
 *   RebuildSnapshot snap = orchestrator.capture();
 *   // ... tear down and re-populate listContainer ...
 *   orchestrator.restoreExpanded(snap);
 *   // ... revalidate / repaint / showListView ...
 *   orchestrator.deferScrollRestore(snap);
 * }</pre>
 */
public final class PanelRebuildOrchestrator
{
	private final JComponent panel;
	private final JPanel listContainer;

	public PanelRebuildOrchestrator(JComponent panel, JPanel listContainer)
	{
		this.panel = panel;
		this.listContainer = listContainer;
	}

	/**
	 * Snapshots the current scroll position and expanded-category state.
	 * Caller is responsible for EDT discipline (matches the original inline
	 * code, which ran inside {@code SwingUtilities.invokeLater}).
	 */
	public RebuildSnapshot capture()
	{
		int scrollPosition = 0;
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
			JScrollPane.class, panel);
		if (scrollPane != null)
		{
			scrollPosition = scrollPane.getVerticalScrollBar().getValue();
		}

		Set<String> expandedCategories = new HashSet<>();
		for (Component comp : listContainer.getComponents())
		{
			if (comp instanceof CategorySummaryPanel)
			{
				CategorySummaryPanel csp = (CategorySummaryPanel) comp;
				if (csp.isExpanded())
				{
					expandedCategories.add(csp.getCategoryName());
				}
			}
		}

		return new RebuildSnapshot(scrollPane, scrollPosition, expandedCategories);
	}

	/**
	 * Restores expanded state on category panels in the list container whose
	 * name matched a previously-expanded entry.
	 */
	public void restoreExpanded(RebuildSnapshot snapshot)
	{
		if (snapshot == null || snapshot.expandedCategories.isEmpty())
		{
			return;
		}
		for (Component comp : listContainer.getComponents())
		{
			if (comp instanceof CategorySummaryPanel)
			{
				CategorySummaryPanel csp = (CategorySummaryPanel) comp;
				if (snapshot.expandedCategories.contains(csp.getCategoryName()))
				{
					csp.setExpanded(true);
				}
			}
		}
	}

	/**
	 * Schedules a deferred scroll-position restore via
	 * {@link SwingUtilities#invokeLater} so the new component tree has a chance
	 * to lay out before we re-apply the saved offset. No-op when the panel is
	 * not inside a scroll pane or when the saved offset is zero.
	 */
	public void deferScrollRestore(RebuildSnapshot snapshot)
	{
		if (snapshot == null
			|| snapshot.scrollPane == null
			|| snapshot.scrollPosition <= 0)
		{
			return;
		}
		final JScrollPane scrollPane = snapshot.scrollPane;
		final int restorePosition = snapshot.scrollPosition;
		SwingUtilities.invokeLater(() ->
			scrollPane.getVerticalScrollBar().setValue(restorePosition));
	}

	/**
	 * Immutable snapshot of view state captured before a rebuild.
	 */
	public static final class RebuildSnapshot
	{
		private final JScrollPane scrollPane;
		private final int scrollPosition;
		private final Set<String> expandedCategories;

		RebuildSnapshot(JScrollPane scrollPane, int scrollPosition,
			Set<String> expandedCategories)
		{
			this.scrollPane = scrollPane;
			this.scrollPosition = scrollPosition;
			this.expandedCategories = Collections.unmodifiableSet(
				new HashSet<>(expandedCategories));
		}

		public int getScrollPosition()
		{
			return scrollPosition;
		}

		public Set<String> getExpandedCategories()
		{
			return expandedCategories;
		}
	}
}
