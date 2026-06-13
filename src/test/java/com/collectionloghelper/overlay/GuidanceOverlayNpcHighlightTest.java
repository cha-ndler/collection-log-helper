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

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the tracked-NPC highlight decision in {@link GuidanceOverlay}.
 *
 * <p>The render path returns early when the step's overworld tile cannot be
 * resolved to the local scene (which is ALWAYS the case inside instanced arenas,
 * where the scene is instance template space). Before the #807 fix that early
 * return ran <em>before</em> the NPC-highlight block, so NPC kill-step highlights
 * never drew for any instanced boss. The fix hoists the highlight decision so it
 * is independent of step-tile resolution; that decision is extracted into
 * {@link GuidanceOverlay#shouldDrawNpcHighlight(int, NPC, int)} so it can be
 * verified without the RuneLite render scaffolding (static
 * {@code LocalPoint.fromWorld} / {@code Perspective} calls).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GuidanceOverlayNpcHighlightTest
{
	private static final int BOSS_NPC_ID = 12345;

	@Mock
	private NPC npc;

	@Test
	public void drawsHighlight_whenTrackedNpcMatchesAndSamePlane()
	{
		// The NPC is plane-gated against its OWN location, not the step tile's.
		// Inside an instanced arena the step tile is unresolvable, but the NPC
		// lives in the real scene and must still be highlighted.
		when(npc.getId()).thenReturn(BOSS_NPC_ID);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(3000, 3000, 0));

		assertTrue(GuidanceOverlay.shouldDrawNpcHighlight(BOSS_NPC_ID, npc, 0),
			"Tracked NPC matching the step id on the player's plane must be highlighted");
	}

	@Test
	public void noHighlight_whenNoNpcId()
	{
		assertFalse(GuidanceOverlay.shouldDrawNpcHighlight(0, npc, 0),
			"A step with no NPC id must not highlight anything");
	}

	@Test
	public void noHighlight_whenTrackedNpcNull()
	{
		assertFalse(GuidanceOverlay.shouldDrawNpcHighlight(BOSS_NPC_ID, null, 0),
			"No tracked NPC means no highlight");
	}

	@Test
	public void noHighlight_whenTrackedNpcIdMismatch()
	{
		when(npc.getId()).thenReturn(BOSS_NPC_ID + 1);

		assertFalse(GuidanceOverlay.shouldDrawNpcHighlight(BOSS_NPC_ID, npc, 0),
			"A tracked NPC whose id differs from the step's must not be highlighted");
	}

	@Test
	public void noHighlight_whenNpcOnDifferentPlane()
	{
		when(npc.getId()).thenReturn(BOSS_NPC_ID);
		when(npc.getWorldLocation()).thenReturn(new WorldPoint(3000, 3000, 2));

		assertFalse(GuidanceOverlay.shouldDrawNpcHighlight(BOSS_NPC_ID, npc, 0),
			"An NPC on a different plane than the player must not be highlighted");
	}

	@Test
	public void noHighlight_whenNpcWorldLocationNull()
	{
		when(npc.getId()).thenReturn(BOSS_NPC_ID);
		when(npc.getWorldLocation()).thenReturn(null);

		assertFalse(GuidanceOverlay.shouldDrawNpcHighlight(BOSS_NPC_ID, npc, 0),
			"An NPC with no resolvable world location must not be highlighted");
	}
}
