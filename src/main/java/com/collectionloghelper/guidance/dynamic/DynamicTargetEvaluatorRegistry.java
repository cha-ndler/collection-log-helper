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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Registry mapping evaluator key strings to {@link DynamicTargetEvaluator} instances.
 *
 * <p>When a {@link com.collectionloghelper.data.GuidanceStep} has a non-null
 * {@code dynamicTargetEvaluator} field, {@link com.collectionloghelper.guidance.GuidanceOverlayCoordinator}
 * calls {@link #get(String)} with that key and invokes the returned evaluator
 * once per game tick to obtain the current target {@link net.runelite.api.coords.WorldPoint}.
 *
 * <p>New evaluators are registered here by name.  Keys are lowercase, using
 * underscores as separators, and should be unique across the plugin
 * (e.g. {@code "wintertodt_active_brazier"}, {@code "barbarian_assault_role"}).
 *
 * <p>The registry is a singleton; Guice injects it into
 * {@link com.collectionloghelper.guidance.GuidanceOverlayCoordinator}.
 */
@Singleton
public class DynamicTargetEvaluatorRegistry
{
	/** Key for the Wintertodt nearest-active-brazier evaluator. */
	public static final String WINTERTODT_ACTIVE_BRAZIER = "wintertodt_active_brazier";

	private final Map<String, DynamicTargetEvaluator> registry;

	@Inject
	public DynamicTargetEvaluatorRegistry()
	{
		registry = new HashMap<>();
		register(WINTERTODT_ACTIVE_BRAZIER, new WintertodtBrazierEvaluator());
	}

	/**
	 * Registers an evaluator under the given key.  Replaces any existing
	 * registration for that key.
	 *
	 * @param key       the evaluator key used in guidance step JSON
	 * @param evaluator the evaluator instance
	 */
	public void register(String key, DynamicTargetEvaluator evaluator)
	{
		registry.put(key, evaluator);
	}

	/**
	 * Returns the evaluator registered under {@code key}, or {@code null} if
	 * no evaluator has been registered for that key.
	 *
	 * @param key the evaluator key from a guidance step's {@code dynamicTargetEvaluator} field
	 * @return the evaluator, or {@code null}
	 */
	@Nullable
	public DynamicTargetEvaluator get(String key)
	{
		if (key == null)
		{
			return null;
		}
		return registry.get(key);
	}
}
