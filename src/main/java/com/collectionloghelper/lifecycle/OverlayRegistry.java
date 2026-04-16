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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.overlay.DialogHighlightOverlay;
import com.collectionloghelper.overlay.GroundItemHighlightOverlay;
import com.collectionloghelper.overlay.GuidanceMinimapOverlay;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ItemHighlightOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import com.collectionloghelper.overlay.WidgetHighlightOverlay;
import com.collectionloghelper.overlay.WorldMapRouteOverlay;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Owns the lifecycle of all plugin overlays. Registration order is
 * preserved exactly as it was in the plugin's startUp/shutDown to avoid
 * any subtle z-order or listener-registration ordering changes.
 */
@Singleton
public class OverlayRegistry
{
	private final OverlayManager overlayManager;
	private final GuidanceOverlay guidanceOverlay;
	private final GuidanceMinimapOverlay guidanceMinimapOverlay;
	private final DialogHighlightOverlay dialogHighlightOverlay;
	private final ObjectHighlightOverlay objectHighlightOverlay;
	private final ItemHighlightOverlay itemHighlightOverlay;
	private final WorldMapRouteOverlay worldMapRouteOverlay;
	private final GroundItemHighlightOverlay groundItemHighlightOverlay;
	private final WidgetHighlightOverlay widgetHighlightOverlay;

	@Inject
	public OverlayRegistry(
		OverlayManager overlayManager,
		GuidanceOverlay guidanceOverlay,
		GuidanceMinimapOverlay guidanceMinimapOverlay,
		DialogHighlightOverlay dialogHighlightOverlay,
		ObjectHighlightOverlay objectHighlightOverlay,
		ItemHighlightOverlay itemHighlightOverlay,
		WorldMapRouteOverlay worldMapRouteOverlay,
		GroundItemHighlightOverlay groundItemHighlightOverlay,
		WidgetHighlightOverlay widgetHighlightOverlay)
	{
		this.overlayManager = overlayManager;
		this.guidanceOverlay = guidanceOverlay;
		this.guidanceMinimapOverlay = guidanceMinimapOverlay;
		this.dialogHighlightOverlay = dialogHighlightOverlay;
		this.objectHighlightOverlay = objectHighlightOverlay;
		this.itemHighlightOverlay = itemHighlightOverlay;
		this.worldMapRouteOverlay = worldMapRouteOverlay;
		this.groundItemHighlightOverlay = groundItemHighlightOverlay;
		this.widgetHighlightOverlay = widgetHighlightOverlay;
	}

	/** Adds all plugin overlays to the OverlayManager. Call from Plugin.startUp(). */
	public void registerAll()
	{
		overlayManager.add(guidanceOverlay);
		overlayManager.add(guidanceMinimapOverlay);
		overlayManager.add(dialogHighlightOverlay);
		overlayManager.add(objectHighlightOverlay);
		overlayManager.add(itemHighlightOverlay);
		overlayManager.add(worldMapRouteOverlay);
		overlayManager.add(groundItemHighlightOverlay);
		overlayManager.add(widgetHighlightOverlay);
	}

	/** Removes all plugin overlays from the OverlayManager. Call from Plugin.shutDown(). */
	public void unregisterAll()
	{
		overlayManager.remove(guidanceOverlay);
		overlayManager.remove(guidanceMinimapOverlay);
		overlayManager.remove(dialogHighlightOverlay);
		overlayManager.remove(objectHighlightOverlay);
		overlayManager.remove(itemHighlightOverlay);
		overlayManager.remove(worldMapRouteOverlay);
		overlayManager.remove(groundItemHighlightOverlay);
		overlayManager.remove(widgetHighlightOverlay);
	}
}
