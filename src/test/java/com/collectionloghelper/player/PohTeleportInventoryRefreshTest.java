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
import net.runelite.api.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Regression guard for the #738-sibling log spam: {@code refresh()} previously
 * {@code log.debug}'d the six POH flags on every {@link net.runelite.api.events.VarbitChanged}
 * with no change detection. {@link PohTeleportInventoryImpl#recomputeFlags()} now
 * reports whether any flag changed so the log fires only on a genuine flip.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PohTeleportInventoryRefreshTest
{
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
	public void setUp()
	{
		lenient().when(client.getVarbitValue(anyInt())).thenReturn(0);
		inventory = new PohTeleportInventoryImpl(client, config);
	}

	@Test
	@DisplayName("recomputeFlags reports change only on a genuine flag flip (#738 sibling)")
	public void recomputeFlagsGuardsOnChange()
	{
		// No POH furniture, fields already default false -> nothing changed.
		assertFalse(inventory.recomputeFlags(), "all-zero varbits from default state is no change");

		// Ornate jewellery box appears -> basic/fancy/ornate flip true.
		when(client.getVarbitValue(VARBIT_JEWELLERY_BOX)).thenReturn(3);
		assertTrue(inventory.recomputeFlags(), "jewellery box appearing is a change");
		assertTrue(inventory.hasTeleport(PohTeleport.JEWELLERY_BOX_ORNATE));

		// Same varbits again -> the per-tick storm case, no change, no log.
		assertFalse(inventory.recomputeFlags(), "unchanged varbits must report no change");

		// Portal nexus appears -> change again.
		when(client.getVarbitValue(VARBIT_PORTAL_NEXUS)).thenReturn(1);
		assertTrue(inventory.recomputeFlags(), "portal nexus appearing is a change");
		assertFalse(inventory.recomputeFlags(), "and is stable on the next identical read");
	}

	@Test
	@DisplayName("recomputeFlags detects digsite and xeric's mounts independently")
	public void recomputeFlagsDetectsMounts()
	{
		inventory.recomputeFlags(); // baseline

		when(client.getVarbitValue(VARBIT_MOUNTED_DIGSITE)).thenReturn(1);
		assertTrue(inventory.recomputeFlags());
		assertTrue(inventory.hasTeleport(PohTeleport.DIGSITE_PENDANT));

		when(client.getVarbitValue(VARBIT_MOUNTED_XERICS)).thenReturn(1);
		assertTrue(inventory.recomputeFlags());
		assertTrue(inventory.hasTeleport(PohTeleport.XERICS_TALISMAN));
	}
}
