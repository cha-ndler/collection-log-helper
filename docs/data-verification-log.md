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
| 1 | Abyssal Sire | BOSSES | - | - | 3 (2026-06-07) |
| 2 | Alchemical Hydra | BOSSES | - | - | 1 (2026-06-07) |
| 3 | Amoxliatl | BOSSES | - | - | 1 (2026-06-07) |
| 4 | Araxxor | BOSSES | - | - | 4 (2026-06-07) |
| 5 | Barrows | BOSSES | 2026-06-07 | - | - |
| 6 | Brutus | BOSSES | - | - | 1 (2026-06-07) |
| 7 | Bryophyta | BOSSES | - | - | 1 (2026-06-07) |
| 8 | Callisto | BOSSES | - | - | 1 (2026-06-07) |
| 9 | Cerberus | BOSSES | 2026-06-07 | - | - |
| 10 | Chaos Elemental | BOSSES | - | - | 3 (2026-06-07) |
| 11 | Chaos Fanatic | BOSSES | - | - | 1 (2026-06-07) |
| 12 | Commander Zilyana | BOSSES | - | - | 1 (2026-06-07) |
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

### Abyssal Sire - items[].itemId (Unsired) - blocker
- check: cross_check_ids + temple_lookup (wrong-variant). OSRS Wiki `Unsired` infobox lists `id = 13273`; osrsbox-db reports BOTH 13273 and 25624 as "Unsired" but flags **25624 `duplicate = True`** while 13273 is the canonical (`duplicate = False`). Released 2015 — its real id is the low one.
- ours: `25624`
- authoritative: `13273` (canonical, non-duplicate Unsired)
- action: change Unsired itemId 25624 -> 13273. The in-game collection log / RuneLite keys off the canonical item; the duplicate id will never match the player's logged item. needs human call to confirm against a live collection-log dump before the fix PR.
- status: open

### Abyssal Sire - guidanceSteps[1].npcId (Eye step) - high
- check: cache_diff_check + coordinate_helper. Step "Enter one of the Sire chambers through an Eye" stores `npcId = 6479`, but 6479 is a **game object** ("Eye (Abyssal Nexus)", object ids 6479,6480 per wiki), NOT an NPC (absent from osrsbox monsters-json; outside the Sire's 5886-5908 NPC block). `GuidanceStep` has a dedicated `objectId`/`objectIds` field; as an `npcId` it resolves to no NPC and the step's interaction target is unreachable.
- ours: `npcId = 6479`
- authoritative: 6479/6480 are OBJECT ids for the Eye; the field should be `objectId`, not `npcId`
- action: move 6479 from npcId to objectId (or objectIds [6479,6480]) for this step. needs human call: confirm the plugin's intended object-interaction modeling for "Peek".
- status: open

### Abyssal Sire - guidanceSteps[1] description/interactAction - low
- check: compare_source vs OSRS wiki ("Eye (Abyssal Nexus)"). Description claims peeking the Eye *enters* a Sire chamber; the wiki states the Eye's "Peek" option only reports **how many players are in the lair** - it does not enter a chamber (entry is by walking into the chamber opening). Misleading guidance prose paired with the wrong action verb.
- ours: description "Enter one of the Sire chambers through an Eye" + `interactAction = "Peek"`
- authoritative: Peek on the Eye = player-count check only; chamber entry is a separate walk-in
- action: reword step to separate "peek to check the chamber is empty" from "walk into the chamber", or drop the Peek framing. needs human call.
- status: open

### Alchemical Hydra - waypoints[0] "Fairy ring CIR" coords - high
- check: coordinate_helper. The waypoint named "Fairy ring CIR" is placed at the Mount Karuulm summit/dungeon area, not at the CIR fairy ring. OSRS Wiki `Map:Fairy rings` gives CIR at (1302, 3762); our coord (1310, 3810) coincides with the Mount Karuulm map centre (1311, 3807 per the Mount Karuulm wiki Map), i.e. ~48 tiles north of the actual ring at the volcano base. (Per the convention in source #1, a "Fairy ring X" waypoint should be the ring's arrival tile, as Abyssal Sire's DIP step is.)
- ours: `worldX=1310, worldY=3810` (plane 0)
- authoritative: CIR fairy ring = `1302, 3762` (OSRS Wiki Map:Fairy rings)
- action: set the "Fairy ring CIR" waypoint to 1302,3762. Note: fairy-ring travel works by menu code regardless, so impact is a misplaced map marker, not broken travel - but coords are graded high per this log's rubric. needs human call.
- status: open

### Amoxliatl - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. `requiredItemIds` is documented as "items that must be in inventory before this step can proceed" (a hard gate). The Bank step gates on [29271 Basic quetzal whistle, 2434 Prayer potion(4), 385 Shark]. Prayer potion(4) and Shark are generic consumables, not access-gating items - and both ids are *also* listed in step 3's `recommendedItemIds` (22325,24444,2434,385), so the generator already treats them as recommendations. The real access gates (The Heart of Darkness quest, 48 Slayer) are correctly in `requirements`. Quetzal whistle is borderline (travel convenience; lodestone + run south is the documented alternative).
- ours: requiredItemIds = `[29271, 2434, 385]` (Quetzal whistle, Prayer potion(4), Shark)
- authoritative: consumables (2434, 385) are recommendations, not requirements; only access-gating items belong in requiredItemIds
- action: move 2434 + 385 out of requiredItemIds (they already appear in recommendedItemIds); reconsider whether the quetzal whistle should be required given the lodestone alternative. needs human call.
- status: open

### Araxxor - requirements (missing Slayer + wrong quest) - high
- check: cache_diff_check / compare_source vs OSRS wiki. Wiki: "Araxxor ... requiring level 92 Slayer and access to Morytania to kill." Our `requirements` has NO skills entry (Slayer 92 omitted entirely) and lists quest SINS_OF_THE_FATHER. Morytania access is Priest in Peril, not Sins of the Father; SotF only provides Drakan's medallion travel, and the cave is reachable unquested via fairy ring (the data's own step text says "fastest unquested route"). The Cave (Morytania Spider Cave) wiki page lists `quest = No` for entry.
- ours: `requirements = {quests: [SINS_OF_THE_FATHER]}` (no skills)
- authoritative: 92 Slayer (hard requirement) + Morytania access (Priest in Peril). Sins of the Father is NOT required.
- action: add SLAYER 92 to requirements; replace/justify the quest gate (Morytania access via Priest in Peril) and drop SINS_OF_THE_FATHER as a hard requirement. needs human call.
- status: open

### Araxxor - travel fairy ring code (CLS vs ALQ) - high
- check: coordinate_helper. Travel guidance routes via "fairy ring CLS" in three places (top-level travelTip, guidanceSteps[1].description, guidanceSteps[1].travelTip). Map:Fairy rings puts CLS at (2682, 3081) - south-west, ~1000 tiles from the NE-Morytania spider cave (entrance ~3657,3407). The Morytania Spider Cave wiki gives the actual quickest ring as ALQ ("fairy ring code ALQ, then running south past the small bridge").
- ours: "fairy ring CLS" (travelTip + step 2 description + step 2 travelTip)
- authoritative: fairy ring ALQ (per Morytania Spider Cave wiki)
- action: replace CLS with ALQ in all three travel strings. needs human call.
- status: open

### Araxxor - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. Bank step hard-gates on [6685 Saradomin brew(4), 3024 Super restore(4), 2434 Prayer potion(4)] - all generic consumables, not access gates; 6685/3024/2434 also appear in step 3's recommendedItemIds.
- ours: requiredItemIds = `[6685, 3024, 2434]`
- authoritative: these are recommendations, not requirements
- action: move them out of requiredItemIds (already in recommendedItemIds). needs human call.
- status: open

### Araxxor - items[].wikiPage (shuffled) - low
- check: cross_check_ids surfaced name<->wikiPage mismatches. itemIds and names are all correct (verified vs wiki), but 5 wikiPage links point to the wrong item's page: 29784 Araxyte venom sack -> "Araxyte_fang"; 29786 Jar of venom -> "Araxyte_head"; 29788 Araxyte head -> "Noxious_blade"; 29792 Noxious blade -> "Noxious_pommel"; 29794 Noxious pommel -> "Rax". (29790/29781/29782/29799/29836 wikiPage values are correct.)
- ours: see list above
- authoritative: wikiPage should match the item name (Araxyte_venom_sack, Jar_of_venom, Araxyte_head, Noxious_blade, Noxious_pommel)
- action: fix the 5 shuffled wikiPage values; "view on wiki" links currently open the wrong page. needs human call.
- status: open

### Brutus - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. Bank step hard-gates on [12695 Super combat potion(4), 385 Shark, 2434 Prayer potion(4), 4151 Abyssal whip, 1704 Amulet of glory] - gear + consumables, none access-gating. This directly contradicts the same step's own text ("any tier above mithril works ... cheap gear is fine", "do not waste BiS supplies"). The only real gate (The Ides of Milk quest) is correctly in `requirements`. 4151/385/2434 also recur in step 3's recommendedItemIds.
- ours: requiredItemIds = `[12695, 385, 2434, 4151, 1704]`
- authoritative: these are recommendations (and the step text says cheap gear suffices); not requirements
- action: empty/trim requiredItemIds to genuine gates (none beyond the quest); move gear/consumables to recommendedItemIds. needs human call.
- status: open

### Bryophyta - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. The Bank step gates on [22374 Mossy key, 12695 Super combat potion(4), 6685 Saradomin brew(4), 2434 Prayer potion(4), 1704 Amulet of glory]. The Mossy key IS a correct access gate (and step 3 correctly gates on [22374] alone), but 12695/6685/2434/1704 are gear+consumables, not requirements (6685/2434 also appear in step 4 recommendedItemIds).
- ours: step 0 requiredItemIds = `[22374, 12695, 6685, 2434, 1704]`
- authoritative: only the Mossy key (22374) is access-gating; the rest are recommendations
- action: keep 22374, move 12695/6685/2434/1704 to recommendedItemIds. needs human call.
- status: open

### Callisto - travel direction (south-east vs north-east) - low
- check: coordinate_helper. Travel text says "Burning amulet -> Chaos Temple, then run south-east into Callisto's Den" (top-level travelTip + guidanceSteps[0].description + guidanceSteps[0].travelTip). Burning amulet Chaos Temple lands at (3234, 3634) (level 15 Wilderness, per Burning amulet wiki); Callisto's Den / boss tile is (3291, 3849). That is +57 east and +215 NORTH -> the run is north-east (mostly north), not south-east. Following "south-east" walks the player away from the Den (and toward lower Wilderness).
- ours: "run south-east" (x3 strings)
- authoritative: from Chaos Temple (3234,3634) to Callisto's Den (~3291,3849) the direction is north / north-east
- action: change "south-east" to "north-east" (or "north") in the three travel strings. needs human call. (Minor: item 27667 display name "Claws of Callisto" vs in-game/GE "Claws of callisto" - casing only, id correct, not logged as a separate finding.)
- status: open

### Chaos Elemental - guidanceSteps[0].requiredItemIds (incl. wrong Burning amulet variant) - high
- check: cross_check_ids + required-vs-recommended. requiredItemIds = [21167, 11941 Looting bag, 1704 Amulet of glory, 385 Shark, 2434 Prayer potion(4)]. (a) 21167 is a DUPLICATE-variant id of "Burning amulet(5)" - the canonical Burning amulet(5) is 21166 (per the Burning amulet wiki infobox: ids 21166/21169/21171/21173/21175); osrsbox shows 21166/21167/21168 all as "Burning amulet(5)" so 21167/21168 are duplicates. A player holding the normal 21166 amulet would NOT satisfy an exact-id requirement on 21167. (b) None of these items access-gate Chaos Elemental (no quest/skill needed; deep Wilderness is open) - they are travel/QoL/consumables.
- ours: requiredItemIds = `[21167, 11941, 1704, 385, 2434]`
- authoritative: canonical Burning amulet(5) = 21166; and none of the list is a true requirement
- action: if a burning amulet must be referenced use 21166 (or accept all charge ids 21166/21169/21171/21173/21175); move travel/consumables out of requiredItemIds. needs human call.
- status: open

### Chaos Elemental - travel direction (north-west vs north-east) - low
- check: coordinate_helper. Step 2 says "Burning amulet -> Lava Maze, run north-west to Rogues' Castle" (description + travelTip). Burning amulet Lava Maze lands at (3028, 3842) (level 41 Wilderness, per Burning amulet wiki); Rogues' Castle / Chaos Elemental tile is (3261, 3927) = +233 EAST and +85 north -> north-east (mostly east), not north-west. "North-west" walks toward the Wilderness Agility / GDZ, away from the castle.
- ours: "run north-west" (x2 strings)
- authoritative: from Lava Maze (3028,3842) to Rogues' Castle (3261,3927) the direction is north-east / east
- action: change "north-west" to "north-east" in the two travel strings. needs human call.
- status: open

### Chaos Elemental - guidanceSteps[2].recommendedItemIds (contradicts cheap-gear advice) - low
- check: required-vs-recommended sanity check. recommendedItemIds = [4827 Comp ogre bow, 4151 Abyssal whip, 12924 Toxic blowpipe (empty), 385 Shark, 1704 Amulet of glory]. The step (and step 0) repeatedly stress "bring cheap gear only - the Chaos Elemental disarms and unequips items, so risking BiS is wasteful", yet the rec list pairs a deliberately-cheap Comp ogre bow with high-value Abyssal whip + Toxic blowpipe - internally contradictory guidance.
- ours: recommendedItemIds = `[4827, 4151, 12924, 385, 1704]`
- authoritative: recommendations should be consistent with the cheap-gear framing (cheap ranged + DDS spec per the prose), not BiS melee/blowpipe
- action: reconcile the rec list with the cheap-gear advice (drop whip/blowpipe or soften the prose). needs human call.
- status: open

### Chaos Fanatic - guidanceSteps[0].requiredItemIds (same Burning amulet variant bug) - high
- check: cross_check_ids + required-vs-recommended. requiredItemIds = [21167, 11941 Looting bag, 1704 Amulet of glory, 385 Shark, 2434 Prayer potion(4)] - identical to Chaos Elemental. 21167 is a DUPLICATE-variant of "Burning amulet(5)" (canonical 21166; osrsbox shows 21166/21167/21168 all "Burning amulet(5)"), so an exact-id inventory check on 21167 won't match a player's normal 21166 amulet. None of the list access-gates Chaos Fanatic (level 42 Wilderness, no quest/skill).
- ours: requiredItemIds = `[21167, 11941, 1704, 385, 2434]`
- authoritative: canonical Burning amulet(5) = 21166; none of the list is a true requirement
- action: use 21166 (or all charge ids) if a burning amulet is referenced; move travel/consumables to recommendedItemIds. Same fix as Chaos Elemental. needs human call.
- status: open

### Commander Zilyana - guidanceSteps[0].requiredItemIds (irrelevant Salve amulet) - high
- check: cross_check_ids + required-vs-recommended. Step 0 is "Teleport to Trollheim and run north to the GWD entrance" with requiredItemIds = [12018]. 12018 = Salve amulet(ei) (per Salve amulet(ei) wiki, ids 12018/25278/26782). Salve amulet has no effect on Commander Zilyana (she is not undead) and is irrelevant to a Trollheim teleport/travel step; hard-gating step 0 on it is wrong. The real access gates (Troll Stronghold quest for Trollheim, 70 Agility for the GWD descent / Saradomin room) are already in `requirements`.
- ours: guidanceSteps[0].requiredItemIds = `[12018]` (Salve amulet (ei))
- authoritative: no item is required to teleport to Trollheim; Salve amulet is not relevant to Zilyana
- action: remove 12018 from this step's requiredItemIds (likely an erroneous/leftover id). needs human call. (Note: step 3 objectId 26504 "Open" - the Saradomin room door - could not be authoritatively confirmed via wiki/osrsbox object data; not contradicted, so not logged as a finding.)
- status: open

## Batch 2026-06-07 (V1) summary

First V1 batch: sources #1-#12 (BOSSES, alphabetical). Method: each itemId/name cross-checked
vs the OSRS Wiki item-id mapping + per-item wiki infobox + osrsbox-db; drop rates vs the wiki
drop tables; NPC/object ids vs osrsbox / wiki; coords vs wiki Map: data and RuneLite source;
required-vs-recommended by the field's "must be in inventory to proceed" semantics. No source
data was edited.

- **Verified clean (2):** Barrows (#5), Cerberus (#9) - every check passed.
- **With findings (10):** the other ten. 17 findings total: 1 blocker, 7 high, 9 low.

Systematic (machine-generation) patterns worth a single bulk fix rather than per-source:
1. **requiredItemIds stuffed with consumables/gear** (Amoxliatl, Araxxor, Brutus, Bryophyta,
   Chaos Elemental, Chaos Fanatic, Commander Zilyana). The field means "must be in inventory to
   proceed", but it's loaded with food/potions/weapons that don't gate access. Genuine gates
   were handled correctly where present (Barrows Spade, Bryophyta/Chaos Fanatic-step Mossy key).
2. **Burning amulet(5) duplicate-variant id 21167** used in requiredItemIds (Chaos Elemental,
   Chaos Fanatic). Canonical is 21166; an exact-id check on 21167 won't match a real amulet.
3. **Duplicate/wrong item variants** also at Abyssal Sire (Unsired 25624 dup of canonical 13273
   - the one blocker).
4. **Travel-string errors:** wrong fairy-ring code (Araxxor CLS->ALQ), wrong cardinal directions
   (Callisto south-east->north-east; Chaos Elemental north-west->north-east), wrong waypoint coord
   (Alchemical Hydra Fairy ring CIR 1310,3810 -> 1302,3762).
5. **Wrong/incomplete access requirements:** Araxxor missing Slayer 92 and listing the wrong quest
   (SINS_OF_THE_FATHER vs Morytania access); Commander Zilyana step-0 requiring an irrelevant
   Salve amulet(ei).
6. **Shuffled wikiPage links** (Araxxor - 5 items point to the wrong wiki page).

Drop rates were accurate across the board - every rate checked matched the wiki (incl. non-obvious
ones: Cerberus crystals 1/520 not 1/512, Key master teleport 1/65 not 1/64, Barrows 1/350.14 +
Bolt rack 0.6026). No missing or extra collection-log items found in any source.

All work committed on branch `verify/2026-06-07` (one commit per source); master untouched.
Next V1 queue position: #13 Corporeal Beast.
