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

import com.collectionloghelper.data.DataSyncState;
import com.collectionloghelper.ui.CollectionLogHelperPanel.SyncState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;

/**
 * Shared widget that displays the collection-log sync status label and the
 * data-sync warning banner (shown when the collection log or bank have not
 * been opened yet).
 *
 * <p>Constructed once by the shell and refreshed via {@link #updateSyncStatus}
 * and {@link #updateDataSyncWarning}.
 */
public class SyncStatusView extends JPanel
{
	private static final Color SYNC_NOT_SYNCED_COLOR = new Color(230, 180, 50);
	private static final Color SYNC_SYNCING_COLOR = new Color(0, 200, 200);
	private static final Color SYNC_SYNCED_COLOR = new Color(50, 200, 50);
	private static final Color DATA_WARNING_COLOR = new Color(220, 50, 50);

	private final DataSyncState dataSyncState;

	private final JLabel syncStatusLabel;
	private final JLabel dataSyncWarningLabel;

	public SyncStatusView(DataSyncState dataSyncState)
	{
		this.dataSyncState = dataSyncState;

		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		syncStatusLabel = new JLabel("Open Collection Log to sync", SwingConstants.CENTER);
		syncStatusLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.ITALIC));
		syncStatusLabel.setForeground(SYNC_NOT_SYNCED_COLOR);
		syncStatusLabel.setAlignmentX(CENTER_ALIGNMENT);
		syncStatusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		syncStatusLabel.setToolTipText("Syncs automatically when you open the Collection Log in-game");
		add(syncStatusLabel);

		dataSyncWarningLabel = new JLabel("", SwingConstants.CENTER);
		dataSyncWarningLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		dataSyncWarningLabel.setForeground(DATA_WARNING_COLOR);
		dataSyncWarningLabel.setAlignmentX(CENTER_ALIGNMENT);
		dataSyncWarningLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		dataSyncWarningLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		dataSyncWarningLabel.setVisible(false);
		add(dataSyncWarningLabel);
	}

	/**
	 * Updates the sync status label to reflect the current sync state.
	 * Safe to call from any thread (dispatches to EDT internally).
	 */
	public void updateSyncStatus(SyncState state, int itemCount)
	{
		SwingUtilities.invokeLater(() ->
		{
			Font smallFont = FontManager.getRunescapeSmallFont();
			switch (state)
			{
				case NOT_SYNCED:
					syncStatusLabel.setText("Open Collection Log to sync");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.ITALIC));
					syncStatusLabel.setForeground(SYNC_NOT_SYNCED_COLOR);
					break;
				case SYNCING:
					syncStatusLabel.setText("Syncing...");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.ITALIC));
					syncStatusLabel.setForeground(SYNC_SYNCING_COLOR);
					break;
				case SYNCED:
					syncStatusLabel.setText("Synced (" + itemCount + " items)");
					syncStatusLabel.setFont(smallFont.deriveFont(Font.PLAIN));
					syncStatusLabel.setForeground(SYNC_SYNCED_COLOR);
					break;
				default:
					break;
			}
		});
	}

	/**
	 * Re-evaluates the data sync warning banner using the current
	 * {@link DataSyncState}. Safe to call from any thread.
	 */
	public void updateDataSyncWarning()
	{
		// Snapshot fields before dispatch to avoid racing with mutation on game thread
		final boolean fullySynced = dataSyncState.isFullySynced();
		final boolean collectionLogSynced = dataSyncState.isCollectionLogSynced();
		final boolean bankScanned = dataSyncState.isBankScanned();

		SwingUtilities.invokeLater(() ->
		{
			if (fullySynced)
			{
				dataSyncWarningLabel.setVisible(false);
			}
			else
			{
				StringBuilder text = new StringBuilder("<html><center>");
				if (!collectionLogSynced && !bankScanned)
				{
					text.append("Open Collection Log & Bank<br>for accurate guidance");
				}
				else if (!collectionLogSynced)
				{
					text.append("Open Collection Log<br>for accurate guidance");
				}
				else
				{
					text.append("Open Bank to scan items<br>for accurate guidance");
				}
				text.append("</center></html>");
				dataSyncWarningLabel.setText(text.toString());
				dataSyncWarningLabel.setVisible(true);
			}
			revalidate();
		});
	}
}
