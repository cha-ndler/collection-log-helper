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
import com.collectionloghelper.data.RequirementsChecker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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

	// Clue-guidance banner (purple/blue)
	private final JPanel clueGuidanceBanner;
	private final JLabel clueGuidanceLabel;

	public GuidanceBannerView(RequirementsChecker requirementsChecker)
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

		add(guidanceBannerPanel);

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
	 * Shows the guidance headline for the given source.
	 * Handles {@code null} source by hiding the banner.
	 * Safe to call from any thread.
	 */
	public void showGuidance(CollectionLogSource source)
	{
		if (source == null)
		{
			hideGuidance();
			return;
		}
		// Snapshot before dispatch
		final String sourceName = source.getName();
		final List<String> unmet = requirementsChecker.getUnmetRequirements(sourceName);

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
			guidanceBannerPanel.revalidate();
			if (guidanceBannerPanel.getParent() != null)
			{
				guidanceBannerPanel.getParent().revalidate();
			}
		});
	}

	/**
	 * Hides the active-guidance headline. Safe to call from any thread.
	 */
	public void hideGuidance()
	{
		SwingUtilities.invokeLater(() ->
		{
			guidanceBannerPanel.setVisible(false);
			requirementsWarningLabel.setVisible(false);
			guidanceBannerPanel.revalidate();
			if (guidanceBannerPanel.getParent() != null)
			{
				guidanceBannerPanel.getParent().revalidate();
			}
		});
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
