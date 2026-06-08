# Data-verification findings - COMPLETENESS (TempleOSRS clog membership)

Bulk TempleOSRS canonical clog sweep across all 225 sources (id->name `items.php`,
source->item `categories.php`). This is the one check `cache_ids` cannot do: it finds clog
items that are **missing** from `drop_rates.json` entirely (a coverage hole the efficiency
calc can never route a player to). IDs of items we *do* carry are clean (0 wrong per cache_ids).

**Result: 0 wrong ids, 3 missing-item (completeness) findings - all `STANDS`, with receipts.**

### Giant Mole - clog items (completeness) - high
- check: TempleOSRS canonical clog sweep (2026-06-07), `temple_lookup` clog dimension
- ours: Giant Mole lists 3 clog items - Baby mole (12646), Mole claw (7416), Mole skin (7418)
- authoritative: TempleOSRS `giant_mole` lists 4 -> `[12646, 7418, 7416, 33382]`. Receipt
  (items.php): `33382 -> "Immaculate mole skin"`. Item is absent from our entire dataset.
- action: add Immaculate mole skin (id 33382) to the Giant Mole source. Giant Mole is its
  only obtainable source, so this is a true coverage hole (efficiency calc can never route
  the player here for it).
- status: open

### Lost Schematics - clog items (completeness) - high
- check: TempleOSRS canonical clog sweep (2026-06-07), `temple_lookup` clog dimension
- ours: Lost Schematics lists 11 schematics (32401-32410, 33143)
- authoritative: TempleOSRS `lost_schematics` lists 12; we are missing one. Receipt
  (items.php): `33423 -> "Bosun's workbench schematic"`; categories.php `lost_schematics`
  contains 33423. Item is absent from our entire dataset.
- action: add Bosun's workbench schematic (id 33423) to the Lost Schematics source.
- status: resolved 2026-06-07 (added Bosun's workbench schematic 33423 matching the other 11 schematics' convention - dropRate 1.0, pointCost 1, independent true; id cache-confirmed; `lintDropRates build` green)

### Slayer (Mystic (dark) set) - clog items (completeness) - high
- check: TempleOSRS canonical clog sweep (2026-06-07), `temple_lookup` clog dimension
- ours: we list 4 of the 5 Mystic (dark) pieces, split across slayer sources -
  Mystic hat (dark) 4099 + Mystic boots (dark) 4107 (Infernal Mage),
  Mystic robe bottom (dark) 4103 (Aberrant Spectre), Mystic robe top (dark) 4101 (Gargoyle).
  Mystic gloves (dark) is on no source.
- authoritative: TempleOSRS clog item `4105 -> "Mystic gloves (dark)"` (items.php); present in
  the `slayer` category set. Item is absent from our entire dataset - the lone missing piece
  of a set we otherwise carry.
- action: add Mystic gloves (dark) (id 4105). Placement is a judgment call (which slayer
  monster's drop table) - the per-source loop should decide; the existing set pieces sit on
  Infernal Mage / Aberrant Spectre / Gargoyle.
- status: open

