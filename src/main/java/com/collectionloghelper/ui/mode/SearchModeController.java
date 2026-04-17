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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.ui.ItemRowPanel;
import net.runelite.client.game.ItemManager;

/**
 * Renders the Search mode: text-based filtering on source and item names
 * (case-insensitive substring match). Respects the hide-obtained and
 * hide-locked config toggles.
 */
public class SearchModeController implements PanelModeController
{
	private final PanelShellContext shell;
	private final CollectionLogHelperConfig config;
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final RequirementsChecker requirementsChecker;
	private final ItemManager itemManager;

	public SearchModeController(
		PanelShellContext shell,
		CollectionLogHelperConfig config,
		DropRateDatabase database,
		PlayerCollectionState collectionState,
		EfficiencyCalculator calculator,
		RequirementsChecker requirementsChecker,
		ItemManager itemManager)
	{
		this.shell = shell;
		this.config = config;
		this.database = database;
		this.collectionState = collectionState;
		this.calculator = calculator;
		this.requirementsChecker = requirementsChecker;
		this.itemManager = itemManager;
	}

	@Override
	public void buildView()
	{
		String query = shell.getSearchQuery();
		if (query.isEmpty())
		{
			shell.addEmptyStateMessage("Search by item or source name");
			return;
		}

		boolean hideObtained = config.hideObtainedItems();
		boolean hideLocked = config.hideLockedContent();
		int resultCount = 0;

		for (CollectionLogSource source : database.getAllSources())
		{
			boolean locked = !requirementsChecker.isAccessible(source.getName());
			if (hideLocked && locked)
			{
				continue;
			}

			boolean sourceNameMatches = source.getName().toLowerCase().contains(query);
			boolean onTask = calculator.isOnSlayerTask(source);
			for (CollectionLogItem item : source.getItems())
			{
				if (sourceNameMatches || item.getName().toLowerCase().contains(query))
				{
					boolean obtained = collectionState.isItemObtained(item.getItemId());
					if (hideObtained && obtained)
					{
						continue;
					}
					ItemRowPanel row = new ItemRowPanel(item, source, obtained, 0,
						locked, onTask,
						requirementsChecker.getUnmetRequirements(source.getName()),
						itemManager, () -> shell.showDetail(item, source));
					shell.addToList(row);
					resultCount++;
				}
			}
		}

		if (resultCount == 0)
		{
			// Escape query to prevent HTML injection in the JLabel
			String safeQuery = query.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			shell.addEmptyStateMessage("No items match '" + safeQuery + "'");
		}
	}
}
