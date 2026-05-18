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
package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;

/**
 * Builder that populates the detail-view container with an
 * {@link ItemDetailPanel} for the given item + source. Extracted from
 * {@link CollectionLogHelperPanel#showDetail(CollectionLogItem, CollectionLogSource)}
 * as part of the issue #503 god-class split.
 *
 * <p>This class is intentionally not a Guice {@code @Singleton}; it is
 * panel-constructed alongside {@link PanelRebuildOrchestrator} and
 * {@link SyncButtonController}. It holds collaborators that supply the data
 * needed to render an item-detail card (obtained state, accessibility,
 * per-source obtained-of-total counts, fairy-ring access, etc.) and exposes a
 * single {@link #populate} method that wires up the view's callbacks.</p>
 */
public class DetailViewBuilder
{
	private final PlayerCollectionState collectionState;
	private final RequirementsChecker requirementsChecker;
	private final ItemManager itemManager;
	private final ClueCompletionEstimator clueEstimator;
	private final BiConsumer<CollectionLogSource, Integer> guidanceActivator;
	private final Runnable guidanceDeactivator;

	public DetailViewBuilder(
		PlayerCollectionState collectionState,
		RequirementsChecker requirementsChecker,
		ItemManager itemManager,
		ClueCompletionEstimator clueEstimator,
		BiConsumer<CollectionLogSource, Integer> guidanceActivator,
		Runnable guidanceDeactivator)
	{
		this.collectionState = Objects.requireNonNull(collectionState, "collectionState");
		this.requirementsChecker = Objects.requireNonNull(requirementsChecker, "requirementsChecker");
		this.itemManager = Objects.requireNonNull(itemManager, "itemManager");
		this.clueEstimator = Objects.requireNonNull(clueEstimator, "clueEstimator");
		this.guidanceActivator = Objects.requireNonNull(guidanceActivator, "guidanceActivator");
		this.guidanceDeactivator = Objects.requireNonNull(guidanceDeactivator, "guidanceDeactivator");
	}

	/**
	 * Clears the supplied detail-view container and renders a fresh
	 * {@link ItemDetailPanel} for the given item + source pinned to
	 * {@link BorderLayout#NORTH}.
	 *
	 * @param target          the detail-view container; must not be {@code null}
	 * @param item            the item to display
	 * @param source          the source the item is associated with
	 * @param guidanceActive  whether guidance is currently active on any source
	 * @param guidedSource    the currently guided source, or {@code null}
	 * @param onBack          callback for the back button (e.g., return to list view)
	 */
	public void populate(
		JPanel target,
		CollectionLogItem item,
		CollectionLogSource source,
		boolean guidanceActive,
		CollectionLogSource guidedSource,
		Runnable onBack)
	{
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(item, "item");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(onBack, "onBack");

		boolean obtained = collectionState.isItemObtained(item.getItemId());
		boolean locked = !requirementsChecker.isAccessible(source.getName());
		boolean isGuidingThis = guidanceActive && guidedSource != null
			&& guidedSource.getName().equals(source.getName());

		int sourceTotal = source.getItems().size();
		int sourceObtained = countObtainedItems(source);

		target.removeAll();
		ItemDetailPanel detail = new ItemDetailPanel(
			item, source, obtained, locked,
			requirementsChecker.getUnmetRequirements(source.getName()),
			sourceObtained, sourceTotal,
			itemManager, clueEstimator,
			requirementsChecker.hasFairyRingAccess(),
			onBack,
			() -> guidanceActivator.accept(source, item.getItemId()),
			guidanceDeactivator,
			isGuidingThis
		);
		target.add(detail, BorderLayout.NORTH);
	}

	private int countObtainedItems(CollectionLogSource source)
	{
		int count = 0;
		for (CollectionLogItem si : source.getItems())
		{
			if (collectionState.isItemObtained(si.getItemId()))
			{
				count++;
			}
		}
		return count;
	}
}
