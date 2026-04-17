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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.efficiency.ScoredItem;
import java.util.Collections;
import java.util.function.Consumer;
import javax.swing.JPanel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link QuickGuidePanelView}.
 */
public class QuickGuidePanelViewTest
{
	private QuickGuidePanelView view;
	private CollectionLogSource testSource;
	private ScoredItem testItem;

	@Before
	public void setUp()
	{
		Consumer<CollectionLogSource> activator = s -> {};
		Runnable deactivator = () -> {};
		view = new QuickGuidePanelView(activator, deactivator);

		testSource = makeSource("Abyssal Sire");
		testItem = new ScoredItem(testSource, 1.0, 3, "High value boss", false, 1.0, null, 0.0);
	}

	private static CollectionLogSource makeSource(String name)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 0, 0, 0,
			60, 0, null, null, null, 0, null, 0, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.emptyList());
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void create_returnsNonNullPanel()
	{
		JPanel panel = view.create(testItem, false, null);
		assertNotNull(panel);
	}

	@Test
	public void create_notGuidingSource_doesNotThrow()
	{
		view.create(testItem, false, null);
	}

	@Test
	public void create_guidingDifferentSource_doesNotThrow()
	{
		CollectionLogSource other = makeSource("Other Boss");
		view.create(testItem, true, other);
	}

	@Test
	public void create_guidingThisSource_doesNotThrow()
	{
		view.create(testItem, true, testSource);
	}

	@Test
	public void create_withNullReasoning_doesNotThrow()
	{
		ScoredItem noReasoning = new ScoredItem(testSource, 1.0, 3, null, false, 1.0, null, 0.0);
		view.create(noReasoning, false, null);
	}

	@Test
	public void create_withEmptyReasoning_doesNotThrow()
	{
		ScoredItem emptyReasoning = new ScoredItem(testSource, 1.0, 3, "", false, 1.0, null, 0.0);
		view.create(emptyReasoning, false, null);
	}

	@Test
	public void syncGuidanceState_withNullButton_doesNotThrow()
	{
		// snapshot-then-null-check: syncGuidanceState before any create() should be safe
		view.syncGuidanceState(true, testSource);
	}

	@Test
	public void syncGuidanceState_afterCreate_doesNotThrow()
	{
		view.create(testItem, false, null);
		view.syncGuidanceState(true, testSource);
	}

	@Test
	public void syncGuidanceState_stopGuidance_doesNotThrow()
	{
		view.create(testItem, true, testSource);
		view.syncGuidanceState(false, null);
	}
}
