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
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Singleton;

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

	private volatile List<String> targetDialogOptions = new ArrayList<>();
	private volatile boolean guidanceActive;

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

		// Highlight matching dialog options in the multi-option dialog
		if (!targetDialogOptions.isEmpty())
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

		for (Widget option : children)
		{
			if (option == null || option.getText() == null)
			{
				continue;
			}

			String optionText = option.getText();
			for (String target : targetDialogOptions)
			{
				if (optionText.toLowerCase().contains(target.toLowerCase()))
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
		if (text != null && text.toLowerCase().contains("click here to continue"))
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
		Color fillColor = new Color(
			highlightColor.getRed(),
			highlightColor.getGreen(),
			highlightColor.getBlue(),
			BACKGROUND_ALPHA
		);
		graphics.setColor(fillColor);
		graphics.fillRect(x, y, width, height);

		// Subtle border
		Color borderColor = new Color(
			highlightColor.getRed(),
			highlightColor.getGreen(),
			highlightColor.getBlue(),
			BORDER_ALPHA
		);
		graphics.setColor(borderColor);
		graphics.drawRect(x, y, width, height);
	}

	public void setTargetDialogOptions(List<String> options)
	{
		this.targetDialogOptions = options != null ? options : new ArrayList<>();
	}

	public void setGuidanceActive(boolean active)
	{
		this.guidanceActive = active;
	}

	public void clear()
	{
		targetDialogOptions = new ArrayList<>();
		guidanceActive = false;
	}
}
