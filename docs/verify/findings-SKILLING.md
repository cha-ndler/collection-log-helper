# Data-verification findings — SKILLING category

Scope: all 17 `SKILLING` sources in
`src/main/resources/com/collectionloghelper/drop_rates.json`.

Method:
- **IDs** — `validate_drop_rates --check cache_ids --source_name "<source>"` per source.
  The abextm game cache is the sole id authority; ids were not adjudicated against
  Temple/wiki.
- **Judgment** (requirements/quest+skill+diary gates, fairy-ring codes, arrival coords,
  travel direction) — only logged with a cited raw receipt, then passed through the
  `domain-skeptic` agent; only verdicts that **STAND** are recorded below.
- `resource_health` confirmed green (osrs-wiki, temple-osrs, prices, WOM, runelite-ids,
  mejrs all 200) before the run.
- `drop_rates.json` was **not** edited. Findings only.

Sources reviewed: Aerial Fishing, Deep Sea Fishing, Colossal Wyrm Agility, Rooftop Agility,
Motherlode Mine, Shooting Stars, Thieving (Seed Stalls), Black Chinchompas,
Woodcutting (Teak Trees), Mining (Gemstone Rocks), Fishing (Swordfish),
Runecrafting (Fire Runes), Farming (Fruit Trees), Underwater Crabs, Cutting Squid,
Prifddinas Rabbit, Pickpocketing Darkmeyer Vyre.

---

## Findings (STANDS)

### 1. Cutting Squid — requirements gate under-specified  — severity: high

- **Source:** `Cutting Squid` (SKILLING), item `Squid beak` (itemId 31572).
- **Data:** `requirements` block at lines 32072–32077 —
  `{ "skills": [ { "skill": "SAILING", "level": 45 } ] }`. No Fishing requirement.
  Guidance prose at line 32030 also states "Sailing level 45 required."
- **Receipt** (OSRS Wiki, https://oldschool.runescape.wiki/w/Raw_swordtip_squid):
  > "Raw swordtip squid is a fish caught by lantern harpooning, requiring level 52 Fishing
  > alongside a lantern and harpoon. … the minimum Sailing level to catch them is 47, the
  > level required to dock at the Onyx Crest."

  Corroborating (https://oldschool.runescape.wiki/w/Lantern_harpooning):
  > "Lantern harpooning is a Fishing activity released with Sailing, requiring at least
  > 47 Sailing and 52 Fishing."
- **Why it stands:** Squid beak is obtained only by cutting squid, and squid are caught
  only via lantern harpooning. The universal activity floor is **Sailing 47 + Fishing 52**.
  The data's Sailing 45 is below the true gate, and the Fishing 52 requirement is absent —
  so the source is reachable in-data at Sailing 45 / 0 Fishing, which no real account can do.
  Account-type-invariant; wiki page unchanged since 2025-06-01 (not staleness). Item id is
  cache-confirmed correct and not in dispute.
- **domain-skeptic:** STANDS (high). Both prongs survived every refutation vector.
- **Suggested fix (not applied):** Sailing 45 → 47, and add `{ "skill": "FISHING",
  "level": 52 }`. Update the guidance prose at line 32030 accordingly.

### 2. Deep Sea Fishing — Fishing gate off by one  — severity: low

- **Source:** `Deep Sea Fishing` (SKILLING), trophy fish Giant blue krill (31408),
  Golden haddock (31412), Orangefin (31416), Huge halibut (31420), Purplefin (31424),
  Swift marlin (31428).
- **Data:** `requirements` block at lines 20644–20653 —
  `{ "skills": [ { "skill": "FISHING", "level": 68 }, { "skill": "SAILING", "level": 1 } ] }`.
  Prose at line 20604 (travelTip) and line 20607 (guidance) also say "68 Fishing".
- **Receipt** (OSRS Wiki, https://oldschool.runescape.wiki/w/Deep_sea_trawling shoals
  table, and the Giant krill shoal entry): the lowest-tier shoal, the **Giant krill shoal**
  (source of Giant blue krill), requires **Fishing 69**. No shoal is listed at Fishing 68.
  Higher trophies: Haddock 73, Yellowfin/Orangefin 79, Halibut 83, Bluefin/Purplefin 87,
  Marlin 91.
- **Why it stands:** The lowest obtainable trophy needs Fishing 69; the lowest shoal in the
  activity *is* the trophy source, so there is no sub-69 entry point. The project convention
  (e.g. the adjacent Pyramid Plunder source) gates on the lowest level that yields a clog
  item, not an "activity-start" level. The gate of 68 corresponds to no obtainable content.
  Severity low: only affects the narrow Fishing-68 boundary; the six trophy ids and drop
  rates are correct. (Sailing 1 was **not** flagged — no contradicting access-level receipt
  found; wiki documents no Sailing minimum for area access.)
- **domain-skeptic:** STANDS (low).
- **Suggested fix (not applied):** Fishing 68 → 69 in the requirements block; correct the
  prose at lines 20604 and 20607 to "69 Fishing".
- **status:** resolved 2026-06-07 (Fishing 68->69 in requirements + both prose strings; Sailing 1 left as-is; `lintDropRates build` green).

---

## Reviewed — no finding

### ID checks (cache_ids)
All 17 sources resolved every item/NPC/object id against the abextm cache. Two
**name-mismatch** notes were returned by the validator; both are deliberate display labels,
not id errors (ids are cache-confirmed), so neither is logged as a finding:

- **Colossal Wyrm Agility** — ids 30048/30051/30054/30057/30060 carry a "(Varlamore)" suffix
  ("Graceful cape (Varlamore)" etc.) vs cache "Graceful cape". The suffix disambiguates the
  Varlamore graceful recolour from the Rooftop graceful set (which shares the base names at
  ids 11852 etc.). Ids correct.
- **Shooting Stars** — id 25539 stored as "Celestial ring" vs cache "Celestial ring
  (uncharged)". Id correct; the abextm cache is the sole id authority and is not adjudicated
  against the name suffix.

### Judgment checks
- **No SKILLING source has fairy-ring codes or out-of-bounds coordinates.**
  `travel_path_verify` scanned all 808 steps / 225 sources; the only coord-out-of-bounds
  findings are in non-SKILLING sources (Miscellaneous, Shellbane Gryphon, Treasure Trails).
- **Requirement gates verified as correct** (no contradicting receipt):
  - Aerial Fishing — Fishing 43 + Hunter 35 (wiki: "requiring 35 Hunter and 43 Fishing"). ✓
  - Colossal Wyrm Agility — Children of the Sun + Agility 50. ✓
  - Rooftop Agility — Agility 10. ✓
  - Motherlode Mine — Mining 30. ✓
  - Shooting Stars — Mining 10. ✓
  - Thieving (Seed Stalls) — Thieving 27. ✓
  - Black Chinchompas — Hunter 73. ✓
  - Woodcutting (Teak Trees) — Woodcutting 35. ✓
  - Mining (Gemstone Rocks) — Shilo Village + Mining 40. ✓
  - Fishing (Swordfish) — Fishing 50. ✓
  - Runecrafting (Fire Runes) — Runecraft 14. ✓
  - Farming (Fruit Trees) — Farming 27. ✓
  - Underwater Crabs — Recipe for Disaster (Pirate Pete subquest). ✓
  - Prifddinas Rabbit — Song of the Elves. ✓
  - Pickpocketing Darkmeyer Vyre — Sins of the Father + Thieving 82. ✓
