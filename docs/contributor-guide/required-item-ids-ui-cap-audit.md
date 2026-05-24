# requiredItemIds UI rendering audit

> Triggered by the PR #639 review finding that the Whisperer (Awakened) bank
> step declares 7 entries in `requiredItemIds`. Reviewers asked whether the
> plugin enforces a documented cap on this field (analogous to the
> `recommendedItemIds <= 6` test guard).

## 1. Is there an enforced cap?

**No.** A repository-wide grep for any test, constant, or runtime check that
bounds `requiredItemIds.size()` returns zero hits.

The only adjacent guard is on the *recommended* list:

- `src/test/java/com/collectionloghelper/data/DropRateDatabaseTest.java:487`
  `load_recommendedItemIds_whenPresent_areValidAndBounded` asserts
  `recommended.size() <= 6` for every step in `drop_rates.json`.

No equivalent assertion exists on `requiredItemIds`. There is no
`MAX_REQUIRED_ITEM_IDS` constant in `src/main/java`, no schema-side bound,
and no `guidance_lint` rule that flags long lists.

## 2. UI rendering behavior

`requiredItemIds` is rendered by
`src/main/java/com/collectionloghelper/ui/widget/RequiredItemsChipPanel.java`.

Layout summary (see `RequiredItemsChipPanel.java:89-103` and `:118-152`):

- Outer panel: `BoxLayout.Y_AXIS`.
- Inner `chipRow` panel: **`BoxLayout.X_AXIS`** -- a single horizontal row,
  no wrapping, no scrolling, no grid.
- Each chip is a fixed `CHIP_SIZE = ICON_SIZE (28) + 2 * CHIP_BORDER (2) = 32px`
  square (`RequiredItemsChipPanel.java:74-80`).
- Chips are separated by a 3px horizontal strut
  (`RequiredItemsChipPanel.java:140`).
- The strip is prefixed by a `"Items needed:"` heading label plus a 6px
  strut (`RequiredItemsChipPanel.java:129-134`).

The recommended-items strip (`RecommendedItemsChipPanel.java`) uses the
same single-row `BoxLayout.X_AXIS` shape, which is why the team capped
that one at 6.

The plugin hosts these strips inside a standard RuneLite `PluginPanel`
(`CollectionLogHelperPanel.java:83-86`), whose published width is **225px**
(`net.runelite.client.ui.PluginPanel.PANEL_WIDTH`).

Approximate horizontal budget for chips on a 225px panel:

- Heading `"Items needed:"` plus 6px strut: ~70px consumed.
- Remaining: ~155px.
- Each chip + strut: ~35px.
- Fit: roughly **4 chips before clipping**, ~5 if RuneLite font metrics are
  slightly narrower, **definitely overflow at 6 and 7**.

Because the row is `BoxLayout.X_AXIS` with no wrap and the parent has no
`JScrollPane`, chips that overflow the panel width are simply clipped on
the right edge -- they are not wrapped to a new line and not scrollable.

## 3. Verdict

**Soft cap -- UI looks bad at ~5+ entries; no enforced cap exists.**

- The Whisperer (Awakened) bank step (7 entries) flagged in #639 is real
  visual overflow, not just a stylistic concern.
- Nine other sources already sit at 6 entries and will exhibit the same
  clipping today.
- Nine more sit at 5 entries and may or may not clip depending on the
  host system's font metrics.
- There is no test guard or runtime constant to lean on; the cap is purely
  visual.

A normative `requiredItemIds <= N` guideline (with `N = 5` as the safe
ceiling, matching the recommended-list discipline of `<= 6`) and a matching
JUnit guard would convert this from "reviewer-spots-it-sometimes" to
"CI-fails-the-PR." Without that guard, every future bank-step authoring PR
will rediscover the same finding.

## 4. Recommended action

Two follow-ups, both filed separately from this audit:

1. **Add a JUnit guard** in `DropRateDatabaseTest` mirroring the existing
   `recommendedItemIds <= 6` assertion, with the threshold set at the
   number this audit settles on (recommend `<= 6` to match recommended,
   or `<= 5` if we want a stricter visual budget).

2. **Trim the existing offenders** listed in section 5. Each trim is a
   data-only edit to `drop_rates.json` -- pick the least-essential items
   on the step (typically alternative teleports already covered by
   `alternatives`, or duplicates of items that are equippable elsewhere
   in the kit). Cross-link the tracking issue from each trim PR.

3. **Update `docs/contributor-guide/deep-guidance-bar.md` Element 6**
   ("Bank-and-return detection") with a one-line note that
   `requiredItemIds` should be kept to roughly 5 entries for visual
   reasons, with a forward reference to the eventual test guard.

A tracking GitHub issue with the offenders list is filed alongside this
audit so the work has an owner.

## 5. Top-10 offenders today

Counts are the maximum `requiredItemIds.length` per source across all
`guidanceSteps`. Source of truth:
`src/main/resources/com/collectionloghelper/drop_rates.json`.

| Source | Max `requiredItemIds` length | Step index |
| ------ | ---------------------------- | ---------- |
| The Whisperer (Awakened) | 7 | 0 |
| Tormented Demons | 6 | 0 |
| The Leviathan (Awakened) | 6 | 0 |
| The Inferno | 6 | 0 |
| The Fight Caves | 6 | 0 |
| Skeletal Wyvern | 6 | 0 |
| Mahogany Homes | 6 | 0 |
| Hydra | 6 | 0 |
| Guardians of the Rift | 6 | 0 |
| Drake | 6 | 0 |

**Distribution across all 84 steps that declare `requiredItemIds`:**

| Length | Step count |
| ------ | ---------- |
| 7 | 1 |
| 6 | 9 |
| 5 | 9 |
| 4 | 8 |
| 3 | 9 |
| 2 | 11 |
| 1 | 37 |

So **10 steps already exceed the safe 5-chip visual budget**, and
**19 are at or above it**. The Whisperer finding is the worst case,
not the only case.

## Appendix -- reproduction

To regenerate the offender list against the current `drop_rates.json`:

```python
import json
from collections import Counter

with open('src/main/resources/com/collectionloghelper/drop_rates.json') as f:
    data = json.load(f)

def walk(node):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == 'requiredItemIds' and isinstance(v, list) and len(v) > 0:
                yield len(v)
            yield from walk(v)
    elif isinstance(node, list):
        for item in node:
            yield from walk(item)

rows = []
for src in data:
    name = src.get('name', '?')
    m = 0
    for L in walk(src):
        m = max(m, L)
    if m > 0:
        rows.append((m, name))

rows.sort(reverse=True)
for r in rows[:15]:
    print(f'{r[0]:>2} {r[1]}')
```
