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
package com.collectionloghelper.chat;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.di.DataModule;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
import com.collectionloghelper.efficiency.ScoredItem;
import com.collectionloghelper.lifecycle.AuthoringLogger;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;

/**
 * Processes chat-message events and the {@code /clh} chat command.
 *
 * <p>Owns the logic that previously lived in the plugin's {@code onChatMessage}
 * and {@code onClhCommand} handlers. The plugin keeps the {@code @Subscribe}
 * method (so the RuneLite event bus discovers it via the existing
 * {@code eventBus.register(plugin)} call in {@code startUp}) and delegates the
 * body here. The plugin also keeps the {@code chatCommandManager.registerCommand}
 * call but binds it to {@link #onClhCommand} on this class.
 *
 * <p>Two narrow callbacks back to the plugin keep ranked-source caching state
 * where it lives today (see {@code getRankedSources()} / {@code rankedSourcesDirty}
 * on the plugin) without leaking those fields into this collaborator:
 * <ul>
 *   <li>{@code rankedSourcesSupplier} — read the cached ranked-efficiency list</li>
 *   <li>{@code onCollectionLogItemObtained} — flag a panel rebuild + invalidate
 *       the ranked-sources cache when a new collection-log item is detected</li>
 * </ul>
 *
 * <p>Part of issue #503 — splitting the {@code CollectionLogHelperPlugin}
 * god-class into focused collaborators.
 */
@Slf4j
@Singleton
public class ChatEventHandler
{
	private static final Pattern COLLECTION_LOG_PATTERN =
		Pattern.compile("New item added to your collection log: (.*)");

	private final Client client;
	private final ClientThread clientThread;
	private final CollectionLogHelperConfig config;
	private final DataModule data;
	private final GuidanceModule guidance;
	private final AuthoringLogger authoringLogger;

	private Supplier<List<ScoredItem>> rankedSourcesSupplier;
	private Runnable onCollectionLogItemObtained;

	@Inject
	ChatEventHandler(
		Client client,
		ClientThread clientThread,
		CollectionLogHelperConfig config,
		DataModule data,
		GuidanceModule guidance,
		AuthoringLogger authoringLogger)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.data = data;
		this.guidance = guidance;
		this.authoringLogger = authoringLogger;
	}

	/**
	 * Wires the callbacks back to the plugin's ranked-source cache.
	 *
	 * <p>Called once from {@code CollectionLogHelperPlugin.startUp()} after the
	 * plugin's private cache state is ready. Kept off the constructor to avoid
	 * a circular Guice dependency on the plugin itself.
	 */
	public void setCallbacks(
		Supplier<List<ScoredItem>> rankedSourcesSupplier,
		Runnable onCollectionLogItemObtained)
	{
		this.rankedSourcesSupplier = rankedSourcesSupplier;
		this.onCollectionLogItemObtained = onCollectionLogItemObtained;
	}

	/**
	 * Handles {@link ChatMessage} game/spam messages.
	 *
	 * <p>Forwards to the guidance sequencer's {@code CHAT_MESSAGE_RECEIVED}
	 * condition and matches the "New item added to your collection log: ..."
	 * notification to mark items obtained.
	 */
	public void handleChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE
			&& event.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		if (config.guidanceAuthoring())
		{
			authoringLogger.log("CHAT type=%s msg='%s'", event.getType(), event.getMessage());
		}

		// Forward chat messages to guidance sequencer for CHAT_MESSAGE_RECEIVED condition
		if (guidance.getGuidanceSequencer().isActive())
		{
			guidance.getGuidanceSequencer().onChatMessage(Text.removeTags(event.getMessage()));
		}

		Matcher matcher = COLLECTION_LOG_PATTERN.matcher(Text.removeTags(event.getMessage()));
		if (matcher.find())
		{
			String itemName = matcher.group(1);
			log.debug("New collection log item: {}", itemName);

			// O(1) lookup by name instead of scanning all sources × items
			CollectionLogItem item = data.getDatabase().getItemByName(itemName);
			if (item != null)
			{
				data.getCollectionState().markItemObtained(item.getItemId());
				guidance.getGuidanceSequencer().onItemObtained(item.getItemId());
				log.debug("Marked item {} (ID: {}) as obtained", itemName, item.getItemId());
			}

			if (onCollectionLogItemObtained != null)
			{
				onCollectionLogItemObtained.run();
			}
		}
	}

	/**
	 * Handles the {@code /clh} chat command — prints a one-line summary of
	 * collection-log progress, the active guidance step, and the current top
	 * efficiency pick.
	 *
	 * <p>Bound from the plugin via
	 * {@code chatCommandManager.registerCommand("clh", chatEventHandler::onClhCommand)}.
	 */
	public void onClhCommand(ChatMessage chatMessage, String message)
	{
		int obtained = data.getCollectionState().getTotalObtained();
		int total = data.getCollectionState().getTotalPossible();
		String progressLine;
		if (total > 0)
		{
			double pct = (obtained * 100.0) / total;
			progressLine = String.format("Collection Log: %d/%d (%.1f%%)", obtained, total, pct);
		}
		else
		{
			progressLine = "Collection Log: not synced";
		}

		String guidanceLine;
		if (guidance.getGuidanceSequencer().isActive() && guidance.getGuidanceSequencer().getActiveSource() != null)
		{
			String sourceName = guidance.getGuidanceSequencer().getActiveSource().getName();
			int step = guidance.getGuidanceSequencer().getCurrentIndex() + 1;
			int totalSteps = guidance.getGuidanceSequencer().getTotalSteps();
			guidanceLine = String.format("Guiding: %s step %d/%d", sourceName, step, totalSteps);
		}
		else
		{
			guidanceLine = "No active guidance";
		}

		String topPickLine;
		List<ScoredItem> ranked = rankedSourcesSupplier != null
			? rankedSourcesSupplier.get()
			: List.of();
		ScoredItem topPick = ranked.stream()
			.filter(s -> !s.isLocked())
			.findFirst()
			.orElse(null);
		if (topPick != null)
		{
			double hours = topPick.getScore() > 0 ? 100.0 / topPick.getScore() : 0;
			String timeStr = ClueCompletionEstimator.formatTime((int) (hours * 3600));
			topPickLine = String.format("Top pick: %s (~%s)", topPick.getSource().getName(), timeStr);
		}
		else
		{
			topPickLine = "Top pick: none available";
		}

		String output = "<col=00c8c8>[Collection Log Helper]</col> "
			+ progressLine + " | " + guidanceLine + " | " + topPickLine;
		clientThread.invokeLater(() ->
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", output, null));
	}
}
