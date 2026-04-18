# Contributing to Collection Log Helper

Thank you for helping expand Collection Log Helper's coverage! This guide explains how to add new collection log sources and items to the plugin's dataset.

## Overview

All drop data lives in a single file:

```
src/main/resources/com/collectionloghelper/drop_rates.json
```

The file is an array of **sources** (bosses, raids, minigames, etc.), each containing an array of **items** (drops, rewards, unlocks).

## JSON Schema Reference

### `CollectionLogSource`

Each source represents a single collection log category (e.g., "General Graardor", "Chambers of Xeric").

```json
{
  "name": "Boss Name",
  "category": "BOSSES",
  "worldX": 2864,
  "worldY": 5354,
  "worldPlane": 2,
  "killTimeSeconds": 90,
  "ironKillTimeSeconds": 180,
  "afkLevel": 0,
  "mutuallyExclusive": true,
  "rollsPerKill": 2,
  "npcId": 2215,
  "interactAction": "Attack",
  "travelTip": "GWD tele -> Bandos",
  "requirements": {
    "quests": ["TROLL_STRONGHOLD"],
    "skills": [{"skill": "STRENGTH", "level": 70}]
  },
  "items": [ ... ],
  "guidanceSteps": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Exact name as it appears in the collection log |
| `category` | string | Yes | One of: `BOSSES`, `RAIDS`, `CLUES`, `MINIGAMES`, `OTHER` |
| `worldX` | int | Yes | X coordinate of the boss/activity location |
| `worldY` | int | Yes | Y coordinate of the boss/activity location |
| `worldPlane` | int | Yes | World plane (0 = surface, 1+ = upper floors, etc.) |
| `killTimeSeconds` | int | Yes | Average **full cycle** time in seconds. See [Kill Time Convention](#kill-time-convention) |
| `ironKillTimeSeconds` | int | No | Override kill time for ironman accounts (e.g., Corp, GWD, wilderness bosses) |
| `afkLevel` | int | No | AFK level: `0` = active, `1` = semi-AFK, `2` = AFK, `3` = very AFK. Used by the AFK filter |
| `mutuallyExclusive` | boolean | No | Set `true` if only one unique can drop per kill. Defaults to `false` |
| `mutuallyExclusiveSources` | array | No | List of source names that share a mutually exclusive loot group (e.g., GWD bosses, DT2 bosses, gauntlet variants) |
| `rollsPerKill` | int | No | Number of drop rolls per kill/completion. Defaults to `1` |
| `aggregated` | boolean | No | Set `true` for aggregated sources (e.g., Miscellaneous) that use best-item scoring |
| `rewardType` | string | No | One of: `DROP` (default), `SHOP`, `MIXED`, `GUARANTEED`, `MILESTONE` |
| `pointsPerHour` | double | No | Points earned per hour for `SHOP`/`MIXED` sources |
| `locationDescription` | string | No | Human-readable location text shown in the item detail panel |
| `travelTip` | string | No | Travel tip text shown in the guidance overlay |
| `npcId` | int | No | NPC ID to highlight during guidance |
| `interactAction` | string | No | Right-click action to highlight on the NPC (e.g., "Attack", "Talk-to") |
| `dialogOptions` | array | No | Dialog choices to highlight when talking to NPCs |
| `waypoints` | array | No | Requirement-gated alternate world locations. See [Waypoints](#waypoints) |
| `requirements` | object | No | Quest and skill requirements. See [Requirements](#requirements) |
| `guidanceSteps` | array | No | Ordered list of `GuidanceStep` objects. See [`GuidanceStep`](#guidancestep) |
| `items` | array | Yes | List of `CollectionLogItem` objects |

### `CollectionLogItem`

Each item represents a single drop or reward within a source.

```json
{
  "itemId": 11832,
  "name": "Bandos chestplate",
  "dropRate": 0.002625,
  "varbitId": 0,
  "isPet": false,
  "wikiPage": "Bandos_chestplate",
  "independent": false,
  "requiresPrevious": false,
  "milestoneKills": 0,
  "pointCost": 0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemId` | int | Yes | OSRS item ID — use [TempleOSRS](https://templeosrs.com/api/collection-log/items.php) first, Wiki as fallback |
| `name` | string | Yes | Item name exactly as shown in-game |
| `dropRate` | double | Yes | Drop probability as a decimal. See [Drop Rates](#drop-rates) |
| `varbitId` | int | Yes | Varbit tracking the item's collection state. Use `0` if unknown |
| `isPet` | boolean | Yes | `true` if this item is a pet |
| `wikiPage` | string | Yes | OSRS Wiki page name (the part after `/w/`) |
| `independent` | boolean | No | `true` if this item rolls on a separate table from the main drop (pets, mutagens, raid uniques) |
| `requiresPrevious` | boolean | No | `true` if this item requires the preceding item in the list to be obtained first (e.g., Bludgeon pieces, Hydra rings) |
| `milestoneKills` | int | No | For guaranteed/milestone items: the kill count at which this item is obtained. Use `1` for one-time rewards |
| `pointCost` | int | No | For `SHOP` sources: the point cost to purchase this item |

### `GuidanceStep`

Sources that require multiple actions can define an ordered list of guidance steps. When "Guide Me" is activated, the plugin walks the player through each step sequentially.

```json
{
  "description": "Get a Mossy key from moss giants",
  "worldX": 3170,
  "worldY": 9899,
  "worldPlane": 0,
  "travelTip": "Varrock teleport -> sewers",
  "npcId": 0,
  "completionCondition": "INVENTORY_HAS_ITEM",
  "completionItemId": 22374,
  "objectId": 0,
  "objectIds": null,
  "objectInteractAction": null,
  "highlightItemIds": null,
  "groundItemIds": null,
  "useItemOnObject": false,
  "loopBackToStep": 0,
  "loopCount": 0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `description` | string | Yes | What the player should do in this step |
| `worldX` | int | No | Target X coordinate (0 = no location overlay) |
| `worldY` | int | No | Target Y coordinate |
| `worldPlane` | int | No | Target world plane |
| `npcId` | int | No | NPC to highlight at the target location |
| `interactAction` | string | No | Right-click action to highlight (e.g., "Attack", "Talk-to") |
| `dialogOptions` | array | No | Dialog choices to highlight when talking to an NPC |
| `travelTip` | string | No | Travel tip shown in the overlay |
| `requiredItemIds` | array | No | Item IDs that must be in inventory. If missing, the plugin redirects to a bank |
| `completionCondition` | string | Yes | How the step is detected as complete. See below |
| `completionItemId` | int | No | Item ID for item-based completion checks |
| `completionItemCount` | int | No | Required quantity for `INVENTORY_HAS_ITEM` (default: 1) |
| `completionDistance` | int | No | Tile distance threshold for `ARRIVE_AT_TILE` (default: 5) |
| `completionNpcId` | int | No | NPC ID for `NPC_TALKED_TO` or `ACTOR_DEATH` checks |
| `completionChatPattern` | string | No | Regex pattern for `CHAT_MESSAGE_RECEIVED` checks |
| `worldMessage` | string | No | Chat message displayed when this step activates |
| `objectId` | int | No | Game object ID to highlight |
| `objectIds` | array of int | No | Additional object IDs to highlight (e.g., both team variants) |
| `objectInteractAction` | string | No | Action text to display on the highlighted object |
| `objectFilterTiles` | array | No | Exact tiles `[[x,y,plane], ...]` to restrict object highlighting. Use when multiple instances of the same object ID exist but only specific ones are correct |
| `objectMaxDistance` | int | No | Max tile distance from step worldX/worldY to highlight objects (alternative to objectFilterTiles) |
| `highlightItemIds` | array of int | No | Item IDs to highlight in the player's inventory |
| `groundItemIds` | array of int | No | Item IDs to highlight on the ground |
| `useItemOnObject` | boolean | No | Show "Use X on Y" prompts instead of simple action labels |
| `loopBackToStep` | int | No | 1-indexed step to loop back to when this step completes (0 = no loop) |
| `loopCount` | int | No | Number of additional loop iterations after the first pass (0 = no loop). Total passes = 1 + loopCount |

**Completion conditions:**
- `ITEM_OBTAINED` — Auto-advances when the item appears in the collection log
- `INVENTORY_HAS_ITEM` — Auto-advances when the player has `completionItemCount` of the item
- `INVENTORY_NOT_HAS_ITEM` — Auto-advances when the item is no longer in inventory
- `ARRIVE_AT_TILE` — Auto-advances when within `completionDistance` tiles of the target
- `ARRIVE_AT_ZONE` — Auto-advances when the player enters the target zone (rectangular area). Uses `completionZone` field
- `PLAYER_ON_PLANE` — Auto-advances when the player is on the specified `worldPlane`
- `NPC_TALKED_TO` — Auto-advances when the player interacts with the specified NPC
- `ACTOR_DEATH` — Auto-advances when the specified NPC dies
- `CHAT_MESSAGE_RECEIVED` — Auto-advances when a chat message matches `completionChatPattern`
- `MANUAL` — Player must click "Next Step" or "Skip" in the panel

**Additional step fields:**

| Field | Type | Description |
|-------|------|-------------|
| `completionZone` | int[5] | For `ARRIVE_AT_ZONE`: `[minX, minY, maxX, maxY, plane]` defining the rectangular area |
| `highlightWidgetIds` | int[][] | Widget IDs to highlight: `[[groupId, childId], ...]` for game interface elements |
| `conditionalAlternatives` | array | Dynamic step branching based on player state. See [Conditional Alternatives](#conditional-alternatives) |

### Conditional Alternatives

Steps can dynamically select different paths based on the player's quest completions, skill levels, or other requirements. When a step has `conditionalAlternatives`, the sequencer evaluates each alternative's requirements and uses the first match:

```json
{
  "description": "Travel to Cerberus (default: walk)",
  "worldX": 2884, "worldY": 3395, "worldPlane": 0,
  "completionCondition": "ARRIVE_AT_TILE",
  "conditionalAlternatives": [
    {
      "requirements": {
        "quests": ["FAIRYTALE_II__CURE_A_QUEEN"]
      },
      "description": "Use Fairy ring BIP to reach Taverley Dungeon.",
      "worldX": 2763, "worldY": 5130, "worldPlane": 0,
      "travelTip": "Fairy ring BIP"
    },
    {
      "requirements": {
        "skills": [{"skill": "AGILITY", "level": 70}]
      },
      "description": "Use the pipe shortcut in Taverley Dungeon.",
      "travelTip": "Taverley Dungeon pipe shortcut"
    }
  ]
}
```

Only non-null fields in the alternative override the base step — null fields fall through to the parent. If no alternative's requirements are met, the base step is used as fallback.

### Requirements

Sources can specify quest and skill requirements. Locked sources are indicated in the UI and can be hidden.

```json
{
  "requirements": {
    "quests": ["SONG_OF_THE_ELVES"],
    "skills": [
      {"skill": "SLAYER", "level": 95}
    ]
  }
}
```

### Waypoints

Sources can have multiple waypoint locations gated by requirements. The plugin uses the first accessible waypoint.

```json
{
  "waypoints": [
    {
      "name": "Fairy ring (BJS)",
      "worldX": 2150,
      "worldY": 3861,
      "worldPlane": 0,
      "requirements": {"quests": ["FAIRYTALE_II"]}
    },
    {
      "name": "Walk from Canifis",
      "worldX": 3500,
      "worldY": 3486,
      "worldPlane": 0
    }
  ]
}
```

## Data sourcing — try MCP first, in-game last

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

### Tier 1 — MCP tools (try first)

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

### Tier 2 — committed registries

Before writing any value by hand, check whether it is already encoded in a committed data file:

- **`src/main/resources/com/collectionloghelper/drop_rates.json`** — The primary data store. Use `search_drop_rates` (Tier 1) to search it without reading the raw JSON. If a source is already present, its NPC IDs, coordinates, and guidance are the ground truth for the codebase and should only be changed after cross-checking against Tier 1 tools.
- **`src/main/resources/com/collectionloghelper/slayer_task_weights.json`** — Slayer master task weights for all four masters (148 tasks). Check here before looking up task weights elsewhere.
- **`src/main/resources/com/collectionloghelper/verified_scene_ids.json`** — *(TODO: link after A5.2 merges)* Verified scene object ID → name mappings for guidance steps. Once this registry ships, look here before using `object_lookup` or `objects_near` for objects that have already been verified.

### Tier 3 — in-game authoring (last resort)

In-game capture is appropriate for data that only exists as runtime state:

- **Instanced coordinate offsets** — boss rooms, raids, and other instanced content have coordinates that vary per session or are offset from a fixed map position.
- **Mid-fight phase transitions** — NPC ID changes between phases (e.g., Zulrah 2042/2043/2044) that fire `ACTOR_DEATH` on unexpected IDs.
- **Dynamic object spawns** — objects that appear only after a specific game event (chest unlocking, barrier dropping) and are not present in static cache data.

For everything else, Tier 1 covers it. In practice, in-game authoring should account for fewer than 5% of future data-addition sessions.

When in-game capture is needed, the `authoring_log` MCP tool (Tier 1) can parse the resulting log file and generate draft `guidanceSteps` JSON without manual transcription. The authoring mode logs `MENU`, `DEATH`, `OBJECT`, `NPC`, `DIALOG`, `ARRIVE`, and `CHAT` events to `~/.runelite/clh-authoring-log.txt`. See the [GuidanceStep schema](#guidancestep) above for the fields that each event type populates.

### Quick tool-selection reference

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

## How to Find Data

### Item IDs

Use [TempleOSRS Collection Log Items](https://templeosrs.com/api/collection-log/items.php) as the primary source for item IDs — Wiki IDs can differ for collection log purposes. Fall back to the [OSRS Wiki](https://oldschool.runescape.wiki/) if TempleOSRS doesn't have the item.

### Drop Rates

Convert wiki drop rates from fraction format to decimal:

| Wiki Format | Decimal | Notes |
|-------------|---------|-------|
| 1/128 | 0.007813 | `1 / 128 = 0.007813` |
| 1/5000 | 0.0002 | `1 / 5000 = 0.0002` |
| 3/128 | 0.023438 | `3 / 128 = 0.023438` |
| Always | 1.0 | Guaranteed drop |

Use at least 6 decimal places for precision.

### World Coordinates

1. Open the OSRS Wiki page for the boss/activity
2. The location is usually shown on the page's map
3. Use the [RuneLite Developer Tools](https://github.com/runelite/runelite/wiki/Developer-Tools) plugin to get exact coordinates in-game
4. `worldPlane` is typically `0` for surface locations

### Wiki Page Names

The `wikiPage` field is the URL path segment after `/w/` on the OSRS Wiki:
- `https://oldschool.runescape.wiki/w/Bandos_chestplate` → `"Bandos_chestplate"`
- `https://oldschool.runescape.wiki/w/Saradomin%27s_light` → `"Saradomin%27s_light"`

## Special Cases

### Kill Time Convention

`killTimeSeconds` represents the **average full cycle time** from one loot opportunity to the next. This is NOT just the kill/action duration — it includes all overhead:

| Source Type | What to Include |
|-------------|----------------|
| **Bosses** | Kill time + respawn timer + banking/resupply trip |
| **Minigames** | Full game/round duration (including queue, setup, rewards) |
| **Event-based** (Forestry, Random Events) | Average interval between events, not event duration |
| **Slayer** | Per-creature kill time (task overhead handled separately) |
| **Clues** | Average time to complete one clue of that tier |
| **Skilling** (Camdozaal, Aerial Fishing) | Time per action/attempt that can yield the drop |

Kill times should be aligned with [TempleOSRS EHB](https://templeosrs.com) rates where available.

### Drop Table Mechanics

The scoring algorithm needs to know how a source's drop table works to calculate efficiency correctly.

**`mutuallyExclusive`** — Set to `true` when only one unique can drop per kill. This prevents the algorithm from summing all missing item rates. Examples:
- All GWD bosses (one unique roll per kill from a shared table)
- Raids (one unique per completion: CoX, ToB, ToA)
- Clue caskets (one unique per casket opening)

**`rollsPerKill`** — Set when a source gives multiple independent drop rolls per kill/completion. When using this field, item `dropRate` values should be **per-roll** rates. Examples:
- Zulrah: 2 rolls per kill
- Wintertodt: ~10 rolls per game

**`independent`** (on items) — Set to `true` for items that roll on a separate table from the main drop. The scoring algorithm handles standard and independent drops with separate combined-rate calculations.

### Clue Scroll Items

Clue items drop from **caskets**, not from kills. The `dropRate` should reflect the chance per casket opened. The `killTimeSeconds` is the average time to complete one clue of that tier.

### Raids (Point-Based Scaling)

Raid drop rates scale with contribution points. All raids should be tagged `mutuallyExclusive: true`. Rates are stored as **per-raid solo rates** — team size scaling is applied dynamically via the RaidTeamSize config.

- **Chambers of Xeric**: Per-raid solo rates at ~20,000 points. CM uses a 2.067x multiplier on normal rates
- **Theatre of Blood**: Per-completion solo rates. ToB Hard Mode is a separate source with better base rates and 3 HM-exclusive items
- **Tombs of Amascut**: Per-raid solo rates at each invocation level (150/300/500 as separate sources). At RL 500, unique weightings shift per the June 2025 rebalance

### Milestone Items (KC Thresholds)

Some items are guaranteed at specific kill count milestones rather than being random drops. Use `"dropRate": 1.0` with `"milestoneKills": N` where N is the KC threshold:

- **Xeric's capes** (CoX CM): 100/500/1000/1500/2000 CM completions
- **Icthlarin's shrouds** (ToA): 100/500/1000/1500/2000 completions
- **Sinhaza shrouds** (ToB): 100/500/1000/1500/2000 completions

Use `requiresPrevious: true` on tiers 2+ since they unlock sequentially.

### Cumulative Rates (Multi-Phase Bosses)

Some bosses involve multiple phases or attempts per "completion," each with independent loot rolls. Store the **cumulative per-completion probability**, not the per-phase rate:

- **Doom of Mokhaiotl**: Delving to depth 10 gives 10 independent rolls (depths 1-10) with improving rates. Use the cumulative `1 - Π(1 - p_depth)` across all depths
- **Sol Heredit**: 12 Colosseum waves with independent rolls from wave 4+. Use the cumulative per-completion rate

### "All Pets" View

Mark pet items with `"isPet": true` in their respective source. The plugin derives the pet efficiency view automatically.

### Varbit IDs

Varbits track whether the player has obtained an item. If you know the varbit ID, include it. Otherwise, use `0`.

## Deep guidance bar — the 10 elements of a fully authored source

A source that ships with *one* travel step and an `ACTOR_DEATH` auto-complete is better than nothing, but it is not yet "Quest-Helper depth." The table below defines the 10 elements that together constitute a fully authored source. Each element has a one-line definition, a pointer to a real source in this repo that demonstrates it well, and a note on the most common mistake. Run `guidance_lint` (see [Tier 1 tools](#tier-1--mcp-tools-try-first)) on your source before filing a PR — a clean or INFO-only lint report is a good signal that the structural scaffolding is sound.

This bar is the authoring target for [Tier D (D2-D4)](docs/ROADMAP.md#tier-d--coverage-breadth). See also decision [D-03](docs/ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs) for the tiered data-sourcing strategy that tells you *where to get the data* for each element.

---

### Element 1 — Travel tip with teleport hierarchy

**Definition.** The `travelTip` field on the source (and optionally on individual `guidanceSteps`) gives the player the fastest realistic teleport in a short string. When multiple teleport options exist, the preferred one is listed first. When access depends on a quest or skill, the source also declares `waypoints` so the plugin can serve an appropriate route to players who lack the preferred unlock.

**Good example:** `General Graardor` — `"travelTip": "Trollheim teleport -> GWD"`, backed by two `waypoints` entries: one for players who have completed Eadgar's Ruse (Trollheim teleport) and a fallback walking route from Death Plateau for those who have not.

**Common pitfall.** Writing only the *fastest* route without a fallback waypoint. New accounts without Eadgar's Ruse silently receive a travel tip that requires a quest they haven't done. Always pair an unlocked-ability travel tip with at least one unconditional fallback waypoint.

> Further reading: [`feedback_object_step_ux_pattern.md`](https://github.com/cha-ndler/runelite-dev-toolkit/blob/main/memory-seed/feedback_object_step_ux_pattern.md) — covers how to set `worldX/Y` correctly on steps so the hint arrow lands on the right tile.

---

### Element 2 — Tile-sequence or auto-arrival waypoint

**Definition.** Every meaningful location change in the guidance flow must be captured as a step with `completionCondition: ARRIVE_AT_TILE` (or `ARRIVE_AT_ZONE` / `PLAYER_ON_PLANE` for multi-floor transitions) and accurate `worldX/worldY/worldPlane` coordinates. The player should never have to wonder "has the plugin noticed I arrived?" — auto-detection fires the step forward without a manual click.

**Good example:** `Barrows` — nine ordered steps, each targeting a specific mound or tunnel, advancing automatically on `ARRIVE_AT_TILE` or `VARBIT_AT_LEAST`. The kill sequence never stalls waiting for a manual "Next" click.

**Common pitfall.** Using `completionCondition: MANUAL` for every step. Manual steps are correct for dialog choices and puzzle interactions where state cannot be detected; they are wrong for travel legs and arrivals where tile or plane detection works cleanly. Reserve `MANUAL` for steps that are genuinely undetectable.

> **Planned schema extension (B2):** Tier B will add `waypointTiles` — an ordered list of intermediate tiles a player must cross in sequence within a single step. This will replace multi-step travel chains with a single richer step once that milestone ships. Do not block authoring on it now; tile-sequence steps written today will be migrated automatically.

---

### Element 3 — Combat gear recommendation or skilling kit

**Definition.** The kill-step description (the step that ends with `ACTOR_DEATH` or equivalent) explicitly names the recommended prayer, the most important offensive swap, and any mechanic-specific item (e.g., Crumble Undead for shades, Bulwark for Corp). Skilling sources name the tier of tool or outfit needed for viable points-per-hour.

**Good example:** `Vorkath` — `"Kill Vorkath. Use Protect from Ranged for standard phase, dodge acid pools, and click the Zombified Spawn immediately when it appears"` names the prayer, the hazard to avoid, and the mechanic-specific action, all in one sentence.

**Common pitfall.** Saying only "Kill the boss" with no mechanical context. A player who has never done the content cannot act on that. You do not need to replicate the Wiki strategy page — one sentence covering the defensive prayer and the single most punishing mechanic is the minimum bar.

---

### Element 4 — Kill-loop / activity-loop detection

**Definition.** Sources that require multiple repetitive cycles before a reward (repeated chest runs, minigame rounds, shade-burning loops) declare a `loopBackToStep` and `loopCount` on the final step of each cycle to return the sequencer to the start of the loop. The player sees "do this again" rather than a dead-ended sequence.

**Good example:** `Shades of Mort'ton` — the chest-opening step loops back to the pyre-burning step via `loopBackToStep`, cycling until all shade keys are spent. `skipIfHasAnyItemIds` on the pyre step skips burning when keys are already held, preventing a spurious bank redirect.

**Common pitfall.** Adding a loop on a *boss* source. Single-kill boss loops should be handled by `ACTOR_DEATH` completion (which advances to a new kill automatically) rather than an explicit loop. Use `loopBackToStep` only for multi-action cycles within a single "session" — minigames, gathering loops, pyre runs.

> **Planned schema extension (B1):** Tier B will add composable AND/OR completion conditions so a loop can exit on either "inventory full" or "key obtained," without separate steps for each branch. Author loops with the current single-condition model for now; the AND/OR extension will layer on top.

---

### Element 5 — Safespot or strategy note where relevant

**Definition.** When a boss or creature has a well-known safespot, a phase transition that trips new players, or a mechanic that dramatically affects survivability (ghost specials, acid phase, Woox walk), the relevant kill step must name it. "Relevant" means: if a first-time player following the guidance dies to something predictable and avoidable, the element is missing.

**Good example:** `Cerberus` — `"Kill Cerberus. Protect from Magic, watch for the triple ghost special attack"` names both the prayer and the mechanic that kills unprepared players. Short, actionable, in the step description where the player can see it while fighting.

**Common pitfall.** Front-loading all strategy detail into the travel step, where the player has already scrolled past it by the time they need it. Strategy context belongs in the step where it is *actionable* — either the arrival step (to prompt preparation) or the kill step (for in-fight reminders).

---

### Element 6 — Bank-and-return detection

**Definition.** If a step requires a consumable item (a key, a pouch, a teleport scroll) that is not always in the player's inventory, that step must declare `requiredItemIds`. When the plugin detects the item is missing, it redirects the player to a bank before proceeding. Without this, the player gets stuck at a locked door with no guidance on what went wrong.

**Good example:** `Bryophyta` — step 0 declares `"requiredItemIds": [22374]` (Mossy key). Players who arrive at the sewer without one are redirected to the bank automatically.

**Common pitfall.** Omitting `requiredItemIds` on any step that references a consumable in its description. If the description says "use X key on the gate," the step needs `requiredItemIds` containing the key's item ID. The `guidance_lint` tool flags this pattern as a warning when a step description mentions "key" or "requires" but has no `requiredItemIds`.

---

### Element 7 — Inventory loadout

**Definition.** The travel step (step 0, or the first step with `completionCondition: ARRIVE_AT_TILE`) includes `requiredItemIds` for every consumable the player *must* bring before they can complete the source. The step description names the loadout explicitly so the player knows what to withdraw before leaving the bank.

**Good example:** `Obor` — `"requiredItemIds": [20754]` (Giant key) on the first travel step; description says "Travel to the Edgeville Dungeon or Varrock Sewers hill giant area." The key requirement surfaces at the earliest possible step, before the player wastes a run to a locked gate.

**Common pitfall.** Listing *all* gear in `requiredItemIds` — food, potions, weapon, armour. The redirect flow is designed for consumables or unlockables that are uncommon or easy to forget, not for a full combat loadout. Including 20+ item IDs in `requiredItemIds` defeats the purpose and makes the redirect fire constantly.

---

### Element 8 — Account-state prerequisites (quests, diaries, items)

**Definition.** The source's top-level `requirements` object lists every quest and skill level the player must have to access the content. This populates the "locked" indicator in the panel and drives `conditionalAlternatives` branching (Tier B) when different travel routes depend on unlocked quests. It is separate from and complementary to in-step `requiredItemIds`.

**Good example:** `Alchemical Hydra` — `"requirements": {"skills": [{"skill": "SLAYER", "level": 95}]}` combined with two `waypoints` (fairy ring CIR for players with Fairytale II unlocked, walking route fallback). The source is hidden for accounts below 95 Slayer, and the travel route adapts to fairy ring access.

**Common pitfall.** Setting `requirements` for the *preferred* route only. If accessing the boss requires a quest, the quest must be in `requirements.quests` — even if an alternate unquested route exists. A player who cannot do the content should see it as locked, not receive a travel tip for a route they cannot complete. Use `waypoints` to handle the unlocked-route variation, not `requirements`.

> See [`feedback_category_taxonomy.md`](https://github.com/cha-ndler/runelite-dev-toolkit/blob/main/memory-seed/feedback_category_taxonomy.md) for category assignment rules — getting `category` right is a prerequisite-adjacent concern because the panel's locked-source filtering is category-aware.

---

### Element 9 — Drop rate confidence (Wiki vs TempleOSRS vs community)

**Definition.** Every item has a `dropRate` value with at least six decimal places of precision, sourced from the highest-confidence data tier available for that data type. The source citation is not stored in the JSON (the file has no comment fields), but the contributor should know where the rate came from and be prepared to justify it in the PR description. Rates that differ materially from the Wiki must note the reconciliation reason in the PR.

**Good example:** `Zulrah` — zero `guidance_lint` issues; `rollsPerKill: 2` reflects the actual dual-table mechanic; `independent: true` on the mutagen items marks their separate table correctly. The rates align with TempleOSRS EHB math.

**Common pitfall.** Using a Wiki drop rate when TempleOSRS or the Log Hunters spreadsheet has a more accurate community-audited figure. Per decision [D-03](docs/ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs), the source-of-truth precedence for drop rates is: **Wiki first**, **Log Hunters second**, **TempleOSRS third** for rate precision. Collection log item IDs are the inverse — use TempleOSRS canonical IDs, not the Wiki item IDs, which diverge for some items.

> See [`feedback_verify_data_against_source.md`](https://github.com/cha-ndler/runelite-dev-toolkit/blob/main/memory-seed/feedback_verify_data_against_source.md) for the reconciliation procedure when sources disagree.

---

### Element 10 — Author's date stamp and source citations

**Definition.** Every PR that adds or materially edits a source must include in the PR description: (a) the game version or update the data was authored against, (b) the primary and secondary data sources used for drop rates, coordinates, and NPC IDs, and (c) whether `guidance_lint` and `validate_drop_rates` passed clean. This information lives in the PR, not in the JSON — it forms an audit trail for future contributors who need to know when data was last verified and against what source.

**Good example:** `Giants' Foundry` — a shop source with `"rewardType": "SHOP"`, `"pointsPerHour": <value>`, and `requirements` covering the prerequisite quest and smithing level. A PR for this source should cite the Wiki reward shop table, the TempleOSRS EHB rate for Kovac's reputation grind, and note the game update month the point costs were verified against.

**Common pitfall.** Opening a PR with no data-source citations. Reviewers cannot verify drop rates without knowing the source. When a rate looks wrong six months later and the PR has no citations, the only option is to re-audit from scratch. One sentence per data type (rates: Wiki; item IDs: TempleOSRS; coordinates: MCP `coordinate_helper`; NPC IDs: MCP `npc_lookup`) is the minimum.

---

### Deep guidance checklist

Before marking a source fully authored, verify all 10 elements are present or explicitly waived (with reason):

- [ ] **Travel tip** — `travelTip` set on source; at least one `waypoints` fallback if the primary tip requires a quest unlock
- [ ] **Auto-arrival** — every meaningful location change uses `ARRIVE_AT_TILE`, `ARRIVE_AT_ZONE`, or `PLAYER_ON_PLANE`; `MANUAL` only where state cannot be detected
- [ ] **Gear / loadout description** — kill step (or skilling equivalent) names the recommended prayer + key mechanic in plain English
- [ ] **Loop detection** — multi-cycle sources declare `loopBackToStep` / `loopCount`; single-kill bosses do not
- [ ] **Strategy note** — any mechanic that kills an unprepared player is named in the step description where it is actionable
- [ ] **Bank-and-return** — consumables required to enter or complete a step are in `requiredItemIds` on the earliest step that needs them
- [ ] **Inventory loadout** — the travel step names what the player must bring and includes `requiredItemIds` for hard-to-replace items
- [ ] **Prerequisites** — `requirements.quests` and `requirements.skills` reflect the *minimum* needed to access content; waypoints cover the route variation
- [ ] **Drop rate confidence** — rates sourced from the correct tier (see [D-03](docs/ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs)); `independent` and `rollsPerKill` set correctly
- [ ] **PR citations** — PR description cites game version, data sources, and `guidance_lint` / `validate_drop_rates` pass status

---

## PR Process

### 1. Claim an Issue

- Browse [open issues](../../issues) for unclaimed work
- Comment on the issue to claim it
- **Claim expiry**: Submit a PR within **2 weeks** or the claim is released

### 2. Fork and Branch

```bash
git clone https://github.com/YOUR_USERNAME/collection-log-helper.git
cd collection-log-helper
git checkout -b add-SOURCE_NAME
```

### 3. Add Your Data

Edit `src/main/resources/com/collectionloghelper/drop_rates.json` and add your source/items following the schema above.

### 4. Verify Your Changes

```bash
./gradlew build
```

Ensure the build passes — the JSON is loaded at startup and malformed data will cause errors.

### 5. Submit a Pull Request

- Push your branch and open a PR
- Reference the issue number in the PR description (e.g., "Closes #42")

## Quality Checklist

Before submitting, verify:

- [ ] All item IDs verified against [TempleOSRS](https://templeosrs.com/api/collection-log/items.php) first, Wiki as fallback
- [ ] Drop rates are converted to decimal correctly
- [ ] Item names match in-game names exactly
- [ ] Wiki page names resolve to valid wiki pages
- [ ] Pets are marked with `"isPet": true`
- [ ] Independent drops marked with `"independent": true`
- [ ] `killTimeSeconds` reflects full cycle time, aligned with TempleOSRS EHB where available
- [ ] World coordinates point to the correct location
- [ ] `./gradlew build` passes
- [ ] The source `name` matches the collection log category name exactly
- [ ] The `category` is one of: `BOSSES`, `RAIDS`, `CLUES`, `MINIGAMES`, `OTHER`
