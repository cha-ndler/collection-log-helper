# v1 Plugin-Hub Validation Checklist (prioritized subset)

> **Purpose**: The hub-gating subset extracted from `in-game-validation-log.md`. That file has 338 rubric rows (294 not-yet-`[x]`); most are sub-tests inside PRs whose primary behaviour is already covered. **This file is the ~27-row set that actually gates v1 Plugin-Hub submission.** Clear every `[ ]` here before resubmit.
>
> **Related**: `plugin-hub-resubmission.md` (reviewer-feedback + code-quality tracker) · `in-game-validation-log.md` (full rubric, source of these rows).
>
> **Status legend**: `[ ]` required/not-done · `[x]` cleared · `[!]` FAIL (fix first) · `[-]` blocked on conditions
>
> **Last pass**: 2026-05-27 (Wave 1, HEAD `48134711`). Rows marked `[x] 05-27` were cleared that session.
>
> **2026-06-04 auto-validation sweep** (HEAD `94756851`): T3.1 fixed code-side (PR #731), T1.1 + T1.5 annotated as code-complete. No open code/data FAILs remain; the rest is in-game-only — see `post-merge-ux-confirmation.md`.

---

## Tier 0 — Instant-rejection gates (must ALL be green)

Plugin-Hub reviewers reject on any console exception, login crash, or dead core feature. Non-negotiable.

- [x] **T0.1** `./gradlew run` launches clean; sidebar icon appears *(05-27)*
- [x] **T0.2** Login on populated account → reaches game world, no `scripts are not reentrant` / no hang (#677) *(05-27)*
- [x] **T0.3** All 6 panel modes render (Efficient/Category/Search/Pet Hunt/Statistics/Dry Streaks) *(05-27)*
- [x] **T0.4** Guide Me on a non-Mort'ton source → panel + overlay both render, state in sync *(05-27)*
- [ ] **T0.5** 5+ min of normal play with guidance active → ZERO CLH stack traces in `client.log` *(partial 05-27: clean while AFK; needs an active-combat session)*
- [ ] **T0.6** Stop → restart guidance on same source → clean cycle, no orphaned overlays/InfoBoxes
- [ ] **T0.7** Privacy: confirm no RSN / account hash / coordinates written to disk or logs during a full session

## Tier 1 — Hub-blocker regression confirms (closed issues → confirm green on master)

Each was an explicit hub blocker. Issue is CLOSED; needs one clean confirm on current master.

- [~] **T1.1 (#485 class)** ARRIVE_AT_TILE auto-advance on a travel source. The 05-27 Phosani's FAIL was an isolated teleport/shortcut data gap (now fixed code-side by PR #731, see T3.1). Still need a clean in-game confirm on a *walk-in* travel source that the systemic #485 fix holds — in-game-only, tracked in `post-merge-ux-confirmation.md`.
- [ ] **T1.2 (#483)** Hard Treasure Trails → panel renders inside its box (no overlap) AND steps auto-advance *(needs a hard clue)*
- [x] **T1.3 (#486/#487/#611)** C7 debug overlay surfaces C1-C5 detected state + correct quest count *(05-27)*
- [~] **T1.4 (#488/#577)** collectionlog.net + TempleOSRS sync are usable action surfaces with clear opt-in; sync populates data, invalid RSN fails cleanly, offline fails without hanging. **OPT-IN HARDENED CODE-SIDE (2026-06-10, PR #795)**: both sync flags now default OFF, their config descriptions state the request "submits your RSN to a third-party server", and the consent flag is re-checked immediately before each outbound request (`CollectionLogNetImporter.doImport` / `TempleOsrsKcSyncer.doSync`) so the RSN cannot leave the client while disabled. Remaining sub-clauses (sync populates data, invalid RSN fails cleanly, offline fails without hanging) are live-play only — tracked in `post-merge-ux-confirmation.md` / `needs-human.md`.
- [~] **T1.5 (#484)** Asgarnian Ice Dungeon sources use fairy ring AIQ (not AKQ) → guidance points to the right ring. **DATA CONFIRMED CORRECT (2026-06-04)**: all Asgarnian Ice Dungeon entries in `drop_rates.json` already read `AIQ` (the Mudskipper Point ring); the `AKQ` occurrences are correctly the Piscatoris/Kraken sources. No code change needed. In-game "ring points right" confirm is the only remaining step — tracked in `post-merge-ux-confirmation.md`.

## Tier 2 — #699–#727 UX wave (never recorded; core advertised guidance UX)

The largest unvalidated block. Panel-side rows cleared 05-27; active-play rows still required.

- [x] **T2.1 (#699)** Panel header: count overlaid on progress bar, Synced indicator, no manual sync buttons *(05-27)*
- [x] **T2.2 (#457/#401)** Requirements header renders with green/red quest-skill-diary rows *(05-27)*
- [x] **T2.3 (#458/#402)** Collapsible step sections; active section auto-expands *(05-27)*
- [x] **T2.4 (#705)** "Items needed" color-coded text list; palette green/gold/red correct *(05-27)*
- [x] **T2.5 (#443)** UTF-8 overlay text (no mojibake on em-dash/apostrophe) *(05-27 incidental)*
- [ ] **T2.6 (#472)** Recommended chip strip renders below required, with bank-scan colors *(needs a step carrying `recommendedItemIds`)*
- [ ] **T2.7 (#710/#711)** "(from activity)" on-site tag + recommended-as-aids on Shades / Brimhaven / Guardians / Tithe Farm
- [ ] **T2.8 (#713/#718)** Guidance auto-stops when target clog item obtained, but does NOT stop mid-loop on a looping source *(needs obtaining a target item)*
- [ ] **T2.9 (#719/#721)** Depleted looping activity resets to the restock step; re-banking clears the latch *(needs Shades catacomb key/remains depletion)*
- [ ] **T2.10 (#714/#715/#720)** Object glow clean (thin, no stacked labels); CLH target marker is the real clog book sprite floating above target *(needs target NPC/object in render distance)*
- [ ] **T2.11 (#725)** Recurring/subsequent-step item highlight stays active past the first pickup
- [ ] **T2.12 (#726)** In-game overlay item-availability matches the side panel (no overlay-vs-panel divergence)
- [ ] **T2.13 (#727)** Activating mid-activity inside a confirmable later-step area + holding its items → jumps forward to that step *(positive case; negative case already confirmed 05-27)*
- [ ] **T2.14 (#712/#723)** Swordfish + Aerial Fishing corrected recommended items show right data (no Saradomin halo / no pearl-rod typo)

## Tier 3 — Open FAIL: fix then revalidate

- [~] **T3.1** Phosani's Nightmare step 1 never auto-advanced on Drakan's-medallion / shortcut entry to Slepe. **CODE-COMPLETE — pending in-game confirm (PR #731, merged 2026-06-04).** Root cause was a data/design gap (tight `ARRIVE_AT_TILE` r=20 can't catch teleport/shortcut landing >20 tiles away); predicate code was always correct. Fix converted step 1 on BOTH "The Nightmare" and "Phosani's Nightmare" to `ARRIVE_AT_ZONE [3700,3290,3770,3360,0]` over Slepe town — the zone contains the medallion landing (3730,3340), the church-stairs tile (3728,3302), and the northern walk-in, while the overlay/hint-arrow still anchors at the stairs. Verified out-of-game: `./gradlew build`+`test` green (1752 tests), `validate_drop_rates` + `data_ascii_lint` + `commit_message_lint` clean. In-game auto-advance behaviour is non-blocking and recorded in `post-merge-ux-confirmation.md`. Systemic audit of other teleport/shortcut-entry travel steps (Ring-of-Shadows DT2 bosses Duke/Leviathan/Whisperer/Vardorvis r=20, ToB Drakan's-medallion entry r=15) captured in the PR #731 body as follow-up iterations.

---

## Tally

| Tier | Total | Done | Still required |
|---|---|---|---|
| T0 instant-rejection | 7 | 4 | 3 |
| T1 hub-blocker confirms | 5 | 1 | 4 (T1.1 + T1.4 + T1.5 now code-complete, in-game confirm only) |
| T2 #699-#727 UX wave | 14 | 5 | 9 |
| T3 open FAIL | 1 | 1 (code-complete) | 0 blocking |
| **Total** | **27** | **10** | **17** |

**0 open code/data FAILs remain.** T3.1 (the only `[!]`) is fixed code-side by PR #731 and is now `[~]` code-complete pending a non-blocking in-game auto-advance confirm. T1.5 data is confirmed correct (AIQ). Every other still-required row needs an active-play session (combat, a hard clue, a depleting loop, items in render distance) and cannot be advanced out of game. The auto-validatable backlog is therefore empty — see `post-merge-ux-confirmation.md` for the human-facing in-game confirm recipes.
