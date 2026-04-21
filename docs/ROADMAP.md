# Collection Log Helper — Long-Form Roadmap

> **Mission**: Be THE plugin for anyone collecting collection log items. Match and eventually exceed the per-item guidance depth of Quest Helper, with player-aware method recommendations for every source in the game.

> **The North Star (what makes this insane):**
> Player-tuned efficiency calculator is the **default filter** and the killer differentiator. We don't just show "best source" — we show "best source *for this player, right now*" with full account context: collection-log unlocks already obtained, KC at every source, gear in the bank that could speed kills, stackable consumables (clue caskets, scrolls, etc.) that shift expected-value math the moment they're opened, quest unlocks, skill thresholds, equipped items. Built on top of the Log Hunters spreadsheet community math, extended with everything RuneLite can read live. Players can switch filters at any time (Category, Search, Stats, etc.), but the default is "the math says do this next, here's exactly how."
>
> Combined with Quest-Helper-level step-by-step guidance — *where to go, what to equip, what to bring, exact tile* — plus current-meta knowledge that updates as Jagex updates the game. Math + automation + meta intelligence in one plugin.

> **Status**: Living document. Update the status block at the end of each milestone or agent session. Last reviewed: 2026-04-16.

---

## Table of Contents

1. [Honest current-state assessment](#1-honest-current-state-assessment)
2. [Plugin Hub submission readiness](#2-plugin-hub-submission-readiness)
3. [GitHub / PR workflow playbook](#3-github--pr-workflow-playbook)
4. [Tiered roadmap with milestones](#4-tiered-roadmap)
5. [Project tracking format](#5-project-tracking-format)
6. [Quick wins for next session](#6-quick-wins-for-next-session)
7. [Decision register](#7-decision-register)
8. [Day 0 → present: completed work](#8-day-0--present-completed-work)
9. [Status log](#9-status-log)

---

## 1. Honest current-state assessment

### What this plugin already does well

- **Data breadth is real**. 225 sources and 2,109 items, all item IDs verified against TempleOSRS, drop rates cross-audited against the Wiki and Loghunters. Quest Helper equivalent would be ~180 quests — we already rival that footprint in *data volume*.
- **Efficiency scoring is a differentiator Quest Helper does not have**. Combined per-kill "any new slot" probability with independent-roll handling, sequential dependencies, raid size scaling, mutually-exclusive variants, and slayer task-weight overhead. This is the feature that makes the plugin recommendable on its own merits even before guidance depth catches up.
- **Step-by-step guidance exists for every source**. All 225 sources have at least one guidance step. Auto-arrival (223), auto-kill (112), zone detection, multi-floor stair navigation, bank redirect on missing items, and `conditionalAlternatives` branching on quest/skill requirements already work.
- **Overlay surface area is mature**. Hint arrow, minimap arrow, world-map route, NPC/object/widget/inventory/ground-item/dialog highlights, InfoBox, tooltips, right-click menu injection. This is arguably at Quest Helper parity for the *rendering* layer.
- **Account-aware filtering is partially wired**. Quest completions, skill levels (including Sailing), teleport inventory from bank scan, spellbook awareness, diary detection, and Shortest Path plugin integration all ship today.
- **Architecture is finally breathing**. After the 2026-04-15/16 decomposition, the plugin class is 1,281 LOC (down 42% from 2,192), with lifecycle and guidance concerns extracted into six dedicated services (`OverlayRegistry`, `SceneEventRouter`, `AuthoringLogger`, `SyncStateCoordinator`, `GuidanceUIState`, `GuidanceSequencer`, `GuidanceOverlayCoordinator`). 503 tests pass on JUnit 4 + Mockito.

### Concrete gap vs Quest Helper — the honest version

Quest Helper's model is that **each quest is a Java class** extending `BasicQuestHelper`, which composes typed step classes (`NpcStep`, `ObjectStep`, `DetailedQuestStep`, `PuzzleWrapperStep`) via a `ConditionalStep` tree evaluated against live varbit/varplayer/inventory/skill/quest state. The branching structure is *code*, not data.

We made the opposite bet: **one `drop_rates.json` file** that declares sources, items, and guidance steps. Branching lives in a `conditionalAlternatives` array on a step. Completion lives in a 10-value `CompletionCondition` enum.

That bet bought us three things Quest Helper doesn't have: fast contributor onboarding, easy bulk edits, and a single source of truth that can be audited by scripts. It cost us three things Quest Helper *does* have:

1. **Composable step types**. Quest Helper's `ConditionalStep` can nest arbitrarily. Our conditional alternatives are one level deep and can only rewrite the current step's fields.
2. **Tile-by-tile pathing within a step**. Quest Helper uses world-point sequences; we use a single target tile with optional intermediate "arrive" steps.
3. **Alternative methods for the same objective**. Quest Helper models "if you have fairy rings, else if you have spirit trees, else walk" as first-class code. We model it as JSON alternatives that rewrite fields, which works for *travel* but not for whole-method swaps like "CoX vs ToA for ancestral".

Concrete examples where the gap bites today:

- **Travel substitution inside a method**. A player with a Royal Seed Pod, a Crafting cape, or Dragonstone jewellery-house teleport gets the same "walk from Ardougne" guidance as a fresh account. We detect the teleports — we just don't branch on them inside step descriptions.
- **Cross-source recommendation for a shared item**. Ancestral hat drops from CoX at ~1/53 uniques and ToA at competitive rates depending on level. We never tell the player "given your current KC and account, ToA 300 is faster for you." The efficiency ranker picks the best *source* for all missing items on that source, not the best *source for a given item*.
- **Pre-requisite chains across sources**. We don't say "to do GWD efficiently you need a bandos godsword, which means KQ first" unless the user hand-reads the panel.
- **Dynamic puzzle/mechanics steps**. Quest Helper has `PuzzleWrapperStep` that can re-render a step's target as the puzzle state changes. We have no equivalent for, e.g., Lightbearer mazes or Wintertodt brazier rotation.

None of this is a reason to throw out the JSON model. It is a reason to decide explicitly, in Tier B, whether we extend the schema, add an optional Java helper layer, or both.

---

## 2. Plugin Hub submission readiness

### What is genuinely ready today

- Data correctness. 2,109 items ID-verified, drop rates audited twice.
- Feature breadth. The `README.md` "Features" list is accurate and most of it has been used end-to-end.
- Test coverage — 503 tests passing, critical services covered.
- Build hygiene. `./gradlew build` clean. shadowJar 293 KB, runeLiteVersion pinned to 1.12.24, verification-metadata.xml committed.
- License, CREDITS.md, CONTRIBUTING.md, privacy — all present and correct.
- Screenshots — exist in `docs/screenshots/`.

### What to do BEFORE resubmission

These are the items that would have made the previous PR cycle smoother.

| Item | Why it matters for review | Effort |
|------|---------------------------|--------|
| **Panel decomposition** (1,661-LOC `CollectionLogHelperPanel`) | Reviewers look at LOC per file. This is the largest untouched god-object. Split into view controllers per mode (Efficient / Category / Search / Pet / Statistics) behind a shared panel shell. | 2-3 agent-days |
| **Close/verify all merge-blocking open issues** (#319, #323 — already verified; #314, #306 need in-game capture or triage; #134 is P3) | A clean issue tracker signals maintenance discipline. | 0.5 day of triage + scheduling in-game capture |
| **Stable tag + changelog cutover** | Tag a `v1.0.0-hub` commit with frozen CHANGELOG.md entry. Don't update it while the PR queues. | 0.5 day |
| **Plugin Hub self-review checklist** | Run the Plugin Hub checklist (no premium-plugin-like features, no external-service calls without user opt-in, no PII, correct manifest icon/description/tags). Write the results into a `docs/plugin-hub-review.md` we can cite in the PR. | 1 day |
| **Final LOC pass on `CollectionLogHelperPlugin`** | Aim for under 1,000 LOC on the plugin class. Extract the remaining event dispatch and any residual overlay wiring. | 1-2 days |
| **Smoke-test onboarding flow** | Record a fresh-install walk-through video (or screenshot series) covering: install → open panel → sync log → Guide Me on a source → step auto-advance. Attach to PR. | 0.5 day |

### Recommendation: hold resubmission ~2 weeks

The previous PR (runelite/plugin-hub#11156) closed because of 4k-line diffs on a queued submission and repeated pushes before reviewer engagement. Even though the plugin is *feature*-ready, the social signal we want to send on resubmission is "this is stable, polished, and will not churn under you." That means:

1. Finish the six items above.
2. Let the branch sit for a week with no pushes — fix only documentation or genuinely critical bugs.
3. Submit fresh (new PR, new branch), squash-merged locally, single focused commit history, zero CHANGELOG churn after open.

This costs two weeks. It saves the much larger cost of another closure.

---

## 3. GitHub / PR workflow playbook

### Rules that apply to every PR

1. **One logical change per PR**. If you find a tangential cleanup while you're in the file, stash or branch it separately.
2. **Keep PR diffs under 500 lines where possible**. Above that, split or stack PRs. 1,000+ line PRs should have a strong justification in the description.
3. **Use draft PRs while iterating**. Mark "ready for review" only when you would be happy for a reviewer to hit merge without comment.
4. **Squash before opening reviewer-facing PRs**. Local merge commits and "wip" commits should not hit a reviewer.
5. **Branch hygiene**:
   - Branch names describe the change: `fix/guidance-null-safety`, `feat/panel-decomposition`, `refactor/extract-sequencer`.
   - Delete merged branches.
   - Keep your fork synced with upstream weekly or before starting a branch.
6. **CHANGELOG discipline**. Update `CHANGELOG.md` in the same PR as the change, under an `## [Unreleased]` heading. Never batch-write CHANGELOG entries the day before a release — you will forget half of them.
7. **Never commit personal info**. Author name is `cha-ndler`, email is the GitHub noreply. Do not let tooling or templates write real first names or personal emails into any file.
8. **No Claude attribution in commits**. No "co-authored-by" or "generated by" lines, full stop.

### Rules that apply specifically to Plugin Hub / upstream PRs

1. **Do not push updates to a queued review PR**. Once a PR is awaiting review, pushing more commits resets the reviewer's mental model and is read as impatience. Exception: a critical bug that would make the plugin unusable in the current queued state, with a reviewer-visible comment explaining.
2. **Do not submit 4k-line diffs to a new-plugin submission**. Break the initial submission into the minimum-viable surface (core plugin, one mode, guidance engine). Feature expansion goes in follow-up PRs after the plugin is accepted.
3. **If a review PR is closed, resubmit fresh**. Do not reopen. A new branch and a clean history is read as "the author took the feedback seriously."
4. **Wait for reviewer engagement before pushing more**. If a reviewer has commented once, give them time to finish their pass before adding commits.
5. **Keep the PR description self-contained**. Reviewer should never need to scroll the commit log to understand what the PR does.
6. **When linking to issues/PRs in a different repo (e.g., commenting on plugin-hub from our repo)**, use the fully-qualified `cha-ndler/collection-log-helper#NNN` form. Bare `#NNN` will auto-link to the *current* repo's PR with that number — almost always wrong and confusing.

### Rules that apply to internal CLH PRs

1. **Tests in the same PR as the code change**. No "tests coming in a follow-up."
2. **Use `code-reviewer` agent before requesting review**. Fix CRITICAL and HIGH issues. Document any intentional deviations.
3. **Run `./gradlew build` locally before push**. CI is not a linter.
4. **Superset detection**: before opening, check whether an already-open PR supersedes yours.

---

## 4. Tiered roadmap

Each tier is independently shippable. Mid-tier work can merge and be released to users without requiring the tier to complete.

### Tier A — Production polish for Plugin Hub resubmission

- **Mission**: Ship a stable 1.0 that passes Plugin Hub review on first try.
- **Outcome**: Plugin installable from Plugin Hub; plugin class <1,000 LOC; panel decomposed; all P0/P1 issues closed; CHANGELOG tagged; screenshots refreshed.
- **Estimated PRs**: 6-10
- **Estimated effort**: 7-10 agent-session-days
- **Agents**: `planner`, `code-reviewer`, `refactor-cleaner`, `doc-updater`
- **Data sources**: none beyond what we have
- **MCP dependency**: low. Work is mostly internal.
- **Dependencies**: none
- **Risks**:
  - *Bikeshedding the panel split*. Mitigation: pick the per-mode-controller split up front, document in a 1-page ADR, stop.
  - *Pulling scope from Tier B into Tier A*. Mitigation: if a change requires schema changes or new CompletionCondition values, it goes to Tier B.

Concrete sub-milestones:

- A1 — Decompose `CollectionLogHelperPanel` into mode controllers + shared shell.
- A2 — Drop `CollectionLogHelperPlugin` below 1,000 LOC.
- A3 — Close or defer every open issue with an explicit label (`hub-blocker`, `hub-nice-to-have`, `post-hub`).
- A4 — Author `docs/plugin-hub-review.md` self-review.
- A5 — Re-record screenshots against current UI.
- A6 — Tag `v1.0.0-hub`, freeze CHANGELOG, wait a week, resubmit.

### Tier A.5 — Data sourcing infrastructure

- **Mission**: Eliminate in-game authoring runs as the default for new content. Make MCP/automation the path of least resistance for any future contributor.
- **Outcome**: ~95% of guidance authoring data needs are met by automated tooling. In-game capture only needed for truly dynamic state (instance offsets, mid-fight phase transitions, dynamic spawns).
- **Estimated PRs**: 3-4
- **Estimated effort**: 3-5 agent-days
- **Agents**: `runelite-plugin-dev`, `osrs-expert`, `architect` (for tool design)
- **Data sources**: RuneLite source (`VarbitID.java`), Quest Helper source (`VarbitRequirement`, `Conditions`, `NpcCondition`, etc.), abextm/osrs-cache, OSRS Wiki API, our existing audit notes
- **MCP dependency**: builds new MCP tools as the deliverable
- **Dependencies**: Can run in parallel with Tier A (does not block Hub resubmission). Should complete before Tier D starts.
- **Risks**:
  - *Quest Helper varbit references may not be exhaustive*. Mitigation: combine sources (RuneLite + Quest Helper + Wiki page sources).
  - *Cache-vs-scene ID divergences may not be enumerable up front*. Mitigation: registry starts seeded from past audit findings; grows as new divergences are found.

Sub-milestones:

- A5.1 — `varbit_lookup` MCP tool. Scrapes RuneLite `VarbitID.java`, Quest Helper requirement classes, and Wiki page sources. Returns named varbits with descriptions and confidence level.
- A5.2 — `verified_scene_ids.json` registry committed to repo. Maps examine-ID ↔ scene-ID for known divergences. Seeded from past audits and Quest Helper's object references.
- A5.3 — `cache_diff_check` MCP tool. Runs at PR time, flags when a referenced object ID is suspect (not in cache or known scene-ID alias).
- A5.4 — Document the tiered sourcing strategy in `CONTRIBUTING.md` so contributors know to try MCP first, in-game last.
- A5.5 — **In-game authoring playbook generator**: when in-game capture *is* needed (truly dynamic state only), the MCP tool returns a step-by-step recipe — exactly what to enable, where to walk, what events to trigger, what to look for in the log file. No more "you'll need to figure this out in-game."
- A5.6 — **Data confidence brain (D-06 implementation)**: precedence registry that selects the source of truth per data type, weighted by historical accuracy. Optionally feeds `continuous-learning-v2` instincts back in to update confidence over time.

### Tier B — Guidance depth parity with Quest Helper

- **Mission**: Expressive step library and real conditional branching so that "our guidance is as deep as Quest Helper's" is a defensible claim.
- **Outcome**: Per-source branching on player state (not just travel substitution), alternative-method modelling, tile-sequence pathing within a step, puzzle-style dynamic re-render steps. Contributors can still author most guidance in JSON.
- **Estimated PRs**: 20-30
- **Estimated effort**: 25-40 agent-session-days
- **Agents**: `planner`, `architect`, `runelite-plugin-dev`, `osrs-expert`, `code-reviewer`
- **Data sources**: Quest Helper source (reference), Wiki
- **MCP dependency**: medium. Quest Helper source introspection helps; can be done manually if MCP is down.
- **Dependencies**: Tier A (otherwise the plugin class/panel can't absorb new services cleanly)
- **Risks**:
  - *Schema drift breaking the 225 existing sources*. Mitigation: every schema extension is purely additive with a default that matches current behavior. Write a migration test that loads `drop_rates.json` and asserts zero parse failures before and after each schema change.
  - *Parallel-universe Java helpers diverging from JSON*. See decision D-01.

Sub-milestones (the order reflects dependencies, not priority):

- B1 — Add composable completion conditions. Today's 10-value enum becomes a discriminated union that can AND/OR other conditions. Keep the old enum values as the atomic leaves.
- B2 — Tile-sequence pathing: `GuidanceStep` can declare an ordered list of waypoint tiles that the player must cross in order, not just "arrive within N tiles of the target."
- B3 — Nested conditional steps: `conditionalAlternatives` can itself branch, not just one-level-deep overrides.
- B4 — Alternative-method modelling on sources: a source can declare multiple methods (e.g., "Konar task", "unlocked ancient Hydra lair") and the sequencer picks one based on player state.
- B5 — Puzzle/dynamic step type: a step can re-render its target every tick based on a pluggable evaluator. Pilot on one source (Wintertodt braziers or Barbarian Assault roles).
- B6 — **Decision D-01**: hybrid Java helpers — see section 7. If we adopt them, scaffold one pilot source (e.g., Cerberus) as a Java helper class to prove the pattern.

### Tier C — Player-aware guidance

- **Mission**: Guidance text and chosen method reflect *this specific account's* capabilities.
- **Outcome**: Same source, same item, two different player accounts, two different recommended paths. Teleports in POH, equipped items, quest unlocks, diary tiers, skill-cape perks, achievement unlocks all feed the branch choice.
- **Estimated PRs**: 15-25
- **Estimated effort**: 20-30 agent-session-days
- **Agents**: `runelite-plugin-dev`, `osrs-expert`, `planner`
- **Data sources**: Wiki (teleport tables, diary requirements), RuneLite SDK docs (varbits/varplayers for quest progress, diary state, POH teleports)
- **MCP dependency**: medium-high. Varbit lookups benefit from MCP.
- **Dependencies**: Tier B (the expressive branching model has to exist before player state can meaningfully drive it)
- **Risks**:
  - *POH teleport detection is fragile*. POH state isn't always visible without the player entering. Mitigation: add a manual "I have the following teleports in my POH" config override.
  - *State detection overreach*. Detecting too eagerly (e.g., assuming a player has Vengeance on Lunars without checking spellbook) produces confidently-wrong guidance. Every capability detection needs a fallback to "unknown → use default path."

Sub-milestones:

- C1 — POH teleport inventory model (manual config + varbit detection where possible).
- C2 — Equipped-item state: detect currently-equipped items that enable shortcuts (e.g., Max cape teleports, Ring of Dueling charges).
- C3 — Diary tier state model. Wire into branch selection.
- C4 — Skill-cape perk state (Crafting cape teleport, Farming cape Farm teleport, etc.).
- C5 — Quest-progress state beyond completion (e.g., partway through Recipe for Disaster unlocks).
- C6 — First pass wiring C1-C5 into Tier-B branch evaluators across the top 20 most-used sources.
- C7 — Player-capability debug overlay (developer-facing) that shows current detected state so authors can verify branching.

### Tier D — Coverage breadth

- **Mission**: Every source in the game has Quest-Helper-depth guidance, not just one happy-path step chain.
- **Outcome**: All 225 current sources reviewed and expanded; any missing sources added (e.g., any post-2026 content).
- **Estimated PRs**: 50-100+
- **Estimated effort**: 60-120 agent-session-days (heavily parallelizable)
- **Agents**: `osrs-expert`, `runelite-plugin-dev`, `planner`, `code-reviewer`
- **Data sources**: Wiki (primary), TempleOSRS, Loghunters, collectionlog.net
- **MCP dependency**: high. Wiki pulls via `runelite-dev` MCP are the main throughput lever. Design tiers around parallel worktree agents so we can keep working when one agent's MCP drops.
- **Dependencies**: Tiers B and C (otherwise we're adding shallow guidance into an old schema we'll have to rewrite later)
- **Risks**:
  - *Data drift during the sweep*. Object IDs and NPC IDs drift across game updates. Mitigation: every batch of source updates runs the verification suite against live data before merge.
  - *Authoring fatigue / quality decay toward the end of the list*. Mitigation: audit scripts that flag sources with fewer than N guidance steps or missing fields. Re-run after every batch.

Sub-milestones:

- D1 — Author a "deep guidance" bar (checklist of 10 required elements: travel tip, tile-sequence or auto-arrival, combat gear recommendation, kill-loop detection, safespot/strategy note where relevant, bank-and-return detection, etc.). Publish in `CONTRIBUTING.md`.
- D2 — Prioritize the top 40 most-farmed sources first (raids, GWD, DT2, slayer bosses, major clue tiers). Ship in batches of 5-10.
- D3 — Mid-tier sources next (remaining bosses, minigames). Batches of 10.
- D4 — Long-tail (shops, skilling, random events, minor sources).
- D5 — Retrofit all sources to the Tier B/C branch model where applicable.

### Tier E — Alternative methods and meta intelligence

- **Mission**: For every item obtainable from multiple sources, recommend the best source *for this player, right now*.
- **Outcome**: A new "cross-source efficiency" mode that reranks by per-item best path, not per-source best path. Meta advice surfaces in the item detail panel ("this item is also from ToA — at your current invocations, CoX is faster").
- **Estimated PRs**: 10-20
- **Estimated effort**: 15-25 agent-session-days
- **Agents**: `osrs-expert`, `planner`, `architect`, `code-reviewer`
- **Data sources**: Wiki, TempleOSRS (for meta shifts after game updates), community tier lists
- **MCP dependency**: medium
- **Dependencies**: Tier B (branching) and Tier D (deep per-source data)
- **Risks**:
  - *Recommendation staleness*. Meta shifts with Jagex updates. Mitigation: every alternative-method recommendation cites the update date it was authored against, and the plugin surfaces "this recommendation is X months old."
  - *Opinion vs fact*. "Best" depends on gear, account type, enjoyment. Mitigation: recommend based on computed expected time, not authorial opinion. Let the player override.

Sub-milestones:

- E1 — Cross-source recommendation mode (per-item best path).
- E2 — Meta-update dating on every recommendation.

### Tier F — Social, imports, and long-tail polish

- **Mission**: External profile imports, KC trackers, dry-streak feed, CI/CD, JaCoCo coverage reporting, account-specific kill-time modelling.
- **Outcome**: The plugin is the one-stop collection log tool.
- **Estimated PRs**: ongoing
- **Estimated effort**: ongoing
- **Agents**: all, as appropriate
- **Data sources**: collectionlog.net API, TempleOSRS API
- **MCP dependency**: low
- **Dependencies**: Tier A at minimum; individual features may depend on B-E
- **Risks**:
  - *External API dependence*. collectionlog.net and TempleOSRS have outages. Mitigation: cache with TTL, fail soft.
  - *Scope sprawl*. This tier is a magnet for "wouldn't it be cool if." Keep a ruthless cut-list.

Representative sub-items:

- F1 — collectionlog.net profile import.
- F2 — TempleOSRS KC sync.
- F3 — Per-account learned kill times (replace static `killTimeSeconds` with rolling average of actual observed kill cycles, per user, opt-in).
- F4 — Dry-streak / long-streak feed.
- F5 — JaCoCo + 80% gate.
- F6 — GitHub Actions CI for build + test on PR.
- F7 — Issue #134 account-specific calcs.

---

## 5. Project tracking format

Each tier sub-milestone tracks in this same file under the **Status log** at the bottom, using the format:

```
- [ ] B1 — Composable completion conditions         status: planned     owner: —          updated: 2026-04-16
- [/] B2 — Tile-sequence pathing                    status: in-progress owner: cha-ndler  updated: 2026-04-20
- [x] A1 — Panel decomposition                      status: done        owner: cha-ndler  updated: 2026-04-18  pr: #355
```

Checkbox symbols: `[ ]` planned, `[/]` in-progress, `[x]` done, `[-]` cancelled, `[?]` blocked.

Review cadence: update after every merged PR that closes a sub-milestone, and do a full tier health review at the end of each tier.

---

## 6. Quick wins for next session

Each under ~1 day and independent of the larger roadmap.

1. **Issue triage sweep**. Close #319 and #323 (already verified). Label #314 and #306 as `needs-in-game-capture`. Label #134 as `tier-F`. Write a one-line comment on each explaining the disposition. *~1 hour.*

2. **Authoring audit script**. A test that loads `drop_rates.json` and reports any source missing: `guidanceSteps`, a `worldX/Y/plane`, kill time, or fewer than 2 steps. Fails the build if the set regresses. Serves as Tier D's authoring bar entrypoint. *~0.5 day.*

3. **`CollectionLogHelperPlugin` LOC trim**. Extract the remaining event dispatch into a `GuidanceEventRouter` (or similar). Target: <1,100 LOC in one PR, <1,000 LOC in the next. *~0.5-1 day per PR.*

4. **`CollectionLogHelperPanel` ADR**. One-page decision doc in `docs/adr/` choosing the per-mode-controller decomposition pattern before any code change. Avoids bikeshedding once the work starts. *~1 hour.*

5. **Plugin Hub self-review checklist doc**. Populate `docs/plugin-hub-review.md` with the hub's checklist, one line per item, green/red/NA today. Immediately shows the gap for resubmission. *~0.5 day.*

---

## 7. Decision register

Decisions that need human judgment, not agent inference. Each gets a short entry, a status, and a date.

### D-01 — JSON-only vs hybrid Java helpers for guidance

- **Context**: Tier B needs composable branching. JSON schema extensions can probably carry us to parity on the *common* cases (conditional travel, alternative methods, nested alternatives). Quest Helper's per-quest Java class model handles the *uncommon* cases (puzzles, dynamic mid-step re-render, arbitrary varbit predicates) more naturally.
- **Options**:
  - (a) JSON-only. Keep the single source of truth. Extend the schema.
  - (b) Java-only. Rewrite each source as a helper class. Massive migration.
  - (c) Hybrid. JSON remains primary. Sources can *optionally* point at a Java helper class for their guidance, which is invoked instead of the JSON sequencer for that source.
- **Decision**: ✅ **(c) hybrid** — future-proof. JSON stays primary; Java helper is the optional escape hatch for hard sources (puzzles, dynamic mid-step re-render, arbitrary varbit predicates). Pilot one source (likely Cerberus) in B6.
- **Status**: decided 2026-04-16.

### D-02 — Where to draw the line on account state detection

- **Context**: Tier C detects many forms of player state. Some are easy (varbits for quests, skill levels). Some are fragile (POH teleports without entering the house, charges on worn jewellery).
- **Player state defined**: persistent unlocks (quests, skills, diaries, combat achievements, music, fairy ring tablets), current loadout (equipped items, inventory, prayer points, run energy, spellbook, prayer book), POH access (built teleports, jewellery box tier), charged item state (Ring of Dueling charges, etc.), activity state (slayer task + master, bounty target, world), bank contents.
- **Options**:
  - (a) Only detect what we can detect cheaply and accurately. Everything else is a manual config override.
  - (b) Detect aggressively with fallbacks to "unknown" → use default path when uncertain.
- **Decision**: ✅ **(b) aggressive detection with fallbacks** — matches the "monkey-proof" goal. Every capability detection must have a default-to-safe fallback when state can't be confirmed. POH teleports get a manual config override as belt-and-suspenders since varbit detection is fragile.
- **Status**: decided 2026-04-16.

### D-03 — Data sourcing strategy: minimize in-game authoring runs

- **Context**: Today we still need in-game authoring captures for varbit IDs not in RuneLite source, scene-vs-cache object ID divergences, multi-form NPC IDs during phase transitions, instanced/Sailing coordinate offsets, and dynamic object spawns. The user's goal is to make in-game capture a last resort, not a default.
- **Existing sources covering ~80% of cases**: OSRS Wiki API (drop tables, NPC infoboxes), TempleOSRS API (canonical clog item IDs, EHB times), abextm/osrs-cache GitHub repo (NPC/Object/Item definitions, auto-updated), mejrs.github.io/osrs (spawn coordinates with permalink URLs), Log Hunters Discord + spreadsheet (community-audited rates), runelite-dev MCP server (wraps the above).
- **Known gaps requiring tooling**:
  1. Varbit IDs not in `VarbitID.java` (e.g., GWD KC 3972-3976, Nex 13080) — community-known but unpublished
  2. Cache-ID vs scene-ID divergences (e.g., Shellbane Gryphon entrance: 58441 cache, 58439 scene)
  3. Object op vs in-game action (cache shows "Open"; in-game is "Search" after first use)
  4. Multi-form NPC IDs during phase transitions (Zulrah, KQ, Hydra)
  5. Sailing/instance coordinate transformations (boat-local vs world)
  6. Dynamic spawn objects (mole hills, Wintertodt braziers — re-ID each spawn)
- **Decision**: ✅ **Tiered sourcing strategy** — pre-committed:
  - **Tier 1**: Use existing MCP tools (`wiki_lookup`, `npc_lookup`, `object_lookup`, `temple_lookup`, etc.) — covers ~80%
  - **Tier 2**: Build three new tools to close ~15% more, eliminating most in-game runs:
    - `varbit_lookup` MCP — scrape RuneLite `VarbitID.java` + Quest Helper source (`VarbitRequirement`, `Conditions`, etc.) + Wiki page source for community-confirmed varbits. Quest Helper has 70+ requirement classes referencing verified varbits — high-value source we haven't tapped.
    - `verified_scene_ids.json` registry committed to repo — maps examine-ID ↔ scene-ID for known divergences. Authored once, reused forever. Populated from past audit findings + Quest Helper's object references.
    - `cache_diff_check` MCP — runs at PR time, flags when a referenced object ID is suspect (not in cache, or known to have a scene-ID alias).
  - **Tier 3**: In-game authoring as last resort — only for truly dynamic state (mid-fight phase transitions, freshly-spawned dynamic objects in instances). Estimated <5% of remaining cases.
- **Effort estimate**: 3-5 agent-days to build the three Tier-2 wrappers + seed the registry from existing audit notes and Quest Helper source.
- **Sequence**: Should be done before Tier D (coverage breadth) — tools amortize across hundreds of source updates.
- **Status**: decided 2026-04-16. Build before Tier D begins.

### D-04 — Quest Helper reference depth: how much of their patterns to adopt

- **Context**: We claim Quest-Helper-level guidance as a goal. How literally do we clone their step type names, class hierarchy, state model?
- **Options**:
  - (a) Port patterns, not names. We're our own thing inspired by Quest Helper, not a clone.
  - (b) Adopt their naming where compatible.
- **Decision**: ✅ **(a) port patterns, not names** — we're "Collection Log Helper" because Quest Helper exists; we want our own flavor. Naming reflects collection log mechanics, not quest mechanics. E.g., we'll have `SourceHelper` not `QuestHelper`, `DropMethod` not `Quest`, `CompletionCondition` not `QuestStep`. Mission is the same "so easy a monkey could do it" UX bar.
- **Status**: decided 2026-04-16.

### D-06 — Data confidence brain: source-of-truth precedence per data type

- **Context**: When Wiki, TempleOSRS, Loghunters, abextm cache, and Quest Helper source disagree on the same fact, which wins? Today this is *implicit* in our MCP tooling (e.g., we use TempleOSRS for clog item IDs because we know wiki diverges). Making it *explicit* lets future agents apply policy without guessing, and lets us learn from past corrections.
- **Approaches discussed**:
  - (a) **Hard-coded precedence table** in our codebase or MCP — explicit but doesn't learn from new evidence.
  - (b) **`continuous-learning-v2` driven** — track every time a source was wrong, downgrade its confidence for that data type.
  - (c) **Hybrid** — start with a hard-coded precedence table seeded from current knowledge, layer continuous-learning instincts on top to adjust over time.
- **Initial precedence (seeds the table; will be tuned)**:

  | Data type | Primary | Secondary | Tertiary |
  |-----------|---------|-----------|----------|
  | Drop rates | Wiki | Log Hunters spreadsheet | TempleOSRS |
  | Collection log item IDs | TempleOSRS | Wiki (known to diverge — fail loud if used) | — |
  | NPC IDs (canonical form) | abextm cache | Wiki infobox | mejrs map |
  | NPC IDs (multi-form bosses) | Authoring log capture | Wiki phase docs | — |
  | Varbit IDs (named) | RuneLite `VarbitID.java` | Quest Helper requirement classes | Wiki page source |
  | Varbit IDs (unnamed/community) | Quest Helper requirement classes | Wiki page source | Authoring log |
  | Object cache definitions | abextm cache | — | — |
  | Object scene IDs | Our `verified_scene_ids.json` | abextm cache (with divergence flag) | Authoring log |
  | Spawn coordinates | mejrs map | Wiki | Authoring log |
  | Kill times (EHB) | TempleOSRS EHB API | Log Hunters spreadsheet | Wiki strategy pages |
  | Quest/diary requirements | Wiki | RuneLite `Quest.java` | — |

- **Decision**: ✅ **(c) hybrid** — implement seeded precedence table now (in A5.6), wire continuous-learning instinct feedback in a follow-up. The continuous-learning hooks are already firing, so we'll have data to mine.
- **Status**: decided 2026-04-16. Build alongside A5.

### D-05 — Cross-source recommendation opinionation (deferred — Tier E)

- **Context**: Tier E will sometimes recommend a source over another for a specific item. "For ancestral hat, prefer ToA 300 over CoX." That's an opinion even when backed by math.
- **Options**:
  - (a) Surface the math and let the player choose.
  - (b) Hard recommendation with a tiebreaker.
  - (c) Opinionated default, soft override in config.
- **Recommendation**: (c).
- **Status**: open. Decide at Tier E kickoff.

---

## 8. Day 0 → present: completed work

Captured for lessons-learned and to anchor the status log against real history. PRs ordered most-recent first.

### Phase 0 — Pre-roadmap baseline (everything before 2026-04-04)

The plugin existed and was functional with most of its current feature surface: 225 sources, efficiency scoring, guidance overlays, sidebar panel, Shortest Path integration, account-aware filtering. PRs #1-#284 covered initial implementation and iterative refinement. Not enumerated here — see `git log` for full history.

### Phase 1 — Pre-submission hardening (2026-04-04 → 2026-04-15)

Focus: data accuracy and audit cleanup before first Plugin Hub submission attempt.

| PR | Title | Date |
|----|-------|------|
| #285 | Test coverage: add 172 tests for 8 high-priority classes | 2026-04-04 |
| #286 | Data audit: 2026-04-04 | 2026-04-04 |
| #287 | Fix flaky tests from PR #285 review | 2026-04-04 |
| #288 | Remove incorrect requiredItemIds from Grotesque Guardians | 2026-04-04 |
| #289 | Remove dead useItemOnObject field from ItemHighlightOverlay | 2026-04-04 |
| #290 | Feature audit fixes: categories, sort options, data improvements | 2026-04-04 |
| #298 | Add object highlighting to 22 sources' guidance steps | 2026-04-04 |
| #299 | Add GWD doors, Slayer Tower stairs, and more object IDs (13 sources) | 2026-04-04 |
| #304 | Data: iron kill time, auto-completion, object IDs for 17 sources | 2026-04-05 |
| #307 | Remove Proximity display mode | 2026-04-04 |
| #308 | Data audit: 2026-04-06 | 2026-04-06 |
| #309 | Shades of Mort'ton: dynamic chest highlighting | 2026-04-07 |
| #310 | Add entrance objectIds, fix NPC IDs, upgrade MANUAL steps | 2026-04-07 |
| #312 | Add Custodian Stalker entrance, upgrade Armoured Zombie to ACTOR_DEATH | 2026-04-07 |
| #313 | Add afkLevel to all sources, fix wilderness ratings | 2026-04-07 |
| #320 | Fix Fossil Island Notes milestones + Revenants iron kill time | 2026-04-15 |
| #326 | Snapshot getLocalPlayer() to prevent NPE/TOCTOU in 4 sites | 2026-04-15 |
| #327 | Fix Skotizo Dark totem drop rate | 2026-04-15 |
| #328 | Add raidRateConvention to 9 raid sources | 2026-04-15 |

**Plugin Hub PR opened**: runelite/plugin-hub#11156, 2026-03-20.

### Phase 2 — Post-submission decomposition + close (2026-04-15 → 2026-04-16)

Focus: god-object decomposition, build hardening, test expansion. Resulted in the Plugin Hub PR being closed by maintainer for excessive churn (4k-line diffs on a queued submission).

| PR | Title | Plugin LOC impact |
|----|-------|-------------------|
| #329 | Plugin Hub readiness (metadata, icon, CHANGELOG) | n/a (build/docs) |
| #330 | Correct NPC IDs and oversized completionDistance values | n/a (data) |
| #331 | Extract OverlayRegistry and SceneObjectRouter from plugin class | -46 |
| #332 | Replace personal name with GitHub alias in copyright headers | n/a (privacy) |
| #333 | shadowJar cleanup + pin runeLiteVersion (#324, #325) | n/a (build); shadowJar 35MB → 293KB |
| #334 | Rename SceneObjectRouter to SceneEventRouter, absorb 4 handlers | -60 |
| #335 | Add 63 tests for OverlayRegistry, SceneObjectRouter, PluginDataManager, DataSyncState | n/a (tests); 440 → 503 |
| #336 | Extract AuthoringLogger from plugin class | -86 |
| #337 | Extract SyncStateCoordinator from plugin class | -216 |
| #338 | Extract GuidanceUIState from plugin class | -9 |
| #339 | Extract GuidanceOverlayCoordinator from plugin class | -630 |

**Net plugin class change**: 2,192 → 1,281 LOC (-911 LOC, -42%).

**Plugin Hub PR closed**: runelite/plugin-hub#11156, 2026-04-16, after reviewer feedback on churn. To be resubmitted fresh after Tier A polish.

### Lessons learned (already incorporated into section 3 playbook)

- **Don't push to a queued review PR** — every push resets the reviewer's mental model and is read as impatience. We did this twice on #11156 and got called out.
- **Don't submit 4k-line diffs to a new-plugin submission** — break into MVP + post-acceptance follow-ups.
- **Resubmit fresh, don't reopen** — closed PRs carry baggage; new PRs signal "took feedback seriously."
- **Cross-repo issue/PR links need fully-qualified `owner/repo#NNN`** — bare `#NNN` auto-links to the *current* repo's PR with that number, which on plugin-hub points at completely unrelated PRs and looks unprofessional.
- **Privacy in commits is non-negotiable** — author name `cha-ndler`, email is GitHub noreply, no Claude attribution, no real first names anywhere in source.
- **Architectural decomposition is best done in many small PRs** — the 4 lifecycle extractions (P1-P4) merged cleanly because each was a single focused service. The earlier monolithic refactor attempts reached for too much at once.
- **Worker agent prompts must be self-contained** — each parallel worktree agent gets zero context from the orchestrating session. Prompt clarity is the bottleneck on parallelism.

---

## 9. Status log

**Tier A — Production polish for Plugin Hub resubmission**

- [x] A1 — Panel decomposition                          status: done          owner: cha-ndler  updated: 2026-04-17  pr: #349  note: 5 mode controllers extracted (94–297 LOC each) behind PanelModeDispatcher; 468/468 tests green. Shell still 1,288 LOC (shared widgets) — follow-up A1b.
- [x] A1b — Shell widget decomposition                   status: done          owner: cha-ndler  updated: 2026-04-17  pr: #351  note: 6 widgets extracted (SyncStatusView, ClueSummaryView, GuidanceBannerView, StepProgressView, SlayerStrategyView, QuickGuidePanelView); shell 1,288 → 698 LOC (−46%); 511 tests green. Shell didn't hit <600 target (final-field constructor overhead ~175 LOC) but is under the 800-LOC Plugin Hub flag threshold.
- [x] A2 — Plugin class <1,000 LOC                       status: done          owner: cha-ndler  updated: 2026-04-17  pr: #347  note: 1,281 → 904 LOC via GuidanceEventRouter extraction
- [x] A3 — Issue triage & labels                         status: done          owner: cha-ndler  updated: 2026-04-17  note: GitHub-only — summary in #345; 10 labels created, #319 closed, #323/#314/#306/#134 labeled
- [x] A4 — Plugin Hub self-review doc                    status: done          owner: cha-ndler  updated: 2026-04-17  pr: #346  note: 27 green / 3 yellow / 2 red — docs/plugin-hub-review.md
- [x] A5 — Screenshots refresh                           status: done          owner: cha-ndler  updated: 2026-04-16  note: docs/screenshots/ populated
- [/] A6 — Tag v1.0.0-hub, wait a week, resubmit         status: in-progress   owner: cha-ndler  updated: 2026-04-17  pr: #355  note: CHANGELOG promoted to 1.0.0-hub, resubmission checklist drafted (docs/plugin-hub-resubmission.md), tag v1.0.0-hub drafted locally — awaiting quiet-week validation before `git push origin v1.0.0-hub`.

**Tier A.5 — Data sourcing infrastructure**

- [x] A5.1 — `varbit_lookup` MCP tool                    status: done          owner: cha-ndler  updated: 2026-04-16  note: closed by runelite-dev-toolkit v0.2.0 — varbit_lookup MCP tool
- [x] A5.2 — `verified_scene_ids.json` registry          status: done          owner: cha-ndler  updated: 2026-04-17  pr: #342
- [x] A5.3 — `cache_diff_check` MCP tool                 status: done          owner: cha-ndler  updated: 2026-04-16  note: closed by runelite-dev-toolkit v0.2.0 — cache_diff_check MCP tool
- [x] A5.4 — Tiered sourcing strategy in CONTRIBUTING    status: done          owner: cha-ndler  updated: 2026-04-17  pr: #341
- [x] A5.5 — In-game authoring playbook generator        status: done          owner: cha-ndler  updated: 2026-04-16  note: closed by runelite-dev-toolkit v0.2.0 — authoring_playbook MCP tool
- [x] A5.6 — Data confidence brain (D-06)                status: done          owner: cha-ndler  updated: 2026-04-16  note: closed by runelite-dev-toolkit v0.2.0 — data_source_router + precedence.json (auto-tuner deferred)

**Tier B — Guidance depth parity with Quest Helper**

- [/] B1 — Composable completion conditions             status: in-progress   owner: cha-ndler  updated: 2026-04-17
- [ ] B2 — Tile-sequence pathing                        status: planned       owner: —          updated: 2026-04-16
- [ ] B3 — Nested conditional steps                     status: planned       owner: —          updated: 2026-04-16
- [ ] B4 — Alternative-method modelling                 status: planned       owner: —          updated: 2026-04-16
- [ ] B5 — Puzzle/dynamic step type                     status: planned       owner: —          updated: 2026-04-16
- [ ] B6 — Decision D-01 pilot (hybrid Java helper)     status: planned       owner: —          updated: 2026-04-16

**Tier C — Player-aware guidance**

- [ ] C1 — POH teleport inventory model                 status: planned       owner: —          updated: 2026-04-16
- [ ] C2 — Equipped-item state                          status: planned       owner: —          updated: 2026-04-16
- [ ] C3 — Diary tier state                             status: planned       owner: —          updated: 2026-04-16
- [ ] C4 — Skill-cape perk state                        status: planned       owner: —          updated: 2026-04-16
- [ ] C5 — Partial-quest state                          status: planned       owner: —          updated: 2026-04-16
- [ ] C6 — Wire into top-20 sources                     status: planned       owner: —          updated: 2026-04-16
- [ ] C7 — Player-capability debug overlay              status: planned       owner: —          updated: 2026-04-16

**Tier D — Coverage breadth**

- [/] D1 — Deep-guidance authoring bar                  status: in-progress   owner: cha-ndler  updated: 2026-04-17  pr: pending
- [ ] D2 — Top-40 sources deep pass                     status: planned       owner: —          updated: 2026-04-16
- [ ] D3 — Mid-tier sources deep pass                   status: planned       owner: —          updated: 2026-04-16
- [ ] D4 — Long-tail sources deep pass                  status: planned       owner: —          updated: 2026-04-16
- [ ] D5 — Retrofit all sources to Tier B/C branch      status: planned       owner: —          updated: 2026-04-16

**Tier E — Alternative methods and meta intelligence**

- [ ] E1 — Cross-source recommendation mode             status: planned       owner: —          updated: 2026-04-16
- [ ] E2 — Meta-update dating                           status: planned       owner: —          updated: 2026-04-16

**Tier F — Social, imports, and long-tail polish**

- [ ] F1 — collectionlog.net import                     status: planned       owner: —          updated: 2026-04-16
- [ ] F2 — TempleOSRS KC sync                           status: planned       owner: —          updated: 2026-04-16
- [ ] F3 — Per-account kill-time learning               status: planned       owner: —          updated: 2026-04-16
- [ ] F4 — Dry-streak feed                              status: planned       owner: —          updated: 2026-04-16
- [ ] F5 — JaCoCo + 80% gate                            status: planned       owner: —          updated: 2026-04-16
- [ ] F6 — GitHub Actions CI                            status: planned       owner: —          updated: 2026-04-16
- [ ] F7 — Issue #134 account-specific calcs            status: planned       owner: —          updated: 2026-04-16
