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
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

/**
 * Routes guidance- and authoring-specific events that were previously handled
 * inline in {@code CollectionLogHelperPlugin}.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code onActorDeath} — forwards NPC deaths to the guidance sequencer</li>
 *   <li>{@code onAnimationChanged} — authoring log only</li>
 *   <li>{@code onNpcSpawned} / {@code onNpcDespawned} — authoring log + coordinator NPC tracking</li>
 *   <li>{@code onInteractingChanged} — guidance NPC_TALKED_TO condition</li>
 *   <li>{@code onHitsplatApplied} — authoring log only</li>
 *   <li>{@code onWidgetLoaded} — authoring dialog capture + sync coordinator delegation</li>
 *   <li>{@code onMenuEntryAdded} — "Collection Log Guide" right-click injection</li>
 *   <li>{@code onMenuOptionClicked} — guidance activation + authoring interaction log</li>
 * </ul>
 *
 * <p>Must be registered on the EventBus via {@code eventBus.register(this)} in
 * {@code Plugin.startUp()} and unregistered in {@code Plugin.shutDown()}.
 *
 * <p>Two callbacks must be set via setters in {@code startUp()} before
 * the router receives any events:
 * <ul>
 *   <li>{@link #setMissingItemsSupplier(Supplier)} — supplies the cached set of source
 *       names that have at least one unobtained item (owned by the plugin)</li>
 *   <li>{@link #setActivateGuidanceCallback(Consumer)} — invokes
 *       {@code CollectionLogHelperPlugin.activateGuidance()} without a direct
 *       plugin reference</li>
 * </ul>
 */
@Slf4j
@Singleton
public class GuidanceEventRouter
{
	/** Right-click menu option label injected onto NPCs with incomplete collection log entries. */
	static final String MENU_OPTION_GUIDE = "Collection Log Guide";

	private final Client client;
	private final ClientThread clientThread;
	private final CollectionLogHelperConfig config;
	private final AuthoringLogger authoringLogger;
	private final GuidanceSequencer guidanceSequencer;
	private final GuidanceOverlayCoordinator guidanceCoordinator;
	private final SyncStateCoordinator syncStateCoordinator;
	private final DropRateDatabase database;

	/**
	 * Supplies the live set of source names with at least one missing item.
	 * Set once by the plugin in {@code startUp()}.
	 */
	private Supplier<Set<String>> missingItemsSupplier;

	/**
	 * Callback to activate guidance for a source — delegates to
	 * {@code CollectionLogHelperPlugin.activateGuidance(CollectionLogSource)}.
	 * Set once by the plugin in {@code startUp()}.
	 */
	private Consumer<CollectionLogSource> activateGuidanceCallback;

	@Inject
	public GuidanceEventRouter(
		Client client,
		ClientThread clientThread,
		CollectionLogHelperConfig config,
		AuthoringLogger authoringLogger,
		GuidanceSequencer guidanceSequencer,
		GuidanceOverlayCoordinator guidanceCoordinator,
		SyncStateCoordinator syncStateCoordinator,
		DropRateDatabase database)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.authoringLogger = authoringLogger;
		this.guidanceSequencer = guidanceSequencer;
		this.guidanceCoordinator = guidanceCoordinator;
		this.syncStateCoordinator = syncStateCoordinator;
		this.database = database;
	}

	/**
	 * Sets the supplier that returns the cached set of source names with missing items.
	 * Must be called from {@code Plugin.startUp()} before any events are processed.
	 */
	public void setMissingItemsSupplier(Supplier<Set<String>> supplier)
	{
		this.missingItemsSupplier = supplier;
	}

	/**
	 * Sets the callback used to activate guidance for a source.
	 * Must be called from {@code Plugin.startUp()} before any events are processed.
	 */
	public void setActivateGuidanceCallback(Consumer<CollectionLogSource> callback)
	{
		this.activateGuidanceCallback = callback;
	}

	// ── Actor death ─────────────────────────────────────────────────────────

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (!guidanceSequencer.isActive())
		{
			return;
		}
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			if (config.guidanceAuthoring())
			{
				authoringLogger.log("DEATH npcId=%d name='%s'", npc.getId(), npc.getName());
			}
			guidanceSequencer.onNpcDeath(npc.getId());
		}
	}

	// ── Animation ────────────────────────────────────────────────────────────

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!config.guidanceAuthoring() || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		int animId = client.getLocalPlayer().getAnimation();
		if (animId != -1)
		{
			authoringLogger.log("ANIMATION player=%d", animId);
		}
	}

	// ── NPC lifecycle ─────────────────────────────────────────────────────────

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();

		if (config.guidanceAuthoring())
		{
			authoringLogger.log("NPC_SPAWN id=%d name='%s' index=%d", npc.getId(), npc.getName(), npc.getIndex());
		}

		// Track the spawned NPC if it matches the current guidance step's target
		guidanceCoordinator.onNpcSpawned(npc);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();

		if (config.guidanceAuthoring())
		{
			authoringLogger.log("NPC_DESPAWN id=%d name='%s'", npc.getId(), npc.getName());
		}

		// Clear tracked NPC if it despawned
		guidanceCoordinator.onNpcDespawned(npc);
	}

	// ── Interaction ──────────────────────────────────────────────────────────

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		if (!guidanceSequencer.isActive())
		{
			return;
		}

		if (event.getSource() == client.getLocalPlayer() && event.getTarget() instanceof NPC)
		{
			NPC npc = (NPC) event.getTarget();
			GuidanceStep step = guidanceSequencer.getRawCurrentStep();
			if (step != null && step.getCompletionCondition() == CompletionCondition.NPC_TALKED_TO
				&& step.getCompletionNpcId() == npc.getId())
			{
				guidanceSequencer.onNpcInteracted(npc.getId());
			}
		}
	}

	// ── Hitsplat ─────────────────────────────────────────────────────────────

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!config.guidanceAuthoring())
		{
			return;
		}
		if (event.getActor() == client.getLocalPlayer())
		{
			authoringLogger.log("HITSPLAT_RECEIVED type=%d amount=%d",
				event.getHitsplat().getHitsplatType(), event.getHitsplat().getAmount());
		}
		else if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			authoringLogger.log("HITSPLAT_DEALT npcId=%d name='%s' type=%d amount=%d",
				npc.getId(), npc.getName(),
				event.getHitsplat().getHitsplatType(), event.getHitsplat().getAmount());
		}
	}

	// ── Widget loaded ─────────────────────────────────────────────────────────

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		// Capture dialog widget text for authoring mode
		if (config.guidanceAuthoring())
		{
			// Player dialog choices (group 219)
			if (event.getGroupId() == 219)
			{
				clientThread.invokeLater(() ->
				{
					Widget container = client.getWidget(219, 1);
					if (container != null)
					{
						Widget[] children = container.getDynamicChildren();
						if (children != null)
						{
							StringBuilder sb = new StringBuilder("DIALOG_OPTIONS");
							for (Widget child : children)
							{
								if (child != null && child.getText() != null && !child.getText().isEmpty())
								{
									sb.append(" '").append(child.getText()).append("'");
								}
							}
							authoringLogger.log(sb.toString());
						}
					}
				});
			}
			// NPC dialog (group 231)
			if (event.getGroupId() == 231)
			{
				clientThread.invokeLater(() ->
				{
					Widget textWidget = client.getWidget(231, 4);
					if (textWidget != null && textWidget.getText() != null)
					{
						authoringLogger.log("DIALOG_NPC text='%s'", textWidget.getText());
					}
				});
			}
		}

		syncStateCoordinator.onWidgetLoaded(event.getGroupId());
	}

	// ── Menu entry injection ──────────────────────────────────────────────────

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!config.showOverlays())
		{
			return;
		}

		int type = event.getType();
		if (type < MenuAction.NPC_FIRST_OPTION.getId() || type > MenuAction.NPC_FIFTH_OPTION.getId())
		{
			return;
		}

		NPC npc = event.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}

		int npcId = npc.getId();
		CollectionLogSource source = database.getSourceByNpcId(npcId);
		if (source == null)
		{
			return;
		}

		// Skip if guidance is already active for this source
		if (guidanceCoordinator.isSourceGuided(source))
		{
			return;
		}

		// Check if source has any missing items (O(1) cached lookup)
		Set<String> missingItems = missingItemsSupplier != null ? missingItemsSupplier.get() : null;
		if (missingItems == null || !missingItems.contains(source.getName()))
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption(MENU_OPTION_GUIDE)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.setIdentifier(npcId);
	}

	// ── Menu option click ─────────────────────────────────────────────────────

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();

		// Handle "Collection Log Guide" right-click menu action
		if (action == MenuAction.RUNELITE && MENU_OPTION_GUIDE.equals(event.getMenuOption()))
		{
			CollectionLogSource source = database.getSourceByNpcId(event.getId());
			if (source != null && activateGuidanceCallback != null)
			{
				activateGuidanceCallback.accept(source);
			}
			return;
		}

		// Authoring mode: log all interactions regardless of guidance state
		if (config.guidanceAuthoring())
		{
			authoringLogger.log("MENU option='%s' target='%s' action=%s id=%d param0=%d param1=%d",
				event.getMenuOption(), event.getMenuTarget(), action,
				event.getId(), event.getParam0(), event.getParam1());

			if (action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
				|| action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
				|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION)
			{
				authoringLogger.log("OBJECT id=%d option='%s'", event.getId(), event.getMenuOption());
			}
			else if (action == MenuAction.NPC_FIRST_OPTION || action == MenuAction.NPC_SECOND_OPTION
				|| action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_FOURTH_OPTION
				|| action == MenuAction.NPC_FIFTH_OPTION)
			{
				NPC npc = event.getMenuEntry().getNpc();
				if (npc != null)
				{
					authoringLogger.log("NPC id=%d name='%s' option='%s'",
						npc.getId(), npc.getName(), event.getMenuOption());
				}
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
			{
				authoringLogger.log("USE_ITEM_ON_OBJECT objectId=%d itemId=%d", event.getId(), event.getParam0());
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_NPC)
			{
				authoringLogger.log("USE_ITEM_ON_NPC npcIndex=%d", event.getId());
			}
			else if (action == MenuAction.WIDGET_TARGET_ON_WIDGET)
			{
				authoringLogger.log("USE_ITEM_ON_ITEM param0=%d param1=%d", event.getParam0(), event.getParam1());
			}
		}

		if (!guidanceSequencer.isActive())
		{
			return;
		}

		// Track cumulative use-item-on-object actions for guidance (e.g., Trouble Brewing hopper)
		if (action == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT)
		{
			CollectionLogSource source = guidanceSequencer.getActiveSource();
			if (source != null && source.getCumulativeTrackItemId() > 0
					&& source.getCumulativeTrackObjectIds() != null)
			{
				int objectId = event.getId();
				int itemId = event.getParam0();
				if (itemId == source.getCumulativeTrackItemId()
						&& source.getCumulativeTrackObjectIds().contains(objectId))
				{
					guidanceSequencer.onTrackedAction();
				}
			}
		}

		// Detect NPC interactions for NPC_TALKED_TO completion condition.
		if (action == MenuAction.NPC_FIRST_OPTION || action == MenuAction.NPC_SECOND_OPTION
			|| action == MenuAction.NPC_THIRD_OPTION || action == MenuAction.NPC_FOURTH_OPTION
			|| action == MenuAction.NPC_FIFTH_OPTION)
		{
			NPC npc = event.getMenuEntry().getNpc();
			if (npc != null)
			{
				guidanceSequencer.onNpcInteracted(npc.getId());
			}
		}
	}
}
