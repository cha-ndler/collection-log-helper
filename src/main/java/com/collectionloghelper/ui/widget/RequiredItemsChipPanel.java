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
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * A horizontal strip of item-icon chips for a guidance step's required items.
 *
 * <p>Each chip is a 28x28 item icon surrounded by a 2-pixel colored border:
 * <ul>
 *   <li><b>Green</b> — item is held in inventory or equipped.</li>
 *   <li><b>White</b> — item was seen in the last bank scan but is not currently
 *       held. The chip tooltip reads "Items can be found in your: Bank"
 *       (Quest Helper convention).</li>
 *   <li><b>Red</b> — item is not held anywhere known to the plugin.</li>
 * </ul>
 * Hovering any chip shows its item name; hovering a white (IN_BANK) chip shows
 * the bank-availability message instead.
 *
 * <p>The panel hides itself when the supplied item list is null or empty,
 * preserving the blank-space contract for steps with no required items.
 *
 * <p>All mutations to Swing state are dispatched on the EDT via
 * {@link SwingUtilities#invokeLater}.
 */
public class RequiredItemsChipPanel extends JPanel
{
	/** Tooltip shown on IN_BANK chips (Quest Helper convention). */
	static final String IN_BANK_TOOLTIP = "Items can be found in your: Bank";

	/** Border width around each chip icon in pixels. */
	private static final int CHIP_BORDER = 2;

	/** Icon size within each chip. */
	private static final int ICON_SIZE = 28;

	/** Total chip dimension: icon + 2 borders on each side. */
	private static final int CHIP_SIZE = ICON_SIZE + CHIP_BORDER * 2;

	private static final Color BG = new Color(25, 35, 55);

	private final ItemManager itemManager;

	/** The horizontal row that holds the heading label and chip labels. */
	private final JPanel chipRow;

	public RequiredItemsChipPanel(ItemManager itemManager)
	{
		this.itemManager = itemManager;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BG);
		setAlignmentX(LEFT_ALIGNMENT);
		setVisible(false);

		chipRow = new JPanel();
		chipRow.setLayout(new BoxLayout(chipRow, BoxLayout.X_AXIS));
		chipRow.setBackground(BG);
		chipRow.setAlignmentX(LEFT_ALIGNMENT);
		add(chipRow);
	}

	/**
	 * Rebuilds the chip strip from the given display rows and sets visibility.
	 * Safe to call from any thread; dispatches to the EDT internally.
	 *
	 * @param rows pre-resolved display rows from
	 *             {@link com.collectionloghelper.guidance.RequiredItemResolver};
	 *             {@code null} or empty hides the panel
	 */
	public void update(List<RequiredItemDisplay> rows)
	{
		final List<RequiredItemDisplay> safeRows =
			(rows != null) ? rows : Collections.<RequiredItemDisplay>emptyList();

		SwingUtilities.invokeLater(() ->
		{
			chipRow.removeAll();

			if (safeRows.isEmpty())
			{
				setVisible(false);
				return;
			}

			// "Items needed:" heading label
			JLabel heading = new JLabel("Items needed:");
			heading.setFont(FontManager.getRunescapeSmallFont());
			heading.setForeground(new Color(180, 180, 180));
			heading.setAlignmentX(LEFT_ALIGNMENT);
			chipRow.add(heading);
			chipRow.add(Box.createHorizontalStrut(6));

			for (RequiredItemDisplay row : safeRows)
			{
				JLabel chip = buildChip(row);
				chipRow.add(chip);
				chipRow.add(Box.createHorizontalStrut(3));
			}

			setVisible(true);
			chipRow.revalidate();
			chipRow.repaint();
			revalidate();
			if (getParent() != null)
			{
				getParent().revalidate();
			}
		});
	}

	/**
	 * Builds a single chip label: a fixed-size icon placeholder surrounded by a
	 * 2-pixel colored border. The icon is loaded asynchronously if the item manager
	 * has an image available; otherwise the chip displays an empty colored square.
	 */
	JLabel buildChip(RequiredItemDisplay row)
	{
		Color borderColor = borderColorFor(row.getStatus());
		String tooltip = tooltipFor(row);

		JLabel chip = new JLabel();
		chip.setPreferredSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setMinimumSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setMaximumSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setHorizontalAlignment(SwingConstants.CENTER);
		chip.setVerticalAlignment(SwingConstants.CENTER);
		chip.setBorder(BorderFactory.createLineBorder(borderColor, CHIP_BORDER));
		chip.setBackground(BG);
		chip.setOpaque(true);
		chip.setToolTipText(tooltip);

		if (row.getItemId() > 0)
		{
			loadChipIcon(row.getItemId(), chip);
		}

		return chip;
	}

	/**
	 * Returns the border color for the given status.
	 * Package-visible for tests.
	 */
	static Color borderColorFor(Status status)
	{
		switch (status)
		{
			case HELD:
				return RequiredItemDisplay.COLOR_HELD;
			case IN_BANK:
				return Color.WHITE;
			case MISSING:
			default:
				return RequiredItemDisplay.COLOR_MISSING;
		}
	}

	/**
	 * Returns the tooltip text for the given display row.
	 * IN_BANK chips use the Quest Helper "Items can be found in your: Bank" convention;
	 * all other chips show the item name.
	 * Package-visible for tests.
	 */
	static String tooltipFor(RequiredItemDisplay row)
	{
		if (row.getStatus() == Status.IN_BANK)
		{
			return IN_BANK_TOOLTIP;
		}
		return row.getName();
	}

	/**
	 * Loads the item sprite for {@code itemId} into {@code chip} asynchronously.
	 * The icon is scaled to {@link #ICON_SIZE} x {@link #ICON_SIZE} before being set.
	 * No-op when the item manager has no image (e.g. in unit tests).
	 */
	void loadChipIcon(int itemId, JLabel chip)
	{
		AsyncBufferedImage asyncImage = itemManager.getImage(itemId);
		if (asyncImage == null)
		{
			return;
		}

		Runnable applyIcon = () ->
		{
			if (asyncImage.getWidth() > 0)
			{
				BufferedImage scaled = scaleImage(asyncImage, ICON_SIZE, ICON_SIZE);
				chip.setIcon(new ImageIcon(scaled));
				chip.revalidate();
				chip.repaint();
			}
		};

		asyncImage.onLoaded(applyIcon);

		// Apply synchronously if already loaded (avoids a one-tick blank flash)
		if (asyncImage.getWidth() > 0)
		{
			applyIcon.run();
		}
	}

	private static BufferedImage scaleImage(BufferedImage source, int width, int height)
	{
		BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(source, 0, 0, width, height, null);
		g.dispose();
		return scaled;
	}
}
