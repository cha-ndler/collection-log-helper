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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.PlayerBankState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link ClueSummaryView}.
 */
public class ClueSummaryViewTest
{
	private PlayerBankState bankState;
	private ClueSummaryView view;

	@Before
	public void setUp()
	{
		bankState = Mockito.mock(PlayerBankState.class);
		view = new ClueSummaryView();
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void update_bothNullSummaries_doesNotThrow()
	{
		Mockito.when(bankState.getCasketSummary()).thenReturn(null);
		Mockito.when(bankState.getContainerSummary()).thenReturn(null);
		view.updateFromBankState(bankState);
	}

	@Test
	public void update_casketSummaryOnly_doesNotThrow()
	{
		Mockito.when(bankState.getCasketSummary()).thenReturn("3 caskets");
		Mockito.when(bankState.getContainerSummary()).thenReturn(null);
		view.updateFromBankState(bankState);
	}

	@Test
	public void update_containerSummaryOnly_doesNotThrow()
	{
		Mockito.when(bankState.getCasketSummary()).thenReturn(null);
		Mockito.when(bankState.getContainerSummary()).thenReturn("2 bird houses");
		view.updateFromBankState(bankState);
	}

	@Test
	public void update_bothSummaries_doesNotThrow()
	{
		Mockito.when(bankState.getCasketSummary()).thenReturn("3 caskets");
		Mockito.when(bankState.getContainerSummary()).thenReturn("2 bird houses");
		view.updateFromBankState(bankState);
	}

	@Test
	public void update_nullBankState_doesNotThrow()
	{
		// snapshot-then-null-check: null input should be handled gracefully
		view.updateFromBankState(null);
	}
}
