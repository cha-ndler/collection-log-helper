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
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Orchestrates an end-to-end TempleOSRS KC sync request triggered from the
 * panel "Sync KC from TempleOSRS" button.
 *
 * <p>Owns the logic that previously lived in the plugin's
 * {@code onTempleSyncRequested} method:
 * <ul>
 *   <li>Validates the feature toggle and player-name preconditions.</li>
 *   <li>Submits the sync to {@link TempleOsrsKcSyncer} and awaits the result on
 *       the supplied {@link ExecutorService} so the EDT/client thread is never
 *       blocked.</li>
 *   <li>Filters mapped names against the source database before writing them
 *       into {@link SourceKcStore} (unknown sources are logged and skipped).</li>
 *   <li>Posts the user-facing chat message on the client thread and notifies
 *       the panel that the button can be restored.</li>
 * </ul>
 *
 * <p>The {@link CollectionLogHelperPanel} and {@link ExecutorService} are
 * passed per-call rather than injected, because the panel is plugin-lifecycle
 * owned (created in {@code startUp}) and the executor is a shared daemon
 * lazily created by the plugin. This matches the call-site delegation pattern
 * established by {@link LootSyncManager}.
 *
 * <p>Part of issue #503 — splitting the {@code CollectionLogHelperPlugin}
 * god-class into focused collaborators.
 */
@Slf4j
@Singleton
public class TempleSyncOrchestrator
{
	private static final long SYNC_TIMEOUT_SECONDS = 30;

	private final CollectionLogHelperConfig config;
	private final DataModule data;
	private final TempleOsrsKcSyncer templeOsrsKcSyncer;
	private final SourceKcStore sourceKcStore;
	private final Client client;
	private final ClientThread clientThread;

	@Inject
	public TempleSyncOrchestrator(
		CollectionLogHelperConfig config,
		DataModule data,
		TempleOsrsKcSyncer templeOsrsKcSyncer,
		SourceKcStore sourceKcStore,
		Client client,
		ClientThread clientThread)
	{
		this.config = config;
		this.data = data;
		this.templeOsrsKcSyncer = templeOsrsKcSyncer;
		this.sourceKcStore = sourceKcStore;
		this.client = client;
		this.clientThread = clientThread;
	}

	/**
	 * Initiates an asynchronous TempleOSRS KC sync for the current player.
	 *
	 * <p>The sync runs on the {@link TempleOsrsKcSyncer} background thread;
	 * on completion the result is applied via {@link #clientThread} and the
	 * panel button is reset.
	 *
	 * <p>Fail-soft: any error is logged and surfaced as a chat message; no
	 * exception reaches the caller.
	 *
	 * @param panel            the panel that owns the sync button — used to
	 *                         reset the button label on completion. May be
	 *                         {@code null} during shutdown races, in which
	 *                         case the panel callback is skipped.
	 * @param resultExecutor   daemon executor used to wait on the sync future
	 *                         off the EDT/client thread.
	 */
	public void requestSync(CollectionLogHelperPanel panel, ExecutorService resultExecutor)
	{
		if (!config.enableTempleOsrsSync())
		{
			return;
		}

		String playerName = data.getPluginDataManager().getCurrentPlayerName();
		if (playerName == null || playerName.isEmpty())
		{
			log.warn("TempleOSRS sync requested but player name is not available");
			if (panel != null)
			{
				panel.onTempleSyncComplete(false);
			}
			return;
		}

		log.debug("Requesting TempleOSRS KC sync for '{}'", playerName);

		Future<SyncResult> future = templeOsrsKcSyncer.syncKc(playerName);

		// Wait for the result on the shared daemon executor so the EDT is not blocked
		resultExecutor.submit(() -> awaitResult(future, playerName, panel));
	}

	private void awaitResult(Future<SyncResult> future, String playerName, CollectionLogHelperPanel panel)
	{
		SyncResult result;
		try
		{
			result = future.get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			log.warn("TempleOSRS sync future failed for '{}': {}", playerName, e.getMessage());
			result = SyncResult.failure("Sync timed out or failed: " + e.getMessage());
		}

		if (result.isSuccess())
		{
			applySuccessfulResult(result);
		}
		else
		{
			log.warn("TempleOSRS KC sync failed: {}", result.getErrorMessage());
			postChatMessage(
				"<col=ff0000>[Collection Log Helper]</col> TempleOSRS KC sync failed: "
					+ result.getErrorMessage());
		}

		if (panel != null)
		{
			panel.onTempleSyncComplete(result.isSuccess());
		}
	}

	private void applySuccessfulResult(SyncResult result)
	{
		// Validate mapped names against the DB and apply only known sources
		Map<String, Integer> validated = new HashMap<>();
		for (Map.Entry<String, Integer> entry : result.getKcBySource().entrySet())
		{
			if (data.getDatabase().getSourceByName(entry.getKey()) != null)
			{
				validated.put(entry.getKey(), entry.getValue());
			}
			else
			{
				log.debug("TempleOSRS KC entry '{}' not found in CLH database - skipping",
					entry.getKey());
			}
		}
		sourceKcStore.update(validated);
		log.info("TempleOSRS KC sync complete: {} sources updated, {} skipped",
			validated.size(), result.getSkippedCount());

		postChatMessage(
			"<col=00c8c8>[Collection Log Helper]</col> TempleOSRS KC synced: "
				+ validated.size() + " sources updated.");
	}

	private void postChatMessage(String message)
	{
		clientThread.invokeLater(() ->
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null));
	}
}
