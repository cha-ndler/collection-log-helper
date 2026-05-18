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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/**
 * Card-layout content host used as the scrollable list container inside
 * {@link com.collectionloghelper.ui.CollectionLogHelperPanel}.
 *
 * <p>The plugin's {@code PluginPanel} parent supplies the enclosing
 * {@code JScrollPane}; this widget owns the sizing contract reported to
 * that ancestor scroll pane. The override of {@link #getPreferredSize()}
 * returns the preferred size of whichever child card is currently visible
 * (plus this panel's insets), so the scroll viewport tracks the active
 * view rather than the largest card.
 *
 * <p>Extracted from {@code CollectionLogHelperPanel} as part of issue
 * #503 (god-class reduction). The behavior is intentionally identical to
 * the prior anonymous inner class.
 */
public class ListContainerScrollPane extends JPanel
{
	/**
	 * Constructs the container with the supplied layout manager. Callers
	 * typically pass a {@link CardLayout} instance so that the visible
	 * child (and therefore the reported preferred size) follows
	 * {@code CardLayout#show}.
	 *
	 * @param layout layout manager to install (must not be {@code null})
	 */
	public ListContainerScrollPane(LayoutManager layout)
	{
		super(layout);
	}

	/**
	 * Returns the preferred size of the first visible child component,
	 * adjusted for this panel's insets. Falls back to the superclass
	 * implementation when no child is visible.
	 *
	 * <p>This mirrors the original anonymous-class behavior: the
	 * enclosing scroll pane sizes its viewport to the active card.
	 */
	@Override
	public Dimension getPreferredSize()
	{
		for (Component comp : getComponents())
		{
			if (comp.isVisible())
			{
				Insets insets = getInsets();
				Dimension d = comp.getPreferredSize();
				return new Dimension(
					d.width + insets.left + insets.right,
					d.height + insets.top + insets.bottom);
			}
		}
		return super.getPreferredSize();
	}
}
