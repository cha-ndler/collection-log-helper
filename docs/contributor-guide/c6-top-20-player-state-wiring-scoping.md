# Tier C6 scoping — wire C1-C5 player-state detectors into the top-20 sources

This document scopes the [Tier C6](../ROADMAP.md#tier-c--player-aware-guidance) milestone: a first-pass wiring of the five player-state detectors (C1 landed in [#470](https://github.com/cha-ndler/collection-log-helper/pull/470), C2 in [#463](https://github.com/cha-ndler/collection-log-helper/pull/463), C3 in [#461](https://github.com/cha-ndler/collection-log-helper/pull/461), C4 in [#471](https://github.com/cha-ndler/collection-log-helper/pull/471), C5 in [#462](https://github.com/cha-ndler/collection-log-helper/pull/462); all five live-refreshed by [#616](https://github.com/cha-ndler/collection-log-helper/pull/616)) into `drop_rates.json` for the 20 most-farmed collection-log sources.

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
| **C1** | `PohTeleportInventory` | POH-fixture teleport access (mounted glory, jewellery box tiers, spirit tree / fairy ring in superior garden, portal nexus, mounted digsite / Xeric's talisman) | #470 |
| **C2** | `EquippedItemState` | Currently-equipped item IDs (teleport jewellery, charged glory, max cape, RoW, slayer helmet, etc.) | #463 |
| **C3** | `DiaryTierState` | Achievement Diary completion (12 regions × 4 tiers) | #461 |
| **C4** | `SkillCapePerkState` | Skill-cape perk availability (equipped-cape OR real-level ≥ 99 fallback) | #471 |
| **C5** | `PlayerQuestProgressState` | Partial-quest sub-milestones (Lost City dramen branch, Plague City complete, Fairytale II started, Children of the Sun complete, all 10 RFD sub-quests) | #462 |

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
| 1  | Chambers of Xeric            | Y — Mounted `XERICS_TALISMAN` in POH portal nexus room teleports to Xeric's Glade, then run south to CoX entrance | N — handheld Xeric's talisman tablet is an *inventory* item, not equipped; covered by the existing `requiredItemIds` step shape, not C2 | Y — Kourend Hard diary unlocks the in-Lovakengj Xeric's Heart teleport which is 2 tiles from the prep room | N | N |
| 2  | Theatre of Blood             | N | Y — Equipped Drakan's medallion teleports straight to Ver Sinhaza (see §5 Q4 — charge-aware fallback required in B1) | N | N | Y — `A_TASTE_OF_HOPE` complete unlocks Drakan's medallion; pre-completion route uses Canifis teleport |
| 3  | Theatre of Blood (Hard Mode) | N | Y — Same Drakan's medallion path as ToB (charge guard per §5 Q4) | N | N | Y — Same `A_TASTE_OF_HOPE` gate as ToB |
| 4  | Tombs of Amascut             | N | N — Pharaoh's sceptre's Jaltevas teleport is invoked from **inventory** regardless of wield state; the sceptre is a wieldable polestaff but the teleport right-click works from the inventory slot (see §5 Q9) | Y — Desert Hard diary makes the Pharaoh's sceptre unlimited-charge | N | N |
| 5  | General Graardor             | Y — `JEWELLERY_BOX_FANCY+` provides Combat bracelet → Warriors' Guild (Burthorpe); player then runs north and uses the separate Trollheim teleport / walk to GWD | N — Bandos godsword / armour are loadout, not travel-affecting (see §5 Q6) | Y — Fremennik Hard diary unlocks the Trollheim teleport requirement skip (Goblin village walk shortcut becomes viable) | N | Y — `TROLL_STRONGHOLD` complete unlocks Trollheim teleport (the canonical approach) |
| 6  | Commander Zilyana            | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet → Warriors' Guild approach shared with Graardor | N — Zamorakian spear / hasta are loadout-only, do not change routing (§5 Q6) | N | N | N |
| 7  | K'ril Tsutsaroth             | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet → Warriors' Guild approach shared with Graardor | N — Zamorakian spear / hasta are loadout-only (§5 Q6) | N | N | N |
| 8  | Kree'arra                    | Y — Same `JEWELLERY_BOX_FANCY+` Combat bracelet → Warriors' Guild approach shared with Graardor | N — Armadyl crossbow / blowpipe are loadout-only (§5 Q6) | N | N | N |
| 9  | Vardorvis                    | N | Y — Equipped charged ring of shadows → DT2 boss lobby (charge guard per §5 Q4) | N | N | Y — `DESERT_TREASURE_II` complete unlocks the boss instance entirely; required gate |
| 10 | Duke Sucellus                | N | Y — Charged ring of shadows equipped → Ghorrock (charge guard per §5 Q4) | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 11 | The Leviathan                | N | Y — Charged ring of shadows equipped → boss lobby (charge guard per §5 Q4) | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 12 | The Whisperer                | N | Y — Charged ring of shadows equipped → Whisperer instance (charge guard per §5 Q4) | N | N | Y — `DESERT_TREASURE_II` complete (shared gate) |
| 13 | Vorkath                      | Y — `MOUNTED_GLORY` → Edgeville is the fallback when Fremennik-tier teleport isn't available; otherwise diary/cape Rellekka teleport bypasses C1 entirely | N — Salve amulet (e) / ava's assembler are loadout-only (§5 Q6); no equipped item drives the route | Y — Fremennik Hard diary unlocks the Lighthouse-via-Rellekka shortcut (cape teleport to Rellekka, run north) | N | Y — `DRAGON_SLAYER_II` complete unlocks the post-quest Vorkath fight (gate, not a route choice) |
| 14 | Zulrah                       | Y — `FAIRY_RING` in superior garden + dramen/lunar staff allows AKQ ring as the fastest Zulrah route (current meta) | N — Zul-andra teleport scroll is an **inventory** item (teleport tablet shape), never equipped (see §5 Q9) | N | N | Y — `FAIRYTALE_II_FAIRY_RINGS_UNLOCKED` (started, not complete) is the gate for the AKQ route; `REGICIDE` complete is the alternative gate. See §5 Q2 for the C5 over-restriction tradeoff. |
| 15 | Phantom Muspah               | N | N — Quetzal whistle is an **inventory** item, not wieldable in its teleport-active form (see §5 Q9) | N | N | Y — `SECRETS_OF_THE_NORTH` complete is the boss-instance gate; required |
| 16 | The Nightmare                | N | Y — Equipped Drakan's medallion → Slepe (Nightmare lobby is 1 tile from Slepe Church; charge guard per §5 Q4) | N | N | Y — `SINS_OF_THE_FATHER` complete unlocks the Drakan's medallion Slepe destination |
| 17 | Phosani's Nightmare          | N | Y — Same Drakan's medallion + Slepe route as Nightmare (charge guard per §5 Q4) | N | N | Y — Same `SINS_OF_THE_FATHER` gate as Nightmare |
| 18 | Corporeal Beast              | Y — `JEWELLERY_BOX_FANCY+` Games necklace → Wilderness Volcano then south to lever room is the standard ironman route | N — Spectral / Elysian sigils are loadout-only for tank role (§5 Q6) | N | N | N |
| 19 | The Gauntlet                 | N | N — Crystal teleport seed is an **inventory** item, never equipped (see §5 Q9) | Y — Western Provinces Elite diary unlocks the cape-teleport-anywhere variant; not a Gauntlet gate but it does affect Prifddinas approach speed | N | Y — `SONG_OF_THE_ELVES` complete is the Prifddinas gate; required |
| 20 | Corrupted Gauntlet           | N | N — Same Crystal teleport seed inventory-item case as Gauntlet (see §5 Q9) | Y — Same Western Provinces Elite shortcut | N | Y — Same `SONG_OF_THE_ELVES` gate |

**Per-detector applicability totals (Y count out of 20):**

- **C1 — POH teleport: 8** (CoX, Graardor, Zilyana, K'ril, Kree'arra, Vorkath, Zulrah, Corporeal Beast). GWD ×4 share one Combat-bracelet → Warriors' Guild authored alternative.
- **C2 — Equipped item: 8** (rows 2, 3, 9, 10, 11, 12, 16, 17 — Drakan's medallion ×4 and Ring of Shadows ×4). Rows 5-8, 13, and 18 are rated N because the candidate equipped items are loadout-only and do not change *routing* — §5 Q6 applies uniformly. Rows 1, 4, 14, 15, 19, 20 are rated N because their teleport vector is an **inventory** item (talisman tablet, Pharaoh's sceptre, Zul-andra scroll, Quetzal whistle, Crystal teleport seed ×2), not an equipped item — see §5 Q9. The 8 Y rows are fully covered by the B1a ([#625](https://github.com/cha-ndler/collection-log-helper/pull/625), Ring of Shadows) and B1b ([#626](https://github.com/cha-ndler/collection-log-helper/pull/626), Drakan's medallion) PRs, so **the B1 batch is complete** — no B1c PR is required.
- **C3 — Diary tier: 6** (CoX-Kourend, ToA-Desert, Graardor-Fremennik, Vorkath-Fremennik, Gauntlet ×2-Western Provinces). Note: 5 *unique* diary tiers cover 6 rows because Gauntlet + Corrupted Gauntlet share Western Provinces.
- **C4 — Skill-cape perk: 0** — no source in the top-20 is gated on a skill-cape perk for travel. Construction cape (POH-teleport-from-anywhere) is the only plausible match and is already covered by the existing source-level `requirements` model when used as a POH-jump shortcut. **All twenty C4 rows are `N` for the top-20 list.**
- **C5 — Quest sub-milestone: 14** (rows 2, 3, 5, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20). All 14 are hard source-level quest gates that the *existing* `requirements.quests` field already expresses with `FINISHED` semantics — no schema work needed. The one over-restriction is Zulrah's Fairytale II *started* state at row 14 (see §5 Q2).

C4 deserves explicit note: it is a *full miss* on the top-20 list. Skill-cape perks unlock travel and tools, but those benefits are skewed toward skilling, slayer-task setup, and clue routes — i.e., the bottom-20 of D2 (Wintertodt → Hunter Firestriker; Master clues → Crafting / Farming / Construction teleports). For C6 specifically, **C4 wiring is a no-op** and should be deferred to a follow-up milestone targeting the D2 bottom-20.

---

## 3. Wiring patterns — schema mechanism per detector

For each detector, the data file expresses an applicability constraint via one of the existing schema mechanisms or a proposed Tier B schema extension.

### 3.1 Existing schema surfaces

The precondition surfaces on a source or step today (verified against `SourceRequirements.java` and `ConditionalAlternative.java`):

- **`SourceRequirements`** carries three list fields: `quests` (e.g. `"DESERT_TREASURE_II"` — `net.runelite.api.Quest` enum names with `FINISHED` semantics), `skills` (list of `SkillRequirement` with skill + level threshold), and `diaries` (flat strings `"<AREA>_<TIER>"`, e.g. `"FREMENNIK_HARD"`, `"KANDARIN_ELITE"`).
- **`ConditionalAlternative.requirements`** is the same `SourceRequirements` type — so `quests`, `skills`, AND `diaries` are all usable at the conditional-alternative level today with no schema change.
- **`conditionalAlternatives`** — array on a step; the sequencer takes the first alternative whose requirements match. Falls through to the base step when no alternative matches.

### 3.2 Per-detector mapping

| Detector | Wiring mechanism today | Gap |
|---|---|---|
| **C1 — POH teleport** | None. `SourceRequirements` has no field for "player has mounted glory" / "player has jewellery box ≥ Fancy". | **Schema gap.** Need a new `requirements.pohTeleports: ["JEWELLERY_BOX_FANCY", "MOUNTED_GLORY", ...]` field (flat strings matching `PohTeleport` enum names) that maps to `PohTeleportInventory.hasTeleport(PohTeleport)`. |
| **C2 — Equipped item** | None. The existing `requiredItemIds` field on a step refers to *inventory*, not equipment. | **Schema gap.** Need a new `requirements.equippedItemIds: [int]` field that maps to `EquippedItemState.hasEquipped(int)`. Semantics: AND across the list, AND with the rest of `requirements`. |
| **C3 — Diary tier** | **`SourceRequirements.diaries: List<String>` already exists** with flat-string `"<AREA>_<TIER>"` shape, and `ConditionalAlternative.requirements` is the same `SourceRequirements` type — so the field is already usable at both source and step-alternative level. | **No new schema field.** Only verification needed during B3: confirm `DiaryTierState.hasDiary(...)` correctly resolves the existing flat-string format. If the wiring path through the alternative evaluator isn't already exercising `diaries`, that's a thin glue PR (not a schema PR). |
| **C4 — Skill-cape perk** | None — and not needed for C6 per §2 (C4 has zero top-20 applications). | **No schema work for C6.** If the bottom-20 milestone surfaces a need, add a `requirements.skillCapePerks: ["CRAFTING_TELE", ...]` field then. |
| **C5 — Quest sub-milestone** | Partial. `requirements.quests` only accepts `net.runelite.api.Quest` names with FINISHED semantics. It cannot express "Fairytale II started" or "RFD freed Pirate Pete" — but `Quest.FAIRYTALE_II__CURE_A_QUEEN` FINISHED *is* a workable proxy for many cases (over-restrictive but never incorrect). | **No schema work for C6.** For all 14 C5 cases the existing `requirements.quests` + `FINISHED` is sufficient (DT2 ×4, ATOH ×2, SOTF ×2, SOTE ×2, DSII, SOTN, TS, FT2-proxy). Only Zulrah row #14 is strictly over-restrictive (Fairytale II *started*); we recommend the proxy in C6 and **defer** introducing `requirements.questMilestones: ["FAIRYTALE_II_FAIRY_RINGS_UNLOCKED"]` to a separate Tier B PR — see §5 Q2. |

**Net B0 scope:** two new fields — `pohTeleports` and `equippedItemIds`. `diaries` already exists; no `diary` field is being added.

### 3.3 Conditional-alternative pattern

For sources where multiple detector-gated routes coexist (e.g., Graardor's "Combat-bracelet POH-teleport approach" vs. the unrestricted Burthorpe walk), each branch is a `conditionalAlternatives[i]` entry whose `requirements` block uses one or more of the C1/C2/C3 fields above. Ordering matters: the sequencer takes the first matching alternative, so list the *fastest* route first and the unrestricted fallback last (matching the base step).

Example (Graardor, illustrative — not authoring text):

```json
{
  "description": "Travel to Bandos throne room (default: walk from Burthorpe)",
  "completionCondition": "ARRIVE_AT_TILE",
  "conditionalAlternatives": [
    {
      "requirements": {
        "pohTeleports": ["JEWELLERY_BOX_FANCY"],
        "diaries": ["FREMENNIK_HARD"]
      },
      "description": "Combat bracelet -> Warriors' Guild; Fremennik Hard diary shortcut to GWD entrance",
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

The new fields `pohTeleports` and `equippedItemIds` are the two B0 schema additions §3.2 calls out; `diaries`, `quests`, and `skills` already exist.

### 3.4 Source-level vs. step-level

- **Source-level `requirements`** stay reserved for hard gates that lock the entire source (boss-instance quests like DSII / SOTE / SOTN / DT2). All 14 C5 rows in §2 fall into this bucket; no schema change needed for them.
- **Step-level `conditionalAlternatives.requirements`** is where the two new fields (`pohTeleports`, `equippedItemIds`) and the existing `diaries` field get used for branch selection. Source-level should rarely use them — a player without a Combat bracelet still kills Graardor, just slower.

---

## 4. Batching plan

Four batches, grouped by **detector** (not by source), so each batch can be reviewed against one detector's mental model and merged independently. C4 is dropped from the batch plan entirely (zero top-20 applications per §2).

| Batch | Theme | Sources touched | Schema dependency | Est. sources |
|---|---|---|---|---|
| **B0 (schema)** | Tier B schema extension PR — adds `requirements.pohTeleports` and `requirements.equippedItemIds` to `SourceRequirements`. Both fields nullable for backwards compatibility. Pure Java + tests; no data changes. **`diaries` is NOT added — it already exists.** | n/a | Lands before B1-B3. **Hard blocker for B1 and B2 only.** B3 can technically land without B0 since `diaries` already exists. | 0 |
| **B1 (C2 — equipped items)** | Wire equipped-item routes on every source in §2 with `Y` in the C2 column. After the §5 Q9 correction, scope is **8 sources** — Drakan's medallion ×4 (rows 2, 3, 16, 17) and Ring of Shadows ×4 (rows 9-12). Includes the §5 Q4 charge-aware-fallback guard. **Shipped: B1a ([#625](https://github.com/cha-ndler/collection-log-helper/pull/625), Ring of Shadows) + B1b ([#626](https://github.com/cha-ndler/collection-log-helper/pull/626), Drakan's medallion) — batch complete.** | 8 | B0 | 8 (shipped) |
| **B2 (C1 — POH teleports)** | Wire POH-teleport routes on the 8 sources with `Y` in C1: GWD ×4 (shared Combat-bracelet → Warriors' Guild alternative), CoX, Vorkath, Zulrah, Corporeal Beast. | 8 | B0 | 8 |
| **B3 (C3 — diary tier)** | Wire diary-shortcut routes on the 6 sources with `Y` in C3 using the existing `diaries` field at the conditional-alternative level: CoX-Kourend, Graardor-Fremennik, ToA-Desert, Vorkath-Fremennik, Gauntlet ×2-Western Provinces. Significant overlap with B1/B2 — same step gets a stacked `requirements` block. | 6 | None (schema-independent) | 6 |
| **C5 — quest sub-milestones** | **Not a batch.** The 14 C5 rows all use hard source-level quest gates that the existing `requirements.quests` field already expresses. Verify each source has the correct `requirements.quests` entry as part of B1/B2 PR review; do not open a separate batch. | (covered inline) | None | 0 |

**Ordering rationale:**

- B0 first because B1 and B2 depend on it.
- B1 next because C2 has high top-20 coverage (8/20 after the §5 Q9 correction) and large user-visible impact (DT2 ring-of-shadows + ToB/Nightmare medallion unlock direct-teleport branches).
- B2 third because POH-teleport routes are a smaller subset and several share the same authored alternative across GWD bosses.
- B3 last because diary shortcuts stack on top of branches authored in B1/B2 — easier to add a `diaries` clause to an existing alternative than to thread a separate diary-only alternative through.

**Estimated PR count:** 4 PRs (1 Tier B schema + 3 Tier C data). Batch coverage: **19 of 20** sources appear in at least one of B1/B2/B3 (row 15 Phantom Muspah is C5-only after the §5 Q9 correction, folded into the C5-handled-inline note in this same table). The inventory-teleport subset of 5 sources is deferred to a future C-extension milestone per §5 Q9. If schedule pressure demands, B3 can run in parallel with B0 since it doesn't depend on the new fields.

---

## 5. Open questions / follow-ups

1. **C4 deferred entirely.** Skill-cape perks score zero applicability on the top-20 list. Should the C6 issue be amended to drop C4 from scope, or do we keep a `C4 — N for top-20, deferred` note as the final wiring outcome? Recommendation: keep the no-op note so the milestone-close audit can confirm intentional skip.
2. **C5 `FAIRYTALE_II_FAIRY_RINGS_UNLOCKED` precision.** Row #14 (Zulrah, AKQ fairy ring) strictly wants the *started-not-finished* state. The current `requirements.quests: ["FAIRYTALE_II__CURE_A_QUEEN"]` is over-restrictive (requires completion). Two options:
   - (a) Accept the over-restriction in C6 and open a Tier B follow-up for `requirements.questMilestones`.
   - (b) Block C6's B1 batch on a Tier B `questMilestones` PR.
   Recommendation: (a). Cost of over-restriction is a slower AKQ route for the narrow window between Fairytale II start and finish; not worth blocking 17 other sources.
3. **CoX C1/C2 split.** `XERICS_TALISMAN` mounted-in-POH gates the C1 "teleport from house" branch; the handheld charged talisman is an *inventory* item covered by the existing `requiredItemIds` step shape, **not** by C2. The C2 cell on row 1 is therefore rated N — the prior draft's Y was a mis-classification.
4. **Charge-aware routing is a B1 requirement, not out-of-scope.** Drakan's medallion (rows 2/3/16/17), charged ring of shadows (rows 9-12), and Quetzal whistle (row 15) are all jewellery-class items with discrete charge counts. `EquippedItemState.hasEquipped(itemId)` returns `true` even when the item is at 0 charges — meaning a naive "medallion equipped → use it" branch will mis-route the player to a useless teleport. **B1 must author each C2 branch with a no-equipped fallback alternative** (e.g. Canifis teleport for ToB if the medallion route fails to fire). Treat this as a hard B1 requirement; do not punt to a follow-up. Charge-quantity detection itself remains a separate detector (out of C6).
5. **Source-level vs. step-level `requirements.diaries`.** Should the Vorkath Fremennik-Hard shortcut be expressed on the boss `worldX/worldY` waypoint (source-level), on a travel step (step-level), or as a `conditionalAlternatives` entry on the first guidance step? Recommendation: step-level `conditionalAlternatives` for consistency with the existing fairy-ring / mounted-glory branch pattern.
6. **Loadout-only equips are rated N for C2.** Bandos godsword, Saradomin items, Zamorakian spear, Armadyl crossbow, Salve amulet (e), ava's assembler, sigils, etc. do not change *travel* routing — they change combat loadout. C2 wiring is scoped to travel-affecting equips (teleport jewellery, charged transport items). Sigil/loadout surfaces are a follow-up.
7. **B0 schema PR — backwards compatibility.** New fields (`pohTeleports`, `equippedItemIds`) must be `@Nullable` on `SourceRequirements` so existing JSON parses unchanged. The Jackson model already tolerates unknown fields per `data-authoring.md` convention; confirm during B0 implementation. Reuse the same flat-string enum-name shape `diaries` uses (e.g. `"JEWELLERY_BOX_FANCY"`) rather than nested objects.
8. **Top-20 vs. top-21+ split.** Sources 21-40 of the D2 list (slayer + classic + clues + minigames) are where C4 (skill-cape perks) and the remaining C1/C3 applications cluster. A follow-up C6.5 / C7 milestone should pick those up; do not let it leak into C6.
9. **B1c collapsed — 5 source candidates were inventory-not-equipped.** A B1c follow-up was originally scoped to cover 5 additional C2 cases (rows 4, 14, 15, 19, 20). Investigation showed all five teleport vectors are *inventory* items, not equipment:
   - **Row 4 ToA — Pharaoh's sceptre.** The sceptre is a wieldable polestaff (requires 30 Atk / 30 Mag), but its Jaltevas teleport is invoked from the inventory right-click menu and works regardless of wield state. The Necropolis obelisk unlock itself requires the sceptre "in the inventory" per the [Necropolis](https://oldschool.runescape.wiki/w/Necropolis) wiki page. RuneLite ItemID also enumerates six sceptre variants (`PHARAOHS_SCEPTRE` 26945/26946/26947, `PHARAOHS_SCEPTRE_CHARGED` 26948/26949, `PHARAOHS_SCEPTRE_CHARGED_INITIAL` 26950) — even if we wanted to treat the wielded variant as an equipped-item branch, the multi-tier OR-semantic would be a second schema gap layered on top of the inventory-vs-equipped gap.
   - **Row 14 Zulrah — Zul-andra teleport scroll.** Inventory teleport scroll, never equipped.
   - **Row 15 Phantom Muspah — Quetzal whistle.** Inventory whistle item; not wieldable in the teleport-active form.
   - **Rows 19/20 Gauntlet / Corrupted Gauntlet — Crystal teleport seed.** Inventory seed, never equipped.

   The corrected C2 row count is **8** (rows 2, 3, 9, 10, 11, 12, 16, 17), already covered by the merged B1a / B1b PRs (#625 / #626). **B1c is dropped from the C6 plan.** The five mis-classified sources are deferred to a future C-extension milestone that adds:
   - `requirements.inventoryItemIds: [int]` (AND-semantic, mirroring the existing `equippedItemIds` field shape) for the single-ID inventory-teleport case (Zul-andra scroll, Quetzal whistle, Crystal teleport seed).
   - `requirements.inventoryItemIdsAny: [int]` (OR-semantic, evaluator `anyMatch`) for the multi-charge-tier case (Pharaoh's sceptre).

   That extension is out of C6 scope and should ship as its own Tier B + data milestone alongside an `InventoryItemState` C-detector. The five sources stay rated N in §2 until that extension lands.

---

## 6. References

- [Tier D2 scoping — top-40 sources](d2-top-40-scoping.md) (source list inheritance)
- [JSON Schema Reference](schema-reference.md) (existing `requirements` + `conditionalAlternatives` surface)
- [Tier C status log](../ROADMAP.md#tier-c--player-aware-guidance) (C1-C5 PR numbers, C6 status)
- PR [#461](https://github.com/cha-ndler/collection-log-helper/pull/461) — landed C3 `DiaryTierState`
- PR [#462](https://github.com/cha-ndler/collection-log-helper/pull/462) — landed C5 `PlayerQuestProgressState`
- PR [#463](https://github.com/cha-ndler/collection-log-helper/pull/463) — landed C2 `EquippedItemState`
- PR [#470](https://github.com/cha-ndler/collection-log-helper/pull/470) — landed C1 `PohTeleportInventory`
- PR [#471](https://github.com/cha-ndler/collection-log-helper/pull/471) — landed C4 `SkillCapePerkState`
- PR [#616](https://github.com/cha-ndler/collection-log-helper/pull/616) — wired `@Subscribe` lifecycle so detectors refresh live
- `com.collectionloghelper.player.PohTeleportInventory` + `PohTeleport` (C1)
- `com.collectionloghelper.player.EquippedItemState` (C2)
- `com.collectionloghelper.player.DiaryTierState` + `DiaryRegion` + `DiaryTier` (C3)
- `com.collectionloghelper.player.SkillCapePerkState` + `SkillCapePerk` (C4)
- `com.collectionloghelper.player.PlayerQuestProgressState` + `QuestSubMilestone` (C5)
- `com.collectionloghelper.data.SourceRequirements` (carries `quests`, `skills`, `diaries`)
- `com.collectionloghelper.data.ConditionalAlternative` (carries `SourceRequirements` field)
