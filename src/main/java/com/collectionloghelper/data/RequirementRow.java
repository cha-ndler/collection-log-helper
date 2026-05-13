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
package com.collectionloghelper.data;

import java.awt.Color;
import lombok.Value;

/**
 * A single display row in the guidance-header requirements section.
 *
 * <p>Rows are produced by {@link RequirementsChecker#buildRequirementRows} on the
 * client thread and consumed on the EDT by {@link com.collectionloghelper.ui.widget.RequirementsView}.
 */
@Value
public class RequirementRow
{
	/** The category this row belongs to. */
	Category category;

	/** Human-readable requirement label, e.g. "Dragon Slayer II" or "Agility 70". */
	String label;

	/** Human-readable state text, e.g. "COMPLETED", "level 65/70", "NOT STARTED". */
	String stateText;

	/** Display colour reflecting whether the requirement is met. */
	Color color;

	/** Whether the player currently satisfies this requirement. */
	boolean met;

	/** Requirement category — used to group rows under sub-headers in the view. */
	public enum Category
	{
		QUEST,
		SKILL,
		DIARY
	}

	// Pre-defined colours
	public static final Color COLOR_MET = new Color(80, 200, 80);
	public static final Color COLOR_IN_PROGRESS = new Color(255, 200, 0);
	public static final Color COLOR_UNMET = new Color(220, 60, 60);
}
