# Su Royal Arena — Minecraft Forge 1.20.1

Prototype 1v1 arena/card battle inspired by the pacing and lane-control idea of
Clash Royale, implemented as an independent Minecraft Forge mod.

## Environment

- Minecraft Java 1.20.1
- Forge 47.4.23
- Mojang official mappings 1.20.1
- Java 17
- Mod ID: `royalarena`
- Root package: `net.susu609.royalarena`
- Shared dependency: `net.susu609.modlib:sumodlib:1.0.0` from Maven Local

Before syncing this project, publish/install `sumodlib_1_20_1` version 1.0.0 to
Maven Local as required by the template workflow.

## MVP flow

1. Start one Forge server with Royal Arena + SuModLib.
2. Connect two players.
3. Press `M` to open Matching, then both choose **Find Match**.
4. First player is BLUE, second is RED.
5. During battle the strategic camera releases the cursor. Click a HUD card or
   use `1`–`4` to select it.
6. Left click a position on the arena to deploy; right click clears selection.
7. Server validates match state, side deploy area, slot and elixir before
   spawning a troop.
8. Troops move toward enemy Princess tower, then King tower. Destroying King
   tower ends the match.

Development shortcut: `/royalarena debug_start` starts a match with the first
two online players. `/royalarena stop_all` ends all active matches.

## Testing

`DomainSmokeTest` covers the pure-Java rules for team mirroring, deploy zones,
camera mirroring, deck cycling, elixir/lifecycle and King tower victory.

When Gradle is available, `check` also depends on `runDomainSmoke`.

See `docs/PARTS.md` for the implementation breakdown and `docs/ONLINE.md` for
deployment scope.
