package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

class ItemDetailPanel extends JPanel
{
	private static final HttpUrl WIKI_BASE = HttpUrl.get("https://oldschool.runescape.wiki");

	ItemDetailPanel(CollectionLogItem item, CollectionLogSource source, boolean obtained,
		ItemManager itemManager, Runnable onBack, Runnable onGuideMe)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Back button
		JButton backButton = new JButton("\u2190 Back");
		backButton.addActionListener(e -> onBack.run());
		add(backButton, BorderLayout.NORTH);

		// Center content
		JPanel contentPanel = new JPanel(new BorderLayout(0, 8));
		contentPanel.setOpaque(false);

		// Item header with icon
		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setOpaque(false);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(36, 36));
		AsyncBufferedImage image = itemManager.getImage(item.getItemId());
		image.onLoaded(() -> iconLabel.setIcon(new ImageIcon(image)));
		iconLabel.setIcon(new ImageIcon(image));
		headerPanel.add(iconLabel, BorderLayout.WEST);

		JLabel nameLabel = new JLabel(item.getName());
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(obtained ? Color.GREEN : Color.WHITE);
		headerPanel.add(nameLabel, BorderLayout.CENTER);

		contentPanel.add(headerPanel, BorderLayout.NORTH);

		// Info rows
		JPanel infoPanel = new JPanel(new GridLayout(0, 1, 0, 4));
		infoPanel.setOpaque(false);

		addInfoRow(infoPanel, "Source:", source.getName());
		addInfoRow(infoPanel, "Category:", source.getCategory().getDisplayName());

		long denominator = item.getDropRate() > 0 ? Math.round(1.0 / item.getDropRate()) : 0;
		addInfoRow(infoPanel, "Drop Rate:", denominator > 0 ? "1/" + denominator : "N/A");

		addInfoRow(infoPanel, "Kill Time:", source.getKillTimeSeconds() + "s");
		addInfoRow(infoPanel, "Location:",
			source.getWorldX() + ", " + source.getWorldY());
		addInfoRow(infoPanel, "Status:", obtained ? "Obtained" : "Missing");

		if (item.isPet())
		{
			addInfoRow(infoPanel, "Type:", "Pet");
		}

		contentPanel.add(infoPanel, BorderLayout.CENTER);
		add(contentPanel, BorderLayout.CENTER);

		// Action buttons
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
		buttonPanel.setOpaque(false);

		JButton guideMeButton = new JButton("Guide Me");
		guideMeButton.setToolTipText("Show overlays to guide you to " + source.getName());
		guideMeButton.addActionListener(e ->
		{
			if (onGuideMe != null)
			{
				onGuideMe.run();
			}
		});
		buttonPanel.add(guideMeButton);

		JButton wikiButton = new JButton("Open Wiki");
		wikiButton.setToolTipText("Open wiki page for " + item.getName());
		wikiButton.addActionListener(e -> openWiki(item));
		buttonPanel.add(wikiButton);

		add(buttonPanel, BorderLayout.SOUTH);
	}

	private void addInfoRow(JPanel parent, String label, String value)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(FontManager.getRunescapeSmallFont());
		labelComponent.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(labelComponent, BorderLayout.WEST);

		JLabel valueComponent = new JLabel(value, SwingConstants.RIGHT);
		valueComponent.setFont(FontManager.getRunescapeSmallFont());
		valueComponent.setForeground(Color.WHITE);
		row.add(valueComponent, BorderLayout.EAST);

		parent.add(row);
	}

	private void openWiki(CollectionLogItem item)
	{
		String page = item.getWikiPage();
		if (page == null || page.isEmpty())
		{
			page = item.getName().replace(' ', '_');
		}

		String url = WIKI_BASE.newBuilder()
			.addPathSegment("w")
			.addPathSegment(page)
			.addQueryParameter("utm_source", "runelite")
			.build()
			.toString();

		LinkBrowser.browse(url);
	}
}
