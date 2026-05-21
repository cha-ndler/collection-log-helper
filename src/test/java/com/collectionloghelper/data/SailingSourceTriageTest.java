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
package com.collectionloghelper.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Triage tests for Sailing-related sources flagged in issue #314.
 *
 * <p>Classifies each candidate source as:
 * <ul>
 *   <li><b>RESOLVED</b> — port-based activity; coords point to Port Piscarilius sailing dock
 *       (1824, 3691) which is confirmed correct via cache. No fix needed.</li>
 *   <li><b>NEEDS-CAPTURE</b> — source is on a Sailing island instance whose runtime
 *       coordinates are not in the static cache and cannot be Wiki-verified. Requires an
 *       in-game authoring-log capture session before coords can be patched.</li>
 * </ul>
 *
 * <p>Port Piscarilius sailing dock canonical coordinates: worldX=1824, worldY=3691.
 * Verified via cache objects: Veos (NPC 2147), Commander Fullore (NPC 11119),
 * Cabin Boy Herbert (NPC 11125) all confirmed at this tile.
 */
public class SailingSourceTriageTest
{
	/** Port Piscarilius sailing dock — confirmed by NPC cache (Veos at 1825,3691). */
	private static final int PORT_PISCARILIUS_X = 1824;
	private static final int PORT_PISCARILIUS_Y = 3691;

	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		database = new DropRateDatabase();
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);
		database.load();
	}

	// ========================================================================
	// RESOLVED — port-based sources whose ARRIVE_AT_TILE coords are correct
	// ========================================================================

	/**
	 * Cutting Squid: squid are processed at the Sailing port. ARRIVE_AT_TILE step
	 * targets the Port Piscarilius dock which is confirmed correct.
	 * Wiki: https://oldschool.runescape.wiki/w/Cutting_squid
	 */
	@Test
	public void cuttingSquid_resolvedPortCoords_matchPortPiscarilius()
	{
		CollectionLogSource source = database.getSourceByName("Cutting Squid");
		assertNotNull( source,"Cutting Squid source must exist in drop_rates.json");

		assertEquals(
			PORT_PISCARILIUS_X, source.getWorldX(),"Cutting Squid worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, source.getWorldY(),"Cutting Squid worldY must be Port Piscarilius dock");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull( steps,"Guidance steps must not be null");
		assertFalse( steps.isEmpty(),"Guidance steps must not be empty");

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			PORT_PISCARILIUS_X, arriveStep.getWorldX(),"ARRIVE_AT_TILE step worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, arriveStep.getWorldY(),"ARRIVE_AT_TILE step worldY must be Port Piscarilius dock");
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"First step must be ARRIVE_AT_TILE");
	}

	/**
	 * Sea Treasures: treasure voyages launched from a Sailing port. ARRIVE_AT_TILE
	 * step targets the Port Piscarilius dock which is confirmed correct.
	 * Wiki: https://oldschool.runescape.wiki/w/Sea_treasures (redirects to Collection log)
	 */
	@Test
	public void seaTreasures_resolvedPortCoords_matchPortPiscarilius()
	{
		CollectionLogSource source = database.getSourceByName("Sea Treasures");
		assertNotNull( source,"Sea Treasures source must exist in drop_rates.json");

		assertEquals(
			PORT_PISCARILIUS_X, source.getWorldX(),"Sea Treasures worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, source.getWorldY(),"Sea Treasures worldY must be Port Piscarilius dock");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull(steps);
		assertFalse(steps.isEmpty());

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			PORT_PISCARILIUS_X, arriveStep.getWorldX(),"ARRIVE_AT_TILE step worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, arriveStep.getWorldY(),"ARRIVE_AT_TILE step worldY must be Port Piscarilius dock");
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"First step must be ARRIVE_AT_TILE");
	}

	/**
	 * Port Tasks: notice-board tasks accepted at Sailing ports. ARRIVE_AT_TILE
	 * step targets the Port Piscarilius dock which is confirmed correct.
	 * Wiki: https://oldschool.runescape.wiki/w/Port_tasks (redirects to Sailing#Port tasks)
	 */
	@Test
	public void portTasks_resolvedPortCoords_matchPortPiscarilius()
	{
		CollectionLogSource source = database.getSourceByName("Port Tasks");
		assertNotNull( source,"Port Tasks source must exist in drop_rates.json");

		assertEquals(
			PORT_PISCARILIUS_X, source.getWorldX(),"Port Tasks worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, source.getWorldY(),"Port Tasks worldY must be Port Piscarilius dock");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull(steps);
		assertFalse(steps.isEmpty());

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			PORT_PISCARILIUS_X, arriveStep.getWorldX(),"ARRIVE_AT_TILE step worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, arriveStep.getWorldY(),"ARRIVE_AT_TILE step worldY must be Port Piscarilius dock");
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"First step must be ARRIVE_AT_TILE");
	}

	/**
	 * Lost Schematics: lockboxes on various Sailing islands, but guidance is
	 * structured as arrive-at-port + MANUAL (player sails out manually). The
	 * ARRIVE_AT_TILE step targeting Port Piscarilius is the correct anchor.
	 * Wiki: https://oldschool.runescape.wiki/w/Lost_schematics
	 */
	@Test
	public void lostSchematics_resolvedPortCoords_matchPortPiscarilius()
	{
		CollectionLogSource source = database.getSourceByName("Lost Schematics");
		assertNotNull( source,"Lost Schematics source must exist in drop_rates.json");

		assertEquals(
			PORT_PISCARILIUS_X, source.getWorldX(),"Lost Schematics worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, source.getWorldY(),"Lost Schematics worldY must be Port Piscarilius dock");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull(steps);
		assertFalse(steps.isEmpty());

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			PORT_PISCARILIUS_X, arriveStep.getWorldX(),"ARRIVE_AT_TILE step worldX must be Port Piscarilius dock");
		assertEquals(
			PORT_PISCARILIUS_Y, arriveStep.getWorldY(),"ARRIVE_AT_TILE step worldY must be Port Piscarilius dock");
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"First step must be ARRIVE_AT_TILE");
	}

	// ========================================================================
	// NEEDS-CAPTURE — Sailing island instances requiring in-game authoring log
	// ========================================================================

	/**
	 * Gryphon (Slayer): found only in eastern/western caves on Great Conch island
	 * (Sailing, fairy ring CJQ). Great Conch is an instanced region — no NPC spawns
	 * in the static cache. Current coords (1456, 2880) are ~101 tiles from Aldarin
	 * and do not correspond to any Great Conch cave. The Shellbane Gryphon fix
	 * (e4dd6a06) showed Great Conch uses runtime coords ~13993, 9901.
	 *
	 * <p>This test pins the source's existence and that its ARRIVE_AT_TILE step
	 * has a non-zero coord. It will need updating once in-game capture provides
	 * the correct island instance coordinates.
	 *
	 * Wiki: https://oldschool.runescape.wiki/w/Gryphon
	 */
	@Test
	public void gryphon_needsCapture_sourceExistsWithArriveAtTileStep()
	{
		CollectionLogSource source = database.getSourceByName("Gryphon");
		assertNotNull( source,"Gryphon source must exist in drop_rates.json");
		assertEquals( CollectionLogCategory.SLAYER, source.getCategory(),"Gryphon must be SLAYER category");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull( steps,"Gryphon guidance steps must not be null");
		assertFalse( steps.isEmpty(),"Gryphon guidance steps must not be empty");

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"Gryphon first step must be ARRIVE_AT_TILE");

		// Coords are known-wrong (placeholder); assert they exist and are non-zero.
		// TODO(#314): replace with authoring-log captured Great Conch instance coords.
		assertTrue(
			arriveStep.getWorldX() != 0,"Gryphon ARRIVE_AT_TILE worldX must be non-zero (placeholder pending capture)");
		assertTrue(
			arriveStep.getWorldY() != 0,"Gryphon ARRIVE_AT_TILE worldY must be non-zero (placeholder pending capture)");
	}

	/**
	 * Aquanite: found in Ynysdail Cavern beneath Ynysdail (73 Sailing). Ynysdail is
	 * a static overworld island north of Gwenith, reachable by rowboat or Sailing ship.
	 * The underground cave coords (2275, 9880) = 3480 + 6400 are geographically plausible,
	 * but the surface entrance step at (2218, 3477) cannot be verified against the static
	 * NPC cache.
	 *
	 * <p>As of #555, the travel step uses ARRIVE_AT_ZONE covering both the surface
	 * entrance band and the underground cavern interior on plane 0, so the
	 * auto-advance no longer depends on satisfying the surface radius first.
	 * In-game capture is still desirable to confirm the surface entrance coord, but
	 * the predicate shape is now resilient either way.
	 *
	 * Wiki: https://oldschool.runescape.wiki/w/Aquanite
	 */
	@Test
	public void aquanite_needsCapture_sourceExistsWithExpectedPlaceholderCoords()
	{
		CollectionLogSource source = database.getSourceByName("Aquanite");
		assertNotNull( source,"Aquanite source must exist in drop_rates.json");
		assertEquals( CollectionLogCategory.SLAYER, source.getCategory(),"Aquanite must be SLAYER category");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull( steps,"Aquanite guidance steps must not be null");
		assertEquals( 2, steps.size(),"Aquanite must have exactly 2 guidance steps");

		// Step 0: ARRIVE_AT_ZONE covering surface entrance + underground cavern (#555).
		GuidanceStep entranceStep = steps.get(0);
		assertEquals(
			CompletionCondition.ARRIVE_AT_ZONE, entranceStep.getCompletionCondition(),"Aquanite step 0 must be ARRIVE_AT_ZONE after #555 batch migration");
		// Entrance near Tirannwn/Gwenith coast (unverified placeholder).
		// TODO(#314): verify via in-game authoring log that this fires on Ynysdail surface.
		assertEquals( 2218, entranceStep.getWorldX(),"Aquanite entrance step worldX placeholder");
		assertEquals( 3477, entranceStep.getWorldY(),"Aquanite entrance step worldY placeholder");

		// Step 1: ACTOR_DEATH inside the cave
		GuidanceStep killStep = steps.get(1);
		assertEquals(
			CompletionCondition.ACTOR_DEATH, killStep.getCompletionCondition(),"Aquanite kill step must be ACTOR_DEATH");
		// Underground coords: 2275, 9880 = surface ~3480 + 6400 (Ynysdail Cavern underground band).
		// TODO(#314): verify via in-game authoring log.
		assertEquals( 2275, killStep.getWorldX(),"Aquanite cave step worldX placeholder");
		assertEquals( 9880, killStep.getWorldY(),"Aquanite cave step worldY placeholder");
	}

	/**
	 * Lava Strykewyrm: found in the Charred Dungeon beneath Charred Island (60 Sailing
	 * required). There is no Wilderness variant — the Wiki documents only the Charred
	 * Island location. Current coords (2150, 2968) and guidance text ("Wilderness Lava
	 * Strykewyrm area") are copy-pasted from a pre-Sailing entry and are incorrect.
	 * Charred Island is a Sailing island instance with no static cache spawns.
	 *
	 * <p>This test pins the source's existence and documents that its current coords
	 * are known-wrong placeholders pending in-game capture.
	 *
	 * Wiki: https://oldschool.runescape.wiki/w/Lava_Strykewyrm
	 */
	@Test
	public void lavaStrykewyrm_needsCapture_sourceExistsWithArriveAtTileStep()
	{
		CollectionLogSource source = database.getSourceByName("Lava Strykewyrm");
		assertNotNull( source,"Lava Strykewyrm source must exist in drop_rates.json");
		assertEquals(
			CollectionLogCategory.SLAYER, source.getCategory(),"Lava Strykewyrm must be SLAYER category");

		List<GuidanceStep> steps = source.getGuidanceSteps();
		assertNotNull( steps,"Lava Strykewyrm guidance steps must not be null");
		assertFalse( steps.isEmpty(),"Lava Strykewyrm guidance steps must not be empty");

		GuidanceStep arriveStep = steps.get(0);
		assertEquals(
			CompletionCondition.ARRIVE_AT_TILE, arriveStep.getCompletionCondition(),"Lava Strykewyrm first step must be ARRIVE_AT_TILE");

		// Coords are known-wrong (Isafdar placeholder); assert non-zero existence.
		// TODO(#314): replace with authoring-log captured Charred Island instance coords.
		assertTrue(
			arriveStep.getWorldX() != 0,"Lava Strykewyrm ARRIVE_AT_TILE worldX must be non-zero (placeholder pending capture)");
		assertTrue(
			arriveStep.getWorldY() != 0,"Lava Strykewyrm ARRIVE_AT_TILE worldY must be non-zero (placeholder pending capture)");
	}
}
