# In-Game Validation Log

> **Purpose**: Track manual in-game testing for merged PRs. Unit tests, type checks, and CI only prove correctness of pure logic — anything touching player state, overlay rendering, or live game events needs human verification against the OSRS client. This file is the source of truth for "did the fix actually work for real users?"

> **Status legend**:
> - `[ ]` — Not tested yet
> - `[~]` — Testing in progress
> - `[x]` — Verified pass
> - `[!]` — Regression / failed (file a new issue and cross-link)
> - `[?]` — Inconclusive (couldn't reproduce / state-dependent)

> **Last updated**: 2026-05-11

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

## Still-open bugs that closed PR #371 left unfixed

These need fresh validation when a follow-up PR ships the real fix. The "still broken on master" column captures the regression baseline observed on 2026-05-10.

| Issue | Reproducer | Still broken on master? | Fix PR | Verified after fix? |
|---|---|---|---|---|
| #362 | Category Focus -> SLAYER -> count reads "0/0", no progress bar | `[ ]` | — | `[ ]` |
| #362 | Category Focus -> SKILLING -> same "0/0" issue | `[ ]` | — | `[ ]` |
| #363 | Activate guidance -> toggle Show Overlays OFF -> overlay should clear immediately (not require Stop/Start) | `[!]` | fix/363 | `[ ]` |
| #363 | Activate guidance -> toggle Show Hint Arrow OFF -> minimap arrow + in-game tile arrow clear immediately | `[!]` | fix/363 | `[ ]` |
| #364 | Toggle Hide Locked Content ON -> items with unmet quest/skill requirements disappear from list | `[!]` | fix/364 | `[ ]` |
| #373 | Toggle Show Overlays OFF mid-guidance -> guidance session CONTINUES; only overlay rendering stops | `[ ]` | — | `[ ]` |
| #374 | Efficient mode with locked items -> locked items appear at their natural efficiency position (e.g., a ~17-min locked item sits near other ~17-min items), not segregated to the bottom | `[ ]` | — | `[ ]` |
| #378 | Walk away from the active step's tile mid-sequence -> step does NOT regress to a prior step | `[ ]` | — | `[ ]` |
| #381 | Mid-guidance: teleport far away and back -> hint arrow re-renders without a Stop/Start cycle | `[ ]` | — | `[ ]` |

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
