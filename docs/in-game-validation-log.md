# In-Game Validation Log

> **Purpose**: Track manual in-game testing for merged PRs. Unit tests, type checks, and CI only prove correctness of pure logic — anything touching player state, overlay rendering, or live game events needs human verification against the OSRS client. This file is the source of truth for "did the fix actually work for real users?"

> **Status legend**:
> - `[ ]` — Not tested yet
> - `[~]` — Testing in progress
> - `[x]` — Verified pass
> - `[!]` — Regression / failed (file a new issue and cross-link)
> - `[?]` — Inconclusive (couldn't reproduce / state-dependent)

> **Last updated**: 2026-05-12

---

## How to use this file

1. Pull / `git checkout master` and `git pull` so you're on the merged state.
2. `./gradlew run` launches dev-mode RuneLite with the plugin sideloaded.
3. Work through the checklist sections in any order; mark each item with the status above.
4. Add a short note (or screenshot path) whenever something is off.
5. If a regression is found, file a GitHub issue and link it from the note column.
6. When every row in a PR section is `[x]`, append the validation date to the section header and consider the work closed.

---

## PR #379 — RuneLite client bump 1.12.24 -> 1.12.26.3 *(merged 2026-05-10)*

Critical because `error_game_js5connect_outofdate` blocked all prior in-game testing.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | `./gradlew run` launches without build/run errors | `[ ]` | |
| 2 | Login screen reaches OSRS (no `error_game_js5connect_outofdate`) | `[ ]` | |
| 3 | Plugin loads; "Collection Log Helper" sidebar icon appears | `[ ]` | |
| 4 | No new console stack traces during 5 minutes of normal play | `[ ]` | |

---

## PR #372 — Shades of Mort'ton guidance fixes *(merged 2026-05-11)*

Closes #366 (step ordering + arrival distance), #367 (skip overlay refresh), #375 (mid-sequence panel render), #376 (shade-key tile).

> NOTE: PR #372 set Mort'ton step 1's description to "Bank: bring pyre logs, shade remains, and a tinderbox to Mort'ton". After PR #384 merged, the description is technically misleading (there's no bank step anymore). If item-requirements UX is confusing in testing, the description can be edited in a small follow-up to "Travel to Mort'ton (bring pyre logs, shade remains, and a tinderbox)" without touching code.

| # | Issue | Test | Status | Notes |
|---|---|---|---|---|
| 1 | #366-B | Mort'ton teleport scroll -> step 1 (travel) auto-completes within ~1 second of landing | `[ ]` | |
| 2 | #366-B | Minigame teleport -> Shades of Mort'ton -> step 1 auto-completes on landing | `[ ]` | |
| 3 | #366-B | Fairy ring BKR -> step 1 auto-completes on landing | `[ ]` | |
| 4 | #366-B | Barrows teleport, run south to Mort'ton -> step 1 auto-completes near town centre, NOT at the river | `[ ]` | |
| 5 | #367 | Mid-sequence click **Skip** -> object highlight, tile marker, and panel all update in the same tick (no visible 1-tick gap) | `[ ]` | |
| 6 | #367 | Mid-sequence click **Next Step** -> same behavior as Skip | `[ ]` | |
| 7 | #367 | Skip rapidly across 3 steps -> no orphaned overlays from prior steps | `[ ]` | |
| 8 | #375 | Mid-sequence landing: stand at funeral pyres with a shade key + pyre logs + remains, activate guidance -> blue box shows landed step 3/5, InfoBox reads **3/5** not **1/5** | `[ ]` | |
| 9 | #375 | All-satisfied: pre-meet every condition for the source, activate guidance -> deactivates cleanly, NO empty "Step 1/0:" box | `[ ]` | |
| 10 | #375 | Plain start: fresh inv at Lumbridge, activate Shades of Mort'ton -> blue box shows step 1/N (regression check for the common path) | `[ ]` | |
| 11 | #376 | While on step 3/5 -> tile marker is INSIDE Mort'ton temple at ~(3495, 3298) near reward pillar, not on surface near the pyre | `[ ]` | |

---

## PR #384 — Surface required-item availability; remove bank-step routing *(merged 2026-05-11)*

Closes #380. Major behavior change: `requiredItemIds` now surface as colored entries in the panel + in-game overlay instead of substituting a synthetic GE bank step.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate Shades of Mort'ton with no tinderbox -> active step is **step 1 (travel)**, NOT a synthetic "Get required items from bank" step | `[ ]` | |
| 2 | Side panel: required-items section shows **Tinderbox** with RED border + "(MISSING)" tooltip | `[ ]` | |
| 3 | In-game overlay (top-left): "Item requirements:" header + "Tinderbox" line rendered in red | `[ ]` | |
| 4 | Withdraw tinderbox -> panel row turns GREEN with "(in inventory)" tooltip; overlay line turns green | `[ ]` | |
| 5 | With tinderbox in BANK only -> panel row YELLOW with "(in bank)" tooltip; overlay shows "Tinderbox (bank)" yellow | `[ ]` | |
| 6 | Equip a `requiredItemIds` item -> panel shows GREEN with "(equipped)" tooltip | `[ ]` | |
| 7 | Walk onto the active step's tile -> minimal overlay panel renders WITH only the requirements list when items are required; NO panel when no items are required | `[ ]` | |
| 8 | Activate a source whose current step has no `requiredItemIds` -> no "Item requirements:" section appears | `[ ]` | |
| 9 | World map view -> route overlay still draws correctly (regression check) | `[ ]` | |
| 10 | Trigger Skip on a step with required items -> next step's requirements render in same tick | `[ ]` | |
| 11 | Stop guidance -> requirements section disappears from both panel and overlay | `[ ]` | |
| 12 | Switch sources without stopping (panel-driven mode change) -> stale items from previous source don't leak | `[ ]` | |

---

## PR #383 — ROADMAP Tier B.5 *(merged 2026-05-11)*

Documentation only. No in-game validation required. **Skip.**

---

## PR #390 (fix/373-overlay-toggle-preserves-guidance) — Show Overlays toggle no longer kills the guidance session *(merged 2026-05-11)*

Closes cha-ndler/collection-log-helper#373. Removes the early-return at the top of `GuidanceOverlayCoordinator.activateGuidance` that aborted the whole call when `config.showOverlays()` was false. Since cha-ndler/collection-log-helper#386 added per-overlay self-gating, the early-return is now redundant *and* wrong: toggling Show Overlays off should hide overlays (which #386 already does), not refuse to start guidance.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | With Show Overlays OFF, click Guide Me on any source → panel button changes to "Stop Guidance", banner shows "Guiding: X", step progress widget appears in panel, but NO 3D world overlays render | `[ ]` | |
| 2 | While guidance is active with Show Overlays OFF, toggle Show Overlays ON → overlays appear immediately at the current step's target (without re-clicking Guide Me) | `[ ]` | |
| 3 | While guidance is active with Show Overlays ON, toggle Show Overlays OFF → overlays disappear immediately but guidance stays active (panel button stays "Stop Guidance") | `[ ]` | |
| 4 | Repeat toggle cycle 3 times rapidly → state stays consistent each cycle; no stuck buttons; no console errors | `[ ]` | |
| 5 | Stop Guidance manually with Show Overlays OFF → button returns to "Guide Me", banner clears | `[ ]` | |
| 6 | Regression: Show Overlays ON + Guide Me on Shades of Mort'ton → still works as in cha-ndler/collection-log-helper#389 (panel + overlay both populate) | `[ ]` | |

---

## PR fix/step-progress-view-client-thread — StepProgressView tooltip lookup no longer crashes *(pending merge)*

Closes the same class of bug as cha-ndler/collection-log-helper#389 but on the side-panel widget. `StepProgressView.updateRequiredItemDisplay` calls `itemManager.getItemComposition(itemId).getName()` to build tooltips for each required-item icon — but that runs on the EDT (the widget is a Swing panel), and `getItemComposition` asserts caller-is-client-thread. The `AssertionError` aborted the loop mid-iteration, leaving the required-items strip hidden in the panel and dumping stack traces to `client.log`.

Minimal fix: wrap the `getItemComposition` call in `try / catch (Throwable)`, falling back to `"Item #N"` for the tooltip. Loop completes, panel renders icons + colored borders correctly; only tooltips degrade to IDs when on EDT.

A proper architectural fix (pre-resolved `List<RequiredItemDisplay>` passed from coordinator on client thread) is queued as a follow-up; out of scope here to keep the PR minimal.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with `requiredItemIds` (e.g. Shades of Mort'ton step 1) → side-panel required-items strip shows item icons with colored borders | `[ ]` | |
| 2 | Hover over an item icon → tooltip shows either the item name (if cache loaded on EDT) OR "Item #N (in inventory / bank / MISSING)" — both acceptable, no crash | `[ ]` | |
| 3 | Open `client.log` after a Guide Me click → ZERO `Uncaught exception` lines pointing at `StepProgressView.updateRequiredItemDisplay` | `[ ]` | |
| 4 | Cycle Guide Me → Stop Guidance → Guide Me 5 times → required-items strip renders each time, no degradation | `[ ]` | |

---

## PR fix/381-hint-arrow-refresh-after-teleport — Hint arrow re-applies after long-distance teleport *(pending merge)*

Closes cha-ndler/collection-log-helper#381. After teleporting away mid-guidance (e.g. Mort'ton → Grand Exchange via bank teleport, then back), the in-game hint arrow stayed cleared at the new step's target location. `client.setHintArrow(WorldPoint)` is bound to the loaded scene — when the scene reloaded, the arrow target was no longer rendered and walking back into range did not auto-restore it. Before this fix, only step transitions and the "Show Hint Arrow" config toggle re-set the arrow; neither fires on a teleport.

Fix: introduces a new small focused class `com.collectionloghelper.guidance.GuidanceMovementTracker` (~160 lines, single responsibility) that subscribes to `GameTick` and tracks the player's WorldPoint each tick while guidance is active. When the single-tick Chebyshev delta exceeds 10 tiles (running caps at 2 tiles/tick — any single-tick jump >10 is unambiguously a teleport) or the plane changes, it calls `GuidanceOverlayCoordinator.refreshHintArrow()`. That existing method already gates on `config.showHintArrow()` and the active step's world target, so the tracker fires unconditionally on teleport and lets the coordinator decide whether to set, clear, or no-op. Extracted as a separate class (rather than another handler on `GuidanceEventRouter`, which already has 10 subscribers and 549 lines) so future movement-driven guidance behavior (e.g. step regression on player-left-step-location for cha-ndler/collection-log-helper#378) can live alongside it.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a multi-step source (Shades of Mort'ton). Mid-sequence, teleport away (Grand Exchange teleport). Teleport back to Mort'ton → hint arrow (minimap + in-game tile arrow) reappears at the current step's target WITHOUT having to Stop / Start Guidance | `[ ]` | |
| 2 | With "Show Hint Arrow" config DISABLED, teleport long-distance during active guidance → no arrow appears (refresh respects the toggle) | `[ ]` | |
| 3 | Plane change mid-guidance (climb a ladder, enter a basement) → hint arrow re-applies at the current step's target on the new plane | `[ ]` | |
| 4 | Ordinary walking 10+ ticks across a normal scene boundary (no teleport) → arrow does NOT flicker / churn (no false-positive refresh) | `[ ]` | |
| 5 | Stop Guidance, teleport, Start Guidance fresh → first tick re-baselines silently; no spurious refresh from prior baseline | `[ ]` | |
| 6 | Step transitions mid-session still fire arrow updates (existing applyStepToOverlays path unchanged) | `[ ]` | |
| 7 | client.log grep for `IllegalStateException` / `AssertionError` from `setHintArrow` after teleport → should be ZERO entries (refresh runs through coordinator which dispatches on client thread) | `[ ]` | |

---

## PR fix/374-locked-items-natural-position — Locked items rank by score, not segregated *(merged 2026-05-11, PR #392)*

Closes cha-ndler/collection-log-helper#374. Three identical `Comparator.comparing(ScoredItem::isLocked).thenComparing(...)` chains in `EfficiencyCalculator` (`rankByEfficiency`, `filterByCategory`, `filterPetsOnly`) sorted by `isLocked` first — pushing every locked item to the bottom of the list regardless of its score. Removed the segregation key: score-only sort. Locked items keep their `isLocked` flag for the renderer (red highlight + lock icon + tooltip), they just appear in their natural efficiency position now.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Efficient mode with a locked source whose score sits between two unlocked ones → locked source appears INTERLEAVED, not at the bottom | `[ ]` | |
| 2 | Locked source still renders with red highlight + lock icon (renderer unchanged) | `[ ]` | |
| 3 | Hover the locked source → tooltip names the unmet requirement (quest / skill / diary) | `[ ]` | |
| 4 | Toggle Hide Locked Content ON → locked sources still disappear entirely (no regression from cha-ndler/collection-log-helper#387) | `[ ]` | |
| 5 | Category Focus mode with mixed locked/unlocked sources → same interleaving behavior | `[ ]` | |
| 6 | Top Pick still picks the highest-score UNLOCKED source (locked sources can't be Top Pick — `findFirst().filter(!isLocked)` semantics intact) | `[ ]` | |

---

## PR fix/required-items-client-thread — Guide Me activation regression *(pending merge)*

Closes the regression surfaced by trace from PR #388: Guide Me on any source with `requiredItemIds` (e.g. Shades of Mort'ton) silently failed because `RequiredItemResolver.lookupName` called `ItemManager.getItemComposition()` from the EDT, which throws `AssertionError("must be called on client thread")`. The original catch only caught `RuntimeException`, so the assertion propagated up and aborted `activateGuidance` mid-flight — leaving the overlay state set but the panel state stale ("Guide Me" instead of "Stop Guidance").

Two-layer fix:
1. `GuidanceOverlayCoordinator.applyStepToOverlays` now dispatches the resolve + set onto `clientThread.invokeLater()`.
2. `RequiredItemResolver.lookupName` widens its catch from `RuntimeException` to `Throwable` (belt-and-suspenders) so a future caller that forgets the client-thread contract can't silently kill activation again.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Click Guide Me on Top Pick "Shades of Mort'ton" (Bronze lock) → panel button changes to "Stop Guidance" AND in-game overlay shows step 1 content. State stays in sync. | `[ ]` | |
| 2 | Click Stop Guidance → panel button returns to "Guide Me" AND overlay clears. | `[ ]` | |
| 3 | Click Guide Me from the Bronze lock item-detail view → button changes correctly there too. | `[ ]` | |
| 4 | Click Guide Me on a source with no `requiredItemIds` (e.g. Larran's Big Chest → Dagon'hai robe bottom) → still works as before (regression check). | `[ ]` | |
| 5 | Repeatedly Guide Me / Stop Guidance on the same source 5 times → state stays consistent each cycle; no stuck buttons. | `[ ]` | |
| 6 | Open client.log and grep `Item composition lookup failed` → should be ZERO entries during normal activation (the assertion path is no longer hit). | `[ ]` | |
| 7 | Required-items section in panel + in-game overlay renders item NAMES (e.g. "Tinderbox"), not raw IDs (e.g. "Item #590"). | `[ ]` | |

---

## PR fix/364-config-triggered-panel-rebuild — Filter-config toggles trigger panel rebuild *(pending merge)*

Closes #364 (Hide Locked Content toggle inert). Approach: extends the `@Subscribe onConfigChanged` handler added in fix/363 with a `FILTER_CONFIG_KEYS` set; when any of those keys change, fires a plugin-supplied callback that flips `pendingPanelRebuild = true` + `rankedSourcesDirty = true`. Next game-tick coalesces a single rebuild against the new filter state.

Covers six toggles previously broken or untested for mid-session changes:
`hideLockedContent`, `hideObtainedItems`, `accountType`, `raidTeamSize`, `afkFilter`, `efficientSortMode`.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Toggle Hide Locked Content ON mid-session -> within 1 tick (~600ms) the list rebuilds; locked items disappear from Efficient / Category / Search lists | `[ ]` | |
| 2 | Toggle Hide Locked Content OFF again -> locked items return at their natural efficiency position (red highlight + lock icon) | `[ ]` | |
| 3 | Toggle Hide Obtained Items ON -> sources with no missing items disappear within 1 tick | `[ ]` | |
| 4 | Switch Account Type Main -> Iron mid-session -> efficiency list re-ranks; iron-only sources change visibility / position | `[ ]` | |
| 5 | Switch Raid Team Size SOLO -> FIVE -> raid sources re-rank with team-size-adjusted efficiency | `[ ]` | |
| 6 | Switch Efficient Sort Mode (Efficiency -> Time, etc.) -> list re-orders within 1 tick | `[ ]` | |
| 7 | Adjust Efficient AFK Filter -> list re-filters by AFK level within 1 tick | `[ ]` | |
| 8 | Regression: change Overlay Color or NPC Highlight Style -> panel does NOT pointlessly rebuild (toggle non-filter key with side panel open; check there's no visual flicker) | `[ ]` | |
| 9 | Regression: rapid-fire-toggle Hide Locked Content 5 times in <1s -> panel rebuilds once on the next tick (coalesced via `pendingPanelRebuild`); no console errors | `[ ]` | |

---

## PR fix/363-config-toggle-propagation — Config toggles propagate immediately *(pending merge)*

Closes #363 (Show Overlays / Show Hint Arrow toggles inert mid-guidance). Approach: each overlay self-gates on `config.showOverlays()` at render time (cheap volatile read per frame); `@Subscribe onConfigChanged` handler revokes / re-applies `client.setHintArrow()` since that's a one-shot client mutation rather than a render-loop predicate.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance, toggle **Show Overlays** OFF -> object highlight, tile marker, world map route, NPC outline, dialog highlight, ground item highlight, and the top-left guidance overlay panel all disappear within 1 frame (~50ms) | `[ ]` | |
| 2 | Toggle Show Overlays ON -> overlays return immediately (state was preserved, just gated) | `[ ]` | |
| 3 | Toggle Show Overlays OFF while sync/bank reminders are visible -> reminders remain (independent of overlay toggle) | `[ ]` | |
| 4 | Activate guidance, toggle **Show Hint Arrow** OFF -> in-game tile arrow + minimap arrow clear immediately | `[ ]` | |
| 5 | Toggle Show Hint Arrow ON mid-guidance -> arrow re-renders on current step's target | `[ ]` | |
| 6 | Toggle Show Hint Arrow rapidly off/on/off/on -> no stale arrows, no errors in console | `[ ]` | |
| 7 | Without guidance active, toggle Show Hint Arrow ON -> no arrow appears (correct: no target) | `[ ]` | |
| 8 | Activate guidance on a clue-tier source (no step target) -> Show Hint Arrow toggle has no visible effect (correct: no target to point at) | `[ ]` | |
| 9 | Regression: skip button mid-guidance with Show Overlays ON -> overlays update on next tick (matches pre-fix behavior) | `[ ]` | |

---

## PR #396 — B.5.6 NPC and object outline-glow highlight style *(pending merge)*

Closes cha-ndler/collection-log-helper#377. Refs cha-ndler/collection-log-helper#382 (B.5.6).

Adds `OUTLINE_GLOW` to `NpcHighlightStyle` and a new `ObjectHighlightStyle` enum (also gains `HULL` as the legacy option). Defaults flip to `OUTLINE_GLOW` for both. Uses RuneLite's native `ModelOutlineRenderer.drawOutline(actor/tileObject, width=6, color, feather=4)` — exposed in 1.12.26.3, no custom vertex projection needed. Existing `HULL` paths preserved for backwards compatibility.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with an NPC step (e.g. Vorkath, Cerberus) → the target NPC renders with a glowing model outline, not a clickbox / hull polygon | `[ ]` | |
| 2 | Activate guidance on a source with an object step (e.g. Shades of Mort'ton funeral pyre, GWD door) → the target object renders with the same outline-glow style | `[ ]` | |
| 3 | Config → NPC Highlight Style → switch from `OUTLINE_GLOW` to `HULL` → bounding hull returns immediately (no Stop/Start required, propagation from PR #386) | `[ ]` | |
| 4 | Config → Object Highlight Style → switch from `OUTLINE_GLOW` to `HULL` → bounding hull returns for objects | `[ ]` | |
| 5 | Both styles set back to `OUTLINE_GLOW` → outline returns | `[ ]` | |
| 6 | Outline colour matches the `Overlay Color` config setting (change it to a vivid colour to verify) | `[ ]` | |
| 7 | Multi-form NPC mid-fight (Zulrah, Hydra) → outline tracks the current form's model | `[ ]` | |
| 8 | Walk a long distance from the highlighted target → outline keeps tracking until target leaves render distance, then disappears cleanly (no lingering outline ghost) | `[ ]` | |
| 9 | FPS check: 30 seconds parked in a busy scene (GE) with guidance active → no perceptible FPS drop vs `HULL` mode (outline should be free per-frame from `ModelOutlineRenderer` cache) | `[ ]` | |
| 10 | Regression: NPC indicator / object indicator stock RuneLite plugins still render their own highlights independently (don't conflict with ours) | `[ ]` | |
| 11 | `client.log` after 5 minutes of guidance → ZERO stack traces mentioning `ModelOutlineRenderer` or `drawOutline` | `[ ]` | |

---

## PR #397 — B.5.5 per-step world map arrow and destination icon *(pending merge)*

Refs cha-ndler/collection-log-helper#382 (B.5.5).

New `WorldMapDestinationOverlay` (338 LOC) adds two world-map visuals tied to the active guidance step:
- **Edge-snap directional arrow** at the world-map boundary when the destination tile is off-screen (8 pre-rendered sprites, every 45°).
- **Destination icon** at the target tile when it is on-screen:
  - NPC step (has `npcId` / `npcName`) → person silhouette icon
  - Object step (has `objectId`) → chest icon
  - Otherwise → diamond tile marker

Wired through `GuidanceOverlayCoordinator` (`setTarget` on `applyStepToOverlays` / `applySourceToOverlays`; `clearTarget` on `deactivateGuidance` and the no-location step branch). `WorldMapRouteOverlay` is untouched — both render layers coexist.

> **Known visual overlap flagged by the worker**: when the destination is off-screen, both this overlay's arrow AND the existing `CollectionLogWorldMapPoint` edge-snap chevron will draw simultaneously. Same teal colour, so visually consistent, but worth a product decision in tests 6–7 below. Easy fix either direction.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with an NPC step. Open world map → on-screen destination shows a **person silhouette** icon at the target tile | `[ ]` | |
| 2 | Activate guidance on a source with an object step (chest, door). Open world map → on-screen destination shows a **chest** icon at the target tile | `[ ]` | |
| 3 | Activate guidance on a clue-tier / generic source. Open world map → on-screen destination shows a **diamond tile marker** | `[ ]` | |
| 4 | Pan the world map so the destination scrolls off-screen → **edge-snap arrow** appears at the map boundary pointing toward the destination; the on-screen icon disappears | `[ ]` | |
| 5 | Pan in 8 directions (N, NE, E, SE, S, SW, W, NW) → the arrow sprite rotates correctly through all 8 pre-rendered orientations | `[ ]` | |
| 6 | **Overlap check**: with destination off-screen, observe whether the existing `CollectionLogWorldMapPoint` chevron ALSO draws. Both visible = expected per worker note. Decide product preference | `[ ]` | both? hide one? |
| 7 | If both chevrons draw and the result looks cluttered → file a follow-up issue; otherwise leave as-is | `[ ]` | |
| 8 | Existing `WorldMapRouteOverlay` route line still draws between waypoints (regression check) | `[ ]` | |
| 9 | Stop guidance → both arrow and icon disappear from world map immediately | `[ ]` | |
| 10 | Step transition mid-sequence → icon + arrow update to the new step's destination on the next tick | `[ ]` | |
| 11 | Step with no destination tile (clue caskets, milestone-only sources) → no arrow, no icon (correct: nothing to point at) | `[ ]` | |
| 12 | FPS check: open world map for 30 seconds with guidance active → no perceptible drop (all sprites cached at construction, zero per-frame allocation) | `[ ]` | |
| 13 | `client.log` after a session including 5+ teleports / step transitions → ZERO stack traces from `WorldMapDestinationOverlay` | `[ ]` | |

---

## PR #398 — B.5.1 per-step item requirements with bank-scan state colouring *(pending merge)*

Refs cha-ndler/collection-log-helper#382 (B.5.1).

Adds an "Items needed:" subsection to `StepProgressView` that lists each entry in the **current step's** `requiredItemIds` with availability colouring:
- **Green** name label: item held in inventory OR worn
- **White** name label + small ℹ icon + tooltip "in bank": present in last bank scan
- **Red** name label: not found anywhere

`StepProgressView` no longer takes `PlayerInventoryState` / `PlayerBankState` directly. It receives pre-resolved `List<RequiredItemDisplay>` rows produced on the **client thread** by `RequiredItemResolver`, then pushed to the EDT (avoids the `ItemManager` client-thread assert seen in #388 / #389). `GuidanceOverlayCoordinator` defers resolution via `clientThread.invokeLater` in both `activateGuidance` and `onStepChanged` paths.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on Shades of Mort'ton (step 1 has `requiredItemIds`: tinderbox + pyre logs + shade remains) → side panel renders an "Items needed:" header followed by one row per required item | `[ ]` | |
| 2 | Required item in INVENTORY → row name renders in GREEN | `[ ]` | |
| 3 | Required item EQUIPPED → row name renders in GREEN (treated same as inventory) | `[ ]` | |
| 4 | Required item ONLY in last bank scan → row name renders in WHITE with small ℹ icon; hover the ℹ → tooltip says "in bank" | `[ ]` | |
| 5 | Required item missing from inventory, equipment, AND bank → row name renders in RED | `[ ]` | |
| 6 | Activate guidance on a source whose current step has no `requiredItemIds` (e.g. Vorkath if no required items, or a clue tier) → NO "Items needed:" header appears | `[ ]` | |
| 7 | Pick up a MISSING required item from the floor → row transitions RED → GREEN on the next tick without rebuilding the whole panel | `[ ]` | |
| 8 | Drop a required item from inventory while standing in front of a bank (so it's still in bank) → row transitions GREEN → WHITE | `[ ]` | |
| 9 | Drop the required item somewhere it's NOT in bank → row transitions GREEN → RED | `[ ]` | |
| 10 | Step advance (auto-arrival or Next Step) to a step whose `requiredItemIds` differs → "Items needed:" list refreshes to the new step's items in the same tick (no stale entries) | `[ ]` | |
| 11 | Step advance to a step with no `requiredItemIds` → header disappears cleanly | `[ ]` | |
| 12 | Equipped item rendering: equip a required item → tooltip on the row says "in inventory" or "equipped" (either acceptable as long as the colour is GREEN) | `[ ]` | |
| 13 | Open bank to refresh bank scan, then close → row that was RED (item only in bank, scan stale) refreshes to WHITE | `[ ]` | |
| 14 | Stop Guidance → "Items needed:" subsection disappears with the rest of the step strip | `[ ]` | |
| 15 | Cycle Guide Me → Stop Guidance → Guide Me 5 times → list renders correctly each time, no stale rows from prior sessions | `[ ]` | |
| 16 | `client.log` after 5 minutes of guidance with step transitions → ZERO `AssertionError` or `IllegalStateException` from `ItemManager.getItemComposition`, `getImage`, or `RequiredItemResolver` | `[ ]` | |
| 17 | Regression: in-game overlay required-items panel (added in #384) still works alongside the new side-panel section | `[ ]` | |

---

## Still-open bugs that closed PR #371 left unfixed

These need fresh validation when a follow-up PR ships the real fix. The "still broken on master" column captures the regression baseline observed on 2026-05-10.

| Issue | Reproducer | Still broken on master? | Fix PR | Verified after fix? |
|---|---|---|---|---|
| #362 | Category Focus -> SLAYER -> count reads "0/0", no progress bar | `[ ]` | — | `[ ]` |
| #362 | Category Focus -> SKILLING -> same "0/0" issue | `[ ]` | — | `[ ]` |
| #363 | Activate guidance -> toggle Show Overlays OFF -> overlay should clear immediately (not require Stop/Start) | `[!]` | fix/363 | `[ ]` |
| #363 | Activate guidance -> toggle Show Hint Arrow OFF -> minimap arrow + in-game tile arrow clear immediately | `[!]` | fix/363 | `[ ]` |
| #364 | Toggle Hide Locked Content ON -> items with unmet quest/skill requirements disappear from list | `[!]` | fix/364 | `[ ]` |
| #373 | Toggle Show Overlays OFF mid-guidance -> guidance session CONTINUES; only overlay rendering stops | `[!]` | fix/373 | `[ ]` |
| #374 | Efficient mode with locked items -> locked items appear at their natural efficiency position (e.g., a ~17-min locked item sits near other ~17-min items), not segregated to the bottom | `[!]` | fix/374 | `[ ]` |
| #378 | Walk away from the active step's tile mid-sequence -> step does NOT regress to a prior step | `[ ]` | — | `[ ]` |
| #381 | Mid-guidance: teleport far away and back -> hint arrow re-renders without a Stop/Start cycle | `[!]` | fix/381 | `[ ]` |

---

## Regression smoke test (run after ANY batch of in-game testing)

Quick check that core features still work end-to-end:

- `[ ]` Sync collection log: open the in-game collection log widget once -> "Synced N items" reminder clears
- `[ ]` Bank scan: open bank once -> "Open Bank to scan items" reminder clears
- `[ ]` Switch between modes (Efficient / Category / Search / Pet Hunt / Statistics) -> no errors, panel renders for each
- `[ ]` Activate guidance on a non-Mort'ton source (Vorkath, Cerberus, Wintertodt, a clue tier) -> panel + overlay render
- `[ ]` Stop guidance, restart guidance on the same source -> clean cycle, no orphaned overlays or InfoBoxes
- `[ ]` Close client, reopen -> bank scan cache restores correctly per RS profile (verify by re-opening Helper panel without re-banking)

---

## Reporting findings

When a regression / failure is observed:

1. Screenshot the in-game state (full RuneLite window, including the panel).
2. Capture the relevant log output (RuneLite `Help` → `Open logs folder` -> `client.log`).
3. File a GitHub issue with: the screenshot, the master commit SHA at testing time (`git rev-parse HEAD`), and the reproducer steps from this file.
4. Cross-link the new issue in the Notes column above.
5. Update this file's status from `[ ]` to `[!]` so the next pass picks it up.

When all items in a PR section are `[x]`:

- Add the validation date to the section header: e.g. `## PR #372 — ... *(merged 2026-05-11, validated 2026-05-15)*`.
- Consider the PR's in-game obligations closed.
- Older sections can be archived to a `validation-archive/` subdirectory if this file grows long.
