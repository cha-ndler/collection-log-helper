# Plugin Hub Resubmission Checklist

Readiness gate for the `v1.0.0-hub` resubmission to `runelite/plugin-hub`.

---

## 1. Tier-A items complete

All Tier-A milestones must be `[x] done` in [docs/ROADMAP.md — Status log](ROADMAP.md#9-status-log)
before the tag is pushed.

Current state (as of 2026-05-12):

| Milestone | Status |
|-----------|--------|
| A1 — Panel decomposition | [x] done — PR #349 |
| A1b — Shell widget decomposition | [x] done — PR #351 |
| A2 — Plugin class < 1,000 LOC | [x] done — PR #347 |
| A3 — Issue triage and labels | [x] done |
| A4 — Plugin Hub self-review doc | [x] done — PR #346 |
| A5 — Screenshots refresh | [x] done |
| A5.1–A5.6 — Data sourcing infrastructure | [x] done |
| A6 — Tag v1.0.0-hub (this milestone) | [/] in-progress — tag re-cut pending |

The originally-drafted tag from 2026-04-17 (commit `b9518653`, mapping to PR #357) is stale.
Between that commit and the current `master` HEAD, 13 PRs landed — RuneLite client bump,
Tier B.5 roadmap addition, in-game validation log, and the v1-blocker fix burst from manual
testing on 2026-05-10 (PRs #372, #384, #386–#394).  The tag will be re-cut on the current
`master` HEAD before the Plugin Hub PR is opened.

---

## 2. No open P1 bugs

Verify with:

```
gh issue list --label bug --label "priority: P1" --state open
```

Expected result: empty list.  If any P1 bugs are open, resolve them before pushing the tag.

---

## 3. Plugin Hub self-review all green or triaged

`docs/plugin-hub-review.md` must have no un-triaged **Red** items.

Current open reds (as of 2026-05-12):

| Item | Mitigation |
|------|-----------|
| `runelite-plugin` Gradle plugin missing | Investigate whether Hub CI requires `com.openosrs.externalplugin`; add if needed. Low-risk: the build passes without it and no reviewer comment cited it explicitly in #11156. |
| Files > 800 LOC | Resolved by A1/A1b: `CollectionLogHelperPanel` shell is now 698 LOC; `CollectionLogHelperPlugin` is 904 LOC (borderline, not over). Residuals: `GuidanceOverlayCoordinator` (859), `EfficiencyCalculator` (754), `GuidanceSequencer` (719) — all functional, acceptable at submission. Re-measure residuals after the 2026-05-10 fix burst before submission. |

Re-run `mcp__plugin_runelite-dev-toolkit_runelite-dev__plugin_hub_validate` on the tag commit to
confirm no new findings before opening the PR.

---

## 4. Quiet-week criteria

- No merges to `master` for **7 calendar days** *before* the tag is pushed and the Plugin
  Hub PR is opened. (Reviewer-facing signal: this codebase has stabilized.)
- Exception: post-tag critical bug-fix commits may land; update the `Unreleased` CHANGELOG
  section if so. Avoid this if at all possible.
- Do **not** amend or re-push the tag once it is live.

The tag was originally drafted locally on 2026-04-17 (commit `b9518653`).  That tag is
**stale** — 13 PRs landed afterward, including the v1-blocker fix burst from in-game testing
on 2026-05-10 (PRs #372, #384, #386–#394).  The tag will be deleted locally and re-cut on
the current `master` HEAD once this CHANGELOG/doc refresh PR merges.

**New earliest-push date**: 7 calendar days after the final merge in the v1.0.0-hub
content set lands on `master`.  The Tier B/B.5/C/E/F cascade closed on 2026-05-14 and
the JDK 17 build fix ([#475](../../pull/475)) merged 2026-05-15, so the earliest push
date is **2026-05-22**.  If any additional merges land during the window — for any
reason, including the post-cascade review fixes — the clock resets to 7 days from the
latest merge.

---

## 5. Re-cut and push the tag

The locally-drafted tag from 2026-04-17 must be deleted before re-cutting so the name is
free to point at the current `master` HEAD:

```bash
# Delete the stale local tag (it was never pushed)
git tag -d v1.0.0-hub

# Confirm not on remote (must return empty)
git ls-remote --tags origin | grep v1.0.0-hub

# Re-cut on current master HEAD
git fetch origin master
git tag -a v1.0.0-hub origin/master -m "v1.0.0-hub: Plugin Hub submission"
```

When quiet-week criteria are met, push:

```bash
git push origin v1.0.0-hub
```

---

## 6. Resubmission steps

1. **Create a fork branch** on `cha-ndler/plugin-hub` (your existing fork):
   ```bash
   git clone --depth 1 https://github.com/cha-ndler/plugin-hub.git
   cd plugin-hub
   git checkout -b add-collection-log-helper
   ```

2. **Add the plugin entry** to `plugins.txt` (alphabetical order):
   ```
   cha-ndler:collection-log-helper
   ```

3. **Commit and push** the single-line change:
   ```bash
   git add plugins.txt
   git commit -m "Add Collection Log Helper"
   git push -u origin add-collection-log-helper
   ```

4. **Open a PR** against `runelite/plugin-hub` from your fork branch.  In the PR body:
   - Link to this repo's `v1.0.0-hub` tag.
   - Reference `docs/plugin-hub-review.md` for the self-review summary.
   - Summarize the major features (efficiency scoring, guidance overlays, five display modes).
   - Do **not** mention the prior closed PR (#11156) unless a reviewer asks.

5. **Do not push to the PR branch again** unless a reviewer requests a change.

---

## 7. Internal PR history

For context during review responses:

| PR | Summary |
|----|---------|
| #329 | Plugin Hub metadata, icon, CHANGELOG baseline |
| #333 | shadowJar 35 MB → 293 KB; runeLiteVersion pinned |
| #331–#339, #347 | Plugin class decomposition (2,192 → 904 LOC) |
| #349, #351 | Panel decomposition (1,661 → 698 LOC shell) |
| #346 | Plugin Hub self-review doc |
| #342, #341 | Data-sourcing infrastructure |
| #353, #354 | Fix `StepProgressView.hide()` Component shadow |
| #358 | Deep-guidance authoring bar (D1) |
| #361 | GitHub Actions CI |
| #379 | RuneLite client 1.12.24 → 1.12.26.3 |
| #383 | ROADMAP: Tier B.5 (UI parity) added |
| #384 | Surface required-item availability inline |
| #385 | In-game validation log (2026-05-10) |
| #372 | Mort'ton step ordering, teleport arrival, skip overlay refresh |
| #386 | Show Overlays / Show Hint Arrow toggle propagation |
| #387 | Filter-affecting config rebuild |
| #389 | Required-item name resolution on client thread |
| #390 | Show Overlays toggle no longer aborts guidance |
| #391 | StepProgressView tooltip lookup safety |
| #392 | Locked items rank by score |
| #393 | Hint arrow refresh after teleport |
| #394 | "Per Clue:" labeling fix |
