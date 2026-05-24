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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.DropRateDatabase;
import com.collectionloghelper.data.RequirementRow;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.SkillRequirement;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.SourceRequirements;
import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PohTeleportInventory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * B.5.3 integration tests for the source-level requirements header.
 *
 * <p>Covers:
 * <ul>
 *   <li>Empty requirements — {@code buildRequirementRows} returns empty list (header hidden).</li>
 *   <li>All requirements met — all rows are green.</li>
 *   <li>Mixed met/unmet — correct colour per row.</li>
 *   <li>Quest requirement type — correct label and state text.</li>
 *   <li>Skill requirement type — correct label and level text.</li>
 *   <li>Diary requirement type (varbit-backed) — correct label and met/unmet text.</li>
 *   <li>Integration: Cerberus (Slayer 91) loads correctly from real drop_rates.json.</li>
 *   <li>Integration: Vorkath (Dragon Slayer II quest) loads correctly from real drop_rates.json.</li>
 *   <li>Integration: General Graardor (quest + skill) loads correctly from real drop_rates.json.</li>
 *   <li>RequirementsView renders empty list as hidden (no empty header space).</li>
 *   <li>RequirementsView renders non-empty list as visible with correct row labels.</li>
 * </ul>
 *
 * <p>Varbit-gated prerequisites are surfaced through the DIARY category —
 * {@link RequirementsChecker#buildRequirementRows} resolves each diary entry to its
 * {@link net.runelite.api.Varbits} constant and reads it via
 * {@link Client#getVarbitValue}. There is no separate VARBIT row category.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SourceRequirementsHeaderTest
{
	@Mock
	private Client client;

	@Mock
	private PohTeleportInventory pohTeleportInventory;

	@Mock
	private EquippedItemState equippedItemState;

	@Mock
	private PlayerInventoryState playerInventoryState;

	private RequirementsChecker checker;
	private DropRateDatabase database;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<RequirementsChecker> ctor =
			RequirementsChecker.class.getDeclaredConstructor(
				Client.class, PohTeleportInventory.class, EquippedItemState.class,
				PlayerInventoryState.class);
		ctor.setAccessible(true);
		checker = ctor.newInstance(client, pohTeleportInventory, equippedItemState, playerInventoryState);

		database = new DropRateDatabase();
		Gson gson = new GsonBuilder().create();
		Field gsonField = DropRateDatabase.class.getDeclaredField("gson");
		gsonField.setAccessible(true);
		gsonField.set(database, gson);
		database.load();
	}

	// =========================================================================
	// Empty requirements — header hidden
	// =========================================================================

	@Test
	public void buildRequirementRows_noRequirements_returnsEmpty()
	{
		CollectionLogSource source = findSource("Giant Mole");
		assertNotNull( source,"Giant Mole must exist in drop_rates.json");
		List<RequirementRow> rows = checker.buildRequirementRows(source);
		assertTrue( rows.isEmpty(),"Source with no requirements must produce no rows");
	}

	@Test
	public void requirementsView_emptyRows_hidden()
	{
		RequirementsView view = new RequirementsView();
		view.updateRows(Collections.emptyList());
		assertFalse(
			view.isVisible(),"RequirementsView must be hidden when there are no rows");
	}

	// =========================================================================
	// Quest requirement type
	// =========================================================================

	@Test
	public void buildRequirementRows_questFinished_greenRow()
	{
		// Quest.getState(client) runs a script then reads client.getIntStack()[0].
		// 2 = FINISHED, 1 = NOT_STARTED, other = IN_PROGRESS  (see Quest.java source).
		when(client.getIntStack()).thenReturn(new int[]{2});

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("DRAGON_SLAYER_II"), null, null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals( 1, rows.size(),"One quest row expected");
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.QUEST, row.getCategory());
		assertTrue( row.isMet(),"Completed quest must be met");
		assertEquals("COMPLETED", row.getStateText());
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue(
			row.getLabel().contains("Dragon Slayer"),"Label must contain quest name fragment");
	}

	@Test
	public void buildRequirementRows_questNotStarted_redRow()
	{
		// intStack[0] = 1 → NOT_STARTED
		when(client.getIntStack()).thenReturn(new int[]{1});

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("DRAGON_SLAYER_II"), null, null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse( row.isMet(),"Not-started quest must be unmet");
		assertEquals("NOT STARTED", row.getStateText());
		assertEquals(RequirementRow.COLOR_UNMET, row.getColor());
	}

	@Test
	public void buildRequirementRows_questInProgress_yellowRow()
	{
		// intStack[0] = 0 (any value other than 1 or 2) → IN_PROGRESS
		when(client.getIntStack()).thenReturn(new int[]{0});

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("DRAGON_SLAYER_II"), null, null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse( row.isMet(),"In-progress quest must not count as met");
		assertEquals("IN PROGRESS", row.getStateText());
		assertEquals(RequirementRow.COLOR_IN_PROGRESS, row.getColor());
	}

	// =========================================================================
	// Skill requirement type
	// =========================================================================

	@Test
	public void buildRequirementRows_skillMet_greenRow()
	{
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(95);

		SourceRequirements req = new SourceRequirements(
			null, Collections.singletonList(new SkillRequirement("SLAYER", 91)), null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Cerberus", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.SKILL, row.getCategory());
		assertTrue( row.isMet(),"Level 95 satisfies Slayer 91");
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue(
			row.getLabel().contains("Slayer") && row.getLabel().contains("91"),"Label must contain skill name and required level");
		assertTrue(
			row.getStateText().contains("95") && row.getStateText().contains("91"),"State text must include current and required levels");
	}

	@Test
	public void buildRequirementRows_skillUnmet_redRow()
	{
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(80);

		SourceRequirements req = new SourceRequirements(
			null, Collections.singletonList(new SkillRequirement("SLAYER", 91)), null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Cerberus", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse( row.isMet(),"Level 80 does not meet Slayer 91");
		assertEquals(RequirementRow.COLOR_UNMET, row.getColor());
	}

	// =========================================================================
	// Diary requirement type (varbit-backed — covers the varbit prerequisite case)
	// =========================================================================

	@Test
	public void buildRequirementRows_diaryCompleted_greenRow()
	{
		// Diary varbit resolved via Varbits reflection; mock getVarbitValue = 1 (completed)
		when(client.getVarbitValue(ArgumentMatchers.anyInt())).thenReturn(1);

		SourceRequirements req = new SourceRequirements(
			null, null, Collections.singletonList("ARDOUGNE_ELITE"), null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Zulrah", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.DIARY, row.getCategory());
		assertTrue( row.isMet(),"Completed diary must be met");
		assertEquals("COMPLETED", row.getStateText());
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue(
			row.getLabel().contains("Ardougne"),"Label must contain diary area name");
	}

	@Test
	public void buildRequirementRows_diaryNotCompleted_redRow()
	{
		when(client.getVarbitValue(ArgumentMatchers.anyInt())).thenReturn(0);

		SourceRequirements req = new SourceRequirements(
			null, null, Collections.singletonList("ARDOUGNE_ELITE"), null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("Zulrah", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse( row.isMet(),"Incomplete diary must be unmet");
		assertEquals("NOT COMPLETED", row.getStateText());
		assertEquals(RequirementRow.COLOR_UNMET, row.getColor());
	}

	// =========================================================================
	// All requirements met — every row green
	// =========================================================================

	@Test
	public void buildRequirementRows_allMet_allGreen()
	{
		// intStack[0] = 2 → FINISHED for the TROLL_STRONGHOLD quest
		when(client.getIntStack()).thenReturn(new int[]{2});
		when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(80);

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("TROLL_STRONGHOLD"),
			Collections.singletonList(new SkillRequirement("STRENGTH", 70)),
			null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("General Graardor", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals( 2, rows.size(),"Quest + skill = 2 rows");
		for (RequirementRow row : rows)
		{
			assertTrue( row.isMet(),"All-met: row '" + row.getLabel() + "' must be met");
			assertEquals(
				RequirementRow.COLOR_MET, row.getColor(),"All-met: row '" + row.getLabel() + "' must be green");
		}
	}

	// =========================================================================
	// Mixed met / unmet — correct colour per row
	// =========================================================================

	@Test
	public void buildRequirementRows_mixed_correctColoursPerRow()
	{
		// intStack[0] = 1 → NOT_STARTED for the TROLL_STRONGHOLD quest
		when(client.getIntStack()).thenReturn(new int[]{1});
		when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(80);

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("TROLL_STRONGHOLD"),
			Collections.singletonList(new SkillRequirement("STRENGTH", 70)),
			null, null, null, null, null);
		CollectionLogSource source = makeSourceWithRequirements("General Graardor", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(2, rows.size());
		// Quest row (index 0) — unmet
		RequirementRow questRow = rows.get(0);
		assertEquals(RequirementRow.Category.QUEST, questRow.getCategory());
		assertFalse( questRow.isMet(),"Quest not started — must be unmet");
		assertEquals(RequirementRow.COLOR_UNMET, questRow.getColor());
		// Skill row (index 1) — met
		RequirementRow skillRow = rows.get(1);
		assertEquals(RequirementRow.Category.SKILL, skillRow.getCategory());
		assertTrue( skillRow.isMet(),"Strength 80 meets 70 — must be met");
		assertEquals(RequirementRow.COLOR_MET, skillRow.getColor());
	}

	// =========================================================================
	// Integration: real sources from drop_rates.json
	// =========================================================================

	@Test
	public void integration_cerberus_hasSlayer91Requirement()
	{
		CollectionLogSource cerberus = findSource("Cerberus");
		assertNotNull( cerberus,"Cerberus must exist in drop_rates.json");

		SourceRequirements req = cerberus.getRequirements();
		assertNotNull( req,"Cerberus must have requirements");
		assertNotNull( req.getSkills(),"Cerberus must have skill requirements");
		assertFalse(
			req.getSkills().isEmpty(),"Cerberus must have at least one skill requirement");

		boolean hasSlayer91 = req.getSkills().stream()
			.anyMatch(s -> "SLAYER".equals(s.getSkill()) && s.getLevel() == 91);
		assertTrue( hasSlayer91,"Cerberus must require Slayer 91");
	}

	@Test
	public void integration_cerberus_buildRequirementRows_slayerRowPresent()
	{
		CollectionLogSource cerberus = findSource("Cerberus");
		assertNotNull(cerberus);

		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(91);

		List<RequirementRow> rows = checker.buildRequirementRows(cerberus);

		assertFalse( rows.isEmpty(),"Cerberus rows must not be empty");
		boolean hasSlayerRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.SKILL
				&& r.getLabel().contains("Slayer")
				&& r.getLabel().contains("91"));
		assertTrue( hasSlayerRow,"Cerberus must have a Slayer 91 row in the header");
	}

	@Test
	public void integration_vorkath_hasDragonSlayerIIQuestRequirement()
	{
		CollectionLogSource vorkath = findSource("Vorkath");
		assertNotNull( vorkath,"Vorkath must exist in drop_rates.json");

		SourceRequirements req = vorkath.getRequirements();
		assertNotNull( req,"Vorkath must have requirements");
		assertNotNull( req.getQuests(),"Vorkath must have quest requirements");
		assertTrue(
			req.getQuests().contains("DRAGON_SLAYER_II"),"Vorkath must require Dragon Slayer II");
	}

	@Test
	public void integration_vorkath_buildRequirementRows_questRowPresent()
	{
		CollectionLogSource vorkath = findSource("Vorkath");
		assertNotNull(vorkath);

		// intStack[0] = 2 → FINISHED
		when(client.getIntStack()).thenReturn(new int[]{2});

		List<RequirementRow> rows = checker.buildRequirementRows(vorkath);

		assertFalse( rows.isEmpty(),"Vorkath rows must not be empty");
		boolean hasDSIIRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.QUEST
				&& r.getLabel().contains("Dragon Slayer"));
		assertTrue(
			hasDSIIRow,"Vorkath must have a Dragon Slayer II quest row in the header");
	}

	@Test
	public void integration_generalGraardor_hasQuestAndSkillRequirements()
	{
		CollectionLogSource graardor = findSource("General Graardor");
		assertNotNull( graardor,"General Graardor must exist in drop_rates.json");

		SourceRequirements req = graardor.getRequirements();
		assertNotNull( req,"General Graardor must have requirements");
		assertTrue(
			req.getQuests() != null && !req.getQuests().isEmpty(),"General Graardor must have quest requirements");
		assertTrue(
			req.getSkills() != null && !req.getSkills().isEmpty(),"General Graardor must have skill requirements");
	}

	@Test
	public void integration_generalGraardor_buildRequirementRows_bothCategoriesPresent()
	{
		CollectionLogSource graardor = findSource("General Graardor");
		assertNotNull(graardor);

		// intStack[0] = 2 → FINISHED for TROLL_STRONGHOLD quest
		when(client.getIntStack()).thenReturn(new int[]{2});
		when(client.getRealSkillLevel(Skill.STRENGTH)).thenReturn(70);

		List<RequirementRow> rows = checker.buildRequirementRows(graardor);

		boolean hasQuestRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.QUEST);
		boolean hasSkillRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.SKILL);

		assertTrue( hasQuestRow,"General Graardor header must include a QUEST row");
		assertTrue( hasSkillRow,"General Graardor header must include a SKILL row");
	}

	// =========================================================================
	// RequirementsView — render integration
	// =========================================================================

	@Test
	public void requirementsView_nonEmptyRows_visible()
	{
		RequirementRow row = new RequirementRow(RequirementRow.Category.QUEST,
			"Dragon Slayer II", "COMPLETED", RequirementRow.COLOR_MET, true);
		RequirementsView view = new RequirementsView();
		view.updateRows(Collections.singletonList(row));
		assertTrue(
			view.isVisible(),"RequirementsView must be visible when rows are provided");
	}

	@Test
	public void requirementsView_diaryRow_labelContainsAreaName()
	{
		RequirementRow row = new RequirementRow(RequirementRow.Category.DIARY,
			"Ardougne Elite", "COMPLETED", RequirementRow.COLOR_MET, true);
		RequirementsView view = new RequirementsView();
		view.updateRows(Collections.singletonList(row));
		assertTrue(view.isVisible());

		boolean found = false;
		for (int i = 0; i < view.getComponentCount(); i++)
		{
			java.awt.Component c = view.getComponent(i);
			if (c instanceof javax.swing.JLabel)
			{
				String text = ((javax.swing.JLabel) c).getText();
				if (text != null && text.contains("Ardougne Elite"))
				{
					found = true;
					break;
				}
			}
		}
		assertTrue( found,"Diary row label must contain 'Ardougne Elite'");
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	private CollectionLogSource findSource(String name)
	{
		return database.getAllSources().stream()
			.filter(s -> name.equals(s.getName()))
			.findFirst()
			.orElse(null);
	}

	private static CollectionLogSource makeSourceWithRequirements(
		String name, SourceRequirements requirements)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			/* worldX */ 0,
			/* worldY */ 0,
			/* worldPlane */ 0,
			/* killTimeSeconds */ 60,
			/* ironKillTimeSeconds */ 0,
			/* locationDescription */ null,
			/* waypoints */ null,
			/* rewardType */ null,
			/* pointsPerHour */ 0,
			/* mutuallyExclusiveSources */ null,
			/* rollsPerKill */ 0,
			/* aggregated */ false,
			/* afkLevel */ 0,
			/* travelTip */ null,
			/* npcId */ 0,
			/* interactAction */ null,
			/* dialogOptions */ null,
			/* guidanceSteps */ null,
			/* guidanceHelperKey */ null,
			requirements,
			/* cumulativeTrackItemId */ 0,
			/* cumulativeTrackObjectIds */ null,
			/* cumulativeTrackThreshold */ 0,
			Collections.emptyList(), null /* metaAuthoredDate */, null, null);
	}
}
