# Tier D2 scoping — top-40 sources deep-guidance audit

This document scopes the [Tier D2](../ROADMAP.md#tier-d--coverage-breadth) milestone: a deep-guidance audit and batch-fix pass over the 40 most-farmed collection-log sources. The authoring target is the 10-element checklist defined in [`deep-guidance-bar.md`](deep-guidance-bar.md), which D1 closed.

This file is the *plan*. Each follow-up PR delivers one batch of 5-10 source fixes against `drop_rates.json`, with tests and an in-game-validation note.

---

## 1. Selection criteria

"Top-40 most-farmed" is operationalized as the union of the following signals. A source qualifies for the list if it appears in **two or more** signals, or is a canonical raid/DT2/GWD source that the community treats as core PvM endgame.

| Signal | Source | What it tells us |
|---|---|---|
| **A. Canonical endgame content** | Raids (3 + ToB HM), GWD bosses (4), DT2 bosses (4), top solo bosses (Vorkath, Zulrah, Muspah, Nightmare, Corp, Gauntlets) | Locked-in by community consensus as "the list everyone farms." No quantitative signal needed. |
| **B. TempleOSRS boss popularity rankings** | TempleOSRS hiscores `/efficiency/player-stats` aggregate KC tables | Empirical KC-per-player-week ordering across all tracked bosses. Top-30 boss tab is the canonical popularity proxy in OSRS analytics. |
| **C. Loghunters audit popularity** | Loghunters community drop-log spreadsheets (maintainer audit notes) | Sources with the highest sample sizes on Loghunters spreadsheets are the ones the community is actually grinding. Anything with >100k recorded KC qualifies. |
| **D. Clue-tier engagement** | Hard/Elite/Master clue casket KC across TempleOSRS + Wiki community polls | Clues are the dominant non-boss farm. Beginner/Easy/Medium are skipped in D2 (rolled into D3 long-tail) because their rare-table tail is much shorter. |
| **E. Slayer task frequency** | Wiki task-weighting tables for Duradel + Konar + Nieve | Confirms which slayer bosses are routinely on-task and therefore farmed by accounts past 80 Slayer. |
| **F. RuneLite plugin author intuition** | Maintainer meta judgment | Sanity-checks the empirical list against current 2026 meta — e.g., DT2 bosses ranking far above pre-2024 slayer bosses now that the awakened orbs and virtus drops are entrenched. |

**Excluded from D2 (deferred to D3/D4):**
- Skilling sources beyond Wintertodt / Tempoross (Hespori, Zalcano, Mahogany Homes, etc. — moved to D3).
- Long-tail minigames (Castle Wars, LMS, Soul Wars — D4).
- Sub-popular wilderness pairs (Spindel/Calvarion/Artio variants — out of scope, currently absent from `drop_rates.json`; will be added in D5 alongside the wilderness boss rework).
- Beginner/Easy/Medium clues (rare table is too thin to repay a deep-guidance pass — D3).
- All `OTHER` and `SKILLING` category sources except the two top minigames (Wintertodt, Tempoross).

**Popularity-tier definition:**
- **Tier 1** = "Everyone farms this." Raids, GWD, DT2, top solo bosses, Master clues, Barrows. ~20 sources.
- **Tier 2** = "Common mid-game / high-slayer farms." Hydra, Cerberus, Kraken, Sire, DKS trio, KQ, KBD, top wilderness bosses, Hard/Elite clues, Wintertodt, Tempoross. ~20 sources.
- **Tier 3** = (Not used in D2; reserved for D3.)

---

## 2. The 40-source list

Each row scores presence of the 10 deep-guidance-bar elements by inspecting the source's `guidanceSteps` array in `drop_rates.json` and ticking each element if observable in the JSON. Element 10 (PR citations) is a *process* artifact — it cannot be detected from the JSON, so it is scored 0 across the board and will be addressed per-batch in the follow-up PR template. Maximum *observable* score is therefore **9/10**; sources at 9 are considered "fully authored" by this audit.

Scoring rubric (1 point per element where the signal is present):

- **E1 Travel tip** — `travelTip` non-empty on the source.
- **E2 Auto-arrival** — at least one step uses `ARRIVE_AT_TILE`, `ARRIVE_AT_ZONE`, or `PLAYER_ON_PLANE`.
- **E3 Gear / loadout description** — kill or activity step description names a prayer, weapon class, or combat keyword (`protect`, `prayer`, `spec`, `tbow`, `scythe`, `blowpipe`, `range`, `melee`, `magic`, etc.).
- **E4 Loop detection** — multi-cycle sources declare `loopBackToStep`; single-kill bosses are credited by default.
- **E5 Strategy note** — step description names a mechanic to avoid (`safespot`, `phase`, `special`, `acid`, `dodge`, `avoid`, `flick`, etc.).
- **E6 Bank-and-return** — at least one non-step-0 step declares `requiredItemIds`.
- **E7 Inventory loadout** — step 0 declares `requiredItemIds`.
- **E8 Prerequisites** — `requirements.quests` or `requirements.skills` is set.
- **E9 Drop-rate confidence** — at least 50 % of items in `items[]` carry a `dropRate`.
- **E10 PR citations** — process artifact; not scored from JSON (always 0 in this audit).

| # | Source (drop_rates.json key) | Category | Popularity tier | Score |
|---|---|---|---|---|
| 1  | Chambers of Xeric             | RAIDS     | Tier 1 | 6/10 |
| 2  | Theatre of Blood              | RAIDS     | Tier 1 | 6/10 |
| 3  | Theatre of Blood (Hard Mode)  | RAIDS     | Tier 1 | 5/10 |
| 4  | Tombs of Amascut              | RAIDS     | Tier 1 | 5/10 |
| 5  | General Graardor              | BOSSES    | Tier 1 | 7/10 |
| 6  | Commander Zilyana             | BOSSES    | Tier 1 | 7/10 |
| 7  | K'ril Tsutsaroth              | BOSSES    | Tier 1 | 7/10 |
| 8  | Kree'arra                     | BOSSES    | Tier 1 | 8/10 |
| 9  | Vardorvis                     | BOSSES    | Tier 1 | 7/10 |
| 10 | Duke Sucellus                 | BOSSES    | Tier 1 | 7/10 |
| 11 | The Leviathan                 | BOSSES    | Tier 1 | 7/10 |
| 12 | The Whisperer                 | BOSSES    | Tier 1 | 7/10 |
| 13 | Vorkath                       | BOSSES    | Tier 1 | 7/10 |
| 14 | Zulrah                        | BOSSES    | Tier 1 | 5/10 |
| 15 | Phantom Muspah                | BOSSES    | Tier 1 | 7/10 |
| 16 | The Nightmare                 | BOSSES    | Tier 1 | 7/10 |
| 17 | Phosani's Nightmare           | BOSSES    | Tier 1 | 6/10 |
| 18 | Corporeal Beast               | BOSSES    | Tier 1 | 6/10 |
| 19 | The Gauntlet                  | BOSSES    | Tier 1 | 5/10 |
| 20 | Corrupted Gauntlet            | BOSSES    | Tier 1 | 6/10 |
| 21 | Alchemical Hydra              | BOSSES    | Tier 2 | 6/10 |
| 22 | Cerberus                      | BOSSES    | Tier 2 | 7/10 |
| 23 | Kraken                        | BOSSES    | Tier 2 | 6/10 |
| 24 | Abyssal Sire                  | BOSSES    | Tier 2 | 5/10 |
| 25 | Grotesque Guardians           | BOSSES    | Tier 2 | 6/10 |
| 26 | Vet'ion                       | BOSSES    | Tier 2 | 5/10 |
| 27 | Callisto                      | BOSSES    | Tier 2 | 5/10 |
| 28 | Venenatis                     | BOSSES    | Tier 2 | 6/10 |
| 29 | King Black Dragon             | BOSSES    | Tier 2 | 5/10 |
| 30 | Dagannoth Rex                 | BOSSES    | Tier 2 | 5/10 |
| 31 | Dagannoth Prime               | BOSSES    | Tier 2 | 5/10 |
| 32 | Dagannoth Supreme             | BOSSES    | Tier 2 | 5/10 |
| 33 | Kalphite Queen                | BOSSES    | Tier 2 | 5/10 |
| 34 | Barrows                       | BOSSES    | Tier 1 | 7/10 |
| 35 | Giant Mole                    | BOSSES    | Tier 2 | 5/10 |
| 36 | Wintertodt                    | MINIGAMES | Tier 2 | 4/10 |
| 37 | Tempoross                     | MINIGAMES | Tier 2 | 4/10 |
| 38 | Hard Treasure Trails          | CLUES     | Tier 2 | 4/10 |
| 39 | Elite Treasure Trails         | CLUES     | Tier 2 | 4/10 |
| 40 | Master Treasure Trails        | CLUES     | Tier 1 | 4/10 |

**Audit summary:**
- **18 of 40 (45 %)** sources score below 6/10 against the observable bar and are the priority backfill targets.
- **22 of 40 (55 %)** sources score 6/10 or higher but are still missing at least one element (most commonly E10 PR citations, plus a mix of E5 strategy notes, E6/E7 item loadouts, and E8 prerequisites).
- **No source in this list is a STUB** — every D2 source already has at least one `guidanceSteps` entry. The work is depth, not green-field authoring. (Green-field is D3/D4.)
- The clue-tier and skilling-minigame rows (#36-#40) score uniformly low because their current `guidanceSteps` are kill-loop-style activities without per-cycle prayer/strategy detail; these will dominate Batch 6's effort.

> The score is intentionally conservative — it credits structural presence (`travelTip` exists, `requiredItemIds` is non-empty) without judging quality. A 7/10 source can still have an inadequate `travelTip`. Each batch PR must verify quality against the [deep-guidance-bar](deep-guidance-bar.md) text, not just bump the score.

---

## 3. Batching plan

Six batches, each scoped to a coherent content cluster so a single contributor can authoring-pass it in one session and validate it in-game in one play session. Batches 1-3 are highest priority (canonical raid/boss endgame); batches 4-6 fill out the rest.

| Batch | Theme | Sources | Count | Why grouped |
|---|---|---|---|---|
| **Batch 1** | GWD bosses | General Graardor, Commander Zilyana, K'ril Tsutsaroth, Kree'arra | 4 | Shared travel route (Trollheim teleport -> GWD), shared key-mechanic structure, shared killcount-gate prereq. Single authoring session. |
| **Batch 2** | DT2 bosses | Vardorvis, Duke Sucellus, The Leviathan, The Whisperer | 4 | Shared quest prereq (Desert Treasure II), shared awakened-orb item structure, shared post-2024 meta. Single authoring session. |
| **Batch 3** | Raids | Chambers of Xeric, Theatre of Blood, Theatre of Blood (Hard Mode), Tombs of Amascut | 4 | Highest authoring complexity per source — each raid is multi-room. Smaller batch so each raid gets adequate per-room strategy notes. |
| **Batch 4** | Top solo bosses | Vorkath, Zulrah, Phantom Muspah, The Nightmare, Phosani's Nightmare, Corporeal Beast, The Gauntlet, Corrupted Gauntlet | 8 | Solo-instance bosses with rotation/phase mechanics needing E5 strategy notes. |
| **Batch 5** | Slayer + classic bosses | Alchemical Hydra, Cerberus, Kraken, Abyssal Sire, Grotesque Guardians, Dagannoth Rex, Dagannoth Prime, Dagannoth Supreme, Kalphite Queen, Giant Mole | 10 | High-slayer + classic safespot bosses with shared "single boss room, one prayer, one mechanic" shape. |
| **Batch 6** | Wilderness bosses + Barrows + clues + skilling minigames | Vet'ion, Callisto, Venenatis, King Black Dragon, Barrows, Wintertodt, Tempoross, Hard Treasure Trails, Elite Treasure Trails, Master Treasure Trails | 10 | Mixed non-instance content. Clues + skilling minigames most need new loop-detection (E4) and per-cycle strategy notes (E5). |

**Total**: 6 PRs, 40 sources. Estimated authoring time: 1-2 days per batch; full milestone in ~2-3 calendar weeks if batches are run in parallel via worktrees.

**Ordering rationale:** Batches 1-3 deliver the highest community-visible value first (raids + GWD + DT2 are the loudest "incomplete guidance" reports). Batch 6 is last because clue + skilling deep guidance is the most novel authoring (E4 loop detection on rates that have never had it) and benefits from lessons learned in earlier batches.

---

## 4. Per-batch fix template

Each batch follow-up PR must:

1. **Title** — `feat(data): D2 batch N — <theme> deep-guidance pass (#<issue>)`.
2. **Branch** — `feat/data/d2-batch-<n>-<slug>`.
3. **Diff scope** — `src/main/resources/com/collectionloghelper/drop_rates.json` only (plus generated test fixtures if needed). No code changes; if a batch needs a new completion-condition or schema field, split it into a separate Tier B PR first.
4. **For every source in the batch, address every missing element** from the [deep-guidance-bar](deep-guidance-bar.md). Use the score table above as the starting checklist; verify each element against the bar's full text, not just the rubric heuristic.
5. **Tests** — extend the authoring-audit test (the one that loads `drop_rates.json` and reports missing structural fields) so every source in the batch passes the deep-guidance bar's machine-checkable elements (E1, E2, E6, E7, E8, E9). Element 3/5 quality is verified by the PR reviewer, not the test.
6. **`guidance_lint` and `validate_drop_rates`** — both must report clean (or INFO-only) for every modified source. Cite the result in the PR description.
7. **In-game validation note** — every batch PR description includes a placeholder block:

   ```
   ## In-game validation
   - [ ] Source 1: <author> verified <date> on game version <YYYY-MM-DD update>
   - [ ] Source 2: ...
   ```

   Marked checkboxes constitute the audit trail for [Element 10 — PR citations](deep-guidance-bar.md#element-10--authors-date-stamp-and-source-citations). Boxes need not all be checked before merge — a source can be re-validated post-merge — but unchecked entries must be tracked in the D2 issue's checklist.
8. **Data-source citations** — PR description names the data tier used per element, per [D-03](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs):
   - Drop rates: Wiki primary, Loghunters secondary, TempleOSRS tertiary.
   - Item IDs: TempleOSRS canonical IDs.
   - Coordinates: MCP `coordinate_helper` or RuneLite dev-tools tile readout.
   - NPC IDs: MCP `npc_lookup`.
9. **Roadmap status table** — *not* updated in batch PRs. The D2 row in [`ROADMAP.md`](../ROADMAP.md) §6 stays at `status: planned` until the final batch merges, then is bumped to `status: done` in a single status-log PR.

---

## 5. References

- [Deep guidance bar — the 10 elements](deep-guidance-bar.md) (D1 deliverable)
- [Decision D-03 — data-sourcing precedence](../ROADMAP.md#d-03--data-sourcing-strategy-minimize-in-game-authoring-runs)
- [Tier D status log](../ROADMAP.md#tier-d--coverage-breadth)
- [JSON Schema Reference](schema-reference.md)
- `feedback_loghunters_audit.md` and `reference_loghunters.md` (private memory; community drop-rate signal)
