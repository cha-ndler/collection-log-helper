# Pass 3 In-Game Validation Results — 2026-05-21

> **Scope**: Re-validate the 7 fixes merged after Pass 2 (#578) plus 4 carry-over rows from Pass 2. Master tested: `966a761e`. Issue #495 follow-up.

> **Status legend** (mirrors `pass2-validation-results.md`):
> - `[x]` verified pass
> - `[!]` regression — issue filed
> - `[/]` partial verify — one half tested, other deferred
> - `[?]` inconclusive / skipped this session, carries over

## Session summary

| | |
|---|---|
| Master tested | `966a761e` |
| Client | dev RuneLite via `./gradlew run`, JDK 17 |
| Account | active player `Hanadji` — 180/180 quests, all stats 99, maxed clue progression bucket |
| Rubric rows exercised | 8 of 12 in scope |
| New issues filed | 3 (#598 tier-A, #599 tier-A, #600 P2 tier-B) |
| Issues re-opened | 1 (#575) |
| Re-open queued | 2 (#486, #487) — pending this PR's screenshots |

## Per-fix re-validation

### PR #579 — GWD boss step order (closes #574) — `[x]`

All 4 GWD bosses verified from `drop_rates.json` and side-panel rendering:

| Boss | Step 1 | Step 2 | Step 3 |
|---|---|---|---|
| General Graardor | Travel to GWD | Get 40 Bandos KC | Enter room + kill |
| Kree'arra | Travel to GWD | Get 40 Armadyl KC | Enter room + kill |
| K'ril Tsutsaroth | Travel to GWD | Get 40 Zamorak KC | Enter room + kill |
| Commander Zilyana | Travel to GWD | Get 40 Saradomin KC | Enter room + kill |

Order is **Travel → KC gate → Kill** for all four. KC gate precedes kill (the Pass 2 regression). Step count is 3 each (no duplicate kill step). Matches Nex's reference pattern (which has an extra "frozen door" step for the Ancient Prison).

### PR #580 — side-panel step text wrap (closes #575) — `[!]` → #575 re-opened

PR #580 wraps lines instead of truncating them, but **drops characters at the wrap boundary**, which is arguably worse than the original bug because the missing characters are invisible to the player.

Repro on General Graardor kill step (step 3/3):

- Expected (from data, 176 chars): `"… tank the minions. Kill Sergeant Steelwill first if you want to reduce damage taken"`
- Observed: `"… tank the minions. K Sergeant Steelwill first if you want to reduce damage taken"` — `"ill"` dropped at line wrap.

#575 re-opened with the new char-drop evidence. Suspected: substring math error in `StepProgressView` / `CategorySummaryPanel` wrap logic where the wrap point advances past the natural word boundary by one character before backtracking.

### PR #581 — STOP icon two-way sync (closes #576) — `[x]`

Both directions verified:

| Action | Step-control STOP icon | Source-level Stop Guidance button |
|---|---|---|
| Click step-control STOP | reflects stopped | also reflects stopped ✓ |
| Click source-level Stop | also reflects stopped ✓ | reflects stopped |

Both icons stay in sync regardless of which one initiates the stop. #576 confirmed fixed.

### PR #582 — F1/F2 config labels + tooltips (closes #577) — `[/]` split verdict

**Labels half: `[x]` verified.** Both rows under the `Sync` section now use the standardized pattern `[Service] Sync`:

- `collectionlog.net Sync` (was F1 import)
- `TempleOSRS KC Sync`

**Tooltips half: `[!]` filed #600.** Tooltips exist on hover but render as paragraph-length text that spans the entire screen width. Both tooltips read as documentation copy rather than scannable hints. #600 (P2 / tier-B) filed asking for trimming to single-sentence form (~1 line at typical panel width).

### PR #583 — C7 debug overlay C1-C5 + quest count (closes #486, #487) — `[!]` re-open pending

Same shape of regression as Pass 2 — labels render, detected data does not. Evidence: `docs/audit/screenshots/pass3-c7-overlay-regression.png`.

| Sub-row | Acceptance | Observed | Verdict |
|---|---|---|---|
| C1 POH Teleports | non-empty list | `(none detected)` despite `POH built: yes` line in same overlay | `[!]` |
| C2 Equipped Items | non-zero count | `Items equipped: 0` with full gear worn | `[!]` |
| C3 Diary Tiers | per-region tier values | region labels (Ardougne / Desert / Falador / Fremennik / Kandarin / …) with no tier value beside any | `[!]` |
| C5 Quest count | 179 per #487 acceptance | `Quests: 208/180` — actual in-game is 180/180 | `[!]` |
| C7 toggle off | overlay vanishes cleanly | toggle logged at 20:37:35 EDT, overlay disappeared, no exception | `[x]` |

#486 and #487 to be re-opened with this PR's screenshot as evidence.

### PR #584 — source-level Recommended gear strip + wiki link (closes #573) — `[!]` filed #599

Two problems surfaced. Evidence: `docs/audit/screenshots/pass3-recommended-strip-placement.png`.

1. **Placement: PR #584 did not elevate Recommended above the step body.** The strip sits below the step description AND below the section list, not above. The "above step body" promise in #573 is unmet.
2. **Per-step, not source-level.** The Recommended strip is only visible when the *last* (kill) step is the active step. A player preparing for an activity sees no recommendation until they have already begun.

#599 (tier-A) filed with refined ask: hoist Recommended to live directly under the source-level Requirements section so it is always visible from step 1. Supersedes #573 (the original ask was not delivered by #584). Wiki strategy link half of #584 did land and stays as-is.

### PR #589 — KillTimeTracker off-thread + local-player attribution (closes #480, #481) — `[/]` partial

**Off-thread persistence (#480): `[x]`.** Single Gryphon kill at 22:00:31 — no client-thread stutter (user-confirmed), no `"must be called on client thread"` warnings in log, KC update + personal best stored cleanly.

**Local-player attribution (#481): `[?]`.** Untested — solo kill scenario, no other player to filter against. Carry over to a future session where a multi-player kill scenario arises.

### PR #592 — F1/F2 sync executor injection (closes #478, #479) — `[?]`

Covered by F1/F2 invocation rows that did not run this session (see carry-over).

## Carry-over rows from Pass 2

| Row | Status | Reason |
|---|---|---|
| Phase 2 B2 — waypoint advancement (3 sub-rows) | `[?]` | Requires teleport to GWD and physical travel through waypoints + teleport-skip test. Not exercised this session. |
| Phase 4 F1/F2 invocation (4 sub-rows) | `[?]` | Requires real RSN + deliberately-bad RSN. Not exercised this session. Covers PR #592 verification as a side effect. |
| Phase 5 B.5.2 row 3 — Recommended hover tooltip | `[?]` | Folded into #599 acceptance criteria 4 (carries to whichever PR addresses #599). |

## New issues filed this session

| # | Severity | Title |
|---|---|---|
| #598 | tier-A | `fix(guidance): only auto-advance when target collection-log slot unlocks, not on last-step completion` |
| #599 | tier-A | `feat(panel): hoist Recommended strip to source-level Requirements area (supersedes #573, fixes #584 placement regression)` |
| #600 | tier-B (P2) | `fix(config): F1/F2 Sync tooltips are paragraph-length; trim to single sentence` |

**#598** is the most significant new finding. Guidance currently auto-advances to the next collection-log item as soon as the player completes the last guidance step, abandoning the current source mid-grind for any rare/keyed drop (Shades of Mort'ton, Larran's chest, almost all boss drops). Should gate on actual collection-log slot unlock or explicit user stop. Affects the majority of sources — surfaced during the GWD rubric walkthrough.

## Issues re-opened this session

| # | Reason |
|---|---|
| #575 | PR #580 introduced char-drop at wrap boundary. |
| #486 | (queued in this PR's body for re-open) C7 C1/C2/C3 still incomplete despite PR #583. |
| #487 | (queued in this PR's body for re-open) C5 quest count diverges; reads `208/180`, in-game is `180/180`. |

## Recommendation for #495 closure

**Still do not close #495.**

Pass 3 surfaced one new tier-A blocker (#598) and one new tier-A regression-of-intent (#599), and confirmed two prior fixes regressed (#575 char-drop, #486/#487 C7 data). Until at least #598, #599, #575, and #486/#487 are resolved, the v1.0.0-hub tag should remain on hold.

Carry-over rubric for the next session:

- F1/F2 invocation (4 rows) — verifies PR #592 as a side effect
- GWD waypoint advancement (3 rows) — requires GWD travel
- KillTimeTracker attribution half (#481) — requires multi-player kill scenario
- Recommended hover tooltip — folded into #599
- Plus the regressions from this pass once their respective fixes land

## How to use this PR

The doc is the canonical record of Pass 3. Future validation sessions should pick up from the `[?]` rows above. The new tier-A blockers (#598, #599) and the re-opened #575 / #486 / #487 are the gating set for `v1.0.0-hub`.
