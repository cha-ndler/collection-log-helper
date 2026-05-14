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

import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.learning.DrynessClass;
import com.collectionloghelper.learning.DryStreakAnalyzer;
import com.collectionloghelper.learning.DryStreakEntry;
import com.collectionloghelper.learning.DryStreakSortMode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Renders the Dry-streak Feed mode: a ranked list of unobtained items for which
 * the player's kill count significantly exceeds the statistical median.
 *
 * <p>Entries with a dryness ratio &gt; 2x are shown; the worst offenders appear
 * first (sorted by {@code multipleOfExpected} descending by default). A sort
 * toggle in the mode header lets the user switch to alphabetical order by item
 * name or by source name.
 *
 * <p>KC data is injected at construction time so the controller stays decoupled
 * from the KC-sync mechanism (F2/F3). Pass an empty map if no KC data is yet
 * available — the controller will show an appropriate empty-state message.
 */
public class DryStreakFeedModeController implements PanelModeController
{
	/** Maximum number of entries shown in the feed to keep the panel legible. */
	static final int MAX_ENTRIES = 50;

	private static final Color VERY_DRY_COLOR = new Color(220, 50, 50);
	private static final Color DRY_COLOR = new Color(220, 160, 0);
	private static final Color VERY_DRY_BG = new Color(60, 20, 20);
	private static final Color DRY_BG = new Color(50, 40, 10);

	private final PanelShellContext shell;
	private final PlayerCollectionState collectionState;
	private final DryStreakAnalyzer analyzer;

	/**
	 * Live KC data injected from the owning panel/plugin. Keyed by source name.
	 * May be empty when no KC sync has occurred yet.
	 */
	private Map<String, Integer> killCounts = Collections.emptyMap();

	/** Current sort order for the feed. Defaults to dryness ratio descending. */
	private DryStreakSortMode sortMode = DryStreakSortMode.RATIO_DESC;

	public DryStreakFeedModeController(
		PanelShellContext shell,
		PlayerCollectionState collectionState,
		DryStreakAnalyzer analyzer)
	{
		this.shell = shell;
		this.collectionState = collectionState;
		this.analyzer = analyzer;
	}

	/**
	 * Updates the KC data used by this controller. Call this whenever the
	 * underlying KC store is refreshed (e.g. after an in-game KC varp update or
	 * a TempleOSRS sync). The panel will need a subsequent {@code rebuild()} to
	 * reflect the new data.
	 */
	public void setKillCounts(Map<String, Integer> killCounts)
	{
		this.killCounts = killCounts != null ? killCounts : Collections.emptyMap();
	}

	/** Returns the current sort mode. */
	public DryStreakSortMode getSortMode()
	{
		return sortMode;
	}

	/** Sets the sort mode and triggers a view rebuild on the next {@link #buildView()} call. */
	public void setSortMode(DryStreakSortMode sortMode)
	{
		this.sortMode = sortMode != null ? sortMode : DryStreakSortMode.RATIO_DESC;
	}

	@Override
	public void buildView()
	{
		List<DryStreakEntry> feed = analyzer.analyze(collectionState, killCounts);

		if (feed.isEmpty())
		{
			if (killCounts.isEmpty())
			{
				shell.addEmptyStateMessage(
					"No KC data available.<br>Kill counts will appear here once synced.");
			}
			else
			{
				shell.addEmptyStateMessage("No dry streaks detected.");
			}
			return;
		}

		List<DryStreakEntry> sorted = applySortMode(feed, sortMode);
		int shown = Math.min(sorted.size(), MAX_ENTRIES);

		for (int i = 0; i < shown; i++)
		{
			DryStreakEntry entry = sorted.get(i);
			shell.addToList(buildEntryRow(entry));
			shell.addToList(Box.createVerticalStrut(2));
		}

		if (sorted.size() > MAX_ENTRIES)
		{
			int hidden = sorted.size() - MAX_ENTRIES;
			JLabel moreLabel = new JLabel("... and " + hidden + " more dry items");
			moreLabel.setFont(FontManager.getRunescapeSmallFont());
			moreLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			moreLabel.setHorizontalAlignment(SwingConstants.CENTER);
			moreLabel.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
			moreLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
			shell.addToList(moreLabel);
		}
	}

	private JPanel buildEntryRow(DryStreakEntry entry)
	{
		boolean veryDry = entry.getClassification() == DrynessClass.VERY_DRY;
		Color rowBg = veryDry ? VERY_DRY_BG : DRY_BG;
		Color accentColor = veryDry ? VERY_DRY_COLOR : DRY_COLOR;

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(rowBg);
		row.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		// Item name label (left)
		JLabel nameLabel = new JLabel(entry.getItem().getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(Color.WHITE);

		// Source name (secondary, below item name)
		JLabel sourceLabel = new JLabel(entry.getSource().getName());
		sourceLabel.setFont(FontManager.getRunescapeSmallFont());
		sourceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel namePanel = new JPanel(new BorderLayout());
		namePanel.setOpaque(false);
		namePanel.add(nameLabel, BorderLayout.NORTH);
		namePanel.add(sourceLabel, BorderLayout.SOUTH);
		row.add(namePanel, BorderLayout.CENTER);

		// Dryness ratio label (right)
		String ratioText = String.format("%.1fx", entry.getMultipleOfExpected());
		JLabel ratioLabel = new JLabel(ratioText);
		ratioLabel.setFont(FontManager.getRunescapeBoldFont());
		ratioLabel.setForeground(accentColor);
		ratioLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		row.add(ratioLabel, BorderLayout.EAST);

		// Tooltip
		long medianKc = Math.round(
			DryStreakAnalyzer.computeMedianKc(entry.getItem().getDropRate()));
		long dropDenom = entry.getItem().getDropRate() > 0
			? Math.round(1.0 / entry.getItem().getDropRate()) : 0;
		String tier = veryDry ? "VERY DRY" : "DRY";
		row.setToolTipText(String.format(
			"<html>%s — %s<br>Drop rate: 1/%d<br>Median KC: ~%d<br>Dryness: %.1fx (%s)</html>",
			entry.getItem().getName(), entry.getSource().getName(),
			dropDenom, medianKc, entry.getMultipleOfExpected(), tier));

		// Click navigates to item detail
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				shell.showDetail(entry.getItem(), entry.getSource());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(veryDry
					? VERY_DRY_BG.brighter() : DRY_BG.brighter());
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(rowBg);
			}
		});

		return row;
	}

	/**
	 * Sorts a copy of {@code entries} according to {@code mode}.
	 * The input list is not modified.
	 */
	static List<DryStreakEntry> applySortMode(List<DryStreakEntry> entries, DryStreakSortMode mode)
	{
		List<DryStreakEntry> result = new ArrayList<>(entries);
		switch (mode)
		{
			case ITEM_NAME_ASC:
				result.sort(Comparator.comparing(e -> e.getItem().getName()));
				break;
			case SOURCE_NAME_ASC:
				result.sort(Comparator.comparing(e -> e.getSource().getName()));
				break;
			case RATIO_DESC:
			default:
				result.sort(Comparator.comparingDouble(DryStreakEntry::getMultipleOfExpected).reversed());
				break;
		}
		return result;
	}
}
