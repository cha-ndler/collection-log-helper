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
package com.collectionloghelper.ui;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SyncButtonController}: the helper that owns the
 * "Sync from collectionlog.net" and "Sync KC from TempleOSRS" buttons.
 * Extracted from {@link CollectionLogHelperPanel} as part of issue #503
 * god-class splits.
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncButtonControllerTest
{
	@Mock
	private CollectionLogHelperConfig config;

	private JPanel hostPanel;
	private SyncButtonController controller;

	@Before
	public void setUp()
	{
		when(config.enableCollectionLogNetImport()).thenReturn(true);
		when(config.enableTempleOsrsSync()).thenReturn(true);

		hostPanel = new JPanel();
		controller = new SyncButtonController(config, hostPanel);
	}

	private static void flushEdt() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> { /* no-op flush */ });
	}

	@Test
	public void buttonsAreNonNullAndDistinct()
	{
		JButton clNet = controller.getCollectionLogNetButton();
		JButton temple = controller.getTempleSyncButton();

		assertNotNull(clNet);
		assertNotNull(temple);
		assertNotSame(clNet, temple);
	}

	@Test
	public void buttonLabelsMatchCanonicalText()
	{
		assertEquals("Sync from collectionlog.net",
			controller.getCollectionLogNetButton().getText());
		assertEquals("Sync KC from TempleOSRS",
			controller.getTempleSyncButton().getText());
	}

	@Test
	public void visibilityReflectsConfigAtConstruction()
	{
		when(config.enableCollectionLogNetImport()).thenReturn(false);
		when(config.enableTempleOsrsSync()).thenReturn(true);

		SyncButtonController c = new SyncButtonController(config, hostPanel);

		assertFalse(c.getCollectionLogNetButton().isVisible());
		assertTrue(c.getTempleSyncButton().isVisible());
	}

	@Test
	public void collectionLogNetButtonClickInvokesCallback()
	{
		AtomicInteger invocations = new AtomicInteger();
		controller.setCollectionLogNetImportCallback(invocations::incrementAndGet);

		JButton btn = controller.getCollectionLogNetButton();
		btn.getActionListeners()[0].actionPerformed(
			new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, ""));

		assertEquals(1, invocations.get());
		assertFalse("button must be disabled while sync runs", btn.isEnabled());
		assertEquals("Syncing...", btn.getText());
	}

	@Test
	public void collectionLogNetButtonClickIsNoOpWhenNoCallbackWired()
	{
		JButton btn = controller.getCollectionLogNetButton();
		// Pre-condition: no callback wired.
		btn.getActionListeners()[0].actionPerformed(
			new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, ""));

		// State must not change — the button stays enabled with its original label.
		assertTrue(btn.isEnabled());
		assertEquals("Sync from collectionlog.net", btn.getText());
	}

	@Test
	public void templeButtonClickInvokesCallback()
	{
		AtomicBoolean called = new AtomicBoolean();
		controller.setTempleSyncCallback(() -> called.set(true));

		JButton btn = controller.getTempleSyncButton();
		btn.getActionListeners()[0].actionPerformed(
			new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, ""));

		assertTrue(called.get());
		assertFalse(btn.isEnabled());
		assertEquals("Syncing...", btn.getText());
	}

	@Test
	public void onCollectionLogNetImportCompleteRestoresEnabledAndShowsMessage() throws Exception
	{
		JButton btn = controller.getCollectionLogNetButton();
		btn.setEnabled(false);
		btn.setText("Syncing...");

		controller.onCollectionLogNetImportComplete("Synced 42 items");
		flushEdt();

		assertTrue(btn.isEnabled());
		assertEquals("Synced 42 items", btn.getText());
	}

	@Test
	public void updateCollectionLogNetImportButtonHidesWhenConfigDisabled() throws Exception
	{
		JButton btn = controller.getCollectionLogNetButton();
		assertTrue(btn.isVisible());

		when(config.enableCollectionLogNetImport()).thenReturn(false);
		controller.updateCollectionLogNetImportButton();
		flushEdt();

		assertFalse(btn.isVisible());
	}

	@Test
	public void updateCollectionLogNetImportButtonResetsLabelWhenReEnabled() throws Exception
	{
		JButton btn = controller.getCollectionLogNetButton();
		btn.setEnabled(false);
		btn.setText("Synced 7 items");
		btn.setVisible(false);

		when(config.enableCollectionLogNetImport()).thenReturn(true);
		controller.updateCollectionLogNetImportButton();
		flushEdt();

		assertTrue(btn.isVisible());
		assertTrue(btn.isEnabled());
		assertEquals("Sync from collectionlog.net", btn.getText());
	}

	@Test
	public void updateTempleSyncButtonVisibilityFollowsConfigToggle() throws Exception
	{
		JButton btn = controller.getTempleSyncButton();
		assertTrue(btn.isVisible());

		when(config.enableTempleOsrsSync()).thenReturn(false);
		controller.updateTempleSyncButtonVisibility();
		flushEdt();

		assertFalse(btn.isVisible());

		when(config.enableTempleOsrsSync()).thenReturn(true);
		controller.updateTempleSyncButtonVisibility();
		flushEdt();

		assertTrue(btn.isVisible());
	}

	@Test
	public void onTempleSyncCompleteSuccessShowsSyncedLabel() throws Exception
	{
		JButton btn = controller.getTempleSyncButton();
		btn.setEnabled(false);
		btn.setText("Syncing...");

		controller.onTempleSyncComplete(true);
		flushEdt();

		assertTrue(btn.isEnabled());
		assertEquals("Synced KC from TempleOSRS", btn.getText());
	}

	@Test
	public void onTempleSyncCompleteFailureRestoresOriginalLabel() throws Exception
	{
		JButton btn = controller.getTempleSyncButton();
		btn.setEnabled(false);
		btn.setText("Syncing...");

		controller.onTempleSyncComplete(false);
		flushEdt();

		assertTrue(btn.isEnabled());
		assertEquals("Sync KC from TempleOSRS", btn.getText());
	}
}
