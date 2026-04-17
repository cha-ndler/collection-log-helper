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
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.ui.CategorySummaryPanel;
import java.util.List;
import net.runelite.client.game.ItemManager;

/**
 * Renders the Category Focus mode: a "Top Pick" banner for the currently
 * selected category plus a {@link CategorySummaryPanel} listing the
 * category's sources and their items.
 */
public class CategoryModeController implements PanelModeController
{
	private final PanelShellContext shell;
	private final CollectionLogHelperConfig config;
	private final DropRateDatabase database;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final RequirementsChecker requirementsChecker;
	private final ItemManager itemManager;

	public CategoryModeController(
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
		CollectionLogCategory category = shell.getSelectedCategory();
		if (category == null)
		{
			return;
		}

		// Top Pick for this category — always an accessible source
		List<ScoredItem> categoryScored = calculator.filterByCategory(category);
		categoryScored.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick -> shell.addToList(shell.createQuickGuidePanel(topPick)));

		List<CollectionLogSource> sources = database.getSourcesByCategory(category);
		CategorySummaryPanel summary = new CategorySummaryPanel(
			category, sources, collectionState, requirementsChecker, itemManager,
			shell::showDetail, config.hideObtainedItems());
		shell.addToList(summary);
	}
}
