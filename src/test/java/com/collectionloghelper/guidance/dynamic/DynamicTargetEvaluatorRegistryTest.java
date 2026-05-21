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
package com.collectionloghelper.guidance.dynamic;

import com.collectionloghelper.guidance.DynamicTargetEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Unit tests for {@link DynamicTargetEvaluatorRegistry}.
 *
 * <p>Verified:
 * <ol>
 *   <li>The registry is populated at construction with the Wintertodt pilot evaluator.</li>
 *   <li>{@link DynamicTargetEvaluatorRegistry#get(String)} returns the registered evaluator.</li>
 *   <li>{@link DynamicTargetEvaluatorRegistry#get(String)} returns null for an unknown key.</li>
 *   <li>{@link DynamicTargetEvaluatorRegistry#get(String)} returns null for a null key.</li>
 *   <li>{@link DynamicTargetEvaluatorRegistry#register(String, DynamicTargetEvaluator)} replaces an existing entry.</li>
 * </ol>
 */
public class DynamicTargetEvaluatorRegistryTest
{
	private DynamicTargetEvaluatorRegistry registry;

	@BeforeEach
	public void setUp()
	{
		registry = new DynamicTargetEvaluatorRegistry();
	}

	@Test
	public void get_wintertodtKey_returnsEvaluator()
	{
		DynamicTargetEvaluator evaluator = registry.get(DynamicTargetEvaluatorRegistry.WINTERTODT_ACTIVE_BRAZIER);
		assertNotNull( evaluator,"Wintertodt evaluator must be registered at construction");
	}

	@Test
	public void get_wintertodtKey_returnsWintertodtBrazierEvaluatorInstance()
	{
		DynamicTargetEvaluator evaluator = registry.get(DynamicTargetEvaluatorRegistry.WINTERTODT_ACTIVE_BRAZIER);
		assertNotNull(evaluator);
		assertEquals("WintertodtBrazierEvaluator",
			evaluator.getClass().getSimpleName());
	}

	@Test
	public void get_unknownKey_returnsNull()
	{
		assertNull( registry.get("does_not_exist"),"Unknown key must return null");
	}

	@Test
	public void get_nullKey_returnsNull()
	{
		assertNull( registry.get(null),"Null key must return null");
	}

	@Test
	public void register_replacesExistingEntry()
	{
		DynamicTargetEvaluator replacement = (client, step) -> null;
		registry.register(DynamicTargetEvaluatorRegistry.WINTERTODT_ACTIVE_BRAZIER, replacement);
		assertSame(
			replacement, registry.get(DynamicTargetEvaluatorRegistry.WINTERTODT_ACTIVE_BRAZIER),"register() must replace the existing evaluator");
	}

	@Test
	public void register_newKey_isRetrievable()
	{
		DynamicTargetEvaluator evaluator = (client, step) -> null;
		registry.register("new_puzzle_evaluator", evaluator);
		assertSame(evaluator, registry.get("new_puzzle_evaluator"));
	}

	// ── Helper ────────────────────────────────────────────────────────────────

	private static void assertEquals(String expected, String actual)
	{
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
	}
}
