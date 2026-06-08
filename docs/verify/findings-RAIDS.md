# RAIDS source verification — findings

Branch: `verify/RAIDS` · Date: 2026-06-07 · Verifier: cache-id + judgment pass per session protocol.

MCP pre-flight: `resource_health` → **ok** (osrs-wiki, prices-api, wise-old-man, temple-osrs, runelite-ids, mejrs-data all 200).

Scope: the 7 RAIDS sources in `src/main/resources/com/collectionloghelper/drop_rates.json`:
Chambers of Xeric · Chambers of Xeric (Challenge Mode) · Theatre of Blood · Theatre of Blood (Hard Mode) · Tombs of Amascut · Tombs of Amascut (300 Invocation) · Tombs of Amascut (500 Invocation).

Method:
- **IDs** — `validate_drop_rates --check cache_ids` per source (abextm cache = sole id authority).
- **Judgment** — requirement/quest gates, fairy-ring codes, arrival coords via `travel_path_verify` + `coordinate_helper`, travel direction. Each judgment finding carries a cited raw receipt and was passed through the `domain-skeptic` agent; only STANDS verdicts are kept.

drop_rates.json was **not** modified (verification only).

---

## Summary

| # | Source(s) | Type | Severity | Verdict |
|---|-----------|------|----------|---------|
| F1 | Theatre of Blood, Theatre of Blood (Hard Mode) | Quest gate over-strict | High | STANDS |
| F2 | Theatre of Blood | Waypoint quest req wrong | Low | STANDS |
| F3 | Theatre of Blood | cache_ids name mismatch (id OK) | Low | tool receipt |
| F4 | Tombs of Amascut ×3 | cache_ids name mismatch (id OK) | Low | tool receipt |

Chambers of Xeric and Chambers of Xeric (Challenge Mode) passed all checks with no findings.
`Unsired` (25624) is not present in RAIDS sources; no action. No "duplicate item" findings logged (the same clog item appearing across multiple sources / item-tier variants is correct by rule).

---

## F1 — Theatre of Blood quest gate is over-strict (High) — STANDS

**Sources:** `Theatre of Blood` (drop_rates.json:6516), `Theatre of Blood (Hard Mode)` (drop_rates.json:6741).
**Data:** top-level `requirements.quests = ["A_TASTE_OF_HOPE"]`.
**Should be:** `["PRIEST_IN_PERIL"]`.

**Raw receipt — `wiki_lookup` "Theatre of Blood":**
> "The only hard requirement to participate in the raid is the completion of the Priest in Peril quest to be able to access Morytania."

**Raw receipt — `wiki_lookup` "Ver Sinhaza" (via skeptic):**
> Drakan's medallion (needs A Taste of Hope) is one of three routes; the Andras-pay-to-Slepe route and the Ectophial route require only Morytania access (Priest in Peril), not A Taste of Hope.

**Why it stands:** `RequirementsChecker.checkRequirementsDetail` treats `requirements.quests` as a **hard FINISHED accessibility gate** (`src/main/java/com/collectionloghelper/data/RequirementsChecker.java:312-321`, marks source inaccessible at 84-90). A player with Priest in Peril but not A Taste of Hope can still enter ToB (Slepe/Andras route), so the current gate hides an accessible source. The project's own convention confirms this: every other Morytania source gates on `PRIEST_IN_PERIL`, and ToB's own travel waypoint already carries `PRIEST_IN_PERIL` while the top-level gate contradicts it. The A Taste of Hope convenience (Drakan's medallion) is already preserved as a `conditionalAlternative`, so correcting the gate loses no information.

**Skeptic:** STANDS — high. Ran all refutation vectors (multi-source / variant / account-type / staleness via `wiki_updates` count=0 / requirement-nuance); none rescued the value.

**status:** resolved 2026-06-07 (set `requirements.quests` to `["PRIEST_IN_PERIL"]` on Theatre of Blood and Theatre of Blood (Hard Mode); `lintDropRates build` green).

---

## F2 — Drakan's medallion → Ver Sinhaza waypoint requires the wrong quest (Low) — STANDS

**Source:** `Theatre of Blood`, waypoint "Drakan's medallion to Ver Sinhaza" (drop_rates.json:6646; requirement at :6652).
**Data:** `requirements.quests = ["SINS_OF_THE_FATHER"]`.
**Should be:** `["A_TASTE_OF_HOPE"]`.

**Raw receipt — `wiki_lookup` "Drakan's medallion":**
> "Drakan's medallion is a reward from the quest **A Taste of Hope**. It allows unlimited teleportation to **Ver Sinhaza**, **Darkmeyer (after completion of Sins of the Father)**, and the Sisterhood Sanctuary under Slepe..."

**Why it stands:** The "after completion of Sins of the Father" parenthetical attaches to the **Darkmeyer** destination only. The Ver Sinhaza teleport is available immediately on obtaining the medallion (i.e. completing A Taste of Hope). The file itself already distinguishes the two gates correctly for the Darkmeyer waypoints (line 32387), so this is an isolated wrong-quest gate, not a convention. Blast radius is low (worst case the plugin suggests the slower Canifis-walk fallback for AToH-but-not-SotF accounts; it never routes a player somewhere they cannot go). Note: `Theatre of Blood (Hard Mode)` has **no** `waypoints` array, so this finding is confined to the regular source.

**Skeptic:** STANDS — low.

**status:** resolved 2026-06-07 (set the "Drakan's medallion to Ver Sinhaza" waypoint `requirements.quests` to `["A_TASTE_OF_HOPE"]`; `lintDropRates build` green).

---

## F3 — Theatre of Blood cache_ids name mismatch (id correct) (Low) — tool receipt

**Raw receipt — `validate_drop_rates --check cache_ids --source_name "Theatre of Blood"`:**
> [Theatre of Blood] [cache_ids] itemId 22486 name mismatch: data "Scythe of vitur" vs cache "Scythe of vitur (uncharged)" — verify the canonical id
> [Theatre of Blood] [cache_ids] itemId 22481 name mismatch: data "Sanguinesti staff" vs cache "Sanguinesti staff (uncharged)" — verify the canonical id

**Assessment:** The **ids are cache-confirmed correct** — 22486 / 22481 are the uncharged variants the collection log actually tracks. Only the stored display-name string is off (missing "(uncharged)"). This is internally inconsistent: the `Theatre of Blood (Hard Mode)` source already names the same ids "Scythe of vitur (uncharged)" / "Sanguinesti staff (uncharged)" and passes clean. No id change needed; cosmetic name string only. (Deterministic tool output — no domain-skeptic pass required.)

---

## F4 — Tombs of Amascut cache_ids name mismatch (id correct) (Low) — tool receipt

**Raw receipt — `validate_drop_rates --check cache_ids --source_name "Tombs of Amascut"`:**
> [Tombs of Amascut] [cache_ids] itemId 27277 name mismatch: data "Tumeken's shadow" vs cache "Tumeken's shadow (uncharged)" — verify the canonical id
> [Tombs of Amascut (300 Invocation)] [cache_ids] itemId 27277 name mismatch: data "Tumeken's shadow" vs cache "Tumeken's shadow (uncharged)"
> [Tombs of Amascut (500 Invocation)] [cache_ids] itemId 27277 name mismatch: data "Tumeken's shadow" vs cache "Tumeken's shadow (uncharged)"

**Assessment:** id 27277 is **cache-confirmed correct** (the uncharged shadow is the clog-tracked drop). Only the display-name string differs (missing "(uncharged)"). Cosmetic name string only across all three ToA variants; no id change needed. (Deterministic tool output — no domain-skeptic pass required.)

---

## Checks that passed (no findings)

- **cache_ids** — Chambers of Xeric, Chambers of Xeric (Challenge Mode): clean. ToA family: all ids valid (only the 27277 name string flagged, F4). ToB family: all ids valid (only 22486/22481 name strings flagged, F3). Drop rates, pet/independent flags, `requiresPrevious` chains, and `mutuallyExclusiveSources` pairings all consistent.
- **Arrival coords (`travel_path_verify` + `coordinate_helper identify`):** all RAIDS coords in-bounds and on the correct plane. CoX (1233,3573)=Chambers of Xeric center; ToB (3650,3218)=Ver Sinhaza (ToB) center; ToA (3234,2784)=Necropolis (58 tiles from Sophanem). `travel_path_verify` reported zero RAIDS findings (its out-of-bounds hits were all in non-RAIDS sources).
- **Requirement gates (other):** ToA / ToA 300 / ToA 500 `requirements.quests = ["BENEATH_CURSED_SANDS"]` — correct (hard gate to access the Tombs). CoX / CoX CM no quest gate — correct.
- **Fairy-ring / travel direction:** ToA guidance "fairy ring AKP and run south" — AKP lands north of the Necropolis, so running south to the entrance is the correct direction. (`travel_path_verify` v1 does not yet semantically cross-check fairy-ring codes; verified by direction consistency, no contradiction found.)
- **Travel tips:** CoX (Xeric's talisman), ToB (Drakan's medallion), ToA (Pharaoh's sceptre Jaltevas) all consistent with wiki travel methods.

---

## Recommended fixes (for a separate data-edit PR — NOT applied here)

1. F1: set `requirements.quests` to `["PRIEST_IN_PERIL"]` for both Theatre of Blood and Theatre of Blood (Hard Mode).
2. F2: set the "Drakan's medallion to Ver Sinhaza" waypoint `requirements.quests` to `["A_TASTE_OF_HOPE"]`.
3. F3/F4: append "(uncharged)" to the data `name` strings for 22486, 22481 (ToB) and 27277 (ToA ×3) to match the cache canonical names and the existing Hard Mode source. Ids are correct — no id change.
