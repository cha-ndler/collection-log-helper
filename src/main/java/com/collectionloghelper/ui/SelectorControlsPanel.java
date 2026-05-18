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

import com.collectionloghelper.AfkFilter;
import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.EfficientSortMode;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.ui.CollectionLogHelperPanel.Mode;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Owns the five selector widgets shown in the panel's control header:
 * mode, AFK-filter, sort-mode, category, and search-text. Extracted from
 * {@link CollectionLogHelperPanel} as part of the issue #503 god-class split.
 *
 * <p>This class encapsulates widget construction, listener wiring, the
 * search-field debounce timer, and the visibility rules that decide which
 * selectors are shown for the current {@link Mode}. The owning panel keeps
 * authority over the {@code currentMode} field; this class only reports
 * changes via callbacks supplied at construction time.</p>
 *
 * <p>It is intentionally not a Guice {@code @Singleton} — it is constructed
 * inside the panel and bound to that panel's lifecycle (its debounce timer is
 * stopped via {@link #shutDown()}).</p>
 */
public class SelectorControlsPanel extends JPanel
{
	private static final int SELECTOR_HEIGHT = 28;
	private static final int SEARCH_DEBOUNCE_MS = 200;

	private final JComboBox<Mode> modeSelector;
	private final JComboBox<AfkFilter> afkFilterSelector;
	private final JComboBox<EfficientSortMode> sortSelector;
	private final JComboBox<CollectionLogCategory> categorySelector;
	private final JTextField searchField;
	private final Timer searchDebounceTimer;

	/**
	 * Constructs the selector controls and wires up listeners. Callbacks are
	 * invoked directly when the corresponding selector changes; the search
	 * field's changes are debounced.
	 *
	 * @param config                  plugin config used to seed initial
	 *                                AFK-filter and sort-mode selections
	 * @param initialMode             mode used to seed initial visibility — the
	 *                                caller still owns {@code currentMode}
	 * @param onModeChanged           invoked when the user picks a new mode
	 * @param onAfkFilterChanged      invoked when the AFK filter changes
	 * @param onSortChanged           invoked when the sort mode changes
	 * @param onSearchOrFilterChanged invoked when the category or search field
	 *                                changes — both call this so the panel can
	 *                                rebuild
	 */
	public SelectorControlsPanel(CollectionLogHelperConfig config,
		Mode initialMode,
		Consumer<Mode> onModeChanged,
		Consumer<AfkFilter> onAfkFilterChanged,
		Consumer<EfficientSortMode> onSortChanged,
		Runnable onSearchOrFilterChanged)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);

		modeSelector = new JComboBox<>(Mode.values());
		modeSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, SELECTOR_HEIGHT));
		modeSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				onModeChanged.accept((Mode) e.getItem());
			}
		});
		add(modeSelector);

		afkFilterSelector = new JComboBox<>(AfkFilter.values());
		afkFilterSelector.setSelectedItem(config.afkFilter());
		afkFilterSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, SELECTOR_HEIGHT));
		afkFilterSelector.setVisible(initialMode == Mode.EFFICIENT || initialMode == Mode.PET_HUNT);
		afkFilterSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				onAfkFilterChanged.accept((AfkFilter) e.getItem());
			}
		});
		add(afkFilterSelector);

		sortSelector = new JComboBox<>(EfficientSortMode.values());
		sortSelector.setSelectedItem(config.efficientSortMode());
		sortSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, SELECTOR_HEIGHT));
		sortSelector.setVisible(initialMode == Mode.EFFICIENT);
		sortSelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				EfficientSortMode selected = (EfficientSortMode) sortSelector.getSelectedItem();
				if (selected != null)
				{
					onSortChanged.accept(selected);
				}
			}
		});
		add(sortSelector);

		categorySelector = new JComboBox<>(CollectionLogCategory.values());
		categorySelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, SELECTOR_HEIGHT));
		categorySelector.setVisible(false);
		categorySelector.addItemListener(e ->
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				onSearchOrFilterChanged.run();
			}
		});
		add(categorySelector);

		searchField = new JTextField();
		searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, SELECTOR_HEIGHT));
		searchField.setVisible(false);
		searchField.setToolTipText("Search by item name or source name");
		searchDebounceTimer = new Timer(SEARCH_DEBOUNCE_MS, e -> onSearchOrFilterChanged.run());
		searchDebounceTimer.setRepeats(false);
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchDebounceTimer.restart();
			}
		});
		add(searchField);
	}

	/**
	 * Updates which selectors are visible based on the current mode. AFK filter
	 * is shown for EFFICIENT, PET_HUNT, and CATEGORY_FOCUS modes; sort selector
	 * is shown only in EFFICIENT; category selector only in CATEGORY_FOCUS;
	 * search field only in SEARCH.
	 */
	public void updateVisibility(Mode currentMode)
	{
		afkFilterSelector.setVisible(currentMode == Mode.EFFICIENT || currentMode == Mode.PET_HUNT
			|| currentMode == Mode.CATEGORY_FOCUS);
		sortSelector.setVisible(currentMode == Mode.EFFICIENT);
		categorySelector.setVisible(currentMode == Mode.CATEGORY_FOCUS);
		searchField.setVisible(currentMode == Mode.SEARCH);
	}

	/** Programmatically selects the given mode in the mode combo. */
	public void setSelectedMode(Mode mode)
	{
		modeSelector.setSelectedItem(mode);
	}

	/** Programmatically selects the given category in the category combo. */
	public void setSelectedCategory(CollectionLogCategory category)
	{
		categorySelector.setSelectedItem(category);
	}

	/** Returns the lower-cased, trimmed search query, or empty string. */
	public String getSearchQuery()
	{
		return searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
	}

	/** Returns the currently selected sort mode (may be {@code null}). */
	public EfficientSortMode getSelectedSortMode()
	{
		return (EfficientSortMode) sortSelector.getSelectedItem();
	}

	/** Returns the currently selected category (may be {@code null}). */
	public CollectionLogCategory getSelectedCategory()
	{
		return (CollectionLogCategory) categorySelector.getSelectedItem();
	}

	/** Stops the search-debounce timer. Call from panel shutdown. */
	public void shutDown()
	{
		searchDebounceTimer.stop();
	}
}
