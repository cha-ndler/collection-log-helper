/*
 * Copyright (c) 2025, Chandler
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
	private volatile boolean fairyRingAccess = false;

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

		// Cache fairy ring access: requires starting Fairytale II (not just completing it).
		// QuestState.NOT_STARTED means no access; IN_PROGRESS or FINISHED means access.
		try
		{
			fairyRingAccess = Quest.FAIRYTALE_II__CURE_A_QUEEN.getState(client) != QuestState.NOT_STARTED;
		}
		catch (Exception e)
		{
			fairyRingAccess = false;
		}

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
	 * Returns true if the player has access to the fairy ring network
	 * (started Fairytale II - Cure a Queen). Safe to call from EDT.
	 */
	public boolean hasFairyRingAccess()
	{
		return fairyRingAccess;
	}

	/**
	 * Clear the cache (e.g., on logout).
	 */
	public void clearCache()
	{
		accessibilityCache = Collections.emptyMap();
		unmetCache = Collections.emptyMap();
		fairyRingAccess = false;
	}

	/**
	 * Check if the player meets the given requirements. Safe to call from client thread.
	 */
	public boolean meetsRequirements(SourceRequirements requirements)
	{
		if (requirements == null)
		{
			return true;
		}
		return checkRequirementsDetail(requirements).isEmpty();
	}

	private List<String> checkRequirements(CollectionLogSource source)
	{
		SourceRequirements requirements = source.getRequirements();
		if (requirements == null)
		{
			return Collections.emptyList();
		}
		return checkRequirementsDetail(requirements);
	}

	private List<String> checkRequirementsDetail(SourceRequirements requirements)
	{
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
