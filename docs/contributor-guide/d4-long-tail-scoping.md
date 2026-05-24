# Tier D4 scoping -- long-tail sources deep-guidance pass

> Picks up after [Tier D3](d3-mid-tier-scoping.md) closed 41 mid-tier sources to the [deep-guidance bar](deep-guidance-bar.md). D4 targets the long tail: shops, skilling activities, random events, minor minigames, low-engagement slayer monsters, sub-page clue reward tables, and the residual `OTHER` category miscellany -- everything below realistic-top-80 farming popularity.

This file is the *plan*. Each follow-up PR delivers one batch of 6-15 source fixes against `drop_rates.json`, with tests and an in-game-validation note, mirroring the [D2 per-batch fix template](d2-top-40-scoping.md#4-per-batch-fix-template) and [D3 batching cadence](d3-mid-tier-scoping.md#3-batching-plan).

---

## 1. Selection criteria

"Long-tail" is operationalized as **every source in `drop_rates.json` not already covered by D2 (40 sources) or D3 (41 sources)**. Selection is less about popularity ranking and more about *coverage completeness*: the D4 milestone goal is "every authored source in the file meets a documented guidance bar," even if the bar is relaxed for sources where the full 10-element treatment is overkill (see [§6 Q1](#6-open-questions--follow-ups)).

A source qualifies for D4 if **all** of the following hold:

| Criterion | Rule |
|---|---|
| **A. Currently in `drop_rates.json`** | The source exists in the snapshot at branch tip. Sources that would need to be *added* (Wilderness boss reworks, Sailing rare-table backfill, plugin-hub-pending items) are out of scope -- they fall under D5 or a future Tier B authoring milestone. |
| **B. Not in D2 or D3** | The 40 D2 sources (see [d2-top-40-scoping.md §2](d2-top-40-scoping.md#2-the-40-source-list)) and 41 D3 sources (see [d3-mid-tier-scoping.md §2](d3-mid-tier-scoping.md#2-the-40-source-list)) are excluded. |
| **C. Not a synthetic / placeholder source** | Sources with no `items[]`, or with only a single `Placeholder` item, get triaged in [§6 Q2](#6-open-questions--follow-ups) rather than batched for full authoring. |

**Popularity is not a gating signal in D4.** A long-tail slayer monster (Crawling Hand) and a high-popularity wilderness pet (Shellbane Gryphon) sit in the same milestone -- both are "outside the top-80 farming list but still in the file."

**Why no exclusions list?** D2 and D3 both maintained an "excluded, deferred to next tier" section. D4 is the final coverage tier *within the deep-guidance authoring pass*; D5 is a structural retrofit (branch model), not new authoring. So D4 either covers a source or formally relaxes the bar for it -- nothing gets deferred further. The relaxation lever is the proposed "long-tail bar" in [§6 Q1](#6-open-questions--follow-ups).

---

## 2. The D4 candidate list

Derived programmatically by reading every `name` field in `src/main/resources/com/collectionloghelper/drop_rates.json` and subtracting the D2 and D3 lists. Snapshot at branch tip:

| Total in file | D2 covered | D3 covered | **D4 candidates** |
|---|---|---|---|
| 225 | 40 | 41 (40 planned + 1 overshoot) | **144** |

Because the list is large, this doc does **not** enumerate every source name inline. Instead, the canonical D4 candidate list is regenerated on demand from the JSON using the script in [§5.1](#51-script----regenerate-the-d4-candidate-list). The category breakdown below is the load-bearing summary.

---

## 3. Category breakdown

D4's 144 sources, grouped by `category` field, with a one-line description of the typical guidance shape per category.

| Category | Count | Typical guidance shape |
|---|---|---|
| **OTHER**     | 58 | Highest variance category. Includes salvage tables (8), Creature Creation (6), Boat Combat (5 sub-tables), wilderness slayer miscellany (Revenants, Catacombs of Kourend), minor activities (Champion's Challenge, Stronghold of Security, Pyramid Plunder, TzHaar), and rare-table draws (Larran's Big Chest, Brimstone Chest, Elven Crystal Chest, Lost Schematics, Fossil Island Notes). Few of these warrant full kill-loop guidance; most need a one-step "open / interact / hand in" flow. |
| **SLAYER**    | 36 | Low-popularity slayer monsters not in D3. Most have one canonical task-area arrival step + one kill step. Almost all are reachable via standard travel teleports and need no E6 bank-and-return. The aggregate authoring win is small per source, but the count is large, so batching by slayer-master-task-cluster matters. |
| **SKILLING**  | 17 | Pure skilling sources (Motherlode Mine, Aerial Fishing, Rooftop Agility, Shooting Stars, Black Chinchompas, etc.). Guidance shape is "travel to skill site -> loop activity step." E3 (gear) becomes "tier of tool / outfit," E5 (strategy) is usually about XP/hour technique rather than survival, and E4 (loop) is mandatory. |
| **MINIGAMES** | 16 | Long-tail minigames and lower-engagement activities (Castle Wars, LMS, Soul Wars, Tithe Farm, Trouble Brewing, Barbarian Assault, Fishing Trawler, Pest Control, Mage Training Arena, Brimhaven Agility Arena, Rogues' Den, Temple Trekking, Gnome Restaurant (2 variants), Barracuda Trials, Vale Totems). Most need loop detection (E4) and a single per-round strategy note (E5). |
| **BOSSES**    | 9  | Sub-popular wilderness pairs (Chaos Elemental, Chaos Fanatic, Crazy archaeologist, Deranged Archaeologist, Scorpia), low-level instance bosses (Obor, Bryophyta), and recent additions (Brutus, Shellbane Gryphon). Mostly slayer-master-quest gated or wilderness-risk content; standard single-kill shape with E6 key-redirect mattering on Obor / Bryophyta. |
| **CLUES**     | 6  | Rare-reward sub-tables (Hard / Elite / Master Treasure Trail Rewards (Rare), Shared Treasure Trail Rewards, Scroll Cases, The Mimic). These are *sub-pages* of the D2 parent clue tiers; they need E9 drop-rate-confidence audit but not full E1-E8 (guidance lives on the parent). Strong candidate for the relaxed long-tail bar -- see [§6 Q1](#6-open-questions--follow-ups). |
| **RAIDS**     | 3  | Chambers of Xeric (Challenge Mode), Tombs of Amascut (300 Invocation), Tombs of Amascut (500 Invocation). All three are mode variants on D2-covered parents; their guidance is mostly a delta on the base raid (invocation setup, expected-time difference, scaling note). Treat like the DT2 Awakened batch in D3 -- piggyback on the base D2 source. |

**Cross-category observations:**

- **OTHER alone is 40 % of D4.** Its internal heterogeneity is the single biggest scoping risk. Sub-grouping below.
- **Slayer-monster batching needs a master-task lens.** Many low-popularity slayer monsters share a single task area (e.g., several Catacombs of Kourend tasks share the Kourend dungeon arrival step), so batching by *task area* rather than alphabetically reduces duplicate travel-tip authoring.
- **Salvage tables (Barracuda / Fishy / Fremennik / Large / Martial / Opulent / Plundered / Small)** are 8 of the 58 OTHER sources and are all essentially the same shape: "open salvage table for chance at jewel." They batch as one PR with shared template.
- **Creature Creation (6 variants)** all share the Tower of Life travel step and the "feed correct ingredients" interaction; another single-PR shared-template batch.
- **Boat Combat (5 variants: Albatross, Great White Shark, Narwhal, Orcas, Vampyre Kraken)** share Sailing release infrastructure -- watch-item per [§6 Q3](#6-open-questions--follow-ups) since Sailing has not yet released.

---

## 4. Batching plan

Twelve batches grouped by category and sub-cluster, sized 6-15 sources each. Larger than D2/D3 batches because (a) per-source authoring depth is shallower for long-tail content, (b) shared-template sub-clusters (salvage, creature creation) amortize across many sources, and (c) some batches can use the relaxed long-tail bar (see [§6 Q1](#6-open-questions--follow-ups)) which is cheaper per source.

| Batch | Theme | Sources (count) | Est PRs | Depth bar | Notes |
|---|---|---|---|---|---|
| **Batch 1** | Wilderness sub-bosses + low-level instance bosses | Chaos Elemental, Chaos Fanatic, Crazy archaeologist, Deranged Archaeologist, Scorpia, Obor, Bryophyta, Brutus, Shellbane Gryphon (9) | 1 | Full 10-element bar | Boss category; warrants full bar. E6 redirect mandatory on Obor (Giant key) + Bryophyta (Mossy key) -- already present per [deep-guidance-bar §E7 good example](deep-guidance-bar.md#element-7--inventory-loadout). Wilderness bosses need E5 escape-route note. |
| **Batch 2** | Raid mode variants | Chambers of Xeric (Challenge Mode), Tombs of Amascut (300 Invocation), Tombs of Amascut (500 Invocation) (3) | 1 | Delta-on-parent bar | Piggyback on D2 raid scaffolds. Each variant authors only the invocation/CM delta + expected-time + drop-rate adjustment. E1-E8 inherit from parent. |
| **Batch 3** | Skilling -- gathering | Motherlode Mine, Mining (Gemstone Rocks), Woodcutting (Teak Trees), Fishing (Swordfish), Deep Sea Fishing, Aerial Fishing, Cutting Squid, Underwater Crabs (8) | 1 | Full bar with skilling-specific E3/E5 | E3 = tool tier (pickaxe/hatchet/harpoon); E4 = loop mandatory; E5 = XP/hour technique not survival. Shooting Stars excluded -- batched with Star Search-style timed events in Batch 4. |
| **Batch 4** | Skilling -- non-gathering + timed events | Rooftop Agility, Colossal Wyrm Agility, Black Chinchompas, Prifddinas Rabbit, Pickpocketing Darkmeyer Vyre, Thieving (Seed Stalls), Farming (Fruit Trees), Runecrafting (Fire Runes), Shooting Stars (9) | 1-2 | Full bar | Mixed skilling sub-types. Shooting Stars needs E5 note on Star Sprite minigame loop. Farming (Fruit Trees) needs E4 multi-day loop pattern. Split if PR diff gets unwieldy. |
| **Batch 5** | Slayer -- low-level cluster (combat <70 req) | Crawling Hand, Cave Crawler, Cockatrice, Pyrefiend, Mogre, Rockslug, Jelly, Brine Rat, Turoth, Bloodveld, Infernal Mage, Dust Devil, Aberrant Spectre, Kurask (14) | 1-2 | Reduced "slayer-task bar" -- see [§6 Q1](#6-open-questions--follow-ups) | High-volume / low-uniqueness. Most need only E1 travel tip + E8 slayer-level prereq + E9 drop-rate confidence. E5 strategy mostly N/A (no phase mechanics). Heavy use of shared "Slayer Tower" / "Fremennik Slayer Dungeon" travel templates. |
| **Batch 6** | Slayer -- mid-high cluster (combat >=70 req) | Abyssal Demon, Dark Beast, Nechryael, Smoke Devil, Cave Kraken, Cave Horror, Spiritual Mage, Spiritual Mage (Zarosian), Basilisk, Basilisk Knight, Fossil Island Wyvern, Skotizo placeholder check, Warped Creature, Lava Strykewyrm, Araxyte (14-15) | 1-2 | Reduced "slayer-task bar" | Mostly same as Batch 5; called out separately because some have higher pet/unique drop rates that warrant E9 rigor (Abyssal Demon abyssal head, Dark Beast dark bow shard etc.). |
| **Batch 7** | Slayer -- recent + Varlamore additions | Earthen Nagua, Frost Nagua, Sulphur Nagua, Gryphon, Custodian Stalker, Aquanite, Terror Dog, Superior Slayer Monster (8) | 1 | Full bar | Recent (2024-2026) slayer additions tend to have richer rare tables and warrant the full bar. Superior Slayer Monster is the cross-task umbrella source -- needs special handling (the rare table applies to all tasks). |
| **Batch 8** | OTHER -- salvage tables | Barracuda Salvage, Fishy Salvage, Fremennik Salvage, Large Salvage, Martial Salvage, Opulent Salvage, Plundered Salvage, Small Salvage (8) | 1 | Reduced "shop / table bar" | Shared template: "open salvage table -> chance at jewel." E1 = where to obtain the salvage; E9 = drop-rate confidence (mostly Wiki). No E3-E5 (no combat). |
| **Batch 9** | OTHER -- Creature Creation + minor combat NPCs | Creature Creation (6 variants), Cyclopes, Glough's Experiments, Ogress Shaman, Adamant Dragon, Mithril Dragon, Rune Dragon, Waterfiend, Armoured Zombie, Elder Chaos Druids, Mogre check, Chompy Bird Hunting (~14-16) | 1-2 | Full bar for combat NPCs; reduced for Creature Creation | Creature Creation shares Tower of Life travel + ingredient interaction. Adamant/Mithril/Rune Dragons are bossing-adjacent slayer-style. |
| **Batch 10** | OTHER -- rare-table chests + skilling extras | Brimstone Chest, Larran's Big Chest, Elven Crystal Chest, Zombie Pirate Locker, Lost Schematics, Fossil Island Notes, Monkey Backpacks, Camdozaal, Forestry, Hunter Guild, Pyramid Plunder, TzHaar, Catacombs of Kourend, Revenants, Stronghold of Security, Champion's Challenge, My Notes, Miscellaneous, Port Tasks, Shayzien Armour (~20) | 2 | Mixed -- full for Revenants + Pyramid Plunder + Catacombs of Kourend; reduced for "open chest" sources | Heterogeneous OTHER residual. May need a sub-split mid-batch. |
| **Batch 11** | OTHER -- Boat Combat + Sailing-adjacent | Albatross, Great White Shark, Narwhal, Orcas, Vampyre Kraken (Boat Combat), Boat Paints, Ocean Encounters, Sailing Misc, Sea Treasures (9) | 1 | Watch-item bar -- see [§6 Q3](#6-open-questions--follow-ups) | Sailing skill has not released. If still pre-release when batch opens, defer the entire batch and reopen post-Sailing-launch. Otherwise full bar. |
| **Batch 12** | Long-tail minigames + clue rare-reward sub-tables | Pest Control, Soul Wars, Last Man Standing, Castle Wars, Tithe Farm, Trouble Brewing, Barbarian Assault, Fishing Trawler, Mage Training Arena, Brimhaven Agility Arena, Rogues' Den, Temple Trekking, Gnome Restaurant (Scarfs + Seed Pods), Barracuda Trials, Vale Totems, Hard / Elite / Master Treasure Trail Rewards (Rare), Scroll Cases, Shared Treasure Trail Rewards, The Mimic, Prifddinas Elf, Fountain of Rune, Random Events (~24) | 2-3 | Mixed -- full for minigames; reduced for clue rare-reward sub-tables | Largest batch; split likely into 12a (10 long-tail minigames), 12b (6 clue rare-reward sub-tables on reduced bar), 12c (8 misc OTHER residue including Random Events). |

**Total:** 144 sources across 12 batches, an estimated **15-18 PRs** when accounting for split-batch overflow. Estimated authoring time: 1-3 days per batch; full milestone in ~6-10 calendar weeks if batches run in parallel via worktrees.

**Ordering rationale:** Batches 1-3 deliver "high per-source value" first (boss + raid variants + popular skilling). Batches 4-7 are the slayer-monster bulk pass where the reduced bar (Q1) has the biggest leverage. Batches 8-11 sweep the OTHER category by sub-cluster. Batch 12 is last because clue rare-reward sub-tables are most novel (D3 deferred them explicitly) and benefits from D3 batch 6 clue-tier authoring lessons. No batch has cross-batch dependencies except Batch 11 (Sailing release) and Batch 2 (depends on D2 raid scaffolds).

**Depth-bar exemption summary:** Batches 5, 6, 8, parts of 9 + 10, and the clue rare-reward sub-tables in 12 are candidates for the relaxed long-tail bar. That covers roughly **80 of the 144 D4 sources (~55 %)**. The remaining ~64 sources stay at the full deep-guidance bar.

---

## 5. Per-source gap audit method

Same script-driven method as [D3 §4](d3-mid-tier-scoping.md#4-per-source-deep-guidance-gap-audit-lean----sample--script). No per-source score table is maintained inline; contributors regenerate the gap matrix on demand for the sources their batch touches.

### 5.1. Script -- regenerate the D4 candidate list

Run from repo root (requires `jq` 1.6+). Edit the `D2|D3` regex if either upstream list expands:

```bash
D2D3='^(Chambers of Xeric|Theatre of Blood|Theatre of Blood \(Hard Mode\)|Tombs of Amascut|General Graardor|Commander Zilyana|K.?ril Tsutsaroth|Kree.?arra|Vardorvis|Duke Sucellus|The Leviathan|The Whisperer|Vorkath|Zulrah|Phantom Muspah|The Nightmare|Phosani.?s Nightmare|Corporeal Beast|The Gauntlet|Corrupted Gauntlet|Alchemical Hydra|Cerberus|Kraken|Abyssal Sire|Grotesque Guardians|Vet.?ion|Callisto|Venenatis|King Black Dragon|Dagannoth Rex|Dagannoth Prime|Dagannoth Supreme|Kalphite Queen|Barrows|Giant Mole|Wintertodt|Tempoross|Hard Treasure Trails|Elite Treasure Trails|Master Treasure Trails|Araxxor|Tormented Demons|Nex|The Hueycoatl|Yama|Doom of Mokhaiotl|Royal Titans|Sol Heredit|The Inferno|The Fight Caves|Vardorvis \(Awakened\)|Duke Sucellus \(Awakened\)|The Leviathan \(Awakened\)|The Whisperer \(Awakened\)|Demonic gorillas|Hydra|Skotizo|Sarachnis|Scurrius|Thermonuclear smoke devil|Perilous Moons|Amoxliatl|Hespori|Zalcano|Wyrm|Drake|Lizardman shaman|Skeletal Wyvern|Vyrewatch Sentinel|Gargoyle|Hallowed Sepulchre|Guardians of the Rift|Mahogany Homes|Giants. Foundry|Shades of Mort.ton|Volcanic Mine|Mastering Mixology|Beginner Treasure Trails|Easy Treasure Trails|Medium Treasure Trails)$'

jq --arg pat "$D2D3" '
  map(select(.name | test($pat) | not))
  | group_by(.category)
  | map({category: .[0].category, count: length, names: [.[].name]})
' src/main/resources/com/collectionloghelper/drop_rates.json
```

Output is a JSON array grouped by category with names. Use it to regenerate [§3](#3-category-breakdown) when D2/D3 boundaries shift or when new sources land in `drop_rates.json`.

### 5.2. Script -- per-source gap matrix

Identical to [D3 §4.1](d3-mid-tier-scoping.md#41-script----derive-the-gap-matrix-for-any-source-list). Re-target the `SOURCES` regex to the batch you are authoring, run the script, paste the before/after rows into the batch PR description.

---

## 6. Open questions / follow-ups

### Q1 -- Relaxed "long-tail bar" for low-engagement sources

The 10-element [deep-guidance-bar](deep-guidance-bar.md) is calibrated for content the community actively grinds. For sources where realistic per-account farming time is sub-hour-per-year (random-event drops, salvage-table chances, sub-page clue rare-reward tables, low-level slayer monsters), several elements are inappropriate or wasteful:

- **E4 (loop detection)** -- N/A for one-shot interactions (open chest, hand in salvage).
- **E5 (strategy note)** -- N/A for sources with no combat mechanic.
- **E7 (inventory loadout)** -- often empty for sources reachable in starter gear.
- **E10 (PR citations)** -- still mandatory, but a single citation covering an entire batch of shop sources is acceptable.

**Proposal:** define a **"long-tail bar"** of 5 elements (E1 travel tip, E2 auto-arrival, E6 bank-and-return when applicable, E8 prerequisites, E9 drop-rate confidence) as an explicit alternative bar. Document it as a section in `deep-guidance-bar.md` before opening Batch 5. Batches 5, 6, 8, parts of 9 + 10, and the clue rare-reward sub-tables in 12 would author against the long-tail bar; everything else stays at full bar.

**Recommendation:** **adopt the long-tail bar before Batch 5 opens.** Without it, ~80 sources eat full-bar authoring effort for sub-hour-per-account engagement, which is not a good trade.

### Q2 -- Triage of placeholder / unobtainable sources

A handful of D4 candidates may be authored as placeholder rows with no real `items[]` content, or may represent content that is no longer obtainable in-game. Specific candidates flagged by name shape:

- **"Miscellaneous"** -- catch-all bucket; unclear whether it represents real content or placeholder rows.
- **"My Notes"** -- likely a UI / notes-feature artefact, not a real collection-log source.
- **"Port Tasks"** -- pre-Player-Owned-Port retired content?
- **"Sailing Misc" / "Boat Paints" / "Sea Treasures"** -- Sailing pre-release placeholders.
- **"Stronghold of Security"** -- one-time clear; arguably belongs in a "one-shot achievements" category.

**Recommendation:** before opening Batches 10-11, run a triage pass that for each named source either (a) confirms it represents real, currently-obtainable content (proceed with authoring), (b) marks it as placeholder for future content (skip authoring, leave a TODO marker), or (c) removes it from `drop_rates.json` (separate cleanup PR -- not part of D4 authoring batches).

### Q3 -- Sailing-locked content (Boat Combat 5 variants + Ocean Encounters + Sailing Misc + Sea Treasures + Boat Paints)

Same watch-item as [D3 §5 Q1](d3-mid-tier-scoping.md#5-open-questions--follow-ups), now in scope for D4 Batch 11. If Sailing has not released when Batch 11 opens, defer the batch and reopen post-release. Do not author guidance against pre-release Wiki speculation.

### Q4 -- Superior Slayer Monster cross-task table

Superior Slayer Monster is a single source whose rare table applies across many slayer tasks (each superior variant is a "spawn chance" rather than a kill). Authoring its guidance as a per-source travel + kill flow is awkward -- the source is task-agnostic. Two options:

- **(a)** Author a single "this rare table applies on any slayer task above level X" note with E1 = no travel tip, E5 = "spawn chance per task," E8 = Slayer level 5 (to unlock superiors).
- **(b)** Treat it as a synthetic source and exempt it from the bar entirely; cross-reference it from every slayer-task-source's guidance.

**Recommendation:** option (a) for Batch 7; revisit if D5 Tier B branch retrofit changes how cross-cutting tables are modelled.

### Q5 -- Random Events source authoring

Random Events is one source covering all random-event drops (gnome scarfs, evil bob trout, drill demon outfit, etc.). The drops are *passive* -- they happen during normal play, with no travel / kill flow. Authoring full E1-E8 guidance for it is contradictory.

**Recommendation:** author with `travelTip = ""`, a single MANUAL step "Random events trigger passively during play," E8 prereq = none, E9 drop-rate confidence pass on the table, and use the long-tail bar from Q1.

### Q6 -- Splitting OTHER into sub-categories

OTHER at 58 sources is the largest single category in D4 and the most heterogeneous. A schema-level split (introducing CHEST, SALVAGE, BOAT_COMBAT, EVENT sub-categories) would improve `category`-based panel filtering for end users and make D5 retrofit cleaner.

**Recommendation:** **do not block D4 on this.** File as a separate Tier B schema PR after D4 completes. D4 authors against current categories as-is.

### Q7 -- D5 handoff

D5 (Tier B/C branch model retrofit) operates on the same sources D2-D4 authored. Once D4 closes, every source in `drop_rates.json` should be at either the full deep-guidance bar or the long-tail bar. D5's job is to layer composable conditions / conditional alternatives on top, not to re-author guidance. Confirm this division at D4 close so D5 scoping does not re-litigate per-source authoring depth.

---

## 7. References

- [Tier D2 top-40 scoping](d2-top-40-scoping.md) -- structural template; established the per-batch fix template inherited by D3 and D4.
- [Tier D3 mid-tier scoping](d3-mid-tier-scoping.md) -- direct predecessor; introduced the LEAN script-driven gap-audit method D4 reuses.
- [Deep guidance bar -- the 10 elements](deep-guidance-bar.md) -- D1 deliverable; the bar D4 authors against (potentially relaxed per [§6 Q1](#6-open-questions--follow-ups)).
- [Tier C6 top-20 player-state wiring scoping](c6-top-20-player-state-wiring-scoping.md) -- batching-format precedent for domain-grouped scoping docs.
- [Decision D-03 -- data-sourcing precedence](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs)
- [Tier D status log](../ROADMAP.md#tier-d--coverage-breadth) -- D4 row, currently `status: planned`.
- [JSON Schema Reference](schema-reference.md)
