# Changelog

## Unreleased

## 0.1.0 — Initial public release

### Added

- **Efficiency scoring** — ranks all 225 collection log sources by expected new slots per
  kill using combined-rate math (`1 - Π(1 - pᵢ)`), independent drop tables, sequential
  prerequisites, and raid team-size scaling
- **Guidance overlays** — tile highlight, NPC/object/widget/item/ground-item highlighting,
  world-map route line, minimap arrow, hint arrow, and hover tooltips for every source
- **Step-by-step guidance sequences** — multi-step navigation with auto-arrival detection
  (ARRIVE_AT_TILE, ARRIVE_AT_ZONE), auto-kill detection (ACTOR_DEATH), multi-floor
  navigation (PLAYER_ON_PLANE), conditional branching, and auto-advance to the next source
- **Bank routing** — redirects to a bank when required key items are missing (mossy key,
  brittle key, spade, etc.) for 8 sources
- **Five display modes** — Efficient, Category Focus, Search, Pet Hunt, Statistics
- **Account-aware requirements** — quest completions and skill levels (including Sailing)
  gate locked content; hovering shows formatted unmet requirements
- **Smart travel routing** — detects fairy rings, spirit trees, spellbook, bank items, and
  diary completions; integrates with Shortest Path plugin if installed
- **Slayer overhead modeling** — task-only sources inflate effective kill time by
  `1 / P(creature | master)` using verified task weights for Duradel, Nieve, Konar, Turael
- **Main / Ironman toggle** — separate kill-time conventions for content with different
  methods (Corp, GWD, wilderness bosses, etc.)
- **Raid difficulty variants** — CoX/CoX CM, ToB/ToB HM, ToA 150/300/500 cross-referenced
  with "(also: ...)" notes
- **Auto-sync** — detects all obtained items when the in-game Collection Log is opened;
  configurable login reminder
- **Live tracking** — detects new entries via chat messages and varbit changes; recalculates
  efficiency rankings in real time
- **Data coverage** — 2,109 items across 225 sources; drop rates wiki-verified and
  cross-audited against the Log Hunters community spreadsheet; 148 slayer task weights
