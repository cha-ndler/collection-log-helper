package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
	private static final Color GUIDE_ME_COLOR = new Color(30, 120, 30);
	private static final Color STOP_GUIDANCE_COLOR = new Color(140, 30, 30);
	private static final Color LOCKED_COLOR = new Color(200, 80, 80);

	private final JButton guideMeButton;

	ItemDetailPanel(CollectionLogItem item, CollectionLogSource source, boolean obtained,
		boolean locked, List<String> unmetRequirements,
		ItemManager itemManager, Runnable onBack, Runnable onGuideMe, Runnable onStopGuidance,
		boolean guidanceActive)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Back button
		JButton backButton = new JButton("\u2190 Back");
		backButton.setAlignmentX(LEFT_ALIGNMENT);
		backButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		backButton.setPreferredSize(new Dimension(0, 28));
		backButton.addActionListener(e -> onBack.run());
		add(backButton);

		// Item header with icon
		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setOpaque(false);
		headerPanel.setAlignmentX(LEFT_ALIGNMENT);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
		headerPanel.setPreferredSize(new Dimension(0, 44));

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

		add(headerPanel);

		// Info rows
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setOpaque(false);
		infoPanel.setAlignmentX(LEFT_ALIGNMENT);
		infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		addInfoRow(infoPanel, "Source:", source.getName());
		addInfoRow(infoPanel, "Category:", source.getCategory().getDisplayName());

		long denominator = item.getDropRate() > 0 ? Math.round(1.0 / item.getDropRate()) : 0;
		addInfoRow(infoPanel, "Drop Rate:", denominator > 0 ? "1/" + denominator : "N/A");

		addInfoRow(infoPanel, "Kill Time:", source.getKillTimeSeconds() + "s");
		addInfoRow(infoPanel, "Location:", source.getDisplayLocation());
		addInfoRow(infoPanel, "Status:", obtained ? "Obtained" : "Missing");

		if (item.isPet())
		{
			addInfoRow(infoPanel, "Type:", "Pet");
		}

		if (locked)
		{
			addInfoRow(infoPanel, "Access:", "Locked", LOCKED_COLOR);
			if (unmetRequirements != null)
			{
				for (String req : unmetRequirements)
				{
					addInfoRow(infoPanel, "Requires:", req, LOCKED_COLOR);
				}
			}
		}

		add(infoPanel);

		// Action buttons
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
		buttonPanel.setOpaque(false);
		buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
		buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		buttonPanel.setPreferredSize(new Dimension(0, 32));

		guideMeButton = new JButton();
		updateGuideButtonState(guidanceActive);
		guideMeButton.addActionListener(e ->
		{
			if (guideMeButton.getText().equals("Stop Guidance"))
			{
				if (onStopGuidance != null)
				{
					onStopGuidance.run();
				}
				updateGuideButtonState(false);
			}
			else
			{
				if (onGuideMe != null)
				{
					onGuideMe.run();
				}
				updateGuideButtonState(true);
			}
		});
		buttonPanel.add(guideMeButton);

		JButton wikiButton = new JButton("Open Wiki");
		wikiButton.setToolTipText("Open wiki page for " + item.getName());
		wikiButton.addActionListener(e -> openWiki(item));
		buttonPanel.add(wikiButton);

		add(buttonPanel);

		// Push content to top — prevents empty scrollable space
		add(Box.createVerticalGlue());
	}

	private void updateGuideButtonState(boolean active)
	{
		if (active)
		{
			guideMeButton.setText("Stop Guidance");
			guideMeButton.setBackground(STOP_GUIDANCE_COLOR);
			guideMeButton.setForeground(Color.WHITE);
			guideMeButton.setToolTipText("Stop guidance overlays");
		}
		else
		{
			guideMeButton.setText("Guide Me");
			guideMeButton.setBackground(GUIDE_ME_COLOR);
			guideMeButton.setForeground(Color.WHITE);
			guideMeButton.setToolTipText("Show overlays to guide you");
		}
	}

	private void addInfoRow(JPanel parent, String label, String value)
	{
		addInfoRow(parent, label, value, Color.WHITE);
	}

	private void addInfoRow(JPanel parent, String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(FontManager.getRunescapeSmallFont());
		labelComponent.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		labelComponent.setVerticalAlignment(SwingConstants.TOP);
		row.add(labelComponent, BorderLayout.WEST);

		String hex = String.format("%02x%02x%02x", valueColor.getRed(), valueColor.getGreen(), valueColor.getBlue());
		JLabel valueComponent = new JLabel(
			"<html><div style='text-align:right;color:#" + hex + "'>" + value + "</div></html>");
		valueComponent.setFont(FontManager.getRunescapeSmallFont());
		// Preferred width of 0 forces HTML text to wrap within the space allocated by BorderLayout CENTER
		valueComponent.setPreferredSize(new Dimension(0, 0));
		row.add(valueComponent, BorderLayout.CENTER);
		valueComponent.setHorizontalAlignment(SwingConstants.RIGHT);

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
