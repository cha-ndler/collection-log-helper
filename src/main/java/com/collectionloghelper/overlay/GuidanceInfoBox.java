package com.collectionloghelper.overlay;

import java.awt.Color;
import java.awt.image.BufferedImage;
import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

/**
 * InfoBox that displays the current guidance step progress (e.g. "3/8")
 * while a multi-step guidance sequence is active.
 */
public class GuidanceInfoBox extends InfoBox
{
	@Setter
	private String stepText = "";

	@Setter
	private String tooltipText = "";

	@Setter
	private Color textColor = Color.WHITE;

	public GuidanceInfoBox(BufferedImage image, Plugin plugin)
	{
		super(image, plugin);
	}

	@Override
	public String getText()
	{
		return stepText;
	}

	@Override
	public Color getTextColor()
	{
		return textColor;
	}

	@Override
	public String getTooltip()
	{
		return tooltipText;
	}
}
