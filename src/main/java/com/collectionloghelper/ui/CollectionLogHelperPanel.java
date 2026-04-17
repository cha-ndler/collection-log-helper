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
import com.collectionloghelper.ui.mode.CategoryModeController;
import com.collectionloghelper.ui.mode.EfficientModeController;
import com.collectionloghelper.ui.mode.PanelModeController;
import com.collectionloghelper.ui.mode.PanelModeDispatcher;
import com.collectionloghelper.ui.mode.PanelShellContext;
import com.collectionloghelper.ui.mode.PetHuntModeController;
import com.collectionloghelper.ui.mode.SearchModeController;
import com.collectionloghelper.ui.mode.StatisticsModeController;
import com.collectionloghelper.ui.widget.ClueSummaryView;
import com.collectionloghelper.ui.widget.GuidanceBannerView;
import com.collectionloghelper.ui.widget.SlayerStrategyView;
import com.collectionloghelper.ui.widget.StepProgressView;
import com.collectionloghelper.ui.widget.SyncStatusView;
import java.util.EnumMap;
import java.util.Map;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

public class CollectionLogHelperPanel extends PluginPanel implements PanelShellContext
{
	private static final Color GUIDE_ME_COLOR = new Color(30, 120, 30);
	private static final Color STOP_GUIDANCE_COLOR = new Color(140, 30, 30);

	public enum Mode
	{
		EFFICIENT("Efficient"),
		CATEGORY_FOCUS("Category Focus"),
		SEARCH("Search"),
		PET_HUNT("Pet Hunt"),
		STATISTICS("Statistics");

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
	private final Consumer<EfficientSortMode> sortModeUpdater;

	private final SyncStatusView syncStatusView;
	private final ClueSummaryView clueSummaryView;

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

	private final GuidanceBannerView guidanceBannerView;
	private final SlayerStrategyView slayerStrategyView;

	private final StepProgressView stepProgressView;

	private final Timer searchDebounceTimer;

	private final Map<Mode, PanelModeController> modeControllers = new EnumMap<>(Mode.class);
	private final PanelModeDispatcher<Mode> modeDispatcher = new PanelModeDispatcher<>(modeControllers);

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
		Consumer<AfkFilter> afkFilterUpdater,
		Consumer<EfficientSortMode> sortModeUpdater)
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
		this.sortModeUpdater = sortModeUpdater;

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

		// Sync status + data-sync warning (extracted to SyncStatusView)
		syncStatusView = new SyncStatusView(dataSyncState);
		syncStatusView.setAlignmentX(CENTER_ALIGNMENT);
		syncStatusView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(syncStatusView);

		// Clue summary (extracted to ClueSummaryView)
		clueSummaryView = new ClueSummaryView();
		clueSummaryView.setAlignmentX(CENTER_ALIGNMENT);
		clueSummaryView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(clueSummaryView);

		// Slayer task indicator + strategy advisor (extracted to SlayerStrategyView)
		slayerStrategyView = new SlayerStrategyView(slayerTaskState, slayerStrategyCalculator);
		slayerStrategyView.setAlignmentX(CENTER_ALIGNMENT);
		slayerStrategyView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(slayerStrategyView);

		// Guidance banners (active + clue) extracted to GuidanceBannerView
		guidanceBannerView = new GuidanceBannerView(requirementsChecker);
		guidanceBannerView.setAlignmentX(CENTER_ALIGNMENT);
		guidanceBannerView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		controlsPanel.add(guidanceBannerView);

		// Step progress (extracted to StepProgressView)
		stepProgressView = new StepProgressView(itemManager, inventoryState, bankState);
		stepProgressView.setAlignmentX(CENTER_ALIGNMENT);
		controlsPanel.add(stepProgressView);

		controlsPanel.add(Box.createVerticalStrut(4));

		// Mode selector
		modeSelector = new JComboBox<>(Mode.values());
		modeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		modeSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				Mode previous = currentMode;
				currentMode = (Mode) e.getItem();
				modeDispatcher.switchMode(previous, currentMode);
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
		sortSelector.setSelectedItem(config.efficientSortMode());
		sortSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		sortSelector.setVisible(currentMode == Mode.EFFICIENT);
		sortSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				EfficientSortMode selected = (EfficientSortMode) sortSelector.getSelectedItem();
				if (selected != null)
				{
					sortModeUpdater.accept(selected);
				}
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

		// Mode controllers — populated once, dispatched from rebuild()
		modeControllers.put(Mode.STATISTICS,
			new StatisticsModeController(this, collectionState, database, calculator));
		modeControllers.put(Mode.PET_HUNT,
			new PetHuntModeController(this, config, collectionState, calculator,
				requirementsChecker, itemManager));
		modeControllers.put(Mode.SEARCH,
			new SearchModeController(this, config, database, collectionState, calculator,
				requirementsChecker, itemManager));
		modeControllers.put(Mode.CATEGORY_FOCUS,
			new CategoryModeController(this, config, database, collectionState, calculator,
				requirementsChecker, itemManager));
		modeControllers.put(Mode.EFFICIENT,
			new EfficientModeController(this, config, collectionState, calculator,
				requirementsChecker, itemManager));

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


	public void updateSyncStatus(SyncState state, int itemCount)
	{
		syncStatusView.updateSyncStatus(state, itemCount);
	}

	public void updateDataSyncWarning()
	{
		syncStatusView.updateDataSyncWarning();
	}

	public void updateClueSummary(PlayerBankState bankState)
	{
		clueSummaryView.updateFromBankState(bankState);
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
				slayerStrategyView.refresh();

				modeDispatcher.buildView(currentMode);

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

	@Override
	public void showDetail(CollectionLogItem item, CollectionLogSource source)
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

	@Override
	public JPanel createQuickGuidePanel(ScoredItem topItem)
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
		if (active && source != null)
		{
			guidanceBannerView.showGuidance(source);
		}
		else
		{
			guidanceBannerView.hideGuidance();
		}
		// Sync top pick button with current guidance state (EDT-safe via invokeLater)
		SwingUtilities.invokeLater(() ->
		{
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
		guidanceBannerView.showClueGuidance(source);
	}

	/**
	 * Updates the step progress banner with current step info.
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<Integer> requiredItemIds)
	{
		stepProgressView.showStep(current, total, description, isManual, requiredItemIds);
	}

	/** Hides the step progress banner. */
	public void hideStepProgress()
	{
		stepProgressView.hide();
	}

	/** Sets callbacks for step advance/skip buttons. */
	public void setStepCallbacks(Runnable advancer, Runnable skipper)
	{
		stepProgressView.setCallbacks(advancer, skipper);
	}

	public void hideClueGuidance()
	{
		guidanceBannerView.hideClueGuidance();
	}

	@Override
	public void addEmptyStateMessage(String message)
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
		afkFilterSelector.setVisible(currentMode == Mode.EFFICIENT || currentMode == Mode.PET_HUNT
			|| currentMode == Mode.CATEGORY_FOCUS);
		sortSelector.setVisible(currentMode == Mode.EFFICIENT);
		categorySelector.setVisible(currentMode == Mode.CATEGORY_FOCUS);
		searchField.setVisible(currentMode == Mode.SEARCH);
	}

	// ── PanelShellContext implementation ────────────────────────────────────

	@Override
	public void addToList(Component component)
	{
		listContainer.add(component);
	}

	@Override
	public void switchToCategoryFocus(CollectionLogCategory category)
	{
		categorySelector.setSelectedItem(category);
		modeSelector.setSelectedItem(Mode.CATEGORY_FOCUS);
	}

	@Override
	public String getSearchQuery()
	{
		return searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
	}

	@Override
	public EfficientSortMode getEfficientSortMode()
	{
		return (EfficientSortMode) sortSelector.getSelectedItem();
	}

	@Override
	public CollectionLogCategory getSelectedCategory()
	{
		return (CollectionLogCategory) categorySelector.getSelectedItem();
	}
}
