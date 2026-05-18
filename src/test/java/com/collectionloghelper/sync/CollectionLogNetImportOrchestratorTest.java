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
package com.collectionloghelper.sync;

import com.collectionloghelper.di.SyncModule;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import net.runelite.api.Client;
import net.runelite.api.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CollectionLogNetImportOrchestrator}.
 *
 * <p>Covers the observable paths previously inlined in the plugin:
 * <ul>
 *   <li>no local player — panel notified, importer never called</li>
 *   <li>empty player name — panel notified, importer never called</li>
 *   <li>successful import — panel toasted with success message and rebuilt</li>
 *   <li>non-success result — panel toasted but no rebuild triggered</li>
 *   <li>future fails — generic error message surfaced, no rebuild</li>
 * </ul>
 */
public class CollectionLogNetImportOrchestratorTest
{
	private Client client;
	private Player localPlayer;
	private SyncModule sync;
	private CollectionLogNetImporter importer;
	private CollectionLogHelperPanel panel;
	private ExecutorService resultExecutor;

	private CollectionLogNetImportOrchestrator orchestrator;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		localPlayer = mock(Player.class);
		sync = mock(SyncModule.class);
		importer = mock(CollectionLogNetImporter.class);
		panel = mock(CollectionLogHelperPanel.class);
		resultExecutor = mock(ExecutorService.class);

		when(sync.getCollectionLogNetImporter()).thenReturn(importer);

		// Run executor submissions inline so side-effects are observable after
		// requestImport returns without waiting on a background thread.
		doAnswer(inv ->
		{
			Runnable r = inv.getArgument(0);
			r.run();
			return null;
		}).when(resultExecutor).submit(any(Runnable.class));

		orchestrator = new CollectionLogNetImportOrchestrator(client, sync);
	}

	@Test
	public void requestImport_noLocalPlayer_notifiesPanelAndSkips()
	{
		when(client.getLocalPlayer()).thenReturn(null);

		orchestrator.requestImport(panel, resultExecutor);

		verify(panel).onCollectionLogNetImportComplete("Log in first");
		verify(importer, never()).importProfile(anyString());
		verify(panel, never()).rebuild();
	}

	@Test
	public void requestImport_emptyPlayerName_notifiesPanelAndSkips()
	{
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn("");

		orchestrator.requestImport(panel, resultExecutor);

		verify(panel).onCollectionLogNetImportComplete("Log in first");
		verify(importer, never()).importProfile(anyString());
		verify(panel, never()).rebuild();
	}

	@Test
	public void requestImport_successfulResultTriggersRebuild()
	{
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn("Zezima");

		ImportResult success = ImportResult.success(42);
		when(importer.importProfile("Zezima"))
			.thenReturn(CompletableFuture.completedFuture(success));

		orchestrator.requestImport(panel, resultExecutor);

		ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);
		verify(panel).onCollectionLogNetImportComplete(msg.capture());
		assertTrue("toast message should be non-empty", !msg.getValue().isEmpty());
		verify(panel).rebuild();
	}

	@Test
	public void requestImport_nonSuccessResultDoesNotRebuild()
	{
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn("Zezima");

		ImportResult notFound = ImportResult.userNotFound("Zezima");
		when(importer.importProfile("Zezima"))
			.thenReturn(CompletableFuture.completedFuture(notFound));

		orchestrator.requestImport(panel, resultExecutor);

		verify(panel).onCollectionLogNetImportComplete(notFound.toToastMessage());
		verify(panel, never()).rebuild();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void requestImport_futureFails_surfacesGenericError()
	{
		when(client.getLocalPlayer()).thenReturn(localPlayer);
		when(localPlayer.getName()).thenReturn("Zezima");

		Future<ImportResult> failing = mock(Future.class);
		try
		{
			when(failing.get()).thenThrow(new ExecutionException(new RuntimeException("boom")));
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
		when(importer.importProfile("Zezima")).thenReturn(failing);

		orchestrator.requestImport(panel, resultExecutor);

		verify(panel).onCollectionLogNetImportComplete("collectionlog.net: error");
		verify(panel, never()).rebuild();
	}
}
