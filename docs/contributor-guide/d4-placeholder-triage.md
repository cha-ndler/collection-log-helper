# D4 placeholder / unobtainable source triage

> **Purpose**: resolve D4 scoping §6 Q2 before opening Batch 10. For each candidate
> source flagged as placeholder-shaped or possibly-unobtainable, this doc records the
> classification, rationale, and recommended action. Actual edits to `drop_rates.json`
> are deferred to the relevant batch PR or a dedicated cleanup PR.
>
> **Date**: 2026-05-25
> **Unblocks**: D4 Batch 10 authoring (sources classified (a) proceed; (b) are skipped
> until the blocking condition resolves; (c) go to a separate cleanup PR).

---

## 1. Classification key

| Code | Meaning | Action |
|------|---------|--------|
| **(a)** | Real, currently-obtainable content | Author in the relevant D4 batch. Notes column records guidance shape. |
| **(b)** | Placeholder for future / conditional content | Skip authoring for now. Leave a TODO marker in the batch plan row. |
| **(c)** | Not a real collection-log source, or structurally removable | Do NOT edit here. File a separate cleanup PR. |

---

## 2. Triage table

### Named candidates from §6 Q2

| Source | Category | Items (n) | Class | Rationale | Action |
|--------|----------|-----------|-------|-----------|--------|
| **Miscellaneous** | OTHER | 38 | **(a)** | Confirmed real collection-log tab on the Wiki. Houses drops that have no dedicated source page: Big fish (Big bass / swordfish / shark), Long bone / Curved bone, Giant key, Mossy key, Xeric's talisman, Evil chicken outfit pieces, Mining gloves set, Herbi, Merfolk trident, Dragon metal slice/lump/limb, Helmet of the moon, Broken zombie axe/helmet, Squid beak, and several skilling-adjacent uniques. All are currently obtainable in-game. The `travelTip` "Varies by activity" accurately reflects that no single travel step exists; guidance shape should be a MANUAL step explaining each item's primary source rather than a travel+kill flow. Authors at the long-tail bar (E1/E8/E9). | Include in Batch 10; use MANUAL step pattern per §6 Q5 analogy. |
| **My Notes** | OTHER | 26 | **(a)** | "My notes" is an in-game book (NPC ID 11339) obtained from Otto Godblessed during Barbarian Training. It records 26 Ancient page entries (IDs 11341-11366), all currently obtainable by rummaging Barbarian skeletons (1/13) or as monster drops in the Ancient Cavern. The travelTip "Barbarian Assault tele -> falls" is slightly misrouted (should reference the Ancient Cavern / Barbarian Outpost, not Barbarian Assault), but the content is real. Authors at the long-tail bar. | Include in Batch 10; correct travelTip to "Barbarian Outpost teleport -> Ancient Cavern" in the same PR. |
| **Port Tasks** | OTHER | 2 | **(a)** | Port tasks are a real Sailing training method (Wiki redirects to Sailing#Port_tasks). Sailing released 19 November 2025. Two items: Soup (31283, 1/4500 task reward) and Shark paint (32090, 1/36 salvage reward). Items are genuine and obtainable. Two items is sparse for a source, but both are real. Authors at the long-tail bar; the guidance shape is "board boat -> accept task from port notice board -> complete task." | Include in Batch 10 (or Batch 11 alongside other Sailing content for narrative cohesion; either is acceptable). |
| **Stronghold of Security** | OTHER | 4 | **(a)** | Real content. The four skull sceptre pieces (Right skull half 9007, Left skull half 9008, Top of sceptre 9010, Bottom of sceptre 9011) drop from monsters on each floor of the Stronghold of Security. The dungeon is permanently accessible (no one-time gating on the drops; sceptre reassembly is repeatable). The Wikis travelTip "Skull sceptre -> Barbarian Village" is ironic (you need the sceptre to fast-travel there, but the items are the components to build it); a better first-visit step is "Edgeville teleport -> run south to Barbarian Village mine entrance." Authors at the long-tail bar. | Include in Batch 10; fix travelTip in the same PR. |
| **Sailing Misc** | OTHER | 12 | **(a)** | Sailing launched 19 November 2025. "Sailing Misc" maps to the Collection Log tab of the same name covering miscellaneous Sailing drops: Dragon metal sheet, Dragon nails, Dragon cannonball, Echo pearl, Swift albatross feather, Narwhal horn, Ray barbs, Broken dragon hook, and others (12 total). All are real Sailing skill drops. No pre-release disclaimer applies. Authors at the long-tail bar; guidance shape is "Sailors' amulet -> port -> board boat -> sail." | Include in Batch 11 (Boat Combat + Sailing-adjacent). Sailing has released so the watch-item flag in §6 Q3 is lifted. |
| **Boat Paints** | OTHER | 11 | **(a)** | Real Sailing content. Boat paints (Barracuda, Shark, Inky, Angler's, Salvor's, Armadylean, Zamorakian, Guthixian, Saradominist, Bandosian, Elven -- 11 total) are cosmetic items obtained from Sailing salvage activities. Wiki redirects to Boat paint (real page). All obtainable since Sailing launch. Authors at the long-tail bar; guidance shape is "board boat -> salvage at sea -> sort salvage table." | Include in Batch 11. Watch-item flag lifted. |
| **Sea Treasures** | OTHER | 17 | **(a)** | Real Sailing content. Sea Treasures is a named Collection Log tab (Wiki redirects to Collection log#Sea Treasures). Contains 17 items: Medallion fragment 1-10 (milestone drops at fixed voyage counts), Ancient scale, Luminous coral, and others. All obtainable via Sailing sea treasure voyages. Authors at the long-tail bar; guidance shape is "board boat -> complete sea treasure voyages." | Include in Batch 11. Watch-item flag lifted. |
| **Ocean Encounters** | OTHER | 11 | **(a)** | Real Sailing content. Ocean Encounters is a named Collection Log tab covering pearl drops from ocean encounters while sailing: Tiny, Small, Shiny, Bright, Big, Huge, Enormous, Shimmering, Brilliant, Radiant, and Majestic pearls (11 total, all with milestoneKills). All obtainable since Sailing launch. Authors at the long-tail bar; guidance shape is "board boat -> sail -> engage ocean encounters." | Include in Batch 11. Watch-item flag lifted. |

### Additional placeholder-shaped sources found in scan

The following sources have 0-2 items but are NOT placeholder-shaped -- they are valid
single-unique or dual-unique sources that already exist in the collection log. No
classification action is needed; they proceed to their assigned D4 batch as normal.

| Source | Items (n) | Note |
|--------|-----------|------|
| Bryophyta | 1 | Single unique (Bryophyta's essence). Real D4 Batch 1 source. |
| Obor | 1 | Single unique (Hill giant club). Real D4 Batch 1 source. |
| Pyramid Plunder | 1 | Single unique (Pharaoh's sceptre). Real D4 Batch 10 source. |
| Zombie Pirate Locker | 1 | Single unique (Teleport anchoring scroll). Real D4 Batch 10 source. |
| Wilderness God Wars Dungeon | 1 | Single unique (Ecumenical key). Separate from GWD; real D4 source. |
| The Mimic | 1 | Single unique (Ring of 3rd age). Real D4 Batch 12 source. |
| All low-pop SLAYER single-unique sources | 1 each | (Crawling Hand, Cave Crawler, Rockslug, Pyrefiend, Basilisk, Brine Rat, Bloodveld, Jelly, Aberrant Spectre, Nechryael, Smoke Devil, Dust Devil, Cave Horror, Araxyte, Dark Beast, Spiritual Mage, etc.) Real D4 Batch 5/6/7 sources. |
| Port Tasks | 2 | See named-candidates row above -- classified (a). |

No sources in the file have 0 items. No sources carry a literal "Placeholder" item name.
No sources were found that are unambiguously unobtainable or retired.

---

## 3. Summary counts

| Classification | Count | Sources |
|----------------|-------|---------|
| **(a) Real, author in batch** | 8 | Miscellaneous, My Notes, Port Tasks, Stronghold of Security, Sailing Misc, Boat Paints, Sea Treasures, Ocean Encounters |
| **(b) Placeholder / future** | 0 | -- |
| **(c) Removable / cleanup** | 0 | -- |

**All 8 named §6 Q2 candidates classify as (a).** No sources were found that warrant
deferral or a cleanup PR from this triage pass.

**Key finding**: Sailing launched 19 November 2025, so the Q3 watch-item flag on
Batch 11 (Boat Combat + Sailing-adjacent content) is resolved. Sailing Misc, Boat
Paints, Sea Treasures, and Ocean Encounters are live content and can be authored in
Batch 11 without any further precondition.

---

## 4. Batch 10 scope (unblocked sources)

The following §6 Q2 sources are assigned to Batch 10 (per the existing batching plan):

| Source | Items (n) | Guidance shape | Bar |
|--------|-----------|----------------|-----|
| Miscellaneous | 38 | MANUAL step per item group explaining primary source | Long-tail |
| My Notes | 26 | Travel: Barbarian Outpost -> Ancient Cavern; MANUAL rummage/drop loop | Long-tail |
| Stronghold of Security | 4 | Travel: Edgeville -> Barbarian Village mine; floor-by-floor kill loop | Long-tail |
| Pyramid Plunder | 1 | Travel: Jaleustrophos teleport; interact loop inside pyramid | Long-tail |
| (Port Tasks) | 2 | (May slot into Batch 11 for Sailing cohesion; see above) | Long-tail |

Batch 10 also contains the other ~15 sources listed in the scoping doc §4 row (rare-table
chests, Revenants, Champion's Challenge, Camdozaal, Forestry, Hunter Guild, TzHaar,
Catacombs of Kourend, Shayzien Armour, etc.) -- those are unaffected by this triage.

---

## 5. Batch 11 scope update (Sailing watch-item resolved)

Batch 11 in the scoping doc carried a "defer if Sailing has not released" conditional.
Sailing released 19 November 2025. Batch 11 is fully unblocked:

| Source | Items (n) | Bar |
|--------|-----------|-----|
| Albatross (Boat Combat) | 1 | Long-tail |
| Great White Shark (Boat Combat) | 1 | Long-tail |
| Narwhal (Boat Combat) | 1 | Long-tail |
| Orcas (Boat Combat) | 1 | Long-tail |
| Vampyre Kraken (Boat Combat) | 1 | Long-tail |
| Stingray (Boat Combat) | 1 | Long-tail |
| Sailing Misc | 12 | Long-tail |
| Boat Paints | 11 | Long-tail |
| Sea Treasures | 17 | Long-tail |
| Ocean Encounters | 11 | Long-tail |

---

## 6. References

- [D4 long-tail scoping §6 Q2](d4-long-tail-scoping.md#q2----triage-of-placeholder--unobtainable-sources)
- [D4 long-tail scoping §6 Q3](d4-long-tail-scoping.md#q3----sailing-locked-content-boat-combat-5-variants--ocean-encounters--sailing-misc--sea-treasures--boat-paints)
- [D4 long-tail scoping §4 Batch 10](d4-long-tail-scoping.md#4-batching-plan)
- [D4 long-tail scoping §4 Batch 11](d4-long-tail-scoping.md#4-batching-plan)
- [ROADMAP Tier D status log](../ROADMAP.md#tier-d----coverage-breadth)
