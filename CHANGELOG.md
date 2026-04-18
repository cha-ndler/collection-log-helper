# Changelog

## Unreleased

## 1.0.0-hub — 2026-04-17

### Changed

- **Panel decomposition (A1 / A1b)** — `CollectionLogHelperPanel` refactored from a 1,661-LOC
  god-object into a thin shell (698 LOC) delegating to five per-mode controllers
  (`EfficientModeController`, `CategoryModeController`, `SearchModeController`,
  `PetHuntModeController`, `StatisticsModeController`) and six shared view widgets
  (`SyncStatusView`, `ClueSummaryView`, `GuidanceBannerView`, `StepProgressView`,
  `SlayerStrategyView`, `QuickGuidePanelView`).  PRs [#349](../../pull/349) and [#351](../../pull/351).
- **Plugin class slim-down (A2)** — `CollectionLogHelperPlugin` reduced from 2,192 LOC to 904 LOC
  (-59%) by extracting seven dedicated service classes: `OverlayRegistry`, `SceneEventRouter`,
  `AuthoringLogger`, `SyncStateCoordinator`, `GuidanceUIState`, `GuidanceOverlayCoordinator`,
  `GuidanceEventRouter`.  PRs [#331](../../pull/331)–[#339](../../pull/339), [#347](../../pull/347).

### Added

- **Data-sourcing infrastructure (A5)** — `verified_scene_ids.json` registry maps cache IDs to
  scene IDs for known divergences; MCP tools `varbit_lookup`, `cache_diff_check`,
  `authoring_playbook`, and `data_source_router` (runelite-dev-toolkit v0.2.0) make in-game
  authoring runs a last resort.  PRs [#341](../../pull/341) and [#342](../../pull/342).
- **Plugin Hub self-review doc** — `docs/plugin-hub-review.md` audits all submission criteria
  (27 green / 3 yellow / 2 red), with fix milestones for each open item.
  PR [#346](../../pull/346).

### Fixed

- **`StepProgressView.hide()` shadowed deprecated `Component.hide()`** — renamed to `hideStep()`
  so Swing's internal visibility dispatch routes correctly.  PR [#353](../../pull/353) /
  [#354](../../pull/354).

---

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
