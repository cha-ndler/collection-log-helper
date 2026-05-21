# Pass 2 In-Game Validation Results — 2026-05-20

> **Scope**: Issue #495 Pass 2 (Phase 2 dispatch paths + Phase 3/4/5 UI surfaces + Phase 6 startup smoke). Pass 1 (data audit) closed via PRs #561, #562, #564, #567, #568, #570, #571.

> **Status legend** (mirrors `docs/in-game-validation-log.md`):
> - `[ ]` not tested
> - `[x]` verified pass
> - `[!]` regression — issue filed
> - `[-]` N/A or blocked-by-design
> - `[?]` inconclusive / skipped this session, carries over

## Session summary

| | |
|---|---|
| Master tested | `15c4ce3d` (after PR #572 hot-fix) |
| Client | dev RuneLite via `./gradlew run`, JDK 17 (`--add-opens` from #475 holding) |
| Account | active player with all diary tiers + 179 quests + POH teleports + Slayer task (Gryphons x38) |
| Test source | General Graardor (covers B2 + B.5.2 + B.5.3 + B.5.4 in one activation) |
| Rubric rows exercised | ~14 of ~50 in scope |
| Regressions filed | 5 new (#573, #574, #575, #576, #577) + reopen requests on 2 prior (#486, #487) |
| Pass 2 closure | partial — Phase 2 B2 + Phase 6 closing smoke + a few small rows remain for a future session |

## Phase 6 — Build / CI / Infra (startup smoke)

| # | Test | Status | Notes |
|---|---|---|---|
| #475 | `./gradlew run` succeeds on JDK 17 without InaccessibleObjectException | `[x]` | confirmed twice this session (He + Vav launches both clean) |
| login | Login screen reaches OSRS (no js5connect_outofdate) | `[x]` | |
| plugin-load | Plugin loads; "Collection Log Helper" sidebar icon appears | `[x]` | |
| panel-modes | Switch through all panel modes (Efficient / Category Focus / Search / Pet Hunt / Statistics / Dry Streaks) without errors | `[x]` | All render. Pre-existing #546 Category > Slayer/Skilling 0/0 fixed; no regression observed. |

## Phase 2 — Dispatch paths

### B2 waypoints (PR #454)

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with `waypoints` step → step completes only after last waypoint | `[?]` | Skipped this session — would need to physically travel to GWD entrance. 11 sources have waypoints; defer to a future session where the player is at one of them. |
| 2 | Walk across each waypoint in order → step completes only after the last waypoint | `[?]` | |
| 3 | Teleport past a waypoint (skipping it) → step does not auto-complete | `[?]` | |

### B3 nested conditional steps (PR #455)

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with nested `conditionalAlternatives` | `[-]` | **N/A** — zero sources have `conditionalAlternatives` field backfilled in `drop_rates.json`. Rubric self-marks N/A in this case. |
| 2 | Player state matches nested branch → step text/target reflects the override | `[-]` | N/A — same as above |
| 3 | Player state changes mid-step → branch re-evaluates without restart | `[-]` | N/A — same as above |

## Phase 3 — Player-aware state (C7 debug overlay)

### PR #459 — C7 player-capability debug overlay

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Enable Show Player Capability Debug Overlay → overlay renders | `[x]` | Overlay renders on right side of screen. |
| 2 | Overlay shows: equipped count, diary tiers per region, POH teleports, skill-cape perks, partial-quest state | `[!]` | **Still incomplete** despite #486 close. Basic stats block (combat/skills/spellbook/prayer/task/POH-yes-no) renders correctly. C1 shows "(none detected)" but player has POH teleports built. C2 shows "Items equipped: 0" but player is equipped. C3 renders region labels (Ardougne, Desert, Falador, Fremennik, Kandarin) but no completion-tier values. Re-open request added on #486. |
| 3 | Toggle off → overlay disappears cleanly | `[?]` | not tested |

### PR #461 — C3 diary tier state · PR #462 — C5 partial-quest state · PR #463 — C2 equipped-item state · PR #470 — C1 POH teleport inventory · PR #471 — C4 skill-cape perk state

All blocked on the C7 surface not exposing data. See #486 re-open comment. Sub-rows remain `[!]`. #487 (quest count 207 vs 179) re-confirmed still broken with label rename "Quests done" → "Quest entries" but same wrong count.

## Phase 4 — External I/O

### PR #465 — F1 collectionlog.net profile import · PR #468 — F2 TempleOSRS KC sync

| # | Test | Status | Notes |
|---|---|---|---|
| F1/F2 row 1 | Panel surface for Import / Sync | `[/]` | Still config checkbox only (no panel button). Per player preference + #488 close, this is the correct UX (side panel reserved for guidance). Filed #577 for polish: naming consistency + per-item tooltips explaining purpose. |
| F1/F2 row 2+ | Invoke import/sync with valid/invalid RSN | `[?]` | not exercised this session |

### PR #466 — F3 per-account kill-time learning · PR #469 — F4 dry-streak feed

| # | Test | Status | Notes |
|---|---|---|---|
| F3 row 1 | Enable Learn Kill Times in config | `[x]` (already verified 2026-05-16, no regression) | |
| F4 row 1 | Panel includes Dry Streak mode/tab | `[x]` (already verified 2026-05-16, no regression) | |
| F4 row 2 | Renders without NPE | `[x]` (already verified 2026-05-16, no regression) | |
| other F3/F4 rows | not exercised this session | `[?]` | depends on F2 sync (#577) being invoked |

## Phase 5 — UI / panel surface

### PR #457 — B.5.3 source-level requirements header

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Requirements header renders with green/red coloring | `[x]` | Graardor: "Troll Stronghold — COMPLETED" + "Strength 70 — level 99/70" both green (player has both). |
| 2 | Unmet renders red; met renders green | `[x]` | Both met for player; green confirmed. Red path not exercised this session (would need a source with an unmet requirement). |

### PR #458 — B.5.4 collapsible step sections

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Section headers visible | `[x]` | Graardor: "Travel (1 step)" + "Combat (3 steps)" headers. |
| 2 | Active step's section auto-expanded; non-active collapsed | `[x]` | Per player visual confirmation. |
| 3 | Click section header → expand/collapse | `[x]` | Chevron animates. |

### PR #472 — B.5.2 recommended items section

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Recommended chip strip renders below required strip | `[x]` | Graardor: 3 chips (Saradomin brew, Prayer potion, Bandos godsword). |
| 2 | Color rules match required strip (green/white/red) | `[x]` | Saradomin brew yellow/bank, Prayer potion green/inv, Bandos godsword white/bank-found. |
| 3 | Hover → tooltip shows item name | `[?]` | not exercised — defer. |

> **B.5.2 product feedback**: player suggested elevating Recommended above the step body and adding a per-source wiki-strategy link. Filed as #573 (enhancement, not regression).

### PR #464 — E1 cross-source per-item recommendation

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Enable cross-source mode flag in config | `[-]` | **Blocked by design** — `enableCrossSourceMode` is `hidden = true` in config until panel integration ships. Cannot toggle from UI. |
| 2 | No regression to existing panel modes | `[x]` | All modes render (covered by Phase 6 panel-modes row). |
| 3 | Debug log shows `CrossSourceRanker` running on recalc | `[?]` | not exercised — flag is hidden, can't enable. |

### PR #467 — E2 meta-update dating

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Source with `metaUpdateDate` set → `MetaAgeBadge` shows age | `[-]` | **N/A** — zero sources have `metaUpdateDate` field. |
| 2 | Source without `metaUpdateDate` → badge hidden, no NPE | `[-]` | N/A — vacuously satisfied. |

## Regressions filed (5 new + 2 prior re-opens)

### Plugin Hub blockers (tier-A)

| # | Title | Status |
|---|---|---|
| **#574** | data: General Graardor + 3 sibling GWD bosses guidance steps out of order (kill before KC gate, duplicate kill step) | filed, fix model = Nex's correct ordering |
| **#575** | ui(panel): step description text truncated in side panel (in-game overlay renders correctly) | filed, suspected `StepProgressView` / `CategorySummaryPanel` layout issue |
| **#576** | ui(guidance): step-control STOP button doesn't sync state with source-level Stop Guidance button | filed, suspected missing event dispatch from #547 icon-controls extraction |
| **#486** (re-open req) | C7: C1/C2/C3 still incomplete despite prior close | re-open comment added |
| **#487** (re-confirm) | C5: quest count diverges (207 vs 179) — label rename without fix | comment added confirming still broken |

### Non-blocking (P2 enhancement)

| # | Title | Status |
|---|---|---|
| **#573** | feat(panel): elevate Recommended gear above step body + link to wiki strategy per source | filed, player-driven UX suggestion |
| **#577** | ui(config): F1/F2 sync config UX — naming consistency + per-item tooltips | filed, complements #488 close |

## What carries over to a future validation session

- **Phase 2 B2 (3 rows)**: requires player physically at GWD entrance or another waypoint-backfilled source (Kraken, Theatre of Blood, Forestry, etc.) — pick one in a session where the player is nearby
- **Phase 4 F1/F2 invocation (4 rows)**: requires invoking the sync paths — would need a known-good RSN + an invalid RSN
- **Phase 5 B.5.2 row 3**: hover tooltip — quick visual check next time the panel is open
- **Phase 5 E1 rows 1+3**: blocked until `enableCrossSourceMode` ships visibly in config (after panel integration ships)
- **C7 rows for C1-C5**: blocked by #486 re-open; will retest once #486 closes again
- **Various other Phase 3/4 rows in the original validation log** that weren't exercised either time (2026-05-16 or 2026-05-20)

## Recommendation for #495 closure

**Do not close #495 yet.** Pass 1 is complete and Pass 2 is partially done. Five regressions surfaced in Pass 2 are unrelated to data-audit work and need code fixes. Close #495 after:

1. Tier-A regressions resolved (#574, #575, #576, #486 re-open, #487)
2. Remaining `[?]` rubric rows above either resolved or explicitly deferred to a separate post-hub validation issue
