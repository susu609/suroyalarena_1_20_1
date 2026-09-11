# Su Royal Arena MVP Design

## Goal

Build the approved 1v1 Clash-Royale-inspired foundation on Forge 1.20.1:
mirrored team cameras, server-authoritative match lifecycle, four-card
HUD/input, validated deployment, elixir/deck cycle, logical towers/win
condition, and an in-server matching queue.

## Constraints

- Minecraft Java 1.20.1, Forge 47.4.23, official 1.20.1 mappings, Java 17
  toolchain.
- Package `net.susu609.royalarena`, mod id `royalarena`.
- Keep `sumodlib` as the existing Maven Local dependency; do not copy library
  code into this mod.
- Client owns camera, HUD, key/mouse input and screens only.
- Server owns team assignment, match lifecycle, deploy validation, elixir, deck
  order, troop simulation, tower HP and winner.
- No database/web API/global matchmaking in MVP. Queue matches players already
  connected to one Forge server.
- Do not keep a battle `Screen` permanently open; battle HUD is an overlay and
  the fixed strategic camera releases the cursor so the client can project
  cursor position onto the arena floor before sending a server-validated deploy
  request.

## Part boundaries

0. Clean template identity/resources.
1. Pure match domain (`TeamSide`, `MatchState`, `MatchInstance`).
2. Pure arena coordinate/layout model.
3. Mirrored client camera derived from arena + side.
4. Server session lifecycle and snapshot sync.
5. Battle HUD, key/card selection, free-cursor projection onto the arena floor,
   and left-click deployment input.
6. Server-validated dummy card deployment using vanilla entities.
7. Eight-card cycle + deterministic 0..10 elixir meter.
8. Logical princess/king towers, troop movement/attack, king-tower win.
9. Matching screen + queue for players already online on the same server.
10. Dedicated-server run configuration and deployment notes; no external backend
    code.

## MVP arena

The long axis is Z. BLUE owns positive Z and looks north; RED owns negative Z
and looks south. The standard layout is 19 blocks wide and 31 blocks long, with
river at Z=-1..1 and two bridges. Camera anchors are symmetric around the
center.

## Networking

One Forge `SimpleChannel` carries queue status, match snapshots and deploy
requests. Match snapshots are enough for camera/HUD because they include side,
arena center, lifecycle state, timer, elixir, hand/next card and tower HP.

## Testing

Pure domain classes are verified by `DomainSmokeTest` without Minecraft classes.
Forge-specific wiring is checked by static source validation in this
environment; full `gradlew check` is expected on the user's normal Forge
development machine because this execution environment has no Gradle
installation/network access.
