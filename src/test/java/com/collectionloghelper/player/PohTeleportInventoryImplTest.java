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
package com.collectionloghelper.player;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.lang.reflect.Constructor;
import java.util.Set;
import net.runelite.api.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link PohTeleportInventoryImpl}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Initial state — all false before refresh</li>
 *   <li>Null safety on {@code hasTeleport(null)}</li>
 *   <li>Varbit detection for each enum value with a varbit</li>
 *   <li>Config override for each enum value relying on manual override</li>
 *   <li>D-02 OR rule: varbit OR config override returns true</li>
 *   <li>Client exception handling</li>
 *   <li>State updates on second refresh</li>
 *   <li>Reset clears all cached state</li>
 *   <li>{@code getAvailableTeleports()} returns correct immutable snapshot</li>
 *   <li>All enum values are exercised (no missing switch case)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PohTeleportInventoryImplTest
{
	// -------------------------------------------------------------------------
	// Varbit IDs (must match PohTeleportInventoryImpl constants)
	// -------------------------------------------------------------------------
	private static final int VARBIT_JEWELLERY_BOX = 2308;
	private static final int VARBIT_PORTAL_NEXUS = 6670;
	private static final int VARBIT_MOUNTED_DIGSITE = 6651;
	private static final int VARBIT_MOUNTED_XERICS = 6617;

	@Mock
	private Client client;

	@Mock
	private CollectionLogHelperConfig config;

	private PohTeleportInventoryImpl inventory;

	@BeforeEach
	public void setUp() throws Exception
	{
		Constructor<PohTeleportInventoryImpl> ctor =
			PohTeleportInventoryImpl.class.getDeclaredConstructor(Client.class, CollectionLogHelperConfig.class);
		ctor.setAccessible(true);
		inventory = ctor.newInstance(client, config);
	}

	// =========================================================================
	// Initial state
	// =========================================================================

	@Test
	public void initialState_allFalseBeforeRefresh()
	{
		for (PohTeleport t : PohTeleport.values())
		{
			assertFalse( inventory.hasTeleport(t),"Expected false before refresh for " + t);
		}
	}

	@Test
	public void initialState_availableTeleportsEmpty()
	{
		assertTrue(inventory.getAvailableTeleports().isEmpty());
	}

	// =========================================================================
	// Null safety
	// =========================================================================

	@Test
	public void hasTeleport_null_returnsFalse()
	{
		assertFalse(inventory.hasTeleport(null));
	}

	// =========================================================================
	// Varbit detection — jewellery box tiers
	// =========================================================================

	@Test
	public void refresh_jewelleryBoxVarbit1_onlyBasicTrue()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(1);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	@Test
	public void refresh_jewelleryBoxVarbit2_basicAndFancyTrue()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(2);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	@Test
	public void refresh_jewelleryBoxVarbit3_allTiersTrue()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(3);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	@Test
	public void refresh_jewelleryBoxVarbit0_allTiersFalse()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(0);

		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	// =========================================================================
	// Varbit detection — portal nexus
	// =========================================================================

	@Test
	public void refresh_portalNexusVarbit1_nexusTrue()
	{
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(1);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
	}

	@Test
	public void refresh_portalNexusVarbit3_nexusTrue()
	{
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(3);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
	}

	@Test
	public void refresh_portalNexusVarbit0_nexusFalse()
	{
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(0);

		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
	}

	// =========================================================================
	// Varbit detection — mounted jewellery
	// =========================================================================

	@Test
	public void refresh_digsitePendantVarbit1_digsiteTrue()
	{
		when(client.getVarbitValue(VARBIT_MOUNTED_DIGSITE)).thenReturn(1);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));
	}

	@Test
	public void refresh_digsitePendantVarbit0_digsiteFalse()
	{
		when(client.getVarbitValue(VARBIT_MOUNTED_DIGSITE)).thenReturn(0);

		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));
	}

	@Test
	public void refresh_xericsTalismanVarbit1_xericsTrue()
	{
		when(client.getVarbitValue(VARBIT_MOUNTED_XERICS)).thenReturn(1);

		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));
	}

	@Test
	public void refresh_xericsTalismanVarbit0_xericsFalse()
	{
		when(client.getVarbitValue(VARBIT_MOUNTED_XERICS)).thenReturn(0);

		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));
	}

	// =========================================================================
	// Config override — MOUNTED_GLORY (no varbit, config is primary)
	// =========================================================================

	@Test
	public void configOverride_mountedGloryTrue_gloryTrue()
	{
		when(config.manualPohMountedGlory()).thenReturn(true);

		assertTrue(inventory.hasTeleport(PohTeleport.MOUNTED_GLORY));
	}

	@Test
	public void configOverride_mountedGloryFalse_gloryFalse()
	{
		when(config.manualPohMountedGlory()).thenReturn(false);

		assertFalse(inventory.hasTeleport(PohTeleport.MOUNTED_GLORY));
	}

	// =========================================================================
	// Config override — SPIRIT_TREE (no varbit, config is primary)
	// =========================================================================

	@Test
	public void configOverride_spiritTreeTrue_spiritTreeTrue()
	{
		when(config.manualPohSpiritTree()).thenReturn(true);

		assertTrue(inventory.hasTeleport(PohTeleport.SPIRIT_TREE));
	}

	@Test
	public void configOverride_spiritTreeFalse_spiritTreeFalse()
	{
		when(config.manualPohSpiritTree()).thenReturn(false);

		assertFalse(inventory.hasTeleport(PohTeleport.SPIRIT_TREE));
	}

	// =========================================================================
	// Config override — FAIRY_RING (no varbit, config is primary)
	// =========================================================================

	@Test
	public void configOverride_fairyRingTrue_fairyRingTrue()
	{
		when(config.manualPohFairyRing()).thenReturn(true);

		assertTrue(inventory.hasTeleport(PohTeleport.FAIRY_RING));
	}

	@Test
	public void configOverride_fairyRingFalse_fairyRingFalse()
	{
		when(config.manualPohFairyRing()).thenReturn(false);

		assertFalse(inventory.hasTeleport(PohTeleport.FAIRY_RING));
	}

	// =========================================================================
	// D-02 OR rule: varbit OR config override returns true
	// =========================================================================

	@Test
	public void orRule_varbitTrueConfigNotSet_returnsTrue()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(1);
		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
	}

	@Test
	public void orRule_varbitFalseConfigTrue_returnsTrue()
	{
		// Mounted glory: no varbit path; config override is the only source
		when(config.manualPohMountedGlory()).thenReturn(true);

		assertTrue(inventory.hasTeleport(PohTeleport.MOUNTED_GLORY));
	}

	@Test
	public void orRule_bothFalse_returnsFalse()
	{
		when(config.manualPohMountedGlory()).thenReturn(false);
		// client returns 0 by default for all varbits
		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.MOUNTED_GLORY));
	}

	// =========================================================================
	// Client exception handling
	// =========================================================================

	@Test
	public void refresh_clientThrows_varbitEntriesAllFalse()
	{
		when(client.getVarbitValue(anyInt()))
			.thenThrow(new RuntimeException("client not ready"));

		// Must not throw; only varbit-detected entries are checked here —
		// config-override entries (MOUNTED_GLORY etc.) are tested in their own tests.
		inventory.refresh();

		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
		assertFalse(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
		assertFalse(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));
		assertFalse(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));
	}

	// =========================================================================
	// State updates on second refresh
	// =========================================================================

	@Test
	public void refresh_secondCallUpdatesState()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(0);
		inventory.refresh();
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));

		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(3);
		inventory.refresh();
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
	}

	// =========================================================================
	// reset clears all varbit cache
	// =========================================================================

	@Test
	public void reset_afterRefreshWithData_varbitsFalse()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(3);
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(2);
		when(client.getVarbitValue(VARBIT_MOUNTED_DIGSITE)).thenReturn(1);
		when(client.getVarbitValue(VARBIT_MOUNTED_XERICS)).thenReturn(1);
		inventory.refresh();

		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
		assertTrue(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
		assertTrue(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));
		assertTrue(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));

		when(config.manualPohMountedGlory()).thenReturn(false);
		when(config.manualPohSpiritTree()).thenReturn(false);
		when(config.manualPohFairyRing()).thenReturn(false);

		inventory.reset();

		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_BASIC));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_FANCY));
		assertFalse(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));
		assertFalse(inventory.hasTeleport(PohTeleport.PORTAL_NEXUS));
		assertFalse(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));
		assertFalse(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));
		assertFalse(inventory.hasTeleport(PohTeleport.MOUNTED_GLORY));
		assertFalse(inventory.hasTeleport(PohTeleport.SPIRIT_TREE));
		assertFalse(inventory.hasTeleport(PohTeleport.FAIRY_RING));
	}

	// =========================================================================
	// getAvailableTeleports — snapshot correctness and immutability
	// =========================================================================

	@Test
	public void getAvailableTeleports_withOrnateBoxAndNexus_containsExpectedEntries()
	{
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(3);
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(1);
		inventory.refresh();

		Set<PohTeleport> available = inventory.getAvailableTeleports();

		assertTrue(available.contains(PohTeleport.JEWELLERY_BOX_BASIC));
		assertTrue(available.contains(PohTeleport.JEWELLERY_BOX_FANCY));
		assertTrue(available.contains(PohTeleport.JEWELLERY_BOX_ORNATE));
		assertTrue(available.contains(PohTeleport.PORTAL_NEXUS));
		assertFalse(available.contains(PohTeleport.MOUNTED_GLORY));
		assertFalse(available.contains(PohTeleport.SPIRIT_TREE));
		assertFalse(available.contains(PohTeleport.FAIRY_RING));
	}

	@Test
	public void getAvailableTeleports_withConfigOverrides_includesManualEntries()
	{
		when(config.manualPohMountedGlory()).thenReturn(true);
		when(config.manualPohSpiritTree()).thenReturn(true);
		when(config.manualPohFairyRing()).thenReturn(true);

		Set<PohTeleport> available = inventory.getAvailableTeleports();

		assertTrue(available.contains(PohTeleport.MOUNTED_GLORY));
		assertTrue(available.contains(PohTeleport.SPIRIT_TREE));
		assertTrue(available.contains(PohTeleport.FAIRY_RING));
	}

	@Test
	public void getAvailableTeleports_returnedSetIsImmutable()
	{
		assertThrows(UnsupportedOperationException.class, () ->
		{
			Set<PohTeleport> available = inventory.getAvailableTeleports();
			available.add(PohTeleport.MOUNTED_GLORY); // must throw
		});
	}

	// =========================================================================
	// All enum values exercised — no missing switch default case
	// =========================================================================

	@Test
	public void allEnumValues_noMissingSwitchCase()
	{
		when(config.manualPohMountedGlory()).thenReturn(false);
		when(config.manualPohSpiritTree()).thenReturn(false);
		when(config.manualPohFairyRing()).thenReturn(false);
		inventory.refresh();
		for (PohTeleport t : PohTeleport.values())
		{
			assertFalse( inventory.hasTeleport(t),"Unexpected true for " + t);
		}
	}
}
