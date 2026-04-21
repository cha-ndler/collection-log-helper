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
import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import com.collectionloghelper.guidance.GuidanceSequencer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GuidanceEventRouterTest
{
	@Mock
	private Client client;

	@Mock
	private net.runelite.client.callback.ClientThread clientThread;

	@Mock
	private CollectionLogHelperConfig config;

	@Mock
	private AuthoringLogger authoringLogger;

	@Mock
	private GuidanceSequencer guidanceSequencer;

	@Mock
	private GuidanceOverlayCoordinator guidanceCoordinator;

	@Mock
	private SyncStateCoordinator syncStateCoordinator;

	@Mock
	private DropRateDatabase database;

	@Mock
	private NPC npc;

	@Mock
	private Player localPlayer;

	@Mock
	private Hitsplat hitsplat;

	private GuidanceEventRouter router;
	private Set<String> missingItems;

	// ---- factory helpers ----

	/** Minimal GuidanceStep with NPC_TALKED_TO condition. */
	private static GuidanceStep makeNpcTalkedToStep(int npcId)
	{
		return new GuidanceStep(
			"Talk to NPC",
			0, 0, 0,
			0, null, null,
			null, null,
			CompletionCondition.NPC_TALKED_TO,
			0, 0, 0, npcId,
			null,  // completionNpcIds
			null,  // worldMessage
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,     // objectMaxDistance
			null,  // objectFilterTiles
			null,  // highlightWidgetIds
			0, 0,  // loopBackToStep, loopCount
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			null,  // conditionalAlternatives
			null   // completionNode
		);
	}

	/**
	 * Minimal CollectionLogSource with just a name (for menu-entry tests).
	 */
	private static CollectionLogSource makeSource(String name)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			0, 0, 0,   // worldX, worldY, worldPlane
			0, 0,      // killTimeSeconds, ironKillTimeSeconds
			null,      // locationDescription
			null,      // waypoints
			null,      // rewardType
			0.0,       // pointsPerHour
			null,      // mutuallyExclusiveSources
			0,         // rollsPerKill
			false,     // aggregated
			0,         // afkLevel
			null,      // travelTip
			0,         // npcId
			null,      // interactAction
			null,      // dialogOptions
			null,      // guidanceSteps
			null,      // requirements
			0,         // cumulativeTrackItemId
			null,      // cumulativeTrackObjectIds
			0,         // cumulativeTrackThreshold
			null       // items
		);
	}

	/**
	 * CollectionLogSource with cumulative-track fields set (for track-action test).
	 */
	private static CollectionLogSource makeSourceWithTrack(int trackItemId, List<Integer> objectIds)
	{
		return new CollectionLogSource(
			"Track Source",
			CollectionLogCategory.BOSSES,
			0, 0, 0,
			0, 0,
			null,
			null,
			null,
			0.0,
			null,
			0,
			false,
			0,
			null,
			0,
			null,
			null,
			null,
			null,
			trackItemId,
			objectIds,
			0,
			null
		);
	}

	@Before
	public void setUp()
	{
		router = new GuidanceEventRouter(
			client, clientThread, config, authoringLogger,
			guidanceSequencer, guidanceCoordinator, syncStateCoordinator, database);
		missingItems = new HashSet<>();
		router.setMissingItemsSupplier(() -> missingItems);
		router.setActivateGuidanceCallback(s -> {});

		// Default: authoring off, sequencer inactive
		when(config.guidanceAuthoring()).thenReturn(false);
		when(guidanceSequencer.isActive()).thenReturn(false);
		when(client.getLocalPlayer()).thenReturn(localPlayer);
	}

	// ========================================================================
	// onActorDeath
	// ========================================================================

	@Test
	public void testOnActorDeathForwardsNpcDeathToSequencer()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(npc.getId()).thenReturn(1234);
		ActorDeath event = new ActorDeath(npc);

		// Act
		router.onActorDeath(event);

		// Assert
		verify(guidanceSequencer).onNpcDeath(1234);
	}

	@Test
	public void testOnActorDeathIgnoredWhenSequencerInactive()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(false);
		ActorDeath event = new ActorDeath(npc);

		// Act
		router.onActorDeath(event);

		// Assert
		verify(guidanceSequencer, never()).onNpcDeath(anyInt());
	}

	@Test
	public void testOnActorDeathLogsWhenAuthoringEnabled()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(config.guidanceAuthoring()).thenReturn(true);
		when(npc.getId()).thenReturn(42);
		when(npc.getName()).thenReturn("Guard");
		ActorDeath event = new ActorDeath(npc);

		// Act
		router.onActorDeath(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(42), eq("Guard"));
	}

	@Test
	public void testOnActorDeathSkipsNonNpcActor()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		Actor nonNpc = mock(Actor.class);
		ActorDeath event = new ActorDeath(nonNpc);

		// Act
		router.onActorDeath(event);

		// Assert
		verify(guidanceSequencer, never()).onNpcDeath(anyInt());
	}

	// ========================================================================
	// onAnimationChanged
	// ========================================================================

	@Test
	public void testOnAnimationChangedLogsWhenAuthoringAndLocalPlayer()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		when(localPlayer.getAnimation()).thenReturn(832);
		AnimationChanged event = new AnimationChanged();
		event.setActor(localPlayer);

		// Act
		router.onAnimationChanged(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(832));
	}

	@Test
	public void testOnAnimationChangedSkipsNonLocalPlayer()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		NPC otherActor = mock(NPC.class);
		AnimationChanged event = new AnimationChanged();
		event.setActor(otherActor);

		// Act
		router.onAnimationChanged(event);

		// Assert — no log calls
		verifyNoInteractions(authoringLogger);
	}

	@Test
	public void testOnAnimationChangedSkipsWhenAuthoringOff()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(false);
		AnimationChanged event = new AnimationChanged();
		event.setActor(localPlayer);

		// Act
		router.onAnimationChanged(event);

		// Assert
		verifyNoInteractions(authoringLogger);
	}

	// ========================================================================
	// onNpcSpawned / onNpcDespawned
	// ========================================================================

	@Test
	public void testOnNpcSpawnedForwardsToCoordinator()
	{
		// Arrange
		NpcSpawned event = new NpcSpawned(npc);

		// Act
		router.onNpcSpawned(event);

		// Assert
		verify(guidanceCoordinator).onNpcSpawned(npc);
	}

	@Test
	public void testOnNpcSpawnedLogsWhenAuthoringEnabled()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		when(npc.getId()).thenReturn(100);
		when(npc.getName()).thenReturn("Goblin");
		when(npc.getIndex()).thenReturn(5);
		NpcSpawned event = new NpcSpawned(npc);

		// Act
		router.onNpcSpawned(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(100), eq("Goblin"), eq(5));
	}

	@Test
	public void testOnNpcDespawnedForwardsToCoordinator()
	{
		// Arrange
		NpcDespawned event = new NpcDespawned(npc);

		// Act
		router.onNpcDespawned(event);

		// Assert
		verify(guidanceCoordinator).onNpcDespawned(npc);
	}

	@Test
	public void testOnNpcDespawnedLogsWhenAuthoringEnabled()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		when(npc.getId()).thenReturn(200);
		when(npc.getName()).thenReturn("Rat");
		NpcDespawned event = new NpcDespawned(npc);

		// Act
		router.onNpcDespawned(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(200), eq("Rat"));
	}

	// ========================================================================
	// onInteractingChanged
	// ========================================================================

	@Test
	public void testOnInteractingChangedForwardsNpcInteractionWhenConditionMatches()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		GuidanceStep step = makeNpcTalkedToStep(999);
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(npc.getId()).thenReturn(999);

		InteractingChanged event = new InteractingChanged(localPlayer, npc);

		// Act
		router.onInteractingChanged(event);

		// Assert
		verify(guidanceSequencer).onNpcInteracted(999);
	}

	@Test
	public void testOnInteractingChangedIgnoredWhenSequencerInactive()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(false);
		InteractingChanged event = new InteractingChanged(localPlayer, npc);

		// Act
		router.onInteractingChanged(event);

		// Assert
		verify(guidanceSequencer, never()).onNpcInteracted(anyInt());
	}

	@Test
	public void testOnInteractingChangedIgnoredWhenNpcIdMismatch()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		GuidanceStep step = makeNpcTalkedToStep(100); // step expects NPC 100
		when(guidanceSequencer.getRawCurrentStep()).thenReturn(step);
		when(npc.getId()).thenReturn(999); // different NPC

		InteractingChanged event = new InteractingChanged(localPlayer, npc);

		// Act
		router.onInteractingChanged(event);

		// Assert
		verify(guidanceSequencer, never()).onNpcInteracted(anyInt());
	}

	// ========================================================================
	// onHitsplatApplied
	// ========================================================================

	@Test
	public void testOnHitsplatAppliedLogsReceivedOnLocalPlayer()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		when(hitsplat.getHitsplatType()).thenReturn(1);
		when(hitsplat.getAmount()).thenReturn(10);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(localPlayer);
		event.setHitsplat(hitsplat);

		// Act
		router.onHitsplatApplied(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(1), eq(10));
	}

	@Test
	public void testOnHitsplatAppliedLogsDealtOnNpc()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(true);
		when(npc.getId()).thenReturn(55);
		when(npc.getName()).thenReturn("Spider");
		when(hitsplat.getHitsplatType()).thenReturn(2);
		when(hitsplat.getAmount()).thenReturn(15);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(npc);
		event.setHitsplat(hitsplat);

		// Act
		router.onHitsplatApplied(event);

		// Assert
		verify(authoringLogger).log(anyString(), eq(55), eq("Spider"), eq(2), eq(15));
	}

	@Test
	public void testOnHitsplatAppliedSkipsWhenAuthoringOff()
	{
		// Arrange
		when(config.guidanceAuthoring()).thenReturn(false);
		HitsplatApplied event = new HitsplatApplied();
		event.setActor(localPlayer);
		event.setHitsplat(hitsplat);

		// Act
		router.onHitsplatApplied(event);

		// Assert
		verifyNoInteractions(authoringLogger);
	}

	// ========================================================================
	// onMenuEntryAdded
	// ========================================================================

	@Test
	public void testOnMenuEntryAddedInjectsGuideOptionWhenSourceHasMissingItems()
	{
		// Arrange
		CollectionLogSource source = makeSource("Goblin Village");
		when(config.showOverlays()).thenReturn(true);
		when(npc.getId()).thenReturn(777);
		when(database.getSourceByNpcId(777)).thenReturn(source);
		when(guidanceCoordinator.isSourceGuided(source)).thenReturn(false);
		missingItems.add("Goblin Village");

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getNpc()).thenReturn(npc);
		when(menuEntry.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION);

		Menu menu = mock(Menu.class);
		MenuEntry newEntry = mock(MenuEntry.class);
		when(client.getMenu()).thenReturn(menu);
		when(menu.createMenuEntry(-1)).thenReturn(newEntry);
		when(newEntry.setOption(any())).thenReturn(newEntry);
		when(newEntry.setTarget(any())).thenReturn(newEntry);
		when(newEntry.setType(any())).thenReturn(newEntry);
		when(newEntry.setIdentifier(anyInt())).thenReturn(newEntry);

		MenuEntryAdded event = new MenuEntryAdded(menuEntry);

		// Act
		router.onMenuEntryAdded(event);

		// Assert
		verify(menu).createMenuEntry(-1);
		verify(newEntry).setOption(GuidanceEventRouter.MENU_OPTION_GUIDE);
	}

	@Test
	public void testOnMenuEntryAddedSkipsWhenOverlaysDisabled()
	{
		// Arrange
		when(config.showOverlays()).thenReturn(false);
		MenuEntry menuEntry = mock(MenuEntry.class);
		MenuEntryAdded event = new MenuEntryAdded(menuEntry);

		// Act
		router.onMenuEntryAdded(event);

		// Assert
		verify(client, never()).getMenu();
	}

	@Test
	public void testOnMenuEntryAddedSkipsWhenSourceAlreadyGuided()
	{
		// Arrange
		CollectionLogSource source = makeSource("Some Source");
		when(config.showOverlays()).thenReturn(true);
		when(npc.getId()).thenReturn(888);
		when(database.getSourceByNpcId(888)).thenReturn(source);
		when(guidanceCoordinator.isSourceGuided(source)).thenReturn(true);
		missingItems.add("Some Source");

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getNpc()).thenReturn(npc);
		when(menuEntry.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION);
		MenuEntryAdded event = new MenuEntryAdded(menuEntry);

		// Act
		router.onMenuEntryAdded(event);

		// Assert
		verify(client, never()).getMenu();
	}

	@Test
	public void testOnMenuEntryAddedSkipsWhenNoMissingItems()
	{
		// Arrange
		CollectionLogSource source = makeSource("Already Complete Source");
		when(config.showOverlays()).thenReturn(true);
		when(npc.getId()).thenReturn(999);
		when(database.getSourceByNpcId(999)).thenReturn(source);
		when(guidanceCoordinator.isSourceGuided(source)).thenReturn(false);
		// missingItems is empty — source not in it

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getNpc()).thenReturn(npc);
		when(menuEntry.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION);
		MenuEntryAdded event = new MenuEntryAdded(menuEntry);

		// Act
		router.onMenuEntryAdded(event);

		// Assert
		verify(client, never()).getMenu();
	}

	// ========================================================================
	// onMenuOptionClicked — guidance activation
	// ========================================================================

	@Test
	public void testOnMenuOptionClickedActivatesGuidanceForGuideOption()
	{
		// Arrange
		CollectionLogSource source = makeSource("Test Source");
		when(database.getSourceByNpcId(111)).thenReturn(source);
		Consumer<CollectionLogSource> activateCallback = mock(Consumer.class);
		router.setActivateGuidanceCallback(activateCallback);

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getType()).thenReturn(MenuAction.RUNELITE);
		when(menuEntry.getOption()).thenReturn(GuidanceEventRouter.MENU_OPTION_GUIDE);
		when(menuEntry.getIdentifier()).thenReturn(111);
		MenuOptionClicked event = new MenuOptionClicked(menuEntry);

		// Act
		router.onMenuOptionClicked(event);

		// Assert
		verify(activateCallback).accept(source);
	}

	@Test
	public void testOnMenuOptionClickedDoesNotActivateWhenSourceNotFound()
	{
		// Arrange
		when(database.getSourceByNpcId(anyInt())).thenReturn(null);
		Consumer<CollectionLogSource> activateCallback = mock(Consumer.class);
		router.setActivateGuidanceCallback(activateCallback);

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getType()).thenReturn(MenuAction.RUNELITE);
		when(menuEntry.getOption()).thenReturn(GuidanceEventRouter.MENU_OPTION_GUIDE);
		MenuOptionClicked event = new MenuOptionClicked(menuEntry);

		// Act
		router.onMenuOptionClicked(event);

		// Assert
		verify(activateCallback, never()).accept(any());
	}

	@Test
	public void testOnMenuOptionClickedForwardsCumulativeTrackAction()
	{
		// Arrange
		List<Integer> objectIds = Arrays.asList(1000, 1001);
		CollectionLogSource source = makeSourceWithTrack(500, objectIds);
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(guidanceSequencer.getActiveSource()).thenReturn(source);

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getType()).thenReturn(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT);
		when(menuEntry.getIdentifier()).thenReturn(1000); // matching object ID
		when(menuEntry.getParam0()).thenReturn(500);       // matching item ID
		MenuOptionClicked event = new MenuOptionClicked(menuEntry);

		// Act
		router.onMenuOptionClicked(event);

		// Assert
		verify(guidanceSequencer).onTrackedAction();
	}

	@Test
	public void testOnMenuOptionClickedForwardsNpcInteraction()
	{
		// Arrange
		when(guidanceSequencer.isActive()).thenReturn(true);
		when(npc.getId()).thenReturn(321);
		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getNpc()).thenReturn(npc);
		when(menuEntry.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION);
		MenuOptionClicked event = new MenuOptionClicked(menuEntry);

		// Act
		router.onMenuOptionClicked(event);

		// Assert
		verify(guidanceSequencer).onNpcInteracted(321);
	}

	// ========================================================================
	// Setter null-safety
	// ========================================================================

	@Test
	public void testMissingItemsSupplierNullSafety()
	{
		// Arrange — null supplier
		router.setMissingItemsSupplier(null);
		CollectionLogSource source = makeSource("Some Source");
		when(config.showOverlays()).thenReturn(true);
		when(npc.getId()).thenReturn(777);
		when(database.getSourceByNpcId(777)).thenReturn(source);
		when(guidanceCoordinator.isSourceGuided(source)).thenReturn(false);

		MenuEntry menuEntry = mock(MenuEntry.class);
		when(menuEntry.getNpc()).thenReturn(npc);
		when(menuEntry.getType()).thenReturn(MenuAction.NPC_FIRST_OPTION);
		MenuEntryAdded event = new MenuEntryAdded(menuEntry);

		// Act — should not throw
		router.onMenuEntryAdded(event);

		// Assert — no menu entry added when supplier is null
		verify(client, never()).getMenu();
	}
}
