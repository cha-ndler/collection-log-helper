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
		map.put("brine rats", Collections.singletonList("Brine Rat"));
		map.put("cave horrors", Collections.singletonList("Cave Horror"));
		map.put("basilisk knights", Collections.singletonList("Basilisk Knight"));
		map.put("bloodveld", Collections.singletonList("Bloodveld"));
		map.put("aberrant spectres", Collections.singletonList("Aberrant Spectre"));
		map.put("dust devils", Collections.singletonList("Dust Devil"));
		map.put("fossil island wyverns", Collections.singletonList("Fossil Island Wyvern"));
		map.put("kurask", Collections.singletonList("Kurask"));
		map.put("skeletal wyverns", Collections.singletonList("Skeletal Wyvern"));
		map.put("gargoyles", Arrays.asList("Gargoyle", "Grotesque Guardians"));
		map.put("nechryael", Collections.singletonList("Nechryael"));
		map.put("spiritual creatures", Collections.singletonList("Spiritual Mage"));
		map.put("drakes", Collections.singletonList("Drake"));
		map.put("abyssal demons", Arrays.asList("Abyssal Demon", "Abyssal Sire"));
		map.put("cave kraken", Arrays.asList("Cave Kraken", "Kraken"));
		map.put("dark beasts", Collections.singletonList("Dark Beast"));
		map.put("araxytes", Arrays.asList("Araxyte", "Araxxor"));
		map.put("smoke devils", Arrays.asList("Smoke Devil", "Thermonuclear smoke devil"));
		map.put("hydras", Arrays.asList("Hydra", "Alchemical Hydra"));
		map.put("wyrms", Collections.singletonList("Wyrm"));
		map.put("turoth", Collections.singletonList("Turoth"));
		map.put("warped creatures", Collections.singletonList("Warped Creature"));
		map.put("vampyres", Collections.singletonList("Vyrewatch Sentinel"));
		map.put("gryphons", Arrays.asList("Gryphon", "Shellbane Gryphon"));

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
