# Data sourcing — try MCP first, in-game last

Every piece of data in this plugin (item IDs, NPC IDs, object IDs, coordinates, drop rates, varbit IDs, dialog text) can be obtained through at least one of three tiers. Always start at Tier 1 and only fall through when a tool genuinely cannot answer the question. In-game capture requires a live RuneLite client, an active game session, and manual recording — it is slow, session-specific, and inherently hard to reproduce. MCP tools and committed registries cover the same ground in milliseconds and can be called from any CI context without a client.

```
Need a data value?
  │
  ├─ Is it a static ID, rate, coordinate, or description?
  │    └─ Tier 1: MCP tool (below) — check tool list first, done in seconds.
  │
  ├─ Is it a mapping you've already established or a known-gotcha ID?
  │    └─ Tier 2: committed registry — search drop_rates.json or verified_scene_ids.json.
  │
  └─ Is it truly dynamic runtime state — instanced offsets, mid-fight
       phase transitions, dynamic object spawns that only exist in a
       live game session?
         └─ Tier 3: in-game authoring capture (last resort, <5% of work).
```

## Tier 1 — MCP tools (try first)

The `runelite-dev-toolkit` MCP server exposes the following tools. Use them before opening a browser, launching a client, or writing any data by hand.

**Lookups**

| Tool | Use when… |
|------|-----------|
| `wiki_lookup` | You need a full drop table, infobox stats, or any page-level data from the OSRS Wiki for a boss, creature, or activity. |
| `npc_lookup` | You need to verify or discover an NPC ID, combat level, slayer category, or actions by name or numeric ID. |
| `object_lookup` | You need all spawn locations and actions for a specific game object ID from the OSRS cache. |
| `objects_near` | You need to discover what objects or NPCs exist within a tile radius of a known coordinate. |
| `npc_spawns` | You need exact spawn coordinates for an NPC across all 24,000+ known spawns (cache-derived, faster than mejrs map). |
| `temple_lookup` | You need TempleOSRS-canonical item IDs for the collection log (these differ from wiki IDs for some items), or EHB rates. |
| `prices_lookup` | You need GE price data for an item by name or ID. |
| `widget_id_lookup` | You need interface group/child IDs for `highlightWidgetIds` in a guidance step. |
| `runelite_id_lookup` | You need a constant from RuneLite's ItemID, NpcID, ObjectID, Varbits, VarPlayer, or AnimationID by name or value. |
| `coordinate_helper` | You need to validate a world coordinate, look up what's at a location, calculate tile distance, or find the region/chunk for a coordinate. |

**Drop-rate math**

| Tool | Use when… |
|------|-----------|
| `search_drop_rates` | You need to find an existing source or item in `drop_rates.json` by name, item ID, or category without reading the raw JSON. |
| `drop_rate_calc` | You need to calculate or cross-check efficiency scores using the same combined-rate formula as the plugin's `EfficiencyCalculator`. |
| `validate_drop_rates` | You want to run integrity checks (missing fields, invalid rates, guidance step problems, duplicate IDs) against `drop_rates.json` before filing a PR. |

**Cross-checking and lint**

| Tool | Use when… |
|------|-----------|
| `cross_check_ids` | You want to verify a list of item IDs against RuneLite's `ItemID.java` and confirm name matches before shipping a drop-table edit. |
| `compare_source` | You want a structured diff between an existing source's rates in `drop_rates.json` and the live OSRS Wiki drop table (missing items, rate mismatches, extra items, ID mismatches). |
| `guidance_lint` | You want deep validation of guidance steps beyond `validate_drop_rates` — coordinate-to-region consistency, completion condition logic, ACTOR_DEATH NPC consistency, step description quality. |
| `overlay_lint` | You want to scan Java overlay files for known anti-patterns (double `getLocalPlayer`, missing null checks, `client.getNpcs()` inside `render()`). |
| `plugin_hub_validate` | You want to statically validate the repo against Jagex plugin-hub submission rules before opening a PR to the hub. |

**Cache introspection**

| Tool | Use when… |
|------|-----------|
| `cache_scope` | You need to know what's covered (and what gaps exist) in the static data caches used by this MCP before asserting that a spawn or object is absent. |
| `cache_status` | You need to check the state of the MCP's in-memory HTTP cache (entry count, URLs, TTL remaining). |

**Codebase and authoring**

| Tool | Use when… |
|------|-----------|
| `codebase_search` | You need to find where a feature is implemented or trace a code reference in the Java source without a local IDE. |
| `authoring_log` | You need to parse events from an in-game recording session, or generate draft `guidanceSteps` JSON from a captured log. |
| `memory_append` | You have discovered a gotcha or lesson that future contributors should know — record it so the pipeline doesn't repeat the mistake. |
| `tail_log` | You need to inspect recent RuneLite client log output filtered by pattern (errors, plugin-specific lines). |

**Screen and scene capture**

These tools require a live RuneLite window. Use them only when Tier 1 lookup tools have failed or the data is genuinely only visible in-game.

| Tool | Use when… |
|------|-----------|
| `screenshot` | You need a full screenshot of the RuneLite window for visual confirmation. |
| `screenshot_diff` | You need to compare two screenshots and highlight changed pixels (before/after review). |
| `region_capture` | You need a cropped region of the window (sidebar panel, minimap, game viewport). |
| `ocr_screen` | You need to extract text from a RuneLite screenshot using Windows OCR. |

**Meta**

| Tool | Use when… |
|------|-----------|
| `resource_health` | You want a pre-flight check that all external data sources (Wiki API, TempleOSRS, prices API) are reachable before starting a pipeline. |
| `runelite_config` | You need to read the RuneLite configuration for a profile (plugin settings, overlay config). |
| `wiki_updates` | You want to check for recent OSRS Wiki drop-table edits that might affect `drop_rates.json` accuracy. |

## Tier 2 — committed registries

Before writing any value by hand, check whether it is already encoded in a committed data file:

- **`src/main/resources/com/collectionloghelper/drop_rates.json`** — The primary data store. Use `search_drop_rates` (Tier 1) to search it without reading the raw JSON. If a source is already present, its NPC IDs, coordinates, and guidance are the ground truth for the codebase and should only be changed after cross-checking against Tier 1 tools.
- **`src/main/resources/com/collectionloghelper/slayer_task_weights.json`** — Slayer master task weights for all four masters (148 tasks). Check here before looking up task weights elsewhere.
- **`src/main/resources/com/collectionloghelper/verified_scene_ids.json`** — *(TODO: link after A5.2 merges)* Verified scene object ID → name mappings for guidance steps. Once this registry ships, look here before using `object_lookup` or `objects_near` for objects that have already been verified.

## Tier 3 — in-game authoring (last resort)

In-game capture is appropriate for data that only exists as runtime state:

- **Instanced coordinate offsets** — boss rooms, raids, and other instanced content have coordinates that vary per session or are offset from a fixed map position.
- **Mid-fight phase transitions** — NPC ID changes between phases (e.g., Zulrah 2042/2043/2044) that fire `ACTOR_DEATH` on unexpected IDs.
- **Dynamic object spawns** — objects that appear only after a specific game event (chest unlocking, barrier dropping) and are not present in static cache data.

For everything else, Tier 1 covers it. In practice, in-game authoring should account for fewer than 5% of future data-addition sessions.

When in-game capture is needed, the `authoring_log` MCP tool (Tier 1) can parse the resulting log file and generate draft `guidanceSteps` JSON without manual transcription. The authoring mode logs `MENU`, `DEATH`, `OBJECT`, `NPC`, `DIALOG`, `ARRIVE`, and `CHAT` events to `~/.runelite/clh-authoring-log.txt`. See the [GuidanceStep schema](schema-reference.md#guidancestep) for the fields that each event type populates.

## Quick tool-selection reference

| Need | Best tool |
|------|-----------|
| NPC ID by name | `npc_lookup` |
| NPC spawn coordinates | `npc_spawns` or `coordinate_helper` (identify) |
| Object ID → name + locations | `object_lookup` |
| Objects near a tile | `objects_near` |
| Drop table from Wiki | `wiki_lookup` |
| Collection log item IDs (canonical) | `temple_lookup` |
| Item ID by name (RuneLite constant) | `runelite_id_lookup` |
| Coordinate validation | `coordinate_helper` (validate / identify) |
| Widget group/child ID | `widget_id_lookup` |
| Search existing data | `search_drop_rates` |
| Validate before PR | `validate_drop_rates` + `guidance_lint` |
| Efficiency score check | `drop_rate_calc` |
| Diff against Wiki | `compare_source` |
| Verify item IDs | `cross_check_ids` |
| In-game log parsing | `authoring_log` |
