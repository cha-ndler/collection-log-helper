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

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * Tracks the player's current Slayer task state using RuneLite VarPlayer/DB APIs.
 * <p>
 * The creature name is resolved from the game's SlayerTask DB table using the
 * SLAYER_TARGET VarPlayer as a lookup key — the same approach RuneLite's own
 * Slayer plugin uses.
 * <p>
 * Reference: RuneLite SlayerPlugin.java updateTask()
 * https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/slayer/SlayerPlugin.java
 */
@Slf4j
@Singleton
public class SlayerTaskState
{
	private static final int BOSS_TASK_ID = 98;

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
	 * Refresh the Slayer task state from varps and the game DB table.
	 * Must be called on the client thread.
	 */
	public void refresh()
	{
		int count = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		if (count <= 0)
		{
			if (taskActive)
			{
				log.debug("Slayer task completed or cleared (count={})", count);
			}
			creatureName = null;
			remaining = 0;
			taskActive = false;
			return;
		}

		int taskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		if (taskId <= 0)
		{
			creatureName = null;
			remaining = 0;
			taskActive = false;
			return;
		}

		String resolved = resolveTaskName(taskId);
		if (resolved == null)
		{
			log.debug("Could not resolve slayer task name for id {}", taskId);
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

	/**
	 * Resolve the creature name from the game's SlayerTask DB table.
	 * For boss tasks (id 98), resolves through the SlayerTaskSublist table first.
	 * This mirrors RuneLite's SlayerPlugin.updateTask() approach.
	 */
	private String resolveTaskName(int taskId)
	{
		try
		{
			int taskDBRow;
			if (taskId == BOSS_TASK_ID)
			{
				int bossId = client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
				List<Integer> bossRows = client.getDBRowsByValue(
					DBTableID.SlayerTaskSublist.ID,
					DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID,
					0,
					bossId);
				if (bossRows.isEmpty())
				{
					log.debug("No boss sublist rows for bossId {}", bossId);
					return null;
				}
				Object[] bossField = client.getDBTableField(
					bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0);
				if (bossField == null || bossField.length == 0)
				{
					log.debug("Empty DB field for boss sublist row");
					return null;
				}
				taskDBRow = (Integer) bossField[0];
			}
			else
			{
				List<Integer> taskRows = client.getDBRowsByValue(
					DBTableID.SlayerTask.ID,
					DBTableID.SlayerTask.COL_ID, 0, taskId);
				if (taskRows.isEmpty())
				{
					log.debug("No task rows for taskId {}", taskId);
					return null;
				}
				taskDBRow = taskRows.get(0);
			}

			Object[] nameField = client.getDBTableField(
				taskDBRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
			if (nameField == null || nameField.length == 0)
			{
				log.debug("Empty DB field for task name, taskDBRow={}", taskDBRow);
				return null;
			}
			String name = (String) nameField[0];

			// DB returns uppercase — convert to title case for display
			if (name != null && name.length() > 1)
			{
				name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
			}
			return name;
		}
		catch (Exception e)
		{
			log.debug("Failed to resolve task name from DB for taskId {}", taskId, e);
			return null;
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
}
