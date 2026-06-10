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
package com.collectionloghelper.sync;

import com.collectionloghelper.CollectionLogHelperConfig;
import java.lang.reflect.Method;
import net.runelite.client.config.ConfigItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Privacy-posture guards for the two external-sync features. Both submit the
 * player's RSN to a third-party server, so they must default to OFF and their
 * config descriptions must say so explicitly (Plugin Hub requirement).
 */
public class SyncPrivacyDefaultsTest
{
	private final CollectionLogHelperConfig config = new CollectionLogHelperConfig()
	{
	};

	@Test
	public void collectionLogNetImportDefaultsOff()
	{
		assertFalse(config.enableCollectionLogNetImport(),
			"collectionlog.net auto-import must default OFF — it submits the RSN to a third-party server");
	}

	@Test
	public void templeOsrsSyncDefaultsOff()
	{
		assertFalse(config.enableTempleOsrsSync(),
			"TempleOSRS auto-sync must default OFF — it submits the RSN to a third-party server");
	}

	@Test
	public void collectionLogNetImportDescriptionWarnsAboutThirdParty() throws Exception
	{
		assertDescriptionMentionsThirdParty("enableCollectionLogNetImport");
	}

	@Test
	public void templeOsrsSyncDescriptionWarnsAboutThirdParty() throws Exception
	{
		assertDescriptionMentionsThirdParty("enableTempleOsrsSync");
	}

	private static void assertDescriptionMentionsThirdParty(String methodName) throws Exception
	{
		Method method = CollectionLogHelperConfig.class.getMethod(methodName);
		ConfigItem item = method.getAnnotation(ConfigItem.class);
		assertTrue(item != null, methodName + " must carry a @ConfigItem annotation");
		String description = item.description().toLowerCase();
		assertTrue(description.contains("third-party server"),
			methodName + " description must warn that the RSN is submitted to a third-party server; was: "
				+ item.description());
	}
}
