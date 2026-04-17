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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.efficiency.ScoredItem;
import java.awt.Component;
import javax.swing.JPanel;

/**
 * View helpers the shared panel shell exposes to each mode controller.
 *
 * <p>Keeps controllers decoupled from the concrete
 * {@code CollectionLogHelperPanel} class so they can be unit-tested with a
 * plain mock implementation.
 */
public interface PanelShellContext
{
	/** Adds a child component to the shared list container. */
	void addToList(Component component);

	/** Renders an "empty state" label into the list container. */
	void addEmptyStateMessage(String message);

	/**
	 * Creates the shared "Top Pick" quick-guide banner used at the top of
	 * Efficient and Category modes. The returned panel is not yet attached.
	 */
	JPanel createQuickGuidePanel(ScoredItem topItem);

	/** Opens the detail view for the given item under the given source. */
	void showDetail(CollectionLogItem item, CollectionLogSource source);

	/**
	 * Switches the panel to Category Focus mode for the given category.
	 * Invoked by the Statistics mode when the user clicks a category tile.
	 */
	void switchToCategoryFocus(CollectionLogCategory category);

	/** Current search query (lowercased, trimmed) — empty when not in Search mode. */
	String getSearchQuery();

	/**
	 * Current Efficient-sort selection. {@code null} means no sort selected
	 * (shell initial state); controllers should treat this as the default.
	 */
	com.collectionloghelper.EfficientSortMode getEfficientSortMode();

	/** Currently selected category in the Category Focus dropdown — {@code null} when empty. */
	CollectionLogCategory getSelectedCategory();
}
