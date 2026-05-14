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
package com.collectionloghelper.ui.widget;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;

/**
 * Pure-logic helper for E2 meta-update dating.
 *
 * <p>Given a source's {@code metaAuthoredDate} ISO 8601 string and a reference
 * "today" date, computes:
 * <ul>
 *   <li>how many months old the guidance is</li>
 *   <li>which staleness tier applies (FRESH / WARNING / STALE)</li>
 *   <li>the display label ("Meta: N months ago")</li>
 * </ul>
 *
 * <p>All state is immutable and computed at construction time. Returns
 * {@code null} from {@link #from(String, LocalDate)} when the date string is
 * absent or unparseable, so callers can easily gate on presence.
 */
public class MetaAgeBadge
{
	/** Guidance authored within the last 3 months — considered current. */
	static final Color COLOR_FRESH = new Color(40, 180, 40);
	/** Guidance authored 3-12 months ago — may be partially outdated. */
	static final Color COLOR_WARNING = new Color(200, 160, 0);
	/** Guidance authored more than 12 months ago — likely stale. */
	static final Color COLOR_STALE = new Color(200, 50, 50);

	static final String TOOLTIP_TEXT =
		"OSRS updates may have shifted the meta since this guidance was authored.";

	private static final long FRESH_MONTHS = 3;
	private static final long STALE_MONTHS = 12;

	private final long ageMonths;
	private final Color color;
	private final String label;

	private MetaAgeBadge(long ageMonths)
	{
		this.ageMonths = ageMonths;
		this.color = computeColor(ageMonths);
		this.label = buildLabel(ageMonths);
	}

	/**
	 * Parses the ISO 8601 date string and computes the age badge relative to
	 * {@code today}. Returns {@code null} if {@code metaAuthoredDate} is null,
	 * empty, or cannot be parsed as a valid ISO 8601 date.
	 *
	 * @param metaAuthoredDate ISO 8601 date string (e.g. "2024-01-15"), or {@code null}
	 * @param today            reference date for age calculation (use {@link LocalDate#now()}
	 *                         in production; inject a fixed date in tests)
	 * @return a populated badge, or {@code null} when the date is absent/invalid
	 */
	@Nullable
	public static MetaAgeBadge from(@Nullable String metaAuthoredDate, LocalDate today)
	{
		if (metaAuthoredDate == null || metaAuthoredDate.isEmpty())
		{
			return null;
		}
		try
		{
			LocalDate authored = LocalDate.parse(metaAuthoredDate);
			long months = ChronoUnit.MONTHS.between(authored, today);
			// Clamp negative values (future-dated strings) to 0
			return new MetaAgeBadge(Math.max(0, months));
		}
		catch (DateTimeParseException e)
		{
			return null;
		}
	}

	/** Number of complete months between the authored date and today. */
	public long getAgeMonths()
	{
		return ageMonths;
	}

	/** Display colour based on staleness tier. */
	public Color getColor()
	{
		return color;
	}

	/** Short display label, e.g. "Meta: 5 months ago" or "Meta: &lt;1 month ago". */
	public String getLabel()
	{
		return label;
	}

	/** Tooltip text shown on hover. */
	public String getTooltip()
	{
		return TOOLTIP_TEXT;
	}

	// -------------------------------------------------------------------------

	private static Color computeColor(long months)
	{
		if (months < FRESH_MONTHS)
		{
			return COLOR_FRESH;
		}
		if (months < STALE_MONTHS)
		{
			return COLOR_WARNING;
		}
		return COLOR_STALE;
	}

	private static String buildLabel(long months)
	{
		if (months == 0)
		{
			return "Meta: <1 month ago";
		}
		String unit = months == 1 ? "month" : "months";
		return "Meta: " + months + " " + unit + " ago";
	}
}
