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

import com.collectionloghelper.data.SlayerCreatureDatabase;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Shared widget that displays the current slayer task indicator and the
 * expandable slayer strategy advisor panel.
 *
 * <p>The task label shows "Slayer: &lt;creature&gt; (&lt;N&gt; remaining)".
 * The strategy panel is collapsible; when expanded it shows the recommended
 * Slayer master, the current task assessment, and the recommended block list.
 *
 * <p>Call {@link #refresh()} after any change to {@link SlayerTaskState} or
 * {@link SlayerStrategyCalculator} output.
 */
public class SlayerStrategyView extends JPanel
{
	private static final Color SLAYER_TASK_COLOR = new Color(180, 80, 220);

	private final SlayerTaskState slayerTaskState;
	private final SlayerStrategyCalculator slayerStrategyCalculator;

	private final JLabel slayerTaskLabel;
	private final JPanel slayerStrategyPanel;
	private final JButton strategyToggle;
	private final JLabel slayerStrategyLabel;
	private boolean expanded = false;

	public SlayerStrategyView(SlayerTaskState slayerTaskState,
		SlayerStrategyCalculator slayerStrategyCalculator)
	{
		this.slayerTaskState = slayerTaskState;
		this.slayerStrategyCalculator = slayerStrategyCalculator;

		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// Slayer task indicator
		slayerTaskLabel = new JLabel("", SwingConstants.CENTER);
		slayerTaskLabel.setFont(FontManager.getRunescapeSmallFont());
		slayerTaskLabel.setForeground(SLAYER_TASK_COLOR);
		slayerTaskLabel.setAlignmentX(CENTER_ALIGNMENT);
		slayerTaskLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		slayerTaskLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		slayerTaskLabel.setVisible(false);
		add(slayerTaskLabel);

		// Slayer strategy advisor panel (expandable)
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

		strategyToggle = new JButton("\u25B6 Slayer Strategy");
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
			expanded = !expanded;
			strategyToggle.setText((expanded ? "\u25BC " : "\u25B6 ") + "Slayer Strategy");
			updateStrategyContent();
		});
		slayerStrategyPanel.add(strategyToggle);

		slayerStrategyLabel = new JLabel();
		slayerStrategyLabel.setFont(FontManager.getRunescapeSmallFont());
		slayerStrategyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		slayerStrategyLabel.setAlignmentX(LEFT_ALIGNMENT);
		slayerStrategyLabel.setVisible(false);
		slayerStrategyPanel.add(slayerStrategyLabel);

		add(slayerStrategyPanel);
	}

	/**
	 * Re-reads {@link SlayerTaskState} and {@link SlayerStrategyCalculator} and
	 * updates both the task label and strategy panel accordingly.
	 * Must be called on the EDT (invoked from the shell's rebuild path).
	 */
	public void refresh()
	{
		if (slayerTaskState.isTaskActive())
		{
			final String creature = slayerTaskState.getCreatureName();
			final int remaining = slayerTaskState.getRemaining();
			slayerTaskLabel.setText("Slayer: " + creature + " (" + remaining + " remaining)");
			slayerTaskLabel.setVisible(true);
			slayerStrategyPanel.setVisible(true);
		}
		else
		{
			slayerTaskLabel.setVisible(false);
			slayerStrategyPanel.setVisible(false);
		}
		updateStrategyContent();
	}

	private void updateStrategyContent()
	{
		if (!slayerStrategyPanel.isVisible())
		{
			slayerStrategyLabel.setVisible(false);
			slayerStrategyPanel.revalidate();
			return;
		}

		String recommended = slayerStrategyCalculator.getRecommendedMaster();

		if (!expanded)
		{
			if (recommended != null)
			{
				slayerStrategyLabel.setText(
					"<html><font color='#b5b5b3'>Best: " + recommended + "</font></html>");
				slayerStrategyLabel.setVisible(true);
			}
			else
			{
				slayerStrategyLabel.setVisible(false);
			}
			slayerStrategyPanel.revalidate();
			return;
		}

		StringBuilder sb = new StringBuilder("<html>");

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

		if (recommended != null)
		{
			sb.append("<b>Best master:</b> ").append(recommended).append("<br>");
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
}
