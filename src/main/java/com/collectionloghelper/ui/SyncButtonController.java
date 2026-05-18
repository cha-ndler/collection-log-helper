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
package com.collectionloghelper.ui;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.FontManager;

/**
 * Owns the two "Sync now" buttons that live in the panel's controls strip:
 * <ul>
 *   <li>"Sync from collectionlog.net" — triggers the collectionlog.net import.</li>
 *   <li>"Sync KC from TempleOSRS" — triggers a TempleOSRS KC fetch.</li>
 * </ul>
 *
 * <p>Responsibilities pulled out of {@link CollectionLogHelperPanel}:
 * <ul>
 *   <li>Construct the two {@link JButton}s with their canonical labels,
 *       fonts, sizes, tooltips, and visibility based on the current config.</li>
 *   <li>Hold the per-button click {@link Runnable} callbacks (wired from the
 *       plugin after panel construction).</li>
 *   <li>Reset button enabled/text state after a sync attempt completes.</li>
 *   <li>Refresh button visibility when the config toggles change.</li>
 * </ul>
 *
 * <p>This class is intentionally not a Guice {@code @Singleton}; it is
 * constructed inside the panel's constructor (same pattern as
 * {@link PanelRebuildOrchestrator}) and bound to that panel's Swing tree.
 *
 * <p>EDT discipline: state-mutating methods that may be invoked off the EDT
 * (sync-complete callbacks, config-change listeners) wrap their work in
 * {@link SwingUtilities#invokeLater}. Methods invoked only from the EDT
 * (action-listener handlers) mutate components directly.
 */
public final class SyncButtonController
{
	private static final String COLLECTION_LOG_NET_LABEL = "Sync from collectionlog.net";
	private static final String COLLECTION_LOG_NET_TOOLTIP =
		"Click to send your display name to collectionlog.net and import your obtained-items list";
	private static final String TEMPLE_OSRS_LABEL = "Sync KC from TempleOSRS";
	private static final String TEMPLE_OSRS_TOOLTIP =
		"Fetch your boss/activity kill counts from templeosrs.com";
	private static final String TEMPLE_OSRS_SUCCESS_LABEL = "Synced KC from TempleOSRS";
	private static final String SYNCING_LABEL = "Syncing...";

	private final CollectionLogHelperConfig config;
	private final JComponent panel;
	private final JButton collectionLogNetSyncButton;
	private final JButton templeSyncButton;

	private Runnable collectionLogNetImportCallback;
	private Runnable templeSyncCallback;

	public SyncButtonController(CollectionLogHelperConfig config, JComponent panel)
	{
		this.config = config;
		this.panel = panel;
		this.collectionLogNetSyncButton = buildCollectionLogNetButton();
		this.templeSyncButton = buildTempleSyncButton();
	}

	private JButton buildCollectionLogNetButton()
	{
		JButton button = new JButton(COLLECTION_LOG_NET_LABEL);
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusPainted(false);
		button.setToolTipText(COLLECTION_LOG_NET_TOOLTIP);
		button.setVisible(config.enableCollectionLogNetImport());
		button.addActionListener(e ->
		{
			if (collectionLogNetImportCallback != null)
			{
				button.setEnabled(false);
				button.setText(SYNCING_LABEL);
				collectionLogNetImportCallback.run();
			}
		});
		return button;
	}

	private JButton buildTempleSyncButton()
	{
		JButton button = new JButton(TEMPLE_OSRS_LABEL);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setAlignmentX(Component.CENTER_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		button.setToolTipText(TEMPLE_OSRS_TOOLTIP);
		button.setVisible(config.enableTempleOsrsSync());
		button.addActionListener(e ->
		{
			if (templeSyncCallback != null)
			{
				button.setEnabled(false);
				button.setText(SYNCING_LABEL);
				templeSyncCallback.run();
			}
		});
		return button;
	}

	/** Returns the "Sync from collectionlog.net" button so the panel can place it in its layout. */
	public JButton getCollectionLogNetButton()
	{
		return collectionLogNetSyncButton;
	}

	/** Returns the "Sync KC from TempleOSRS" button so the panel can place it in its layout. */
	public JButton getTempleSyncButton()
	{
		return templeSyncButton;
	}

	/**
	 * Wires the callback invoked when the "Sync from collectionlog.net" button is clicked.
	 * Must be called from the EDT or before the panel is shown.
	 */
	public void setCollectionLogNetImportCallback(Runnable callback)
	{
		this.collectionLogNetImportCallback = callback;
	}

	/**
	 * Registers the callback invoked when the "Sync KC from TempleOSRS" button is clicked.
	 * Must be called from the EDT or before the panel is shown.
	 */
	public void setTempleSyncCallback(Runnable callback)
	{
		this.templeSyncCallback = callback;
	}

	/**
	 * Resets the "Sync from collectionlog.net" button to its ready state and
	 * shows a brief result message in the button text. Safe to call from any thread.
	 *
	 * @param message short result message, e.g. "Synced 42 items from collectionlog.net"
	 */
	public void onCollectionLogNetImportComplete(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			collectionLogNetSyncButton.setEnabled(true);
			collectionLogNetSyncButton.setText(message);
		});
	}

	/**
	 * Shows or hides the collectionlog.net sync button based on the current config flag.
	 * Call when the config section toggle changes. Safe to call from any thread.
	 */
	public void updateCollectionLogNetImportButton()
	{
		SwingUtilities.invokeLater(() ->
		{
			boolean enabled = config.enableCollectionLogNetImport();
			collectionLogNetSyncButton.setVisible(enabled);
			if (enabled)
			{
				collectionLogNetSyncButton.setEnabled(true);
				collectionLogNetSyncButton.setText(COLLECTION_LOG_NET_LABEL);
			}
			panel.revalidate();
			panel.repaint();
		});
	}

	/**
	 * Refreshes the visibility of the TempleOSRS sync button based on the current config.
	 * Call after the config value changes. Safe to call from any thread.
	 */
	public void updateTempleSyncButtonVisibility()
	{
		SwingUtilities.invokeLater(() ->
			templeSyncButton.setVisible(config.enableTempleOsrsSync()));
	}

	/**
	 * Resets the TempleOSRS sync button to its idle state after a sync attempt completes.
	 * Safe to call from any thread.
	 *
	 * @param success whether the sync succeeded (affects button label)
	 */
	public void onTempleSyncComplete(boolean success)
	{
		SwingUtilities.invokeLater(() ->
		{
			templeSyncButton.setEnabled(true);
			templeSyncButton.setText(success ? TEMPLE_OSRS_SUCCESS_LABEL : TEMPLE_OSRS_LABEL);
		});
	}
}
