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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.overlay;

import com.collectionloghelper.CollectionLogHelperConfig;
import com.collectionloghelper.NpcHighlightStyle;
import com.collectionloghelper.ObjectHighlightStyle;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies style-selection logic: enum values are well-formed, the default
 * config methods return OUTLINE_GLOW, and all style values round-trip through
 * their toString() representation.
 */
public class HighlightStyleSelectionTest
{
	// --- NpcHighlightStyle ---

	@Test
	public void npcHighlightStyle_outlineGlowIsFirstEnumValue()
	{
		// OUTLINE_GLOW is declared first in the enum so it appears first in
		// the RuneLite config dropdown. The authoritative default is set in the
		// CollectionLogHelperConfig default method (tested separately below).
		assertEquals(NpcHighlightStyle.OUTLINE_GLOW, NpcHighlightStyle.values()[0]);
	}

	@Test
	public void npcHighlightStyle_allValuesHaveNonNullName()
	{
		for (NpcHighlightStyle style : NpcHighlightStyle.values())
		{
			assertNotNull("getName() must not be null for " + style, style.getName());
		}
	}

	@Test
	public void npcHighlightStyle_toStringMatchesName()
	{
		for (NpcHighlightStyle style : NpcHighlightStyle.values())
		{
			assertEquals(style.getName(), style.toString());
		}
	}

	@Test
	public void npcHighlightStyle_outlineGlowDisplayName()
	{
		assertEquals("Outline Glow", NpcHighlightStyle.OUTLINE_GLOW.getName());
	}

	@Test
	public void npcHighlightStyle_existingValuesPreserved()
	{
		// Backwards-compat: HULL, OUTLINE and TILE must still exist
		assertNotNull(NpcHighlightStyle.HULL);
		assertNotNull(NpcHighlightStyle.OUTLINE);
		assertNotNull(NpcHighlightStyle.TILE);
	}

	// --- ObjectHighlightStyle ---

	@Test
	public void objectHighlightStyle_outlineGlowIsFirstEnumValue()
	{
		assertEquals(ObjectHighlightStyle.OUTLINE_GLOW, ObjectHighlightStyle.values()[0]);
	}

	@Test
	public void objectHighlightStyle_allValuesHaveNonNullName()
	{
		for (ObjectHighlightStyle style : ObjectHighlightStyle.values())
		{
			assertNotNull("getName() must not be null for " + style, style.getName());
		}
	}

	@Test
	public void objectHighlightStyle_toStringMatchesName()
	{
		for (ObjectHighlightStyle style : ObjectHighlightStyle.values())
		{
			assertEquals(style.getName(), style.toString());
		}
	}

	@Test
	public void objectHighlightStyle_outlineGlowDisplayName()
	{
		assertEquals("Outline Glow", ObjectHighlightStyle.OUTLINE_GLOW.getName());
	}

	@Test
	public void objectHighlightStyle_hullPreserved()
	{
		assertNotNull(ObjectHighlightStyle.HULL);
	}

	// --- Config default verification ---

	@Test
	public void config_npcHighlightStyleDefaultIsOutlineGlow()
	{
		// The interface default method must return OUTLINE_GLOW.
		// We create an anonymous proxy that delegates to the default.
		CollectionLogHelperConfig cfg = new CollectionLogHelperConfig()
		{
		};
		assertEquals(NpcHighlightStyle.OUTLINE_GLOW, cfg.npcHighlightStyle());
	}

	@Test
	public void config_objectHighlightStyleDefaultIsOutlineGlow()
	{
		CollectionLogHelperConfig cfg = new CollectionLogHelperConfig()
		{
		};
		assertEquals(ObjectHighlightStyle.OUTLINE_GLOW, cfg.objectHighlightStyle());
	}
}
