# Contributing to Collection Log Helper

Thank you for helping expand Collection Log Helper's coverage! This guide is a focused quick-start. Detailed schema, data-sourcing, and deep-guidance authoring documentation lives in [`docs/contributor-guide/`](docs/contributor-guide/).

## Overview

All drop data lives in a single file:

```
src/main/resources/com/collectionloghelper/drop_rates.json
```

The file is an array of **sources** (bosses, raids, minigames, etc.), each containing an array of **items** (drops, rewards, unlocks).

## Quick start

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/collection-log-helper.git
cd collection-log-helper

# Create a topic branch
git checkout -b add-SOURCE_NAME

# Verify the build (JSON is loaded at startup; malformed data fails fast)
./gradlew build

# Run the plugin locally
./gradlew run
```

## Documentation map

| Topic | Where it lives |
|-------|----------------|
| Full JSON schema (`CollectionLogSource`, `CollectionLogItem`, `GuidanceStep`, `Requirements`, `Waypoints`, `Conditional Alternatives`) | [`docs/contributor-guide/schema-reference.md`](docs/contributor-guide/schema-reference.md) |
| Where to source data (MCP tools, committed registries, in-game last) | [`docs/contributor-guide/data-sourcing.md`](docs/contributor-guide/data-sourcing.md) |
| How to find item IDs, drop rates, coordinates; special cases (raids, milestones, multi-phase bosses) | [`docs/contributor-guide/data-authoring.md`](docs/contributor-guide/data-authoring.md) |
| Deep guidance bar — 10-element checklist for fully authored sources | [`docs/contributor-guide/deep-guidance-bar.md`](docs/contributor-guide/deep-guidance-bar.md) |

> **Tip — try MCP first.** Item IDs, NPC IDs, object IDs, coordinates, drop rates, and varbit IDs can almost always be obtained through the `runelite-dev-toolkit` MCP server (`wiki_lookup`, `npc_lookup`, `temple_lookup`, `validate_drop_rates`, `guidance_lint`, etc.) without launching a client. In-game capture should be <5% of work. See [data-sourcing.md](docs/contributor-guide/data-sourcing.md) for the full tool list and decision tree.

## Branch and commit conventions

- **Branch name**: `add-SOURCE_NAME`, `fix-<issue-number>`, or `docs/<topic>` — keep it short and descriptive.
- **Commit message format**: `<type>: <description>` where `<type>` is one of `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`.
- **One logical change per PR.** Multiple sources can ship in one PR if they belong to the same content cluster (e.g., all GWD bosses); unrelated edits should be separate PRs.
- **Reference the issue** in the PR description (e.g., `Closes #42`).
- **Keep PRs small.** Reviewers can ship a tight 200-line PR same-day; a 2,000-line PR sits for a week.

## PR workflow

1. **Claim an issue.** Browse [open issues](../../issues) and comment to claim. Claims expire after **2 weeks** without a PR.
2. **Fork, branch, edit.** Add your source/items following the [schema reference](docs/contributor-guide/schema-reference.md).
3. **Validate locally.** Run `./gradlew build` and, where applicable, `validate_drop_rates` + `guidance_lint` MCP tools.
4. **Cite your sources in the PR description.** Note where rates, IDs, and coordinates came from (Wiki, TempleOSRS, MCP `coordinate_helper`, etc.) and the game-update month the data was verified against. See [deep-guidance-bar.md § Element 10](docs/contributor-guide/deep-guidance-bar.md#element-10--authors-date-stamp-and-source-citations).
5. **Open the PR.** Push your branch and open a PR against `master`.

## Quality checklist

Before submitting, verify:

- [ ] All item IDs verified against [TempleOSRS](https://templeosrs.com/api/collection-log/items.php) first, Wiki as fallback
- [ ] Drop rates converted to decimal with 6+ decimal places of precision
- [ ] Item names match in-game names exactly
- [ ] Wiki page names resolve to valid wiki pages
- [ ] Pets marked with `"isPet": true`
- [ ] Independent drops marked with `"independent": true`
- [ ] `killTimeSeconds` reflects full cycle time, aligned with TempleOSRS EHB where available — see [data-authoring.md § Kill Time Convention](docs/contributor-guide/data-authoring.md#kill-time-convention)
- [ ] World coordinates point to the correct location
- [ ] `./gradlew build` passes
- [ ] Source `name` matches the collection log category name exactly
- [ ] `category` is one of: `BOSSES`, `RAIDS`, `CLUES`, `MINIGAMES`, `OTHER`
- [ ] For new guidance sources, run through the [deep guidance checklist](docs/contributor-guide/deep-guidance-bar.md#deep-guidance-checklist)

## Where to ask questions

- Open a [GitHub Discussion](../../discussions) for design or scope questions.
- File an [issue](../../issues) for bugs and concrete data corrections.
- For schema or completion-condition edge cases, link the relevant `docs/contributor-guide/` section in your question so reviewers can see exactly what you're referencing.
