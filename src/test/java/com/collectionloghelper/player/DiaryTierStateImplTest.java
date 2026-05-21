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

import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DiaryTierStateImplTest
{
	@Mock
	private Client client;

	private DiaryTierStateImpl diaryState;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<DiaryTierStateImpl> ctor =
			DiaryTierStateImpl.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		diaryState = ctor.newInstance(client);
	}

	// =========================================================================
	// Initial state — all false before any refresh
	// =========================================================================

	@Test
	public void initialState_allFalse()
	{
		for (DiaryRegion region : DiaryRegion.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				assertFalse(
					diaryState.hasDiary(region, tier),"Expected false before refresh for " + region + " " + tier);
			}
		}
	}

	// =========================================================================
	// hasDiary — null guards
	// =========================================================================

	@Test
	public void hasDiary_nullRegion_returnsFalse()
	{
		assertFalse(diaryState.hasDiary(null, DiaryTier.HARD));
	}

	@Test
	public void hasDiary_nullTier_returnsFalse()
	{
		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, null));
	}

	@Test
	public void hasDiary_bothNull_returnsFalse()
	{
		assertFalse(diaryState.hasDiary(null, null));
	}

	// =========================================================================
	// refresh — single tier complete
	// =========================================================================

	@Test
	public void refresh_karamjaHardComplete_returnsTrue()
	{
		when(client.getVarbitValue(Varbits.DIARY_KARAMJA_HARD)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD));
	}

	@Test
	public void refresh_karamjaHardNotComplete_returnsFalse()
	{
		when(client.getVarbitValue(Varbits.DIARY_KARAMJA_HARD)).thenReturn(0);

		diaryState.refresh();

		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD));
	}

	@Test
	public void refresh_ardougneEliteComplete_returnsTrue()
	{
		when(client.getVarbitValue(Varbits.DIARY_ARDOUGNE_ELITE)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.ARDOUGNE, DiaryTier.ELITE));
	}

	@Test
	public void refresh_lumbridgeMediumComplete_returnsTrue()
	{
		when(client.getVarbitValue(Varbits.DIARY_LUMBRIDGE_MEDIUM)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.LUMBRIDGE, DiaryTier.MEDIUM));
	}

	@Test
	public void refresh_morytaniaEasyComplete_returnsTrue()
	{
		when(client.getVarbitValue(Varbits.DIARY_MORYTANIA_EASY)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.MORYTANIA, DiaryTier.EASY));
	}

	// =========================================================================
	// refresh — multiple tiers/regions independently tracked
	// =========================================================================

	@Test
	public void refresh_onlySelectedTiersComplete_othersRemainFalse()
	{
		when(client.getVarbitValue(Varbits.DIARY_VARROCK_HARD)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_WILDERNESS_ELITE)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.VARROCK, DiaryTier.HARD));
		assertTrue(diaryState.hasDiary(DiaryRegion.WILDERNESS, DiaryTier.ELITE));

		// Tiers not set should still be false
		assertFalse(diaryState.hasDiary(DiaryRegion.VARROCK, DiaryTier.ELITE));
		assertFalse(diaryState.hasDiary(DiaryRegion.WILDERNESS, DiaryTier.EASY));
		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD));
	}

	@Test
	public void refresh_allForOneRegion_allTiersDetected()
	{
		when(client.getVarbitValue(Varbits.DIARY_KANDARIN_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KANDARIN_MEDIUM)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KANDARIN_HARD)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KANDARIN_ELITE)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.KANDARIN, DiaryTier.EASY));
		assertTrue(diaryState.hasDiary(DiaryRegion.KANDARIN, DiaryTier.MEDIUM));
		assertTrue(diaryState.hasDiary(DiaryRegion.KANDARIN, DiaryTier.HARD));
		assertTrue(diaryState.hasDiary(DiaryRegion.KANDARIN, DiaryTier.ELITE));
	}

	// =========================================================================
	// refresh — all 12 regions have distinct varbit IDs (coverage check)
	// =========================================================================

	@Test
	public void refresh_eachRegionEasyTierRead_usesDistinctVarbits()
	{
		// Stub each region's Easy varbit to return 1
		when(client.getVarbitValue(Varbits.DIARY_ARDOUGNE_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_DESERT_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_FALADOR_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_FREMENNIK_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KANDARIN_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KARAMJA_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KOUREND_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_LUMBRIDGE_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_MORYTANIA_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_VARROCK_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_WESTERN_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_WILDERNESS_EASY)).thenReturn(1);

		diaryState.refresh();

		for (DiaryRegion region : DiaryRegion.values())
		{
			assertTrue(
				diaryState.hasDiary(region, DiaryTier.EASY),"Expected EASY true for " + region);
		}
	}

	// =========================================================================
	// refresh — client exception is handled gracefully
	// =========================================================================

	@Test
	public void refresh_clientThrows_treatedAsNotComplete()
	{
		when(client.getVarbitValue(anyInt()))
			.thenThrow(new RuntimeException("client not ready"));

		// Must not throw; all entries should remain false
		diaryState.refresh();

		for (DiaryRegion region : DiaryRegion.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				assertFalse(
					diaryState.hasDiary(region, tier),"Expected false when client throws for " + region + " " + tier);
			}
		}
	}

	// =========================================================================
	// refresh — state updates on second call
	// =========================================================================

	@Test
	public void refresh_secondCallUpdatesState()
	{
		// First refresh: Fremennik Hard not complete
		when(client.getVarbitValue(Varbits.DIARY_FREMENNIK_HARD)).thenReturn(0);
		diaryState.refresh();
		assertFalse(diaryState.hasDiary(DiaryRegion.FREMENNIK, DiaryTier.HARD));

		// Second refresh: Fremennik Hard now complete
		when(client.getVarbitValue(Varbits.DIARY_FREMENNIK_HARD)).thenReturn(1);
		diaryState.refresh();
		assertTrue(diaryState.hasDiary(DiaryRegion.FREMENNIK, DiaryTier.HARD));
	}

	// =========================================================================
	// reset — clears all cached state
	// =========================================================================

	@Test
	public void reset_afterRefreshWithData_allFalse()
	{
		when(client.getVarbitValue(Varbits.DIARY_DESERT_ELITE)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KARAMJA_HARD)).thenReturn(1);
		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.DESERT, DiaryTier.ELITE));
		assertTrue(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD));

		diaryState.reset();

		for (DiaryRegion region : DiaryRegion.values())
		{
			for (DiaryTier tier : DiaryTier.values())
			{
				assertFalse(
					diaryState.hasDiary(region, tier),"Expected false after reset for " + region + " " + tier);
			}
		}
	}

	// =========================================================================
	// refresh — Kourend (highest varbit IDs ~7925-7928) and Karamja (non-sequential
	// IDs 3578/3599/3611/4566) work correctly
	// =========================================================================

	@Test
	public void refresh_kourendAllTiers_allDetected()
	{
		when(client.getVarbitValue(Varbits.DIARY_KOUREND_EASY)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KOUREND_MEDIUM)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KOUREND_HARD)).thenReturn(1);
		when(client.getVarbitValue(Varbits.DIARY_KOUREND_ELITE)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.KOUREND, DiaryTier.EASY));
		assertTrue(diaryState.hasDiary(DiaryRegion.KOUREND, DiaryTier.MEDIUM));
		assertTrue(diaryState.hasDiary(DiaryRegion.KOUREND, DiaryTier.HARD));
		assertTrue(diaryState.hasDiary(DiaryRegion.KOUREND, DiaryTier.ELITE));
	}

	@Test
	public void refresh_karamjaElite_usesCorrectVarbit()
	{
		// Karamja Elite is varbit 4566, non-sequential with Easy/Medium/Hard (3578/3599/3611)
		when(client.getVarbitValue(Varbits.DIARY_KARAMJA_ELITE)).thenReturn(1);

		diaryState.refresh();

		assertTrue(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.ELITE));
		// Lower tiers not set should remain false
		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.EASY));
		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.MEDIUM));
		assertFalse(diaryState.hasDiary(DiaryRegion.KARAMJA, DiaryTier.HARD));
	}
}
