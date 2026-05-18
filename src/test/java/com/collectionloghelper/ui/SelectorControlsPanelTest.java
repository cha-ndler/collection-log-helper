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
import java.awt.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SelectorControlsPanel}: the helper that owns the
 * mode, AFK-filter, sort-mode, category, and search selectors. Extracted
 * from {@link CollectionLogHelperPanel} as part of issue #503 god-class
 * splits.
 */
@RunWith(MockitoJUnitRunner.class)
public class SelectorControlsPanelTest
{
	@Mock
	private CollectionLogHelperConfig config;

	@Before
	public void seedConfig()
	{
		when(config.afkFilter()).thenReturn(AfkFilter.OFF);
		when(config.efficientSortMode()).thenReturn(EfficientSortMode.EFFICIENCY);
	}

	private SelectorControlsPanel newPanel(Mode initialMode,
		AtomicReference<Mode> modeSink,
		AtomicReference<AfkFilter> afkSink,
		AtomicReference<EfficientSortMode> sortSink,
		AtomicInteger rebuildCount)
	{
		return new SelectorControlsPanel(
			config,
			initialMode,
			modeSink::set,
			afkSink::set,
			sortSink::set,
			rebuildCount::incrementAndGet);
	}

	@Test
	public void initialVisibility_efficient_showsAfkAndSort() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), new AtomicInteger());

			assertTrue(findCombo(panel, Mode.class).isVisible());
			assertTrue(findCombo(panel, AfkFilter.class).isVisible());
			assertTrue(findCombo(panel, EfficientSortMode.class).isVisible());
			assertFalse(findCombo(panel, CollectionLogCategory.class).isVisible());
			assertFalse(findField(panel).isVisible());
		});
	}

	@Test
	public void updateVisibility_searchMode_showsOnlySearchField() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), new AtomicInteger());

			panel.updateVisibility(Mode.SEARCH);

			assertFalse(findCombo(panel, AfkFilter.class).isVisible());
			assertFalse(findCombo(panel, EfficientSortMode.class).isVisible());
			assertFalse(findCombo(panel, CollectionLogCategory.class).isVisible());
			assertTrue(findField(panel).isVisible());
		});
	}

	@Test
	public void updateVisibility_categoryFocus_showsCategoryAndAfk() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), new AtomicInteger());

			panel.updateVisibility(Mode.CATEGORY_FOCUS);

			assertTrue(findCombo(panel, AfkFilter.class).isVisible());
			assertFalse(findCombo(panel, EfficientSortMode.class).isVisible());
			assertTrue(findCombo(panel, CollectionLogCategory.class).isVisible());
			assertFalse(findField(panel).isVisible());
		});
	}

	@Test
	public void selectMode_invokesModeChangedCallback() throws Exception
	{
		AtomicReference<Mode> sink = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				sink, new AtomicReference<>(), new AtomicReference<>(),
				new AtomicInteger());

			findCombo(panel, Mode.class).setSelectedItem(Mode.SEARCH);
		});
		assertEquals(Mode.SEARCH, sink.get());
	}

	@Test
	public void selectAfkFilter_invokesAfkChangedCallback() throws Exception
	{
		AtomicReference<AfkFilter> sink = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), sink, new AtomicReference<>(),
				new AtomicInteger());

			findCombo(panel, AfkFilter.class).setSelectedItem(AfkFilter.AFK);
		});
		assertEquals(AfkFilter.AFK, sink.get());
	}

	@Test
	public void selectSortMode_invokesSortChangedCallback() throws Exception
	{
		AtomicReference<EfficientSortMode> sink = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				sink, new AtomicInteger());

			findCombo(panel, EfficientSortMode.class).setSelectedItem(EfficientSortMode.KILL_TIME);
		});
		assertEquals(EfficientSortMode.KILL_TIME, sink.get());
	}

	@Test
	public void selectCategory_invokesRebuildCallback() throws Exception
	{
		AtomicInteger rebuilds = new AtomicInteger();
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), rebuilds);

			JComboBox<CollectionLogCategory> combo = findCombo(panel, CollectionLogCategory.class);
			CollectionLogCategory initial = (CollectionLogCategory) combo.getSelectedItem();
			CollectionLogCategory next = null;
			for (int i = 0; i < combo.getItemCount(); i++)
			{
				if (combo.getItemAt(i) != initial)
				{
					next = combo.getItemAt(i);
					break;
				}
			}
			assertNotNull(next);
			combo.setSelectedItem(next);
		});
		assertEquals(1, rebuilds.get());
	}

	@Test
	public void getSearchQuery_returnsLowerCasedTrimmed() throws Exception
	{
		AtomicReference<String> observed = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.SEARCH,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), new AtomicInteger());

			findField(panel).setText("  Slayer Helm  ");
			observed.set(panel.getSearchQuery());
			panel.shutDown();
		});
		assertEquals("slayer helm", observed.get());
	}

	@Test
	public void setSelectedMode_updatesCombo() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			SelectorControlsPanel panel = newPanel(Mode.EFFICIENT,
				new AtomicReference<>(), new AtomicReference<>(),
				new AtomicReference<>(), new AtomicInteger());

			panel.setSelectedMode(Mode.STATISTICS);

			assertEquals(Mode.STATISTICS, findCombo(panel, Mode.class).getSelectedItem());
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> JComboBox<T> findCombo(SelectorControlsPanel panel, Class<T> itemType)
	{
		for (Component c : panel.getComponents())
		{
			if (c instanceof JComboBox)
			{
				JComboBox<?> combo = (JComboBox<?>) c;
				if (combo.getItemCount() > 0 && itemType.isInstance(combo.getItemAt(0)))
				{
					return (JComboBox<T>) combo;
				}
			}
		}
		throw new AssertionError("JComboBox for " + itemType.getSimpleName() + " not found");
	}

	private static JTextField findField(SelectorControlsPanel panel)
	{
		for (Component c : panel.getComponents())
		{
			if (c instanceof JTextField)
			{
				return (JTextField) c;
			}
		}
		throw new AssertionError("JTextField not found");
	}
}
