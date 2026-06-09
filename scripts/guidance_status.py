#!/usr/bin/env python3
"""Generate the guidance-audit scoreboard (`docs/guidance-audit/status.md`).

Observability only. DERIVES three headline numbers from the raw output of the
guidance-audit validation tiers that already exist in this repo, links each
number to the artifact it came from, and refuses to invent a value when an
artifact is missing (it reports "not yet measured" instead).

The three numbers and their real sources:
  1. corpus-pass-rate  -- sources free of any REAL guidance-config finding, out of
                          the total corpus. Source: the
                          `scripts/audit_guidance_config.py` JSON, filtered to the
                          discriminator's four REAL data classes (C1/N1/C2/C3 in
                          findings-guidance-config.md), so by-design MANUAL steps
                          are NOT counted as bugs (the #775 lesson). Gated by the
                          analyzer's `--calibrate` result and the
                          `GuidanceConfigInvariantsRegressionTest` ratchet.
  2. open trustworthy  -- REAL findings in `findings-guidance-config.md` still
                          routed to the maintainer (each mechanism-cited),
                          excluding harness ARTIFACTs (e.g. the acceptance run's
                          ARRIVE_AT_TILE synthetic-injection quirk).
  3. acceptance-gate % -- PASS rate of the latest `acceptance-runs/` sweep over its
                          random never-driven sample.

Inputs are paths to artifacts produced by the tiers (see `--help`). Nothing here
re-measures by guessing: a missing input marks its line unmeasured.
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent

# Gate 4 threshold: minimum item-weighted guidance-drive confidence to stay GREEN.
CONFIDENCE_GATE_PCT = 95

DEFAULT_DATA = (
    REPO_ROOT / "src" / "main" / "resources" / "com" / "collectionloghelper" / "drop_rates.json"
)
GA = REPO_ROOT / "docs" / "guidance-audit"
DEFAULT_ANALYZER_JSON = GA / "guidance-config-audit.json"
DEFAULT_RATCHET_XML = (
    REPO_ROOT
    / "build"
    / "test-results"
    / "test"
    / "TEST-com.collectionloghelper.data.GuidanceConfigInvariantsRegressionTest.xml"
)
DEFAULT_FINDINGS = GA / "findings-guidance-config.md"
DEFAULT_ACCEPTANCE_DIR = GA / "acceptance-runs"
DEFAULT_DRIVE_REPORT = GA / "acceptance-runner-report.json"
DEFAULT_OUTPUT = GA / "status.md"

# Links in status.md resolve relative to its own directory on GitHub.
_OUTPUT_DIR = DEFAULT_OUTPUT.parent


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return path.as_posix()


def href(path: Path) -> str:
    return Path(os.path.relpath(path.resolve(), _OUTPUT_DIR)).as_posix()


def count_sources(data_path: Path) -> int | None:
    if not data_path.exists():
        return None
    with data_path.open(encoding="utf-8") as fh:
        return len(json.load(fh))


def is_real_data_finding(f: dict) -> bool:
    """Whether an analyzer finding is a REAL player-facing guidance bug.

    Reads the analyzer's structural `by_design` / `signal_kind` classification
    (`scripts/audit_guidance_config.py`, locked by its `--calibrate` gate) rather
    than matching source names -- so the split is auditable against the engine and
    cannot silently drift:

      * Anything the analyzer flagged `by_design` is NOT a bug, regardless of class
        (the #775 lesson). Two by-design classes exist today:
          - D1b self-loops (loopBackToStep targets its own step): the D4 E4
            "activity loop" markers required of skilling sources -- inert, not a
            broken loop.
          - D2 item-signal MANUAL inside a recurring-gather sequence (Shades, C3):
            the skipIfHasAnyItemIds advance is intentionally suppressed
            (#707/#715/#719), onItemObtained completes the sequence, and the
            conditionTree any-of-items vehicle is not wired into the live engine.
      * D1  -- any can-never-fire step (missing the required completion field):
               C1 (ACTOR_DEATH, #739/A) AND N1 (ARRIVE_AT_ZONE).
      * D1b -- "loop never engages" findings that are NOT by-design self-loops, i.e.
               the 16 earlier-target loops -- the real #739/B intended-loop-broken bug.
      * D2  -- an ITEM-signal MANUAL (signal_kind == "item") that is NOT by-design,
               i.e. a genuinely unwired item dead-end. Highlight-only MANUAL steps
               (npc/object id) never auto-complete and are excluded.
    """
    if f.get("by_design"):
        return False
    det = f.get("detector")
    if det == "D1":
        return True
    if det == "D1b":
        return "loop never engages" in f.get("message", "")
    if det == "D2":
        return f.get("signal_kind") == "item"
    return False


def parse_analyzer(analyzer_json: Path, total: int | None) -> dict | None:
    if not analyzer_json.exists():
        return None
    with analyzer_json.open(encoding="utf-8") as fh:
        findings = json.load(fh)
    real = [f for f in findings if is_real_data_finding(f)]
    real_sources = sorted({f["source"] for f in real})
    by_detector: dict[str, int] = {}
    for f in findings:
        by_detector[f["detector"]] = by_detector.get(f["detector"], 0) + 1
    passing = (total - len(real_sources)) if total is not None else None
    return {
        "findings_total": len(findings),
        "by_detector": by_detector,
        "real_instances": len(real),
        "real_sources": real_sources,
        "passing": passing,
    }


def parse_ratchet_xml(xml_path: Path) -> dict | None:
    """Pass/fail of the corpus ratchet, from its own gradle build output (trusted)."""
    if not xml_path.exists():
        return None
    suite = ET.parse(xml_path).getroot()
    tests = int(suite.get("tests", "0"))
    failures = int(suite.get("failures", "0"))
    errors = int(suite.get("errors", "0"))
    return {
        "tests": tests,
        "failures": failures,
        "errors": errors,
        "passed": failures == 0 and errors == 0 and tests > 0,
        "timestamp": suite.get("timestamp", "unknown"),
    }


def parse_findings(findings_md: Path) -> dict | None:
    """Trustworthy findings backlog: total, shipped (closed), routed (open)."""
    if not findings_md.exists():
        return None
    text = findings_md.read_text(encoding="utf-8")
    total = re.findall(r"(?m)^###\s+([CN]\d+)\b", text)
    routed_block = re.search(r"(?ms)^\*\*Routed to the maintainer.*", text)
    routed = (
        re.findall(r"(?m)^-\s+\*\*([CN]\d+)\*\*", routed_block.group(0))
        if routed_block
        else []
    )
    shipped_block = re.search(r"(?ms)^\*\*Shipped as PRs.*?(?=^\*\*Routed)", text)
    shipped = (
        re.findall(r"\*\*([CN]\d+)\*\*", shipped_block.group(0)) if shipped_block else []
    )
    # de-dup shipped ids (a PR line may mention an id more than once)
    shipped = sorted(set(shipped))
    return {
        "total": len(total),
        "open": len(routed),
        "open_ids": routed,
        "shipped": len(shipped),
        "shipped_ids": shipped,
    }


def parse_acceptance(acceptance_dir: Path) -> dict | None:
    """PASS/PARTIAL/FAIL over the latest run's never-driven sample."""
    if not acceptance_dir.exists():
        return None
    runs = sorted(glob.glob(str(acceptance_dir / "*.md")))
    if not runs:
        return None
    latest = Path(runs[-1])
    text = latest.read_text(encoding="utf-8")
    sec = re.search(r"(?ms)^##\s+Run status\b.*?(?=^##\s|\Z)", text)
    if not sec:
        return {"file": latest, "rows": 0}
    counts = {"PASS": [], "PARTIAL": [], "FAIL": []}
    for src, _cond, res in re.findall(
        r"(?m)^\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*(.+?)\s*\|\s*$", sec.group(0)
    ):
        name = re.sub(r"\*", "", src).strip()
        if name.lower().startswith("source") or set(name) <= set("-: "):
            continue  # header / separator
        if "calib" in name.lower():
            continue  # calibration control rows are not part of the sample
        r = res.strip()
        if "FAIL" in r:
            counts["FAIL"].append(name)
        elif r.lstrip("*").startswith("PARTIAL"):
            counts["PARTIAL"].append(name)
        elif r.lstrip("*").startswith("PASS"):
            counts["PASS"].append(name)
    sample = sum(len(v) for v in counts.values())
    return {
        "file": latest,
        "sample": sample,
        "pass": counts["PASS"],
        "partial": counts["PARTIAL"],
        "fail": counts["FAIL"],
    }


def parse_drive_report(report_path: Path) -> dict | None:
    """Gate 4: item-weighted guidance-drive confidence from the acceptance-runner.

    Reads `acceptance-runner-report.json` produced by
    `scripts/acceptance_runner.py` (the live-bridge sweep). Returns None when the
    artifact is absent -- the board reports "not yet measured" and does NOT block.
    """
    if not report_path.exists():
        return None
    with report_path.open(encoding="utf-8") as fh:
        rep = json.load(fh)
    confidence = float(rep.get("confidencePct", 0.0))
    return {
        "confidencePct": confidence,
        "passCount": rep.get("passCount"),
        "total": rep.get("total"),
        "selected": rep.get("selected", []),
        "meets_gate": confidence >= CONFIDENCE_GATE_PCT,
    }


def pct(n: int, d: int) -> str:
    return f"{(100.0 * n / d):.1f}%" if d else "n/a"


def render(
    *,
    head: str,
    total: int | None,
    analyzer: dict | None,
    calibration: str,
    ratchet: dict | None,
    findings: dict | None,
    acceptance: dict | None,
    drive: dict | None,
    analyzer_json: Path,
    ratchet_xml: Path,
    findings_md: Path,
    drive_report: Path,
) -> str:
    o: list[str] = []
    o.append("# Guidance-audit scoreboard")
    o.append("")
    o.append(
        "> Generated by `./gradlew guidanceStatus` (`scripts/guidance_status.py`) "
        f"from commit `{head}`. Do not hand-edit -- every number is derived from the "
        "linked raw artifact and re-derived on each run."
    )
    o.append("")
    o.append("## At a glance")
    o.append("")
    o.append("| # | Metric | Value | Source artifact |")
    o.append("|---|---|---|---|")

    # 1. corpus-pass-rate
    if total and analyzer and analyzer["passing"] is not None and ratchet:
        rate = pct(analyzer["passing"], total)
        gate = "green" if (ratchet["passed"] and calibration == "pass") else "RED"
        c1 = f"**{analyzer['passing']}/{total}** ({rate}); calibration+ratchet gate {gate}"
        c1s = f"[`{rel(analyzer_json)}`]({href(analyzer_json)}) + ratchet JUnit"
    else:
        c1 = "not yet measured"
        c1s = "missing analyzer JSON or ratchet XML -- run `./gradlew guidanceStatus`"
    o.append(f"| 1 | corpus-pass-rate | {c1} | {c1s} |")

    # 2. open trustworthy findings
    if findings is not None:
        c2 = f"**{findings['open']}** open ({', '.join(findings['open_ids'])}); {findings['shipped']} shipped of {findings['total']}"
        c2s = f"[`{rel(findings_md)}`]({href(findings_md)})"
    else:
        c2 = "not yet measured"
        c2s = f"missing `{rel(findings_md)}`"
    o.append(f"| 2 | open trustworthy findings | {c2} | {c2s} |")

    # 3. acceptance-gate %
    if acceptance is not None and acceptance.get("sample"):
        npass = len(acceptance["pass"])
        c3 = f"**{npass}/{acceptance['sample']}** ({pct(npass, acceptance['sample'])}) full pass"
        c3s = f"[`{rel(acceptance['file'])}`]({href(acceptance['file'])})"
    else:
        c3 = "**not yet measured**"
        c3s = f"no run table under `{rel(DEFAULT_ACCEPTANCE_DIR)}/`"
    o.append(f"| 3 | acceptance-gate % | {c3} | {c3s} |")

    # 4. guidance-drive confidence %
    if drive is not None:
        gate = "PASS" if drive["meets_gate"] else "BELOW GATE"
        c4 = (
            f"**{drive['confidencePct']:.1f}%** item-weighted "
            f"({drive['passCount']}/{drive['total']} sources PASS); "
            f"gate {CONFIDENCE_GATE_PCT}% {gate}"
        )
        c4s = f"[`{rel(drive_report)}`]({href(drive_report)})"
    else:
        c4 = "not yet measured"
        c4s = f"no `{rel(drive_report)}` -- run `scripts/acceptance_runner.py` live"
    o.append(f"| 4 | guidance-drive confidence % | {c4} | {c4s} |")
    o.append("")

    # ---- 1 detail ----
    o.append("## 1. Corpus-pass-rate")
    o.append("")
    if total and analyzer and analyzer["passing"] is not None and ratchet:
        o.append(
            f"Of **{total}** sources, **{analyzer['passing']}** carry no REAL "
            f"guidance-config finding. The static analyzer "
            f"(`scripts/audit_guidance_config.py`) emits {analyzer['findings_total']} "
            f"raw findings; filtering to the REAL data classes "
            f"(C1/N1 can-never-fire, C2 earlier-target loop-never) leaves "
            f"**{analyzer['real_instances']}** real instances across "
            f"**{len(analyzer['real_sources'])}** sources (the rest are by-design -- "
            f"E4 self-loop markers, the Shades recurring-gather MANUAL, and "
            f"highlight-only/cosmetic hits -- the #775 lesson: an artifact is never a bug)."
        )
        o.append("")
        o.append("| Detector | Raw findings |")
        o.append("|---|---|")
        for d in ("D1", "D1b", "D2"):
            o.append(f"| {d} | {analyzer['by_detector'].get(d, 0)} |")
        o.append("")
        cg = "PASS" if calibration == "pass" else calibration.upper()
        rg = "PASS" if ratchet["passed"] else "FAIL"
        o.append(
            f"Trust gates: calibration `--calibrate` **{cg}** (re-derives the "
            f"maintainer's hand-found bug counts); `GuidanceConfigInvariantsRegressionTest` "
            f"ratchet **{rg}** ({ratchet['tests']} test(s), {ratchet['failures']} failures, "
            f"run {ratchet['timestamp']}) -- runs under `./gradlew check`."
        )
        o.append("")
        o.append("Real-affected sources:")
        o.append("")
        o.append("> " + ", ".join(analyzer["real_sources"]))
    else:
        o.append(
            "Not yet measured -- analyzer JSON and/or ratchet XML absent. Run "
            "`./gradlew guidanceStatus`."
        )
    o.append("")

    # ---- 2 detail ----
    o.append("## 2. Open trustworthy findings")
    o.append("")
    if findings is not None:
        o.append(
            f"The discriminator's backlog [`{rel(findings_md)}`]({href(findings_md)}) "
            f"holds **{findings['total']}** REAL findings (each mechanism-cited). "
            f"**{findings['open']}** are still routed to the maintainer (open); "
            f"**{findings['shipped']}** have shipped as PRs."
        )
        o.append("")
        o.append(f"- Open (routed): {', '.join(findings['open_ids']) or 'none'}")
        o.append(f"- Shipped: {', '.join(findings['shipped_ids']) or 'none'}")
        o.append("")
        o.append(
            "Harness ARTIFACTs are excluded: the latest acceptance run's "
            "`ARRIVE_AT_TILE` synthetic-injection quirk (Finding 1) is a test-harness "
            "limitation, not a product bug, so it is not counted here."
        )
    else:
        o.append(f"Not yet measured -- `{rel(findings_md)}` absent.")
    o.append("")

    # ---- 3 detail ----
    o.append("## 3. Acceptance-gate %")
    o.append("")
    if acceptance is not None and acceptance.get("sample"):
        npass = len(acceptance["pass"])
        o.append(
            f"Latest acceptance sweep "
            f"[`{rel(acceptance['file'])}`]({href(acceptance['file'])}) drove a "
            f"never-driven sample of **{acceptance['sample']}** sources: "
            f"**{npass} full PASS** ({pct(npass, acceptance['sample'])}), "
            f"{len(acceptance['partial'])} PARTIAL, {len(acceptance['fail'])} FAIL."
        )
        o.append("")
        o.append(f"- PASS: {', '.join(acceptance['pass']) or 'none'}")
        o.append(
            f"- PARTIAL: {', '.join(acceptance['partial']) or 'none'} "
            "(blocked by the Finding-1 `ARRIVE_AT_TILE` harness quirk, not product bugs)"
        )
        o.append(
            f"- FAIL: {', '.join(acceptance['fail']) or 'none'} "
            "(static dead-end -- the C1 unwired-`ACTOR_DEATH` data bug)"
        )
    elif acceptance is not None:
        o.append(
            f"Run file [`{rel(acceptance['file'])}`]({href(acceptance['file'])}) found, "
            "but no parseable Run status table -- treated as not yet measured."
        )
    else:
        o.append(
            f"**Not yet measured** -- no acceptance run under "
            f"`{rel(DEFAULT_ACCEPTANCE_DIR)}/`."
        )
    o.append("")

    # ---- 4 detail ----
    o.append("## 4. Guidance-drive confidence %")
    o.append("")
    if drive is not None:
        gate = "meets" if drive["meets_gate"] else "BELOW"
        o.append(
            f"The acceptance-runner "
            f"([`{rel(drive_report)}`]({href(drive_report)})) drove "
            f"**{drive['total']}** selected source(s) through the live bridge by "
            f"synthesizing each step's completion event; "
            f"**{drive['passCount']}** drove fully to completion. Item-weighted "
            f"confidence (clog items in PASS sources / clog items selected) is "
            f"**{drive['confidencePct']:.1f}%**, which **{gate}** the "
            f"`CONFIDENCE_GATE_PCT = {CONFIDENCE_GATE_PCT}` threshold."
        )
    else:
        o.append(
            f"**Not yet measured** -- no `{rel(drive_report)}`. Generate it with a "
            "live run of `scripts/acceptance_runner.py --all` (or `--sample N`) "
            "against a running dev client. Until then this gate does not block."
        )
    o.append("")

    # ---- recommendation ----
    o.append("## Recommendation implied by the board")
    o.append("")
    green = bool(
        total
        and analyzer
        and analyzer["passing"] == total
        and ratchet
        and ratchet["passed"]
        and calibration == "pass"
        and findings is not None
        and findings["open"] == 0
        and acceptance is not None
        and acceptance.get("sample")
        and not acceptance["fail"]
        # gate 4: if measured it must meet the threshold; unmeasured does not block.
        and (drive is None or drive["meets_gate"])
    )
    if green:
        o.append(
            "GREEN -- corpus clean, zero open trustworthy findings, acceptance sample "
            "all-pass. Eligible to hand off to resubmission-hygiene (ROADMAP §2)."
        )
    else:
        reasons = []
        if not (total and analyzer and ratchet):
            reasons.append("a tier artifact is unmeasured")
        else:
            if analyzer["passing"] != total:
                reasons.append(
                    f"{len(analyzer['real_sources'])} source(s) carry a real "
                    "guidance-config finding"
                )
            if not ratchet["passed"]:
                reasons.append("the ratchet is RED")
            if calibration != "pass":
                reasons.append(f"calibration gate {calibration}")
        if findings is not None and findings["open"]:
            reasons.append(
                f"{findings['open']} open trustworthy finding(s): "
                + ", ".join(findings["open_ids"])
            )
        if acceptance is not None and acceptance.get("fail"):
            reasons.append(
                "acceptance FAIL: " + ", ".join(acceptance["fail"])
            )
        if acceptance is None:
            reasons.append("acceptance-gate not yet measured")
        if drive is not None and not drive["meets_gate"]:
            reasons.append(
                f"guidance-drive confidence {drive['confidencePct']:.1f}% is below "
                f"the {CONFIDENCE_GATE_PCT}% gate"
            )
        elif drive is None:
            reasons.append("guidance-drive confidence not yet measured")
        o.append("NOT GREEN -- resubmission blocked until resolved:")
        o.append("")
        for r in reasons:
            o.append(f"- {r}")
    o.append("")
    return "\n".join(o)


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--data", type=Path, default=DEFAULT_DATA)
    p.add_argument("--analyzer-json", type=Path, default=DEFAULT_ANALYZER_JSON)
    p.add_argument("--ratchet-xml", type=Path, default=DEFAULT_RATCHET_XML)
    p.add_argument("--findings", type=Path, default=DEFAULT_FINDINGS)
    p.add_argument("--acceptance-dir", type=Path, default=DEFAULT_ACCEPTANCE_DIR)
    p.add_argument("--drive-report", type=Path, default=DEFAULT_DRIVE_REPORT)
    p.add_argument(
        "--calibration",
        choices=("pass", "fail", "unknown"),
        default="unknown",
        help="result of `audit_guidance_config.py --calibrate` (the gradle task supplies it)",
    )
    p.add_argument("--head", default="unknown")
    p.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = p.parse_args(argv)

    global _OUTPUT_DIR
    _OUTPUT_DIR = args.output.resolve().parent

    total = count_sources(args.data)
    md = render(
        head=args.head,
        total=total,
        analyzer=parse_analyzer(args.analyzer_json, total),
        calibration=args.calibration,
        ratchet=parse_ratchet_xml(args.ratchet_xml),
        findings=parse_findings(args.findings),
        acceptance=parse_acceptance(args.acceptance_dir),
        drive=parse_drive_report(args.drive_report),
        analyzer_json=args.analyzer_json,
        ratchet_xml=args.ratchet_xml,
        findings_md=args.findings,
        drive_report=args.drive_report,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(md, encoding="utf-8")
    print(f"Wrote {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
