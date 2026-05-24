/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.collectionloghelper.data.condition.ConditionNode;
import com.collectionloghelper.data.condition.ConditionNodeDeserializer;
import com.google.gson.annotations.JsonAdapter;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * A single step in a multi-step guidance sequence for obtaining collection log items.
 * Each step describes what the player should do, where to go, and how the system
 * detects completion.
 */
@Value
public class GuidanceStep
{
	/** Human-readable description of what to do in this step. */
	String description;

	/**
	 * Optional per-item override for the step's description text.  When the active
	 * guidance has a specific target collection-log item AND this map has an entry
	 * for that item, the override string is used in place of {@link #description}.
	 * Falls back to the static description when no override applies (null map, null
	 * target, or target not present in the map).
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 */
	@Nullable
	Map<Integer, String> perItemStepDescription;

	/** Target world coordinates for this step (0 = no location). */
	int worldX;
	int worldY;
	int worldPlane;

	/** NPC to highlight for this step (0 = no NPC). */
	int npcId;

	/**
	 * Optional per-item override for the step's npcId.  When the active guidance
	 * target has an entry in this map, the overlay highlights / targets that NPC
	 * instead of the step's static {@link #npcId}.  Falls back to the static
	 * npcId when no override applies (null map, null target, target not in map).
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 */
	@Nullable
	Map<Integer, Integer> perItemNpcId;

	/** Right-click action to highlight on the NPC (e.g., "Attack", "Talk-to"). */
	String interactAction;

	/** Dialog options to highlight when talking to an NPC. */
	List<String> dialogOptions;

	/** Travel tip for reaching this step's location. */
	String travelTip;

	/** Item IDs that must be in inventory before this step can proceed. */
	List<Integer> requiredItemIds;

	/**
	 * Optional per-item override for requiredItemIds.  When the active guidance has a
	 * specific target collection-log item AND this map has an entry for that item, the
	 * override list is used in place of {@link #requiredItemIds}.  Falls back to the
	 * static {@link #requiredItemIds} when no override applies (null map, null target,
	 * or target not present in the map).
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 */
	@Nullable
	Map<Integer, List<Integer>> perItemRequiredItemIds;

	/**
	 * Soft-recommended item IDs for this step: food, prayer potions, anti-venom,
	 * combat gear, teleports, etc. Items that materially improve the run but are
	 * not strictly required to complete the step.
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 * The panel hides the "Recommended:" subsection when this list is null or empty.
	 */
	List<Integer> recommendedItemIds;

	/**
	 * Optional per-item override for recommendedItemIds.  When the active guidance has a
	 * specific target collection-log item AND this map has an entry for that item, the
	 * override list is used in place of {@link #recommendedItemIds}.  Falls back to the
	 * static {@link #recommendedItemIds} when no override applies (null map, null target,
	 * or target not present in the map).
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 */
	@Nullable
	Map<Integer, List<Integer>> perItemRecommendedItemIds;

	/** How the system detects this step is complete. */
	CompletionCondition completionCondition;

	/** Item ID used for ITEM_OBTAINED or INVENTORY_HAS_ITEM completion checks. */
	int completionItemId;

	/** Required quantity for INVENTORY_HAS_ITEM checks (0 or 1 = any quantity). */
	int completionItemCount;

	/** Tile distance threshold for ARRIVE_AT_TILE completion (default 5). */
	int completionDistance;

	/** NPC ID for NPC_TALKED_TO or ACTOR_DEATH completion check. */
	int completionNpcId;

	/** Additional NPC IDs for ACTOR_DEATH completion (multi-form bosses like Zulrah). */
	List<Integer> completionNpcIds;

	/** Chat message to display when this step activates (null = no message). */
	String worldMessage;

	/** Game object ID to highlight for this step (0 = no object). */
	int objectId;

	/** Additional object IDs to highlight (e.g., both team variants in Trouble Brewing). */
	List<Integer> objectIds;

	/** Right-click action to display on the highlighted object (e.g., "Chop", "Mine"). */
	String objectInteractAction;

	/** Item IDs to highlight in the player's inventory during this step. */
	List<Integer> highlightItemIds;

	/** Item IDs to highlight on the ground during this step. */
	List<Integer> groundItemIds;

	/** Regex pattern for CHAT_MESSAGE_RECEIVED completion check. */
	String completionChatPattern;

	/** Varbit ID for VARBIT_AT_LEAST completion check. */
	int completionVarbitId;

	/** Minimum varbit value for VARBIT_AT_LEAST completion check. */
	int completionVarbitValue;

	/** When true, overlays show "Use X on Y" style prompts instead of simple action labels. */
	boolean useItemOnObject;

	/**
	 * Max tile distance from worldX/worldY to highlight objects (0 = no filter).
	 * When set, only objects within this radius of the step's coordinates are highlighted.
	 * Useful when multiple instances of the same object ID exist but only one is correct
	 * (e.g., Trouble Brewing hoppers).
	 */
	int objectMaxDistance;

	/**
	 * Specific tile coordinates where objects should be highlighted.
	 * Each entry is [x, y, plane]. When non-null, only objects at one of these
	 * tiles are highlighted, overriding objectMaxDistance.
	 */
	List<int[]> objectFilterTiles;

	/** Widget IDs to highlight in the game interface during this step. Each entry is [groupId, childId]. */
	int[][] highlightWidgetIds;

	/** 1-indexed step number to loop back to when this step completes (0 = no loop). */
	int loopBackToStep;

	/** Total number of loop iterations before advancing past this step (0 = no loop). */
	int loopCount;

	/**
	 * Item IDs that trigger auto-skip when ANY of them is in inventory (OR logic).
	 * Used to skip steps like "burn shade remains" when the player already has keys.
	 */
	List<Integer> skipIfHasAnyItemIds;

	/**
	 * Prioritized item-to-object tiers for dynamic overlay resolution. When non-null,
	 * the plugin scans inventory for the lowest tier with matching items and overrides
	 * the object and item highlight overlays to target that tier's objects and items.
	 * Tiers are evaluated in list order (first = lowest priority to use first).
	 */
	List<ItemObjectTier> dynamicItemObjectTiers;

	/** Zone bounds [minX, minY, maxX, maxY, plane] for ARRIVE_AT_ZONE completion (null if not a zone step). */
	int[] completionZone;

	/**
	 * Conditional alternatives for this step. When non-null, the sequencer evaluates
	 * each alternative's requirements and uses the first one whose requirements are met.
	 * Falls back to this step's own data if no alternative matches.
	 */
	List<ConditionalAlternative> conditionalAlternatives;

	/**
	 * Section label used to group consecutive steps into named collapsible blocks in the panel.
	 * Null by default — steps without a section render in the flat (ungrouped) layout.
	 * When ANY step in the active source has a non-null section, all steps are rendered
	 * with section headers; null-section steps are placed in an "Other" group.
	 */
	String section;

	/**
	 * Ordered list of waypoint tiles a player must cross to complete this step (B2).
	 * Each waypoint has a tile coordinate and a per-waypoint crossing radius.
	 *
	 * <p>When non-null and non-empty, the sequencer tracks a per-step
	 * crossed-waypoint index. On each game tick the player's position is compared
	 * against the current waypoint; when the player enters its radius the index
	 * advances. Waypoints must be crossed <em>in order</em> — crossing a later
	 * waypoint before an earlier one does not advance the index. The step is
	 * satisfied when all waypoints have been crossed in order.
	 *
	 * <p>Null or empty list preserves legacy single-target {@code ARRIVE_AT_TILE}
	 * behaviour unchanged. This field is strictly additive: production data never
	 * sets it until an explicit D-tier backfill PR adds waypoint sequences.
	 *
	 * <p>The step's {@link #worldX}/{@link #worldY}/{@link #completionDistance}
	 * still serve as the logical destination for overlay rendering; authors should
	 * set the final waypoint to the same coordinates so the minimap arrow and world
	 * map route remain accurate.
	 */
	@Nullable
	List<StepWaypoint> waypoints;

	/**
	 * Optional key that activates per-tick dynamic target evaluation for this step.
	 * When non-null, {@link com.collectionloghelper.guidance.GuidanceOverlayCoordinator}
	 * looks up the corresponding
	 * {@link com.collectionloghelper.guidance.DynamicTargetEvaluator} in the
	 * {@link com.collectionloghelper.guidance.dynamic.DynamicTargetEvaluatorRegistry}
	 * and calls it once per game tick.  The returned
	 * {@link net.runelite.api.coords.WorldPoint} overrides the step's static
	 * {@link #worldX}/{@link #worldY}/{@link #worldPlane} for that tick's overlay update.
	 *
	 * <p>Null by default — existing JSON without this field deserialises unchanged.
	 * Production steps must NOT set this field; it is reserved for puzzle/dynamic
	 * content authored specifically for dynamic evaluators (e.g. Wintertodt braziers).
	 */
	@Nullable
	String dynamicTargetEvaluator;

	/**
	 * Optional composable boolean tree (AND / OR / NOT over the 11 atomic
	 * {@link CompletionCondition} leaves) describing when this step is
	 * complete (B1 Phase 1+2).
	 *
	 * <p>When non-null, the tree wins over the flat {@link #completionCondition}
	 * enum + supporting fields. When null (the default for all 225 existing
	 * sources), the legacy flat-enum path runs unchanged.
	 *
	 * <p>JSON shape is documented in
	 * {@link com.collectionloghelper.data.condition.ConditionNodeDeserializer}.
	 * Authors should only set this field when the step's completion is genuinely
	 * conjunctive or disjunctive over multiple atomic conditions; a single-leaf
	 * tree adds no value over the flat enum.
	 *
	 * <p>Phase 1+2 lands the schema and evaluator; no production source sets
	 * this field. Phase 3 (separate PR) introduces the first pilot wiring.
	 */
	@Nullable
	@JsonAdapter(ConditionNodeDeserializer.class)
	ConditionNode conditionTree;

	public int getCompletionDistance()
	{
		return completionDistance > 0 ? completionDistance : 5;
	}

	public int getCompletionItemCount()
	{
		return completionItemCount > 0 ? completionItemCount : 1;
	}

	/**
	 * Resolves the description for this step given the active guidance target item.
	 * When {@link #perItemStepDescription} has an entry for {@code targetItemId}, that
	 * override is returned.  Falls back to {@link #description} when the map is null,
	 * {@code targetItemId} is null, or no entry exists for the target.
	 *
	 * @param targetItemId the clog item ID the player is hunting, or {@code null}
	 * @return the resolved description string (never null if {@link #description} is set)
	 */
	public String resolveDescription(@Nullable Integer targetItemId)
	{
		if (targetItemId != null && perItemStepDescription != null)
		{
			String override = perItemStepDescription.get(targetItemId);
			if (override != null)
			{
				return override;
			}
		}
		return description;
	}

	/**
	 * Resolves the NPC ID for this step given the active guidance target item.
	 * When {@link #perItemNpcId} has an entry for {@code targetItemId}, that
	 * override is returned.  Falls back to {@link #npcId} when the map is null,
	 * {@code targetItemId} is null, or no entry exists for the target.
	 *
	 * @param targetItemId the clog item ID the player is hunting, or {@code null}
	 * @return the resolved NPC ID (falls back to the static npcId primitive)
	 */
	public int resolveNpcId(@Nullable Integer targetItemId)
	{
		if (targetItemId != null && perItemNpcId != null)
		{
			Integer override = perItemNpcId.get(targetItemId);
			if (override != null)
			{
				return override;
			}
		}
		return npcId;
	}

	/**
	 * Returns true if the given NPC ID matches this step's completion NPC.
	 * Checks both the single completionNpcId and the completionNpcIds list,
	 * supporting multi-form bosses (e.g., Zulrah's 3 combat forms).
	 */
	public boolean matchesCompletionNpc(int npcId)
	{
		if (completionNpcId == npcId)
		{
			return true;
		}
		if (completionNpcIds != null)
		{
			for (int id : completionNpcIds)
			{
				if (id == npcId)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the Zone defined by completionZone, or null if not set.
	 */
	public Zone getZone()
	{
		if (completionZone != null && completionZone.length == 5)
		{
			return new Zone(completionZone[0], completionZone[1], completionZone[2], completionZone[3], completionZone[4]);
		}
		return null;
	}

	/**
	 * Returns all object IDs to highlight, combining objectId and objectIds.
	 */
	public Set<Integer> getAllObjectIds()
	{
		Set<Integer> ids = new HashSet<>();
		if (objectId > 0)
		{
			ids.add(objectId);
		}
		if (objectIds != null)
		{
			for (int id : objectIds)
			{
				if (id > 0)
				{
					ids.add(id);
				}
			}
		}
		return ids.isEmpty() ? Collections.emptySet() : ids;
	}

	/**
	 * Resolves conditional alternatives against the player's current state.
	 * If the step has conditional alternatives, returns a merged step using the
	 * first alternative whose requirements are met. If no alternative matches,
	 * returns this step unchanged.
	 *
	 * <p><b>B3 — Nested conditional steps:</b> When the first matching alternative
	 * carries {@link ConditionalAlternative#getNestedAlternatives() nestedAlternatives},
	 * the evaluator merges the parent override first and then recurses: the nested list
	 * is evaluated against the already-overridden step, and the first matching nested
	 * alternative is applied on top. This process repeats for each depth level — the
	 * tree is evaluated so deeper overrides win. The recursion terminates when there are
	 * no further nested alternatives or no nested alternative matches.
	 *
	 * @param checker the requirements checker to evaluate alternatives against
	 * @return the resolved step (may be this step or a merged copy)
	 */
	public GuidanceStep resolveAlternative(RequirementsChecker checker)
	{
		if (conditionalAlternatives == null || conditionalAlternatives.isEmpty())
		{
			return this;
		}
		for (ConditionalAlternative alt : conditionalAlternatives)
		{
			if (alt.getRequirements() != null && checker.meetsRequirements(alt.getRequirements()))
			{
				return resolveAlternativeRecursive(this, alt, checker);
			}
		}
		return this;
	}

	/**
	 * Merges {@code alt} onto {@code base} and, if {@code alt} has nested alternatives,
	 * recurses into the first matching one. Returns the fully-resolved step.
	 *
	 * <p>The recursive descent is bounded by the tree depth declared in the JSON data.
	 * Nesting is structurally acyclic (child lists are inline declarations; parent
	 * references are not possible), so infinite recursion cannot occur.
	 */
	private static GuidanceStep resolveAlternativeRecursive(GuidanceStep base,
		ConditionalAlternative alt, RequirementsChecker checker)
	{
		GuidanceStep merged = base.mergeAlternative(alt);

		List<ConditionalAlternative> nested = alt.getNestedAlternatives();
		if (nested == null || nested.isEmpty())
		{
			return merged;
		}

		for (ConditionalAlternative nestedAlt : nested)
		{
			if (nestedAlt.getRequirements() != null
				&& checker.meetsRequirements(nestedAlt.getRequirements()))
			{
				return resolveAlternativeRecursive(merged, nestedAlt, checker);
			}
		}
		// No nested alternative matched — return the parent-level merge as-is.
		return merged;
	}

	/**
	 * Creates a new GuidanceStep by merging this step's values with overrides from
	 * the given alternative. Non-null fields in the alternative override the base step;
	 * null fields fall through to the base step's values.
	 */
	private GuidanceStep mergeAlternative(ConditionalAlternative alt)
	{
		return new GuidanceStep(
			alt.getDescription() != null ? alt.getDescription() : this.description,
			this.perItemStepDescription,
			alt.getWorldX() != null ? alt.getWorldX() : this.worldX,
			alt.getWorldY() != null ? alt.getWorldY() : this.worldY,
			alt.getWorldPlane() != null ? alt.getWorldPlane() : this.worldPlane,
			alt.getNpcId() != null ? alt.getNpcId() : this.npcId,
			this.perItemNpcId,
			alt.getInteractAction() != null ? alt.getInteractAction() : this.interactAction,
			this.dialogOptions,
			alt.getTravelTip() != null ? alt.getTravelTip() : this.travelTip,
			this.requiredItemIds,
			this.perItemRequiredItemIds,
			this.recommendedItemIds,
			this.perItemRecommendedItemIds,
			alt.getCompletionCondition() != null ? alt.getCompletionCondition() : this.completionCondition,
			this.completionItemId,
			this.completionItemCount,
			alt.getCompletionDistance() != null ? alt.getCompletionDistance() : this.completionDistance,
			alt.getCompletionNpcId() != null ? alt.getCompletionNpcId() : this.completionNpcId,
			this.completionNpcIds,
			this.worldMessage,
			alt.getObjectId() != null ? alt.getObjectId() : this.objectId,
			this.objectIds,
			this.objectInteractAction,
			this.highlightItemIds,
			this.groundItemIds,
			this.completionChatPattern,
			this.completionVarbitId,
			this.completionVarbitValue,
			this.useItemOnObject,
			this.objectMaxDistance,
			this.objectFilterTiles,
			this.highlightWidgetIds,
			this.loopBackToStep,
			this.loopCount,
			this.skipIfHasAnyItemIds,
			this.dynamicItemObjectTiers,
			this.completionZone,
			null, // merged steps don't carry alternatives (already resolved)
			this.section,
			null, // waypoints: merged steps inherit null; authors set waypoints on the base step
			this.dynamicTargetEvaluator,
			this.conditionTree // tree is inherited from base; alternatives do not override it in B1
		);
	}
}
