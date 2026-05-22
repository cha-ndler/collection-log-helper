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

import com.collectionloghelper.CollectionLogHelperConfig;
import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
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
 * Verifies that {@link PohTeleportInventoryImpl}'s {@code @Subscribe} handlers
 * trigger a snapshot refresh on the relevant game events (#611).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PohTeleportInventoryImplSubscribeTest
{
	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	private PohTeleportInventoryImpl detector;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<PohTeleportInventoryImpl> ctor =
			PohTeleportInventoryImpl.class.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		detector = ctor.newInstance(client, config);
	}

	@Test
	public void onVarbitChanged_refreshesSnapshot()
	{
		// Arrange — jewellery box at tier 2 (Fancy) so refresh sets at least one varbit flag.
		when(client.getVarbitValue(2308)).thenReturn(2);
		when(client.getVarbitValue(6670)).thenReturn(0);
		when(client.getVarbitValue(6651)).thenReturn(0);
		when(client.getVarbitValue(6617)).thenReturn(0);

		// Act
		detector.onVarbitChanged(new VarbitChanged());

		// Assert — refresh ran (varbit reads happened) and state reflects the tier
		verify(client, atLeastOnce()).getVarbitValue(2308);
		assertTrue(detector.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(detector.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	@Test
	public void onGameStateChanged_loggedIn_refreshesSnapshot()
	{
		when(client.getVarbitValue(2308)).thenReturn(3); // ornate
		when(client.getVarbitValue(6670)).thenReturn(0);
		when(client.getVarbitValue(6651)).thenReturn(0);
		when(client.getVarbitValue(6617)).thenReturn(0);

		GameStateChanged event = new GameStateChanged();
		event.setGameState(GameState.LOGGED_IN);
		detector.onGameStateChanged(event);

		verify(client, atLeastOnce()).getVarbitValue(2308);
		assertTrue(detector.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	@Test
	public void onGameStateChanged_loginScreen_doesNotRefresh()
	{
		GameStateChanged event = new GameStateChanged();
		event.setGameState(GameState.LOGIN_SCREEN);
		detector.onGameStateChanged(event);

		// No varbit reads should have happened — refresh skipped for non-LOGGED_IN
		verify(client, never()).getVarbitValue(anyInt());
	}
}
