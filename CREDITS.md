# Credits & Acknowledgments

## Referenced Plugins

### Quest Helper
- **Author:** Zoinkwiz
- **Repository:** https://github.com/Zoinkwiz/quest-helper
- **License:** BSD 2-Clause
- Referenced for: hint arrow patterns, minimap overlay techniques, world map navigation patterns, step-by-step guidance sequencing (completion conditions, auto-advance, step skipping), conditional step branching (ConditionalStep pattern), NPC highlight styles (hull/outline/tile rendering), widget highlighting overlay patterns, zone-based area detection, pre-flight requirement check UX, item requirement sprite display

### Shortest Path
- **Author:** Skretzo
- **Repository:** https://github.com/Skretzo/shortest-path
- **License:** BSD 2-Clause
- Referenced for: PluginMessage API integration pattern for inter-plugin pathfinding

### Object Indicators
- **Author:** RuneLite team
- **Repository:** https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/objectindicators
- Referenced for: game object highlighting overlay patterns

### Inventory Tags
- **Author:** RuneLite team
- **Repository:** https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/inventorytags
- Referenced for: inventory item highlighting overlay patterns

## Community Data Sources

### Log Hunters
- **Community:** Log Hunters Discord
- **Invite:** https://discord.gg/loghunters
- **Resource:** Log Adviser spreadsheet (v1.3.4 by Main Mukkor)
- Referenced for: TempleOSRS EHB kill time alignment, independent drop table modeling, sequential drop dependencies (requiresPrevious), main vs ironman completion rates, identification of missing skilling pet sources, raid variant data, full item-by-item drop rate audit, SHOP/MIXED pointsPerHour corrections, and Miscellaneous source extraction

### C Engineer: Completed
- **Author:** m0bilebtw
- **Repository:** https://github.com/m0bilebtw/c-engineer-completed
- Referenced for: collection log notification setting detection (VarbitID.OPTION_COLLECTION_NEW_ITEM check to warn when chat notifications are disabled)

## External Data Sources

### TempleOSRS
- **Website:** https://templeosrs.com
- **API Docs:** https://templeosrs.com/api_doc.php
- Referenced for: canonical collection log item ID verification (all 2,101 item IDs validated across 224 sources), EHB kill time alignment, collection log category verification

### OSRS Wiki
- **Website:** https://oldschool.runescape.wiki
- Referenced for: drop rate verification (all 224 sources audited), quest requirements, NPC IDs, world coordinates, game mechanics research, raid loot mechanics (CoX points, ToB team scaling, ToA raid level formula), slayer task weights (4 masters verified), DT2 Awakened boss rates, wilderness boss variant rates

### Bitterkoekje DPS Calculator
- **Repository:** https://github.com/weirdgloop/osrs-dps-calc
- **Spreadsheet:** https://docs.google.com/spreadsheets/d/1wBXIlvAmqoQpu5u9XBfD4B0PW7D8owyO_CnRDiTHBKQ
- Evaluated for: account-specific kill time calculations (not implemented — DPS ≠ effective kill time)
