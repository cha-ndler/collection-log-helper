package com.collectionloghelper.data;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.InventoryID;

/**
 * Tracks the player's available teleport and travel methods based on
 * quest state, varbits, and bank contents. Updated by the plugin at
 * appropriate times (login, varbit change, bank open) — no @Subscribe
 * annotations here.
 */
@Slf4j
@Singleton
public class PlayerTravelCapabilities
{
	// --- Varbit IDs ---
	private static final int VARBIT_ACTIVE_SPELLBOOK = 4070;
	private static final int VARBIT_POH_LOCATION = 2187;
	private static final int VARBIT_LUMBRIDGE_ELITE_DIARY = 4498;

	// --- Spellbook constants ---
	public static final int SPELLBOOK_STANDARD = 0;
	public static final int SPELLBOOK_ANCIENT = 1;
	public static final int SPELLBOOK_LUNAR = 2;
	public static final int SPELLBOOK_ARCEUUS = 3;

	// --- Bank item IDs ---

	/** Games necklace (8) through Games necklace (1). */
	private static final int GAMES_NECKLACE_MIN = 3853;
	private static final int GAMES_NECKLACE_MAX = 3867;

	/** Ring of dueling (8) through Ring of dueling (1). */
	private static final int RING_OF_DUELING_MIN = 2552;
	private static final int RING_OF_DUELING_MAX = 2566;

	/** Amulet of glory (6) through Amulet of glory (1), including (t) variants. */
	private static final int GLORY_MIN = 1704;
	private static final int GLORY_MAX = 1712;

	/** Slayer ring (8) through Slayer ring (1). */
	private static final int SLAYER_RING_MIN = 11866;
	private static final int SLAYER_RING_MAX = 11873;

	/** Dramen staff. */
	private static final int DRAMEN_STAFF = 772;

	/** Xeric's talisman (charged). */
	private static final int XERICS_TALISMAN = 13393;

	/** Digsite pendant (5) through Digsite pendant (1). */
	private static final int DIGSITE_PENDANT_MIN = 11190;
	private static final int DIGSITE_PENDANT_MAX = 11194;

	/** Royal seed pod. */
	private static final int ROYAL_SEED_POD = 19564;

	private final Client client;

	// --- Quest-based capabilities ---
	@Getter
	private volatile boolean fairyRings;
	@Getter
	private volatile boolean staffFreeFairyRings;
	@Getter
	private volatile boolean spiritTrees;
	@Getter
	private volatile boolean lunarSpellbook;
	@Getter
	private volatile boolean ancientSpellbook;

	// --- Varbit-based capabilities ---
	@Getter
	private volatile int activeSpellbook;
	@Getter
	private volatile int pohLocation;

	// --- Bank-based capabilities ---
	@Getter
	private volatile boolean gamesNecklace;
	@Getter
	private volatile boolean ringOfDueling;
	@Getter
	private volatile boolean glory;
	@Getter
	private volatile boolean slayerRing;
	@Getter
	private volatile boolean dramenStaff;
	@Getter
	private volatile boolean xericsTalisman;
	@Getter
	private volatile boolean digsitePendant;
	@Getter
	private volatile boolean royalSeedPod;

	@Inject
	private PlayerTravelCapabilities(Client client)
	{
		this.client = client;
	}

	/**
	 * Refresh quest-based travel capabilities. Must be called on the client thread
	 * (e.g., after login or profile change).
	 */
	public void refreshQuestState()
	{
		fairyRings = questNotStarted(Quest.FAIRYTALE_II__CURE_A_QUEEN, false);
		spiritTrees = questFinished(Quest.TREE_GNOME_VILLAGE) && questFinished(Quest.THE_GRAND_TREE);
		lunarSpellbook = questFinished(Quest.LUNAR_DIPLOMACY);
		ancientSpellbook = questFinished(Quest.DESERT_TREASURE_I);

		log.debug("Travel quest state refreshed: fairyRings={}, spiritTrees={}, lunar={}, ancient={}",
			fairyRings, spiritTrees, lunarSpellbook, ancientSpellbook);
	}

	/**
	 * Refresh varbit-based travel capabilities. Must be called on the client thread
	 * (e.g., on VarbitChanged events).
	 */
	public void refreshVarbits()
	{
		try
		{
			activeSpellbook = client.getVarbitValue(VARBIT_ACTIVE_SPELLBOOK);
			pohLocation = client.getVarbitValue(VARBIT_POH_LOCATION);
			staffFreeFairyRings = client.getVarbitValue(VARBIT_LUMBRIDGE_ELITE_DIARY) == 1;
		}
		catch (Exception e)
		{
			log.warn("Failed to read travel varbits", e);
		}
	}

	/**
	 * Scan the bank container for teleport items. Call when the bank is opened
	 * and ItemContainerChanged fires for the bank.
	 *
	 * @param bankContainer the bank's ItemContainer
	 */
	public void scanBank(ItemContainer bankContainer)
	{
		// Reset bank-based flags
		gamesNecklace = false;
		ringOfDueling = false;
		glory = false;
		slayerRing = false;
		dramenStaff = false;
		xericsTalisman = false;
		digsitePendant = false;
		royalSeedPod = false;

		if (bankContainer == null)
		{
			return;
		}

		Item[] items = bankContainer.getItems();
		if (items == null)
		{
			return;
		}

		for (Item item : items)
		{
			int id = item.getId();
			if (id <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}

			checkItemId(id);
		}

		// Also check inventory and equipment for dramen staff
		checkContainerForDramen(InventoryID.INV);
		checkContainerForDramen(InventoryID.WORN);

		log.debug("Travel bank scan: games={}, dueling={}, glory={}, slayer={}, dramen={}, " +
				"xerics={}, digsite={}, seedpod={}",
			gamesNecklace, ringOfDueling, glory, slayerRing, dramenStaff,
			xericsTalisman, digsitePendant, royalSeedPod);
	}

	/**
	 * Check a single item ID and set the corresponding flag if it matches
	 * a known teleport item.
	 */
	private void checkItemId(int id)
	{
		if (id >= GAMES_NECKLACE_MIN && id <= GAMES_NECKLACE_MAX)
		{
			gamesNecklace = true;
		}
		else if (id >= RING_OF_DUELING_MIN && id <= RING_OF_DUELING_MAX)
		{
			ringOfDueling = true;
		}
		else if (id >= GLORY_MIN && id <= GLORY_MAX)
		{
			glory = true;
		}
		else if (id >= SLAYER_RING_MIN && id <= SLAYER_RING_MAX)
		{
			slayerRing = true;
		}
		else if (id == DRAMEN_STAFF)
		{
			dramenStaff = true;
		}
		else if (id == XERICS_TALISMAN)
		{
			xericsTalisman = true;
		}
		else if (id >= DIGSITE_PENDANT_MIN && id <= DIGSITE_PENDANT_MAX)
		{
			digsitePendant = true;
		}
		else if (id == ROYAL_SEED_POD)
		{
			royalSeedPod = true;
		}
	}

	/**
	 * Check a non-bank container (inventory or equipment) for the dramen staff.
	 */
	private void checkContainerForDramen(int containerId)
	{
		if (dramenStaff)
		{
			return; // already found
		}

		try
		{
			ItemContainer container = client.getItemContainer(containerId);
			if (container == null)
			{
				return;
			}
			Item[] items = container.getItems();
			if (items == null)
			{
				return;
			}
			for (Item item : items)
			{
				if (item.getId() == DRAMEN_STAFF && item.getQuantity() > 0)
				{
					dramenStaff = true;
					return;
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Failed to check container {} for dramen staff", containerId, e);
		}
	}

	/**
	 * Returns true if the player can currently use fairy rings (quest started + has
	 * dramen staff or Lumbridge Elite diary completed for staff-free access).
	 */
	public boolean canUseFairyRings()
	{
		return fairyRings && (staffFreeFairyRings || dramenStaff);
	}

	/**
	 * Returns true if the player's currently active spellbook is Lunar.
	 */
	public boolean isOnLunarSpellbook()
	{
		return activeSpellbook == SPELLBOOK_LUNAR;
	}

	/**
	 * Returns true if the player's currently active spellbook is Ancient.
	 */
	public boolean isOnAncientSpellbook()
	{
		return activeSpellbook == SPELLBOOK_ANCIENT;
	}

	/**
	 * Returns true if the player's currently active spellbook is Standard.
	 */
	public boolean isOnStandardSpellbook()
	{
		return activeSpellbook == SPELLBOOK_STANDARD;
	}

	/**
	 * Returns true if the player's currently active spellbook is Arceuus.
	 */
	public boolean isOnArceuusSpellbook()
	{
		return activeSpellbook == SPELLBOOK_ARCEUUS;
	}

	/**
	 * Returns true if the player has a POH (location varbit > 0).
	 */
	public boolean hasPoh()
	{
		return pohLocation > 0;
	}

	/**
	 * Selects the best travel option from a travelTip string based on the player's
	 * current capabilities. Options in the tip are assumed to be ordered by priority
	 * (best first), so the first available option is returned.
	 *
	 * @param travelTip the raw travel tip string, with options separated by ", or " or " or "
	 * @return the best available option, or the full original string if none match
	 */
	public String selectBestTravelTip(String travelTip)
	{
		if (travelTip == null || travelTip.isEmpty())
		{
			return travelTip;
		}

		// Split on ", or " first, then " or " for remaining segments
		String[] options = travelTip.split(",?\\s+or\\s+");

		if (options.length <= 1)
		{
			return travelTip;
		}

		for (String option : options)
		{
			String trimmed = option.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			if (isOptionAvailable(trimmed))
			{
				return trimmed;
			}
		}

		// No options matched — return the full original as fallback
		return travelTip;
	}

	/**
	 * Checks whether a single travel option is available to the player based on
	 * keyword matching against current capabilities.
	 */
	private boolean isOptionAvailable(String option)
	{
		String lower = option.toLowerCase();

		if (lower.contains("fairy ring"))
		{
			return canUseFairyRings();
		}
		if (lower.contains("spirit tree"))
		{
			return isSpiritTrees();
		}
		if (lower.contains("lunar") || lower.contains("moonclan"))
		{
			return isOnLunarSpellbook();
		}
		if (lower.contains("ancient") || lower.contains("lassar") || lower.contains("paddewwa"))
		{
			return isOnAncientSpellbook();
		}
		if (lower.contains("arceuus") || lower.contains("barrows teleport") || lower.contains("mort'ton teleport"))
		{
			return isOnArceuusSpellbook();
		}
		if (lower.contains("games necklace"))
		{
			return isGamesNecklace();
		}
		if (lower.contains("ring of dueling"))
		{
			return isRingOfDueling();
		}
		if (lower.contains("glory") || lower.contains("amulet of glory"))
		{
			return isGlory();
		}
		if (lower.contains("slayer ring"))
		{
			return isSlayerRing();
		}
		if (lower.contains("xeric"))
		{
			return isXericsTalisman();
		}
		if (lower.contains("digsite pendant"))
		{
			return isDigsitePendant();
		}
		if (lower.contains("royal seed pod"))
		{
			return isRoyalSeedPod();
		}
		if (lower.contains("poh") || lower.contains("house teleport"))
		{
			return hasPoh();
		}
		if (lower.contains("minigame teleport") || lower.contains("walk") || lower.contains("run"))
		{
			return true;
		}

		// No keyword match — assume available
		return true;
	}

	/**
	 * Clears all cached state (e.g., on logout).
	 */
	public void reset()
	{
		fairyRings = false;
		staffFreeFairyRings = false;
		spiritTrees = false;
		lunarSpellbook = false;
		ancientSpellbook = false;
		activeSpellbook = 0;
		pohLocation = 0;
		gamesNecklace = false;
		ringOfDueling = false;
		glory = false;
		slayerRing = false;
		dramenStaff = false;
		xericsTalisman = false;
		digsitePendant = false;
		royalSeedPod = false;
	}

	/**
	 * Returns a human-readable summary of available transport methods.
	 */
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		appendIf(sb, "Fairy rings", canUseFairyRings());
		appendIf(sb, "Spirit trees", spiritTrees);
		appendIf(sb, "Games necklace", gamesNecklace);
		appendIf(sb, "Ring of dueling", ringOfDueling);
		appendIf(sb, "Glory", glory);
		appendIf(sb, "Slayer ring", slayerRing);
		appendIf(sb, "Xeric's talisman", xericsTalisman);
		appendIf(sb, "Digsite pendant", digsitePendant);
		appendIf(sb, "Royal seed pod", royalSeedPod);
		appendIf(sb, "POH", hasPoh());

		if (sb.length() == 0)
		{
			return "No teleport methods detected";
		}
		return sb.toString();
	}

	private void appendIf(StringBuilder sb, String label, boolean available)
	{
		if (available)
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(label);
		}
	}

	// --- Private quest helpers ---

	/**
	 * Returns true if the given quest is FINISHED.
	 */
	private boolean questFinished(Quest quest)
	{
		try
		{
			return quest.getState(client) == QuestState.FINISHED;
		}
		catch (Exception e)
		{
			log.warn("Failed to check quest state for {}", quest, e);
			return false;
		}
	}

	/**
	 * For fairy rings: returns true if the quest is NOT in NOT_STARTED state
	 * (i.e., IN_PROGRESS or FINISHED both grant access).
	 *
	 * @param quest          the quest to check
	 * @param defaultValue   value to return if the check fails
	 */
	private boolean questNotStarted(Quest quest, boolean defaultValue)
	{
		try
		{
			return quest.getState(client) != QuestState.NOT_STARTED;
		}
		catch (Exception e)
		{
			log.warn("Failed to check quest state for {}", quest, e);
			return defaultValue;
		}
	}
}
