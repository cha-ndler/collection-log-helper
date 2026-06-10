#!/usr/bin/env python3
"""Offline unit tests for acceptance_runner.synthesize_events (+ helpers).

Pure tests: NO live bridge, no I/O. Pins the completionCondition -> event map,
coord clamping, completionNpcIds[0] fallback, ITEM_OBTAINED fallback, and the
no-driver -> [] contract. Run directly (`python test_acceptance_runner.py`) or
via `acceptance_runner.py --selftest`.
"""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

import acceptance_runner as ar


class SynthesizeEventsTest(unittest.TestCase):
    def test_arrive_at_tile_move_away_then_arrive(self):
        step = {"completionCondition": "ARRIVE_AT_TILE", "worldX": 2918,
                "worldY": 3745, "worldPlane": 0}
        evs = ar.synthesize_events(step, [])
        # +128 away (was +40): a manual Catacombs drive needed ~64 tiles before
        # the arrival registered.
        self.assertEqual(
            evs,
            [
                {"type": "playerMoved", "x": 3046, "y": 3873, "plane": 0},
                {"type": "playerMoved", "x": 2918, "y": 3745, "plane": 0},
            ],
        )

    def test_arrive_at_tile_clamps_negative_away_tile(self):
        step = {"completionCondition": "ARRIVE_AT_TILE", "worldX": 0,
                "worldY": 0, "worldPlane": 2}
        evs = ar.synthesize_events(step, [])
        # +128 away is fine; the clamp only matters if worldX/Y were negative,
        # but worldX/Y themselves must never emit negative either.
        self.assertEqual(evs[0], {"type": "playerMoved", "x": 128, "y": 128, "plane": 2})
        self.assertEqual(evs[1], {"type": "playerMoved", "x": 0, "y": 0, "plane": 2})
        # explicit negative-input clamp on the arrive tile
        neg = ar.synthesize_events(
            {"completionCondition": "ARRIVE_AT_TILE", "worldX": -5, "worldY": -5,
             "worldPlane": 0},
            [],
        )
        self.assertEqual(neg[1], {"type": "playerMoved", "x": 0, "y": 0, "plane": 0})

    def test_arrive_at_zone_uses_zone_centre(self):
        step = {"completionCondition": "ARRIVE_AT_ZONE",
                "completionZone": [1300, 3795, 1380, 10280, 0]}
        evs = ar.synthesize_events(step, [])
        self.assertEqual(
            evs,
            [{"type": "playerMoved", "x": (1300 + 1380) // 2,
              "y": (3795 + 10280) // 2, "plane": 0}],
        )

    def test_player_on_plane_emits_transition(self):
        # PLAYER_ON_PLANE injects a DIFFERENT plane first, then the target,
        # so the engine sees a real plane change (a single same-plane inject
        # can no-op). For a non-zero target plane the "other" plane is 0.
        step = {"completionCondition": "PLAYER_ON_PLANE", "worldX": 2862,
                "worldY": 5357, "worldPlane": 2}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [
                {"type": "playerMoved", "x": 2862, "y": 5357, "plane": 0},
                {"type": "playerMoved", "x": 2862, "y": 5357, "plane": 2},
            ],
        )

    def test_player_on_plane_zero_target_uses_plane_one_as_other(self):
        # For a plane-0 target the "other" plane is 1, so the transition is real.
        step = {"completionCondition": "PLAYER_ON_PLANE", "worldX": 3200,
                "worldY": 3200, "worldPlane": 0}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [
                {"type": "playerMoved", "x": 3200, "y": 3200, "plane": 1},
                {"type": "playerMoved", "x": 3200, "y": 3200, "plane": 0},
            ],
        )

    def test_npc_talked_to(self):
        step = {"completionCondition": "NPC_TALKED_TO", "completionNpcId": 10405}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "npcInteracted", "npcId": 10405}],
        )

    def test_actor_death_single_id(self):
        step = {"completionCondition": "ACTOR_DEATH", "completionNpcId": 2215}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "npcDeath", "npcId": 2215}],
        )

    def test_actor_death_falls_back_to_npc_ids_first(self):
        step = {"completionCondition": "ACTOR_DEATH",
                "completionNpcIds": [3127, 3128, 3129]}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "npcDeath", "npcId": 3127}],
        )

    def test_chat_message_received(self):
        step = {"completionCondition": "CHAT_MESSAGE_RECEIVED",
                "completionChatPattern": "Your Nightmare kill count is:"}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "chatMessage", "text": "Your Nightmare kill count is:"}],
        )

    def test_varbit_at_least(self):
        step = {"completionCondition": "VARBIT_AT_LEAST",
                "completionVarbitId": 3975, "completionVarbitValue": 40}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "varbitChanged", "varbitId": 3975, "value": 40}],
        )

    def test_item_obtained_explicit_id(self):
        step = {"completionCondition": "ITEM_OBTAINED", "completionItemId": 995}
        self.assertEqual(
            ar.synthesize_events(step, [11832, 11834]),
            [{"type": "itemObtained", "itemId": 995}],
        )

    def test_item_obtained_falls_back_to_first_clog_item(self):
        step = {"completionCondition": "ITEM_OBTAINED"}
        self.assertEqual(
            ar.synthesize_events(step, [11832, 11834]),
            [{"type": "itemObtained", "itemId": 11832}],
        )

    def test_no_driver_conditions_return_empty(self):
        for cond in ("MANUAL", "INVENTORY_HAS_ITEM", "INVENTORY_NOT_HAS_ITEM"):
            step = {"completionCondition": cond, "completionItemId": 1925}
            self.assertEqual(ar.synthesize_events(step, [7777]), [],
                             f"{cond} should yield no synthetic driver")


class HelperTest(unittest.TestCase):
    def test_clog_item_ids_extracts_item_ids(self):
        src = {"items": [{"itemId": 11832}, {"itemId": 11834}, {"name": "noid"}]}
        self.assertEqual(ar.clog_item_ids(src), [11832, 11834])

    def test_clamp(self):
        self.assertEqual(ar._clamp(-3), 0)
        self.assertEqual(ar._clamp(0), 0)
        self.assertEqual(ar._clamp(7), 7)

    def test_other_plane(self):
        # always a DIFFERENT plane than the input, so a transition is forced.
        self.assertEqual(ar._other_plane(0), 1)
        self.assertEqual(ar._other_plane(1), 0)
        self.assertEqual(ar._other_plane(2), 0)
        self.assertEqual(ar._other_plane(3), 0)

    def test_move_away_offset_is_large_enough(self):
        # Catacombs needed ~64 tiles in a manual drive; +40 was insufficient.
        self.assertGreaterEqual(ar._MOVE_AWAY_OFFSET, 64)

    def test_is_complete_variants(self):
        self.assertTrue(ar._is_complete({"guidanceActive": False}, "X"))
        self.assertTrue(
            ar._is_complete({"guidanceActive": True, "activeSource": "Y"}, "X")
        )
        self.assertTrue(
            ar._is_complete(
                {"guidanceActive": True, "activeSource": "X",
                 "currentStepIndex": 3, "totalSteps": 3}, "X"
            )
        )
        self.assertFalse(
            ar._is_complete(
                {"guidanceActive": True, "activeSource": "X",
                 "currentStepIndex": 1, "totalSteps": 3}, "X"
            )
        )

    def test_chat_event_translated_to_message_at_wire(self):
        # BridgeClient maps synthesize_events' `text` -> harness `message`.
        ev = {"type": "chatMessage", "text": "hi"}
        out = ar.BridgeClient._translate_event(ev)
        self.assertEqual(out, {"type": "chatMessage", "message": "hi"})
        # non-chat events pass through unchanged
        self.assertEqual(
            ar.BridgeClient._translate_event({"type": "npcDeath", "npcId": 5}),
            {"type": "npcDeath", "npcId": 5},
        )

    def test_aggregate_is_item_weighted(self):
        results = [
            {"status": "PASS", "itemCount": 30},
            {"status": "FAIL", "itemCount": 10},
            {"status": "PASS", "itemCount": 10},
        ]
        agg = ar.aggregate(results)
        self.assertEqual(agg["passItems"], 40)
        self.assertEqual(agg["totalItems"], 50)
        self.assertEqual(agg["confidencePct"], 80.0)
        self.assertEqual(agg["passCount"], 2)


class FakeClient:
    """In-memory stand-in for BridgeClient -- NO live bridge, no I/O.

    Scripted with a state-machine: each injected event may advance
    `currentStepIndex` or complete the sequence, optionally after a number of
    reads (to simulate tick-deferred completion). `reads_until` maps an event
    signature to "complete after N further reads".
    """

    def __init__(self, name, total_steps, plan):
        self.name = name
        self.total = total_steps
        self.plan = plan  # list of dicts describing how injects mutate state
        self.idx = 0
        self.done = False
        self.slot_unlocked = False
        self.injects = []
        self._pending = None  # (target_state, reads_remaining)

    def ping(self):
        return {}

    def activate(self, source):
        return {}

    def inject(self, event):
        self.injects.append(event)
        for rule in self.plan:
            if rule["match"](event):
                action = rule["action"]
                if action.get("deferReads"):
                    self._pending = dict(action)
                    self._pending["reads_remaining"] = action["deferReads"]
                else:
                    self._apply(action)
                break
        return {}

    def _apply(self, action):
        if "advanceTo" in action:
            self.idx = action["advanceTo"]
        if action.get("complete"):
            self.done = True
        if action.get("slotUnlocked"):
            # Looping ACTIVITY source: the target slot unlocks but guidance stays
            # active (the engine keeps the grind loop running by design).
            self.slot_unlocked = True

    def read_state(self):
        if self._pending is not None:
            self._pending["reads_remaining"] -= 1
            if self._pending["reads_remaining"] <= 0:
                self._apply(self._pending)
                self._pending = None
        return {
            "guidanceActive": not self.done,
            "activeSource": self.name,
            "currentStepIndex": self.idx,
            "totalSteps": self.total,
            "targetSlotUnlocked": self.slot_unlocked,
        }


class RunSourceTest(unittest.TestCase):
    """Drive run_source against FakeClient. time.sleep patched to a no-op."""

    def setUp(self):
        self._sleep = ar.time.sleep
        ar.time.sleep = lambda *_a, **_k: None

    def tearDown(self):
        ar.time.sleep = self._sleep

    def test_tick_deferred_actor_death_passes(self):
        # Narwhal-style: step 0 ARRIVE_AT_TILE advances, step 1 ACTOR_DEATH
        # completes but only on a LATER read (one tick late). Must PASS, not
        # PARTIAL -- this is the tick-deferral fix.
        src = {
            "name": "Narwhal (Boat Combat)",
            "items": [{"itemId": 31406}],
            "guidanceSteps": [
                {"completionCondition": "ARRIVE_AT_TILE", "worldX": 1, "worldY": 1,
                 "worldPlane": 0},
                {"completionCondition": "ACTOR_DEATH", "completionNpcId": 15202},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "playerMoved" and e.get("x") == 1,
             "action": {"advanceTo": 1}},
            {"match": lambda e: e["type"] == "npcDeath",
             "action": {"complete": True, "deferReads": 3}},
        ]
        client = FakeClient("Narwhal (Boat Combat)", 2, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PASS", res["reason"])
        self.assertEqual(res["lastStep"], 1)

    def test_force_complete_on_non_terminal_manual_step(self):
        # Tempoross-style: step 0 ARRIVE advances, step 1 MANUAL never advances
        # on its own; the runner force-completes via clog item even though MANUAL
        # is NOT the last step (step 2 follows). Must PASS.
        src = {
            "name": "Tempoross",
            "items": [{"itemId": 25564}],
            "guidanceSteps": [
                {"completionCondition": "ARRIVE_AT_TILE", "worldX": 5, "worldY": 5,
                 "worldPlane": 0},
                {"completionCondition": "MANUAL"},
                {"completionCondition": "MANUAL"},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "playerMoved" and e.get("x") == 5,
             "action": {"advanceTo": 1}},
            {"match": lambda e: e["type"] == "itemObtained",
             "action": {"complete": True}},
        ]
        client = FakeClient("Tempoross", 3, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PASS", res["reason"])
        self.assertEqual(res["lastStep"], 1)
        self.assertIn("force-completed", res["reason"])

    def test_looping_activity_slot_unlock_passes(self):
        # Tempoross/Wintertodt-style looping ACTIVITY source: step 0 ARRIVE
        # advances, step 1 MANUAL is mid-loop. The force-complete itemObtained
        # unlocks the target slot but the engine intentionally KEEPS guidance
        # active (the grind loop continues by design). The slot-unlock IS the
        # success: must PASS, not PARTIAL.
        src = {
            "name": "Tempoross",
            "items": [{"itemId": 25564}],
            "guidanceSteps": [
                {"completionCondition": "ARRIVE_AT_TILE", "worldX": 5, "worldY": 5,
                 "worldPlane": 0},
                {"completionCondition": "MANUAL"},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "playerMoved" and e.get("x") == 5,
             "action": {"advanceTo": 1}},
            # itemObtained unlocks the slot but does NOT complete (mid-loop).
            {"match": lambda e: e["type"] == "itemObtained",
             "action": {"slotUnlocked": True}},
        ]
        client = FakeClient("Tempoross", 2, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PASS", res["reason"])
        self.assertEqual(res["lastStep"], 1)
        self.assertIn("target slot unlocked", res["reason"])

    def test_active_no_slot_unlock_stays_partial(self):
        # Same shape but the force-complete does NOT unlock the slot (engine still
        # active, targetSlotUnlocked false). This is a genuinely-stuck no-driver
        # step, NOT a by-design loop: must stay PARTIAL.
        src = {
            "name": "Mystery Activity",
            "items": [{"itemId": 25564}],
            "guidanceSteps": [
                {"completionCondition": "ARRIVE_AT_TILE", "worldX": 5, "worldY": 5,
                 "worldPlane": 0},
                {"completionCondition": "MANUAL"},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "playerMoved" and e.get("x") == 5,
             "action": {"advanceTo": 1}},
            # itemObtained does nothing -> still active, slot NOT unlocked.
        ]
        client = FakeClient("Mystery Activity", 2, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PARTIAL", res["reason"])
        self.assertEqual(res["lastStep"], 1)

    def test_player_on_plane_transition_completes(self):
        # The transition shape (other-plane then target-plane) drives the engine.
        src = {
            "name": "Barrows",
            "items": [{"itemId": 4708}],
            "guidanceSteps": [
                {"completionCondition": "PLAYER_ON_PLANE", "worldX": 2, "worldY": 2,
                 "worldPlane": 0},
            ],
        }
        plan = [
            # only the SECOND (target-plane) inject completes; first is the
            # off-plane transition.
            {"match": lambda e: e["type"] == "playerMoved" and e.get("plane") == 0,
             "action": {"complete": True, "deferReads": 2}},
        ]
        client = FakeClient("Barrows", 1, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PASS", res["reason"])

    def test_genuine_stuck_step_is_not_masked(self):
        # A DRIVABLE step (ACTOR_DEATH) that never completes must stay PARTIAL --
        # force-complete is reserved for no-driver steps only.
        src = {
            "name": "Broken Boss",
            "items": [{"itemId": 999}],
            "guidanceSteps": [
                {"completionCondition": "ARRIVE_AT_TILE", "worldX": 7, "worldY": 7,
                 "worldPlane": 0},
                {"completionCondition": "ACTOR_DEATH", "completionNpcId": 12345},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "playerMoved" and e.get("x") == 7,
             "action": {"advanceTo": 1}},
            # npcDeath does nothing -> genuinely stuck, no force-complete allowed
        ]
        client = FakeClient("Broken Boss", 2, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PARTIAL", res["reason"])
        self.assertEqual(res["lastStep"], 1)
        # never injected an itemObtained for a drivable step
        self.assertTrue(all(e["type"] != "itemObtained" for e in client.injects))


class CountingClient(FakeClient):
    """FakeClient that counts read_state() calls, to prove the fast path."""

    def __init__(self, *a, **k):
        super().__init__(*a, **k)
        self.reads = 0

    def read_state(self):
        self.reads += 1
        return super().read_state()


class FastPathTest(unittest.TestCase):
    """The per-step read budget must NOT be consumed when a step advances on
    the first read."""

    def setUp(self):
        self._sleep = ar.time.sleep
        self._slept = []
        ar.time.sleep = lambda *a, **_k: self._slept.append(a)

    def tearDown(self):
        ar.time.sleep = self._sleep

    def test_first_success_does_not_consume_budget(self):
        # A single ACTOR_DEATH step that completes IMMEDIATELY on the first read:
        # _read_until must do exactly one read and never sleep.
        src = {
            "name": "Insta Boss",
            "items": [{"itemId": 42}],
            "guidanceSteps": [
                {"completionCondition": "ACTOR_DEATH", "completionNpcId": 1},
            ],
        }
        plan = [
            {"match": lambda e: e["type"] == "npcDeath",
             "action": {"complete": True}},  # no deferReads -> immediate
        ]
        client = CountingClient("Insta Boss", 1, plan)
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "PASS", res["reason"])
        # activate read (1) + the single in-loop _read_until read (1) = 2 reads,
        # and zero sleeps inside _read_until (fast path returned on read #1).
        self.assertEqual(client.reads, 2, "fast path should not re-read")
        self.assertEqual(self._slept, [], "fast path should not sleep")

    def test_stuck_step_does_consume_budget(self):
        # A genuinely-stuck DRIVABLE step exhausts the (now 3-read) budget:
        # _read_until reads 3 times and sleeps 2 times between them.
        src = {
            "name": "Stuck Boss",
            "items": [{"itemId": 9}],
            "guidanceSteps": [
                {"completionCondition": "ACTOR_DEATH", "completionNpcId": 1},
            ],
        }
        client = CountingClient("Stuck Boss", 1, [])  # nothing advances
        res = ar.run_source(src, client)
        self.assertEqual(res["status"], "FAIL", res["reason"])
        # activate read (1) + _SETTLE_READS reads in _read_until.
        self.assertEqual(client.reads, 1 + ar._SETTLE_READS)
        self.assertEqual(len(self._slept), ar._SETTLE_READS - 1)


def _src(name, items, steps):
    return {"name": name, "items": [{"itemId": i} for i in items],
            "guidanceSteps": steps}


class _Args:
    """Minimal stand-in for the argparse namespace fields resume reads."""

    def __init__(self, source=None, sample=None, seed=0, all=False, fresh=False):
        self.source = source
        self.sample = sample
        self.seed = seed
        self.all = all
        self.fresh = fresh


class ResumeReportTest(unittest.TestCase):
    """Incremental write + resume/skip-PASS + --fresh, all offline."""

    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.report = Path(self._dir.name) / "report.json"
        self.data = ar.DEFAULT_DATA  # only used for the generatedFrom string

    def tearDown(self):
        self._dir.cleanup()

    def _write(self, results, selected, signature, partial):
        ar.write_report(results, selected, self.report, self.data,
                        signature, partial)

    def test_incremental_write_is_valid_json_with_partial_flag(self):
        sig = {"mode": "all"}
        selected = [{"name": "A"}, {"name": "B"}]
        r1 = [{"source": "A", "status": "PASS", "lastStep": 0,
               "reason": "ok", "itemCount": 5}]
        self._write(r1, selected, sig, partial=True)
        doc = json.loads(self.report.read_text(encoding="utf-8"))
        self.assertTrue(doc["partial"])
        self.assertEqual(doc["selection"], sig)
        self.assertEqual(doc["passCount"], 1)
        self.assertEqual(doc["confidencePct"], 100.0)  # over results-so-far
        # final write of full selection flips partial -> False
        r2 = r1 + [{"source": "B", "status": "FAIL", "lastStep": 0,
                    "reason": "stuck", "itemCount": 5}]
        self._write(r2, selected, sig, partial=False)
        doc = json.loads(self.report.read_text(encoding="utf-8"))
        self.assertFalse(doc["partial"])
        self.assertEqual(doc["passCount"], 1)
        self.assertEqual(doc["confidencePct"], 50.0)

    def test_resume_loads_pass_only_when_signature_matches(self):
        sig = {"mode": "sample", "seed": 0, "sample": 2}
        results = [
            {"source": "A", "status": "PASS", "lastStep": 1, "reason": "ok",
             "itemCount": 3},
            {"source": "B", "status": "FAIL", "lastStep": 0, "reason": "x",
             "itemCount": 2},
        ]
        self._write(results, [{"name": "A"}, {"name": "B"}], sig, partial=False)
        # same signature -> PASS A loaded, FAIL B NOT loaded (re-driven)
        loaded = ar.load_prior_results(self.report, sig)
        self.assertEqual(set(loaded), {"A"})
        # different seed -> treated as fresh (nothing loaded)
        self.assertEqual(
            ar.load_prior_results(self.report,
                                  {"mode": "sample", "seed": 7, "sample": 2}),
            {},
        )
        # different mode -> fresh
        self.assertEqual(ar.load_prior_results(self.report, {"mode": "all"}), {})

    def test_load_prior_results_missing_report_is_fresh(self):
        self.assertEqual(
            ar.load_prior_results(self.report / "nope.json", {"mode": "all"}), {}
        )

    def test_selection_signature_modes(self):
        self.assertEqual(ar.selection_signature(_Args(all=True)), {"mode": "all"})
        self.assertEqual(
            ar.selection_signature(_Args(source="Zulrah")),
            {"mode": "source", "source": "Zulrah"},
        )
        self.assertEqual(
            ar.selection_signature(_Args(sample=20, seed=3)),
            {"mode": "sample", "seed": 3, "sample": 20},
        )

    def test_main_resume_skips_pass_and_redrives_non_pass(self):
        # End-to-end main() with a stub BridgeClient (NO live bridge). Prior
        # report has A=PASS, B=FAIL; resume must skip A and re-drive B (which the
        # stub now completes).
        sig = {"mode": "all"}
        prior = [
            {"source": "A", "status": "PASS", "lastStep": 0, "reason": "ok",
             "itemCount": 4},
            {"source": "B", "status": "FAIL", "lastStep": 0, "reason": "stuck",
             "itemCount": 2},
        ]
        self._write(prior, [{"name": "A"}, {"name": "B"}], sig, partial=False)

        data = [
            _src("A", [4], [{"completionCondition": "MANUAL"}]),
            _src("B", [2], [{"completionCondition": "ACTOR_DEATH",
                             "completionNpcId": 1}]),
        ]
        data_path = Path(self._dir.name) / "data.json"
        data_path.write_text(json.dumps(data), encoding="utf-8")

        driven = []

        class StubClient:
            def __init__(self, *a, **k):
                pass

            def ping(self):
                return {}

            def activate(self, source):
                driven.append(source)
                return {}

            def inject(self, event):
                return {}

            def read_state(self):
                # always report the (only) source complete on first read
                return {"guidanceActive": False}

        orig_client = ar.BridgeClient
        ar.BridgeClient = StubClient
        try:
            rc = ar.main([
                "--all", "--data", str(data_path), "--report", str(self.report),
            ])
        finally:
            ar.BridgeClient = orig_client

        self.assertEqual(rc, 0)
        # A was skipped (never activated); only B was driven.
        self.assertEqual(driven, ["B"])
        doc = json.loads(self.report.read_text(encoding="utf-8"))
        self.assertFalse(doc["partial"])
        by = {r["source"]: r for r in doc["results"]}
        self.assertEqual(by["A"]["status"], "PASS")  # carried over
        self.assertEqual(by["B"]["status"], "PASS")  # re-driven, now passes
        self.assertEqual(doc["total"], 2)

    def test_main_fresh_ignores_prior(self):
        sig = {"mode": "all"}
        prior = [{"source": "A", "status": "PASS", "lastStep": 0, "reason": "ok",
                  "itemCount": 4}]
        self._write(prior, [{"name": "A"}], sig, partial=False)
        data = [_src("A", [4], [{"completionCondition": "MANUAL"}])]
        data_path = Path(self._dir.name) / "data.json"
        data_path.write_text(json.dumps(data), encoding="utf-8")

        driven = []

        class StubClient:
            def __init__(self, *a, **k):
                pass

            def ping(self):
                return {}

            def activate(self, source):
                driven.append(source)
                return {}

            def inject(self, event):
                return {}

            def read_state(self):
                return {"guidanceActive": False}

        orig_client = ar.BridgeClient
        ar.BridgeClient = StubClient
        try:
            ar.main(["--all", "--fresh", "--data", str(data_path),
                     "--report", str(self.report)])
        finally:
            ar.BridgeClient = orig_client

        # --fresh -> A IS re-driven despite prior PASS.
        self.assertEqual(driven, ["A"])


def run_selftests() -> int:
    suite = unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(run_selftests())
