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

import java.lang.reflect.Constructor;
import org.junit.Test;

import static org.junit.Assert.*;

public class BossGuidanceRegistryTest
{
	private BossGuidanceRegistry buildRegistry() throws Exception
	{
		CerberusGuidance cerberusGuidance = buildCerberusGuidance();
		Constructor<BossGuidanceRegistry> ctor =
			BossGuidanceRegistry.class.getDeclaredConstructor(CerberusGuidance.class);
		ctor.setAccessible(true);
		return ctor.newInstance(cerberusGuidance);
	}

	private CerberusGuidance buildCerberusGuidance() throws Exception
	{
		Constructor<CerberusGuidance> ctor = CerberusGuidance.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		return ctor.newInstance();
	}

	@Test
	public void get_cerberusKey_returnsCerberusGuidance() throws Exception
	{
		BossGuidanceRegistry registry = buildRegistry();
		BossGuidance boss = registry.get("cerberus");
		assertNotNull("Registry must return boss guidance for 'cerberus'", boss);
		assertTrue("Boss guidance must be a CerberusGuidance", boss instanceof CerberusGuidance);
	}

	@Test
	public void get_nullKey_returnsNull() throws Exception
	{
		BossGuidanceRegistry registry = buildRegistry();
		assertNull("Registry must return null for null key", registry.get(null));
	}

	@Test
	public void get_unknownKey_returnsNull() throws Exception
	{
		BossGuidanceRegistry registry = buildRegistry();
		assertNull("Registry must return null for unknown key", registry.get("zulrah"));
	}

	@Test
	public void getAllBosses_containsCerberus() throws Exception
	{
		BossGuidanceRegistry registry = buildRegistry();
		assertTrue("'cerberus' must be in getAllBosses()", registry.getAllBosses().containsKey("cerberus"));
	}
}
