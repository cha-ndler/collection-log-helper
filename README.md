# Collection Log Helper

A RuneLite plugin that guides players through efficient collection log completion with intelligent efficiency scoring, on-screen guidance overlays, and multiple viewing modes.

**Coverage:** 2,111 items across 225 sources — all item IDs verified against TempleOSRS, drop rates audited against OSRS Wiki and the [Log Hunters](https://discord.gg/loghunters) community spreadsheet.

## Features

### Efficiency Scoring

Ranks collection log sources by how efficiently you can obtain missing items. The score factors in drop rates, kill times, and drop table mechanics to suggest the most time-effective grinds:

- **Combined-rate scoring** — `1 - Π(1 - p_i)` for all missing items on a source, giving the probability of ANY new log slot per kill
- **Independent drop modeling** — Items on separate tables (pets, mutagens, raid uniques) are scored correctly using independent probability
- **Sequential dependencies** — Items requiring predecessors (Bludgeon pieces, Hydra rings, defender chain) are excluded from scoring until their prerequisite is obtained
- **Raid team size scaling** — Adjustable Solo/Duo/Trio/Full team size affects drop rates and kill times for raids
- **Slayer task overhead** — Task-only sources inflate effective kill time by 1/P(creature|master) when not on task
- **Main/Ironman toggle** — Separate completion rates for accounts with significantly different methods (Corp, GWD, wilderness bosses, etc.)
- **Mutually exclusive variants** — Raid difficulty variants (CoX/CoX CM, ToB/ToB HM, ToA 150/300/500), DT2 Awakened bosses, and shared-loot boss groups are cross-referenced with "(also: ...)" notes

### Account-Aware Requirements

Sources are checked against your account's quest completions and skill levels. Locked content (e.g., Corrupted Gauntlet without Song of the Elves, Alchemical Hydra without 95 Slayer) can be hidden or shown with lock indicators. The Top Pick always recommends accessible content. A pre-flight warning alerts you to unmet requirements when activating guidance.

### Smart Travel Routing

The plugin detects your available teleports and transport methods, then shows only the travel options you can actually use:

- **Quest-based detection** — Fairy ring access, spirit tree network, spellbook unlocks
- **Bank scanning** — Detects teleport jewelry (games necklace, glory, slayer ring, etc.)
- **Diary detection** — Staff-free fairy rings (Lumbridge Elite), cloak teleports
- **Spellbook awareness** — Shows Arceuus/Lunar/Ancient teleports only when on that spellbook
- **Shortest Path integration** — Automatically requests optimal routes from the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin if installed

### Five Display Modes

- **Efficient** — Missing items ranked by efficiency score (items per expected hour)
- **Category Focus** — Items grouped by collection log tab (Bosses, Raids, Clues, Minigames, Other) with progress bars and collapsible sections
- **Search** — Full-text search across all items and sources
- **Pet Hunt** — Pet drops only, ranked by efficiency
- **Efficient by Proximity (Experimental)** — Nearby sources ranked by a composite score of efficiency and distance from your current location

### Automatic Sync

When you open your in-game Collection Log, the plugin automatically detects all obtained items — no manual browsing through each tab required. A configurable reminder nudges you to open the log after login if you haven't synced yet.

### Guidance Overlays

Click "Guide Me" on any item to activate navigation aids:

- **Tile highlight** — Marker on the target location in the game world
- **NPC highlighting** — Configurable hull, outline, or tile highlight with action text on target NPCs
- **Object highlighting** — Thin outline on game objects with "Use X on Y" prompts
- **Widget highlighting** — Highlights game interface elements (spells, prayers, equipment)
- **Inventory item highlighting** — Colored borders on items needed for interactions
- **Ground item highlighting** — Highlights lootable items relevant to current guidance step
- **Dialog highlighting** — Highlights correct dialog choices during NPC interactions
- **World map route** — Colored line with arrowhead from player to target on the world map
- **Minimap arrow** — Directional arrow pointing toward off-screen targets
- **Hint arrow** — Native yellow hint arrow at the target (plane-aware, won't show on wrong floor)
- **Hover tooltips** — Rich tooltip info when hovering highlighted NPCs and objects
- **Right-click menu** — "Collection Log Guide" option on right-clicking tracked NPCs
- **Item requirement sprites** — Required items shown with green (have) / red (missing) borders in the panel

### Step-by-Step Guidance

All 225 sources have multi-step guidance sequences with auto-completing steps:

- **Auto-arrival detection** — 223 sources detect when you arrive at the target location (ARRIVE_AT_TILE)
- **Auto-kill detection** — 112 sources detect when you kill the target (ACTOR_DEATH)
- **Multi-floor navigation** — 9 sources guide you through stair climbing with PLAYER_ON_PLANE detection
- **Zone-based detection** — ARRIVE_AT_ZONE for large areas (rectangular zone matching)
- **Bank routing** — 8 sources redirect you to a bank when required items are missing (mossy key, brittle key, spade, etc.)
- **Conditional branching** — Steps can dynamically select different paths based on quest/skill requirements
- **Step progress** — "Step 2/5: Kill Cerberus" displayed in the panel with InfoBox
- **Skip satisfied steps** — Steps whose conditions are already met are automatically skipped
- **Auto-advance** — When a sequence completes, guidance automatically activates for the next best source (configurable)
- **Dialog options** — 7 minigame sources highlight correct dialog choices (Barbarian Assault, Castle Wars, Pest Control, etc.)

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
| NPC Highlight Style | Hull | Choose hull, outline, or tile highlight for guided NPCs |
| Auto-Advance Guidance | On | Automatically start guidance for the next best source when a sequence completes |
| Show Overlays | On | Toggle all guidance overlays |
| Show Hint Arrow | On | Show the yellow hint arrow at the target |
| Shortest Path Integration | On | Request pathfinding from Shortest Path plugin |
| Overlay Color | Cyan | Customize the overlay highlight color |
| **Proximity** | | |
| Max Distance | 0 (unlimited) | Filter proximity mode results to within this tile distance |

## Data

All drop data lives in [`src/main/resources/com/collectionloghelper/drop_rates.json`](src/main/resources/com/collectionloghelper/drop_rates.json). 225 sources with 2,111 items covering all bosses, raids, slayer creatures, clue scrolls, minigames, shops, and skilling activities. Each source includes world coordinates, kill times (main + iron), drop table mechanics, quest/skill requirements, NPC IDs, multi-step guidance, and items with OSRS item IDs, decimal drop rates, and wiki links. Kill times are aligned with TempleOSRS EHB rates. Drop rates have been wiki-verified and cross-audited against the Log Hunters Log Adviser spreadsheet.

Slayer task weights for all 4 masters (Duradel, Nieve, Konar, Turael) are in [`slayer_task_weights.json`](src/main/resources/com/collectionloghelper/slayer_task_weights.json) — 148 tasks verified against OSRS Wiki.

## Building

```bash
./gradlew build
```

Requires Java 11+. Uses RuneLite client API (latest release), Lombok, and JUnit 4.

## Contributing

We're actively expanding toward Quest Helper-level guidance for every source. See:

- [**CONTRIBUTING.md**](CONTRIBUTING.md) — JSON schema reference, how to find data, PR process

## Acknowledgments

This plugin references patterns from [Quest Helper](https://github.com/Zoinkwiz/quest-helper) (hint arrows, overlays, step sequencing, conditional branching) and [Shortest Path](https://github.com/Skretzo/shortest-path) (inter-plugin pathfinding). Kill time and scoring data validated against the [Log Hunters](https://discord.gg/loghunters) community's Log Adviser spreadsheet. See [CREDITS.md](CREDITS.md) for full details.

## License

BSD 2-Clause
