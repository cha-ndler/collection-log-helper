# Tier B1 scoping: composable completion conditions (AND / OR / NOT)

> Finishes [Tier B1](../ROADMAP.md#tier-b--guidance-depth-parity-with-quest-helper). The 11 atomic `CompletionCondition` leaves stay; this adds boolean composition over them so a step can declare "completes when (in zone X AND has item Y) OR (manual click)" without hard-coding new enum values.

## 1. Current state

`CompletionCondition` (see `src/main/java/com/collectionloghelper/data/CompletionCondition.java`) declares 11 atomic values:

| Value | Meaning |
|---|---|
| `ITEM_OBTAINED` | A specific item appears in the collection log. |
| `INVENTORY_HAS_ITEM` | Inventory contains a specific item (with optional count). |
| `INVENTORY_NOT_HAS_ITEM` | Inventory no longer contains a specific item. |
| `ARRIVE_AT_TILE` | Player is within `completionDistance` of a target tile. |
| `ARRIVE_AT_ZONE` | Player enters a rectangular zone. |
| `NPC_TALKED_TO` | Player interacts with a specific NPC. |
| `ACTOR_DEATH` | A specific NPC dies (multi-form via `completionNpcIds`). |
| `PLAYER_ON_PLANE` | Player reaches a target world plane. |
| `CHAT_MESSAGE_RECEIVED` | Chat regex matches an incoming message. |
| `VARBIT_AT_LEAST` | A varbit reaches a threshold. |
| `MANUAL` | Player advances the step from the panel. |

Today's contract is one enum value per step plus supporting flat fields (`completionItemId`, `completionZone`, `completionNpcId`, `completionVarbitId`, `completionChatPattern`, ...). `CompletionChecker` dispatches on the enum value: each `is...Satisfying` method checks one branch, and `isStepAlreadySatisfied` is a `switch` over the enum. There is no boolean composition.

The closest workaround is `ConditionalAlternative`. It re-shapes a step's fields when an outer `SourceRequirements` check matches (and as of B3 it can nest). It does NOT compose the live completion condition of a single active step; it picks between mutually exclusive variants up front and then evaluates each variant's single condition normally.

Concretely, no current schema expresses: "the step is done when the player is in the boss arena AND holds the trophy item, OR when they manually click done." That is the gap B1 needs to close.

## 2. Target shape

Two viable directions.

### Option A: discriminated-union nested object

```json
"completionCondition": {
 "type": "AND",
 "children": [
 { "type": "ARRIVE_AT_ZONE", "zone": [3200, 3200, 3210, 3210, 0] },
 { "type": "INVENTORY_HAS_ITEM", "itemId": 12345, "count": 1 }
 ]
}
```

`type` is the discriminator. Leaves carry their own supporting fields inline instead of borrowing them from the parent step.

### Option B: keep flat enum, add a sibling `conditionTree`

```json
"completionCondition": "ARRIVE_AT_ZONE",
"conditionTree": {
 "and": [
 { "zone": [3200, 3200, 3210, 3210, 0] },
 { "item": 12345, "count": 1 }
 ]
}
```

The legacy field is used when `conditionTree` is absent; when present, `conditionTree` wins. Leaves can still borrow data from the parent step or carry it inline.

### Comparison

| Axis | Option A | Option B |
|---|---|---|
| Backwards compat with 225 sources | Field type changes from `String` to `Object`. Gson can be steered with a custom deserializer, but every existing JSON value becomes the leaf form `{ "type": "ITEM_OBTAINED" }` and must be rewritten or transparently up-converted. | Existing JSON unchanged. `conditionTree` is purely additive and `@Nullable`. |
| Gson deserialization | Custom `TypeAdapter` or `JsonDeserializer` keyed on `type`. Moderate complexity but a one-time cost. | Stock Gson on both fields. Custom adapter only for the tree shape itself, which is small. |
| Reviewer / contributor friction | Cleaner long-term mental model: one nested object describes everything. Awkward in the short term because contributors must keep two shapes in mind during migration. | Two fields on the step for the same concern. Trade-off is honest: the new field is opt-in and only appears on the small set of steps that need composition. |
| Unit testing | Tree-shaped, easy to assert on. Same test surface as Option B. | Tree-shaped, easy to assert on. Same test surface as Option A. |

### Recommendation: Option B

The decisive factor is the 225-source migration. Option A forces every existing step through a deserializer migration or a one-time data rewrite, both of which carry real regression risk. Option B is additive: existing JSON parses exactly as it does today, the new field is `@Nullable`, and only steps that genuinely need composition opt in. We accept the cost of two fields on the same concern because the alternative is touching every source for zero functional gain on 220 of them.

If a future B-tier wave demonstrates that the dual-field shape is creating chronic confusion, the migration to Option A is mechanical: walk every step, wrap the flat field into a single-leaf `conditionTree`, drop the flat field. That migration is strictly easier from Option B than the reverse.

## 3. Java API

Net-new types (all under `com.collectionloghelper.data.condition`):

- `ConditionNode` - sealed interface. `boolean evaluate(ConditionEvaluationContext ctx)`.
- `AndNode implements ConditionNode` - `List<ConditionNode> children`. Short-circuits on first `false`.
- `OrNode implements ConditionNode` - `List<ConditionNode> children`. Short-circuits on first `true`.
- `NotNode implements ConditionNode` - single `ConditionNode child`.
- `LeafNode implements ConditionNode` - wraps one of the 11 atomic conditions plus its supporting fields (`itemId`, `count`, `zone`, `npcId`, `varbitId`, `varbitValue`, `chatPattern`, `tile`, `distance`, `plane`).
- `ConditionEvaluationContext` - record carrying `PlayerInventoryState`, `PlayerCollectionState`, `WorldPoint playerLocation`, plus optional in-flight event data (last chat message, last NPC death id, varbit snapshot). Mirrors what `CompletionChecker` already reads.
- `ConditionNodeBuilder` - static factories (`and(...)`, `or(...)`, `not(...)`, `inventoryHasItem(id, count)`, `arriveAtZone(zone)`, ...) for terse test setup.

Existing files touched:

- `GuidanceStep` - add one `@Nullable ConditionNode conditionTree` field. No other changes.
- `CompletionChecker` - add `boolean isConditionTreeSatisfied(GuidanceStep, ConditionEvaluationContext)`. Existing `isStepAlreadySatisfied` and each `is...Satisfying` method gains a single guard at the top: when `conditionTree` is non-null, delegate to the new method and ignore the flat field.
- Gson registration in whichever module wires the type adapters today (likely the JSON loader for `drop_rates.json`).

Existing JSON-loading and conditional-alternative code paths stay untouched. `ConditionalAlternative.completionCondition` remains a flat enum override; if a future need arises to override the tree from an alternative, that is a follow-up scope and not part of B1.

## 4. Migration plan

**Phase 1 - schema landing.** Add `conditionTree` to `GuidanceStep` as `@Nullable`. Land the `ConditionNode` type hierarchy and the Gson adapter. Parser tolerates both shapes; absent field deserializes to `null`. Zero touch on the 225 existing sources, zero behavior change.

**Phase 2 - evaluator + tests.** Implement `CompletionChecker.isConditionTreeSatisfied` and wire the guard into the existing satisfying methods. Unit tests cover each leaf type, AND short-circuit, OR short-circuit, NOT inversion, and 2-3-level nesting. Regression test: load every entry in `drop_rates.json` and assert zero parse failures and zero behavior deltas vs. the pre-Phase-1 build.

**Phase 3 - pilot on 1-2 sources.** Inspecting `drop_rates.json` for steps where the current flat enum is genuinely insufficient (the criterion is: the step's "done" state today is approximated by `MANUAL` or by a single condition that fires too early or too late). Likely candidates:

- **Trouble Brewing victory drop.** Today the brewing-round steps lean on `MANUAL` or chat patterns because the "round finished and I am back at the lobby" state is conjunctive (in lobby zone AND no active brew item in inventory). A `(ARRIVE_AT_ZONE AND INVENTORY_NOT_HAS_ITEM)` tree captures this cleanly.
- **Wintertodt brazier-light or pyromancer-heal flow.** The step is satisfied when the player has fed enough logs OR the round ends via chat message. Today this is split across two steps or coerced into a single chat regex; an `OR` tree captures it directly.

Final pilot picks will be confirmed against `drop_rates.json` during Phase 3 itself; the criterion is "today's flat enum misrepresents completion," not "we want to use the new toy."

**Phase 4 - optional deprecation note.** After pilots prove out, add a contributor-guide note recommending `conditionTree` for any new step whose completion is genuinely conjunctive or disjunctive. The flat field stays supported indefinitely; we do not bulk-migrate.

## 5. Backwards compatibility

- Existing JSON parses unchanged. `conditionTree` deserializes to `null` when absent.
- Existing evaluator path stays default. `CompletionChecker` only delegates to the tree when `conditionTree != null`.
- Migration test: load `drop_rates.json` on the Phase-1 build and on the previous release, diff the deserialized object graphs, and assert no field-level changes other than the new `null` `conditionTree`.
- `ConditionalAlternative` behavior is unchanged in Phase 1 and Phase 2.

## 6. Open questions

- **Interaction with `conditionalAlternatives`.** An alternative can override `completionCondition` today. Should it also be able to override `conditionTree`? Defer until a real authoring need surfaces; until then, alternatives that need a tree should be re-shaped as a single step with a tree containing the alternative logic as `OR` branches.
- **Where NOT is allowed.** Permit NOT anywhere a `ConditionNode` is valid (leaves, AND, OR, other NOT). Restricting it adds complexity for no real safety gain - double-negation is rare in authored data and easy to spot in review.
- **Interaction with B4 per-item activation.** B4 needs to ask "is this item the one that activates the current step?" which is orthogonal to "is the step's completion condition satisfied." The tree only describes the latter. If B4 later wants per-item branching inside the tree, add a new leaf type (`PER_ITEM_TARGET`) rather than overloading the existing leaves.
- **Per-item override of the tree itself.** Out of scope for B1. If needed, follow the existing `perItemX` field pattern.

## 7. References

- Tier B mission in `docs/ROADMAP.md` section 4; B1 row in section 9.
- Quest Helper `Conditions` class hierarchy (upstream repo) as design reference for AND/OR/NOT shape. Not a code dependency.
- `src/main/java/com/collectionloghelper/data/CompletionCondition.java`
- `src/main/java/com/collectionloghelper/data/ConditionalAlternative.java`
- `src/main/java/com/collectionloghelper/data/GuidanceStep.java`
- `src/main/java/com/collectionloghelper/guidance/CompletionChecker.java`
- PR #623 (B0 schema PR) as the precedent for additive `@Nullable` schema extensions.
