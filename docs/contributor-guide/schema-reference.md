# JSON Schema Reference

All drop data lives in a single file:

```
src/main/resources/com/collectionloghelper/drop_rates.json
```

The file is an array of **sources** (bosses, raids, minigames, etc.), each containing an array of **items** (drops, rewards, unlocks).

## `CollectionLogSource`

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
| `killTimeSeconds` | int | Yes | Average **full cycle** time in seconds. See [Kill Time Convention](data-authoring.md#kill-time-convention) |
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

## `CollectionLogItem`

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
| `dropRate` | double | Yes | Drop probability as a decimal. See [Drop Rates](data-authoring.md#drop-rates) |
| `varbitId` | int | Yes | Varbit tracking the item's collection state. Use `0` if unknown |
| `isPet` | boolean | Yes | `true` if this item is a pet |
| `wikiPage` | string | Yes | OSRS Wiki page name (the part after `/w/`) |
| `independent` | boolean | No | `true` if this item rolls on a separate table from the main drop (pets, mutagens, raid uniques) |
| `requiresPrevious` | boolean | No | `true` if this item requires the preceding item in the list to be obtained first (e.g., Bludgeon pieces, Hydra rings) |
| `milestoneKills` | int | No | For guaranteed/milestone items: the kill count at which this item is obtained. Use `1` for one-time rewards |
| `pointCost` | int | No | For `SHOP` sources: the point cost to purchase this item |

## `GuidanceStep`

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

## Conditional Alternatives

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

## Requirements

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

## Waypoints

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
