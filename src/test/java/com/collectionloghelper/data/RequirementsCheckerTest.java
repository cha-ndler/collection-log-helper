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

import net.runelite.api.Varbits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RequirementsCheckerTest
{
	// ========================================================================
	// formatEnumName — package-private static utility
	// ========================================================================

	@Test
	public void formatEnumName_singleWord()
	{
		assertEquals("Attack", RequirementsChecker.formatEnumName("ATTACK"));
	}

	@Test
	public void formatEnumName_multipleWords()
	{
		assertEquals("Dragon Slayer", RequirementsChecker.formatEnumName("DRAGON_SLAYER"));
	}

	@Test
	public void formatEnumName_romanNumerals()
	{
		assertEquals("Desert Treasure II", RequirementsChecker.formatEnumName("DESERT_TREASURE_II"));
	}

	@Test
	public void formatEnumName_mixedRomanNumerals()
	{
		// "of" and "the" are connector words — they stay lowercase mid-name
		assertEquals("Song of the Elves IV", RequirementsChecker.formatEnumName("SONG_OF_THE_ELVES_IV"));
	}

	@Test
	public void formatEnumName_singleCharacterRomanNumeral()
	{
		assertEquals("Fairytale I", RequirementsChecker.formatEnumName("FAIRYTALE_I"));
	}

	@Test
	public void formatEnumName_consecutiveUnderscores()
	{
		// Fairytale II has double underscore in enum: FAIRYTALE_II__CURE_A_QUEEN
		// "a" is a connector word so it stays lowercase mid-name
		assertEquals("Fairytale II Cure a Queen",
			RequirementsChecker.formatEnumName("FAIRYTALE_II__CURE_A_QUEEN"));
	}

	@Test
	public void formatEnumName_trailingUnderscore()
	{
		assertEquals("Test", RequirementsChecker.formatEnumName("TEST_"));
	}

	@Test
	public void formatEnumName_allLowercase()
	{
		// Method preserves first char case — no toUpperCase call
		assertEquals("hitpoints", RequirementsChecker.formatEnumName("hitpoints"));
	}

	@Test
	public void formatEnumName_romanNumeralX()
	{
		assertEquals("X", RequirementsChecker.formatEnumName("X"));
	}

	@Test
	public void formatEnumName_romanNumeralVII()
	{
		assertEquals("Part VII", RequirementsChecker.formatEnumName("PART_VII"));
	}

	// #680 — connector words and apostrophe overrides

	@Test
	public void formatEnumName_connectorOf_isLowercase()
	{
		assertEquals("Shades of Mort'ton", RequirementsChecker.formatEnumName("SHADES_OF_MORTTON"));
	}

	@Test
	public void formatEnumName_connectorThe_isLowercase()
	{
		// "the" mid-name stays lowercase; first word is always capitalised
		assertEquals("Song of the Elves IV", RequirementsChecker.formatEnumName("SONG_OF_THE_ELVES_IV"));
	}

	@Test
	public void formatEnumName_firstWordAlwaysCapitalised()
	{
		// Even when the first token is a connector word it must be capitalised;
		// "and" in the middle stays lowercase
		assertEquals("Of Mice and Men", RequirementsChecker.formatEnumName("OF_MICE_AND_MEN"));
	}

	@Test
	public void formatEnumName_connectorAnd_isLowercase()
	{
		assertEquals("Rum Deal", RequirementsChecker.formatEnumName("RUM_DEAL")); // sanity — no connector
		assertEquals("Rune and Ruin", RequirementsChecker.formatEnumName("RUNE_AND_RUIN")); // "and" lowercase
	}

	@Test
	public void formatEnumName_dragonSlayerUnchanged()
	{
		// Regression: non-connector words still title-cased normally
		assertEquals("Dragon Slayer", RequirementsChecker.formatEnumName("DRAGON_SLAYER"));
	}

	// =========================================================================
	// resolveDiaryVarbit — static resolver for DIARY_<AREA>_<TIER> Varbits constants
	// =========================================================================

	@Test
	public void resolveDiaryVarbit_knownDiary_returnsPositiveVarbit()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit("ARDOUGNE_ELITE");
		assertTrue( varbit > 0,"Known diary must resolve to a positive varbit ID");
		assertEquals(Varbits.DIARY_ARDOUGNE_ELITE, varbit);
	}

	@Test
	public void resolveDiaryVarbit_lumbridgeHard_returnsExpected()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit("LUMBRIDGE_HARD");
		assertEquals(Varbits.DIARY_LUMBRIDGE_HARD, varbit);
	}

	@Test
	public void resolveDiaryVarbit_kandarin_medium_returnsExpected()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit("KANDARIN_MEDIUM");
		assertEquals(Varbits.DIARY_KANDARIN_MEDIUM, varbit);
	}

	@Test
	public void resolveDiaryVarbit_unknownDiary_returnsMinusOne()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit("NONEXISTENT_AREA_ELITE");
		assertEquals( -1, varbit,"Unknown diary must return -1");
	}

	@Test
	public void resolveDiaryVarbit_nullInput_returnsMinusOne()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit(null);
		assertEquals( -1, varbit,"Null diary must return -1");
	}

	@Test
	public void resolveDiaryVarbit_emptyInput_returnsMinusOne()
	{
		int varbit = RequirementsChecker.resolveDiaryVarbit("");
		assertEquals( -1, varbit,"Empty diary must return -1");
	}

	@Test
	public void resolveDiaryVarbit_lowercaseInput_resolvedCorrectly()
	{
		// resolveDiaryVarbit normalises to uppercase internally
		int varbit = RequirementsChecker.resolveDiaryVarbit("ardougne_easy");
		assertEquals(Varbits.DIARY_ARDOUGNE_EASY, varbit);
	}
}
