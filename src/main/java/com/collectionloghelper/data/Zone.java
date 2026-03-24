package com.collectionloghelper.data;

import net.runelite.api.coords.WorldPoint;
import lombok.Getter;

@Getter
public class Zone
{
	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	private final int plane;

	public Zone(int minX, int minY, int maxX, int maxY, int plane)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.plane = plane;
	}

	public boolean contains(WorldPoint point)
	{
		return point != null
			&& point.getPlane() == plane
			&& point.getX() >= minX && point.getX() <= maxX
			&& point.getY() >= minY && point.getY() <= maxY;
	}
}
