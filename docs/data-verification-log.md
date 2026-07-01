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
| 16 | Dagannoth Prime | BOSSES | `2026-06-30` | - | - |
| 17 | Dagannoth Rex | BOSSES | `2026-06-30` | - | - |
| 18 | Dagannoth Supreme | BOSSES | `2026-06-30` | - | - |
| 19 | Deranged Archaeologist | BOSSES | `2026-06-30` | - | - |
| 20 | Doom of Mokhaiotl | BOSSES | `2026-06-30` | - | - |
| 21 | Duke Sucellus | BOSSES | `2026-06-30` | - | - |
| 22 | Duke Sucellus (Awakened) | BOSSES | `2026-06-30` | - | - |
| 23 | General Graardor | BOSSES | `2026-06-30` | - | - |
| 24 | Giant Mole | BOSSES | `2026-06-30` | - | - |
| 25 | Grotesque Guardians | BOSSES | `2026-06-30` | - | - |
| 26 | Hespori | BOSSES | `2026-06-30` | - | - |
| 27 | K'ril Tsutsaroth | BOSSES | `2026-06-30` | - | - |
| 28 | Kalphite Queen | BOSSES | `2026-06-30` | - | - |
| 29 | King Black Dragon | BOSSES | `2026-06-30` | - | - |
| 30 | Kraken | BOSSES | `2026-06-30` | - | - |
| 31 | Kree'arra | BOSSES | `2026-06-30` | - | - |
| 32 | Nex | BOSSES | `2026-06-30` | - | - |
| 33 | Obor | BOSSES | `2026-06-30` | - | - |
| 34 | Perilous Moons | BOSSES | `2026-06-30` | - | - |
| 35 | Phantom Muspah | BOSSES | `2026-06-30` | - | - |
| 36 | Phosani's Nightmare | BOSSES | `2026-06-30` | - | - |
| 37 | Royal Titans | BOSSES | `2026-06-30` | - | - |
| 38 | Sarachnis | BOSSES | `2026-06-30` | - | - |
| 39 | Scorpia | BOSSES | `2026-06-30` | - | - |
| 40 | Scurrius | BOSSES | `2026-06-30` | - | - |
| 41 | Shellbane Gryphon | BOSSES | `2026-06-30` | - | - |
| 42 | Skotizo | BOSSES | `2026-06-30` | - | - |
| 43 | Sol Heredit | BOSSES | `2026-06-30` | - | - |
| 44 | The Fight Caves | BOSSES | `2026-06-30` | - | - |
| 45 | The Gauntlet | BOSSES | `2026-06-30` | - | - |
| 46 | The Hueycoatl | BOSSES | `2026-06-30` | - | - |
| 47 | The Inferno | BOSSES | `2026-06-30` | - | - |
| 48 | The Leviathan | BOSSES | `2026-06-30` | - | - |
| 49 | The Leviathan (Awakened) | BOSSES | `2026-06-30` | - | - |
| 50 | The Nightmare | BOSSES | `2026-06-30` | - | - |
| 51 | The Whisperer | BOSSES | `2026-06-30` | - | - |
| 52 | The Whisperer (Awakened) | BOSSES | `2026-06-30` | - | - |
| 53 | Thermonuclear smoke devil | BOSSES | `2026-06-30` | - | - |
| 54 | Vardorvis | BOSSES | `2026-06-30` | - | - |
| 55 | Vardorvis (Awakened) | BOSSES | `2026-06-30` | - | - |
| 56 | Venenatis | BOSSES | `2026-06-30` | - | - |
| 57 | Vet'ion | BOSSES | `2026-06-30` | - | - |
| 58 | Vorkath | BOSSES | `2026-06-30` | - | - |
| 59 | Yama | BOSSES | `2026-06-30` | - | - |
| 60 | Zalcano | BOSSES | `2026-06-30` | - | - |
| 61 | Zulrah | BOSSES | `2026-06-30` | - | - |
| 62 | Aberrant Spectre | SLAYER | `2026-06-30` | - | - |
| 63 | Abyssal Demon | SLAYER | `2026-06-30` | - | - |
| 64 | Aquanite | SLAYER | `2026-06-30` | - | - |
| 65 | Araxyte | SLAYER | `2026-06-30` | - | - |
| 66 | Basilisk | SLAYER | `2026-06-30` | - | - |
| 67 | Basilisk Knight | SLAYER | `2026-06-30` | - | - |
| 68 | Bloodveld | SLAYER | `2026-06-30` | - | - |
| 69 | Brine Rat | SLAYER | `2026-06-30` | - | - |
| 70 | Cave Crawler | SLAYER | `2026-06-30` | - | - |
| 71 | Cave Horror | SLAYER | `2026-06-30` | - | - |
| 72 | Cave Kraken | SLAYER | `2026-06-30` | - | - |
| 73 | Cockatrice | SLAYER | `2026-06-30` | - | - |
| 74 | Crawling Hand | SLAYER | `2026-06-30` | - | - |
| 75 | Custodian Stalker | SLAYER | `2026-06-30` | - | - |
| 76 | Dark Beast | SLAYER | `2026-06-30` | - | - |
| 77 | Demonic gorillas | SLAYER | `2026-06-30` | - | - |
| 78 | Drake | SLAYER | `2026-06-30` | - | - |
| 79 | Dust Devil | SLAYER | `2026-06-30` | - | - |
| 80 | Earthen Nagua | SLAYER | `2026-06-30` | - | - |
| 81 | Fossil Island Wyvern | SLAYER | `2026-06-30` | - | - |
| 82 | Frost Nagua | SLAYER | `2026-06-30` | - | - |
| 83 | Gargoyle | SLAYER | `2026-06-30` | - | - |
| 84 | Gryphon | SLAYER | `2026-06-30` | - | - |
| 85 | Hydra | SLAYER | `2026-06-30` | - | - |
| 86 | Infernal Mage | SLAYER | `2026-06-30` | - | - |
| 87 | Jelly | SLAYER | `2026-06-30` | - | - |
| 88 | Kurask | SLAYER | `2026-06-30` | - | - |
| 89 | Lava Strykewyrm | SLAYER | `2026-06-30` | - | - |
| 90 | Lizardman shaman | SLAYER | `2026-06-30` | - | - |
| 91 | Mogre | SLAYER | `2026-06-30` | - | - |
| 92 | Nechryael | SLAYER | `2026-06-30` | - | - |
| 93 | Pyrefiend | SLAYER | `2026-06-30` | - | - |
| 94 | Rockslug | SLAYER | `2026-06-30` | - | - |
| 95 | Skeletal Wyvern | SLAYER | `2026-06-30` | - | - |
| 96 | Smoke Devil | SLAYER | `2026-06-30` | - | - |
| 97 | Spiritual Mage | SLAYER | `2026-06-30` | - | - |
| 98 | Spiritual Mage (Zarosian) | SLAYER | `2026-06-30` | - | - |
| 99 | Sulphur Nagua | SLAYER | `2026-06-30` | - | - |
| 100 | Superior Slayer Monster | SLAYER | `2026-06-30` | - | - |
| 101 | Terror Dog | SLAYER | `2026-06-30` | - | - |
| 102 | Tormented Demons | SLAYER | `2026-06-30` | - | - |
| 103 | Turoth | SLAYER | `2026-06-30` | - | - |
| 104 | Vyrewatch Sentinel | SLAYER | `2026-06-30` | - | - |
| 105 | Warped Creature | SLAYER | `2026-06-30` | - | - |
| 106 | Wyrm | SLAYER | `2026-06-30` | - | - |
| 107 | Chambers of Xeric | RAIDS | `2026-06-30` | - | - |
| 108 | Chambers of Xeric (Challenge Mode) | RAIDS | `2026-06-30` | - | - |
| 109 | Theatre of Blood | RAIDS | `2026-06-30` | - | - |
| 110 | Theatre of Blood (Hard Mode) | RAIDS | `2026-06-30` | - | - |
| 111 | Tombs of Amascut | RAIDS | `2026-06-30` | - | - |
| 112 | Tombs of Amascut (300 Invocation) | RAIDS | `2026-06-30` | - | - |
| 113 | Tombs of Amascut (500 Invocation) | RAIDS | `2026-06-30` | - | - |
| 114 | Barbarian Assault | MINIGAMES | `2026-06-30` | - | - |
| 115 | Barracuda Trials | MINIGAMES | `2026-06-30` | - | - |
| 116 | Brimhaven Agility Arena | MINIGAMES | - | - | #1142 prose; structural pointCost deferred |
| 117 | Castle Wars | MINIGAMES | `2026-06-30` | - | - |
| 118 | Fishing Trawler | MINIGAMES | `2026-06-30` | - | - |
| 119 | Giants' Foundry | MINIGAMES | `2026-06-30` | - | - |
| 120 | Gnome Restaurant (Scarfs) | MINIGAMES | `2026-06-30` | - | - |
| 121 | Gnome Restaurant (Seed Pods) | MINIGAMES | `2026-06-30` | - | - |
| 122 | Guardians of the Rift | MINIGAMES | `2026-06-30` | - | - |
| 123 | Hallowed Sepulchre | MINIGAMES | `2026-06-30` | - | - |
| 124 | Last Man Standing | MINIGAMES | `2026-06-30` | - | - |
| 125 | Mage Training Arena | MINIGAMES | `2026-06-30` | - | - |
| 126 | Mahogany Homes | MINIGAMES | - | - | coord deferred (unverifiable tile) |
| 127 | Mastering Mixology | MINIGAMES | `2026-06-30` | - | - |
| 128 | Pest Control | MINIGAMES | `2026-06-30` | - | - |
| 129 | Rogues' Den | MINIGAMES | `2026-06-30` | - | - |
| 130 | Shades of Mort'ton | MINIGAMES | `2026-06-30` | - | - |
| 131 | Soul Wars | MINIGAMES | `2026-06-30` | - | - |
| 132 | Temple Trekking | MINIGAMES | `2026-06-30` | - | - |
| 133 | Tempoross | MINIGAMES | `2026-06-30` | - | - |
| 134 | Tithe Farm | MINIGAMES | `2026-06-30` | - | - |
| 135 | Trouble Brewing | MINIGAMES | `2026-06-30` | - | - |
| 136 | Vale Totems | MINIGAMES | `2026-06-30` | - | - |
| 137 | Volcanic Mine | MINIGAMES | `2026-06-30` | - | - |
| 138 | Wintertodt | MINIGAMES | `2026-06-30` | - | - |
| 139 | Adamant Dragon | OTHER | `2026-06-30` | - | - |
| 140 | Albatross (Boat Combat) | OTHER | `2026-06-30` | - | - |
| 141 | Armoured Zombie | OTHER | `2026-06-30` | - | - |
| 142 | Barracuda Salvage | OTHER | `2026-06-30` | - | - |
| 143 | Boat Paints | OTHER | `2026-06-30` | - | - |
| 144 | Brimstone Chest | OTHER | `2026-06-30` | - | - |
| 145 | Camdozaal | OTHER | `2026-06-30` | - | - |
| 146 | Catacombs of Kourend | OTHER | `2026-06-30` | - | - |
| 147 | Champion's Challenge | OTHER | `2026-06-30` | - | - |
| 148 | Chompy Bird Hunting | OTHER | `2026-06-30` | - | - |
| 149 | Creature Creation (Frogeel) | OTHER | `2026-06-30` | - | - |
| 150 | Creature Creation (Jubster) | OTHER | `2026-06-30` | - | - |
| 151 | Creature Creation (Newtroost) | OTHER | `2026-06-30` | - | - |
| 152 | Creature Creation (Spidine) | OTHER | `2026-06-30` | - | - |
| 153 | Creature Creation (Swordchick) | OTHER | `2026-06-30` | - | - |
| 154 | Creature Creation (Unicow) | OTHER | `2026-06-30` | - | - |
| 155 | Cyclopes | OTHER | `2026-06-30` | - | - |
| 156 | Elder Chaos Druids | OTHER | `2026-06-30` | - | - |
| 157 | Elven Crystal Chest | OTHER | `2026-06-30` | - | - |
| 158 | Fishy Salvage | OTHER | `2026-06-30` | - | - |
| 159 | Forestry | OTHER | `2026-06-30` | - | - |
| 160 | Fossil Island Notes | OTHER | `2026-06-30` | - | - |
| 161 | Fountain of Rune | OTHER | `2026-06-30` | - | - |
| 162 | Fremennik Salvage | OTHER | `2026-06-30` | - | - |
| 163 | Glough's Experiments | OTHER | `2026-06-30` | - | - |
| 164 | Great White Shark (Boat Combat) | OTHER | `2026-06-30` | - | - |
| 165 | Hunter Guild | OTHER | `2026-06-30` | - | - |
| 166 | Large Salvage | OTHER | `2026-06-30` | - | - |
| 167 | Larran's Big Chest | OTHER | `2026-06-30` | - | - |
| 168 | Lost Schematics | OTHER | - | - | deferred (Piscarilius anchor test-guarded) |
| 169 | Martial Salvage | OTHER | `2026-07-01` | - | - |
| 170 | Miscellaneous | OTHER | `2026-07-01` | - | - |
| 171 | Mithril Dragon | OTHER | `2026-07-01` | - | - |
| 172 | Monkey Backpacks | OTHER | `2026-07-01` | - | - |
| 173 | My Notes | OTHER | `2026-07-01` | - | - |
| 174 | Narwhal (Boat Combat) | OTHER | `2026-07-01` | - | - |
| 175 | Ocean Encounters | OTHER | `2026-07-01` | - | - |
| 176 | Ogress Shaman | OTHER | `2026-07-01` | - | - |
| 177 | Opulent Salvage | OTHER | `2026-07-01` | - | - |
| 178 | Orcas (Boat Combat) | OTHER | `2026-07-01` | - | - |
| 179 | Plundered Salvage | OTHER | `2026-07-01` | - | - |
| 180 | Port Tasks | OTHER | `2026-07-01` | - | - |
| 181 | Prifddinas Elf | OTHER | `2026-07-01` | - | - |
| 182 | Pyramid Plunder | OTHER | `2026-07-01` | - | - |
| 183 | Random Events | OTHER | `2026-07-01` | - | - |
| 184 | Revenants | OTHER | `2026-07-01` | - | - |
| 185 | Rune Dragon | OTHER | `2026-07-01` | - | - |
| 186 | Sailing Misc | OTHER | `2026-07-01` | - | - |
| 187 | Sea Treasures | OTHER | - | - | deferred (fragment re-model, test-guarded) |
| 188 | Shayzien Armour | OTHER | `2026-07-01` | - | - |
| 189 | Small Salvage | OTHER | `2026-07-01` | - | - |
| 190 | Stingray (Boat Combat) | OTHER | `2026-07-01` | - | - |
| 191 | Stronghold of Security | OTHER | `2026-07-01` | - | - |
| 192 | TzHaar | OTHER | - | - | deferred (aggregate multi-NPC rate model) |
| 193 | Vampyre Kraken (Boat Combat) | OTHER | `2026-07-01` | - | - |
| 194 | Waterfiend | OTHER | `2026-07-01` | - | - |
| 195 | Wilderness God Wars Dungeon | OTHER | `2026-07-01` | - | - |
| 196 | Zombie Pirate Locker | OTHER | `2026-07-01` | - | - |
| 197 | Aerial Fishing | SKILLING | `2026-07-01` | - | - |
| 198 | Black Chinchompas | SKILLING | `2026-07-01` | - | - |
| 199 | Colossal Wyrm Agility | SKILLING | `2026-07-01` | - | - |
| 200 | Cutting Squid | SKILLING | `2026-07-01` | - | - |
| 201 | Deep Sea Fishing | SKILLING | - | - | 1 (PR #1161) |
| 202 | Farming (Fruit Trees) | SKILLING | - | - | 1 (PR #1164) |
| 203 | Fishing (Swordfish) | SKILLING | `2026-07-01` | - | - |
| 204 | Mining (Gemstone Rocks) | SKILLING | `2026-07-01` | - | - |
| 205 | Motherlode Mine | SKILLING | `2026-07-01` | - | - |
| 206 | Pickpocketing Darkmeyer Vyre | SKILLING | `2026-07-01` | - | - |
| 207 | Prifddinas Rabbit | SKILLING | `2026-07-01` | - | - |
| 208 | Rooftop Agility | SKILLING | - | - | 1 (PR #1162) |
| 209 | Runecrafting (Fire Runes) | SKILLING | - | - | 1 (PR #1163) |
| 210 | Shooting Stars | SKILLING | `2026-07-01` | - | - |
| 211 | Thieving (Seed Stalls) | SKILLING | `2026-07-01` | - | - |
| 212 | Underwater Crabs | SKILLING | - | - | deferred (Mogre entry route wiki-contradicted; needs sourced coords) |
| 213 | Woodcutting (Teak Trees) | SKILLING | `2026-07-01` | - | - |
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

## 2026-06-30 — Accuracy-convergence v2, Phase-2 semantic pass: BOSSES batch 3 (15 sources)

Cache `2026-06-25-rev239`. Batch: Kree'arra, Nex, Obor, Perilous Moons, Phantom Muspah, Phosani's
Nightmare, Royal Titans, Sarachnis, Scorpia, Scurrius, Shellbane Gryphon, Skotizo, Sol Heredit,
The Fight Caves, The Gauntlet. Verifier returned **9 CLEAN, 6 sources with skeptic-survived
findings** (5 high + 1 low). One verifier "low" was DROPPED on review (Shellbane cave-direction
already correct). All findings independently re-confirmed by direct WebFetch before editing.

- **The Fight Caves — PR #1090 (high):** wave 1-62 prayers were wrong for 3 of 4 named monsters —
  Protect from Magic vs Tz-Kih (22) and Yt-MejKot (180) and Protect from Missiles vs Tz-Kek (45),
  but all three are MELEE; Ket-Zek (the actual magic attacker) was mislabelled a melee minion and
  omitted from the prayer list. Corrected to attack-style prayers (Melee: Tz-Kih/Tz-Kek/Yt-MejKot/
  Yt-HurKot; Missiles: Tok-Xil; Magic: Ket-Zek). Receipt: Fight Cave monster table.
- **Scurrius — PR #1091 (high):** "Protect from Missiles" → **Protect from Melee** (phases 1-2; Tail
  Swipe melee is fastest/most accurate; Missiles only blocks the minor Flying Fur). Receipt:
  Scurrius/Strategies.
- **Sarachnis — PR #1092 (high):** fabricated "Protect from Magic when red / Protect from Ranged
  when yellow" — she has no magic attack and no colour-glow tells (melee+ranged only). Replaced
  with distance-based prayers; removed the unverified "magic-using spawn". Receipt: Sarachnis page.
- **Scorpia — PR #1093 (high):** "run south-west" from the Lava Maze teleport → **north-east**; the
  Scorpion Pit is NE Wilderness while the teleport lands west/south. Receipt: "Cave in the Scorpion
  Pit (north-east Wilderness)".
- **Shellbane Gryphon — PR #1094 (high):** 3 steps recommended a **Quetzal whistle** to reach/return
  from The Great Conch — impossible (Quetzal is Varlamore-only; the Great Conch is a Sailing island
  reached by fairy ring CJQ / charter ship / Sailing). Receipt: Great Conch Transportation section.
  (The companion "low" cave-direction finding was dropped — data already says "north".)
- **Phantom Muspah — PR #1095 (low):** "Ancient Prison entrance" mislabel — the Muspah lair is a
  SOUTHERN crevice of Ghorrock Dungeon; the Ghorrock/Ancient Prison (Duke Sucellus) is the distinct
  NW entrance. "Run south" was already correct; only the landmark name was fixed. Receipt: Ghorrock
  Dungeon page.

**Process note — verifier blind spot:** as in batch 2 (Dagannoth Rex), the per-source verifier
returned a source CLEAN that shared a defect with a flagged sibling. None recurred here, but the
pattern stands: cross-source consistency catches what independent per-source verification misses.

**Stamps:** V1 = `2026-06-30` for the 4 now-merged batch-2 fixes (Dagannoth Prime/Rex, Doom,
Kalphite Queen) + the 9 batch-3 clean sources. The 6 batch-3 fixes (PRs #1090–#1095) stay V1 `-`
until merged + re-verified. V2 (in-game) corpus-wide `-` (operator-gated). Fix PRs await operator
merge (self-merge policy-blocked).

**Repo-hygiene PR #1089 (open):** untracks a 17 MB `backup.bundle` (255 refs) + 10 validation
screenshots accidentally swept into the tree by a `git add -A`; awaiting operator merge. History
purge of the bundle blob is flagged as a separate operator decision.

**Running total V1-stamped:** 39 / 226 (26 prior + 4 batch-2 fixes-merged + 9 batch-3 clean;
batch-3's 6 fixes pending merge → 45 once merged).

## 2026-06-30 — Accuracy-convergence v2, Phase-2 semantic pass: BOSSES batch 4 (16 sources) — BOSSES COMPLETE

Cache `2026-06-25-rev239`. Final BOSSES batch: The Hueycoatl, The Inferno, The Leviathan (+Awakened),
The Nightmare, The Whisperer (+Awakened), Thermonuclear smoke devil, Vardorvis (+Awakened), Venenatis,
Vet'ion, Vorkath, Yama, Zalcano, Zulrah. Verifier returned **7 CLEAN, 9 sources with skeptic-survived
findings** (12 findings: 1 blocker + 1 second blocker + 7 high + 3 low; one flagged drop rate deferred).
The verifier prompt was hardened this batch to inspect per-step `travelTip` fields (the gap that caused
the batch-3 Shellbane miss). All findings independently re-confirmed by direct WebFetch before editing.

- **The Inferno — PR #1098 (BLOCKER):** wave 1-66 prayers wrong for 4 of 5 monsters (prayed Magic vs
  the ranger Jal-Xil, Ranged vs the meleer Jal-Imkot, Ranged vs the mage Jal-AkRek-Mej, Melee vs the
  ranged bat Jal-MejRah). Corrected to attack-style prayers + fixed the Jal-Xil/blob and healer labels.
- **Vet'ion — PR #1099 (BLOCKER + high):** no access gate (needs medium Wilderness Diary OR Vet'ion
  boss task) — added `diaries: WILDERNESS_MEDIUM` + prose; and "Protect from Magic, magic only" was
  wrong (lightning is a dodgeable AoE; Protect from Melee for the melee hellhounds).
- **Venenatis — PR #1100 (high):** same missing Silk Chasm access gate; added diary requirement + prose.
- **Vorkath — PR #1101 (high):** "Lunar Isle teleport -> Rellekka" impossible (Lunar teleport -> Lunar
  Isle); replaced with Fremennik sea boots / Enchanted lyre / house portal. (Same class as Dagannoth.)
- **Zulrah — PR #1102 (high):** red/magma form is a typeless tail attack (move 2 tiles), NOT
  "Protect from Melee" — no prayer reduces it.
- **The Whisperer (Awakened) — PR #1103 (high):** fabricated "break four shadow tiles before her shield
  resets / 30s extension" replaced with the real Soul Siphon (kill a same-chant soul set; fail = 50 dmg
  + heal 100) — which the same step already referenced later.
- **Yama — PR #1104 (high + low):** "Protect from Magic primary" wrong (melee/ranged/magic, dynamic
  switch; melee pierces prayer); fabricated "demonic tallow slams / imps" → void flares + shadow waves.
- **The Whisperer — PR #1105 (low):** ranged barbs are grey, not purple.
- **The Hueycoatl — PR #1106 (low):** quetzal whistle billed as direct/fastest to the Darkfrost, but
  the Darkfrost is not a Quetzal landing site (nearest Quetzacalli Gorge); pendant of ates is the direct
  teleport.

**Deferred (logged, not patched):** **Yama — Oathplate shards drop rate.** Current `0.03333` (1/30) is
almost certainly too low, but my sources disagree on the correct value (wiki main page "1/15" vs the
verifier's "1/17.07"). Not guessed — needs a single precise drop-table source before editing. (Tier-2
rate work, per the loop's "leave rates to a precise source" rule.)

**Stamps:** V1 = `2026-06-30` for the 5 now-merged batch-3 fixes (Fight Caves, Scurrius, Sarachnis,
Scorpia, Phantom Muspah) + the 7 batch-4 clean sources. Shellbane Gryphon stays V1 `-` pending its
follow-up **PR #1097** (a per-step `travelTip` the batch-3 fix missed — still had the Quetzal-whistle +
"east" error). The 9 batch-4 fixes (PRs #1098–#1106) stay V1 `-` until merged + re-verified.

**BOSSES category status:** all 61 BOSSES verified. **51/61 V1-stamped**; the remaining 10 are pending
fix PRs (#1097 Shellbane + #1098–#1106 batch-4) → **61/61 once merged.** This closes the BOSSES leg of
the convergence loop; SLAYER is next (note the logged Basilisk Knight Lunar-teleport straggler).

**Aggregate across all 61 BOSSES:** 22 real current-meta defects found and fixed (≈36% of sources
carried at least one) — 2 blockers (Inferno/Vet'ion prayers+gate), travel impossibilities (Dagannoth
×2, Vorkath, Shellbane, Hueycoatl), inverted/wrong prayers (Callisto, Scurrius, Fight Caves, Inferno,
Vet'ion, Zulrah, Yama, Sarachnis), fabricated mechanics (Bryophyta, Sarachnis, Whisperer-Awakened,
Yama), and missing access gates (Araxxor, Venenatis, Vet'ion). Strong evidence the corpus-wide sweep
is worthwhile.

**Running total V1-stamped:** 51 / 226.

## 2026-06-30 — Accuracy-convergence v2: SLAYER batch 1 (16) + BOSSES category CLOSED + Tier-0 #96 backlog

Cache `2026-06-25-rev239`. BOSSES batch-4 fixes all merged → **BOSSES category COMPLETE: 61/61 V1-stamped.**

**SLAYER batch 1** (Aberrant Spectre → Demonic gorillas): verifier returned **12 CLEAN, 4 sources with
findings** (5 findings); cross-source consistency added a **5th** (Basilisk Knight). All re-confirmed by
direct WebFetch.
- **Crawling Hand — PR #1108 (high):** combat step said "Slayer Tower basement"; Crawling Hands are on
  the GROUND FLOOR (self-contradicted its own locationDescription). Receipt: "ground floor[UK]".
- **Custodian Stalker — PR #1109 (high ×2 + Tier-0):** travel "run south" → NORTH-WEST from fairy ring
  AIS (×3 fields); southern squeeze-through entrance needs 59 Agility (noted + northern BLS alternative);
  AND the Tier-0 junk gear id 392 (noted Manta ray) → 391. Receipt: "AIS to Auburn Valley, then run
  north-west"; "Southern entrance requiring 59 Agility".
- **Cave Kraken — PR #1110 (low):** missing 50 Magic assignment requirement (had SLAYER 87 only).
  Receipt: "require level 87 Slayer and 50 Magic".
- **Cave Horror — PR #1111 (low):** witchwood icon does NOT convert the scream to a regular roll - it
  makes the horror alternate kick/scream (partial); special always 10% base HP, fully negated only by
  Protect from Melee.
- **Basilisk Knight — PR #1112 (high, cross-source catch):** "Lunar Isle tele -> Rellekka" impossible
  (Lunar teleport -> Lunar Isle). Island of Stone is reached by Haskell's boat from Rellekka. Verifier
  returned it CLEAN — the **third** Lunar-teleport miss (after Dagannoth Rex), confirming the per-source
  verifier blind spot for this travel class.

**NEW Tier-0 backlog — item-id-plausibility (detector #96, enabled by the 2026-06-30 toolkit update).**
The updated `run_corpus_detectors` now flags junk NOTED-item ids (cache name "null", `noteTemplate` 799)
used in required/recommended loadouts that `cache_ids` passes. 10 flagged; **1 fixed** (Custodian Stalker
392→391 in #1109). **9 remaining**, all on OTHER/already-stamped sources — a distinct deterministic sweep:
- **Burning amulet** (junk 21167 = noted Burning amulet (5)) → **21166** on 5 Wilderness sources:
  Chaos Elemental, Scorpia, Chaos Fanatic, Crazy archaeologist, Revenants. CLEAR fix (it's the required
  wildy travel item the guidance already names). Systematic copy-paste error.
- **Magic logs** (junk 1514 → 1513) on Revenants recommendedItemIds ×2 — nonsense as gear; wrong id,
  intended item unknown (likely should be removed or corrected). NEEDS intent.
- **Runite bar** (junk 2364 → 2363) on Gryphon recommendedItemIds — nonsense as gear; NEEDS intent.
- **23713** (noted; unnoted 23712 has no RuneLite constant) on Deranged Archaeologist requiredItemIds —
  NEEDS cache resolution + intent.
  *Held for operator decision (one-PR-per-source vs bundled; and intent investigation for the 3 unclear).*

**Still deferred:** Yama Oathplate shards drop rate (1/15 vs 1/17.07 unresolved — needs one precise
source). Repo-hygiene: the 17 MB bundle blob still in history (filter-repo purge is an operator call).

**Stamps:** V1 = `2026-06-30` for the 10 now-merged BOSSES batch-4 fixes + the 11 SLAYER batch-1 clean
sources. The 5 SLAYER fixes (PRs #1108–#1112) stay V1 `-` until merged + re-verified.

**Running total V1-stamped:** 72 / 226 (61 BOSSES complete + 11 SLAYER clean; SLAYER's 5 fixes pending
merge → 77). SLAYER progress: 16/106 verified.

## 2026-06-30 — Accuracy-convergence v2: SLAYER batch 2 (Drake → Rockslug, 16)

Cache `2026-06-25-rev239`. SLAYER batch-1 fixes all merged. Batch 2 verifier returned **15 CLEAN, 1
finding** — the cleanest batch yet (the Slayer mid-tier is well-maintained).
- **Earthen Nagua — PR #1114 (high):** missing access gate — the Tonali Cavern is only reachable after
  The Final Dawn quest. Added `requirements.quests THE_FINAL_DAWN` (verified present in the RuneLite
  `Quest` enum the project builds against) + a travel-step prose note. Receipt: "found in the Tonali
  Cavern after completion of the The Final Dawn quest".

**Gryphon — semantic CLEAN but held from V1 (open Tier-0 #96):** Gryphon's recommendedItemIds carries
junk id 2364 (noted Runite bar, null cache name). A source isn't V1-validated until it clears all tiers,
so Gryphon stays V1 `-` with an open Tier-0 finding until the item-id sweep resolves it (the unnoted
2363 = Runite bar is nonsense as gear, so it needs intent — likely removal or a corrected food/gear id).

**Stamps:** V1 = `2026-06-30` for the 5 now-merged SLAYER batch-1 fixes + the 14 batch-2 clean sources
(excludes Gryphon [open Tier-0] and Earthen Nagua [fixed, pending merge]).

**Running total V1-stamped:** 91 / 226 (61 BOSSES + 16 SLAYER b1 + 14 SLAYER b2-clean). SLAYER progress:
32/106 verified (Earthen Nagua → 92 once #1114 merges; Gryphon pending its Tier-0 id).

**Open Tier-0 #96 backlog (unchanged, awaiting operator):** burning amulet 21167→21166 (×5 Wilderness,
clear); magic logs 1514→1513 (Revenants ×2), runite bar 2364→2363 (Gryphon), 23713 (Deranged
Archaeologist) — the latter 3 need intent. Yama Oathplate-shards rate + bundle-blob history purge also
still pending operator calls.

## 2026-06-30 — Tier-0 item-id-plausibility (#96) sweep — DRAINED (operator-approved)

All 10 junk noted-item ids flagged by detector #96 fixed (7 PRs, one source each). After fixing the
intended-item ones and removing the unrecoverable ones, the intent split resolved cleanly:
- **Burning amulet 21167 → 21166** (`BURNING_AMULET_5`), the wilderness travel item the guidance names:
  **Chaos Elemental #1116, Scorpia #1117, Chaos Fanatic #1118, Crazy archaeologist #1119, Revenants
  #1120** (a single systematic copy-paste of the NOTED amulet id across the 5 Wilderness sources).
- **Removed** (junk id, unnoted form equally nonsensical as gear, intent unrecoverable — removed rather
  than guessed): **Revenants** magic logs 1514 ×2 (in #1120), **Gryphon** runite bar 2364 (#1121),
  **Deranged Archaeologist** noted id 23713 (#1122; not a hard requirement — Fossil Island is boat-
  reachable, the Digsite pendant is travel convenience).
- Earlier in-batch fix: **Custodian Stalker** 392 → 391 (Manta ray), folded into #1109.

Each fix is deterministic (detector #96 + `runelite_id_lookup` for the resolvable ids) and passes
`validate_drop_rates --check cache_ids`. **Re-run `run_corpus_detectors` after these merge to confirm the
#96 queue is empty.** Note: the 5 Wilderness sources + Deranged Archaeologist are already V1-stamped
BOSSES (the Tier-0 fix is a residual id correction, not a re-verification — stamps unchanged). **Gryphon**
becomes V1-stampable once #1121 merges (this was its sole open finding). **Revenants** stays unstamped —
it is an OTHER-category source (#184) not yet semantically verified; the Tier-0 fix clears only one tier.

## 2026-06-30 — Accuracy-convergence v2: SLAYER batch 3 (final 13) — SLAYER category COMPLETE

Cache `2026-06-25-rev239`. Tier-0 #96 sweep merged + confirmed drained (0 findings). Batch 3 (the last 13,
incl. **Lava Strykewyrm** which had been skipped in the batch-2 range): verifier returned **10 CLEAN, 3
findings**. All re-confirmed by direct WebFetch.
- **Sulphur Nagua — PR #1124 (high):** missing access gate — Neypotzli (beneath Cam Torum) needs partial
  completion of Perilous Moons. Added `requirements.quests PERILOUS_MOONS` (the checker treats an
  in-progress quest as access, correctly modelling "partial"). Receipt: "Partial completion of Perilous
  Moons is required for access."
- **Vyrewatch Sentinel — PR #1125 (high):** loot step said Blood shard "1/5000" — that's the Darkmeyer
  Vyre PICKPOCKETING rate; the combat drop is 1/1500 (the source's own dropRate field 0.000667 was
  already correct; only the prose was wrong, overstating the grind ~3.3x). Receipt: "Vyrewatch Sentinel
  (combat): 1/1,500".
- **Warped Creature — PR #1126 (low):** "Requires completion of The Path of Glouphrie" → PARTIAL
  completion (the structured quest requirement was already partial-aware; prose-only). Receipt:
  "requiring partial completion of The Path of Glouphrie".

**Note on counts:** SLAYER is **45 sources** (rows 62-106), not 106 (106 is the last row number). Earlier
"X/106" SLAYER-progress notes meant row index, not count.

**Stamps:** V1 = `2026-06-30` for: Earthen Nagua (#1114 merged), Gryphon (#1121 merged, #96 cleared), and
the 10 batch-3 clean sources. The 3 batch-3 fixes (#1124-#1126) stay V1 `-` until merged.

**SLAYER category status:** all 45 verified. **42/45 V1-stamped**; 45/45 once #1124-#1126 merge.
**BOSSES + SLAYER both complete** = 106 of 225 sources (≈47%).

**Aggregate across 45 SLAYER sources:** 9 real defects fixed — missing access gates (Cave Kraken 50
Magic, Earthen Nagua + Sulphur Nagua quest gates), travel (Custodian Stalker direction+agility, Basilisk
Knight Lunar-teleport), prose/mechanic (Crawling Hand floor, Cave Horror witchwood, Vyrewatch rate-text,
Warped partial-quest) + 1 Tier-0 id (Custodian Stalker / Gryphon). Much cleaner than BOSSES (≈20% vs 36%).

**Running total V1-stamped:** 103 / 225 (61 BOSSES + 42 SLAYER). Next category: RAIDS (7 sources, CoX →
ToA), then MINIGAMES.

## 2026-06-30 — Accuracy-convergence v2: RAIDS (7) — RAIDS category COMPLETE

Cache `2026-06-25-rev239`. SLAYER batch-3 fixes all merged → SLAYER complete (45/45). RAIDS verifier
returned **6 CLEAN, 1 finding** — the raids are very well-maintained (the points/completion/invocation
reward model is correctly represented; access/travel/mechanics all checked out).
- **Tombs of Amascut (500 Invocation) — PR #1128 (low):** the Masori crafting kit (27372) and Menaphite
  ornament kit (27255) were modeled as 1/50 RNG, but they are deterministic CHALLENGE rewards (raid level
  350+ / 425+, deathless — "Always" rarity). At 500 invocation both thresholds are met, so both are
  guaranteed on a first deathless clear. Changed to `dropRate 1.0 + milestoneKills 1`, matching the
  sibling Cursed phalanx / Remnants in the same entry. Scoped to the 500 tier only (300/normal entries,
  where raid level can be below the thresholds, were not flagged). Receipts: "level 350+ ... 1500 points"
  / "level 425 (or higher) with zero deaths", both "Always" rarity.

**Stamps:** V1 = `2026-06-30` for the 3 now-merged SLAYER batch-3 fixes (Sulphur Nagua, Vyrewatch
Sentinel, Warped Creature) + the 6 RAIDS clean sources. ToA-500 stays V1 `-` until #1128 merges.

**RAIDS category status:** all 7 verified. **6/7 V1-stamped**; 7/7 once #1128 merges.
**BOSSES + SLAYER + RAIDS complete** = 113 of 225 sources (~50%) — the three combat-PvM categories done.

**Running total V1-stamped:** 112 / 225 (61 BOSSES + 45 SLAYER + 6 RAIDS). Next category: MINIGAMES (25
sources, Barbarian Assault → Wintertodt), then OTHER (58), SKILLING (17), CLUES (12).

## 2026-06-30 — Accuracy-convergence v2: MINIGAMES (25) — highest defect rate yet

Cache `2026-06-25-rev239`. RAIDS ToA-500 fix merged. MINIGAMES verifier returned **11 CLEAN, 14 sources
with findings (17 total)** — by far the highest hit rate (~56% of sources), because the prose-only
2026-06-28 wiki-meta sweep left many STRUCTURED point-shop / dropRate values wrong. **13 sources fixed
this turn; 2 deferred.** All findings re-confirmed by direct WebFetch.

**High / blocker fixes:**
- **Barbarian Assault #1134:** Granite body had fabricated pointCost 1000 — it's a 95,000-coin purchase
  (Penance Queen kill), the only BA reward not from honour points. Removed pointCost → dropRate 1.0 +
  milestoneKills 1 + prose.
- **Pest Control #1131:** Elite void top/robe pointCost 650 → 200 (the upgrade cost; requiresPrevious
  already gates the regular 250).
- **Soul Wars #1130:** Ectoplasmator pointCost 2500 → 250 Zeal Tokens.
- **Volcanic Mine #1133:** Dragon pickaxe (broken) was dropRate 1.0 + pointCost 4000 (guaranteed buy);
  it's a 1/100 roll from 4000-point ore packs → dropRate 0.01.
- **Giants' Foundry #1135:** no fixed hammer×2/grind/polish pattern — tool is dictated by the sword's
  TEMPERATURE band (hot=hammer, medium=grindstone, cold=polish).
- **Gnome Restaurant (Seed Pods) #1137:** Gianne jnr / range on the Grand Tree 1st floor [UK], not
  ground floor.
- **Mastering Mixology #1132:** resin colours — Mox is blue, Lye is red (were orange/blue).
- **Temple Trekking #1136 (blocker):** Lumberjack outfit is dropped by undead lumberjacks in the
  bridge-repair event (25% each), NOT bought from a fabricated "token trader"; drop is
  difficulty-independent.

**Low fixes:** Barracuda Trials #1141 (missing SAILING 30 gate + Soup rate 1/2252→1/3000), Fishing
Trawler #1138 (swamp-paste leak task), Hallowed Sepulchre #1139 (Strange old lockpick drops on all
floors, not just floor 5), Vale Totems #1140 (vale research points at the Vale Research Exchange, not
offerings direct), Brimhaven #1142 (reward currency: tickets→XP, items via vouchers, hook 800 vouchers).

**Deferred (2 — operator/verification needed):**
- **Brimhaven Agility Arena — graceful pointCost (structural):** 6 pieces × 250 = 1500 vs the real
  whole-set 250-voucher recolour (6× overstatement). Needs a plugin set-recolour modeling decision (a
  single purchase unlocks all 6). Prose already correct; #1142 fixed the currency. **Row stays open.**
- **Mahogany Homes — Amy coord:** the entry's ARRIVE tile (2954,3369) is likely wrong (finding says
  ~2990,3365, "south of the park"), but the wiki gives no exact tile, so I did not swap in an unverified
  coord. **Row stays open** — resolve via npc_spawns/coordinate_helper or in-game.

**Stamps:** V1 = `2026-06-30` for ToA-500 (merged) + the 11 MINIGAMES clean. The 12 fixed sources
(#1130-1141) stamp after merge. Brimhaven + Mahogany Homes stay open (deferred above).

**MINIGAMES status:** 11/25 stamped; 23/25 once the 12 fix PRs merge; Brimhaven + Mahogany the 2
residual open items.

**Running total V1-stamped:** 124 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 11 MINIGAMES). Categories
done: BOSSES, SLAYER, RAIDS (3/7). Next: OTHER (58 — the largest), SKILLING (17), CLUES (12).

## 2026-06-30 — Accuracy-convergence v2: OTHER batch 1 (15/58)

Cache `2026-06-25-rev239`. MINIGAMES fixes merged (Brimhaven + Mahogany still the 2 residual open items).
OTHER batch 1 (Adamant Dragon → Creature Creation Swordchick): **10 CLEAN, 5 with findings (7 total)**.
All re-confirmed by direct WebFetch / MCP id lookups.
- **Armoured Zombie — PR #1144 (BLOCKER + high):** source + both steps used coords 3566,3390 =
  **Darkmeyer, Morytania** (84 tiles away, coordinate_helper), contradicting the entry's own "far north
  Troll Country" description; ARRIVE could never resolve. npcId 14113 = Zemouregal's Fort armoured zombie
  (drop table matches). Fixed coords → 2780,10277 (region 11168, underground fort tunnels) and the travel
  (no direct teleport; via the Trollweiss Mountain peak tunnel). Note: npc_spawns had no mejrs data for
  this quest-instanced NPC; coord from the wiki spawn receipt, region-verified.
- **Albatross (Boat Combat) — PR #1145 (high):** SAILING requirement 55 → 50 (bounty-task Sailing level;
  matches sibling Narwhal=62 convention).
- **Champion's Challenge — PR #1146 (high + low):** gatekeeper "Larxia" → **Larxus**; the Champion's cape
  needs all **11** champions (10 race + Leon d'Cour), not 10 (fixed prose + Champion's cape
  milestoneKills 10→11).
- **Creature Creation (Spidine) — PR #1147 (high):** requiredItemIds [223, **331**] → [223, **327**] —
  331=Raw salmon, 327=Raw sardine (the description already said "raw sardine"). `runelite_id_lookup` +
  cache_ids verified.
- **Catacombs of Kourend — PR #1148 (low):** dark totem is not a flat "1/500 from any monster" - it's
  HP-dependent 1/(500-hitpoints), excludes ghosts, guaranteed from Superior Slayer monsters.

**Stamps:** V1 = `2026-06-30` for the 12 now-merged MINIGAMES fixes + the 10 OTHER batch-1 clean. The 5
OTHER fixes (#1144-#1148) stamp after merge. MINIGAMES now 23/25 (Brimhaven structural pointCost +
Mahogany Amy coord still open/deferred).

**Running total V1-stamped:** 146 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 23 MINIGAMES + 10 OTHER).
OTHER progress: 15/58. Next: OTHER batch 2 (Creature Creation Unicow → Fremennik Salvage).

## 2026-06-30 — Accuracy-convergence v2: OTHER batch 2 (30/58)

Cache `2026-06-25-rev239`. OTHER batch-1 fixes merged. Batch 2 (Creature Creation Unicow → Lost
Schematics): **11 CLEAN, 4 with findings; 3 fixed, 1 reverted/deferred**. All re-confirmed by WebFetch /
coordinate_helper.
- **Lost Schematics — DEFERRED (reverted PR #1150):** the finding said the coords 1824,3691 (Port
  Piscarilius, Sailing 15) should move to Port Sarim (3044,3193) to match the entry's "Port Sarim docks"
  travelTip + Sailing-1 requirement. **CI rejected it:** `SailingSourceTriageTest.
  lostSchematics_resolvedPortCoords_matchPortPiscarilius` deliberately asserts the Piscarilius anchor
  ("The ARRIVE_AT_TILE step targeting Port Piscarilius is the correct anchor"), with a sibling assertion
  for Port Tasks. The coord is an intentional, test-guarded design choice - editing the test to match the
  data change would be double-fixing to silence CI. Two wiki-cited positions genuinely conflict
  (verifier: Port-Sarim/Sailing-1; test author: Piscarilius), so this is **deferred for authoring review**
  - resolve which port is the true anchor, then update coord+test OR travelTip+requirement together.
  **Lesson: CI is the authoritative gate; local `validate_drop_rates` passed but the intentional
  regression test caught the mismatch.**
- **Forestry — PR #1151 (high):** Golden pheasant egg rate 0.003704 (stale 1/270, pre-Feb-2025) → 0.02
  (~1/50 per event, the wiki's corrected effective rate; not the 1/30 news figure).
- **Glough's Experiments — PR #1152 (high):** locationDescription "beneath Ape Atoll" → Kandarin (north
  of Tree Gnome Stronghold). Coords were already Kandarin; only the prose named the wrong continent (the
  sibling Demonic gorillas entry, same npcId 7144, was already correct).
- **Cyclopes — PR #1153 (low):** defender tier progression is leave-and-re-enter (re-show to Kamfreena),
  not "hold the defender in inventory"; holding only yields same-tier duplicates.

**Stamps:** V1 = `2026-06-30` for the 5 now-merged OTHER batch-1 fixes + the 11 OTHER batch-2 clean. The 3
batch-2 fixes (#1151 Forestry, #1152 Glough's, #1153 Cyclopes) stamp after merge; Lost Schematics deferred
(reverted #1150) - stays open.

**Running total V1-stamped:** 162 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 23 MINIGAMES + 26 OTHER).
OTHER progress: 30/58. Next: OTHER batch 3 (Martial Salvage → Prifddinas Elf).

## 2026-07-01 — Accuracy-convergence v2: OTHER batch 3 (44/58)

Cache `2026-06-25-rev239`. OTHER batch-2 fixes merged (#1151/#1152/#1153; Lost Schematics stays
deferred). Batch 3 (Martial Salvage → Pyramid Plunder, 14 sources — heavily Sailing content):
**12 CLEAN, 2 with findings; 2 fixed.** Tier-0 detectors (npc-id-cache, dead-end, phantom-mechanic,
over-highlight) had zero hits on any batch-3 source. Semantic pass = 14×verify → domain-skeptic
refutation; both upheld findings re-confirmed by WebFetch (0 refuted).
- **Ocean Encounters — PR #1155 (major):** Sailing requirement `28` → `40`. Every clog item is a
  giant-clam pearl (Tiny 31770 → Radiant 31800); giant clams are the only encounter yielding them and
  appear from Sailing 40 (below 40 only as a passenger). A solo player at 28 gets nothing, and 28 was
  not sourced to any wiki threshold. Wiki (Giant clam): "can appear anywhere on the ocean while the
  boat is moving, starting at level 40 Sailing." `D4Batch11RegressionTest` only asserts a SAILING entry
  is present (not a level), so the change is test-safe.
- **Port Tasks — PR #1156 (major+minor):** Soup (31283) is the Sailing **skilling pet** but was
  `isPet:false` → set `true`; also `wikiPage` `Soup_(item)` (404) → `Soup`. Wiki (Soup): "Soup is a
  skilling pet that can be obtained while training the Sailing skill." Item-field change only — the
  test-guarded Piscarilius coord anchor for Port Tasks (`SailingSourceTriageTest`) is untouched.
- **Manual pre-clears (not defects):** Ogress Shaman step[1] `objectId 31791` = "Hole" at (2523,2861)
  via `object_lookup` — the correct Corsair Cove Dungeon entrance (item-id collision with Shimmering
  pearl is a cross-namespace coincidence). Port Tasks / all salvage / boat-combat sources sharing
  `1824,3691` = the cache-confirmed, test-guarded Port Piscarilius anchor (design intent, not a bug).

**Cross-source follow-up (recorded, not yet fixed):** the same Soup (31283) `isPet:false` /
`wikiPage:"Soup_(item)"` misclassification also exists in **Barracuda Trials** (a different source /
batch). Left out of the Port Tasks PR to keep it one-source-scoped; fix when Barracuda Trials is
verified.

**Stamps:** V1 = `2026-06-30` for the 3 now-merged batch-2 fixes (#1151/#1152/#1153); V1 = `2026-07-01`
for the 12 batch-3 clean. The 2 batch-3 fixes (#1155 Ocean Encounters, #1156 Port Tasks) stamp after
merge.

**Running total V1-stamped:** 177 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 23 MINIGAMES + 41 OTHER).
OTHER progress: 44/58. Next: OTHER batch 4 (Random Events → Zombie Pirate Locker, incl. remaining
salvage/boat-combat + Revenants, Rune Dragon, Shayzien Armour, Stronghold of Security, TzHaar,
Waterfiend, Wilderness GWD).

## 2026-07-01 — Accuracy-convergence v2: OTHER batch 4 (58/58 — OTHER CATEGORY COMPLETE)

Cache `2026-06-25-rev239`. Batch-3 fixes merged (#1155/#1156). Final OTHER batch (Random Events →
Zombie Pirate Locker, 14 sources): **10 CLEAN, 4 with findings; 2 fixed, 2 deferred.** Tier-0 detectors
zero-hit across the batch. Semantic pass = 14×verify → domain-skeptic refutation.
- **Shayzien Armour — PR #1158 (major+minor):** milestoneKills were `40/80/120/160/200` (8x too high).
  Each Combat Ring tier drops one piece per defeat with no duplicates until the 5-piece set is complete,
  so a tier is guaranteed after 5 defeats; with sequential unlock the cumulative guarantee is
  `5/10/15/20/25`. Fixed all 25 milestoneKills + the matching `perItemStepDescription` "(N kills)" text,
  and added the missing `Defence 20` requirement (favour removed Jan 2024). Wiki (Combat Ring): "defeat
  each tier of soldier a total of five times"; "no duplicate pieces until ... the complete set from that
  tier"; "requires level 20 Defence."
- **Vampyre Kraken — PR #1159 (minor):** combat-step prose said Bottled storm `1/512`, contradicting the
  source's own Bottled storm item dropRate (`0.003333` = `1/300`). Aligned prose to the item rate.
- **TzHaar — DEFERRED (aggregate multi-NPC model).** The verifier upheld 9 per-NPC rate "fixes" (e.g.
  Obsidian helm 1/8000→1/2000, Toktz-mej-tal 1/16384→1/4096), each ~4x richer than the current values.
  But source `npcId 2173 = TzHaar-Ket`, while the item set spans TzHaar-**Ket** (obsidian armour),
  TzHaar-**Mej** (Toktz-mej-tal) AND TzHaar-**Xil** (Toktz-xil-* weapons) — items that don't drop from
  Ket at all. The near-uniform, consistently ~4x-down-weighted rates are the signature of a **deliberate
  aggregate "kill TzHaar in the city" effective-rate model** (only a fraction of the mixed population
  drops each item). The findings compare per-NPC wiki rates to aggregate effective rates — a domain-blind
  mismatch; mass-applying them would break the efficiency model. Deferred for authoring review (decide
  per-NPC vs aggregate modeling). [Same class as the "multi-source items are a feature" guardrail.]
- **Sea Treasures — DEFERRED (fragment re-model, test-guarded).** The verifier upheld that the 8 Medallion
  fragments are one-time collectibles from 8 distinct island activities (guided by the waterlogged
  journal), not milestone kills from a repeatable notice-board task. That is a structural source re-model
  (per-fragment coords/requirements/activities), not a data-value fix, and the source is test-guarded
  (`SailingSourceTriageTest.seaTreasures_...Piscarilius`). Deferred for authoring capture. Wiki (Medallion
  of the Deep): "Assembling the medallion requires that the player read the waterlogged journal ... hints
  on the location of the fragments."

**Stamps:** V1 = `2026-07-01` for the 2 now-merged batch-3 fixes (#1155/#1156) + the 10 batch-4 clean.
The 2 batch-4 fixes (#1158 Shayzien, #1159 Vampyre Kraken) stamp after merge; TzHaar + Sea Treasures
carry `deferred` in the fixes column.

**Running total V1-stamped:** 189 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 23 MINIGAMES + 53 OTHER).
**OTHER category COMPLETE: 58/58 verified** (53 clean-stamped, 2 fixes pending merge, 3 deferred: TzHaar,
Sea Treasures, Lost Schematics). Next category: **SKILLING** (17 sources), then **CLUES** (12).

## 2026-07-01 — Accuracy-convergence v2: SKILLING (17/17 — SKILLING CATEGORY COMPLETE)

Cache `2026-06-25-rev239`. OTHER batch-4 fixes merged (#1158/#1159). Full SKILLING category in one batch:
**12 CLEAN, 5 with findings; 4 fixed, 1 deferred.** Tier-0 accuracy layer clean (the dead-end-MANUAL /
phantom-mechanic / over-highlight warnings on Aerial Fishing, Motherlode, Fire Runes, Black Chinchompas
belong to the separate guidance-engine loop-class workstream, not accuracy — deliberately out of scope).
- **Deep Sea Fishing — PR #1161 (major):** Sailing requirement `1` → `56`. All clog fish come from deep
  sea trawling shoals; every shoal needs a trawling net and the lowest (rope net / Giant krill shoal) is
  69 Fishing / **56 Sailing**. Wiki (Deep sea trawling): "69 Fishing/56 Sailing for Giant krill shoals."
- **Rooftop Agility — PR #1162 (major+minor):** Agility gate `10` → `1` (Draynor rooftop became level 1
  when the May-2024 update removed its level-10 req; the generic source should gate at the lowest course).
  Also fixed a step travelTip labelling Varrock rooftop "(10+)" → "(30+)". Wiki: Draynor "available to
  players with 1 Agility ... requirement of 10 has been removed"; Varrock "30 or higher."
- **Runecrafting (Fire Runes) — PR #1163 (minor):** travelTip direction "run north-east" → "north/
  north-west" (fire altar is NW of Emir's Arena, not NE; the step description already said "west of
  Emir's Arena"). Wiki (Fire Altar): "north of Al Kharid, west of the Emir's Arena."
- **Farming (Fruit Trees) — PR #1164 (major):** Tree Gnome Village fairy ring `CLR` → `CIQ` (CLR lands
  on Ape Atoll; CIQ is the Tree Gnome Village patch code). Wiki (Fairy ring): CLR = Ape Atoll; CIQ =
  Tree Gnome Village.
- **Underwater Crabs — DEFERRED.** The verifier upheld (correctly, per wiki) that the Mogre Camp is
  entered by speaking to **Murphy at Port Khazard** ("speak to Murphy at Port Khazard to go diving"), not
  by diving at Mudskipper Point as the current 3-step guidance routes. But the correct replacement needs
  sourced Port-Khazard/Mogre-Camp dive coordinates for the ARRIVE/dive steps, which are not available
  without in-game capture (the AIQ→Mudskipper Point fairy-ring code itself is valid and was in-game-
  validated for Skeletal Wyvern, but nothing validates a Mudskipper→Mogre dive). Won't guess coords —
  deferred for authoring / in-game capture, wiki citation attached. Wiki (Mogre Camp / RFD Pirate Pete):
  "speak to Murphy at Port Khazard to go diving."

**Stamps:** V1 = `2026-07-01` for the 2 now-merged OTHER batch-4 fixes (#1158/#1159) + the 12 SKILLING
clean. The 4 SKILLING fixes (#1161-#1164) stamp after merge; Underwater Crabs carries `deferred`.

**Running total V1-stamped:** 203 / 225 (61 BOSSES + 45 SLAYER + 7 RAIDS + 23 MINIGAMES + 55 OTHER +
12 SKILLING). **SKILLING category COMPLETE: 17/17 verified** (12 clean-stamped, 4 fixes pending merge, 1
deferred: Underwater Crabs). Final category remaining: **CLUES** (12 sources).