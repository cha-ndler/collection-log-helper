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
package com.collectionloghelper.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the {@code waypoints} field on {@link GuidanceStep} (B2 schema extension).
 *
 * <p>Verifies that:
 * <ol>
 *   <li>JSON without {@code waypoints} deserialises to {@code null} (backward-compatible).</li>
 *   <li>JSON with {@code waypoints} populates the list with correct field values.</li>
 *   <li>Waypoint order is preserved (first element, last element checks).</li>
 *   <li>All four fields (worldX, worldY, plane, radius) are deserialised correctly.</li>
 * </ol>
 */
public class GuidanceStepWaypointTest
{
	private final Gson gson = new GsonBuilder().create();

	// ── Backward-compatibility: no waypoints field ────────────────────────────

	@Test
	public void deserialise_jsonWithoutWaypoints_waypointsIsNull()
	{
		String json = "{"
			+ "\"description\":\"Walk to the dungeon\","
			+ "\"worldX\":1435,\"worldY\":9700,\"worldPlane\":0,"
			+ "\"completionDistance\":20,"
			+ "\"completionCondition\":\"ARRIVE_AT_TILE\""
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNull( step.getWaypoints(),"waypoints should be null when absent from JSON");
	}

	// ── Waypoints field present ───────────────────────────────────────────────

	@Test
	public void deserialise_jsonWithWaypoints_populatesList()
	{
		String json = "{"
			+ "\"description\":\"Travel through Cam Torum\","
			+ "\"worldX\":1435,\"worldY\":9700,\"worldPlane\":0,"
			+ "\"completionDistance\":80,"
			+ "\"completionCondition\":\"ARRIVE_AT_TILE\","
			+ "\"waypoints\":["
			+ "  {\"worldX\":1435,\"worldY\":3128,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1418,\"worldY\":9590,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1435,\"worldY\":9700,\"plane\":0,\"radius\":20}"
			+ "]"
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);

		assertNotNull( step.getWaypoints(),"waypoints should not be null");
		assertEquals(3, step.getWaypoints().size());
	}

	@Test
	public void deserialise_firstWaypoint_fieldsCorrect()
	{
		String json = "{"
			+ "\"description\":\"Travel through Cam Torum\","
			+ "\"worldX\":1435,\"worldY\":9700,\"worldPlane\":0,"
			+ "\"completionCondition\":\"ARRIVE_AT_TILE\","
			+ "\"waypoints\":["
			+ "  {\"worldX\":1435,\"worldY\":3128,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1418,\"worldY\":9590,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1435,\"worldY\":9700,\"plane\":0,\"radius\":20}"
			+ "]"
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);
		List<StepWaypoint> waypoints = step.getWaypoints();
		StepWaypoint first = waypoints.get(0);

		assertEquals(1435, first.getWorldX());
		assertEquals(3128, first.getWorldY());
		assertEquals(0, first.getPlane());
		assertEquals(10, first.getRadius());
	}

	@Test
	public void deserialise_lastWaypoint_fieldsCorrect()
	{
		String json = "{"
			+ "\"description\":\"Travel through Cam Torum\","
			+ "\"worldX\":1435,\"worldY\":9700,\"worldPlane\":0,"
			+ "\"completionCondition\":\"ARRIVE_AT_TILE\","
			+ "\"waypoints\":["
			+ "  {\"worldX\":1435,\"worldY\":3128,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1418,\"worldY\":9590,\"plane\":0,\"radius\":10},"
			+ "  {\"worldX\":1435,\"worldY\":9700,\"plane\":0,\"radius\":20}"
			+ "]"
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);
		List<StepWaypoint> waypoints = step.getWaypoints();
		StepWaypoint last = waypoints.get(2);

		assertEquals(1435, last.getWorldX());
		assertEquals(9700, last.getWorldY());
		assertEquals(0, last.getPlane());
		assertEquals(20, last.getRadius());
	}

	@Test
	public void deserialise_waypointOnUpperFloor_planeDeserialised()
	{
		String json = "{"
			+ "\"description\":\"Climb to floor 2\","
			+ "\"worldX\":3200,\"worldY\":3400,\"worldPlane\":2,"
			+ "\"completionCondition\":\"ARRIVE_AT_TILE\","
			+ "\"waypoints\":["
			+ "  {\"worldX\":3200,\"worldY\":3400,\"plane\":2,\"radius\":5}"
			+ "]"
			+ "}";

		GuidanceStep step = gson.fromJson(json, GuidanceStep.class);
		StepWaypoint wp = step.getWaypoints().get(0);

		assertEquals(2, wp.getPlane());
	}
}
