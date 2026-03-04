package com.collectionloghelper.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

@Slf4j
@Singleton
public class RequirementsChecker
{
	private final Client client;

	private volatile Map<String, Boolean> accessibilityCache = Collections.emptyMap();
	private volatile Map<String, List<String>> unmetCache = Collections.emptyMap();

	@Inject
	private RequirementsChecker(Client client)
	{
		this.client = client;
	}

	/**
	 * Refresh accessibility cache for all sources. Must be called on the client thread.
	 *
	 * @return true if any source's accessibility status changed compared to the previous cache
	 */
	public boolean refreshAccessibility(List<CollectionLogSource> sources)
	{
		Map<String, Boolean> oldAccessibility = accessibilityCache;
		Map<String, Boolean> newAccessibility = new HashMap<>();
		Map<String, List<String>> newUnmet = new HashMap<>();

		for (CollectionLogSource source : sources)
		{
			List<String> unmetRequirements = checkRequirements(source);
			boolean accessible = unmetRequirements.isEmpty();
			newAccessibility.put(source.getName(), accessible);
			if (!accessible)
			{
				newUnmet.put(source.getName(), unmetRequirements);
			}
		}

		accessibilityCache = newAccessibility;
		unmetCache = newUnmet;

		// Check if any source's accessibility status actually changed
		for (Map.Entry<String, Boolean> entry : newAccessibility.entrySet())
		{
			Boolean oldValue = oldAccessibility.get(entry.getKey());
			if (oldValue == null || !oldValue.equals(entry.getValue()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a source is accessible. Safe to call from EDT (reads from cache).
	 */
	public boolean isAccessible(String sourceName)
	{
		return accessibilityCache.getOrDefault(sourceName, true);
	}

	/**
	 * Get cached unmet requirements for a source. Safe to call from EDT.
	 */
	public List<String> getUnmetRequirements(String sourceName)
	{
		return unmetCache.getOrDefault(sourceName, Collections.emptyList());
	}

	/**
	 * Clear the cache (e.g., on logout).
	 */
	public void clearCache()
	{
		accessibilityCache = Collections.emptyMap();
		unmetCache = Collections.emptyMap();
	}

	private List<String> checkRequirements(CollectionLogSource source)
	{
		SourceRequirements requirements = source.getRequirements();
		if (requirements == null)
		{
			return Collections.emptyList();
		}

		List<String> unmet = new ArrayList<>();

		if (requirements.getQuests() != null)
		{
			for (String questName : requirements.getQuests())
			{
				try
				{
					Quest quest = Quest.valueOf(questName);
					if (quest.getState(client) != QuestState.FINISHED)
					{
						unmet.add("Quest: " + formatEnumName(questName));
					}
				}
				catch (IllegalArgumentException e)
				{
					log.warn("Unknown quest enum: {}", questName);
				}
			}
		}

		if (requirements.getSkills() != null)
		{
			for (SkillRequirement skillReq : requirements.getSkills())
			{
				try
				{
					Skill skill = Skill.valueOf(skillReq.getSkill());
					int playerLevel = client.getRealSkillLevel(skill);
					if (playerLevel < skillReq.getLevel())
					{
						unmet.add(formatEnumName(skillReq.getSkill()) + " level " + skillReq.getLevel()
							+ " (current: " + playerLevel + ")");
					}
				}
				catch (IllegalArgumentException e)
				{
					log.warn("Unknown skill enum: {}", skillReq.getSkill());
				}
			}
		}

		return unmet;
	}

	static String formatEnumName(String enumName)
	{
		String[] parts = enumName.split("_");
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String part : parts)
		{
			// split("_") produces empty tokens for consecutive or trailing underscores
			if (part.isEmpty())
			{
				continue;
			}
			if (!first)
			{
				sb.append(' ');
			}
			first = false;
			if (part.matches("[IVX]+"))
			{
				// Roman numerals stay uppercase
				sb.append(part);
			}
			else
			{
				sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
			}
		}
		return sb.toString();
	}
}
