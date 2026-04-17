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

/**
 * Contract for a single panel mode view. Each controller renders the list
 * contents for its mode by delegating to the shared shell's list container
 * via {@link PanelShellContext}.
 *
 * <p>Mode controllers are pure view builders — they hold no long-lived Swing
 * widgets of their own and are safe to re-invoke on every {@code rebuild()}.
 *
 * <p>Lifecycle hooks {@link #onModeActivated()} and {@link #onModeDeactivated()}
 * fire when the user switches to/from this mode, letting a controller do any
 * one-time setup or teardown (e.g. resetting cached filter state). Most
 * controllers have empty implementations.
 */
public interface PanelModeController
{
	/**
	 * Renders this mode's view into the shared shell's list container.
	 * Called from the shell's {@code rebuild()} on the Swing EDT.
	 */
	void buildView();

	/**
	 * Called when the shell switches INTO this mode (after the selector
	 * changes but before the first {@link #buildView()}).
	 * Default: no-op.
	 */
	default void onModeActivated()
	{
	}

	/**
	 * Called when the shell switches AWAY from this mode (before the new
	 * mode's {@link #onModeActivated()} fires).
	 * Default: no-op.
	 */
	default void onModeDeactivated()
	{
	}
}
