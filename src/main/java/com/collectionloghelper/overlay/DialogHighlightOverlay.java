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
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Highlights dialog options that the player should select when interacting
 * with NPCs during guidance. Similar to Quest Helper's dialog highlighting.
 *
 * Widget groups used:
 *   219 = Player dialog choices (multi-option)
 *   231 = NPC dialog (continue)
 */
@Singleton
public class DialogHighlightOverlay extends Overlay
{
	private static final int DIALOG_OPTION_GROUP = 219;
	private static final int DIALOG_OPTION_CHILD = 1;

	private static final int NPC_DIALOG_GROUP = 231;
	private static final int NPC_DIALOG_CONTINUE_CHILD = 5;

	private static final String ARROW_INDICATOR = "\u25b6 ";
	private static final int BACKGROUND_PADDING_X = 4;
	private static final int BACKGROUND_PADDING_Y = 2;
	private static final int BACKGROUND_ALPHA = 50;
	private static final int BORDER_ALPHA = 120;

	private final Client client;
	private final CollectionLogHelperConfig config;

	private volatile List<String> targetDialogOptionsLower = new ArrayList<>();
	private volatile boolean guidanceActive;
	private Color cachedHighlightColor;
	private Color cachedFillColor;
	private Color cachedBorderColor;

	@Inject
	private DialogHighlightOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!guidanceActive)
		{
			return null;
		}

		Color highlightColor = config.overlayColor();
		if (!highlightColor.equals(cachedHighlightColor))
		{
			cachedHighlightColor = highlightColor;
			cachedFillColor = new Color(highlightColor.getRed(), highlightColor.getGreen(),
				highlightColor.getBlue(), BACKGROUND_ALPHA);
			cachedBorderColor = new Color(highlightColor.getRed(), highlightColor.getGreen(),
				highlightColor.getBlue(), BORDER_ALPHA);
		}

		// Highlight matching dialog options in the multi-option dialog
		if (!targetDialogOptionsLower.isEmpty())
		{
			renderDialogOptionHighlights(graphics, highlightColor);
		}

		// Highlight "Click here to continue" in NPC dialog to keep the player moving
		renderContinueHighlight(graphics, highlightColor);

		return null;
	}

	private void renderDialogOptionHighlights(Graphics2D graphics, Color highlightColor)
	{
		Widget optionContainer = client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD);
		if (optionContainer == null)
		{
			return;
		}

		Widget[] children = optionContainer.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		List<String> targets = targetDialogOptionsLower;
		for (Widget option : children)
		{
			if (option == null || option.getText() == null)
			{
				continue;
			}

			String optionText = option.getText();
			// Skip if already highlighted (arrow prepended on a previous frame)
			if (optionText.startsWith(ARROW_INDICATOR))
			{
				renderWidgetHighlight(graphics, option, highlightColor);
				continue;
			}

			String optionLower = optionText.toLowerCase();
			for (String target : targets)
			{
				if (optionLower.contains(target))
				{
					renderWidgetHighlight(graphics, option, highlightColor);
					option.setText(ARROW_INDICATOR + optionText);
					option.setTextColor(highlightColor.getRGB() & 0xFFFFFF);
					break;
				}
			}
		}
	}

	private void renderContinueHighlight(Graphics2D graphics, Color highlightColor)
	{
		Widget continueWidget = client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD);
		if (continueWidget == null || continueWidget.isHidden())
		{
			return;
		}

		String text = continueWidget.getText();
		if (text != null && text.contains("Click here to continue"))
		{
			renderWidgetHighlight(graphics, continueWidget, highlightColor);
			continueWidget.setTextColor(highlightColor.getRGB() & 0xFFFFFF);
		}
	}

	private void renderWidgetHighlight(Graphics2D graphics, Widget widget, Color highlightColor)
	{
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return;
		}

		int x = bounds.x - BACKGROUND_PADDING_X;
		int y = bounds.y - BACKGROUND_PADDING_Y;
		int width = bounds.width + BACKGROUND_PADDING_X * 2;
		int height = bounds.height + BACKGROUND_PADDING_Y * 2;

		// Semi-transparent fill similar to object/NPC highlight overlays
		graphics.setColor(cachedFillColor);
		graphics.fillRect(x, y, width, height);

		// Subtle border
		graphics.setColor(cachedBorderColor);
		graphics.drawRect(x, y, width, height);
	}

	public void setTargetDialogOptions(List<String> options)
	{
		if (options == null || options.isEmpty())
		{
			this.targetDialogOptionsLower = new ArrayList<>();
		}
		else
		{
			List<String> lower = new ArrayList<>(options.size());
			for (String opt : options)
			{
				lower.add(opt.toLowerCase());
			}
			this.targetDialogOptionsLower = lower;
		}
	}

	public void setGuidanceActive(boolean active)
	{
		this.guidanceActive = active;
	}

	public void clear()
	{
		targetDialogOptionsLower = new ArrayList<>();
		guidanceActive = false;
	}
}
