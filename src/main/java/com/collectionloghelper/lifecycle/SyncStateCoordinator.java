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
import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.data.PlayerBankState;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

/**
 * Manages the auto-sync and collection log scan lifecycle.
 *
 * <p>Owns the 10 sync-related mutable fields extracted from the plugin class and
 * processes their state transitions. The plugin delegates all sync-tick logic
 * here via {@link #tickSync} and routes relevant events through the public
 * {@code on*} methods.
 */
@Slf4j
@Singleton
public class SyncStateCoordinator
{
	/**
	 * Client script 4100 fires once per obtained item when the collection log
	 * enters search mode. Args: [source, itemId, itemCount].
	 */
	private static final int SCRIPT_COLLECTION_LOG_ITEM = 4100;

	/** Client script 2240 triggers the collection log search mode. */
	private static final int SCRIPT_COLLECTION_LOG_SEARCH = 2240;

	/** Ticks to wait after the last script 4100 fires before finalising the scan. */
	private static final int SCAN_SETTLE_TICKS = 3;

	private final Client client;
	private final CollectionLogHelperConfig config;
	private final PlayerCollectionState collectionState;
	private final DataSyncState dataSyncState;
	private final PlayerBankState playerBankState;
	private final GuidanceOverlay guidanceOverlay;

	// ---- Sync state fields ----

	/** Set when the collection log widget opens; cleared once scan is triggered. */
	private boolean autoSyncPending;

	/** True while script 4100 is still firing items; false once settle countdown expires. */
	private boolean scriptScanActive;

	/** Number of items captured during the current script scan. */
	private int scriptScanItemCount;

	/** Ticks remaining before the scan is considered settled (no more script 4100 events). */
	private int scanSettleCountdown;

	/** True once a full sync has been completed for the current session. */
	private boolean hasCompletedFullSync;

	/** True while the in-game collection log interface is open. */
	private boolean collectionLogOpen;

	/** True once the login-time sync reminder has been sent this session. */
	private boolean syncReminderSent;

	/** Countdown ticks after login before showing sync reminders. */
	private int loginTickDelay;

	/** Last known total obtained count; used to detect changes in onVarbitChanged. */
	private int lastObtainedCount = -1;

	/** True once the collection log notification setting has been checked this session. */
	private boolean clogNotificationChecked;

	@Inject
	SyncStateCoordinator(
		Client client,
		CollectionLogHelperConfig config,
		PlayerCollectionState collectionState,
		DataSyncState dataSyncState,
		PlayerBankState playerBankState,
		GuidanceOverlay guidanceOverlay)
	{
		this.client = client;
		this.config = config;
		this.collectionState = collectionState;
		this.dataSyncState = dataSyncState;
		this.playerBankState = playerBankState;
		this.guidanceOverlay = guidanceOverlay;
	}

	// ---- Public accessors used by the plugin ----

	/**
	 * Returns true while script 4100 is actively firing.
	 * The plugin's {@code onVarbitChanged} uses this to suppress panel rebuilds mid-scan.
	 */
	public boolean isScriptScanActive()
	{
		return scriptScanActive;
	}

	public int getLastObtainedCount()
	{
		return lastObtainedCount;
	}

	public void setLastObtainedCount(int count)
	{
		lastObtainedCount = count;
	}

	/**
	 * Returns the current login tick delay counter.
	 * Used by the plugin's slayer refresh logic which fires at a specific delay threshold.
	 */
	public int getLoginTickDelay()
	{
		return loginTickDelay;
	}

	// ---- Event entry points ----

	/**
	 * Called from the plugin's {@code onWidgetLoaded} when the collection log group loads.
	 *
	 * @param groupId the widget group ID that was loaded
	 */
	public void onWidgetLoaded(int groupId)
	{
		int collectionLogGroupId = InterfaceID.Collection.FRAME >> 16;
		if (groupId != collectionLogGroupId)
		{
			return;
		}

		log.info("Collection log widget loaded (group {})", groupId);
		collectionLogOpen = true;
		autoSyncPending = true;
	}

	/**
	 * Called from the plugin's {@code onScriptPreFired} for every script event.
	 * Handles script 4100 (collection log item) to accumulate scan results.
	 *
	 * @param scriptId the fired script ID
	 * @param args     the script arguments
	 */
	public void onScriptPreFired(int scriptId, Object[] args)
	{
		if (scriptId != SCRIPT_COLLECTION_LOG_ITEM)
		{
			return;
		}

		// Script 4100 fires per obtained item during search-mode iteration
		// Args: [source_widget, itemId, itemCount]
		if (args != null && args.length >= 3)
		{
			int itemId = (int) args[1];
			if (itemId > 0)
			{
				collectionState.markItemObtained(itemId);
				scriptScanItemCount++;
			}
		}

		// Reset the settle countdown — more items may be coming
		scriptScanActive = true;
		scanSettleCountdown = SCAN_SETTLE_TICKS;
	}

	/**
	 * Handles all sync-related tick logic. Must be called from the plugin's
	 * {@code onGameTick} immediately after the slayer refresh block.
	 *
	 * <p>Execution order exactly matches the original plugin's {@code onGameTick}:
	 * <ol>
	 *   <li>Deferred cache-fresh check — promote to synced if cache matches last sync</li>
	 *   <li>One-time clog notification check</li>
	 *   <li>Auto-sync trigger — fire search scan once when log first opens</li>
	 *   <li>Scan settle countdown — finalise when script 4100 stops firing</li>
	 *   <li>Login tick delay countdown + sync reminders</li>
	 *   <li>Auto-dismiss expired overlay reminders</li>
	 *   <li>Detect collection log closed</li>
	 * </ol>
	 *
	 * @param panel         the panel for sync-status and rebuild calls; may be null
	 * @param panelCallback invoked whenever the panel should rebuild
	 * @param exportCallback invoked when an efficiency export should run
	 */
	public SyncTickResult tickSync(CollectionLogHelperPanel panel, Runnable panelCallback,
								  Runnable exportCallback)
	{
		boolean rankedDirty = false;

		// Deferred cache-fresh check: varps aren't loaded during LOGGED_IN or
		// RuneScapeProfileChanged, so retry here once totalObtained becomes valid
		if (!hasCompletedFullSync && collectionState.getTotalObtained() > 0
			&& collectionState.isCacheFresh())
		{
			dataSyncState.setCollectionLogSynced(true);
			hasCompletedFullSync = true;
			log.info("Cache is fresh (varp {} matches last sync) — skipping sync prompt",
				collectionState.getTotalObtained());

			if (playerBankState.loadFromCache())
			{
				dataSyncState.setBankScanned(true);
				log.info("Bank cache loaded — skipping bank scan prompt");
			}

			exportCallback.run();

			if (panel != null)
			{
				panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
					collectionState.getTotalObtained());
				panel.updateDataSyncWarning();
			}
			rankedDirty = true;
			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
			panelCallback.run();
		}

		// One-time check: warn if in-game collection log notification is disabled.
		// Without it, the "New item added to your collection log" chat message
		// never fires and our real-time detection silently fails.
		// Approach from C Engineer: Completed plugin (m0bilebtw).
		if (!clogNotificationChecked && loginTickDelay <= 5 && client.getGameState() == GameState.LOGGED_IN)
		{
			clogNotificationChecked = true;
			int setting = client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM);
			if (setting == 0 || setting == 2)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"[Collection Log Helper] Warning: Your collection log notification setting is " +
					"disabled. Enable it in Settings > All Settings > Collection Log > 'New item notification' " +
					"for real-time item detection.", "");
			}
		}

		// Auto-sync: trigger search mode when collection log first opens
		if (autoSyncPending && collectionLogOpen)
		{
			autoSyncPending = false;
			triggerSearchModeScan(panel);
		}

		// Wait for script 4100 to finish firing, then finalise
		if (scriptScanActive)
		{
			scanSettleCountdown--;
			if (scanSettleCountdown <= 0)
			{
				scriptScanActive = false;
				hasCompletedFullSync = true;
				dataSyncState.setCollectionLogSynced(true);
				guidanceOverlay.setShowCollectionLogReminder(false);
				collectionState.saveLastSyncedCount();
				int capturedCount = scriptScanItemCount;
				log.info("Auto-sync complete: {} obtained items captured via script scan",
					capturedCount);
				scriptScanItemCount = 0;
				if (panel != null)
				{
					panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCED,
						collectionState.getTotalObtained());
					panel.updateDataSyncWarning();
				}
				rankedDirty = true;
				panelCallback.run();
				exportCallback.run();
			}
		}

		// Send one-time sync reminders after login
		if (loginTickDelay > 0)
		{
			loginTickDelay--;
			if (loginTickDelay == 0 && !syncReminderSent)
			{
				syncReminderSent = true;
				if (config.showSyncReminder() && !hasCompletedFullSync)
				{
					guidanceOverlay.setShowCollectionLogReminder(true);
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						"<col=00c8c8>[Collection Log Helper]</col> Open your in-game Collection Log (click the quest tab icon) to sync progress.",
						null);
				}
				if (config.showBankScanReminder() && !dataSyncState.isBankScanned())
				{
					guidanceOverlay.setShowBankReminder(true);
				}
				if (panel != null)
				{
					panel.updateDataSyncWarning();
				}
				panelCallback.run();
			}
		}

		// Auto-dismiss overlay reminders after 2 minutes
		if (dataSyncState.isReminderExpired())
		{
			guidanceOverlay.setShowCollectionLogReminder(false);
			guidanceOverlay.setShowBankReminder(false);
		}

		// Detect collection log closed
		if (collectionLogOpen)
		{
			Widget frame = client.getWidget(InterfaceID.Collection.FRAME);
			if (frame == null || frame.isHidden())
			{
				collectionLogOpen = false;
			}
		}

		return rankedDirty ? SyncTickResult.RANKED_DIRTY : SyncTickResult.CLEAN;
	}

	/**
	 * Called when the game state transitions to {@code LOGGED_IN}.
	 * Resets sync state for a fresh login but avoids wiping flags mid-session
	 * (LOGGED_IN fires multiple times during transitions).
	 */
	public void onGameStateLoggedIn()
	{
		boolean freshLogin = loginTickDelay == 0 && !syncReminderSent;
		loginTickDelay = 10;
		if (freshLogin)
		{
			dataSyncState.reset();
			dataSyncState.setLoginTimestamp(System.currentTimeMillis());
		}
	}

	/**
	 * Called when the game state transitions to {@code LOGIN_SCREEN}.
	 * Resets all sync state for the next login.
	 */
	public void onGameStateLoginScreen()
	{
		collectionLogOpen = false;
		autoSyncPending = false;
		scriptScanActive = false;
		scanSettleCountdown = 0;
		hasCompletedFullSync = false;
		syncReminderSent = false;
		clogNotificationChecked = false;
		loginTickDelay = 0;
	}

	/**
	 * Called when the collection log item-count varbit changes.
	 * Updates the internal last-obtained-count cache.
	 *
	 * @param newCount the new total obtained count
	 */
	public void onCollectionStateChanged(int newCount)
	{
		lastObtainedCount = newCount;
	}

	/** Full state reset — call during {@code shutDown}. */
	public void reset()
	{
		lastObtainedCount = -1;
		collectionLogOpen = false;
		autoSyncPending = false;
		scriptScanActive = false;
		scanSettleCountdown = 0;
		hasCompletedFullSync = false;
		syncReminderSent = false;
		clogNotificationChecked = false;
		loginTickDelay = 0;
		scriptScanItemCount = 0;
	}

	// ---- Private helpers ----

	/**
	 * Programmatically triggers collection log search mode, which causes
	 * script 4100 to fire once per obtained item. This is the same technique
	 * used by TempleOSRS and WikiSync for full collection log scanning.
	 * After triggering search, immediately clicks "Back" so the user
	 * doesn't see the search UI flash.
	 */
	private void triggerSearchModeScan(CollectionLogHelperPanel panel)
	{
		scriptScanItemCount = 0;
		scriptScanActive = false;
		scanSettleCountdown = SCAN_SETTLE_TICKS;

		if (panel != null)
		{
			panel.updateSyncStatus(CollectionLogHelperPanel.SyncState.SYNCING, 0);
		}

		log.info("Triggering collection log search-mode scan");

		// Click the Search toggle to enter search mode (fires script 4100 for each obtained item)
		client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE,
			MenuAction.CC_OP, 1, -1, "Search", null);

		// Run the search script to complete the transition
		client.runScript(SCRIPT_COLLECTION_LOG_SEARCH);

		// Click Back to exit search mode so the UI returns to normal
		client.menuAction(-1, InterfaceID.Collection.SEARCH_TOGGLE,
			MenuAction.CC_OP, 1, -1, "Back", null);
	}

	/**
	 * Result token returned by {@link #tickSync} to communicate state changes
	 * back to the plugin without direct panel/calculator references.
	 */
	public enum SyncTickResult
	{
		/** No ranked-sources invalidation occurred this tick. */
		CLEAN,
		/** The ranked sources cache should be marked dirty this tick. */
		RANKED_DIRTY
	}
}
