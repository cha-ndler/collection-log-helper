# Guidance-audit: consolidated maintainer decision-request (2026-06-08)

> One round-trip. The four open trustworthy findings on the board (C1, N1, C2, C3) are all
> **judgment-bound** — none is statically decidable, so none was guessed. Each section states the
> analyzer flag, the engine rule it cites, why a blind fix is wrong, and the options (each cited to
> an engine line or the wiki). Pick an option per item; the data/code edits then become mechanical
> and land as one focused PR each, decrementing the matching ratchet ceiling in
> `GuidanceConfigInvariantsRegressionTest`.

Source of truth: `docs/guidance-audit/status.md` (run `./gradlew guidanceStatus`). Board reads
**NOT GREEN**: 197/226 corpus-pass, acceptance 4/8 (Catacombs FAIL = C1). This request is what
stands between the board and GREEN. No data was edited in producing it.

The #775 lesson governs throughout: **a static-analyzer flag is not automatically a product bug.**
Two of these four items turned out to be exactly that on inspection — see C2 and C3.

---

## C1 — 5 `ACTOR_DEATH` steps with no completion NPC id

**Flag.** 5 steps use `ACTOR_DEATH` with both `completionNpcId` unset and `completionNpcIds` empty.

**Engine rule.** `CompletionChecker.isNpcDeathSatisfying` (`CompletionChecker.java:128-133`) delegates
to `GuidanceStep.matchesCompletionNpc(npcId)`, which is true only when `npcId == completionNpcId`
(default 0) or `npcId ∈ completionNpcIds`. There is **no "match any death" branch**, so a real
(non-zero) NPC death can never satisfy these steps — the chain freezes there in real play (Catacombs
is the acceptance FAIL).

**Why not statically decidable.** The 5 do **not** share one fix; 3 of them have the *wrong condition*
entirely. The intent question — is the kill the completion gate (complete-on-kill, the
General-Graardor single-kill pattern at `CompletionChecker.java:131`) or is this a *persistent farming
step* meant never to auto-advance? — is a design call. ("Complete-on-kill" = the single-kill
matcher `GuidanceStep.matchesCompletionNpc` driven by `CompletionChecker.isNpcDeathSatisfying`
(`:128-133`), as used by single-kill bosses such as General Graardor.)

| Source | step | Finding | Decision needed |
|--------|------|---------|-----------------|
| TzHaar | s1 | "Kill TzHaar-Ket/Mej/Xil in the city." Clog = obsidian equipment from city TzHaar. | complete-on-kill vs persistent. **If complete-on-kill**, cache-verified city ids are staged (apply via `completionNpcIds`): Ket `2173-2179,2186`; Xil `2167-2172`; Mej `2154-2160`; Hur `2161-2166` (Hur drops obsidian rings but is omitted from the step text — include-or-not is a call). Fight Cave / Inferno `Jal-*` deliberately excluded. |
| Stronghold of Security | s1 | "Kill monsters on each floor for skull sceptre pieces" — 4 floors (minotaurs, flesh crawlers, catablepon, ankou). | complete-on-kill vs persistent. If complete-on-kill, a large 4-floor `completionNpcIds` list (each floor needs a cache lookup). |
| My Notes | s1 | "**Rummage** barbarian skeletons for ancient pages." | **Wrong condition** — a rummage is not a kill. `ACTOR_DEATH` is incorrect; likely `MANUAL` or a chat/item completion. |
| Catacombs of Kourend | s3 | "Dark totem pieces 1/500 from **any** Catacombs monster." | **Not enumerable** as `completionNpcIds`. Wants a different completion model (match-any sentinel, or accept it as a persistent step). |
| Champion's Challenge | s2 | Clog items (champion scrolls) drop from the **regular monsters in s0**, not the arena champion. | **Wrong step** — the arena kill is not the clog gate. The kill-gate, if any, belongs on s0. |

**Options (apply per source).**
- **(A) complete-on-kill** — add `completionNpcId`/`completionNpcIds` (cache-verified, domain-skeptic-gated). Fits TzHaar, Stronghold *if* you want one kill to advance.
- **(B) persistent step** — change the condition so the step intentionally never auto-advances (it is a terminal farming step; `onItemObtained` already completes the sequence when the clog slot unlocks — `GuidanceSequencer.java:496-536`).
- **(C) condition correction** — My Notes (rummage → MANUAL/chat), Champion's Challenge (move/remove the kill gate), Catacombs (match-any or persistent). These are *not* id-backfills.

---

## N1 — 5 `ARRIVE_AT_ZONE` steps missing `completionZone`

**Flag.** 5 minigame "enter the arena" steps use `ARRIVE_AT_ZONE` with no length-5 `completionZone`.

**Engine rule.** `GuidanceStep.getZone()` returns null without a length-5 `completionZone`, so
`CompletionChecker.isArriveAtZoneSatisfying` (`CompletionChecker.java:162-171`) always returns false.

**Why not a "box the existing coord" fix.** All 5 centers verify correct via `coordinate_helper`,
but two facts make a blind zone-around-the-coord wrong:

- **`ARRIVE_AT_TILE` is the wrong direction.** The #485-class FAIL in `in-game-validation-log.md`
  (Phosani's `ARRIVE_AT_TILE` r=20 never fired on teleport/shortcut entry) shows a tile gate can't
  catch teleport landings — and players *teleport* into minigames. A zone is correct; the author's
  `ARRIVE_AT_ZONE` choice was right.
- **The zone is not a box around `worldX/worldY`.** The coord is often where the player *already
  stands* when the step activates (e.g. Pest Control s1 "board the lander" — the lander/outpost),
  so a zone there completes immediately. The real "arrived" target is the (effectively instanced)
  destination the data does not carry.

| Source | step | Decision needed |
|--------|------|-----------------|
| Pest Control | s1 | "Board the lander" — destination is the Pest Control **island** (separate/instanced coords not in data). Need the island bounds. |
| Castle Wars | s1 | "Enter a team portal" — arena (instanced) bounds. |
| Soul Wars | s1 | "Enter the Soul Wars portal" — Isle of Souls arena bounds. |
| Last Man Standing | s1 | "Enter the LMS lobby" — lobby/arena bounds. |
| Barbarian Assault | s1 | "**Talk to Captain Cain**" — **condition-type mismatch**: this is `NPC_TALKED_TO`, not a zone. (Captain Cain npc id needed; cache lookup.) |

**Options (apply per source).**
- **(A) add `completionZone`** = the *destination/instanced* arena bounds (needs wiki + in-game coordinate verification, not the activation coord).
- **(B) change condition** — Barbarian Assault → `NPC_TALKED_TO` (Captain Cain).
- Each needs a per-step "what does *arrived* mean here" decision + (for transport cases) the instanced destination coordinates. This is a judgment + in-game-coordinate task.

---

## C2 — 23 `loopBackToStep` steps with `loopCount` 0/absent

**Flag.** A loop runs only when `loopBackToStep > 0 && loopCount > 0`
(`StepAdvancer.advance`, `StepAdvancer.java:135-137`). All 23 set `loopBackToStep` but leave
`loopCount` 0/absent, so the loop never engages.

**Why not "blanket-set `loopCount`" and — newly discovered — why the self-loop subset is NOT vestigial.**
The 23 split into two shapes:

### C2a — 7 self-loops (`loopBackToStep` targets the step itself) — **all D4 Batch-3 skilling sources**

Aerial Fishing s2, Deep Sea Fishing s2, Woodcutting (Teak Trees) s2, Mining (Gemstone Rocks) s2,
Fishing (Swordfish) s2, Underwater Crabs s3, Cutting Squid s2.

An earlier pass proposed removing these as "vestigial dead config." **That is wrong, and the build
proves it.** All 7 are in `D4Batch3RegressionTest.BATCH3_SOURCES`, and its `allBatch3SourcesHaveDeepGuidanceShape`
test asserts (line 118-121, "Element 4 — activity loop mandatory for skilling sources"):

```java
boolean hasLoop = source.getGuidanceSteps().stream().anyMatch(s -> s.getLoopBackToStep() > 0);
assertTrue(hasLoop, name + " has no loop step ... E4 is mandatory for skilling sources");
```

So the self-loop `loopBackToStep` is **authored guidance shape** (the E4 "activity loop" marker for
skilling grinds), not dead config — removing it breaks the contract test. But a self-loop with
`loopCount=0` is inert at runtime (`StepAdvancer.java:135-137`) and re-running a *single* step in
place is not a meaningful loop either. So E4 is currently satisfied only cosmetically.

**Decision needed (C2a).** What should an open-ended skilling grind's "activity loop" actually be?
- **(A)** Set a real `loopCount` on the self-loop. *But what value?* An open grind ("fish until the
  Heron/clog item") has no natural finite count; the engine already completes via
  `onItemObtained` (`GuidanceSequencer.java:496-536`). A large arbitrary count just re-highlights the
  same step N times.
- **(B)** Keep `loopBackToStep`=self + `loopCount`=0 as a deliberate **display marker** and teach the
  analyzer to not flag self-loops that satisfy E4 (adjust `scripts/audit_guidance_config.py` + the
  ratchet so the self-loop class is recognized as by-design, not a "never engages" bug).
- **(C)** Remove the self-loop **and** relax the D4 E4 contract (the loop affordance is dropped for
  open grinds; completion stays on `onItemObtained`).

### C2b — 16 earlier-target loops (`loopBackToStep` points at an earlier step)

Brimhaven Agility Arena s2, Pest Control s2, Mage Training Arena s2, Tithe Farm s2,
Gnome Restaurant (Scarfs) s4, Fishing Trawler s2, Temple Trekking s4, Rogues' Den s3, Castle Wars s2,
Soul Wars s2, Last Man Standing s2, Vale Totems s3, Barbarian Assault s2, Pyramid Plunder s3,
Motherlode Mine s3, Gnome Restaurant (Seed Pods) s4.

For these the loop *might* be intended (re-run an earlier sub-sequence, e.g. Gnome Restaurant s4→s1,
Motherlode s3→relative) and just missing `loopCount`; or it may be vestigial.

**Decision needed (C2b), per source.** **(A)** set a real `loopCount` (looping intended) or
**(B)** remove `loopBackToStep` (vestigial). Decide each against the step semantics. (Several of
these sources also appear in N1 — fixing both in the same per-source PR is natural.)

---

## C3 — Shades of Mort'ton step[2] `MANUAL` ("Pick up your shade key")

**Flag.** Step[2] is `MANUAL` and "never auto-advances," yet it enumerates all ~25 shade-key ids in
`groundItemIds` + `skipIfHasAnyItemIds` — a known "player now holds a key" signal that looks unwired.
The earlier prescription: "wire an OR-of-`INVENTORY_HAS_ITEM` via the B1 `conditionTree`, drop MANUAL."

**On inspection this is a #775-class flag, not a clean win. Three engine facts:**

1. **The B1 `conditionTree` is NOT wired into the live engine.** `conditionTree` is deserialized onto
   `GuidanceStep` (`GuidanceStep.java:289-290`) and unit-tested in isolation, but **no production code
   evaluates `step.getConditionTree()`** — a grep of `src/main/java` finds zero callers in the
   completion path; `ConditionEvaluationContext` is referenced only by its own factory + javadoc, and
   `GuidanceSequencer`/`CompletionChecker` never consult the tree. So "set a `conditionTree` and drop
   MANUAL" would leave the step with **no firing condition** — still a dead-end. Wiring `conditionTree`
   into the sequencer's event/tick path is a real **engine feature**, not a data edit.

2. **The engine already has an OR-of-items auto-advance — and it is deliberately suppressed here.**
   `onInventoryChanged` (`GuidanceSequencer.java:595-602`) advances on `skipIfHasAnyItemIds`
   *regardless of `completionCondition`* — and Shades step[2] already declares all 25 keys there. It
   does not fire because of the `!isRecurringGatherSequence()` guard (`:596`). Shades **is** a
   recurring-gather sequence: step[4] carries `restockIfMissingAllItemIds` and a real 50-iteration
   loop (`loopBackToStep:2, loopCount:50`), so `isRecurringGatherSequence()`
   (`GuidanceSequencer.java:887-901`) returns true. This suppression is **intentional** (#707/#715/#719):
   auto-advancing off the key-pickup step after the *first* key clears the gather highlight
   prematurely while the player is still collecting keys for the chest loop.

3. **The step is not a permanent trap.** `onItemObtained` (`GuidanceSequencer.java:496-536`)
   force-completes the sequence when the clog slot unlocks, so a real run never gets stuck on step[2].

**So the two prior issues are in direct conflict on this exact source:** #729 wants step[2] to
auto-advance (smoother first-run); #707/#715/#719 deliberately stop it from doing so to preserve the
gather-loop highlight. Whichever vehicle (conditionTree or a new `INVENTORY_HAS_ANY_ITEM` mode), making
step[2] auto-advance on "holds any key" re-introduces exactly the behavior #707 removed.

**Decision needed (C3).**
- **(A) Leave as-is** (`MANUAL`, #707-preserving). The "dead-end" is the intended gather-highlight
  behavior; `onItemObtained` handles real completion. Lowest risk; closes C3 as by-design (update the
  findings doc, do not edit data).
- **(B) Make step[2] auto-advance on holding a key.** Requires either (i) wiring `conditionTree` into
  the engine (new feature + the B1 evaluation path) or (ii) a new `INVENTORY_HAS_ANY_ITEM` completion
  mode with a `completionItemIds` list — **and** a rule for how it coexists with the #707
  recurring-gather suppression (e.g. only on the first, non-loop pass). Higher risk; needs in-game
  validation against the #707 regression.

A choice here also determines whether the un-wired B1 `conditionTree` gets promoted to a live engine
feature or stays a dormant primitive.

---

## What lands after each decision

| Item | If chosen | Mechanical follow-up |
|------|-----------|----------------------|
| C1 | per-source A/B/C | one PR per source; cache-verified ids via `npc_lookup` + domain-skeptic; decrement `ACTOR_DEATH_MISSING_NPC_CEILING` |
| N1 | per-source A/B | one PR per source; instanced coords via `coordinate_helper`/wiki; decrement `ARRIVE_AT_ZONE_MISSING_ZONE_CEILING` |
| C2a | A/B/C | data + analyzer/D4-contract alignment; decrement `LOOP_NEVER_ENGAGES_CEILING` only if removed |
| C2b | per-source A/B | one PR; decrement `LOOP_NEVER_ENGAGES_CEILING` |
| C3 | A or B | A = doc-only (by-design); B = engine PR + in-game #707 regression check |

No ratchet ceiling was decremented and no data edited in this pass — every change is gated on a
decision above. When an item resolves and lands, decrement the matching constant in
`GuidanceConfigInvariantsRegressionTest` so the ratchet stays tight, then re-run
`./gradlew guidanceStatus` to confirm the board moved.
