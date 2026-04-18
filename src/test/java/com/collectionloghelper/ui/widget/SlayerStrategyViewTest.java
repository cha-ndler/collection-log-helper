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
package com.collectionloghelper.ui.widget;

import com.collectionloghelper.data.SlayerTaskState;
import com.collectionloghelper.efficiency.SlayerStrategyCalculator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SlayerStrategyView}.
 */
public class SlayerStrategyViewTest
{
	private SlayerTaskState slayerTaskState;
	private SlayerStrategyCalculator calculator;
	private SlayerStrategyView view;

	@Before
	public void setUp()
	{
		slayerTaskState = Mockito.mock(SlayerTaskState.class);
		calculator = Mockito.mock(SlayerStrategyCalculator.class);
		view = new SlayerStrategyView(slayerTaskState, calculator);
	}

	@Test
	public void constructsWithoutThrowing()
	{
		assertNotNull(view);
	}

	@Test
	public void refresh_noActiveTask_doesNotThrow()
	{
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(false);
		view.refresh();
	}

	@Test
	public void refresh_activeTask_collapsedState_doesNotThrow()
	{
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(true);
		Mockito.when(slayerTaskState.getCreatureName()).thenReturn("Abyssal Demons");
		Mockito.when(slayerTaskState.getRemaining()).thenReturn(120);
		Mockito.when(calculator.getRecommendedMaster()).thenReturn("Duradel");
		view.refresh();
	}

	@Test
	public void refresh_activeTask_noRecommendedMaster_doesNotThrow()
	{
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(true);
		Mockito.when(slayerTaskState.getCreatureName()).thenReturn("Cows");
		Mockito.when(slayerTaskState.getRemaining()).thenReturn(10);
		Mockito.when(calculator.getRecommendedMaster()).thenReturn(null);
		view.refresh();
	}

	@Test
	public void refresh_nullCreatureName_doesNotThrow()
	{
		// snapshot-then-null-check discipline: guard against null task name
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(true);
		Mockito.when(slayerTaskState.getCreatureName()).thenReturn(null);
		Mockito.when(slayerTaskState.getRemaining()).thenReturn(0);
		Mockito.when(calculator.getRecommendedMaster()).thenReturn(null);
		Mockito.when(calculator.getUsefulSourcesForCreature(null))
			.thenReturn(Collections.emptyList());
		Mockito.when(calculator.getMissingItemsForCreature(null)).thenReturn(0);
		view.refresh();
	}

	@Test
	public void refresh_activeTask_expandedState_doesNotThrow() throws Exception
	{
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(true);
		Mockito.when(slayerTaskState.getCreatureName()).thenReturn("Abyssal Demons");
		Mockito.when(slayerTaskState.getRemaining()).thenReturn(120);
		Mockito.when(calculator.getRecommendedMaster()).thenReturn("Duradel");
		Mockito.when(calculator.getUsefulSourcesForCreature("Abyssal Demons"))
			.thenReturn(Collections.singletonList("Abyssal Lord"));
		Mockito.when(calculator.getMissingItemsForCreature("Abyssal Demons")).thenReturn(2);
		Mockito.when(calculator.getRecommendedBlockList("Duradel"))
			.thenReturn(Collections.emptyList());

		view.refresh();

		// Drive the toggle button's ActionListener on the EDT to expand
		SwingUtilities.invokeAndWait(() ->
		{
			ActionListener[] listeners = view.getComponents()[1].getListeners(ActionListener.class);
			for (ActionListener listener : listeners)
			{
				listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
			}
		});

		// No exception should occur; expanded content should render without error
	}

	@Test
	public void toggleExpanded_multipleToggles_doesNotThrow() throws Exception
	{
		Mockito.when(slayerTaskState.isTaskActive()).thenReturn(true);
		Mockito.when(slayerTaskState.getCreatureName()).thenReturn("Slayer Master");
		Mockito.when(slayerTaskState.getRemaining()).thenReturn(50);
		Mockito.when(calculator.getRecommendedMaster()).thenReturn("Konar");
		Mockito.when(calculator.getUsefulSourcesForCreature("Slayer Master"))
			.thenReturn(Collections.singletonList("Boss"));
		Mockito.when(calculator.getMissingItemsForCreature("Slayer Master")).thenReturn(1);
		Mockito.when(calculator.getRecommendedBlockList("Konar"))
			.thenReturn(Collections.singletonList("Easy Task"));

		view.refresh();

		// Toggle expanded and collapsed states repeatedly
		SwingUtilities.invokeAndWait(() ->
		{
			ActionListener[] listeners = view.getComponents()[1].getListeners(ActionListener.class);
			if (listeners.length > 0)
			{
				ActionListener listener = listeners[0];
				// Toggle to expanded
				listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
				// Toggle back to collapsed
				listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
			}
		});
	}
}
