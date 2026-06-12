# Wiki-meta audit findings - 2026-06 (tranche 1: sources 1-30)

Three-stage audit (claim extraction -> wiki verification with raw receipts -> adversarial
domain-skeptic gate) over the first 30 boss sources in drop_rates.json. Scope: guidance-text
semantics only (combat meta, mechanics, items-in-text, gates, travel). Drop rates, numeric
ids, and coordinates are out of scope (adjudicated against the game cache / travel detector).
Only skeptic-CONFIRMED findings are listed; every finding embeds the raw wiki quote.

Run summary: 30 sources processed; 27 with confirmed findings; clean: Commander Zilyana, Kree'arra, Giant Mole.

## General Graardor

### [high] C2: The Fremennik Hard diary has no 'Lighthouse-area transport' reward and there is no 'Goblin Village agility shortcut' to the God Wars Dungeon. The described overland route is fabricated. The Hard diary does unlock stony basalt teleport to the top of Troll Stronghold (useful for GWD access), but that is not what the guidance describes.

- **Data says:** Trollheim teleport not required: use the Fremennik Hard diary unlocks (e.g. Lighthouse-area transport) plus the Goblin Village agility shortcut to reach the God Wars Dungeon entrance overland
- **Wiki says (raw):** Hard tier Fremennik diary rewards: Five daily teleports to Rellekka marketplace; Adamantite bars from aviansie killed in the God Wars Dungeon are now noted; Enchanted lyre teleport expansion (Waterbirth Island); Tan Leather and Recharge Dragonstone spells; Stony basalt now teleports you on top of the Troll Stronghold, past the rocky shortcut; Rellekka Rooftop Course bonus. No Lighthouse-area transport. No Goblin Village agility shortcut to GWD.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fremennik_Province_Diary
- **Suggested fix:** Replace the Fremennik Hard diary alternative with the actual diary benefit: stony basalt teleport to top of Troll Stronghold, then run south-east down to the GWD entrance. Remove the Lighthouse-area transport and Goblin Village agility shortcut references, which do not exist.
- **Skeptic receipt:** Fremennik Diary Hard Tier Rewards (wiki): 'Five daily teleports to Rellekka marketplace; ... Adamantite bars from aviansie killed in the God Wars Dungeon are now noted; ... Stony basalt teleport - Now teleports you on top of the Troll Stronghold, past the rocky shortcut'. To the specific questions: 'No lighthouse-area transport reward exists' and 'No goblin village agility shortcut reward exists'.
- **Skeptic reasoning:** Survived every refutation vector. The diary GATE itself (FREMENNIK_HARD) is legitimate, but the conditionalAlternatives[0] DESCRIPTION text (drop_rates.json lines 126-127) cites two unlocks that do not exist. The authoritative Hard-tier reward list contains no 'Lighthouse-area transport' and no 'Goblin Village agility shortcut to GWD'. This is not an item-variant, multi-source, staleness, or account-type issue -- it is user-facing guidance text describing a fabricated overland route. The actual relevant Hard reward is the stony basalt teleport to the top of Troll Stronghold (Making Friends with My Arm required), which the suggestedFix correctly identifies. STANDS as a low/medium content-accuracy fix: only the description/travelTip strings are wrong, the requirement wiring is fine.

### [medium] C10: Full Troll Stronghold quest completion is not required. Only partial completion (defeating Dad, the level 101 troll mid-quest) plus 15 Agility is needed to enter the God Wars Dungeon. Players who have defeated Dad but not finished the quest can already access GWD. Additionally, Easy Combat Achievements (Ghommal's Hilt 1) fully bypass the quest requirement.

- **Data says:** TROLL_STRONGHOLD (quest completion listed as requirements.quests[0])
- **Wiki says (raw):** 15 Agility and defeating Dad are prerequisites for entry to the God Wars Dungeon (without Ghommals Hilt). God Wars Dungeon can be accessed without the quest via the Ghommal's Hilt teleport.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Troll_Stronghold_(quest)
- **Suggested fix:** Consider changing the requirement to reflect that only partial Troll Stronghold completion (defeating Dad) is needed, not full quest completion. Alternatively, note the Ghommal's Hilt bypass. The current entry overclaims the gate.
- **Skeptic receipt:** God Wars Dungeon (wiki): 'Accessing it requires partial completion of Troll Stronghold (defeating Dad) or full completion of the Easy Combat Achievements (in order to use the teleport on Ghommal's hilt 1) and either [Eadgar's Ruse / climbing boots]. A rope is also required the first time.'
- **Skeptic reasoning:** Survived the account-type/progress-specificity vector (vector 3) rather than being refuted by it. The GWD wiki explicitly states access needs only PARTIAL Troll Stronghold (defeating Dad), or Easy Combat Achievements via Ghommal's hilt 1 -- full quest completion is required by NO account. RequirementsChecker.java line 319 checks the `quests` field strictly against QuestState.FINISHED and adds it to `unmet` otherwise, so a player who has defeated Dad (quest IN_PROGRESS) but not finished is wrongly told they cannot farm Graardor. The codebase already models partial-completion gates (QuestSubMilestone enum: Lost City dramen-branch, RFD tiers) and has a separate looser 'started is enough' quest check (lines 434-441), so the strict full-completion gate here is an over-claim, not a modeling limitation. This is the systemic GWD-entry gate (Zilyana/K'ril/Kree'arra share it), so any fix should be applied consistently. STANDS at medium (the affected population -- beat-Dad-but-unfinished -- is narrow); not a blocker.

## K'ril Tsutsaroth

### [high] C9: Neither Balfrug Kreeyath nor Tstanon Karlak drain prayer according to the wiki. The guidance incorrectly attributes prayer drain to both minions and instructs the player to 'eat through this drain' -- this is fabricated mechanic guidance that could cause the player to mismanage resources or misidentify the source of prayer loss during the fight.

- **Data says:** Balfrug and Tstanon drain prayer, and the player must eat through this drain
- **Wiki says (raw):** Balfrug Kreeyath: 'a black demon that launches fire spells at his target, dealing up to 16 damage.' No prayer drain effects are mentioned for this minion. Tstanon Karlak: 'a greater demon that uses melee, clawing his target, dealing up to 15 damage.' No prayer drain effects mentioned for this minion.
- **Wiki URL:** https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies
- **Suggested fix:** Remove the prayer drain attribution from Balfrug and Tstanon. Their threat is HP damage (up to 16 and 15 respectively). Only K'ril's special attack ('Prayer Smash') drains prayer.
- **Skeptic receipt:** Balfrug Kreeyath: 'a black demon that launches fire spells at his target, dealing up to 16 damage.' Tstanon Karlak: 'a greater demon that uses melee, clawing his target, dealing up to 15 damage.' Prayer Drain: 'This minion does not drain prayer--only K'ril's special attack has that mechanic.' (oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies)
- **Skeptic reasoning:** The guidance string (line 500) reads: 'Protect from Melee, eat through Balfrug + Tstanon prayer drain, watch for K'ril's curse special which strips your prayer in one hit.' I applied every refutation vector. (1) Multi-source/variant/account-type vectors are inapplicable -- this is guidance prose, not a clog itemId or requirement gate. (2) Staleness: wiki_updates for 'K'ril' since 2026-01-01 returned count 0, so our data is NOT reflecting a newer game version -- the current wiki is authoritative and stable. (3) Context read: 'eat through ... prayer drain' attributes a prayer-drain mechanic to the two minions and tells the player to manage it. The authoritative wiki Strategies page is explicit that Balfrug Kreeyath 'launches fire spells at his target, dealing up to 16 damage' (a Magic attacker) and Tstanon Karlak 'uses melee, clawing his target, dealing up to 15 damage' (a Melee attacker), and that ONLY K'ril's Prayer Smash drains prayer. Neither minion drains prayer. The minions' threat is HP damage, not prayer loss. This is a fabricated mechanic attribution that could cause the player to misidentify the source of prayer loss and mismanage restores. Finding stands.

### [medium] C10: The wiki names the special attack 'Prayer Smash' (not a 'curse') and specifies it drains prayer by half, not all prayer in one hit. 'Strips prayer in one hit' implies a complete drain, which is incorrect and matters strategically -- players may still have half their prayer remaining after the special and can continue using protection prayers without immediately potting.

- **Data says:** K'ril Tsutsaroth has a curse special attack that strips prayer in one hit
- **Wiki says (raw):** K'ril's special attack is called 'Prayer Smash' -- it hits 35-49 damage and involves 'draining his target's current prayer points by half, rounded down.' Using a Spectral spirit shield 'halves the prayer drain to 1/4.'
- **Wiki URL:** https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies
- **Suggested fix:** Update to: 'K'ril has a special attack called Prayer Smash that deals 35-49 damage and drains half of your current prayer points; use a Spectral spirit shield to reduce the drain to one-quarter.'
- **Skeptic receipt:** K'ril's Special Attack -- Name & Effect: 'Prayer Smash'. 'He will yell YARRRRRRR! when using his prayer smash, always dealing 35-49 damage and draining his target's current prayer points by half, rounded down.' Spectral spirit shield reduces this drain to 25%. (oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies)
- **Skeptic reasoning:** Same guidance string asserts K'ril's special 'strips your prayer in one hit', implying a complete drain, and calls it a 'curse'. The authoritative wiki names the special 'Prayer Smash' and states it always deals 35-49 damage while 'draining his target's current prayer points by half, rounded down' -- i.e., HALF, not all. The strategic difference is real and material: the player retains ~half their prayer points after the special and can keep Protect from Melee active without an immediate prayer pot, contrary to the 'stripped in one hit' framing. No refutation vector rescues it: no variant/account-type/staleness nuance applies (wiki_updates count 0 since 2026-01-01), and the context reading does not soften 'strips ... in one hit' into 'drains half'. The 'curse' label is also a naming inaccuracy versus the canonical 'Prayer Smash'. Logged at medium per the candidate severity -- it is a guidance-accuracy correction, not a clog blocker.

## Zulrah

### [blocker] C5: Fairy ring AKQ leads to the Piscatoris Hunter area in Kandarin -- not to Zul-Andra. The correct code for reaching the Zul-Andra stepping stone is BJS. Dialling AKQ would strand the player hundreds of tiles away in the wrong region entirely.

- **Data says:** Use the POH superior garden fairy ring (dramen/lunar staff equipped) to dial AKQ, then run east to Zul-Andra
- **Wiki says (raw):** Code: AKQ | Map: Kandarin | Location: "Piscatoris Hunter area" | Points of Interest: "Kraken Cove, Chinchompas (grey), Mining spots, Piscatoris Fishing Colony."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring#Codes
- **Suggested fix:** Replace AKQ with BJS in the POH conditionalAlternatives[0] description and travelTip. The corrected text should read: 'Use the POH superior garden fairy ring to dial BJS, then run east to Zul-Andra'.
- **Skeptic receipt:** AKQ leads to the "Piscatoris Hunter area" in Kandarin, which features chinchompas and a fishing colony. BJS leads to an island "Near Zul-Andra" and requires completion of the Regicide quest. This is the ring associated with Zulrah access.
- **Skeptic reasoning:** The POH conditionalAlternatives[0] (drop_rates.json line 803-804) tells the player to dial fairy ring code AKQ and 'run east to Zul-Andra'. The wiki confirms AKQ is in the wrong region entirely. Critically, the source's OWN main travelTip (line 784) and guidanceSteps[0] (lines 789/795) correctly use BJS for Zul-Andra, so the AKQ in the POH alternative is an internal inconsistency, not a legitimate alternate route. BJS is gated on Regicide, which this source already requires (line 704), so it is valid for any account that can reach Zulrah. Not a multi-source/variant/account-type case. The finding holds.

### [blocker] C7: AKQ deposits the player in Piscatoris (Kandarin, far north of Zul-Andra). Running east from Piscatoris does not lead to Zul-Andra. This travelTip would send a player to the completely wrong area. The correct code is BJS, which lands on the island west of Zul-Andra.

- **Data says:** POH superior garden fairy ring -> AKQ -> run east to Zul-Andra
- **Wiki says (raw):** Code: AKQ | Map: Kandarin | Location: "Piscatoris Hunter area"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring#Codes
- **Suggested fix:** Change AKQ to BJS in conditionalAlternatives[0].travelTip. Correct text: 'POH superior garden fairy ring -> BJS -> run east to Zul-Andra'.
- **Skeptic receipt:** AKQ leads to the "Piscatoris Hunter area" in Kandarin... BJS leads to an island "Near Zul-Andra" and requires completion of the Regicide quest. This is the ring associated with Zulrah access.
- **Skeptic reasoning:** Same defect as C5, targeting the companion travelTip on the same conditionalAlternative (line 804: 'POH superior garden fairy ring -> AKQ -> run east to Zul-Andra'). AKQ deposits the player at Piscatoris in Kandarin; running east from there does not reach Zul-Andra. BJS is the code that lands on the island beside Zul-Andra. Confirmed by the same authoritative fairy-ring receipt. C5 and C7 are the description and travelTip halves of one fix.

### [blocker] C12: The red/crimson form attacks with Melee, so the correct protection prayer is Protect from Melee -- not Protect from Missiles. The guidance has the correct label ('Melee form') but the wrong prayer, which would leave the player completely unprotected against the form's attacks.

- **Data says:** red (Melee form) -> Protect from Missiles
- **Wiki says (raw):** Zulrah's crimson form, in which it will attack with Melee. Players should use Protect from Melee or position themselves at the safespots shown in the images.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zulrah/Strategies
- **Suggested fix:** In guidanceSteps[2].description, change 'red (Melee form) -> Protect from Missiles' to 'red (Melee form) -> Protect from Melee'.
- **Skeptic receipt:** Red (Crimson/Magma) Form: Attack style: Typeless melee via tail swing... "Zulrah will stare at the player's position for several seconds before whipping its tail at that area." Safespot to evade Zulrah's melee form.
- **Skeptic reasoning:** guidanceSteps[2].description (line 830) reads 'red (Melee form) -> Protect from Missiles'. Protect from Missiles is the Ranged-protection overhead; it does nothing against a Melee attack. The wiki confirms the red/crimson form attacks with Melee (a tail-swing/tail-whip), handled by Protect from Melee or by moving to a safespot. This is not an account-type or variant nuance - it is a flat wrong prayer for the stated form. The suggested fix 'Protect from Melee' is correct as a prayer (the wiki additionally notes the melee tail attack can be safespotted/avoided by stepping two tiles away), but either way 'Protect from Missiles' is unambiguously wrong. The finding holds.

### [medium] C16: The wiki describes snakelings and toxic clouds as separate things that Zulrah summons -- snakelings attack the player directly, while toxic clouds are a distinct hazard. Snakelings do not spit or produce the toxic clouds; Zulrah generates both independently. The guidance misattributes the clouds to snakeling behavior.

- **Data says:** avoid the toxic clouds the snakelings spit
- **Wiki says (raw):** It can also summon snakelings, smaller versions of itself, on the platform to attack the player, along with toxic clouds that deal rapid damage if they stand in one.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zulrah
- **Suggested fix:** Change 'toxic clouds the snakelings spit' to 'toxic clouds Zulrah summons' in guidanceSteps[2].description.
- **Skeptic receipt:** It can also summon snakelings, smaller versions of itself, on the platform to attack the player, along with toxic clouds that deal rapid damage if they stand in one. (Snakelings are distinct enemies; the clouds are environmental hazards Zulrah creates independently.)
- **Skeptic reasoning:** guidanceSteps[2].description ends with 'avoid the toxic clouds the snakelings spit' (line 830). The wiki describes snakelings and toxic clouds as two separate things Zulrah summons - snakelings are minion NPCs that attack directly, and the toxic clouds are an independent environmental hazard Zulrah generates. The clouds are not produced/spit by the snakelings. The data misattributes the clouds to snakeling behavior. This is a genuine (medium) factual inaccuracy in the guidance text; no multi-source/variant/account-type reading rescues it. Finding holds.

## Vorkath

### [blocker] C11: The wiki describes the freeze attack as 'icy breath' that locks the player in place -- there is no slow projectile to dodge with diagonal walking. The diagonal/tick-manipulation technique (Woox Walk) applies to the acid phase (Rapid Fire), not the freeze/spawn phase. A player following this guidance would attempt to move diagonally during the freeze phase (when they are immobilised) and neglect the actual required response (killing the Zombified Spawn).

- **Data says:** A slow purple ice projectile at Vorkath requires diagonal walking to dodge
- **Wiki says (raw):** While this deals no damage, it will freeze the player in place, and Vorkath will spit out a Zombified Spawn that always lands ten tiles away from the player.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vorkath/Strategies
- **Suggested fix:** Remove the diagonal-walking instruction for the freeze phase. The freeze immobilises the player; the correct response is to kill the Zombified Spawn (ideally with Crumble Undead). The Woox Walk description belongs with the acid/Rapid Fire phase guidance.
- **Skeptic receipt:** While this deals no damage, it will freeze the player in place, and Vorkath will spit out a Zombified Spawn that always lands ten tiles away from the player.
- **Skeptic reasoning:** Survives all refutation vectors. The data instructs 'walk diagonally to dodge the slow purple ice projectile.' The authoritative strategies page states the ice/freeze attack FREEZES the player in place - the player is immobilized and cannot move, let alone walk diagonally. There is no dodgeable slow purple ice projectile; the freeze itself deals no damage and exists to set up the Zombified Spawn. The diagonal/tick-manipulation 'Woox Walk' belongs to the acid/Rapid Fire phase (a method to attack while moving on clean tiles), not the freeze phase. The data's instruction is mechanically impossible (you cannot move while frozen) and would mislead a player into ignoring the actual required response (killing the spawn). This is a genuine fabricated mechanic, not a variant or account-type nuance.

### [high] C12: Two errors: (1) The wiki's recommended method is casting Crumble Undead, not clicking/attacking the spawn. Clicking it to melee/range attack is possible but suboptimal and not what the wiki instructs. (2) 'Ramping damage if ignored' misrepresents the mechanic -- the explosion deals damage scaled to the spawn's remaining Hitpoints at the moment of explosion (up to 60), not damage that ramps while it walks toward the player.

- **Data says:** Zombified Spawn must be clicked immediately when it appears, as it deals ramping damage if ignored
- **Wiki says (raw):** Casting Crumble Undead will instantly kill the spawn, and it will always hit, provided that the player's Magic attack bonus is above -64. If it reaches the player, it will explode, dealing damage based on its current Hitpoints prior to explosion; this can deal up to 60 damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vorkath/Strategies
- **Suggested fix:** Replace 'click it immediately' with 'cast Crumble Undead on it'; replace 'ramping damage if ignored' with 'explodes on contact, dealing up to 60 damage based on its remaining HP'.
- **Skeptic receipt:** Casting Crumble Undead will instantly kill the spawn, and it will always hit, provided that the player's Magic attack bonus is above -64. If it reaches the player, it will explode, dealing damage based on its current Hitpoints prior to explosion; this can deal up to 60 damage.
- **Skeptic reasoning:** Survives the vectors. The data says 'click the Zombified Spawn the instant it appears (it deals ramping damage if ignored).' Two distinct errors against the authoritative quote: (1) the wiki's recommended kill method is casting Crumble Undead (instant kill, always hits with Magic attack bonus above -64), not clicking to melee/range it; 'click it' omits the meta method. (2) 'ramping damage if ignored' misstates the mechanic: the spawn explodes ON CONTACT for damage scaled to its REMAINING HP at the moment of explosion (up to 60), which decreases as you damage it - it does not 'ramp up' over time while walking. The correct play is to reduce its HP (or instakill via Crumble Undead) BEFORE it reaches you so the explosion is weak/nullified - the opposite intuition from 'ramping damage'. Both halves of the parenthetical are wrong.

### [high] C13: The zombie axe (Vet'ion's axe) does not appear anywhere on the Vorkath or Vorkath/Strategies wiki pages. The wiki lists the Dragon Hunter Lance as the most effective melee weapon, followed by Osmumten's fang, Ghrazi rapier, and Zamorakian hasta. Presenting the zombie axe as 'modern meta' is not supported by the authoritative source.

- **Data says:** Zombie axe is modern meta for Vorkath
- **Wiki says (raw):** The dragon hunter lance...is the most effective melee weapon to use against Vorkath.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vorkath/Strategies
- **Suggested fix:** Replace zombie axe with Dragon Hunter Lance (or Dragon Hunter Crossbow for ranged) as the primary meta weapon recommendation.
- **Skeptic receipt:** The dragon hunter lance is the most effective melee weapon to use against Vorkath. ... The dragon hunter crossbow, (with ruby and diamond dragon bolts (e)), is the most effective ranged weapon to use against Vorkath. ['Zombie axe': Not mentioned anywhere in the document; 'Vet'ion's axe': Not mentioned anywhere in the document]
- **Skeptic reasoning:** Survives the vectors including staleness (wiki_updates shows the Strategies page was edited 2026-06-12, adding Scythe as a melee alternative and still confirming DHL as primary - no zombie axe). The data prose claims 'Zombie axe and Bow of faerdhinen are the modern meta.' The authoritative strategies page names the dragon hunter lance as the most effective melee weapon (alongside Osmumten's fang, Ghrazi rapier, Zamorakian hasta), and the dragon hunter crossbow as the most effective ranged. Zombie axe (Vet'ion's axe) appears NOWHERE on the page - it is a budget melee weapon with no draconic passive, so it is genuinely not meta against Vorkath, a draconic target where DHL/DHCB passives dominate. The claim is unsupported by and contradicts the authoritative source. NOTE for the fixer: 'Bow of faerdhinen' IS wiki-listed as a viable ranged option, so only the 'Zombie axe ... modern meta' portion is the bug; the fix should replace zombie axe with dragon hunter lance (and optionally dragon hunter crossbow for ranged), not strip bofa. The recommendedItemIds (Anglerfish 13441, Super antifire 3024, Antivenom+ 12913) are consumables and unaffected.

## Cerberus

### [medium] C3: Visual cue for Magic attack is inverted. The data says Cerberus 'rears back' but the wiki says she 'lowers her head'. A player following this guidance would look for the wrong animation cue before prayer-flicking.

- **Data says:** Flick Protect from Magic when Cerberus rears back and fires a smooth black ball
- **Wiki says (raw):** Magic - Cerberus lowers her head and fires a smooth black ball.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Cerberus/Strategies
- **Suggested fix:** Change 'rears back and fires' to 'lowers her head and fires' to match the wiki's description of the magic attack animation.
- **Skeptic receipt:** Magic - Cerberus lowers her head and fires a smooth black ball. (https://oldschool.runescape.wiki/w/Cerberus/Strategies)
- **Skeptic reasoning:** Data guidance says Cerberus 'rears back and fires a smooth black ball' for the Magic attack. The authoritative Cerberus/Strategies page describes the same attack as 'Cerberus lowers her head and fires a smooth black ball.' The projectile descriptor ('smooth black ball') is correct, but the body-motion cue is inverted (lowers head vs rears back). This is a genuine wrong-animation-cue error, though impact is mild since the ball description alone disambiguates the attack -- medium-to-low severity. No refutation vector applies: this is guidance prose, not a multi-source item, variant, or account-type nuance.

### [blocker] C6: The trigger interval and condition for the souls mechanic are both wrong. The data says souls are summoned every 10 attacks; the wiki says every seventh attack and only below 400 HP. The 'every 10 attacks' figure belongs to a completely separate mechanic: the triple normal attack sequence (magic + ranged + melee rapid succession), which has no souls. Following this guidance would leave players unprepared for souls at the correct frequency and confused about when to expect them.

- **Data says:** Every 10 attacks Cerberus summons three souls (Magic ghost, Ranged ghost, Melee ghost)
- **Wiki says (raw):** This attack is performed every seventh attack, but only after Cerberus has under 400 hitpoints.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Cerberus/Strategies
- **Suggested fix:** Change the souls trigger description to 'every 7 attacks once Cerberus is below 400 HP' and ensure the description is clearly about the summoned souls mechanic, not the triple normal attack.
- **Skeptic receipt:** Souls/Ghosts Mechanic - Trigger: Only starts when Cerberus falls below 400 HP; Frequency: Every seventh attack; Count: Three summoned souls appear. Triple Attack: 'is initiated as her first attack and is repeated after every ten attacks in sequence.' (https://oldschool.runescape.wiki/w/Cerberus/Strategies)
- **Skeptic reasoning:** Data says 'Every 10 attacks she summons three souls.' The authoritative Strategies page states the souls attack triggers 'every seventh attack' and 'Only starts when Cerberus falls below 400 HP.' Separately, the same page assigns 'every ten attacks' to the unrelated triple-attack sequence (magic->ranged->melee, 'repeated after every ten attacks in sequence'), which summons no souls. The data has conflated the triple-attack interval with the souls mechanic AND dropped the sub-400-HP gate entirely. Both the interval and the trigger condition are wrong. Not an account-type/variant/staleness artifact -- it is a flat mechanic conflation.

### [blocker] C8: Cerberus is classified as 'Demon', not undead. The Salve amulet (ei) only provides bonuses against undead monsters. Recommending it as gear for Cerberus is incorrect -- it would provide no bonus, wasting an amulet slot that should be filled with Amulet of torture or Amulet of rancour. The Cerberus wiki page lists no undead attribute for her.

- **Data says:** Bring Salve amulet (ei)
- **Wiki says (raw):** Increases melee, ranged and magic damage & accuracy by 20% against the undead.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Salve_amulet_(ei)
- **Suggested fix:** Remove the Salve amulet (ei) recommendation. Replace with an appropriate amulet such as Amulet of torture (for melee) or Amulet of rancour (for ranged) as listed on the Cerberus/Strategies wiki page.
- **Skeptic receipt:** npc_lookup (abextm cache, source of truth): Cerberus ... Attributes: demon. Wiki neck-slot recommendations: 1. Amulet of rancour 2. Amulet of torture 3. Amulet of blood fury 4. Amulet of fury 5. Amulet of glory. Salve amulet (ei) is not mentioned in the equipment recommendations. (https://oldschool.runescape.wiki/w/Cerberus/Strategies)
- **Skeptic reasoning:** Data recommends 'Bring Salve amulet (ei).' Salve amulet (ei) only boosts damage/accuracy against UNDEAD. The abextm game cache npc_lookup for Cerberus returns 'Attributes: demon', and the wiki classifies her as a demon, not undead -- confirmed independently by two authorities. Salve (ei) therefore grants zero bonus at Cerberus; the Strategies gear table never lists it, recommending Amulet of rancour/torture/blood fury/fury/glory for the neck slot instead. No refutation vector rescues this: not a multi-source item, not a variant id question (it's prose, not an itemId), and not account-type-dependent -- Salve never helps vs a demon for any account.

## Alchemical Hydra

### [high] C20: Lightning is attributed to the Flame (red) phase in the data, but the wiki clearly states electricity occurs during the BLUE phase and fire occurs during the RED phase. The lightning mechanic is a blue-phase attack; the flame phase has fire walls only. Bundling 'lightning chase' under the Flame phase will mislead players into expecting lightning when fighting the red boss, which does not happen.

- **Data says:** on the Flame phase dodge the fire walls and the lightning chase
- **Wiki says (raw):** It also has special attacks for each phase: poison pools when green and black, electricity when blue and fire when red.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Alchemical_Hydra
- **Suggested fix:** Split the phase-specific dodge notes: blue/electric phase -> dodge the lightning chase; red/flame phase -> dodge the fire walls. The current text incorrectly places both under 'Flame phase'.
- **Skeptic receipt:** Alchemical Hydra: 'It also has special attacks for each phase: poison pools when green and black, electricity when blue and fire when red.' Strategies: Flame phase (red): 'it breathes 5x5 layer of fire to the player's sides...launching a tracking fire.' Lightning phase (blue): 'spawns four lightning currents in the four corners of the chamber, converging on the player's location.'
- **Skeptic reasoning:** The data text reads 'on the Flame phase dodge the fire walls and the lightning chase'. The wiki attributes fire/tracking fire to the RED (flame) phase but lightning/electricity to the BLUE (electric) phase. The earlier sentence in the same guidance string even correctly labels 'Blue/electric' and 'Red/flame' as distinct phases, so bundling the 'lightning chase' under the Flame phase contradicts the data's own phase labels and the wiki. Refutation vectors checked: not a variant/multi-source/account-type issue -- it is a phase-attribution error. Lightning currents converge on the player during the blue phase, not the red flame phase. This will mislead players to expect lightning on red.

### [medium] C13: The data implies the DIRECTION of the head sway tells you which style (magic vs ranged) is incoming. The wiki only documents that a sway indicates a style CHANGE, with no mention of directional differentiation between magic and ranged. The wiki does not support that sway direction is a style-specific tell.

- **Data says:** watch the head sway between basic attacks - Magic head sways one way, Ranged the other
- **Wiki says (raw):** after every three basic attacks, the Hydra will sway its heads backwards as an indicator that it is changing combat styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Alchemical_Hydra/Strategies
- **Suggested fix:** Revise to: 'watch for the head sway after every three attacks as a signal that the combat style is changing' rather than implying the direction disambiguates magic vs ranged.
- **Skeptic receipt:** Alchemical Hydra/Strategies: 'after every three basic attacks, the Hydra will sway its heads backwards as an indicator that it is changing combat styles.' Confirmed on fetch: 'The direction of the sway does not predict whether Magic or Ranged will follow.'
- **Skeptic reasoning:** The data asserts 'Magic head sways one way, Ranged the other' -- i.e. the sway DIRECTION tells you the incoming style. The wiki documents only that the sway signals a style CHANGE, with no directional differentiation, and the Strategies page explicitly states the direction does not predict which style follows. This is a fabricated tell, not legitimate game behaviour, and would teach players to react to a non-existent directional signal. Not account-type or variant dependent. The downstream actionable advice ('flick Protect from Magic vs Magic...') is fine; only the directional sway claim is wrong.

### [medium] C14: Same issue as C13: the data implies a directional sway distinguishes Ranged attacks from Magic attacks, but the wiki documents only that the sway signals a style change, with no directional distinction between the two styles.

- **Data says:** watch the head sway between basic attacks - Magic head sways one way, Ranged the other
- **Wiki says (raw):** after every three basic attacks, the Hydra will sway its heads backwards as an indicator that it is changing combat styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Alchemical_Hydra/Strategies
- **Suggested fix:** Same fix as C13: remove the 'one way / other' directional framing; the sway is a change indicator, not a style-specific directional signal.
- **Skeptic receipt:** Alchemical Hydra/Strategies: 'after every three basic attacks, the Hydra will sway its heads backwards as an indicator that it is changing combat styles.'
- **Skeptic reasoning:** Same underlying defect as C13, same source text. The 'Magic head sways one way, Ranged the other' phrasing implies directional disambiguation of the incoming style; the wiki supports only that the sway indicates a style change. No directional tell exists. CONFIRMED on the same authoritative receipt. (C13 and C14 are one fix: remove the directional framing from the single guidance string.)

### [low] C21: The wiki states Fairytale I - Growing Pains is the quest that MUST be completed for fairy ring access, not Fairytale II (full completion). Fairytale II requires only partial completion (up to receiving Fairy Godfather permission). Gating the CIR waypoint on full Fairytale II completion is stricter than the actual requirement and will hide the waypoint from players who have completed Fairytale I and partially completed Fairytale II but not finished it.

- **Data says:** waypoints[0].requirements.quests: ["FAIRYTALE_II__CURE_A_QUEEN"]
- **Wiki says (raw):** Note that Fairytale I - Growing Pains must be completed to access the fairy rings. Fairytale II - Cure a Queen does not need to be completed, and players do not need any of the skill requirements.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Replace FAIRYTALE_II__CURE_A_QUEEN with FAIRYTALE_I__GROWING_PAINS (or a partial-Fairytale-II marker if the schema supports it). The hard gate for basic fairy ring access is Fairytale I completion.
- **Skeptic receipt:** Fairy_ring wiki: 'Note that Fairytale I - Growing Pains must be completed to access the fairy rings. Fairytale II - Cure a Queen does not need to be completed, and players do not need any of the skill requirements.' Data line 1214: "FAIRYTALE_II__CURE_A_QUEEN" under Fairy ring CIR waypoint requirements.quests.
- **Skeptic reasoning:** waypoints[0] (the 'Fairy ring CIR' waypoint, lines 1207-1217) gates on quests:['FAIRYTALE_II__CURE_A_QUEEN']. The wiki authoritatively states only Fairytale I - Growing Pains MUST be completed for fairy ring access; Fairytale II 'does not need to be completed'. Gating on full Fairytale II is stricter than the real requirement and would hide the fairy-ring travel option from accounts that have working fairy rings (Fairytale I done) but have not finished Fairytale II. Not account-type-legitimate -- fairy rings work for any account post-Fairytale-I. Severity is low: alternate waypoints (Walk from Shayzien) and the Rada's blessing 4 / Mount Karuulm alternatives remain, so it degrades rather than breaks routing. Caveat for the fixer: confirm FAIRYTALE_I__GROWING_PAINS is a valid quest token in the schema before substituting; the gate being wrong is what stands, not the exact replacement token. Same wrong gate also appears at line 4491 and is worth checking under its own source.

## Corporeal Beast

### [high] C3: The alternative route via 'Wilderness Volcano teleport -> run south -> lever room -> pull lever' is not documented anywhere on the wiki and contradicts its explicit advice to avoid Wilderness entry entirely. The wiki names no lever room for Corp access. The Wilderness lever near Edgeville teleports players to level 50+ Wilderness -- far north of Corp's level 21 entrance -- making this route both fabricated and dangerous.

- **Data says:** POH jewellery box with Fancy upgrade can access Games necklace teleport, then teleport to Wilderness Volcano, then run south to the lever room and pull the lever to enter the Corporeal Beast cave
- **Wiki says (raw):** The Corporeal Beast can be found in a cave at level 19-20 Wilderness, which is surrounded by hostile ents and black unicorns. As the area is frequently visited by PKers, entering through the Wilderness is not recommended. Instead, use a games necklace and teleport to the Corporeal Beast. The cave in which the Corporeal Beast resides is not part of the Wilderness.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Corporeal_Beast/Strategies
- **Suggested fix:** Remove the Wilderness Volcano + lever room conditional alternative. The only documented alternative to the games necklace teleport is walking through level 21 Wilderness from the surface entrance east of the Graveyard of Shadows -- which the wiki explicitly discourages due to PKers.
- **Skeptic receipt:** Wiki (Corporeal_Beast/Strategies): "use a games necklace and teleport to the Corporeal Beast. The cave in which the Corporeal Beast resides is not part of the Wilderness." The strategy page documents only the direct games-necklace teleport and advises against Wilderness entry; it names no Wilderness Volcano hop, no lever, and no lever room for Corp access.
- **Skeptic reasoning:** The conditionalAlternative in the data reads: 'POH jewellery box -> Games necklace -> Wilderness Volcano, then run south to the lever room and pull the lever to enter the Corporeal Beast cave'. A Fancy Jewellery Box does grant Games necklace teleports, but the Corp-relevant Games necklace destination is the direct 'Corporeal Beast' teleport, NOT 'Wilderness Volcano'. The Games necklace has no Wilderness Volcano option, and Corp's cave is entered directly from the games-necklace tile -- there is no lever-room/pull-lever step. The route as written is fabricated and routes a player into deep Wilderness. This is a genuine data bug in the guidance text, not a legitimate mechanic. Note: the alternative should simply mirror the primary step (POH jewellery box -> Games necklace -> Corporeal Beast); the Volcano/lever room language is the wrong part. Verified against both WebFetch of the Strategies page and the MCP wiki context.

## Kalphite Queen

### [high] C2: Desert amulet 4 is an Elite Desert Diary reward, not Hard. The Hard Desert Diary provides permanent ropes at the Kalphite Lair entrance; the Elite Diary provides the amulet (with the Kalphite Cave teleport). The gate category is wrong -- players following this guidance expecting the amulet from the Hard diary will not have it.

- **Data says:** Desert amulet 4 teleports directly to Kalphite Cave entrance as Hard Desert Diary reward
- **Wiki says (raw):** The desert amulet 4 is a reward from completing the Elite Desert Diary. ... Unlimited teleports to Nardah and the Kalphite Cave
- **Wiki URL:** https://oldschool.runescape.wiki/w/Desert_amulet_4
- **Suggested fix:** Change 'Hard Desert Diary reward' to 'Elite Desert Diary reward' in the conditionalAlternatives description.
- **Skeptic receipt:** Desert Diary Rewards -- Hard Diary: "Ropes are now permanently secured once placed at the Kalphite Lair entrance and Queen's Lair". Elite Diary (Desert amulet 4): "Unlimited teleports to the entrance of the Kalphite Cave". Plus Desert_amulet_4 page: "The desert amulet 4 is rewarded by the Elite Desert Diary tier."
- **Skeptic reasoning:** The data gates the amulet teleport on requirements.diaries=["DESERT_HARD"] and the descriptions say "Hard Desert Diary reward" (drop_rates.json lines 1484-1486, plus the parent travelTip line 1481). The wiki is unambiguous: the desert amulet 4 is an Elite-tier reward, while the Hard tier gives only the permanent-rope benefit at the Kalphite Lair. This is an account-progress correctness bug: an account that has completed only the Hard diary would be told it can teleport with amulet 4 when it cannot have the amulet. This is a real gate-tier error, not a multi-source/variant/account-type nuance. STANDS as a real bug; fix the diary tier to DESERT_ELITE and the description text accordingly.

## Phantom Muspah

### [high] C5: The Quetzal Transport System serves 14 Varlamore-region landing sites only. No destination near Phantom Muspah, Ghorrock Dungeon, or the Ancient Prison exists. The claim that the quetzal whistle has a 'Phantom Muspah destination' is fabricated.

- **Data says:** Quetzal whistle (basic, enhanced, perfected, or perfected-infinite variants) can be right-clicked to access a destination menu that includes the Phantom Muspah destination.
- **Wiki says (raw):** A basic quetzal whistle is a teleportation item that takes players to any built Quetzal Transport System landing site. [Landing sites listed:] Aldarin, Auburnvale, Civitas illa Fortis, Hunter Guild, Quetzacalli Gorge, Sunset Coast, Tal Teklan, The Teomat, Cam Torum entrance, Colossal Wyrm Remains, Fortis Colosseum, Kastori, Outer Fortis, Salvager Overlook
- **Wiki URL:** https://oldschool.runescape.wiki/w/Quetzal_Transport_System
- **Suggested fix:** Remove the quetzal whistle conditional alternative entirely, or replace it with a correct travel alternative (e.g., Ring of Shadows with frozen tablet, which does teleport to Ghorrock Dungeon).
- **Skeptic receipt:** wiki_lookup Quetzal Transport System: 'There are currently 14 landing sites in total ... Aldarin | Auburnvale | Civitas illa Fortis | Hunter Guild | Quetzacalli Gorge | Sunset Coast | Tal Teklan | The Teomat | Cam Torum entrance | Colossal Wyrm Remains | Fortis Colosseum | Kastori | Outer Fortis | Salvager Overlook' -- and WebFetch confirms: 'The article does not mention any landing sites near Phantom Muspah, the Ancient Prison, Ghorrock Dungeon, or Weiss.'
- **Skeptic reasoning:** The data file (drop_rates.json line 1616) asserts a Quetzal whistle 'Phantom Muspah destination' that lands 'directly at the Ancient Prison entrance.' The authoritative wiki_lookup of the Quetzal Transport System returns the COMPLETE list of all 14 landing sites, every one in the Varlamore region (Aldarin, Auburnvale, Civitas illa Fortis, Hunter Guild, Quetzacalli Gorge, Sunset Coast, Tal Teklan, The Teomat, Cam Torum entrance, Colossal Wyrm Remains, Fortis Colosseum, Kastori, Outer Fortis, Salvager Overlook). None is near Phantom Muspah / Ancient Prison / Ghorrock / Weiss (the Ancient Prison is in the far north, reached via Weiss). There is no 'Phantom Muspah destination' on any quetzal whistle. Refutation vectors fail: this is not a multi-source clog item, not a variant id, not account-type dependent, and not stale (the system has always been Varlamore-only). The conditionalAlternative is fabricated.

### [high] C6: The Ancient Prison entrance is not a quetzal whistle destination. The Quetzal Transport System is exclusive to the Varlamore region and contains no Ghorrock or Ancient Prison landing site. This claim is fabricated.

- **Data says:** The Quetzal whistle feature allows skipping the Weiss to Ghorrock Dungeon overland route by landing directly at the Ancient Prison entrance.
- **Wiki says (raw):** A basic quetzal whistle is a teleportation item that takes players to any built Quetzal Transport System landing site. [Landing sites listed:] Aldarin, Auburnvale, Civitas illa Fortis, Hunter Guild, Quetzacalli Gorge, Sunset Coast, Tal Teklan, The Teomat, Cam Torum entrance, Colossal Wyrm Remains, Fortis Colosseum, Kastori, Outer Fortis, Salvager Overlook
- **Wiki URL:** https://oldschool.runescape.wiki/w/Quetzal_Transport_System
- **Suggested fix:** Remove or replace this conditional alternative. The correct fast method is the Ring of Shadows (frozen tablet) which teleports directly to Ghorrock Dungeon per the wiki.
- **Skeptic receipt:** wiki_lookup Quetzal Transport System landing sites (all 14, Varlamore-only) listed above; WebFetch: 'There is no information about transport destinations in the locations you referenced [Phantom Muspah, Ancient Prison, Ghorrock Dungeon, Weiss].' Data line 1600 already provides the correct travel: 'Use icy basalt to teleport to Weiss, or Ring of Shadows -> Ghorrock Dungeon.'
- **Skeptic reasoning:** Same fabricated conditionalAlternative as C5 (drop_rates.json lines 1606-1618). It claims the quetzal whistle skips the Weiss->Ghorrock overland route by landing at the Ancient Prison entrance. The authoritative landing-site list (all Varlamore) contains no Ghorrock or Ancient Prison site, so the whistle cannot perform this skip. The genuine fast methods to Phantom Muspah are Icy basalt -> Weiss and Ring of Shadows -> Ghorrock Dungeon, which the primary guidance step already states (line 1600). The quetzal alternative must be removed.

### [blocker] C10: The melee form of Phantom Muspah requires Protect from Melee, not Protect from Magic. Magic is the recommended offensive style against the melee form (it is 'weakest to magic'), but the defensive prayer is Protect from Melee. Instructing the player to use Protect from Magic during the melee form will result in taking full melee damage.

- **Data says:** Phantom Muspah's shielded/melee form requires the use of Protect from Magic.
- **Wiki says (raw):** for 0 damage if protect from melee is active
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies
- **Suggested fix:** Change guidance to 'Use Protect from Melee during the melee/shielded form.' Note that Magic is recommended offensively (freeze/kite), but the prayer that blocks the melee attacks is Protect from Melee.
- **Skeptic receipt:** Phantom Muspah/Strategies: 'Muspah will chase and attack player same tick, for 0 damage if protect from melee is active.' Confirmed twice via WebFetch of the Strategies page: 'Use Protect from Melee during the melee phase' / 'Protect from Melee is the prayer that guards against the melee form's attacks.'
- **Skeptic reasoning:** The guidance (drop_rates.json line 1630) instructs 'Shielded/Melee form -> Protect from Magic.' The Phantom Muspah/Strategies page is explicit that the melee form is countered by Protect from Melee, and that with it active the melee hit deals 0 damage. Using Protect from Magic during the melee form would let the player take full melee damage -- a genuine gameplay-harming error, not a misread. Refutation vectors fail: this is not account-type dependent, not a variant, and the strategies page is current. Note Magic IS the recommended OFFENSIVE style vs the melee form, which may be the source of the confusion, but the defensive overhead must be Protect from Melee.

### [medium] C11: The wiki describes a knockback-and-heal mechanic tied to spikes (not cyan orbs). No cyan-colored teleport orbs are described anywhere on the Phantom Muspah main page or strategies page. The only orb described is a purple magic orb. The 'cyan teleport heal-orbs' label appears fabricated; the heal mechanic is actually the spike attack.

- **Data says:** Phantom Muspah spawns cyan teleport heal-orbs that the player must dodge.
- **Wiki says (raw):** which should be dodged or else it will knock the player aside and heal the Muspah based on damage dealt.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phantom_Muspah
- **Suggested fix:** Replace 'cyan teleport heal-orbs' with accurate language referencing the spikes/shockwave mechanic: 'Dodge spikes during transformation -- they heal Muspah if they damage the player.'
- **Skeptic receipt:** Phantom Muspah main page: 'When changing forms it spawns a few spikes around the arena including one under the player, which should be dodged or else it will knock the player aside and heal the Muspah based on damage dealt.' WebFetch confirms: 'The document contains no mention of cyan orbs, teleport heal-orbs, or purple prayer-drain orbs ... the mechanic is the spike attack.'
- **Skeptic reasoning:** The guidance (drop_rates.json line 1630) labels the transformation heal mechanic 'cyan teleport heal-orbs.' The authoritative Phantom Muspah main page describes this mechanic as SPIKES spawned during form changes, with no cyan orb anywhere. No 'cyan teleport heal-orb' entity exists on either the main or strategies page. The fabricated label should be replaced with 'spikes.' This is a naming/label inaccuracy of lower impact than C10: the data's parenthetical '(do NOT stand on the spike tile during a transformation)' already gives the correct behavioral instruction and correctly names the spike, so the player is not misled into wrong play -- only the prepended 'cyan teleport heal-orbs' label is invented. Confirmed as a real (low/medium) data inaccuracy worth correcting, but it does not cause incorrect actions.

## Duke Sucellus

### [blocker] C2: The guidance recommends crush weapons, but Duke Sucellus is weak to slash and has high crush Defence -- crush weapons will frequently miss. The recommended weapons are slash-based (scythe of vitur, emberlight, soulreaper axe).

- **Data says:** Crush weapon (Bandos godsword or Inquisitor's mace) is required at Duke Sucellus
- **Wiki says (raw):** Duke Sucellus is weak to slash attacks. It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Replace 'crush weapon (Bandos godsword or Inquisitor's mace)' with a slash weapon recommendation (e.g. scythe of vitur, emberlight, or soulreaper axe).
- **Skeptic receipt:** WebFetch of https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies: "Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight." and "It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence." Top recommended weapons: Scythe of Vitur, Emberlight, Soulreaper Axe.
- **Skeptic reasoning:** I read the actual data: every guidance step recommends a crush weapon. Line 1742 'Bring crush weapon (Bandos godsword or Inquisitor's mace)'; line 1755 same; line 1781 'use Protect from Melee with crush weapons (Bandos godsword or Inquisitor's mace) since Duke resists slash and stab'. The wiki Strategies page is unambiguous that Duke is weak to slash and that crush is the worst choice. This is not an account-type, variant, or multi-source nuance -- it is a flat combat-meta error that steers every account toward the highest-miss-rate style. Note: Bandos godsword is in fact a SLASH weapon, so even the data's own example contradicts its 'crush' label, but the explicit instruction text and the rationale ('Duke resists slash and stab') make the intended recommendation crush, which is wrong.

### [blocker] C6: The guidance has the combat meta exactly inverted: it states Duke 'resists slash and stab' when the wiki explicitly states he is weak to slash and resists crush. Following this guidance directly causes the player to use the worst possible weapon style.

- **Data says:** Duke Sucellus resists slash and stab, requiring crush weapons
- **Wiki says (raw):** Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight. It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Replace 'since Duke resists slash and stab' with 'since Duke is weak to slash' and recommend slash weapons instead of crush weapons.
- **Skeptic receipt:** Data line 1781 (raw): "...use Protect from Melee with crush weapons (Bandos godsword or Inquisitor's mace) since Duke resists slash and stab." Wiki (Strategies): "Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight... It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence."
- **Skeptic reasoning:** The data literally contains the inverted-meta string in the combat step (line 1781): '...use Protect from Melee with crush weapons (Bandos godsword or Inquisitor's mace) since Duke resists slash and stab.' The wiki states the exact opposite: Duke is weak to slash and resists crush (high crush Defence). This is a direct, verbatim contradiction, not a misreading of context. Same root cause as C2; both receipts hold independently against the data text. This is the canonical blocker -- following it makes the player use the worst style.

### [high] C8: The guidance implies mining materials happens during the combat phase. The wiki is clear this is a preparation phase activity performed while the boss is asleep -- before combat begins. The materials are gathered from the asylum before awakening Duke.

- **Data says:** Arder powder, musca powder, and salax salt can be mined from asylum vents during Duke Sucellus fight
- **Wiki says (raw):** Upon entering the prison, Duke Sucellus is initially asleep and must be weakened with arder-musca poisons. The salt piles are found in the centre of the asylum, surrounded by gas vents... the mushrooms are found at the end of the opposing sides of the asylum, past the extremities.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Move the material-gathering and poison-crafting instruction to the preparation/travel step (before the combat step), not the 'Kill Duke Sucellus' combat step.
- **Skeptic receipt:** WebFetch (Strategies): "Upon entering the prison, Duke Sucellus is initially asleep and must be weakened with arder-musca poisons, made from resources found within the asylum." Data line 1781 places 'Mine arder powder, musca powder and salax salt... craft arder-musca poisons' inside the combat step keyed on completionCondition ACTOR_DEATH.
- **Skeptic reasoning:** The data folds the prep-phase wake-up activity into the combat step (line 1781): 'Kill Duke Sucellus. Mine arder powder, musca powder and salax salt from the asylum vents, craft arder-musca poisons, then use Protect from Melee...'. The wiki is explicit that gathering materials and applying the arder-musca poisons happens while Duke is asleep, BEFORE combat begins ('Upon entering the prison, Duke Sucellus is initially asleep and must be weakened with arder-musca poisons'). Placing it inside the ACTOR_DEATH 'Kill Duke Sucellus' step misrepresents sequence. This is a real guidance-placement defect, not an account/variant nuance. Severity is appropriately high (sequence-misleading, not game-breaking).

### [medium] C9: Arder-musca poisons are crafted and used in the preparation phase to wake the boss, not crafted mid-fight. The guidance step places this in the combat description, misleading players about when it occurs.

- **Data says:** Arder-musca poisons can be crafted and used during the Duke Sucellus fight
- **Wiki says (raw):** Upon entering the prison, Duke Sucellus is initially asleep and must be weakened with arder-musca poisons. Making this poison requires six portions of each... place them in both Fermentation Vats and let it ferment for several seconds before collecting two finished poisons.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Clarify that crafting and deploying the poisons is the wake-up/preparation step before combat, not an action during the combat itself.
- **Skeptic receipt:** WebFetch (Strategies): "Duke Sucellus is initially asleep and must be weakened with arder-musca poisons, made from resources found within the asylum." Data line 1781 places 'craft arder-musca poisons' within the 'Kill Duke Sucellus' (ACTOR_DEATH) step.
- **Skeptic reasoning:** Accurate but a strict subset of C8 -- the same poison-timing placement issue, narrowed to the crafting verb. The wiki confirms poison crafting/use is the asleep-phase wake-up step, and the data places it in the combat step. The finding holds, but it is redundant with C8 and should be merged into a single fix (relocate the gather-and-craft instruction to the prep/arena-entry step), not logged as a separate edit.

### [medium] C10: Falling ice patches are a dungeon-navigation hazard encountered while traversing the Ghorrock Prison corridors to reach the boss, not a mechanic during the Duke Sucellus fight itself. The combat step description incorrectly places this as a fight mechanic.

- **Data says:** Duke Sucellus fight features falling ice patches as a mechanic to dodge
- **Wiki says (raw):** ice falls will randomly strike on a tile, freezing the player for 2 ticks (1.2 seconds) and dealing up to 18 damage. These occur specifically in the area right before the second staircase on both sides, and are indicated by a shadow looming over the tile they will strike on.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Move the falling ice dodge instruction to the dungeon navigation step (guidanceSteps[1]) rather than the combat step, or clarify it occurs in the approach corridors.
- **Skeptic receipt:** WebFetch (Strategies): "ice falls will randomly strike on a tile, freezing the player for 2 ticks (1.2 seconds) and dealing up to 18 damage. These occur specifically in the area right before the second staircase on both sides." Data line 1781 lists 'Dodge the falling ice patches' inside the combat/ACTOR_DEATH step rather than the navigation step (guidanceSteps[1]).
- **Skeptic reasoning:** The data combat step (line 1781) instructs 'Dodge the falling ice patches' as a fight mechanic. The wiki places ice falls in the dungeon-approach corridors -- 'specifically in the area right before the second staircase on both sides' -- i.e. during navigation (the data's own step 2, 'Navigate through Ghorrock Prison to the Asylum stairs', worldPlane 2), not in the boss arena. This is a distinct placement error from C8/C9 (different physical location, different step). Considered and rejected the refutation that ice could also fall in-arena: the wiki localizes it to the pre-second-staircase corridor only. Medium severity is fair -- it does not invert a meta, it misattributes a hazard to the wrong step.

## The Leviathan

### [high] C1: The rowboat is at Wizard Persten's camp east of the Abyssal Rift (inside the Scar), not in the Wizards' Tower basement. The Wizards' Tower contains a portal to the Temple of the Eye, which is an intermediate step to reach the Scar -- a distinct location from the rowboat itself.

- **Data says:** Travel to The Scar by taking the rowboat in the Wizards' Tower basement.
- **Wiki says (raw):** To reach the Leviathan in the Scar, players must proceed east of the Abyssal Rift towards Wizard Persten's camp. A rowboat there can then be used to reach the island where the Leviathan is located.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan
- **Suggested fix:** Change to: 'Travel to The Scar. From the Wizards' Tower basement portal, proceed through the Temple of the Eye to the Abyssal Rift, then head east to Wizard Persten's camp and take the rowboat to the Leviathan's island.' Or simply omit the Wizards' Tower detail and say 'Take the rowboat at Wizard Persten's camp east of the Abyssal Rift to reach the Leviathan.'
- **Skeptic receipt:** To reach the Leviathan in the Scar, players must proceed east of the Abyssal Rift towards Wizard Persten's camp. A rowboat there can then be used to reach the island where the Leviathan is located. ... Players must first access The Scar through the Temple of the Eye [by] talking to the Catalytic Guardian in the Temple of the Eye. (oldschool.runescape.wiki/w/The_Leviathan)
- **Skeptic reasoning:** The guidance text says 'Travel to The Scar by taking the rowboat in the Wizards' Tower basement.' The Wizards' Tower basement contains the portal to the Temple of the Eye, not a rowboat -- the entry's own locationDescription field confirms this ('The Scar, accessed via Temple of the Eye beneath Wizards' Tower'). The actual rowboat is at Wizard Persten's camp, east of the Abyssal Rift, inside the Scar. The text conflates two distinct, geographically separate things. No multi-source/variant/account-type/staleness vector applies -- this is a player-facing route error, and wiki_updates shows zero Leviathan edits since March 2026, so it is not drift. The suggested fix is sound.

### [medium] C2: The Twisted bow fires arrows, not bolts -- pairing it with 'diamond bolts (e)' is factually incorrect as that ammunition is for crossbows. Diamond bolts (e) are not mentioned anywhere on the Leviathan wiki or strategies page. The primary crossbow recommendation is the Zaryte crossbow (not Twisted bow), and the Twisted bow is preferred only in Awakened mode.

- **Data says:** Bring ranged setup (Bow of faerdhinen or Twisted bow with diamond bolts (e))
- **Wiki says (raw):** For backside attacks: The twisted bow 'usually deals higher damage than crossbow attacks' ... the Zaryte crossbow is best in slot against Leviathan until it reaches 450 HP ... The Bow of faerdhinen and full Crystal armour doesn't have the damage falloff
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan/Strategies
- **Suggested fix:** Remove 'diamond bolts (e)' from the Twisted bow pairing. Either specify 'Zaryte crossbow' for the crossbow option or clarify that Twisted bow is the Awakened-mode recommendation. A corrected form: 'Bow of faerdhinen (with Crystal armour) or Zaryte crossbow, or Twisted bow for Awakened mode.'
- **Skeptic receipt:** Zaryte Crossbow: Uses 'Ruby dragon bolts (e)' as the primary ammunition choice, with 'Ruby bolts (e)' as an alternative. ... No diamond bolts or dragon arrows are mentioned in the equipment tables. ... Twisted Bow ... is a ranged weapon that doesn't require bolts or arrows. (oldschool.runescape.wiki/w/The_Leviathan/Strategies)
- **Skeptic reasoning:** The guidance pairs 'Twisted bow' with 'diamond bolts (e)'. In OSRS the Twisted bow is a bow that fires arrows; bolts are crossbow-only ammunition, so a Twisted-bow-plus-diamond-bolts loadout is mechanically impossible regardless of account type. Diamond bolts (e) appear nowhere on the Leviathan or Strategies pages; the wiki's bolt recommendation for the Zaryte crossbow is ruby bolts (e) / ruby dragon bolts (e). This is a flat factual error, not a variant or multi-source nuance. Note: the same erroneous phrase is duplicated in the conditionalAlternatives Ring-of-Shadows description (line 1909), so the fix must touch both occurrences.

### [low] C9: The wiki caps the debris roar at 7 chip damage and describes permanent rocks as restricting movement/line-of-sight. There is no wiki support for the claim that permanent rock spawns can one-shot lower-defence accounts. The debris chip damage (up to 7) would not one-shot any account.

- **Data says:** step at least one tile away from the falling-debris warning at the end of each volley to avoid the permanent rock spawns that one-shot lower-defence accounts.
- **Wiki says (raw):** Players will take up to 7 chip damage from the roar but can avoid the debris by moving at least 1 tile away.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan/Strategies
- **Suggested fix:** Replace 'that one-shot lower-defence accounts' with 'that restrict movement and can block line of sight for the remainder of the fight'.
- **Skeptic receipt:** At the end of each volley, the boss will roar and cause debris to fall around the arena, with one falling down on the player's location as a permanent spawn for the remainder of the fight. Players will take up to 7 chip damage from the roar but can avoid the debris by moving at least 1 tile away. (oldschool.runescape.wiki/w/The_Leviathan/Strategies)
- **Skeptic reasoning:** The guidance claims the falling-debris/permanent rock spawns 'one-shot lower-defence accounts.' The wiki caps the roar at up to 7 chip damage and describes the permanent debris as a movement/line-of-sight obstruction that accumulates through the fight, never a one-shot damage hazard. 7 chip damage cannot one-shot any account. The 'one-shot lower-defence accounts' clause is unsupported and misleading; the rest of the step (step 1 tile away, prayer-swap by orb colour) is correct. Suggested fix (replace with 'restrict movement and can block line of sight for the remainder of the fight') matches the wiki.

## The Whisperer

### [blocker] C4: Twisted bow fires arrows, not bolts -- diamond bolts (e) are crossbow ammunition and cannot be loaded into a Twisted bow. This is a mechanically impossible equipment combination. Additionally, the wiki does not mention Twisted bow at all for this fight; it recommends magic as the primary style and Venator bow for the souls phase only.

- **Data says:** Twisted bow with diamond bolts (e) is a viable ranged weapon for The Whisperer fight
- **Wiki says (raw):** A fast ranged weapon is necessary for the souls phase. Mid-level players may use the Rune thrownaxe's special attack, while the Venator bow is best-in-slot.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Remove the Twisted bow + diamond bolts (e) combination entirely. If a ranged option must be listed for the souls phase, use Venator bow (best-in-slot) or a blowpipe. For main DPS, replace with a magic weapon per the wiki's primary recommendation.
- **Skeptic receipt:** Wiki (The_Whisperer/Strategies): 'A fast ranged weapon is necessary for the souls phase... the Venator bow is best-in-slot.' Twisted bow / diamond bolts (e) not mentioned. Game mechanic: a bow cannot load crossbow bolts.
- **Skeptic reasoning:** The guidance text (drop_rates.json lines 2048 and 2064) literally reads 'ranged setup (Bow of faerdhinen or Twisted bow with diamond bolts (e))'. A Twisted bow is a BOW and fires arrows; diamond bolts (e) are CROSSBOW ammunition and cannot be loaded into any bow. This is a mechanically impossible equipment combination, not a clog itemId, not a variant, not account-type-dependent, and not staleness (wiki_updates returned 0 changes since 2026-01-01). The refutation vectors do not apply to an impossible gear pairing. Independently, the wiki recommends magic as the primary style and only a FAST ranged weapon (Venator bow BiS) for the souls phase, never the Twisted bow. The bug stands regardless of the optimal-weapon debate: 'Twisted bow with diamond bolts (e)' can never exist in-game.

### [high] C9: The Shadow Realm mechanic involves killing soul groupings through combat (attacking lost souls), not stepping on tiles. There is no 'four correct tiles' interaction and no shield-banking mechanic described anywhere on the wiki. The soul sets have 2, 3, 3, or 4 members (not a uniform 'four tiles'), and the consequence of the phase is damage/healing, not a shield reduction.

- **Data says:** Stepping on four correct tiles in the Shadow Realm banks The Whisperer's shield
- **Wiki says (raw):** The Whisperer will appear in the centre and summon twelve lost souls, beginning to siphon energy from them for a powerful chant that completes in 20 ticks (12 seconds). [Players must] activate their blackstone fragment to enter the Shadow Realm and kill at least one set of souls chanting the same phrase.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Replace with: enter the Shadow Realm via the blackstone fragment during the Soul Siphon special attack, then kill at least one complete set of lost souls (grouped by chant phrase: Vita/Oratio/Sanitas/Mors) before the 20-tick timer expires to prevent 50 damage and 100 HP heal.
- **Skeptic receipt:** Wiki (The_Whisperer/Strategies): 'Players must activate their blackstone fragment to enter the Shadow Realm and kill at least one set of souls chanting the same phrase. If the player fails to kill a full set of souls within the time limit, the chant will complete, dealing 50 damage to the player and heals her for 100 health. Souls can only be damaged in the Shadow Realm.' Wiki (The_Whisperer): 'There is no traditional shield mechanic.'
- **Skeptic reasoning:** The guidance (drop_rates.json line 2096) says 'use the blackstone fragment to enter the Shadow Realm and step on the four correct tiles to bank her shield'. The wiki describes the souls/Soul-Siphon phase as entering the Shadow Realm and KILLING a set of souls chanting the same phrase; failure deals 50 damage and heals the boss 100. There is no tile-stepping interaction and no 'shield' mechanic anywhere on either wiki page (confirmed on both The_Whisperer and The_Whisperer/Strategies). This is a fabricated mechanic, not a variant/account/staleness nuance (wiki_updates: 0 changes since 2026-01-01). The phrase 'step on the four correct tiles to bank her shield' describes gameplay that does not exist.

### [low] C6: The ranged projectiles are described as purple barbs on the wiki, not grey-barb. The prayer recommendation (Protect from Missiles) is correct. This is a cosmetic/visual description error that does not affect gameplay.

- **Data says:** The Whisperer uses grey-barb ranged volleys that require Protect from Missiles prayer
- **Wiki says (raw):** The ranged shots are purple barbs, while the magic shots are blue orbs.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer
- **Suggested fix:** Change 'grey-barb ranged volleys' to 'purple-barb ranged volleys' to match the wiki's visual description.
- **Skeptic receipt:** Wiki (The_Whisperer): 'The ranged shots are purple barbs, while the magic shots are blue orbs.'
- **Skeptic reasoning:** The guidance (drop_rates.json line 2096) says 'grey-barb ranged volleys'. The wiki states the ranged shots are PURPLE barbs. The prayer recommendation (Protect from Missiles) is correct, so the only error is the projectile color descriptor. This is a genuine but purely cosmetic text mismatch -- it does not change which prayer to use or any gameplay outcome -- hence low severity. No variant/multi-source/account vector applies; wiki_updates shows 0 changes since 2026-01-01 so it is not staleness.

## Vardorvis

### [medium] C9: The axes and ground cracks are two distinct special attacks, not a single mechanic. Axes are brought up by Strangler's tendrils and released across the arena. Ground cracks come from the Darting Spikes attack and release tendrils/vines -- not axes. The guidance incorrectly conflates these into 'axes spawned from ground cracks'.

- **Data says:** keep moving to dodge the spinning axes spawned from ground cracks
- **Wiki says (raw):** Swinging Axes: 'The Strangler's tendrils will bring up Vardorvis' axes into the arena before releasing them across the arena.' Darting Spikes: 'Vardorvis will dart around the player, causing cracks in the ground which will release tendrils after a few seconds.'
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Separate the two mechanics: 'dodge the swinging axes (brought up by tendrils) and avoid standing in ground cracks from the Darting Spikes attack (which erupt into tendrils).'
- **Skeptic receipt:** Swinging Axes: 'The Strangler's tendrils will bring up Vardorvis' axes into the arena before releasing them across the arena.' Darting Spikes: 'Vardorvis will dart around the player, causing cracks in the ground which will release tendrils after a few seconds, dealing up to 25 damage and healing him for half the damage dealt.'
- **Skeptic reasoning:** The fight step reads 'keep moving to dodge the spinning axes spawned from ground cracks'. The wiki establishes these as two distinct mechanics with different sources. Swinging Axes are brought into the arena by the Strangler's tendrils and released across the arena (count scales with boss HP). Darting Spikes are the ground-crack mechanic, and those cracks release TENDRILS (not axes) that deal up to 25 damage and heal Vardorvis. So 'axes spawned from ground cracks' misattributes the axe source: axes do not come from ground cracks. The guidance conflates the Swinging Axes and Darting Spikes attacks into one phantom mechanic. Refutation vectors do not apply (these are universal fight mechanics, not multi-source/variant/account-type dependent; wiki is the current state). The finding's receipt holds.

### [blocker] C11: The wiki places Strangled as environmental hazards encountered while travelling to the boss, not as spawns inside the Vardorvis fight arena. The wiki describes no Strangled mechanic occurring during the fight itself.

- **Data says:** kill the Strangled spawns immediately or they will heal him
- **Wiki says (raw):** All routes will require player to traverse the Stranglewood, and players will be running past several Strangled and a Strangled Boar while reaching the boss.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Remove the Strangled-spawn instruction from the fight step. Strangled are path hazards before the fight, not arena mechanics. If the Awakened variant spawns them (see guidanceSteps for Vardorvis Awakened), that note belongs only to the Awakened entry.
- **Skeptic receipt:** Strangled Spawns: appear only 'while reaching the boss' on the path through the Stranglewood -- not during the arena fight itself. Strangle: Below 570 health, Vardorvis 'strikes his arm into the ground and entangle the player in tendrils.' Players must destroy 4-6 spores within 5 ticks or suffer high damage.
- **Skeptic reasoning:** The fight step (guidanceSteps[2]) instructs to 'kill the Strangled spawns immediately or they will heal him'. The wiki confirms Strangled creatures are path hazards encountered while traversing the Stranglewood to reach the boss, NOT arena spawns during the fight. There is no in-fight 'Strangled spawn' mechanic. The closest in-fight mechanic is 'Strangle' (below 570 HP), where Vardorvis entangles the PLAYER in tendrils and the player must destroy 4-6 SPORES within 5 ticks to avoid taking high damage themselves -- destroying spores does not prevent the boss healing; they are not 'Strangled spawns'. Vardorvis's healing comes from his own attacks (50% lifesteal) and the Darting Spikes tendrils, not from killing spawns. So placing a 'kill Strangled spawns' instruction in the fight step is wrong; Strangled belong to the travel/path step. Not multi-source/variant/account-type sensitive. The receipt holds.

### [blocker] C12: The wiki has no mention of Strangled spawning inside the fight arena or healing Vardorvis if not killed quickly. This mechanic is entirely absent from the wiki's description of the Vardorvis fight. The Strangled-heals-boss mechanic appears to be fabricated or confused with a different boss's mechanic.

- **Data says:** kill the Strangled spawns immediately or they will heal him
- **Wiki says (raw):** All routes will require player to traverse the Stranglewood, and players will be running past several Strangled and a Strangled Boar while reaching the boss.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Remove the claim that Strangled heal Vardorvis. This mechanic is not described anywhere on the Vardorvis wiki page or strategies page.
- **Skeptic receipt:** Healing Mechanic: 'Both his normal and special attacks will also heal him for 50% of the damage inflicted, rounded down.' Darting Spikes: cracks 'release tendrils after a few seconds, dealing up to 25 damage and healing him for half the damage dealt.' (No mechanic anywhere states that killing Strangled spawns heals Vardorvis; Strangled appear only on the path, not in the arena.)
- **Skeptic reasoning:** Same clause as C11 ('kill the Strangled spawns immediately or they will heal him'), but this finding targets the specific causal claim that Strangled spawns heal Vardorvis. The wiki has no fight mechanic where killing Strangled (or any spawn) heals the boss. Vardorvis's healing is self-generated: 'Both his normal and special attacks will also heal him for 50% of the damage inflicted, rounded down,' and the Darting Spikes tendrils heal him for half the damage they deal. The healing-via-spawns mechanic is not described anywhere on the page and is a confusion/fabrication. Note: C11 and C12 are two facets of the SAME defective clause (mis-located entity + fabricated heal causality); a single fix to that clause resolves both. Confirmed independently as a real error with a quoted authoritative receipt.

## Duke Sucellus (Awakened)

### [blocker] C1: The guidance inverts the combat meta. Duke Sucellus is weak to slash (defence +65), not crush (defence +190). Recommending crush as mandatory and Inquisitor's mace (a crush weapon) as a main weapon directly contradicts the wiki. Saradomin sword is a slash weapon so that part is incidentally correct, but the overall framing of 'crush is mandatory' is a blocker-level error.

- **Data says:** Crush is mandatory for Duke Sucellus (Awakened); Bandos godsword is recommended for spec, Inquisitor's mace or Saradomin sword as main weapon
- **Wiki says (raw):** Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight. It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Replace the crush framing with slash. Recommended main weapons should be slash-based (e.g. Scythe of vitur, Emberlight, Soulreaper axe). Remove Inquisitor's mace as a recommendation. Bandos godsword for spec remains appropriate.
- **Skeptic receipt:** Defence bonuses: Stab +255, Slash +65, Crush +190. Strategies page: "Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight. It is not recommended to use an elder maul or dragon warhammer because it is much more likely to miss due to Duke Sucellus' high crush Defence."
- **Skeptic reasoning:** The data frames crush as the mandatory style and recommends Inquisitor's mace (a crush weapon) as a main weapon. The authoritative wiki shows Duke's Slash defence is the lowest (+65) and Crush is high (+190), and the strategies page explicitly warns against crush weapons. Recommending crush as mandatory and a crush mace as the main weapon inverts the combat meta. (Bandos godsword for spec is fine; Saradomin sword is slash and incidentally correct. The blocker is the crush-mandatory framing + Inquisitor's mace main-weapon recommendation.)

### [blocker] C2: The claim is directly inverted. Duke has the lowest defence against slash (+65) making it the preferred attack style, and the highest resistance to stab (+255). Claiming Duke resists slash is the opposite of the wiki.

- **Data says:** Duke resists slash and stab attacks
- **Wiki says (raw):** Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight. [Defence bonuses: Stab +255, Slash +65, Crush +190]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies
- **Suggested fix:** Correct to: Duke resists stab and crush; slash is the preferred attack style.
- **Skeptic receipt:** Defence bonuses: Stab +255, Slash +65, Crush +190. "Duke Sucellus is weak to slash, so slashing weapons are preferred for the fight."
- **Skeptic reasoning:** The data states Duke resists slash and stab attacks. Slash defence is +65 -- the lowest of the three melee styles and the documented weakness -- so claiming Duke resists slash is directly inverted. He resists stab (+255, highest) and crush (+190); slash is the preferred style.

## The Leviathan (Awakened)

### [high] C7: The rowboat is at Wizard Persten's camp inside The Scar (east of the Abyssal Rift), not in the Wizards' Tower basement. The Wizards' Tower basement contains a portal to the Temple of the Eye, which is a different step in the travel chain. Conflating the two misdirects players to look for a rowboat in the wrong location.

- **Data says:** A rowboat in Wizards' Tower basement provides access to The Scar
- **Wiki says (raw):** "A rowboat there can then be used to reach the island where the Leviathan is located." (The rowboat is east of the Abyssal Rift at Wizard Persten's camp inside The Scar -- reached after entering The Scar, not from the Wizards' Tower basement.)
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan
- **Suggested fix:** Correct the travel description: the rowboat is located east of the Abyssal Rift inside The Scar at Wizard Persten's camp, used to reach the island where the Leviathan is. The Wizards' Tower basement portal leads to Temple of the Eye, which is a separate step.
- **Skeptic receipt:** To reach the Leviathan in the Scar, players must proceed east of the Abyssal Rift towards Wizard Persten's camp. A rowboat there can then be used to reach the island where the Leviathan is located.
- **Skeptic reasoning:** The data places the rowboat (used to reach the Leviathan's island) in the Wizards' Tower basement. The wiki places it inside The Scar at Wizard Persten's camp, east of the Abyssal Rift. The Wizards' Tower basement holds the portal to the Temple of the Eye, which is a distinct earlier step in the travel chain (Temple of the Eye -> talk to Catalytic Guardian -> The Scar -> east to Persten's camp -> rowboat). Conflating the two genuinely misdirects players to hunt for a rowboat where there is only a Temple-of-the-Eye portal. Not a variant, account-type, or multi-source issue; not stale (no Leviathan wiki edits since 2026-04). The receipt directly contradicts the data.

### [medium] C11: The wiki describes permanent debris counts of 1 (normal) and 5 in a '+' formation (Awakened) -- not 6-8 thrown rocks per volley. The '6-8' figure is not supported by the wiki.

- **Data says:** Rocks phase adds 6-8 thrown rocks you must run between
- **Wiki says (raw):** Normal mode: "one falling down on the player's location as a permanent spawn for the remainder of the fight." Awakened mode: "The permanent blocker debris is increased, from one to five dropped in a '+' style formation on the player."
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan/Strategies
- **Suggested fix:** Update to reflect the wiki: Awakened mode drops 5 permanent debris pieces in a '+' formation per end-of-volley roar, not 6-8 thrown rocks.
- **Skeptic receipt:** At the end of each volley, the boss will roar and cause debris to fall around the arena, with one falling down on the player's location as a permanent spawn for the remainder of the fight. ... The permanent blocker debris is increased, from one to five dropped in a '+' style formation on the player.
- **Skeptic reasoning:** The data describes 'Rocks phase adds 6-8 thrown rocks you must run between.' The wiki describes permanent blocker debris dropped at the end of each volley roar: one in normal mode, increased to five in a '+' formation in Awakened mode. There is no wiki-supported '6-8 thrown rocks' volley nor a 'run between thrown rocks' mechanic; the actual mechanic is moving off the single (or '+' shaped) permanent-spawn tile. The '6-8' figure is unsupported and the framing mischaracterizes the blocker-debris mechanic. Caveat noted: the wiki quote enumerates only the permanent debris (1 / 5) and says other debris falls 'around the arena' without counting it, so the receipt does not literally disprove that 6-8 transient pieces fall arena-wide; but the data's number and 'run between' framing are not supported by the authoritative text, and the finding's own corrected value (5 in a '+' for Awakened) is what the wiki states. Stands as a guidance accuracy fix at medium severity, not a blocker.

### [medium] C13: The wiki states moving at least 1 tile away avoids the debris. The claim says 2 tiles are required. The tile threshold is wrong (1, not 2), and the wiki describes the roar chip damage as 'up to 7', not a '60+ unprotectable hit'.

- **Data says:** Stepping at least 2 tiles off the warning tile prevents a 60+ unprotectable hit from debris
- **Wiki says (raw):** "Players will take up to 7 chip damage from the roar but can avoid the debris by moving at least 1 tile away."
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Leviathan/Strategies
- **Suggested fix:** Correct to: moving at least 1 tile away from the warning tile avoids the permanent debris spawn. The roar itself deals up to 7 chip damage regardless.
- **Skeptic receipt:** Players will take up to 7 chip damage from the roar but can avoid the debris by moving at least 1 tile away.
- **Skeptic reasoning:** The data asserts (a) stepping at least 2 tiles off the warning tile is required and (b) failure incurs a '60+ unprotectable hit' from debris. The wiki contradicts both: moving at least 1 tile away avoids the debris, and the roar deals 'up to 7 chip damage' (not 60+). Both the tile threshold (1, not 2) and the damage figure (up to 7 chip, not 60+ unprotectable) are wrong against the authoritative receipt. Not variant/account/staleness dependent. Two factual errors confirmed; medium severity guidance fix.

## The Whisperer (Awakened)

### [blocker] C13: The guidance recommends Bow of faerdhinen (c) with Masori (ranged meta) but the wiki explicitly states magic is the combat meta, with Tumeken's Shadow and Ancestral robes as the top setup. Ranged is only used situationally for the Soul Siphon phase via Venator bow. Following the ranged guidance will produce meaningfully worse kills.

- **Data says:** Bow of faerdhinen (c) plus Masori is the standard meta
- **Wiki says (raw):** Use the best magic gear possible. Tumeken's shadow is listed as the top weapon choice, with Ancestral robes as preferred armor. Ranged is only mentioned for the Soul Siphon phase using a Venator bow.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Replace the gear recommendation with Tumeken's Shadow (or Harmonised Nightmare Staff as alternative) and Ancestral robes. Retain the Venator bow mention only as a situational Soul Siphon weapon.
- **Skeptic receipt:** Wiki Strategies (best weapons): "Tumeken's shadow / Venator bow (for souls)" with alternatives "Sanguinesti staff > Trident of the swamp" and "Mystic smoke staff > Twinflame staff"; best armor "Slayer helmet (i) or Ancestral hat / Ancestral robe top / Ancestral robe bottom". Data line 2748: "Bow of faerdhinen (c) plus Masori is the standard meta."
- **Skeptic reasoning:** The Awakened guidance step (drop_rates.json line 2748) literally states 'Bow of faerdhinen (c) plus Masori is the standard meta.' The wiki's strategy page recommends magic as the primary style, with the ranged Venator bow used only for the souls phase. No ranged setup is presented as the standard meta. Following the data's ranged guidance produces meaningfully worse kills against a magic-meta boss. wiki_updates returned 0 changes since 2024, so this is current, not stale drift.

### [high] C11: The inventory guidance is built around a ranged setup (Saradomin brews, ranged potion, diamond bolts (e)) which contradicts the wiki's magic-based meta. The wiki recommends prayer potions, manta rays/anglerfish, and runes for ice spells -- not brews or bolts. Diamond bolts (e) have no role in the magic-meta fight.

- **Data says:** Bring a full inventory of Saradomin brews and super restores, prayer potions, ranged potion, diamond bolts (e), an Awakener's orb (one per kill), and a blackstone fragment
- **Wiki says (raw):** Essentials: Blackstone fragment (1 slot), Venator bow with arrows (for souls), Special attack weapon (Eldritch Nightmare Staff recommended), 12-13 prayer potions (4-dose), 3-4 manta rays or anglerfish for food, Rune pouch with runes for ice spells or thralls. The boss drops plenty of manta rays to help sustain food.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Update inventory to reflect the magic meta: prayer potions, manta rays or anglerfish, rune pouch (ice spells/thralls), Venator bow with arrows for soul phase, special attack weapon (Eldritch Nightmare Staff), blackstone fragment, Awakener's orb.
- **Skeptic receipt:** Wiki Strategies recommended inventory (verbatim): "Blackstone fragment, Anglerfish, Anglerfish, Anglerfish, Venator bow, Manta ray, Manta ray, Manta ray, Eldritch nightmare staff, Prayer potion(4) [x13], Super restore(4), Book of the dead, Saturated heart, Divine rune pouch, Ring of shadows, Explorer's ring 4, Blood rune, Fire rune, Aether rune, Death rune" -- no Saradomin brew, no ranged potion, no diamond bolts (e). Data line 2748: "Saradomin brews and super restores, prayer potions, ranged potion, diamond bolts (e)".
- **Skeptic reasoning:** The Awakened bank step (line 2748) lists a ranged-centric kit -- 'Saradomin brews and super restores, prayer potions, ranged potion, diamond bolts (e)' -- which contradicts the wiki's magic meta. The wiki's verbatim recommended inventory contains no Saradomin brews, no ranged potion, and no diamond bolts (e); it is prayer potions + anglerfish/manta rays + magic supplies (Eldritch nightmare staff, divine rune pouch, ice-spell runes). Diamond bolts (e) and a ranged potion have no role in the magic-meta fight (Venator bow is used only for souls). Same root error as C13.

### [high] C7: The wiki says full sanity depletion causes death within 10 ticks (a death timer), not a '10 damage per tick' hit. The guidance misrepresents this as a per-tick damage value of 10, which may mislead players into thinking it is survivable by tanking rather than immediately teleporting.

- **Data says:** Letting sanity bar fully deplete results in a 10-tick degen that hits for 10 per tick
- **Wiki says (raw):** If the sanity bar fully depletes, the player will go insane and will quickly die within 10 ticks; therefore, if this occurs, teleportation is mandatory.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Replace '10 per tick' with: sanity depletion causes near-instant death within 10 ticks -- teleport immediately if this occurs.
- **Skeptic receipt:** Wiki Strategies: "If the sanity bar fully depletes, the player will go insane and will quickly die within 10 ticks; therefore, if this occurs, teleportation is mandatory." Data line 2806: "Never let your sanity bar fully deplete or you take a 10-tick degen that hits for 10 per tick."
- **Skeptic reasoning:** The Awakened combat step (line 2806) states sanity depletion causes 'a 10-tick degen that hits for 10 per tick' -- i.e. ~100 total damage, survivable for an account at this content. The wiki describes it as a near-certain-death timer, not chip damage: full sanity depletion makes the player 'go insane' and 'quickly die within 10 ticks', mandating immediate teleport. Misrepresenting a death timer as 10 dmg/tick could lead a player to tank it instead of teleporting. Genuine error.

### [low] C2: The wiki describes awakened mode as having more tentacles and a specific 4-tick attack speed, but does not describe tentacles 'chaining into screech twice as fast'. The Screech is a separate special attack triggered at health thresholds, not chained from tentacles. This framing appears fabricated.

- **Data says:** Tentacle patterns chain into the screech attack twice as fast in Awakened mode
- **Wiki says (raw):** Awakened mode: Attack speed: 4 ticks (2.4 seconds). Tentacle changes: Single tentacle per attack instead of clusters of four [in enrage phase]. Seven in X formation, six in + formation (vs. four) [in main phase].
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Remove the 'chain into screech twice as fast' framing. Describe the awakened differences accurately: more tentacles (7 in X, 6 in + vs 4), 4-tick attack speed in enrage, and the Screech dealing 5 hits at 70% sanity drain per hit.
- **Skeptic receipt:** Wiki Strategies: "When the Whisperer reaches or falls below 720 (80%), 495 (55%), and 270 (30%) hitpoints, she will perform a special attack" -- the Screech is one of three specials in rotation, triggered by HP thresholds, not chained from tentacle attacks. Data lines 2748/2806: "tentacle patterns chain into the screech attack twice as fast".
- **Skeptic reasoning:** The Awakened combat step (line 2806) says 'tentacle patterns chain into the screech attack roughly twice as fast', and the bank step (line 2748) repeats 'tentacle patterns chain into the screech attack twice as fast'. The wiki establishes the Screech is a HP-threshold-triggered special in a rotation, not a continuation chained from the tentacle attack. The 'chains into screech' relationship does not exist; the framing is fabricated.

### [low] C8: The wiki does not describe 'shadow puddles' as a mechanic in the Shadow Realm for awakened mode. The additional hazards in awakened mode are more tentacles (7/6 vs 4) and more souls (16 vs 12), not 'shadow puddles'. This appears to be fabricated terminology.

- **Data says:** Shadow Realm has 3 extra shadow puddles to dodge in Awakened mode
- **Wiki says (raw):** Awakened mode: Tentacles: Seven in X formation, six in + formation (vs. four). Soul Siphon: 16 souls (up from 12); 21-second timer. Corrupted Seeds: 15.6-second timer; stepping dark seeds deals 60-75 damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Whisperer/Strategies
- **Suggested fix:** Replace 'shadow puddles' with accurate awakened mode differences: additional tentacles (7 in X formation, 6 in + formation vs 4) and additional souls in the Soul Siphon (16 vs 12 with a 21-second timer).
- **Skeptic receipt:** Wiki Strategies (three separate fetches): "There is no mention of shadow puddles to dodge anywhere in this guide." / "No mention of 'shadow puddles' appears in this document." Data line 2806: "the Shadow Realm has 3 extra shadow puddles to dodge."
- **Skeptic reasoning:** The Awakened combat step (line 2806) states 'the Shadow Realm has 3 extra shadow puddles to dodge.' Multiple targeted wiki fetches confirm no 'shadow puddles' mechanic exists anywhere in the Whisperer strategy page. The real Shadow Realm hazards in Awakened mode are additional tentacles (7 in X / 6 in + vs 4) and additional souls in the Soul Siphon (16 vs 12). 'Shadow puddles' is fabricated terminology.

## Vardorvis (Awakened)

### [blocker] C3: Scythe of Vitur is listed as BiS in the data but the wiki explicitly states Soulreaper axe is the best weapon and that Vardorvis's 2x2 size inhibits the Scythe. The BiS label is inverted -- Soulreaper is BiS, Scythe is the inferior/inhibited option.

- **Data says:** Slash mainhand: Scythe of Vitur (BiS) or Soulreaper axe; Blade of Saeldor (c) is the trade-off pick.
- **Wiki says (raw):** The soulreaper axe is the best weapon to use against Vardorvis, as his 2x2 size inhibits the scythe of vitur.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Swap the order and labels: Soulreaper axe is BiS; Scythe of Vitur should be removed or listed as a downgrade/not-recommended, since the wiki explicitly explains why it underperforms here.
- **Skeptic receipt:** The soulreaper axe is the best weapon to use against Vardorvis, as his 2x2 size inhibits the scythe of vitur.
- **Skeptic reasoning:** The data labels Scythe of Vitur as BiS for Vardorvis with Soulreaper as a secondary pick. The wiki explicitly inverts this: the Scythe is actively inhibited by Vardorvis's 2x2 size and the Soulreaper axe is the best weapon. This survives every refutation vector -- it is not account-type dependent (both are mains/high-end gear), not a variant id issue, not staleness (current strategy page). The BiS label on Scythe is a genuine boss-specific error in the guidance. Treated as high rather than blocker since it is guidance text, not a clog id or unlock gate.

### [blocker] C2: The claim that awakened mode 'adds a heal-spawn enrage at low HP' is not supported by the wiki. The wiki's complete awakened mode change list contains no heal-spawning mechanic. No Strangled enemies spawn during the Vardorvis boss fight in any mode -- wiki only mentions them as travel obstacles en route to the boss.

- **Data says:** The Awakened axe pattern is roughly 1 axe denser per phase and adds a heal-spawn enrage at low HP.
- **Wiki says (raw):** In Awakened mode, Vardorvis has the following changes: The boss' health increases from 700 to 1,400. The boss is immune to stat drains. An additional Swinging Axe appears for each set of axe thrown. Swinging Axes damage increased from 35 to 96. Bleed damage is increased from 3 to 5. The spores from his Strangle attack have less time to be destroyed before the tendrils tighten.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Remove the 'adds a heal-spawn enrage at low HP' language. The awakened mechanic changes are: +1 axe per set, axes at enrage-cadence throughout, doubled HP, stat drain immunity, and increased damage values.
- **Skeptic receipt:** In Awakened mode... The boss' health increases from 700 to 1,400. The boss is immune to stat drains. An additional Swinging Axe appears for each set of axe thrown. Swinging Axes damage increased from 35 to 96. Bleed damage is increased from 3 to 5. ... Both his normal and special attacks will also heal him for 50% of the damage inflicted, rounded down.
- **Skeptic reasoning:** The wiki's complete Awakened change list contains no heal-spawn-at-low-HP enrage. The only healing mechanic is Vardorvis's own 50% lifesteal on his attacks -- not spawns. The enrage phase only increases axe cadence. No 'heal-spawn' mechanic exists in any mode. The fabricated mechanic survives no refutation vector (not variant, not account-type, not stale). The factual awakened changes are doubled HP (700->1400), stat-drain immunity, +1 axe per set, increased axe/bleed/spore/spike damage, and the head's magic prayer-disable. The 'heal-spawn enrage at low HP' phrase is unsupported.

### [blocker] C17: The wiki describes Strangled enemies only as travel obstacles in the Stranglewood on the way to the boss. There is no wiki mention of Strangled enemies spawning during the Vardorvis boss fight itself, nor of them healing the boss for any amount per tick. This mechanic appears to be fabricated.

- **Data says:** Kill the Strangled spawns the moment they appear or they heal him for 15-25 per tick.
- **Wiki says (raw):** players will be running past several Strangled and a Strangled Boar while reaching the boss. The strangled will deal 1-3 damage, while the boar can deal 2-4 damage if they attack the player.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Remove the Strangled spawn kill-on-sight instruction and the 15-25 per tick healing claim. If a similar mechanic exists it would need a verifiable wiki source before being included.
- **Skeptic receipt:** players will be running past several Strangled and a Strangled Boar while reaching the boss. The strangled will deal 1-3 damage, while the boar can deal 2-4 damage if they attack the player.
- **Skeptic reasoning:** No wiki source describes Strangled enemies spawning during the Vardorvis fight or healing him 15-25 per tick. Strangled and the Strangled Boar are travel obstacles in the Stranglewood en route to the boss, dealing 1-4 damage to the player. Vardorvis's only healing is 50% lifesteal from his own attacks. The kill-on-sight-or-he-heals mechanic is fabricated. Survives all vectors.

### [high] C18: Two errors: (1) the enrage threshold is 33% HP per the wiki, not 25%; (2) there is no attack called 'head-slam' -- the wiki describes a 'Head Gaze' projectile attack that can disable overhead prayers, not a slam. Following the sub-25% trigger would mean the player misses the actual enrage transition at 33%.

- **Data says:** On the sub-25% enrage, prayer-flick to skip the head-slam and Voidwaker spec the spawn waves to keep pressure.
- **Wiki says (raw):** Upon reaching 33% of his health (~231 health), Vardorvis will enter an enrage phase after his next non-enraged attack. During the enrage phase, Vardorvis' axes will appear more frequently, with a new set of axes appearing after the previous set have been thrown off the arena.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Change 'sub-25% enrage' to 'sub-33% enrage'. Replace 'head-slam' with 'Head Gaze' (the wiki's name for the prayer-disabling projectile from Vardorvis's severed head). Also remove the 'spawn waves' reference since C17's Strangled spawn mechanic is unsupported.
- **Skeptic receipt:** Upon reaching 33% of his health (~231 health), Vardorvis will enter an enrage phase after his next non-enraged attack. ... Head Gaze - 'Vardorvis' head gazes upon you'; fires a 'green, cone-shaped projectile'. ... Failing to block the attack will drain 10 prayer points and prevent use of overhead protection prayers for 3 ticks.
- **Skeptic reasoning:** Two distinct errors both hold. (1) The enrage threshold is 33% (~231 HP), not 25% -- directly quoted from the wiki. (2) There is no 'head-slam' attack; the wiki's attack is 'Head Gaze', a green cone-shaped projectile, and it CANNOT be prayer-flicked to skip -- it must be blocked with Protect from Missiles, and failing it disables overhead prayers (the opposite of the data's 'flick to skip' framing). The 'spawn waves' clause is also unsupported (see C17). Not stale, not account-type dependent. Severity high (guidance correctness, not a clog id).

### [medium] C6: The wiki recommends Voidwaker for speccing Vardorvis himself at kill-start when his defence is highest, not for use against 'Strangled spawn waves' (which per C17 do not exist as a fight mechanic). The recommended use-case in the data is unsupported.

- **Data says:** Voidwaker for spec on the Strangled spawn waves.
- **Wiki says (raw):** Since Voidwaker ignores defence, it's best used at the start of kills when Vardorvis's defence is maximised.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Update Voidwaker guidance to match the wiki: spec Vardorvis at the start of the kill when his defence is maximised. Remove the 'Strangled spawn waves' framing.
- **Skeptic receipt:** Since Voidwaker ignores defence, it's best used at the start of kills when Vardorvis's defence is maximised.
- **Skeptic reasoning:** The data recommends Voidwaker spec on 'Strangled spawn waves', which do not exist as a Vardorvis fight mechanic (see C17 -- Strangled are travel mobs, boss lifesteals from his own attacks, no in-fight spawns). The wiki's actual Voidwaker use-case is speccing Vardorvis himself at kill-start when his defence is maximised (defence-ignoring property). The recommended target in the data is fabricated. Medium severity -- wrong tactical framing, not a data-integrity blocker.

### [medium] C19: Same issue as C6: the wiki does not support using Voidwaker spec on spawn waves. Voidwaker is recommended against Vardorvis at high defence (kill-start). The 'spawn waves' framing is tied to the unsupported Strangled-spawn mechanic in C17.

- **Data says:** Voidwaker spec the spawn waves to keep pressure.
- **Wiki says (raw):** Since Voidwaker ignores defence, it's best used at the start of kills when Vardorvis's defence is maximised.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vardorvis/Strategies
- **Suggested fix:** Remove the 'spawn waves' context. Combine with C6 fix: Voidwaker is specced on Vardorvis at kill-start for its defence-ignoring property.
- **Skeptic receipt:** Since Voidwaker ignores defence, it's best used at the start of kills when Vardorvis's defence is maximised.
- **Skeptic reasoning:** Same underlying error as C6, surfaced in a different guidance step: 'Voidwaker spec the spawn waves to keep pressure.' The spawn-wave target does not exist (C17); the wiki recommends Voidwaker on Vardorvis at kill-start for his maximised defence. The 'spawn waves' framing is unsupported. Medium severity. Note this is a duplicate of the C6 root cause -- both should be fixed together to the wiki-supported kill-start framing.

## Grotesque Guardians

### [blocker] C6: Phase 1 is Dawn alone -- Dusk is fully immune and cannot be attacked. The claim that they 'fight together initially' inverts the opening structure. Players following this guidance would waste DPS trying to attack an immune Dusk.

- **Data says:** Dusk (melee) and Dawn (ranged) fight together initially
- **Wiki says (raw):** Dusk is immune to all damage during this phase, so the player must fight Dawn first. As she is airborne, the player must attack her with ranged, magic, or with a halberd; she is immune to non-halberd melee attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- **Suggested fix:** Correct the description to reflect that Phase 1 is Dawn solo (Dusk is immune). Only in Phase 3 are both guardians relevant simultaneously (Dawn re-activates while Dusk becomes invulnerable again).
- **Skeptic receipt:** Dusk is immune to all damage during this phase, so the player must fight Dawn first. As she is airborne, the player must attack her with ranged, magic, or with a halberd; she is immune to non-halberd melee attacks.
- **Skeptic reasoning:** The data step-4 text literally reads 'Dusk (melee) and Dawn (ranged) fight together until one dies, then the survivor enrages'. This models the encounter as both guardians simultaneously attackable from the start, which is wrong. The wiki is explicit that Phase 1 is Dawn solo with Dusk fully immune; the encounter alternates which guardian is attackable (P1 Dawn only, P2 Dusk only, P3 lightning/both only briefly during the charge, P4 Dusk only) and they are never both freely attackable as the data implies. A player following the data would waste DPS attacking an immune Dusk in Phase 1. Not a multi-source/variant/account-type nuance -- it is a flat mechanical error in the guidance model. The finding's paraphrase ('initially') is slightly looser than the data's 'until one dies', but the core contradiction (data: fight together; wiki: Dusk immune, fight Dawn first) holds.

### [medium] C9: Dusk gains a stat boost by absorbing Dawn's essence but does NOT heal to full HP. The claim of a full HP reset is not supported by the wiki; it is misleading about the phase transition.

- **Data says:** After one Guardian dies, the survivor enrages and heals to full
- **Wiki says (raw):** Dusk will absorb the leftover remains of her essence. Dusk's Attack level increases to 300, with Strength, Magic, and Ranged increased to 250, Defence to 150, and add one flat armour.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- **Suggested fix:** Remove 'heals to full'. Replace with: Dusk absorbs Dawn's essence, significantly boosting his combat stats (Attack to 300, Strength/Magic/Ranged to 250, Defence to 150) for the final phase.
- **Skeptic receipt:** Dusk's Attack level increases to 300, with Strength, Magic, and Ranged increased to 250, Defence to 150, and add one flat armour.
- **Skeptic reasoning:** The data step-4 text explicitly says 'the survivor heals to full'. The wiki describes the death transition as Dusk absorbing Dawn's essence and gaining a large stat boost (Attack 300; Strength/Magic/Ranged 250; Defence 150; one flat armour) -- it does NOT describe an HP reset to full. Dusk carries his remaining HP into the final phase with boosted combat stats; there is no full-HP heal. 'Heals to full' is unsupported and misleads the player about the difficulty/length of the closing phase. This is not staleness or account-type nuance; it is a fabricated mechanic.

### [low] C14: The wiki does not specify a '2 tile' dodge distance, nor does it describe a 'raises her arms' animation telegraph. The advice to move is correct but the specific tile count and trigger description are not found on the wiki.

- **Data says:** When Dawn raises her arms, the player must move 2 tiles to avoid her lightning AoE
- **Wiki says (raw):** Stand in a spot clear of any vortices; although some vortices appear later than others, they will all expire once the pair become attackable.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- **Suggested fix:** Remove the '2 tiles' distance and 'raises her arms' trigger. Replace with guidance to move away from any vortex that spawns on or adjacent to the player's tile.
- **Skeptic receipt:** There is a one-second warning frame indicated with a vortex of the respective colour before the player starts taking damage. ... Stand in a spot clear of any vortices; although some vortices appear later than others, they will all expire once the pair become attackable.
- **Skeptic reasoning:** The data says 'Dodge Dawn's lightning AoE (move 2 tiles when she raises her arms)'. The wiki gives no '2 tile' move distance and, critically, no 'raises her arms' animation telegraph. The actual mechanic (Phase 3) is a joint lightning charge where vortices spawn across the roof -- one always on the player's current tile -- with a one-second colour-vortex warning; the player simply moves off any vortex. The fabricated 'raises her arms' telegraph could cause a player to wait for an animation that does not exist, and the '2 tiles' figure is invented. The general 'move to dodge' advice is correct, so this is low severity, but the specific telegraph/distance details are fabricated and not on the wiki.

### [medium] C16: The wiki describes a 'trample' attack (standing under Dusk causes up to 40 damage) but does NOT describe pre-marked tiles or indicators. The claim of tile-marking shadows/indicators before the stomp is fabricated detail not supported by the wiki.

- **Data says:** The surviving Guardian after the first dies gains a stomp special attack that marks tiles
- **Wiki says (raw):** standing under Dusk during this phase can result in him trampling the player for up to 40 damage and should be avoided at all costs.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies
- **Suggested fix:** Replace 'marks tiles' with the accurate mechanic: standing under Dusk causes a trample attack dealing up to 40 damage. There are no pre-marked tile indicators described.
- **Skeptic receipt:** standing under Dusk during this phase can result in him trampling the player for up to 40 damage and should be avoided at all costs.
- **Skeptic reasoning:** The data says the survivor 'gains a stomp special - move off the marked tile'. The wiki describes a trample (standing under Dusk can deal up to 40 damage) and gives NO pre-marked tile indicator -- the mechanic is purely positional (do not stand under Dusk). The 'marked tile' telegraph is fabricated, and the data also wrongly frames it as a new ability gained only 'after the first Guardian dies', whereas the trample danger exists in Phase 2 as well as Phase 4. Medium severity: a player watching for a tile marker that does not exist will mis-handle the mechanic.

## The Nightmare

### [high] C4: The Drakan's medallion Slepe teleport requires using a Slepey tablet on the medallion (a 1/25 drop from Phosani's Nightmare), not Sins of the Father. Sins of the Father unlocks the Darkmeyer teleport, not the Slepe one. The guidance names the wrong gate.

- **Data says:** Drakan's medallion teleports directly to Slepe (requires Sins of the Father quest)
- **Wiki says (raw):** the Sisterhood Sanctuary under Slepe (after using a Slepey tablet on it, which is a 1/25 drop from Phosani's Nightmare)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Drakan%27s_medallion
- **Suggested fix:** Change the requirement to note that a Slepey tablet (1/25 drop from Phosani's Nightmare) must be used on the medallion to unlock the Slepe teleport, not completion of Sins of the Father.
- **Skeptic receipt:** after using a Slepey tablet on it, which is a 1/25 drop from Phosani's Nightmare. ... Darkmeyer (unlocked after completing Sins of the Father).
- **Skeptic reasoning:** The Drakan's medallion has three teleport destinations: Ver Sinhaza (always), Darkmeyer (unlocked by Sins of the Father), and Slepe/Sisterhood Sanctuary (unlocked by using a Slepey tablet, a 1/25 drop from Phosani's Nightmare). The data attributes the Slepe teleport to Sins of the Father, which actually gates the Darkmeyer teleport. This is a flat requirement error, not an account-type/variant/multi-source nuance. STANDS - high.

### [blocker] C6: The Nightmare is located in the NORTH of the Sisterhood Sanctuary, not the south. Players entering from Slepe encounter Oathbreakers first (south area) and must proceed farther north to reach The Nightmare. The guidance is inverted.

- **Data says:** The Nightmare's arena is located south within the Sisterhood Sanctuary
- **Wiki says (raw):** Farther north, players will find Shura and The Nightmare, where players can fight her.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sisterhood_Sanctuary
- **Suggested fix:** Change 'south within the Sisterhood Sanctuary' to 'north within the Sisterhood Sanctuary'.
- **Skeptic receipt:** Players entering the Sanctuary will first see the Oathbreakers... Farther north, players will find Shura and The Nightmare, where players can fight her.
- **Skeptic reasoning:** The Sisterhood Sanctuary places The Nightmare and Shura in the NORTH; players enter from the south and first meet the Oathbreakers, then proceed north to the boss. The data says the arena is 'south within the Sisterhood Sanctuary', which is inverted. No legitimate-mechanic reading rescues it. STANDS - blocker.

### [blocker] C12: The Nightmare has no sanity mechanic. Parasites that are not dealt with burst from the player dealing massive HP damage and then heal The Nightmare. The claim about 'sanity drops' is fabricated -- the correct consequence is HP damage.

- **Data says:** Parasites at The Nightmare must be killed quickly to avoid sanity drops
- **Wiki says (raw):** After ~18 ticks (10.8s), it will grow and burst out of the player, dealing massive damage, and will then begin healing the Nightmare until killed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Nightmare/Strategies
- **Suggested fix:** Remove 'sanity drops' entirely. The correct consequence is massive Hitpoints damage to the player, plus The Nightmare healing from the parasite until it is killed.
- **Skeptic receipt:** The Nightmare throws a parasite onto random players. After ~18 ticks (10.8s), it will grow and burst out of the player, dealing massive damage, and will then begin healing the Nightmare until killed.
- **Skeptic reasoning:** The Nightmare has no sanity mechanic (sanity belongs to other content, not this boss). Unkilled parasites burst dealing massive HP damage and then heal the Nightmare. The data's 'sanity drops' consequence is fabricated; the real consequence is HP damage plus the parasite healing the boss. STANDS - blocker.

### [blocker] C14: The totems must be CHARGED by players (they deal 800 damage to The Nightmare when fully charged), not 'broken'. The guidance describes the player action as 'breaking' the totems, which is the opposite of the actual mechanic -- players interact with them to charge them up as the offensive mechanic against the boss.

- **Data says:** Healing totems must be broken each phase during The Nightmare fight
- **Wiki says (raw):** breaking down its shield sufficiently before they can charge the four totems in the corners of the arena, unleashing a magical blast that deals massive damage to her.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Nightmare
- **Suggested fix:** Replace 'healing totems must be broken' with 'totems in the corners of the arena must be charged each phase; when all four are fully charged they deal 800 damage to The Nightmare'.
- **Skeptic receipt:** all three of which involve the player(s) breaking down its shield sufficiently before they can charge the four totems in the corners of the arena, unleashing a magical blast that deals massive damage to her. ... When all four totems are fully charged, they will unleash a magical blast of energy onto the Nightmare, dealing 800 damage to her.
- **Skeptic reasoning:** Players CHARGE the four corner totems (after breaking down the boss's shield); fully charged totems unleash a blast dealing 800 damage to the Nightmare. The data describes 'healing totems must be broken', which inverts the player action (charge, not break) and mislabels them as 'healing'. The shield-breaking step is separate from the totem-charging step. STANDS - blocker.

### [medium] C5: The Sisterhood Sanctuary entrance is in 'the small building directly south of the graveyard' in Slepe, not inside the church. The Slepe wiki further specifies 'a house just south-west of the town's church'. Players directed to the church will look in the wrong building.

- **Data says:** There are stairs in Slepe church that lead down into the Sisterhood Sanctuary
- **Wiki says (raw):** the player can go down the stairs in the small building directly south of the graveyard
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Nightmare
- **Suggested fix:** Change 'stairs in Slepe church' to 'stairs in the small building south of the graveyard in Slepe (next to, but not the church itself)'.
- **Skeptic receipt:** From there, the player can go down the stairs in the small building directly south of the graveyard, and then run east to arrive at the bridge to the Sisterhood Sanctuary.
- **Skeptic reasoning:** The Sisterhood Sanctuary entrance from Slepe is via stairs in the small building directly south of the graveyard, not the church. The data says 'stairs in Slepe church', which points players to the wrong building. Minor landmark error, but factually distinct. STANDS - low.

## Phosani's Nightmare

### [blocker] C8: The wiki clearly states sleepwalkers walk toward Phosani's Nightmare, not toward the player. The claim that they 'chase the player' is factually wrong. This matters because it misrepresents the mechanic: the danger of sleepwalkers is that they reach the Nightmare and cause a 15-damage blast -- players must intercept and kill them before they reach the boss, not because the minions are chasing the player themselves.

- **Data says:** Sleepwalker minions chase the player during Phosani's Nightmare and must be killed
- **Wiki says (raw):** At the end of the first three phases, Phosani's Nightmare will summon sleepwalkers that walk toward her
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phosani%27s_Nightmare/Strategies
- **Suggested fix:** Change the description to: sleepwalkers spawn at the edges and walk toward Phosani's Nightmare -- players must kill them before they reach her, as each one that reaches her triggers a 15-damage attack on the player.
- **Skeptic receipt:** Sleepwalkers walk toward her, the number of which is dependent upon which phase the player is on, up to four at the end of phase three. ... The number of sleepwalkers that reach her will determine how strong her power blast will be. If no sleepwalkers are killed, the resulting blast will always kill the player.
- **Skeptic reasoning:** The data (drop_rates.json line 3527) states 'Sleepwalker minions chase you and must be killed.' The wiki authoritatively states sleepwalkers walk toward Phosani's Nightmare (her), NOT the player, and the reason to kill them is to prevent her power blast -- not because they attack the player. This is a genuine, non-variant mechanic error: it misrepresents both the direction of movement and the reason for the kill. Not a multi-source, variant, or account-type nuance. Survives all refutation vectors.

### [blocker] C10: There is no sanity mechanic in OSRS; parasites deal massive damage when they burst out and then heal the Nightmare boss, not drain sanity. The claim invents a non-existent game mechanic. A player following this guidance would not understand the real urgency (burst damage + healing the boss) and might not prioritize killing the parasite.

- **Data says:** Parasites drain sanity during Phosani's Nightmare
- **Wiki says (raw):** After ~18 ticks (10.8s), it will grow and burst out of the player, dealing massive damage, and will then begin healing Phosani's Nightmare until killed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phosani%27s_Nightmare/Strategies
- **Suggested fix:** Replace 'drain sanity' with the correct mechanic: parasites attach to the player and, after ~18 ticks, burst out dealing massive damage and begin healing Phosani's Nightmare. Drink Sanfew serum or Relicym's balm before the burst to greatly reduce the damage.
- **Skeptic receipt:** Phosani's Nightmare impregnates the player with a parasite. After ~18 ticks (10.8s), it will grow and burst out of the player, dealing massive damage, and will then begin healing Phosani's Nightmare until killed. Drinking a dose of Sanfew serum or Relicym's balm before it emerges will weaken the parasite and greatly reduce the damage taken when the parasite bursts out.
- **Skeptic reasoning:** The data (line 3527) states 'parasites still drain sanity.' OSRS has no sanity mechanic (sanity is an RS3 Nightmare concept). The wiki authoritatively describes the real mechanic: the parasite bursts after ~18 ticks dealing massive damage, then heals Phosani's Nightmare until killed, mitigated by Sanfew serum / Relicym's balm. The data invents a non-existent mechanic and omits the real urgency (burst damage + boss healing). Genuine fabrication, not a variant or nuance. (Note: the same false 'sanity' text also appears at line 3326 for The Nightmare -- a separate out-of-scope source.)

### [blocker] C11: Husks have fixed, predetermined attack styles (blue husk = magic, green husk = ranged) -- they do not mirror the player's gear. This is a fabricated mechanic. A player following this guidance would have no idea how to actually handle husks, which requires knowing fixed attack types and the correct kill order (ranged husk first in phases 1-2).

- **Data says:** The husk attack mirrors the player's gear during Phosani's Nightmare
- **Wiki says (raw):** The blue, skinny husk uses magic attacks, while the green, bulky husk uses ranged attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phosani%27s_Nightmare/Strategies
- **Suggested fix:** Replace the mirroring description with: the blue (magic) husk and green (ranged) husk have fixed attack styles. Kill the ranged husk first during phases 1 and 2. During phase 3, if Phosani's Nightmare uses melee, kill the magic husk first.
- **Skeptic receipt:** The blue, skinny husk uses magic attacks, while the green, bulky husk uses ranged attacks. To fully avoid damage from husks, kill the ranged husk first during phases 1 and 2.
- **Skeptic reasoning:** The data (line 3527) states 'the husk attack will mirror your gear so bring max gear you can lose.' The wiki authoritatively states husks have FIXED attack styles tied to colour: blue/skinny = magic, green/bulky = ranged. They do not mirror player gear. The 'bring max gear you can lose' advice is also nonsensical for this mechanic. Fabricated mechanic; survives all vectors (not a variant or account nuance).

### [high] C13: The Slepe teleport on Drakan's medallion is unlocked by using a Slepey tablet (a 1/25 drop from Phosani's Nightmare itself), not by completing Sins of the Father. Sins of the Father unlocks the Darkmeyer teleport destination. The quest requirement is therefore wrong -- it gates the waypoint behind the wrong quest.

- **Data says:** Sins of the Father quest is required to use Drakan's medallion teleport to Slepe
- **Wiki says (raw):** Teleporting directly to Slepe via Drakan's medallion after unlocking this teleport via Slepey tablet, which is a drop from Phosani's Nightmare.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Phosani%27s_Nightmare/Strategies
- **Suggested fix:** Change the waypoint requirement from Sins of the Father to 'Slepey tablet used on Drakan's medallion (1/25 drop from Phosani's Nightmare)' -- or remove the quest gate entirely and note the Slepey tablet unlock instead.
- **Skeptic receipt:** Slepe: 'Unlocked by using a Slepey tablet on the medallion' ... 'Slepey tablet' is 'a 1/25 drop from Phosani's Nightmare.' Darkmeyer: 'Unlocked by completing Sins of the Father'. / [A Taste of Hope] 'Completion of A Taste of Hope is required for the following: A Night at the Theatre; Sins of the Father.'
- **Skeptic reasoning:** The data waypoint 'Drakan's medallion to Slepe' (drop_rates.json lines 3460-3468) gates the teleport behind quest SINS_OF_THE_FATHER. This is the wrong gate on two counts. (1) The Slepe destination is unlocked by using a Slepey tablet on the medallion (a 1/25 drop from Phosani's Nightmare), not by any quest. (2) Sins of the Father unlocks the DARKMEYER destination, not Slepe. (3) The medallion itself comes from A Taste of Hope, which the wiki confirms is a PREREQUISITE FOR Sins of the Father, not the reverse ('Completion of A Taste of Hope is required for the following: A Night at the Theatre; Sins of the Father'). So Sins is neither the medallion's gate nor the Slepe-teleport's gate. Genuine requirement error confirmed via wiki. Severity high (per finding): it gates a travel waypoint on the wrong quest.

## Nex

### [blocker] C13: Smoke phase primary prayer is Protect from Melee, not Protect from Magic. The wiki instructs players to stand in melee distance precisely to avoid her magic attacks, with Protect from Melee as the baseline prayer. Advising Protect from Magic here inverts the correct prayer.

- **Data says:** Protect from Magic prayer should be used during Smoke phase at Nex
- **Wiki says (raw):** Protect from Melee initially, then switch to Protect from Magic [situationally when Nex switches targets]. Players should stand in melee distance to force her to use melee attacks instead of multi-target magic.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex/Strategies
- **Suggested fix:** Change Smoke phase prayer guidance to 'Protect from Melee' as the primary recommendation.
- **Skeptic receipt:** Smoke phase: 'the player being targeted by Nex may use Protect from Melee to reduce the overall damage taken, and then immediately switch to Protect from Magic'; players 'should stand in melee distance so Nex uses her melee attack which only targets one player, as opposed to her magic attack which targets multiple players.' (oldschool.runescape.wiki/w/Nex/Strategies)
- **Skeptic reasoning:** The drop_rates.json guidance step (line 3691) reads 'Smoke (Protect from Magic, dodge smoke clouds)'. The wiki's smoke-phase guidance is the opposite: players stand in melee distance and use Protect from Melee to force Nex into her single-target melee attack rather than her multi-target magic attack, switching to Protect from Magic only situationally. The established Nex meta camps Protect from Melee during smoke. Listing Protect from Magic as the smoke-phase prayer inverts the primary prayer. This is a real data error.

### [blocker] C15: Shadow phase prayer is Protect from Missiles (Protect from Ranged), not Protect from Magic. Nex uses ranged shadow attacks during this phase. Using the wrong prayer provides no damage reduction.

- **Data says:** Protect from Magic prayer should be used during Shadow phase at Nex
- **Wiki says (raw):** Prayer: Protect from Missiles (reduces damage by 50%). Shoot shadow ranged attacks. Damage increases closer to Nex (up to 30 with prayer active). Standing far away is critical.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex/Strategies
- **Suggested fix:** Change Shadow phase prayer guidance to 'Protect from Missiles' (Protect from Ranged).
- **Skeptic receipt:** Shadow phase: 'Having Protect from Missiles active will cut the damage taken in half.' (shadow shots are ranged attacks) (oldschool.runescape.wiki/w/Nex/Strategies)
- **Skeptic reasoning:** The data step reads 'Shadow (Protect from Magic, run from the shadow target marker)'. Nex's shadow attacks are RANGED, and the wiki recommends Protect from Missiles (Protect from Ranged), which halves the damage. Protect from Magic provides no reduction against the ranged shadow shots. The data names the wrong protection prayer for this phase.

### [blocker] C17: Blood phase prayer is Protect from Magic, not Protect from Melee. Nex uses Blood Barrage (a magic attack) during this phase. The claim inverts the correct prayer entirely.

- **Data says:** Protect from Melee prayer should be used during Blood phase at Nex
- **Wiki says (raw):** Prayer: Protect from Magic. Attack Pattern: Uses Blood Barrage which heals her and drains prayer based on damage dealt. Single-target with 3x3 AoE. Very accurate.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex/Strategies
- **Suggested fix:** Change Blood phase prayer guidance to 'Protect from Magic'.
- **Skeptic receipt:** Blood phase: 'Protect from Magic is recommended since Nex uses Blood Barrage, a magic-based attack that heals her based on damage dealt.' (oldschool.runescape.wiki/w/Nex/Strategies)
- **Skeptic reasoning:** The data step reads 'Blood (Protect from Melee, stop attacking during the siphon or she heals)'. The blood phase attack is Blood Barrage, a MAGIC attack that heals Nex; the wiki recommends Protect from Magic. The 'stop attacking during the siphon' note is correct, but the prayer is inverted from Magic to Melee. Real data error.

### [high] C23: Nex is a five-phase fight, not four. The four named phases (Smoke, Shadow, Blood, Ice) are followed by a fifth Zaros/enrage phase. Describing it as four-phase omits the final phase where Nex gains Soul Split, Deflect Melee, and Wrath, which requires different strategy (Protect from Melee camping).

- **Data says:** Nex is a four-phase fight consisting of Smoke, Shadow, Blood, and Ice phases
- **Wiki says (raw):** The fight consists of five distinct phases based on the Ancient Magicks, with Nex using smoke, shadow, blood, and ice magic, before entering an empowered final state.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex
- **Suggested fix:** Update to describe Nex as a five-phase fight: Smoke, Shadow, Blood, Ice, and Zaros (enrage) phases.
- **Skeptic receipt:** 'The fight contains five total phases. After the four elemental phases (Smoke, Shadow, Blood, Ice), Nex enters the Zaros phase when her health reaches 20% (680 hitpoints)... healing her for 500 hitpoints, and will only use her melee and non-attuned magic attacks.' (oldschool.runescape.wiki/w/Nex/Strategies and /w/Nex: 'five distinct phases ... before entering an empowered final state')
- **Skeptic reasoning:** The data step (line 3691) describes a 'Four-phase fight' and lists only Smoke, Shadow, Blood, Ice. The wiki states the fight has five distinct phases: the four elemental phases plus a final Zaros/empowered phase that begins at 20% HP, where Nex heals 500 and uses only melee and non-attuned magic. Describing it as four-phase omits the distinct final phase. Genuinely incomplete. (Note: overlaps with C22, which corrects the threshold for that same final phase.)

### [medium] C22: The Zaros/enrage phase begins at 20% health (when Glacies dies at 680 HP), not at 25% as claimed. The 25% threshold is incorrect.

- **Data says:** Nex enrages below 25% health
- **Wiki says (raw):** Phase ends when Glacies is killed after Nex reaches 20% health (680 HP).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex/Strategies
- **Suggested fix:** Correct enrage threshold to 20% health.
- **Skeptic receipt:** 'Nex enters the Zaros phase when her health reaches 20% (680 hitpoints)' / 'Phase ends when Glacies is killed after Nex reaches 20% health (680 HP).' (oldschool.runescape.wiki/w/Nex/Strategies)
- **Skeptic reasoning:** The data step reads 'she enrages below 25%'. The wiki places the Zaros/enrage phase transition at 20% HP (680 HP), when Glacies dies. 25% is the wrong threshold. Confirmed by two independent fetches of the Strategies page.

### [high] C27: Nihil shards are used to make ancient brews (herblore) and to craft Zaryte crossbows -- not for extending Ancient ceremonial robes. The claim describes a use that does not exist on the wiki.

- **Data says:** Nihil shards are used for extending Ancient ceremonial robes
- **Wiki says (raw):** When used with a pestle and mortar, the nihil shard becomes nihil dust, which can be mixed with an unfinished dwarf weed potion at level 85 Herblore to make an ancient brew. 250 Nihil shards are required to create a Zaryte crossbow when combined with an Armadyl crossbow and a Nihil horn.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nihil_shard
- **Suggested fix:** Correct the description of Nihil shards to their actual uses: ingredients for ancient brews and crafting Zaryte crossbows.
- **Skeptic receipt:** 'When used with a pestle and mortar, the nihil shard becomes nihil dust, which can be mixed with an unfinished dwarf weed potion at level 85 Herblore to make an ancient brew. 250 Nihil shards are required to create a Zaryte crossbow...' AND Ancient ceremonial robes page: 'there is no indication that the robes can be extended, repaired, charged, or modified using Nihil shards.' (oldschool.runescape.wiki/w/Nihil_shard, /w/Ancient_ceremonial_robes)
- **Skeptic reasoning:** The data loot step (line 3710) reads 'Pick up Nihil shards for extending Ancient ceremonial robes too'. The Nihil shard wiki page lists only two uses: grinding into nihil dust for ancient brews (85 Herblore), and 250 shards to craft a Zaryte crossbow. The Ancient ceremonial robes page confirms no relationship with Nihil shards (the robes are fixed-bonus prayer cosmetics that cannot be extended/charged). The 'extending Ancient ceremonial robes' justification is fabricated. Real data error.

### [high] C6: The Salve amulet (ei) occupies the amulet/neck slot, not the helm slot. Regardless of whether it is BiS at Nex, describing it as a helm slot item is factually wrong and would mislead players about equipment setup.

- **Data says:** Salve amulet (ei) is BiS for the helm slot at Nex
- **Wiki says (raw):** Salve amulet (ei) is an amulet worn in the amulet (neck) slot, not the helm slot.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nex/Strategies
- **Suggested fix:** Correct 'helm slot' to 'amulet slot'. Also note the wiki Strategies page does not list Salve amulet (ei) among recommended neck items -- it recommends Amulet of blood fury and Necklace of anguish instead.
- **Skeptic receipt:** Salve amulet (ei) page: occupies the 'neck slot' (Neck slot table); 'would not be relevant at Nex because Nex is not an undead enemy.' (oldschool.runescape.wiki/w/Salve_amulet(ei))
- **Skeptic reasoning:** The data bank step (line 3642) reads 'Salve amulet (ei) is BIS for the helm slot.' The Salve amulet (ei) occupies the NECK (amulet) slot, not the helm slot. Additionally it is an undead-only item and Nex is not undead, so it confers no bonus at Nex and is not recommended on the Strategies page. The slot designation is factually wrong and would mislead equipment setup.

## Sarachnis

### [medium] C8: The claim that spiderlings MUST be killed immediately or they will out-damage the player is contradicted by the wiki. Melee spawns may be tanked and need not be killed; all spawns die automatically when Sarachnis dies. The guidance overstates the urgency and may cause players to waste time on spawns instead of continuing to damage Sarachnis.

- **Data says:** Sarachnis spiderlings must be killed immediately or they will out-damage the player
- **Wiki says (raw):** the melee spawns may be tanked with sufficiently high defensive bonuses, but are not necessary to kill. When Sarachnis is killed, any remaining spawns will automatically die off with her.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sarachnis/Strategies
- **Suggested fix:** Change to reflect that the magic-using spawn (level 68, max hit 11) should be prioritized, the melee spawn (level 107, max hit 13) can be tanked if defensive bonuses are sufficient, and all remaining spawns die when Sarachnis is killed.
- **Skeptic receipt:** The melee spawns may be tanked with sufficiently high defensive bonuses, but are not necessary to kill as the player should be praying Protect from Melee outside her web attack. ... When Sarachnis is killed, any remaining spawns will automatically die off with her.
- **Skeptic reasoning:** The data asserts spiderlings MUST be killed immediately or they out-damage the player. The wiki directly contradicts the mandatory framing: the melee spawns 'may be tanked with sufficiently high defensive bonuses, but are not necessary to kill,' and any remaining spawns die automatically with Sarachnis. I checked refutation vectors: this is not a variant/multi-source/account-type nuance -- it is a flat strategy overstatement. The wiki does prioritize magic defence due to two magic-using spawns, so the suggested fix's emphasis on the magic spawn is well founded. The 'must be killed immediately or they out-damage the player' framing is genuinely wrong guidance that would cause players to break from damaging the boss unnecessarily.

### [medium] C9: The claim inverts the wall-tile mechanic. The wiki says standing adjacent to walls places you OUT of Sarachnis's melee range (letting you pray missiles to nullify all her damage), but explicitly states this does NOT prevent spawn damage. The claim says 'one tile off the wall prevents spiderlings from stacking damage', which reverses the actual mechanic.

- **Data says:** Staying at least one tile off the wall during Sarachnis fight prevents spawned spiderlings from stacking damage
- **Wiki says (raw):** All tiles directly adjacent to walls are out of Sarachnis's melee range. Standing on one of these tiles and praying Protect from Missiles will nullify all of her damage, but not the damage of her spawn.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sarachnis/Strategies
- **Suggested fix:** Correct to: standing adjacent to the wall puts you out of Sarachnis's melee range so you can pray Protect from Missiles to nullify all her damage. However, spawn damage is not prevented by this positioning.
- **Skeptic receipt:** All tiles directly adjacent to walls are out of Sarachnis's melee range. Standing on one of these tiles and praying Protect from Missiles will nullify all of her damage, but not the damage of her spawn.
- **Skeptic reasoning:** The data claims standing 'at least one tile off the wall ... prevents spawned spiderlings from stacking damage.' The wiki inverts this on both axes confirmed verbatim against the live Strategies page: (1) the safe tiles are those 'directly adjacent to walls' (out of Sarachnis's melee range), not 'one tile off'; and (2) praying Protect from Missiles on those tiles 'will nullify all of her damage, but not the damage of her spawn' -- i.e. wall positioning explicitly does NOT prevent spawn damage, which is the exact effect the data attributes to it. This is not an account-type or variant nuance; it is a mechanically reversed claim. The finding correctly reads the quote and establishes a real contradiction.

## Bryophyta

### [blocker] C3: Bryophyta is a plant/moss boss, not undead. The wiki provides no undead classification. Salve amulet (ei) has no effect on non-undead enemies. Additionally, the wiki recommends Protect from Magic (not a melee-focused prayer), making a melee-salve setup doubly inappropriate as BiS guidance.

- **Data says:** Salve amulet (ei) is BiS (best-in-slot) for Bryophyta due to the +20% melee bonus against undead
- **Wiki says (raw):** This is what organic growth looks like! [...] The page contains no 'undead' classification for Bryophyta. [...] It is recommended to activate Protect from Magic before entering, or immediately upon entry, before opening the gate.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Remove the salve amulet (ei) BiS claim. Bryophyta has a 50% fire magic weakness per the wiki; recommended gear should reflect that and include Protect from Magic.
- **Skeptic receipt:** Wiki Bryophyta: 'Bryophyta is not classified as undead. The wiki lists her combat attributes but contains no undead designation.' / 'Salve amulet is not mentioned anywhere in the strategy or equipment recommendations.' / 'It is recommended to activate Protect from Magic before entering...' / 'Fire weakness: 50%; Stab/Slash/Crush defense: +0 each'
- **Skeptic reasoning:** Data guidance step 1 says verbatim 'Salve amulet (ei) is BiS - Bryophyta is undead'. The wiki has no undead classification for Bryophyta, never mentions salve amulet, lists her weakness as 50% fire magic, and recommends Protect from Magic (a magic-defence prayer) - so a melee-salve setup is inappropriate guidance. Survives refutation vectors: this is a guidance-prose error, not a multi-source/variant-id/account-type artifact. Salve amulet (ei) gives no bonus vs a non-undead target.

### [blocker] C4: The wiki classifies Bryophyta as a plant/moss creature (examine: 'This is what organic growth looks like!'). There is no undead classification anywhere on the page. This incorrect classification is the root cause of the salve amulet guidance errors in C3 and C13.

- **Data says:** Bryophyta is an undead boss
- **Wiki says (raw):** This is what organic growth looks like! [...] The page contains no 'undead' classification for Bryophyta.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Remove the undead classification from the source entry.
- **Skeptic receipt:** Wiki Bryophyta: 'Bryophyta is not classified as undead. The wiki lists her combat attributes but contains no undead designation.' Examine/strategy contains no undead attribute line.
- **Skeptic reasoning:** Data guidance asserts in prose 'Bryophyta is undead' (steps 1 and 4). The wiki carries no undead classification. This is the root false premise behind the salve guidance (C3/C13). Note: the suggested fix wording 'remove the undead classification from the source entry' slightly misframes it - there is no structured undead field; it is a textual assertion in the guidanceSteps - but the underlying factual claim (data says undead, wiki refutes) holds.

### [blocker] C7: The wiki states explicitly that the key is NOT consumed when opening the gate, and that subsequent visits do not require a key at all. The claim that a fresh key is needed per kill is doubly wrong: the key survives gate use, and once the lair is unlocked the gate stays open.

- **Data says:** The Mossy key is consumed on use when opening Bryophyta's gate, requiring a fresh key for each kill
- **Wiki says (raw):** A mossy key is needed to unlock the gate leading to Bryophyta's lair for the first time, but subsequent access to the lair does not require a key. The key will not be consumed when unlocking Bryophyta's lair.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Correct guidance: the mossy key unlocks Bryophyta's lair for the first time only; it is not consumed when used at the gate. Subsequent kills do not require a new key.
- **Skeptic receipt:** Wiki Bryophyta: 'A mossy key is needed to unlock the gate leading to Bryophyta's lair for the first time, but subsequent access to the lair does not require a key. The key will not be consumed when unlocking Bryophyta's lair.' Wiki Mossy_key infobox: 'A key is consumed when looting the chest, and one key is also required to access Bryophyta's lair for the first time, after which the lair can be freely entered. The key is not consumed when unlocking access to Bryophyta's lair for the first time.'
- **Skeptic reasoning:** Data guidance step 3 says 'Use the Mossy key on the gate... The key is consumed on use - each kill needs a fresh key'. The wiki is explicit that the key is NOT consumed unlocking the gate and that after the first unlock the lair is freely accessible. (The key IS consumed when looting the chest, a separate mechanic the data conflates with gate access.) The 'fresh key per kill at the gate' claim is wrong. Not a variant/multi-source artifact - a flat mechanic error.

### [high] C8: The wiki gives 1/150 as the standard moss giant drop rate for the mossy key (1/75 on Slayer task). The 1/16 figure claimed does not correspond to any standard moss giant drop rate listed on the wiki.

- **Data says:** Moss giant key drops occur at approximately 1/16 rate
- **Wiki says (raw):** The drop rate of mossy keys is 1/150 from moss giants [...] When on a moss giant Slayer task, the drop rate is increased to 1/75 from regular moss giants.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mossy_key
- **Suggested fix:** Update the mossy key drop rate to 1/150 (standard) or 1/75 (on Slayer task) as stated on the wiki.
- **Skeptic receipt:** Wiki Mossy_key: 'The drop rate of mossy keys is 1/150 from moss giants, 1/120 from Iorwerth Dungeon moss giants, and 1/100 from Wilderness moss giants. However, while on a moss giant Slayer task, the drop rate is increased to 1/75 from regular moss giants...' Wiki Bryophyta drop table (boss): 'Mossy key | qty: 1 | rarity: 1/16' (this is the boss drop, not the moss giant rate).
- **Skeptic reasoning:** Data guidance steps 3/5 cite '~1/16' as the moss giant key drop rate. The 1/16 figure is actually the rate at which the BOSS Bryophyta drops a Mossy key (per her own drop table), not the moss-giant farm rate. The Mossy key wiki page gives moss giants 1/150 (1/75 on a moss giant Slayer task). 1/16 misattributes the boss-table rate to moss giants and overstates the farm rate by ~9x. Verified the 1/16 origin in the wiki_lookup boss drop table, refuting any 'maybe 1/16 is a valid task/location rate' defence.

### [blocker] C9: The wiki explicitly recommends Protect from Magic, not Protect from Melee. Using the wrong protection prayer directly exposes the player to Bryophyta's magic attacks.

- **Data says:** Protect from Melee is the appropriate protection prayer to use during Bryophyta fight
- **Wiki says (raw):** It is recommended to activate Protect from Magic before entering, or immediately upon entry, before opening the gate.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Change guidance to recommend Protect from Magic.
- **Skeptic receipt:** Wiki Bryophyta: 'It is recommended to activate Protect from Magic before entering, or immediately upon entry, before opening the gate.'
- **Skeptic reasoning:** Data combat step says 'Use Protect from Melee'. The wiki explicitly recommends Protect from Magic. Refutation check: not account-type dependent; the prayer recommendation is universal in the wiki strategy. Using Protect from Melee leaves the player exposed to her magic attacks.

### [high] C10: The wiki combat infobox shows all melee defence bonuses (stab, slash, crush) are equal at +0. There is no elevated stab defence. Bryophyta's actual weakness is fire magic (50%). The claim fabricates a stab-defence advantage for crush/slash that does not exist in the wiki data.

- **Data says:** Bryophyta has high stab defence, making crush or slash weapons preferable
- **Wiki says (raw):** The infobox shows: Fire elemental weakness (50% weakness); all other damage types listed as +0.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Remove the stab-defence claim. Note that Bryophyta has a 50% fire magic weakness; melee styles are all equally effective against her.
- **Skeptic receipt:** Wiki Bryophyta infobox: 'Fire weakness: 50%; Stab/Slash/Crush defense: +0 each; Magic defense: +0; Ranged defense: +0'
- **Skeptic reasoning:** Data combat step claims 'Bryophyta has high stab defence' favouring crush/slash. The wiki infobox lists stab, slash, and crush defence all at +0 - no elevated stab defence exists. Her actual exploitable weakness is 50% fire magic. The stab-defence advantage is fabricated. Not a variant/version artifact.

### [high] C12: The wiki describes Bryophyta's invulnerability while Growthlings are alive, not a per-tick healing mechanic. There is no mention of Growthlings healing Bryophyta for 3 HP per tick anywhere on the page. The actual mechanic is damage immunity, not regeneration.

- **Data says:** Each living Growthling heals Bryophyta for 3 HP per tick
- **Wiki says (raw):** While they are weak, they must be killed because Bryophyta is immune to all damage (except poison and venom) while they are alive.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Replace the 3 HP/tick healing claim with the correct mechanic: Bryophyta is immune to all damage (except poison/venom) while any Growthling is alive.
- **Skeptic receipt:** Wiki Bryophyta: 'Bryophyta is immune to all damage (except poison and venom) while they are alive.'
- **Skeptic reasoning:** Data combat step claims 'each living Growthling heals Bryophyta for 3 HP per tick'. The wiki describes the mechanic as full damage immunity (except poison/venom) while any Growthling is alive - not a per-tick heal. No HP/tick healing figure appears anywhere on the page. The mechanic is misdescribed (a fabricated regen number in place of the real immunity mechanic).

### [blocker] C13: Bryophyta is not undead per the wiki; the salve amulet (ei) would provide no bonus. Marking it 'mandatory' based on a false undead classification is incorrect and would mislead players into equipping an ineffective amulet slot item.

- **Data says:** Salve amulet (ei) is mandatory for Bryophyta due to providing +20% melee bonus against the undead boss
- **Wiki says (raw):** This is what organic growth looks like! [...] The page contains no 'undead' classification for Bryophyta.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bryophyta
- **Suggested fix:** Remove the mandatory salve amulet (ei) requirement. Bryophyta is a plant boss, not undead.
- **Skeptic receipt:** Wiki Bryophyta: 'Bryophyta is not classified as undead... contains no undead designation.' / 'Salve amulet is not mentioned anywhere in the strategy or equipment recommendations.'
- **Skeptic reasoning:** Data combat step says 'Salve amulet (ei) is mandatory for the +20% melee bonus against this undead boss'. Same false undead premise as C3/C4: Bryophyta is not undead, so salve (ei) gives no bonus and is certainly not mandatory. Marking an ineffective amulet 'mandatory' is misleading guidance. Refutation vectors (variant/multi-source/account-type) do not apply.

### [medium] C2: The mossy key wiki page lists moss giants in Varrock Sewers (correct), Iorwerth Dungeon, and the Wilderness as key sources -- Ardougne is not listed. Brimhaven Dungeon moss giants do exist per the Moss giant page but are not listed as a mossy key source with a named rate on the key's wiki page. Ardougne is a fabricated location for this drop.

- **Data says:** Mossy key can be farmed from Moss giants in Varrock Sewers, Brimhaven dungeon, or Ardougne
- **Wiki says (raw):** The drop rate of mossy keys is 1/150 from moss giants, 1/120 from Iorwerth Dungeon moss giants, and 1/100 from Wilderness moss giants. [...] No Ardougne or Ardougne Sewers location is mentioned in the article.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mossy_key
- **Suggested fix:** Remove Ardougne as a mossy key farming location. Confirmed sources are Varrock Sewers moss giants, Iorwerth Dungeon moss giants, and Wilderness moss giants.
- **Skeptic receipt:** Wiki Moss_giant Locations: Brimhaven Dungeon - yes (20 spawns, lvl 42); Varrock Sewers - yes (10 spawns, lvl 42); 'No locations in or near Ardougne (West Ardougne, Ardougne Sewers) are mentioned as moss giant spawn points.' Wiki Mossy_key: rates listed only for moss giants (1/150), Iorwerth Dungeon (1/120), Wilderness (1/100) - no Ardougne.
- **Skeptic reasoning:** Data step 1 lists mossy-key farming locations as 'Varrock Sewers / Brimhaven dungeon / Ardougne'. Varrock Sewers and Brimhaven Dungeon are genuine moss giant locations (verified on the Moss giant page), but Ardougne is not a moss giant location at all. Ardougne is the fabricated element. Brimhaven moss giants are 'regular' moss giants (1/150 rate), so listing Brimhaven is fine; only Ardougne should be removed. Medium severity is appropriate (misleading farm location, not a hard mechanic break).

### [low] C5: The manhole is 'just east of Varrock Palace' per the wiki, not 'behind' it. The Edgeville Dungeon connection is a pipe shortcut (Agility-gated, members-only), not a ladder.

- **Data says:** Varrock Sewers can be accessed via the manhole behind Varrock Palace or via the Edgeville Dungeon ladder
- **Wiki says (raw):** The Varrock Sewers can be accessed via a manhole just east of Varrock Palace. [...] Members can also access the sewers through Edgeville Dungeon by using a pipe shortcut near Vannaka - this requires 51 Agility.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Varrock_Sewers
- **Suggested fix:** Update to: manhole 'just east of Varrock Palace'; Edgeville Dungeon access is via a pipe shortcut requiring level 51 Agility.
- **Skeptic receipt:** Wiki Varrock_Sewers: 'The Varrock Sewers can be accessed via a manhole just east of Varrock Palace.' / 'Members can also access the sewers through Edgeville Dungeon by using a pipe shortcut near Vannaka - this requires 51 Agility.'
- **Skeptic reasoning:** Data travel step says manhole 'behind Varrock Palace' and Edgeville access 'via the Edgeville Dungeon ladder'. Wiki: manhole is 'just east of Varrock Palace' (not behind), and the Edgeville connection is a pipe shortcut requiring 51 Agility, not a ladder. Both are minor wording/mechanism inaccuracies - correctly rated low severity. The Agility gate on the Edgeville route is also unstated in the data, which matters for routing.

## Obor

### [high] C1: The giant key is NOT consumed when entering Obor's lair. It IS consumed when opening the chest inside. The guidance claim inverts this -- players will be misled into farming extra keys thinking each entry burns one.

- **Data says:** Giant key is required to access Obor's lair and is consumed on each use
- **Wiki says (raw):** The key will not be consumed when unlocking Obor's lair. ... the chest present in his lair can be opened for loot by consuming the giant key.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Obor
- **Suggested fix:** Split the key usage description: clarify that the key unlocks the lair gate (not consumed) on first visit, and that a key is consumed each time the chest is opened for loot.
- **Skeptic receipt:** Obor wiki: "The key will not be consumed when unlocking Obor's lair." / "the chest present in his lair can be opened for loot by consuming the giant key"
- **Skeptic reasoning:** drop_rates.json Obor guidance step 3 reads: 'Use the Giant key on the gate to enter Obor's lair. The key is consumed - each kill needs a fresh key, so per-trip farm rate is gated by hill giant key drops.' This is factually inverted. The wiki states the key is NOT consumed to open the gate; it is consumed only to open the loot chest inside. So the gate is reusable indefinitely, and a key is burned per chest-open. The 'each kill needs a fresh key because the gate eats it' framing is wrong (the per-kill key cost is real, but the stated MECHANISM - gate consuming the key - is false, and the downstream 'restock a fresh Giant key' logic in steps 1/3/5 is anchored on the wrong mechanic). Refutation vectors checked: not a multi-source/variant/account-type issue - it is a flat mechanics error in the prose.

### [medium] C2: The 1/64 rate applies only to Wilderness locations (e.g. Lava Maze, Deep Wilderness Dungeon). Hill giants in Edgeville Dungeon drop the key at 1/128. The guidance overstates the rate by 2x for the recommended farming spot.

- **Data says:** Giant key drops from hill giants in Edgeville Dungeon or Varrock Sewers at approximately 1/64 rate
- **Wiki says (raw):** it's a 1/128 drop rate for the giant key outwith the wilderness and 1/64 inside it
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hill_Giant
- **Suggested fix:** Correct the drop rate to 1/128 for Edgeville Dungeon hill giants, and note 1/64 is Wilderness-only.
- **Skeptic receipt:** Hill Giant wiki: giant key "1/128; 2/128" with footnote "The drop rate is doubled in the Wilderness." => 1/128 outside Wilderness (Edgeville Dungeon), 1/64 only inside.
- **Skeptic reasoning:** Obor guidance step 1 prose says keys are 'farmed from hill giants in Edgeville Dungeon or Varrock Sewers - ~1/64 drop rate'. The recommended/listed farming spot is Edgeville Dungeon, which is NOT in the Wilderness. Wiki gives the giant key rate as 1/128 outside the Wilderness, doubled to 1/64 only inside it. So the ~1/64 figure overstates the rate 2x for the spot the guidance actually points players to. This is internally inconsistent too: the source's own item entry uses dropRate 0.0078 (? 1/128), so the prose contradicts its own data field. Not staleness (rate is current) and not account-type dependent.

### [high] C3: Varrock Sewers is not listed as a hill giant location anywhere on the Hill Giant wiki page. The listed free-to-play locations are Edgeville Dungeon, Giants' Plateau, Boneyard Hunter area, and Lava Maze. Directing players to Varrock Sewers for key farming is fabricated.

- **Data says:** Hill giants that drop Giant keys are located in Edgeville Dungeon and Varrock Sewers
- **Wiki says (raw):** The page does not mention Varrock Sewers as a Hill Giant location.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hill_Giant
- **Suggested fix:** Remove Varrock Sewers. Replace with accurate F2P alternatives such as Giants' Plateau or keep only Edgeville Dungeon.
- **Skeptic receipt:** Varrock Sewers wiki monster list: Moss giants present, "There is no mention of Hill Giants anywhere in the document." / Hill Giant wiki locations: "Varrock Sewers: Not listed as a Hill Giant location"; Edgeville Dungeon IS listed.
- **Skeptic reasoning:** Obor guidance step 1 prose lists key sources as 'hill giants in Edgeville Dungeon or Varrock Sewers'. Hill Giants do not spawn in the Varrock Sewers - the Varrock Sewers giant population is Moss Giants (which drop the Mossy key, a different item, used for Bryophyta). Both the Hill Giant location table and the Varrock Sewers monster list confirm no Hill Giants there. Directing key-farmers to Varrock Sewers is a fabricated location. Verified the Hill Giant NPC ids via cache for completeness; the error is the location, not the id.

### [medium] C5: The trapdoor entrance to Edgeville Dungeon is south of the bank, not north. A player following the guidance north of the bank will not find the entrance.

- **Data says:** Edgeville Dungeon entrance is accessible via a trapdoor north of Edgeville bank
- **Wiki says (raw):** One is found in Edgeville, in the ruined house south of the bank. It is a trapdoor by a coffin marked by a dungeon icon on the minimap.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Edgeville_Dungeon
- **Suggested fix:** Change 'north of Edgeville bank' to 'south of Edgeville bank'.
- **Skeptic receipt:** Edgeville Dungeon wiki: "One is found in Edgeville, in the ruined house south of the bank. It is a trapdoor by a coffin marked by a dungeon icon"
- **Skeptic reasoning:** Two Obor guidance steps direct the player to the 'trapdoor north of the bank' (step 1 conditional alternative: 'run east to the dungeon trapdoor north of the bank'; step 2: 'run east into the trapdoor north of the bank'). The Edgeville Dungeon trapdoor entrance is south of the Edgeville bank, in the ruined house. A player going north will not find it. Direct directional error, not variant/account/staleness.

### [blocker] C7: The wiki explicitly recommends Protect from Missiles, not Protect from Melee. Obor's ranged attack (max hit 26) is the primary prayer target -- it deals 50% less damage under Protect from Missiles. Using Protect from Melee instead leaves the 26-damage ranged attack fully unmitigated.

- **Data says:** Protect from Melee prayer should be used against Obor
- **Wiki says (raw):** always turn on Protect from Missiles before climbing down and starting the fight
- **Wiki URL:** https://oldschool.runescape.wiki/w/Obor
- **Suggested fix:** Replace 'Protect from Melee' with 'Protect from Missiles' in this guidance step.
- **Skeptic receipt:** Obor wiki: "always turn on Protect from Missiles before climbing down and starting the fight" / "Obor's ranged attack deals 50% less damage when Protect from Missiles is used." / Max hit: 22 (Melee), 26 (Ranged)
- **Skeptic reasoning:** Obor combat step says 'Use Protect from Melee' (and 'keep Protect from Melee active'). The wiki recommends Protect from Missiles, because Obor's ranged attack is his higher max hit (26) and is the attack a protection prayer can mitigate (50% reduction). Protect from Melee leaves the 26 ranged hit unmitigated and only blocks the lower 22 melee hit. This is a genuine combat-guidance error, not account-type dependent (Obor's mechanics are the same for all account types).

### [medium] C8: Obor's melee max hit is 22, not approximately 25. The guidance overstates it by ~14%, which may cause players to over-prepare food/prayer for the wrong attack.

- **Data says:** Obor performs a melee attack dealing approximately 25 damage
- **Wiki says (raw):** 22 (Melee)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Obor
- **Suggested fix:** Correct the melee max hit to 22.
- **Skeptic receipt:** Obor wiki: "Max hit: 22 (Melee) 26 (Ranged)"
- **Skeptic reasoning:** Obor guidance describes melee as 'hits ~25 with melee' (step 1) and '25-damage smash auto' (combat step). The wiki melee max hit is 22, not ~25. This is a numeric overstatement of ~14%. Minor in impact (downgrade to low severity) but the value is genuinely wrong against the authoritative max hit. Not a variant/staleness artifact.

### [blocker] C10: Two errors: (1) The ranged max hit is 26 (13 with Protect from Missiles active), not approximately 8. (2) The ranged attack does NOT ignore Protect from Melee in any meaningful sense -- the relevant prayer is Protect from Missiles which halves the damage. The claim mislabels the prayer and massively understates the damage, which is the single most dangerous attack Obor has.

- **Data says:** Obor occasionally throws a small boulder for approximately 8 ranged damage that ignores Protect from Melee
- **Wiki says (raw):** Obor's ranged attack deals 50% less damage when Protect from Missiles is used.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Obor
- **Suggested fix:** Correct the ranged attack max hit to 26 (13 with Protect from Missiles). Remove 'ignores Protect from Melee' and note that Protect from Missiles halves this attack's damage.
- **Skeptic receipt:** Obor wiki: Max hit 26 (Ranged); "Obor's ranged attack deals 50% less damage when Protect from Missiles is used."
- **Skeptic reasoning:** Obor combat step says he 'occasionally throws a small boulder for ~8 ranged damage that ignores Protect from Melee.' Two errors: (1) the ranged max hit is 26 (13 under Protect from Missiles), not ~8 - this badly understates the single most dangerous attack; (2) the framing 'ignores Protect from Melee' is misleading - the point is that the correct prayer is Protect from Missiles, which halves it. Combined with C7, the guidance both names the wrong prayer and understates the damage of the attack that prayer is meant to block. Confirmed mechanics, not variant/account-type.

### [medium] C11: The wiki describes a knockback mechanic on Obor's slam attack, not a 'bait the melee animation' safe-spot mechanic. The guidance implies standing under Obor is a reliable defensive technique, but the wiki indicates knockback still occurs even near a wall.

- **Data says:** Standing under Obor baits the melee animation
- **Wiki says (raw):** Obor's slam attack can knock you back, and your next attack will be slightly delayed whether you are standing next to a wall or not.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Obor
- **Suggested fix:** Replace the 'baits the melee animation' description with an accurate description of the knockback mechanic and note that standing against a wall is recommended to limit displacement, not to bait or skip attacks.
- **Skeptic receipt:** Obor wiki: "Obor's slam attack can knock you back, and your next attack will be slightly delayed whether you are standing next to a wall or not." / "Stand with your back against a wall to avoid knock-backs."
- **Skeptic reasoning:** Obor combat step says 'Stand under him to bait the melee animation.' There is no 'bait the melee animation' safe-spot/skip mechanic on the wiki. What the wiki describes is a slam/knockback: the slam can knock you back and delay your next attack whether or not you are against a wall, and the advice is to stand with your back to a wall to limit knockback displacement - not to bait or skip an attack. The data's framing misrepresents a knockback mechanic as an animation-baiting technique. Confirmed against wiki; not account/variant/staleness.

## Dagannoth Rex

### [high] C9: The rune thrownaxe is used to destroy all three doors blocking access to the Kings' lair (it bounces between them), not to selectively pull Dagannoth Rex. The thrownaxe is a door-opening mechanic, not a Rex-specific pull technique.

- **Data says:** A rune thrownaxe is required to pull only Dagannoth Rex
- **Wiki says (raw):** Take the northern or southern path, and use a Rune thrownaxe's special attack on the western (middle) door. This will cause the thrownaxe to bounce between and destroy all three doors.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Waterbirth_Island_Dungeon
- **Suggested fix:** Update the guidance step to describe the rune thrownaxe as needed to open the three locked doors leading to the Kings' lair (spec the middle door and it bounces to destroy all three), not as a Rex pull mechanic.
- **Skeptic receipt:** Take the northern or southern path, and use a Rune thrownaxe's special attack on the western (middle) door. This will cause the thrownaxe to bounce between and destroy all three doors, allowing you in.
- **Skeptic reasoning:** The data step reads 'Bring a rune thrownaxe (or Mind Altar shortcut at 70 Agility) to pull only Rex'. The rune thrownaxe special is a one-time ENTRY mechanic that destroys the three doors blocking the Kings' lair (it bounces between them), not a technique to selectively pull/aggro Dagannoth Rex. The wiki explicitly ties the thrownaxe spec to destroying the three doors. Conflating door-opening with a Rex-specific pull is a genuine guidance error. Not account-type or variant dependent.

### [high] C10: Two factual errors in one claim: (1) the agility requirement is 81 or 85, not 70; (2) there is no Mind Altar shortcut in Waterbirth Island Dungeon -- no Mind Altar is mentioned anywhere on the dungeon page.

- **Data says:** 70 Agility unlocks access to a Mind Altar shortcut
- **Wiki says (raw):** There is a shortcut on the eastern end of this room requiring level 81 Agility that leads directly to room 14. [Separately:] The propped doors can be bypassed with an alternate entrance on the highest point of waterbirth island. This can be reached with a shortcut requiring level 85 Agility.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Waterbirth_Island_Dungeon
- **Suggested fix:** Remove the Mind Altar reference entirely -- it does not exist in this dungeon. Update the agility level to 81 (shortcut to room 14) or 85 (surface bypass to the ladder) depending on which shortcut is intended.
- **Skeptic receipt:** level 85 Agility (highest point of Waterbirth Island); 81 Agility (Giant Rock Crab room shortcut). The term "Mind Altar" does not appear anywhere on this page.
- **Skeptic reasoning:** The data names a 'Mind Altar shortcut at 70 Agility'. WebFetch of the Waterbirth Island Dungeon page confirms the term 'Mind Altar' does not appear anywhere on the page, and the only Agility shortcuts are 81 (to room 14 / Giant Rock Crab room) and 85 (surface bypass), never 70. Both the named landmark and the level are fabricated. No variant/account-type rescue applies.

### [blocker] C14: Rex has +255 defence against all melee styles including crush -- it is not weak to crush. Rex's weakness is Magic (extremely low Magic Defence of +10, with 35% elemental weakness to earth spells). Sending players in with crush weapons is an inverted combat meta that will result in near-zero damage.

- **Data says:** Dagannoth Rex is weak to crush weapons
- **Wiki says (raw):** Melee Defence: +255 against stab, slash, and crush attacks
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dagannoth_Rex
- **Suggested fix:** Remove the crush weakness claim. Rex is weak to Magic only. The recommended attack method is magic (earth spells, powered staves, etc.).
- **Skeptic receipt:** Melee Defence: Stab +255, Slash +255, Crush +255; Magic Defence +10. Dagannoth Rex has a 35% weakness to earth spells.
- **Skeptic reasoning:** The data states 'Rex is weak to Magic and crush'. The Magic portion is correct, but the crush portion is refuted: WebFetch of the Rex page confirms +255 defence against stab, slash, AND crush, versus only +10 Magic defence. Rex is not weak to crush; it is maximally defended against it. The crush-weakness claim is an inverted combat meta and stands as a blocker on that portion. (The 'weak to Magic' half remains correct and is not a finding.)

### [blocker] C16: Same root error as C14. Rex has +255 crush defence -- crush weapons are maximally ineffective against it. This directly contradicts the wiki's unambiguous stats.

- **Data says:** Crush weapons are effective against Dagannoth Rex
- **Wiki says (raw):** Melee Defence: +255 against stab, slash, and crush attacks
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dagannoth_Rex
- **Suggested fix:** Remove crush weapon recommendations. Replace with Magic (earth spells for the 35% elemental weakness, or powered staves).
- **Skeptic receipt:** Dagannoth Rex has an extremely low Magic Defence, so it is recommended to use Magic against him. Popular choices include Slayer Dart, Iban Blast, powered staves, and Earth spells. [Crush Defence +255]
- **Skeptic reasoning:** Same root as C14: the data recommends 'a crush weapon work best'. Rex has +255 crush defence, so crush weapons are maximally ineffective. The wiki's own strategy recommends Magic due to the extremely low (+10) Magic defence. Recommending crush directly contradicts the unambiguous defensive stats. Blocker-level: it would send players to a near-zero-damage style.

### [medium] C15: The wiki recommends earth spells (Rex has a 35% elemental weakness to earth spells as of June 2025) and powered staves, not Fire Surge. Fire Surge is a fire spell and does not benefit from Rex's earth weakness. Tumeken's Shadow is not mentioned but as a powered staff it is plausible; Fire Surge specifically is a suboptimal recommendation given the earth spell weakness.

- **Data says:** Fire Surge or Tumeken's Shadow are effective weapons against Dagannoth Rex
- **Wiki says (raw):** Dagannoth Rex has an extremely low Magic Defence, so it is recommended to use Magic against him. Popular choices include Slayer Dart, Iban Blast, powered staves, and Earth spells.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dagannoth_Rex
- **Suggested fix:** Replace Fire Surge with earth spells (e.g. Earth Surge/Wave) to leverage the 35% elemental weakness. Tumeken's Shadow is acceptable as a powered staff. Cite earth spell weakness explicitly.
- **Skeptic receipt:** Dagannoth Rex has an extremely low Magic Defence, so it is recommended to use Magic against him. Popular choices include Slayer Dart, Iban Blast, powered staves, and Earth spells. Dagannoth Rex has a 35% weakness to earth spells.
- **Skeptic reasoning:** The data recommends 'fire spells (Fire Surge / Tumeken's Shadow) ... work best'. The wiki's recommended magic options are Slayer Dart, Iban Blast, powered staves, and Earth spells, and Rex carries a 35% weakness to EARTH spells (flagged as a recent change, so the wiki is current, not stale). Fire Surge is a fire spell and does not benefit from the earth weakness, making it a suboptimal/incorrect named recommendation. Tumeken's Shadow is a powered staff and is itself defensible, so the finding correctly scopes the error to the Fire Surge / 'fire spells' label rather than to magic in general. Medium severity is appropriate (suboptimal, not zero-damage).

### [medium] C4: The Hard Fremennik Diary unlocks the enchanted lyre as a direct teleport to Waterbirth Island -- it does not remove the 1,000 gp boat fee. The Fremennik Diary page lists no reward that waives the Jarvald boat cost. Players who follow this guidance expecting a free boat trip after completing the diary will still be charged.

- **Data says:** Hard Fremennik Diary removes the 1,000 gp boat fee to Waterbirth Island
- **Wiki says (raw):** Enchanted lyre and its imbued variant can now be played to teleport to Waterbirth Island, aside from Rellekka
- **Wiki URL:** https://oldschool.runescape.wiki/w/Enchanted_lyre
- **Suggested fix:** Correct the description: the Hard Fremennik Diary benefit is that the enchanted lyre gains a Waterbirth Island teleport destination, bypassing the need for the boat entirely -- not that the boat fee is removed.
- **Skeptic receipt:** [Jarvald] charges 1,000 coins per trip before quest completion; the fee is waived by completing The Fremennik Trials quest, not the Fremennik Diary: "after the quest, Jarvald will transport players for free." [Enchanted lyre] With Hard Fremennik Diary Completed: "Waterbirth Island" (gains this as an additional option).
- **Skeptic reasoning:** The data's conditionalAlternative says 'Rub the enchanted lyre to teleport directly to Rellekka, then take Jarvald's boat to Waterbirth Island for free. Hard Fremennik Diary removes the 1,000 gp boat fee.' Two errors: (1) the 1,000 gp Jarvald fee is waived by completing The Fremennik Trials quest, NOT by the Hard Fremennik Diary (Jarvald page: free after the quest; the fee applies while the quest is incomplete) - note the source's own main step 1 already states this correctly, making the alt-step internally inconsistent; (2) the lyre with the Hard Diary teleports DIRECTLY to Waterbirth Island, so the lyre->Rellekka->boat routing is both wrong and wasteful. The Hard Diary's benefit is the lyre teleport destination, not boat-fee removal. Multiple wiki receipts corroborate. Medium severity holds.

## Dagannoth Prime

### [high] C3: The 1,000 gp boat fee is waived by completing The Fremennik Trials quest, not by the Hard Fremennik Diary. The Hard diary adds an enchanted lyre teleport destination to Waterbirth Island but does not affect Jarvald's boat fee.

- **Data says:** Hard Fremennik Diary removes the 1,000 gp boat fee to Waterbirth Island
- **Wiki says (raw):** If the player has not completed The Fremennik Trials quest, they must pay 1,000 coins per trip; after the quest, Jarvald will transport players for free.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Jarvald
- **Suggested fix:** Change the gate condition from Hard Fremennik Diary to completion of The Fremennik Trials quest for the free Jarvald boat passage.
- **Skeptic receipt:** Jarvald page: 'If the player has not completed The Fremennik Trials quest, they must pay 1,000 coins per trip; after the quest, Jarvald will transport players for free.' The Fremennik Trials rewards: 'Jarvald will charge no fee for travelling to Waterbirth Island'.
- **Skeptic reasoning:** The data's step-1 conditional alternative (gated on FREMENNIK_HARD) states 'Hard Fremennik Diary removes the 1,000 gp boat fee.' This misattributes the free-boat benefit. Authoritative pages confirm the 1,000 gp Jarvald fee is waived by completing The Fremennik Trials QUEST, not the Hard Fremennik Diary. Ran the refutation vectors: it is not account-type-dependent (the fee waiver applies identically to all account types via the quest), not staleness (the base step itself already correctly says '1,000 gp without The Fremennik Trials', and the mechanic is long-standing), and not a variant issue. The Hard diary does legitimately grant the enchanted-lyre teleport-to-Rellekka destination, but it does NOT touch the boat fee. A player with the Hard diary but without the Trials quest would still pay 1,000 gp, so the alternative's prose is a genuine requirement-nuance error.

### [high] C4: Free passage via Jarvald's boat is gated on The Fremennik Trials quest completion, not the Hard Fremennik Diary. The diary instead unlocks enchanted lyre teleport to Waterbirth Island.

- **Data says:** Hard Fremennik Diary allows free passage via Jarvald's boat to Waterbirth Island
- **Wiki says (raw):** If the player has not completed The Fremennik Trials quest, they must pay 1,000 coins per trip; after the quest, Jarvald will transport players for free.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Jarvald
- **Suggested fix:** Replace the diary gate with The Fremennik Trials quest completion as the condition for free Jarvald boat passage. The Hard diary benefit (lyre teleport to Waterbirth Island) could be noted separately as an alternative travel method.
- **Skeptic receipt:** Jarvald page: 'If the player has not completed The Fremennik Trials quest, they must pay 1,000 coins per trip; after the quest, Jarvald will transport players for free.'
- **Skeptic reasoning:** Same underlying error as C3, framed as the free-passage claim: the data ties free Jarvald passage to the Hard Fremennik Diary ('take Jarvald's boat to Waterbirth Island for free. Hard Fremennik Diary removes the 1,000 gp boat fee'), but free passage is gated on The Fremennik Trials quest. Confirmed against two independent authoritative pages. The diary's actual benefit (enchanted lyre teleport destination to Rellekka/Waterbirth) is real and can stay, but the fee-waiver attribution is wrong. Not account-type or progress dependent in a way that rescues it; not stale.

### [high] C7: The wiki lists agility shortcuts at 85 and 81, not 70. A 70 Agility shortcut does not exist in the Waterbirth Island Dungeon according to the wiki. The pet rock is used to open doors (as a substitute player), not combined with an agility shortcut.

- **Data says:** A pet rock combined with 70 Agility can access a shortcut to lure Prime away from Supreme and Rex
- **Wiki says (raw):** 85 Agility shortcut: Bypasses door mechanics entirely, "placing the player directly next to the ladder and skipping the need to bring a pet rock, rune thrownaxes, or another player." 81 Agility shortcut: Located in the Giant Rock Crab room, "leads directly to room 14" as a one-way passage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Waterbirth_Island_Dungeon
- **Suggested fix:** Remove the 70 Agility reference. The pet rock is used to open the multi-player doors (not tied to any agility shortcut). The relevant agility shortcuts are 85 (bypasses doors entirely) and 81 (Giant Rock Crab room). Update the description to accurately reflect these mechanics.
- **Skeptic receipt:** Waterbirth Island Dungeon: 85 Agility shortcut places players 'directly next to the ladder and skipping the need to bring a pet rock, rune thrownaxes, or another player.' 81 Agility shortcut in the Giant Rock Crab room 'leads directly to room 14.' Pet rock usage: 'the player can use the pet rock or the egg from Scrambled! to substitute as a player to open the doors.'
- **Skeptic reasoning:** The data's step-2 description says 'Bring a rune thrownaxe (or pet rock + 70 Agility shortcut) to lure Prime away from Supreme and Rex.' Two errors confirmed against the Waterbirth Island Dungeon wiki: (1) no 70 Agility shortcut exists - the dungeon shortcuts are at 81 (Giant Rock Crab room, one-way to room 14) and 85 (places player next to the ladder); (2) the pet rock is NOT combined with an agility shortcut - it substitutes as a second 'player' to knock down the doors, an alternative to bringing a rune thrownaxe or another player. The 85 shortcut explicitly skips the need for a pet rock, rune thrownaxes, or another player, confirming the pet rock and shortcuts are mutually exclusive paths, not a combination. The '70 Agility shortcut' is fabricated and the pet-rock mechanic is mischaracterized. Refutation vectors clear: not a variant, not account-type dependent, and the established shortcut levels rule out staleness.

## Dagannoth Supreme

### [medium] C2: The guidance says 'Lunar Isle teleport' but the relevant Lunar spellbook spell is 'Waterbirth Teleport', which teleports directly to Waterbirth Island -- not to Lunar Isle. The Lunar Isle teleport (Moonclan Teleport) goes to Lunar Isle, a different destination. A player following the guidance literally may use the wrong spell.

- **Data says:** Travel to Waterbirth Island via Rellekka docks (1,000 gp without The Fremennik Trials) or Lunar Isle teleport
- **Wiki says (raw):** There are several methods to quickly getting to Waterbirth Island, with the Waterbirth Teleport from the Lunar spellbook, a portal (nexus) in a POH directed to the island, or using an enchanted lyre to teleport to the island directly (after completion of the Hard Fremennik Diary) being the fastest ways.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dagannoth_Kings
- **Suggested fix:** Replace 'Lunar Isle teleport' with 'Waterbirth Teleport (Lunar spellbook)' to name the correct spell.
- **Skeptic receipt:** Waterbirth Teleport: 'Teleports you to Waterbirth Island' -- more specifically the spell takes you 'beside Jarvald's ship on Waterbirth Island, right by the snape grass spawns.' (oldschool.runescape.wiki/w/Waterbirth_Teleport)
- **Skeptic reasoning:** Step[0] offers 'Lunar Isle teleport' as the Lunar-spellbook option to reach Waterbirth Island, and the entry's travelTip reads 'Lunar Isle tele -> Rellekka'. The relevant Lunar spell is Waterbirth Teleport, which the wiki confirms drops the player 'beside Jarvald's ship on Waterbirth Island' -- it does NOT teleport to Lunar Isle, and it does NOT teleport to Rellekka. 'Lunar Isle teleport' names a different spell (Moonclan/Lunar Isle Teleport) with a different destination, so a literal reading misdirects the player. This is a naming error, not a game mechanic -- confirmed at LOW severity. The fix should name 'Waterbirth Teleport (Lunar spellbook)'.

### [high] C3: The conditional alternative (gated on Hard Fremennik Diary) describes the lyre as teleporting to Rellekka and then taking Jarvald's boat -- but that is the non-diary route. The Hard Diary reward is that the lyre gains a direct teleport to Waterbirth Island, bypassing the boat entirely. The guidance inverts the benefit: it assigns the Rellekka-then-boat route to the diary path and mischaracterises the diary reward as a 'fee removal' rather than a new teleport destination.

- **Data says:** Rub the enchanted lyre to teleport directly to Rellekka, then take Jarvald's boat to Waterbirth Island for free. Hard Fremennik Diary removes the 1,000 gp boat fee.
- **Wiki says (raw):** When the hard Fremennik Diary is completed, the enchanted lyre can also teleport to Waterbirth Island, and gains the ability to teleport to Jatizso and Neitiznot after completion of the elite Fremennik Diary.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Enchanted_lyre
- **Suggested fix:** Rewrite the conditional alternative: 'Rub the enchanted lyre to teleport directly to Waterbirth Island (Hard Fremennik Diary unlocks this teleport destination on the lyre).' Remove the boat and fee language from this alternative.
- **Skeptic receipt:** Enchanted lyre: 'When the hard Fremennik Diary is completed, the enchanted lyre can also teleport to Waterbirth Island.' The lyre gains the ability to teleport to Waterbirth Island as an additional destination after the Hard diary (oldschool.runescape.wiki/w/Enchanted_lyre).
- **Skeptic reasoning:** The Hard-diary conditionalAlternative on step[0] describes the lyre as teleporting 'directly to Rellekka, then take Jarvald's boat to Waterbirth Island'. That is the pre-diary behavior. The wiki confirms the Hard Fremennik Diary reward is that the lyre gains Waterbirth Island as an additional DIRECT teleport destination, bypassing Jarvald's boat entirely. The alternative inverts the diary benefit (assigns the boat route to the diary path). Confirmed -- and it should be fixed jointly with C5 since they describe the same inverted alternative.

### [high] C5: The Hard Fremennik Diary does not reduce or remove Jarvald's boat fee. The diary reward is that the enchanted lyre gains Waterbirth Island as a teleport destination, bypassing the boat route altogether. The claim mischaracterises the diary benefit and may mislead players into paying 1,000 gp even after diary completion.

- **Data says:** Hard Fremennik Diary removes the 1,000 gp boat fee.
- **Wiki says (raw):** When the hard Fremennik Diary is completed, the enchanted lyre can also teleport to Waterbirth Island
- **Wiki URL:** https://oldschool.runescape.wiki/w/Enchanted_lyre
- **Suggested fix:** Replace 'Hard Fremennik Diary removes the 1,000 gp boat fee' with 'Hard Fremennik Diary adds Waterbirth Island as a lyre teleport destination, skipping the boat entirely.'
- **Skeptic receipt:** Waterbirth Island: boat 'costs 1,000 coins each time if the quest [The Fremennik Trials] is not complete'. Enchanted lyre: 'When the hard Fremennik Diary is completed, the enchanted lyre can also teleport to Waterbirth Island.'
- **Skeptic reasoning:** Two independent errors. (1) The 1,000 gp Jarvald boat fee is removed by completing The Fremennik Trials QUEST, not the Hard Fremennik Diary -- the Waterbirth Island page states the boat 'costs 1,000 coins each time if the quest is not complete', and step[0]'s own base text already correctly attributes the fee to 'without The Fremennik Trials'. (2) The Hard diary's actual reward is a new lyre teleport destination to Waterbirth Island, not a boat-fee waiver. The conditional therefore both mischaracterises the diary reward and conflates the quest gate with the diary gate. Confirmed.

### [low] C10: The wiki shows identical defence values for stab (+10), slash (+10), and crush (+10) -- Supreme has no differential melee weakness. The wiki describes the weakness only as 'melee' in general. Singling out stab and slash is unsupported; crush performs equally well.

- **Data says:** Supreme is weak to stab/slash
- **Wiki says (raw):** Dagannoth Supreme is most weak to melee attacks. [Stab defence: +10, Slash defence: +10, Crush defence: +10 -- all equal]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dagannoth_Supreme
- **Suggested fix:** Replace 'weak to stab/slash' with 'weak to Melee (stab, slash, and crush equally)' or simply 'weak to Melee'.
- **Skeptic receipt:** Dagannoth Supreme: melee defence '+10 for stab, slash, and crush attacks' (all equal); 'most weak to melee attacks' (oldschool.runescape.wiki/w/Dagannoth_Supreme).
- **Skeptic reasoning:** Step[2] says 'Supreme is weak to stab/slash'. The wiki lists Supreme's melee defence as equal across all three melee styles (+10 stab, +10 slash, +10 crush) and describes the weakness only as 'most weak to melee attacks' -- there is no differential stab/slash defensive hole; crush is mechanically just as effective. The suggested meta weapons (scimitar/whip = slash, fang = stab) are correct, but stating a defensive weakness to stab/slash specifically is inaccurate. Confirmed at LOW severity -- fix to 'weak to Melee'.

Total: 107 confirmed findings.