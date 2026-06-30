# Data Verification Log

> The `runelite-dev-toolkit` MCP autonomously grabs guidance steps + required/recommended
> items per activity from the OSRS wiki. **That data is `unverified` until it is (V1)
> cross-checked against an independent source AND (V2) confirmed in a running client.**
> This log is the queue + findings ledger for those two loops, kept honest by V3 (drift).

## How the loops use this file
- **V1 (data verification):** pick the next source with `-` in *Data verified*. Run independent
  cross-checks (`cross_check_ids`, `temple_lookup`, `compare_source`, `coordinate_helper`,
  `cache_diff_check`). If a check disagrees with the source, append a **Findings** block and
  leave *Data verified* as `-`. If all checks agree, stamp it with the date. Never edit the
  source data here  -  verification only flags; humans (or a separate fix PR) resolve.
- **V2 (in-game ground truth):** pick the next source with `-` in *In-game*. Run
  `/validate-ingame`; record PASS / FAIL(what was wrong) / DEFERRED. A FAIL on auto-grabbed
  data is a real finding  -  append a Findings block.
- **V3 (drift):** when `wiki_updates` / `cache_diff_check` show a source's wiki page or IDs
  changed, clear both stamps back to `-` so V1/V2 re-check it.

## Severity
- **blocker**  -  wrong canonical collection-log item id (`temple_lookup` mismatch) or a
  nonexistent item/NPC/object id. Breaks the feature.
- **high**  -  wrong drop rate, coords, or a mis-stated *required* item (access-gating).
- **low**  -  questionable *recommended* item, prose nit, account-type assumption.

## Status (auto-grabbed sources to verify)

| # | Source | Category | Data verified (V1) | In-game (V2) | Open findings |
|---|--------|----------|--------------------|--------------|---------------|
| 1 | Abyssal Sire | BOSSES | `2026-06-30` | - | - |
| 2 | Alchemical Hydra | BOSSES | `2026-06-30` | - | - |
| 3 | Amoxliatl | BOSSES | `2026-06-30` | - | - |
| 4 | Araxxor | BOSSES | `2026-06-30` | - | -|
| 5 | Barrows | BOSSES | `2026-06-30` | - | - |
| 6 | Brutus | BOSSES | `2026-06-30` | - | - |
| 7 | Bryophyta | BOSSES | `2026-06-30` | - | -|
| 8 | Callisto | BOSSES | `2026-06-30` | - | -|
| 9 | Cerberus | BOSSES | `2026-06-30` | - | - |
| 10 | Chaos Elemental | BOSSES | `2026-06-30` | - | - |
| 11 | Chaos Fanatic | BOSSES | `2026-06-30` | - | - |
| 12 | Commander Zilyana | BOSSES | `2026-06-30` | - | - |
| 13 | Corporeal Beast | BOSSES | `2026-06-30` | - | - |
| 14 | Corrupted Gauntlet | BOSSES | `2026-06-30` | - | - |
| 15 | Crazy archaeologist | BOSSES | `2026-06-30` | - | - |
| 16 | Dagannoth Prime | BOSSES | - | - | 1 (PR #1084) |
| 17 | Dagannoth Rex | BOSSES | - | - | 1 (PR #1085) |
| 18 | Dagannoth Supreme | BOSSES | `2026-06-30` | - | - |
| 19 | Deranged Archaeologist | BOSSES | `2026-06-30` | - | - |
| 20 | Doom of Mokhaiotl | BOSSES | - | - | 1 (PR #1086) |
| 21 | Duke Sucellus | BOSSES | `2026-06-30` | - | - |
| 22 | Duke Sucellus (Awakened) | BOSSES | `2026-06-30` | - | - |
| 23 | General Graardor | BOSSES | `2026-06-30` | - | - |
| 24 | Giant Mole | BOSSES | `2026-06-30` | - | - |
| 25 | Grotesque Guardians | BOSSES | `2026-06-30` | - | - |
| 26 | Hespori | BOSSES | `2026-06-30` | - | - |
| 27 | K'ril Tsutsaroth | BOSSES | `2026-06-30` | - | - |
| 28 | Kalphite Queen | BOSSES | - | - | 1 (PR #1087) |
| 29 | King Black Dragon | BOSSES | `2026-06-30` | - | - |
| 30 | Kraken | BOSSES | `2026-06-30` | - | - |
| 31 | Kree'arra | BOSSES | - | - | - |
| 32 | Nex | BOSSES | - | - | - |
| 33 | Obor | BOSSES | - | - | - |
| 34 | Perilous Moons | BOSSES | - | - | - |
| 35 | Phantom Muspah | BOSSES | - | - | - |
| 36 | Phosani's Nightmare | BOSSES | - | - | - |
| 37 | Royal Titans | BOSSES | - | - | - |
| 38 | Sarachnis | BOSSES | - | - | - |
| 39 | Scorpia | BOSSES | - | - | - |
| 40 | Scurrius | BOSSES | - | - | - |
| 41 | Shellbane Gryphon | BOSSES | - | - | - |
| 42 | Skotizo | BOSSES | - | - | - |
| 43 | Sol Heredit | BOSSES | - | - | - |
| 44 | The Fight Caves | BOSSES | - | - | - |
| 45 | The Gauntlet | BOSSES | - | - | - |
| 46 | The Hueycoatl | BOSSES | - | - | - |
| 47 | The Inferno | BOSSES | - | - | - |
| 48 | The Leviathan | BOSSES | - | - | - |
| 49 | The Leviathan (Awakened) | BOSSES | - | - | - |
| 50 | The Nightmare | BOSSES | - | - | - |
| 51 | The Whisperer | BOSSES | - | - | - |
| 52 | The Whisperer (Awakened) | BOSSES | - | - | - |
| 53 | Thermonuclear smoke devil | BOSSES | - | - | - |
| 54 | Vardorvis | BOSSES | - | - | - |
| 55 | Vardorvis (Awakened) | BOSSES | - | - | - |
| 56 | Venenatis | BOSSES | - | - | - |
| 57 | Vet'ion | BOSSES | - | - | - |
| 58 | Vorkath | BOSSES | - | - | - |
| 59 | Yama | BOSSES | - | - | - |
| 60 | Zalcano | BOSSES | - | - | - |
| 61 | Zulrah | BOSSES | - | - | - |
| 62 | Aberrant Spectre | SLAYER | - | - | - |
| 63 | Abyssal Demon | SLAYER | - | - | - |
| 64 | Aquanite | SLAYER | - | - | - |
| 65 | Araxyte | SLAYER | - | - | - |
| 66 | Basilisk | SLAYER | - | - | - |
| 67 | Basilisk Knight | SLAYER | - | - | - |
| 68 | Bloodveld | SLAYER | - | - | - |
| 69 | Brine Rat | SLAYER | - | - | - |
| 70 | Cave Crawler | SLAYER | - | - | - |
| 71 | Cave Horror | SLAYER | - | - | - |
| 72 | Cave Kraken | SLAYER | - | - | - |
| 73 | Cockatrice | SLAYER | - | - | - |
| 74 | Crawling Hand | SLAYER | - | - | - |
| 75 | Custodian Stalker | SLAYER | - | - | - |
| 76 | Dark Beast | SLAYER | - | - | - |
| 77 | Demonic gorillas | SLAYER | - | - | - |
| 78 | Drake | SLAYER | - | - | - |
| 79 | Dust Devil | SLAYER | - | - | - |
| 80 | Earthen Nagua | SLAYER | - | - | - |
| 81 | Fossil Island Wyvern | SLAYER | - | - | - |
| 82 | Frost Nagua | SLAYER | - | - | - |
| 83 | Gargoyle | SLAYER | - | - | - |
| 84 | Gryphon | SLAYER | - | - | - |
| 85 | Hydra | SLAYER | - | - | - |
| 86 | Infernal Mage | SLAYER | - | - | - |
| 87 | Jelly | SLAYER | - | - | - |
| 88 | Kurask | SLAYER | - | - | - |
| 89 | Lava Strykewyrm | SLAYER | - | - | - |
| 90 | Lizardman shaman | SLAYER | - | - | - |
| 91 | Mogre | SLAYER | - | - | - |
| 92 | Nechryael | SLAYER | - | - | - |
| 93 | Pyrefiend | SLAYER | - | - | - |
| 94 | Rockslug | SLAYER | - | - | - |
| 95 | Skeletal Wyvern | SLAYER | - | - | - |
| 96 | Smoke Devil | SLAYER | - | - | - |
| 97 | Spiritual Mage | SLAYER | - | - | - |
| 98 | Spiritual Mage (Zarosian) | SLAYER | - | - | - |
| 99 | Sulphur Nagua | SLAYER | - | - | - |
| 100 | Superior Slayer Monster | SLAYER | - | - | - |
| 101 | Terror Dog | SLAYER | - | - | - |
| 102 | Tormented Demons | SLAYER | - | - | - |
| 103 | Turoth | SLAYER | - | - | - |
| 104 | Vyrewatch Sentinel | SLAYER | - | - | - |
| 105 | Warped Creature | SLAYER | - | - | - |
| 106 | Wyrm | SLAYER | - | - | - |
| 107 | Chambers of Xeric | RAIDS | - | - | - |
| 108 | Chambers of Xeric (Challenge Mode) | RAIDS | - | - | - |
| 109 | Theatre of Blood | RAIDS | - | - | - |
| 110 | Theatre of Blood (Hard Mode) | RAIDS | - | - | - |
| 111 | Tombs of Amascut | RAIDS | - | - | - |
| 112 | Tombs of Amascut (300 Invocation) | RAIDS | - | - | - |
| 113 | Tombs of Amascut (500 Invocation) | RAIDS | - | - | - |
| 114 | Barbarian Assault | MINIGAMES | - | - | - |
| 115 | Barracuda Trials | MINIGAMES | - | - | - |
| 116 | Brimhaven Agility Arena | MINIGAMES | - | - | - |
| 117 | Castle Wars | MINIGAMES | - | - | - |
| 118 | Fishing Trawler | MINIGAMES | - | - | - |
| 119 | Giants' Foundry | MINIGAMES | - | - | - |
| 120 | Gnome Restaurant (Scarfs) | MINIGAMES | - | - | - |
| 121 | Gnome Restaurant (Seed Pods) | MINIGAMES | - | - | - |
| 122 | Guardians of the Rift | MINIGAMES | - | - | - |
| 123 | Hallowed Sepulchre | MINIGAMES | - | - | - |
| 124 | Last Man Standing | MINIGAMES | - | - | - |
| 125 | Mage Training Arena | MINIGAMES | - | - | - |
| 126 | Mahogany Homes | MINIGAMES | - | - | - |
| 127 | Mastering Mixology | MINIGAMES | - | - | - |
| 128 | Pest Control | MINIGAMES | - | - | - |
| 129 | Rogues' Den | MINIGAMES | - | - | - |
| 130 | Shades of Mort'ton | MINIGAMES | - | - | - |
| 131 | Soul Wars | MINIGAMES | - | - | - |
| 132 | Temple Trekking | MINIGAMES | - | - | - |
| 133 | Tempoross | MINIGAMES | - | - | - |
| 134 | Tithe Farm | MINIGAMES | - | - | - |
| 135 | Trouble Brewing | MINIGAMES | - | - | - |
| 136 | Vale Totems | MINIGAMES | - | - | - |
| 137 | Volcanic Mine | MINIGAMES | - | - | - |
| 138 | Wintertodt | MINIGAMES | - | - | - |
| 139 | Adamant Dragon | OTHER | - | - | - |
| 140 | Albatross (Boat Combat) | OTHER | - | - | - |
| 141 | Armoured Zombie | OTHER | - | - | - |
| 142 | Barracuda Salvage | OTHER | - | - | - |
| 143 | Boat Paints | OTHER | - | - | - |
| 144 | Brimstone Chest | OTHER | - | - | - |
| 145 | Camdozaal | OTHER | - | - | - |
| 146 | Catacombs of Kourend | OTHER | - | - | - |
| 147 | Champion's Challenge | OTHER | - | - | - |
| 148 | Chompy Bird Hunting | OTHER | - | - | - |
| 149 | Creature Creation (Frogeel) | OTHER | - | - | - |
| 150 | Creature Creation (Jubster) | OTHER | - | - | - |
| 151 | Creature Creation (Newtroost) | OTHER | - | - | - |
| 152 | Creature Creation (Spidine) | OTHER | - | - | - |
| 153 | Creature Creation (Swordchick) | OTHER | - | - | - |
| 154 | Creature Creation (Unicow) | OTHER | - | - | - |
| 155 | Cyclopes | OTHER | - | - | - |
| 156 | Elder Chaos Druids | OTHER | - | - | - |
| 157 | Elven Crystal Chest | OTHER | - | - | - |
| 158 | Fishy Salvage | OTHER | - | - | - |
| 159 | Forestry | OTHER | - | - | - |
| 160 | Fossil Island Notes | OTHER | - | - | - |
| 161 | Fountain of Rune | OTHER | - | - | - |
| 162 | Fremennik Salvage | OTHER | - | - | - |
| 163 | Glough's Experiments | OTHER | - | - | - |
| 164 | Great White Shark (Boat Combat) | OTHER | - | - | - |
| 165 | Hunter Guild | OTHER | - | - | - |
| 166 | Large Salvage | OTHER | - | - | - |
| 167 | Larran's Big Chest | OTHER | - | - | - |
| 168 | Lost Schematics | OTHER | - | - | - |
| 169 | Martial Salvage | OTHER | - | - | - |
| 170 | Miscellaneous | OTHER | - | - | - |
| 171 | Mithril Dragon | OTHER | - | - | - |
| 172 | Monkey Backpacks | OTHER | - | - | - |
| 173 | My Notes | OTHER | - | - | - |
| 174 | Narwhal (Boat Combat) | OTHER | - | - | - |
| 175 | Ocean Encounters | OTHER | - | - | - |
| 176 | Ogress Shaman | OTHER | - | - | - |
| 177 | Opulent Salvage | OTHER | - | - | - |
| 178 | Orcas (Boat Combat) | OTHER | - | - | - |
| 179 | Plundered Salvage | OTHER | - | - | - |
| 180 | Port Tasks | OTHER | - | - | - |
| 181 | Prifddinas Elf | OTHER | - | - | - |
| 182 | Pyramid Plunder | OTHER | - | - | - |
| 183 | Random Events | OTHER | - | - | - |
| 184 | Revenants | OTHER | - | - | - |
| 185 | Rune Dragon | OTHER | - | - | - |
| 186 | Sailing Misc | OTHER | - | - | - |
| 187 | Sea Treasures | OTHER | - | - | - |
| 188 | Shayzien Armour | OTHER | - | - | - |
| 189 | Small Salvage | OTHER | - | - | - |
| 190 | Stingray (Boat Combat) | OTHER | - | - | - |
| 191 | Stronghold of Security | OTHER | - | - | - |
| 192 | TzHaar | OTHER | - | - | - |
| 193 | Vampyre Kraken (Boat Combat) | OTHER | - | - | - |
| 194 | Waterfiend | OTHER | - | - | - |
| 195 | Wilderness God Wars Dungeon | OTHER | - | - | - |
| 196 | Zombie Pirate Locker | OTHER | - | - | - |
| 197 | Aerial Fishing | SKILLING | - | - | - |
| 198 | Black Chinchompas | SKILLING | - | - | - |
| 199 | Colossal Wyrm Agility | SKILLING | - | - | - |
| 200 | Cutting Squid | SKILLING | - | - | - |
| 201 | Deep Sea Fishing | SKILLING | - | - | - |
| 202 | Farming (Fruit Trees) | SKILLING | - | - | - |
| 203 | Fishing (Swordfish) | SKILLING | - | - | - |
| 204 | Mining (Gemstone Rocks) | SKILLING | - | - | - |
| 205 | Motherlode Mine | SKILLING | - | - | - |
| 206 | Pickpocketing Darkmeyer Vyre | SKILLING | - | - | - |
| 207 | Prifddinas Rabbit | SKILLING | - | - | - |
| 208 | Rooftop Agility | SKILLING | - | - | - |
| 209 | Runecrafting (Fire Runes) | SKILLING | - | - | - |
| 210 | Shooting Stars | SKILLING | - | - | - |
| 211 | Thieving (Seed Stalls) | SKILLING | - | - | - |
| 212 | Underwater Crabs | SKILLING | - | - | - |
| 213 | Woodcutting (Teak Trees) | SKILLING | - | - | - |
| 214 | Beginner Treasure Trails | CLUES | - | - | - |
| 215 | Easy Treasure Trails | CLUES | - | - | - |
| 216 | Elite Treasure Trail Rewards (Rare) | CLUES | - | - | - |
| 217 | Elite Treasure Trails | CLUES | - | - | - |
| 218 | Hard Treasure Trail Rewards (Rare) | CLUES | - | - | - |
| 219 | Hard Treasure Trails | CLUES | - | - | - |
| 220 | Master Treasure Trail Rewards (Rare) | CLUES | - | - | - |
| 221 | Master Treasure Trails | CLUES | - | - | - |
| 222 | Medium Treasure Trails | CLUES | - | - | - |
| 223 | Scroll Cases | CLUES | - | - | - |
| 224 | Shared Treasure Trail Rewards | CLUES | - | - | - |
| 225 | The Mimic | CLUES | - | - | - |

_Total: 225 sources. Stamp format: `YYYY-MM-DD`. Set *Open findings* to the count
when a Findings block is added for that source._

## Findings (append-only)

<!-- One block per finding. Copy this template:

### <source> - <field> - <severity>
- check: <which cross-check / in-game step surfaced it>
- ours: <value currently in drop_rates.json>
- authoritative: <wiki / temple_lookup / RuneLite ID value>
- action: <proposed fix, or 'needs human call: why'>
- status: open
-->

### 2026-06-25 session — full drop-rate leg + step-coord sweep complete (4 fixes); id leg blocked, membership leg started

**Skeletal Wyvern - guidanceSteps[2].objectId - defect (FIXED)**
- check: P3 step-level semantic coord/id sweep (objectId-spawns-near-coord)
- ours: `objectId 30100` ("Climb-down") on the AID trapdoor travel step
- authoritative: cache `object_lookup(30100)` = "Armed Forces Report" (Varlamore
  readable, no actions, sole spawn (1538,10224)) — not a trapdoor; coord (3008,3150)
  is correct (Thurgo/AID entrance)
- action: removed objectId + objectInteractAction (no cache-confirmed AID-trapdoor
  id to substitute; step is ARRIVE_AT_TILE coord-gated). Skeptic: SURVIVES.
- status: merged (#1004 / PR #1005)

**BOSSES drop-rate leg — COMPLETE (61/61 reconciled or deferred), 0 real defects.**
Effective-rate cases verified CORRECT and skeptic-refuted (do NOT re-flag):
- Zulrah uniques 1/512 (twice-per-kill main table) and mutagens ~2/13107 — correct.
- Royal Titans (via "Eldric the Ice King") uniques 1/75, Giantsoul amulet 1/16,
  pet Bran 1/1500 (double-roll) — correct; coord (3008,3150) AID is correct (not a
  Varlamore boss — accessed via the Asgarnian Ice Dungeon).
- Fight Caves Tzrek-jad 1/67 and Inferno Jal-nib-rek 1/43 — wiki's combined
  on-Slayer-task + cape-gamble effective rates (not the bare 1/200 / 1/100) — correct.
- DT2 quartz 1/200 / tablets 1/25 vs wiki 1/206.6 / 1/25.8 — intentional rounding.

**DEFERRED (BOSSES, unverifiable via wiki_lookup — prose-only pages, need alt source):**
- Sol Heredit (Fortis Colosseum) — glory-weighted rewards, no parseable table.
- Perilous Moons (Lunar Chest) — moons-subdued scaling, no parseable table.

**Non-coord id-layer flags (deferred to the blocked cache-id leg, opcode-160 / tk#92):**
- Motherlode Mine npcId 6712 — cache `npc_lookup` null; mejrs = "Guard" (Ardougne);
  coord is correct MLM. Re-check under `validate_drop_rates --check cache_ids` once
  the MCP server restarts.

**Item-id leg (DoD #1): BLOCKED** — `cross_check_ids` still throws `7938: unknown
opcode 160`. Patch re-applied to the live `0.5.27/mcp/bundle.mjs` (auto-update wiped
the prior patch); needs an MCP server restart to go live. tk#92.

**DROP-RATE LEG (DoD #3) — COMPLETE across all 7 categories / 226 sources.**
3 rate defects found, skeptic-survived, and fixed (+ the Skeletal Wyvern coord/id fix above):

**Lava Strykewyrm - Dragon metal sheet (31996) - rate defect (FIXED)**
- ours 0.0025 (1/400, stale pre-Sailing); wiki 1/115 (rate increased in the Sailing
  era; Sailing live 2025-11-19). action: -> 0.008696. Skeptic SURVIVES (high).
- status: merged (#1006 / PR #1007)

**Fishing (Swordfish) - Big swordfish (7991) - rate defect (FIXED)**
- ours 0.000833 (1/1200); wiki 1/2500 per raw swordfish caught. Self-contradicted by
  the same item at 0.0004 under Miscellaneous; Rada's doubling is clog-excluded.
  action: -> 0.0004. Skeptic SURVIVES (low). status: merged (#1008 / PR #1009)

**Mithril Dragon - Dragon full helm (11335) - rate defect (FIXED)**
- ours 0.000122 (1/8192, 2^13); wiki 1/32768 (2^15) — transcription typo, ~4x too
  generous. action: -> 0.0000305. Skeptic SURVIVES (high). status: PR #1011 (automerge)

**Clean categories (0 defects):** RAIDS (7 — point/level tables internally consistent,
ToA-500 caps modeled correctly), MINIGAMES (25 — point-shop deferred, RNG rates match),
CLUES (12 — per-clue effective-rate formula reconciles all tiers).

**False positives caught by cite-or-discard / skeptic-gate (do NOT re-flag):**
- Lizardman shaman Dragon warhammer 1/3000 is CORRECT (buffed from 1/5000 on
  2024-05-29) — NOT a defect.
- Vyrewatch Sentinel Blood shard 1/1500 — correct.
- All Sailing "Boat Combat" rates CONFIRMED live (Great White Shark metal sheet 1/200,
  Vampyre Kraken 1/150, Narwhal horn 1/20, etc.) — Sailing is live content, verify don't defer.

**Rate DEFERRED (no single parseable wiki table — point-shop / aggregate / composite):**
Sol Heredit, Perilous Moons (BOSSES); ~23 OTHER aggregate/point-shop sources;
~11 MINIGAMES point-shops; Shared Treasure Trail Rewards (CLUES composite). Reasons logged.

_REMAINING before DoD closes: item-id leg (#1, blocked on MCP restart);
TempleOSRS clog-membership leg (#4, pilot launched); operator seam round (#7, P5)._

---

## 2026-06-27 — Guidance-engine loop-class leg (engine, not data)

A live Shades-of-Mort'ton run surfaced a CLASS of guidance-engine defects. Engine map finding:
the engine already resolves the active step from world-state AT ACTIVATION
(skipSatisfiedSteps -> advanceToFurthestSatisfiedState -> checkDepletionAndMaybeReset),
it just never re-ran mid-session. Operator gate chose "wire the existing chain" over
"build a new resolver DSL".

- **F1 (loop not state-driven mid-session)** -> PR #1018: `GuidanceSequencer.reDeriveState()`
  re-runs the activation chain on inventory/teleport, opt-in to gather-loop sources
  (restock-fuel signal, no new schema), notify-only-on-change. 6-case Shades reference
  matrix. CI build GREEN. Awaiting operator merge (structural -> no automerge).
- **F3 (stale highlight not cleared)** -> PR #1019: clear tier-driven highlight when a
  dynamic-tier step no longer matches inventory (used last key). 3 tests. CI pending.
- **F4 (over-highlight ~72 pyres)** -> operator chose NEAREST-ONLY (single nearest matched
  instance). CLH PR pending (own opt-in mechanism + tests); queued behind P2.
- **F5 (clog-unlock termination)** -> already handled: onItemObtained fires
  fireSequenceComplete on target-slot unlock; live confirm deferred to P4 seam round.
- **F7 (DiaryTierState perf)** -> minor; refresh already varbit-gated. Deferred.
- **F2 (phantom "reward pillar" MANUAL step) + F8 wording** -> DATA, loop 03 sweep.

**P2 (static corpus detectors)** -> toolkit PR #94 (operator chose Part C only; Part A/B seam
deferred to the live-client P4 round). Four detectors folded into `run_corpus_detectors`:
dead-end-manual-step (WARN), phantom-mechanic-text (WARN), over-highlight-risk (INFO),
loop-structure (INFO). Loop-marker carve-out enforced (D4Batch inconsistency never flagged).
Real-corpus sanity over 226 sources: dead-end-manual 12/8 (incl. Shades step[2]), phantom 22/19,
over-highlight 9/4, loop-structure 0. 385 toolkit tests green, bundle rebuilt. NOT automerge ->
operator merge + release + `/mcp` reconnect before P3 trusts it ("merged != live").

**P3 (Workflow corpus sweep)** not started — explicitly cross-session; runs after P2 is released
and reconnected. pipeline(survivors, run-detectors -> skeptic-verify -> fix-straggler).

**F4 nearest-only** CLH highlight PR still queued (operator chose nearest-only single).

---

## 2026-06-28 — Wiki-meta guidance-prose tail sweep (35 sources merged, one PR each)

Drained the open `findings-wiki-meta-2026-06.md` tranche-4 backlog (minigame/activity tail,
Hallowed Sepulchre → Stronghold of Security) plus 2 straggler tail sources, via a
re-verify → fix Workflow pipeline. NB: this is the **data/guidance-prose** backlog — distinct
from the P3 corpus-detector sweep (the dead-end-manual 12 / phantom 22 / over-highlight 9 = 43
detector survivors above), which remains pending P2 toolkit #94 release + `/mcp` reconnect.

- **Re-verify gate (read-only):** 37 sources re-checked against CURRENT data; each finding
  re-passed a domain-skeptic RAW-receipt gate (fresh verbatim wiki/MCP quote, cite-or-discard).
  **95 CONFIRMED, 18 DROPPED** (already-fixed / domain-refuted / absence-only receipt). Gate
  caught a false straggler: Fishing-Swordfish "boostable from 62" — relies on a non-existent
  "super fishing potion"; max Fishing boost is +5 (Admiral pie), so the data's "from 63" is
  CORRECT (do NOT re-flag).
- **Fixes:** 35 sources, one PR each, **prose/requirement-only** (no itemId / dropRate / coord /
  `loopBackToStep` / `loopCount` touched), `validate_drop_rates` + `guidance_lint` +
  `commit_message_lint` per source, CI `build` the authoritative gate, strict-protection
  serialized auto-merge. **PRs #1021–#1056.**
- Sample blockers/highs (all skeptic-SURVIVED with raw receipts):
  - Hallowed Sepulchre floor-5 Agility gate 72 → **92** (not boostable); entrance NE not SE.
  - Brimhaven Agility Arena reward NPC Cap'n Izzy → **Pirate Jackie the Fruit**; graceful
    recolour **250 vouchers full-set** (was "250 each / 1500 total" — 6× overstated).
  - Aerial Fishing fabricated fish names (violet perch → mottled eel, cerulean twitch →
    greater siren) + inverted Molch-pearl mechanic.
  - Guardians of the Rift three fabricated Arceuus/Mind-altar travel routes removed; unique
    points threshold 150 → **300** (Nov-2024 change).
  - Soul Wars Protect-Melee on Avatar; Ectoplasmator 2500 → **250**; removed phantom Unsired
    shards reward.
  - Champion's Challenge removed fabricated `DRAGON_SLAYER_I` quest gate (operator-reviewed, #1055).
- **5 sources held for review** (4 weak-receipt re-pulled in-PR + the Champion's Challenge
  structured `requirements` edit); all merged after operator confirmation (#1026/#1029/#1030/
  #1032/#1055). Hunter Guild #1044 transient BLOCK cleared and merged.
- Transient server-side rate-limiting during fan-out; re-running the affected sources cleared it.

## 2026-06-29 — Guidance mechanical sweep: PLANE-1 dead-step class (8 sources, all merged)

- **Method:** full 226-source guidance mechanical sweep (Spectator `--sweep`, live dev client,
  harness v0.7) → **216 PASS / 9 DIVERGENCE / 0 ERROR**. Root-caused, fixed, live-verified by
  relaunch + re-sweep, merged. Final re-sweep: **225 PASS / 0 DIVERGENCE** (the 1 STATE_DRIVEN
  is Shades of Mort'ton, activation-only by design) — corpus mechanically green.
- **Defect class PLANE-1:** an "enter dungeon/cavern/sanctuary via stairs/whirlpool/statue/board"
  step used `PLAYER_ON_PLANE` with `worldPlane` equal to the plane the player **already stands
  on** at the prior step. `CompletionChecker` evaluates `PLAYER_ON_PLANE` as a pure state check
  (`playerLocation.getPlane() == step.getWorldPlane()`), so the condition was true the instant
  the step activated — the engine auto-advanced past it and **the player never saw the
  instruction in-game**. Confirmed by the live-engine sweep (double-advance) + the engine source.
- **Fix pattern:** replace `PLAYER_ON_PLANE` with `ARRIVE_AT_ZONE` over the actual destination,
  each zone anchored to a RAW spawn/in-data receipt; the original highlight (object/tile) is left
  untouched. The "instanced" trio proved to have **static** destination coords (spawn-data), so no
  in-game capture was needed.
  - Mithril Dragon **#1060** (Ancient Cavern upper, p1 · npc 2919) / Waterfiend **#1061**
    (lower, p0 · npc 2916) — same cavern, different levels.
  - Catacombs of Kourend **#1062** (underground, p0) / Barrows **#1063** (tunnels, p0,
    dropped stale `completionPlane`) / Revenants **#1064** (caves, p0 · npc 7881).
  - The Nightmare **#1065** / Phosani's Nightmare **#1066** (Sisterhood Sanctuary, p1 · npc 9460)
    / Fishing Trawler **#1067** (trawler deck, p1 · npc 10707; dropped stale `targetPlane`).
- **Verification:** per source `validate_drop_rates` + `guidance_lint`; CI `build`+`scripts`
  green; two relaunch + live re-sweep cycles (5/5 then 3/3 PASS); one source per PR; merged once
  CI-green and live-verified (operator-approved merge policy).
- The 9th divergence (Trouble Brewing) was a transient drive-state artifact off the pre-edit
  client; not reproduced on a clean relaunch — no data change, no spectator fix needed.

## 2026-06-30 — Accuracy-convergence v2, Phase-2 semantic pass: BOSSES batch 1 (15 sources)

Cache `2026-06-25-rev239` (no drift). Ran the three-tier loop:

- **Phase 0 (deterministic) — corpus DRAINED.** `run_corpus_detectors` + `validate_drop_rates(all)`
  + `guidance_lint` over all 226: 6 detector classes queue-empty (fairy-ring, salve-non-undead,
  wilderness-level, lodestone, sailors-amulet, loop-structure). Everything else is by-design
  (multi-source duplicate itemIds; loop markers) or corpus-wide advisory heuristics (#49 npc-id,
  #50 area-confirmability, over-highlight→tracked engine F4 work, phantom-mechanic on descriptive
  prose). **No new actionable mechanical defects.** Two cosmetic cache name-nits noted only
  (Shades "lock"→"locks"; Hard-TT glory "(t)"→"(t4)") — display strings, correct ids.
- **Phase 1 (mechanical sweep):** last corpus sweep 2026-06-29 = 225 PASS / 0 DIVERGENCE
  (mechanically green). Operator-gated to re-run; not re-run this batch.
- **Phase 2 (semantic/meta):** accuracy-verifier Workflow (`pipeline(verify → domain-skeptic gate)`,
  cite-or-discard, live OSRS wiki) over 15 BOSSES. **12 CLEAN, 3 with skeptic-survived high
  findings** — all independently re-confirmed by direct WebFetch of the wiki before editing:
  - **Araxxor — PR #1080 (high):** requirements omitted the 92 Slayer gate AND the on-task lock
    (spider/araxyte task; Konar may assign). Receipt: "requiring level 92 Slayer and access to
    Morytania to kill"; "may only be killed while on a spider or araxyte Slayer task". Fix:
    `skills SLAYER 92` + prep-step prose.
  - **Bryophyta — PR #1081 (high):** Growthlings can ONLY be killed with a woodcutting axe or
    magic secateurs (one-shot); gear listed only a whip, so the kill was impossible as written.
    Receipt: "they must be attacked with a woodcutting axe or magic secateurs, one-shotting the
    growthlings"; "battleaxes or the zombie axe, cannot be used". Fix: prep + combat prose.
  - **Callisto — PR #1082 (high):** prayer guidance INVERTED — stood on Protect from Magic and
    disparaged Protect from Melee, but his AoE melee (~50% reduced by Protect from Melee) is the
    primary threat; Magic is only for the white-orb knockback. Receipt: "using Protect from Melee
    reduces the damage by ~50%". Fix: arrival + combat prose.

**Stamps:** V1 = `2026-06-30` for the 12 clean sources (Abyssal Sire, Alchemical Hydra, Amoxliatl,
Barrows, Brutus, Cerberus, Chaos Elemental, Chaos Fanatic, Commander Zilyana, Corporeal Beast,
Corrupted Gauntlet, Crazy archaeologist). The 3 fixed sources stay V1 `-` until their PRs merge
+ re-verify. V2 (in-game) stays `-` corpus-wide (operator-gated). All 3 fix PRs are prose/
requirement-only (no itemId/dropRate/coord/`loopBackToStep`/`loopCount`), one source per PR,
`automerge skipping` (data PRs are operator-merged), CI `build`+`scripts` the authoritative gate.

**Next batch:** continue Phase 2 over the remaining BOSSES (Dagannoth trio → Zulrah), then SLAYER,
then RAIDS, per the traffic-priority order.

## 2026-06-30 — Accuracy-convergence v2, Phase-2 semantic pass: BOSSES batch 2 (15 sources)

Cache `2026-06-25-rev239`. Batch: Dagannoth Prime/Rex/Supreme, Deranged Archaeologist, Doom of
Mokhaiotl, Duke Sucellus (+Awakened), General Graardor, Giant Mole, Grotesque Guardians, Hespori,
K'ril Tsutsaroth, Kalphite Queen, King Black Dragon, Kraken. Verifier returned **12 CLEAN, 3 with
skeptic-survived findings**; cross-source consistency found a **4th** (Dagannoth Rex) the verifier
missed. All independently re-confirmed by direct WebFetch before editing.

- **Dagannoth Prime — PR #1084 (high):** `travelTip`/step-1 said "Lunar Isle tele → Rellekka". The
  Lunar Isle Teleport lands on Lunar Isle, not Rellekka, and is not the Kings' route; the Lunar
  option is **Waterbirth Teleport → Waterbirth Island** (already correct on Dagannoth Supreme).
  Receipt: "Teleports you to Waterbirth Island" (Lunar spellbook).
- **Dagannoth Rex — PR #1085 (high):** identical travel defect, **caught by cross-source consistency**
  (verifier returned Rex CLEAN). Same fix. Surfaced because Prime/Rex/Supreme share the lair and
  Supreme already had the correct form — a verifier blind spot worth noting for future batches.
- **Doom of Mokhaiotl — PR #1086 (high ×2):** (a) gear inverted — entry said melee/"Scythe is BiS";
  the fight is **ranged-primary** (twisted bow BiS given Magic 275; scorching bow budget), melee
  only as a punish switch. recItems scythe+soulreaper → twisted-bow(20997)+crystal-halberd(23987),
  `cache_ids` verified. (b) prayers are **projectile-colour** based (blue=mage, green=ranged,
  red=melee), not phase-based; old text omitted ranged. Receipts: "twisted bow is the best weapon…
  given Doom's Magic level of 275"; "a single projectile, with its colour… blue for magic, green
  for ranged, and red for melee".
- **Kalphite Queen — PR #1087 (high + low):** phase-2 "stay outside melee range to avoid her stomp"
  — KQ has **no stomp/AoE** (only stab/range/magic); correct is **walk under her** as phase 2
  begins. Also dropped the unnecessary forced Missiles→Magic prayer switch (she uses both styles in
  both forms; either overhead, Magic slightly preferred). Receipts: "Players should walk under her
  as soon as this phase begins…"; "it generally does not matter whether you pray Protect from Magic
  or Protect from Missiles".

**Straggler noted (out of batch):** **Basilisk Knight** (SLAYER) carries the same impossible
"Lunar Isle tele → Rellekka" travelTip, but its correct route differs (Neitiznot / Jormungand's
Prison, not Waterbirth). Left for the SLAYER batch to verify+fix per-source — not patched blind.

**Stamps:** V1 = `2026-06-30` for the 3 now-merged batch-1 fixes (Araxxor, Bryophyta, Callisto) +
the 11 batch-2 clean sources (clean list excludes Dagannoth Rex, now a fixed source). The 4 batch-2
fixes (PRs #1084–#1087) stay V1 `-` until merged + re-verified. V2 (in-game) corpus-wide `-`
(operator-gated). Fix PRs await operator merge (data PRs are operator-merged; the auto-mode
classifier correctly blocked self-merge of this batch).

**Running total V1-stamped:** 26 / 226 (12 batch-1 clean + 3 batch-1 fixes-merged + 11 batch-2
clean; batch-2's 4 fixes pending merge).