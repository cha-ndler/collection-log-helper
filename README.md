# Collection Log Helper

A RuneLite plugin that guides players through efficient collection log completion with intelligent efficiency scoring, on-screen guidance overlays, and multiple viewing modes.

**Coverage:** Expanding toward full collection log support (~1,698 items) — [see current progress](ITEMS_TRACKER.md)

## Features

### Efficiency Scoring

Ranks collection log sources by how efficiently you can obtain missing items. The score factors in combined drop rates and average kill times to suggest the most time-effective grinds.

### Four Display Modes

- **Efficient** — Missing items ranked by efficiency score (items per expected hour)
- **Category Focus** — Items grouped by collection log tab (Bosses, Raids, Clues, Minigames, Other) with progress bars and collapsible sections
- **Search** — Full-text search across all items and sources
- **Pet Hunt** — Pet drops only, ranked by efficiency

### Guidance Overlays

Click "Guide Me" on any item to activate three navigation aids:

- **Tile highlight** — Cyan marker on the target location in the game world
- **Minimap dot** — Cyan indicator on the minimap
- **World map marker** — Clickable pin on the world map with edge snapping

### Item Details

Click any item to see its full info: source, category, drop rate (as 1/X), kill time, world coordinates, obtained status, and links to the OSRS Wiki.

### Live Tracking

Automatically detects new collection log entries via chat messages and varbit changes, then recalculates efficiency rankings in real time.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| Default Mode | Efficient | Which mode opens on startup |
| Show Overlays | On | Toggle all guidance overlays |
| Overlay Color | Cyan | Customize the overlay highlight color |

## Data

All drop data lives in [`src/main/resources/com/collectionloghelper/drop_rates.json`](src/main/resources/com/collectionloghelper/drop_rates.json). Each source includes world coordinates, kill time, and items with OSRS item IDs, decimal drop rates, pet flags, and wiki page links.

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
