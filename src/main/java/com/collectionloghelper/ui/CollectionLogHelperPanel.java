package com.collectionloghelper.ui;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
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
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
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
		PET_HUNT("Pet Hunt"),
		PROXIMITY("Efficient by Proximity (Experimental)");

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
	private final Consumer<CollectionLogSource> guidanceActivator;
	private final Runnable guidanceDeactivator;
	private final Supplier<WorldPoint> playerLocationSupplier;

	private final JLabel syncStatusLabel;
	private final JLabel dataSyncWarningLabel;
	private final JLabel clueSummaryLabel;
	private final JLabel slayerTaskLabel;

	private final JComboBox<Mode> modeSelector;
	private final JComboBox<CollectionLogCategory> categorySelector;
	private final JTextField searchField;
	private final JLabel completionLabel;
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

	private Mode currentMode = Mode.EFFICIENT;
	private boolean rebuilding = false;
	private boolean rebuildPending = false;
	private boolean guidanceActive = false;
	private CollectionLogSource guidedSource = null;
	private boolean inDetailView = false;

	public CollectionLogHelperPanel(CollectionLogHelperConfig config,
		DropRateDatabase database, PlayerCollectionState collectionState,
		EfficiencyCalculator calculator, ClueCompletionEstimator clueEstimator,
		ItemManager itemManager,
		RequirementsChecker requirementsChecker, DataSyncState dataSyncState,
		SlayerTaskState slayerTaskState,
		SlayerStrategyCalculator slayerStrategyCalculator,
		Consumer<CollectionLogSource> guidanceActivator, Runnable guidanceDeactivator,
		Supplier<WorldPoint> playerLocationSupplier)
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
		this.guidanceActivator = guidanceActivator;
		this.guidanceDeactivator = guidanceDeactivator;
		this.playerLocationSupplier = playerLocationSupplier;

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
		completionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		controlsPanel.add(completionLabel);

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

		JButton strategyToggle = new JButton("Slayer Strategy");
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
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
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
		if (!slayerStrategyPanel.isVisible() || !slayerStrategyExpanded)
		{
			slayerStrategyLabel.setVisible(false);
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
		String recommended = slayerStrategyCalculator.getRecommendedMaster();
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
					case PROXIMITY:
						buildProximityView();
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
		boolean hideObtained = config.hideObtainedItems();

		// Top Pick should always be an accessible (unlocked) source
		scored.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick -> listContainer.add(createQuickGuidePanel(topPick)));

		for (ScoredItem si : scored)
		{
			boolean onTask = calculator.isOnSlayerTask(si.getSource());
			boolean hasGuaranteed = hasUnobtainedGuaranteedItems(si);

			for (CollectionLogItem item : si.getSource().getItems())
			{
				boolean obtained = collectionState.isItemObtained(item.getItemId());
				if (hideObtained && obtained)
				{
					continue;
				}
				// When a source has unobtained guaranteed items, only show those
				if (hasGuaranteed && item.getDropRate() < 1.0)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, si.getSource(), obtained,
					si.getScore(), si.isLocked(), onTask, itemManager,
					() -> showDetail(item, si.getSource()));
				listContainer.add(row);
			}
		}
	}

	private boolean hasUnobtainedGuaranteedItems(ScoredItem si)
	{
		for (CollectionLogItem item : si.getSource().getItems())
		{
			if (item.getDropRate() >= 1.0 && !collectionState.isItemObtained(item.getItemId()))
			{
				return true;
			}
		}
		return false;
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
			return;
		}

		boolean hideObtained = config.hideObtainedItems();
		boolean hideLocked = config.hideLockedContent();

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			boolean onTask = calculator.isOnSlayerTask(source);
			for (CollectionLogItem item : source.getItems())
			{
				if (item.getName().toLowerCase().contains(query)
					|| source.getName().toLowerCase().contains(query))
				{
					boolean obtained = collectionState.isItemObtained(item.getItemId());
					if (hideObtained && obtained)
					{
						continue;
					}
					ItemRowPanel row = new ItemRowPanel(item, source, obtained, 0,
						locked, onTask, itemManager, () -> showDetail(item, source));
					listContainer.add(row);
				}
			}
		}
	}

	private void buildPetHuntView()
	{
		List<ScoredItem> scored = calculator.filterPetsOnly();
		boolean hideObtained = config.hideObtainedItems();

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
			}
		}
	}

	private void buildProximityView()
	{
		// cachedPlayerLocation is written on the client thread and read here on the EDT.
		// volatile guarantees the EDT always sees the latest value.
		WorldPoint playerLocation = playerLocationSupplier.get();
		// (0,0,0) occurs briefly during login/logout transitions — treat as not ready
		if (playerLocation == null
			|| (playerLocation.getX() == 0 && playerLocation.getY() == 0))
		{
			JLabel loginLabel = new JLabel("Log in to use Proximity mode");
			loginLabel.setFont(FontManager.getRunescapeSmallFont());
			loginLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			loginLabel.setAlignmentX(LEFT_ALIGNMENT);
			loginLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
			listContainer.add(loginLabel);
			return;
		}

		boolean hideObtained = config.hideObtainedItems();
		boolean hideLocked = config.hideLockedContent();

		// Iterate all sources directly — do not use rankByEfficiency() which
		// filters out completed sources and may skip sources with no drop scores.
		List<SourceDistance> sourceDistances = new ArrayList<>();
		for (CollectionLogSource source : database.getAllSources())
		{
			// Clue sources have no meaningful physical location
			if (source.getCategory() == CollectionLogCategory.CLUES)
			{
				continue;
			}

			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			WorldPoint sourcePoint = source.getWorldPoint();
			if (sourcePoint == null || (sourcePoint.getX() == 0 && sourcePoint.getY() == 0))
			{
				continue;
			}

			int missingCount = 0;
			for (CollectionLogItem item : source.getItems())
			{
				if (!collectionState.isItemObtained(item.getItemId()))
				{
					missingCount++;
				}
			}
			if (missingCount == 0)
			{
				continue;
			}

			// Use plane-agnostic distance so sources remain visible when
			// the player is on a different plane (e.g. sailing, instances).
			// distanceTo2D ignores plane and returns Chebyshev distance.
			// Ref: RuneLite API WorldPoint.distanceTo2D()
			int distance = playerLocation.distanceTo2D(sourcePoint);

			int maxDistance = config.proximityMaxDistance();
			if (maxDistance > 0 && distance > maxDistance)
			{
				continue;
			}

			// Compute efficiency score for composite ranking
			ScoredItem scored = calculator.scoreSource(source, locked);
			double efficiencyScore = scored != null ? scored.getScore() : 0.0;

			sourceDistances.add(new SourceDistance(source, distance, missingCount, locked,
				efficiencyScore));
		}

		if (sourceDistances.isEmpty())
		{
			JLabel emptyLabel = new JLabel(
				"<html>All nearby items obtained.<br>Open your Collection Log to sync.</html>");
			emptyLabel.setFont(FontManager.getRunescapeSmallFont());
			emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
			emptyLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
			listContainer.add(emptyLabel);
			return;
		}

		// Sort by composite score (efficiency weighted by proximity) descending
		sourceDistances.sort(
			Comparator.comparingDouble((SourceDistance sd) -> sd.compositeScore()).reversed());

		// Top Pick: highest composite-scored accessible (unlocked) source
		for (SourceDistance sd : sourceDistances)
		{
			if (!sd.locked)
			{
				String reasoning = String.format("%d missing items, %d tiles away (score: %.1f)",
					sd.missingCount, sd.distance, sd.compositeScore());
				ScoredItem topPick = new ScoredItem(sd.source, sd.compositeScore(), sd.missingCount, reasoning, false, sd.compositeScore());
				listContainer.add(createQuickGuidePanel(topPick));
				break;
			}
		}

		// List sources with their items, grouped by source with distance header
		for (SourceDistance sd : sourceDistances)
		{
			CollectionLogSource source = sd.source;

			// Check whether any items are visible before adding the header,
			// so we don't render an orphaned header with nothing below it.
			boolean hasVisibleItems = false;
			for (CollectionLogItem item : source.getItems())
			{
				boolean obtained = collectionState.isItemObtained(item.getItemId());
				if (!hideObtained || !obtained)
				{
					hasVisibleItems = true;
					break;
				}
			}
			if (!hasVisibleItems)
			{
				continue;
			}

			JLabel sourceHeader = new JLabel(String.format("%s  \u2014  %d tiles  (%.1f)",
				source.getName(), sd.distance, sd.compositeScore()));
			sourceHeader.setFont(FontManager.getRunescapeBoldFont());
			sourceHeader.setForeground(new Color(200, 200, 200));
			sourceHeader.setAlignmentX(LEFT_ALIGNMENT);
			sourceHeader.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));
			sourceHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
			listContainer.add(sourceHeader);

			boolean onTask = calculator.isOnSlayerTask(source);
			for (CollectionLogItem item : source.getItems())
			{
				boolean obtained = collectionState.isItemObtained(item.getItemId());
				if (hideObtained && obtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, source, obtained,
					sd.missingCount, sd.locked, onTask, itemManager,
					() -> showDetail(item, source));
				listContainer.add(row);
			}
		}
	}

	/** Carries a source's computed distance and efficiency score for the proximity view. */
	private static final class SourceDistance
	{
		final CollectionLogSource source;
		final int distance;
		final int missingCount;
		final boolean locked;
		final double efficiencyScore;

		SourceDistance(CollectionLogSource source, int distance, int missingCount, boolean locked,
			double efficiencyScore)
		{
			this.source = source;
			this.distance = distance;
			this.missingCount = missingCount;
			this.locked = locked;
			this.efficiencyScore = efficiencyScore;
		}

		/** Composite score: efficiency weighted by proximity. */
		double compositeScore()
		{
			return efficiencyScore / (1.0 + distance / 100.0);
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

		return panel;
	}

	public void setGuidanceState(boolean active, CollectionLogSource source)
	{
		guidanceActive = active;
		guidedSource = source;
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

	private void updateControlVisibility()
	{
		categorySelector.setVisible(currentMode == Mode.CATEGORY_FOCUS);
		searchField.setVisible(currentMode == Mode.SEARCH);
	}
}
