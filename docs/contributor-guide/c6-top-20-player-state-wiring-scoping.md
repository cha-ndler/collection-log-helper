# Tier C6 scoping — wire C1-C5 player-state detectors into the top-20 sources

This document scopes the [Tier C6](../ROADMAP.md#tier-c--player-aware-guidance) milestone: a first-pass wiring of the five player-state detectors landed in C1-C5 (PRs [#608](https://github.com/cha-ndler/collection-log-helper/pull/608) and [#616](https://github.com/cha-ndler/collection-log-helper/pull/616)) into `drop_rates.json` for the 20 most-farmed collection-log sources.

This file is the *plan*. Each follow-up PR delivers one batch of detector-wiring against `drop_rates.json`, accompanied (where required) by Tier B schema PRs that introduce the missing precondition fields.

---

## 1. Selection criteria

C6 inherits the top-20 source list from [Tier D2 §2](d2-top-40-scoping.md#2-the-40-source-list) — entries 1-20 of that table. The D2 list operationalizes "top 20 most-farmed" as the union of:

- **A.** Canonical endgame content (raids, GWD, DT2, top solo bosses).
- **B.** TempleOSRS boss popularity rankings (top-30 KC-per-player-week).
- **C.** Loghunters audit popularity (sources with >100k recorded KC).
- **F.** RuneLite plugin author intuition (current 2026 meta).

Rationale: a source that ranks in the D2 top-20 is by definition the one most accounts farm repeatedly, so the marginal value of a personalised travel/loadout route on that source is highest. The bottom-20 of D2 (Tier-2 slayer bosses, wilderness bosses, clue tiers, skilling minigames) is deferred to a follow-up C-tier milestone after C6 lands.

The C-tier detector surface is fixed at five components landed in C1-C5; this scoping does **not** introduce new detectors.

| Component | Interface | Detector role | Landed in |
|---|---|---|---|
| **C1** | `PohTeleportInventory` | POH-fixture teleport access (mounted glory, jewellery box tiers, spirit tree / fairy ring in superior garden, portal nexus, mounted digsite / Xeric's talisman) | #608 |
| **C2** | `EquippedItemState` | Currently-equipped item IDs (teleport jewellery, charged glory, max cape, RoW, slayer helmet, etc.) | #608 |
| **C3** | `DiaryTierState` | Achievement Diary completion (12 regions × 4 tiers) | #608 |
| **C4** | `SkillCapePerkState` | Skill-cape perk availability (equipped-cape OR real-level ≥ 99 fallback) | #608 |
| **C5** | `QuestProgressState` | Partial-quest sub-milestones (Lost City dramen branch, Plague City complete, Fairytale II started, Children of the Sun complete, all 10 RFD sub-quests) | #608 |

All five detectors now refresh live via `@Subscribe` per #616, so any C6 wiring (sequencer-evaluated, overlay-evaluated, or conditional-alternative branch) sees fresh state on the next tick.

---

## 2. The 20-source list with C1-C5 applicability matrix

Each row is one of the top-20 D2 sources. Each `C*` cell is:

- `Y — <example>` — detector applies meaningfully to this source's travel, loadout, or branch selection.
- `N` — detector has no relevant effect on this source.
- `?` — applicability unclear; listed in §5 follow-ups.

Cells cite a concrete branch or step the data file should express. "Example" is the routing decision; the schema mechanism that expresses it is described in §3.

| # | Source | C1 — POH teleport | C2 — Equipped item | C3 — Diary tier | C4 — Skill-cape perk | C5 — Quest sub-milestone |
|---|---|---|---|---|---|---|
| 1  | Chambers of Xeric            | Y — `XERICS_TALISMAN` to Xeric's Glade is the fastest CoX approach if mounted; else fall back to Kourend Castle teleport | Y — `XERICS_TALISMAN` charged in inventory is the standard tablet route | Y — Kourend Hard diary unlocks the in-Lovakengj Xeric's Heart teleport which is 2 tiles from the prep room | N | N |
| 2  | Theatre of Blood             | N | Y — Equipped Drakan's medallion teleports straight to Ver Sinhaza | N | N | Y — `A_TASTE_OF_HOPE` complete unlocks Drakan's medallion; pre-completion route uses Canifis teleport |
| 3  | Theatre of Blood (Hard Mode) | N | Y — Same Drakan's medallion path as ToB | N | N | Y — Same `A_TASTE_OF_HOPE` gate as ToB |
| 4  | Tombs of Amascut             | N | Y — Pharaoh's sceptre equipped teleports directly to Jaltevas Pyramid | Y — Desert Hard diary makes the Pharaoh's sceptre unlimited-charge | N | N |
| 5  | General Graardor             | Y — `JEWELLERY_BOX_FANCY+` Combat bracelet → Warriors' Guild → Trollheim | Y — Equipped Bandos godsword / armour reduces GWD KC requirement (none — godsword is loadout, not gate) | Y — Fremennik Hard diary skips the Trollheim teleport requirement (faster Goblin village walk) | N | Y — `TROLL_STRONGHOLD` complete unlocks Trollheim teleport (the canonical approach) |
| 6  | Commander Zilyana            | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet route shared with Graardor | Y — Equipped Saradomin items (mjolnir / godsword) factor only as loadout, not travel | N | N | N |
| 7  | K'ril Tsutsaroth             | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet route shared with Graardor | Y — Zamorakian spear / hasta equipped is the canonical melee approach | N | N | N |
| 8  | Kree'arra                    | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet route shared with Graardor | Y — Armadyl crossbow / blowpipe equipped is the canonical ranged loadout | N | N | N |
| 9  | Vardorvis                    | N | Y — Equipped Strangled tablet / charged ring of shadows → DT2 boss lobby | N | N | Y — `DESERT_TREASURE_II` complete unlocks the boss instance entirely; required gate |
| 10 | Duke Sucellus                | N | Y — Charged ring of shadows equipped → Ghorrock | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 11 | The Leviathan                | N | Y — Charged ring of shadows equipped → boss lobby | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 12 | The Whisperer                | N | Y — Charged ring of shadows equipped → Whisperer instance | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 13 | Vorkath                      | Y — `JEWELLERY_BOX_FANCY+` Combat bracelet → Rellekka POH-style is unrelated; use mounted glory to Edgeville fallback if no Rellekka access | Y — Equipped Salve amulet (e) is loadout-grade; equipped ava's assembler is loadout | Y — Fremennik Hard diary lets the player skip the Rellekka tax (faster Lighthouse approach via Fremennik diary cape) | N | Y — `DRAGON_SLAYER_II` complete unlocks the post-quest Vorkath fight (gate, not a route choice) |
| 14 | Zulrah                       | Y — `FAIRY_RING` in superior garden + dramen/lunar staff allows AKQ ring as the fastest Zulrah route (current meta) | Y — Equipped Zul-andra teleport tab | N | N | Y — `FAIRYTALE_II_FAIRY_RINGS_UNLOCKED` (started, not complete) is the gate for the AKQ route; `REGICIDE` complete is the alternative gate |
| 15 | Phantom Muspah               | N | Y — Equipped Quetzal whistle is fastest if Children of the Sun complete (alternative is fairy ring AXP) | N | N | Y — `SECRETS_OF_THE_NORTH` complete is the boss-instance gate; required |
| 16 | The Nightmare                | N | Y — Equipped Drakan's medallion → Slepe (Nightmare lobby is 1 tile from Slepe Church) | N | N | Y — `SINS_OF_THE_FATHER` complete unlocks the Drakan's medallion Slepe destination |
| 17 | Phosani's Nightmare          | N | Y — Same Drakan's medallion + Slepe route as Nightmare | N | N | Y — Same `SINS_OF_THE_FATHER` gate as Nightmare |
| 18 | Corporeal Beast              | Y — `JEWELLERY_BOX_FANCY+` Games necklace → Wilderness (Corp cave is south of the lever room) is the standard ironman route | Y — Equipped Spectral / Elysian sigil for tank role | N | N | N |
| 19 | The Gauntlet                 | N | Y — Equipped Crystal teleport seed (Prifddinas) is the fastest Prifddinas approach | Y — Western Provinces Elite diary unlocks the cape-teleport-anywhere variant; not a Gauntlet gate but it does affect Prifddinas approach speed | N | Y — `SONG_OF_THE_ELVES` complete is the Prifddinas gate; required |
| 20 | Corrupted Gauntlet           | N | Y — Same Crystal teleport seed path as Gauntlet | Y — Same Western Provinces Elite shortcut | N | Y — Same `SONG_OF_THE_ELVES` gate |

**Per-detector applicability totals (Y count out of 20):**

- C1 — POH teleport: **8** (GWD ×4 sharing Combat-bracelet route, CoX, Vorkath, Zulrah, Corp).
- C2 — Equipped item: **18** (only CoX's pure POH route at #1 and Graardor at #5 use C2 lightly; all 4 DT2 + all raid analogues + every charged-jewellery boss use it heavily).
- C3 — Diary tier: **5** (CoX-Kourend, Vorkath-Fremennik, ToA-Desert, Gauntlets-Western Provinces, Graardor-Fremennik).
- C4 — Skill-cape perk: **0** — no source in the top-20 is gated on a skill-cape perk for travel.  Construction cape (POH-teleport-from-anywhere) is the only plausible match and is already covered by the existing source-level `requirements` model when used as a POH-jump shortcut. **All five C4 rows are `N` for the top-20 list.**
- C5 — Quest sub-milestone: **10** (DT2 ×4, ToB + HM ×2, Nightmare + Phosani ×2, Vorkath, Zulrah, Muspah, Gauntlets ×2 — minus duplicate-count for shared gates).

C4 deserves explicit note: it is a *full miss* on the top-20 list. Skill-cape perks unlock travel and tools, but those benefits are skewed toward skilling, slayer-task setup, and clue routes — i.e., the bottom-20 of D2 (Wintertodt → Hunter Firestriker; Master clues → Crafting / Farming / Construction teleports). For C6 specifically, **C4 wiring is a no-op** and should be deferred to a follow-up milestone targeting the D2 bottom-20.

---

## 3. Wiring patterns — schema mechanism per detector

For each detector, the data file expresses an applicability constraint via one of two existing schema mechanisms or a proposed Tier B schema extension.

### 3.1 Existing schema surfaces

The two precondition surfaces on a step or source today (per [schema-reference.md](schema-reference.md)):

- **`requirements.quests` / `requirements.skills`** — at source level *or* on a `conditionalAlternatives[i]` entry. Quest matching uses `net.runelite.api.Quest` enum names (e.g., `DESERT_TREASURE_II`, `SONG_OF_THE_ELVES`). Skill matching is a level threshold per skill.
- **`conditionalAlternatives`** — array on a step that selects an alternative branch when its `requirements` match. Falls through to the base step when no alternative matches.

### 3.2 Per-detector mapping

| Detector | Wiring mechanism today | Gap |
|---|---|---|
| **C1 — POH teleport** | None. `requirements.quests` cannot express "player has mounted glory" / "player has jewellery box ≥ Fancy". | **Schema gap.** Need a new `requirements.pohTeleports: ["JEWELLERY_BOX_FANCY", "MOUNTED_GLORY", ...]` field that maps to `PohTeleportInventory.hasTeleport(PohTeleport)`. |
| **C2 — Equipped item** | None. `requiredItemIds` on a step refers to *inventory*, not equipment. | **Schema gap.** Need a new `requirements.equippedItemIds: [int]` field that maps to `EquippedItemState.hasEquipped(int)`. Semantics: AND across the list, OR with the rest of `requirements`. |
| **C3 — Diary tier** | None. `requirements` has no diary field. | **Schema gap.** Need a new `requirements.diary: {"region": "FREMENNIK", "tier": "HARD"}` (or array thereof) that maps to `DiaryTierState.hasDiary(region, tier)`. |
| **C4 — Skill-cape perk** | None — and not needed for C6 per §2 (C4 has zero top-20 applications). | **No schema work for C6.** If the bottom-20 milestone surfaces a need, add a `requirements.skillCapePerks: ["CRAFTING_TELE", ...]` field then. |
| **C5 — Quest sub-milestone** | Partial. `requirements.quests` only accepts `net.runelite.api.Quest` names with FINISHED semantics. It cannot express "Fairytale II started" or "RFD freed Pirate Pete" — but `Quest.FAIRYTALE_II__CURE_A_QUEEN` FINISHED *is* a workable proxy for many cases (over-restrictive but never incorrect). | **Schema gap (partial).** For most C6 cases, the existing `requirements.quests` + `FINISHED` is sufficient (DT2, SOTE, SOTF, ATOH, DSII, SOTN). Only Zulrah row #14 strictly needs the sub-milestone (Fairytale II *started*); we recommend using the existing `requirements.quests: ["FAIRYTALE_II__CURE_A_QUEEN"]` with a relaxed-evaluation comment for C6, and **defer** introducing `requirements.questMilestones: ["FAIRYTALE_II_FAIRY_RINGS_UNLOCKED"]` to a separate Tier B PR. |

### 3.3 Conditional-alternative pattern

For sources where multiple detector-gated routes coexist (e.g., Graardor's "Trollheim + Combat bracelet" vs. "Goblin Village walk"), each branch is a `conditionalAlternatives[i]` entry whose `requirements` block uses one or more of the C1/C2/C3 fields above. Ordering matters: the sequencer takes the first matching alternative, so list the *fastest* route first and the unrestricted fallback last (matching the base step).

Example (Graardor, illustrative — not authoring text):

```json
{
  "description": "Travel to Bandos throne room (default: walk from Burthorpe)",
  "completionCondition": "ARRIVE_AT_TILE",
  "conditionalAlternatives": [
    {
      "requirements": {
        "pohTeleports": ["JEWELLERY_BOX_FANCY"],
        "diary": {"region": "FREMENNIK", "tier": "HARD"}
      },
      "description": "Combat bracelet → Warriors' Guild + Fremennik Hard diary shortcut",
      "travelTip": "POH jewellery box -> Combat bracelet -> Warriors' Guild"
    },
    {
      "requirements": {"quests": ["TROLL_STRONGHOLD"]},
      "description": "Trollheim teleport -> south to GWD",
      "travelTip": "Trollheim teleport"
    }
  ]
}
```

The fields `pohTeleports`, `equippedItemIds`, and `diary` are the three Tier B additions §3.2 calls out.

### 3.4 Source-level vs. step-level

- **Source-level `requirements`** stay reserved for hard gates that lock the entire source (boss-instance quests like DSII / SOTE / SOTN / DT2). All ten C5 rows in §2 fall into this bucket; no schema change needed for them.
- **Step-level `conditionalAlternatives.requirements`** is where the three new fields (`pohTeleports`, `equippedItemIds`, `diary`) get used. Source-level should rarely need them — a player without a Combat bracelet still kills Graardor, just slower.

---

## 4. Batching plan

Four batches, grouped by **detector** (not by source), so each batch can be reviewed against one detector's mental model and merged independently. C4 is dropped from the batch plan entirely (zero top-20 applications per §2).

| Batch | Theme | Sources touched | Schema dependency | Est. sources |
|---|---|---|---|---|
| **B0 (schema)** | Tier B schema extension PR — adds `requirements.pohTeleports`, `requirements.equippedItemIds`, `requirements.diary` to the conditional-alternative requirement model. Pure Java + tests; no data changes. | n/a | Lands before B1-B3. **Hard blocker for all subsequent batches.** | 0 |
| **B1 (C2 — equipped items)** | Wire equipped-item routes on every source in §2 that has `Y` in the C2 column. This is the biggest payoff because it covers all 18 sources at least lightly and is the most common branch trigger. | 18 | B0 | 18 |
| **B2 (C1 — POH teleports)** | Wire POH-teleport routes on the 8 sources with `Y` in C1: GWD ×4 (shared Combat-bracelet route), CoX, Vorkath, Zulrah, Corporeal Beast. | 8 | B0 | 8 |
| **B3 (C3 — diary tier)** | Wire diary-shortcut routes on the 5 sources with `Y` in C3: CoX-Kourend, Vorkath-Fremennik, ToA-Desert, Gauntlet ×2-Western Provinces, Graardor-Fremennik. | 5 (some overlap with B1/B2 — same step gets a stacked `requirements` block) | B0 | 5 |
| **C5 — quest sub-milestones** | **Not a batch.** The 10 C5 rows are all hard source-level gates that the existing `requirements.quests` model already expresses. Verify each source has the correct `requirements.quests` entry as part of B1/B2 PR review; do not open a separate batch. | (covered inline) | None | 0 |

**Ordering rationale:**

- B0 first because every later batch depends on it.
- B1 next because C2 has the highest top-20 coverage (18/20) and the largest user-visible impact.
- B2 third because POH-teleport routes are a smaller subset and several share the same authored step across GWD bosses.
- B3 last because diary shortcuts are the narrowest set (5 sources) and several of those sources will already be touched in B1/B2 — B3 is a refinement pass that stacks a third `requirements` clause onto existing alternatives.

**Estimated PR count:** 4 PRs (1 Tier B schema + 3 Tier C data). Total touched sources: 20 (each appears in at least one batch).

---

## 5. Open questions / follow-ups

1. **C4 deferred entirely.** Skill-cape perks score zero applicability on the top-20 list. Should the C6 issue be amended to drop C4 from scope, or do we keep a `C4 — N for top-20, deferred` note as the final wiring outcome? Recommendation: keep the no-op note so the milestone-close audit can confirm intentional skip.
2. **C5 `FAIRYTALE_II_FAIRY_RINGS_UNLOCKED` precision.** Row #14 (Zulrah, AKQ fairy ring) strictly wants the *started-not-finished* state. The current `requirements.quests: ["FAIRYTALE_II__CURE_A_QUEEN"]` is over-restrictive (requires completion). Two options:
   - (a) Accept the over-restriction in C6 and open a Tier B follow-up for `requirements.questMilestones`.
   - (b) Block C6's B1 batch on a Tier B `questMilestones` PR.
   Recommendation: (a). Cost of over-restriction is a slower AKQ route for the narrow window between Fairytale II start and finish; not worth blocking 17 other sources.
3. **CoX C1 row applicability.** `XERICS_TALISMAN` mounted vs. handheld is currently both `Y` for C1 and C2 — the wiring should pick one to avoid double-counting. Recommendation: mounted-in-POH gates the "teleport from house" branch (C1); handheld charged talisman in inventory gates the "tablet from bank" branch (C2). They are *different* alternatives, not duplicates.
4. **Drakan's medallion (rows 2/3/16/17) and ring of shadows (rows 9-12) as C2 triggers.** Both are jewellery items with discrete charge counts. C2 only checks "equipped", not "has charges". Recommendation: scope C6 to equipment-presence only; charge-aware routing is out of scope and would be its own detector.
5. **Source-level vs. step-level `requirements.diary`.** Should the Vorkath Fremennik-Hard shortcut be expressed on the boss `worldX/worldY` waypoint (source-level), on a travel step (step-level), or as a `conditionalAlternatives` entry on the first guidance step? Recommendation: step-level `conditionalAlternatives` for consistency with the existing fairy-ring / mounted-glory branch pattern.
6. **Graardor row C2 caveat.** The C2 cell on row #5 lists "Bandos godsword / armour equipped" but those are loadout, not travel — they do not change routing. Recommend rating row #5 as a partial `Y` for C2 only if a travel-affecting equip (Combat bracelet equipped, mounted-glory tablet equipped) is what the wiring keys on; loadout equips are out of C6 scope.
7. **B0 schema PR — backwards compatibility.** New fields must be optional on the requirements record so existing JSON parses unchanged. The Jackson model already tolerates unknown fields per `data-authoring.md` convention; confirm during B0 implementation.
8. **Top-20 vs. top-21+ split.** Sources 21-40 of the D2 list (slayer + classic + clues + minigames) are where C4 (skill-cape perks) and the remaining C1/C3 applications cluster. A follow-up C6.5 / C7 milestone should pick those up; do not let it leak into C6.

---

## 6. References

- [Tier D2 scoping — top-40 sources](d2-top-40-scoping.md) (source list inheritance)
- [JSON Schema Reference](schema-reference.md) (existing `requirements` + `conditionalAlternatives` surface)
- [Tier C status log](../ROADMAP.md#tier-c--player-aware-guidance) (C1-C5 PR numbers, C6 status)
- PR [#608](https://github.com/cha-ndler/collection-log-helper/pull/608) — landed C1-C5 detector interfaces + impls
- PR [#616](https://github.com/cha-ndler/collection-log-helper/pull/616) — wired `@Subscribe` lifecycle so detectors refresh live
- `com.collectionloghelper.player.PohTeleportInventory` (C1)
- `com.collectionloghelper.player.EquippedItemState` (C2)
- `com.collectionloghelper.player.DiaryTierState` + `DiaryRegion` + `DiaryTier` (C3)
- `com.collectionloghelper.player.SkillCapePerkState` + `SkillCapePerk` (C4)
- `com.collectionloghelper.player.QuestProgressState` + `QuestSubMilestone` (C5)
