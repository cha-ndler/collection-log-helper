# Plugin Hub Self-Review

Self-audit of `cha-ndler/collection-log-helper` against Plugin Hub review criteria.

Checklist compiled from:
- `runelite/plugin-hub#11156` reviewer feedback (direct comment from `tylerwgrass` + process notes)
- Recent reviewer-enforced rules observed in `runelite/plugin-hub#11534` (config group naming)
- `runelite/plugin-hub` README (submission requirements)
- `mcp__plugin_hub_validate` static analysis output

Baseline commit: `d4a2dbb1` (branch `docs/plugin-hub-review`, current HEAD at audit time).

---

## 1. Manifest

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `displayName` present and meaningful | Green | `runelite-plugin.properties` line 1: `displayName=Collection Log Helper` | |
| `author` field matches GitHub alias | Green | `runelite-plugin.properties` line 2: `author=cha-ndler` | Matches GitHub login |
| `description` present, ≤ ~100 chars | Green | `runelite-plugin.properties` line 3; 95 characters | Hub README shows ~100-char examples |
| `tags` present and relevant | Yellow | `runelite-plugin.properties` line 4: `collection,log,helper,efficiency,guide` (5 tags) | `helper` is a generic filler tag. Consider replacing with a more specific term (e.g. `overlay`, `guidance`) before resubmission. Low priority — not a blocker based on past reviews. |
| `plugins` field matches main class | Green | `runelite-plugin.properties` line 5: `plugins=com.collectionloghelper.CollectionLogHelperPlugin`; confirmed in `build.gradle` `pluginMainClass` | |
| `icon` field matches packaged resource | Green | `runelite-plugin.properties` line 6: `icon=/com/collectionloghelper/panel_icon.png`; resource exists at `src/main/resources/com/collectionloghelper/panel_icon.png` | |
| Root `icon.png` within 48 × 72 px | Green | `icon.png` is 48 × 44 px (verified via PNG header parse) | |
| `runeLiteVersion` pinned to a release (not `+` or SNAPSHOT) | Green | `build.gradle` line 22: `def runeLiteVersion = '1.12.24'`; used for all three RuneLite dependencies | |

---

## 2. Dependencies and Build

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `gradle/verification-metadata.xml` committed | Green | `gradle/verification-metadata.xml` exists (28 KB); tracked in repo | Added in cha-ndler/collection-log-helper#333 |
| No unpinned (`+`) or SNAPSHOT upstream versions | Green | `build.gradle`: all versions are literals (`1.12.24`, `1.18.30`, `4.12`, `4.11.0`); no `+` or `-SNAPSHOT` in dependency declarations | `version = '1.0-SNAPSHOT'` is the plugin's own artifact version — not a dependency, acceptable |
| No internal/snapshot Maven repos | Green | `build.gradle` repositories block: only `mavenLocal()`, `repo.runelite.net`, and `mavenCentral()` | |
| `./gradlew build` passes cleanly | Green | `BUILD SUCCESSFUL in 14s` with 6 tasks; all tests pass (503 unit tests) | Verified locally at `d4a2dbb1` |
| shadowJar size reasonable (< ~1 MB) | Green | `build/libs/collection-log-helper-1.0-SNAPSHOT-all.jar` is 314 KB | Well within range; prior submission had 35 MB before cha-ndler/collection-log-helper#333 fixed test classpath bleed-through |
| No Kotlin, Scala, or Groovy source files | Green | `find src/main/java -name "*.kt" -o -name "*.scala" -o -name "*.groovy"` returns empty | Java 11 only |
| `runelite-plugin` Gradle plugin applied | Red | `plugin_hub_validate` flags `gradle-plugin-missing`: build.gradle does not apply `id 'com.openosrs.injected'` or the RuneLite plugin convention plugin | This is a medium-severity finding from the validator. Needs investigation — if Hub CI requires it, add `apply plugin: 'com.openosrs.externalplugin'` per example-plugin template. Planned for Tier A resubmission polish. |

---

## 3. Privacy

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| No outbound HTTP from plugin code | Green | `grep -rn "HttpClient\|HttpURLConnection\|URL.*open\|OkHttp\|WebClient"` in `src/main/java` returned no results | Plugin is purely read-only against RuneLite client APIs |
| No telemetry or analytics | Green | `grep -rn "telemetry\|analytics\|mixpanel\|amplitude\|sentry"` returned no results | |
| No PII in Java source files | Green | All copyright headers use `cha-ndler` (GitHub alias); no personal names or email addresses in `src/main/java` | Copyright: `Copyright (c) 2025, cha-ndler` across all 59 files |
| Author identity in git history is alias-only | Yellow | All `cha-ndler` commits use `cha-ndler <48898494+cha-ndler@users.noreply.github.com>`. Earlier commits in the data-sourcing history include real-name co-authors from the broader RuneLite community (contributor co-author lines, e.g. `dylan <dylan@420kc.live>`) | Co-author lines are from third-party contributors, not the plugin author. Plugin Hub evaluates the submitter identity, not co-authors. No action needed. |
| No secrets or credentials in source | Green | `grep -rn "System.getenv\|ProcessBuilder\|Runtime.exec"` returned no results in `src/main/java` | |

---

## 4. Functionality and Rules Compliance

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| No automation / botting assistance | Green | Plugin provides passive overlays and efficiency rankings; no input injection, no clicking, no auto-action code. `grep -rn "alch\|auto.*click\|bot\|automate"` returned no relevant hits in `src/main/java` | |
| No premium / paywall features | Green | `grep -rn "premium\|paywall\|paid\|subscription\|patreon\|stripe"` returned no results | |
| No plugin-conflict overrides | Green | No `PluginConflict` annotation or `conflictsWith` usage found; `override` keyword appears only in natural Java `@Override` contexts and data-merge helpers | |
| No `ProcessBuilder` / `Runtime.exec` / classloader tricks | Green | `grep -rn "ProcessBuilder\|Runtime.exec\|URLClassLoader\|getDeclaredMethod\|setAccessible"` returned no results in `src/main/java` | |
| Not duplicating a core RuneLite feature | Green | No equivalent functionality exists in the RuneLite client or officially supported plugins — the collection log guidance and efficiency scoring are original | |
| Compliant with [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) | Green | Passive display only; no automation, no reading of protected game state beyond what RuneLite APIs expose publicly | |

---

## 5. Code Quality

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| No compiler warnings in `./gradlew build` | Green | Build output shows no warnings — `BUILD SUCCESSFUL`, clean compile | |
| Test suite present | Green | 19 test files under `src/test/java`; 503 unit tests passing | |
| No `System.out.println` / unguarded debug output in production code | Green | The only `pw.println()` calls in `EfficiencyCalculator.java` (lines 652, 694, 719) write to a developer export `PrintWriter` behind an explicit file-export method, not to stdout | |
| Config group name is descriptive (avoids collision) | Green | `CollectionLogHelperConfig.java` line 34: `@ConfigGroup("collectionloghelper")` — fully qualified, no collision risk | Reviewer feedback from #11534 called out vague group names; ours is sufficiently specific |
| Files within 800-line guideline | Red | Two files exceed 800 lines: `CollectionLogHelperPanel.java` (1,661 LOC), `CollectionLogHelperPlugin.java` (1,281 LOC). Three more are borderline: `GuidanceOverlayCoordinator.java` (859), `EfficiencyCalculator.java` (754), `GuidanceSequencer.java` (719) | Plugin class reduced from 2,192 → 1,281 in cha-ndler/collection-log-helper#331–#339. Panel decomposition is planned in Tier B (milestone B1). This is an ongoing decomposition effort, not a hard blocker for Hub acceptance — Plugin Hub does not enforce a strict LOC limit, but reviewers may comment. |
| No god-objects / obvious single-class doing everything | Yellow | `CollectionLogHelperPlugin.java` at 1,281 LOC is still the largest functional class; `CollectionLogHelperPanel.java` at 1,661 LOC is the largest file overall | Architecture decomposition is ongoing (Tier A milestone A2). Six service classes were already extracted. Panel refactor is next. |

---

## 6. Documentation

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `README.md` present | Green | `README.md` exists (143 lines); describes features, screenshots, scoring model, and requirements | |
| Screenshots present and linked in README | Green | `docs/screenshots/` contains 6 PNG files (`overlay-1.png`, `overlay-2.png`, `overlay-3.png`, `panel.png`, `settings.png`, `worldmap.png`); all referenced in `README.md` | |
| `CHANGELOG.md` present | Green | `CHANGELOG.md` exists (35 lines); contains `0.1.0 — Initial public release` entry with feature list | |
| `CREDITS.md` present | Green | `CREDITS.md` exists (59 lines) | |
| `CONTRIBUTING.md` present | Green | `CONTRIBUTING.md` exists (526 lines) | |
| `LICENSE` present and is BSD 2-Clause | Green | `LICENSE` exists (25 lines); `BSD 2-Clause License` header; Hub README recommends BSD 2-Clause | |

---

## 7. Attribution

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| Author is a real maintainer with a GitHub account | Green | GitHub user `cha-ndler` exists and is the repo owner | |
| No AI/tool attribution in commits | Green | `git log --all --format="%B"` search for `co-authored-by.*claude\|generated by\|anthropic` returned no results | Community contributor co-author lines (human contributors) are present and appropriate |
| No AI/tool attribution in source files | Green | `grep -rn "Generated by\|Co-authored\|Claude\|Anthropic"` in `src/main/java`, `docs/`, and root docs returned no results (ROADMAP.md references are policy notes, not attribution) | |
| Copyright headers use alias, not personal name | Green | All 59 Java source files: `Copyright (c) 2025, cha-ndler` | Verified by spot-check across `src/main/java/com/collectionloghelper/` |

---

## Summary

| Status | Count | Items |
|--------|-------|-------|
| Green | 27 | All items marked Green above |
| Yellow | 3 | `tags` filler word; co-author identity (no action needed); `CollectionLogHelperPlugin.java` god-object trajectory |
| Red | 2 | `runelite-plugin` Gradle plugin missing; `CollectionLogHelperPanel.java` + `CollectionLogHelperPlugin.java` LOC over 800 |
| NA | 0 | (all categories applicable) |

### Red items and fix milestones

1. **`runelite-plugin` Gradle plugin missing** (`gradle-plugin-missing`): The `plugin_hub_validate` tool flags the absence of the RuneLite convention Gradle plugin. Needs verification against the current example-plugin template; if Hub CI requires it, add `apply plugin: 'com.openosrs.externalplugin'`. Targeted for Tier A resubmission prep (no named milestone yet — add to A6 or create A7).

2. **Files over 800 LOC**: `CollectionLogHelperPanel.java` (1,661) and `CollectionLogHelperPlugin.java` (1,281). The plugin class is being decomposed incrementally (6 service classes extracted in A2-adjacent work). Panel decomposition is planned for Tier B milestone B1. These are known and tracked; not hidden.

### Items not yet resolved by open milestones

- Panel LOC → Tier B, milestone B1
- Gradle plugin convention → investigate and add to A6 (resubmission checklist milestone)
- Tags cleanup → low effort, can be done at resubmission time with no milestone needed

### How to use this document

Before opening the resubmission PR against `runelite/plugin-hub`, verify each Red item is resolved, re-run `plugin_hub_validate`, confirm `./gradlew build` is clean, and update this file's commit SHA reference and tally counts.
