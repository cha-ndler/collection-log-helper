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

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.RequirementRow;
import com.collectionloghelper.data.RequirementsChecker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

/**
 * Shared widget that displays active-guidance state banners:
 * <ul>
 *   <li>The green "Guiding: &lt;source&gt;" headline with optional unmet-requirements
 *       warning (shown when guidance is active).</li>
 *   <li>The purple "Clue Guidance" banner (shown when a clue source is guided).</li>
 * </ul>
 *
 * <p>The shell constructs this once and delegates {@link #showGuidance},
 * {@link #hideGuidance}, {@link #showClueGuidance}, and
 * {@link #hideClueGuidance} to it.
 */
public class GuidanceBannerView extends JPanel
{
	private final RequirementsChecker requirementsChecker;

	// Active-guidance banner (green)
	private final JPanel guidanceBannerPanel;
	private final JLabel guidanceBannerLabel;
	private final JLabel requirementsWarningLabel;
	/** E2 — meta-update dating badge; hidden when metaAuthoredDate is null. */
	private final JLabel metaAgeBadgeLabel;

	// Per-source requirements section (quest / skill / diary rows)
	private final RequirementsView requirementsView;

	/**
	 * Source-level Recommended Gear chip strip (#599). Rendered directly below
	 * the per-source requirements section so recommended kit is visible from
	 * step 1, not buried below the active step body. Null when constructed via
	 * the legacy 1-arg constructor (no {@link ItemManager} available — strip is
	 * silently omitted in that path).
	 */
	private final SourceRecommendedItemsChipPanel recommendedChipPanel;

	// Clue-guidance banner (purple/blue)
	private final JPanel clueGuidanceBanner;
	private final JLabel clueGuidanceLabel;

	/**
	 * Legacy 1-arg constructor retained for tests that do not exercise the
	 * source-level Recommended chip strip. New production callers should use
	 * {@link #GuidanceBannerView(RequirementsChecker, ItemManager, ClientThread)}.
	 */
	public GuidanceBannerView(RequirementsChecker requirementsChecker)
	{
		this(requirementsChecker, null, null);
	}

	/**
	 * 2-arg constructor retained for call sites that have {@link ItemManager}
	 * but not {@link ClientThread}. Chip tooltips degrade to {@code "Item <id>"}.
	 */
	public GuidanceBannerView(RequirementsChecker requirementsChecker, ItemManager itemManager)
	{
		this(requirementsChecker, itemManager, null);
	}

	/**
	 * Full constructor. When {@code clientThread} is non-null the recommended-gear
	 * chip tooltips are resolved on the client thread, avoiding the EDT assertion
	 * in {@link net.runelite.client.game.ItemManager#getItemComposition}.
	 */
	public GuidanceBannerView(RequirementsChecker requirementsChecker, ItemManager itemManager,
		ClientThread clientThread)
	{
		this.requirementsChecker = requirementsChecker;

		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// Active guidance banner (shows which source is being guided)
		guidanceBannerPanel = new JPanel();
		guidanceBannerPanel.setLayout(new BoxLayout(guidanceBannerPanel, BoxLayout.Y_AXIS));
		guidanceBannerPanel.setBackground(new Color(25, 50, 25));
		guidanceBannerPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 200, 80), 1),
			BorderFactory.createEmptyBorder(3, 6, 3, 6)
		));
		guidanceBannerPanel.setAlignmentX(CENTER_ALIGNMENT);
		guidanceBannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
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

		// E2 — meta-update dating: shown only when metaAuthoredDate is set
		metaAgeBadgeLabel = new JLabel();
		metaAgeBadgeLabel.setFont(FontManager.getRunescapeSmallFont());
		metaAgeBadgeLabel.setAlignmentX(LEFT_ALIGNMENT);
		metaAgeBadgeLabel.setVisible(false);
		guidanceBannerPanel.add(metaAgeBadgeLabel);

		add(guidanceBannerPanel);

		// Per-source requirements section (hidden by default; shown when source has requirements)
		requirementsView = new RequirementsView();
		requirementsView.setAlignmentX(CENTER_ALIGNMENT);
		requirementsView.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		add(requirementsView);

		// Source-level Recommended chip strip (#599) — rendered directly below
		// the requirements section so it is visible from step 1 of guidance.
		// Hidden by default; populated in showGuidance from
		// CollectionLogSource.getEffectiveRecommendedItemIds().
		if (itemManager != null)
		{
			recommendedChipPanel = new SourceRecommendedItemsChipPanel(itemManager, clientThread);
			recommendedChipPanel.setAlignmentX(LEFT_ALIGNMENT);
			recommendedChipPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
			add(recommendedChipPanel);
		}
		else
		{
			recommendedChipPanel = null;
		}

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
		add(clueGuidanceBanner);
	}

	/**
	 * Shows the guidance headline for the given source and, below it, the
	 * per-source requirements section.
	 *
	 * <p>Handles {@code null} source by hiding the banner.
	 * Safe to call from any thread.
	 *
	 * @param source  the source being guided (may be {@code null})
	 * @param requirementRows pre-built requirement rows produced by
	 *                        {@link com.collectionloghelper.data.RequirementsChecker#buildRequirementRows}
	 *                        on the client thread; may be {@code null} or empty
	 */
	public void showGuidance(CollectionLogSource source, List<RequirementRow> requirementRows)
	{
		if (source == null)
		{
			hideGuidance();
			return;
		}
		// Snapshot before dispatch
		final String sourceName = source.getName();
		final List<String> unmet = requirementsChecker.getUnmetRequirements(sourceName);
		final List<RequirementRow> rows =
			requirementRows != null ? requirementRows : Collections.emptyList();
		// E2 \u2014 compute badge outside EDT (pure computation)
		final MetaAgeBadge badge = MetaAgeBadge.from(source.getMetaAuthoredDate(), LocalDate.now());

		SwingUtilities.invokeLater(() ->
		{
			guidanceBannerLabel.setText("Guiding: " + sourceName);
			guidanceBannerPanel.setVisible(true);

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

			// E2 \u2014 meta-update dating badge
			if (badge != null)
			{
				metaAgeBadgeLabel.setText(badge.getLabel());
				metaAgeBadgeLabel.setForeground(badge.getColor());
				metaAgeBadgeLabel.setToolTipText(badge.getTooltip());
				metaAgeBadgeLabel.setVisible(true);
			}
			else
			{
				metaAgeBadgeLabel.setVisible(false);
			}

			guidanceBannerPanel.revalidate();
			if (guidanceBannerPanel.getParent() != null)
			{
				guidanceBannerPanel.getParent().revalidate();
			}
		});

		// Delegate to RequirementsView \u2014 hides itself when rows is empty
		requirementsView.showRequirements(rows);

		// #599 \u2014 populate source-level Recommended chip strip. Hides itself when
		// the source has no effective recommended items.
		if (recommendedChipPanel != null)
		{
			recommendedChipPanel.update(source.getEffectiveRecommendedItemIds());
		}
	}

	/**
	 * Hides the active-guidance headline and requirements section.
	 * Safe to call from any thread.
	 */
	public void hideGuidance()
	{
		SwingUtilities.invokeLater(() ->
		{
			guidanceBannerPanel.setVisible(false);
			requirementsWarningLabel.setVisible(false);
			metaAgeBadgeLabel.setVisible(false);
			guidanceBannerPanel.revalidate();
			if (guidanceBannerPanel.getParent() != null)
			{
				guidanceBannerPanel.getParent().revalidate();
			}
		});
		requirementsView.hideRequirements();
		if (recommendedChipPanel != null)
		{
			recommendedChipPanel.update(Collections.<Integer>emptyList());
		}
	}

	/**
	 * Shows the clue-guidance banner for the given source.
	 * Safe to call from any thread.
	 */
	public void showClueGuidance(CollectionLogSource source)
	{
		final String name = source != null ? source.getName() : "";
		SwingUtilities.invokeLater(() ->
		{
			clueGuidanceLabel.setText(
				"<html><b>Guidance: " + name + "</b><br>"
				+ "Use the RuneLite <b>Clue Scroll</b> plugin for step-by-step guidance</html>");
			clueGuidanceBanner.setVisible(true);
			clueGuidanceBanner.revalidate();
			if (clueGuidanceBanner.getParent() != null)
			{
				clueGuidanceBanner.getParent().revalidate();
			}
		});
	}

	/**
	 * Hides the clue-guidance banner. Safe to call from any thread.
	 */
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
}
