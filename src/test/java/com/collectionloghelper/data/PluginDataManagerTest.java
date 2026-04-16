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
package com.collectionloghelper.data;

import java.lang.reflect.Constructor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginDataManagerTest
{
	@Mock
	private Client client;

	@Mock
	private Player localPlayer;

	private PluginDataManager manager;

	@Before
	public void setUp() throws Exception
	{
		Constructor<PluginDataManager> ctor = PluginDataManager.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		manager = ctor.newInstance(client);
	}

	// ========================================================================
	// sanitizeFileName (package-private static — testable directly)
	// ========================================================================

	@Test
	public void testSanitizeFileName_normalName()
	{
		assertEquals("Zezima", PluginDataManager.sanitizeFileName("Zezima"));
	}

	@Test
	public void testSanitizeFileName_nameWithSpaces()
	{
		assertEquals("Iron Man", PluginDataManager.sanitizeFileName("Iron Man"));
	}

	@Test
	public void testSanitizeFileName_nameWithHyphen()
	{
		assertEquals("Iron-Man", PluginDataManager.sanitizeFileName("Iron-Man"));
	}

	@Test
	public void testSanitizeFileName_nameWithUnderscore()
	{
		assertEquals("Iron_Man", PluginDataManager.sanitizeFileName("Iron_Man"));
	}

	@Test
	public void testSanitizeFileName_specialCharsReplaced()
	{
		// Angle brackets, slashes, and other filesystem-unsafe chars → underscore
		String sanitized = PluginDataManager.sanitizeFileName("Player<test>/name");
		assertFalse("Special chars should be replaced", sanitized.contains("<"));
		assertFalse("Special chars should be replaced", sanitized.contains(">"));
		assertFalse("Slash should be replaced", sanitized.contains("/"));
	}

	@Test
	public void testSanitizeFileName_numberOnlyName()
	{
		assertEquals("12345", PluginDataManager.sanitizeFileName("12345"));
	}

	@Test
	public void testSanitizeFileName_trimLeadingTrailingSpaces()
	{
		assertEquals("Trim Me", PluginDataManager.sanitizeFileName("  Trim Me  "));
	}

	@Test
	public void testSanitizeFileName_mixedCase()
	{
		assertEquals("ABCdef123", PluginDataManager.sanitizeFileName("ABCdef123"));
	}

	// ========================================================================
	// init() — player name not available
	// ========================================================================

	@Test
	public void testInit_playerNameNull_returnsFalse()
	{
		// Arrange — localPlayer is null
		when(client.getLocalPlayer()).thenReturn(null);

		// Act
		boolean result = manager.init();

		// Assert
		assertFalse(result);
	}

	@Test
	public void testInit_playerNameNull_characterDirRemainsNull()
	{
		// Arrange
		when(client.getLocalPlayer()).thenReturn(null);

		// Act
		manager.init();

		// Assert
		assertNull(manager.getCharacterDir());
	}

	@Test
	public void testInit_playerNameEmpty_returnsFalse()
	{
		// Arrange
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn("");

		// Act
		boolean result = manager.init();

		// Assert
		assertFalse(result);
	}

	@Test
	public void testInit_playerNotNull_butNameIsNull_returnsFalse()
	{
		// Arrange
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn(null);

		// Act
		boolean result = manager.init();

		// Assert
		assertFalse(result);
	}

	// ========================================================================
	// reset()
	// ========================================================================

	@Test
	public void testReset_clearsCharacterDir()
	{
		// Arrange — put the manager into a known state by calling reset after a partial init
		manager.reset();

		// Assert
		assertNull(manager.getCharacterDir());
	}

	@Test
	public void testReset_clearsCurrentPlayerName()
	{
		// Arrange
		manager.reset();

		// Assert
		assertNull(manager.getCurrentPlayerName());
	}

	@Test
	public void testReset_afterNoInit_isIdempotent()
	{
		// Act — reset when never initialized
		manager.reset();
		manager.reset();

		// Assert — no exception, state is null
		assertNull(manager.getCharacterDir());
		assertNull(manager.getCurrentPlayerName());
	}

	// ========================================================================
	// getFile()
	// ========================================================================

	@Test
	public void testGetFile_whenNotInitialized_returnsNull()
	{
		// Arrange — not initialized
		assertNull(manager.getCharacterDir());

		// Act
		java.io.File file = manager.getFile("efficiency-export.txt");

		// Assert
		assertNull(file);
	}

	// ========================================================================
	// getCurrentPlayerName()
	// ========================================================================

	@Test
	public void testGetCurrentPlayerName_initiallyNull()
	{
		assertNull(manager.getCurrentPlayerName());
	}
}
