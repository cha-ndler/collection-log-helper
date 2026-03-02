package com.collectionloghelper.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

		image = createPinImage();
		smallImage = createEdgeDotImage();
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

	private static BufferedImage createPinImage()
	{
		int width = 32;
		int height = 36;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Color pinColor = new Color(0xFF, 0x45, 0x00); // OrangeRed
		int headDiameter = 22;
		int headX = (width - headDiameter) / 2;
		int headY = 2;
		int headCenterY = headY + headDiameter / 2;

		// Pin point triangle
		int[] triX = {width / 2 - 9, width / 2 + 9, width / 2};
		int[] triY = {headCenterY, headCenterY, height - 1};

		// White border: draw larger shapes underneath
		g.setColor(Color.WHITE);
		g.fillOval(headX - 2, headY - 2, headDiameter + 4, headDiameter + 4);
		g.setStroke(new BasicStroke(3.0f));
		g.drawPolygon(triX, triY, 3);

		// Fill pin body
		g.setColor(pinColor);
		g.fillOval(headX, headY, headDiameter, headDiameter);
		g.fillPolygon(triX, triY, 3);

		// White outline on circle
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(2.0f));
		g.drawOval(headX, headY, headDiameter, headDiameter);

		// White center dot
		int dotDiam = 8;
		g.setColor(Color.WHITE);
		g.fillOval((width - dotDiam) / 2, headY + (headDiameter - dotDiam) / 2, dotDiam, dotDiam);

		g.dispose();
		return img;
	}

	private static BufferedImage createEdgeDotImage()
	{
		int size = 14;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Black outline
		g.setColor(Color.BLACK);
		g.fillOval(0, 0, size, size);

		// Orange fill
		g.setColor(new Color(0xFF, 0x8C, 0x00)); // DarkOrange
		g.fillOval(2, 2, size - 4, size - 4);

		// White center
		g.setColor(Color.WHITE);
		g.fillOval(4, 4, size - 8, size - 8);

		g.dispose();
		return img;
	}
}
