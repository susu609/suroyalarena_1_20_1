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

- `model_sources/card_art/card_art_renderer.js` (Blockbench plugin)
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
