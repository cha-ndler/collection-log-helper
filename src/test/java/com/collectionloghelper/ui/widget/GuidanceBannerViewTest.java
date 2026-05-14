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
import com.collectionloghelper.data.RequirementRow;
import com.collectionloghelper.data.RequirementsChecker;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link GuidanceBannerView}.
 */
public class GuidanceBannerViewTest
{
	private RequirementsChecker requirementsChecker;
	private GuidanceBannerView view;

	/** Minimal CollectionLogSource for tests — CollectionLogSource is a final Lombok @Value class. */
	private static CollectionLogSource makeSource(String name)
	{
		return new CollectionLogSource(name, CollectionLogCategory.BOSSES, 0, 0, 0,
			60, 0, null, null,
			null, 0, null, 0, false, 0, null, 0, null, null, null, null, 0, null, 0,
			Collections.emptyList(), null);
	}

	@Before
	public void setUp()
	{
		requirementsChecker = Mockito.mock(RequirementsChecker.class);
		view = new GuidanceBannerView(requirementsChecker);
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void showGuidance_withSource_noUnmetRequirements_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Zulrah");
		Mockito.when(requirementsChecker.getUnmetRequirements("Zulrah"))
			.thenReturn(Collections.emptyList());
		view.showGuidance(source, Collections.emptyList());
	}

	@Test
	public void showGuidance_withSource_hasUnmetRequirements_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Arrays.asList("Dragon Slayer II"));
		view.showGuidance(source, Collections.emptyList());
	}

	@Test
	public void hideGuidance_doesNotThrow()
	{
		view.hideGuidance();
	}

	@Test
	public void showGuidance_nullSource_doesNotThrow()
	{
		// snapshot-then-null-check: null source hides the banner
		view.showGuidance(null, null);
	}

	@Test
	public void showClueGuidance_withSource_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Easy Clue Scroll");
		view.showClueGuidance(source);
	}

	@Test
	public void hideClueGuidance_doesNotThrow()
	{
		view.hideClueGuidance();
	}

	// =========================================================================
	// B.5.3 — requirements section (quest / skill / diary rows)
	// =========================================================================

	/** Helper: a RequirementRow coloured green (met). */
	private static RequirementRow metQuestRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.QUEST, label,
			"COMPLETED", RequirementRow.COLOR_MET, true);
	}

	/** Helper: a RequirementRow coloured red (unmet). */
	private static RequirementRow unmetQuestRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.QUEST, label,
			"NOT STARTED", RequirementRow.COLOR_UNMET, false);
	}

	/** Helper: a met skill row. */
	private static RequirementRow metSkillRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.SKILL, label,
			"level 75/70", RequirementRow.COLOR_MET, true);
	}

	/** Helper: an unmet skill row. */
	private static RequirementRow unmetSkillRow(String label)
	{
		return new RequirementRow(RequirementRow.Category.SKILL, label,
			"level 60/70", RequirementRow.COLOR_UNMET, false);
	}

	@Test
	public void showGuidance_allRequirementsMet_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Collections.emptyList());

		List<RequirementRow> rows = Arrays.asList(
			metQuestRow("Dragon Slayer II"),
			metSkillRow("Agility 70")
		);

		// All green — should not throw and should show the banner
		view.showGuidance(source, rows);
	}

	@Test
	public void showGuidance_oneUnmetRequirement_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Arrays.asList("Dragon Slayer II"));

		List<RequirementRow> rows = Arrays.asList(
			unmetQuestRow("Dragon Slayer II"),
			metSkillRow("Agility 70")
		);

		// Mixed — should not throw
		view.showGuidance(source, rows);
	}

	@Test
	public void showGuidance_noRequirements_emptyRowList_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Giant Mole");
		Mockito.when(requirementsChecker.getUnmetRequirements("Giant Mole"))
			.thenReturn(Collections.emptyList());

		// Empty list → requirements section should be hidden (no-throw contract)
		view.showGuidance(source, Collections.emptyList());
	}

	@Test
	public void showGuidance_nullRowList_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Giant Mole");
		Mockito.when(requirementsChecker.getUnmetRequirements("Giant Mole"))
			.thenReturn(Collections.emptyList());

		// null rows treated as empty
		view.showGuidance(source, null);
	}

	@Test
	public void stateTransition_unmetBecomingMet_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Collections.emptyList());

		// First call: skill not yet met
		List<RequirementRow> firstRows = Collections.singletonList(unmetSkillRow("Agility 70"));
		view.showGuidance(source, firstRows);

		// Second call: skill is now met (simulates level-up between refreshes)
		List<RequirementRow> secondRows = Collections.singletonList(metSkillRow("Agility 70"));
		view.showGuidance(source, secondRows);
	}

	@Test
	public void hideGuidance_clearsRequirementsSection_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Vorkath");
		Mockito.when(requirementsChecker.getUnmetRequirements("Vorkath"))
			.thenReturn(Collections.emptyList());

		view.showGuidance(source, Collections.singletonList(metQuestRow("Dragon Slayer II")));
		// Deactivating guidance should clear the requirements section
		view.hideGuidance();
	}

	@Test
	public void showGuidance_diaryRequirement_doesNotThrow()
	{
		CollectionLogSource source = makeSource("Lumbridge Source");
		Mockito.when(requirementsChecker.getUnmetRequirements("Lumbridge Source"))
			.thenReturn(Collections.emptyList());

		List<RequirementRow> rows = Collections.singletonList(
			new RequirementRow(RequirementRow.Category.DIARY, "Lumbridge Elite",
				"COMPLETED", RequirementRow.COLOR_MET, true)
		);

		view.showGuidance(source, rows);
	}
}
