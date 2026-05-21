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

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.PluginDataManager;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TempleSyncOrchestrator}.
 *
 * <p>Covers the observable paths previously inlined in the plugin:
 * <ul>
 *   <li>feature toggle disabled — short-circuit, no panel callback</li>
 *   <li>missing or empty player name — panel notified of failure, syncer never called</li>
 *   <li>successful result — KC store updated with only DB-known sources</li>
 *   <li>failure result — KC store untouched, panel notified of failure</li>
 * </ul>
 */
public class TempleSyncOrchestratorTest
{
	private CollectionLogHelperConfig config;
	private DataModule data;
	private DropRateDatabase database;
	private PluginDataManager pluginDataManager;
	private TempleOsrsKcSyncer syncer;
	private SourceKcStore kcStore;
	private Client client;
	private ClientThread clientThread;
	private CollectionLogHelperPanel panel;
	private ExecutorService resultExecutor;

	private TempleSyncOrchestrator orchestrator;

	@BeforeEach
	public void setUp()
	{
		config = mock(CollectionLogHelperConfig.class);
		data = mock(DataModule.class);
		database = mock(DropRateDatabase.class);
		pluginDataManager = mock(PluginDataManager.class);
		syncer = mock(TempleOsrsKcSyncer.class);
		kcStore = mock(SourceKcStore.class);
		client = mock(Client.class);
		clientThread = mock(ClientThread.class);
		panel = mock(CollectionLogHelperPanel.class);
		resultExecutor = mock(ExecutorService.class);

		when(data.getDatabase()).thenReturn(database);
		when(data.getPluginDataManager()).thenReturn(pluginDataManager);

		// Run any clientThread.invokeLater callbacks inline so we can assert
		// the chat-message side-effect synchronously.
		doAnswer(inv ->
		{
			Runnable r = inv.getArgument(0);
			r.run();
			return null;
		}).when(clientThread).invokeLater(any(Runnable.class));

		// Run executor submissions inline so side-effects are observable after
		// requestSync returns without waiting on a background thread.
		doAnswer(inv ->
		{
			Runnable r = inv.getArgument(0);
			r.run();
			return null;
		}).when(resultExecutor).submit(any(Runnable.class));

		orchestrator = new TempleSyncOrchestrator(
			config, data, syncer, kcStore, client, clientThread);
	}

	@Test
	public void requestSync_featureDisabled_doesNothing()
	{
		when(config.enableTempleOsrsSync()).thenReturn(false);

		orchestrator.requestSync(panel, resultExecutor);

		verify(syncer, never()).syncKc(anyString());
		verify(panel, never()).onTempleSyncComplete(anyBoolean());
		verify(kcStore, never()).update(any());
	}

	@Test
	public void requestSync_missingPlayerName_notifiesPanelFailure()
	{
		when(config.enableTempleOsrsSync()).thenReturn(true);
		when(pluginDataManager.getCurrentPlayerName()).thenReturn(null);

		orchestrator.requestSync(panel, resultExecutor);

		verify(syncer, never()).syncKc(anyString());
		verify(panel).onTempleSyncComplete(false);
	}

	@Test
	public void requestSync_emptyPlayerName_notifiesPanelFailure()
	{
		when(config.enableTempleOsrsSync()).thenReturn(true);
		when(pluginDataManager.getCurrentPlayerName()).thenReturn("");

		orchestrator.requestSync(panel, resultExecutor);

		verify(syncer, never()).syncKc(anyString());
		verify(panel).onTempleSyncComplete(false);
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void requestSync_successFiltersUnknownSources()
	{
		when(config.enableTempleOsrsSync()).thenReturn(true);
		when(pluginDataManager.getCurrentPlayerName()).thenReturn("Zezima");

		Map<String, Integer> kcMap = new HashMap<>();
		kcMap.put("Zulrah", 250);
		kcMap.put("Unknown Boss", 99);
		SyncResult success = SyncResult.success(kcMap, 0);
		when(syncer.syncKc("Zezima")).thenReturn(CompletableFuture.completedFuture(success));

		CollectionLogSource zulrahSource = stubSource("Zulrah");
		when(database.getSourceByName("Zulrah")).thenReturn(zulrahSource);
		when(database.getSourceByName("Unknown Boss")).thenReturn(null);

		orchestrator.requestSync(panel, resultExecutor);

		ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
		verify(kcStore).update(mapCaptor.capture());
		Map<String, Integer> validated = mapCaptor.getValue();
		assertNotNull(validated);
		assertEquals(1, validated.size());
		assertEquals(Integer.valueOf(250), validated.get("Zulrah"));

		verify(panel).onTempleSyncComplete(true);
		verify(client).addChatMessage(any(), eq(""), anyString(), eq(null));
	}

	@Test
	public void requestSync_failureResultLeavesStoreUntouched()
	{
		when(config.enableTempleOsrsSync()).thenReturn(true);
		when(pluginDataManager.getCurrentPlayerName()).thenReturn("Zezima");

		SyncResult failure = SyncResult.failure("HTTP 503");
		when(syncer.syncKc("Zezima")).thenReturn(CompletableFuture.completedFuture(failure));

		orchestrator.requestSync(panel, resultExecutor);

		verify(kcStore, never()).update(any());
		verify(panel).onTempleSyncComplete(false);
		verify(client).addChatMessage(any(), eq(""), anyString(), eq(null));
	}

	/**
	 * Build a real (non-mocked) {@link CollectionLogSource} — the class is final
	 * (Lombok {@code @Value}) so Mockito can't mock it directly. We only need the
	 * orchestrator to see a non-null lookup result.
	 */
	private static CollectionLogSource stubSource(String name)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 0, 0, 0,
			60, 0, null, Collections.emptyList(),
			null, 0.0, null, 0, false, 0, null, 0, null, null, null, null, null, 0, null, 0,
			Collections.emptyList(), null, null, null);
	}
}
