# drop_rates.json audit -- SLAYER

Total sources audited: **45**

| Bucket | Count |
|---|---|
| verified | 41 |
| flagged-fixable | 0 |
| flagged-research | 4 |
| error | 0 |

## Findings

| Source | Category | Check | Field | Current | Suggested | Severity | Bucket | Note |
|---|---|---|---|---|---|---|---|---|
| Frost Nagua | SLAYER | coord | worldX/worldY | (1693, 3232) | ~(1362, 4511) [Ruins of Tapoyauik] | high | flagged-research | coords are 1610 tiles from canonical Ruins of Tapoyauik; tolerance is 50. Could be intentional (e.g. lobby vs. entrance) but verify against the Wiki. |
| Sulphur Nagua | SLAYER | coord | worldX/worldY | (1440, 9602) | ~(1435, 3131) [Cam Torum] | high | flagged-research | coords are 6476 tiles from canonical Cam Torum; tolerance is 50. Could be intentional (e.g. lobby vs. entrance) but verify against the Wiki. |
| Drake | SLAYER | coord | worldX/worldY | (1310, 10057) | ~(1311, 10170) [Karuulm Slayer Dungeon] | high | flagged-research | coords are 114 tiles from canonical Karuulm Slayer Dungeon; tolerance is 50. Could be intentional (e.g. lobby vs. entrance) but verify against the Wiki. |
| Superior Slayer Monster | SLAYER | npc-id | npcId+interactAction | both missing | set npcId + interactAction OR confirm 'instance lobby' style source | low | flagged-research | combat-style category but no npcId and no interactAction. May be intentional for raid lobbies but worth verifying. |

## Verified (41)

- Demonic gorillas
- Lizardman shaman
- Tormented Demons
- Crawling Hand
- Cave Crawler
- Rockslug
- Cockatrice
- Pyrefiend
- Mogre
- Basilisk
- Terror Dog
- Infernal Mage
- Brine Rat
- Earthen Nagua
- Bloodveld
- Gryphon
- Jelly
- Turoth
- Warped Creature
- Cave Horror
- Aberrant Spectre
- Basilisk Knight
- Wyrm
- Lava Strykewyrm
- Dust Devil
- Fossil Island Wyvern
- Kurask
- Skeletal Wyvern
- Gargoyle
- Custodian Stalker
- Aquanite
- Nechryael
- Spiritual Mage
- Spiritual Mage (Zarosian)
- Abyssal Demon
- Cave Kraken
- Dark Beast
- Araxyte
- Smoke Devil
- Hydra
- Vyrewatch Sentinel
