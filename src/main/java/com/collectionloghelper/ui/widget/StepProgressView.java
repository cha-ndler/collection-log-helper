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

import com.collectionloghelper.guidance.RequiredItemDisplay;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Shared widget that displays the multi-step guidance progress bar,
 * the required-items subsection (with colour-coded availability), and the
 * Next Step / Skip buttons.
 *
 * <p>Required-item rows are passed in as pre-resolved {@link RequiredItemDisplay}
 * objects (name + status already determined on the client thread by
 * {@link com.collectionloghelper.guidance.RequiredItemResolver}) so this widget
 * never touches inventory/bank state directly.
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

	private final ItemManager itemManager;

	private final JLabel stepProgressLabel;
	private final JPanel requiredItemsPanel;
	private final JButton nextStepButton;
	private final JButton skipStepButton;

	private Runnable stepAdvancer;
	private Runnable stepSkipper;

	public StepProgressView(ItemManager itemManager)
	{
		this.itemManager = itemManager;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(new Color(25, 35, 55));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 150, 220), 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)
		));
		setAlignmentX(CENTER_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		setVisible(false);

		stepProgressLabel = new JLabel();
		stepProgressLabel.setFont(FontManager.getRunescapeSmallFont());
		stepProgressLabel.setForeground(new Color(80, 180, 255));
		stepProgressLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(stepProgressLabel);

		requiredItemsPanel = new JPanel();
		requiredItemsPanel.setLayout(new BoxLayout(requiredItemsPanel, BoxLayout.Y_AXIS));
		requiredItemsPanel.setBackground(new Color(25, 35, 55));
		requiredItemsPanel.setAlignmentX(LEFT_ALIGNMENT);
		requiredItemsPanel.setVisible(false);
		add(requiredItemsPanel);

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

		add(Box.createVerticalStrut(3));
		add(stepButtonRow);
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
	 * Shows and populates the step progress panel.
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
		final List<RequiredItemDisplay> rows =
			requiredItems != null ? requiredItems : Collections.emptyList();

		SwingUtilities.invokeLater(() ->
		{
			stepProgressLabel.setText(
				"<html>Step " + current + "/" + total + ": " + description + "</html>");
			nextStepButton.setVisible(isManual);
			updateRequiredItemDisplay(rows);
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
			requiredItemsPanel.removeAll();
			requiredItemsPanel.setVisible(false);
			setVisible(false);
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});
	}

	/**
	 * Rebuilds the required-items subsection from pre-resolved display rows.
	 *
	 * <p>Each row shows a 28×28 item sprite icon followed by the item name.
	 * The name label is coloured by availability:
	 * <ul>
	 *   <li><b>Green</b> — held in inventory or equipped</li>
	 *   <li><b>White</b> (+ tooltip suffix "ℹ in bank") — present in last bank scan</li>
	 *   <li><b>Red</b> — not found anywhere</li>
	 * </ul>
	 * The subsection is hidden when {@code rows} is empty.
	 * Must be called on the EDT.
	 *
	 * @param rows pre-resolved display rows from
	 *             {@link com.collectionloghelper.guidance.RequiredItemResolver}
	 */
	void updateRequiredItemDisplay(List<RequiredItemDisplay> rows)
	{
		requiredItemsPanel.removeAll();

		if (rows == null || rows.isEmpty())
		{
			requiredItemsPanel.setVisible(false);
			return;
		}

		JLabel headerLabel = new JLabel("Items needed:");
		headerLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		headerLabel.setForeground(new Color(180, 180, 180));
		headerLabel.setAlignmentX(LEFT_ALIGNMENT);
		requiredItemsPanel.add(headerLabel);

		requiredItemsPanel.add(Box.createVerticalStrut(2));

		for (RequiredItemDisplay row : rows)
		{
			JPanel rowPanel = buildItemRow(row);
			rowPanel.setAlignmentX(LEFT_ALIGNMENT);
			requiredItemsPanel.add(rowPanel);
			requiredItemsPanel.add(Box.createVerticalStrut(2));
		}

		requiredItemsPanel.setVisible(true);
		requiredItemsPanel.revalidate();
		requiredItemsPanel.repaint();
	}

	/**
	 * Builds a single item row: a 28×28 sprite icon + a name label coloured
	 * by the row's availability status.
	 */
	private JPanel buildItemRow(RequiredItemDisplay row)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setBackground(new Color(25, 35, 55));
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

		// --- Icon ---
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(28, 28));
		iconLabel.setMinimumSize(new Dimension(28, 28));
		iconLabel.setMaximumSize(new Dimension(28, 28));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		iconLabel.setVerticalAlignment(SwingConstants.CENTER);

		// --- Name label ---
		Color nameColor;
		String tooltipSuffix;
		switch (row.getStatus())
		{
			case HELD:
				nameColor = RequiredItemDisplay.COLOR_HELD;
				tooltipSuffix = "";
				break;
			case IN_BANK:
				nameColor = Color.WHITE;
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
		iconLabel.setToolTipText(row.getName() + tooltipSuffix);

		rowPanel.add(iconLabel);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(nameLabel);

		// Load sprite asynchronously; itemManager.getImage is safe to call on the EDT.
		if (row.getItemId() > 0)
		{
			loadItemIcon(row.getItemId(), iconLabel);
		}

		return rowPanel;
	}

	/**
	 * Loads the item sprite for the given item ID into {@code iconLabel} asynchronously.
	 * Called after the row panel is constructed so the icon appears as soon as the
	 * image is available. Safe to call when {@code itemManager} has no image for the
	 * given ID (e.g., in tests) — the icon label is simply left empty.
	 */
	void loadItemIcon(int itemId, JLabel iconLabel)
	{
		AsyncBufferedImage asyncImage = itemManager.getImage(itemId);
		if (asyncImage == null)
		{
			return;
		}
		asyncImage.onLoaded(() ->
		{
			if (asyncImage.getWidth() > 0)
			{
				BufferedImage scaled = scaleImage(asyncImage, 28, 28);
				iconLabel.setIcon(new ImageIcon(scaled));
				iconLabel.revalidate();
				iconLabel.repaint();
			}
		});
		// Apply synchronously in case the image is already loaded
		if (asyncImage.getWidth() > 0)
		{
			BufferedImage scaled = scaleImage(asyncImage, 28, 28);
			iconLabel.setIcon(new ImageIcon(scaled));
		}
	}

	private static BufferedImage scaleImage(BufferedImage source, int width, int height)
	{
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.drawImage(source, 0, 0, width, height, null);
		g.dispose();
		return scaled;
	}
}
