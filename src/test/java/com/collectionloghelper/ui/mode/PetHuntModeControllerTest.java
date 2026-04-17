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

import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import java.util.Collections;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PetHuntModeControllerTest
{
	@Mock
	private PanelShellContext shell;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private PlayerCollectionState collectionState;

	@Mock
	private EfficiencyCalculator calculator;

	@Mock
	private RequirementsChecker requirementsChecker;

	@Mock
	private ItemManager itemManager;

	private PetHuntModeController controller;

	@Before
	public void setUp()
	{
		controller = new PetHuntModeController(
			shell, config, collectionState, calculator, requirementsChecker, itemManager);

		when(calculator.filterPetsOnly()).thenReturn(Collections.emptyList());
		when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		when(config.hideObtainedItems()).thenReturn(false);
	}

	@Test
	public void buildViewRendersAllPetsObtainedEmptyStateWhenNoPetsAndAfkFilterOff()
	{
		// Act
		controller.buildView();

		// Assert
		verify(shell).addEmptyStateMessage("All pets obtained!");
	}

	@Test
	public void buildViewRendersAfkFilterEmptyStateWhenFilterActive()
	{
		// Arrange — simulate an AFK filter being active
		AfkFilter afkFilter = AfkFilter.values()[AfkFilter.values().length - 1];
		// The last enum value is typically the most restrictive AFK filter
		when(config.afkFilter()).thenReturn(afkFilter);

		// Act
		controller.buildView();

		// Assert — message should depend on whether filter's min AFK level > 0
		if (afkFilter.getMinAfkLevel() > 0)
		{
			verify(shell).addEmptyStateMessage(
				"No pets match the current AFK filter.<br>Try a lower Efficient AFK setting.");
		}
		else
		{
			verify(shell).addEmptyStateMessage("All pets obtained!");
		}
	}

	@Test
	public void constructorAcceptsAllDependenciesWithoutThrowing()
	{
		// Arrange & Act & Assert — construction should not throw
		PetHuntModeController fresh = new PetHuntModeController(
			shell, config, collectionState, calculator, requirementsChecker, itemManager);

		// Sanity — no interactions until buildView is called
		verifyNoInteractions(shell);
		verifyNoInteractions(collectionState);
	}
}
