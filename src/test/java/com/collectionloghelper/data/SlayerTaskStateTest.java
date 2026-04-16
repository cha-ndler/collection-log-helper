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

import java.lang.reflect.Constructor;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SlayerTaskStateTest
{
	@Mock
	private Client client;

	private SlayerTaskState taskState;

	@Before
	public void setUp() throws Exception
	{
		Constructor<SlayerTaskState> ctor = SlayerTaskState.class.getDeclaredConstructor(Client.class);
		ctor.setAccessible(true);
		taskState = ctor.newInstance(client);
	}

	// ========================================================================
	// Initial state
	// ========================================================================

	@Test
	public void initialState_noTask()
	{
		assertFalse(taskState.isTaskActive());
		assertNull(taskState.getCreatureName());
		assertEquals(0, taskState.getRemaining());
	}

	// ========================================================================
	// refresh — no task
	// ========================================================================

	@Test
	public void refresh_zeroCount_noTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(0);
		taskState.refresh();

		assertFalse(taskState.isTaskActive());
		assertNull(taskState.getCreatureName());
		assertEquals(0, taskState.getRemaining());
	}

	@Test
	public void refresh_negativeCount_noTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(-1);
		taskState.refresh();

		assertFalse(taskState.isTaskActive());
	}

	@Test
	public void refresh_zeroTaskId_noTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(50);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(0);
		taskState.refresh();

		assertFalse(taskState.isTaskActive());
	}

	// ========================================================================
	// refresh — with task
	// ========================================================================

	@Test
	public void refresh_validTask_activatesTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(150);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(42);
		when(client.getDBRowsByValue(eq(DBTableID.SlayerTask.ID),
			eq(DBTableID.SlayerTask.COL_ID), eq(0), eq(42)))
			.thenReturn(Collections.singletonList(100));
		when(client.getDBTableField(eq(100), eq(DBTableID.SlayerTask.COL_NAME_UPPERCASE), eq(0)))
			.thenReturn(new Object[]{"ABYSSAL DEMONS"});

		taskState.refresh();

		assertTrue(taskState.isTaskActive());
		assertEquals("Abyssal demons", taskState.getCreatureName());
		assertEquals(150, taskState.getRemaining());
	}

	@Test
	public void refresh_emptyDBRows_noTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(50);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(42);
		when(client.getDBRowsByValue(anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(Collections.emptyList());

		taskState.refresh();

		assertFalse(taskState.isTaskActive());
	}

	@Test
	public void refresh_nullDBField_noTask()
	{
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(50);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(42);
		when(client.getDBRowsByValue(anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(Collections.singletonList(100));
		when(client.getDBTableField(anyInt(), anyInt(), anyInt()))
			.thenReturn(null);

		taskState.refresh();

		assertFalse(taskState.isTaskActive());
	}

	// ========================================================================
	// Task completion (task disappears)
	// ========================================================================

	@Test
	public void refresh_taskCompletes_deactivates()
	{
		// Activate task
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(1);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(42);
		when(client.getDBRowsByValue(anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(Collections.singletonList(100));
		when(client.getDBTableField(anyInt(), anyInt(), anyInt()))
			.thenReturn(new Object[]{"HELLHOUNDS"});
		taskState.refresh();
		assertTrue(taskState.isTaskActive());

		// Complete task
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(0);
		taskState.refresh();
		assertFalse(taskState.isTaskActive());
		assertNull(taskState.getCreatureName());
	}

	// ========================================================================
	// reset
	// ========================================================================

	@Test
	public void reset_clearsAll()
	{
		// Activate task
		when(client.getVarpValue(VarPlayerID.SLAYER_COUNT)).thenReturn(50);
		when(client.getVarpValue(VarPlayerID.SLAYER_TARGET)).thenReturn(42);
		when(client.getDBRowsByValue(anyInt(), anyInt(), anyInt(), anyInt()))
			.thenReturn(Collections.singletonList(100));
		when(client.getDBTableField(anyInt(), anyInt(), anyInt()))
			.thenReturn(new Object[]{"DARK BEASTS"});
		taskState.refresh();
		assertTrue(taskState.isTaskActive());

		taskState.reset();
		assertFalse(taskState.isTaskActive());
		assertNull(taskState.getCreatureName());
		assertEquals(0, taskState.getRemaining());
	}
}
