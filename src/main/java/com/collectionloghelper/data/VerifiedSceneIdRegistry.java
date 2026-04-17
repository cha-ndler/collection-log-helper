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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads and exposes the {@code verified_scene_ids.json} registry.
 *
 * <p>The registry records the authoritative examine-ID &lt;-&gt; scene-ID mapping for objects
 * where the OSRS cache examine tooltip ID differs from the scene tile ID exposed by RuneLite's
 * tile API. Contributors and lint tools can consult this registry at PR time to detect suspect
 * {@code objectId} references before they reach production.
 *
 * <p>This class is read-only and purely additive — no existing code paths depend on it yet.
 * It is intended as Phase 0 data infrastructure for Tier A.5.
 */
@Slf4j
public class VerifiedSceneIdRegistry
{
	private static final String RESOURCE_PATH = "/com/collectionloghelper/verified_scene_ids.json";
	private static final int EXPECTED_SCHEMA_VERSION = 1;

	private final List<VerifiedSceneId> entries;
	private final Map<Integer, VerifiedSceneId> byExamineId;
	private final Map<Integer, VerifiedSceneId> bySceneId;

	VerifiedSceneIdRegistry(List<VerifiedSceneId> entries)
	{
		this.entries = Collections.unmodifiableList(entries);

		Map<Integer, VerifiedSceneId> examineMap = new HashMap<>();
		Map<Integer, VerifiedSceneId> sceneMap = new HashMap<>();

		for (VerifiedSceneId entry : entries)
		{
			examineMap.putIfAbsent(entry.getExamineId(), entry);
			for (int sceneId : entry.getSceneIds())
			{
				sceneMap.putIfAbsent(sceneId, entry);
			}
		}

		this.byExamineId = Collections.unmodifiableMap(examineMap);
		this.bySceneId = Collections.unmodifiableMap(sceneMap);
	}

	/**
	 * Loads the registry from the bundled resource file. The result is not cached — callers that
	 * need repeated access should hold the returned instance.
	 *
	 * @return a populated registry, or an empty registry if the resource cannot be parsed
	 */
	public static VerifiedSceneIdRegistry load()
	{
		return load(new Gson());
	}

	/**
	 * Loads the registry using the provided {@link Gson} instance. Exposed package-private for
	 * testing.
	 */
	static VerifiedSceneIdRegistry load(Gson gson)
	{
		try (InputStream is = VerifiedSceneIdRegistry.class.getResourceAsStream(RESOURCE_PATH))
		{
			if (is == null)
			{
				log.warn("verified_scene_ids.json not found at {}", RESOURCE_PATH);
				return empty();
			}

			InputStreamReader reader = new InputStreamReader(is);
			JsonElement parsed = new JsonParser().parse(reader);
			return parseRoot(parsed.getAsJsonObject(), gson);
		}
		catch (Exception e)
		{
			log.error("Failed to load verified_scene_ids.json", e);
			return empty();
		}
	}

	/**
	 * Parses a registry from a raw JSON string. Package-private for testing; production callers
	 * should use {@link #load()}.
	 */
	static VerifiedSceneIdRegistry loadFromJson(String json, Gson gson)
	{
		try
		{
			JsonElement parsed = new JsonParser().parse(json);
			return parseRoot(parsed.getAsJsonObject(), gson);
		}
		catch (Exception e)
		{
			log.error("Failed to parse verified_scene_ids JSON", e);
			return empty();
		}
	}

	private static VerifiedSceneIdRegistry parseRoot(JsonObject root, Gson gson)
	{
		JsonElement versionElement = root.get("version");
		if (versionElement == null || versionElement.isJsonNull())
		{
			log.warn("verified_scene_ids.json missing 'version' field; assuming schema version {}",
				EXPECTED_SCHEMA_VERSION);
		}
		else
		{
			int version = versionElement.getAsInt();
			if (version != EXPECTED_SCHEMA_VERSION)
			{
				log.warn("verified_scene_ids.json schema version {} is unsupported (expected {}); loading anyway",
					version, EXPECTED_SCHEMA_VERSION);
			}
		}

		Type listType = new TypeToken<List<VerifiedSceneId>>()
		{
		}.getType();
		List<VerifiedSceneId> entries = gson.fromJson(root.get("entries"), listType);

		if (entries == null || entries.isEmpty())
		{
			log.warn("verified_scene_ids.json loaded with no entries");
			return empty();
		}

		log.debug("Loaded {} verified scene-ID entries", entries.size());
		return new VerifiedSceneIdRegistry(entries);
	}

	/** Returns a registry with no entries. */
	public static VerifiedSceneIdRegistry empty()
	{
		return new VerifiedSceneIdRegistry(Collections.emptyList());
	}

	/**
	 * Looks up a registry entry by its cache examine ID.
	 *
	 * @param examineId the object ID shown by the OSRS examine tooltip or cache viewer
	 * @return the matching entry, or {@link Optional#empty()} if not recorded
	 */
	public Optional<VerifiedSceneId> lookupByExamineId(int examineId)
	{
		return Optional.ofNullable(byExamineId.get(examineId));
	}

	/**
	 * Looks up a registry entry by one of its scene tile IDs.
	 *
	 * @param sceneId the object ID returned by RuneLite's tile API
	 * @return the matching entry, or {@link Optional#empty()} if not recorded
	 */
	public Optional<VerifiedSceneId> lookupBySceneId(int sceneId)
	{
		return Optional.ofNullable(bySceneId.get(sceneId));
	}

	/**
	 * Returns all entries in the registry in load order.
	 *
	 * @return an unmodifiable list of all entries
	 */
	public List<VerifiedSceneId> all()
	{
		return entries;
	}
}
