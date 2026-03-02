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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Consumer;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

public class CollectionLogHelperPanel extends PluginPanel
{
	private static final Color GUIDE_ME_COLOR = new Color(30, 120, 30);
	private static final Color STOP_GUIDANCE_COLOR = new Color(140, 30, 30);

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
	private final Consumer<CollectionLogSource> guidanceActivator;
	private final Runnable guidanceDeactivator;

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

	private Mode currentMode = Mode.EFFICIENT;
	private boolean rebuilding = false;
	private boolean rebuildPending = false;
	private boolean guidanceActive = false;
	private CollectionLogSource guidedSource = null;

	public CollectionLogHelperPanel(DropRateDatabase database, PlayerCollectionState collectionState,
		EfficiencyCalculator calculator, ItemManager itemManager,
		Consumer<CollectionLogSource> guidanceActivator, Runnable guidanceDeactivator)
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
				showListView();
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
		cardLayout.show(contentPanel, "list");
		refreshScrollPane();
	}

	private void showDetailView()
	{
		cardLayout.show(contentPanel, "detail");
		refreshScrollPane();
	}

	private void refreshScrollPane()
	{
		JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
			JScrollPane.class, this);
		if (scrollPane != null)
		{
			scrollPane.validate();
			scrollPane.getVerticalScrollBar().setValue(0);
		}
	}

	private void buildEfficientView()
	{
		List<ScoredItem> scored = calculator.rankByEfficiency();

		if (!scored.isEmpty())
		{
			listContainer.add(createQuickGuidePanel(scored.get(0)));
		}

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
		boolean isGuidingThis = guidanceActive && guidedSource != null
			&& guidedSource.getName().equals(source.getName());

		detailView.removeAll();
		ItemDetailPanel detail = new ItemDetailPanel(
			item, source, obtained, itemManager,
			() ->
			{
				guidanceDeactivator.run();
				setGuidanceState(false, null);
				showListView();
			},
			() ->
			{
				guidanceActivator.accept(source);
				setGuidanceState(true, source);
			},
			() ->
			{
				guidanceDeactivator.run();
				setGuidanceState(false, null);
			},
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
