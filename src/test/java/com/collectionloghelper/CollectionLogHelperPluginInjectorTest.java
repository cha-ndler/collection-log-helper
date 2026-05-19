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
package com.collectionloghelper;

import com.collectionloghelper.player.DiaryTierState;
import com.collectionloghelper.player.DiaryTierStateImpl;
import com.collectionloghelper.player.EquippedItemState;
import com.collectionloghelper.player.EquippedItemStateImpl;
import com.collectionloghelper.player.PohTeleportInventory;
import com.collectionloghelper.player.PohTeleportInventoryImpl;
import com.collectionloghelper.player.SkillCapePerkState;
import com.collectionloghelper.player.SkillCapePerkStateImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.runelite.api.Client;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Regression test for the Plugin-Hub-blocking class of bug where an interface
 * dependency is injected somewhere in the plugin's object graph but no
 * concrete binding is registered (no module {@code bind(I.class).to(Impl.class)}
 * and no {@code @ImplementedBy} on the interface).
 *
 * <p>The original failure mode was a runtime {@code PluginInstantiationException}
 * caused by four interfaces ({@link DiaryTierState}, {@link EquippedItemState},
 * {@link PohTeleportInventory}, {@link SkillCapePerkState}) being injected into
 * {@code PlayerCapabilityDebugOverlay} without bindings. Existing tests passed
 * because they constructed the {@code Impl}s directly or bound them by hand;
 * the real Guice injector had no such hint and failed to start the plugin.
 *
 * <p>This test asks Guice to resolve each interface against a minimal module
 * that only binds the leaf-level {@link Client} dependency the four
 * {@code Impl} constructors need. If any interface lacks a binding or
 * {@code @ImplementedBy} annotation, the call to
 * {@link Injector#getInstance(Class)} throws {@code ConfigurationException}
 * and the test fails — at CI time, not at Plugin-Hub-submission time.
 */
public class CollectionLogHelperPluginInjectorTest
{
	/**
	 * Minimal module that binds only what the four player-state {@code Impl}
	 * constructors actually consume. The interfaces themselves are
	 * intentionally NOT bound here — resolution must come from
	 * {@code @ImplementedBy} on the interface.
	 */
	private static Module minimalModule()
	{
		return new AbstractModule()
		{
			@Override
			protected void configure()
			{
				bind(Client.class).toInstance(mock(Client.class));
				bind(CollectionLogHelperConfig.class).toInstance(mock(CollectionLogHelperConfig.class));
			}
		};
	}

	@Test
	public void diaryTierState_resolvesToImpl()
	{
		Injector injector = Guice.createInjector(minimalModule());

		DiaryTierState instance = injector.getInstance(DiaryTierState.class);

		assertNotNull("DiaryTierState must resolve via @ImplementedBy", instance);
		assertTrue(
			"DiaryTierState must resolve to DiaryTierStateImpl",
			instance instanceof DiaryTierStateImpl);
	}

	@Test
	public void equippedItemState_resolvesToImpl()
	{
		Injector injector = Guice.createInjector(minimalModule());

		EquippedItemState instance = injector.getInstance(EquippedItemState.class);

		assertNotNull("EquippedItemState must resolve via @ImplementedBy", instance);
		assertTrue(
			"EquippedItemState must resolve to EquippedItemStateImpl",
			instance instanceof EquippedItemStateImpl);
	}

	@Test
	public void pohTeleportInventory_resolvesToImpl()
	{
		Injector injector = Guice.createInjector(minimalModule());

		PohTeleportInventory instance = injector.getInstance(PohTeleportInventory.class);

		assertNotNull("PohTeleportInventory must resolve via @ImplementedBy", instance);
		assertTrue(
			"PohTeleportInventory must resolve to PohTeleportInventoryImpl",
			instance instanceof PohTeleportInventoryImpl);
	}

	@Test
	public void skillCapePerkState_resolvesToImpl()
	{
		Injector injector = Guice.createInjector(minimalModule());

		SkillCapePerkState instance = injector.getInstance(SkillCapePerkState.class);

		assertNotNull("SkillCapePerkState must resolve via @ImplementedBy", instance);
		assertTrue(
			"SkillCapePerkState must resolve to SkillCapePerkStateImpl",
			instance instanceof SkillCapePerkStateImpl);
	}

	/**
	 * Belt-and-braces: a single injector resolves all four player-state
	 * interfaces in one shot. Catches the case where one binding fails even
	 * though the others succeed.
	 */
	@Test
	public void allPlayerStateInterfaces_resolveTogether()
	{
		Injector injector = Guice.createInjector(minimalModule());

		assertNotNull(injector.getInstance(DiaryTierState.class));
		assertNotNull(injector.getInstance(EquippedItemState.class));
		assertNotNull(injector.getInstance(PohTeleportInventory.class));
		assertNotNull(injector.getInstance(SkillCapePerkState.class));
	}
}
