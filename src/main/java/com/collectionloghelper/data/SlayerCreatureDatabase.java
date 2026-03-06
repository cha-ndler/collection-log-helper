package com.collectionloghelper.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Slayer task creature names to collection log source names
 * that are relevant (typically boss variants killable on that task).
 * <p>
 * Source names must match exactly those in drop_rates.json.
 */
public final class SlayerCreatureDatabase
{
	private static final Map<String, List<String>> CREATURE_TO_SOURCES;

	static
	{
		Map<String, List<String>> map = new HashMap<>();

		map.put("abyssal demons", Collections.singletonList("Abyssal Sire"));
		map.put("black demons", Arrays.asList("Demonic gorillas", "Skotizo"));
		map.put("greater demons", Collections.singletonList("K'ril Tsutsaroth"));
		map.put("aviansie", Collections.singletonList("Kree'arra"));
		map.put("kalphite", Collections.singletonList("Kalphite Queen"));
		map.put("gargoyles", Collections.singletonList("Grotesque Guardians"));
		map.put("smoke devils", Collections.singletonList("Thermonuclear smoke devil"));
		map.put("hellhounds", Collections.singletonList("Cerberus"));
		map.put("dagannoth", Arrays.asList("Dagannoth Rex", "Dagannoth Prime", "Dagannoth Supreme"));
		map.put("cave kraken", Collections.singletonList("Kraken"));
		map.put("hydras", Collections.singletonList("Alchemical Hydra"));

		CREATURE_TO_SOURCES = Collections.unmodifiableMap(map);
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
}
