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

import com.collectionloghelper.data.RequirementRow;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;

/**
 * Displays per-source quest, skill, and diary requirements in the guidance panel
 * header area — below the "Guiding: &lt;source&gt;" headline and above the step strip.
 *
 * <p>Each requirement is one text row coloured by met/unmet state:
 * <ul>
 *   <li><b>Quest</b> — GREEN (COMPLETED), YELLOW (IN PROGRESS), RED (NOT STARTED)</li>
 *   <li><b>Skill</b> — GREEN (level met), RED (level not met)</li>
 *   <li><b>Diary</b> — GREEN (COMPLETED), RED (NOT COMPLETED)</li>
 * </ul>
 *
 * <p>The entire panel is hidden when the source has no requirements (no empty header).
 *
 * <p><b>Note:</b> The public hide method is named {@code hideRequirements} (not {@code hide})
 * to avoid shadowing the deprecated {@link java.awt.Component#hide()} — see issue #353.
 */
public class RequirementsView extends JPanel
{
	private static final Color BACKGROUND = new Color(20, 30, 20);
	private static final Color BORDER_COLOR = new Color(60, 120, 60);
	private static final Color HEADER_COLOR = new Color(160, 160, 160);

	RequirementsView()
	{
		setOpaque(true);
		setBackground(BACKGROUND);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(BORDER_COLOR, 1),
			BorderFactory.createEmptyBorder(3, 6, 3, 6)
		));
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		setVisible(false);
	}

	/**
	 * Populates and shows the requirements section from pre-built display rows.
	 * Safe to call from any thread.
	 *
	 * @param rows display rows produced by
	 *             {@link com.collectionloghelper.data.RequirementsChecker#buildRequirementRows}
	 *             on the client thread; may be {@code null} or empty (section will hide)
	 */
	public void showRequirements(List<RequirementRow> rows)
	{
		final List<RequirementRow> safeRows = (rows != null) ? rows : Collections.emptyList();
		SwingUtilities.invokeLater(() -> updateRows(safeRows));
	}

	/**
	 * Hides the requirements section and clears all rows.
	 * Safe to call from any thread.
	 *
	 * <p>Intentionally named {@code hideRequirements} (not {@code hide}) — see class
	 * Javadoc and issue #353 for why overriding {@link java.awt.Component#hide()}
	 * would break {@link #setVisible}.
	 */
	public void hideRequirements()
	{
		SwingUtilities.invokeLater(() ->
		{
			removeAll();
			setVisible(false);
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});
	}

	/**
	 * Rebuilds requirement rows and toggles visibility. Must be called on the EDT.
	 */
	void updateRows(List<RequirementRow> rows)
	{
		removeAll();

		if (rows.isEmpty())
		{
			setVisible(false);
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
			return;
		}

		JLabel header = new JLabel("Requirements:");
		header.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		header.setForeground(HEADER_COLOR);
		header.setAlignmentX(LEFT_ALIGNMENT);
		add(header);
		add(Box.createVerticalStrut(2));

		RequirementRow.Category lastCategory = null;
		for (RequirementRow row : rows)
		{
			// Insert a small gap between category groups
			if (lastCategory != null && lastCategory != row.getCategory())
			{
				add(Box.createVerticalStrut(1));
			}
			lastCategory = row.getCategory();

			JLabel rowLabel = buildRowLabel(row);
			rowLabel.setAlignmentX(LEFT_ALIGNMENT);
			add(rowLabel);
		}

		setVisible(true);
		revalidate();
		repaint();
		if (getParent() != null)
		{
			getParent().revalidate();
		}
	}

	/**
	 * Builds a single-line label: "{label} — {stateText}", coloured by the row's state.
	 */
	private JLabel buildRowLabel(RequirementRow row)
	{
		String text = row.getLabel() + " — " + row.getStateText();
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(row.getColor());
		label.setToolTipText(text);
		return label;
	}
}
