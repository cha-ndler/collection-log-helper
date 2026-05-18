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

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ListContainerScrollPane}: the card-layout content
 * host whose preferred size tracks the visible child card. Extracted from
 * {@link com.collectionloghelper.ui.CollectionLogHelperPanel} as part of
 * issue #503 god-class splits.
 */
public class ListContainerScrollPaneTest
{
	private ListContainerScrollPane container;
	private CardLayout cardLayout;

	@Before
	public void setUp()
	{
		cardLayout = new CardLayout();
		container = new ListContainerScrollPane(cardLayout);
	}

	private static JPanel cardOfSize(int width, int height)
	{
		JPanel card = new JPanel();
		card.setPreferredSize(new Dimension(width, height));
		return card;
	}

	@Test
	public void preferredSizeMatchesVisibleCardWhenSingleCardShown()
	{
		container.add(cardOfSize(180, 400), "only");

		Dimension result = container.getPreferredSize();

		assertEquals(180, result.width);
		assertEquals(400, result.height);
	}

	@Test
	public void preferredSizeAddsContainerInsetsToVisibleCard()
	{
		container.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 10));
		container.add(cardOfSize(100, 200), "only");

		Dimension result = container.getPreferredSize();

		// width = card.width + insets.left + insets.right = 100 + 6 + 10
		assertEquals(116, result.width);
		// height = card.height + insets.top + insets.bottom = 200 + 4 + 8
		assertEquals(212, result.height);
	}

	@Test
	public void preferredSizeFollowsCardLayoutShow()
	{
		JPanel small = cardOfSize(150, 100);
		JPanel large = cardOfSize(250, 600);
		container.add(small, "small");
		container.add(large, "large");

		// CardLayout shows first added card by default ("small").
		Dimension before = container.getPreferredSize();
		assertEquals(150, before.width);
		assertEquals(100, before.height);

		cardLayout.show(container, "large");

		Dimension after = container.getPreferredSize();
		assertEquals(250, after.width);
		assertEquals(600, after.height);
	}

	@Test
	public void preferredSizeIgnoresHiddenChildAndPicksFirstVisible()
	{
		JPanel hidden = cardOfSize(999, 999);
		JPanel visible = cardOfSize(120, 240);
		container.add(hidden, "hidden");
		container.add(visible, "visible");
		// CardLayout marks the first added card visible by default;
		// flip the flag explicitly after add to simulate a card that
		// has been hidden by CardLayout#show on another card.
		hidden.setVisible(false);
		visible.setVisible(true);

		Dimension result = container.getPreferredSize();

		assertEquals(120, result.width);
		assertEquals(240, result.height);
	}

	@Test
	public void preferredSizeFallsBackToSuperWhenNoChildVisible()
	{
		JPanel hidden = cardOfSize(500, 500);
		container.add(hidden, "hidden");
		// Force the only child invisible after the layout has had a
		// chance to mark it visible during add().
		hidden.setVisible(false);

		Dimension result = container.getPreferredSize();

		// With no visible child the override falls through to
		// super.getPreferredSize(), which delegates to the installed
		// CardLayout. CardLayout sizes itself to the largest card
		// regardless of visibility, so we expect the lone card's
		// preferred size back through that path.
		assertEquals(500, result.width);
		assertEquals(500, result.height);
	}
}
