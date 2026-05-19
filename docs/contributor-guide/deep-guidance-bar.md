# Deep guidance bar — the 10 elements of a fully authored source

A source that ships with *one* travel step and an `ACTOR_DEATH` auto-complete is better than nothing, but it is not yet "Quest-Helper depth." The table below defines the 10 elements that together constitute a fully authored source. Each element has a one-line definition, a pointer to a real source in this repo that demonstrates it well, and a note on the most common mistake. Run `guidance_lint` (see [data sourcing — Tier 1 tools](data-sourcing.md#tier-1--mcp-tools-try-first)) on your source before filing a PR — a clean or INFO-only lint report is a good signal that the structural scaffolding is sound.

This bar is the authoring target for [Tier D (D2-D4)](../ROADMAP.md#tier-d--coverage-breadth). See also decision [D-03](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs) for the tiered data-sourcing strategy that tells you *where to get the data* for each element.

---

## Element 1 — Travel tip with teleport hierarchy

**Definition.** The `travelTip` field on the source (and optionally on individual `guidanceSteps`) gives the player the fastest realistic teleport in a short string. When multiple teleport options exist, the preferred one is listed first. When access depends on a quest or skill, the source also declares `waypoints` so the plugin can serve an appropriate route to players who lack the preferred unlock.

**Good example:** `General Graardor` — `"travelTip": "Trollheim teleport -> GWD"`, backed by two `waypoints` entries: one for players who have completed Eadgar's Ruse (Trollheim teleport) and a fallback walking route from Death Plateau for those who have not.

**Common pitfall.** Writing only the *fastest* route without a fallback waypoint. New accounts without Eadgar's Ruse silently receive a travel tip that requires a quest they haven't done. Always pair an unlocked-ability travel tip with at least one unconditional fallback waypoint.

> **Coordinate precision note:** `worldX/worldY` on every step must match the object's *actual* tile, not the room centre. The hint arrow and minimap dot are both derived from `worldX/worldY` — an off-by-one puts the arrow on the wrong tile. Verify coordinates with the `coordinate_helper` MCP tool or the RuneLite Developer Tools tile-hover readout.

---

## Element 2 — Tile-sequence or auto-arrival waypoint

**Definition.** Every meaningful location change in the guidance flow must be captured as a step with `completionCondition: ARRIVE_AT_TILE` (or `ARRIVE_AT_ZONE` / `PLAYER_ON_PLANE` for multi-floor transitions) and accurate `worldX/worldY/worldPlane` coordinates. The player should never have to wonder "has the plugin noticed I arrived?" — auto-detection fires the step forward without a manual click.

**Good example:** `Barrows` — nine ordered steps, each targeting a specific mound or tunnel, advancing automatically on `ARRIVE_AT_TILE` or `VARBIT_AT_LEAST`. The kill sequence never stalls waiting for a manual "Next" click.

**Common pitfall.** Using `completionCondition: MANUAL` for every step. Manual steps are correct for dialog choices and puzzle interactions where state cannot be detected; they are wrong for travel legs and arrivals where tile or plane detection works cleanly. Reserve `MANUAL` for steps that are genuinely undetectable.

> **Planned schema extension (B2):** Tier B will add `waypointTiles` — an ordered list of intermediate tiles a player must cross in sequence within a single step. This will replace multi-step travel chains with a single richer step once that milestone ships. Do not block authoring on it now; tile-sequence steps written today will be migrated automatically.

---

## Element 3 — Combat gear recommendation or skilling kit

**Definition.** The kill-step description (the step that ends with `ACTOR_DEATH` or equivalent) explicitly names the recommended prayer, the most important offensive swap, and any mechanic-specific item (e.g., Crumble Undead for shades, Bulwark for Corp). Skilling sources name the tier of tool or outfit needed for viable points-per-hour.

**Good example:** `Vorkath` — `"Kill Vorkath. Use Protect from Ranged for standard phase, dodge acid pools, and click the Zombified Spawn immediately when it appears"` names the prayer, the hazard to avoid, and the mechanic-specific action, all in one sentence.

**Common pitfall.** Saying only "Kill the boss" with no mechanical context. A player who has never done the content cannot act on that. You do not need to replicate the Wiki strategy page — one sentence covering the defensive prayer and the single most punishing mechanic is the minimum bar.

---

## Element 4 — Kill-loop / activity-loop detection

**Definition.** Sources that require multiple repetitive cycles before a reward (repeated chest runs, minigame rounds, shade-burning loops) declare a `loopBackToStep` and `loopCount` on the final step of each cycle to return the sequencer to the start of the loop. The player sees "do this again" rather than a dead-ended sequence.

**Good example:** `Shades of Mort'ton` — the chest-opening step loops back to the pyre-burning step via `loopBackToStep`, cycling until all shade keys are spent. `skipIfHasAnyItemIds` on the pyre step skips burning when keys are already held, preventing a spurious bank redirect.

**Common pitfall.** Adding a loop on a *boss* source. Single-kill boss loops should be handled by `ACTOR_DEATH` completion (which advances to a new kill automatically) rather than an explicit loop. Use `loopBackToStep` only for multi-action cycles within a single "session" — minigames, gathering loops, pyre runs.

> **Planned schema extension (B1):** Tier B will add composable AND/OR completion conditions so a loop can exit on either "inventory full" or "key obtained," without separate steps for each branch. Author loops with the current single-condition model for now; the AND/OR extension will layer on top.

---

## Element 5 — Safespot or strategy note where relevant

**Definition.** When a boss or creature has a well-known safespot, a phase transition that trips new players, or a mechanic that dramatically affects survivability (ghost specials, acid phase, Woox walk), the relevant kill step must name it. "Relevant" means: if a first-time player following the guidance dies to something predictable and avoidable, the element is missing.

**Good example:** `Cerberus` — `"Kill Cerberus. Protect from Magic, watch for the triple ghost special attack"` names both the prayer and the mechanic that kills unprepared players. Short, actionable, in the step description where the player can see it while fighting.

**Common pitfall.** Front-loading all strategy detail into the travel step, where the player has already scrolled past it by the time they need it. Strategy context belongs in the step where it is *actionable* — either the arrival step (to prompt preparation) or the kill step (for in-fight reminders).

---

## Element 6 — Bank-and-return detection

**Definition.** If a step requires a consumable item (a key, a pouch, a teleport scroll) that is not always in the player's inventory, that step must declare `requiredItemIds`. When the plugin detects the item is missing, it redirects the player to a bank before proceeding. Without this, the player gets stuck at a locked door with no guidance on what went wrong.

**Good example:** `Bryophyta` — step 0 declares `"requiredItemIds": [22374]` (Mossy key). Players who arrive at the sewer without one are redirected to the bank automatically.

**Common pitfall.** Omitting `requiredItemIds` on any step that references a consumable in its description. If the description says "use X key on the gate," the step needs `requiredItemIds` containing the key's item ID. The `guidance_lint` tool flags this pattern as a warning when a step description mentions "key" or "requires" but has no `requiredItemIds`.

---

## Element 7 — Inventory loadout

**Definition.** The travel step (step 0, or the first step with `completionCondition: ARRIVE_AT_TILE`) includes `requiredItemIds` for every consumable the player *must* bring before they can complete the source. The step description names the loadout explicitly so the player knows what to withdraw before leaving the bank.

**Good example:** `Obor` — `"requiredItemIds": [20754]` (Giant key) on the first travel step; description says "Travel to the Edgeville Dungeon or Varrock Sewers hill giant area." The key requirement surfaces at the earliest possible step, before the player wastes a run to a locked gate.

**Common pitfall.** Listing *all* gear in `requiredItemIds` — food, potions, weapon, armour. The redirect flow is designed for consumables or unlockables that are uncommon or easy to forget, not for a full combat loadout. Including 20+ item IDs in `requiredItemIds` defeats the purpose and makes the redirect fire constantly.

---

## Element 8 — Account-state prerequisites (quests, diaries, items)

**Definition.** The source's top-level `requirements` object lists every quest and skill level the player must have to access the content. This populates the "locked" indicator in the panel and drives `conditionalAlternatives` branching (Tier B) when different travel routes depend on unlocked quests. It is separate from and complementary to in-step `requiredItemIds`.

**Good example:** `Alchemical Hydra` — `"requirements": {"skills": [{"skill": "SLAYER", "level": 95}]}` combined with two `waypoints` (fairy ring CIR for players with Fairytale II unlocked, walking route fallback). The source is hidden for accounts below 95 Slayer, and the travel route adapts to fairy ring access.

**Common pitfall.** Setting `requirements` for the *preferred* route only. If accessing the boss requires a quest, the quest must be in `requirements.quests` — even if an alternate unquested route exists. A player who cannot do the content should see it as locked, not receive a travel tip for a route they cannot complete. Use `waypoints` to handle the unlocked-route variation, not `requirements`.

> **Category reminder:** The `category` field affects locked-source filtering. Use `SLAYER` for slayer creatures, `SKILLING` for non-combat skill activities, and `OTHER` only for genuinely miscellaneous sources. See the [JSON Schema Reference](schema-reference.md#collectionlogsource) table for the full enum.

---

## Element 9 — Drop rate confidence (Wiki vs TempleOSRS vs community)

**Definition.** Every item has a `dropRate` value with at least six decimal places of precision, sourced from the highest-confidence data tier available for that data type. The source citation is not stored in the JSON (the file has no comment fields), but the contributor should know where the rate came from and be prepared to justify it in the PR description. Rates that differ materially from the Wiki must note the reconciliation reason in the PR.

**Good example:** `Zulrah` — zero `guidance_lint` issues; `independent: true` on the mutagen items marks their separate table correctly. The rates align with TempleOSRS EHB math.

**Common pitfall.** Using a Wiki drop rate when TempleOSRS or the Log Hunters spreadsheet has a more accurate community-audited figure. Per decision [D-03](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs), the source-of-truth precedence for drop rates is: **Wiki first**, **Log Hunters second**, **TempleOSRS third** for rate precision. Collection log item IDs are the inverse — use TempleOSRS canonical IDs, not the Wiki item IDs, which diverge for some items.

> See [`feedback_verify_data_against_source.md`](https://github.com/cha-ndler/runelite-dev-toolkit/blob/main/memory-seed/feedback_verify_data_against_source.md) for the reconciliation procedure when sources disagree.

---

## Element 10 — Author's date stamp and source citations

**Definition.** Every PR that adds or materially edits a source must include in the PR description: (a) the game version or update the data was authored against, (b) the primary and secondary data sources used for drop rates, coordinates, and NPC IDs, and (c) whether `guidance_lint` and `validate_drop_rates` passed clean. This information lives in the PR, not in the JSON — it forms an audit trail for future contributors who need to know when data was last verified and against what source.

**Good example:** `Giants' Foundry` — a shop source with `"rewardType": "SHOP"`, `"pointsPerHour": <value>`, and `requirements` covering the prerequisite quest and smithing level. A PR for this source should cite the Wiki reward shop table, the TempleOSRS EHB rate for Kovac's reputation grind, and note the game update month the point costs were verified against.

**Common pitfall.** Opening a PR with no data-source citations. Reviewers cannot verify drop rates without knowing the source. When a rate looks wrong six months later and the PR has no citations, the only option is to re-audit from scratch. One sentence per data type (rates: Wiki; item IDs: TempleOSRS; coordinates: MCP `coordinate_helper`; NPC IDs: MCP `npc_lookup`) is the minimum.

---

## Deep guidance checklist

Before marking a source fully authored, verify all 10 elements are present or explicitly waived (with reason):

- [ ] **Travel tip** — `travelTip` set on source; at least one `waypoints` fallback if the primary tip requires a quest unlock
- [ ] **Auto-arrival** — every meaningful location change uses `ARRIVE_AT_TILE`, `ARRIVE_AT_ZONE`, or `PLAYER_ON_PLANE`; `MANUAL` only where state cannot be detected
- [ ] **Gear / loadout description** — kill step (or skilling equivalent) names the recommended prayer + key mechanic in plain English
- [ ] **Loop detection** — multi-cycle sources declare `loopBackToStep` / `loopCount`; single-kill bosses do not
- [ ] **Strategy note** — any mechanic that kills an unprepared player is named in the step description where it is actionable
- [ ] **Bank-and-return** — consumables required to enter or complete a step are in `requiredItemIds` on the earliest step that needs them
- [ ] **Inventory loadout** — the travel step names what the player must bring and includes `requiredItemIds` for hard-to-replace items
- [ ] **Prerequisites** — `requirements.quests` and `requirements.skills` reflect the *minimum* needed to access content; waypoints cover the route variation
- [ ] **Drop rate confidence** — rates sourced from the correct tier (see [D-03](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs)); `independent` and `rollsPerKill` set correctly
- [ ] **PR citations** — PR description cites game version, data sources, and `guidance_lint` / `validate_drop_rates` pass status
