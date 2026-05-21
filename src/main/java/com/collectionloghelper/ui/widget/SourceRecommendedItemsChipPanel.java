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
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * A horizontal strip of item-icon chips rendered in the source-detail header
 * for the source-level "Recommended Gear" list (#573).
 *
 * <p>This differs from {@link RecommendedItemsChipPanel} in three ways:
 * <ul>
 *   <li>Renders in the source header (above the step list) so players see the
 *       source's recommended kit <em>before</em> activating guidance, not just
 *       during an active step.</li>
 *   <li>No availability colouring — guidance is not yet active, so live
 *       inventory/bank status is irrelevant at this point. The chip border uses
 *       a single muted neutral colour.</li>
 *   <li>The data source is {@link com.collectionloghelper.data.CollectionLogSource#getEffectiveRecommendedItemIds()}
 *       which rolls up the per-step recommended items by default and accepts an
 *       optional source-level override.</li>
 * </ul>
 *
 * <p>The panel hides itself when the supplied item list is null or empty,
 * preserving the blank-space contract for sources with no recommended items
 * authored on any step.
 *
 * <p>All mutations to Swing state are dispatched on the EDT via
 * {@link SwingUtilities#invokeLater}.
 */
public class SourceRecommendedItemsChipPanel extends JPanel
{
	/** Border width in pixels — matches the muted advisory feel of the per-step strip. */
	static final int CHIP_BORDER = 1;

	/** Icon size within each chip. */
	static final int ICON_SIZE = 28;

	/** Total chip dimension: icon + borders on each side. */
	static final int CHIP_SIZE = ICON_SIZE + CHIP_BORDER * 2;

	/** Background of the source-header strip. Matches the requirements/banner section tone. */
	private static final Color BG = new Color(25, 35, 55);

	/** Muted neutral border — chip strip is informational only at this surface. */
	static final Color BORDER_COLOR = new Color(180, 180, 180, 160);

	/** Heading text shown to the left of the chip row. */
	static final String HEADING_TEXT = "Recommended:";

	private final ItemManager itemManager;

	/** The horizontal row that holds the heading label and chip labels. */
	private final JPanel chipRow;

	public SourceRecommendedItemsChipPanel(ItemManager itemManager)
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
	 * Rebuilds the chip strip from the given item IDs and sets visibility.
	 * Safe to call from any thread; dispatches to the EDT internally.
	 *
	 * @param itemIds the source-level recommended item IDs (typically the result
	 *                of {@link com.collectionloghelper.data.CollectionLogSource#getEffectiveRecommendedItemIds()});
	 *                {@code null} or empty hides the panel
	 */
	public void update(List<Integer> itemIds)
	{
		final List<Integer> safeIds =
			(itemIds != null) ? itemIds : Collections.<Integer>emptyList();

		SwingUtilities.invokeLater(() ->
		{
			chipRow.removeAll();

			if (safeIds.isEmpty())
			{
				setVisible(false);
				return;
			}

			JLabel heading = new JLabel(HEADING_TEXT);
			heading.setFont(FontManager.getRunescapeSmallFont());
			heading.setForeground(new Color(150, 150, 150));
			heading.setAlignmentX(LEFT_ALIGNMENT);
			chipRow.add(heading);
			chipRow.add(Box.createHorizontalStrut(6));

			for (Integer itemId : safeIds)
			{
				if (itemId == null || itemId <= 0)
				{
					continue;
				}
				JLabel chip = buildChip(itemId);
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
	 * 1-pixel muted neutral border. The icon and tooltip are loaded
	 * asynchronously when {@code itemManager} can resolve them.
	 *
	 * <p>Package-visible for test reflection.
	 */
	JLabel buildChip(int itemId)
	{
		JLabel chip = new JLabel();
		chip.setPreferredSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setMinimumSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setMaximumSize(new Dimension(CHIP_SIZE, CHIP_SIZE));
		chip.setHorizontalAlignment(SwingConstants.CENTER);
		chip.setVerticalAlignment(SwingConstants.CENTER);
		chip.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, CHIP_BORDER));
		chip.setBackground(BG);
		chip.setOpaque(true);

		// Tooltip — item name when resolvable; fall back to the raw ID so
		// players can spot-check missing names in dev builds.
		String tooltip = lookupName(itemId);
		chip.setToolTipText(tooltip);

		loadChipIcon(itemId, chip);

		return chip;
	}

	/**
	 * Loads the item sprite for {@code itemId} into {@code chip} asynchronously.
	 * No-op when the item manager has no image (e.g. in unit tests).
	 *
	 * <p>Package-visible so tests can stub behavior without touching the EDT.
	 */
	void loadChipIcon(int itemId, JLabel chip)
	{
		if (itemManager == null)
		{
			return;
		}
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

		if (asyncImage.getWidth() > 0)
		{
			applyIcon.run();
		}
	}

	/**
	 * Returns the item name for {@code itemId} via {@link ItemManager#getItemComposition},
	 * falling back to {@code "Item " + itemId} if the lookup fails or returns null.
	 */
	private String lookupName(int itemId)
	{
		if (itemManager == null)
		{
			return "Item " + itemId;
		}
		try
		{
			ItemComposition comp = itemManager.getItemComposition(itemId);
			if (comp != null && comp.getName() != null)
			{
				return comp.getName();
			}
		}
		catch (RuntimeException ignored)
		{
			// Some test contexts don't wire up ItemManager fully.
		}
		return "Item " + itemId;
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
