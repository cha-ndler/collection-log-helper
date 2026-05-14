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
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

/**
 * RuneLite-backed implementation of {@link PohTeleportInventory}.
 *
 * <h2>Detection sources</h2>
 * <p>Each {@link PohTeleport} value is checked against two independent sources.
 * Either source returning {@code true} is sufficient (D-02 resolution rule):
 *
 * <ol>
 *   <li><b>Varbit detection</b> (where available):
 *     <ul>
 *       <li>JEWELLERY_BOX_BASIC  — varbit 2308 &gt;= 1</li>
 *       <li>JEWELLERY_BOX_FANCY  — varbit 2308 &gt;= 2</li>
 *       <li>JEWELLERY_BOX_ORNATE — varbit 2308 &gt;= 3</li>
 *       <li>PORTAL_NEXUS         — varbit 6670 != 0</li>
 *       <li>DIGSITE_PENDANT      — varbit 6651 != 0</li>
 *       <li>XERICS_TALISMAN      — varbit 6617 != 0</li>
 *     </ul>
 *   </li>
 *   <li><b>Manual config override</b> — user-declared checkboxes for teleports
 *       whose varbits are not reliably readable outside the house:
 *     <ul>
 *       <li>MOUNTED_GLORY — {@link CollectionLogHelperConfig#manualPohMountedGlory()}</li>
 *       <li>SPIRIT_TREE   — {@link CollectionLogHelperConfig#manualPohSpiritTree()}</li>
 *       <li>FAIRY_RING    — {@link CollectionLogHelperConfig#manualPohFairyRing()}</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h2>Varbit IDs</h2>
 * <p>The varbit IDs used here are raw integer literals because the RuneLite
 * {@code Varbits} class does not (as of this writing) expose named constants
 * for POH furniture state. The IDs were verified against the OSRS Wiki cache
 * data and cross-checked with RuneLite's existing POH plugin source:
 * <ul>
 *   <li>2308  — jewellery box tier (0=none, 1=basic, 2=fancy, 3=ornate)</li>
 *   <li>6670  — portal nexus tier (0=none, 1=marble, 2=gilded, 3=crystalline)</li>
 *   <li>6651  — mounted digsite pendant (0=absent, non-zero=present)</li>
 *   <li>6617  — mounted Xeric's talisman (0=absent, non-zero=present)</li>
 * </ul>
 *
 * <p>All varbit reads are wrapped in try/catch to handle the client not being
 * ready or the varbit returning an unexpected value gracefully — they default
 * to {@code false} on error per the D-02 safe-fallback principle.
 */
@Slf4j
@Singleton
public class PohTeleportInventoryImpl implements PohTeleportInventory
{
	// -------------------------------------------------------------------------
	// Varbit IDs for POH furniture state
	// Named per their role since RuneLite Varbits.java does not expose these.
	// -------------------------------------------------------------------------

	/** Jewellery box tier (0=none, 1=Basic, 2=Fancy, 3=Ornate). */
	private static final int VARBIT_JEWELLERY_BOX = 2308;

	/** Portal nexus tier (0=none, 1=Marble, 2=Gilded, 3=Crystalline). */
	private static final int VARBIT_PORTAL_NEXUS = 6670;

	/** Mounted digsite pendant (0=absent, non-zero=present). */
	private static final int VARBIT_MOUNTED_DIGSITE = 6651;

	/** Mounted Xeric's talisman (0=absent, non-zero=present). */
	private static final int VARBIT_MOUNTED_XERICS = 6617;

	// -------------------------------------------------------------------------

	private final Client client;
	private final CollectionLogHelperConfig config;

	/**
	 * Cached varbit-detected flags. Populated by {@link #refresh()},
	 * cleared by {@link #reset()}.
	 */
	private volatile boolean varbitJewelleryBoxBasic;
	private volatile boolean varbitJewelleryBoxFancy;
	private volatile boolean varbitJewelleryBoxOrnate;
	private volatile boolean varbitPortalNexus;
	private volatile boolean varbitDigsitePendant;
	private volatile boolean varbitXericsTalisman;

	@Inject
	PohTeleportInventoryImpl(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Resolution: varbit positive OR config override positive returns
	 * {@code true}. {@code null} input returns {@code false}.
	 */
	@Override
	public boolean hasTeleport(PohTeleport teleport)
	{
		if (teleport == null)
		{
			return false;
		}
		return varbitDetected(teleport) || configOverride(teleport);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Returns an immutable {@link EnumSet}-backed snapshot of all teleports
	 * for which {@link #hasTeleport} returns {@code true}.
	 */
	@Override
	public Set<PohTeleport> getAvailableTeleports()
	{
		Set<PohTeleport> result = EnumSet.noneOf(PohTeleport.class);
		for (PohTeleport teleport : PohTeleport.values())
		{
			if (hasTeleport(teleport))
			{
				result.add(teleport);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Reads all four POH varbits from the client and caches them. Any read
	 * failure is logged at WARN and treated as {@code false}.
	 */
	@Override
	public void refresh()
	{
		int jewelleryBox = readVarbit(VARBIT_JEWELLERY_BOX);
		varbitJewelleryBoxBasic = jewelleryBox >= 1;
		varbitJewelleryBoxFancy = jewelleryBox >= 2;
		varbitJewelleryBoxOrnate = jewelleryBox >= 3;

		varbitPortalNexus = readVarbit(VARBIT_PORTAL_NEXUS) != 0;
		varbitDigsitePendant = readVarbit(VARBIT_MOUNTED_DIGSITE) != 0;
		varbitXericsTalisman = readVarbit(VARBIT_MOUNTED_XERICS) != 0;

		log.debug("PohTeleportInventory refreshed: jboxBasic={}, jboxFancy={}, jboxOrnate={}, "
				+ "nexus={}, digsite={}, xerics={}",
			varbitJewelleryBoxBasic, varbitJewelleryBoxFancy, varbitJewelleryBoxOrnate,
			varbitPortalNexus, varbitDigsitePendant, varbitXericsTalisman);
	}

	/** {@inheritDoc} */
	@Override
	public void reset()
	{
		varbitJewelleryBoxBasic = false;
		varbitJewelleryBoxFancy = false;
		varbitJewelleryBoxOrnate = false;
		varbitPortalNexus = false;
		varbitDigsitePendant = false;
		varbitXericsTalisman = false;
		log.debug("PohTeleportInventory reset");
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Returns the varbit-detected state for the given teleport.
	 * Returns {@code false} for teleports that have no reliable varbit.
	 */
	private boolean varbitDetected(PohTeleport teleport)
	{
		switch (teleport)
		{
			case JEWELLERY_BOX_BASIC:
				return varbitJewelleryBoxBasic;
			case JEWELLERY_BOX_FANCY:
				return varbitJewelleryBoxFancy;
			case JEWELLERY_BOX_ORNATE:
				return varbitJewelleryBoxOrnate;
			case PORTAL_NEXUS:
				return varbitPortalNexus;
			case DIGSITE_PENDANT:
				return varbitDigsitePendant;
			case XERICS_TALISMAN:
				return varbitXericsTalisman;
			// MOUNTED_GLORY, SPIRIT_TREE, FAIRY_RING have no reliable varbit
			default:
				return false;
		}
	}

	/**
	 * Returns the manual config-override state for the given teleport.
	 * Returns {@code false} for teleports that have no config override.
	 */
	private boolean configOverride(PohTeleport teleport)
	{
		switch (teleport)
		{
			case MOUNTED_GLORY:
				return config.manualPohMountedGlory();
			case SPIRIT_TREE:
				return config.manualPohSpiritTree();
			case FAIRY_RING:
				return config.manualPohFairyRing();
			// Varbit-detected entries are considered authoritative when positive;
			// no separate config override is provided for them at C1.
			default:
				return false;
		}
	}

	/**
	 * Reads a varbit value from the client, returning {@code 0} on any error.
	 *
	 * @param varbitId the varbit ID to read
	 * @return the varbit value, or {@code 0} if the client throws
	 */
	private int readVarbit(int varbitId)
	{
		try
		{
			return client.getVarbitValue(varbitId);
		}
		catch (Exception e)
		{
			log.warn("Failed to read POH varbit {}", varbitId, e);
			return 0;
		}
	}
}
