#!/usr/bin/env python3
"""Static guidance-correctness analyzer for `drop_rates.json`.

Re-derives, by reading the data alone, the class of guidance-step config bugs
that the maintainer has been finding by hand in-game. Every invariant asserted
here is grounded in the live completion semantics of the guidance engine; the
source citation for each rule is recorded next to the detector so a reviewer can
confirm the rule against the Java rather than trusting a linter heuristic.

Detectors implemented (the statically-decidable, data-level classes):

  D1  Data-invariant violations — a step whose `completionCondition` is missing
      the field(s) the engine REQUIRES for that condition to ever fire. Derived
      from CompletionChecker + StepAdvancer:
        * ITEM_OBTAINED / INVENTORY_HAS_ITEM / INVENTORY_NOT_HAS_ITEM
              require completionItemId > 0
              (CompletionChecker.java:96-121, 322-327)
        * ARRIVE_AT_TILE   requires worldX > 0
              (CompletionChecker.java:236, 329)
        * ARRIVE_AT_ZONE   requires completionZone of length 5
              (GuidanceStep.getZone, CompletionChecker.java:169-170)
        * NPC_TALKED_TO    requires completionNpcId > 0
              (CompletionChecker.java:142-143)
        * ACTOR_DEATH      requires completionNpcId > 0 OR non-empty
                           completionNpcIds (matchesCompletionNpc;
              CompletionChecker.java:131-132, GuidanceStep.java:458-475)
        * CHAT_MESSAGE_RECEIVED requires completionChatPattern
              (CompletionChecker.java:269-270)
        * VARBIT_AT_LEAST  requires completionVarbitId > 0
              (CompletionChecker.java:153-154)
        * PLAYER_ON_PLANE  always satisfiable (no required field)

  D1b Loop-config violations — derived from StepAdvancer.advance
      (StepAdvancer.java:135-137): a loop runs only when BOTH
      loopBackToStep > 0 AND loopCount > 0.
        * loopBackToStep > 0 but loopCount <= 0  -> never loops (the #739/B bug)
        * loopCount > 0 but loopBackToStep <= 0  -> dead loopCount (no effect)
        * loopBackToStep out of 1..len range     -> would index out of bounds
                           (nextIndex = loopBackToStep - 1; StepAdvancer.java:142)
      A "never loops" finding whose loopBackToStep targets its OWN step (a
      self-loop) is flagged by_design: it cannot express a multi-step loop and is
      the deliberate D4 E4 "activity loop" marker every skilling source must carry
      (D4Batch3RegressionTest asserts loopBackToStep>0). Only the earlier-target
      "never loops" findings are the real #739/B intended-loop-broken bug.

  D2  Auto-advance dead-ends — a non-final MANUAL step blocks the hands-free
      guidance flow because the engine never auto-completes MANUAL
      (CompletionChecker.isStepAlreadySatisfied returns false for it once parked;
      MANUAL is absent from every satisfying-evaluation path). signal_kind records
      whether the step carries an ITEM signal (skipIfHasAnyItemIds / groundItemIds /
      completionItemId — could drive an auto-complete) vs a HIGHLIGHT-only signal
      (npcId / objectId — overlay-only, never completes a step). Only an item-signal
      MANUAL is a candidate unwired dead-end (the #729 class). An item-signal MANUAL
      inside a recurring-gather sequence is flagged by_design: the signal is
      intentionally suppressed (GuidanceSequencer.isRecurringGatherSequence,
      #707/#715/#719), onItemObtained completes the sequence, and the conditionTree
      any-of-items vehicle is not wired into the live engine.

This harness is side-effect-free: it never edits drop_rates.json.

Usage:
    python scripts/audit_guidance_config.py [--data PATH] [--json-output PATH]
    python scripts/audit_guidance_config.py --calibrate   # assert known counts
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Optional

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DATA_PATH = (
    REPO_ROOT / "src" / "main" / "resources" / "com" / "collectionloghelper" / "drop_rates.json"
)

# Conditions and the field each one REQUIRES to ever auto-complete.
# Value is a predicate over the raw step dict.
def _has_item(s):       return int(s.get("completionItemId", 0) or 0) > 0
def _has_world_x(s):    return int(s.get("worldX", 0) or 0) > 0
def _has_zone(s):       return isinstance(s.get("completionZone"), list) and len(s["completionZone"]) == 5
def _has_talk_npc(s):   return int(s.get("completionNpcId", 0) or 0) > 0
def _has_death_npc(s):  return int(s.get("completionNpcId", 0) or 0) > 0 or bool(s.get("completionNpcIds"))
def _has_chat(s):       return bool(s.get("completionChatPattern"))
def _has_varbit(s):     return int(s.get("completionVarbitId", 0) or 0) > 0

REQUIRED_FIELD = {
    "ITEM_OBTAINED":        (_has_item,     "completionItemId > 0"),
    "INVENTORY_HAS_ITEM":   (_has_item,     "completionItemId > 0"),
    "INVENTORY_NOT_HAS_ITEM": (_has_item,   "completionItemId > 0"),
    "ARRIVE_AT_TILE":       (_has_world_x,  "worldX > 0"),
    "ARRIVE_AT_ZONE":       (_has_zone,     "completionZone of length 5"),
    "NPC_TALKED_TO":        (_has_talk_npc, "completionNpcId > 0"),
    "ACTOR_DEATH":          (_has_death_npc, "completionNpcId > 0 or completionNpcIds non-empty"),
    "CHAT_MESSAGE_RECEIVED": (_has_chat,    "completionChatPattern set"),
    "VARBIT_AT_LEAST":      (_has_varbit,   "completionVarbitId > 0"),
    # PLAYER_ON_PLANE: always satisfiable; MANUAL: handled by D2.
}

WIREABLE_SIGNAL_FIELDS = ("skipIfHasAnyItemIds", "groundItemIds")


@dataclass
class Finding:
    detector: str        # D1 / D1b / D2
    severity: str        # HIGH / MEDIUM / LOW
    source: str
    step_index: int      # 0-based index into guidanceSteps
    condition: Optional[str]
    message: str
    description: str
    # ---- structural classification (additive; consumed by the board discriminator) ----
    # by_design marks a finding that is mechanically true (the detector fired) but is NOT
    # a player-facing bug, with the rationale recorded so a reviewer can confirm it against
    # the engine rather than trusting a name match. The raw detector counts (and therefore
    # --calibrate) are unaffected; only the REAL/by-design split downstream reads these.
    by_design: bool = False
    by_design_reason: Optional[str] = None
    signal_kind: Optional[str] = None       # D2 only: "item" | "highlight" | "none"
    loop_back_to_step: Optional[int] = None  # D1b only
    loop_count: Optional[int] = None         # D1b only

    def key(self):
        return (self.detector, self.source, self.step_index, self.message)


def _steps(source: dict) -> list:
    return source.get("guidanceSteps") or []


def detect_d1(source: dict) -> list[Finding]:
    out = []
    name = source.get("name", "?")
    steps = _steps(source)
    for i, st in enumerate(steps):
        cond = st.get("completionCondition")
        if cond not in REQUIRED_FIELD:
            continue
        pred, desc = REQUIRED_FIELD[cond]
        if not pred(st):
            out.append(Finding(
                detector="D1",
                severity="MEDIUM",
                source=name,
                step_index=i,
                condition=cond,
                message=f"{cond} step missing required field ({desc}) -> never auto-advances",
                description=(st.get("description") or "")[:120],
            ))
    return out


def detect_d1b(source: dict) -> list[Finding]:
    out = []
    name = source.get("name", "?")
    steps = _steps(source)
    n = len(steps)
    for i, st in enumerate(steps):
        lbs = int(st.get("loopBackToStep", 0) or 0)
        lc = int(st.get("loopCount", 0) or 0)
        if lbs > 0 and lc <= 0:
            # A self-loop (loopBackToStep targets its OWN step, lbs == i + 1) cannot express
            # a multi-step loop and is inert with loopCount<=0 (StepAdvancer.java:135-137).
            # It is not a broken loop: it is the deliberate marker the D4 "activity loop"
            # contract requires for skilling sources (D4Batch3RegressionTest E4 asserts
            # loopBackToStep>0). Such grinds are open-ended and complete via onItemObtained,
            # not a counted loop. By-design; only the earlier-target loops (lbs != i + 1) are
            # the #739/B "intended loop silently broken" bug.
            self_loop = (lbs == i + 1)
            out.append(Finding(
                detector="D1b", severity="MEDIUM", source=name, step_index=i,
                condition=st.get("completionCondition"),
                message=f"loopBackToStep={lbs} but loopCount={lc} -> loop never engages",
                description=(st.get("description") or "")[:120],
                by_design=self_loop,
                by_design_reason=(
                    "self-loop: loopBackToStep targets its own step, so it cannot express a "
                    "multi-step loop and is inert with loopCount<=0; it is the deliberate D4 "
                    "E4 'activity loop' marker for an open-ended skilling grind "
                    "(D4Batch3RegressionTest requires loopBackToStep>0), which completes via "
                    "onItemObtained, not a counted loop"
                ) if self_loop else None,
                loop_back_to_step=lbs,
                loop_count=lc,
            ))
        if lc > 0 and lbs <= 0:
            out.append(Finding(
                detector="D1b", severity="LOW", source=name, step_index=i,
                condition=st.get("completionCondition"),
                message=f"loopCount={lc} but loopBackToStep={lbs} -> loopCount has no effect",
                description=(st.get("description") or "")[:120],
            ))
        if lbs > 0 and not (1 <= lbs <= n):
            out.append(Finding(
                detector="D1b", severity="HIGH", source=name, step_index=i,
                condition=st.get("completionCondition"),
                message=f"loopBackToStep={lbs} is outside 1..{n} -> out-of-range loop target",
                description=(st.get("description") or "")[:120],
            ))
    return out


def detect_d2(source: dict) -> list[Finding]:
    out = []
    name = source.get("name", "?")
    steps = _steps(source)
    n = len(steps)
    # A recurring-gather sequence (any step declares restockIfMissingAllItemIds) makes the
    # engine deliberately SUPPRESS the skipIfHasAnyItemIds auto-advance so a gather step's
    # highlight survives past the first pickup (GuidanceSequencer.isRecurringGatherSequence,
    # #707/#715/#719); the sequence still completes via onItemObtained.
    recurring_gather = any(s.get("restockIfMissingAllItemIds") for s in steps)
    for i, st in enumerate(steps):
        if st.get("completionCondition") != "MANUAL":
            continue
        if i == n - 1:
            continue  # a final MANUAL step ends the sequence; nothing to dead-end into
        # An ITEM signal (skipIfHasAnyItemIds/groundItemIds/completionItemId) could drive an
        # auto-complete; a HIGHLIGHT signal (npc/object id) is overlay-only and never
        # completes a step. Only an item signal makes a MANUAL step a candidate dead-end.
        item_signal = (
            any(st.get(f) for f in WIREABLE_SIGNAL_FIELDS)
            or int(st.get("completionItemId", 0) or 0) > 0
        )
        highlight_signal = (
            int(st.get("npcId", 0) or 0) > 0
            or int(st.get("objectId", 0) or 0) > 0
            or bool(st.get("objectIds"))
        )
        signal_kind = "item" if item_signal else ("highlight" if highlight_signal else "none")
        has_signal = item_signal or highlight_signal
        sev = "MEDIUM" if has_signal else "LOW"
        why = ("carries a wireable completion signal "
               "(skipIfHasAnyItemIds/groundItemIds/item/npc/object) that is not wired"
               if has_signal else "no obvious auto-completion signal available")
        # The item signal exists but is intentionally suppressed in a recurring-gather
        # sequence, and the conditionTree any-of-items vehicle is not wired into the live
        # engine -- so this is the by-design #729 case, not an unwired-bug dead-end.
        by_design = item_signal and recurring_gather
        out.append(Finding(
            detector="D2", severity=sev, source=name, step_index=i,
            condition="MANUAL",
            message=f"non-final MANUAL step blocks hands-free auto-advance; {why}",
            description=(st.get("description") or "")[:120],
            signal_kind=signal_kind,
            by_design=by_design,
            by_design_reason=(
                "recurring-gather sequence (a step declares restockIfMissingAllItemIds): the "
                "skipIfHasAnyItemIds advance is intentionally suppressed "
                "(GuidanceSequencer.isRecurringGatherSequence, #707/#715/#719) and "
                "onItemObtained completes the sequence; the conditionTree any-of-items vehicle "
                "is not wired into the live engine -- by-design MANUAL (#729)"
            ) if by_design else None,
        ))
    return out


def analyze(data: list) -> list[Finding]:
    findings = []
    for source in data:
        findings.extend(detect_d1(source))
        findings.extend(detect_d1b(source))
        findings.extend(detect_d2(source))
    return findings


def load(path: Path) -> list:
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


SEV_ORDER = {"HIGH": 0, "MEDIUM": 1, "LOW": 2}


def print_report(findings: list[Finding]) -> None:
    by_det: dict[str, list[Finding]] = {}
    for f in findings:
        by_det.setdefault(f.detector, []).append(f)
    for det in ("D1", "D1b", "D2"):
        fs = by_det.get(det, [])
        print(f"\n=== {det}: {len(fs)} finding(s) ===")
        for f in sorted(fs, key=lambda x: (SEV_ORDER[x.severity], x.source, x.step_index)):
            print(f"  [{f.severity:<6}] {f.source} step[{f.step_index}] "
                  f"({f.condition}): {f.message}")
            if f.description:
                print(f"            \"{f.description}\"")
    print(f"\nTotal findings: {len(findings)}")


def calibrate(findings: list[Finding]) -> int:
    """Assert the analyzer re-derives the known-bug counts from #739/#729.

    Two layers are locked here:
      * RAW detector counts (the mechanical truth the maintainer found by hand) --
        these never change as the by-design split is refined, so they remain the
        trust anchor that the detectors are faithful.
      * The BY-DESIGN split (the re-triage): how many of the raw findings are
        provably not player-facing bugs, and why. Locking these makes the
        discriminator un-regressable -- a future edit cannot silently reclassify a
        real defect as by-design (the count would move and fail here).
    """
    actor_death_missing = [f for f in findings
                           if f.detector == "D1" and f.condition == "ACTOR_DEATH"]
    loop_never = [f for f in findings
                  if f.detector == "D1b" and "loop never engages" in f.message]
    loop_never_self = [f for f in loop_never if f.by_design]
    loop_never_real = [f for f in loop_never if not f.by_design]
    shades = [f for f in findings
              if f.detector == "D2" and f.source.startswith("Shades of Mort")]
    d2_item_signal = [f for f in findings if f.detector == "D2" and f.signal_kind == "item"]
    d2_item_real = [f for f in d2_item_signal if not f.by_design]

    ok = True
    print("CALIBRATION (expected from issues #739 / #729):")

    def check(label, got, expect):
        nonlocal ok
        status = "PASS" if got == expect else "FAIL"
        if got != expect:
            ok = False
        print(f"  [{status}] {label}: got {got}, expect {expect}")

    # ---- raw detector counts (post-fix baseline; the C1/N1/C2 fixes in #781/#782/#783
    # changed the underlying data, so these were re-derived 2026-06-09) ----
    check("D1 ACTOR_DEATH missing npc id (#739/A)", len(actor_death_missing), 0)
    # #739/B was 23 raw on the pre-fix data (16 earlier-target bugs + 7 self-loops).
    # The 16 earlier-target loops were removed in #782/#783 and Motherlode Mine was
    # converted to a self-loop for sibling consistency, leaving 8 by-design self-loops.
    check("D1b loopBackToStep w/ loopCount=0 never loops (#739/B), raw", len(loop_never), 8)
    # #729: Shades of Mort'ton has at least one non-final MANUAL dead-end.
    check("D2 Shades of Mort'ton MANUAL dead-end present (#729)",
          1 if len(shades) >= 1 else 0, 1)

    # ---- by-design split (re-triage lock) ----
    # 8 self-loops are the D4 E4 'activity loop' markers (all 8 skilling Batch-3 sources,
    # incl. Motherlode Mine); the 16 earlier-target real bugs were all resolved (#782/#783).
    check("D1b self-loop by-design (D4 E4 markers)", len(loop_never_self), 8)
    check("D1b earlier-target loop-never REAL (#739/B)", len(loop_never_real), 0)
    # The lone item-signal MANUAL (Shades) is by-design (recurring-gather suppression +
    # unwired conditionTree); no item-signal MANUAL is a real unwired dead-end.
    check("D2 item-signal MANUAL real (unwired dead-end)", len(d2_item_real), 0)

    if actor_death_missing:
        print("  ACTOR_DEATH-missing sources:",
              sorted({f.source for f in actor_death_missing}))
    if loop_never_self:
        print("  self-loop (by-design) sources:",
              sorted({f.source for f in loop_never_self}))
    print("\nCALIBRATION", "PASS" if ok else "FAIL")
    return 0 if ok else 1


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--data", type=Path, default=DEFAULT_DATA_PATH)
    ap.add_argument("--json-output", type=Path, default=None)
    ap.add_argument("--calibrate", action="store_true",
                    help="assert the analyzer re-derives known-bug counts")
    args = ap.parse_args(argv)

    data = load(args.data)
    findings = analyze(data)

    if args.json_output:
        args.json_output.write_text(
            json.dumps([asdict(f) for f in findings], indent=2), encoding="utf-8")

    print_report(findings)

    if args.calibrate:
        return calibrate(findings)
    return 0


if __name__ == "__main__":
    sys.exit(main())
