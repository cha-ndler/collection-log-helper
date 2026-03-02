package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

public class CollectionLogHelperPanel extends PluginPanel
{
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

	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final ItemManager itemManager;
	private final BiConsumer<WorldPoint, String> guidanceActivator;
	private final Runnable guidanceDeactivator;

	private final JComboBox<Mode> modeSelector;
	private final JComboBox<CollectionLogCategory> categorySelector;
	private final JTextField searchField;
	private final JLabel completionLabel;
	private final JPanel listContainer;
	private final CardLayout detailCardLayout;
	private final JPanel detailCardPanel;
	private final JPanel listView;
	private final JPanel detailView;

	private Mode currentMode = Mode.EFFICIENT;
	private boolean rebuilding = false;
	private boolean rebuildPending = false;

	public CollectionLogHelperPanel(DropRateDatabase database, PlayerCollectionState collectionState,
		EfficiencyCalculator calculator, ItemManager itemManager,
		BiConsumer<WorldPoint, String> guidanceActivator, Runnable guidanceDeactivator)
	{
		this.database = database;
		this.collectionState = collectionState;
		this.calculator = calculator;
		this.itemManager = itemManager;
		this.guidanceActivator = guidanceActivator;
		this.guidanceDeactivator = guidanceDeactivator;

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
		completionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		controlsPanel.add(completionLabel);

		// Mode selector
		modeSelector = new JComboBox<>(Mode.values());
		modeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		modeSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				currentMode = (Mode) e.getItem();
				updateControlVisibility();
				rebuild();
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

		add(controlsPanel, BorderLayout.NORTH);

		// Card layout for list vs detail views
		detailCardLayout = new CardLayout();
		detailCardPanel = new JPanel(detailCardLayout);
		detailCardPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// List view
		listView = new JPanel(new BorderLayout());
		listView.setBackground(ColorScheme.DARK_GRAY_COLOR);

		listContainer = new JPanel();
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
		listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listView.add(listContainer, BorderLayout.NORTH);

		detailCardPanel.add(listView, "list");

		// Detail view placeholder
		detailView = new JPanel(new BorderLayout());
		detailView.setBackground(ColorScheme.DARK_GRAY_COLOR);
		detailCardPanel.add(detailView, "detail");

		add(detailCardPanel, BorderLayout.CENTER);

		updateControlVisibility();
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
				SwingUtil.fastRemoveAll(listContainer);
				updateCompletionHeader();

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

				listContainer.revalidate();
				listContainer.repaint();
				detailCardLayout.show(detailCardPanel, "list");
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

	private void buildEfficientView()
	{
		List<ScoredItem> scored = calculator.rankByEfficiency();

		for (ScoredItem si : scored)
		{
			for (CollectionLogItem item : si.getSource().getItems())
			{
				boolean obtained = collectionState.isItemObtained(item.getVarbitId());
				if (obtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, si.getSource(), false,
					si.getScore(), itemManager,
					() -> showDetail(item, si.getSource()));
				listContainer.add(row);
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

		List<CollectionLogSource> sources = database.getSourcesByCategory(category);
		CategorySummaryPanel summary = new CategorySummaryPanel(
			category, sources, collectionState, itemManager,
			this::showDetail);
		listContainer.add(summary);
	}

	private void buildSearchView()
	{
		String query = searchField.getText().toLowerCase().trim();
		if (query.isEmpty())
		{
			return;
		}

		for (CollectionLogSource source : database.getAllSources())
		{
			for (CollectionLogItem item : source.getItems())
			{
				if (item.getName().toLowerCase().contains(query)
					|| source.getName().toLowerCase().contains(query))
				{
					boolean obtained = collectionState.isItemObtained(item.getVarbitId());
					ItemRowPanel row = new ItemRowPanel(item, source, obtained, 0,
						itemManager, () -> showDetail(item, source));
					listContainer.add(row);
				}
			}
		}
	}

	private void buildPetHuntView()
	{
		List<ScoredItem> scored = calculator.filterPetsOnly();

		for (ScoredItem si : scored)
		{
			for (CollectionLogItem item : si.getSource().getItems())
			{
				if (!item.isPet())
				{
					continue;
				}
				boolean obtained = collectionState.isItemObtained(item.getVarbitId());
				if (obtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, si.getSource(), false,
					si.getScore(), itemManager,
					() -> showDetail(item, si.getSource()));
				listContainer.add(row);
			}
		}
	}

	private void showDetail(CollectionLogItem item, CollectionLogSource source)
	{
		boolean obtained = collectionState.isItemObtained(item.getVarbitId());

		SwingUtilities.invokeLater(() ->
		{
			detailView.removeAll();
			ItemDetailPanel detail = new ItemDetailPanel(
				item, source, obtained, itemManager,
				() ->
				{
					guidanceDeactivator.run();
					detailCardLayout.show(detailCardPanel, "list");
				},
				() -> guidanceActivator.accept(source.getWorldPoint(), source.getName())
			);
			detailView.add(detail, BorderLayout.CENTER);
			detailView.revalidate();
			detailCardLayout.show(detailCardPanel, "detail");
		});
	}

	private void updateControlVisibility()
	{
		categorySelector.setVisible(currentMode == Mode.CATEGORY_FOCUS);
		searchField.setVisible(currentMode == Mode.SEARCH);
	}
}
