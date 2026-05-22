# Changelog

## Unreleased

### Added

- **Source-level Recommended gear strip + wiki strategy link** — the
  guidance panel header now renders a chip strip for any source carrying
  a `recommendedGearItemIds` field, advisory-only (no bank-scan colors),
  and a "Strategy guide" link that opens the canonical wiki article in
  the system browser. The strip and link both render below the existing
  general requirements / required-items section so the established
  hierarchy (mandatory → recommended → strategy) stays visually
  consistent. Pilot data populates the field on a handful of high-impact
  bossing sources; broader backfill is a separate data pass. Closes
  [#573](../../issues/573) (PR [#584](../../pull/584)).
- **`drop_rates.json` audit harness** — new `scripts/audit_drop_rates.py`
  cross-checks every source's `worldX/worldY` / `npcId` / step
  descriptions / fairy-ring codes against the in-repo wiki snapshot and
  flags drift. Runs as part of the pre-hub data sweep tracked in
  [#495](../../issues/495); harness landed alongside the RAIDS pass
  ([#561](../../pull/561)) and was widened twice — once for large arenas
  ([#586](../../pull/586), [#563](../../issues/563)) and once to fix a
  longest-token bias that mismatched multi-city sources like "Edgeville
  / Lumbridge" ([#594](../../issues/594), [#595](../../pull/595)).

### Changed

- **`F1`/`F2` sync components now receive a shared `ScheduledExecutorService` via DI** —
  `CollectionLogNetSyncWorker` and `TempleOsrsSyncWorker` used to
  `Executors.newSingleThreadScheduledExecutor()` in their own
  constructors, which (a) leaked threads on plugin shutdown if a sync
  was in flight and (b) made the workers untestable without spawning
  real OS threads. They now receive the executor via `@Inject` from a
  shared sync module that owns shutdown ordering. End-users will notice
  cleaner client shutdown timing during a sync; the public sync UX is
  unchanged. Closes [#478](../../issues/478) /
  [#479](../../issues/479) (PR [#592](../../pull/592)).
- **`F1`/`F2` config labels standardized + tooltips added** — the two
  external-sync entries in the plugin config panel were inconsistently
  named ("Enable collectionlog.net" vs "TempleOSRS sync"); both now
  follow a `"Sync from <service>"` pattern and carry explanatory
  tooltips that name the data each sync pulls and the rate limit the
  client honors. Pure UX clean-up — no behavior change. Closes
  [#577](../../issues/577) (PR [#582](../../pull/582)).
- **Step-control buttons replaced with icon controls + tooltips** — the
  per-step Skip / Reset / Sync text buttons inside the guidance side
  panel were wide enough to push the panel wider than the standard
  RuneLite sidebar on some skins. Replaced with three icon buttons
  carrying tooltips that match the previous label text. The source-level
  Stop Guidance button now uses the same STOP icon as the step-control
  pause so the panel's two stop affordances no longer drift visually
  ([#576](../../issues/576) / PR [#581](../../pull/581)). Closes
  [#547](../../issues/547) (PR [#552](../../pull/552)).
- **`CONTRIBUTING.md` trimmed to a focused quickstart** — the deep
  guidance authoring bar, schema reference, audit playbook, and
  per-source category taxonomy moved into a new
  `docs/contributor-guide/` directory. `CONTRIBUTING.md` itself is now a
  one-screen quickstart that links into the deeper docs. Closes
  [#549](../../issues/549) (PR [#556](../../pull/556)).
- **23 travel-then-descend sources migrated to `ARRIVE_AT_ZONE`** —
  sources with a "travel to surface tile, then descend a ladder/stairs"
  pattern (Custodian Stalker [#548](../../issues/548) /
  [#553](../../pull/553) was the pilot;
  [#558](../../pull/558) batched the remaining 23) used to depend on
  `ARRIVE_AT_TILE` on the surface coordinate, which silently failed when
  the player teleported directly underground. They now use
  `ARRIVE_AT_ZONE` against the broader surface region, so the
  travel-step auto-advance fires reliably regardless of how the player
  reached the area. Closes [#555](../../issues/555).
- **Plugin Hub self-review doc refreshed against current master** —
  `docs/plugin-hub-review.md` re-audited; all previously-yellow items
  closed and the table is now 0 Red. Cited in the upcoming hub
  resubmission PR (PR [#593](../../pull/593)).

### Fixed

- **GWD boss guidance step order corrected + duplicate kill step
  dropped** — Armadyl, Bandos, Saradomin, and Zamorak GWD sources had
  inverted step ordering (kill step listed before the boss-room entry
  step) and a duplicate kill step at the end of each sequence. Both
  artefacts dated back to the original GWD data pass and were missed by
  the Pass 1 audit harness, which only checked coord / npcId drift and
  not step-order coherence. Closes [#574](../../issues/574)
  (PR [#579](../../pull/579)); harness gap captured for a follow-up
  audit class.
- **Plugin no longer fails to instantiate in a live RuneLite client** —
  Guice could not resolve four C-tier player-state interfaces
  (`DiaryTierState`, `EquippedItemState`, `PohTeleportInventory`,
  `SkillCapePerkState`) because their `@Singleton` impls were enough to
  JIT-bind the concrete class but nothing told Guice how to satisfy the
  interface dependency injected into `PlayerCapabilityDebugOverlay`. The
  failure was latent since [#459](../../pull/459) / [#461](../../pull/461)
  and only surfaced on `./gradlew run` — unit tests passed because they
  bind the impls directly. Fix adds `@ImplementedBy` to each interface
  (smallest possible diff, no impl-body changes). New regression test
  `CollectionLogHelperPluginInjectorTest` resolves each interface against
  a minimal injector and fails at CI time if a future change drops the
  annotation or adds another unbound interface dep. Closes
  [#543](../../pull/543).

- **Long step descriptions no longer overflow the guidance panel** —
  `StepProgressView.showStep` now wraps the step text in
  `<html><body style='width:220px'>…</body></html>` so Swing word-wraps
  long step descriptions instead of stretching the JLabel's preferred
  width past the panel's clip region. Most visible on clue tiers where
  step strings like "Buy a sleek hairband from the Falador hairdresser"
  pushed content outside the sidebar box. Closes
  [#483](../../issues/483) (rendering half; the auto-advance half is
  covered by [#485](../../issues/485)).

- **Kraken and Cave Kraken step descriptions corrected to point at Piscatoris** —
  both sources had stale step descriptions referencing the "Fremennik Slayer
  Dungeon" and a non-existent slayer-ring teleport to Kraken Cove. The
  `worldX/worldY`, `npcId`, `locationDescription`, and source-level `travelTip`
  fields were already correct (Piscatoris Fishing Colony, fairy ring `AKQ`); only
  the per-step `description` (and step-level `travelTip` on the boss source) was
  misleading. Descriptions now correctly state Kraken Cove is south-west of the
  Piscatoris Fishing Colony, per the wiki. Closes
  [#494](../../issues/494). Surfaced during the [#484](../../issues/484)
  fairy-ring audit and rolled forward as part of the broader pre-hub data
  pass tracked in [#495](../../issues/495).

- **ARRIVE_AT_TILE auto-advance in instanced regions** — `resolvePlayerWorldLocation`
  now translates the player's local point back to overworld template coordinates
  via `WorldPoint.fromLocalInstance` when the player is inside an instanced
  region (Royal Titans, raids, GWD, Vorkath, many quest/clue rooms). Without this,
  the player's reported coords inside an instance never matched the static
  `worldX/worldY` in `drop_rates.json`, so step auto-advance silently failed for
  every guidance sequence that crossed into an instance. Closes
  [#485](../../issues/485).

- **Fairy-ring code corrected to `AIQ` for the Asgarnian Ice Dungeon
  travel step** — the Cave Krakens / Krakens travel step was emitting
  `AKQ`, which lands at the Piscatoris Fishing Colony, not the
  Asgarnian Ice Dungeon. Player-facing step text and the underlying
  `travelTip` now both reference `AIQ`. Closes
  [#484](../../issues/484) (PR [#493](../../pull/493)).

- **Step descriptions wrap to the panel width** — long single-string
  descriptions used to render mid-word truncated on narrow side panels
  because the rendering path skipped the `<html>` wrapper added in
  [#483](../../issues/483). The wrap now applies uniformly to every
  step-rendering surface. Closes [#575](../../issues/575) (PR
  [#580](../../pull/580)).

- **`KillTimeTracker` disk I/O moved off the client thread + attribution
  filtered to the local player** — the per-account kill-time learning
  feature (`F3`) wrote to disk synchronously on every `ACTOR_DEATH`
  observed in the scene, which (a) blocked the client tick under slow
  disk and (b) attributed kills from nearby other players to the local
  account. I/O now hops to the shared sync executor and attribution is
  gated to `client.getLocalPlayer()`. Closes [#480](../../issues/480) /
  [#481](../../issues/481) (PR [#589](../../pull/589)).

- **`C7` debug overlay surfaces Tier C detection state + accurate quest
  count** — the player-capability debug overlay shipped in
  [#459](../../pull/459) showed each tier's bound impl but never
  reflected whether that impl had detected current player state (e.g.
  diary tier, equipped slot, POH teleport availability). The overlay
  now renders detected state per tier and includes a corrected
  partial-quest count (the previous string reflected total quests, not
  partial-progress entries). Closes [#486](../../issues/486) /
  [#487](../../issues/487) (PR [#583](../../pull/583)).

- **Side-panel sync button visibility refreshes on config toggle** —
  toggling `F1` / `F2` sync from the plugin config used to require a
  panel rebuild for the sync button row to appear or disappear. The
  panel now subscribes to the relevant config keys and refreshes the
  sync button strip on the next tick. PR
  [#492](../../pull/492).

- **`SLAYER` and `SKILLING` category counts aggregate from the source
  database** — both categories rendered "0/N" in the panel header
  because the count aggregator only read the top-level category keys
  and missed sub-categorized sources. Counts now sum from a
  source-database walk so the header reflects actual completion.
  Closes [#545](../../issues/545) (PR [#546](../../pull/546)).

- **23 data corrections from the Pass 1 audit sweep** — the audit
  harness from [#561](../../pull/561) was applied across CLUES
  (PR [#562](../../pull/562), 12 sources clean), BOSSES
  (PR [#564](../../pull/564), 61 sources triaged, 1 research issue
  filed), SLAYER (PR [#567](../../pull/567), 45 sources, 2 surface→
  underground coord fixes + 2 research issues), MINIGAMES
  (PR [#568](../../pull/568), 25 sources, 4 wording fixes), SKILLING
  (PR [#570](../../pull/570), 17 sources, 1 surface→underground fix),
  OTHER (PR [#571](../../pull/571), 58 sources, 2 mismatches fixed),
  RAIDS (PR [#561](../../pull/561), npc/coord fixes + ToB-HM cleanup in
  PR [#585](../../pull/585)), and a follow-up Slayer pass for Frost
  Nagua coord-vs-description drift + Lava Strykewyrm Charred Island
  move (PRs [#587](../../pull/587), closes
  [#565](../../issues/565) / [#566](../../issues/566)). Pass 1 closed
  as part of [#495](../../issues/495).

### Changed (refactors)

- **`CollectionLogHelperPlugin` decomposed into routers, orchestrators,
  and modules (#503)** — the plugin class dropped from 904 LOC to 555
  LOC through a 25+ PR campaign. Event handling now flows through
  dedicated routers (`GameStateRouter`,
  `VarbitChangeRouter`, `SequencerEventAdapter`,
  `ChatEventHandler`); per-tick work runs in `GameTickOrchestrator`;
  shutdown sequencing lives in `PluginShutdownRoutine`; and Guice
  bindings are split across `DataModule`, `EfficiencyModule`,
  `GuidanceModule`, `SyncModule`
  (PRs [#509](../../pull/509), [#514](../../pull/514),
  [#518](../../pull/518), [#521](../../pull/521)). The panel was
  similarly split — `PanelRebuildOrchestrator`,
  `PanelHeaderBuilder`, `SelectorControlsPanel`,
  `SyncButtonController`, `ListContainerScrollPane`, and
  `DetailViewBuilder` are now stand-alone collaborators
  (PRs [#513](../../pull/513), [#515](../../pull/515),
  [#519](../../pull/519), [#522](../../pull/522),
  [#531](../../pull/531), [#534](../../pull/534)). The guidance
  coordinator and sequencer shed `WorldMapController`,
  `NpcTrackerHelper`, `DynamicTargetManager`,
  `DynamicItemObjectTierResolver`, `StepChangeHandler`,
  `OverlayStepApplier`, `OverlayDeactivator`, `OverlaySourceApplier`,
  `StepAdvancer`, and `CompletionChecker`
  (PRs [#510](../../pull/510), [#512](../../pull/512),
  [#516](../../pull/516), [#520](../../pull/520),
  [#523](../../pull/523), [#526](../../pull/526),
  [#529](../../pull/529), [#532](../../pull/532),
  [#535](../../pull/535), [#537](../../pull/537)). Closes
  [#503](../../issues/503) (umbrella PR [#542](../../pull/542)).
  No user-visible behavior change; the result is a substantially
  smaller core class and far better test isolation.

- **`guidance.helper` package renamed to `guidance.bosses`** to match
  the actual contents (boss-specific `GuidanceHelper` implementations,
  e.g. `CerberusHelper`). Closes [#502](../../issues/502) (PR
  [#508](../../pull/508)).

### Docs

- **Hub-readiness state reconciled to current master** — `docs/ROADMAP.md`,
  `docs/plugin-hub-review.md`, and `docs/plugin-hub-resubmission.md`
  updated to reflect the post-Pass-2 state of the codebase: B4.3.3
  (Cyclopes Defenders per-item descriptions, PR [#444](../../pull/444))
  flipped from in-progress to done; B.5.5 (per-step world-map arrow +
  destination icon, [#430](../../issues/430)) flipped from partial to
  done after the CLH-distinct orange `#FF8C00` arrow colour landed via
  PR [#583](../../pull/583); A6 status note rewritten to record that
  every 2026-05-16 validation issue (#483–#488) is now closed and the
  audit umbrella [#495](../../issues/495) is ready to close; LOC table
  re-measured against current master (Plugin 488, Panel 605,
  Coordinator 557, Sequencer 623, EfficiencyCalculator 777 — all five
  under the 800 floor); earliest hub-resubmission push date pushed
  from 2026-05-22 to 2026-05-28 to reflect today's documentation,
  test-infra, and Pass-2 cleanup merges.

### Changed (build hygiene)

- **Allocation-free guard-fail paths in `DynamicTargetManager` +
  hoisted `exportEfficiencyIfEnabled` field** — the per-tick guard-fail
  path in `DynamicTargetManager` allocated a fresh `Optional` even when
  no dynamic target was active (PR [#539](../../pull/539),
  [#527](../../issues/527)), and `Plugin.exportEfficiencyIfEnabled`
  resolved its config flag on every tick instead of caching it as a
  field (PR [#554](../../pull/554), follow-up to
  [#541](../../issues/541)). Both paths now run literal-zero
  allocations on the steady-state tick.

- **Non-ASCII guard on `drop_rates.json`** — the build now fails on
  non-ASCII characters in `drop_rates.json` (em-dashes were the
  recurring offender, sourced from Wiki copy-paste). Existing
  em-dashes were replaced with ASCII equivalents in the same PR.
  Closes [#501](../../issues/501) (PR [#507](../../pull/507)).

- **Plugin metadata polish for hub resubmission** — `icon.png`
  re-exported at the 48x72 convention the hub expects
  (PR [#505](../../pull/505) / [#499](../../issues/499)) and plugin
  description + tags refined to align with the hub's manifest schema
  (PR [#506](../../pull/506) / [#500](../../issues/500)).

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
