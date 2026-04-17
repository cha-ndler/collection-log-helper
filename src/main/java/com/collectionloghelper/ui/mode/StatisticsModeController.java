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
package com.collectionloghelper.ui.mode;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Renders the Statistics mode: overall collection-log progress plus a
 * per-category breakdown with progress bars, source counts, and time
 * estimates. Clicking a category tile navigates to Category Focus mode
 * via the shell context.
 *
 * <p>Pure view builder — holds no long-lived widgets and can be rebuilt
 * on every tick without leaking listeners.
 */
public class StatisticsModeController implements PanelModeController
{
	private final PanelShellContext shell;
	private final PlayerCollectionState collectionState;
	private final DropRateDatabase database;
	private final EfficiencyCalculator calculator;

	public StatisticsModeController(
		PanelShellContext shell,
		PlayerCollectionState collectionState,
		DropRateDatabase database,
		EfficiencyCalculator calculator)
	{
		this.shell = shell;
		this.collectionState = collectionState;
		this.database = database;
		this.calculator = calculator;
	}

	@Override
	public void buildView()
	{
		// Overall summary — use varp-based counts for accurate deduplicated totals
		int totalObtained = collectionState.getTotalObtained();
		int totalItems = collectionState.getTotalPossible();
		double overallPct = collectionState.getCompletionPercentage();

		String overallText = String.format("%d / %d (%.1f%%)", totalObtained, totalItems, overallPct);

		JPanel overallPanel = new JPanel(new BorderLayout(5, 2));
		overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		overallPanel.setToolTipText("Collection Log: " + overallText);

		JLabel overallLabel = new JLabel(overallText);
		overallLabel.setFont(FontManager.getRunescapeBoldFont());
		overallLabel.setForeground(Color.WHITE);
		overallPanel.add(overallLabel, BorderLayout.NORTH);

		JPanel overallBar = createProgressBar(totalObtained, totalItems);
		overallPanel.add(overallBar, BorderLayout.SOUTH);
		shell.addToList(overallPanel);

		shell.addToList(Box.createVerticalStrut(4));

		// Per-category breakdown
		for (CollectionLogCategory category : CollectionLogCategory.values())
		{
			renderCategoryTile(category);
		}
	}

	private void renderCategoryTile(CollectionLogCategory category)
	{
		List<CollectionLogSource> sources = database.getSourcesByCategory(category);
		if (sources == null || sources.isEmpty())
		{
			return;
		}

		int catSources = sources.size();
		int catSourcesComplete = 0;

		// Use varp-based counts for categories the game tracks (deduplicated),
		// fall back to manual counting for SLAYER/SKILLING (no game varps)
		int varpCount = collectionState.getCategoryCount(category);
		int varpMax = collectionState.getCategoryMax(category);
		boolean useVarps = varpMax > 0;

		int catObtained = 0;
		int catTotal = 0;

		for (CollectionLogSource source : sources)
		{
			boolean sourceComplete = true;
			for (CollectionLogItem item : source.getItems())
			{
				if (!useVarps)
				{
					catTotal++;
					if (collectionState.isItemObtained(item.getItemId()))
					{
						catObtained++;
					}
					else
					{
						sourceComplete = false;
					}
				}
				else
				{
					if (!collectionState.isItemObtained(item.getItemId()))
					{
						sourceComplete = false;
					}
				}
			}
			if (sourceComplete)
			{
				catSourcesComplete++;
			}
		}

		if (useVarps)
		{
			catObtained = varpCount;
			catTotal = varpMax;
		}

		double catPct = catTotal > 0 ? 100.0 * catObtained / catTotal : 0;

		String countText = String.format("%d / %d (%.1f%%)", catObtained, catTotal, catPct);

		// Estimate remaining hours from efficiency scores
		double totalHours = 0;
		List<ScoredItem> categoryScored = calculator.filterByCategory(category);
		for (ScoredItem si : categoryScored)
		{
			if (si.getScore() > 0)
			{
				totalHours += 100.0 / si.getScore();
			}
		}

		String timeStr;
		if (totalHours <= 0 || catObtained >= catTotal)
		{
			timeStr = null;
		}
		else if (totalHours < 1)
		{
			timeStr = "~" + Math.max(1, Math.round(totalHours * 60)) + " min left";
		}
		else if (totalHours < 24)
		{
			timeStr = "~" + Math.round(totalHours) + " hrs left";
		}
		else
		{
			long days = Math.round(totalHours / 24);
			timeStr = "~" + days + (days == 1 ? " day left" : " days left");
		}

		JPanel catPanel = new JPanel(new BorderLayout(5, 2));
		catPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		catPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		// Tooltip with full details for truncated text
		String tooltip = String.format("%s: %s | %d sources, %d complete",
			category.getDisplayName(), countText, catSources, catSourcesComplete);
		if (timeStr != null)
		{
			tooltip += " | " + timeStr;
		}
		catPanel.setToolTipText(tooltip);

		// Header: category name (bold) + count (right, small)
		JLabel catNameLabel = new JLabel(category.getDisplayName());
		catNameLabel.setFont(FontManager.getRunescapeBoldFont());
		catNameLabel.setForeground(Color.WHITE);

		JLabel catCountLabel = new JLabel(countText);
		catCountLabel.setFont(FontManager.getRunescapeSmallFont());
		catCountLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		catCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel catHeader = new JPanel(new BorderLayout());
		catHeader.setOpaque(false);
		catHeader.add(catNameLabel, BorderLayout.WEST);
		catHeader.add(catCountLabel, BorderLayout.EAST);
		catPanel.add(catHeader, BorderLayout.NORTH);

		JPanel catBar = createProgressBar(catObtained, catTotal);
		catPanel.add(catBar, BorderLayout.CENTER);

		// Footer: source count (left) + time estimate (right)
		String footerText = catSources + " sources, " + catSourcesComplete + " complete";
		if (timeStr != null)
		{
			footerText += "  |  " + timeStr;
		}
		JLabel catFooterLabel = new JLabel(footerText);
		catFooterLabel.setFont(FontManager.getRunescapeSmallFont());
		catFooterLabel.setForeground(timeStr != null
			? new Color(255, 200, 0) : ColorScheme.LIGHT_GRAY_COLOR);
		catPanel.add(catFooterLabel, BorderLayout.SOUTH);

		// Click to navigate to Category Focus mode
		final CollectionLogCategory clickedCategory = category;
		catPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		catPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				shell.switchToCategoryFocus(clickedCategory);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				catPanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				catPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});

		shell.addToList(catPanel);
		shell.addToList(Box.createVerticalStrut(2));
	}

	private static JPanel createProgressBar(int obtained, int total)
	{
		final double pct = total > 0 ? (double) obtained / total : 0;
		JPanel bar = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				super.paintComponent(g);
				int w = getWidth();
				int h = getHeight();
				g.setColor(new Color(60, 60, 60));
				g.fillRect(0, 0, w, h);
				if (pct > 0)
				{
					g.setColor(new Color(0, 200, 0));
					g.fillRect(0, 0, (int) (w * pct), h);
				}
			}
		};
		bar.setPreferredSize(new Dimension(0, 6));
		return bar;
	}
}
