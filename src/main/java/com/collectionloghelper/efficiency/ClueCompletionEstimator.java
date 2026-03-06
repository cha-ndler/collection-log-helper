package com.collectionloghelper.efficiency;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;

@Slf4j
@Singleton
public class ClueCompletionEstimator
{
	public enum ProgressionBucket
	{
		EARLY("Early-game"),
		MID("Mid-game"),
		LATE("Late-game"),
		MAXED("Maxed");

		@Getter
		private final String displayName;

		ProgressionBucket(String displayName)
		{
			this.displayName = displayName;
		}
	}

	// Completion times in seconds: [Beginner, Easy, Medium, Hard, Elite, Master]
	private static final int[] EARLY_TIMES = {120, 300, 420, 600, 900, 1200};
	private static final int[] MID_TIMES = {90, 180, 300, 420, 600, 900};
	private static final int[] LATE_TIMES = {60, 120, 180, 240, 360, 600};
	private static final int[] MAXED_TIMES = {45, 90, 120, 180, 300, 420};

	private final Client client;

	private ProgressionBucket cachedBucket;

	@Inject
	private ClueCompletionEstimator(Client client)
	{
		this.client = client;
	}

	/**
	 * Estimate the completion time in seconds for a clue source based on the
	 * player's account progression.
	 *
	 * @param sourceName the collection log source name, e.g. "Hard Treasure Trails"
	 * @return estimated seconds per clue completion, or -1 if the source is not a clue tier
	 */
	public int estimateCompletionSeconds(String sourceName)
	{
		int tierIndex = getTierIndex(sourceName);
		if (tierIndex < 0)
		{
			return -1;
		}

		ProgressionBucket bucket = getBucket();
		switch (bucket)
		{
			case EARLY:
				return EARLY_TIMES[tierIndex];
			case MID:
				return MID_TIMES[tierIndex];
			case LATE:
				return LATE_TIMES[tierIndex];
			case MAXED:
				return MAXED_TIMES[tierIndex];
			default:
				return LATE_TIMES[tierIndex];
		}
	}

	/**
	 * Get the player's detected progression bucket. Computed once per session
	 * and cached.
	 */
	public ProgressionBucket getBucket()
	{
		if (cachedBucket != null)
		{
			return cachedBucket;
		}
		cachedBucket = computeBucket();
		log.debug("Account progression bucket: {}", cachedBucket.getDisplayName());
		return cachedBucket;
	}

	/**
	 * Clear the cached bucket so it is recomputed on the next call.
	 * Should be called on logout.
	 */
	public void resetBucket()
	{
		cachedBucket = null;
	}

	private ProgressionBucket computeBucket()
	{
		int totalLevel = 0;
		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				continue;
			}
			totalLevel += client.getRealSkillLevel(skill);
		}

		int combatLevel = estimateCombatLevel();
		int questPoints = getQuestPoints();

		// Maxed: total level 2277 (all 99s) or very close, high quest points
		if (totalLevel >= 2200 && questPoints >= 280)
		{
			return ProgressionBucket.MAXED;
		}

		// Late: 80+ average stats, high combat, many quests
		// 80 avg across 23 skills = 1840 total level
		if (totalLevel >= 1800 && combatLevel >= 110 && questPoints >= 200)
		{
			return ProgressionBucket.LATE;
		}

		// Mid: 50-80 average stats, decent combat
		// 50 avg across 23 skills = 1150 total level
		if (totalLevel >= 1000 && combatLevel >= 70)
		{
			return ProgressionBucket.MID;
		}

		return ProgressionBucket.EARLY;
	}

	private int estimateCombatLevel()
	{
		// Simplified combat level estimation using base stats
		int attack = client.getRealSkillLevel(Skill.ATTACK);
		int strength = client.getRealSkillLevel(Skill.STRENGTH);
		int defence = client.getRealSkillLevel(Skill.DEFENCE);
		int hitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
		int prayer = client.getRealSkillLevel(Skill.PRAYER);
		int ranged = client.getRealSkillLevel(Skill.RANGED);
		int magic = client.getRealSkillLevel(Skill.MAGIC);

		double base = 0.25 * (defence + hitpoints + Math.floor(prayer / 2.0));
		double melee = 0.325 * (attack + strength);
		double range = 0.325 * Math.floor(ranged * 1.5);
		double mage = 0.325 * Math.floor(magic * 1.5);

		return (int) (base + Math.max(melee, Math.max(range, mage)));
	}

	private int getQuestPoints()
	{
		// Quest points are stored in varp 101
		return client.getVarpValue(101);
	}

	/**
	 * Map a clue source name to a tier index (0-5).
	 * Returns -1 if the source is not a recognised clue tier.
	 */
	static int getTierIndex(String sourceName)
	{
		if (sourceName == null)
		{
			return -1;
		}
		String lower = sourceName.toLowerCase();
		if (lower.contains("beginner"))
		{
			return 0;
		}
		if (lower.contains("easy"))
		{
			return 1;
		}
		if (lower.contains("medium"))
		{
			return 2;
		}
		if (lower.contains("hard"))
		{
			return 3;
		}
		if (lower.contains("elite"))
		{
			return 4;
		}
		if (lower.contains("master"))
		{
			return 5;
		}
		return -1;
	}

	/**
	 * Format a completion time in seconds to a human-readable string.
	 * e.g. 120 -> "2 min", 90 -> "1 min 30s", 45 -> "45s"
	 */
	public static String formatTime(int seconds)
	{
		if (seconds < 60)
		{
			return seconds + "s";
		}
		int min = seconds / 60;
		int sec = seconds % 60;
		if (sec == 0)
		{
			return min + " min";
		}
		return min + " min " + sec + "s";
	}
}
