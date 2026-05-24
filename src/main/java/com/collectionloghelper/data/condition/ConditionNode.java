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
package com.collectionloghelper.data.condition;

/**
 * One node in a boolean tree composed over the 11 atomic
 * {@link com.collectionloghelper.data.CompletionCondition} leaves.
 *
 * <p>Tier B1 Phase 1+2 - additive schema only. Existing 225 sources continue
 * to use the flat enum + supporting fields on
 * {@link com.collectionloghelper.data.GuidanceStep}; a step opts in to a
 * composable tree by setting its {@code conditionTree} field.
 *
 * <p>Implementations: {@link AndNode}, {@link OrNode}, {@link NotNode},
 * {@link LeafNode}. The set is intentionally closed.
 *
 * <p>See {@code docs/contributor-guide/b1-composable-conditions-scoping.md}
 * for the full design rationale (Option B - additive sibling field).
 */
public interface ConditionNode
{
	/**
	 * Evaluates the node against the supplied context. The context aggregates
	 * the live state that the existing flat-enum
	 * {@link com.collectionloghelper.guidance.CompletionChecker} already reads
	 * (player location, inventory, collection log, last varbit / chat / NPC
	 * death event).
	 *
	 * <p>Evaluation must be pure: no caches mutated, no events fired, no
	 * side effects on the context.
	 *
	 * @param ctx the snapshot of live state against which to evaluate
	 * @return true if the boolean expression rooted at this node is satisfied
	 */
	boolean evaluate(ConditionEvaluationContext ctx);
}
