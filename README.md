# Collection Log Helper

A RuneLite plugin that guides players through efficient collection log completion with intelligent efficiency scoring, on-screen guidance overlays, and multiple viewing modes.

**Coverage:** Expanding toward full collection log support (~1,698 items across 128 sources) — [see current progress](ITEMS_TRACKER.md)

## Features

### Efficiency Scoring

Ranks collection log sources by how efficiently you can obtain missing items. The score factors in drop rates, kill times, and drop table mechanics to suggest the most time-effective grinds. Sources with mutually exclusive unique tables (GWD, raids, clue scrolls, etc.) use the highest individual item rate instead of summing rates, preventing inflated rankings. Multi-roll sources (Zulrah, Wintertodt, etc.) account for multiple drop chances per kill. Reward shop and guaranteed items are scored separately from probabilistic drops.

### Account-Aware Requirements

Sources are checked against your account's quest completions and skill levels. Locked content (e.g., Corrupted Gauntlet without Song of the Elves, Alchemical Hydra without 95 Slayer) can be hidden or shown with lock indicators. The Top Pick always recommends accessible content.

### Five Display Modes

- **Efficient** — Missing items ranked by efficiency score (items per expected hour)
- **Category Focus** — Items grouped by collection log tab (Bosses, Raids, Clues, Minigames, Other) with progress bars and collapsible sections
- **Search** — Full-text search across all items and sources
- **Pet Hunt** — Pet drops only, ranked by efficiency
- **Efficient by Proximity (Experimental)** — Nearby sources ranked by a composite score of efficiency and distance from your current location

### Automatic Sync

Opens your in-game Collection Log once per session to automatically detect all obtained items — no manual browsing required. A configurable reminder nudges you to open the log after login if you haven't synced yet.

### Guidance Overlays

Click "Guide Me" on any item to activate navigation aids:

- **Tile highlight** — Cyan marker on the target location in the game world
- **Minimap dot** — Cyan indicator on the minimap
- **World map marker** — Clickable pin on the world map with edge snapping
- **Hint arrow** — Native yellow hint arrow at the target (same as Quest Helper)
- **Shortest Path integration** — Automatically requests a route from the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin if installed

### Item Details

Click any item to see its full info: source, category, drop rate (as 1/X), kill time, world coordinates, obtained status, and links to the OSRS Wiki.

### Live Tracking

Automatically detects new collection log entries via chat messages and varbit changes, then recalculates efficiency rankings in real time.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| Default Mode | Efficient | Which mode opens on startup |
| Hide Obtained Items | On | Hide items already obtained from the list |
| Hide Locked Content | On | Hide sources requiring quests or skills you haven't met |
| Show Sync Reminder | On | Remind you to open the Collection Log after login |
| **Guidance** | | |
| Show Overlays | On | Toggle all guidance overlays |
| Show Hint Arrow | On | Show the yellow hint arrow at the target |
| Shortest Path Integration | On | Request pathfinding from Shortest Path plugin |
| Overlay Color | Cyan | Customize the overlay highlight color |
| **Proximity** | | |
| Max Distance | 0 (unlimited) | Filter proximity mode results to within this tile distance |

## Data

All drop data lives in [`src/main/resources/com/collectionloghelper/drop_rates.json`](src/main/resources/com/collectionloghelper/drop_rates.json). Each source includes world coordinates, kill time, drop table mechanics (`mutuallyExclusive`, `rollsPerKill`), and items with OSRS item IDs, decimal drop rates, pet flags, and wiki page links.

## Building

```bash
./gradlew build
```

Requires Java 11+. Uses RuneLite client API (latest release), Lombok, and JUnit 4.

## Contributing

We're actively expanding toward full collection log coverage. See:

- [**CONTRIBUTING.md**](CONTRIBUTING.md) — JSON schema reference, how to find data, PR process
- [**ITEMS_TRACKER.md**](ITEMS_TRACKER.md) — Full coverage breakdown with claimable issues by priority

## Acknowledgments

This plugin references patterns from [Quest Helper](https://github.com/Zoinkwiz/quest-helper) (hint arrows, overlays) and [Shortest Path](https://github.com/Skretzo/shortest-path) (inter-plugin pathfinding via PluginMessage). See [CREDITS.md](CREDITS.md) for full details.

## License

BSD 2-Clause
