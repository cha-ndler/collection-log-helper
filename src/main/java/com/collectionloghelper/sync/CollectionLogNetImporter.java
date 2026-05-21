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

import com.collectionloghelper.data.PlayerCollectionState;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches a player's obtained-item list from collectionlog.net and marks
 * each item as obtained via {@link PlayerCollectionState#markItemObtained}.
 *
 * <p>All HTTP work is dispatched on a dedicated single-thread executor so the
 * game thread and EDT are never blocked.  Every failure mode is caught and
 * reflected in the returned {@link ImportResult} — this class never throws
 * to its caller.
 *
 * <p>API endpoint:
 * {@code GET https://api.collectionlog.net/collectionlog/user/{username}}
 */
@Slf4j
@Singleton
public class CollectionLogNetImporter
{
	static final String BASE_URL = "https://api.collectionlog.net/collectionlog/user/";

	/** Hard cap on response body size to prevent OOM from a misbehaving/malicious server. */
	private static final long MAX_RESPONSE_BYTES = 10L * 1024L * 1024L;

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final PlayerCollectionState collectionState;
	private final ScheduledExecutorService executor;

	/**
	 * Constructs the importer with all dependencies supplied by Guice.
	 *
	 * <p>The {@link ScheduledExecutorService} is provided by the RuneLite runtime
	 * (see {@code RuneLite.scheduledExecutorService}) — a shared, singleton pool
	 * whose lifecycle is owned by the client rather than by this plugin.  Plugin
	 * Hub convention favours this pattern over per-class
	 * {@code Executors.newSingleThreadExecutor()} construction because it avoids
	 * leaked threads on plugin restart and lets the runtime cap total worker
	 * count across all plugins.
	 *
	 * @param httpClient HTTP client used for the collectionlog.net request
	 * @param gson JSON parser
	 * @param collectionState target store for obtained-item marks
	 * @param executor shared scheduled executor provided by the runtime
	 */
	@Inject
	CollectionLogNetImporter(OkHttpClient httpClient, Gson gson, PlayerCollectionState collectionState,
		ScheduledExecutorService executor)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.collectionState = collectionState;
		this.executor = executor;
	}

	/**
	 * Submits an import task for the given username.  Returns immediately with a
	 * {@link Future} that resolves to an {@link ImportResult} describing the
	 * outcome.  Never throws — all failure modes produce a failure {@link ImportResult}.
	 *
	 * @param username the collectionlog.net RSN (URL-encoded by this method)
	 * @return a {@link Future} resolving to the import result
	 */
	public Future<ImportResult> importProfile(String username)
	{
		return executor.submit(() -> doImport(username));
	}

	private ImportResult doImport(String username)
	{
		String url = BASE_URL + encodeUsername(username);
		log.debug("Fetching collectionlog.net profile for '{}' from {}", username, url);

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", "collection-log-helper-plugin")
			.build();

		Call call = httpClient.newCall(request);
		try (Response response = call.execute())
		{
			int code = response.code();

			if (code == 404)
			{
				log.warn("collectionlog.net: user not found — {}", username);
				return ImportResult.userNotFound(username);
			}

			if (!response.isSuccessful())
			{
				log.warn("collectionlog.net: HTTP {} for user '{}'", code, username);
				return ImportResult.serviceUnavailable(code);
			}

			ResponseBody body = response.body();
			if (body == null)
			{
				log.warn("collectionlog.net: empty response body for user '{}'", username);
				return ImportResult.serviceUnavailable(code);
			}

			long contentLength = body.contentLength();
			if (contentLength > MAX_RESPONSE_BYTES)
			{
				log.warn("collectionlog.net: response too large ({} bytes) for user '{}'",
					contentLength, username);
				return ImportResult.serviceUnavailable(code);
			}

			String json = body.string();
			if (json.length() > MAX_RESPONSE_BYTES)
			{
				log.warn("collectionlog.net: response body exceeded {} bytes for user '{}'",
					MAX_RESPONSE_BYTES, username);
				return ImportResult.serviceUnavailable(code);
			}
			return parseAndMark(json, username);
		}
		catch (IOException e)
		{
			log.warn("collectionlog.net: network error importing profile for '{}'", username, e);
			return ImportResult.serviceUnavailable(0);
		}
	}

	/**
	 * Parses the JSON response and marks all obtained items.  Returns a failure
	 * result (not an exception) when the JSON cannot be parsed or is structurally
	 * invalid.
	 *
	 * <p>Expected shape (simplified):
	 * <pre>{@code
	 * {
	 *   "collectionLog": {
	 *     "tabs": {
	 *       "TabName": {
	 *         "SourceName": {
	 *           "items": [
	 *             { "id": 12345, "obtained": true, ... },
	 *             ...
	 *           ]
	 *         }
	 *       }
	 *     }
	 *   }
	 * }
	 * }</pre>
	 */
	ImportResult parseAndMark(String json, String username)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			if (root == null)
			{
				log.warn("collectionlog.net: null root for user '{}'", username);
				return ImportResult.malformedResponse();
			}

			JsonObject collectionLog = getObject(root, "collectionLog");
			if (collectionLog == null)
			{
				log.warn("collectionlog.net: missing 'collectionLog' key for user '{}'", username);
				return ImportResult.malformedResponse();
			}

			JsonObject tabs = getObject(collectionLog, "tabs");
			if (tabs == null)
			{
				log.warn("collectionlog.net: missing 'tabs' key for user '{}'", username);
				return ImportResult.malformedResponse();
			}

			int marked = 0;
			int skipped = 0;

			for (String tabName : tabs.keySet())
			{
				JsonElement tabEl = tabs.get(tabName);
				if (!tabEl.isJsonObject())
				{
					continue;
				}
				JsonObject tab = tabEl.getAsJsonObject();

				for (String sourceName : tab.keySet())
				{
					JsonElement sourceEl = tab.get(sourceName);
					if (!sourceEl.isJsonObject())
					{
						continue;
					}
					JsonObject source = sourceEl.getAsJsonObject();
					JsonElement itemsEl = source.get("items");
					if (itemsEl == null || !itemsEl.isJsonArray())
					{
						continue;
					}

					JsonArray items = itemsEl.getAsJsonArray();
					for (JsonElement itemEl : items)
					{
						if (!itemEl.isJsonObject())
						{
							skipped++;
							continue;
						}
						JsonObject item = itemEl.getAsJsonObject();
						JsonElement obtainedEl = item.get("obtained");
						if (obtainedEl == null || !obtainedEl.getAsBoolean())
						{
							continue;
						}
						JsonElement idEl = item.get("id");
						if (idEl == null || !idEl.isJsonPrimitive())
						{
							skipped++;
							continue;
						}
						int itemId = idEl.getAsInt();
						if (collectionState.markItemObtained(itemId))
						{
							marked++;
						}
					}
				}
			}

			log.debug("collectionlog.net import complete for '{}': {} items marked, {} skipped",
				username, marked, skipped);
			return ImportResult.success(marked);
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			log.warn("collectionlog.net: malformed JSON for user '{}'", username, e);
			return ImportResult.malformedResponse();
		}
	}

	/** URL-encode the username for safe use in a path segment. */
	private static String encodeUsername(String username)
	{
		try
		{
			return java.net.URLEncoder.encode(username, "UTF-8").replace("+", "%20");
		}
		catch (java.io.UnsupportedEncodingException e)
		{
			// UTF-8 is always available
			return username;
		}
	}

	private static JsonObject getObject(JsonObject parent, String key)
	{
		JsonElement el = parent.get(key);
		if (el == null || !el.isJsonObject())
		{
			return null;
		}
		return el.getAsJsonObject();
	}

}
