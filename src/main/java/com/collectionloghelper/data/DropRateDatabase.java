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
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DropRateDatabase
{
	private List<CollectionLogSource> sources = Collections.emptyList();
	private Map<Integer, CollectionLogItem> itemsById = Collections.emptyMap();
	private Map<String, CollectionLogItem> itemsByName = Collections.emptyMap();
	private Map<Integer, CollectionLogSource> sourcesByNpcId = Collections.emptyMap();
	private Map<CollectionLogCategory, List<CollectionLogSource>> sourcesByCategory = Collections.emptyMap();
	private Map<String, CollectionLogSource> sourcesByNameLower = Collections.emptyMap();

	@Inject
	private Gson gson;

	public void load()
	{
		try (InputStream is = getClass().getResourceAsStream("/com/collectionloghelper/drop_rates.json"))
		{
			if (is == null)
			{
				log.warn("drop_rates.json not found in resources");
				return;
			}

			Type listType = new TypeToken<List<CollectionLogSource>>(){}.getType();
			sources = gson.fromJson(new InputStreamReader(is), listType);

			itemsById = sources.stream()
				.flatMap(s -> s.getItems().stream())
				.collect(Collectors.toMap(
					CollectionLogItem::getItemId,
					item -> item,
					(a, b) -> a
				));

			Map<String, CollectionLogItem> nameMap = new HashMap<>();
			for (CollectionLogItem item : itemsById.values())
			{
				nameMap.putIfAbsent(item.getName().toLowerCase(), item);
			}
			itemsByName = nameMap;

			Map<Integer, CollectionLogSource> npcMap = new HashMap<>();
			Map<CollectionLogCategory, List<CollectionLogSource>> catMap = new EnumMap<>(CollectionLogCategory.class);
			Map<String, CollectionLogSource> nameMap2 = new HashMap<>();
			for (CollectionLogSource source : sources)
			{
				if (source.getNpcId() > 0)
				{
					npcMap.put(source.getNpcId(), source);
				}
				catMap.computeIfAbsent(source.getCategory(), k -> new ArrayList<>()).add(source);
				nameMap2.put(source.getName().toLowerCase(), source);
			}
			sourcesByNpcId = npcMap;
			sourcesByCategory = catMap;
			sourcesByNameLower = nameMap2;

			validateData();
			log.debug("Loaded {} sources with {} items", sources.size(), itemsById.size());
		}
		catch (Exception e)
		{
			log.error("Failed to load drop rate database", e);
		}
	}

	public List<CollectionLogSource> getAllSources()
	{
		return Collections.unmodifiableList(sources);
	}

	public List<CollectionLogSource> getSourcesByCategory(CollectionLogCategory category)
	{
		return sourcesByCategory.getOrDefault(category, Collections.emptyList());
	}

	public CollectionLogSource getSourceByName(String name)
	{
		return sourcesByNameLower.get(name.toLowerCase());
	}

	public CollectionLogItem getItemById(int itemId)
	{
		return itemsById.get(itemId);
	}

	public CollectionLogItem getItemByName(String name)
	{
		return itemsByName.get(name.toLowerCase());
	}

	public CollectionLogSource getSourceByNpcId(int npcId)
	{
		return sourcesByNpcId.get(npcId);
	}

	private void validateData()
	{
		for (CollectionLogSource source : sources)
		{
			for (CollectionLogItem item : source.getItems())
			{
				if (item.getDropRate() < 0)
				{
					log.warn("Invalid negative dropRate {} for item '{}' in source '{}'",
						item.getDropRate(), item.getName(), source.getName());
				}
				else if (item.getDropRate() == 0)
				{
					log.warn("Zero dropRate for item '{}' in source '{}' — will use fallback scoring",
						item.getName(), source.getName());
				}
				else if (item.getDropRate() > 1.0)
				{
					log.warn("dropRate {} > 1.0 for non-guaranteed item '{}' in source '{}'",
						item.getDropRate(), item.getName(), source.getName());
				}
			}
		}
	}
}
