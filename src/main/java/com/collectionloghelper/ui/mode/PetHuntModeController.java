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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.ui.ItemRowPanel;
import java.util.List;
import net.runelite.client.game.ItemManager;

/**
 * Renders the Pet Hunt mode: lists only pet drops across all sources,
 * ranked by efficiency. Respects the AFK-filter on empty state.
 */
public class PetHuntModeController implements PanelModeController
{
	private final PanelShellContext shell;
	private final CollectionLogHelperConfig config;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final RequirementsChecker requirementsChecker;
	private final ItemManager itemManager;

	public PetHuntModeController(
		PanelShellContext shell,
		CollectionLogHelperConfig config,
		PlayerCollectionState collectionState,
		EfficiencyCalculator calculator,
		RequirementsChecker requirementsChecker,
		ItemManager itemManager)
	{
		this.shell = shell;
		this.config = config;
		this.collectionState = collectionState;
		this.calculator = calculator;
		this.requirementsChecker = requirementsChecker;
		this.itemManager = itemManager;
	}

	@Override
	public void buildView()
	{
		List<ScoredItem> scored = calculator.filterPetsOnly();
		boolean hideObtained = config.hideObtainedItems();
		int resultCount = 0;

		for (ScoredItem si : scored)
		{
			boolean onTask = calculator.isOnSlayerTask(si.getSource());
			for (CollectionLogItem item : si.getSource().getItems())
			{
				if (!item.isPet())
				{
					continue;
				}
				boolean obtained = collectionState.isItemObtained(item.getItemId());
				if (hideObtained && obtained)
				{
					continue;
				}
				ItemRowPanel row = new ItemRowPanel(item, si.getSource(), obtained,
					si.getScore(), si.isLocked(), onTask,
					requirementsChecker.getUnmetRequirements(si.getSource().getName()),
					itemManager, () -> shell.showDetail(item, si.getSource()));
				shell.addToList(row);
				resultCount++;
			}
		}

		if (resultCount == 0)
		{
			if (config.afkFilter().getMinAfkLevel() > 0)
			{
				shell.addEmptyStateMessage("No pets match the current AFK filter.<br>Try a lower Efficient AFK setting.");
			}
			else
			{
				shell.addEmptyStateMessage("All pets obtained!");
			}
		}
	}
}
