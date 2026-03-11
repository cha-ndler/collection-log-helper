package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
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
public class DialogHighlightOverlay extends Overlay
{
	private static final int DIALOG_OPTION_GROUP = 219;
	private static final int DIALOG_OPTION_CHILD = 1;

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
		if (!guidanceActive || targetDialogOptions.isEmpty())
		{
			return null;
		}

		Widget optionContainer = client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD);
		if (optionContainer == null)
		{
			return null;
		}

		Widget[] children = optionContainer.getDynamicChildren();
		if (children == null)
		{
			return null;
		}

		Color highlightColor = config.overlayColor();

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
					option.setTextColor(highlightColor.getRGB() & 0xFFFFFF);
					break;
				}
			}
		}

		return null;
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
