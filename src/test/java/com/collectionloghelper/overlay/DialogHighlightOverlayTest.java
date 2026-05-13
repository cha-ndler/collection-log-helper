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
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DialogHighlightOverlay} covering:
 * - Returns null when guidance is not active
 * - Returns null when showOverlays() is false
 * - Returns null and draws nothing when targetDialogOptions is empty or null
 * - Draws highlight rectangle for a matching dialog option (pure drawing, no widget mutation)
 * - Does not draw anything for a non-matching option
 * - Continue-prompt highlight is drawn only when dialog options are active
 * - Widget state (text, textColor) is never mutated during render
 * - clear() resets all state so subsequent renders skip drawing
 */
@RunWith(MockitoJUnitRunner.class)
public class DialogHighlightOverlayTest
{
	private static final int DIALOG_OPTION_GROUP = 219;
	private static final int DIALOG_OPTION_CHILD = 1;
	private static final int NPC_DIALOG_GROUP = 231;
	private static final int NPC_DIALOG_CONTINUE_CHILD = 5;

	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private Graphics2D graphics;

	private DialogHighlightOverlay overlay;

	@Before
	public void setUp() throws Exception
	{
		when(config.showOverlays()).thenReturn(true);
		when(config.overlayColor()).thenReturn(Color.CYAN);

		java.lang.reflect.Constructor<DialogHighlightOverlay> ctor =
			DialogHighlightOverlay.class.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		overlay = ctor.newInstance(client, config);
	}

	// --- guard-clause checks ---

	@Test
	public void render_returnsNull_whenGuidanceNotActive()
	{
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoFill();
	}

	@Test
	public void render_returnsNull_whenShowOverlaysFalse()
	{
		when(config.showOverlays()).thenReturn(false);
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoFill();
	}

	@Test
	public void render_drawsNothing_whenDialogOptionsEmpty()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.emptyList());

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoFill();
	}

	@Test
	public void render_drawsNothing_whenDialogOptionsNull()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(null);

		Dimension result = overlay.render(graphics);

		assertNull(result);
		verifyNoFill();
	}

	// --- option matching ---

	@Test
	public void render_drawsHighlight_forMatchingOption()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));

		Widget option = mockOption("Yes, I would like to trade.", new Rectangle(10, 20, 200, 16));
		Widget container = mockContainer(new Widget[]{option});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verifyFillDrawn();
	}

	@Test
	public void render_doesNotDrawHighlight_forNonMatchingOption()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("teleport"));

		Widget option = mockOption("Yes, I would like to trade.", new Rectangle(10, 20, 200, 16));
		Widget container = mockContainer(new Widget[]{option});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verifyNoFill();
	}

	@Test
	public void render_neverMutatesWidgetText_forMatchingOption()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));

		Widget option = mockOption("Yes", new Rectangle(10, 20, 200, 16));
		Widget container = mockContainer(new Widget[]{option});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verify(option, never()).setText(org.mockito.ArgumentMatchers.anyString());
		verify(option, never()).setTextColor(anyInt());
	}

	@Test
	public void render_matchesCaseInsensitive()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("YES"));

		Widget option = mockOption("Yes please", new Rectangle(10, 20, 200, 16));
		Widget container = mockContainer(new Widget[]{option});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verifyFillDrawn();
	}

	@Test
	public void render_skipsOptionWithNullText()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));

		Widget nullTextOption = mock(Widget.class);
		when(nullTextOption.getText()).thenReturn(null);

		Widget container = mockContainer(new Widget[]{nullTextOption});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verifyNoFill();
	}

	// --- continue-prompt highlight ---

	@Test
	public void render_drawsContinueHighlight_whenDialogOptionsActiveAndContinueVisible()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("teleport"));

		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(null);

		Widget continueWidget = mock(Widget.class);
		when(continueWidget.isHidden()).thenReturn(false);
		when(continueWidget.getText()).thenReturn("Click here to continue");
		when(continueWidget.getBounds()).thenReturn(new Rectangle(50, 400, 300, 20));
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(continueWidget);

		overlay.render(graphics);

		verifyFillDrawn();
	}

	@Test
	public void render_doesNotDrawContinue_whenDialogOptionsEmpty()
	{
		// With empty options the outer guard (targetDialogOptionsLower.isEmpty()) short-circuits
		// before any client.getWidget() call, so no widget stubs are needed here.
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.emptyList());

		overlay.render(graphics);

		verifyNoFill();
	}

	@Test
	public void render_neverMutatesContinueWidgetTextColor()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("teleport"));

		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(null);

		Widget continueWidget = mock(Widget.class);
		when(continueWidget.isHidden()).thenReturn(false);
		when(continueWidget.getText()).thenReturn("Click here to continue");
		when(continueWidget.getBounds()).thenReturn(new Rectangle(50, 400, 300, 20));
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(continueWidget);

		overlay.render(graphics);

		verify(continueWidget, never()).setTextColor(anyInt());
		verify(continueWidget, never()).setText(org.mockito.ArgumentMatchers.anyString());
	}

	// --- clear() ---

	@Test
	public void clear_preventsRenderAfterBeingSet()
	{
		// clear() resets guidanceActive to false; the !guidanceActive guard exits before
		// any client.getWidget() call, so no widget stubs are needed.
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Collections.singletonList("yes"));
		overlay.clear();

		overlay.render(graphics);

		verifyNoFill();
	}

	// --- multiple options ---

	@Test
	public void render_highlightsMatchingOptionAmongMultiple()
	{
		overlay.setGuidanceActive(true);
		overlay.setTargetDialogOptions(Arrays.asList("bank", "yes"));

		Widget optionBank = mockOption("Use the Bank", new Rectangle(10, 20, 200, 16));
		Widget optionNo = mockOption("No thank you", new Rectangle(10, 40, 200, 16));
		Widget container = mockContainer(new Widget[]{optionBank, optionNo});
		when(client.getWidget(DIALOG_OPTION_GROUP, DIALOG_OPTION_CHILD)).thenReturn(container);
		when(client.getWidget(NPC_DIALOG_GROUP, NPC_DIALOG_CONTINUE_CHILD)).thenReturn(null);

		overlay.render(graphics);

		verify(graphics).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
	}

	// --- helpers ---

	private Widget mockOption(String text, Rectangle bounds)
	{
		Widget w = mock(Widget.class);
		when(w.getText()).thenReturn(text);
		when(w.getBounds()).thenReturn(bounds);
		return w;
	}

	private Widget mockContainer(Widget[] children)
	{
		Widget container = mock(Widget.class);
		when(container.getDynamicChildren()).thenReturn(children);
		return container;
	}

	private void verifyFillDrawn()
	{
		verify(graphics).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
	}

	private void verifyNoFill()
	{
		verify(graphics, never()).fillRect(anyInt(), anyInt(), anyInt(), anyInt());
	}
}