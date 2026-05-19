# Data Authoring Guide

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
