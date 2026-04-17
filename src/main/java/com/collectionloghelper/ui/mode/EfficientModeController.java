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
import com.collectionloghelper.EfficientSortMode;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.EfficiencyCalculator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.ui.ItemRowPanel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.game.ItemManager;

/**
 * Renders the Efficient mode: all accessible sources ranked by the
 * efficiency score (or the user-chosen alternate sort). Shows a
 * "Top Pick" banner for the first accessible source and a per-source
 * row with the "best" (fastest-to-obtain) missing item.
 */
public class EfficientModeController implements PanelModeController
{
	private final PanelShellContext shell;
	private final CollectionLogHelperConfig config;
	private final PlayerCollectionState collectionState;
	private final EfficiencyCalculator calculator;
	private final RequirementsChecker requirementsChecker;
	private final ItemManager itemManager;

	public EfficientModeController(
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
		List<ScoredItem> scored = calculator.rankByEfficiency();
		EfficientSortMode sortMode = shell.getEfficientSortMode();
		if (sortMode != null && sortMode != EfficientSortMode.EFFICIENCY)
		{
			scored = new ArrayList<>(scored);
			switch (sortMode)
			{
				case KILL_TIME:
					scored.sort(Comparator.comparingDouble(s -> s.getSource().getKillTimeSeconds()));
					break;
				case DROP_RATE:
					scored.sort(Comparator.comparingDouble(ScoredItem::getDropOnlyScore).reversed());
					break;
				case ALPHABETICAL:
					scored.sort(Comparator.comparing(s -> s.getSource().getName()));
					break;
				case ITEMS_REMAINING:
					scored.sort(Comparator.comparingInt(ScoredItem::getMissingItemCount).reversed());
					break;
				case COMPLETION_PERCENTAGE:
					scored.sort(Comparator.comparingDouble((ScoredItem s) ->
					{
						int total = s.getSource().getItems().size();
						return total > 0 ? (double) (total - s.getMissingItemCount()) / total : 0;
					}).reversed());
					break;
			}
		}

		if (scored.isEmpty())
		{
			if (config.afkFilter().getMinAfkLevel() > 0)
			{
				shell.addEmptyStateMessage("No sources match the current AFK filter.<br>Try a lower Efficient AFK setting.");
			}
			else if (config.hideLockedContent())
			{
				shell.addEmptyStateMessage("All sources are locked or completed.<br>Adjust filters to see more.");
			}
			else
			{
				shell.addEmptyStateMessage("All items obtained. Congratulations!");
			}
			return;
		}

		// Top Pick should always be an accessible (unlocked) source
		scored.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.ifPresent(topPick -> shell.addToList(shell.createQuickGuidePanel(topPick)));

		for (ScoredItem si : scored)
		{
			boolean onTask = calculator.isOnSlayerTask(si.getSource());

			// Show the best (fastest-to-obtain) item per source, but use
			// the source-level score for the time display so the displayed
			// time matches the sort order.
			CollectionLogItem bestItem = si.getBestItem();
			if (bestItem != null && !collectionState.isItemObtained(bestItem.getItemId()))
			{
				ItemRowPanel row = new ItemRowPanel(bestItem, si.getSource(), false,
					si.getScore(), si.isLocked(), onTask,
					requirementsChecker.getUnmetRequirements(si.getSource().getName()),
					itemManager, () -> shell.showDetail(bestItem, si.getSource()));
				shell.addToList(row);
			}
			else
			{
				// Fallback: show first missing item if bestItem is null or already obtained
				for (CollectionLogItem item : si.getSource().getItems())
				{
					if (!collectionState.isItemObtained(item.getItemId()))
					{
						ItemRowPanel row = new ItemRowPanel(item, si.getSource(), false,
							si.getScore(), si.isLocked(), onTask,
							requirementsChecker.getUnmetRequirements(si.getSource().getName()),
							itemManager, () -> shell.showDetail(item, si.getSource()));
						shell.addToList(row);
						break;
					}
				}
			}
		}
	}
}
