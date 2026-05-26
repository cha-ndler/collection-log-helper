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
import com.collectionloghelper.guidance.RequiredItemDisplay;
import com.collectionloghelper.guidance.RequiredItemDisplay.Status;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import com.collectionloghelper.ui.CollectionLogHelperPanel.Mode;
import com.collectionloghelper.ui.widget.SourceRecommendedItemsChipPanel;
import com.collectionloghelper.ui.widget.StepProgressView;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

	/**
	 * Standalone render of just the source-level "Recommended:" chip strip with
	 * enough chips (6) to force wrapping at the real 225px side-panel width. This
	 * proves the heading sits on its own full-width line and the chips wrap onto
	 * additional rows instead of clipping at the right edge (#573 fix). The chip
	 * panel is hosted inside a vertical-BoxLayout container that mirrors the real
	 * call sites (GuidanceBannerView / ItemDetailPanel).
	 */
	@Test
	public void recommendedStripWrapScenario() throws Exception
	{
		AtomicReference<JPanel> hostRef = new AtomicReference<>();
		AtomicReference<BufferedImage> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();

		// Ten item IDs: a 30px chip plus 3px gaps fits about six per 225px row, so
		// ten chips must wrap onto multiple rows. Real OSRS item IDs (gear) so
		// icons load if present.
		final List<Integer> itemIds = Arrays.asList(
			11832, 11834, 11836, 4151, 11802, 12954, 11806, 11808, 11810, 11812);

		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				ItemManager itemManager = Mockito.mock(ItemManager.class);
				Mockito.when(itemManager.getImage(Mockito.anyInt())).thenAnswer(
					inv -> new AsyncBufferedImage(null, 32, 32, BufferedImage.TYPE_INT_ARGB));

				SourceRecommendedItemsChipPanel strip =
					new SourceRecommendedItemsChipPanel(itemManager);
				strip.update(itemIds);

				JPanel host = new JPanel();
				host.setLayout(new javax.swing.BoxLayout(host, javax.swing.BoxLayout.Y_AXIS));
				host.add(strip);
				hostRef.set(host);
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

		// Second EDT task: the strip's update() invokeLater has now run.
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				result.set(PanelSnapshot.render(hostRef.get(), WIDTH));
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

		BufferedImage img = result.get();
		Path out = OUT_DIR.resolve("recommended-strip-wrap.png");
		PanelSnapshot.writePng(img, out);

		assertTrue(Files.exists(out), "PNG not written: " + out.toAbsolutePath());
		assertTrue(Files.size(out) > 0, "PNG is empty: " + out.toAbsolutePath());
		assertEquals(WIDTH, img.getWidth(), "render width must equal PANEL_WIDTH");
		// Heading line + at least two wrapped chip rows => clearly taller than a
		// single 30px chip row. Guards against the FlowLayout single-line-height pit.
		// CHIP_SIZE is 30 (28px icon + 1px border each side); two rows + heading
		// must exceed 60px.
		assertTrue(img.getHeight() > 60,
			"strip height (" + img.getHeight() + ") too short to have wrapped");
	}

	/**
	 * Phase 1 guidance items redesign: renders a {@link StepProgressView} (flat
	 * layout) with a mix of required and recommended items across all three
	 * statuses (HELD / IN_BANK / MISSING), plus one recommended item whose id
	 * duplicates a required item (to prove the dedup filter drops it). Proves the
	 * compact color-coded text list renders icon-free, three-coloured, deduped,
	 * and unclipped at the real 225px side-panel width.
	 */
	@Test
	public void guidanceItemsTextListScenario() throws Exception
	{
		final int heldId = 590;     // Tinderbox
		final int inBankId = 3438;  // Pyre logs
		final int missingId = 3392; // Loar remains
		final int recMissingId = 4151; // Abyssal whip (recommended, missing)

		final List<RequiredItemDisplay> required = Arrays.asList(
			new RequiredItemDisplay(heldId, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(inBankId, "Pyre logs", Status.IN_BANK),
			new RequiredItemDisplay(missingId, "Loar remains", Status.MISSING));

		// Recommended list includes a duplicate of the HELD required item
		// (heldId) which the dedup filter must drop, plus one genuinely new item.
		final List<RequiredItemDisplay> recommended = Arrays.asList(
			new RequiredItemDisplay(heldId, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(recMissingId, "Abyssal whip", Status.MISSING));

		AtomicReference<StepProgressView> viewRef = new AtomicReference<>();
		AtomicReference<BufferedImage> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				ItemManager itemManager = Mockito.mock(ItemManager.class);
				Mockito.when(itemManager.getImage(Mockito.anyInt())).thenReturn(null);

				StepProgressView view = new StepProgressView(itemManager);
				view.showStep(2, 6, "Light the pyre and defeat the shade", false,
					required, recommended, java.util.Collections.emptyList());
				viewRef.set(view);
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

		// Second EDT task: the showStep invokeLater has now run and populated rows.
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				result.set(PanelSnapshot.render(viewRef.get(), WIDTH));
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

		BufferedImage img = result.get();
		Path out = OUT_DIR.resolve("guidance-items-textlist.png");
		PanelSnapshot.writePng(img, out);

		assertTrue(Files.exists(out), "PNG not written: " + out.toAbsolutePath());
		assertTrue(Files.size(out) > 0, "PNG is empty: " + out.toAbsolutePath());
		assertEquals(WIDTH, img.getWidth(), "render width must equal PANEL_WIDTH");
		assertTrue(img.getHeight() > 50, "render height too small (" + img.getHeight() + ")");
	}

	/**
	 * Phase 2 guidance-items redesign (Shades of Mort'ton pilot): renders a
	 * {@link StepProgressView} flat layout for the burn step. "Items needed" carries
	 * the on-site Fiyr remains tagged with "(from activity)"; "Recommended" shows the
	 * single convenience aid (Flamtaer bag) instead of the old material list. Proves
	 * the aids-only recommended section and the muted on-site tag render correctly.
	 */
	@Test
	public void shadesActivityObtainableScenario() throws Exception
	{
		final int fiyrRemainsId = 3404;  // shade remains, obtained on-site
		final int tinderboxId = 590;     // brought material
		final int flamtaerBagId = 25630; // convenience aid (the one recommended item)

		final List<RequiredItemDisplay> required = Arrays.asList(
			new RequiredItemDisplay(tinderboxId, "Tinderbox", Status.HELD),
			new RequiredItemDisplay(fiyrRemainsId, "Fiyr remains", Status.HELD));
		final List<RequiredItemDisplay> recommended = Arrays.asList(
			new RequiredItemDisplay(flamtaerBagId, "Flamtaer bag", Status.IN_BANK));
		final java.util.Set<Integer> activityIds = java.util.Collections.singleton(fiyrRemainsId);

		AtomicReference<StepProgressView> viewRef = new AtomicReference<>();
		AtomicReference<BufferedImage> result = new AtomicReference<>();
		AtomicReference<Exception> error = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				ItemManager itemManager = Mockito.mock(ItemManager.class);
				Mockito.when(itemManager.getImage(Mockito.anyInt())).thenReturn(null);

				StepProgressView view = new StepProgressView(itemManager);
				view.showStep(2, 6, "Burn shade remains on the funeral pyres", false,
					required, recommended, java.util.Collections.emptyList(), activityIds);
				viewRef.set(view);
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

		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				result.set(PanelSnapshot.render(viewRef.get(), WIDTH));
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

		BufferedImage img = result.get();
		Path out = OUT_DIR.resolve("shades-activity-obtainable.png");
		PanelSnapshot.writePng(img, out);

		assertTrue(Files.exists(out), "PNG not written: " + out.toAbsolutePath());
		assertTrue(Files.size(out) > 0, "PNG is empty: " + out.toAbsolutePath());
		assertEquals(WIDTH, img.getWidth(), "render width must equal PANEL_WIDTH");
		assertTrue(img.getHeight() > 50, "render height too small (" + img.getHeight() + ")");
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
