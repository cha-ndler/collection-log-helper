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
import com.collectionloghelper.data.SourceRequirements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
@RunWith(MockitoJUnitRunner.class)
public class SourceRequirementsHeaderTest
{
	@Mock
	private Client client;

	private RequirementsChecker checker;
	private DropRateDatabase database;

	@Before
	public void setUp() throws Exception
	{
		Constructor<RequirementsChecker> ctor =
			RequirementsChecker.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		checker = ctor.newInstance(client);

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
		assertNotNull("Giant Mole must exist in drop_rates.json", source);
		List<RequirementRow> rows = checker.buildRequirementRows(source);
		assertTrue("Source with no requirements must produce no rows", rows.isEmpty());
	}

	@Test
	public void requirementsView_emptyRows_hidden()
	{
		RequirementsView view = new RequirementsView();
		view.updateRows(Collections.emptyList());
		assertFalse("RequirementsView must be hidden when there are no rows",
			view.isVisible());
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
			Collections.singletonList("DRAGON_SLAYER_II"), null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals("One quest row expected", 1, rows.size());
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.QUEST, row.getCategory());
		assertTrue("Completed quest must be met", row.isMet());
		assertEquals("COMPLETED", row.getStateText());
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue("Label must contain quest name fragment",
			row.getLabel().contains("Dragon Slayer"));
	}

	@Test
	public void buildRequirementRows_questNotStarted_redRow()
	{
		// intStack[0] = 1 → NOT_STARTED
		when(client.getIntStack()).thenReturn(new int[]{1});

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("DRAGON_SLAYER_II"), null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse("Not-started quest must be unmet", row.isMet());
		assertEquals("NOT STARTED", row.getStateText());
		assertEquals(RequirementRow.COLOR_UNMET, row.getColor());
	}

	@Test
	public void buildRequirementRows_questInProgress_yellowRow()
	{
		// intStack[0] = 0 (any value other than 1 or 2) → IN_PROGRESS
		when(client.getIntStack()).thenReturn(new int[]{0});

		SourceRequirements req = new SourceRequirements(
			Collections.singletonList("DRAGON_SLAYER_II"), null, null);
		CollectionLogSource source = makeSourceWithRequirements("Vorkath", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse("In-progress quest must not count as met", row.isMet());
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
			null, Collections.singletonList(new SkillRequirement("SLAYER", 91)), null);
		CollectionLogSource source = makeSourceWithRequirements("Cerberus", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.SKILL, row.getCategory());
		assertTrue("Level 95 satisfies Slayer 91", row.isMet());
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue("Label must contain skill name and required level",
			row.getLabel().contains("Slayer") && row.getLabel().contains("91"));
		assertTrue("State text must include current and required levels",
			row.getStateText().contains("95") && row.getStateText().contains("91"));
	}

	@Test
	public void buildRequirementRows_skillUnmet_redRow()
	{
		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(80);

		SourceRequirements req = new SourceRequirements(
			null, Collections.singletonList(new SkillRequirement("SLAYER", 91)), null);
		CollectionLogSource source = makeSourceWithRequirements("Cerberus", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse("Level 80 does not meet Slayer 91", row.isMet());
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
			null, null, Collections.singletonList("ARDOUGNE_ELITE"));
		CollectionLogSource source = makeSourceWithRequirements("Zulrah", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertEquals(RequirementRow.Category.DIARY, row.getCategory());
		assertTrue("Completed diary must be met", row.isMet());
		assertEquals("COMPLETED", row.getStateText());
		assertEquals(RequirementRow.COLOR_MET, row.getColor());
		assertTrue("Label must contain diary area name",
			row.getLabel().contains("Ardougne"));
	}

	@Test
	public void buildRequirementRows_diaryNotCompleted_redRow()
	{
		when(client.getVarbitValue(ArgumentMatchers.anyInt())).thenReturn(0);

		SourceRequirements req = new SourceRequirements(
			null, null, Collections.singletonList("ARDOUGNE_ELITE"));
		CollectionLogSource source = makeSourceWithRequirements("Zulrah", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(1, rows.size());
		RequirementRow row = rows.get(0);
		assertFalse("Incomplete diary must be unmet", row.isMet());
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
			null);
		CollectionLogSource source = makeSourceWithRequirements("General Graardor", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals("Quest + skill = 2 rows", 2, rows.size());
		for (RequirementRow row : rows)
		{
			assertTrue("All-met: row '" + row.getLabel() + "' must be met", row.isMet());
			assertEquals("All-met: row '" + row.getLabel() + "' must be green",
				RequirementRow.COLOR_MET, row.getColor());
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
			null);
		CollectionLogSource source = makeSourceWithRequirements("General Graardor", req);

		List<RequirementRow> rows = checker.buildRequirementRows(source);

		assertEquals(2, rows.size());
		// Quest row (index 0) — unmet
		RequirementRow questRow = rows.get(0);
		assertEquals(RequirementRow.Category.QUEST, questRow.getCategory());
		assertFalse("Quest not started — must be unmet", questRow.isMet());
		assertEquals(RequirementRow.COLOR_UNMET, questRow.getColor());
		// Skill row (index 1) — met
		RequirementRow skillRow = rows.get(1);
		assertEquals(RequirementRow.Category.SKILL, skillRow.getCategory());
		assertTrue("Strength 80 meets 70 — must be met", skillRow.isMet());
		assertEquals(RequirementRow.COLOR_MET, skillRow.getColor());
	}

	// =========================================================================
	// Integration: real sources from drop_rates.json
	// =========================================================================

	@Test
	public void integration_cerberus_hasSlayer91Requirement()
	{
		CollectionLogSource cerberus = findSource("Cerberus");
		assertNotNull("Cerberus must exist in drop_rates.json", cerberus);

		SourceRequirements req = cerberus.getRequirements();
		assertNotNull("Cerberus must have requirements", req);
		assertNotNull("Cerberus must have skill requirements", req.getSkills());
		assertFalse("Cerberus must have at least one skill requirement",
			req.getSkills().isEmpty());

		boolean hasSlayer91 = req.getSkills().stream()
			.anyMatch(s -> "SLAYER".equals(s.getSkill()) && s.getLevel() == 91);
		assertTrue("Cerberus must require Slayer 91", hasSlayer91);
	}

	@Test
	public void integration_cerberus_buildRequirementRows_slayerRowPresent()
	{
		CollectionLogSource cerberus = findSource("Cerberus");
		assertNotNull(cerberus);

		when(client.getRealSkillLevel(Skill.SLAYER)).thenReturn(91);

		List<RequirementRow> rows = checker.buildRequirementRows(cerberus);

		assertFalse("Cerberus rows must not be empty", rows.isEmpty());
		boolean hasSlayerRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.SKILL
				&& r.getLabel().contains("Slayer")
				&& r.getLabel().contains("91"));
		assertTrue("Cerberus must have a Slayer 91 row in the header", hasSlayerRow);
	}

	@Test
	public void integration_vorkath_hasDragonSlayerIIQuestRequirement()
	{
		CollectionLogSource vorkath = findSource("Vorkath");
		assertNotNull("Vorkath must exist in drop_rates.json", vorkath);

		SourceRequirements req = vorkath.getRequirements();
		assertNotNull("Vorkath must have requirements", req);
		assertNotNull("Vorkath must have quest requirements", req.getQuests());
		assertTrue("Vorkath must require Dragon Slayer II",
			req.getQuests().contains("DRAGON_SLAYER_II"));
	}

	@Test
	public void integration_vorkath_buildRequirementRows_questRowPresent()
	{
		CollectionLogSource vorkath = findSource("Vorkath");
		assertNotNull(vorkath);

		// intStack[0] = 2 → FINISHED
		when(client.getIntStack()).thenReturn(new int[]{2});

		List<RequirementRow> rows = checker.buildRequirementRows(vorkath);

		assertFalse("Vorkath rows must not be empty", rows.isEmpty());
		boolean hasDSIIRow = rows.stream()
			.anyMatch(r -> r.getCategory() == RequirementRow.Category.QUEST
				&& r.getLabel().contains("Dragon Slayer"));
		assertTrue("Vorkath must have a Dragon Slayer II quest row in the header",
			hasDSIIRow);
	}

	@Test
	public void integration_generalGraardor_hasQuestAndSkillRequirements()
	{
		CollectionLogSource graardor = findSource("General Graardor");
		assertNotNull("General Graardor must exist in drop_rates.json", graardor);

		SourceRequirements req = graardor.getRequirements();
		assertNotNull("General Graardor must have requirements", req);
		assertTrue("General Graardor must have quest requirements",
			req.getQuests() != null && !req.getQuests().isEmpty());
		assertTrue("General Graardor must have skill requirements",
			req.getSkills() != null && !req.getSkills().isEmpty());
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

		assertTrue("General Graardor header must include a QUEST row", hasQuestRow);
		assertTrue("General Graardor header must include a SKILL row", hasSkillRow);
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
		assertTrue("RequirementsView must be visible when rows are provided",
			view.isVisible());
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
		assertTrue("Diary row label must contain 'Ardougne Elite'", found);
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
			requirements,
			/* cumulativeTrackItemId */ 0,
			/* cumulativeTrackObjectIds */ null,
			/* cumulativeTrackThreshold */ 0,
			Collections.emptyList()
		);
	}
}
