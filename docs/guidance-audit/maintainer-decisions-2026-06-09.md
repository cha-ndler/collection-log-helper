# Guidance-audit — maintainer decisions (2026-06-09)

Supersedes the *decision-request* posture of PR #778. The three findings the analyzer
"routed to the maintainer" (C1, N1, C2) are the **only** items blocking the `v1.0.0-hub`
resubmission board from going GREEN — `plugin_hub_validate` is already 0 critical / 0 high,
`./gradlew build` is clean, and every code finding (C4, C5, N2, N3) merged.

The maintainer has delegated these calls. Each decision below states the resolution, the
defensible default where the call is product-subjective, and the engine fact it rests on.
Execution protocol is unchanged: **one coherent batch per PR, branched off `master`,
domain-skeptic-gated, validated with `./gradlew build` + `validate_drop_rates` +
`guidance_lint`, ratchet ceilings decremented in the same PR, never auto-merged without the
green gate.**

---

## Operating principle (resolves the recurring intent question)

A terminal "kill/gather X repeatedly for drops" step on a **multi-item** clog source must
**not** auto-complete after one event. The engine already has the right idiom for this: a
`MANUAL` highlight-only step whose `completionNpcId(s)`/`objectId` drives the *overlay*, with
the sequence force-completing via `onItemObtained` when the clog slot unlocks
(`GuidanceSequencer.java:496-536`). This is the established by-design pattern behind the 51
highlight-only `MANUAL` steps already in the data. `ACTOR_DEATH` + ids (auto-advance after one
kill) is correct ONLY for a **single-drop gate** (the General Graardor pattern,
`CompletionChecker.java:131`).

This single principle resolves the "complete-on-kill vs persistent" question the analyzer
could not decide statically.

---

## C1 — 5 `ACTOR_DEATH` steps with no NPC id → never auto-advance

**Decision: all 5 → `MANUAL` highlight-only; completion via `onItemObtained`.** Two are a
wrong-mechanic correction, three are persistent multi-item farming steps that resolve to the
same idiom. Where a kill *is* shown to the player, keep cache-verified `completionNpcIds` for
overlay highlight only.

| Source | Resolution | Basis |
|--------|-----------|-------|
| My Notes step[1] | `ACTOR_DEATH` → `MANUAL` (no npc ids) | "**Rummage** barbarian skeletons" is not a kill — `ACTOR_DEATH` is objectively the wrong condition. Ancient pages arrive via `onItemObtained`. |
| Champion's Challenge step[2] | `ACTOR_DEATH` → `MANUAL` | Champion scrolls drop from the **regular** monsters in step[0], not the arena champion. The champion kill is not the clog gate. |
| Catacombs of Kourend step[3] | `ACTOR_DEATH` → `MANUAL` (no ids) | Dark totem pieces are 1/500 from **any** Catacombs monster — not enumerable as `completionNpcIds`. `onItemObtained` is the only correct gate. **This flips the acceptance-gate FAIL.** |
| TzHaar step[1] | `ACTOR_DEATH` → `MANUAL` + `completionNpcIds` (highlight only) | Obsidian equipment is a **multi-item** city-TzHaar farm. City ids (cache-verified, re-confirm via `npc_lookup` before commit): Ket `2173-2179,2186`, Xil `2167-2172`, Mej `2154-2160`, Hur `2161-2166`. Fight Cave/Inferno `Jal-*` excluded. |
| Stronghold of Security step[1] | `ACTOR_DEATH` → `MANUAL` + `completionNpcIds` (highlight only) | Skull-sceptre pieces are a **multi-floor, multi-item** farm (minotaurs/flesh crawlers/catablepon/ankou). Persistent step; `onItemObtained` completes. |

---

## N1 — 5 `ARRIVE_AT_ZONE` steps missing `completionZone` → never auto-advance

`ARRIVE_AT_ZONE` was the right *family* (teleport/portal entry can't be caught by an
`ARRIVE_AT_TILE` radius — see the #485 Phosani's FAIL in `in-game-validation-log.md`), but a
zone boxed around the existing `worldX/worldY` is wrong for the transport cases.

| Source | Resolution | Basis |
|--------|-----------|-------|
| Barbarian Assault step[1] | `ARRIVE_AT_ZONE` → `NPC_TALKED_TO` (Captain Cain, cache id via `npc_lookup`) | "**Talk to** Captain Cain" is a condition-type mismatch, not a missing zone. |
| Castle Wars step[1] | add `completionZone` (arena bounds) | "Enter a team portal" — arena bounds verifiable via `coordinate_helper`/wiki; zone is the correct gate for portal entry. |
| Soul Wars step[1] | add `completionZone` (Isle of Souls arena bounds) | Same portal-entry shape. |
| Last Man Standing step[1] | add `completionZone` (Ferox Enclave LMS lobby bounds) | Same. |
| Pest Control step[1] | `ARRIVE_AT_ZONE` → `MANUAL` (default), instanced-island coords deferred | "Board the lander" — the step's coord is the outpost the player *already stands on*, so a zone there completes immediately. The true "arrived" target is the (effectively instanced) Pest Control island, whose coords the data does not carry → **needs an in-game capture**. Default to `MANUAL` to unblock; add an `in-game-validation-log.md` row to capture island coords and upgrade to `ARRIVE_AT_ZONE` later. |

These 5 sources also carry a C2 earlier-target loop — fix the loop in the **same per-source
edit** (see C2).

---

## C2 — 16 earlier-target `loopBackToStep` with `loopCount` 0/absent → loop never engages

(The 7 *self-loops* are by-design D4 E4 markers — out of scope, locked by `--calibrate`.)

**Decision: remove the inert `loopBackToStep` from all 16 earlier-target steps.** This is a
**behavior-preserving** edit — a loop only runs when `loopBackToStep > 0 && loopCount > 0`
(`StepAdvancer.java:135-137`), so with `loopCount` 0/absent these do nothing at runtime today;
removing them only clears dead config the analyzer flags. The genuine product capability some
of these gesture at — an **open-ended multi-step activity cycle** (e.g. Gnome Restaurant
s4→s1 "deliver, get next order, repeat") — is not expressible with the finite `loopCount`
primitive and is therefore an **engine enhancement**, filed separately, not a data fix.

Affected (11 unique to C2; the other 5 are handled in their N1 per-source PR): Motherlode Mine
s3, Pyramid Plunder s3, Rogues' Den s3, Tithe Farm s2, Vale Totems s3, Gnome Restaurant
(Scarfs) s4, Gnome Restaurant (Seed Pods) s4, Fishing Trawler s2, Brimhaven Agility Arena s2,
Mage Training Arena s2 — plus, in the N1 PR: Barbarian Assault s2, Castle Wars s2, Soul Wars
s2, Last Man Standing s2, Pest Control s2.

Follow-up (not board-blocking): engine enhancement issue — *open-ended multi-step activity
loop* primitive (a loop that repeats steps N..M until `onItemObtained`, no finite count).

---

## Execution batches (non-overlapping → no cross-PR conflicts)

| Batch | Scope | Sources | Greens |
|-------|-------|---------|--------|
| **A** | C1 — `ACTOR_DEATH` → `MANUAL` | Champion's Challenge, My Notes, Stronghold of Security, Catacombs of Kourend, TzHaar | C1 (5) + acceptance FAIL → PASS |
| **B** | N1 condition/zone fix **+** that source's C2 loop removal | Pest Control, Castle Wars, Soul Wars, LMS, Barbarian Assault | N1 (5) + C2 (5 of 16) |
| **C** | C2 — remove inert `loopBackToStep` (remaining) | Motherlode Mine, Pyramid Plunder, Rogues' Den, Tithe Farm, Vale Totems, Gnome Restaurant (Scarfs), Gnome Restaurant (Seed Pods), Fishing Trawler, Brimhaven Agility Arena, Mage Training Arena | C2 (11 of 16) |

Each batch decrements its ratchet ceiling in `GuidanceConfigInvariantsRegressionTest` and adds
`in-game-validation-log.md` rows for the player-facing condition changes. When all three land,
`./gradlew guidanceStatus` should report the board GREEN and the `v1.0.0-hub` quiet-week clock
starts on the final merge.
