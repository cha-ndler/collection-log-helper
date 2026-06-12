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
# Tranche 2: sources 31-70

Run summary: 40 sources processed; 33 with confirmed findings; clean: Kraken, Chambers of Xeric (Challenge Mode), Crawling Hand, Cave Crawler, Rockslug, Cockatrice, Mogre.

## Thermonuclear smoke devil

### [blocker] C9: The guidance recommends Protect from Magic against regular smoke devils, but the wiki explicitly states Protect from Missiles negates their attacks. Smoke devils use a magical ranged attack, not a magic attack -- the wrong prayer is recommended.

- **Data says:** Regular smoke devils can be avoided by flicking Protect from Magic
- **Wiki says (raw):** When fighting them, using Protect from Missiles will negate their attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Smoke_devil
- **Suggested fix:** Replace 'Protect from Magic' with 'Protect from Missiles' for the regular smoke devil traversal step.
- **Skeptic receipt:** Smoke devil page (osrs wiki): attack style 'Magical ranged'; 'When fighting them, using Protect from Missiles will negate their attacks.'
- **Skeptic reasoning:** The data guidance step reads 'Take care of the regular smoke devils on the way if Protect from Magic is not flicked.' Regular smoke devils use a Magical ranged attack, which is a Ranged-type attack and is negated by Protect from Missiles, NOT Protect from Magic. The wiki Smoke devil page is explicit. Protect from Magic does nothing against their attacks. This is a genuine wrong-prayer error, not a defensible variant or account-type nuance.

### [blocker] C10: The boss uses a typeless attack, which is not reduced by any protection prayer. The strategies page recommends Piety (melee) or Redemption (magic) -- not Protect from Magic. Advising Protect from Magic will give players false confidence that they are reducing incoming damage.

- **Data says:** Protect from Magic prayer should be used against the Thermonuclear smoke devil
- **Wiki says (raw):** The thermonuclear smoke devil only attacks with a typeless magical ranged attack
- **Wiki URL:** https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies
- **Suggested fix:** Remove the Protect from Magic recommendation. The correct advice is to use Piety for melee or Redemption for magic; no protection prayer mitigates the boss's typeless hits.
- **Skeptic receipt:** Thermonuclear smoke devil/Strategies (osrs wiki): max hit 8 via a 'typeless magical ranged attack'; "'Typeless' means that protection prayers will not have any effect."
- **Skeptic reasoning:** The boss guidance reads 'Use Protect from Magic - it hits 30s+ through no prayer.' The Thermonuclear smoke devil's attack is a TYPELESS magical ranged attack; typeless damage is unaffected by any protection prayer. Recommending Protect from Magic as mitigation is wrong and gives false confidence. Confirmed via the Strategies page. This is not account-type or progress dependent.

### [blocker] C11: The claimed max hit of 30+ is nearly four times the wiki-confirmed max hit of 8. This overstates the danger and is factually wrong.

- **Data says:** The Thermonuclear smoke devil hits 30s or more through no prayer
- **Wiki says (raw):** The max hit of this attack is 8.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies
- **Suggested fix:** Update the max hit reference to 8, in line with the wiki.
- **Skeptic receipt:** Thermonuclear smoke devil/Strategies (osrs wiki): 'The max hit of this attack is 8.'
- **Skeptic reasoning:** The boss guidance states the boss 'hits 30s+ through no prayer.' The wiki-confirmed max hit of the boss's attack is 8. 30+ overstates the damage by roughly 4x and is factually wrong. No game-version drift accounts for a 30+ figure (wiki_updates not needed; max hit 8 is long-standing).

### [high] C12: The wiki explicitly states the boss was designed to walk through obstacles specifically to prevent rock-based safe-spotting. A freeze-and-retreat method using magic's longer attack range exists, but it is not a static 'behind a rock' safe-spot. Telling players to safe-spot behind a rock will not work.

- **Data says:** The Thermonuclear smoke devil can be safe-spotted behind a rock
- **Wiki says (raw):** Unlike most NPCs, the Thermonuclear smoke devil is always able to walk through other monsters/players. This is to prevent players from safespotting the boss using the smoke devils found in its lair.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil
- **Suggested fix:** Remove the rock safe-spot claim. If a safe-distance method is described, frame it as the freeze-kite technique: freeze the boss and move just outside its 8-tile attack range while remaining within magic's 9-tile range.
- **Skeptic receipt:** Thermonuclear smoke devil (osrs wiki): boss 'is always able to walk through other monsters/players. This is to prevent players from safespotting the boss using the smoke devils found in its lair.' Strategies: "player's magic spells have a longer attack range (9 tiles) than the thermonuclear smoke devil (8 tiles)... utilised by freezing the thermonuclear smoke devil and then moving out of its attack range."
- **Skeptic reasoning:** The boss guidance says 'safe-spot the boss behind a rock so you can flick the prayer easily.' The main wiki page states the boss is deliberately able to walk through other monsters/players specifically to PREVENT rock/obstacle safespotting. The only ranged-distance technique the Strategies page describes is freeze-and-kite exploiting magic's 9-tile range vs the boss's 8-tile range - that is a kiting method, not a static rock safespot. The 'behind a rock' instruction is incorrect.

### [medium] C6: Masked earmuffs are listed as valid protection but the wiki does not include them in the valid protection list. The wiki lists gas mask, facemask, and slayer helmet. Including masked earmuffs could mislead players into entering the dungeon unprotected.

- **Data says:** A facemask, Slayer helmet, or Masked earmuffs is required to survive the smoke devil dungeon without constant damage
- **Wiki says (raw):** players must equip either a gas mask, facemask, or slayer helmet
- **Wiki URL:** https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies
- **Suggested fix:** Replace 'Masked earmuffs' with 'gas mask' to match the wiki's protection list: facemask, slayer helmet, or gas mask.
- **Skeptic receipt:** Thermonuclear smoke devil/Strategies (osrs wiki): 'players must equip either a gas mask, facemask, or slayer helmet, or they will take damage from the smoke.' RuneLite ItemID: SLAYER_FACEMASK=4164 and SLAYER_EARMUFFS=4166 are distinct items; no 'Masked earmuffs' item exists.
- **Skeptic reasoning:** The enter-cave step lists 'A facemask, Slayer helmet, or Masked earmuffs is required.' The wiki's valid smoke-protection list is gas mask, facemask, or slayer helmet. 'Masked earmuffs' is not a valid smoke-protection item - it is not even a real OSRS item: the RuneLite cache has separate SLAYER_FACEMASK (4164) and SLAYER_EARMUFFS (4166); earmuffs protect against scream/sound, not smoke. So the listed item neither exists nor protects against the dungeon's smoke. The travel step one line up already correctly says 'facemask or Slayer helmet', confirming the 'Masked earmuffs' entry is an erroneous fabrication, not a legitimate alternative.

## Abyssal Sire

### [medium] C12: The Sire releases miasma pools when it walks to the centre at ~210/424 HP (~50%), which the wiki calls the start of Phase 3. The guidance attributes this to 'Phase 3 (25% HP)', but 25% is not a canonical threshold -- the wiki's Phase 3 Stage II (teleport + AoE) triggers at ~140 HP (~33%). The 25% figure is inaccurate.

- **Data says:** Phase 3 (25% HP): tentacles re-activate and Sire releases poison miasma pools
- **Wiki says (raw):** When the Sire gets below 210 health, it will walk to the middle of the room and stop moving. It no longer attacks with melee, instead creating many spawns and summoning a miasma pool under the player every few seconds.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Abyssal_Sire
- **Suggested fix:** Remove the '25% HP' label; the miasma pool phase (wiki Phase 3) begins at ~50% HP (~210 HP). The Phase 3 Stage II escalation is around 33-35% HP (~140-143 HP).
- **Skeptic receipt:** Raw Strategies line 118: 'Once the Sire is damaged below 35% of its health (around 143 or so health), it will teleport the player, beginning the final stage.' Raw line 75: 'During Phase 3, when the Sire goes under 140 health, it will teleport the player next to itself and trigger a highly damaging explosion'.
- **Skeptic reasoning:** The data's 'Phase 3 (25% HP)' uses a threshold that appears nowhere on the wiki. 25% of the Sire's 424 HP is ~106 HP, which matches no documented trigger. The wiki's only sub-50% thresholds are the 35%/~143 HP final-stage teleport and the <140 HP explosion trigger. The 25% label is a factual numeric error. (This is a low-severity wording/number fix, not the blocker -- that is C17.)

### [blocker] C17: In Phase 3 (wiki naming), the Sire explicitly stops using melee attacks entirely. Recommending Protect from Melee during this phase is incorrect -- the correct protection during the spawns stage is Protect from Missiles (the spawns deal ranged damage). Running Protect from Melee when the boss no longer melees provides no mitigation and wastes a prayer slot versus the actual threat.

- **Data says:** keep Protect from Melee [during Phase 3]
- **Wiki says (raw):** It no longer attacks with melee, instead creating many spawns and summoning a miasma pool under the player every few seconds.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Abyssal_Sire
- **Suggested fix:** Change 'keep Protect from Melee' in the Phase 3 description to 'switch to Protect from Missiles' to mitigate damage from the spawns that appear in Phase 3.
- **Skeptic receipt:** Raw Strategies line 60: 'Because the ranged attacks are much more damaging and accurate than their melee attacks, it is highly recommended to keep Protect from Missiles on at all times, as numerous adds using ranged can quickly kill a player in seconds regardless of their defence bonuses.' Line 110: 'Once the Sire is damaged below 50% of its health (212 health or less) it will start walking south. Run to Row 2 and activate Protect from Missiles.' Main page: 'It no longer attacks with melee, instead creating many spawns and summoning a miasma pool under the player every few seconds.'
- **Skeptic reasoning:** Blocker confirmed. The data recommends 'keep Protect from Melee' during Phase 3, but in Phase 3 the Sire no longer melees and the lethal threat is ranged spawns/scions. The wiki unambiguously directs Protect from Missiles for this phase and at the 50% transition. Protect from Melee in Phase 3 provides no mitigation against the actual ranged add damage and is incorrect guidance. Not refutable by account type, variant, or staleness: the spawn-damage mechanic and the prayer recommendation are stable and current on the wiki.

## King Black Dragon

### [blocker] C9: The wiki's primary recommendation is Protect from Melee when fighting at melee distance, not Protect from Magic. Protect from Magic is only recommended when using two-handed ranged/magic setups that cannot also use a shield. Recommending Protect from Magic unconditionally inverts the main combat meta and would leave melee players unprotected from KBD's primary melee hits.

- **Data says:** Protect from Magic prayer should be used during KBD fight
- **Wiki says (raw):** It is recommended to fight the King Black Dragon in Melee distance with the Protect from Melee prayer activated.
- **Wiki URL:** https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies
- **Suggested fix:** Change to 'Use Protect from Melee when fighting at melee distance. Use Protect from Magic if using a two-handed ranged or magic setup (e.g. twisted bow).'
- **Skeptic receipt:** Wiki KBD/Strategies: "It is recommended to fight the King Black Dragon in Melee distance with the Protect from Melee prayer activated." ... "However, when a player is using a two-handed weapon, such as the twisted bow, the Protect from Magic prayer is necessary to partly negate the damage of the dragonfire attacks."
- **Skeptic reasoning:** Data step 3 says 'Use Protect from Magic' unconditionally AND 'fight with ranged or melee stab'. The wiki's primary recommendation is Protect from Melee at melee distance; Protect from Magic is only the exception for two-handed weapons (e.g. twisted bow) that cannot equip a shield. For the melee-stab path the data explicitly prescribes, the recommended prayer is wrong, leaving melee players unprotected from KBD's primary stab hits. This inverts the main combat meta -- same class of error as the Shellbane Gryphon fix on this branch. STANDS as blocker.

### [high] C10: The wiki documents KBD's attack styles as 'Stab, Dragonfire' only. The wiki's poison mechanic comes from the toxic breath (poison dragonfire) attack, not from a melee hit. There is no venom-laced melee attack described anywhere on the page or strategies subpage.

- **Data says:** KBD has a venom-laced melee attack
- **Wiki says (raw):** The King Black Dragon uses melee and dragonfire ... Attack style: Stab, Dragonfire
- **Wiki URL:** https://oldschool.runescape.wiki/w/King_Black_Dragon
- **Suggested fix:** Remove the 'venom-laced melee attack' claim. KBD's melee is a standard stab attack. Poison comes from the toxic breath (poison dragonfire), which antipoison/antidote++ protects against.
- **Skeptic receipt:** Wiki KBD infobox Attack style: "Stab, Dragonfire". Wiki KBD/Strategies: "Toxic breath inflicts poison to the player, starting at 8 damage" (poison is from breath, not melee). No 'venom-laced melee attack' appears on the page.
- **Skeptic reasoning:** Data step 3 states 'the dragon also has a venom-laced melee attack'. The wiki infobox attack style is 'Stab, Dragonfire' -- KBD's melee is a plain stab. The poison originates from the toxic breath (a dragonfire variant), not from melee. The strategies page confirms 'Toxic breath inflicts poison to the player, starting at 8 damage', and the NPC infobox lists 'Poisonous: Yes (8)' tied to the fiery/dragon breath, not a venom-laced melee. The data conflates the poison breath with the melee attack. STANDS as high.

### [high] C11: The wiki lists KBD's attack styles as 'Stab, Dragonfire' with no ranged attack style. KBD does not use projectile ranged attacks. This claim is fabricated and would mislead players into incorrect prayer or gear decisions.

- **Data says:** KBD uses ranged attacks
- **Wiki says (raw):** The King Black Dragon uses melee and dragonfire ... Attack style: Stab, Dragonfire
- **Wiki URL:** https://oldschool.runescape.wiki/w/King_Black_Dragon
- **Suggested fix:** Remove the ranged attack claim. KBD uses melee (stab) and four dragonfire variants (normal, shock, ice, poison). There is no ranged attack.
- **Skeptic receipt:** Wiki KBD infobox Attack style: "Stab, Dragonfire". Wiki KBD page: "The King Black Dragon uses melee and dragonfire, which is of a higher tier compared to those made by adult chromatic dragons." -- no ranged attack listed.
- **Skeptic reasoning:** Data step 3 claims 'The KBD uses dragonfire, magic, melee, ranged and a poison breath'. The wiki infobox attack style is 'Stab, Dragonfire' only, and the description states 'The King Black Dragon uses melee and dragonfire'. There is no ranged attack style documented; KBD's dragonfire is its magic-style component. The 'ranged' attack is fabricated and could mislead prayer/gear choices. STANDS as high.

## Chaos Elemental

### [medium] C8: The wiki describes no healing mechanic for the Chaos Elemental. The strategies page states the player will 'always be taking damage', implying the boss never pauses to heal. No phase-based healing is documented anywhere on the main page or strategies subpage.

- **Data says:** Chaos Elemental heals modestly between phases
- **Wiki says (raw):** you will always be taking damage unless you are flinching or using the safe spot. Therefore, it is critical to maximise the amount of healing you bring while minimising your risk.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chaos_Elemental/Strategies
- **Suggested fix:** Remove the claim that the boss 'heals modestly between phases'. The Chaos Elemental has no documented self-healing mechanic; its three attacks are Discord, Confusion, and Madness -- none involve the boss regaining HP.
- **Skeptic receipt:** Strategies page: 'As it attacks with random damage types, you will always be taking damage unless you are flinching or using the safe spot.' Main page mechanics fetch: 'The page contains no mention of self-healing, hitpoint regeneration, or any recovery mechanics during combat. There is no discussion of the boss regaining health at any point... The article does not reference any phase mechanics. The boss appears to be a single-phase encounter.' Infobox: 'Hitpoints: 250'. Attacks: Discord (random damage type), Confusion (teleport), Madness (unequips up to four items).
- **Skeptic reasoning:** The guidance step at drop_rates.json line 4963 states 'The boss heals modestly between phases but has no combat triggers.' This invents two mechanics that do not exist. (1) No healing: both the main page and Strategies page fetch confirm zero mention of self-healing or HP regeneration; the infobox lists a fixed 250 HP and the documented attacks are only Discord, Confusion, and Madness, none of which restore the boss's HP. (2) No phases: the wiki documents no phase mechanic at all - it is a single-phase encounter with one consistent attack pattern, so 'between phases' is also fabricated. This is not a multi-source/variant/account-type case - it is a flat factual error in guidance text. The 'always be taking damage' line is corroborating context (the player is always taking damage; nothing implies the boss recovers HP). The fix is to drop the 'heals modestly between phases' clause; the kit/Protect-from-Magic/disarm advice in the same step is otherwise accurate to the Madness mechanic.

## Scorpia

### [medium] C2: The guidance describes the offspring's status effect as 'venom' but the wiki says they inflict poison. Venom and poison require different cures (anti-venom+ vs anti-poison), so describing this as venom could mislead players about which supplies to bring -- though in practice anti-venom+ cures both, so the practical impact is low.

- **Data says:** Scorpia's guardians inflict venom that ticks 6 per hit
- **Wiki says (raw):** Both Scorpia and her offspring are capable of inflicting poison, starting at 20 and 6 damage, respectively.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scorpia
- **Suggested fix:** Replace 'venom that ticks 6 per hit' with 'poison starting at 6 damage' to match the wiki.
- **Skeptic receipt:** Both Scorpia and her offspring are capable of inflicting poison, starting at 20 and 6 damage, respectively.
- **Skeptic reasoning:** The guidance labels the offspring/guardian status effect as 'venom that ticks 6 per hit', but the wiki explicitly calls it 'poison' (offspring starting at 6 damage, matching the '6' figure). Poison and venom are distinct OSRS mechanics with different cures. Refutation vectors fail: not account-type or content dependent, no recent wiki edit (wiki_updates returned 0 changes since 2023), and the quote in context uses the word 'poison' unambiguously. So the term is factually wrong. Practical impact is low because the guidance recommends anti-venom+, which also cures regular poison, so a player following it is not actually under-equipped. Confirmed as a factual mislabel of low real-world severity rather than the finding's stated medium.

### [blocker] C7: The guidance instructs players to use Protect from Melee, but the Strategies page recommends Protect from Missiles to block damage from Scorpia's offspring (which deal ranged attacks). The main wiki page also notes that Scorpia's melee drains 2 prayer points even when Protect from Melee is active, making it an inefficient choice. Following the guidance's prayer recommendation means taking full damage from the offspring's ranged attacks throughout the fight.

- **Data says:** Kill Scorpia. Use Protect from Melee
- **Wiki says (raw):** praying Protect from Missiles to prevent damage from Scorpia's offspring
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scorpia/Strategies
- **Suggested fix:** Change the protection prayer recommendation from 'Protect from Melee' to 'Protect from Missiles' (to block offspring ranged damage). Optionally note that Scorpia herself drains prayer through Protect from Melee regardless.
- **Skeptic receipt:** praying Protect from Missiles to prevent damage from Scorpia's offspring [Strategies] | Scorpia herself uses melee attacks which, whether successful or not, drain the player's prayer points by 2 if they have the Protect from Melee prayer active [main page]
- **Skeptic reasoning:** Guidance instructs 'Use Protect from Melee'; the Strategies page explicitly recommends 'Protect from Missiles' to block the offspring's ranged damage, and the main page notes Protect from Melee is drained by Scorpia's own melee. Refutation attempt: Protect from Melee is not nonsensical because Scorpia herself attacks with melee, so it blocks her hits and is partially legitimate. But the authoritative recommended prayer for this fight is Protect from Missiles (the offspring/guardians are the sustained ranged threat the guidance itself warns about), so the data contradicts the wiki's explicit recommendation. This is a genuine recommendation-quality bug. Downgrading from the finding's 'blocker' to high, since the recommended prayer still mitigates the boss's own melee and is not a flat fabrication.

### [high] C8: The guidance recommends crush as the primary attack style, but crush is tied for Scorpia's highest defence (+284), equal to slash. Stab is her weakest melee defence (+246) but still high. The Strategies page does not recommend any melee style -- it recommends magic, exploiting her very low magic defence (+44). Claiming crush 'bypasses her defence' is directly contradicted by her defence stat table.

- **Data says:** attack with crush (Bandos godsword, Inquisitor's mace) or stab to bypass her defence
- **Wiki says (raw):** Stab: +246 [defence], Slash: +284 [defence], Crush: +284 [defence] ... An effective strategy is to exploit her low magic defence by using ice barrage to freeze her.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scorpia/Strategies
- **Suggested fix:** Remove crush from the weapon recommendation. Either recommend stab as the best melee option (noting +246 vs +284 for crush/slash), or align with the strategies page and recommend magic (fire spells have 35% elemental weakness as of June 2025).
- **Skeptic receipt:** Stab: +246, Slash: +284, Crush: +284 [melee defence]; Magic defence bonus: +44; An effective strategy is to exploit her low magic defence ... using ice barrage to freeze her ... staying out of melee range while attacking her with magic.
- **Skeptic reasoning:** Guidance recommends crush 'to bypass her defence', but the defensive stat table shows crush (+284) is tied for her HIGHEST melee defence, equal to slash, while stab (+246) is the weakest melee and magic (+44) is by far her lowest defence overall. The Strategies page recommends magic/freezing, not melee. Crush therefore does not 'bypass' her defence; it hits her best-defended style. Refutation vectors fail: stats are stable (no recent wiki edit) and the claim is directly contradicted by the quoted stat table. Confirmed at high severity.

### [high] C9: The guidance states guardians spawn at ~33% HP, but the wiki clearly states the threshold is 50% HP. The Strategies page corroborates this: 'Once Scorpia reaches 99 health or lower' (which is 50% of her 198 HP). Players following the ~33% figure will be surprised by the guardian spawn much earlier than expected, potentially catching them off guard mid-fight.

- **Data says:** At ~33% HP Scorpia spawns two Scorpia's Guardians that heal her
- **Wiki says (raw):** Once Scorpia falls below 50% health, the next attack against her, whether successful or not, will cause her to summon two Scorpia's guardians, which heal her rapidly.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scorpia
- **Suggested fix:** Change '~33% HP' to '~50% HP' (or '99 HP or lower') to match the wiki.
- **Skeptic receipt:** Once Scorpia falls below 50% health, the next attack against her ... will cause her to summon two Scorpia's guardians, which heal her rapidly. [main page] | Once Scorpia reaches 99 health or lower, she will summon two Scorpia's guardians [Strategies]
- **Skeptic reasoning:** Guidance states guardians spawn at '~33% HP'; both wiki pages put the threshold at 50% (below 50% health, equivalently 99 of her 200 HP). 99/200 is ~49.5%, i.e. ~50%, not ~33%. Refutation vectors fail: not account-type dependent, no recent wiki edit, and both authoritative pages agree on the 50%/99-HP threshold. A player relying on the ~33% figure would be surprised by an earlier guardian spawn. Confirmed at high severity.

## Chaos Fanatic

### [high] C3: Mage of Zamorak is not listed as a travel method to Chaos Fanatic on either the main page or Strategies page. The Mage of Zamorak teleports players to the Abyss (a non-PvP runecrafting area at low Wilderness), not to level 42 Wilderness near Chaos Fanatic. The wiki lists three travel methods: Ghorrock Teleport, Burning Amulet, and POH Obelisk (hard Wilderness Diary). A 60 Magic requirement is also not cited anywhere on the wiki for any route.

- **Data says:** Mage of Zamorak teleport (requires 60 Magic) is an alternative travel route to Chaos Fanatic
- **Wiki says (raw):** The fastest way to get there is by casting Ghorrock Teleport or using its magic tablet equivalent. Alternatively, a burning amulet can teleport players to the entrance of the Lava Maze; run west from there. Alternatively, players with the hard Wilderness Diary completed and a POH obelisk can select the level 44 Wilderness teleport.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chaos_Fanatic
- **Suggested fix:** Remove the Mage of Zamorak reference entirely. The valid teleport alternatives are Ghorrock Teleport (96 Magic) and the POH obelisk (hard Wilderness Diary). The burning amulet guidance (C2) is already separately correct.
- **Skeptic receipt:** Mage of Zamorak page: 'teleports players into the Abyss (resulting in a drain of all Prayer points).' Chaos Fanatic page travel section: 'The fastest way to get there is by casting Ghorrock Teleport or using its magic tablet equivalent. Alternatively, a burning amulet can teleport players to the entrance of the Lava Maze; run west from there. Alternatively, players with the hard Wilderness Diary completed and a POH obelisk can select the level 44 Wilderness teleport.' (no Mage of Zamorak, no 60 Magic gate mentioned)
- **Skeptic reasoning:** Data step (drop_rates.json:5179) tells players to 'web-walk via the Mage of Zamorak teleport (Lvl-60 Magic)' to reach the Chaos Fanatic camp. The Mage of Zamorak teleport destination is the Abyss (a runecrafting area), not the Lava Maze or level 42 Wilderness near Chaos Fanatic, so it is not a valid route there; and no Lvl-60 Magic requirement is documented for that teleport. The wiki's own travel section lists Ghorrock Teleport, burning amulet, and the POH obelisk (hard Wilderness Diary) only. Not multi-source, not a variant, not account-type dependent, not stale (no Chaos Fanatic wiki edits since 2025-12-01). Genuine error.

### [blocker] C7: The wiki describes Chaos Fanatic's special attack as an explosive green projectile players must run away from. There is no mention of a self-damage tantrum, a stun animation, or any window where the boss is briefly stunned for special attack retaliation. This mechanic appears to be fabricated.

- **Data says:** Chaos Fanatic has a self-damage tantrum animation that briefly stuns it, allowing for special attack retaliation
- **Wiki says (raw):** a green magical attack that explodes and can deal high damage, similar to the Crazy Archaeologist's explosive book attack. Players must run away from them to avoid damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chaos_Fanatic
- **Suggested fix:** Remove the claim about a tantrum/stun animation entirely. The only special mechanic documented on the wiki is the explosive green AoE attack (which players avoid by moving) and the item-disarming mechanic similar to Chaos Elemental.
- **Skeptic receipt:** Chaos Fanatic page: 'a green magical attack that explodes and can deal high damage, similar to the Crazy Archaeologist's explosive book attack. Players must run away from them to avoid damage.' and 'able to disarm players, much like how the Chaos Elemental does.' No mention of tantrum/stun/retaliation on either page.
- **Skeptic reasoning:** Data step (drop_rates.json:5189) asserts 'The boss has a self-damage tantrum animation that briefly stuns it; capitalise with a special attack.' Both the main Chaos Fanatic page and the Strategies page document only two special mechanics: the green explosive AoE (run away to avoid) and item disarming (like Chaos Elemental). Neither page describes any self-damage tantrum, stun animation, or special-attack-retaliation window. No recent wiki edits (0 changes since 2025-12-01) could explain this as drift. Fabricated mechanic.

### [blocker] C8: The wiki records an April 2, 2025 patch where the AoE attack was changed to only affect the last player who engaged in combat. The claim that clustering causes increased splash damage contradicts this: under the current mechanic, only one player is hit by the AoE regardless of clustering. No wiki source supports the idea that proximity to the spawn point amplifies splash damage.

- **Data says:** Clustering with the Chaos Fanatic spawn point causes increased multi-hit projectile splash damage
- **Wiki says (raw):** The Chaos Fanatic's area-of-effect attack will now only affect the last player who engaged in combat.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chaos_Fanatic
- **Suggested fix:** Remove or rewrite this guidance. The AoE splash now only hits the last player who engaged, not nearby grouped players. Warn players to move away from the green AoE projectile landing spot, not to avoid clustering near the spawn.
- **Skeptic receipt:** Chaos Fanatic page patch note (2 April 2025): 'The Chaos Fanatic's area-of-effect attack will now only affect the last player who engaged in combat.'
- **Skeptic reasoning:** Data step (drop_rates.json:5189) claims the 'multi-hit projectile splash deals 4-12 if you cluster with the spawn point', implying clustering increases damage. The 2 April 2025 patch changed the AoE to affect only the last player who engaged in combat, so clustering near the spawn point cannot amplify or add splash hits. The premise is contradicted by the current documented mechanic. Not a variant or account-type issue; no wiki edits since 2025-12-01 to suggest staleness in our favor.

### [medium] C6: The wiki gives no specific tile-distance mitigation for the AoE attack nor any damage range (4-12) associated with partial avoidance. The wiki's only advice is to run away from the attack entirely. The claim about standing 1-2 tiles away reducing damage to a specific range is not supported by the wiki.

- **Data says:** Standing 1-2 tiles away from Chaos Fanatic reduces multi-hit projectile splash damage to 4-12 damage
- **Wiki says (raw):** a green magical attack that explodes and can deal high damage, similar to the Crazy Archaeologist's explosive book attack. Players must run away from them to avoid damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chaos_Fanatic
- **Suggested fix:** Replace with wiki-supported guidance: move away from the green AoE landing spot to avoid the explosion entirely. Do not reference a specific tile distance or partial damage range that the wiki does not document.
- **Skeptic receipt:** Chaos Fanatic page: 'a green magical attack that explodes and can deal high damage ... Players must run away from them to avoid damage.' Strategies page provides no specific damage numbers, tile-distance calculations, or mitigation percentages; advice relies on Protect from Magic and moving away.
- **Skeptic reasoning:** Data step (drop_rates.json:5189) gives a specific mitigation claim: 'stand 1-2 tiles away (range is fine) - the multi-hit projectile splash deals 4-12'. Neither wiki page documents any tile-distance-based damage mitigation nor any 4-12 damage range for the AoE; the only documented advice is to run away from the explosion entirely (and to use Protect from Magic on Strategies). The specific tile distance and damage numbers are unsupported. No recent wiki edits to explain as drift.

### [medium] C11: The wiki explicitly lists what the Pool of Refreshment restores (HP, stats, prayer, run energy) and does not include amulet recharging. The pool does not recharge charged jewelry. This is an unsupported claim.

- **Data says:** Ferox Enclave fountain can be used to recharge amulets
- **Wiki says (raw):** the Pool of Refreshment which restores the player's hitpoints, stats, prayer points, and run energy (but not special attack energy)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ferox_Enclave
- **Suggested fix:** Remove the amulet recharge claim. The pool restores HP, stats, prayer points, and run energy only.
- **Skeptic receipt:** Ferox Enclave page: 'the Pool of Refreshment which restores the player's hitpoints, stats, prayer points, and run energy (but not special attack energy)' -- no amulet/jewelry recharging facility listed.
- **Skeptic reasoning:** Data conditional-alternative (drop_rates.json:5172) instructs players to 'recharge amulets at the fountain' at Ferox Enclave. The Ferox Enclave Pool of Refreshment restores hitpoints, stats, prayer points, and run energy only (explicitly not special attack energy), and the wiki lists no amulet/charged-jewelry recharging facility at Ferox Enclave. Burning amulets and glory amulets are not recharged at the pool. Unsupported claim; not account-type or variant dependent.

## Venenatis

### [medium] C1: Claim omits ranged as a third attack style. Venenatis uses melee, magic, AND ranged -- not just magic and melee. The strategies page describes an explicit cycle of '8 range attacks, followed by 8 magic attacks.' Describing only two of three attack styles is misleading.

- **Data says:** Venenatis attacks with magic and melee
- **Wiki says (raw):** Venenatis attacks with melee towards any player that's adjacent to her, and either magic or ranged for any player out of her melee range.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Venenatis
- **Suggested fix:** Update description to state Venenatis attacks with melee, magic, AND ranged -- e.g. 'Venenatis attacks with melee (adjacent), ranged, and magic (at range).'
- **Skeptic receipt:** Venenatis employs three combat styles: Melee, Ranged ('Launches green spit at players'), and Magic. 'She will begin the fight using 8 range attacks, followed by 8 magic attacks, all while moving position every 4 attacks.'
- **Skeptic reasoning:** Data (drop_rates.json line 5302) says 'Venenatis attacks with magic and melee', omitting ranged entirely. The strategies page confirms three styles and that ranged is in fact the OPENING phase. Omitting ranged is not a defensible simplification because the boss spends the first 8 of every 16-attack cycle on ranged - the exact style the data's recommended Protect from Magic does not block. Not a multi-source/variant/account-type matter; it is a flat factual omission about the boss's combat.

### [blocker] C2: Venenatis cycles between ranged and magic attacks. Using only Protect from Magic leaves all ranged attacks completely unblocked. Players must switch between Protect from Missiles and Protect from Magic in sync with her attack cycle. Advising a single static prayer is an inverted combat meta that will cause significant unmitigated damage.

- **Data says:** Venenatis uses Protect from Magic as a defensive prayer against her attacks
- **Wiki says (raw):** She follows the pattern of '8 range attacks, followed by 8 magic attacks, all while moving position every 4 attacks.' Protection prayers will fully block the attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Venenatis/Strategies
- **Suggested fix:** Replace with prayer-switching guidance: alternate between Protect from Missiles (ranged phase) and Protect from Magic (magic phase) following her 8-attack cycle.
- **Skeptic receipt:** 'She will begin the fight using 8 range attacks, followed by 8 magic attacks, all while moving position every 4 attacks.' (The strategies page contains NO sentence reading 'Protection prayers will fully block the attacks' - that part of the candidate receipt is unsupported.)
- **Skeptic reasoning:** Data (line 5302) advises 'Kill Venenatis with melee using Protect from Magic' as a single static prayer. The wiki-confirmed attack cycle is 8 ranged attacks then 8 magic attacks. A static Protect from Magic leaves all 8 ranged spits per cycle unmitigated, so the recommended prayer is half-wrong/inverted for the opening phase. NOTE: the candidate's cited receipt 'Protection prayers will fully block the attacks' does NOT appear on the strategies page (fabricated/unsupported quote), and the page gives no explicit prayer recommendation. The finding nonetheless stands on the independently-confirmed attack cycle rather than the bad quote. Because the page offers no prayer recommendation, the precise fix wording is a judgment call (a non-melee player should swap Protect from Missiles<->Protect from Magic per phase, or stay in melee distance to force melee-only), so this is high, not a hard blocker.

### [high] C4: Spiderlings do NOT heal Venenatis. The wiki states they increase her maximum hit and allow her to hit through prayers, and drain the player's prayer -- not a healing mechanic. The healing claim is fabricated and misrepresents the actual danger (prayer penetration and damage boost), which could cause players to mis-prioritise their response.

- **Data says:** Venenatis spiderlings hit hard and heal Venenatis if left alive
- **Wiki says (raw):** While fairly weak, they will drain prayer on every hit regardless if prayed against or hits. They will also increase her damage and allow her to hit through prayers if left alive.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Venenatis/Strategies
- **Suggested fix:** Replace 'heal Venenatis if left alive' with 'increase her damage and allow her to hit through protection prayers if left alive.'
- **Skeptic receipt:** 'While fairly weak, they will drain prayer on every hit regardless if prayed against or hits. They will also increase her damage and allow her to hit through prayers if left alive. Thrall damage is reduced while spiderlings are alive.'
- **Skeptic reasoning:** Data (line 5302) says spiderlings 'hit hard and heal her if left alive.' No healing mechanic exists. The wiki states the real effects: drain the player's prayer, increase HER damage, and let her hit through protection prayers. The 'heal Venenatis' clause is fabricated and misdescribes the danger. Not account-type or variant dependent.

### [high] C8: The wiki lists six travel methods for Venenatis and does not include 'Burning amulet to Bandit Camp' among them. This route is associated with other Wilderness bosses (historically Chaos Elemental / Callisto area), not the Silk Chasm where Venenatis now resides following the January 2023 rework. Directing players to the wrong teleport destination wastes effort and exposes them to unnecessary wilderness risk.

- **Data says:** The travel method to reach Venenatis is Burning amulet to Bandit Camp then run east
- **Wiki says (raw):** Multiple routes exist: Wilderness crabs teleport (fastest option); Ancient spellbook's Annakarl Teleport at 90 Magic; Waka canoe with 57 Woodcutting; Games necklace to Corporeal Beast's lair, then run northeast; Hard Wilderness Diary completion enables obelisk teleportation to levels 19, 35, or 55; Wilderness Sword 4 teleport to Fountain of Rune.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Venenatis/Strategies
- **Suggested fix:** Replace with a wiki-supported route, preferably 'Wilderness crabs teleport' (fastest) or 'Waka canoe with 57 Woodcutting' as accessible alternatives.
- **Skeptic receipt:** Wiki travel methods: 'Wilderness crabs teleport; Annakarl Teleport (Magic 90+); Waka canoe (Woodcutting 57+); Games necklace to Corporeal Beast; Wilderness obelisks (Hard Diary); Wilderness Sword 4 to Fountain of Rune.' Burning amulet to Bandit Camp is not among them.
- **Skeptic reasoning:** Data top-level travelTip (line 5271) and guidance step 1 (lines 5283/5289) both route via 'Burning amulet -> Bandit Camp, then run east to the Silk Chasm cave.' The wiki lists the actual routes and does not include a Burning amulet / Bandit Camp option. Burning amulet teleports to the Bandit Camp area, which is the Chaos Elemental/Scorpia/Vet'ion side of the Wilderness, not the Silk Chasm where post-2023-rework Venenatis resides - the route does not reach her. This is a genuine travel-data error, not staleness (the rework predates the data and the wiki routes are the post-rework set). Account-type independent.

## Vet'ion

### [blocker] C2: Saradomin sword is a slash weapon, not a crush weapon. Vet'ion has +200 slash defense, making it near-useless. The wiki confirms only crush attacks are effective (crush defense -10). Saradomin sword should be removed from this list entirely.

- **Data says:** Crush weapons suitable for Vet'ion: Inquisitor's mace, Soulreaper axe, or Saradomin sword
- **Wiki says (raw):** Vet'ion is highly resistant to any combat style other than crush. [Defensive stats: Stab +201, Slash +200, Crush -10]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vet%27ion
- **Suggested fix:** Remove 'Saradomin sword' from the crush weapon list. Valid alternatives from the wiki include Abyssal bludgeon, Elder maul, Zamorakian hasta (crush style), Sarachnis cudgel, or Dual macuahuitl.
- **Skeptic receipt:** Defensive Stats (from infobox): Stab defence: +201, Slash defence: +200, Crush defence: -10 ... [strategies] Use the attack style that inflicts 'Crush' damage. Suggested weapons: Ursine chainmace / Viggora's chainmace (most effective), Soulreaper axe, Inquisitor's mace. Saradomin sword is not mentioned in the strategies.
- **Skeptic reasoning:** The guidance lists Saradomin sword in a list explicitly framed as crush weapons ('Travel ... with a crush weapon (Inquisitor's mace, Soulreaper axe, or Saradomin sword)'). The Saradomin sword's attack styles are Stab/Slash/Lunge -- it has no Crush option, so it cannot benefit from Vet'ion's -10 crush defence and instead hits against +200 slash defence. Inquisitor's mace and Soulreaper axe are legitimately crush, so they stay; only Saradomin sword is miscategorized. Not a multi-source, variant, account-type, or staleness case -- it is a flat factual error in the guidance text. Wiki strategies page also omits Saradomin sword from its recommended weapons. Fix: remove 'Saradomin sword' from this crush-weapon list (Inquisitor's mace and Soulreaper axe remain correct; Abyssal bludgeon / Ursine or Viggora's chainmace / Elder maul are valid crush alternatives).

### [blocker] C8: Vet'ion has no ranged attack. The wiki lists only magic (lightning bolts) and a non-damaging melee animation as his attack types. The guidance describes a prayer swap for a mechanic that does not exist, which would mislead players into dropping their active prayer unnecessarily.

- **Data says:** Swap to Protect from Missiles when Vet'ion switches to a ranged attack
- **Wiki says (raw):** Vet'ion primarily attacks with targeted magic, launching lightning in a 3x3 radius around each player upon attacking. While he appears to swing his sword during these animations, they do not deal any melee damage. [No ranged attack is listed among Vet'ion's attack styles.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vet%27ion
- **Suggested fix:** Remove the instruction to swap to Protect from Missiles. Vet'ion does not use a ranged attack at any point. The relevant prayer swap is Protect from Melee when Skeletal Hellhounds spawn.
- **Skeptic receipt:** Attack Styles: Vet'ion employs 'Slash, Magic' attacks. He does not have a ranged attack option. [strategies] Vet'ion uses magic and melee only--no ranged attacks ... 'Magic attack: Five bolts of lightning...' and 'Shield Bash' (melee).
- **Skeptic reasoning:** The guidance instructs 'swap to Protect from Missiles when he switches to a ranged attack.' Both the main wiki page and the strategies page confirm Vet'ion's attack styles are Magic and Melee (Slash) only -- he has no ranged attack. His +270 ranged defence is a defensive stat, not an attack he uses, so there is no mechanic to swap for. This is not account-type or progress dependent and is not a variant/staleness issue; the instruction tells players to drop their correct overhead prayer (Protect from Magic, his main attack) for a mechanic that never occurs. Fix: remove the Protect-from-Missiles swap; the real swap is Protect from Melee when the Skeletal Hellhounds spawn.

### [high] C9: Hellhounds spawn at 50% HP per form (once in the normal form, once in the enraged form), not at three separate thresholds of 75%, 50%, and 25%. The 75% and 25% thresholds are fabricated. This gives players incorrect expectations about when to prepare for the adds.

- **Data says:** Vet'ion spawns Skeletal Hellhounds at 75%, 50%, and 25% HP
- **Wiki says (raw):** When he reaches half health in his first form for the first time, he summons two level 194 skeleton hellhounds that attack with powerful melee, and must be killed as they grant him complete damage immunity while alive. [In the enraged/second form, two level 231 greater skeleton hellhounds spawn at 50% of that form's HP.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vet%27ion
- **Suggested fix:** Correct to: Skeletal Hellhounds spawn once at 50% HP in the first form, and Greater Skeletal Hellhounds spawn at 50% HP in the enraged (second) form.
- **Skeptic receipt:** When he reaches half health in his first form for the first time, he summons two level 194 skeleton hellhounds that attack with powerful melee, and must be killed as they grant him complete damage immunity while alive. [strategies] When Vet'ion reaches 50% of his health in either form he will summon Skeletal Hellhounds ... during his first and Greater Skeletal Hellhounds in his second phase.
- **Skeptic reasoning:** The guidance states hellhounds spawn 'at 75%, 50%, and 25% HP.' The wiki confirms the mechanic is once at 50% per form: Skeletal Hellhounds at 50% in the first form and Greater Skeletal Hellhounds at 50% in the enraged/second form. The 75% and 25% thresholds are not in the wiki for either form -- they are fabricated, and they misdescribe a two-spawn (one-per-form-at-half) mechanic as a three-threshold single-form mechanic. Not a variant/multi-source/account-type case. Receipt holds. Fix: 'Skeletal Hellhounds spawn at 50% HP in the first form; Greater Skeletal Hellhounds spawn at 50% HP in the enraged second form -- kill them immediately as they grant Vet'ion damage immunity while alive.'

## Callisto

### [blocker] C3: Callisto uses three attack styles -- melee, ranged, and magic -- not melee only. Players out of melee range are attacked with ranged, and a slow magic projectile is also used. A player following this guidance would not prepare for ranged or magic attacks.

- **Data says:** Callisto only attacks with melee
- **Wiki says (raw):** Callisto primarily attacks with melee... will use ranged against players if he cannot reach them... Callisto will occasionally send out a slow magic attack towards the player.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Correct guidance step to state Callisto uses melee (primary), ranged, and magic attacks. Highlight the need to use Protect from Magic against the magic knockback attack.
- **Skeptic receipt:** wiki_lookup/WebFetch (Callisto): 'Callisto employs three distinct attack styles: melee, ranged, and magic.' 'Used against players if he cannot reach them.' 'Callisto will occasionally send out a slow magic attack towards the player.'
- **Skeptic reasoning:** Data guidance step 3 states verbatim 'Callisto only attacks with melee.' The wiki documents three attack styles. A player following the data would not anticipate the ranged or the magic knockback attack. This is a flat factual error in the data text.

### [blocker] C2: Protect from Melee is specifically the least effective prayer against Callisto, only reducing melee damage by ~50%. The wiki explicitly states Protection from Ranged and Magic fully negate those attack types, making Protect from Magic the critical prayer to use against the knockback attack. Framing Protect from Melee as 'effective' is misleading and potentially dangerous.

- **Data says:** Protect from Melee is effective against Callisto
- **Wiki says (raw):** Protection prayers will negate all ranged and magic damage of his attacks, but Protect from Melee will only reduce the damage by ~50%.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Revise to clarify that Protect from Melee only reduces damage by ~50%, and that Protect from Magic should be used to negate the magic knockback attack fully.
- **Skeptic receipt:** WebFetch (Callisto): 'Protection prayers will negate all ranged and magic damage of his attacks, but Protect from Melee will only reduce the damage by ~50%.'
- **Skeptic reasoning:** Data step 3 recommends 'Kill Callisto with melee using Protect from Melee.' The wiki states Protect from Melee only mitigates ~50% of his melee damage (the least effective protection prayer against him), whereas Protect from Magic/Ranged fully negate. Relying on Protect from Melee as the kill-method prayer is the wrong, dangerous meta; the critical prayer is Protect from Magic against the knockback attack.

### [blocker] C13: Piety is a melee-boosting prayer. The wiki explicitly states melee combat is not advised because Callisto deals incredibly high melee damage even through prayer. The recommended meta is ranged. Recommending Piety actively encourages the wrong (and dangerous) combat style.

- **Data says:** Piety prayer should be active during Callisto combat
- **Wiki says (raw):** ranged is the ideal method for doing damage... Callisto can deal incredibly high melee damage through prayer, so melee combat is not advised.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto/Strategies
- **Suggested fix:** Remove Piety recommendation. Replace with the ranged combat meta -- recommending Rigour or Eagle Eye prayer instead, consistent with the wiki's ranged-is-ideal guidance.
- **Skeptic receipt:** WebFetch (Callisto/Strategies): 'Callisto can deal incredibly high melee damage through prayer, so melee combat is not advised.' 'ranged is the ideal method for doing damage' because he 'has very low defence against ranged.' 'The guide recommends Rigour.'
- **Skeptic reasoning:** Data step 2 says 'Turn on Piety' (a melee accuracy/strength prayer). The Strategies page explicitly states melee combat is not advised and ranged is the ideal method. Recommending Piety actively steers players into the discouraged combat style. The correct prayer for the ranged meta is Rigour.

### [blocker] C9: The wiki explicitly discourages melee combat at Callisto. Bandos and Torva are both melee armour sets. Recommending them steers players into a playstyle the wiki calls inadvisable. The wiki recommends ranged setups (Masori, Webweaver bow, etc.).

- **Data says:** Bandos or Torva are appropriate melee setups for Callisto
- **Wiki says (raw):** ranged is the ideal method for doing damage... Callisto can deal incredibly high melee damage through prayer, so melee combat is not advised... A charged Webweaver bow has a powerful bonus against Wilderness monsters... Masori body (f) [as top-tier ranged armour].
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto/Strategies
- **Suggested fix:** Replace Bandos/Torva with ranged gear recommendations consistent with the wiki: Masori or equivalent ranged armour, and a Webweaver bow or similar crossbow.
- **Skeptic receipt:** WebFetch (Callisto/Strategies): 'melee combat is not advised.' 'Masori body (f) is listed as the most effective ranged body armor option.'
- **Skeptic reasoning:** Data step 1 prescribes a 'melee setup (Bandos / Torva...).' Both are melee armour. The wiki explicitly advises against melee and recommends ranged armour (Masori body (f)). The data steers the player into the wrong gear class for the content.

### [high] C10: Voidwaker is a melee weapon. The wiki does not mention Voidwaker at Callisto and explicitly advises against melee combat. All recommended weapons in the strategy guide are ranged or magic.

- **Data says:** Voidwaker is a recommended weapon for Callisto
- **Wiki says (raw):** A charged Webweaver bow has a powerful bonus against Wilderness monsters... [strategy guide recommends ranged weapons: Zaryte crossbow, Armadyl crossbow, Dragon crossbow; magic weapons: Accursed sceptre].
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto/Strategies
- **Suggested fix:** Remove Voidwaker. Recommend a ranged weapon (Webweaver bow, Zaryte crossbow, or Armadyl crossbow) per the wiki's strategy guidance.
- **Skeptic receipt:** WebFetch (Callisto/Strategies): 'Webweaver bow (charged) and Craw's bow offer the fastest kills... Zaryte crossbow and Armadyl crossbow are solid alternatives.' 'melee combat is not advised.'
- **Skeptic reasoning:** Data step 1 recommends bringing a Voidwaker (a melee weapon). The Strategies guide's recommended weapons are all ranged (Webweaver bow, Craw's bow, Zaryte crossbow, Armadyl crossbow); melee is advised against. Note: Voidwaker hilt is a legitimate DROP from Callisto, but the data recommends Voidwaker as a weapon to bring, which is the melee-meta error, not a drop-table issue.

### [high] C12: Super Combat potions boost melee stats (Attack, Strength, Defence). The wiki does not mention them at Callisto; the recommended potion is a Divine ranging potion. Recommending Super Combats reinforces the wrong (melee) combat meta.

- **Data says:** Super Combat potions should be used before engaging Callisto
- **Wiki says (raw):** Divine ranging potion(4) [listed in inventory recommendations for both ranged and magic setups; Super combat potions are not mentioned].
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto/Strategies
- **Suggested fix:** Replace Super Combat potion with Divine ranging potion (or Divine magic potion for magic setups) per the wiki's inventory recommendations.
- **Skeptic receipt:** WebFetch (Callisto/Strategies): 'Divine ranging potion is specifically mentioned as recommended inventory equipment.' 'ranged is the ideal method for doing damage.'
- **Skeptic reasoning:** Data step 2 says 'pre-pot Super Combat' (boosts melee stats). The Strategies inventory recommendation is Divine ranging potion, consistent with the ranged meta. Recommending Super Combat reinforces the discouraged melee approach.

### [high] C4: The wiki describes no 'bear-stomp shockwave.' The knockback effect belongs to the magic projectile attack, not a stomp. At health thresholds Callisto deploys bear traps, which snare -- they do not cause knockback. This is a fabricated mechanic name and incorrect attribution of the knockback.

- **Data says:** Callisto's bear-stomp has a shockwave effect that causes knockback
- **Wiki says (raw):** At specific health intervals (66% and 33%), Callisto lifts his head, becomes unfrozen, and deploys bear traps that snare and damage players when stepped on... Callisto will occasionally send out a slow magic attack towards the player... this will result in the player being knocked back and temporarily stunned.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Remove bear-stomp shockwave description. Explain instead that the magic attack causes knockback/stun if Protect from Magic is not active, and that bear traps are deployed at 66% and 33% HP.
- **Skeptic receipt:** WebFetch (Callisto): 'If the player is not praying Protect from Magic when the attack reaches the player, this will result in the player being knocked back and temporarily stunned...' and 'At specific health intervals (66% and 33%), Callisto ... deploys bear traps that snare and damage players when stepped on.'
- **Skeptic reasoning:** Data step 3 describes a 'bear-stomp shockwave (knockback ...).' The wiki attributes knockback/stun to the slow MAGIC attack (negated by Protect from Magic), not to any stomp. At 66%/33% Callisto deploys bear traps that snare and damage when stepped on - they do not knock back. The 'bear-stomp shockwave' is a fabricated mechanic name and misattributes the knockback.

### [high] C5: The wiki contains no mention of any mechanic that unequips ranged shields. This appears to be a fabricated detail. The bear-stomp shockwave itself is not a described mechanic.

- **Data says:** The bear-stomp shockwave unequips ranged shields
- **Wiki says (raw):** At specific health intervals (66% and 33%), Callisto lifts his head, becomes unfrozen, and deploys bear traps that snare and damage players when stepped on. [No mention of shield unequipping anywhere on the page.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Remove the shield-unequipping claim entirely -- it has no basis in the wiki.
- **Skeptic receipt:** WebFetch (Callisto): full mechanics text describes only melee/ranged/magic attacks, the magic knockback, freezing, and 66%/33% bear traps - no mechanic that unequips any equipment is described.
- **Skeptic reasoning:** Data step 3 claims the shockwave 'unequips ranged shields.' No shield-unequipping mechanic exists anywhere on the Callisto or Strategies pages. The underlying 'bear-stomp shockwave' is itself fabricated, so the shield-unequip detail has no basis.

### [high] C6: The wiki contains no mention of combat stalling from any stomp or shockwave. This is a fabricated detail built on the fabricated 'bear-stomp shockwave' mechanic.

- **Data says:** The bear-stomp shockwave stalls combat
- **Wiki says (raw):** At specific health intervals (66% and 33%), Callisto lifts his head, becomes unfrozen, and deploys bear traps that snare and damage players when stepped on. [No mention of combat stalling anywhere on the page.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Remove the combat-stalling claim entirely.
- **Skeptic receipt:** WebFetch (Callisto): the only health-interval behaviour documented is 'lifts his head, becomes unfrozen, and deploys bear traps that snare and damage players when stepped on' - no combat stall.
- **Skeptic reasoning:** Data step 3 claims the shockwave 'stalls combat.' No combat-stalling mechanic is documented on either wiki page; it rests on the fabricated bear-stomp shockwave.

### [high] C7: No 'bear-stomp shockwave' mechanic exists per the wiki, and no tile-standing interaction is described. This claim is built on a fabricated mechanic.

- **Data says:** Standing on Callisto's tile causes the bear-stomp shockwave to hit at melee range
- **Wiki says (raw):** At specific health intervals (66% and 33%), Callisto lifts his head, becomes unfrozen, and deploys bear traps that snare and damage players when stepped on. [No tile-standing mechanic or stomp-range interaction described.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Remove this guidance step. Replace with accurate mechanic: bear traps are deployed at 66%/33% HP and snare players who step on them.
- **Skeptic receipt:** WebFetch (Callisto/Strategies): 'melee combat is not advised'; WebFetch (Callisto): no stomp/shockwave or tile-standing interaction is described - only bear traps at 66%/33%.
- **Skeptic reasoning:** Data step 3 instructs the player to 'Stand on his tile so the bear-stomp shockwave ... hits at melee range.' No such tile-standing/shockwave-range interaction exists in the wiki, and it presupposes both the fabricated shockwave and the discouraged melee positioning. Standing on the boss's tile at melee range is the dangerous melee approach the wiki advises against.

### [medium] C8: The wiki does not describe Callisto cubs healing the boss. No cub-healing mechanic is documented. This claim may be invented or confused with another boss mechanic.

- **Data says:** Callisto cubs heal Callisto if left alive
- **Wiki says (raw):** [No mention of Callisto cubs or any healing mechanic from cubs anywhere on the Callisto page or Strategies page.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto
- **Suggested fix:** Remove or verify from an in-game source. If cubs exist as NPCs in the den, their mechanic should be re-checked against in-game observation rather than an undocumented wiki claim.
- **Skeptic receipt:** wiki_lookup (Callisto drop table): 'Callisto cub | qty: 1 | rarity: 1/1500'. WebFetch (Callisto/Strategies): 'The guide does not mention Callisto cubs healing the boss or appearing during combat. Cubs are only listed as valuable rare drops.'
- **Skeptic reasoning:** Data step 3 claims Callisto cubs 'heal him if left alive.' Callisto cub is a 1/1500 pet drop (independent, isPet:true in our data), not a combat NPC that spawns or heals the boss. Neither wiki page documents any cub-healing mechanic. This is a fabricated mechanic.

### [medium] C15: Callisto's Den is deeper in the Wilderness (south of Demonic Ruins, ~level 55) than the Chaos Temple (level 11-12). The Den is north of the Chaos Temple, not south-east. A player following this direction would run the wrong way.

- **Data says:** Callisto's Den is located south-east of Chaos Temple
- **Wiki says (raw):** Callisto's Den is located just south of Demonic Ruins [in the Wilderness]. The Chaos Temple is at level 11-12 Wilderness. [Demonic Ruins are at approximately level 44-46 Wilderness, placing Callisto's Den well north of the Chaos Temple.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Callisto%27s_Den
- **Suggested fix:** Correct to state the Den is north of (or deep into the Wilderness from) the Chaos Temple. The burning amulet teleport to Chaos Temple is valid as a starting point, but the direction to travel is north.
- **Skeptic receipt:** WebFetch (Callisto's_Den): 'located in the Wilderness, just south of Demonic Ruins.' 'Wilderness level 40.' Data entry: Den worldX 3291, worldY 3849 (north of the lvl ~11-13 Chaos Temple).
- **Skeptic reasoning:** Data travelTip and step 1 say 'run south-east into Callisto's Den.' The wiki places the Den just south of Demonic Ruins at Wilderness level 40, far NORTH of the Chaos Temple (Wilderness ~level 11-13). Our own data confirms this: the Den is worldY 3849 while the Chaos Temple burning-amulet point is ~worldY 3636 - travel is north, not south-east. A player following 'south-east' from the Chaos Temple runs away from the Den. The burning-amulet-to-Chaos-Temple start is valid; only the direction is wrong.

## Demonic gorillas

### [blocker] C1: The trigger is inverted. The guidance says the gorilla switches prayer after three *successful* hits; the wiki is explicit that it switches after three *missed* hits (i.e., no damage taken). Following the current guidance would cause players to switch their own prayer at completely the wrong time, directly failing the kill.

- **Data says:** Demonic gorillas cycle through Protect from Melee / Magic / Range after every three successful hits of the same style
- **Wiki says (raw):** They will switch attack styles after three (3) missed hits against the player (i.e., no damage is taken), regardless if the player's protection prayer blocked them or not.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies
- **Suggested fix:** Change the description to: 'Demonic gorillas switch their attack style (and the protection prayer you need) after landing three missed hits in a row -- keep your protection prayer up until you have blocked three of their attacks.'
- **Skeptic receipt:** They will switch attack styles after three (3) missed hits against the player (i.e., no damage is taken), regardless if the player's protection prayer blocked them or not. ... The protection prayer lasts until the gorilla takes 70+ damage in the style(s) it is not protecting against, then it will change prayers to another style.
- **Skeptic reasoning:** The data (drop_rates.json line 5621) says gorillas 'cycle through Protect from Melee / Magic / Range after every three successful hits of the same style.' This is wrong on two counts. (1) The trigger is inverted: the wiki is explicit that the gorilla switches its OWN attack style after three MISSED hits, not successful ones. (2) More importantly, the player-facing prayer flip the guidance is trying to describe is governed by a completely different rule - the gorilla changes its protection prayer only after it TAKES 70+ damage in an unprotected style, which has nothing to do with a hit count at all. A player following 'swap your style each time after three successful hits' would mistime both their offensive prayer-style swaps and their reactive protection-prayer swaps. Survived refutation vectors: not multi-source/variant/account-type dependent - this is a flat combat-mechanic statement contradicted by the wiki for all accounts.

### [high] C3: Two errors: (1) the orb is green, not purple; (2) the wiki does not describe this attack as hitting through prayer -- it is a normal magic attack that can be blocked with Protect from Magic. Telling players it bypasses prayer could cause them to unnecessarily panic and waste supplies or make incorrect decisions during the fight.

- **Data says:** Demonic gorillas' Magic-special purple orb attack hits for up to 30 damage through prayer
- **Wiki says (raw):** Magic attack: The gorilla rears up and dangles its forelegs, then emits a green orb from its mouth. each one hitting up to 30 damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies
- **Suggested fix:** Change to: 'Their magic attack (a green orb) hits up to 30 damage -- keep Protect from Magic active to block it.'
- **Skeptic receipt:** The gorilla rears up and dangles its forelegs, then emits a green orb from its mouth ... each one hitting up to 30 damage. [Boulder special:] Players will take 33% of their health as damage if they don't move away from it.
- **Skeptic reasoning:** The data (line 5621) describes the 'Magic-special purple orb (hits for up to 30 through prayer).' Two errors confirmed against the wiki. (1) Color: the wiki says the gorilla 'emits a GREEN orb from its mouth' - the data calls it purple. (2) 'Through prayer': the wiki describes the magic attack as an ordinary magic attack (blockable by Protect from Magic via the standard protection-prayer immunity), and the ONLY prayer-bypassing special on the page is the boulder attack, which deals 33% of the player's health and is dodgeable (and bypassable with Verac's). Attributing prayer-bypassing 30 damage to the magic orb is unsupported by the wiki. Survived refutation vectors: not a variant or account-type nuance - the attack's visuals and prayer interaction are fixed game mechanics.

### [medium] C7: Ghrazi Rapier is absent from all equipment recommendations on the strategies page. The wiki favours Emberlight and Arclight for melee because demonic gorillas are classified as demons, giving those weapons a significant accuracy and damage bonus. Recommending the Ghrazi Rapier as the meta melee swap is misleading and will result in noticeably worse performance.

- **Data says:** Bow of faerdhinen or Toxic blowpipe with melee swap to Ghrazi rapier is the modern meta for Demonic gorillas
- **Wiki says (raw):** Twisted bow, Scorching bow, and Toxic blowpipe have very similar performance, but weapons with slower attack speed are easier to use. For melee, Emberlight and Arclight are preferred for demon-type damage bonuses.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies
- **Suggested fix:** Replace the melee swap recommendation: 'Use Emberlight or Arclight for the melee swap -- demonic gorillas are demons, so these weapons have a significant accuracy and damage bonus over the Ghrazi Rapier.'
- **Skeptic receipt:** Melee is a preferred style to use at demonics for its cost, ease of use, and early access to Arclight, one of the strongest weapons to fight Demonics with. ... Emberlight for increased damage output against demon-type monsters. ... Arclight for increased damage output against demon-type monsters.
- **Skeptic reasoning:** The data (line 5621) calls 'Bow of faerdhinen or Toxic blowpipe + Ghrazi rapier on the swap ... the modern meta.' The wiki's melee recommendations are demonbane weapons - Arclight ('one of the strongest weapons to fight Demonics with') and Emberlight ('for increased damage output against demon-type monsters') - because demonic gorillas are demons and take bonus damage from demonbane weapons. The Ghrazi rapier appears nowhere in the wiki's melee equipment guidance. I scrutinized this hardest under the staleness/account-type vectors: the demonbane bonus is a current, live mechanic, and a rapier is not account-type-specific here. HOWEVER, downgrading from the proposed severity: a Ghrazi rapier is still a functional melee swap (it does not fail the kill, it is merely suboptimal vs demonbane), so this is a recommendation-quality gap, not a blocker. The wiki receipt supports correcting the recommended weapon, hence low severity rather than discard.

## Lizardman shaman

### [blocker] C1: Inquisitor's mace is a crush weapon. The wiki explicitly states crush attacks are not recommended due to high defensive bonuses (+30 crush defence). The wiki recommends stab weapons (Ghrazi rapier, Osmumten's fang). The combat meta is inverted.

- **Data says:** Bring your best crush weapon (Inquisitor's mace, Saradomin sword, or Soulreaper axe)
- **Wiki says (raw):** Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies
- **Suggested fix:** Replace crush weapon recommendations with stab weapons. The wiki-recommended melee options are Ghrazi rapier and Osmumten's fang. Update guidanceSteps[0].description and guidanceSteps[2].description to recommend stab, not crush.
- **Skeptic receipt:** Strategies page: "Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles." ... "they have negative stab defence and no ranged defence bonuses." Main page defence: "Stab -20, Slash +40, Crush +30." Recommended melee stab weapons listed: "Ghrazi rapier," "Noxious halberd," "Osmumten's fang."
- **Skeptic reasoning:** The data step 1 and step 3 recommend a 'best crush weapon (Inquisitor's mace...)'. Inquisitor's mace is crush. The wiki Strategies page explicitly states crush is not recommended and the monster has +30 crush defence vs -20 stab defence, recommending stab weapons (Ghrazi rapier, Osmumten's fang) and ranged. The combat meta in the data is inverted. This is a genuine, authoritative-receipt-backed error, consistent with the active branch's broader inverted-meta correction work.

### [high] C2: Saradomin sword is a slash weapon (+82 slash bonus, +60 crush), not a crush weapon. The guidance misclassifies it. Furthermore, slash is also explicitly not recommended by the wiki for this monster.

- **Data says:** Bring your best crush weapon (Inquisitor's mace, Saradomin sword, or Soulreaper axe)
- **Wiki says (raw):** Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies
- **Suggested fix:** Remove Saradomin sword from the weapon list. It is a slash weapon and the wiki recommends stab for Lizardman shamans (Stab defence: -20 vs Crush defence: +30).
- **Skeptic receipt:** Strategies page: "Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles." Main page defence: "Stab -20, Slash +40, Crush +30."
- **Skeptic reasoning:** Saradomin sword is a slash weapon, misclassified in the data as crush, and slash is also explicitly not recommended for this monster (Slash +40 defence). The whole weapon list is wrong regardless of the sub-classification; this is a valid supporting correction to C1. Note the suggested replacement (stab: Ghrazi rapier, Osmumten's fang) matches the wiki receipt.

### [high] C3: Soulreaper axe is a slash weapon, not a crush weapon. The guidance misclassifies it. Additionally, slash is not recommended by the wiki for this monster.

- **Data says:** Bring your best crush weapon (Inquisitor's mace, Saradomin sword, or Soulreaper axe)
- **Wiki says (raw):** Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies
- **Suggested fix:** Remove Soulreaper axe from the weapon list. Replace with stab weapons as recommended by the wiki (e.g. Ghrazi rapier, Osmumten's fang).
- **Skeptic receipt:** Strategies page: "Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles." Main page defence: "Stab -20, Slash +40, Crush +30."
- **Skeptic reasoning:** Soulreaper axe is a slash weapon, misclassified in the data as crush, and slash is explicitly not recommended (Slash +40 defence). Same supporting correction to the inverted-meta finding. Wiki-recommended replacement is stab (Ghrazi rapier, Osmumten's fang) or ranged.

### [blocker] C8: Two factual errors: (1) The spawns are purple, not green. The wrong color means players look for the wrong visual cue. (2) The wiki states spawn explosion deals 8-10 damage (Strategies page: 'dealing 8-10 damage to players that are two spaces or less from it'), not 20+ as claimed. The 20+ damage figure is more than double the actual value.

- **Data says:** Step one tile off the green spawn pool as soon as it appears - the spawn explodes for 20+ damage if you stand on it.
- **Wiki says (raw):** Lizardman shamans can summon 3 small purple spawns that appear next to the player
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lizardman_shaman
- **Suggested fix:** Change 'green spawn pool' to 'purple spawns' and correct the damage figure to '8-10 damage' to match the wiki. Also note the wiki says damage occurs within two tiles of the spawn, so stepping one tile may still be within range -- the guidance to move at least two tiles away is more accurate.
- **Skeptic receipt:** Main page: "summon 3 small purple spawns that appear next to the player" ... explosion deals "anywhere from 5-10 depending on the player's proximity." Strategies page: "the spawn will explode, dealing 8-10 damage to players that are two spaces or less from it when it explodes."
- **Skeptic reasoning:** Data says 'green spawn pool' that 'explodes for 20+ damage.' The wiki main page says the spawns are purple and the Strategies page caps the explosion at 8-10 damage (main page says 5-10). Both the color and the damage figure in the data are wrong. The summary's load-bearing claims (purple not green; ~8-10 not 20+) are both backed by authoritative quotes. (Minor: the suggested 'move at least two tiles' is itself slightly off since damage hits 'two spaces or less' so 3+ tiles is the safe distance, but the core factual corrections stand.)

### [blocker] C10: Recommending crush weapons for optimal drop rates is directly contradicted by the wiki. Shamans have +30 crush defence and -20 stab defence. The wiki recommends stab weapons. Using crush weapons reduces hit frequency and thus kill rate, not improves it.

- **Data says:** Use your crush weapon for the Dragon warhammer / Lizardman fang rare drops.
- **Wiki says (raw):** Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lizardman_shaman/Strategies
- **Suggested fix:** Replace 'Use your crush weapon' with guidance to use a stab weapon. The wiki-recommended melee options are Ghrazi rapier and Osmumten's fang.
- **Skeptic receipt:** Strategies page: "Slash, crush, and magical attacks are not recommended as they have high defensive bonuses against those styles." Main page defence: "Stab -20, Slash +40, Crush +30."
- **Skeptic reasoning:** Step 3 says 'Use your crush weapon for the Dragon warhammer / Lizardman fang rare drops.' Crush is explicitly not recommended; the monster has +30 crush defence and -20 stab defence, so crush reduces accuracy and kill rate rather than improving drop acquisition. Same inverted-meta error as C1, in a separate guidance sentence. Authoritative receipt holds.

### [high] C11: Xeric's Glade teleports to the Hosidius farming patch area, not to Shayzien. Using it to reach a Shayzien bank would require a long run across regions. The wiki Strategies page does not mention Glade as a banking route. The Shayzien bank (north of Graveyard of Heroes) is not near the Glade destination.

- **Data says:** Return to bank when supplies run low. Xeric's talisman -> Glade is the fastest cycle back to a Shayzien bank.
- **Wiki says (raw):** Xeric's Glade, by the magic trees north-east of the Hosidius farming patch.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Xeric%27s_talisman
- **Suggested fix:** Verify and correct the banking teleport. Xeric's Lookout (southeast of Shayzien) would put the player closer to a Shayzien bank than Glade. The wiki strategies page does not document a recommended banking cycle, so this should be verified in-game before re-wiring.
- **Skeptic receipt:** Xeric's talisman page: Xeric's Lookout teleports "at its entrance south-east of Shayzien" (closest to Shayzien); Xeric's Glade teleports "by the magic trees north-east of the Hosidius farming patch."
- **Skeptic reasoning:** Step 5 claims 'Xeric's talisman -> Glade is the fastest cycle back to a Shayzien bank.' The wiki shows Glade is north-east of the Hosidius farming patch, far from Shayzien, while Xeric's Lookout is south-east of Shayzien and is the destination the data itself already uses for the outbound travel step. Glade is the wrong teleport for a Shayzien bank cycle; Lookout (or another Shayzien-proximate option) is correct.

## Skotizo

### [blocker] C10: The claim inverts the altar mechanic. Active altars reduce the player's damage output to Skotizo -- they do NOT amplify Skotizo's damage to the player. Following this guidance leads the player to misunderstand the fight: the penalty for ignoring altars is that Skotizo becomes unkillable (100% damage reduction with non-demonbane weapons), not that the player takes more damage.

- **Data says:** Leaving Awakened Altars alive amplifies Skotizo's damage dramatically
- **Wiki says (raw):** These altars reduce all damage inflicted on Skotizo while they are active. [...] For each activated altar, the player will deal less damage to him - 15% less with Arclight/Emberlight and 25% less with other weapons (up to a maximum of 60% and 100% damage reduction, respectively).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Awakened_Altar
- **Suggested fix:** Change to: 'Leaving Awakened Altars active reduces your damage dealt to Skotizo by 25% each (up to 100% with non-demonbane weapons, or 15% each up to 60% with Arclight/Emberlight). Disable them quickly to avoid being unable to damage him.'
- **Skeptic receipt:** The awakened altars reduce the player's damage to Skotizo, not the other way around. "If you're wielding arclight, each altar reduces damage by 15% up to a maximum of 60%. Otherwise it's 25%, up to a maximum of 100%."
- **Skeptic reasoning:** The data inverts the altar mechanic. Awakened Altars reduce the PLAYER's damage output to Skotizo while active; they do not amplify Skotizo's damage to the player. With non-demonbane weapons the reduction stacks to 100% (Skotizo becomes unkillable), with Arclight/Emberlight it caps at 60%. The danger of leaving altars active is being unable to damage him, not taking more damage. Survives refutation: not multi-source/variant/account-type; a stable mechanic, no staleness. Receipt directly contradicts the data.

### [blocker] C4: The wiki recommends switching TO Protect from Magic during altar runs (implying it meaningfully reduces magic damage), contradicting the claim that Skotizo hits through it on every attack. The primary prayer meta is Protect from Melee (not Magic) paired with high Magic Defence gear -- not because PfM is bypassed, but because melee is the bigger threat when in melee range.

- **Data says:** Skotizo hits through Protect from Magic protection on every attack
- **Wiki says (raw):** When travelling to disable the Awakened Altars, the player should temporarily switch to Protect from Magic, as Skotizo does not approach to use melee attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skotizo/Strategies
- **Suggested fix:** Remove the claim that Skotizo hits through Protect from Magic. Correct guidance: the recommended prayer is Protect from Melee while fighting in melee range; switch to Protect from Magic when running out to disable altars.
- **Skeptic receipt:** When travelling to disable the Awakened Altars, the player should temporarily switch to Protect from Magic, as Skotizo does not approach to use melee attacks.
- **Skeptic reasoning:** Data claims Skotizo 'hits through Protect from Magic on every attack.' The wiki recommends temporarily switching TO Protect from Magic during altar runs, which is only sensible because PfM does mitigate his magic. When fighting in melee range the player runs Protect from Melee (so magic lands unblocked) - that is the player choosing not to pray magic, NOT his magic bypassing PfM. The data misstates the mechanic. The correct meta is Protect from Melee + high Magic Defence in melee range, Protect from Magic on altar runs.

### [blocker] C12: Same inversion as C4, repeated in a later guidance step. The wiki explicitly recommends Protect from Magic as the correct prayer to use during altar runs, which directly contradicts the assertion that his magic hits through it. This claim would cause players to not bother with Protect from Magic at all, leaving them unprotected during altar runs.

- **Data says:** Skotizo's magic still hits through Protect from Magic protection
- **Wiki says (raw):** When travelling to disable the Awakened Altars, the player should temporarily switch to Protect from Magic, as Skotizo does not approach to use melee attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skotizo/Strategies
- **Suggested fix:** Remove 'hits through Protect from Magic' language. The correct guidance is: use Protect from Melee as the primary prayer while in melee range; switch to Protect from Magic when running to disable altars.
- **Skeptic receipt:** When travelling to disable the Awakened Altars, the player should temporarily switch to Protect from Magic, as Skotizo does not approach to use melee attacks.
- **Skeptic reasoning:** Same inversion as C4 repeated in a later guidance step ('his magic still hits through Protect from Magic'). Refuted by the same authoritative receipt: the wiki explicitly prescribes Protect from Magic on altar runs, establishing PfM does protect against his magic. No multi-source/variant/staleness defense applies.

### [high] C2: The wiki's recommended primary prayer is Protect from Melee (not Protect from Magic). The strategy is to block Skotizo's melee with PfMelee and mitigate the remaining magic damage via high Magic Defence gear. Recommending PfM as the primary prayer leaves the player exposed to Skotizo's melee attacks.

- **Data says:** Protect from Magic prayer should be worn to reduce Skotizo's damage
- **Wiki says (raw):** Wearing armour with high Magic Defence (e.g. black dragonhide armour) while using Protect from Melee so the only source of damage is from his Magic attacks is helpful
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skotizo/Strategies
- **Suggested fix:** Change to: 'Use Protect from Melee as your primary prayer and wear high Magic Defence gear (e.g. black dragonhide) to reduce his magic damage. Switch to Protect from Magic when running out to disable altars.'
- **Skeptic receipt:** Wearing armour with high Magic Defence (e.g. black dragonhide armour) while using Protect from Melee so the only source of damage is from his Magic attacks
- **Skeptic reasoning:** Data recommends Protect from Magic as the primary worn prayer. The wiki's recommended primary prayer is Protect from Melee, paired with high Magic Defence armour (black dragonhide) to absorb the only remaining damage source (magic). Running PfM as primary leaves the player exposed to his melee. Not account-type dependent; stable strategy.

### [medium] C8: The three pieces are combined through the inventory interface (not specifically 'at the Dark Altar'). The completed totem is then used on the altar to spawn Skotizo. The claim conflates two steps: assembly (done anywhere in inventory) and using the assembled totem on the altar.

- **Data says:** Dark totem is assembled from three pieces (Dark totem top / middle / base) dropped by Catacomb monsters and combined at the Dark Altar
- **Wiki says (raw):** The dark totem is created from three pieces: the dark totem base, dark totem middle, and dark totem top. [...] Upon assembly, players receive the message: 'You bring the totem pieces together with a click.' [...] the complete totem is then used on the altar in the centre of the Catacombs of Kourend to access the lower level
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dark_totem
- **Suggested fix:** Change to: 'Dark totem is assembled from three pieces (Dark totem base / middle / top) dropped by Catacomb monsters. Combine them in your inventory, then use the assembled Dark totem on the altar in the centre of the Catacombs to access Skotizo.'
- **Skeptic receipt:** The dark totem pieces are combined in the inventory ... "You bring the totem pieces together with a click..." The assembled totem is then used on "the altar in the centre of the Catacombs of Kourend" to access the lower level and fight Skotizo.
- **Skeptic reasoning:** Data says the totem is assembled 'at the Dark Altar.' The wiki confirms the three pieces are combined in the INVENTORY (the 'you bring the totem pieces together with a click' message), and only the completed totem is then used on the altar in the centre of the Catacombs of Kourend. The data conflates inventory assembly with using the totem on the altar. This is a genuine but minor phrasing inaccuracy - correctly logged at medium, not a blocker. Receipt holds.

## Hespori

### [medium] C1: Wrong direction: the Hespori cave is in the west wing of the Farming Guild, not a south room.

- **Data says:** Hespori patch is in the south room of the Farming Guild
- **Wiki says (raw):** The Hespori is a sporadic boss fought in the cave within the west wing of the Farming Guild
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hespori
- **Suggested fix:** Change 'south room' to 'west wing cave' in guidanceSteps[0].description.
- **Skeptic receipt:** The Hespori is a sporadic Farming boss fought in the cave within the west wing of the Farming Guild which requires 65 Farming [boostable] to access.
- **Skeptic reasoning:** Data says 'south room of the Farming Guild' in guidanceSteps[0] and repeats 'south room' in steps[1] (description + farming-cape travelTip). Wiki (verbatim, https://oldschool.runescape.wiki/w/Hespori, unchanged since before 2025-01-01 per wiki_updates) places the cave in the 'west wing of the Farming Guild'. This is a genuine directional error, not an account-type, variant, or staleness artifact. Single-source Farming boss, so multi-source/clog-id vectors do not apply. STANDS as a correctness fix (low/medium -- misdirects routing prose but the worldX/worldY coords are already on the patch).

### [medium] C11: Two errors: (1) there are four flower buds per phase, not three; (2) the binding roots are Hespori's own special attack ('Hespori entangles you in some vines!'), not an auto-target attack from the flowers.

- **Data says:** Hespori summons three flowers each phase that auto-target with binding roots
- **Wiki says (raw):** The Hespori is surrounded by four flower buds that keep it invulnerable while it attacks the player. These buds open three times during the fight: on initiation of the fight, when the Hespori has 66%, and when the Hespori has 33% health remaining. Each bud has 10 hitpoints but will die in one hit from any weapon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hespori
- **Suggested fix:** Correct to four buds per phase. Remove the claim that flowers auto-target with binding roots -- that is Hespori's own special attack, separate from the flower phase.
- **Skeptic receipt:** The Hespori is surrounded by four flower buds that keep it invulnerable while it attacks the player. These buds open three times during the fight: on initiation of the fight, when the Hespori has 66%, and when the Hespori has 33% health remaining. Each bud has 10 hitpoints but will die in one hit from any weapon. ... Special Attack: Hespori entangles you in some vines!
- **Skeptic reasoning:** Two genuine errors confirmed by the wiki. (1) There are FOUR flower buds, not 'three flowers each phase' -- the '3' in the wiki refers to the number of times the buds open, which our data conflated into a count of flowers. (2) The buds merely keep Hespori invulnerable; they do not 'auto-target you with binding roots'. The vines/entangle is Hespori's OWN special attack ('Hespori entangles you in some vines!'), separate from the bud phase. Both sub-claims of the finding hold with verbatim receipts.

### [blocker] C12: Fabricated mechanic: the wiki describes no flower colors (white/red/blue) and no mandatory kill order. All four buds die in one hit from any weapon with no sequencing requirement. A player following this guidance would waste time and potentially take unnecessary damage searching for a color-based kill order that does not exist.

- **Data says:** Flowers must be killed in order: white, then red, then blue, or they will stack damage
- **Wiki says (raw):** The Hespori is surrounded by four flower buds that keep it invulnerable while it attacks the player. Each bud has 10 hitpoints but will die in one hit from any weapon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hespori
- **Suggested fix:** Remove the color-coded kill order entirely. Replace with: all four flower buds die in one hit from any weapon and can be killed in any order.
- **Skeptic receipt:** The Hespori is surrounded by four flower buds that keep it invulnerable while it attacks the player. ... Each bud has 10 hitpoints but will die in one hit from any weapon. (The wiki page contains no mention of flower colours white/red/blue and no mandatory kill order; wiki_updates shows zero changes to the page since 2025-01-01, so this is not removed-but-formerly-true content.)
- **Skeptic reasoning:** Fabricated mechanic. Our data states flowers 'kill them in order (white, then red, then blue) or they will stack damage'. The wiki describes no flower colours and no sequencing requirement; every bud has 10 HP and dies in one hit from any weapon, killable in any order. This is not a variant, account-type, or staleness case -- the page is stable and never described such a mechanic. STANDS as a blocker: a player following the invented colour order would waste time hunting for non-existent coloured flowers. Replace with: all four buds die in one hit from any weapon, any order.

## Chambers of Xeric

### [blocker] C10: The prayers for the green-orb and crystal-chunk attacks are inverted. The wiki unambiguously assigns Protect from Missiles to the green sphere, not Protect from Magic. Following the current guidance would leave the player using the wrong prayer against Olm's green-orb attack.

- **Data says:** Use Protect from Magic against the green-orb attack and Protect from Missiles against crystal chunks
- **Wiki says (raw):** Green (sphere of accuracy and dexterity): Protect from Missiles
- **Wiki URL:** https://oldschool.runescape.wiki/w/Great_Olm
- **Suggested fix:** Replace 'Use Protect from Magic against the green-orb attack and Protect from Missiles against crystal chunks' with 'Use Protect from Missiles against the green-orb attack and Protect from Magic against magic attacks'.
- **Skeptic receipt:** Great Olm (oldschool.runescape.wiki/w/Great_Olm): 'Green Sphere (Accuracy and Dexterity): Use Protect from Missiles to completely prevent damage.' / 'Purple Sphere (Magical Power): Use Protect from Magic to completely prevent damage.' Crystal Burst: 'doesn't have a specific prayer to fully negate it.'
- **Skeptic reasoning:** The data step (line 6203) says 'Use Protect from Magic against the green-orb attack and Protect from Missiles against crystal chunks'. The Great Olm wiki assigns the green sphere to Protect from Missiles and the purple sphere to Protect from Magic -- so the green-orb prayer is inverted (should be Missiles, not Magic). Additionally the crystal burst attack has no protection prayer that negates it, so 'Protect from Missiles against crystal chunks' is also incorrect. No multi-source / variant / account-type vector applies; this is pure mechanics guidance. wiki_updates shows no recent drift. This is a real, mechanically-wrong guidance error a player would follow into ~50% HP hits.

### [high] C4: The Salve amulet (ei) does not work against Vasa Nistirio, who is not undead. The guidance recommends bringing it for this encounter, which would lead the player to occupy an amulet slot with a non-functional item for that fight.

- **Data says:** Salve (ei) for Vasa/Vespula
- **Wiki says (raw):** Salve Amulet does not work against Vasa.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vasa_Nistirio
- **Suggested fix:** Remove Vasa Nistirio from the Salve (ei) recommendation. It may still apply for undead rooms (Skeletal Mystics). Vespula is also an abyssal creature, not undead, so she should be removed from this recommendation as well.
- **Skeptic receipt:** Vasa Nistirio (oldschool.runescape.wiki/w/Vasa_Nistirio): 'Attribute: Xerician' and mechanics section: 'Salve Amulet does not work against Vasa.' Vespula (oldschool.runescape.wiki/w/Vespula): attributes 'Xerician' and 'Flying', not undead. Salve amulet (ei): 'Increases melee, ranged and magic damage & accuracy by 20% against the undead.'
- **Skeptic reasoning:** Data (lines 6171 & 6180) recommends 'Salve (ei) for Vasa/Vespula'. Salve amulet (ei) only boosts damage/accuracy against monsters with the undead attribute. Vasa Nistirio and Vespula are both confirmed Xerician (Vespula also Flying), NOT undead, so the Salve provides zero benefit against either -- and the Vasa page states this explicitly. Refutation vectors fail: no undead variant of these bosses exists, no account-type makes a non-undead boss undead. (CoX does contain undead Skeletal Mystics, so Salve is not useless in the raid generally -- but the data ties the recommendation specifically to Vasa/Vespula, which is the error.)

### [high] C7: Xeric's Heart teleports to Kourend Castle (next to the statue of King Rada I), not to a location near the Chambers of Xeric prep room. The claim that it lands the player 'roughly two tiles from the Chambers of Xeric prep room' is false -- Kourend Castle is far from Mount Quidamortem. This would send the player to the wrong destination entirely.

- **Data says:** Kourend Hard diary: use the Xeric's Heart teleport inside Lovakengj to land roughly two tiles from the Chambers of Xeric prep room
- **Wiki says (raw):** Xeric's Heart, next to the statue at Kourend Castle.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Xeric%27s_talisman
- **Suggested fix:** The Xeric's Heart teleport is not a useful shortcut to the Chambers of Xeric. The Kourend Hard diary alternative should either be removed or replaced with the correct Xeric's Honour teleport (which requires unlocking via an ancient tablet from a CoX reward) and accurate travel directions.
- **Skeptic receipt:** Xeric's talisman (oldschool.runescape.wiki/w/Xeric's_talisman): 'Xeric's Heart ... next to the statue at Kourend Castle'; 'Xeric's Inferno ... at the lovakite furnace in the centre of Lovakengj'; 'Xeric's Honour ... at the summit of Mount Quidamortem'.
- **Skeptic reasoning:** Data (line 6171, travelTip 6172) claims the Kourend Hard / Xeric's Heart teleport lands the player 'inside Lovakengj' and 'roughly two tiles from the Chambers of Xeric prep room'. Xeric's Heart teleports to Kourend Castle, geographically far from Mount Quidamortem / Chambers of Xeric -- it is neither in Lovakengj nor near the prep room. The data appears to conflate Xeric's Heart with Xeric's Inferno (the destination that actually is in Lovakengj), and neither lands near CoX. The only destination near CoX is Xeric's Honour (Mount Quidamortem summit), which the data does not use. wiki_updates shows no Xeric-page edits since 2024, so this is not staleness.

### [medium] C6: The description incorrectly places Xeric's Heart 'inside Lovakengj'. Xeric's Heart is at Kourend Castle, not in Lovakengj. Additionally, Xeric's Heart is not gated by the Kourend Hard diary -- the diary task requires using the teleport, not unlocks it. The talisman destination is always available once the talisman is charged.

- **Data says:** diaries: [KOUREND_HARD] -- requirement gates the Xeric's Heart teleport (described as being 'inside Lovakengj')
- **Wiki says (raw):** Xeric's Heart, next to the statue at Kourend Castle.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Xeric%27s_talisman
- **Suggested fix:** Remove the incorrect 'inside Lovakengj' location reference. The Kourend Hard diary does not unlock Xeric's Heart -- correct or remove this gate entirely.
- **Skeptic receipt:** Xeric's talisman (oldschool.runescape.wiki/w/Xeric's_talisman): per-destination unlock -- 'Xeric's Heart - All teleports are available by default'; explicit confirmation 'there is no Achievement Diary requirement to unlock or use any of the five teleport destinations'; 'Xeric's Heart ... next to the statue at Kourend Castle (previously required unlocking via Architectural Alliance, removed January 2024)'.
- **Skeptic reasoning:** Two-part error, both confirmed. (1) Location: the description places Xeric's Heart 'inside Lovakengj'; the wiki places it at Kourend Castle (Lovakengj is Xeric's Inferno). (2) Gate: the data structures KOUREND_HARD as gating this teleport alternative, but no Xeric's talisman destination has any Achievement Diary requirement -- Xeric's Heart is 'available by default' (its old Architectural Alliance unlock was removed Jan 2024, and that was never a diary gate). A fabricated requirement that does not exist in-game. wiki_updates confirms no post-2024 drift.

## Theatre of Blood

### [blocker] C4: Twisted bow fires arrows, not bolts. Diamond bolts (e) are crossbow ammunition and cannot be loaded into a Twisted bow. The guidance pairs the wrong ammo type with the wrong weapon -- a player following it would bring useless crossbow bolts for a bow slot and arrive at Verzik P3 with the wrong ammunition.

- **Data says:** Tbow + diamond bolts (e) for Verzik P3
- **Wiki says (raw):** It can fire any type of arrow, including dragon arrows. [Twisted bow wiki] AND: Enchanted Diamond tipped Adamantite Crossbow Bolts. [Diamond bolts (e) wiki]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Twisted_bow
- **Suggested fix:** Replace 'diamond bolts (e)' with an arrow type (e.g. 'dragon arrows') in both the travel step description and the Verzik step description. If the intent was a crossbow setup, replace 'Tbow' with the appropriate crossbow (e.g. Zaryte crossbow or Armadyl crossbow) and clarify the weapon.
- **Skeptic receipt:** Twisted bow wiki: 'It can fire any type of arrow, including dragon arrows.' / Diamond bolts (e) wiki: 'Enchanted Diamond tipped Adamantite Crossbow Bolts' - 'Diamond bolts (e) are crossbow ammunition, not arrows. They cannot be used with a bow.'
- **Skeptic reasoning:** The data text (travel guidance step, lines 6645 and 6660) reads 'Tbow + diamond bolts (e) for Verzik P3'. This pairs incompatible equipment: the Twisted bow is a bow that fires arrows, and diamond bolts (e) are crossbow ammunition that cannot be loaded into any bow. A player literally following this guidance would bring ammunition that does not fit the weapon. No refutation vector rescues it: this is guidance text, not a clog itemId, so multi-source/variant/account-type vectors do not apply; the incompatibility is a flat OSRS mechanic, not a meta choice. Both authoritative wiki pages confirm the mismatch. STANDS as a blocker - the intended ammo for a Tbow is an arrow (e.g. dragon arrows), or if a bolt setup was intended the weapon must be a crossbow.

### [blocker] C14: Same incompatible pairing as C4: Twisted bow uses arrows, diamond bolts (e) are crossbow ammo. Repeated in the Verzik step description. Both occurrences must be corrected.

- **Data says:** P3: Tbow with diamond bolts (e), pray Protect from Magic, dance the lightning zaps and never stand on a tornado
- **Wiki says (raw):** It can fire any type of arrow, including dragon arrows. [Twisted bow wiki] AND: Enchanted Diamond tipped Adamantite Crossbow Bolts. [Diamond bolts (e) wiki]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Diamond_bolts_%28e%29
- **Suggested fix:** Change 'Tbow with diamond bolts (e)' to 'Tbow with dragon arrows' (or the arrow type appropriate to the player's gear level). Alternatively, if a crossbow spec is intended for P3, replace 'Tbow' with the correct crossbow and keep the bolt reference.
- **Skeptic receipt:** Twisted bow wiki: 'It can fire any type of arrow, including dragon arrows.' / Diamond bolts (e) wiki: 'Enchanted Diamond tipped Adamantite Crossbow Bolts' confirming crossbow-only ammunition that cannot be used with a bow.
- **Skeptic reasoning:** Same incompatible pairing as C4, occurring a second time in the Verzik fight step (line 6683): 'P3: Tbow with diamond bolts (e)'. Twisted bow fires arrows; diamond bolts (e) are crossbow ammo - they cannot be loaded into a bow. This is a distinct occurrence in the data (different guidanceStep) and must be corrected alongside C4. No multi-source/variant/account-type angle applies to guidance prose describing a weapon-ammo combination. STANDS as a blocker.

### [medium] C10: The guidance implies supply chests are available after every boss ('each chest'), but the wiki states chests are only accessible after two specific bosses: Pestilent Bloat (boss 2) and Sotetseg (boss 4). There is no chest after Maiden, Nylocas, or Xarpus.

- **Data says:** Clear The Maiden, Pestilent Bloat, Nylocas, Sotetseg, and Xarpus. Refresh supplies at each chest and prep gear for the Verzik fight
- **Wiki says (raw):** After Bloat and Sotetseg, players have an opportunity to buy supplies such as food, saradomin brews, and prayer potions, using points earned within the raid at a supply chest.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Theatre_of_Blood
- **Suggested fix:** Change 'Refresh supplies at each chest' to 'Refresh supplies at the chests after Bloat and Sotetseg' to match the actual supply points in the raid.
- **Skeptic receipt:** Theatre of Blood wiki: 'After Bloat and Sotetseg, players have an opportunity to buy supplies such as food, saradomin brews, and prayer potions, using points earned within the raid at a supply chest.' Wiki confirms no chest after Maiden, Nylocas, or Xarpus.
- **Skeptic reasoning:** Data step (line 6675) says 'Refresh supplies at each chest', implying a supply chest after every boss. The Theatre of Blood wiki states supply chests are only available after Bloat (boss 2) and Sotetseg (boss 4) - there is no supply chest after Maiden, Nylocas, or Xarpus. The guidance overstates available restock points and could lead a player to expect resupply opportunities that do not exist. This is a verifiable game-mechanic statement, not account-type dependent (supply-chest placement is identical for all account types). STANDS as medium - reword to 'the chests after Bloat and Sotetseg'.

### [low] C13: The guidance calls Verzik P2's chaining projectile a 'red ball'; the wiki calls it a 'lightning ball'. Minor naming discrepancy -- does not affect combat execution but is inaccurate terminology.

- **Data says:** P2: dodge red ball, switch prayers to match the Nylocas colour
- **Wiki says (raw):** She will send a lightning ball that will chain between players in a random sequence, hitting them for a small amount of damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Verzik_Vitur
- **Suggested fix:** Change 'dodge red ball' to 'dodge lightning ball' to match wiki terminology.
- **Skeptic receipt:** Verzik Vitur wiki P2: 'a lightning ball that will chain between players in a random sequence' and 'a slow purple projectile which can be avoided but if it lands on the tile the player's on it can deal up to 78 damage.' 'There is no separate red ball attack mentioned.'
- **Skeptic reasoning:** Data step (line 6683) says 'P2: dodge red ball'. The Verzik Vitur wiki names no 'red ball' in Phase 2. The chaining projectile is the 'lightning ball', and the avoidable projectile players actually dodge is described as a 'slow purple projectile' (which spawns the Nylocas Athanatos). Neither is red. I considered whether 'red ball' is a legitimate community nickname (refutation vector: terminology drift), but the authoritative page establishes the in-game projectile color/name is not red - the dodge-able ball is purple and the chaining ball is lightning. This is a purely cosmetic naming inaccuracy that does not change combat execution, so it STANDS only at low severity. Recommend aligning to wiki terminology (e.g. 'dodge the purple projectile' for the avoidable one).

## Theatre of Blood (Hard Mode)

### [blocker] C19: Nylocas Athanatos spawn in P2, not P1. The guidance wrongly places this mechanic in Phase 1. Additionally, Phase 1's primary mechanic is the Dawnbringer special attack ('players in the raid must take it in turn to use the Dawnbringer's special attack in order to harm Verzik'), not a dagger. Players following this step would hunt for the Athanatos in the wrong phase entirely.

- **Data says:** In Verzik P1, use Protect from Magic prayer and dagger the Athanatos
- **Wiki says (raw):** spawn during the second phase of the Verzik Vitur encounter during the Theatre of Blood. If it is hit with a weapon capable of afflicting poison or venom (or when the serpentine helm is equipped), the Nylocas Athanatos will instead burst and deal damage to Verzik.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Nylocas_Athanatos
- **Suggested fix:** Move the Athanatos mechanic to the Verzik P2 step. Rewrite the P1 step to describe the actual P1 mechanic: use Protect from Magic and take turns using the Dawnbringer special attack while hiding behind pillars.
- **Skeptic receipt:** Nylocas_Athanatos page: "the Nylocas Athanatos appears during ... the second phase of the Verzik Vitur encounter". Verzik P1: "Players must take turns using the Dawnbringer's special attack to damage Verzik" and "It is mandatory to hide behind a pillar".
- **Skeptic reasoning:** The data's P1 step (line 6911) reads 'P1: pray Protect from Magic and dagger the Athanatos'. The Nylocas Athanatos spawns in Phase 2, not Phase 1 (confirmed by both the Verzik page and the dedicated Athanatos page). Phase 1's actual core mechanic is taking turns with the Dawnbringer special attack while hiding behind pillars, which the data omits entirely. This is not terminology nuance: the step names a creature that does not exist in P1 and describes the wrong action for the phase, so it survives the refutation pass. Severity blocker is appropriate (sends the player to act on a P2 creature during P1).

### [blocker] C6: The Twisted bow uses arrows, not bolts. Diamond bolts (e) are crossbow ammunition and are physically incompatible with the Twisted bow. This guidance would send players to equip the wrong ammunition type for the weapon.

- **Data says:** Tbow with diamond bolts (e) is a recommended weapon for Theatre of Blood Hard Mode
- **Wiki says (raw):** It can fire any type of arrow, including dragon arrows. Uses arrows as ammunition up to and including dragon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Twisted_bow
- **Suggested fix:** Change to 'Twisted bow with dragon arrows' or separate the two items: 'Twisted bow (with dragon arrows)' or alternatively reference a crossbow if diamond bolts (e) was the intended ammo.
- **Skeptic receipt:** Twisted_bow page: "It can fire any type of arrow, including dragon arrows." / "Uses arrows as ammunition up to and including dragon." No bolt compatibility anywhere in the article.
- **Skeptic reasoning:** The loadout/travel step (line 6873) recommends 'Tbow + diamond bolts (e)'. The Twisted bow uses arrows only; diamond bolts (e) are crossbow ammunition and are physically incompatible with the bow. This is not an item-variant or multi-source nuance - it is a weapon/ammo mismatch in recommended gear. Confirmed by the Twisted bow page. Suggested fix to dragon arrows is correct.

### [blocker] C23: Same ammunition incompatibility as C6. The Twisted bow cannot use diamond bolts (e); those are crossbow bolts. The P3 step would instruct players to equip incompatible ammunition.

- **Data says:** In Verzik P3, use Tbow with diamond bolts (e)
- **Wiki says (raw):** It can fire any type of arrow, including dragon arrows. Uses arrows as ammunition up to and including dragon.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Twisted_bow
- **Suggested fix:** Correct to 'Tbow with dragon arrows' in the P3 Verzik guidance step.
- **Skeptic receipt:** Twisted_bow page: "It can fire any type of arrow, including dragon arrows." / "Uses arrows as ammunition up to and including dragon."
- **Skeptic reasoning:** The P3 step (line 6911) reads 'P3: Tbow with diamond bolts (e)'. Same ammunition incompatibility as C6 - the Twisted bow cannot fire crossbow bolts. Authoritative receipt is the Twisted bow ammunition text. Correct fix is dragon arrows.

### [medium] C25: Lightning is a Phase 2 mechanic, not Phase 3. The wiki places the 'lightning ball' attack in P2. Phase 3 features tornadoes, webs, Nylocas spawns, and green projectiles - but no lightning attack.

- **Data says:** In Verzik P3, dodge lightning zaps
- **Wiki says (raw):** She will send a lightning ball that will chain between players in a random sequence, hitting them for a small amount of damage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Verzik_Vitur
- **Suggested fix:** Remove 'dodge lightning zaps' from the P3 guidance step. If a lightning avoidance cue is needed, move it to the P2 step.
- **Skeptic receipt:** Verzik P2: "Lightning chains between players, with potential for '48+ damage' if the ball reaches a fourth player". Verzik P3 specials: "Four special attacks in set order: Nylocas spawns, webs, safespots, green projectile" plus at 20%: "summon a purple magical tornado for each player" - no lightning listed in P3.
- **Skeptic reasoning:** The data's P3 step (line 6911) includes 'dance the lightning zaps'. The wiki places the lightning chain attack in Phase 2 ('Lightning chains between players'). Phase 3's four set-order specials are Nylocas spawns, webs, safespots, and the green projectile, plus tornadoes at 20% HP - there is no lightning attack in P3. The lightning cue is misplaced into the wrong phase. Medium severity is appropriate (a phantom avoidance cue, not a fatal misdirection).

### [medium] C20: The wiki describes P2 projectiles as 'urnbombs' (described as purple) and a 'slow purple projectile', not 'red balls'. There is no red projectile mechanic documented in any phase of Verzik.

- **Data says:** In Verzik P2, dodge red balls
- **Wiki says (raw):** She tosses out urnbombs at the players' positions which deals up to 44 damage. This purple projectile also transforms into Nylocas Athanatos which will heal her.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Verzik_Vitur
- **Suggested fix:** Change 'dodge red balls' to 'dodge urnbombs' or 'dodge the purple urnbombs' to match the wiki's terminology.
- **Skeptic receipt:** Verzik P2: "Urnbombs that deal 'up to 44 damage if they are not avoided'" and the Athanatos-spawning "slow purple projectile". No red projectile appears in any documented phase.
- **Skeptic reasoning:** The data's P2 step (line 6911) says 'dodge red ball'. The P2 projectile that must be dodged is the urnbomb (described as a slow purple projectile, up to 44 damage), and the only other notable P2 projectile is the green-coloured Nylocas-spawning purple projectile. There is no red projectile mechanic in any Verzik phase. This is a genuine colour/terminology error, not a community shorthand the wiki recognises. Medium severity is right: a player can still dodge a visible projectile, so it is misleading rather than fatal.

## Tombs of Amascut

### [high] C7: The Desert Hard diary does not grant unlimited charges. It increases the maximum charge capacity from 6 to 50. The sceptre still depletes and still requires recharging at the desert lectern; it is not spam-teleportable indefinitely. The Elite diary raises the cap to 100 -- still not unlimited.

- **Data says:** Desert Hard diary makes the sceptre unlimited-charge, so you can spam-teleport without recharging at the desert lectern
- **Wiki says (raw):** The Hard Diary now allows a maximum of 50 charges, up from 6.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pharaoh%27s_sceptre
- **Suggested fix:** Change the conditional alternative description to accurately state that Desert Hard diary raises the sceptre's max charge capacity to 50 (rather than claiming unlimited charges). Remove the claim that recharging at the desert lectern is eliminated.
- **Skeptic receipt:** Raw wiki_lookup (Pharaoh's sceptre): 'By default, the sceptre holds three charges. Players who have progressed in the Desert Diary have their sceptre hold more charges; up to 10 with the easy diary, 25 with the medium diary, 50 with the hard diary, and 100 with the elite diary.' and 'To recharge the sceptre, the player must talk to the guardian mummy inside Jalsavrah, the Pyramid Plunder pyramid.'
- **Skeptic reasoning:** The data's conditionalAlternative for DESERT_HARD claims the diary 'makes the sceptre unlimited-charge, so you can spam-teleport without recharging at the desert lectern' (and travelTip 'unlimited charges via Desert Hard diary'). The wiki is explicit that the sceptre always holds a FINITE, capped number of charges -- 3 by default, 50 with the hard diary, 100 with elite -- and must be recharged via the guardian mummy at Jalsavrah (Pyramid Plunder), not a desert lectern. No diary grants unlimited charges. The data is factually wrong on a high-visibility mechanic and should be corrected to state the hard diary raises the cap to 50 (still finite, still requires recharging).

### [blocker] C16: P3 is not always Tumeken's Warden. Which Warden appears in P3 depends on which was fought in P2: if Elidinis' was P2, then Tumeken's is P3; if Tumeken's was P2, then Elidinis' is P3. The guidance hard-codes P3 as Tumeken's Warden, which is wrong half the time and misdirects players who chose the other trough in P1.

- **Data says:** P3 (Tumeken's Warden): pray Protect from Magic against the energy ball volleys, dodge the obelisk hand-slam, swap prayers when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks
- **Wiki says (raw):** If Elidinis' Warden was fought in the second phase, players will face against Tumeken's Warden in the third phase. / If Tumeken's Warden was fought in the second phase, players will face the final phase against Elidinis' Warden.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tumeken%27s_Warden
- **Suggested fix:** Rewrite the P3 description to reflect that P3 is whichever Warden was not fought in P2, rather than asserting it is always Tumeken's Warden.
- **Skeptic receipt:** Wiki (Tumeken's Warden): 'If Elidinis' Warden was fought in the second phase, players will face against Tumeken's Warden in the third phase. / If Tumeken's Warden was fought in the second phase, players will face the final phase against Elidinis' Warden.' Phase determination: 'To power up Tumeken's Warden, a player must stand on the west trough.'
- **Skeptic reasoning:** The P3 guidance hard-codes 'P3 (Tumeken's Warden)'. The wiki confirms which Warden is fought in P3 is conditional on P1/P2 choice: whichever Warden was NOT empowered/fought in P2 is faced in P3. A player who empowered Tumeken's for P2 fights Elidinis' in P3. Hard-coding P3 as Tumeken's is wrong for that branch and misdirects players. The mechanic is player-choice-dependent, not a fixed sequence.

### [blocker] C19: Each P3 Warden only summons phantoms of the two path bosses associated with it -- Tumeken's Warden P3 summons Zebak and Ba-Ba phantoms; Elidinis' Warden P3 summons Akkha and Kephri phantoms. Neither P3 Warden summons all four phantom bosses simultaneously. Claiming players must swap prayers for all four bosses in a single P3 is factually wrong and misleads players.

- **Data says:** swap prayers when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks
- **Wiki says (raw):** summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom) [for Tumeken's Warden P3] / summon phantoms of the bosses the player fought in the upper levels of the tomb (Akkha and Kephri) [for Elidinis' Warden P3]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Elidinis%27_Warden
- **Suggested fix:** Split the P3 guidance into two branches (Tumeken's Warden P3: Zebak + Ba-Ba phantoms; Elidinis' Warden P3: Akkha + Kephri phantoms), or describe generically that the P3 Warden summons phantoms of the two path bosses relevant to that Warden.
- **Skeptic receipt:** Wiki (Tumeken's Warden final phase): 'summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom).' Per the candidate's Elidinis' Warden quote, that Warden instead summons Akkha and Kephri phantoms -- confirming each P3 Warden summons only its own two, never all four.
- **Skeptic reasoning:** The P3 description says 'swap prayers when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks' -- implying a single P3 Warden summons all four path-boss phantoms. The wiki shows each P3 Warden summons only the TWO phantoms tied to it: Tumeken's Warden P3 summons Zebak's Phantom + Ba-Ba's Phantom; Elidinis' Warden P3 summons Akkha + Kephri. Listing all four under one (hard-coded Tumeken's) P3 is factually wrong. This compounds the C16 hard-coding error. Fix is to branch the P3 phantom set by which Warden is faced (or describe it generically as the two relevant path bosses).

## Tombs of Amascut (300 Invocation)

### [high] C7: The data claims the Desert Hard diary grants unlimited charges on the Pharaoh's sceptre. The wiki shows Hard diary = 50 charges and Elite diary = 100 charges. No tier provides unlimited charges. Calling it 'unlimited-charge' is factually wrong and will mislead players into not recharging when they run out.

- **Data says:** Pharaoh's sceptre to the Necropolis (Desert Hard diary makes the sceptre unlimited-charge, so you can spam-teleport without recharging at the desert lectern)
- **Wiki says (raw):** up to 50 with the hard diary, and 100 with the elite diary
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pharaoh%27s_sceptre#Recharging
- **Suggested fix:** Replace 'Desert Hard diary makes the sceptre unlimited-charge' with 'Desert Hard diary increases the sceptre to 50 charges (100 with Elite diary)' or simply remove the erroneous parenthetical about unlimited charges.
- **Skeptic receipt:** By default, the sceptre is recharged to three charges. Players who have progressed in the Desert Diary have their sceptre recharged with more charges; up to 10 with the easy diary, 25 with the medium diary, 50 with the hard diary, and 100 with the elite diary. (oldschool.runescape.wiki/w/Pharaoh's_sceptre -- 'No tier grants infinite charges. The Elite Diary provides the maximum at 100 charges.')
- **Skeptic reasoning:** The data's travel guidance states the Desert Hard diary 'makes the sceptre unlimited-charge, so you can spam-teleport without recharging at the desert lectern' (drop_rates.json line 7192, with the travelTip on line 7193 repeating 'unlimited charges via Desert Hard diary'). The OSRS Wiki is unambiguous that NO diary tier grants unlimited charges: the Hard diary recharges the sceptre to 50 charges and the Elite diary to 100. This is not a variant, multi-source, account-type, or staleness nuance -- the charge caps are stable, long-standing mechanics. The claim of 'unlimited charges' is a flat factual error that would lead players to stop recharging and strand themselves. The receipt holds. STANDS - high.

### [medium] C13: The data lists Akkha and Kephri as phantoms summoned by Tumeken's Warden in Phase 3. The wiki documents that Tumeken's Warden summons Zebak's Phantom and Ba-Ba's Phantom only. Akkha and Kephri belong to Elidinis' Warden's Phase 3. The guidance conflates the two wardens' phantom rosters, which would cause incorrect prayer-swap expectations for players facing Tumeken's Warden.

- **Data says:** P3 (Tumeken's Warden): pray Protect from Magic against the energy balls, dodge the obelisk hand-slam, swap prayers when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks
- **Wiki says (raw):** attack the player by raising the floors, and summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tumeken%27s_Warden
- **Suggested fix:** Change the phantom list for Tumeken's Warden Phase 3 to 'Zebak/Ba-Ba attacks' (removing Akkha and Kephri). If the guidance is meant to cover both wardens, note which phantoms belong to which warden.
- **Skeptic receipt:** Tumeken's Warden: 'summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom)'. Elidinis' Warden: 'summon phantoms of the bosses the player fought in the upper levels of the tomb (Akkha and Kephri)'. (oldschool.runescape.wiki/w/Tumeken%27s_Warden and /w/Elidinis%27_Warden)
- **Skeptic reasoning:** The data step (drop_rates.json line 7230) is explicitly labelled 'P3 (Tumeken's Warden)' and then instructs the player to 'swap prayers when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks'. The OSRS Wiki ties phantom summons to the specific warden, not the phase: Tumeken's Warden summons ONLY Zebak's Phantom and Ba-Ba's Phantom, while Elidinis' Warden summons Akkha's Phantom and Kephri's Phantom. I checked the refutation vectors: the encounter is randomized (either warden can be the third/final 'phase three' boss per the Tumeken's Warden page), but the data pins the label to 'Tumeken's Warden' by name -- and the named warden demonstrably does NOT mimic Akkha or Kephri. So randomization does not rescue the entry; the named warden and its listed attacks contradict the wiki. Akkha and Kephri belong to the other warden's roster. The contradiction is established by direct receipt. STANDS - low (the prayer-swap advice is still broadly actionable since the full encounter does involve all four styles, but the warden-to-phantom attribution is wrong and should list Zebak/Ba-Ba for Tumeken's Warden, or be reworded to cover both wardens).

## Tombs of Amascut (500 Invocation)

### [high] C9: The Desert Hard diary raises the sceptre charge cap from 25 to 50, not unlimited. Describing this as 'unlimited charges' is factually wrong and would mislead players into not recharging the sceptre.

- **Data says:** Desert Hard diary makes Pharaoh's sceptre have unlimited charges
- **Wiki says (raw):** "[Pharaoh's sceptre](/w/Pharaoh%27s_sceptre) will now hold up to 50 [charges](/w/Pharaoh%27s_sceptre#Recharging), up from 25"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Desert_Diary
- **Suggested fix:** Change the description to: 'Desert Hard diary increases Pharaoh's sceptre capacity to 50 charges (up from 25), reducing how often it needs recharging.'
- **Skeptic receipt:** "By default, the sceptre holds three charges. Players who have progressed in the Desert Diary have their sceptre hold more charges; up to 10 with the easy diary, 25 with the medium diary, 50 with the hard diary, and 100 with the elite diary." (OSRS Wiki, Pharaoh's sceptre). Wiki also confirms: "The document does not mention any teleportation items with unlimited charges."
- **Skeptic reasoning:** The data's conditionalAlternative (line 7839) explicitly states 'Desert Hard diary makes the sceptre unlimited-charge, so you can spam-teleport without recharging at the desert lectern', and the travelTip (line 7840) repeats 'unlimited charges via Desert Hard diary'. The OSRS Wiki Pharaoh's sceptre page is unambiguous that the Desert Hard diary raises the charge cap to a FINITE 50 (and even Elite caps at 100) -- there is no unlimited-charge tier at all. Refutation vectors checked: not account-type-dependent (the cap is fixed per diary tier for every account), not a variant/multi-source issue, and not stale (the diary charge scaling has not changed). 'Unlimited-charge' is a flat misstatement of a recharge-required mechanic and would mislead a player into never recharging, then getting stranded. STANDS as a low/medium-severity guidance bug -- the wording must say '50 charges (up from 25)', not unlimited. The finding's own suggestedFix wording is correct.

### [medium] C19: Each Warden only mimics two phantoms in P3, not all four. Tumeken's Warden summons Zebak's and Ba-Ba's Phantoms; Elidinis' Warden summons Akkha's and Kephri's Phantoms. Claiming a single Warden mimics all four is inaccurate -- the set of phantoms depends on which Warden was chosen.

- **Data says:** During P3, the Warden mimics attacks from Akkha, Zebak, Ba-Ba, and Kephri, requiring exact prayer swaps
- **Wiki says (raw):** Tumeken's Warden: "It will attack the player by raising the floors, and summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom)." Elidinis' Warden: "It will attack the player by raising the floors, and summon phantoms of the bosses the player fought in the upper levels (Akkha's Phantom and Kephri's Phantom)."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tumeken%27s_Warden
- **Suggested fix:** Clarify per warden: 'P3 phantoms depend on which Warden was chosen -- Tumeken's Warden summons Zebak and Ba-Ba phantoms; Elidinis' Warden summons Akkha and Kephri phantoms. Pray accordingly for each phantom's attack style.'
- **Skeptic receipt:** Tumeken's Warden: "summon phantoms of the bosses the player fought in the upper levels of the tomb (Zebak's Phantom and Ba-Ba's Phantom)". Elidinis' Warden: "summon phantoms of the bosses the player fought in the upper levels of the tomb (Akkha and Kephri)". (OSRS Wiki, Tumeken's Warden / Elidinis' Warden pages)
- **Skeptic reasoning:** The data P3 step (line 7884), although headed '(Tumeken's Warden)', instructs the player to 'swap prayers exactly when the Warden mimics Akkha/Zebak/Ba-Ba/Kephri attacks' -- listing all four path-boss phantoms. The OSRS Wiki establishes that each Warden summons only TWO phantoms in P3: Tumeken's Warden summons Zebak's and Ba-Ba's phantoms; Elidinis' Warden summons Akkha's and Kephri's phantoms. In any single raid the player faces exactly one Warden in P3 (the one that enraged in P1), so only two of the four phantoms ever appear in a given P3 -- and for the step's own labelled case (Tumeken's Warden) the correct phantoms are Zebak and Ba-Ba, NOT Akkha/Kephri. Listing Akkha and Kephri under the Tumeken's-Warden step is factually attributing the OTHER Warden's phantoms. Refutation vectors checked: not account-type/variant dependent, not multi-source, and the phantom mechanic is not stale. Confirmed as a guidance-accuracy bug; medium severity is fair since a reactive player prays to each phantom's telegraphed style regardless, but the listed phantom set is wrong. The finding's suggestedFix (split per Warden) is correct.

## Corrupted Gauntlet

### [high] C1: The guidance labels the Gauntlet's district as 'Lletya district'. Lletya is a separate elven settlement outside Prifddinas entirely. The Gauntlet is in the Amlodd district of Prifddinas. This appears in three guidance steps (guidanceSteps[0], [0].conditionalAlternatives[0], [0].conditionalAlternatives[1]).

- **Data says:** run north-west to the Gauntlet portal in Lletya district
- **Wiki says (raw):** The Amlodd district is located in the north-western part of the city and features the following: [The Gauntlet], a training ground created by the elves to ensure their warriors were capable of defending Prifddinas during the God Wars.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Prifddinas
- **Suggested fix:** Replace 'Lletya district' with 'Amlodd district' in all three occurrences within guidanceSteps[0] and its conditionalAlternatives.
- **Skeptic receipt:** Prifddinas wiki: The Amlodd district '...features "The Gauntlet, a training ground created by the elves..."' and is 'positioned in the north-western part of the city'. Lletya: 'Lletya is not a district of Prifddinas but rather a separate settlement... the few survivors stranded in Isafdar (notably Lletya, a small, hidden village the refugees erected).'
- **Skeptic reasoning:** The guidance (drop_rates.json lines 7968, 7981, 7991 for Corrupted Gauntlet, and 8090, 8103, 8113 for standard Gauntlet) repeatedly labels the Gauntlet portal as being in the 'Lletya district'. The wiki confirms The Gauntlet is in the AMLODD district (north-western Prifddinas), and Lletya is NOT a district of Prifddinas at all but a separate hidden village in Isafdar. This is a flat geographic error, not a variant, account-type, or staleness nuance. The cardinal direction ('north-west') is correct, but the district name is wrong. Note the finding lists only Corrupted Gauntlet occurrences; the identical error also appears in the standard Gauntlet entry (lines 8090/8103/8113), so the fix should cover both sources.

### [high] C4: The Western Provinces Elite diary does not unlock a Prifddinas teleport on any cape. The diary's elite rewards are entirely unrelated to Prifddinas or the Gauntlet. The max cape does have a Prifddinas teleport option but that is an inherent cape feature, not a diary unlock. The guidance fabricates a diary gate for this travel method.

- **Data says:** Western Provinces Elite diary attaches the Prifddinas teleport to your max / quest / achievement cape, so you can teleport from anywhere without dedicating an inventory slot to the seed
- **Wiki says (raw):** The Elite diary rewards listed are: Western banner 4 with unlimited teleports to Piscatoris Fishing Colony, Antique lamp worth 50,000 experience, Chompy bird hunting improvements, Free ogre arrows from Rantz, Slayer reward point improvements with Nieve/Steve, Daily resurrection at Zulrah. None of these rewards include a Prifddinas teleport on any cape.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Western_Provinces_Diary
- **Suggested fix:** Remove the WESTERN_ELITE diary requirement from conditionalAlternatives[0]. The description should reference the max cape's built-in Prifddinas teleport without tying it to Western Provinces Elite diary.
- **Skeptic receipt:** Western Provinces Diary (Elite rewards, verbatim): 'Western banner 4 (... unlimited teleports to Piscatoris Fishing Colony); Antique lamp (elite) ... 50,000 experience; Two chompy birds always appear at once...; chompy chick ... 1/500; Rantz offers 150 ogre arrows daily; Nieve/Steve grant same slayer reward points as Duradel/Kuradal; Daily resurrection with full health at Zulrah.' And: 'The Elite diary does not grant new Prifddinas teleportation or modify crystal teleport seed mechanics. The only relevant passage states: "Teleport crystals will now hold up to 5 charges, up from 3" -- but this benefit appears under the Hard tier rewards, not Elite.' Teleport crystal page: 'Completing the Hard Western Provinces Diary allows "she can enchant 5 instead of 3 charges".'
- **Skeptic reasoning:** The data (lines 7981/7982 and 8103/8104) asserts the Western Provinces ELITE diary 'attaches the Prifddinas teleport to your max / quest / achievement cape' so you can teleport from anywhere without a seed in inventory. The wiki refutes both halves: (1) the Western ELITE diary rewards are entirely unrelated to Prifddinas/crystal teleports (Piscatoris banner, antique lamp, chompy improvements, ogre arrows, slayer points, Zulrah resurrection); (2) the only crystal-teleport-related diary benefit is at the HARD tier (charges 3->5), not Elite; and (3) the teleport crystal / crystal teleport seed pages contain no rub-on-cape Prifddinas mechanic gated by any diary. The data fabricates a diary gate AND attributes it to the wrong tier. This survives the account-type vector (no account type unlocks a diary-gated cape Prifddinas teleport). The finding's core claim holds; the suggested fix to drop the WESTERN_ELITE gate is correct, though authors should note the 'cape Prifddinas teleport' travel method itself lacks wiki support and may warrant removal rather than mere relabeling.

### [high] C7: Raw chompy meat is not a resource in The Gauntlet or Corrupted Gauntlet. The wiki lists paddlefish, linum tirinum, phren bark, crystal ore, grym leaves, and crystal shards as prep-phase resources. Chompy meat is a separate item from Chompy bird hunting (tied to the Western Provinces area) and has no role inside the Gauntlet instance.

- **Data says:** gather paddlefish + linum tirinum + raw chompy meat + corrupted shards
- **Wiki says (raw):** Resources gathered: '3 crystal ores, linum tirinums and phren barks', '2 grym leaves', 'raw paddlefish', '2 weapon frames', '320 crystal shards'. Chompy meat is not mentioned anywhere in this strategies guide.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Gauntlet/Strategies
- **Suggested fix:** Remove 'raw chompy meat' from the prep phase description. The correct resource list is: paddlefish, linum tirinum, phren bark, grym leaves, crystal ore, and crystal/corrupted shards.
- **Skeptic receipt:** The Gauntlet/Strategies wiki: 'No chompy or bird meat is mentioned. The only food resource cited is paddlefish: "Gather a sufficient amount of raw paddlefish as well as three crystal ores, linum tirinums and phren barks" ... "Each paddlefish heals 20 Hitpoints."'
- **Skeptic reasoning:** The prep-phase descriptions (line 8006 Corrupted, line 8128 standard) instruct the player to 'gather paddlefish + linum tirinum + raw chompy meat + (corrupted/crystal) shards'. The wiki strategies page lists the gatherable Gauntlet resources as raw paddlefish, crystal/corrupted ore, linum tirinum, phren bark, grym leaves, crystal/corrupted shards, and weapon frames -- with raw paddlefish as the ONLY food. No chompy or bird meat exists inside the Gauntlet instance; raw chompy meat belongs to chompy bird hunting in the Western Provinces (a different activity that merely shares the wiki cluster of this source). This is a genuine fabrication present in both Gauntlet entries, not a variant or staleness issue.

### [medium] C16: The guidance claims Corrupted Hunllef swaps to its own protection prayer faster than standard Hunllef. The wiki states both variants use the same mechanics and the same 6-hit prayer-change cycle. No faster swap is documented for the Corrupted version. The claim about hitting harder is supported ('increased stats and increased damage' per the strategies page), but the prayer-swap speed difference is contradicted.

- **Data says:** it hits noticeably harder and swaps to its own protection prayer faster
- **Wiki says (raw):** Both the Crystalline and Corrupted Hunllef use the same mechanics. During the fight, the Hunllef will change its protection prayers to protect against the player's current attack style after every sixth attack by the player.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hunllef
- **Suggested fix:** Remove 'and swaps to its own protection prayer faster' from the guidanceSteps[3] description. The prayer mechanic is identical between standard and Corrupted Hunllef (every 6 off-prayer attacks).
- **Skeptic receipt:** Crystalline Hunllef wiki: 'Both the Crystalline and Corrupted Hunllef use the same mechanics.' and 'Every sixth off-prayer attack that the player performs on the Hunllef, including attacks which deal zero damage, will trigger a protection prayer change.'
- **Skeptic reasoning:** The Corrupted Gauntlet fight description (line 8014) claims the Corrupted Hunllef 'swaps to its own protection prayer faster' than the standard Hunllef. The wiki explicitly states both variants use identical mechanics and the protection-prayer change occurs every sixth off-prayer hit for both -- there is no faster swap for the Corrupted version. The 'hits noticeably harder' portion is correct (increased stats/damage) and is not flagged; only the prayer-swap-speed clause is contradicted, matching the finding's scoped suggested fix. This is not an account-type or variant nuance -- the mechanic is documented as identical.

## The Gauntlet

### [medium] C1: The Gauntlet portal is in the Amlodd clan district of Prifddinas, not in 'Lletya district'. Lletya is an entirely separate village outside Prifddinas in Isafdar, not a district within the city. The guidance names the wrong location, which would cause confusion for players trying to navigate.

- **Data says:** run north-west to the Gauntlet portal in Lletya district
- **Wiki says (raw):** The Amlodd district is located in the north-western part of the city and features the following: [The Gauntlet], a training ground created by the elves... Lletya is a small, hidden village the refugees erected [in Isafdar after the failed liberation attempt].
- **Wiki URL:** https://oldschool.runescape.wiki/w/Prifddinas
- **Suggested fix:** Replace 'Lletya district' with 'Amlodd district' in all three occurrences within guidanceSteps (steps 0, conditionalAlternatives[0], and conditionalAlternatives[1]).
- **Skeptic receipt:** Prifddinas wiki: 'The Amlodd district is located in the north-western part of the city and features the following: The Gauntlet, a training ground created by the elves...' and 'Lletya is a separate village outside Prifddinas, not a district within the city... a small, hidden village the refugees erected in Isafdar.'
- **Skeptic reasoning:** The guidance places the Gauntlet portal in 'Lletya district' in three places (step 0 description, and both conditionalAlternatives). Two independent wiki fetches confirm the Gauntlet is in the Amlodd clan district of Prifddinas (north-western part of the city), and that Lletya is a separate hidden village outside Prifddinas in Isafdar, not a district within the city. This is a repeated, player-facing navigation error that misnames the location. No multi-source/variant/account-type vector applies; the receipt directly establishes the contradiction. Fix: replace all three 'Lletya district' occurrences with 'Amlodd district'.

### [medium] C6: Two issues: (1) 'Raw chompy meat' is not a resource gathered during The Gauntlet prep phase -- the wiki lists paddlefish, linum tirinum, phren bark, grym leaf, and crystal shards; chompy is not among them. (2) The prep timer is 10 minutes for the regular Gauntlet (7:30 for Corrupted); '7-9 minutes' is inaccurate for the regular variant this source covers.

- **Data says:** Prep phase (about 7-9 minutes): gather paddlefish + linum tirinum + raw chompy meat + crystal shards
- **Wiki says (raw):** The required resources list includes paddlefish for healing but does not mention chompy meat. The document focuses on crystal ores, linum tirinums, phren barks, and grym leaves as preparation materials. The player has 10 minutes to prepare to fight this boss.
- **Wiki URL:** https://oldschool.runescape.wiki/w/The_Gauntlet
- **Suggested fix:** Remove 'raw chompy meat' from the resource list. Correct the timing to '10 minutes' (regular Gauntlet). If describing the Corrupted variant, the timing is 7:30.
- **Skeptic receipt:** The Gauntlet wiki: prep resources are 'Raw paddlefish ... Crystal ore ... Phren bark ... Grym leaf ... Linum tirinum' and 'Raw chompy meat is not mentioned anywhere'; timer: 'Regular Gauntlet: 10 minutes to prepare to fight this boss; Corrupted Gauntlet: 7 minutes and 30 seconds.'
- **Skeptic reasoning:** Step 4 lists 'raw chompy meat' among resources gathered during the Gauntlet prep phase. Two independent wiki fetches confirm the prep resources are raw paddlefish, crystal ore, phren bark, grym leaf, and linum tirinum -- raw chompy meat plays no role in the Gauntlet at all (chompy is a Western Provinces hunting item, unrelated). This is an unambiguous content error in player-facing guidance. Secondarily, the prep timer is 10 minutes for the regular Gauntlet (7:30 for Corrupted); the step's 'about 7-9 minutes' is inaccurate for the regular variant the step otherwise describes ('Enter -> Standard challenge'). The chompy-meat error alone makes the finding stand; the timer is also wrong for the regular variant. No variant/multi-source vector rescues a non-existent ingredient.

## Barrows

### [blocker] C17: Protect from Melee does not halve the chance -- it negates 75% of attacks fully, with only a 25% bypass chance. The guidance overstates the prayer's failure rate by exactly double, causing players to misunderstand how much protection the prayer actually provides.

- **Data says:** use Protect from Melee anyway to halve the chance, wear high-prayer gear
- **Wiki says (raw):** Verac has a 25% chance to hit through Protection Prayers, while the rest will have their damage fully negated.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Verac_the_Defiled
- **Suggested fix:** Change 'use Protect from Melee anyway to halve the chance' to 'use Protect from Melee -- it fully negates 75% of his attacks; only 25% bypass prayer'.
- **Skeptic receipt:** OSRS Wiki, Verac the Defiled: 'Despite being able to hit through Protect from Melee, it is still recommended as it negates on average 75% of his attacks and when the set effect does activate, lowers his max hit from 23 to 15.' His special 'penetrates through protection prayers with a 25% chance per attack.'
- **Skeptic reasoning:** Refutation vectors checked: this is a combat-mechanics guidance-text claim, so multi-source / item-variant / account-type vectors do not apply. The data step (line 8393) says 'use Protect from Melee anyway to halve the chance'. Verac's flail has a fixed 25% chance to bypass Protection Prayers; the prayer negates the other ~75% fully. 'Halve the chance' is wrong by exactly double - it implies a 50% bypass when it is 25%. The functional advice (use the prayer) is correct, so this is a magnitude/wording error, not a kill-breaking error - the proposed 'blocker' severity is overstated (low). But the numeric claim itself is genuinely wrong and contradicted by the wiki receipt.

### [high] C18: Verac is weak to Magic, not Crush. The wiki explicitly states his melee equipment makes him vulnerable to Magic. His defence bonuses show Crush +221 (high), confirming Crush is not a weakness. Recommending Crush wastes offensive efficiency.

- **Data says:** attack with Melee (he is weak to Crush)
- **Wiki says (raw):** He uses melee equipment, which make him vulnerable to Magic attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Verac_the_Defiled
- **Suggested fix:** Change '(he is weak to Crush)' to '(he is weak to Magic)' in the Verac guidance step description.
- **Skeptic receipt:** OSRS Wiki, Verac the Defiled defence bonuses: Stab +227, Slash +230, Crush +221, Magic +0 (50% weakness to air), Ranged +225. 'He uses melee attacks and is vulnerable to Magic attacks ... Verac has a 50% weakness to wind spells (as of May 29, 2024).' Barrows/Strategies: 'All of the Barrows brothers have a 50% elemental weakness against air, making Wind spells an effective way of killing them.'
- **Skeptic reasoning:** Combat-mechanics guidance text; no multi-source/variant/account-type rescue applies. Data step (line 8393) asserts '(he is weak to Crush)'. The wiki gives Verac a Crush defence of +221 (high - NOT a weakness) and a Magic defence of +0 with a 50% weakness to wind/air spells. The Barrows strategy guide confirms ALL six brothers share a 50% elemental weakness to air, making Magic their true weakness. Crush is therefore not a weakness; the parenthetical is factually false. (Recommending Melee as the style is practically defensible since his flail bypasses prayer, but the stated REASON 'weak to Crush' is wrong.) Note the same-entry Dharok and Torag steps correctly say 'Magic (his weakness)', so this is an internal inconsistency too.

### [medium] C24: The Agility drain is 20% of current level, not 50% (half). Claiming it 'halves' Agility substantially overstates the debuff severity. The effect also only triggers on 25% of hits.

- **Data says:** Karil uses Ranged and can halve your Agility
- **Wiki says (raw):** Karil's set effect is Tainted Shot, which gives him a 25% chance per successful hit to lower the player's Agility by 20%.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Karil_the_Tainted
- **Suggested fix:** Change 'can halve your Agility' to 'can reduce your Agility by 20% on a 25% chance per hit'.
- **Skeptic receipt:** OSRS Wiki, Karil the Tainted: "Karil's set effect is Tainted Shot, which gives him a 25% chance per successful hit to lower the player's Agility by 20%."
- **Skeptic reasoning:** Combat-mechanics guidance text; no multi-source/variant/account-type rescue. Data step (line 8423) says Karil 'can halve your Agility'. The wiki's Tainted Shot effect lowers Agility by 20% (of level) on a 25% chance per successful hit - not 50% ('halve'). 'Halve' overstates the drain by 2.5x. Genuine magnitude error; medium severity is appropriate (it does not break the kill but misstates the debuff).

### [high] C29: Guthan is weak to Magic, not Ranged. The wiki states he is 'vulnerable to Magic attacks' and lists only Magic-based methods as recommended offensive approaches. His ranged defence bonuses are +250, making Ranged an actively poor choice.

- **Data says:** attack with Ranged (his weakness)
- **Wiki says (raw):** Guthan is vulnerable to Magic attacks... Guthan is commonly defeated with the use of a trident of the swamp/seas, Iban Blast, Wind spells, or Magic Dart.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guthan_the_Infested
- **Suggested fix:** Change 'attack with Ranged (his weakness)' to 'attack with Magic (his weakness)' in the Guthan guidance step description.
- **Skeptic receipt:** OSRS Wiki, Guthan the Infested: 'Guthan uses melee equipment, making him vulnerable to Magic attacks ... a 50% elemental weakness to wind spells. Recommended defeat methods include: trident of the swamp/seas, Iban Blast, Wind spells, or Magic Dart.' Defence bonuses: Magic -11, Ranged +250 (Light/Standard/Heavy all equal).
- **Skeptic reasoning:** Combat-mechanics guidance text; no multi-source/variant/account-type rescue. Data step (line 8438) says 'attack with Ranged (his weakness)'. The wiki gives Guthan a Ranged defence of +250 (high - Ranged is an actively poor choice) and a Magic defence of -11, plus a 50% weakness to wind/air spells; recommended methods are all Magic-based (trident, Iban Blast, Wind spells, Magic Dart). Ranged is therefore wrong on both the recommended style and the stated weakness; Magic is correct. Confirmed wrong on both counts; high severity is justified (it steers the player to the worst-defended-against style).

## Tempoross

### [high] C7: Tempoross is not listed as a destination on the Minigame Group Finder (Minigame Grouping teleport). The step-0 travel tip instructs players to use this teleport, but it does not exist for Tempoross. Players following this guidance would open the interface and not find Tempoross listed.

- **Data says:** Minigame Grouping teleport -> Tempoross
- **Wiki says (raw):** Barbarian Assault, Blast Furnace, Bounty Hunter, Burthorpe Games Room, Castle Wars, Fishing Trawler, Giants' Foundry, Guardians of the Rift, Last Man Standing, Mage Training Arena, Mastering Mixology, Nightmare Zone, Pest Control, Rat Pits, Sorceress's Garden, Shades of Mort'ton, Soul Wars, Tithe Farm, Trouble Brewing, and TzHaar Fight Pit
- **Wiki URL:** https://oldschool.runescape.wiki/w/Minigame_Group_Finder
- **Suggested fix:** Remove the Minigame Grouping teleport reference entirely. Replace with the actual fastest travel methods: Ferryman Sathwood's ferry south of the Al Kharid bank, or Pharaoh's sceptre teleport to Ancient Pyramid then south-west.
- **Skeptic receipt:** No, Tempoross is not included. The article provides a complete table of activities ... and Tempoross does not appear in this comprehensive list.
- **Skeptic reasoning:** The step-0 travelTip instructs 'Minigame Grouping teleport -> Tempoross'. Tempoross is NOT an entry in the Minigame Group Finder. My independent fetch of the MGF activity list returns Barbarian Assault, Blast Furnace, Bounty Hunter, Burthorpe Games Room, Castle Wars, Clan Wars, Dagannoth Kings, Fishing Trawler, Giants' Foundry, God Wars Dungeon, Guardians of the Rift, Last Man Standing, Mage Training Arena, Mastering Mixology, Nightmare Zone, Pest Control, Player Owned Houses, Rat Pits, Royal Titans, Sorceress's Garden, Shades of Mort'ton, Shield of Arrav, Shooting Stars, Soul Wars, Theatre of Blood, Tithe Farm, Tombs of Amascut, Trouble Brewing, TzHaar Fight Pit, and Volcanic Mine -- with no Tempoross. The travel method is fabricated; a player opening the MGF would not find Tempoross. The actual travel is Ferryman Sathwood's ferry / pharaoh's sceptre, which the data already has in the adjacent ferry steps.

### [high] C8: Same fabricated Minigame Grouping teleport as C7, appearing also in the top-level travelTip field. Tempoross does not appear in the Minigame Group Finder at all.

- **Data says:** Minigame Grouping teleport -> Tempoross, or sail from the Ruins of Unkah
- **Wiki says (raw):** Barbarian Assault, Blast Furnace, Bounty Hunter, Burthorpe Games Room, Castle Wars, Fishing Trawler, Giants' Foundry, Guardians of the Rift, Last Man Standing, Mage Training Arena, Mastering Mixology, Nightmare Zone, Pest Control, Rat Pits, Sorceress's Garden, Shades of Mort'ton, Soul Wars, Tithe Farm, Trouble Brewing, and TzHaar Fight Pit
- **Wiki URL:** https://oldschool.runescape.wiki/w/Minigame_Group_Finder
- **Suggested fix:** Replace with 'Ferryman Sathwood ferry south of Al Kharid bank, or sail from Ruins of Unkah dock'
- **Skeptic receipt:** No, Tempoross is not included. ... Tempoross does not appear in this comprehensive list.
- **Skeptic reasoning:** Same fabrication as C7 in the top-level travelTip field ('Minigame Grouping teleport -> Tempoross, or sail from the Ruins of Unkah'). Tempoross is absent from the Minigame Group Finder. The 'sail from the Ruins of Unkah' half is correct, but the Minigame Grouping teleport half is a nonexistent travel method and should be replaced with the ferry (Ferryman Sathwood, south of Al Kharid bank).

### [medium] C10: The wiki does not use the term 'dark cloud spots' for fishing locations. Fishing spots are called 'harpoon spots' or 'fishing spots'. The grey/dark clouds on the wiki are a hazard (lightning attack) that creates fires -- they are not markers for fishing. The terminology conflates two different in-game elements, which could mislead players into thinking they should fish under lightning clouds rather than at designated fishing spots.

- **Data says:** Fish harpoonfish at the dark cloud spots
- **Wiki says (raw):** Large grey clouds move over the island and ships, expand, and then emit lightning bolts lighting fires.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tempoross
- **Suggested fix:** Change to 'Fish harpoonfish at the fishing spots (harpoon spots)' and note separately that dark clouds signal incoming lightning (move away).
- **Skeptic receipt:** The guide uses the term "Fishing spot (Tempoross Cove)" and mentions: "Always fish at the double spots where possible."
- **Skeptic reasoning:** The data says 'Fish harpoonfish at the dark cloud spots'. 'Dark cloud spots' is not an OSRS Tempoross concept. The wiki terms the fishing locations 'Fishing spot (Tempoross Cove)' and advises 'Always fish at the double spots where possible'; the large grey/dark clouds are the lightning HAZARD that move over the island and emit lightning bolts lighting fires. Telling players to fish at the 'dark cloud spots' conflates the fishing spots with the lightning-cloud hazard and could direct players to stand under incoming lightning. Correct term is fishing spots / double spots.

### [blocker] C11: Harpoonfish are cooked at the Shrine (Tempoross Cove), not 'on a cannon'. The cannon/ammunition crate is the loading destination for already-cooked fish. The guidance instructs 'use Fish on Cannon' which is the wrong interaction -- players must use the shrine to cook first, then load the cooked fish into the ammunition crate at the cannon. Following this step as written would fail to cook the fish.

- **Data says:** cook (use Fish on Cannon)
- **Wiki says (raw):** disrupt as many actions when it's incoming, such as when cooking at the shrine
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tempoross
- **Suggested fix:** Change to 'cook harpoonfish at the Shrine, then load cooked harpoonfish into the ammunition crate (cannon)'
- **Skeptic receipt:** Cooking Station: The guide refers to "the shrine" as the cooking location. ... Ammunition Crates: "Load all cooked harpoonfish into the closest harpoonfish cannon ammunition crate."
- **Skeptic reasoning:** The data step says 'run to a cannon and cook (use Fish on Cannon)'. Cooking harpoonfish is done at the SHRINE, not on the cannon. The strategy page: cooking location is 'the shrine'; then 'Load all cooked harpoonfish into the closest harpoonfish cannon ammunition crate.' The cannon/ammunition crate is the LOADING destination for already-cooked fish, not a cooking station. 'Use Fish on Cannon' to cook is the wrong interaction and would fail to cook the fish. The fix should split cooking (shrine) from loading (cannon ammunition crate). Note: the finding's 'blocker' severity is overstated -- the step is MANUAL and players cook at the shrine regardless -- but the instructional text is genuinely incorrect.

## Wintertodt

### [medium] C4: "Hooded cloak" is not listed as a warm clothing item on the wiki's warm clothing page. The wiki lists Clue hunter cloak and Wolf cloak as cloaks that qualify, but not a generic "hooded cloak". The invented item name could mislead players into thinking a non-qualifying item counts.

- **Data says:** warm clothing (Pyromancer / Hunter outfit / hooded cloak)
- **Wiki says (raw):** "Hooded cloak" does not appear anywhere on this page as a warm clothing item.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Warm_clothing
- **Suggested fix:** Replace 'hooded cloak' with a qualifying item or remove it. E.g. 'warm clothing (Pyromancer outfit / Clue hunter outfit / Lumberjack hat)'
- **Skeptic receipt:** Warm_clothing page (verbatim): "hooded cloak" does not appear; cloaks listed include "Wolf cloak", "Rainbow cape", "Clue hunter cloak", "Firemaking cape"... Wintertodt/Warm_clothing: "No, 'hooded cloak' does not appear on this list. The cloaks mentioned include 'Wolf cloak' and 'Rainbow cape', but no hooded cloak variant."
- **Skeptic reasoning:** 'Hooded cloak' is a real OSRS item but it provides NO cold protection at Wintertodt. Three independent authoritative pages (Warm_clothing, Wintertodt/Warm_clothing, Wintertodt/Strategies) were checked and none lists a 'hooded cloak'; the qualifying cloaks are Wolf cloak, Clue hunter cloak, Rainbow cape, and the various fire/max capes. Listing 'hooded cloak' as warm clothing in the guidance is a genuine factual error that would mislead a player into bringing a non-qualifying item. Not a variant or multi-source nuance - it simply is not a warm item. Suggested fix: replace 'hooded cloak' with a qualifying item (e.g. Clue hunter cloak / Wolf cloak) or drop it. STANDS as a low-to-medium correctness fix.

### [high] C13: Supply crates were replaced by a reward cart in October 2024. The guidance tells players to 'claim Supply crates from the box' which no longer matches the in-game mechanic -- the reward system is now a cart, not a box of crates.

- **Data says:** claim Supply crates from the box
- **Wiki says (raw):** Supply crates have been replaced with a reward cart...Released 9 October 2024
- **Wiki URL:** https://oldschool.runescape.wiki/w/Supply_crate_(Wintertodt)
- **Suggested fix:** Update guidance to 'claim rewards from the reward cart' instead of 'claim Supply crates from the box'
- **Skeptic receipt:** Supply_crate_(Wintertodt) page: "For the old variant of the main Wintertodt rewards, see Supply crate (discontinued)." Reward cart "released 9 October 2024"; rewards claimed from a "reward cart" located "just outside the Wintertodt Camp bank."
- **Skeptic reasoning:** Supply crates were replaced by a reward cart on 9 October 2024; the in-game claim mechanic is now a reward cart outside the bank, not 'a box' of crates. The guidance 'claim Supply crates from the box' describes outdated UI. This is legitimate staleness/drift (wiki_updates shows no recent re-edit reverting it). Note the collection-log item is still named 'Supply crate (Wintertodt)', so the clog membership is unaffected - only the claim-action phrasing is stale. Worth a wording update to 'reward cart', but severity is medium not high: the player still claims rewards and obtains the same clog items. STANDS as a low/medium phrasing-drift fix.

### [high] C14: The data claims 500+ points yields 4 crates but the wiki explicitly states 500 points = 2 rolls. The claimed figure of 4 is off by 2 and would set a false expectation for players.

- **Data says:** 500+ points per game (4 crates)
- **Wiki says (raw):** 500 points = 2 rolls.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Supply_crate_(Wintertodt)
- **Suggested fix:** Change '500+ points per game (4 crates)' to '500+ points per game (2 rolls)'
- **Skeptic receipt:** Supply_crate_(Wintertodt): "500 points = 2 rolls." Supply_crate_(discontinued): "Players received one supply crate per defeat, but the contents varied based on points... 2 rolls at 500 points, 3 rolls at 1,000 points, 4 rolls at 1,500 points." - never 4 crates at 500 points.
- **Skeptic reasoning:** The data's '500+ points per game (4 crates)' is wrong under BOTH the current and the discontinued systems. Current page: 500 points = 2 rolls. Discontinued page: a player received ONE crate per defeat, with 2 rolls at 500 points - never 4 crates. So '4 crates' at 500 points is fabricated against every documented version of the mechanic; it conflates 'crates' with 'rolls' and inflates the count. Setting an expectation of 4 crates at 500 points is materially misleading. STANDS. Fix: express as rolls per the wiki (500+ = 2 rolls), and note one crate/cart claim per game.

### [high] C15: The 850-point threshold does not exist in the wiki's reward table. The wiki shows roll counts only at 500/1000/1500 point milestones. Neither '850 points' nor '5 crates/rolls' appears -- both the threshold and the quantity are fabricated.

- **Data says:** 850+ (5 crates / Pyromancer outfit fast)
- **Wiki says (raw):** 500 points = 2 rolls. 1000 points = 3 rolls. 1500 points = 4 rolls.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Supply_crate_(Wintertodt)
- **Suggested fix:** Remove the '850+ (5 crates)' claim. If a higher-points milestone is desired, use '1500+ points (4 rolls)' which is the documented next milestone above 1000.
- **Skeptic receipt:** Supply_crate_(Wintertodt): "500 points = 2 rolls. 505 points = 2 rolls + 1% chance of an extra roll. 750 points = 2 rolls + 50% chance of an extra roll. 1000 points = 3 rolls. 1500 points = 4 rolls. For every 5 points past 500 points player gets 1% extra chance to get additional loot." - no 850 threshold, no 5-roll/5-crate entry.
- **Skeptic reasoning:** No '850 points' milestone exists in any documented reward table (current or discontinued); the milestones are 500/1000/1500 points giving 2/3/4 rolls, with intermediate +1%-per-5-points scaling. There is no '5 crates' outcome - players get one crate/cart per game, and the next roll milestone above 1000 is 1500 = 4 rolls. Both the 850 threshold and the '5 crates' quantity are fabricated. STANDS. Fix: drop '850+ (5 crates)'; if a higher milestone is wanted, use the documented 1500 points = 4 rolls.

## Zalcano

### [blocker] C18: Tetsu, Virtus, and Aurora dye moulds do not appear anywhere in Zalcano's drop table on the wiki. The unique drops are Crystal tool seed, Zalcano shard, Uncut onyx, and Smolcano. These dye-mould items appear to be fabricated; the loot step instructs players to pick up items that Zalcano does not drop.

- **Data says:** Pick up the Tetsu / Virtus / Aurora dye-mould reward, plus any Zalcano shards or Crystal tool seeds dropped.
- **Wiki says (raw):** Notable drops include Crystal shards, Infernal ashes, various runes, ores, bars, and unique items like Crystal tool seed (1/200) and Smolcano pet (1/2,250).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zalcano#Drops
- **Suggested fix:** Replace the dye-mould mention with the actual unique drops: 'Pick up any Zalcano shards or Crystal tool seeds dropped. Drops appear at your feet once the fight ends.'
- **Skeptic receipt:** wiki_lookup Zalcano drop table (26 entries): Crystal shard | Infernal ashes | runes | ores | bars | Uncut diamond | Uncut dragonstone | Onyx bolt tips | Pure essence | Crystal tool seed 39/8000 | Zalcano shard 1/750 | Smolcano 1/2250 | Uncut onyx 1/8000. WebFetch: 'According to the article, Zalcano does not drop any dyes or dye moulds... The boss does not drop Tetsu, Virtus, or Aurora-related equipment.'
- **Skeptic reasoning:** The data step literally instructs 'Pick up the Tetsu / Virtus / Aurora dye-mould reward.' The full Zalcano drop table (26 entries via wiki_lookup) contains no dye, no mould, and nothing named Tetsu/Virtus/Aurora. Those three dyes are Tombs of Amascut content, not Zalcano. Refutation vectors checked: not a multi-source clog item (no such clog item is sourced from Zalcano), not a variant, not account-type dependent, not staleness (current drop table is exhaustive). This is fabricated loot guidance that tells players to pick up items the boss does not drop. Blocker stands.

### [high] C10: The fairy ring code BIS does not exist and there is no fairy ring destination for Prifddinas. The wiki's complete list of 55 working codes does not include BIS or any destination in or near Prifddinas. This conditional alternative would route players to a nonexistent destination.

- **Data says:** POH fairy ring -> BIS (Prifddinas), then run south-east to the Trahaearn mining area and enter Zalcano's chamber
- **Wiki says (raw):** Currently, there are 55 working fairy ring codes out of a total possible 64 codes. There are only 64 possible combinations using 4 letters on each ring.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Remove the fairy ring BIS conditional alternative entirely, or replace it with a confirmed transport method such as 'Spirit tree to Prifddinas' (which the wiki does confirm as a valid route).
- **Skeptic receipt:** WebFetch Fairy_ring: 'BIS: Yes, the wiki confirms this code goes to the Ardougne Zoo. The entry states: "Ardougne Zoo - Unicorns"... Required for the medium Ardougne Diary.' and 'No fairy ring codes directly serve Prifddinas or the Zalcano/Trahaearn area.'
- **Skeptic reasoning:** Data alternative reads 'POH fairy ring -> BIS (Prifddinas)' and travelTip 'POH fairy ring BIS -> Prifddinas -> run SE to Zalcano'. The wiki confirms code BIS resolves to Ardougne Zoo - Unicorns in Kandarin, not Prifddinas. No fairy ring code labelled BIS reaches Prifddinas or the Trahaearn mining area. The data routes players to the wrong continent. Refutation vectors checked: not account-type dependent (BIS destination is fixed for all accounts), not staleness (BIS has long been Ardougne Zoo). The finding is correct that this conditional alternative is mis-coded. Note the suggested fix wording need not be adopted verbatim, but the underlying defect (BIS != Prifddinas) holds.

### [medium] C5: The Smiths' uniform set effect is explicitly tied to anvil actions and Giants' Foundry preform work. The wiki makes no mention of the uniform providing any benefit at Zalcano's tephra-smithing furnace. Advising players to bring these pieces wastes inventory slots.

- **Data says:** Bring a Dragon pickaxe (or Crystal pickaxe), Smiths uniform pieces if owned, a Goldsmith gauntlets for bonus XP
- **Wiki says (raw):** Each piece gives a 20% chance to speed up Smithing actions performed at an anvil ... the complete outfit guaranteeing this effect ... [preform bonuses apply] at the Giants' Foundry.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Smiths%27_uniform
- **Suggested fix:** Remove 'Smiths uniform pieces if owned' from the gear recommendation, as the set bonus does not apply to Zalcano's tephra smithing.
- **Skeptic receipt:** Smiths' uniform: '...20% chance to speed up Smithing actions performed at an anvil ... preform bonuses apply at the Giants' Foundry.' WebFetch Zalcano/Strategies: 'The wiki provides no mention of Smiths' uniform or Goldsmith gauntlets offering any benefit at Zalcano' and recommends 'Regen bracelet' for the hand slot.
- **Skeptic reasoning:** Data gear list recommends 'Smiths uniform pieces if owned'. The Smiths' uniform set effect (20%-per-piece chance to speed Smithing actions at an anvil; preform bonus at Giants' Foundry) is tied to anvil/Giants' Foundry work. Zalcano's tephra processing uses a furnace and an altar, not an anvil, and grants its own Mining/Smithing XP without anvil actions. The Strategies page recommends a Regen bracelet for the hand slot, never the Smiths' uniform. Refutation vectors: not account-type dependent, not a variant, not staleness. Recommending the uniform wastes inventory and misleads. Medium stands.

### [medium] C6: The goldsmith gauntlets bonus XP effect only applies to smelting gold ore into gold bars (including at Blast Furnace). The wiki does not list Zalcano or tephra smithing as an activity where the gauntlets provide any benefit. Advising players to bring them for 'bonus XP' at Zalcano is unsupported.

- **Data says:** Bring ... a Goldsmith gauntlets for bonus XP
- **Wiki says (raw):** Goldsmith gauntlets ... increase the experience gained from smelting gold ore into a gold bar, from 22.5 to 56.2 experience.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Goldsmith_gauntlets
- **Suggested fix:** Remove 'a Goldsmith gauntlets for bonus XP' from the gear recommendation. If a gloves-slot item is recommended, the strategies page suggests Regen bracelet instead.
- **Skeptic receipt:** Goldsmith gauntlets: '...increase the experience gained from smelting gold ore into a gold bar, from 22.5 to 56.2 experience.' WebFetch Zalcano/Strategies: 'The wiki provides no mention of Smiths' uniform or Goldsmith gauntlets offering any benefit at Zalcano.'
- **Skeptic reasoning:** Data recommends 'a Goldsmith gauntlets for bonus XP'. Goldsmith gauntlets only boost XP from smelting gold ore into gold bars (22.5 -> 56.2). Zalcano involves no gold-ore smelting; its tephra refining/imbuing grants fixed Mining/Smithing XP unaffected by the gauntlets. The Strategies page recommends a Regen bracelet for the hand slot, not Goldsmith gauntlets. Refutation vectors: not account-type/progress dependent, not a variant, not staleness. The 'bonus XP at Zalcano' premise is unsupported. Medium stands.

### [medium] C11: The cycle described in the guidance omits the imbuing step at the western altar. According to the wiki, the process is four steps: mine -> refine at furnace -> imbue at altar -> throw. The guidance collapses steps 2-3 into 'smith at furnace into imbued tephra', skipping the separate altar step. A player following this guidance would be confused when refined tephra from the furnace cannot be thrown directly at Zalcano.

- **Data says:** Cycle: mine glowing tephra rocks, smith tephra at the furnace into imbued tephra, then throw imbued tephra at Zalcano.
- **Wiki says (raw):** Tephra Production Process: 1. Mine glowing rock formations in prison corners 2. Refine tephra at eastern furnace 3. Imbue refined tephra at western altar 4. Throw imbued tephra at Zalcano.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zalcano/Strategies
- **Suggested fix:** Update cycle description to: 'mine glowing tephra rocks, refine tephra at the eastern furnace, imbue refined tephra at the western altar, then throw imbued tephra at Zalcano.'
- **Skeptic receipt:** WebFetch Zalcano/Strategies: '1. Mining: mine the glowing rock formation... 2. Furnace Refinement: at the furnace on the eastern side... refine your tephra into refined tephra 3. Altar Imbuing: at the altar on the western side... imbue your refined tephra into imbued tephra. Yes, the altar step is separate from the furnace.'
- **Skeptic reasoning:** Data cycle reads 'mine glowing tephra rocks, smith tephra at the furnace into imbued tephra, then throw'. The wiki Strategies page describes three distinct stations: (1) mine glowing rock for tephra, (2) refine into refined tephra at the eastern furnace, (3) imbue into imbued tephra at the western altar, then throw. The data collapses the furnace and altar into a single 'furnace into imbued tephra' step and omits the western altar entirely - a player would have refined (not imbued) tephra and be unable to throw it. Refutation vectors: not staleness (current mechanic), not account-type dependent. Medium stands.

## Sol Heredit

### [blocker] C11: Manticores use all three combat styles (ranged, magic, melee) in rapid succession and require prayer flicking between styles. The guidance to use a single 'Protect from Melee' is wrong -- it leaves the player unprotected against the ranged and magic hits that precede the final melee hit.

- **Data says:** Use Protect from Melee vs Manticores
- **Wiki says (raw):** the Manticore will proceed to charge up a triple attack with all three combat styles. It will either use a range-magic or magic-range as the first two hits; the last hit is always melee. [...] Tier 1 causes each orb to hit twice rather than once, intended to punish players who do not flick the attacks correctly
- **Wiki URL:** https://oldschool.runescape.wiki/w/Manticore
- **Suggested fix:** Replace 'Protect from Melee vs Manticores' with guidance to prayer-flick (Protect from Missiles -> Protect from Magic -> Protect from Melee, or vice versa) through the Manticore's triple-hit sequence.
- **Skeptic receipt:** the Manticore will proceed to charge up a triple attack with all three combat styles. It will either use a range-magic or magic-range as the first two hits; the last hit is always melee. [...] Tier 1 causes each orb to hit twice rather than once, intended to punish players who do not flick the attacks correctly
- **Skeptic reasoning:** The Manticore charges a triple attack across all three combat styles (range-magic or magic-range as the first two hits, melee always last), each landing one tick apart. A single static 'Protect from Melee' protects only the third hit and leaves the player exposed to the preceding ranged (max 36, the highest of the three) and magic (max 31) hits. The wiki states the mechanic is 'intended to punish players who do not flick the attacks correctly', confirming prayer flicking is the intended handling. The osrs-expert KB and wiki agree this is not handled by a single melee prayer. No refutation vector applies: this is a combat-mechanic instruction, not item/variant/account-type/staleness. The guidance is genuinely wrong.

### [blocker] C13: Sunfire splinters are not a consumable item that can be drunk during combat. The wiki describes them solely as crafting materials (for charging equipment, making sunfire runes, and brewing sunfire wine). Sunfire wine itself has the examine text 'Not for drinking.' There is no mechanic for consuming splinters to restore HP or stats during a Colosseum run.

- **Data says:** Drink Sunfire splinters when low
- **Wiki says (raw):** They can be used to charge Dizana's quiver and the Tonalztics of Ralos as well as blessing Dizana's quiver to make it permanently charged [...] Creating sunfire runes with rune essence [...] Creating jugs of sunfire wine from a jug of wine
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sunfire_splinters
- **Suggested fix:** Remove the 'Drink Sunfire splinters when low' instruction entirely. If a food/potion reminder is needed, reference sharks or Saradomin brews instead.
- **Skeptic receipt:** They can be used to charge Dizana's quiver and the Tonalztics of Ralos as well as blessing Dizana's quiver to make it permanently charged [...] Creating sunfire runes with rune essence [...] Creating jugs of sunfire wine from a jug of wine [...] Examine: Vaporised material from the sun.
- **Skeptic reasoning:** Both the OSRS Wiki and the osrs-expert meta knowledge base confirm Sunfire splinters are crafting/charging materials only: charging Dizana's quiver and Tonalztics of Ralos, creating sunfire runes, and brewing sunfire wine. Examine text is 'Vaporised material from the sun.' There is no eat/drink action and they restore no HP or stats inside the Colosseum. 'Drink Sunfire splinters when low' describes a mechanic that does not exist. No multi-source/variant/account-type/staleness vector rescues it.

### [high] C15: The wiki does not use the attack names 'Triple Spear', 'Triple Shield', or 'Spear Wall'. Sol Heredit's attacks are named Spear 1, Spear 2, Shield 1, and Shield 2. The described dodge mechanics also do not match: the wiki does not describe a 'move 2 tiles perpendicular' dodge, a '4 tiles away then back' dodge, or a 'run to the wall opposite his facing' dodge. The wiki's Grapple and Triple Parry specials (below 75% and 90% HP respectively) are also absent from the guidance.

- **Data says:** Sol Heredit cycles three melee specials: Triple Spear (move 2 tiles perpendicular), Triple Shield (run 4 tiles away then back), and Spear Wall (run to the wall opposite his facing)
- **Wiki says (raw):** Sol Heredit has 4 main AoE attacks [...] Spear 1: Sol will always begin the fight with this attack. Dodged by moving back from his centre or edge tiles. Spear 2: Dodged by moving to any of his off-centre tiles. Shield 1: Dodged by moving 1 tile back. Shield 2: [...] dodged by moving 2 tiles back.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sol_Heredit
- **Suggested fix:** Rewrite the Sol Heredit special attack guidance to use the wiki's actual attack names (Spear 1, Spear 2, Shield 1, Shield 2) and correct dodge directions. Add guidance for the Grapple (click the called body-part slot within 4 ticks, below 75% HP) and Triple Parry (activate Protect from Melee on the tick before each of the three hits, below 90% HP).
- **Skeptic receipt:** Spear 1: Sol will always begin the fight with this attack. Dodged by moving back from his centre or edge tiles. Spear 2: Dodged by moving to any of his off-centre tiles. Shield 1: [...] dodged by moving 1 tile back. Shield 2: [...] increasing the middle hazard's area to 9x9, which is dodged by moving 2 tiles back. Grapple (below 75% HP): he will drop his shield and call out a body part [...] 4 ticks to click on the item in the respective slot to parry. Triple Parry (under 90% HP): requires the player to precisely activate Protect from Melee on the tick before he lands his spear.
- **Skeptic reasoning:** The wiki names Sol Heredit's attacks Spear 1, Spear 2, Shield 1, Shield 2, plus Grapple (below 75% HP) and Triple Parry (under 90% HP). The data's names 'Triple Spear', 'Triple Shield', and 'Spear Wall' do not exist, and none of the described dodges ('move 2 tiles perpendicular', 'run 4 tiles away then back', 'run to the wall opposite his facing') match the wiki's documented dodges (move back from centre/edge tiles; move to off-centre tiles; move 1 tile back; move 2 tiles back). The Grapple and Triple Parry specials are absent entirely. Not an account-type or staleness issue -- the mechanics are simply fabricated/inverted. Severity high (misleading but not run-ending on its own).

### [high] C16: The wiki documents no 'shockwave' attack for Sol Heredit. His full documented attack repertoire is: Spear 1, Spear 2, Shield 1, Shield 2, Grapple (below 75% HP), and Triple Parry (below 90% HP). 'Shockwave Colosseum' is a wave NPC in the Fortis Colosseum (section 3.6 in the strategy guide), not a Sol Heredit mechanic. The 'unavoidable shockwave' description is fabricated.

- **Data says:** Sol Heredit's shockwave attack is unavoidable
- **Wiki says (raw):** Sol primarily uses AoE attacks that are unaffected by prayer, forcing the player to dodge his attacks to avoid taking up to 44 typeless melee damage. [...] Spear 1 [...] Spear 2 [...] Shield 1 [...] Shield 2 [...] Grapple [...] Triple Parry
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sol_Heredit
- **Suggested fix:** Remove the 'shockwave attack is unavoidable' claim from the Sol Heredit step. If the intent was to describe one of Sol's actual attacks that deals unavoidable typeless damage (his AoE attacks hit regardless of prayer), rewrite to reflect that Sol's primary AoE attacks are unaffected by prayer and must be dodged by movement.
- **Skeptic receipt:** Sol primarily uses AoE attacks that are unaffected by prayer, forcing the player to dodge his attacks to avoid taking up to 44 typeless melee damage. [Attacks: Spear 1, Spear 2, Shield 1, Shield 2, Grapple, Triple Parry] -- no attack named 'shockwave' is documented.
- **Skeptic reasoning:** Sol Heredit has no attack named 'shockwave'. His documented attacks are Spear 1/2, Shield 1/2, Grapple (below 75%), Triple Parry (under 90%), plus a 10%-HP enrage phase -- all dodged by movement, none unavoidable. The 'shockwave is unavoidable, tank it with Protect from Melee on' guidance is doubly wrong: there is no such attack, and Sol's AoE attacks are unaffected by prayer, so keeping 'Protect from Melee on' to mitigate damage does nothing. ('Shockwave Colossus' is a separate Colosseum wave NPC, not a Sol mechanic.) No refutation vector applies.

### [medium] C6: Sunfire splinters are crafting/charging materials, not items consumed during a Colosseum run. The instruction to bring them 'in case you reset on early waves' implies they serve a mid-run combat purpose (consistent with the sibling C13 claim to 'drink' them), which the wiki does not support. Bringing them as a bank-restock item for charging Dizana's quiver after a run is the only plausible legitimate use, but that framing is absent from the guidance.

- **Data says:** Take at least 5 Sunfire splinters in case you reset on early waves
- **Wiki says (raw):** They can be used to charge Dizana's quiver and the Tonalztics of Ralos [...] Creating sunfire runes with rune essence [...] Creating jugs of sunfire wine from a jug of wine
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sunfire_splinters
- **Suggested fix:** Remove the inventory slot recommendation for Sunfire splinters from the bank step. If the intent is to recharge Dizana's quiver between attempts, note that context explicitly rather than implying they are a mid-run consumable.
- **Skeptic receipt:** They can be used to charge Dizana's quiver and the Tonalztics of Ralos [...] Creating sunfire runes with rune essence [...] Creating jugs of sunfire wine from a jug of wine
- **Skeptic reasoning:** Same root defect as C13: the bank step tells the player to bring '>=5 Sunfire splinters in case you reset on early waves', implying a mid-run combat/sustain purpose. Splinters are a charging/crafting material with no in-run function, and are themselves a Colosseum drop (the entry lists Sunfire splinters as a wave reward, dropRate 1.0) -- you obtain them from runs, you do not consume them during one. The 'in case you reset on early waves' framing is mechanically meaningless and wastes an inventory slot. The recharge-quiver-between-attempts use the finding allows is not what the text says. Medium severity (inventory/loadout note, not a run-failing instruction).

## Crazy archaeologist

### [medium] C7: Wrong travel direction: the data says to 'run north-west' from Bandit Camp, but the wiki states the Bandit Camp is 'just east of the ruins', meaning the ruins are to the west of Bandit Camp (not north-west).

- **Data says:** Burning amulet -> Bandit Camp then run north-west
- **Wiki says (raw):** a burning amulet can be used to teleport to the Bandit Camp, which is just east of the ruins.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Crazy_archaeologist
- **Suggested fix:** Change 'run north-west' to 'run west' (or 'run west to the ruins') in guidanceSteps[1].description and travelTip.
- **Skeptic receipt:** Wiki: 'a burning amulet can be used to teleport to the Bandit Camp (Wilderness), which is just east of the ruins.' | Coords: Crazy archaeologist (2977,3702); Bandit Camp teleport/landing ~ (3038,3699). dx=-61 (west), dy=+3 -- direction is due west, not north-west.
- **Skeptic reasoning:** Hard geometry settles this. The burning-amulet Bandit Camp teleport lands inside the bandit camp (~3038, 3699; bandit spawns cluster x3024-3044, y3683-3706 per npc_spawns). The ruins are at (2977, 3702). Delta-x = -61 (due west), delta-y = +3 (a trivial 3 tiles north over a 61-tile run). The true bearing from Bandit Camp to the ruins is essentially due WEST, exactly matching the wiki's 'just east of the ruins.' The data's 'run north-west' implies a ~45-degree northward component that does not exist and would misdirect the player. This is not a multi-source, variant, account-type, or staleness issue -- it is a plain directional error, confirmed by both the wiki quote and cache coordinates.

## Araxxor

### [blocker] C1: The 'magic breaks acid spitting' mechanic does not exist in OSRS Araxxor. OSRS Araxxor uses an egg-based system (acidic/mirrorback/ruptura araxytes), not named paths. Acidic araxytes scatter acid pools on death; there is no acid-breaking mechanic. This appears to be RS3 Araxxor mechanics incorrectly applied to the OSRS version.

- **Data says:** Path 1 (Aviansie/Spitting) requires magic to break acid spitting.
- **Wiki says (raw):** Acidic Araxytes are ranged-attacking spiders that hatch from green eggs during Araxxor battles. They deal up to 15 damage and 'scatters acid pools around it upon death.' [No mechanic where magic breaks acid spitting is described anywhere on the page.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Acidic_araxyte
- **Suggested fix:** Replace the Path 1 description. For acidic araxytes, the strategy is to avoid the acid pools they create and kill them quickly with a noxious halberd or heavy ballista. Remove the 'magic to break acid' claim.
- **Skeptic receipt:** Acidic Araxytes: 'attack with ranged that can deal up to 15 damage. If killed, it will explode, splattering acid on random squares within a 7x7 AoE surrounding it.' (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (drop_rates.json line 9332) says 'Path 1 (Aviansie) - use magic to break acid spitting'. OSRS Araxxor has no 'Aviansie' path and no 'magic breaks acid spitting' mechanic. The wiki describes acidic araxytes as ranged attackers that explode into an acid AoE on death - there is nothing magic 'breaks'. This is RS3-flavored fabrication applied to the OSRS boss. Confirmed by wiki_lookup + WebFetch of the Strategies page.

### [blocker] C2: The mirrorback strategy is inverted. The wiki says NOT to melee mirrorbacks in melee range (they recoil 50% of damage back). The correct approach is to attack with ranged/magic/halberd from distance, or cast Vengeance before hitting. 'Tank with Protect from Melee and ignore the reflection' is the wrong guidance and will cause significant self-inflicted damage.

- **Data says:** Path 2 (Mirror) requires tanking with Protect from Melee and high defense; the reflection should be ignored.
- **Wiki says (raw):** Mirrorback Araxytes will redirect 20% of the damage dealt to Araxxor towards themselves, and further recoil 50% of that damage to the player... Attacking it with ranged, magic, or with a halberd (while one tile away) will ignore their deflection... if the player has Vengeance currently active, attacking it will result in no damage taken and the mirrorback will instead receive the damage
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor/Strategies
- **Suggested fix:** Replace Path 2 description: 'Mirrorback araxytes recoil melee damage - attack with ranged, magic, or a halberd from one tile away, or cast Vengeance before attacking. Do not melee them in melee range without Vengeance active.'
- **Skeptic receipt:** Mirrorback Araxytes: 'redirect 20% of the damage dealt to Araxxor towards themselves, and further recoil 50% of that damage to the player...' and 'Attacking it with ranged, magic, or with a halberd (while one tile away) will ignore their deflection.' (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9332) says 'Path 2 (Mirror) - tank with Protect from Melee and high def, ignore the reflection'. The wiki says the opposite: mirrorback araxytes recoil melee damage, and you should attack with ranged/magic/halberd-at-range or under Vengeance to ignore the deflection. Telling the player to tank and ignore reflection directs them into self-inflicted recoil damage. Confirmed by WebFetch of the Strategies page.

### [blocker] C4: The enrage/final phase prayer is wrong. The wiki specifies Protect from Melee for the enrage cleave attack. Protect from Magic is never recommended for any phase of OSRS Araxxor - the boss's dangerous enrage attack is a melee cleave.

- **Data says:** Phase 4 (final phase) requires Protect from Magic.
- **Wiki says (raw):** If the player fails to dodge the cleave attack, Protect from Melee will still reduce its damage by ~50%.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor/Strategies
- **Suggested fix:** Replace 'Protect from Magic' with 'Protect from Melee' for the enrage/final phase.
- **Skeptic receipt:** 'Araxxor will replace his melee attack with a cleave attack' dealing 'melee damage and leaving acid pools.' Recommended prayer is 'Protect from Melee' will 'reduce its damage by ~50%.' (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9332) says 'Phase 4 (final) requires Protect from Magic'. The enrage phase converts Araxxor's melee into a cleave attack, and the wiki recommends Protect from Melee (reduces it ~50%). Protect from Magic is not the enrage prayer. Confirmed by wiki_lookup (Protect from Melee reduces melee to 5; magic max hit only 21) and WebFetch of the Strategies enrage section.

### [blocker] C5: The 'web cocoon' special attack does not exist in OSRS Araxxor. Neither the main wiki page nor the strategies page mentions it. This appears to be fabricated or imported from a different game/boss. The enrage phase has a cleave attack, not a web cocoon.

- **Data says:** Phase 4 has a web cocoon special attack that must be dodged.
- **Wiki says (raw):** [Web cocoon is not mentioned anywhere on the Araxxor main page or Araxxor/Strategies page. The enrage phase special attacks are: a cleave attack that targets a 1x3 area, and Araxxor 'replaces his melee attack with a cleave attack'.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor
- **Suggested fix:** Remove the web cocoon claim entirely. Replace with accurate enrage phase mechanics: dodge the 1x3 cleave attack by standing directly under Araxxor (reduces it to 1x1 area).
- **Skeptic receipt:** Special attacks described: 'Acid Ball, Acid Splatter, and Acid Drip.' No web cocoon mechanic exists on the page. (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9332) says 'dodge the web cocoon special'. No web cocoon special attack exists for OSRS Araxxor on either the main page or the Strategies page; the special attacks are acid-based (Acid Ball / Acid Splatter / Acid Drip) plus the enrage cleave. The web cocoon is fabricated. Confirmed by WebFetch of both pages.

### [blocker] C8: Melee tank gear for the mirrorback path is the wrong recommendation. Mirrorbacks recoil 50% of melee damage back to the player. The wiki recommends ranged, magic, or halberd attacks from distance to negate the reflection. Recommending melee tank gear directs the player into the worst possible strategy.

- **Data says:** Melee tank gear is appropriate for Path 2 (Mirror).
- **Wiki says (raw):** Attacking it with ranged, magic, or with a halberd (while one tile away) will ignore their deflection... Mirrorback Araxytes will redirect 20% of the damage dealt to Araxxor towards themselves, and further recoil 50% of that damage to the player
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor/Strategies
- **Suggested fix:** Replace melee tank gear recommendation for Path 2 with ranged or halberd setup, consistent with wiki guidance that ranged/magic/halberd from distance ignores mirrorback deflection.
- **Skeptic receipt:** 'Attacking it with ranged, magic, or with a halberd (while one tile away) will ignore their deflection... Mirrorback Araxytes will redirect 20% of the damage dealt to Araxxor towards themselves, and further recoil 50% of that damage to the player.' (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9297) recommends 'melee tank for path 2 (Mirror)'. Meleeing a mirrorback in melee range incurs the 50% recoil the wiki warns against; the wiki directs ranged/magic/halberd-from-range or Vengeance to ignore the deflection. The melee-tank gear recommendation steers the player into the worst approach. Same authoritative receipt as C2.

### [high] C9: Mage gear is not recommended by the wiki for any Araxxor path. Araxxor has +237 Magic Defence making it highly resistant to magic. The wiki lists only melee and ranged setups. The mage-for-acid-path claim has no wiki support and conflicts with the boss's defensive stats.

- **Data says:** Mage gear is appropriate for Path 1 (Aviansie/Spitting).
- **Wiki says (raw):** [The strategies page lists melee and ranged setups for Araxxor. No magic-specific gear setup is listed. The page states Araxxor has Magic Defence of +237, making magic attacks less effective. The acidic araxyte page mentions no magic-specific strategy.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor/Strategies
- **Suggested fix:** Remove mage gear as a path recommendation. The wiki strategy guide lists melee (scythe/inquisitor) and ranged setups only.
- **Skeptic receipt:** 'Though Araxxor has an elemental weakness to fire spells, his high magical defences makes this trait mostly thematic, and the generally lower defences of magic robes will have the player take more damage overall.' (wiki_lookup, Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9297) recommends 'mage gear for path 1 (Aviansie/Spitting)'. The wiki explicitly discourages magic: Araxxor has high magical defence, the fire weakness is mostly thematic, and magic robes' lower defences make the player take more damage. The Strategies gear section lists only melee and ranged setups. Confirmed by wiki_lookup and WebFetch.

### [high] C12: OSRS Araxxor does not have numbered 'paths' assigned per encounter. The fight uses an egg-hatching pattern that determines which araxyte type spawns first within a single fight. The three patterns cycle eggs within the fight; there is no cross-kill path rotation system described anywhere in the wiki.

- **Data says:** The first Araxxor encounter is randomly assigned one of three paths.
- **Wiki says (raw):** The eggs will always follow one of three potential patterns, with the last two sets copying that of the first: Green > White > Red / White > Red > Green / Red > Green > White
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor/Strategies
- **Suggested fix:** Replace the path-assignment description with accurate egg-pattern mechanics: the fight uses three egg patterns (Green>White>Red, White>Red>Green, Red>Green>White) that determine which araxyte type appears first.
- **Skeptic receipt:** 'The eggs will always follow one of three potential patterns, with the last two sets copying that of the first: Green > White > Red / White > Red > Green / Red > Green > White' and 'No numbered paths exist.' (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9321) says 'The first encounter is randomly assigned one of three paths'. OSRS Araxxor has no numbered per-kill paths. The fight uses an egg-hatching pattern (Green>White>Red etc.) within a single kill, and the special is determined by the south-easternmost egg, not a pre-assigned path. The 'three paths' framing is RS3 mechanics. Confirmed by wiki_lookup and WebFetch.

### [high] C13: There is no kill-to-kill path cycling mechanic in OSRS Araxxor. The wiki describes an egg-based system that operates within each individual fight. The claim that 'subsequent kills cycle through remaining paths' is not supported by the wiki and appears to be RS3 mechanics incorrectly applied.

- **Data says:** Subsequent Araxxor kills cycle through the remaining two paths.
- **Wiki says (raw):** [No cross-kill path rotation or cycling system is described anywhere on the Araxxor main page or Araxxor/Strategies page. The egg pattern system operates within individual fights, not across kills.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxxor
- **Suggested fix:** Remove the cross-kill cycling claim. Describe the egg pattern system accurately: each fight, eggs hatch in one of three fixed patterns determining which araxyte types appear.
- **Skeptic receipt:** Egg patterns 'follow one of three potential patterns, with the last two sets copying that of the first... they remain consistent throughout a single kill.' No cross-kill cycling system exists on the page. (https://oldschool.runescape.wiki/w/Araxxor/Strategies)
- **Skeptic reasoning:** Our data (line 9321) says 'subsequent kills cycle through the remaining two' paths. There is no cross-kill path-rotation system in OSRS Araxxor. The egg-pattern system operates within each individual fight, with the last two egg sets copying the first set's pattern. Cross-kill cycling is fabricated. Confirmed by WebFetch.

## Perilous Moons

### [blocker] C3: Bowfa (ranged) is listed as a required weapon but all three moon bosses have +500 ranged defence. The wiki strategies page calls ranged heavily resisted and recommends only melee weapons.

- **Data says:** Bowfa for ranged, Ancient sceptre / Trident for magic - all three styles rotate during the fight
- **Wiki says (raw):** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks. [Defensively, they have +100 melee defence to the styles they are not weak to, along with +500 magic and ranged defence.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies
- **Suggested fix:** Remove Bowfa from the recommended/required items list. Ranged is not viable at Perilous Moons.
- **Skeptic receipt:** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks. Defensively, they have +100 melee defence to the styles they are not weak to, along with +500 magic and ranged defence.
- **Skeptic reasoning:** The step-1 description tells the player to equip 'Bowfa for ranged' and says 'all three styles rotate during the fight'. The wiki strategies page confirms all three moons have +500 ranged defence and are 'heavily resistant to magic and ranged attacks' - ranged is not viable. The guidance prose is genuinely wrong. Refutation vectors checked: not a variant/multi-source/account-type/staleness issue - this is a core mechanic of a melee-only encounter unchanged since the 2024 Varlamore release. Note for the fixer: Bowfa is NOT in the requiredItemIds/recommendedItemIds arrays (those hold melee ids 29271/28997/28988/11785), so the suggestedFix's 'remove from required items list' is mis-aimed; the error lives in the description string only.

### [blocker] C4: Ancient sceptre and Trident (magic weapons) are listed as required gear but all three moon bosses have +500 magic defence. The wiki explicitly states magic is heavily resisted and the strategies page recommends zero magic weapons.

- **Data says:** Ancient sceptre / Trident for magic - all three styles rotate during the fight
- **Wiki says (raw):** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks. [Defensively, they have +100 melee defence to the styles they are not weak to, along with +500 magic and ranged defence.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies
- **Suggested fix:** Remove Ancient sceptre and Trident from the recommended/required items. Magic is not viable at Perilous Moons.
- **Skeptic receipt:** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks. Defensively, they have +100 melee defence to the styles they are not weak to, along with +500 magic and ranged defence.
- **Skeptic reasoning:** The description recommends 'Ancient sceptre / Trident for magic'. Wiki confirms +500 magic defence and magic 'heavily resistant'. Magic is not viable; the guidance prose is wrong. Same vector check as C3 - core mechanic, not a variant/account/staleness artifact. Same fixer note: these magic weapons appear only in the prose, not in the id arrays, so the fix is to the description text.

### [blocker] C5: The guidance claims all three combat styles (melee, ranged, magic) rotate during the fight. The wiki states the content is melee-only; the only rotation is switching between melee weapon types (stab/crush/slash) to match each boss's weakness. Ranged and magic are heavily resisted and never used.

- **Data says:** all three styles rotate during the fight
- **Wiki says (raw):** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies
- **Suggested fix:** Correct the description to clarify that players switch between melee styles (stab for Eclipse, crush for Blue, slash for Blood) -- not between melee/ranged/magic.
- **Skeptic receipt:** Each boss is weakest to a specific melee style and heavily resistant to magic and ranged attacks. [...] Only melee weapons will damage the boss during this phase - ranged and magic weapons will not deal damage.
- **Skeptic reasoning:** The description asserts 'all three styles rotate during the fight', implying melee/ranged/magic rotation. The wiki confirms the content is melee-only; the only rotation is between melee weakness types (Eclipse=stab, Blue=crush, Blood=slash). All three moons roll their standard attacks 'against the player's melee defence' per their individual wiki pages, and the Eclipse clone phase explicitly states 'Only melee weapons will damage the boss'. The 'three styles rotate' framing is wrong.

### [blocker] C9: Two errors in one claim. First, the Blue Moon uses typeless slash (melee), not magic. Second, the freeze mechanic freezes the player's hands to reduce attack accuracy -- it does not freeze players to ice tiles they must step off. The weapon-freeze special attack places the player's weapon in a block of ice they must punch, which is also distinct from 'ice tiles to step off'.

- **Data says:** Fight Blue Moon (magic, freezes you to ice tiles - step off them)
- **Wiki says (raw):** Attack style: Typeless slash. Not having enough defence and being hit by the boss will cause your hands to freeze, causing you to miss attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Blue_Moon
- **Suggested fix:** Correct to: Blue Moon uses melee (typeless slash), weak to crush. The hand-freeze curse causes missed attacks when hit. The weapon-freeze special places your weapon in an ice block you must punch to retrieve while dodging icy spikes.
- **Skeptic receipt:** The boss' standard attacks are a series of three hits, rolling accuracy and damage against the player's melee defence. [...] Not having enough defence and being hit by the boss will cause your hands to freeze, causing you to miss attacks. [...] Weapon Freeze - Your weapon is forcefully removed and placed in a block of ice, where you must punch the ice with Eyatlalli's glyph under it.
- **Skeptic reasoning:** Data says 'Fight Blue Moon (magic, freezes you to ice tiles - step off them)'. Two errors confirmed against the Blue Moon wiki page. First, her standard attack rolls 'against the player's melee defence' (typeless melee), not magic. Second, the freeze is a hand-freeze that causes missed attacks, plus a Weapon Freeze special that places your weapon in an ice block you must punch - there is no 'ice tiles to step off' mechanic. The mechanic described in the data is fabricated.

### [high] C10: The guidance says to 'kill the real one' among mirror clones. The wiki describes a completely different mechanic: the player is bound in place while clones attack in waves, and the correct action is to face your character toward each clone to parry it -- not to identify and kill a real boss among fakes.

- **Data says:** Eclipse Moon (melee, summons mirror clones, kill the real one)
- **Wiki says (raw):** You are shoved to the centre of the room and are bound in place. Clones of the Eclipse Moon will spawn around you, attacking you in five cycles of three, for a total of 15 attacks. You must face your character toward each clone as it spawns to prevent damage. Doing so will parry the incoming attack, sending an undelayed attack back at the boss.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Eclipse_Moon
- **Suggested fix:** Correct to: Eclipse Moon clone special -- you are bound in the centre while clones attack in waves; face your character toward each clone as it spawns to parry the attack and send damage back to the boss.
- **Skeptic receipt:** Clones - You are shoved to the centre of the room and are bound in place. Clones of the Eclipse Moon will spawn around you, attacking you in five cycles of three, for a total of 15 attacks. You must face your character toward each clone as it spawns to prevent damage. Doing so will parry the incoming attack, sending an undelayed attack back at the boss.
- **Skeptic reasoning:** Data says Eclipse Moon 'summons mirror clones, kill the real one'. The wiki Clone special describes the player being bound in the centre while clones attack in five cycles of three; the correct response is to face your character toward each clone to parry. There is no identify-the-real-one-and-kill-it mechanic. The guidance is wrong.

### [blocker] C11: Two errors. Blood Moon uses typeless slash (melee), not ranged. The wiki describes no teleporting mechanic; after six standard melee attacks it alternates between Blood Rain and Blood Jaguar specials.

- **Data says:** Blood Moon (ranged, teleports around the arena)
- **Wiki says (raw):** Attack style: Typeless slash. The boss' standard attacks are a series of three hits, rolling accuracy and damage against the player's melee defence.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Blood_Moon
- **Suggested fix:** Correct to: Blood Moon uses melee (typeless slash), weak to slash. It does not teleport; specials are Blood Rain (move off blood pools) and Blood Jaguar (run to Eyatlalli's glyph and attack the jaguar).
- **Skeptic receipt:** The boss' standard attacks are a series of three hits, rolling accuracy and damage against the player's melee defence. [...] After six standard attacks, she will use one of two special attacks, alternating between them: Blood Rain [...] Blood Jaguar
- **Skeptic reasoning:** Data says Blood Moon is 'ranged, teleports around the arena'. The Blood Moon wiki page shows her standard attacks roll 'against the player's melee defence' (typeless melee, weak to slash per the strategies page), not ranged. There is no teleport mechanic; her specials are Blood Rain and Blood Jaguar. Both the style label and the teleport claim are wrong.

### [blocker] C12: The wiki describes no mechanic where Blood Moon auto-targets based on the player's last combat style, nor any prayer mechanic controlled by style rotation. This appears to be fabricated guidance -- potentially confused with a different boss (e.g., Tormented Demons). The wiki's Blood Moon page shows a straightforward melee attacker with two special attacks and no style-tracking behaviour.

- **Data says:** it auto-targets the same style you used last hit, so rotate styles to control its prayer
- **Wiki says (raw):** The boss' standard attacks are a series of three hits, rolling accuracy and damage against the player's melee defence.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Blood_Moon
- **Suggested fix:** Remove the auto-targeting / style-rotation / prayer-control claim entirely. Blood Moon does not have this mechanic according to the wiki.
- **Skeptic receipt:** The boss' standard attacks are a series of three hits, rolling accuracy and damage against the player's melee defence. [...] The entire fight repeats on a cycle of 156 game ticks making a tick counting cycle of either 2, 3, or 13 most useful.
- **Skeptic reasoning:** Data claims Blood Moon 'auto-targets the same style you used last hit, so rotate styles to control its prayer'. The Blood Moon wiki page describes no last-style-tracking auto-target and no prayer mechanic controlled by style rotation - her behaviour is a fixed 156-tick cycle of standard melee attacks plus Blood Rain / Blood Jaguar specials. This is fabricated guidance (the style-tracking/prayer-flicking behaviour resembles Tormented Demons, not Blood Moon). Also internally inconsistent: rotating styles is impossible at a melee-only boss.

### [high] C13: The wiki shows no Lunar key mechanic. The Lunar Chest opens via a 'Claim' option after defeating the bosses -- no per-kill key drops are described. The 'Lunar key' page on the wiki redirects to 'Moon key', which is an entirely different item used in Ruins of Tapoyauik, unrelated to Perilous Moons.

- **Data says:** Each kill drops a Lunar key
- **Wiki says (raw):** It can be opened after defeating at least one of the Moons of Peril, although all Moons must be defeated once in order to unlock the chest for the first time.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lunar_chest
- **Suggested fix:** Remove Lunar key references. Describe the Lunar Chest as opening after defeating the bosses (all three for first-time unlock, at least one per run thereafter).
- **Skeptic receipt:** The chest does not require keys to open. Instead, it features a 'Claim' option [...] the opened version of the chest now also gives 'Claim' option, so that multiple players can loot the chest at once. [Lunar key redirects to Moon key:] there is no separate 'Lunar key' item [...] now redirects to the Moon key page.
- **Skeptic reasoning:** Data says 'Each kill drops a Lunar key'. The Lunar Chest wiki page describes a 'Claim' option to open the chest; no per-kill keys exist. The wiki 'Lunar key' title redirects to 'Moon key', an unrelated Ruins of Tapoyauik item built from loop+tooth halves. No Lunar key item exists for Perilous Moons.

### [high] C14: No key-insertion mechanic exists. The chest is opened with a 'Claim' option after defeating bosses, not by using three keys on it. The number of bosses killed determines how many loot rolls you receive (1 boss = 1 roll, 2 bosses = 3 rolls, 3 bosses = 6 rolls), but there are no keys involved.

- **Data says:** use all three on the Lunar Chest for unique drops
- **Wiki says (raw):** It can be opened after defeating at least one of the Moons of Peril, although all Moons must be defeated once in order to unlock the chest for the first time.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lunar_chest
- **Suggested fix:** Correct to: defeat all three moons for 6 loot rolls from the Lunar Chest. No keys are required -- just claim the chest after the kills.
- **Skeptic receipt:** The chest does not require keys to open. Instead, it features a 'Claim' option [...] '1 Moon gives 1 roll', '2 Moons give 3 rolls', '3 Moons give 6 rolls'.
- **Skeptic reasoning:** Data says 'use all three on the Lunar Chest for unique drops', describing a key-insertion mechanic. No such mechanic exists - the chest is opened via the Claim option, and the number of moons defeated determines loot rolls (1 moon = 1 roll, 2 = 3 rolls, 3 = 6 rolls). The key-insertion framing is fabricated.

### [medium] C15: The Lunar Chest is in the Ancient Shrine within Neypotzli, not the centre of the combat arena. The strategies page directs players to 'head south to enter the Ancient Shrine to claim your loot', indicating it is in a separate shrine chamber, not the arena centre.

- **Data says:** the Lunar Chest is located in the centre of the Perilous Moons arena
- **Wiki says (raw):** The Lunar Chest, [is] found in the Ancient Shrine within Neypotzli.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lunar_chest
- **Suggested fix:** Correct to: after defeating the moons, head south to the Ancient Shrine within Neypotzli to claim the Lunar Chest.
- **Skeptic receipt:** [Location:] The Ancient Shrine within Neypotzli is where this chest is found. [Strategies:] Head south to enter the Ancient Shrine to claim your loot.
- **Skeptic reasoning:** Data (step 4) says to open the Lunar Chest 'in the centre of the arena'. The wiki places the Lunar Chest in the Ancient Shrine within Neypotzli, with the strategies page directing players to 'head south to enter the Ancient Shrine to claim your loot' - a separate chamber, not the combat-arena centre. Confirmed, but lowest stakes of the set (location, not a combat-survival error); severity 'medium'/low is appropriate.

## Tormented Demons

### [low] C5: The Guthixian temple teleport goes to the Ancient Guthixian Temple, not the Chasm of Tears. These are related but distinct locations -- the temple is at the bottom of the chasm, reached via it. The destination label in the guidance is wrong.

- **Data says:** Use a Guthixian temple teleport to drop straight into the Chasm of Tears
- **Wiki says (raw):** Teleports you to the Ancient Guthixian Temple.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guthixian_temple_teleport
- **Suggested fix:** Change to 'Use a Guthixian temple teleport to travel directly to the Ancient Guthixian Temple entrance.'
- **Skeptic receipt:** Guthixian_temple_teleport: 'Teleports you to the entrance of the Ancient Guthixian Temple.' Strategy page: temple teleport reaches 'the temple'; the Chasm is a distinct route via 'a games necklace to teleport to Tears of Guthix... Follow the chasm and climb the walls next to the centre skull.'
- **Skeptic reasoning:** Refutation vectors checked: not a clog-id/variant/account-type issue, purely a destination-label question. The Guthixian temple teleport's authoritative destination is the Ancient Guthixian Temple entrance, NOT the Chasm of Tears. The strategy page treats the Chasm route (games necklace to Tears of Guthix, light creature, climb walls) as a SEPARATE alternative to the teleport. So 'drop straight into the Chasm of Tears' mislabels the destination. Genuinely low severity: both routes converge on the same fight area and this is a cosmetic label, not a navigation-breaking error, so it should be logged as low, not a blocker.

### [high] C8: The wiki does not describe Darklight or Emberlight's special attack as the shield-breaking mechanism. The shield drops automatically after the player's first attack (any attack) and after each fire bomb attack. Darklight/Emberlight are valuable as demonbane weapons (bypassing the damage reduction) but are not required to break the shield.

- **Data says:** Hit the demon with Darklight (or Emberlight) special attack to break its shield
- **Wiki says (raw):** Tormented demons usually have a fire shield that reduces damage done by non-demonbane weapons/spells and non-abyssal weapons by 20%. They will drop this shield after the player's first attack and after each fire bomb attack.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Change to: 'The fire shield drops after your first hit and after each fire bomb attack -- demonbane weapons like Darklight or Emberlight bypass the shield's damage reduction entirely.'
- **Skeptic receipt:** 'Tormented demons usually have a fire shield that reduces damage done by non-demonbane weapons/spells and non-abyssal weapons by 20%. ... The shield drops after the player's first attack and after each fire bomb attack, remaining down until attacked once more.'
- **Skeptic reasoning:** No variant/multi-source dimension applies. The guidance attributes shield-breaking to the Darklight/Emberlight SPECIAL ATTACK, but the wiki mechanic is that the fire shield drops after the player's FIRST attack (any attack) and after each fire bomb attack. Demonbane weapons' role is to bypass the 20% damage reduction, not to 'break the shield' via a special. The data misstates the mechanic. The fix correctly separates 'shield drops on first hit / after fire bombs' from 'demonbane bypasses the reduction.'

### [blocker] C10: The prayer swap trigger is every 150 HP dealt to the demon, not every two successful hits. Swapping on every two hits is wrong -- it will cause unnecessary style switches that do not track the actual prayer-swap threshold, potentially resulting in the player always hitting into an active protection prayer.

- **Data says:** Swap your style between melee / ranged / magic on every two successful hits to keep accuracy up
- **Wiki says (raw):** Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Change to: 'Switch your combat style after dealing roughly 150 damage to the demon -- at that threshold it changes its protection prayer to match your last style, so swap to a different style to keep accuracy up.'
- **Skeptic receipt:** 'Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).'
- **Skeptic reasoning:** The prayer-swap trigger is HP-based, not hit-count-based. 'Every two successful hits' does not track the 150-HP threshold and will desync the player's style swaps from the demon's actual prayer changes, causing hits into an active protection prayer. Not account-type dependent. Receipt is unambiguous.

### [blocker] C11: The wiki explicitly states Tormented Demons have a 30% elemental weakness to WATER spells (updated June 25, 2025). Recommending fire spells is directly wrong -- fire spells receive no bonus and the player would forgo the 30% water weakness bonus. Smoke Burst is shadow/smoke magic and also does not exploit the water weakness.

- **Data says:** Use fire spells or Smoke Burst for the magic phase to take advantage of their fire weakness
- **Wiki says (raw):** Tormented Demons now have a 30% elemental weakness to water spells.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Change to: 'Use water spells (e.g. Water Surge or Sang staff on water spell) for the magic phase to exploit their 30% elemental weakness to water.'
- **Skeptic receipt:** 'Per the June 2025 update, tormented demons now have a 30% elemental weakness to water spells.'
- **Skeptic reasoning:** Checked staleness vector: WebFetch of the live page confirms the current (post June-2025 update) state is a 30% elemental weakness to WATER spells. Recommending fire spells or Smoke Burst (smoke/shadow) forgoes the water bonus and is directly wrong against current mechanics. This is the inverse of a stale-data false positive: the DATA is stale/wrong, the wiki is current. Blocker severity is appropriate.

### [medium] C13: The wiki describes only one named special attack -- the fire bomb bind attack. There is no 'molten spray cone' mechanic named or described, and no 'raises both arms' telegraph. The special attack is a player-binding followed by two fire bombs at fixed positions, not a directional cone to dodge.

- **Data says:** Step out of the molten spray cone (the demon telegraphs it by raising both arms)
- **Wiki says (raw):** They will briefly bind the player in place roughly every 60 ticks (36s; 10 attacks) and disable run... then release two fire bombs: one on the player's position as of the start of the attack, and a second on a random one of the 8 adjacent squares.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Remove or replace with accurate fire bomb description: 'When the demon binds you in place, a fire bomb will land on your tile and a second on a random adjacent tile -- pre-move before the bind to avoid the on-tile bomb.'
- **Skeptic receipt:** 'They will briefly bind the player in place roughly every 60 ticks (36s; 10 attacks) and disable run, then release two fire bombs: one on the player's position as of the start of the attack, and a second on a random one of the 8 adjacent squares.' The page makes no mention of 'molten spray,' 'cone' attacks, or 'raises both arms' telegraphs.
- **Skeptic reasoning:** The 'molten spray cone' with a 'raises both arms' directional telegraph is not a documented Tormented Demon mechanic. The only special attack on the page is the periodic bind (every ~60 ticks) followed by two fire bombs (one on the player's start tile, one on a random adjacent tile). The guidance describes a fabricated dodge mechanic. The suggested fix matches the real fire-bomb behavior. The page explicitly contains no 'molten spray'/'cone'/'raises both arms' text.

### [blocker] C14: The 'two hits per style' rotation is contradicted by the wiki. The prayer swap is triggered by 150 cumulative HP dealt, not by hit count. A rigid 2-hit rotation will not align with actual prayer swaps, causing the player to frequently hit into an active protection prayer.

- **Data says:** Three-style rotation: hit twice with melee, swap to ranged for two hits, then magic for two hits
- **Wiki says (raw):** Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Replace with HP-based rotation guidance consistent with C10 fix: 'Switch style after dealing ~150 damage (roughly 25% of the demon's HP) to stay ahead of its prayer changes.'
- **Skeptic receipt:** 'Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).'
- **Skeptic reasoning:** Same root cause as C10: the rigid 'two hits per style' three-style rotation is contradicted by the HP-threshold (150 damage) prayer-swap trigger. A fixed hit-count rotation does not align with the actual swap point and will repeatedly hit into an active protection prayer. Not account-type or progress dependent.

### [high] C15: The 'lags behind by one cycle' framing is inaccurate. The demon changes its prayer TO the last style that damaged it (at 150 HP intervals), so after the swap the player should switch to a DIFFERENT style. There is no fixed 'one cycle lag' -- the prayer changes at an HP threshold, not on a game-tick cycle tied to the player's attacks.

- **Data says:** The demon's protection prayer always lags behind your last successful style by one cycle, so the swap keeps your accuracy high
- **Wiki says (raw):** Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tormented_Demons
- **Suggested fix:** Revise to: 'After each 150-damage threshold, the demon updates its protection prayer to match the style that last damaged it -- swap to a new style immediately after triggering the threshold.'
- **Skeptic receipt:** 'Every 150 health lost since their last prayer swap (roughly 25% of their max health), they will change their protection prayer to the last combat style they were damaged with, and will stop attacking for 6 ticks (3.6s).'
- **Skeptic reasoning:** The mechanism description 'always lags behind your last successful style by one cycle' is inaccurate. The demon changes its protection prayer TO the style that last damaged it, at a 150-HP-dealt threshold -- an HP event, not a per-attack 'cycle' with a fixed one-step lag. The correct reaction is to swap to a DIFFERENT style after each threshold. While the directional takeaway ('swap to keep accuracy') is roughly right, the stated mechanic is wrong and misleads on WHEN to swap, so the finding holds. High (not blocker) is fair since the practical advice is not catastrophically inverted.

## Beginner Treasure Trails

### [high] C4: Hans does not give any free daily beginner clue scroll, and no H.A.M. encampment task exists that awards clue scrolls. Hans is a cryptic clue answer NPC -- players speak to him to complete a step, not receive one. The H.A.M. Hideout wiki page confirms H.A.M. pickpocketing yields easy clue scrolls (not beginner), and Hans has no H.A.M. connection at all. This guidance fabricates a daily reward mechanic that does not exist.

- **Data says:** Hans in Lumbridge gives a free one per day on the H.A.M. encampment task
- **Wiki says (raw):** Hans is the solution to the beginner cryptic clue: 'Always walking around the castle grounds and somehow knows everyone's age.' ... He can tell players how old their accounts are or how long they've spent online by talking to him.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hans
- **Suggested fix:** Remove the 'Hans in Lumbridge gives a free one per day on the H.A.M. encampment task' claim. Hans can be cited as a cryptic clue answer (talk to Hans at Lumbridge Castle), not as a clue source.
- **Skeptic receipt:** Based on the wiki page provided, there is no mention of Hans giving out free beginner clue scrolls daily, nor any H.A.M. encampment tasks associated with him. Hans serves as a destination/answer NPC for cryptic clues rather than a clue scroll distributor. The page states he is 'the solution to the beginner cryptic clue'... meaning players solve clues by finding him, not that he distributes them.
- **Skeptic reasoning:** The data step (line 9845) claims 'Hans in Lumbridge gives a free one per day on the H.A.M. encampment task.' Refutation vectors all fail: this is not a multi-source clog item, not an item variant, not account-type dependent, and not staleness -- it asserts a daily reward mechanic. The Hans wiki page describes him only as a cryptic-clue ANSWER/destination NPC ('the solution to the beginner cryptic clue'); players visit Hans to COMPLETE a step, they do not receive a clue from him. There is no H.A.M. encampment task associated with Hans, and no free daily beginner clue mechanic exists. The same fabrication also leaks into the source-level travelTip (line 9886: 'or Hans in Lumbridge') and the recommendedItemIds context. Genuine bug: invented reward mechanic.

### [medium] C6: The wiki states beginner clues are 1-3 steps, not 1-5 as the guidance claims. The upper bound is wrong by 2 steps. Additionally, beginner emote clues verified via the wiki include Al Kharid (north of Al Kharid mine) which is outside the claimed Lumbridge/Varrock/Falador/Draynor-only coverage, so the geographic restriction is also overstated.

- **Data says:** Beginner clues are short (1-5 steps) and cover Lumbridge / Varrock / Falador / Draynor only.
- **Wiki says (raw):** Beginner clues can be between 1-3 steps long. Unlike all other clue types, which are equally likely to have each number of steps, beginner clues have a 10% chance of completion at 1 step and a 45% chance of completion at 2 or 3 steps.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Clue_scroll_(beginner)
- **Suggested fix:** Change '1-5 steps' to '1-3 steps'. Also broaden the location description -- beginner clues include Al Kharid among other locations, so 'primarily in the starter areas around Lumbridge, Varrock, Falador, and Draynor' is more accurate than 'only'.
- **Skeptic receipt:** Beginner clues can be between 1-3 steps long. Unlike all other clue types, which are equally likely to have each number of steps, beginner clues have a 10% chance of completion at 1 step and a 45% chance of completion at 2 or 3 steps. [Emotes guide:] 'Al Kharid mine is located just north of Al Kharid... Al Kharid mine represents a fifth distinct region beyond Lumbridge, Varrock, Falador, and Draynor.'
- **Skeptic reasoning:** The data step (line 9860) states 'Beginner clues are short (1-5 steps) and cover Lumbridge / Varrock / Falador / Draynor only.' The wiki explicitly states beginner clues are 1-3 steps, not 1-5 -- the upper bound is overstated by 2. This is not staleness (longstanding mechanic) nor account-type dependent. The location 'only' restriction is additionally wrong: Al Kharid mine (north of Al Kharid) is a documented beginner emote-clue location outside the four named towns. The unambiguous, dominant error is the step count.

### [high] C8: The sandwich lady is a game-wide random event, not a mechanic within beginner clue scrolls. She does not appear as a 'wave' or encounter step inside a clue. The guidance conflates her cosmetic outfit (a beginner clue reward) with an in-clue mechanic that does not exist. There is also no 'world-3' specificity to her appearance. Telling players to 'skip the world-3 sandwich lady wave' describes something that cannot happen during clue completion.

- **Data says:** No combat encounters - skip the world-3 sandwich lady wave by accepting whichever sandwich she offers first.
- **Wiki says (raw):** The Sandwich Lady is not a clue scroll mechanic--she's a random event that appears during gameplay. ... The Sandwich Lady's outfit can be obtained as a rare reward from beginner clue scrolls, but she herself doesn't appear within clue scroll encounters.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sandwich_lady
- **Suggested fix:** Remove the sandwich lady wave sentence entirely. The sandwich lady outfit is a reward from beginner caskets, not an in-clue encounter.
- **Skeptic receipt:** The sandwich lady is a random event. She appears anywhere to offer players a selection from her tray of food. ... Players can obtain the sandwich lady's outfit as a rare reward from beginner clue scroll treasure trails. ... There is no mention of world-specific mechanics or the Sandwich Lady appearing as a 'wave' or step within clue progression.
- **Skeptic reasoning:** The data step (line 9860) instructs players to 'skip the world-3 sandwich lady wave by accepting whichever sandwich she offers first.' The Sandwich lady is a game-wide RANDOM EVENT during normal gameplay, not a step/wave within a beginner clue scroll. Her outfit is a rare reward from completing beginner caskets (correctly listed separately at line 9868), but she does not appear as an in-clue encounter, and there is no 'world-3' specificity. The guidance conflates the cosmetic reward with a non-existent in-clue mechanic. The broader 'No combat encounters' framing for beginner clues is itself fine, but the sandwich-lady sentence describes something that cannot occur during clue completion.

## Revenants

### [medium] C3: No entrance exists at level 28 Wilderness. The closest entrance to the Bandit Camp route is at level 26 Wilderness. The guidance step should read 'level 26 Wilderness' not 'level 28'.

- **Data says:** the cave entrance at level 28 Wilderness
- **Wiki says (raw):** Level 17 Wilderness -- east of the Dark Warriors' Fortress ... Level 26 Wilderness -- north of the Bandit Camp ... Level 40 Wilderness -- south-east of the Lava Maze
- **Wiki URL:** https://oldschool.runescape.wiki/w/Revenant_Caves
- **Suggested fix:** Change 'level 28 Wilderness' to 'level 26 Wilderness' in guidanceSteps[1].description.
- **Skeptic receipt:** Level 26 Wilderness -- north of the Bandit Camp
- **Skeptic reasoning:** The data's Travel step (guidanceSteps[1]) says teleport to Bandit Camp then 'run north-east to the cave entrance at level 28 Wilderness'. The wiki lists exactly three entrances, and the one reachable from Bandit Camp is at level 26, not 28. There is no level-28 entrance. This is not the 'level 28-34' depth-range phrasing used in the Bank step (that is a general risk note); this step concretely names the entrance location, which is wrong. Refutation vectors (multi-source/variant/account-type/staleness) do not apply to a static geographic fact, and the wiki page is current. Note: severity is at most medium/low since the step also relies on coordinates (3073,3654) for completion, so navigation is not broken, only the descriptive text.

### [medium] C5: The bracelet reduces revenant damage by 75% -- it does not prevent or negate it. Describing it as 'prevents' is inaccurate since 25% of each hit still lands. The 'passive damage' framing is also imprecise; this applies to all revenant attack damage, not just a special passive.

- **Data says:** Bracelet of ethereum (prevents revenant passive damage)
- **Wiki says (raw):** Wearing the bracelet of ethereum no longer gives complete immunity of revenant attacks, now reducing incoming damage from them by 75%.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bracelet_of_ethereum
- **Suggested fix:** Change 'prevents revenant passive damage' to 'reduces revenant damage by 75%' in guidanceSteps[0].description (and similarly in guidanceSteps[2].description).
- **Skeptic receipt:** Wearing the bracelet of ethereum no longer gives complete immunity of revenant attacks, now reducing incoming damage from them by 75%.
- **Skeptic reasoning:** guidanceSteps[0] describes the bracelet as 'prevents revenant passive damage'. The wiki is explicit that since the Aug 6 2020 update it 'no longer gives complete immunity of revenant attacks, now reducing incoming damage from them by 75%'. 25% of each revenant hit still lands, so 'prevents' overstates the effect and could lead players to under-stock food/prayer. The 'passive damage' framing is also imprecise: the reduction applies to revenant attack damage generally and consumes a charge per attack, not a separate passive. No variant/account/staleness vector rescues this -- the 75% mechanic has been stable since 2020.

### [medium] C6: The bracelet negates nothing -- it reduces damage by 75%. Using 'negate' tells players they will take no damage from revenants, which is false and could lead to under-preparing food/prayer potions.

- **Data says:** Activate the Bracelet of ethereum to negate revenant passive damage
- **Wiki says (raw):** Wearing the bracelet of ethereum no longer gives complete immunity of revenant attacks, now reducing incoming damage from them by 75%.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bracelet_of_ethereum
- **Suggested fix:** Change 'negate revenant passive damage' to 'reduce revenant damage by 75%' in guidanceSteps[2].description.
- **Skeptic receipt:** When charged, the bracelet becomes untradeable and damage from revenants is reduced by 75%, with 1 charge being consumed per attack.
- **Skeptic reasoning:** guidanceSteps[2] says 'Activate the Bracelet of ethereum to negate revenant passive damage' and guidanceSteps[3] repeats 'The Bracelet of ethereum negates passive damage.' 'Negate' implies zero damage, which the wiki directly contradicts: it reduces by 75% with 1 charge consumed per attack. Same legitimate-mechanic check as C5 -- the bracelet has not given full immunity since 2020. This is a genuine inaccuracy that affects player preparation, not a misread of the data.

### [high] C7: Revenants use all three combat styles (melee, magic, and ranged), not just magic or missiles. The guidance omits melee entirely and incorrectly frames prayer choice as fixed per revenant type, when in fact revenants dynamically adapt to the player's weaknesses and prayer usage.

- **Data says:** Use Protect from Magic or Protect from Missiles based on the revenant type
- **Wiki says (raw):** Revenants use all three forms of combat, using a combat style that the player is weak against as their primary attacks, and will adapt to any changes the player does.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Revenant_Caves
- **Suggested fix:** Update guidanceSteps[3].description to note that revenants use all three combat styles and adapt to the player's weakness; advise Protect from Melee, Magic, or Missiles as appropriate to the situation rather than implying only two options exist.
- **Skeptic receipt:** Revenants use all three forms of combat, using a combat style that the player is weak against as their primary attacks, and will adapt to any changes the player does.
- **Skeptic reasoning:** guidanceSteps[3] says 'Use Protect from Magic or Protect from Missiles based on the revenant type.' The wiki states revenants use ALL THREE combat styles, dynamically picking the one the player is weakest to and adapting to changes. The guidance omits melee entirely and frames prayer choice as fixed per revenant type, when in fact the correct play is to pray against whatever the revenant is currently using and to avoid letting it adapt. This is a real, materially misleading combat instruction, not an account-type or variant nuance. Severity high is reasonable given it directly governs survival in deep Wilderness.

### [blocker] C8: The Revenant Caves are singles-plus combat, NOT multi-combat. This is a blocker because 'multi-combat' appears in both guidanceSteps[2] and guidanceSteps[3].description and would give players a fundamentally wrong understanding of the PvP and PvM mechanics in the area.

- **Data says:** the caves are deep Wilderness and multi-combat
- **Wiki says (raw):** it is exclusively a singles-plus combat area
- **Wiki URL:** https://oldschool.runescape.wiki/w/Revenant_Caves
- **Suggested fix:** Replace 'multi-combat' with 'singles-plus combat' in guidanceSteps[2].description and guidanceSteps[3].description.
- **Skeptic receipt:** it is an extremely dangerous cave that ranges from level 17 to 40 Wilderness and is exclusively a singles-plus combat area.
- **Skeptic reasoning:** guidanceSteps[2] and guidanceSteps[3] both call the caves 'multi-combat'. The wiki states it is 'exclusively a singles-plus combat area' -- meaning you fight one revenant at a time (singles) but another player may attack you while you are engaged (the '-plus' PvP exception). That is mechanically distinct from multi-combat, where multiple NPCs/players can all stack on a single target simultaneously. Calling it multi-combat gives a fundamentally wrong picture of both the PvM (no NPC stacking) and the safe-spotting/escape dynamics. No refutation vector applies: this is the current, exclusive area type per the live wiki. Strongest of the five; warrants the blocker severity.

## Pyrefiend

### [blocker] C5: The guidance recommends Protect from Magic, but the wiki recommends Protect from Melee. Pyrefiends use 'Magical melee' -- an attack style that is calculated against the player's magic defence stat, but is blocked by Protect from Melee (not Protect from Magic). Using the wrong prayer provides zero protection.

- **Data says:** Pyrefiends use magic attacks and should be protected against with Protect from Magic
- **Wiki says (raw):** As their attacks are magic based, it is recommended to wear armour with a high magic defence bonus, such as dragonhide. Players who intend to use melee and have 43 Prayer can flick the Protect from Melee prayer to avoid being damaged.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pyrefiend
- **Suggested fix:** Change the guidance to recommend Protect from Melee (requires 43 Prayer) to avoid damage, and note that dragonhide or ranged armour should be worn because accuracy is calculated against magic defence.
- **Skeptic receipt:** WIKI (oldschool.runescape.wiki/w/Pyrefiend): "Players who intend to use melee and have 43 Prayer can flick the Protect from Melee prayer to avoid being damaged." Attack style: "Magical melee ... utilizing magic-based combat." | DATA (drop_rates.json line 10410): "Kill Pyrefiends in the Fremennik Slayer Dungeon or Catacombs of Kourend (30 Slayer). Use Protect from Magic"
- **Skeptic reasoning:** Pyrefiends use 'Magical melee' -- a melee-class attack whose accuracy is rolled against the player's magic defence stat but which is BLOCKED by Protect from Melee, not Protect from Magic. This is a standard OSRS 'magic melee' mechanic: Protect from Magic provides zero protection against it. The wiki strategy section explicitly recommends Protect from Melee at 43 Prayer, and recommends dragonhide/ranged armour for the magic-defence roll. The drop_rates.json guidance step (line 10410) says 'Use Protect from Magic' -- the wrong prayer, giving zero protection. Refutation vectors do not apply: this is account-type-independent and not a variant/multi-source/staleness issue. The WebFetch summarizer's aside that 'Protect from Magic would theoretically be more appropriate' is incorrect editorializing that contradicts the quoted wiki text and the underlying mechanic; the wiki and the game both make Protect from Melee the blocking prayer. Confirmed blocker. Receipt embeds both the data text and the wiki text.

### [high] C3: The wiki lists five locations for Pyrefiends and Catacombs of Kourend is not among them. Directing players there would send them to the wrong location.

- **Data says:** Pyrefiends can be found in Catacombs of Kourend as an alternative to Fremennik Slayer Dungeon
- **Wiki says (raw):** Pyrefiends are Slayer monsters that require level 30 Slayer to kill. [Locations listed: Fremennik Slayer Cave, God Wars Dungeon, Isle of Souls, Sisterhood Sanctuary, Smoke Dungeon -- Catacombs of Kourend is not listed.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pyrefiend
- **Suggested fix:** Remove the Catacombs of Kourend reference. Valid alternative locations include the Smoke Dungeon (23 spawns) or Sisterhood Sanctuary (21 spawns).
- **Skeptic receipt:** WIKI (oldschool.runescape.wiki/w/Pyrefiend) locations: "Fremennik Slayer Cave, God Wars Dungeon, Isle of Souls, Sisterhood Sanctuary, Smoke Dungeon, Wilderness God Wars Dungeon" -- "'Catacombs of Kourend' is not mentioned as a Pyrefiend location on this page." | DATA (line 10410): "Kill Pyrefiends in the Fremennik Slayer Dungeon or Catacombs of Kourend (30 Slayer)."
- **Skeptic reasoning:** The wiki lists six Pyrefiend locations: Fremennik Slayer Cave, God Wars Dungeon, Isle of Souls, Sisterhood Sanctuary, Smoke Dungeon, and Wilderness God Wars Dungeon. Catacombs of Kourend is not among them (verified twice via independent WebFetch passes: 'Catacombs of Kourend is not mentioned as a Pyrefiend location on this page'). The drop_rates.json guidance step (line 10410) offers 'Catacombs of Kourend' as an alternative location, which would send players to a dungeon that does not contain Pyrefiends. Not a multi-source/variant/account-type nuance -- a flat wrong-location reference. Confirmed high. Note C6 is a near-duplicate of this finding citing the same defect.

### [high] C6: Pyrefiends do not spawn in the Catacombs of Kourend according to the wiki. This is a fabricated location reference.

- **Data says:** Pyrefiends can be found in Catacombs of Kourend
- **Wiki says (raw):** Pyrefiends are Slayer monsters that require level 30 Slayer to kill. [Locations listed: Fremennik Slayer Cave, God Wars Dungeon, Isle of Souls, Sisterhood Sanctuary, Smoke Dungeon -- Catacombs of Kourend is not listed.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pyrefiend
- **Suggested fix:** Remove the Catacombs of Kourend reference from the guidance step. Replace with a valid alternative location such as the Smoke Dungeon or Sisterhood Sanctuary if an alternative is needed.
- **Skeptic receipt:** WIKI (oldschool.runescape.wiki/w/Pyrefiend): "'Catacombs of Kourend' is not mentioned as a Pyrefiend location on this page." Full location list: "Fremennik Slayer Cave (13 spawns), God Wars Dungeon (4 spawns), Isle of Souls (10 spawns), Sisterhood Sanctuary (21 spawns), Smoke Dungeon (23 spawns), Wilderness God Wars Dungeon (2 spawns)." | DATA (line 10410): "... or Catacombs of Kourend (30 Slayer)."
- **Skeptic reasoning:** Same defect as C3: the drop_rates.json guidance step references Catacombs of Kourend as a Pyrefiend location, but the wiki's six listed locations do not include it (verified twice). Pyrefiends do not spawn in the Catacombs of Kourend. This is a real wrong-location reference, not legitimate game behavior, variant, or account-type nuance. Confirmed high. This finding is a duplicate of C3 (both target the single 'or Catacombs of Kourend' phrase at line 10410); a single edit removing the Catacombs reference resolves both -- they should not be logged as two separate fixes.

Tranche 2 total: 141 confirmed findings.
# Tranche 3: sources 71-110 (slayer monsters)

Note: the first tranche-3 run was invalidated by a spend-limit outage that killed the
skeptic stage mid-run; this is the full re-run (skeptic stage on a smaller model).

Run summary: 40 sources processed; 26 with confirmed findings; clean: Basilisk, Infernal Mage, Gryphon, Jelly, Turoth, Basilisk Knight, Dust Devil, Kurask, Spiritual Mage, Spiritual Mage (Zarosian), Abyssal Demon, Cave Kraken, Smoke Devil, Brimstone Chest.

## Terror Dog

### [high] C3: The fairy ring code is BIP, not BIQ. BIQ leads to the Kharidian Desert near the Kalphite Hive -- an entirely wrong region. A player following this guidance would teleport to the desert and find no path to Tarn's Lair.

- **Data says:** Fairy ring BIQ can be used to reach the location
- **Wiki says (raw):** By fairy ring BIP (50 Agility needed) and then walking south.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tarn%27s_Lair
- **Suggested fix:** Change BIQ to BIP in guidanceSteps[1].description.
- **Skeptic receipt:** Fairy rings wiki: 'BIP - Islands: South-west of Mort Myre Swamp, on the river Salve with an agility shortcut requiring level 50.' / 'BIQ - Located near the Kalphite Hive in the Kharidian Desert.' Tarn's Lair wiki: 'By fairy ring (50 Agility needed) and then walking south.'
- **Skeptic reasoning:** Two independent wiki sources confirm the correct fairy ring code is BIP, not BIQ. The Tarn's Lair wiki page states: 'By fairy ring (50 Agility needed) and then walking south.' -- the rendered code in that bracket is BIP as confirmed by the fairy rings page. The fairy rings page explicitly maps BIP to 'South-west of Mort Myre Swamp, on the river Salve' (the correct Morytania approach for Tarn's Lair) and BIQ to the Kalphite Hive in the Kharidian Desert. These are two entirely different rings in two different regions. No multi-source item variant, account-type, or staleness vector applies -- this is a plain wrong code in the guidance text. No recent wiki changes were found (zero edits to the Fairy ring page since 2026-01-01), confirming the current authoritative code is BIP.

## Brine Rat

### [medium] C3: The guidance adds 'near Rellekka lighthouse' as a locating landmark for the entrance, but the wiki does not associate the lighthouse with this entrance at all. The Lighthouse is north of the Barbarian Outpost (west of Rellekka), a separate area entirely. The wiki identifies the windswept tree's position as 'east of Olaf Hradson and south-east of the Rellekka Hunter area' -- no lighthouse mentioned. The fabricated landmark could mislead players toward the wrong area.

- **Data says:** Dig with a spade south of the Windswept Tree near Rellekka lighthouse to enter
- **Wiki says (raw):** dig with a spade south of the windswept tree, which is on a plateau east of Olaf Hradson and south-east of the Rellekka Hunter area
- **Wiki URL:** https://oldschool.runescape.wiki/w/Brine_Rat_Cavern
- **Suggested fix:** Remove 'near Rellekka lighthouse' from the description. Use the wiki-accurate landmark instead: 'Dig with a spade south of the Windswept Tree (south-east of the Rellekka Hunter area) to enter.'
- **Skeptic receipt:** Brine Rat Cavern wiki: 'The Brine Rat Cavern is in the north-eastern region of the Fremennik Province... Players can enter the dungeon by digging with a spade south of the windswept tree, which is on a plateau east of Olaf Hradson and south-east of the Rellekka Hunter area.' Lighthouse wiki: 'The Lighthouse is an area north of the Barbarian Outpost.'
- **Skeptic reasoning:** The guidance text says 'near Rellekka lighthouse' as a locating landmark for the Brine Rat Cavern entrance. The wiki confirms the cavern entrance is in the north-eastern Fremennik Province, east of Olaf Hradson and south-east of the Rellekka Hunter area. The Lighthouse wiki page explicitly places it north of the Barbarian Outpost -- which is on the western side of the Fremennik Province, a completely different area. The two locations are geographically separated by most of the Fremennik Province. Pointing a player 'near the Rellekka lighthouse' would direct them to the wrong region entirely. No refutation vector applies: this is not a multi-source item issue, not a variant, not account-type-specific, and not a staleness issue -- the geography is stable game content.

## Frost Nagua

### [blocker] C4: The guidance tells players to use Protect from Melee, but Frost Nagua's attack style is Magic. The wiki explicitly states Protect from Magic negates all damage from their basic attacks. Following this guidance means taking full magic damage every kill.

- **Data says:** Kill Frost Nagua. Protect from Melee; fire spells deal bonus damage.
- **Wiki says (raw):** Protect from Magic will negate all damage from their basic attacks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Frost_Nagua
- **Suggested fix:** Replace 'Protect from Melee' with 'Protect from Magic' in guidanceSteps[3].description.
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Frost_Nagua: '1) Attack Style/Type: "[Magic]" - The monster uses magic-based attacks.' '4) Recommended Prayer: "Protect from Magic will negate all damage from their basic attacks."'
- **Skeptic reasoning:** The data at drop_rates.json line 10804 says 'Protect from Melee; fire spells deal bonus damage.' The wiki page for Frost Nagua explicitly states the monster uses Magic-based attacks and 'Protect from Magic will negate all damage from their basic attacks.' WebFetch of https://oldschool.runescape.wiki/w/Frost_Nagua confirms attack style is '[Magic]' and recommended prayer is 'Protect from Magic.' Telling players to use Protect from Melee against a Magic-attacking monster means they take full magic damage every kill.

### [high] C2: The guidance classifies Frost Nagua as 'spectral undead', but the wiki lists only 'Attribute: Spectral'. There is no undead attribute. Spectral and undead are separate monster attributes in OSRS; Salve amulet (ei) only provides its bonus against undead monsters, not spectral ones.

- **Data says:** Spectral undead so Salve amulet (ei) outperforms Slayer helmet on task
- **Wiki says (raw):** Attribute: Spectral
- **Wiki URL:** https://oldschool.runescape.wiki/w/Frost_Nagua
- **Suggested fix:** Remove 'undead' from the classification -- Frost Nagua is 'spectral' only. Update both guidanceSteps[0] and guidanceSteps[3] to remove 'undead' from the description.
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Frost_Nagua: '2) Monster Attributes: "[Spectral]" - It is classified as a spectral creature.' No undead attribute is listed.
- **Skeptic reasoning:** The data at line 10776 says 'spectral undead so Salve amulet (ei) outperforms Slayer helmet on task.' WebFetch of the Frost Nagua wiki page confirms the only listed attribute is 'Spectral' -- there is no 'undead' attribute. Spectral and undead are distinct OSRS monster attributes. The 'undead' classification in the data description is factually wrong.

### [blocker] C3: Because Frost Nagua has only the Spectral attribute (not undead), the Salve amulet (ei) does not grant its undead damage/accuracy bonus on them. Advising players to equip Salve amulet (ei) over their Slayer helmet while on task is materially wrong -- Slayer helmet's on-task bonus would apply, while Salve amulet's undead bonus would not.

- **Data says:** spectral undead so Salve amulet (ei) outperforms Slayer helmet on task
- **Wiki says (raw):** Attribute: Spectral
- **Wiki URL:** https://oldschool.runescape.wiki/w/Frost_Nagua
- **Suggested fix:** Remove the Salve amulet (ei) recommendation from guidanceSteps[0] and guidanceSteps[3]. On task, the Slayer helmet is the correct choice. Off task, no special amulet advantage applies from Salve.
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Salve_amulet_(ei): 'the Salve amulet(ei) specifically works on monsters with the undead attribute. The exact text states: "Increases melee, ranged and magic damage & accuracy by 20% against the undead." There is no mention of the amulet working on spectral monsters or any other attributes--it is exclusively designed for undead enemies.'
- **Skeptic reasoning:** Frost Nagua has only the Spectral attribute, not undead. WebFetch of https://oldschool.runescape.wiki/w/Salve_amulet_(ei) confirms the amulet works exclusively on undead monsters: 'Increases melee, ranged and magic damage & accuracy by 20% against the undead.' Since Frost Nagua is not undead, the Salve amulet (ei) provides no bonus against them. The data at lines 10776 and 10804 recommending Salve (ei) over the Slayer helmet on task is materially incorrect -- the Slayer helmet's on-task bonus applies and Salve (ei)'s undead bonus does not.

### [medium] C1: The guidance recommends melee crush specifically, but the wiki's defensive stats show Frost Nagua has +10 crush defence, +50 slash defence, and +0 stab defence. If melee is used, stab is the lowest-resistance style, not crush. There is no crush weakness -- recommending crush over stab misguides melee users.

- **Data says:** Fire-spell magic or melee crush gear works best
- **Wiki says (raw):** Stab: "+0", Slash: "+50", Crush: "+10"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Frost_Nagua
- **Suggested fix:** Replace 'melee crush gear' with 'melee stab gear' in guidanceSteps[0].description to match the wiki's defensive stats (crush +10, stab +0).
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Frost_Nagua: '3) Defensive Stats (exact values): Stab: "+0" | Slash: "+50" | Crush: "+10" | Magic: "+60" | Ranged (Light/Standard/Heavy): "+200"'
- **Skeptic reasoning:** The data at line 10776 says 'melee crush gear works best.' WebFetch of the Frost Nagua wiki page returns defensive stats: Stab +0, Slash +50, Crush +10. Stab has zero defence (+0) while crush has +10 resistance. If melee is used, stab is strictly the lowest-resistance style. The guidance recommending crush over stab is incorrect for melee users.

### [medium] C9: The Pendant of ates teleports to the Twilight Temple (near the lift), not to the Ruins of Tapoyauik directly. The Ruins are beneath the Temple and accessed via that lift. The travelTip and guidanceSteps[1] description misstate the teleport destination as 'Ruins of Tapoyauik' rather than 'Twilight Temple'.

- **Data says:** Pendant of ates -> Ruins of Tapoyauik (requires The Heart of Darkness)
- **Wiki says (raw):** The pendant of ates can teleport directly to the Twilight Temple, near the lift.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ruins_of_Tapoyauik
- **Suggested fix:** Update the travelTip and guidanceSteps[1] description to say the Pendant of ates teleports to the Twilight Temple (near the lift), from which you descend into the Ruins of Tapoyauik.
- **Skeptic receipt:** WebFetch https://oldschool.runescape.wiki/w/Ruins_of_Tapoyauik: '"The pendant of ates can teleport directly to the Twilight Temple, near the lift." From there, you access the dungeon via the lift platform.'
- **Skeptic reasoning:** The data at line 10785 says 'Use the Pendant of ates to teleport directly to the Ruins of Tapoyauik.' WebFetch of https://oldschool.runescape.wiki/w/Ruins_of_Tapoyauik confirms the pendant lands at the Twilight Temple, not inside the Ruins: 'The pendant of ates can teleport directly to the Twilight Temple, near the lift.' The Ruins are a separate underground area accessed via the lift after the teleport. Note that drop_rates.json line 10795 already has a distinct step 'Enter the Ruins of Tapoyauik beneath the Twilight Temple,' confirming the guidance architecture already treats the Temple arrival and the Ruins descent as separate steps -- making the 'teleport directly to the Ruins' label on line 10785 a factual mislabel of the teleport tile.

## Sulphur Nagua

### [blocker] C1: Sulphur Nagua are spectral but NOT undead. The Salve amulet (ei) has zero effect on them. The guidance incorrectly labels them 'spectral undead' and recommends Salve amulet over Slayer helmet -- the Slayer helmet is actually superior on task. This is an inverted combat meta that actively misleads the player.

- **Data says:** spectral undead so Salve amulet (ei) outperforms Slayer helmet on task
- **Wiki says (raw):** They are spectral creatures but not undead, meaning the Ectoplasmator will provide prayer experience for defeating them, but the Salve amulet and its variants will not affect them.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sulphur_Nagua
- **Suggested fix:** Replace 'spectral undead so Salve amulet (ei) outperforms Slayer helmet on task' with 'spectral but NOT undead -- Salve amulet has no effect; use Slayer helmet on task'.
- **Skeptic receipt:** "They are spectral creatures but not undead, meaning the Ectoplasmator will provide prayer experience for defeating them, but the Salve amulet and its variants will not affect them." -- https://oldschool.runescape.wiki/w/Sulphur_Nagua (fetched 2026-06-12, zero wiki changes since 2026-01-01 per wiki_updates)
- **Skeptic reasoning:** The data at drop_rates.json lines 10852 and 10880 calls Sulphur Nagua 'spectral undead' and states 'Salve amulet (ei) outperforms Slayer helmet on task' in both the bank-prep and combat guidance steps. The wiki directly refutes both sub-claims: the creatures are spectral but explicitly NOT undead, and the Salve amulet and all its variants have zero effect on them. The Slayer helmet is therefore unconditionally superior on task. This is a live inverted combat meta actively misleading players into equipping a dead-weight item.

### [medium] C4: The claim 'no special attacks' is false. Sulphur Nagua have a prayer-draining special attack triggered every five attacks. Players who step away in time can avoid the drain, but the attack exists and the guidance misstates this.

- **Data says:** No phase mechanics or special attacks
- **Wiki says (raw):** After five attacks they will use a special attack in which they raise their weapon, and if the player remains within their melee range (including diagonal spaces) after three ticks, they will have 5 prayer points drained.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sulphur_Nagua
- **Suggested fix:** Replace 'No phase mechanics or special attacks' with 'No phase mechanics; special attack every 5 hits drains 5 prayer points if you remain in melee range -- step away to avoid it'.
- **Skeptic receipt:** "After five attacks they will use a special attack in which they raise their weapon, and if the player remains within their melee range (including diagonal spaces) after three ticks, they will have 5 prayer points drained." -- https://oldschool.runescape.wiki/w/Sulphur_Nagua (fetched 2026-06-12)
- **Skeptic reasoning:** The combat guidance step at drop_rates.json line 10880 explicitly states 'No phase mechanics or special attacks'. The wiki documents a named special attack that triggers every five attacks: the creature raises its weapon, and if the player remains in melee range (including diagonal tiles) for three ticks afterward, 5 prayer points are drained. The attack is avoidable by stepping away but it unambiguously exists, making the 'no special attacks' claim false. No account-type or progress dependency applies -- the mechanic is present for all players.

### [high] C8: The plugin only gates on Slayer 48 but the wiki states partial completion of the quest Perilous Moons is required to access Neypotzli at all. A player who meets only the Slayer level will be unable to enter the area. This is a missing hard gate.

- **Data says:** requirements.skills[0]: SLAYER level 48
- **Wiki says (raw):** Partial completion of Perilous Moons is required for access.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sulphur_Nagua
- **Suggested fix:** Add a quest requirement for 'Perilous Moons' (partial completion) to the requirements block alongside the Slayer 48 skill requirement.
- **Skeptic receipt:** "Sulphur nagua are slayer monsters that require level 48 Slayer to kill. They are found in the Ancient Prison within Neypotzli, accessed from the south-west entrance of the antechamber. Partial completion of Perilous Moons is required for access." -- https://oldschool.runescape.wiki/w/Sulphur_Nagua (fetched 2026-06-12)
- **Skeptic reasoning:** The requirements block at drop_rates.json lines 10829-10835 contains only SLAYER level 48. The wiki states that partial completion of Perilous Moons is a separate hard access gate for Neypotzli, where Sulphur Nagua are exclusively found. A player who has Slayer 48 but has not partially completed Perilous Moons cannot enter the area at all. Perilous Moons is a standard members quest with no ironman or account-type carve-out, so this gate is universal -- refutation vector 3 does not apply.

## Earthen Nagua

### [blocker] C1: The salve amulet (ei) recommendation is predicated on Earthen Nagua being undead. The wiki classifies them as 'Spectral creature', not undead. If the salve does not trigger on spectral creatures, this guidance leads players to use a suboptimal amulet slot on task.

- **Data says:** they are spectral undead so Salve amulet (ei) outperforms Slayer helmet on task
- **Wiki says (raw):** Type: Spectral creature
- **Wiki URL:** https://oldschool.runescape.wiki/w/Earthen_Nagua
- **Suggested fix:** Confirm in-game or via wiki whether 'Spectral creature' type triggers salve amulet bonuses. If not, replace with Slayer helmet on task recommendation.
- **Skeptic receipt:** Earthen Nagua wiki: attribute = 'Spectral' only -- no Undead attribute listed. Salve amulet (ei) wiki: 'Increases melee, ranged and magic damage & accuracy by 20% against the undead.' Spectral creature wiki: 'Most spectral creatures also classify as undead (attribute), and are therefore weak to the effects of the salve amulet and its upgraded variants, however, this does not apply to all spectral creatures.'
- **Skeptic reasoning:** The data at line 10928 calls Earthen Nagua 'spectral undead' and recommends Salve amulet (ei) outperforms Slayer helmet on task. Two wiki receipts refute the underlying premise: (a) the Earthen Nagua wiki page lists only 'Spectral' as the monster's attribute -- 'Undead' is absent from the infobox; (b) the Salve amulet (ei) wiki states its bonus applies only to undead monsters ('Increases melee, ranged and magic damage & accuracy by 20% against the undead'); (c) the Spectral creature page explicitly warns 'this does not apply to all spectral creatures' re salve. Earthen Nagua is spectral but not undead, so the salve does not apply and the on-task recommendation to use it over the Slayer helmet is wrong.

### [blocker] C2: Earthen Nagua are classified as 'Spectral creature' on the wiki, not 'spectral undead'. Salve amulet bonuses apply to undead monsters; if Earthen Nagua are not undead, the salve amulet (ei) recommendation is wrong and following it would result in worse DPS than using a Slayer helmet on task.

- **Data says:** they are spectral undead so Salve amulet (ei) outperforms Slayer helmet on task
- **Wiki says (raw):** Type: Spectral creature
- **Wiki URL:** https://oldschool.runescape.wiki/w/Earthen_Nagua
- **Suggested fix:** Remove 'spectral undead' and replace with 'spectral creature'. Verify whether Salve amulet (ei) actually applies to spectral creatures before keeping C1's recommendation; if it does not, correct C1 to recommend Slayer helmet on task instead.
- **Skeptic receipt:** Earthen Nagua wiki infobox: attribute listed is 'Spectral' -- Undead attribute is absent. Spectral creature wiki: Earthen Nagua (Normal, Reinforced, Weakened variants) all appear in the spectral creature table without any undead designation.
- **Skeptic reasoning:** This is the same underlying fact as C1 -- the data labels Earthen Nagua 'spectral undead' but the wiki confirms the only attribute tag is 'Spectral'. C2 specifically targets the label 'spectral undead' in the guidance text (line 10928 and 10956). That label is factually wrong and causes the incorrect salve recommendation. Both the type label and the salve guidance are errors traceable to the same false premise.

### [high] C4: The guidance says to travel to Cam Torum via quetzal whistle and then enter Tonali Cavern 'beneath the city'. The wiki lists the closest Quetzal Transport System stops as Tal Teklan or Kastori -- not Cam Torum. Directing players to Cam Torum adds unnecessary travel distance.

- **Data says:** Use the quetzal whistle to travel to Cam Torum in Varlamore, then enter the Tonali Cavern beneath the city
- **Wiki says (raw):** Mokhaiotl waystones can be used to teleport directly inside the Ruins of Mokhaiotl, then run south to the cavern. The pendant of ates can be used to teleport to Kastori just east of the cavern's entrances. The Quetzal Transport System can be used to land at Tal Teklan or Kastori, both near the cavern's entrances.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tonali_Cavern
- **Suggested fix:** Update the travel step to use the Kastori or Tal Teklan quetzal stop, which the wiki confirms are 'near the cavern's entrances', rather than routing through Cam Torum.
- **Skeptic receipt:** Tonali Cavern wiki: 'The Quetzal Transport System can be used to land at Tal Teklan or Kastori, both near the cavern's entrances.' Cam Torum is not mentioned as a routing option for Tonali Cavern.
- **Skeptic reasoning:** Line 10937 instructs players to 'Use the quetzal whistle to travel to Cam Torum in Varlamore, then enter the Tonali Cavern beneath the city.' The Tonali Cavern wiki page does not list Cam Torum as a Quetzal Transport System stop for reaching this dungeon. The two listed stops are Tal Teklan and Kastori, described as 'both near the cavern's entrances'. Cam Torum is a separate Varlamore location and routing through it adds unnecessary travel distance compared to Kastori, which is east of the cavern entrances.

### [high] C5: The data claims Tonali Cavern is beneath Cam Torum. The wiki states it is beneath the Crypt of Tonali in the Tlati Rainforest -- a different location entirely. This misdirects players to look under Cam Torum rather than the correct area.

- **Data says:** locationDescription: "Tonali Cavern, beneath Cam Torum, Varlamore" / guidanceSteps[1].description: "enter the Tonali Cavern beneath the city"
- **Wiki says (raw):** The Tonali Cavern is a dungeon located beneath the Crypt of Tonali in the Tlati Rainforest region of Varlamore.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tonali_Cavern
- **Suggested fix:** Update locationDescription to 'Tonali Cavern, beneath the Crypt of Tonali, Tlati Rainforest, Varlamore' and fix the guidance step description accordingly.
- **Skeptic receipt:** Tonali Cavern wiki: 'The Tonali Cavern is a dungeon located beneath the Crypt of Tonali in the Tlati Rainforest region of Varlamore.' Cam Torum is not mentioned in the location description.
- **Skeptic reasoning:** Line 10922 sets locationDescription to 'Tonali Cavern, beneath Cam Torum, Varlamore'. The wiki is unambiguous: Tonali Cavern is beneath the Crypt of Tonali in the Tlati Rainforest, not beneath Cam Torum. These are distinct locations in Varlamore. The misdescription would direct players to the wrong area of the map.

### [blocker] C7: The data says Earthen Nagua are 'weak to slash'. The wiki states their primary weakness is Air (15%), and that below 60% HP they become MORE resistant to slash (not weaker). Using slash throughout the fight is suboptimal and becomes worse in the second phase.

- **Data says:** spectral undead weak to slash
- **Wiki says (raw):** Air: 15% weakness ... When an earthen nagua is damaged after falling below 60% health, they enter a reinforced form, shifting their defences to be more resistant to slash while being weaker to crush.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Earthen_Nagua
- **Suggested fix:** Correct the weakness to Air (15%). Add a note that below 60% HP the nagua shifts to being weak to crush, and that a crush special attack can shatter its armour to 0 melee defence.
- **Skeptic receipt:** Earthen Nagua wiki: Air elemental weakness 15%. Phase mechanic: 'When an earthen nagua is damaged after falling below 60% health, they enter a reinforced form, shifting their defences to be more resistant to slash while being weaker to crush weapons.'
- **Skeptic reasoning:** Line 10956 states 'spectral undead weak to slash.' The wiki establishes two facts that contradict this: (1) the monster's elemental weakness is Air at 15%, not slash; (2) in the reinforced phase (below 60% HP) the nagua explicitly becomes MORE resistant to slash -- 'shifting their defences to be more resistant to slash while being weaker to crush weapons.' Calling slash the weakness is wrong in both phases; Air is the true primary weakness and crush becomes relevant in phase 2.

### [blocker] C8: The data claims there are no phase mechanics. The wiki explicitly describes a phase transition at 60% HP where the nagua enters a 'reinforced form' with shifted defences. Ignoring this mechanic leads players to continue using slash when they should switch to crush.

- **Data says:** No phase mechanics or special attacks
- **Wiki says (raw):** When an earthen nagua is damaged after falling below 60% health, they enter a reinforced form, shifting their defences to be more resistant to slash while being weaker to crush.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Earthen_Nagua
- **Suggested fix:** Remove the 'No phase mechanics' claim. Add a note that below 60% HP the nagua reinforces, shifting weakness from slash to crush.
- **Skeptic receipt:** Earthen Nagua wiki: 'When an earthen nagua is damaged after falling below 60% health, they enter a reinforced form, shifting their defences to be more resistant to slash while being weaker to crush weapons.'
- **Skeptic reasoning:** Line 10956 states 'No phase mechanics or special attacks.' The wiki directly contradicts 'No phase mechanics': at 60% HP the nagua transitions to a reinforced form with shifted defences. This is a meaningful combat phase change that affects the correct weapon style to use. The claim is factually wrong.

## Bloodveld

### [high] C6: The wiki shows Bloodvelds have equal (+0) defence against all attack styles including ranged, and explicitly lists 'No elemental weakness'. There is no ranged weakness. The wiki instead emphasises that players should use armour with high Magic defence (such as dragonhide) because the Bloodveld's attack accuracy is checked against the player's magic defence, not melee defence. Claiming a ranged weakness is factually wrong and will mislead players into thinking ranged is the preferred combat style when the wiki implies magic-defence gear (typically ranged/hybrid) matters, but not because of a ranged weakness on the monster.

- **Data says:** Bloodvelds are weak to ranged
- **Wiki says (raw):** Ranged: +0 [defence bonus in the combat stats table -- all defence bonuses (stab, slash, crush, magic, ranged) are +0; the page states 'No elemental weakness']
- **Wiki URL:** https://oldschool.runescape.wiki/w/Bloodveld
- **Suggested fix:** Remove the 'weak to ranged' claim. Bloodvelds have no specific weakness. The correct guidance is to wear armour with high Magic defence (e.g. dragonhide) to reduce the accuracy of their magical-melee attacks, not to use ranged weapons because of a combat triangle weakness.
- **Skeptic receipt:** WebFetch (Bloodveld wiki): 'All defensive bonuses are +0 across stab, slash, crush, magic, and ranged categories. The creature has No elemental weakness.' and 'it is the player's magic defence (rather than their melee defence) that is used to determine the accuracy of the bloodveld's attack.' WebFetch (Mutated Bloodveld wiki): 'All defensive stats are listed as +0: Stab, Slash, Crush bonuses: +0 / Magic defence: +0 / Ranged defence (all types): +0 / No elemental weaknesses are noted.'
- **Skeptic reasoning:** The wiki explicitly states all Bloodveld defensive bonuses are +0 across every attack style including ranged, and lists 'No elemental weakness'. The Bloodveld's attack is magical melee checked against the player's magic defence stat -- this informs gear choice (wear dragonhide for magic defence), but it is not a weakness of the Bloodveld to ranged attacks. Claiming a ranged weakness is a factual error that would mislead players about the combat triangle. The wiki receipt directly contradicts 'weak to ranged'.

## Warped Creature

### [blocker] C3: The gate is wrong. Warped Creatures require completion of The Path of Glouphrie, not Song of the Elves. Song of the Elves unlocks Prifddinas. A player following this guidance would grind a completely unrelated grandmaster quest (Song of the Elves) and still be unable to access Warped Creatures.

- **Data says:** Warped Creatures require completion of Song of the Elves
- **Wiki says (raw):** Ability to unlock Warped creatures as a Slayer task which drop the warped sceptre [unlocked by completing The Path of Glouphrie]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Path_of_Glouphrie
- **Suggested fix:** Replace 'Song of the Elves' with 'The Path of Glouphrie' as the quest gate in guidanceSteps[1].description.
- **Skeptic receipt:** wiki_lookup('Slayer task/Warped creatures'): '|otherreq = The Path of Glouphrie' ... 'Warped creatures are found exclusively in the Poison Waste Dungeon, requiring completion of The Path of Glouphrie.'
- **Skeptic reasoning:** The plugin's guidanceSteps description text reads 'Requires completion of Song of the Elves'. The OSRS Wiki Slayer task page for Warped creatures has the infobox field '|otherreq = The Path of Glouphrie' and the body reads 'Warped creatures are found exclusively in the Poison Waste Dungeon, requiring completion of The Path of Glouphrie.' Song of the Elves is the grandmaster elf-city quest that unlocks Prifddinas -- it has no connection to Warped Creatures or the Poison Waste Dungeon. The two quests gate entirely different content. A player directed to complete Song of the Elves would invest a grandmaster quest and still be unable to access Warped Creatures.

### [blocker] C4: The location is entirely wrong. Warped Creatures are found exclusively in the Poison Waste Dungeon in Tirannwn, not beneath Lumbridge Swamp. There is no 'Warped area beneath Lumbridge Swamp' in OSRS -- this appears to be a fabricated or confused location (possibly a conflation with Warped Jellies in the Catacombs of Kourend, or with the Lumbridge Swamp Caves).

- **Data says:** Warped Creatures are in the Warped area beneath Lumbridge Swamp
- **Wiki says (raw):** Warped creatures are found exclusively in the Poison Waste Dungeon
- **Wiki URL:** https://oldschool.runescape.wiki/w/Warped_Creature
- **Suggested fix:** Replace the location description with 'Poison Waste Dungeon, Tirannwn' consistent with C1 and the wiki.
- **Skeptic receipt:** wiki_lookup('Slayer task/Warped creatures'): 'Warped Tortoise ... Locations: Poison Waste Dungeon | Warped Terrorbird ... Locations: Poison Waste Dungeon | Mutated Tortoise ... Locations: Poison Waste Dungeon'. wiki_lookup('Poison Waste Dungeon'): 'The dungeon is in a canyon east of the Poison Waste. It is technically in Tirannwn...' Plugin data field: locationDescription = 'Poison Waste Dungeon, Tirannwn' (correct); guidanceSteps description = 'Warped area beneath Lumbridge Swamp' (wrong).
- **Skeptic reasoning:** The plugin's guidanceSteps description text reads 'Kill Warped Creatures in the Warped area beneath Lumbridge Swamp.' No location called 'Warped area beneath Lumbridge Swamp' exists in OSRS. The wiki Slayer task page, the Poison Waste Dungeon article, and the monster tables all place these creatures exclusively in the Poison Waste Dungeon in Tirannwn. The plugin's own top-level locationDescription field ('Poison Waste Dungeon, Tirannwn') is correct, confirming this is an isolated error in the guidance step text only.

### [high] C2: No item called a 'Tirannwn quiver' exists in OSRS. The wiki lists spirit trees (fastest), grapple via fairy ring BKP, and walking from Castle Wars as the travel options to the Poison Waste Dungeon. Players following this travelTip would search for a non-existent item.

- **Data says:** Travel to Warped Creatures can be done via Tirannwn quiver to Poison Waste
- **Wiki says (raw):** The entrance to the dungeon is west of the spirit tree, along the edge of the poison waste. [Travel methods listed:] spirit trees (fastest); a crossbow and mithril grapple to cross the river; fairy ring BKP; walking from Castle Wars.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Poison_Waste_Dungeon
- **Suggested fix:** Replace the travelTip with an accurate travel method, e.g. 'Use the spirit tree network to reach the Poison Waste spirit tree (unlocked by The Path of Glouphrie), then walk west to the dungeon entrance.'
- **Skeptic receipt:** WebFetch('https://oldschool.runescape.wiki/w/Special:Search?search=Tirannwn+quiver'): 'no item called Tirannwn quiver exists in OSRS ... The wiki even prompts users to Create the page Tirannwn quiver since it doesn't exist.' wiki_lookup('Poison Waste Dungeon'): 'The dungeon can be accessed by spirit trees (fastest) or by using a crossbow and mithril grapple to cross the river. The grapple location can be accessed by the fairy ring BKP ... or by walking from Castle Wars.' Western Provinces Diary wiki: reward is 'Western banner' with teleport to Piscatoris Fishing Colony -- no Poison Waste teleport at any tier.
- **Skeptic reasoning:** The plugin's travelTip reads 'Tirannwn quiver -> Poison Waste'. No item named 'Tirannwn quiver' exists in OSRS: the RuneLite ItemID constants return zero matches for TIRANNWN_QUIVER, the wiki search returns no page for the item and prompts to create it, and the Western Provinces Diary reward (the only Tirannwn-region diary) is the Western banner, which teleports to Piscatoris Fishing Colony, not the Poison Waste. The Poison Waste Dungeon article lists only two travel methods -- spirit trees (fastest) and crossbow+mithril grapple via fairy ring BKP or walking from Castle Wars -- with no quiver mentioned. Players following this travelTip would search for a non-existent item.

## Cave Horror

### [high] C3: The Cave Horror special attack deals HP damage (10% of base Hitpoints), not a stat drain. The wiki makes no mention of any stat drain mechanic. The guidance description is factually wrong about the type of effect.

- **Data says:** Cave Horrors drain player stats during combat
- **Wiki says (raw):** special attack which always deals damage equal to 10% of the player's base Hitpoints level
- **Wiki URL:** https://oldschool.runescape.wiki/w/Cave_Horror
- **Suggested fix:** Update guidanceSteps[2].description to describe the special attack as dealing damage equal to 10% of the player's base Hitpoints level, not a stat drain.
- **Skeptic receipt:** Cave Horror wiki: "The cave horrors have a special attack which always deals damage equal to 10% of the player's base Hitpoints level, rounded down" -- no stat drain mechanic is mentioned anywhere on the page.
- **Skeptic reasoning:** The Cave Horror guidance at guidanceSteps[2].description (drop_rates.json line 11390) reads: 'Kill Cave Horrors in the Mos Le Harmless cave. Requires witchwood icon necklace equipped to prevent stat drain (58 Slayer)'. The wiki contains no stat drain mechanic for Cave Horrors. The special attack is purely HP-damage based. The label 'stat drain' is factually wrong and would mislead players about the nature of the threat.

### [high] C4: Two errors: (1) there is no stat drain to prevent -- the special attack deals HP damage; (2) the witchwood icon does not prevent the damage but converts the guaranteed-damage scream into a regular attack roll that can be defended against normally. Telling players it 'prevents stat drain' misdescribes both the mechanic and the icon's effect.

- **Data says:** A witchwood icon necklace must be equipped to prevent stat drain from Cave Horrors
- **Wiki says (raw):** Wearing the icon does not negate all damage from the scream, it instead causes the scream attack to roll as a regular attack.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Witchwood_icon
- **Suggested fix:** Update guidanceSteps[2].description to say the witchwood icon converts the Cave Horror's guaranteed-damage scream attack into a regular attack roll, rather than 'preventing stat drain'.
- **Skeptic receipt:** Witchwood icon wiki: "Wearing the icon does not negate all damage from the scream, it instead causes the scream attack to roll as a regular attack." Cave Horror wiki: "This special attack can be partially avoided by wearing a witchwood icon, OR completely nullified by having Protect from Melee activated" -- partial avoidance via regular attack roll, not prevention of a stat drain.
- **Skeptic reasoning:** The guidance says the witchwood icon 'prevent[s] stat drain'. Both halves of this are wrong. First, there is no stat drain -- the scream deals HP damage (10% of base Hitpoints). Second, the witchwood icon does not prevent the effect; it converts the guaranteed-damage scream into a regular attack roll that can be blocked or missed. Describing it as preventing a stat drain misdescribes both the Cave Horror's mechanic and the icon's actual function.

## Aberrant Spectre

### [blocker] C5: The Ranging Guild is in Kandarin (between Ardougne and Seers' Village). The Slayer Tower is near Canifis in Morytania -- a completely different region of the game world. Running south-east from the Ranging Guild leads toward East Ardougne, not anywhere near the Slayer Tower. A player following this guidance would travel in entirely the wrong direction.

- **Data says:** From Ranging Guild, running south-east along the road leads to Slayer Tower entrance
- **Wiki says (raw):** The Ranging Guild is 'East of Hemenster, between Ardougne and Seers' Village.' Adjacent locations: North: Seers' Village, South: East Ardougne, East: Sorcerer's Tower, West: Fishing Guild.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ranging_Guild
- **Suggested fix:** Remove this alternative entirely, or replace with a correct route to Slayer Tower (e.g. Fairy ring CKS lands just south of the tower, or slayer ring teleports directly to the tower entrance).
- **Skeptic receipt:** wiki_lookup Ranging Guild: 'Location: East of Hemenster, between Ardougne and Seers' Village ... leagueRegion = Kandarin'. wiki_lookup Slayer Tower: 'The tower is located north-west of Canifis ... Location: Morytania'. These are different regions; no road between Ranging Guild and Slayer Tower exists.
- **Skeptic reasoning:** The Ranging Guild is definitively in Kandarin (between Ardougne and Seers' Village). The Slayer Tower is in Morytania, north-west of Canifis. These are entirely separate regions with no road connecting them. Running south-east from the Ranging Guild leads toward East Ardougne, not toward Morytania or the Slayer Tower. Any guidance step describing this route would send a player in completely the wrong direction.

## Wyrm

### [high] C1: Cerberus boots are not listed anywhere on the wiki as valid heat protection for the Karuulm Slayer Dungeon. The three valid options are boots of stone, boots of brimstone, and granite boots. A player following this guidance who equips Cerberus boots would take continuous heat damage.

- **Data says:** Boots of stone are mandatory to prevent passive heat damage in the Karuulm dungeon; granite boots or Cerberus boots are acceptable alternatives
- **Wiki says (raw):** players who wish to explore further beyond the entrance chamber must wear the boots of stone, boots of brimstone or granite boots to protect themselves from the extreme heat of the dungeon floor.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon
- **Suggested fix:** Replace 'Cerberus boots' with 'boots of brimstone' to match the three wiki-confirmed alternatives: boots of stone, boots of brimstone, and granite boots.
- **Skeptic receipt:** Karuulm Slayer Dungeon wiki (wiki_lookup): 'players who wish to explore further beyond the entrance chamber must wear the boots of stone, boots of brimstone or granite boots to protect themselves from the extreme heat of the dungeon floor.' Boots of stone wiki (wiki_lookup): 'Boots of stone can be used on a drake's claw to upgrade them into boots of brimstone, which have increased stats and can also be worn as an alternative within the Karuulm Slayer Dungeon, along with granite boots.' Cerberus boots wiki_lookup: '#REDIRECT Cerberus#Uniques' -- no such footwear item exists.
- **Skeptic reasoning:** The guidance step at line 11602 of drop_rates.json names 'Cerberus boots' as a valid heat-protection alternative for the Karuulm Slayer Dungeon. 'Cerberus boots' is not a real OSRS item -- the wiki page redirects to the Cerberus boss uniques and no such item exists in-game. The three valid options confirmed by the Karuulm Slayer Dungeon wiki page are boots of stone, boots of brimstone, and granite boots. The intended item is boots of brimstone (crafted by applying a drake's claw to boots of stone). A player following this guidance cannot equip 'Cerberus boots' for heat protection because the item does not exist under that name.

### [blocker] C7: The wiki describes no prayer-bypass mechanic for Wyrms whatsoever. It recommends Protect from Magic as a straightforward defensive option with no caveat about the prayer being bypassed or ineffective for any window. This claim appears fabricated and would cause players to mismanage their prayer/health expectations.

- **Data says:** Wyrms hit through prayer for 1 second only
- **Wiki says (raw):** using a form of magic protection, such as Protect from Magic, or armour with high magic defence bonuses like dragonhide armour, a viable method for killing wyrms.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Wyrm
- **Suggested fix:** Remove the 'hits through prayer for 1 second' claim entirely. Wyrms do not have a prayer bypass mechanic per the wiki. Guidance should simply recommend Protect from Magic.
- **Skeptic receipt:** Wyrm wiki (WebFetch): 'The wiki contains no information indicating that Wyrms have any mechanic to bypass or penetrate Protect from Magic.' Wyrm wiki (wiki_lookup combat section): using 'a form of magic protection, such as Protect from Magic, or armour with high magic defence bonuses like dragonhide armour, a viable method for killing wyrms' -- no prayer-bypass caveat present anywhere on the page.
- **Skeptic reasoning:** The guidance step at line 11635 of drop_rates.json states 'they hit through prayer for 1s only'. The OSRS wiki page for Wyrm contains no prayer-bypass or prayer-penetration mechanic whatsoever. Wyrms use magic attacks at distance and Protect from Magic is described as a straightforward defensive option with no caveat about a penetration window. This claim appears to be a fabricated mechanic not present in the game, and would cause players to mismanage prayer usage and health expectations.

### [high] C8: The wiki describes the safespot as a ranged safespot (staying 6+ squares away). The Dragon hunter lance (DHL) is a melee weapon and cannot be used at a ranged safespot -- only the Dragon hunter crossbow (DHC) applies here. Listing the DHL as usable at this safespot is incorrect.

- **Data says:** You can long-range safespot Wyrms from outside their attack range using a Dragon hunter lance or Dragon hunter crossbow
- **Wiki says (raw):** attack and then lure them to the corner to trap them, allowing players to range them at a specific spot while staying out of their magic attack range of 6 squares.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Wyrm
- **Suggested fix:** Remove Dragon hunter lance from the safespot description. Only Dragon hunter crossbow (and other ranged dragonbane weapons) can be used at the 6-square ranged safespot. DHL is melee and cannot be used at range.
- **Skeptic receipt:** Wyrm wiki (WebFetch): 'lure [wyrms] to the corner to trap them, allowing players to range them at a specific spot while staying out of their magic attack range of 6 squares.' And: 'The Dragon hunter lance is a melee weapon, and safespotting requires ranged attacks to maintain distance. No, the Dragon hunter lance cannot be effectively used at the safespot.'
- **Skeptic reasoning:** The guidance step at line 11635 of drop_rates.json states 'you can long-range safespot from outside their attack range with a Dragon hunter lance or Dragon hunter crossbow'. The Dragon hunter lance is a melee weapon requiring adjacency (1-tile range) and cannot be used at a 6-square ranged safespot. The wiki safespot is explicitly a ranged safespot that requires staying beyond the Wyrm's 6-square magic attack range. Listing the Dragon hunter lance as usable at this safespot is mechanically impossible -- only the Dragon hunter crossbow (and other ranged weapons) can be used there.

## Lava Strykewyrm

### [blocker] C7: Lava Strykewyrms have at least two significant special mechanics: (1) a conditional combat style switch (melee at close range, ranged at distance) that affects safespotting and protection prayer choices, and (2) fire spells heal them equal to damage dealt. A player following guidance that dismisses these as non-existent could use fire spells and actively heal the monster, materially failing the kill.

- **Data says:** Lava Strykewyrms use standard combat mechanics with no special phases or mechanics
- **Wiki says (raw):** they use only melee when attacked from within one tile range of them, and only ranged when attacked from a distance. Fire spells will heal them equal to the amount hit. all damage can be avoided with protection prayers.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lava_Strykewyrm
- **Suggested fix:** Replace the 'standard combat mechanics' claim with accurate guidance: use Protect from Ranged when attacking at distance, avoid fire spells entirely (they heal the strykewyrm), and note the melee/ranged style switch based on distance.
- **Skeptic receipt:** "they use only melee when attacked from within one tile range of them, and only ranged when attacked from a distance. Fire spells will heal them equal to the amount hit. all damage can be avoided with protection prayers." -- https://oldschool.runescape.wiki/w/Lava_Strykewyrm (WebFetch)
- **Skeptic reasoning:** The guidance text at drop_rates.json line 11716 reads 'Standard combat mechanics; no PKer risk'. The wiki documents at least two mechanics that are materially non-standard and player-harmful if unknown: (1) fire spells restore the creature's HP equal to damage dealt -- a player reading 'standard combat' who uses fire magic actively heals the monster; (2) the creature switches between melee-only (within 1 tile) and ranged-only (at distance), which determines the correct protection prayer. The phrase 'standard combat mechanics' actively suppresses this information. The fire-spell healing interaction in particular is a trap a player will fall into if not warned. wiki_updates confirms zero recent changes to the Strykewyrm page, so this is not a staleness issue -- the mechanic is real and current. No refutation vector applies: this is not a multi-source ID question, not a variant, not account-type gated, and the wiki receipt is a direct, unparaphrased quote.

## Fossil Island Wyvern

### [medium] C2: No shield is required to enter the Wyvern Cave. The only entry gates are: an active Slayer task for the northern (task-only) cavern, or nothing for the southern cavern. The wiki lists no shield as a cave-entry prerequisite. The elemental/mind/dragonfire shields provide in-combat protection against ice breath but are not an entry gate.

- **Data says:** Elemental/mind/dragonfire shield is required to enter Wyvern Cave
- **Wiki says (raw):** the northern cavern is monitored by Weve (Steve's son) who only allows players on a Slayer task to kill the wyverns inside; the wyverns in the southern cavern can be killed at any time.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Wyvern_Cave
- **Suggested fix:** Change the guidance step description from framing the shield as required to enter the cave, to framing it as required protection against ice breath while fighting wyverns inside. The gate for cave entry is the Slayer task, not the shield.
- **Skeptic receipt:** Wiki: 'the northern cavern is monitored by Weve (Steve's son) who only allows players on a Slayer task to kill the wyverns inside; the wyverns in the southern cavern can be killed at any time.' -- no shield listed as entry requirement. Data line 11848: "description": "Enter the Wyvern Cave. Elemental/mind/dragonfire shield required", "section": "Travel", objectInteractAction: "Enter".
- **Skeptic reasoning:** The wiki confirms the only cave-entry gates are: a Slayer task (northern cavern only) or nothing at all (southern cavern). No shield is checked at the door. The data at line 11848 places shield language on the Travel/Enter step -- 'Enter the Wyvern Cave. Elemental/mind/dragonfire shield required' -- which falsely frames the shield as a cave-entry prerequisite. The shield is an in-combat protection against ice breath, not a door check. This is a genuine misframing: the shield language belongs only on the kill step (which already correctly says 'Requires elemental/mind/dragonfire shield for ice breath' at line 11858), not on the Enter action. The suggested fix is correct: remove or rephrase the shield note on the entry step so it no longer implies an entry gate.

## Skeletal Wyvern

### [medium] C8: The dungeon entrance is a trapdoor, not a ladder. Functionally equivalent but the description is factually wrong.

- **Data says:** Asgarnian Ice Dungeon is entered via a ladder that you climb down
- **Wiki says (raw):** You enter via a "trapdoor" located south of Port Sarim and north of Mudskipper Point.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Asgarnian_Ice_Dungeon
- **Suggested fix:** Change 'ladder' to 'trapdoor' in guidanceSteps[2].description.
- **Skeptic receipt:** Asgarnian Ice Dungeon wiki: 'found south of Port Sarim and north of Mudskipper Point, under a trapdoor.'
- **Skeptic reasoning:** The Asgarnian Ice Dungeon entrance is a trapdoor, not a ladder. The wiki states it is 'found south of Port Sarim and north of Mudskipper Point, under a trapdoor.' A guidance description calling it a ladder is factually wrong. The two objects are not mechanically equivalent from a player-facing description standpoint -- a trapdoor is a distinct interactive object in OSRS. No refutation vector applies: this is not a multi-source item, not a variant, not account-type-dependent, and the wiki receipt is unambiguous.

### [high] C9: The guidance says to run east, but the wiki says to run north first and then east. A player following the guidance would head east immediately from the entrance and get lost or end up in the wrong section of the dungeon.

- **Data says:** The Skeletal Wyvern room is reached by running east from the Asgarnian Ice Dungeon ladder entrance
- **Wiki says (raw):** first run north and then run east to find an open space
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skeletal_Wyvern/Strategies
- **Suggested fix:** Update guidanceSteps[2].description to say 'run north then east' to reach the Skeletal Wyvern chamber.
- **Skeptic receipt:** Skeletal Wyvern/Strategies wiki: 'first run north and then run east to find an open space with ice warriors and ice giants. The entrance to the wyverns' lair can be found on the southern wall of this room.'
- **Skeptic reasoning:** The Strategies page gives explicit step-by-step directions: run north first, then east. Guidance that says only 'run east' from the entrance omits the required northward leg. The dungeon layout means running directly east from the entrance does not lead to the wyvern chamber. The wiki quote is unambiguous and the directional omission would mislead a player. No refutation vector applies.

### [blocker] C12: No 'tail attack' exists in the wiki. The three attack styles are Ranged, Slash, and Icy breath -- no separate tail attack is listed or described. The prayer guidance (Protect from Missiles) is correct, but attributing it to a fabricated 'tail attack' is wrong and could cause confusion about the actual combat mechanics.

- **Data says:** Skeletal Wyvern has a close-range tail attack that can be protected against with Protect from Missiles prayer
- **Wiki says (raw):** Attack styles: Ranged, Slash, Icy breath. "Protect from Missiles is the suggested Prayer to use against them due to the amount of range damage skeletal wyverns do"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skeletal_Wyvern
- **Suggested fix:** Remove the 'close-range tail attack' language. Protect from Missiles applies to the ranged attack. Update guidanceSteps[3].description to reflect the actual attack styles: melee (slash), ranged, and icy breath.
- **Skeptic receipt:** Skeletal Wyvern wiki: attack styles listed as 'Ranged, Slash, Icy breath' -- no tail attack appears. Prayer guidance: 'Protect from Missiles is the suggested Prayer to use against them due to the amount of range damage skeletal wyverns do.'
- **Skeptic reasoning:** The wiki lists exactly three attack styles: 'Ranged, Slash, Icy breath.' There is no 'tail attack' described anywhere on the Skeletal Wyvern page or Strategies page. Protect from Missiles is the correct prayer recommendation, but attributing it to a fabricated 'close-range tail attack' misrepresents the actual combat mechanic. The three real attack styles are melee slash (close range), ranged (6-tile range), and icy breath. No OSRS mechanic or variant creates a distinct 'tail attack' object for Skeletal Wyverns.

### [blocker] C13: Two errors: (1) Long Bones are not a Skeletal Wyvern drop -- the wiki does not list Skeletal Wyverns among the sources for Long Bones. (2) The XP value is wrong: Long Bones give 4,500 Construction XP (via Barlak), not 5 XP. Both the drop source and the XP figure are incorrect.

- **Data says:** Long Bones are a drop from Skeletal Wyverns worth 5 Construction XP each
- **Wiki says (raw):** Long bones provide 4,500 Construction experience when traded to Barlak in Dorgesh-Kaan
- **Wiki URL:** https://oldschool.runescape.wiki/w/Long_bone
- **Suggested fix:** Remove the Long Bones claim from guidanceSteps[4].description entirely, or replace with an accurate Skeletal Wyvern drop note. If Long Bones are intended, verify the drop source first -- they do not appear on the Skeletal Wyvern drop table.
- **Skeptic receipt:** Long Bone wiki: 'Skeletal Wyvern is not listed as a source of long bones.' XP: 'players with level 30+ Construction receive 4,500 Construction XP per bone.' Skeletal Wyvern drop table (wiki_lookup, 36 entries): no Long Bone entry present.
- **Skeptic reasoning:** Two distinct errors are both confirmed. (1) The Long Bone wiki page lists over 80 monster sources and Skeletal Wyvern does not appear among them. The wiki_lookup drop table for Skeletal Wyvern also contains no Long Bone entry across all 36 drops. (2) Long Bones give 4,500 Construction XP when traded to Barlak, not 5 XP. Both the drop attribution and the XP figure are wrong.

### [blocker] C14: Skeletal Wyverns drop the draconic visage, not the wyvern visage. The wiki explicitly calls this out as a common misconception. Players relying on this guidance would be grinding the wrong monster for a wyvern visage.

- **Data says:** Wyvern visage is a drop from Skeletal Wyverns
- **Wiki says (raw):** Unlike the wyverns that reside within Fossil Island, skeletal wyverns do not drop the wyvern visage; rather, they drop the draconic visage.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Skeletal_Wyvern
- **Suggested fix:** Change 'Wyvern visage' to 'Draconic visage' in guidanceSteps[4].description.
- **Skeptic receipt:** Skeletal Wyvern wiki: 'Unlike the wyverns that reside within Fossil Island, skeletal wyverns do not drop the wyvern visage; rather, they drop the draconic visage.' wiki_lookup drop table: 'Draconic visage | qty: 1 | rarity: 1/10000' -- wyvern visage absent from all 36 drop entries.
- **Skeptic reasoning:** The Skeletal Wyvern wiki page explicitly states the monster drops a draconic visage, not a wyvern visage, and calls out the distinction directly. The wiki_lookup drop table confirms only 'Draconic visage | qty: 1 | rarity: 1/10000' -- no wyvern visage entry exists. The wiki quote preemptively addresses this as a known point of confusion between Skeletal Wyverns and the Fossil Island wyvern variants.

## Twisted Banshee

### [medium] C4: The guidance describes the no-protection penalty as Banshees dealing 'increased damage', but the actual mechanic is a fixed 7 damage hit plus heavy stat drains across multiple skills. 'Increased damage' misrepresents the effect -- it is a stat-drain debuff, not a damage multiplier. A player reading this would not understand they are taking passive stat drain hits, which affects their decision-making (e.g., Prayer drain means they lose protection prayers faster, Attack/Strength drain reduces their DPS).

- **Data says:** Without earmuffs or a slayer helmet, Banshees hit hard (deal increased damage)
- **Wiki says (raw):** the player to take 7 damage and have their stats greatly reduced in Attack, Strength, Defence, Ranged, Magic, Prayer, and Agility with every attack received
- **Wiki URL:** https://oldschool.runescape.wiki/w/Twisted_Banshee
- **Suggested fix:** Change to: 'Without earmuffs or a slayer helmet, each attack deals 7 damage and heavily drains your Attack, Strength, Defence, Ranged, Magic, Prayer, and Agility stats.'
- **Skeptic receipt:** Wiki text (https://oldschool.runescape.wiki/w/Twisted_Banshee): "cause the player to take 7 damage and have their stats greatly reduced in Attack, Strength, Defence, Ranged, Magic, Prayer, and Agility with every attack received"
- **Skeptic reasoning:** The data guidance describes the no-protection penalty as Banshees 'dealing increased damage', but the wiki is unambiguous that the mechanic is a fixed 7-damage hit combined with heavy stat drain across Attack, Strength, Defence, Ranged, Magic, Prayer, and Agility on every attack received. 'Increased damage' is wrong on both counts: the damage value is fixed (not a multiplier or increase over normal), and the description omits the multi-skill stat drain entirely -- which is the mechanically significant part of the penalty. A player reading 'hits hard' would not understand they are losing Prayer points (and thus protection prayers) and DPS stats on every single hit. No refutation vector applies: this is not a multi-source item issue, not a variant id issue, not account-type-specific, and the wiki page is current. The finding stands.

### [low] C3: The travel tip describes fairy ring CIS as going to Kourend Castle and then descending, but CIS lands at Demon's Run -- a northeast entrance directly into the Catacombs, bypassing Kourend Castle entirely. The route is internally inconsistent (CIS and Kourend Castle are separate access points, not a chain). A player using CIS would arrive near the Catacombs entrance directly, not at Kourend Castle. Low severity because either leg of the instruction still gets the player there; no effort is truly wasted.

- **Data says:** Travel to Twisted Banshees via Xeric's talisman to Xeric's Heart, or fairy ring CIS then Kourend Castle then descend into the Catacombs
- **Wiki says (raw):** Fairy rings: "CIS" and "DJR" codes provide quick access ... fairy ring CIS reaches Demon's Run (northeast)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Catacombs_of_Kourend
- **Suggested fix:** Split into distinct routes: 'Xeric's talisman (Xeric's Heart) -> enter Catacombs at King Rada I statue; or fairy ring CIS -> Demon's Run northeast entrance directly into the Catacombs; or Kourend Castle teleport -> descend into the Catacombs.'
- **Skeptic receipt:** Wiki text (https://oldschool.runescape.wiki/w/Catacombs_of_Kourend): "Once the exit has been used it can be quickly accessed via fairy ring CIS and running east." -- CIS lands at Demon's Run, a direct northeast Catacombs surface entrance, not at Kourend Castle.
- **Skeptic reasoning:** The data chains fairy ring CIS with Kourend Castle as a single route ('fairy ring CIS then Kourend Castle then descend'), but CIS lands at Demon's Run -- a northeast surface entrance that leads directly into the Catacombs. CIS and the Kourend Castle statue entrance are two distinct, independent access points; they cannot be chained. A player following the data's route as written would be confused: after arriving at Demon's Run via CIS they are already at a Catacombs entrance and there is no Kourend Castle step involved. The finding's low-severity characterisation is fair (a player would eventually reach the Catacombs), but the route description is genuinely incoherent as authored. The wiki confirms CIS goes to Demon's Run, not to Kourend Castle.

## Gargoyle

### [low] C10: The wiki states no Slayer level requirement for the perk itself -- only a cost of 120 Slayer points. The 75 Slayer level is the requirement to receive gargoyles as a Slayer task, not a requirement to purchase or use the perk. Attributing '75+ Slayer' as the perk's gate is technically a misattribution, though in practice no player would encounter the perk without already having 75+ Slayer.

- **Data says:** Gargoyle smasher perk auto-smashes Gargoyles for players with 75+ Slayer
- **Wiki says (raw):** players can buy the 'Gargoyle Smasher' perk from Slayer Masters for 120 Slayer points, which will automatically smash the gargoyle when it falls to 9 or lower Hitpoints, provided that the player has a rock hammer (or equivalent) in their inventory.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gargoyle
- **Suggested fix:** Rephrase to: 'The Gargoyle Smasher perk (120 Slayer points) auto-smashes gargoyles at 9 HP or lower when a rock hammer is in inventory -- no separate Slayer level requirement for the perk itself.'
- **Skeptic receipt:** Wiki (fetched directly): 'players can buy the Gargoyle Smasher perk from Slayer Masters for 120 Slayer points, which will automatically smash the gargoyle when it falls to 9 or lower Hitpoints, provided that the player has a rock hammer (or equivalent) in their inventory.' -- no Slayer level requirement stated for the perk itself.
- **Skeptic reasoning:** The data attributes '75+ Slayer' as the operative gate for the Gargoyle Smasher perk. The wiki is unambiguous: the perk costs 120 Slayer points and carries no stated Slayer level requirement of its own. The 75 Slayer level is a requirement to be assigned gargoyles as a Slayer task and to damage them -- it is not a requirement to purchase or activate the Gargoyle Smasher perk. Presenting '75+ Slayer' as the perk's gate misattributes a task-assignment requirement as a perk-purchase requirement. While in practice no player encounters the perk without 75 Slayer, the description as written creates a false causal link between the perk and the Slayer level.

### [blocker] C12: The wiki describes no HP-reset or 'boost-the-HP trick' mechanic for gargoyles whatsoever. This appears to be a fabricated mechanic. Following this guidance step could cause a player to deliberately restrict their damage output based on a mechanic that does not exist in the game.

- **Data says:** Gargoyles have a mechanic where over-damaging them resets their HP to full (boost-the-HP trick)
- **Wiki says (raw):** In order to kill a gargoyle, a rock hammer, a rock thrownhammer, or a granite hammer must be used when they reach 8 or lower Hitpoints.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gargoyle
- **Suggested fix:** Remove or replace this step entirely -- no such HP-reset mechanic exists for gargoyles per the wiki.
- **Skeptic receipt:** Wiki (fetched): 'In order to kill a gargoyle, a rock hammer, a rock thrownhammer, or a granite hammer must be used when they reach 8 or lower Hitpoints.' -- zero mention of any HP-reset, over-damage penalty, or 'boost-the-HP trick' anywhere on the page. The mechanic does not exist.
- **Skeptic reasoning:** The Gargoyle wiki page contains no mention of any HP-reset or 'boost-the-HP trick' mechanic whatsoever. Gargoyles do not have an over-damage HP-reset mechanic in OSRS. Guidance instructing a player to deliberately limit their damage output based on this non-existent mechanic would actively harm their efficiency and is flatly incorrect.

### [medium] C13: The wiki describes the granite maul's special attack as 'an instant attack', which is understood in the community to be a 1-tick special (it fires outside the normal attack cycle). Labelling it as '3-tick' contradicts the wiki and the known mechanic -- 3 ticks is the weapon's normal attack speed, not its special attack speed.

- **Data says:** Granite mauls are 3-tick spec weapons
- **Wiki says (raw):** Quick Smash, consumes 60% of the player's special attack energy and deals an instant attack.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Granite_maul
- **Suggested fix:** Correct to: 'Granite mauls have a 1-tick (instant) special attack that fires outside the normal attack cycle, costing 60% special attack energy.'
- **Skeptic receipt:** Wiki (fetched): 'The granite maul's special attack, Quick Smash, consumes 60% of the player's special attack energy and deals an instant attack.' -- 'instant', not '3-tick'. The wiki provides no tick count for the spec because it fires outside the normal attack cycle.
- **Skeptic reasoning:** The granite maul's special attack Quick Smash is explicitly described by the wiki as 'an instant attack' -- meaning it fires outside the normal attack cycle, not on a 3-tick delay. The granite maul's normal attack speed is 7 ticks (4.2 seconds), and '3-tick' does not describe any aspect of the weapon or its special attack. Labelling the spec as '3-tick' directly contradicts the wiki's 'instant attack' description and the universally understood community mechanic. This is a material error: players making decisions about using the granite maul spec for finishing gargoyles would have a wrong mental model of the weapon's timing.

## Custodian Stalker

### [medium] C4: The guidance tells players to 'move off infected spore patches when they appear', implying ground patches to dodge. The wiki describes no such mechanic -- the elder stalker directly douses the player in spores (a targeted attack, blockable by Protect from Magic), not patches placed on the ground. The guidance implies a positional dodge mechanic that does not exist.

- **Data says:** move off infected spore patches when they appear
- **Wiki says (raw):** Once they reach 20% health, they can perform a special attack where they stomp the ground and douse the player in spores. The attack can miss or be blocked by Protect from Magic, but if it hits, it will cause the player to bleed for 9 total damage over 15 ticks (9 seconds).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Elder_custodian_stalker
- **Suggested fix:** Replace 'move off infected spore patches when they appear' with 'the spore attack causes Bleed -- use Protect from Magic to block it or accept the 9 damage and heal through it'.
- **Skeptic receipt:** WebFetch (Elder custodian stalker): "The stalker 'stomp[s] the ground and douse[s] the player in spores.' This attack targets the player directly rather than creating environmental ground patches. The spore attack can miss or be blocked by Protect from Magic." WebFetch (Stalker Den): "there are no descriptions of infected spore patches, area-denial mechanics, environmental hazards, or ground objects that players must dodge or move away from."
- **Skeptic reasoning:** The guidance step in drop_rates.json (line 12368) reads: "Elder variants apply a Bleed - move off infected spore patches when they appear." This implies a positional ground-patch mechanic requiring the player to reposition. The wiki establishes no such mechanic exists. The Elder custodian stalker's spore attack targets the player directly (not via ground objects) and is blockable by Protect from Magic -- the correct player response is to use the prayer, not to move. The Stalker Den wiki page confirms zero ground hazard or area-denial mechanics in the dungeon. The guidance actively misdirects players toward repositioning when they should be praying Protect from Magic.

### [medium] C6: The guidance claims 'mature, elder' variants have better rates on Alchemist's signet. The wiki shows the Mature custodian stalker does not drop Alchemist's signet at all -- only Elder and Ancient Custodian are sources. Lumping mature in as having 'better rates' on this item is incorrect; mature is not a source.

- **Data says:** Higher-tier variants (mature, elder) have better rates on Antler guard and Alchemist's signet
- **Wiki says (raw):** Antler Guard" - described as a tertiary drop with a 1/800 rarity [for mature]; Elder custodian stalker drops [Alchemist's signet] at 2/124 rarity [and Ancient Custodian at 3 x 2/124]; [Mature custodian stalker drops section contains no Alchemist's signet entry]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mature_custodian_stalker
- **Suggested fix:** Change 'Higher-tier variants (mature, elder) have better rates on Antler guard and Alchemist's signet' to 'Elder/Ancient variants have better rates on Antler guard and are the only sources of Alchemist's signet; mature drops Antler guard at a lower rate (1/800 vs 1/650) and does not drop Alchemist's signet'.
- **Skeptic receipt:** wiki_lookup (Mature custodian stalker) -- full 16-entry drop table: Big bones, Steel cannonball, Air rune, Fire rune, Rune arrow, Death rune, Pure essence, Monkfish, Broken antler, Mithril bar, Raw beef, Huasca seed, Coins, Antler guard (1/800), Long bone, Curved bone. No Alchemist's signet entry. wiki_lookup (Elder custodian stalker): "Alchemist's signet | qty: 1 | rarity: 2/124"
- **Skeptic reasoning:** The guidance step (line 12368) states 'Higher-tier variants (mature, elder) have better rates on Antler guard and Alchemist's signet', presenting mature as a source of Alchemist's signet. The wiki_lookup for Mature custodian stalker returns 16 drop entries with no Alchemist's signet anywhere in the table. Only Elder custodian stalker drops Alchemist's signet (2/124). Mature drops Antler guard at 1/800 versus Elder's 1/650 -- so the Antler guard tier claim is correct, but grouping mature into the Alchemist's signet claim is factually wrong.

## Aquanite

### [blocker] C3: Crush is wrong. Aquanites have +80 Crush defence (tied for highest). The correct meta is Slash to sever the lure, then Stab (Ghrazi rapier). Recommending Abyssal bludgeon or Inquisitor's mace actively works against the player.

- **Data says:** Crush weapon (Abyssal bludgeon, Inquisitor's mace) recommended for best accuracy against Aquanites
- **Wiki says (raw):** Ghrazi rapier for consistent stab damage ... if an Aquanite is hit with a slash attack, its lure will be severed and its stab Defence will be reduced by 50 (from 60 to 10)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aquanite/Strategies
- **Suggested fix:** Replace crush recommendation with: use a fast slash weapon (whip, scimitar) to sever the lure on the first hit, reducing Stab defence from 60 to 10, then switch to a high-accuracy stab weapon (e.g. Ghrazi rapier) for the kill.
- **Skeptic receipt:** Defence bonuses from wiki: Stab +60, Slash +80, Crush +80. Strategies page: "a fast 4-tick slash weapon such as a Scimitar or Abyssal whip is recommended" [for severing the lure]; primary stab weapons include Ghrazi rapier and Osmumten's fang. "The wiki does not mention Abyssal bludgeon or Inquisitor's mace in the Aquanite strategies guide."
- **Skeptic reasoning:** The wiki defence table shows Crush defence +80 (tied with Slash for highest, worse than post-lure-sever Stab which drops to +10). The strategies page names zero crush weapons; it explicitly recommends a 4-tick slash weapon to sever the lure then a stab weapon (Ghrazi rapier / Osmumten's fang) for the kill. Abyssal bludgeon and Inquisitor's mace are not mentioned anywhere on the strategies page. Recommending a crush weapon steers the player into the worst defence bracket and bypasses the core lure mechanic entirely.

### [blocker] C4: Aquanites attack exclusively with Magic, not melee. Protect from Melee offers zero damage reduction and will result in taking full Magic hits.

- **Data says:** Protect from Melee prayer is the appropriate defensive protection against Aquanites
- **Wiki says (raw):** Aquanites attack with Magic and have high accuracy, so using Protect from Magic is highly recommended.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aquanite/Strategies
- **Suggested fix:** Change to Protect from Magic.
- **Skeptic receipt:** wiki_lookup + WebFetch (Aquanite main page): Attack Style: "Magic", Max Hit: 18, Attack Speed: "5 ticks (3.0 seconds)". Strategies page: "Aquanites attack with Magic and have high accuracy, so using Protect from Magic is highly recommended."
- **Skeptic reasoning:** Aquanites have a single attack style: Magic. The strategies page is unambiguous that Protect from Magic is the correct prayer. Protect from Melee provides no mitigation against a Magic-only attacker. No account-type, progress, or variant consideration changes this -- there is no melee phase or alternate form. This is a direct inversion with zero legitimate-game-mechanic defence.

### [blocker] C5: Aquanites attack exclusively with Magic (max hit 18), not melee. This claim is a direct inversion of their attack style.

- **Data says:** Aquanites deal melee damage only
- **Wiki says (raw):** Magic" with a max hit of 18, attacking every "5 ticks (3.0 seconds)"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aquanite
- **Suggested fix:** Change to: Aquanites attack with Magic only (max hit 18, 5-tick attack speed). Use Protect from Magic.
- **Skeptic receipt:** WebFetch (https://oldschool.runescape.wiki/w/Aquanite): Attack Style: "Magic", Max Hit: 18, Attack Speed: "5 ticks (3.0 seconds)". npc_lookup: Aggressive: No, Poisonous: No -- no melee attack style listed.
- **Skeptic reasoning:** The main Aquanite wiki page lists the attack style as Magic (only), max hit 18, attack speed 5 ticks. The NPC lookup confirms combat level 145 / HP 180 with no melee attack noted. There is no melee phase, no alternate form, and no account-type variation that makes this correct for any player. The claim is a direct factual inversion.

### [medium] C6: Aquanites have a notable lure mechanic: one slash hit severs the lure, permanently reducing Stab defence by 50 for that kill. Omitting this understates the combat complexity and causes players to miss a meaningful accuracy boost.

- **Data says:** Aquanites have no special attacks or phase mechanics
- **Wiki says (raw):** if an Aquanite is hit with a slash attack, its lure will be severed and its stab Defence will be reduced by 50 (from 60 to 10), displaying the message Your attack cuts off the aquanite's lure! ... dealing damage is not required to sever the lure
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aquanite/Strategies
- **Suggested fix:** Add a note about the lure mechanic: a single slash attack severs the lure (no damage required), reducing Stab defence from 60 to 10, significantly improving accuracy with subsequent stab attacks.
- **Skeptic receipt:** WebFetch (https://oldschool.runescape.wiki/w/Aquanite): "When attacked with slash weapons, the aquanite's lure can be severed, reducing stab defence by 50 points (from 60 to 10). The attack 'does not need to deal damage to sever the lure.'" Strategies page: "if an Aquanite is hit with a slash attack, its lure will be severed and its stab Defence will be reduced by 50 (from 60 to 10)."
- **Skeptic reasoning:** The lure mechanic is explicitly documented on the strategies page and cross-confirmed by the main page. It is not a minor flavour detail -- it reduces Stab defence from +60 to +10, a 50-point swing that materially affects accuracy for the recommended weapon style. The main page also notes the lure does not need to deal damage to be severed. Omitting this from guidance leaves players with no knowledge of the single most impactful combat mechanic for this monster. wiki_updates returns zero Aquanite changes since 2025-01-01, so this is not a staleness issue -- the mechanic is current.

## Nechryael

### [medium] C3: Direction from Ranging Guild to Slayer Tower is stated as south-east, but the Slayer Tower is north-west of Canifis and well to the north-east of the Ranging Guild (which sits between Seers' Village and East Ardougne). The correct run direction from the Ranging Guild teleport is north-east, not south-east.

- **Data says:** POH jewellery box can summon Combat bracelet to teleport to Ranging Guild, from which Slayer Tower can be reached by running south-east
- **Wiki says (raw):** The tower is located north-west of Canifis.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Slayer_Tower
- **Suggested fix:** Change 'running south-east' to 'running north-east' in the conditionalAlternatives description for guidanceSteps[0].
- **Skeptic receipt:** coordinate_helper distance (2655,3445) -> (3429,3535): Euclidean 779.2 tiles, Chebyshev 774. Delta-X = +774 (east), Delta-Y = +90 (north). wiki_lookup Slayer Tower: 'The tower is located north-west of Canifis.' wiki_lookup Ranging Guild: 'located between Seers Village and East Ardougne' / 'A charged combat bracelet will teleport just outside the entrance.' drop_rates.json line 12497-12498: description: 'POH jewellery box -> Combat bracelet -> Ranging Guild, then run south-east along the road to the Slayer Tower entrance', travelTip: 'POH Combat bracelet -> Ranging Guild -> run south-east to Slayer Tower'
- **Skeptic reasoning:** The data text 'run south-east along the road to the Slayer Tower entrance' is geometrically wrong. The Ranging Guild Combat bracelet teleport destination is at approximately (2655, 3445). The Slayer Tower ground-floor entrance guidance target is (3429, 3535). Delta-X = +774 (east), Delta-Y = +90 (north). The Slayer Tower is east-northeast of the Ranging Guild -- not south-east. 'South-east' requires a negative delta-Y (player moves south), which is the opposite of what the geometry shows. The wiki receipt ('The tower is located north-west of Canifis') is consistent: Canifis is at ~(3494, 3485) and the tower at ~(3428, 3547), confirming the tower is NW of Canifis and therefore well to the east of Kandarin. The suggested fix of 'north-east' lands in the correct compass quadrant (positive X, positive Y) and is acceptable as casual guidance text, though 'east' would be marginally more precise (bearing is ~6.6 degrees north of due east). The core error -- 'south-east' instead of 'east/north-east' -- is a genuine directional inversion that would actively mislead a player.

## Drake

### [blocker] C6: The maximum dragonfire breath damage is 4 hits of up to 8 = 32, not 40. The claim overstates the max hit by 8.

- **Data says:** Dragonfire breath hits up to 40 damage if you stand still
- **Wiki says (raw):** deals 6-8 damage for four hits if the player is struck by the projectile
- **Wiki URL:** https://oldschool.runescape.wiki/w/Drake
- **Suggested fix:** Change '40 damage' to '32 damage (up to 8 per hit, four hits)' to match the wiki's stated 6-8 damage per hit mechanic.
- **Skeptic receipt:** Drake/Strategies page: 'After seven auto-attacks, drakes will launch a volcanic breath towards the player's current position, which deals 6-8 damage for four hits if the player is struck by the projectile.' Drake infobox max hit field: '32 (Dragonfire)'
- **Skeptic reasoning:** The wiki states the dragonfire breath deals '6-8 damage for four hits' giving a maximum of 32, not 40. The Drake infobox also lists '32 (Dragonfire)' as the max hit. The data value of 40 overstates the ceiling by 8 damage and is not supported by any mechanic or protection calculation on the wiki.

### [blocker] C7: Drakes' standard attacks are Ranged -- the correct prayer is Protect from Missiles, not Protect from Magic. Advising Magic prayer leaves the player fully exposed to their normal attacks.

- **Data says:** Pray Magic for the standard ranged-magic attack style
- **Wiki says (raw):** Normal ranged attacks of drakes can be fully blocked by Protect from Missiles.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Drake/Strategies
- **Suggested fix:** Change 'Pray Magic' to 'Pray Missiles (Protect from Missiles)' to match Drake's ranged attack style.
- **Skeptic receipt:** Drake/Strategies: 'Drakes use both ranged and ranged melee, so melee armour is recommended against their attacks.' and 'Drakes normal ranged attacks can be fully blocked by Protect from Missiles.' and 'Together, a player can take no damage when using Protect from Missiles and dragonfire protection, if attacking from at least 1 tile away.' No Protect from Magic recommendation appears anywhere on the strategies page.
- **Skeptic reasoning:** Drake's standard attack style is Ranged, not Magic. The wiki is unambiguous that Protect from Missiles is the correct prayer. Advising 'Pray Magic' leaves the player fully exposed to Drake's normal attacks. No Drake attack style listed on the wiki is Magic-based.

### [medium] C3: Rada's blessing 3 requires completing the Kourend & Kebos Hard Diary -- it is not 'unquested.' Describing it as unquested misstates the unlock requirement.

- **Data says:** Rada's blessing 3/4 teleport is an unquested alternative to fairy ring CIR for reaching Mount Karuulm
- **Wiki says (raw):** Players may use Rada's blessing 3 or higher, which is unlocked after completing the Kourend & Kebos Hard Diary, to teleport straight to the dungeon using 'Mount Karuulm' option.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon
- **Suggested fix:** Remove 'unquested' and replace with 'requires Kourend & Kebos Hard Diary' or simply omit the qualifier.
- **Skeptic receipt:** wiki_lookup Karuulm Slayer Dungeon: 'Players may use Rada's blessing 3 or higher, which is unlocked after completing the Kourend & Kebos Hard Diary.' Kourend & Kebos Diary wiki: Hard tier quest requirements include 'The Ascent of Arceuus', 'The Depths of Despair', 'Dream Mentor', 'The Forsaken Tower', 'The Queen of Thieves', 'Tale of the Righteous'.
- **Skeptic reasoning:** Rada's blessing 3 is unlocked via the Kourend & Kebos Hard Diary, which itself requires completing six quests: The Ascent of Arceuus, The Depths of Despair, Dream Mentor, The Forsaken Tower, The Queen of Thieves, and Tale of the Righteous. Describing this teleport as 'unquested' is incorrect -- it sits behind a substantial quest gate. The finding's suggested fix (replace 'unquested' with 'requires Kourend & Kebos Hard Diary') is accurate.

### [low] C2: The fairy ring CIR does not land at the dungeon entrance -- the mountain and dungeon entrance are a short walk north of the ring. 'Right at the entrance' overstates proximity.

- **Data says:** Fairy ring CIR drops you right at the dungeon entrance
- **Wiki says (raw):** Players can get there via the fairy ring CIR. The mountain is a short walk north of this fairy ring.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon
- **Suggested fix:** Change to 'Fairy ring CIR -- short walk north up the volcano to the dungeon entrance.'
- **Skeptic receipt:** wiki_lookup Karuulm Slayer Dungeon: 'Players can get there via the fairy ring . The mountain is a short walk north of this fairy ring.'
- **Skeptic reasoning:** The wiki explicitly states the fairy ring CIR lands at a point from which 'the mountain is a short walk north.' The dungeon entrance is at the top of the mountain volcano. Describing the ring as dropping you 'right at the dungeon entrance' overstates proximity in a way that could mislead a player expecting to be at the entrance. The wiki quote contradicts the 'right at the entrance' phrasing directly.

## Dark Beast

### [high] C3: Dark Beasts no longer exist in the Catacombs of Kourend; they were removed in the 25 July 2019 update. Sending players there would find no Dark Beasts.

- **Data says:** Dark Beasts can be found in the Catacombs of Kourend
- **Wiki says (raw):** Dark beasts were added to Iorwerth Dungeon and removed from Kourend Catacombs for thematic reasons.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dark_beast
- **Suggested fix:** Remove the Catacombs of Kourend from the list of Dark Beast locations in guidanceSteps[1].description. The two valid locations are the Iorwerth Dungeon and the Mourner Tunnels.
- **Skeptic receipt:** WebFetch Dark_beast: "Dark Beasts were previously located in the Catacombs of Kourend. According to the update log, 'Dark beasts were added to Iorwerth Dungeon and removed from Katacombs of Kourend for thematic reasons' on July 25, 2019, coinciding with the Song of the Elves update." Current locations: Mourner Tunnels (19 spawns), Iorwerth Dungeon (10 spawns). Data line 13025: "Kill Dark Beasts in the Mourner Tunnels or Catacombs of Kourend."
- **Skeptic reasoning:** The guidance step at drop_rates.json line 13025 reads: "Kill Dark Beasts in the Mourner Tunnels or Catacombs of Kourend." The wiki confirms Dark Beasts were removed from the Catacombs of Kourend on 25 July 2019 as part of the Song of the Elves update and relocated to the Iorwerth Dungeon. The two valid locations today are Mourner Tunnels and Iorwerth Dungeon. Sending players to the Catacombs of Kourend will find no Dark Beasts.

### [blocker] C5: Fairy ring code CIQ teleports to north-west of Yanille in Kandarin, not to Prifddinas or the Iorwerth Dungeon entrance. Following this guidance would take the player to entirely the wrong location.

- **Data says:** POH superior garden fairy ring with dramen staff or lunar staff equipped can dial CIQ to reach the Iorwerth Dungeon entrance in Prifddinas
- **Wiki says (raw):** Kandarin: North-west of Yanille
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Remove or correct the CIQ fairy ring alternative for reaching the Iorwerth Dungeon. There is no fairy ring code that leads to Prifddinas. The correct travel method is a Teleport crystal (or eternal teleport crystal) to Prifddinas after completing Song of the Elves.
- **Skeptic receipt:** WebFetch Fairy_ring full code table: "CIQ | North-west of Yanille" and "DIQ | Player-owned house (superior garden)". No code maps to Prifddinas or Iorwerth Dungeon. Data line 13018: "POH superior garden fairy ring (dramen/lunar staff equipped) -> dial CIQ to land directly at the Iorwerth Dungeon entrance in Prifddinas".
- **Skeptic reasoning:** The guidance step at drop_rates.json line 13018 claims CIQ leads to "the Iorwerth Dungeon entrance in Prifddinas". The full fairy ring code table from the wiki shows CIQ = "North-west of Yanille" in Kandarin. No fairy ring code in the entire 55-code table leads to Prifddinas or the Iorwerth Dungeon. DIQ leads to a player-owned house superior garden (not outbound to Prifddinas). Following this guidance would take the player to the wrong continent entirely.

### [medium] C4: The item that teleports to Prifddinas is the Teleport crystal (or eternal teleport crystal), not the crystal seed. The crystal teleport seed is the depleted/uncharged form and cannot teleport on its own -- it must be recharged by Eluned into a teleport crystal first.

- **Data says:** Teleport via crystal seed to reach Prifddinas for the Iorwerth Dungeon
- **Wiki says (raw):** can teleport a player to the elven village of Lletya (and Prifddinas upon completion of the Song of the Elves quest)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Teleport_crystal
- **Suggested fix:** Change 'crystal seed' to 'teleport crystal (or eternal teleport crystal)' in guidanceSteps[0].description.
- **Skeptic receipt:** WebFetch Teleport_crystal: "A teleport crystal is a small crystal which can teleport a player to the elven village of Lletya (and Prifddinas upon completion of the Song of the Elves quest)." WebFetch Crystal_teleport_seed: "the crystal teleport seed is a depleted teleport crystal that has run out of charges ... the seed cannot teleport players independently. It must be recharged first before use." Data line 13005: "(crystal seed teleport)". Data line 13000 (travelTip): "Teleport crystal -> Prifddinas".
- **Skeptic reasoning:** The guidance step at drop_rates.json line 13005 says "(crystal seed teleport)". The crystal teleport seed is the depleted, uncharged form of a teleport crystal -- it cannot teleport players on its own. It must be recharged by Eluned into a functional teleport crystal first. The item that actually teleports to Prifddinas (post-Song of the Elves) is the teleport crystal or eternal teleport crystal, not the seed. The travelTip field at line 13000 correctly says "Teleport crystal -> Prifddinas", creating an internal inconsistency within the same entry.

## Araxyte

### [blocker] C3: The wiki documents no web attack mechanic for Araxytes. There is no dodgeable web projectile described anywhere on the page. This claim appears to be fabricated -- Araxytes have a single standard crush attack. Guidance telling players to dodge web attacks is incorrect and would be confusing in-game.

- **Data says:** Araxytes perform web attacks that players must dodge
- **Wiki says (raw):** The wiki contains no description of any web attack mechanic for Araxytes. Attack style: Crush with 4 ticks (2.4 seconds) attack speed and max hit of 13.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxyte
- **Suggested fix:** Remove the web attack dodge step entirely. The only special consideration the wiki notes is managing venom -- replace with guidance to use antivenoms/antipoison.
- **Skeptic receipt:** wiki_lookup (Araxyte): Attack style Crush, attack speed 4 ticks, max hit 13. No special attacks section present. npc_lookup: 'Poisonous: Yes (venom)' -- no web-attack attribute. Morytania Spider Cave wiki: 'The caves are full of araxytes that are capable of inflicting venom.' No web attack or dodgeable projectile mentioned on either page.
- **Skeptic reasoning:** The wiki page for Araxyte documents a single Crush attack (4-tick, max hit 13/17), venom on hit, and no special attack section whatsoever. The Morytania Spider Cave page mentions only that araxytes 'are capable of inflicting venom' -- no web projectile, no web mechanic, no dodgeable attack of any kind appears on either page. The npc_lookup returns no special-attack flag. There is no authoritative source in any consulted tool that describes a web attack mechanic for Araxytes. Guidance instructing players to dodge web attacks describes a mechanic that does not exist.

### [medium] C4: The wiki confirms Araxytes inflict venom (Poisonous: Yes (venom)), but describes no dodgeable venom attack mechanic. Venom is a passive affliction managed with antivenoms, not something players can dodge. Framing it as a 'dodge' mechanic is misleading and could cause players to waste time trying to avoid it rather than using the correct counter-item.

- **Data says:** Araxytes perform venom attacks that players must dodge
- **Wiki says (raw):** it is dangerous to fight them without prayer and a method to stave off venom
- **Wiki URL:** https://oldschool.runescape.wiki/w/Araxyte
- **Suggested fix:** Change the guidance from 'dodge venom attacks' to 'bring antivenoms or a serpentine helm to counter venom'.
- **Skeptic receipt:** Araxyte wiki: 'it is dangerous to fight them without prayer and a method to stave off venom' -- prescribes item counter, not dodging. npc_lookup: 'Poisonous: Yes (venom)' -- confirms venom is a passive on-hit affliction, not a projectile. No dodgeable venom mechanic described on either the Araxyte or Morytania Spider Cave wiki pages.
- **Skeptic reasoning:** Venom from Araxytes is real and confirmed by both the wiki and npc_lookup. However, OSRS venom from standard melee monsters is applied passively on a successful melee hit -- it is not a projectile or a mechanic that can be dodged. The wiki prescribes an item-based counter ('a method to stave off venom'), not an avoidance manoeuvre. Framing this as a 'dodge venom attacks' guidance step is mechanically incorrect: there is no venom projectile to dodge. The correct guidance is to bring antivenoms, antidotes, or a serpentine helm. The finding's diagnosis of the framing error is sound and the receipt supports it.

## Hydra

### [blocker] C5: The claim that Hydra starts with Magic is wrong -- the wiki states it starts with a random combat style. Additionally the wiki says 'three attacks' (regardless of whether they hit), not 'three hits'. A player following this guidance and pre-praying Protect from Magic would be praying incorrectly roughly half the time from the first attack.

- **Data says:** Hydra switches attack styles: Magic first, then Ranged after three hits (like Alchemical Hydra)
- **Wiki says (raw):** Hydras start off with a random combat style at the start of the battle, switching after three attacks, regardless if they hit or not.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hydra
- **Suggested fix:** Change to: 'Hydra starts on a random combat style (Magic or Ranged), switching after every three attacks regardless of hit/miss -- watch its animation to determine the opening style and switch protection prayer accordingly.'
- **Skeptic receipt:** Wiki verbatim: 'Hydras start off with a random combat style at the start of the battle, switching after three attacks, regardless if they hit or not.'
- **Skeptic reasoning:** The guidance text at line 13264 of drop_rates.json states 'Pray against the active style (Magic first, then Ranged after three hits - just like Alchemical Hydra).' The wiki authoritatively contradicts both sub-claims. First, the starting style is not fixed to Magic -- it is random. Second, the switch trigger is 'three attacks, regardless if they hit or not,' not 'three hits.' A player following this guidance would pre-pray Protect from Magic incorrectly roughly half the time when the Hydra opens on Ranged, and would misunderstand the switch timing by conflating attacks with successful hits. Both errors survive all refutation vectors: this is not a multi-source item issue, not a variant issue, not account-type-specific, and the wiki page has had zero edits since January 2026 (wiki_updates confirmed 0 changes). The Alchemical Hydra comparison compounds the error -- the Alchemical Hydra does have fixed phase ordering, but the regular Hydra does not.

### [medium] C6: The wiki describes the special attack as poison splats (light green orbs), not fire puddles. Calling them 'fire-puddle telegraphs' misidentifies both the damage type and the visual. The mechanic description (tile under your feet + two random tiles) is otherwise correct.

- **Data says:** Hydra uses fire-puddle telegraphs under your feet as a mechanic
- **Wiki says (raw):** the middle head of the hydra will spit poison at three random tiles (one at the player's location, two elsewhere), dealing 4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity). ... poison splats, which are light green orbs
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hydra
- **Suggested fix:** Replace 'fire-puddle telegraphs' with 'poison splat telegraphs' (light green orbs).
- **Skeptic receipt:** Wiki verbatim: 'the middle head of the hydra will spit poison at three random tiles (one at the player's location, two elsewhere), dealing 4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity) when the player is standing on one. The initial splat has a 3x3 AoE.' The wiki also notes the projectiles are 'light green orbs' (poison splats), not fire.
- **Skeptic reasoning:** The guidance text calls the special attack 'fire-puddle telegraphs.' The wiki describes the mechanic as poison splats -- 'light green orbs' -- dealing poison damage, not fire damage. In OSRS, 'fire puddles' are a distinct visual and mechanical concept associated with content like Vorkath's acid/fire phase. Calling poison splats 'fire puddles' misidentifies both the damage type (poison vs fire) and the visual appearance (light green orbs vs fire/orange puddles). The mechanic description of 'tile under your feet' is directionally correct, but the name and damage type are wrong. No refutation vector applies: this is not a variant or multi-source issue, there is no account-type nuance, and the wiki page is current (0 changes since January 2026).

### [blocker] C7: The wiki gives the special attack damage as 4 poison damage every two ticks, not '30+ burn damage ticks'. The '30+' figure is fabricated and the damage type is poison, not burn/fire. A player acting on this guidance would severely misestimate the threat level.

- **Data says:** Fire-puddles deal 30+ burn damage ticks and can be avoided by moving one tile
- **Wiki says (raw):** dealing 4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hydra
- **Suggested fix:** Replace '30+ burn damage ticks' with '4 poison damage every two ticks (halved if poison-immune)' and correct 'fire-puddles' to 'poison splats'.
- **Skeptic receipt:** Wiki verbatim: 'dealing 4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity)'
- **Skeptic reasoning:** The guidance text states 'avoid the 30+ burn tick.' The wiki gives the damage as '4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity).' The '30+' figure has no basis in the wiki or any known game data -- the maximum possible from a single poison splat tick is 4 damage. The damage type is 'poison,' not 'burn' (burn is a separate mechanic in OSRS, e.g. from the Nagua quest reward weapon). Combined with C6, the guidance misnames the visual (fire-puddle), misidentifies the damage type (burn vs poison), and wildly overstates the damage (30+ vs 4). A player following this guidance would misestimate the threat level and might overbrew or misprioritize their avoidance relative to actual combat damage. No refutation vector applies.

### [high] C8: The wiki describes poison splat attacks, not venom. Venom and poison are distinct mechanics in OSRS (venom escalates and requires Antidote++/Serpentine helm to cure; poison does not escalate). The wiki lists the Hydra's special attack as a poison splat dealing 4 damage every two ticks, with no mention of venom being inflicted on the player by this mechanic.

- **Data says:** Hydra uses venom attacks that tick for 6+ damage even after the boss dies
- **Wiki says (raw):** the middle head of the hydra will spit poison at three random tiles (one at the player's location, two elsewhere), dealing 4 poison damage every two ticks
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hydra
- **Suggested fix:** Replace 'venom attacks' with 'poison splat attacks'. Remove the '6+ damage' claim and replace with '4 poison damage every two ticks'. Clarify that poison immunity (e.g. from prayer or items) halves this to 1-2 damage.
- **Skeptic receipt:** Wiki verbatim: 'the middle head of the hydra will spit poison at three random tiles (one at the player's location, two elsewhere), dealing 4 poison damage every two ticks (or 1 to 2 damage if the player has poison immunity).' No mention of venom being inflicted on the player, no mention of damage persisting after death.
- **Skeptic reasoning:** The guidance text states 'their venom attack ticks for 6+ even after they die.' Three distinct errors: (1) The damage type is poison, not venom -- these are mechanically distinct in OSRS; venom escalates and requires Antidote++ or a Serpentine helm to cure, while poison does not escalate. The wiki consistently uses 'poison' for this mechanic and never mentions venom being applied to the player. (2) The damage figure '6+' is contradicted by the wiki's explicit '4 poison damage every two ticks.' (3) The 'even after they die' claim has no wiki support. The guidance text already correctly recommends Antidote++ (which cures both), so the antidote advice is sound -- but the justification framing it as 'venom...after death' is fabricated. No refutation vector applies: this is not a multi-source or variant issue, there is no account-type nuance, and the wiki page is current.

## Vyrewatch Sentinel

### [medium] C3: The wiki's strategies page lists Amulet of rancour as the top (BIS) neck slot item for both DPS and AFK prayer setups at Vyrewatch Sentinels. Amulet of blood fury is not mentioned anywhere in the strategies page. The guidance step incorrectly names blood fury as BIS.

- **Data says:** an Amulet of blood fury is BIS for the lifesteal
- **Wiki says (raw):** Amulet of rancour" is listed first in the Neck slot for both DPS and AFK Prayer equipment tables.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel/Strategies
- **Suggested fix:** Replace 'an Amulet of blood fury is BIS for the lifesteal' with 'an Amulet of rancour is BIS; Amulet of blood fury is a budget option'.
- **Skeptic receipt:** Strategies page neck slot (DPS): 'Amulet of rancour, Amulet of torture, Amulet of fury, Amulet of strength, Amulet of glory'. Strategies page neck slot (AFK/Prayer): 'Amulet of rancour / Dragonbone necklace, Amulet of torture, Amulet of fury'. Blood fury: not mentioned on the strategies page. Rancour stats: Stab/Slash/Crush +25, Strength +12. Blood fury stats: Stab/Slash/Crush +20, Strength +8.
- **Skeptic reasoning:** The strategies page lists Amulet of rancour as the top neck-slot item in both the DPS and AFK/prayer gear tables. Amulet of blood fury does not appear anywhere on the strategies page. The rancour (released 28 August 2024 with Araxxor) outperforms blood fury on every melee stat: +25 vs +20 attack, +12 vs +8 strength. Calling blood fury BIS is flatly wrong for this content as of current wiki state.

### [medium] C7: The wiki describes Sentinels as patrolling 'the streets of the upper class areas of Darkmeyer', not a 'central Darkmeyer square'. The Darkmeyer page also confirms they patrol 'throughout the middle tier' and 'throughout the upper tier' with 19 spawn locations. The 'central square' framing is not supported by the wiki and could direct players to the wrong area.

- **Data says:** Kill Vyrewatch Sentinels in the central Darkmeyer square.
- **Wiki says (raw):** They can be found prowling the streets of the upper class areas of Darkmeyer, in search of unruly humans to snack on.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel
- **Suggested fix:** Replace 'in the central Darkmeyer square' with 'in the upper-class areas (upper and middle tiers) of Darkmeyer'.
- **Skeptic receipt:** Darkmeyer wiki (middle tier): 'Vyrewatch Sentinels patrolling throughout the middle tier.' Darkmeyer wiki (upper tier): 'Vyrewatch Sentinels patrolling throughout the upper tier.' Bank location: 'A bank' listed under upper tier features. No mention of 'central square' anywhere on the Darkmeyer article. Vyrewatch Sentinel main page: 'They can be found prowling the streets of the upper class areas of Darkmeyer.'
- **Skeptic reasoning:** The phrase 'central Darkmeyer square' does not correspond to any named or described area in Darkmeyer. The Darkmeyer wiki confirms Sentinels patrol two distinct tiers -- the middle tier and the upper tier -- with 19 spawn locations spread across both. There is no 'central square'. The bank (relevant to pathing) is in the upper tier. 'Central square' is geographically inaccurate and could misdirect players away from the upper-tier spawn cluster near the bank.

### [medium] C11: The strategies page does not mention the Amulet of blood fury at all. The recommended BIS neck slot is the Amulet of rancour. The claim that blood fury 'keeps your HP topped without brews' is unsupported and inconsistent with the wiki's gear recommendations for this content.

- **Data says:** Amulet of blood fury speccing keeps your HP topped without brews.
- **Wiki says (raw):** Amulet of rancour" is listed first in the Neck slot for both DPS and AFK Prayer equipment tables.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel/Strategies
- **Suggested fix:** Remove the blood fury claim from the combat step or demote it to a budget option. Reference the Amulet of rancour as the wiki-recommended BIS piece.
- **Skeptic receipt:** Blood fury passive: 'When dealing melee damage, the amulet grants a passive 20% chance to heal 30% of the damage dealt on any hit.' (No special attack exists.) Strategies page neck slot (DPS): 'Amulet of rancour, Amulet of torture, Amulet of fury...' Blood fury: not mentioned anywhere on the strategies page.
- **Skeptic reasoning:** Two independent errors. First, the Amulet of blood fury has no special attack -- it has a passive 20% chance to heal 30% of melee damage dealt. Calling this 'speccing' is a mechanical misstatement. Second, the strategies page does not mention blood fury at all; the wiki-recommended BIS neck slot is the Amulet of rancour for both DPS and sustained AFK play. The claim is wrong on both mechanics and meta.

### [high] C12: The wiki explicitly states Sentinels can be killed 'next to an altar and bank', and the strategies page confirms 'run north to the Bank area. There is a Bank and an Altar.' There is a bank directly in Darkmeyer. Guiding players to teleport via Drakan's medallion to Ver Sinhaza or to Canifis lodestone for banking is strictly worse than walking to the on-site Darkmeyer bank, wasting real time per trip.

- **Data says:** Return to bank when supplies run low. Drakan's medallion to Ver Sinhaza or Canifis lodestone for the fastest bank cycle.
- **Wiki says (raw):** They are often killed off task as a low-effort money maker because they can be killed next to an altar and bank
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vyrewatch_Sentinel
- **Suggested fix:** Replace the banking step with: 'Walk north to the Darkmeyer bank (on-site, next to an altar). The bank and altar are within the Darkmeyer upper tier near the sentinel spawn area.'
- **Skeptic receipt:** Strategies page: 'run north to the Bank area. There is a Bank and an Altar.' Sentinel main page: 'They are often killed off task as a low-effort money maker because they can be killed next to an altar and bank.' Darkmeyer wiki (upper tier): 'A bank' listed as upper-tier feature -- the same tier Sentinels patrol.
- **Skeptic reasoning:** There is a bank directly inside Darkmeyer, in the upper tier where Sentinels patrol. The strategies page explicitly instructs players to 'run north to the Bank area. There is a Bank and an Altar.' The main Sentinel wiki page states they 'can be killed next to an altar and bank.' Guiding players to teleport via Drakan's medallion to Ver Sinhaza or to the Canifis lodestone for banking is strictly worse than using the on-site Darkmeyer bank -- it wastes multiple teleport slots and real time per trip.

## Larran's Big Chest

### [high] C2: The chest is near Pirates' Hideout which the wiki places at Wilderness level 54, not level 39. Level 39 is mid-Wilderness; level 54 is deep Wilderness. This matters for PK risk assessment -- a player preparing for level 39 Wilderness (expecting combat range vulnerability of ~+9 levels) will be severely underprepared for level 54 deep Wilderness exposure.

- **Data says:** Travel to the ship west of Pirates' Hideout (level 39 Wilderness).
- **Wiki says (raw):** Wilderness level: 54
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pirates%27_Hideout
- **Suggested fix:** Change 'level 39 Wilderness' to 'level 54 Wilderness' in guidanceSteps[0].description.
- **Skeptic receipt:** Pirates' Hideout wiki infobox: 'Wilderness level: 54' (for the hideout building east of the ship). Chest coordinates from drop_rates.json line 13511-13513: worldX=3018, worldY=3955, worldPlane=0. Formula: floor((3955-3520)/8)+1 = floor(54.375)+1 = 55. Guidance text from drop_rates.json line 13510: 'Travel to the ship west of Pirates' Hideout (level 39 Wilderness).'
- **Skeptic reasoning:** The guidance text in drop_rates.json reads 'Travel to the ship west of Pirates' Hideout (level 39 Wilderness).' The chest coordinates in the same entry are worldX=3018, worldY=3955. Applying the standard OSRS Wilderness level formula -- floor((Y - 3520) / 8) + 1 -- yields floor((3955 - 3520) / 8) + 1 = floor(54.375) + 1 = 55. Cross-checked against the Pirates' Hideout wiki page, which places the hideout building (slightly east of the ship) at Wilderness level 54. The ship is one level deeper still at level 55. 'Level 39' is mid-Wilderness and is categorically wrong for this location. The finding's proposed fix of 'level 54' is also slightly imprecise -- the chest coordinates resolve to level 55 -- but the core finding (level 39 is wrong; the chest is in deep Wilderness ~54-55) is confirmed. Note: the finding's receipt ('Wilderness level: 54') is from the Pirates' Hideout page (the building), not the chest's own page, which does not state a Wilderness level number. The authoritative anchor is the chest's own worldY=3955 coordinate, which the data entry itself provides.

## Superior Slayer Monster

### [medium] C3: The claim that Slayer level 5 is the minimum to receive any task is wrong. Turael (and Spria) have no Slayer level requirement, so tasks can be received at level 1. Level 5 is the minimum Slayer level required to kill a Crawling Hand (the lowest monster with a superior variant), not to receive any task. The guidance step conflates the lowest superior-unlock monster requirement with a universal task-receiving minimum.

- **Data says:** Slayer level 5 is the minimum to receive any task
- **Wiki says (raw):** All but two Slayer Masters have a Combat level requirement, and the final Slayer Master also has a Slayer level requirement. [Turael has None listed under requirements, meaning tasks are available at level 1 Slayer.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Slayer
- **Suggested fix:** Change 'Slayer level 5 is the minimum to receive any task' to 'Slayer level 5 is the minimum required to kill a Crawling Hand, the lowest monster with a superior variant'.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Slayer_Masters): "They have no minimum combat or Slayer level requirements before players may receive assignments." [Turael/Spria row: Combat=None, Slayer=None, Quest=None]. Wiki (https://oldschool.runescape.wiki/w/Crawling_Hand): "require level 5 Slayer to kill." Wiki (https://oldschool.runescape.wiki/w/Superior_slayer_monster): Lowest superior is Crushing Hand at Slayer level 5 (from Crawling Hand). Data text (drop_rates.json:13582): "Slayer level 5 is the minimum to receive any task"
- **Skeptic reasoning:** The guidance step at drop_rates.json line 13582 states: "Slayer level 5 is the minimum to receive any task." This is factually wrong. The OSRS Wiki Slayer Masters page confirms Turael and Spria have no Slayer level requirement: "They have no minimum combat or Slayer level requirements before players may receive assignments." Tasks are available at Slayer level 1. Slayer level 5 is the minimum to *kill* a Crawling Hand -- which is the lowest monster with a superior variant (its superior is the Crushing Hand) -- not the minimum to receive any task. The guidance text conflates a monster kill requirement with a task-receiving minimum. The fix is narrow: change the sentence to correctly describe why level 5 is relevant in this context (i.e., it is the Slayer level required to kill a Crawling Hand, the lowest superior-variant monster), rather than misstate it as a universal task-receiving floor.

## Zombie Pirate Locker

### [medium] C3: The guidance says keys are dropped 'on the ship,' but the wiki specifies keys are dropped by zombie pirates at the Chaos Temple. The ship is where the lockers are, not where the key-dropping zombie pirates are farmed. There is only a lone single zombie pirate NPC at the shipwreck; the main population (and key drops) are at the temple.

- **Data says:** Zombie pirate keys are dropped by zombie pirates on the ship
- **Wiki says (raw):** Zombie pirate keys are dropped by zombie pirates found at the Wilderness Chaos Temple. They are used to open the pirates' lockers on the shipwreck located on the coast east of the Chaos Temple.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zombie_pirate_key
- **Suggested fix:** Change to: 'Zombie pirate keys are dropped by zombie pirates at the Chaos Temple' (or similar), making clear the key-farming happens at the temple, not on the ship.
- **Skeptic receipt:** wiki_lookup 'Zombie pirate key': "Zombie pirate keys are dropped by zombie pirates found at the Wilderness Chaos Temple. They are used to open the pirates' lockers on the shipwreck located on the coast east of the Chaos Temple."
- **Skeptic reasoning:** The guidance text at line 13624 reads: "Bring zombie pirate keys dropped by zombie pirates on the ship." The phrase 'on the ship' unambiguously places the key-dropping zombie pirates at the shipwreck. The wiki is explicit that keys are dropped at the Chaos Temple, not on the ship: 'Zombie pirate keys are dropped by zombie pirates found at the Wilderness Chaos Temple. They are used to open the pirates' lockers on the shipwreck located on the coast east of the Chaos Temple.' The ship is where the lockers are opened; the temple is where keys are farmed. The guidance text inverts this, sending the player to farm keys at the wrong location.

Tranche 3 total: 65 confirmed findings.
# Tranche 4: sources 111-150 (minigames and activities)

Run summary: 40 sources processed; 36 with confirmed findings; clean: Trouble Brewing, Barracuda Trials, Glough's Experiments, My Notes.

## Hallowed Sepulchre

### [blocker] C6: Floor 5 requires 92 Agility, not 72. The guidance understates the requirement by 20 levels -- a player at 72 Agility cannot access floor 5 at all.

- **Data says:** Run the floors in order: 1, 2, 3, 4 (then 5 at 72+ Agility). Loot every brown coffin you pass for Hallowed marks.
- **Wiki says (raw):** Floor 5: Agility 92 Agility
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hallowed_Sepulchre
- **Suggested fix:** Change '72+ Agility' to '92+ Agility' in guidanceSteps[3].description.
- **Skeptic receipt:** wiki_lookup('Hallowed Sepulchre'): '**Floor 5: [Agility 92 Agility]' (floor requirements list); finding receipt: 'Floor 5: Agility 92 Agility'
- **Skeptic reasoning:** The data at guidanceSteps[3] reads 'then 5 at 72+ Agility'. The authoritative floor-access requirement for floor 5 is 92 Agility. The wiki_lookup on 'Hallowed Sepulchre' returned the requirements list with the entry '**Floor 5:' paired with 'Agility 92 Agility' -- matching the finding's own receipt verbatim. No boostable gate, no account-type variation, no variant item angle applies here. The Ring of endurance equip requirement (70 Agility) and the entry-level requirement (52 Agility) are the only other Agility gates in the Sepulchre context, and neither is 72. The 72 figure does not correspond to any known Sepulchre mechanic. The data understates the requirement by 20 levels.

### [high] C8: Dark dye and Dark acorn are reward shop purchases (300 and 3,000 hallowed marks respectively), not drops from floor 5 grand coffins. Additionally, Strange old lockpick drops from coffins on all floors (1/200 floor 1 through 1/40 floor 5), not exclusively floor 5. Only Ring of endurance is floor-5 grand coffin exclusive. The guidance lumps all four items together incorrectly.

- **Data says:** Strange old lockpick / Ring of endurance / Dark dye / Dark acorn rare drops only roll on floor 5 grand coffins, so prioritise getting to floor 5 every run once you can.
- **Wiki says (raw):** It can be purchased from the Mysterious Hallowed Goods shop for 300 hallowed marks. [Dark dye] / Dark acorn [cost] 3,000 [hallowed marks]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Dark_dye
- **Suggested fix:** Revise guidanceSteps[3].description to reflect that only Ring of endurance is an exclusive floor-5 grand coffin drop; Strange old lockpick drops from all floors (better odds on higher floors); Dark dye and Dark acorn are purchased from the reward shop with hallowed marks.
- **Skeptic receipt:** wiki_lookup('Dark dye'): 'It can be purchased from the Mysterious Hallowed Goods shop for 300 hallowed marks.' | wiki_lookup('Dark acorn'): 'It is purchased from the Mysterious Stranger's Mysterious Hallowed Goods shop for 3,000 hallowed marks.' | wiki_lookup('Strange old lockpick'): 'The drop rate is 1/200 for the 1st floor, 1/120 for the 2nd floor, 1/90 for the 3rd floor, 1/60 for the 4th floor and 1/40 for the 5th floor.' | wiki_lookup('Ring of endurance'): 'a potential reward from the Grand Hallowed Coffin on the final floor of the Hallowed Sepulchre'
- **Skeptic reasoning:** The data at guidanceSteps[3] states: 'Strange old lockpick / Ring of endurance / Dark dye / Dark acorn rare drops only roll on floor 5 grand coffins'. Three of the four items are wrong: (1) Dark dye is a shop purchase at 300 hallowed marks, not a coffin drop; (2) Dark acorn is a shop purchase at 3,000 hallowed marks, not a coffin drop; (3) Strange old lockpick drops from coffins on ALL five floors at rates 1/200 (floor 1) through 1/40 (floor 5), not exclusively floor 5. Only Ring of endurance is correctly characterised as a floor-5 grand coffin exclusive. The later guidanceStep[5] already corrects this ('The Ring of endurance and Strange old lockpick come from the floor-5 grand coffins; everything else is bought with marks') but step[3] still contains the false claim and would mislead players reading only that step. The multi-source and variant-item refutation vectors do not apply -- this is a plain factual error about drop mechanism and floor exclusivity.

### [high] C4: The guidance says the Mausoleum Door is in the south-east of Darkmeyer, but the wiki states the Hallowed Sepulchre is on the north-east side of Darkmeyer. The directional claim is inverted.

- **Data says:** Head to the Mausoleum Door in the south-east of Darkmeyer (south of the bank, near the canal).
- **Wiki says (raw):** The Hallowed Sepulchre is located on the north east side of Darkmeyer.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hallowed_Sepulchre
- **Suggested fix:** Change 'south-east of Darkmeyer' to 'north-east of Darkmeyer' in guidanceSteps[1].description.
- **Skeptic receipt:** wiki_lookup('Hallowed Sepulchre'): 'The Hallowed Sepulchre is located on the north east side of Darkmeyer.' | wiki_lookup('Mysterious Hallowed Goods'): 'found on the north-eastern corner of Darkmeyer.'
- **Skeptic reasoning:** The data at guidanceSteps[1] reads 'Head to the Mausoleum Door in the south-east of Darkmeyer'. The wiki_lookup on 'Hallowed Sepulchre' directly states 'The Hallowed Sepulchre is located on the north east side of Darkmeyer.' The wiki_lookup on 'Mysterious Hallowed Goods' (the reward shop at the same complex) further states it is 'found on the north-eastern corner of Darkmeyer.' Both independent wiki pages agree on north-east. The data's 'south-east' is a direct directional inversion. No account-type or progress-gate nuance applies to entrance location.

## Guardians of the Rift

### [medium] C3: The plugin restricts the pickaxe requirement to 'rune or better', but the wiki specifies any pickaxe is sufficient. Players with lower-tier pickaxes (bronze through adamant) are incorrectly told their pickaxe is inadequate.

- **Data says:** A Pickaxe (rune or better) is needed for mining the altar fragments
- **Wiki says (raw):** "Any [pickaxe](/w/Pickaxe)"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Change pickaxe description from 'rune or better' to 'any pickaxe'.
- **Skeptic receipt:** Wiki (wiki_lookup + WebFetch): "Any pickaxe" -- listed as a requirement with no tier qualifier. Data line 14063: "a Pickaxe (rune or better) for mining the altar fragments".
- **Skeptic reasoning:** The wiki states the pickaxe requirement is 'Any pickaxe' with no tier restriction. The guidance step at line 14063 of drop_rates.json says 'a Pickaxe (rune or better) for mining the altar fragments', which incorrectly excludes bronze through adamant pickaxes. The wiki_lookup and WebFetch both confirm the unrestricted requirement.

### [high] C6: The wiki's travel section lists seven routes to reach Guardians of the Rift and does not include any Arceuus teleport. Directing players to use an Arceuus teleport would send them to the wrong location.

- **Data says:** Arceuus teleport leads to Temple of the Eye
- **Wiki says (raw):** Travel methods listed: "The amulet of the eye teleport", "A Minigame Teleport through any spellbook or the Grouping tab to Guardians of the Rift", "A necklace of passage teleport to the Wizards' Tower", "Fairy ring code DIS to the Wizards' Tower", "An amulet of glory teleport to Draynor Village and walking south", "Lumbridge Home Teleport and walking south-west", "Teleporting to the Scar via ring of shadows, requiring Desert Treasure II"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Remove the Arceuus teleport claim. Use one of the wiki-confirmed routes: amulet of the eye, minigame teleport, necklace of passage to Wizards' Tower, or fairy ring DIS.
- **Skeptic receipt:** Wiki (wiki_lookup): travel methods are 'The amulet of the eye teleport', 'A Minigame Teleport through any spellbook or the Grouping tab to Guardians of the Rift', 'A necklace of passage teleport to the Wizards' Tower', 'Fairy ring code DIS to the Wizards' Tower', 'An amulet of glory teleport to Draynor Village and walking south', 'Lumbridge Home Teleport and walking south-west', 'Teleporting to the Scar via ring of shadows, requiring Desert Treasure II'. No Arceuus teleport listed. Data line 14054: "travelTip": "Arceuus teleport -> Temple".
- **Skeptic reasoning:** The top-level travelTip field (line 14054) says 'Arceuus teleport -> Temple'. The Arceuus spellbook contains no teleport to the Wizards' Tower or Temple of the Eye. The wiki lists seven travel methods and none involve any Arceuus spellbook teleport. This would misdirect players.

### [high] C7: Neither an Arceuus library teleport nor a Mind altar teleport appears anywhere in the wiki's travel section for this minigame. Both routes are fabricated.

- **Data says:** Arceuus library teleport is a viable route to Temple of the Eye (via Wizards' Tower portal or Mind altar teleport)
- **Wiki says (raw):** Travel methods listed: "The amulet of the eye teleport", "A Minigame Teleport through any spellbook or the Grouping tab to Guardians of the Rift", "A necklace of passage teleport to the Wizards' Tower", "Fairy ring code DIS to the Wizards' Tower", "An amulet of glory teleport to Draynor Village and walking south", "Lumbridge Home Teleport and walking south-west", "Teleporting to the Scar via ring of shadows, requiring Desert Treasure II"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Remove both the Arceuus library teleport and Mind altar teleport claims. Replace with wiki-confirmed routes.
- **Skeptic receipt:** Wiki (wiki_lookup): seven travel methods listed -- no Arceuus library teleport. Data line 14079: "Arceuus library teleport + run south-west or Mind altar teleport is also viable". Data line 14085 travelTip: "Arceuus library tele -> Temple of the Eye, or Mind altar tele".
- **Skeptic reasoning:** Guidance step 2 (line 14079) says 'Arceuus library teleport + run south-west or Mind altar teleport is also viable', and its travelTip (line 14085) repeats 'Arceuus library tele -> Temple of the Eye, or Mind altar tele'. The Arceuus library teleport does not land near Wizards' Tower or the Temple of the Eye. Neither route appears in the wiki's travel section. This is the same underlying data error as C6 expressed in a guidance step rather than the top-level travelTip field.

### [high] C8: The Mind altar teleport does not appear in the wiki's travel methods for Guardians of the Rift. The Mind altar is not near the Temple of the Eye and this route would mislead players.

- **Data says:** Mind altar teleport is a viable route to Temple of the Eye
- **Wiki says (raw):** Travel methods listed: "The amulet of the eye teleport", "A Minigame Teleport through any spellbook or the Grouping tab to Guardians of the Rift", "A necklace of passage teleport to the Wizards' Tower", "Fairy ring code DIS to the Wizards' Tower", "An amulet of glory teleport to Draynor Village and walking south", "Lumbridge Home Teleport and walking south-west", "Teleporting to the Scar via ring of shadows, requiring Desert Treasure II"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Remove Mind altar teleport. Use minigame teleport or necklace of passage to Wizards' Tower instead.
- **Skeptic receipt:** Wiki (wiki_lookup): seven travel methods listed -- no Mind altar teleport. Data line 14079: "Mind altar teleport is also viable". Data line 14085 travelTip: "Arceuus library tele -> Temple of the Eye, or Mind altar tele".
- **Skeptic reasoning:** The Mind altar teleport (Arceuus spellbook) deposits the player at the Mind altar, which is north of Falador. The Wizards' Tower is south of Draynor -- these are on opposite sides of the map. The wiki's travel section contains no Mind altar teleport among the seven listed methods. This is part of the same guidance step error as C6/C7.

### [blocker] C12: The 150-point threshold is outdated. The wiki explicitly states it was increased to 300. Players following this guidance would underperform thinking 150 points is enough when the current threshold is 300.

- **Data says:** Aiming for 150+ total points per round rolls the unique table
- **Wiki says (raw):** "The minimum points threshold required to receive the large experience drop at the end of the minigame has been increased from 150 to 300."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Update the point threshold from 150 to 300 to reflect the current game state.
- **Skeptic receipt:** Wiki (WebFetch): "The minimum points threshold required to receive the large experience drop at the end of the minigame has been increased from 150 to 300." Data line 14099: "Aim for 150+ total points per round to roll the unique table".
- **Skeptic reasoning:** The wiki explicitly states the threshold was updated from 150 to 300. The guidance step (line 14099) still says 'Aim for 150+ total points per round to roll the unique table', which is the old value. This is a stale guidance claim contradicted by an explicit wiki patch note.

### [medium] C14: The wiki lists intricate pouch rewards as tarnished locket, lost bag, runes, dragon items, and hard clue. Abyssal pearls are not listed as a possible reward from intricate pouches. The 'rare uniques' framing is not verified.

- **Data says:** Intricate pouches obtained during the minigame can be handed in for bonus rewards (Abyssal pearls / rare uniques)
- **Wiki says (raw):** "Can give a tarnished locket or a lost bag. Grants a random amount of runes or one of several dragon items. Also has a chance at granting a hard clue."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Guardians_of_the_Rift
- **Suggested fix:** Remove Abyssal pearls from the list of intricate pouch rewards. Correct to: tarnished locket, lost bag, runes, dragon items, hard clue scroll.
- **Skeptic receipt:** Wiki (WebFetch, Intricate pouch page): reward list is blood essence (1/200), lamp (1/15), tarnished locket (1/40), lost bag (1/60), rune quantities (astral/blood/chaos/cosmic/death/law/nature/soul), shield left half, dragon spear, crystal key, dragon med helm, pure essence (500-1000 noted), clue scroll hard (1/10). No Abyssal pearls listed. Data line 14119: 'Abyssal pearls / rare uniques'.
- **Skeptic reasoning:** The wiki's Intricate pouch page lists every possible reward: blood essence, lamp, tarnished locket, lost bag, rune quantities, shield left half, dragon spear, crystal key, dragon med helm, pure essence, and clue scroll (hard). Abyssal pearls do not appear. Guidance step at line 14119 says 'Hand in any Intricate pouches you obtained to receive bonus rewards (Abyssal pearls / rare uniques)', which incorrectly attributes Abyssal pearl drops to intricate pouch hand-ins. Abyssal pearls are earned from the Rewards Guardian after a completed game, not from opening intricate pouches.

## Brimhaven Agility Arena

### [blocker] C7: Wrong NPC and wrong cost. The wiki names Pirate Jackie the Fruit as the reward exchanger (not Cap'n Izzy No-Beard). The graceful recolour costs 250 vouchers for the full set, not 250 per piece -- the guidance overstates the cost by a factor of 6.

- **Data says:** Exchange agility tickets with Cap'n Izzy No-Beard for Brimhaven Graceful recolours (250 tickets each piece) or the Pirate's hook (800 tickets). The full Graceful set takes 1500 tickets total
- **Wiki says (raw):** The tickets and vouchers can be traded in to Pirate Jackie the Fruit for Agility experience and a selection of items respectively. ... The graceful recolour costs 250 Brimhaven vouchers to recolour the full uncoloured graceful outfit.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Brimhaven_Agility_Arena
- **Suggested fix:** Replace 'Cap'n Izzy No-Beard' with 'Pirate Jackie the Fruit' as the exchange NPC. Change '250 tickets each piece' to '250 tickets for the full set'.
- **Skeptic receipt:** wiki_lookup (Brimhaven Agility Arena): 'The tickets and vouchers can be traded in to Pirate Jackie the Fruit for Agility experience and a selection of items respectively.' | Agility Arena Store page: 'Graceful Recolour -- 250 vouchers' (single line item, full set). | Cap'n Izzy No-Beard page: collects 200-coin entry fee and sells Agility cape only -- no ticket/voucher exchange role.
- **Skeptic reasoning:** Two distinct errors confirmed by authoritative sources. (1) NPC: The wiki_lookup raw output for Brimhaven Agility Arena states verbatim: 'The tickets and vouchers can be traded in to Pirate Jackie the Fruit for Agility experience and a selection of items respectively.' Cap'n Izzy No-Beard's wiki page confirms his only roles are collecting the 200-coin entry fee and selling the Agility cape -- he does not exchange tickets or vouchers for rewards. (2) Cost: The Agility Arena Store wiki page lists 'Graceful Recolour' as a single line item at 250 vouchers for the full set, not 250 per piece. The data's '250 tickets each piece' assertion and the derived '1500 tickets total' are both wrong. The finding's two sub-claims survive all refutation vectors: this is not a multi-source item issue, not a variant issue, not account-type dependent, and the wiki is current.

### [high] C8: Wrong NPC. The Pirate's hook is purchased from Pirate Jackie the Fruit, not Cap'n Izzy No-Beard. The 800 voucher cost is correct.

- **Data says:** Exchange agility tickets with Cap'n Izzy No-Beard for Brimhaven Graceful recolours (250 tickets each piece) or the Pirate's hook (800 tickets).
- **Wiki says (raw):** It can be purchased from Pirate Jackie the Fruit for 800 Brimhaven vouchers.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pirate%27s_hook
- **Suggested fix:** Replace 'Cap'n Izzy No-Beard' with 'Pirate Jackie the Fruit' as the exchange NPC for the Pirate's hook.
- **Skeptic receipt:** Pirate's hook wiki page: 'It can be purchased from Pirate Jackie the Fruit for 800 Brimhaven vouchers.' | Cap'n Izzy No-Beard wiki page: entry fee collector and Agility cape vendor only -- no voucher exchange.
- **Skeptic reasoning:** The wrong-NPC sub-claim is confirmed. The Pirate's hook wiki page states: 'It can be purchased from Pirate Jackie the Fruit for 800 Brimhaven vouchers.' The data attributes the exchange to Cap'n Izzy No-Beard, who has no exchange role per his wiki page. The 800-voucher cost is correct. This finding is narrower than C7 but the NPC error is real and independently confirmed by the Pirate's hook page.

### [high] C9: The full Brimhaven Graceful recolour costs 250 vouchers for the entire set, not 1500. The data's 1500 figure comes from incorrectly treating 250 as a per-piece cost multiplied across 6 pieces -- that per-piece assumption is wrong.

- **Data says:** The full Graceful set takes 1500 tickets total
- **Wiki says (raw):** The graceful recolour costs 250 Brimhaven vouchers to recolour the full uncoloured graceful outfit.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Brimhaven_Agility_Arena
- **Suggested fix:** Change 'The full Graceful set takes 1500 tickets total' to 'The full Graceful set takes 250 tickets total'.
- **Skeptic receipt:** Agility Arena Store wiki page: 'Graceful Recolour -- 250 vouchers' (single line item; the page does not list separate per-piece costs). | Brimhaven Agility Arena wiki page: 'The graceful recolour costs 250 Brimhaven vouchers to recolour the full uncoloured graceful outfit.'
- **Skeptic reasoning:** The Agility Arena Store wiki page lists 'Graceful Recolour' as a single line item costing 250 vouchers -- not per piece, but for the full set bundle. The data's '1500 tickets total' figure is derived from incorrectly treating 250 as a per-piece cost across 6 pieces (6 x 250 = 1500). The actual cost is 250 vouchers for the complete recolour. This is not a multi-source ambiguity, variant issue, or account-type gate -- it is a flat factual error confirmed by the store's exchange table. The finding survives all refutation vectors.

## Giants' Foundry

### [high] C5: The item is called 'Double ammo mould' (not 'Ammo mould'), it is purchased FROM the Foundry shop for 2,000 reputation -- it is not brought in to receive reputation. The wiki contains no mention of the mould granting 'bonus reputation'. The claim inverts the direction of the reputation transaction.

- **Data says:** An Ammo mould can be brought to receive bonus reputation
- **Wiki says (raw):** The Double ammo mould costs 2,000 Foundry Reputation points. Can only be purchased if the player has completed the Dwarf Cannon quest.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Giants'_Foundry
- **Suggested fix:** Correct to: 'The Double ammo mould can be purchased from the Foundry Reward Shop for 2,000 Foundry Reputation (requires Dwarf Cannon quest).'
- **Skeptic receipt:** Rewards table (direct wiki fetch, oldschool.runescape.wiki/w/Giants'_Foundry): 'Double ammo mould | 2,000 [Foundry Reputation] | Can only be purchased if the player has completed the Dwarf Cannon quest.' Double ammo mould item page (direct fetch): 'makes cannonballs twice as fast compared to a regular ammo mould, making 8 cannonballs rather than 4 (and using 2 metal bars rather than 1) per action.' No text on either page describes any mould being brought to the foundry to receive reputation.
- **Skeptic reasoning:** The data claim contains two independent errors that both survive every refutation vector. First, the item name is wrong: the wiki names it 'Double ammo mould', not 'Ammo mould'. Second, and more critically, the direction of the reputation transaction is fully inverted: the Double ammo mould is purchased FROM the Foundry Reward Shop for 2,000 Foundry Reputation -- a player spends reputation to acquire it. The wiki contains no mechanic, on the main page or the Strategies page, in which any mould is brought to the foundry to receive or generate bonus reputation. The item's actual function (once purchased) is faster cannonball production outside the minigame entirely. No multi-source item argument applies (this is a shop purchase, not a drop). No variant-ID ambiguity applies (the mould is not in the collection log as a drop). The finding stands on direct wiki evidence.

### [high] C7: Falador teleport is not listed among the travel methods on the wiki. Giants' Foundry is located in the Kharidian Desert area (Giants' Plateau), and Falador is in the far north-west of Misthalin -- running south-east from Falador does not lead to Giants' Foundry. The wiki's fallback routes are Amulet of glory to Al Kharid, Ring of dueling to Emir's Arena, gnome glider, or Lumbridge toll gate.

- **Data says:** Falador teleport followed by running south-east is an unquested fallback route to Giants' Foundry
- **Wiki says (raw):** The Giants' Foundry is located in a cave beneath the Giants' Plateau. Players can get there by using: Using Minigame Teleport through the spellbook or the Grouping tab to the Giants' Foundry; An amulet of glory teleport to the Al Kharid Palace; A ring of dueling teleport to the Emir's Arena; The gnome glider flight to Kar-Hewo; The toll gate from Lumbridge.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Giants'_Foundry
- **Suggested fix:** Replace Falador teleport fallback with a correct alternative such as: 'Amulet of glory to Al Kharid Palace, then run south' or 'Ring of dueling to Emir's Arena, then run west.'
- **Skeptic receipt:** wiki_lookup raw output (Giants' Foundry, direct MCP call): 'The Giants' Foundry is located in a cave beneath the Giants' Plateau. Players can get there by using: Using Minigame Teleport through the spellbook or the Grouping tab to the Giants' Foundry; An amulet of glory teleport to the Al Kharid Palace; A ring of dueling teleport to the Emir's Arena; The gnome glider flight to Kar-Hewo; The toll gate from Lumbridge.' Falador does not appear. wiki_lookup infobox: 'Location: Giants' Plateau | leagueRegion = Desert'. Strategies page fetch: no mention of Falador or any travel routes.
- **Skeptic reasoning:** Falador is in northern Asgarnia (~2965, 3380). Giants' Foundry is beneath Giants' Plateau in the Kharidian Desert, east of Al Kharid (~3295, 3110) -- over 400 tiles away in the opposite direction. Running south-east from Falador leads through Draynor and Lumbridge, not to Al Kharid or the Desert. The wiki's exhaustive travel list -- fetched directly -- makes no mention of Falador in any form. All four listed unquested fallback options (Amulet of glory to Al Kharid Palace, Ring of dueling to Emir's Arena, gnome glider to Kar-Hewo, toll gate from Lumbridge) are desert-adjacent or direct, not Falador-adjacent. The C7 finding is not a misread of the wiki quote; the Falador route is simply absent from the wiki and is geographically incoherent for this destination.

## Pest Control

### [blocker] C4: Novice lander minimum is combat level 40 (not 'below 100'). The data frames this as an upper bound when the wiki defines it as a lower bound minimum.

- **Data says:** Novice lander is for combat level below 100
- **Wiki says (raw):** The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pest_Control
- **Suggested fix:** Change Novice description to 'combat level 40+' (minimum 40).
- **Skeptic receipt:** "The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively." -- https://oldschool.runescape.wiki/w/Pest_Control
- **Skeptic reasoning:** The wiki defines the Novice lander minimum as combat level 40 with no upper bound. Describing it as 'below 100' inverts the mechanic entirely -- it is a lower-bound entry requirement, not an upper-bound cap. No wiki text supports a maximum combat level for any lander. The framing 'below 100' would mislead a level 40-99 player into thinking they cannot board the Novice lander after reaching 100, which is false.

### [blocker] C5: Intermediate lander minimum is combat level 70, not 100-150. The claimed range is wrong on both ends.

- **Data says:** Intermediate lander is for combat level 100-150
- **Wiki says (raw):** The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pest_Control
- **Suggested fix:** Change Intermediate description to 'combat level 70+' (minimum 70).
- **Skeptic receipt:** "The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively." -- https://oldschool.runescape.wiki/w/Pest_Control
- **Skeptic reasoning:** The wiki specifies combat level 70 as the Intermediate lander minimum. The data claims '100-150', which puts the lower bound at the Veteran threshold (100) and invents an upper bound of 150 that has no wiki basis whatsoever. Both ends of the stated range are wrong.

### [blocker] C6: Veteran lander minimum is combat level 100, not 150. The claimed threshold of 150 is 50 levels too high and would misdirect players.

- **Data says:** Veteran lander is for combat level 150 and above
- **Wiki says (raw):** The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pest_Control
- **Suggested fix:** Change Veteran description to 'combat level 100+' (minimum 100).
- **Skeptic receipt:** "The only requirement to participate in Pest Control is a combat level of 40, 70 or 100 for Novice, Intermediate and Veteran games respectively." -- https://oldschool.runescape.wiki/w/Pest_Control
- **Skeptic reasoning:** The wiki specifies combat level 100 as the Veteran lander minimum. The data claims 150+, which is 50 levels too high and has no basis in any wiki text. A player at combat level 100-149 would be wrongly told they cannot access the Veteran lander.

### [high] C8: Spinners prioritise healing portals, not swarming the Void Knight. The claimed behaviour is factually wrong and would cause players to misunderstand when and why Spinners are dangerous.

- **Data says:** Spinners swarm near the Void Knight during combat
- **Wiki says (raw):** Spinners repair nearby portals at a rapid speed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pest_Control/Strategies
- **Suggested fix:** Change description to reflect that Spinners repair/heal portals and must be killed to make portals vulnerable again.
- **Skeptic receipt:** Spinner page: "Their role is to repair the portal they came from, and they should be defeated if the portals are to be destroyed quickly." Strategies page: "Spinners repair nearby portals at a rapid speed. Since they often prevent players from destroying the portals quickly...they are the first priority to kill."
- **Skeptic reasoning:** Both the Spinner wiki page and the Strategies page are unambiguous: Spinners repair portals, they do not swarm the Void Knight. The Spinner page states 'Their role is to repair the portal they came from.' The Strategies page states 'Spinners repair nearby portals at a rapid speed.' Neither source places Spinners near the Void Knight. The data's described behavior is factually incorrect.

### [high] C10: The wiki's strategy page explicitly inverts this: Spinners must be killed first because they can render portals invulnerable. Prioritising portals over Spinner kills is contra-indicated.

- **Data says:** Portals should be prioritized over kills in strategy
- **Wiki says (raw):** Spinners are the first priority to kill, even if the portal they surround is still protected. Multiple Spinners will stack their repair effects, rendering a portal effectively invulnerable.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pest_Control/Strategies
- **Suggested fix:** Change guidance to: kill Spinners first (they heal portals and stack their repair effects making portals invulnerable), then destroy the portal.
- **Skeptic receipt:** "Spinners are the first priority to kill, even if the portal they surround is still protected. Multiple Spinners will stack their repair effects, rendering a portal effectively invulnerable." -- https://oldschool.runescape.wiki/w/Pest_Control/Strategies
- **Skeptic reasoning:** The Strategies page explicitly inverts the data's stated priority. The wiki says Spinners must be killed first -- even before the portal becomes vulnerable -- because stacked Spinner healing renders a portal effectively invulnerable. Guidance that tells players to prioritize portals over Spinner kills directly contradicts this and would cause game-losing behavior.

## Mage Training Arena

### [medium] C3: Direction from Emir's Arena is north, not north-east. The wiki states the arena is 'located to the north of the Emir's Arena' and the travel method is 'running north', not running north-east.

- **Data says:** Ring of dueling to Emir's Arena is fastest; alternatively teleport to Al Kharid and run north-east past the Duel Arena ruins
- **Wiki says (raw):** A ring of dueling teleport to the Al Kharid Emir's Arena and running north
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mage_Training_Arena
- **Suggested fix:** Change 'run north-east' to 'run north' in guidanceSteps[0].description and travelTip. The arena is north of Emir's Arena, not north-east.
- **Skeptic receipt:** wiki_lookup raw: 'A ring of dueling teleport to the Al Kharid Emir\'s Arena and running north.' and 'The Mage Training Arena is located north of the Emir\'s Arena and north-east of Al Kharid.'
- **Skeptic reasoning:** The travelTip (line 14748) and guidanceSteps[0].description (line 14742) both say 'run north-east' after the Ring of Dueling teleport to Emir's Arena. The wiki is unambiguous that the direction from Emir's Arena is north, not north-east. The 'north-east' geography is correct only for the direction from Al Kharid itself. The data conflates two distinct geographic facts and gives the wrong compass bearing for the Ring of Dueling route specifically.

### [medium] C7: The Enchanting Chamber accepts all enchant spell levels (Lvl-1 through Lvl-7), and players earn more pizazz points per cast at higher levels. Advising 'Lvl-1 Enchant' is misleading -- players should use the highest enchant spell available.

- **Data says:** Enchanting: Lvl-1 Enchant jewelry
- **Wiki says (raw):** Players will receive 1 Enchantment Pizazz point per level of Enchantment spell cast. The article indicates players should use 'The highest enchant jewellery spell' they have access to, ranging from Level 1 through Level 7 Enchant.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mage_Training_Arena
- **Suggested fix:** Change 'Lvl-1 Enchant jewelry' to 'Enchant jewelry (highest spell level available)' or similar to reflect that higher-level enchants yield more pizazz points per cast.
- **Skeptic receipt:** wiki_lookup raw: 'The highest enchant jewellery spell (Lvl-1 Enchant, Lvl-2 Enchant, Lvl-3 Enchant, Lvl-4 Enchant, Lvl-5 Enchant, Lvl-6 Enchant, optionally Lvl-7 Enchant) the player has access to'
- **Skeptic reasoning:** The guidance step at line 14752 lists 'Enchanting: Lvl-1 Enchant jewelry' as the Enchanting room activity. Read in full context this is a room-description line, not a minimum-entry note, and it names a specific spell rather than 'use the highest available.' The wiki explicitly states players should use the highest enchant spell they have access to, since pizazz points equal the spell level per cast. Naming Lvl-1 as the spell to use is genuinely misleading for any account above level 7 Magic.

### [medium] C8: The Graveyard room does NOT require Bones to Peaches -- Bones to Bananas also works and is available without any unlock. The claim implies the room is gated behind Bones to Peaches, which is incorrect.

- **Data says:** Graveyard (needs Bones to Peaches unlocked): convert bones
- **Wiki says (raw):** collect bones from the ground, convert into fruit using Bones to Bananas or Bones to Peaches, and deposit in food chutes
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mage_Training_Arena
- **Suggested fix:** Remove the '(needs Bones to Peaches unlocked)' gate from the Graveyard room description. Bones to Bananas is sufficient. The Bones to Peaches note is only relevant for the efficiency tip in step 2/3.
- **Skeptic receipt:** wiki_lookup raw: 'Bones To Bananas or Bones To Peaches' (listed as the two valid spells, no gate implied). Creature Graveyard page: 'use the Bones to Bananas or Bones to Peaches spells to turn them into bananas or peaches respectively' with no entry requirement stated.
- **Skeptic reasoning:** The guidance step at line 14752 says 'Graveyard (needs Bones to Peaches unlocked): convert bones', implying the room is gated behind the Bones to Peaches unlock. The wiki and the Creature Graveyard page both confirm Bones to Bananas also works with no unlock required. The room is not gated -- Bones to Peaches is an efficiency unlock, not an access requirement. The parenthetical phrasing 'needs Bones to Peaches unlocked' is factually incorrect as a gate description.

### [blocker] C11: The Master wand costs hundreds to thousands of pizazz points per type, not '17 pizazz of each type'. This is completely wrong by multiple orders of magnitude and would severely mislead players about the effort required.

- **Data says:** The Master wand (17 pizazz of each type) and Mage's book are the primary targets
- **Wiki says (raw):** the master wand...the price of 480 Telekinetic, 480 Graveyard, 4800 Enchantment, and 530 Alchemist pizazz points. [full upgrade path]; Master wand (Upgrade): 240 Telekinetic, 240 Graveyard, 2400 Enchantment, 240 Alchemist pizazz points
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mage_Training_Arena
- **Suggested fix:** Replace '17 pizazz of each type' with the correct costs. The upgrade cost (requiring Teacher wand in inventory) is 240 Telekinetic, 240 Graveyard, 2400 Enchantment, and 240 Alchemist pizazz points. The full cumulative cost from scratch is 480 Telekinetic, 480 Graveyard, 4800 Enchantment, and 530 Alchemist pizazz points.
- **Skeptic receipt:** Master wand wiki page: 'From scratch, buying the master wand and all of its three prerequisite wands requires: 480 Telekinetic pizazz points, 530 Alchemist pizazz points, 4800 Enchantment pizazz points, 480 Graveyard pizazz points.' Upgrade cost (with Teacher wand): '240 Telekinetic, 240 Alchemist, 2400 Enchantment, 240 Graveyard pizazz points.'
- **Skeptic reasoning:** The guidance step at line 14773 tells players 'The Master wand (17 pizazz of each type)'. The value 17 is the plugin's internal pointCost field for the Master wand item entry -- it is an artefact of the plugin's data model, not the in-game pizazz cost. The actual cost to obtain the Master wand (via the upgrade path with Teacher wand in inventory) is 240 Telekinetic, 240 Graveyard, 2400 Enchantment, and 240 Alchemist pizazz points. The cumulative cost from scratch is 480/480/4800/530. '17 pizazz of each type' is wrong by more than an order of magnitude and would severely mislead players about the effort required.

## Shades of Mort'ton

### [medium] C9: The reward pillar/pedestal where shade keys appear is located near the funeral pyres (south and west sides of town), not inside the temple. The temple is north of town; the pyres are on the south and west sides. A player following this guidance step would look inside the temple for a key that actually spawns at the pyre where they just burned the shade remains.

- **Data says:** Pick up your shade key from the reward pillar inside the temple.
- **Wiki says (raw):** An animation of a shade's spirit will float up into the air, then either a key or a stack of coins will appear on the pedestal nearby.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Shades_of_Mort%27ton_(minigame)
- **Suggested fix:** Change description to: "Pick up your shade key from the pedestal next to the funeral pyre." Remove the "inside the temple" qualifier -- the reward appears at the pyre location, not inside the temple.
- **Skeptic receipt:** Mort'ton wiki (https://oldschool.runescape.wiki/w/Mort%27ton): 'North of the town is the Flamtaer temple' / 'On the south and west sides of the town are funeral pyres for burning shade remains.' Funeral pyre wiki (https://oldschool.runescape.wiki/w/Funeral_pyre): 'This will burn the remains and cause a reward (either coins or a shade key) to appear on the nearby stone stand.' Shades of Mort'ton minigame wiki: 'An animation of a shade's spirit will float up into the air, then either a key or a stack of coins will appear on the pedestal nearby.'
- **Skeptic reasoning:** Three independent wiki sources establish that the reward pedestal is adjacent to the funeral pyre, and that pyres are geographically separate from the temple. The Mort'ton wiki states verbatim: 'North of the town is the Flamtaer temple' and 'On the south and west sides of the town are funeral pyres for burning shade remains.' The Funeral pyre wiki states rewards appear on 'the nearby stone stand' -- nearby the pyre, not inside the temple. The guidance step says 'Pick up your shade key from the reward pillar inside the temple', which directs the player to the wrong location: the temple is north of town while the reward pedestal is adjacent to the pyres on the south and west edges. No refutation vector applies: this is not a multi-source clog item issue, not a variant ID issue, not account-type-dependent, and the wiki has not changed recently (zero edits in the past 7 days). The contradiction is direct and geography-specific.

## Mahogany Homes

### [high] C5: All three tier names in the guidance are wrong. The wiki defines four tiers: Beginner (1), Novice (20), Adept (50), Expert (70). There is no Master tier. The guidance calls the level-20 tier 'Adept' (should be Novice), the level-50 tier 'Expert' (should be Adept), and invents a non-existent 'Master' tier at level 70 (should be Expert).

- **Data says:** Higher Construction levels unlock the Carpenter (Adept 20+, Expert 50+, Master 70+) tiers
- **Wiki says (raw):** Higher tier contracts are unlocked with higher Construction levels. Beginner contracts can be started at level 1, Novice at level 20, Adept at level 50 and Expert at level 70.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mahogany_Homes
- **Suggested fix:** Replace '(Adept 20+, Expert 50+, Master 70+)' with '(Novice 20+, Adept 50+, Expert 70+)' to match the four wiki-defined tiers.
- **Skeptic receipt:** wiki_lookup raw output: 'Four types of contract are available: Beginner (level 1), Novice (level 20), Adept (level 50), and Expert (level 70).' WebFetch: 'Beginner (level 1), Novice (level 20), Adept (level 50), Expert (level 70).' No 'Master' tier appears anywhere in either source.
- **Skeptic reasoning:** The data labels the level-20 tier 'Adept', the level-50 tier 'Expert', and invents a 'Master' tier at level 70. The wiki and the raw wiki_lookup output both define exactly four tiers with specific names: Beginner (1), Novice (20), Adept (50), Expert (70). 'Master' does not exist anywhere on the wiki page. All three tier names in the guidance are wrong. The level numbers are correct but assigned to wrong names.

### [medium] C6: The tier names cited (Adept / Expert / Master) are all wrong -- the correct names are Novice / Adept / Expert. 'Master' does not exist. The underlying mechanic (higher tiers unlocking contracts beyond Falador) may be correct, but the tier names are fabricated.

- **Data says:** Higher tiers (Adept / Expert / Master) ignore the starter Falador-only contracts.
- **Wiki says (raw):** Higher tier contracts are unlocked with higher Construction levels. Beginner contracts can be started at level 1, Novice at level 20, Adept at level 50 and Expert at level 70.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mahogany_Homes
- **Suggested fix:** Replace '(Adept / Expert / Master)' with '(Novice / Adept / Expert)' to use the correct wiki tier names.
- **Skeptic receipt:** wiki_lookup raw output: 'Four types of contract are available: Beginner (level 1), Novice (level 20), Adept (level 50), and Expert (level 70).' WebFetch confirms identical tier list. 'Master' appears in neither source.
- **Skeptic reasoning:** The guidance reads 'Higher tiers (Adept / Expert / Master) ignore the starter Falador-only contracts.' The correct tier names for levels above Beginner are Novice, Adept, and Expert. 'Master' does not exist. 'Adept' in the guidance maps to the level-20 slot (correct name: Novice), 'Expert' maps to level-50 (correct name: Adept), and 'Master' maps to level-70 (correct name: Expert). All three names are shifted by one tier relative to the canonical list.

### [medium] C7: There is no Master tier. The mahogany-plank tier is Expert (level 70). Labelling it 'Master' references a tier that does not exist in the game.

- **Data says:** use the correct plank tier (Master = Mahogany only)
- **Wiki says (raw):** Higher tier contracts are unlocked with higher Construction levels. Beginner contracts can be started at level 1, Novice at level 20, Adept at level 50 and Expert at level 70.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mahogany_Homes
- **Suggested fix:** Replace 'Master = Mahogany only' with 'Expert = Mahogany only' to use the correct tier name.
- **Skeptic receipt:** wiki_lookup raw output: 'Expert (level 70)' is the highest tier. WebFetch plank types: 'Expert: Mahogany planks.' No 'Master' tier in either source.
- **Skeptic reasoning:** The data says 'Master = Mahogany only.' The wiki defines no Master tier. The level-70 tier is Expert, and Expert contracts use mahogany planks. The plank assignment is correct but the tier name is wrong. This is the same fabricated 'Master' label appearing in C5 and C6, here applied specifically to the plank-tier association.

### [high] C9: Mahogany Homes contracts are in East Ardougne, not West Ardougne. West Ardougne is a separate walled city with no bank accessible to players doing construction contracts there. Directing players to the West Ardougne bank sends them across the city wall to the wrong side of Ardougne.

- **Data says:** West Ardougne bank is closest to Ardougne contracts.
- **Wiki says (raw):** Four cities have contracts: Falador, Varrock, East Ardougne, and Hosidius
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mahogany_Homes
- **Suggested fix:** Replace 'West Ardougne bank' with 'East Ardougne bank' (or 'Ardougne north/south bank') since contracts are in East Ardougne.
- **Skeptic receipt:** Mahogany Homes wiki (wiki_lookup): 'Amy will provide the player with a contract for one of several possible houses in one of four locations: south-east Falador, north-east or southern Varrock, southern or central Hosidius, or throughout East Ardougne.' West Ardougne wiki fetch: 'there is no bank mentioned in West Ardougne... For banking, players would need to travel to East Ardougne or another location with banking facilities.'
- **Skeptic reasoning:** The guidance step at line 15630 directs players to 'West Ardougne bank' for restocking. The wiki confirms contracts are in East Ardougne, not West Ardougne. West Ardougne has no bank -- the West Ardougne wiki page lists no banking facility and explicitly notes it only accessible to players who have started Plague City. Directing players to a non-existent bank in the wrong half of Ardougne is a genuine navigation error.

## Mastering Mixology

### [high] C2: The reagent pouch stores herblore secondary ingredients (snape grass, wine of zamorak, etc.) for traditional potion-making. It has no functionality related to paste or the Mastering Mixology minigame. The 'auto-deposit paste' claim is fabricated.

- **Data says:** a Reagent pouch if you own one to auto-deposit paste
- **Wiki says (raw):** Stores up to 26 of each secondary ingredient used in Herblore. Requires Herblore 81 to use.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove references to the reagent pouch auto-depositing paste. The reagent pouch is not relevant to the Mastering Mixology minigame.
- **Skeptic receipt:** Reagent pouch wiki: 'Stores up to 26 of each secondary ingredient used in Herblore. Requires Herblore 81 to use.' Mastering Mixology wiki: no mention of reagent pouch interacting with paste or deposit boxes anywhere on the page.
- **Skeptic reasoning:** The reagent pouch is purchasable from the Mastering Mixology shop, but its in-game function is storing herblore secondary ingredients for standard Herblore, not paste. The guidance text at drop_rates.json line 15730 claims it 'auto-deposit[s] paste' -- no such mechanic exists. The wiki page for Reagent pouch and the Mastering Mixology page both confirm it has no paste-storage or deposit-box interaction inside the minigame.

### [medium] C3: The guidance describes Aldarin as being 'in southern Varlamore' implying it is part of the Varlamore mainland. The wiki describes it as an island off the south-west coast -- a distinct geographic entity only reachable by sea/teleport, not by running south from Varlamore.

- **Data says:** Travel to Aldarin in southern Varlamore.
- **Wiki says (raw):** Aldarin is a large island off the south-west coast of Varlamore.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aldarin
- **Suggested fix:** Change 'Aldarin in southern Varlamore' to 'Aldarin, an island off the south-west coast of Varlamore'.
- **Skeptic receipt:** Aldarin wiki: 'Aldarin is a large island off the south-west coast of Varlamore.'
- **Skeptic reasoning:** The guidance at drop_rates.json line 15742 describes the destination as 'Aldarin in southern Varlamore', implying it is a mainland location reachable overland. The wiki is explicit that Aldarin is a separate island, not part of the Varlamore landmass, which is material to travel advice -- a player cannot simply run there from Varlamore.

### [high] C4: Civitas illa Fortis is the capital of Varlamore on the mainland. Aldarin is a large island off the south-west coast of Varlamore reachable only by ship, fairy ring, quetzal transport, or minigame teleport. The wiki lists no Civitas illa Fortis route to Aldarin.

- **Data says:** Civitas illa Fortis teleport + run south-west also works.
- **Wiki says (raw):** The wiki lists transport options as: Minigame Teleport, Fairy ring CKQ, Quetzal Transport System to Aldarin then run north, Ship from Sunset Coast, Player-owned house on Aldarin, Charter ship to Aldarin, Pendant of Ates. Civitas illa Fortis is not listed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove 'Civitas illa Fortis teleport + run south-west'. Replace with an accurate alternative such as 'Fairy ring CKQ' or 'Minigame teleport'.
- **Skeptic receipt:** Mastering Mixology wiki transport options: 'Minigame Teleport... fairy ring code CKQ and run south-east... Quetzal Transport System to Aldarin, then run north... Ship ride from Sunset Coast (requires 20 coins), then run due south... player-owned house on Aldarin... Charter a charter ship to Aldarin... Pendant of Ates...' Civitas illa Fortis is not listed. Aldarin wiki: 'Aldarin is a large island off the south-west coast of Varlamore.'
- **Skeptic reasoning:** The guidance at drop_rates.json lines 15742 and 15748 states 'Civitas illa Fortis teleport + run south-west also works.' Aldarin is an island separated from the Varlamore mainland by sea -- there is no overland running route from Civitas illa Fortis. The wiki's exhaustive transport list does not include this route.

### [blocker] C6: The data states 'Children of the Sun started' but the wiki requires full completion of the quest. A player who has only started the quest would be denied entry.

- **Data says:** You need 60 Herblore + Children of the Sun started.
- **Wiki says (raw):** Participating in mixology requires level 60 Herblore [not boostable] and completion of Children of the Sun
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Change 'Children of the Sun started' to 'Children of the Sun completed'.
- **Skeptic receipt:** Mastering Mixology wiki: 'Participating in mixology requires level 60 Herblore [not boostable] and completion of Children of the Sun.' PlayerQuestProgressState.java line 253-255: '// Children of the Sun -- must be fully complete for Varlamore access' / 'out.put(QuestSubMilestone.CHILDREN_OF_THE_SUN_COMPLETE, isQuestFinished(Quest.CHILDREN_OF_THE_SUN));'
- **Skeptic reasoning:** The guidance step text at drop_rates.json line 15752 says 'You need 60 Herblore + Children of the Sun started.' The wiki requires full completion, not merely starting the quest. The requirements block in the JSON correctly uses the 'quests' field (which maps to QuestState.FINISHED per SourceRequirements.java line 34 and PlayerQuestProgressState.java line 254-255), but the human-readable guidance description contradicts the requirement by saying 'started.'

### [medium] C8: The wiki describes a correction mechanic that explicitly states mistakes can be fixed 'without wasting any paste.' There is no mechanic described anywhere on the wiki where over-agitation converts a potion into paste.

- **Data says:** over-agitating turns the potion into paste (wasted XP)
- **Wiki says (raw):** If all three spots in the mixer are full, pulling any of the levers will also override the oldest element selected. This allows for correcting mistakes without wasting any paste.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove the over-agitation claim. If a warning about order indicators is still desired, base it on the actual mechanic: pulling the wrong lever overrides the oldest input, which can be corrected without paste waste.
- **Skeptic receipt:** Mastering Mixology wiki: 'If all three spots in the mixer are full, pulling any of the levers will also override the oldest element selected. This allows for correcting mistakes without wasting any paste.'
- **Skeptic reasoning:** The guidance at drop_rates.json line 15760 states 'over-agitating turns the potion into paste (wasted XP).' The wiki describes no such mechanic. The correction mechanic explicitly states that mistakes can be resolved without any waste.

### [high] C9: Same fabrication as C2: the reagent pouch stores herblore secondaries for standard herblore, not paste for Mastering Mixology. There is no 'deposit box walk' mechanic described anywhere on the wiki.

- **Data says:** Reagent pouch auto-stores paste so you don't need to walk to deposit boxes.
- **Wiki says (raw):** Stores up to 26 of each secondary ingredient used in Herblore. Requires Herblore 81 to use.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove all reagent pouch paste-storage claims from guidanceSteps[3]. The item is irrelevant to this minigame.
- **Skeptic receipt:** Reagent pouch wiki: 'Stores up to 26 of each secondary ingredient used in Herblore. Requires Herblore 81 to use.' The Mastering Mixology wiki page describes no deposit-box walk mechanic and does not mention the reagent pouch having any in-minigame function.
- **Skeptic reasoning:** The guidance at drop_rates.json line 15760 claims 'Reagent pouch auto-stores paste so you don't need to walk to deposit boxes.' This is fabricated -- the reagent pouch stores herblore secondary ingredients for standard Herblore potions, not paste produced inside the Mastering Mixology minigame. There is no 'deposit box walk' mechanic on the wiki.

### [blocker] C18: The guidance tells players not to bring herbs because the lab supplies them. The wiki states the opposite -- players must bring their own clean herbs or unfinished potions. A player following this guidance would arrive with no herbs and be unable to make any potions.

- **Data says:** The minigame supplies all herbs and vials in the lobby; you do not need to bring herblore reagents.
- **Wiki says (raw):** Clean herbs or the corresponding unfinished potions must be brought to refine into paste.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove the claim that the lab supplies herbs/vials. Change to: 'Bring clean herbs or unfinished potions -- the lab does not supply them. A bank chest is available on-site.'
- **Skeptic receipt:** Mastering Mixology wiki: 'Clean herbs or the corresponding unfinished potions must be brought to refine into paste. There is a bank chest available. Herbs must be un-noted.'
- **Skeptic reasoning:** The guidance step at drop_rates.json line 15730 states 'The minigame supplies all herbs and vials in the lobby; you do not need to bring herblore reagents.' The wiki is unambiguous that players must supply their own herbs, and the bank chest exists specifically to support restocking them.

### [blocker] C19: The step-5 guidance reinforces the false claim that no supplies are needed. The wiki confirms players must supply their own herbs. The bank chest is on-site precisely because players need to restock herbs.

- **Data says:** No restock is needed - the lab supplies everything.
- **Wiki says (raw):** Clean herbs or the corresponding unfinished potions must be brought to refine into paste. There is a bank chest available.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Remove 'the lab supplies everything'. Change to: 'Use the on-site bank chest to restock herbs as needed.'
- **Skeptic receipt:** Mastering Mixology wiki: 'Clean herbs or the corresponding unfinished potions must be brought to refine into paste. There is a bank chest available.'
- **Skeptic reasoning:** The guidance step at drop_rates.json line 15782 states 'No restock is needed - the lab supplies everything.' The wiki confirms the opposite: players must bring their own herbs, and the on-site bank chest exists precisely to support restocking between runs.

### [medium] C20: Aldarin is an island with no direct running route to Civitas illa Fortis. Suggesting 'Civitas illa Fortis teleport' as a banking method after Mixology is misleading -- there is no such teleport from the minigame, and a player would still need to travel from Civitas to a bank. The Minigame Teleport or a POH teleport would be standard routes.

- **Data says:** Teleport back to Civitas illa Fortis or your home bank to deposit any rewards.
- **Wiki says (raw):** The wiki lists no Civitas illa Fortis teleport connection to the Mastering Mixology area. Transport options listed are Minigame Teleport, Fairy ring CKQ, Quetzal Transport System, Ship from Sunset Coast, Player house on Aldarin, Charter ship, Pendant of Ates.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Mastering_Mixology
- **Suggested fix:** Replace 'Civitas illa Fortis' with an accurate egress: e.g., 'Use the Grouping interface minigame teleport to leave, or teleport to your player-owned house on Aldarin, then use the on-site bank chest.'
- **Skeptic receipt:** Mastering Mixology wiki transport options (exhaustive list): 'Minigame Teleport... fairy ring code CKQ... Quetzal Transport System to Aldarin... Ship ride from Sunset Coast... player-owned house on Aldarin... Charter a charter ship to Aldarin... Pendant of Ates...' No Civitas illa Fortis egress listed. Aldarin wiki: 'Aldarin is a large island off the south-west coast of Varlamore.'
- **Skeptic reasoning:** The guidance step at drop_rates.json line 15782 tells players to 'Teleport back to Civitas illa Fortis' after the minigame. Aldarin is an island; the wiki lists no teleport from the minigame area to Civitas illa Fortis. No such egress route exists. The standard egress is the Minigame Teleport or a player-owned house teleport on Aldarin itself.

## Volcanic Mine

### [medium] C6: Falling rocks (stalactites/meteorites) deal HP damage only. The wiki never states they reduce mine stability. Stability is managed exclusively through the gas chambers (A, B, C) -- keeping pressure in the 40-60% range. Claiming stalactites also 'chip away stability' is an invented mechanic.

- **Data says:** Falling rocks in Volcanic Mine chip away HP and stability
- **Wiki says (raw):** stalactites will start falling from the ceiling which will damage any players they land on for upwards of 25 damage
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Remove 'and stability' from the description. Falling rocks deal HP damage only; stability is controlled via the gas chamber vent mechanics.
- **Skeptic receipt:** "Stalactites will start falling from the ceiling which will damage any players they land on for upwards of 25 damage." -- wiki_lookup + WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine. Stability mechanic quote: "The pressure of each chamber should be kept around 40-60%, or else the mine stability will start to decrease."
- **Skeptic reasoning:** The wiki states stalactites deal HP damage only -- 'stalactites will start falling from the ceiling which will damage any players they land on for upwards of 25 damage.' Mine stability is controlled exclusively by gas chamber pressure (the 40-60% range). No wiki text links stalactite falls to any stability reduction. The claim that falling rocks 'chip away stability' is an invented mechanic not described anywhere on the page.

### [high] C9: The Digsite pendant (standard charge) teleports to the Digsite area, not the Mushroom Forest. The Mushroom Forest is not a pendant destination at all. The route from the barge landing is east to the rope anchor, not north-east from a forest.

- **Data says:** Digsite pendant teleport takes you to the Mushroom Forest, then run north-east to Volcanic Mine
- **Wiki says (raw):** Digsite pendant to the Digsite, travel to Fossil Island via the barge, then go East and climb up the Rope anchor (requires Agility 64)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Replace 'Digsite pendant teleport takes you to the Mushroom Forest, then run north-east' with the correct route: Digsite pendant to the Digsite -> barge to Fossil Island -> run east and climb the rope anchor (requires 64 Agility). Alternatively, describe the Fossil Island charge: pendant to House on the Hill -> Mycelium Transportation System to Verdant Valley -> run east then north to the mine.
- **Skeptic receipt:** "Digsite pendant to the Digsite, travel to Fossil Island via the barge, then go East and climb up the Rope anchor" -- wiki_lookup output from https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Skeptic reasoning:** The wiki lists the Digsite pendant route as: 'Digsite pendant to the Digsite, travel to Fossil Island via the barge, then go East and climb up the Rope anchor (requires Agility 64).' The pendant does not teleport to the Mushroom Forest -- that is not a pendant destination at all. The direction from the barge landing is east to the rope anchor, not north-east from a forest.

### [medium] C10: The Fossil Island charge of the Digsite pendant lands at House on the Hill, not a generic 'Fossil Island' arrival point. The onward route is east-then-north (via Mycelium Transport to Verdant Valley), not simply 'north-east.'

- **Data says:** POH mounted digsite pendant teleports to Fossil Island, then run north-east to Volcanic Mine
- **Wiki says (raw):** Digsite pendant to House on the Hill, then Mycelium Transportation System to Verdant Valley, then run east below the lava, then run north
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Update to: 'POH mounted digsite pendant teleports to House on the Hill -> use Mycelium Transportation System to Verdant Valley -> run east below the lava then north to Volcanic Mine.'
- **Skeptic receipt:** "Digsite pendant to House on the Hill, then Mycelium Transportation System to Verdant Valley, then run east below the lava, then run north" -- wiki_lookup output from https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Skeptic reasoning:** The wiki gives the Fossil Island pendant destination as 'House on the Hill', not a generic 'Fossil Island' arrival. The onward route is 'Mycelium Transportation System to Verdant Valley, then run east below the lava, then run north' -- not simply 'north-east'. The data's phrasing 'teleports to Fossil Island, then run north-east' is imprecise enough to be materially misleading about the actual route.

### [high] C11: The wiki lists World 323 (United States west) as the official Volcanic Mine world. Worlds 408 and 409 are not mentioned anywhere on the page.

- **Data says:** Worlds 408 and 409 are the official Volcanic Mine theme worlds
- **Wiki says (raw):** World 323
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Replace worlds 408 and 409 with World 323.
- **Skeptic receipt:** Wiki worlds table returns World 323 (United States west, Members) as the sole official world -- WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine section '(3) Official World Number'
- **Skeptic reasoning:** The wiki lists exactly one official Volcanic Mine world: World 323 (United States west, Members). Worlds 408 and 409 do not appear anywhere on the page.

### [medium] C12: The wiki describes no multiplier system for team vs solo play. Points are individually earned by each player based on their own contributions (capping vents, mining ore). The '3-5x multiplier' figure is not supported by the wiki.

- **Data says:** Team play in Volcanic Mine multiplies points/hour by 3-5x compared to solo
- **Wiki says (raw):** Points are awarded individually based on performance regardless of team size.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Remove or rephrase the multiplier claim. Team play improves efficiency by allowing role specialisation (dedicated cappers vs miners), enabling the full boulder bonus to be completed -- but there is no stated numeric multiplier.
- **Skeptic receipt:** "The page does not mention points multipliers based on team size or any differential point earning between team and solo play." -- WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine section '(4) Points Multipliers for Team vs Solo'
- **Skeptic reasoning:** The wiki describes no points multiplier for team vs solo play. Points are earned individually based on personal contributions. The '3-5x multiplier' figure has no basis on the page. Team play improves efficiency through role specialisation and enabling the boulder bonus, but no numeric multiplier is stated.

### [medium] C13: The three venting features are gas chambers labelled A, B, and C on the overlay -- not coloured walls named red, green, and blue. The wiki makes no mention of wall colours as a game mechanic.

- **Data says:** Volcanic Mine has three coloured walls: red, green, and blue
- **Wiki says (raw):** The gas chambers are marked on the minigame overlay as A, B, and C.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Replace 'three coloured walls: red, green, and blue' with 'three gas chambers labelled A, B, and C.'
- **Skeptic receipt:** "The gas chambers are marked on the minigame overlay as A, B, and C." -- WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine section '(2) Gas Chambers'
- **Skeptic reasoning:** The wiki describes gas chambers labelled A, B, and C on the minigame overlay. There are no 'coloured walls named red, green, and blue' described anywhere on the page. The colour-wall framing does not correspond to any mechanic on the Volcanic Mine wiki page.

### [medium] C14: This claim is based on the incorrect colour-wall framing from C13. The actual mechanic involves gas chambers (A/B/C) that players cap or uncap to maintain pressure between 40-60%. There are no 'coloured walls that fuel vents.'

- **Data says:** Each coloured wall in Volcanic Mine fuels a different vent
- **Wiki says (raw):** Players can block the gas chambers with a large rock to increase gas pressure, or remove the large rock to decrease gas pressure.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Rephrase to describe the gas chamber pressure mechanic: players cap or uncap chambers A, B, and C to keep gas pressure in the 40-60% range and maintain mine stability.
- **Skeptic receipt:** "Players can block the gas chambers with a large rock to increase gas pressure, or remove the large rock to decrease gas pressure." and "The pressure of each chamber should be kept around 40-60%, or else the mine stability will start to decrease." -- WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Skeptic reasoning:** This claim is derived from the incorrect colour-wall framing of C13. The actual mechanic is gas chambers (A/B/C) that players cap or uncap with large rocks to control pressure between 40-60%. There are no coloured walls and no 'vents fuelled by walls.' The description is a fabrication that builds on C13's false premise.

### [medium] C17: The wiki does not describe a 150-point threshold for maximising the unique reward table roll. No such scoring gate appears anywhere on the page. The claim appears to be fabricated or confused with a different minigame.

- **Data says:** Scoring 150+ points per round in Volcanic Mine maximizes unique table roll chance
- **Wiki says (raw):** The document mentions earning points but does not specify a 150-point threshold for unique items.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Volcanic_Mine
- **Suggested fix:** Remove the 150-point unique threshold claim, or source the actual point threshold from the wiki's rewards section if one exists.
- **Skeptic receipt:** "No mention of a 150-point threshold for unique reward rolls appears on this page." -- WebFetch of https://oldschool.runescape.wiki/w/Volcanic_Mine section '(5) 150-Point Threshold for Unique Rewards'
- **Skeptic reasoning:** The wiki contains no mention of a 150-point threshold for maximising the unique reward table roll chance. No such scoring gate appears on the page. The claim is not supported by any authoritative source.

## Tithe Farm

### [medium] C1: Xeric's talisman teleports to Xeric's Glade (not Xeric's Heart). Using the wrong talisman destination would place the player at a different location.

- **Data says:** Travel to Tithe Farm in Hosidius via Xeric's talisman (Xeric's Heart) or fairy ring CIR then walk south.
- **Wiki says (raw):** Use a Xeric's talisman to teleport to Xeric's Glade and go south-east
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Change 'Xeric's Heart' to 'Xeric's Glade' in guidanceSteps[0].description.
- **Skeptic receipt:** wiki_lookup raw output: 'Use a Xeric's talisman to teleport to Xeric's Glade and go south-east'
- **Skeptic reasoning:** The data guidance says 'Xeric's Heart' as the talisman destination. Both the WebFetch of the wiki page and the wiki_lookup MCP return the same raw text: 'Use a Xeric's talisman to teleport to Xeric's Glade and go south-east.' Xeric's Heart and Xeric's Glade are distinct talisman teleport destinations; a player following the data guidance would land at the wrong location. The fix is to replace 'Xeric's Heart' with 'Xeric's Glade' in guidanceSteps[0].description.

### [high] C2: Fairy ring code CIR does not lead near Tithe Farm. The correct code is AKR (Hosidius Vinery). A player following this guidance would teleport to the wrong location entirely.

- **Data says:** Travel to Tithe Farm in Hosidius via Xeric's talisman (Xeric's Heart) or fairy ring CIR then walk south.
- **Wiki says (raw):** Use the fairy ring AKR to teleport to the Hosidius Vinery and go south
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Change 'fairy ring CIR' to 'fairy ring AKR' in guidanceSteps[0].description.
- **Skeptic receipt:** wiki_lookup raw output: 'Use the fairy ring , to teleport to the Hosidius Vinery and go south' (AKR confirmed by WebFetch: 'Use the fairy ring AKR, to teleport to the Hosidius Vinery and go south')
- **Skeptic reasoning:** The data guidance says 'fairy ring CIR'. Both the WebFetch and wiki_lookup MCP return: 'Use the fairy ring AKR, to teleport to the Hosidius Vinery and go south.' CIR is not a fairy ring code that leads to Tithe Farm. A player using CIR would be teleported to the wrong location entirely. The fix is to replace 'CIR' with 'AKR'.

### [medium] C3: Hosidius favour requirement was removed in January 2024. The guidance still lists '34% Hosidius favour' as a gate, which is no longer required and may mislead players into grinding favour unnecessarily.

- **Data says:** Travel to Tithe Farm in Hosidius via Xeric's talisman (Xeric's Heart) or fairy ring CIR then walk south. Requires 34% Hosidius favour and 34 Farming.
- **Wiki says (raw):** The Tithe Farm can now be accessed starting at level 34 Farming as Kourend Favour has been removed.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Remove '34% Hosidius favour' from the requirements text in guidanceSteps[0].description. The only access gate is 34 Farming.
- **Skeptic receipt:** WebFetch raw output: 'As of January 10, 2024: "The Tithe Farm can now be accessed starting at level 34 Farming as Kourend Favour has been removed."'
- **Skeptic reasoning:** The data guidance lists '34% Hosidius favour' as an access gate. The WebFetch confirms that as of January 10, 2024, this requirement was removed: 'The Tithe Farm can now be accessed starting at level 34 Farming as Kourend Favour has been removed.' wiki_updates returns zero changes since 2024-01-01, meaning this removal is stable and current. The sole access gate is 34 Farming. Leaving the favour requirement in guidance would mislead players into grinding favour unnecessarily.

### [blocker] C6: Logavano seeds require level 74, not 54. The data has Logavano and Bologano inverted: Logavano is listed at 54+ but the wiki confirms Logavano=74, Bologano=54. A player would bring the wrong seeds for their level.

- **Data says:** Bring seeds scaled to your Farming level (Golovanova at 34+, Logavano at 54+, Bologano at 74+)
- **Wiki says (raw):** Bolivano seeds ( Farming 54 ) ... Logavano seeds ( Farming 74 )
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Swap the seed levels in guidanceSteps[1].description: 'Golovanova at 34+, Bologano at 54+, Logavano at 74+'. This corrects the inversion.
- **Skeptic receipt:** wiki_lookup raw output: 'Golovanova seeds () / Bologano seeds () / Logavano seeds ()' and WebFetch: 'Golovanova seeds (Farming 34) / Bologano seeds (Farming 54) / Logavano seeds (Farming 74)'
- **Skeptic reasoning:** The data lists 'Logavano at 54+'. Both sources confirm Logavano requires Farming 74, not 54. The correct seed tier order is Golovanova=34, Bologano=54, Logavano=74. The data has Logavano and Bologano level thresholds inverted. This is the same inversion as C7; both must be corrected together by swapping the two level values in the guidance text.

### [blocker] C7: Bologano seeds require level 54, not 74. The data has Logavano and Bologano inverted: Bologano is listed at 74+ but the wiki confirms Bologano=54, Logavano=74. This is the same inversion as C6 -- both claims must be fixed together.

- **Data says:** Bring seeds scaled to your Farming level (Golovanova at 34+, Logavano at 54+, Bologano at 74+)
- **Wiki says (raw):** Bolivano seeds ( Farming 54 ) ... Logavano seeds ( Farming 74 )
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Swap the seed levels in guidanceSteps[1].description: 'Golovanova at 34+, Bologano at 54+, Logavano at 74+'. This corrects the inversion.
- **Skeptic receipt:** WebFetch raw output: 'Bologano seeds (Farming 54)' and 'Logavano seeds (Farming 74)'
- **Skeptic reasoning:** The data lists 'Bologano at 74+'. Both sources confirm Bologano requires Farming 54, not 74. This is the mirror half of the C6 inversion. The fix is the same: swap the level assignments so Bologano=54 and Logavano=74.

### [low] C10: Bonus rewards are triggered by depositing the 75th fruit (not by reaching 74+ points). The threshold is fruit count, not a points total, and the threshold is 75 fruits (not 74+). The guidance conflates two different metrics.

- **Data says:** aim for 74+ Tithe Farm points per round to trigger bonus rewards
- **Wiki says (raw):** Players receive bonus experience of 250 times the harvest experience rate when depositing the 75th fruit ... double experience is awarded for the 75th through 100th fruit added to the sack, and 2 bonus points are awarded to players when they add the 100th fruit.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Tithe_Farm
- **Suggested fix:** Change 'aim for 74+ Tithe Farm points per round to trigger bonus rewards' to 'aim to deposit all 100 fruits per round; bonus experience begins at the 75th fruit deposited'.
- **Skeptic receipt:** WebFetch raw output: 'Players receive bonus experience of 250 times the harvest experience rate when depositing the 75th fruit...Additionally, double experience is awarded for the 75th through 100th fruit added to the sack, and 2 bonus points are awarded to players when they add the 100th fruit.' Points formula: 'For every 3 plants a player deposits in the sack, they will earn 1 point and for every 100 plants deposited, they will earn an additional 2 points.'
- **Skeptic reasoning:** The data says 'aim for 74+ Tithe Farm points per round to trigger bonus rewards.' The wiki defines no such threshold. The documented bonus trigger is fruit count, not points: 'Players receive bonus experience of 250 times the harvest experience rate when depositing the 75th fruit...double experience is awarded for the 75th through 100th fruit.' The points formula is: 1 point per 3 fruits deposited, plus 2 bonus points at the 100th fruit, making a full round worth ~35 points total. 74 points per round is arithmetically impossible in a single round (would require 222+ fruits; the farm only has 100 deposit slots per round). The guidance conflates two different metrics and cites an unreachable threshold.

## Gnome Restaurant (Scarfs)

### [medium] C1: Wrong floor designation. The claim says 'ground floor' (floor 0 in OSRS UK notation) but the wiki says '1st floor[UK] / 2nd floor[US]', which is one floor above the ground floor. A player following this guidance would search the wrong level of the Grand Tree.

- **Data says:** Gianne jnr. is located on the west end of the ground floor at the Grand Tree
- **Wiki says (raw):** talk to Gianne jnr. on the west end of the 1st floor[UK]2nd floor[US] of the Grand Tree
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gnome_Restaurant
- **Suggested fix:** Change 'ground floor' to '1st floor (2nd floor US)' or equivalently 'first floor above ground level' of the Grand Tree to match the wiki's floor numbering.
- **Skeptic receipt:** wiki_lookup(Gnome Restaurant): 'talk to Gianne jnr. on the west end of the [FloorNumber uk=1] of the Grand Tree' | wiki_lookup(Grand Tree): 'The Gnome Restaurant minigame is on second level' | data line 16122: 'Talk to Gianne jnr. on the west end of the ground floor to get a hard delivery order.'
- **Skeptic reasoning:** The data file (line 16122) reads: "Talk to Gianne jnr. on the west end of the ground floor to get a hard delivery order." The Gnome Restaurant wiki page uses the FloorNumber template rendered as '1st floor[UK] / 2nd floor[US]', and the Grand Tree article states 'The Gnome Restaurant minigame is on second level' (i.e. UK 1st floor, one level above ground). The worldPlane:0 coordinate on that guidance step is the exterior travel waypoint outside the tree, not validation of the floor description. A player reading 'ground floor' would search floor 0 of the Grand Tree and not find Gianne jnr., who is one floor above. No account type or progress state places Gianne jnr. on the ground floor.

### [high] C5: Completely wrong location for Captain Ninto. The claim places him south-west of the Gnome Stronghold gate, but the wiki states he is in the dwarf bar beneath White Wolf Mountain -- a different region entirely. A player following this guidance would waste significant time searching the wrong area.

- **Data says:** Captain Ninto is located south-west of the Stronghold gate
- **Wiki says (raw):** In the dwarf bar beneath White Wolf Mountain.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gnome_Restaurant
- **Suggested fix:** Update Captain Ninto's location to 'in the dwarf bar beneath White Wolf Mountain'.
- **Skeptic receipt:** npc_lookup(Captain Ninto): 'Location: White Wolf Tunnel' (abextm cache, NPC ID 2569) | wiki_lookup(Captain Ninto): 'inside the dwarven passage under the White Wolf Mountain (Sindarpos)' | data line 16145: 'Captain Ninto (south-west of the Stronghold gate)'
- **Skeptic reasoning:** The data file (line 16145) states Captain Ninto is 'south-west of the Stronghold gate.' The npc_lookup (abextm cache) returns Location: 'White Wolf Tunnel' for NPC ID 2569. The Captain Ninto wiki page confirms: 'Captain Ninto is a drunk, off-duty Gnome glider pilot that can be found inside the dwarven passage under the White Wolf Mountain (Sindarpos).' White Wolf Mountain / the dwarven tunnel is a completely different region from the Tree Gnome Stronghold gate. There is no account state or game version where Captain Ninto is south-west of the Stronghold gate.

### [high] C6: Completely wrong location for Captain Daerkin. The claim places him at the Gnome Stronghold entrance, but the wiki states he is at the top of the south-western arena in Emir's Arena -- a completely different region. A player following this guidance would be sent to entirely the wrong part of the map.

- **Data says:** Captain Daerkin is located at the Gnome stronghold entrance
- **Wiki says (raw):** Top of the south-western arena in Emir's Arena.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Gnome_Restaurant
- **Suggested fix:** Update Captain Daerkin's location to 'top of the south-western arena in Emir's Arena'.
- **Skeptic receipt:** npc_lookup(Captain Daerkin): 'Location: Emir's Arena' (abextm cache, NPC ID 2570) | wiki_lookup(Captain Daerkin): 'enjoys watching fights at the Emir's Arena viewing area' | data line 16145: 'Captain Daerkin (Gnome stronghold entrance)'
- **Skeptic reasoning:** The data file (line 16145) states Captain Daerkin is at 'Gnome stronghold entrance.' The npc_lookup (abextm cache) returns Location: 'Emir's Arena' for NPC ID 2570. The Captain Daerkin wiki page confirms: 'Captain Daerkin is an off-duty gnome glider pilot who enjoys watching fights at the Emir's Arena viewing area.' Emir's Arena is in the desert, entirely separate from the Tree Gnome Stronghold. There is no game state where Captain Daerkin is at the Gnome stronghold entrance.

## Fishing Trawler

### [high] C6: The voyage duration is stated as 10 minutes in the guidance, but the wiki clearly states it is 5 minutes. A player following this guidance would expect a voyage twice as long as the actual one.

- **Data says:** During the 10-minute voyage, bail water from flooded sections (use bucket on water) and repair damaged nets with rope.
- **Wiki says (raw):** To play, board Murphy's trawling boat and work to prevent it from sinking for 5 minutes as it trawls the sea.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Suggested fix:** Change '10-minute voyage' to '5-minute voyage' in guidanceSteps[2].description.
- **Skeptic receipt:** "To play, board Murphy's trawling boat and work to prevent it from sinking for 5 minutes as it trawls the sea." -- https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Skeptic reasoning:** The data states '10-minute voyage' in guidanceSteps[2].description (line 16251). The wiki is unambiguous: 'work to prevent it from sinking for 5 minutes as it trawls the sea.' The voyage is 5 minutes, not 10. A player following this guidance would expect a voyage twice the actual length. The receipt directly contradicts the data value with no variant or account-type caveat.

### [blocker] C10: The guidance fabricates a trip-ending mechanic for unrepaired nets. The wiki explicitly states the only loss condition is water level rising too high; unrepaired nets only reduce fish catch -- they do not end the trip early. This could mislead players into ignoring water bailing (the actual loss condition) while over-prioritising net repair.

- **Data says:** Two or more unrepaired nets will end the trip early
- **Wiki says (raw):** The only way to lose is if the water level gets too high before the time is up. [...] While the net is torn, it will not catch any fish, so it is advisable to fix a ripped net as quickly as possible.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Suggested fix:** Remove the sentence 'Two or more unrepaired nets will end the trip early' from guidanceSteps[2].description. Replace with an accurate note such as 'Unrepaired nets catch no fish, so fix them quickly to maximise your haul.'
- **Skeptic receipt:** "The only way to lose is if the water level gets too high before the time is up. The boat will sink, ending the game." and "While the net is torn, it will not catch any fish, so it is advisable to fix a ripped net as quickly as possible." -- https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Skeptic reasoning:** The data claims 'Two or more unrepaired nets will end the trip early' (line 16251). The wiki explicitly defines the sole loss condition as water level, not nets: 'The only way to lose is if the water level gets too high before the time is up. The boat will sink, ending the game.' For torn nets, the wiki states only: 'While the net is torn, it will not catch any fish, so it is advisable to fix a ripped net as quickly as possible.' No trip-ending mechanic for nets exists in any form. The data fabricates a loss condition that does not exist, which could mislead players into neglecting bailing (the actual loss condition) in favour of net repair.

### [medium] C11: The guidance directs the player to collect rewards from Murphy, but the wiki states rewards are obtained by inspecting the trawler net, not by talking to Murphy. A player following this step as written would look for the wrong interaction.

- **Data says:** When the trawler returns to Port Khazard, collect your fish haul from Murphy.
- **Wiki says (raw):** Upon successfully returning to the Port Khazard dock, players who contributed at least 50 points can inspect the trawler net for their reward.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Suggested fix:** Change 'collect your fish haul from Murphy' to 'inspect the trawler net to collect your fish haul' in guidanceSteps[3].description.
- **Skeptic receipt:** "Upon successfully returning to the Port Khazard dock, players who contributed at least 50 points can inspect the trawler net for their reward." -- https://oldschool.runescape.wiki/w/Fishing_Trawler
- **Skeptic reasoning:** The data instructs players to 'collect your fish haul from Murphy' (line 16263). The wiki states rewards are obtained by a different interaction entirely: 'players who contributed at least 50 points can inspect the trawler net for their reward.' Murphy is the NPC used to board the trawler, not the reward source. A player following the guidance as written would attempt to talk to Murphy at the end rather than inspect the net, missing the reward interaction. The receipt directly establishes the correct mechanic.

## Temple Trekking

### [blocker] C12: Rolayne Twickit is described as the 'strongest follower' but the wiki explicitly states she has 'very low Attack, Defence, and Hitpoints' -- she is the weakest/hardest follower, not the strongest. A player following this guidance who expects an easy escort will instead face the most dangerous trek variant.

- **Data says:** Choose the hardest route and strongest follower (Rolayne Twickit) for the best Lumberjack outfit drop chance.
- **Wiki says (raw):** They have very low Attack, Defence, and Hitpoints, and five monsters will appear in combat events.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Temple_Trekking
- **Suggested fix:** Replace 'strongest follower (Rolayne Twickit)' with 'weakest follower (Rolayne Twickit)' -- she is chosen for maximum challenge/rewards, not because she is strong.
- **Skeptic receipt:** wiki_lookup Temple Trekking: 'Escorting weaker followers means a greater number of monsters will attack when a monster attack event occurs.' WebFetch Temple Trekking followers section: 'Rolayne Twickit belongs to the hard difficulty tier and has notably weak defensive capabilities, making escort missions substantially more demanding.'
- **Skeptic reasoning:** The data calls Rolayne Twickit the 'strongest follower' but the wiki is unambiguous: she belongs to the hard/weak difficulty tier with 'very low Attack, Defence, and Hitpoints.' She is the weakest follower, chosen because her weakness makes the trek harder (more monsters, more points). The descriptor 'strongest' is factually inverted and will confuse players about the risk level.

### [blocker] C1: The guidance claims Drakan's medallion teleports to Burgh de Rott, but the wiki confirms it teleports to Ver Sinhaza, Darkmeyer, and Slepe -- never Burgh de Rott. Following this tip would strand players in the wrong city with no direct route to the trek start.

- **Data says:** Drakan's medallion -> Burgh de Rott is fastest; ... Drakan's medallion -> Burgh de Rott
- **Wiki says (raw):** It allows unlimited teleportation to Ver Sinhaza, Darkmeyer (after completion of Sins of the Father), and the Sisterhood Sanctuary under Slepe.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Drakan%27s_medallion
- **Suggested fix:** Replace 'Drakan's medallion -> Burgh de Rott' with 'Drakan's medallion -> Ver Sinhaza, then run south to Burgh de Rott' (or remove and substitute a correct route such as Morytania legs 3 teleport to Burgh de Rott).
- **Skeptic receipt:** wiki_lookup Drakan's medallion: 'It allows unlimited teleportation to Ver Sinhaza, Darkmeyer (after completion of Sins of the Father), and the Sisterhood Sanctuary under Slepe.' wiki_lookup Temple Trekking travel routes: 'Drakan's medallion to Ver Sinhaza' (not Burgh de Rott).
- **Skeptic reasoning:** The data states 'Drakan's medallion -> Burgh de Rott' as the fastest travel route, implying a direct teleport to Burgh de Rott. The medallion has no such destination. The wiki confirms it teleports to Ver Sinhaza, Darkmeyer, and Slepe only. The Temple Trekking wiki page itself lists the correct route as 'Drakan's medallion to Ver Sinhaza.' The shorthand misleads players about where they will land.

### [high] C3: The data labels fairy ring BKR as 'Canifis' but the wiki identifies CKS -- not BKR -- as the Canifis fairy ring. BKR is located in Mort Myre Swamp south of Canifis.

- **Data says:** fairy ring BKR (Canifis) run south-west
- **Wiki says (raw):** Fairy ring code CKS (Canifis)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Temple_Trekking
- **Suggested fix:** Replace 'fairy ring BKR (Canifis)' with 'fairy ring CKS (Canifis)' or 'fairy ring BKR (Mort Myre Swamp)' depending on which ring is actually intended as the fallback route.
- **Skeptic receipt:** WebFetch fairy rings: BKR = 'Morytania: Mort Myre Swamp, south of Canifis'; CKS = 'Morytania: Canifis.'
- **Skeptic reasoning:** The data labels fairy ring BKR as '(Canifis)' but BKR teleports to Mort Myre Swamp, south of Canifis. CKS is the actual Canifis fairy ring. The parenthetical mislabels the ring and could send players to the wrong location or cause confusion about which ring to dial.

### [medium] C4: BKR places players in Mort Myre Swamp; the wiki describes Burgh de Rott as reachable by taking the swamp boat to the south-east from BKR, not by running south-west. The direction 'south-west' is inconsistent with how Burgh de Rott is reached from BKR.

- **Data says:** fairy ring BKR (Canifis) run south-west
- **Wiki says (raw):** Swamp boat south-east (towards Barrows and Burgh de Rott).
- **Wiki URL:** https://oldschool.runescape.wiki/w/Fairy_ring
- **Suggested fix:** Correct the direction or replace with a more accurate route description (e.g. take the swamp boat south-east toward Burgh de Rott).
- **Skeptic receipt:** WebFetch fairy rings, BKR row: 'Swamp boat south-east (towards Barrows and Burgh de Rott).'
- **Skeptic reasoning:** The data instructs players to 'run south-west' from BKR to reach Burgh de Rott. BKR lands in Mort Myre Swamp. The correct route from BKR toward Burgh de Rott uses the swamp boat going south-east, not running south-west. The direction is wrong and would strand players in the swamp.

## Rogues' Den

### [medium] C4: The wiki states no items can be brought or worn inside at all -- there is no mechanic where 'light carry weight' helps with specific obstacles. The data implies weight management is a strategy, but the real rule is a blanket no-items restriction.

- **Data says:** Bring no weapons or armour - the maze requires light carry weight for some obstacles
- **Wiki says (raw):** "no items can be brought or worn inside" and "Enter the Rogues' Den without any items in your inventory or equipped"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Replace 'the maze requires light carry weight for some obstacles' with 'the maze requires no items -- all inventory and equipment is stripped on entry'.
- **Skeptic receipt:** Wiki (WebFetch): 'no items can be brought or worn inside' | 'The document contains no information regarding how weight or carrying capacity affects obstacle success rates.'
- **Skeptic reasoning:** The wiki states a blanket no-items rule on maze entry: 'no items can be brought or worn inside.' The WebFetch also confirms there is no weight mechanic mentioned anywhere on the page -- weight cannot affect obstacle success because players enter with zero items. The data's implication that 'light carry weight helps with some obstacles' describes a mechanic that does not exist.

### [medium] C8: 'Balance beams' is not an obstacle type listed on the wiki. The wiki lists contortion bars, pendulums, spinning blades, ledges, floor blades, wall traps, and floor traps -- no balance beams.

- **Data says:** Navigate the maze using Thieving and Agility. Avoid pressure pads (step around them), pick locks on doors, and cross balance beams.
- **Wiki says (raw):** The obstacles mentioned are: contortion bars, pendulums, spinning blades, ledges, floor blades, wall traps, and floor traps. This term does not appear anywhere in the article.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Remove 'balance beams' from the list of obstacles. Replace with an accurate obstacle type such as 'contortion bars' or 'pendulums'.
- **Skeptic receipt:** Wiki (WebFetch) obstacle list: 'Floor traps, Spinning blades on pedestals, Contortion Bars, Pendulums, Wall traps, Ledges (hand-hold ledges or bridges), Floor blades, Moving blades hidden in walls, Rogue Guards' -- 'balance beams' absent.
- **Skeptic reasoning:** The wiki's obstacle list (verbatim from WebFetch) is: floor traps, spinning blades on pedestals, contortion bars, pendulums, wall traps, ledges (hand-hold ledges or bridges), floor blades, moving blades hidden in walls, and rogue guards. 'Balance beams' does not appear anywhere on the page. The closest real obstacle to a balance beam is 'ledges,' which involves crossing a gap -- not a beam mechanic. The term 'balance beams' is fabricated and should be removed.

### [high] C10: The data frames armour as optional but inadvisable ('wear nothing in armour slots to minimise failures'), implying players could choose to wear armour. The wiki states no items can be worn inside at all -- armour is not permitted, not merely inadvisable.

- **Data says:** wear nothing in armour slots to minimise failures
- **Wiki says (raw):** "no items can be brought or worn inside" and "Enter the Rogues' Den without any items in your inventory or equipped"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Change to reflect the actual rule: players cannot wear any items inside the maze. Remove the implication that wearing armour is a choice that increases failure odds.
- **Skeptic receipt:** Wiki (WebFetch): 'no items can be brought or worn inside' -- blanket prohibition, not an advisory.
- **Skeptic reasoning:** The data says 'wear nothing in armour slots to minimise failures,' phrasing the no-armour rule as optional advice to improve odds. The wiki is unambiguous that wearing items is prohibited outright: 'no items can be brought or worn inside.' Players have no choice in the matter -- the game enforces the restriction on entry. Framing it as a failure-rate optimisation misrepresents a hard game rule as a strategic preference.

### [medium] C12: The wiki states the reward is either a Rogue kit or a Rogue's equipment crate (which allows selecting a specific piece) -- not a random piece of rogue equipment given directly. The reward mechanism and naming are both wrong.

- **Data says:** search the safe to receive a random piece of rogue equipment
- **Wiki says (raw):** "Players will also receive 12 experience per stat level drained" plus "one of the following items: Rogue kit...or Rogue's equipment crate" (a crate allowing selection of specific equipment pieces, not direct equipment)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Update to: 'search a wall safe to receive either a Rogue kit or a Rogue's equipment crate (which lets you choose a specific piece)'.
- **Skeptic receipt:** Wiki (WebFetch): 'a Rogue's equipment crate or a Rogue kit. The crate lets players choose one piece of rogue equipment, or alternatively select a rogue kit instead.'
- **Skeptic reasoning:** The data says players 'receive a random piece of rogue equipment.' The wiki states the reward is 'a Rogue's equipment crate or a Rogue kit' -- the crate allows the player to choose a specific piece, meaning it is neither random nor direct equipment. Two errors: (1) the reward is a crate/kit, not equipment directly, and (2) the player selects the piece; it is not randomised.

### [blocker] C14: The data instructs players to exit via the trapdoor in the pub basement after completing the maze, but the wiki states that cracking a wall safe automatically teleports players back to the lobby. Players are not manually navigating back to a trapdoor to exit.

- **Data says:** To get remaining pieces, return through the trapdoor in the pub basement and repeat the maze.
- **Wiki says (raw):** "Cracking the safe teleports players back to the lobby"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Replace the exit instruction with: 'Cracking a wall safe at the end of the maze teleports you back to the lobby. Re-enter via the trapdoor in the pub to repeat the maze.'
- **Skeptic receipt:** Wiki (WebFetch): 'Cracking the safe teleports players back to the lobby'
- **Skeptic reasoning:** The data instructs players to 'return through the trapdoor in the pub basement' to exit after a run. The wiki states the exit is automatic: 'Cracking the safe teleports players back to the lobby.' Players do not manually navigate back to any trapdoor -- they are teleported to the lobby on safe crack. The instruction to use a trapdoor is mechanically wrong.

### [low] C16: The wiki gives the standard route time as ~3 minutes 40 seconds, not under 3 minutes. The 'under 3 minutes' figure only applies to the Level 80 Thieving shortcut route, which is not universally accessible.

- **Data says:** memorise the safe route to complete runs in under 3 minutes
- **Wiki says (raw):** "a single run can take about 3 minutes and 40 seconds to complete" (without shortcut); "With the level 80 Thieving shortcut, runs only take about 2 minutes"
- **Wiki URL:** https://oldschool.runescape.wiki/w/Rogues%27_Den
- **Suggested fix:** Update to reflect the realistic standard time: 'memorise the safe route to complete runs in approximately 3-4 minutes (under 2 minutes with the level 80 Thieving shortcut)'.
- **Skeptic receipt:** Wiki (WebFetch): 'a single run can take about 3 minutes and 40 seconds' (no shortcut); 'With the level 80 Thieving shortcut, runs only take about 2 minutes to complete.'
- **Skeptic reasoning:** The data claims 'under 3 minutes' as a general target for a memorised run. The wiki gives the standard completion time as approximately 3 minutes 40 seconds, with the level 80 Thieving shortcut reducing this to approximately 2 minutes. 'Under 3 minutes' is only achievable via the level 80 shortcut, which is not universally accessible. Presenting it as a general aspiration misstates the expected time for most players.

## Scroll Cases

### [medium] C4: The claim that minor cases unlock at 50 completions 'per tier' is incorrect for Easy (100) and Medium (100) tiers. Only Beginner, Hard, and Elite minor cases require 50 completions; Master requires 25. The guidance step overgeneralises to a single threshold of 50 that does not apply to two of the six tiers.

- **Data says:** Minor cases unlock at 50 completions per tier
- **Wiki says (raw):** | Easy | 100 | 200 |
| Medium | 100 | 250 |
- **Wiki URL:** https://oldschool.runescape.wiki/w/Scroll_case
- **Suggested fix:** Replace 'Minor cases unlock at 50 completions per tier' with tier-specific thresholds, e.g. 'Minor cases unlock at 50 (Beginner/Hard/Elite), 100 (Easy/Medium), or 25 (Master) completions depending on the tier.'
- **Skeptic receipt:** Wiki table (WebFetch, https://oldschool.runescape.wiki/w/Scroll_case): Beginner Minor=50, Easy Minor=100, Medium Minor=100, Hard Minor=50, Elite Minor=50, Master Minor=25. Data file line 16629: "description": "Complete clue scrolls of all tiers. Minor cases unlock at 50 completions per tier; major cases unlock at tier-specific higher thresholds."
- **Skeptic reasoning:** The guidance step at drop_rates.json line 16629 reads: "Minor cases unlock at 50 completions per tier". The wiki and the file's own milestoneKills fields both contradict this: Easy minor = 100, Medium minor = 100, Master minor = 25. Only Beginner (50), Hard (50), and Elite (50) match the stated threshold. The phrase 'per tier' implies a uniform value across all tiers, which is false for three of the six tiers. The per-item milestoneKills data in the same source entry is correct; only the summary guidance text is wrong.

## Castle Wars

### [high] C10: Flag captures earn 1 point (toward the game score), not 1 ticket. Tickets are awarded at game end based on the match outcome: 4 tickets for a shut-out win, 3 for a regular win or draw-with-points, 2 for a scoreless draw (on dedicated worlds). A player following this guidance would misunderstand the reward economy -- expecting 1 ticket per capture when in reality they earn 2-4 tickets at the end of a 20-minute game.

- **Data says:** Each successful capture earns 1 Castle Wars ticket.
- **Wiki says (raw):** The aim of the game is to take the enemy's standard (flag) from their castle and return it to the standard in your own castle, gaining one point each time.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Castle_Wars
- **Suggested fix:** Change to: 'Each flag capture earns 1 point toward your team's score. Tickets are awarded at game end based on the result -- 4 for a shut-out win (1-0), 3 for a regular win or draw with points, 2 for a scoreless draw (on dedicated Castle Wars worlds).'
- **Skeptic receipt:** Wiki verbatim (https://oldschool.runescape.wiki/w/Castle_Wars): "The aim of the game is to take the enemy's standard (flag) from their castle and return it to the standard in your own castle, gaining one point each time." End-of-game ticket table: "Shut-out win (1-0) = 4 tickets to winning team, 2 tickets for losing team. Regular win (2-1) = 3 tickets to the winning team, 2 tickets for the losing team. Scoreless Draw (0-0) = 2 tickets for both teams. Draw with points (1-1) = 3 tickets for both teams." Additionally: "At least 15 minutes spent in the game is required to earn tickets and plaudits."
- **Skeptic reasoning:** The data file (line 17020 of drop_rates.json) reads: "Each successful capture earns 1 Castle Wars ticket." This conflates the in-round scoring mechanic with the end-of-game ticket reward. The wiki is explicit that flag captures earn points, not tickets: "gaining one point each time." Tickets are awarded only at game end based on match outcome -- on dedicated Castle Wars worlds: 4 tickets for a shut-out win, 3 for a regular win or draw-with-points, 2 for a scoreless draw. The per-capture wording is a genuine factual error in the guidance step that would mislead a player about the reward economy.

## Soul Wars

### [blocker] C13: Both the Avatar of Creation and Avatar of Destruction use Crush (melee) attacks. The correct prayer is Protect from Melee, not Protect from Magic. Advising Protect from Magic leaves the player fully exposed to melee damage.

- **Data says:** Use Protect from Magic when fighting the Avatar directly.
- **Wiki says (raw):** Praying Protect from Melee negates both the Hitpoints and Prayer drain.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Avatar_of_Creation
- **Suggested fix:** Change "Use Protect from Magic when fighting the Avatar directly." to "Use Protect from Melee when fighting the Avatar directly."
- **Skeptic receipt:** Avatar of Creation wiki: 'Its attack targets all red team players standing in front of it, and drains prayer points equal to the Damage dealt to the player. Praying Protect from Melee negates both the Hitpoints and Prayer drain.' Avatar of Destruction wiki: identical phrasing -- 'Praying Protect from Melee negates both the Hitpoints and Prayer drain.'
- **Skeptic reasoning:** Both avatars use melee attacks and the correct prayer is Protect from Melee. The wiki page for Avatar of Creation states explicitly: 'Praying Protect from Melee negates both the Hitpoints and Prayer drain.' The Avatar of Destruction page is identical. The guidance step at line 17118 says 'Use Protect from Magic when fighting the Avatar directly.' -- that is the wrong prayer and leaves the player fully exposed to melee damage and prayer drain. No multi-source, variant, or account-type vector rescues this; it is a straightforward prayer recommendation error.

### [high] C15: The Ectoplasmator costs 250 Zeal tokens, not 2500. The data overstates the cost by a factor of 10, which would mislead players about how long they need to grind.

- **Data says:** Spend on the Ectoplasmator (2500)
- **Wiki says (raw):** Requiring 40 Prayer, it can be purchased from Nomad on the Isle of Souls for 250 Zeal Tokens.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ectoplasmator
- **Suggested fix:** Change the Ectoplasmator cost in the description from 2500 to 250.
- **Skeptic receipt:** Ectoplasmator wiki: 'Requiring 40 Prayer, it can be purchased from Nomad on the Isle of Souls for 250 Zeal Tokens.'
- **Skeptic reasoning:** The Ectoplasmator costs 250 Zeal Tokens, not 2500. The guidance step (line 17131) displays the cost parenthetically as '(2500)', which is a 10x overstatement. The wiki page is unambiguous and has not been edited in the past week. No staleness or variant explanation applies -- the item has a single price in the Nomad shop.

### [medium] C17: Unsired shards are not purchasable with Zeal tokens. The Zeal token reward shop (Nomad) does not carry them. Listing them as a Zeal reward is fabricated and will mislead players.

- **Data says:** Spend on the Ectoplasmator (2500), Soul cape (2500), or Unsired shards.
- **Wiki says (raw):** No unsired shards or unsired-related items are listed in the rewards section of this page.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Zeal_Tokens
- **Suggested fix:** Remove 'Unsired shards' from the Zeal token reward description.
- **Skeptic receipt:** Nomad wiki shop table (full enumeration): '/30}}|name=Spoils of war}} /10}}|name=Blighted entangle sack ... /10}}|name=Blighted super restore(4)...' -- no Unsired shards present anywhere in the shop listing.
- **Skeptic reasoning:** 'Unsired shards' do not exist in the Nomad/Soul Wars reward shop. 'Unsired' is an item from the Abyssal Sire, entirely unrelated to Soul Wars. The Nomad wiki page enumerates the full shop: Spoils of war (30 Zeal), Blighted entangle sack, Blighted teleport spell sack, Blighted vengeance sack, Blighted ancient ice sack, Blighted manta ray, Blighted anglerfish, Blighted karambwan, Blighted super restore(4), plus the Ectoplasmator and Soul cape. No 'Unsired shards' appear anywhere in that list. The guidance text at line 17131 listing them as a Zeal spend option is fabricated and will mislead players.

### [medium] C18: There is no 'reward chest' -- the pet comes from Spoils of War, a purchasable item costing 30 Zeal tokens from Nomad. While the 1/400 rate is correct, calling the source a 'reward chest' is inaccurate and may confuse players looking for the wrong thing.

- **Data says:** The Lil' creator pet has a 1/400 drop rate from the reward chest.
- **Wiki says (raw):** The Lil' Creator is a pet obtained from spoils of war in Soul Wars. [...] Purchase from Nomad on the Isle of Souls for 30 Zeal Tokens [...] The Soul Wars pet rate is 1/400. The roll happens on the crate separately from the loot table.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Spoils_of_war
- **Suggested fix:** Change 'reward chest' to 'Spoils of War (30 Zeal each)'.
- **Skeptic receipt:** Spoils of war wiki: 'The spoils of war is a reward from the Soul Wars Minigame. It can be purchased from Nomad on the Isle of Souls for 30 Zeal Tokens.' Avatar of Creation wiki: 'A pet version of the Avatar of Creation, Lil' creator, can be obtained as a possible reward from opening spoils of war.'
- **Skeptic reasoning:** There is no in-game object called a 'reward chest' in Soul Wars. The item that contains the Lil' creator pet is called 'Spoils of war' and costs 30 Zeal Tokens from Nomad. The guidance text at line 17131 says 'The Lil' creator pet has a 1/400 drop rate from the reward chest' -- 'reward chest' is not the name of any Soul Wars object. The pet rate of 1/400 (0.0025) is consistent with the data. Only the source name is wrong. Severity is low rather than medium, as the mechanic is correct but the item name will cause player confusion when looking for the source.

## Last Man Standing

### [medium] C1: The LMS lobby (where you speak to Lisa to enter) is at Ferox Enclave itself, not on a separate island east of it. The island is the match arena, not the lobby location. No directional 'east' description appears anywhere on the wiki page.

- **Data says:** LMS lobby is on the island east of Ferox Enclave
- **Wiki says (raw):** Players can begin the minigame by speaking to Lisa, located in Ferox Enclave.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Last_Man_Standing
- **Suggested fix:** Change guidance to: 'The LMS lobby is at Ferox Enclave -- speak to Lisa to enter a game.'
- **Skeptic receipt:** Wiki infobox: 'Location: Ferox Enclave'. Wiki body: 'Players can begin the minigame by speaking to Lisa, located in Ferox Enclave.' Wiki image caption: 'The Last Man Standing lobby in Ferox Enclave.'
- **Skeptic reasoning:** The wiki establishes unambiguously that Lisa -- and the LMS lobby -- is at Ferox Enclave itself, not on any island east of it. The match arena is the island players are transported to after entering; the lobby where you speak to Lisa is at the enclave. No 'east of Ferox Enclave' directional description appears anywhere on the wiki page. The data description is factually wrong about the lobby location.

### [blocker] C4: Competitive mode no longer requires any coin stake/entry fee -- it was removed in March 2020. The current gate is a stat requirement (750 total + 30 QP, or 1,500 total), not a coin payment. Telling players they need to stake coins would cause confusion and incorrectly describe the mode's access barrier.

- **Data says:** Competitive mode requires staking coins to enter
- **Wiki says (raw):** Competitive Mode entry fee has been removed. [...] Participating in Competitive and High Stakes requires players to have a minimum total level of 750 and 30 Quest points, or a minimum total level of 1,500 if they have less than 30 Quest points.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Last_Man_Standing
- **Suggested fix:** Replace 'requires staking coins to enter' with the current requirement: requires 750+ total level and 30+ quest points (or 1,500+ total level with fewer than 30 QP).
- **Skeptic receipt:** Wiki body: 'High Stakes - Like competitive mode, but players must pay 500,000 coins to enter.' And: 'Participating in Competitive and High Stakes requires players to have a minimum total level of 750 and 30 Quest points, or a minimum total level of 1,500 if they have less than 30 Quest points.' No coin requirement is listed for Competitive alone.
- **Skeptic reasoning:** Competitive mode carries no coin entry fee -- only High Stakes does. The wiki explicitly separates the two: Competitive is free to enter (gated by stat/QP requirements), while High Stakes requires 500,000 coins. Describing Competitive as 'requires staking coins to enter' conflates it with High Stakes and will mislead players. The stat gate applies to both Competitive and High Stakes but is not a coin payment.

### [medium] C5: Loot chests (opened with bloody/bloodier keys) contain weapons and armour (offensive and defensive gear tables), but NOT food. Food (sharks, brews, serums) is part of the fixed starting loadout given to every player on entry, not obtained from chests. The 'food' component of this claim is fabricated.

- **Data says:** Supply crates on the island contain weapons, armour, and food
- **Wiki says (raw):** To improve their chance of survival, players must loot chests throughout the island with bloody keys and bloodier keys. Chests opened with bloody keys will roll for an item from two loot tables, one of which contains offensive gear, and the other defensive, while bloodier keys access an upgraded offensive loot table.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Last_Man_Standing
- **Suggested fix:** Remove 'and food' from the supply crate description. Clarify that food comes from the starting loadout, while chests provide weapons and armour.
- **Skeptic receipt:** Wiki body: 'A player's supplies are limited to 10 sharks, 3 Saradomin brews, 2 Sanfew serums, and 1 each of every other potion for their loadout.' And: 'Chests opened with bloody keys will roll for an item from two loot tables, one of which contains offensive gear, and the other defensive, while bloodier keys access an upgraded offensive loot table.' No food items appear on any chest loot table.
- **Skeptic reasoning:** Food in LMS comes exclusively from the fixed starting loadout distributed before each match, not from any chest or crate on the island. The island chests (opened with bloody keys and bloodier keys) roll only from offensive and defensive gear loot tables -- no food items appear on any chest loot table. Stating that 'supply crates on the island contain weapons, armour, and food' is wrong on the food component; food cannot be obtained from island chests under any game mechanic described on the wiki.

## Vale Totems

### [medium] C14: Totems do NOT deplete after every ent visit. They lose one durability point per ent visit and only collapse when fully depleted -- which takes 2 visits for oak, up to 7 for redwood. The guidance implies players must rebuild every cycle, which is misleading for players using higher-tier logs.

- **Data says:** Totems deplete after each ent visit
- **Wiki says (raw):** "Totems will lose one durability for each ent that visits and will need to be rebuilt once the totem is depleted." "The starting durability of a totem is based on the type of wood used. An oak totem starts with a durability of 2, whereas a redwood totem starts with a durability of 7."
- **Wiki URL:** https://oldschool.runescape.wiki/w/Vale_Totems
- **Suggested fix:** Change to: 'Totems lose 1 durability per ent visit (e.g. oak totems last 2 visits; redwood last 7) and must be rebuilt once depleted.'
- **Skeptic receipt:** "Totems will lose one durability for each ent that visits and will need to be rebuilt once the totem is depleted. [...] An oak totem starts with a durability of 2, whereas a redwood totem starts with a durability of 7." -- https://oldschool.runescape.wiki/w/Vale_Totems (wiki_lookup output, no recent changes per wiki_updates)
- **Skeptic reasoning:** The guidance text at drop_rates.json line 17568 reads: 'Totems deplete after each ent visit and must be rebuilt with 1 log plus 4 fletched decorations.' The word 'deplete' means fully exhausted, implying a rebuild is required after every single ent visit. The wiki directly contradicts this: totems lose only one durability per ent visit and are only depleted (requiring rebuild) when all durability is gone -- a minimum of 2 visits for oak and up to 7 for redwood. No wood type in the game depletes in a single visit. The multi-source, variant, and account-type refutation vectors do not apply here -- this is a plain mechanical description error in the guidance text. No recent wiki changes were found (0 changes since 2026-06-05). The finding stands on a verbatim wiki receipt.

## Barbarian Assault

### [high] C18: The high gamble (which is the gamble that gives the pet) costs 500 honour points AND requires 1 queen kill. The claim omits the queen kill requirement entirely. A player who has never killed the Penance Queen cannot access the high gamble regardless of their honour points total.

- **Data says:** Buy Gambles (500 pts) for a chance at the Pet penance queen (1/1000 per gamble).
- **Wiki says (raw):** High: 500 and 1 queen kill
- **Wiki URL:** https://oldschool.runescape.wiki/w/Barbarian_Assault/Rewards
- **Suggested fix:** Update to: 'High Gambles (500 pts + 1 queen kill) give a chance at the Pet penance queen.' The queen kill prerequisite is a hard gate and must be surfaced.
- **Skeptic receipt:** Barbarian Assault/Gambles wiki table: 'Level: High | Honour points needed: 500 and 1 queen kill | Required wave: 1 or higher'
- **Skeptic reasoning:** The Gambles wiki page explicitly lists the high gamble cost as '500 and 1 queen kill'. The data states only '500 pts', omitting the queen kill prerequisite entirely. A player with 500 honour points but zero queen kills cannot access the high gamble at all -- the queen kill is a hard binary gate, not a soft recommendation. Omitting it from the guidance will send players to the gamble NPC prematurely and they will be unable to proceed. This is a genuine missing requirement.

## Aerial Fishing

### [high] C7: Tithe Farm teleport is not documented as a travel route to Lake Molch. The wiki's documented routes are fairy ring DJR, Skills necklace to Farming Guild (run south-east), and Kharedst's memoirs/book of the dead to Shayzien. No Tithe Farm method appears anywhere in the relevant wiki pages.

- **Data says:** Tithe Farm teleport (Hosidius) is an alternative access method that leads north-west to Lake Molch
- **Wiki says (raw):** To reach the island in the centre of Lake Molch, the player must take a boat at one of the three docks located on the northern, western, and eastern shores of the lake. [Northern or Eastern dock:] fairy ring DJR to the chasm of fire and either run south-west for the eastern dock or west for the northern dock. [Eastern dock:] Teleport to Shayzien using Kharedst's memoirs or the book of the dead and run west. [Western dock:] a skills necklace to teleport to the Farming Guild and run south-east through Molch.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Molch_Island
- **Suggested fix:** Remove the Tithe Farm teleport travel option. Replace with one of the documented routes: Skills necklace to Farming Guild then run south-east, or Kharedst's memoirs/book of the dead teleport to Shayzien then run west.
- **Skeptic receipt:** Lake Molch wiki transport section (fetched 2026-06-12): "use a skills necklace to teleport to the Farming Guild and run south-east through Molch" / "teleport to Shayzien using Kharedst's memoirs or the book of the dead and run west" / "use fairy ring DJR to the chasm of fire and either run south-west for the eastern dock or west for the northern dock" / "use any Rada's blessing to teleport to the Kourend Woodland, then travel to C: Shayzien West using the nearby Lovakengj Minecart Network". No Tithe Farm or Hosidius teleport appears.
- **Skeptic reasoning:** The Lake Molch wiki page documents four route families to the lake: fairy ring DJR, Kharedst's memoirs/book of the dead to Shayzien, skills necklace to Farming Guild (run south-east), and Rada's blessing to Kourend Woodland + Lovakengj minecart. Tithe Farm is situated in south-east Hosidius and does not appear anywhere in the transportation section. No Hosidius teleport of any kind is listed as a route to Lake Molch. The data's claim that a Tithe Farm (Hosidius) teleport leads north-west to Lake Molch is unsupported by the wiki and geographically inverted -- Lake Molch is west of Hosidius city, not north-west of Tithe Farm in a way that makes it a documented or efficient route.

### [medium] C11: The wiki describes optimal range as the player's 3x3 reach (directly adjacent, i.e. Chebyshev distance 1). The claim states 'within 3 tiles' which overstates the optimal range. A 3x3 reach means 1 tile in any direction, not 3 tiles.

- **Data says:** Pools must be within 3 tiles for the cormorant to fish them
- **Wiki says (raw):** The shortest wait occurs after targeting a fishing spot within the player's 3x3 reach (i.e. directly adjacent to the player) whereby the Cormorant will be available again on the next tick.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aerial_Fishing
- **Suggested fix:** Update to reflect that the optimal range is directly adjacent to the player (within the player's 3x3 reach / 1 tile), not 3 tiles.
- **Skeptic receipt:** Aerial Fishing wiki page (fetched 2026-06-12): "The shortest wait occurs after targeting a fishing spot within the player's 3x3 reach (i.e. directly adjacent to the player) whereby the Cormorant will be available again on the next tick."
- **Skeptic reasoning:** The wiki is explicit that the optimal range is the player's 3x3 reach, which it defines parenthetically as 'directly adjacent to the player' -- this is Chebyshev distance 1, meaning only the 8 immediately surrounding tiles. The data claim of 'within 3 tiles' describes a 7x7 area (Chebyshev distance 3), which is three times larger than the actual optimal range. This is a factual overstatement of the mechanic. The wiki's parenthetical 'i.e. directly adjacent' leaves no ambiguity. However, note that the severity is low-to-medium at most: the cormorant will still fish spots beyond 1 tile, just with a longer return wait -- so 'within 3 tiles' is not wholly incorrect as a usable range, only incorrect as the *optimal* range description.

### [low] C14: The wiki shows 3.5 Cooking XP for bluegill (the most commonly caught fish), not 3. The claim rounds down incorrectly. Other fish yield 10, 20, or 25 XP per cut, so '3 XP per offcut' is only approximately correct for bluegill and wrong for all other species.

- **Data says:** Cutting a fish produces 3 Cooking XP per offcut
- **Wiki says (raw):** Bluegill | 43 | 11.5 | 35 | 16.5 | 1 | 3.5 (Cooking XP column shows 3.5 for bluegill, 10 for common tench, 20 for mottled eel, 25 for greater siren)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aerial_Fishing
- **Suggested fix:** Update to '3.5 Cooking XP per offcut (for bluegill)' or note that XP varies by fish type (3.5 for bluegill, 10 for common tench, 20 for mottled eel, 25 for greater siren).
- **Skeptic receipt:** Aerial Fishing wiki fish table (fetched 2026-06-12): Bluegill Cooking XP = 3.5, Common tench = 10, Mottled eel = 20, Greater siren = 25. Bluegill individual page confirms: "Cutting the fish provides 3.5 Cooking experience."
- **Skeptic reasoning:** The data states '3 Cooking XP per offcut' as a flat value. The wiki table shows bluegill gives 3.5 XP (not 3), common tench gives 10 XP, mottled eel gives 20 XP, and greater siren gives 25 XP. The claimed value of 3 XP is wrong for every species: it rounds down the most common fish (bluegill) and is an order of magnitude off for the others. This is a genuine data error, not a rounding convention or variant issue.

### [blocker] C15: Two fish names ('violet perch', 'cerulean twitch') do not exist in OSRS aerial fishing. The actual species are mottled eel and greater siren. Additionally, the mechanic is wrong: Molch pearls are not obtained by trading fish to Alry -- they are random drops (1/100 to 1/75 chance) received alongside a regular catch. Following this guidance step as written would have players attempting to trade fish that cannot be traded.

- **Data says:** Caught fish can be traded with Alry the Angler for Molch pearls (bluegill=1, common tench=2, violet perch=3, cerulean twitch=4)
- **Wiki says (raw):** When fishing within Lake Molch, each catch has a 1/100 to 1/75 chance of giving Molch pearls in addition to the player's catch, which scales based on the player's Fishing and Hunter levels. [Fish species table lists:] Bluegill, Common tench, Mottled eel, Greater siren. [No fish named violet perch or cerulean twitch are mentioned anywhere on the page.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Aerial_Fishing
- **Suggested fix:** Replace 'violet perch' with 'mottled eel' and 'cerulean twitch' with 'greater siren'. Correct the mechanic: Molch pearls drop randomly alongside catches (chance scales with Fishing+Hunter level); they are not obtained by trading fish to Alry. The pearl exchange rates per fish type (=1, =2, =3, =4) are not documented on the wiki and should be removed or sourced.
- **Skeptic receipt:** Aerial Fishing wiki fish table (fetched 2026-06-12): species listed are bluegill, common tench, mottled eel, greater siren -- 'violet perch' and 'cerulean twitch' absent. Molch pearl wiki page: "Each catch has a 1/100 to 1/75 chance of giving Molch pearls in addition to the player's catch, which scales based on the player's Fishing and Hunter levels." Alry the Angler wiki page: "He teaches the player Aerial fishing and sells fishing-related items in exchange for Molch pearls" -- no fish-for-pearls trade exists; only golden tench -> 100 pearls is documented.
- **Skeptic reasoning:** Two fish names in the data ('violet perch', 'cerulean twitch') do not exist in OSRS. The four aerial fishing species are exclusively bluegill, common tench, mottled eel, and greater siren -- confirmed by the wiki fish table. Additionally the Molch pearl acquisition mechanic is completely wrong: pearls are not obtained by trading fish to Alry at tiered rates (1/2/3/4 per fish type). They are random drops received alongside catches at a 1/100 to 1/75 chance scaling with Fishing and Hunter levels. The only item Alry accepts for pearls is the golden tench (a rare catch), which yields 100 pearls flat. No per-species pearl exchange rate (=1, =2, =3, =4) is documented or exists in the game.

## Boat Paints

### [high] C9: The plugin states 25 Construction is required to apply boat paints, but the wiki consistently states the Construction requirement is 20, not 25. This is confirmed across the Boat trim page and multiple individual paint pages (Shark paint, Barracuda paint, Saradominist paint).

- **Data says:** 25 Construction level is required to apply boat paints
- **Wiki says (raw):** Each trim requires 25 Sailing and 20 Construction.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Boat_trim
- **Suggested fix:** Change the Construction requirement in guidanceSteps[1].description from 25 to 20.
- **Skeptic receipt:** wiki_lookup (Trims page): "Each trim requires 25 Sailing and 20 Construction." | WebFetch (Shark paint page): "Trimming a boat requires level 25 Sailing and 20 Construction."
- **Skeptic reasoning:** The plugin stores CONSTRUCTION: 25 in requirements.skills (drop_rates.json line 18703) and repeats '25 Construction required' in the guidance step text (line 18682). The wiki is unambiguous and doubly confirmed: the Trims page states 'Each trim requires 25 Sailing and 20 Construction' and the Shark paint individual page states 'Trimming a boat requires level 25 Sailing and 20 Construction.' No refutation vector applies -- this is not a variant item issue, not multi-source, not account-type-dependent, and wiki_updates returned zero recent changes to this page. The Construction requirement in both the requirements block and the guidance step text must be corrected from 25 to 20.

### [high] C10: The plugin states the Sailing requirement for Boat Paints is 28, but the wiki consistently states 25 Sailing is required to apply a trim. Level 28 Sailing unlocks the Bronze cannon (a shipbuilding part), not boat paint application. The gate in requirements.skills[0] should be 25, not 28.

- **Data says:** 28 Sailing level is required for Boat Paints
- **Wiki says (raw):** Each trim requires 25 Sailing and 20 Construction.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Boat_trim
- **Suggested fix:** Change the Sailing skill requirement from 28 to 25 in requirements.skills[0].
- **Skeptic receipt:** wiki_lookup (Trims page): "Each trim requires 25 Sailing and 20 Construction." | WebFetch (Shark paint page): "Trimming a boat requires level 25 Sailing and 20 Construction." | WebFetch (Sailing#Levels): "Level 28 Sailing unlocks: The Bronze cannon, described as the 'first cannon for boat combat.' Level 28 does not unlock any boat-trim features."
- **Skeptic reasoning:** The plugin stores SAILING: 28 in requirements.skills (drop_rates.json line 18699). The wiki states the requirement to apply any trim is 25 Sailing, confirmed on both the Trims page and the Shark paint page. The Sailing level table (WebFetch of Sailing#Levels) confirms level 28 exclusively unlocks the Bronze cannon -- the first cannon for boat combat -- with no relationship to trim or paint application. Level 28 is a materially wrong gate that would block players with 25-27 Sailing from being recommended this source despite being eligible. No refutation vector applies -- this is not a progress-gate nuance or account-type variant; the correct application gate is a flat 25 Sailing across all account types.

## Champion's Challenge

### [blocker] C7: Dragon Slayer I is not a requirement for Champion's Challenge. The wiki lists only two requirements: members server access and 32 quest points. Dragon Slayer I appears in the Champions' Guild template as a navigational link because the quest starts there, but it is not a prerequisite for Champion's Challenge.

- **Data says:** Dragon Slayer I is a quest requirement for this source
- **Wiki says (raw):** a player must be on a members server and have at least 32 quest points.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Champion%27s_Challenge
- **Suggested fix:** Remove Dragon Slayer I from requirements.quests. The only gate is 32 quest points (already captured in C3) and members access.
- **Skeptic receipt:** Champions' Challenge wiki (live fetch): "In order to be eligible to receive a champion's scroll, a player must be on a members server and have at least 32 quest points." -- No mention of Dragon Slayer I as a requirement anywhere on the page. Champions' Guild wiki body text: "The Champions' Guild is located south of Varrock and is accessible to players with at least 32 quest points. It has Scavvo's Rune Store and Valaine's Shop of Champions, and is also the starting point of the free-to-play quest, Dragon Slayer I." -- Dragon Slayer I is the starting point of a quest here, not a prerequisite.
- **Skeptic reasoning:** Both the Champions' Challenge wiki page and the Champions' Guild wiki page confirm that Dragon Slayer I is not a requirement for Champion's Challenge or for entering the guild. The Champion's Challenge page states requirements verbatim as only members-server access and 32 quest points. Dragon Slayer I is mentioned solely as a quest that starts at the guild, not as a gate. If the plugin's data lists Dragon Slayer I as a quest requirement for this source, that is a genuine data error with no refutation vector available -- no multi-source ambiguity, no variant, no account-type specificity applies here.

## Chompy Bird Hunting

### [medium] C1: The guidance says the hunting area is south of Rantz, but the wiki states the 3 ponds are west of Rantz. A player following the guidance would search in the wrong direction.

- **Data says:** The activity location is in Feldip Hills, south of Rantz
- **Wiki says (raw):** 3 ponds in the Feldip Hills west of Rantz (fairy ring code AKS)
- **Wiki URL:** https://oldschool.runescape.wiki/w/Chompy_Bird_Hunting
- **Suggested fix:** Change 'south of Rantz' to 'west of Rantz' in guidanceSteps[0].description.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Chompy_bird_hunting): "3 ponds in the Feldip Hills west of Rantz (fairy ring code AKS)". NPC spawn data: Rantz (NPC 1470) at (2630, 2981, plane 0); Feldip Hills ogre/hunting cluster at X?2568-2614 -- west of Rantz.
- **Skeptic reasoning:** The wiki page explicitly states the Feldip Hills hunting ponds are 'west of Rantz', not south. Rantz's primary spawn is at coordinate (2630, 2981). In the OSRS coordinate system X increases eastward, so 'west of Rantz' means lower X values -- consistent with the ogre/hunting activity cluster at X?2560-2614 confirmed by NPC spawn data. A player directed 'south of Rantz' (lower Y) would search in an entirely different area and miss the ponds. None of the refutation vectors apply: this is a plain directional string in a guidance description, not a multi-source item, not a variant ID, not account-type-dependent, and the wiki is current (the page describes post-Big Chompy Bird Hunting quest content that has not changed). The finding is a genuine data error.

## Forestry

### [high] C2: No oak trees exist at Seers' Village per the wiki. The wiki lists Seers' Village as having maple trees (north of bank) and yew trees (church). Oak trees are located at Draynor Village (east of bank). The guidance directs players to oak trees at Seers' Village, which do not exist there.

- **Data says:** Seers' Village oak trees south of the bank on a W444 Forestry world are the most common hub.
- **Wiki says (raw):** Maple: Seers village - North of the bank, there are 4 trees. Yew: Seers village - In the church court yard entrance there are 3 trees. Oak: Draynor village - East side of the bank, there are 3 trees.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Forestry
- **Suggested fix:** Change tree type from 'oak' to 'maple' (north of bank) or 'yew' (church). The locationDescription and guidanceSteps[0].description should reference maple or yew trees, not oak, at Seers' Village.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Forestry): "Oak: Draynor village - East side of the bank, there are 3 trees." and "Maple: Seers village - North of the bank, there are 4 trees."
- **Skeptic reasoning:** The wiki's official Forestry locations table places oak trees at Draynor Village (east of bank), not Seers' Village. The Seers' Village hub is maple trees, north of the bank. The data guidance text on line 19298 says 'Seers' Village oak trees south of the bank' -- wrong tree type and wrong sub-location relative to the bank. No account-type or progress gate creates an oak-at-Seers exception; the Forestry location table is universal. The waypoint coordinate (2725, 3490) is south of the bank (bankers sit at y?3495 per objects_near), consistent with the text -- but the wiki's Seers' Village Forestry hub is maples north of the bank, not oaks south of it.

### [high] C4: The wiki places the Seers' Village maple trees north of the bank, not south-east. The guidance tells players to run south-east from the bank, which is the wrong direction for the nearest Seers' Village trees. (The south-east direction also compounds the wrong tree-type issue from C2.)

- **Data says:** Use the Camelot teleport and run south-east from the bank.
- **Wiki says (raw):** Maple: Seers village - North of the bank, there are 4 trees.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Forestry
- **Suggested fix:** If the hub is updated to maple trees, change direction from 'south-east' to 'north' -- the maples are north of the Seers' Village bank per the wiki.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Forestry): "Maple: Seers village - North of the bank, there are 4 trees."
- **Skeptic reasoning:** The wiki places the Seers' Village Forestry hub (maple trees) north of the bank. The data guidance on line 19298 says 'run south-east from the bank.' South-east from the Seers' Village bank leads toward the courthouse/agility wall area, not toward any official Forestry hub. Even treating C2 and C4 as a single flawed guidance string, the direction is independently wrong: the correct direction for the Seers' Village maple hub is north, not south-east.

### [medium] C5: 'Tree spirit' is not a Forestry event. The wiki lists exactly nine named events and none is called 'tree spirit'. This appears to be a confused reference to the old Woodcutting random event (Tree Spirit) which is unrelated to the Forestry activity.

- **Data says:** Events spawn periodically (pheasant, flower, poacher, tree spirit, bee hive, roots).
- **Wiki says (raw):** The nine Forestry event types are: Rising Roots, Struggling Sapling, Flowering Tree, Woodcutting Leprechaun, Beehive, Friendly Ent, Poachers, Enchantment Ritual, Pheasant Control. There is no 'tree spirit' event listed on the page.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Forestry
- **Suggested fix:** Remove 'tree spirit' from the event list in guidanceSteps[1].description. Replace with accurate event names, e.g.: 'pheasant, flower, poacher, beehive, roots, ent, enchantment ritual, leprechaun'.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Forestry) nine events listed: "Rising Roots, Struggling Sapling, Flowering Tree, Woodcutting Leprechaun, Beehive, Friendly Ent, Poachers, Enchantment Ritual, Pheasant Control" -- no 'tree spirit' event exists in Forestry.
- **Skeptic reasoning:** The wiki enumerates exactly nine Forestry events; none is called 'tree spirit': Rising Roots, Struggling Sapling, Flowering Tree, Woodcutting Leprechaun, Beehive, Friendly Ent, Poachers, Enchantment Ritual, Pheasant Control. The data guidance on line 19308 includes 'tree spirit' in the event list. 'Tree Spirit' is a removed legacy Woodcutting random event (removed ~2012) with no relation to the Forestry activity. There is no account-type or progress gate that surfaces a 'tree spirit' event in Forestry. wiki_updates shows no Forestry page changes since 2025-01-01, so this is not a staleness issue.

### [low] C6: The wiki uses 'Anima-infused bark' as the sole currency name throughout. There is no distinct 'Forestry points' currency -- the guidance lists them as two separate reward types, but the wiki treats Anima-infused bark as the single reward currency for events and shop purchases.

- **Data says:** Participate in each event for Anima-infused bark and Forestry points.
- **Wiki says (raw):** Reward currency [Anima-infused bark]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Forestry
- **Suggested fix:** Change 'Anima-infused bark and Forestry points' to just 'Anima-infused bark' in guidanceSteps[1].description. The shop references in C7/C8 ('Spend points at the Forestry Shop') should also be updated to 'Spend Anima-infused bark at the Forestry Shop'.
- **Skeptic receipt:** Wiki (https://oldschool.runescape.wiki/w/Forestry) infobox: "Reward currency: Anima-infused bark" -- sole currency named throughout the page; no 'Forestry points' currency mentioned anywhere.
- **Skeptic reasoning:** The wiki names a single reward currency throughout: 'Anima-infused bark' (infobox: 'Reward currency: Anima-infused bark'). There is no separate 'Forestry points' currency. The data guidance on line 19308 reads 'Anima-infused bark and Forestry points' as if they are two distinct reward types. The internal schema uses a pointCost field to represent Anima-infused bark shop costs, but that is a schema implementation detail -- no player-facing currency called 'Forestry points' exists in the game.

## Fossil Island Notes

### [medium] C7: The guidance says to search 'displays' but the wiki consistently describes the searchable objects as 'stone chests'. There are no 'displays' in the House on the Hill -- the interactive objects that yield notes are stone chests requiring 100 numulite each. A player looking for 'displays' may not recognise the stone chests as the correct object.

- **Data says:** Search the House on the Hill displays and complete activities for fossil notes.
- **Wiki says (raw):** Within both floors of the House, there are five stone chests with a search option. Searching the stone chest will ask for a numulite. After inserting the numulite, the chest will give the search option to insert 100 numulites into the hole. The chests have a chance of having nothing happen, dealing damage, or giving one of various notes.
- **Wiki URL:** https://oldschool.runescape.wiki/w/House_on_the_Hill
- **Suggested fix:** Change 'Search the House on the Hill displays' to 'Search the stone chests in the House on the Hill (100 numulite each) and complete activities for fossil notes.'
- **Skeptic receipt:** Within both floors of the House, there are five stone chests with a search option. Searching the stone chest will ask for a numulite. After inserting the numulite, the chest will give the search option to insert 100 numulites into the hole. The chests have a chance of having nothing happen, dealing damage, or giving one of various notes. -- wiki_lookup("House on the Hill")
- **Skeptic reasoning:** The data uses the word 'displays' to describe the interactive objects players must find in the House on the Hill. The wiki_lookup output for 'House on the Hill' names these objects 'stone chests' in every reference on the page and makes no mention of any object called 'displays.' There are no objects labelled 'displays' in the House on the Hill -- the only interactable note-yielding objects are the five stone chests that cost 100 numulite to search. No multi-source, variant, account-type, or staleness vector applies; this is a guidance-text accuracy issue where the non-standard term 'displays' could cause a player to fail to identify the correct clickable object. The wiki receipt directly contradicts the data wording.

## Hunter Guild

### [medium] C3: Direction from teleport is wrong. The data says run south-west; the wiki says north-west (from fairy ring AJP). More importantly, the quetzal whistle teleports players directly to 'the quetzal landing zone in front of the guild' -- no running is required at all when using the whistle.

- **Data says:** run south-west to the guild
- **Wiki says (raw):** use the fairy ring AJP and run north-west to reach the Hunters Guild
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hunter_Guild
- **Suggested fix:** Remove the south-west direction. If retaining travel instructions: 'Use the Quetzal whistle to teleport directly to the guild landing zone.' If referencing fairy ring: 'Fairy ring AJP, then run north-west to the guild.'
- **Skeptic receipt:** Wiki Hunter Guild page: 'A quetzal whistle teleports players to the quetzal landing zone in front of the guild.' and 'Use the fairy ring [AJP] and run north-west to reach the Hunters Guild.'
- **Skeptic reasoning:** The quetzal whistle teleports directly to the landing zone in front of the guild -- no running in any direction is needed when using that method. The only travel method that involves running is the fairy ring, and that direction is north-west (not south-west). The guidance step says 'Use the Quetzal whistle to teleport to Varlamore, then run south-west to the guild,' which is wrong on both counts: the whistle drops the player at the guild entrance (no run needed), and the fairy-ring direction is north-west. Neither variant of south-west running is supported.

### [blocker] C7: There is no Hunter Guild points currency. The guild's reward mechanic is Hunters' Rumours, which award Hunter XP and hunters' loot sacks of varying quality -- not a spendable points currency. Guidance telling players to accumulate 'Hunter Guild points' to spend at a 'reward shop' describes a mechanic that does not exist.

- **Data says:** Track and catch creatures in the guild hunting grounds for Hunter Guild points. Spend points at the guild reward shop for guild-exclusive items.
- **Wiki says (raw):** Completing Hunters' Rumours rewards the player with additional Hunter experience and a hunters' loot sack, the quality of which depends on the tier of the rumour assigned.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hunter%27s_rumour
- **Suggested fix:** Replace with the actual mechanic: 'Complete Hunters\' Rumours assigned by a Guild Hunter. Each completed rumour rewards Hunter XP and a hunters\' loot sack (quality scales with rumour tier: Novice/Adept/Expert/Master). Milestone completions (10, 25, 100, 250 rumours) unlock quetzal whistle blueprints and other rewards.'
- **Skeptic receipt:** Wiki Hunters' Rumours page: 'Completing Hunters' Rumours rewards the player with additional Hunter experience and a hunters' loot sack, the quality of which depends on the tier of the rumour assigned.' Wiki Hunter Guild page: 'Completing Hunters' Rumours reward the player with Hunter experience and a hunter's loot sack; the quality of these depend on the tier of the rumour assigned. Hunter's loot sacks typically contain various hunter meats, furs, and blessed bone shards, and may also reward unique items like the guild hunter outfit and huntsman's kit.'
- **Skeptic reasoning:** No 'Hunter Guild points' currency exists. The guidance step text reads: 'Track and catch creatures in the guild hunting grounds for Hunter Guild points. Spend points at the guild reward shop for guild-exclusive items.' The actual mechanic is Hunters' Rumours, which reward Hunter XP and hunters' loot sacks of varying quality -- not a spendable points currency. The guild-exclusive items (quetzal whistle blueprints, guild hunter outfit, huntsman's kit, Quetzin) come from loot sacks and rumour milestones, not a points shop. This is a fabricated mechanic description, not a game mechanic simplification.

### [blocker] C8: No points-based reward shop exists at the Hunter Guild. The wiki describes the guild's shops as Imia's Supplies, Pellem's Fur Store, and The Burrow (general shops) -- not a points-redemption reward shop. The fabricated 'guild-exclusive reward shop' mechanic does not exist.

- **Data says:** Items at the Hunter Guild reward shop are exclusive to the guild
- **Wiki says (raw):** Completing Hunters' Rumours rewards the player with additional Hunter experience and a hunters' loot sack, the quality of which depends on the tier of the rumour assigned.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hunter%27s_rumour
- **Suggested fix:** Remove the reward shop claim. The guild-exclusive items (quetzal whistle blueprints) are unlocked via rumour milestones, not purchased from a shop with points.
- **Skeptic receipt:** Wiki Hunter Guild page: 'players who have completed ten Hunters' Rumours can [unlock quetzal whistles]' and 'Completing Hunters' Rumours reward the player with Hunter experience and a hunter's loot sack.' No points shop is mentioned anywhere on the Hunter Guild wiki page.
- **Skeptic reasoning:** No points-based reward shop exists at the Hunter Guild. The guild's actual shops (Imia's Supplies, Pellem's Fur Store, The Burrow pub) are general supply shops, not points-redemption stores. Guild-exclusive items such as quetzal whistle blueprints are unlocked by completing rumour milestones (e.g., 10 rumours unlocks the whistle), not purchased with an accumulated points currency. The claim that guild-exclusive items are available via a 'reward shop' describes a mechanic that does not exist in the game.

### [high] C10: There are no Hunter Guild points, so the claim that higher levels give greater point rewards is doubly wrong. Higher Hunter levels do unlock higher rumour tiers (Adept 57, Expert 72, Master 91) which award better-quality loot sacks -- but this is loot sack quality scaling, not a points currency.

- **Data says:** Higher Hunter levels provide greater Hunter Guild point rewards
- **Wiki says (raw):** Completing Hunters' Rumours rewards the player with additional Hunter experience and a hunters' loot sack, the quality of which depends on the tier of the rumour assigned.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Hunter%27s_rumour
- **Suggested fix:** Replace with: 'Higher Hunter levels unlock higher rumour tiers (Adept at 57, Expert at 72, Master at 91), which reward higher-quality hunters\' loot sacks.'
- **Skeptic receipt:** Wiki Hunters' Rumours page tier table: 'Novice [level not shown] | Adept [57] | Expert [72] | Master [91, also requires At First Light]' and 'Completing Hunters' Rumours rewards the player with additional Hunter experience and a hunters' loot sack, the quality of which depends on the tier of the rumour assigned.' No points currency or points-scaling mechanic is described anywhere.
- **Skeptic reasoning:** Because no 'Hunter Guild points' currency exists (confirmed by C7/C8), the claim that 'Higher Hunter levels provide greater Hunter Guild point rewards' is doubly wrong: there are no points, and the guidance text compounds the fabricated mechanic. What is true is that higher Hunter levels unlock higher rumour tiers (Adept at 57, Expert at 72, Master at 91 + At First Light), which award higher-quality loot sacks -- but this is tier-gated loot sack quality, not a points scaling system. The guidance step text actively misleads players about how the activity works.

## Lost Schematics

### [high] C5: The plugin sets the gate at Sailing level 1, but the lowest-level schematic (Salvaging station) requires Sailing 42. No lost schematic is accessible at level 1.

- **Data says:** "skill": "SAILING", "level": 1
- **Wiki says (raw):** Salvaging station schematic | 42 [the lowest entry in the full schematic requirements table; no schematic is listed below Sailing 42]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Lost_schematics
- **Suggested fix:** Change the SAILING skill requirement from level 1 to level 42 (the minimum needed for the first lockbox schematic, the Salvaging station schematic).
- **Skeptic receipt:** From https://oldschool.runescape.wiki/w/Lost_schematics (WebFetch, rendered table): 'Salvaging station schematic | 42 | Lockbox (Chinchompa Island)' ... 'Dragon keel schematic | 87 | Lockbox (Grimstone)'. Quest schematics: 'Ballistic attractor schematic | Ava | Completion of Animal Magnetism and Pandemonium' and 'Bosun's workbench schematic | Elder Raley (Level 52) | Completion of The Red Reef'. Wiki text: 'You cannot boost your Sailing level to moor on islands you would not normally have the level for.' The lowest Sailing level for any lockbox schematic is 42; no schematic is accessible at Sailing level 1.
- **Skeptic reasoning:** The wiki confirms that the Lost Schematics source has no Sailing level 1 access gate. All ten lockbox schematics are gated by per-island Sailing level requirements ranging from 42 to 87 -- the lowest being the Salvaging station schematic at Sailing 42. The two quest-awarded schematics (Ballistic attractor, Bosun's workbench) require no Sailing level at all but are gated by quest completion (Animal Magnetism + Pandemonium, and The Red Reef respectively). The current data entry lists only `SAILING level: 1` as the sole requirement, which is wrong on two counts: it misrepresents the minimum Sailing level for the island-based schematics (should be 42, not 1), and it omits the quest requirements for the non-island schematics entirely. No game mechanic makes Sailing level 1 a correct access gate for this source. The `level: 1` pattern used for Port Tasks and Deep Sea Trawling (where Sailing 1 genuinely allows access) does not apply here -- the wiki explicitly states that players cannot boost to access islands above their level, meaning the level gates are hard per-island requirements, not soft activity floors.

## Monkey Backpacks

### [high] C2: The guidance claims Monkey Madness II is the access gate for the Ape Atoll Agility Course. The wiki shows only partial Monkey Madness I (Chapter 2) is required for course access. Monkey Madness II is relevant only for obtaining the backpack variants, not for entering the course.

- **Data says:** Monkey Madness II quest is required to access the Ape Atoll Agility Course
- **Wiki says (raw):** Access to this course requires partial completion of Monkey Madness I, up to Chapter 2.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ape_Atoll_Agility_Course
- **Suggested fix:** Change the access gate requirement to 'partial completion of Monkey Madness I (up to Chapter 2)'. The Monkey Madness II requirement should be noted separately as a prerequisite for backpack drop eligibility, not course access.
- **Skeptic receipt:** wiki_lookup: 'Access to this course requires partial completion of Monkey Madness I, up to Chapter 2.' (https://oldschool.runescape.wiki/w/Ape_Atoll_Agility_Course)
- **Skeptic reasoning:** The wiki is unambiguous that course access requires only partial completion of Monkey Madness I (up to Chapter 2), not Monkey Madness II. No refutation vector applies: this is not account-type dependent (all accounts share the same access gate), the wiki page has had zero recent changes since 2025-01-01 (wiki_updates returned count:0), and the receipt is a direct quote -- not a paraphrase. MM II is relevant only for backpack drop eligibility, not course entry. The data erroneously gates course access behind MM II.

### [medium] C3: The guidance describes the course start as 'the top of an agility ladder'. The wiki identifies the first obstacle as a Stepping stone with no agility ladder at the start of the course.

- **Data says:** The Ape Atoll Agility Course starts at the top of an agility ladder
- **Wiki says (raw):** The course begins with a "Stepping stone" obstacle that rewards 40 Agility experience. There is no agility ladder mentioned as a starting point.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ape_Atoll_Agility_Course
- **Suggested fix:** Update the description of the course starting point to reference the stepping stone area near Marim rather than 'the top of an agility ladder'.
- **Skeptic receipt:** WebFetch obstacle table: '1. Stepping Stone (40 XP), 2. Tropical Tree - Climb (40 XP), 3. Monkeybars (40 XP), 4. Skull Slope (60 XP), 5. Rope (100 XP), 6. Tropical Tree - Climb-down (0 XP)' -- no agility ladder present. (https://oldschool.runescape.wiki/w/Ape_Atoll_Agility_Course)
- **Skeptic reasoning:** The obstacle table from the wiki lists the first obstacle as 'Stepping Stone (40 XP)', not an agility ladder. The course has six obstacles in order: Stepping Stone, Tropical Tree (climb), Monkeybars, Skull Slope, Rope, Tropical Tree (climb-down). No agility ladder appears anywhere in the course. The wiki_updates tool returned zero changes to this page since 2025-01-01, ruling out staleness. No multi-source or variant vector applies. The data's description of a starting 'agility ladder' does not match the course layout.

## Ocean Encounters

### [medium] C1: The wiki describes four fixed named destinations unlocked via Sailors' Markers -- there is no 'nearest activated port' routing mechanic. The amulet gives a menu of unlocked destinations; it does not dynamically select the nearest one.

- **Data says:** Sailors' amulet is used to teleport to The Pandemonium or nearest activated port
- **Wiki says (raw):** The amulet allows the player to teleport to the Pandemonium, Port Roberts, Red Rock, and Deepfin Point. In order to make use of the amulet's destinations, the player must activate their corresponding Sailors' Marker there.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sailors%27_amulet
- **Suggested fix:** Replace 'nearest activated port' with 'any port whose Sailors\' Marker you have activated' (or similar). E.g.: 'Use the Sailors\' amulet to teleport to The Pandemonium or any port where you have activated the Sailors\' Marker.'
- **Skeptic receipt:** "The amulet allows the player to teleport to the Pandemonium, Port Roberts, Red Rock, and Deepfin Point. In order to make use of the amulet's destinations, the player must activate their corresponding Sailors' Marker there." / "The teleport to The Pandemonium is unlocked by default. To use the teleports to Port Roberts, Red Rock, and Deepfin Point, the player must first inspect the Sailors' Marker on their docks." -- wiki_lookup: Sailors' amulet (https://oldschool.runescape.wiki/w/Sailors'_amulet)
- **Skeptic reasoning:** The wiki page for the Sailors' amulet describes four fixed, individually named teleport destinations (The Pandemonium, Port Roberts, Red Rock, Deepfin Point), each unlocked by inspecting the corresponding Sailors' Marker. The Pandemonium is unlocked by default; the others require per-port marker activation. There is no proximity-based 'nearest activated port' routing mechanic anywhere on the page. The guidance text's phrase 'nearest activated port' implies a dynamic auto-select that does not exist -- the amulet gives a menu of individually unlocked fixed destinations.

### [medium] C3: The wiki is explicit that the spawn timer only counts down while the boat is moving and pauses when stationary. Turning in place is stationary, so encounters do not spawn while turning in place -- the parenthetical '(not moving)' directly contradicts the wiki mechanic.

- **Data says:** Ocean encounters spawn randomly while sailing, including when turning in place (not moving)
- **Wiki says (raw):** When a boat is moving, a 120 tick (72 second) timer counts down, and when it hits zero, an event has a 1/6 chance to spawn. The timer pauses if a boat is stationary or if the player disembarks.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Ocean_encounter
- **Suggested fix:** Remove the 'including when turning in place (not moving)' clause. The description should note that encounters only spawn while the boat is actively moving.
- **Skeptic receipt:** "When a boat is moving, a 120 tick (72 second) timer counts down, and when it hits zero, an event has a 1/6 chance to spawn. [...] The timer pauses if a boat is stationary or if the player disembarks" / "As long as a boat is moving, its speed does not influence the spawn chance or duration between spawns." -- wiki_lookup: Ocean encounters (https://oldschool.runescape.wiki/w/Ocean_encounters)
- **Skeptic reasoning:** The wiki page for Ocean encounters is unambiguous: the 120-tick spawn timer counts down only while the boat is moving and explicitly pauses when the boat is stationary. 'Turning in place' is a stationary state (no translational movement through the water), so no spawn timer progress occurs while turning. The data's parenthetical '(not moving)' directly contradicts the documented mechanic. Additionally, the wiki notes that boat speed does not influence spawn rate, which confirms the only relevant variable is whether the boat is moving at all -- not how fast. No wiki staleness applies: zero edits to the Ocean encounters page since 2026-01-01.

## Sailing Misc

### [medium] C2: The amulet does not teleport to the 'nearest activated port' -- it has four fixed, named destinations unlocked individually by inspecting Sailors' Markers at each dock. There is no dynamic nearest-port selection. The description implies context-aware routing that does not exist.

- **Data says:** Sailors' amulet can teleport to 'your nearest activated port'
- **Wiki says (raw):** The teleport to The Pandemonium is unlocked by default. [Other destinations: Port Roberts (requires Sailing 50), Red Rock (requires Sailing 52), Deepfin Point (requires Sailing 67). Players must first inspect the corresponding Sailors' Marker at their docks to unlock each teleport option.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Sailors%27_amulet
- **Suggested fix:** Replace 'your nearest activated port' with 'a chosen unlocked port (Pandemonium by default; others require inspecting the Sailors' Marker at Port Roberts, Red Rock, or Deepfin Point)'.
- **Skeptic receipt:** Wiki (Sailors' amulet): 'The teleport to The Pandemonium is unlocked by default. To use the teleports to Port Roberts, Red Rock, and Deepfin Point, the player must first inspect the Sailors' Marker on their docks.' Data line 20358: 'Use Sailors' amulet to teleport to The Pandemonium or your nearest activated port.'
- **Skeptic reasoning:** The guidance description text at line 20358 says 'your nearest activated port', implying dynamic context-aware routing to the geographically closest unlocked destination. The wiki is unambiguous: the amulet has four fixed named destinations (Pandemonium unlocked by default; Port Roberts, Red Rock, and Deepfin Point each unlocked individually by inspecting the Sailors' Marker at that specific dock). The player selects which unlocked destination to teleport to -- there is no nearest-port routing logic. Notably, the travelTip on the same step already correctly says 'The Pandemonium (default unlock)', which partially contradicts the description's 'nearest activated port' framing. The description text is misleading about how the amulet works.

### [high] C9: Echo pearl is a boat combat drop from Dolphins and Orcas, not an ocean encounter reward. Ocean encounters give pearls ranging from tiny to radiant (plus clue scrolls, cocktails, etc.), but not Echo pearl. Sending players to grind ocean encounters for Echo pearl would waste real effort.

- **Data says:** Ocean encounters yield Echo pearl
- **Wiki says (raw):** An echo pearl is an item dropped by dolphins and orcas. [Dolphin (Combat level 22): Drop rate 1/1,200; Orca (Combat level 205): Drop rate 1/70 or 1/35.]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Echo_pearl
- **Suggested fix:** Move Echo pearl to the boat combat description (alongside Dragon nails, Dragon cannon barrel, Bottled storm) rather than ocean encounters. Correct the source to 'dropped by Dolphins and Orcas during boat combat'.
- **Skeptic receipt:** Wiki (Dolphin drop table): 'Echo pearl | qty: 1 | rarity: 1/1200'. Wiki (Orca drop table): 'Echo pearl | qty: 1 | rarity: 1/70'. Wiki (Ocean encounters page): full reward table lists strong winds, mysterious glow, lost crate, castaway, giant clam, clue turtle, Ocean Man, lost casket -- no Echo pearl entry. Data line 20363: 'ocean encounters (Echo pearl)'.
- **Skeptic reasoning:** The guidance text at line 20363 explicitly parenthesises Echo pearl as an ocean encounter reward: 'ocean encounters (Echo pearl)'. The wiki's Ocean encounters page lists every ocean encounter type and its possible rewards (strong winds, mysterious glow, lost crate, castaway, giant clam, clue turtle, Ocean Man, lost casket) -- Echo pearl does not appear in any of them. The Dolphin and Orca drop tables confirm Echo pearl is exclusively a boat combat drop. Placing Echo pearl under ocean encounters would send players grinding the wrong activity entirely.

## Sea Treasures

### [blocker] C3: Medallion fragments 1-8 combine into the Medallion of the Deep, not the Sailors' amulet (inert). The Sailors' amulet (inert) is obtained through shipwreck salvaging at rates of 1/300 to 1/2,000 depending on salvage type. These are two entirely separate items with different obtain methods. Following the current guidance step would lead a player to believe collecting medallion fragments is how they get a Sailors' amulet -- that is factually wrong.

- **Data says:** Interact with the treasure spot to collect Medallion fragments (1-8, combine to form Sailors' amulet (inert))
- **Wiki says (raw):** There are eight of these fragments, which are assembled into the Medallion of the Deep, an item that allows the wielder to breathe underwater.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Medallion_fragment
- **Suggested fix:** Change 'Medallion fragments (1-8, combine to form Sailors' amulet (inert))' to 'Medallion fragments (1-8, combine to form Medallion of the Deep)'. The Sailors' amulet (inert) should be noted separately as obtained from shipwreck salvaging (see Sailors'_amulet_(inert) wiki page).
- **Skeptic receipt:** wiki_lookup('Medallion fragment'): 'There are eight of these fragments, which are assembled into the Medallion of the Deep, an item that allows the wielder to breathe underwater.' | wiki_lookup('Sailors\' amulet'): 'The sailors\' amulet is an item that can be obtained as loot when sorting through salvage obtained from shipwreck salvaging.'
- **Skeptic reasoning:** The guidance step states that Medallion fragments 1-8 'combine to form Sailors' amulet (inert)'. The wiki_lookup on 'Medallion fragment' returns verbatim: 'There are eight of these fragments, which are assembled into the Medallion of the Deep, an item that allows the wielder to breathe underwater.' The wiki_lookup on 'Sailors' amulet' confirms it is an entirely separate item: 'The sailors' amulet is an item that can be obtained as loot when sorting through salvage obtained from shipwreck salvaging.' No variant mechanic, no multi-source overlap, and no account-type gate resolves this -- the data names the wrong assembly product for Medallion fragments. The receipt is unambiguous and the finding stands as a blocker.

## Deep Sea Fishing

### [high] C3: The guidance instructs players to obtain a trawling net 'from Netmaster or Sailing shops', but the wiki shows nets are boat facilities built via Construction -- they are not purchasable items carried in inventory. Netmaster Kellan is purely an informational NPC. Directing players to a shop that does not exist wastes real effort.

- **Data says:** Bring a trawling net (from Netmaster or Sailing shops)
- **Wiki says (raw):** The wiki describes trawling nets only through construction recipes: each net type lists specific Materials and Construction XP values (e.g. Rope trawling net: 'Rope x7, Teak plank x4, Steel bar x4, Lead bar x2' with 458 Construction XP). Netmaster Kellan 'provides an in-game explanation on the basics of deep sea trawling' -- no shop inventory is described. No sailing shop selling pre-made nets is mentioned anywhere on the page.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Deep_sea_trawling
- **Suggested fix:** Remove '(from Netmaster or Sailing shops)'. Trawling nets are boat facilities built via Construction, not items obtained from a vendor. Guidance should instead note that the boat must have a trawling net facility built before heading out.
- **Skeptic receipt:** wiki_lookup (Trawling net): 'A trawling net is a type of facility that can be installed on player-owned skiffs and sloops.' Materials table shows Rope trawling net requires 'x7 [rope], x4 [teak plank], x4 [steel bar], x2 [lead bar]' built at a facility hotspot. wiki_lookup (Rope trawling net): 'It can be built at the corresponding facility hotspot, requiring level 56 Sailing and 45 Construction.' wiki_lookup (Deep sea trawling): 'Netmaster Kellan, a retired trawler found at the Rolling Tide pub in Port Roberts, provides an in-game explanation on the basics of deep sea trawling.' -- No vendor sale of nets is described anywhere.
- **Skeptic reasoning:** The guidance says 'Bring a trawling net (from Netmaster or Sailing shops)' -- this frames a trawling net as a carried inventory item purchasable from a vendor. The wiki is unambiguous: trawling nets are boat facilities installed via Construction materials; they are never sold by any shop NPC. Netmaster Kellan is confirmed as purely informational. No Sailing shop selling pre-made nets exists. The guidance will mislead players into searching for a non-existent vendor item before their trip.

## Pyramid Plunder

### [high] C3: The grand gold chest is present in every room starting from room 1 (21 Thieving). The 71 Thieving figure is the requirement to enter room 6, not a gate on the grand gold chest itself. A player with 21+ Thieving can access the chest in room 1.

- **Data says:** Need 71 Thieving minimum to access the grand gold chest
- **Wiki says (raw):** There is a golden chest in the centre of each room. [...] Every ten levels thereafter will grant entrance to the more valuable rooms, up to level 91. [Room 1: 21+, Room 2: 31+, Room 3: 41+, Room 4: 51+, Room 5: 61+, Room 6: 71+, Room 7: 81+, Room 8: 91+]
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pyramid_Plunder
- **Suggested fix:** Remove the 71 Thieving gate on the grand gold chest, or clarify it as the room 6 access requirement. The chest is available in all rooms; the minimum to enter the minigame at all is 21 Thieving (room 1).
- **Skeptic receipt:** Pyramid Plunder wiki (via wiki_lookup): 'At least 21 Thieving is necessary to start the game. Every ten levels thereafter will grant entrance to the more valuable rooms, up to level 91.' Room table: Room 1: 21+, Room 2: 31+, Room 3: 41+, Room 4: 51+, Room 5: 61+, Room 6: 71+, Room 7: 81+, Room 8: 91+. 'There is a golden chest in the centre of each room.' The 71 figure is room 6's entry requirement, not a gate on the golden chest generically.
- **Skeptic reasoning:** The data at line 20710 states 'Need 71 Thieving minimum to access the grand gold chest (the only Pharaoh's sceptre source).' The wiki confirms the golden chest is present in every room, with room 1 accessible at 21 Thieving and room 8 (the optimal target) requiring 91 Thieving. The figure 71 corresponds to room 6 only -- it is neither the minimum to access any golden chest (21) nor the requirement for the room the guidance step later identifies as the target (91, per line 20739). The finding correctly identifies the 71 level as misattributed. The parenthetical claim that the grand gold chest is 'the only Pharaoh's sceptre source' is also contradicted -- the sceptre drops from sarcophagi as well -- but that error is covered under C8. The 71 figure standing alone as a chest-access gate is a genuine data error.

### [blocker] C8: The grand gold chest does not exclusively yield the Pharaoh's sceptre. It primarily yields artefacts and only rarely contains a sceptre. Describing it as an exclusive sceptre source is factually inverted -- the sceptre is the rare outcome, not the guaranteed one.

- **Data says:** The grand gold chest exclusively yields the Pharaoh's sceptre
- **Wiki says (raw):** There is a golden chest in the centre of each room. The player will always open it instantly, and may rarely find a Pharaoh's sceptre inside one.
- **Wiki URL:** https://oldschool.runescape.wiki/w/Pyramid_Plunder
- **Suggested fix:** Change the description to state that the grand gold chest yields artefacts and may rarely contain a Pharaoh's sceptre, rather than claiming it exclusively yields the sceptre.
- **Skeptic receipt:** Pharaoh's sceptre wiki (via wiki_lookup): 'Pharaoh's sceptres are a very rare reward received from the golden chests and sarcophagi during the Pyramid Plunder minigame.' Pyramid Plunder wiki: 'There is a golden chest in the centre of each room. The player will always open it instantly, and may rarely find a Pharaoh's sceptre inside one.' The sceptre is rare (not exclusive), and sarcophagi are a confirmed parallel source.
- **Skeptic reasoning:** The data at line 20739 states the grand gold chest 'exclusively yields the Pharaoh's sceptre.' This is doubly wrong. First, the wiki states the sceptre is a rare outcome ('may rarely find a Pharaoh's sceptre inside one'), not the exclusive yield -- the chest can be opened without producing a sceptre, and artefacts are the primary loot. Second, the sceptre is obtainable from sarcophagi as well as golden chests, so even framing the chest as the exclusive sceptre source is incorrect. 'Exclusively yields' inverts the rarity relationship and ignores sarcophagi as a confirmed second sceptre source. The finding stands on both counts with direct wiki receipts.

## Stronghold of Security

### [blocker] C6: Flesh crawlers drop the bottom of sceptre, not the left skull half. The guidance has the drop assignments for flesh crawlers (Floor 2) and ankous (Floor 4) inverted.

- **Data says:** Floor 2 (Catacomb of Famine) - flesh crawlers and other monsters drop the left skull half
- **Wiki says (raw):** level 28/35/41 flesh crawlers, which drop the bottom of sceptre for the skull sceptre
- **Wiki URL:** https://oldschool.runescape.wiki/w/Stronghold_of_Security
- **Suggested fix:** Change 'flesh crawlers and other monsters drop the left skull half' to 'flesh crawlers drop the bottom of sceptre'
- **Skeptic receipt:** Flesh Crawler drop table (wiki_lookup): 'Bottom of sceptre | qty: 1 | rarity: 3/100'. Skull Sceptre wiki (WebFetch): 'Bottom of Sceptre - dropped by flesh crawlers on the second level'. Stronghold of Security wiki (WebFetch): 'Level 2 - Catacomb of Famine: Flesh crawlers drop the bottom of sceptre'.
- **Skeptic reasoning:** The data claims flesh crawlers (Floor 2, Catacomb of Famine) drop the left skull half. All three independent sources agree this is wrong: flesh crawlers drop the bottom of sceptre. The Flesh Crawler wiki drop table is unambiguous, the Skull Sceptre wiki states 'dropped by flesh crawlers on the second level' for the bottom of sceptre, and the Stronghold of Security page confirms 'Flesh crawlers drop the bottom of sceptre'. The left skull half belongs to ankous on Floor 4. This is the same inversion described in C8.

### [blocker] C8: Ankous drop the left skull half, not the bottom of sceptre. The guidance has the drop assignments for ankous (Floor 4) and flesh crawlers (Floor 2) inverted.

- **Data says:** Floor 4 (Sepulchre of Death) - ankous drop the bottom of sceptre
- **Wiki says (raw):** level 75/82/86 ankou, which drop the left skull half for the skull sceptre
- **Wiki URL:** https://oldschool.runescape.wiki/w/Stronghold_of_Security
- **Suggested fix:** Change 'ankous drop the bottom of sceptre' to 'ankous drop the left skull half'
- **Skeptic receipt:** Ankou drop table (wiki_lookup): 'Left skull half | qty: 1 | rarity: 3/100' -- no bottom of sceptre entry present. Skull Sceptre wiki (WebFetch): 'Left Skull Half - dropped by ankou on the fourth level'. Stronghold of Security wiki (WebFetch): 'Level 4 - Sepulchre of Death: Ankou drop the left skull half'.
- **Skeptic reasoning:** The data claims ankous (Floor 4, Sepulchre of Death) drop the bottom of sceptre. All three independent sources agree this is wrong: ankous drop the left skull half. The Ankou wiki drop table lists 'Left skull half | qty: 1 | rarity: 3/100' with no bottom of sceptre entry anywhere. The Skull Sceptre wiki states 'dropped by ankou on the fourth level' for the left skull half. The bottom of sceptre belongs to flesh crawlers on Floor 2. This is the same inversion described in C6.

Tranche 4 total: 108 confirmed findings.