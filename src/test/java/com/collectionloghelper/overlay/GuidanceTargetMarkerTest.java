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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Verifies the guidance target marker icon is loaded and resized once at
 * overlay construction (the render hot-path must not perform IO). The actual
 * in-world drawing cannot be exercised headless, so these tests confirm the
 * icon resource resolves and is sized as expected.
 */
public class GuidanceTargetMarkerTest
{
	private static final int EXPECTED_MARKER_SIZE = 18;

	@Test
	public void objectOverlayLoadsResizedMarkerIcon() throws Exception
	{
		Client client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);

		Constructor<ObjectHighlightOverlay> ctor = ObjectHighlightOverlay.class
			.getDeclaredConstructor(Client.class, ClientThread.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		ObjectHighlightOverlay overlay = ctor.newInstance(client, clientThread, config);

		BufferedImage icon = readMarkerIcon(overlay);
		assertNotNull(icon, "marker icon should load from the classpath resource");
		assertEquals(EXPECTED_MARKER_SIZE, icon.getWidth());
		assertEquals(EXPECTED_MARKER_SIZE, icon.getHeight());
	}

	@Test
	public void groundItemOverlayLoadsResizedMarkerIcon() throws Exception
	{
		Client client = mock(Client.class);
		CollectionLogHelperConfig config = mock(CollectionLogHelperConfig.class);

		Constructor<GroundItemHighlightOverlay> ctor = GroundItemHighlightOverlay.class
			.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		GroundItemHighlightOverlay overlay = ctor.newInstance(client, config);

		BufferedImage icon = readMarkerIcon(overlay);
		assertNotNull(icon, "marker icon should load from the classpath resource");
		assertEquals(EXPECTED_MARKER_SIZE, icon.getWidth());
		assertEquals(EXPECTED_MARKER_SIZE, icon.getHeight());
	}

	private static BufferedImage readMarkerIcon(Object overlay) throws Exception
	{
		Field field = overlay.getClass().getDeclaredField("markerIcon");
		field.setAccessible(true);
		return (BufferedImage) field.get(overlay);
	}
}
