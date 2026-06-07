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

> **Reconciliation pass — 2026-06-07 (domain-skeptic).** Every finding below from the first
> V1 run was re-passed through the `domain-skeptic` charter (default-REFUTED adversarial OSRS
> review) to separate real bugs from domain artifacts before any fix. Each entry now carries a
> `reconcile` verdict (STANDS / REFUTED / INSUFFICIENT) and a quoted `receipt`.
> **Tooling note:** in this background session the `domain-skeptic` *agent type* and the
> `runelite-dev` MCP (`temple_lookup`/`wiki_lookup`/`cross_check_ids`) were not loadable
> (plugin agents/MCP are absent in headless runs). The charter was therefore run via
> general-purpose subagents using the same underlying authoritative sources the MCP wraps:
> the OSRS Wiki raw infobox wikitext (`<page>?action=raw`, quoted verbatim with URL) and the
> RuneLite item-id map (`prices.runescape.wiki/api/v1/osrs/mapping`, GE-tradeable items only).
> Receipts are quoted raw output, not paraphrase.
> **Outcome:** 11 STAND (1 blocker, 6 high, 4 low), 2 REFUTED (drop), 4 INSUFFICIENT (need
> human ratification — all the "consumables-in-requiredItemIds" set, where the domain confirms
> the items don't gate access but the required-vs-recommended fix is a plugin-schema call).

### Abyssal Sire - items[].itemId (Unsired) - blocker
- check: cross_check_ids + temple_lookup (wrong-variant). OSRS Wiki `Unsired` infobox lists `id = 13273`; osrsbox-db reports BOTH 13273 and 25624 as "Unsired" but flags **25624 `duplicate = True`** while 13273 is the canonical (`duplicate = False`). Released 2015 — its real id is the low one.
- ours: `25624`
- authoritative: `13273` (canonical, non-duplicate Unsired)
- action: change Unsired itemId 25624 -> 13273. The in-game collection log / RuneLite keys off the canonical item; the duplicate id will never match the player's logged item. needs human call to confirm against a live collection-log dump before the fix PR.
- reconcile (2026-06-07): **STANDS - blocker.** (Note: the original framing was inverted — it guessed 13273 was the "duplicate"; the wiki shows 13273 is the *only* Unsired id and 25624 appears nowhere. The defect is real and the fix direction unchanged: 25624 -> 13273.)
- receipt: `curl ".../w/Unsired?action=raw"` -> `{{Infobox Item |name = Unsired ... |tradeable = No ... |id = 13273}}` (single `|id =`, no `|id1/|id2`); 25624 occurs 0× on the page. Untradeable, so correctly absent from the GE map — that absence does not validate 25624. <https://oldschool.runescape.wiki/w/Unsired?action=raw>
- status: STANDS (blocker) — fix to 13273; still confirm against a live clog dump before the fix PR.

### Abyssal Sire - guidanceSteps[1].npcId (Eye step) - high
- check: cache_diff_check + coordinate_helper. Step "Enter one of the Sire chambers through an Eye" stores `npcId = 6479`, but 6479 is a **game object** ("Eye (Abyssal Nexus)", object ids 6479,6480 per wiki), NOT an NPC (absent from osrsbox monsters-json; outside the Sire's 5886-5908 NPC block). `GuidanceStep` has a dedicated `objectId`/`objectIds` field; as an `npcId` it resolves to no NPC and the step's interaction target is unreachable.
- ours: `npcId = 6479`
- authoritative: 6479/6480 are OBJECT ids for the Eye; the field should be `objectId`, not `npcId`
- action: move 6479 from npcId to objectId (or objectIds [6479,6480]) for this step. needs human call: confirm the plugin's intended object-interaction modeling for "Peek".
- reconcile (2026-06-07): **REFUTED — drop.** The Eye is an NPC, not a game object; 6479/6480 are NPC ids and storing 6479 as `npcId` with `interactAction="Peek"` is correct. The first-run finding was domain-blind (assumed object).
- receipt: `curl ".../w/Eye_(Abyssal_Nexus)?action=raw"` -> `{{Infobox NPC |name = Eye |location = [[Abyssal Nexus]] |options = Peek |examine = The Abyss stares back. ... |id = 6479,6480}}` — `{{Infobox NPC}}`, ids under the NPC infobox `|id =`. <https://oldschool.runescape.wiki/w/Eye_(Abyssal_Nexus)?action=raw>
- status: REFUTED (drop — npcId 6479 is correct; not a bug)

### Abyssal Sire - guidanceSteps[1] description/interactAction - low
- check: compare_source vs OSRS wiki ("Eye (Abyssal Nexus)"). Description claims peeking the Eye *enters* a Sire chamber; the wiki states the Eye's "Peek" option only reports **how many players are in the lair** - it does not enter a chamber (entry is by walking into the chamber opening). Misleading guidance prose paired with the wrong action verb.
- ours: description "Enter one of the Sire chambers through an Eye" + `interactAction = "Peek"`
- authoritative: Peek on the Eye = player-count check only; chamber entry is a separate walk-in
- action: reword step to separate "peek to check the chamber is empty" from "walk into the chamber", or drop the Peek framing. needs human call.
- reconcile (2026-06-07): **STANDS - low.** Peek reports chamber occupancy only; it does not enter. The description "Enter ... through an Eye" misdescribes the mechanic. Prose-only fix (the npcId/interactAction pairing itself is valid per the entry above).
- receipt: `curl ".../w/Eye_(Abyssal_Nexus)?action=raw"` -> "Using the \"Peek\" option will tell the player how many players are present in the chamber, and additionally how long it has been since the last player was there." <https://oldschool.runescape.wiki/w/Eye_(Abyssal_Nexus)?action=raw>
- status: STANDS (low) — reword guidance prose only.

### Alchemical Hydra - waypoints[0] "Fairy ring CIR" coords - high
- check: coordinate_helper. The waypoint named "Fairy ring CIR" is placed at the Mount Karuulm summit/dungeon area, not at the CIR fairy ring. OSRS Wiki `Map:Fairy rings` gives CIR at (1302, 3762); our coord (1310, 3810) coincides with the Mount Karuulm map centre (1311, 3807 per the Mount Karuulm wiki Map), i.e. ~48 tiles north of the actual ring at the volcano base. (Per the convention in source #1, a "Fairy ring X" waypoint should be the ring's arrival tile, as Abyssal Sire's DIP step is.)
- ours: `worldX=1310, worldY=3810` (plane 0)
- authoritative: CIR fairy ring = `1302, 3762` (OSRS Wiki Map:Fairy rings)
- action: set the "Fairy ring CIR" waypoint to 1302,3762. Note: fairy-ring travel works by menu code regardless, so impact is a misplaced map marker, not broken travel - but coords are graded high per this log's rubric. needs human call.
- reconcile (2026-06-07): **STANDS - high.** Our (1310,3810) ≈ Mount Karuulm summit centre (1311,3807), ~48 tiles north of the CIR ring's actual arrival tile (1302,3762). A waypoint named for the ring should be the ring's arrival tile.
- receipt: Fairy_rings raw -> `|{{Map|...|title=Fairy ring (CIR)|1302,3762|mtype=pin...}}` / "[[Kebos Lowlands]]: South of [[Mount Karuulm]]"; Mount_Karuulm raw -> `|map = {{Map|name=Mount Karuulm|x=1311|y=3807|...}}` + "Using the [[fairy ring]] (code CIR) and travelling up the volcano". <https://oldschool.runescape.wiki/w/Fairy_rings?action=raw>
- status: STANDS (high) — set waypoint to 1302,3762.

### Amoxliatl - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. `requiredItemIds` is documented as "items that must be in inventory before this step can proceed" (a hard gate). The Bank step gates on [29271 Basic quetzal whistle, 2434 Prayer potion(4), 385 Shark]. Prayer potion(4) and Shark are generic consumables, not access-gating items - and both ids are *also* listed in step 3's `recommendedItemIds` (22325,24444,2434,385), so the generator already treats them as recommendations. The real access gates (The Heart of Darkness quest, 48 Slayer) are correctly in `requirements`. Quetzal whistle is borderline (travel convenience; lodestone + run south is the documented alternative).
- ours: requiredItemIds = `[29271, 2434, 385]` (Quetzal whistle, Prayer potion(4), Shark)
- authoritative: consumables (2434, 385) are recommendations, not requirements; only access-gating items belong in requiredItemIds
- action: move 2434 + 385 out of requiredItemIds (they already appear in recommendedItemIds); reconsider whether the quetzal whistle should be required given the lodestone alternative. needs human call.
- reconcile (2026-06-07): **INSUFFICIENT — needs human ratification.** Domain confirms the factual core (quest + 48 Slayer are the real gates, already in `requirements`; Prayer pot/Shark gate nothing; quetzal whistle has documented alternatives) — but whether non-gating items belong in `requiredItemIds` is a plugin-schema-semantics call the wiki can't decide.
- receipt: Amoxliatl raw -> "fought during [[The Heart of Darkness]] ... After the quest, Amoxliatl can be fought again as a regular boss" + `|slaylvl = 48`; Ruins_of_Tapoyauik raw lists travel alts: "[[pendant of ates]] can teleport directly ..." and "[[Quetzal Transport System]] to [[Quetzacalli Gorge]] (unlocked by default)". <https://oldschool.runescape.wiki/w/Amoxliatl?action=raw>
- status: INSUFFICIENT — needs human ratification of requiredItemIds semantics (one of 4 in this cluster: Amoxliatl/Araxxor/Brutus/Bryophyta).

### Araxxor - requirements (missing Slayer + wrong quest) - high
- check: cache_diff_check / compare_source vs OSRS wiki. Wiki: "Araxxor ... requiring level 92 Slayer and access to Morytania to kill." Our `requirements` has NO skills entry (Slayer 92 omitted entirely) and lists quest SINS_OF_THE_FATHER. Morytania access is Priest in Peril, not Sins of the Father; SotF only provides Drakan's medallion travel, and the cave is reachable unquested via fairy ring (the data's own step text says "fastest unquested route"). The Cave (Morytania Spider Cave) wiki page lists `quest = No` for entry.
- ours: `requirements = {quests: [SINS_OF_THE_FATHER]}` (no skills)
- authoritative: 92 Slayer (hard requirement) + Morytania access (Priest in Peril). Sins of the Father is NOT required.
- action: add SLAYER 92 to requirements; replace/justify the quest gate (Morytania access via Priest in Peril) and drop SINS_OF_THE_FATHER as a hard requirement. needs human call.
- reconcile (2026-06-07): **STANDS - high.** Wiki: kill requires 92 Slayer + Morytania access + an active spider/araxyte task. Our requirements omit Slayer 92 and wrongly list SINS_OF_THE_FATHER; the cave entrance is NE of Darkmeyer, reachable overland (fairy ring ALQ + run) without entering Darkmeyer city, and a changelog notes the cave no longer needs A Night at the Theatre.
- receipt: Araxxor raw -> `|slaylvl = 92` + "requiring level 92 [[Slayer]] and access to [[Morytania]] to kill ... may only be killed while on a spider or araxyte [[Slayer task]]"; Morytania raw -> "Players must complete the [[Priest in Peril]] quest to enter." <https://oldschool.runescape.wiki/w/Araxxor?action=raw>
- status: STANDS (high) — add SLAYER 92; drop SINS_OF_THE_FATHER (Morytania access = Priest in Peril).

### Araxxor - travel fairy ring code (CLS vs ALQ) - high
- check: coordinate_helper. Travel guidance routes via "fairy ring CLS" in three places (top-level travelTip, guidanceSteps[1].description, guidanceSteps[1].travelTip). Map:Fairy rings puts CLS at (2682, 3081) - south-west, ~1000 tiles from the NE-Morytania spider cave (entrance ~3657,3407). The Morytania Spider Cave wiki gives the actual quickest ring as ALQ ("fairy ring code ALQ, then running south past the small bridge").
- ours: "fairy ring CLS" (travelTip + step 2 description + step 2 travelTip)
- authoritative: fairy ring ALQ (per Morytania Spider Cave wiki)
- action: replace CLS with ALQ in all three travel strings. needs human call.
- reconcile (2026-06-07): **STANDS - high.** Wiki names ALQ as the quickest fairy-ring route to the cave entrance (lands ~3597,3495 in NE Morytania by Darkmeyer); CLS lands at 2682,3081 in the far SW (Tirannwn) — wrong code entirely.
- receipt: Morytania_Spider_Cave raw -> "Teleporting via [[ectophial]] or [[fairy ring]] code {{fairycode|ALQ}}, then running south past the small bridge"; Fairy_rings raw -> CLS `{{Map|...|title=Fairy ring (CLS)|2682,3081|...}}`, ALQ at (3597,3495). <https://oldschool.runescape.wiki/w/Morytania_Spider_Cave?action=raw>
- status: STANDS (high) — replace CLS -> ALQ in all three travel strings.

### Araxxor - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. Bank step hard-gates on [6685 Saradomin brew(4), 3024 Super restore(4), 2434 Prayer potion(4)] - all generic consumables, not access gates; 6685/3024/2434 also appear in step 3's recommendedItemIds.
- ours: requiredItemIds = `[6685, 3024, 2434]`
- authoritative: these are recommendations, not requirements
- action: move them out of requiredItemIds (already in recommendedItemIds). needs human call.
- reconcile (2026-06-07): **INSUFFICIENT — needs human ratification.** Domain confirms none of 6685/3024/2434 gate access to Araxxor (no item is an access prerequisite on the wiki) — but moving them to recommendedItemIds is a plugin-schema-semantics call (does `requiredItemIds` mean "access gate" or "bring-list"?), which the wiki can't decide. Most players treat brews+restores as effectively mandatory for the kill.
- receipt: Araxxor raw -> requirements stated only as "level 92 [[Slayer]] and access to [[Morytania]] ... on a spider or araxyte [[Slayer task]]"; no consumable named as a gate. GE map: 6685 Saradomin brew(4), 3024 Super restore(4), 2434 Prayer potion(4) — ordinary tradeable consumables. <https://oldschool.runescape.wiki/w/Araxxor?action=raw>
- status: INSUFFICIENT — needs human ratification of requiredItemIds semantics (cluster of 4).

### Araxxor - items[].wikiPage (shuffled) - low
- check: cross_check_ids surfaced name<->wikiPage mismatches. itemIds and names are all correct (verified vs wiki), but 5 wikiPage links point to the wrong item's page: 29784 Araxyte venom sack -> "Araxyte_fang"; 29786 Jar of venom -> "Araxyte_head"; 29788 Araxyte head -> "Noxious_blade"; 29792 Noxious blade -> "Noxious_pommel"; 29794 Noxious pommel -> "Rax". (29790/29781/29782/29799/29836 wikiPage values are correct.)
- ours: see list above
- authoritative: wikiPage should match the item name (Araxyte_venom_sack, Jar_of_venom, Araxyte_head, Noxious_blade, Noxious_pommel)
- action: fix the 5 shuffled wikiPage values; "view on wiki" links currently open the wrong page. needs human call.
- reconcile (2026-06-07): **STANDS - low.** All 5 name->wikiPage links are shifted by one (each links to the next item's page); itemIds and names are correct. Confirmed by id-cross-checked infoboxes. Notably 29794 "Noxious pommel" -> "Rax", which is a `#REDIRECT` to the Nid monster, not an item.
- receipt: wiki `?action=raw` per item -> Araxyte_venom_sack `|id = 29784`; Araxyte_fang `|id = 29799`; Jar_of_venom `|id = 29786`; Araxyte_head `|id = 29788`; Noxious_blade `|id = 29792`; Noxious_pommel `|id = 29794`; Rax -> `#REDIRECT [[Nid#Rax]]`. Correct pages: Araxyte_venom_sack / Jar_of_venom / Araxyte_head / Noxious_blade / Noxious_pommel. <https://oldschool.runescape.wiki/w/Noxious_pommel?action=raw>
- status: STANDS (low) — fix the 5 shuffled wikiPage values.

### Brutus - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. Bank step hard-gates on [12695 Super combat potion(4), 385 Shark, 2434 Prayer potion(4), 4151 Abyssal whip, 1704 Amulet of glory] - gear + consumables, none access-gating. This directly contradicts the same step's own text ("any tier above mithril works ... cheap gear is fine", "do not waste BiS supplies"). The only real gate (The Ides of Milk quest) is correctly in `requirements`. 4151/385/2434 also recur in step 3's recommendedItemIds.
- ours: requiredItemIds = `[12695, 385, 2434, 4151, 1704]`
- authoritative: these are recommendations (and the step text says cheap gear suffices); not requirements
- action: empty/trim requiredItemIds to genuine gates (none beyond the quest); move gear/consumables to recommendedItemIds. needs human call.
- reconcile (2026-06-07): **INSUFFICIENT — needs human ratification.** Domain confirms Brutus has no item access-gate (sole gate is the quest, correctly in `requirements`; the quest itself needs no items) and none of [12695,385,2434,4151,1704] gate access — but demoting them out of `requiredItemIds` is a plugin-schema-semantics call the wiki can't decide.
- receipt: The_Ides_of_Milk raw -> Quest infobox "|requirements = None |items = None |recommended = {{SCP|Combat|15...}}", reward "Access to [[Brutus]], the cow boss"; Brutus raw -> "low-level free-to-play boss ... can be reasonably fought almost immediately after starting the game." <https://oldschool.runescape.wiki/w/The_Ides_of_Milk?action=raw>
- status: INSUFFICIENT — needs human ratification of requiredItemIds semantics (cluster of 4).

### Bryophyta - guidanceSteps[0].requiredItemIds - low
- check: required-vs-recommended sanity check. The Bank step gates on [22374 Mossy key, 12695 Super combat potion(4), 6685 Saradomin brew(4), 2434 Prayer potion(4), 1704 Amulet of glory]. The Mossy key IS a correct access gate (and step 3 correctly gates on [22374] alone), but 12695/6685/2434/1704 are gear+consumables, not requirements (6685/2434 also appear in step 4 recommendedItemIds).
- ours: step 0 requiredItemIds = `[22374, 12695, 6685, 2434, 1704]`
- authoritative: only the Mossy key (22374) is access-gating; the rest are recommendations
- action: keep 22374, move 12695/6685/2434/1704 to recommendedItemIds. needs human call.
- reconcile (2026-06-07): **INSUFFICIENT — needs human ratification.** Domain confirms both halves: the Mossy key (22374) IS a genuine first-time access gate, and 12695/6685/2434/1704 are not access gates (not even named on the page). But the remedy (keep the key, demote the rest) is a plugin-schema-semantics call the wiki can't decide.
- receipt: Bryophyta raw -> "A [[mossy key]] is needed to unlock the [[Gate (Bryophyta)|gate]] leading to Bryophyta's lair for the first time ... The key will not be consumed when unlocking Bryophyta's lair." The four contested ids appear nowhere as access requirements. <https://oldschool.runescape.wiki/w/Bryophyta?action=raw>
- status: INSUFFICIENT — needs human ratification of requiredItemIds semantics (cluster of 4; the Mossy-key gate itself is confirmed correct).

### Callisto - travel direction (south-east vs north-east) - low
- check: coordinate_helper. Travel text says "Burning amulet -> Chaos Temple, then run south-east into Callisto's Den" (top-level travelTip + guidanceSteps[0].description + guidanceSteps[0].travelTip). Burning amulet Chaos Temple lands at (3234, 3634) (level 15 Wilderness, per Burning amulet wiki); Callisto's Den / boss tile is (3291, 3849). That is +57 east and +215 NORTH -> the run is north-east (mostly north), not south-east. Following "south-east" walks the player away from the Den (and toward lower Wilderness).
- ours: "run south-east" (x3 strings)
- authoritative: from Chaos Temple (3234,3634) to Callisto's Den (~3291,3849) the direction is north / north-east
- action: change "south-east" to "north-east" (or "north") in the three travel strings. needs human call. (Minor: item 27667 display name "Claws of Callisto" vs in-game/GE "Claws of callisto" - casing only, id correct, not logged as a separate finding.)
- reconcile (2026-06-07): **STANDS - low.** Verified landing (3234,3634) -> verified boss tile (3291,3849) is +57 east / +215 north = predominantly NORTH (NNE). "South-east" is wrong on the dominant axis and misdirects toward lower Wilderness.
- receipt: Burning_amulet raw -> `{{TeleportLocationLine|name=[[Chaos Temple ...]] (level 15 Wilderness)|x=3234|y=3634}}`; Callisto LocLine -> `x:3291,y:3849` (mapID=0, plane=0). dx=+57, dy=+215. <https://oldschool.runescape.wiki/w/Burning_amulet?action=raw>
- status: STANDS (low) — change "south-east" -> "north"/"north-east" in the three strings.

### Chaos Elemental - guidanceSteps[0].requiredItemIds (incl. wrong Burning amulet variant) - high
- check: cross_check_ids + required-vs-recommended. requiredItemIds = [21167, 11941 Looting bag, 1704 Amulet of glory, 385 Shark, 2434 Prayer potion(4)]. (a) 21167 is a DUPLICATE-variant id of "Burning amulet(5)" - the canonical Burning amulet(5) is 21166 (per the Burning amulet wiki infobox: ids 21166/21169/21171/21173/21175); osrsbox shows 21166/21167/21168 all as "Burning amulet(5)" so 21167/21168 are duplicates. A player holding the normal 21166 amulet would NOT satisfy an exact-id requirement on 21167. (b) None of these items access-gate Chaos Elemental (no quest/skill needed; deep Wilderness is open) - they are travel/QoL/consumables.
- ours: requiredItemIds = `[21167, 11941, 1704, 385, 2434]`
- authoritative: canonical Burning amulet(5) = 21166; and none of the list is a true requirement
- action: if a burning amulet must be referenced use 21166 (or accept all charge ids 21166/21169/21171/21173/21175); move travel/consumables out of requiredItemIds. needs human call.
- reconcile (2026-06-07): **STANDS - high.** The wiki Burning amulet infobox lists charge ids {21175,21173,21171,21169,21166} only; **21167 appears nowhere** — it is not a valid Burning amulet id, so an exact-id check on 21167 never matches a real amulet (21166 = the (5) charge). (The non-gating sub-claim about the other ids overlaps the requiredItemIds-semantics cluster, but the 21167 id defect STANDS independently.)
- receipt: Burning_amulet raw -> `|id1 = 21175 |id2 = 21173 |id3 = 21171 |id4 = 21169 |id5 = 21166` with `|version1..5 = (1)..(5)`. <https://oldschool.runescape.wiki/w/Burning_amulet?action=raw>
- status: STANDS (high) — replace 21167 with 21166 (or accept the full charge set). requiredItemIds-trimming portion is part of the semantics cluster.

### Chaos Elemental - travel direction (north-west vs north-east) - low
- check: coordinate_helper. Step 2 says "Burning amulet -> Lava Maze, run north-west to Rogues' Castle" (description + travelTip). Burning amulet Lava Maze lands at (3028, 3842) (level 41 Wilderness, per Burning amulet wiki); Rogues' Castle / Chaos Elemental tile is (3261, 3927) = +233 EAST and +85 north -> north-east (mostly east), not north-west. "North-west" walks toward the Wilderness Agility / GDZ, away from the castle.
- ours: "run north-west" (x2 strings)
- authoritative: from Lava Maze (3028,3842) to Rogues' Castle (3261,3927) the direction is north-east / east
- action: change "north-west" to "north-east" in the two travel strings. needs human call.
- reconcile (2026-06-07): **STANDS - low.** Exact wiki coords: Lava Maze burning-amulet landing (3028,3842) -> Rogues' Castle (3286,3933) = +258 east / +91 north = dominantly EAST (north-east). "North-west" is wrong on both axes.
- receipt: Burning_amulet raw -> `{{TeleportLocationLine|name=[[Lava Maze]] (level 41 Wilderness)|x=3028|y=3842|r=2}}`; Rogues'_Castle raw -> `|map = {{Map|name=Rogues' Castle|x=3286|y=3933|...}}`. <https://oldschool.runescape.wiki/w/Rogues%27_Castle?action=raw>
- status: STANDS (low) — change "north-west" -> "north-east" in the two strings.

### Chaos Elemental - guidanceSteps[2].recommendedItemIds (contradicts cheap-gear advice) - low
- check: required-vs-recommended sanity check. recommendedItemIds = [4827 Comp ogre bow, 4151 Abyssal whip, 12924 Toxic blowpipe (empty), 385 Shark, 1704 Amulet of glory]. The step (and step 0) repeatedly stress "bring cheap gear only - the Chaos Elemental disarms and unequips items, so risking BiS is wasteful", yet the rec list pairs a deliberately-cheap Comp ogre bow with high-value Abyssal whip + Toxic blowpipe - internally contradictory guidance.
- ours: recommendedItemIds = `[4827, 4151, 12924, 385, 1704]`
- authoritative: recommendations should be consistent with the cheap-gear framing (cheap ranged + DDS spec per the prose), not BiS melee/blowpipe
- action: reconcile the rec list with the cheap-gear advice (drop whip/blowpipe or soften the prose). needs human call.
- reconcile (2026-06-07): **REFUTED — drop.** The disarm mechanic the prose warns about is real and correctly described, so nothing in the OSRS source makes the recommendedItemIds list factually *wrong*. The cheap-vs-BiS inconsistency is a guidance-tone/design preference, not a domain bug — out of scope for a data-verification finding.
- receipt: Chaos_Elemental raw -> special-attack list `* '''Madness''' - Unequips up to four equipped items`. <https://oldschool.runescape.wiki/w/Chaos_Elemental?action=raw>
- status: REFUTED (drop — no domain/data error; gear-list consistency is a design choice, not a bug)

### Chaos Fanatic - guidanceSteps[0].requiredItemIds (same Burning amulet variant bug) - high
- check: cross_check_ids + required-vs-recommended. requiredItemIds = [21167, 11941 Looting bag, 1704 Amulet of glory, 385 Shark, 2434 Prayer potion(4)] - identical to Chaos Elemental. 21167 is a DUPLICATE-variant of "Burning amulet(5)" (canonical 21166; osrsbox shows 21166/21167/21168 all "Burning amulet(5)"), so an exact-id inventory check on 21167 won't match a player's normal 21166 amulet. None of the list access-gates Chaos Fanatic (level 42 Wilderness, no quest/skill).
- ours: requiredItemIds = `[21167, 11941, 1704, 385, 2434]`
- authoritative: canonical Burning amulet(5) = 21166; none of the list is a true requirement
- action: use 21166 (or all charge ids) if a burning amulet is referenced; move travel/consumables to recommendedItemIds. Same fix as Chaos Elemental. needs human call.
- reconcile (2026-06-07): **STANDS - high.** Same 21167 id defect as Chaos Elemental — 21167 is not a valid Burning amulet id (canonical (5) = 21166; full set {21175,21173,21171,21169,21166}), so an exact-id check never matches a real amulet. (requiredItemIds-trimming portion is part of the semantics cluster.)
- receipt: Burning_amulet raw -> `|id1 = 21175 |id2 = 21173 |id3 = 21171 |id4 = 21169 |id5 = 21166` (versions (1)..(5)); 21167 absent. <https://oldschool.runescape.wiki/w/Burning_amulet?action=raw>
- status: STANDS (high) — replace 21167 with 21166 (or full charge set). Same fix as Chaos Elemental.

### Commander Zilyana - guidanceSteps[0].requiredItemIds (irrelevant Salve amulet) - high
- check: cross_check_ids + required-vs-recommended. Step 0 is "Teleport to Trollheim and run north to the GWD entrance" with requiredItemIds = [12018]. 12018 = Salve amulet(ei) (per Salve amulet(ei) wiki, ids 12018/25278/26782). Salve amulet has no effect on Commander Zilyana (she is not undead) and is irrelevant to a Trollheim teleport/travel step; hard-gating step 0 on it is wrong. The real access gates (Troll Stronghold quest for Trollheim, 70 Agility for the GWD descent / Saradomin room) are already in `requirements`.
- ours: guidanceSteps[0].requiredItemIds = `[12018]` (Salve amulet (ei))
- authoritative: no item is required to teleport to Trollheim; Salve amulet is not relevant to Zilyana
- action: remove 12018 from this step's requiredItemIds (likely an erroneous/leftover id). needs human call. (Note: step 3 objectId 26504 "Open" - the Saradomin room door - could not be authoritatively confirmed via wiki/osrsbox object data; not contradicted, so not logged as a finding.)
- reconcile (2026-06-07): **STANDS - high.** Zilyana is a living Icyene (attack style Crush/Magic), not undead, so a Salve amulet gives no combat benefit; and the Trollheim Teleport requires only runes + Eadgar's Ruse, no item. Hard-gating a teleport/run step on 12018 is an erroneous/leftover id. (Distinct from the consumables cluster — this is a wrong/irrelevant required item, not a consumable-vs-recommendation question.)
- receipt: Commander_Zilyana raw -> "|attack style = [[Crush]], [[Magic]]" + "one of only two known still living representatives of the [[Icyene]] race" (no "undead"); Trollheim_Teleport raw -> "|cost = {{RuneReq|Law=2|Fire=2}}" + "Players must have completed the [[Eadgar's Ruse]] quest" (no item required). 12018 is a valid Salve amulet(ei) id, so this is a relevance bug, not a wrong-id bug. <https://oldschool.runescape.wiki/w/Commander_Zilyana?action=raw>
- status: STANDS (high) — remove 12018 from guidanceSteps[0].requiredItemIds.

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

## Reconciliation summary — 2026-06-07 (domain-skeptic pass)

The 17 first-run findings were re-passed through the `domain-skeptic` charter (default-REFUTED
adversarial OSRS review) before any fix, each with a quoted authoritative receipt. Tooling: the
`domain-skeptic` agent type and the `runelite-dev` MCP were not loadable in this background/headless
session, so the charter ran via general-purpose subagents against the same authoritative sources
the MCP wraps — OSRS Wiki raw infobox wikitext (`<page>?action=raw`) and the RuneLite item-id map
(`prices.runescape.wiki/api/v1/osrs/mapping`, GE-tradeable only).

**Verdict tally: 11 STANDS · 2 REFUTED · 4 INSUFFICIENT.**

- **STANDS (11) — real bugs, receipt-backed (1 blocker, 6 high, 4 low):**
  - blocker: Abyssal Sire Unsired id 25624 (wiki `|id = 13273`; 25624 unknown to the wiki).
  - high: Araxxor requirements (missing Slayer 92, wrong quest SINS_OF_THE_FATHER); Araxxor fairy
    ring CLS->ALQ; Alchemical Hydra "Fairy ring CIR" waypoint at the summit not the ring (1302,3762);
    Chaos Elemental & Chaos Fanatic Burning amulet id 21167 (invalid; canonical (5) = 21166);
    Commander Zilyana step-0 required Salve amulet(ei) 12018 (Zilyana not undead; teleport needs no item).
  - low: Abyssal Sire "Peek enters chamber" prose (Peek = occupancy check only); Araxxor 5 shuffled
    wikiPage links; Chaos Elemental NW->NE direction; Callisto SE->N/NE direction.
- **REFUTED (2) — dropped as legitimate mechanics / non-domain:**
  - Abyssal Sire `npcId = 6479` (the Eye IS an NPC — `{{Infobox NPC}}`, ids 6479/6480, `options=Peek`;
    the first-run "it's an object" claim was domain-blind).
  - Chaos Elemental recommendedItemIds "cheap-gear contradiction" (the disarm mechanic is real and
    correctly described; gear-list tone is a design choice, not a data error).
- **INSUFFICIENT (4) — need human ratification (one cluster):** Amoxliatl / Araxxor / Brutus /
  Bryophyta `requiredItemIds` carrying consumables/gear. The domain confirms in every case that the
  items do **not** gate access (and that genuine gates — quests, 48/92 Slayer, Bryophyta's Mossy key —
  are correctly placed), but whether non-gating items belong in `requiredItemIds` vs `recommendedItemIds`
  is a plugin-schema-semantics decision the wiki cannot make. **Decide the meaning of `requiredItemIds`
  once** (hard access gate vs intended bring-list) and all four resolve together.

Net: 11 findings are cleared to proceed to a fix PR; 2 are retracted; 4 await a one-time schema ruling.
No source data was edited in this pass — verification only.
