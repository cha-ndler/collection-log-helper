package com.collectionloghelper.ui;

import com.collectionloghelper.data.CollectionLogItem;
import com.collectionloghelper.data.CollectionLogSource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

class ItemRowPanel extends JPanel
{
	private static final Color OBTAINED_COLOR = new Color(0, 100, 0, 80);
	private static final Color LOCKED_COLOR = new Color(50, 40, 40);
	private static final Color LOCKED_TEXT_COLOR = new Color(160, 140, 140);
	private static final Dimension ROW_SIZE = new Dimension(0, 36);

	private final CollectionLogItem item;
	private final CollectionLogSource source;

	ItemRowPanel(CollectionLogItem item, CollectionLogSource source, boolean obtained,
		double score, boolean locked, ItemManager itemManager, Runnable onClick)
	{
		this.item = item;
		this.source = source;

		Color bgColor;
		if (obtained)
		{
			bgColor = OBTAINED_COLOR;
		}
		else if (locked)
		{
			bgColor = LOCKED_COLOR;
		}
		else
		{
			bgColor = ColorScheme.DARKER_GRAY_COLOR;
		}

		setLayout(new BorderLayout(5, 0));
		setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		setBackground(bgColor);
		setPreferredSize(ROW_SIZE);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		setAlignmentX(LEFT_ALIGNMENT);

		// Item icon
		JLabel iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(24, 24));
		AsyncBufferedImage image = itemManager.getImage(item.getItemId());
		image.onLoaded(() -> iconLabel.setIcon(new ImageIcon(image)));
		iconLabel.setIcon(new ImageIcon(image));
		add(iconLabel, BorderLayout.WEST);

		// Center: name + source
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setOpaque(false);

		Color nameColor;
		if (obtained)
		{
			nameColor = Color.GREEN;
		}
		else if (locked)
		{
			nameColor = LOCKED_TEXT_COLOR;
		}
		else
		{
			nameColor = Color.WHITE;
		}

		JLabel nameLabel = new JLabel(item.getName());
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(nameColor);
		centerPanel.add(nameLabel, BorderLayout.NORTH);

		String sourceLabelText = locked ? "\uD83D\uDD12 " + source.getName() : source.getName();
		JLabel sourceLabel = new JLabel(sourceLabelText);
		sourceLabel.setFont(FontManager.getRunescapeSmallFont());
		sourceLabel.setForeground(locked ? LOCKED_TEXT_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		centerPanel.add(sourceLabel, BorderLayout.SOUTH);
		add(centerPanel, BorderLayout.CENTER);

		// Right: drop rate + score
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setOpaque(false);

		String rateStr = formatDropRate(item.getDropRate());
		JLabel rateLabel = new JLabel(rateStr, SwingConstants.RIGHT);
		rateLabel.setFont(FontManager.getRunescapeSmallFont());
		rateLabel.setForeground(locked ? LOCKED_TEXT_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
		rightPanel.add(rateLabel, BorderLayout.NORTH);

		if (score > 0)
		{
			JLabel scoreLabel = new JLabel(formatScore(score), SwingConstants.RIGHT);
			scoreLabel.setToolTipText("Estimated time to next new drop from this source");
			scoreLabel.setFont(FontManager.getRunescapeSmallFont());
			scoreLabel.setForeground(locked ? LOCKED_TEXT_COLOR : new Color(255, 200, 0));
			rightPanel.add(scoreLabel, BorderLayout.SOUTH);
		}
		add(rightPanel, BorderLayout.EAST);

		if (onClick != null)
		{
			final Color normalBg = bgColor;
			addMouseListener(new java.awt.event.MouseAdapter()
			{
				@Override
				public void mousePressed(java.awt.event.MouseEvent e)
				{
					onClick.run();
				}

				@Override
				public void mouseEntered(java.awt.event.MouseEvent e)
				{
					if (obtained)
					{
						setBackground(OBTAINED_COLOR.brighter());
					}
					else if (locked)
					{
						setBackground(new Color(60, 50, 50));
					}
					else
					{
						setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
					}
				}

				@Override
				public void mouseExited(java.awt.event.MouseEvent e)
				{
					setBackground(normalBg);
				}
			});
		}
	}

	private static String formatScore(double score)
	{
		double hours = 100.0 / score;
		if (hours < 1.0 / 60.0)
		{
			return "< 1 min";
		}
		else if (hours < 1)
		{
			long min = Math.max(1, Math.round(hours * 60));
			return "~" + min + " min";
		}
		else if (hours < 24)
		{
			long hrs = Math.round(hours);
			return "~" + hrs + (hrs == 1 ? " hr" : " hrs");
		}
		else if (hours < 720)
		{
			long days = Math.round(hours / 24);
			return "~" + days + (days == 1 ? " day" : " days");
		}
		else
		{
			long months = Math.round(hours / 720);
			return "~" + months + (months == 1 ? " month" : " months");
		}
	}

	private static String formatDropRate(double rate)
	{
		if (rate <= 0)
		{
			return "N/A";
		}
		long denominator = Math.round(1.0 / rate);
		return "1/" + denominator;
	}
}
