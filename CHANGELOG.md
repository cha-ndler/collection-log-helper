# Changelog

## Unreleased

### Fixed

- **C7 overlay quest-count row label disambiguated** — renamed `"Quests done"`
  to `"Quests + miniquests"` and added a dim explanatory sub-row
  `"(RuneLite enum count)"`. The previous label invited confusion when the
  number exceeded the player-visible in-game Quest List count (e.g. 207 vs
  179/179) because the RuneLite Quest enum includes miniquests + RFD
  sub-entries. Closes [#487](../../issues/487).

## 1.0.0-hub — 2026-05-15

### Added

- **Composable guidance schema** — tile-sequence pathing (B2, [#456](../../pull/456)),
  nested conditional steps (B3, [#455](../../pull/455)), and puzzle/dynamic step type
  with `DynamicTargetEvaluator` + Wintertodt brazier pilot (B5, [#473](../../pull/473)).
- **Hybrid Java helper pilot** — `GuidanceHelper` interface + registry routing in the
  sequencer; Cerberus pilot (B6, [#474](../../pull/474)). Closes D-01.
- **Per-step UI parity with Quest Helper** — required-items chips with bank-scan colors
  (B.5.1, [#452](../../pull/452)), advisory recommended-items section
  (B.5.2, [#472](../../pull/472)), source-level requirements header
  (B.5.3, [#457](../../pull/457)), collapsible step sections
  (B.5.4, [#458](../../pull/458)), dialog highlight polish
  (B.5.7, [#449](../../pull/449)).
- **Player-aware state model (Tier C)** — POH teleport inventory
  (C1, [#470](../../pull/470)), equipped-item state (C2, [#463](../../pull/463)),
  diary tier state (C3, [#461](../../pull/461)), skill-cape perk state
  (C4, [#471](../../pull/471)), partial-quest state (C5, [#462](../../pull/462)),
  and a dev-facing player-capability debug overlay (C7, [#459](../../pull/459)).
- **Cross-source recommendation mode** — `CrossSourceRanker` +
  `CrossSourceRecommendation` + `enableCrossSourceMode` config flag; panel integration
  deferred (E1, [#464](../../pull/464)).
- **Meta-update dating** — recommendations carry meta-update timestamps so stale
  guidance is visible at a glance (E2, [#467](../../pull/467)).
- **External profile sync** — collectionlog.net import (F1, [#465](../../pull/465))
  and TempleOSRS KC sync (F2, [#468](../../pull/468)).
- **Per-account kill-time learning (opt-in)** — `KillTimeTracker` + on-disk
  personalized estimates per character with a config flag and 19 tests
  (F3, [#466](../../pull/466)).
- **Dry-streak feed** — surfaces the player's longest dry streaks per source
  (F4, [#469](../../pull/469)).
- **CI + coverage gate** — JaCoCo report + verification at 45% (F5, [#454](../../pull/454))
  and GitHub Actions build+test workflow on PR/push (F6, [#451](../../pull/451)).

### Fixed

- **JDK 17 startup** — `gradlew run` no longer throws `InaccessibleObjectException` in
  RuneLite's `ReflectUtil.invalidateAnnotationCaches` on JDK 17+; added five
  `--add-opens` flags to the run task ([#475](../../pull/475)).
- **Duplicate tier label on search hover** — search-result rows no longer show two
  tier labels stacked ([#446](../../pull/446)).
- **Tier 3 auto-completion backfill** — two sources gained
  `CHAT_MESSAGE_RECEIVED`-based completion detection ([#448](../../pull/448),
  [#306](../../issues/306)).

### Notes

- **Sailing island-instance vs port coords** — triaged ([#450](../../pull/450),
  [#314](../../issues/314)); follow-up data PRs deferred until Sailing mechanics
  stabilize.
- **D1 deep-guidance bar** — closed retroactively ([#447](../../pull/447)) — the
  10-element authoring checklist was already shipped in [#358](../../pull/358).
- **B.5.5 worldmap arrow** — 5/6 sub-requirements confirmed shipped via prior PRs
  ([#453](../../pull/453)); blocked on a pending color constant PR.
- **In-game validation log** — `docs/in-game-validation-log.md` records the
  2026-05-14 PR-batch validation pass ([#460](../../pull/460)).

## 1.0.0-hub — 2026-05-12

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
- **RuneLite client bumped to 1.12.26.3** (from 1.12.24).  PR [#379](../../pull/379).
- **Required-item availability surfaced inline** — the guidance panel now shows whether each
  `requiredItemIds` entry is held in inventory / equipment / bank, replacing the older
  bank-step routing.  PR [#384](../../pull/384).
- **Clue per-casket time labeled "Per Clue:"** — for clue sources the secondary time row
  (player-progression bucket) was previously also labeled "Est. Time:", making the two
  values ("total" vs "per casket") visually indistinguishable.  PR [#394](../../pull/394)
  ([#368](../../issues/368)).

### Added

- **Data-sourcing infrastructure (A5)** — `verified_scene_ids.json` registry maps cache IDs to
  scene IDs for known divergences; MCP tools `varbit_lookup`, `cache_diff_check`,
  `authoring_playbook`, and `data_source_router` (runelite-dev-toolkit v0.2.0) make in-game
  authoring runs a last resort.  PRs [#341](../../pull/341) and [#342](../../pull/342).
- **Plugin Hub self-review doc** — `docs/plugin-hub-review.md` audits all submission criteria
  (27 green / 3 yellow / 2 red), with fix milestones for each open item.
  PR [#346](../../pull/346).
- **Deep-guidance authoring bar (D1)** — `CONTRIBUTING.md` documents the 10-element
  checklist required for any new guidance source.  PR [#358](../../pull/358).
- **GitHub Actions CI** — `build` and `test` jobs run on every PR against `master`.
  PR [#361](../../pull/361).
- **In-game validation log** — `docs/in-game-validation-2026-05-10.md` captures the manual
  test pass that surfaced the v1-blocker bugs closed by PRs #384, #386–#394.
  PR [#385](../../pull/385).

### Fixed

- **`StepProgressView.hide()` shadowed deprecated `Component.hide()`** — renamed to `hideStep()`
  so Swing's internal visibility dispatch routes correctly.  PR [#353](../../pull/353) /
  [#354](../../pull/354).
- **Shades of Mort'ton guidance** — bank-before-travel step ordering, wider
  `ARRIVE_AT_TILE` completion distance for teleport-scroll landings, shade-key pickup tile
  re-anchored inside the temple, and the InfoBox/blue-box render correctly when activation
  skips into a mid-sequence step.  PR [#372](../../pull/372)
  ([#366](../../issues/366), [#367](../../issues/367), [#375](../../issues/375), [#376](../../issues/376)).
- **Step-advance and skip callbacks routed through the client thread** — overlays now
  refresh on the same game frame instead of one tick later.  Part of PR [#372](../../pull/372)
  ([#367](../../issues/367)).
- **`Show Overlays` / `Show Hint Arrow` toggles propagate immediately** — flipping either
  setting while guidance is active updates overlay visibility on the next tick instead of
  requiring a restart.  PR [#386](../../pull/386) ([#363](../../issues/363)).
- **Filter-affecting config changes rebuild the panel** — `Hide Obtained`, `Hide Locked`,
  `Account Type`, `Raid Team Size`, `AFK Filter`, `Efficient Sort`, sync/bank-scan reminders,
  and `Default Mode` toggles take effect immediately.  PR [#387](../../pull/387)
  ([#364](../../issues/364), [#365](../../issues/365)).
- **Required-item name resolution happens on the client thread** — eliminates rare
  cross-thread access exceptions during guidance-step item lookup.  PR [#389](../../pull/389).
- **`Show Overlays` toggle no longer aborts the active guidance session** — disabling
  overlays clears their visuals without tearing down sequencer state, InfoBox, or panel
  step display.  PR [#390](../../pull/390) ([#373](../../issues/373)).
- **StepProgressView tooltip lookup safe against missing entries** — a single bad tooltip
  no longer aborts the entire step strip.  PR [#391](../../pull/391).
- **Locked items rank by efficiency score** — locked content is no longer segregated to the
  bottom of the Efficient list; the lock indicator remains visual-only.  PR [#392](../../pull/392)
  ([#374](../../issues/374)).
- **Hint arrow refreshes after long-distance teleport mid-guidance** — `client.setHintArrow`
  and similar one-shot APIs are re-applied when a per-tick Chebyshev delta > 10 tiles is
  detected, so the arrow no longer points at the prior scene origin.  PR [#393](../../pull/393)
  ([#381](../../issues/381)).

### Notes

- The `v1.0.0-hub` git tag is being re-cut on the post-fix `master` HEAD.  The
  locally-drafted tag from 2026-04-17 (commit b9518653) is stale and will be replaced
  before the Plugin Hub PR is opened.  See `docs/plugin-hub-resubmission.md` for the
  quiet-week schedule and push procedure.

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
