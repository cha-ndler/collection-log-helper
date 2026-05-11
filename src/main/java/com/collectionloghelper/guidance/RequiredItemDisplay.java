/*
 * Copyright (c) 2025, cha-ndler
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
package com.collectionloghelper.guidance;

import java.awt.Color;
import java.util.Objects;

/**
 * Immutable display row describing a single required item's availability
 * for the active guidance step.
 *
 * <p>Resolved by {@link RequiredItemResolver} at step-transition time and
 * pushed to both the side panel (StepProgressView) and the in-game overlay
 * (GuidanceOverlay) so the render path never touches inventory or bank state.
 *
 * <p>Color constants are shared by both render targets so a "missing" badge
 * looks the same in the panel and the in-game overlay.
 */
public final class RequiredItemDisplay
{
	/** Item is in inventory or equipped on the player. */
	public static final Color COLOR_HELD = new Color(40, 180, 40);
	/** Item is not on the player but was seen in the last bank scan. */
	public static final Color COLOR_IN_BANK = new Color(200, 180, 40);
	/** Item is not held anywhere known to the plugin. */
	public static final Color COLOR_MISSING = new Color(200, 40, 40);

	public enum Status
	{
		HELD,
		IN_BANK,
		MISSING
	}

	private final String name;
	private final Status status;

	public RequiredItemDisplay(String name, Status status)
	{
		this.name = Objects.requireNonNull(name, "name");
		this.status = Objects.requireNonNull(status, "status");
	}

	public String getName()
	{
		return name;
	}

	public Status getStatus()
	{
		return status;
	}

	/** Returns the color associated with this row's status. */
	public Color getStatusColor()
	{
		switch (status)
		{
			case HELD:
				return COLOR_HELD;
			case IN_BANK:
				return COLOR_IN_BANK;
			case MISSING:
			default:
				return COLOR_MISSING;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof RequiredItemDisplay))
		{
			return false;
		}
		RequiredItemDisplay other = (RequiredItemDisplay) o;
		return status == other.status && name.equals(other.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, status);
	}

	@Override
	public String toString()
	{
		return "RequiredItemDisplay{name='" + name + "', status=" + status + '}';
	}
}
