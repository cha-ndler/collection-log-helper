# Guidance-correctness static audit

Static analysis of `drop_rates.json` guidance steps **and** the guidance-engine Java, built to
mechanize the class of in-game guidance bugs the maintainer has been finding by hand. The goal
is to turn in-game testing from *discovery* into *spot-check verification*.

**This folder is the findings record. Fixes are a separate, reviewed step** (one finding per PR,
off `master`, never auto-merged). In-game validation of player-facing fixes is left to a human —
a non-human account must not drive the live game.

## The analyzer

`scripts/audit_guidance_config.py` — data-level detectors (D1/D1b/D2), runnable in CI.

```
python scripts/audit_guidance_config.py            # full report
python scripts/audit_guidance_config.py --calibrate # assert known-bug counts (the trust gate)
```

Every invariant is grounded in the live engine, cited next to its detector:

- **D1 — missing required completion field.** Each `CompletionCondition` requires specific
  fields to ever fire (derived from `CompletionChecker` + `GuidanceStep`). A step lacking them
  can never auto-advance. E.g. `ACTOR_DEATH` needs `completionNpcId > 0` or non-empty
  `completionNpcIds` (`CompletionChecker.java:131-132`, `GuidanceStep.java:458-475`);
  `ARRIVE_AT_ZONE` needs a length-5 `completionZone` (`GuidanceStep.getZone`,
  `CompletionChecker.java:169-170`).
- **D1b — loop never engages.** A loop runs only when BOTH `loopBackToStep > 0` AND
  `loopCount > 0` (`StepAdvancer.java:135-137`). A step with `loopBackToStep` set but
  `loopCount` 0/absent never loops.
- **D2 — auto-advance dead-end.** A non-final `MANUAL` step blocks the hands-free flow; the
  engine never auto-completes `MANUAL`. High-confidence only when a wireable completion signal
  (`skipIfHasAnyItemIds` / `groundItemIds`) is already present but unwired (the #729 class).

Code-level classes (D3 state-lifecycle, D4 hot-path perf/logging) are audited by a reading pass
documented in the findings file, not by the script.

## Calibration (proof the detectors are real)

`--calibrate` re-derives the maintainer's hand-found bugs exactly:

| Known bug | Expected | Analyzer |
|-----------|----------|----------|
| #739/A — ACTOR_DEATH steps missing npc id | 5 (named sources) | **5, exact same sources** |
| #739/B — loopBackToStep with loopCount=0 | "~23" (issue title) | **23** |
| #729 — Shades of Mort'ton MANUAL dead-end | 1 | **1** |
| #737 — tilePointCache not reset (code pass) | confirmed | **confirmed + 3 extra entry points** |
| #738 — quest-state log spam (code pass) | confirmed | **confirmed + PohTeleport sibling** |

The mission brief cited "22" for #739/B; the engine-true count is exactly 23 (matches the
issue's own "~23"). The script's calibration mode fails the build if any count regresses.

## Files

- `findings-guidance-config.md` — the ranked backlog (CONFIRMED vs NEW), each cited.
