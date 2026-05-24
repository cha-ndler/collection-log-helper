# Tier D3 scoping -- mid-tier sources deep-guidance pass

> Picks up after [Tier D2](d2-top-40-scoping.md) closed 40 top-farmed sources to the [deep-guidance bar](deep-guidance-bar.md). D3 targets sources ranked 41-80 in realistic farming popularity -- the "second wave" of routinely-farmed content (recent boss releases, mid-tier slayer bosses, popular skilling minigames, and the lower-tier clue scrolls).

This file is the *plan*. Each follow-up PR delivers one batch of 5-8 source fixes against `drop_rates.json`, with tests and an in-game-validation note, mirroring the [D2 per-batch fix template](d2-top-40-scoping.md#4-per-batch-fix-template).

---

## 1. Selection criteria

"Mid-tier sources" is operationalized as the next 40 sources after the D2 top-40, drawn from the same signal union. A source qualifies for D3 if it appears in **two or more** of the signals below and is not already on the D2 list, OR it is a 2025-2026 boss release that the community treats as mainstream PvM content but did not yet have enough audit data to qualify for D2.

| Signal | Source | What it tells us |
|---|---|---|
| **A. Recent boss releases** | Araxxor (2024), Hueycoatl (2024), Yama (2025), Doom of Mokhaiotl (2025), Royal Titans (2025), Amoxliatl (2024), Sol Heredit (2024), Awakened DT2 variants (2024) | Post-D2 game updates that already have widespread player engagement but were not in the original D2 cut. |
| **B. TempleOSRS rank 30-70** | TempleOSRS boss popularity hiscores rows 30 through 70 | Empirical "second wave" boss popularity, below the Tier-1 endgame cluster but still routinely farmed. |
| **C. Mid-tier slayer task frequency** | Wiki Duradel + Konar + Nieve task-weight tables, filtered to tasks that yield rare-table drops | Slayer bosses below the D2 cut (Hydra is in D2; the regular Hydra slayer task, Demonic gorillas, Wyrms, Drakes, Tormented Demons, etc. are not). |
| **D. Lower clue tiers** | Beginner / Easy / Medium Treasure Trails + Shared Treasure Trail Rewards | Carried over from D2's deferral list. These are heavily farmed via skilling task drops and master-clue chains but have shallower rare tables than Hard/Elite/Master. |
| **E. Mid-popularity minigames** | Hallowed Sepulchre, Mahogany Homes, Guardians of the Rift, Shades of Mort'ton, Giants' Foundry, Volcanic Mine, Mastering Mixology, Vale Totems, Pest Control, Soul Wars | Skilling/minigame content with active rare-drop hunts the community currently farms. |
| **F. "Completes a category" rationale** | Sarachnis, Skotizo, Thermonuclear smoke devil, Sol Heredit, Inferno, Fight Caves, Nex, Scurrius, Perilous Moons | Standalone bosses outside slayer that round out the BOSSES category to "every boss in the game has deep guidance" -- a stated D3 milestone goal. |
| **G. RuneLite plugin author intuition** (`osrs-expert` / `runelite-plugin-dev`) | This repo's skill set | Sanity-checks the empirical list against current 2026 meta -- confirms e.g. Araxxor / Yama / Mokhaiotl are routinely farmed despite recent release. |

**Excluded from D3 (deferred to D4 long-tail):**

- All `OTHER` category sources except the two mid-popularity ones explicitly listed (Catacombs of Kourend, Revenants). Random Events, Salvage variants, Boat Combat, Creature Creation, Sailing Misc, etc. all defer to D4.
- All `SKILLING` category sources (Motherlode Mine, Aerial Fishing, Rooftop Agility, Hunter, Runecrafting, etc.). These have shallow rare tables and benefit more from a unified skilling-guidance pass in D4.
- Long-tail minigames (Castle Wars, LMS, Trouble Brewing, Barbarian Assault, Fishing Trawler, Rogues' Den, Tithe Farm, Brimhaven Agility Arena, Mage Training Arena, Temple Trekking, Gnome Restaurant, Barracuda Trials). D4.
- All remaining slayer monsters not listed in section 2 (Aberrant Spectre, Bloodveld, Basilisk variants, Jelly, Pyrefiend, etc.). D4.
- Sub-pages of clue rewards (Hard / Elite / Master Treasure Trail Rewards Rare, Scroll Cases, The Mimic). Already covered by D2's clue-tier pass at the source level; rare-reward sub-tables defer to D4.

**Popularity-tier definition:**

- **Tier 2-late** = "Mainstream PvM second wave + popular minigames." Recent boss releases, mid-tier slayer bosses, top non-skilling minigames. ~25 sources.
- **Tier 3** = "Popular but lower-engagement-per-account." Lower clue tiers, niche bosses, mid-popularity skilling minigames. ~15 sources.

---

## 2. The 40-source list

Ranked roughly by popularity within the mid-tier band. Exact within-band ordering is judgement-call -- community popularity data thins out past TempleOSRS rank ~50 -- so the rank column is grouped (41-50, 51-60, 61-70, 71-80) rather than absolute.

| # | Source (drop_rates.json `name`) | Category | KC tier | Content origin |
|---|---|---|---|---|
| 41 | Araxxor                              | BOSSES    | Tier 2-late | 2024 boss release |
| 42 | Tormented Demons                     | SLAYER    | Tier 2-late | 2024 rework |
| 43 | Nex                                  | BOSSES    | Tier 2-late | Original GWD2 release |
| 44 | The Hueycoatl                        | BOSSES    | Tier 2-late | 2024 boss release |
| 45 | Yama                                 | BOSSES    | Tier 2-late | 2025 boss release |
| 46 | Doom of Mokhaiotl                    | BOSSES    | Tier 2-late | 2025 boss release |
| 47 | Royal Titans                         | BOSSES    | Tier 2-late | 2025 mid-level boss |
| 48 | Sol Heredit                          | BOSSES    | Tier 2-late | 2024 Colosseum boss |
| 49 | The Inferno                          | BOSSES    | Tier 2-late | Capstone PvM (Inferno cape) |
| 50 | The Fight Caves                      | BOSSES    | Tier 2-late | Fire cape grind |
| 51 | Vardorvis (Awakened)                 | BOSSES    | Tier 2-late | DT2 awakened variant |
| 52 | Duke Sucellus (Awakened)             | BOSSES    | Tier 2-late | DT2 awakened variant |
| 53 | The Leviathan (Awakened)             | BOSSES    | Tier 2-late | DT2 awakened variant |
| 54 | The Whisperer (Awakened)             | BOSSES    | Tier 2-late | DT2 awakened variant |
| 55 | Demonic gorillas                     | SLAYER    | Tier 2-late | Zenyte + ballista uniques |
| 56 | Hydra                                | SLAYER    | Tier 2-late | Pre-Alchemical Hydra task |
| 57 | Skotizo                              | BOSSES    | Tier 2-late | Catacombs totem boss |
| 58 | Sarachnis                            | BOSSES    | Tier 2-late | Sarachnis cudgel grind |
| 59 | Scurrius                             | BOSSES    | Tier 2-late | Mid-game group boss |
| 60 | Thermonuclear smoke devil            | BOSSES    | Tier 2-late | Smoke devil task boss |
| 61 | Perilous Moons                       | BOSSES    | Tier 2-late | Varlamore mid-tier boss trio |
| 62 | Amoxliatl                            | BOSSES    | Tier 2-late | 2024 Varlamore mini-boss |
| 63 | Hespori                              | BOSSES    | Tier 2-late | Farming-gated boss |
| 64 | Zalcano                              | BOSSES    | Tier 2-late | Smithing/Mining group activity |
| 65 | Wyrm                                 | SLAYER    | Tier 3 | Slayer task with skilling pet route |
| 66 | Drake                                | SLAYER    | Tier 3 | Boots of stone tier slayer task |
| 67 | Lizardman shaman                     | SLAYER    | Tier 3 | DWH grind |
| 68 | Skeletal Wyvern                      | SLAYER    | Tier 3 | Granite legs / wyvern visage |
| 69 | Vyrewatch Sentinel                   | SLAYER    | Tier 3 | Blood shard grind |
| 70 | Gargoyle                             | SLAYER    | Tier 3 | Granite maul + pet rock |
| 71 | Hallowed Sepulchre                   | MINIGAMES | Tier 2-late | Agility XP + rare cosmetic grind |
| 72 | Guardians of the Rift                | MINIGAMES | Tier 2-late | RC + lantern / abyssal pet |
| 73 | Mahogany Homes                       | MINIGAMES | Tier 3 | Construction contracts + Amy outfit |
| 74 | Giants' Foundry                      | MINIGAMES | Tier 3 | Smithing rep + double ammo mould |
| 75 | Shades of Mort'ton                   | MINIGAMES | Tier 3 | Shade key rare table |
| 76 | Volcanic Mine                        | MINIGAMES | Tier 3 | Mining XP + dragon pickaxe ornament |
| 77 | Mastering Mixology                   | MINIGAMES | Tier 3 | Herblore minigame |
| 78 | Beginner Treasure Trails             | CLUES     | Tier 3 | Lower clue tier deferred from D2 |
| 79 | Easy Treasure Trails                 | CLUES     | Tier 3 | Lower clue tier deferred from D2 |
| 80 | Medium Treasure Trails               | CLUES     | Tier 3 | Lower clue tier deferred from D2 |

**Selection notes:**

- The four Awakened DT2 variants are batched with the recent boss releases because their guidance is largely a delta on top of the non-Awakened (D2) sources -- same travel route, different fight mechanics.
- Demonic gorillas + Hydra (slayer) + Lizardman shaman + Tormented Demons are the highest-popularity slayer-band tasks not absorbed into D2. The remaining slayer monsters defer to D4.
- Hespori and Zalcano are nominally `BOSSES` but functionally skilling activities; they earn D3 inclusion because their guidance complexity (farming patch / mining-by-running) is non-trivial.
- Beginner / Easy / Medium clues are the only `CLUES` entries; the rare-reward sub-tables (`Hard Treasure Trail Rewards (Rare)`, etc.) stay deferred to D4 since their depth is a function of the parent clue tier's guidance, not their own.

---

## 3. Batching plan

Six batches of 5-8 sources each, grouped by content domain so a single contributor can authoring-pass and in-game-validate each batch in one session. Batches 1-3 deliver highest community-visible value (recent boss releases + Awakened DT2); batches 4-6 fill the slayer / minigame / clue long-tail of the mid-tier.

| Batch | Theme | Sources | Count | Est PRs | Dependencies |
|---|---|---|---|---|---|
| **Batch 1** | 2024-2025 boss releases (combat-instance) | Araxxor, The Hueycoatl, Yama, Doom of Mokhaiotl, Royal Titans, Amoxliatl | 6 | 1 | None. Recent releases may need fresh Wiki/Loghunters drop-rate audits per [D-03](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs). |
| **Batch 2** | DT2 Awakened variants | Vardorvis (Awakened), Duke Sucellus (Awakened), The Leviathan (Awakened), The Whisperer (Awakened) | 4 | 1 | Depends on D2 batch 2 (DT2) for shared travel + Ring of Shadows wiring. Awakened uniques (orbs, virtus, etc.) are mutually-exclusive with non-Awakened -- confirm `mutuallyExclusiveSources` and `independent` flags are set per [Element 9](deep-guidance-bar.md#element-9--drop-rate-confidence-wiki-vs-templeosrs-vs-community). |
| **Batch 3** | Capstone PvM + nostalgic instances | Sol Heredit, The Inferno, The Fight Caves, Nex | 4 | 1 | None. High per-source authoring complexity (multi-wave / multi-phase); each source carries its own travel + loadout + per-wave strategy notes. |
| **Batch 4** | Mid-tier bosses (instance + slayer-adjacent) | Skotizo, Sarachnis, Scurrius, Thermonuclear smoke devil, Perilous Moons, Hespori, Zalcano | 7 | 1 | None. Shared "single boss room, one prayer, one mechanic" shape; can be authored from Wiki + osrs-expert recall. |
| **Batch 5** | Mid-popularity slayer monsters | Tormented Demons, Demonic gorillas, Hydra, Wyrm, Drake, Lizardman shaman, Skeletal Wyvern, Vyrewatch Sentinel, Gargoyle | 9 (split if needed) | 1-2 | Depends on Tier B `requirements.equippedItemIds` from D2 batch 1a (#623, #625, #626 wired Ring of Shadows / Drakan's medallion). If batch grows past ~7 sources, split into 5a (Tormented Demons + Demonic gorillas + Hydra + Lizardman shaman) and 5b (Wyrm + Drake + Skeletal Wyvern + Vyrewatch Sentinel + Gargoyle). |
| **Batch 6** | Skilling minigames + lower clue tiers | Hallowed Sepulchre, Guardians of the Rift, Mahogany Homes, Giants' Foundry, Shades of Mort'ton, Volcanic Mine, Mastering Mixology, Beginner Treasure Trails, Easy Treasure Trails, Medium Treasure Trails | 10 (split if needed) | 1-2 | Depends on D2 batch 6 patterns for clue-tier loop-detection (E4) and per-cycle strategy notes (E5). Split as 6a (4 minigames with rare-cosmetic grinds) + 6b (3 lower-clue tiers + 3 skilling minigames) if needed. |

**Total**: 6-9 PRs, 40 sources. Estimated authoring time: 1-2 days per batch; full milestone in ~3-4 calendar weeks if batches run in parallel via worktrees.

**Ordering rationale:** Batch 1 first because recent releases are the loudest "no guidance at all" reports. Batch 2 second because it piggybacks on D2's DT2 work and is the cheapest delta. Batch 3 third because capstone PvM is the most visible mastery content. Batches 4-6 are interchangeable; pick by contributor availability of in-game accounts for each domain (e.g., a maxed slayer alt for Batch 5).

---

## 4. Per-source deep-guidance gap audit (LEAN -- sample + script)

Unlike D2, this scoping doc does **not** enumerate per-source element scores for all 40 sources. The D2 table was useful as a one-time prioritization signal; for D3 the cost of maintaining a hand-curated score table outweighs the benefit, since the batching is already settled by content domain. Instead, contributors derive the gap matrix on demand using the script below, and this section provides a *sample* of 5 worst-case and 2 best-case rows for illustration.

### 4.1. Script -- derive the gap matrix for any source list

Run from repo root (requires `jq` 1.6+):

```bash
SOURCES='Araxxor|Yama|Doom of Mokhaiotl|Royal Titans|Tormented Demons|Hespori|Hallowed Sepulchre'
jq --arg pat "$SOURCES" '
  map(select(.name | test($pat)))
  | map({
      name,
      e1_travelTip:       (.travelTip // "" | length > 0),
      e2_autoArrival:     ([.guidanceSteps[]?.completionCondition] | map(select(. == "ARRIVE_AT_TILE" or . == "ARRIVE_AT_ZONE" or . == "PLAYER_ON_PLANE")) | length > 0),
      e3_gearMentioned:   ([.guidanceSteps[]?.description // ""] | join(" ") | test("(?i)protect|prayer|spec|tbow|scythe|blowpipe|range|melee|magic")),
      e4_loopDetected:    ([.guidanceSteps[]?.loopBackToStep] | map(select(. != null)) | length > 0),
      e5_strategyNote:    ([.guidanceSteps[]?.description // ""] | join(" ") | test("(?i)safespot|phase|special|acid|dodge|avoid|flick|stomp")),
      e6_bankReturn:      ([.guidanceSteps[1:][]?.requiredItemIds // []] | flatten | length > 0),
      e7_inventoryLoad:   ((.guidanceSteps[0]?.requiredItemIds // []) | length > 0),
      e8_prereqs:         ((.requirements.quests // []) + (.requirements.skills // []) | length > 0),
      e9_dropRateConf:    ((.items | map(select(.dropRate != null)) | length) >= ((.items | length) / 2 | floor)),
      e10_prCitations:    false
    })
' src/main/resources/com/collectionloghelper/drop_rates.json
```

The script returns a JSON array of objects, one per matched source, with one boolean per deep-guidance element. Aggregate to a score with `add | to_entries | map(select(.value == true)) | length` per row. **E10 is always `false`** -- it is a process artefact (the PR description), not a JSON field, so the maximum observable score remains **9/10**, consistent with the D2 rubric.

### 4.2. Sample -- worst-case (5) and best-case (2)

Sampled on the snapshot at branch tip. Numbers are observable score / 10 (E10 always 0):

| Source | Category | Observed score | Notable gap |
|---|---|---|---|
| Beginner Treasure Trails  | CLUES    | 2/10  | No `travelTip`, no per-cycle strategy note, no rare-table `dropRate` precision -- clue loops authored as one-shot kill-step shape. |
| Easy Treasure Trails      | CLUES    | 2/10  | Same shape as Beginner; loop-detection (E4) missing -- clues are by definition a loop activity. |
| Medium Treasure Trails    | CLUES    | 2/10  | Same shape. The lower clue tiers were stubbed during initial authoring with placeholder rates; need a full E9 confidence pass. |
| Hallowed Sepulchre        | MINIGAMES | 3/10 | Single travel step, no per-floor strategy notes, no `loopBackToStep` on the floor-5 grand-coffin loop. |
| Doom of Mokhaiotl         | BOSSES    | 3/10 | Recent release; current guidance is a stub kill step. No `travelTip`, no prayer/mechanic note, no E8 prereq for the quest gate. |
| Araxxor                   | BOSSES    | 6/10  | Already at near-bar; missing only E5 (Path 1/2/3 specials note) and E10. |
| Vardorvis (Awakened)      | BOSSES    | 6/10  | Inherits D2's Vardorvis scaffold; missing E5 awakened-mechanic note (axe + ring rotation differs from base) and E10. |

The sample is illustrative only -- the canonical method is the section 4.1 script. Each batch PR re-runs the script for the sources it touches and pastes the before/after rows into the PR description.

### 4.3. Why sample-and-script instead of a full table

- The D3 list is selected by domain, not by score; sorting by gap-score adds no batching information.
- The score table is a *snapshot* -- by the time a contributor opens a batch PR, the upstream `drop_rates.json` has likely already changed underneath the table.
- The script invocation in section 4.1 is the single source of truth; the sample table is only here so a reader can see what the output shape looks like without running it.

---

## 5. Open questions / follow-ups

- **Q1 -- Sailing-locked content (Boat Combat, Ocean Encounters, Sea Treasures).** All `OTHER` category, deferred from D3 to D4 per section 1, but Sailing reaches release readiness during the D3 window the deferral may need revisiting. Track in [D-05](../ROADMAP.md#tier-d--coverage-breadth) as a watch-item.
- **Q2 -- Post-2026 boss updates.** If Jagex ships a new boss while D3 is in flight, decide per-release whether to inject into the active batch (if the release is a clear Tier-2-late fit) or queue for D4. Default: queue for D4 to avoid moving-target scoping.
- **Q3 -- Awakened variants and `mutuallyExclusiveSources`.** The four Awakened DT2 sources in Batch 2 need a schema audit to confirm their unique tables are correctly marked mutually-exclusive with the base variant (orbs are awakened-only; virtus is shared). Confirm before batch 2 ships; may surface a small Tier B schema PR.
- **Q4 -- Tier B equipped-item charge guards.** Batches 2 and 5 rely on Ring of Shadows / Drakan's medallion / Salve amulet (e) charge-aware fallbacks. The B1 `equippedItemIds` requirement landed in #623; charge-guard semantics ([D2 scoping section 5 Q4](d2-top-40-scoping.md#5-open-questions--follow-ups) equivalent) are still open. If unresolved when Batch 5 opens, fall back to unconditional `requiredItemIds` and note the limitation.
- **Q5 -- Clue rare-reward sub-tables.** `Beginner / Easy / Medium Treasure Trails` are in D3. Their rare-reward sub-tables (`Hard / Elite / Master Treasure Trail Rewards (Rare)`, `Scroll Cases`, `The Mimic`) stay D4. Confirm the sub-tables are not orphaned in `drop_rates.json` after Batch 6 -- they should still resolve at runtime since they are independent sources.
- **Q6 -- Skilling-source D4 scope creep.** Several Batch 4/6 sources (Hespori, Zalcano, Hallowed Sepulchre, Mahogany Homes) blur the boss-vs-skilling line. Use the category in `drop_rates.json` as the source of truth; if D4 starts and Hespori shows up again in a "skilling deep pass" scoping, drop it from D4 since D3 already covered it.

---

## 6. References

- [Tier D2 top-40 scoping](d2-top-40-scoping.md) -- direct predecessor; D3 mirrors its structure and inherits its per-batch fix template.
- [Deep guidance bar -- the 10 elements](deep-guidance-bar.md) (D1 deliverable)
- [Tier C6 top-20 player-state wiring scoping](c6-top-20-player-state-wiring-scoping.md) -- batching-format precedent for domain-grouped scoping docs.
- [Decision D-03 -- data-sourcing precedence](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs)
- [Tier D status log](../ROADMAP.md#tier-d--coverage-breadth) -- D3 row, currently `status: planned`.
- [JSON Schema Reference](schema-reference.md)
