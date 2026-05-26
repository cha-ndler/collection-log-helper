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
package com.collectionloghelper.data;

import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.PohTeleport;
import com.collectionloghelper.player.PohTeleportInventory;
import java.awt.Color;
import java.lang.reflect.Field;
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
import net.runelite.api.Varbits;

@Slf4j
@Singleton
public class RequirementsChecker
{
	private final Client client;
	private final PohTeleportInventory pohTeleportInventory;
	private final EquippedItemState equippedItemState;
	private final PlayerInventoryState playerInventoryState;

	private volatile Map<String, Boolean> accessibilityCache = Collections.emptyMap();
	private volatile Map<String, List<String>> unmetCache = Collections.emptyMap();
	private volatile boolean fairyRingAccess = false;

	@Inject
	private RequirementsChecker(Client client,
		PohTeleportInventory pohTeleportInventory,
		EquippedItemState equippedItemState,
		PlayerInventoryState playerInventoryState)
	{
		this.client = client;
		this.pohTeleportInventory = pohTeleportInventory;
		this.equippedItemState = equippedItemState;
		this.playerInventoryState = playerInventoryState;
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

	/**
	 * Build a display-ready list of {@link RequirementRow}s for the given source.
	 * Must be called on the client thread (reads live client state).
	 * Returns an empty list when the source has no requirements.
	 */
	public List<RequirementRow> buildRequirementRows(CollectionLogSource source)
	{
		if (source == null)
		{
			return Collections.emptyList();
		}
		SourceRequirements requirements = source.getRequirements();
		if (requirements == null)
		{
			return Collections.emptyList();
		}

		List<RequirementRow> rows = new ArrayList<>();

		// --- Quest rows ---
		if (requirements.getQuests() != null)
		{
			for (String questName : requirements.getQuests())
			{
				try
				{
					Quest quest = Quest.valueOf(questName);
					QuestState state = quest.getState(client);
					String label = formatEnumName(questName);
					String stateText;
					Color color;
					boolean met;
					switch (state)
					{
						case FINISHED:
							stateText = "COMPLETED";
							color = RequirementRow.COLOR_MET;
							met = true;
							break;
						case IN_PROGRESS:
							stateText = "IN PROGRESS";
							color = RequirementRow.COLOR_IN_PROGRESS;
							met = false;
							break;
						default:
							stateText = "NOT STARTED";
							color = RequirementRow.COLOR_UNMET;
							met = false;
							break;
					}
					rows.add(new RequirementRow(RequirementRow.Category.QUEST, label, stateText, color, met));
				}
				catch (IllegalArgumentException e)
				{
					log.warn("Unknown quest enum for requirement row: {}", questName);
				}
			}
		}

		// --- Skill rows ---
		if (requirements.getSkills() != null)
		{
			for (SkillRequirement skillReq : requirements.getSkills())
			{
				try
				{
					Skill skill = Skill.valueOf(skillReq.getSkill());
					int playerLevel = client.getRealSkillLevel(skill);
					int required = skillReq.getLevel();
					String label = formatEnumName(skillReq.getSkill()) + " " + required;
					String stateText = "level " + playerLevel + "/" + required;
					boolean met = playerLevel >= required;
					Color color = met ? RequirementRow.COLOR_MET : RequirementRow.COLOR_UNMET;
					rows.add(new RequirementRow(RequirementRow.Category.SKILL, label, stateText, color, met));
				}
				catch (IllegalArgumentException e)
				{
					log.warn("Unknown skill enum for requirement row: {}", skillReq.getSkill());
				}
			}
		}

		// --- Diary rows ---
		if (requirements.getDiaries() != null)
		{
			for (String diaryName : requirements.getDiaries())
			{
				int varbitId = resolveDiaryVarbit(diaryName);
				String label = formatEnumName(diaryName);
				boolean met;
				if (varbitId < 0)
				{
					// Unrecognised diary — treat as unmet and warn
					log.warn("Unknown diary requirement: {}", diaryName);
					met = false;
				}
				else
				{
					met = client.getVarbitValue(varbitId) == 1;
				}
				String stateText = met ? "COMPLETED" : "NOT COMPLETED";
				Color color = met ? RequirementRow.COLOR_MET : RequirementRow.COLOR_UNMET;
				rows.add(new RequirementRow(RequirementRow.Category.DIARY, label, stateText, color, met));
			}
		}

		return Collections.unmodifiableList(rows);
	}

	/**
	 * Resolves a diary requirement string such as {@code "LUMBRIDGE_HARD"} to the
	 * matching {@link Varbits} constant using reflection on the {@code DIARY_<name>}
	 * field naming convention. Returns {@code -1} if the constant is not found.
	 */
	static int resolveDiaryVarbit(String diaryName)
	{
		if (diaryName == null || diaryName.isEmpty())
		{
			return -1;
		}
		String fieldName = "DIARY_" + diaryName.toUpperCase();
		try
		{
			Field f = Varbits.class.getField(fieldName);
			return f.getInt(null);
		}
		catch (NoSuchFieldException | IllegalAccessException e)
		{
			return -1;
		}
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

		if (requirements.getPohTeleports() != null)
		{
			for (String teleportName : requirements.getPohTeleports())
			{
				try
				{
					PohTeleport teleport = PohTeleport.valueOf(teleportName);
					if (!pohTeleportInventory.hasTeleport(teleport))
					{
						unmet.add("POH teleport: " + formatEnumName(teleportName));
					}
				}
				catch (IllegalArgumentException e)
				{
					log.warn("Unknown POH teleport enum: {}", teleportName);
					unmet.add("POH teleport: " + formatEnumName(teleportName));
				}
			}
		}

		if (requirements.getEquippedItemIds() != null)
		{
			for (Integer itemId : requirements.getEquippedItemIds())
			{
				if (itemId == null)
				{
					continue;
				}
				if (!equippedItemState.hasEquipped(itemId))
				{
					unmet.add("Equipped item: " + itemId);
				}
			}
		}

		if (requirements.getInventoryItemIds() != null)
		{
			for (Integer itemId : requirements.getInventoryItemIds())
			{
				if (itemId == null)
				{
					continue;
				}
				if (!playerInventoryState.hasItem(itemId))
				{
					unmet.add("Inventory item: " + itemId);
				}
			}
		}

		if (requirements.getInventoryItemIdsAny() != null
			&& !requirements.getInventoryItemIdsAny().isEmpty())
		{
			boolean anyHeld = false;
			boolean sawNonNull = false;
			for (Integer itemId : requirements.getInventoryItemIdsAny())
			{
				if (itemId == null)
				{
					continue;
				}
				sawNonNull = true;
				if (playerInventoryState.hasItem(itemId))
				{
					anyHeld = true;
					break;
				}
			}
			// An all-null list collapses to "no real entries", which matches the
			// vacuous-empty contract on the AND-semantic siblings: skip the check.
			if (sawNonNull && !anyHeld)
			{
				unmet.add("Inventory item (any of): "
					+ requirements.getInventoryItemIdsAny());
			}
		}

		if (requirements.getQuestMilestones() != null)
		{
			for (String questName : requirements.getQuestMilestones())
			{
				try
				{
					Quest quest = Quest.valueOf(questName);
					// Looser than the quests field: STARTED is enough. Met when
					// IN_PROGRESS or FINISHED, i.e. anything other than NOT_STARTED.
					if (quest.getState(client) == QuestState.NOT_STARTED)
					{
						unmet.add("Quest started: " + formatEnumName(questName));
					}
				}
				catch (IllegalArgumentException e)
				{
					// Fail closed: an unverifiable requirement must not silently
					// promise a shortcut the player may not qualify for.
					log.warn("Unknown quest enum: {}", questName);
					unmet.add("Quest started: " + formatEnumName(questName));
				}
			}
		}

		if (requirements.getSkillCapePerks() != null)
		{
			for (String skillName : requirements.getSkillCapePerks())
			{
				try
				{
					Skill skill = Skill.valueOf(skillName);
					// Level-99 proxy: owning the skill cape (and its travel perk)
					// requires level 99 in the named skill.
					if (client.getRealSkillLevel(skill) < 99)
					{
						unmet.add("Skill cape: " + formatEnumName(skillName));
					}
				}
				catch (IllegalArgumentException e)
				{
					// Fail closed: an unverifiable requirement must not silently
					// promise a shortcut the player may not qualify for.
					log.warn("Unknown skill enum: {}", skillName);
					unmet.add("Skill cape: " + formatEnumName(skillName));
				}
			}
		}

		return unmet;
	}

	/**
	 * Overrides for quest/source enum names that naive title-casing mis-formats.
	 *
	 * <p>Covers two classes of problem:
	 * <ul>
	 *   <li>Connector words ("of", "the", "and", "in", "a", "to") that should be
	 *       lowercase in the middle of a name.</li>
	 *   <li>Names containing apostrophes or other punctuation that is lost when
	 *       the enum constant was defined (e.g. {@code SHADES_OF_MORTTON} →
	 *       {@code "Shades of Mort'ton"}).</li>
	 * </ul>
	 *
	 * <p>Keys are the raw enum-constant strings (uppercase, underscores).
	 * Values are the exact display strings to use instead of the generated one.
	 */
	private static final java.util.Map<String, String> ENUM_NAME_OVERRIDES;

	static
	{
		java.util.Map<String, String> m = new java.util.HashMap<>();
		// Apostrophe names — naive split loses the punctuation
		m.put("SHADES_OF_MORTTON", "Shades of Mort'ton");
		m.put("ICTHLARIN_S_LITTLE_HELPER", "Icthlarin's Little Helper");
		m.put("ENAKHRA_S_LAMENT", "Enakhra's Lament");
		m.put("RATCATCHERS", "Ratcatchers"); // no change needed, just guard
		ENUM_NAME_OVERRIDES = java.util.Collections.unmodifiableMap(m);
	}

	/**
	 * Words that should remain lowercase when appearing in the middle of a
	 * formatted name (English connector/preposition/article set).
	 */
	private static final java.util.Set<String> LOWERCASE_CONNECTORS =
		java.util.Collections.unmodifiableSet(new java.util.HashSet<>(java.util.Arrays.asList(
			"of", "the", "and", "in", "a", "an", "to", "for", "at", "by", "or"
		)));

	static String formatEnumName(String enumName)
	{
		// Check the override map first — handles apostrophes and other
		// punctuation that can't be reconstructed from the enum constant name.
		String override = ENUM_NAME_OVERRIDES.get(enumName);
		if (override != null)
		{
			return override;
		}

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
			String lower = part.toLowerCase();
			if (first)
			{
				// Always capitalise the first word
				sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
			}
			else if (part.matches("[IVX]+"))
			{
				// Roman numerals stay uppercase
				sb.append(part);
			}
			else if (LOWERCASE_CONNECTORS.contains(lower))
			{
				// Connector words stay lowercase mid-name
				sb.append(lower);
			}
			else
			{
				sb.append(part.charAt(0)).append(part.substring(1).toLowerCase());
			}
			first = false;
		}
		return sb.toString();
	}
}
