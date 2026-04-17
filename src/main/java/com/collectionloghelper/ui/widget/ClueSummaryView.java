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
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;

/**
 * Shared widget that displays a summary of unopened clue caskets and
 * other containers found during the last bank scan.
 *
 * <p>Hidden when the bank has not been scanned or when there are no
 * relevant items. Updated via {@link #update(PlayerBankState)}.
 */
public class ClueSummaryView extends JPanel
{
	private static final Color CLUE_SUMMARY_COLOR = new Color(200, 170, 50);

	private final JLabel clueSummaryLabel;

	public ClueSummaryView()
	{
		setOpaque(false);

		clueSummaryLabel = new JLabel("", SwingConstants.CENTER);
		clueSummaryLabel.setFont(FontManager.getRunescapeSmallFont());
		clueSummaryLabel.setForeground(CLUE_SUMMARY_COLOR);
		clueSummaryLabel.setAlignmentX(CENTER_ALIGNMENT);
		clueSummaryLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		clueSummaryLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		clueSummaryLabel.setVisible(false);
		add(clueSummaryLabel);
	}

	/**
	 * Refreshes the clue summary label from the given bank state.
	 * Handles a {@code null} bank state gracefully (hides the label).
	 * Safe to call from any thread.
	 */
	public void updateFromBankState(PlayerBankState bankState)
	{
		// Snapshot before dispatch so the EDT closure captures values, not a reference
		final String casketSummary = bankState != null ? bankState.getCasketSummary() : null;
		final String containerSummary = bankState != null ? bankState.getContainerSummary() : null;

		SwingUtilities.invokeLater(() ->
		{
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
}
