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

import java.util.EnumMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.InOrder;

@RunWith(MockitoJUnitRunner.class)
public class PanelModeDispatcherTest
{
	/** Small enum used to keep the dispatcher test Swing-free. */
	enum TestMode { ALPHA, BETA, GAMMA }

	@Mock
	private PanelModeController alpha;

	@Mock
	private PanelModeController beta;

	private PanelModeDispatcher<TestMode> dispatcher;

	@Before
	public void setUp()
	{
		Map<TestMode, PanelModeController> controllers = new EnumMap<>(TestMode.class);
		controllers.put(TestMode.ALPHA, alpha);
		controllers.put(TestMode.BETA, beta);
		dispatcher = new PanelModeDispatcher<>(controllers);
	}

	@Test
	public void switchModeFiresDeactivateThenActivateInOrder()
	{
		// Act — switch from ALPHA to BETA
		dispatcher.switchMode(TestMode.ALPHA, TestMode.BETA);

		// Assert — leaving controller's hook fires before entering controller's hook
		InOrder order = inOrder(alpha, beta);
		order.verify(alpha).onModeDeactivated();
		order.verify(beta).onModeActivated();
	}

	@Test
	public void switchModeNoopsWhenSameMode()
	{
		// Act — self-selection
		dispatcher.switchMode(TestMode.ALPHA, TestMode.ALPHA);

		// Assert — no lifecycle hooks fire
		verify(alpha, never()).onModeActivated();
		verify(alpha, never()).onModeDeactivated();
	}

	@Test
	public void switchModeIgnoresAbsentControllers()
	{
		// Act — GAMMA has no registered controller
		dispatcher.switchMode(TestMode.GAMMA, TestMode.ALPHA);

		// Assert — entering controller still gets onModeActivated
		verify(alpha).onModeActivated();
		verify(alpha, never()).onModeDeactivated();
	}

	@Test
	public void switchModeIgnoresAbsentEntryController()
	{
		// Act — switching to an unregistered mode
		dispatcher.switchMode(TestMode.ALPHA, TestMode.GAMMA);

		// Assert — leaving controller still gets onModeDeactivated; no error
		verify(alpha).onModeDeactivated();
	}

	@Test
	public void buildViewDelegatesToRegisteredController()
	{
		// Act
		dispatcher.buildView(TestMode.ALPHA);

		// Assert
		verify(alpha).buildView();
	}

	@Test
	public void buildViewNoopsForUnregisteredMode()
	{
		// Act — GAMMA is not registered
		dispatcher.buildView(TestMode.GAMMA);

		// Assert — nothing thrown, no interactions
		verify(alpha, never()).buildView();
		verify(beta, never()).buildView();
	}
}
