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
import net.runelite.api.events.VarbitChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that {@link PlayerQuestProgressState} refreshes its snapshot on
 * {@code VarbitChanged} (#611).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PlayerQuestProgressStateSubscribeTest
{
	@Mock
	private Client client;

	private PlayerQuestProgressState detector;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<PlayerQuestProgressState> ctor =
			PlayerQuestProgressState.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		detector = ctor.newInstance(client);
	}

	@Test
	public void onVarbitChanged_refreshesSnapshot()
	{
		// Lost City varplayer 147 at value 5 = Dramen staff crafted.
		when(client.getVarpValue(147)).thenReturn(5);
		when(client.getVarpValue(165)).thenReturn(0);
		when(client.getVarpValue(68)).thenReturn(0);

		detector.onVarbitChanged(new VarbitChanged());

		verify(client, atLeastOnce()).getVarpValue(147);
		assertTrue(detector.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_BRANCH_CUT));
		assertTrue(detector.hasSubProgress(QuestSubMilestone.LOST_CITY_DRAMEN_STAFF_CRAFTED));
		assertFalse(detector.hasSubProgress(QuestSubMilestone.LOST_CITY_COMPLETE));
	}
}
