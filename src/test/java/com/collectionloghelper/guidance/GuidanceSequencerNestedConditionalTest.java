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
package com.collectionloghelper.guidance;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.data.CompletionCondition;
import com.collectionloghelper.data.ConditionalAlternative;
import com.collectionloghelper.data.GuidanceStep;
import com.collectionloghelper.data.PlayerCollectionState;
import com.collectionloghelper.data.PlayerInventoryState;
import com.collectionloghelper.data.RequirementsChecker;
import com.collectionloghelper.data.RewardType;
import com.collectionloghelper.data.SkillRequirement;
import com.collectionloghelper.data.SourceRequirements;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * B3 — evaluator state-machine tests for nested conditional steps.
 *
 * <p>Tests cover:
 * <ol>
 *   <li>Flat conditional — unchanged legacy behaviour.</li>
 *   <li>Nested: matching parent + matching child — both overrides applied.</li>
 *   <li>Nested: matching parent + non-matching child — parent override only.</li>
 *   <li>Nested: no parent match — all skipped, default step used.</li>
 *   <li>3-level nesting — full recursive proof.</li>
 *   <li>First matching nested alternative wins (order is preserved).</li>
 *   <li>Nested alternatives are evaluated against the already-overridden step.</li>
 * </ol>
 */
@RunWith(MockitoJUnitRunner.class)
public class GuidanceSequencerNestedConditionalTest
{
	@Mock
	private PlayerInventoryState inventoryState;
	@Mock
	private PlayerCollectionState collectionState;
	@Mock
	private RequirementsChecker requirementsChecker;

	private GuidanceSequencer sequencer;

	@Before
	public void setUp() throws Exception
	{
		lenient().when(inventoryState.hasItem(anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasItemCount(anyInt(), anyInt())).thenReturn(false);
		lenient().when(inventoryState.hasAllItems(org.mockito.ArgumentMatchers.anyList())).thenReturn(true);
		lenient().when(collectionState.isItemObtained(anyInt())).thenReturn(false);

		Constructor<GuidanceSequencer> ctor = GuidanceSequencer.class.getDeclaredConstructor(
			PlayerInventoryState.class, PlayerCollectionState.class, RequirementsChecker.class);
		ctor.setAccessible(true);
		sequencer = ctor.newInstance(inventoryState, collectionState, requirementsChecker);
	}

	// ── 1. Flat conditional — identical to legacy ─────────────────────────────

	@Test
	public void flatConditional_matchingRequirements_appliesOverride()
	{
		SourceRequirements reqs = makeQuestReq("LOST_CITY");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative flat = makeAlt("Fairy ring route", 2400, 4435, reqs, null);
		GuidanceStep base = makeStepWithAlts("Walk there", 3000, 3000, Arrays.asList(flat));

		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Fairy ring route", current.getDescription());
		assertEquals(2400, current.getWorldX());
		assertEquals(4435, current.getWorldY());
	}

	@Test
	public void flatConditional_nonMatchingRequirements_usesBaseStep()
	{
		SourceRequirements reqs = makeQuestReq("SONG_OF_THE_ELVES");
		when(requirementsChecker.meetsRequirements(reqs)).thenReturn(false);

		ConditionalAlternative flat = makeAlt("Prifddinas teleport", 3200, 6100, reqs, null);
		GuidanceStep base = makeStepWithAlts("Walk from Ardougne", 2600, 3300, Arrays.asList(flat));

		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Walk from Ardougne", current.getDescription());
		assertEquals(2600, current.getWorldX());
	}

	// ── 2. Nested: matching parent + matching child → both applied ────────────

	@Test
	public void nested_parentMatchesChildMatches_bothOverridesApplied()
	{
		SourceRequirements parentReqs = makeQuestReq("LOST_CITY");
		SourceRequirements childReqs = makeSkillReq("AGILITY", 70);
		when(requirementsChecker.meetsRequirements(parentReqs)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(childReqs)).thenReturn(true);

		// Child refines description and worldX beyond parent's values
		ConditionalAlternative child = makeAlt(
			"Agility shortcut via fairy ring", 2410, null, childReqs, null);
		// Parent sets description, worldX=2400, worldY=4435
		ConditionalAlternative parent = makeAlt(
			"Fairy ring BIP", 2400, 4435, parentReqs, Collections.singletonList(child));

		GuidanceStep base = makeStepWithAlts("Walk there", 3000, 3000, Arrays.asList(parent));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		// Child description wins
		assertEquals("Agility shortcut via fairy ring", current.getDescription());
		// Child worldX (2410) wins over parent's 2400
		assertEquals(2410, current.getWorldX());
		// Parent worldY stays (child doesn't override it)
		assertEquals(4435, current.getWorldY());
	}

	// ── 3. Nested: matching parent + non-matching child → parent only ─────────

	@Test
	public void nested_parentMatchesChildNotMatches_parentOverrideOnly()
	{
		SourceRequirements parentReqs = makeQuestReq("LOST_CITY");
		SourceRequirements childReqs = makeSkillReq("AGILITY", 99);
		when(requirementsChecker.meetsRequirements(parentReqs)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(childReqs)).thenReturn(false);

		ConditionalAlternative child = makeAlt("Max agility shortcut", 2420, null, childReqs, null);
		ConditionalAlternative parent = makeAlt(
			"Fairy ring BIP", 2400, 4435, parentReqs, Collections.singletonList(child));

		GuidanceStep base = makeStepWithAlts("Walk there", 3000, 3000, Arrays.asList(parent));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		// Parent override only — child skipped
		assertEquals("Fairy ring BIP", current.getDescription());
		assertEquals(2400, current.getWorldX());
		assertEquals(4435, current.getWorldY());
	}

	// ── 4. No parent match → all skipped, default step ───────────────────────

	@Test
	public void nested_noParentMatch_usesBaseStep()
	{
		SourceRequirements parentReqs = makeQuestReq("SONG_OF_THE_ELVES");
		SourceRequirements childReqs = makeSkillReq("AGILITY", 70);
		when(requirementsChecker.meetsRequirements(parentReqs)).thenReturn(false);
		// childReqs is never evaluated because parent doesn't match — lenient to avoid strict-stub failure
		lenient().when(requirementsChecker.meetsRequirements(childReqs)).thenReturn(true);

		ConditionalAlternative child = makeAlt(
			"Would apply if parent matched", 9999, null, childReqs, null);
		ConditionalAlternative parent = makeAlt(
			"Elf city route", 3200, 6100, parentReqs, Collections.singletonList(child));

		GuidanceStep base = makeStepWithAlts("Long walk via Ardougne", 2600, 3300,
			Arrays.asList(parent));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("Long walk via Ardougne", current.getDescription());
		assertEquals(2600, current.getWorldX());
		assertEquals(3300, current.getWorldY());
	}

	// ── 5. 3-level nesting — full recursive proof ─────────────────────────────

	@Test
	public void threeLevel_allMatch_allOverridesAppliedDepthFirst()
	{
		SourceRequirements reqL1 = makeQuestReq("FAIRYTALE_II__CURE_A_QUEEN");
		SourceRequirements reqL2 = makeSkillReq("AGILITY", 60);
		SourceRequirements reqL3 = makeSkillReq("AGILITY", 80);
		when(requirementsChecker.meetsRequirements(reqL1)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(reqL2)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(reqL3)).thenReturn(true);

		// Level 3: override travelTip only
		ConditionalAlternative level3 = new ConditionalAlternative(
			reqL3,
			null, null, null, null,
			"80 Agility shortcut saves 2 tiles",
			null, null, null, null, null, null,
			null
		);

		// Level 2: override description and worldX; level 3 refines travelTip
		ConditionalAlternative level2 = new ConditionalAlternative(
			reqL2,
			"Use agility shortcut near fairy ring",
			2405, null, null,
			"60 Agility shortcut",
			null, null, null, null, null, null,
			Collections.singletonList(level3)
		);

		// Level 1: override description, worldX, worldY
		ConditionalAlternative level1 = new ConditionalAlternative(
			reqL1,
			"Fairy ring BIP",
			2400, 4435, null,
			"Use fairy ring",
			null, null, null, null, null, null,
			Collections.singletonList(level2)
		);

		GuidanceStep base = makeStepWithAlts("Walk there manually", 3000, 3000,
			Arrays.asList(level1));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		// Level 2 overrides description (level 3 does not override description)
		assertEquals("Use agility shortcut near fairy ring", current.getDescription());
		// Level 1 set worldX=2400; level 2 refines to 2405 (level 3 doesn't touch worldX)
		assertEquals(2405, current.getWorldX());
		// Level 1 set worldY=4435; level 2/3 don't override it
		assertEquals(4435, current.getWorldY());
		// Level 3 overrides travelTip
		assertEquals("80 Agility shortcut saves 2 tiles", current.getTravelTip());
	}

	@Test
	public void threeLevel_level3NotMatch_stopsAtLevel2()
	{
		SourceRequirements reqL1 = makeQuestReq("FAIRYTALE_II__CURE_A_QUEEN");
		SourceRequirements reqL2 = makeSkillReq("AGILITY", 60);
		SourceRequirements reqL3 = makeSkillReq("AGILITY", 80);
		when(requirementsChecker.meetsRequirements(reqL1)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(reqL2)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(reqL3)).thenReturn(false);

		ConditionalAlternative level3 = new ConditionalAlternative(
			reqL3, "Would not be reached", null, null, null, "Max shortcut",
			null, null, null, null, null, null, null
		);
		ConditionalAlternative level2 = new ConditionalAlternative(
			reqL2, "60 Agility shortcut", 2405, 4430, null, "60 Agility shortcut",
			null, null, null, null, null, null,
			Collections.singletonList(level3)
		);
		ConditionalAlternative level1 = new ConditionalAlternative(
			reqL1, "Fairy ring BIP", 2400, 4435, null, "Use fairy ring",
			null, null, null, null, null, null,
			Collections.singletonList(level2)
		);

		GuidanceStep base = makeStepWithAlts("Walk there manually", 3000, 3000,
			Arrays.asList(level1));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		assertEquals("60 Agility shortcut", current.getDescription());
		assertEquals(2405, current.getWorldX());
		assertEquals(4430, current.getWorldY());
		// Level 3 not reached — travelTip from level 2
		assertEquals("60 Agility shortcut", current.getTravelTip());
	}

	// ── 6. First matching nested alternative wins ─────────────────────────────

	@Test
	public void nested_multipleChildren_firstMatchingChildWins()
	{
		SourceRequirements parentReqs = makeQuestReq("LOST_CITY");
		SourceRequirements childReqs1 = makeSkillReq("AGILITY", 99);
		SourceRequirements childReqs2 = makeSkillReq("AGILITY", 70);
		SourceRequirements childReqs3 = makeSkillReq("AGILITY", 60);
		when(requirementsChecker.meetsRequirements(parentReqs)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(childReqs1)).thenReturn(false);
		when(requirementsChecker.meetsRequirements(childReqs2)).thenReturn(true);
		// childReqs3 is never evaluated because child2 matches first — lenient to avoid strict-stub failure
		lenient().when(requirementsChecker.meetsRequirements(childReqs3)).thenReturn(true);

		ConditionalAlternative child1 = makeAlt("99 Agility route", null, null, childReqs1, null);
		ConditionalAlternative child2 = makeAlt("70 Agility route", null, null, childReqs2, null);
		ConditionalAlternative child3 = makeAlt("60 Agility route", null, null, childReqs3, null);

		ConditionalAlternative parent = makeAlt("Fairy ring", 2400, 4435, parentReqs,
			Arrays.asList(child1, child2, child3));

		GuidanceStep base = makeStepWithAlts("Walk", 3000, 3000, Arrays.asList(parent));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		// child1 fails; child2 is first match and wins (not child3)
		assertEquals("70 Agility route", current.getDescription());
	}

	// ── 7. Nested alternatives evaluated against already-overridden step ───────

	@Test
	public void nested_childSeesParentOverriddenValues()
	{
		SourceRequirements parentReqs = makeQuestReq("LOST_CITY");
		SourceRequirements childReqs = makeSkillReq("AGILITY", 70);
		when(requirementsChecker.meetsRequirements(parentReqs)).thenReturn(true);
		when(requirementsChecker.meetsRequirements(childReqs)).thenReturn(true);

		// Child only overrides travelTip — all other fields should retain parent's overrides
		ConditionalAlternative child = new ConditionalAlternative(
			childReqs,
			null, null, null, null,  // description + coords: fall through from parent merge
			"Agility shortcut available",
			null, null, null, null, null, null,
			null
		);
		ConditionalAlternative parent = makeAlt("Fairy ring BIP", 2400, 4435, parentReqs,
			Collections.singletonList(child));

		GuidanceStep base = makeStepWithAlts("Walk there", 3000, 3000, Arrays.asList(parent));
		startSequence(Arrays.asList(base, makeManualStep("Done")));

		GuidanceStep current = sequencer.getCurrentStep();
		// Parent description and coords survive (child didn't override them)
		assertEquals("Fairy ring BIP", current.getDescription());
		assertEquals(2400, current.getWorldX());
		assertEquals(4435, current.getWorldY());
		// Child travelTip override applied
		assertEquals("Agility shortcut available", current.getTravelTip());
	}

	// ── 8. resolveAlternative direct call — nested, all match ────────────────

	@Test
	public void resolveAlternative_direct_nested_allMatch()
	{
		SourceRequirements parentReqs = makeQuestReq("LOST_CITY");
		SourceRequirements childReqs = makeSkillReq("AGILITY", 60);
		RequirementsChecker mockChecker = org.mockito.Mockito.mock(RequirementsChecker.class);
		when(mockChecker.meetsRequirements(parentReqs)).thenReturn(true);
		when(mockChecker.meetsRequirements(childReqs)).thenReturn(true);

		ConditionalAlternative child = makeAlt("Shortcut route", 2410, null, childReqs, null);
		ConditionalAlternative parent = makeAlt("Fairy ring", 2400, 4435, parentReqs,
			Collections.singletonList(child));

		GuidanceStep base = makeStepWithAlts("Walk", 3000, 3000, Arrays.asList(parent));
		GuidanceStep resolved = base.resolveAlternative(mockChecker);

		// Child description wins
		assertEquals("Shortcut route", resolved.getDescription());
		// Child worldX (2410) wins over parent's 2400
		assertEquals(2410, resolved.getWorldX());
		// Parent worldY stays (child null)
		assertEquals(4435, resolved.getWorldY());
		// Resolved step carries no alternatives (already flattened)
		assertNull(resolved.getConditionalAlternatives());
	}

	// ── 9. resolveAlternative — null nestedAlternatives → flat behaviour ───────

	@Test
	public void resolveAlternative_nullNestedAlternatives_treatedAsFlat()
	{
		SourceRequirements reqs = makeQuestReq("LOST_CITY");
		RequirementsChecker mockChecker = org.mockito.Mockito.mock(RequirementsChecker.class);
		when(mockChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = makeAlt("Flat alt", 2400, 4435, reqs, null);
		GuidanceStep base = makeStepWithAlts("Base", 3000, 3000, Arrays.asList(alt));
		GuidanceStep resolved = base.resolveAlternative(mockChecker);

		assertEquals("Flat alt", resolved.getDescription());
		assertEquals(2400, resolved.getWorldX());
	}

	// ── 10. resolveAlternative — empty nestedAlternatives → flat behaviour ─────

	@Test
	public void resolveAlternative_emptyNestedAlternatives_treatedAsFlat()
	{
		SourceRequirements reqs = makeQuestReq("LOST_CITY");
		RequirementsChecker mockChecker = org.mockito.Mockito.mock(RequirementsChecker.class);
		when(mockChecker.meetsRequirements(reqs)).thenReturn(true);

		ConditionalAlternative alt = makeAlt("Flat alt", 2400, 4435, reqs,
			Collections.emptyList());
		GuidanceStep base = makeStepWithAlts("Base", 3000, 3000, Arrays.asList(alt));
		GuidanceStep resolved = base.resolveAlternative(mockChecker);

		assertEquals("Flat alt", resolved.getDescription());
		assertEquals(2400, resolved.getWorldX());
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private SourceRequirements makeQuestReq(String questName)
	{
		return new SourceRequirements(Arrays.asList(questName), null, null);
	}

	private SourceRequirements makeSkillReq(String skill, int level)
	{
		return new SourceRequirements(null, Arrays.asList(new SkillRequirement(skill, level)), null);
	}

	/**
	 * Builds a minimal {@link ConditionalAlternative} overriding description, worldX,
	 * and worldY. worldX/worldY may be null (fall through to parent step). Other fields
	 * are null.
	 */
	private static ConditionalAlternative makeAlt(String description, Integer worldX, Integer worldY,
		SourceRequirements requirements, List<ConditionalAlternative> nestedAlternatives)
	{
		return new ConditionalAlternative(
			requirements,
			description,
			worldX, worldY, null,  // worldPlane falls through
			null,                  // travelTip
			null,                  // npcId
			null,                  // interactAction
			null,                  // objectId
			null,                  // completionCondition
			null,                  // completionDistance
			null,                  // completionNpcId
			nestedAlternatives
		);
	}

	private static GuidanceStep makeManualStep(String description)
	{
		return new GuidanceStep(
			description,
			null,           // perItemStepDescription
			0, 0, 0,
			0, null, null, null,
			null, null,
			null,           // perItemRequiredItemIds
			null,           // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.MANUAL,
			0, 0, 0, 0,
			null,           // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null,           // skipIfHasAnyItemIds
			null,           // dynamicItemObjectTiers
			null,           // completionZone
			null,           // conditionalAlternatives
			null, // section
			null, // waypoints
			null  // dynamicTargetEvaluator
		);
	}

	private static GuidanceStep makeStepWithAlts(String description, int worldX, int worldY,
		List<ConditionalAlternative> alternatives)
	{
		return new GuidanceStep(
			description,
			null,  // perItemStepDescription
			worldX, worldY, 0,
			0, null, null, null,
			null, null,
			null,  // perItemRequiredItemIds
			null,  // recommendedItemIds
			null,           // perItemRecommendedItemIds
			CompletionCondition.ARRIVE_AT_TILE,
			0, 0, 5, 0,
			null,  // completionNpcIds
			null,
			0, null, null,
			null, null,
			null,
			0, 0,
			false,
			0,
			null,
			null,
			0, 0,
			null,  // skipIfHasAnyItemIds
			null,  // dynamicItemObjectTiers
			null,  // completionZone
			alternatives,
			null, // section
			null, // waypoints
			null  // dynamicTargetEvaluator
		);
	}

	private void startSequence(List<GuidanceStep> steps)
	{
		CollectionLogSource source = new CollectionLogSource(
			"Test Source", CollectionLogCategory.BOSSES, 3000, 3000, 0,
			60, 0, "Test Source", Collections.emptyList(),
			RewardType.DROP, 0, null, 1, false, 0, null, 0, null, null,
			steps, null, 0, null, 0, Collections.emptyList(),
			null /* metaAuthoredDate */
		);
		sequencer.startSequence(source, step -> {}, () -> {});
	}
}
