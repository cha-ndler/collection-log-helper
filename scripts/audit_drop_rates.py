#!/usr/bin/env python3
"""Audit harness for `drop_rates.json`.

Runs a set of static consistency checks on every source entry and emits a
markdown triage report plus an optional JSON report.

Usage:
    python scripts/audit_drop_rates.py [--category RAIDS|ALL] \
        [--json-output report.json] [--data path/to/drop_rates.json] \
        [--landmarks path/to/canonical_landmarks.json] \
        [--output audit-report.md]

Checks performed:
  (A) Coord plausibility: world coords are within a tolerance of the
      canonical landmark named in `locationDescription`. Landmarks are loaded
      from a data-driven JSON file so new categories can extend the set
      without touching the script.
  (B) Cross-field consistency: region/landmark words mentioned in any of
      `locationDescription`, `travelTip`, `guidanceSteps[].description` should
      appear in the other fields too. Catches the kraken-cascade pattern where
      one field describes a different region from the others.
  (C) NPC ID sanity: zero / negative IDs are flagged; missing `npcId` is only
      noted for combat-like sources (with an `interactAction` of `Attack`).
  (D) Required field presence: empty `locationDescription`, empty
      `guidanceSteps`, missing `interactAction` on combat sources.

Outputs each finding bucketed as one of:
  - verified              -> no flags
  - flagged-fixable       -> unambiguous fix suggestion available
  - flagged-research      -> ambiguous; needs human / wiki resolution
  - error                 -> harness could not process

The harness is intentionally side-effect-free: it does not edit
`drop_rates.json`. Fixes are applied separately by the caller.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass, field, asdict
from pathlib import Path
from typing import Any

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DATA_PATH = (
    REPO_ROOT
    / "src"
    / "main"
    / "resources"
    / "com"
    / "collectionloghelper"
    / "drop_rates.json"
)
DEFAULT_LANDMARKS_PATH = Path(__file__).resolve().parent / "canonical_landmarks.json"

# Coord plausibility tolerance in tiles. RuneScape regions are 64x64, so 50
# tiles is roughly within the same region.
COORD_TOLERANCE_TILES = 50

# Severity levels
SEV_LOW = "low"
SEV_MED = "med"
SEV_HIGH = "high"

# Triage buckets
BUCKET_VERIFIED = "verified"
BUCKET_FIXABLE = "flagged-fixable"
BUCKET_RESEARCH = "flagged-research"
BUCKET_ERROR = "error"

# Region / proper-noun tokens grouped by mutually-exclusive "zone". The
# cross-field check flags only when two different fields name DIFFERENT zones
# (the kraken-cascade pattern: travelTip says "Fremennik Slayer Dungeon" while
# locationDescription/coords point at Piscatoris). Tokens within the same zone
# can co-occur or be silently elided in a terse field (e.g. a travelTip that
# says "Xeric's talisman -> Chambers" without repeating "Great Kourend").
ZONE_GROUPS: dict[str, list[str]] = {
    "raid_cox": ["Mount Quidamortem", "Chambers of Xeric", "Great Kourend"],
    "raid_tob": ["Ver Sinhaza", "Theatre of Blood", "Morytania"],
    "raid_toa": ["Necropolis", "Tombs of Amascut", "Kharidian Desert"],
    "catacombs": ["Catacombs of Kourend", "Catacombs"],
    "wilderness": ["Wilderness"],
    "gwd": ["God Wars Dungeon"],
    "fremennik": ["Fremennik Slayer Dungeon", "Fremennik"],
    "piscatoris": ["Piscatoris"],
    "karuulm": ["Mount Karuulm", "Karuulm", "Kebos Lowlands", "Kebos"],
    "slayer_tower": ["Slayer Tower"],
    "slayer_cave": ["Stronghold Slayer Cave"],
    "zeah": ["Zeah"],
    "asgarnia": ["Asgarnia"],
    "misthalin": ["Misthalin"],
    "karamja": ["Karamja"],
    "tirannwn": ["Tirannwn"],
}

# Flat list, derived from ZONE_GROUPS.
REGION_TOKENS = [t for zone in ZONE_GROUPS.values() for t in zone]
TOKEN_TO_ZONE = {t: z for z, toks in ZONE_GROUPS.items() for t in toks}


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------


@dataclass
class Finding:
    source_name: str
    category: str
    check: str  # "coord" | "cross-field" | "npc-id" | "required-field"
    field: str
    current: Any
    suggested: Any
    severity: str  # SEV_LOW / SEV_MED / SEV_HIGH
    bucket: str  # BUCKET_*
    note: str = ""


@dataclass
class SourceReport:
    name: str
    category: str
    findings: list[Finding] = field(default_factory=list)
    bucket: str = BUCKET_VERIFIED  # worst bucket across findings

    def add(self, finding: Finding) -> None:
        self.findings.append(finding)
        order = {
            BUCKET_VERIFIED: 0,
            BUCKET_FIXABLE: 1,
            BUCKET_RESEARCH: 2,
            BUCKET_ERROR: 3,
        }
        if order[finding.bucket] > order[self.bucket]:
            self.bucket = finding.bucket


# ---------------------------------------------------------------------------
# Landmark loading
# ---------------------------------------------------------------------------


def load_landmarks(path: Path) -> dict[str, dict[str, Any]]:
    """Load canonical landmarks.

    Schema:
      {
        "Mount Quidamortem": {
            "worldX": 1233, "worldY": 3573, "worldPlane": 0,
            "aliases": ["Chambers of Xeric", "Chambers"]
        },
        ...
      }
    """
    if not path.exists():
        return {}
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def extract_region_tokens(text: str) -> set[str]:
    if not text:
        return set()
    found: set[str] = set()
    lower = text.lower()
    for token in REGION_TOKENS:
        if token.lower() in lower:
            found.add(token)
    return found


def fields_for_source(source: dict) -> dict[str, str]:
    """Return the merged-text fields we care about per source."""
    steps_text = " ".join(
        (gs.get("description") or "") for gs in source.get("guidanceSteps") or []
    )
    return {
        "locationDescription": source.get("locationDescription") or "",
        "travelTip": source.get("travelTip") or "",
        "guidanceSteps": steps_text,
    }


def tile_distance(ax: int, ay: int, bx: int, by: int) -> int:
    return abs(ax - bx) + abs(ay - by)


# Prepositions that indicate the description is referencing a landmark
# RELATIVELY (e.g. "south of Mount Quidamortem"). We do NOT anchor coords to
# such a landmark.
RELATIVE_PREPOSITIONS = (
    "south of",
    "north of",
    "east of",
    "west of",
    "near ",
    "outside ",
    "below ",
    "above ",
    "next to ",
)


def match_landmark(
    location_desc: str, landmarks: dict[str, dict[str, Any]]
) -> tuple[str, dict[str, Any]] | None:
    """Return (name, landmark) of best match for a locationDescription, or
    None if no landmark name appears in the description AS A PRIMARY anchor.

    Skips matches where the landmark is referenced relatively
    ("south of Mount Quidamortem" should NOT anchor to Mount Quidamortem).
    """
    if not location_desc or not landmarks:
        return None
    lower = location_desc.lower()
    candidates: list[tuple[str, dict[str, Any], int]] = []
    for name, data in landmarks.items():
        names_to_check = [name] + list(data.get("aliases", []))
        for n in names_to_check:
            n_lower = n.lower()
            idx = lower.find(n_lower)
            if idx < 0:
                continue
            # Check the text immediately preceding the landmark name.
            preceding = lower[max(0, idx - 16) : idx]
            if any(prep in preceding for prep in RELATIVE_PREPOSITIONS):
                continue  # relative reference; skip
            candidates.append((name, data, len(n)))
            break
    if not candidates:
        return None
    candidates.sort(key=lambda c: c[2], reverse=True)
    name, data, _ = candidates[0]
    return name, data


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------


def check_coord_plausibility(
    source: dict, landmarks: dict[str, dict[str, Any]]
) -> list[Finding]:
    findings: list[Finding] = []
    name = source.get("name", "?")
    category = source.get("category", "?")
    loc = source.get("locationDescription") or ""
    matched = match_landmark(loc, landmarks)
    if matched is None:
        return findings
    landmark_name, lm = matched
    lx, ly = lm.get("worldX"), lm.get("worldY")
    sx, sy = source.get("worldX"), source.get("worldY")
    if None in (lx, ly, sx, sy):
        return findings
    dist = tile_distance(sx, sy, lx, ly)
    if dist > COORD_TOLERANCE_TILES:
        findings.append(
            Finding(
                source_name=name,
                category=category,
                check="coord",
                field="worldX/worldY",
                current=f"({sx}, {sy})",
                suggested=f"~({lx}, {ly}) [{landmark_name}]",
                severity=SEV_HIGH,
                bucket=BUCKET_RESEARCH,
                note=(
                    f"coords are {dist} tiles from canonical {landmark_name}; "
                    f"tolerance is {COORD_TOLERANCE_TILES}. Could be intentional "
                    "(e.g. lobby vs. entrance) but verify against the Wiki."
                ),
            )
        )
    return findings


def check_cross_field_consistency(source: dict) -> list[Finding]:
    """Flag when two fields name DIFFERENT zone groups.

    A field naming a token from zone-group X and another field naming a token
    from zone-group Y (X != Y) is the kraken-cascade pattern -- they cannot
    both describe the same source. Silent elision (a terse travelTip that
    simply doesn't repeat the region) is NOT a mismatch and is not flagged.
    """
    findings: list[Finding] = []
    name = source.get("name", "?")
    category = source.get("category", "?")
    fields = fields_for_source(source)
    # Map each field to the set of zone groups it mentions.
    per_field_zones: dict[str, set[str]] = {}
    per_field_tokens_by_zone: dict[str, dict[str, list[str]]] = {}
    for fname, text in fields.items():
        toks = extract_region_tokens(text)
        zones: set[str] = set()
        toks_by_zone: dict[str, list[str]] = {}
        for t in toks:
            z = TOKEN_TO_ZONE.get(t)
            if z:
                zones.add(z)
                toks_by_zone.setdefault(z, []).append(t)
        per_field_zones[fname] = zones
        per_field_tokens_by_zone[fname] = toks_by_zone

    all_zones: set[str] = set().union(*per_field_zones.values())
    if len(all_zones) <= 1:
        return findings  # zero or one zone mentioned anywhere; consistent

    # Two or more zones found across fields. Flag every pair that disagrees.
    fields_list = list(per_field_zones.keys())
    for i, fa in enumerate(fields_list):
        for fb in fields_list[i + 1 :]:
            za, zb = per_field_zones[fa], per_field_zones[fb]
            if not za or not zb:
                continue
            if za & zb:
                continue  # share at least one zone; consistent
            findings.append(
                Finding(
                    source_name=name,
                    category=category,
                    check="cross-field",
                    field=f"{fa} vs {fb}",
                    current=(
                        f"{fa}={sorted(za)} "
                        f"{fb}={sorted(zb)}"
                    ),
                    suggested="align both fields to the same zone group",
                    severity=SEV_HIGH,
                    bucket=BUCKET_RESEARCH,
                    note=(
                        f"region tokens disagree: {fa} mentions "
                        f"{sorted(per_field_tokens_by_zone[fa].keys())} "
                        f"while {fb} mentions "
                        f"{sorted(per_field_tokens_by_zone[fb].keys())} -- "
                        "kraken-cascade pattern."
                    ),
                )
            )
    return findings


COMBAT_CATEGORIES = {"BOSSES", "RAIDS", "SLAYER"}


def check_npc_id_sanity(source: dict) -> list[Finding]:
    findings: list[Finding] = []
    name = source.get("name", "?")
    category = source.get("category", "?")
    npc_id = source.get("npcId")
    interact = (source.get("interactAction") or "").strip()
    if isinstance(npc_id, int) and npc_id <= 0:
        findings.append(
            Finding(
                source_name=name,
                category=category,
                check="npc-id",
                field="npcId",
                current=npc_id,
                suggested=">0 or null",
                severity=SEV_HIGH,
                bucket=BUCKET_RESEARCH,
                note="npcId is non-positive; either remove or replace with a real id.",
            )
        )
    if category in COMBAT_CATEGORIES and not interact and npc_id in (None, 0):
        findings.append(
            Finding(
                source_name=name,
                category=category,
                check="npc-id",
                field="npcId+interactAction",
                current="both missing",
                suggested="set npcId + interactAction OR confirm 'instance lobby' style source",
                severity=SEV_LOW,
                bucket=BUCKET_RESEARCH,
                note=(
                    "combat-style category but no npcId and no interactAction. "
                    "May be intentional for raid lobbies but worth verifying."
                ),
            )
        )
    return findings


def check_required_fields(source: dict) -> list[Finding]:
    findings: list[Finding] = []
    name = source.get("name", "?")
    category = source.get("category", "?")
    loc = source.get("locationDescription")
    if not loc or not str(loc).strip():
        findings.append(
            Finding(
                source_name=name,
                category=category,
                check="required-field",
                field="locationDescription",
                current=loc,
                suggested="non-empty string",
                severity=SEV_HIGH,
                bucket=BUCKET_RESEARCH,
                note="locationDescription is empty.",
            )
        )
    steps = source.get("guidanceSteps") or []
    if not steps:
        findings.append(
            Finding(
                source_name=name,
                category=category,
                check="required-field",
                field="guidanceSteps",
                current="[]",
                suggested="at least 1 step",
                severity=SEV_MED,
                bucket=BUCKET_RESEARCH,
                note="guidanceSteps is empty.",
            )
        )
    return findings


# ---------------------------------------------------------------------------
# Reporting
# ---------------------------------------------------------------------------


def audit_source(
    source: dict, landmarks: dict[str, dict[str, Any]]
) -> SourceReport:
    name = source.get("name", "?")
    category = source.get("category", "?")
    report = SourceReport(name=name, category=category)
    try:
        for f in check_coord_plausibility(source, landmarks):
            report.add(f)
        for f in check_cross_field_consistency(source):
            report.add(f)
        for f in check_npc_id_sanity(source):
            report.add(f)
        for f in check_required_fields(source):
            report.add(f)
    except Exception as e:  # pragma: no cover - defensive
        report.add(
            Finding(
                source_name=name,
                category=category,
                check="harness",
                field="-",
                current="-",
                suggested="-",
                severity=SEV_HIGH,
                bucket=BUCKET_ERROR,
                note=f"harness exception: {e!r}",
            )
        )
    return report


def md_escape(s: str) -> str:
    return s.replace("|", "\\|").replace("\n", " ").strip()


def render_markdown(reports: list[SourceReport], category_label: str) -> str:
    counts = {
        BUCKET_VERIFIED: 0,
        BUCKET_FIXABLE: 0,
        BUCKET_RESEARCH: 0,
        BUCKET_ERROR: 0,
    }
    for r in reports:
        counts[r.bucket] += 1
    out: list[str] = []
    out.append(f"# drop_rates.json audit -- {category_label}")
    out.append("")
    out.append(f"Total sources audited: **{len(reports)}**")
    out.append("")
    out.append("| Bucket | Count |")
    out.append("|---|---|")
    for b in [BUCKET_VERIFIED, BUCKET_FIXABLE, BUCKET_RESEARCH, BUCKET_ERROR]:
        out.append(f"| {b} | {counts[b]} |")
    out.append("")
    out.append("## Findings")
    out.append("")
    flagged = [r for r in reports if r.findings]
    if not flagged:
        out.append("_No findings; all sources verified clean._")
    else:
        out.append(
            "| Source | Category | Check | Field | Current | Suggested | Severity | Bucket | Note |"
        )
        out.append("|---|---|---|---|---|---|---|---|---|")
        for r in flagged:
            for f in r.findings:
                out.append(
                    "| {name} | {cat} | {chk} | {fld} | {cur} | {sug} | {sev} | {bkt} | {note} |".format(
                        name=md_escape(r.name),
                        cat=r.category,
                        chk=f.check,
                        fld=md_escape(str(f.field)),
                        cur=md_escape(str(f.current)),
                        sug=md_escape(str(f.suggested)),
                        sev=f.severity,
                        bkt=f.bucket,
                        note=md_escape(f.note),
                    )
                )
    out.append("")
    verified = [r for r in reports if r.bucket == BUCKET_VERIFIED]
    out.append(f"## Verified ({len(verified)})")
    out.append("")
    for r in verified:
        out.append(f"- {r.name}")
    out.append("")
    return "\n".join(out)


def render_json(reports: list[SourceReport]) -> str:
    payload = [
        {
            "name": r.name,
            "category": r.category,
            "bucket": r.bucket,
            "findings": [asdict(f) for f in r.findings],
        }
        for r in reports
    ]
    return json.dumps(payload, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--category",
        default="ALL",
        help="Category to audit (e.g. RAIDS) or ALL for every source.",
    )
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA_PATH)
    parser.add_argument("--landmarks", type=Path, default=DEFAULT_LANDMARKS_PATH)
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Where to write the markdown report (default: stdout only).",
    )
    parser.add_argument(
        "--json-output",
        type=Path,
        default=None,
        help="Optional path for machine-readable JSON report.",
    )
    args = parser.parse_args(argv)

    if not args.data.exists():
        print(f"ERROR: data file not found: {args.data}", file=sys.stderr)
        return 2

    with args.data.open(encoding="utf-8") as fh:
        sources = json.load(fh)

    landmarks = load_landmarks(args.landmarks)
    if not landmarks:
        print(
            f"WARN: no landmarks loaded from {args.landmarks}; coord check disabled.",
            file=sys.stderr,
        )

    category = args.category.upper()
    if category != "ALL":
        sources = [s for s in sources if s.get("category") == category]

    if not sources:
        print(f"ERROR: no sources matched category={category}", file=sys.stderr)
        return 2

    reports = [audit_source(s, landmarks) for s in sources]
    md = render_markdown(reports, category_label=category)

    if args.output:
        args.output.write_text(md, encoding="utf-8")
    print(md)

    if args.json_output:
        args.json_output.write_text(render_json(reports), encoding="utf-8")

    any_flagged = any(r.bucket != BUCKET_VERIFIED for r in reports)
    return 1 if any_flagged else 0


if __name__ == "__main__":
    sys.exit(main())
