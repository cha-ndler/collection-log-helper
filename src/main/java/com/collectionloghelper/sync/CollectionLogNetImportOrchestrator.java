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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

/**
 * Orchestrates an end-to-end collectionlog.net profile import triggered from
 * the panel "Import from collectionlog.net" button.
 *
 * <p>Owns the logic that previously lived inline in the plugin's
 * {@code startUp} method (callback wired via
 * {@link CollectionLogHelperPanel#setCollectionLogNetImportCallback(Runnable)}):
 * <ul>
 *   <li>Resolves the logged-in player's name from {@link Client}.</li>
 *   <li>Submits the import to {@link CollectionLogNetImporter} and awaits the
 *       result on the supplied {@link ExecutorService} so the EDT is never
 *       blocked.</li>
 *   <li>Posts the user-facing toast message back via
 *       {@link CollectionLogHelperPanel#onCollectionLogNetImportComplete(String)}.</li>
 *   <li>On success, triggers a panel rebuild so the new obtained-item state
 *       renders immediately.</li>
 * </ul>
 *
 * <p>The {@link CollectionLogHelperPanel} and {@link ExecutorService} are
 * passed per-call rather than injected, because the panel is plugin-lifecycle
 * owned (created in {@code startUp}) and the executor is a shared daemon
 * lazily created by the plugin. This matches the pattern established by
 * {@link TempleSyncOrchestrator} and {@link LootSyncManager}.
 *
 * <p>Part of issue #503 — splitting the {@code CollectionLogHelperPlugin}
 * god-class into focused collaborators.
 */
@Slf4j
@Singleton
public class CollectionLogNetImportOrchestrator
{
	private static final String NOT_LOGGED_IN_MESSAGE = "Log in first";
	private static final String GENERIC_ERROR_MESSAGE = "collectionlog.net: error";

	private final Client client;
	private final SyncModule sync;

	@Inject
	public CollectionLogNetImportOrchestrator(Client client, SyncModule sync)
	{
		this.client = client;
		this.sync = sync;
	}

	/**
	 * Initiates an asynchronous collectionlog.net profile import for the
	 * currently logged-in player.
	 *
	 * <p>The import runs on the {@link CollectionLogNetImporter} background
	 * executor; on completion the result is surfaced to the panel via the
	 * supplied {@code resultExecutor}. If no player is logged in the panel is
	 * notified immediately and no import is dispatched.
	 *
	 * <p>Fail-soft: any error during the result wait is logged and surfaced as
	 * a generic toast; no exception reaches the caller.
	 *
	 * @param panel          the panel that owns the import button — used to
	 *                       surface the completion message. Must be non-null
	 *                       (the panel always owns the callback lifetime).
	 * @param resultExecutor daemon executor used to wait on the import future
	 *                       off the EDT/client thread.
	 */
	public void requestImport(CollectionLogHelperPanel panel, ExecutorService resultExecutor)
	{
		String username = client.getLocalPlayer() != null
			? client.getLocalPlayer().getName()
			: null;
		if (username == null || username.isEmpty())
		{
			panel.onCollectionLogNetImportComplete(NOT_LOGGED_IN_MESSAGE);
			return;
		}

		Future<ImportResult> future = sync.getCollectionLogNetImporter().importProfile(username);
		// Poll the result on the shared daemon executor so the EDT is not blocked
		resultExecutor.submit(() -> awaitResult(future, panel));
	}

	private void awaitResult(Future<ImportResult> future, CollectionLogHelperPanel panel)
	{
		try
		{
			ImportResult result = future.get();
			panel.onCollectionLogNetImportComplete(result.toToastMessage());
			if (result.isSuccess())
			{
				// Trigger a panel rebuild on the EDT
				panel.rebuild();
			}
		}
		catch (Exception e)
		{
			log.warn("collectionlog.net import result waiter failed", e);
			panel.onCollectionLogNetImportComplete(GENERIC_ERROR_MESSAGE);
		}
	}
}
