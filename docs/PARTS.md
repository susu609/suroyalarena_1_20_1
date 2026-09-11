# Royal Arena Parts 0–10

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
