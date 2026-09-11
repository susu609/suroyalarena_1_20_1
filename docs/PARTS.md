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
