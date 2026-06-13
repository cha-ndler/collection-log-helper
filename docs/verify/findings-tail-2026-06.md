# Tail audit findings (sources 151-226) — 2026-06-13

Phase 3 semantic audit of the never-audited source tail. Pipeline: sonnet wiki-verify (cite-or-discard) -> opus domain-skeptic refutation. Only CONFIRMED findings recorded. One PR per source.

## Tranche 1 (pilot, sources 151-160)

## Catacombs of Kourend (source #151)

### [high] Skeletal wyverns are listed as a popular Slayer task inside the Catacombs of Kourend.

- **Field:** step 4
- **Data says:** Skeletal wyverns are listed as a popular Slayer task inside the Catacombs of Kourend.
- **Wiki says (raw):** They are found at the end of the Asgarnian Ice Dungeon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skeletal_Wyvern
- **Suggested fix:** Remove 'skeletal wyverns' from the list of popular Slayer tasks in the Catacombs. Skeletal wyverns are located in the Asgarnian Ice Dungeon, not the Catacombs of Kourend.
- **Skeptic receipt:** drop_rates.json line 20864 (Catacombs of Kourend, guidanceSteps[3], section "Combat"): "Kill monsters in the Catacombs for dark totem pieces (base, middle, top - each 1/500 from any monster). Burying bones restores Prayer. Popular Slayer tasks here include abyssal demons, nechryaels, dark beasts, and skeletal wyverns. Combine Slayer trips with totem piece hunting for efficient farming."  Wiki — Skeletal Wyvern (wiki_lookup raw infobox): "Combat level: 140 / Slayer level: 72 / Slayer category: Skeletal Wyverns / Assigned by: chaeldar,konar,nieve,duradel" Wiki — Skeletal_Wyvern page (verbatim): "found at the end of the Asgarnian Ice Dungeon ... a shortcut at the ladder to the Ice Dungeon leading to the skeletal wyverns requiring level 82 Agility."  Wiki — Catacombs of Kourend complete monster list (verbatim, by section): Demon's Run: Abyssal demon, Black demon, Ghost, Greater demon, Greater nechryael, Mutated Bloodveld, Lesser demon Dragon's Den: Bronze dragon, Brutal black dragon, Brutal blue dragon, Brutal red dragon, Fire giant, Hellhound, Iron dragon, Steel dragon Reeking Cove: Ankou, Cyclops, Deviant spectre, Ghost, Hill Giant, Magic axe, Moss giant, Mutated Bloodveld, Possessed pickaxe, Shade, Skeleton, Twisted Banshee The Shallows: Cyclops, Dagannoth, Dust devil, King Sand Crab, Mutated Bloodveld, Moss giant, Warped Jelly (No skeletal wyverns in any section.)  Note (separate stale sub-error, not part of this finding's claim): "dark beasts" in the same list were removed from the Catacombs on 2019-07-25 and moved to the Iorwerth Dungeon.
- **Skeptic reasoning:** Survives every refutation vector. (1) Not a multi-source clog-id artifact — this is guidance prose, not an itemId shared across sources. (2) No variant question — the disputed dark-totem item IDs aren't touched. (3) Not account-type/progress dependent: the wiki's full Catacombs monster list (Demon's Run, Dragon's Den, Reeking Cove, The Shallows) contains no wyverns for any account. (4) Canonical clog id not in dispute. (5) Staleness cuts in favor of the finding — the same sentence also lists "dark beasts," removed from the Catacombs in 2019, so the line reflects an outdated game state. (6) Requirement nuance: the Skeletal Wyvern wiki page is explicit that they sit at the end of the Asgarnian Ice Dungeon (Slayer level 72, category "Skeletal Wyverns"), with no Catacombs spawn. The data genuinely contradicts the wiki and the contradiction is backed by raw authoritative quotes, so the finding CONFIRMS. Suggested fix (remove "skeletal wyverns" from the Catacombs popular-Slayer-task list) is correct; the "dark beasts" entry in the same sentence is an additional latent error worth flagging when the fix is applied.

### [high] Dark beasts are listed as a popular Slayer task inside the Catacombs of Kourend.

- **Field:** step 4
- **Data says:** Dark beasts are listed as a popular Slayer task inside the Catacombs of Kourend.
- **Wiki says (raw):** removed from Catacombs of Kourend for thematic reasons
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dark_beast
- **Suggested fix:** Remove 'dark beasts' from the list of popular Slayer tasks in the Catacombs. Dark beasts were removed from the Catacombs of Kourend in 2019 and now only spawn in the Mourner Tunnels and Iorwerth Dungeon.
- **Skeptic receipt:** OSRS Wiki, Dark beast page (raw source, https://oldschool.runescape.wiki/w/Dark_beast?action=raw): "Dark beasts were added to [[Iorwerth Dungeon]] and removed from [[Catacombs of Kourend|Kourend Catacombs]] for thematic reasons." "Dark beasts are monsters that require level 90 [[Slayer]] to kill, and are found in the [[Mourner Tunnels]] as well as in [[Iorwerth Dungeon]] in [[Prifddinas]]."  OSRS Wiki, Catacombs of Kourend monster list — dark beast does NOT appear in any section (Demon's Run, Dragon's Den, Reeking Cove, The Shallows).  Our data (drop_rates.json line 20864, source "Catacombs of Kourend", guidance step 4, section "Combat"): "Kill monsters in the Catacombs for dark totem pieces (base, middle, top - each 1/500 from any monster). Burying bones restores Prayer. Popular Slayer tasks here include abyssal demons, nechryaels, dark beasts, and skeletal wyverns. Combine Slayer trips with totem piece hunting for efficient farming."
- **Skeptic reasoning:** Verified our data first: line 20864 of drop_rates.json (source "Catacombs of Kourend", guidance step 4, section "Combat") explicitly lists "dark beasts" among popular Slayer tasks in the Catacombs. The proposed finding accurately describes our data.  Verified the claim against three independent OSRS wiki fetches, including the raw page source: dark beasts were removed from the Catacombs of Kourend and added to Iorwerth Dungeon for thematic reasons (July 25, 2019, Song of the Elves), and now spawn only in the Mourner Tunnels and Iorwerth Dungeon. The Catacombs of Kourend monster list does not include dark beasts.  Refutation pass (all vectors fail to refute): (1) Not a multi-source clog-id artifact — the finding concerns an in-guidance Slayer-task example, not an itemId across sources; the totem piece ids (19679/19681/19683) are untouched and correct. (2) No item variant involved. (3) Not account-type/progress dependent — dark beasts do not spawn in the Catacombs for any account; Iorwerth access gating does not make them Catacombs-obtainable. (4) Canonical clog ids unaffected. (5) This is genuine staleness/drift — our text reflects a pre-2019 game state that now contradicts current mechanics, which is exactly the kind of user-facing error worth fixing. (6) Wiki confirms the spawn locations explicitly.  The finding survives every refutation vector and is backed by a verbatim authoritative quote. The guidance would mislead a player into hunting dark beasts in the wrong dungeon. Scope note: the same step also lists "skeletal wyverns," which likewise do not appear in the current Catacombs monster list, but this finding is scoped to dark beasts only. Severity high is appropriate for incorrect routing in user-facing guidance, though it is non-blocking for clog tracking (the totem-piece data itself is correct).

## Wilderness God Wars Dungeon (source #152)

### [high] From Dareeyak Teleport landing spot, run south to reach the Wilderness GWD

- **Field:** travelTip
- **Data says:** From Dareeyak Teleport landing spot, run south to reach the Wilderness GWD
- **Wiki says (raw):** Dareeyak Teleport is a spell in the Ancient Magicks spellbook, requiring a Magic level of 78 to cast. It teleports the caster to the ruins south of The Forgotten Cemetery in level 23 Wilderness.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dareeyak_Teleport
- **Suggested fix:** Change 'run south' to 'run north-east' — Dareeyak lands south of The Forgotten Cemetery at level 23 Wilderness, and the GWD entrance is east of the cemetery at level 28, so you must run north-east (deeper into the Wilderness) to reach it. Running south takes you away from the dungeon.
- **Skeptic receipt:** RAW OSRS Wiki (Crazy archaeologist, action=raw) — Dareeyak Teleport landing / Ruins (west): "Coordinates: x:2977, y:3702 ... The boss resides in the Ruins (west), situated south of The Forgotten Cemetery in level 23 Wilderness." (Dareeyak Teleport lands here.)  RAW OSRS Wiki (Wilderness God Wars Dungeon, action=raw) — entrance + access: "The map frame shows: x=3016.5, y=3739.5 (dungeon entrance)" "*Casting the [[Dareeyak Teleport]] spell on the [[Ancient spellbook]] or breaking a [[Dareeyak teleport|Dareeyak teleport tablet]] to the [[Ruins (west)|ruins]] at level 23 Wilderness. Both require completion of [[Desert Treasure I]] to use."  OSRS Wiki (The Forgotten Cemetery) relative map: North = King Black Dragon Lair; West = God Wars Dungeon; East = Wilderness God Wars Dungeon; South = Ruins (west). Dareeyak lands at the Ruins (south of cemetery); the Wilderness GWD is east of the cemetery.  coordinate_helper distance (2977,3702)->(3017,3740): "Euclidean: 55.2 tiles / Chebyshev (game movement): 40 tiles". Displacement = +40 X (EAST), +38 Y (NORTH) => run NORTH-EAST.
- **Skeptic reasoning:** Our data field travelTip reads "Cemetery teleport (Arceuus) -> run east; or Dareeyak teleport (Desert Treasure I) -> run south". The Dareeyak segment's cardinal direction is verifiably wrong.  Authoritative coordinates (quoted raw from the OSRS wiki): - Dareeyak Teleport landing / Ruins (west) where Crazy archaeologist spawns: x:2977, y:3702. - Wilderness GWD entrance: x:3016.5, y:3739.5. Delta is +39.5 east and +37.5 north (coordinate_helper confirms ~40 east / 38 north). In OSRS, higher Y is north; running "south" decreases Y and moves directly AWAY from the dungeon. The correct heading is north-east, exactly as the suggested fix states. This is also corroborated by the Forgotten Cemetery map (ruins are south of the cemetery; the Wilderness GWD is east of the cemetery, so ruins->GWD is north-east).  Refutation vectors all fail to save the entry: this is a free-text travelTip, not an itemId or requirement gate, so the multi-source-item, item-variant, canonical-clog-id and account-type vectors do not apply. The geography is account-independent (same for main/ironman/GIM/UIM) and progress-independent, so it is a flat error rather than a per-account note. The Wilderness GWD location and Dareeyak destination are long-standing with no recent geography change, so it is not staleness/drift. The companion Cemetery-teleport-then-east route in the same tip is correct, which isolates the bug to the Dareeyak direction.  Severity high is appropriate: the tip would send a player south, deeper toward the Ruins/Crazy archaeologist and away from the GWD entrance, defeating the travel guidance. Suggested fix "run north-east" matches the coordinate delta.

## Miscellaneous (source #154)

### [high] Merfolk trident drops from / is obtained via Tempoross

- **Field:** step 1
- **Data says:** Merfolk trident drops from / is obtained via Tempoross
- **Wiki says (raw):** A merfolk trident can be bought from Mairin's Market in the underwater area of Fossil Island for 400 mermaid's tears obtained from Underwater Agility and Thieving.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Merfolk_trident
- **Suggested fix:** Replace 'Merfolk trident (Tempoross)' with 'Merfolk trident (Mairin's Market, Fossil Island underwater area — costs 400 mermaid's tears from Underwater Agility/Thieving)'
- **Skeptic receipt:** wiki_lookup "Merfolk trident" (oldschool.runescape.wiki/w/Merfolk_trident), Infobox NPC IDs: 21649: "A merfolk trident can be bought from Mairin's Market in the underwater area of Fossil Island for 400 mermaid's tears obtained from Underwater Agility and Thieving."  drop_rates.json line 21344 (Miscellaneous, guidanceSteps[0].description): "...Herbi (Herbiboar), Merfolk trident (Tempoross), Dragon metal pieces (Vorkath)..."  temple_lookup clog_categories: "tempoross": [25602, 25559, 25592, 25594, 25596, 25598, 25576, 25578, 25580, 25582, 21028, 25588] — itemId 21649 (Merfolk trident) is NOT present in the Tempoross collection-log membership.
- **Skeptic reasoning:** The data's guidance step text attributes the Merfolk trident to "Tempoross". The authoritative OSRS wiki states unambiguously that it is purchased from Mairin's Market in the Fossil Island underwater area for 400 mermaid's tears (Underwater Agility/Thieving) — a shop purchase, not a Tempoross reward.  Refutation vectors all fail to save the finding: 1. Multi-source: itemId 21649 appears only under "Miscellaneous" in our data and is NOT in TempleOSRS's Tempoross clog list — so this is not a legitimate multi-source attribution; "Tempoross" is simply wrong. 2. Variant: same id 21649 confirmed by both wiki infobox (NPC IDs: 21649) and our data — no variant mismatch. 3. Account-type/progress: the acquisition method is identical for all account types; not account-specific. 4. Canonical clog id: the itemId (21649) is correct; the error is the source-attribution text only. 5. Staleness: wiki text is current and explicit. 6. Requirement nuance: wiki gives a single, explicit acquisition path contradicting "Tempoross".  The Tempoross wiki page corroborates: it lists no Merfolk trident among requirements or rewards; the trident is purely a Fossil Island underwater shop item. The guidance string is a genuine factual error and should read e.g. "Merfolk trident (Mairin's Market, Fossil Island underwater — 400 mermaid's tears from Underwater Agility/Thieving)". Note: this is a guidance-text correction only; the itemId, dropRate, and clog membership are unaffected. Relevant file: C:\Users\Chandler\Documents\Projects\collection-log-helper\src\main\resources\com\collectionloghelper\drop_rates.json line 21344.

### [high] Miscellaneous source, guidance step 1: prose says "Dragon metal pieces (Vorkath)" — but dragon metal slice/lump do not come from Vorkath.

- **Field:** step 1
- **Data says:** Dragon metal pieces drop from Vorkath
- **Wiki says (raw):** The dragon metal slice is an item dropped exclusively by adamant dragons. ... Dragon metal lumps are dropped by rune dragons. ... A dragon metal shard is an item that can be purchased from the Myths' Guild Armoury for 1,800,000 coins, which requires the completion of Dragon Slayer II to access.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dragon_metal_slice
- **Suggested fix:** Replace 'Dragon metal pieces (Vorkath)' with 'Dragon metal slice (Adamant Dragons), Dragon metal lump (Rune Dragons), Dragon metal shard (Myths' Guild Armoury — requires Dragon Slayer II)'
- **Skeptic receipt:** Wiki (Dragon_metal_slice): "The dragon metal slice is an item dropped exclusively by adamant dragons." NPC/item IDs: 22100. Category:Collection log items. Wiki (Dragon_metal_lump): "Dragon metal lumps are dropped by rune dragons." NPC/item IDs: 22103. Category:Collection log items. Wiki (Vorkath) full 47-entry drop table contains NO dragon metal item of any kind (no slice/lump/sheet/shard). Notable rares: Vorkath's head, Dragonbone necklace, Jar of decay, Vorki, Draconic visage, Skeletal visage. TempleOSRS clog_categories vorkath = [21992, 21907, 11286, 22006, 22106, 22111] — does not include 22100 or 22103. drop_rates.json line 21344 prose: "...Merfolk trident (Tempoross), Dragon metal pieces (Vorkath). Check each item's Wiki page for its specific drop source." NOTE on suggested fix: Wiki (Dragon_metal_shard) id 22097 is Category:Upgrade items ONLY — NOT a collection log item — so the finding's suggested "Dragon metal shard (Myths' Guild Armoury)" addition must NOT be wired as a clog item. Correct attribution is only: Dragon metal slice -> adamant dragons; Dragon metal lump -> rune dragons.
- **Skeptic reasoning:** The guidance prose attributes "Dragon metal pieces" to Vorkath. The wiki authoritatively states the slice is dropped exclusively by adamant dragons and the lump by rune dragons, and Vorkath's complete drop table plus its TempleOSRS clog list contain no dragon metal item. The error survives all refutation vectors: the underlying clog item IDs (22100, 22103) are correct and present, so this is not a dedup/variant/account-type artifact — it is a genuine wrong source attribution in user-facing prose that would misdirect a player to the wrong content. The finding's premise is therefore CONFIRMED against a raw authoritative quote. Two caveats reduce its scope versus the original report: (1) severity is overstated as "high" — no itemId, drop rate, or requirement the plugin acts on is wrong; this is a prose-correctness issue (low). (2) The suggested fix over-reaches by adding "Dragon metal shard (Myths' Guild Armoury)"; the wiki shows the shard (22097) is an Upgrade item, not a collection-log item, so it must not be wired as a clog source. The correct fix is to replace "Dragon metal pieces (Vorkath)" with "Dragon metal slice (Adamant Dragons), Dragon metal lump (Rune Dragons)" only.

## Camdozaal (source #156)

### [high] Camdozaal guidance step 1 says "use amulet of glory to Edgeville and run east to Ice Mountain" — direction should be west, not east.

- **Field:** step 1
- **Data says:** From Edgeville, run east to Ice Mountain
- **Wiki says (raw):** Walking west from Edgeville or Barbarian Village.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ruins_of_Camdozaal
- **Suggested fix:** Change 'run east' to 'run west' — Ice Mountain is west of Edgeville, not east.
- **Skeptic receipt:** drop_rates.json (line 21679): "...Alternatively, use amulet of glory to Edgeville and run east to Ice Mountain." and travelTip (line 21685): "...or glory -> Edgeville -> run east".  OSRS Wiki, Ruins of Camdozaal (wiki_lookup raw): - Infobox: "Location: Below Ice Mountain, Asgarnia" - "It is located beneath Ice Mountain, accessed through the entrance on the western side of the mountain after completion of the quest Below Ice Mountain." - Access list: "*Walking west from Edgeville or Barbarian Village." - "Once players have reached Ice Mountain, they can walk west around the mountain (past the entrance to the Dwarven Mine) and then north until they reach the entrance."  Corroborating internal evidence: the step's own travel target is worldX 3012 (line 21680), west (lower X) of the Edgeville glory-teleport area (~worldX 3087).
- **Skeptic reasoning:** Ran the full refutation pass. This is not a multi-source clog artifact, not a variant-id issue, not account-type/progress-dependent, and not a temple clog-id question — it is a directional travel-guidance fact, checkable against the wiki. The OSRS Wiki states three times (infobox location, prose "entrance on the western side", and the access list "Walking west from Edgeville") that Ice Mountain / the Camdozaal entrance lies WEST of Edgeville. Our data step says "run east to Ice Mountain" from Edgeville, and repeats "run east" in the travelTip. The data's own ARRIVE_AT_TILE target (worldX 3012) is lower-X than Edgeville (~3087), independently confirming the entrance is west, so "east" is genuinely wrong. The proposed finding's quote of our data is a light paraphrase ("From Edgeville, run east" vs the literal "use amulet of glory to Edgeville and run east to Ice Mountain"), but the load-bearing error — the word "east" — is verbatim present in our data and contradicted by an authoritative raw quote. Severity is realistically low rather than high: completion is coordinate-driven (worldX 3012), so the engine still routes the player correctly; only the human-readable prose hint misdirects. But it is a genuine factual error backed by a raw wiki quote, so it survives as CONFIRMED.

### [medium] Lassar Teleport lands near Ice Mountain; run south to the entrance

- **Field:** step 1
- **Data says:** Lassar Teleport lands near Ice Mountain; run south to the entrance
- **Wiki says (raw):** Casting Lassar Teleport from Ancient Magicks to teleport to the top of Ice Mountain. Level 68 Agility is required to use the shortcut down the mountain which leads directly to the entrance to Camdozaal.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ruins_of_Camdozaal
- **Suggested fix:** Lassar Teleport lands at the top of Ice Mountain, not to the south of the entrance. The direction 'south' is incorrect — from the summit you descend westward to reach the entrance on the western side. Reword to: 'Cast Lassar Teleport (Ancient Magicks) to the top of Ice Mountain, then make your way down and west to the entrance on the western side of the mountain (Level 68 Agility shortcut available).'
- **Skeptic receipt:** OSRS Wiki — Ruins of Camdozaal (https://oldschool.runescape.wiki/w/Ruins_of_Camdozaal), Transportation section, verbatim: "Casting Lassar Teleport from Ancient Magicks to teleport to the top of Ice Mountain. Level 68 Agility is required to use the shortcut down the mountain which leads directly to the entrance to Camdozaal." "Once players have reached Ice Mountain, they can walk west around the mountain (past the entrance to the Dwarven Mine) and then north until they reach the entrance."  drop_rates.json (Camdozaal source, guidanceSteps[0], line 21679), verbatim: "Travel to the Ruins of Camdozaal beneath Ice Mountain. Cast Lassar Teleport (Ancient Magicks, requires Desert Treasure I), then run south to the Ice Mountain entrance. Alternatively, use amulet of glory to Edgeville and run east to Ice Mountain."  coordinate_helper identify (3012, 3451, plane 0) — the step's target tile: "Within known areas: Falador — 85 tiles from center." The target worldY 3451 sits SOUTH of the Ice Mountain summit (~3475), confirming the step routes the player away from the entrance, which the wiki places to the west-then-north of the landing.
- **Skeptic reasoning:** Ran the refutation pass. Not a multi-source/variant artifact (this is travel prose, not an itemId; the Lassar matches at lines 2019-2767 are the separate Duke Sucellus / Lassar Undercity DT2 source and are unrelated). Not account-type or progress dependent — the directional routing is the same for every account. Not stale — Camdozaal/Ice Mountain geography is unchanged. The authoritative wiki text directly contradicts "run south": Lassar Teleport lands at the TOP of Ice Mountain, and the entrance is reached by going west around the mountain then north — i.e. north-west of the landing, never south. The step's own target tile (worldY 3451) is south of the summit (~3475), so both the prose and the coordinate send the player the wrong way. The finding's core correction is wiki-supported and STANDS at medium severity. One human-ratification nuance: the suggested rewording says "down and west / western side"; the wiki's precise phrasing is "west around the mountain... and then north," so the entrance is more accurately north-west, and the 68 Agility shortcut detail in the rewording is wiki-confirmed. The directional error itself (south is wrong) is confirmed.

## Colossal Wyrm Agility (source #157)

### [high] Teleport scroll is purchased from Telia

- **Field:** travelTip
- **Data says:** Teleport scroll is purchased from Telia
- **Wiki says (raw):** Use a Colossal wyrm teleport scroll, bought from Worm Tongue or the Grand Exchange. This is the quickest method to get there.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Colossal_Wyrm_Agility_Course
- **Suggested fix:** Replace 'Telia' with 'Worm Tongue'. The scroll is purchased from Worm Tongue (for 40 termites) or the Grand Exchange, not from an NPC named Telia.
- **Skeptic receipt:** drop_rates.json line 21805 (travelTip): "Quetzal transport whistle -> Civitas illa Fortis, run south-west to Avium Savannah; or Colossal wyrm teleport scroll (purchased from Telia)"  OSRS Wiki (Colossal_wyrm_teleport_scroll), wiki_lookup raw: "The Colossal Wyrm teleport scroll are consumable teleport scrolls that teleport the player to the Colossal Wyrm Remains. They can be purchased from Worm Tongue's Wares for 40 termites obtained from the Colossal Wyrm Agility Course."  OSRS Wiki (Colossal_Wyrm_Agility_Course): "Use a Colossal wyrm teleport scroll, bought from Worm Tongue or the Grand Exchange."  npc_lookup "Telia": "abextm cache: no NPC named \"Telia\" (check spelling/variant). NPC \"Telia\" not found on OSRS Wiki."  npc_lookup "Worm Tongue": "abextm cache (source of truth) NPC ids for \"Worm Tongue\": 13981, 13982 ... Location: Colossal Wyrm Remains".
- **Skeptic reasoning:** The travelTip field at line 21805 of drop_rates.json literally states the Colossal wyrm teleport scroll is "purchased from Telia," so the finding's "data says" portion is accurate (not a paraphrase). Two independent authoritative wiki receipts (the scroll page and the course page) both name Worm Tongue's Wares as the seller for 40 termites; neither mentions Telia. Crucially, npc_lookup confirms "Telia" is not a real NPC in the abextm cache OR on the wiki, while "Worm Tongue" is a confirmed cache NPC (id 13982) at the Colossal Wyrm Remains. So "Telia" is a fabricated NPC name, not a legitimate variant, multi-source artifact, account-type nuance, or stale game version — it fails every refutation vector. The data genuinely contradicts the wiki and the suggested fix (Telia -> Worm Tongue) is correct. NOTE for the fixer: the same fabricated name "Telia" also appears in guidanceSteps line 21829 ("Spend marks at Telia for Varlamore graceful pieces and Calcified acorns"); that occurrence is outside the scope of this travelTip finding but is the same underlying error (Worm Tongue's Wares also vends the graceful/acorn rewards) and should be corrected in the same edit.

### [low] Teleport scroll is purchased from Telia (per travelTip context carried into step 1)

- **Field:** step 1
- **Data says:** Teleport scroll is purchased from Telia (per travelTip context carried into step 1)
- **Wiki says (raw):** Use a Colossal wyrm teleport scroll, bought from Worm Tongue or the Grand Exchange. This is the quickest method to get there.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Colossal_Wyrm_Agility_Course
- **Suggested fix:** The step itself does not name the seller, but the travelTip it echoes names 'Telia' — fix the travelTip seller reference to 'Worm Tongue'.
- **Skeptic receipt:** wiki_lookup (Colossal wyrm teleport scroll), RAW: "The Colossal Wyrm teleport scroll are consumable teleport scrolls that teleport the player to the Colossal Wyrm Remains. They can be purchased from Worm Tongue's Wares for 40 termites obtained from the Colossal Wyrm Agility Course."  WebFetch (Worm_Tongue's_Wares), RAW: "Shopkeeper: The shop is run by Worm Tongue, who can be found in the centre of the agility course. Items Sold: ... Colossal wyrm teleport scroll (40 Termites) ... Yes, the shop does sell the Colossal wyrm teleport scroll for 40 Termites."  drop_rates.json line 21805 (RAW): "travelTip": "Quetzal transport whistle -> Civitas illa Fortis, run south-west to Avium Savannah; or Colossal wyrm teleport scroll (purchased from Telia)"
- **Skeptic reasoning:** The source-level travelTip at drop_rates.json line 21805 says the Colossal wyrm teleport scroll is "purchased from Telia". Two authoritative wiki sources agree this is wrong: wiki_lookup states the scroll is "purchased from Worm Tongue's Wares for 40 termites", and the Worm Tongue's Wares shop page confirms the shopkeeper is Worm Tongue (in the centre of the agility course) and the scroll is stocked there for 40 Termites. The GE is the only other source. Refutation vectors fail to save it: this is a travelTip seller string, not a clog itemId (no multi-source/variant artifact); the scroll has one in-game vendor regardless of account type; and the wiki names no Telia involvement for the scroll. Telia IS legitimately referenced elsewhere in this entry (line 21829: "Spend marks at Telia for Varlamore graceful pieces and Calcified acorns") as the marks-of-grace vendor, which is correct and must stay — the error is solely the scroll-seller attribution on line 21805. Note the finding's premise that this is "carried into step 1": step 1's own description (21808) and travelTip (21814) do NOT name a seller, so the fix targets line 21805 only. Severity low: a guidance hint string, not an itemId/rate/requirement. The fix is "Telia" -> "Worm Tongue" in the line 21805 travelTip only.

### [high] Laps reward 'Varlamore marks of grace' directly

- **Field:** step 2
- **Data says:** Laps reward 'Varlamore marks of grace' directly
- **Wiki says (raw):** Termites collected from running the Colossal Wyrm Agility Course can be exchanged at Worm Tongue's Wares.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Colossal_Wyrm_Agility_Course
- **Suggested fix:** Remove 'Varlamore marks of grace' as a direct lap reward. The course awards termites (and blessed bone shards) — marks of grace are not earned directly from laps. Varlamore graceful pieces are purchased from Worm Tongue using termites.
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/Colossal_Wyrm_Agility_Course), verbatim: - "Before attempting the course, players must first speak to Worm Tongue, an anteater, about helping him gather termites around the scaffolding in order to preserve the wyrm's remains." - "Termites collected from running the Colossal Wyrm Agility Course can be exchanged at Worm Tongue's Wares." - Worm Tongue's Wares: "Graceful crafting kit" costs 650 termites ("Gives your graceful kit a Varlamore-themed makeover"); "Calcified acorn" costs 900 termites. - "Termites may indirectly be exchanged for marks of grace by selling amylase packs purchased here to Grace's Graceful Clothing shop in return for 8 marks of grace." - WebFetch confirmation: "The course does not award marks of grace directly." and "There is no character or location named 'Telia' mentioned in this article. The NPC who manages rewards is 'Worm Tongue.'"  Our data (drop_rates.json, Colossal Wyrm Agility, guidanceSteps step 2): "Complete laps of the Colossal Wyrm Agility Course for Varlamore marks of grace and termite currency. Collect marks when they appear on obstacles - they despawn after 10 minutes. Spend marks at Telia for Varlamore graceful pieces and Calcified acorns."
- **Skeptic reasoning:** Ran every refutation vector. This is not a multi-source clog artifact, not a variant id, not account-type/progress specificity, and not staleness — it is a substantive guidance error about the reward currency model. The wiki states unambiguously that the Colossal Wyrm course currency is TERMITES (auto-collected by scooping during laps), exchanged at Worm Tongue's Wares for graceful crafting kit (650) and Calcified acorn (900). Marks of grace are NOT a direct reward; they only arise indirectly by buying amylase packs and selling them to Grace's shop. Our step 2 misstates the currency as "Varlamore marks of grace," imports the rooftop-course "marks appear on obstacles / despawn after 10 minutes" mechanic (which does not apply to this termite-scoop course), and names "Telia" as the vendor — the wiki vendor is Worm Tongue's Wares, and Telia is not on the page (Telia in our travelTip is also dubious; the wiki says the teleport scroll is bought from Worm Tongue or the GE). The contradiction is direct and backed by a raw authoritative quote, so the finding STANDS as CONFIRMED. Severity high is appropriate: the entire reward/currency description for the step is wrong and would misdirect a player. Suggested fix: rewrite step 2 to state laps award termites (scooped automatically) plus blessed bone shards, spent at Worm Tongue's Wares for Varlamore graceful pieces / graceful crafting kit and Calcified acorns; remove the marks-of-grace-on-obstacles and "Telia" language.

### [blocker] Graceful pieces and Calcified acorns are purchased from Telia using marks

- **Field:** step 2
- **Data says:** Graceful pieces and Calcified acorns are purchased from Telia using marks
- **Wiki says (raw):** Bought from Worm Tongue or the Grand Exchange. [...] Worm Tongue sells [...] Graceful crafting kit for 650 termites [...] Calcified acorns for 900 termites.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Worm_Tongue
- **Suggested fix:** Replace 'Telia' with 'Worm Tongue', and replace 'marks' with 'termites'. Items are purchased from Worm Tongue using termites, not from Telia using marks of grace.
- **Skeptic receipt:** From wiki_lookup "Colossal Wyrm Agility Course": "Before attempting the course, players must first speak to Worm Tongue, an anteater, about helping him gather termites around the scaffolding... Players can speak to Worm Tongue to see statistics about the course". "Use a Colossal wyrm teleport scroll, bought from Worm Tongue or the Grand Exchange."  From WebFetch of https://oldschool.runescape.wiki/w/Worm_Tongue: "Worm Tongue is an anteater in charge of the Colossal Wyrm Agility Course." Currency: "Players use Termites (collected from the agility course) to purchase items from this NPC." Items: Colossal wyrm teleport scroll 40 Termites; Graceful crafting kit (Varlamore variant) 650 Termites; Calcified acorn 900 Termites. "The currency is exclusively termites—not marks of grace."  WebFetch of https://oldschool.runescape.wiki/w/Telia returned HTTP 404 Not Found — no such NPC page exists.  Our data (drop_rates.json line 21829): "Spend marks at Telia for Varlamore graceful pieces and Calcified acorns." (also travelTip line 21805: teleport scroll "purchased from Telia").
- **Skeptic reasoning:** The finding survives every refutation vector. (1) Not a multi-source clog/itemId artifact — it targets a guidance-text field. (2) Not a legitimate variant. (3) Not account-type/progress dependent — Worm Tongue is the sole vendor and termites the sole currency for all account types. (4) N/A to canonical clog id. (5) Not staleness — the Colossal Wyrm course is 2024 Varlamore content that has always used Worm Tongue + termites; there was never a "Telia"/marks-of-grace version. (6) Requirement gate unaffected.  Two genuine contradictions confirmed with raw authoritative quotes: (a) vendor "Telia" is wrong — the wiki has NO Telia page (404), and the course page + Worm Tongue page confirm the vendor is Worm Tongue, an anteater; (b) currency "marks" is wrong — purchases use termites, the wiki explicitly stating "exclusively termites—not marks of grace." The suggested fix (Telia->Worm Tongue, marks->termites) is correct; price detail (650/900 termites) is corroborated.  Caveat on severity: the proposed "blocker" is over-rated. This is player-facing guidance text, not an itemId, clog-membership, or drop-rate defect — it cannot mis-rank sources or corrupt the log. The same wrong vendor "Telia" also appears in the travelTip (line 21805), so a fix should correct both occurrences. Real bug, but high (guidance accuracy), not blocker.

## Rooftop Agility (source #158)

### [high] Varrock Rooftop course requires Agility level 10+

- **Field:** step 1
- **Data says:** Varrock Rooftop course requires Agility level 10+
- **Wiki says (raw):** Varrock: 30 (minimum agility level requirement for the Varrock Rooftop course)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rooftop_Agility
- **Suggested fix:** Change 'Varrock (10+)' to 'Varrock (30+)'
- **Skeptic receipt:** OSRS Wiki (Varrock_Rooftop_Course) via runelite-dev wiki_lookup, raw: "The Varrock Rooftop Course is a Rooftop Agility Course located in Varrock that is available to players with an Agility level of 30 or higher. It is preceded by the Al Kharid Rooftop Course and followed by the Canifis Rooftop Course."  Our data (drop_rates.json, Rooftop Agility, guidanceSteps[0]) raw:   description: "...Use Varrock (10+), Canifis (40+), Falador (50+), or Pollnivneach (70+) depending on your level."   travelTip:   "Camelot teleport -> Seers' Village rooftop (60+ Agility); Varrock teleport for Varrock rooftop (10+)"
- **Skeptic reasoning:** Ran every refutation vector and the finding survives all of them:  1. Multi-source / variant / account-type vectors do not apply — this is a course unlock level in guidance text, not a clog itemId or an account-specific gate. 2. Staleness: the level-30 Varrock requirement is long-standing OSRS mechanics, not recent drift. 3. The contested value is specifically the per-course label "Varrock (10+)" in step 1 (guidanceSteps[0]), which appears twice (description and travelTip). The authoritative wiki quote states the Varrock course requires Agility 30 or higher — a direct, unambiguous contradiction of the "10+" label. 4. Internal-consistency cross-check: the other course numbers in the same string (Canifis 40, Falador 50, Seers' 60, Pollnivneach 70) are correct, which isolates "Varrock (10+)" as a genuine factual error rather than a deliberate convention. Level 10 is the Draynor Rooftop entry level (and matches the activity-wide requirements.skills Agility 10), so the "10" was mis-attributed to the Varrock course.  The suggested fix (Varrock 10+ -> 30+) matches the authoritative receipt. Note for the fixer: only the per-course "Varrock (10+)" labels in step 1 should change to 30+; the top-level requirements.skills Agility=10 is the legitimate activity-entry level (Draynor opens at 10) and is a separate question, not contradicted by this receipt.

### [medium] Seers' Village gives the most marks per hour at 60+

- **Field:** step 1
- **Data says:** Seers' Village gives the most marks per hour at 60+
- **Wiki says (raw):** Canifis yields the highest rate at 16–18 marks per hour (notably constant across all levels). The Ardougne course with elite diary completion offers 22 marks per hour, which is the highest listed rate.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mark_of_grace
- **Suggested fix:** Change to indicate Canifis (40+) gives the best marks per hour for most players, with Ardougne being highest at elite diary completion. Seers' Village is good for XP but not the marks/hr leader.
- **Skeptic receipt:** From https://oldschool.runescape.wiki/w/Mark_of_grace (wiki_lookup + WebFetch, verbatim):  "Canifis Rooftop Course has the greatest chance at spawning marks for players between level 40 to 89 Agility, due to its higher-than-normal spawn rate of 2/3 instead of 1/3, and the lack of the 20-levels-over penalty that is applied to the other courses. Using the Canifis Rooftop Course will typically net players ~12-18 marks per hour, depending on luck."  Per-course marks/hr (verbatim from the page table): - Canifis: "~12-18 marks per hour" - Seers' Village: "11-13.8 (base)" ... "14-16.6 (hard diary)" - Ardougne: "16-18.1 (base)" ... "22 (elite diary)" - Pollnivneach: "13-14 (base)" ... "16-18 (hard diary)" - Rellekka: "13-15.5 (base)" ... "16-19.5 (hard diary)"  Data under review (drop_rates.json lines 21922, 21933, 21939): travelTip "Camelot teleport -> Seers' Village rooftop (best marks/hr at 60+)"; guidance "Seers' Village (60+ Agility, Camelot teleport) gives the most marks per hour."
- **Skeptic reasoning:** The data asserts Seers' Village gives the most marks per hour at 60+. The wiki contradicts this on its face: even with the hard Kandarin diary, Seers' tops out at 14-16.6 marks/hr, which is below Canifis (~12-18), Ardougne base (16-18.1) and elite (22), Pollnivneach hard (16-18), and Rellekka hard (16-19.5). The wiki explicitly names Canifis as having "the greatest chance at spawning marks for players between level 40 to 89 Agility" due to its 2/3 spawn rate and no over-level penalty. A 60+ account sits squarely in that 40-89 band, so Seers' is not the marks/hr leader for that range — it is the well-known XP/hr leader, a distinct metric the data conflates.  Refutation vectors checked: (1) not a multi-source clog id issue — this is a guidance/travelTip text claim, not an itemId. (2) not a variant id. (3) not account-type dependent — Canifis's advantage holds for mains and ironmen alike across 40-89; Seers' is never the marks/hr leader at 60+ for any account type. (4) not a temple clog-id question. (5) not staleness — the 2/3 Canifis spawn rate and the XP-vs-marks distinction are longstanding, and Ardougne elite's 22 is the current ceiling. (6) requirement nuance confirmed via wiki, not memory.  Caveat: the finding's own phrasing slightly overstates Canifis ("16-18" — wiki says ~12-18) and the absolute highest is Ardougne elite at 22, not Canifis. But the claim actually under review — that the data's "Seers' gives the most marks/hr at 60+" is wrong — is correct and authoritatively backed. The fix should correct Seers' from "best marks/hr" to "best XP/hr," and point marks-focused players to Canifis (40-89) / Ardougne elite (22). Severity medium is appropriate: it misroutes marks-farming players but does not break item tracking.

### [high] Each graceful piece reduces run energy drain by 30%

- **Field:** step 2
- **Data says:** Each graceful piece reduces run energy drain by 30%
- **Wiki says (raw):** Hood: +3%, Top: +4%, Legs: +4%, Gloves: +3%, Boots: +3%, Cape: +3%. The full set combined provides a total of +20% run energy restoration rate.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mark_of_grace
- **Suggested fix:** Graceful increases run energy restoration rate (not reduces drain), with each piece giving +3% to +4% restoration bonus (totalling +20% for the full set). Change to: 'Equipping graceful pieces increases run energy restoration rate (+3–4% per piece, +20% full set).'
- **Skeptic receipt:** drop_rates.json step 2 (line 21943): "Complete rooftop agility laps. ... Equipping graceful reduces run drain by 30% per piece. ..."  OSRS Wiki — https://oldschool.runescape.wiki/w/Graceful_outfit (verbatim): "While individual pieces of the graceful outfit will increase the rate of the player's natural run energy restoration (adding up to 20%), wearing the full graceful outfit gives an additional 10% bonus, giving a total of 30% increased natural run energy restoration." Per-piece table: Hood +3%, Top +4%, Legs +4%, Gloves +3%, Boots +3%, Cape +3%. Footnote: "The total of the individual items is 20%, but the set effect adds another 10% increasing the total to 30%." WebFetch summary: "the Graceful outfit increases run energy restoration rate rather than reducing drain ... The outfit does not reduce how quickly your character loses stamina while running. Rather, it accelerates the passive recovery rate."
- **Skeptic reasoning:** The data contradicts the wiki on two independently verifiable points, both confirmed by a raw quote from the cited authoritative source.  1. Wrong mechanic direction: the data says graceful "reduces run drain." The wiki states graceful "increase[s] the rate of the player's natural run energy restoration" and the fetch explicitly confirms it "does not reduce how quickly your character loses stamina." Restoration rate and drain are distinct mechanics; the data describes the wrong one.  2. Wrong magnitude/distribution: the data says "30% per piece." The wiki shows individual pieces give only +3-4% each (summing to +20%), with the +30% being the FULL-SET total (20% pieces + 10% set bonus). "30% per piece" is not a real OSRS value at all.  Refutation vectors checked and cleared: this is guidance text, not an itemId/source mapping, so the multi-source and variant vectors are inapplicable. The graceful mechanic is identical across all account types (not account/progress-dependent). The +20%/+30% restoration mechanic and the +3/+4 per-piece split are long-stable; no recent wiki rework makes "reduces drain 30% per piece" correct, so this is not staleness/drift. The finding is a genuine player-facing factual error and survives every refutation vector.  Caveat on the proposed FIX wording only (does not affect the verdict): the finding's suggested replacement "+3-4% per piece, +20% full set" understates the full-set total — the wiki full set is +30% (the +20% is pieces-only before the +10% set bonus). The fix text should read "+30% full set." The underlying bug is confirmed regardless.

## Cyclopes (source #160)

### [high] 10 warrior guild tokens are consumed per Cyclopes kill

- **Field:** step 3
- **Data says:** 10 warrior guild tokens are consumed per Cyclopes kill
- **Wiki says (raw):** Ten tokens are spent upon entering the room, and another ten every minute inside
- **Wiki URL:** https://oldschool.runescape.wiki/w/Warriors%27_Guild
- **Suggested fix:** Replace 'Each kill costs 10 warrior guild tokens' with 'Tokens are spent on entry (10 tokens) and then 10 tokens per minute while inside the room — bring a large supply.'
- **Skeptic receipt:** OSRS Wiki, Warriors' Guild page (https://oldschool.runescape.wiki/w/Warriors%27_Guild), "Top floor" section, verbatim: "Ten tokens are spent upon entering the room, and another ten every minute inside." Additionally: "If the player runs out of tokens, they have a minute left before being teleported outside of the room." Wiki summary corroboration (wiki_lookup, Warriors' Guild): "These activities reward warrior guild tokens, which grant access to the top floor and basement of the guild where players can kill cyclopes for defenders... Players wearing an Attack cape can enter these areas without tokens."  Our drop_rates.json (line 22199) step 3 reads: "Kill Cyclopes for defender drops. Each kill costs 10 warrior guild tokens. Hold the current tier defender in inventory to unlock the next tier. Protect from Melee recommended."
- **Skeptic reasoning:** Ran all six refutation vectors; the finding survives every one. (1) Not a multi-source clog itemId issue — this is guidance prose. (2) Not an item-variant id question. (3) Not account-type/progress dependent: the entry+per-minute token drain applies to every account that lacks an Attack cape; the "per kill" framing is wrong for all of them, and the only exemption (Attack cape) is orthogonal to the per-kill-vs-per-minute error. (4) Not a canonical-id question. (5) Not staleness: the entry+per-minute mechanic is the long-standing, current behavior — "per kill" was never the actual mechanic, so this is not version drift. (6) Requirement nuance: the wiki "Top floor" section directly and verbatim contradicts the "per kill" framing.  The contradiction is genuine and authoritatively sourced, not a paraphrase. The Cyclops monster page does not mention tokens, but the Warriors' Guild page (the cited authoritative URL) does, verbatim matching the finding. Our text materially misleads the player on how to provision tokens — a slow or AFK player pays by wall-clock minutes, not by kill count. The suggested fix ("Tokens are spent on entry (10 tokens) and then 10 tokens per minute while inside the room — bring a large supply") matches the wiki. Severity "high" is arguably generous since this is descriptive guidance, not an itemId/rate/gating defect, but the claim itself is factually correct and the data does contradict the wiki, so the verdict is CONFIRMED.

## Tranche 2 (sources 161-195) - 45 confirmed

## Motherlode Mine (source #163)

### [high] Motherlode Mine (field: step 1)

- **Data says:** 72 Mining required for upper level access
- **Wiki says (raw):** The current requirement is 57 Mining. The wiki notes that this was "decreased from 72 to 57" in a May 8, 2024 update.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Motherlode_Mine
- **Suggested fix:** Change '72 Mining' to '57 Mining' for the upper level requirement.
- **Skeptic receipt:** Authoritative wiki (https://oldschool.runescape.wiki/w/Motherlode_Mine?action=raw): "Unlocking the upper level of the mine requires base Mining 57, and a one-time fee of 100 golden nuggets paid to Prospector Percy." Update note (8 May 2024): "The Mining level required to unlock the upper level has been decreased from 72 to 57."  Our data (src/main/resources/com/collectionloghelper/drop_rates.json): Line 22652: "...30 Mining required; upper level needs 72 Mining and 100 golden nuggets one-time." Line 22701: "...Upper level hopper needs 72 Mining and 100 nuggets one-time unlock, and gives roughly 10% more nuggets per hour."  wiki_updates (title_contains "Motherlode", since 2026-05-01): {"ok": true, "count": 0, "changes": []} — page is stable, not mid-edit; our value is genuine post-update drift, not a transient wiki revision.

### [high] Motherlode Mine (field: step 4)

- **Data says:** 72 Mining required for upper level hopper
- **Wiki says (raw):** The current requirement is 57 Mining. The wiki notes that this was "decreased from 72 to 57" in a May 8, 2024 update.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Motherlode_Mine
- **Suggested fix:** Change '72 Mining' to '57 Mining' for the upper level hopper requirement.
- **Skeptic receipt:** OSRS Wiki raw wikitext (https://oldschool.runescape.wiki/w/Motherlode_Mine?action=raw): "Unlocking the upper level of the mine requires base {{SCP|Mining|57|link=yes}}, and a one-time fee of 100 [[golden nugget]]s" Update history (8 May 2024): "The Mining level required to unlock the upper level has been decreased from 72 to 57."  Our data (src/main/resources/com/collectionloghelper/drop_rates.json, Motherlode Mine source): Line 22652: "...30 Mining required; upper level needs 72 Mining and 100 golden nuggets one-time." Line 22701: "...Upper level hopper needs 72 Mining and 100 nuggets one-time unlock, and gives roughly 10% more nuggets per hour."

### [high] Motherlode Mine (field: step 5)

- **Data says:** Prospector legs cost 40 golden nuggets
- **Wiki says (raw):** Legs: 50 nuggets
- **Wiki URL:** https://oldschool.runescape.wiki/w/Motherlode_Mine
- **Suggested fix:** Change 'legs (40)' to 'legs (50)'.
- **Skeptic receipt:** OSRS Wiki — Prospector kit (https://oldschool.runescape.wiki/w/Prospector_kit)  Raw pricing table (item images stripped, "Buys for (in nuggets)" column): | XP Boost | Buys for | Sells for | Boost %pt |+0.4%||40||32||0.010   (helmet) |+0.8%||60||48||0.013   (jacket) |+0.6%||50||40||0.012   (legs) |+0.2%||30||24||0.007   (boots) ! Complete set ! +2.5% ! 180 ! 144  Named-piece confirmation (WebFetch of same page): - Prospector helmet: 40 golden nuggets - Prospector jacket: 60 golden nuggets - Prospector legs: 50 golden nuggets - Prospector boots: 30 golden nuggets Total set = 180 (40+60+50+30), matching the wiki's stated complete-set total of 180.  Our data (drop_rates.json line 22713, Motherlode Mine guidance step "Reward"): "Spend golden nuggets at Prospector Percy shop: helmet (40), jacket (60), legs (40), boots (40), coal bag (100), gem bag (100)..."

### [high] Motherlode Mine (field: step 5)

- **Data says:** Prospector boots cost 40 golden nuggets
- **Wiki says (raw):** Boots: 30 nuggets
- **Wiki URL:** https://oldschool.runescape.wiki/w/Motherlode_Mine
- **Suggested fix:** Change 'boots (40)' to 'boots (30)'.
- **Skeptic receipt:** OSRS Wiki — Prospector boots (wiki_lookup raw): "They can be purchased from Prospector Percy for 30 golden nuggets or from Volcanic Mine for 21,000 points."  Contrasted with the canonical Prospector helmet page (wiki_lookup raw), which confirms the 40-nugget tier is correct ONLY for the helmet: "bought from the Motherlode Mine in exchange for 40 golden nuggets".  drop_rates.json (Motherlode Mine source) currently says: - line 22622-22626: itemId 12016 "Prospector boots" ... "pointCost": 40 - line 22713 guidance step: "Spend golden nuggets at Prospector Percy shop: helmet (40), jacket (60), legs (40), boots (40), coal bag (100), gem bag (100)."  wiki_updates (title_contains=Prospector, since 2026-01-01): {"ok": true, "count": 0, "changes": []} — confirms the 30-nugget price is stable, not recent drift.

## Shooting Stars (source #164)

### [blocker] Shooting Stars (field: step 2)

- **Data says:** A Star Sprite emerges from the core when the star reaches tier 1, and talking to it awards bonus stardust.
- **Wiki says (raw):** Star Sprites do not exist in Old School RuneScape; rewards are exchanged for stardust via a shop run by Dusuri instead.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shooting_Stars
- **Suggested fix:** Remove the Star Sprite sentence entirely. Stardust is not obtained by talking to a sprite — it is collected while mining and then exchanged at Dusuri's Star Shop.
- **Skeptic receipt:** OSRS Wiki, Star_Sprite (https://oldschool.runescape.wiki/w/Star_Sprite), verbatim: "Star Sprites are unused NPCs who were intended to appear after a Crashed Star has been completely mined to the core." Article further: due to player feedback the crashed stars now "disintegrate to dust instead after being completely mined."  OSRS Wiki, Shooting_Stars (wiki_lookup summary), verbatim: "Stardust may be exchanged at Dusuri's Star Shop by the Mining Guild entrance in Falador for various rewards." The full activity description (telescope scouting, mining layers at fixed 7-minute rate, stardust rolls 29%-46% by Mining level) contains NO Star Sprite step.  npc_lookup "Star Sprite": abextm cache NPC id 10632 ("Star Sprite"), Location: N/A — i.e. present only in cache/APIs, no in-world location. npc_lookup "Dusuri": ids 10630/10631, Location: Mining Guild.

### [medium] Shooting Stars (field: step 1)

- **Data says:** Stars have a spawn window of 85-95 minutes.
- **Wiki says (raw):** A star will crash roughly every 90 minutes on each server (with a variation of up to 15 minutes)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shooting_Stars
- **Suggested fix:** Change to "Stars fall roughly every 90 minutes (±15 minutes)" to match the wiki's stated variation range of 75-105 minutes.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Shooting_Stars), verbatim: "A star will crash roughly every 90 minutes on each server (with a variation of up to 15 minutes), falling at a random spot selected from a predetermined list."  Our data (drop_rates.json line 22774): "...Stars fall every 85-95 minutes and vary in size (tier 1-9)..." (also line 22762 locationDescription: "spawn every ~90 minutes per world").

## The Hueycoatl (source #171)

### [high] The Hueycoatl (field: step 3)

- **Data says:** 4 anchor pillars that Hueycoatl rams each phase; player runs to active pillar and DPS the head while bound
- **Wiki says (raw):** three pillars located west of the head ... blue, red, and green pillars corresponding to Magic, Melee, and Ranged prayers respectively
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Hueycoatl/Strategies
- **Suggested fix:** Replace the anchor-pillar ramming description with the actual mechanic: the fight involves three prayer-colour pillars (blue/Magic, red/Melee, green/Ranged) west of the head. Players activate the corresponding protection prayer based on the colour of incoming projectiles.
- **Skeptic receipt:** From OSRS Wiki "The Hueycoatl/Strategies" (raw via wiki_lookup + verbatim WebFetch quotes):  Pillar count and nature: "The seer will begin charging the three pillars located west of the head." "Players can charge the blue, red, and green pillars by praying Protect from Magic, Protect from Melee, and Protect from Missiles respectively." "To charge all 3 pillars, teams need to equally balance the number of players using each protection prayer."  Head vulnerability (not via binding): "Once the path to the summit opens, the Hueycoatl can be attacked directly." "Once the tail is broken, the head of the Hueycoatl can be attacked once again."  Absent terms (verbatim search result): 'The words "anchor," "ram," "rams," or "bound/binding" do not appear on this page.'  wiki_lookup raw (encounter structure, no anchor/ram/bind mechanic): "a fight consists of five body parts (with each having 250 health), the head itself (with 2,500 health), and the tail (with 300 health)... The Hueycoatl will primarily use two attacks throughout the entire fight: Glowing symbols... Rain Fire - ... players must activate the corresponding protection prayer based on the colour of the incoming projectile."  Our data (drop_rates.json line 23304): "The fight cycles across 4 anchor pillars - Hueycoatl rams a different pillar each phase. Run to the active pillar and DPS the head while it is bound."

### [medium] The Hueycoatl (field: step 2)

- **Data says:** Travel alternative: Civitas illa Fortis lodestone and run west to the arena
- **Wiki says (raw):** Using the Quetzal Transport System to reach Quetzacalli Gorge, then running north-east through the gate and then north-west around the mountain
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Hueycoatl/Strategies
- **Suggested fix:** Replace 'Civitas illa Fortis lodestone and run west' with the wiki-confirmed alternative: use the Quetzal Transport System to Quetzacalli Gorge, then run north-east through the gate and north-west around the mountain. Also note that the pendant of ates can teleport directly to the Darkfrost once unlocked.
- **Skeptic receipt:** wiki_lookup "Lodestone": https://oldschool.runescape.wiki/w/Lodestone -> "#REDIRECT Waystone" (OSRS has no lodestone network; lodestones are an RS3 mechanic).  wiki_lookup "Civitas illa Fortis" transport section (raw): "The player must first travel to Civitas illa Fortis by travelling with Primio... * The Civitas illa Fortis Teleport spell on the standard spellbook... * Charter ships... * Players can teleport into the Fortis Colosseum via ring of dueling... * A fairy ring (code ) will take players to the centre of the Avium Savannah... * The portal nexus or portal chamber in a player-owned house..." — NO lodestone is listed.  wiki_lookup "The Hueycoatl/Strategies" (raw), quickest methods of reaching the base camp: "* Using a pendant of ates to teleport to the Darkfrost, after activating the Statue of Ates just west of the base camp. * Using the Quetzal Transport System to reach Quetzacalli Gorge, then running north-east through the gate and then north-west around the mountain."  coordinate_helper: arena (1509,3290) = The Hueycoatl (0 tiles); Civitas illa Fortis center ~ (1633,3122). Arena is NW of the city (lower X, higher Y); access is via Quetzacalli Gorge, not a straight overland "run west."  wiki_updates "Hueycoatl": count 0 since 2026-06-06 (not staleness).

## Yama (source #172)

### [high] Yama (field: step 3)

- **Data says:** Party of 2-4 players
- **Wiki says (raw):** Yama can be fought alone or with another player that has signed the initial contract with him.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Yama
- **Suggested fix:** Change 'party of 2-4' to 'solo or duo (max 2 players)'. The maximum party size is 2, not 4.
- **Skeptic receipt:** drop_rates.json guidance (raw grep): "description": "Form a party of 2-4 and enter Yama's instance. Solo is possible at endgame but DPS check is brutal; duo is the standard. Make sure all members have prayer pots and brews."  OSRS Wiki (https://oldschool.runescape.wiki/w/Yama), verbatim: "Yama can be fought alone or with another player that has signed the initial contract with him." "Yama is similar to the Royal Titans in that he can be fought alongside a partner in his base form." WebFetch confirmation: "only 1-2 players can enter the fight. There is no mention of 3 or 4 players being able to participate in the encounter." Maximum party size: 2 players (challenger plus one partner).

### [medium] Yama (field: step 2)

- **Data says:** Chasm of Fire is beneath Shayzien, Varlamore
- **Wiki says (raw):** The Chasm of Fire is a dungeon located in the north-west corner of Shayzien, housing a variety of demons led by Yama, the Master of Pacts.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chasm_of_Fire
- **Suggested fix:** Remove 'Varlamore'. Shayzien is part of Great Kourend (Zeah), not Varlamore. Change to 'Chasm of Fire in north-west Shayzien, Great Kourend.'
- **Skeptic receipt:** DATA (drop_rates.json line 23456, Yama Travel step):   "description": "Travel to the Chasm of Fire beneath Shayzien, Varlamore. Use a chasm teleport scroll for the fastest route, or Xeric's talisman to Heart and run south",  WIKI (wiki_lookup "Chasm of Fire", raw):   Infobox:   Location: Great Kourend   Members: Yes   "The Chasm of Fire is a dungeon located in the north-west corner of Shayzien, housing a variety of demons led by Yama, the Master of Pacts."  WIKI (wiki_lookup "Shayzien", raw):   Infobox:   Location: Great Kourend   "Shayzien is one of the five cities in the Kingdom of Great Kourend."

### [medium] Yama (field: travelTip)

- **Data says:** Xeric's talisman to Heart of Gielinor, then run south to the chasm
- **Wiki says (raw):** Travel methods listed: chasm teleport scroll, fairy ring DJR (south-east of chasm), Kharedst's Memoirs (History and Hearsay), Battlefront Teleport (north-west of chasm), Skills Necklace to Farming Guild then run south-east. No Xeric's talisman route is listed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chasm_of_Fire
- **Suggested fix:** Replace the Xeric's talisman route with a documented alternative. For example: 'fairy ring DJR then run northwest' or 'Battlefront Teleport then run southeast to the chasm entrance.'
- **Skeptic receipt:** Chasm of Fire (https://oldschool.runescape.wiki/w/Chasm_of_Fire) — Transportation, verbatim: 1. "A chasm teleport scroll brings players inside the dungeon on the bottom floor (only if you've previously been to Great Kourend)." 2. "The Fairy ring code DJR will bring the players just south-east of the Chasm (only if you've previously been to Great Kourend)." 3. "Using Kharedst's memoirs to teleport to the Graveyard of Heroes via reminiscing 'History and Hearsay'. Requires completion of Tale of the Righteous." 4. "Using Battlefront teleport brings players just north-west of the chasm." 5. "Using a skills necklace to teleport to the Farming Guild, and then running south-east. A form of antipoison is recommended due to lizardmen on the way." => "No, the page does not mention Xeric's talisman or Heart of Gielinor (God Wars Dungeon 2) as methods to reach the Chasm of Fire."  Yama (https://oldschool.runescape.wiki/w/Yama) — travel section: "Chasm teleport scroll", "Fairy ring code DJR ... then run northwest", "Kharedst's memoirs/Book of the Dead - History and Hearsay ... then run north". "The article does not mention Xeric's talisman or Heart of Gielinor as travel methods."  Xeric's talisman (https://oldschool.runescape.wiki/w/Xeric%27s_talisman) — five destinations only: Xeric's Lookout (SE of Shayzien), Xeric's Glade (NE of Hosidius farming patch), Xeric's Inferno (Lovakengj lovakite furnace), Xeric's Heart (Kourend Castle statue), Xeric's Honour (summit of Mount Quidamortem). "There is no 'Heart of Gielinor' teleport option available on this item."

### [medium] Yama (field: step 2)

- **Data says:** Xeric's talisman to Heart, then run south to the chasm
- **Wiki says (raw):** Travel methods listed: chasm teleport scroll, fairy ring DJR (south-east of chasm), Kharedst's Memoirs (History and Hearsay), Battlefront Teleport (north-west of chasm), Skills Necklace to Farming Guild then run south-east. No Xeric's talisman route is listed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chasm_of_Fire
- **Suggested fix:** Replace with a documented travel method, e.g. 'fairy ring DJR then run northwest' or 'Battlefront Teleport then run southeast to the entrance.'
- **Skeptic receipt:** Chasm of Fire (wiki_lookup, raw): "The Chasm of Fire is a dungeon located in the north-west corner of Shayzien, housing a variety of demons led by Yama, the Master of Pacts."  Chasm of Fire travel methods (wiki_lookup, raw): "There are several ways to reach the dungeon: * A chasm teleport scroll brings players inside the dungeon on the bottom floor (only if you've previously been to Great Kourend). * The Fairy ring code  will bring the players just south-east of the Chasm (only if you've previously been to Great Kourend). * Using Kharedst's memoirs to teleport to the Graveyard of Heroes via reminiscing "History and Hearsay". Requires completion of Tale of the Righteous. * Using Battlefront teleport brings players just north-west of the chasm. * Using a skills necklace to teleport to the Farming Guild, and then running south-east. A form of antipoison is recommended due to lizardmen on the way." (No Xeric's talisman route is listed.)  Xeric's talisman destinations (wiki, WebFetch raw): "Xeric's Heart - next to the statue at Kourend Castle." Xeric's Honour requires an ancient tablet; the other four (Lookout SE of Shayzien, Glade NE of Hosidius farm, Inferno in Lovakengj, Heart at Kourend Castle) are default — none is at the north-west Shayzien chasm.

## Doom of Mokhaiotl (source #173)

### [high] Doom of Mokhaiotl (field: travelTip)

- **Data says:** The quetzal whistle destination is listed as the Mokhaiotl waystone
- **Wiki says (raw):** Fly to Tal Teklan, then run east to the Tonali Cavern
- **Wiki URL:** https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl
- **Suggested fix:** Change to: 'Quetzal whistle -> Tal Teklan, run east to Tonali Cavern, or Mokhaiotl waystone for direct teleport'
- **Skeptic receipt:** Wiki raw (https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl?action=raw), transportation section verbatim: "Players can use a [[Mokhaiotl waystone]] to teleport to the [[Ruins of Mokhaiotl]]." "Players can use [[Pendant of ates]] and teleport to the [[Kastori]] waystone and then run north-west." "Use a [[Quetzal whistle]] to fly to [[Tal Teklan]], and then run east to the [[Tonali Cavern]]."  Our data (drop_rates.json, "Doom of Mokhaiotl", line 23571): "travelTip": "Quetzal whistle -> Mokhaiotl waystone, or Civitas illa Fortis lodestone"

## Royal Titans (source #174)

### [medium] Royal Titans (field: step 2)

- **Data says:** From fairy ring AIQ (Mudskipper Point), run south to reach the Asgarnian Ice Dungeon
- **Wiki says (raw):** AIQ teleports you to Mudskipper Point in Asgarnia. Nearby points of interest include Mogres, Rimmington, Port Sarim, Asgarnia Ice Dungeon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Change 'run south' to 'run north' (or 'run north-east'). Mudskipper Point is at the southern tip of Asgarnia; the Asgarnian Ice Dungeon entrance is north/north-east of the AIQ landing spot, not south.
- **Skeptic receipt:** wiki_lookup "Asgarnian Ice Dungeon" (https://oldschool.runescape.wiki/w/Asgarnian_Ice_Dungeon) raw: "The Asgarnian Ice Dungeon is a moderately large dungeon in Asgarnia. It is found south of Port Sarim and north of Mudskipper Point, , under a trapdoor." ... "Fairy ring code , which goes to Mudskipper Point."  coordinate_helper distance from AIQ landing (2999,3113) to dungeon trapdoor/Skeletal-Wyvern area (3008,3150): "Euclidean: 38.1 tiles ... Chebyshev (game movement): 37 tiles". Destination Y (3150) is 37 tiles GREATER than landing Y (3113) = north in OSRS coords (higher Y = north); X delta is only +9 (slightly east).  coordinate_helper identify (2999,3110): "Nearest drop_rates.json source: Mogre — 12 tiles" (Mudskipper Point AIQ landing). identify (3008,3150): "Nearest drop_rates.json source: Skeletal Wyvern — 0 tiles" (the Asgarnian Ice Dungeon entrance area).

### [medium] Royal Titans (field: travelTip)

- **Data says:** After taking fairy ring AIQ to Mudskipper Point, the prose instructs the player to run south
- **Wiki says (raw):** AIQ teleports you to Mudskipper Point in Asgarnia. Nearby points of interest include Mogres, Rimmington, Port Sarim, Asgarnia Ice Dungeon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Change 'run south' to 'run north-east'. Mudskipper Point is south of Port Sarim and south of the Asgarnian Ice Dungeon entrance, so after landing at AIQ you run north-east to reach the dungeon.
- **Skeptic receipt:** wiki_lookup/WebFetch (https://oldschool.runescape.wiki/w/Fairy_ring): "fairy ring code AIQ teleports to Asgarnia: Mudskipper Point." WebFetch (https://oldschool.runescape.wiki/w/Mudskipper_Point): "Port Sarim is to the north: 'It can be reached by journeying south past the coastal town of Port Sarim.'" + "Next to him is a small hill with a trapdoor leading into the dangerous Asgarnian Ice Dungeon." coordinate_helper identify (2999,3112) [AIQ/Mudskipper Point]: "Nearest drop_rates.json source: Mogre - 12 tiles." coordinate_helper identify (3008,3150) [Royal Titans / Ice Dungeon entrance, the source's own worldX/worldY]: "Nearest drop_rates.json source: Skeletal Wyvern - 0 tiles" (an Asgarnian Ice Dungeon monster). coordinate_helper distance (2999,3112)->(3008,3150): "Euclidean: 39.1 tiles ... Approx walk time: 22.8s (running)" with delta X = +9 (east) and delta Y = +38 (north) -> dominant direction is NORTH, not south. drop_rates.json line 23709: "travelTip": "Giantsoul amulet -> direct teleport, or fairy ring AIQ -> run south"; line 23728 Travel step: "...fairy ring AIQ to Mudskipper Point and run south..."; line 23734: "fairy ring AIQ -> south + 60 Agility shortcut".

## Brutus (source #176)

### [high] Brutus (field: step 1)

- **Data says:** ~200 HP
- **Wiki says (raw):** Hitpoints: 58
- **Wiki URL:** https://oldschool.runescape.wiki/w/Brutus
- **Suggested fix:** Replace '~200 HP' with '~58 HP' (combat level 30 boss with only 58 hitpoints).
- **Skeptic receipt:** wiki_lookup "Brutus": "Infobox: NPC IDs: 15626, 15627 Combat level: 30 Slayer XP: 116 Slayer category: Cows Assigned by: turael,spria Hitpoints: 58 Members: No"  npc_lookup "Brutus" (abextm cache + wiki): "abextm cache (source of truth) NPC ids for "Brutus": 15626 ("Brutus"), 15627 ("Brutus"), 15628 ("Demonic Brutus"), 15629 ("Demonic Brutus") ... NPC IDs: 15626, 15627 Name: Brutus Combat level: 30 Hitpoints: 58"  drop_rates.json (Brutus, npcId 15626) guidance step 1 description: "Bring melee gear (any tier above mithril works - Brutus has low defence and ~200 HP)"

### [medium] Brutus (field: step 3)

- **Data says:** Brutus has only auto-attacks (a slam and a stomp), framed as trivial mechanics with no need to move
- **Wiki says (raw):** Two special attacks that give players 'a few ticks to move out of the way'
- **Wiki URL:** https://oldschool.runescape.wiki/w/Brutus
- **Suggested fix:** Revise to note that Brutus has two special attacks requiring the player to move out of the way within a few ticks, rather than describing all attacks as simple auto-attacks. Example: 'Brutus has two special attacks that require stepping out of the way — watch for the animation cue and move a tile or two.'
- **Skeptic receipt:** OSRS Wiki — https://oldschool.runescape.wiki/w/Brutus (verbatim, confirmed by two independent fetches): "Aside from a basic melee attack that can be negated through the use of the Protect from Melee prayer, Brutus uses two special attacks, both of which give players a few ticks to move out of the way."  NPC identity confirmation (wiki_lookup, raw): "Infobox: NPC IDs: 15626, 15627 | Combat level: 30 | Slayer category: Cows | Hitpoints: 58 ... Mooleta 5/150, Bottomless milk bucket (empty) 4/150, Cow slippers 1/150" — matches the source's npcId 15626 and Cow slippers / Mooleta drops, so this is the correct Brutus page.  Current data (drop_rates.json, step 3, line 23984): "Kill Brutus. Use Protect from Melee and attack with any decent melee weapon - Brutus is a straightforward fight with only a slam auto-attack and a low-damage stomp."

## Shellbane Gryphon (source #177)

### [high] Shellbane Gryphon (field: step 2)

- **Data says:** The cave entrance is on the eastern beach, reached by running east from the Great Conch arrival point to a cliff-side cave mouth.
- **Wiki says (raw):** The shellbane gryphon is located in a cave in the centre of the Great Conch, north of fairy ring CJQ.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shellbane_gryphon
- **Suggested fix:** Change to: 'Travel to The Great Conch and locate the Shellbane Gryphon Cave entrance in the centre of the island. Fairy ring CJQ or Quetzal whistle -> Great Conch, then head north to the cave entrance in the centre of the Great Conch.'
- **Skeptic receipt:** Independent WebFetch of https://oldschool.runescape.wiki/w/Shellbane_gryphon returned the exact location sentence:  "The shellbane gryphon is located in a cave in the centre of the Great Conch, north of fairy ring CJQ."  Travel detail from same fetch: "Players access this boss fight through a cave situated in the middle of the Great Conch island. The nearest fairy ring for quick travel is the CJQ code, from which the cave entrance lies to the north."  wiki_lookup confirms the NPC exists and is on the Great Conch content set (NPC ID 14860, Slayer level 51, category Gryphons/Bosses, drops Gryphon feather, Elkhorn frag, Pillar frag, Gull pet, etc.).

### [medium] Shellbane Gryphon (field: step 5)

- **Data says:** Gryphon feather drops 1 per kill (implied single item).
- **Wiki says (raw):** Gryphon feather [drops] 7–10 [in the 100% always section] and 35–50 [in the Other section]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shellbane_gryphon
- **Suggested fix:** Change '(1/kill)' to '(7–10 guaranteed per kill)' to reflect the actual drop quantity from the wiki's always-drop table.
- **Skeptic receipt:** MCP wiki_lookup raw output (Shellbane gryphon, NPC ID 14860):   Gryphon feather | qty: 7-10 | rarity: Always   Gryphon feather | qty: 35-50 | rarity: 11/128   Big bones | qty: 1 | rarity: Always  WebFetch (https://oldschool.runescape.wiki/w/Shellbane_gryphon):   "Gryphon feather 7-10 Always" (guaranteed every kill)   "Gryphon feather 35-50 ... 11/128" (separate, less common roll)  drop_rates.json step 5 (line 24122):   "Pick up the Gryphon feather (1/kill) and any rares, then exit the cave and teleport via Quetzal whistle to restock. Belle's folly (tarnished) is the main unique chase at 1/256"

## Scurrius (source #178)

### [medium] Scurrius (field: step 3)

- **Data says:** The manhole is located behind Varrock Palace (step 2 says 'manhole behind Varrock Palace')
- **Wiki says (raw):** Scurrius' lair is located within the Varrock Sewers, with the sewers being accessible via a manhole just east of Varrock Palace.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scurrius
- **Suggested fix:** Change step 2 to read 'manhole east of Varrock Palace' instead of 'manhole behind Varrock Palace'.
- **Skeptic receipt:** Wiki (Scurrius page, verbatim): "Scurrius' lair is located within the Varrock Sewers, with the sewers being accessible via a manhole just east of Varrock Palace."  Wiki (Varrock Sewers page, verbatim): "The Varrock Sewers can be accessed via a manhole just east of Varrock Palace."  Data (drop_rates.json line 24178, Scurrius entry, Travel step): "Travel to Varrock Sewers via the manhole behind Varrock Palace or south of Edgeville bank. Use the Varrock teleport for the fastest route" (worldX 3237, worldY 3458).

### [medium] Scurrius (field: step 5)

- **Data says:** Giant rats heal Scurrius if left alive
- **Wiki says (raw):** Scurrius may heal from the Food Piles between each phase. While he is eating, he has a slight damage reduction applied to him.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scurrius
- **Suggested fix:** Healing is attributed to Food Piles between phases, not directly to Giant rats. Change to 'kill the Giant rat spawns immediately to disrupt the fight; Scurrius heals from Food Piles between phases.'
- **Skeptic receipt:** RAW drop_rates.json (C:\Users\Chandler\Documents\Projects\collection-log-helper\src\main\resources\com\collectionloghelper\drop_rates.json, line 24218): "description": "Kill Scurrius. Stand under the boss for melee, dodge the falling rocks (red tiles), and kill the Giant rat spawns immediately - they heal Scurrius if left alive. Use Protect from Missiles to negate his ranged-style auto. Easy first-boss fight that often drops the Scurrius' spine (rune pouch upgrade)."  RAW wiki (https://oldschool.runescape.wiki/w/Scurrius/Strategies): "At the food piles, Scurrius will eat every few seconds, healing 5 or 10 hitpoints per player in the arena." For the giant rats: "Scurrius will screech, causing six level 46 giant rats to appear around the arena" -- with no mention of Scurrius healing from these minions; the only healing mechanic described involves the Food Pile locations.  RAW wiki (https://oldschool.runescape.wiki/w/Scurrius): "Scurrius may heal from the Food Piles between each phase. While he is eating, he has a slight damage reduction applied to him." For rats: "Scurrius may also summon six giant rats to aid him during the fight." -- no healing attributed to the rats.

### [high] Scurrius (field: step 2)

- **Data says:** The manhole is behind (north/rear of) Varrock Palace
- **Wiki says (raw):** with the sewers being accessible via a manhole just east of Varrock Palace.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scurrius
- **Suggested fix:** Change 'behind Varrock Palace' to 'east of Varrock Palace'.
- **Skeptic receipt:** Scurrius wiki (https://oldschool.runescape.wiki/w/Scurrius): "Scurrius' lair is located within the Varrock Sewers, with the sewers being accessible via a manhole just east of Varrock Palace."  Varrock Sewers wiki (https://oldschool.runescape.wiki/w/Varrock_Sewers): "The Varrock Sewers can be accessed via a manhole just east of Varrock Palace."  Varrock Palace wiki (https://oldschool.runescape.wiki/w/Varrock_Palace): "Entry points into the Varrock sewers can be found outside the compound by the eastern entryway and in the cage in the north-east corner."  Our data (drop_rates.json line 24178): "Travel to Varrock Sewers via the manhole behind Varrock Palace or south of Edgeville bank. Use the Varrock teleport for the fastest route"

## The Fight Caves (source #179)

### [blocker] The Fight Caves (field: step 5)

- **Data says:** Rearing back = ranged attack cue; stomping = magic attack cue
- **Wiki says (raw):** Magic attack: 'TzTok-Jad rears up on his hind legs and dangles his forward legs for a few seconds before launching a fireball from his mouth at the player.' Ranged attack: 'TzTok-Jad rears up on his hind legs, then slams down his front legs onto the ground causing a boulder to fall on the player'
- **Wiki URL:** https://oldschool.runescape.wiki/w/TzTok-Jad
- **Suggested fix:** Swap the visual cues: 'Pray Protect from Missiles when he slams/stomps his front legs (ranged attack) and Protect from Magic when he rears up and dangles his front legs (magic attack).' Both attacks begin with rearing up — the distinguishing cue is what follows: dangling forward legs = magic; slamming/stomping front legs down = ranged.
- **Skeptic receipt:** WebFetch of https://oldschool.runescape.wiki/w/TzTok-Jad (verbatim): Magic Attack: "TzTok-Jad rears up on his hind legs and dangles his forward legs for a few seconds before launching a fireball from his mouth at the player." Ranged Attack: "TzTok-Jad rears up on his hind legs, then slams down his front legs onto the ground causing a boulder to fall on the player." The page recommends Protect from Magic for the fireball and Protect from Missiles for the boulder.  Data under test (drop_rates.json line 24345): "Pray Protect from Missiles when he rears back (ranged attack) and Protect from Magic when he stomps (magic attack)..."

## The Inferno (source #180)

### [high] The Inferno (field: step 5)

- **Data says:** The triple JalTok-Jad wave is wave 67
- **Wiki says (raw):** Wave 68: 3x Jad
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Inferno
- **Suggested fix:** Change 'Triple-Jad wave 67' to 'Triple-Jad wave 68'
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/Inferno), verbatim wave descriptions: - Wave 66: "3x Nibbler, 2x Mage" ... "Any remaining pillars collapse when the final enemy is slain..." - Wave 67: "1x Jad" with "The JalTok-Jad summons five Yt-HurKot at half health." - Wave 68: "3x Jad" described as "The penultimate wave. The Jads attack three ticks apart. Each will summon three Yt-HurKot at half health." - Wave 69: final wave features TzKal-Zuk ("On the 69th and final wave, TzKal-Zuk will break free from the northern wall.")  drop_rates.json "The Inferno" step 5 (line 24479): "Triple-Jad wave 67. Each Jad rotates Magic / Ranged prayers independently..." and step 4 (line 24463): "Clear waves 1-66 ... entering wave 67" and step 6 (line 24495): "Defeat TzKal-Zuk on wave 69".

### [blocker] The Inferno (field: step 6)

- **Data says:** Jal-MejJak healers spawn at 480 HP
- **Wiki says (raw):** At 480 hitpoints, a JalTok-Jad will appear and attack the shield. At 240 hitpoints, Zuk will become enraged as it fights for its life, attacking 3 ticks (1.8s) faster and summoning four Jal-MejJaks on the lava in front of it.
- **Wiki URL:** https://oldschool.runescape.wiki/w/TzKal-Zuk
- **Suggested fix:** At 480 HP a JalTok-Jad spawns to attack the shield (not Jal-MejJak healers). The four Jal-MejJak healers spawn at 240 HP. Remove the false 480-HP healer spawn sentence and correct step 6 accordingly.
- **Skeptic receipt:** WebFetch of https://oldschool.runescape.wiki/w/TzKal-Zuk: "At 480 hitpoints, a JalTok-Jad will appear and attack the shield." "At 240 hitpoints, Zuk will become enraged as it fights for its life, attacking 3 ticks (1.8s) faster and summoning four Jal-MejJaks on the lava in front of it." "Once attacked, they [Jal-MejJaks] will stop healing Zuk and bombard the player's area for 5-10 damage..." "At 480 hitpoints, a different minion appears—not the Jal-MejJak... The Jal-MejJak spawn occurs later, at 240 hitpoints when Zuk becomes enraged."  npc_lookup (abextm cache, source of truth): Jal-MejJak = 7708 (cb 250); JalTok-Jad = 7700/7704/10623 (cb 900) — two genuinely distinct NPCs, supporting the wiki's distinction.

## Deranged Archaeologist (source #181)

### [high] Deranged Archaeologist (field: step 3)

- **Data says:** The Fedora cosmetic drop is attributed to the Deranged Archaeologist.
- **Wiki says (raw):** A fedora is a rare drop from the crazy archaeologist, it being the hat he wears.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fedora
- **Suggested fix:** Remove the Fedora reference entirely, or replace with a correct unique drop for this boss (e.g., Steel ring). The Fedora is dropped only by the Crazy Archaeologist, not the Deranged Archaeologist.
- **Skeptic receipt:** wiki_lookup "Fedora" (RAW): "A fedora is a rare drop from the crazy archaeologist, it being the hat he wears. It is purely cosmetic and has no bonuses." NPC IDs: 11990.  wiki_lookup "Deranged archaeologist" (RAW) drop table (24 entries), Fedora ABSENT; clog item is: "Steel ring | qty: 1 | rarity: 3/131". Other entries: Bones, Black d'hide body, Rune 2h sword, Rune sword, Water rune, Mud rune, Rune knife, Dragon arrow, Steel cannonball, Grimy dwarf weed, White berries, Runite limbs, Black dragonhide, Gold ore, Uncut diamond, Onyx bolt tips, Anchovy pizza, Prayer potion(3), Potato with cheese, Shark, Crystal key, Long bone, Brimstone key.  drop_rates.json (RAW), Deranged Archaeologist guidanceSteps[2] / Combat section, line 24586: "Kill the Deranged Archaeologist. ... This is a popular early-account boss for the Fedora cosmetic and the Steel ring slayer-task drop"  search_drop_rates item_name=Fedora (RAW): only source is "Crazy archaeologist" itemId 11990 — correctly attributed in the items array.

## Easy Treasure Trails (source #182)

### [high] Easy Treasure Trails (field: step 3)

- **Data says:** Easy clue scrolls contain cryptic clues, emote clues, and 6 anagram clues.
- **Wiki says (raw):** Easy clue scrolls can be between 2 and 4 steps long. [Clue types listed:] Cryptic clues, Emote clues, Maps, Music. Easy clue scrolls do not feature any coordinate clues, and require nothing to be fought. [The Treasure Trails/Guide/Anagrams page organizes anagrams into: Beginner, Medium, Hard, Elite, Master — no Easy tier exists.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Easy_clue_scroll
- **Suggested fix:** Remove 'and 6 anagram clues' entirely. Easy clue scrolls include cryptic clues, emote clues, map clues, and music clues — anagram clues do not appear at the easy tier at all.
- **Skeptic receipt:** Raw drop_rates.json step 3 (section "Combat"): "Complete all clue steps with the RuneLite Clue Scroll plugin. Easy clues add cryptic clues, emote clues, and 6 anagram clues. Always wear the exact emote-clue costume the clue requests - missing one piece resets the puzzle."  Raw wiki_lookup (Clue scroll (easy)): "Easy clue scrolls do not feature any coordinate clues, and require nothing to be fought. ... Easy clue scrolls can be between 2 and 4 steps long."  Raw WebFetch of https://oldschool.runescape.wiki/w/Clue_scroll_(easy) — exact clue-type list from the article: "Cryptic clues / Emote clues / Maps / Music". "Anagrams are NOT included in easy clue scrolls. The page explicitly lists only four clue types at the easy tier: cryptic clues, emote clues, maps, and music clues."  Raw WebFetch of https://oldschool.runescape.wiki/w/Treasure_Trails/Guide/Anagrams: "no Easy tier anagrams are listed. The guide includes anagram clues for the following tiers: 1. Beginner Anagrams 2. Medium Anagrams 3. Hard Anagrams 4. Elite Anagrams 5. Master Anagrams. The Easy difficulty tier is conspicuously absent from this anagrams guide."

## Medium Treasure Trails (source #183)

### [high] Medium Treasure Trails (field: step 3)

- **Data says:** Medium clues involve wizard fights with level 27 Saradomin/Zamorak/Guthix wizards requiring Protect from Magic
- **Wiki says (raw):** In hard and higher-levelled Treasure Trails, some steps will require the player to fight a random enemy based on the Treasure Trail's tier. [Medium clues show N/A for antagonists.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Treasure_Trails
- **Suggested fix:** Remove the wizard fight claim entirely from the medium clue guidance. Wizard encounters (Saradomin wizard level 108, Zamorak wizard level 65) are exclusive to hard clues and above. Medium clue coordinate steps do not spawn antagonist wizards.
- **Skeptic receipt:** RAW wiki (oldschool.runescape.wiki/w/Treasure_Trails) antagonists section — verbatim: "In hard and higher-levelled Treasure Trails, some steps will require the player to fight a random enemy based on the Treasure Trail's tier when the correct spot is dug at, as indicated with the list below:" Antagonists table — Medium row: "N/A" for all antagonist columns (Coordinate clues and Emote clues both list N/A). Antagonists table — Hard row, Coordinate clues: Level(s): 108; Enemy: "Saradomin wizard" and "Zamorak wizard"; Location: "Non-Wilderness" and "Wilderness".  RAW wiki (oldschool.runescape.wiki/w/Coordinates) — verbatim: "On hard Treasure Trails, digging on the correct spot will cause a hostile level 108 Saradomin wizard to appear when outside of the Wilderness, otherwise the level 65 Zamorak wizard will appear." Medium clues: "No enemies spawn. The 'Fight' column shows an X mark for all medium coordinates." Both pages: do NOT mention any level 27 wizard for any clue tier.

### [medium] Medium Treasure Trails (field: step 3)

- **Data says:** The sextant, watch, and chart are obtained from an NPC named Trinitus
- **Wiki says (raw):** A chart can be obtained from the Observatory professor at the Observatory.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chart
- **Suggested fix:** Replace 'from Trinitus' with the correct sources: Chart from the Observatory Professor at the Observatory; Sextant from Murphy at Port Khazard; Watch from Brother Kojo at the Clock Tower. Note: the RuneLite Clue Scroll plugin handles coordinate solving automatically so these items are rarely needed.
- **Skeptic receipt:** wiki_lookup "Chart": "A chart is used for coordinate clues for Treasure Trails... A chart can be obtained from the Observatory professor at the Observatory."  wiki_lookup "Sextant": "A chart can be obtained by first talking to the Observatory professor... then, speaking to Murphy in Port Khazard, who will give you a sextant. Murphy is found on the dock near the trawler for the Fishing Trawler. Brother Kojo in the Clock Tower can then give the player a watch."  wiki_lookup "Watch": "A chart can be obtained by first talking to the Observatory professor... After talking to him, you have to receive the sextant from Murphy at Port Khazard in order for Brother Kojo to give you a watch at the Clock Tower."  npc_lookup "Trinitus": "abextm cache: no NPC named \"Trinitus\" (check spelling/variant). NPC \"Trinitus\" not found on OSRS Wiki."  npc_lookup "Murphy": "abextm cache (source of truth) NPC ids for \"Murphy\": 5607, 5608, 5609, 5610, 10707 ... Location: Port Khazard"  drop_rates.json step 3 (Medium Treasure Trails): "Always carry a Sextant / Watch / Chart from Trinitus before the clue starts for instant coordinate solves."

## Elite Treasure Trails (source #185)

### [high] Elite Treasure Trails (field: step 3)

- **Data says:** Elite clue scrolls have double-agents ranging from level 84-210
- **Wiki says (raw):** They have a combat level of 108 for hard clues outside of the Wilderness and 65 within. They are level 141 for master clues. / Level 141 Double agents were added to the cache, presumably to appear when doing Elite clue scrolls, but were unused. / coordinate clues will require the player to fight an Armadylean guard or a Bandosian guard.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Double_agent
- **Suggested fix:** Elite clue scrolls do not have double-agents. The double-agent mechanic applies to hard clues (level 65 or 108) and master clues (level 141). Elite coordinate clues instead spawn an Armadylean guard or Bandosian guard. Remove the reference to 'elite double-agents' and replace with 'Armadylean or Bandosian guards (for coordinate clues)'.
- **Skeptic receipt:** Double_agent wiki (raw): "Level 141 Double agents were added to the cache, presumably to appear when doing Elite clue scrolls, but were unused." Combat levels: Hard = 108 (outside Wilderness) / 65 (within); Master = 141; Elite = not active. // Treasure_Trails/Guide/Coordinates wiki (raw, elite coordinates): "digging on the correct spot will cause either a level 97 Armadylean guard or a level 125 Bandosian guard to appear" — no double agents for elite clues. // Treasure_Trails (Antagonists) wiki: elite coordinate clues = Level 97 Armadylean guard / Level 125 Bandosian guard; "there are no double agents for elite clues. The double agent encounters appear only in hard clues (levels 65-108) and master clues (level 141)."

### [medium] Elite Treasure Trails (field: step 2)

- **Data says:** Elite clue geodes can be obtained while mining gem rocks and at the Motherlode Mine
- **Wiki says (raw):** A clue geode (elite) is randomly obtained from Mining if the player has not completed X Marks the Spot [sources include gem rocks, gold rocks, mithril rocks, adamantite rocks, runite rocks — motherlode mine is not listed]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Clue_geode_(elite)
- **Suggested fix:** Remove 'motherlode mine' from the geode sources. Elite clue geodes are obtained from standard ore and gem rocks, but the Motherlode Mine is not listed as a source. The travelTip has the same issue ('elite clue geodes while mining' is fine, but step 2 should not claim motherlode mine specifically).
- **Skeptic receipt:** From https://oldschool.runescape.wiki/w/Motherlode_Mine (verbatim): "Clue geodes cannot be obtained from ore veins."  From https://oldschool.runescape.wiki/w/Clue_geode_(elite) source list (gem, copper, tin, coal, clay, silver, gold, mithril, adamantite, runite, blurite, limestone, granite, sandstone, lovakite, amethyst, etc.) — Motherlode Mine is NOT listed.  Data text (drop_rates.json line 27915): "Elite clues also drop from elite clue geodes while mining gem rocks and motherlode mine".

## Master Treasure Trails (source #186)

### [high] Master Treasure Trails (field: step 1)

- **Data says:** Master double-agents are levels 108-210
- **Wiki says (raw):** They have a combat level of 108 for hard clues outside of the Wilderness and 65 within. They are level 141 for master clues.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Double_agent
- **Suggested fix:** Change 'level 108-210 master double-agents' to 'level 141 master double-agents' (108 is the hard-clue double-agent level, not master; 210 does not exist)
- **Skeptic receipt:** DATA (drop_rates.json line 28301, Master Treasure Trails, guidanceSteps[0]): "description": "Travel to the Grand Exchange with a spade, sextant + watch + chart, mirror shield (for double-agents), all teleport jewellery, a Stamina potion, prayer potions, and high-tier combat gear for the level 108-210 master double-agents and elite monster fights"  AUTHORITATIVE (OSRS Wiki, https://oldschool.runescape.wiki/w/Double_agent, verbatim): "They have a combat level of 108 for hard clues outside of the Wilderness and 65 within. They are level 141 for master clues."  WebFetch re-query, verbatim result: "The page does **not** mention combat level 210 anywhere." and "It only specifies levels for hard (108/65) and master (141) difficulties."

## Hard Treasure Trail Rewards (Rare) (source #187)

### [high] Hard Treasure Trail Rewards (Rare) (field: step 2)

- **Data says:** Kalphite Queen is listed as a source of hard clue scrolls via PvM.
- **Wiki says (raw):** Clue scroll (elite) — rarity 1/100 (increases to 1/95 after unlocking the elite Combat Achievements rewards tier).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Kalphite_Queen#Drops
- **Suggested fix:** Remove 'Kalphite Queen' from the hard clue scroll PvM sources list. The Kalphite Queen drops elite clue scrolls (1/100), not hard clue scrolls. Replace with a genuine hard clue source such as Hellhounds or Bloodvelds.
- **Skeptic receipt:** drop_rates.json line 28536 (guidance, "Getting Clues" step of "Hard Treasure Trail Rewards (Rare)"): "Obtain hard clue scrolls via PvM (Hellhounds, Kalphite Queen, etc.), skilling, or Ninja impling jars (approx 1/25 per jar) from the Grand Exchange."  Authoritative wiki (https://oldschool.runescape.wiki/w/Kalphite_Queen, drop table, via WebFetch): "the Kalphite Queen drops only elite clue scrolls. From the Tertiary drops section: 'Clue scroll (elite)' appears with a rarity of '1/100' and a note that 'The elite clue scroll drop rate increases to 1/95 after unlocking the elite Combat Achievements rewards tier.' No other clue scroll tiers (easy, medium, hard, or master) are listed in the Kalphite Queen's drop table."  Cross-check — Hellhounds (other named source / suggested replacement) DO drop hard clues (https://oldschool.runescape.wiki/w/Hellhound): "Hard Clue Scrolls: Level 122: '1/64; 1/32'; Level 127 (God Wars Dungeon): '1/64'; Level 136 (Wilderness Slayer Cave): '1/64; 1/32'."

## Elite Treasure Trail Rewards (Rare) (source #188)

### [high] Elite Treasure Trail Rewards (Rare) (field: step 1)

- **Data says:** Ranger boots are listed as a reward from elite clue scrolls.
- **Wiki says (raw):** Ranger boots are a rare reward received from the completion of medium Treasure Trails.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ranger_boots
- **Suggested fix:** Remove 'ranger boots' from the elite clue reward list. Ranger boots come from medium clue scrolls, not elite. The elite-specific equivalent footwear items (e.g. ranger gloves) or simply 'other rare rewards' should be used instead.
- **Skeptic receipt:** wiki_lookup "Ranger boots" (RAW): "Ranger boots are a rare reward received from the completion of medium Treasure Trails. It is part of the ranger kit, and requires 40 Ranged to wear... The drop rate of ranger boots is 1/1,133 per slot. With each medium clue reward, a player will receive 3-5 slots of items..."  Contrast — wiki_lookup "Rangers' tunic" (RAW): "The Rangers' tunic is a rare item received from elite Treasure Trails."  Offending data — drop_rates.json line 28851 (guidanceSteps[0].description): "Rare sub-table for elite clue scrolls... Complete elite clues for a chance at 3rd age items, ranger boots, rangers tunic, and other mega-rare rewards."

## Master Treasure Trail Rewards (Rare) (source #189)

### [high] Master Treasure Trail Rewards (Rare) (field: step 2)

- **Data says:** Master clues can be obtained as a rare drop from high-level PvM.
- **Wiki says (raw):** As a potential reward from completing any easy, medium, hard or elite treasure trails, with the chances of obtaining them being 1/50, 1/30, 1/15, and 1/5 respectively.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Clue_scroll_(master)
- **Suggested fix:** Replace 'or as a rare drop from high-level PvM' with 'or as a reward from completing any easy, medium, hard, or elite clue scroll'
- **Skeptic receipt:** wiki_lookup "Clue scroll (master)" (OSRS Wiki, https://oldschool.runescape.wiki/w/Clue_scroll_(master)), RAW: "Players can obtain a master clue scroll from the following methods: *By bringing one each of the easy, medium, hard, and elite clue scrolls to Watson, who is located in the large, fenced-in house west of the Forthos Ruin in Hosidius. ... *As a potential reward from completing any easy, medium, hard or elite treasure trails, with the chances of obtaining them being 1/50, 1/30, 1/15, and 1/5 respectively."  Our data (drop_rates.json line 29241), RAW: "description": "Obtain master clues by trading one clue of each tier (easy, medium, hard, elite) to Watson in Hosidius, or as a rare drop from high-level PvM."

## The Mimic (source #191)

### [high] The Mimic (field: step 1)

- **Data says:** Both elite and master caskets have a 1/35 chance to become a Mimic encounter.
- **Wiki says (raw):** Elite Caskets: "1/35 chance of being a mimic" | Master Caskets: "1/15 chance"
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Mimic
- **Suggested fix:** Correct step 1 to state that elite caskets have a 1/35 chance and master caskets have a 1/15 chance to become a Mimic encounter, since the rates differ by tier.
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/The_Mimic), verbatim: "The elite reward casket has a 1/35 chance of being a mimic, while master reward caskets have a 1/15 chance."  drop_rates.json line 29685 (step 1): "...Once enabled, elite and master caskets have a 1/35 chance to become a Mimic encounter."  drop_rates.json line 29694 (step 4, same defect): "Each master casket opened has approx 1/35 chance to trigger The Mimic."

## Thieving (Seed Stalls) (source #192)

### [blocker] Thieving (Seed Stalls) (field: locationDescription)

- **Data says:** The Hosidius seed stall is a valid thieving location with no guard, preferred for safer thieving.
- **Wiki says (raw):** Unlike other seed stalls, the Hosidius seed stall cannot be stolen from.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius)
- **Suggested fix:** Remove all references to the Hosidius seed stall as a thieving location. The only thievable seed stall is in Draynor Village. The locationDescription should reference Draynor Village only.
- **Skeptic receipt:** RAW wiki (https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius), ?action=raw): "Unlike other seed stalls, the Hosidius seed stall cannot be stolen from."  RAW wiki (https://oldschool.runescape.wiki/w/Seed_stall — the Thieving stall page): only thieving location listed is "Draynor Village" (2 spawns, members); "Level required [Thieving] 27", "10 xp", "2.4 seconds" restock. Hosidius is not listed; the page's hatnote routes Hosidius to the decorative "Seed Stall (Hosidius)".  Our data (drop_rates.json, "Thieving (Seed Stalls)"): L29731 locationDescription: "Draynor Village seed market, or Hosidius seed stall (no guard aggression)" L29732 travelTip: "...or Tithe Farm teleport -> Hosidius seed stall for safer thieving" L29743 guidance: "...Hosidius seed stall (Kourend) has no market guard and is preferred at higher levels..." L29753 guidance: "...At Hosidius there is no guard..."

### [blocker] Thieving (Seed Stalls) (field: travelTip)

- **Data says:** Players can teleport via Tithe Farm to reach the Hosidius seed stall for safer (guardless) thieving.
- **Wiki says (raw):** Unlike other seed stalls, the Hosidius seed stall cannot be stolen from.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius)
- **Suggested fix:** Remove the Tithe Farm teleport travel tip entirely. The Hosidius stall is non-interactive decorative scenery and cannot be stolen from. Only the Draynor Village stall is valid; the Amulet of Glory tip is correct and sufficient.
- **Skeptic receipt:** OSRS Wiki — Seed Stall (Hosidius) [https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius)], raw quote: "Unlike other seed stalls, the Hosidius seed stall cannot be stolen from." Page is categorized as "Non-interactive scenery."  OSRS Wiki — Seed Stall (Thieving) [https://oldschool.runescape.wiki/w/Seed_Stall], raw quote: "This article is about the Thieving stall. For the decorative stall, see Seed Stall (Hosidius)." Stealable stall described: Draynor Village — "Players can steal seeds from them with level 27 Thieving." The only stealable seed stall covered is the one in Draynor Village.  Current data (drop_rates.json, "Thieving (Seed Stalls)"):   "locationDescription": "Draynor Village seed market, or Hosidius seed stall (no guard aggression)"   "travelTip": "Amulet of glory -> Draynor Village; or Tithe Farm teleport -> Hosidius seed stall for safer thieving"   guidanceSteps[0].travelTip: "Amulet of glory -> Draynor Village; or Tithe Farm teleport -> Hosidius seed stall"   guidanceSteps[1].description: "...At Hosidius there is no guard..."

### [blocker] Thieving (Seed Stalls) (field: step 1)

- **Data says:** The Hosidius seed stall is a real thieving alternative with no guard, preferred at higher levels.
- **Wiki says (raw):** Unlike other seed stalls, the Hosidius seed stall cannot be stolen from.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius)
- **Suggested fix:** Remove the Hosidius stall reference from step 1. Only the Draynor Village seed stall is thievable. The step should focus solely on Draynor Village.
- **Skeptic receipt:** Raw wikitext of https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius) (via api.php action=parse prop=wikitext):  {{Otheruses|def=no|the Thieving stall|Seed Stall}} {{Infobox Scenery |name = Seed Stall |members = Yes |quest = None |location = [[Hosidius]] |options = None |examine = Plenty of seeds for sale here. |id = 6947 }} '''Seed stall''' is a decorative stall in the [[Hosidius]] square near [[Vannah's Farming Stall]]. Unlike other [[seed stalls]], the Hosidius seed stall cannot be stolen from.  Corroborating, the general page https://oldschool.runescape.wiki/w/Seed_Stall states for Draynor: "Players can steal seeds from them with level 27 Thieving." and refers Hosidius out as "For the decorative stall, see Seed Stall (Hosidius)."  Current data (drop_rates.json, "Thieving (Seed Stalls)"): - locationDescription: "Draynor Village seed market, or Hosidius seed stall (no guard aggression)" - travelTip: "...or Tithe Farm teleport -> Hosidius seed stall for safer thieving" - guidanceSteps[0]: "...Hosidius seed stall (Kourend) has no market guard and is preferred at higher levels." - guidanceSteps[1]: "...At Hosidius there is no guard."

### [blocker] Thieving (Seed Stalls) (field: step 2)

- **Data says:** Hosidius is a viable guardless thieving spot players should use.
- **Wiki says (raw):** Unlike other seed stalls, the Hosidius seed stall cannot be stolen from.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Seed_Stall_(Hosidius)
- **Suggested fix:** Remove all Hosidius guidance from step 2. The stall cannot be stolen from. Step 2 should describe only the Draynor Village stall and its Market Guard mechanic.
- **Skeptic receipt:** Seed_Stall_(Hosidius) page: "Unlike other seed stalls, the Hosidius seed stall cannot be stolen from." Categorized as "Non-interactive scenery" with no player interaction options.  Seed Stall (thieving) page: "This article is about the Thieving stall. For the decorative stall, see Seed Stall (Hosidius)." Only one thievable seed stall listed: Draynor Village, Thieving 27, 10 XP per steal, Spawns: 2, Members: Yes.  Thieving page table row: "Seed stall — 27 — 10 — Various seeds — Draynor Village" (Draynor is the only seed-stall location listed for Thieving).

## Black Chinchompas (source #193)

### [medium] Black Chinchompas (field: step 1)

- **Data says:** Spring trap OR box trap can be used to catch black chinchompas
- **Wiki says (raw):** Players must bring a box trap for each trap they plan to set up.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chinchompa_hunting
- **Suggested fix:** Remove 'spring trap' — only box traps are used to catch chinchompas. Change to: 'A box trap (bring spares) and emergency teleport are recommended.'
- **Skeptic receipt:** Box_trap page (raw WebFetch): "A box trap is a type of trap used in the Hunter skill to catch ferrets, embertailed jerboas and chinchompas." ... "The trap can capture five different creatures total: ferrets, embertailed jerboas, regular chinchompas, carnivorous chinchompas, and black chinchompas. Each requires different Hunter levels, ranging from level 27 for ferrets up to level 73 for black chinchompas."  Spring_trap page (raw WebFetch): "The server returned HTTP 404 Not Found." (no such page/item exists)  Trap page (raw WebFetch): "Is there a 'spring trap'? No, there is no trap called a 'spring trap' mentioned in the article." Hunter trap list contains: Aerial fishing, Bird house trapping, Bird snaring, Box trapping, Butterfly netting, Crab trapping, Deadfall trapping, Drift net fishing, Falconry, Net trapping, Magic box trapping, Pitfall trapping, Rabbit snaring, Tracking — no "spring trap". "Box trapping is used to catch ferrets, embertailed jerboas, and chinchompas."  Chinchompa_hunting page (raw WebFetch): "Players must bring a box trap for each trap they plan to set up. Players can use the same box trap over and over to catch multiple creatures." "Spring traps are not mentioned at all as a viable method for catching chinchompas."  Source data (drop_rates.json line 29793): "...A spring trap or box trap and emergency teleport are recommended."

## Woodcutting (Teak Trees) (source #194)

### [high] Woodcutting (Teak Trees) (field: step 3)

- **Data says:** The 2-tick method at Ape Atoll and Prifddinas uses knife on bait (tick 1) then click tree (tick 2)
- **Wiki says (raw):** The technique works by having "two monsters attacking you such that a hitsplat appears every two ticks." Players coordinate "one monster hits you on tick 1 and the second monster hits you on tick 3" within a 4-tick cycle.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pay-to-play_Woodcutting_training#Teak_logs
- **Suggested fix:** Remove the 'knife on bait' description from the 2-tick method. The 2-tick method at Ape Atoll (birds) and Prifddinas (rabbits) works by letting two monsters attack you on alternating ticks — no knife or bait items are involved. The knife-on-bait tick cycle belongs to the 1.5-tick method at Fossil Island.
- **Skeptic receipt:** WebFetch of https://oldschool.runescape.wiki/w/Pay-to-play_Woodcutting_training (raw): "2-tick woodcutting is done by auto-retaliating to being attacked every two ticks and clicking on a tree." ... "get two monsters attacking you such that a hitsplat appears every two ticks (such that in a 4-tick cycle, one monster hits you on tick 1 and the second monster hits you on tick 3)." Birds at Ape Atoll/Isle of Souls or rabbits at Prifddinas. "1.5-tick woodcutting is done by setting up a 3-tick cycle (such as using swamp tar on a clean herb) and alternating tiles next to the hardwood patches." Performed on Fossil Island.  MCP wiki_lookup "Tick manipulation" (raw), listing 3-tick-delay actions (the 1.5-tick cycle builders): "Using teak logs or mahogany logs and a knife on each other... Using kebbit claws and any vambraces on each other." (i.e., the knife/kebbit-claw actions belong to the 3-tick-cycle / 1.5-tick technique, NOT the 2-tick method.)  drop_rates.json line 29869 (current data): "2-tick method at Ape Atoll or Prifddinas: use knife on bait on tick 1, click tree on tick 2."

## Mining (Gemstone Rocks) (source #195)

### [blocker] Mining (Gemstone Rocks) (field: requirements)

- **Data says:** Only Shilo Village quest and Mining 40 are listed as requirements.
- **Wiki says (raw):** Players who have completed the Medium Karamja Diary are able to access the underground portion of the gem mine.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shilo_Village_mine
- **Suggested fix:** Add a Medium Karamja Diary requirement (diary completion) alongside the Shilo Village quest and Mining 40, since without it the player cannot enter the underground mine at all.
- **Skeptic receipt:** OSRS Wiki (Shilo Village mine), verbatim: "Players who have completed the Medium Karamja Diary are able to access the underground portion of the gem mine, which contains an additional 48 gem rocks and a bank deposit chest." "Players now only require completion of the medium Karamja Diary to enter the underground portion." "You can once again climb the ladder to the underground Shilo gem mine without having completed the Karamja elite tasks."  drop_rates.json entry "Mining (Gemstone Rocks)":   "locationDescription": "Shilo Village underground mine",   guidance step 2: "Travel to Shilo Village underground mine. ... then enter the underground mine on the south side."   guidance step 3 worldX/Y 2842/9381 (underground level), "Mine gemstone rocks ... in the underground mine."   "requirements": { "quests": ["SHILO_VILLAGE"], "skills": [{"skill":"MINING","level":40}] }   -> no diary requirement present.

### [high] Mining (Gemstone Rocks) (field: step 2)

- **Data says:** Karamja gloves 3 and 4 both teleport to Shilo Village, then the player walks to the underground mine entrance.
- **Wiki says (raw):** Players may use the Karamja gloves 3 to teleport directly to the underground mines an unlimited number of times.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shilo_Village_mine
- **Suggested fix:** Change to: 'Teleport directly to the underground mine via Karamja gloves 3 (requires Medium Karamja Diary). Karamja gloves 4 teleports to Duradel/Kuradal and does not reach Shilo Village.' Remove the 'then enter the underground mine on the south side' instruction, as gloves 3 land you inside the underground mine already.
- **Skeptic receipt:** Karamja gloves 3 (https://oldschool.runescape.wiki/w/Karamja_gloves_3): "Free, unlimited teleportation to the underground portion of the Shilo Village mine." ... "The teleport provided by these gloves is very close to a deposit chest" (arrives inside the underground mine, NOT above ground in Shilo Village town). Required tier: "the reward for completing all of the hard Karamja Diary tasks."  Shilo Village mine (https://oldschool.runescape.wiki/w/Shilo_Village_mine): "Players may use the Karamja gloves 3 to teleport directly to the underground mines an unlimited number of times."  Karamja gloves 4 (https://oldschool.runescape.wiki/w/Karamja_gloves_4): named teleport "Unlimited teleports to Duradel/Kuradal" (it also retains the gem-mine teleport option).

### [medium] Mining (Gemstone Rocks) (field: step 1)

- **Data says:** The gem bag holds up to 60 uncut gems total.
- **Wiki says (raw):** 60 of each (300 total) uncut gems
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gem_bag
- **Suggested fix:** Update to: 'The gem bag holds up to 60 of each uncut gem type (300 total) to delay banking.'
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/Gem_bag), verbatim: "It can hold 60 of each (300 total) uncut gems of the following types". Our drop_rates.json (line 29961): "Bank gems once your inventory is full. The gem bag holds up to 60 uncut gems to delay banking. Shilo Village bank is immediately above the underground mine entrance."

## Tranche 3 (sources 196-226) - 31 confirmed

## Fishing (Swordfish) (source #196)

### [high] Fishing (Swordfish) (field: step 1)

- **Data says:** Barehand swordfish fishing requires 55 Strength and Agility
- **Wiki says (raw):** Raw swordfish | 70 [Fishing] | 50 [Strength] | 100 | 10 | 110 — there is no Agility requirement listed. General barehand fishing skill requirements are: "Skills needed: Fishing 55 Fishing, Strength 35 Strength" (no Agility).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Barehand_fishing
- **Suggested fix:** Change to 'barehand fishing at 70 Fishing and 50 Strength required' — there is no Agility requirement for barehand fishing, and the swordfish-specific Strength requirement is 50, not 55.
- **Skeptic receipt:** wiki_lookup "Barehand fishing": "#REDIRECT Barbarian Training#Barehanded fishing"  WebFetch https://oldschool.runescape.wiki/w/Barbarian_Training (Barehanded fishing): "Skills needed: [Fishing] 55 Fishing, [Strength] 35 Strength" For Tuna and Swordfish: Fishing 55 (tuna), 70 (swordfish); Strength 35 (tuna), 50 (swordfish). For Sharks: Fishing 96, Strength 76. "Agility Requirement: No, there is no Agility requirement for barehanded fishing. The section header specifies only Fishing and Strength as needed skills. This differs from heavy rod fishing, which requires Agility 15."  WebFetch https://oldschool.runescape.wiki/w/Barehand_fishing (redirect target same content): "Tuna and Swordfish - Fishing: 55, Strength: 35, Agility: None mentioned. Sharks - Fishing: 96, Strength: 76, Agility: None mentioned. There is no Agility requirement for barehanded fishing—Agility is only involved in heavy rod fishing, where it grants bonus experience."  drop_rates.json line 30010 (step 1): "...or use barehand fishing at 55 Strength and Agility for higher XP..." drop_rates.json line 30032 (step 3): "Barehand fishing (55 Strength + Agility required) gives higher XP at the same drop rates."

### [high] Fishing (Swordfish) (field: step 3)

- **Data says:** Barehand fishing requires 55 Strength and Agility
- **Wiki says (raw):** Raw swordfish | 70 [Fishing] | 50 [Strength] | 100 | 10 | 110 — the article lists no Agility column or requirement. "barehanded fishing is identical in speed and catch rate as fishing with a regular harpoon, it requires 20 additional Fishing levels to fish the same fish barehanded."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Barehand_fishing
- **Suggested fix:** Change to 'Barehand fishing (70 Fishing + 50 Strength required) gives higher XP at the same drop rates.' — no Agility requirement exists, and both the Fishing level (70) and Strength level (50) are wrong in the current text.
- **Skeptic receipt:** OSRS Wiki — Barbarian_Training (https://oldschool.runescape.wiki/w/Barbarian_Training), barehanded fishing: (1) "Skills needed: 55 Fishing, 35 Strength" (2) "To barehand fish swordfish requires 70 Fishing and 50 Strength." (3) "No agility level is required for barehanded fishing." (4) "While barehanded fishing is identical in speed and catch rate as fishing with a regular harpoon, it requires 20 additional Fishing levels to fish the same fish barehanded."  Current data (drop_rates.json, "Fishing (Swordfish)"): - step 1 (line 30010): "...or use barehand fishing at 55 Strength and Agility for higher XP..." - step 3 (line 30032): "Barehand fishing (55 Strength + Agility required) gives higher XP at the same drop rates."

### [medium] Fishing (Swordfish) (field: step 2)

- **Data says:** The minimum level to enter the Fishing Guild with a boost is 63
- **Wiki says (raw):** temporary boosts can be used to boost one's level in order to enter the guild, making the lowest level to possibly enter 62 using a super fishing potion.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fishing_Guild
- **Suggested fix:** Change 'boostable from 63' to 'boostable from 62 (using a super fishing potion)'.
- **Skeptic receipt:** OSRS Wiki, Fishing Guild (via wiki_lookup, page summary):  "To enter, players must have a Fishing level of 68; temporary boosts can be used to boost one's level in order to enter the guild, making the lowest level to possibly enter 62 using a super fishing potion."  Our data (drop_rates.json, "Fishing (Swordfish)", guidanceSteps[0], line 30010): "Fishing Guild entrance requires 68 Fishing (boostable from 63). Catherby works from 50 Fishing with no guild requirement."  (guidanceSteps[1], line 30022 also says "68 Fishing, boostable" but does not quote a number, so the off-by-one error is concentrated in step 1's "boostable from 63".)

## Runecrafting (Fire Runes) (source #197)

### [medium] Runecrafting (Fire Runes) (field: travelTip)

- **Data says:** The Ring of dueling teleport destination is called "Duel Arena"
- **Wiki says (raw):** "Emir's Arena, Castle Wars Arena, Ferox Enclave, Fortis Colosseum" ... on July 10, 2024, "The 'PvP Arena' teleport option was replaced with 'Emir's Arena'."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ring_of_dueling
- **Suggested fix:** Replace "Duel Arena" with "Emir's Arena" in the travelTip.
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Ring_of_dueling — "The current teleport destinations are: 'Emir's Arena', 'Castle Wars Arena', 'Ferox Enclave', 'Fortis Colosseum'. ... on July 10, 2024, 'The PvP Arena teleport option was replaced with Emir's Arena.'"  WebFetch https://oldschool.runescape.wiki/w/Emir%27s_Arena — "The location was historically called the Duel Arena (operated by Mubariz), then was renovated and reopened as the Emir's Arena following acquisition by the Emir of Al Kharid. ... also known as the Al Kharid PvP Arena."  drop_rates.json line 30082 travelTip: "Ring of dueling -> Duel Arena (Al Kharid) -> run north-east to fire altar ruins; or Abyss ..." line 30093: "Ring of dueling to Duel Arena is the standard banking method ..." line 30099 step travelTip: "Ring of dueling -> Duel Arena -> run north-east to fire altar ruins"

### [medium] Runecrafting (Fire Runes) (field: step 1)

- **Data says:** The Ring of dueling teleport destination near Al Kharid is called "Duel Arena"
- **Wiki says (raw):** "Emir's Arena, Castle Wars Arena, Ferox Enclave, Fortis Colosseum" ... on July 10, 2024, "The 'PvP Arena' teleport option was replaced with 'Emir's Arena'."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ring_of_dueling
- **Suggested fix:** Replace "Ring of dueling to Duel Arena" with "Ring of dueling to Emir's Arena" in step 1.
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/Ring_of_dueling), teleport options: "1. Emir's Arena 2. Castle Wars Arena 3. Ferox Enclave 4. Fortis Colosseum"; update log: "The 'PvP Arena' teleport option was replaced with 'Emir's Arena' on July 10, 2024." OSRS Wiki (https://oldschool.runescape.wiki/w/Emir%27s_Arena): "a minigame that replaced the Duel Arena," "soft-launched on 6 July 2022" and "fully launched the next week on 13 July" 2022. The page does not reference any current "Duel Arena" name. Data (drop_rates.json lines 30082, 30093, 30099) reads "Ring of dueling -> Duel Arena".

## Farming (Fruit Trees) (source #198)

### [high] Farming (Fruit Trees) (field: travelTip)

- **Data says:** Skills necklace teleports to Catherby
- **Wiki says (raw):** The necklace can carry up to four charges (six if recharged at the Fountain of Rune), each of which can be used to teleport to the entrances of one of six locations: Fishing Guild, Mining Guild, Crafting Guild, Cooks' Guild, Woodcutting Guild, Farming Guild
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skills_necklace
- **Suggested fix:** Change to 'Skills necklace -> Farming Guild' (Farming Guild patch travel). For Catherby patch, use 'Catherby Teleport' or 'Camelot Teleport'.
- **Skeptic receipt:** WIKI (Skills necklace, https://oldschool.runescape.wiki/w/Skills_necklace): "The necklace can carry up to four charges (six if recharged at the Fountain of Rune), each of which can be used to teleport to the entrances of one of six locations:" [six guild entrances — Fishing Guild, Mining Guild, Crafting Guild, Cooks' Guild, Woodcutting Guild, Farming Guild]. Catherby is NOT among the destinations.  WIKI (Catherby, https://oldschool.runescape.wiki/w/Catherby) — how to reach Catherby: "*Camelot Teleport, then run south-east. *Catherby Teleport from the Lunar spellbook. *Kandarin headgear 3/4 teleport to Sherlock and run north-east. *Take a Charter ship to Catherby docks." (No skills-necklace method listed.)  DATA (drop_rates.json line 30135): "travelTip": "Spirit tree -> Gnome Stronghold (patch 1); Gnome glider to Lleyta; Brimhaven via boat; Skills necklace -> Catherby; fairy ring CIR -> Farming Guild (75+ Farming)" (Line 30146 guidance step repeats: "Catherby (skills necklace or Camelot teleport)".)  COORDINATE_HELPER distance Fishing-Guild-tele (2611,3391) -> Catherby fruit tree patch (2860,3433): "Euclidean: 252.5 tiles / Chebyshev (game movement): 249 tiles / Approx walk time: 149.4s (running)" identify (2860,3433): "Nearest known landmark: Catherby — 56 tiles away" (confirms patch coord is the Catherby patch).

### [high] Farming (Fruit Trees) (field: locationDescription)

- **Data says:** Farming Guild fruit tree patch requires 75 Farming
- **Wiki says (raw):** The advanced tier comprises the guild's north wing, requiring 85 Farming to enter.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Farming_Guild
- **Suggested fix:** Change '75+' to '85+' — the fruit tree patch is in the advanced tier which requires 85 Farming.
- **Skeptic receipt:** wiki_lookup (Farming Guild): "...each tier requiring an increasing Farming level to enter... the intermediate and advanced tier requires level 65 and 85 Farming to enter respectively."  WebFetch oldschool.runescape.wiki/w/Farming_Guild: Opening sentence verbatim — "The advanced tier comprises the guild's north wing, requiring 85 Farming to enter." Patch types in the advanced tier: 1. Fruit tree patch  2. Spirit tree patch  3. Celastrus patch  4. Redwood patch.  WebFetch oldschool.runescape.wiki/w/Fruit_tree_patch: "( 85 Farming required)" — the Farming Guild fruit tree patch requires 85 Farming, the highest among all seven fruit tree patches.  Current data (drop_rates.json line 30134): "locationDescription": "Fruit tree patches: ... Farming Guild (75+)" and (line 30135) travelTip: "...fairy ring CIR -> Farming Guild (75+ Farming)".

### [high] Farming (Fruit Trees) (field: step 1)

- **Data says:** Farming Guild fruit tree patch requires 75 Farming
- **Wiki says (raw):** The advanced tier comprises the guild's north wing, requiring 85 Farming to enter.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Farming_Guild
- **Suggested fix:** Change '75+ Farming' to '85+ Farming' in the step text.
- **Skeptic receipt:** wiki_lookup "Farming Guild" (raw): "The guild is a large greenhouse separated into three tiers, with each tier requiring an increasing Farming level to enter ... To enter the guild, a minimum Farming level of 45 is required. ... the intermediate and advanced tier requires level 65 and 85 Farming to enter respectively." Also: "Players with 85 Farming can plant a spirit tree in the advanced tier of the guild."  WebFetch https://oldschool.runescape.wiki/w/Farming_Guild (raw): "Advanced Tier (85 Farming): Fruit tree patch, Spirit tree patch, Celastrus patch, Redwood patch (requires 90 Farming) ... Fruit Tree Patch Location: The fruit tree patch is in the advanced tier, requiring 85 Farming to access. According to the guide, 'The advanced tier comprises the guild's north wing, requiring 85 Farming to enter.'"  Data (drop_rates.json lines 30134-30161, source "Farming (Fruit Trees)"): locationDescription "...Farming Guild (75+)"; travelTip "fairy ring CIR -> Farming Guild (75+ Farming)"; guidance step 1 "...Farming Guild (75+ Farming, fairy ring CIR)..."; conditionalAlternative "Farming cape: teleport directly to the Farming Guild for its fruit tree patch (75+ Farming)...".

### [medium] Farming (Fruit Trees) (field: step 2)

- **Data says:** Fruit trees take 8 growth cycles of approximately 2 hours each
- **Wiki says (raw):** 6×160 mins (16 Hours)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fruit_tree
- **Suggested fix:** Change to '6 growth cycles at ~160 minutes (about 2 hours 40 minutes) each'. The total of 16 hours is correct but the cycle count (8) and cycle duration (~2 hours) are both wrong.
- **Skeptic receipt:** Our data (drop_rates.json line 30167, "Farming (Fruit Trees)" step 2): "Wait approximately 16 hours for fruit trees to fully grow (each tree takes 8 growth cycles at ~2 hours each)."  Authoritative wiki (https://oldschool.runescape.wiki/w/Fruit_tree), growth time column for all fruit tree varieties, quoted verbatim: "6×160 mins (16 Hours)" — i.e. 6 growth cycles, each 160 minutes.

## Underwater Crabs (source #199)

### [high] Underwater Crabs (field: step 2)

- **Data says:** Fairy ring AIQ teleports to the Feldip area, requiring a run north-west to reach Mudskipper Point.
- **Wiki says (raw):** A I Q — Asgarnia: Mudskipper Point — nearby: Mogres, Rimmington, Port Sarim, Asgarnia Ice Dungeon
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Fairy ring AIQ teleports directly to Mudskipper Point (no run required).
- **Skeptic receipt:** drop_rates.json (Underwater Crabs, step index 1): "description": "Travel to Mudskipper Point. Fairy ring AIQ teleports to the Feldip area; run north-west to Mudskipper Point. Alternatively amulet of glory to Draynor Village then run south-west along the coast." "travelTip": "Fairy ring AIQ -> Mudskipper Point (run NW); ..."  OSRS Wiki (https://oldschool.runescape.wiki/w/Fairy_rings), fairy ring code table, RAW: "AIQ [Asgarnia](/w/Asgarnia): [Mudskipper Point](/w/Mudskipper_Point)" And, for the Feldip area: "AKS [Feldip Hills]: [Feldip Hunter area]" Wiki, confirming AIQ is NOT near Feldip: "No, AIQ does not teleport to Feldip Hills... it teleports to Asgarnia's Mudskipper Point instead, which is a different region entirely."

## Gnome Restaurant (Seed Pods) (source #200)

### [medium] Gnome Restaurant (Seed Pods) (field: step 2)

- **Data says:** All four customers (Wingstone, Penwie, Brambickle, Prof. Manglethorp) give both Mint cake and Grand seed pod.
- **Wiki says (raw):** Grand seed pod: Only available from Wingstone, Penwie, Brambickle, and Professor Manglethorp. Mint cake: Only available from Wingstone, Penwie, and Brambickle.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gnome_Restaurant
- **Suggested fix:** Clarify that Prof. Manglethorp gives Grand seed pod but NOT Mint cake. Rewrite as: 'Only Wingstone, Penwie, Brambickle, and Prof. Manglethorp give Grand seed pod; only Wingstone, Penwie, and Brambickle also give Mint cake.'
- **Skeptic receipt:** Raw OSRS wiki source (https://oldschool.runescape.wiki/w/Gnome_Restaurant?action=raw): {{DropsLineReward|name=Grand seed pod|quantity=5|rarity=23/512|...|raritynotes=<ref group=d>Only available from [[Wingstone]], [[Penwie]], [[Brambickle]], and [[Professor Manglethorp]].</ref>}} {{DropsLineReward|name=Mint cake|quantity=1|rarity=23/512|...|raritynotes=<ref group=d>Only available from [[Wingstone]], [[Penwie]], and [[Brambickle]].</ref>}}  Our data (drop_rates.json line 30313, "Gnome Restaurant (Seed Pods)" step 2): "Ask Gianne jnr. for a hard delivery ... If the assigned customer is not Wingstone, Penwie, Brambickle, or Prof. Manglethorp, decline ... Only these four customers give Mint cake or Grand seed pod." And line 30342 (step 4): "Re-roll orders until Wingstone, Penwie, Brambickle, or Prof. Manglethorp is assigned to maximise seed pod and mint cake drop rate."

## Stingray (Boat Combat) (source #201)

### [high] Stingray (Boat Combat) (field: step 2)

- **Data says:** Stingrays are framed as random sea encounters that require a cannon facility to fight
- **Wiki says (raw):** Stingrays are monsters that can be found in the Unquiet Ocean, Sunset Ocean, and Shrouded Ocean. They can be assigned as bounty tasks starting at level 50 Sailing.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Stingray
- **Suggested fix:** Remove the 'sea encounters requiring a cannon facility (28 Sailing)' framing. Stingrays are found at fixed locations in named ocean regions (Unquiet Ocean, Sunset Ocean, Shrouded Ocean). The wiki documents no cannon requirement — they can be safespotted across rock formations. Update to reflect fixed-location ocean monster that can be assigned as a bounty task at level 50 Sailing.
- **Skeptic receipt:** RAW wiki_lookup (mcp runelite-dev): "NPC IDs: 15218 | Combat level: 92 | Hitpoints: 106 | Members: Yes" with a 52-entry direct drop table (Ray barbs 1/30, Stingray fin 1/2, etc.) -- a directly-killable monster, not a cannon-target encounter.  RAW wiki wikitext (oldschool.runescape.wiki/w/Stingray?action=raw): - Combat method: 'The word "cannon" does not appear anywhere in the wikitext.' Strategy passage quoted verbatim: "Stingrays west of the [[Colossal Wyrm Remains]] can be safespotted across many of the nearby rock formations, and they can not deal damage to boats with an [[adamant keel]] or better." - Locations (intro): "Stingrays are monsters that can be found in the [[Unquiet Ocean]], [[Sunset Ocean]], and [[Shrouded Ocean]]." Bounty: "They can be assigned as [[bounty tasks]] starting at level 50 [[Sailing]]."  OUR DATA (drop_rates.json line 30390): "Board your boat and sail into open water. Stingrays are sea encounters requiring a cannon facility (28 Sailing) with cannonballs loaded. No special mechanics -- fire cannon until dead, then Harvest the carcass for Ray barbs (1/30)." Same entry's structured fields contradict the cannon prose: "interactAction": "Attack", "completionCondition": "ACTOR_DEATH", "completionNpcId": 15218, recommendedItemIds [32399, 31916, 385] (sailors' amulet, food/gear -- no cannon, no cannonballs).

## Barracuda Salvage (source #209)

### [medium] Barracuda Salvage (field: travelTip)

- **Data says:** The Sailors' amulet can teleport to any port.
- **Wiki says (raw):** It allows players to access four distinct locations after unlocking their corresponding Sailors' Markers. The Pandemonium (unlocked by default), Port Roberts (requires Sailing 50), Red Rock (requires Sailing 52 and partial completion of "The Red Reef" quest), Deepfin Point (requires Sailing 67).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sailors%27_amulet
- **Suggested fix:** Change "Sailors' amulet to any port" to "Sailors' amulet to one of four specific ports (e.g. The Pandemonium by default)" or similar wording that reflects the amulet only teleports to 4 unlockable destinations, not any port.
- **Skeptic receipt:** wiki_lookup (https://oldschool.runescape.wiki/w/Sailors'_amulet), raw: "The amulet allows the player to teleport to the Pandemonium, Port Roberts, Red Rock, and Deepfin Point. In order to make use of the amulet's destinations, the player must activate their corresponding Sailors' Marker there." ... "The teleport to The Pandemonium is unlocked by default. To use the teleports to Port Roberts, Red Rock, and Deepfin Point, the player must first inspect the Sailors' Marker on their docks. The Red Rock teleport requires partial completion of the quest The Red Reef to land at the port and be able to inspect the Sailors' Marker."  WebFetch (same URL), raw: "The amulet provides four teleport destinations, not 'any port': 1. The Pandemonium - Unlocked by default 2. Port Roberts - Requires Sailing level 50 3. Red Rock - Requires Sailing level 52 and partial completion of 'The Red Reef' quest 4. Deepfin Point - Requires Sailing level 67."  Data (drop_rates.json line 31013): "travelTip": "Sailors' amulet to any port, board boat, sail to salvage"

## Large Salvage (source #210)

### [blocker] Large Salvage (field: requirements)

- **Data says:** Sailing level 45 is listed as the skill requirement for Large Salvage
- **Wiki says (raw):** Large salvage ... Level 53 Sailing is needed to salvage large shipwrecks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Large_salvage
- **Suggested fix:** Change the Sailing level requirement from 45 to 53
- **Skeptic receipt:** Wiki page "Large salvage" (https://oldschool.runescape.wiki/w/Large_salvage), raw wiki_lookup output: "Large salvage is an item recovered from large shipwrecks via shipwreck salvaging. Level 53 Sailing is required to salvage these, granting 48 experience."  Corroborating raw wiki_lookup output, "Shipwreck salvaging" table: "| Level | Shipwreck | ... |15 | Small shipwreck ... |26 | Fisherman's shipwreck ... |35 | Barracuda shipwreck ... |53 | Large shipwreck | ... | 48 | ... | 24 |"  Our data (drop_rates.json line 31095-31102): requirements.skills = [ { "skill": "SAILING", "level": 45 } ].

## Plundered Salvage (source #211)

### [blocker] Plundered Salvage (field: requirements)

- **Data says:** Sailing level 55 is required to obtain plundered salvage
- **Wiki says (raw):** Level 64 Sailing is required to salvage these, granting 76 experience.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Plundered_salvage
- **Suggested fix:** Change the Sailing requirement level from 55 to 64
- **Skeptic receipt:** MCP wiki_lookup ("Plundered salvage"), raw: "Plundered salvage is an item recovered from pirate shipwrecks via shipwreck salvaging. Level 64 Sailing is required to salvage these, granting 76 experience." (https://oldschool.runescape.wiki/w/Plundered_salvage)  Independent WebFetch of same URL, raw quote: "Level 64 Sailing is required to salvage these, granting 76 experience."  Our data (drop_rates.json, source "Plundered Salvage", lines 31160-31167):   "requirements": { "skills": [ { "skill": "SAILING", "level": 55 } ] }

## Martial Salvage (source #212)

### [blocker] Martial Salvage (field: requirements)

- **Data says:** Sailing level 65 is required for Martial Salvage
- **Wiki says (raw):** Level 73 Sailing is required to salvage these (mercenary shipwrecks, which yield martial salvage)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Martial_salvage
- **Suggested fix:** Change the SAILING requirement level from 65 to 73
- **Skeptic receipt:** MCP wiki_lookup ("Martial salvage"): "Martial salvage is an item recovered from mercenary shipwrecks via shipwreck salvaging. Level 73 Sailing is required to salvage these, granting 138 experience."  WebFetch (https://oldschool.runescape.wiki/w/Martial_salvage), exact sentence: "Level 73 Sailing is required to salvage these, granting 138 experience."  drop_rates.json (Martial Salvage entry, lines 31232-31239): "requirements": { "skills": [ { "skill": "SAILING", "level": 65 } ] }

## Fremennik Salvage (source #213)

### [blocker] Fremennik Salvage (field: requirements)

- **Data says:** Sailing level 70 is required
- **Wiki says (raw):** Level 80 Sailing is required to salvage these, granting 162 experience.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fremennik_salvage
- **Suggested fix:** Change the SAILING skill requirement from level 70 to level 80.
- **Skeptic receipt:** MCP wiki_lookup("Fremennik salvage") raw output: "Fremennik salvage is an item recovered from Fremennik shipwrecks via shipwreck salvaging. Level 80 Sailing is required to salvage these, granting 162 experience. When sorted at a salvaging station (or the boat facility), players will additionally receive 75 Sailing experience and loot."  WebFetch (https://oldschool.runescape.wiki/w/Fremennik_salvage) raw quote: "Level 80 Sailing is required to salvage these, granting 162 experience."  Our drop_rates.json (lines 31290-31297) requirements: { "skills": [ { "skill": "SAILING", "level": 70 } ] }

## Opulent Salvage (source #214)

### [high] Opulent Salvage (field: requirements)

- **Data says:** Sailing level 80 required
- **Wiki says (raw):** A minimum of 87 Sailing is required to salvage these items from merchant shipwrecks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Opulent_salvage
- **Suggested fix:** Change SAILING requirement level from 80 to 87.
- **Skeptic receipt:** wiki_lookup("Opulent salvage") raw output: "Opulent salvage is an item recovered from merchant shipwrecks via shipwreck salvaging. Level 87 Sailing is required to salvage these, granting 200 experience if a player is using the salvaging hook." NPC IDs: 32861 | Members: Yes URL: https://oldschool.runescape.wiki/w/Opulent_salvage  drop_rates.json (lines 31383-31390), source "Opulent Salvage": "requirements": {   "skills": [     {       "skill": "SAILING",       "level": 80     }   ] }

## Mithril Dragon (source #215)

### [high] Mithril Dragon (field: step 1)

- **Data says:** Barbarian Fishing training is required to access the whirlpool/Ancient Cavern.
- **Wiki says (raw):** To access the Ancient Cavern, one must have completed the first part of Barbarian Firemaking training and then talked to Otto about pyre ships.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mithril_dragon
- **Suggested fix:** Replace 'Barbarian Fishing training is required to dive into the whirlpool' with 'The pyre ship section of Barbarian Firemaking training (Barbarian Training miniquest) is required to dive into the whirlpool.'
- **Skeptic receipt:** OSRS Wiki (https://oldschool.runescape.wiki/w/Ancient_Cavern), via wiki_lookup raw output:  "The Ancient Cavern is an area reached from the whirlpool near Otto Godblessed's house. Players must dive into the whirlpool to enter the cavern...  The cavern can only be accessed after the player has completed the first part of Barbarian Firemaking, requiring level 35 Firemaking. After that, the player must ask Otto about making pyre ships, and then will be granted access to the dungeon in order to fulfil the objectives required to make them. If a player dives into the whirlpool before completing these tasks, they will be swept away...  To access the dungeon the player must have learned about funeral pyre burning, in the Barbarian Training miniquest from Otto Godblessed."  Data under audit (drop_rates.json line 31623, guidanceSteps[0].description): "Travel to Otto's Grotto, south of the Barbarian Outpost (games necklace). Barbarian Fishing training is required to dive into the whirlpool."

### [medium] Mithril Dragon (field: step 3)

- **Data says:** Chewed bones unlock the Ourg bones prayer XP method.
- **Wiki says (raw):** Chewed bones can be offered on a pyre ship, providing a rare reward avenue for obtaining the dragon full helm.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mithril_dragon
- **Suggested fix:** Replace 'Chewed bones unlock the Ourg bones prayer XP method' with 'Chewed bones can be offered on a pyre ship for a chance at the Dragon full helm.'
- **Skeptic receipt:** Wiki (Chewed_bones), raw: "Chewed bones are obtained exclusively as a drop from mithril dragons in the Ancient Cavern at a rate of 3/128... The bones can be burnt on a pyre ship for rewards, including the dragon full helm... Burning chewed bones via a pyre ship provides a 300% experience bonus to the next few bones buried, including those buried by the bonecrusher, with the amount varying by which logs are burnt."  Wiki (Ourg_bones), raw: "Ourg bones are bones obtained by killing Slash Bash, the boss from Zogre Flesh Eaters, stealing them from ogre coffins via the Thieving skill, or using keys dropped by zogres and skogres on nearby chests... Ourg bones give 140 Prayer experience when buried."  Data (drop_rates.json line 31645), raw: "Kill mithril dragons in the Ancient Cavern. Use extended antifire and Protect from Magic. Chewed bones unlock the Ourg bones prayer XP method. Ancient pages and Dragon full helm are the rarest drops."

## Armoured Zombie (source #219)

### [blocker] Armoured Zombie (field: requirements)

- **Data says:** Quest requirement: DEFENDER_OF_VARROCK
- **Wiki says (raw):** Armoured zombie (Zemouregal's Fort), found in Zemouregal's Fort during and after The Curse of Arrav quest
- **Wiki URL:** https://oldschool.runescape.wiki/w/Armoured_zombie
- **Suggested fix:** Change quest requirement to THE_CURSE_OF_ARRAV. The drops listed (broken zombie axe, broken zombie helmet, champion scroll) match the Fort variant, which requires The Curse of Arrav — not Defender of Varrock. The Base variant (Defender of Varrock) does not drop broken zombie helmet at all.
- **Skeptic receipt:** Wiki disambiguation (wiki_lookup "Armoured zombie"): "* Armoured zombie (Zemouregal's Base), found in Zemouregal's Base during and after the Defender of Varrock quest  * Armoured zombie (Zemouregal's Fort), found in Zemouregal's Fort during and after The Curse of Arrav quest"  Wiki drop table — Armoured zombie (Zemouregal's Fort): "Broken zombie helmet | qty: 1 | rarity: 1/600  Broken zombie axe | qty: 1 | rarity: 1/600  Zombie champion scroll | qty: 1 | rarity: 1/5000"  Wiki drop table — Armoured zombie (Zemouregal's Base) [NO broken zombie helmet]: "Broken zombie axe | qty: 1 | rarity: 1/800  Zombie champion scroll | qty: 1 | rarity: 1/5000"  drop_rates.json current entry (lines 31954-31977): "itemId": 30324, "name": "Broken zombie helmet", "dropRate": 0.001667  (= 1/600, Fort-exclusive) "itemId": 28813, "name": "Broken zombie axe", "dropRate": 0.001667     (= 1/600, the Fort rate; Base is 1/800) "locationDescription": "Zemouregal's Fort, north of Varrock" "requirements": { "quests": [ "DEFENDER_OF_VARROCK" ] } guidance: "Kill Armoured Zombies at Zemouregal's Fort..."

### [high] Armoured Zombie (field: locationDescription)

- **Data says:** locationDescription: Zemouregal's Fort, north of Varrock
- **Wiki says (raw):** Zemouregal's Fort is situated in Troll Country, within the northern regions of the game world. It is positioned between Trollweiss Mountain to the west and Ghorrock to the east.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zemouregal%27s_Fort
- **Suggested fix:** Change to 'Zemouregal's Fort, in Troll Country (far north, between Trollweiss Mountain and Ghorrock)'. The fort is not north of Varrock — it is in the extreme northern part of the map, in Troll Country.
- **Skeptic receipt:** npcId verification (cache + wiki): drop_rates.json "Armoured Zombie" uses npcId 14113. abextm cache: npcId 14113 = "Armoured zombie". OSRS Wiki (Armoured zombie (Zemouregal's Fort)): "Melee variants: IDs 14113-14117 / Ranged variants: IDs 14118-14122. Combat Level: 105. Required Quest: The Curse of Arrav - the zombies appear 'after the completion of The Curse of Arrav.' ... found in the tunnels west of the basement in Zemouregal's Fort." So npcId 14113 is unambiguously the Zemouregal's Fort variant.  Location of Zemouregal's Fort (OSRS Wiki, https://oldschool.runescape.wiki/w/Zemouregal%27s_Fort): "located in [the North]" and categorized under "Troll Country". Map: "Trollweiss Mountain <- Zemouregal's Fort -> Ghorrock". "can be accessed via a tunnel found on the peak of Trollweiss Mountain."  drop_rates.json (lines 31969): "locationDescription": "Zemouregal's Fort, north of Varrock".

## Cutting Squid (source #220)

### [medium] Cutting Squid (field: step 1)

- **Data says:** Jumbo squid give 1/512 per cut; swordtip squid give 1/1024. Sailing 47 and Fishing 52 required (lantern harpooning).
- **Wiki says (raw):** Fishing: 52 minimum (to catch swordtip squid); 69 (to catch jumbo squid)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lantern_harpooning
- **Suggested fix:** Jumbo squid require Fishing 69, not Fishing 52. The step implies both squid types are accessible at Fishing 52. Revise the requirements note to clarify: 'Sailing 47 and Fishing 52 required (swordtip squid); Fishing 69 required for jumbo squid (better beak rate).'
- **Skeptic receipt:** wiki_lookup (Lantern harpooning), raw: "Lantern harpooning is a Fishing activity released with Sailing, requiring at least 47 Sailing and 52 Fishing... Players with 52 Fishing can catch swordtip squid, while players with 69 Fishing can catch jumbo squid as well."  WebFetch (https://oldschool.runescape.wiki/w/Lantern_harpooning), raw: Fishing Levels - "Players with 52 Fishing can catch swordtip squid, while players with 69 Fishing can catch jumbo squid as well." Drop rates - Swordtip squid "1/1,024"; Jumbo squid "1/512".  drop_rates.json line 32029 (Cutting Squid, step 1): "...Jumbo squid give 1/512 per cut; swordtip squid give 1/1024. Sailing 47 and Fishing 52 required (lantern harpooning)..."

## Prifddinas Elf (source #221)

### [medium] Prifddinas Elf (field: step 2)

- **Data says:** Ardougne cloak 4 reduces pickpocket failure rate significantly.
- **Wiki says (raw):** 10% increased chance of successfully pickpocketing anywhere around Gielinor (Hard tier reward — Ardougne cloak 3). The Elite tier (Ardougne cloak 4) does not list any additional thieving bonuses beyond what's granted at the Hard level.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ardougne_Diary
- **Suggested fix:** Change 'Ardougne cloak 4' to 'Ardougne cloak 3' — the pickpocket success boost (10% increased chance) is a Hard Ardougne Diary reward (cloak 3); the Elite diary (cloak 4) grants no additional Thieving bonus.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Ardougne_Diary), raw extract: "There are two tiers that grant pickpocketing/thieving bonuses: Medium Tier (Ardougne cloak 2): '10% increased chance of successfully pickpocketing within Ardougne' Hard Tier (Ardougne cloak 3): '10% increased chance of successfully pickpocketing anywhere around Gielinor' The key difference is scope: the Medium reward applies only in Ardougne, while the Hard reward extends the bonus globally across the game world."  Wiki (https://oldschool.runescape.wiki/w/Ardougne_cloak), raw: "Ardougne cloak 3, from the hard tasks set / Ardougne cloak 4, from the elite tasks set" — the cloak tier maps to its diary tier; no additional thieving bonus is granted at the Elite (cloak 4) tier.  Data (drop_rates.json line 32125): "Wearing the Ardougne cloak 4 or a dodgy necklace reduces failure rate significantly."

### [medium] Prifddinas Elf (field: step 2)

- **Data says:** The dodgy necklace reduces pickpocket failure rate.
- **Wiki says (raw):** While equipped, the necklace provides a 25% chance to prevent the player from being stunned and damaged while pickpocketing NPCs.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dodgy_necklace
- **Suggested fix:** The dodgy necklace does not reduce the failure rate — it gives a 25% chance to avoid the stun and damage when a pickpocket fails. Rewrite to: 'The dodgy necklace gives a 25% chance to avoid the stun and damage on a failed pickpocket.'
- **Skeptic receipt:** RAW wiki_lookup (Dodgy necklace, https://oldschool.runescape.wiki/w/Dodgy_necklace): "A dodgy necklace is an opal necklace enchanted via the Lvl-1 Enchant spell. While equipped, the necklace provides a 25% chance to prevent the player from being stunned and damaged while pickpocketing NPCs. The necklace begins with 10 charges; one charge is consumed each time the necklace successfully prevents damage. When the last charge is consumed, the necklace crumbles to dust... Depending on the NPC being pickpocketed, one necklace can prevent 10 to 50 damage and 40 to 60 seconds of being stunned."  RAW data (drop_rates.json line 32125, Prifddinas Elf, guidanceSteps[1], section "Pickpocket"): "Pickpocket elves in Prifddinas (85 Thieving). Each successful pickpocket has a 1/1024 chance of yielding an enhanced crystal teleport seed. Wearing the Ardougne cloak 4 or a dodgy necklace reduces failure rate significantly."

## Waterfiend (source #223)

### [high] Waterfiend (field: step 1)

- **Data says:** Barbarian Fishing training is the prerequisite to enter the Ancient Cavern via the whirlpool.
- **Wiki says (raw):** The cavern can only be accessed after the player has completed the first part of Barbarian Firemaking, requiring level 35 Firemaking.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ancient_Cavern
- **Suggested fix:** Change "Barbarian Fishing training" to "Barbarian Firemaking training (level 35 Firemaking)" to match the wiki's stated access requirement.
- **Skeptic receipt:** OSRS Wiki — Ancient Cavern (via wiki_lookup, https://oldschool.runescape.wiki/w/Ancient_Cavern): "The cavern can only be accessed after the player has completed the first part of Barbarian Firemaking, requiring level 35 Firemaking. After that, the player must ask Otto about making pyre ships, and then will be granted access to the dungeon... To access the dungeon the player must have learned about funeral pyre burning, in the Barbarian Training miniquest from Otto Godblessed."  OSRS Wiki — Barbarian Training (via wiki_lookup, https://oldschool.runescape.wiki/w/Barbarian_Training): "Barbarian Training is a miniquest that explores a set of expansions to Farming, Firemaking, Herblore, Fishing, and Smithing. The miniquest provides access to the Ancient Cavern..." (Firemaking and Fishing are listed as distinct sub-sections.)  Our data (drop_rates.json, Waterfiend source, guidanceSteps[0].description, line 32214): "Travel to Otto's Grotto, south of the Barbarian Outpost (games necklace). Barbarian Fishing training is required to dive into the whirlpool."

## Port Tasks (source #224)

### [high] Port Tasks (field: step 2)

- **Data says:** Shark paint is obtained as a salvage reward at 1/36
- **Wiki says (raw):** Completing any port task awards the player with Sailing experience, coins, and occasionally resources such as shark paint.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sailing#Port_tasks
- **Suggested fix:** Shark paint comes from completing port tasks, not from salvage. The source label '1/36 salvage reward' is wrong. Shark paint should be described as a port task reward (numeric rate is out of scope for this audit).
- **Skeptic receipt:** OSRS Wiki (Shark_paint page, raw via wiki_lookup): "Shark paint is received at a rate of 1/36 upon completing a port task." WebFetch of Shark_paint: "Shark paint is not obtainable from shipwreck salvaging. The wiki makes no mention of salvage as a source for this item. It comes only from the port task activity at a 1-in-36 drop rate." OSRS Wiki (Sailing#Port tasks, raw): "Completing any port task awards the player with Sailing experience, coins, and occasionally resources such as shark paint." Sailing skill confirmed live: "Sailing is a members-only skill... Released on 19 November 2025." Data under audit (drop_rates.json line 32295): "...Soup (1/4,500 task reward) and Shark paint (1/36 salvage reward). Tasks are repeatable and vary by port and Sailing level."

## Prifddinas Rabbit (source #225)

### [blocker] Prifddinas Rabbit (field: locationDescription)

- **Data says:** The rabbit is located in the south-east corner of Prifddinas, in the Ithell district.
- **Wiki says (raw):** The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rabbit_(Prifddinas)
- **Suggested fix:** Change to something like: "Gwenith Hunter area cave, north of Prifddinas" — the rabbit is outside the city walls, not in the south-east Ithell district.
- **Skeptic receipt:** OSRS Wiki — Crystal_grail (verbatim): "The crystal grail is obtained as a drop from killing the powerful rabbit located in the cave in the Gwenith Hunter area north of Prifddinas, requiring completion of Song of the Elves to access."  OSRS Wiki — Rabbit_(Prifddinas): "The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas." Location table: Location = Elven rabbit cave; Coordinates x:3295, y:12581; Plane 0; Region Tirannwn.  wiki_lookup (Rabbit (Prifddinas)): NPC ID 9118, Combat level 2, drops Crystal grail (Always) and Rabbit bone (1/4).  Current data (drop_rates.json line 32333): "locationDescription": "Prifddinas, south-east corner (Ithell district)" with worldX 3267, worldY 6082, and guidance steps repeating "south-east corner of the city (Ithell district)".

### [blocker] Prifddinas Rabbit (field: step 1)

- **Data says:** The rabbit spawns in the south-east corner of Prifddinas (Ithell district).
- **Wiki says (raw):** The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rabbit_(Prifddinas)
- **Suggested fix:** Change to: "The rabbit is found in a cave in the Gwenith Hunter area north of Prifddinas. From the city, exit through the northern gates, head north, then turn east beneath the rope bridge."
- **Skeptic receipt:** OUR DATA (drop_rates.json, "Prifddinas Rabbit", npcId 9118): - "locationDescription": "Prifddinas, south-east corner (Ithell district)" - guidanceSteps[0].description: "Travel to Prifddinas ... The rabbit spawns in the south-east corner of the city (Ithell district)." - guidanceSteps[1].description: "Locate and kill the Prifddinas Rabbit in the south-east corner of the city." - worldX 3267, worldY 6082, worldPlane 0  AUTHORITATIVE WIKI (https://oldschool.runescape.wiki/w/Rabbit_(Prifddinas)): Rendered page: "The rabbit is found in a cave in the Gwenith Hunter area north of Prifddinas." Raw wikitext: "The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas." / Infobox Location field: "Elven rabbit cave" wiki_lookup confirms NPC IDs: 9118, Combat level: 2, drops Crystal grail (Always) + Rabbit bone (1/4).

### [blocker] Prifddinas Rabbit (field: step 2)

- **Data says:** The rabbit is in the south-east corner of the city.
- **Wiki says (raw):** The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rabbit_(Prifddinas)
- **Suggested fix:** Change to reference the Gwenith Hunter area cave north of Prifddinas rather than the south-east corner of the city.
- **Skeptic receipt:** WIKI (WebFetch, https://oldschool.runescape.wiki/w/Rabbit_(Prifddinas)) raw quote: "The rabbit is a monster that is found in a cave in the Gwenith Hunter area north of Prifddinas." "It is not located in the south-east corner of the city. The cave itself is called the Elven rabbit cave, and there is only one spawn point for this creature within that cave."  wiki_lookup (TempleOSRS/wiki drop table) confirms identity: "Drop table for Rabbit (Prifddinas) (2 entries): ... NPC IDs: 9118 | Combat level: 2 | Crystal grail | qty: 1 | rarity: Always | Rabbit bone | qty: 1 | rarity: 1/4"  npc_spawns (mejrs/data_osrs) raw quote — independent corroboration of a NORTH location, not the city's south-east corner: "NPC 9118: \"Rabbit\" (lvl 2) [Attack]  Spawns: 1  (3295, 12581, plane 0)"  coordinate_helper identify on the actual spawn vs our data: spawn (3295, 12581, plane 0) -> "Region: 13252 (51, 196)" (far north, no Prifddinas-city landmark) our data (3267, 6082, plane 0) -> "Within known areas: Prifddinas — 17 tiles from center ... Region: 13151 (51, 95)"  OUR DATA (drop_rates.json, "Prifddinas Rabbit"): locationDescription: "Prifddinas, south-east corner (Ithell district)" guidanceSteps[1].description: "Locate and kill the Prifddinas Rabbit in the south-east corner of the city..." worldX 3267 / worldY 6082 (resolves to Prifddinas city center, NOT the cave spawn).

## Pickpocketing Darkmeyer Vyre (source #226)

### [high] Pickpocketing Darkmeyer Vyre (field: travelTip)

- **Data says:** From the Kharyrll teleport landing in Canifis, run north to reach Darkmeyer
- **Wiki says (raw):** Darkmeyer is the capital city of Morytania located in the Sanguinesti region, north of Meiyerditch. [...] South of Haunted Woods, West of Slepe. [Canifis wiki] Slayer Tower to the north, Haunted Woods to the east, Mort Myre Swamp to the south
- **Wiki URL:** https://oldschool.runescape.wiki/w/Darkmeyer
- **Suggested fix:** Change 'run north' to 'run south-east' (through Mort Myre Swamp toward Meiyerditch, then into the Sanguinesti region). Darkmeyer is deep in the south-east of Morytania; Canifis is in the north-west. North of Canifis leads to the Slayer Tower, not Darkmeyer.
- **Skeptic receipt:** Data (drop_rates.json line 32387): "travelTip": "Drakan's medallion -> Darkmeyer; or Kharyrll teleport (Ancient Magicks) -> run north"; source worldX/worldY = 3631/3325.  coordinate_helper identify (3631,3325): "Darkmeyer — 27 tiles from center" / Region 14387. coordinate_helper identify (3493,3473) [Kharyrll/Canifis landing]: "Canifis — 16 tiles from center" / Region 13878. coordinate_helper distance (3493,3473)->(3631,3325): "Chebyshev (game movement): 148 tiles". Vector = +138 x (east), -148 y (SOUTH).  wiki_lookup Darkmeyer (https://oldschool.runescape.wiki/w/Darkmeyer): "Darkmeyer is the capital city of Morytania located in the Sanguinesti region, north of Meiyerditch. ... Darkmeyer and Meiyerditch ... with Darkmeyer making up the northern half of the region and Meiyerditch the southern half." (Sanguinesti is the deep south-east of Morytania; Canifis sits in the north-west.)
