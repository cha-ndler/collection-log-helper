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
import com.collectionloghelper.data.PlayerCollectionState;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * Unit tests for {@link CollectionLogNetImporter}.
 *
 * HTTP calls are intercepted via a mocked {@link OkHttpClient} — the real
 * collectionlog.net API is never contacted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CollectionLogNetImporterTest
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

	/** Two-item response: both obtained. */
	private static final String HAPPY_PATH_JSON = "{"
		+ "\"collectionLog\":{"
		+ "  \"tabs\":{"
		+ "    \"Bosses\":{"
		+ "      \"Zulrah\":{"
		+ "        \"items\":["
		+ "          {\"id\":12932,\"name\":\"Tanzanite fang\",\"obtained\":true,\"quantity\":1},"
		+ "          {\"id\":12934,\"name\":\"Magic fang\",\"obtained\":true,\"quantity\":2}"
		+ "        ]"
		+ "      }"
		+ "    }"
		+ "  }"
		+ "}}";

	/** One obtained, one not. */
	private static final String PARTIAL_OBTAINED_JSON = "{"
		+ "\"collectionLog\":{"
		+ "  \"tabs\":{"
		+ "    \"Bosses\":{"
		+ "      \"Vorkath\":{"
		+ "        \"items\":["
		+ "          {\"id\":21892,\"name\":\"Dragonbone necklace\",\"obtained\":true,\"quantity\":1},"
		+ "          {\"id\":21896,\"name\":\"Vorkath's head\",\"obtained\":false,\"quantity\":0}"
		+ "        ]"
		+ "      }"
		+ "    }"
		+ "  }"
		+ "}}";

	private static final String NOT_FOUND_BODY = "{\"error\":\"Player not found\"}";

	@Mock
	private OkHttpClient mockHttpClient;

	@Mock
	private okhttp3.Call mockCall;

	@Mock
	private PlayerCollectionState mockCollectionState;

	@Mock
	private CollectionLogHelperConfig mockConfig;

	private final Gson gson = new Gson();
	private ScheduledExecutorService sameThread;

	private CollectionLogNetImporter importer;

	@BeforeEach
	public void setUp()
	{
		// Single-thread scheduled executor — deterministic enough for these tests
		// and a reasonable stand-in for the runtime-supplied scheduler.
		sameThread = Executors.newSingleThreadScheduledExecutor();
		when(mockConfig.enableCollectionLogNetImport()).thenReturn(true);
		importer = new CollectionLogNetImporter(mockHttpClient, gson, mockCollectionState, sameThread, mockConfig);
		when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
	}

	@AfterEach
	public void tearDown()
	{
		sameThread.shutdownNow();
	}

	// ── Happy path ────────────────────────────────────────────────────────────

	@Test
	public void happyPath_marksObtainedItems() throws Exception
	{
		stubResponse(200, HAPPY_PATH_JSON);
		when(mockCollectionState.markItemObtained(anyInt())).thenReturn(true);

		ImportResult result = get(importer.importProfile("Zezima"));

		assertTrue(result.isSuccess());
		assertEquals(ImportResult.Status.SUCCESS, result.getStatus());
		assertEquals(2, result.getItemsMarked());

		ArgumentCaptor<Integer> idCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(mockCollectionState, atLeastOnce()).markItemObtained(idCaptor.capture());
		assertTrue(idCaptor.getAllValues().contains(12932));
		assertTrue(idCaptor.getAllValues().contains(12934));
	}

	@Test
	public void happyPath_skipsNonObtainedItems() throws Exception
	{
		stubResponse(200, PARTIAL_OBTAINED_JSON);
		when(mockCollectionState.markItemObtained(21892)).thenReturn(true);

		ImportResult result = get(importer.importProfile("Zezima"));

		assertTrue(result.isSuccess());
		assertEquals(1, result.getItemsMarked());
		verify(mockCollectionState).markItemObtained(21892);
		verify(mockCollectionState, never()).markItemObtained(21896);
	}

	@Test
	public void happyPath_toastMessageContainsCount() throws Exception
	{
		stubResponse(200, HAPPY_PATH_JSON);
		when(mockCollectionState.markItemObtained(anyInt())).thenReturn(true);

		ImportResult result = get(importer.importProfile("Zezima"));

		assertTrue(result.toToastMessage().contains("2"));
		assertTrue(result.toToastMessage().contains("collectionlog.net"));
	}

	// ── 404 — user not found ─────────────────────────────────────────────────

	@Test
	public void notFound_returns404Result() throws Exception
	{
		stubResponse(404, NOT_FOUND_BODY);

		ImportResult result = get(importer.importProfile("NoSuchPlayer"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.USER_NOT_FOUND, result.getStatus());
		assertEquals(0, result.getItemsMarked());
	}

	@Test
	public void notFound_toastMessageMentionsUserNotFound() throws Exception
	{
		stubResponse(404, NOT_FOUND_BODY);

		ImportResult result = get(importer.importProfile("NoSuchPlayer"));

		assertTrue(result.toToastMessage().toLowerCase().contains("not found"));
	}

	@Test
	public void notFound_doesNotCallMarkItemObtained() throws Exception
	{
		stubResponse(404, NOT_FOUND_BODY);

		get(importer.importProfile("NoSuchPlayer"));

		verify(mockCollectionState, never()).markItemObtained(anyInt());
	}

	// ── 500 — service error ──────────────────────────────────────────────────

	@Test
	public void serverError_returnsServiceUnavailable() throws Exception
	{
		stubResponse(500, "{\"error\":\"Internal Server Error\"}");

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.SERVICE_UNAVAILABLE, result.getStatus());
	}

	@Test
	public void serverError_toastMessageMentionsServiceUnavailable() throws Exception
	{
		stubResponse(500, "{\"error\":\"Internal Server Error\"}");

		ImportResult result = get(importer.importProfile("Zezima"));

		assertTrue(result.toToastMessage().toLowerCase().contains("unavailable"));
	}

	@Test
	public void serverError_doesNotCallMarkItemObtained() throws Exception
	{
		stubResponse(500, "{\"error\":\"Internal Server Error\"}");

		get(importer.importProfile("Zezima"));

		verify(mockCollectionState, never()).markItemObtained(anyInt());
	}

	// ── Network failure ──────────────────────────────────────────────────────

	@Test
	public void networkError_returnsServiceUnavailable() throws Exception
	{
		when(mockCall.execute()).thenThrow(new IOException("Connection refused"));

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.SERVICE_UNAVAILABLE, result.getStatus());
	}

	@Test
	public void networkError_doesNotThrowToCaller() throws Exception
	{
		when(mockCall.execute()).thenThrow(new IOException("Timeout"));

		// Must not throw ExecutionException wrapping a non-checked exception
		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
	}

	// ── Malformed JSON ───────────────────────────────────────────────────────

	@Test
	public void malformedJson_returnsMalformedResponse() throws Exception
	{
		stubResponse(200, "this is not json at all");

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.MALFORMED_RESPONSE, result.getStatus());
	}

	@Test
	public void malformedJson_missingTabsKey_returnsMalformedResponse() throws Exception
	{
		stubResponse(200, "{\"collectionLog\":{}}");

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.MALFORMED_RESPONSE, result.getStatus());
	}

	@Test
	public void malformedJson_missingCollectionLogKey_returnsMalformedResponse() throws Exception
	{
		stubResponse(200, "{\"other\":\"data\"}");

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.MALFORMED_RESPONSE, result.getStatus());
	}

	@Test
	public void malformedJson_doesNotCallMarkItemObtained() throws Exception
	{
		stubResponse(200, "{ broken json");

		get(importer.importProfile("Zezima"));

		verify(mockCollectionState, never()).markItemObtained(anyInt());
	}

	// ── Disabled by config — no request must leave the client ─────────────────

	@Test
	public void importDisabled_doesNotExecuteRequest() throws Exception
	{
		when(mockConfig.enableCollectionLogNetImport()).thenReturn(false);

		ImportResult result = get(importer.importProfile("Zezima"));

		assertFalse(result.isSuccess());
		assertEquals(ImportResult.Status.DISABLED, result.getStatus());
		// The privacy-critical assertion: the RSN never reached the network.
		verify(mockHttpClient, never()).newCall(any(Request.class));
		verify(mockCollectionState, never()).markItemObtained(anyInt());
	}

	// ── parseAndMark unit tests ───────────────────────────────────────────────

	@Test
	public void parseAndMark_emptyTabs_returnsSuccessWithZeroItems()
	{
		String json = "{\"collectionLog\":{\"tabs\":{}}}";

		ImportResult result = importer.parseAndMark(json, "Zezima");

		assertTrue(result.isSuccess());
		assertEquals(0, result.getItemsMarked());
	}

	@Test
	public void parseAndMark_alreadyObtainedItem_countedOnlyIfNewlyAdded()
	{
		String json = "{\"collectionLog\":{\"tabs\":{\"Bosses\":{\"Zulrah\":{\"items\":"
			+ "[{\"id\":12932,\"obtained\":true}]}}}}}";
		// markItemObtained returns false when item was already present
		when(mockCollectionState.markItemObtained(12932)).thenReturn(false);

		ImportResult result = importer.parseAndMark(json, "Zezima");

		assertTrue(result.isSuccess());
		assertEquals(0, result.getItemsMarked());
	}

	// ── Helpers ──────────────────────────────────────────────────────────────

	private void stubResponse(int code, String body) throws IOException
	{
		Request dummyRequest = new Request.Builder()
			.url(CollectionLogNetImporter.BASE_URL + "Zezima")
			.build();
		Response response = new Response.Builder()
			.request(dummyRequest)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "Error")
			.body(ResponseBody.create(JSON, body))
			.build();
		when(mockCall.execute()).thenReturn(response);
	}

	private static ImportResult get(Future<ImportResult> future) throws ExecutionException, InterruptedException
	{
		return future.get();
	}
}
