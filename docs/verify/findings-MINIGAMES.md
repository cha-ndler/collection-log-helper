# Data-verification findings — MINIGAMES

- **Category:** `MINIGAMES` (25 sources in `src/main/resources/com/collectionloghelper/drop_rates.json`)
- **Branch:** `verify/MINIGAMES`
- **Date:** 2026-06-07
- **MCP pre-flight:** `resource_health` → all sources `ok` (osrs-wiki, prices-api, wise-old-man, temple-osrs, runelite-ids, mejrs-data).
- **Scope:** This file is intentionally separate from the shared `data-verification-log.md` so parallel
  per-category verification sessions do not conflict. It records findings for the MINIGAMES category only.
- **Rule:** verification only — **no edits were made to `drop_rates.json`**. Fixes are a separate reviewed PR.

## Method

Two checks per source, exactly as scoped:

1. **IDs** — `validate_drop_rates --check cache_ids --source_name "<source>"`, run per source. The
   **abextm game cache is the sole id authority**; ids were *not* adjudicated against Temple/wiki.
2. **Judgment** — requirements (quest/skill/diary gates), fairy-ring codes, arrival coordinates
   (`travel_path_verify`), and travel direction. A candidate was logged **only with a cited raw receipt**,
   then passed through the `domain-skeptic` agent; **only `STANDS` verdicts are recorded as findings.**

Multi-source clog items are correct by design (never flagged as "duplicate"). Unsired `25624` is
cache-confirmed and was not flagged.

## Result summary

- **IDs (cache_ids):** all 25 MINIGAMES sources **id-clean** — every item/NPC/object id resolves in the
  abextm cache. The per-source runs surfaced name-display *mismatches* on 7 sources (Hallowed Sepulchre,
  Brimhaven Agility Arena, Pest Control, Shades of Mort'ton, Mastering Mixology, Castle Wars, Soul Wars,
  Trouble Brewing). **None is an id error** — each is an intentional collection-log disambiguation label
  (variant/charge/colour suffixes such as `(Brimhaven)`, `Mysterious page 1..5`, `(red)/(white)/(gold)`,
  `(uncharged)`, `(disassembled)`). Cache is the id authority and the ids are valid, so these are **not
  findings**. See *Considered & not logged* below.
- **Coordinates (`travel_path_verify`):** zero MINIGAMES hits. The global run (808 steps / 225 sources)
  flagged only non-MINIGAMES sources (Treasure Trails, Shellbane Gryphon, Miscellaneous). All MINIGAMES
  coords are within legal world bounds.
- **Fairy-ring codes:** every cited code verified against the wiki fairy-ring code table. **Three wrong**
  (Brimhaven `CKR`, Volcanic Mine `DLR`, Fishing Trawler `BKP`).
- **Requirements gates:** quest+skill gates spot-verified against the wiki. Mage Training Arena
  (`MAGIC 7`) and Vale Totems (`Vale Totems` miniquest + `FLETCHING 20`) **confirmed correct**. Brimhaven
  Agility Arena carries a **fabricated hard `AGILITY 40`** gate (below).

**Open findings: 4 (1 × high, 3 × low). All four passed the `domain-skeptic` gate as `STANDS`.**

| # | Source | Data verified (V1) | Open findings |
|---|--------|--------------------|---------------|
| 1 | Tempoross | 2026-06-07 | - |
| 2 | Wintertodt | 2026-06-07 | - |
| 3 | Hallowed Sepulchre | 2026-06-07 | - |
| 4 | Guardians of the Rift | 2026-06-07 | - |
| 5 | Brimhaven Agility Arena | - | 2 (high: fabricated `AGILITY 40` gate; low: wrong fairy ring `CKR`) |
| 6 | Giants' Foundry | 2026-06-07 | - |
| 7 | Pest Control | 2026-06-07 | - |
| 8 | Mage Training Arena | 2026-06-07 | - |
| 9 | Shades of Mort'ton | 2026-06-07 | - |
| 10 | Mahogany Homes | 2026-06-07 | - |
| 11 | Mastering Mixology | 2026-06-07 | - |
| 12 | Volcanic Mine | - | 1 (low: fabricated fairy ring `DLR` fallback) |
| 13 | Tithe Farm | 2026-06-07 | - |
| 14 | Gnome Restaurant (Scarfs) | 2026-06-07 | - |
| 15 | Fishing Trawler | - | 1 (low: wrong fairy ring `BKP`, should be `DJP`) |
| 16 | Temple Trekking | 2026-06-07 | - |
| 17 | Rogues' Den | 2026-06-07 | - |
| 18 | Castle Wars | 2026-06-07 | - |
| 19 | Soul Wars | 2026-06-07 | - |
| 20 | Last Man Standing | 2026-06-07 | - |
| 21 | Vale Totems | 2026-06-07 | - |
| 22 | Trouble Brewing | 2026-06-07 | - |
| 23 | Barbarian Assault | 2026-06-07 | - |
| 24 | Barracuda Trials | 2026-06-07 (unreleased Sailing content; not externally verifiable) | - |
| 25 | Gnome Restaurant (Seed Pods) | 2026-06-07 | - |

## Findings

### Finding 1 — Brimhaven Agility Arena: fabricated `AGILITY 40` requirement — severity high

- **Where:** `Brimhaven Agility Arena` → `requirements.skills` = `[{"skill":"AGILITY","level":40}]`.
- **Receipt (OSRS Wiki, Brimhaven Agility Arena):** *"The course has no requirements to access other
  than a 200 coins fee paid to Cap'n Izzy No-Beard before each entry... Though it is possible to reach
  dispensers without any Agility levels, level 20 Agility is required to pass the pressure pad and floor
  spike obstacles, while level 40 Agility is required for the spinning blades and darts."*
- **Why it's wrong:** there is no access/reward gate at 40 Agility. Tickets and the clog rewards
  (Brimhaven Graceful recolours, Pirate's hook) are obtainable below 40 (even at 0) Agility — level 40
  only unlocks specific obstacles, not the source. A hard `AGILITY 40` gate incorrectly hides a
  reachable clog source from the low-level accounts who most need the recommendation.
- **domain-skeptic:** `STANDS` (high). No account type makes 40 Agility a hard requirement to obtain
  rewards; the multi-source / variant / account-type vectors do not apply.
- **Suggested fix (separate PR):** drop the gate, or model it as a soft/efficiency note (~20–40 Agility),
  not a blocking requirement.
- **status:** resolved 2026-06-07 (removed the `requirements` block - it held only the fabricated AGILITY 40; the source has no real access gate; `lintDropRates build` green).

### Finding 2 — Brimhaven Agility Arena: wrong fairy ring code `CKR` — severity low

- **Where:** top-level `travelTip` "Fairy ring CKR -> Brimhaven, or charter ship to Brimhaven" and
  `guidanceSteps[0].travelTip` "Fairy ring CKR -> run east to arena entrance".
- **Receipt (OSRS Wiki fairy-ring code table):** `CKR` = "Karamja: South of Tai Bwo Wannai Village"
  (deep south-central Karamja). The wiki Brimhaven travel section lists **no** fairy-ring route.
- **Why it's wrong:** the arena is at `(2809,3194)` in **northern** Karamja; CKR (~`2801,3003`) is
  ~191 tiles **south** (`coordinate_helper` distance). The route is a long run north, not "east", and CKR
  is not a sensible Brimhaven approach.
- **domain-skeptic:** `STANDS` (low) — travel-hint accuracy only; does not hide the source or affect ids.
- **status:** resolved 2026-06-07 (struck the `CKR` clause from all 3 strings; kept the charter-ship route; `lintDropRates build` green).

### Finding 3 — Volcanic Mine: fabricated fairy ring `DLR` fallback — severity low

- **Where:** `guidanceSteps[1].travelTip` "Digsite pendant -> Mushroom Forest + run NE, or Fairy ring DLR"
  and the step description "...or use Fairy ring DLR for the closest fairy ring fallback".
- **Receipt (OSRS Wiki, Volcanic Mine travel section):** lists only the Volcanic mine teleport and
  Digsite-pendant routes — **no fairy ring**. Fossil Island is reached by barge and has no fairy ring.
  `DLR` = "Islands: Poison Waste south of Isafdar" (Tirannwn), an unrelated region.
- **Why it's wrong:** there is no fairy-ring route to the Volcanic Mine; DLR lands in Tirannwn. (The
  top-level `travelTip` "Digsite pendant -> Fossil Island" is correct — only the DLR addition is wrong.)
- **domain-skeptic:** `STANDS` (low) — misleading fallback hint; does not hide the source or affect ids.
- **status:** resolved 2026-06-07 (struck the `DLR` clause from both strings; kept the Digsite-pendant route; `lintDropRates build` green).

### Finding 4 — Fishing Trawler: wrong fairy ring code `BKP` — severity low

- **Where:** top-level `travelTip` "fairy ring BKP -> Port Khazard" and `guidanceSteps[0].travelTip`/
  description "fairy ring BKP -> run south".
- **Receipt (OSRS Wiki):** `BKP` = "Feldip Hills: Chompy Marsh, south of Castle Wars". The Fishing
  Trawler page's fairy-ring bullet is **`DJP`** ("Fairy ring code DJP, then run south") — DJP is by the
  Tower of Life, just north of Port Khazard.
- **Why it's wrong:** a fairy-ring-then-south route to the Trawler is legitimate, but the code is wrong —
  BKP is in Feldip Hills, far south of and unconnected to Port Khazard; running south from BKP heads away
  from Khazard. The correct code is `DJP`.
- **domain-skeptic:** `STANDS` (low) — route shape is right, only the code letters are wrong; does not
  hide the source or affect ids.

## Considered & not logged

- **cache_ids name-display mismatches (7 sources, ~57 entries):** every flagged id resolves in the abextm
  cache; the warnings are name-label differences vs the cache canonical name, all intentional
  collection-log disambiguations:
  - Hallowed Sepulchre — `Mysterious page 1..5` (5 distinct ids 24763/24765/24767/24769/24771, cache name
    "Mysterious page"); `Strange old lockpick` (24740, cache "(full)"); `Ring of endurance` (24844, cache
    "(uncharged)") — the uncharged variant is the clog-tracked item.
  - Brimhaven Agility Arena — Graceful `(Brimhaven)` recolour suffixes (21061/21067/21070/21073/21076/21064).
  - Pest Control — `Void seal` (11666, cache "Void seal(8)").
  - Shades of Mort'ton — `Amulet of the damned` (12851, cache "(full)"); lock colour names
    (`Bronze/Steel/Black/Silver/Gold lock` vs cache "…locks").
  - Mastering Mixology — `Chugging barrel` (30002, cache "(disassembled)").
  - Castle Wars — `Decorative …` colour/slot suffixes and `Castlewars hood/cloak (Saradomin/Zamorak)`.
  - Soul Wars — `Soul cape (blue)` (25346, cache "Soul cape").
  - Trouble Brewing — `Rum (red)`/`Rum (blue)` (8940/8941, cache "Rum").
  Cache is the sole id authority and the ids are valid → **not findings**.
- **Mage Training Arena `MAGIC 7`** — wiki: *"Players need a minimum of level 7 Magic to participate"* →
  correct.
- **Vale Totems `Vale Totems` miniquest + `FLETCHING 20`** — wiki: *"Completion of the Vale Totems
  miniquest. Level 20 Fletching is required"* → both correct.
- **Shades of Mort'ton `BKR`, Tithe Farm `CIR`** — fairy-ring codes verified against the kingdom they
  serve (Mort Myre/Canifis and Kebos/Kourend respectively); within the correct region, not logged.
- **Pest Control / Soul Wars combat-level gates** — the schema models skill/quest/diary gates only;
  combat-level minimums are not representable and were not flagged.
- **Multi-source clog items** — left as correct by design.
