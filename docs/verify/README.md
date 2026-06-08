# Data-verification findings (consolidated)

Per-category verification of the auto-grabbed `drop_rates.json` data. Each category was
verified independently (separate file so parallel sessions don't conflict); this folder is
the consolidated record. **Verification only - no `drop_rates.json` edits.** Fixes are a
separate, reviewed step gated by `validate_drop_rates --check cache_ids`.

## Method
- **IDs:** `validate_drop_rates --check cache_ids` - the abextm game cache is the sole id
  authority. Result across all 225 sources: **0 wrong/missing ids.** (Name-mismatch warnings
  are display-string divergences with correct ids, not bugs.)
- **Semantics:** requirements, fairy-ring codes, arrival coords, travel direction - each
  finding cited and passed through the `domain-skeptic` agent; only `STANDS` recorded.

## Actionable fix backlog (the real bugs - all `STANDS`)
The auto-grab's failure mode is **fabricated requirements** and **copy/paste-wrong items**,
not ids. Highest-value fixes:

- **BOSSES** (`findings-BOSSES.md`): 8 Desert Treasure II entries carry **fabricated `skills`
  requirements** (e.g. Duke Sucellus `MINING 65`); Araxxor fairy-ring `CLS` is wrong.
- **OTHER** (`findings-OTHER.md`): Creature Creation **wrong recipe item ids** (Newtroost uses
  Raw chicken `2138` -> Feather `314`; Grimy torstol `219` -> Eye of newt `221`), plus
  cross-creature copy/paste ingredients.
- **MINIGAMES** (`findings-MINIGAMES.md`): Brimhaven Agility Arena **fabricated `AGILITY 40`**
  gate; 3 wrong fairy-ring codes.
- **SLAYER** (`findings-SLAYER.md`): Terror Dog wrong fairy-ring code + bearing; 2 low.
- **RAIDS** (`findings-RAIDS.md`): ToB waypoint **wrong quest gates** (PRIEST_IN_PERIL,
  A_TASTE_OF_HOPE); Scythe/Sang display names missing "(uncharged)" (ids correct, cosmetic).
- **SKILLING** (`findings-SKILLING.md`): see file.

## Not a fix backlog
The `cache_ids` name-mismatch list (24 after suffix/word-order tuning) is mostly display-name
divergence with **correct ids** (Chompy hat rank names, `lock`/`locks` plurals, `glory (t)` vs
`(t4)`); optional cosmetic cleanup, not data bugs.
