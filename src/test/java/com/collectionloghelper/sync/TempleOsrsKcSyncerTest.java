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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TempleOsrsKcSyncer}.
 *
 * <p>All tests use mocked HTTP responses — the real TempleOSRS API is never hit.
 */
public class TempleOsrsKcSyncerTest
{
	private OkHttpClient mockClient;
	private TempleOsrsKcSyncer syncer;
	private Gson gson;

	@BeforeEach
	public void setUp()
	{
		mockClient = mock(OkHttpClient.class);
		gson = new Gson();
		syncer = new TempleOsrsKcSyncer(mockClient, gson);
	}

	// ========================================================================
	// parseResponse — valid JSON
	// ========================================================================

	@Test
	public void parseResponse_validData_returnsMappedKc()
	{
		String json = "{\"data\": {\"Zulrah\": 500, \"Vorkath\": 300}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(500, (int) result.getKcBySource().get("Zulrah"));
		assertEquals(300, (int) result.getKcBySource().get("Vorkath"));
	}

	@Test
	public void parseResponse_explicitNameOverride_mapsCorrectly()
	{
		// "Barrows Chests" in TempleOSRS -> "Barrows" in CLH
		String json = "{\"data\": {\"Barrows Chests\": 150}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertNotNull(result.getKcBySource().get("Barrows"));
		assertEquals(150, (int) result.getKcBySource().get("Barrows"));
		assertNull(
			result.getKcBySource().get("Barrows Chests"),"Should not contain raw TempleOSRS key");
	}

	@Test
	public void parseResponse_theNightmare_mapsCorrectly()
	{
		String json = "{\"data\": {\"Nightmare\": 75}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(75, (int) result.getKcBySource().get("The Nightmare"));
	}

	@Test
	public void parseResponse_theLeviathan_mapsCorrectly()
	{
		String json = "{\"data\": {\"Leviathan\": 42}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(42, (int) result.getKcBySource().get("The Leviathan"));
	}

	@Test
	public void parseResponse_theMimic_mapsCorrectly()
	{
		String json = "{\"data\": {\"Mimic\": 3}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(3, (int) result.getKcBySource().get("The Mimic"));
	}

	@Test
	public void parseResponse_theWhisperer_mapsCorrectly()
	{
		String json = "{\"data\": {\"Whisperer\": 200}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(200, (int) result.getKcBySource().get("The Whisperer"));
	}

	@Test
	public void parseResponse_krilTsutsaroth_identityMapping()
	{
		// K'ril Tsutsaroth name is the same in TempleOSRS and CLH — identity fallback
		String json = "{\"data\": {\"K'ril Tsutsaroth\": 99}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(99, (int) result.getKcBySource().get("K'ril Tsutsaroth"));
	}

	@Test
	public void parseResponse_zeroKcSkipped()
	{
		// Zero KC entries should be silently skipped
		String json = "{\"data\": {\"Zulrah\": 0, \"Vorkath\": 300}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertNull( result.getKcBySource().get("Zulrah"),"Zero KC should not be stored");
		assertEquals(300, (int) result.getKcBySource().get("Vorkath"));
	}

	@Test
	public void parseResponse_multipleEntries_allMapped()
	{
		String json = "{\"data\": {"
			+ "\"Abyssal Sire\": 100,"
			+ "\"Alchemical Hydra\": 200,"
			+ "\"Cerberus\": 300,"
			+ "\"Corporeal Beast\": 50"
			+ "}}";

		SyncResult result = syncer.parseResponse(json);

		assertTrue(result.isSuccess());
		assertEquals(4, result.getKcBySource().size());
		assertEquals(100, (int) result.getKcBySource().get("Abyssal Sire"));
		assertEquals(200, (int) result.getKcBySource().get("Alchemical Hydra"));
		assertEquals(300, (int) result.getKcBySource().get("Cerberus"));
		assertEquals(50, (int) result.getKcBySource().get("Corporeal Beast"));
	}

	// ========================================================================
	// parseResponse — failure modes
	// ========================================================================

	@Test
	public void parseResponse_missingDataField_returnsFailure()
	{
		String json = "{\"error\": \"Player not found\"}";

		SyncResult result = syncer.parseResponse(json);

		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
		assertTrue(result.getErrorMessage().contains("'data'"));
	}

	@Test
	public void parseResponse_invalidJson_returnsFailure()
	{
		String json = "not valid json {{{";

		SyncResult result = syncer.parseResponse(json);

		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	public void parseResponse_nullJson_returnsFailure()
	{
		// Gson returns null object for "null" JSON
		SyncResult result = syncer.parseResponse("null");

		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
	}

	@Test
	public void parseResponse_dataIsArray_returnsFailure()
	{
		String json = "{\"data\": [1, 2, 3]}";

		SyncResult result = syncer.parseResponse(json);

		assertFalse(result.isSuccess());
		assertNotNull(result.getErrorMessage());
		assertTrue(result.getErrorMessage().contains("not an object"));
	}

	// ========================================================================
	// mapActivitiesToSources — non-numeric / skipped entries
	// ========================================================================

	@Test
	public void mapActivitiesToSources_nonNumericValue_skipped()
	{
		JsonObject data = gson.fromJson("{\"Zulrah\": \"hundred\", \"Vorkath\": 10}", JsonObject.class);

		SyncResult result = syncer.mapActivitiesToSources(data);

		assertTrue(result.isSuccess());
		assertEquals(1, result.getKcBySource().size());
		assertEquals(1, result.getSkippedCount());
	}

	@Test
	public void mapActivitiesToSources_nonPrimitiveValue_skipped()
	{
		JsonObject data = gson.fromJson("{\"Zulrah\": {\"kc\": 100}, \"Vorkath\": 5}", JsonObject.class);

		SyncResult result = syncer.mapActivitiesToSources(data);

		assertTrue(result.isSuccess());
		assertEquals(1, result.getKcBySource().size());
		assertEquals(1, result.getSkippedCount());
		assertEquals(5, (int) result.getKcBySource().get("Vorkath"));
	}

	@Test
	public void mapActivitiesToSources_emptyData_returnsEmptySuccess()
	{
		JsonObject data = new JsonObject();

		SyncResult result = syncer.mapActivitiesToSources(data);

		assertTrue(result.isSuccess());
		assertTrue(result.getKcBySource().isEmpty());
		assertEquals(0, result.getSkippedCount());
	}

	@Test
	public void mapActivitiesToSources_phosanis_separateEntry()
	{
		// Phosani's Nightmare is a separate CLH source from The Nightmare
		JsonObject data = gson.fromJson(
			"{\"Nightmare\": 50, \"Phosani's Nightmare\": 100}", JsonObject.class);

		SyncResult result = syncer.mapActivitiesToSources(data);

		assertTrue(result.isSuccess());
		assertTrue(result.getKcBySource().containsKey("The Nightmare"));
		assertTrue(result.getKcBySource().containsKey("Phosani's Nightmare"));
		assertEquals(50, (int) result.getKcBySource().get("The Nightmare"));
		assertEquals(100, (int) result.getKcBySource().get("Phosani's Nightmare"));
	}

	// ========================================================================
	// resolveClhName
	// ========================================================================

	@Test
	public void resolveClhName_explicitOverride_returnsMappedName()
	{
		assertEquals("Barrows", syncer.resolveClhName("Barrows Chests"));
		assertEquals("The Nightmare", syncer.resolveClhName("Nightmare"));
		assertEquals("The Leviathan", syncer.resolveClhName("Leviathan"));
		assertEquals("The Mimic", syncer.resolveClhName("Mimic"));
		assertEquals("The Whisperer", syncer.resolveClhName("Whisperer"));
	}

	@Test
	public void resolveClhName_noOverride_returnsIdentity()
	{
		assertEquals("Zulrah", syncer.resolveClhName("Zulrah"));
		assertEquals("K'ril Tsutsaroth", syncer.resolveClhName("K'ril Tsutsaroth"));
		assertEquals("Chambers of Xeric", syncer.resolveClhName("Chambers of Xeric"));
		assertEquals("SomeUnknownBoss", syncer.resolveClhName("SomeUnknownBoss"));
	}

	// ========================================================================
	// doSync — HTTP-level failure modes (mocked OkHttpClient)
	// ========================================================================

	@Test
	public void doSync_emptyUsername_returnsFailure()
	{
		SyncResult result = syncer.doSync("");

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("empty"));
	}

	@Test
	public void doSync_nullUsername_returnsFailure()
	{
		SyncResult result = syncer.doSync(null);

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("empty"));
	}

	@Test
	public void doSync_http404_returnsFailure() throws IOException
	{
		mockHttpResponse(404, "");

		SyncResult result = syncer.doSync("PlayerNotFound");

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("404"));
	}

	@Test
	public void doSync_http500_returnsFailure() throws IOException
	{
		mockHttpResponse(500, "Internal Server Error");

		SyncResult result = syncer.doSync("TestPlayer");

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("500"));
	}

	@Test
	public void doSync_networkException_returnsFailure() throws IOException
	{
		Call mockCall = mock(Call.class);
		when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
		when(mockCall.execute()).thenThrow(new IOException("Connection refused"));

		SyncResult result = syncer.doSync("TestPlayer");

		assertFalse(result.isSuccess());
		assertTrue(result.getErrorMessage().contains("Network error"));
	}

	@Test
	public void doSync_validResponse_returnsMappedKc() throws IOException
	{
		String json = "{\"data\": {\"Zulrah\": 999, \"Barrows Chests\": 42}}";
		mockHttpResponse(200, json);

		SyncResult result = syncer.doSync("TestPlayer");

		assertTrue(result.isSuccess());
		assertEquals(999, (int) result.getKcBySource().get("Zulrah"));
		assertEquals(42, (int) result.getKcBySource().get("Barrows"));
	}

	@Test
	public void doSync_emptyResponseBody_returnsFailure() throws IOException
	{
		mockHttpResponse(200, "");

		SyncResult result = syncer.doSync("TestPlayer");

		assertFalse(result.isSuccess());
	}

	// ========================================================================
	// SourceKcStore integration
	// ========================================================================

	@Test
	public void sourceKcStore_updateAndGet_roundTrip()
	{
		SourceKcStore store = new SourceKcStore();
		Map<String, Integer> kc = new HashMap<>();
		kc.put("Zulrah", 500);
		kc.put("Vorkath", 300);

		store.update(kc);

		assertEquals(500, store.getKc("Zulrah"));
		assertEquals(300, store.getKc("Vorkath"));
		assertEquals(0, store.getKc("Abyssal Sire")); // not synced
		assertEquals(2, store.size());
	}

	@Test
	public void sourceKcStore_clear_removesAllEntries()
	{
		SourceKcStore store = new SourceKcStore();
		Map<String, Integer> kc = new HashMap<>();
		kc.put("Zulrah", 500);
		store.update(kc);
		assertEquals(1, store.size());

		store.clear();

		assertEquals(0, store.size());
		assertEquals(0, store.getKc("Zulrah"));
	}

	@Test
	public void sourceKcStore_snapshot_isImmutable()
	{
		SourceKcStore store = new SourceKcStore();
		Map<String, Integer> kc = new HashMap<>();
		kc.put("Vorkath", 100);
		store.update(kc);

		Map<String, Integer> snapshot = store.snapshot();

		try
		{
			snapshot.put("NewEntry", 1);
			fail("Expected UnsupportedOperationException from immutable snapshot");
		}
		catch (UnsupportedOperationException expected)
		{
			// expected — snapshot must be immutable
		}
	}

	@Test
	public void sourceKcStore_updateTwice_latestValueWins()
	{
		SourceKcStore store = new SourceKcStore();
		Map<String, Integer> first = new HashMap<>();
		first.put("Zulrah", 100);
		store.update(first);

		Map<String, Integer> second = new HashMap<>();
		second.put("Zulrah", 200);
		store.update(second);

		assertEquals(200, store.getKc("Zulrah"));
	}

	// ========================================================================
	// SyncResult — value object
	// ========================================================================

	@Test
	public void syncResult_success_hasCorrectFields()
	{
		Map<String, Integer> kc = new HashMap<>();
		kc.put("Zulrah", 10);
		SyncResult result = SyncResult.success(kc, 3);

		assertTrue(result.isSuccess());
		assertNull(result.getErrorMessage());
		assertEquals(1, result.getKcBySource().size());
		assertEquals(3, result.getSkippedCount());
	}

	@Test
	public void syncResult_failure_hasCorrectFields()
	{
		SyncResult result = SyncResult.failure("Connection timed out");

		assertFalse(result.isSuccess());
		assertEquals("Connection timed out", result.getErrorMessage());
		assertTrue(result.getKcBySource().isEmpty());
		assertEquals(0, result.getSkippedCount());
	}

	// ========================================================================
	// Helper
	// ========================================================================

	private void mockHttpResponse(int code, String body) throws IOException
	{
		Call mockCall = mock(Call.class);
		when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);

		Request dummyRequest = new Request.Builder()
			.url(TempleOsrsKcSyncer.BASE_URL + "?player=test")
			.build();

		Response response = new Response.Builder()
			.request(dummyRequest)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "Error")
			.body(ResponseBody.create(MediaType.parse("application/json"), body))
			.build();

		when(mockCall.execute()).thenReturn(response);
	}
}
