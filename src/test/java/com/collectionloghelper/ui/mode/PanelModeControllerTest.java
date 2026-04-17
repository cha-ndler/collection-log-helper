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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PanelModeControllerTest
{
	/**
	 * Verifies the {@link PanelModeController#onModeActivated()} and
	 * {@link PanelModeController#onModeDeactivated()} default methods are
	 * no-ops so an implementation can skip them when it has no lifecycle work.
	 */
	@Test
	public void defaultLifecycleHooksAreNoOps()
	{
		int[] buildCount = { 0 };
		PanelModeController controller = () -> buildCount[0]++;

		// Act — default lifecycle hooks should not throw and should not invoke buildView
		controller.onModeActivated();
		controller.onModeDeactivated();

		// Assert
		assertEquals(0, buildCount[0]);
	}

	@Test
	public void buildViewDelegatesToImplementation()
	{
		int[] buildCount = { 0 };
		PanelModeController controller = () -> buildCount[0]++;

		controller.buildView();
		controller.buildView();

		assertEquals(2, buildCount[0]);
	}
}
