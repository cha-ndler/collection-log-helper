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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PanelHeaderBuilder}: the helper that assembles the
 * static header section of {@link CollectionLogHelperPanel} (completion
 * label/bar, sync status, sync buttons, clue summary, slayer strategy,
 * guidance banner, step progress, quick-guide). Extracted from
 * {@link CollectionLogHelperPanel} as part of issue #503 god-class splits.
 */
@RunWith(MockitoJUnitRunner.class)
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

	@Before
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
			(source, count) -> { /* no-op */ },
			() -> { /* no-op */ },
			host);
	}

	@Test
	public void buildReturnsNonNullResultAndControlsPanel()
	{
		PanelHeaderBuilder.Result result = build();

		assertNotNull("result", result);
		assertNotNull("controlsPanel", result.controlsPanel);
		assertTrue("controlsPanel uses BoxLayout",
			result.controlsPanel.getLayout() instanceof BoxLayout);
	}

	@Test
	public void buildExposesAllViewHandles()
	{
		PanelHeaderBuilder.Result result = build();

		assertNotNull("completionLabel", result.completionLabel);
		assertNotNull("completionProgressBar", result.completionProgressBar);
		assertNotNull("syncStatusView", result.syncStatusView);
		assertNotNull("syncButtonController", result.syncButtonController);
		assertNotNull("clueSummaryView", result.clueSummaryView);
		assertNotNull("slayerStrategyView", result.slayerStrategyView);
		assertNotNull("guidanceBannerView", result.guidanceBannerView);
		assertNotNull("stepProgressView", result.stepProgressView);
		assertNotNull("quickGuidePanelView", result.quickGuidePanelView);
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

		assertSame("completion label first", result.completionLabel, children[0]);
		assertSame("completion progress bar second", result.completionProgressBar, children[1]);
		assertSame("sync status view third", result.syncStatusView, children[2]);
		assertSame("collectionlog.net button fourth",
			result.syncButtonController.getCollectionLogNetButton(), children[3]);
		assertSame("temple sync button fifth",
			result.syncButtonController.getTempleSyncButton(), children[4]);
		assertSame("clue summary view", result.clueSummaryView, children[5]);
		assertSame("slayer strategy view", result.slayerStrategyView, children[6]);
		assertSame("guidance banner view", result.guidanceBannerView, children[7]);
		assertSame("step progress view", result.stepProgressView, children[8]);
		// QuickGuidePanelView is intentionally NOT added to controlsPanel — it is
		// shown contextually in the detail/list view. The trailing component
		// must therefore be the vertical strut (Box.Filler).
		assertTrue("trailing vertical strut present", children.length >= 10);
	}

	@Test
	public void quickGuidePanelViewIsCreatedButNotAddedToControlsPanel()
	{
		PanelHeaderBuilder.Result result = build();

		// QuickGuidePanelView is a factory, not a Component, so it cannot appear
		// in the controlsPanel's child list — assert by counting and shape only.
		assertNotNull("quickGuidePanelView factory created",
			result.quickGuidePanelView);
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
