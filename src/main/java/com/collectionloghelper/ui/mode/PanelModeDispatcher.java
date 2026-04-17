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
package com.collectionloghelper.ui.mode;

import java.util.Map;

/**
 * Pure dispatch helper that fires the {@link PanelModeController} lifecycle
 * hooks when the shell's mode-selector flips from one mode to another, and
 * delegates the {@code buildView()} call to the entry for the current mode.
 *
 * <p>Extracted from the shell's item-listener to give the mode-switching
 * semantics a test surface independent of Swing.
 *
 * <p>The type parameter {@code M} is the enum of modes (the shell uses
 * {@code CollectionLogHelperPanel.Mode}); kept generic so the dispatcher
 * itself has no dependency on the panel class.
 */
public final class PanelModeDispatcher<M extends Enum<M>>
{
	private final Map<M, ? extends PanelModeController> controllers;

	public PanelModeDispatcher(Map<M, ? extends PanelModeController> controllers)
	{
		this.controllers = controllers;
	}

	/**
	 * Fires {@link PanelModeController#onModeDeactivated()} on the previous
	 * controller (if any and if different) and
	 * {@link PanelModeController#onModeActivated()} on the next controller
	 * (if any and if different). A self-selection (previous == next) is a
	 * no-op for lifecycle hooks.
	 */
	public void switchMode(M previous, M next)
	{
		if (previous == next)
		{
			return;
		}
		PanelModeController leaving = controllers.get(previous);
		if (leaving != null)
		{
			leaving.onModeDeactivated();
		}
		PanelModeController entering = controllers.get(next);
		if (entering != null)
		{
			entering.onModeActivated();
		}
	}

	/**
	 * Delegates {@link PanelModeController#buildView()} to the controller
	 * for the given mode. No-op if no controller is registered.
	 */
	public void buildView(M mode)
	{
		PanelModeController controller = controllers.get(mode);
		if (controller != null)
		{
			controller.buildView();
		}
	}
}
