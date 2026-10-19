# Royal Arena Parts 0–11

## Part 0 — Template cleanup

- `settings.gradle`: project name changed to `suroyalarena_1_20_1`.
- `gradle.properties`: Royal Arena identity + license fixed.
- `assets/royalarena`: replaces `assets/examplemod`.
- `royalarena.mixins.json`: refmap fixed to `royalarena.refmap.json`.
- Removed the unused template `WConfig` example.

## Part 1 — Match domain

- `match/TeamSide.java`
- `match/MatchState.java`
- `match/MatchPlayer.java`
- `match/MatchInstance.java`

All gameplay decisions belong to server-owned `MatchInstance`.

## Part 2 — Arena layout

- `arena/ArenaPoint.java`
- `arena/ArenaBox.java`
- `arena/ArenaLayout.java`
- `server/arena/ArenaAllocator.java`
- `server/arena/ArenaBuilder.java`

`ArenaLayout` is the single coordinate source for towers, deployment areas and
camera/controller anchors.

## Part 3 — BLUE/RED camera

- `client/camera/ArenaCameraProfile.java`
- `client/camera/ArenaCameraTransform.java`
- `client/camera/ArenaCameraController.java`

One camera profile is mirrored by `TeamSide`; there are no separate
BlueCamera/RedCamera classes.

## Part 4 — Match lifecycle + sync

- `server/match/ServerMatchSession.java`
- `server/match/MatchManager.java`
- `network/ModNetwork.java`
- `network/s2c/S2CMatchSnapshotPacket.java`
- `client/state/ClientMatchState.java`

Lifecycle: `WAITING -> COUNTDOWN -> BATTLE -> FINISHED`.

## Part 5 — Battle HUD/input

- `client/hud/BattleHudOverlay.java`
- `client/input/RoyalArenaKeyMappings.java`
- `client/ClientForgeEvents.java`
- `client/ClientModEvents.java`

Battle UI stays an overlay; it does not keep a `Screen` open during the match.
The strategic camera releases the OS cursor and projects cursor coordinates onto
the arena floor, so cards can be clicked and deployed at arbitrary visible
positions.

## Part 6 — Dummy troop deployment

- `card/BattleCard.java`
- `network/c2s/C2SDeployCardPacket.java`
- `server/battle/ServerTroop.java`
- `server/match/ServerMatchSession.java`

The client sends slot + requested position. The server re-validates state, side,
deployment region and elixir.

## Part 7 — Deck + elixir

- `card/BattleDeck.java`
- `match/ElixirMeter.java`

Starter deck has 8 placeholder vanilla-mob cards, a 4-card hand and one next
card.

## Part 8 — Towers + win condition

- `tower/TowerKind.java`
- `tower/TowerId.java`
- `tower/TowerState.java`
- `match/MatchInstance.java`

Troops attack the lane Princess tower, then the corresponding King tower.
Destroying the King tower finishes the match.

## Part 9 — Matching Screen/Queue

- `client/screen/MatchingScreen.java`
- `client/state/ClientQueueState.java`
- `server/match/MatchQueue.java`
- `network/c2s/C2SJoinQueuePacket.java`
- `network/c2s/C2SLeaveQueuePacket.java`
- `network/s2c/S2CQueueStatusPacket.java`

Queue only matches players already connected to the same Forge server.

## Part 10 — Internet deployment

- `build.gradle`: dedicated `server` run configuration.
- `docs/ONLINE.md`: deployment model and scope.

No external matchmaking backend/database is introduced in this MVP.

## Part 11 — Combat entity foundation

- `entity/ArenaEntityTypes.java`
- `entity/building/TowerHitboxEntity.java`
- `server/battle/ServerTroop.java`
- `server/match/ServerMatchSession.java`

Each logical tower now has an ephemeral invisible `royalarena:tower_hitbox`
entity with a real bounding box. `TowerState` remains authoritative for HP/win
state; the hitbox is only the physical world target.

A deployed troop locks one lane tower instead of re-selecting every tick from
its current X position. When that locked Princess tower is destroyed, the troop
retargets the enemy King tower. Troop-vs-troop targeting is intentionally
deferred to the next combat part.

A scoped `Mob.isSunBurnTick()` mixin disables only vanilla daylight burning for
entities tagged `royalarena_troop`. Ordinary mobs are unchanged, and normal
fire/lava damage remains available for later combat mechanics.

## Part 12 — Tower model entities + renderers

- `entity/building/TowerVisualType.java`
- `entity/building/CrownTowerEntity.java`
- `entity/building/KingTowerEntity.java`
- `client/model/CrownTowerModel.java`
- `client/model/KingTowerModel.java`
- `client/render/entity/CrownTowerRenderer.java`
- `client/render/entity/KingTowerRenderer.java`

Princess towers now spawn the supplied 3x4-block Crown Tower model and King
towers spawn the supplied 4x4-block King Tower model. These world entities
remain physical/rendering representatives only; `TowerState` still owns HP and
match victory. The previous wool/gold tower pillars are no longer built,
preventing overlap with the new entity models.

The editable Blockbench sources are retained under `model_sources/towers/`, and
both generated entity models use the shared
`assets/royalarena/textures/entity/tower/tower_blocks.png` atlas.

## Part 13 — Even arena + practice match

- `arena/ArenaLayout.java`, `arena/ArenaWaypoint.java`
- `server/arena/ArenaBuilder.java`
- `entity/building/TowerVisualType.java`
- `client/render/entity/CrownTowerRenderer.java`
- `server/match/MatchManager.java`, `server/match/ServerMatchSession.java`
- `network/c2s/C2SStartPracticePacket.java`
- `client/screen/MatchingScreen.java`, `command/RoyalArenaCommand.java`

The arena is now an even 18x32 grid with a water river, 2-wide bridges in
line with 4x4 Princess towers, and an 8-row gap between river and towers
(see `docs/ARENA.md`). Troops route over the bridge on their side.
`Practice Match` starts a solo match against an empty RED side. The network
protocol version is bumped to 2.

## Part 14 — Battle UI in reference units

- `client/hud/HudLayout.java`, `client/hud/BattleHudOverlay.java`
- `client/camera/ArenaCameraProfile.java`
- `client/render/WorldHealthBar.java`, tower renderers,
  `client/ClientForgeEvents.java`
- `tower/TowerKind.java` (max HP per tower kind)

HUD rebuilt with the original's proportions (`u = height / 356`), camera
tilted (see Part 17 for the current angle), and simple health bars for towers and damaged
troops. See `docs/UI.md`.

## Part 15 — Card hover, deploy preview, 1 s deploy time

- `client/hud/HudLayout.java`, `client/hud/BattleHudOverlay.java`
- `client/render/DeployPreviewRenderer.java`, `client/ClientForgeEvents.java`
- `client/state/ClientMatchState.java`
- `match/PendingDeploy.java`, `match/MatchInstance.java`,
  `match/MatchSnapshot.java`, `network/s2c/S2CMatchSnapshotPacket.java`
- `server/match/ServerMatchSession.java`

Hovered/selected cards rise; the cursor tile shows a green ghost or a red
tint; troops spawn one second after an accepted click. Network protocol 3.

## Part 16 — Troops fight, towers shoot

- `combat/CombatEngine.java`, `CombatUnit.java`, `TickResult.java`,
  `ChildSpawn.java`, `Formation.java`
- `card/TroopStats.java`, `SplitRule.java`, `CostFormula.java`,
  `BattleCard.java`, `BattleDeck.java`
- `tower/TowerKind.java`, `TowerState.java`, `arena/ArenaLayout.java`
- `server/battle/BattleField.java` (replaces `ServerTroop`),
  `server/match/ServerMatchSession.java`
- `docs/COMBAT.md`

Troop-vs-troop combat, tower fire with a sleeping King, slime splitting and
multi-unit cards on the vanilla health scale (towers 110/185 HP). The deck is
now the five designed troops plus Stray, Vindicator and Iron Golem as fillers.



## Part 17 — Troops move, camera fix

- `server/battle/BattleField.java` (moves mobs with `Entity.move`),
  `combat/Separation.java`, `arena/ArenaLayout.java` (bridge routing)
- `client/camera/ArenaCameraProfile.java`

NoAI troop mobs ignored the velocity they were given and stood still; they are
now moved explicitly. The camera no longer swings sideways: it looks straight
down the arena at 50 degrees and the whole arena fits above the HUD panel.

## Part 18 — Solo match against a bot

- `bot/BotBrain.java`
- `server/match/ServerMatchSession.java`, `server/match/MatchManager.java`
- `client/screen/MatchingScreen.java`, `command/RoyalArenaCommand.java`
- `docs/BOT.md`

Practice matches now have a bot playing RED through the normal deploy rules.
`/royalarena bot` (alias `practice`) and the **Play vs Bot** button start one.

## Part 19 — Config files, steeper camera, movement debugging

- `config/ArenaClientConfig.java` (`royalarena-client.toml`: camera)
- `config/ArenaCommonConfig.java` (`royalarena-common.toml`: troop speed, bot,
  debug)
- `bot/BotSettings.java`, `client/camera/ArenaCameraProfile.java`
- `docs/CONFIG.md`

The camera is now almost top-down (pitch 68, distance 30). Camera, troop speed,
bot pace and two debug aids (route particles, movement log) are configurable
and reload when the file is saved.

## Part 20 — Spells: Arrow Storm and Fireball

- `card/SpellStats.java`, `card/BattleCard.java` (`ARROW_STORM`, `FIREBALL`),
  `card/CostFormula.java` (spell cost)
- `combat/CombatEngine.java` (`castSpell`, pulses, knockback, burn),
  `combat/TickResult.java` (`SpellPulse`, `Push`)
- `arena/ArenaLayout.java` (`canPlay`: spells anywhere on the arena)
- `bot/BotBrain.java` (casts a spell on a group worth it)
- `server/battle/BattleField.java` (particles and sounds),
  `client/render/DeployPreviewRenderer.java` (spell circle)
- `SpellSmokeTest`

The two spells replace Stray and Vindicator in the starter deck. See
`docs/CARDS.md` for the numbers.

## Part 21 — Card art template for Blockbench

- `model_sources/card_art/royalarena_card_art.js` (Blockbench plugin)
- `model_sources/card_art/README.md`

Renders the open model as a 31:39 card portrait with the same framing and
background on every card, previews it with the in-game card edge and cost
badge, and saves `<card id>.png`. `DomainSmokeTest` checks that the plugin's
card list matches `BattleCard`.

## Part 22 — Deck editor, card library, bot decks

- `card/DeckRules.java`, `card/CardRole.java`, `bot/BotDeck.java`,
  `card/BattleDeck.java` (`shuffled`, `cards`)
- `server/deck/DeckStorage.java` (saved per player with the world)
- `network/c2s/C2SDeckRequestPacket.java`, `C2SSaveDeckPacket.java`,
  `network/s2c/S2CDeckPacket.java` (protocol 4)
- `client/screen/DeckScreen.java`, `client/state/ClientDeckState.java`,
  `client/hud/CardFace.java` (card drawing shared with the HUD)
- `server/match/MatchManager.java` (saved decks, shuffled; random bot deck)
- `DeckSmokeTest`

## Part 23 — Empty arena dimension, Void test worlds

- `data/royalarena/dimension/arena.json`, `server/arena/ArenaDimension.java`
- `server/arena/ArenaAllocator.java`, `server/match/MatchManager.java`
- `event/CommonForgeEvents.java` (logins left in the arena dimension)
- `build.gradle` (`sumodlib.devWorld`), `run-server/server.properties`

Needs sumodlib with `VoidWorlds` (the `sumodlib:void` dimension type and
world preset).

## Part 24 — Arrows you can see, damage on arrival

- `combat/CombatEngine.java` (shots in flight, `flightTicks`),
  `combat/TickResult.java` (`Attack.flightTicks`, `landed`)
- `network/s2c/S2CProjectilePacket.java` (protocol 5),
  `network/ModNetwork.java` (`sendNear`)
- `client/render/ProjectileRenderer.java` (3D arrows and fireballs)
- `server/battle/BattleField.java` (launch on fire, effects on landing,
  Arrow Storm volleys, Fireball flight)

## Part 25 — Troop animations

- `server/battle/TroopAnimator.java`, `server/battle/BattleField.java`

Attack poses, the archers' bow draw, the Iron Golem swing and toss, slime
hops and squish, spider pounce; troops turn toward what they fight. See
`docs/COMBAT.md`.

## Part 26 — Spell targets glow, ghost formations, smaller swarms, new cards

- `client/render/SpellTargetHighlighter.java`, `client/render/TroopVisuals.java`
- `client/render/DeployPreviewRenderer.java` (one ghost per unit)
- `card/TroopTraits.java`, `card/BattleCard.java` (Creeper, Witch, Wolves,
  Ravager, `modelScale`), `card/CostFormula.java`, `card/CardRole.java`
- `combat/CombatEngine.java` (splash, self-destruct, melee knockback)
- `server/battle/BattleField.java`, `server/battle/TroopAnimator.java`
- `TraitSmokeTest`

## Part 27 — AoE flashes, ember burn, the bot's hand

- `network/s2c/S2CAoeFlashPacket.java`, `S2CBurnPacket.java`,
  `S2CBotDragPacket.java` (protocol 6)
- `client/render/AoeFlashRenderer.java`, `BurnEffects.java`,
  `BotDragRenderer.java`, `WorldShapes.java`
- `server/battle/BattleField.java`, `server/match/ServerMatchSession.java`,
  `bot/BotBrain.java` (`DRAG_TICKS`)

## Part 28 — Rebalance for tanks, tower-only troops, Hoglin, Pillager, Magma Cube

- `card/TroopTraits.java` (`buildingsOnly`), `card/CostFormula.java`
  (x0.85), `card/CardRole.java` (`BUILDINGS`), `card/BattleCard.java`
- `combat/CombatEngine.java` (tower-only troops ignore troops),
  `tower/TowerKind.java` (Princess 3 damage, range 7.0)
- `bot/BotBrain.java` (no tower-only defenders),
  `server/battle/TroopAnimator.java`, `server/battle/BattleField.java`
  (Hoglin swing, Pillager crossbow)
- `client/screen/DeckScreen.java` (tooltip line)
- `CombatSmokeTest.tanksReachTheTowerHealthy`, `TraitSmokeTest`

See `docs/CARDS.md` (Rebalance) for the before/after numbers.

## Part 29 — Scenery, the King on its tower, weapons, four new spells

- `arena/ArenaScenery.java`, `server/arena/ArenaBuilder.java` (hills and
  forest), `mixin/MobDespawnMixin.java` (no despawn on peaceful)
- `client/render/entity/KingFigure.java`, `KingTowerRenderer.java`,
  `client/render/WorldHealthBar.java`, `match/MatchSnapshot.java`
  (players' ids, protocol 7), `tower/TowerKind.java` (`shotHeight`)
- `card/SpellStats.java` (`maxTargets`, `stunTicks`, `freezeTicks`, `heal`),
  `card/CostFormula.java`, `card/BattleCard.java` (Lightning, Potion of
  Healing, Freeze, TNT; axe, sword and crossbow timings)
- `combat/CombatEngine.java`, `combat/CombatUnit.java`,
  `combat/TickResult.java` (`Strike`), `bot/BotBrain.java`
- `server/battle/BattleField.java`, `client/render/ProjectileRenderer.java`,
  `AoeFlashRenderer.java`, `SpellTargetHighlighter.java`,
  `network/s2c/S2CAoeFlashPacket.java` (colour), `S2CProjectilePacket.java`
- `SpellSmokeTest` (Lightning, healing, Freeze, TNT)

## Part 30 — Hurt flashes, the King wakes up

- `server/battle/BattleField.java` (`flashIfHurt`; the old entity event 2
  did nothing in 1.20.1)
- `client/state/ClientMatchState.java` (`ticksSinceTowerHurt`, `kingAwake`),
  `client/render/WorldHealthBar.java` (`towerOverlay`),
  `client/render/entity/KingFigure.java`, `CrownTowerRenderer.java`,
  `KingTowerRenderer.java`

## Part 31 — Battle effects, double elixir and overtime

- `client/render/DamageNumbers.java`, `TroopEffects.java`,
  `client/camera/CameraShake.java`, `client/hud/CrownFlights.java`,
  `client/hud/BattleHudOverlay.java`, `config/ArenaClientConfig.java`
  (`[effects]`)
- `network/s2c/S2CShakePacket.java` (protocol 8),
  `server/battle/BattleField.java` (shakes, death bits),
  `server/match/ServerMatchSession.java`, `server/arena/ArenaBuilder.java`
  (rubble)
- `match/ElixirMeter.java`, `match/MatchInstance.java` (x2 / x3 elixir,
  overtime), `match/MatchSnapshot.java`
- `model_scale` in the troop card JSONs
- `DomainSmokeTest.elixirSpeedsUpAndOvertimeIsSuddenDeath`

## Part 32 — Power band and the balance report

- `card/CostFormula.java` (range per block, knockback, slow),
  `card/TroopTraits.java` (`slowTicks`), `combat/CombatEngine.java`,
  `combat/CombatUnit.java`, `combat/TickResult.java` (slow on hit)
- Ravager 110 HP / 8 damage, Pillager 8 damage, Stray slows 1 s
- `src/test/.../Sim.java`, `PowerCreepTest.java` (in `check`),
  `BalanceReport.java` (`gradlew balanceReport`), `docs/BALANCE_REPORT.md`

## Part 33 — Phone stats page

- `docs/stats/index.html` (static page: cards, compare, matchups, matches),
  `docs/stats/README.md`, `docs/stats/data/` (generated)
- `src/test/.../StatsReport.java` (`gradlew statsReport`),
  `StatsServer.java` (`gradlew statsServe`), `Sim.solo`,
  `BalanceReport.play`

## Part 34 — Match log

- `match/MatchRecord.java` (plays, elixir over time, end reason),
  `match/MatchInstance.java` (`finish(winner, reason)`)
- `combat/CombatStats.java` (damage, tower damage, kills, healing per card),
  `combat/CombatEngine.java`, `combat/CombatUnit.java` (burn source)
- `stats/MatchLog.java` (one JSON line per match), `stats/Json.java`
- `server/stats/MatchLogWriter.java`, `server/match/ServerMatchSession.java`,
  `config/ArenaCommonConfig.java` (`[stats] recordMatches`)
- `MatchLogSmokeTest`, `MiniJson` (tests); see `docs/MATCH_LOG.md`

## Part 35 — Seven mobs with their own rules

- `card/TroopTraits.java` (poison, teleport, hover, crosses river, deploy on
  river, frost and water weakness), `card/CostFormula.java`,
  `card/BattleCard.java` (the seven cards), `card/*` JSON files
- `combat/CombatEngine.java` (teleport and dodge, poison, river and frost
  rules, Blaze route), `combat/CombatUnit.java`, `combat/TickResult.java`
  (`Teleport`)
- `arena/ArenaLayout.java` (`canPlay`: Drowned on the river)
- `server/battle/BattleField.java` (hover, teleport, poison drips, fireballs,
  trident), `server/battle/TroopAnimator.java` (trident draw)
- `client/screen/DeckScreen.java` (rules in the tooltip)
- `NewMobsSmokeTest` (teleport, poison, Blaze, Drowned, Vex), CombatSmokeTest
  (the JSON trait keys)
- `docs/CARDS.md` (table and open points), `docs/COMBAT.md` (rules)

## Part 36 — Every card in 40-60% of bot matches

- `card/BattleCard.java` and `card/*` JSON files: Ghast, Ravager, Freeze,
  Enderman, Cave Spider, Creeper; `model_sources/card_art` (Freeze cost)
- `card/CostFormula.java` (poison counts only until the next hit replaces it),
  `card/SpellStats.java` (a second of freeze or stun is worth 2)
- `bot/BotBrain.java` (freeze/stun only on fighting enemies, worth their
  damage per second; Freeze on a tower that shoots own troops)
- SpellSmokeTest, NewMobsSmokeTest (new numbers)
- `docs/CARDS.md` ("Tuning into 40-60%"), `docs/BALANCE.md`, `docs/BOT.md`,
  `docs/BALANCE_REPORT.md`

## Part 37 — Deck library filters and the second wave of mobs

- `card/CardTactic.java` (Attack, Defense, Support; `BattleCard.tactics()`,
  `"roles"` in every card JSON), `card/CardFilter.java` (tabs, chips, sort,
  16-card pages), `client/screen/DeckScreen.java` (the buttons, paging, mouse
  wheel, tactics and new rules in the tooltip)
- `card/BattleCard.java`, `card/*` JSON, `model_sources/card_art`: Husk,
  Wither Skeleton, Piglin Brute, Polar Bear, Phantom, Silverfish, Bee, Evoker,
  Guardian; Pillager range 6.0, Cave Spider bite 2
- `card/TroopTraits.java` (frost-proof, line, lock-on, river strength;
  `stinging`, `swooping`, `laser`), `card/CostFormula.java` (their modifiers,
  full poison for self-destruct), `card/CardRole.java` (line is area)
- `combat/CombatEngine.java` (line hits, lock-on, river strength,
  frost-proof), `combat/CombatUnit.java`, `combat/TickResult.java`
  (`Attack.line`, `lineLength`, `fromX`, `fromZ`)
- `server/battle/BattleField.java` (fangs, laser beam, bee sting, wither
  dust), `network/ModNetwork.java` (protocol 9)
- `MobsWave2SmokeTest`, CombatSmokeTest (roles and the new JSON keys)
- `docs/CARDS.md`, `docs/COMBAT.md`, `docs/BALANCE.md`, `docs/UI.md`

## Part 38 — Result screen

- `match/MatchResult.java` (winner, crowns, end reason, length, MVP per
  side), `match/ResultTimeline.java` (animation order, sizes and sound cues)
- `network/s2c/S2CMatchResultPacket.java`,
  `network/c2s/C2SResultDonePacket.java`, `network/ModNetwork.java`
  (protocol 10; also fixes `S2CBotDragPacket` sharing an id with
  `S2CShakePacket`)
- `client/screen/ResultScreen.java`, `client/hud/BattleHudOverlay.java`
  (hidden under it), `textures/gui/result/` (ribbons, rays)
- `server/match/ServerMatchSession.java` (sends the result, fireworks over
  the winner, each player goes home on OK or after 15 s),
  `server/match/MatchManager.java` (`resultDone`, Play again)
- `ResultScreenSmokeTest`; `docs/UI.md` (Result screen)

## Part 39 — Autopilot: Snow Golem

- `card/BattleCard.java`, `cards/snow_golem.json`, card art: Snow Golem;
  Skeleton Archer and Ghast range 5.5, Ghast HP 28, Blaze range 6.0
- `combat/CombatEngine.java` (slowing hits do double to frost-weak units),
  `client/screen/DeckScreen.java` (tooltip), `server/battle/BattleField.java`
  (snowballs), `network/ModNetwork.java` (protocol 11)
- `MobsWave2SmokeTest.snowballsAreFrost`; `docs/CARDS.md`, `docs/COMBAT.md`

## Part 40 — Nether mobs do not burn; the next cards

- `card/BattleCard.java` (`fireImmune()`), the seven Nether card JSON files
  (`fire_immune`), `combat/CombatEngine.java` (no ignite), tooltip
- `MobsWave2SmokeTest.netherMobsDoNotBurn`, CombatSmokeTest (JSON flag)
- `docs/CARD_QUEUE.md`: the user's designs (Zombie Villager, Spider Jockey,
  Fox, Skeleton Rider, Strider, Ender Pearl, Warden, Shulker) and Claude's
  (Goat, Llama, Piglin, Allay, three potions); autopilot cap 55 cards

## Part 41 — Poison by body

- `card/Body.java` (living 1.5x, undead 1x, construct immune),
  `BattleCard.body()`, `"body"` in every troop JSON, tooltip
- `combat/CombatEngine.java` (`bodyOf`: no poison on constructs, 1.5x on the
  living)
- `MobsWave2SmokeTest.poisonDependsOnTheBody`, CombatSmokeTest (JSON)
- `docs/CARDS.md`, `docs/COMBAT.md`

## Part 42 — Mob sounds

- `card/MobSounds.java` (hurt, death, attack event per mob),
  `server/battle/BattleField.java` (`mobSound`: hurt with the flash, death,
  melee attack)
- `MobSoundsSmokeTest`, `src/test/resources/vanilla_sound_events_1_20_1.txt`
- `docs/COMBAT.md` (Mob sounds)

## Part 43 — Match and card sounds

- `match/ArenaSounds.java` (every chosen sound id), `match/SoundCues.java`
  (which match cue fires between two client frames)
- `client/sound/ClientSounds.java`, `client/sound/BattleSounds.java` (match
  found, countdown, FIGHT, x2/x3, overtime, last 10 seconds, elixir full,
  Pigstep battle music), `ClientForgeEvents` (refused deploy),
  `CrownFlights` (crown lands), `DeckScreen` (card placed, deck saved or
  refused), `ArenaClientConfig.BATTLE_MUSIC`
- `server/battle/BattleField.java`: troop appears, tower and King hit, tower
  cracking, poison, frost, Blaze in the river, Vex charge, Guardian lock,
  Creeper hiss, Ravager roar, Slime and Magma Cube split
- `SoundCuesSmokeTest`; `docs/SOUND_DESIGN.md`, `docs/CONFIG.md`

## Part 44 — TNT reach, menu hint, tutorial switch

- `card/SpellStats.java` (`middleReach`), `card/BattleCard.java` (TNT),
  `arena/ArenaLayout.java` (`reachesMiddle`, `nearestReach`, `canPlay`),
  `bot/BotBrain.java` (aims at the nearest row TNT reaches),
  `client/screen/DeckScreen.java` (tooltip), `cards/tnt.json` (`reach`)
- `client/hud/BattleHudOverlay.java` (`drawMenuHint`),
  `ArenaClientConfig.MENU_HINT`, `lang/en_us.json`
- `SpellSmokeTest.tntReachesOnlyTheMiddleOfTheBridge`; TNT stays at 47% in
  the bot balance report (4000 matches), same as before
- `build.gradle` sets `sumodlib.noTutorial` on the dev client; the switch
  itself is `client/Tutorials.java` in sumodlib

## Part 45 — Spider Jockey

- `card/SplitRule.java` (`into`), `card/CostFormula.java` and
  `card/CardRole.java` (spider and rider health), `combat/CombatEngine.java`
  (one rider child), `card/BattleCard.java` (SPIDER_JOCKEY, Ghast 33/19),
  `cards/spider_jockey.json`, `model_sources/card_art` (id and cost),
  `server/battle/BattleField.java` (a Skeleton rider on the spider,
  `discardMob`), `client/screen/DeckScreen.java` (tooltip), protocol 12
- `MobsWave2SmokeTest.spiderJockeyLeavesItsArcher`; `docs/CARDS.md`,
  `docs/COMBAT.md`, `docs/CARD_QUEUE.md`

## Part 46 — Bee stings on, Big Slime 36 health

- `card/TroopTraits.java` (`stinging` no longer self-destructs),
  `card/BattleCard.java` (Bee 4/1, poison 1; Big Slime 36/5, speed 0.75,
  4 elixir), `bee.json`, `big_slime.json`, card art cost,
  `server/battle/BattleField.java` (an angry Bee face on its sting)
- `MobsWave2SmokeTest.beeStingsAgainAndPoisons`, `DeckSmokeTest` (Big Slime
  health 108, starter deck average 26/8); `docs/CARDS.md`, `docs/COMBAT.md`

## Part 47 — Range and body tuning, smarter healing

- `card/BattleCard.java` and the JSON files: Ghast and Skeleton Archer range
  5.0, Husk 3 x 13 HP (slow 2 s, speed 1.0), Hoglin 50/10, Enderman 68 HP
  (teleport every 5 s), Evoker range 4.5
- `bot/BotBrain.java` (`fighting`, `HEAL_BELOW`): the healing potion only goes
  on fighting troops at 70% health or less
- Tests: `SpellSmokeTest` (Hoglin 50 - 40), `NewMobsSmokeTest` (Enderman
  health from the card), `MobsWave2SmokeTest` (rider range 5.0)

## Part 48 — Bot personalities

- `bot/BotPersonality.java` (Tank Push, Rush, Counter, Spell Siege; drawn
  from the seed), `bot/BotBrain.java` (attack threshold per personality,
  tank push and counter-push follow-ups, Rush pairs, counter-scored
  defenders, lane switch and split, kite, punishing low opponent elixir, no
  spell on a sleeping King, siege spells)
- `config/ArenaCommonConfig.java` (`personality`),
  `server/match/MatchManager.java` (config, chat notice),
  `stats/MatchLog.java` and `ServerMatchSession` (`"personality"`), lang
- `BotPersonalitySmokeTest`; `BotSmokeTest` (the counter score, a neutral
  personality for the settings test); `BalanceReport` prints each
  personality's win rate; `docs/BOT.md`, `docs/CONFIG.md`, `docs/MATCH_LOG.md`

## Part 49 — Watch bot against bot

- `server/match/ServerMatchSession.java` (a list of bots, each with its own
  card drag; a spectator in BLUE's seat whose deploys are ignored; result
  names "Bot (Rush)"), `MatchManager.startWatch` (and "Play again" for it),
  `command/RoyalArenaCommand.java` (`/royalarena watch`),
  `network/c2s/C2SStartPracticePacket.java` (`watch`),
  `match/MatchSnapshot.java` and `S2CMatchSnapshotPacket` (`spectating`),
  protocol 13
- `client/screen/MatchingScreen.java` (Watch Bot vs Bot),
  `client/state/ClientMatchState.java` (`spectating`: no selection or
  deploy), `ClientForgeEvents` (no clicks), `BattleHudOverlay` (WATCHING),
  `BotDragRenderer` (a blue hand for the BLUE bot), lang
- `docs/BOT.md`

## Part 50 — Watching: both hands, side-on camera, x2

- `match/MatchSnapshot.java` (`Watch`: RED's hand, next card and elixir, both
  personalities, speed), `S2CMatchSnapshotPacket`, protocol 14
- `client/hud/SpectatorHud.java` (RED panel left, BLUE right),
  `BattleHudOverlay` (spectator layout, "WATCHING x1 [X]"),
  `client/camera/ArenaCameraProfile.watch` (high, side-on, yaw -90),
  `ArenaCameraController`, `client/state/ClientMatchState.java` (both sides)
- `client/render/BotDragRenderer.java` (ring and card name at the landing
  spot), `client/input/RoyalArenaKeyMappings.WATCH_SPEED` (X),
  `network/c2s/C2SWatchSpeedPacket.java`, `MatchManager.toggleWatchSpeed`,
  `ServerMatchSession` (`speed`, two ticks at x2)
- `DomainSmokeTest.watchViewFitsBetweenThePanels`; `docs/BOT.md`

## Part 51 — Readable numbers, closer cameras, watch setup

- `client/render/WorldHealthBar.java` (bars 1.3-1.45x bigger, a black frame
  2 px on towers and 1 px on troops, outlined "health/max"; `outlined`),
  `client/render/DamageNumbers.java` (1.5x, outlined),
  `ArenaClientConfig.WORLD_TEXT_SCALE` (`worldTextScale`)
- `client/camera/ArenaCameraProfile.java`: player camera 30 -> 26 blocks
  (look-ahead 5 -> 6.5), watch camera 32 -> 28 with 17% side panels
- `match/WatchSetup.java`, `client/screen/WatchSetupScreen.java`,
  `C2SStartPracticePacket` (the setup), `MatchManager.startWatch(player,
  setup)` (decks: random, the spectator's own, starter; personalities),
  protocol 15
- `BotPersonalitySmokeTest.watchSetupKeepsValidChoices`,
  `DomainSmokeTest` (camera framing at the new distances); `docs/BOT.md`,
  `docs/CONFIG.md`

## Part 52 — Creeper up, Fox, Skeleton Archer range

- `card/BattleCard.java` and JSON: Creeper 23 HP, speed 1.8, blast 3.0; FOX
  (3 x 6 HP, 2 damage every 0.7 s, speed 1.9, 2 elixir); Skeleton Archer
  range 4.5; card art cost; protocol 16
- `MobsWave2SmokeTest.foxesAreAFastCheapPack`, the rider test follows the
  archer's range; `docs/CARDS.md`, `docs/BALANCE.md`, `docs/CARD_QUEUE.md`

## Part 53 — Spell buffs and four new spells

- `card/SpellStats.java` (`slowTicks`, `poison`, `hasteTicks`, `friendly()`,
  slow and haste in `controlValue`), `combat/CombatEngine.java` (friendly
  targets, poison by body and never on towers, webs, `HASTE_SPEED`,
  `WEB_SPEED`), `combat/CombatUnit.java` (`web`, `haste`)
- `card/BattleCard.java` and JSON: free buffs (flight time, tower share,
  burn) for Arrow Storm, Fireball, Lightning, Healing, Freeze, TNT; new
  POISON_CLOUD, COBWEB, SWIFTNESS_POTION, SNOWBALL_VOLLEY; protocol 17
- `bot/BotBrain.java` (values of poison, webs and haste; no-damage spells
  wait for 1.5x the value), `client/render/SpellTargetHighlighter.java`,
  `client/screen/DeckScreen.java` (tooltip), `server/battle/BattleField.java`
  (thrown potions, webs and snowballs; clouds, web bits, swiftness puffs),
  `S2CAoeFlashPacket` (poison, web and swiftness colours)
- `SpellSmokeTest` (four new tests; flight, burn and tower numbers follow the
  buffs), `CombatSmokeTest` (spell JSON keys), `MatchLogSmokeTest` (burn)

## Part 54 — Skeleton Rider (autopilot cycle 5)

- `card/BattleCard.java` and `skeleton_rider.json` (26 HP, 8 damage a
  second, range 5.5, speed 1.4, towers only, undead, 3 elixir), card art
  cost, `server/battle/BattleField.java` (a Skeleton rides the horse, as on
  the Spider Jockey), protocol 18; Wither Skeleton speed 0.85
- `MobsWave2SmokeTest.skeletonRiderIgnoresTroops`

## Part 55 — Vanilla health and armour

- `card/TroopStats.java` (`armor`, `armorFactor`, `effectiveHealth`),
  `card/CostFormula.java` (effective health, `traitModifier`),
  `combat/CombatEngine.java` (armour on hits and spell hits, not on poison
  or fire), `card/CardRole.java` and `bot/BotBrain.java` (count armour),
  `card/BattleCard.java` and every troop JSON (`"armor"`; vanilla health;
  `armorItems`), `server/battle/BattleField.java` (worn armour),
  `client/screen/DeckScreen.java` (tooltip), card art costs, protocol 19
- `scripts/card-library/config.py`, `config-template.html`, `vanilla.json`:
  the card config page; `docs/CARDS.md`, `docs/COMBAT.md`, `docs/BALANCE.md`
- tests follow the new health and armour (numbers from the code)

## Part 56 — Tribes

- `card/Tribe.java`, `card/TribeBonus.java`, `BattleCard.tribes()`;
  `combat/CombatEngine.java` and `combat/CombatUnit.java` (tribe health,
  damage, speed, attack rate, armour, piercing, healing, lifesteal, burn and
  poison hits, first-hit freeze, tower and river damage; `setTribes`);
  `bot/BotDeck.java` (`around`, `plain`; half the bot decks are built
  around a tribe); `config/ArenaCommonConfig.java` (`tribes`);
  `server/battle/BattleField.java`; `client/screen/DeckScreen.java` (tribe
  line and tooltip)
- `TribeSmokeTest`, `TribeReport` and `scripts/tribes.sh`; `BalanceReport`
  measures cards on plain decks; card pages show tribes;
  `docs/TRIBES.md`, `docs/CARDS.md`, `docs/COMBAT.md`, `docs/BALANCE.md`

## Part 57 — Tribes in five steps, with special rules

- `card/Tribe.java`: steps at 2, 3, 4, 5 and 6 cards; step 5 is each
  tribe's special rule: Zombie rise again, Skeleton volley, Illager totem
  of undying, Nether last flame, Arthropod silk, Beast pack, Night shadow
- `card/TribeBonus.java` (the new rules, side cooldowns),
  `combat/CombatEngine.java` and `combat/CombatUnit.java` (saving a
  troop once per side cooldown, volleys, pack damage, last flame, dodges,
  slowing hits), `combat/TickResult.java` (`TribeEvent`),
  `server/battle/BattleField.java` (totem, soul, flame and smoke effects),
  `bot/BotDeck.java` (3 to 6 tribe cards)
- `TribeSmokeTest` (every rule), `TribeReport` (2 to 6 cards),
  `docs/TRIBES.md`, the tribes page

## Part 58 — Strider (autopilot cycle 6)

- `card/TroopTraits.java` (`fireTrailTicks`), `card/CostFormula.java`
  (fire trail x1.15), `combat/CombatEngine.java` (burning patches,
  `firePatches`), `card/BattleCard.java` and `strider.json` (20 HP, 3
  damage a second, speed 1.7, fire immune, Nether and Beast, 2 elixir),
  card art cost, `server/battle/BattleField.java` (flames),
  `client/screen/DeckScreen.java` (tooltip), protocol 20; Evoker speed 0.6,
  Ghast speed 0.3, Snowball Volley flies 0.4 s
- `MobsWave2SmokeTest.striderLeavesAFireTrail`, `"fire_trail_ticks"` in
  `CombatSmokeTest`

## Part 59 — Goat (autopilot cycle 7)

- `card/BattleCard.java` and `goat.json` (10 HP, 5 damage every 2 s,
  knockback 3.0, speed 1.3, Beast tribe, defence, 1 elixir), card art
  cost, protocol 21; Piglin Brute speed 0.9
- `TraitSmokeTest.goatRamsFarther`

## Part 60 — Llama (autopilot cycle 8)

- `card/BattleCard.java` and `llama.json` (22 HP, 3 damage every 1.5 s,
  range 4.5, slows 0.75 s, speed 0.9, Beast tribe, 2 elixir), card art
  cost, protocol 22; Evoker speed 0.5, Polar Bear speed 1.2
- `MobsWave2SmokeTest.llamaSpitsAndSlowsALittle`

## Part 61 — Piglin (autopilot cycle 9)

- `card/TroopTraits.java` (`burst`), `card/CostFormula.java` (shots per
  interval), `combat/CombatUnit.java` (`startShotCooldown`),
  `card/BattleCard.java` and `piglin.json` (16 HP, 2 bolts of 5 every 3 s,
  range 5, fire immune, Nether tribe, 2 elixir), card art cost,
  `client/screen/DeckScreen.java` (tooltip), protocol 23; Piglin Brute
  speed 0.8
- `MobsWave2SmokeTest.piglinFiresTwoThenReloads`, `"burst"` in
  `CombatSmokeTest`

## Part 62 — Arrow Storm rework, spells thrown from the King

- `card/SpellStats.java` (`flightSpeed`, `thrownAt`, `flightTicks`),
  `card/BattleCard.java` and the spell JSONs (`"flight_speed"`): Arrow Storm
  3 volleys of 7 every 0.4 s, radius 2.3 (was 16 over 4 s, radius 3)
- `arena/ArenaLayout.deployTicks` (flight from the King by distance),
  `match/MatchInstance.java`, `match/PendingDeploy.java` (`totalTicks`),
  `S2CMatchSnapshotPacket`, `ClientMatchState`, protocol 24
- `bot/BotBrain.java` (spell lead by flight), `client/screen/DeckScreen.java`
  (tooltip), `server/battle/BattleField.java` (arrows from the King)
- `SpellSmokeTest.arrowStormFiresThreeVolleys`, flight checks in
  `SpellSmokeTest`, `"flight_speed"` in `CombatSmokeTest`

## Part 63 — Zap

- `card/BattleCard.java` and `zap.json` (2 elixir, radius 2.5, 6 damage,
  0.5 s shock that restarts the attack, no target limit), `BattleField`
  (sparks, flash, thunder), card art plugin list, protocol 25
- `SpellSmokeTest.zapShocksEveryoneInTheCircle`

## Part 64 — Iron Golem rework, white spell targets, "!" markers

- `card/BattleCard.java` and `iron_golem.json`: fights every target,
  armour 4, 15 damage every 2.75 s (was 14 every 1.6 s, towers only);
  tests that used it as the tower-only troop now use the Hoglin or count its
  armour
- `client/render/SpellTargetHighlighter.java` (white pulse instead of the
  glowing outline), `mixin/LivingEntityRendererMixin.java` (client mixin on
  `getOverlayCoords`), `WorldHealthBar.towerOverlay`
- `client/render/AggroMarkers.java` ("!" over enemies the ghosts would
  draw), `DeployPreviewRenderer.spots`, `ClientForgeEvents`

## Part 65 — Sandbox

- docs/SANDBOX.md: `/royalarena sandbox` and a matching-screen button; a
  panel to play any card for either side (Tab switches, C picks), clear
  troops, restore towers, unbreakable towers, endless elixir, pause /
  0.5x / 1x / 2x, tribe steps per side, and a range / sight / target overlay
- `match/SandboxSettings.java`, `MatchInstance` (sandbox deploys, no clock,
  no end, tower reset), `CombatEngine` (tribe steps, `kingAwake`),
  `MatchManager`, `ServerMatchSession`, `C2SSandboxPacket`,
  `MatchSnapshot.Sandbox`, protocol 26
- client: `ClientSandbox`, `SandboxHud`, `SandboxCardScreen`,
  `SandboxTribeScreen`, `SandboxStatsRenderer`, `WorldShapes.line`, keys
- `SandboxSmokeTest`
