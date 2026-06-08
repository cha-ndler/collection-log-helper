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
package com.collectionloghelper.player;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for #738: {@code refresh()} previously re-stored and
 * {@code log.debug}'d the full quest-milestone map on every tick a quest varbit
 * changed (~2x/second) with byte-identical payload. The extracted
 * {@link PlayerQuestProgressState#applyRefresh(Map)} now stores + logs only when
 * the resolved map actually changed.
 *
 * <p>Tested via the {@code applyRefresh} seam so the client-script reads in the
 * {@code populate*} helpers (which run {@code QUEST_STATUS_GET}) need not be
 * mocked. A skipped store is observed as snapshot-reference identity.
 */
public class PlayerQuestProgressStateRefreshTest
{
	private PlayerQuestProgressState state;

	@BeforeEach
	public void setUp()
	{
		// applyRefresh does not touch client / clientThread, so null deps are fine.
		state = new PlayerQuestProgressState(null, null);
	}

	@Test
	@DisplayName("applyRefresh stores a changed map and skips an unchanged one (#738)")
	public void applyRefreshGuardsOnContentEquality() throws Exception
	{
		Map<QuestSubMilestone, Boolean> first = map(true);
		state.applyRefresh(first);
		assertSame(first, snapshot(), "first non-empty refresh must be stored");

		// Equal content, different instance — the per-tick varbit storm case.
		Map<QuestSubMilestone, Boolean> sameContent = map(true);
		state.applyRefresh(sameContent);
		assertSame(first, snapshot(),
			"an unchanged map must NOT replace the snapshot (no redundant store/log)");

		// Genuine milestone change — must be stored.
		Map<QuestSubMilestone, Boolean> changed = map(false);
		state.applyRefresh(changed);
		assertSame(changed, snapshot(), "a changed map must replace the snapshot");
		assertNotSame(first, snapshot());
	}

	@Test
	@DisplayName("applyRefresh stores the snapshot so hasSubProgress reflects it")
	public void applyRefreshUpdatesObservableState()
	{
		state.applyRefresh(map(true));
		assertTrue(state.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}

	private static Map<QuestSubMilestone, Boolean> map(boolean lostCityComplete)
	{
		Map<QuestSubMilestone, Boolean> m = new EnumMap<>(QuestSubMilestone.class);
		m.put(QuestSubMilestone.LOST_CITY_COMPLETE, lostCityComplete);
		m.put(QuestSubMilestone.PLAGUE_CITY_COMPLETE, true);
		return m;
	}

	@SuppressWarnings("unchecked")
	private Map<QuestSubMilestone, Boolean> snapshot() throws Exception
	{
		Field f = PlayerQuestProgressState.class.getDeclaredField("snapshot");
		f.setAccessible(true);
		return (Map<QuestSubMilestone, Boolean>) f.get(state);
	}
}
