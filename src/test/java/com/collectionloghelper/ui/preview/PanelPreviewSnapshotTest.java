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
package com.collectionloghelper.ui.preview;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.collectionloghelper.ui.CollectionLogHelperPanel.Mode;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the side panel for several scenarios and writes PNG previews to
 * {@code preview-out/} at the worktree root. Not a behavioural assertion suite;
 * it exists so the panel layout can be eyeballed (and diffed) without launching
 * the RuneLite dev client. The assertions only guard that a non-trivial image
 * was produced for each scenario.
 */
public class PanelPreviewSnapshotTest
{
	private static final int WIDTH = PluginPanel.PANEL_WIDTH;
	private static final Path OUT_DIR = Paths.get("preview-out");

	@BeforeAll
	public static void headless()
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	public void efficientDefaultScenario() throws Exception
	{
		renderScenario("efficient-default",
			PanelPreviewFixtures.scenario().mode(Mode.EFFICIENT));
	}

	@Test
	public void slayerTaskActiveScenario() throws Exception
	{
		// An active task in the default EFFICIENT mode must NOT surface the slayer
		// advisor at the top of the panel; the banner is gated to slayer context.
		renderScenario("slayer-task-active",
			PanelPreviewFixtures.scenario().mode(Mode.EFFICIENT).slayerTaskActive(true));
	}

	@Test
	public void slayerCategoryFocusScenario() throws Exception
	{
		// Category Focus on the Slayer category with an active task: this is the
		// only context where the slayer task label + strategy advisor appear.
		renderScenario("slayer-category-focus",
			PanelPreviewFixtures.scenario()
				.focusCategory(CollectionLogCategory.SLAYER)
				.slayerTaskActive(true));
	}

	@Test
	public void longLabelsCutoffStressScenario() throws Exception
	{
		renderScenario("long-labels-cutoff-stress",
			PanelPreviewFixtures.scenario()
				.mode(Mode.EFFICIENT)
				.slayerTaskActive(true)
				.longLabels(true));
	}

	private void renderScenario(String name, PanelPreviewFixtures.Scenario scenario) throws Exception
	{
		BufferedImage img = buildAndRenderOnEdt(scenario);
		Path out = OUT_DIR.resolve(name + ".png");
		PanelSnapshot.writePng(img, out);

		assertTrue(Files.exists(out), "PNG not written: " + out.toAbsolutePath());
		assertTrue(Files.size(out) > 0, "PNG is empty: " + out.toAbsolutePath());
		assertEquals(WIDTH, img.getWidth(), "render width must equal PANEL_WIDTH");
		assertTrue(img.getHeight() > 50,
			"render height too small (" + img.getHeight() + ") for " + name);
	}

	/**
	 * Builds the panel and renders it on the EDT, surfacing any exception.
	 *
	 * <p>{@code buildPanel} calls {@code setMode(...)} which schedules the list
	 * rebuild via {@code invokeLater}. Building happens in one EDT task; a second
	 * EDT task then renders. Because the EDT runs tasks FIFO, the queued rebuild
	 * (and its mode-controller content + slayer-strategy refresh) completes
	 * before the render task runs, so the snapshot reflects populated content.
	 */
	private BufferedImage buildAndRenderOnEdt(PanelPreviewFixtures.Scenario scenario) throws Exception
	{
		AtomicReference<CollectionLogHelperPanel> panelRef = new AtomicReference<>();
		AtomicReference<BufferedImage> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				panelRef.set(PanelPreviewFixtures.buildPanel(scenario));
			}
			catch (Exception e)
			{
				error.set(e);
			}
		});
		if (error.get() != null)
		{
			throw error.get();
		}

		// Second EDT task: the queued rebuild has now run, so render populated.
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				result.set(PanelSnapshot.render(panelRef.get(), WIDTH));
			}
			catch (Exception e)
			{
				error.set(e);
			}
		});
		if (error.get() != null)
		{
			throw error.get();
		}
		return result.get();
	}
}
