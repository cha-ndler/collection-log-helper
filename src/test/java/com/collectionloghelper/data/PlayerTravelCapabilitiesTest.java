/*
 * Copyright (c) 2025, Chandler <ch@ndler.net>
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerTravelCapabilitiesTest
{
	@Mock
	private Client client;

	private PlayerTravelCapabilities capabilities;

	@Before
	public void setUp() throws Exception
	{
		Constructor<PlayerTravelCapabilities> ctor =
			PlayerTravelCapabilities.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		capabilities = ctor.newInstance(client);
	}

	// --- Helper to set fields via reflection ---

	private void setField(String fieldName, Object value) throws Exception
	{
		Field field = PlayerTravelCapabilities.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(capabilities, value);
	}

	private void enableFairyRings() throws Exception
	{
		setField("fairyRings", true);
		setField("staffFreeFairyRings", true);
	}

	private void enableSpiritTrees() throws Exception
	{
		setField("spiritTrees", true);
	}

	private void enablePoh() throws Exception
	{
		setField("pohLocation", 1);
	}

	// ========================================================================
	// (a) Null/empty input
	// ========================================================================

	@Test
	public void selectBestTravelTip_null_returnsNull()
	{
		assertNull(capabilities.selectBestTravelTip(null));
	}

	@Test
	public void selectBestTravelTip_empty_returnsEmpty()
	{
		assertEquals("", capabilities.selectBestTravelTip(""));
	}

	// ========================================================================
	// (b) Single option, available
	// ========================================================================

	@Test
	public void selectBestTravelTip_singleAvailableGamesNecklace() throws Exception
	{
		setField("gamesNecklace", true);
		String tip = "Games necklace -> Corp";
		// Single option with no " or " separator returns the full string directly
		assertEquals(tip, capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (c) Single option, unavailable — returns full string as fallback
	// ========================================================================

	@Test
	public void selectBestTravelTip_singleUnavailableFairyRing()
	{
		// fairyRings defaults to false
		String tip = "Fairy ring BIP";
		// Single option: no splitting occurs, returns full string
		assertEquals(tip, capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (d) Multiple options, first available
	// ========================================================================

	@Test
	public void selectBestTravelTip_multipleFirstAvailable() throws Exception
	{
		enableFairyRings();
		enablePoh();
		String tip = "Fairy ring BIP, or POH portal";
		assertEquals("Fairy ring BIP", capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (e) Multiple options, first unavailable — picks second
	// ========================================================================

	@Test
	public void selectBestTravelTip_multipleFirstUnavailable() throws Exception
	{
		// fairyRings not enabled, but POH is
		enablePoh();
		String tip = "Fairy ring BIP, or POH portal";
		assertEquals("POH portal", capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (f) Multiple options, none available — returns full original
	// ========================================================================

	@Test
	public void selectBestTravelTip_multipleNoneAvailable()
	{
		// Neither fairy rings nor lunar spellbook available
		String tip = "Fairy ring BIP, or Lunar teleport";
		assertEquals(tip, capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (g) Always-available options (minigame teleport)
	// ========================================================================

	@Test
	public void selectBestTravelTip_minigameTeleportAlwaysAvailable()
	{
		String tip = "Minigame teleport -> Shades";
		assertEquals(tip, capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (h) Mixed with walk fallback
	// ========================================================================

	@Test
	public void selectBestTravelTip_walkFallbackAlwaysAvailable() throws Exception
	{
		// Fairy rings NOT available, walk should be picked
		String tip = "Fairy ring AKQ, or walk from Piscatoris";
		assertEquals("walk from Piscatoris", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_fairyRingPreferredOverWalk() throws Exception
	{
		enableFairyRings();
		String tip = "Fairy ring AKQ, or walk from Piscatoris";
		assertEquals("Fairy ring AKQ", capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (i) Spellbook-specific: Arceuus, available
	// ========================================================================

	@Test
	public void selectBestTravelTip_arceuusSpellbookAvailable() throws Exception
	{
		setField("activeSpellbook", PlayerTravelCapabilities.SPELLBOOK_ARCEUUS);
		String tip = "Barrows teleport (Arceuus), or walk from Canifis";
		assertEquals("Barrows teleport (Arceuus)", capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// (j) Spellbook-specific, wrong book — falls through
	// ========================================================================

	@Test
	public void selectBestTravelTip_arceuusSpellbookWrongBook() throws Exception
	{
		setField("activeSpellbook", PlayerTravelCapabilities.SPELLBOOK_STANDARD);
		String tip = "Barrows teleport (Arceuus), or walk from Canifis";
		assertEquals("walk from Canifis", capabilities.selectBestTravelTip(tip));
	}

	// ========================================================================
	// Additional edge cases
	// ========================================================================

	@Test
	public void selectBestTravelTip_threeOptions_middleAvailable() throws Exception
	{
		// Fairy rings off, games necklace on, walk always available
		setField("gamesNecklace", true);
		String tip = "Fairy ring CKR, or Games necklace -> Burthorpe, or walk from Taverly";
		assertEquals("Games necklace -> Burthorpe", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_spiritTree() throws Exception
	{
		enableSpiritTrees();
		String tip = "Spirit tree to Gnome Stronghold, or walk from Ardougne";
		assertEquals("Spirit tree to Gnome Stronghold", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_spiritTreeUnavailable()
	{
		// spiritTrees defaults to false
		String tip = "Spirit tree to Gnome Stronghold, or walk from Ardougne";
		assertEquals("walk from Ardougne", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_glory() throws Exception
	{
		setField("glory", true);
		String tip = "Amulet of glory -> Edgeville, or walk from Varrock";
		assertEquals("Amulet of glory -> Edgeville", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_slayerRing() throws Exception
	{
		setField("slayerRing", true);
		String tip = "Slayer ring -> Stronghold, or walk from Nieve";
		assertEquals("Slayer ring -> Stronghold", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_xericsTalisman() throws Exception
	{
		setField("xericsTalisman", true);
		String tip = "Xeric's talisman -> Lookout, or walk from Shayzien";
		assertEquals("Xeric's talisman -> Lookout", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_digsitePendant() throws Exception
	{
		setField("digsitePendant", true);
		String tip = "Digsite pendant -> Fossil Island, or charter ship";
		assertEquals("Digsite pendant -> Fossil Island", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_royalSeedPod() throws Exception
	{
		setField("royalSeedPod", true);
		String tip = "Royal seed pod -> Grand Exchange, or walk from Lumbridge";
		assertEquals("Royal seed pod -> Grand Exchange", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_ringOfDueling() throws Exception
	{
		setField("ringOfDueling", true);
		String tip = "Ring of dueling -> Ferox, or walk from Varrock";
		assertEquals("Ring of dueling -> Ferox", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_lunarSpellbook() throws Exception
	{
		setField("activeSpellbook", PlayerTravelCapabilities.SPELLBOOK_LUNAR);
		String tip = "Moonclan teleport, or walk from Rellekka";
		assertEquals("Moonclan teleport", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_ancientSpellbook() throws Exception
	{
		setField("activeSpellbook", PlayerTravelCapabilities.SPELLBOOK_ANCIENT);
		String tip = "Paddewwa teleport, or walk from Edgeville";
		assertEquals("Paddewwa teleport", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_unknownKeywordAssumedAvailable() throws Exception
	{
		// Options with no recognized keyword are assumed available
		String tip = "Charter ship to Port Sarim, or walk from Draynor";
		assertEquals("Charter ship to Port Sarim", capabilities.selectBestTravelTip(tip));
	}

	@Test
	public void selectBestTravelTip_orWithinOptionName() throws Exception
	{
		// "or" as separator only — single option without " or " should return as-is
		String tip = "Minigame teleport -> Nightmare Zone";
		assertEquals(tip, capabilities.selectBestTravelTip(tip));
	}
}
