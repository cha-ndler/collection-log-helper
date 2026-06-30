# Data maintenance & drift workflow

How we keep `drop_rates.json` accurate over time — both **validating the metas we already
have** and **catching new content** the game ships, so the plugin can be updated retroactively.

There are two loops. The first proves what we ship is correct *now*; the second tells us
*when* reality has moved and what to re-check. They share one trigger (the abextm cache
revision) so we re-validate exactly when there is new game data, not on a blind timer.

---

## Loop A — sustained accuracy validation (all metas, all items)

**Goal:** every source's guidance reflects the *current* obtaining meta (location, travel,
requirements, gear/prayer, drop mechanics & rates), not just that it drives mechanically.

**Why a fan-out, not one-at-a-time:** a single source takes a full wiki/cache cross-check;
serially that is ~225 long turns. Fanning out N independent verifiers (one per source) runs
the expensive verification in parallel — the proven pattern from the 2026-06-29 sample run
(17 sources, ~5 min wall-clock, found 2 blockers + 16 highs).

**Mechanism (the accuracy-sample / fix-spec Workflow):**
1. Pick a batch — a stratified random sample for a confidence estimate, or a full sweep in
   slices of ~20 to march through all 226.
2. `pipeline(sources, verifier)` — each verifier agent:
   - reads the source's `guidanceSteps` / `requiredItemIds` / `items` from `drop_rates.json`,
   - establishes current ground truth from the OSRS wiki (+ spawn/cache for ids/coords),
   - returns a structured verdict (`ACCURATE` | `ISSUES`) with **cite-or-discard** receipts
     and a domain-skeptic gate (legitimate mechanics are never flagged).
3. Triage the scorecard: blockers/highs become one-source-per-PR fixes (each re-verified,
   `validate_drop_rates` + `guidance_lint`, CI-gated); lows are batched/deferred.
4. A second `fix-spec` fan-out can turn confirmed findings into exact, receipt-backed edits.

**Confidence definition:** the corpus is "confident" when a fresh random sample returns a
low blocker/high rate (target: 0 blockers, <1 high per ~20). Track the rate per sweep in
`docs/data-verification-log.md`; re-sweep after each fix batch and after any cache-revision
drift (Loop B).

**Cadence:** march the full corpus in slices between releases; on a cache-revision bump
(Loop B), re-validate the categories most likely touched by the update first.

> The Workflow scripts live under the session's `workflows/scripts/` (e.g.
> `clh-guidance-accuracy-sample`, `clh-accuracy-fix-spec`). They are the reusable templates —
> change the source list / batch slice and re-run.

---

## Loop B — toolkit-independent drift detection (new content & ids)

**Goal:** know when new game data lands and what new collection-log items/ids to add — using
**only plain HTTPS to the same upstreams the toolkit wraps**, so it runs with no MCP, in CI,
on a cron, or anywhere Node can fetch.

Implemented in [`scripts/drift_check.mjs`](../scripts/drift_check.mjs) (read-only; persists
markers under `.drift-cache/`, gitignored).

### Data sources (all direct, no runelite-dev-toolkit)

| Need | Source | Endpoint |
|------|--------|----------|
| Authoritative item/NPC ids (what the plugin uses) | **abextm/osrs-cache** | `api.github.com/repos/abextm/osrs-cache/commits/master` (revision/freshness) + `raw.githubusercontent.com/abextm/osrs-cache/<sha>/<n>.flatcache` (bytes, parsed via the `@abextm/cache2` npm package) |
| Semantics: new items, rates, requirements, clog membership | **OSRS Wiki** | `oldschool.runescape.wiki/api.php` (MediaWiki: `recentchanges`, `search`, `parse` for item-infobox ids) |
| Spawn coordinates | **mejrs/data_osrs** | `raw.githubusercontent.com/mejrs/data_osrs/.../master` |
| Official patch notes | **OSRS news** | `oldschool.runescape.com/news` (fetch) |

The abextm commit message carries the **cache revision** string (e.g.
`Cache version 2026-06-25-rev239`). A changed revision == a game update shipped new/changed
cache data — the precise, low-noise trigger for both loops.

### What `drift_check.mjs` does

1. Fetch the latest abextm cache revision; diff against the last-seen marker. Changed ⇒
   **NEW CACHE REVISION** verdict (re-run Loop A on the likely-affected categories).
2. Pull wiki `recentchanges` since the last run, filter to clog-relevant titles
   (collection log / pet / jar / relic / medallion / vestige / …).
3. Load the plugin's covered item ids (currently 226 sources / 1701 distinct ids) to diff
   against new-item candidates.
4. Emit a drift report + candidate list (`--json` for machine consumption) and persist
   `.drift-cache/last-seen.json` (gitignored) so the next run is a true diff.

### Turning drift into updates

New cache revision **or** new clog item on the wiki → resolve the item id (wiki infobox, or
parse abextm flatcache with `@abextm/cache2` for exact id↔name) → feed the candidate into the
`add-source` / `drop-rate-entry` authoring pipeline → the new/changed source then flows
through Loop A's verification before it ships. The two loops compose: **B finds what changed,
A proves the fix is right.**

### Suggested automation

- Schedule `node scripts/drift_check.mjs` (cron / CI weekly + post-patch-Wednesday). On a
  NEW-CACHE verdict, open a tracking issue with the candidate list and kick Loop A.
- It needs no secrets and no MCP — only outbound HTTPS — so it is safe to run headless.
