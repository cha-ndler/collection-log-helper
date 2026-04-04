# Feature Gap Analysis Report — 2026-04-04

Analysis of Collection Log Helper plugin (225 sources, 2,101 items) for data gaps, feature opportunities, and recent game updates.

---

## 1. Data Gaps in drop_rates.json

### 1.1 Missing `ironKillTimeSeconds` on BOSSES/RAIDS (13 sources)

Iron kill times differ significantly from mains for these sources. Without this field, ironman efficiency scoring uses the main kill time, which underestimates grind length.

| Source | Category | Fix Approach |
|--------|----------|-------------|
| The Whisperer | BOSSES | Wiki + community data |
| The Whisperer (Awakened) | BOSSES | Wiki + community data |
| Phosani's Nightmare | BOSSES | Wiki — solo-only, iron = main |
| Chambers of Xeric (Challenge Mode) | RAIDS | Community data (likely same as normal) |
| Tombs of Amascut (300 Invocation) | RAIDS | Community data |
| Corrupted Gauntlet | BOSSES | Solo-only, iron = main (verify) |
| Crazy Archaeologist | BOSSES | Low priority wilderness boss |
| Perilous Moons | BOSSES | New content, needs benchmarking |
| The Hueycoatl | BOSSES | New content, needs benchmarking |
| Royal Titans | BOSSES | New content, needs benchmarking |
| Brutus | BOSSES | New boss, needs benchmarking |
| Shellbane Gryphon | BOSSES | Sailing boss, needs benchmarking |
| Deranged Archaeologist | BOSSES | Low priority wilderness boss |

**Complexity:** S | **Priority:** P2 — Affects ironman ranking accuracy for 13 sources

### 1.2 Missing `npcId` (100 sources)

100 sources lack an NPC ID, which means no NPC highlighting, no right-click "Collection Log Guide" menu option, and no NPC-based overlays. Breakdown:

- **Expected (no NPC):** Clues (12), shop/chest sources, skilling, salvage — these genuinely don't have a target NPC to highlight
- **Actionable — could add npcId:**
  - Revenants — multiple NPCs, could use primary (Revenant demon = 7937)
  - Pyramid Plunder — Guardian mummy (8648)
  - Elven Crystal Chest — no NPC but has objectId potential
  - Champion's Challenge — various champion NPCs
  - Ogress Shaman (7990), Armoured Zombie (12454), Prifddinas Elf (various), Pickpocketing Darkmeyer Vyre (various)

**~15 sources** could reasonably gain npcId for better overlay support.

**Complexity:** S per source | **Priority:** P3 — Nice-to-have, most of these are niche sources

### 1.3 Sources with MANUAL Final Step (98 sources)

98 sources end with a MANUAL completion condition, meaning the guidance sequence never auto-completes. Categories:

- **Minigames (22):** Understandable — most minigames don't have a single "kill" event. Could potentially use `CHAT_MESSAGE_RECEIVED` for reward messages.
- **Clues (12):** Could use `CHAT_MESSAGE_RECEIVED` for "You've completed X clue scrolls" or `WIDGET_VISIBLE` for reward casket interface.
- **OTHER/Skilling (48):** Many are 2-step sequences (arrive + manual). Some could auto-complete via loot events or chat messages.
- **Boat Combat (6):** Could use `ACTOR_DEATH` with sea creature NPC IDs.
- **Salvage (8):** Could use `INTERACTION_COMPLETED` or `CHAT_MESSAGE_RECEIVED` on salvage opening.

**High-value targets for auto-completion:**
1. Boat Combat sources (6) — add npcId + ACTOR_DEATH
2. Clue sources (12) — CHAT_MESSAGE_RECEIVED on completion message
3. Fight Caves / Inferno (2) — CHAT_MESSAGE_RECEIVED on wave completion
4. Minigames with reward messages (Tempoross, GoTR already have it — extend to others)

**Complexity:** M | **Priority:** P2 — Improves guidance UX significantly

### 1.4 Trouble Brewing Guidance Data (8 critical lint issues)

All 8 critical guidance lint issues are in Trouble Brewing:
- 6 steps have `loopBackToStep` set but `loopCount` missing — will cause infinite loops or undefined behavior
- 1 step has `useItemOnObject=true` but no `objectId` — overlay can't render

**Complexity:** S | **Priority:** P1 — Data bug, could cause runtime issues

### 1.5 NPC ID Mismatches (2 sources)

Validation flagged:
- **Alchemical Hydra:** source npcId (8615) != kill step completionNpcId (8621) — expected, Hydra transforms through phases
- **Kalphite Queen:** source npcId (963) != kill step completionNpcId (965) — expected, KQ has 2 forms

These are intentional (multi-phase bosses) but the validator doesn't know that. Consider adding a `multiPhaseNpcIds` field or suppressing these warnings.

**Complexity:** S | **Priority:** P3 — Cosmetic/validator issue, no runtime impact

### 1.6 Category Taxonomy Gap

The `OTHER` category contains 120 sources (53% of all sources), acting as a catch-all for:
- Slayer creatures (45 sources) — should be `SLAYER`
- Skilling activities (12 sources) — should be `SKILLING`
- Boat Combat (6 sources) — could be `SAILING`
- Salvage (8 sources) — could be `SAILING`
- Miscellaneous (49 sources) — legitimately OTHER

The memory/docs reference SLAYER and SKILLING categories, but they don't exist in the actual data. This affects Category Focus mode filtering.

**Complexity:** M | **Priority:** P2 — Improves Category Focus mode and panel filtering

---

## 2. Codebase Feature Gaps

### 2.1 Panel UI Improvements

#### 2.1a Sort Options in Efficient Mode
**Gap:** Efficient mode only sorts by efficiency score. No option to sort by kill time, drop rate, category, or completion percentage.
**Fix:** Add dropdown selector in panel header for sort key. Data already available in `EfficiencyCalculator`.
**Complexity:** S | **Priority:** P2

#### 2.1b Statistics Dashboard
**Gap:** No summary view showing category completion breakdown, estimated total time remaining, or progress over time.
**Fix:** Add new `STATISTICS` mode to the 5 existing panel modes. Export data from `EfficiencyCalculator.rankByEfficiency()`.
**Complexity:** M | **Priority:** P2

#### 2.1c Favorites/Bookmarks
**Gap:** No way to pin sources for quick access when alternating between grinds.
**Fix:** Add star/bookmark toggle per source row, persisted via ConfigManager. New "Favorites" filter or mode.
**Complexity:** M | **Priority:** P3

#### 2.1d Requirements Tooltip
**Gap:** Locked sources show greyed out but don't explain which quest/skill is missing.
**Fix:** Enhance `setToolTipText()` on source rows with detailed requirement info from `RequirementsChecker`.
**Complexity:** S | **Priority:** P2

### 2.2 Overlay Improvements

#### 2.2a Animated/Pulsing Overlays
**Gap:** All overlays are static. No visual urgency cues for time-sensitive steps.
**Fix:** Add tick-based pulse/glow cycle to highlighted objects/NPCs. Use `client.getGameCycle()` for animation timing.
**Complexity:** S | **Priority:** P3

#### 2.2b Path Highlighting on Ground
**Gap:** World map shows route line but no ground-level path tiles.
**Fix:** If Shortest Path plugin is active, could request path data and render tile highlights along the route. Heavy overlap with Shortest Path plugin's own rendering.
**Complexity:** L | **Priority:** P3 — Shortest Path already handles this

### 2.3 New Completion Conditions

#### 2.3a WIDGET_VISIBLE Condition
**Gap:** Already defined in `CompletionCondition.java` but only used in 0 guidance steps currently. Could auto-complete clue reward screens, shop interfaces, minigame reward interfaces.
**Fix:** Wire up `WIDGET_VISIBLE` handler in `GuidanceSequencer` and add to clue/minigame sources.
**Complexity:** S | **Priority:** P2

#### 2.3b Composite Conditions (ANY_OF / ALL_OF)
**Gap:** Steps can only have one completion condition. Some steps would benefit from "arrive at tile OR interact with object" logic.
**Fix:** Add `ANY_OF` and `ALL_OF` wrappers to `CompletionCondition` with a `subconditions` array in guidance step data.
**Complexity:** L | **Priority:** P3

### 2.4 Plugin Integrations

#### 2.4a Loot Tracker Integration
**Gap:** No sync with Loot Tracker's recorded drops. Players get instant loot notifications from Loot Tracker but collection log helper only knows about items after the clog is opened.
**Fix:** Listen for `PluginMessage("loottracker", ...)` events. Pattern already exists for Shortest Path integration.
**Complexity:** M | **Priority:** P3 — Would be nice but clog sync already works

#### 2.4b Quest Helper Awareness
**Gap:** No awareness of quest progress. Can't suggest "do this quest to unlock this source" in a structured way.
**Fix:** Query quest state via Quest Helper plugin messages or varbits directly.
**Complexity:** M | **Priority:** P3

### 2.5 Config Additions

#### 2.5a Per-Overlay Colors
**Gap:** Single `overlayColor` applies to all overlay types (NPC, object, tile, item). Users may want different colors for different overlay types for visual clarity.
**Fix:** Add `npcHighlightColor`, `objectHighlightColor`, `tileHighlightColor` config options with fallback to main color.
**Complexity:** S | **Priority:** P3

#### 2.5b Notification Granularity
**Gap:** No fine-grained control over notification types (step completion, sequence completion, drop detected).
**Fix:** Add boolean config options for each notification type.
**Complexity:** S | **Priority:** P3

---

## 3. Recent OSRS Game Updates (March-April 2026)

### 3.1 Demonic Brutus (March 4 — Cow Boss Update)

**What:** Hard mode variant of Brutus added. Combat Level 1,224, 750 HP. Requires Desert Treasure II completion.
**Drops:** Brutus slippers (always, 100%), Beef pet at improved ~1/400 rate.
**Current state:** Regular Brutus is tracked. Demonic Brutus is NOT tracked.
**Action needed:** Verify whether Brutus slippers appear in the collection log as a separate item. If yes, add Demonic Brutus as a mutually exclusive source with regular Brutus.

**Complexity:** S | **Priority:** P2

### 3.2 Veiled Kraken (March 18 — Boat Combat Fixes)

**What:** New sea creature (Combat Level 210, 225 HP) in Southern Expanse. Requires Level 80 Sailing.
**Drops:** Dragon cannon barrel (1/1,000), Dragon metal sheet (~1/1,150), Dragon keel parts (~1/1,850), Inky paint (~1/1,900), Large dragon keel parts (~1/2,100).
**Current state:** NOT tracked as a standalone source. Its drops overlap with existing Sailing sources.
**Action needed:** Check TempleOSRS collection log categories to see if Veiled Kraken has its own clog section or if kills count under existing Sailing categories.

**Complexity:** S | **Priority:** P2

### 3.3 No Other Impact (March-April 2026)

- **March 11 (Getting Around Poll):** QoL only, no drop changes
- **March 25 (Easter 2026):** Temporary event, pet "Archibald" not permanent
- **April 1 (Getting Around Changes):** QoL transport improvements, no drops
- **Wilderness boss spawn changes:** Mechanical only (fewer adds), no drop table changes
- **Cannonball thieving rates changed:** Not collection log relevant

### 3.4 Existing Data Verified Correct

- Brutus regular drop rates match wiki exactly
- Shellbane Gryphon Belle's Folly rate already updated to 1/256
- No other tracked sources had rate changes in this period

---

## 4. Summary by Priority

### P1 — Fix Now (Data Bugs)

| Issue | Category | Complexity |
|-------|----------|-----------|
| Trouble Brewing guidance data (8 lint errors) | Data | S |

### P2 — High Value (UX Improvements)

| Issue | Category | Complexity |
|-------|----------|-----------|
| Auto-complete MANUAL final steps (boat combat, clues, Fight Caves/Inferno) | Data | M |
| Add ironKillTimeSeconds for 13 BOSSES/RAIDS | Data | S |
| Add SLAYER/SKILLING categories to replace OTHER catch-all | Data + Code | M |
| Demonic Brutus — verify clog entry, add if needed | Data | S |
| Veiled Kraken — verify clog entry, add if needed | Data | S |
| Sort options in Efficient mode | Code | S |
| Statistics dashboard mode | Code | M |
| Requirements tooltip on locked sources | Code | S |
| Wire up WIDGET_VISIBLE for clue/minigame auto-completion | Code | S |

### P3 — Nice to Have

| Issue | Category | Complexity |
|-------|----------|-----------|
| Add npcId to ~15 eligible OTHER sources | Data | S |
| Favorites/bookmarks system | Code | M |
| Animated/pulsing overlays | Code | S |
| Per-overlay-type colors | Code | S |
| Notification granularity config | Code | S |
| Composite completion conditions (ANY_OF/ALL_OF) | Code | L |
| Loot Tracker plugin integration | Code | M |
| Quest Helper plugin integration | Code | M |
| Path highlighting on ground tiles | Code | L |
| NPC ID mismatch validator suppression | Code | S |

---

## 5. Recommended Next Steps

1. **Immediate:** Fix Trouble Brewing guidance data (P1, 15 minutes)
2. **Short term:** Add ironKillTimeSeconds to 13 sources, verify Demonic Brutus + Veiled Kraken clog status
3. **Medium term:** Implement SLAYER/SKILLING categories, add sort options and stats dashboard
4. **Long term:** Upgrade MANUAL final steps to auto-completing conditions for boat combat, clues, and minigames
