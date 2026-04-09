/*
 * Copyright (c) 2025, Chandler
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class CategorySummaryPanel extends JPanel
{
	private static final Color PROGRESS_BAR_COLOR = new Color(0, 200, 0);
	private static final Color PROGRESS_BG_COLOR = new Color(60, 60, 60);

	private final JPanel itemsContainer;
	private final String categoryName;
	private final JLabel nameLabel;
	private final int obtained;
	private final int total;
	private boolean expanded = false;
	private boolean populated = false;

	// Data retained for lazy population
	private List<CollectionLogSource> lazySources;
	private PlayerCollectionState lazyCollectionState;
	private RequirementsChecker lazyRequirementsChecker;
	private ItemManager lazyItemManager;
	private ItemClickHandler lazyClickHandler;
	private boolean lazyHideObtained;

	CategorySummaryPanel(CollectionLogCategory category, List<CollectionLogSource> sources,
		PlayerCollectionState collectionState, RequirementsChecker requirementsChecker,
		ItemManager itemManager, ItemClickHandler clickHandler, boolean hideObtained)
	{
		this.categoryName = category.getDisplayName();
		this.lazySources = sources;
		this.lazyCollectionState = collectionState;
		this.lazyRequirementsChecker = requirementsChecker;
		this.lazyItemManager = itemManager;
		this.lazyClickHandler = clickHandler;
		this.lazyHideObtained = hideObtained;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		// Header with category name + progress bar
		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		obtained = collectionState.getCategoryCount(category);
		total = collectionState.getCategoryMax(category);

		nameLabel = new JLabel(
			String.format("\u25B6 %s (%d/%d)", category.getDisplayName(), obtained, total));
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(Color.WHITE);
		headerPanel.add(nameLabel, BorderLayout.WEST);

		// Progress bar
		JPanel progressBar = new ProgressBar(obtained, total);
		progressBar.setPreferredSize(new Dimension(80, 12));
		headerPanel.add(progressBar, BorderLayout.EAST);

		add(headerPanel, BorderLayout.NORTH);

		// Expandable items container (rows created lazily on first expand)
		itemsContainer = new JPanel();
		itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
		itemsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		itemsContainer.setVisible(false);

		add(itemsContainer, BorderLayout.CENTER);

		// Click to expand/collapse
		headerPanel.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				expanded = !expanded;
				if (expanded && !populated)
				{
					populateItems();
				}
				itemsContainer.setVisible(expanded);
				nameLabel.setText(String.format("%s %s (%d/%d)",
					expanded ? "\u25BC" : "\u25B6",
					category.getDisplayName(), obtained, total));
				revalidate();
			}

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				headerPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private void populateItems()
	{
		for (CollectionLogSource source : lazySources)
		{
			boolean locked = !lazyRequirementsChecker.isAccessible(source.getName());
			for (CollectionLogItem item : source.getItems())
			{
				boolean itemObtained = lazyCollectionState.isItemObtained(item.getItemId());
				if (lazyHideObtained && itemObtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, source, itemObtained, 0,
					locked, lazyRequirementsChecker.getUnmetRequirements(source.getName()),
					lazyItemManager, () -> lazyClickHandler.onItemClicked(item, source));
				itemsContainer.add(row);
			}
		}
		populated = true;
		// Release references no longer needed
		lazySources = null;
		lazyCollectionState = null;
		lazyRequirementsChecker = null;
		lazyItemManager = null;
		lazyClickHandler = null;
	}

	boolean isExpanded()
	{
		return expanded;
	}

	String getCategoryName()
	{
		return categoryName;
	}

	void setExpanded(boolean expand)
	{
		expanded = expand;
		if (expanded && !populated)
		{
			populateItems();
		}
		itemsContainer.setVisible(expanded);
		nameLabel.setText(String.format("%s %s (%d/%d)",
			expanded ? "\u25BC" : "\u25B6",
			categoryName, obtained, total));
	}

	interface ItemClickHandler
	{
		void onItemClicked(CollectionLogItem item, CollectionLogSource source);
	}

	private static class ProgressBar extends JPanel
	{
		private final int obtained;
		private final int total;

		ProgressBar(int obtained, int total)
		{
			this.obtained = obtained;
			this.total = total;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			int width = getWidth();
			int height = getHeight();

			g.setColor(PROGRESS_BG_COLOR);
			g.fillRect(0, 0, width, height);

			if (total > 0)
			{
				int fillWidth = (int) ((obtained / (double) total) * width);
				g.setColor(PROGRESS_BAR_COLOR);
				g.fillRect(0, 0, fillWidth, height);
			}
		}
	}
}
