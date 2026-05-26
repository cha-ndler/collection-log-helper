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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.guidance.RequiredItemDisplay;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

/**
 * Shared widget that displays the multi-step guidance progress bar,
 * the required-items chip strip (B.5.1 — {@link RequiredItemsChipPanel}),
 * the collapsible step sections (when any step carries a section label), and the
 * Next Step / Skip buttons.
 *
 * <h3>Required / recommended item text lists</h3>
 * <p>Required-item and recommended-item rows render as a compact, icon-free
 * color-coded text list: each row is a single line showing the item name
 * coloured green (held), gold (in bank - tooltip suffix "in bank"), or red
 * (missing). Rows stack vertically so long lists stay compact at the 225px
 * side-panel width. Within a step, a recommended item that already appears in
 * "Items needed" is filtered out (compared by item id). The legacy icon-chip
 * strip ({@link RequiredItemsChipPanel}) is retained in the component tree but
 * fed an empty list (hidden) so items render only once, as text.
 *
 * <h3>Section rendering (B.5.4)</h3>
 * <p>When the active source has at least one step with a non-null
 * {@link GuidanceStep#getSection()} value, steps are grouped into named
 * collapsible blocks computed by {@link StepSectionGrouper}. Each block
 * shows a header: "▼ {Section Name} (N steps)" when expanded, or
 * "▶ {Section Name} (N steps)" when collapsed. Clicking the header toggles
 * the block. The section containing the active step is always force-expanded.
 * When no step in the source has a section label the flat single-line layout
 * is used unchanged.
 *
 * <p>Required-item rows and recommended-item rows are passed in as pre-resolved
 * {@link RequiredItemDisplay} objects (name + status already determined on the
 * client thread by {@link com.collectionloghelper.guidance.RequiredItemResolver})
 * so this widget never touches inventory/bank state directly.
 *
 * <p>Constructed once by the shell; updated via {@link #showStep} and
 * hidden via {@link #hideStep}. Step-advance and skip callbacks are wired
 * via {@link #setCallbacks}.
 *
 * <p><b>Note:</b> the hide method is named {@code hideStep} rather than
 * {@code hide} to avoid shadowing the deprecated {@link java.awt.Component#hide()}.
 * {@code Component.setVisible(false)} internally dispatches to {@code hide()};
 * an override with the same signature breaks {@code setVisible(false)} for this
 * widget and leaves it visible in pre-guidance states (see issue #353).
 */
public class StepProgressView extends JPanel
{
	/** Tooltip suffix appended to items whose status is IN_BANK. */
	private static final String IN_BANK_TOOLTIP_SUFFIX = " ℹ in bank";

	private static final Color BG = new Color(25, 35, 55);
	private static final Color SECTION_HEADER_FG = new Color(200, 200, 200);
	private static final Color SECTION_HEADER_ACTIVE_FG = new Color(80, 180, 255);

	private final ItemManager itemManager;

	/**
	 * Step-description widget. JTextArea (not JLabel) because Swing's HTML
	 * renderer drops glyphs at pixel-clip wrap boundaries inside long step text
	 * (#575: mid-string truncation, #580 follow-up: char drop at the wrap point).
	 * JTextArea word-wraps natively from the component's actual width with no
	 * pixel arithmetic, so the rendered text always equals the input verbatim.
	 *
	 * <p>Configured to look and behave like the prior label: read-only,
	 * non-focusable, no border, panel-matching background, RuneLite small font.
	 */
	private final JTextArea stepProgressArea;
	/**
	 * B.5.1 chip strip — rendered directly below the step-progress label in the
	 * flat layout. Each chip is a 28x28 item icon with a colored border reflecting
	 * item availability (green/white/red). Hidden when the step has no required items.
	 */
	private final RequiredItemsChipPanel chipPanel;
	/**
	 * B.5.2 chip strip — rendered directly below the required-items chip strip.
	 * Uses a thinner 1-pixel muted-alpha border to signal advisory status.
	 * Hidden when the step has no recommended items.
	 */
	private final RecommendedItemsChipPanel recChipPanel;
	private final JPanel requiredItemsPanel;
	private final JPanel recommendedItemsPanel;
	/** Container rendered when the source uses section labels (sectioned mode). */
	private final JPanel sectionsPanel;
	private final JButton nextStepButton;
	private final JButton skipStepButton;
	private final JButton stopButton;
	private final JButton resetButton;
	private final JButton syncButton;

	/**
	 * Per-section collapse state. True = expanded. New sections default to collapsed
	 * (except for the active-step section which is force-expanded on each render).
	 */
	private final Map<String, Boolean> sectionExpandState = new HashMap<>();

	/**
	 * Last-rendered inputs, used by {@link #showStep} to skip a teardown/rebuild when
	 * the incoming render is identical to the one already on screen (#681).
	 *
	 * <p>Rationale: while guidance is active, {@code GuidanceSequencer.onInventoryChanged}
	 * re-notifies the current (unchanged) step on every inventory change — effectively
	 * every action during activities like Shades of Mort'ton. Each re-notify reaches
	 * {@code showStep}; without this guard the item rows are torn down ({@code removeAll})
	 * and rebuilt every time, making the "Items needed" section visibly flash. When the
	 * full render signature is unchanged we return early and leave Swing untouched.
	 *
	 * <p>These fields are only ever read/written inside the {@code SwingUtilities.invokeLater}
	 * body of {@link #showStep}, i.e. exclusively on the EDT, so no volatile/synchronization
	 * is needed. {@code lastRenderValid} is {@code false} until the first render completes.
	 */
	private boolean lastRenderValid = false;
	private int lastCurrent;
	private int lastTotal;
	private boolean lastIsManual;
	private String lastDescription;
	private List<RequiredItemDisplay> lastRows = Collections.emptyList();
	private List<RequiredItemDisplay> lastRecRows = Collections.emptyList();
	private List<StepSectionGroup> lastGroups = Collections.emptyList();

	private Runnable stepAdvancer;
	private Runnable stepSkipper;
	private Runnable stepStopper;
	private Runnable stepResetter;
	private Runnable stepSyncer;

	public StepProgressView(ItemManager itemManager)
	{
		this.itemManager = itemManager;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 150, 220), 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		setAlignmentX(CENTER_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		setVisible(false);

		stepProgressArea = new JTextArea();
		stepProgressArea.setEditable(false);
		stepProgressArea.setFocusable(false);
		stepProgressArea.setLineWrap(true);
		stepProgressArea.setWrapStyleWord(true);
		stepProgressArea.setOpaque(true);
		stepProgressArea.setBackground(BG);
		stepProgressArea.setForeground(new Color(80, 180, 255));
		stepProgressArea.setFont(FontManager.getRunescapeSmallFont());
		stepProgressArea.setBorder(BorderFactory.createEmptyBorder());
		stepProgressArea.setAlignmentX(LEFT_ALIGNMENT);
		add(stepProgressArea);

		// B.5.1 chip strip — directly below the step description label
		chipPanel = new RequiredItemsChipPanel(itemManager);
		chipPanel.setAlignmentX(LEFT_ALIGNMENT);
		add(chipPanel);

		// B.5.2 chip strip — kept in the layout for backwards compatibility with
		// test helpers that locate components by ordinal position, but permanently
		// hidden. The source-level Recommended chip strip rendered by
		// GuidanceBannerView (#599) replaces this per-step strip so recommended
		// kit is visible from step 1 instead of only when the active step itself
		// carries recommended items.
		recChipPanel = new RecommendedItemsChipPanel(itemManager);
		recChipPanel.setAlignmentX(LEFT_ALIGNMENT);
		recChipPanel.setVisible(false);
		add(recChipPanel);
		add(Box.createVerticalStrut(2));

		requiredItemsPanel = new JPanel();
		requiredItemsPanel.setLayout(new BoxLayout(requiredItemsPanel, BoxLayout.Y_AXIS));
		requiredItemsPanel.setBackground(BG);
		requiredItemsPanel.setAlignmentX(LEFT_ALIGNMENT);
		requiredItemsPanel.setVisible(false);
		add(requiredItemsPanel);

		recommendedItemsPanel = new JPanel();
		recommendedItemsPanel.setLayout(new BoxLayout(recommendedItemsPanel, BoxLayout.Y_AXIS));
		recommendedItemsPanel.setBackground(BG);
		recommendedItemsPanel.setAlignmentX(LEFT_ALIGNMENT);
		recommendedItemsPanel.setVisible(false);
		add(recommendedItemsPanel);

		sectionsPanel = new JPanel();
		sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
		sectionsPanel.setBackground(BG);
		sectionsPanel.setAlignmentX(LEFT_ALIGNMENT);
		sectionsPanel.setVisible(false);
		add(sectionsPanel);

		JPanel stepButtonRow = new JPanel();
		stepButtonRow.setLayout(new BoxLayout(stepButtonRow, BoxLayout.X_AXIS));
		stepButtonRow.setBackground(BG);
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

		// Icon-driven controls (#547). The Reset button used to be labelled "Reset",
		// which during validation read as "stop guidance" when it actually means
		// "restart this task from step 1". The music-player-style glyphs disambiguate:
		//   skip-next   → advance/skip (preserves existing Skip semantics)
		//   stop square → deactivate guidance and clear overlays (NEW)
		//   restart     → restart the current task from step 1
		//   sync        → re-evaluate skip-chain against current collection-log state
		skipStepButton = buildIconButton(
			StepControlIcons.skipNext(ICON_IDLE_COLOR),
			StepControlIcons.skipNext(ICON_HOVER_COLOR),
			"Skip to next step",
			ICON_BUTTON_BG,
			e ->
			{
				if (stepSkipper != null)
				{
					stepSkipper.run();
				}
			});
		stepButtonRow.add(skipStepButton);

		stepButtonRow.add(Box.createHorizontalStrut(4));

		// Stop button — visually distinct (red tint) to emphasize that it terminates
		// guidance and clears all overlays. Resolves the #547 "Reset doesn't stop"
		// surprise by giving deactivation its own affordance.
		stopButton = buildIconButton(
			StepControlIcons.stop(STOP_ICON_IDLE_COLOR),
			StepControlIcons.stop(STOP_ICON_HOVER_COLOR),
			"Stop guidance — clear all overlays",
			STOP_BUTTON_BG,
			e ->
			{
				if (stepStopper != null)
				{
					stepStopper.run();
				}
			});
		stopButton.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(STOP_ICON_IDLE_COLOR, 1),
			BorderFactory.createEmptyBorder(2, 2, 2, 2)
		));
		// buildIconButton() disables border painting for the clean icon-only look used
		// by the other controls. The Stop button needs the red border to render so the
		// destructive affordance is visually distinct, so re-enable border painting here.
		stopButton.setBorderPainted(true);
		stepButtonRow.add(stopButton);

		stepButtonRow.add(Box.createHorizontalStrut(4));

		resetButton = buildIconButton(
			StepControlIcons.restart(ICON_IDLE_COLOR),
			StepControlIcons.restart(ICON_HOVER_COLOR),
			"Restart this task from the first step",
			ICON_BUTTON_BG,
			e ->
			{
				if (stepResetter != null)
				{
					stepResetter.run();
				}
			});
		stepButtonRow.add(resetButton);

		stepButtonRow.add(Box.createHorizontalStrut(4));

		syncButton = buildIconButton(
			StepControlIcons.sync(ICON_IDLE_COLOR),
			StepControlIcons.sync(ICON_HOVER_COLOR),
			"Sync collection-log state",
			ICON_BUTTON_BG,
			e ->
			{
				if (stepSyncer != null)
				{
					stepSyncer.run();
				}
			});
		stepButtonRow.add(syncButton);

		add(Box.createVerticalStrut(3));
		add(stepButtonRow);
	}

	/** Idle-state icon tint for the non-destructive step controls. */
	private static final Color ICON_IDLE_COLOR = new Color(200, 200, 200);
	/** Hover-state icon tint for the non-destructive step controls. */
	private static final Color ICON_HOVER_COLOR = new Color(255, 255, 255);
	/** Background for the non-destructive step-control buttons. */
	private static final Color ICON_BUTTON_BG = new Color(45, 55, 75);
	/** Idle-state icon tint for the stop / deactivate button. */
	private static final Color STOP_ICON_IDLE_COLOR = new Color(220, 110, 110);
	/** Hover-state icon tint for the stop / deactivate button. */
	private static final Color STOP_ICON_HOVER_COLOR = new Color(255, 150, 150);
	/** Background for the stop / deactivate button. */
	private static final Color STOP_BUTTON_BG = new Color(70, 40, 40);
	/** Click-target size for each icon button (≥ 24x24 per the #547 acceptance criteria). */
	private static final Dimension ICON_BUTTON_SIZE = new Dimension(28, 28);

	/**
	 * Builds a square icon-only {@link JButton} with hover icon swap, plain Swing
	 * tooltip, and a 28x28 click target. Used to compose the #547 music-player
	 * style step-control row.
	 */
	private static JButton buildIconButton(BufferedImage idle, BufferedImage hover,
		String tooltip, Color background, java.awt.event.ActionListener listener)
	{
		final ImageIcon idleIcon = new ImageIcon(idle);
		final ImageIcon hoverIcon = new ImageIcon(hover);
		JButton button = new JButton(idleIcon);
		button.setToolTipText(tooltip);
		button.setBackground(background);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setContentAreaFilled(true);
		button.setOpaque(true);
		button.setBorderPainted(false);
		button.setMargin(new java.awt.Insets(0, 0, 0, 0));
		button.setPreferredSize(ICON_BUTTON_SIZE);
		button.setMinimumSize(ICON_BUTTON_SIZE);
		button.setMaximumSize(ICON_BUTTON_SIZE);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addActionListener(listener);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				button.setIcon(hoverIcon);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				button.setIcon(idleIcon);
			}
		});
		return button;
	}

	/**
	 * Sets the step-advance and step-skip callbacks invoked by the buttons.
	 * Either or both may be {@code null}.
	 */
	public void setCallbacks(Runnable advancer, Runnable skipper)
	{
		this.stepAdvancer = advancer;
		this.stepSkipper = skipper;
	}

	/**
	 * Sets all four step-control callbacks: advance, skip, reset, and sync.
	 * Any callback may be {@code null} (the corresponding button click will be a no-op).
	 *
	 * @param advancer  callback for the Next Step button
	 * @param skipper   callback for the Skip button
	 * @param resetter  callback for the Reset button (restart from step 1, no skip-chain)
	 * @param syncer    callback for the Sync button (re-evaluate skip-chain against current state)
	 */
	public void setCallbacks(Runnable advancer, Runnable skipper, Runnable resetter, Runnable syncer)
	{
		this.stepAdvancer = advancer;
		this.stepSkipper = skipper;
		this.stepResetter = resetter;
		this.stepSyncer = syncer;
	}

	/**
	 * Sets all five step-control callbacks introduced in #547: advance, skip,
	 * stop (deactivate guidance), reset, and sync. Any callback may be {@code null}
	 * (the corresponding button click will be a no-op).
	 *
	 * @param advancer  callback for the Next Step button
	 * @param skipper   callback for the Skip icon button
	 * @param stopper   callback for the Stop icon button (deactivate guidance, clear overlays)
	 * @param resetter  callback for the Restart icon button (restart from step 1, no skip-chain)
	 * @param syncer    callback for the Sync icon button (re-evaluate skip-chain against current state)
	 */
	public void setCallbacks(Runnable advancer, Runnable skipper, Runnable stopper,
		Runnable resetter, Runnable syncer)
	{
		this.stepAdvancer = advancer;
		this.stepSkipper = skipper;
		this.stepStopper = stopper;
		this.stepResetter = resetter;
		this.stepSyncer = syncer;
	}

	/**
	 * Shows and populates the step progress panel (flat layout — no section grouping).
	 * Safe to call from any thread.
	 *
	 * @param current       one-based step index
	 * @param total         total number of steps
	 * @param description   human-readable step description
	 * @param isManual      whether the Next Step button should be shown
	 * @param requiredItems pre-resolved display rows (may be {@code null} or empty);
	 *                      resolved on the client thread by
	 *                      {@link com.collectionloghelper.guidance.RequiredItemResolver}
	 */
	public void showStep(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems)
	{
		showStep(current, total, description, isManual, requiredItems,
			Collections.<RequiredItemDisplay>emptyList(), Collections.<GuidanceStep>emptyList());
	}

	/**
	 * Shows and populates the step progress panel, rendering collapsible section blocks
	 * when {@code allSteps} contains at least one step with a non-null section label.
	 * Safe to call from any thread.
	 *
	 * @param current            one-based step index
	 * @param total              total number of steps
	 * @param description        human-readable step description
	 * @param isManual           whether the Next Step button should be shown
	 * @param requiredItems      pre-resolved required-item display rows (may be {@code null}
	 *                           or empty)
	 * @param allSteps           full ordered step list for the active source; used to compute
	 *                           section groups. Pass an empty list to force flat layout.
	 */
	public void showStep(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems, List<GuidanceStep> allSteps)
	{
		showStep(current, total, description, isManual, requiredItems,
			Collections.<RequiredItemDisplay>emptyList(), allSteps);
	}

	/**
	 * Shows and populates the step progress panel, rendering both the required-items and
	 * recommended-items subsections, and collapsible section blocks when {@code allSteps}
	 * contains at least one step with a non-null section label.
	 * Safe to call from any thread.
	 *
	 * @param current            one-based step index
	 * @param total              total number of steps
	 * @param description        human-readable step description
	 * @param isManual           whether the Next Step button should be shown
	 * @param requiredItems      pre-resolved required-item display rows (may be {@code null}
	 *                           or empty)
	 * @param recommendedItems   pre-resolved recommended-item display rows (may be
	 *                           {@code null} or empty); hidden when empty
	 * @param allSteps           full ordered step list for the active source; used to
	 *                           compute section groups. Pass an empty list to force flat
	 *                           layout.
	 */
	public void showStep(int current, int total, String description, boolean isManual,
		List<RequiredItemDisplay> requiredItems, List<RequiredItemDisplay> recommendedItems,
		List<GuidanceStep> allSteps)
	{
		final List<RequiredItemDisplay> rows =
			requiredItems != null ? requiredItems : Collections.emptyList();
		final List<RequiredItemDisplay> recRows =
			recommendedItems != null ? recommendedItems : Collections.emptyList();
		final List<GuidanceStep> steps =
			allSteps != null ? allSteps : Collections.emptyList();
		final List<StepSectionGroup> groups = StepSectionGrouper.group(steps);

		SwingUtilities.invokeLater(() ->
		{
			// Idempotency guard (#681): when the full render signature is identical to
			// what is already on screen, skip the teardown/rebuild entirely. This makes a
			// same-step re-notify (fired on every inventory change while guidance is active)
			// a no-op instead of a visible flash. Section grouping is compared via the
			// computed StepSectionGroup list, which carries Lombok-generated equals/hashCode,
			// so List.equals gives a true content comparison. Runs on the EDT only.
			if (lastRenderValid
				&& lastCurrent == current
				&& lastTotal == total
				&& lastIsManual == isManual
				&& java.util.Objects.equals(lastDescription, description)
				&& lastRows.equals(rows)
				&& lastRecRows.equals(recRows)
				&& lastGroups.equals(groups))
			{
				return;
			}
			lastRenderValid = true;
			lastCurrent = current;
			lastTotal = total;
			lastIsManual = isManual;
			lastDescription = description;
			lastRows = new java.util.ArrayList<>(rows);
			lastRecRows = new java.util.ArrayList<>(recRows);
			lastGroups = new java.util.ArrayList<>(groups);

			// Plain-text setText on a JTextArea word-wraps from the actual rendered
			// width — no HTML, no pixel-width budget, no escaping needed. Previous
			// JLabel + <html><body width=...> approach dropped glyphs at the wrap
			// boundary when the rendered width exceeded the panel's clip region
			// (#575: "Enter the Bandos boss room and Graardor" missing "kill
			// General"; #580 follow-up dropped "Kill" → "K" on the next attempt).
			stepProgressArea.setText("Step " + current + "/" + total + ": " + description);
			nextStepButton.setVisible(isManual);

			if (groups.isEmpty())
			{
				// Flat layout - hide sections, show required/recommended items as compact
				// text lists. The icon chip strip (chipPanel) is intentionally fed an empty
				// list so it hides: the text list is now the single display for required
				// items (no double-render of icons + text). recChipPanel is likewise unused
				// (it was hoisted to source-level, #599).
				sectionsPanel.setVisible(false);
				chipPanel.update(Collections.<RequiredItemDisplay>emptyList());
				updateRequiredItemDisplay(rows);
				updateRecommendedItemDisplay(dedupRecommended(rows, recRows));
			}
			else
			{
				// Sectioned layout — hide inline panels (including chip strip), render section blocks.
				// recChipPanel is intentionally not updated; see #599.
				chipPanel.update(Collections.<RequiredItemDisplay>emptyList());
				requiredItemsPanel.removeAll();
				requiredItemsPanel.setVisible(false);
				recommendedItemsPanel.removeAll();
				recommendedItemsPanel.setVisible(false);
				updateSectionDisplay(groups, current, rows, recRows);
			}

			setVisible(true);
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});
	}

	/**
	 * Hides the step progress panel and clears required items.
	 * Safe to call from any thread.
	 *
	 * <p>Intentionally named {@code hideStep} (not {@code hide}) — see class
	 * Javadoc and issue #353 for why overriding {@link java.awt.Component#hide()}
	 * would break {@link #setVisible}.
	 */
	public void hideStep()
	{
		SwingUtilities.invokeLater(() ->
		{
			// Invalidate the #681 idempotency cache: after a hide the panels are torn
			// down, so the next showStep must rebuild even if its inputs match the
			// last shown step.
			lastRenderValid = false;
			chipPanel.update(Collections.<RequiredItemDisplay>emptyList());
			recChipPanel.update(Collections.<RequiredItemDisplay>emptyList());
			requiredItemsPanel.removeAll();
			requiredItemsPanel.setVisible(false);
			recommendedItemsPanel.removeAll();
			recommendedItemsPanel.setVisible(false);
			sectionsPanel.removeAll();
			sectionsPanel.setVisible(false);
			setVisible(false);
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});
	}

	// ── Section rendering ────────────────────────────────────────────────────

	/**
	 * Rebuilds the collapsible section blocks.
	 *
	 * <p>The section containing {@code activeStepIndex} is force-expanded and its
	 * required-item and recommended-item rows are rendered immediately below the step
	 * label. Other sections respect their stored collapse state (defaulting to
	 * collapsed).
	 *
	 * <p>Must be called on the EDT.
	 *
	 * @param groups           ordered section groups from {@link StepSectionGrouper#group}
	 * @param activeStepIndex  1-based index of the currently active step
	 * @param requiredItems    required-item display rows for the active step
	 * @param recommendedItems recommended-item display rows for the active step
	 */
	void updateSectionDisplay(List<StepSectionGroup> groups, int activeStepIndex,
		List<RequiredItemDisplay> requiredItems, List<RequiredItemDisplay> recommendedItems)
	{
		sectionsPanel.removeAll();

		String activeSectionName = StepSectionGrouper.sectionNameFor(groups, activeStepIndex);

		// Force-expand the active section
		if (activeSectionName != null)
		{
			sectionExpandState.put(activeSectionName, Boolean.TRUE);
		}

		for (StepSectionGroup group : groups)
		{
			boolean isActiveSection = group.getName().equals(activeSectionName);
			boolean expanded = sectionExpandState.getOrDefault(group.getName(), Boolean.FALSE);

			JPanel block = buildSectionBlock(group, isActiveSection, expanded,
				activeStepIndex, requiredItems, recommendedItems);
			block.setAlignmentX(LEFT_ALIGNMENT);
			sectionsPanel.add(block);
			sectionsPanel.add(Box.createVerticalStrut(2));
		}

		sectionsPanel.setVisible(true);
		sectionsPanel.revalidate();
		sectionsPanel.repaint();
	}

	/**
	 * Overload retained for backwards compatibility with existing test callers that
	 * do not yet pass recommended items.
	 */
	void updateSectionDisplay(List<StepSectionGroup> groups, int activeStepIndex,
		List<RequiredItemDisplay> requiredItems)
	{
		updateSectionDisplay(groups, activeStepIndex, requiredItems, Collections.emptyList());
	}

	/**
	 * Builds a single collapsible section block: a clickable header row + a body
	 * panel containing the step indices (and required-item + recommended-item rows
	 * for the active step when expanded).
	 *
	 * <p>Must be called on the EDT.
	 */
	private JPanel buildSectionBlock(StepSectionGroup group, boolean isActiveSection,
		boolean expanded, int activeStepIndex, List<RequiredItemDisplay> requiredItems,
		List<RequiredItemDisplay> recommendedItems)
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setBackground(BG);
		block.setAlignmentX(LEFT_ALIGNMENT);
		block.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		// Body panel (shown when expanded)
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(new Color(20, 28, 45));
		body.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 0));
		body.setAlignmentX(LEFT_ALIGNMENT);
		body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		for (int stepIdx : group.getStepIndices())
		{
			boolean isActive = stepIdx == activeStepIndex;
			Color stepFg = isActive ? SECTION_HEADER_ACTIVE_FG : new Color(160, 160, 160);

			JLabel stepLabel = new JLabel("• Step " + stepIdx);
			stepLabel.setFont(FontManager.getRunescapeSmallFont());
			stepLabel.setForeground(stepFg);
			stepLabel.setAlignmentX(LEFT_ALIGNMENT);
			body.add(stepLabel);

			if (isActive && requiredItems != null && !requiredItems.isEmpty())
			{
				JPanel itemsInSection = new JPanel();
				itemsInSection.setLayout(new BoxLayout(itemsInSection, BoxLayout.Y_AXIS));
				itemsInSection.setBackground(new Color(20, 28, 45));
				itemsInSection.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				itemsInSection.setAlignmentX(LEFT_ALIGNMENT);
				itemsInSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

				JLabel neededLabel = new JLabel("Items needed:");
				neededLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
				neededLabel.setForeground(new Color(180, 180, 180));
				neededLabel.setAlignmentX(LEFT_ALIGNMENT);
				itemsInSection.add(neededLabel);
				itemsInSection.add(Box.createVerticalStrut(2));

				for (RequiredItemDisplay row : requiredItems)
				{
					JPanel rowPanel = buildItemRow(row);
					rowPanel.setAlignmentX(LEFT_ALIGNMENT);
					itemsInSection.add(rowPanel);
					itemsInSection.add(Box.createVerticalStrut(2));
				}

				body.add(itemsInSection);
			}

			List<RequiredItemDisplay> recForStep =
				isActive ? dedupRecommended(requiredItems, recommendedItems) : Collections.emptyList();
			if (isActive && !recForStep.isEmpty())
			{
				JPanel recInSection = new JPanel();
				recInSection.setLayout(new BoxLayout(recInSection, BoxLayout.Y_AXIS));
				recInSection.setBackground(new Color(20, 28, 45));
				recInSection.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
				recInSection.setAlignmentX(LEFT_ALIGNMENT);
				recInSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

				JLabel recLabel = new JLabel("Recommended:");
				recLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
				recLabel.setForeground(new Color(180, 180, 180));
				recLabel.setAlignmentX(LEFT_ALIGNMENT);
				recInSection.add(recLabel);
				recInSection.add(Box.createVerticalStrut(2));

				for (RequiredItemDisplay row : recForStep)
				{
					JPanel rowPanel = buildItemRow(row);
					rowPanel.setAlignmentX(LEFT_ALIGNMENT);
					recInSection.add(rowPanel);
					recInSection.add(Box.createVerticalStrut(2));
				}

				body.add(recInSection);
			}
		}

		body.setVisible(expanded);

		// Header button
		int stepCount = group.getStepIndices().size();
		String headerText = buildHeaderText(group.getName(), stepCount, expanded);

		JButton header = new JButton(headerText);
		header.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		header.setForeground(isActiveSection ? SECTION_HEADER_ACTIVE_FG : SECTION_HEADER_FG);
		header.setBackground(new Color(35, 45, 65));
		header.setBorderPainted(false);
		header.setFocusPainted(false);
		header.setContentAreaFilled(true);
		header.setHorizontalAlignment(SwingConstants.LEFT);
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		final String groupName = group.getName();
		header.addActionListener(e ->
		{
			boolean nowExpanded = !body.isVisible();
			sectionExpandState.put(groupName, nowExpanded);
			body.setVisible(nowExpanded);
			header.setText(buildHeaderText(groupName, stepCount, nowExpanded));
			sectionsPanel.revalidate();
			sectionsPanel.repaint();
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});

		block.add(header);
		block.add(body);
		return block;
	}

	/** Formats the section header text with the expand/collapse arrow glyph. */
	private static String buildHeaderText(String sectionName, int stepCount, boolean isExpanded)
	{
		String arrow = isExpanded ? "▼ " : "▶ ";
		return arrow + sectionName + " (" + stepCount + " step" + (stepCount == 1 ? "" : "s") + ")";
	}

	/**
	 * Filters {@code recommended} so it excludes any item already present in
	 * {@code required} (compared by item id). Within a single step an item that is
	 * already in "Items needed" must not be repeated under "Recommended".
	 * Order of the surviving recommended rows is preserved.
	 */
	private static List<RequiredItemDisplay> dedupRecommended(
		List<RequiredItemDisplay> required, List<RequiredItemDisplay> recommended)
	{
		if (recommended == null || recommended.isEmpty()
			|| required == null || required.isEmpty())
		{
			return recommended != null ? recommended : Collections.emptyList();
		}

		java.util.Set<Integer> requiredIds = new java.util.HashSet<>();
		for (RequiredItemDisplay row : required)
		{
			requiredIds.add(row.getItemId());
		}

		List<RequiredItemDisplay> filtered = new java.util.ArrayList<>(recommended.size());
		for (RequiredItemDisplay row : recommended)
		{
			if (!requiredIds.contains(row.getItemId()))
			{
				filtered.add(row);
			}
		}
		return filtered;
	}

	// ── Flat required/recommended-items display ─────────────────────────────

	/**
	 * Rebuilds the required-items subsection from pre-resolved display rows.
	 *
	 * <p>Each row is a single-line, icon-free text label coloured by availability:
	 * <ul>
	 *   <li><b>Green</b> - held in inventory or equipped</li>
	 *   <li><b>Gold</b> (+ tooltip suffix "in bank") - present in last bank scan</li>
	 *   <li><b>Red</b> - not found anywhere</li>
	 * </ul>
	 * The subsection is hidden when {@code rows} is empty.
	 * Must be called on the EDT.
	 *
	 * @param rows pre-resolved display rows from
	 *             {@link com.collectionloghelper.guidance.RequiredItemResolver}
	 */
	void updateRequiredItemDisplay(List<RequiredItemDisplay> rows)
	{
		updateItemSubsection(requiredItemsPanel, "Items needed:", rows);
	}

	/**
	 * Rebuilds the recommended-items subsection from pre-resolved display rows.
	 * Uses the same row layout as {@link #updateRequiredItemDisplay} so colouring
	 * (green/gold/red) is identical.
	 * The subsection is hidden when {@code rows} is empty.
	 * Must be called on the EDT.
	 *
	 * @param rows pre-resolved display rows from
	 *             {@link com.collectionloghelper.guidance.RequiredItemResolver#resolveRecommended}
	 */
	void updateRecommendedItemDisplay(List<RequiredItemDisplay> rows)
	{
		updateItemSubsection(recommendedItemsPanel, "Recommended:", rows);
	}

	/**
	 * Shared implementation for both the required-items and recommended-items
	 * subsections. Clears {@code panel}, populates it with a header label and one
	 * row per entry, then sets its visibility based on whether {@code rows} is empty.
	 * Must be called on the EDT.
	 */
	private void updateItemSubsection(JPanel panel, String headerText,
		List<RequiredItemDisplay> rows)
	{
		panel.removeAll();

		if (rows == null || rows.isEmpty())
		{
			panel.setVisible(false);
			return;
		}

		JLabel headerLabel = new JLabel(headerText);
		headerLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		headerLabel.setForeground(new Color(180, 180, 180));
		headerLabel.setAlignmentX(LEFT_ALIGNMENT);
		panel.add(headerLabel);

		panel.add(Box.createVerticalStrut(2));

		for (RequiredItemDisplay row : rows)
		{
			JPanel rowPanel = buildItemRow(row);
			rowPanel.setAlignmentX(LEFT_ALIGNMENT);
			panel.add(rowPanel);
			panel.add(Box.createVerticalStrut(2));
		}

		panel.setVisible(true);
		panel.revalidate();
		panel.repaint();
	}

	/**
	 * Builds a single item row: a single-line name label coloured by the row's
	 * availability status. No icon is rendered (the compact text list keeps long
	 * item lists from overflowing the 225px side panel).
	 *
	 * <ul>
	 *   <li><b>Green</b> ({@link RequiredItemDisplay#COLOR_HELD}) - held in inventory
	 *       or equipped</li>
	 *   <li><b>Gold</b> ({@link RequiredItemDisplay#COLOR_IN_BANK}) (+ tooltip suffix
	 *       "in bank") - present in last bank scan</li>
	 *   <li><b>Red</b> ({@link RequiredItemDisplay#COLOR_MISSING}) - not found
	 *       anywhere</li>
	 * </ul>
	 */
	private JPanel buildItemRow(RequiredItemDisplay row)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setBackground(BG);
		rowPanel.setAlignmentX(LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));

		Color nameColor;
		String tooltipSuffix;
		switch (row.getStatus())
		{
			case HELD:
				nameColor = RequiredItemDisplay.COLOR_HELD;
				tooltipSuffix = "";
				break;
			case IN_BANK:
				nameColor = RequiredItemDisplay.COLOR_IN_BANK;
				tooltipSuffix = IN_BANK_TOOLTIP_SUFFIX;
				break;
			case MISSING:
			default:
				nameColor = RequiredItemDisplay.COLOR_MISSING;
				tooltipSuffix = "";
				break;
		}

		JLabel nameLabel = new JLabel(row.getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(nameColor);
		nameLabel.setToolTipText(row.getName() + tooltipSuffix);
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);

		rowPanel.add(nameLabel);

		return rowPanel;
	}
}
