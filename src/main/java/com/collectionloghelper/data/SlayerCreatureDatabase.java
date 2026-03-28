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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Slayer task creature names to collection log source names
 * that are relevant (typically boss variants killable on that task).
 * <p>
 * Source names must match exactly those in drop_rates.json.
 */
public final class SlayerCreatureDatabase
{
	private static final Map<String, List<String>> CREATURE_TO_SOURCES;

	/** Reverse lookup: source name → creature task name. */
	private static final Map<String, String> SOURCE_TO_CREATURE;

	/**
	 * Sources that require an active Slayer task to kill. These are the per-creature
	 * Slayer sources — boss variants (Abyssal Sire, Kraken, etc.) can be fought off-task.
	 */
	private static final Set<String> TASK_ONLY_SOURCES;

	static
	{
		Map<String, List<String>> map = new HashMap<>();

		// Boss-only mappings (existing)
		map.put("black demons", Arrays.asList("Demonic gorillas", "Skotizo"));
		map.put("greater demons", Collections.singletonList("K'ril Tsutsaroth"));
		map.put("aviansie", Collections.singletonList("Kree'arra"));
		map.put("kalphite", Collections.singletonList("Kalphite Queen"));
		map.put("hellhounds", Collections.singletonList("Cerberus"));
		map.put("dagannoth", Arrays.asList("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme"));
		map.put("lizardmen", Collections.singletonList("Lizardman shaman"));
		map.put("tzhaar", Arrays.asList("TzHaar", "The Fight Caves", "The Inferno"));

		// Per-creature Slayer sources + boss variants
		map.put("crawling hands", Collections.singletonList("Crawling Hand"));
		map.put("cave crawlers", Collections.singletonList("Cave Crawler"));
		map.put("rockslugs", Collections.singletonList("Rockslug"));
		map.put("cockatrices", Collections.singletonList("Cockatrice"));
		map.put("pyrefiends", Collections.singletonList("Pyrefiend"));
		map.put("mogres", Collections.singletonList("Mogre"));
		map.put("basilisks", Arrays.asList("Basilisk", "Basilisk Knight"));
		map.put("terror dogs", Collections.singletonList("Terror Dog"));
		map.put("infernal mages", Collections.singletonList("Infernal Mage"));
		map.put("brine rats", Collections.singletonList("Brine Rat"));
		map.put("jellies", Collections.singletonList("Jelly"));
		map.put("lesser nagua", Arrays.asList("Frost Nagua", "Sulphur Nagua", "Earthen Nagua"));
		map.put("cave horrors", Collections.singletonList("Cave Horror"));
		map.put("bloodveld", Collections.singletonList("Bloodveld"));
		map.put("aberrant spectres", Collections.singletonList("Aberrant Spectre"));
		map.put("dust devils", Collections.singletonList("Dust Devil"));
		map.put("fossil island wyverns", Collections.singletonList("Fossil Island Wyvern"));
		map.put("kurask", Collections.singletonList("Kurask"));
		map.put("skeletal wyverns", Collections.singletonList("Skeletal Wyvern"));
		map.put("gargoyles", Arrays.asList("Gargoyle", "Grotesque Guardians"));
		map.put("custodian stalkers", Collections.singletonList("Custodian Stalker"));
		map.put("aquanites", Collections.singletonList("Aquanite"));
		map.put("nechryael", Collections.singletonList("Nechryael"));
		map.put("spiritual creatures", Arrays.asList("Spiritual Mage", "Spiritual Mage (Zarosian)"));
		map.put("drakes", Collections.singletonList("Drake"));
		map.put("abyssal demons", Arrays.asList("Abyssal Demon", "Abyssal Sire"));
		map.put("cave kraken", Arrays.asList("Cave Kraken", "Kraken"));
		map.put("dark beasts", Collections.singletonList("Dark Beast"));
		map.put("araxytes", Arrays.asList("Araxyte", "Araxxor"));
		map.put("smoke devils", Arrays.asList("Smoke Devil", "Thermonuclear smoke devil"));
		map.put("hydras", Arrays.asList("Hydra", "Alchemical Hydra"));
		map.put("wyrms", Arrays.asList("Wyrm", "Lava Strykewyrm"));
		map.put("turoth", Collections.singletonList("Turoth"));
		map.put("warped creatures", Collections.singletonList("Warped Creature"));
		map.put("vampyres", Collections.singletonList("Vyrewatch Sentinel"));
		map.put("gryphons", Arrays.asList("Gryphon", "Shellbane Gryphon"));

		CREATURE_TO_SOURCES = Collections.unmodifiableMap(map);

		// Build reverse lookup
		Map<String, String> reverseMap = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : map.entrySet())
		{
			for (String sourceName : entry.getValue())
			{
				reverseMap.put(sourceName, entry.getKey());
			}
		}
		SOURCE_TO_CREATURE = Collections.unmodifiableMap(reverseMap);

		// Per-creature Slayer sources where efficiency depends on task assignment.
		// Boss variants (Sire, Kraken, Cerberus, etc.) are NOT task-only.
		Set<String> taskOnly = new HashSet<>();
		taskOnly.add("Crawling Hand");
		taskOnly.add("Cave Crawler");
		taskOnly.add("Rockslug");
		taskOnly.add("Cockatrice");
		taskOnly.add("Pyrefiend");
		taskOnly.add("Mogre");
		taskOnly.add("Basilisk");
		taskOnly.add("Basilisk Knight");
		taskOnly.add("Terror Dog");
		taskOnly.add("Infernal Mage");
		taskOnly.add("Brine Rat");
		taskOnly.add("Jelly");
		taskOnly.add("Frost Nagua");
		taskOnly.add("Sulphur Nagua");
		taskOnly.add("Earthen Nagua");
		taskOnly.add("Cave Horror");
		taskOnly.add("Bloodveld");
		taskOnly.add("Aberrant Spectre");
		taskOnly.add("Dust Devil");
		taskOnly.add("Fossil Island Wyvern");
		taskOnly.add("Kurask");
		taskOnly.add("Skeletal Wyvern");
		taskOnly.add("Gargoyle");
		taskOnly.add("Custodian Stalker");
		taskOnly.add("Aquanite");
		taskOnly.add("Nechryael");
		taskOnly.add("Spiritual Mage");
		taskOnly.add("Spiritual Mage (Zarosian)");
		taskOnly.add("Drake");
		taskOnly.add("Abyssal Demon");
		taskOnly.add("Cave Kraken");
		taskOnly.add("Dark Beast");
		taskOnly.add("Araxyte");
		taskOnly.add("Smoke Devil");
		taskOnly.add("Hydra");
		taskOnly.add("Lava Strykewyrm");
		taskOnly.add("Wyrm");
		taskOnly.add("Turoth");
		taskOnly.add("Warped Creature");
		taskOnly.add("Vyrewatch Sentinel");
		taskOnly.add("Gryphon");
		TASK_ONLY_SOURCES = Collections.unmodifiableSet(taskOnly);
	}

	private SlayerCreatureDatabase()
	{
	}

	/**
	 * Returns the collection log source names associated with a Slayer creature,
	 * or an empty list if no mapping exists.
	 *
	 * @param creatureName the Slayer task creature name (case-insensitive)
	 */
	public static List<String> getSourcesForCreature(String creatureName)
	{
		if (creatureName == null)
		{
			return Collections.emptyList();
		}
		List<String> sources = CREATURE_TO_SOURCES.get(creatureName.toLowerCase());
		return sources != null ? sources : Collections.emptyList();
	}

	/**
	 * Returns true if the given collection log source name is associated
	 * with the given Slayer creature.
	 *
	 * @param creatureName the Slayer task creature name (case-insensitive)
	 * @param sourceName   the collection log source name (exact match)
	 */
	public static boolean isSourceOnTask(String creatureName, String sourceName)
	{
		if (creatureName == null || sourceName == null)
		{
			return false;
		}
		List<String> sources = CREATURE_TO_SOURCES.get(creatureName.toLowerCase());
		return sources != null && sources.contains(sourceName);
	}

	/**
	 * Returns the Slayer task creature name for a given source, or null
	 * if the source has no creature mapping.
	 */
	public static String getCreatureForSource(String sourceName)
	{
		if (sourceName == null)
		{
			return null;
		}
		return SOURCE_TO_CREATURE.get(sourceName);
	}

	/**
	 * Returns true if the source requires an active Slayer task to access.
	 * Boss variants (Abyssal Sire, Kraken, etc.) return false since they
	 * can be fought off-task.
	 */
	public static boolean isTaskOnlySource(String sourceName)
	{
		return sourceName != null && TASK_ONLY_SOURCES.contains(sourceName);
	}
}
