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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * A "Recommended:" heading on its own full-width line followed by a strip of
 * item-icon chips that wrap onto additional lines, rendered in the source-detail
 * header for the source-level "Recommended Gear" list (#573).
 *
 * <p>The chips live in a {@link WrapLayout} container so that at the narrow
 * 225px side-panel width several chips flow onto multiple rows instead of
 * overflowing and clipping the rightmost chip(s). The heading matches the
 * full-width style of the sibling "Requirements:" section.
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

	/** Heading text shown on its own full-width line above the chip strip. */
	static final String HEADING_TEXT = "Recommended:";

	/** Horizontal gap between wrapped chips, in pixels. */
	private static final int CHIP_HGAP = 3;

	/** Vertical gap between wrapped chip rows, in pixels. */
	private static final int CHIP_VGAP = 3;

	private final ItemManager itemManager;

	/**
	 * Optional client thread used to resolve item names off the EDT.
	 * When {@code null} (e.g. in {@link com.collectionloghelper.ui.ItemDetailPanel}
	 * or unit tests), tooltips degrade gracefully to {@code "Item <id>"}.
	 */
	@Nullable
	private final ClientThread clientThread;

	/** Full-width heading line ("Recommended:") shown above the chip strip. */
	private final JLabel heading;

	/**
	 * The wrapping container that holds the chip labels. Uses {@link WrapLayout}
	 * so chips flow onto additional rows at narrow panel widths instead of
	 * clipping at the right edge.
	 */
	private final JPanel chipRow;

	/**
	 * Item IDs rendered on the last successful build. {@link #update(List)} is a
	 * no-op when called again with an equal list -- without this guard the strip
	 * tears down ({@code removeAll}) and reloads its async icons on every guidance
	 * refresh, which manifested as a subtle flash on frequent updates (e.g. each
	 * XP drop while guidance was active). Touched only on the EDT.
	 */
	private List<Integer> lastRenderedIds = null;

	/**
	 * Constructs the panel without a client thread. Chip tooltips will always
	 * show {@code "Item <id>"} rather than the resolved item name. Use this
	 * constructor from call sites that don't have access to {@link ClientThread}
	 * (e.g. {@link com.collectionloghelper.ui.ItemDetailPanel}).
	 */
	public SourceRecommendedItemsChipPanel(ItemManager itemManager)
	{
		this(itemManager, null);
	}

	/**
	 * Constructs the panel with an optional client thread for async name
	 * resolution. When {@code clientThread} is non-null, each chip's tooltip
	 * is resolved on the client thread and then applied on the EDT — ensuring
	 * {@link ItemManager#getItemComposition} is never called from the EDT.
	 *
	 * @param itemManager  the item manager (may be {@code null} in unit tests)
	 * @param clientThread the client thread for deferred name lookup, or
	 *                     {@code null} to skip name resolution
	 */
	public SourceRecommendedItemsChipPanel(@Nullable ItemManager itemManager,
		@Nullable ClientThread clientThread)
	{
		this.itemManager = itemManager;
		this.clientThread = clientThread;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BG);
		setAlignmentX(LEFT_ALIGNMENT);
		setVisible(false);

		heading = new JLabel(HEADING_TEXT);
		heading.setFont(FontManager.getRunescapeSmallFont());
		heading.setForeground(new Color(150, 150, 150));
		heading.setAlignmentX(LEFT_ALIGNMENT);
		add(heading);

		// LEADING-aligned WrapLayout: chips fill left-to-right and wrap onto a new
		// row when they exceed the panel width, so none clip. Zero left/right inset
		// keeps the strip flush with the heading and the surrounding blocks.
		chipRow = new JPanel(new WrapLayout(FlowLayout.LEADING, CHIP_HGAP, CHIP_VGAP));
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
			// Idempotent: if the same IDs are already rendered, do nothing. This
			// stops the strip from rebuilding (and reloading async icons) on every
			// guidance refresh -- the subtle per-update flash.
			if (safeIds.equals(lastRenderedIds))
			{
				return;
			}
			lastRenderedIds = new java.util.ArrayList<>(safeIds);

			chipRow.removeAll();

			if (safeIds.isEmpty())
			{
				setVisible(false);
				return;
			}

			for (Integer itemId : safeIds)
			{
				if (itemId == null || itemId <= 0)
				{
					continue;
				}
				JLabel chip = buildChip(itemId);
				chipRow.add(chip);
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
	 * 1-pixel muted neutral border. The icon is loaded asynchronously via
	 * {@link #loadChipIcon}. The tooltip is initially set to {@code "Item <id>"}
	 * and then updated asynchronously on the client thread (if one is available)
	 * to avoid calling {@link ItemManager#getItemComposition} from the EDT, which
	 * throws {@code AssertionError: must be called on client thread}.
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

		// Safe placeholder — never calls getItemComposition on the EDT.
		chip.setToolTipText("Item " + itemId);

		// Resolve the real item name on the client thread, then push it back to
		// the EDT. This is a no-op when clientThread is null (e.g. ItemDetailPanel
		// call site or unit tests), leaving the placeholder tooltip in place.
		resolveNameAsync(itemId, chip);

		loadChipIcon(itemId, chip);

		return chip;
	}

	/**
	 * Schedules an item-name lookup on the client thread and, when done, applies
	 * the result as the chip's tooltip text on the EDT. No-op when
	 * {@code clientThread} or {@code itemManager} is null.
	 */
	private void resolveNameAsync(int itemId, JLabel chip)
	{
		if (clientThread == null || itemManager == null)
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			String name;
			try
			{
				ItemComposition comp = itemManager.getItemComposition(itemId);
				name = (comp != null && comp.getName() != null) ? comp.getName() : "Item " + itemId;
			}
			catch (Exception ex)
			{
				name = "Item " + itemId;
			}
			final String resolvedName = name;
			SwingUtilities.invokeLater(() -> chip.setToolTipText(resolvedName));
		});
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

	/**
	 * A {@link FlowLayout} that wraps its components onto additional rows and,
	 * crucially, reports a preferred size whose <em>height</em> reflects the
	 * wrapped row count at the container's actual (target) width.
	 *
	 * <p>Plain {@code FlowLayout} always reports a single-row preferred height,
	 * so when nested inside a vertical {@code BoxLayout} it is given only enough
	 * vertical space for one row and the wrapped rows are clipped. This subclass
	 * computes the height by laying the children out against the available width
	 * (taken from the container, an ancestor with a non-zero width, or the
	 * preferred row width as a last resort), so the parent {@code BoxLayout}
	 * allocates the correct vertical space.
	 *
	 * <p>Adapted from the long-standing public-domain "WrapLayout" pattern
	 * (a FlowLayout that computes wrapped preferred height); trimmed to the
	 * single LEADING use case this panel needs.
	 */
	static final class WrapLayout extends FlowLayout
	{
		WrapLayout(int align, int hgap, int vgap)
		{
			super(align, hgap, vgap);
		}

		@Override
		public Dimension preferredLayoutSize(Container target)
		{
			return layoutSize(target, true);
		}

		@Override
		public Dimension minimumLayoutSize(Container target)
		{
			Dimension minimum = layoutSize(target, false);
			minimum.width -= (getHgap() + 1);
			return minimum;
		}

		/**
		 * Computes the layout size, flowing children onto rows constrained to the
		 * effective target width and summing the resulting row heights.
		 */
		private Dimension layoutSize(Container target, boolean preferred)
		{
			synchronized (target.getTreeLock())
			{
				int targetWidth = resolveTargetWidth(target);

				Insets insets = target.getInsets();
				int horizontalInsetsAndGap = insets.left + insets.right + (getHgap() * 2);
				int maxWidth = targetWidth - horizontalInsetsAndGap;

				Dimension dim = new Dimension(0, 0);
				int rowWidth = 0;
				int rowHeight = 0;

				int memberCount = target.getComponentCount();
				for (int i = 0; i < memberCount; i++)
				{
					Component m = target.getComponent(i);
					if (!m.isVisible())
					{
						continue;
					}
					Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

					// Wrap to a new row when the next chip would exceed the width.
					if (rowWidth + d.width > maxWidth)
					{
						addRow(dim, rowWidth, rowHeight);
						rowWidth = 0;
						rowHeight = 0;
					}

					if (rowWidth != 0)
					{
						rowWidth += getHgap();
					}

					rowWidth += d.width;
					rowHeight = Math.max(rowHeight, d.height);
				}

				addRow(dim, rowWidth, rowHeight);

				dim.width += horizontalInsetsAndGap;
				dim.height += insets.top + insets.bottom + (getVgap() * 2);
				return dim;
			}
		}

		/**
		 * Returns a usable width for wrapping: the target's own width if laid out,
		 * else the nearest ancestor with a non-zero width, else the target's
		 * preferred width (single-row fallback before the first real layout pass).
		 */
		private int resolveTargetWidth(Container target)
		{
			int width = target.getWidth();
			Container ancestor = target.getParent();
			while (width == 0 && ancestor != null)
			{
				width = ancestor.getWidth();
				ancestor = ancestor.getParent();
			}
			if (width == 0)
			{
				width = target.getPreferredSize().width;
			}
			return width;
		}

		private void addRow(Dimension dim, int rowWidth, int rowHeight)
		{
			dim.width = Math.max(dim.width, rowWidth);
			if (dim.height > 0)
			{
				dim.height += getVgap();
			}
			dim.height += rowHeight;
		}
	}
}
