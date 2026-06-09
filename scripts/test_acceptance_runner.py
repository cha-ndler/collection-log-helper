#!/usr/bin/env python3
"""Offline unit tests for acceptance_runner.synthesize_events (+ helpers).

Pure tests: NO live bridge, no I/O. Pins the completionCondition -> event map,
coord clamping, completionNpcIds[0] fallback, ITEM_OBTAINED fallback, and the
no-driver -> [] contract. Run directly (`python test_acceptance_runner.py`) or
via `acceptance_runner.py --selftest`.
"""

from __future__ import annotations

import sys
import unittest

import acceptance_runner as ar


class SynthesizeEventsTest(unittest.TestCase):
    def test_arrive_at_tile_move_away_then_arrive(self):
        step = {"completionCondition": "ARRIVE_AT_TILE", "worldX": 2918,
                "worldY": 3745, "worldPlane": 0}
        evs = ar.synthesize_events(step, [])
        self.assertEqual(
            evs,
            [
                {"type": "playerMoved", "x": 2958, "y": 3785, "plane": 0},
                {"type": "playerMoved", "x": 2918, "y": 3745, "plane": 0},
            ],
        )

    def test_arrive_at_tile_clamps_negative_away_tile(self):
        step = {"completionCondition": "ARRIVE_AT_TILE", "worldX": 0,
                "worldY": 0, "worldPlane": 2}
        evs = ar.synthesize_events(step, [])
        # +40 away is fine; the clamp only matters if worldX/Y were negative,
        # but worldX/Y themselves must never emit negative either.
        self.assertEqual(evs[0], {"type": "playerMoved", "x": 40, "y": 40, "plane": 2})
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

    def test_player_on_plane(self):
        step = {"completionCondition": "PLAYER_ON_PLANE", "worldX": 2862,
                "worldY": 5357, "worldPlane": 2}
        self.assertEqual(
            ar.synthesize_events(step, []),
            [{"type": "playerMoved", "x": 2862, "y": 5357, "plane": 2}],
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


def run_selftests() -> int:
    suite = unittest.defaultTestLoader.loadTestsFromModule(sys.modules[__name__])
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    sys.exit(run_selftests())
