package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class GuidanceOverlay extends OverlayPanel
{
	private static final int MAX_PANEL_WIDTH = 200;
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;

	private final Client client;
	private final CollectionLogHelperConfig config;

	private volatile WorldPoint targetPoint;
	private volatile String targetName;
	private volatile String locationDescription;
	private volatile String travelTip;
	private volatile String clueGuidanceText;
	private volatile int targetNpcId;
	private volatile String interactAction;
	private volatile boolean showCollectionLogReminder;
	private volatile boolean showBankReminder;

	@Inject
	private GuidanceOverlay(Client client, CollectionLogHelperConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setMovable(true);
		setSnappable(true);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Color overlayColor = config.overlayColor();

		boolean hasReminder = showCollectionLogReminder || showBankReminder;

		if (targetPoint == null)
		{
			if (clueGuidanceText != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(clueGuidanceText)
					.color(Color.WHITE)
					.build());
				addSyncRemindersIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			if (hasReminder)
			{
				panelComponent.getChildren().clear();
				addSyncRemindersIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		// Tile highlight rendering
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetPoint);
		if (localPoint == null)
		{
			// Target tile is not on screen — show compact direction panel
			if (targetName != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(targetName)
					.color(new Color(255, 200, 0))
					.build());
				String loc = locationDescription != null ? locationDescription : "";
				if (!loc.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(loc)
						.leftColor(Color.WHITE)
						.build());
				}
				if (travelTip != null && !travelTip.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left("Travel: " + travelTip)
						.leftColor(new Color(100, 200, 255))
						.build());
				}
				if (interactAction != null && !interactAction.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(interactAction)
						.leftColor(new Color(100, 255, 100))
						.build());
				}
				addSyncRemindersIfNeeded();
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		// NPC highlighting — if we have a target NPC ID, try to find and highlight it
		boolean npcHighlighted = false;
		if (targetNpcId > 0)
		{
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				if (npc != null && npc.getId() == targetNpcId)
				{
					renderNpcHighlight(graphics, npc, overlayColor);
					npcHighlighted = true;
					break;
				}
			}
		}

		// Tile highlight rendering (skip if NPC is already highlighted nearby)
		if (!npcHighlighted)
		{
			Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
			if (poly == null)
			{
				return null;
			}

			Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 50);
			OverlayUtil.renderPolygon(graphics, poly, overlayColor, fillColor,
				new BasicStroke(2.0f));

			if (targetName != null)
			{
				OverlayUtil.renderTextLocation(graphics,
					Perspective.getCanvasTextLocation(client, graphics, localPoint, targetName, 150),
					targetName, overlayColor);
			}
		}

		return null;
	}

	/**
	 * Renders a highlight around an NPC with a downward-pointing arrow and
	 * action label above it, similar to Quest Helper's NPC step rendering.
	 */
	private void renderNpcHighlight(Graphics2D graphics, NPC npc, Color overlayColor)
	{
		Shape hull = npc.getConvexHull();
		if (hull != null)
		{
			Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 30);
			graphics.setColor(fillColor);
			graphics.fill(hull);
			graphics.setColor(overlayColor);
			graphics.setStroke(new BasicStroke(2.0f));
			graphics.draw(hull);

			// Draw downward-pointing arrow above the NPC hull
			Rectangle bounds = hull.getBounds();
			int arrowX = (int) bounds.getCenterX();
			int arrowTipY = (int) bounds.getMinY() - ARROW_GAP;
			renderDirectionArrow(graphics, arrowX, arrowTipY, overlayColor);
		}

		// Render action text above the NPC and arrow
		LocalPoint npcLocal = npc.getLocalLocation();
		if (npcLocal != null)
		{
			String label = interactAction != null
				? interactAction + " " + npc.getName()
				: npc.getName();

			Point textPoint = Perspective.getCanvasTextLocation(
				client, graphics, npcLocal, label,
				npc.getLogicalHeight() + ARROW_HEIGHT + ARROW_GAP + 40);
			if (textPoint != null)
			{
				renderOutlinedText(graphics, textPoint, label, overlayColor);
			}
		}
	}

	/**
	 * Draws a downward-pointing arrow at the given position, with a black outline
	 * for visibility. The tip of the arrow is at (x, tipY).
	 */
	private void renderDirectionArrow(Graphics2D graphics, int x, int tipY, Color color)
	{
		int halfW = ARROW_WIDTH / 2;
		int topY = tipY - ARROW_HEIGHT;

		Polygon arrow = new Polygon(
			new int[]{x, x + halfW, x - halfW},
			new int[]{tipY, topY, topY},
			3
		);

		// Black outline
		graphics.setColor(Color.BLACK);
		graphics.setStroke(new BasicStroke(2.0f));
		graphics.drawPolygon(arrow);

		// Colored fill
		graphics.setColor(color);
		graphics.fillPolygon(arrow);
	}

	/**
	 * Renders text with a dark outline for readability against any background.
	 */
	private void renderOutlinedText(Graphics2D graphics, Point point, String text, Color color)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		Font font = graphics.getFont().deriveFont(Font.BOLD, 12f);
		graphics.setFont(font);
		FontMetrics fm = graphics.getFontMetrics();
		int x = point.getX() - fm.stringWidth(text) / 2;
		int y = point.getY();

		// Black outline
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.drawString(text, x - 1, y - 1);
		graphics.drawString(text, x + 1, y - 1);
		graphics.drawString(text, x - 1, y + 1);

		// Colored text
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	public void setTargetPoint(WorldPoint targetPoint)
	{
		this.targetPoint = targetPoint;
	}

	public void setTargetName(String targetName)
	{
		this.targetName = targetName;
	}

	public void setLocationDescription(String locationDescription)
	{
		this.locationDescription = locationDescription;
	}

	public void setTravelTip(String travelTip)
	{
		this.travelTip = travelTip;
	}

	public void setClueGuidanceText(String clueGuidanceText)
	{
		this.clueGuidanceText = clueGuidanceText;
	}

	public void setTargetNpcId(int npcId)
	{
		this.targetNpcId = npcId;
	}

	public void setInteractAction(String action)
	{
		this.interactAction = action;
	}

	public void setShowCollectionLogReminder(boolean show)
	{
		this.showCollectionLogReminder = show;
	}

	public void setShowBankReminder(boolean show)
	{
		this.showBankReminder = show;
	}

	public void clearTarget()
	{
		targetPoint = null;
		targetName = null;
		locationDescription = null;
		travelTip = null;
		clueGuidanceText = null;
		targetNpcId = 0;
		interactAction = null;
	}

	private void addSyncRemindersIfNeeded()
	{
		if (showCollectionLogReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Collection Log to sync")
				.color(new Color(255, 170, 0))
				.build());
		}
		if (showBankReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Bank to scan items")
				.color(new Color(255, 170, 0))
				.build());
		}
	}
}
