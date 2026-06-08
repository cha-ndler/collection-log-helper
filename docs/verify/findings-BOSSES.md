# Data-verification findings — BOSSES

- **Category:** `BOSSES` (61 sources in `src/main/resources/com/collectionloghelper/drop_rates.json`)
- **Branch:** `verify/BOSSES`
- **Date:** 2026-06-07
- **MCP pre-flight:** `resource_health` → all sources `ok` (osrs-wiki, temple-osrs, runelite-ids, mejrs-data, prices, wise-old-man).
- **Scope:** This file is intentionally separate from the shared `data-verification-log.md` so parallel
  per-category verification sessions do not conflict. It records findings for the BOSSES category only.
- **Rule:** verification only — **no edits were made to `drop_rates.json`**. Fixes are a separate reviewed PR.

## Method

Two checks per source, exactly as scoped:

1. **IDs** — `validate_drop_rates --check cache_ids`. The **abextm game cache is the sole id authority**;
   ids were *not* adjudicated against Temple/wiki. Run globally across all 225 sources, then filtered to BOSSES.
2. **Judgment** — requirements (quest/skill/diary gates), fairy-ring codes, arrival coordinates
   (`travel_path_verify`), and travel direction. A candidate was logged **only with a cited raw receipt**,
   then passed through the `domain-skeptic` agent; **only `STANDS` verdicts are recorded as findings.**

Multi-source clog items are correct by design (never flagged as "duplicate"). Unsired `25624` is
cache-confirmed and was not flagged.

## Result summary

- **IDs (cache_ids):** all 61 BOSSES sources **clean** — zero unresolved ids and zero name-mismatches.
  (The global run surfaced 162 name-mismatch warnings, but none belong to a BOSSES source.)
- **Coordinates (`travel_path_verify`):** one BOSSES hit (Shellbane Gryphon) — **refuted** as a tool
  false-positive on a deliberately-instanced coordinate (see *Considered & refuted* below).
- **Fairy-ring codes:** every cited code verified against the wiki; **one wrong** (Araxxor `CLS`).
- **Requirements gates:** GWD ×4 + Nex, the 8 Desert Treasure II entries, Amoxliatl, and Royal Titans
  were externally verified. The **8 DT2 entries carry fabricated `skills` requirements** (below); the rest
  are correct.

**Open findings: 2 issues spanning 9 source-entries (all severity `high`).**

| # | Source | Data verified (V1) | Open findings |
|---|--------|--------------------|---------------|
| 1 | General Graardor | 2026-06-07 | - |
| 2 | Commander Zilyana | 2026-06-07 | - |
| 3 | K'ril Tsutsaroth | 2026-06-07 | - |
| 4 | Kree'arra | 2026-06-07 | - |
| 5 | Zulrah | 2026-06-07 | - |
| 6 | Vorkath | 2026-06-07 | - |
| 7 | Cerberus | 2026-06-07 | - |
| 8 | Alchemical Hydra | 2026-06-07 | - |
| 9 | Corporeal Beast | 2026-06-07 | - |
| 10 | Kalphite Queen | 2026-06-07 | - |
| 11 | Phantom Muspah | 2026-06-07 | - |
| 12 | Duke Sucellus | - | 1 (high: fabricated skills req MINING 65) |
| 13 | The Leviathan | - | 1 (high: fabricated skills req RANGED 70) |
| 14 | The Whisperer | - | 1 (high: fabricated skills req RANGED 75) |
| 15 | Vardorvis | - | 1 (high: fabricated skills req ATTACK 70) |
| 16 | Duke Sucellus (Awakened) | - | 1 (high: fabricated skills req MINING 65) |
| 17 | The Leviathan (Awakened) | - | 1 (high: fabricated skills req RANGED 75) |
| 18 | The Whisperer (Awakened) | - | 1 (high: fabricated skills req RANGED 80) |
| 19 | Vardorvis (Awakened) | - | 1 (high: fabricated skills req ATTACK 75) |
| 20 | Grotesque Guardians | 2026-06-07 | - |
| 21 | The Nightmare | 2026-06-07 | - |
| 22 | Phosani's Nightmare | 2026-06-07 | - |
| 23 | Nex | 2026-06-07 | - |
| 24 | Sarachnis | 2026-06-07 | - |
| 25 | Giant Mole | 2026-06-07 | - |
| 26 | Bryophyta | 2026-06-07 | - |
| 27 | Obor | 2026-06-07 | - |
| 28 | Dagannoth Rex | 2026-06-07 | - |
| 29 | Dagannoth Prime | 2026-06-07 | - |
| 30 | Dagannoth Supreme | 2026-06-07 | - |
| 31 | Kraken | 2026-06-07 | - |
| 32 | Thermonuclear smoke devil | 2026-06-07 | - |
| 33 | Abyssal Sire | 2026-06-07 | - |
| 34 | King Black Dragon | 2026-06-07 | - |
| 35 | Chaos Elemental | 2026-06-07 | - |
| 36 | Scorpia | 2026-06-07 | - |
| 37 | Chaos Fanatic | 2026-06-07 | - |
| 38 | Venenatis | 2026-06-07 | - |
| 39 | Vet'ion | 2026-06-07 | - |
| 40 | Callisto | 2026-06-07 | - |
| 41 | Skotizo | 2026-06-07 | - |
| 42 | Hespori | 2026-06-07 | - |
| 43 | Corrupted Gauntlet | 2026-06-07 | - |
| 44 | The Gauntlet | 2026-06-07 | - |
| 45 | Barrows | 2026-06-07 | - |
| 46 | Zalcano | 2026-06-07 | - |
| 47 | Sol Heredit | 2026-06-07 | - |
| 48 | Crazy archaeologist | 2026-06-07 | - |
| 49 | Araxxor | - | 1 (high: travel — fairy ring CLS wrong region) |
| 50 | Perilous Moons | 2026-06-07 | - |
| 51 | The Hueycoatl | 2026-06-07 | - |
| 52 | Yama | 2026-06-07 | - |
| 53 | Doom of Mokhaiotl | 2026-06-07 | - |
| 54 | Royal Titans | 2026-06-07 | - |
| 55 | Amoxliatl | 2026-06-07 | - |
| 56 | Brutus | 2026-06-07 | - |
| 57 | Shellbane Gryphon | 2026-06-07 | - |
| 58 | Scurrius | 2026-06-07 | - |
| 59 | The Fight Caves | 2026-06-07 | - |
| 60 | The Inferno | 2026-06-07 | - |
| 61 | Deranged Archaeologist | 2026-06-07 | - |

---

## Findings (STANDS)

### F1 — Desert Treasure II bosses: fabricated `requirements.skills` access gates — high
- **Sources (8):** Duke Sucellus, The Leviathan, The Whisperer, Vardorvis, and the four `(Awakened)` variants.
- **check:** requirements (skill gate) judgment → `wiki_lookup` + live wiki fetch + `wiki_updates` → `domain-skeptic` (STANDS, both passes).
- **ours:** each source's `requirements.skills` holds a hard skill gate, and the Awakened variants escalate it:
  - Duke Sucellus / (Awakened): `MINING 65` — `drop_rates.json:1646`, `drop_rates.json:2292`
  - The Leviathan: `RANGED 70` — `drop_rates.json:1806`; (Awakened): `RANGED 75` — `drop_rates.json:2492`
  - The Whisperer: `RANGED 75` — `drop_rates.json:1964`; (Awakened): `RANGED 80` — `drop_rates.json:2688`
  - Vardorvis: `ATTACK 70` — `drop_rates.json:2133`; (Awakened): `ATTACK 75` — `drop_rates.json:2892`
- **authoritative (raw receipts, oldschool.runescape.wiki, fetched 2026-06-07):**
  - The Whisperer: *"there are no stated skill level requirements to fight The Whisperer. The only explicit requirement … completed Desert Treasure II - The Fallen Empire … no minimum Combat, Magic, Ranged, or other skill levels … no additional skill requirements … for the Awakened variant beyond needing an Awakener's orb."*
  - Duke Sucellus: *"there are no stated skill level requirements … no mention of Mining, Herblore, or any other skill … no minimum Herblore or Mining level specified [for the salt/vent prep] … Both the standard and Awakened variants show identical requirement information with no skill prerequisites."*
  - The Leviathan: *"there are no explicit skill level requirements … The only mentioned requirement is … Desert Treasure II … Awakened … requires an Awakener's orb … but no additional skill requirements."*
  - Vardorvis: *"there are no explicit skill level requirements … The only requirement mentioned is Quest Completion … recommendations for efficiency, not access requirements."*
  - `wiki_updates` (Whisperer, Vardorvis): `count: 0` since 2025-01-01 → not staleness drift.
- **domain-skeptic:** STANDS ×2 (The Whisperer adjudicated separately; Duke/Leviathan/Vardorvis adjudicated together, per-source STANDS). Refutation vectors worked and failed: no hidden area/fight skill gate; Duke's vent-mining imposes no Mining level; Awakened adds only the orb (so a *higher* level for the identical encounter is incoherent); the wiki's only efficiency suggestions are unrelated (e.g. Magic 90+/Prayer 77+ for Augury) and live in no "requirement."
- **why it matters:** `requirements.skills` is the field the plugin uses to gate/lock a source. A DT2-complete account below the listed level can fight these bosses but would be wrongly locked out of the source. Same schema shape that, for General Graardor, correctly encodes the real Strength-70 GWD gate — so this is a functional defect, not a cosmetic note.
- **action (for the separate fix PR):** remove the `skills` array from `requirements` on all 8 entries; **keep** `quests: ["DESERT_TREASURE_II__THE_FALLEN_EMPIRE"]`.
- **status:** open

### F2 — Araxxor: travel guidance cites fairy ring `CLS`, which lands in Kandarin (Yanille), not Morytania — high
- **Source:** Araxxor (npcId 13668).
- **check:** fairy-ring-code judgment → `WebFetch` (OSRS Wiki Fairy ring + Araxxor pages) + `wiki_updates` → `domain-skeptic` (STANDS).
  Note: `travel_path_verify` v1 does **not** catch this — it only checks coordinate bounds, not fairy-ring-code-to-destination semantics (per its own `notes`).
- **ours:** the Araxxor route prose claims `fairy ring CLS` as the route to the Araxyte cave in NE Morytania, in **three** places:
  - source `travelTip` — `drop_rates.json:9317` (`"Drakan's medallion -> Darkmeyer, or fairy ring CLS"`)
  - step #2 `description` — `drop_rates.json:9336` (`"… or fairy ring CLS for the fastest unquested route"`)
  - step #2 `travelTip` — `drop_rates.json:9342` (`"… run east, or fairy ring CLS"`)
- **authoritative (raw receipts, oldschool.runescape.wiki, fetched 2026-06-07):**
  - Fairy ring page: **`CLS` → "Islands: Yanille Chain"** (Kandarin) — opposite side of the map from Morytania. (Morytania's code is `CKS` → Canifis.)
  - Araxxor page: *"The wiki page does not mention any fairy ring codes for reaching Araxxor's lair. The only travel method specified is: 'Araxxor's lair is accessible from the Morytania Spider Cave … north-east of Darkmeyer.'"* Canonical route is Drakan's medallion → Darkmeyer.
  - `wiki_updates` (Araxxor): `count: 0` since 2024-08-01 → not staleness drift.
- **domain-skeptic:** STANDS. Vectors failed: `CLS` is not in/near Morytania; no fairy ring serves the Araxyte cave; even the nearest Morytania code `CKS` (Canifis) does not reach it, so the fix is to **strike the fairy-ring claim**, not swap `CLS`→`CKS` while keeping "fastest unquested route."
- **action (for the separate fix PR):** remove the `fairy ring CLS` clause from all three strings; keep the Drakan's medallion → Darkmeyer route.
- **status:** open

---

## Considered & refuted (not logged as defects)

### Shellbane Gryphon — `travel_path_verify` coord-out-of-bounds → REFUTED
- `travel_path_verify` flagged Shellbane Gryphon steps #2–#4 at tile `(13993, 9901, 0)` as outside legal
  surface bounds (x 900..4500, y 1100..13500).
- `domain-skeptic` **REFUTED**: the project's own `verified_scene_ids.json` records this as a deliberately
  **instanced** coordinate for "The Great Conch island (instance coords ~13993, 9901, plane 0)" with a prior
  documented coordinate fix (commit `e4dd6a06`). OSRS instanced areas legitimately use coordinates far beyond
  the surface x-bound; `travel_path_verify` v1 models only surface space and false-positives on instanced
  sources. The value is correct by design. **Not a finding.**

## Coverage notes (honesty about scope)

- **IDs:** exhaustive — `cache_ids` ran over every item/NPC/object id in all 61 sources.
- **Coordinates:** exhaustive bound-check via `travel_path_verify` over all 61 sources (808 steps scanned across the dataset).
- **Fairy-ring codes:** every code cited in BOSSES guidance was verified (BJS, CIR, AKQ, CKS, BKP, CLS, AIQ, BLP, DIP).
  Only `CLS` (Araxxor) was wrong.
- **Requirements gates:** externally verified against the wiki for GWD ×4 (Troll Stronghold + per-room 70 skill gates — correct),
  Nex (consistent), all 8 DT2 entries (wrong — F1), Amoxliatl (Slayer 48 + The Heart of Darkness — correct), and Royal Titans
  (no requirement — correct). The remaining sources carry standard Slayer-level / single-quest gates consistent with known
  game data and were not contradicted by any receipt; they were not each independently re-fetched. Yama's page 404'd at the
  tried slug, so its `A_KINGDOM_DIVIDED` gate was not externally re-confirmed (left as `verified` on domain grounds, flagged here for transparency).
- **Travel direction:** spot-checked alongside the fairy-ring/coord work; no additional standing discrepancy found.
