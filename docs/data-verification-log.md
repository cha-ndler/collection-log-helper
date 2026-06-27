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
| 1 | Abyssal Sire | BOSSES | - | - | - |
| 2 | Alchemical Hydra | BOSSES | - | - | - |
| 3 | Amoxliatl | BOSSES | - | - | - |
| 4 | Araxxor | BOSSES | - | - | - |
| 5 | Barrows | BOSSES | - | - | - |
| 6 | Brutus | BOSSES | - | - | - |
| 7 | Bryophyta | BOSSES | - | - | - |
| 8 | Callisto | BOSSES | - | - | - |
| 9 | Cerberus | BOSSES | - | - | - |
| 10 | Chaos Elemental | BOSSES | - | - | - |
| 11 | Chaos Fanatic | BOSSES | - | - | - |
| 12 | Commander Zilyana | BOSSES | - | - | - |
| 13 | Corporeal Beast | BOSSES | - | - | - |
| 14 | Corrupted Gauntlet | BOSSES | - | - | - |
| 15 | Crazy archaeologist | BOSSES | - | - | - |
| 16 | Dagannoth Prime | BOSSES | - | - | - |
| 17 | Dagannoth Rex | BOSSES | - | - | - |
| 18 | Dagannoth Supreme | BOSSES | - | - | - |
| 19 | Deranged Archaeologist | BOSSES | - | - | - |
| 20 | Doom of Mokhaiotl | BOSSES | - | - | - |
| 21 | Duke Sucellus | BOSSES | - | - | - |
| 22 | Duke Sucellus (Awakened) | BOSSES | - | - | - |
| 23 | General Graardor | BOSSES | - | - | - |
| 24 | Giant Mole | BOSSES | - | - | - |
| 25 | Grotesque Guardians | BOSSES | - | - | - |
| 26 | Hespori | BOSSES | - | - | - |
| 27 | K'ril Tsutsaroth | BOSSES | - | - | - |
| 28 | Kalphite Queen | BOSSES | - | - | - |
| 29 | King Black Dragon | BOSSES | - | - | - |
| 30 | Kraken | BOSSES | - | - | - |
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
