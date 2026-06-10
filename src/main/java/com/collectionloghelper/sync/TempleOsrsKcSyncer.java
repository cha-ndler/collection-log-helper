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
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches boss/activity kill counts from the TempleOSRS API and maps them to
 * CLH source names.
 *
 * <p>API endpoint: {@code GET https://templeosrs.com/api/player_info.php?player={username}}
 *
 * <p>Response shape (simplified):
 * <pre>
 * {
 *   "data": {
 *     "Abyssal Sire": 0,
 *     "K'ril Tsutsaroth": 100,
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>Fail-soft: any HTTP error, parse failure, or timeout yields
 * {@link SyncResult#failure(String)}; no exception propagates to the caller.
 * Unknown TempleOSRS activity names are logged at DEBUG and skipped; they do not
 * cause the overall sync to fail.
 */
@Slf4j
@Singleton
public class TempleOsrsKcSyncer
{
	static final String BASE_URL = "https://templeosrs.com/api/player_info.php";

	/** Hard cap on response body size to prevent OOM from a misbehaving/malicious server. */
	private static final long MAX_RESPONSE_BYTES = 1L * 1024L * 1024L;

	/**
	 * Static name mapping from TempleOSRS display name to CLH source name.
	 *
	 * <p>Keys are TempleOSRS activity names exactly as returned by the API.
	 * Values are CLH {@link com.collectionloghelper.data.CollectionLogSource#getName()} strings.
	 *
	 * <p>Entries are added only when the names differ; identical names fall through
	 * to the identity-return path in {@link #resolveClhName}.
	 */
	static final Map<String, String> TEMPLE_TO_CLH;

	static
	{
		Map<String, String> m = new HashMap<>();
		// Explicit overrides where TempleOSRS name differs from CLH source name
		m.put("Barrows Chests", "Barrows");
		m.put("Leviathan", "The Leviathan");
		m.put("Mimic", "The Mimic");
		m.put("Nightmare", "The Nightmare");
		m.put("Whisperer", "The Whisperer");
		TEMPLE_TO_CLH = Collections.unmodifiableMap(m);
	}

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final CollectionLogHelperConfig config;

	/**
	 * Constructs the syncer with all dependencies supplied by Guice.
	 *
	 * <p>The {@link ScheduledExecutorService} is provided by the RuneLite runtime
	 * (see {@code RuneLite.scheduledExecutorService}) — a shared, singleton pool
	 * whose lifecycle is owned by the client rather than by this plugin.  Plugin
	 * Hub convention favours this pattern over per-class
	 * {@code Executors.newSingleThreadExecutor()} construction because it avoids
	 * leaked threads on plugin restart and lets the runtime cap total worker
	 * count across all plugins.
	 *
	 * @param okHttpClient HTTP client used for the TempleOSRS request
	 * @param gson JSON parser
	 * @param executor shared scheduled executor provided by the runtime
	 * @param config plugin config — re-checked immediately before the request so
	 *               the RSN is never submitted while the sync is disabled
	 */
	@Inject
	TempleOsrsKcSyncer(OkHttpClient okHttpClient, Gson gson, ScheduledExecutorService executor,
		CollectionLogHelperConfig config)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
		this.executor = executor;
		this.config = config;
	}

	/**
	 * Asynchronously fetches KC data from TempleOSRS for {@code username} and
	 * maps it to CLH source names.
	 *
	 * <p>Never throws; failures are encoded as {@link SyncResult#failure(String)}.
	 *
	 * @param username the OSRS display name (spaces allowed; URL-encoded internally)
	 * @return a Future that resolves to a {@link SyncResult}
	 */
	public Future<SyncResult> syncKc(String username)
	{
		return executor.submit(() -> doSync(username));
	}

	// ---- package-private for testing ----

	/**
	 * Performs the actual HTTP fetch and parse. Exposed package-private so tests
	 * can inject a mocked {@link OkHttpClient} and call directly.
	 */
	SyncResult doSync(String username)
	{
		// Re-check the consent flag immediately before issuing the request: this
		// runs asynchronously after the orchestrator's check, so the player could
		// have disabled the sync in between. The RSN must never leave the client
		// while the sync is off.
		if (!config.enableTempleOsrsSync())
		{
			log.debug("TempleOSRS sync disabled in config; skipping request for '{}'", username);
			return SyncResult.failure("TempleOSRS sync is disabled in settings");
		}

		if (username == null || username.trim().isEmpty())
		{
			return SyncResult.failure("Username is empty");
		}

		String url = BASE_URL + "?player=" + encodeUsername(username.trim());
		log.debug("Fetching TempleOSRS KC for '{}': {}", username, url);

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", "collection-log-helper-runelite-plugin")
			.build();

		try (Response response = okHttpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				String msg = "TempleOSRS returned HTTP " + response.code()
					+ " for user '" + username + "'";
				log.warn(msg);
				return SyncResult.failure(msg);
			}

			if (response.body() == null)
			{
				return SyncResult.failure("Empty response from TempleOSRS");
			}

			long contentLength = response.body().contentLength();
			if (contentLength > MAX_RESPONSE_BYTES)
			{
				log.warn("TempleOSRS response too large ({} bytes) for user '{}'",
					contentLength, username);
				return SyncResult.failure("TempleOSRS response exceeded size cap");
			}

			String body = response.body().string();
			if (body.isEmpty())
			{
				return SyncResult.failure("Empty response from TempleOSRS");
			}
			if (body.length() > MAX_RESPONSE_BYTES)
			{
				log.warn("TempleOSRS response body exceeded {} bytes for user '{}'",
					MAX_RESPONSE_BYTES, username);
				return SyncResult.failure("TempleOSRS response exceeded size cap");
			}

			return parseResponse(body);
		}
		catch (IOException e)
		{
			log.warn("Network error fetching TempleOSRS KC for '{}': {}",
				username, e.getMessage());
			return SyncResult.failure("Network error: " + e.getMessage());
		}
		catch (Exception e)
		{
			log.warn("Unexpected error fetching TempleOSRS KC for '{}': {}",
				username, e.getMessage());
			return SyncResult.failure("Unexpected error: " + e.getMessage());
		}
	}

	/**
	 * Parses the TempleOSRS JSON response into a {@link SyncResult}.
	 *
	 * <p>Expected shape: {@code {"data": {"Boss Name": 100, ...}}}
	 */
	SyncResult parseResponse(String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			if (root == null || !root.has("data"))
			{
				return SyncResult.failure("TempleOSRS response missing 'data' field");
			}

			JsonElement dataEl = root.get("data");
			if (!dataEl.isJsonObject())
			{
				return SyncResult.failure("TempleOSRS 'data' field is not an object");
			}

			return mapActivitiesToSources(dataEl.getAsJsonObject());
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Failed to parse TempleOSRS response: {}", e.getMessage());
			return SyncResult.failure("JSON parse error: " + e.getMessage());
		}
	}

	/**
	 * Maps TempleOSRS activity names to CLH source names, skipping unknowns.
	 *
	 * <p>Mapping strategy:
	 * <ol>
	 *   <li>Explicit override in {@link #TEMPLE_TO_CLH}</li>
	 *   <li>Identity — return the name as-is (caller validates against the DB)</li>
	 *   <li>Zero-KC entries are skipped (player has not done that activity)</li>
	 * </ol>
	 */
	SyncResult mapActivitiesToSources(JsonObject data)
	{
		Map<String, Integer> result = new HashMap<>();
		int skipped = 0;

		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			String templeName = entry.getKey();
			JsonElement kcEl = entry.getValue();

			if (!kcEl.isJsonPrimitive())
			{
				skipped++;
				continue;
			}

			int kc;
			try
			{
				kc = kcEl.getAsInt();
			}
			catch (NumberFormatException e)
			{
				skipped++;
				continue;
			}

			if (kc <= 0)
			{
				// Zero KC — player has not completed this activity; skip silently
				continue;
			}

			String clhName = resolveClhName(templeName);
			// Last-write wins if the same CLH name appears under multiple TempleOSRS keys
			result.merge(clhName, kc, Integer::max);
		}

		log.debug("TempleOSRS KC mapped {} sources, skipped {} non-numeric entries",
			result.size(), skipped);
		return SyncResult.success(result, skipped);
	}

	/**
	 * Resolves a TempleOSRS activity name to a CLH-compatible source name.
	 *
	 * <p>First checks the explicit override map {@link #TEMPLE_TO_CLH}; if absent,
	 * returns the name unchanged (identity fallback).  Callers should validate the
	 * name against {@link com.collectionloghelper.data.DropRateDatabase#getSourceByName}
	 * to confirm it matches a known CLH source.
	 *
	 * @return always non-null
	 */
	String resolveClhName(String templeName)
	{
		String mapped = TEMPLE_TO_CLH.get(templeName);
		return mapped != null ? mapped : templeName;
	}

	/** URL-encode a username for use as a query-parameter value. */
	private static String encodeUsername(String username)
	{
		try
		{
			return java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20");
		}
		catch (java.io.UnsupportedEncodingException e)
		{
			// UTF-8 is always supported in Java
			return username;
		}
	}
}
