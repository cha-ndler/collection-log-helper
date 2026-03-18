# Credits & Acknowledgments

## Referenced Plugins

### Quest Helper
- **Author:** Zoinkwiz
- **Repository:** https://github.com/Zoinkwiz/quest-helper
- **License:** BSD 2-Clause
- Referenced for: hint arrow patterns, minimap overlay techniques, world map navigation patterns, step-by-step guidance sequencing (completion conditions, auto-advance, step skipping)

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
- Referenced for: canonical collection log item ID verification (all 1,932 item IDs validated), EHB kill time alignment

### OSRS Wiki
- **Website:** https://oldschool.runescape.wiki
- Referenced for: drop rate verification, quest requirements, NPC IDs, monster coordinates, game mechanics research

### Bitterkoekje DPS Calculator
- **Repository:** https://github.com/weirdgloop/osrs-dps-calc
- **Spreadsheet:** https://docs.google.com/spreadsheets/d/1wBXIlvAmqoQpu5u9XBfD4B0PW7D8owyO_CnRDiTHBKQ
- Evaluated for: account-specific kill time calculations (not implemented — DPS ≠ effective kill time)
