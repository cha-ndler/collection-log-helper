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

import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerInventoryState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
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
 * the required-items panel (with colour-coded availability borders),
 * and the Next Step / Skip buttons.
 *
 * <p>Constructed once by the shell; updated via {@link #showStep} and
 * hidden via {@link #hide}. Step-advance and skip callbacks are wired
 * via {@link #setCallbacks}.
 */
public class StepProgressView extends JPanel
{
	private static final Color ITEM_STATUS_GREEN = new Color(40, 180, 40);
	private static final Color ITEM_STATUS_YELLOW = new Color(200, 180, 40);
	private static final Color ITEM_STATUS_RED = new Color(200, 40, 40);

	private final ItemManager itemManager;
	private final PlayerInventoryState inventoryState;
	private final PlayerBankState bankState;

	private final JLabel stepProgressLabel;
	private final JPanel requiredItemsPanel;
	private final JButton nextStepButton;
	private final JButton skipStepButton;

	private Runnable stepAdvancer;
	private Runnable stepSkipper;

	public StepProgressView(ItemManager itemManager,
		PlayerInventoryState inventoryState,
		PlayerBankState bankState)
	{
		this.itemManager = itemManager;
		this.inventoryState = inventoryState;
		this.bankState = bankState;

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

		requiredItemsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
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
	 * @param current         one-based step index
	 * @param total           total number of steps
	 * @param description     human-readable step description
	 * @param isManual        whether the Next Step button should be shown
	 * @param requiredItemIds item IDs to display (may be {@code null} or empty)
	 */
	public void showStep(int current, int total, String description, boolean isManual,
		List<Integer> requiredItemIds)
	{
		// Snapshot immutable values before dispatch
		final List<Integer> itemIds = requiredItemIds;

		SwingUtilities.invokeLater(() ->
		{
			stepProgressLabel.setText(
				"<html>Step " + current + "/" + total + ": " + description + "</html>");
			nextStepButton.setVisible(isManual);
			updateRequiredItemDisplay(itemIds);
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
	 */
	public void hide()
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
	 * Updates the required-item icon strip with colour-coded availability borders.
	 * <ul>
	 *   <li>Green — item is in inventory or equipped</li>
	 *   <li>Yellow — item is in the bank but not on the player</li>
	 *   <li>Red — item not found anywhere</li>
	 * </ul>
	 * Must be called on the EDT.
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

			AsyncBufferedImage asyncImage = itemManager.getImage(itemId);
			asyncImage.onLoaded(() ->
			{
				BufferedImage scaled = scaleImage(asyncImage, 28, 28);
				itemLabel.setIcon(new ImageIcon(scaled));
				itemLabel.revalidate();
				itemLabel.repaint();
			});
			BufferedImage scaled = scaleImage(asyncImage, 28, 28);
			itemLabel.setIcon(new ImageIcon(scaled));

			itemLabel.setToolTipText(itemManager.getItemComposition(itemId).getName() + statusText);
			requiredItemsPanel.add(itemLabel);
		}

		requiredItemsPanel.setVisible(true);
		requiredItemsPanel.revalidate();
		requiredItemsPanel.repaint();
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
