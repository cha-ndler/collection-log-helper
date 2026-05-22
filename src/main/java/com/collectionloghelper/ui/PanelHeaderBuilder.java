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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.ui.widget.ClueSummaryView;
import com.collectionloghelper.ui.widget.GuidanceBannerView;
import com.collectionloghelper.ui.widget.QuickGuidePanelView;
import com.collectionloghelper.ui.widget.SlayerStrategyView;
import com.collectionloghelper.ui.widget.StepProgressView;
import com.collectionloghelper.ui.widget.SyncStatusView;
import java.awt.Color;
import java.awt.Dimension;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Builds the static header section of {@link CollectionLogHelperPanel}: completion
 * label, progress bar, sync status, sync buttons, clue summary, slayer strategy,
 * guidance banner, and step progress views. Extracted from
 * {@link CollectionLogHelperPanel} as part of the issue #503 god-class split.
 *
 * <p>The host panel still owns mutation of the view widgets via the handles
 * exposed on {@link Result}. The builder only assembles the layout and returns
 * the assembled {@code controlsPanel} plus references to widgets the host needs
 * to update later (completion label/bar, sync views, guidance views).
 *
 * <p>The mode/AFK/sort/category/search selectors are NOT built here — those
 * remain the responsibility of {@link SelectorControlsPanel} which the host
 * adds to the returned {@code controlsPanel} after this builder runs.
 */
final class PanelHeaderBuilder
{
	private PanelHeaderBuilder()
	{
	}

	/**
	 * Assembles the static header widgets and returns the configured controls
	 * panel together with handles to the views the host panel mutates later.
	 *
	 * <p>The returned {@link Result#controlsPanel} is a fresh {@link JPanel} laid
	 * out with a vertical {@link BoxLayout}, populated with all the header
	 * widgets in display order, followed by a trailing vertical strut. Callers
	 * add the selector controls panel and any additional north-region content
	 * after the strut.
	 */
	static Result build(CollectionLogHelperConfig config,
		DataSyncState dataSyncState,
		SlayerTaskState slayerTaskState,
		SlayerStrategyCalculator slayerStrategyCalculator,
		RequirementsChecker requirementsChecker,
		ItemManager itemManager,
		BiConsumer<CollectionLogSource, Integer> guidanceActivator,
		Runnable guidanceDeactivator,
		JComponent syncButtonOwner)
	{
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel completionLabel = new JLabel("Collection Log: 0/0 (0.0%)", SwingConstants.CENTER);
		completionLabel.setFont(FontManager.getRunescapeBoldFont());
		completionLabel.setForeground(Color.WHITE);
		completionLabel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		completionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
		controlsPanel.add(completionLabel);

		JProgressBar completionProgressBar = new JProgressBar(0, 1699);
		completionProgressBar.setValue(0);
		completionProgressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6));
		completionProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		completionProgressBar.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		completionProgressBar.setBorderPainted(false);
		completionProgressBar.setBackground(new Color(40, 40, 40));
		completionProgressBar.setForeground(new Color(80, 200, 80));
		completionProgressBar.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
		controlsPanel.add(completionProgressBar);

		SyncStatusView syncStatusView = new SyncStatusView(dataSyncState);
		syncStatusView.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		syncStatusView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(syncStatusView);

		SyncButtonController syncButtonController = new SyncButtonController(config, syncButtonOwner);
		controlsPanel.add(syncButtonController.getCollectionLogNetButton());
		controlsPanel.add(syncButtonController.getTempleSyncButton());

		ClueSummaryView clueSummaryView = new ClueSummaryView();
		clueSummaryView.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		clueSummaryView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(clueSummaryView);

		SlayerStrategyView slayerStrategyView = new SlayerStrategyView(slayerTaskState, slayerStrategyCalculator);
		slayerStrategyView.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		slayerStrategyView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(slayerStrategyView);

		GuidanceBannerView guidanceBannerView = new GuidanceBannerView(requirementsChecker, itemManager);
		guidanceBannerView.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		guidanceBannerView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(guidanceBannerView);

		StepProgressView stepProgressView = new StepProgressView(itemManager);
		stepProgressView.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		controlsPanel.add(stepProgressView);

		QuickGuidePanelView quickGuidePanelView = new QuickGuidePanelView(guidanceActivator, guidanceDeactivator);

		controlsPanel.add(Box.createVerticalStrut(4));

		return new Result(
			controlsPanel,
			completionLabel,
			completionProgressBar,
			syncStatusView,
			syncButtonController,
			clueSummaryView,
			slayerStrategyView,
			guidanceBannerView,
			stepProgressView,
			quickGuidePanelView);
	}

	/**
	 * View handles returned by {@link #build}. The host panel retains mutation
	 * authority on every widget here; the builder does not hold references
	 * after the call returns.
	 */
	static final class Result
	{
		final JPanel controlsPanel;
		final JLabel completionLabel;
		final JProgressBar completionProgressBar;
		final SyncStatusView syncStatusView;
		final SyncButtonController syncButtonController;
		final ClueSummaryView clueSummaryView;
		final SlayerStrategyView slayerStrategyView;
		final GuidanceBannerView guidanceBannerView;
		final StepProgressView stepProgressView;
		final QuickGuidePanelView quickGuidePanelView;

		Result(JPanel controlsPanel,
			JLabel completionLabel,
			JProgressBar completionProgressBar,
			SyncStatusView syncStatusView,
			SyncButtonController syncButtonController,
			ClueSummaryView clueSummaryView,
			SlayerStrategyView slayerStrategyView,
			GuidanceBannerView guidanceBannerView,
			StepProgressView stepProgressView,
			QuickGuidePanelView quickGuidePanelView)
		{
			this.controlsPanel = controlsPanel;
			this.completionLabel = completionLabel;
			this.completionProgressBar = completionProgressBar;
			this.syncStatusView = syncStatusView;
			this.syncButtonController = syncButtonController;
			this.clueSummaryView = clueSummaryView;
			this.slayerStrategyView = slayerStrategyView;
			this.guidanceBannerView = guidanceBannerView;
			this.stepProgressView = stepProgressView;
			this.quickGuidePanelView = quickGuidePanelView;
		}
	}
}
