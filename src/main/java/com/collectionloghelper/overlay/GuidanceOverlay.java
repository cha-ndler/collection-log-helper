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
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import javax.inject.Singleton;

@Singleton
public class GuidanceOverlay extends OverlayPanel
{
	private static final int MAX_PANEL_WIDTH = 200;
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;

	private final Client client;
	private final CollectionLogHelperConfig config;

	@Inject
	private TooltipManager tooltipManager;

	private volatile WorldPoint targetPoint;
	private volatile String targetName;
	private volatile String locationDescription;
	private volatile String travelTip;
	private volatile String clueGuidanceText;
	private volatile int targetNpcId;
	private volatile String interactAction;
	private volatile boolean showCollectionLogReminder;
	private volatile boolean showBankReminder;
	private volatile NPC trackedNpc;

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
		// Snapshot all volatile fields to prevent thread-safety races
		final WorldPoint point = this.targetPoint;
		final String name = this.targetName;
		final String locDesc = this.locationDescription;
		final String travel = this.travelTip;
		final String clueText = this.clueGuidanceText;
		final int npcId = this.targetNpcId;
		final String action = this.interactAction;
		final boolean logReminder = this.showCollectionLogReminder;
		final boolean bankReminder = this.showBankReminder;

		Color overlayColor = config.overlayColor();

		boolean hasReminder = logReminder || bankReminder;

		if (point == null)
		{
			if (clueText != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(clueText)
					.color(Color.WHITE)
					.build());
				addSyncRemindersIfNeeded(logReminder, bankReminder);
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			if (hasReminder)
			{
				panelComponent.getChildren().clear();
				addSyncRemindersIfNeeded(logReminder, bankReminder);
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		// Tile highlight rendering
		LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
		if (localPoint == null)
		{
			// Target tile is not on screen — show compact direction panel
			if (name != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(name)
					.color(new Color(255, 200, 0))
					.build());
				String loc = locDesc != null ? locDesc : "";
				if (!loc.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(loc)
						.leftColor(Color.WHITE)
						.build());
				}
				if (travel != null && !travel.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left("Travel: " + travel)
						.leftColor(new Color(100, 200, 255))
						.build());
				}
				if (action != null && !action.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(action)
						.leftColor(new Color(100, 255, 100))
						.build());
				}
				addSyncRemindersIfNeeded(logReminder, bankReminder);
				panelComponent.setPreferredSize(new Dimension(MAX_PANEL_WIDTH, 0));
				return super.render(graphics);
			}
			return null;
		}

		// NPC highlighting — if we have a target NPC ID, try to find and highlight it
		boolean npcHighlighted = false;
		if (npcId > 0)
		{
			NPC npc = this.trackedNpc;
			// Use tracked NPC if available and still matches; fall back to full scan
			if (npc == null || npc.getId() != npcId)
			{
				npc = null;
				for (NPC candidate : client.getTopLevelWorldView().npcs())
				{
					if (candidate != null && candidate.getId() == npcId)
					{
						npc = candidate;
						// Cache the fallback result to avoid re-scanning every frame
						this.trackedNpc = npc;
						break;
					}
				}
			}
			if (npc != null)
			{
				renderNpcHighlight(graphics, npc, overlayColor, action, locDesc);
				npcHighlighted = true;
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

			if (name != null)
			{
				OverlayUtil.renderTextLocation(graphics,
					Perspective.getCanvasTextLocation(client, graphics, localPoint, name, 150),
					name, overlayColor);
			}
		}

		return null;
	}

	/**
	 * Renders a highlight around an NPC with a downward-pointing arrow and
	 * action label above it, similar to Quest Helper's NPC step rendering.
	 */
	private void renderNpcHighlight(Graphics2D graphics, NPC npc, Color overlayColor, String action,
		String locDesc)
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

			// Show tooltip when mouse hovers over the NPC hull
			String builtTooltip = OverlayTooltipHelper.buildTooltip(locDesc, action);
			if (builtTooltip != null)
			{
				Point mousePos = client.getMouseCanvasPosition();
				if (mousePos != null && hull.contains(mousePos.getX(), mousePos.getY()))
				{
					tooltipManager.add(new Tooltip(builtTooltip));
				}
			}
		}

		// Render action text above the NPC and arrow
		LocalPoint npcLocal = npc.getLocalLocation();
		if (npcLocal != null)
		{
			String label = action != null
				? action + " " + npc.getName()
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

	public void setTrackedNpc(NPC npc)
	{
		this.trackedNpc = npc;
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
		trackedNpc = null;
	}

	private void addSyncRemindersIfNeeded(boolean logReminder, boolean bankReminder)
	{
		if (logReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Collection Log to sync")
				.color(new Color(255, 170, 0))
				.build());
		}
		if (bankReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Bank to scan items")
				.color(new Color(255, 170, 0))
				.build());
		}
	}
}
