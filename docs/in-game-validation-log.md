# In-Game Validation Log

> **Purpose**: Track manual in-game testing for merged PRs. Unit tests, type checks, and CI only prove correctness of pure logic — anything touching player state, overlay rendering, or live game events needs human verification against the OSRS client. This file is the source of truth for "did the fix actually work for real users?"

> **Status legend**:
> - `[ ]` — Not tested yet
> - `[~]` — Testing in progress
> - `[x]` — Verified pass
> - `[!]` — Regression / failed (file a new issue and cross-link)
> - `[?]` — Inconclusive (couldn't reproduce / state-dependent)

> **Last updated**: 2026-05-14 (session: continuous Tier B/B.5/C/E/F implementation)

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

## PR #400 — B.5.1b `requiredItemIds` backfill across 7 high-impact sources *(merged 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.1b). Pure data — no code change.

Added `requiredItemIds` arrays to 12 guidance steps across 7 sources. Validation lights up the existing B.5.1 "Items needed:" subsection for these sources.

| # | Source | Step | Expected item(s) in "Items needed:" | Status | Notes |
|---|---|---|---|---|---|
| 1 | Kree'arra | Step 4 (grapple room entry) | Mithril grapple | `[ ]` | |
| 2 | Skotizo | Step 1 (dark totem use) | Dark totem | `[ ]` | |
| 3 | Hespori | Step 1 (plant seed) | Hespori seed + spade | `[ ]` | |
| 4 | Barrows | Step 2 (Ahrim dig) | Spade | `[ ]` | |
| 5 | Barrows | Step 3 (Dharok dig) | Spade | `[ ]` | |
| 6 | Barrows | Step 4 (Guthan dig) | Spade | `[ ]` | |
| 7 | Barrows | Step 5 (Karil dig) | Spade | `[ ]` | |
| 8 | Barrows | Step 6 (Torag dig) | Spade | `[ ]` | |
| 9 | Barrows | Step 7 (Verac dig) | Spade | `[ ]` | |
| 10 | Tempoross | Step 2 (fish harpoonfish) | Harpoon | `[ ]` | |
| 11 | Wintertodt | Step 2 (enter arena) | Tinderbox + axe | `[ ]` | |
| 12 | The Inferno | Step 1 (sacrifice cape) | Fire cape | `[ ]` | |
| 13 | Spot-check excluded source (e.g. Vorkath) | Step 1 | NO "Items needed:" header (correct — Vorkath has no hard-gated items) | `[ ]` | |
| 14 | `./gradlew test` passes locally on master at HEAD | n/a | n/a | `[ ]` | |

---

## PR #401 — B.5.3 General requirements (quest/skill/diary) in guidance header *(merged 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.3).

When guidance activates, a "Requirements:" subsection appears below the "Guiding: \<source\>" headline (above the step strip), listing each prerequisite as a coloured row:
- Quest: GREEN (COMPLETED) / YELLOW (IN PROGRESS) / RED (NOT STARTED)
- Skill: GREEN (level met) / RED (level short, shows "level X/Y")
- Diary: GREEN (COMPLETED) / RED (NOT COMPLETED)
- Hidden entirely when source has no requirements.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a multi-requirement source (e.g. Cerberus → 91 Slayer) → "Requirements:" subsection appears under the headline | `[ ]` | |
| 2 | Player meets a skill requirement → row renders GREEN, format like "Slayer 91/91" | `[ ]` | |
| 3 | Player short on a skill requirement → row renders RED, format like "Slayer 85/91" | `[ ]` | |
| 4 | Quest requirement met → row renders GREEN with "COMPLETED" | `[ ]` | |
| 5 | Quest requirement in progress → row renders YELLOW with "IN PROGRESS" | `[ ]` | |
| 6 | Quest requirement not started → row renders RED with "NOT STARTED" | `[ ]` | |
| 7 | Diary requirement met (e.g. Kandarin Diary tier for a source that gates on it) → row renders GREEN | `[ ]` | |
| 8 | Diary requirement not met → row renders RED with the tier name | `[ ]` | |
| 9 | Activate guidance on a source with NO requirements (e.g. a low-level clue tier) → "Requirements:" subsection is absent (no empty header) | `[ ]` | |
| 10 | Stop Guidance → "Requirements:" subsection disappears with the rest of the header | `[ ]` | |
| 11 | Switch sources mid-session → "Requirements:" refreshes to the new source's requirements (no stale rows) | `[ ]` | |
| 12 | Train a skill from below to at-or-above the requirement → row transitions RED → GREEN on the next reconciliation (may require a Stop/Start cycle if state isn't reactive — note observation) | `[ ]` | |
| 13 | Regression: B.5.1 "Items needed:" subsection still appears below the step strip, unaffected by the new header section | `[ ]` | |
| 14 | `client.log` after 5 minutes of guidance → ZERO stack traces mentioning `RequirementsChecker`, `RequirementsView`, `buildRequirementRows`, or diary varbit lookup | `[ ]` | |

---

## PR #402 — B.5.4 Collapsible step sections via `section` field on `GuidanceStep` *(merged 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.4).

Adds a `String section` field to `GuidanceStep` (additive, backwards compatible — null = legacy flat layout). When ANY step on the active source has `section` set, `StepProgressView` groups consecutive same-section steps into collapsible blocks. Active step's section auto-expands. Null-section steps fall into an "Other" group.

> **Validation note**: No source in `drop_rates.json` has a `section` field set yet (data backfill is a follow-up milestone B.5.4b). Most in-client checks here are **regression checks** that the existing flat layout is unchanged. The sectioned-layout behaviour is currently only exercised by unit tests; in-client validation will expand once sections are authored on real sources.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on any multi-step source (e.g. Shades of Mort'ton) → step strip renders in the legacy flat layout, identical to pre-#402 master | `[ ]` | |
| 2 | Step strip shows all steps in order with the existing "Items needed:" subsection attached to the active step | `[ ]` | |
| 3 | Auto-advance through 2–3 steps mid-session → strip updates per step, no section-header artefacts appear | `[ ]` | |
| 4 | Skip / Next Step buttons work as before | `[ ]` | |
| 5 | Stop Guidance → strip clears as before | `[ ]` | |
| 6 | `./gradlew test` passes locally on master at HEAD (the 18 new StepSectionGrouper / StepProgressView section tests cover the sectioned-layout paths) | `[ ]` | |
| 7 | OPTIONAL (advanced): hand-edit `drop_rates.json` for a single source to set `section` on a few consecutive steps (e.g. add `"section": "Bank prep"` to step 1, `"section": "Travel"` to steps 2–3, `"section": "Combat"` to steps 4+ on a test source) → activate guidance → verify section headers render with "▼" expanded chevron, active step's section is expanded, other sections collapse on click | `[ ]` | optional pending B.5.4b data |
| 8 | `client.log` after 5 minutes of guidance → ZERO stack traces mentioning `StepSectionGrouper`, `sectionsPanel`, or `StepProgressView` section paths | `[ ]` | |

---

## PR #404 — B.5.7 dialog highlighting audit + polish *(merged 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.7). Fixed 3 dialog-overlay bugs surfaced during audit: location-less dialog steps not wiring overlay state; widget-state mutation across frames; always-on NPC continue-prompt rendering when no dialog step is active.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a dialog-heavy source (any source with a `dialogChoice` step) → expected dialog option highlights when player opens that NPC dialog | `[?]` | Not exercised in 2026-05-13 pass (T7 skipped). |
| 2 | Without any active guidance step, talk to a random NPC (banker, Doomsayer, shop NPC) → NO dialog highlight artifacts; the overlay is cleanly absent | `[?]` | Not exercised. |
| 3 | Switch sources mid-conversation → dialog highlight refreshes (no stale arrow prefix from prior step) | `[ ]` | |
| 4 | `client.log` after 5 minutes of dialog interactions → ZERO `Uncaught exception` traces in `DialogHighlightOverlay` | `[ ]` | |

---

## PR #405 — B.5.2 recommended items section per guidance step *(merged 2026-05-13, validated 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.2). New "Recommended:" subsection below "Items needed:" rendering `recommendedItemIds` with the same bank-scan colouring as B.5.1.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on Barrows (Dharok step has recommendedItemIds from B.5.2b: Prayer potion + Super restore) → "Recommended:" subsection appears below "Items needed:" with both rows colour-coded | `[x]` | T6 PASS — region capture confirmed Prayer potion(4) + Super restore(4) rows rendered correctly. |
| 2 | Step with no `recommendedItemIds` → "Recommended:" subsection hidden (no empty header) | `[x]` | Implicit from T2 Shades of Mort'ton Bank prep step — no Recommended section rendered. |
| 3 | Required + recommended both populated → both subsections coexist in panel | `[x]` | T6 PASS — Spade (Items needed) + Prayer potion / Super restore (Recommended) visible together on Barrows Dharok step. |
| 4 | State transition: withdraw a recommended item from bank → row transitions WHITE → GREEN within ~1 tick | `[?]` | Not specifically exercised this pass; behaviour expected to mirror B.5.1's GREEN/WHITE/RED transitions which DID work for required items in T5. |

---

## PR #406 — B.5.7 follow-ups (widget ID constants + drawAfterInterface decision) *(merged 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.7 follow-ups). Replaced magic widget literals 219/231 with `InterfaceID.CHATMENU` / `InterfaceID.CHAT_LEFT`. Decision: skip `drawAfterInterface()` — `OverlayLayer.ABOVE_WIDGETS` already paints above all widgets.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source whose current step has a `dialogChoice` → dialog highlight renders on the correct option text (regression check) | `[?]` | Not exercised in 2026-05-13 pass. |
| 2 | `client.log` startup check → ZERO warnings about `InterfaceID` lookup or unknown widget IDs | `[x]` | T9 log audit confirmed clean. |
| 3 | No z-order regression: dialog highlight still appears ABOVE the dialog widget text (not behind) | `[?]` | Not exercised. |

---

## PR #407 — #370 efficiency sort in Category Focus mode *(merged 2026-05-13, partially validated 2026-05-13)*

Closes cha-ndler/collection-log-helper#370. Category Focus mode item list now sorts by descending efficiency score. Includes the `requiresPrevious` guard fix discovered during self-review (122 items across Hydra, Sire, ToB/ToA/CoX shroud tiers, Defenders, Graceful recolours) — items with unmet prerequisites no longer outrank legitimately-pursuable items.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Open Category Focus → Bosses → top item is the highest-efficiency UNOBTAINED item in that category | `[?]` | T1 of prior pass observed locked-item interleaving by score (Bronze lock at top by score, ~6 min); could not confirm specifically the Bosses filter was active. Functional path confirmed; specific category confirmation deferred. |
| 2 | Locked items interleave by score (not pinned at bottom) — regression check vs PR #392 | `[x]` | T1 evidence: Bronze lock (locked) appeared at top by score, not at bottom. |
| 3 | "Hide Obtained Items" OFF → obtained items appear AFTER all unobtained in the same category, in their own efficiency order | `[ ]` | |
| 4 | Cross-source items with `requiresPrevious` (e.g. Hydra eye/fang/heart, ToA shroud upgrades) appear with non-zero score ONLY when the prerequisite is owned | `[ ]` | |
| 5 | Switch between Category Focus and Efficient mode → Efficient mode behaviour unchanged from pre-#407 | `[ ]` | |

---

## PR #408 — B.5.4b sections + B.5.2b recommendedItemIds backfill *(merged 2026-05-13, partially validated 2026-05-13)*

Refs cha-ndler/collection-log-helper#382 (B.5.4b, B.5.2b). 18 sources received `section` labels (64 steps); 15 sources received `recommendedItemIds` arrays. Vocabulary: `Travel` / `Combat` / `Loot` / `Bank prep` / `Skilling` / `Reward`.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on Shades of Mort'ton → step strip groups under "Bank prep (1 step)" / "Skilling (1 step)" / "Loot (3 steps)" | `[x]` | T2 PASS — sections all visible in region capture. |
| 2 | Activate guidance on Barrows → "Travel (1 step)" / "Combat (6 steps)" / "Loot (2 steps)" sections | `[x]` | T6 PASS — Barrows sections visible. |
| 3 | Active step's section is auto-expanded; other sections collapse to header | `[x]` | T2 + T6 both showed: Bank prep expanded for Mort'ton (active step was step 1), Combat expanded for Barrows (active was step 2). |
| 4 | Activate guidance on Barrows Dharok step → "Recommended:" shows Prayer potion + Super restore | `[x]` | T6 PASS — both rows visible. |
| 5 | Activate guidance on a sectioned source with multiple steps in the same section → consecutive steps render under one header | `[ ]` | |
| 6 | Activate guidance on a source NOT in the 18-source priority list → flat layout (no section headers) | `[ ]` | |

---

## PR #409 — fix(guidance): wrap activateGuidance entry point on client thread *(merged 2026-05-13, validated 2026-05-13)*

Closes the regression discovered in the 2026-05-13 pre-fix validation pass: `Plugin.activateGuidance` ran on the EDT but called `RequirementsChecker.buildRequirementRows` (client-thread-only) introduced by PR #401.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Click Guide Me on Shades of Mort'ton (which fully failed pre-fix) → guidance activates, step strip renders | `[x]` | T2 PASS — full activation, step 1/5 visible, panel populated. |
| 2 | Click Top Pick green Guide Me button on any source → guidance activates cleanly | `[x]` | Implicit from session — multiple sources activated without issue. |
| 3 | Click Guide Me from the Item Detail view → guidance activates cleanly | `[x]` | Implicit — Barrows activation in T6 used this path. |
| 4 | `client.log` grep `AssertionError` since boot → ZERO matches | `[x]` | T9 log audit confirmed: no AssertionError since 09:35 EDT boot. |
| 5 | Regression: step-advance and skip (`c528d0ae`) still work | `[ ]` | Not specifically re-tested in 2026-05-13 pass; no evidence of regression. |

---

## PR #441 — feat(overlay): richer worldmap chevron + CLH-distinct color *(merged 2026-05-14)*

Closes #430. Replaces the plain 4-point filled polygon edge-snap arrow on the world map with a `Path2D.Double` notched chevron shape. Arrow fill changes from teal to orange (`#FF8C00`) to distinguish the CLH source arrow from native OSRS worldmap chevrons and Quest Helper indicators. Destination icons (NPC/object/tile) retain their existing teal fill.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on any multi-step source with a worldX/worldY target. Open world map and pan so the destination is off-screen → edge-snap arrow appears at the map boundary in **orange** (not teal) | `[ ]` | |
| 2 | Arrow shape: compare to the old PR #397 teal polygon — the new shape should look like a deeper-notched chevron with a dark-brown outline stroke, not a blunt filled triangle | `[ ]` | |
| 3 | Pan so the destination is on-screen → the orange edge arrow disappears; teal destination icon (NPC/object/tile) appears at the target tile (icon color unchanged) | `[ ]` | |
| 4 | Pan in all 8 cardinal/intercardinal directions with destination off-screen → arrow rotates correctly through all 8 pre-rendered orientations | `[ ]` | |
| 5 | With a `CollectionLogWorldMapPoint` (collection-log item pin) active simultaneously → CLH guidance arrow is suppressed (mapPointActive=true path from #438 still respected) | `[ ]` | |
| 6 | Stop guidance → orange arrow disappears from world map immediately | `[ ]` | |
| 7 | Regression: teal destination icons (NPC silhouette, chest, diamond) are unchanged in color and shape | `[ ]` | |

---

## PR #443 — fix(charset): force UTF-8 when loading guidance step text *(merged 2026-05-14)*

Fixes #433. `InputStreamReader` was opened without an explicit charset, defaulting to Windows-1252 on Windows hosts. This corrupted the three UTF-8 bytes of U+2014 (em-dash, `e2 80 94`) into `â€"` mojibake in the in-game overlay on Windows. Fix passes `StandardCharsets.UTF_8` to every `InputStreamReader` that opens a bundled JSON resource (`DropRateDatabase`, `SlayerMasterDatabase`, `VerifiedSceneIdRegistry`).

| # | Issue | Test | Status | Notes |
|---|---|---|---|---|
| 1 | #433 | Activate guidance on **Perilous Moons** → step description containing an em-dash (e.g. step 3 "Travel to Cam Torum — enter via cave entrance") renders as `—` not `â€"` in the in-game overlay | `[ ]` | Primary reproducer from the issue |
| 2 | #433 | Same check in the **side panel** step strip (not just the in-game overlay) → em-dash renders correctly | `[ ]` | |
| 3 | #433 | Activate guidance on any source whose `perItemStepDescription` contains an em-dash → overlay shows the correct character | `[ ]` | |
| 4 | — | Activate guidance on any source with ordinary ASCII step descriptions → text is unchanged (regression check, no corruption introduced) | `[ ]` | |
| 5 | — | Open `client.log` after plugin startup → ZERO `MalformedInputException` or charset-related errors from `DropRateDatabase`, `SlayerMasterDatabase`, or `VerifiedSceneIdRegistry` | `[ ]` | |

---

## PR #444 — data(cyclopes): per-defender step descriptions (B4.3.3) *(merged 2026-05-14)*

Closes B4.3.3 from #420. Backfills `perItemStepDescription` on the Cyclopes kill step (Warriors' Guild source step 2) for all 8 defender tiers: Bronze → Dragon. Each override names the tier being targeted and the defender that must be held in inventory for the drop to land. The static fallback description is preserved when no target item is set.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | In Efficient mode, identify the Warriors' Guild / Cyclopes source. Right-click **Iron defender** → Guide Me → activate guidance → in-game overlay step description mentions both "Iron" and "Bronze" (e.g. "Kill Cyclopes holding a Bronze defender to receive the Iron defender") | `[ ]` | |
| 2 | Repeat for **Dragon defender** → description mentions "Dragon" and "Rune" | `[ ]` | |
| 3 | Repeat for **Rune defender** → description mentions "Rune" and "Adamant" | `[ ]` | |
| 4 | Activate guidance on Warriors' Guild without right-clicking a specific defender (no target item) → step description shows the generic static text ("Kill Cyclopes for defender drops..."), not a tier-specific override | `[ ]` | |
| 5 | Side panel step strip: same tier-specific description appears in the panel (not just overlay) when a specific defender is targeted | `[ ]` | |
| 6 | Regression: non-Cyclopes sources with `perItemStepDescription` (e.g. Perilous Moons with per-Moon NPC retarget) are unaffected | `[ ]` | |

---

## PR #445 — feat(guidance): B4.4 — Top Pick auto-advance carries per-item target ID *(merged 2026-05-14)*

Closes the B4.4 milestone. When guidance auto-advances to the next Top Pick source after a sequence completes, the new session now activates with the per-item target ID set (from `ScoredItem.getBestItem().getItemId()`). Previously, auto-advance always used static 1-arg guidance, so per-item overrides (`perItemRequiredItemIds`, `perItemStepDescription`, `perItemNpcId`) were never lit up automatically.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source that will complete quickly (e.g. Shades of Mort'ton with all pre-conditions met and all items already in collection log except one). Complete the sequence → auto-advance fires and starts the next Top Pick source | `[ ]` | |
| 2 | If the auto-advanced Top Pick source is Warriors' Guild (Cyclopes defenders), the guidance session should open with the specific next defender tier as the target — verify the overlay description is tier-specific (not the generic fallback) | `[ ]` | |
| 3 | If the auto-advanced source does not use per-item overrides (e.g. a plain kill source) → guidance activates normally with null targetItemId (no regression; static description shown) | `[ ]` | |
| 4 | Manual right-click Guide Me on any source still works as before (does not pass a per-item ID from the right-click NPC menu path) | `[ ]` | |
| 5 | Stop guidance and manually Guide Me on the same source the auto-advance would have chosen → per-item target appears correctly via the manual path too (existing path unchanged) | `[ ]` | |

---

## PR #446 — fix(panel): prevent duplicate tier label on search-result row hover *(merged 2026-05-14)*

Fixes #428. `ItemRowPanel.mouseExited` fired when the mouse moved from the row panel into any child component (icon, name label, source label, rate label), not only when the mouse truly left the row. Each spurious exit triggered `setBackground()` + `repaint()` that raced with in-flight child `JLabel` paints, smearing label text and producing ghost-text artefacts such as `Shayzien helm (1) (1)`. Fix: guard `mouseExited` with `!contains(e.getPoint())` so intra-row child transitions are ignored.

| # | Issue | Test | Status | Notes |
|---|---|---|---|---|
| 1 | #428 | Switch to **Search** mode. Type "shay" → results load. Slowly hover over the **Shayzien helm (1)** row, deliberately moving between the icon, the name label, and the source label → item name renders as `Shayzien helm (1)` exactly once, no `(1) (1)` ghost text | `[ ]` | Primary reproducer |
| 2 | #428 | Repeat with a row whose source label includes a tier or count suffix (e.g. `Rune scimitar (Slayer)`, `Bronze defender`) → no doubled suffix on hover | `[ ]` | |
| 3 | — | Hover over a row and then move the mouse completely outside the panel → row background resets to default (mouseExited correctly fires on true boundary exit) | `[ ]` | |
| 4 | — | Hover cycle: enter → move to child → move back to row background → exit → re-enter → row stays highlighted the whole time while inside (no background flicker) | `[ ]` | |
| 5 | — | Regression: search results in Efficient and Category modes render row labels without artefacts (same ItemRowPanel paths used) | `[ ]` | |

---

## PR #448 — data(auto-completion): Tier 3 CHAT_MESSAGE_RECEIVED backfill for Pest Control and Brimhaven *(merged 2026-05-14)*

Partially fixes #306. Upgrades the final step of Pest Control and Brimhaven Agility Arena from `MANUAL` to `CHAT_MESSAGE_RECEIVED` using in-game chat message patterns verified against the OSRS Wiki. The sequencer matches these patterns via `Pattern.compile(pattern).matcher(message).find()`.

| # | Source | Test | Status | Notes |
|---|---|---|---|---|
| 1 | Pest Control | Complete a Pest Control game → on receiving the "You have successfully defended the island!" chat message, guidance auto-advances to the next step (or sequence completes) without a manual Skip click | `[ ]` | |
| 2 | Pest Control | Fail a Pest Control round (portals breach) → guidance does NOT auto-advance (failure message differs from the success pattern) | `[ ]` | |
| 3 | Brimhaven Agility Arena | Complete a ticket-earning obstacle → on receiving "You have received an Agility Arena Ticket" chat message, guidance auto-advances | `[ ]` | |
| 4 | Brimhaven Agility Arena | Run obstacles that do not grant a ticket → guidance does NOT advance prematurely | `[ ]` | |
| 5 | — | Regression: sources still on MANUAL completion (Soul Wars, Fishing Trawler, Volcanic Mine, Pyramid Plunder, Rogues' Den, Trouble Brewing) still require the user to click Skip/Next manually | `[ ]` | |

---

## PR #449 — feat(overlay): B.5.7 dialog highlight polish (color/flicker/primary) *(merged 2026-05-14)*

B.5.7 follow-up to PRs #404 and #406. Three rendering issues fixed: `BasicStroke` now hoisted to class-level constants (`PRIMARY_STROKE` 2.5 px, `SECONDARY_STROKE` 1 px) rather than inheriting whatever stroke was on the context; a `PRIMARY_BORDER_ALPHA = 220` variant added so the first (preferred) dialog option gets a visually heavier highlight; `RenderingHints.VALUE_ANTIALIAS_ON` applied at render entry for smoother rect outlines. `Click here to continue` prompt always treated as primary.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on a source with a `dialogChoice` step. Open that NPC's dialog → the matching option text box shows a **thick (~2.5 px) near-opaque border** | `[ ]` | |
| 2 | If the step lists two acceptable options (e.g. "Yes" and "Continue"), the **first** option gets the thick primary border; the second gets a thin (1 px) secondary border | `[ ]` | |
| 3 | A "Click here to continue" NPC continue prompt → renders with the thick primary border, not the thin secondary | `[ ]` | |
| 4 | Open the dialog and watch for several seconds → no border flicker between frames (color-equality cache should prevent per-frame `Color` allocation) | `[ ]` | |
| 5 | Display scaling >100% (if accessible in testing environment) → rect outlines appear smoother due to anti-aliasing hint | `[ ]` | |
| 6 | Open a non-guidance dialog (bank NPC, Doomsayer, shop NPC) → NO dialog highlight appears (guidanceActive = false guard intact) | `[ ]` | |
| 7 | Close dialog mid-guidance → highlight disappears; no lingering border on the widget | `[ ]` | |
| 8 | `client.log` after 5 minutes of dialog interactions → ZERO `Uncaught exception` traces from `DialogHighlightOverlay` | `[ ]` | |

---

## PR #450 — data(sailing): triage island-instance vs port-based coord sources *(merged 2026-05-14)*

Partially closes #314. Triages all 7 Sailing-related sources for `ARRIVE_AT_TILE` coord correctness. No JSON data changes — 4 sources confirmed RESOLVED (Port Piscarilius dock at 1824, 3691); 3 sources classified NEEDS-CAPTURE (instanced Sailing islands with no static cache coords).

> No in-game validation is needed for the RESOLVED sources — their Port Piscarilius coords (1824, 3691) were confirmed via NPC/dock cache. The NEEDS-CAPTURE sources below require in-game authoring-log capture during a Sailing voyage before their coords can be corrected.

| # | Issue | Source | NEEDS-CAPTURE test | Status | Notes |
|---|---|---|---|---|---|
| 1 | #314 | Lava Strykewyrm | Sail to Charred Island (60 Sailing required). Stand at the Charred Dungeon entrance. Record `WorldPoint` from RuneLite coordinate display. Compare to current coords in `drop_rates.json` — **expected mismatch** (current coords land in Isafdar/Tirannwn) | `[ ]` | Highest priority — guidance text is also wrong (copy-paste Wilderness text) |
| 2 | #314 | Gryphon (slayer) | Sail to Great Conch island (45 Sailing; fairy ring CJQ). Stand at eastern or western cave entrance. Record `WorldPoint`. Compare to current coords ~(1456, 2880) — expected mismatch (those land ~101 tiles from Aldarin) | `[ ]` | |
| 3 | #314 | Aquanite | Sail to Ynysdail (73 Sailing required). Enter Ynysdail Cavern. Stand at the entrance. Record `WorldPoint`. Determine if coords are static or instance-relative | `[ ]` | May already be plausible if Ynysdail is static; confirm before update |
| 4 | — | Regression: Cutting Squid, Lost Schematics, Sea Treasures, Port Tasks all use Port Piscarilius dock (1824, 3691) → activate guidance on each and confirm the arrival tile overlays center on the Port Piscarilius dockside area | `[ ]` | RESOLVED sources — quick sanity check |

---

## PR #452 — feat(panel): B.5.1 per-step required-items chips with bank-scan colors *(merged 2026-05-14)*

Implements B.5.1: adds `RequiredItemsChipPanel`, a horizontal strip of 28x28 item-icon chips rendered directly below the step-progress label in the guidance panel. Each chip has a 2-pixel colored border: green (held in inventory/equipment), white (found in last bank scan), or red (not found anywhere). White chips show tooltip "Items can be found in your: Bank". The chip strip coexists with the existing list-style "Items needed:" section added by PR #398.

| # | Test | Status | Notes |
|---|---|---|---|
| 1 | Activate guidance on **Shades of Mort'ton** (step 1 has tinderbox + pyre logs + shade remains) → a horizontal row of 3 item-icon chips appears directly below the step-progress label in the side panel | `[ ]` | |
| 2 | Item held in **inventory** → chip border is **green** | `[ ]` | |
| 3 | Item **equipped** → chip border is **green** (same as inventory) | `[ ]` | |
| 4 | Item **only in last bank scan** → chip border is **white**; hover the chip → tooltip reads "Items can be found in your: Bank" | `[ ]` | |
| 5 | Item **missing from inventory, equipment, and bank** → chip border is **red** | `[ ]` | |
| 6 | Activate guidance on a source whose current step has **no `requiredItemIds`** → chip strip is hidden (no blank row of empty chips) | `[ ]` | |
| 7 | Pick up a missing item → chip transitions **red → green** within ~1 tick, no panel rebuild required | `[ ]` | |
| 8 | Drop an item while it's still in bank → chip transitions **green → white** | `[ ]` | |
| 9 | Step advance → chip strip refreshes to show the new step's required items in the same tick | `[ ]` | |
| 10 | Open bank (refresh bank scan) then close → a chip that was red (item in bank, scan stale) transitions to white | `[ ]` | |
| 11 | Stop guidance → chip strip disappears with the rest of the step strip | `[ ]` | |
| 12 | Regression: the existing list-style "Items needed:" section (PR #398) still renders below the chip strip; both display surfaces active simultaneously | `[ ]` | |
| 13 | `client.log` after 5 minutes of guidance with step transitions → ZERO `AssertionError` or `IllegalStateException` from `RequiredItemsChipPanel` or `ItemManager.getImage` | `[ ]` | |

---

## In-game validation pass — 2026-05-16 (unvalidated PRs #454–#476) (in-progress)

The 2026-05-14 cascade merged ~24 PRs across Tiers B/B.5/C/E/F. None have a validation date yet. Pre-validation static review flagged 5 issues (#478–#482) that should be fixed in follow-up PRs but do NOT block in-game validation. Phased walkthrough below.

> **2026-05-16 attempt 1 — not started.** A subagent walkthrough was launched to drive this rubric interactively. Dev client (`./gradlew run`) started successfully on JDK 17 — plugin registered all event subscribers, `CollectionLogHelperPlugin started`, RuneLite frame shown at 13:22:39 EDT (PR #475 verified). However the subagent environment did not have `AskUserQuestion` registered (deferred-tool manifest excluded it; keyword search returned no match), and the background gradle task was then killed externally before any rubric step could be exercised. All `[?]` rows below remain unchanged. Re-run from a session that has `AskUserQuestion` available, or have the parent agent drive the rubric directly using the runelite-dev MCP screenshot tools.

> **2026-05-20 attempt 2 — partial pass.** Parent session drove the rubric directly via `AskUserQuestion` (subagent attempts hit the same blocker as attempt 1; pattern now encoded in memory). Full Pass 2 results recorded at [`docs/audit/pass2-validation-results.md`](audit/pass2-validation-results.md). ~14 of ~50 rubric rows exercised. **5 regressions filed** (#574 GWD step-order cascade, #575 panel text truncation, #576 STOP-button state desync, plus re-open requests on #486 + #487). **2 enhancements filed** (#573 Recommended UX + wiki strategy link, #577 F1/F2 polish). Phase 5 B.5.x rows green on General Graardor. Phase 2 B2 + Phase 4 F1/F2 invocation + C7 C1-C5 retest carry over to a future session.

### Phase 1 — Smoke (run first)

Use the existing "Regression smoke test" section near the end of this file. Do not proceed to later phases if any smoke row fails.

### Phase 2 — New dispatch paths (highest regression risk)

#### PR #473 — B5 puzzle/dynamic step type (Wintertodt pilot) *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on Wintertodt → step description loads, hint arrow points at an active brazier | `[?]` | |
| 2 | A brazier breaks mid-round → next tick, hint arrow / minimap arrow retargets to the next-nearest active brazier | `[?]` | |
| 3 | All braziers active simultaneously → arrow points at nearest active | `[?]` | |
| 4 | Round ends (subgame between waves) → no NPE / ConcurrentModification in `client.log` from `WintertodtBrazierEvaluator` or `GuidanceOverlayCoordinator.tick` | `[?]` | |

#### PR #474 — B6 Java helper pilot (Cerberus) *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on Cerberus → sequence loads; step list matches JSON baseline (no missing or duplicated steps) | `[?]` | |
| 2 | Walk through ghost phase → step transitions trigger correctly | `[?]` | |
| 3 | Stop and restart guidance on Cerberus → clean cycle, no orphaned overlays | `[?]` | |
| 4 | Activate any non-Cerberus source (e.g. Vorkath) → JSON sequencer still drives; helper does not incorrectly intercept | `[?]` | |

#### PR #456 — B2 tile-sequence pathing *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with a `waypoints` step (check `drop_rates.json` for `"waypoints"` — if no source backfilled yet, mark N/A) | `[?]` | |
| 2 | Walk across each waypoint in order → step completes only after the last waypoint is crossed | `[?]` | |
| 3 | Teleport past a waypoint (skipping it) → step does not auto-complete | `[?]` | |

#### PR #455 — B3 nested conditional steps *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with nested `conditionalAlternatives` (search `drop_rates.json` for nesting) | `[?]` | |
| 2 | Player state matches nested branch → step text/target reflects the override | `[?]` | |
| 3 | Player state changes mid-step → branch re-evaluates without restart | `[?]` | |

### Phase 3 — Player-aware state (validate C1–C5 via the C7 debug overlay)

#### PR #459 — C7 player-capability debug overlay *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Enable **Show Player Capability Debug Overlay** in config → overlay renders on the game screen | `[x]` | 2026-05-16: overlay renders with basic stats block |
| 2 | Overlay shows: equipped items count, diary tiers per region, POH teleports detected, skill-cape perks detected, partial-quest state | `[!]` | 2026-05-16: **incomplete** — only basic stats (combat/skills/spellbook/prayer/task/quests/POH-yes-no). Missing C1 teleport inventory, C2 equipped, C3 diary per region, C4 cape perks, C5 sub-milestones. Filed #486. 2026-05-20: **re-confirmed broken** despite #486 close — C1 still "(none detected)", C2 still "0", C3 region labels render without completion data. Re-open request added on #486. |
| 3 | Toggle off → overlay disappears cleanly, no orphan render | `[?]` | not tested |

#### PR #461 — C3 diary tier state *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | C7 overlay → diary state per region matches your actual diary completions (cross-check Achievement Diary tab in-game) | `[-]` | 2026-05-16: blocked — C7 does not surface diary state (#486) |
| 2 | Region with no diary completed → all tiers show false | `[-]` | 2026-05-16: blocked — same as above (#486) |

#### PR #462 — C5 partial-quest state *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Pick a mid-progress quest you've started but not completed → C7 overlay shows the correct partial state | `[!]` | 2026-05-16: overlay shows "Quests done: 207" but user has 179/179 in-game. Filed #487. Sub-milestone state not exposed (blocked by #486). 2026-05-20: re-confirmed — label renamed to "Quest entries: 207" but same wrong count. |

#### PR #463 — C2 equipped-item state *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | C7 overlay → equipped-item count matches your current equipment | `[-]` | 2026-05-16: blocked — overlay does not surface equipped items (#486) |
| 2 | Equip a teleport-enabling item (Crafting cape, Max cape, Ring of Wealth) → appears in state on next tick | `[-]` | 2026-05-16: blocked — same as above (#486) |
| 3 | Unequip → removed from state | `[-]` | 2026-05-16: blocked — same as above (#486) |

#### PR #470 — C1 POH teleport inventory *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | C7 overlay → POH teleports detected match what you actually have built in your house | `[-]` | 2026-05-16: blocked — overlay shows "POH: yes" only, no teleport inventory (#486) |
| 2 | Add a manual override in config → appears as enabled in overlay | `[/]` | 2026-05-16: 3 manual overrides exist (Mounted Glory, Spirit Tree, Fairy Ring) — toggling not verified via overlay (#486) |
| 3 | Remove the manual override → state reverts to varbit-only | `[-]` | 2026-05-16: blocked — same as above (#486) |

#### PR #471 — C4 skill-cape perk state *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | C7 overlay → skill cape perks match capes you own / skills at 99 | `[-]` | 2026-05-16: blocked — overlay does not surface skill cape perks (#486) |
| 2 | Equip a different skill cape → state updates on next tick | `[-]` | 2026-05-16: blocked — same as above (#486) |

### Phase 4 — External I/O

#### PR #465 — F1 collectionlog.net profile import *(merged 2026-05-14, hardened in #476)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Open panel → "Import from collectionlog.net" button visible | `[!]` | 2026-05-16: **wrong widget** — implemented as a config checkbox in the Sync section, not a panel button. User confused whether toggling does anything. Filed #488. |
| 2 | Click import with your RSN → returns success result with N items marked; panel refreshes | `[?]` | 2026-05-16: blocked — no clear action surface to trigger (#488) |
| 3 | Import with a clearly-invalid RSN → returns "user not found" without crashing | `[?]` | 2026-05-16: blocked — same as above (#488) |
| 4 | Disable network (airplane mode) mid-import → returns network-error result without hanging | `[?]` | 2026-05-16: blocked — same as above (#488) |

#### PR #468 — F2 TempleOSRS KC sync *(merged 2026-05-14, hardened in #476)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Panel button "Sync from TempleOSRS" visible | `[!]` | 2026-05-16: same as F1 — implemented as config checkbox, not panel button. Filed #488. |
| 2 | Sync with your RSN → KC values populate for activities TempleOSRS tracks | `[?]` | 2026-05-16: blocked — no clear action surface (#488) |
| 3 | Sync with an invalid RSN → returns failure result cleanly | `[?]` | 2026-05-16: blocked — same as above (#488) |

#### PR #466 — F3 per-account kill-time learning (opt-in) *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Enable **Learn Kill Times** in config | `[x]` | 2026-05-16: config checkbox present under Learning section |
| 2 | Kill a single-target source (Vorkath, Cerberus, any boss) 5 times → debug log shows the rolling window populating | `[?]` | 2026-05-16: not exercised this session |
| 3 | Verify `~/.runelite/profiles2/<profile>/clh/kill_times.json` exists with the entries | `[?]` | 2026-05-16: not exercised this session |
| 4 | Restart client → values reload (verify via debug log line "loaded N sources from") | `[?]` | 2026-05-16: not exercised this session |
| 5 | **KNOWN ISSUE #480** — watch for client-thread stutter on every kill | `[?]` | reference, not fail-blocking |
| 6 | **KNOWN ISSUE #481** — try in a populated Wintertodt; expect poisoned averages | `[?]` | reference, not fail-blocking |

#### PR #469 — F4 dry-streak feed *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Panel includes a "Dry Streak" mode/tab | `[x]` | 2026-05-16: Dry Streaks accordion visible in collection-log-items section |
| 2 | Renders without NPE on a fresh account profile / on your synced account | `[x]` | 2026-05-16: renders empty-state hint "No KC data available. Kill counts will appear here once synced." — no crash. Depends on F2 sync (#488). |
| 3 | Sort modes (by source / dryness class) work | `[?]` | 2026-05-16: blocked — no KC data to sort yet (depends on F2 #488) |

### Phase 5 — UI / panel surface

#### PR #457 — B.5.3 source-level requirements header *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with quest/skill/diary requirements → requirements header renders with green/red coloring | `[x]` | 2026-05-20: General Graardor renders "Troll Stronghold — COMPLETED" + "Strength 70 — level 99/70" both green (player has both). |
| 2 | An unmet requirement renders red; a met one renders green | `[x]` | 2026-05-20: green path verified. Red path not exercised this session (would need a source with an unmet requirement). |

#### PR #458 — B.5.4 collapsible step sections *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with `section` field on its steps → section headers visible | `[x]` | 2026-05-20: Graardor shows "Travel (1 step)" + "Combat (3 steps)" headers. |
| 2 | Active step's section is auto-expanded; non-active sections collapsed | `[x]` | 2026-05-20: per player visual confirmation. |
| 3 | Click a section header → expand/collapse works | `[x]` | 2026-05-20: chevron animates, toggle clean. |

#### PR #472 — B.5.2 recommended items section *(merged 2026-05-14, supersedes #405)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with `recommendedItemIds` → recommended chip strip renders below the required strip | `[x]` | 2026-05-20: Graardor shows 3 chips (Saradomin brew, Prayer potion, Bandos godsword). Note: player suggested elevating Recommended above the step body + per-source wiki strategy link — filed #573 (enhancement, not regression). |
| 2 | Color rules match the required strip (green/white/red) | `[x]` | 2026-05-20: Saradomin brew yellow/bank, Prayer potion green/inv, Bandos godsword white/bank-found. |
| 3 | Hover → tooltip shows item name | `[?]` | not exercised — defer. |

#### PR #464 — E1 cross-source per-item recommendation *(merged 2026-05-14, panel integration deferred)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Enable cross-source mode flag in config | `[-]` | 2026-05-20: **blocked by design** — `enableCrossSourceMode` is `hidden = true` in config until panel integration ships (per CollectionLogHelperConfig.java:332). |
| 2 | No regression to the existing panel modes | `[x]` | 2026-05-20: all 6 modes render (Efficient / Category Focus / Search / Pet Hunt / Statistics / Dry Streaks). |
| 3 | Debug log shows `CrossSourceRanker` running on recalc | `[?]` | not exercised — flag is hidden, can't enable from config UI. |

#### PR #467 — E2 meta-update dating *(merged 2026-05-14)*

| step | description | status | notes |
|---|---|---|---|
| 1 | Activate guidance on a source with `metaUpdateDate` set → `MetaAgeBadge` shows the age | `[-]` | 2026-05-20: **N/A** — zero sources have `metaUpdateDate` field set in `drop_rates.json`. |
| 2 | Source without `metaUpdateDate` → badge hidden, no NPE | `[-]` | N/A — vacuously satisfied. |

### Phase 6 — Build / CI / Infra

| PR | description | status | notes |
|---|---|---|---|
| #475 | `./gradlew run` succeeds on JDK 17 without `InaccessibleObjectException` | `[x]` | verified 2026-05-16 (this session) |
| #454 | `./gradlew build` runs JaCoCo and passes 45% threshold | `[x]` | verified 2026-05-16 (this session) |
| #451 | GitHub Actions runs build+test on push | `[ ]` | verify on GitHub after next push; not in-game testable |
| #476 | Plugin shutDown → no thread leak; sync actions don't double-fire | `[?]` | partly covered in F1/F2 phases above |

#### Earlier PRs with rubric rows already in this log but no "validated" date

Re-run the existing rubric rows for:
- #441 worldmap chevron + CLH-distinct color
- #443 UTF-8 charset fix
- #444 Cyclopes per-defender descriptions
- #445 B4.4 Top Pick auto-advance carries per-item target ID
- #446 duplicate tier label fix
- #448 Tier 3 CHAT_MESSAGE_RECEIVED backfill
- #449 dialog highlight polish
- #452 B.5.1 required-item chips

---

### Related issues filed during pre-validation review (2026-05-16)

- `cha-ndler/collection-log-helper#478` — F1: injected ScheduledExecutorService for CollectionLogNetImporter
- `cha-ndler/collection-log-helper#479` — F2: injected ScheduledExecutorService for TempleOsrsKcSyncer
- `cha-ndler/collection-log-helper#480` — F3: KillTimeTracker.saveToDisk on client thread
- `cha-ndler/collection-log-helper#481` — F3: KillTimeTracker records every NPC death (no ownership check)
- `cha-ndler/collection-log-helper#482` — chore(test): JUnit 5 migration

### Issues filed DURING in-game validation pass (2026-05-16)

- `cha-ndler/collection-log-helper#483` — P1: Hard Treasure Trails panel rendering overlap + step auto-advance failure
- `cha-ndler/collection-log-helper#484` — P2 data fix: wrong fairy ring code AKQ → AIQ for Asgarnian Ice Dungeon sources
- `cha-ndler/collection-log-helper#485` — **P0 HUB BLOCKER**: ARRIVE_AT_TILE auto-advance not firing systemically (reproduces on Royal Titans + Hard Clue)
- `cha-ndler/collection-log-helper#486` — P1: C7 debug overlay incomplete (missing C1-C5 detected state) — blocks Tier C validation
- `cha-ndler/collection-log-helper#487` — P2: C5 "Quests done: 207" vs in-game 179/179
- `cha-ndler/collection-log-helper#488` — **P1 HUB BLOCKER**: F1/F2 sync are config checkboxes, should be panel buttons (privacy-opt-in implications)

### Phase result summary (2026-05-16)

| Phase | What was validated | Result |
|---|---|---|
| **1 — Smoke** | All 6 items attempted | 3 pass, 1 partial (#362 pre-existing slayer/skilling 0/0 bug), 1 fail (#483 Hard Clue), 1 deferred |
| **2 — Dispatch** | Royal Titans (proxy for B6) | Auto-advance failed → escalated to systemic #485. B5 Wintertodt, full B6 Cerberus, B2 waypoints, B3 nested NOT exercised — paused pending #485 fix |
| **3 — Player state** | C7 overlay enabled, contents inspected | #486 + #487 filed. C1/C2/C3/C4/C5 detection layers cannot be validated until #486 fixed |
| **4 — External I/O** | F1, F2, F4 inspected | #488 filed (F1+F2 UX bug). F4 renders correctly, depends on F2 sync working |
| **5 — UI / panel** | Not exercised | Paused — lower priority than fixing P0/P1 blockers first |
| **6 — Build/CI** | Already verified pre-walkthrough | #475, #454 pass; #451 verify on next GitHub push |

### Pause note

Validation paused 2026-05-16 after 6 hub-blocking-or-relevant issues surfaced. Resume after fixes for **#485 (auto-advance)** and **#488 (sync UX)** at minimum — those two alone block sensible re-validation of multiple downstream rubric rows.

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

- `[x]` Sync collection log: open the in-game collection log widget once -> "Synced N items" reminder clears  *(2026-05-16: synced on login automatically; no reminder appeared because counts were already correct from prior session)*
- `[x]` Bank scan: open bank once -> "Open Bank to scan items" reminder clears  *(2026-05-16: no reminder appeared; bank scan cache restored from prior session)*
- `[/]` Switch between modes (Efficient / Category / Search / Pet Hunt / Statistics) -> no errors, panel renders for each  *(2026-05-16: all modes render; pre-existing #362 bug still present (Category Focus > Slayer / Skilling show "0/0"); the obtained-items render bug appears resolved)*
- `[!]` Activate guidance on a non-Mort'ton source (Vorkath, Cerberus, Wintertodt, a clue tier) -> panel + overlay render  *(2026-05-16: TWO regressions on Hard Treasure Trails — (a) panel rendering overlaps outside its box; (b) step auto-advance not firing, had to manually progress steps. Screenshot: /tmp/clh-validation-2026-05-16/smoke-4-hard-clue-overlap.png. Needs new issue.)*
- `[?]` Stop guidance, restart guidance on the same source -> clean cycle, no orphaned overlays or InfoBoxes  *(2026-05-16: not yet tested; smoke 4 already failed)*
- `[?]` Close client, reopen -> bank scan cache restores correctly per RS profile (verify by re-opening Helper panel without re-banking)  *(2026-05-16: implicitly partly verified by Smoke 1 + Smoke 2 cache-restore behavior on login)*

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
