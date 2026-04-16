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
package com.collectionloghelper.lifecycle;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.data.PluginDataManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.RuneLite;

/**
 * Manages the guidance authoring event log.
 * Writes timestamped, location-stamped events to a per-character log file
 * when {@code config.guidanceAuthoring()} is enabled.
 */
@Slf4j
@Singleton
public class AuthoringLogger
{
	private static final DateTimeFormatter TIME_FMT =
		DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final DateTimeFormatter DATETIME_FMT =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String[] EQUIPMENT_SLOT_NAMES = {
		"Head", "Cape", "Amulet", "Weapon", "Body",
		"Shield", "?", "Legs", "?", "Gloves", "Boots", "?", "Ring", "Ammo"
	};

	private final CollectionLogHelperConfig config;
	private final PluginDataManager pluginDataManager;

	/** Last game tick on which a varbit change was logged — throttles varbit log spam. */
	private int lastVarbitLogTick = -1;

	/** Player location cached by the plugin each game tick. */
	private volatile WorldPoint cachedPlayerLocation;

	/** Open log writer — lazily initialised on first write, closed on {@link #close()}. */
	private PrintWriter authoringLogWriter;

	@Inject
	public AuthoringLogger(CollectionLogHelperConfig config, PluginDataManager pluginDataManager)
	{
		this.config = config;
		this.pluginDataManager = pluginDataManager;
	}

	/**
	 * Updates the cached player location used to stamp each log entry.
	 * Call this from the plugin's {@code onGameTick} after resolving world coordinates.
	 */
	public void setPlayerLocation(WorldPoint location)
	{
		cachedPlayerLocation = location;
	}

	/** Returns the last varbit-change tick that was logged, for throttle comparison. */
	public int getLastVarbitLogTick()
	{
		return lastVarbitLogTick;
	}

	/** Records the tick on which the most recent varbit change was logged. */
	public void setLastVarbitLogTick(int tick)
	{
		lastVarbitLogTick = tick;
	}

	/**
	 * Writes a formatted line to the authoring log.
	 * Does nothing when {@code config.guidanceAuthoring()} is false.
	 * Opens the log file lazily on the first call.
	 */
	public void log(String format, Object... args)
	{
		if (!config.guidanceAuthoring())
		{
			return;
		}
		if (authoringLogWriter == null)
		{
			openLogFile();
			if (authoringLogWriter == null)
			{
				return;
			}
		}
		WorldPoint loc = cachedPlayerLocation;
		String locStr = loc != null
			? String.format("[%d,%d,%d]", loc.getX(), loc.getY(), loc.getPlane())
			: "[?,?,?]";
		String timestamp = LocalTime.now().format(TIME_FMT);
		authoringLogWriter.printf("%s %s %s%n", timestamp, locStr, String.format(format, args));
	}

	/**
	 * Logs an inventory or equipment container change with slot-names for equipment.
	 * Only writes when {@code config.guidanceAuthoring()} is true.
	 *
	 * @param event  the container-changed event
	 * @param client the RuneLite client (used to read container contents)
	 */
	public void logContainerChange(ItemContainerChanged event, Client client)
	{
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV)
		{
			ItemContainer c = client.getItemContainer(InventoryID.INV);
			if (c == null)
			{
				return;
			}
			StringBuilder sb = new StringBuilder("INVENTORY");
			for (Item item : c.getItems())
			{
				if (item.getId() > 0 && item.getQuantity() > 0)
				{
					sb.append(String.format(" %d x%d", item.getId(), item.getQuantity()));
				}
			}
			log("%s", sb.toString());
		}
		else if (containerId == InventoryID.WORN)
		{
			ItemContainer c = client.getItemContainer(InventoryID.WORN);
			if (c == null)
			{
				return;
			}
			StringBuilder sb = new StringBuilder("EQUIPMENT");
			Item[] items = c.getItems();
			for (int i = 0; i < items.length && i < EQUIPMENT_SLOT_NAMES.length; i++)
			{
				if (items[i].getId() > 0)
				{
					sb.append(String.format(" %s=%d", EQUIPMENT_SLOT_NAMES[i], items[i].getId()));
				}
			}
			log("%s", sb.toString());
		}
	}

	/**
	 * Closes the log writer and resets state.
	 * Call this from the plugin's {@code shutDown()}.
	 */
	public void close()
	{
		if (authoringLogWriter != null)
		{
			authoringLogWriter.close();
			authoringLogWriter = null;
		}
		lastVarbitLogTick = -1;
		cachedPlayerLocation = null;
	}

	// ---- private helpers ----

	private void openLogFile()
	{
		File logFile = pluginDataManager.getFile("authoring-log.txt");
		if (logFile == null)
		{
			logFile = new File(RuneLite.RUNELITE_DIR, "clh-authoring-log.txt");
		}
		try
		{
			authoringLogWriter = new PrintWriter(new FileWriter(logFile, true), true);
			authoringLogWriter.printf("=== Authoring session started %s ===%n",
				LocalDateTime.now().format(DATETIME_FMT));
		}
		catch (IOException e)
		{
			log.error("Failed to open authoring log", e);
		}
	}
}
