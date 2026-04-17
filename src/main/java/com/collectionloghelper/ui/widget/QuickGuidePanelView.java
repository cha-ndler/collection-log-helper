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

import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.efficiency.ScoredItem;
import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Factory that creates the "Top Pick" quick-guide banner panels used at the
 * top of Efficient and Category Focus modes.
 *
 * <p>Callers must invoke {@link #create(ScoredItem, boolean, CollectionLogSource)}
 * each time a new banner is needed (i.e. on each rebuild). The internal
 * button reference is updated on each call and kept in sync by
 * {@link #syncGuidanceState(boolean, CollectionLogSource)}.
 */
public class QuickGuidePanelView
{
	private static final Color GUIDE_ME_COLOR = new Color(30, 120, 30);
	private static final Color STOP_GUIDANCE_COLOR = new Color(140, 30, 30);

	private final Consumer<CollectionLogSource> guidanceActivator;
	private final Runnable guidanceDeactivator;

	/** Retained so {@link #syncGuidanceState} can update button text and color. */
	private JButton lastGuideButton;
	/** The source the last-created panel is tracking. */
	private CollectionLogSource lastSource;

	public QuickGuidePanelView(Consumer<CollectionLogSource> guidanceActivator,
		Runnable guidanceDeactivator)
	{
		this.guidanceActivator = guidanceActivator;
		this.guidanceDeactivator = guidanceDeactivator;
	}

	/**
	 * Creates a new quick-guide panel for the given top-scored item.
	 * Must be called on the EDT (typically from the mode controller's buildView).
	 *
	 * @param topItem       scored item to display
	 * @param guidanceActive whether guidance is currently active
	 * @param guidedSource  the currently guided source (may be {@code null})
	 * @return a new {@code JPanel} ready to be added to the list container
	 */
	public JPanel create(ScoredItem topItem, boolean guidanceActive, CollectionLogSource guidedSource)
	{
		final CollectionLogSource source = topItem.getSource();

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(30, 50, 30));
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(80, 200, 80), 1),
			BorderFactory.createEmptyBorder(6, 6, 6, 6)
		));
		panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JLabel titleLabel = new JLabel("<html><b>Top Pick: " + source.getName() + "</b></html>");
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(new Color(255, 200, 0));
		titleLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		panel.add(titleLabel);

		String reasoning = topItem.getReasoning();
		if (reasoning != null && !reasoning.isEmpty())
		{
			JLabel reasonLabel = new JLabel("<html>" + reasoning + "</html>");
			reasonLabel.setFont(FontManager.getRunescapeSmallFont());
			reasonLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			reasonLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
			panel.add(reasonLabel);
		}

		panel.add(Box.createRigidArea(new Dimension(0, 4)));

		boolean isGuidingThis = guidanceActive && guidedSource != null
			&& guidedSource.getName().equals(source.getName());

		JButton guideButton = new JButton(isGuidingThis ? "Stop Guidance" : "Guide Me");
		guideButton.setBackground(isGuidingThis ? STOP_GUIDANCE_COLOR : GUIDE_ME_COLOR);
		guideButton.setForeground(Color.WHITE);
		guideButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
		guideButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		guideButton.addActionListener(e ->
		{
			if (guideButton.getText().equals("Stop Guidance"))
			{
				guidanceDeactivator.run();
				guideButton.setText("Guide Me");
				guideButton.setBackground(GUIDE_ME_COLOR);
			}
			else
			{
				guidanceActivator.accept(source);
				guideButton.setText("Stop Guidance");
				guideButton.setBackground(STOP_GUIDANCE_COLOR);
			}
		});
		panel.add(guideButton);

		lastGuideButton = guideButton;
		lastSource = source;

		return panel;
	}

	/**
	 * Syncs the most-recently-created guide button to the current guidance state.
	 * Safe to call from any thread.
	 *
	 * @param active       whether guidance is now active
	 * @param guidedSource the currently guided source (may be {@code null})
	 */
	public void syncGuidanceState(boolean active, CollectionLogSource guidedSource)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (lastGuideButton == null || lastSource == null)
			{
				return;
			}
			boolean isGuidingTopPick = active && guidedSource != null
				&& guidedSource.getName().equals(lastSource.getName());
			lastGuideButton.setText(isGuidingTopPick ? "Stop Guidance" : "Guide Me");
			lastGuideButton.setBackground(isGuidingTopPick ? STOP_GUIDANCE_COLOR : GUIDE_ME_COLOR);
		});
	}
}
