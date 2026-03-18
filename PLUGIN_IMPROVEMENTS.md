# Collection Log Helper — Plugin Improvement Analysis

> Captured from live RuneLite session on March 18, 2026 (Hanadji @ Trouble Brewing, 700/1699 slots)

---

## Visual / UI Observations from Screenshots

**What's working well:**
- The "Top Pick" recommendation card is prominent and scannable
- "Guide Me" / "Map Guide" / "Open Wiki" buttons are discoverable
- Trouble Brewing source data loads correctly with Pieces of Eight tracking
- Collection Log progress (700/1699, 41.2%) displays in title bar
- Right sidebar panel renders with readable text at RuneLite's default width

**Issues spotted in session:**
- The sidebar panel is fairly text-dense when fully expanded — at RuneLite's default panel width (~225px), line wrapping on source names like "Travel to Trouble Brewing on Mos Le'Harmless" causes visual clutter
- No obvious visual separation between the "Top Pick" card and the rest of the ranked list below it
- The progress counter in the title bar (700/1699) is easy to miss — could benefit from a progress bar at the top of the panel

---

## Architecture & Code Improvements

### 1. Panel Rebuild Performance (High Impact)

`CollectionLogHelperPanel.rebuild()` reconstructs the entire Swing component tree on every update. With 192 sources and complex scoring, this causes a visible flicker when the list refreshes (e.g., after obtaining a new item or switching modes).

**Recommendation:** Cache `ScoredItem` rankings and only rebuild when data actually changes (new item obtained, level up, config change). Add a dirty flag:

```java
private boolean rankingsDirty = true;
private List<ScoredItem> cachedRankings;

public void markDirty() { rankingsDirty = true; }

private List<ScoredItem> getRankings() {
    if (rankingsDirty || cachedRankings == null) {
        cachedRankings = calculator.scoreAllSources(...);
        rankingsDirty = false;
    }
    return cachedRankings;
}
```

Also preserve scroll position across rebuilds — currently it resets to top.

### 2. Efficiency Calculator Refactor (Maintainability)

`EfficiencyCalculator.scoreSource()` is 150+ lines with nested probability logic handling standard drops, independent drops, multi-roll corrections, guaranteed items, shop economics, and raid scaling all in one method.

**Recommendation:** Extract into focused calculators:
- `DropProbabilityCalculator` — handles `1 - Π(1 - p_i)` math for standard/independent tables
- `ShopEconomicsCalculator` — handles points-per-hour / milestone scoring
- `RaidScalingCalculator` — team size adjustments

This also improves testability — you can unit test probability math in isolation.

### 3. Guidance Sequencer Thread Safety (Reliability)

The `GuidanceSequencer` uses multiple `volatile` fields (`activeSource`, `steps`, `currentIndex`, `bankRouting`) that can change independently between game ticks.

**Recommendation:** Wrap state in an immutable snapshot:

```java
@Value
public class SequenceState {
    CollectionLogSource source;
    List<GuidanceStep> steps;
    int currentIndex;
    boolean bankRouting;
}

// Atomic swap on game thread:
private volatile SequenceState state;
```

This eliminates the risk of reading `currentIndex` from one tick and `steps` from another.

### 4. Mode State Preservation (UX Polish)

Switching between Efficient → Category Focus → Search rebuilds from scratch each time. Scroll position and expanded/collapsed categories are lost.

**Recommendation:** Store per-mode UI state:
- Scroll position per mode
- Expanded category set for Category Focus mode
- Last search query for Search mode
- Selected category filter

### 5. Slayer Strategy: Ranked Master List (Feature)

Currently recommends one "best" Slayer master. Power users want to compare masters.

**Recommendation:** Show top 2-3 masters ranked by useful-task probability:
```
Nieve: 1 useful task every 1.2 assignments
Konar: 1 useful task every 1.8 assignments
Duradel: 1 useful task every 2.1 assignments
```

### 6. Clue Bucket Cache Invalidation (Bug Prevention)

`ClueCompletionEstimator` caches the progression bucket (Early/Mid/Late/Maxed) and only calls `resetBucket()` on logout. If you level up mid-session (e.g., 85→86 Construction changes Mahogany Homes scoring), the bucket becomes stale.

**Recommendation:** Add skill change listeners to invalidate the cache:
```java
@Subscribe
public void onStatChanged(StatChanged event) {
    clueEstimator.resetBucket();
    panel.markDirty();
}
```

### 7. Hard-Coded Coordinates (Code Quality)

GE bank coordinates (3164, 3489, 0) and other magic numbers are scattered across `GuidanceSequencer`, overlay classes, and data files.

**Recommendation:** Centralize in a `GameLocations` constants class:
```java
public final class GameLocations {
    public static final WorldPoint GE_BANK = new WorldPoint(3164, 3489, 0);
    public static final int DEFAULT_COMPLETION_DISTANCE = 5;
    // ...
}
```

### 8. Data Sync Granularity (Feature)

`DataSyncState` tracks a single NOT_SYNCED → SYNCING → SYNCED lifecycle. It doesn't distinguish between collection log data, bank state, and inventory state.

**Recommendation:** Track sync status per data type so the panel can show "Collection log synced, bank data stale" — especially useful after death or long sessions where bank contents change.

---

## Visual / UX Improvement Ideas

### 9. Top Pick Card Visual Separation

The #1 recommendation ("Top Pick") blends into the rest of the list. A subtle background color or border would make it pop:
- Light gold background tint for the top pick row
- Or a thin gold left-border (similar to how Slayer task items get a purple border)

### 10. Panel Header Progress Bar

Add a thin progress bar at the top of the panel showing overall completion (700/1699). This gives instant visual feedback and a sense of momentum. The bar already exists conceptually in the title — making it graphical would be a nice touch.

### 11. Source Name Truncation

Long source names like "Opening chests with shades of mort'ton keys" wrap awkwardly at 225px panel width. Consider:
- Truncating with ellipsis at ~30 chars
- Showing full name on hover tooltip (already partially done)
- Or using a two-line layout: source name on line 1, drop rate + score on line 2

### 12. Proximity Mode Polish

The experimental Proximity mode could show distance in tiles next to each source, e.g.:
```
Barrows (42 tiles away) — Score: 8.3
```
This would make the "efficient by proximity" concept more intuitive.

---

## Data / Scoring Refinements

### 13. Spreadsheet Alignment Check

The Log Adviser spreadsheet (v1.3.4, by Main Mukkor) shows 697/1692 slots while your plugin shows 700/1699. The ~7 item difference likely comes from newly added items (Sailing/Varlamore content). Consider:
- Documenting which items differ between your data and the community spreadsheet
- Adding a data version field to `drop_rates.json` for tracking drift

### 14. AFK Level Filtering in UI

The `afkLevel` field exists in the data schema but doesn't appear to have a UI filter. Adding an "AFK-friendly" toggle or filter would help players who want low-effort grinds (e.g., Tempoross, Wintertodt) vs. high-attention bosses.

### 15. Kill Time Source Attribution

Kill times are aligned with TempleOSRS EHB rates. Consider showing the source in item detail view: "Kill time: 62s (TempleOSRS EHB)" — this builds trust with the Log Hunters community who cross-reference these numbers.

---

## Priority Summary

| # | Improvement | Impact | Effort |
|---|------------|--------|--------|
| 1 | Panel rebuild caching | High (UX smoothness) | Medium |
| 2 | Efficiency calculator refactor | Medium (maintainability) | Medium |
| 3 | Guidance thread safety | Medium (reliability) | Low |
| 4 | Mode state preservation | Medium (UX) | Low |
| 5 | Ranked Slayer masters | Medium (feature) | Low |
| 6 | Clue bucket cache fix | Low (bug prevention) | Low |
| 7 | Centralize constants | Low (code quality) | Low |
| 8 | Granular data sync | Low (feature) | Medium |
| 9 | Top Pick visual card | Medium (UX) | Low |
| 10 | Progress bar header | Medium (UX) | Low |
| 11 | Source name truncation | Low (UX) | Low |
| 12 | Proximity distance display | Low (feature) | Low |
| 13 | Spreadsheet alignment | Low (data quality) | Low |
| 14 | AFK filter UI | Medium (feature) | Low |
| 15 | Kill time attribution | Low (trust) | Low |
