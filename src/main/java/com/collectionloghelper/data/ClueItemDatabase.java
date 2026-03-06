package com.collectionloghelper.data;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * Static database mapping OSRS item IDs to clue tiers and types.
 * Covers reward caskets, clue bottles (fishing), clue geodes (mining),
 * and clue nests (woodcutting/birdhouses).
 *
 * Item IDs sourced from RuneLite gameval ItemID constants and the OSRS Wiki.
 * Ref: https://github.com/runelite/runelite (net.runelite.api.gameval.ItemID)
 * Ref: https://oldschool.runescape.wiki/w/Clue_scroll
 */
public class ClueItemDatabase
{
	public enum ClueTier
	{
		BEGINNER("Beginner"),
		EASY("Easy"),
		MEDIUM("Medium"),
		HARD("Hard"),
		ELITE("Elite"),
		MASTER("Master");

		private final String displayName;

		ClueTier(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	public enum ClueItemType
	{
		SCROLL("Clue scroll"),
		CASKET("Reward casket"),
		BOTTLE("Clue bottle"),
		GEODE("Clue geode"),
		NEST("Clue nest");

		private final String displayName;

		ClueItemType(String displayName)
		{
			this.displayName = displayName;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	private static final class ClueItemInfo
	{
		final ClueTier tier;
		final ClueItemType type;

		ClueItemInfo(ClueTier tier, ClueItemType type)
		{
			this.tier = tier;
			this.type = type;
		}
	}

	private static final Map<Integer, ClueItemInfo> CLUE_ITEMS = new HashMap<>();

	static
	{
		// Clue scrolls (unstarted) — beginner and master have single IDs.
		// Easy/medium/hard/elite have hundreds of step-specific IDs that are
		// not practical to enumerate here; those are identified at scan time
		// via item name matching in PlayerBankState.
		register(ItemID.TRAIL_CLUE_BEGINNER, ClueTier.BEGINNER, ClueItemType.SCROLL);
		register(ItemID.TRAIL_CLUE_MASTER, ClueTier.MASTER, ClueItemType.SCROLL);

		// Reward caskets (unopened clue rewards)
		register(ItemID.TRAIL_REWARD_CASKET_BEGINNER, ClueTier.BEGINNER, ClueItemType.CASKET);
		register(ItemID.TRAIL_REWARD_CASKET_EASY, ClueTier.EASY, ClueItemType.CASKET);
		register(ItemID.TRAIL_REWARD_CASKET_MEDIUM, ClueTier.MEDIUM, ClueItemType.CASKET);
		register(ItemID.TRAIL_REWARD_CASKET_HARD, ClueTier.HARD, ClueItemType.CASKET);
		register(ItemID.TRAIL_REWARD_CASKET_ELITE, ClueTier.ELITE, ClueItemType.CASKET);
		register(ItemID.TRAIL_REWARD_CASKET_MASTER, ClueTier.MASTER, ClueItemType.CASKET);

		// Clue bottles (from fishing)
		register(ItemID.FISHING_CLUE_BOTTLE_BEGINNER, ClueTier.BEGINNER, ClueItemType.BOTTLE);
		register(ItemID.FISHING_CLUE_BOTTLE_EASY, ClueTier.EASY, ClueItemType.BOTTLE);
		register(ItemID.FISHING_CLUE_BOTTLE_MEDIUM, ClueTier.MEDIUM, ClueItemType.BOTTLE);
		register(ItemID.FISHING_CLUE_BOTTLE_HARD, ClueTier.HARD, ClueItemType.BOTTLE);
		register(ItemID.FISHING_CLUE_BOTTLE_ELITE, ClueTier.ELITE, ClueItemType.BOTTLE);

		// Clue geodes (from mining)
		register(ItemID.MINING_CLUE_GEODE_BEGINNER, ClueTier.BEGINNER, ClueItemType.GEODE);
		register(ItemID.MINING_CLUE_GEODE_EASY, ClueTier.EASY, ClueItemType.GEODE);
		register(ItemID.MINING_CLUE_GEODE_MEDIUM, ClueTier.MEDIUM, ClueItemType.GEODE);
		register(ItemID.MINING_CLUE_GEODE_HARD, ClueTier.HARD, ClueItemType.GEODE);
		register(ItemID.MINING_CLUE_GEODE_ELITE, ClueTier.ELITE, ClueItemType.GEODE);

		// Clue nests (from woodcutting / birdhouses)
		register(ItemID.WC_CLUE_NEST_BEGINNER, ClueTier.BEGINNER, ClueItemType.NEST);
		register(ItemID.WC_CLUE_NEST_EASY, ClueTier.EASY, ClueItemType.NEST);
		register(ItemID.WC_CLUE_NEST_MEDIUM, ClueTier.MEDIUM, ClueItemType.NEST);
		register(ItemID.WC_CLUE_NEST_HARD, ClueTier.HARD, ClueItemType.NEST);
		register(ItemID.WC_CLUE_NEST_ELITE, ClueTier.ELITE, ClueItemType.NEST);
	}

	private static void register(int itemId, ClueTier tier, ClueItemType type)
	{
		CLUE_ITEMS.put(itemId, new ClueItemInfo(tier, type));
	}

	/**
	 * Returns the clue tier for an item ID, or null if not a known clue item.
	 */
	public static ClueTier getTier(int itemId)
	{
		ClueItemInfo info = CLUE_ITEMS.get(itemId);
		return info != null ? info.tier : null;
	}

	/**
	 * Returns the clue item type for an item ID, or null if not a known clue item.
	 */
	public static ClueItemType getType(int itemId)
	{
		ClueItemInfo info = CLUE_ITEMS.get(itemId);
		return info != null ? info.type : null;
	}

	/**
	 * Returns true if this item is a known clue-related item (casket, bottle, geode, nest, or scroll).
	 */
	public static boolean isClueItem(int itemId)
	{
		return CLUE_ITEMS.containsKey(itemId);
	}

	/**
	 * Returns true if this item is an unopened reward casket.
	 */
	public static boolean isCasket(int itemId)
	{
		ClueItemInfo info = CLUE_ITEMS.get(itemId);
		return info != null && info.type == ClueItemType.CASKET;
	}

	/**
	 * Attempts to determine the clue tier from an item name string.
	 * Used for items with many step-specific IDs (easy/medium/hard/elite clue scrolls)
	 * that cannot be practically enumerated in the static map.
	 *
	 * @param itemName the item's name (e.g., "Clue scroll (easy)")
	 * @return the tier, or null if the name does not match a clue scroll pattern
	 */
	public static ClueTier getTierFromName(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}
		String lower = itemName.toLowerCase();
		if (!lower.startsWith("clue scroll"))
		{
			return null;
		}
		if (lower.contains("beginner"))
		{
			return ClueTier.BEGINNER;
		}
		if (lower.contains("easy"))
		{
			return ClueTier.EASY;
		}
		if (lower.contains("medium"))
		{
			return ClueTier.MEDIUM;
		}
		if (lower.contains("hard"))
		{
			return ClueTier.HARD;
		}
		if (lower.contains("elite"))
		{
			return ClueTier.ELITE;
		}
		if (lower.contains("master"))
		{
			return ClueTier.MASTER;
		}
		return null;
	}
}
