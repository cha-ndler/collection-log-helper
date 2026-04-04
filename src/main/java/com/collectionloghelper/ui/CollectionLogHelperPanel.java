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

import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.EfficientSortMode;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerCreatureDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.SwingUtil;

public class CollectionLogHelperPanel extends PluginPanel
{
	private static final Color GUIDE_ME_COLOR = new Color(30, 120, 30);
	private static final Color STOP_GUIDANCE_COLOR = new Color(140, 30, 30);
	private static final Color SYNC_NOT_SYNCED_COLOR = new Color(230, 180, 50);
	private static final Color SYNC_SYNCING_COLOR = new Color(0, 200, 200);
	private static final Color SYNC_SYNCED_COLOR = new Color(50, 200, 50);

	public enum Mode
	{
		EFFICIENT("Efficient"),
		CATEGORY_FOCUS("Category Focus"),
		SEARCH("Search"),
		PET_HUNT("Pet Hunt");

		private final String displayName;

		Mode(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	public enum SyncState
	{
		NOT_SYNCED,
		SYNCING,
		SYNCED
	}

	private static final Color DATA_WARNING_COLOR = new Color(220, 50, 50);
	private static final Color CLUE_SUMMARY_COLOR = new Color(200, 170, 50);
	private static final Color SLAYER_TASK_COLOR = new Color(180, 80, 220);

	private final CollectionLogHelperConfig config;
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final ClueCompletionEstimator clueEstimator;
	private final ItemManager itemManager;
	private final RequirementsChecker requirementsChecker;
	private final DataSyncState dataSyncState;
	private final SlayerTaskState slayerTaskState;
	private final SlayerStrategyCalculator slayerStrategyCalculator;
	private final PlayerInventoryState inventoryState;
	private final PlayerBankState bankState;
	private final Consumer<CollectionLogSource> guidanceActivator;
	private final Runnable guidanceDeactivator;
	private final Consumer<AfkFilter> afkFilterUpdater;

	private final JLabel syncStatusLabel;
	private final JLabel dataSyncWarningLabel;
	private final JLabel clueSummaryLabel;
	private final JLabel slayerTaskLabel;

	private final JComboBox<Mode> modeSelector;
	private final JComboBox<AfkFilter> afkFilterSelector;
	private final JComboBox<EfficientSortMode> sortSelector;
	private final JComboBox<CollectionLogCategory> categorySelector;
	private final JTextField searchField;
	private final JLabel completionLabel;
	private final JProgressBar completionProgressBar;
	private final JPanel listContainer;
	private final CardLayout cardLayout;
	private final JPanel contentPanel;
	private final JPanel listView;
	private final JPanel detailView;

	private final JPanel clueGuidanceBanner;
	private final JLabel clueGuidanceLabel;
	private final JPanel slayerStrategyPanel;
	private final JLabel slayerStrategyLabel;
	private boolean slayerStrategyExpanded = false;

	private final JLabel guidanceBannerLabel;
	private final JLabel requirementsWarningLabel;
	private final JPanel guidanceBannerPanel;

	private final JPanel stepProgressPanel;
	private final JLabel stepProgressLabel;
	private final JPanel requiredItemsPanel;
	private final JButton nextStepButton;
	private final JButton skipStepButton;

	private Runnable stepAdvancer;
	private Runnable stepSkipper;

	private final Timer searchDebounceTimer;

	private Mode currentMode = Mode.EFFICIENT;
	private boolean rebuilding = false;
	private boolean rebuildPending = false;
	private boolean guidanceActive = false;
	private CollectionLogSource guidedSource = null;
	private JButton topPickGuideButton = null;
	private CollectionLogSource topPickSource = null;
	private boolean inDetailView = false;

	public CollectionLogHelperPanel(CollectionLogHelperConfig config,
		DropRateDatabase database, PlayerCollectionState collectionState,
		EfficiencyCalculator calculator, ClueCompletionEstimator clueEstimator,
		ItemManager itemManager,
		RequirementsChecker requirementsChecker, DataSyncState dataSyncState,
		SlayerTaskState slayerTaskState,
		SlayerStrategyCalculator slayerStrategyCalculator,
		PlayerInventoryState inventoryState,
		PlayerBankState bankState,
		Consumer<CollectionLogSource> guidanceActivator, Runnable guidanceDeactivator,
		Consumer<AfkFilter> afkFilterUpdater)
	{
		this.config = config;
		this.database = database;
		this.collectionState = collectionState;
		this.calculator = calculator;
		this.clueEstimator = clueEstimator;
		this.itemManager = itemManager;
		this.requirementsChecker = requirementsChecker;
		this.dataSyncState = dataSyncState;
		this.slayerTaskState = slayerTaskState;
		this.slayerStrategyCalculator = slayerStrategyCalculator;
		this.inventoryState = inventoryState;
		this.bankState = bankState;
		this.guidanceActivator = guidanceActivator;
		this.guidanceDeactivator = guidanceDeactivator;
		this.afkFilterUpdater = afkFilterUpdater;

		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		// Top controls panel
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Completion header
		completionLabel = new JLabel("Collection Log: 0/0 (0.0%)", SwingConstants.CENTER);
		completionLabel.setFont(FontManager.getRunescapeBoldFont());
		completionLabel.setForeground(Color.WHITE);
		completionLabel.setAlignmentX(CENTER_ALIGNMENT);
		completionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
		controlsPanel.add(completionLabel);

		// Progress bar
		completionProgressBar = new JProgressBar(0, 1699);
		completionProgressBar.setValue(0);
		completionProgressBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 6));
		completionProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
		completionProgressBar.setAlignmentX(CENTER_ALIGNMENT);
		completionProgressBar.setBorderPainted(false);
		completionProgressBar.setBackground(new Color(40, 40, 40));
		completionProgressBar.setForeground(new Color(80, 200, 80));
		completionProgressBar.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));
		controlsPanel.add(completionProgressBar);

		// Sync status label
		syncStatusLabel = new JLabel("Open Collection Log to sync", SwingConstants.CENTER);
		syncStatusLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.ITALIC));
		syncStatusLabel.setForeground(SYNC_NOT_SYNCED_COLOR);
		syncStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
		syncStatusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		syncStatusLabel.setToolTipText("Syncs automatically when you open the Collection Log in-game");
		controlsPanel.add(syncStatusLabel);

		// Data sync warning banner (hidden when all data sources are synced)
		dataSyncWarningLabel = new JLabel("", SwingConstants.CENTER);
		dataSyncWarningLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		dataSyncWarningLabel.setForeground(DATA_WARNING_COLOR);
		dataSyncWarningLabel.setAlignmentX(CENTER_ALIGNMENT);
		dataSyncWarningLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		dataSyncWarningLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		dataSyncWarningLabel.setVisible(false);
		controlsPanel.add(dataSyncWarningLabel);

		// Clue summary label (shows unopened caskets/containers from bank scan)
		clueSummaryLabel = new JLabel("", SwingConstants.CENTER);
		clueSummaryLabel.setFont(FontManager.getRunescapeSmallFont());
		clueSummaryLabel.setForeground(CLUE_SUMMARY_COLOR);
		clueSummaryLabel.setAlignmentX(CENTER_ALIGNMENT);
		clueSummaryLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		clueSummaryLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		clueSummaryLabel.setVisible(false);
		controlsPanel.add(clueSummaryLabel);

		// Slayer task indicator
		slayerTaskLabel = new JLabel("", SwingConstants.CENTER);
		slayerTaskLabel.setFont(FontManager.getRunescapeSmallFont());
		slayerTaskLabel.setForeground(SLAYER_TASK_COLOR);
		slayerTaskLabel.setAlignmentX(CENTER_ALIGNMENT);
		slayerTaskLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		slayerTaskLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		slayerTaskLabel.setVisible(false);
		controlsPanel.add(slayerTaskLabel);

		// Slayer strategy advisor panel (expandable, below task indicator)
		slayerStrategyPanel = new JPanel();
		slayerStrategyPanel.setLayout(new BoxLayout(slayerStrategyPanel, BoxLayout.Y_AXIS));
		slayerStrategyPanel.setBackground(new Color(35, 25, 50));
		slayerStrategyPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(140, 60, 180), 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		slayerStrategyPanel.setAlignmentX(CENTER_ALIGNMENT);
		slayerStrategyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		slayerStrategyPanel.setVisible(false);

		JButton strategyToggle = new JButton("\u25B6 Slayer Strategy");
		strategyToggle.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		strategyToggle.setForeground(new Color(180, 80, 220));
		strategyToggle.setBackground(new Color(35, 25, 50));
		strategyToggle.setBorderPainted(false);
		strategyToggle.setFocusPainted(false);
		strategyToggle.setContentAreaFilled(false);
		strategyToggle.setAlignmentX(LEFT_ALIGNMENT);
		strategyToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		strategyToggle.addActionListener(e ->
		{
			slayerStrategyExpanded = !slayerStrategyExpanded;
			strategyToggle.setText((slayerStrategyExpanded ? "\u25BC " : "\u25B6 ") + "Slayer Strategy");
			updateSlayerStrategy();
		});
		slayerStrategyPanel.add(strategyToggle);

		slayerStrategyLabel = new JLabel();
		slayerStrategyLabel.setFont(FontManager.getRunescapeSmallFont());
		slayerStrategyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		slayerStrategyLabel.setAlignmentX(LEFT_ALIGNMENT);
		slayerStrategyLabel.setVisible(false);
		slayerStrategyPanel.add(slayerStrategyLabel);

		controlsPanel.add(slayerStrategyPanel);

		// Active guidance banner (shows which source is being guided)
		guidanceBannerPanel = new JPanel();
		guidanceBannerPanel.setLayout(new BoxLayout(guidanceBannerPanel, BoxLayout.Y_AXIS));
		guidanceBannerPanel.setBackground(new Color(25, 50, 25));
		guidanceBannerPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 200, 80), 1),
			BorderFactory.createEmptyBorder(3, 6, 3, 6)
		));
		guidanceBannerPanel.setAlignmentX(CENTER_ALIGNMENT);
		guidanceBannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
		guidanceBannerPanel.setVisible(false);
		guidanceBannerLabel = new JLabel();
		guidanceBannerLabel.setFont(FontManager.getRunescapeSmallFont());
		guidanceBannerLabel.setForeground(new Color(80, 200, 80));
		guidanceBannerLabel.setAlignmentX(LEFT_ALIGNMENT);
		guidanceBannerPanel.add(guidanceBannerLabel);
		requirementsWarningLabel = new JLabel();
		requirementsWarningLabel.setFont(FontManager.getRunescapeSmallFont());
		requirementsWarningLabel.setForeground(new Color(255, 170, 0));
		requirementsWarningLabel.setAlignmentX(LEFT_ALIGNMENT);
		requirementsWarningLabel.setVisible(false);
		guidanceBannerPanel.add(requirementsWarningLabel);
		controlsPanel.add(guidanceBannerPanel);

		// Step progress panel (for multi-step guidance sequences)
		stepProgressPanel = new JPanel();
		stepProgressPanel.setLayout(new BoxLayout(stepProgressPanel, BoxLayout.Y_AXIS));
		stepProgressPanel.setBackground(new Color(25, 35, 55));
		stepProgressPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 150, 220), 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		stepProgressPanel.setAlignmentX(CENTER_ALIGNMENT);
		stepProgressPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		stepProgressPanel.setVisible(false);

		stepProgressLabel = new JLabel();
		stepProgressLabel.setFont(FontManager.getRunescapeSmallFont());
		stepProgressLabel.setForeground(new Color(80, 180, 255));
		stepProgressLabel.setAlignmentX(LEFT_ALIGNMENT);
		stepProgressPanel.add(stepProgressLabel);

		requiredItemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		requiredItemsPanel.setBackground(new Color(25, 35, 55));
		requiredItemsPanel.setAlignmentX(LEFT_ALIGNMENT);
		requiredItemsPanel.setVisible(false);
		stepProgressPanel.add(requiredItemsPanel);

		JPanel stepButtonRow = new JPanel();
		stepButtonRow.setLayout(new BoxLayout(stepButtonRow, BoxLayout.X_AXIS));
		stepButtonRow.setBackground(new Color(25, 35, 55));
		stepButtonRow.setAlignmentX(LEFT_ALIGNMENT);

		nextStepButton = new JButton("Next Step");
		nextStepButton.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		nextStepButton.setBackground(new Color(30, 100, 30));
		nextStepButton.setForeground(Color.WHITE);
		nextStepButton.setVisible(false);
		nextStepButton.addActionListener(e ->
		{
			if (stepAdvancer != null)
			{
				stepAdvancer.run();
			}
		});
		stepButtonRow.add(nextStepButton);

		stepButtonRow.add(Box.createHorizontalStrut(4));

		skipStepButton = new JButton("Skip");
		skipStepButton.setFont(FontManager.getRunescapeSmallFont());
		skipStepButton.setBackground(new Color(80, 80, 80));
		skipStepButton.setForeground(Color.WHITE);
		skipStepButton.addActionListener(e ->
		{
			if (stepSkipper != null)
			{
				stepSkipper.run();
			}
		});
		stepButtonRow.add(skipStepButton);

		stepProgressPanel.add(Box.createVerticalStrut(3));
		stepProgressPanel.add(stepButtonRow);
		controlsPanel.add(stepProgressPanel);

		controlsPanel.add(Box.createVerticalStrut(4));

		// Mode selector
		modeSelector = new JComboBox<>(Mode.values());
		modeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		modeSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				currentMode = (Mode) e.getItem();
				updateControlVisibility();
				inDetailView = false;
				rebuild();
				resetScrollPosition();
			}
		});
		controlsPanel.add(modeSelector);

		// AFK filter selector (visible in Efficient and Pet Hunt modes)
		afkFilterSelector = new JComboBox<>(AfkFilter.values());
		afkFilterSelector.setSelectedItem(config.afkFilter());
		afkFilterSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		afkFilterSelector.setVisible(currentMode == Mode.EFFICIENT || currentMode == Mode.PET_HUNT);
		afkFilterSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				AfkFilter selected = (AfkFilter) e.getItem();
				afkFilterUpdater.accept(selected);
				rebuild();
			}
		});
		controlsPanel.add(afkFilterSelector);

		// Sort selector (visible in Efficient mode)
		sortSelector = new JComboBox<>(EfficientSortMode.values());
		sortSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		sortSelector.setVisible(currentMode == Mode.EFFICIENT);
		sortSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				rebuild();
			}
		});
		controlsPanel.add(sortSelector);

		// Category selector (visible in Category Focus mode)
		categorySelector = new JComboBox<>(CollectionLogCategory.values());
		categorySelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		categorySelector.setVisible(false);
		categorySelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				rebuild();
			}
		});
		controlsPanel.add(categorySelector);

		// Search field (visible in Search mode)
		searchField = new JTextField();
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		searchField.setVisible(false);
		searchField.setToolTipText("Search by item name or source name");
		searchDebounceTimer = new Timer(200, e -> rebuild());
		searchDebounceTimer.setRepeats(false);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}
		});
		controlsPanel.add(searchField);

		// Clue guidance banner (hidden by default)
		clueGuidanceBanner = new JPanel(new BorderLayout());
		clueGuidanceBanner.setBackground(new Color(40, 40, 60));
		clueGuidanceBanner.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(100, 100, 200), 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)
		));
		clueGuidanceLabel = new JLabel();
		clueGuidanceLabel.setForeground(Color.WHITE);
		clueGuidanceLabel.setFont(FontManager.getRunescapeSmallFont());
		clueGuidanceBanner.add(clueGuidanceLabel, BorderLayout.CENTER);
		clueGuidanceBanner.setVisible(false);
		controlsPanel.add(clueGuidanceBanner);

		add(controlsPanel, BorderLayout.NORTH);

		// CardLayout with preferred size based on visible card only (fixes scrollbar)
		cardLayout = new CardLayout();
		contentPanel = new JPanel(cardLayout)
		{
			@Override
			public Dimension getPreferredSize()
			{
				for (Component comp : getComponents())
				{
					if (comp.isVisible())
					{
						Insets insets = getInsets();
						Dimension d = comp.getPreferredSize();
						return new Dimension(
							d.width + insets.left + insets.right,
							d.height + insets.top + insets.bottom);
					}
				}
				return super.getPreferredSize();
			}
		};
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// List view
		listView = new JPanel(new BorderLayout());
		listView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		listContainer = new JPanel();
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
		listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listView.add(listContainer, BorderLayout.NORTH);

		// Detail view
		detailView = new JPanel(new BorderLayout());
		detailView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		contentPanel.add(listView, "list");
		contentPanel.add(detailView, "detail");

		add(contentPanel, BorderLayout.CENTER);

		updateControlVisibility();
	}

	public Mode getCurrentMode()
	{
		return currentMode;
	}

	public void setMode(Mode mode)
	{
		currentMode = mode;
		modeSelector.setSelectedItem(mode);
		updateControlVisibility();
		rebuild();
	}

	public void updateCompletionHeader()
	{
		int obtained = collectionState.getTotalObtained();
		int total = collectionState.getTotalPossible();
		double pct = collectionState.getCompletionPercentage();
		completionLabel.setText(String.format("Collection Log: %d/%d (%.1f%%)", obtained, total, pct));
		completionProgressBar.setMaximum(Math.max(total, 1));
		completionProgressBar.setValue(obtained);
	}

	private void updateSlayerTaskLabel()
	{
		if (slayerTaskState.isTaskActive())
		{
			slayerTaskLabel.setText("Slayer: " + slayerTaskState.getCreatureName()
				+ " (" + slayerTaskState.getRemaining() + " remaining)");
			slayerTaskLabel.setVisible(true);
			slayerStrategyPanel.setVisible(true);
		}
		else
		{
			slayerTaskLabel.setVisible(false);
			slayerStrategyPanel.setVisible(false);
		}
		updateSlayerStrategy();
	}

	private void updateSlayerStrategy()
	{
		if (!slayerStrategyPanel.isVisible())
		{
			slayerStrategyLabel.setVisible(false);
			slayerStrategyPanel.revalidate();
			return;
		}

		String recommended = slayerStrategyCalculator.getRecommendedMaster();

		if (!slayerStrategyExpanded)
		{
			// Show a one-line summary when collapsed so users know content exists
			if (recommended != null)
			{
				slayerStrategyLabel.setText("<html><font color='#b5b5b3'>Best: " + recommended + "</font></html>");
				slayerStrategyLabel.setVisible(true);
			}
			else
			{
				slayerStrategyLabel.setVisible(false);
			}
			slayerStrategyPanel.revalidate();
			return;
		}

		StringBuilder sb = new StringBuilder("<html>");

		// Current task assessment
		if (slayerTaskState.isTaskActive())
		{
			String creature = slayerTaskState.getCreatureName();
			List<String> usefulSources = slayerStrategyCalculator.getUsefulSourcesForCreature(creature);
			int missingItems = slayerStrategyCalculator.getMissingItemsForCreature(creature);
			if (!usefulSources.isEmpty())
			{
				sb.append("<b>Current task:</b> ");
				sb.append(String.join(", ", usefulSources));
				sb.append(" (").append(missingItems).append(" missing)");
			}
			else
			{
				List<String> allSources = SlayerCreatureDatabase.getSourcesForCreature(creature);
				if (!allSources.isEmpty())
				{
					sb.append("<b>Current task:</b> All items obtained");
				}
				else
				{
					sb.append("<b>Current task:</b> No boss variants");
				}
			}
			sb.append("<br>");
		}

		// Recommended master
		if (recommended != null)
		{
			sb.append("<b>Best master:</b> ").append(recommended).append("<br>");
		}

		// Recommended blocks
		if (recommended != null)
		{
			List<String> blocks = slayerStrategyCalculator.getRecommendedBlockList(recommended);
			if (!blocks.isEmpty())
			{
				sb.append("<b>Block:</b> ").append(String.join(", ", blocks));
			}
		}

		sb.append("</html>");
		slayerStrategyLabel.setText(sb.toString());
		slayerStrategyLabel.setVisible(true);
		slayerStrategyPanel.revalidate();
	}

	public void updateSyncStatus(SyncState state, int itemCount)
	{
		SwingUtilities.invokeLater(() ->
		{
			Font smallFont = FontManager.getRunescapeSmallFont();
			switch (state)
			{
				case NOT_SYNCED:
					syncStatusLabel.setText("Open Collection Log to sync");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.ITALIC));
					syncStatusLabel.setForeground(SYNC_NOT_SYNCED_COLOR);
					break;
				case SYNCING:
					syncStatusLabel.setText("Syncing...");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.ITALIC));
					syncStatusLabel.setForeground(SYNC_SYNCING_COLOR);
					break;
				case SYNCED:
					syncStatusLabel.setText("Synced (" + itemCount + " items)");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.PLAIN));
					syncStatusLabel.setForeground(SYNC_SYNCED_COLOR);
					break;
			}
		});
	}

	public void updateDataSyncWarning()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (dataSyncState.isFullySynced())
			{
				dataSyncWarningLabel.setVisible(false);
			}
			else
			{
				StringBuilder text = new StringBuilder("<html><center>");
				if (!dataSyncState.isCollectionLogSynced() && !dataSyncState.isBankScanned())
				{
					text.append("Open Collection Log & Bank<br>for accurate guidance");
				}
				else if (!dataSyncState.isCollectionLogSynced())
				{
					text.append("Open Collection Log<br>for accurate guidance");
				}
				else
				{
					text.append("Open Bank to scan items<br>for accurate guidance");
				}
				text.append("</center></html>");
				dataSyncWarningLabel.setText(text.toString());
				dataSyncWarningLabel.setVisible(true);
			}
			revalidate();
		});
	}

	public void updateClueSummary(PlayerBankState bankState)
	{
		SwingUtilities.invokeLater(() ->
		{
			String casketSummary = bankState.getCasketSummary();
			String containerSummary = bankState.getContainerSummary();

			if (casketSummary == null && containerSummary == null)
			{
				clueSummaryLabel.setVisible(false);
			}
			else
			{
				StringBuilder text = new StringBuilder("<html><center>");
				if (casketSummary != null)
				{
					text.append(casketSummary);
				}
				if (containerSummary != null)
				{
					if (casketSummary != null)
					{
						text.append("<br>");
					}
					text.append(containerSummary);
				}
				text.append("</center></html>");
				clueSummaryLabel.setText(text.toString());
				clueSummaryLabel.setVisible(true);
			}
			revalidate();
		});
	}

	public void shutDown()
	{
		searchDebounceTimer.stop();
	}

	public void rebuild()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (rebuilding)
			{
				rebuildPending = true;
				return;
			}

			rebuilding = true;
			try
			{
				// Save scroll position before rebuild
				int savedScrollPosition = 0;
				JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
					JScrollPane.class, this);
				if (scrollPane != null)
				{
					savedScrollPosition = scrollPane.getVerticalScrollBar().getValue();
				}

				// Save expanded category names before clearing
				java.util.Set<String> expandedCategories = new java.util.HashSet<>();
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

				SwingUtil.fastRemoveAll(listContainer);
				updateCompletionHeader();
				updateSlayerTaskLabel();

				switch (currentMode)
				{
					case EFFICIENT:
						buildEfficientView();
						break;
					case CATEGORY_FOCUS:
						buildCategoryView();
						break;
					case SEARCH:
						buildSearchView();
						break;
					case PET_HUNT:
						buildPetHuntView();
						break;
				}

				// Restore expanded category state
				for (Component comp : listContainer.getComponents())
				{
					if (comp instanceof CategorySummaryPanel)
					{
						CategorySummaryPanel csp = (CategorySummaryPanel) comp;
						if (expandedCategories.contains(csp.getCategoryName()))
						{
							csp.setExpanded(true);
						}
					}
				}

				listContainer.revalidate();
				listContainer.repaint();
				if (!inDetailView)
				{
					showListView();

					// Restore scroll position after rebuild
					final int restorePosition = savedScrollPosition;
					if (scrollPane != null && restorePosition > 0)
					{
						SwingUtilities.invokeLater(() ->
							scrollPane.getVerticalScrollBar().setValue(restorePosition));
					}
				}
			}
			finally
			{
				rebuilding = false;
				if (rebuildPending)
				{
					rebuildPending = false;
					rebuild();
				}
			}
		});
	}

	private void showListView()
	{
		inDetailView = false;
		cardLayout.show(contentPanel, "list");
		refreshScrollPane();
	}

	private void showDetailView()
	{
		inDetailView = true;
		cardLayout.show(contentPanel, "detail");
		refreshScrollPane();
		resetScrollPosition();
	}

	private void refreshScrollPane()
	{
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
			JScrollPane.class, this);
		if (scrollPane != null)
		{
			scrollPane.validate();
		}
	}

	private void resetScrollPosition()
	{
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
			JScrollPane.class, this);
		if (scrollPane != null)
		{
			scrollPane.getVerticalScrollBar().setValue(0);
		}
	}

	private void buildEfficientView()
	{
		List<ScoredItem> scored = calculator.rankByEfficiency();
		EfficientSortMode sortMode = (EfficientSortMode) sortSelector.getSelectedItem();
		if (sortMode != null && sortMode != EfficientSortMode.EFFICIENCY)
		{
			scored = new ArrayList<>(scored);
			switch (sortMode)
			{
				case KILL_TIME:
					scored.sort(Comparator.comparingDouble(s -> s.getSource().getKillTimeSeconds()));
					break;
				case DROP_RATE:
					scored.sort(Comparator.comparingDouble(ScoredItem::getDropOnlyScore).reversed());
					break;
				case ALPHABETICAL:
					scored.sort(Comparator.comparing(s -> s.getSource().getName()));
					break;
				case ITEMS_REMAINING:
					scored.sort(Comparator.comparingInt(ScoredItem::getMissingItemCount).reversed());
					break;
			}
		}
		boolean hideObtained = config.hideObtainedItems();

		if (scored.isEmpty())
		{
			if (config.afkFilter().getMinAfkLevel() > 0)
			{
				addEmptyStateMessage("No sources match the current AFK filter.<br>Try a lower Efficient AFK setting.");
			}
			else if (config.hideLockedContent())
			{
				addEmptyStateMessage("All sources are locked or completed.<br>Adjust filters to see more.");
			}
			else
			{
				addEmptyStateMessage("All items obtained. Congratulations!");
			}
			return;
		}

		// Top Pick should always be an accessible (unlocked) source
		scored.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick -> listContainer.add(createQuickGuidePanel(topPick)));

		for (ScoredItem si : scored)
		{
			boolean onTask = calculator.isOnSlayerTask(si.getSource());

			// Show the best (fastest-to-obtain) item per source, but use
			// the source-level score for the time display so the displayed
			// time matches the sort order.
			CollectionLogItem bestItem = si.getBestItem();
			if (bestItem != null && !collectionState.isItemObtained(bestItem.getItemId()))
			{
				ItemRowPanel row = new ItemRowPanel(bestItem, si.getSource(), false,
					si.getScore(), si.isLocked(), onTask, itemManager,
					() -> showDetail(bestItem, si.getSource()));
				listContainer.add(row);
			}
			else
			{
				// Fallback: show first missing item if bestItem is null or already obtained
				for (CollectionLogItem item : si.getSource().getItems())
				{
					if (!collectionState.isItemObtained(item.getItemId()))
					{
						ItemRowPanel row = new ItemRowPanel(item, si.getSource(), false,
							si.getScore(), si.isLocked(), onTask, itemManager,
							() -> showDetail(item, si.getSource()));
						listContainer.add(row);
						break;
					}
				}
			}
		}
	}

	private void buildCategoryView()
	{
		CollectionLogCategory category = (CollectionLogCategory) categorySelector.getSelectedItem();
		if (category == null)
		{
			return;
		}

		// Top Pick for this category — always an accessible source
		List<ScoredItem> categoryScored = calculator.filterByCategory(category);
		categoryScored.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick -> listContainer.add(createQuickGuidePanel(topPick)));

		List<CollectionLogSource> sources = database.getSourcesByCategory(category);
		CategorySummaryPanel summary = new CategorySummaryPanel(
			category, sources, collectionState, requirementsChecker, itemManager,
			this::showDetail, config.hideObtainedItems());
		listContainer.add(summary);
	}

	private void buildSearchView()
	{
		String query = searchField.getText().toLowerCase().trim();
		if (query.isEmpty())
		{
			addEmptyStateMessage("Search by item or source name");
			return;
		}

		boolean hideObtained = config.hideObtainedItems();
		boolean hideLocked = config.hideLockedContent();
		int resultCount = 0;

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			boolean sourceNameMatches = source.getName().toLowerCase().contains(query);
			boolean onTask = calculator.isOnSlayerTask(source);
			for (CollectionLogItem item : source.getItems())
			{
				if (sourceNameMatches || item.getName().toLowerCase().contains(query))
				{
					boolean obtained = collectionState.isItemObtained(item.getItemId());
					if (hideObtained && obtained)
					{
						continue;
					}
					ItemRowPanel row = new ItemRowPanel(item, source, obtained, 0,
						locked, onTask, itemManager, () -> showDetail(item, source));
					listContainer.add(row);
					resultCount++;
				}
			}
		}

		if (resultCount == 0)
		{
			// Escape query to prevent HTML injection in the JLabel
			String safeQuery = query.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			addEmptyStateMessage("No items match '" + safeQuery + "'");
		}
	}

	private void buildPetHuntView()
	{
		List<ScoredItem> scored = calculator.filterPetsOnly();
		boolean hideObtained = config.hideObtainedItems();
		int resultCount = 0;

		for (ScoredItem si : scored)
		{
			boolean onTask = calculator.isOnSlayerTask(si.getSource());
			for (CollectionLogItem item : si.getSource().getItems())
			{
				if (!item.isPet())
				{
					continue;
				}
				boolean obtained = collectionState.isItemObtained(item.getItemId());
				if (hideObtained && obtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, si.getSource(), obtained,
					si.getScore(), si.isLocked(), onTask, itemManager,
					() -> showDetail(item, si.getSource()));
				listContainer.add(row);
				resultCount++;
			}
		}

		if (resultCount == 0)
		{
			if (config.afkFilter().getMinAfkLevel() > 0)
			{
				addEmptyStateMessage("No pets match the current AFK filter.<br>Try a lower Efficient AFK setting.");
			}
			else
			{
				addEmptyStateMessage("All pets obtained!");
			}
		}
	}

	private void showDetail(CollectionLogItem item, CollectionLogSource source)
	{
		boolean obtained = collectionState.isItemObtained(item.getItemId());
		boolean locked = !requirementsChecker.isAccessible(source.getName());
		boolean isGuidingThis = guidanceActive && guidedSource != null
			&& guidedSource.getName().equals(source.getName());

		int sourceTotal = source.getItems().size();
		int sourceObtained = 0;
		for (CollectionLogItem si : source.getItems())
		{
			if (collectionState.isItemObtained(si.getItemId()))
			{
				sourceObtained++;
			}
		}

		detailView.removeAll();
		ItemDetailPanel detail = new ItemDetailPanel(
			item, source, obtained, locked,
			requirementsChecker.getUnmetRequirements(source.getName()),
			sourceObtained, sourceTotal,
			itemManager, clueEstimator,
			requirementsChecker.hasFairyRingAccess(),
			this::showListView,
			() -> guidanceActivator.accept(source),
			() -> guidanceDeactivator.run(),
			isGuidingThis
		);
		detailView.add(detail, BorderLayout.NORTH);
		showDetailView();
	}

	private JPanel createQuickGuidePanel(ScoredItem topItem)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(30, 50, 30));
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 200, 80), 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)
		));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JLabel titleLabel = new JLabel("<html><b>Top Pick: " + topItem.getSource().getName() + "</b></html>");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(new Color(255, 200, 0));
		titleLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(titleLabel);

		String reasoning = topItem.getReasoning();
		if (reasoning != null && !reasoning.isEmpty())
		{
			JLabel reasonLabel = new JLabel("<html>" + reasoning + "</html>");
			reasonLabel.setFont(FontManager.getRunescapeSmallFont());
			reasonLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			reasonLabel.setAlignmentX(LEFT_ALIGNMENT);
			panel.add(reasonLabel);
		}

		panel.add(Box.createRigidArea(new Dimension(0, 4)));

		boolean isGuidingThis = guidanceActive && guidedSource != null
			&& guidedSource.getName().equals(topItem.getSource().getName());

		JButton guideButton = new JButton(isGuidingThis ? "Stop Guidance" : "Guide Me");
		guideButton.setBackground(isGuidingThis ? STOP_GUIDANCE_COLOR : GUIDE_ME_COLOR);
		guideButton.setForeground(Color.WHITE);
		guideButton.setAlignmentX(LEFT_ALIGNMENT);
		guideButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		guideButton.addActionListener(e ->
		{
			if (guideButton.getText().equals("Stop Guidance"))
			{
				guidanceDeactivator.run();
				setGuidanceState(false, null);
				guideButton.setText("Guide Me");
				guideButton.setBackground(GUIDE_ME_COLOR);
			}
			else
			{
				guidanceActivator.accept(topItem.getSource());
				setGuidanceState(true, topItem.getSource());
				guideButton.setText("Stop Guidance");
				guideButton.setBackground(STOP_GUIDANCE_COLOR);
			}
		});
		panel.add(guideButton);

		topPickGuideButton = guideButton;
		topPickSource = topItem.getSource();

		return panel;
	}

	public void setGuidanceState(boolean active, CollectionLogSource source)
	{
		guidanceActive = active;
		guidedSource = source;
		SwingUtilities.invokeLater(() ->
		{
			if (active && source != null)
			{
				guidanceBannerLabel.setText("Guiding: " + source.getName());
				guidanceBannerPanel.setVisible(true);

				// Show unmet requirements warning if applicable
				java.util.List<String> unmet = requirementsChecker.getUnmetRequirements(source.getName());
				if (!unmet.isEmpty())
				{
					String warningText = "\u26A0 Requires: " + String.join(", ", unmet);
					requirementsWarningLabel.setText("<html>" + warningText + "</html>");
					requirementsWarningLabel.setVisible(true);
				}
				else
				{
					requirementsWarningLabel.setVisible(false);
				}
			}
			else
			{
				guidanceBannerPanel.setVisible(false);
				requirementsWarningLabel.setVisible(false);
			}
			guidanceBannerPanel.revalidate();
			if (guidanceBannerPanel.getParent() != null)
			{
				guidanceBannerPanel.getParent().revalidate();
			}

			// Sync top pick button with current guidance state
			if (topPickGuideButton != null && topPickSource != null)
			{
				boolean isGuidingTopPick = active && source != null
					&& source.getName().equals(topPickSource.getName());
				topPickGuideButton.setText(isGuidingTopPick ? "Stop Guidance" : "Guide Me");
				topPickGuideButton.setBackground(isGuidingTopPick ? STOP_GUIDANCE_COLOR : GUIDE_ME_COLOR);
			}
		});
	}

	public void showClueGuidance(CollectionLogSource source)
	{
		SwingUtilities.invokeLater(() ->
		{
			clueGuidanceLabel.setText(
				"<html><b>Guidance: " + source.getName() + "</b><br>"
				+ "Use the RuneLite <b>Clue Scroll</b> plugin for step-by-step guidance</html>");
			clueGuidanceBanner.setVisible(true);
			clueGuidanceBanner.revalidate();
			clueGuidanceBanner.getParent().revalidate();
		});
	}

	/**
	 * Updates the step progress banner with current step info.
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<Integer> requiredItemIds)
	{
		SwingUtilities.invokeLater(() ->
		{
			stepProgressLabel.setText(
				"<html>Step " + current + "/" + total + ": " + description + "</html>");
			nextStepButton.setVisible(isManual);
			updateRequiredItemDisplay(requiredItemIds);
			stepProgressPanel.setVisible(true);
			stepProgressPanel.revalidate();
			if (stepProgressPanel.getParent() != null)
			{
				stepProgressPanel.getParent().revalidate();
			}
		});
	}

	private static final Color ITEM_STATUS_GREEN = new Color(40, 180, 40);
	private static final Color ITEM_STATUS_YELLOW = new Color(200, 180, 40);
	private static final Color ITEM_STATUS_RED = new Color(200, 40, 40);

	/**
	 * Updates the required items display with item sprites and colored status borders.
	 * Green border = item is in inventory or equipped (ready to use).
	 * Yellow border = item is in the bank but not on the player.
	 * Red border = item is not found anywhere (not in inventory, equipment, or bank).
	 */
	private void updateRequiredItemDisplay(List<Integer> requiredItemIds)
	{
		requiredItemsPanel.removeAll();

		if (requiredItemIds == null || requiredItemIds.isEmpty())
		{
			requiredItemsPanel.setVisible(false);
			return;
		}

		JLabel headerLabel = new JLabel("Required:");
		headerLabel.setFont(FontManager.getRunescapeSmallFont());
		headerLabel.setForeground(new Color(180, 180, 180));
		requiredItemsPanel.add(headerLabel);

		for (int itemId : requiredItemIds)
		{
			boolean inInventory = inventoryState.hasItem(itemId);
			boolean equipped = inventoryState.hasEquippedItem(itemId);
			boolean inBank = bankState.hasItem(itemId);

			Color borderColor;
			String statusText;
			if (inInventory || equipped)
			{
				borderColor = ITEM_STATUS_GREEN;
				statusText = equipped && !inInventory ? " (equipped)" : " (in inventory)";
			}
			else if (inBank)
			{
				borderColor = ITEM_STATUS_YELLOW;
				statusText = " (in bank)";
			}
			else
			{
				borderColor = ITEM_STATUS_RED;
				statusText = " (MISSING)";
			}

			JLabel itemLabel = new JLabel();
			itemLabel.setPreferredSize(new Dimension(32, 32));
			itemLabel.setBorder(BorderFactory.createLineBorder(borderColor, 2));
			itemLabel.setHorizontalAlignment(SwingConstants.CENTER);
			itemLabel.setVerticalAlignment(SwingConstants.CENTER);

			// Load item sprite asynchronously
			AsyncBufferedImage asyncImage = itemManager.getImage(itemId);
			asyncImage.onLoaded(() ->
			{
				BufferedImage scaled = scaleImage(asyncImage, 28, 28);
				itemLabel.setIcon(new ImageIcon(scaled));
				itemLabel.revalidate();
				itemLabel.repaint();
			});
			// Set initial image (may be placeholder)
			BufferedImage scaled = scaleImage(asyncImage, 28, 28);
			itemLabel.setIcon(new ImageIcon(scaled));

			itemLabel.setToolTipText(itemManager.getItemComposition(itemId).getName() + statusText);

			requiredItemsPanel.add(itemLabel);
		}

		requiredItemsPanel.setVisible(true);
		requiredItemsPanel.revalidate();
		requiredItemsPanel.repaint();
	}

	/**
	 * Scales a BufferedImage to the given dimensions.
	 */
	private static BufferedImage scaleImage(BufferedImage source, int width, int height)
	{
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.drawImage(source, 0, 0, width, height, null);
		g.dispose();
		return scaled;
	}

	/**
	 * Hides the step progress banner.
	 */
	public void hideStepProgress()
	{
		SwingUtilities.invokeLater(() ->
		{
			requiredItemsPanel.removeAll();
			requiredItemsPanel.setVisible(false);
			stepProgressPanel.setVisible(false);
			stepProgressPanel.revalidate();
			if (stepProgressPanel.getParent() != null)
			{
				stepProgressPanel.getParent().revalidate();
			}
		});
	}

	/**
	 * Sets callbacks for step advance/skip buttons.
	 */
	public void setStepCallbacks(Runnable advancer, Runnable skipper)
	{
		this.stepAdvancer = advancer;
		this.stepSkipper = skipper;
	}

	public void hideClueGuidance()
	{
		SwingUtilities.invokeLater(() ->
		{
			clueGuidanceBanner.setVisible(false);
			clueGuidanceBanner.revalidate();
			if (clueGuidanceBanner.getParent() != null)
			{
				clueGuidanceBanner.getParent().revalidate();
			}
		});
	}

	private void addEmptyStateMessage(String message)
	{
		JLabel label = new JLabel("<html><center>" + message + "</center></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setAlignmentX(CENTER_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
		label.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		listContainer.add(label);
	}

	private void updateControlVisibility()
	{
		afkFilterSelector.setVisible(currentMode == Mode.EFFICIENT || currentMode == Mode.PET_HUNT);
		sortSelector.setVisible(currentMode == Mode.EFFICIENT);
		categorySelector.setVisible(currentMode == Mode.CATEGORY_FOCUS);
		searchField.setVisible(currentMode == Mode.SEARCH);
	}
}
