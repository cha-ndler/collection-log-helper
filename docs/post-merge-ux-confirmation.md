# Post-Merge In-Game UX Confirmation

> **Purpose**: The human-facing deliverable of the hub auto-validation loop. Each row is a change that was implemented and validated **out of game** (unit tests + data linters) but whose *player-visible behaviour* still needs one eyeball in the live OSRS client before its hub-checklist row can flip from `[~]` (code-complete) to `[x]` (verified).
>
> This file never gates the loop — it accumulates so a single in-game session can clear several rows at once.
>
> **How to use**: `git pull` to the merged `master`, `./gradlew run`, then walk each recipe. Mark the checklist row in `v1-hub-validation-checklist.md` (and the matching row in `in-game-validation-log.md`) when it passes.
>
> **Status legend**: `[ ]` not yet eyeballed · `[x]` confirmed in game · `[!]` regression (file an issue, link it).

---

> **2026-06-10 session**: every recipe below is now confirmed — Slepe zone fires for both
> the medallion landing (06-08 receipt) and the walk-in path (synthetic north-edge tile +
> a real on-foot ARRIVE_AT_ZONE on another source), overlay anchor unchanged, log clean,
> and the AIQ ring text confirmed on the live overlay. The matching checklist rows
> (T1.1 / T1.5 / T3.1 / T0.5) are flipped `[x]` in `v1-hub-validation-checklist.md`;
> receipts in `guidance-audit/acceptance-runs/2026-06-10.md`.

## 2026-06-04 — PR #731 (T3.1 / T1.1): Slepe arrival now `ARRIVE_AT_ZONE`

**What changed**: Step 1 of both **The Nightmare** and **Phosani's Nightmare** travel sources changed from `ARRIVE_AT_TILE` (church stairs 3728,3302, radius 20) to `ARRIVE_AT_ZONE [3700,3290,3770,3360,0]` covering Slepe town. The overlay/hint-arrow still anchors at the church stairs; only the *auto-advance trigger* widened.

| # | Source | Do this | "Correct" looks like | Confirms |
|---|--------|---------|----------------------|----------|
| 1 | Phosani's Nightmare | With Drakan's medallion equipped (Sins of the Father complete), stand far from Slepe, click **Guide Me** -> step 1/4. Teleport via the medallion to Slepe. | Step auto-advances from 1/4 to 2/4 ("Climb down the stairs...") within ~1 tick of landing, **without** clicking Skip. The whole chain no longer freezes. | T3.1, T1.1 (#485 class) |
| 2 | The Nightmare | Same as #1 but on the group-boss source (it shares the identical Slepe step). | Step 1 auto-advances on medallion landing; chain proceeds. | T3.1 |
| 3 | Phosani's / The Nightmare | Approach Slepe on foot (Kharyrll teleport or fairy ring CKS to Canifis, run south through Mort'ton) instead of the medallion. | Step 1 still auto-advances as you enter Slepe town from the north — the wider zone must not have broken the walk-in path. | T1.1 walk-in confirm |
| 4 | Phosani's Nightmare | After step 1 auto-advances, glance at the in-game hint arrow / tile marker. | Marker still sits on the **church stairs** (3728,3302), not floating in the middle of the zone. The visual target is unchanged. | T3.1 (no overlay regression) |
| 5 | Either source | Open `client.log` after the run. | Zero CLH stack traces around the Slepe transition; no `ARRIVE_AT_ZONE` / completion errors. | T0.5 (incidental) |

> If step 1 still fails to auto-advance on medallion landing, re-open the T3.1 reproducer in `in-game-validation-log.md` and file a regression against PR #731.

---

## 2026-06-04 — Data-confirmed, in-game eyeball only

These had **no code change** this sweep (data already correct on `master`); they just need a one-time visual confirm.

| # | Source | Do this | "Correct" looks like | Confirms |
|---|--------|---------|----------------------|----------|
| 6 | Asgarnian Ice Dungeon sources (Skeletal Wyvern + related entries) | Click **Guide Me** -> read the travel step / travelTip. | The fairy-ring code shown is **AIQ** (Mudskipper Point, then run south), **never AKQ** (which is Piscatoris). | T1.5 (#484) |
