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

/**
 * Converts incoming game events (NPC death, NPC interaction, chat message,
 * varbit change) into a small {@link EventOutcome} that tells the
 * {@link GuidanceSequencer} whether the current step is satisfied.
 *
 * <p>The adapter is stateless apart from the per-step chat regex cache,
 * which is owned by the sequencer and threaded through {@link #onChatMessage}.
 * The sequencer remains the sole owner of step-index mutation; this class
 * never advances the sequence directly.
 */
public class SequencerEventAdapter
{
	private final CompletionChecker completionChecker;

	public SequencerEventAdapter(CompletionChecker completionChecker)
	{
		this.completionChecker = completionChecker;
	}

	/**
	 * Outcome of a single event evaluation against the active step.
	 *
	 * <p>When {@code satisfied} is true, {@code completionLogMessage} carries
	 * a fully-formatted log line that the sequencer should emit before
	 * advancing. {@code chatCache} is only populated by
	 * {@link #onChatMessage}; other event types leave it null.
	 */
	public static final class EventOutcome
	{
		private final boolean satisfied;
		private final String completionLogMessage;
		private final CompletionChecker.ChatPatternCache chatCache;

		private EventOutcome(boolean satisfied, String completionLogMessage,
			CompletionChecker.ChatPatternCache chatCache)
		{
			this.satisfied = satisfied;
			this.completionLogMessage = completionLogMessage;
			this.chatCache = chatCache;
		}

		public boolean satisfied()
		{
			return satisfied;
		}

		public String completionLogMessage()
		{
			return completionLogMessage;
		}

		public CompletionChecker.ChatPatternCache chatCache()
		{
			return chatCache;
		}

		private static final EventOutcome NOT_SATISFIED = new EventOutcome(false, null, null);
	}

	/**
	 * Evaluates the ACTOR_DEATH condition against the given NPC id.
	 */
	public EventOutcome onNpcDeath(GuidanceStep step, int currentStepNumber, int npcId)
	{
		if (!completionChecker.isNpcDeathSatisfying(step, npcId))
		{
			return EventOutcome.NOT_SATISFIED;
		}
		return new EventOutcome(true,
			"Step " + currentStepNumber + " complete (ACTOR_DEATH: " + npcId + ")",
			null);
	}

	/**
	 * Evaluates the NPC_TALKED_TO condition against the given NPC id.
	 */
	public EventOutcome onNpcInteracted(GuidanceStep step, int currentStepNumber, int npcId)
	{
		if (!completionChecker.isNpcInteractedSatisfying(step, npcId))
		{
			return EventOutcome.NOT_SATISFIED;
		}
		return new EventOutcome(true,
			"Step " + currentStepNumber + " complete (NPC_TALKED_TO: " + npcId + ")",
			null);
	}

	/**
	 * Evaluates the CHAT_MESSAGE_RECEIVED regex condition. The returned
	 * outcome always carries the (possibly refreshed) chat pattern cache;
	 * callers must store it for the next invocation regardless of
	 * {@code satisfied}.
	 */
	public EventOutcome onChatMessage(GuidanceStep step, int currentStepNumber, String message,
		CompletionChecker.ChatPatternCache cache)
	{
		CompletionChecker.ChatMatchResult chatResult = completionChecker.evaluateChatMessage(step, message, cache);
		if (!chatResult.matched())
		{
			return new EventOutcome(false, null, chatResult.cache());
		}
		return new EventOutcome(true,
			"Step " + currentStepNumber + " complete (CHAT_MESSAGE_RECEIVED: matched '"
				+ step.getCompletionChatPattern() + "')",
			chatResult.cache());
	}

	/**
	 * Evaluates the VARBIT_AT_LEAST condition against the given varbit id and value.
	 */
	public EventOutcome onVarbitChanged(GuidanceStep step, int currentStepNumber, int varbitId, int value)
	{
		if (!completionChecker.isVarbitAtLeastSatisfying(step, varbitId, value))
		{
			return EventOutcome.NOT_SATISFIED;
		}
		return new EventOutcome(true,
			"Step " + currentStepNumber + " complete (VARBIT_AT_LEAST: varbit "
				+ varbitId + " = " + value + " >= " + step.getCompletionVarbitValue() + ")",
			null);
	}
}
