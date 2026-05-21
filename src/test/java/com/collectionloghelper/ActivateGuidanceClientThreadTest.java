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

import com.collectionloghelper.data.CollectionLogCategory;
import com.collectionloghelper.data.CollectionLogSource;
import com.collectionloghelper.di.GuidanceModule;
import com.collectionloghelper.guidance.GuidanceOverlayCoordinator;
import java.lang.reflect.Field;
import net.runelite.client.callback.ClientThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Regression test: {@link CollectionLogHelperPlugin#activateGuidance} must dispatch
 * its work via {@link ClientThread#invokeLater} rather than executing directly on
 * the Swing EDT.  If this test fails it means the activation path has been wired
 * back to run on the calling thread, which triggers
 * {@code AssertionError: must be called on client thread} inside
 * {@link com.collectionloghelper.data.RequirementsChecker#buildRequirementRows}.
 *
 * <p>See commit c528d0ae (step-advance / skip callbacks) for the canonical pattern
 * that this fix mirrors.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ActivateGuidanceClientThreadTest
{
	@Mock
	private ClientThread clientThread;

	@Mock
	private GuidanceOverlayCoordinator guidanceCoordinator;

	private CollectionLogHelperPlugin plugin;

	@BeforeEach
	public void setUp() throws Exception
	{
		plugin = new CollectionLogHelperPlugin();
		injectField("clientThread", clientThread);
		GuidanceModule guidanceModule = new GuidanceModule(
			null, guidanceCoordinator, null, null, null, null, null, null);
		injectField("guidance", guidanceModule);
	}

	/**
	 * Calling {@code activateGuidance} from the EDT must route all work through
	 * {@code clientThread.invokeLater()} — never call {@code guidanceCoordinator}
	 * directly on the calling thread.
	 */
	@Test
	public void activateGuidance_routesWorkThroughClientThread()
	{
		CollectionLogSource source = makeMinimalSource("Test Source");

		plugin.activateGuidance(source);

		// The body must be submitted to the client thread, not executed inline.
		verify(clientThread).invokeLater(any(Runnable.class));
	}

	/**
	 * {@code guidanceCoordinator.activateGuidance} must NOT be called directly
	 * on the calling thread — it would run on the EDT and trigger the assertion
	 * inside RequirementsChecker.
	 */
	@Test
	public void activateGuidance_doesNotCallCoordinatorDirectlyOnCallingThread()
	{
		CollectionLogSource source = makeMinimalSource("Test Source");

		plugin.activateGuidance(source);

		// guidanceCoordinator must not be touched directly (only via the lambda
		// queued to the client thread).
		verifyNoInteractions(guidanceCoordinator);
	}

	// ---- helpers ----

	private void injectField(String fieldName, Object value) throws Exception
	{
		Field field = CollectionLogHelperPlugin.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private static CollectionLogSource makeMinimalSource(String name)
	{
		return new CollectionLogSource(
			name,
			CollectionLogCategory.BOSSES,
			0, 0, 0,   // worldX, worldY, worldPlane
			0, 0,      // killTimeSeconds, ironKillTimeSeconds
			null,      // locationDescription
			null,      // waypoints
			null,      // rewardType
			0.0,       // pointsPerHour
			null,      // mutuallyExclusiveSources
			0,         // rollsPerKill
			false,     // aggregated
			0,         // afkLevel
			null,      // travelTip
			0,         // npcId
			null,      // interactAction
			null,      // dialogOptions
			null,      // guidanceSteps
			null,      // guidanceHelperKey
			null,      // requirements
			0,         // cumulativeTrackItemId
			null,      // cumulativeTrackObjectIds
			0,         // cumulativeTrackThreshold
			null,      // items
			null       // metaAuthoredDate
		, null, null);
	}
}
