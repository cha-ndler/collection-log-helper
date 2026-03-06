package com.collectionloghelper.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks the player's current Slayer task state using RuneLite VarPlayer/Varbit APIs.
 * <p>
 * The creature name is resolved from the SLAYER_TARGET VarPlayer, which encodes a
 * task ID. The task ID is mapped to a creature name via the {@link SlayerTask} enum.
 * <p>
 * Reference: RuneLite SlayerPlugin.java
 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java
 */
@Slf4j
@Singleton
public class SlayerTaskState
{
	private final Client client;

	private volatile String creatureName;
	private volatile int remaining;
	private volatile boolean taskActive;

	@Inject
	private SlayerTaskState(Client client)
	{
		this.client = client;
	}

	/**
	 * Refresh the Slayer task state from varps/varbits.
	 * Must be called on the client thread.
	 */
	public void refresh()
	{
		int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		int count = client.getVarpValue(VarPlayerID.SLAYER_COUNT);

		if (taskId <= 0 || count <= 0)
		{
			if (taskActive)
			{
				log.debug("Slayer task completed or cleared (taskId={}, count={})", taskId, count);
			}
			creatureName = null;
			remaining = 0;
			taskActive = false;
			return;
		}

		String resolved = SlayerTask.forId(taskId);
		if (resolved == null)
		{
			// Unknown task ID — clear state rather than showing stale data
			log.debug("Unknown slayer task ID: {}", taskId);
			creatureName = null;
			remaining = 0;
			taskActive = false;
			return;
		}

		boolean changed = !taskActive || !resolved.equals(creatureName) || count != remaining;
		creatureName = resolved;
		remaining = count;
		taskActive = true;

		if (changed)
		{
			log.debug("Slayer task: {} x{}", creatureName, remaining);
		}
	}

	public String getCreatureName()
	{
		return creatureName;
	}

	public int getRemaining()
	{
		return remaining;
	}

	public boolean isTaskActive()
	{
		return taskActive;
	}

	public void reset()
	{
		creatureName = null;
		remaining = 0;
		taskActive = false;
	}

	/**
	 * Maps SLAYER_TARGET VarPlayer task IDs to creature names.
	 * <p>
	 * Reference: RuneLite SlayerPlugin Task enum
	 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/Task.java
	 */
	enum SlayerTask
	{
		ABERRANT_SPECTRES(4, "Aberrant spectres"),
		ABYSSAL_DEMONS(5, "Abyssal demons"),
		ADAMANT_DRAGONS(6, "Adamant dragons"),
		ANKOU(7, "Ankou"),
		AVIANSIE(8, "Aviansie"),
		BANSHEES(10, "Banshees"),
		BASILISKS(11, "Basilisks"),
		BATS(12, "Bats"),
		BEARS(13, "Bears"),
		BIRDS(14, "Birds"),
		BLACK_DEMONS(15, "Black demons"),
		BLACK_DRAGONS(16, "Black dragons"),
		BLOODVELD(18, "Bloodveld"),
		BLUE_DRAGONS(19, "Blue dragons"),
		BRINE_RATS(20, "Brine rats"),
		CAVE_BUGS(22, "Cave bugs"),
		CAVE_CRAWLERS(23, "Cave crawlers"),
		CAVE_HORRORS(24, "Cave horrors"),
		CAVE_SLIMES(26, "Cave slimes"),
		COCKATRICE(28, "Cockatrice"),
		COWS(29, "Cows"),
		CRAWLING_HANDS(30, "Crawling hands"),
		DAGANNOTH(31, "Dagannoth"),
		DARK_BEASTS(32, "Dark beasts"),
		DOGS(34, "Dogs"),
		DUST_DEVILS(36, "Dust devils"),
		DWARVES(37, "Dwarves"),
		EARTH_WARRIORS(38, "Earth warriors"),
		ELVES(39, "Elves"),
		FEVER_SPIDERS(41, "Fever spiders"),
		FIRE_GIANTS(42, "Fire giants"),
		FLESH_CRAWLERS(43, "Flesh crawlers"),
		GARGOYLES(47, "Gargoyles"),
		GHOSTS(48, "Ghosts"),
		GHOULS(49, "Ghouls"),
		GOBLINS(51, "Goblins"),
		GREATER_DEMONS(52, "Greater demons"),
		GREEN_DRAGONS(53, "Green dragons"),
		HARPIE_BUG_SWARMS(55, "Harpie bug swarms"),
		HELLHOUNDS(56, "Hellhounds"),
		HILL_GIANTS(57, "Hill giants"),
		HOBGOBLINS(58, "Hobgoblins"),
		ICE_GIANTS(60, "Ice giants"),
		ICE_WARRIORS(61, "Ice warriors"),
		INFERNAL_MAGES(62, "Infernal mages"),
		IRON_DRAGONS(63, "Iron dragons"),
		JELLIES(64, "Jellies"),
		JUNGLE_HORRORS(65, "Jungle horrors"),
		KALPHITE(66, "Kalphite"),
		KILLERWATTS(67, "Killerwatts"),
		KURASK(69, "Kurask"),
		LESSER_DEMONS(71, "Lesser demons"),
		LIZARDMEN(72, "Lizardmen"),
		MINOTAURS(75, "Minotaurs"),
		MITHRIL_DRAGONS(78, "Mithril dragons"),
		MOGRES(79, "Mogres"),
		MOLANISKS(80, "Molanisks"),
		MONKEYS(81, "Monkeys"),
		MOSS_GIANTS(82, "Moss giants"),
		NECHRYAEL(83, "Nechryael"),
		OGRES(84, "Ogres"),
		OTHERWORLDLY_BEINGS(85, "Otherworldly beings"),
		PYREFIENDS(87, "Pyrefiends"),
		RATS(89, "Rats"),
		RED_DRAGONS(90, "Red dragons"),
		ROCKSLUGS(92, "Rockslugs"),
		RUNE_DRAGONS(93, "Rune dragons"),
		SCABARITES(94, "Scabarites"),
		SCORPIONS(95, "Scorpions"),
		SEA_SNAKES(96, "Sea snakes"),
		SHADES(97, "Shades"),
		SKELETAL_WYVERNS(99, "Skeletal wyverns"),
		SKELETONS(100, "Skeletons"),
		SMOKE_DEVILS(101, "Smoke devils"),
		SPIDERS(103, "Spiders"),
		SPIRITUAL_CREATURES(104, "Spiritual creatures"),
		STEEL_DRAGONS(105, "Steel dragons"),
		SUQAH(107, "Suqah"),
		TROLLS(109, "Trolls"),
		TUROTH(110, "Turoth"),
		TZHAAR(112, "TzHaar"),
		VAMPYRES(114, "Vampyres"),
		WALL_BEASTS(115, "Wall beasts"),
		WATERFIENDS(116, "Waterfiends"),
		WEREWOLVES(117, "Werewolves"),
		WOLVES(119, "Wolves"),
		ZOMBIES(121, "Zombies"),
		ZYGOMITES(122, "Zygomites"),
		FOSSIL_ISLAND_WYVERNS(131, "Fossil island wyverns"),
		WYRMS(132, "Wyrms"),
		DRAKES(133, "Drakes"),
		HYDRAS(134, "Hydras"),
		BASILISK_KNIGHTS(135, "Basilisk knights"),
		WARPED_CREATURES(136, "Warped creatures"),
		ARAXYTES(137, "Araxytes");

		private final int id;
		private final String name;

		SlayerTask(int id, String name)
		{
			this.id = id;
			this.name = name;
		}

		static String forId(int id)
		{
			for (SlayerTask task : values())
			{
				if (task.id == id)
				{
					return task.name;
				}
			}
			return null;
		}
	}
}
