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

import com.collectionloghelper.data.GuidanceStep;
import javax.annotation.Nullable;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

/**
 * Pluggable per-tick target resolver for puzzle/dynamic guidance steps.
 *
 * <p>When a {@link GuidanceStep} carries a non-null
 * {@link GuidanceStep#getDynamicTargetEvaluator()} key, the
 * {@link com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry}
 * looks up the registered evaluator by that key and calls
 * {@link #evaluate(Client, GuidanceStep)} once per game tick.  The returned
 * {@link WorldPoint} replaces the step's static {@code worldX/worldY/worldPlane}
 * for that tick's overlay update.
 *
 * <p>Evaluators run on the client thread (called from the game-tick handler).
 * Implementations may freely call any {@link Client} API without thread-safety
 * concerns.  Return {@code null} to signal that no dynamic target is available
 * for the current tick; the coordinator falls back to the step's static
 * coordinates (or no target when those are also absent).
 *
 * <p>Implementations must be stateless or externally thread-safe if shared
 * across multiple simultaneous guidance sessions (unlikely in practice, but
 * the registry stores one instance per key).
 */
public interface DynamicTargetEvaluator
{
	/**
	 * Computes the current target {@link WorldPoint} for the given guidance step.
	 *
	 * <p>Called once per game tick while the step is active.  Must complete
	 * quickly — no blocking I/O, no heavy computation.
	 *
	 * @param client the RuneLite {@link Client}, available for NPC/varbit queries
	 * @param step   the currently active guidance step (read-only)
	 * @return the computed target, or {@code null} when no dynamic target applies
	 */
	@Nullable
	WorldPoint evaluate(Client client, GuidanceStep step);
}
