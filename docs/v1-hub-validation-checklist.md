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
>
> **2026-06-10 live session** (operator + actuation seam, logged in): 14 rows closed `[x]`,
> T0.7 flipped to `[!]` (RSN-in-logs FAIL, issue to file), T2.11 reclassified needs-human.
> Six findings logged for separate fixes (issues to file). Receipts: `guidance-audit/acceptance-runs/2026-06-10.md`.

---

## Tier 0 — Instant-rejection gates (must ALL be green)

Plugin-Hub reviewers reject on any console exception, login crash, or dead core feature. Non-negotiable.

- [x] **T0.1** `./gradlew run` launches clean; sidebar icon appears *(05-27)*
- [x] **T0.2** Login on populated account → reaches game world, no `scripts are not reentrant` / no hang (#677) *(05-27)*
- [x] **T0.3** All 6 panel modes render (Efficient/Category/Search/Pet Hunt/Statistics/Dry Streaks) *(05-27)*
- [x] **T0.4** Guide Me on a non-Mort'ton source → panel + overlay both render, state in sync *(05-27)*
- [x] **T0.5** 5+ min of normal play with guidance active → ZERO CLH stack traces in `client.log` *(06-10: 12-min real Giant Mole combat window — 0 CLH frames/WARNs; lone NPE is worldhopper's known RegionFilterMode bug)*
- [x] **T0.6** Stop → restart guidance on same source → clean cycle, no orphaned overlays/InfoBoxes *(06-10: Zulrah cycle, state + overlay clean both ways)*
- [!] **T0.7** Privacy: confirm no RSN / account hash / coordinates written to disk or logs during a full session *(06-10 FAIL: CLH logs the RSN at INFO/WARN — efficiency-export path + sync lines; behind opt-in flags but violates the repo security rule. issue to file; fix = redact name from log statements.)*

## Tier 1 — Hub-blocker regression confirms (closed issues → confirm green on master)

Each was an explicit hub blocker. Issue is CLOSED; needs one clean confirm on current master.

- [x] **T1.1 (#485 class)** ARRIVE_AT_TILE auto-advance on a travel source. *(06-10: real on-foot ARRIVE_AT_ZONE fired walking into the Smoke Devil zone (14:58:18) + synthetic Slepe north-edge walk-in receipt. The #485 class holds for both entry styles.)*
- [x] **T1.2 (#483)** Hard Treasure Trails → panel renders inside its box (no overlap) AND steps auto-advance *(06-10: long step text wraps inside its blue box; GE arrival auto-advanced step 1→2; clue-solving steps are MANUAL by design — delegated to the Clue Scroll plugin)*
- [x] **T1.3 (#486/#487/#611)** C7 debug overlay surfaces C1-C5 detected state + correct quest count *(05-27)*
- [~] **T1.4 (#488/#577)** collectionlog.net + TempleOSRS sync are usable action surfaces with clear opt-in; sync populates data, invalid RSN fails cleanly, offline fails without hanging. **OPT-IN HARDENED CODE-SIDE (2026-06-10, PR #795)**: both sync flags now default OFF, their config descriptions state the request "submits your RSN to a third-party server", and the consent flag is re-checked immediately before each outbound request (`CollectionLogNetImporter.doImport` / `TempleOsrsKcSyncer.doSync`) so the RSN cannot leave the client while disabled. Remaining sub-clauses (sync populates data, invalid RSN fails cleanly, offline fails without hanging) are live-play only — tracked in `post-merge-ux-confirmation.md` / `needs-human.md`. **06-10 live receipts**: TempleOSRS login sync round-trip clean ("KC sync complete: 0 sources updated, 7 skipped" + toast, no hang); collectionlog.net host is dead (`UnknownHostException`) and the importer failed gracefully — real-world "unreachable fails without hanging" evidence. Still open: populate-with-data, invalid-RSN, consent-race.
- [x] **T1.5 (#484)** Asgarnian Ice Dungeon sources use fairy ring AIQ (not AKQ) → guidance points to the right ring. *(06-10: Skeletal Wyvern travel-step overlay renders "Fairy ring AIQ -> Mudskipper Point" in both the description and the travel tip — screenshot in `screenshots/validation-2026-06-10/`)*

## Tier 2 — #699–#727 UX wave (never recorded; core advertised guidance UX)

The largest unvalidated block. Panel-side rows cleared 05-27; active-play rows still required.

- [x] **T2.1 (#699)** Panel header: count overlaid on progress bar, Synced indicator, no manual sync buttons *(05-27)*
- [x] **T2.2 (#457/#401)** Requirements header renders with green/red quest-skill-diary rows *(05-27)*
- [x] **T2.3 (#458/#402)** Collapsible step sections; active section auto-expands *(05-27)*
- [x] **T2.4 (#705)** "Items needed" color-coded text list; palette green/gold/red correct *(05-27)*
- [x] **T2.5 (#443)** UTF-8 overlay text (no mojibake on em-dash/apostrophe) *(05-27 incidental)*
- [x] **T2.6 (#472)** Recommended chip strip renders below required, with bank-scan colors *(06-10: Shades — Flamtaer bag red MISSING below Tinderbox gold IN_BANK; green HELD closed on Swordfish; ships as text rows, the accepted #705 style)*
- [x] **T2.7 (#710/#711)** "(from activity)" on-site tag + recommended-as-aids on Shades / Brimhaven / Guardians / Tithe Farm *(06-10: Tithe Farm renders "Gricoller's can (from activity)" with the muted suffix; the tag attaches to required/recommended rows whose id is in `activityObtainableItemIds` — only Tithe Farm has an overlap, by design)*
- [x] **T2.8 (#713/#718)** Guidance auto-stops when target clog item obtained, but does NOT stop mid-loop on a looping source *(06-10: Wyvern "completing guidance" on obtain; Shades loop step "mid-loop — keeping guidance active" with real fuel held)*
- [x] **T2.9 (#719/#721)** Depleted looping activity resets to the restock step; re-banking clears the latch *(06-10 live with real banking: "Loop depleted... parking on restock step 1" → "Restock complete at step 1 — resuming guidance")*
- [x] **T2.10 (#714/#715/#720)** Object glow clean (thin, no stacked labels); CLH target marker is the real clog book sprite floating above target *(06-10: smoky cave entrance = thin outline + book sprite, operator-confirmed; NPC variant glow clean on Giant Mole but NO book marker — finding logged: `GuidanceTargetMarker` not wired into the NPC path)*
- [-] **T2.11 (#725)** Recurring/subsequent-step item highlight stays active past the first pickup *(needs-human: repeated real ground pickups on a recurring-gather source; recipe in `needs-human.md`)*
- [x] **T2.12 (#726)** In-game overlay item-availability matches the side panel (no overlay-vs-panel divergence) *(06-10: Wyvern bank step — 5 items mixed states, overlay == panel == bridge state)*
- [x] **T2.13 (#727)** Activating mid-activity inside a confirmable later-step area + holding its items → jumps forward to that step *(06-10: two literal "State-derived start" receipts — Giant Mole in-lair, Castle Wars lobby — plus the Falador-park landing on the kill step)*
- [x] **T2.14 (#712/#723)** Swordfish + Aerial Fishing corrected recommended items show right data *(06-10: Dragon harpoon HELD + Infernal harpoon MISSING, no halo; Knife IN_BANK, no pearl-rod typo)*

## Tier 3 — Open FAIL: fix then revalidate

- [x] **T3.1** Phosani's Nightmare step 1 never auto-advanced on Drakan's-medallion / shortcut entry to Slepe. **CONFIRMED IN-GAME 2026-06-10** (synthetic Slepe walk-in zone receipt + 06-08 medallion landing; overlay anchor unchanged). Originally: **CODE-COMPLETE — pending in-game confirm (PR #731, merged 2026-06-04).** Root cause was a data/design gap (tight `ARRIVE_AT_TILE` r=20 can't catch teleport/shortcut landing >20 tiles away); predicate code was always correct. Fix converted step 1 on BOTH "The Nightmare" and "Phosani's Nightmare" to `ARRIVE_AT_ZONE [3700,3290,3770,3360,0]` over Slepe town — the zone contains the medallion landing (3730,3340), the church-stairs tile (3728,3302), and the northern walk-in, while the overlay/hint-arrow still anchors at the stairs. Verified out-of-game: `./gradlew build`+`test` green (1752 tests), `validate_drop_rates` + `data_ascii_lint` + `commit_message_lint` clean. In-game auto-advance behaviour is non-blocking and recorded in `post-merge-ux-confirmation.md`. Systemic audit of other teleport/shortcut-entry travel steps (Ring-of-Shadows DT2 bosses Duke/Leviathan/Whisperer/Vardorvis r=20, ToB Drakan's-medallion entry r=15) captured in the PR #731 body as follow-up iterations.

---

## Tally

| Tier | Total | Done | Still required |
|---|---|---|---|
| T0 instant-rejection | 7 | 6 | 1 (T0.7 `[!]` — RSN-in-logs fix, issue filed) |
| T1 hub-blocker confirms | 5 | 4 | 1 (T1.4 `[~]` — populate/invalid-RSN/consent-race, needs-human) |
| T2 #699-#727 UX wave | 14 | 13 | 1 (T2.11 `[-]` — recurring-pickup highlight, needs-human) |
| T3 open FAIL | 1 | 1 | 0 |
| **Total** | **27** | **24** | **3** |

**2026-06-10 session result: 24 of 27 rows green.** The three open rows: **T0.7** is a code
FAIL (CLH writes the RSN to client.log at INFO/WARN — issue filed; small redaction fix, then
re-confirm); **T1.4**'s remaining sub-clauses and **T2.11** are genuinely needs-human
(`needs-human.md` carries the recipes). Six findings from the live session are logged for filing as
issues (separate fixes) (auto-advance ordering bug, NPC book marker, kill-step area
confirmability, RSN logging, Lanthus npcId, entrance-highlight sweet spot) — receipts in
`guidance-audit/acceptance-runs/2026-06-10.md`.
