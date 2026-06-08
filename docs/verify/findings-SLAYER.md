# SLAYER source verification

Verification of every `SLAYER` source in
`src/main/resources/com/collectionloghelper/drop_rates.json`.

- **Date:** 2026-06-07
- **Sources checked:** 45
- **Tooling:** `runelite-dev` MCP (`resource_health`, `validate_drop_rates --check cache_ids`,
  `travel_path_verify`, `wiki_lookup`), OSRS Wiki receipts, `domain-skeptic` agent gating.
- **MCP health at start:** all sources `ok` (osrs-wiki, temple-osrs, runelite-ids, etc.).
- **Scope rule:** IDs adjudicated against the abextm cache only; judgment findings logged
  only with a cited raw receipt and kept only if they survive the `domain-skeptic` pass.
- **Not edited:** `drop_rates.json` was not modified (verification only).

## Summary

| Check | Result |
|-------|--------|
| Item / NPC IDs (`cache_ids`) | ✅ All 45 sources clean — zero SLAYER entries in the 162 cache-id findings (all 162 are non-SLAYER name-variant cosmetics). |
| Coordinate bounds (`travel_path_verify`) | ✅ Clean for SLAYER. (The one out-of-bounds slayer-like hit, *Shellbane Gryphon* `13993,9901`, is category **BOSSES** — out of scope here.) |
| Requirements (skill / quest / Sailing gates) | ✅ All verified or defensibly modeled (see notes). |
| Fairy-ring codes & travel direction | ⚠️ 2 findings (Terror Dog, Spiritual Mage) — both **STAND**. |

**Net: 2 findings, both low severity, both in guidance travel-route text (not IDs, rates, or coords).**

---

## Findings (STANDS)

### 1. Terror Dog — wrong fairy-ring code + bearing in Travel step

- **Source:** `Terror Dog` (npc 6474), guidance Travel step, arrival tile `(3414,3232,0)`.
- **Text:** *"…Alternatively, use fairy ring AJR to Fremennik Province and run east to the
  Abandoned Mine entrance."* (source-level `travelTip` repeats *"fairy ring AJR -> run east to Abandoned Mine"*).
- **Receipt:** OSRS Wiki *Fairy ring* — `AJR` = **"Slayer cave south-east of Rellekka"**
  (Fremennik Province, ~`2700,3700`). The Abandoned Mine entrance is `~3416,3232` (Mort'ton),
  ~700 tiles south-east across water. You cannot "run east" from AJR to reach it.
- **Correct route (per Wiki *Abandoned Mine* / *Tarn's Lair*):** fairy ring **BIQ** → cross the
  River Salve shortcut (50 Agility) → run **south**.
- **domain-skeptic verdict:** **STANDS — low.** A fairy-ring route *does* exist (BIQ + Salve +
  south), so the defect is a **wrong code and wrong bearing**, not a nonexistent route. The
  primary route in the same step (Slayer ring → Tarn's Lair) is correct and independently
  confirmed, so the recommendation still functions.
- **Suggested fix (data owner):** replace the AJR clause with "fairy ring **BIQ**, cross the Salve
  shortcut (50 Agility) and run **south**," or drop the fairy-ring alternative entirely.
- **status:** resolved 2026-06-07 (swapped AJR -> BIQ + Salve shortcut (50 Agility) + run south in all 3 Terror Dog strings; the Slayer-ring route is unchanged; `lintDropRates build` green).

### 2. Spiritual Mage — invalid fairy-ring route to the God Wars Dungeon

- **Source:** `Spiritual Mage` (npc 2212), guidance Travel step `description`, arrival tile
  `(2844,5280,plane 2)` (GWD main chamber).
- **Text:** *"Travel to the God Wars Dungeon (fairy ring AJQ then south, or Trollheim teleport)".*
- **Receipt:** OSRS Wiki *Fairy ring* — `AJQ` = **"Cave south of Dorgesh-Kaan"** (central Misthalin
  underground). OSRS Wiki *God Wars Dungeon* access lists only "north of Trollheim … Trollheim
  Teleport … or teleport to Burthorpe"; there is **no fairy ring** serving the GWD. AJQ lands
  ~1,800 tiles south; running south moves further from the far-north GWD.
- **domain-skeptic verdict:** **STANDS — low.** No OSRS mechanic links the Dorgesh-Kaan cave to the
  GWD. The arrival tile and the source `travelTip` ("Trollheim teleport -> GWD") are correct; only
  the parenthetical fairy-ring clause is bad.
- **Suggested fix (data owner):** drop the "fairy ring AJQ then south, or " clause, leaving
  *"Travel to the God Wars Dungeon (Trollheim teleport)"*, matching the already-correct `travelTip`.

---

## Verified correct (no finding)

### IDs
All 45 SLAYER sources pass `validate_drop_rates --check cache_ids`. The full-file run surfaced 162
cache-id findings; **none** are SLAYER (every one is a non-SLAYER charged/uncharged or coloured
name variant). New high-id NPCs are all cache-confirmed: Gryphon 14857, Custodian Stalker 14704,
Lava Strykewyrm 15500, Aquanite 15497, Earthen Nagua 14420, Frost Nagua 13788, Araxyte 13667.
Per project rule, Unsired (25624) is correct and was not flagged (not in SLAYER anyway).

### Requirements / gates (receipted)
- **Naguas** — Sulphur / Frost / Earthen Nagua all **Slayer 48**, "Lesser Nagua" category (Wiki
  infoboxes). Matches data. Frost Nagua drop rates also confirmed: Glacial temotli 1/500, Frozen
  tear 1/10, Pendant of ates (inert) 1/100.
- **Gryphon** — **Slayer 51** (Wiki), npc 14857; Horn of plenty (empty) 1/2500 confirmed. Data adds
  Troubled Tortugans quest (Sailing access to Great Conch) — plausible, no contradicting receipt.
- **Lava Strykewyrm** — **Slayer 62 + Sailing 60** confirmed verbatim (Wiki: "Sailing Level: 60
  (required to reach the location)" — Charred Island/Charred Dungeon). Matches data exactly.
- **Aquanite** — **Slayer 78 + Sailing 73** confirmed verbatim (Wiki: "Sailing Level: 73 (to access
  Ynysdail)"). Matches data exactly.
- **Custodian Stalker** — Wiki: "fought after completion of the Shadows of Custodia quest." Matches
  data (Slayer 76 + SHADOWS_OF_CUSTODIA).
- **Araxyte** — **Slayer 92** (Wiki). Matches data.
- **Spiritual Mage / Spiritual Mage (Zarosian)** — **Slayer 83** (Wiki). Matches data; Zarosian
  variant correctly notes frozen key for the Ancient Prison.
- **Mogre** — **Slayer 32** (Wiki), npc 2592; Mudskipper hat 5/128, Flippers 2/128 confirmed.
- **Vyrewatch Sentinel** — data lists only SINS_OF_THE_FATHER (no Slayer level). Wiki confirms the
  Slayer 38 applies **only to task assignment**, not to attacking ("can be fought after completing
  Sins of the Father"). Data's quest-only gate is correct — **not a finding**.
- Classic gates spot-consistent with known values: Crawling Hand 5 … Hydra 95; quest gates
  (Monkey Madness II, While Guthix Sleeps, Haunted Mine, Olaf's Quest, Cabin Fever, Bone Voyage,
  The Fremennik Exiles, The Path of Glouphrie, Song of the Elves) all match.

### Fairy-ring codes verified against the canonical Wiki map (correct)
| Code | Wiki destination | Used by | Verdict |
|------|------------------|---------|---------|
| DJR | Chasm of Fire (Kebos) | Lizardman shaman → Canyon | ✅ |
| AJR | Slayer cave SE of Rellekka | Cave Crawler, Rockslug, Cockatrice, Pyrefiend, Basilisk, Jelly, Turoth, Kurask (Fremennik dungeon) | ✅ |
| CKS | Canifis | Infernal Mage, Bloodveld, Aberrant Spectre, Gargoyle, Nechryael (Slayer Tower) | ✅ |
| DKS | Polar/Snowy Hunter area | Brine Rat (cavern NE of Rellekka) | ✅ |
| CJQ | The Great Conch | Gryphon | ✅ |
| CIR | South of Mount Karuulm | Wyrm, Drake, Hydra (Karuulm dungeon) | ✅ |
| AIQ | Mudskipper Point | Skeletal Wyvern (Asgarnian Ice Dungeon) | ✅ |
| AIS | Auburn Valley | Custodian Stalker (Stalker Den) | ✅ |
| AKQ | Piscatoris Hunter area | Cave Kraken (Kraken Cove) | ✅ |
| BKP | Chompy Marsh S of Castle Wars | Smoke Devil (Smoke Devil Dungeon) | ✅ |
| AJQ | Cave south of Dorgesh-Kaan | Spiritual Mage | ❌ Finding #2 |

(Vyrewatch Sentinel uses Drakan's medallion → Darkmeyer, not a fairy ring — "BIS" in its text is
"best-in-slot", a false positive.)
