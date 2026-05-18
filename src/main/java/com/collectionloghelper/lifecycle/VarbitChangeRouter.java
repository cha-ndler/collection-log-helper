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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.di.SyncModule;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;

/**
 * Processes {@link VarbitChanged} events on the client thread.
 *
 * <p>Owns the body that previously lived inline in
 * {@code CollectionLogHelperPlugin.onVarbitChanged}. The plugin keeps the
 * {@code @Subscribe} annotation (so the RuneLite event bus discovers it via
 * the existing {@code eventBus.register(plugin)} call in {@code startUp}) and
 * delegates to {@link #handle(VarbitChanged)}.
 *
 * <p>Five responsibilities are kept here:
 * <ol>
 *   <li>Refresh the cached varp snapshot.</li>
 *   <li>Throttled authoring-log line (one per tick).</li>
 *   <li>Forward to {@code GuidanceSequencer.onVarbitChanged}.</li>
 *   <li>Detect Slayer task deltas (active / creature / remaining).</li>
 *   <li>Gate the obtained-count delta on the script-scan flag and forward
 *       to {@link SyncStateCoordinator}.</li>
 * </ol>
 *
 * <p>Three narrow callbacks back to the plugin keep the per-tick refresh
 * flags and rebuild triggers where they live today, without leaking those
 * fields into this collaborator:
 * <ul>
 *   <li>{@code flagRefreshes} -- set both
 *       {@code pendingRequirementsRefresh} and
 *       {@code pendingTravelVarbitRefresh} on the plugin.</li>
 *   <li>{@code onCollectionStateChanged} -- rebuild the sources-with-missing
 *       cache when the obtained-item count moves.</li>
 *   <li>{@code onSlayerTaskChanged} -- flag a panel rebuild + ranked-source
 *       recompute when the Slayer task delta fires.</li>
 * </ul>
 *
 * <p>Performance: {@link VarbitChanged} fires extremely frequently in OSRS
 * (hundreds of times during login alone). The no-op / guard-fail path
 * allocates nothing -- there are no per-event {@code new} sites and no
 * lambdas captured here.
 *
 * <p>Part of issue #503 -- splitting {@code CollectionLogHelperPlugin} into
 * focused collaborators (Wave 12).
 */
@Slf4j
@Singleton
public class VarbitChangeRouter
{
	private final Client client;
	private final CollectionLogHelperConfig config;
	private final DataModule data;
	private final GuidanceModule guidance;
	private final SyncModule sync;
	private final AuthoringLogger authoringLogger;

	private Runnable flagRefreshes;
	private Runnable onCollectionStateChanged;
	private Runnable onSlayerTaskChanged;

	@Inject
	VarbitChangeRouter(
		Client client,
		CollectionLogHelperConfig config,
		DataModule data,
		GuidanceModule guidance,
		SyncModule sync,
		AuthoringLogger authoringLogger)
	{
		this.client = client;
		this.config = config;
		this.data = data;
		this.guidance = guidance;
		this.sync = sync;
		this.authoringLogger = authoringLogger;
	}

	/**
	 * Wires the callbacks back to the plugin's per-tick refresh flags and
	 * rebuild triggers. Called once from {@code CollectionLogHelperPlugin.startUp()}
	 * after the plugin's private state is ready. Kept off the constructor to
	 * avoid a circular Guice dependency on the plugin itself.
	 */
	public void setCallbacks(
		Runnable flagRefreshes,
		Runnable onCollectionStateChanged,
		Runnable onSlayerTaskChanged)
	{
		this.flagRefreshes = flagRefreshes;
		this.onCollectionStateChanged = onCollectionStateChanged;
		this.onSlayerTaskChanged = onSlayerTaskChanged;
	}

	/**
	 * Handles a single {@link VarbitChanged} event. Runs on the client thread,
	 * so client-API access is safe.
	 */
	public void handle(VarbitChanged event)
	{
		// Runs on the client thread -- safe to call client API
		data.getCollectionState().refreshVarps();

		// Authoring: log varbit changes (throttle to 1 per tick to avoid spam)
		if (config.guidanceAuthoring() && client.getTickCount() != authoringLogger.getLastVarbitLogTick())
		{
			authoringLogger.setLastVarbitLogTick(client.getTickCount());
			authoringLogger.log("VARBIT_CHANGED varbitId=%d value=%d",
				event.getVarbitId(), event.getValue());
		}

		// Forward varbit change to guidance sequencer for VARBIT_AT_LEAST completion
		guidance.getGuidanceSequencer().onVarbitChanged(event.getVarbitId(), event.getValue());

		// Refresh Slayer task state and rebuild if the task changed
		boolean wasActive = data.getSlayerTaskState().isTaskActive();
		String oldCreature = data.getSlayerTaskState().getCreatureName();
		int oldRemaining = data.getSlayerTaskState().getRemaining();
		data.getSlayerTaskState().refresh();

		boolean slayerChanged = wasActive != data.getSlayerTaskState().isTaskActive()
			|| (data.getSlayerTaskState().getCreatureName() != null
				&& !data.getSlayerTaskState().getCreatureName().equals(oldCreature))
			|| data.getSlayerTaskState().getRemaining() != oldRemaining;

		// Flag requirements and travel capabilities for refresh on next game tick (debounced).
		// Quest states are varbits that fire hundreds of times during login,
		// so we can't call refreshAccessibility() here -- it scans all sources.
		if (flagRefreshes != null)
		{
			flagRefreshes.run();
		}

		// Don't trigger rebuilds mid-scan; the settle logic in onGameTick
		// will fire a single rebuild once script 4100 stops firing.
		if (sync.getSyncStateCoordinator().isScriptScanActive())
		{
			return;
		}

		int currentCount = data.getCollectionState().getTotalObtained();
		if (currentCount != sync.getSyncStateCoordinator().getLastObtainedCount())
		{
			sync.getSyncStateCoordinator().onCollectionStateChanged(currentCount);
			if (onCollectionStateChanged != null)
			{
				onCollectionStateChanged.run();
			}
			slayerChanged = true; // rebuild anyway
		}

		if (slayerChanged && onSlayerTaskChanged != null)
		{
			onSlayerTaskChanged.run();
		}
	}
}
