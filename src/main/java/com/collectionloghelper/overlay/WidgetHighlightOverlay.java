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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
 * Highlights arbitrary game interface widgets during guidance steps.
 * Each widget is identified by a [groupId, childId] pair. This enables
 * highlighting UI elements like spell icons, prayer buttons, or shop
 * interfaces as part of step-by-step guidance.
 */
@Singleton
public class WidgetHighlightOverlay extends Overlay
{
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
	private static final int FILL_ALPHA = 100;

	private final Client client;
	private final CollectionLogHelperConfig config;
	private volatile List<int[]> highlightWidgets = new ArrayList<>();
	private Color cachedOverlayColor;
	private Color cachedFillColor;

	@Inject
	private WidgetHighlightOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	/**
	 * Sets the list of widgets to highlight. Each entry is an int[] of
	 * [groupId, childId].
	 */
	public void setHighlightWidgets(List<int[]> widgets)
	{
		if (widgets == null || widgets.isEmpty())
		{
			this.highlightWidgets = new ArrayList<>();
		}
		else
		{
			this.highlightWidgets = new ArrayList<>(widgets);
		}
	}

	public void clearHighlights()
	{
		this.highlightWidgets = new ArrayList<>();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<int[]> widgets = this.highlightWidgets;
		if (widgets.isEmpty() || !config.showOverlays())
		{
			return null;
		}

		Color color = config.overlayColor();
		if (!color.equals(cachedOverlayColor))
		{
			cachedOverlayColor = color;
			cachedFillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), FILL_ALPHA);
		}
		graphics.setStroke(STROKE_2);

		for (int[] widgetRef : widgets)
		{
			if (widgetRef == null || widgetRef.length < 2)
			{
				continue;
			}

			Widget widget = client.getWidget(widgetRef[0], widgetRef[1]);
			if (widget == null || widget.isHidden())
			{
				continue;
			}

			Rectangle bounds = widget.getBounds();
			if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
			{
				continue;
			}

			graphics.setColor(cachedFillColor);
			graphics.fill(bounds);
			graphics.setColor(color);
			graphics.draw(bounds);
		}

		return null;
	}
}
