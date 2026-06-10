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
import com.collectionloghelper.data.RequirementRow;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.guidance.RequiredItemDisplay;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import com.collectionloghelper.learning.DryStreakAnalyzer;
import com.collectionloghelper.ui.mode.CategoryModeController;
import com.collectionloghelper.ui.mode.DryStreakFeedModeController;
import com.collectionloghelper.ui.mode.EfficientModeController;
import com.collectionloghelper.ui.mode.PanelModeController;
import com.collectionloghelper.ui.mode.PanelModeDispatcher;
import com.collectionloghelper.ui.mode.PanelShellContext;
import com.collectionloghelper.ui.mode.PetHuntModeController;
import com.collectionloghelper.ui.mode.SearchModeController;
import com.collectionloghelper.ui.mode.StatisticsModeController;
import com.collectionloghelper.ui.widget.ClueSummaryView;
import com.collectionloghelper.ui.widget.GuidanceBannerView;
import com.collectionloghelper.ui.widget.ListContainerScrollPane;
import com.collectionloghelper.ui.widget.QuickGuidePanelView;
import com.collectionloghelper.ui.widget.SlayerStrategyView;
import com.collectionloghelper.ui.widget.StepProgressView;
import com.collectionloghelper.ui.widget.SyncStatusView;
import java.util.EnumMap;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class CollectionLogHelperPanel extends PluginPanel implements PanelShellContext
{

	public enum Mode
	{
		EFFICIENT("Efficient"),
		CATEGORY_FOCUS("Category Focus"),
		SEARCH("Search"),
		PET_HUNT("Pet Hunt"),
		STATISTICS("Statistics"),
		DRY_STREAK("Dry Streaks");

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
	private DryStreakFeedModeController dryStreakFeedController;
	private final ClueCompletionEstimator clueEstimator;
	private final ItemManager itemManager;
	private final RequirementsChecker requirementsChecker;
	private final BiConsumer<CollectionLogSource, Integer> guidanceActivator;
	private final Runnable guidanceDeactivator;
	private final Consumer<AfkFilter> afkFilterUpdater;
	private final Consumer<EfficientSortMode> sortModeUpdater;

	private final SyncStatusView syncStatusView;
	private final ClueSummaryView clueSummaryView;

	private final SelectorControlsPanel selectorControls;
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
	private final QuickGuidePanelView quickGuidePanelView;

	private final Map<Mode, PanelModeController> modeControllers = new EnumMap<>(Mode.class);
	private final PanelModeDispatcher<Mode> modeDispatcher = new PanelModeDispatcher<>(modeControllers);
	private final PanelRebuildOrchestrator rebuildOrchestrator;
	private final DetailViewBuilder detailViewBuilder;

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
		PlayerInventoryState inventoryState,
		PlayerBankState bankState,
		DryStreakAnalyzer dryStreakAnalyzer,
		BiConsumer<CollectionLogSource, Integer> guidanceActivator, Runnable guidanceDeactivator,
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
		this.guidanceActivator = guidanceActivator;
		this.guidanceDeactivator = guidanceDeactivator;
		this.afkFilterUpdater = afkFilterUpdater;
		this.sortModeUpdater = sortModeUpdater;

		setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		// === Controls panel (north) — static header widgets built by PanelHeaderBuilder ===
		PanelHeaderBuilder.Result header = PanelHeaderBuilder.build(
			config,
			dataSyncState,
			slayerTaskState,
			slayerStrategyCalculator,
			requirementsChecker,
			itemManager,
			guidanceActivator,
			guidanceDeactivator);
		JPanel controlsPanel = header.controlsPanel;
		completionLabel = header.completionLabel;
		completionProgressBar = header.completionProgressBar;
		syncStatusView = header.syncStatusView;
		clueSummaryView = header.clueSummaryView;
		slayerStrategyView = header.slayerStrategyView;
		guidanceBannerView = header.guidanceBannerView;
		stepProgressView = header.stepProgressView;
		quickGuidePanelView = header.quickGuidePanelView;

		selectorControls = new SelectorControlsPanel(
			config,
			currentMode,
			this::onModeSelected,
			afkFilter ->
			{
				afkFilterUpdater.accept(afkFilter);
				rebuild();
			},
			sortMode ->
			{
				sortModeUpdater.accept(sortMode);
				rebuild();
			},
			this::rebuild);
		controlsPanel.add(selectorControls);

		add(controlsPanel, BorderLayout.NORTH);

		// === Content panel (center, CardLayout) ===
		cardLayout = new CardLayout();
		contentPanel = new ListContainerScrollPane(cardLayout);
		contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		listView = new JPanel(new BorderLayout());
		listView.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listContainer = new JPanel();
		listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
		listContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listView.add(listContainer, BorderLayout.NORTH);
		rebuildOrchestrator = new PanelRebuildOrchestrator(this, listContainer);
		detailViewBuilder = new DetailViewBuilder(
			collectionState,
			requirementsChecker,
			itemManager,
			clueEstimator,
			guidanceActivator,
			guidanceDeactivator);

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
			new CategoryModeController(this, config, collectionState, calculator,
				requirementsChecker, itemManager));
		modeControllers.put(Mode.EFFICIENT,
			new EfficientModeController(this, config, collectionState, calculator,
				requirementsChecker, itemManager));

		dryStreakFeedController = new DryStreakFeedModeController(this, collectionState,
			dryStreakAnalyzer);
		modeControllers.put(Mode.DRY_STREAK, dryStreakFeedController);

		updateControlVisibility();
	}

	public Mode getCurrentMode()
	{
		return currentMode;
	}

	public void setMode(Mode mode)
	{
		// Route programmatic mode changes through the selector listener so the
		// mode-controller lifecycle hooks (onModeDeactivated / onModeActivated)
		// fire just like a user-driven selection. Mutating currentMode here
		// before calling setSelectedMode(...) would cause the listener to see
		// previous == next and short-circuit the dispatcher, silently breaking
		// the contract for any controller that overrides those hooks.
		if (mode == currentMode)
		{
			// Same-mode call: the combo listener won't fire, so apply the
			// visibility + rebuild side effects directly.
			updateControlVisibility();
			rebuild();
			return;
		}
		selectorControls.setSelectedMode(mode);
	}

	private void onModeSelected(Mode mode)
	{
		Mode previous = currentMode;
		currentMode = mode;
		modeDispatcher.switchMode(previous, currentMode);
		updateControlVisibility();
		inDetailView = false;
		rebuild();
		resetScrollPosition();
	}

	public void updateCompletionHeader()
	{
		int obtained = collectionState.getTotalObtained();
		int total = collectionState.getTotalPossible();
		double pct = collectionState.getCompletionPercentage();
		completionProgressBar.setMaximum(Math.max(total, 1));
		completionProgressBar.setValue(obtained);
		completionProgressBar.setString(String.format("%d / %d  (%.1f%%)", obtained, total, pct));
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

	/**
	 * Updates the kill counts available to the dry-streak feed. Call this after any
	 * KC-data refresh (in-game varp update, TempleOSRS sync, etc.). A subsequent
	 * {@link #rebuild()} is required to repaint the feed.
	 *
	 * @param killCounts map of source name to kill count; {@code null} is treated as empty
	 */
	public void updateDryStreakKillCounts(java.util.Map<String, Integer> killCounts)
	{
		if (dryStreakFeedController != null)
		{
			dryStreakFeedController.setKillCounts(killCounts);
		}
	}

	public void shutDown()
	{
		selectorControls.shutDown();
	}

	/**
	 * Reports the result of an auto TempleOSRS KC sync as a brief, one-line
	 * status in the sync-status view. Safe to call from any thread.
	 *
	 * @param success whether the sync succeeded
	 */
	public void onTempleSyncComplete(boolean success)
	{
		syncStatusView.showTransientStatus(
			success ? "Synced KC from TempleOSRS" : "TempleOSRS KC sync failed", success);
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
				PanelRebuildOrchestrator.RebuildSnapshot snapshot = rebuildOrchestrator.capture();

				// Plain removeAll() rather than SwingUtil.fastRemoveAll(): the latter
				// pumps pending AWT events mid-rebuild, which lets Swing paint the
				// now-empty container before buildView() repopulates it -- the visible
				// flash on frequent updates (e.g. every XP drop while on a Slayer task).
				// removeAll() does not pump, so the EDT stays inside this runnable from
				// clear through repopulate to the single revalidate/repaint below, and
				// no empty intermediate frame is ever painted.
				listContainer.removeAll();
				updateCompletionHeader();
				slayerStrategyView.refresh(isSlayerContext());

				modeDispatcher.buildView(currentMode);

				rebuildOrchestrator.restoreExpanded(snapshot);

				listContainer.revalidate();
				listContainer.repaint();
				if (!inDetailView)
				{
					showListView();
					rebuildOrchestrator.deferScrollRestore(snapshot);
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
		detailViewBuilder.populate(detailView, item, source, guidanceActive, guidedSource, this::showListView);
		showDetailView();
	}

	@Override
	public JPanel createQuickGuidePanel(ScoredItem topItem)
	{
		return quickGuidePanelView.create(topItem, guidanceActive, guidedSource);
	}

	public void setGuidanceState(boolean active, CollectionLogSource source,
		List<RequirementRow> requirementRows)
	{
		guidanceActive = active;
		guidedSource = source;
		if (active && source != null)
		{
			guidanceBannerView.showGuidance(source, requirementRows);
		}
		else
		{
			guidanceBannerView.hideGuidance();
		}
		quickGuidePanelView.syncGuidanceState(active, source);
		// #576 — sync the source-level Guide Me / Stop Guidance button on the
		// active item-detail card. Without this fan-out, deactivating guidance
		// from the step-control STOP icon would update the Quick Guide button
		// but leave the detail-view button stuck in "Stop Guidance" mode.
		detailViewBuilder.syncGuidanceState(active, source);
	}

	public void showClueGuidance(CollectionLogSource source)
	{
		guidanceBannerView.showClueGuidance(source);
	}

	/**
	 * Updates the step progress banner with current step info (flat layout).
	 *
	 * @param requiredItems pre-resolved display rows from
	 *                      {@link com.collectionloghelper.guidance.RequiredItemResolver};
	 *                      may be {@code null} or empty when the step has no required items
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems)
	{
		stepProgressView.showStep(current, total, description, isManual, requiredItems);
	}

	/**
	 * Updates the step progress banner with current step info, enabling collapsible
	 * section blocks when {@code allSteps} contains steps with section labels.
	 *
	 * @param requiredItems    pre-resolved required-item display rows; may be {@code null}
	 *                         or empty
	 * @param allSteps         full ordered step list for the active source; used to compute
	 *                         section groups — pass an empty list for flat layout
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems,
		List<com.collectionloghelper.data.GuidanceStep> allSteps)
	{
		stepProgressView.showStep(current, total, description, isManual, requiredItems, allSteps);
	}

	/**
	 * Updates the step progress banner with current step info, both required and
	 * recommended items, and enables collapsible section blocks when {@code allSteps}
	 * contains steps with section labels.
	 *
	 * @param requiredItems    pre-resolved required-item display rows; may be {@code null}
	 *                         or empty
	 * @param recommendedItems pre-resolved recommended-item display rows; may be
	 *                         {@code null} or empty
	 * @param allSteps         full ordered step list for the active source; used to compute
	 *                         section groups — pass an empty list for flat layout
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems,
		List<RequiredItemDisplay> recommendedItems,
		List<com.collectionloghelper.data.GuidanceStep> allSteps)
	{
		stepProgressView.showStep(current, total, description, isManual,
			requiredItems, recommendedItems, allSteps);
	}

	/**
	 * Step-progress update variant that additionally tags item rows whose id is in
	 * {@code activityObtainableIds} with a muted "(from activity)" suffix
	 * (Phase 2 guidance-items redesign).
	 *
	 * @param activityObtainableIds item IDs obtained during the active step's activity;
	 *                              may be {@code null} or empty
	 */
	public void updateStepProgress(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems,
		List<RequiredItemDisplay> recommendedItems,
		List<com.collectionloghelper.data.GuidanceStep> allSteps,
		java.util.Set<Integer> activityObtainableIds)
	{
		stepProgressView.showStep(current, total, description, isManual,
			requiredItems, recommendedItems, allSteps, activityObtainableIds);
	}

	/** Hides the step progress banner. */
	public void hideStepProgress()
	{
		stepProgressView.hideStep();
	}

	/** Sets callbacks for step advance/skip buttons. */
	public void setStepCallbacks(Runnable advancer, Runnable skipper)
	{
		stepProgressView.setCallbacks(advancer, skipper);
	}

	/**
	 * Sets callbacks for all four step-control buttons: Next Step, Skip, Reset,
	 * and Sync. Any callback may be {@code null}.
	 */
	public void setStepCallbacks(Runnable advancer, Runnable skipper,
		Runnable resetter, Runnable syncer)
	{
		stepProgressView.setCallbacks(advancer, skipper, resetter, syncer);
	}

	/**
	 * Sets callbacks for all five icon-driven step-control buttons (#547):
	 * Next Step, Skip, Stop (deactivate guidance), Restart, and Sync.
	 * Any callback may be {@code null}.
	 */
	public void setStepCallbacks(Runnable advancer, Runnable skipper, Runnable stopper,
		Runnable resetter, Runnable syncer)
	{
		stepProgressView.setCallbacks(advancer, skipper, stopper, resetter, syncer);
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
		selectorControls.updateVisibility(currentMode);
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
		selectorControls.setSelectedCategory(category);
		selectorControls.setSelectedMode(Mode.CATEGORY_FOCUS);
	}

	@Override
	public String getSearchQuery()
	{
		return selectorControls.getSearchQuery();
	}

	@Override
	public EfficientSortMode getEfficientSortMode()
	{
		return selectorControls.getSelectedSortMode();
	}

	@Override
	public CollectionLogCategory getSelectedCategory()
	{
		return selectorControls.getSelectedCategory();
	}

	/**
	 * Returns true when the panel is in a slayer-relevant context: Category Focus
	 * mode with the Slayer category selected. The slayer strategy advisor only
	 * surfaces here, never globally at the top of the panel.
	 */
	private boolean isSlayerContext()
	{
		return currentMode == Mode.CATEGORY_FOCUS
			&& getSelectedCategory() == CollectionLogCategory.SLAYER;
	}
}
