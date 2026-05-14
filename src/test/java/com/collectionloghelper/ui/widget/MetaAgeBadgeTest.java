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

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link MetaAgeBadge}.
 *
 * <p>All tests inject a fixed {@code today} date so colour-threshold assertions
 * are deterministic regardless of when the suite runs.
 *
 * <p>Coverage:
 * <ul>
 *   <li>ISO 8601 parsing — valid dates, malformed strings, future dates</li>
 *   <li>Colour-coding age thresholds (green &lt;3 mo, yellow 3-12 mo, red &gt;12 mo)</li>
 *   <li>Null-safety — null / empty {@code metaAuthoredDate}</li>
 *   <li>Label formatting — singular/plural month display</li>
 * </ul>
 */
public class MetaAgeBadgeTest
{
	/** Fixed reference date for all threshold tests: 2026-06-01. */
	private static final LocalDate TODAY = LocalDate.of(2026, 6, 1);

	// =========================================================================
	// Null / empty safety
	// =========================================================================

	@Test
	public void from_nullDate_returnsNull()
	{
		MetaAgeBadge badge = MetaAgeBadge.from(null, TODAY);
		assertNull(badge);
	}

	@Test
	public void from_emptyString_returnsNull()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("", TODAY);
		assertNull(badge);
	}

	// =========================================================================
	// ISO 8601 parsing
	// =========================================================================

	@Test
	public void from_validIso8601_returnsNonNull()
	{
		// 1 month before TODAY
		MetaAgeBadge badge = MetaAgeBadge.from("2026-05-01", TODAY);
		assertNotNull(badge);
	}

	@Test
	public void from_malformedDate_notIso8601_returnsNull()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("01/06/2026", TODAY);
		assertNull(badge);
	}

	@Test
	public void from_malformedDate_partialString_returnsNull()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-06", TODAY);
		assertNull(badge);
	}

	@Test
	public void from_malformedDate_randomText_returnsNull()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("not-a-date", TODAY);
		assertNull(badge);
	}

	@Test
	public void from_futureDate_returnsZeroAgeMonths()
	{
		// Date in the future should clamp to 0, not throw
		MetaAgeBadge badge = MetaAgeBadge.from("2030-01-01", TODAY);
		assertNotNull(badge);
		assertEquals(0, badge.getAgeMonths());
	}

	// =========================================================================
	// Colour-coding thresholds (mock today = 2026-06-01)
	// =========================================================================

	@Test
	public void color_zeroMonths_isFresh()
	{
		// Same month as today
		MetaAgeBadge badge = MetaAgeBadge.from("2026-06-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_FRESH, badge.getColor());
	}

	@Test
	public void color_oneMonth_isFresh()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-05-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_FRESH, badge.getColor());
	}

	@Test
	public void color_twoMonths_isFresh()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-04-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_FRESH, badge.getColor());
	}

	@Test
	public void color_threeMonths_isWarning()
	{
		// Exactly 3 months old — first warning tier boundary
		MetaAgeBadge badge = MetaAgeBadge.from("2026-03-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_WARNING, badge.getColor());
	}

	@Test
	public void color_sixMonths_isWarning()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2025-12-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_WARNING, badge.getColor());
	}

	@Test
	public void color_elevenMonths_isWarning()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2025-07-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_WARNING, badge.getColor());
	}

	@Test
	public void color_twelveMonths_isStale()
	{
		// Exactly 12 months old — crosses stale boundary
		MetaAgeBadge badge = MetaAgeBadge.from("2025-06-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_STALE, badge.getColor());
	}

	@Test
	public void color_twentyFourMonths_isStale()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2024-06-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.COLOR_STALE, badge.getColor());
	}

	// =========================================================================
	// Age month count
	// =========================================================================

	@Test
	public void ageMonths_exactlyFiveMonthsAgo()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-01-01", TODAY);
		assertNotNull(badge);
		assertEquals(5, badge.getAgeMonths());
	}

	@Test
	public void ageMonths_exactlyTwelveMonthsAgo()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2025-06-01", TODAY);
		assertNotNull(badge);
		assertEquals(12, badge.getAgeMonths());
	}

	// =========================================================================
	// Label formatting
	// =========================================================================

	@Test
	public void label_zeroMonths_showsLessThanOne()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-06-01", TODAY);
		assertNotNull(badge);
		assertEquals("Meta: <1 month ago", badge.getLabel());
	}

	@Test
	public void label_oneMonth_singular()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-05-01", TODAY);
		assertNotNull(badge);
		assertEquals("Meta: 1 month ago", badge.getLabel());
	}

	@Test
	public void label_twoMonths_plural()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2026-04-01", TODAY);
		assertNotNull(badge);
		assertEquals("Meta: 2 months ago", badge.getLabel());
	}

	@Test
	public void label_twelveMonths_plural()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2025-06-01", TODAY);
		assertNotNull(badge);
		assertEquals("Meta: 12 months ago", badge.getLabel());
	}

	// =========================================================================
	// Tooltip text
	// =========================================================================

	@Test
	public void tooltip_alwaysTheCanonicalString()
	{
		MetaAgeBadge badge = MetaAgeBadge.from("2025-06-01", TODAY);
		assertNotNull(badge);
		assertEquals(MetaAgeBadge.TOOLTIP_TEXT, badge.getTooltip());
		assertEquals(
			"OSRS updates may have shifted the meta since this guidance was authored.",
			badge.getTooltip());
	}
}
