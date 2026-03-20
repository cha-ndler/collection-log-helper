# Collection Log Helper

A RuneLite plugin that guides players through efficient collection log completion with intelligent efficiency scoring, on-screen guidance overlays, and multiple viewing modes.

**Coverage:** 1,932 items across 192 sources — all item IDs verified against TempleOSRS, drop rates audited against OSRS Wiki and the [Log Hunters](https://discord.gg/loghunters) community spreadsheet.

## Features

### Efficiency Scoring

Ranks collection log sources by how efficiently you can obtain missing items. The score factors in drop rates, kill times, and drop table mechanics to suggest the most time-effective grinds:

- **Combined-rate scoring** — `1 - Π(1 - p_i)` for all missing items on a source, giving the probability of ANY new log slot per kill
- **Independent drop modeling** — Items on separate tables (pets, mutagens, raid uniques) are scored correctly using independent probability
- **Sequential dependencies** — Items requiring predecessors (Bludgeon pieces, Hydra rings, defender chain) are excluded from scoring until their prerequisite is obtained
- **Raid team size scaling** — Adjustable Solo/Duo/Trio/Full team size affects drop rates and kill times for raids
- **Slayer task overhead** — Task-only sources inflate effective kill time by 1/P(creature|master) when not on task
- **Main/Ironman toggle** — Separate completion rates for accounts with significantly different methods (Corp, GWD, wilderness bosses, etc.)
- **Mutually exclusive variants** — Raid difficulty variants (CoX/CoX CM, ToA 150/300/500) and shared-loot boss groups (GWD, DT2) are cross-referenced with "(also: ...)" notes

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
- **World map route line** — Colored line with arrowhead from player to target on the world map
- **Hint arrow** — Native yellow hint arrow at the target (same as Quest Helper)
- **NPC highlighting** — Hull highlight with action text on target NPCs
- **Object highlighting** — Game object highlights with "Use X on Y" prompts
- **Ground item highlighting** — Highlights lootable items relevant to current guidance step
- **Inventory item highlighting** — Arrow indicators on items needed for interactions
- **Dialog highlighting** — Highlights correct dialog choices matching the current source
- **Hover tooltips** — Rich tooltip info when hovering highlighted NPCs and objects
- **Right-click menu** — "Collection Log Guide" option on right-clicking tracked NPCs
- **Shortest Path integration** — Automatically requests a route from the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin if installed

### Step-by-Step Guidance

Sources with multi-step sequences (e.g., Bryophyta, Obor, Barbarian Assault) provide Quest Helper-style guided walkthroughs:

- **Step progress banner** — "Step 2/5: Enter Bryophyta's lair and kill her" displayed in the panel
- **Auto-completion detection** — Steps auto-advance when you obtain a key item, arrive at a tile, or interact with an NPC
- **Bank routing** — If a step requires items you don't have, the system redirects you to a bank first
- **Skip satisfied steps** — Steps whose conditions are already met (e.g., key already in inventory) are automatically skipped
- **Auto-advance** — When a sequence completes, guidance automatically activates for the next best source (configurable)

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
| Account Type | Main | Adjusts completion rates for main accounts vs ironmen |
| Raid Team Size | Solo | Your typical raid team size — adjusts drop rates and kill times |
| Show Sync Reminder | On | Remind you to open the Collection Log after login |
| **Guidance** | | |
| Auto-Advance Guidance | On | Automatically start guidance for the next best source when a sequence completes |
| Show Overlays | On | Toggle all guidance overlays |
| Show Hint Arrow | On | Show the yellow hint arrow at the target |
| Shortest Path Integration | On | Request pathfinding from Shortest Path plugin |
| Overlay Color | Cyan | Customize the overlay highlight color |
| **Proximity** | | |
| Max Distance | 0 (unlimited) | Filter proximity mode results to within this tile distance |

## Data

All drop data lives in [`src/main/resources/com/collectionloghelper/drop_rates.json`](src/main/resources/com/collectionloghelper/drop_rates.json). Each source includes world coordinates, kill times (main + iron), drop table mechanics (`mutuallyExclusive`, `rollsPerKill`, `mutuallyExclusiveSources`), quest/skill requirements, NPC IDs, guidance steps, and items with OSRS item IDs, decimal drop rates, pet flags, `independent`/`requiresPrevious` flags, and wiki page links. Kill times are aligned with TempleOSRS EHB rates.

## Building

```bash
./gradlew build
```

Requires Java 11+. Uses RuneLite client API (latest release), Lombok, and JUnit 4.

## Contributing

We're actively expanding toward full collection log coverage. See:

- [**CONTRIBUTING.md**](CONTRIBUTING.md) — JSON schema reference, how to find data, PR process

## Acknowledgments

This plugin references patterns from [Quest Helper](https://github.com/Zoinkwiz/quest-helper) (hint arrows, overlays, step sequencing) and [Shortest Path](https://github.com/Skretzo/shortest-path) (inter-plugin pathfinding). Kill time and scoring data validated against the [Log Hunters](https://discord.gg/loghunters) community's Log Adviser spreadsheet. See [CREDITS.md](CREDITS.md) for full details.

## License

BSD 2-Clause
