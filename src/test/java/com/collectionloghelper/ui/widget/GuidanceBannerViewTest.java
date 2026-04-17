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
import com.collectionloghelper.data.RequirementsChecker;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link GuidanceBannerView}.
 */
public class GuidanceBannerViewTest
{
	private RequirementsChecker requirementsChecker;
	private GuidanceBannerView view;

	/** Minimal CollectionLogSource for tests — CollectionLogSource is a final Lombok @Value class. */
	private static CollectionLogSource makeSource(String name)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 0, 0, 0,
			60, 0, null, null,
			null, 0, null, 0, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.emptyList());
	}

	@Before
	public void setUp()
	{
		requirementsChecker = Mockito.mock(RequirementsChecker.class);
		view = new GuidanceBannerView(requirementsChecker);
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void showGuidance_withSource_noUnmetRequirements_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Zulrah");
		Mockito.when(requirementsChecker.getUnmetRequirements("Zulrah"))
			.thenReturn(Collections.emptyList());
		view.showGuidance(source);
	}

	@Test
	public void showGuidance_withSource_hasUnmetRequirements_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Arrays.asList("Dragon Slayer II"));
		view.showGuidance(source);
	}

	@Test
	public void hideGuidance_doesNotThrow()
	{
		view.hideGuidance();
	}

	@Test
	public void showGuidance_nullSource_doesNotThrow()
	{
		// snapshot-then-null-check: null source hides the banner
		view.showGuidance(null);
	}

	@Test
	public void showClueGuidance_withSource_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Easy Clue Scroll");
		view.showClueGuidance(source);
	}

	@Test
	public void hideClueGuidance_doesNotThrow()
	{
		view.hideClueGuidance();
	}
}
