package com.collectionloghelper.data;

import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;

/**
 * Manages per-character data directories under ~/.runelite/collection-log-helper/{playerName}/.
 * Provides the canonical location for efficiency exports and any future file-based data.
 */
@Slf4j
@Singleton
public class PluginDataManager
{
	private static final String PLUGIN_DIR_NAME = "collection-log-helper";

	private final Client client;

	/** Resolved on login when the player name is available. */
	private volatile File characterDir;
	private volatile String currentPlayerName;

	@Inject
	private PluginDataManager(Client client)
	{
		this.client = client;
	}

	/**
	 * Initialize the per-character directory. Must be called from the client thread
	 * after login when the player name is available.
	 *
	 * @return true if the directory was created/exists successfully
	 */
	public boolean init()
	{
		String playerName = getPlayerName();
		if (playerName == null || playerName.isEmpty())
		{
			log.warn("Cannot init plugin data directory — player name not available");
			return false;
		}

		// Sanitize player name for filesystem safety
		String safeName = sanitizeFileName(playerName);
		currentPlayerName = playerName;

		File pluginDir = new File(RuneLite.RUNELITE_DIR, PLUGIN_DIR_NAME);
		characterDir = new File(pluginDir, safeName);

		if (!characterDir.exists() && !characterDir.mkdirs())
		{
			log.error("Failed to create plugin data directory: {}", characterDir.getAbsolutePath());
			return false;
		}

		log.debug("Plugin data directory ready: {}", characterDir.getAbsolutePath());
		return true;
	}

	/**
	 * Returns the per-character data directory, or null if not initialized.
	 */
	public File getCharacterDir()
	{
		return characterDir;
	}

	/**
	 * Returns a file handle within the per-character directory.
	 *
	 * @param fileName the file name (e.g., "efficiency-export.txt")
	 * @return the File, or null if the character dir is not initialized
	 */
	public File getFile(String fileName)
	{
		if (characterDir == null)
		{
			return null;
		}
		return new File(characterDir, fileName);
	}

	/**
	 * Returns the current player name, or null if not logged in.
	 */
	public String getCurrentPlayerName()
	{
		return currentPlayerName;
	}

	/**
	 * Reset state on logout.
	 */
	public void reset()
	{
		characterDir = null;
		currentPlayerName = null;
	}

	private String getPlayerName()
	{
		if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null)
		{
			return client.getLocalPlayer().getName();
		}
		return null;
	}

	/**
	 * Sanitize a player name for safe use as a directory name.
	 * Replaces characters that are invalid on Windows/Linux filesystems.
	 */
	static String sanitizeFileName(String name)
	{
		// OSRS names can contain letters, numbers, spaces, hyphens, underscores
		// Replace anything else, and trim
		return name.replaceAll("[^a-zA-Z0-9 _-]", "_").trim();
	}
}
