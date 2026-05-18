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
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.PlayerTravelCapabilities;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import com.collectionloghelper.lifecycle.AuthoringLogger;
import com.collectionloghelper.overlay.GuidanceOverlay;
import com.collectionloghelper.ui.CollectionLogHelperPanel;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemStack;

/**
 * Processes loot and inventory/bank container change events.
 *
 * <p>Owns the logic that previously lived in the plugin's {@code onNpcLootReceived}
 * and {@code onItemContainerChanged} handlers. The plugin keeps the {@code @Subscribe}
 * methods (so the RuneLite event bus discovers them via the existing
 * {@code eventBus.register(plugin)} call in {@code startUp}) and delegates the bodies
 * here.
 *
 * <p>Part of issue #503 — splitting the {@code CollectionLogHelperPlugin} god-class
 * into focused collaborators.
 */
@Slf4j
@Singleton
public class LootSyncManager
{
	private final Client client;
	private final CollectionLogHelperConfig config;
	private final DataModule data;
	private final GuidanceSequencer guidanceSequencer;
	private final GuidanceOverlayCoordinator guidanceCoordinator;
	private final GuidanceOverlay guidanceOverlay;
	private final AuthoringLogger authoringLogger;
	private final PlayerTravelCapabilities travelCapabilities;

	@Inject
	LootSyncManager(
		Client client,
		CollectionLogHelperConfig config,
		DataModule data,
		GuidanceSequencer guidanceSequencer,
		GuidanceOverlayCoordinator guidanceCoordinator,
		GuidanceOverlay guidanceOverlay,
		AuthoringLogger authoringLogger,
		PlayerTravelCapabilities travelCapabilities)
	{
		this.client = client;
		this.config = config;
		this.data = data;
		this.guidanceSequencer = guidanceSequencer;
		this.guidanceCoordinator = guidanceCoordinator;
		this.guidanceOverlay = guidanceOverlay;
		this.authoringLogger = authoringLogger;
		this.travelCapabilities = travelCapabilities;
	}

	/**
	 * Handles {@link NpcLootReceived} — emits a chat notification for newly obtained
	 * collection-log drops and pokes the guidance sequencer.
	 *
	 * <p>Marking the item obtained happens later on
	 * {@code ChatMessage} ("New item added to your collection log: ...") to stay
	 * authoritative with the game's own notification.
	 */
	public void handleNpcLoot(NpcLootReceived event)
	{
		for (ItemStack itemStack : event.getItems())
		{
			int itemId = itemStack.getId();
			CollectionLogItem item = data.getDatabase().getItemById(itemId);
			if (item != null && !data.getCollectionState().isItemObtained(itemId))
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"<col=00c8c8>[Collection Log Helper]</col> Collection log drop: " + item.getName(),
					null);
				guidanceSequencer.onItemObtained(itemId);
			}
		}
	}

	/**
	 * Handles {@link ItemContainerChanged} for inventory ({@link InventoryID#INV}),
	 * worn equipment ({@link InventoryID#WORN}), and bank ({@link InventoryID#BANK}).
	 *
	 * <p>The {@code panel} reference is passed in from the plugin because it is
	 * created lazily in the plugin's {@code startUp}, not via Guice. The plugin
	 * forwards a possibly-null panel; this method handles the null case.
	 *
	 * @param event the container-change event from the RuneLite event bus
	 * @param panel the side panel; may be {@code null} before {@code startUp} wires it
	 */
	public void handleItemContainerChanged(ItemContainerChanged event, CollectionLogHelperPanel panel)
	{
		log.debug("ItemContainerChanged fired: containerId={}, BANK={}, match={}",
			event.getContainerId(), InventoryID.BANK, event.getContainerId() == InventoryID.BANK);

		if (config.guidanceAuthoring())
		{
			authoringLogger.logContainerChange(event, client);
		}

		if (event.getContainerId() == InventoryID.INV)
		{
			ItemContainer invContainer = event.getItemContainer();
			if (invContainer != null)
			{
				data.getPlayerInventoryState().scanInventory(invContainer);

				if (guidanceSequencer.isActive())
				{
					guidanceSequencer.onInventoryChanged();
					guidanceCoordinator.onItemContainerChanged();
				}
			}
		}

		if (event.getContainerId() == InventoryID.WORN)
		{
			ItemContainer equipContainer = event.getItemContainer();
			if (equipContainer != null)
			{
				data.getPlayerInventoryState().scanEquipment(equipContainer);
			}
		}

		if (event.getContainerId() == InventoryID.BANK)
		{
			// Scan bank for clue-related items and travel teleports every time it updates
			ItemContainer bankContainer = event.getItemContainer();
			if (bankContainer != null)
			{
				data.getPlayerBankState().scanBank(bankContainer);
				travelCapabilities.scanBank(bankContainer);
			}

			if (!data.getDataSyncState().isBankScanned())
			{
				data.getDataSyncState().setBankScanned(true);
				guidanceOverlay.setShowBankReminder(false);
				log.info("Bank opened — marked as scanned for this session");
			}

			if (panel != null)
			{
				panel.updateDataSyncWarning();
				panel.updateClueSummary(data.getPlayerBankState());
			}
		}
	}
}
