# Guidance-quality backlog — deep-guidance-bar audit

Discovery-only worklist for raising every source in `drop_rates.json` to the [10-element deep-guidance bar](contributor-guide/deep-guidance-bar.md). **No source data was modified to produce this file.** Each of the 225 sources was run through the `runelite-dev-toolkit` `guidance_lint` checks and scored against the bar's 10 elements. Rows are sorted worst-first; **BOSSES and SLAYER are listed first** (highest player traffic).

## How this was produced

The `guidance_lint` MCP tool was not reachable as a live server in the audit session, so its per-source checks (`mcp/src/index.ts` tool 16) were replicated **verbatim** in a standalone Node script and run against `src/main/resources/com/collectionloghelper/drop_rates.json`. The `Lint` column reports the resulting CRITICAL (`C`) and WARNING (`W`) counts per source.

### Element scoring legend

Each element is **✓ met** (present in the data), **– n/a** (not applicable / waived for this category under the [long-tail bar](contributor-guide/deep-guidance-bar.md#long-tail-bar-relaxed-for-low-engagement-sources)), or **✗ missing** (applicable but absent). **Bar score = (met + n/a) out of 10** — the bar defines a fully-authored source as all elements *"present or explicitly waived."* The **Missing elements** column lists only the **✗** items: the concrete authoring work.

Detection rules (all derived from JSON fields — no in-game capture):

- **E1** — `travelTip` set; if the source is quest-gated, a `waypoints` fallback must exist. Missing if no tip, or quest-gated with no `waypoints`.
- **E2** — at least one step with `ARRIVE_AT_TILE` / `ARRIVE_AT_ZONE` / `PLAYER_ON_PLANE`.
- **E3** — for combat sources (an `ACTOR_DEATH` step exists), a step description names a prayer / gear / spec. N/A for non-combat.
- **E4** — `loopBackToStep` present. N/A for single-kill bosses and one-shot OTHER sources; flagged missing only for MINIGAMES / SKILLING that lack a declared loop.
- **E5** — for combat sources, a step description names a safespot / mechanic / phase. N/A for non-combat.
- **E6 / E7 / E8** — scored from data fields only (`requiredItemIds`, `requirements`). Absent ⇒ N/A, **not** missing: description-keyword detection produced false positives on teleport item names ("Key master teleport", "teleport scroll") and gate-free bosses (Giant Mole, KBD) legitimately carry no `requirements`. E7 is flagged missing only when a source has `requiredItemIds` somewhere but **not** on its travel/arrival step (loadout not surfaced early).
- **E9** — every item `dropRate` is positive and the best per-source precision is **≥ 6 decimal places** (the bar's literal standard). Flags both lazily-rounded rates (`0.02`, `0.001`) and a few precise-but-5-dp sources that warrant a confidence re-check.
- **E10** — PR-description obligation (game version, data-source citations). Not encodable in JSON, so it is **N/A** for every row and tracked at PR time, not here.

> Because E6/E7/E8/E10 are non-penalising when absent, the live authoring discriminators are **E1, E3, E5, E9** (and the rarer E2/E4/E7). A source scoring 10/10 here is structurally at-bar in the data; E10 citations are still required in its PR.

## Summary

- **Sources audited:** 225
- **Bar-score distribution:** 6/10 → 1, 7/10 → 16, 8/10 → 31, 9/10 → 90, 10/10 → 87
- **guidance_lint totals:** 28 CRITICAL, 61 WARNING, 382 INFO across all sources
- **Missing-element frequency:** E1 ×77, E2 ×3, E3 ×9, E4 ×8, E5 ×31, E7 ×8, E9 ×68

### guidance_lint CRITICAL findings (28 sources)

These are structural defects flagged at CRITICAL severity — fix alongside the bar work:

| Source | Category | CRITICAL | WARN |
|--------|----------|----------|------|
| Aerial Fishing | SKILLING | 1 | 0 |
| Barbarian Assault | MINIGAMES | 1 | 0 |
| Brimhaven Agility Arena | MINIGAMES | 1 | 0 |
| Castle Wars | MINIGAMES | 1 | 0 |
| Catacombs of Kourend | OTHER | 1 | 0 |
| Champion's Challenge | OTHER | 1 | 0 |
| Cutting Squid | SKILLING | 1 | 0 |
| Deep Sea Fishing | SKILLING | 1 | 0 |
| Fishing (Swordfish) | SKILLING | 1 | 0 |
| Fishing Trawler | MINIGAMES | 1 | 1 |
| Gnome Restaurant (Scarfs) | MINIGAMES | 1 | 1 |
| Gnome Restaurant (Seed Pods) | MINIGAMES | 1 | 1 |
| Last Man Standing | MINIGAMES | 1 | 0 |
| Mage Training Arena | MINIGAMES | 1 | 0 |
| Mining (Gemstone Rocks) | SKILLING | 1 | 0 |
| Motherlode Mine | SKILLING | 1 | 0 |
| My Notes | OTHER | 1 | 0 |
| Pest Control | MINIGAMES | 1 | 0 |
| Pyramid Plunder | OTHER | 1 | 0 |
| Rogues' Den | MINIGAMES | 1 | 1 |
| Soul Wars | MINIGAMES | 1 | 0 |
| Stronghold of Security | OTHER | 1 | 0 |
| Temple Trekking | MINIGAMES | 1 | 1 |
| Tithe Farm | MINIGAMES | 1 | 0 |
| TzHaar | OTHER | 1 | 0 |
| Underwater Crabs | SKILLING | 1 | 0 |
| Vale Totems | MINIGAMES | 1 | 0 |
| Woodcutting (Teak Trees) | SKILLING | 1 | 0 |

The two critical patterns: **`loopBackToStep` set with `loopCount` ≤ 0** (23 MINIGAMES — indefinite-loop intent the linter rejects; set an explicit `loopCount` or the round count) and **`ACTOR_DEATH` with no `completionNpcId`/`completionNpcIds`** (5 sources — the kill step cannot auto-complete).

## BOSSES & SLAYER — worst first (highest traffic)

| Source | Category | Bar score | Missing elements (actionable gaps) | Lint |
|--------|----------|-----------|------------------------------------|------|
| Brine Rat | SLAYER | 7/10 | E1 travel-tip/waypoint fallback; E3 gear/prayer note in kill step; E5 strategy/safespot note | — |
| Crawling Hand | SLAYER | 7/10 | E3 gear/prayer note in kill step; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Sol Heredit | BOSSES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | 1W |
| Basilisk | SLAYER | 8/10 | E7 loadout on travel step; E9 drop-rate precision (≥6 dp) | — |
| Cave Horror | SLAYER | 8/10 | E1 travel-tip/waypoint fallback; E7 loadout on travel step | — |
| Cave Kraken | SLAYER | 8/10 | E3 gear/prayer note in kill step; E5 strategy/safespot note | — |
| Corrupted Gauntlet | BOSSES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Dark Beast | SLAYER | 8/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note | — |
| Gryphon | SLAYER | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Lava Strykewyrm | SLAYER | 8/10 | E3 gear/prayer note in kill step; E9 drop-rate precision (≥6 dp) | — |
| Phantom Muspah | BOSSES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Rockslug | SLAYER | 8/10 | E5 strategy/safespot note; E7 loadout on travel step | — |
| Spiritual Mage (Zarosian) | SLAYER | 8/10 | E3 gear/prayer note in kill step; E5 strategy/safespot note | — |
| Superior Slayer Monster | SLAYER | 8/10 | E1 travel-tip/waypoint fallback; E2 auto-arrival waypoint | — |
| The Whisperer | BOSSES | 8/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note | — |
| Warped Creature | SLAYER | 8/10 | E1 travel-tip/waypoint fallback; E3 gear/prayer note in kill step | — |
| Perilous Moons | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 2W |
| Amoxliatl | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Araxxor | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Brutus | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Demonic gorillas | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Deranged Archaeologist | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Doom of Mokhaiotl | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Duke Sucellus (Awakened) | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Frost Nagua | SLAYER | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| Shellbane Gryphon | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| The Fight Caves | BOSSES | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| The Hueycoatl | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| The Inferno | BOSSES | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| The Leviathan (Awakened) | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| The Whisperer (Awakened) | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Tormented Demons | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Vardorvis (Awakened) | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Vyrewatch Sentinel | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Wyrm | SLAYER | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| Yama | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Zalcano | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | 1W |
| Araxyte | SLAYER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Barrows | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Basilisk Knight | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Bloodveld | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Cave Crawler | SLAYER | 9/10 | E3 gear/prayer note in kill step | — |
| Custodian Stalker | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Duke Sucellus | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Earthen Nagua | SLAYER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Fossil Island Wyvern | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Infernal Mage | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Jelly | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Kree'arra | BOSSES | 9/10 | E5 strategy/safespot note | — |
| Kurask | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Mogre | SLAYER | 9/10 | E7 loadout on travel step | — |
| Pyrefiend | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Terror Dog | SLAYER | 9/10 | E1 travel-tip/waypoint fallback | — |
| The Gauntlet | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| The Leviathan | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Turoth | SLAYER | 9/10 | E5 strategy/safespot note | — |
| Vardorvis | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Vorkath | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Zulrah | BOSSES | 9/10 | E1 travel-tip/waypoint fallback | — |
| Bryophyta | BOSSES | 10/10 | — | 1W |
| Chaos Elemental | BOSSES | 10/10 | — | 1W |
| Chaos Fanatic | BOSSES | 10/10 | — | 1W |
| Crazy archaeologist | BOSSES | 10/10 | — | 1W |
| Drake | SLAYER | 10/10 | — | 1W |
| Gargoyle | SLAYER | 10/10 | — | 1W |
| Hespori | BOSSES | 10/10 | — | 1W |
| Hydra | SLAYER | 10/10 | — | 1W |
| Lizardman shaman | SLAYER | 10/10 | — | 1W |
| Nex | BOSSES | 10/10 | — | 1W |
| Obor | BOSSES | 10/10 | — | 1W |
| Royal Titans | BOSSES | 10/10 | — | 1W |
| Sarachnis | BOSSES | 10/10 | — | 1W |
| Scorpia | BOSSES | 10/10 | — | 1W |
| Scurrius | BOSSES | 10/10 | — | 1W |
| Skeletal Wyvern | SLAYER | 10/10 | — | 1W |
| Skotizo | BOSSES | 10/10 | — | 1W |
| Thermonuclear smoke devil | BOSSES | 10/10 | — | 1W |
| Aberrant Spectre | SLAYER | 10/10 | — | — |
| Abyssal Demon | SLAYER | 10/10 | — | — |
| Abyssal Sire | BOSSES | 10/10 | — | — |
| Alchemical Hydra | BOSSES | 10/10 | — | — |
| Aquanite | SLAYER | 10/10 | — | — |
| Callisto | BOSSES | 10/10 | — | — |
| Cerberus | BOSSES | 10/10 | — | — |
| Cockatrice | SLAYER | 10/10 | — | — |
| Commander Zilyana | BOSSES | 10/10 | — | — |
| Corporeal Beast | BOSSES | 10/10 | — | — |
| Dagannoth Prime | BOSSES | 10/10 | — | — |
| Dagannoth Rex | BOSSES | 10/10 | — | — |
| Dagannoth Supreme | BOSSES | 10/10 | — | — |
| Dust Devil | SLAYER | 10/10 | — | — |
| General Graardor | BOSSES | 10/10 | — | — |
| Giant Mole | BOSSES | 10/10 | — | — |
| Grotesque Guardians | BOSSES | 10/10 | — | — |
| K'ril Tsutsaroth | BOSSES | 10/10 | — | — |
| Kalphite Queen | BOSSES | 10/10 | — | — |
| King Black Dragon | BOSSES | 10/10 | — | — |
| Kraken | BOSSES | 10/10 | — | — |
| Nechryael | SLAYER | 10/10 | — | — |
| Phosani's Nightmare | BOSSES | 10/10 | — | — |
| Smoke Devil | SLAYER | 10/10 | — | — |
| Spiritual Mage | SLAYER | 10/10 | — | — |
| Sulphur Nagua | SLAYER | 10/10 | — | — |
| The Nightmare | BOSSES | 10/10 | — | — |
| Venenatis | BOSSES | 10/10 | — | — |
| Vet'ion | BOSSES | 10/10 | — | — |

## Other categories — worst first

Ordered RAIDS → MINIGAMES → SKILLING → CLUES → OTHER, worst-first within each.

| Source | Category | Bar score | Missing elements (actionable gaps) | Lint |
|--------|----------|-----------|------------------------------------|------|
| Theatre of Blood (Hard Mode) | RAIDS | 9/10 | E1 travel-tip/waypoint fallback | — |
| Tombs of Amascut | RAIDS | 9/10 | E1 travel-tip/waypoint fallback | — |
| Tombs of Amascut (300 Invocation) | RAIDS | 9/10 | E1 travel-tip/waypoint fallback | — |
| Tombs of Amascut (500 Invocation) | RAIDS | 9/10 | E1 travel-tip/waypoint fallback | — |
| Chambers of Xeric | RAIDS | 10/10 | — | — |
| Chambers of Xeric (Challenge Mode) | RAIDS | 10/10 | — | — |
| Theatre of Blood | RAIDS | 10/10 | — | — |
| Giants' Foundry | MINIGAMES | 7/10 | E1 travel-tip/waypoint fallback; E4 loop detection; E9 drop-rate precision (≥6 dp) | 1W |
| Hallowed Sepulchre | MINIGAMES | 7/10 | E1 travel-tip/waypoint fallback; E4 loop detection; E9 drop-rate precision (≥6 dp) | 1W |
| Mastering Mixology | MINIGAMES | 7/10 | E1 travel-tip/waypoint fallback; E4 loop detection; E9 drop-rate precision (≥6 dp) | 1W |
| Volcanic Mine | MINIGAMES | 7/10 | E1 travel-tip/waypoint fallback; E4 loop detection; E9 drop-rate precision (≥6 dp) | 1W |
| Temple Trekking | MINIGAMES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | **1C** 1W |
| Vale Totems | MINIGAMES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | **1C** |
| Mahogany Homes | MINIGAMES | 8/10 | E4 loop detection; E9 drop-rate precision (≥6 dp) | 3W |
| Guardians of the Rift | MINIGAMES | 8/10 | E1 travel-tip/waypoint fallback; E4 loop detection | 1W |
| Shades of Mort'ton | MINIGAMES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | 1W |
| Trouble Brewing | MINIGAMES | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Fishing Trawler | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** 1W |
| Gnome Restaurant (Scarfs) | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** 1W |
| Gnome Restaurant (Seed Pods) | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** 1W |
| Rogues' Den | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** 1W |
| Barbarian Assault | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Castle Wars | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Last Man Standing | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Mage Training Arena | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Pest Control | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Soul Wars | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Tithe Farm | MINIGAMES | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Barracuda Trials | MINIGAMES | 9/10 | E4 loop detection | — |
| Brimhaven Agility Arena | MINIGAMES | 10/10 | — | **1C** |
| Tempoross | MINIGAMES | 10/10 | — | — |
| Wintertodt | MINIGAMES | 10/10 | — | — |
| Prifddinas Rabbit | SKILLING | 6/10 | E1 travel-tip/waypoint fallback; E3 gear/prayer note in kill step; E4 loop detection; E9 drop-rate precision (≥6 dp) | — |
| Underwater Crabs | SKILLING | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | **1C** |
| Pickpocketing Darkmeyer Vyre | SKILLING | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Aerial Fishing | SKILLING | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Mining (Gemstone Rocks) | SKILLING | 9/10 | E1 travel-tip/waypoint fallback | **1C** |
| Motherlode Mine | SKILLING | 9/10 | E9 drop-rate precision (≥6 dp) | **1C** |
| Farming (Fruit Trees) | SKILLING | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| Shooting Stars | SKILLING | 9/10 | E9 drop-rate precision (≥6 dp) | 1W |
| Colossal Wyrm Agility | SKILLING | 9/10 | E1 travel-tip/waypoint fallback | — |
| Runecrafting (Fire Runes) | SKILLING | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Cutting Squid | SKILLING | 10/10 | — | **1C** |
| Deep Sea Fishing | SKILLING | 10/10 | — | **1C** |
| Fishing (Swordfish) | SKILLING | 10/10 | — | **1C** |
| Woodcutting (Teak Trees) | SKILLING | 10/10 | — | **1C** |
| Rooftop Agility | SKILLING | 10/10 | — | 1W |
| Black Chinchompas | SKILLING | 10/10 | — | — |
| Thieving (Seed Stalls) | SKILLING | 10/10 | — | — |
| Scroll Cases | CLUES | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Beginner Treasure Trails | CLUES | 10/10 | — | 1W |
| Easy Treasure Trails | CLUES | 10/10 | — | 1W |
| Medium Treasure Trails | CLUES | 10/10 | — | 1W |
| Elite Treasure Trail Rewards (Rare) | CLUES | 10/10 | — | — |
| Elite Treasure Trails | CLUES | 10/10 | — | — |
| Hard Treasure Trail Rewards (Rare) | CLUES | 10/10 | — | — |
| Hard Treasure Trails | CLUES | 10/10 | — | — |
| Master Treasure Trail Rewards (Rare) | CLUES | 10/10 | — | — |
| Master Treasure Trails | CLUES | 10/10 | — | — |
| Shared Treasure Trail Rewards | CLUES | 10/10 | — | — |
| The Mimic | CLUES | 10/10 | — | — |
| Stronghold of Security | OTHER | 7/10 | E3 gear/prayer note in kill step; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | **1C** |
| Chompy Bird Hunting | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Frogeel) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Jubster) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Newtroost) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Spidine) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Swordchick) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Creature Creation (Unicow) | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | — |
| Elven Crystal Chest | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E7 loadout on travel step; E9 drop-rate precision (≥6 dp) | — |
| Random Events | OTHER | 7/10 | E1 travel-tip/waypoint fallback; E2 auto-arrival waypoint; E9 drop-rate precision (≥6 dp) | — |
| Catacombs of Kourend | OTHER | 8/10 | E5 strategy/safespot note; E9 drop-rate precision (≥6 dp) | **1C** |
| Champion's Challenge | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | **1C** |
| Adamant Dragon | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note | — |
| Brimstone Chest | OTHER | 8/10 | E7 loadout on travel step; E9 drop-rate precision (≥6 dp) | — |
| Fossil Island Notes | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Glough's Experiments | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note | — |
| Hunter Guild | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Monkey Backpacks | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E9 drop-rate precision (≥6 dp) | — |
| Ogress Shaman | OTHER | 8/10 | E1 travel-tip/waypoint fallback; E5 strategy/safespot note | — |
| My Notes | OTHER | 9/10 | E5 strategy/safespot note | **1C** |
| Pyramid Plunder | OTHER | 9/10 | E1 travel-tip/waypoint fallback | **1C** |
| Albatross (Boat Combat) | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Armoured Zombie | OTHER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Boat Paints | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Camdozaal | OTHER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Cyclopes | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Fountain of Rune | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Great White Shark (Boat Combat) | OTHER | 9/10 | E5 strategy/safespot note | — |
| Larran's Big Chest | OTHER | 9/10 | E7 loadout on travel step | — |
| Lost Schematics | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Miscellaneous | OTHER | 9/10 | E2 auto-arrival waypoint | — |
| Mithril Dragon | OTHER | 9/10 | E5 strategy/safespot note | — |
| Ocean Encounters | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Prifddinas Elf | OTHER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Rune Dragon | OTHER | 9/10 | E1 travel-tip/waypoint fallback | — |
| Sailing Misc | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Shayzien Armour | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Stingray (Boat Combat) | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Vampyre Kraken (Boat Combat) | OTHER | 9/10 | E5 strategy/safespot note | — |
| Waterfiend | OTHER | 9/10 | E5 strategy/safespot note | — |
| Wilderness God Wars Dungeon | OTHER | 9/10 | E9 drop-rate precision (≥6 dp) | — |
| Zombie Pirate Locker | OTHER | 9/10 | E7 loadout on travel step | — |
| TzHaar | OTHER | 10/10 | — | **1C** |
| Barracuda Salvage | OTHER | 10/10 | — | — |
| Elder Chaos Druids | OTHER | 10/10 | — | — |
| Fishy Salvage | OTHER | 10/10 | — | — |
| Forestry | OTHER | 10/10 | — | — |
| Fremennik Salvage | OTHER | 10/10 | — | — |
| Large Salvage | OTHER | 10/10 | — | — |
| Martial Salvage | OTHER | 10/10 | — | — |
| Narwhal (Boat Combat) | OTHER | 10/10 | — | — |
| Opulent Salvage | OTHER | 10/10 | — | — |
| Orcas (Boat Combat) | OTHER | 10/10 | — | — |
| Plundered Salvage | OTHER | 10/10 | — | — |
| Port Tasks | OTHER | 10/10 | — | — |
| Revenants | OTHER | 10/10 | — | — |
| Sea Treasures | OTHER | 10/10 | — | — |
| Small Salvage | OTHER | 10/10 | — | — |
