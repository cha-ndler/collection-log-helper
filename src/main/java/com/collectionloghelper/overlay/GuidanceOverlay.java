/*
 * Copyright (c) 2025, Chandler
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
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.NpcHighlightStyle;
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
import javax.inject.Singleton;
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
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Singleton
public class GuidanceOverlay extends OverlayPanel
{
	private static final int MAX_PANEL_WIDTH = 200;
	private static final Dimension PANEL_PREFERRED_SIZE = new Dimension(MAX_PANEL_WIDTH, 0);
	private static final int ARROW_HEIGHT = 14;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_GAP = 5;
	private static final BasicStroke STROKE_2 = new BasicStroke(2.0f);
	private static final Font BOLD_12 = new Font(Font.DIALOG, Font.BOLD, 12);
	private static final Color TITLE_COLOR = new Color(255, 200, 0);
	private static final Color TRAVEL_COLOR = new Color(100, 200, 255);
	private static final Color ACTION_COLOR = new Color(100, 255, 100);
	private static final Color REMINDER_COLOR = new Color(255, 170, 0);
	private static final int MAX_RENDER_DISTANCE = 2400;

	private final Client client;
	private final CollectionLogHelperConfig config;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private ModelOutlineRenderer modelOutlineRenderer;

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
	private final Polygon arrowPolygon = new Polygon();
	private Color cachedOverlayColor;
	private Color cachedFillColor50;
	private Color cachedFillColor30;
	private volatile String cachedTravelLabel;
	private volatile String cachedNpcLabel;

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
		updateCachedColors(overlayColor);

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
				panelComponent.setPreferredSize(PANEL_PREFERRED_SIZE);
				return super.render(graphics);
			}
			if (hasReminder)
			{
				panelComponent.getChildren().clear();
				addSyncRemindersIfNeeded(logReminder, bankReminder);
				panelComponent.setPreferredSize(PANEL_PREFERRED_SIZE);
				return super.render(graphics);
			}
			return null;
		}

		// Skip world rendering if player is on a different plane than the target
		boolean samePlane = client.getLocalPlayer() != null
			&& client.getLocalPlayer().getWorldLocation().getPlane() == point.getPlane();

		// Tile highlight rendering
		LocalPoint localPoint = samePlane
			? LocalPoint.fromWorld(client.getTopLevelWorldView(), point) : null;
		if (localPoint == null)
		{
			// Target tile is not on screen — show compact direction panel
			if (name != null)
			{
				panelComponent.getChildren().clear();
				panelComponent.getChildren().add(TitleComponent.builder()
					.text(name)
					.color(TITLE_COLOR)
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
						.left(cachedTravelLabel != null ? cachedTravelLabel : "Travel: " + travel)
						.leftColor(TRAVEL_COLOR)
						.build());
				}
				if (action != null && !action.isEmpty())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(action)
						.leftColor(ACTION_COLOR)
						.build());
				}
				addSyncRemindersIfNeeded(logReminder, bankReminder);
				panelComponent.setPreferredSize(PANEL_PREFERRED_SIZE);
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
				net.runelite.api.WorldView wv = client.getTopLevelWorldView();
				LocalPoint playerLocal = client.getLocalPlayer() != null
					? client.getLocalPlayer().getLocalLocation() : null;
				if (wv != null)
				{
					for (NPC candidate : wv.npcs())
					{
						if (candidate != null && candidate.getId() == npcId)
						{
							// Skip NPCs too far from the player to be visible
							if (playerLocal != null)
							{
								LocalPoint candidateLocal = candidate.getLocalLocation();
								if (candidateLocal != null
									&& playerLocal.distanceTo(candidateLocal) > MAX_RENDER_DISTANCE)
								{
									continue;
								}
							}
							npc = candidate;
							this.trackedNpc = candidate;
							rebuildNpcLabel(this.interactAction, candidate);
							break;
						}
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

			OverlayUtil.renderPolygon(graphics, poly, overlayColor, cachedFillColor50,
				STROKE_2);

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
		NpcHighlightStyle style = config.npcHighlightStyle();

		switch (style)
		{
			case OUTLINE:
				modelOutlineRenderer.drawOutline(npc, 2, overlayColor, 4);
				break;
			case TILE:
			{
				LocalPoint tileLocal = npc.getLocalLocation();
				if (tileLocal != null)
				{
					Polygon tilePoly = Perspective.getCanvasTilePoly(client, tileLocal);
					if (tilePoly != null)
					{
						OverlayUtil.renderPolygon(graphics, tilePoly, overlayColor, cachedFillColor50,
							STROKE_2);
					}
				}
				break;
			}
			case HULL:
			default:
			{
				Shape hull = npc.getConvexHull();
				if (hull != null)
				{
					graphics.setColor(cachedFillColor30);
					graphics.fill(hull);
					graphics.setColor(overlayColor);
					graphics.setStroke(STROKE_2);
					graphics.draw(hull);
				}
				break;
			}
		}

		// Draw downward-pointing arrow above the NPC (reuse hull from HULL case if available)
		Shape arrowHull = npc.getConvexHull();
		if (arrowHull != null)
		{
			Rectangle bounds = arrowHull.getBounds();
			int arrowX = (int) bounds.getCenterX();
			int arrowTipY = (int) bounds.getMinY() - ARROW_GAP;
			renderDirectionArrow(graphics, arrowX, arrowTipY, overlayColor);

			// Show tooltip when mouse hovers over the NPC hull
			String builtTooltip = OverlayTooltipHelper.buildTooltip(locDesc, action);
			if (builtTooltip != null)
			{
				Point mousePos = client.getMouseCanvasPosition();
				if (mousePos != null && arrowHull.contains(mousePos.getX(), mousePos.getY()))
				{
					tooltipManager.add(new Tooltip(builtTooltip));
				}
			}
		}

		// Render action text above the NPC and arrow
		LocalPoint npcLocal = npc.getLocalLocation();
		if (npcLocal != null)
		{
			String label = cachedNpcLabel != null ? cachedNpcLabel : npc.getName();

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

		arrowPolygon.reset();
		arrowPolygon.addPoint(x, tipY);
		arrowPolygon.addPoint(x + halfW, topY);
		arrowPolygon.addPoint(x - halfW, topY);

		// Black outline
		graphics.setColor(Color.BLACK);
		graphics.setStroke(STROKE_2);
		graphics.drawPolygon(arrowPolygon);

		// Colored fill
		graphics.setColor(color);
		graphics.fillPolygon(arrowPolygon);
	}

	/**
	 * Renders text with a dark outline for readability against any background.
	 */
	private void renderOutlinedText(Graphics2D graphics, Point point, String text, Color color)
	{
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(BOLD_12);
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

	private void updateCachedColors(Color overlayColor)
	{
		if (!overlayColor.equals(cachedOverlayColor))
		{
			cachedOverlayColor = overlayColor;
			cachedFillColor50 = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 50);
			cachedFillColor30 = new Color(overlayColor.getRed(), overlayColor.getGreen(),
				overlayColor.getBlue(), 30);
		}
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
		this.cachedTravelLabel = (travelTip != null && !travelTip.isEmpty())
			? "Travel: " + travelTip : null;
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
		rebuildNpcLabel(action, this.trackedNpc);
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
		rebuildNpcLabel(this.interactAction, npc);
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
		cachedTravelLabel = null;
		cachedNpcLabel = null;
	}

	private void rebuildNpcLabel(String action, NPC npc)
	{
		if (npc != null)
		{
			String npcName = npc.getName();
			cachedNpcLabel = (action != null)
				? action + " " + npcName
				: npcName;
		}
		else
		{
			cachedNpcLabel = null;
		}
	}

	private void addSyncRemindersIfNeeded(boolean logReminder, boolean bankReminder)
	{
		if (logReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Collection Log to sync")
				.color(REMINDER_COLOR)
				.build());
		}
		if (bankReminder)
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Open Bank to scan items")
				.color(REMINDER_COLOR)
				.build());
		}
	}
}
