package com.collectionloghelper.overlay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

public class CollectionLogWorldMapPoint extends WorldMapPoint
{
	private final BufferedImage image;
	private final BufferedImage smallImage;
	private final Point imageAnchorPoint;

	public CollectionLogWorldMapPoint(WorldPoint worldPoint, String name)
	{
		super(worldPoint, null);

		// Create a simple colored marker
		image = createMarkerImage(16, 16);
		smallImage = createMarkerImage(8, 8);
		imageAnchorPoint = new Point(image.getWidth() / 2, image.getHeight());

		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setName(name);
		this.setImage(image);
		this.setImagePoint(imageAnchorPoint);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(smallImage);
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(image);
		this.setImagePoint(imageAnchorPoint);
	}

	private static BufferedImage createMarkerImage(int width, int height)
	{
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0, 255, 255));
		g.fillOval(0, 0, width, height);
		g.setColor(new Color(0, 200, 200));
		g.drawOval(0, 0, width - 1, height - 1);
		g.dispose();
		return img;
	}
}
