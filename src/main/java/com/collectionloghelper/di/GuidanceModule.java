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
package com.collectionloghelper.di;

import com.collectionloghelper.guidance.GuidanceMovementTracker;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import com.collectionloghelper.guidance.RequiredItemResolver;
import com.collectionloghelper.lifecycle.GuidanceEventRouter;
import com.collectionloghelper.lifecycle.GuidanceUIState;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.overlay.ObjectHighlightOverlay;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

/**
 * Aggregate holder for the plugin's guidance-layer Guice singletons.
 *
 * <p>Groups the eight guidance-layer dependencies that {@code CollectionLogHelperPlugin}
 * previously injected individually, reducing the plugin's {@code @Inject} field count
 * and providing a single seam for guidance-layer wiring.
 *
 * <p>Second of four planned sub-module extractions tracked by issue #504.
 */
@Singleton
@Getter
public class GuidanceModule
{
	private final GuidanceSequencer guidanceSequencer;
	private final GuidanceOverlayCoordinator guidanceCoordinator;
	private final GuidanceOverlay guidanceOverlay;
	private final ObjectHighlightOverlay objectHighlightOverlay;
	private final RequiredItemResolver requiredItemResolver;
	private final GuidanceUIState guidanceUIState;
	private final GuidanceEventRouter guidanceEventRouter;
	private final GuidanceMovementTracker guidanceMovementTracker;

	@Inject
	public GuidanceModule(
		GuidanceSequencer guidanceSequencer,
		GuidanceOverlayCoordinator guidanceCoordinator,
		GuidanceOverlay guidanceOverlay,
		ObjectHighlightOverlay objectHighlightOverlay,
		RequiredItemResolver requiredItemResolver,
		GuidanceUIState guidanceUIState,
		GuidanceEventRouter guidanceEventRouter,
		GuidanceMovementTracker guidanceMovementTracker)
	{
		this.guidanceSequencer = guidanceSequencer;
		this.guidanceCoordinator = guidanceCoordinator;
		this.guidanceOverlay = guidanceOverlay;
		this.objectHighlightOverlay = objectHighlightOverlay;
		this.requiredItemResolver = requiredItemResolver;
		this.guidanceUIState = guidanceUIState;
		this.guidanceEventRouter = guidanceEventRouter;
		this.guidanceMovementTracker = guidanceMovementTracker;
	}
}
