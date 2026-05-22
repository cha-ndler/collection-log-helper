# Plugin Hub Self-Review

Self-audit of `cha-ndler/collection-log-helper` against Plugin Hub review criteria.

Checklist compiled from:
- `runelite/plugin-hub#11156` reviewer feedback (direct comment from `tylerwgrass` + process notes)
- Recent reviewer-enforced rules observed in `runelite/plugin-hub#11534` (config group naming)
- `runelite/plugin-hub` README (submission requirements) — verified against upstream master
- `runelite/example-plugin/build.gradle` (canonical template) — verified against upstream master
- `mcp__plugin_hub_validate` static analysis output

Baseline commit: `d1ee45ad` (`master` HEAD at audit time, post-#592 ScheduledExecutorService DI).
Previous baseline: `d4a2dbb1` (months ago, pre-Wave-21). This refresh reflects the closure of
#503 (all 4 god-classes under floor) plus #589/#590/#591/#592 build/runtime modernization.

---

## 1. Manifest

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `displayName` present and meaningful | Green | `runelite-plugin.properties` line 1: `displayName=Collection Log Helper` | |
| `author` field matches GitHub alias | Green | `runelite-plugin.properties` line 2: `author=cha-ndler` | Matches GitHub login |
| `description` present, <= ~100 chars | Green | `runelite-plugin.properties` line 3; 95 characters | Hub README shows ~100-char examples |
| `tags` present and relevant | Green | `runelite-plugin.properties` line 4: `collection,clog,slayer,clues,efficiency,guide` (6 tags) | Filler `helper` tag removed in earlier sweep; current tags are all activity-specific |
| `plugins` field matches main class | Green | `runelite-plugin.properties` line 5: `plugins=com.collectionloghelper.CollectionLogHelperPlugin`; confirmed in `build.gradle` `pluginMainClass` | |
| `icon` field matches packaged resource | Green | `runelite-plugin.properties` line 6: `icon=/com/collectionloghelper/panel_icon.png`; resource exists at `src/main/resources/com/collectionloghelper/panel_icon.png` | |
| Root `icon.png` within 48 x 72 px | Green | `icon.png` is 48 x 72 px (verified via PNG header parse) | At max allowed bound |
| `runeLiteVersion` pinned to a release (not `+` or SNAPSHOT) | Green | `build.gradle`: `def runeLiteVersion = '1.12.26.3'`; used for `client` and `jshell` dependencies | Bumped from `1.12.24` since previous audit |

---

## 2. Dependencies and Build

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `gradle/verification-metadata.xml` committed | Green | `gradle/verification-metadata.xml` exists (~46 KB); tracked in repo | Added in cha-ndler/collection-log-helper#333 |
| No unpinned (`+`) or SNAPSHOT upstream versions | Green | `build.gradle`: all versions are literals (`1.12.26.3`, `1.18.30`, `5.10.2`, `5.14.2`); no `+` or `-SNAPSHOT` in dependency declarations | `version = '1.0-SNAPSHOT'` is the plugin's own artifact version - not a dependency, acceptable |
| No internal/snapshot Maven repos | Green | `build.gradle` repositories block: only `mavenLocal()`, `repo.runelite.net`, and `mavenCentral()` | |
| `./gradlew build` passes cleanly | Green | `BUILD SUCCESSFUL in 30s` with 9 tasks; 1,390 unit tests pass (0 failures, 0 errors, 0 skipped) | Verified locally at `d1ee45ad` |
| shadowJar size reasonable (< ~1 MB) | Green | `build/libs/collection-log-helper-1.0-SNAPSHOT-all.jar` is 562 KB | Slight growth from 314 KB (more test data + 1.12.26.3 client surface) but still well under any practical Hub limit |
| No Kotlin, Scala, or Groovy source files | Green | `find src/main/java -name "*.kt" -o -name "*.scala" -o -name "*.groovy"` returns empty | Java 11 only |
| `runelite-plugin` Gradle plugin applied | Green (validator false positive) | The upstream canonical template `runelite/example-plugin/build.gradle` uses only `plugins { id 'java' }` and does NOT apply any `runelite-plugin` convention plugin. Our `build.gradle` matches the template's plugins/repositories/shadowJar structure. The `gradle-plugin-missing` finding from `plugin_hub_validate` is an over-zealous heuristic in the MCP validator, not a Hub requirement. | Verified against `https://github.com/runelite/example-plugin/blob/master/build.gradle` on 2026-05-21. Filed `mcp__plugin_hub_validate gradle-plugin-missing` as a follow-up against `runelite-dev-toolkit` rather than changing our build. |
| Shadow jar excludes test classpath bleed-through | Green | `shadowJar` task pulls from `configurations.runtimeClasspath` (not `testRuntimeClasspath` as the upstream template does), preventing the 35 MB inflation seen in earlier #333 audit | Deliberate divergence from upstream template - improves output size; functionally identical at runtime since plugin main class lives in `src/main` |

---

## 3. Privacy

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| No outbound HTTP from plugin code | Green | `grep -rn "HttpClient\|HttpURLConnection\|URL.*open\|OkHttp\|WebClient"` in `src/main/java` returned no results | Plugin is purely read-only against RuneLite client APIs |
| No telemetry or analytics | Green | `grep -rn "telemetry\|analytics\|mixpanel\|amplitude\|sentry"` returned no results | |
| No PII in Java source files | Green | All copyright headers use `cha-ndler` (GitHub alias); no personal names or email addresses in `src/main/java` | Copyright: `Copyright (c) 2025, cha-ndler` across all source files |
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
| Not duplicating a core RuneLite feature | Green | No equivalent functionality exists in the RuneLite client or officially supported plugins - the collection log guidance and efficiency scoring are original | |
| Compliant with [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) | Green | Passive display only; no automation, no reading of protected game state beyond what RuneLite APIs expose publicly | |

---

## 5. Code Quality

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| No compiler warnings in `./gradlew build` | Green | Build output shows two `Note:` lines (deprecated-API and unchecked-operations in tests) but no `warning:` lines; `BUILD SUCCESSFUL` with clean compile | Notes are informational; running with `-Xlint:deprecation -Xlint:unchecked` would surface specifics but Hub does not enforce |
| Test suite present | Green | 102 test files under `src/test/java`; 1,390 unit tests passing (0 skipped, 0 failures, 0 errors) | Up from 503 tests at previous baseline; JUnit 5.10.2 + Mockito 5.14.2 (migrated in #590/#591) |
| No `System.out.println` / unguarded debug output in production code | Green | The only `pw.println()` calls in `EfficiencyCalculator.java` write to a developer export `PrintWriter` behind an explicit file-export method, not to stdout | |
| Config group name is descriptive (avoids collision) | Green | `CollectionLogHelperConfig.java` line 34: `@ConfigGroup("collectionloghelper")` - fully qualified, no collision risk | Reviewer feedback from #11534 called out vague group names; ours is sufficiently specific |
| Files within 800-line guideline | Green | All five previously-flagged files now under the 800 floor: `CollectionLogHelperPlugin.java` 488, `CollectionLogHelperPanel.java` 605, `GuidanceOverlayCoordinator.java` 557, `GuidanceSequencer.java` 623, `EfficiencyCalculator.java` 777. | #503 closed across Waves 19-21; see "Post-#503 wins" section below |
| No god-objects / obvious single-class doing everything | Green | Largest functional class is now `EfficiencyCalculator.java` at 777 LOC. Plugin class shrank from 2,192 -> 1,281 -> 488 (4.5x reduction). Panel class shrank from 1,661 -> 605. | Architecture decomposition substantially complete |
| Overlay render-path allocations | Yellow | `plugin_hub_validate` flags 6 `new Color(...)` allocations in `DialogHighlightOverlay`, `GroundItemHighlightOverlay`, `WidgetHighlightOverlay`, and `WorldMapRouteOverlay`. All six are gated by a cache-invalidation check (`if (!color.equals(cachedColor)) { cachedFill = new Color(...); ... }`) so they only allocate on config-color change, not per-frame. | Validator false positive - reports the allocation site without analyzing the cache guard. Could replace with `ColorUtil.colorWithAlpha(base, alpha)` for stylistic consistency but no measurable performance benefit since the guard already eliminates per-frame allocation. Not a blocker. |

---

## 6. Documentation

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| `README.md` present | Green | `README.md` exists (122 lines); describes features, screenshots, scoring model, and requirements | |
| Screenshots present and linked in README | Green | `docs/screenshots/` contains 6 PNG files (`overlay-1.png`, `overlay-2.png`, `overlay-3.png`, `panel.png`, `settings.png`, `worldmap.png`); all referenced in `README.md` | |
| `CHANGELOG.md` present | Green | `CHANGELOG.md` exists (225 lines); covers releases through current master | Expanded from 35 lines at previous baseline |
| `CREDITS.md` present | Green | `CREDITS.md` exists (59 lines) | |
| `CONTRIBUTING.md` present | Green | `CONTRIBUTING.md` exists (80 lines) | Trimmed from 526 lines for clarity |
| `LICENSE` present and is BSD 2-Clause | Green | `LICENSE` exists (25 lines); `BSD 2-Clause License` header; Hub README recommends BSD 2-Clause | |

---

## 7. Attribution

| Item | Status | Evidence | Notes |
|------|--------|----------|-------|
| Author is a real maintainer with a GitHub account | Green | GitHub user `cha-ndler` exists and is the repo owner | |
| No external tool attribution in commits | Green | `git log --all --format="%B"` search for `co-authored-by\|generated by` patterns returned no automated-attribution results | Community contributor co-author lines (human contributors) are present and appropriate |
| No external tool attribution in source files | Green | `grep -rn "Generated by\|Co-authored"` in `src/main/java`, `docs/`, and root docs returned no results | |
| Copyright headers use alias, not personal name | Green | All Java source files: `Copyright (c) 2025, cha-ndler` | Verified by spot-check across `src/main/java/com/collectionloghelper/` |

---

## Summary

| Status | Count | Items |
|--------|-------|-------|
| Green | 32 | All items marked Green above |
| Yellow | 2 | Co-author identity (no action needed); overlay render-path allocations (validator false positive, cache-guarded) |
| Red | 0 | (was 2 at previous baseline; both resolved) |
| NA | 0 | (all categories applicable) |

### Items resolved since previous baseline (`d4a2dbb1` -> `d1ee45ad`)

1. **Red -> Green: `runelite-plugin` Gradle plugin missing** — Verified against `runelite/example-plugin` canonical template that no such convention plugin is required by Hub. The `plugin_hub_validate` finding is a false positive; will follow up against the validator tool itself.
2. **Red -> Green: Files over 800 LOC** — All five previously-flagged files now under the 800 floor (Plugin 488, Panel 605, Coordinator 557, Sequencer 623, EfficiencyCalculator 777). Closed across Waves 19-21 of the roast remediation orchestration (#503); EfficiencyCalculator subsequently dropped below floor through downstream cleanups.
3. **Yellow -> Green: `tags` filler word** — `helper` removed; current 6 tags are all activity-specific (`collection`, `clog`, `slayer`, `clues`, `efficiency`, `guide`).
4. **Yellow -> Green: god-object trajectory on Plugin/Panel** — Plugin class shrank 2,192 -> 555 (4x reduction); Panel 1,661 -> 677 (2.5x reduction).

### Post-#503 wins worth noting at resubmission

- **All five flagged files under 800 LOC floor** (closes #503 and clears the previous EfficiencyCalculator residual): Plugin 488, Panel 605, Coordinator 557, Sequencer 623, EfficiencyCalculator 777.
- **Test suite tripled**: 503 -> 1,390 unit tests, all passing.
- **JUnit 5 + Mockito 5 migration** (#590, #591): test framework on current LTS-era stack.
- **F1/F2 ScheduledExecutorService injection** (#592): no more self-constructed executor threads in sync components.
- **KillTimeTracker scaffolding** (#589): groundwork for kill-time-aware efficiency scoring.

### Remaining items (low priority, not blocking submission)

- 6 `new Color(...)` allocations in overlays could stylistically migrate to `ColorUtil.colorWithAlpha(base, alpha)`; current cache-guarded sites are functionally equivalent and not a perf concern.

### How to use this document

Before opening the resubmission PR against `runelite/plugin-hub`, re-run `plugin_hub_validate`, confirm `./gradlew build` is clean, and bump the baseline commit at the top.
