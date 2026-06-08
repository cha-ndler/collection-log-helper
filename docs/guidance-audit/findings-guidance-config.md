# Guidance-correctness findings — ranked backlog

Produced by `scripts/audit_guidance_config.py` (data) + a guidance-engine reading pass (code).
Baseline: `master` @ `3015c35d`. Each finding cites its source location and the engine rule it
violates. **CONFIRMED** = re-found a known issue; **NEW** = surfaced by this audit.

Fix protocol: one finding (or one coherent class) per PR, branched off `master`, validated with
`./gradlew build` + `validate_drop_rates`, regression test in the same PR, never auto-merged.
Player-facing fixes add an `in-game-validation-log.md` row for a human to tick — the live-game
check is not automatable.

---

## CONFIRMED (re-found known issues)

### C1 — #739/A · 5 `ACTOR_DEATH` steps with no NPC id → never auto-advance · MEDIUM · DATA
`ACTOR_DEATH` is satisfied only via `step.matchesCompletionNpc(npcId)`
(`CompletionChecker.java:131-132`), which needs `completionNpcId > 0` or a non-empty
`completionNpcIds`. These 5 steps have neither, so a kill never advances them:

| Source | step (0-based) | Description gist |
|--------|---------------|------------------|
| Champion's Challenge | step[2] | kill the champion |
| My Notes | step[1] | — |
| Stronghold of Security | step[1] | "Kill monsters on each floor…" (multiple NPCs → needs `completionNpcIds`) |
| Catacombs of Kourend | step[3] | — |
| TzHaar | step[1] | "Kill TzHaar-Ket/Mej/Xil…" (multiple NPCs → needs `completionNpcIds`) |

**Fix:** add `completionNpcIds` (cache-verified ids) for the multi-NPC cases, `completionNpcId`
for single. If a kill is not really the gate, change the condition. IDs from the abextm cache
via `npc_lookup`; gate each through the domain-skeptic before committing.

**Per-source analysis (2026-06-08) — this class needs a maintainer INTENT decision, not a blind
id backfill.** Drilling into all 5 revealed they do NOT share one fix:

- **TzHaar** step[1] — "Kill TzHaar-Ket/Mej/Xil in the city." Clog items are the obsidian
  equipment, which drop from the city TzHaar. Cache-verified city ids ready to apply IF the
  complete-on-kill option is chosen: Ket `2173-2179,2186`; Xil `2167-2172`; Mej `2154-2160`;
  Hur `2161-2166` (Hur drops the obsidian rings but is omitted from the step text — include-or-not
  is a call). The Fight Cave / Inferno `Jal-*` variants are deliberately excluded.
- **Stronghold of Security** step[1] — "Kill monsters on each floor for skull sceptre pieces"
  spans 4 floors (minotaurs, flesh crawlers, catablepon, ankou). Large multi-id list; each
  floor monster needs a cache lookup.
- **My Notes** step[1] — "**rummage** barbarian skeletons for ancient pages." This is a *rummage*
  action, not a kill — `ACTOR_DEATH` is the wrong condition entirely. Likely `MANUAL` or an
  item/chat completion.
- **Catacombs of Kourend** step[3] — "dark totem pieces 1/500 from **any** Catacombs monster."
  Not practically enumerable as `completionNpcIds`; wants a different completion model.
- **Champion's Challenge** step[2] — the clog items (champion scrolls) drop from the regular
  monsters in step[0], NOT from the arena champion. The arena kill is not the clog gate.

**The intent question (applies to TzHaar + Stronghold too):** these are terminal farming steps.
`ACTOR_DEATH` + ids makes guidance *complete after one kill* (the General Graardor pattern,
`CompletionChecker.java:131`). The alternative is that they are meant to be *persistent* farming
steps that intentionally never auto-advance — in which case the fix is to change the condition,
not add ids. **Routed to the maintainer to decide complete-on-kill vs persistent-step before any
data edit** — the analyzer flags the defect; the resolution is a judgment call, not statically
decidable.

### C2 — #739/B · 23 `loopBackToStep` steps with `loopCount` 0/absent → loop never engages · MEDIUM · DATA
A loop runs only when `loopBackToStep > 0 && loopCount > 0` (`StepAdvancer.java:135-137`). All 23
set `loopBackToStep` but leave `loopCount` 0/absent. Full list (source / step / lbs / lc):
Aerial Fishing s2 (3/0), Barbarian Assault s2 (1/0), Brimhaven Agility Arena s2 (1/0),
Castle Wars s2 (1/0), Cutting Squid s2 (3/0), Deep Sea Fishing s2 (3/0), Fishing (Swordfish) s2
(3/0), Fishing Trawler s2 (2/0), Gnome Restaurant (Scarfs) s4 (1/0), Gnome Restaurant (Seed Pods)
s4 (1/0), Last Man Standing s2 (1/0), Mage Training Arena s2 (1/0), Mining (Gemstone Rocks) s2
(3/0), Motherlode Mine s3 (3/0), Pest Control s2 (1/0), Pyramid Plunder s3 (1/absent),
Rogues' Den s3 (1/0), Soul Wars s2 (1/0), Temple Trekking s4 (1/0), Tithe Farm s2 (2/0),
Underwater Crabs s3 (4/0), Vale Totems s3 (1/0), Woodcutting (Teak Trees) s2 (3/0).

**Per-source judgment required (do NOT blanket-set `loopCount`).** Most are *self-loops*
(`loopBackToStep` points at the step itself) where the step is a "repeat the activity" step that
only auto-completes on `ITEM_OBTAINED` anyway — for those the stray `loopBackToStep` is dead
config and should be **removed**. Where the loop is meant to re-run an earlier sub-sequence
(e.g. Gnome Restaurant s4 → s1, Motherlode s3 → s3-relative), set a real `loopCount`. Decide
each against the step semantics.

### C3 — #729 · Shades of Mort'ton step[2] `MANUAL` → never auto-advances · MEDIUM · DATA
Step "Pick up your shade key…" is `MANUAL` but already enumerates every shade-key id in
`groundItemIds` + `skipIfHasAnyItemIds`, so "player now holds a key" is a known completion signal
that just isn't wired. The flat `completionItemId` is a single int and cannot express "any of
~25 keys". **Fix:** wire an any-of-items completion (B1 `conditionTree` OR-of-`INVENTORY_HAS_ITEM`,
or a new completion mode), then drop `MANUAL`. This is the only D2 finding with a concrete
unwired item signal — the other 51 MANUAL "kill/loot" MEDIUM steps use `npcId` for overlay
highlight only and are by-design (see "Not a fix backlog").

### C4 — #737 · `tilePointCache` never reset → stale-tile auto-advance after source switch · HIGH · CODE
`GuidanceSequencer.tilePointCache` (`GuidanceSequencer.java:79`) is keyed by step-index only
(`CompletionChecker.java:243`) and is **not** reset in `startSequence` (172-182),
`restartFromStep0` (323-346), `syncToCurrentState` (364-402), or `stopSequence` (211-223). A new
source / Reset / Sync whose step N is also `ARRIVE_AT_TILE` reuses the prior tile cached for
index N — the player stands on the correct tile but the check compares against a stale one. The
#737 report scopes this to `startSequence`; it leaks across 3 more entry points.
**Fix:** `tilePointCache = null;` in all four. (Sibling `chatPatternCache` shares the omission but
is keyed by pattern *string* so it self-heals — benign; null it for symmetry only.)

### C5 — #738 · `PlayerQuestProgressState.refresh()` rebuilds + logs the full quest map ~2×/s · MEDIUM · CODE
`refresh()` stores then **unconditionally** `log.debug("…{}", next)` the whole map
(`PlayerQuestProgressState.java:137-138`), driven by `onVarbitChanged` (`:158`). Coalesced to
1/tick, but any quest varbit ticking re-logs the identical map every tick, dominating
`client.log`. **Fix:** guard store+log behind `if (!next.equals(snapshot))`.

---

## NEW (surfaced by this audit)

### N1 — 5 `ARRIVE_AT_ZONE` steps missing `completionZone` → never auto-advance · MEDIUM · DATA
Same "can never fire" class as C1 but a different condition, and **not** covered by #739.
`getZone()` returns null without a length-5 `completionZone`, so `isArriveAtZoneSatisfying`
always returns false (`CompletionChecker.java:162-171`). All 5 are minigame "enter the arena"
steps that *do* have a `worldX`:

| Source | step | Description gist |
|--------|------|------------------|
| Pest Control | step[1] | "Board the lander…" |
| Castle Wars | step[1] | "Enter a team portal…" |
| Soul Wars | step[1] | "…enter the Soul Wars portal…" |
| Last Man Standing | step[1] | "Enter the LMS lobby…" |
| Barbarian Assault | step[1] | "Talk to Captain Cain…" |

**Fix per source:** either add the correct `completionZone` (arena bounds), or switch the
condition to `ARRIVE_AT_TILE` (the `worldX/worldY` are already present) where a single tile is
the right gate. Zone bounds need wiki/coordinate verification. (These 5 sources also appear in
C2 — fixing both in the same per-source PR is natural.)

**Per-source investigation (2026-06-08) — NOT a "box the existing coord" fix; routed to the
maintainer.** All 5 centers verify correct via `coordinate_helper` (Void Knights' Outpost,
Castle Wars, Isle of Souls, Ferox Enclave, Barbarian Outpost). But two findings make a blind
zone-around-the-coord fix wrong:

- **`ARRIVE_AT_TILE` is the wrong direction.** The #485-class FAIL already logged in
  `in-game-validation-log.md` (Phosani's `ARRIVE_AT_TILE` r=20 never fired on teleport/shortcut
  entry) shows a tile gate can't catch teleport landings — and players *teleport* into
  minigames. So a zone is correct; the author's `ARRIVE_AT_ZONE` choice was right.
- **But the zone is not a box around `worldX/worldY`.** Pest Control step[1] is "*board the
  lander*" — its coord is the lander/outpost where the player **already stands** when the step
  activates, so a zone there completes immediately. The real "arrived" target is the Pest
  Control **island** (separate, effectively instanced coords), which the data does not carry.
- **Barbarian Assault step[1] is a condition-type mismatch**, not a missing zone: "*Talk to
  Captain Cain*" should be `NPC_TALKED_TO`, not `ARRIVE_AT_ZONE`.

Each of the 5 needs a per-step "what does *arrived* mean here" decision plus, for the
transport cases, the destination (instanced) coordinates — a judgment + in-game-coordinate task,
not statically decidable. **Routed to the maintainer.**

### N2 — `PohTeleportInventoryImpl` unconditional `log.debug` per varbit · MEDIUM · CODE
`onVarbitChanged → refresh()` (`PohTeleportInventoryImpl.java:182`) logs 6 formatted booleans on
**every** varbit with no coalescing and no equality guard (`:196-200`) — the #738 log-spam class,
firing even more often. **Fix:** compute a `changed` flag over the 6 booleans; store+log only on
change (mirror `SlayerTaskState`, which guards correctly).

### N3 — minor per-event recompute (perf only, no log spam) · LOW · CODE
- `SkillCapePerkStateImpl.onStatChanged` (`:157-161`) rebuilds the full perk map on **every XP
  drop** (very hot in combat/skilling); perks only change at level 99. Fix: early-return unless
  the skill crosses a 99 boundary.
- `DiaryTierStateImpl.onVarbitChanged` (`:254-258`) rebuilds all 48 diary varbits per varbit
  event, no coalescing. Cheap reads; optional coalesce.
- `SlayerTaskState.refresh` (`:92`) runs `resolveTaskName()` (DB lookup) per varbit even when
  unchanged. Log is correctly guarded; only the resolve is mild waste.

---

## Not a fix backlog (audited, by-design)

- **51 `MANUAL` non-final steps with `npcId`/`objectId` only** (D2 MEDIUM minus Shades): the id is
  for overlay highlight; completion is the clog item on a later step. By-design. Confirmed: only
  1 of 52 carries an unwired item signal (#729). Do not mass-convert.
- **122 `MANUAL` "loot the chest / hop worlds" steps** (D2 LOW): terminal-ish flavor steps with no
  auto-completion signal; intentionally manual. Advisory only.
- **3 `loopCount` set with `loopBackToStep` 0** (D1b LOW): dead `loopCount`, cosmetic; remove for
  tidiness, not a player-facing bug.

---

## Status (2026-06-08 autonomous pass)

**Shipped as PRs (code/test, statically verifiable, none merged — awaiting review):**

- **C4** (#737 tilePointCache reset) — PR #770, regression test included.
- **C5** (#738 quest-log guard) — PR #771.
- **N2** (PohTeleport log guard) — PR #772.
- **N3** (SkillCapePerk XP-only refresh skip) — PR #774.
- **Phase 5 ratchet** — PR #773: JUnit `GuidanceConfigInvariantsRegressionTest` locks every
  invariant above in CI (hard-zero guards + ceilings 5/23/5 that may only shrink). The analyzer
  + this doc are PR #769.

**Routed to the maintainer (needs a judgment/intent decision or in-game coordinates — NOT
statically decidable, so deliberately not guessed):**

- **C1** (5 ACTOR_DEATH) — complete-on-kill vs persistent-step; 3 of 5 also have a wrong
  condition (rummage / any-monster / wrong-step). TzHaar city ids staged above.
- **N1** (5 ARRIVE_AT_ZONE) — per-step "what does *arrived* mean" + instanced destination
  coords; Barbarian Assault is a condition-type mismatch. See the per-source investigation above.
- **C2** (23 loop steps) — per source: was looping intended (`loopCount`) or vestigial
  (`loopBackToStep` removal)? ~7 are self-loops.
- **C3** (#729 Shades) — needs an any-of-items completion (B1 `conditionTree`), a small engine
  feature + in-game validation.

When a routed item is resolved and lands, decrement the matching ceiling constant in
`GuidanceConfigInvariantsRegressionTest` so the ratchet stays tight.
