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

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

/**
 * RuneLite-backed implementation of {@link SkillCapePerkState}.
 *
 * <p>For each perk, availability is determined by two checks in order:
 * <ol>
 *   <li><b>Equipped-item check</b>: the equipment container
 *       ({@link InventoryID#EQUIPMENT}) is scanned for any item ID listed in
 *       {@link SkillCapePerk#getCapeItemIds()}. If a match is found the perk
 *       is immediately considered available.</li>
 *   <li><b>Owned-status fallback</b>: if the cape is not currently equipped,
 *       {@link Client#getRealSkillLevel(Skill)} is called for the perk's
 *       backing skill. A real level of {@code >= 99} indicates the player can
 *       wear the cape on demand.</li>
 * </ol>
 *
 * <p>The {@link SkillCapePerk#MAX_CAPE_TELES} perk is an exception: its
 * backing skill is {@link Skill#OVERALL} (a sentinel), so the owned-status
 * fallback is skipped for that perk and only the equipped-item check applies.
 *
 * <p>The implementation caches one {@link EnumMap} snapshot per {@link #refresh()}
 * call. Reads from {@link #hasPerkAvailable(SkillCapePerk)} are lock-free
 * because the map reference is replaced atomically via a {@code volatile}
 * field on each refresh.
 *
 * <p>Thread safety: {@link #refresh()} and {@link #reset()} must be called on
 * the client thread. {@link #hasPerkAvailable(SkillCapePerk)} may be called
 * from any thread.
 */
@Slf4j
@Singleton
public class SkillCapePerkStateImpl implements SkillCapePerkState
{
	private final Client client;

	/**
	 * Volatile reference to the cached perk-availability map.
	 * Replaced atomically on each {@link #refresh()} to avoid locking.
	 */
	private volatile Map<SkillCapePerk, Boolean> cache;

	/**
	 * Last-seen real (unboosted) level per skill, used to suppress redundant
	 * refreshes on XP-only {@link StatChanged} events. Accessed only on the client
	 * thread, so no synchronization is needed.
	 */
	private final Map<Skill, Integer> lastRealLevel = new EnumMap<>(Skill.class);

	@Inject
	SkillCapePerkStateImpl(Client client)
	{
		this.client = client;
		this.cache = buildEmptyCache();
	}

	// -------------------------------------------------------------------------
	// SkillCapePerkState
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public boolean hasPerkAvailable(SkillCapePerk perk)
	{
		if (perk == null)
		{
			return false;
		}
		Boolean value = cache.get(perk);
		return value != null && value;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Reads the equipment container once and caches the equipped item IDs
	 * into a temporary set. Then, for each {@link SkillCapePerk}, checks:
	 * <ol>
	 *   <li>Whether any of the perk's cape item IDs appear in that set.</li>
	 *   <li>If not, and the perk's backing skill is not {@link Skill#OVERALL},
	 *       whether {@code getRealSkillLevel(skill) >= 99}.</li>
	 * </ol>
	 */
	@Override
	public void refresh()
	{
		Set<Integer> equippedIds = readEquipmentIds();

		Map<SkillCapePerk, Boolean> next = new EnumMap<>(SkillCapePerk.class);
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			next.put(perk, computeAvailable(perk, equippedIds));
		}
		cache = Collections.unmodifiableMap(next);
		log.debug("SkillCapePerkState refreshed");
	}

	/**
	 * Refreshes the perk-availability cache when the worn or inventory container
	 * changes. Inventory matters because a player may carry a cape they own and
	 * swap to it on demand (Max cape teleport menu, etc.).
	 */
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == net.runelite.api.gameval.InventoryID.WORN
			|| containerId == net.runelite.api.gameval.InventoryID.INV)
		{
			refresh();
		}
	}

	/**
	 * Refreshes the perk-availability cache on any {@link StatChanged} event.
	 *
	 * <p>Hitting level 99 in any skill unlocks a new cape perk, so a stat-level
	 * change can flip {@link #hasPerkAvailable(SkillCapePerk)} from {@code false}
	 * to {@code true}. {@link StatChanged}, however, fires on every XP drop — not
	 * just on level-ups — so it can arrive many times per second while training.
	 * The perk fallback depends only on the real level, so a refresh is skipped
	 * unless the changed skill's real level actually moved.
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		int level = event.getLevel();
		Integer previous = lastRealLevel.put(skill, level);
		if (previous != null && previous == level)
		{
			// XP-only change at the same real level — perk availability cannot change.
			return;
		}
		refresh();
	}

	/** {@inheritDoc} */
	@Override
	public void reset()
	{
		cache = buildEmptyCache();
		lastRealLevel.clear();
		log.debug("SkillCapePerkState reset");
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Reads the equipment container and returns a set of the item IDs currently
	 * equipped. Returns an empty set if the container is absent (e.g., login
	 * screen) or if reading fails.
	 */
	private Set<Integer> readEquipmentIds()
	{
		try
		{
			ItemContainer container = client.getItemContainer(InventoryID.EQUIPMENT);
			if (container == null)
			{
				return Collections.emptySet();
			}
			Item[] items = container.getItems();
			Set<Integer> ids = new HashSet<>(items.length * 2);
			for (Item item : items)
			{
				if (item.getId() != -1)
				{
					ids.add(item.getId());
				}
			}
			return ids;
		}
		catch (Exception e)
		{
			log.warn("SkillCapePerkState: failed to read equipment container", e);
			return Collections.emptySet();
		}
	}

	/**
	 * Determines whether {@code perk} is available given the current
	 * {@code equippedIds} snapshot.
	 *
	 * <ol>
	 *   <li>Equipped check: returns {@code true} if any item ID in
	 *       {@link SkillCapePerk#getCapeItemIds()} is present in
	 *       {@code equippedIds}.</li>
	 *   <li>Owned-status fallback: if the perk's skill is not
	 *       {@link Skill#OVERALL}, returns {@code true} when
	 *       {@code getRealSkillLevel(skill) >= 99}.</li>
	 * </ol>
	 */
	private boolean computeAvailable(SkillCapePerk perk, Set<Integer> equippedIds)
	{
		// 1. Equipped-item check
		for (int id : perk.getCapeItemIds())
		{
			if (equippedIds.contains(id))
			{
				return true;
			}
		}

		// 2. Owned-status fallback (skipped for MAX_CAPE_TELES)
		Skill skill = perk.getSkill();
		if (skill == Skill.OVERALL)
		{
			// Max cape requires ALL skills at 99 — cannot verify with a single
			// skill level; rely on equipped-item check only.
			return false;
		}

		try
		{
			return client.getRealSkillLevel(skill) >= 99;
		}
		catch (Exception e)
		{
			log.warn("SkillCapePerkState: failed to read skill level for {}", skill, e);
			return false;
		}
	}

	/** Returns an unmodifiable EnumMap with all perks mapped to {@code false}. */
	private static Map<SkillCapePerk, Boolean> buildEmptyCache()
	{
		Map<SkillCapePerk, Boolean> map = new EnumMap<>(SkillCapePerk.class);
		for (SkillCapePerk perk : SkillCapePerk.values())
		{
			map.put(perk, false);
		}
		return Collections.unmodifiableMap(map);
	}
}
