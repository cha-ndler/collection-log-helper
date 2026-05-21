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
import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.player.DiaryRegion;
import com.collectionloghelper.player.DiaryTier;
import com.collectionloghelper.player.DiaryTierState;
import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PlayerQuestProgressState;
import com.collectionloghelper.player.PohTeleport;
import com.collectionloghelper.player.PohTeleportInventory;
import com.collectionloghelper.player.QuestSubMilestone;
import com.collectionloghelper.player.SkillCapePerk;
import com.collectionloghelper.player.SkillCapePerkState;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PlayerCapabilityDebugOverlay} covering:
 *
 * <ul>
 *   <li>Guard clause when config flag is disabled.</li>
 *   <li>Account summary fields (combat, skills, slayer task, quest counts, POH).</li>
 *   <li>Tier C1–C5 sections render labelled rows for detected state, and
 *       {@code (none ...)} placeholders when state is empty.</li>
 *   <li>{@code (error)} fallback when a section's data source throws.</li>
 *   <li>Null {@code localPlayer} does not throw.</li>
 *   <li>Quest count line uses the enum-derived ratio (finished/total) plus the
 *       in-game quest-points varplayer — guard against the #487 regression.</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerCapabilityDebugOverlayTest
{
	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private SlayerTaskState slayerTaskState;

	@Mock
	private PohTeleportInventory pohTeleportInventory;

	@Mock
	private EquippedItemState equippedItemState;

	@Mock
	private DiaryTierState diaryTierState;

	@Mock
	private SkillCapePerkState skillCapePerkState;

	@Mock
	private PlayerQuestProgressState questProgressState;

	/** Mock Graphics2D — suitable only for guard-clause tests (no actual rendering). */
	@Mock
	private Graphics2D mockGraphics;

	/**
	 * Real Graphics2D backed by a BufferedImage, required for tests that call
	 * {@code super.render(graphics)} which internally calls
	 * {@code graphics.getFontMetrics()}.
	 */
	private Graphics2D realGraphics;

	private PlayerCapabilityDebugOverlay overlay;

	@Before
	public void setUp() throws Exception
	{
		java.lang.reflect.Constructor<PlayerCapabilityDebugOverlay> ctor =
			PlayerCapabilityDebugOverlay.class.getDeclaredConstructor(
				Client.class, CollectionLogHelperConfig.class, SlayerTaskState.class,
				PohTeleportInventory.class, EquippedItemState.class,
				DiaryTierState.class, SkillCapePerkState.class,
				PlayerQuestProgressState.class);
		ctor.setAccessible(true);
		overlay = ctor.newInstance(client, config, slayerTaskState,
			pohTeleportInventory, equippedItemState,
			diaryTierState, skillCapePerkState, questProgressState);

		// OverlayPanel.render() clears panel children at the end of each frame
		// by default. Disable that here so tests can inspect the rendered rows
		// after calling overlay.render(...).
		overlay.setClearChildren(false);

		// Default mock behaviour: empty / false for all Tier-C state. Individual
		// tests override as needed.
		when(pohTeleportInventory.getAvailableTeleports()).thenReturn(Collections.emptySet());
		when(equippedItemState.getEquippedItems()).thenReturn(Collections.emptySet());
		when(diaryTierState.hasDiary(any(), any())).thenReturn(false);
		when(skillCapePerkState.hasPerkAvailable(any())).thenReturn(false);
		when(questProgressState.hasSubProgress(any())).thenReturn(false);

		BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		realGraphics = img.createGraphics();
	}

	// -------------------------------------------------------------------------
	// Guard clause: disabled in config
	// -------------------------------------------------------------------------

	@Test
	public void render_returnsNull_whenConfigDisabled()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(false);

		Dimension result = overlay.render(mockGraphics);

		assertNull(result);
	}

	// -------------------------------------------------------------------------
	// Enabled path — uses real Graphics2D so FontMetrics is available
	// -------------------------------------------------------------------------

	@Test
	public void render_returnsNonNull_whenEnabled()
	{
		enableWithBaseStubs();

		Dimension result = overlay.render(realGraphics);

		assertNotNull(result);
	}

	// -------------------------------------------------------------------------
	// Null localPlayer — must not throw
	// -------------------------------------------------------------------------

	@Test
	public void render_handlesNullLocalPlayer_gracefully()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);
		when(client.getLocalPlayer()).thenReturn(null);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);
		when(client.getVarpValue(anyInt())).thenReturn(0);
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		// Must complete without NullPointerException; combat level shown as "?"
		Dimension result = overlay.render(realGraphics);
		assertNotNull(result);
	}

	// -------------------------------------------------------------------------
	// Slayer task rows
	// -------------------------------------------------------------------------

	@Test
	public void render_includesTaskRow_whenTaskActive()
	{
		enableWithBaseStubs();
		when(slayerTaskState.isTaskActive()).thenReturn(true);
		when(slayerTaskState.getCreatureName()).thenReturn("Abyssal demons");
		when(slayerTaskState.getRemaining()).thenReturn(150);

		overlay.render(realGraphics);

		assertEquals("expected Task row to include creature name and remaining",
			"Abyssal demons x150", rightForLeft(panelRows(), "Task"));
	}

	@Test
	public void render_showsNoneTask_whenNoTaskActive()
	{
		enableWithBaseStubs();
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		overlay.render(realGraphics);

		assertTrue("expected Task row to say 'none' when no slayer task is active",
			rightForLeft(panelRows(), "Task").contains("none"));
	}

	// -------------------------------------------------------------------------
	// #487 — Quest count rendering
	// -------------------------------------------------------------------------

	@Test
	public void render_includesQuestRatio_andQuestPointsLine()
	{
		enableWithBaseStubs();
		when(client.getVarpValue(VarPlayer.QUEST_POINTS)).thenReturn(333);

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		String questsValue = rightForLeft(rows, "Quests");
		assertTrue("expected Quests row to render as <finished>/<total>, got: " + questsValue,
			questsValue.matches("\\d+/\\d+"));
		assertEquals("expected Quests denominator to equal Quest.values().length",
			Quest.values().length, parseDenominator(questsValue));

		assertEquals("expected Quest points to match VarPlayer.QUEST_POINTS",
			"333", rightForLeft(rows, "Quest points"));

		// #487 guard: ensure the old "Quest entries" label no longer ships.
		for (Row r : rows)
		{
			assertFalse("did not expect legacy 'Quest entries' label after #487 fix",
				"Quest entries".equals(r.left));
		}
	}

	// -------------------------------------------------------------------------
	// #486 — Tier C sections each render a header + content
	// -------------------------------------------------------------------------

	@Test
	public void render_pohTeleportSection_emptyState_showsNoneDetected()
	{
		enableWithBaseStubs();
		when(pohTeleportInventory.getAvailableTeleports()).thenReturn(Collections.emptySet());

		overlay.render(realGraphics);

		List<String> lefts = panelLeftStrings();
		assertTrue("expected POH Teleports section header",
			lefts.stream().anyMatch(s -> s.startsWith("POH Teleports")));
		assertTrue("expected '(none detected)' placeholder when teleport set is empty",
			lefts.contains("(none detected)"));
	}

	@Test
	public void render_pohTeleportSection_populated_listsTeleports()
	{
		enableWithBaseStubs();
		PohTeleport sample = PohTeleport.values()[0];
		when(pohTeleportInventory.getAvailableTeleports())
			.thenReturn(EnumSet.of(sample));

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		assertTrue("expected a row for the detected POH teleport",
			rows.stream().anyMatch(r -> "yes".equals(r.right)
				&& r.left != null
				&& !r.left.equals("POH built")));
	}

	@Test
	public void render_equippedSection_rendersCount()
	{
		enableWithBaseStubs();
		Set<Integer> equipped = new HashSet<>(Arrays.asList(4151, 11832, 11834, 11840, 9185));
		when(equippedItemState.getEquippedItems()).thenReturn(equipped);

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		assertEquals("expected items-equipped count to reflect snapshot size",
			"5", rightForLeft(rows, "Items equipped"));
	}

	@Test
	public void render_diarySection_rendersHighestCompletedTier()
	{
		enableWithBaseStubs();
		// Stub: Ardougne ELITE is the highest completed; everything else false.
		when(diaryTierState.hasDiary(eq(DiaryRegion.ARDOUGNE), eq(DiaryTier.EASY))).thenReturn(true);
		when(diaryTierState.hasDiary(eq(DiaryRegion.ARDOUGNE), eq(DiaryTier.MEDIUM))).thenReturn(true);
		when(diaryTierState.hasDiary(eq(DiaryRegion.ARDOUGNE), eq(DiaryTier.HARD))).thenReturn(true);
		when(diaryTierState.hasDiary(eq(DiaryRegion.ARDOUGNE), eq(DiaryTier.ELITE))).thenReturn(true);

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		assertEquals("expected Ardougne to render as ELITE",
			"ELITE", rightForLeft(rows, humanise("ARDOUGNE")));
		// Other regions still appear with "-"
		assertEquals("expected Desert region (no diary stubbed) to render as '-'",
			"-", rightForLeft(rows, humanise("DESERT")));
	}

	@Test
	public void render_capePerkSection_listsAvailablePerks()
	{
		enableWithBaseStubs();
		SkillCapePerk sample = SkillCapePerk.values()[0];
		when(skillCapePerkState.hasPerkAvailable(sample)).thenReturn(true);

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		assertTrue("expected at least one cape-perk row marked 'yes'",
			rows.stream().anyMatch(r -> "yes".equals(r.right)
				&& humanise(sample.name()).equals(r.left)));
	}

	@Test
	public void render_capePerkSection_empty_showsNoneAvailable()
	{
		enableWithBaseStubs();
		// All perks return false by default in setUp.

		overlay.render(realGraphics);

		assertTrue("expected '(none available)' placeholder when no cape perks detected",
			panelLeftStrings().contains("(none available)"));
	}

	@Test
	public void render_questSubMilestoneSection_listsCompletedMilestones()
	{
		enableWithBaseStubs();
		QuestSubMilestone sample = QuestSubMilestone.values()[0];
		when(questProgressState.hasSubProgress(sample)).thenReturn(true);

		overlay.render(realGraphics);

		List<Row> rows = panelRows();
		assertTrue("expected at least one quest sub-milestone row marked 'yes'",
			rows.stream().anyMatch(r -> "yes".equals(r.right)
				&& humanise(sample.name()).equals(r.left)));
	}

	@Test
	public void render_questSubMilestoneSection_empty_showsNoneCompleted()
	{
		enableWithBaseStubs();
		// All sub-milestones return false by default.

		overlay.render(realGraphics);

		assertTrue("expected '(none completed)' placeholder when no sub-milestones complete",
			panelLeftStrings().contains("(none completed)"));
	}

	// -------------------------------------------------------------------------
	// Error fallback — data source throwing must not kill the overlay
	// -------------------------------------------------------------------------

	@Test
	public void render_dataSourceThrows_emitsErrorRowAndContinues()
	{
		enableWithBaseStubs();
		when(pohTeleportInventory.getAvailableTeleports())
			.thenThrow(new RuntimeException("simulated failure"));

		// Must not throw out of render().
		Dimension result = overlay.render(realGraphics);
		assertNotNull(result);

		List<String> lefts = panelLeftStrings();
		assertTrue("expected an '(error)' placeholder for the failing section",
			lefts.stream().anyMatch(s -> s.contains("(error)")));
		// Subsequent sections still render.
		assertTrue("expected Diary section to still render after upstream section failure",
			lefts.stream().anyMatch(s -> s.startsWith("Diary tiers")));
	}

	// -------------------------------------------------------------------------
	// Test helpers
	// -------------------------------------------------------------------------

	private void enableWithBaseStubs()
	{
		when(config.enableCapabilityDebugOverlay()).thenReturn(true);

		Player player = mock(Player.class);
		when(player.getCombatLevel()).thenReturn(126);
		when(client.getLocalPlayer()).thenReturn(player);

		stubSkillLevels();
		when(client.getVarbitValue(anyInt())).thenReturn(0);
		when(client.getVarpValue(anyInt())).thenReturn(0);
		when(slayerTaskState.isTaskActive()).thenReturn(false);

		// Default quest state for Quest enum: each quest returns NOT_STARTED.
		// Actual countFinishedQuests() walks Quest.values() and calls
		// quest.getState(client) — that's a static enum call we cannot mock,
		// but it tolerates the client mock returning 0 and yields NOT_STARTED.
	}

	private void stubSkillLevels()
	{
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(99);
		when(client.getRealSkillLevel(Skill.CONSTRUCTION)).thenReturn(83);
		when(client.getRealSkillLevel(Skill.FARMING)).thenReturn(99);
		when(client.getRealSkillLevel(Skill.MAGIC)).thenReturn(99);
	}

	/**
	 * Inspect the {@link PanelComponent} the overlay built up via reflection so we
	 * can assert on individual {@link LineComponent} rows without needing a full
	 * graphics pipeline.
	 */
	private List<Row> panelRows()
	{
		try
		{
			PanelComponent panel = overlay.getPanelComponent();
			List<Row> rows = new ArrayList<>();
			for (LayoutableRenderableEntity child : panel.getChildren())
			{
				if (child instanceof LineComponent)
				{
					LineComponent lc = (LineComponent) child;
					rows.add(new Row(readField(lc, "left"), readField(lc, "right")));
				}
			}
			return rows;
		}
		catch (ReflectiveOperationException e)
		{
			throw new AssertionError("Unable to reflectively inspect overlay panel rows", e);
		}
	}

	private List<String> panelLeftStrings()
	{
		List<String> lefts = new ArrayList<>();
		for (Row r : panelRows())
		{
			if (r.left != null)
			{
				lefts.add(r.left);
			}
		}
		return lefts;
	}

	private static String readField(Object target, String fieldName) throws ReflectiveOperationException
	{
		Field f = target.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		Object v = f.get(target);
		return v == null ? null : v.toString();
	}

	private static String rightForLeft(List<Row> rows, String leftLabel)
	{
		for (Row r : rows)
		{
			if (leftLabel.equals(r.left))
			{
				return r.right == null ? "" : r.right;
			}
		}
		throw new AssertionError("no row found with left=" + leftLabel
			+ "; available lefts=" + rows);
	}

	private static int parseDenominator(String ratio)
	{
		int slash = ratio.indexOf('/');
		return Integer.parseInt(ratio.substring(slash + 1));
	}

	/**
	 * Mirror the overlay's private {@code humanise} helper so test expectations
	 * read in the same form as rendered rows.
	 */
	private static String humanise(String upperSnake)
	{
		String lower = upperSnake.toLowerCase().replace('_', ' ');
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}

	private static final class Row
	{
		final String left;
		final String right;

		Row(String left, String right)
		{
			this.left = left;
			this.right = right;
		}

		@Override
		public String toString()
		{
			return "Row{" + left + " -> " + right + "}";
		}
	}
}
