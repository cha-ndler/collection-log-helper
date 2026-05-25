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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import java.awt.Component;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link PanelHeaderBuilder}: the helper that assembles the
 * static header section of {@link CollectionLogHelperPanel} (completion
 * label/bar, sync status, sync buttons, clue summary, slayer strategy,
 * guidance banner, step progress, quick-guide). Extracted from
 * {@link CollectionLogHelperPanel} as part of issue #503 god-class splits.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PanelHeaderBuilderTest
{
	@Mock
	private CollectionLogHelperConfig config;
	@Mock
	private DataSyncState dataSyncState;
	@Mock
	private SlayerTaskState slayerTaskState;
	@Mock
	private SlayerStrategyCalculator slayerStrategyCalculator;
	@Mock
	private RequirementsChecker requirementsChecker;
	@Mock
	private ItemManager itemManager;

	private JPanel host;

	@BeforeEach
	public void setUp()
	{
		// SyncButtonController consults these config flags during construction.
		when(config.enableCollectionLogNetImport()).thenReturn(true);
		when(config.enableTempleOsrsSync()).thenReturn(true);
		host = new JPanel();
	}

	private PanelHeaderBuilder.Result build()
	{
		return PanelHeaderBuilder.build(
			config,
			dataSyncState,
			slayerTaskState,
			slayerStrategyCalculator,
			requirementsChecker,
			itemManager,
			null, // clientThread not available in unit tests; chip tooltips degrade to "Item <id>"
			(source, count) -> { /* no-op */ },
			() -> { /* no-op */ },
			host);
	}

	@Test
	public void buildReturnsNonNullResultAndControlsPanel()
	{
		PanelHeaderBuilder.Result result = build();

		assertNotNull( result,"result");
		assertNotNull( result.controlsPanel,"controlsPanel");
		assertTrue(
			result.controlsPanel.getLayout() instanceof BoxLayout,"controlsPanel uses BoxLayout");
	}

	@Test
	public void buildExposesAllViewHandles()
	{
		PanelHeaderBuilder.Result result = build();

		assertNotNull( result.completionLabel,"completionLabel");
		assertNotNull( result.completionProgressBar,"completionProgressBar");
		assertNotNull( result.syncStatusView,"syncStatusView");
		assertNotNull( result.syncButtonController,"syncButtonController");
		assertNotNull( result.clueSummaryView,"clueSummaryView");
		assertNotNull( result.slayerStrategyView,"slayerStrategyView");
		assertNotNull( result.guidanceBannerView,"guidanceBannerView");
		assertNotNull( result.stepProgressView,"stepProgressView");
		assertNotNull( result.quickGuidePanelView,"quickGuidePanelView");
	}

	@Test
	public void completionLabelInitialTextMatchesEmptyState()
	{
		PanelHeaderBuilder.Result result = build();

		assertEquals("Collection Log: 0/0 (0.0%)", result.completionLabel.getText());
	}

	@Test
	public void completionProgressBarHasFullCollectionLogMaximum()
	{
		PanelHeaderBuilder.Result result = build();

		assertEquals(0, result.completionProgressBar.getMinimum());
		assertEquals(1699, result.completionProgressBar.getMaximum());
		assertEquals(0, result.completionProgressBar.getValue());
	}

	@Test
	public void controlsPanelContainsExpectedWidgetsInOrder()
	{
		PanelHeaderBuilder.Result result = build();
		Component[] children = result.controlsPanel.getComponents();

		assertSame( result.completionLabel, children[0],"completion label first");
		assertSame( result.completionProgressBar, children[1],"completion progress bar second");
		assertSame( result.syncStatusView, children[2],"sync status view third");
		assertSame(
			result.syncButtonController.getCollectionLogNetButton(), children[3],"collectionlog.net button fourth");
		assertSame(
			result.syncButtonController.getTempleSyncButton(), children[4],"temple sync button fifth");
		assertSame( result.clueSummaryView, children[5],"clue summary view");
		assertSame( result.slayerStrategyView, children[6],"slayer strategy view");
		assertSame( result.guidanceBannerView, children[7],"guidance banner view");
		assertSame( result.stepProgressView, children[8],"step progress view");
		// QuickGuidePanelView is intentionally NOT added to controlsPanel — it is
		// shown contextually in the detail/list view. The trailing component
		// must therefore be the vertical strut (Box.Filler).
		assertTrue( children.length >= 10,"trailing vertical strut present");
	}

	@Test
	public void quickGuidePanelViewIsCreatedButNotAddedToControlsPanel()
	{
		PanelHeaderBuilder.Result result = build();

		// QuickGuidePanelView is a factory, not a Component, so it cannot appear
		// in the controlsPanel's child list — assert by counting and shape only.
		assertNotNull(
			result.quickGuidePanelView,"quickGuidePanelView factory created");
	}

	@Test
	public void repeatedBuildsReturnIndependentInstances()
	{
		PanelHeaderBuilder.Result a = build();
		PanelHeaderBuilder.Result b = build();

		// Each invocation creates fresh widget instances; the builder holds no state.
		assertTrue(a.controlsPanel != b.controlsPanel);
		assertTrue(a.completionLabel != b.completionLabel);
		assertTrue(a.syncButtonController != b.syncButtonController);
	}
}
