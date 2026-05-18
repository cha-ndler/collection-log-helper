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
package com.collectionloghelper.guidance.bosses;

import com.collectionloghelper.data.GuidanceStep;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

/**
 * Optional escape hatch for sources that cannot be fully expressed in JSON.
 *
 * <p>When a {@link com.collectionloghelper.data.CollectionLogSource} has a
 * non-null {@code guidanceHelperKey}, the {@link BossGuidanceRegistry}
 * resolves it to an implementation of this interface. The
 * {@link com.collectionloghelper.guidance.GuidanceSequencer} then delegates
 * step generation and satisfaction checks to the boss guidance instead of
 * relying solely on the source's {@code guidanceSteps} JSON list.
 *
 * <p>This is the D-01 hybrid design: JSON stays primary; Java boss guidance
 * is the opt-in escape hatch for hard sources (dynamic re-render, arbitrary
 * varbit predicates, puzzle logic).
 */
public interface BossGuidance
{
	/**
	 * Returns the ordered list of guidance steps for this source.
	 *
	 * <p>Called once when a guidance sequence is started for the source.
	 * The returned list is used in place of the source's JSON
	 * {@code guidanceSteps}.
	 *
	 * @param client       the RuneLite client (may be null in tests)
	 * @param clientThread the RuneLite client thread for deferred callbacks
	 * @return non-null, non-empty list of steps; order is significant
	 */
	List<GuidanceStep> getSteps(Client client, ClientThread clientThread);

	/**
	 * Returns true if the given step is already satisfied given current
	 * client state, allowing the sequencer to skip it on start-up.
	 *
	 * <p>Returning {@code false} means "I don't know" — the sequencer
	 * falls back to its built-in {@link com.collectionloghelper.data.CompletionCondition}
	 * evaluation. Implementations that want to override the default
	 * satisfaction check should return {@code true} when appropriate.
	 *
	 * <p>For this pilot PR the implementation always returns {@code false}
	 * (delegate to the default sequencer evaluation). Future implementations
	 * may implement richer logic here.
	 *
	 * @param client    the RuneLite client
	 * @param step      the step to evaluate
	 * @param stepIndex 0-based index of the step in the list returned by
	 *                  {@link #getSteps}
	 * @return {@code true} if the step is definitely already satisfied;
	 *         {@code false} to fall back to default evaluation
	 */
	boolean isStepSatisfied(Client client, GuidanceStep step, int stepIndex);
}
