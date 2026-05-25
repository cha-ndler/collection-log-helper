# Tier D5 scoping -- Tier B/C branch-model retrofit across the full source set

> Picks up after Tier D4 closes (all 144 long-tail sources at the full or relaxed bar). D5
> does NOT re-author per-source guidance. Its job is to layer composable conditions and
> conditional alternatives on top of every source in `drop_rates.json` that has a
> player-state-dependent route, method, or access gate -- and that has not already been
> wired by Tier C6 or the C-extension inventory-teleport milestone.

This file is the *plan*. Follow-up PRs deliver batches of `drop_rates.json` wiring against
the existing schema, grouped by detector type and source category. No new guidance text is
authored; no schema fields are introduced unless an open question in SS5 forces one.

---

## 1. Selection criteria

A source is a **D5 retrofit candidate** if ALL of the following hold:

| Criterion | Rule |
|---|---|
| **A. In `drop_rates.json`** | Source exists in the file at D5 branch tip. New sources are out of scope. |
| **B. Not already wired by C6 or the C-extension** | C6 wired the top-20 sources (C2 equipped items x8, C1 POH teleports x8, C3 diaries x6, C5 quest gates x14 via existing `requirements.quests`). The Wave-7c C-extension wired 5 inventory-teleport sources (ToA Pharaoh's sceptre, Zulrah, Phantom Muspah, Gauntlet x2) via `inventoryItemIds`/`inventoryItemIdsAny`. Sources already fully wired are out of scope. |
| **C. Has a player-state-dependent route or method** | At least one of these applies: (1) a POH-fixture teleport shortens the travel path vs the base step; (2) an equipped or inventory-held item teleports the player directly; (3) an Achievement Diary tier unlocks a shorter route or removes a gate; (4) a quest unlock (FINISHED or sub-milestone) gates access or materially changes the route; (5) a skill-cape perk provides a relevant travel or access benefit. Sources with a single canonical method reachable by any account are **out of scope**. |
| **D. The branch is expressible in the existing schema** | The conditional alternative can be declared using the current `SourceRequirements` fields: `pohTeleports`, `equippedItemIds`, `inventoryItemIds`, `inventoryItemIdsAny`, `diaries`, `quests`, `skills`. Sources that need a new detector or field are flagged in SS5 rather than batched. |

**Out-of-scope by definition:**
- Sources whose only multi-method variation is combat gear or food loadout (no routing effect) -- these are the "loadout-only" N cases from C6 SS5 Q6.
- Sources already fully wired (the 20 C6 sources + 5 C-extension inventory-teleport sources).
- Sources that genuinely have a single access path regardless of account state (e.g., Clue sub-tables, many salvage tables, Creature Creation).

---

## 2. Candidate inventory

### 2.1 Script -- surface D5 retrofit candidates

The script below reads `drop_rates.json` and emits every source that has at least one
guidance step with no `conditionalAlternatives` entry, filtered to exclude the already-wired
C6 + C-extension set. Run from repo root (requires `jq` 1.6+):

```bash
# Already-wired set (C6 top-20 + C-extension inventory-teleport wave)
WIRED='^(Chambers of Xeric|Theatre of Blood|Theatre of Blood \(Hard Mode\)|Tombs of Amascut|General Graardor|Commander Zilyana|K.?ril Tsutsaroth|Kree.?arra|Vardorvis|Duke Sucellus|The Leviathan|The Whisperer|Vorkath|Zulrah|Phantom Muspah|The Nightmare|Phosani.?s Nightmare|Corporeal Beast|The Gauntlet|Corrupted Gauntlet)$'

jq --arg wired "$WIRED" '
  map(
    select(.name | test($wired) | not)
  )
  | map(select(
      .guidanceSteps
      | if . == null then false
        else any(
          .;
          (.conditionalAlternatives == null or (.conditionalAlternatives | length) == 0)
        )
        end
    ))
  | group_by(.category)
  | map({
      category: .[0].category,
      count: length,
      names: [.[].name]
    })
' src/main/resources/com/collectionloghelper/drop_rates.json
```

The secondary filter (a source that does have alternatives but only on some steps) is not
captured by this script alone. A second pass groups by detector applicability:

```bash
# Surface sources that already have at least one conditionalAlternative
# (partial wiring -- may need additional branches)
jq --arg wired "$WIRED" '
  map(
    select(.name | test($wired) | not)
  )
  | map(select(
      .guidanceSteps
      | if . == null then false
        else any(.; (.conditionalAlternatives | length) > 0)
        end
    ))
  | [.[].name]
' src/main/resources/com/collectionloghelper/drop_rates.json
```

Run both and union the results; manually triage each name against the SS1 criteria.

### 2.2 Category and detector breakdown

The table below is the **pre-triage estimate** derived from the D4 source inventory and the
C6 applicability analysis. Exact counts are finalized by running the SS2.1 script against
the D4-complete `drop_rates.json`.

| Detector | Schema field(s) | Estimated candidate sources | Typical application |
|---|---|---|---|
| **POH teleport** | `pohTeleports` | ~25-35 | Mounted glory / jewellery box / spirit tree / fairy ring / portal nexus shortcuts for slayer, boss, skilling, and wilderness sources not in the C6 top-20 |
| **Diary shortcut** | `diaries` | ~20-30 | Ardougne, Morytania, Kandarin, Lumbridge, Falador, Karamja, Desert, Varrock, Western Provinces, Wilderness shortcuts across mid-tier boss + skilling sources |
| **Equipped item** | `equippedItemIds` | ~10-15 | Teleport jewellery (Ring of Dueling, Skills necklace, Combat bracelet, Amulet of glory) for sources not covered by C6 -- note charge-aware fallback is mandatory for all charged equips |
| **Inventory teleport** | `inventoryItemIds`, `inventoryItemIdsAny` | ~10-15 | Teleport tablets, Slayer rings, Xeric's talisman (handheld), Iorwerth teleport crystal, and other right-click-from-inventory vectors for sources outside the C-extension set |
| **Quest gate** | `quests` | ~15-25 | Boss-instance or route gates not yet in `requirements.quests` (e.g., partial diary-quest interactions, secondary route unlocks like Elf camp Prifddinas approach for non-SOTE sources) |
| **Skill-cape perk** | (see SS5 (d)) | ~5-10 | Crafting cape -> Crafting Guild, Farming cape -> any farm patch, Construction cape -> POH-from-anywhere, Slayer cape -> Slayer masters; highly relevant for skilling sources missed by C6 |

**Total pre-triage estimate: ~85-130 sources across the 225 in the file**, after subtracting
the 25 already-wired (20 C6 + 5 C-extension). The exact count depends on the D4-final
snapshot and the triage pass; this estimate collapses to the smallest valid set after
applying the SS1 criteria strictly.

**C6 was the top-20.** D5 is the long tail: ~160-185 of the 200 non-C6 sources, of which
roughly half will satisfy criterion C (have a player-state-dependent route).

---

## 3. Batching plan

Eight batches, grouped by **detector type** and then by source category within each
detector. Batch size: 8-15 sources. Each batch is one PR against `drop_rates.json` with
companion regression-test additions. No batch has a hard dependency on another except where
the same step is being amended by two detectors (in which case the second batch stacks its
`requirements` clause onto the alternative authored by the first -- same pattern as C6 B3
stacking diary clauses onto B2 POH alternatives).

| Batch | Theme | Detector | Est. sources | Depth note |
|---|---|---|---|---|
| **D5-A** | POH-teleport cluster -- slayer + mid-tier boss | `pohTeleports` | ~10-12 | Mounted glory / jewellery box shortcuts for slayer-dungeon arrivals (Abyssal Demon, Aberrant Spectre, Smoke Devil, Nechryael, Dark Beast), mid-tier bosses (Sarachnis, Scurrius, Giant Mole, Obor, Bryophyta) where a fancy-jewellery-box or mounted-glory alternative cuts travel time meaningfully. Each source gets one `conditionalAlternatives` entry per faster POH path, with the unrestricted base step as fallback. |
| **D5-B** | POH-teleport cluster -- skilling + wilderness | `pohTeleports` | ~8-10 | Skilling sources (Rooftop Agility courses, Volcanic Mine, Zalcano, Motherlode Mine, Hallowed Sepulchre) where spirit tree / fairy ring / portal nexus changes arrival. Wilderness sources (Revenant Caves, Catacombs of Kourend) where mounted glory -> Edgeville is the non-wilderness shortcut. Charge-aware fallback not required for POH (mount is static). |
| **D5-C** | Diary-shortcut cluster -- combat | `diaries` | ~10-12 | Diary-gated route improvements for mid-tier boss sources not in C6: Kalphite Queen (Desert Hard), King Black Dragon (Wilderness Diaries tiers), Dagannoth Kings (Fremennik Hard -> Waterbirth island shortcut), Alchemical Hydra (Kourend Elite removes travel gate), Cerberus (Falador Hard), Grotesque Guardians (N/A -- no diary shortcut; check triage). Each uses the existing `diaries` field at the conditional-alternative level with the unrestricted base step as fallback. |
| **D5-D** | Diary-shortcut cluster -- skilling + minigame | `diaries` | ~8-10 | Diary-gated shortcuts for skilling/minigame sources: Tithe Farm (Hosidius favour -> skip intro, covered by existing requirements but not as a conditional alternative travel branch), Guardians of the Rift (Ardougne Medium -> Salve Graveyard teleport shortcut), Trouble Brewing (Karamja Hard), Pest Control (Ardougne Medium -> shortcut to outpost). Stack diary clauses on top of any D5-A/B POH alternatives where the same step benefits from both. |
| **D5-E** | Equipped-item cluster -- teleport jewellery | `equippedItemIds` | ~10-12 | Teleport jewellery routes for sources outside C6: Ring of Dueling (Pest Control, Castle Wars, Ranging Guild, Fist of Guthix); Skills necklace (Fishing Guild, Crafting Guild, Mining Guild, Woodcutting Guild); Combat bracelet (Warriors' Guild non-GWD use cases, Ranging Guild); Amulet of glory (Edgeville approach for Revenants, Lava Dragons, Brutal blue dragons). Each charged equip requires a non-equipped fallback alternative (SS5 (b)). |
| **D5-F** | Inventory-teleport cluster | `inventoryItemIds`, `inventoryItemIdsAny` | ~10-12 | Inventory-held teleport vectors: Slayer ring -> Slayer master / dungeon for all slayer sources not in C-extension; Xeric's talisman (handheld) for Hosidius / Kourend sources; Ectophial for Morytania sources (Port Phasmatys approach); Necklace of passage (Wizards' Tower, Outpost) for skilling and minigame arrivals. Use `inventoryItemIdsAny` for multi-charge-tier items (Slayer ring 1-8). |
| **D5-G** | Quest-gate + sub-milestone cluster | `quests` | ~12-15 | Quest gates not yet in `requirements.quests` and not covered by C6: Shilo Village (Shilo Village quest for Gem Rocks source), Underground Pass (Underground Pass quest for cave travel), Jungle Potion (for Karamja-interior skilling), Spirit of the Elid (Nardah bank shortcut for Desert sources), Ernest the Chicken (Draynor Manor sources), Mountain Daughter (Fremennik snowflake route). Also: partial sub-milestones where `FINISHED` is over-restrictive -- flag each for the potential `questMilestones` extension (SS5 (d)). |
| **D5-H** | Skill-cape perk cluster + stacking passes | `requirements.skillCapePerks` (new field? -- see SS5 (d)) | ~8-10 | Skill-cape travel perks for skilling sources: Crafting cape -> Crafting Guild; Farming cape -> any patch (Fruit Trees, Hespori); Construction cape -> POH-from-anywhere as fallback to `pohTeleports`; Slayer cape -> any slayer master; Hunter cape -> any Hunter area. Stack where the same step already has a D5-A/B POH alternative. If `skillCapePerks` requires a new schema field, this batch is blocked on a Tier B schema PR first (parallel with D5-F). |

**Estimated PR count: 8-12 PRs** (one per batch, with possible splits on D5-C and D5-G if
diff size exceeds 400 lines).

**Ordering rationale:**
- D5-A and D5-B (POH) first -- `pohTeleports` field already in schema via B0/C6; no new
  Java required. Highest-volume detector by source count.
- D5-C and D5-D (diaries) second -- `diaries` field also pre-existing; can run in parallel
  with D5-A/B since they touch different sources.
- D5-E (equipped items) third -- `equippedItemIds` pre-existing; charge-aware fallback
  authoring adds complexity, so placed after the simpler detector batches.
- D5-F (inventory teleports) fourth -- `inventoryItemIds`/`inventoryItemIdsAny` pre-existing
  via the C-extension; the author must know both OR and AND semantics before batching.
- D5-G (quest gates) fifth -- minimal schema dependency; touches the largest variety of
  source categories so benefits from D5-A through D5-F authoring lessons.
- D5-H (skill-cape perks) last -- potentially blocked on a new schema field; placed at the
  end so all other batches can proceed if the schema question in SS5 (d) is unresolved.

D5-C can partially begin in parallel with D5-A/B since `diaries` needs no schema work.
D5-G can begin in parallel with D5-E/F for the same reason.

---

## 4. Per-source audit method

Same LEAN script-driven method as D4 SS5 and D3 SS4. No per-source score table is
maintained inline; contributors regenerate the gap matrix on demand for their batch.

### 4.1 Pre-batch triage checklist

For each source in the batch target list, confirm:

1. Run SS2.1 scripts and confirm the source appears in the unwired or partially-wired set.
2. Check wiki travel table: does any player-state-dependent shortcut exist?
   - Teleport jewellery? -> `equippedItemIds` or `inventoryItemIds`
   - POH fixture? -> `pohTeleports`
   - Diary tier? -> `diaries`
   - Quest unlock? -> `quests`
   - Skill-cape? -> `skillCapePerks` (SS5 (d))
3. For each identified branch, confirm the alternative is the FASTEST meaningful route (not
   just any route). Don't add an alternative that saves fewer than ~30 seconds -- the branch
   clutter is not worth it.
4. For chargeable equips (`equippedItemIds`), confirm a non-equipped fallback is authored
   immediately below the gated alternative in the `conditionalAlternatives` array.
5. For `inventoryItemIdsAny`, confirm all charge-tier item IDs are enumerated (use
   `runelite_id_lookup` MCP tool to get the full ID list for multi-tier items like Slayer
   ring 1-8).
6. Run `inject_density_check` after each source addition to confirm the source does not
   exceed the per-step alternative-density limit.
7. Run `data_ascii_lint` to confirm no smart quotes or em-dashes entered the description
   or travelTip overrides.

### 4.2 JSON alternative shape (reference)

The `conditionalAlternatives` array on a step must be ordered fastest-route-first with the
unrestricted base step as the implicit fallback. Each alternative overrides only the fields
that differ from the base step; null fields fall through to the parent step.

```json
{
  "description": "Travel to source (default: walk from nearest lodestone)",
  "completionCondition": "ARRIVE_AT_TILE",
  "worldX": 3000,
  "worldY": 3400,
  "worldPlane": 0,
  "conditionalAlternatives": [
    {
      "requirements": {
        "pohTeleports": ["JEWELLERY_BOX_FANCY"],
        "diaries": ["KANDARIN_HARD"]
      },
      "description": "Jewellery box -> Skills necklace -> Fishing Guild; Kandarin Hard unlocks shortcut",
      "travelTip": "POH jewellery box -> Skills necklace -> Fishing Guild"
    },
    {
      "requirements": {
        "equippedItemIds": [11090]
      },
      "description": "Skills necklace teleport to Fishing Guild directly",
      "travelTip": "Skills necklace -> Fishing Guild"
    }
  ]
}
```

The `requirements` block may combine multiple detector fields (AND semantics across all
fields within one block). For OR semantics between two mutually exclusive routes, use
separate alternatives -- the first matching one wins.

---

## 5. Open questions

### (a) Alternative-bloat and inject-density limits

C6 introduced up to 3 alternatives per step (a fastest branch, a medium branch, and the
base fallback). D5 covers ~85-130 sources each potentially receiving 1-3 new alternatives.
The `inject_density_check` MCP tool enforces a per-source cap, but the cap value and its
interaction with already-wired C6 alternatives (which themselves count toward the cap) is
not documented.

**Resolution needed before D5-A:** confirm the current inject-density cap (alternatives per
step, and per source total), and confirm that stacking D5 diary alternatives on top of C6
POH alternatives on the same step (as D5-D proposes) does not breach it. If the cap is too
low, either raise it (Tier B schema PR) or cap the number of stacked detectors per step.

### (b) Charge-aware fallback rule for equipped items

`EquippedItemState.hasEquipped(int)` returns `true` even when the item is at 0 charges
(documented in C6 SS5 Q4 and the `SourceRequirements.equippedItemIds` Javadoc). Every
D5-E and D5-H alternative that gates on a chargeable equip (Ring of Dueling, Combat
bracelet, Amulet of glory, Skills necklace, Slayer helmet with Slayer ring access) **must**
be paired with a non-equipped fallback alternative immediately below it.

**Resolution needed before D5-E:** decide whether charged-jewellery items (Ring of Dueling
1-8, Amulet of glory 1-4) should be gated on the OR-semantic `inventoryItemIdsAny` (which
does not face the 0-charge problem since a depleted ring would have a different item ID)
rather than `equippedItemIds`. Checking the inventory for the highest-charge variant and
using `equippedItemIds` for the worn variant may require two alternatives at the same step
(inventory-held path + equipped path), which doubles the authoring cost. Document the
chosen pattern before D5-E opens.

### (c) Regression-test strategy for branch coverage

C6 regression tests (`C6B1aRegressionTest`, `C6B2PohTeleportRegressionTest`, etc.) each
validate a single detector across its wired sources. D5 wires the same detectors across a
much larger source set.

**Resolution needed before D5-A:** decide whether D5 regression tests are:
- (i) per-batch additions to the existing C6 test classes (extending the `@ParameterizedTest`
  source lists), or
- (ii) new D5-prefixed test classes that validate only the D5 wired sources.

Option (i) keeps the test footprint smaller and avoids duplication; option (ii) makes D5
PRs self-contained and easier to review in isolation. Recommendation: (i) for detectors
shared with C6 (POH, equipped, diaries); (ii) for the new detector surface (skill-cape
perks) if a new field is added.

Also: decide whether a D5 "full coverage" regression test should assert that every source
meeting the SS1 triage criteria has at least one conditional alternative. This would catch
any source that was triaged as a candidate but missed in a batch.

### (d) New schema fields: skill-cape perks and quest sub-milestones

Two potential gaps that D5 may surface:

**Skill-cape perks** (`SkillCapePerkState`, C4 detector): C6 found zero top-20 applications
for C4. D5 targets skilling sources where Crafting cape, Farming cape, Construction cape,
and Slayer cape perks matter. The current `SourceRequirements` has no `skillCapePerks`
field. Options:
- (A) Model skill-cape access as a `pohTeleports` entry (Construction cape -> POH-from-
  anywhere is semantically equivalent to a POH teleport and could reuse that field).
- (B) Add `requirements.skillCapePerks: ["CRAFTING_CAPE", "FARMING_CAPE", ...]` as a new
  field on `SourceRequirements`, matching `SkillCapePerkState` enum names.

Option (A) avoids a schema PR for the Construction cape case but is semantically wrong for
Crafting/Farming/Slayer capes (those are not POH fixtures). Option (B) is correct but
requires a Tier B schema PR before D5-H. **Recommend (B); block D5-H on the schema PR.**

**Quest sub-milestones** (`PlayerQuestProgressState`, C5 detector): the existing
`requirements.quests` field uses `FINISHED` semantics. D5-G will encounter cases where a
*started* or *partially-completed* quest unlocks a route (e.g., Fairytale II started for
the AKQ fairy ring route -- the same over-restriction flagged in C6 SS5 Q2). The C6
recommendation was to defer a `requirements.questMilestones` field; D5-G is the natural
trigger to scope that PR. **Recommend: scope `questMilestones` as a separate Tier B PR,
opened in parallel with D5-G, and block D5-G alternatives that strictly need sub-milestone
precision on that PR.** Alternatives where `FINISHED` is a workable (if over-restrictive)
proxy proceed without the new field.

**If neither new field is needed after the D5 triage pass,** the schema is sufficient as-is
and D5 is pure data wiring. Confirm after D5-A through D5-F complete.

---

## 6. References

- [Tier C6 top-20 player-state wiring scoping](c6-top-20-player-state-wiring-scoping.md)
  -- precedent for conditional-alternative batching by detector type; defines the schema
  fields D5 reuses; establishes the charge-aware-fallback requirement.
- [ROADMAP Tier B SS B1](../ROADMAP.md#tier-b--guidance-depth-parity-with-quest-helper)
  -- composable completion conditions (the `CompletionCondition` enum + `ConditionalAlternative`
  tree that D5 populates with data).
- [ROADMAP Tier B SS B4](../ROADMAP.md#tier-b--guidance-depth-parity-with-quest-helper)
  -- alternative-method modelling; D5's branch-retrofit is the data expression of B4's
  schema infrastructure.
- [ROADMAP Tier C SS C6 closure note](../ROADMAP.md#tier-c--player-aware-guidance)
  -- C6 batch plan, per-detector wiring, and the C-extension inventory-teleport milestone
  that preceded D5.
- [ROADMAP D5 status row](../ROADMAP.md#tier-d--coverage-breadth) -- D5 milestone row
  (`status: planned`); update to `in-progress` when D5-A opens.
- [D4 long-tail scoping SS6 Q7](d4-long-tail-scoping.md#q7----d5-handoff)
  -- explicit handoff note confirming D5 layers branches on top of D4-authored guidance,
  not re-authors it.
- [Deep-guidance bar](deep-guidance-bar.md)
  -- the 10-element (or relaxed 5-element long-tail) authoring bar D4 sources already
  satisfy; D5 does not re-apply the bar.
- [JSON Schema Reference](schema-reference.md) -- `SourceRequirements` fields, their
  semantics, and the `ConditionalAlternative` override model.
- `com.collectionloghelper.data.SourceRequirements` -- `pohTeleports`, `equippedItemIds`,
  `inventoryItemIds`, `inventoryItemIdsAny`, `diaries`, `quests`, `skills`.
- `com.collectionloghelper.data.ConditionalAlternative` -- override fields and
  `nestedAlternatives` (B3).
- `com.collectionloghelper.data.RequirementsChecker` -- evaluator that resolves
  `SourceRequirements` against live player state.
