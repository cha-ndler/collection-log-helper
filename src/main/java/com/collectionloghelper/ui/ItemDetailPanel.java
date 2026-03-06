package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.efficiency.ClueCompletionEstimator;
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
		int sourceObtained, int sourceTotal,
		ItemManager itemManager, ClueCompletionEstimator clueEstimator,
		Runnable onBack, Runnable onGuideMe, Runnable onStopGuidance,
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

		// Info rows — rendered as a single HTML table so the Swing HTML
		// renderer handles text wrapping and row heights correctly.
		// Individual BoxLayout rows can't compute height for wrapped HTML.
		StringBuilder info = new StringBuilder();
		info.append("<html><table width='195' cellpadding='3' cellspacing='0'>");

		appendInfoRow(info, "Source:", source.getName());
		appendInfoRow(info, "Progress:", sourceObtained + "/" + sourceTotal + " items");
		appendInfoRow(info, "Category:", source.getCategory().getDisplayName());

		boolean isGuaranteed = item.getDropRate() >= 1.0;
		if (isGuaranteed && item.getPointCost() > 0)
		{
			appendInfoRow(info, "Cost:", item.getPointCost() + " pts");
			if (source.getPointsPerHour() > 0)
			{
				double hours = (double) item.getPointCost() / source.getPointsPerHour();
				appendInfoRow(info, "Est. Time:", "~" + ClueCompletionEstimator.formatTime((int) (hours * 3600)));
			}
		}
		else if (isGuaranteed && item.getMilestoneKills() > 0)
		{
			appendInfoRow(info, "Milestone:", item.getMilestoneKills() + " kills");
			if (source.getKillTimeSeconds() > 0)
			{
				double hours = item.getMilestoneKills() * (source.getKillTimeSeconds() / 3600.0);
				appendInfoRow(info, "Est. Time:", "~" + ClueCompletionEstimator.formatTime((int) (hours * 3600)));
			}
		}
		else
		{
			long denominator = item.getDropRate() > 0 ? Math.round(1.0 / item.getDropRate()) : 0;
			appendInfoRow(info, "Drop Rate:", denominator > 0 ? "1/" + denominator : "N/A");
		}

		boolean isClueSource = source.getCategory() == CollectionLogCategory.CLUES;
		boolean hideLocationDetails = isClueSource || isGuaranteed;
		if (isClueSource && clueEstimator != null)
		{
			int estSeconds = clueEstimator.estimateCompletionSeconds(source.getName());
			if (estSeconds > 0)
			{
				String bucketName = clueEstimator.getBucket().getDisplayName();
				appendInfoRow(info, "Est. Time:",
					"~" + ClueCompletionEstimator.formatTime(estSeconds)
						+ " (" + bucketName.toLowerCase() + ")");
			}
		}
		else if (!hideLocationDetails)
		{
			appendInfoRow(info, "Kill Time:", source.getKillTimeSeconds() + "s");
			appendInfoRow(info, "Location:", source.getDisplayLocation());
		}
		appendInfoRow(info, "Status:", obtained ? "Obtained" : "Missing");

		if (item.isPet())
		{
			appendInfoRow(info, "Type:", "Pet");
		}

		if (locked)
		{
			appendInfoRow(info, "Access:", "Locked", LOCKED_COLOR);
			if (unmetRequirements != null)
			{
				for (String req : unmetRequirements)
				{
					appendInfoRow(info, "Requires:", req, LOCKED_COLOR);
				}
			}
		}

		info.append("</table></html>");

		JLabel infoLabel = new JLabel(info.toString());
		infoLabel.setFont(FontManager.getRunescapeSmallFont());
		infoLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		add(infoLabel);

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

	private void appendInfoRow(StringBuilder sb, String label, String value)
	{
		appendInfoRow(sb, label, value, Color.WHITE);
	}

	private void appendInfoRow(StringBuilder sb, String label, String value, Color valueColor)
	{
		String hex = String.format("%02x%02x%02x", valueColor.getRed(), valueColor.getGreen(), valueColor.getBlue());
		sb.append("<tr>")
			.append("<td valign='top'><font color='#b5b5b3'>").append(label).append("</font></td>")
			.append("<td align='right' valign='top'><font color='#").append(hex).append("'>")
			.append(value).append("</font></td>")
			.append("</tr>");
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
