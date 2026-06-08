# Data-verification findings — OTHER category

Scope: all 58 `OTHER` sources in
`src/main/resources/com/collectionloghelper/drop_rates.json`.

Method:
- **IDs** — `validate_drop_rates --check cache_ids` (whole-file run, OTHER sources
  filtered out). The abextm game cache is the sole id authority; ids were not adjudicated
  against Temple/wiki. Guidance-step `requiredItemIds` (which `cache_ids` does not cover)
  were spot-checked with `cross_check_ids` where a discrepancy was suspected.
- **Judgment** (requirements/quest+skill+diary gates, fairy-ring codes, arrival coords,
  travel direction) — only logged with a cited raw receipt, then passed through the
  `domain-skeptic` agent; only verdicts that **STAND** are recorded below.
- `resource_health` confirmed green (osrs-wiki, prices, WOM, temple-osrs, runelite-ids,
  mejrs all 200) before the run.
- `drop_rates.json` was **not** edited. Findings only.

Sources reviewed (58): Revenants, Brimstone Chest, Larran's Big Chest, Zombie Pirate
Locker, Boat Paints, Champion's Challenge, Chompy Bird Hunting, Forestry, Fossil Island
Notes, Glough's Experiments, Hunter Guild, Lost Schematics, Monkey Backpacks, My Notes,
Ocean Encounters, Sailing Misc, Sea Treasures, Pyramid Plunder, Stronghold of Security,
Catacombs of Kourend, Wilderness God Wars Dungeon, Elven Crystal Chest, Miscellaneous,
Random Events, Camdozaal, TzHaar, Cyclopes, Elder Chaos Druids, Shayzien Armour,
Creature Creation (Newtroost, Unicow, Spidine, Swordchick, Jubster, Frogeel),
Stingray / Albatross / Narwhal / Orcas / Great White Shark / Vampyre Kraken (Boat Combat),
Small / Fishy / Barracuda / Large / Plundered / Martial / Fremennik / Opulent Salvage,
Mithril Dragon, Adamant Dragon, Rune Dragon, Ogress Shaman, Armoured Zombie,
Prifddinas Elf, Fountain of Rune, Waterfiend, Port Tasks.

---

## ID verification summary

`validate_drop_rates --check cache_ids` returns **no unresolved / orphaned item, NPC, or
object id** for any OTHER source. Every id resolves in the abextm cache. The cache_ids
"name mismatch" rows that touch OTHER sources (Revenants `Bracelet of ethereum`,
Champion's Challenge scroll naming, Chompy Bird Hunting hats → cache "Chompy bird hat",
My Notes / Mithril Dragon `Ancient page 1–26`, Sea Treasures `Medallion fragment 1–8`,
Pyramid Plunder `Pharaoh's sceptre`, Miscellaneous `Xeric's talisman`, Armoured Zombie
zombie scroll) are **cosmetic display-name choices** — the id is correct and resolves;
the cache simply uses a generic/base or `(uncharged)` name. Per the rule that the cache is
the sole id authority and these are not wrong ids, none are logged as ID findings.
`Unsired` (25624) is cache-confirmed correct and was not flagged.

The id defect below was found in a guidance-step `requiredItemIds` (outside cache_ids'
coverage) and is reported under the Newtroost finding.

---

## Findings (STANDS)

### 1. Creature Creation (Newtroost) — wrong creation ingredient + wrong Eye-of-newt id  — severity: high

- **Source:** `Creature Creation (Newtroost)` (OTHER), source block at line 22806.
- **Data:**
  - `guidanceSteps[0].description`: "Bring **raw chicken** and eye of newt as ingredients."
  - `guidanceSteps[0].requiredItemIds`: `[2138, 219]`.
  - `guidanceSteps[1].description`: "Place **raw chicken** and eye of newt on the Altar of Life to create a Newtroost…"
  - The source's own `locationDescription`: "Tower of Life, south of Ardougne (**Eye of newt + Feather**)".
- **Receipt** (OSRS Wiki, https://oldschool.runescape.wiki/w/Creature_Creation recipe
  table, fetched 2026-06-07): **Newtroost = 1 Eye of newt + 1 Feather.** Raw chicken is a
  *Swordchick* ingredient, not a Newtroost one.
- **ID receipt** (`cross_check_ids`, abextm cache): `219 → "Grimy torstol"` (name-mismatch),
  `221 → "Eye of newt"` (match), `314 → "Feather"` (match), `2138 → "Raw chicken"` (match).
  So `requiredItemIds` is wrong twice: 2138 (Raw chicken) should be **314 (Feather)**, and
  219 (Grimy torstol) should be **221 (Eye of newt)**.
- **Why it stands:** Recipe is fixed game content (Tower of Life, 2007), identical across
  account types; diary tiers only affect whether *drops* are noted, not inputs. The step
  contradicts both the wiki and the source's own `locationDescription`.
- **domain-skeptic:** STANDS (high) — survived every refutation vector; also independently
  surfaced the 219→Grimy torstol id error.
- **Suggested fix (not applied):** `requiredItemIds` `[2138, 219]` → `[221, 314]`; both
  step descriptions "raw chicken and eye of newt" → "eye of newt and feather".

### 2. Creature Creation (Frogeel) — wrong creation ingredients (cross-creature copy/paste)  — severity: high

- **Source:** `Creature Creation (Frogeel)` (OTHER), source block at line 23156.
- **Data:**
  - `guidanceSteps[0].description`: "Bring **raw lobster and red spiders' eggs** as ingredients."
  - `guidanceSteps[0].requiredItemIds`: `[377, 223]`.
  - `guidanceSteps[1].description`: "Place **raw lobster and red spiders' eggs** on the Altar of Life to create a Frogeel…"
  - The source's own `locationDescription`: "Tower of Life, south of Ardougne (**Raw cave eel + Giant frog legs**)".
- **Receipt** (OSRS Wiki, Creature Creation recipe table, fetched 2026-06-07):
  **Frogeel = 1 Raw cave eel + 1 Giant frog legs.**
- **ID receipt** (`cross_check_ids`): `377 → "Raw lobster"` (a *Jubster* ingredient),
  `223 → "Red spiders' eggs"` (a *Spidine* ingredient), `5001 → "Raw cave eel"` (match),
  `4517 → "Giant frog legs"` (match). The listed ingredients are a cross-creature
  copy/paste, not the Frogeel recipe.
- **Why it stands:** Fixed, ancient game content; no mechanic produces a Frogeel from
  lobster + spider eggs. Contradicts both the wiki and the source's own `locationDescription`.
- **domain-skeptic:** STANDS (high) — survived every refutation vector.
- **Suggested fix (not applied):** `requiredItemIds` `[377, 223]` → `[5001, 4517]`; both
  step descriptions → "raw cave eel and giant frog legs".

### 3. Creature Creation (Unicow) — wrong creation ingredient  — severity: low

- **Source:** `Creature Creation (Unicow)` (OTHER), source block at line 22876.
- **Data:**
  - `guidanceSteps[0].description`: "Bring **raw beef** and unicorn horn as ingredients."
  - `guidanceSteps[0].requiredItemIds`: `[2132, 237]`.
  - `guidanceSteps[1].description`: "Place **raw beef** and unicorn horn on the Altar of Life…"
  - The source's own `locationDescription`: "Tower of Life, south of Ardougne (**Cowhide + Unicorn horn**)".
- **Receipt** (OSRS Wiki, Creature Creation recipe table, fetched 2026-06-07):
  **Unicow = 1 Cowhide + 1 Unicorn horn.**
- **ID receipt** (`cross_check_ids`): `2132 → "Raw beef"` (match), `1739 → "Cowhide"` (match),
  `237 → "Unicorn horn"` (match). The intended ingredient (Cowhide) carries id 1739, not 2132.
- **Why it stands:** Recipes are fixed per altar and noted items aren't accepted; raw beef
  is never a Unicow input. Contradicts both the wiki and the source's own `locationDescription`.
  Low severity: player-facing inventory hint, does not affect the clog item id or drop rate.
- **domain-skeptic:** STANDS (low) — survived every applicable refutation vector.
- **Suggested fix (not applied):** `requiredItemIds` `[2132, 237]` → `[1739, 237]`; both
  step descriptions "raw beef and unicorn horn" → "cowhide and unicorn horn".

### 4. Pyramid Plunder — Pharaoh's sceptre source/level guidance is wrong and self-contradictory  — severity: low

- **Source:** `Pyramid Plunder` (OTHER), source block at line 20658, item
  `Pharaoh's sceptre` (itemId 26945 — cache-confirmed, not in dispute).
- **Data:**
  - `guidanceSteps[0].description`: "Need **71 Thieving minimum** to access the grand gold
    chest (**the only Pharaoh's sceptre source**)."
  - `guidanceSteps[2].description`: "At room 8 (requires **91 Thieving**), loot the grand
    gold chest which **exclusively** yields the Pharaoh's sceptre."
- **Receipts:**
  - `wiki_lookup` "Pharaoh's sceptre": "…found during the Pyramid Plunder minigame in
    Sophanem, **from the golden chests and sarcophagi**." / "The chance of receiving a
    pharaoh's sceptre **scales depending on the room**."
  - `wiki_lookup` / WebFetch "Pyramid Plunder": "Every ten levels thereafter will grant
    entrance to the more valuable rooms, **up to level 91**." Room table: rooms 6/7/8 =
    71/81/91; every room contains a golden chest **and** a sarcophagus.
- **Why it stands:** The sceptre drops from golden chests **and sarcophagi** in **every
  room** (room-scaled odds), so both the "only … source" (step0) and "grand gold chest …
  exclusively" (step8/2) framings are false, and there is no distinct "grand gold chest"
  entity. The 71-vs-91 wording is also inconsistent — but note the correct repair is **not**
  simply "71 → 91": a player can already obtain the sceptre at 71 (room 6) at worse odds;
  91 (room 8) is the best-odds recommendation, not the access gate. Low severity: advisory
  prose only; item id and drop rate unaffected.
- **domain-skeptic:** STANDS (low) — confirmed the prose error and corrected the finding's
  own proposed fix.
- **Suggested fix (not applied):** Rewrite both steps to state the sceptre drops from golden
  chests and sarcophagi in every room with room-scaled odds, recommend room 8 (91 Thieving)
  for best odds, and drop the "grand gold chest" / "exclusive" / "only source" framing.

---

## Checked and cleared (no finding)

- **ID integrity** — every OTHER-source item/NPC/object id resolves in the abextm cache
  (cache_ids name-mismatches are cosmetic; see ID summary above). `Unsired` (25624) correct.
- **Fairy-ring codes** (all consistent with destination): Brimstone Chest **CIR** (Mount
  Karuulm), Chompy Bird Hunting **AKS** (Feldip Hills), Monkey Backpacks **CLR** (Ape
  Atoll), TzHaar **BLP** (TzHaar City), Catacombs of Kourend **CIS** (Kourend Castle),
  Creature Creation ×6 **DJP** (Tower of Life).
- **Hunter Guild** — "Requires 46 Hunter and completion of Children of the Sun" matches the
  wiki (46 Hunter, not boostable; Children of the Sun gates Varlamore travel). Correct.
- **Monkey Backpacks** — "requires Monkey Madness II": the Ape Atoll Agility *course* needs
  only MM1, but the monkey-backpack transformations (the clog items) genuinely require MM2,
  so the requirement is legitimate for this source. domain-skeptic refutation expected → not
  logged.
- **Fountain of Rune** — "Charging glory requires completion of Heroes' Quest" matches the
  wiki ("Charging amulets of glory requires completion of Heroes' Quest"). Correct.
- **Champion's Challenge** — "guild requires 32 Quest Points to enter" matches the wiki.
- **Miscellaneous** — `travel_path_verify` flags `Miscellaneous#1` tile `(0,0,0)` as
  out-of-bounds, but this is a deliberate "no single location" sentinel for an aggregate tab
  ("items with no dedicated source"); the source-level coords are valid (3222,3218).
  Intentional → not logged as a correctness finding.
- **Sailing-skill sources** (Boat Paints, Lost Schematics, Ocean Encounters, Sailing Misc,
  Sea Treasures, Port Tasks, the 6 Boat Combat NPCs, the 8 Salvage sources) describe
  unreleased Sailing content with no OSRS Wiki / Temple authority to cite. No raw receipt
  can be produced for or against their judgment fields, so they are out of verifiable scope
  and intentionally not logged either way.
