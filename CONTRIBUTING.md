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
  "mutuallyExclusive": true,
  "rollsPerKill": 2,
  "items": [ ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Exact name as it appears in the collection log |
| `category` | string | Yes | One of: `BOSSES`, `RAIDS`, `CLUES`, `MINIGAMES`, `OTHER` |
| `worldX` | int | Yes | X coordinate of the boss/activity location |
| `worldY` | int | Yes | Y coordinate of the boss/activity location |
| `worldPlane` | int | Yes | World plane (0 = surface, 1+ = upper floors, etc.) |
| `killTimeSeconds` | int | Yes | Average kill/completion time in seconds for an efficient mid-to-late-game setup |
| `mutuallyExclusive` | boolean | No | Set `true` if only one unique can drop per kill (GWD, raids, clue caskets, etc.). Defaults to `false`. See [Drop Table Mechanics](#drop-table-mechanics) |
| `rollsPerKill` | int | No | Number of drop rolls per kill/completion. Defaults to `1`. Set for multi-roll sources (e.g., Zulrah: 2, Wintertodt: 10). See [Drop Table Mechanics](#drop-table-mechanics) |
| `rewardType` | string | No | One of: `DROP` (default), `SHOP`, `MIXED`, `GUARANTEED`, `MILESTONE`. Controls how the scoring algorithm handles the source |
| `locationDescription` | string | No | Human-readable location text shown in the item detail panel |
| `pointsPerHour` | double | No | Points earned per hour for `SHOP` sources (used in scoring) |
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
  "wikiPage": "Bandos_chestplate"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `itemId` | int | Yes | OSRS item ID (find on the [OSRS Wiki](https://oldschool.runescape.wiki/)) |
| `name` | string | Yes | Item name exactly as shown in-game |
| `dropRate` | double | Yes | Drop probability as a decimal (see [Drop Rates](#drop-rates) below) |
| `varbitId` | int | Yes | Varbit tracking the item's collection state. Use `0` if unknown |
| `isPet` | boolean | Yes | `true` if this item is a pet. Used by the "All Pets" efficiency view |
| `wikiPage` | string | Yes | OSRS Wiki page name (the part after `oldschool.runescape.wiki/w/`). URL-encode special characters (e.g., `%27` for apostrophes) |

## How to Find Data

### Item IDs

1. Go to the item's [OSRS Wiki](https://oldschool.runescape.wiki/) page
2. The item ID is shown in the infobox on the right side
3. Alternatively, search the wiki's item database: `https://oldschool.runescape.wiki/w/Special:Lookup?type=item&name=ITEM_NAME`

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

### Drop Table Mechanics

The scoring algorithm needs to know how a source's drop table works to calculate efficiency correctly.

**`mutuallyExclusive`** — Set to `true` when only one unique can drop per kill. This prevents the algorithm from summing all missing item rates (which would massively overestimate efficiency). Instead, it uses the highest single item rate. Examples:
- All GWD bosses (one unique roll per kill from a shared table)
- Raids (one unique per completion: CoX, ToB, ToA)
- Clue caskets (one unique per casket opening)
- Barrows (one item per chest)

**`rollsPerKill`** — Set when a source gives multiple independent drop rolls per kill/completion. The algorithm computes the effective per-kill rate as `1 - (1 - perRollRate)^rollsPerKill`. When using this field, item `dropRate` values should be **per-roll** rates. Examples:
- Zulrah: 2 rolls per kill
- Grotesque Guardians: 2 rolls per kill
- Wintertodt: ~10 rolls per game (mid-level assumption)

### Clue Scroll Items

Clue items drop from **caskets**, not from kills. The `dropRate` should reflect the chance per casket opened, not per clue scroll obtained. The `killTimeSeconds` is the average time to complete one clue of that tier. All clue sources should be tagged `mutuallyExclusive: true`.

### Raids (Point-Based Scaling)

Raid drop rates scale with contribution points. All raids should be tagged `mutuallyExclusive: true` since only one unique drops per completion.

- **Chambers of Xeric**: Rates scaled to a ~30,000 point raid (~3.45% unique chance), with correct item weights
- **Theatre of Blood**: Weighted rates per 4-man completion (~1/9.1 unique chance, items weighted by rarity)
- **Tombs of Amascut**: Rates per 300 invocation-level completion

### "All Pets" View

There is no separate "All Pets" source. Instead, mark pet items with `"isPet": true` in their respective source. The plugin's `EfficiencyCalculator.filterPetsOnly()` method automatically derives the pet efficiency view from all sources.

### Varbit IDs

Varbits track whether the player has obtained an item. If you know the varbit ID, include it. Otherwise, use `0` — we can fill these in later. Check the [RuneLite Varbit list](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/VarbitID.java) for known values.

## PR Process

### 1. Claim an Issue

- Browse [open issues](../../issues) or check [ITEMS_TRACKER.md](ITEMS_TRACKER.md) for unclaimed categories
- Comment on the issue to claim it (or self-assign if you have permissions)
- **Claim expiry**: Submit a PR within **2 weeks** or the claim is released for others

### 2. Fork and Branch

```bash
git clone https://github.com/YOUR_USERNAME/collection-log-helper.git
cd collection-log-helper
git checkout -b add-SOURCE_NAME
```

### 3. Add Your Data

Edit `src/main/resources/com/collectionloghelper/drop_rates.json`:

```json
{
  "name": "New Boss",
  "category": "BOSSES",
  "worldX": 1234,
  "worldY": 5678,
  "worldPlane": 0,
  "killTimeSeconds": 120,
  "items": [
    {
      "itemId": 12345,
      "name": "Unique drop",
      "dropRate": 0.00125,
      "varbitId": 0,
      "isPet": false,
      "wikiPage": "Unique_drop"
    }
  ]
}
```

### 4. Verify Your Changes

```bash
./gradlew build
```

Ensure the build passes — the JSON is loaded at startup and malformed data will cause errors.

### 5. Submit a Pull Request

- Push your branch and open a PR
- Reference the issue number in the PR description (e.g., "Closes #42")
- Fill out the PR template checklist

## Quality Checklist

Before submitting, verify:

- [ ] All item IDs match the OSRS Wiki
- [ ] Drop rates are converted to decimal correctly
- [ ] Item names match in-game names exactly
- [ ] Wiki page names resolve to valid wiki pages
- [ ] Pets are marked with `"isPet": true`
- [ ] `killTimeSeconds` reflects an efficient mid-to-late-game setup
- [ ] World coordinates point to the correct location
- [ ] `./gradlew build` passes
- [ ] The source `name` matches the collection log category name exactly
- [ ] The `category` is one of: `BOSSES`, `RAIDS`, `CLUES`, `MINIGAMES`, `OTHER`
