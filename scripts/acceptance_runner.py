#!/usr/bin/env python3
"""Guidance acceptance-runner -- a resubmission GATE.

Measures "what % of collection-log items have guidance that actually drives to
completion" by, for each source, synthesizing the events that each guidance
step's `completionCondition` expects, injecting them into the LIVE dev client
through the file bridge, and reading back whether the sequence advanced and
completed.

Two halves, cleanly separated so the analysis is unit-testable offline:

  * `synthesize_events(step, clog_item_ids)` -- PURE. Maps one guidance step to
    the synthetic event(s) that would complete it. No I/O. This is the part the
    self-tests pin.
  * `BridgeClient` -- the LIVE driver. Talks the `~/.runelite/rl-dev-bridge/`
    file protocol (atomic command.json write + response.json poll). NEVER
    touched by the self-tests; only `run_source` / `--source|--sample|--all`
    against a running client exercises it.

Aggregate confidence is ITEM-WEIGHTED: confidencePct = 100 * (clog items in PASS
sources) / (clog items in selected sources). A source with 30 clog items that
drives to completion counts 30x a 1-item source.

stdlib only. Build + offline self-test must never call the live bridge.
"""

from __future__ import annotations

import argparse
import json
import os
import random
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DATA = (
    REPO_ROOT / "src" / "main" / "resources" / "com" / "collectionloghelper" / "drop_rates.json"
)
DEFAULT_REPORT = REPO_ROOT / "docs" / "guidance-audit" / "acceptance-runner-report.json"


# --------------------------------------------------------------------------- #
# Atomic write (shared by command.json + the incremental report)              #
# --------------------------------------------------------------------------- #
def atomic_write_text(path: Path, text: str, *, encoding: str = "utf-8") -> None:
    """Write `text` to `path` atomically (tmp + os.replace).

    On Windows the destination can be held open by another process at the
    instant we rename over it, which raises PermissionError (WinError 5). Retry
    the replace briefly, then fall back to a direct write. Reused for both the
    dev-bridge command.json and the incremental acceptance report so the
    interrupt-safe-write logic lives in exactly one place.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_name(path.name + ".tmp")
    tmp.write_text(text, encoding=encoding)
    for _ in range(40):
        try:
            os.replace(tmp, path)
            return
        except PermissionError:
            time.sleep(0.05)
    path.write_text(text, encoding=encoding)
    try:
        tmp.unlink()
    except OSError:
        pass


# --------------------------------------------------------------------------- #
# PURE: completionCondition -> synthetic events                               #
# --------------------------------------------------------------------------- #
def _clamp(v: int) -> int:
    """World coords are never negative; clamp the move-away offset at the edge."""
    return v if v >= 0 else 0


# ARRIVE_AT_TILE move-away distance. The engine only registers an arrival after
# a genuine position change away-then-back; +40 proved too small to clear some
# completionDistance radii (Catacombs needed ~64 tiles in a manual drive), so we
# move a full +128 away before arriving.
_MOVE_AWAY_OFFSET = 128


def _other_plane(wp: int) -> int:
    """A DIFFERENT plane than `wp`, used to force a PLAYER_ON_PLANE transition.

    PLAYER_ON_PLANE completes when playerLocation.getPlane() == worldPlane
    (CompletionChecker.isPlayerOnPlaneSatisfying). Completion is evaluated on a
    later game-tick, so a single same-plane inject can no-op if the engine
    already thinks the player is on that plane. Injecting a move at a different
    plane first guarantees the subsequent same-plane inject is a real change.
    """
    return 1 if wp == 0 else 0


def synthesize_events(step: dict, clog_item_ids: list) -> list:
    """Map one guidance step to the synthetic event(s) that complete it.

    PURE: no I/O, deterministic. The shape of each event mirrors the dev-bridge
    harness `event.inject` payload (see DevBridgeHarnessPlugin.injectEvent); the
    BridgeClient translates the small naming gaps (chat `text` -> harness
    `message`) at the wire.

    `clog_item_ids` is the source's collection-log item IDs, used only as the
    fallback obtain-signal for ITEM_OBTAINED and for force-completion of
    MANUAL/INVENTORY terminal steps by the caller (not here).
    """
    cond = step.get("completionCondition")

    if cond == "ARRIVE_AT_TILE":
        wx = step.get("worldX", 0)
        wy = step.get("worldY", 0)
        wp = step.get("worldPlane", 0)
        # Move AWAY then ARRIVE -- a single arrive is unreliable post-login
        # (FINDING-1). Clamp the away-tile to never go negative. +128 (was +40):
        # a manual Catacombs drive needed ~64 tiles before the arrival registered.
        return [
            {"type": "playerMoved", "x": _clamp(wx + _MOVE_AWAY_OFFSET),
             "y": _clamp(wy + _MOVE_AWAY_OFFSET), "plane": wp},
            {"type": "playerMoved", "x": _clamp(wx), "y": _clamp(wy), "plane": wp},
        ]

    if cond == "ARRIVE_AT_ZONE":
        zone = step.get("completionZone") or [0, 0, 0, 0, 0]
        x0, y0, x2, y2, zplane = zone[0], zone[1], zone[2], zone[3], zone[4]
        return [
            {
                "type": "playerMoved",
                "x": (x0 + x2) // 2,
                "y": (y0 + y2) // 2,
                "plane": zplane,
            }
        ]

    if cond == "PLAYER_ON_PLANE":
        wx = step.get("worldX", 0)
        wy = step.get("worldY", 0)
        wp = step.get("worldPlane", 0)
        # Emit a plane TRANSITION: inject at a different plane first, then at the
        # target plane, so the engine sees an actual plane change (mirrors the
        # move-away-then-arrive pattern for tiles). A single same-plane inject can
        # no-op when the engine already believes the player is on that plane.
        return [
            {"type": "playerMoved", "x": _clamp(wx), "y": _clamp(wy),
             "plane": _other_plane(wp)},
            {"type": "playerMoved", "x": _clamp(wx), "y": _clamp(wy), "plane": wp},
        ]

    if cond == "NPC_TALKED_TO":
        return [{"type": "npcInteracted", "npcId": step.get("completionNpcId")}]

    if cond == "ACTOR_DEATH":
        npc_id = step.get("completionNpcId")
        if npc_id is None:
            ids = step.get("completionNpcIds") or []
            npc_id = ids[0] if ids else None
        return [{"type": "npcDeath", "npcId": npc_id}]

    if cond == "CHAT_MESSAGE_RECEIVED":
        return [{"type": "chatMessage", "text": step.get("completionChatPattern")}]

    if cond == "VARBIT_AT_LEAST":
        return [
            {
                "type": "varbitChanged",
                "varbitId": step.get("completionVarbitId"),
                "value": step.get("completionVarbitValue"),
            }
        ]

    if cond == "ITEM_OBTAINED":
        item_id = step.get("completionItemId")
        if item_id is None and clog_item_ids:
            item_id = clog_item_ids[0]
        return [{"type": "itemObtained", "itemId": item_id}]

    # MANUAL / INVENTORY_HAS_ITEM / INVENTORY_NOT_HAS_ITEM: no synthetic driver.
    return []


def clog_item_ids(source: dict) -> list:
    """The source's collection-log item IDs (the `items[].itemId` list)."""
    out = []
    for it in source.get("items") or []:
        iid = it.get("itemId")
        if iid is not None:
            out.append(iid)
    return out


# --------------------------------------------------------------------------- #
# LIVE: dev-bridge file-protocol client                                       #
# --------------------------------------------------------------------------- #
def _bridge_dir() -> Path:
    override = os.environ.get("RL_DEV_BRIDGE_DIR")
    if override:
        return Path(override)
    return Path.home() / ".runelite" / "rl-dev-bridge"


class BridgeClient:
    """File-protocol client for the in-client DevBridgeHarnessPlugin.

    Wire format (matches DevBridgeHarnessPlugin exactly):
      command.json  = {"id":<int>,"action":<str>,"params":{...},"ts":<int ms>}
      response.json = {"id":<int>,"ok":<bool>,"result":{...},"error":<str>,
                       "harnessVersion":<str>}
    Atomic command write (tmp + os.replace); poll response.json for matching id.

    Action/param catalog used here (harness `dispatch` / `injectEvent`):
      ping              -> {}
      guidance.activate -> {"source": <name>}
      event.inject      -> {"type": ..., <payload>}   (see _translate_event)
      state.read        -> {}
    """

    def __init__(self, bridge_dir: Path | None = None, timeout_s: float = 4.0,
                 poll_s: float = 0.05):
        self.dir = bridge_dir or _bridge_dir()
        self.command_file = self.dir / "command.json"
        self.response_file = self.dir / "response.json"
        self.timeout_s = timeout_s
        self.poll_s = poll_s
        self._id = int(time.time() * 1000)

    def _next_id(self) -> int:
        self._id += 1
        return self._id

    def _call(self, action: str, params: dict | None = None) -> dict:
        self.dir.mkdir(parents=True, exist_ok=True)
        cmd_id = self._next_id()
        payload = {
            "id": cmd_id,
            "action": action,
            "params": params or {},
            "ts": int(time.time() * 1000),
        }
        # Atomic command write (shared helper handles the Windows WinError-5
        # lock retry + direct-write fallback).
        atomic_write_text(self.command_file, json.dumps(payload))

        deadline = time.time() + self.timeout_s
        while time.time() < deadline:
            try:
                resp = json.loads(self.response_file.read_text(encoding="utf-8"))
            except (OSError, ValueError):
                resp = None
            if resp and resp.get("id") == cmd_id:
                if not resp.get("ok"):
                    raise RuntimeError(
                        f"bridge {action} failed: {resp.get('error')}"
                    )
                return resp.get("result") or {}
            time.sleep(self.poll_s)
        raise TimeoutError(f"bridge {action} timed out after {self.timeout_s}s")

    @staticmethod
    def _translate_event(event: dict) -> dict:
        """Map a synthesize_events() event to the harness inject payload.

        The harness `chatMessage` case reads `message` (not `text`); every other
        field name already matches. We pass the type plus payload through.
        """
        out = dict(event)
        if out.get("type") == "chatMessage" and "text" in out:
            out["message"] = out.pop("text")
        return out

    def ping(self) -> dict:
        return self._call("ping")

    def activate(self, source: str) -> dict:
        return self._call("guidance.activate", {"source": source})

    def inject(self, event: dict) -> dict:
        return self._call("event.inject", self._translate_event(event))

    def read_state(self) -> dict:
        return self._call("state.read")


# --------------------------------------------------------------------------- #
# run_source: drive one source through its steps                              #
# --------------------------------------------------------------------------- #
def _is_complete(state: dict, source_name: str) -> bool:
    if not state.get("guidanceActive"):
        return True
    if state.get("activeSource") != source_name:
        return True  # auto-advanced to another source / cleared
    cur = state.get("currentStepIndex")
    total = state.get("totalSteps")
    return cur is not None and total is not None and total > 0 and cur >= total
    # note: currentStepIndex == totalSteps means we ran off the end


# step conditions with no synthetic driver -> may need force-complete
_NO_DRIVER = {"MANUAL", "INVENTORY_HAS_ITEM", "INVENTORY_NOT_HAS_ITEM"}

# Completion checks run on a later game-tick (CompletionChecker is driven from
# GameTick / PlayerChanged), so a single state.read right after an inject can be
# one tick too early. Re-read a few times before concluding a step is stuck.
# FAST PATH: _read_until returns the instant the step advances/completes on the
# first read; the multi-read budget is ONLY spent on a step that has not yet
# moved. Because almost every step advances on read #1, the typical full pass
# never pays the stuck budget -- so it is tuned modestly (3 attempts x 120ms,
# down from 4 x 150ms) to bound the cost of the rare genuinely-deferred step
# without weakening the tick-deferral coverage the regression tests pin.
_SETTLE_READS = 3
_SETTLE_DELAY_S = 0.12


def _read_until(client: BridgeClient, name: str, prev_index: int) -> dict:
    """Re-read state until the sequence completes or the step index advances.

    FAST PATH: the first read is immediate (no pre-sleep) and, if the engine
    already reports complete or currentStepIndex moved past `prev_index`, we
    return WITHOUT consuming any of the stuck budget or sleeping. Only a step
    that has NOT advanced on read #1 spends the remaining `_SETTLE_READS - 1`
    re-reads (with `_SETTLE_DELAY_S` between them), covering tick-deferred
    completion (ACTOR_DEATH / PLAYER_ON_PLANE / ARRIVE_AT_TILE that land on the
    next GameTick). Otherwise returns the last read.
    """
    state = client.read_state()
    for _ in range(_SETTLE_READS - 1):
        if _is_complete(state, name):
            return state
        if state.get("currentStepIndex", prev_index) > prev_index:
            return state
        time.sleep(_SETTLE_DELAY_S)
        state = client.read_state()
    return state


def run_source(source: dict, client: BridgeClient) -> dict:
    """Drive a single source end to end through the live bridge.

    ping -> activate -> read (confirm step 0). For each step, inject its
    synthesized events, read, and check currentStepIndex advanced. If a
    no-synthetic-driver step (MANUAL/INVENTORY) does not advance, inject an
    itemObtained on the source's first clog item to force-complete (the engine's
    onItemObtained completes the sequence -- validated live on Catacombs).

    Classification:
      PASS    -- the sequence completed
      PARTIAL -- advanced >=1 step but never completed, even after force
      FAIL    -- stuck at step 0 (never advanced at all)
      SKIP    -- only INVENTORY-gated and no clog item to force-complete with
    """
    name = source.get("name")
    steps = source.get("guidanceSteps") or []
    items = clog_item_ids(source)
    result = {
        "source": name,
        "status": None,
        "lastStep": None,
        "reason": None,
        "itemCount": len(items),
    }

    if not steps:
        result["status"] = "SKIP"
        result["reason"] = "no guidance steps"
        return result

    client.ping()
    client.activate(name)
    state = client.read_state()

    if not state.get("guidanceActive") or state.get("activeSource") != name:
        # activated but engine reports another/no source -- treat as completed
        # only if it is genuinely off; otherwise it never engaged.
        if _is_complete(state, name):
            result["status"] = "PASS"
            result["lastStep"] = 0
            result["reason"] = "completed on activate (single-step / auto)"
            return result
        result["status"] = "FAIL"
        result["lastStep"] = 0
        result["reason"] = f"guidance did not engage for {name!r}"
        return result

    advanced = False
    only_inventory_gated = True

    for idx, step in enumerate(steps):
        cond = step.get("completionCondition")
        if cond not in _NO_DRIVER:
            only_inventory_gated = False

        prev_index = state.get("currentStepIndex", 0)
        events = synthesize_events(step, items)
        for ev in events:
            client.inject(ev)
        # Completion is often one tick late -- re-read a few times before
        # concluding the step is stuck (covers ACTOR_DEATH/PLAYER_ON_PLANE/
        # ARRIVE_AT_TILE that complete on the next GameTick).
        state = _read_until(client, name, prev_index)

        if _is_complete(state, name):
            advanced = True
            result["status"] = "PASS"
            result["lastStep"] = idx
            result["reason"] = f"completed after step {idx} ({cond})"
            return result

        cur_index = state.get("currentStepIndex", prev_index)
        if cur_index > prev_index:
            advanced = True
            continue

        # Did not advance. For a no-driver step (MANUAL / INVENTORY_*) at ANY
        # position -- not just the terminal one -- force-complete via the
        # source's first clog item. The engine's onItemObtained completes the
        # whole sequence regardless of step index (validated live on Catacombs),
        # mirroring the real in-game clog gate. This is legitimate ONLY for
        # no-driver steps: a step that SHOULD advance on a synthetic event but
        # does not is a real bug and must NOT be masked here.
        if cond in _NO_DRIVER and items:
            client.inject({"type": "itemObtained", "itemId": items[0]})
            state = _read_until(client, name, cur_index)
            if _is_complete(state, name):
                advanced = True
                result["status"] = "PASS"
                result["lastStep"] = idx
                result["reason"] = (
                    f"force-completed at step {idx} ({cond}) via clog item {items[0]}"
                )
                return result
            new_index = state.get("currentStepIndex", cur_index)
            if new_index > cur_index:
                advanced = True
                continue

        # stuck on this step
        result["lastStep"] = idx
        if not advanced:
            result["status"] = "FAIL"
            result["reason"] = f"stuck at step 0 ({cond}); no advance"
        else:
            result["status"] = "PARTIAL"
            result["reason"] = f"advanced then stuck at step {idx} ({cond})"
        return result

    # Walked every step without _is_complete firing.
    result["lastStep"] = len(steps) - 1
    if only_inventory_gated and not items:
        result["status"] = "SKIP"
        result["reason"] = "only INVENTORY-gated steps and no clog item to force-complete"
    elif advanced:
        result["status"] = "PARTIAL"
        result["reason"] = "advanced through all steps but engine never reported complete"
    else:
        result["status"] = "FAIL"
        result["reason"] = "stuck at step 0 across all steps"
    return result


# --------------------------------------------------------------------------- #
# Orchestration                                                               #
# --------------------------------------------------------------------------- #
def load_sources(data_path: Path) -> list:
    with data_path.open(encoding="utf-8") as fh:
        return json.load(fh)


def select_sources(sources: list, args) -> list:
    if args.source:
        needle = args.source.lower()
        exact = [s for s in sources if (s.get("name") or "").lower() == needle]
        if exact:
            return exact
        partial = [s for s in sources if needle in (s.get("name") or "").lower()]
        if not partial:
            raise SystemExit(f"no source matches {args.source!r}")
        return partial
    if args.sample is not None:
        rng = random.Random(args.seed)
        pool = list(sources)
        rng.shuffle(pool)
        return pool[: args.sample]
    return list(sources)  # --all


def aggregate(results: list) -> dict:
    total_items = sum(r["itemCount"] for r in results)
    pass_items = sum(r["itemCount"] for r in results if r["status"] == "PASS")
    confidence = (100.0 * pass_items / total_items) if total_items else 0.0
    return {
        "confidencePct": round(confidence, 2),
        "passCount": sum(1 for r in results if r["status"] == "PASS"),
        "total": len(results),
        "passItems": pass_items,
        "totalItems": total_items,
    }


def print_table(results: list, agg: dict) -> None:
    name_w = max((len(str(r["source"])) for r in results), default=6)
    name_w = max(name_w, 6)
    print(f"{'SOURCE'.ljust(name_w)}  {'STATUS':<8} {'ITEMS':>5}  LAST  REASON")
    print("-" * (name_w + 40))
    for r in sorted(results, key=lambda x: (x["status"] or "", -x["itemCount"])):
        print(
            f"{str(r['source']).ljust(name_w)}  {str(r['status']):<8} "
            f"{r['itemCount']:>5}  {str(r['lastStep']):>4}  {r['reason']}"
        )
    print("-" * (name_w + 40))
    print(
        f"PASS {agg['passCount']}/{agg['total']} sources | "
        f"item-weighted confidence {agg['confidencePct']}% "
        f"({agg['passItems']}/{agg['totalItems']} clog items)"
    )


def selection_signature(args) -> dict:
    """A stable descriptor of WHICH sources a run targets.

    Resume only reuses a prior report whose signature matches the current run's
    selection, so e.g. a `--sample 20 --seed 0` report is never reused for a
    `--sample 20 --seed 7` (or `--all`) invocation. `--source` matches on the
    same needle; `--sample` matches on the same seed AND N.
    """
    if args.source:
        return {"mode": "source", "source": args.source}
    if args.sample is not None:
        return {"mode": "sample", "seed": args.seed, "sample": args.sample}
    return {"mode": "all"}


def build_report(results: list, agg: dict, selected: list, data_path: Path,
                 signature: dict, partial: bool) -> dict:
    return {
        "generatedFrom": str(data_path.resolve().relative_to(REPO_ROOT).as_posix())
        if data_path.resolve().is_relative_to(REPO_ROOT)
        else str(data_path),
        "selection": signature,
        "partial": partial,
        "selected": [s.get("name") for s in selected],
        "results": [
            {
                "source": r["source"],
                "status": r["status"],
                "lastStep": r["lastStep"],
                "reason": r["reason"],
                "itemCount": r["itemCount"],
            }
            for r in results
        ],
        "confidencePct": agg["confidencePct"],
        "passCount": agg["passCount"],
        "total": agg["total"],
    }


def write_report(results: list, selected: list, report_path: Path,
                 data_path: Path, signature: dict, partial: bool) -> None:
    """Atomically (re)write the report over the results gathered SO FAR.

    Called after every source so an interrupted run keeps completed results.
    `confidencePct`/`passCount` are recomputed each write over results-so-far;
    `partial` is True until the full selection has been driven.
    """
    agg = aggregate(results)
    report = build_report(results, agg, selected, data_path, signature, partial)
    atomic_write_text(report_path, json.dumps(report, indent=2) + "\n")


def load_prior_results(report_path: Path, signature: dict) -> dict:
    """Load PASS results from a prior report iff its selection matches.

    Returns {source_name: result_dict} for sources recorded as PASS, so a resume
    can skip re-driving them. A missing/unreadable report, or one whose
    `selection` signature differs from the current run, yields {} (fresh start).
    """
    try:
        prior = json.loads(report_path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    if prior.get("selection") != signature:
        return {}
    out = {}
    for r in prior.get("results") or []:
        if r.get("status") == "PASS" and r.get("source") is not None:
            out[r["source"]] = r
    return out


def main(argv: list | None = None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    sel = p.add_mutually_exclusive_group()
    sel.add_argument("--source", help="run a single source by (partial) name")
    sel.add_argument("--sample", type=int, help="run N random sources")
    sel.add_argument("--all", action="store_true", help="run every source")
    p.add_argument("--seed", type=int, default=0, help="RNG seed for --sample")
    p.add_argument("--data", type=Path, default=DEFAULT_DATA)
    p.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    p.add_argument("--bridge-dir", type=Path, default=None)
    p.add_argument(
        "--timeout", type=float, default=4.0,
        help="per-call bridge response timeout in seconds (default 4; the "
             "harness normally responds sub-second)",
    )
    p.add_argument(
        "--fresh", action="store_true",
        help="ignore any existing report and re-drive every selected source",
    )
    p.add_argument(
        "--selftest", action="store_true",
        help="run offline unit tests (no live bridge) and exit",
    )
    args = p.parse_args(argv)

    if args.selftest:
        from test_acceptance_runner import run_selftests  # noqa: local sibling
        return run_selftests()

    if not (args.source or args.sample is not None or args.all):
        p.error("one of --source / --sample / --all is required (or --selftest)")

    sources = load_sources(args.data)
    selected = select_sources(sources, args)
    signature = selection_signature(args)
    client = BridgeClient(bridge_dir=args.bridge_dir, timeout_s=args.timeout)

    # Resume: load PASS results from a prior matching report and skip re-driving
    # them. PARTIAL/FAIL/SKIP are intentionally re-driven so fixes get
    # re-measured. --fresh ignores any prior report entirely.
    prior_pass = {} if args.fresh else load_prior_results(args.report, signature)
    to_drive = [s for s in selected if s.get("name") not in prior_pass]
    if prior_pass:
        print(
            f"resuming: {len(prior_pass)} PASS loaded, {len(to_drive)} to drive."
        )

    # Seed results with the carried-over PASS rows (in selection order) so the
    # incremental report is complete from the first write.
    results = [prior_pass[s.get("name")] for s in selected
               if s.get("name") in prior_pass]

    for s in to_drive:
        try:
            results.append(run_source(s, client))
        except (TimeoutError, RuntimeError) as e:
            results.append(
                {
                    "source": s.get("name"),
                    "status": "FAIL",
                    "lastStep": None,
                    "reason": f"bridge error: {e}",
                    "itemCount": len(clog_item_ids(s)),
                }
            )
        # Incremental checkpoint after EACH source so an interrupt (reboot,
        # Ctrl-C) preserves everything completed so far.
        partial = len(results) < len(selected)
        write_report(results, selected, args.report, args.data, signature, partial)

    # Final write flips partial -> False even when nothing was driven (full
    # resume of an already-complete selection).
    write_report(results, selected, args.report, args.data, signature, False)
    agg = aggregate(results)
    print_table(results, agg)
    print(f"\nWrote {args.report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
